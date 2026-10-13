package net.npg.wt2t.domain.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.BookingAdjustmentDirection
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.data.repository.*

class MockProjectRepository : ProjectRepository {
    val projects = MutableStateFlow<List<Project>>(emptyList())
    override fun getAllProjects() = projects
    override fun getProjectById(id: String) = projects.map { list -> list.find { it.id == id } }
    override suspend fun saveProject(project: Project) {
        val current = projects.value.toMutableList()
        current.removeIf { it.id == project.id }
        current.add(project)
        projects.value = current
    }

    override suspend fun deleteProject(id: String) {
        projects.value = projects.value.filter { it.id != id }
    }
}

class MockTaskRepository : TaskRepository {
    val tasks = MutableStateFlow<List<Task>>(emptyList())
    override fun getAllTasks() = tasks
    override fun getTasksByProject(projectId: String?) = tasks.map { list -> list.filter { it.projectId == projectId } }
    override suspend fun saveTask(task: Task) {
        val current = tasks.value.toMutableList()
        current.removeIf { it.id == task.id }
        current.add(task)
        tasks.value = current
    }

    override suspend fun deleteTask(id: String) {
        tasks.value = tasks.value.filter { it.id != id }
    }
}

class MockTimeRepository : TimeRepository {
    val times = MutableStateFlow<List<Time>>(emptyList())
    override fun getTimesForDate(date: LocalDate) = times.map { list -> list.filter { it.date == date } }
    override fun getAllTimes() = times
    override suspend fun saveTime(time: Time) {
        val current = times.value.toMutableList()
        current.removeIf { it.id == time.id }
        current.add(time)
        times.value = current
    }

    override suspend fun switchBooking(
        taskId: String,
        date: LocalDate,
        startTime: LocalTime,
        mergeThresholdMinutes: Int,
    ): Time {
        val activeTime = times.value.find { it.date == date && it.end == null }
        if (activeTime?.taskId == taskId && activeTime.start == startTime) return activeTime
        if (activeTime != null) {
            require(startTime >= activeTime.start)
            saveTime(activeTime.copy(end = startTime))
        }
        val lastEnd = times.value
            .filter { it.date == date && it.end != null }
            .maxOfOrNull { it.end!! }
        require(lastEnd == null || startTime >= lastEnd)
        val effectiveStart = lastEnd?.takeIf {
            startTime.toSecondOfDay() - it.toSecondOfDay() <= mergeThresholdMinutes * 60
        } ?: startTime
        return Time(taskId = taskId, date = date, start = effectiveStart).also { saveTime(it) }
    }

    override suspend fun endActiveBooking(date: LocalDate, endTime: LocalTime): Time? {
        val activeTime = times.value.find { it.date == date && it.end == null } ?: return null
        require(endTime >= activeTime.start)
        return activeTime.copy(end = endTime).also { saveTime(it) }
    }

    override suspend fun adjustActiveBookingStart(
        direction: BookingAdjustmentDirection,
        date: LocalDate,
        adjustmentMinutes: Int,
        currentTime: LocalTime,
    ): Time {
        val activeTime = checkNotNull(times.value.find { it.date == date && it.end == null })
        val previousTime = times.value
            .filter { it.date == date && it.end != null && it.end <= activeTime.start }
            .maxByOrNull { it.end!! }
        val adjustment = calculateBookingStartAdjustment(
            activeTime,
            previousTime,
            direction,
            adjustmentMinutes,
            currentTime,
        )
        if (adjustment.adjustsPreviousTime) {
            saveTime(previousTime!!.copy(end = adjustment.start))
        }
        return activeTime.copy(start = adjustment.start).also { saveTime(it) }
    }

    override suspend fun deleteTime(id: String) {
        times.value = times.value.filter { it.id != id }
    }

    override suspend fun replaceTimesForDate(date: LocalDate, times: List<Time>) {
        this.times.value = this.times.value.filter { it.date != date } + times
    }
}

