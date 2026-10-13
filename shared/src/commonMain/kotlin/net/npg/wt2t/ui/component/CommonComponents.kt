package net.npg.wt2t.ui.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A standard card component with elevation and padding.
 * 
 * @param modifier Modifier to apply to the card.
 * @param content The content of the card.
 */
@Composable
fun StandardCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = content
    )
}

/**
 * A checkbox with a label positioned to its right.
 * 
 * @param label The text label for the checkbox.
 * @param checked The current checked state.
 * @param onCheckedChange Callback for checkbox state changes.
 * @param modifier Modifier to apply to the row container.
 */
@Composable
fun LabeledCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}