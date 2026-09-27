package net.npg.wt2t.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.npg.wt2t.domain.service.DatabaseExportRequest
import net.npg.wt2t.domain.service.DatabaseImportRequest
import net.npg.wt2t.domain.usecase.BackupUseCase

/** Identifies the result message produced by a database transfer. */
enum class DatabaseTransferMessage {
    EXPORT_SUCCEEDED,
    EXPORT_FAILED,
    IMPORT_SUCCEEDED,
    IMPORT_FAILED,
    DELETE_SUCCEEDED,
    DELETE_FAILED,
}

/** Represents the observable UI state for the menu. */
data class MenuUiState(
    val isTransferRunning: Boolean = false,
    val isImportConfirmationVisible: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false,
    val transferMessage: DatabaseTransferMessage? = null,
    val canExportDatabase: Boolean = false,
    val canImportDatabase: Boolean = false,
)

/** Manages database transfer state and user actions for the menu. */
class MenuViewModel(
    private val backupUseCase: BackupUseCase,
    private val exportDatabaseFile: ((DatabaseExportRequest) -> Unit)?,
    private val importDatabaseFile: ((DatabaseImportRequest) -> Unit)?,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        MenuUiState(
            canExportDatabase = exportDatabaseFile != null,
            canImportDatabase = importDatabaseFile != null,
        ),
    )
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    /** Exports a complete database backup through the platform file picker. */
    fun exportDatabase() {
        val fileExporter = exportDatabaseFile ?: return
        if (_uiState.value.isTransferRunning) return

        _uiState.update { it.copy(isTransferRunning = true) }
        viewModelScope.launch {
            try {
                val content = backupUseCase.export()
                fileExporter(
                    DatabaseExportRequest(DATABASE_EXPORT_FILE_NAME, content, ::completeExport),
                )
            } catch (exception: Exception) {
                logBoundaryException(exception, "Database export preparation failed")
                finishTransfer(DatabaseTransferMessage.EXPORT_FAILED)
            }
        }
    }

    /** Shows the confirmation prompt before importing a database backup. */
    fun requestDatabaseImport() {
        if (
            importDatabaseFile == null ||
            _uiState.value.isTransferRunning ||
            _uiState.value.isDeleteConfirmationVisible
        ) return
        _uiState.update { it.copy(isImportConfirmationVisible = true) }
    }

    /** Dismisses the database import confirmation prompt. */
    fun dismissDatabaseImport() {
        _uiState.update { it.copy(isImportConfirmationVisible = false) }
    }

    /** Confirms and starts importing a database backup through the platform file picker. */
    fun confirmDatabaseImport() {
        val fileImporter = importDatabaseFile ?: return
        if (_uiState.value.isTransferRunning) return

        _uiState.update {
            it.copy(
                isImportConfirmationVisible = false,
                isTransferRunning = true,
            )
        }
        try {
            fileImporter(DatabaseImportRequest(::completeImportSelection))
        } catch (exception: Exception) {
            logBoundaryException(exception, "Database import file selection could not be started")
            finishTransfer(DatabaseTransferMessage.IMPORT_FAILED)
        }
    }

    /** Shows the confirmation prompt before deleting all persisted data. */
    fun requestDatabaseDeletion() {
        if (_uiState.value.isTransferRunning || _uiState.value.isImportConfirmationVisible) return
        _uiState.update { it.copy(isDeleteConfirmationVisible = true) }
    }

    /** Dismisses the database deletion confirmation prompt. */
    fun dismissDatabaseDeletion() {
        _uiState.update { it.copy(isDeleteConfirmationVisible = false) }
    }

    /** Deletes all persisted data after the confirmation prompt has been shown. */
    fun confirmDatabaseDeletion() {
        if (!_uiState.value.isDeleteConfirmationVisible || _uiState.value.isTransferRunning) return
        _uiState.update {
            it.copy(
                isDeleteConfirmationVisible = false,
                isTransferRunning = true,
            )
        }
        viewModelScope.launch {
            val message = runCatching {
                backupUseCase.recreateDatabase()
            }.fold(
                onSuccess = { DatabaseTransferMessage.DELETE_SUCCEEDED },
                onFailure = { exception ->
                    logBoundaryException(exception, "Database deletion failed")
                    DatabaseTransferMessage.DELETE_FAILED
                },
            )
            finishTransfer(message)
        }
    }

    /** Marks the current transfer message as displayed. */
    fun clearTransferMessage() {
        _uiState.update { it.copy(transferMessage = null) }
    }

    private fun completeExport(result: Result<Boolean>) {
        val message = result.fold(
            onSuccess = { wasSaved ->
                if (wasSaved) DatabaseTransferMessage.EXPORT_SUCCEEDED else null
            },
            onFailure = { exception ->
                logBoundaryException(exception, "Database export file transfer failed")
                DatabaseTransferMessage.EXPORT_FAILED
            },
        )
        finishTransfer(message)
    }

    private fun completeImportSelection(result: Result<ByteArray?>) {
        val content = result.getOrElse { exception ->
            logBoundaryException(exception, "Database import file transfer failed")
            finishTransfer(DatabaseTransferMessage.IMPORT_FAILED)
            return
        }
        if (content == null) {
            finishTransfer()
            return
        }

        viewModelScope.launch {
            val message = runCatching {
                backupUseCase.import(content)
            }.fold(
                onSuccess = { DatabaseTransferMessage.IMPORT_SUCCEEDED },
                onFailure = { exception ->
                    logBoundaryException(exception, "Database import failed")
                    DatabaseTransferMessage.IMPORT_FAILED
                },
            )
            finishTransfer(message)
        }
    }

    private fun finishTransfer(message: DatabaseTransferMessage? = null) {
        _uiState.update {
            it.copy(
                isTransferRunning = false,
                transferMessage = message,
            )
        }
    }
}

private const val DATABASE_EXPORT_FILE_NAME = "worktime2track-backup.json"
