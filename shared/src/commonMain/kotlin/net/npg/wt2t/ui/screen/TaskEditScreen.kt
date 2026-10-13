package net.npg.wt2t.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import net.npg.wt2t.ui.component.LabeledCheckbox
import net.npg.wt2t.ui.viewmodel.TaskViewModel
import org.jetbrains.compose.resources.stringResource
import worktime2track.shared.generated.resources.*

/**
 * Screen for creating or editing a task.
 * 
 * @param viewModel The ViewModel for task editing logic.
 * @param onBack Callback to navigate back to the previous screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    viewModel: TaskViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.id == null) stringResource(Res.string.add) else stringResource(Res.string.edit)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            if (uiState.errorMessage != null) {
                Text(stringResource(uiState.errorMessage!!), color = MaterialTheme.colorScheme.error)
            }

            TextField(
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text(stringResource(Res.string.task_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.saveTask() })
            )

            Spacer(Modifier.height(16.dp))

            LabeledCheckbox(
                label = stringResource(Res.string.free_time),
                checked = uiState.freeTime,
                onCheckedChange = { viewModel.updateFreeTime(it) }
            )

            LabeledCheckbox(
                label = stringResource(Res.string.closed),
                checked = uiState.closed,
                onCheckedChange = { viewModel.updateClosed(it) }
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { viewModel.saveTask() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.save))
            }
        }
    }
}
