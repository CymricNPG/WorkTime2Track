package net.npg.wt2t.ui.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.BookingAdjustmentDirection
import net.npg.wt2t.data.model.AppSettings
import net.npg.wt2t.domain.service.MockConfigRepository
import net.npg.wt2t.domain.service.MockDailyWorkTimeRepository
import net.npg.wt2t.domain.service.MockDayRepository
import net.npg.wt2t.domain.service.MockProjectRepository
import net.npg.wt2t.domain.service.MockTaskRepository
import net.npg.wt2t.domain.service.MockTimeRepository
import net.npg.wt2t.domain.usecase.BookingUseCase
import net.npg.wt2t.domain.usecase.ReportingUseCase
import net.npg.wt2t.domain.service.ReportData
import net.npg.wt2t.domain.service.ReportService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class BookingViewModelTest {

    @Test
    fun `end of day is disabled without a booking for today`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val clock = FixedClock(Instant.parse("2023-10-27T12:00:00Z"))
            val inputOtherDate = LocalDate(2023, 10, 26)
            val viewModel = createViewModel(
                clock = clock,
                times = listOf(createTime("other-day", "task", inputOtherDate.toString(), "09:00")),
            )

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.canEndDay)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `end of day is enabled by a completed booking for today`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val clock = FixedClock(Instant.parse("2023-10-27T12:00:00Z"))
            val inputDate = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val inputTime = Time(
                id = "today",
                taskId = "task",
                date = inputDate,
                start = LocalTime(9, 0),
                end = LocalTime(10, 0),
            )
            val viewModel = createViewModel(clock, listOf(inputTime))

            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.canEndDay)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `stopping a same-minute booking requests a zero-minute confirmation`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val clock = FixedClock(Instant.parse("2026-08-29T14:37:55Z"))
            val inputDateTime = clock.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val inputDate = inputDateTime.date
            val inputStart = LocalTime(inputDateTime.time.hour, inputDateTime.time.minute)
            val activeBooking = Time(
                id = "active",
                taskId = "task",
                date = inputDate,
                start = inputStart,
                description = listOf("Quick note"),
            )
            val testContext = createViewModelContext(clock, listOf(activeBooking), adjustmentMinutes = 5)

            advanceUntilIdle()
            testContext.viewModel.stopBooking()
            advanceUntilIdle()

            assertTrue(testContext.viewModel.uiState.value.isZeroMinuteBookingSaved)
            assertEquals(inputStart, testContext.timeRepository.times.value.single().end)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `overtime summary updates only through an explicit refresh`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val clock = FixedClock(Instant.parse("2026-08-29T12:00:00Z"))
            val inputDate = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val testContext = createViewModelContext(
                clock = clock,
                times = listOf(
                    Time(id = "work", taskId = "task", date = inputDate, start = LocalTime(9, 0), end = LocalTime(10, 0)),
                ),
                adjustmentMinutes = 5,
                dailyWorkTimes = listOf(DailyWorkTime(date = inputDate, minutes = 120)),
            )

            advanceUntilIdle()
            assertEquals((-60).minutes, testContext.viewModel.uiState.value.monthOvertime)
            assertEquals((-60).minutes, testContext.viewModel.uiState.value.yearOvertime)

            testContext.dailyWorkTimeRepository.saveDailyWorkTime(
                DailyWorkTime(date = inputDate, minutes = 30),
            )
            assertEquals((-60).minutes, testContext.viewModel.uiState.value.monthOvertime)

            testContext.viewModel.refreshOvertime()
            advanceUntilIdle()
            assertEquals(30.minutes, testContext.viewModel.uiState.value.monthOvertime)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `tasks are ordered by latest booking first`() {
        val inputTasks = listOf(
            Task(id = "never-used", name = "Never used"),
            Task(id = "older", name = "Older"),
            Task(id = "latest", name = "Latest"),
        )
        val inputTimes = listOf(
            createTime(id = "older-first", taskId = "older", date = "2026-06-19", start = "15:00"),
            createTime(id = "latest", taskId = "latest", date = "2026-06-20", start = "08:00"),
            createTime(id = "older-last", taskId = "older", date = "2026-06-19", start = "16:00"),
        )

        val actualTasks = sortTasksByLastUsed(inputTasks, inputTimes)

        assertEquals(listOf("latest", "older", "never-used"), actualTasks.map(Task::id))
    }

    @Test
    fun `latest start time determines order for bookings on the same date`() {
        val inputTasks = listOf(
            Task(id = "morning", name = "Morning"),
            Task(id = "afternoon", name = "Afternoon"),
        )
        val inputTimes = listOf(
            createTime(id = "morning", taskId = "morning", date = "2026-06-20", start = "08:00"),
            createTime(id = "afternoon", taskId = "afternoon", date = "2026-06-20", start = "14:00"),
        )

        val actualTasks = sortTasksByLastUsed(inputTasks, inputTimes)

        assertEquals(listOf("afternoon", "morning"), actualTasks.map(Task::id))
    }

    @Test
    fun `adjustment availability respects the configured full step`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val clock = FixedClock(Instant.parse("2026-08-22T10:04:59Z"))
            val currentDateTime = clock.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val inputDate = currentDateTime.date
            val activeTime = Time(
                id = "active",
                taskId = "task",
                date = inputDate,
                start = LocalTime.fromSecondOfDay(currentDateTime.time.toSecondOfDay() - 299),
            )
            val viewModel = createViewModel(clock, listOf(activeTime), adjustmentMinutes = 5)

            advanceUntilIdle()

            assertEquals(5, viewModel.uiState.value.bookingStartAdjustmentMinutes)
            assertTrue(viewModel.uiState.value.canAdjustBookingStartEarlier)
            assertFalse(viewModel.uiState.value.canAdjustBookingStartLater)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `earlier adjustment is unavailable when the previous booking would be too short`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val clock = FixedClock(Instant.parse("2026-08-22T11:00:00Z"))
            val inputDate = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val inputTimes = listOf(
                Time(
                    id = "previous",
                    taskId = "previous-task",
                    date = inputDate,
                    start = LocalTime(9, 0),
                    end = LocalTime(9, 5),
                ),
                Time(
                    id = "active",
                    taskId = "active-task",
                    date = inputDate,
                    start = LocalTime(9, 5),
                ),
            )
            val viewModel = createViewModel(clock, inputTimes, adjustmentMinutes = 5)

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.canAdjustBookingStartEarlier)
            assertTrue(viewModel.uiState.value.canAdjustBookingStartLater)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `adjust booking start persists the change immediately`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val clock = FixedClock(Instant.parse("2026-08-22T11:00:00Z"))
            val inputDate = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val activeTime = Time(
                id = "active",
                taskId = "task",
                date = inputDate,
                start = LocalTime(10, 0),
            )
            val testContext = createViewModelContext(clock, listOf(activeTime), adjustmentMinutes = 5)

            advanceUntilIdle()
            testContext.viewModel.adjustBookingStart(BookingAdjustmentDirection.EARLIER)
            advanceUntilIdle()

            assertEquals(LocalTime(9, 55), testContext.timeRepository.times.value.single().start)
            assertFalse(testContext.viewModel.uiState.value.isAdjustingBookingStart)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createTime(
        id: String,
        taskId: String,
        date: String,
        start: String,
    ) = Time(
        id = id,
        taskId = taskId,
        date = LocalDate.parse(date),
        start = LocalTime.parse(start),
    )

    private suspend fun createViewModel(
        clock: Clock,
        times: List<Time>,
        adjustmentMinutes: Int = 5,
    ): BookingViewModel = createViewModelContext(clock, times, adjustmentMinutes).viewModel

    private suspend fun createViewModelContext(
        clock: Clock,
        times: List<Time>,
        adjustmentMinutes: Int,
        dailyWorkTimes: List<DailyWorkTime> = emptyList(),
    ): BookingViewModelTestContext {
        val timeRepository = MockTimeRepository().apply { this.times.value = times }
        val dailyWorkTimeRepository = MockDailyWorkTimeRepository().apply { this.dailyWorkTimes.value = dailyWorkTimes }
        val configRepository = MockConfigRepository()
        val taskRepository = MockTaskRepository()
        val projectRepository = MockProjectRepository()
        configRepository.setInt(AppSettings.BOOKING_START_ADJUSTMENT_MINUTES.name, adjustmentMinutes)
        val viewModel = BookingViewModel(
            bookingUseCase = BookingUseCase(timeRepository, taskRepository, projectRepository, configRepository, clock),
            reportingUseCase = ReportingUseCase(
                timeRepository,
                dailyWorkTimeRepository,
                taskRepository,
                projectRepository,
                FakeReportService(),
                clock,
            ),
            clock = clock,
        )
        return BookingViewModelTestContext(viewModel, timeRepository, dailyWorkTimeRepository)
    }

    private class FixedClock(private val instant: Instant) : Clock {
        override fun now(): Instant = instant
    }
}

private class FakeReportService : ReportService {
    override suspend fun generatePdfReport(data: ReportData, fileName: String): String = fileName
}

private data class BookingViewModelTestContext(
    val viewModel: BookingViewModel,
    val timeRepository: MockTimeRepository,
    val dailyWorkTimeRepository: MockDailyWorkTimeRepository,
)