class MockDailyWorkTimeRepository : DailyWorkTimeRepository {
    val dailyWorkTimes = MutableStateFlow<List<DailyWorkTime>>(emptyList())
    override fun getDailyWorkTime(date: LocalDate) = dailyWorkTimes.map { list -> list.find { it.date == date } }
    override fun getAllDailyWorkTimes() = dailyWorkTimes
    override suspend fun saveDailyWorkTime(dailyWorkTime: DailyWorkTime) {
        val current = dailyWorkTimes.value.toMutableList()
        current.removeIf { it.id == dailyWorkTime.id }
        current.add(dailyWorkTime)
        dailyWorkTimes.value = current
    }

    override suspend fun deleteAllDailyWorkTimes() {
        TODO("Not yet implemented")
    }
}

class MockDayRepository(
    private val timeRepository: MockTimeRepository,
    private val dailyWorkTimeRepository: MockDailyWorkTimeRepository,
) : DayRepository {
    override suspend fun createEmptyDay(
        date: LocalDate,
        targetMinutes: Int,
        breakMinutes: Int,
    ): DailyWorkTime {
        check(timeRepository.times.value.none { it.date == date }) { "Day $date already has bookings." }
        check(dailyWorkTimeRepository.dailyWorkTimes.value.none { it.date == date }) {
            "Day $date already has daily values."
        }
        return DailyWorkTime(date = date, minutes = targetMinutes, breakMinutes = breakMinutes).also {
            dailyWorkTimeRepository.saveDailyWorkTime(it)
        }
    }

    override suspend fun completeDay(
        date: LocalDate,
        endTime: LocalTime,
        targetMinutes: Int,
        breakMinutes: Int,
    ): DailyWorkTime {
        val dateTimes = timeRepository.times.value.filter { it.date == date }
        check(dateTimes.isNotEmpty()) { "End of day requires at least one booking on $date." }
        val activeTime = dateTimes.find { it.end == null }
        if (activeTime != null) {
            require(endTime >= activeTime.start)
            timeRepository.saveTime(activeTime.copy(end = endTime))
        }
        val dailyWorkTime = dailyWorkTimeRepository.dailyWorkTimes.value
            .find { it.date == date }
            ?.copy(minutes = targetMinutes, breakMinutes = breakMinutes)
            ?: DailyWorkTime(date = date, minutes = targetMinutes, breakMinutes = breakMinutes)
        dailyWorkTimeRepository.saveDailyWorkTime(dailyWorkTime)
        return dailyWorkTime
    }

    override suspend fun replaceDay(
        date: LocalDate,
        targetMinutes: Int,
        breakMinutes: Int,
        times: List<Time>,
    ): DailyWorkTime {
        timeRepository.replaceTimesForDate(date, times)
        val dailyWorkTime = dailyWorkTimeRepository.dailyWorkTimes.value
            .find { it.date == date }
            ?.copy(minutes = targetMinutes, breakMinutes = breakMinutes)
            ?: DailyWorkTime(date = date, minutes = targetMinutes, breakMinutes = breakMinutes)
        dailyWorkTimeRepository.saveDailyWorkTime(dailyWorkTime)
        return dailyWorkTime
    }

    override suspend fun deleteDay(date: LocalDate) {
        timeRepository.replaceTimesForDate(date, emptyList())
        dailyWorkTimeRepository.dailyWorkTimes.value =
            dailyWorkTimeRepository.dailyWorkTimes.value.filterNot { it.date == date }
    }
}

class MockConfigRepository : ConfigRepository {
    private val data = mutableMapOf<String, String>()
    override suspend fun getString(key: String, defaultValue: String): String = data[key] ?: defaultValue
    override suspend fun setString(key: String, value: String) {
        data[key] = value
    }

    override suspend fun getInt(key: String, defaultValue: Int): Int = data[key]?.toIntOrNull() ?: defaultValue
    override suspend fun setInt(key: String, value: Int) {
        data[key] = value.toString()
    }

    override suspend fun setAll(values: Map<String, String>) {
        data.putAll(values)
    }
}
