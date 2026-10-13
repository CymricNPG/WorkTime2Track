package net.npg.wt2t.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.npg.wt2t.ui.component.PlatformVerticalScrollbar
import org.jetbrains.compose.resources.stringResource
import worktime2track.shared.generated.resources.Res
import worktime2track.shared.generated.resources.back
import worktime2track.shared.generated.resources.help_collapsed
import worktime2track.shared.generated.resources.help_expanded
import worktime2track.shared.generated.resources.help_quickstart_intro
import worktime2track.shared.generated.resources.help_title

/** Displays the compact guide with one expandable topic open at a time. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    var expandedTopic by rememberSaveable { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val topics = createHelpTopics()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.help_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                contentPadding = PaddingValues(16.dp),
            ) {
                item(key = "intro") {
                    Text(
                        stringResource(Res.string.help_quickstart_intro),
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
                topics.forEachIndexed { index, topic ->
                    item(key = "heading-$index") {
                        HelpTopicHeader(topic.title, expandedTopic == index) {
                            expandedTopic = if (expandedTopic == index) -1 else index
                        }
                    }
                    if (expandedTopic == index) {
                        topic.steps.forEachIndexed { stepIndex, step ->
                            item(key = "step-$index-$stepIndex") {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 8.dp),
                                ) {
                                    Text("${stepIndex + 1}.")
                                    Text(formatHelpText(step), modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        item(key = "hint-$index") {
                            Text(
                                formatHelpText(topic.hint),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                            )
                        }
                    }
                }
            }
            PlatformVerticalScrollbar(
                listState,
                Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun HelpTopicHeader(title: String, expanded: Boolean, onClick: () -> Unit) {
    val state = stringResource(if (expanded) Res.string.help_expanded else Res.string.help_collapsed)
    Column {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics {
                    heading()
                    stateDescription = state
                }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
        }
    }
}

/** Emphasizes GUI labels enclosed in German quotation marks, preserving the text. */
internal fun formatHelpText(text: String): AnnotatedString = buildAnnotatedString {
    append(text)
    Regex("„[^“]+“").findAll(text).forEach { match ->
        addStyle(SpanStyle(fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
    }
}
