package net.npg.wt2t.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.BookingAdjustmentDirection
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.data.model.atMinutePrecision
import net.npg.wt2t.db.WorkTimeDatabase
import net.npg.wt2t.db.WorkTimeDatabaseQueries

/**
 * Implementation of [ProjectRepository] using SQLDelight.
 *
 * @property db The database instance.
 * @property dispatcher The dispatcher to run database operations on.
 */
class SqlDelightProjectRepository(
    private val db: WorkTimeDatabase,
    private val dispatcher: CoroutineDispatcher
) : ProjectRepository {
    /** Returns all projects. */
    override fun getAllProjects(): Flow<List<Project>> =
        db.workTimeDatabaseQueries.selectAllProjects()
            .asFlow()
            .mapToList(dispatcher)
            .map { list -> list.map { it.toModel() } }

    /** Returns the project with the specified identifier. */
    override fun getProjectById(id: String): Flow<Project?> =
        db.workTimeDatabaseQueries.selectProjectById(id)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { it?.toModel() }

    /** Saves project. */
    override suspend fun saveProject(project: Project): Unit = withContext(dispatcher) {
        val queries = db.workTimeDatabaseQueries
        queries.transaction {
            if (queries.selectProjectById(project.id).executeAsOneOrNull() == null) {
                queries.insertProject(project.id, project.name, project.closed)
            } else {
                queries.updateProject(project.name, project.closed, project.id)
            }
        }
    }

    /** Deletes project. */
    override suspend fun deleteProject(id: String): Unit = withContext(dispatcher) {
        db.workTimeDatabaseQueries.deleteProject(id)
    }
}

/**
 * Implementation of [TaskRepository] using SQLDelight.
 *
 * @property db The database instance.
 * @property dispatcher The dispatcher to run database operations on.
 */
class SqlDelightTaskRepository(
    private val db: WorkTimeDatabase,
    private val dispatcher: CoroutineDispatcher
) : TaskRepository {
    /** Returns all tasks. */
    override fun getAllTasks(): Flow<List<Task>> =
        db.workTimeDatabaseQueries.selectAllTasks()
            .asFlow()
            .mapToList(dispatcher)
            .map { list -> list.map { it.toModel() } }

    /** Returns tasks by project. */
    override fun getTasksByProject(projectId: String?): Flow<List<Task>> {
        val query = if (projectId == null) {
            db.workTimeDatabaseQueries.selectTasksWithoutProject()
        } else {
            db.workTimeDatabaseQueries.selectTasksByProject(projectId)
        }

        return query
            .asFlow()
            .mapToList(dispatcher)
            .map { list -> list.map { it.toModel() } }
    }

    /** Saves task. */
    override suspend fun saveTask(task: Task): Unit = withContext(dispatcher) {
        val queries = db.workTimeDatabaseQueries
        queries.transaction {
            if (queries.selectTaskById(task.id).executeAsOneOrNull() == null) {
                queries.insertTask(task.id, task.name, task.freeTime, task.closed, task.projectId)
            } else {
                queries.updateTask(task.name, task.freeTime, task.closed, task.projectId, task.id)
            }
        }
    }

    /** Deletes task. */
    override suspend fun deleteTask(id: String): Unit = withContext(dispatcher) {
        db.workTimeDatabaseQueries.deleteTask(id)
    }
}

/**
 * Implementation of [TimeRepository] using SQLDelight.
 *
 * @property db The database instance.
 * @property dispatcher The dispatcher to run database operations on.
 */
