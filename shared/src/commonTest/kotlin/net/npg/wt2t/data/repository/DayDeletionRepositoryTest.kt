package net.npg.wt2t.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.AppSettings
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.db.WorkTimeDatabase
import net.npg.wt2t.domain.usecase.DayEditingUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

class DayDeletionRepositoryTest {
    @Test
    fun `delete day removes its bookings and daily values while preserving other dates`() = runTest {
        createTestContext().use { context ->
            val deletedDate = LocalDate.parse("2026-08-18")
            val preservedDate = LocalDate.parse("2026-08-19")
            val task = context.createTask()
            context.createDay(task, deletedDate)
            context.createDay(task, preservedDate)

            context.dayEditingUseCase.delete(deletedDate)

            assertEquals(emptyList(), context.timeRepository.getTimesForDate(deletedDate).first())
            assertNull(context.dailyWorkTimeRepository.getDailyWorkTime(deletedDate).first())
            assertEquals(1, context.timeRepository.getTimesForDate(preservedDate).first().size)
            assertEquals(480, context.dailyWorkTimeRepository.getDailyWorkTime(preservedDate).first()?.minutes)
        }
    }

    @Test
    fun `delete day rolls back bookings when daily value deletion fails`() = runTest {
        createTestContext().use { context ->
            val inputDate = LocalDate.parse("2026-08-18")
            val task = context.createTask()
            val inputTime = context.createDay(task, inputDate)
            context.failDailyValueDeletion(inputDate)

            assertFails { context.dayEditingUseCase.delete(inputDate) }

            assertEquals(listOf(inputTime), context.timeRepository.getTimesForDate(inputDate).first())
            assertEquals(480, context.dailyWorkTimeRepository.getDailyWorkTime(inputDate).first()?.minutes)
        }
    }

    @Test
    fun `create empty day persists configured daily values without bookings`() = runTest {
        createTestContext().use { context ->
            val inputDate = LocalDate.parse("2026-08-20")
            context.configRepository.setString(AppSettings.DEFAULT_TARGET_HOURS.name, "7")
            context.configRepository.setString(AppSettings.DEFAULT_TARGET_MINUTES.name, "30")

            val actualDay = context.dayEditingUseCase.createEmptyDay(inputDate)

            assertEquals(450, actualDay.minutes)
            assertEquals(0, actualDay.breakMinutes)
            assertEquals(emptyList(), context.timeRepository.getTimesForDate(inputDate).first())
            assertEquals(actualDay, context.dailyWorkTimeRepository.getDailyWorkTime(inputDate).first())
        }
    }

    @Test
    fun `create empty day rejects an existing daily value without replacing it`() = runTest {
        createTestContext().use { context ->
            val inputDate = LocalDate.parse("2026-08-20")
            val existingDay = DailyWorkTime(date = inputDate, minutes = 480, breakMinutes = 30)
            context.dailyWorkTimeRepository.saveDailyWorkTime(existingDay)

            assertFails { context.dayEditingUseCase.createEmptyDay(inputDate) }

            assertEquals(existingDay, context.dailyWorkTimeRepository.getDailyWorkTime(inputDate).first())
        }
    }

    private fun createTestContext(): TestContext {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WorkTimeDatabase.Schema.create(driver)
        val database = WorkTimeDatabase(driver)
        val dispatcher = Dispatchers.Default
        val timeRepository = SqlDelightTimeRepository(database, dispatcher)
        val dailyWorkTimeRepository = SqlDelightDailyWorkTimeRepository(database, dispatcher)
        val taskRepository = SqlDelightTaskRepository(database, dispatcher)
        val projectRepository = SqlDelightProjectRepository(database, dispatcher)
        val configRepository = SqlDelightConfigRepository(database, dispatcher)
        return TestContext(driver, timeRepository, dailyWorkTimeRepository, taskRepository, configRepository, DayEditingUseCase(SqlDelightDayRepository(database, dispatcher), timeRepository, dailyWorkTimeRepository, taskRepository, projectRepository, configRepository))
    }

    private class TestContext(
        private val driver: JdbcSqliteDriver,
        val timeRepository: TimeRepository,
        val dailyWorkTimeRepository: DailyWorkTimeRepository,
        private val taskRepository: TaskRepository,
        val configRepository: ConfigRepository,
        val dayEditingUseCase: DayEditingUseCase,
    ) : AutoCloseable {
        suspend fun createTask(): Task = Task(name = "Task").also { taskRepository.saveTask(it) }

        suspend fun createDay(task: Task, date: LocalDate): Time {
            val time = Time(
                taskId = task.id,
                date = date,
                start = LocalTime.parse("09:00"),
                end = LocalTime.parse("10:00"),
            )
            timeRepository.saveTime(time)
            dailyWorkTimeRepository.saveDailyWorkTime(DailyWorkTime(date = date, minutes = 480))
            return time
        }

        fun failDailyValueDeletion(date: LocalDate) {
            driver.execute(
                identifier = null,
                sql = """
                    CREATE TRIGGER fail_daily_value_delete
                    BEFORE DELETE ON dailyWorkTimeEntity
                    WHEN OLD.date = '${date}'
                    BEGIN
                        SELECT RAISE(ABORT, 'forced daily value deletion failure');
                    END
                """.trimIndent(),
                parameters = 0,
            )
        }

        override fun close() {
            driver.close()
        }
    }
}
