package net.npg.wt2t.domain.usecase

import kotlinx.datetime.LocalDate
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.AppSettings
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.data.repository.ConfigRepository
import net.npg.wt2t.data.repository.DailyWorkTimeRepository
import net.npg.wt2t.data.repository.DayRepository
import net.npg.wt2t.data.repository.ProjectRepository
import net.npg.wt2t.data.repository.TaskRepository
import net.npg.wt2t.data.repository.TimeRepository
import kotlinx.coroutines.flow.first

/**
 * Replaces all time entries and the target work time for one day.
 *
 * @property storageService Service used to load and persist day data.
 */
class DayEditingUseCase(
    private val dayRepository: DayRepository,
    private val timeRepository: TimeRepository,
    private val dailyWorkTimeRepository: DailyWorkTimeRepository,
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val configRepository: ConfigRepository,
) {
    /** Creates an empty day using the configured default target time. */
    suspend fun createEmptyDay(date: LocalDate): DailyWorkTime {
        val targetMinutes = getInt(AppSettings.DEFAULT_TARGET_HOURS) * 60 +
            getInt(AppSettings.DEFAULT_TARGET_MINUTES)
        return dayRepository.createEmptyDay(
            date = date,
            targetMinutes = targetMinutes,
            breakMinutes = 0,
        )
    }

    suspend fun load(date: LocalDate): DayEditingData = DayEditingData(
        times = timeRepository.getTimesForDate(date).first(),
        dailyWorkTime = dailyWorkTimeRepository.getDailyWorkTime(date).first(),
        tasks = taskRepository.getAllTasks().first(),
        projects = projectRepository.getAllProjects().first().associateBy(Project::id),
        bookingStartAdjustmentMinutes = AppSettings.BOOKING_START_ADJUSTMENT_MINUTES.parseIntOrDefault(
            configRepository.getString(
                AppSettings.BOOKING_START_ADJUSTMENT_MINUTES.name,
                AppSettings.BOOKING_START_ADJUSTMENT_MINUTES.defaultValue,
            ),
        ),
    )

    /**
     * Validates and persists the full editable state for a single day.
     *
     * @param request Day update request.
     * @throws IllegalArgumentException when the edited day contains invalid or overlapping entries.
     */
    suspend fun execute(request: UpdateDayRequest): UpdateDayResult {
        validateRequest(request)

        val sortedTimes = request.times.sortedBy(Time::start)
        val dailyWorkTime = dayRepository.replaceDay(
            date = request.date,
            targetMinutes = request.targetMinutes,
            breakMinutes = request.breakMinutes,
            times = sortedTimes,
        )

        return UpdateDayResult(
            date = request.date,
            times = sortedTimes,
            dailyWorkTime = dailyWorkTime,
        )
    }

    suspend fun delete(date: LocalDate) {
        dayRepository.deleteDay(date)
    }

    private suspend fun getInt(setting: AppSettings): Int =
        setting.parseIntOrDefault(
            configRepository.getString(setting.name, setting.defaultValue),
        )

    /** Validates the target time and booking data in the update request. */
    private fun validateRequest(request: UpdateDayRequest) {
        require(request.targetMinutes >= MINUTES_PER_DAY_RANGE.first) { "Target minutes must not be negative." }
        require(request.targetMinutes <= MINUTES_PER_DAY_RANGE.last) { "Target minutes must not exceed one day." }
        require(request.breakMinutes >= MINUTES_PER_DAY_RANGE.first) { "Break minutes must not be negative." }
        require(request.breakMinutes <= MINUTES_PER_DAY_RANGE.last) { "Break minutes must not exceed one day." }
        require(request.times.all { it.date == request.date }) { "All time entries must belong to the edited date." }
        require(request.times.all { it.end != null && it.start <= it.end }) {
            "Each time entry must not start after its end time."
        }
        require(request.times.none { it.taskId.isBlank() }) { "Each time entry must have a task." }
        require(!hasOverlappingTimes(request.times)) { "Time entries must not overlap." }
    }

    /** Returns whether any bookings overlap. */
    private fun hasOverlappingTimes(times: List<Time>): Boolean {
        val sortedTimes = times
            .filterNot { it.start == it.end }
            .sortedBy(Time::start)
        return sortedTimes
            .zipWithNext()
            .any { (previous, next) -> previous.end != null && previous.end > next.start }
    }

    private companion object {
        val MINUTES_PER_DAY_RANGE = 0..1_440
    }
}

data class DayEditingData(
    val times: List<Time>,
    val dailyWorkTime: DailyWorkTime?,
    val tasks: List<Task>,
    val projects: Map<String, Project>,
    val bookingStartAdjustmentMinutes: Int,
)

/**
 * Complete editable data for a day.
 *
 * @property date Date being edited.
 * @property targetMinutes Target work time for the day in minutes.
 * @property times Final list of time entries for the day.
 * @property breakMinutes Break time for the day in minutes.
 */
data class UpdateDayRequest(
    val date: LocalDate,
    val targetMinutes: Int,
    val times: List<Time>,
    val breakMinutes: Int = 0,
)

/**
 * Persisted result of a day update.
 *
 * @property date Updated date.
 * @property times Persisted time entries sorted chronologically.
 * @property dailyWorkTime Persisted target work time.
 */
data class UpdateDayResult(
    val date: LocalDate,
    val times: List<Time>,
    val dailyWorkTime: DailyWorkTime,
)
