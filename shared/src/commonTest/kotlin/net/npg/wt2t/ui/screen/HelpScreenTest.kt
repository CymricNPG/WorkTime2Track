package net.npg.wt2t.ui.screen

import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HelpScreenTest {
    @Test
    fun `GUI labels are bold while German instructions and translated labels stay intact`() {
        val inputText = "Wähle „Save“ oder „Discard“."

        val actualText = formatHelpText(inputText)

        assertEquals(inputText, actualText.text)
        assertEquals(listOf("„Save“", "„Discard“"), actualText.spanStyles.map {
            actualText.text.substring(it.start, it.end)
        })
        assertTrue(actualText.spanStyles.all { it.item.fontWeight == FontWeight.Bold })
    }

    @Test
    fun `plain instructions and incomplete quotation marks remain readable`() {
        val inputText = "Wähle einen Task. „Unvollständig"

        val actualText = formatHelpText(inputText)

        assertEquals(inputText, actualText.text)
        assertTrue(actualText.spanStyles.isEmpty())
    }
}
