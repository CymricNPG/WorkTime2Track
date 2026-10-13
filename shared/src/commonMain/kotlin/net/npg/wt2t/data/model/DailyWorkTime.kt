package net.npg.wt2t.data.model

import com.benasher44.uuid.uuid4
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Represents the goals of work time (e.g., planned minutes) for a specific day.
 * @property id Unique identifier for the entry.
 * @property date The date to which this record applies.
 * @property minutes The number of minutes to be worked on this date.
 * @property breakMinutes The break duration deducted from worked time, in minutes.
 */
@Serializable
data class DailyWorkTime(
    val id: String = uuid4().toString(),
    val date: LocalDate,
    val minutes: Int,
    val breakMinutes: Int = 0,
) {
    /** Returns a human-readable representation of this value. */
    override fun toString(): String =
        "DailyWorkTime(id=$id, date=$date, minutes=$minutes, breakMinutes=$breakMinutes)"
}
