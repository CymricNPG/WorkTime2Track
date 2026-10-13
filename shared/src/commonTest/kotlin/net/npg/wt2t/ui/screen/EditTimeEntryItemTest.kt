package net.npg.wt2t.ui.screen

import androidx.compose.ui.unit.dp
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditTimeEntryItemTest {

    @Test
    fun `time controls stack only below the wide layout breakpoint`() {
        assertTrue(usesStackedTimeLayout(599.dp))
        assertFalse(usesStackedTimeLayout(600.dp))
    }

    @Test
    fun `note icon is shown for a nonblank description`() {
        val hasDescription = hasTaskDescription("Implementation details")

        assertTrue(hasDescription)
    }

    @Test
    fun `note icon is hidden when description is blank`() {
        val hasDescription = hasTaskDescription(" \n\t")

        assertFalse(hasDescription)
    }

    @Test
    fun `task label contains project and task name`() {
        val inputTask = Task(id = "task", name = "Implementation", projectId = "project")
        val inputProject = Project(id = "project", name = "WorkTime2Track")

        val actualLabel = formatTaskLabel(inputTask, inputProject)

        assertEquals("WorkTime2Track/Implementation", actualLabel)
    }

    @Test
    fun `projectless task label contains only task name`() {
        val inputTask = Task(id = "task", name = "Vacation")

        val actualLabel = formatTaskLabel(inputTask, null)

        assertEquals("Vacation", actualLabel)
    }
}
