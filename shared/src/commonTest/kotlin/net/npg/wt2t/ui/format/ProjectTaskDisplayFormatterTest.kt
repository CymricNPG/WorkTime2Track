package net.npg.wt2t.ui.format

import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectTaskDisplayFormatterTest {

    @Test
    fun `project and task names are displayed in one label`() {
        val actualLabel = formatProjectTaskName(
            projectName = "Project",
            taskName = "Task",
        )

        assertEquals("Project / Task", actualLabel)
    }

    @Test
    fun `projectless task is displayed without a project placeholder`() {
        val actualLabel = formatProjectTaskName(
            projectName = null,
            taskName = "Task",
        )

        assertEquals("Task", actualLabel)
    }
}
