package net.npg.wt2t.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.BookingAdjustmentDirection
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.db.WorkTimeDatabase
import net.npg.wt2t.domain.usecase.BookingUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Clock

class BookingTransitionRepositoryTest {
    @Test
    fun `concurrent starts are serialized into one active booking`() = runTest {
        createTestContext().use { context ->
            val inputTask = context.createTask("Concurrent task")
            val inputDate = LocalDate(2026, 8, 22)
            val inputStart = LocalTime(9, 0)

            val actualBookings = coroutineScope {
                List(20) {
                    async(Dispatchers.Default) {
                        context.bookingUseCase.startBooking(inputTask.id, inputDate, inputStart)
                    }
                }.awaitAll()
            }

            val actualTimes = context.timeRepository.getTimesForDate(inputDate).first()
            assertEquals(1, actualTimes.count { it.end == null })
            assertEquals(1, actualTimes.size)
            assertEquals(1, actualBookings.map { it.id }.distinct().size)
        }
    }

    @Test
    fun `rapid repeated tap is idempotent`() = runTest {
        createTestContext().use { context ->
            val inputTask = context.createTask("Rapid tap task")
            val inputDate = LocalDate(2026, 8, 22)
            val inputStart = LocalTime(9, 0)

            val firstBooking = context.bookingUseCase.startBooking(inputTask.id, inputDate, inputStart)
            val secondBooking = context.bookingUseCase.startBooking(inputTask.id, inputDate, inputStart)

            assertEquals(firstBooking, secondBooking)
            assertEquals(1, context.timeRepository.getTimesForDate(inputDate).first().size)
        }
    }

    @Test
    fun `switching tasks within the start minute closes the previous booking at zero minutes`() = runTest {
        createTestContext().use { context ->
            val firstTask = context.createTask("First immediate switch task")
            val secondTask = context.createTask("Second immediate switch task")
            val inputDate = LocalDate(2026, 8, 29)

            val firstBooking = context.bookingUseCase.startBooking(firstTask.id, inputDate, LocalTime(15, 59, 5))
            val secondBooking = context.bookingUseCase.startBooking(secondTask.id, inputDate, LocalTime(15, 59, 55))

            val actualTimes = context.timeRepository.getTimesForDate(inputDate).first().associateBy(Time::id)
            assertEquals(LocalTime(15, 59), actualTimes.getValue(firstBooking.id).end)
            assertNull(actualTimes.getValue(secondBooking.id).end)
        }
    }

    @Test
    fun `failed switch rolls back the closed active booking`() = runTest {
        createTestContext().use { context ->
            val inputTask = context.createTask("Existing task")
            val inputDate = LocalDate(2026, 8, 22)
            val activeBooking = context.bookingUseCase.startBooking(inputTask.id, inputDate, LocalTime(9, 0))

            assertFails {
                context.bookingUseCase.startBooking("missing-task", inputDate, LocalTime(10, 0))
            }

            val actualTimes = context.timeRepository.getTimesForDate(inputDate).first()
            assertEquals(1, actualTimes.size)
            assertEquals(activeBooking.id, actualTimes.single().id)
            assertNull(actualTimes.single().end)
        }
    }

    @Test
    fun `start rejects a clock moving behind the latest booking`() = runTest {
        createTestContext().use { context ->
            val inputTask = context.createTask("Clock task")
            val inputDate = LocalDate(2026, 8, 22)
            context.createTime(inputTask, inputDate, LocalTime(9, 0), LocalTime(10, 0))

            assertFailsWith<IllegalArgumentException> {
                context.bookingUseCase.startBooking(inputTask.id, inputDate, LocalTime(9, 59))
            }

            assertEquals(1, context.timeRepository.getTimesForDate(inputDate).first().size)
        }
    }

    @Test
    fun `switch rejects a boundary behind a later finished booking`() = runTest {
        createTestContext().use { context ->
            val inputTask = context.createTask("Overlapping clock task")
            val inputDate = LocalDate(2026, 8, 22)
            val activeBooking = context.createTime(inputTask, inputDate, LocalTime(8, 0))
            context.createTime(inputTask, inputDate, LocalTime(9, 0), LocalTime(10, 0))

            assertFailsWith<IllegalArgumentException> {
                context.bookingUseCase.startBooking(inputTask.id, inputDate, LocalTime(9, 30))
            }

            val actualActiveBooking = context.timeRepository.getTimesForDate(inputDate).first()
                .single { it.end == null }
            assertEquals(activeBooking.id, actualActiveBooking.id)
        }
    }

