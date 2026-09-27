package net.npg.wt2t.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.npg.wt2t.data.model.AppSettings
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.domain.service.MockConfigRepository
import net.npg.wt2t.domain.service.MockDayRepository
import net.npg.wt2t.domain.service.MockDailyWorkTimeRepository
import net.npg.wt2t.domain.service.MockProjectRepository
import net.npg.wt2t.domain.service.MockTaskRepository
import net.npg.wt2t.domain.service.MockTimeRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DayEditingUseCaseTest {

    @Test
    fun `execute replaces edited day entries and keeps other days`() = runTest {
        val inputDate = LocalDate.parse("2026-06-20")
        val inputOtherDate = LocalDate.parse("2026-06-19")
        val testContext = createTestContext()
        testContext.timeRepository.times.value = listOf(
            createTime(id = "old", date = inputDate, start = "08:00", end = "09:00"),
            createTime(id = "other", date = inputOtherDate, start = "10:00", end = "11:00"),
        )
        val inputTimes = listOf(
            createTime(id = "new-late", date = inputDate, start = "13:00", end = "14:00"),
            createTime(id = "new-early", date = inputDate, start = "09:00", end = "10:00"),
        )

        testContext.useCase.execute(
            UpdateDayRequest(
                date = inputDate,
                targetMinutes = 420,
                times = inputTimes,
            ),
        )

        val actualEditedTimes = testContext.timeRepository.getTimesForDate(inputDate).first()
        val actualOtherTimes = testContext.timeRepository.getTimesForDate(inputOtherDate).first()
        assertEquals(listOf("new-early", "new-late"), actualEditedTimes.map(Time::id))
        assertEquals(listOf("other"), actualOtherTimes.map(Time::id))
    }

    @Test
    fun `execute updates existing daily work time for date`() = runTest {
        val inputDate = LocalDate.parse("2026-06-20")
        val testContext = createTestContext()
        testContext.dailyWorkTimeRepository.dailyWorkTimes.value = listOf(
            DailyWorkTime(id = "existing-target", date = inputDate, minutes = 480, breakMinutes = 30),
        )

        testContext.useCase.execute(
            UpdateDayRequest(
                date = inputDate,
                targetMinutes = 360,
                breakMinutes = 45,
                times = listOf(createTime(id = "time", date = inputDate, start = "09:00", end = "12:00")),
            ),
        )

        val actualDailyWorkTime = testContext.dailyWorkTimeRepository.getDailyWorkTime(inputDate).first()
        assertEquals("existing-target", actualDailyWorkTime?.id)
        assertEquals(360, actualDailyWorkTime?.minutes)
        assertEquals(45, actualDailyWorkTime?.breakMinutes)
    }

    @Test
    fun `execute rejects overlapping entries`() = runTest {
        val inputDate = LocalDate.parse("2026-06-20")
        val testContext = createTestContext()

        assertFailsWith<IllegalArgumentException> {
            testContext.useCase.execute(
                UpdateDayRequest(
                    date = inputDate,
                    targetMinutes = 480,
                    times = listOf(
                        createTime(id = "first", date = inputDate, start = "09:00", end = "11:00"),
                        createTime(id = "second", date = inputDate, start = "10:00", end = "12:00"),
                    ),
                ),
            )
        }
    }

    @Test
    fun `execute accepts zero-minute entry within a regular booking`() = runTest {
        val inputDate = LocalDate.parse("2026-06-20")
        val testContext = createTestContext()

        testContext.useCase.execute(
            UpdateDayRequest(
                date = inputDate,
                targetMinutes = 480,
                times = listOf(
                    createTime(id = "work", date = inputDate, start = "09:00", end = "10:00"),
                    createTime(id = "note", date = inputDate, start = "09:30", end = "09:30"),
                ),
            ),
        )

        assertEquals(2, testContext.timeRepository.getTimesForDate(inputDate).first().size)
    }

    @Test
    fun `execute rejects entries without end time`() = runTest {
        val inputDate = LocalDate.parse("2026-06-20")
        val testContext = createTestContext()

        assertFailsWith<IllegalArgumentException> {
            testContext.useCase.execute(
                UpdateDayRequest(
                    date = inputDate,
                    targetMinutes = 480,
                    times = listOf(
                        Time(
                            id = "open",
                            taskId = "task",
                            date = inputDate,
                            start = LocalTime.parse("09:00"),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `create empty day uses configured target time without bookings`() = runTest {
        val inputDate = LocalDate.parse("2026-06-20")
        val testContext = createTestContext()
        testContext.configRepository.setInt(AppSettings.DEFAULT_TARGET_HOURS.name, 7)
        testContext.configRepository.setInt(AppSettings.DEFAULT_TARGET_MINUTES.name, 30)

        val actualDay = testContext.useCase.createEmptyDay(inputDate)

        assertEquals(inputDate, actualDay.date)
        assertEquals(450, actualDay.minutes)
        assertEquals(0, actualDay.breakMinutes)
        assertEquals(emptyList(), testContext.timeRepository.getTimesForDate(inputDate).first())
    }

    @Test
    fun `create empty day rejects a date with bookings`() = runTest {
        val inputDate = LocalDate.parse("2026-06-20")
        val testContext = createTestContext()
        testContext.timeRepository.saveTime(createTime(id = "existing", date = inputDate, start = "09:00", end = "10:00"))

        assertFailsWith<IllegalStateException> {
            testContext.useCase.createEmptyDay(inputDate)
        }
    }

    private fun createTestContext(): UpdateDayUseCaseTestContext {
        val projectRepository = MockProjectRepository()
        val taskRepository = MockTaskRepository()
        val timeRepository = MockTimeRepository()
        val dailyWorkTimeRepository = MockDailyWorkTimeRepository()
        val dayRepository = MockDayRepository(timeRepository, dailyWorkTimeRepository)
        val configRepository = MockConfigRepository()
        taskRepository.tasks.value = listOf(Task(id = "task", name = "Task"))

        return UpdateDayUseCaseTestContext(
            timeRepository = timeRepository,
            dailyWorkTimeRepository = dailyWorkTimeRepository,
            configRepository = configRepository,
            useCase = DayEditingUseCase(dayRepository, timeRepository, dailyWorkTimeRepository, taskRepository, projectRepository, configRepository),
        )
    }

    private fun createTime(
        id: String,
        date: LocalDate,
        start: String,
        end: String,
    ) = Time(
        id = id,
        taskId = "task",
        date = date,
        start = LocalTime.parse(start),
        end = LocalTime.parse(end),
    )
}

private data class UpdateDayUseCaseTestContext(
    val timeRepository: MockTimeRepository,
    val dailyWorkTimeRepository: MockDailyWorkTimeRepository,
    val configRepository: MockConfigRepository,
    val useCase: DayEditingUseCase,
)
