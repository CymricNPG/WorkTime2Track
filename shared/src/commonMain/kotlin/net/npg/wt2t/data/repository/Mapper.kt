package net.npg.wt2t.data.repository

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.data.model.atMinutePrecision
import net.npg.wt2t.db.DailyWorkTimeEntity
import net.npg.wt2t.db.ProjectEntity
import net.npg.wt2t.db.TaskEntity
import net.npg.wt2t.db.TimeEntity

/**
 * Maps Database entities to model entities and vice versa
 */
fun ProjectEntity.toModel() = Project(
    id = id,
    name = name,
    closed = closed
)

/** Converts this database entity to its domain model. */
fun TaskEntity.toModel() = Task(
    id = id,
    name = name,
    freeTime = freeTime,
    closed = closed,
    projectId = projectId
)

/** Converts this database entity to its domain model. */
fun TimeEntity.toModel(): Time {
    val descriptionList = try {
        Json.decodeFromString<List<String>>(description)
    } catch (_: Exception) {
        description.split(",").filter { it.isNotBlank() }
    }
    return Time(
        id = id,
        taskId = taskId,
        date = LocalDate.parse(date),
        start = LocalTime.parse(start).atMinutePrecision(),
        end = end?.let { LocalTime.parse(it).atMinutePrecision() },
        description = descriptionList
    )
}

/** Converts this database entity to its domain model. */
fun DailyWorkTimeEntity.toModel() = DailyWorkTime(
    id = id,
    date = LocalDate.parse(date),
    minutes = minutes.toInt(),
    breakMinutes = breakMinutes.toInt(),
)

/** Converts this domain model to its database entity. */
fun Project.toEntity() = ProjectEntity(
    id = id,
    name = name,
    closed = closed
)

/** Converts this domain model to its database entity. */
fun Task.toEntity() = TaskEntity(
    id = id,
    name = name,
    freeTime = freeTime,
    closed = closed,
    projectId = projectId
)

/** Converts this domain model to its database entity. */
fun Time.toEntity() = TimeEntity(
    id = id,
    taskId = taskId,
    date = date.toString(),
    start = start.atMinutePrecision().toString(),
    end = end?.atMinutePrecision()?.toString(),
    description = Json.encodeToString(description)
)

/** Converts this domain model to its database entity. */
fun DailyWorkTime.toEntity() = DailyWorkTimeEntity(
    id = id,
    date = date.toString(),
    minutes = minutes.toLong(),
    breakMinutes = breakMinutes.toLong(),
)
