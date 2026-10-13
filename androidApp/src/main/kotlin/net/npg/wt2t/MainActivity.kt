package net.npg.wt2t

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import net.npg.wt2t.di.initKoin
import net.npg.wt2t.domain.service.DatabaseExportRequest
import net.npg.wt2t.domain.service.DatabaseFileService
import net.npg.wt2t.domain.service.DatabaseImportRequest
import net.npg.wt2t.domain.service.PdfExportRequest
import net.npg.wt2t.domain.service.ReportService
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Main entry point for the Android application.
 * It initializes the dependency injection (Koin) and sets up the Compose UI.
 */
class MainActivity : ComponentActivity() {
    /** Initializes dependency injection and displays the Android application UI. */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        try {
            initKoin(module {
                single { this@MainActivity.applicationContext }
            })
        } catch (exception: Exception) {
            Log.w(APPLICATION_LOG_TAG, "Koin initialization was skipped", exception)
        }

        setContent {
            AndroidApp(
                reportService = GlobalContext.get().get(),
                databaseFileService = GlobalContext.get().get(),
                onDatabaseRecreated = {
                    stopKoin()
                    recreate()
                },
            )
        }
    }
}

/** Displays the Android application with its platform report service. */
@Composable
private fun AndroidApp(
    reportService: ReportService,
    databaseFileService: DatabaseFileService,
    onDatabaseRecreated: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var pendingExport by remember { mutableStateOf<PdfExportRequest?>(null) }
    var pendingDatabaseExport by remember { mutableStateOf<DatabaseExportRequest?>(null) }
    var pendingDatabaseImport by remember { mutableStateOf<DatabaseImportRequest?>(null) }
    val createDocument = rememberLauncherForActivityResult(CreateDocument("application/pdf")) { destination ->
        val request = pendingExport
        pendingExport = null
        if (destination != null && request != null) {
            coroutineScope.launch {
                reportService.generatePdfReport(request.data, destination.toString())
            }
        }
    }
    val createDatabaseDocument = rememberLauncherForActivityResult(CreateDocument("application/json")) { destination ->
        val request = pendingDatabaseExport
        pendingDatabaseExport = null
        if (request == null) return@rememberLauncherForActivityResult
        if (destination == null) {
            request.onFinished(Result.success(false))
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            val result = runCatching {
                databaseFileService.write(request.content, destination.toString())
                true
            }
            request.onFinished(result)
        }
    }
    val openDatabaseDocument = rememberLauncherForActivityResult(OpenDocument()) { source ->
        val request = pendingDatabaseImport
        pendingDatabaseImport = null
        if (request == null) return@rememberLauncherForActivityResult
        if (source == null) {
            request.onFinished(Result.success(null))
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            val result = runCatching { databaseFileService.read(source.toString()) }
            request.onFinished(result)
        }
    }

    App(
        onExportPdf = { request ->
            pendingExport = request
            createDocument.launch(request.fileName)
        },
        onExportDatabase = { request ->
            pendingDatabaseExport = request
            createDatabaseDocument.launch(request.fileName)
        },
        onImportDatabase = { request ->
            pendingDatabaseImport = request
            openDatabaseDocument.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
        },
        onDatabaseRecreated = onDatabaseRecreated,
    )
}

/** Previews the Android application UI. */
@Preview
@Composable
fun AppAndroidPreview() {
    App(onDatabaseRecreated = {})
}

private const val APPLICATION_LOG_TAG = "WorkTime2Track"
