package net.npg.wt2t.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.data.model.atMinutePrecision
import net.npg.wt2t.data.model.BookingAdjustmentDirection
import net.npg.wt2t.domain.usecase.BookingUseCase
import net.npg.wt2t.domain.usecase.ReportingUseCase
import org.jetbrains.compose.resources.StringResource
import worktime2track.shared.generated.resources.Res
import worktime2track.shared.generated.resources.booking_start_adjustment_failed
import worktime2track.shared.generated.resources.booking_stop_failed
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Represents the observable UI state for booking. */
data class BookingUiState(
    val tasks: List<Task> = emptyList(),
    val projects: Map<String, Project> = emptyMap(),
    val activeTime: Time? = null,
    val isLoading: Boolean = false,
    val note: String = "",
    val canEndDay: Boolean = false,
    val bookingStartAdjustmentMinutes: Int = 5,
    val canAdjustBookingStartEarlier: Boolean = false,
    val canAdjustBookingStartLater: Boolean = false,
    val isAdjustingBookingStart: Boolean = false,
    val bookingStartAdjustmentError: StringResource? = null,
    val bookingStopError: StringResource? = null,
    val isZeroMinuteBookingSaved: Boolean = false,
    val monthOvertime: Duration = Duration.ZERO,
    val yearOvertime: Duration = Duration.ZERO,
)

