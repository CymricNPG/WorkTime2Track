package net.npg.wt2t.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toLocalDateTime
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.data.model.atMinutePrecision
import net.npg.wt2t.data.repository.DailyWorkTimeRepository
import net.npg.wt2t.data.repository.ProjectRepository
import net.npg.wt2t.data.repository.TaskRepository
import net.npg.wt2t.data.repository.TimeRepository
import net.npg.wt2t.domain.service.PdfExportRequest
import net.npg.wt2t.domain.service.ReportData
import net.npg.wt2t.domain.service.ReportItem
import net.npg.wt2t.domain.service.ReportService
import net.npg.wt2t.domain.service.ReportText
import net.npg.wt2t.ui.format.formatForDisplay
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Loads a bounded overview snapshot and creates report exports. */
class ReportingUseCase(
    private val timeRepository: TimeRepository,
    private val dailyWorkTimeRepository: DailyWorkTimeRepository,
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val reportService: ReportService,
    private val clock: Clock,
) {
    suspend fun loadDefaultPeriod(today: LocalDate): ReportPeriod {
        val dates = loadRecordedDates()
        return ReportPeriod(dates.minOrNull() ?: today, dates.maxOrNull() ?: today)
    }

    /** Returns every date that has bookings or stored daily values. */
    suspend fun loadRecordedDates(): Set<LocalDate> =
        timeRepository.getAllTimes().first().map(Time::date).toSet() +
            dailyWorkTimeRepository.getAllDailyWorkTimes().first().map(DailyWorkTime::date)

    suspend fun loadOverview(period: ReportPeriod): List<ReportDay> {
        val times = timeRepository.getAllTimes().first()
        val dailyWorkTimes = dailyWorkTimeRepository.getAllDailyWorkTimes().first().associateBy(DailyWorkTime::date)
        val tasks = taskRepository.getAllTasks().first().associateBy(Task::id)
        val projects = projectRepository.getAllProjects().first().associateBy(Project::id)
        val today = currentDate()
        return (times.map(Time::date) + dailyWorkTimes.keys)
            .asSequence()
            .filter { it in period.startDate..period.endDate }
            .distinct()
            .sortedDescending()
            .map { date -> createDay(date, times.filter { it.date == date }, dailyWorkTimes[date], tasks, projects, today) }
            .toList()
    }

    /** Loads the overtime totals for the complete current calendar month and year. */
    suspend fun loadCurrentOvertime(today: LocalDate): OvertimeSummary {
        val yearDays = loadOverview(
            ReportPeriod(
                startDate = LocalDate(today.year, 1, 1),
                endDate = LocalDate(today.year, 12, 31),
            ),
        )
        return OvertimeSummary(
            month = yearDays
                .filter { it.date.month == today.month }
                .fold(Duration.ZERO) { total, day -> total + day.overtime },
            year = yearDays.fold(Duration.ZERO) { total, day -> total + day.overtime },
        )
    }

    fun createPdfExportRequest(period: ReportPeriod, days: List<ReportDay>, reportText: ReportText): PdfExportRequest {
        val items = days.flatMap(ReportDay::items).map { item ->
            if (item.task == "-") item.copy(task = reportText.unknownTask) else item
        }
        return PdfExportRequest(
            data = ReportData(
                startDate = period.startDate,
                endDate = period.endDate,
                items = items,
                totalDuration = formatDuration(days.sumOf { it.totalWorkedTime.inWholeMinutes }),
                overtime = formatDuration(days.sumOf { it.overtime.inWholeMinutes }),
                text = reportText,
            ),
            fileName = "report_${period.startDate}_${period.endDate}.pdf",
        )
    }

    suspend fun generatePdfReport(request: PdfExportRequest): String =
        reportService.generatePdfReport(request.data, request.fileName)

    private fun createDay(
        date: LocalDate,
        times: List<Time>,
        dailyWorkTime: DailyWorkTime?,
        tasks: Map<String, Task>,
        projects: Map<String, Project>,
        today: LocalDate,
    ): ReportDay {
        val timeEntries = times.groupBy(Time::taskId).map { (taskId, taskTimes) ->
            val task = tasks[taskId]
            ReportTaskEntry(task?.name, task?.projectId?.let { projects[it]?.name }, taskTimes.sumOfDuration { duration(it, date == today) })
        }
        val totalWorkedTime = times.filter { !tasks[it.taskId].orFreeTime() }.sumOfDuration { duration(it, date == today) }
        val breakTime = (dailyWorkTime?.breakMinutes ?: 0).minutes
        val overtime = totalWorkedTime - breakTime - (dailyWorkTime?.minutes ?: 0).minutes
        return ReportDay(
            date = date,
            totalWorkedTime = totalWorkedTime,
            overtime = overtime,
            breakTime = breakTime,
            taskEntries = timeEntries,
            items = times.sortedBy(Time::start).map { time ->
                val task = tasks[time.taskId]
                val end = time.end ?: if (date == today) currentTime() else time.start
                ReportItem(date, task?.projectId?.let { projects[it]?.name } ?: "-", task?.name ?: "-", time.start.formatForDisplay("de"), end.formatForDisplay("de"), formatDuration(duration(time, date == today).inWholeMinutes), time.description)
            },
        )
    }

    private fun Task?.orFreeTime(): Boolean = this?.freeTime ?: false
    private fun duration(time: Time, isToday: Boolean): Duration =
        ((time.end ?: if (isToday) currentTime() else time.start).toSecondOfDay() - time.start.toSecondOfDay()).div(60).minutes
    private fun <T> Iterable<T>.sumOfDuration(selector: (T) -> Duration): Duration = fold(Duration.ZERO) { sum, item -> sum + selector(item) }
    private fun currentDate(): LocalDate = clock.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
    private fun currentTime(): LocalTime = clock.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).time.atMinutePrecision()
    private fun formatDuration(minutes: Long): String { val absolute = minutes.absoluteValue; return (if (minutes < 0) "-" else "") + "%02d:%02d".format(absolute / 60, absolute % 60) }
}

data class ReportPeriod(val startDate: LocalDate, val endDate: LocalDate)
data class OvertimeSummary(val month: Duration, val year: Duration)
data class ReportDay(val date: LocalDate, val totalWorkedTime: Duration, val overtime: Duration, val breakTime: Duration, val taskEntries: List<ReportTaskEntry>, val items: List<ReportItem>)
data class ReportTaskEntry(val taskName: String?, val projectName: String?, val duration: Duration)
