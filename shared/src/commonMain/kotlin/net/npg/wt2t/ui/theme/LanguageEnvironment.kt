package net.npg.wt2t.ui.theme

import androidx.compose.runtime.Composable

/** Provides localized resources using the selected application language. */
@Composable
expect fun LanguageEnvironment(
    language: String,
    content: @Composable () -> Unit,
)
