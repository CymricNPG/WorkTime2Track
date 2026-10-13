package net.npg.wt2t.ui.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookingScreenTest {

    @Test
    fun `last note is displayed without line breaks and limited to 40 characters`() {
        val inputNotes = listOf(
            "Older note",
            "Latest note with a line break\r\nand more than forty characters in total",
        )

        val actualNote = formatLastNote(inputNotes)

        assertEquals("Latest note with a line break and more t", actualNote)
    }

    @Test
    fun `no note is displayed when no note exists`() {
        val actualNote = formatLastNote(emptyList())

        assertNull(actualNote)
    }
}
