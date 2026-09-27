package net.npg.wt2t.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.BookingAdjustmentDirection
import net.npg.wt2t.ui.component.StandardCard
import net.npg.wt2t.ui.viewmodel.EditableTimeBoundary
import net.npg.wt2t.ui.viewmodel.EditableTimeEntry
import net.npg.wt2t.ui.viewmodel.TimeAdjustmentAvailability
import org.jetbrains.compose.resources.stringResource
import worktime2track.shared.generated.resources.Res
import worktime2track.shared.generated.resources.cancel
import worktime2track.shared.generated.resources.delete
import worktime2track.shared.generated.resources.edit_day_delete_entry_title
import worktime2track.shared.generated.resources.edit_day_description
import worktime2track.shared.generated.resources.task_has_description

/** Displays the edit time entry item UI element. */
@Composable
internal fun EditTimeEntryItem(
    entry: EditableTimeEntry,
    tasks: List<Task>,
    projects: Map<String, Project>,
    adjustmentMinutes: Int,
    adjustmentAvailability: TimeAdjustmentAvailability,
    onTaskChange: (String) -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onAdjustTime: (EditableTimeBoundary, BookingAdjustmentDirection) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var isDeleteDialogVisible by remember { mutableStateOf(false) }

    StandardCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TaskSelector(
                    taskId = entry.taskId,
                    tasks = tasks,
                    projects = projects,
                    hasDescription = hasTaskDescription(entry.description),
                    onTaskChange = onTaskChange,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { isDeleteDialogVisible = true }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.delete))
                }
            }
            Spacer(Modifier.height(8.dp))
            EditDayTimeEditor(
                start = entry.start,
                end = entry.end,
                adjustmentMinutes = adjustmentMinutes,
                adjustmentAvailability = adjustmentAvailability,
                onStartChange = onStartChange,
                onEndChange = onEndChange,
                onAdjustTime = onAdjustTime,
            )
            OutlinedTextField(
                value = entry.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(Res.string.edit_day_description)) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }

    if (isDeleteDialogVisible) {
        AlertDialog(
            onDismissRequest = { isDeleteDialogVisible = false },
            title = { Text(stringResource(Res.string.edit_day_delete_entry_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleteDialogVisible = false
                        onDelete()
                    },
                ) {
                    Text(stringResource(Res.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteDialogVisible = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}

/** Displays the task selector UI element. */
@Composable
private fun TaskSelector(
    taskId: String,
    tasks: List<Task>,
    projects: Map<String, Project>,
    hasDescription: Boolean,
    onTaskChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val selectedTask = tasks.find { it.id == taskId }

    Column(modifier = modifier) {
        OutlinedButton(
            onClick = { isExpanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = selectedTask?.let { task ->
                    formatTaskLabel(task, task.projectId?.let(projects::get))
                }.orEmpty(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (hasDescription) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Note,
                    contentDescription = stringResource(Res.string.task_has_description),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            tasks.forEach { task ->
                DropdownMenuItem(
                    text = {
                        Text(formatTaskLabel(task, task.projectId?.let(projects::get)))
                    },
                    onClick = {
                        isExpanded = false
                        onTaskChange(task.id)
                    },
                )
            }
        }
    }
    Spacer(Modifier.width(8.dp))
}

/** Returns whether the editable time entry contains a visible description. */
internal fun hasTaskDescription(description: String): Boolean = description.isNotBlank()

/** Returns a task name prefixed by its project name when available. */
internal fun formatTaskLabel(task: Task, project: Project?): String =
    project?.let { "${it.name}/${task.name}" } ?: task.name
