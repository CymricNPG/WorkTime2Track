package net.npg.wt2t.ui.component

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Displays the platform-specific horizontal scrollbar. */
@Composable
actual fun PlatformHorizontalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier,
) {
    HorizontalScrollbar(
        modifier = modifier,
        adapter = rememberScrollbarAdapter(scrollState),
        style = createPlatformScrollbarStyle(),
    )
}
