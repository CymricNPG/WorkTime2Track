package net.npg.wt2t.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.npg.wt2t.ui.viewmodel.ConfigViewModel
import net.npg.wt2t.ui.viewmodel.ThemeMode
import org.jetbrains.compose.resources.stringResource
import worktime2track.shared.generated.resources.*

private val languages = listOf("de", "en")

/**
 * Screen for application configuration. Allows users to set default target work times 
 * and the merge threshold for booking times.
 * 
 * @param viewModel The ViewModel for configuration state and logic.
 * @param onBack Callback to navigate back to the previous screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    viewModel: ConfigViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val areControlsEnabled = !uiState.isLoading && !uiState.isSaving

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.config_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !uiState.isSaving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(stringResource(Res.string.default_target_time), style = MaterialTheme.typography.titleMedium)
            Row {
                TextField(
                    value = uiState.defaultTargetHours.toString(),
                    onValueChange = { val h = it.toIntOrNull() ?: 0; viewModel.updateDefaultTargetTime(h, uiState.defaultTargetMinutes) },
                    label = { Text(stringResource(Res.string.hours)) },
                    modifier = Modifier.weight(1f),
                    enabled = areControlsEnabled,
                )
                Spacer(Modifier.width(8.dp))
                TextField(
                    value = uiState.defaultTargetMinutes.toString(),
                    onValueChange = { val m = it.toIntOrNull() ?: 0; viewModel.updateDefaultTargetTime(uiState.defaultTargetHours, m) },
                    label = { Text(stringResource(Res.string.minutes)) },
                    modifier = Modifier.weight(1f),
                    enabled = areControlsEnabled,
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(stringResource(Res.string.merge_threshold), style = MaterialTheme.typography.titleMedium)
            TextField(
                value = uiState.mergeThresholdMinutes.toString(),
                onValueChange = { val m = it.toIntOrNull() ?: 5; viewModel.updateMergeThreshold(m) },
                label = { Text(stringResource(Res.string.minutes)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = areControlsEnabled,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(Res.string.booking_start_adjustment),
                style = MaterialTheme.typography.titleMedium,
            )
            TextField(
                value = uiState.bookingStartAdjustmentMinutes,
                onValueChange = viewModel::updateBookingStartAdjustment,
                label = { Text(stringResource(Res.string.minutes)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = areControlsEnabled,
                isError = !uiState.isBookingStartAdjustmentValid,
                supportingText = if (!uiState.isBookingStartAdjustmentValid) {
                    { Text(stringResource(Res.string.booking_start_adjustment_invalid)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )

            Spacer(Modifier.height(16.dp))

            Text(stringResource(Res.string.theme), style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = uiState.themeMode == mode,
                        onClick = { viewModel.updateThemeMode(mode) },
                        enabled = areControlsEnabled,
                        label = {
                            Text(
                                stringResource(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> Res.string.theme_system
                                        ThemeMode.LIGHT -> Res.string.theme_light
                                        ThemeMode.DARK -> Res.string.theme_dark
                                    }
                                )
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(stringResource(Res.string.language), style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                languages.forEach { language ->
                    FilterChip(
                        selected = uiState.language == language,
                        onClick = { viewModel.updateLanguage(language) },
                        enabled = areControlsEnabled,
                        label = {
                            Text(
                                stringResource(
                                    when (language) {
                                        "de" -> Res.string.language_german
                                        else -> Res.string.language_english
                                    }
                                )
                            )
                        }
                    )
                }
            }

            uiState.errorMessage?.let { message ->
                Spacer(Modifier.height(16.dp))
                Text(stringResource(message), color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.saveConfig() },
                modifier = Modifier.fillMaxWidth(),
                enabled = areControlsEnabled && uiState.isBookingStartAdjustmentValid,
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(Res.string.save))
            }
        }
    }
}
