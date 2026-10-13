package net.npg.wt2t.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/** Intercepts Android Back through the lifecycle-aware activity dispatcher. */
@Composable
internal actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    BackHandler(enabled = enabled, onBack = onBack)
}
