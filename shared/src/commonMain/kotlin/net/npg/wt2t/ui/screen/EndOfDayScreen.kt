package net.npg.wt2t.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.npg.wt2t.ui.format.formatForDisplay
import net.npg.wt2t.ui.component.PlatformVerticalScrollbar
import net.npg.wt2t.ui.theme.LocalLanguageState
import net.npg.wt2t.ui.viewmodel.EndOfDayViewModel
import org.jetbrains.compose.resources.stringResource
import worktime2track.shared.generated.resources.Res
import worktime2track.shared.generated.resources.back
import worktime2track.shared.generated.resources.break_time
import worktime2track.shared.generated.resources.date_value
import worktime2track.shared.generated.resources.duration_hours_minutes
import worktime2track.shared.generated.resources.end_of_day_title
import worktime2track.shared.generated.resources.end_time
import worktime2track.shared.generated.resources.hours
import worktime2track.shared.generated.resources.minutes
import worktime2track.shared.generated.resources.save
import worktime2track.shared.generated.resources.start_time
import worktime2track.shared.generated.resources.target_time
import worktime2track.shared.generated.resources.time_separator
import worktime2track.shared.generated.resources.worked_time
import kotlin.time.Duration

/** Displays current work progress and allows the user to complete the work day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndOfDayScreen(
    viewModel: EndOfDayViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalLanguageState.current.language
    val screenListState = rememberLazyListState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.end_of_day_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = !uiState.isSaving,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 12.dp),
                    state = screenListState,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item {
                        if (uiState.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    stringResource(Res.string.date_value, uiState.date.formatForDisplay(language)),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(Modifier.height(16.dp))

                                InfoRow(
                                    label = stringResource(Res.string.start_time),
                                    value = uiState.firstBooking?.formatForDisplay(language) ?: "--:--",
                                )
                                InfoRow(
                                    label = stringResource(Res.string.end_time),
                                    value = uiState.lastBooking?.formatForDisplay(language) ?: "--:--",
                                )
                                InfoRow(
                                    label = stringResource(Res.string.worked_time),
                                    value = formatDuration(uiState.workedTime),
                                )

                                Spacer(Modifier.height(32.dp))

                                DurationInput(
                                    title = stringResource(Res.string.target_time),
                                    totalMinutes = uiState.targetMinutes,
                                    enabled = uiState.isLoaded && !uiState.isSaving,
                                    onTimeChange = viewModel::updateTargetTime,
                                )

                                Spacer(Modifier.height(16.dp))

                                DurationInput(
                                    title = stringResource(Res.string.break_time),
                                    totalMinutes = uiState.breakMinutes,
                                    enabled = uiState.isLoaded && !uiState.isSaving,
                                    onTimeChange = viewModel::updateBreakTime,
                                )

                                uiState.errorMessage?.let { message ->
                                    Text(
                                        text = stringResource(message),
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 16.dp),
                                    )
                                }

                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                }

                PlatformVerticalScrollbar(
                    listState = screenListState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                )
            }

            if (!uiState.isLoading) {
                Button(
                    onClick = viewModel::saveAndEndDay,
                    enabled = uiState.isLoaded && !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(Res.string.save))
                    }
                }
            }
        }
    }
}

/** Displays an editable duration as hours and minutes. */
@Composable
private fun DurationInput(
    title: String,
    totalMinutes: Int,
    enabled: Boolean,
    onTimeChange: (hours: Int, minutes: Int) -> Unit,
) {
    var hours by remember(totalMinutes) { mutableStateOf((totalMinutes / MINUTES_PER_HOUR).toString()) }
    var minutes by remember(totalMinutes) { mutableStateOf((totalMinutes % MINUTES_PER_HOUR).toString()) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = hours,
                onValueChange = {
                    hours = it.filter(Char::isDigit)
                    onTimeChange(hours.toIntOrNull() ?: 0, minutes.toIntOrNull() ?: 0)
                },
                label = { Text(stringResource(Res.string.hours)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = enabled,
                modifier = Modifier.width(100.dp),
            )
            Text(stringResource(Res.string.time_separator), style = MaterialTheme.typography.headlineMedium)
            TextField(
                value = minutes,
                onValueChange = {
                    minutes = it.filter(Char::isDigit).take(MAX_MINUTE_DIGITS)
                    val minuteValue = minutes.toIntOrNull() ?: 0
                    if (minuteValue < MINUTES_PER_HOUR) {
                        onTimeChange(hours.toIntOrNull() ?: 0, minuteValue)
                    }
                },
                label = { Text(stringResource(Res.string.minutes)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = enabled,
                isError = (minutes.toIntOrNull() ?: 0) >= MINUTES_PER_HOUR,
                modifier = Modifier.width(100.dp),
            )
        }
    }
}

/** Displays an end-of-day summary value. */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
}

/** Formats duration for display. */
@Composable
private fun formatDuration(duration: Duration): String {
    val totalMinutes = duration.inWholeMinutes
    val hours = totalMinutes / MINUTES_PER_HOUR
    val minutes = totalMinutes % MINUTES_PER_HOUR
    return stringResource(Res.string.duration_hours_minutes, hours, minutes)
}

private const val MINUTES_PER_HOUR = 60
private const val MAX_MINUTE_DIGITS = 2
