package net.npg.wt2t.ui.format

/** Returns a task name prefixed by its project name when available. */
internal fun formatProjectTaskName(projectName: String?, taskName: String): String =
    projectName?.let { "$it / $taskName" } ?: taskName