    @Test
    fun `end rejects a time before the active booking start`() = runTest {
        createTestContext().use { context ->
            val inputTask = context.createTask("End boundary task")
            val inputDate = LocalDate(2026, 8, 22)
            val activeBooking = context.bookingUseCase.startBooking(inputTask.id, inputDate, LocalTime(10, 0))

            assertFailsWith<IllegalArgumentException> {
                context.bookingUseCase.endBooking(inputDate, LocalTime(9, 0))
            }

            val actualBooking = context.timeRepository.getTimesForDate(inputDate).first().single()
            assertEquals(activeBooking.id, actualBooking.id)
            assertNull(actualBooking.end)
        }
    }

    @Test
    fun `ending within the start minute preserves a zero minute booking and its note`() = runTest {
        createTestContext().use { context ->
            val inputTask = context.createTask("Immediate stop task")
            val inputDate = LocalDate(2026, 8, 29)
            val activeBooking = context.bookingUseCase.startBooking(inputTask.id, inputDate, LocalTime(14, 37, 5))
            context.timeRepository.saveTime(activeBooking.copy(description = listOf("Quick note")))

            context.bookingUseCase.endBooking(inputDate, LocalTime(14, 37, 55))

            val actualBooking = context.timeRepository.getTimesForDate(inputDate).first().single()
            assertEquals(LocalTime(14, 37), actualBooking.start)
            assertEquals(LocalTime(14, 37), actualBooking.end)
            assertEquals(listOf("Quick note"), actualBooking.description)
        }
    }

    @Test
    fun `repository rejects a second active booking for the same date`() = runTest {
        createTestContext().use { context ->
            val firstTask = context.createTask("First invariant task")
            val secondTask = context.createTask("Second invariant task")
            val inputDate = LocalDate(2026, 8, 22)
            val firstBooking = context.createTime(firstTask, inputDate, LocalTime(9, 0))

            assertFails {
                context.createTime(secondTask, inputDate, LocalTime(10, 0))
            }

            val actualTimes = context.timeRepository.getTimesForDate(inputDate).first()
            assertEquals(listOf(firstBooking), actualTimes)
        }
    }

    @Test
    fun `earlier adjustment moves a shared boundary atomically`() = runTest {
        createTestContext().use { context ->
            val previousTask = context.createTask("Previous task")
            val activeTask = context.createTask("Active task")
            val inputDate = LocalDate(2026, 8, 22)
            val previousBooking = context.createTime(
                previousTask,
                inputDate,
                LocalTime(9, 0),
                LocalTime(10, 0),
            )
            val activeBooking = context.createTime(activeTask, inputDate, LocalTime(10, 0))

            context.bookingUseCase.adjustBookingStart(
                BookingAdjustmentDirection.EARLIER,
                inputDate,
                LocalTime(11, 0),
            )

            val actualTimes = context.timeRepository.getTimesForDate(inputDate).first().associateBy(Time::id)
            assertEquals(LocalTime(9, 55), actualTimes.getValue(previousBooking.id).end)
            assertEquals(LocalTime(9, 55), actualTimes.getValue(activeBooking.id).start)
        }
    }

    @Test
    fun `later adjustment moves a shared boundary atomically`() = runTest {
        createTestContext().use { context ->
            val previousTask = context.createTask("Previous task")
            val activeTask = context.createTask("Active task")
            val inputDate = LocalDate(2026, 8, 22)
            val previousBooking = context.createTime(
                previousTask,
                inputDate,
                LocalTime(9, 0),
                LocalTime(10, 0),
            )
            val activeBooking = context.createTime(activeTask, inputDate, LocalTime(10, 0))

            context.bookingUseCase.adjustBookingStart(
                BookingAdjustmentDirection.LATER,
                inputDate,
                LocalTime(10, 10),
            )

            val actualTimes = context.timeRepository.getTimesForDate(inputDate).first().associateBy(Time::id)
            assertEquals(LocalTime(10, 5), actualTimes.getValue(previousBooking.id).end)
            assertEquals(LocalTime(10, 5), actualTimes.getValue(activeBooking.id).start)
        }
    }

