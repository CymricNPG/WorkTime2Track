package net.npg.wt2t

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import net.npg.wt2t.data.model.AppSettings
import net.npg.wt2t.domain.service.*
import net.npg.wt2t.domain.usecase.*
import net.npg.wt2t.ui.navigation.NavigationStack
import net.npg.wt2t.ui.navigation.PlatformBackHandler
import net.npg.wt2t.ui.navigation.Screen
import net.npg.wt2t.ui.screen.*
import net.npg.wt2t.ui.theme.*
import net.npg.wt2t.ui.viewmodel.*
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.stringResource
import worktime2track.shared.generated.resources.Res
import worktime2track.shared.generated.resources.app_name
import worktime2track.shared.generated.resources.report_screen
import kotlin.time.Clock

/**
 * The main UI entry point of the application.
 * It provides compositional logic to navigate between screens based on the current state
 * and injects required services using Koin.
 */
@Composable
fun App(
    onExportPdf: ((PdfExportRequest) -> Unit)? = null,
    onExportDatabase: ((DatabaseExportRequest) -> Unit)? = null,
    onImportDatabase: ((DatabaseImportRequest) -> Unit)? = null,
    onDatabaseRecreated: () -> Unit,
) {
    val themeState = remember { ThemeState() }
    val languageState = remember { LanguageState() }
    val navigationStack = remember { NavigationStack(Screen.Booking) }
    val configurationUseCase = koinInject<ConfigurationUseCase>()

    DisposableEffect(navigationStack) {
        onDispose(navigationStack::dispose)
    }

    LaunchedEffect(Unit) {
        val theme = configurationUseCase.getString(AppSettings.THEME_MODE)
        themeState.themeSetting = ThemeMode.parseOrDefault(theme).toThemeSetting()

        val lang = configurationUseCase.getString(AppSettings.LANGUAGE)
        languageState.language = lang
    }

    CompositionLocalProvider(
        LocalThemeState provides themeState,
        LocalLanguageState provides languageState
    ) {
        AppTheme {
            AppContent(
                navigationStack,
                onExportPdf,
                onExportDatabase,
                onImportDatabase,
                onDatabaseRecreated,
            )
        }
    }
}

/** Returns the localized application name. */
@Composable
fun applicationName(): String = stringResource(Res.string.app_name)