/** Manages state and user actions for booking. */
class BookingViewModel(
    private val bookingUseCase: BookingUseCase,
    private val reportingUseCase: ReportingUseCase,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()
    private var currentTodayTimes: List<Time> = emptyList()
    private var availabilityRefreshJob: Job? = null

    private val today: LocalDate
        get() = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    init {
        loadData()
        refreshOvertime()
    }

    /** Loads data. */
    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val adjustmentMinutes = bookingUseCase.getBookingStartAdjustmentMinutes()
            _uiState.update { it.copy(bookingStartAdjustmentMinutes = adjustmentMinutes) }

            val tasksFlow = bookingUseCase.getAllTasks()
            val projectsFlow = bookingUseCase.getAllProjects()
            val timesFlow = bookingUseCase.getAllTimes()

            combine(
                tasksFlow,
                projectsFlow,
                timesFlow,
            ) { tasks: List<Task>, projects: List<Project>, times: List<Time> ->
                val activeTime = times.find { it.date == today && it.end == null }
                val canEndDay = times.any { it.date == today }
                val projectMap = projects.associateBy { it.id }

                // Filter geschlossene Tasks/Projekte aus, außer sie sind aktiv
                val filteredTasks = tasks.filter { task ->
                    !task.closed || task.id == activeTime?.taskId
                }.filter { task ->
                    val project = task.projectId?.let { projectMap[it] }
                    project == null || !project.closed || task.id == activeTime?.taskId
                }
                val sortedTasks = sortTasksByLastUsed(filteredTasks, times)

                BookingData(
                    tasks = sortedTasks,
                    projects = projectMap,
                    activeTime = activeTime,
                    canEndDay = canEndDay,
                    todayTimes = times.filter { it.date == today },
                )
            }.collect { data ->
                currentTodayTimes = data.todayTimes
                val currentTime = getCurrentTime()
                val availability = bookingUseCase.getBookingAdjustmentAvailability(
                    activeTime = data.activeTime,
                    times = data.todayTimes,
                    adjustmentMinutes = _uiState.value.bookingStartAdjustmentMinutes,
                    currentTime = currentTime,
                )
                _uiState.update {
                    it.copy(
                        tasks = data.tasks,
                        projects = data.projects,
                        activeTime = data.activeTime,
                        isLoading = false,
                        canEndDay = data.canEndDay,
                        canAdjustBookingStartEarlier = availability.canAdjustEarlier,
                        canAdjustBookingStartLater = availability.canAdjustLater,
                    )
                }
                scheduleAvailabilityRefresh(data.activeTime, currentTime)
            }
        }
    }

    /** Groups the task, project, and active-booking data observed by the screen. */
    private data class BookingData(
        val tasks: List<Task>,
        val projects: Map<String, Project>,
        val activeTime: Time?,
        val canEndDay: Boolean,
        val todayTimes: List<Time>,
    )

    /** Reloads the persisted adjustment step when the booking destination becomes visible. */
    fun refreshBookingStartAdjustment() {
        viewModelScope.launch {
            val adjustmentMinutes = bookingUseCase.getBookingStartAdjustmentMinutes()
            _uiState.update { it.copy(bookingStartAdjustmentMinutes = adjustmentMinutes) }
            refreshAdjustmentAvailability()
            scheduleAvailabilityRefresh(_uiState.value.activeTime, getCurrentTime())
        }
    }

    /** Reloads the month and year overtime summary. */
    fun refreshOvertime() {
        viewModelScope.launch {
            updateOvertime()
        }
    }

    /** Starts booking. */
    fun startBooking(taskId: String) {
        viewModelScope.launch {
            val now = getCurrentTime()
            bookingUseCase.startBooking(taskId, today, now)
            updateOvertime()
        }
    }

    /** Stops booking. */
    fun stopBooking() {
        viewModelScope.launch {
            try {
                val completedBooking = bookingUseCase.endBooking(today, getCurrentTime())
                updateOvertime()
                _uiState.update { it.copy(bookingStopError = null) }
                if (completedBooking?.let { it.start == it.end } == true) {
                    _uiState.update {
                        it.copy(
                            isZeroMinuteBookingSaved = true,
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                logBoundaryException(exception, "Booking stop failed")
                _uiState.update {
                    it.copy(
                        bookingStopError = Res.string.booking_stop_failed,
                    )
                }
            }
        }
    }

    /** Marks the zero-minute booking confirmation as shown. */
    fun clearZeroMinuteBookingSaved() {
        _uiState.update { it.copy(isZeroMinuteBookingSaved = false) }
    }

    /** Moves the active booking start by one configured step. */
    fun adjustBookingStart(direction: BookingAdjustmentDirection) {
        val state = _uiState.value
        val canAdjust = when (direction) {
            BookingAdjustmentDirection.EARLIER -> state.canAdjustBookingStartEarlier
            BookingAdjustmentDirection.LATER -> state.canAdjustBookingStartLater
        }
        if (!canAdjust || state.isAdjustingBookingStart) return
        _uiState.update {
            it.copy(
                isAdjustingBookingStart = true,
                bookingStartAdjustmentError = null,
            )
        }
        viewModelScope.launch {
            try {
                bookingUseCase.adjustBookingStart(direction, today, getCurrentTime())
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                logBoundaryException(exception, "Booking start adjustment failed")
                _uiState.update {
                    it.copy(bookingStartAdjustmentError = Res.string.booking_start_adjustment_failed)
                }
            } finally {
                _uiState.update { it.copy(isAdjustingBookingStart = false) }
            }
        }
    }

    /** Updates note. */
    fun updateNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    /** Adds note. */
    fun addNote() {
        val activeTimeId = _uiState.value.activeTime?.id ?: return
        val note = _uiState.value.note
        if (note.isNotBlank()) {
            viewModelScope.launch {
                bookingUseCase.addNoteToTime(activeTimeId, note)
                _uiState.update { it.copy(note = "") }
                updateOvertime()
            }
        }
    }

    private fun refreshAdjustmentAvailability() {
        val state = _uiState.value
        val availability = bookingUseCase.getBookingAdjustmentAvailability(
            activeTime = state.activeTime,
            times = currentTodayTimes,
            adjustmentMinutes = state.bookingStartAdjustmentMinutes,
            currentTime = getCurrentTime(),
        )
        _uiState.update {
            it.copy(
                canAdjustBookingStartEarlier = availability.canAdjustEarlier,
                canAdjustBookingStartLater = availability.canAdjustLater,
            )
        }
    }

    private suspend fun updateOvertime() {
        val overtime = reportingUseCase.loadCurrentOvertime(today)
        _uiState.update {
            it.copy(
                monthOvertime = overtime.month,
                yearOvertime = overtime.year,
            )
        }
    }

    private fun scheduleAvailabilityRefresh(activeTime: Time?, currentTime: LocalTime) {
        availabilityRefreshJob?.cancel()
        if (activeTime == null || _uiState.value.canAdjustBookingStartLater) return
        val targetMinute = activeTime.start.atMinutePrecision().hour * MINUTES_PER_HOUR +
            activeTime.start.atMinutePrecision().minute + _uiState.value.bookingStartAdjustmentMinutes
        val currentMinute = currentTime.atMinutePrecision().hour * MINUTES_PER_HOUR +
            currentTime.atMinutePrecision().minute
        val remainingMinutes = targetMinute - currentMinute
        if (remainingMinutes <= 0 || targetMinute >= MINUTES_PER_DAY) return
        availabilityRefreshJob = viewModelScope.launch {
            delay(remainingMinutes.minutes)
            refreshAdjustmentAvailability()
        }
    }

    private fun getCurrentTime(): LocalTime =
        clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).time.atMinutePrecision()
}

private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 1_440

/** Orders tasks by their most recent booking, followed by unused tasks. */
internal fun sortTasksByLastUsed(tasks: List<Task>, times: List<Time>): List<Task> {
    val lastUsedTimes = times
        .groupBy(Time::taskId)
        .mapValues { (_, taskTimes) ->
            taskTimes.maxWithOrNull(compareBy<Time>(Time::date).thenBy(Time::start))
        }

    return tasks.sortedWith(
        compareByDescending<Task> { lastUsedTimes[it.id]?.date }
            .thenByDescending { lastUsedTimes[it.id]?.start }
    )
}
