package net.npg.wt2t.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.npg.wt2t.data.model.AppSettings
import net.npg.wt2t.domain.usecase.ConfigurationUseCase
import org.jetbrains.compose.resources.StringResource
import worktime2track.shared.generated.resources.Res
import worktime2track.shared.generated.resources.config_save_failed

/** Defines the persisted application theme modes. */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        /** Parses a persisted theme, using the system theme for corrupt values. */
        fun parseOrDefault(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

/** Represents the observable UI state for config. */
data class ConfigUiState(
    val defaultTargetHours: Int = 8,
    val defaultTargetMinutes: Int = 0,
    val mergeThresholdMinutes: Int = 5,
    val bookingStartAdjustmentMinutes: String = "5",
    val language: String = "de",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: StringResource? = null,
) {
    /** Returns whether the booking-start adjustment draft can be persisted. */
    val isBookingStartAdjustmentValid: Boolean
        get() = bookingStartAdjustmentMinutes.toIntOrNull() in BOOKING_START_ADJUSTMENT_MINUTES_RANGE
}

/** Represents one-time application effects requested by the configuration workflow. */
sealed interface ConfigEffect {
    /** Previews an unsaved theme selection. */
    data class PreviewTheme(
        val themeMode: ThemeMode,
    ) : ConfigEffect

    /** Commits the successfully persisted appearance. */
    data class SaveSucceeded(
        val themeMode: ThemeMode,
        val language: String,
    ) : ConfigEffect

    /** Restores the theme that was active when configuration opened. */
    data object RestoreTheme : ConfigEffect
}

/** Manages state and user actions for config. */
class ConfigViewModel(
    private val configurationUseCase: ConfigurationUseCase,
    initialLanguage: String,
    initialThemeMode: ThemeMode = ThemeMode.SYSTEM,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ConfigUiState(
            language = initialLanguage,
            themeMode = initialThemeMode,
        ),
    )
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()
    private val _effects = Channel<ConfigEffect>(Channel.UNLIMITED)
    val effects: Flow<ConfigEffect> = _effects.receiveAsFlow()

    init {
        loadConfig()
    }

    /** Loads config. */
    private fun loadConfig() {
        viewModelScope.launch {
            val hours = configurationUseCase.getInt(AppSettings.DEFAULT_TARGET_HOURS)
            val minutes = configurationUseCase.getInt(AppSettings.DEFAULT_TARGET_MINUTES)
            val threshold = configurationUseCase.getInt(AppSettings.MERGE_THRESHOLD_MINUTES)
            val bookingStartAdjustment = configurationUseCase.getInt(AppSettings.BOOKING_START_ADJUSTMENT_MINUTES)
            val lang = configurationUseCase.getString(AppSettings.LANGUAGE)
            val theme = configurationUseCase.getString(AppSettings.THEME_MODE)

            _uiState.update {
                it.copy(
                    defaultTargetHours = hours,
                    defaultTargetMinutes = minutes,
                    mergeThresholdMinutes = threshold,
                    bookingStartAdjustmentMinutes = bookingStartAdjustment.toString(),
                    language = lang,
                    themeMode = ThemeMode.parseOrDefault(theme),
                    isLoading = false,
                )
            }
        }
    }

    /** Updates default target time. */
    fun updateDefaultTargetTime(hours: Int, minutes: Int) {
        if (_uiState.value.isSaving) return
        _uiState.update {
            it.copy(
                defaultTargetHours = hours,
                defaultTargetMinutes = minutes,
                errorMessage = null,
            )
        }
    }

    /** Updates merge threshold. */
    fun updateMergeThreshold(minutes: Int) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(mergeThresholdMinutes = minutes, errorMessage = null) }
    }

    /** Updates the booking-start adjustment draft. */
    fun updateBookingStartAdjustment(minutes: String) {
        if (_uiState.value.isSaving) return
        _uiState.update {
            it.copy(
                bookingStartAdjustmentMinutes = minutes,
                errorMessage = null,
            )
        }
    }

    /** Updates language. */
    fun updateLanguage(language: String) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(language = language, errorMessage = null) }
    }

    /** Updates theme mode. */
    fun updateThemeMode(mode: ThemeMode) {
        if (_uiState.value.isSaving) return
        val updatedState = _uiState.value.copy(themeMode = mode, errorMessage = null)
        _uiState.value = updatedState
        emitEffect(ConfigEffect.PreviewTheme(updatedState.themeMode))
    }

    /** Saves config. */
    fun saveConfig() {
        val draft = _uiState.value
        if (draft.isLoading || draft.isSaving || !draft.isBookingStartAdjustmentValid) return
        _uiState.value = draft.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            try {
                configurationUseCase.save(draft.toPersistedValues())
                _uiState.value = draft.copy(isSaving = false)
                emitEffect(ConfigEffect.SaveSucceeded(draft.themeMode, draft.language))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                logBoundaryException(exception, "Configuration save failed")
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = Res.string.config_save_failed,
                    )
                }
                emitEffect(ConfigEffect.RestoreTheme)
            }
        }
    }

    private fun emitEffect(effect: ConfigEffect) {
        check(_effects.trySend(effect).isSuccess) { "Configuration effect could not be delivered." }
    }
}

private fun ConfigUiState.toPersistedValues(): Map<AppSettings, String> = linkedMapOf(
    AppSettings.DEFAULT_TARGET_HOURS to defaultTargetHours.toString(),
    AppSettings.DEFAULT_TARGET_MINUTES to defaultTargetMinutes.toString(),
    AppSettings.MERGE_THRESHOLD_MINUTES to mergeThresholdMinutes.toString(),
    AppSettings.BOOKING_START_ADJUSTMENT_MINUTES to bookingStartAdjustmentMinutes,
    AppSettings.LANGUAGE to language,
    AppSettings.THEME_MODE to themeMode.name,
)

private val BOOKING_START_ADJUSTMENT_MINUTES_RANGE = 1..30
