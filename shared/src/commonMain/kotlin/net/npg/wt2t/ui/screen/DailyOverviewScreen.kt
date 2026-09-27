package net.npg.wt2t.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import net.npg.wt2t.domain.service.ReportColumnHeaders
import net.npg.wt2t.domain.service.ReportText
import net.npg.wt2t.ui.component.PlatformVerticalScrollbar
import net.npg.wt2t.ui.component.StandardCard
import net.npg.wt2t.ui.format.formatForDisplay
import net.npg.wt2t.ui.format.formatProjectTaskName
import net.npg.wt2t.ui.theme.LocalLanguageState
import net.npg.wt2t.ui.viewmodel.DailyOverviewViewModel
import net.npg.wt2t.ui.viewmodel.DayEntry
import org.jetbrains.compose.resources.stringResource
import worktime2track.shared.generated.resources.*
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Screen for viewing the daily overview of work times.
 * 
 * @param viewModel The ViewModel for daily overview data.
 * @param onNavigateToReport Callback to export the report.
 * @param onBack Callback to navigate back to the previous screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyOverviewScreen(
    viewModel: DailyOverviewViewModel,
    onEditDay: (LocalDate) -> Unit,
    onNavigateToReport: (ReportText) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalLanguageState.current.language
    val lazyListState = rememberLazyListState()
    var isPeriodPickerVisible by remember { mutableStateOf(false) }
    var isDayPickerVisible by remember { mutableStateOf(false) }
    val reportText = ReportText(
        title = stringResource(
            Res.string.report_title_value,
            uiState.startDate.formatForDisplay(language),
            uiState.endDate.formatForDisplay(language)
        ),
        columnHeaders = ReportColumnHeaders(
            date = stringResource(Res.string.report_column_date),
            project = stringResource(Res.string.report_column_project),
            task = stringResource(Res.string.report_column_task),
            start = stringResource(Res.string.report_column_start),
            end = stringResource(Res.string.report_column_end),
            duration = stringResource(Res.string.report_column_duration),
        ),
        totalTime = stringResource(Res.string.report_total_time),
        overtime = stringResource(Res.string.overtime),
        unknownTask = stringResource(Res.string.unknown_task)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.overview_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            PeriodSelection(
                startDate = uiState.startDate,
                endDate = uiState.endDate,
                language = language,
                onClick = { isPeriodPickerVisible = true },
                onAddDay = {
                    viewModel.clearCreationError()
                    isDayPickerVisible = true
                },
                isAddDayEnabled = !uiState.isLoading && !uiState.isCreatingDay,
            )

            if (uiState.creationFailed) {
                Text(
                    text = stringResource(Res.string.overview_create_day_failed),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 12.dp),
                    state = lazyListState,
                ) {
                    items(uiState.dayEntries) { entry ->
                        DayEntryItem(
                            entry = entry,
                            language = language,
                            onClick = { onEditDay(entry.date) },
                        )
                    }
                }

                PlatformVerticalScrollbar(
                    listState = lazyListState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                )
            }

            Button(
                onClick = { onNavigateToReport(reportText) },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text(stringResource(Res.string.export_pdf))
            }
        }
    }

    if (isPeriodPickerVisible) {
        PeriodPickerDialog(
            startDate = uiState.startDate,
            endDate = uiState.endDate,
            onDismiss = { isPeriodPickerVisible = false },
            onConfirm = { startDate, endDate ->
                viewModel.updatePeriod(startDate, endDate)
                isPeriodPickerVisible = false
            },
        )
    }

    if (isDayPickerVisible) {
        EmptyDayPickerDialog(
            recordedDates = uiState.recordedDates,
            onDismiss = { isDayPickerVisible = false },
            onConfirm = { date ->
                viewModel.createEmptyDay(date)
                isDayPickerVisible = false
            },
        )
    }
}

