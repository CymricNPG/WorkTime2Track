package net.npg.wt2t.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.npg.wt2t.data.model.AppSettings
import net.npg.wt2t.data.model.BookingAdjustmentDirection
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.data.model.atMinutePrecision
import net.npg.wt2t.data.repository.ConfigRepository
import net.npg.wt2t.data.repository.ProjectRepository
import net.npg.wt2t.data.repository.TaskRepository
import net.npg.wt2t.data.repository.TimeRepository
import net.npg.wt2t.data.repository.calculateBookingStartAdjustment
import kotlin.time.Clock

/** Coordinates booking commands and supplies booking-screen data. */
class BookingUseCase(
    private val timeRepository: TimeRepository,
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val configRepository: ConfigRepository,
    private val clock: Clock,
) {
    fun getAllTimes(): Flow<List<Time>> = timeRepository.getAllTimes()
    fun getTimesForDate(date: LocalDate): Flow<List<Time>> = timeRepository.getTimesForDate(date)
    fun getAllTasks(): Flow<List<Task>> = taskRepository.getAllTasks()
    fun getAllProjects(): Flow<List<Project>> = projectRepository.getAllProjects()

    suspend fun startBooking(taskId: String, date: LocalDate, startTime: LocalTime): Time =
        timeRepository.switchBooking(
            taskId = taskId,
            date = date,
            startTime = startTime.atMinutePrecision(),
            mergeThresholdMinutes = getInt(AppSettings.MERGE_THRESHOLD_MINUTES),
        )

    suspend fun endBooking(date: LocalDate, endTime: LocalTime): Time? =
        timeRepository.endActiveBooking(date, endTime.atMinutePrecision())

    suspend fun adjustBookingStart(
        direction: BookingAdjustmentDirection,
        date: LocalDate,
        currentTime: LocalTime,
    ): Time = timeRepository.adjustActiveBookingStart(
        direction,
        date,
        getInt(AppSettings.BOOKING_START_ADJUSTMENT_MINUTES),
        currentTime.atMinutePrecision(),
    )

    suspend fun getBookingStartAdjustmentMinutes(): Int = getInt(AppSettings.BOOKING_START_ADJUSTMENT_MINUTES)

    fun getBookingAdjustmentAvailability(
        activeTime: Time?,
        times: List<Time>,
        adjustmentMinutes: Int,
        currentTime: LocalTime,
    ): BookingAdjustmentAvailability {
        if (activeTime == null) return BookingAdjustmentAvailability()
        val minuteActiveTime = activeTime.atMinutePrecision()
        val previousTime = times.map(Time::atMinutePrecision)
            .filter { it.id != minuteActiveTime.id && it.date == minuteActiveTime.date && it.end != null && it.end <= minuteActiveTime.start }
            .maxByOrNull { requireNotNull(it.end) }
        return BookingAdjustmentAvailability(
            canAdjustEarlier = canAdjust(minuteActiveTime, previousTime, BookingAdjustmentDirection.EARLIER, adjustmentMinutes, currentTime),
            canAdjustLater = canAdjust(minuteActiveTime, previousTime, BookingAdjustmentDirection.LATER, adjustmentMinutes, currentTime),
        )
    }

    suspend fun addNoteToTime(timeId: String, note: String) = updateNote(timeId) { it + note }
    suspend fun removeNoteFromTime(timeId: String, note: String) = updateNote(timeId) { it - note }

    private suspend fun updateNote(timeId: String, transform: (List<String>) -> List<String>) {
        timeRepository.getAllTimes().first().find { it.id == timeId }?.let { time ->
            timeRepository.saveTime(time.copy(description = transform(time.description)))
        }
    }

    private suspend fun getInt(setting: AppSettings): Int =
        setting.parseIntOrDefault(configRepository.getString(setting.name, setting.defaultValue))

    private fun canAdjust(
        activeTime: Time,
        previousTime: Time?,
        direction: BookingAdjustmentDirection,
        adjustmentMinutes: Int,
        currentTime: LocalTime,
    ): Boolean = runCatching {
        calculateBookingStartAdjustment(activeTime, previousTime, direction, adjustmentMinutes, currentTime.atMinutePrecision())
    }.isSuccess

    fun currentTime(): LocalTime = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).time.atMinutePrecision()
}

data class BookingAdjustmentAvailability(
    val canAdjustEarlier: Boolean = false,
    val canAdjustLater: Boolean = false,
)
