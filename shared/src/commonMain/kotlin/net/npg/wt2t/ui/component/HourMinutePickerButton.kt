package net.npg.wt2t.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource
import worktime2track.shared.generated.resources.Res
import worktime2track.shared.generated.resources.cancel
import worktime2track.shared.generated.resources.dial_input
import worktime2track.shared.generated.resources.keyboard_input
import worktime2track.shared.generated.resources.ok
import worktime2track.shared.generated.resources.select_time

/**
 * Displays a labeled button that selects hours and minutes in a 24-hour dialog.
 *
 * [displayText] lets callers format clock times and durations independently of
 * the valid picker [value]. Only confirmation invokes [onTimeSelected].
 */
@Composable
internal fun HourMinutePickerButton(
    label: String,
    displayText: String,
    value: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var initialTime by remember { mutableStateOf<LocalTime?>(null) }

    LaunchedEffect(enabled) {
        if (!enabled) initialTime = null
    }

    OutlinedButton(
        onClick = { initialTime = value },
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    if (enabled) {
        initialTime?.let { time ->
            HourMinutePickerDialog(
                title = label,
                initialTime = time,
                onDismiss = { initialTime = null },
                onConfirm = { selectedTime ->
                    initialTime = null
                    onTimeSelected(selectedTime)
                },
            )
        }
    }
}

/** Keeps the picker draft local until the user confirms it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HourMinutePickerDialog(
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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (usesTextInput) {
                    TimeInput(state = timePickerState)
                } else {
                    // The dialog is narrower than a landscape picker, even on wide screens.
                    // https://kotlinlang.org/api/compose-multiplatform/material3/androidx.compose.material3/-time-picker-layout-type/-companion/
                    TimePicker(
                        state = timePickerState,
                        layoutType = TimePickerLayoutType.Vertical,
                    )
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
