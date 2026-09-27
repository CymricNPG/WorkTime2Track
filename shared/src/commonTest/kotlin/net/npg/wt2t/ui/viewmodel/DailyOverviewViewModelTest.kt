package net.npg.wt2t.ui.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.BookingAdjustmentDirection
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.domain.service.MockConfigRepository
import net.npg.wt2t.domain.service.MockDayRepository
import net.npg.wt2t.domain.service.MockDailyWorkTimeRepository
import net.npg.wt2t.domain.service.MockProjectRepository
import net.npg.wt2t.domain.service.MockTaskRepository
import net.npg.wt2t.domain.service.MockTimeRepository
import net.npg.wt2t.domain.service.ReportData
import net.npg.wt2t.domain.service.ReportService
import net.npg.wt2t.domain.usecase.ReportingUseCase
import net.npg.wt2t.domain.usecase.DayEditingUseCase
import net.npg.wt2t.data.repository.TimeRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.math.absoluteValue

@OptIn(ExperimentalCoroutinesApi::class)
class DailyOverviewViewModelTest {

    @Test
    fun `refresh reloads persisted changes into cached overview`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val inputDate = LocalDate(2023, 10, 27)
            val clock = FixedClock(Instant.parse("2023-10-27T12:00:00Z"))
            val dailyWorkTimeRepository = MockDailyWorkTimeRepository()
            val taskRepository = MockTaskRepository()
            val timeRepository = MockTimeRepository()
            val projectRepository = MockProjectRepository()
            val inputDailyWorkTime = DailyWorkTime(
                date = inputDate,
                minutes = 480,
                breakMinutes = 30,
            )
            dailyWorkTimeRepository.saveDailyWorkTime(inputDailyWorkTime)
            val viewModel = DailyOverviewViewModel(
                reportingUseCase = ReportingUseCase(timeRepository, dailyWorkTimeRepository, taskRepository, projectRepository, FakeReportService(), clock),
                dayEditingUseCase = createDayEditingUseCase(timeRepository, dailyWorkTimeRepository),
                clock = clock,
            )
            advanceUntilIdle()
            assertEquals(30.minutes, viewModel.uiState.value.dayEntries.single().breakTime)
            assertEquals(emptyList(), viewModel.uiState.value.dayEntries.single().taskEntries)

            dailyWorkTimeRepository.saveDailyWorkTime(
                inputDailyWorkTime.copy(minutes = 420, breakMinutes = 45),
            )
            val inputTask = Task(name = "Updated task")
            taskRepository.saveTask(inputTask)
            timeRepository.saveTime(
                Time(
                    taskId = inputTask.id,
                    date = inputDate,
                    start = LocalTime.parse("09:00"),
                    end = LocalTime.parse("10:00"),
                ),
            )
            viewModel.refresh()
            advanceUntilIdle()

            val actualEntry = viewModel.uiState.value.dayEntries.single()
            assertEquals(45.minutes, actualEntry.breakTime)
            assertEquals(60.minutes, actualEntry.totalWorkedTime)
            assertEquals("Updated task", actualEntry.taskEntries.single().taskName)
            assertEquals(60.minutes, actualEntry.taskEntries.single().duration)
            assertEquals((-405).minutes, actualEntry.overtime)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `day with target time and no task is included in overview`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val inputDate = LocalDate(2023, 10, 27)
            val clock = FixedClock(Instant.parse("2023-10-27T12:00:00Z"))
            val dailyWorkTimeRepository = MockDailyWorkTimeRepository()
            val timeRepository = MockTimeRepository()
            val taskRepository = MockTaskRepository()
            val projectRepository = MockProjectRepository()
            dailyWorkTimeRepository.saveDailyWorkTime(
                DailyWorkTime(
                    date = inputDate,
                    minutes = 480,
                    breakMinutes = 30,
                ),
            )

            val viewModel = DailyOverviewViewModel(
                reportingUseCase = ReportingUseCase(timeRepository, dailyWorkTimeRepository, taskRepository, projectRepository, FakeReportService(), clock),
                dayEditingUseCase = createDayEditingUseCase(timeRepository, dailyWorkTimeRepository),
                clock = clock,
            )

            advanceUntilIdle()

            val actualEntry = viewModel.uiState.value.dayEntries.single()
            assertEquals(inputDate, actualEntry.date)
            assertEquals(0.minutes, actualEntry.totalWorkedTime)
            assertEquals((-510).minutes, actualEntry.overtime)
            assertEquals(30.minutes, actualEntry.breakTime)
            assertEquals(emptyList(), actualEntry.taskEntries)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `negative report duration uses a single minus sign`() {
        val inputMinutes = -90L

        val actualDuration = formatDuration(inputMinutes)

        assertEquals("-01:30", actualDuration)
    }

    @Test
    fun `negative report duration below one hour keeps minus before hours`() {
        val inputMinutes = -30L

        val actualDuration = formatDuration(inputMinutes)

        assertEquals("-00:30", actualDuration)
    }