class SqlDelightTimeRepository(
    private val db: WorkTimeDatabase,
    private val dispatcher: CoroutineDispatcher
) : TimeRepository {
    private val bookingMutex = Mutex()

    /** Returns times for date. */
    override fun getTimesForDate(date: LocalDate): Flow<List<Time>> =
        db.workTimeDatabaseQueries.selectTimesByDate(date.toString())
            .asFlow()
            .mapToList(dispatcher)
            .map { list -> list.map { it.toModel() } }

    /** Returns all times. */
    override fun getAllTimes(): Flow<List<Time>> =
        db.workTimeDatabaseQueries.selectAllTimes()
            .asFlow()
            .mapToList(dispatcher)
            .map { list -> list.map { it.toModel() } }

    /** Saves time. */
    override suspend fun saveTime(time: Time): Unit = withContext(dispatcher) {
        val minuteTime = time.atMinutePrecision()
        val queries = db.workTimeDatabaseQueries
        queries.transaction {
            if (queries.selectTimeById(minuteTime.id).executeAsOneOrNull() == null) {
                queries.insertTime(minuteTime)
            } else {
                queries.updateTime(minuteTime)
            }
        }
    }

    /** Atomically closes the active booking and starts the requested booking. */
    override suspend fun switchBooking(
        taskId: String,
        date: LocalDate,
        startTime: LocalTime,
        mergeThresholdMinutes: Int,
    ): Time = withContext(dispatcher) {
        bookingMutex.withLock {
            switchBookingInTransaction(taskId, date, startTime.atMinutePrecision(), mergeThresholdMinutes)
        }
    }

    /** Atomically ends the active booking for a date, if one exists. */
    override suspend fun endActiveBooking(date: LocalDate, endTime: LocalTime): Time? =
        withContext(dispatcher) {
            bookingMutex.withLock {
                val minuteEndTime = endTime.atMinutePrecision()
                val queries = db.workTimeDatabaseQueries
                var endedBooking: Time? = null
                queries.transaction {
                    val activeTime = queries.selectActiveTimeByDate(date.toString())
                        .executeAsOneOrNull()
                        ?.toModel()
                        ?: return@transaction
                    require(minuteEndTime >= activeTime.start) {
                        "Booking end $minuteEndTime must not precede start ${activeTime.start} on $date"
                    }
                    endedBooking = activeTime.copy(end = minuteEndTime)
                    queries.updateTime(requireNotNull(endedBooking))
                }
                endedBooking
            }
        }

    /** Atomically adjusts the active booking start and a connected preceding end. */
    override suspend fun adjustActiveBookingStart(
        direction: BookingAdjustmentDirection,
        date: LocalDate,
        adjustmentMinutes: Int,
        currentTime: LocalTime,
    ): Time = withContext(dispatcher) {
        bookingMutex.withLock {
            val queries = db.workTimeDatabaseQueries
            lateinit var adjustedActiveTime: Time
            queries.transaction {
                val activeTime = checkNotNull(
                    queries.selectActiveTimeByDate(date.toString()).executeAsOneOrNull()?.toModel(),
                ) { "No active booking exists on $date" }
                val previousTime = queries.selectPreviousFinishedTimeIdByDate(
                    date.toString(),
                    activeTime.start.toString(),
                ).executeAsOneOrNull()?.let { previousId ->
                    queries.selectTimeById(previousId).executeAsOne().toModel()
                }
                val adjustment = calculateBookingStartAdjustment(
                    activeTime = activeTime,
                    previousTime = previousTime,
                    direction = direction,
                    adjustmentMinutes = adjustmentMinutes,
                    currentTime = currentTime.atMinutePrecision(),
                )

                if (adjustment.adjustsPreviousTime) {
                    queries.updateTime(previousTime!!.copy(end = adjustment.start))
                }
                adjustedActiveTime = activeTime.copy(start = adjustment.start)
                queries.updateTime(adjustedActiveTime)
            }
            adjustedActiveTime
        }
    }

    /** Deletes time. */
    override suspend fun deleteTime(id: String): Unit = withContext(dispatcher) {
        db.workTimeDatabaseQueries.deleteTime(id)
    }

    /** Replaces times for date. */
    override suspend fun replaceTimesForDate(date: LocalDate, times: List<Time>): Unit = withContext(dispatcher) {
        db.workTimeDatabaseQueries.transaction {
            db.workTimeDatabaseQueries.deleteTimesByDate(date.toString())
            times.forEach { time ->
                db.workTimeDatabaseQueries.insertTime(time)
            }
        }
    }

    private fun switchBookingInTransaction(
        taskId: String,
        date: LocalDate,
        startTime: LocalTime,
        mergeThresholdMinutes: Int,
    ): Time {
        require(mergeThresholdMinutes in MERGE_THRESHOLD_MINUTES_RANGE) {
            "Merge threshold must be between 0 and 1440 minutes"
        }
        val queries = db.workTimeDatabaseQueries
        lateinit var result: Time
        queries.transaction {
            val activeTime = queries.selectActiveTimeByDate(date.toString())
                .executeAsOneOrNull()
                ?.toModel()
            if (activeTime?.taskId == taskId && activeTime.start == startTime) {
                result = activeTime
                return@transaction
            }
            val latestFinishedEnd = findLatestFinishedEnd(date)
            require(latestFinishedEnd == null || startTime >= latestFinishedEnd) {
                "Booking start $startTime must not precede latest end $latestFinishedEnd on $date"
            }
            if (activeTime != null) {
                require(startTime >= activeTime.start) {
                    "Booking start $startTime must not precede active start ${activeTime.start} on $date"
                }
                queries.updateTime(activeTime.copy(end = startTime))
            }

            val effectiveStart = if (activeTime == null) {
                calculateEffectiveStart(startTime, latestFinishedEnd, mergeThresholdMinutes)
            } else {
                startTime
            }
            result = Time(taskId = taskId, date = date, start = effectiveStart)
            queries.insertTime(result)
        }
        return result
    }

    private fun findLatestFinishedEnd(date: LocalDate): LocalTime? =
        db.workTimeDatabaseQueries.selectLatestFinishedTimeByDate(date.toString())
            .executeAsOneOrNull()
            ?.end
            ?.let(LocalTime::parse)
            ?.atMinutePrecision()

    private fun calculateEffectiveStart(
        startTime: LocalTime,
        latestFinishedEnd: LocalTime?,
        mergeThresholdMinutes: Int,
    ): LocalTime {
        if (latestFinishedEnd == null) return startTime
        val gapMinutes = startTime.toMinuteOfDay() - latestFinishedEnd.toMinuteOfDay()
        return if (gapMinutes <= mergeThresholdMinutes) latestFinishedEnd else startTime
    }

    private fun WorkTimeDatabaseQueries.insertTime(time: Time) {
        val minuteTime = time.atMinutePrecision()
        insertTime(
            id = minuteTime.id,
            taskId = minuteTime.taskId,
            date = minuteTime.date.toString(),
            start = minuteTime.start.toString(),
            end = minuteTime.end?.toString(),
            description = Json.encodeToString(minuteTime.description),
        )
    }

    private fun WorkTimeDatabaseQueries.updateTime(time: Time) {
        val minuteTime = time.atMinutePrecision()
        updateTime(
            taskId = minuteTime.taskId,
            date = minuteTime.date.toString(),
            start = minuteTime.start.toString(),
            end = minuteTime.end?.toString(),
            description = Json.encodeToString(minuteTime.description),
            id = minuteTime.id,
        )
    }

    private companion object {
        val MERGE_THRESHOLD_MINUTES_RANGE = 0..1_440
    }
}

