package net.npg.wt2t.data.storage

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import net.npg.wt2t.db.WorkTimeDatabase
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmDatabaseMigrationTest {
    @Test
    fun `on disk migration normalizes duplicate active bookings before adding invariant`() {
        withLegacyDatabase { databaseFile ->
            val legacyDriver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
            try {
                WorkTimeDatabase.Schema.create(legacyDriver)
                legacyDriver.execute(null, "DROP INDEX idx_active_time_date", 0)
                listOf(
                    "INSERT INTO taskEntity VALUES ('task', 'Task', 0, 0, NULL)",
                    "INSERT INTO timeEntity VALUES ('same-b', 'task', '2026-08-01', '09:00', NULL, '[]')",
                    "INSERT INTO timeEntity VALUES ('same-a', 'task', '2026-08-01', '09:00', NULL, '[]')",
                    "INSERT INTO timeEntity VALUES ('latest', 'task', '2026-08-01', '10:00', NULL, '[]')",
                    "INSERT INTO timeEntity VALUES ('other-date', 'task', '2026-08-02', '08:00', NULL, '[]')",
                ).forEach { statement -> legacyDriver.execute(null, statement, 0) }
                legacyDriver.execute(null, "PRAGMA user_version = 4", 0)
            } finally {
                legacyDriver.close()
            }

            val driver = JvmDatabaseStorage(databaseFile).openDriver() as JdbcSqliteDriver
            try {
                val queries = WorkTimeDatabase(driver).workTimeDatabaseQueries
                val actualFirstDate = queries.selectTimesByDate("2026-08-01").executeAsList()
                    .sortedBy { it.start }
                assertEquals(listOf("same-a", "latest"), actualFirstDate.map { it.id })
                assertEquals(listOf("10:00", null), actualFirstDate.map { it.end })
                assertEquals(1, actualFirstDate.count { it.end == null })
                assertEquals(1, queries.selectTimesByDate("2026-08-02").executeAsList().count { it.end == null })
                assertEquals(WorkTimeDatabase.Schema.version, databaseVersion(driver))
                assertFailsWith<Exception> {
                    queries.insertTime("duplicate", "task", "2026-08-01", "11:00", null, "[]")
                }
            } finally {
                driver.close()
            }
        }
    }

    @Test
    fun `on disk migration normalizes every legacy value rejected by current constraints`() {
        withLegacyDatabase { databaseFile ->
            createLegacyDatabase(databaseFile) { driver ->
                listOf(
                    "CREATE TABLE taskEntity_new (marker TEXT NOT NULL)",
                    "INSERT INTO projectEntity VALUES ('project', 'Project', 0)",
                    "INSERT INTO projectEntity VALUES ('duplicate-project', 'Project', 1)",
                    "INSERT INTO projectEntity VALUES (' ', 'Invalid identifier', 0)",
                    "INSERT INTO projectEntity VALUES ('blank-project-name', ' ', 0)",
                    "INSERT INTO taskEntity VALUES ('task', 'Task', 0, 0, 'project')",
                    "INSERT INTO taskEntity VALUES ('duplicate-task', 'Task', 1, 1, 'duplicate-project')",
                    "INSERT INTO taskEntity VALUES (' ', 'Invalid identifier', 0, 0, NULL)",
                    "INSERT INTO taskEntity VALUES ('blank-task-name', ' ', 0, 0, NULL)",
                    "INSERT INTO taskEntity VALUES ('orphan-task', 'Orphan', 0, 0, 'missing-project')",
                    "INSERT INTO timeEntity VALUES ('first-time', 'task', '2026-08-01', '08:00', '09:00', '[]')",
                    "INSERT INTO timeEntity VALUES ('remapped-time', 'duplicate-task', '2026-08-01', '09:00', '10:00', '[]')",
                    "INSERT INTO timeEntity VALUES (' ', 'task', '2026-08-01', '10:00', '11:00', '[]')",
                    "INSERT INTO timeEntity VALUES ('invalid-range', 'task', '2026-08-01', '12:00', '11:00', '[]')",
                    "INSERT INTO timeEntity VALUES ('orphan-time', 'missing-task', '2026-08-01', '12:00', '13:00', '[]')",
                    "INSERT INTO dailyWorkTimeEntity VALUES ('valid-target', '2026-08-01', 480, 30)",
                    "INSERT INTO dailyWorkTimeEntity VALUES ('high-target', '2026-08-02', 2000, -1)",
                    "INSERT INTO dailyWorkTimeEntity VALUES ('low-target', '2026-08-03', -10, 0)",
                    "INSERT INTO dailyWorkTimeEntity VALUES (' ', '2026-08-04', 420, 0)",
                ).forEach { statement -> driver.execute(null, statement, 0) }
            }

            val driver = JvmDatabaseStorage(databaseFile).openDriver() as JdbcSqliteDriver
            try {
                val queries = WorkTimeDatabase(driver).workTimeDatabaseQueries
                assertEquals(listOf("project"), queries.selectAllProjects().executeAsList().map { it.id })
                assertEquals(listOf("task"), queries.selectAllTasks().executeAsList().map { it.id })
                assertEquals(
                    listOf("first-time", "remapped-time"),
                    queries.selectAllTimes().executeAsList().map { it.id },
                )
                assertEquals(
                    listOf("task", "task"),
                    queries.selectAllTimes().executeAsList().map { it.taskId },
                )
                assertEquals(
                    listOf(480L, 1_440L, 0L),
                    queries.selectAllDailyWorkTimes().executeAsList().map { it.minutes },
                )
                assertEquals(
                    listOf(30L, 0L, 0L),
                    queries.selectAllDailyWorkTimes().executeAsList().map { it.breakMinutes },
                )
                assertEquals(WorkTimeDatabase.Schema.version, databaseVersion(driver))
            } finally {
                driver.close()
            }
        }
    }

    @Test
    fun `failed on disk migration rolls back replacement tables and version`() {
        withLegacyDatabase { databaseFile ->
            createLegacyDatabase(databaseFile) { driver ->
                driver.execute(null, "INSERT INTO projectEntity VALUES ('project', 'Project', 0)", 0)
                driver.execute(null, "CREATE INDEX idx_project_name ON configEntity(value)", 0)
            }

            assertFailsWith<Exception> {
                JvmDatabaseStorage(databaseFile).openDriver()
            }

            val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
            try {
                assertEquals(3L, databaseVersion(driver))
                assertEquals(0, countTable(driver, "projectEntity_new"))
                assertEquals(1, countTable(driver, "projectEntity"))
                val projectCount = driver.getConnection().createStatement().use { statement ->
                    statement.executeQuery("SELECT COUNT(*) FROM projectEntity WHERE id = 'project'").use { result ->
                        result.next()
                        result.getInt(1)
                    }
                }
                assertEquals(1, projectCount)
            } finally {
                driver.close()
            }
        }
    }

    private fun createLegacyDatabase(
        databaseFile: File,
        populate: (JdbcSqliteDriver) -> Unit,
    ) {
        val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
        try {
            legacySchemaStatements.forEach { statement -> driver.execute(null, statement, 0) }
            populate(driver)
            driver.execute(null, "PRAGMA user_version = 3", 0)
        } finally {
            driver.close()
        }
    }

    private fun withLegacyDatabase(block: (File) -> Unit) {
        val databaseDirectory = Files.createTempDirectory("worktime-migration-test-").toFile()
        try {
            block(databaseDirectory.resolve("worktime.db"))
        } finally {
            databaseDirectory.deleteRecursively()
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

    private companion object {
        val legacySchemaStatements = listOf(
            "CREATE TABLE projectEntity (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, closed INTEGER NOT NULL DEFAULT 0)",
            "CREATE TABLE taskEntity (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, freeTime INTEGER NOT NULL DEFAULT 0, closed INTEGER NOT NULL DEFAULT 0, projectId TEXT)",
            "CREATE TABLE timeEntity (id TEXT NOT NULL PRIMARY KEY, taskId TEXT NOT NULL, date TEXT NOT NULL, start TEXT NOT NULL, end TEXT, description TEXT NOT NULL)",
            "CREATE TABLE dailyWorkTimeEntity (id TEXT NOT NULL PRIMARY KEY, date TEXT NOT NULL, minutes INTEGER NOT NULL, breakMinutes INTEGER NOT NULL DEFAULT 0)",
            "CREATE TABLE configEntity (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)",
            "CREATE INDEX idx_time_date ON timeEntity(date)",
            "CREATE UNIQUE INDEX idx_daily_work_time_date ON dailyWorkTimeEntity(date)",
        )
    }
}
