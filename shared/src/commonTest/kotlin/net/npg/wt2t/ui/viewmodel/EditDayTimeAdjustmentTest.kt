package net.npg.wt2t.ui.viewmodel

import net.npg.wt2t.data.model.BookingAdjustmentDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditDayTimeAdjustmentTest {

    @Test
    fun `moving a connected start earlier moves the previous end`() {
        val inputEntries = listOf(
            createEntry(id = "previous", start = "09:00", end = "10:00"),
            createEntry(id = "selected", start = "10:00", end = "11:00"),
        )

        val actualEntries = adjustTimeEntries(
            entries = inputEntries,
            entryId = "selected",
            boundary = EditableTimeBoundary.Start,
            direction = BookingAdjustmentDirection.EARLIER,
            adjustmentMinutes = 5,
        )

        assertEquals("09:55", actualEntries?.first()?.end)
        assertEquals("09:55", actualEntries?.last()?.start)
    }

    @Test
    fun `moving a connected end later moves the next start`() {
        val inputEntries = listOf(
            createEntry(id = "selected", start = "10:00", end = "11:00"),
            createEntry(id = "next", start = "11:00", end = "12:00"),
        )

        val actualEntries = adjustTimeEntries(
            entries = inputEntries,
            entryId = "selected",
            boundary = EditableTimeBoundary.End,
            direction = BookingAdjustmentDirection.LATER,
            adjustmentMinutes = 5,
        )

        assertEquals("11:05", actualEntries?.first()?.end)
        assertEquals("11:05", actualEntries?.last()?.start)
    }

    @Test
    fun `moving a connected start later moves the previous end`() {
        val inputEntries = listOf(
            createEntry(id = "previous", start = "09:00", end = "10:00"),
            createEntry(id = "selected", start = "10:00", end = "11:00"),
        )

        val actualEntries = adjustTimeEntries(
            entries = inputEntries,
            entryId = "selected",
            boundary = EditableTimeBoundary.Start,
            direction = BookingAdjustmentDirection.LATER,
            adjustmentMinutes = 5,
        )

        assertEquals("10:05", actualEntries?.first()?.end)
        assertEquals("10:05", actualEntries?.last()?.start)
    }

    @Test
    fun `moving a connected end earlier moves the next start`() {
        val inputEntries = listOf(
            createEntry(id = "selected", start = "10:00", end = "11:00"),
            createEntry(id = "next", start = "11:00", end = "12:00"),
        )

        val actualEntries = adjustTimeEntries(
            entries = inputEntries,
            entryId = "selected",
            boundary = EditableTimeBoundary.End,
            direction = BookingAdjustmentDirection.EARLIER,
            adjustmentMinutes = 5,
        )

        assertEquals("10:55", actualEntries?.first()?.end)
        assertEquals("10:55", actualEntries?.last()?.start)
    }

    @Test
    fun `moving into a gap changes only the selected boundary`() {
        val inputEntries = listOf(
            createEntry(id = "selected", start = "09:00", end = "10:00"),
            createEntry(id = "next", start = "10:05", end = "11:00"),
        )

        val actualEntries = adjustTimeEntries(
            entries = inputEntries,
            entryId = "selected",
            boundary = EditableTimeBoundary.End,
            direction = BookingAdjustmentDirection.LATER,
            adjustmentMinutes = 3,
        )

        assertEquals("10:03", actualEntries?.first()?.end)
        assertEquals("10:05", actualEntries?.last()?.start)
    }

    @Test
    fun `adjustment drops seconds`() {
        val inputEntries = listOf(createEntry(start = "09:00:30", end = "10:00:30"))

        val actualEntries = adjustTimeEntries(
            entries = inputEntries,
            entryId = "entry",
            boundary = EditableTimeBoundary.Start,
            direction = BookingAdjustmentDirection.LATER,
            adjustmentMinutes = 7,
        )

        assertEquals("09:07", actualEntries?.single()?.start)
    }

    @Test
    fun `same-minute boundaries move together after dropping seconds`() {
        val inputEntries = listOf(
            createEntry(id = "previous", start = "09:00:40", end = "10:00:50"),
            createEntry(id = "selected", start = "10:00:10", end = "11:00:20"),
        )

        val actualEntries = adjustTimeEntries(
            entries = inputEntries,
            entryId = "selected",
            boundary = EditableTimeBoundary.Start,
            direction = BookingAdjustmentDirection.EARLIER,
            adjustmentMinutes = 5,
        )

        assertEquals("09:55", actualEntries?.first()?.end)
        assertEquals("09:55", actualEntries?.last()?.start)
    }

    @Test
    fun `adjustment allows an affected booking exactly one step long`() {
        val inputEntries = listOf(createEntry(start = "09:00", end = "09:10"))

        val actualEntries = adjustTimeEntries(
            entries = inputEntries,
            entryId = "entry",
            boundary = EditableTimeBoundary.Start,
            direction = BookingAdjustmentDirection.LATER,
            adjustmentMinutes = 5,
        )

        assertEquals("09:05", actualEntries?.single()?.start)
    }

    @Test
    fun `adjustment rejects when either affected booking would be shorter than one step`() {
        val shortSelectedEntry = listOf(createEntry(start = "09:00", end = "09:09"))
        val shortPreviousEntry = listOf(
            createEntry(id = "previous", start = "09:00", end = "09:09"),
            createEntry(id = "selected", start = "09:09", end = "10:00"),
        )
        val shortNextEntry = listOf(
            createEntry(id = "selected", start = "09:00", end = "10:00"),
            createEntry(id = "next", start = "10:00", end = "10:09"),
        )

        val selectedResult = adjustTimeEntries(
            entries = shortSelectedEntry,
            entryId = "entry",
            boundary = EditableTimeBoundary.Start,
            direction = BookingAdjustmentDirection.LATER,
            adjustmentMinutes = 5,
        )
        val previousResult = adjustTimeEntries(
            entries = shortPreviousEntry,
            entryId = "selected",
            boundary = EditableTimeBoundary.Start,
            direction = BookingAdjustmentDirection.EARLIER,
            adjustmentMinutes = 5,
        )
        val nextResult = adjustTimeEntries(
            entries = shortNextEntry,
            entryId = "selected",
            boundary = EditableTimeBoundary.End,
            direction = BookingAdjustmentDirection.LATER,
            adjustmentMinutes = 5,
        )

        assertNull(selectedResult)
        assertNull(previousResult)
        assertNull(nextResult)
    }

    @Test
    fun `adjustment rejects overlaps collapsed durations and midnight overflow`() {
        val overlappingEntries = listOf(
            createEntry(id = "selected", start = "09:00", end = "10:00"),
            createEntry(id = "next", start = "10:05", end = "11:00"),
        )
        val collapsingEntry = listOf(createEntry(start = "09:00", end = "09:05"))
        val midnightEntry = listOf(createEntry(start = "00:04", end = "01:00"))

        val overlapResult = adjustTimeEntries(
            entries = overlappingEntries,
            entryId = "selected",
            boundary = EditableTimeBoundary.End,
            direction = BookingAdjustmentDirection.LATER,
            adjustmentMinutes = 10,
        )
        val collapseResult = adjustTimeEntries(
            entries = collapsingEntry,
            entryId = "entry",
            boundary = EditableTimeBoundary.Start,
            direction = BookingAdjustmentDirection.LATER,
            adjustmentMinutes = 5,
        )
        val midnightResult = adjustTimeEntries(
            entries = midnightEntry,
            entryId = "entry",
            boundary = EditableTimeBoundary.Start,
            direction = BookingAdjustmentDirection.EARLIER,
            adjustmentMinutes = 5,
        )

        assertNull(overlapResult)
        assertNull(collapseResult)
        assertNull(midnightResult)
    }

    @Test
    fun `availability requires valid boundaries and a full-step remaining duration`() {
        val inputEntries = listOf(createEntry(start = "00:04", end = "00:10"))

        val actualAvailability = calculateTimeAdjustmentAvailability(
            entries = inputEntries,
            entryId = "entry",
            adjustmentMinutes = 5,
        )

        assertFalse(actualAvailability.canAdjustStartEarlier)
        assertFalse(actualAvailability.canAdjustStartLater)
        assertFalse(actualAvailability.canAdjustEndEarlier)
        assertTrue(actualAvailability.canAdjustEndLater)
    }

    @Test
    fun `availability is disabled when an entry time is blank`() {
        val inputEntries = listOf(createEntry(start = "09:00", end = ""))

        val actualAvailability = calculateTimeAdjustmentAvailability(
            entries = inputEntries,
            entryId = "entry",
            adjustmentMinutes = 5,
        )

        assertEquals(TimeAdjustmentAvailability(), actualAvailability)
    }

    private fun createEntry(
        id: String = "entry",
        start: String,
        end: String,
    ) = EditableTimeEntry(
        id = id,
        taskId = "task-$id",
        start = start,
        end = end,
    )
}
