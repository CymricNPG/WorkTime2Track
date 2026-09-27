package net.npg.wt2t.ui.navigation

import androidx.compose.runtime.Composable

/** Desktop navigation continues to use its visible Back actions. */
@Composable
internal actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) = Unit
