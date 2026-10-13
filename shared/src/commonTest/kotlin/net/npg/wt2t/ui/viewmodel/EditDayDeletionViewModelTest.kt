package net.npg.wt2t.ui.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.data.repository.DayRepository
import net.npg.wt2t.domain.service.MockConfigRepository
import net.npg.wt2t.domain.service.MockDailyWorkTimeRepository
import net.npg.wt2t.domain.service.MockDayRepository
import net.npg.wt2t.domain.service.MockProjectRepository
import net.npg.wt2t.domain.service.MockTaskRepository
import net.npg.wt2t.domain.service.MockTimeRepository
import net.npg.wt2t.domain.usecase.DayEditingUseCase
import worktime2track.shared.generated.resources.Res
import worktime2track.shared.generated.resources.edit_day_delete_failed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EditDayDeletionViewModelTest {
    @Test
    fun `day becomes deletable only after loading with no draft entries`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val context = createTestContext()
            val inputTask = Task(id = "task", name = "Task")
            context.taskRepository.tasks.value = listOf(inputTask)
            val inputTime = Time(
                id = "time",
                taskId = inputTask.id,
                date = context.date,
                start = LocalTime.parse("09:00"),
                end = LocalTime.parse("10:00"),
            )
            context.timeRepository.times.value = listOf(inputTime)
            val viewModel = context.createViewModel()

            assertFalse(viewModel.uiState.value.canDeleteDay)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.canDeleteDay)
            viewModel.deleteDay()
            advanceUntilIdle()
            assertEquals(0, context.dayRepository.deleteCount)

            viewModel.deleteEntry(inputTime.id)

            assertTrue(viewModel.uiState.value.canDeleteDay)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `duplicate delete requests execute once and mark day deleted`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val context = createTestContext()
            context.dailyWorkTimeRepository.dailyWorkTimes.value = listOf(
                DailyWorkTime(date = context.date, minutes = 480),
            )
            val viewModel = context.createViewModel()
            advanceUntilIdle()

            viewModel.deleteDay()
            viewModel.deleteDay()

            assertTrue(viewModel.uiState.value.isDeleting)
            advanceUntilIdle()
            assertEquals(1, context.dayRepository.deleteCount)
            assertTrue(viewModel.uiState.value.isDeleted)
            assertFalse(viewModel.uiState.value.isDeleting)
            assertNull(context.dailyWorkTimeRepository.getDailyWorkTime(context.date).first())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `delete failure keeps day and exposes error state`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val context = createTestContext(deleteFailure = IllegalStateException("failed"))
            val dailyWorkTime = DailyWorkTime(date = context.date, minutes = 480)
            context.dailyWorkTimeRepository.dailyWorkTimes.value = listOf(dailyWorkTime)
            val viewModel = context.createViewModel()
            advanceUntilIdle()

            viewModel.deleteDay()
            advanceUntilIdle()

            assertEquals(1, context.dayRepository.deleteCount)
            assertFalse(viewModel.uiState.value.isDeleted)
            assertFalse(viewModel.uiState.value.isDeleting)
            assertEquals(Res.string.edit_day_delete_failed, viewModel.uiState.value.errorMessage)
            assertEquals(dailyWorkTime, context.dailyWorkTimeRepository.getDailyWorkTime(context.date).first())
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createTestContext(deleteFailure: Exception? = null): TestContext {
        val timeRepository = MockTimeRepository()
        val dailyWorkTimeRepository = MockDailyWorkTimeRepository()
        val taskRepository = MockTaskRepository()
        val delegate = MockDayRepository(timeRepository, dailyWorkTimeRepository)
        val dayRepository = RecordingDayRepository(delegate, deleteFailure)
        return TestContext(
            date = LocalDate.parse("2026-08-18"),
            timeRepository = timeRepository,
            dailyWorkTimeRepository = dailyWorkTimeRepository,
            taskRepository = taskRepository,
            dayRepository = dayRepository,
            projectRepository = MockProjectRepository(),
            configRepository = MockConfigRepository(),
        )
    }

    private class RecordingDayRepository(
        private val delegate: DayRepository,
        private val deleteFailure: Exception?,
    ) : DayRepository by delegate {
        var deleteCount = 0
            private set

        override suspend fun deleteDay(date: LocalDate) {
            deleteCount++
            deleteFailure?.let { throw it }
            delegate.deleteDay(date)
        }
    }

    private data class TestContext(
        val date: LocalDate,
        val timeRepository: MockTimeRepository,
        val dailyWorkTimeRepository: MockDailyWorkTimeRepository,
        val taskRepository: MockTaskRepository,
        val dayRepository: RecordingDayRepository,
        val projectRepository: MockProjectRepository,
        val configRepository: MockConfigRepository,
    ) {
        fun createViewModel() = EditDayViewModel(
            date = date,
            dayEditingUseCase = DayEditingUseCase(dayRepository, timeRepository, dailyWorkTimeRepository, taskRepository, projectRepository, configRepository),
        )
    }
}
