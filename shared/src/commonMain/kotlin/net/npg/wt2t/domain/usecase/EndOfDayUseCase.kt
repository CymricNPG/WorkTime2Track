package net.npg.wt2t.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.npg.wt2t.data.model.AppSettings
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.data.repository.ConfigRepository
import net.npg.wt2t.data.repository.DailyWorkTimeRepository
import net.npg.wt2t.data.repository.DayRepository
import net.npg.wt2t.data.repository.TimeRepository
import net.npg.wt2t.data.repository.TaskRepository
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class EndOfDayData(val targetMinutes: Int, val breakMinutes: Int)

/** Loads and completes a work day through the atomic day repository boundary. */
class EndOfDayUseCase(
    private val dayRepository: DayRepository,
    private val timeRepository: TimeRepository,
    private val dailyWorkTimeRepository: DailyWorkTimeRepository,
    private val configRepository: ConfigRepository,
    private val taskRepository: TaskRepository,
) {
    fun getTimesForDate(date: LocalDate): Flow<List<Time>> = timeRepository.getTimesForDate(date)

    suspend fun getWorkedTime(date: LocalDate): Duration {
        val tasks = taskRepository.getAllTasks().first().associateBy { it.id }
        return timeRepository.getTimesForDate(date).first()
            .filter { !tasks[it.taskId].orFreeTime() }
            .sumOf { ((it.end ?: it.start).toSecondOfDay() - it.start.toSecondOfDay()).div(60).toLong() }
            .minutes
    }

    suspend fun load(date: LocalDate): EndOfDayData {
        dailyWorkTimeRepository.getDailyWorkTime(date).first()?.let {
            return EndOfDayData(it.minutes, it.breakMinutes)
        }
        val hours = getInt(AppSettings.DEFAULT_TARGET_HOURS)
        val minutes = getInt(AppSettings.DEFAULT_TARGET_MINUTES)
        return EndOfDayData(hours * 60 + minutes, 0)
    }

    suspend fun complete(date: LocalDate, endTime: LocalTime, targetMinutes: Int, breakMinutes: Int): DailyWorkTime {
        require(targetMinutes >= 0) { "Target minutes must not be negative." }
        require(breakMinutes >= 0) { "Break minutes must not be negative." }
        return dayRepository.completeDay(date, endTime, targetMinutes, breakMinutes)
    }

    private suspend fun getInt(setting: AppSettings): Int =
        setting.parseIntOrDefault(configRepository.getString(setting.name, setting.defaultValue))
}

private fun net.npg.wt2t.data.model.Task?.orFreeTime(): Boolean = this?.freeTime ?: false
