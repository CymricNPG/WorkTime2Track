package net.npg.wt2t.ui.viewmodel

import kotlinx.datetime.LocalTime
import net.npg.wt2t.data.model.atMinutePrecision
import net.npg.wt2t.data.model.BookingAdjustmentDirection

/** Identifies the editable boundary of a time entry. */
enum class EditableTimeBoundary {
    Start,
    End,
}

/** Describes which complete configured adjustments are valid for one editable entry. */
data class TimeAdjustmentAvailability(
    val canAdjustStartEarlier: Boolean = false,
    val canAdjustStartLater: Boolean = false,
    val canAdjustEndEarlier: Boolean = false,
    val canAdjustEndLater: Boolean = false,
)

/** Returns which time adjustments can be applied without creating an invalid day draft. */
internal fun calculateTimeAdjustmentAvailability(
    entries: List<EditableTimeEntry>,
    entryId: String,
    adjustmentMinutes: Int,
): TimeAdjustmentAvailability = TimeAdjustmentAvailability(
    canAdjustStartEarlier = canAdjust(
        entries,
        entryId,
        EditableTimeBoundary.Start,
        BookingAdjustmentDirection.EARLIER,
        adjustmentMinutes,
    ),
    canAdjustStartLater = canAdjust(
        entries,
        entryId,
        EditableTimeBoundary.Start,
        BookingAdjustmentDirection.LATER,
        adjustmentMinutes,
    ),
    canAdjustEndEarlier = canAdjust(
        entries,
        entryId,
        EditableTimeBoundary.End,
        BookingAdjustmentDirection.EARLIER,
        adjustmentMinutes,
    ),
    canAdjustEndLater = canAdjust(
        entries,
        entryId,
        EditableTimeBoundary.End,
        BookingAdjustmentDirection.LATER,
        adjustmentMinutes,
    ),
)

/** Applies one complete adjustment, returning null when it would make the day draft invalid. */
internal fun adjustTimeEntries(
    entries: List<EditableTimeEntry>,
    entryId: String,
    boundary: EditableTimeBoundary,
    direction: BookingAdjustmentDirection,
    adjustmentMinutes: Int,
): List<EditableTimeEntry>? {
    if (adjustmentMinutes !in ADJUSTMENT_MINUTES_RANGE) return null

    val parsedEntries = entries.map { entry -> entry.parse() ?: return null }.sortedBy { it.start }
    val selectedIndex = parsedEntries.indexOfFirst { it.entry.id == entryId }
    if (selectedIndex < 0) return null

    val selected = parsedEntries[selectedIndex]
    val sourceTime = when (boundary) {
        EditableTimeBoundary.Start -> selected.start
        EditableTimeBoundary.End -> selected.end
    }
    val adjustedTime = sourceTime.adjust(direction, adjustmentMinutes) ?: return null
    val updates = mutableMapOf(entryId to selected.entry.withBoundary(boundary, adjustedTime))

    when (boundary) {
        EditableTimeBoundary.Start -> parsedEntries.getOrNull(selectedIndex - 1)
            ?.takeIf { it.end == selected.start }
            ?.let { previous ->
                updates[previous.entry.id] = previous.entry.copy(end = adjustedTime.toString())
            }

        EditableTimeBoundary.End -> parsedEntries.getOrNull(selectedIndex + 1)
            ?.takeIf { it.start == selected.end }
            ?.let { next ->
                updates[next.entry.id] = next.entry.copy(start = adjustedTime.toString())
            }
    }

    val adjustedEntries = entries.map { entry -> updates[entry.id] ?: entry }
    return adjustedEntries.takeIf { candidateEntries ->
        areValidTimeEntries(candidateEntries) &&
            haveMinimumDuration(candidateEntries, updates.keys, adjustmentMinutes)
    }
}

private fun canAdjust(
    entries: List<EditableTimeEntry>,
    entryId: String,
    boundary: EditableTimeBoundary,
    direction: BookingAdjustmentDirection,
    adjustmentMinutes: Int,
): Boolean = adjustTimeEntries(
    entries,
    entryId,
    boundary,
    direction,
    adjustmentMinutes,
) != null

private fun EditableTimeEntry.parse(): ParsedEditableTimeEntry? {
    val parsedStart = runCatching { LocalTime.parse(start) }.getOrNull()?.atMinutePrecision() ?: return null
    val parsedEnd = runCatching { LocalTime.parse(end) }.getOrNull()?.atMinutePrecision() ?: return null
    return ParsedEditableTimeEntry(this, parsedStart, parsedEnd)
}

private fun EditableTimeEntry.withBoundary(
    boundary: EditableTimeBoundary,
    time: LocalTime,
): EditableTimeEntry = when (boundary) {
    EditableTimeBoundary.Start -> copy(start = time.toString())
    EditableTimeBoundary.End -> copy(end = time.toString())
}

private fun LocalTime.adjust(
    direction: BookingAdjustmentDirection,
    adjustmentMinutes: Int,
): LocalTime? {
    val adjustedMinute = toMinuteOfDay() + direction.multiplier * adjustmentMinutes
    return adjustedMinute
        .takeIf { it in 0 until MINUTES_PER_DAY }
        ?.let { minute -> LocalTime.fromSecondOfDay(minute * SECONDS_PER_MINUTE) }
}

private fun areValidTimeEntries(entries: List<EditableTimeEntry>): Boolean {
    val parsedEntries = entries.map { entry -> entry.parse() ?: return false }.sortedBy { it.start }
    val intervalEntries = parsedEntries.filterNot { it.start == it.end }
    return parsedEntries.all { it.start <= it.end } &&
        intervalEntries.zipWithNext().all { (previous, next) -> previous.end <= next.start }
}

private fun haveMinimumDuration(
    entries: List<EditableTimeEntry>,
    affectedEntryIds: Set<String>,
    adjustmentMinutes: Int,
): Boolean = entries
    .filter { it.id in affectedEntryIds }
    .map { entry -> entry.parse() ?: return false }
    .all { entry -> entry.end.toMinuteOfDay() - entry.start.toMinuteOfDay() >= adjustmentMinutes }

private fun LocalTime.toMinuteOfDay(): Int = hour * MINUTES_PER_HOUR + minute

private data class ParsedEditableTimeEntry(
    val entry: EditableTimeEntry,
    val start: LocalTime,
    val end: LocalTime,
)

private val ADJUSTMENT_MINUTES_RANGE = 1..30
private const val SECONDS_PER_MINUTE = 60
private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 1_440
