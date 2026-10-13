package net.npg.wt2t

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.npg.wt2t.di.initKoin
import net.npg.wt2t.domain.service.DatabaseExportRequest
import net.npg.wt2t.domain.service.DatabaseFileService
import net.npg.wt2t.domain.service.DatabaseImportRequest
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * Main entry point for the Desktop application.
 * It initializes the project and starts the Compose UI.
 */
fun main() {
    Logger.i { "Initializing DesktopApp" }
    initKoin()
    application {
        val databaseFileService = GlobalContext.get().get<DatabaseFileService>()
        var appGeneration by remember { mutableIntStateOf(0) }
        val coroutineScope = rememberCoroutineScope()
        Window(
            onCloseRequest = ::exitApplication,
            title = applicationName(),
        ) {
            key(appGeneration) {
                App(
                    onExportDatabase = { request ->
                        saveDatabase(window, request, databaseFileService, coroutineScope)
                    },
                    onImportDatabase = { request ->
                        openDatabase(window, request, databaseFileService, coroutineScope)
                    },
                    onDatabaseRecreated = {
                        stopKoin()
                        initKoin()
                        appGeneration++
                    },
                )
            }
        }
    }
}

private fun saveDatabase(
    parent: Frame,
    request: DatabaseExportRequest,
    databaseFileService: DatabaseFileService,
    coroutineScope: CoroutineScope,
) {
    val dialog = FileDialog(parent, "Export database", FileDialog.SAVE).apply {
        file = request.fileName
        isVisible = true
    }
    val fileName = dialog.file
    if (fileName == null) {
        request.onFinished(Result.success(false))
        return
    }
    val destination = File(dialog.directory, fileName).absolutePath
    coroutineScope.launch {
        request.onFinished(runCatching {
            databaseFileService.write(request.content, destination)
            true
        })
    }
}

private fun openDatabase(
    parent: Frame,
    request: DatabaseImportRequest,
    databaseFileService: DatabaseFileService,
    coroutineScope: CoroutineScope,
) {
    val dialog = FileDialog(parent, "Import database", FileDialog.LOAD).apply {
        isVisible = true
    }
    val fileName = dialog.file
    if (fileName == null) {
        request.onFinished(Result.success(null))
        return
    }
    val source = File(dialog.directory, fileName).absolutePath
    coroutineScope.launch {
        request.onFinished(runCatching { databaseFileService.read(source) })
    }
}
