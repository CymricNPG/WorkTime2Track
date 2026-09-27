package net.npg.wt2t.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.repository.ProjectRepository
import net.npg.wt2t.data.repository.TaskRepository
import net.npg.wt2t.data.repository.TimeRepository

/** Owns project and task management rules. */
class ProjectManagementUseCase(
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val timeRepository: TimeRepository,
) {
    fun getAllProjects(): Flow<List<Project>> = projectRepository.getAllProjects()
    fun getAllTasks(): Flow<List<Task>> = taskRepository.getAllTasks()
    fun getTasksByProject(projectId: String?): Flow<List<Task>> = taskRepository.getTasksByProject(projectId)

    suspend fun createProject(name: String): Project {
        validateProjectName(name)
        val project = Project(name = name)
        projectRepository.saveProject(project)
        return project
    }

    suspend fun updateProject(project: Project) {
        validateProjectName(project.name, project.id)
        projectRepository.saveProject(project)
    }

    suspend fun closeProject(projectId: String) {
        projectRepository.getAllProjects().first().find { it.id == projectId }
            ?.let { projectRepository.saveProject(it.copy(closed = true)) }
    }

    suspend fun deleteProject(projectId: String) {
        val tasks = taskRepository.getTasksByProject(projectId).first()
        check(timeRepository.getAllTimes().first().none { it.taskId in tasks.map(Task::id).toSet() }) {
            "Cannot delete project with existing bookings"
        }
        tasks.forEach { taskRepository.deleteTask(it.id) }
        projectRepository.deleteProject(projectId)
    }

    suspend fun createTask(name: String, projectId: String?, freeTime: Boolean = false): Task {
        validateTaskName(name, projectId)
        val task = Task(name = name, projectId = projectId, freeTime = freeTime)
        taskRepository.saveTask(task)
        return task
    }

    suspend fun updateTask(task: Task) {
        validateTaskName(task.name, task.projectId, task.id)
        taskRepository.saveTask(task)
    }

    suspend fun closeTask(taskId: String) {
        taskRepository.getAllTasks().first().find { it.id == taskId }
            ?.let { taskRepository.saveTask(it.copy(closed = true)) }
    }

    suspend fun deleteTask(taskId: String) {
        check(timeRepository.getAllTimes().first().none { it.taskId == taskId }) {
            "Cannot delete task with existing bookings"
        }
        taskRepository.deleteTask(taskId)
    }

    private suspend fun validateProjectName(name: String, excludeId: String? = null) {
        require(name.isNotBlank()) { "Project name cannot be empty" }
        require(projectRepository.getAllProjects().first().none { it.name == name && it.id != excludeId }) {
            "Project name must be unique"
        }
    }

    private suspend fun validateTaskName(name: String, projectId: String?, excludeId: String? = null) {
        require(name.isNotBlank()) { "Task name cannot be empty" }
        require(taskRepository.getTasksByProject(projectId).first().none { it.name == name && it.id != excludeId }) {
            "Task name must be unique within project"
        }
    }
}
