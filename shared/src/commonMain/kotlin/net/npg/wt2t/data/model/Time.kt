package net.npg.wt2t.data.model

import com.benasher44.uuid.uuid4
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

/**
 * Represents a logged block of time for a specific task on a certain date.
 * @property taskId The ID of the associated task.
 * @property start The time when work began.
 * @property end The time when work ended (null if currently active).
 * @property description A list of notes or details regarding this time entry.
 */
@Serializable
data class Time(
    val id: String = uuid4().toString(),
    val taskId: String,
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime? = null,
    val description: List<String> = emptyList()
) {
    /** Returns a human-readable representation of this value. */
    override fun toString(): String = "Time(id=$id, taskId=$taskId, date=$date, start=$start, end=$end, description=$description)"
}

/** Returns this time with seconds and nanoseconds removed. */
fun LocalTime.atMinutePrecision(): LocalTime = LocalTime(hour, minute)

/** Returns this booking with all boundaries reduced to minute precision. */
fun Time.atMinutePrecision(): Time = copy(
    start = start.atMinutePrecision(),
    end = end?.atMinutePrecision(),
)
