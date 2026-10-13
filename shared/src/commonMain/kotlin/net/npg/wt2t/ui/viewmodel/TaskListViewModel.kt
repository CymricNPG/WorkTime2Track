package net.npg.wt2t.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.domain.usecase.ProjectManagementUseCase
import org.jetbrains.compose.resources.StringResource
import worktime2track.shared.generated.resources.Res
import worktime2track.shared.generated.resources.task_delete_failed

/** Represents the observable UI state for task list. */
data class TaskListUiState(
    val projectId: String? = null,
    val project: Project? = null,
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: StringResource? = null
)

/** Manages state and user actions for task list. */
class TaskListViewModel(
    private val projectId: String?,
    private val projectManagementUseCase: ProjectManagementUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListUiState(projectId = projectId))
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    /** Loads data. */
    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val projectFlow = if (projectId != null) {
                projectManagementUseCase.getAllProjects().map { list -> list.find { it.id == projectId } }
            } else {
                flowOf(null)
            }

            val tasksFlow = projectManagementUseCase.getTasksByProject(projectId)

            combine(projectFlow, tasksFlow) { project, tasks ->
                project to tasks.sortedBy { it.name }
            }.collect { (project, tasks) ->
                _uiState.update {
                    it.copy(
                        project = project,
                        tasks = tasks,
                        isLoading = false
                    )
                }
            }
        }
    }

    /** Deletes task. */
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                projectManagementUseCase.deleteTask(taskId)
            } catch (exception: Exception) {
                logBoundaryException(exception, "Task deletion failed for $taskId")
                _uiState.update { it.copy(errorMessage = Res.string.task_delete_failed) }
            }
        }
    }

    /** Clears error. */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