/** Displays the period selection UI element. */
@Composable
private fun PeriodSelection(
    startDate: LocalDate,
    endDate: LocalDate,
    language: String,
    onClick: () -> Unit,
    onAddDay: () -> Unit,
    isAddDayEnabled: Boolean,
) {
    Row(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PeriodDateButton(
            label = stringResource(Res.string.start_date),
            date = startDate.formatForDisplay(language),
            modifier = Modifier.weight(1f),
            onClick = onClick,
        )
        Text(
            text = "–",
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        PeriodDateButton(
            label = stringResource(Res.string.end_date),
            date = endDate.formatForDisplay(language),
            modifier = Modifier.weight(1f),
            onClick = onClick,
        )
        IconButton(
            enabled = isAddDayEnabled,
            onClick = onAddDay,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(Res.string.add),
            )
        }
    }
}

/** Displays the period date button UI element. */
@Composable
private fun PeriodDateButton(
    label: String,
    date: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(date, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Displays the period picker dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodPickerDialog(
    startDate: LocalDate,
    endDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
) {
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = startDate.toUtcEpochMilliseconds(),
        initialSelectedEndDateMillis = endDate.toUtcEpochMilliseconds(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = pickerState.selectedStartDateMillis != null &&
                    pickerState.selectedEndDateMillis != null,
                onClick = {
                    val selectedStartDate = pickerState.selectedStartDateMillis?.toLocalDate()
                    val selectedEndDate = pickerState.selectedEndDateMillis?.toLocalDate()
                    if (selectedStartDate != null && selectedEndDate != null) {
                        onConfirm(selectedStartDate, selectedEndDate)
                    }
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
    ) {
        DateRangePicker(
            state = pickerState,
            title = {
                Text(
                    text = stringResource(Res.string.select_period),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
            modifier = Modifier.height(500.dp),
        )
    }
}

/** Lets the user select a date that has no stored bookings or daily values. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyDayPickerDialog(
    recordedDates: Set<LocalDate>,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val selectableDates = remember(recordedDates) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis.toLocalDate() !in recordedDates
        }
    }
    val pickerState = rememberDatePickerState(selectableDates = selectableDates)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = pickerState.selectedDateMillis != null,
                onClick = {
                    pickerState.selectedDateMillis
                        ?.toLocalDate()
                        ?.let(onConfirm)
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
    ) {
        DatePicker(
            state = pickerState,
            title = {
                Text(
                    text = stringResource(Res.string.select_day),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
        )
    }
}

/** Displays the day entry item UI element. */
@Composable
fun DayEntryItem(
    entry: DayEntry,
    language: String,
    onClick: () -> Unit = {},
) {
    StandardCard(modifier = Modifier.clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(entry.date.formatForDisplay(language), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(Res.string.total_value, formatDuration(entry.totalWorkedTime)),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                stringResource(Res.string.overtime_value, formatDuration(entry.overtime)),
                color = if (entry.overtime.isNegative()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(Res.string.break_time), style = MaterialTheme.typography.bodySmall)
                Text(formatDuration(entry.breakTime), style = MaterialTheme.typography.bodySmall)
            }

            if (entry.taskEntries.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                entry.taskEntries.forEach { taskEntry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formatProjectTaskName(
                                projectName = taskEntry.projectName,
                                taskName = taskEntry.taskName ?: stringResource(Res.string.unknown_task),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(formatDuration(taskEntry.duration), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

/** Formats duration. */
private fun formatDuration(duration: Duration): String {
    val totalMinutes = duration.inWholeMinutes
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return if (duration.isNegative()) {
        val absMins = kotlin.math.abs(totalMinutes)
        "-%02d:%02d".format(absMins / 60, absMins % 60)
    } else {
        "%02d:%02d".format(hours, mins)
    }
}

/** Converts this date to UTC epoch milliseconds at the start of day. */
private fun LocalDate.toUtcEpochMilliseconds(): Long =
    atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

/** Converts these epoch milliseconds to a date in UTC. */
private fun Long.toLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date
