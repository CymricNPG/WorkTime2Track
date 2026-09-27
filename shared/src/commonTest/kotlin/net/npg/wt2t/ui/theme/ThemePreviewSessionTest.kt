package net.npg.wt2t.ui.theme

import net.npg.wt2t.ui.viewmodel.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemePreviewSessionTest {
    @Test
    fun `restore reapplies theme captured when session opened`() {
        val themeState = ThemeState(ThemeSetting.LIGHT)
        val session = ThemePreviewSession(themeState)

        session.preview(ThemeMode.DARK)
        assertEquals(ThemeSetting.DARK, themeState.themeSetting)

        session.restore()
        assertEquals(ThemeSetting.LIGHT, themeState.themeSetting)
    }

    @Test
    fun `committed theme is not restored when destination is disposed`() {
        val themeState = ThemeState(ThemeSetting.LIGHT)
        val session = ThemePreviewSession(themeState)

        session.preview(ThemeMode.DARK)
        session.commit(ThemeMode.DARK)
        session.restore()

        assertEquals(ThemeSetting.DARK, themeState.themeSetting)
    }
}
