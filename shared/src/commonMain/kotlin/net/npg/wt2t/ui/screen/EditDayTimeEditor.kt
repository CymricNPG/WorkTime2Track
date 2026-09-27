package net.npg.wt2t.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime
import net.npg.wt2t.data.model.BookingAdjustmentDirection
import net.npg.wt2t.ui.format.formatForDisplay
import net.npg.wt2t.ui.theme.LocalLanguageState
import net.npg.wt2t.ui.viewmodel.EditableTimeBoundary
import net.npg.wt2t.ui.viewmodel.TimeAdjustmentAvailability
import org.jetbrains.compose.resources.stringResource
import worktime2track.shared.generated.resources.Res
import worktime2track.shared.generated.resources.cancel
import worktime2track.shared.generated.resources.dial_input
import worktime2track.shared.generated.resources.end_time
import worktime2track.shared.generated.resources.keyboard_input
import worktime2track.shared.generated.resources.ok
import worktime2track.shared.generated.resources.select_time
import worktime2track.shared.generated.resources.start_time
import worktime2track.shared.generated.resources.time_adjust_earlier
import worktime2track.shared.generated.resources.time_adjust_earlier_description
import worktime2track.shared.generated.resources.time_adjust_later
import worktime2track.shared.generated.resources.time_adjust_later_description

/** Displays the editable start and end time controls. */
@Composable
internal fun EditDayTimeEditor(
    start: String,
    end: String,
    adjustmentMinutes: Int,
    adjustmentAvailability: TimeAdjustmentAvailability,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onAdjustTime: (EditableTimeBoundary, BookingAdjustmentDirection) -> Unit,
) {
    var editedTimeBoundary by remember { mutableStateOf<EditableTimeBoundary?>(null) }
    val language = LocalLanguageState.current.language
    val adjustmentFields = listOf(
        TimeAdjustmentFieldState(
            boundary = EditableTimeBoundary.Start,
            value = start,
            label = stringResource(Res.string.start_time),
            canAdjustEarlier = adjustmentAvailability.canAdjustStartEarlier,
            canAdjustLater = adjustmentAvailability.canAdjustStartLater,
        ),
        TimeAdjustmentFieldState(
            boundary = EditableTimeBoundary.End,
            value = end,
            label = stringResource(Res.string.end_time),
            canAdjustEarlier = adjustmentAvailability.canAdjustEndEarlier,
            canAdjustLater = adjustmentAvailability.canAdjustEndLater,
        ),
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        TimeAdjustmentFields(
            fields = adjustmentFields,
            language = language,
            adjustmentMinutes = adjustmentMinutes,
            usesStackedLayout = usesStackedTimeLayout(maxWidth),
            onTimeClick = { editedTimeBoundary = it },
            onAdjustTime = onAdjustTime,
        )
    }

    editedTimeBoundary?.let { boundary ->
        TimePickerDialog(
            title = when (boundary) {
                EditableTimeBoundary.Start -> stringResource(Res.string.start_time)
                EditableTimeBoundary.End -> stringResource(Res.string.end_time)
            },
            initialTime = when (boundary) {
                EditableTimeBoundary.Start -> start.parseLocalTimeOrDefault()
                EditableTimeBoundary.End -> end.parseLocalTimeOrDefault()
            },
            onDismiss = { editedTimeBoundary = null },
            onConfirm = { selectedTime ->
                editedTimeBoundary = null
                when (boundary) {
                    EditableTimeBoundary.Start -> onStartChange(selectedTime.toStorageText())
                    EditableTimeBoundary.End -> onEndChange(selectedTime.toStorageText())
                }
            },
        )
    }
}

