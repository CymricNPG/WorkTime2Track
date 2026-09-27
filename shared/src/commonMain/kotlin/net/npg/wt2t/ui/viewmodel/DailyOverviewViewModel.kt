package net.npg.wt2t.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import net.npg.wt2t.domain.service.PdfExportRequest
import net.npg.wt2t.domain.service.ReportItem
import net.npg.wt2t.domain.service.ReportText
import net.npg.wt2t.domain.usecase.ReportPeriod
import net.npg.wt2t.domain.usecase.ReportingUseCase
import net.npg.wt2t.domain.usecase.DayEditingUseCase
import kotlin.time.Clock
import kotlin.time.Duration

/** Represents the observable UI state for daily overview. */
data class DailyOverviewUiState(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val dayEntries: List<DayEntry> = emptyList(),
    val recordedDates: Set<LocalDate> = emptySet(),
    val isLoading: Boolean = false,
    val isCreatingDay: Boolean = false,
    val creationFailed: Boolean = false,
)

/** Represents day entry data. */
data class DayEntry(
    val date: LocalDate,
    val totalWorkedTime: Duration,
    val overtime: Duration,
    val breakTime: Duration,
    val taskEntries: List<TaskTimeEntry>,
    val reportItems: List<ReportItem>,
)

/** Represents task time entry data. */
data class TaskTimeEntry(
    val taskName: String?,
    val projectName: String?,
    val duration: Duration
)

/** Manages state and user actions for daily overview. */
class DailyOverviewViewModel(
    private val reportingUseCase: ReportingUseCase,
    private val dayEditingUseCase: DayEditingUseCase,
    private val clock: Clock
) : ViewModel() {
    private var loadJob: Job? = null

    private val today: LocalDate
        get() = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    private val _uiState = MutableStateFlow(
        DailyOverviewUiState(
            startDate = today,
            endDate = today,
            isLoading = true,
        )
    )
    val uiState: StateFlow<DailyOverviewUiState> = _uiState.asStateFlow()

    init {
        loadDefaultPeriod()
    }

    /** Updates period. */
    fun updatePeriod(startDate: LocalDate, endDate: LocalDate) {
        require(startDate <= endDate) { "The start date must not be after the end date." }
        _uiState.update { it.copy(startDate = startDate, endDate = endDate) }
        loadData()
    }

    /** Reloads the selected period from persisted data. */
    fun refresh() {
        loadData()
    }

    /** Creates an empty day and makes it visible in the selected period. */
    fun createEmptyDay(date: LocalDate) {
        val state = _uiState.value
        if (state.isCreatingDay || date in state.recordedDates) return

        _uiState.update { it.copy(isCreatingDay = true, creationFailed = false) }
        viewModelScope.launch {
            try {
                dayEditingUseCase.createEmptyDay(date)
                _uiState.update {
                    it.copy(
                        startDate = minOf(it.startDate, date),
                        endDate = maxOf(it.endDate, date),
                        isCreatingDay = false,
                    )
                }
                loadData()
            } catch (_: Exception) {
                _uiState.update { it.copy(isCreatingDay = false, creationFailed = true) }
                loadData()
            }
        }
    }

    /** Clears the empty-day creation error. */
    fun clearCreationError() {
        _uiState.update { it.copy(creationFailed = false) }
    }

    /** Loads default period. */
    private fun loadDefaultPeriod() {
        viewModelScope.launch {
            val period = reportingUseCase.loadDefaultPeriod(today)
            val startDate = period.startDate
            val endDate = period.endDate

            _uiState.update { it.copy(startDate = startDate, endDate = endDate) }
            loadData()
        }
    }

    /** Loads data. */
    private fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val state = _uiState.value

            val period = ReportPeriod(state.startDate, state.endDate)
            val dayEntries = reportingUseCase.loadOverview(period).map { day ->
                DayEntry(day.date, day.totalWorkedTime, day.overtime, day.breakTime, day.taskEntries.map { TaskTimeEntry(it.taskName, it.projectName, it.duration) }, day.items)
            }
            val recordedDates = reportingUseCase.loadRecordedDates()

            _uiState.update { it.copy(dayEntries = dayEntries, recordedDates = recordedDates, isLoading = false) }
        }
    }

    /** Exports the currently selected overview period as a PDF report. */
    fun exportToPdf(reportText: ReportText) {
        viewModelScope.launch {
            reportingUseCase.generatePdfReport(createPdfExportRequest(reportText))
        }
    }

    /** Builds a PDF export request from the current overview state. */
    fun createPdfExportRequest(reportText: ReportText): PdfExportRequest {
        val state = _uiState.value
        return reportingUseCase.createPdfExportRequest(
            ReportPeriod(state.startDate, state.endDate),
            state.dayEntries.map { DayEntry -> net.npg.wt2t.domain.usecase.ReportDay(DayEntry.date, DayEntry.totalWorkedTime, DayEntry.overtime, DayEntry.breakTime, DayEntry.taskEntries.map { net.npg.wt2t.domain.usecase.ReportTaskEntry(it.taskName, it.projectName, it.duration) }, DayEntry.reportItems) },
            reportText,
        )
    }

}

/** Formats a minute count for display in a report. */
