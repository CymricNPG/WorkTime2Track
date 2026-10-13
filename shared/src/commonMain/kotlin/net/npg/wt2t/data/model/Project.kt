package net.npg.wt2t.data.model

import com.benasher44.uuid.uuid4
import kotlinx.serialization.Serializable

/**
 * Represents a project in the tracking system.
 * @property name The unique name of the project.
 * @property closed Indicates if the project is marked as finished/closed.
 */
@Serializable
data class Project(
    val id: String = uuid4().toString(),
    val name: String,
    val closed: Boolean = false
) {
    init {
        require(name.isNotBlank()) { "Project name cannot be empty" }
    }

    /** Returns a human-readable representation of this value. */
    override fun toString(): String = "Project(id=$id, name='$name', closed=$closed)"
}
