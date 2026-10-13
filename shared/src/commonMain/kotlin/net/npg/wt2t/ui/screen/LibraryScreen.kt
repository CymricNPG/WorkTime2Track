package net.npg.wt2t.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.npg.wt2t.domain.service.SBOMComponent
import net.npg.wt2t.ui.component.PlatformHorizontalScrollbar
import net.npg.wt2t.ui.component.PlatformVerticalScrollbar
import net.npg.wt2t.ui.viewmodel.LibraryViewModel
import org.jetbrains.compose.resources.stringResource
import worktime2track.shared.generated.resources.*

private val nameColumnWidth = 260.dp
private val licenseColumnWidth = 180.dp
private val packageUrlColumnWidth = 520.dp
private val dependencyColumnWidth = 120.dp
private val tableWidth = 1128.dp

/**
 * Displays the third-party components contained in the application's CycloneDX SBOM.
 *
 * @param viewModel ViewModel that loads and exposes library component state.
 * @param onBack Callback to navigate back to the previous screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.library_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LibraryContent(
            components = uiState.components,
            hasLoadFailed = uiState.hasLoadFailed,
            modifier = Modifier.padding(padding),
        )
    }
}

/** Displays the library content UI element. */
@Composable
private fun LibraryContent(
    components: List<SBOMComponent>?,
    hasLoadFailed: Boolean,
    modifier: Modifier = Modifier,
) {
    if (hasLoadFailed) {
        Text(stringResource(Res.string.library_load_failed), modifier.padding(16.dp))
        return
    }
    if (components == null) {
        Text(stringResource(Res.string.library_loading), modifier.padding(16.dp))
        return
    }
    if (components.isEmpty()) {
        Text(stringResource(Res.string.library_empty), modifier.padding(16.dp))
        return
    }

    val horizontalScrollState = rememberScrollState()
    val lazyListState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 12.dp, bottom = 12.dp)
                .horizontalScroll(horizontalScrollState),
        ) {
            LibraryHeader()
            HorizontalDivider()
            LazyColumn(
                modifier = Modifier.weight(1f).width(tableWidth),
                state = lazyListState,
            ) {
                items(components, key = SBOMComponent::packageUrl) { component ->
                    LibraryRow(component)
                    HorizontalDivider()
                }
            }
        }

        PlatformVerticalScrollbar(
            listState = lazyListState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(bottom = 12.dp),
        )
        PlatformHorizontalScrollbar(
            scrollState = horizontalScrollState,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(end = 12.dp),
        )
    }
}

/** Displays the library header UI element. */
@Composable
private fun LibraryHeader() {
    Row(
        modifier = Modifier.width(tableWidth).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LibraryCell(stringResource(Res.string.library_column_name_version), nameColumnWidth, FontWeight.Bold)
        LibraryCell(stringResource(Res.string.library_column_license), licenseColumnWidth, FontWeight.Bold)
        LibraryCell(stringResource(Res.string.library_column_package_url), packageUrlColumnWidth, FontWeight.Bold)
        LibraryCell(stringResource(Res.string.library_column_dependency), dependencyColumnWidth, FontWeight.Bold)
    }
}

/** Displays the library row UI element. */
@Composable
private fun LibraryRow(component: SBOMComponent) {
    val dependency = if (component.isDirect) {
        stringResource(Res.string.library_direct)
    } else {
        stringResource(Res.string.library_transitive)
    }
    Row(
        modifier = Modifier.width(tableWidth).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LibraryCell("${component.name} ${component.version}", nameColumnWidth)
        LibraryCell(component.license, licenseColumnWidth)
        LibraryCell(component.packageUrl, packageUrlColumnWidth)
        LibraryCell(dependency, dependencyColumnWidth)
    }
}

/** Displays the library cell UI element. */
@Composable
private fun LibraryCell(
    value: String,
    width: androidx.compose.ui.unit.Dp,
    fontWeight: FontWeight? = null,
) {
    Text(
        text = value,
        modifier = Modifier.width(width),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = fontWeight,
    )
}
