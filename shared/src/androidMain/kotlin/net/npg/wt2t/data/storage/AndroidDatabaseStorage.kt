package net.npg.wt2t.data.storage

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import net.npg.wt2t.db.WorkTimeDatabase
import java.io.File

/** Stores the Android database in the application's private database directory. */
class AndroidDatabaseStorage(
    private val context: Context,
) : DatabaseStorage {
    override fun openDriver(): SqlDriver =
        AndroidSqliteDriver(WorkTimeDatabase.Schema, context, DATABASE_NAME).also { driver ->
            driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        }

    override fun deleteDatabase() {
        val databaseFile = context.getDatabasePath(DATABASE_NAME)
        if (databaseFile.exists() && !context.deleteDatabase(DATABASE_NAME)) {
            error("Could not delete database ${databaseFile.absolutePath}")
        }
        SQLITE_SIDECAR_SUFFIXES.forEach { suffix ->
            val sidecar = File("${databaseFile.absolutePath}$suffix")
            if (sidecar.exists() && !sidecar.delete()) {
                error("Could not delete SQLite sidecar ${sidecar.absolutePath}")
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "worktime.db"
        val SQLITE_SIDECAR_SUFFIXES = listOf("-wal", "-shm", "-journal")
    }
}
