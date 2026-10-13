package net.npg.wt2t.ui.component

import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

private const val SCROLLBAR_IDLE_ALPHA = 0.5f
private const val SCROLLBAR_HOVER_ALPHA = 0.8f

/** Creates the scrollbar style used by the desktop application. */
@Composable
internal fun createPlatformScrollbarStyle(): ScrollbarStyle = defaultScrollbarStyle().copy(
    unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = SCROLLBAR_IDLE_ALPHA),
    hoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = SCROLLBAR_HOVER_ALPHA),
)
