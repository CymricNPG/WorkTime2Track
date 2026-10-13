package net.npg.wt2t.ui.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Displays the platform-specific vertical scrollbar. */
@Composable
expect fun PlatformVerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
)
