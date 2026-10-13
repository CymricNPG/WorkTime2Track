package net.npg.wt2t.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.npg.wt2t.domain.usecase.EndOfDayUseCase
import net.npg.wt2t.data.model.atMinutePrecision
import org.jetbrains.compose.resources.StringResource
import worktime2track.shared.generated.resources.Res
import worktime2track.shared.generated.resources.end_of_day_load_failed
import worktime2track.shared.generated.resources.end_of_day_no_bookings
import worktime2track.shared.generated.resources.end_of_day_save_failed
import kotlin.time.Clock
import kotlin.time.Duration

/** Represents the observable UI state for end of day. */
data class EndOfDayUiState(
    val date: LocalDate,
    val firstBooking: LocalTime? = null,
    val lastBooking: LocalTime? = null,
    val workedTime: Duration = Duration.ZERO,
    val targetMinutes: Int = 0,
    val breakMinutes: Int = 0,
    val isLoading: Boolean = true,
    val isLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: StringResource? = null,
)

/** Manages state and user actions for end of day. */
class EndOfDayViewModel(
    private val endOfDayUseCase: EndOfDayUseCase,
    private val clock: Clock,
) : ViewModel() {
    private val date = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    private val _uiState = MutableStateFlow(EndOfDayUiState(date = date))
    val uiState: StateFlow<EndOfDayUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    /** Updates target time. */
    fun updateTargetTime(hours: Int, minutes: Int) {
        _uiState.update {
            it.copy(
                targetMinutes = hours * MINUTES_PER_HOUR + minutes,
                errorMessage = null,
            )
        }
    }

    /** Updates the break duration. */
    fun updateBreakTime(hours: Int, minutes: Int) {
        _uiState.update {
            it.copy(
                breakMinutes = hours * MINUTES_PER_HOUR + minutes,
                errorMessage = null,
            )
        }
    }

    /** Saves the selected target time and ends the current workday. */
    fun saveAndEndDay() {
        val state = _uiState.value
        if (!state.isLoaded || state.isSaving) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val endTime = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).time.atMinutePrecision()
            try {
                endOfDayUseCase.complete(
                    date = state.date,
                    endTime = endTime,
                    targetMinutes = state.targetMinutes,
                    breakMinutes = state.breakMinutes,
                )
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (exception: IllegalStateException) {
                logBoundaryException(exception, "End of day rejected for ${state.date}")
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = Res.string.end_of_day_no_bookings,
                    )
                }
            } catch (exception: Exception) {
                logBoundaryException(exception, "End of day failed for ${state.date}")
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = Res.string.end_of_day_save_failed,
                    )
                }
            }
        }
    }

    /** Loads end-of-day values and observes the current day's bookings. */
    private fun loadData() {
        viewModelScope.launch {
            try {
                val endOfDayData = endOfDayUseCase.load(date)
                _uiState.update {
                    it.copy(
                        targetMinutes = endOfDayData.targetMinutes,
                        breakMinutes = endOfDayData.breakMinutes,
                        isLoading = false,
                        isLoaded = true,
                        errorMessage = null,
                    )
                }
                endOfDayUseCase.getTimesForDate(date).collect { times ->
                    val firstBooking = times.minByOrNull { it.start }?.start
                    val lastBooking = times
                        .maxByOrNull { it.end ?: it.start }
                        ?.let { it.end ?: it.start }
                    _uiState.update {
                        it.copy(
                            firstBooking = firstBooking,
                            lastBooking = lastBooking,
                            workedTime = endOfDayUseCase.getWorkedTime(date),
                        )
                    }
                }
            } catch (exception: Exception) {
                logBoundaryException(exception, "End of day load failed for $date")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoaded = false,
                        errorMessage = Res.string.end_of_day_load_failed,
                    )
                }
            }
        }
    }

    private companion object {
        const val MINUTES_PER_HOUR = 60
    }
}
