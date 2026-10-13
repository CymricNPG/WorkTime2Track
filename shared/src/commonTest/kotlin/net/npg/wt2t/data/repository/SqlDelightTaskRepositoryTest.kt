package net.npg.wt2t.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.db.WorkTimeDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SqlDelightTaskRepositoryTest {

    @Test
    fun `get tasks by project includes projectless tasks when project id is null`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WorkTimeDatabase.Schema.create(driver)
        val database = WorkTimeDatabase(driver)
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val projectRepository = SqlDelightProjectRepository(database, dispatcher)
        val taskRepository = SqlDelightTaskRepository(database, dispatcher)
        val project = Project(id = "project-id", name = "Project")
        val projectlessTask = Task(id = "projectless-task", name = "Projectless task")
        val projectTask = Task(id = "project-task", name = "Project task", projectId = project.id)

        try {
            projectRepository.saveProject(project)
            taskRepository.saveTask(projectlessTask)
            taskRepository.saveTask(projectTask)

            val actualProjectlessTasks = taskRepository.getTasksByProject(null).first()
            val actualProjectTasks = taskRepository.getTasksByProject(project.id).first()

            assertEquals(listOf(projectlessTask), actualProjectlessTasks)
            assertEquals(listOf(projectTask), actualProjectTasks)
        } finally {
            driver.close()
        }
    }
}
