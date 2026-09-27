package net.npg.wt2t.data.repository

import net.npg.wt2t.data.model.AppSettings
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.data.model.atMinutePrecision

/** Identifies why a database backup could not be imported. */
enum class DatabaseImportFailure {
    MALFORMED_CONTENT,
    UNSUPPORTED_FORMAT,
    INVALID_IDENTIFIER,
    DUPLICATE_PROJECT_ID,
    DUPLICATE_PROJECT_NAME,
    DUPLICATE_TASK_ID,
    DUPLICATE_TASK_NAME,
    MISSING_TASK_PROJECT,
    DUPLICATE_TIME_ID,
    MISSING_TIME_TASK,
    INVALID_TIME_RANGE,
    OVERLAPPING_TIMES,
    DUPLICATE_DAILY_WORK_TIME_ID,
    DUPLICATE_DAILY_WORK_TIME_DATE,
    INVALID_DAILY_WORK_TIME,
    UNKNOWN_CONFIGURATION,
    INVALID_CONFIGURATION,
    PERSISTENCE_FAILED,
}

/** Describes a domain validation or persistence failure while importing a backup. */
class DatabaseImportException(
    val failure: DatabaseImportFailure,
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

/** Validates complete backup snapshots before persistence is modified. */
internal object DatabaseImportValidator {
    /** Validates all local and cross-record invariants in [export]. */
    fun validate(export: DatabaseExport) {
        validateProjects(export.projects)
        validateTasks(export.tasks, export.projects)
        validateTimes(export.times.map(Time::atMinutePrecision), export.tasks)
        validateDailyWorkTimes(export.dailyWorkTimes)
        validateConfigs(export.configs)
    }

    private fun validateProjects(projects: List<Project>) {
        requireIdentifiers(projects, Project::id, "project")
        requireUnique(
            values = projects,
            key = Project::id,
            failure = DatabaseImportFailure.DUPLICATE_PROJECT_ID,
            description = "project ID",
        )
        requireUnique(
            values = projects,
            key = Project::name,
            failure = DatabaseImportFailure.DUPLICATE_PROJECT_NAME,
            description = "project name",
        )
    }

    private fun validateTasks(tasks: List<Task>, projects: List<Project>) {
        requireIdentifiers(tasks, Task::id, "task")
        requireUnique(
            values = tasks,
            key = Task::id,
            failure = DatabaseImportFailure.DUPLICATE_TASK_ID,
            description = "task ID",
        )
        requireUnique(
            values = tasks,
            key = { it.projectId to it.name },
            failure = DatabaseImportFailure.DUPLICATE_TASK_NAME,
            description = "task name within a project",
        )

        val projectIds = projects.mapTo(mutableSetOf(), Project::id)
        tasks.firstOrNull { it.projectId != null && it.projectId !in projectIds }?.let { task ->
            fail(
                DatabaseImportFailure.MISSING_TASK_PROJECT,
                "Task ${task.id} references missing project ${task.projectId}",
            )
        }
    }

    private fun validateTimes(times: List<Time>, tasks: List<Task>) {
        requireIdentifiers(times, Time::id, "time entry")
        requireUnique(
            values = times,
            key = Time::id,
            failure = DatabaseImportFailure.DUPLICATE_TIME_ID,
            description = "time entry ID",
        )

        val taskIds = tasks.mapTo(mutableSetOf(), Task::id)
        times.firstOrNull { it.taskId !in taskIds }?.let { time ->
            fail(
                DatabaseImportFailure.MISSING_TIME_TASK,
                "Time entry ${time.id} references missing task ${time.taskId}",
            )
        }
        times.firstOrNull { it.end != null && it.start > it.end }?.let { time ->
            fail(
                DatabaseImportFailure.INVALID_TIME_RANGE,
                "Time entry ${time.id} must not start after it ends",
            )
        }

        times.groupBy(Time::date).forEach { (date, entries) ->
            val overlap = entries
                .filterNot { it.end == it.start }
                .sortedBy(Time::start)
                .zipWithNext()
                .firstOrNull { (previous, next) ->
                    previous.end == null || previous.end > next.start
                }
            if (overlap != null) {
                fail(
                    DatabaseImportFailure.OVERLAPPING_TIMES,
                    "Time entries ${overlap.first} and ${overlap.second} overlap on $date",
                )
            }
        }
    }

    private fun validateDailyWorkTimes(dailyWorkTimes: List<DailyWorkTime>) {
        requireIdentifiers(dailyWorkTimes, DailyWorkTime::id, "daily work time")
        requireUnique(
            values = dailyWorkTimes,
            key = DailyWorkTime::id,
            failure = DatabaseImportFailure.DUPLICATE_DAILY_WORK_TIME_ID,
            description = "daily work time ID",
        )
        requireUnique(
            values = dailyWorkTimes,
            key = DailyWorkTime::date,
            failure = DatabaseImportFailure.DUPLICATE_DAILY_WORK_TIME_DATE,
            description = "daily work time date",
        )
        dailyWorkTimes.firstOrNull {
            it.minutes !in MINUTES_PER_DAY_RANGE || it.breakMinutes < 0
        }?.let { dailyWorkTime ->
            fail(
                DatabaseImportFailure.INVALID_DAILY_WORK_TIME,
                "Daily work time ${dailyWorkTime.id} contains an invalid minute value",
            )
        }
    }

    private fun validateConfigs(configs: Map<String, String>) {
        configs.forEach { (key, value) ->
            val setting = AppSettings.fromKey(key) ?: fail(
                DatabaseImportFailure.UNKNOWN_CONFIGURATION,
                "Database backup contains unknown configuration key: $key",
            )
            if (!setting.isValidValue(value)) {
                fail(
                    DatabaseImportFailure.INVALID_CONFIGURATION,
                    "Database backup contains an invalid value for configuration key $key",
                )
            }
        }

        val hours = configs[AppSettings.DEFAULT_TARGET_HOURS.name]?.toIntOrNull() ?: 0
        val minutes = configs[AppSettings.DEFAULT_TARGET_MINUTES.name]?.toIntOrNull() ?: 0
        if (hours * MINUTES_PER_HOUR + minutes > MINUTES_PER_DAY_RANGE.last) {
            fail(
                DatabaseImportFailure.INVALID_CONFIGURATION,
                "Database backup contains a default target time longer than one day",
            )
        }
    }

    private fun <Value, Key> requireUnique(
        values: List<Value>,
        key: (Value) -> Key,
        failure: DatabaseImportFailure,
        description: String,
    ) {
        val duplicate = values
            .groupingBy(key)
            .eachCount()
            .entries
            .firstOrNull { it.value > 1 }
            ?.key
        if (duplicate != null) {
            fail(failure, "Database backup contains duplicate $description: $duplicate")
        }
    }

    private fun <Value> requireIdentifiers(
        values: List<Value>,
        identifier: (Value) -> String,
        description: String,
    ) {
        if (values.any { identifier(it).isBlank() }) {
            fail(
                DatabaseImportFailure.INVALID_IDENTIFIER,
                "Database backup contains a blank $description identifier",
            )
        }
    }

    private fun fail(failure: DatabaseImportFailure, message: String): Nothing =
        throw DatabaseImportException(failure, message)

    private val MINUTES_PER_DAY_RANGE = 0..1_440
    private const val MINUTES_PER_HOUR = 60
}
