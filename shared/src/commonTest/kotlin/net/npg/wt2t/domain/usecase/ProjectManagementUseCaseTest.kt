package net.npg.wt2t.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.domain.service.MockProjectRepository
import net.npg.wt2t.domain.service.MockTaskRepository
import net.npg.wt2t.domain.service.MockTimeRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProjectManagementUseCaseTest {

    private fun setupServices(): Pair<ProjectManagementUseCase, MockTimeRepository> {
        val projectRepo = MockProjectRepository()
        val taskRepo = MockTaskRepository()
        val timeRepo = MockTimeRepository()
        return ProjectManagementUseCase(projectRepo, taskRepo, timeRepo) to timeRepo
    }

    @Test
    fun testCreateProject() = runTest {
        val (pService, _) = setupServices()
        val project = pService.createProject("New Project")
        assertEquals("New Project", project.name)

        val all = pService.getAllProjects().first()
        assertEquals(1, all.size)
    }

    @Test
    fun testDuplicateProjectNameFails() = runTest {
        val (pService, _) = setupServices()
        pService.createProject("Duplicate")
        assertFailsWith<IllegalArgumentException> {
            pService.createProject("Duplicate")
        }
    }

    @Test
    fun testDeleteProjectWithBookingsFails() = runTest {
        val (pService, timeRepository) = setupServices()
        val project = pService.createProject("Project")
        val task = pService.createTask("Task", project.id)

        timeRepository.saveTime(Time(taskId = task.id, date = LocalDate(2023, 10, 27), start = LocalTime(9, 0)))

        assertFailsWith<IllegalStateException> {
            pService.deleteProject(project.id)
        }
    }

    @Test
    fun testDeleteTaskWithBookingsFails() = runTest {
        val (service, timeRepository) = setupServices()
        val task = service.createTask("Task", null)

        timeRepository.saveTime(Time(taskId = task.id, date = LocalDate(2023, 10, 27), start = LocalTime(9, 0)))

        assertFailsWith<IllegalStateException> {
            service.deleteTask(task.id)
        }
    }

    @Test
    fun testCloseProject() = runTest {
        val (pService, _) = setupServices()
        val project = pService.createProject("Open")
        pService.closeProject(project.id)

        val updated = pService.getAllProjects().first().find { it.id == project.id }
        assertTrue(updated!!.closed)
    }
}
