package net.npg.wt2t.ui.theme

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
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

    val currentConfiguration = LocalConfiguration.current
    val localizedConfiguration = remember(currentConfiguration, language) {
        Configuration(currentConfiguration).apply {
            setLocales(LocaleList.forLanguageTags(language))
        }
    }

    CompositionLocalProvider(LocalConfiguration provides localizedConfiguration) {
        key(language) {
            content()
        }
    }
}
