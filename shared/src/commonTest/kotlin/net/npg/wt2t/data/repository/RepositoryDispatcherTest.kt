package net.npg.wt2t.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.db.WorkTimeDatabase
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryDispatcherTest {

    @Test
    fun `database writes use the injected dispatcher`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WorkTimeDatabase.Schema.create(driver)
        val database = WorkTimeDatabase(driver)
        val dispatcher = RecordingDispatcher(StandardTestDispatcher(testScheduler))
        val projectRepository = SqlDelightProjectRepository(database, dispatcher)
        val taskRepository = SqlDelightTaskRepository(database, dispatcher)
        val timeRepository = SqlDelightTimeRepository(database, dispatcher)
        val dailyWorkTimeRepository = SqlDelightDailyWorkTimeRepository(database, dispatcher)
        val project = Project(id = "project", name = "Project")
        val task = Task(id = "task", name = "Task", projectId = project.id)
        val time = Time(
            id = "time",
            taskId = task.id,
            date = LocalDate(2026, 8, 22),
            start = LocalTime(8, 0),
            end = LocalTime(9, 0),
        )

        try {
            assertDispatched(dispatcher) { projectRepository.saveProject(project) }
            assertDispatched(dispatcher) { taskRepository.saveTask(task) }
            assertDispatched(dispatcher) { timeRepository.saveTime(time) }
            assertDispatched(dispatcher) {
                timeRepository.replaceTimesForDate(time.date, listOf(time.copy(id = "replacement")))
            }
            assertDispatched(dispatcher) { timeRepository.deleteTime("replacement") }
            assertDispatched(dispatcher) {
                dailyWorkTimeRepository.saveDailyWorkTime(
                    DailyWorkTime(date = time.date, minutes = 480, breakMinutes = 30),
                )
            }
            assertDispatched(dispatcher) { dailyWorkTimeRepository.deleteAllDailyWorkTimes() }
            assertDispatched(dispatcher) { taskRepository.deleteTask(task.id) }
            assertDispatched(dispatcher) { projectRepository.deleteProject(project.id) }
        } finally {
            driver.close()
        }
    }

    private suspend fun assertDispatched(
        dispatcher: RecordingDispatcher,
        operation: suspend () -> Unit,
    ) {
        val dispatchCountBefore = dispatcher.dispatchCount

        operation()

        assertTrue(dispatcher.dispatchCount > dispatchCountBefore)
    }
}

private class RecordingDispatcher(
    private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher() {
    var dispatchCount = 0
        private set

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatchCount++
        delegate.dispatch(context, block)
    }
}
