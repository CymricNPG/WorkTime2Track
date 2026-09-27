package net.npg.wt2t.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.domain.usecase.ProjectManagementUseCase
import org.jetbrains.compose.resources.StringResource
import worktime2track.shared.generated.resources.Res
import worktime2track.shared.generated.resources.project_delete_failed

/** Represents the observable UI state for project list. */
data class ProjectListUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: StringResource? = null
)

/** Manages state and user actions for project list. */
class ProjectListViewModel(
    private val projectManagementUseCase: ProjectManagementUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectListUiState())
    val uiState: StateFlow<ProjectListUiState> = _uiState.asStateFlow()

    init {
        loadProjects()
    }

    /** Loads projects. */
    private fun loadProjects() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            projectManagementUseCase.getAllProjects().collect { projects ->
                _uiState.update { it.copy(projects = projects.sortedBy { p -> p.name }, isLoading = false) }
            }
        }
    }

    /** Deletes project. */
    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            try {
                projectManagementUseCase.deleteProject(projectId)
            } catch (exception: Exception) {
                logBoundaryException(exception, "Project deletion failed for $projectId")
                _uiState.update { it.copy(errorMessage = Res.string.project_delete_failed) }
            }
        }
    }

    /** Clears error. */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