/** Displays the content for the current navigation destination. */
@Composable
fun AppContent(
    navigationStack: NavigationStack,
    onExportPdf: ((PdfExportRequest) -> Unit)? = null,
    onExportDatabase: ((DatabaseExportRequest) -> Unit)? = null,
    onImportDatabase: ((DatabaseImportRequest) -> Unit)? = null,
    onDatabaseRecreated: () -> Unit,
) {
    val bookingUseCase = koinInject<BookingUseCase>()
    val projectManagementUseCase = koinInject<ProjectManagementUseCase>()
    val endOfDayUseCase = koinInject<EndOfDayUseCase>()
    val dayEditingUseCase = koinInject<DayEditingUseCase>()
    val configurationUseCase = koinInject<ConfigurationUseCase>()
    val backupUseCase = koinInject<BackupUseCase>()
    val reportingUseCase = koinInject<ReportingUseCase>()
    val sbomService = koinInject<SBOMService>()
    val clock = koinInject<Clock>()
    var shouldRefreshDailyOverview by remember { mutableStateOf(false) }

    val currentEntry = navigationStack.currentEntry
    val currentScreen = currentEntry.screen
    val onBack: () -> Unit = { navigationStack.handleBack() }

    CompositionLocalProvider(LocalViewModelStoreOwner provides currentEntry) {
        if (
            currentScreen !is Screen.EndOfDay &&
            currentScreen !is Screen.EditDay &&
            currentScreen !is Screen.Config
        ) {
            NavigationBackHandler(navigationStack)
        }
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (currentScreen) {
                is Screen.Booking -> {
                    val viewModel = viewModel { BookingViewModel(bookingUseCase, reportingUseCase, clock) }
                    BookingScreen(
                        viewModel = viewModel,
                        onNavigateToOverview = { navigationStack.navigateTo(Screen.DailyOverview) },
                        onNavigateToProjects = { navigationStack.navigateTo(Screen.ProjectList) },
                        onNavigateToEndOfDay = { navigationStack.navigateTo(Screen.EndOfDay) },
                        onNavigateToMenu = { navigationStack.navigateTo(Screen.Menu) },
                    )
                }

                is Screen.Menu -> {
                    val currentExportDatabase = rememberUpdatedState(onExportDatabase)
                    val currentImportDatabase = rememberUpdatedState(onImportDatabase)
                    val canExportDatabase = onExportDatabase != null
                    val canImportDatabase = onImportDatabase != null
                    val viewModel = viewModel {
                        MenuViewModel(
                            backupUseCase = backupUseCase,
                            exportDatabaseFile = if (canExportDatabase) {
                                { request -> currentExportDatabase.value?.invoke(request) }
                            } else {
                                null
                            },
                            importDatabaseFile = if (canImportDatabase) {
                                { request -> currentImportDatabase.value?.invoke(request) }
                            } else {
                                null
                            },
                        )
                    }
                    MenuScreen(
                        viewModel = viewModel,
                        onNavigateToConfig = { navigationStack.navigateTo(Screen.Config) },
                        onNavigateToHelp = { navigationStack.navigateTo(Screen.Help) },
                        onNavigateToImpressum = { navigationStack.navigateTo(Screen.Impressum) },
                        onNavigateToLibrary = { navigationStack.navigateTo(Screen.Library) },
                        onDatabaseRecreated = onDatabaseRecreated,
                        onBack = onBack,
                    )
                }

                is Screen.DailyOverview -> {
                    val viewModel = viewModel {
                        DailyOverviewViewModel(
                            reportingUseCase,
                            dayEditingUseCase,
                            clock,
                        )
                    }
                    LaunchedEffect(shouldRefreshDailyOverview) {
                        if (shouldRefreshDailyOverview) {
                            viewModel.refresh()
                            shouldRefreshDailyOverview = false
                        }
                    }
                    DailyOverviewScreen(
                        viewModel = viewModel,
                        onEditDay = { navigationStack.navigateTo(Screen.EditDay(it)) },
                        onNavigateToReport = { reportText ->
                            if (onExportPdf == null) {
                                viewModel.exportToPdf(reportText)
                            } else {
                                onExportPdf(viewModel.createPdfExportRequest(reportText))
                            }
                        },
                        onBack = onBack,
                    )
                }

                is Screen.EditDay -> {
                    val viewModel = viewModel {
                        EditDayViewModel(
                            currentScreen.date,
                            dayEditingUseCase,
                        )
                    }
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    LaunchedEffect(uiState.isSaved) {
                        if (uiState.isSaved) shouldRefreshDailyOverview = true
                    }
                    NavigationBackHandler(
                        navigationStack = navigationStack,
                        canLeaveCurrentDestination = !uiState.isSaving && !uiState.isDeleting,
                    )
                    EditDayScreen(
                        viewModel = viewModel,
                        onDeleted = {
                            shouldRefreshDailyOverview = false
                            navigationStack.pop()
                            navigationStack.replace(Screen.DailyOverview)
                        },
                        onBack = onBack,
                    )
                }

                is Screen.EndOfDay -> {
                    val viewModel = viewModel { EndOfDayViewModel(endOfDayUseCase, clock) }
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    NavigationBackHandler(
                        navigationStack = navigationStack,
                        canLeaveCurrentDestination = !uiState.isSaving,
                    )
                    EndOfDayScreen(viewModel = viewModel, onBack = onBack)
                }

                is Screen.ProjectList -> {
                    val viewModel = viewModel { ProjectListViewModel(projectManagementUseCase) }
                    ProjectListScreen(
                        viewModel = viewModel,
                        onNavigateToTasks = { navigationStack.navigateTo(Screen.TaskList(it)) },
                        onEditProject = { navigationStack.navigateTo(Screen.ProjectEdit(it)) },
                        onBack = onBack,
                    )
                }

                is Screen.TaskList -> {
                    val viewModel = viewModel {
                        TaskListViewModel(currentScreen.projectId, projectManagementUseCase)
                    }
                    TaskListScreen(
                        viewModel = viewModel,
                        onEditTask = { taskId, projectId -> navigationStack.navigateTo(Screen.TaskEdit(taskId, projectId)) },
                        onBack = onBack,
                    )
                }

                is Screen.ProjectEdit -> {
                    val viewModel = viewModel { ProjectViewModel(currentScreen.projectId, projectManagementUseCase) }
                    ProjectEditScreen(viewModel = viewModel, onBack = onBack)
                }

                is Screen.TaskEdit -> {
                    val viewModel = viewModel {
                        TaskViewModel(currentScreen.taskId, currentScreen.projectId, projectManagementUseCase)
                    }
                    TaskEditScreen(viewModel = viewModel, onBack = onBack)
                }

                is Screen.Config -> {
                    val themeState = LocalThemeState.current
                    val languageState = LocalLanguageState.current
                    val viewModel = viewModel {
                        ConfigViewModel(
                            configurationUseCase = configurationUseCase,
                            initialLanguage = languageState.language,
                            initialThemeMode = themeState.themeSetting.toThemeMode(),
                        )
                    }
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val previewSession = remember(currentEntry) {
                        ThemePreviewSession(themeState)
                    }
                    val discardAndBack: () -> Unit = {
                        if (!uiState.isSaving) {
                            discardConfigurationDraft(previewSession, navigationStack)
                        }
                    }

                    DisposableEffect(previewSession) {
                        onDispose(previewSession::restore)
                    }
                    LaunchedEffect(viewModel, previewSession) {
                        viewModel.effects.collect { effect ->
                            when (effect) {
                                is ConfigEffect.PreviewTheme -> {
                                    previewSession.preview(effect.themeMode)
                                }

                                is ConfigEffect.SaveSucceeded -> {
                                    previewSession.commit(effect.themeMode)
                                    languageState.language = effect.language
                                    navigationStack.handleBack()
                                }

                                ConfigEffect.RestoreTheme -> previewSession.restore()
                            }
                        }
                    }
                    NavigationBackHandler(
                        navigationStack = navigationStack,
                        canLeaveCurrentDestination = !uiState.isSaving,
                        onBack = discardAndBack,
                    )
                    ConfigScreen(viewModel = viewModel, onBack = discardAndBack)
                }

                is Screen.Help -> HelpScreen(onBack = onBack)
                is Screen.Impressum -> ImpressumScreen(onBack = onBack)
                is Screen.Library -> {
                    val viewModel = viewModel { LibraryViewModel(sbomService) }
                    LibraryScreen(viewModel = viewModel, onBack = onBack)
                }
                is Screen.Report -> Text(stringResource(Res.string.report_screen))
            }
        }
    }
}

/** Restores an unsaved configuration preview before leaving its destination. */
internal fun discardConfigurationDraft(
    previewSession: ThemePreviewSession,
    navigationStack: NavigationStack,
): Boolean {
    previewSession.restore()
    return navigationStack.handleBack()
}

@Composable
private fun NavigationBackHandler(
    navigationStack: NavigationStack,
    canLeaveCurrentDestination: Boolean = true,
    onBack: () -> Unit = { navigationStack.handleBack() },
) {
    PlatformBackHandler(enabled = navigationStack.canNavigateBack) {
        if (canLeaveCurrentDestination) onBack()
    }
}
