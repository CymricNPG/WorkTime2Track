package net.npg.wt2t.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.domain.usecase.ProjectManagementUseCase
import org.jetbrains.compose.resources.StringResource
import worktime2track.shared.generated.resources.Res
import worktime2track.shared.generated.resources.task_not_found
import worktime2track.shared.generated.resources.task_save_failed

/** Represents the observable UI state for task. */
data class TaskUiState(
    val id: String? = null,
    val projectId: String? = null,
    val name: String = "",
    val freeTime: Boolean = false,
    val closed: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: StringResource? = null
)

/** Manages state and user actions for task. */
class TaskViewModel(
    private val taskId: String?,
    private val projectId: String?,
    private val projectManagementUseCase: ProjectManagementUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState(id = taskId, projectId = projectId))
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    init {
        if (taskId != null) {
            loadTask()
        }
    }

    /** Loads task. */
    private fun loadTask() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val task = projectManagementUseCase.getAllTasks().first().find { it.id == taskId }
            if (task != null) {
                _uiState.update {
                    it.copy(
                        name = task.name,
                        freeTime = task.freeTime,
                        closed = task.closed,
                        projectId = task.projectId,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = Res.string.task_not_found) }
            }
        }
    }

    /** Updates name. */
    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    /** Updates whether the task represents free time. */
    fun updateFreeTime(freeTime: Boolean) {
        _uiState.update { it.copy(freeTime = freeTime) }
    }

    /** Updates whether the task is closed. */
    fun updateClosed(closed: Boolean) {
        _uiState.update { it.copy(closed = closed) }
    }

    /** Saves task. */
    fun saveTask() {
        viewModelScope.launch {
            try {
                val current = _uiState.value
                if (current.id == null) {
                    projectManagementUseCase.createTask(current.name, current.projectId, current.freeTime)
                } else {
                    projectManagementUseCase.updateTask(
                        Task(
                            id = current.id,
                            name = current.name,
                            projectId = current.projectId,
                            freeTime = current.freeTime,
                            closed = current.closed
                        )
                    )
                }
                _uiState.update { it.copy(isSaved = true) }
            } catch (exception: Exception) {
                logBoundaryException(exception, "Task save failed")
                _uiState.update { it.copy(errorMessage = Res.string.task_save_failed) }
            }
        }
    }
}
