package net.npg.wt2t.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.npg.wt2t.data.storage.JvmDatabaseStorage
import net.npg.wt2t.db.WorkTimeDatabase
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class DatabaseManagementRepositoryTest {
    @Test
    fun `recreating database replaces the file and creates a usable current schema`() = runTest {
        val databaseDirectory = Files.createTempDirectory("worktime-database-test-")
        val databaseFile = databaseDirectory.resolve("worktime.db").toFile()
        val databaseStorage = JvmDatabaseStorage(databaseFile)
        val driver = databaseStorage.openDriver()
        val queries = WorkTimeDatabase(driver).workTimeDatabaseQueries
        queries.insertProject("project", "Project", false)
        driver.execute(null, "CREATE TABLE resetMarker (value TEXT NOT NULL)", 0)
        val sidecarFiles = listOf("-wal", "-shm", "-journal").map { suffix ->
            databaseFile.resolveSibling("${databaseFile.name}$suffix").apply {
                writeText("sidecar")
            }
        }

        DatabaseManagementRepository(
            driver = driver,
            databaseStorage = databaseStorage,
            dispatcher = UnconfinedTestDispatcher(testScheduler),
        ).recreateDatabase()

        val recreatedDriver = databaseStorage.openDriver() as JdbcSqliteDriver
        try {
            val recreatedQueries = WorkTimeDatabase(recreatedDriver).workTimeDatabaseQueries
            assertEquals(emptyList(), recreatedQueries.selectAllProjects().executeAsList())
            assertEquals(0, countTable(recreatedDriver, "resetMarker"))
            assertEquals(WorkTimeDatabase.Schema.version, databaseVersion(recreatedDriver))
            sidecarFiles.forEach { sidecarFile -> assertFalse(sidecarFile.exists()) }

            recreatedQueries.insertProject("new-project", "New Project", false)
            assertEquals("new-project", recreatedQueries.selectAllProjects().executeAsOne().id)
        } finally {
            recreatedDriver.close()
            databaseDirectory.toFile().deleteRecursively()
        }
    }

    private fun countTable(driver: JdbcSqliteDriver, tableName: String): Int =
        driver.getConnection().createStatement().use { statement ->
            statement.executeQuery(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = '$tableName'",
            ).use { result ->
                result.next()
                result.getInt(1)
            }
        }

    private fun databaseVersion(driver: JdbcSqliteDriver): Long =
        driver.getConnection().createStatement().use { statement ->
            statement.executeQuery("PRAGMA user_version").use { result ->
                result.next()
                result.getLong(1)
            }
        }
}
