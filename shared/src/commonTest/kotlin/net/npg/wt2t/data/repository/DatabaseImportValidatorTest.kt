package net.npg.wt2t.data.repository

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DatabaseImportValidatorTest {
    @Test
    fun `valid complete snapshot passes validation`() {
        DatabaseImportValidator.validate(validExport())
    }

    @Test
    fun `duplicate entity identifiers are rejected`() {
        val export = validExport()

        assertFailure(
            DatabaseImportFailure.DUPLICATE_PROJECT_ID,
            export.copy(projects = export.projects + Project(id = "project", name = "Other project")),
        )
        assertFailure(
            DatabaseImportFailure.DUPLICATE_TASK_ID,
            export.copy(tasks = export.tasks + Task(id = "task", name = "Other task")),
        )
        assertFailure(
            DatabaseImportFailure.DUPLICATE_TIME_ID,
            export.copy(
                times = export.times + Time(
                    id = "morning",
                    taskId = "task",
                    date = TEST_DATE,
                    start = LocalTime(10, 0),
                    end = LocalTime(11, 0),
                ),
            ),
        )
        assertFailure(
            DatabaseImportFailure.DUPLICATE_DAILY_WORK_TIME_ID,
            export.copy(
                dailyWorkTimes = export.dailyWorkTimes + DailyWorkTime(
                    id = "target",
                    date = LocalDate(2026, 8, 2),
                    minutes = 480,
                ),
            ),
        )
    }

    @Test
    fun `blank entity identifier is rejected`() {
        val export = validExport()

        assertFailure(
            DatabaseImportFailure.INVALID_IDENTIFIER,
            export.copy(times = listOf(export.times.first().copy(id = " "))),
        )
    }

    @Test
    fun `duplicate domain keys are rejected`() {
        val export = validExport()

        assertFailure(
            DatabaseImportFailure.DUPLICATE_PROJECT_NAME,
            export.copy(projects = export.projects + Project(id = "duplicate-name-project", name = "Project")),
        )
        assertFailure(
            DatabaseImportFailure.DUPLICATE_TASK_NAME,
            export.copy(tasks = export.tasks + Task(id = "other-task", name = "Task", projectId = "project")),
        )
        assertFailure(
            DatabaseImportFailure.DUPLICATE_DAILY_WORK_TIME_DATE,
            export.copy(
                dailyWorkTimes = export.dailyWorkTimes + DailyWorkTime(
                    id = "other-target",
                    date = TEST_DATE,
                    minutes = 420,
                ),
            ),
        )
    }

    @Test
    fun `missing references are rejected`() {
        val export = validExport()

        assertFailure(
            DatabaseImportFailure.MISSING_TASK_PROJECT,
            export.copy(tasks = listOf(Task(id = "task", name = "Task", projectId = "missing"))),
        )
        assertFailure(
            DatabaseImportFailure.MISSING_TIME_TASK,
            export.copy(times = listOf(export.times.first().copy(taskId = "missing"))),
        )
    }

    @Test
    fun `invalid and overlapping bookings are rejected`() {
        val export = validExport()

        assertFailure(
            DatabaseImportFailure.INVALID_TIME_RANGE,
            export.copy(times = listOf(export.times.first().copy(end = LocalTime(7, 59)))),
        )
        assertFailure(
            DatabaseImportFailure.OVERLAPPING_TIMES,
            export.copy(
                times = export.times + Time(
                    id = "overlap",
                    taskId = "task",
                    date = TEST_DATE,
                    start = LocalTime(8, 30),
                    end = LocalTime(10, 0),
                ),
            ),
        )
        assertFailure(
            DatabaseImportFailure.OVERLAPPING_TIMES,
            export.copy(
                times = listOf(
                    export.times.first().copy(end = null),
                    Time(
                        id = "second-active",
                        taskId = "task",
                        date = TEST_DATE,
                        start = LocalTime(10, 0),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `zero-minute booking can share a timestamp with a regular booking`() {
        val export = validExport()
        val zeroMinuteBooking = Time(
            id = "note",
            taskId = "task",
            date = TEST_DATE,
            start = LocalTime(8, 30),
            end = LocalTime(8, 30),
            description = listOf("Quick note"),
        )

        DatabaseImportValidator.validate(export.copy(times = export.times + zeroMinuteBooking))
    }

    @Test
    fun `invalid daily work time is rejected`() {
        val export = validExport()

        assertFailure(
            DatabaseImportFailure.INVALID_DAILY_WORK_TIME,
            export.copy(dailyWorkTimes = listOf(export.dailyWorkTimes.single().copy(minutes = 1_441))),
        )
        assertFailure(
            DatabaseImportFailure.INVALID_DAILY_WORK_TIME,
            export.copy(dailyWorkTimes = listOf(export.dailyWorkTimes.single().copy(breakMinutes = -1))),
        )
    }

    @Test
    fun `unknown configuration key is rejected`() {
        assertFailure(
            DatabaseImportFailure.UNKNOWN_CONFIGURATION,
            validExport().copy(configs = mapOf("UNKNOWN" to "value")),
        )
    }

    @Test
    fun `invalid theme and unsupported language are rejected`() {
        assertFailure(
            DatabaseImportFailure.INVALID_CONFIGURATION,
            validExport().copy(configs = mapOf("THEME_MODE" to "MIDNIGHT")),
        )
        assertFailure(
            DatabaseImportFailure.INVALID_CONFIGURATION,
            validExport().copy(configs = mapOf("LANGUAGE" to "fr")),
        )
    }

    @Test
    fun `malformed and out of range numeric configuration is rejected`() {
        listOf(
            mapOf("DEFAULT_TARGET_HOURS" to "eight"),
            mapOf("DEFAULT_TARGET_HOURS" to "25"),
            mapOf("DEFAULT_TARGET_MINUTES" to "60"),
            mapOf("MERGE_THRESHOLD_MINUTES" to "-1"),
            mapOf(
                "DEFAULT_TARGET_HOURS" to "24",
                "DEFAULT_TARGET_MINUTES" to "1",
            ),
        ).forEach { configs ->
            assertFailure(
                DatabaseImportFailure.INVALID_CONFIGURATION,
                validExport().copy(configs = configs),
            )
        }
    }

    @Test
    fun `supported configuration values pass validation`() {
        DatabaseImportValidator.validate(
            validExport().copy(
                configs = mapOf(
                    "DEFAULT_TARGET_HOURS" to "8",
                    "DEFAULT_TARGET_MINUTES" to "30",
                    "MERGE_THRESHOLD_MINUTES" to "5",
                    "LANGUAGE" to "en",
                    "THEME_MODE" to "DARK",
                ),
            ),
        )
    }

    private fun assertFailure(expectedFailure: DatabaseImportFailure, export: DatabaseExport) {
        val exception = assertFailsWith<DatabaseImportException> {
            DatabaseImportValidator.validate(export)
        }
        assertEquals(expectedFailure, exception.failure)
    }

    private fun validExport(): DatabaseExport = DatabaseExport(
        projects = listOf(
            Project(id = "project", name = "Project"),
            Project(id = "other-project", name = "Other project"),
        ),
        tasks = listOf(
            Task(id = "task", name = "Task", projectId = "project"),
            Task(id = "same-name-other-project", name = "Task", projectId = "other-project"),
        ),
        times = listOf(
            Time(
                id = "morning",
                taskId = "task",
                date = TEST_DATE,
                start = LocalTime(8, 0),
                end = LocalTime(9, 0),
            ),
        ),
        dailyWorkTimes = listOf(
            DailyWorkTime(
                id = "target",
                date = TEST_DATE,
                minutes = 480,
                breakMinutes = 30,
            ),
        ),
    )

    private companion object {
        val TEST_DATE = LocalDate(2026, 8, 1)
    }
}
