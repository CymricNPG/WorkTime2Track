package net.npg.wt2t.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.npg.wt2t.ui.component.PlatformVerticalScrollbar
import net.npg.wt2t.ui.component.StandardCard
import net.npg.wt2t.ui.format.formatForDisplay
import net.npg.wt2t.ui.theme.LocalLanguageState
import net.npg.wt2t.ui.viewmodel.EditDayViewModel
import net.npg.wt2t.ui.viewmodel.EditableTimeEntry
import org.jetbrains.compose.resources.stringResource
import worktime2track.shared.generated.resources.*

/**
 * Screen for editing all time entries and the work target for a single day.
 *
 * @param viewModel ViewModel for the editable day draft.
 * @param onDeleted Callback invoked after the complete day was deleted.
 * @param onBack Callback to navigate back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDayScreen(
    viewModel: EditDayViewModel,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalLanguageState.current.language
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(Res.string.edit_day_saved)
    val formattedDate = uiState.date.formatForDisplay(language)
    val adjustmentAvailabilityByEntryId = uiState.adjustmentAvailabilityByEntryId
    val entryListState = rememberLazyListState()
    var isDeleteConfirmationVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar(savedMessage)
        }
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onDeleted()
    }

    if (isDeleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { isDeleteConfirmationVisible = false },
            title = { Text(stringResource(Res.string.edit_day_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.edit_day_delete_confirm_message, formattedDate)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleteConfirmationVisible = false
                        viewModel.deleteDay()
                    },
                ) {
                    Text(stringResource(Res.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteConfirmationVisible = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        enabled = !uiState.isSaving && !uiState.isDeleting,
                        onClick = onBack,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            EditDayHeader(
                date = formattedDate,
                canAddEntry = uiState.tasks.isNotEmpty() &&
                        !uiState.isSaving &&
                        !uiState.isDeleting &&
                        !uiState.isDeleted,
                canDeleteDay = uiState.canDeleteDay && !uiState.isSaving && !uiState.isDeleted,
                isDeleting = uiState.isDeleting,
                onAddEntry = viewModel::addEntry,
                onDeleteDay = { isDeleteConfirmationVisible = true },
            )
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else {
                DurationEditor(
                    title = stringResource(Res.string.target_time),
                    hours = uiState.targetHours,
                    minutes = uiState.targetMinutes,
                    onHoursChange = viewModel::updateTargetHours,
                    onMinutesChange = viewModel::updateTargetMinutes,
                )
                DurationEditor(
                    title = stringResource(Res.string.pause),
                    hours = uiState.breakHours,
                    minutes = uiState.breakMinutes,
                    onHoursChange = viewModel::updateBreakHours,
                    onMinutesChange = viewModel::updateBreakMinutes,
                )

                uiState.errorMessage?.let { message ->
                    Text(
                        text = stringResource(message),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 12.dp),
                        state = entryListState,
                    ) {
                        items(uiState.entries, key = EditableTimeEntry::id) { entry ->
                            EditTimeEntryItem(
                                entry = entry,
                                tasks = uiState.tasks,
                                projects = uiState.projects,
                                adjustmentMinutes = uiState.timeAdjustmentMinutes,
                                adjustmentAvailability = adjustmentAvailabilityByEntryId.getValue(entry.id),
                                onTaskChange = { viewModel.updateEntryTask(entry.id, it) },
                                onStartChange = { viewModel.updateEntryStart(entry.id, it) },
                                onEndChange = { viewModel.updateEntryEnd(entry.id, it) },
                                onAdjustTime = { boundary, direction ->
                                    viewModel.adjustEntryTime(entry.id, boundary, direction)
                                },
                                onDescriptionChange = { viewModel.updateEntryDescription(entry.id, it) },
                                onDelete = { viewModel.deleteEntry(entry.id) },
                            )
                        }
                    }

                    PlatformVerticalScrollbar(
                        listState = entryListState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        enabled = uiState.hasChanges && !uiState.isSaving && !uiState.isDeleting,
                        onClick = viewModel::discardChanges,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(Res.string.discard))
                    }
                    Button(
                        enabled = uiState.hasChanges && !uiState.isSaving && !uiState.isDeleting,
                        onClick = viewModel::saveDay,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(Res.string.save))
                    }
                }
            }
        }
    }
}

/** Displays the date and the actions that operate on the edited day. */
@Composable
private fun EditDayHeader(
    date: String,
    canAddEntry: Boolean,
    canDeleteDay: Boolean,
    isDeleting: Boolean,
    onAddEntry: () -> Unit,
    onDeleteDay: () -> Unit,
) {
    StandardCard {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (canDeleteDay) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(12.dp).size(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    IconButton(onClick = onDeleteDay) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.delete))
                    }
                }
            }
            IconButton(
                enabled = canAddEntry,
                onClick = onAddEntry,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.add))
            }
        }
    }
}

/** Displays an editable duration in hours and minutes. */
@Composable
private fun DurationEditor(
    title: String,
    hours: String,
    minutes: String,
    onHoursChange: (String) -> Unit,
    onMinutesChange: (String) -> Unit,
) {
    StandardCard {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = hours,
                onValueChange = onHoursChange,
                label = { Text(stringResource(Res.string.hours)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(88.dp).weight(1f),
            )
            OutlinedTextField(
                value = minutes,
                onValueChange = onMinutesChange,
                label = { Text(stringResource(Res.string.minutes)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = (minutes.toIntOrNull() ?: 0) >= MINUTES_PER_HOUR,
                modifier = Modifier.width(88.dp).weight(1f),
            )
        }
    }
}

private const val MINUTES_PER_HOUR = 60