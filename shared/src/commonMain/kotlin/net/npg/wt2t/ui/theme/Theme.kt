package net.npg.wt2t.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import net.npg.wt2t.ui.viewmodel.ThemeMode

/** Defines the theme choices available to the UI. */
enum class ThemeSetting {
    SYSTEM, LIGHT, DARK
}

/** Maps this persisted theme mode to its UI theme setting. */
fun ThemeMode.toThemeSetting(): ThemeSetting = when (this) {
    ThemeMode.SYSTEM -> ThemeSetting.SYSTEM
    ThemeMode.LIGHT -> ThemeSetting.LIGHT
    ThemeMode.DARK -> ThemeSetting.DARK
}

/** Maps the applied UI theme to its persisted theme mode. */
fun ThemeSetting.toThemeMode(): ThemeMode = when (this) {
    ThemeSetting.SYSTEM -> ThemeMode.SYSTEM
    ThemeSetting.LIGHT -> ThemeMode.LIGHT
    ThemeSetting.DARK -> ThemeMode.DARK
}

/** Holds the currently selected application theme. */
class ThemeState(initialTheme: ThemeSetting = ThemeSetting.SYSTEM) {
    var themeSetting by mutableStateOf(initialTheme)
}

val LocalThemeState = staticCompositionLocalOf { ThemeState() }

/** Holds the currently selected application language. */
class LanguageState(initialLanguage: String = "de") {
    var language by mutableStateOf(initialLanguage)
}

val LocalLanguageState = staticCompositionLocalOf { LanguageState() }

/** Coordinates immediate theme previews and rollback for one configuration destination. */
internal class ThemePreviewSession(
    private val themeState: ThemeState,
) {
    private val originalTheme = themeState.themeSetting
    private var isCommitted = false

    fun preview(themeMode: ThemeMode) {
        if (isCommitted) return
        themeState.themeSetting = themeMode.toThemeSetting()
    }

    fun restore() {
        if (isCommitted) return
        themeState.themeSetting = originalTheme
    }

    fun commit(themeMode: ThemeMode) {
        preview(themeMode)
        isCommitted = true
    }
}

private val DarkColorScheme = darkColorScheme()
private val LightColorScheme = lightColorScheme()

/**
 * Main application theme wrapper.
 * 
 * @param themeState The state of the theme.
 * @param content The UI content to be wrapped by the theme.
 */
@Composable
fun AppTheme(
    themeState: ThemeState = LocalThemeState.current,
    languageState: LanguageState = LocalLanguageState.current,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeState.themeSetting) {
        ThemeSetting.SYSTEM -> isSystemInDarkTheme()
        ThemeSetting.LIGHT -> false
        ThemeSetting.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    LanguageEnvironment(languageState.language) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
