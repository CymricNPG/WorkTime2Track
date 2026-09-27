package net.npg.wt2t.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.npg.wt2t.domain.service.SBOMComponent
import net.npg.wt2t.domain.service.SBOMService

/** Represents the observable UI state for the library overview. */
data class LibraryUiState(
    val components: List<SBOMComponent>? = null,
    val hasLoadFailed: Boolean = false,
)

/** Loads and exposes the third-party components shown by the library screen. */
class LibraryViewModel(
    private val sbomService: SBOMService,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadComponents()
    }

    private fun loadComponents() {
        viewModelScope.launch {
            runCatching { sbomService.loadComponents() }.fold(
                onSuccess = { components ->
                    _uiState.update { it.copy(components = components) }
                },
                onFailure = { exception ->
                    logBoundaryException(exception, "Software component list loading failed")
                    _uiState.update { it.copy(hasLoadFailed = true) }
                },
            )
        }
    }
}
