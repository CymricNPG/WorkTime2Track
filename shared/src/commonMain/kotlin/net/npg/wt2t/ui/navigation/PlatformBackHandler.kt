package net.npg.wt2t.ui.navigation

import androidx.compose.runtime.Composable

/** Intercepts the platform Back action while [enabled]. */
@Composable
internal expect fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
)
