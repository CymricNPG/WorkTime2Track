package net.npg.wt2t.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.data.model.atMinutePrecision
import net.npg.wt2t.data.model.BookingAdjustmentDirection
import net.npg.wt2t.domain.usecase.DayEditingUseCase
import net.npg.wt2t.domain.usecase.UpdateDayRequest
import org.jetbrains.compose.resources.StringResource
import worktime2track.shared.generated.resources.Res
import worktime2track.shared.generated.resources.edit_day_delete_failed
import worktime2track.shared.generated.resources.edit_day_invalid_time
import worktime2track.shared.generated.resources.edit_day_save_failed

/**
 * UI state for editing a complete work day.
 *
 * @property date Date being edited.
 * @property entries Draft time entries.
 * @property tasks Available tasks for reassignment.
 * @property projects Projects keyed by id for task display.
 * @property targetHours Target work time hour component.
 * @property targetMinutes Target work time minute component.
 * @property breakHours Break time hour component.
 * @property breakMinutes Break time minute component.
 * @property timeAdjustmentMinutes Configured step for start and end adjustments.
 * @property isLoading Whether initial data is loading.
 * @property isSaving Whether a save operation is running.
 * @property isSaved Whether the latest save succeeded.
 * @property isDeleting Whether a day deletion is running.
 * @property isDeleted Whether the day was deleted successfully.
 * @property hasChanges Whether the draft differs from the loaded data.
 * @property errorMessage Message resource for the latest error.
 */
data class EditDayUiState(
    val date: LocalDate,
    val entries: List<EditableTimeEntry> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val projects: Map<String, Project> = emptyMap(),
    val targetHours: String = "0",
    val targetMinutes: String = "0",
    val breakHours: String = "0",
    val breakMinutes: String = "0",
    val timeAdjustmentMinutes: Int = DEFAULT_TIME_ADJUSTMENT_MINUTES,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val hasChanges: Boolean = false,
    val errorMessage: StringResource? = null,
) {
    /** Whether the loaded draft contains no booking entries and can be deleted. */
    val canDeleteDay: Boolean
        get() = !isLoading && entries.isEmpty()

    /** Valid time adjustments keyed by editable entry id. */
    val adjustmentAvailabilityByEntryId: Map<String, TimeAdjustmentAvailability>
        get() = entries.associate { entry ->
            entry.id to calculateTimeAdjustmentAvailability(
                entries = entries,
                entryId = entry.id,
                adjustmentMinutes = timeAdjustmentMinutes,
            )
        }

    private companion object {
        const val DEFAULT_TIME_ADJUSTMENT_MINUTES = 5
    }
}

/**
 * Draft representation of a single editable time entry.
 *
 * @property id Stable entry id.
 * @property taskId Selected task id.
 * @property start Start time as an editable text value.
 * @property end End time as an editable text value.
 * @property description Notes as editable text.
 */
data class EditableTimeEntry(
    val id: String,
    val taskId: String,
    val start: String,
    val end: String,
    val description: String = "",
)

/**
 * ViewModel that manages a mutable draft for one editable day.
 *
 * @property date Date being edited.
 * @property dayEditingUseCase Use case used to load and change the day.
 */