    @Test
    fun `earlier adjustment crossing a gap attaches the previous booking`() = runTest {
        createTestContext().use { context ->
            val previousTask = context.createTask("Previous task")
            val activeTask = context.createTask("Active task")
            val inputDate = LocalDate(2026, 8, 22)
            val previousBooking = context.createTime(
                previousTask,
                inputDate,
                LocalTime(9, 0),
                LocalTime(10, 0),
            )
            val activeBooking = context.createTime(activeTask, inputDate, LocalTime(10, 3))

            context.bookingUseCase.adjustBookingStart(
                BookingAdjustmentDirection.EARLIER,
                inputDate,
                LocalTime(11, 0),
            )

            val actualTimes = context.timeRepository.getTimesForDate(inputDate).first().associateBy(Time::id)
            assertEquals(LocalTime(9, 58), actualTimes.getValue(previousBooking.id).end)
            assertEquals(LocalTime(9, 58), actualTimes.getValue(activeBooking.id).start)
        }
    }

    @Test
    fun `earlier adjustment inside a gap only moves the active booking`() = runTest {
        createTestContext().use { context ->
            val previousTask = context.createTask("Previous task")
            val activeTask = context.createTask("Active task")
            val inputDate = LocalDate(2026, 8, 22)
            val previousBooking = context.createTime(
                previousTask,
                inputDate,
                LocalTime(9, 0),
                LocalTime(10, 0),
            )
            val activeBooking = context.createTime(activeTask, inputDate, LocalTime(10, 10, 30))

            context.bookingUseCase.adjustBookingStart(
                BookingAdjustmentDirection.EARLIER,
                inputDate,
                LocalTime(11, 0),
            )

            val actualTimes = context.timeRepository.getTimesForDate(inputDate).first().associateBy(Time::id)
            assertEquals(LocalTime(10, 0), actualTimes.getValue(previousBooking.id).end)
            assertEquals(LocalTime(10, 5), actualTimes.getValue(activeBooking.id).start)
        }
    }

    @Test
    fun `adjustment rejects a previous booking shorter than one configured step without partial updates`() = runTest {
        createTestContext().use { context ->
            val previousTask = context.createTask("Previous task")
            val activeTask = context.createTask("Active task")
            val inputDate = LocalDate(2026, 8, 22)
            val previousBooking = context.createTime(
                previousTask,
                inputDate,
                LocalTime(9, 0),
                LocalTime(9, 9),
            )
            val activeBooking = context.createTime(activeTask, inputDate, LocalTime(9, 9))

            assertFailsWith<IllegalArgumentException> {
                context.bookingUseCase.adjustBookingStart(
                    BookingAdjustmentDirection.EARLIER,
                    inputDate,
                    LocalTime(11, 0),
                )
            }

            val actualTimes = context.timeRepository.getTimesForDate(inputDate).first().associateBy(Time::id)
            assertEquals(LocalTime(9, 9), actualTimes.getValue(previousBooking.id).end)
            assertEquals(LocalTime(9, 9), actualTimes.getValue(activeBooking.id).start)
        }
    }

    @Test
    fun `adjustment allows a previous booking of exactly one configured step`() = runTest {
        createTestContext().use { context ->
            val previousTask = context.createTask("Previous task")
            val activeTask = context.createTask("Active task")
            val inputDate = LocalDate(2026, 8, 22)
            val previousBooking = context.createTime(
                previousTask,
                inputDate,
                LocalTime(9, 0),
                LocalTime(9, 10),
            )
            val activeBooking = context.createTime(activeTask, inputDate, LocalTime(9, 10))

            context.bookingUseCase.adjustBookingStart(
                BookingAdjustmentDirection.EARLIER,
                inputDate,
                LocalTime(11, 0),
            )

            val actualTimes = context.timeRepository.getTimesForDate(inputDate).first().associateBy(Time::id)
            assertEquals(LocalTime(9, 5), actualTimes.getValue(previousBooking.id).end)
            assertEquals(LocalTime(9, 5), actualTimes.getValue(activeBooking.id).start)
        }
    }

