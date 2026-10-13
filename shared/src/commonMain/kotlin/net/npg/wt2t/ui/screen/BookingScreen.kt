package net.npg.wt2t.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.npg.wt2t.ui.component.PlatformVerticalScrollbar
import net.npg.wt2t.data.model.BookingAdjustmentDirection
import net.npg.wt2t.ui.format.formatForDisplay
import net.npg.wt2t.ui.format.formatProjectTaskName
import net.npg.wt2t.ui.theme.LocalLanguageState
import net.npg.wt2t.ui.viewmodel.BookingViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import worktime2track.shared.generated.resources.*
import kotlin.time.Duration

/**
 * Screen for booking work time. Displays a list of tasks and the currently active booking.
 * 
 * @param viewModel The ViewModel for booking logic.
 * @param onNavigateToOverview Callback to navigate to the daily overview.
 * @param onNavigateToProjects Callback to navigate to the project list.
 * @param onNavigateToEndOfDay Callback to navigate to the end-of-day screen.
 * @param onNavigateToMenu Callback to navigate to the menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    viewModel: BookingViewModel,
    onNavigateToOverview: () -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToEndOfDay: () -> Unit,
    onNavigateToMenu: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalLanguageState.current.language
    val listScrollState = rememberScrollState()
    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val zeroMinuteBookingMessage = if (uiState.isZeroMinuteBookingSaved) {
        stringResource(Res.string.booking_zero_minute_saved)
    } else {
        null
    }

    LaunchedEffect(viewModel) {
        viewModel.refreshBookingStartAdjustment()
        viewModel.refreshOvertime()
    }

    LaunchedEffect(zeroMinuteBookingMessage) {
        zeroMinuteBookingMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearZeroMinuteBookingSaved()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.wt2t_app_logo),
                            contentDescription = stringResource(Res.string.app_name),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(48.dp),
                        )
                        Column(
                            modifier = Modifier.padding(start = 8.dp).weight(1f),
                        ) {
                            OvertimeSummaryValue(
                                label = stringResource(
                                    Res.string.overtime_month_value,
                                    formatOvertime(uiState.monthOvertime),
                                ),
                                isNegative = uiState.monthOvertime.isNegative(),
                            )
                            OvertimeSummaryValue(
                                label = stringResource(
                                    Res.string.overtime_year_value,
                                    formatOvertime(uiState.yearOvertime),
                                ),
                                isNegative = uiState.yearOvertime.isNegative(),
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToOverview) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = stringResource(Res.string.overview_title),
                        )
                    }
                    IconButton(onClick = onNavigateToProjects) {
                        Icon(
                            Icons.AutoMirrored.Filled.NoteAdd,
                            contentDescription = stringResource(Res.string.projects_title)
                        )
                    }
                    IconButton(onClick = onNavigateToMenu) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(Res.string.menu_title))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (uiState.canEndDay) onNavigateToEndOfDay()
                },
                modifier = Modifier.semantics {
                    if (!uiState.canEndDay) disabled()
                },
                containerColor = if (uiState.canEndDay) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (uiState.canEndDay) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.error
                },
            ) {
                Icon(Icons.Default.Home, contentDescription = stringResource(Res.string.logoff))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Aktiver Task Bereich
            if (uiState.activeTime != null) {
                val taskId = uiState.activeTime!!.taskId
                val task = uiState.tasks.find { it.id == taskId }
                val project = task?.projectId?.let { uiState.projects[it] }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(
                                    Res.string.active_task,
                                    formatProjectTaskName(
                                        projectName = project?.name,
                                        taskName = task?.name ?: stringResource(Res.string.unknown_task),
                                    ),
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Button(onClick = { viewModel.stopBooking() }) {
                                Text(stringResource(Res.string.stop_booking))
                            }
                        }
                        val adjustmentMinutes = uiState.bookingStartAdjustmentMinutes
                        val earlierAdjustmentDescription = stringResource(
                            Res.string.booking_start_adjust_earlier_description,
                            adjustmentMinutes,
                        )
                        val laterAdjustmentDescription = stringResource(
                            Res.string.booking_start_adjust_later_description,
                            adjustmentMinutes,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                stringResource(
                                    Res.string.start_time_value,
                                    uiState.activeTime!!.start.formatForDisplay(language)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            OutlinedButton(
                                onClick = {
                                    viewModel.adjustBookingStart(BookingAdjustmentDirection.EARLIER)
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .semantics {
                                        contentDescription = earlierAdjustmentDescription
                                    },
                                enabled = uiState.canAdjustBookingStartEarlier &&
                                    !uiState.isAdjustingBookingStart,
                            ) {
                                Text(
                                    stringResource(
                                        Res.string.booking_start_adjust_earlier,
                                        adjustmentMinutes,
                                    ),
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.adjustBookingStart(BookingAdjustmentDirection.LATER)
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .semantics {
                                        contentDescription = laterAdjustmentDescription
                                    },
                                enabled = uiState.canAdjustBookingStartLater &&
                                    !uiState.isAdjustingBookingStart,
                            ) {
                                Text(
                                    stringResource(
                                        Res.string.booking_start_adjust_later,
                                        adjustmentMinutes,
                                    ),
                                )
                            }
                        }
                        uiState.bookingStartAdjustmentError?.let { errorMessage ->
                            Text(
                                text = stringResource(errorMessage),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        uiState.bookingStopError?.let { errorMessage ->
                            Text(
                                text = stringResource(errorMessage),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextField(
                                    value = uiState.note,
                                    onValueChange = { viewModel.updateNote(it) },
                                    label = { Text(stringResource(Res.string.note_hint)) },
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.addNote() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = stringResource(Res.string.send)
                                    )
                                }
                            }
                            formatLastNote(uiState.activeTime!!.description)?.let { lastNote ->
                                Text(
                                    text = lastNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,
                                )
                            }
                        }
                    }
                }
            } else {
                // Keine aktive Buchung
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(Res.string.no_active_booking), modifier = Modifier.weight(1f))
                    }
                }
            }

            // Task Liste
            Text(
                stringResource(Res.string.tasks_title),
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                style = MaterialTheme.typography.titleMedium
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 12.dp),
                    state = lazyListState,
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.tasks) { task ->
                        val project = task.projectId?.let { uiState.projects[it] }
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = formatProjectTaskName(project?.name, task.name),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            leadingContent = {
                                if (task.freeTime) {
                                    Icon(Icons.Default.Star, contentDescription = stringResource(Res.string.free_time))
                                } else {
                                    Icon(Icons.Default.Info, contentDescription = stringResource(Res.string.task_label))
                                }
                            },
                            modifier = Modifier.clickable { viewModel.startBooking(task.id) }
                        )
                    }
                }

                PlatformVerticalScrollbar(
                    listState = lazyListState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                )
            }
        }
    }
}

/** Displays one overtime total in the top-bar summary. */
@Composable
private fun OvertimeSummaryValue(
    label: String,
    isNegative: Boolean,
) {
    Text(
        text = label,
        color = if (isNegative) MaterialTheme.colorScheme.error else LocalContentColor.current,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Formats an overtime duration as a signed hour-and-minute value. */
private fun formatOvertime(duration: Duration): String {
    val totalMinutes = duration.inWholeMinutes
    val absoluteMinutes = kotlin.math.abs(totalMinutes)
    val sign = if (totalMinutes < 0) "-" else ""
    return "$sign%02d:%02d".format(absoluteMinutes / 60, absoluteMinutes % 60)
}

private const val LAST_NOTE_MAX_LENGTH = 40
private val noteLineBreakPattern = Regex("[\\r\\n]+")

/** Returns the most recent nonblank note for display. */
internal fun formatLastNote(notes: List<String>): String? = notes
    .lastOrNull()
    ?.replace(noteLineBreakPattern, " ")
    ?.take(LAST_NOTE_MAX_LENGTH)
