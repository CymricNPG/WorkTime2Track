package net.npg.wt2t.data.repository

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.npg.wt2t.data.storage.DatabaseStorage

/** Manages destructive operations on the complete application database. */
class DatabaseManagementRepository(
    private val driver: SqlDriver,
    private val databaseStorage: DatabaseStorage,
    private val dispatcher: CoroutineDispatcher,
) {
    /** Replaces the current database file with an empty database using the latest schema. */
    suspend fun recreateDatabase(): Unit = withContext(dispatcher) {
        driver.close()
        databaseStorage.deleteDatabase()
        databaseStorage.openDriver().close()
    }
}