    @Test
    fun `adjustment drops seconds from a shared boundary`() = runTest {
        createTestContext().use { context ->
            val previousTask = context.createTask("Previous task")
            val activeTask = context.createTask("Active task")
            val inputDate = LocalDate(2026, 8, 22)
            val previousBooking = context.createTime(
                previousTask,
                inputDate,
                LocalTime(9, 0, 45),
                LocalTime(10, 0, 30),
            )
            val activeBooking = context.createTime(activeTask, inputDate, LocalTime(10, 0, 30))

            context.bookingUseCase.adjustBookingStart(
                BookingAdjustmentDirection.EARLIER,
                inputDate,
                LocalTime(11, 0, 59),
            )

            val actualTimes = context.timeRepository.getTimesForDate(inputDate).first().associateBy(Time::id)
            assertEquals(LocalTime(9, 55), actualTimes.getValue(previousBooking.id).end)
            assertEquals(LocalTime(9, 55), actualTimes.getValue(activeBooking.id).start)
        }
    }

    @Test
    fun `earlier adjustment rejects crossing midnight without changing the active booking`() = runTest {
        createTestContext().use { context ->
            val activeTask = context.createTask("Active task")
            val inputDate = LocalDate(2026, 8, 22)
            val activeBooking = context.createTime(activeTask, inputDate, LocalTime(0, 4, 30))

            assertFailsWith<IllegalArgumentException> {
                context.bookingUseCase.adjustBookingStart(
                    BookingAdjustmentDirection.EARLIER,
                    inputDate,
                    LocalTime(1, 0),
                )
            }

            val actualTime = context.timeRepository.getTimesForDate(inputDate).first().single()
            assertEquals(activeBooking.id, actualTime.id)
            assertEquals(LocalTime(0, 4), actualTime.start)
        }
    }

    @Test
    fun `later adjustment requires the active booking to remain one configured step long`() = runTest {
        createTestContext().use { context ->
            val activeTask = context.createTask("Active task")
            val inputDate = LocalDate(2026, 8, 22)
            val activeBooking = context.createTime(activeTask, inputDate, LocalTime(10, 0))

            assertFailsWith<IllegalArgumentException> {
                context.bookingUseCase.adjustBookingStart(
                    BookingAdjustmentDirection.LATER,
                    inputDate,
                    LocalTime(10, 9, 59),
                )
            }
            context.bookingUseCase.adjustBookingStart(
                BookingAdjustmentDirection.LATER,
                inputDate,
                LocalTime(10, 10, 59),
            )

            val actualTime = context.timeRepository.getTimesForDate(inputDate).first().single()
            assertEquals(activeBooking.id, actualTime.id)
            assertEquals(LocalTime(10, 5), actualTime.start)
        }
    }

    private fun createTestContext(): TestContext {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WorkTimeDatabase.Schema.create(driver)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        val database = WorkTimeDatabase(driver)
        val dispatcher = Dispatchers.Default
        val timeRepository = SqlDelightTimeRepository(database, dispatcher)
        val dailyWorkTimeRepository = SqlDelightDailyWorkTimeRepository(database, dispatcher)
        val projectRepository = SqlDelightProjectRepository(database, dispatcher)
        val taskRepository = SqlDelightTaskRepository(database, dispatcher)
        val configRepository = SqlDelightConfigRepository(database, dispatcher)
        return TestContext(
            driver = driver,
            timeRepository = timeRepository,
            taskRepository = taskRepository,
            bookingUseCase = BookingUseCase(timeRepository, taskRepository, projectRepository, configRepository, Clock.System),
        )
    }

    private class TestContext(
        private val driver: JdbcSqliteDriver,
        val timeRepository: TimeRepository,
        private val taskRepository: TaskRepository,
        val bookingUseCase: BookingUseCase,
    ) : AutoCloseable {
        suspend fun createTask(name: String): Task {
            val task = Task(name = name)
            taskRepository.saveTask(task)
            return task
        }

        suspend fun createTime(
            task: Task,
            date: LocalDate,
            start: LocalTime,
            end: LocalTime? = null,
        ): Time {
            val time = Time(taskId = task.id, date = date, start = start, end = end)
            timeRepository.saveTime(time)
            return time
        }

        override fun close() {
            driver.close()
        }
    }
}
