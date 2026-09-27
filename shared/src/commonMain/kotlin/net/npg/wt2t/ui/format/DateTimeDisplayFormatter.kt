package net.npg.wt2t.ui.format

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** Formats this date for display in the selected language. */
fun LocalDate.formatForDisplay(language: String): String {
    val monthText = (month.ordinal + 1).toString().padStart(2, '0')
    val dayText = day.toString().padStart(2, '0')

    return when (language) {
        LANGUAGE_GERMAN -> "$dayText.$monthText.$year"
        else -> "$monthText/$dayText/$year"
    }
}

/** Formats this time for display in the selected language. */
fun LocalTime.formatForDisplay(language: String): String {
    val minuteText = minute.toString().padStart(2, '0')
    if (language == LANGUAGE_GERMAN) {
        return "${hour.toString().padStart(2, '0')}:$minuteText"
    }

    val period = if (hour < 12) "AM" else "PM"
    val twelveHour = when (val convertedHour = hour % 12) {
        0 -> 12
        else -> convertedHour
    }
    return "$twelveHour:$minuteText $period"
}

private const val LANGUAGE_GERMAN = "de"
