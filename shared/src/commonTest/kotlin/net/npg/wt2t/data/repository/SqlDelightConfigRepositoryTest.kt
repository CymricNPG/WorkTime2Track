package net.npg.wt2t.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.npg.wt2t.data.model.AppSettings
import net.npg.wt2t.db.WorkTimeDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class SqlDelightConfigRepositoryTest {
    @Test
    fun `batch update rolls back every setting when one write fails`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WorkTimeDatabase.Schema.create(driver)
        val database = WorkTimeDatabase(driver)
        val queries = database.workTimeDatabaseQueries
        val repository = SqlDelightConfigRepository(
            database,
            UnconfinedTestDispatcher(testScheduler),
        )
        queries.insertConfig(AppSettings.LANGUAGE.name, "de")
        queries.insertConfig(AppSettings.THEME_MODE.name, "LIGHT")
        driver.execute(
            identifier = null,
            sql = """
                CREATE TRIGGER reject_theme_update
                BEFORE INSERT ON configEntity
                WHEN NEW.key = 'THEME_MODE'
                BEGIN
                    SELECT RAISE(ABORT, 'theme update failed');
                END
            """.trimIndent(),
            parameters = 0,
        )

        try {
            assertFailsWith<Exception> {
                repository.setAll(
                    linkedMapOf(
                        AppSettings.LANGUAGE.name to "en",
                        AppSettings.THEME_MODE.name to "DARK",
                    ),
                )
            }

            assertEquals("de", queries.selectConfig(AppSettings.LANGUAGE.name).executeAsOne())
            assertEquals("LIGHT", queries.selectConfig(AppSettings.THEME_MODE.name).executeAsOne())
        } finally {
            driver.close()
        }
    }
}
