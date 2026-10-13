package net.npg.wt2t.data.storage

import app.cash.sqldelight.db.SqlDriver

/** Owns the platform-specific database file and creates initialized drivers for it. */
interface DatabaseStorage {
    /** Opens the database and creates or migrates its schema as required. */
    fun openDriver(): SqlDriver

    /** Deletes the database file and any associated SQLite sidecar files. */
    fun deleteDatabase()
}