/** Describes a validated booking-start adjustment. */
internal data class BookingStartAdjustment(
    val start: LocalTime,
    val adjustsPreviousTime: Boolean,
)

/** Calculates and validates one complete booking-start adjustment. */
internal fun calculateBookingStartAdjustment(
    activeTime: Time,
    previousTime: Time?,
    direction: BookingAdjustmentDirection,
    adjustmentMinutes: Int,
    currentTime: LocalTime,
): BookingStartAdjustment {
    require(adjustmentMinutes in BOOKING_START_ADJUSTMENT_MINUTES_RANGE) {
        "Booking start adjustment must be between 1 and 30 minutes"
    }
    val activeStartMinute = activeTime.start.toMinuteOfDay()
    val adjustedStartMinute = activeStartMinute + direction.multiplier * adjustmentMinutes
    require(adjustedStartMinute in 0 until MINUTES_PER_DAY) {
        "Booking start adjustment must remain on the same day"
    }
    val currentMinute = currentTime.toMinuteOfDay()
    val adjustedStart = LocalTime.fromSecondOfDay(adjustedStartMinute * SECONDS_PER_MINUTE)
    require(adjustedStartMinute <= currentMinute) {
        "Booking start $adjustedStart must not be after current time $currentTime"
    }
    require(currentMinute - adjustedStartMinute >= adjustmentMinutes) {
        "Active booking must remain at least $adjustmentMinutes minutes long"
    }

    val previousEndMinute = previousTime?.end?.toMinuteOfDay()
    val adjustsPreviousTime = previousEndMinute != null &&
        (previousEndMinute == activeStartMinute || adjustedStartMinute < previousEndMinute)
    if (adjustsPreviousTime) {
        val previousDurationMinutes = adjustedStartMinute - previousTime.start.toMinuteOfDay()
        require(previousDurationMinutes >= adjustmentMinutes) {
            "Previous booking must remain at least $adjustmentMinutes minutes long"
        }
    }
    return BookingStartAdjustment(adjustedStart, adjustsPreviousTime)
}

