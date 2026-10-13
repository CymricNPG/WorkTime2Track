package net.npg.wt2t.ui.format

import kotlinx.datetime.LocalTime

/** Formats a duration as hours and minutes without wrapping it at midnight. */
internal fun formatHourMinuteDuration(totalMinutes: Int): String {
    val hours = (totalMinutes / MINUTES_PER_HOUR).toString().padStart(2, '0')
    val minutes = (totalMinutes % MINUTES_PER_HOUR).toString().padStart(2, '0')
    return "$hours:$minutes"
}

/** Returns the nearest clock-picker value without changing the stored duration. */
internal fun toDurationPickerTime(totalMinutes: Int): LocalTime {
    val pickerMinutes = totalMinutes.coerceIn(0, MAX_PICKER_MINUTES)
    return LocalTime(pickerMinutes / MINUTES_PER_HOUR, pickerMinutes % MINUTES_PER_HOUR)
}

private const val MINUTES_PER_HOUR = 60
private const val MAX_PICKER_MINUTES = 24 * MINUTES_PER_HOUR - 1
