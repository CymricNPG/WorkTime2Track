package net.npg.wt2t.ui.viewmodel

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.npg.wt2t.data.model.AppSettings
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.data.repository.DayRepository
import net.npg.wt2t.domain.service.MockConfigRepository
import net.npg.wt2t.domain.service.MockDailyWorkTimeRepository
import net.npg.wt2t.domain.service.MockDayRepository
import net.npg.wt2t.domain.service.MockProjectRepository
import net.npg.wt2t.domain.service.MockTaskRepository
import net.npg.wt2t.domain.service.MockTimeRepository
import net.npg.wt2t.domain.usecase.EndOfDayUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class EndOfDayViewModelTest {
    private lateinit var testDispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `configured target is loaded into UI state`() = runTest(testDispatcher.scheduler) {
        val testContext = createTestContext()
        testContext.configRepository.setInt(AppSettings.DEFAULT_TARGET_HOURS.name, 7)
        testContext.configRepository.setInt(AppSettings.DEFAULT_TARGET_MINUTES.name, 30)

        val viewModel = testContext.createViewModel()

        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isLoaded)
        assertEquals(450, viewModel.uiState.value.targetMinutes)
        assertEquals(0, viewModel.uiState.value.breakMinutes)
    }

    @Test
    fun `existing daily values are loaded instead of configured target`() = runTest(testDispatcher.scheduler) {
        val testContext = createTestContext()
        testContext.configRepository.setInt(AppSettings.DEFAULT_TARGET_HOURS.name, 7)
        testContext.configRepository.setInt(AppSettings.DEFAULT_TARGET_MINUTES.name, 30)
        testContext.dailyWorkTimeRepository.dailyWorkTimes.value = listOf(
            DailyWorkTime(
                id = "existing-day",
                date = testContext.date,
                minutes = 480,
                breakMinutes = 45,
            ),
        )

        val viewModel = testContext.createViewModel()
        advanceUntilIdle()

        assertEquals(480, viewModel.uiState.value.targetMinutes)
        assertEquals(45, viewModel.uiState.value.breakMinutes)
    }

    @Test
    fun `save failure is exposed and does not complete navigation state`() = runTest(testDispatcher.scheduler) {
        val testContext = createTestContext(completionFailure = Exception("Expected failure"))
        testContext.addBooking()
        val viewModel = testContext.createViewModel()
        advanceUntilIdle()

        viewModel.saveAndEndDay()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaving)
        assertFalse(viewModel.uiState.value.isSaved)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `duplicate submissions invoke completion once`() = runTest(testDispatcher.scheduler) {
        val completionRelease = CompletableDeferred<Unit>()
        val testContext = createTestContext(completionRelease = completionRelease)
        testContext.addBooking()
        val viewModel = testContext.createViewModel()
        advanceUntilIdle()

        viewModel.saveAndEndDay()
        viewModel.saveAndEndDay()
        runCurrent()

        assertTrue(viewModel.uiState.value.isSaving)
        assertEquals(1, testContext.dayRepository.completionCount)

        completionRelease.complete(Unit)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `successful completion updates saved state and daily values`() = runTest(testDispatcher.scheduler) {
        val testContext = createTestContext()
        testContext.addBooking()
        val viewModel = testContext.createViewModel()
        advanceUntilIdle()

        viewModel.updateTargetTime(hours = 7, minutes = 0)
        viewModel.updateBreakTime(hours = 0, minutes = 45)
        viewModel.saveAndEndDay()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        assertFalse(viewModel.uiState.value.isSaving)
        val actualDailyWorkTime = testContext.dailyWorkTimeRepository.dailyWorkTimes.value.single()
        assertEquals(420, actualDailyWorkTime.minutes)
        assertEquals(45, actualDailyWorkTime.breakMinutes)
    }

    private fun createTestContext(
        completionFailure: Exception? = null,
        completionRelease: CompletableDeferred<Unit>? = null,
    ): EndOfDayViewModelTestContext {
        val clock = FixedClock(Instant.parse("2023-10-27T12:00:00Z"))
        val date = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val timeRepository = MockTimeRepository()
        val dailyWorkTimeRepository = MockDailyWorkTimeRepository()
        val delegateDayRepository = MockDayRepository(timeRepository, dailyWorkTimeRepository)
        val dayRepository = RecordingDayRepository(
            delegate = delegateDayRepository,
            completionFailure = completionFailure,
            completionRelease = completionRelease,
        )
        val taskRepository = MockTaskRepository()
        val configRepository = MockConfigRepository()
        return EndOfDayViewModelTestContext(
            date = date,
            clock = clock,
            timeRepository = timeRepository,
            dailyWorkTimeRepository = dailyWorkTimeRepository,
            dayRepository = dayRepository,
            taskRepository = taskRepository,
            configRepository = configRepository,
        )
    }

    private class FixedClock(private val instant: Instant) : Clock {
        override fun now(): Instant = instant
    }
}

private data class EndOfDayViewModelTestContext(
    val date: LocalDate,
    val clock: Clock,
    val timeRepository: MockTimeRepository,
    val dailyWorkTimeRepository: MockDailyWorkTimeRepository,
    val dayRepository: RecordingDayRepository,
    val taskRepository: MockTaskRepository,
    val configRepository: MockConfigRepository,
) {
    fun addBooking() {
        timeRepository.times.value = listOf(
            Time(
                id = "active-time",
                taskId = "task",
                date = date,
                start = LocalTime(9, 0),
            ),
        )
    }

    fun createViewModel(): EndOfDayViewModel {
        return EndOfDayViewModel(
            endOfDayUseCase = EndOfDayUseCase(dayRepository, timeRepository, dailyWorkTimeRepository, configRepository, taskRepository),
            clock = clock,
        )
    }
}

private class RecordingDayRepository(
    private val delegate: DayRepository,
    private val completionFailure: Exception?,
    private val completionRelease: CompletableDeferred<Unit>?,
) : DayRepository by delegate {
    var completionCount = 0
        private set

    override suspend fun completeDay(
        date: LocalDate,
        endTime: LocalTime,
        targetMinutes: Int,
        breakMinutes: Int,
    ): DailyWorkTime {
        completionCount += 1
        completionRelease?.await()
        completionFailure?.let { throw it }
        return delegate.completeDay(date, endTime, targetMinutes, breakMinutes)
    }
}