private fun LocalTime.toMinuteOfDay(): Int = hour * MINUTES_PER_HOUR + minute

private val BOOKING_START_ADJUSTMENT_MINUTES_RANGE = 1..30
private const val SECONDS_PER_MINUTE = 60
private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 1_440

/**
 * Implementation of [DailyWorkTimeRepository] using SQLDelight.
 *
 * @property db The database instance.
 * @property dispatcher The dispatcher to run database operations on.
 */
class SqlDelightDailyWorkTimeRepository(
    private val db: WorkTimeDatabase,
    private val dispatcher: CoroutineDispatcher
) : DailyWorkTimeRepository {
    /** Returns daily work time. */
    override fun getDailyWorkTime(date: LocalDate): Flow<DailyWorkTime?> =
        db.workTimeDatabaseQueries.selectDailyWorkTimeByDate(date.toString())
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { it?.toModel() }

    /** Returns all daily work times. */
    override fun getAllDailyWorkTimes(): Flow<List<DailyWorkTime>> =
        db.workTimeDatabaseQueries.selectAllDailyWorkTimes()
            .asFlow()
            .mapToList(dispatcher)
            .map { entities -> entities.map { it.toModel() } }

    /** Saves daily work time. */
    override suspend fun saveDailyWorkTime(dailyWorkTime: DailyWorkTime): Unit = withContext(dispatcher) {
        db.workTimeDatabaseQueries.insertDailyWorkTime(
            id = dailyWorkTime.id,
            date = dailyWorkTime.date.toString(),
            minutes = dailyWorkTime.minutes.toLong(),
            breakMinutes = dailyWorkTime.breakMinutes.toLong(),
        )
    }

    /** Deletes all daily work times. */
    override suspend fun deleteAllDailyWorkTimes(): Unit = withContext(dispatcher) {
        db.workTimeDatabaseQueries.deleteAllDailyWorkTimes()
    }
}

/** Provides persistence operations for sql delight config data. */
class SqlDelightConfigRepository(
    private val db: WorkTimeDatabase,
    private val dispatcher: CoroutineDispatcher
) : ConfigRepository {
    /** Returns the string setting for a key, or the supplied default. */
    override suspend fun getString(key: String, defaultValue: String): String = withContext(dispatcher) {
        db.workTimeDatabaseQueries.selectConfig(key).executeAsOneOrNull() ?: defaultValue
    }

    /** Stores a string setting. */
    override suspend fun setString(key: String, value: String): Unit = withContext(dispatcher) {
        db.workTimeDatabaseQueries.insertConfig(key, value)
    }

    /** Returns the integer setting for a key, or the supplied default. */
    override suspend fun getInt(key: String, defaultValue: Int): Int = withContext(dispatcher) {
        db.workTimeDatabaseQueries.selectConfig(key).executeAsOneOrNull()?.toIntOrNull() ?: defaultValue
    }

    /** Stores an integer setting. */
    override suspend fun setInt(key: String, value: Int): Unit = withContext(dispatcher) {
        db.workTimeDatabaseQueries.insertConfig(key, value.toString())
    }

    /** Stores all supplied settings atomically. */
    override suspend fun setAll(values: Map<String, String>): Unit = withContext(dispatcher) {
        val queries = db.workTimeDatabaseQueries
        queries.transaction {
            values.forEach { (key, value) -> queries.insertConfig(key, value) }
        }
    }
}

