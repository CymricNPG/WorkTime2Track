package net.npg.wt2t.data.repository

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import kotlin.test.Test
import kotlin.test.assertEquals

class MapperTest {

    @Test
    fun testProjectMapper() {
        val model = Project(id = "1", name = "Project", closed = true)
        val entity = model.toEntity()
        assertEquals(model.id, entity.id)
        assertEquals(model.name, entity.name)
        assertEquals(model.closed, entity.closed)

        val fromEntity = entity.toModel()
        assertEquals(model, fromEntity)
    }

    @Test
    fun testTaskMapper() {
        val model = Task(id = "1", name = "Task", freeTime = true, closed = false, projectId = "p1")
        val entity = model.toEntity()
        assertEquals(model.id, entity.id)
        assertEquals(model.name, entity.name)
        assertEquals(model.freeTime, entity.freeTime)
        assertEquals(model.closed, entity.closed)
        assertEquals(model.projectId, entity.projectId)

        val fromEntity = entity.toModel()
        assertEquals(model, fromEntity)
    }

    @Test
    fun testTimeMapper() {
        val model = Time(
            id = "1",
            taskId = "t1",
            date = LocalDate(2023, 10, 27),
            start = LocalTime(9, 0),
            end = LocalTime(10, 0),
            description = listOf("Note1", "Note2")
        )
        val entity = model.toEntity()
        assertEquals(model.id, entity.id)
        assertEquals(model.taskId, entity.taskId)
        assertEquals(model.date.toString(), entity.date)
        assertEquals(model.start.toString(), entity.start)
        assertEquals(model.end.toString(), entity.end)
        assertEquals(Json.encodeToString(model.description), entity.description)

        val fromEntity = entity.toModel()
        assertEquals(model, fromEntity)
    }

    @Test
    fun testDailyWorkTimeMapper() {
        val model = DailyWorkTime(
            id = "1",
            date = LocalDate(2023, 10, 27),
            minutes = 480,
            breakMinutes = 45,
        )
        val entity = model.toEntity()
        assertEquals(model.id, entity.id)
        assertEquals(model.date.toString(), entity.date)
        assertEquals(model.minutes.toLong(), entity.minutes)
        assertEquals(model.breakMinutes.toLong(), entity.breakMinutes)

        val fromEntity = entity.toModel()
        assertEquals(model, fromEntity)
    }
}
