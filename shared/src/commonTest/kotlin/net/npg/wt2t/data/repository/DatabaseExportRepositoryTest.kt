package net.npg.wt2t.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.AppSettings
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.db.WorkTimeDatabase
import net.npg.wt2t.domain.usecase.ConfigurationUseCase
import net.npg.wt2t.ui.viewmodel.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class DatabaseExportRepositoryTest {
    @Test
    fun `replacing a day stores times at minute precision`() = runTest {
        val database = createDatabase()
        val repositories = createRepositories(database, UnconfinedTestDispatcher(testScheduler))
        val inputDate = LocalDate(2026, 8, 1)
        repositories.taskRepository.saveTask(Task(id = "task", name = "Task"))

        repositories.dayRepository.replaceDay(
            date = inputDate,
            targetMinutes = 480,
            breakMinutes = 30,
            times = listOf(
                Time(
                    id = "time",
                    taskId = "task",
                    date = inputDate,
                    start = LocalTime(8, 0, 45),
                    end = LocalTime(9, 30, 15),
                ),
            ),
        )

        val actualTime = database.workTimeDatabaseQueries.selectAllTimes().executeAsOne()
        assertEquals("08:00", actualTime.start)
        assertEquals("09:30", actualTime.end)
    }

    @Test
    fun `import stores times at minute precision`() = runTest {
        val database = createDatabase()
        val queries = database.workTimeDatabaseQueries
        val repository = DatabaseExportRepository(database, UnconfinedTestDispatcher(testScheduler))
        val export = DatabaseExport(
            projects = emptyList(),
            tasks = listOf(Task(id = "task", name = "Task")),
            times = listOf(
                Time(
                    id = "time",
                    taskId = "task",
                    date = LocalDate(2026, 8, 1),
                    start = LocalTime(8, 0, 45),
                    end = LocalTime(9, 30, 15),
                ),
            ),
            dailyWorkTimes = emptyList(),
        )

        repository.importFromJsonContent(Json.encodeToString(export))

        val actualTime = queries.selectAllTimes().executeAsOne()
        assertEquals("08:00", actualTime.start)
        assertEquals("09:30", actualTime.end)
    }

    @Test
    fun `export and import restores every table`() = runTest {
        val database = createDatabase()
        val queries = database.workTimeDatabaseQueries
        val repository = DatabaseExportRepository(database, UnconfinedTestDispatcher(testScheduler))
        queries.insertProject("project", "Project", false)
        queries.insertTask("task", "Task", false, false, "project")
        queries.insertTime("time", "task", "2026-08-01", "08:00", "09:00", "[\"note\"]")
        queries.insertDailyWorkTime("day", "2026-08-01", 480, 30)
        queries.insertConfig(AppSettings.THEME_MODE.name, "DARK")

        val backup = repository.exportToJson()
        queries.insertProject("extra", "Extra", false)
        queries.insertConfig(AppSettings.THEME_MODE.name, "LIGHT")
        repository.importFromJsonContent(backup)

        assertEquals(listOf("project"), queries.selectAllProjects().executeAsList().map { it.id })
        assertEquals(listOf("task"), queries.selectAllTasks().executeAsList().map { it.id })
        assertEquals(listOf("time"), queries.selectAllTimes().executeAsList().map { it.id })
        assertEquals(listOf("day"), queries.selectAllDailyWorkTimes().executeAsList().map { it.id })
        assertEquals("DARK", queries.selectConfig(AppSettings.THEME_MODE.name).executeAsOne())
    }

    @Test
    fun `export serializes persisted data without a platform file transfer`() = runTest {
        val database = createDatabase()
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val repository = DatabaseExportRepository(database, dispatcher)
        database.workTimeDatabaseQueries.insertProject("project", "Project", false)
        database.workTimeDatabaseQueries.insertConfig(AppSettings.THEME_MODE.name, "DARK")

        val actualExport = Json.decodeFromString<DatabaseExport>(repository.exportToJson())

        assertEquals(listOf("project"), actualExport.projects.map(Project::id))
        assertEquals("DARK", actualExport.configs[AppSettings.THEME_MODE.name])
    }

    @Test
    fun `valid imported configuration can initialize startup settings`() = runTest {
        val database = createDatabase()
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val repository = DatabaseExportRepository(database, dispatcher)
        val backup = DatabaseExport(
            projects = emptyList(),
            tasks = emptyList(),
            times = emptyList(),
            dailyWorkTimes = emptyList(),
            configs = mapOf(
                AppSettings.THEME_MODE.name to "DARK",
                AppSettings.LANGUAGE.name to "en",
                AppSettings.DEFAULT_TARGET_HOURS.name to "7",
                AppSettings.DEFAULT_TARGET_MINUTES.name to "30",
                AppSettings.MERGE_THRESHOLD_MINUTES.name to "10",
            ),
        )

        repository.importFromJsonContent(Json.encodeToString(backup))
        val configurationUseCase = ConfigurationUseCase(createRepositories(database, dispatcher).configRepository)

        assertEquals(
            ThemeMode.DARK,
            ThemeMode.parseOrDefault(configurationUseCase.getString(AppSettings.THEME_MODE)),
        )
        assertEquals("en", configurationUseCase.getString(AppSettings.LANGUAGE))
        assertEquals(7, configurationUseCase.getInt(AppSettings.DEFAULT_TARGET_HOURS))
        assertEquals(30, configurationUseCase.getInt(AppSettings.DEFAULT_TARGET_MINUTES))
        assertEquals(10, configurationUseCase.getInt(AppSettings.MERGE_THRESHOLD_MINUTES))
    }

    @Test
    fun `corrupt persisted configuration uses startup defaults`() = runTest {
        val database = createDatabase()
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        database.workTimeDatabaseQueries.insertConfig(AppSettings.THEME_MODE.name, "MIDNIGHT")
        database.workTimeDatabaseQueries.insertConfig(AppSettings.LANGUAGE.name, "fr")
        database.workTimeDatabaseQueries.insertConfig(AppSettings.DEFAULT_TARGET_HOURS.name, "eight")
        val configurationUseCase = ConfigurationUseCase(createRepositories(database, dispatcher).configRepository)

        assertEquals(
            ThemeMode.SYSTEM,
            ThemeMode.parseOrDefault(configurationUseCase.getString(AppSettings.THEME_MODE)),
        )
        assertEquals("de", configurationUseCase.getString(AppSettings.LANGUAGE))
        assertEquals(8, configurationUseCase.getInt(AppSettings.DEFAULT_TARGET_HOURS))
    }

    @Test
    fun `invalid import leaves existing database unchanged`() = runTest {
        val database = createDatabase()
        val queries = database.workTimeDatabaseQueries
        val repository = DatabaseExportRepository(database, UnconfinedTestDispatcher(testScheduler))
        queries.insertProject("project", "Project", false)

        val exception = assertFailsWith<DatabaseImportException> {
            repository.importFromJsonContent("not json")
        }

        assertEquals(DatabaseImportFailure.MALFORMED_CONTENT, exception.failure)
        assertEquals("project", queries.selectAllProjects().executeAsOne().id)
    }

    @Test
    fun `saving daily work time replaces the entry for its date`() = runTest {
        val database = createDatabase()
        val queries = database.workTimeDatabaseQueries
        queries.insertDailyWorkTime("first", "2026-08-01", 420, 30)

        queries.insertDailyWorkTime("latest", "2026-08-01", 480, 45)

        val actualDailyWorkTimes = queries.selectAllDailyWorkTimes().executeAsList()
        assertEquals(1, actualDailyWorkTimes.size)
        assertEquals("latest", actualDailyWorkTimes.single().id)
        assertEquals(480, actualDailyWorkTimes.single().minutes)
        assertEquals(45, actualDailyWorkTimes.single().breakMinutes)
    }

    @Test
    fun `duplicate daily work time dates reject import before deleting data`() = runTest {
        val database = createDatabase()
        val queries = database.workTimeDatabaseQueries
        val repository = DatabaseExportRepository(database, UnconfinedTestDispatcher(testScheduler))
        queries.insertProject("existing", "Existing", false)
        val duplicateDate = LocalDate(2026, 8, 1)
        val backup = DatabaseExport(
            projects = emptyList(),
            tasks = emptyList(),
            times = emptyList(),
            dailyWorkTimes = listOf(
                DailyWorkTime(id = "first", date = duplicateDate, minutes = 420),
                DailyWorkTime(id = "latest", date = duplicateDate, minutes = 480),
            ),
        )

        val exception = assertFailsWith<DatabaseImportException> {
            repository.importFromJsonContent(Json.encodeToString(backup))
        }

        assertEquals(DatabaseImportFailure.DUPLICATE_DAILY_WORK_TIME_DATE, exception.failure)
        assertEquals("existing", queries.selectAllProjects().executeAsOne().id)
        assertEquals(0, queries.selectAllDailyWorkTimes().executeAsList().size)
    }

    @Test
    fun `database constraints reject invalid direct writes`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WorkTimeDatabase.Schema.create(driver)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        val queries = WorkTimeDatabase(driver).workTimeDatabaseQueries
        queries.insertProject("project", "Project", false)
        queries.insertTask("task", "Task", false, false, "project")
        queries.insertTime("morning", "task", "2026-08-01", "08:00", "10:00", "[]")

        queries.updateProject("Renamed project", false, "project")
        queries.updateTask("Renamed task", false, false, "project", "task")
        assertEquals("task", queries.selectAllTimes().executeAsOne().taskId)
        assertFailsWith<Exception> {
            queries.insertProject("duplicate-project", "Renamed project", false)
        }
        assertFailsWith<Exception> {
            queries.insertTask("duplicate-task", "Renamed task", false, false, "project")
        }
        assertFailsWith<Exception> {
            queries.insertTask("orphan", "Orphan", false, false, "missing-project")
        }
        assertFailsWith<Exception> {
            queries.insertTime("invalid", "task", "2026-08-01", "11:00", "10:00", "[]")
        }
        assertFailsWith<Exception> {
            queries.insertProject("blank-project", " ", false)
        }
        assertFailsWith<Exception> {
            queries.insertProject(" ", "Blank identifier", false)
        }
        assertFailsWith<Exception> {
            queries.insertTask("blank-task", " ", false, false, null)
        }
        assertFailsWith<Exception> {
            queries.insertDailyWorkTime("invalid-target", "2026-08-01", 1_441, 0)
        }
    }

    @Test
    fun `migration keeps latest daily work time for each date`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(
            identifier = null,
            sql = """
                CREATE TABLE dailyWorkTimeEntity (
                    id TEXT NOT NULL PRIMARY KEY,
                    date TEXT NOT NULL,
                    minutes INTEGER NOT NULL,
                    breakMinutes INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent(),
            parameters = 0,
        )
        driver.execute(null, "CREATE INDEX idx_daily_work_time_date ON dailyWorkTimeEntity(date)", 0)
        driver.execute(null, "INSERT INTO dailyWorkTimeEntity VALUES ('first', '2026-08-01', 420, 30)", 0)
        driver.execute(null, "INSERT INTO dailyWorkTimeEntity VALUES ('latest', '2026-08-01', 480, 45)", 0)

        WorkTimeDatabase.Schema.migrate(driver, 2, 3)

        val queries = WorkTimeDatabase(driver).workTimeDatabaseQueries
        val actualDailyWorkTime = queries.selectDailyWorkTimeByDate("2026-08-01").executeAsOne()
        assertEquals("latest", actualDailyWorkTime.id)
        assertEquals(480, actualDailyWorkTime.minutes)
        assertEquals(45, actualDailyWorkTime.breakMinutes)

        queries.insertDailyWorkTime("newest", "2026-08-01", 510, 60)
        assertEquals("newest", queries.selectDailyWorkTimeByDate("2026-08-01").executeAsOne().id)
    }

    @Test
    fun `validation constraint migration preserves data and foreign keys`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        listOf(
            "CREATE TABLE projectEntity (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, closed INTEGER NOT NULL DEFAULT 0)",
            "CREATE TABLE taskEntity (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, freeTime INTEGER NOT NULL DEFAULT 0, closed INTEGER NOT NULL DEFAULT 0, projectId TEXT, FOREIGN KEY (projectId) REFERENCES projectEntity(id))",
            "CREATE TABLE timeEntity (id TEXT NOT NULL PRIMARY KEY, taskId TEXT NOT NULL, date TEXT NOT NULL, start TEXT NOT NULL, end TEXT, description TEXT NOT NULL, FOREIGN KEY (taskId) REFERENCES taskEntity(id))",
            "CREATE TABLE dailyWorkTimeEntity (id TEXT NOT NULL PRIMARY KEY, date TEXT NOT NULL, minutes INTEGER NOT NULL, breakMinutes INTEGER NOT NULL DEFAULT 0)",
            "CREATE TABLE configEntity (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)",
            "CREATE INDEX idx_time_date ON timeEntity(date)",
            "CREATE UNIQUE INDEX idx_daily_work_time_date ON dailyWorkTimeEntity(date)",
            "INSERT INTO projectEntity VALUES ('project', 'Project', 0)",
            "INSERT INTO taskEntity VALUES ('task', 'Task', 0, 0, 'project')",
            "INSERT INTO timeEntity VALUES ('time', 'task', '2026-08-01', '08:00', '09:00', '[]')",
            "INSERT INTO dailyWorkTimeEntity VALUES ('target', '2026-08-01', 480, 30)",
            "INSERT INTO configEntity VALUES ('theme', 'DARK')",
        ).forEach { statement -> driver.execute(null, statement, 0) }

        WorkTimeDatabase.Schema.migrate(driver, 3, 4)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)

        val queries = WorkTimeDatabase(driver).workTimeDatabaseQueries
        assertEquals("Project", queries.selectAllProjects().executeAsOne().name)
        assertEquals("Task", queries.selectAllTasks().executeAsOne().name)
        assertEquals("time", queries.selectAllTimes().executeAsOne().id)
        assertEquals("target", queries.selectAllDailyWorkTimes().executeAsOne().id)
        assertEquals("DARK", queries.selectConfig("theme").executeAsOne())
        assertFailsWith<Exception> {
            queries.insertTask("orphan", "Orphan", false, false, "missing-project")
        }
    }

    @Test
    fun `zero-minute migration preserves existing bookings and permits equal boundaries`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        listOf(
            "CREATE TABLE taskEntity (id TEXT NOT NULL PRIMARY KEY)",
            "CREATE TABLE timeEntity (id TEXT NOT NULL PRIMARY KEY, taskId TEXT NOT NULL, date TEXT NOT NULL, start TEXT NOT NULL, end TEXT, description TEXT NOT NULL, FOREIGN KEY (taskId) REFERENCES taskEntity(id), CHECK (end IS NULL OR start < end))",
            "CREATE INDEX idx_time_date ON timeEntity(date)",
            "CREATE UNIQUE INDEX idx_active_time_date ON timeEntity(date) WHERE end IS NULL",
            "INSERT INTO taskEntity VALUES ('task')",
            "INSERT INTO timeEntity VALUES ('existing', 'task', '2026-08-29', '14:00', '14:30', '[]')",
        ).forEach { statement -> driver.execute(null, statement, 0) }

        WorkTimeDatabase.Schema.migrate(driver, 5, 6)
        driver.execute(null, "INSERT INTO timeEntity VALUES ('note', 'task', '2026-08-29', '14:37', '14:37', '[\"Quick note\"]')", 0)

        val actualTimes = WorkTimeDatabase(driver).workTimeDatabaseQueries.selectAllTimes().executeAsList()
        assertEquals(listOf("existing", "note"), actualTimes.map { it.id })
    }

    private fun createDatabase(): WorkTimeDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WorkTimeDatabase.Schema.create(driver)
        return WorkTimeDatabase(driver)
    }

    private fun createRepositories(
        database: WorkTimeDatabase,
        dispatcher: CoroutineDispatcher,
    ) = TestRepositories(
        taskRepository = SqlDelightTaskRepository(database, dispatcher),
        dayRepository = SqlDelightDayRepository(database, dispatcher),
        configRepository = SqlDelightConfigRepository(database, dispatcher),
    )

    private data class TestRepositories(
        val taskRepository: TaskRepository,
        val dayRepository: DayRepository,
        val configRepository: ConfigRepository,
    )
}
