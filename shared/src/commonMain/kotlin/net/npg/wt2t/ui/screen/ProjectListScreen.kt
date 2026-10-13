package net.npg.wt2t.ui.screen

import androidx.compose.foundation.clickable
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
import net.npg.wt2t.ui.viewmodel.ProjectListViewModel
import org.jetbrains.compose.resources.stringResource
import worktime2track.shared.generated.resources.*

/**
 * Screen for viewing and managing projects.
 * 
 * @param viewModel The ViewModel for project list state and actions.
 * @param onNavigateToTasks Callback to navigate to a specific project's task list.
 * @param onEditProject Callback to navigate to the project edit screen.
 * @param onBack Callback to navigate back to the previous screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    viewModel: ProjectListViewModel,
    onNavigateToTasks: (String?) -> Unit,
    onEditProject: (String?) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.projects_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEditProject(null) }) {
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
                    // Eintrag für projektlose Tasks
                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(Res.string.projectless_tasks)) },
                            modifier = Modifier.clickable { onNavigateToTasks(null) }
                        )
                        HorizontalDivider()
                    }

                    items(uiState.projects) { project ->
                        ListItem(
                            headlineContent = { Text(project.name) },
                            supportingContent = { if (project.closed) Text(stringResource(Res.string.closed)) },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { onEditProject(project.id) }) {
                                        Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.edit))
                                    }
                                    IconButton(onClick = { viewModel.deleteProject(project.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.delete))
                                    }
                                }
                            },
                            modifier = Modifier.clickable { onNavigateToTasks(project.id) }
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
