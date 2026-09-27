package net.npg.wt2t.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.npg.wt2t.AppBuildInfo
import org.jetbrains.compose.resources.stringResource
import worktime2track.shared.generated.resources.*

/**
 * Screen for legal information and imprint.
 * 
 * @param onBack Callback to navigate back to the previous screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpressumScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.impressum_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text(stringResource(Res.string.legal_app_name), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.version, AppBuildInfo.VERSION))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(Res.string.developed_by))
        }
    }
}
