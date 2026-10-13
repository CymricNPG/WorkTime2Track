package net.npg.wt2t.ui.viewmodel

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.npg.wt2t.data.repository.DatabaseExportRepository
import net.npg.wt2t.data.repository.DatabaseManagementRepository
import net.npg.wt2t.data.storage.DatabaseStorage
import net.npg.wt2t.db.WorkTimeDatabase
import net.npg.wt2t.domain.service.DatabaseExportRequest
import net.npg.wt2t.domain.usecase.BackupUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MenuViewModelTest {
    @Test
    fun `export remains active until platform transfer completes`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            var actualRequest: DatabaseExportRequest? = null
            val viewModel = MenuViewModel(
                backupUseCase = createBackupUseCase(testDispatcher),
                exportDatabaseFile = { actualRequest = it },
                importDatabaseFile = null,
            )

            viewModel.exportDatabase()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isTransferRunning)
            assertTrue(viewModel.uiState.value.canExportDatabase)
            assertFalse(viewModel.uiState.value.canImportDatabase)
            val request = assertNotNull(actualRequest)
            assertEquals("worktime2track-backup.json", request.fileName)

            request.onFinished(Result.success(true))

            assertFalse(viewModel.uiState.value.isTransferRunning)
            assertEquals(DatabaseTransferMessage.EXPORT_SUCCEEDED, viewModel.uiState.value.transferMessage)

            viewModel.clearTransferMessage()

            assertEquals(null, viewModel.uiState.value.transferMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `import starts only after confirmation`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            var wasImporterCalled = false
            val viewModel = MenuViewModel(
                backupUseCase = createBackupUseCase(testDispatcher),
                exportDatabaseFile = null,
                importDatabaseFile = { wasImporterCalled = true },
            )

            viewModel.requestDatabaseImport()

            assertTrue(viewModel.uiState.value.isImportConfirmationVisible)
            assertFalse(wasImporterCalled)

            viewModel.confirmDatabaseImport()

            assertFalse(viewModel.uiState.value.isImportConfirmationVisible)
            assertTrue(viewModel.uiState.value.isTransferRunning)
            assertTrue(wasImporterCalled)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `delete starts only after visible confirmation`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            WorkTimeDatabase.Schema.create(driver)
            val database = WorkTimeDatabase(driver)
            database.workTimeDatabaseQueries.insertProject("project", "Project", false)
            val viewModel = MenuViewModel(
                backupUseCase = BackupUseCase(DatabaseExportRepository(database, testDispatcher), createManagementRepository(testDispatcher, driver)),
                exportDatabaseFile = null,
                importDatabaseFile = null,
            )

            viewModel.confirmDatabaseDeletion()
            advanceUntilIdle()

            assertEquals(1, database.workTimeDatabaseQueries.selectAllProjects().executeAsList().size)

            viewModel.requestDatabaseDeletion()

            assertTrue(viewModel.uiState.value.isDeleteConfirmationVisible)

            viewModel.dismissDatabaseDeletion()

            assertFalse(viewModel.uiState.value.isDeleteConfirmationVisible)
            assertEquals(1, database.workTimeDatabaseQueries.selectAllProjects().executeAsList().size)

            viewModel.requestDatabaseDeletion()
            viewModel.confirmDatabaseDeletion()

            assertFalse(viewModel.uiState.value.isDeleteConfirmationVisible)
            assertTrue(viewModel.uiState.value.isTransferRunning)

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isTransferRunning)
            assertEquals(DatabaseTransferMessage.DELETE_SUCCEEDED, viewModel.uiState.value.transferMessage)
        } finally {
            driver.close()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `delete failure is reported without exposing the exception`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            WorkTimeDatabase.Schema.create(driver)
            val viewModel = MenuViewModel(
                backupUseCase = BackupUseCase(DatabaseExportRepository(WorkTimeDatabase(driver), testDispatcher), createManagementRepository(
                    testDispatcher = testDispatcher,
                    driver = driver,
                    failDeletion = true,
                )),
                exportDatabaseFile = null,
                importDatabaseFile = null,
            )
            viewModel.requestDatabaseDeletion()
            viewModel.confirmDatabaseDeletion()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isTransferRunning)
            assertEquals(DatabaseTransferMessage.DELETE_FAILED, viewModel.uiState.value.transferMessage)
        } finally {
            runCatching { driver.close() }
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `unavailable transfers remain disabled`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val viewModel = MenuViewModel(
                backupUseCase = createBackupUseCase(testDispatcher),
                exportDatabaseFile = null,
                importDatabaseFile = null,
            )

            viewModel.exportDatabase()
            viewModel.requestDatabaseImport()

            assertEquals(MenuUiState(), viewModel.uiState.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createBackupUseCase(testDispatcher: CoroutineDispatcher): BackupUseCase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WorkTimeDatabase.Schema.create(driver)
        return BackupUseCase(
            DatabaseExportRepository(WorkTimeDatabase(driver), testDispatcher),
            createManagementRepository(testDispatcher, driver),
        )
    }

    private fun createManagementRepository(
        testDispatcher: CoroutineDispatcher,
        driver: JdbcSqliteDriver = createDriver(),
        failDeletion: Boolean = false,
    ): DatabaseManagementRepository = DatabaseManagementRepository(
        driver = driver,
        databaseStorage = object : DatabaseStorage {
            override fun openDriver() = createDriver()

            override fun deleteDatabase() {
                if (failDeletion) error("Deletion failed")
            }
        },
        dispatcher = testDispatcher,
    )

    private fun createDriver(): JdbcSqliteDriver =
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also(WorkTimeDatabase.Schema::create)
}
