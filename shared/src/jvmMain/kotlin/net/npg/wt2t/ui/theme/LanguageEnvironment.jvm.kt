package net.npg.wt2t.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import java.util.Locale

/** Provides localized resources using the selected application language. */
@Composable
actual fun LanguageEnvironment(
    language: String,
    content: @Composable () -> Unit,
) {
    remember(language) {
        Locale.setDefault(Locale.forLanguageTag(language))
        language
    }
    key(language) {
        content()
    }
}