class EditDayViewModel(
    private val date: LocalDate,
    private val dayEditingUseCase: DayEditingUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditDayUiState(date = date))
    val uiState: StateFlow<EditDayUiState> = _uiState.asStateFlow()

    init {
        loadDay()
    }

    /** Updates target hours. */
    fun updateTargetHours(hours: String) {
        updateDraft { it.copy(targetHours = hours.filter(Char::isDigit)) }
    }

    /** Updates target minutes. */
    fun updateTargetMinutes(minutes: String) {
        updateDraft { it.copy(targetMinutes = minutes.filter(Char::isDigit).take(MAX_MINUTE_DIGITS)) }
    }

    /** Updates break hours. */
    fun updateBreakHours(hours: String) {
        updateDraft { it.copy(breakHours = hours.filter(Char::isDigit)) }
    }

    /** Updates break minutes. */
    fun updateBreakMinutes(minutes: String) {
        updateDraft { it.copy(breakMinutes = minutes.filter(Char::isDigit).take(MAX_MINUTE_DIGITS)) }
    }

    /** Updates entry task. */
    fun updateEntryTask(entryId: String, taskId: String) {
        updateEntry(entryId) { it.copy(taskId = taskId) }
    }

    /** Updates entry start. */
    fun updateEntryStart(entryId: String, start: String) {
        updateEntry(entryId) { it.copy(start = start) }
    }

    /** Updates entry end. */
    fun updateEntryEnd(entryId: String, end: String) {
        updateEntry(entryId) { it.copy(end = end) }
    }

    /** Moves one entry boundary by the complete configured step when the resulting draft is valid. */
    fun adjustEntryTime(
        entryId: String,
        boundary: EditableTimeBoundary,
        direction: BookingAdjustmentDirection,
    ) {
        val state = _uiState.value
        val adjustedEntries = adjustTimeEntries(
            entries = state.entries,
            entryId = entryId,
            boundary = boundary,
            direction = direction,
            adjustmentMinutes = state.timeAdjustmentMinutes,
        ) ?: return
        updateDraft { it.copy(entries = adjustedEntries) }
    }

    /** Updates entry description. */
    fun updateEntryDescription(entryId: String, description: String) {
        updateEntry(entryId) { it.copy(description = description) }
    }

    /** Adds entry. */
    fun addEntry() {
        val taskId = _uiState.value.tasks.firstOrNull()?.id ?: return
        val entry = EditableTimeEntry(
            id = uuid4().toString(),
            taskId = taskId,
            start = DEFAULT_START_TIME,
            end = DEFAULT_END_TIME,
        )
        updateDraft { it.copy(entries = it.entries + entry) }
    }

    /** Deletes entry. */
    fun deleteEntry(entryId: String) {
        updateDraft { it.copy(entries = it.entries.filterNot { entry -> entry.id == entryId }) }
    }

    /** Saves day. */
    fun saveDay() {
        val state = _uiState.value
        if (state.isSaving || state.isDeleting || state.isDeleted) return
        val request = createUpdateRequest()
        if (request == null) {
            _uiState.update { it.copy(errorMessage = Res.string.edit_day_invalid_time) }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                dayEditingUseCase.execute(request)
                _uiState.update { it.copy(isSaving = false, isSaved = true, hasChanges = false) }
            } catch (exception: IllegalArgumentException) {
                logBoundaryException(exception, "Day validation failed for $date")
                _uiState.update { it.copy(isSaving = false, errorMessage = Res.string.edit_day_invalid_time) }
            } catch (exception: Exception) {
                logBoundaryException(exception, "Day save failed for $date")
                _uiState.update { it.copy(isSaving = false, errorMessage = Res.string.edit_day_save_failed) }
            }
        }
    }

    /** Deletes the complete day when its current draft contains no bookings. */
    fun deleteDay() {
        val state = _uiState.value
        if (!state.canDeleteDay || state.isSaving || state.isDeleting || state.isDeleted) return

        _uiState.update { it.copy(isDeleting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                dayEditingUseCase.delete(date)
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        isDeleted = true,
                        hasChanges = false,
                    )
                }
            } catch (exception: Exception) {
                logBoundaryException(exception, "Day deletion failed for $date")
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = Res.string.edit_day_delete_failed,
                    )
                }
            }
        }
    }

    /** Restores the last loaded day and clears unsaved edits. */
    fun discardChanges() {
        loadDay()
    }

    /** Loads day. */
    private fun loadDay() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val data = dayEditingUseCase.load(date)
            val times = data.times
            val dailyWorkTime = data.dailyWorkTime
            val tasks = data.tasks
            val projects = data.projects
            val timeAdjustmentMinutes = data.bookingStartAdjustmentMinutes
            val targetMinutes = dailyWorkTime?.minutes ?: 0
            val breakMinutes = dailyWorkTime?.breakMinutes ?: 0

            _uiState.update {
                it.copy(
                    entries = times.sortedBy(Time::start).map { time -> time.toEditableEntry() },
                    tasks = tasks,
                    projects = projects,
                    targetHours = (targetMinutes / MINUTES_PER_HOUR).toString(),
                    targetMinutes = (targetMinutes % MINUTES_PER_HOUR).toString(),
                    breakHours = (breakMinutes / MINUTES_PER_HOUR).toString(),
                    breakMinutes = (breakMinutes % MINUTES_PER_HOUR).toString(),
                    timeAdjustmentMinutes = timeAdjustmentMinutes,
                    isLoading = false,
                    isSaved = false,
                    isDeleted = false,
                    hasChanges = false,
                )
            }
        }
    }

    /** Updates entry. */
    private fun updateEntry(
        entryId: String,
        transform: (EditableTimeEntry) -> EditableTimeEntry,
    ) {
        updateDraft { state ->
            state.copy(
                entries = state.entries.map { entry ->
                    if (entry.id == entryId) transform(entry) else entry
                },
            )
        }
    }

    /** Updates draft. */
    private fun updateDraft(transform: (EditDayUiState) -> EditDayUiState) {
        _uiState.update { transform(it).copy(hasChanges = true, isSaved = false, errorMessage = null) }
    }

    /** Creates update request. */
    private fun createUpdateRequest(): UpdateDayRequest? {
        val state = _uiState.value
        val targetMinutes = parseDuration(state.targetHours, state.targetMinutes) ?: return null
        val breakMinutes = parseDuration(state.breakHours, state.breakMinutes) ?: return null
        val times = state.entries.map { entry -> entry.toTimeOrNull(date) ?: return null }
        return UpdateDayRequest(
            date = date,
            targetMinutes = targetMinutes,
            times = times,
            breakMinutes = breakMinutes,
        )
    }

    /** Parses editable hours and minutes into a duration in minutes. */
    private fun parseDuration(hours: String, minutes: String): Int? {
        val hourValue = hours.toIntOrNull() ?: return null
        val minuteValue = minutes.toIntOrNull() ?: return null
        if (minuteValue >= MINUTES_PER_HOUR) return null
        return hourValue * MINUTES_PER_HOUR + minuteValue
    }

    /** Converts this stored booking to an editable entry. */
    private fun Time.toEditableEntry(): EditableTimeEntry = EditableTimeEntry(
        id = id,
        taskId = taskId,
        start = start.toMinuteText(),
        end = end?.toMinuteText().orEmpty(),
        description = description.joinToString(separator = "\n"),
    )

    /** Formats this time at the minute precision supported by the editor. */
    private fun LocalTime.toMinuteText(): String =
        "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

    /** Converts this editable entry to a booking when its values are valid. */
    private fun EditableTimeEntry.toTimeOrNull(date: LocalDate): Time? {
        val parsedStart = runCatching { LocalTime.parse(start) }.getOrNull()?.atMinutePrecision() ?: return null
        val parsedEnd = runCatching { LocalTime.parse(end) }.getOrNull()?.atMinutePrecision() ?: return null
        return Time(
            id = id,
            taskId = taskId,
            date = date,
            start = parsedStart,
            end = parsedEnd,
            description = description.lines().filter(String::isNotBlank),
        )
    }

    private companion object {
        const val DEFAULT_START_TIME = "09:00"
        const val DEFAULT_END_TIME = "10:00"
        const val MAX_MINUTE_DIGITS = 2
        const val MINUTES_PER_HOUR = 60
    }
}