/** Provides persistence operations for sql delight day data. */
class SqlDelightDayRepository(
    private val db: WorkTimeDatabase,
    private val dispatcher: CoroutineDispatcher,
) : DayRepository {
    /** Creates daily values for a date that has no existing data. */
    override suspend fun createEmptyDay(
        date: LocalDate,
        targetMinutes: Int,
        breakMinutes: Int,
    ): DailyWorkTime = withContext(dispatcher) {
        val dateText = date.toString()
        val dailyWorkTime = DailyWorkTime(
            date = date,
            minutes = targetMinutes,
            breakMinutes = breakMinutes,
        )

        db.workTimeDatabaseQueries.transaction {
            check(db.workTimeDatabaseQueries.selectTimesByDate(dateText).executeAsList().isEmpty()) {
                "Day $date already has bookings."
            }
            check(db.workTimeDatabaseQueries.selectDailyWorkTimeByDate(dateText).executeAsOneOrNull() == null) {
                "Day $date already has daily values."
            }
            db.workTimeDatabaseQueries.insertNewDailyWorkTime(
                id = dailyWorkTime.id,
                date = dateText,
                minutes = targetMinutes.toLong(),
                breakMinutes = breakMinutes.toLong(),
            )
        }

        dailyWorkTime
    }

    /** Atomically closes the active booking and stores the daily values. */
    override suspend fun completeDay(
        date: LocalDate,
        endTime: LocalTime,
        targetMinutes: Int,
        breakMinutes: Int,
    ): DailyWorkTime = withContext(dispatcher) {
        val minuteEndTime = endTime.atMinutePrecision()
        val queries = db.workTimeDatabaseQueries
        val dateText = date.toString()
        lateinit var result: DailyWorkTime

        queries.transaction {
            check(queries.selectTimesByDate(dateText).executeAsList().isNotEmpty()) {
                "End of day requires at least one booking on $date."
            }

            val activeTime = queries.selectActiveTimeByDate(dateText)
                .executeAsOneOrNull()
                ?.toModel()
            if (activeTime != null) {
                require(minuteEndTime >= activeTime.start) {
                    "Booking end $minuteEndTime must not precede start ${activeTime.start} on $date"
                }
                queries.updateTime(
                    taskId = activeTime.taskId,
                    date = dateText,
                    start = activeTime.start.toString(),
                    end = minuteEndTime.toString(),
                    description = Json.encodeToString(activeTime.description),
                    id = activeTime.id,
                )
            }

            val existingDailyWorkTime = queries.selectDailyWorkTimeByDate(dateText)
                .executeAsOneOrNull()
                ?.toModel()
            result = existingDailyWorkTime?.copy(
                minutes = targetMinutes,
                breakMinutes = breakMinutes,
            ) ?: DailyWorkTime(
                date = date,
                minutes = targetMinutes,
                breakMinutes = breakMinutes,
            )
            queries.insertDailyWorkTime(
                id = result.id,
                date = dateText,
                minutes = result.minutes.toLong(),
                breakMinutes = result.breakMinutes.toLong(),
            )
        }

        result
    }

    /** Replaces day. */
    override suspend fun replaceDay(
        date: LocalDate,
        targetMinutes: Int,
        breakMinutes: Int,
        times: List<Time>,
    ): DailyWorkTime =
        withContext(dispatcher) {
            val dateText = date.toString()
            val existingDailyWorkTime = db.workTimeDatabaseQueries
                .selectDailyWorkTimeByDate(dateText)
                .executeAsOneOrNull()
                ?.toModel()
            val dailyWorkTime = existingDailyWorkTime?.copy(
                minutes = targetMinutes,
                breakMinutes = breakMinutes,
            ) ?: DailyWorkTime(
                date = date,
                minutes = targetMinutes,
                breakMinutes = breakMinutes,
            )

            db.workTimeDatabaseQueries.transaction {
                db.workTimeDatabaseQueries.deleteTimesByDate(dateText)
                times.forEach { time ->
                    val minuteTime = time.atMinutePrecision()
                    db.workTimeDatabaseQueries.insertTime(
                        id = minuteTime.id,
                        taskId = minuteTime.taskId,
                        date = minuteTime.date.toString(),
                        start = minuteTime.start.toString(),
                        end = minuteTime.end?.toString(),
                        description = Json.encodeToString(minuteTime.description),
                    )
                }
                db.workTimeDatabaseQueries.insertDailyWorkTime(
                    id = dailyWorkTime.id,
                    date = dailyWorkTime.date.toString(),
                    minutes = dailyWorkTime.minutes.toLong(),
                    breakMinutes = dailyWorkTime.breakMinutes.toLong(),
                )
            }

            dailyWorkTime
        }

    /** Atomically deletes all bookings and daily values for a date. */
    override suspend fun deleteDay(date: LocalDate): Unit = withContext(dispatcher) {
        val dateText = date.toString()
        db.workTimeDatabaseQueries.transaction {
            db.workTimeDatabaseQueries.deleteTimesByDate(dateText)
            db.workTimeDatabaseQueries.deleteDailyWorkTimeByDate(dateText)
        }
    }
}
