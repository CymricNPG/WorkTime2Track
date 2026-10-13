package net.npg.wt2t.ui.navigation

import net.npg.wt2t.discardConfigurationDraft
import net.npg.wt2t.ui.theme.LanguageState
import net.npg.wt2t.ui.theme.ThemePreviewSession
import net.npg.wt2t.ui.theme.ThemeSetting
import net.npg.wt2t.ui.theme.ThemeState
import net.npg.wt2t.ui.viewmodel.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigurationNavigationTest {
    @Test
    fun `toolbar back restores appearance preview before navigating`() {
        val navigationStack = NavigationStack(Screen.Booking)
        navigationStack.navigateTo(Screen.Config)
        val themeState = ThemeState(ThemeSetting.LIGHT)
        val languageState = LanguageState("de")
        val previewSession = ThemePreviewSession(themeState)
        previewSession.preview(ThemeMode.DARK)

        assertTrue(discardConfigurationDraft(previewSession, navigationStack))

        assertEquals(ThemeSetting.LIGHT, themeState.themeSetting)
        assertEquals("de", languageState.language)
        assertEquals(Screen.Booking, navigationStack.currentScreen)
    }
}
