package net.npg.wt2t.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.domain.usecase.ProjectManagementUseCase
import org.jetbrains.compose.resources.StringResource
import worktime2track.shared.generated.resources.Res
import worktime2track.shared.generated.resources.project_name_exists
import worktime2track.shared.generated.resources.project_not_found
import worktime2track.shared.generated.resources.project_save_failed

/** Represents the observable UI state for project. */
data class ProjectUiState(
    val id: String? = null,
    val name: String = "",
    val closed: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: StringResource? = null
)

/** Manages state and user actions for project. */
class ProjectViewModel(
    private val projectId: String?,
    private val projectManagementUseCase: ProjectManagementUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectUiState(id = projectId))
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    init {
        if (projectId != null) {
            loadProject()
        }
    }

    /** Loads project. */
    private fun loadProject() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val project = projectManagementUseCase.getAllProjects().first().find { it.id == projectId }
            if (project != null) {
                _uiState.update {
                    it.copy(
                        name = project.name,
                        closed = project.closed,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = Res.string.project_not_found) }
            }
        }
    }

    /** Updates name. */
    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    /** Updates whether the project is closed. */
    fun updateClosed(closed: Boolean) {
        _uiState.update { it.copy(closed = closed) }
    }

    /** Saves project. */
    fun saveProject() {
        viewModelScope.launch {
            try {
                val current = _uiState.value
                if (current.id == null) {
                    // Check for existing project name
                    val existingProjects = projectManagementUseCase.getAllProjects().first()
                    val nameExists = existingProjects.any { it.name == current.name }

                    if (nameExists) {
                        _uiState.update { it.copy(errorMessage = Res.string.project_name_exists) }
                        return@launch // Abort creation
                    }
                    projectManagementUseCase.createProject(current.name)
                } else {
                    projectManagementUseCase.updateProject(Project(id = current.id, name = current.name, closed = current.closed))
                }
                _uiState.update { it.copy(isSaved = true) }
            } catch (exception: Exception) {
                logBoundaryException(exception, "Project save failed")
                _uiState.update { it.copy(errorMessage = Res.string.project_save_failed) }
            }
        }
    }
}
