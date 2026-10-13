package net.npg.wt2t.data.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import co.touchlab.kermit.Logger
import net.npg.wt2t.db.WorkTimeDatabase
import java.io.File
import java.nio.file.Files

/** Stores the desktop database in a local SQLite file. */
class JvmDatabaseStorage(
    private val databaseFile: File,
) : DatabaseStorage {
    override fun openDriver(): SqlDriver {
        databaseFile.parentFile?.mkdirs()
        val shouldCreateSchema = !databaseFile.exists() || databaseFile.length() == 0L
        val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
        try {
            if (shouldCreateSchema) {
                Logger.i("Creating database ${databaseFile.absolutePath}")
                WorkTimeDatabase.Schema.create(driver)
                setDatabaseVersion(driver, WorkTimeDatabase.Schema.version)
            } else {
                Logger.i("Using database ${databaseFile.absolutePath}")
                migrateDatabase(driver)
            }
            driver.execute(null, "PRAGMA foreign_keys = ON", 0)
            return driver
        } catch (exception: Exception) {
            driver.close()
            throw exception
        }
    }

    override fun deleteDatabase() {
        SQLITE_FILE_SUFFIXES.forEach { suffix ->
            Files.deleteIfExists(File("${databaseFile.absolutePath}$suffix").toPath())
        }
    }

    private fun migrateDatabase(driver: JdbcSqliteDriver) {
        val storedVersion = getDatabaseVersion(driver)
        val currentVersion = WorkTimeDatabase.Schema.version
        val oldVersion = if (storedVersion == 0L) INITIAL_SCHEMA_VERSION else storedVersion
        if (oldVersion >= currentVersion) return

        Logger.i("Migrating database from version $oldVersion to $currentVersion")
        try {
            WorkTimeDatabase(driver).transaction {
                WorkTimeDatabase.Schema.migrate(driver, oldVersion, currentVersion)
                setDatabaseVersion(driver, currentVersion)
            }
        } catch (exception: Exception) {
            throw DatabaseMigrationException(
                databaseFile = databaseFile,
                oldVersion = oldVersion,
                currentVersion = currentVersion,
                cause = exception,
            )
        }
    }

    private fun getDatabaseVersion(driver: JdbcSqliteDriver): Long =
        driver.getConnection().createStatement().use { statement ->
            statement.executeQuery("PRAGMA user_version").use { result ->
                if (result.next()) result.getLong(1) else 0L
            }
        }

    private fun setDatabaseVersion(driver: JdbcSqliteDriver, version: Long) {
        driver.execute(null, "PRAGMA user_version = $version", 0)
    }

    private companion object {
        const val INITIAL_SCHEMA_VERSION = 1L
        val SQLITE_FILE_SUFFIXES = listOf("", "-wal", "-shm", "-journal")
    }
}

/** Reports a recoverable migration failure after the original database was restored. */
class DatabaseMigrationException(
    databaseFile: File,
    oldVersion: Long,
    currentVersion: Long,
    cause: Throwable,
) : IllegalStateException(
    "Could not migrate database ${databaseFile.absolutePath} from version $oldVersion " +
        "to $currentVersion; the original database was preserved",
    cause,
)
