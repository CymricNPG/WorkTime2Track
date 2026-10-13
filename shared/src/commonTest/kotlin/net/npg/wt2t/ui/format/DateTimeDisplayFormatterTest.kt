package net.npg.wt2t.ui.format

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeDisplayFormatterTest {

    @Test
    fun formatsGermanDate() {
        val inputDate = LocalDate(2026, 6, 9)

        val actualDate = inputDate.formatForDisplay("de")

        assertEquals("09.06.2026", actualDate)
    }

    @Test
    fun formatsEnglishDate() {
        val inputDate = LocalDate(2026, 6, 9)

        val actualDate = inputDate.formatForDisplay("en")

        assertEquals("06/09/2026", actualDate)
    }

    @Test
    fun formatsGermanTimeWithoutSecondsOrNanoseconds() {
        val inputTime = LocalTime(8, 5, 42, 123_000_000)

        val actualTime = inputTime.formatForDisplay("de")

        assertEquals("08:05", actualTime)
    }

    @Test
    fun formatsEnglishTimeWithoutSecondsOrNanoseconds() {
        val inputTime = LocalTime(20, 5, 42, 123_000_000)

        val actualTime = inputTime.formatForDisplay("en")

        assertEquals("8:05 PM", actualTime)
    }
}
