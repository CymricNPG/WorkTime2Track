package net.npg.wt2t.testing

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.npg.wt2t.data.model.AppSettings
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.data.repository.SqlDelightConfigRepository
import net.npg.wt2t.data.repository.SqlDelightDayRepository
import net.npg.wt2t.data.repository.SqlDelightDailyWorkTimeRepository
import net.npg.wt2t.data.repository.SqlDelightProjectRepository
import net.npg.wt2t.data.repository.SqlDelightTaskRepository
import net.npg.wt2t.data.repository.SqlDelightTimeRepository
import net.npg.wt2t.db.WorkTimeDatabase
import net.npg.wt2t.domain.usecase.BookingUseCase
import net.npg.wt2t.domain.usecase.ConfigurationUseCase
import net.npg.wt2t.domain.usecase.DayEditingUseCase
import net.npg.wt2t.domain.usecase.EndOfDayUseCase
import net.npg.wt2t.domain.usecase.ProjectManagementUseCase
import java.io.File
import kotlin.time.Clock

sealed interface IntegrationTestDatabase {
    data object InMemory : IntegrationTestDatabase

    data class Persistent(
        val path: String,
        val resetBeforeTest: Boolean = true,
    ) : IntegrationTestDatabase
}

class IntegrationTestEnvironment private constructor(
    private val driver: SqlDriver,
    val databasePath: String?,
    val database: WorkTimeDatabase,
    val timeRepository: SqlDelightTimeRepository,
    val dailyWorkTimeRepository: SqlDelightDailyWorkTimeRepository,
    val configRepository: SqlDelightConfigRepository,
    val projectManagementUseCase: ProjectManagementUseCase,
    val bookingUseCase: BookingUseCase,
    val configurationUseCase: ConfigurationUseCase,
    val dayEditingUseCase: DayEditingUseCase,
    val endOfDayUseCase: EndOfDayUseCase,
) : AutoCloseable {

    suspend fun createProject(
        name: String,
        closed: Boolean = false,
    ): Project {
        val project = projectManagementUseCase.createProject(name)
        if (closed) {
            projectManagementUseCase.closeProject(project.id)
            return project.copy(closed = true)
        }
        return project
    }

    suspend fun createTask(
        name: String,
        project: Project? = null,
        freeTime: Boolean = false,
        closed: Boolean = false,
    ): Task {
        val task = projectManagementUseCase.createTask(
            name = name,
            projectId = project?.id,
            freeTime = freeTime,
        )
        if (closed) {
            projectManagementUseCase.closeTask(task.id)
            return task.copy(closed = true)
        }
        return task
    }

    suspend fun createTime(
        task: Task,
        date: LocalDate,
        start: LocalTime,
        end: LocalTime? = null,
        notes: List<String> = emptyList(),
    ): Time {
        val time = Time(
            taskId = task.id,
            date = date,
            start = start,
            end = end,
            description = notes,
        )
        timeRepository.saveTime(time)
        return time
    }

    suspend fun createDailyWorkTime(
        date: LocalDate,
        minutes: Int,
    ): DailyWorkTime {
        val dailyWorkTime = DailyWorkTime(date = date, minutes = minutes)
        dailyWorkTimeRepository.saveDailyWorkTime(dailyWorkTime)
        return dailyWorkTime
    }

    suspend fun setConfig(
        setting: AppSettings,
        value: String,
    ) {
        configRepository.setString(setting.name, value)
    }

    suspend fun setConfig(
        setting: AppSettings,
        value: Int,
    ) {
        configRepository.setInt(setting.name, value)
    }

    override fun close() {
        driver.close()
    }

    companion object {
        fun create(
            databaseConfiguration: IntegrationTestDatabase = IntegrationTestDatabase.InMemory,
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
            clock: Clock = Clock.System,
        ): IntegrationTestEnvironment {
            val openedDatabase = openDatabase(databaseConfiguration)
            val database = WorkTimeDatabase(openedDatabase.driver)
            val projectRepository = SqlDelightProjectRepository(database, dispatcher)
            val taskRepository = SqlDelightTaskRepository(database, dispatcher)
            val timeRepository = SqlDelightTimeRepository(database, dispatcher)
            val dailyWorkTimeRepository = SqlDelightDailyWorkTimeRepository(database, dispatcher)
            val dayRepository = SqlDelightDayRepository(database, dispatcher)
            val configRepository = SqlDelightConfigRepository(database, dispatcher)
            return IntegrationTestEnvironment(
                driver = openedDatabase.driver,
                databasePath = openedDatabase.path,
                database = database,
                timeRepository = timeRepository,
                dailyWorkTimeRepository = dailyWorkTimeRepository,
                configRepository = configRepository,
                projectManagementUseCase = ProjectManagementUseCase(projectRepository, taskRepository, timeRepository),
                bookingUseCase = BookingUseCase(timeRepository, taskRepository, projectRepository, configRepository, clock),
                configurationUseCase = ConfigurationUseCase(configRepository),
                dayEditingUseCase = DayEditingUseCase(dayRepository, timeRepository, dailyWorkTimeRepository, taskRepository, projectRepository, configRepository),
                endOfDayUseCase = EndOfDayUseCase(dayRepository, timeRepository, dailyWorkTimeRepository, configRepository, taskRepository),
            )
        }

        private fun openDatabase(configuration: IntegrationTestDatabase): OpenedDatabase {
            return when (configuration) {
                IntegrationTestDatabase.InMemory -> {
                    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
                    WorkTimeDatabase.Schema.create(driver)
                    OpenedDatabase(driver = driver, path = null)
                }

                is IntegrationTestDatabase.Persistent -> openPersistentDatabase(configuration)
            }
        }

        private fun openPersistentDatabase(
            configuration: IntegrationTestDatabase.Persistent,
        ): OpenedDatabase {
            val databaseFile = File(configuration.path).absoluteFile
            databaseFile.parentFile?.mkdirs()

            if (configuration.resetBeforeTest && databaseFile.exists()) {
                check(databaseFile.delete()) {
                    "Could not reset integration-test database: ${databaseFile.absolutePath}"
                }
            }

            val shouldCreateSchema = !databaseFile.exists() || databaseFile.length() == 0L
            val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
            if (shouldCreateSchema) {
                WorkTimeDatabase.Schema.create(driver)
            }

            return OpenedDatabase(
                driver = driver,
                path = databaseFile.absolutePath,
            )
        }
    }
}

private data class OpenedDatabase(
    val driver: SqlDriver,
    val path: String?,
)
