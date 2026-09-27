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
import net.npg.wt2t.data.model.AppSettings
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.data.model.BookingAdjustmentDirection
import net.npg.wt2t.domain.service.MockConfigRepository
import net.npg.wt2t.domain.service.MockDailyWorkTimeRepository
import net.npg.wt2t.domain.service.MockDayRepository
import net.npg.wt2t.domain.service.MockProjectRepository
import net.npg.wt2t.domain.service.MockTaskRepository
import net.npg.wt2t.domain.service.MockTimeRepository
import net.npg.wt2t.domain.usecase.DayEditingUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EditDayViewModelTest {

    @Test
    fun `configured adjustment updates only the editable draft`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val inputDate = LocalDate.parse("2026-08-18")
            val inputTask = Task(id = "task", name = "Task")
            val inputTime = Time(
                id = "time",
                taskId = inputTask.id,
                date = inputDate,
                start = LocalTime.parse("09:00:30"),
                end = LocalTime.parse("10:00:45"),
            )
            val configRepository = MockConfigRepository()
            val taskRepository = MockTaskRepository().also { it.tasks.value = listOf(inputTask) }
            val timeRepository = MockTimeRepository().also { it.times.value = listOf(inputTime) }
            val dailyWorkTimeRepository = MockDailyWorkTimeRepository()
            val projectRepository = MockProjectRepository()
            val dayRepository = MockDayRepository(timeRepository, dailyWorkTimeRepository)
            configRepository.setInt(AppSettings.BOOKING_START_ADJUSTMENT_MINUTES.name, 7)
            val viewModel = EditDayViewModel(
                date = inputDate,
                dayEditingUseCase = DayEditingUseCase(dayRepository, timeRepository, dailyWorkTimeRepository, taskRepository, projectRepository, configRepository),
            )
            advanceUntilIdle()

            assertEquals("09:00", viewModel.uiState.value.entries.single().start)
            assertEquals("10:00", viewModel.uiState.value.entries.single().end)

            viewModel.adjustEntryTime(
                entryId = inputTime.id,
                boundary = EditableTimeBoundary.Start,
                direction = BookingAdjustmentDirection.LATER,
            )

            assertEquals(7, viewModel.uiState.value.timeAdjustmentMinutes)
            assertEquals("09:07", viewModel.uiState.value.entries.single().start)
            assertTrue(viewModel.uiState.value.hasChanges)
            assertEquals(LocalTime.parse("09:00:30"), timeRepository.times.value.single().start)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `projects are loaded for task labels`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val inputDate = LocalDate.parse("2026-08-18")
            val inputProject = Project(id = "project", name = "WorkTime2Track")
            val timeRepository = MockTimeRepository()
            val dailyWorkTimeRepository = MockDailyWorkTimeRepository()
            val projectRepository = MockProjectRepository()
            val taskRepository = MockTaskRepository()
            val configRepository = MockConfigRepository()
            val dayRepository = MockDayRepository(timeRepository, dailyWorkTimeRepository)
            projectRepository.saveProject(inputProject)

            val viewModel = EditDayViewModel(
                date = inputDate,
                dayEditingUseCase = DayEditingUseCase(dayRepository, timeRepository, dailyWorkTimeRepository, taskRepository, projectRepository, configRepository),
            )

            advanceUntilIdle()

            assertEquals(inputProject, viewModel.uiState.value.projects[inputProject.id])
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `break time is loaded edited and saved`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val inputDate = LocalDate.parse("2026-08-18")
            val dailyWorkTimeRepository = MockDailyWorkTimeRepository()
            val timeRepository = MockTimeRepository()
            val projectRepository = MockProjectRepository()
            val taskRepository = MockTaskRepository()
            val configRepository = MockConfigRepository()
            val dayRepository = MockDayRepository(timeRepository, dailyWorkTimeRepository)
            dailyWorkTimeRepository.saveDailyWorkTime(
                DailyWorkTime(
                    date = inputDate,
                    minutes = 480,
                    breakMinutes = 30,
                ),
            )
            val viewModel = EditDayViewModel(
                date = inputDate,
                dayEditingUseCase = DayEditingUseCase(dayRepository, timeRepository, dailyWorkTimeRepository, taskRepository, projectRepository, configRepository),
            )

            advanceUntilIdle()

            assertEquals("0", viewModel.uiState.value.breakHours)
            assertEquals("30", viewModel.uiState.value.breakMinutes)

            viewModel.updateBreakMinutes("45")
            viewModel.saveDay()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isSaved)
            assertEquals(45, dailyWorkTimeRepository.getDailyWorkTime(inputDate).first()?.breakMinutes)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
