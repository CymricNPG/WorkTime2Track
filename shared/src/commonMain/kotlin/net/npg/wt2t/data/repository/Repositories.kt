package net.npg.wt2t.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.BookingAdjustmentDirection
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time

/** Provides persistence operations for project data. */
interface ProjectRepository {
    /** Returns all projects. */
    fun getAllProjects(): Flow<List<Project>>
    /** Returns the project with the specified identifier. */
    fun getProjectById(id: String): Flow<Project?>
    /** Saves project. */
    suspend fun saveProject(project: Project)
    /** Deletes project. */
    suspend fun deleteProject(id: String)
}

/** Provides persistence operations for task data. */
interface TaskRepository {
    /** Returns all tasks. */
    fun getAllTasks(): Flow<List<Task>>
    /** Returns tasks by project. */
    fun getTasksByProject(projectId: String?): Flow<List<Task>>
    /** Saves task. */
    suspend fun saveTask(task: Task)
    /** Deletes task. */
    suspend fun deleteTask(id: String)
}

/** Provides persistence operations for time data. */
interface TimeRepository {
    /** Returns times for date. */
    fun getTimesForDate(date: LocalDate): Flow<List<Time>>
    /** Returns all times. */
    fun getAllTimes(): Flow<List<Time>>
    /** Saves time. */
    suspend fun saveTime(time: Time)
    /**
     * Atomically closes the active booking and starts the requested booking.
     *
     * @throws IllegalArgumentException when [startTime] precedes the latest chronological boundary.
     */
    suspend fun switchBooking(
        taskId: String,
        date: LocalDate,
        startTime: LocalTime,
        mergeThresholdMinutes: Int,
    ): Time
    /**
     * Atomically ends the active booking for a date, if one exists.
     *
     * @return The finished booking, or null when no active booking exists for [date].
     * @throws IllegalArgumentException when [endTime] precedes the active booking's start.
     */
    suspend fun endActiveBooking(date: LocalDate, endTime: LocalTime): Time?
    /**
     * Atomically moves the active booking's start by a complete configured step.
     *
     * A preceding booking is moved with the active start when both share a boundary or when an
     * earlier adjustment crosses its end.
     *
     * @throws IllegalStateException when no active booking exists for [date].
     * @throws IllegalArgumentException when the complete adjustment would violate a time boundary.
     */
    suspend fun adjustActiveBookingStart(
        direction: BookingAdjustmentDirection,
        date: LocalDate,
        adjustmentMinutes: Int,
        currentTime: LocalTime,
    ): Time
    /** Deletes time. */
    suspend fun deleteTime(id: String)
    /** Replaces times for date. */
    suspend fun replaceTimesForDate(date: LocalDate, times: List<Time>)
}

/** Provides persistence operations for daily work time data. */
interface DailyWorkTimeRepository {
    /** Returns daily work time. */
    fun getDailyWorkTime(date: LocalDate): Flow<DailyWorkTime?>
    /** Returns all daily work times. */
    fun getAllDailyWorkTimes(): Flow<List<DailyWorkTime>>
    /** Saves daily work time. */
    suspend fun saveDailyWorkTime(dailyWorkTime: DailyWorkTime)
    /** Deletes all daily work times. */
    suspend fun deleteAllDailyWorkTimes()
}

/** Provides persistence operations for day data. */
interface DayRepository {
    /**
     * Creates an empty day with daily values when no data exists for [date].
     *
     * @throws IllegalStateException when [date] already has a booking or daily values.
     */
    suspend fun createEmptyDay(
        date: LocalDate,
        targetMinutes: Int,
        breakMinutes: Int,
    ): DailyWorkTime

    /**
     * Atomically closes the active booking and stores the daily values for [date].
     *
     * @throws IllegalStateException when the date has no bookings.
     * @throws IllegalArgumentException when [endTime] is not after the active booking's start.
     */
    suspend fun completeDay(
        date: LocalDate,
        endTime: LocalTime,
        targetMinutes: Int,
        breakMinutes: Int,
    ): DailyWorkTime

    /** Replaces day. */
    suspend fun replaceDay(
        date: LocalDate,
        targetMinutes: Int,
        breakMinutes: Int,
        times: List<Time>,
    ): DailyWorkTime

    /** Atomically deletes all bookings and daily values for [date]. */
    suspend fun deleteDay(date: LocalDate)
}

/** Provides persistence operations for config data. */
interface ConfigRepository {
    /** Returns the string setting for a key, or the supplied default. */
    suspend fun getString(key: String, defaultValue: String): String
    /** Stores a string setting. */
    suspend fun setString(key: String, value: String)
    /** Returns the integer setting for a key, or the supplied default. */
    suspend fun getInt(key: String, defaultValue: Int): Int
    /** Stores an integer setting. */
    suspend fun setInt(key: String, value: Int)
    /** Stores all supplied settings atomically. */
    suspend fun setAll(values: Map<String, String>)
}