/** Lays out both adjustment fields for compact or wide content. */
@Composable
private fun TimeAdjustmentFields(
    fields: List<TimeAdjustmentFieldState>,
    language: String,
    adjustmentMinutes: Int,
    usesStackedLayout: Boolean,
    onTimeClick: (EditableTimeBoundary) -> Unit,
    onAdjustTime: (EditableTimeBoundary, BookingAdjustmentDirection) -> Unit,
) {
    if (usesStackedLayout) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            fields.forEach { field ->
                TimeAdjustmentField(
                    field = field,
                    language = language,
                    adjustmentMinutes = adjustmentMinutes,
                    onTimeClick = onTimeClick,
                    onAdjustTime = onAdjustTime,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            fields.forEach { field ->
                TimeAdjustmentField(
                    field = field,
                    language = language,
                    adjustmentMinutes = adjustmentMinutes,
                    onTimeClick = onTimeClick,
                    onAdjustTime = onAdjustTime,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Displays one editable time flanked by its complete-step adjustment controls. */
@Composable
private fun TimeAdjustmentField(
    field: TimeAdjustmentFieldState,
    language: String,
    adjustmentMinutes: Int,
    onTimeClick: (EditableTimeBoundary) -> Unit,
    onAdjustTime: (EditableTimeBoundary, BookingAdjustmentDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val earlierDescription = stringResource(
        Res.string.time_adjust_earlier_description,
        field.label,
        adjustmentMinutes,
    )
    val laterDescription = stringResource(
        Res.string.time_adjust_later_description,
        field.label,
        adjustmentMinutes,
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AdjustmentButton(
            text = stringResource(Res.string.time_adjust_earlier, adjustmentMinutes),
            contentDescription = earlierDescription,
            enabled = field.canAdjustEarlier,
            onClick = { onAdjustTime(field.boundary, BookingAdjustmentDirection.EARLIER) },
        )
        TimeField(
            value = field.value,
            label = field.label,
            language = language,
            onClick = { onTimeClick(field.boundary) },
            modifier = Modifier.weight(1f),
        )
        AdjustmentButton(
            text = stringResource(Res.string.time_adjust_later, adjustmentMinutes),
            contentDescription = laterDescription,
            enabled = field.canAdjustLater,
            onClick = { onAdjustTime(field.boundary, BookingAdjustmentDirection.LATER) },
        )
    }
}

private data class TimeAdjustmentFieldState(
    val boundary: EditableTimeBoundary,
    val value: String,
    val label: String,
    val canAdjustEarlier: Boolean,
    val canAdjustLater: Boolean,
)

/** Displays one accessible time-adjustment action. */
@Composable
private fun AdjustmentButton(
    text: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Text(text)
    }
}

/** Displays the time field UI element. */
@Composable
private fun TimeField(
    value: String,
    label: String,
    language: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(
                text = value.parseLocalTimeOrNull()?.formatForDisplay(language).orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Returns whether time controls use separate rows for the available width. */
internal fun usesStackedTimeLayout(maxWidth: Dp): Boolean = maxWidth < WIDE_TIME_LAYOUT_MIN_WIDTH

/** Displays the time picker dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    var usesTextInput by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.select_time, title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (usesTextInput) {
                    TimeInput(state = timePickerState)
                } else {
                    TimePicker(state = timePickerState)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(LocalTime(timePickerState.hour, timePickerState.minute))
                },
            ) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        icon = {
            IconButton(onClick = { usesTextInput = !usesTextInput }) {
                if (usesTextInput) {
                    Icon(Icons.Default.AccessTime, contentDescription = stringResource(Res.string.dial_input))
                } else {
                    Icon(Icons.Default.Keyboard, contentDescription = stringResource(Res.string.keyboard_input))
                }
            }
        },
    )
}

/** Parses this text as a local time or returns midnight. */
private fun String.parseLocalTimeOrDefault(): LocalTime = parseLocalTimeOrNull() ?: LocalTime(0, 0)

/** Parses this text as a local time, or returns null when invalid. */
private fun String.parseLocalTimeOrNull(): LocalTime? = runCatching { LocalTime.parse(this) }.getOrNull()

/** Formats this time for storage and editing. */
private fun LocalTime.toStorageText(): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

private val WIDE_TIME_LAYOUT_MIN_WIDTH = 600.dp
