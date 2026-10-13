package net.npg.wt2t.ui.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.npg.wt2t.data.model.AppSettings
import net.npg.wt2t.data.repository.ConfigRepository
import net.npg.wt2t.domain.usecase.ConfigurationUseCase
import net.npg.wt2t.ui.theme.LanguageState
import net.npg.wt2t.ui.theme.ThemePreviewSession
import net.npg.wt2t.ui.theme.ThemeSetting
import net.npg.wt2t.ui.theme.ThemeState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigViewModelTest {
    private lateinit var testDispatcher: TestDispatcher

    @BeforeTest
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `only theme changes preview before persistence`() = runTest(testDispatcher.scheduler) {
        val testContext = createTestContext()
        val viewModel = testContext.createViewModel()
        val themeState = ThemeState(ThemeSetting.LIGHT)
        val languageState = LanguageState("de")
        val previewSession = ThemePreviewSession(themeState)
        advanceUntilIdle()

        viewModel.updateThemeMode(ThemeMode.DARK)
        val preview = viewModel.effects.first() as ConfigEffect.PreviewTheme
        previewSession.preview(preview.themeMode)
        viewModel.updateLanguage("en")

        assertEquals(ThemeSetting.DARK, themeState.themeSetting)
        assertEquals("de", languageState.language)
        assertEquals("LIGHT", testContext.repository.value(AppSettings.THEME_MODE))
        assertEquals("de", testContext.repository.value(AppSettings.LANGUAGE))
    }

    @Test
    fun `successful save persists the draft and emits commit`() = runTest(testDispatcher.scheduler) {
        val testContext = createTestContext()
        val viewModel = testContext.createViewModel()
        advanceUntilIdle()
        viewModel.updateThemeMode(ThemeMode.DARK)
        viewModel.effects.first()
        viewModel.updateLanguage("en")

        viewModel.saveConfig()
        advanceUntilIdle()

        assertEquals(ConfigEffect.SaveSucceeded(ThemeMode.DARK, "en"), viewModel.effects.first())
        assertEquals("DARK", testContext.repository.value(AppSettings.THEME_MODE))
        assertEquals("en", testContext.repository.value(AppSettings.LANGUAGE))
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `booking start adjustment boundary values are persisted`() = runTest(testDispatcher.scheduler) {
        listOf("1", "30").forEach { inputMinutes ->
            val testContext = createTestContext()
            val viewModel = testContext.createViewModel()
            advanceUntilIdle()

            viewModel.updateBookingStartAdjustment(inputMinutes)
            viewModel.saveConfig()
            advanceUntilIdle()

            assertEquals(
                inputMinutes,
                testContext.repository.value(AppSettings.BOOKING_START_ADJUSTMENT_MINUTES),
            )
            assertEquals(true, viewModel.uiState.value.isBookingStartAdjustmentValid)
        }
    }

    @Test
    fun `invalid booking start adjustment is not persisted`() = runTest(testDispatcher.scheduler) {
        listOf("", "0", "31", "five").forEach { inputMinutes ->
            val testContext = createTestContext()
            val viewModel = testContext.createViewModel()
            advanceUntilIdle()

            viewModel.updateBookingStartAdjustment(inputMinutes)
            viewModel.saveConfig()
            advanceUntilIdle()

            assertEquals(
                "5",
                testContext.repository.value(AppSettings.BOOKING_START_ADJUSTMENT_MINUTES),
            )
            assertFalse(viewModel.uiState.value.isBookingStartAdjustmentValid)
            assertFalse(viewModel.uiState.value.isSaving)
        }
    }

    @Test
    fun `theme and language save failures restore application appearance and keep draft`() =
        runTest(testDispatcher.scheduler) {
            listOf(AppSettings.THEME_MODE, AppSettings.LANGUAGE).forEach { failingSetting ->
                val testContext = createTestContext()
                val viewModel = testContext.createViewModel()
                val themeState = ThemeState(ThemeSetting.LIGHT)
                val languageState = LanguageState("de")
                val previewSession = ThemePreviewSession(themeState)
                advanceUntilIdle()
                viewModel.updateThemeMode(ThemeMode.DARK)
                val themePreview = viewModel.effects.first() as ConfigEffect.PreviewTheme
                previewSession.preview(themePreview.themeMode)
                viewModel.updateLanguage("en")
                assertEquals(ThemeSetting.DARK, themeState.themeSetting)
                assertEquals("de", languageState.language)
                testContext.repository.failingSetting = failingSetting

                viewModel.saveConfig()
                advanceUntilIdle()

                assertEquals(ConfigEffect.RestoreTheme, viewModel.effects.first())
                previewSession.restore()
                assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
                assertEquals("en", viewModel.uiState.value.language)
                assertEquals(ThemeSetting.LIGHT, themeState.themeSetting)
                assertEquals("de", languageState.language)
                assertFalse(viewModel.uiState.value.isSaving)
                assertNotNull(viewModel.uiState.value.errorMessage)
                assertEquals("LIGHT", testContext.repository.value(AppSettings.THEME_MODE))
                assertEquals("de", testContext.repository.value(AppSettings.LANGUAGE))
            }
        }

    private fun createTestContext(): ConfigViewModelTestContext {
        val repository = FailingConfigRepository()
        return ConfigViewModelTestContext(
            repository = repository,
            configurationUseCase = ConfigurationUseCase(repository),
        )
    }
}

private data class ConfigViewModelTestContext(
    val repository: FailingConfigRepository,
    val configurationUseCase: ConfigurationUseCase,
) {
    fun createViewModel() = ConfigViewModel(
        configurationUseCase = configurationUseCase,
        initialLanguage = "de",
        initialThemeMode = ThemeMode.LIGHT,
    )
}

private class FailingConfigRepository : ConfigRepository {
    private val values = AppSettings.entries.associate { setting ->
        setting.name to when (setting) {
            AppSettings.THEME_MODE -> "LIGHT"
            else -> setting.defaultValue
        }
    }.toMutableMap()

    var failingSetting: AppSettings? = null

    fun value(setting: AppSettings): String? = values[setting.name]

    override suspend fun getString(key: String, defaultValue: String): String = values[key] ?: defaultValue

    override suspend fun setString(key: String, value: String) {
        values[key] = value
    }

    override suspend fun getInt(key: String, defaultValue: Int): Int = values[key]?.toIntOrNull() ?: defaultValue

    override suspend fun setInt(key: String, value: Int) {
        values[key] = value.toString()
    }

    override suspend fun setAll(values: Map<String, String>) {
        val updatedValues = this.values.toMutableMap()
        values.forEach { (key, value) ->
            check(key != failingSetting?.name) { "Expected configuration failure" }
            updatedValues[key] = value
        }
        this.values.putAll(updatedValues)
    }
}
