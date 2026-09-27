package net.npg.wt2t.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Displays the platform-specific horizontal scrollbar. */
@Composable
expect fun PlatformHorizontalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
)
