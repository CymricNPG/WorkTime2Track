package net.npg.wt2t.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.npg.wt2t.ui.component.PlatformVerticalScrollbar
import net.npg.wt2t.ui.viewmodel.TaskListViewModel
import org.jetbrains.compose.resources.stringResource
import worktime2track.shared.generated.resources.*

/**
 * Screen for viewing and managing tasks within a specific project.
 * 
 * @param viewModel The ViewModel for task list state and actions.
 * @param onEditTask Callback to navigate to the task edit screen.
 * @param onBack Callback to navigate back to the previous screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel,
    onEditTask: (String?, String?) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.projectId == null) {
                            stringResource(Res.string.projectless_tasks)
                        } else {
                            uiState.project?.name ?: stringResource(Res.string.projects_title)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEditTask(null, uiState.projectId) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.add))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Error Message
            if (uiState.errorMessage != null) {
                Snackbar(
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text(stringResource(Res.string.ok))
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(stringResource(uiState.errorMessage!!))
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 12.dp),
                    state = lazyListState
                ) {
                    items(uiState.tasks) { task ->
                        ListItem(
                            headlineContent = { Text(task.name) },
                            supportingContent = {
                                val status = mutableListOf<String>()
                                if (task.freeTime) status.add(stringResource(Res.string.free_time))
                                if (task.closed) status.add(stringResource(Res.string.closed))
                                if (status.isNotEmpty()) Text(status.joinToString(", "))
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { onEditTask(task.id, task.projectId) }) {
                                        Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.edit))
                                    }
                                    IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.delete))
                                    }
                                }
                            }
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
