package net.npg.wt2t.data.model

import com.benasher44.uuid.uuid4
import kotlinx.serialization.Serializable

/**
 * Represents an individual task within a project or as a standalone "free time" entry.
 * @property name The name of the task.
 * @property freeTime Indicates if this task represents non-project work.
 * @property closed Indicates whether the task is marked as finished.
 */
@Serializable
data class Task(
    val id: String = uuid4().toString(),
    val name: String,
    val freeTime: Boolean = false,
    val closed: Boolean = false,
    val projectId: String? = null
) {
    init {
        require(name.isNotBlank()) { "Task name cannot be empty" }
    }

    /** Returns a human-readable representation of this value. */
    override fun toString(): String = "Task(id=$id, name='$name', freeTime=$freeTime, closed=$closed, projectId=$projectId)"
}
