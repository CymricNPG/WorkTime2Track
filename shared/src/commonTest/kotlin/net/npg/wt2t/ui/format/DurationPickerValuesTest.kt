package net.npg.wt2t.ui.format

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DurationPickerValuesTest {
    @Test
    fun `durations display as zero padded hours and minutes without a clock period`() {
        val inputDurations = mapOf(
            0 to "00:00",
            45 to "00:45",
            510 to "08:30",
            1_439 to "23:59",
        )

        inputDurations.forEach { (inputMinutes, expectedText) ->
            val actualText = formatHourMinuteDuration(inputMinutes)

            assertEquals(expectedText, actualText)
        }
    }

    @Test
    fun `loaded durations of a day or longer display without wrapping`() {
        val inputDurations = mapOf(1_440 to "24:00", 1_500 to "25:00")

        inputDurations.forEach { (inputMinutes, expectedText) ->
            val actualText = formatHourMinuteDuration(inputMinutes)

            assertEquals(expectedText, actualText)
        }
    }

    @Test
    fun `supported durations initialize the picker with the same hours and minutes`() {
        val inputDurations = mapOf(
            0 to LocalTime(0, 0),
            45 to LocalTime(0, 45),
            510 to LocalTime(8, 30),
            1_439 to LocalTime(23, 59),
        )

        inputDurations.forEach { (inputMinutes, expectedTime) ->
            val actualTime = toDurationPickerTime(inputMinutes)

            assertEquals(expectedTime, actualTime)
        }
    }

    @Test
    fun `picker drafts use the nearest supported time for longer durations`() {
        val inputDurations = listOf(1_440, 1_500, Int.MAX_VALUE)

        inputDurations.forEach { inputMinutes ->
            val actualTime = toDurationPickerTime(inputMinutes)

            assertEquals(LocalTime(23, 59), actualTime)
        }
    }
}
