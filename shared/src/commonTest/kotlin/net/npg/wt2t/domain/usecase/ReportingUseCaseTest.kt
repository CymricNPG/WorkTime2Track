package net.npg.wt2t.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.data.repository.DailyWorkTimeRepository
import net.npg.wt2t.data.repository.ProjectRepository
import net.npg.wt2t.data.repository.TaskRepository
import net.npg.wt2t.data.repository.TimeRepository
import net.npg.wt2t.domain.service.PdfExportRequest
import net.npg.wt2t.domain.service.ReportData
import net.npg.wt2t.domain.service.ReportService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class ReportingUseCaseTest {
    @Test
    fun `loading a large period uses one bounded snapshot per repository`() = runTest {
        val inputStartDate = LocalDate(2026, 1, 1)
        val times = (0 until 365).map { offset ->
            Time(
                id = "time-$offset",
                taskId = "task",
                date = inputStartDate.plus(offset, kotlinx.datetime.DateTimeUnit.DAY),
                start = LocalTime(9, 0),
                end = LocalTime(10, 0),
            )
        }
        val timeRepository = CountingTimeRepository(times)
        val dailyWorkTimeRepository = CountingDailyWorkTimeRepository(emptyList())
        val taskRepository = CountingTaskRepository(listOf(Task(id = "task", name = "Task")))
        val projectRepository = CountingProjectRepository(emptyList())
        val useCase = ReportingUseCase(
            timeRepository,
            dailyWorkTimeRepository,
            taskRepository,
            projectRepository,
            FakeReportService(),
            Clock.System,
        )

        val actualDays = useCase.loadOverview(ReportPeriod(inputStartDate, LocalDate(2026, 12, 31)))

        assertEquals(365, actualDays.size)
        assertEquals(1, timeRepository.allTimesLoadCount)
        assertEquals(1, dailyWorkTimeRepository.allDailyWorkTimesLoadCount)
        assertEquals(1, taskRepository.allTasksLoadCount)
        assertEquals(1, projectRepository.allProjectsLoadCount)
    }

    @Test
    fun `zero-minute booking is reported with notes but adds no worked time`() = runTest {
        val inputDate = LocalDate(2026, 8, 29)
        val useCase = ReportingUseCase(
            CountingTimeRepository(
                listOf(
                    Time(
                        id = "note",
                        taskId = "task",
                        date = inputDate,
                        start = LocalTime(14, 37),
                        end = LocalTime(14, 37),
                        description = listOf("Quick note"),
                    ),
                ),
            ),
            CountingDailyWorkTimeRepository(emptyList()),
            CountingTaskRepository(listOf(Task(id = "task", name = "Task"))),
            CountingProjectRepository(emptyList()),
            FakeReportService(),
            Clock.System,
        )

        val actualDay = useCase.loadOverview(ReportPeriod(inputDate, inputDate)).single()

        assertEquals(0, actualDay.totalWorkedTime.inWholeMinutes)
        assertEquals(listOf("Quick note"), actualDay.items.single().notes)
        assertEquals("00:00", actualDay.items.single().duration)
    }

    @Test
    fun `current overtime summary includes the full month and year`() = runTest {
        val currentDate = LocalDate(2026, 8, 29)
        val useCase = ReportingUseCase(
            CountingTimeRepository(
                listOf(
                    Time(id = "august-work", taskId = "task", date = LocalDate(2026, 8, 5), start = LocalTime(9, 0), end = LocalTime(11, 0)),
                    Time(id = "august-future", taskId = "task", date = LocalDate(2026, 8, 30), start = LocalTime(9, 0), end = LocalTime(10, 0)),
                    Time(id = "december-work", taskId = "task", date = LocalDate(2026, 12, 1), start = LocalTime(9, 0), end = LocalTime(10, 0)),
                ),
            ),
            CountingDailyWorkTimeRepository(
                listOf(
                    DailyWorkTime(date = LocalDate(2026, 8, 5), minutes = 60),
                    DailyWorkTime(date = LocalDate(2026, 12, 1), minutes = 120),
                ),
            ),
            CountingTaskRepository(listOf(Task(id = "task", name = "Task"))),
            CountingProjectRepository(emptyList()),
            FakeReportService(),
            Clock.System,
        )

        val actualSummary = useCase.loadCurrentOvertime(currentDate)

        assertEquals(120, actualSummary.month.inWholeMinutes)
        assertEquals(60, actualSummary.year.inWholeMinutes)
    }
}

private class CountingTimeRepository(private val values: List<Time>) : TimeRepository {
    var allTimesLoadCount = 0

    override fun getTimesForDate(date: LocalDate): Flow<List<Time>> = flowOf(values.filter { it.date == date })
    override fun getAllTimes(): Flow<List<Time>> = flowOf(values).also { allTimesLoadCount++ }
    override suspend fun saveTime(time: Time) = Unit
    override suspend fun switchBooking(taskId: String, date: LocalDate, startTime: LocalTime, mergeThresholdMinutes: Int): Time = error("Not used")
    override suspend fun endActiveBooking(date: LocalDate, endTime: LocalTime): Time? = null
    override suspend fun adjustActiveBookingStart(direction: net.npg.wt2t.data.model.BookingAdjustmentDirection, date: LocalDate, adjustmentMinutes: Int, currentTime: LocalTime): Time = error("Not used")
    override suspend fun deleteTime(id: String) = Unit
    override suspend fun replaceTimesForDate(date: LocalDate, times: List<Time>) = Unit
}

private class CountingDailyWorkTimeRepository(private val values: List<DailyWorkTime>) : DailyWorkTimeRepository {
    var allDailyWorkTimesLoadCount = 0

    override fun getDailyWorkTime(date: LocalDate): Flow<DailyWorkTime?> = flowOf(values.find { it.date == date })
    override fun getAllDailyWorkTimes(): Flow<List<DailyWorkTime>> = flowOf(values).also { allDailyWorkTimesLoadCount++ }
    override suspend fun saveDailyWorkTime(dailyWorkTime: DailyWorkTime) = Unit
    override suspend fun deleteAllDailyWorkTimes() = Unit
}

private class CountingTaskRepository(private val values: List<Task>) : TaskRepository {
    var allTasksLoadCount = 0

    override fun getAllTasks(): Flow<List<Task>> = flowOf(values).also { allTasksLoadCount++ }
    override fun getTasksByProject(projectId: String?): Flow<List<Task>> = flowOf(values.filter { it.projectId == projectId })
    override suspend fun saveTask(task: Task) = Unit
    override suspend fun deleteTask(id: String) = Unit
}

private class CountingProjectRepository(private val values: List<Project>) : ProjectRepository {
    var allProjectsLoadCount = 0

    override fun getAllProjects(): Flow<List<Project>> = flowOf(values).also { allProjectsLoadCount++ }
    override fun getProjectById(id: String): Flow<Project?> = flowOf(values.find { it.id == id })
    override suspend fun saveProject(project: Project) = Unit
    override suspend fun deleteProject(id: String) = Unit
}

private class FakeReportService : ReportService {
    override suspend fun generatePdfReport(data: ReportData, fileName: String): String = fileName
}
