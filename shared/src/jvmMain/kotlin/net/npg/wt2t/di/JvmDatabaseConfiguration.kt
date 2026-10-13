package net.npg.wt2t.di

import java.io.File

/** Resolves filesystem configuration for the desktop database. */
object JvmDatabaseConfiguration {
    const val DATABASE_PATH_PROPERTY = "wt2t.database.path"
    const val DATABASE_PATH_ENVIRONMENT_VARIABLE = "WT2T_DATABASE_PATH"

    /** Resolves the desktop database file, creating its parent directory if needed. */
    fun resolveDatabaseFile(): File {
        val configuredPath = System.getProperty(DATABASE_PATH_PROPERTY)
            ?.takeIf { it.isNotBlank() }
            ?: System.getenv(DATABASE_PATH_ENVIRONMENT_VARIABLE)
                ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_DATABASE_FILE

        return File(configuredPath).absoluteFile
    }

    private const val DEFAULT_DATABASE_FILE = "worktime.db"
}
