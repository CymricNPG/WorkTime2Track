package net.npg.wt2t.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime
import net.npg.wt2t.data.model.BookingAdjustmentDirection
import net.npg.wt2t.ui.component.HourMinutePickerButton
import net.npg.wt2t.ui.format.formatForDisplay
import net.npg.wt2t.ui.theme.LocalLanguageState
import net.npg.wt2t.ui.viewmodel.EditableTimeBoundary
import net.npg.wt2t.ui.viewmodel.TimeAdjustmentAvailability
import org.jetbrains.compose.resources.stringResource
import worktime2track.shared.generated.resources.Res
import worktime2track.shared.generated.resources.end_time
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
            onTimeChange = { boundary, selectedTime ->
                when (boundary) {
                    EditableTimeBoundary.Start -> onStartChange(selectedTime.toStorageText())
                    EditableTimeBoundary.End -> onEndChange(selectedTime.toStorageText())
                }
            },
            onAdjustTime = onAdjustTime,
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
    onTimeChange: (EditableTimeBoundary, LocalTime) -> Unit,
    onAdjustTime: (EditableTimeBoundary, BookingAdjustmentDirection) -> Unit,
) {
    if (usesStackedLayout) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            fields.forEach { field ->
                TimeAdjustmentField(
                    field = field,
                    language = language,
                    adjustmentMinutes = adjustmentMinutes,
                    onTimeChange = onTimeChange,
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
                    onTimeChange = onTimeChange,
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
    onTimeChange: (EditableTimeBoundary, LocalTime) -> Unit,
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
        HourMinutePickerButton(
            label = field.label,
            displayText = field.value.parseLocalTimeOrNull()?.formatForDisplay(language).orEmpty(),
            value = field.value.parseLocalTimeOrDefault(),
            onTimeSelected = { onTimeChange(field.boundary, it) },
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

/** Returns whether time controls use separate rows for the available width. */
internal fun usesStackedTimeLayout(maxWidth: Dp): Boolean = maxWidth < WIDE_TIME_LAYOUT_MIN_WIDTH

/** Parses this text as a local time or returns midnight. */
private fun String.parseLocalTimeOrDefault(): LocalTime = parseLocalTimeOrNull() ?: LocalTime(0, 0)

/** Parses this text as a local time, or returns null when invalid. */
private fun String.parseLocalTimeOrNull(): LocalTime? = runCatching { LocalTime.parse(this) }.getOrNull()

/** Formats this time for storage and editing. */
private fun LocalTime.toStorageText(): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

private val WIDE_TIME_LAYOUT_MIN_WIDTH = 600.dp
