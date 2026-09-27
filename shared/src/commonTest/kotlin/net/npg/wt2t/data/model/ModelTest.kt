package net.npg.wt2t.data.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class ModelTest {
    @Test
    fun `persisted settings use documented defaults for corrupt values`() {
        assertEquals("SYSTEM", AppSettings.THEME_MODE.parseStringOrDefault("MIDNIGHT"))
        assertEquals("de", AppSettings.LANGUAGE.parseStringOrDefault("fr"))
        assertEquals(8, AppSettings.DEFAULT_TARGET_HOURS.parseIntOrDefault("eight"))
        assertEquals(0, AppSettings.DEFAULT_TARGET_MINUTES.parseIntOrDefault("60"))
        assertEquals(5, AppSettings.MERGE_THRESHOLD_MINUTES.parseIntOrDefault("-1"))
        assertEquals(5, AppSettings.BOOKING_START_ADJUSTMENT_MINUTES.parseIntOrDefault("31"))
    }

    @Test
    fun `booking start adjustment accepts only one to thirty minutes`() {
        assertEquals(true, AppSettings.BOOKING_START_ADJUSTMENT_MINUTES.isValidValue("1"))
        assertEquals(true, AppSettings.BOOKING_START_ADJUSTMENT_MINUTES.isValidValue("30"))
        assertEquals(false, AppSettings.BOOKING_START_ADJUSTMENT_MINUTES.isValidValue("0"))
        assertEquals(false, AppSettings.BOOKING_START_ADJUSTMENT_MINUTES.isValidValue("31"))
        assertEquals(false, AppSettings.BOOKING_START_ADJUSTMENT_MINUTES.isValidValue("five"))
    }

    @Test
    fun testProjectValidation() {
        val project = Project(name = "Test Project")
        assertEquals("Test Project", project.name)
        assertNotNull(project.id)

        assertFailsWith<IllegalArgumentException> {
            Project(name = "")
        }
        assertFailsWith<IllegalArgumentException> {
            Project(name = " ")
        }
    }

    @Test
    fun testTaskValidation() {
        val task = Task(name = "Test Task", projectId = "project-id")
        assertEquals("Test Task", task.name)
        assertEquals("project-id", task.projectId)
        assertNotNull(task.id)

        assertFailsWith<IllegalArgumentException> {
            Task(name = "")
        }
    }

    @Test
    fun testTimeModel() {
        val date = LocalDate(2023, 10, 27)
        val start = LocalTime(9, 0)
        val end = LocalTime(17, 0)
        val time = Time(
            taskId = "task-id",
            date = date,
            start = start,
            end = end,
            description = listOf("Note 1", "Note 2")
        )

        assertEquals("task-id", time.taskId)
        assertEquals(date, time.date)
        assertEquals(start, time.start)
        assertEquals(end, time.end)
        assertEquals(2, time.description.size)
    }

    @Test
    fun testDailyWorkTimeModel() {
        val date = LocalDate(2023, 10, 27)
        val dailyWorkTime = DailyWorkTime(
            date = date,
            minutes = 480
        )

        assertEquals(date, dailyWorkTime.date)
        assertEquals(480, dailyWorkTime.minutes)
    }
}