    @Test
    fun `positive report duration has no sign`() {
        val inputMinutes = 90L

        val actualDuration = formatDuration(inputMinutes)

        assertEquals("01:30", actualDuration)
    }

    @Test
    fun `delayed older overview load cannot overwrite a newer period`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val olderDate = LocalDate(2026, 1, 1)
            val newerDate = LocalDate(2026, 2, 1)
            val timeRepository = DelayedTimeRepository(
                listOf(
                    Time(id = "old", taskId = "task", date = olderDate, start = LocalTime(9, 0), end = LocalTime(10, 0)),
                    Time(id = "new", taskId = "task", date = newerDate, start = LocalTime(9, 0), end = LocalTime(10, 0)),
                ),
            )
            val viewModel = DailyOverviewViewModel(
                ReportingUseCase(
                    timeRepository,
                    MockDailyWorkTimeRepository(),
                    MockTaskRepository().also { it.tasks.value = listOf(Task(id = "task", name = "Task")) },
                    MockProjectRepository(),
                    FakeReportService(),
                    FixedClock(Instant.parse("2026-02-01T12:00:00Z")),
                ),
                createDayEditingUseCase(MockTimeRepository(), MockDailyWorkTimeRepository()),
                FixedClock(Instant.parse("2026-02-01T12:00:00Z")),
            )
            advanceUntilIdle()

            viewModel.updatePeriod(olderDate, olderDate)
            viewModel.updatePeriod(newerDate, newerDate)
            advanceUntilIdle()

            assertEquals(listOf(newerDate), viewModel.uiState.value.dayEntries.map(DayEntry::date))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `creating an empty day expands the overview period and shows the new day`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val existingDate = LocalDate(2026, 6, 20)
            val createdDate = LocalDate(2026, 6, 10)
            val clock = FixedClock(Instant.parse("2026-06-20T12:00:00Z"))
            val timeRepository = MockTimeRepository()
            val dailyWorkTimeRepository = MockDailyWorkTimeRepository()
            dailyWorkTimeRepository.saveDailyWorkTime(DailyWorkTime(date = existingDate, minutes = 480))
            val viewModel = DailyOverviewViewModel(
                reportingUseCase = ReportingUseCase(
                    timeRepository,
                    dailyWorkTimeRepository,
                    MockTaskRepository(),
                    MockProjectRepository(),
                    FakeReportService(),
                    clock,
                ),
                dayEditingUseCase = createDayEditingUseCase(timeRepository, dailyWorkTimeRepository),
                clock = clock,
            )
            advanceUntilIdle()

            viewModel.createEmptyDay(createdDate)
            advanceUntilIdle()

            assertEquals(createdDate, viewModel.uiState.value.startDate)
            assertEquals(existingDate, viewModel.uiState.value.endDate)
            assertEquals(listOf(existingDate, createdDate), viewModel.uiState.value.dayEntries.map(DayEntry::date))
            assertEquals(480, dailyWorkTimeRepository.getDailyWorkTime(createdDate).first()?.minutes)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createDayEditingUseCase(
        timeRepository: MockTimeRepository,
        dailyWorkTimeRepository: MockDailyWorkTimeRepository,
    ): DayEditingUseCase = DayEditingUseCase(
        dayRepository = MockDayRepository(timeRepository, dailyWorkTimeRepository),
        timeRepository = timeRepository,
        dailyWorkTimeRepository = dailyWorkTimeRepository,
        taskRepository = MockTaskRepository(),
        projectRepository = MockProjectRepository(),
        configRepository = MockConfigRepository(),
    )

    private class FixedClock(private val instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    private fun formatDuration(minutes: Long): String {
        val absolute = minutes.absoluteValue
        return (if (minutes < 0) "-" else "") + "%02d:%02d".format(absolute / 60, absolute % 60)
    }

    private class FakeReportService : ReportService {
        override suspend fun generatePdfReport(data: ReportData, fileName: String): String = fileName
    }

    private class DelayedTimeRepository(private val values: List<Time>) : TimeRepository {
        private var allTimesLoadCount = 0

        override fun getTimesForDate(date: LocalDate): Flow<List<Time>> = flowOf(values.filter { it.date == date })
        override fun getAllTimes(): Flow<List<Time>> = flow {
            allTimesLoadCount++
            if (allTimesLoadCount == 3) delay(100)
            emit(values)
        }
        override suspend fun saveTime(time: Time) = Unit
        override suspend fun switchBooking(taskId: String, date: LocalDate, startTime: LocalTime, mergeThresholdMinutes: Int): Time = error("Not used")
        override suspend fun endActiveBooking(date: LocalDate, endTime: LocalTime): Time? = null
        override suspend fun adjustActiveBookingStart(direction: BookingAdjustmentDirection, date: LocalDate, adjustmentMinutes: Int, currentTime: LocalTime): Time = error("Not used")
        override suspend fun deleteTime(id: String) = Unit
        override suspend fun replaceTimesForDate(date: LocalDate, times: List<Time>) = Unit
    }
}
