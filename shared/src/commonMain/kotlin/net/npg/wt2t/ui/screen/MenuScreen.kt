package net.npg.wt2t.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import net.npg.wt2t.ui.viewmodel.DatabaseTransferMessage
import net.npg.wt2t.ui.viewmodel.MenuViewModel
import org.jetbrains.compose.resources.stringResource
import worktime2track.shared.generated.resources.Res
import worktime2track.shared.generated.resources.back
import worktime2track.shared.generated.resources.cancel
import worktime2track.shared.generated.resources.config_subtitle
import worktime2track.shared.generated.resources.config_title
import worktime2track.shared.generated.resources.database_delete_confirm_message
import worktime2track.shared.generated.resources.database_delete_confirm_title
import worktime2track.shared.generated.resources.database_delete_failed
import worktime2track.shared.generated.resources.database_delete_subtitle
import worktime2track.shared.generated.resources.database_delete_title
import worktime2track.shared.generated.resources.database_export_failed
import worktime2track.shared.generated.resources.database_export_subtitle
import worktime2track.shared.generated.resources.database_export_succeeded
import worktime2track.shared.generated.resources.database_export_title
import worktime2track.shared.generated.resources.database_import_confirm_message
import worktime2track.shared.generated.resources.database_import_confirm_title
import worktime2track.shared.generated.resources.database_import_failed
import worktime2track.shared.generated.resources.database_import_subtitle
import worktime2track.shared.generated.resources.database_import_succeeded
import worktime2track.shared.generated.resources.database_import_title
import worktime2track.shared.generated.resources.help_subtitle
import worktime2track.shared.generated.resources.help_title
import worktime2track.shared.generated.resources.impressum_subtitle
import worktime2track.shared.generated.resources.impressum_title
import worktime2track.shared.generated.resources.library_subtitle
import worktime2track.shared.generated.resources.library_title
import worktime2track.shared.generated.resources.menu_title

/**
 * Main menu screen providing navigation to various sub-screens.
 *
 * @param viewModel ViewModel that manages database transfers and screen state.
 * @param onNavigateToConfig Callback to navigate to configuration.
 * @param onNavigateToHelp Callback to navigate to help.
 * @param onNavigateToImpressum Callback to navigate to impressum.
 * @param onNavigateToLibrary Callback to navigate to the library overview.
 * @param onDatabaseRecreated Callback to rebuild the application after replacing its database.
 * @param onBack Callback to navigate back to the previous screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    viewModel: MenuViewModel,
    onNavigateToConfig: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToImpressum: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onDatabaseRecreated: () -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val transferMessage = uiState.transferMessage
    val transferMessageResource = when (transferMessage) {
        DatabaseTransferMessage.EXPORT_SUCCEEDED -> Res.string.database_export_succeeded
        DatabaseTransferMessage.EXPORT_FAILED -> Res.string.database_export_failed
        DatabaseTransferMessage.IMPORT_SUCCEEDED -> Res.string.database_import_succeeded
        DatabaseTransferMessage.IMPORT_FAILED -> Res.string.database_import_failed
        DatabaseTransferMessage.DELETE_FAILED -> Res.string.database_delete_failed
        DatabaseTransferMessage.DELETE_SUCCEEDED,
        null,
        -> null
    }
    val localizedTransferMessage = transferMessageResource?.let { stringResource(it) }

    LaunchedEffect(transferMessage) {
        if (transferMessage == DatabaseTransferMessage.DELETE_SUCCEEDED) {
            onDatabaseRecreated()
        } else {
            localizedTransferMessage?.let { snackbarHostState.showSnackbar(it) }
            if (transferMessage == DatabaseTransferMessage.DELETE_FAILED) {
                onDatabaseRecreated()
            }
        }
        if (transferMessage != null) viewModel.clearTransferMessage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.menu_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            MenuItem(
                icon = Icons.Default.Settings,
                title = stringResource(Res.string.config_title),
                subtitle = stringResource(Res.string.config_subtitle),
                onClick = onNavigateToConfig
            )
            MenuItem(
                icon = Icons.Default.FileUpload,
                title = stringResource(Res.string.database_export_title),
                subtitle = stringResource(Res.string.database_export_subtitle),
                enabled = !uiState.isTransferRunning && uiState.canExportDatabase,
                onClick = viewModel::exportDatabase,
            )
            MenuItem(
                icon = Icons.Default.FileDownload,
                title = stringResource(Res.string.database_import_title),
                subtitle = stringResource(Res.string.database_import_subtitle),
                enabled = !uiState.isTransferRunning && uiState.canImportDatabase,
                onClick = viewModel::requestDatabaseImport,
            )
            MenuItem(
                icon = Icons.Default.DeleteForever,
                title = stringResource(Res.string.database_delete_title),
                subtitle = stringResource(Res.string.database_delete_subtitle),
                enabled = !uiState.isTransferRunning,
                onClick = viewModel::requestDatabaseDeletion,
            )
            MenuItem(
                icon = Icons.Default.Info,
                title = stringResource(Res.string.help_title),
                subtitle = stringResource(Res.string.help_subtitle),
                onClick = onNavigateToHelp
            )
            MenuItem(
                icon = Icons.Default.Build,
                title = stringResource(Res.string.impressum_title),
                subtitle = stringResource(Res.string.impressum_subtitle),
                onClick = onNavigateToImpressum
            )
            MenuItem(
                icon = Icons.AutoMirrored.Filled.List,
                title = stringResource(Res.string.library_title),
                subtitle = stringResource(Res.string.library_subtitle),
                onClick = onNavigateToLibrary,
            )
        }
    }

    if (uiState.isImportConfirmationVisible) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDatabaseImport,
            title = { Text(stringResource(Res.string.database_import_confirm_title)) },
            text = { Text(stringResource(Res.string.database_import_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDatabaseImport,
                ) {
                    Text(stringResource(Res.string.database_import_title))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDatabaseImport) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    if (uiState.isDeleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDatabaseDeletion,
            title = { Text(stringResource(Res.string.database_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.database_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDatabaseDeletion,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(Res.string.database_delete_title))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDatabaseDeletion) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}

/** Displays the menu item UI element. */
@Composable
private fun MenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        colors = ListItemDefaults.colors(
            headlineColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            supportingColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            leadingIconColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
    )
}
