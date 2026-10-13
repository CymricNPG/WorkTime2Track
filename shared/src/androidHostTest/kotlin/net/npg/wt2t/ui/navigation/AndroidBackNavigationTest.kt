package net.npg.wt2t.ui.navigation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import net.npg.wt2t.discardConfigurationDraft
import net.npg.wt2t.domain.service.MockProjectRepository
import net.npg.wt2t.domain.service.MockTaskRepository
import net.npg.wt2t.domain.service.MockTimeRepository
import net.npg.wt2t.domain.usecase.ProjectManagementUseCase
import net.npg.wt2t.ui.theme.LanguageState
import net.npg.wt2t.ui.theme.ThemePreviewSession
import net.npg.wt2t.ui.theme.ThemeSetting
import net.npg.wt2t.ui.theme.ThemeState
import net.npg.wt2t.ui.viewmodel.ProjectViewModel
import net.npg.wt2t.ui.viewmodel.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidBackNavigationTest {
    @Test
    fun `back on nested destination pops one shared destination`() {
        val navigationStack = NavigationStack(Screen.Booking)
        navigationStack.navigateTo(Screen.Menu)
        navigationStack.navigateTo(Screen.Config)

        assertTrue(navigationStack.canNavigateBack)
        assertTrue(navigationStack.handleBack())

        assertEquals(Screen.Menu, navigationStack.currentScreen)
        assertTrue(navigationStack.canNavigateBack)
    }

    @Test
    fun `back at root is not consumed`() {
        val navigationStack = NavigationStack(Screen.Booking)

        assertFalse(navigationStack.canNavigateBack)
        assertFalse(navigationStack.handleBack())

        assertEquals(Screen.Booking, navigationStack.currentScreen)
    }

    @Test
    fun `back is consumed without popping when current destination blocks leaving`() {
        val navigationStack = NavigationStack(Screen.Booking)
        navigationStack.navigateTo(Screen.EndOfDay)

        assertTrue(navigationStack.handleBack(canLeaveCurrentDestination = false))

        assertEquals(Screen.EndOfDay, navigationStack.currentScreen)
        assertTrue(navigationStack.canNavigateBack)
    }

    @Test
    fun `system back restores unsaved configuration appearance`() {
        val navigationStack = NavigationStack(Screen.Booking)
        navigationStack.navigateTo(Screen.Config)
        val themeState = ThemeState(ThemeSetting.LIGHT)
        val languageState = LanguageState("de")
        val previewSession = ThemePreviewSession(themeState)
        previewSession.preview(ThemeMode.DARK)

        assertTrue(discardConfigurationDraft(previewSession, navigationStack))

        assertEquals(ThemeSetting.LIGHT, themeState.themeSetting)
        assertEquals("de", languageState.language)
        assertEquals(Screen.Booking, navigationStack.currentScreen)
    }

    @Test
    fun `back discards unsaved project draft`() {
        val projectRepository = MockProjectRepository()
        val projectManagementUseCase = createProjectManagementUseCase(projectRepository)
        val navigationStack = NavigationStack(Screen.Booking)
        navigationStack.navigateTo(Screen.ProjectEdit(projectId = null))
        val draftViewModel = createProjectViewModel(navigationStack, projectManagementUseCase)
        draftViewModel.updateName("Unsaved project")
        assertEquals("Unsaved project", draftViewModel.uiState.value.name)

        assertTrue(navigationStack.handleBack())

        assertTrue(projectRepository.projects.value.isEmpty())
        navigationStack.navigateTo(Screen.ProjectEdit(projectId = null))
        val reopenedViewModel = createProjectViewModel(navigationStack, projectManagementUseCase)
        assertEquals("", reopenedViewModel.uiState.value.name)
    }

    private fun createProjectManagementUseCase(projectRepository: MockProjectRepository): ProjectManagementUseCase {
        val taskRepository = MockTaskRepository()
        val timeRepository = MockTimeRepository()
        return ProjectManagementUseCase(projectRepository, taskRepository, timeRepository)
    }

    private fun createProjectViewModel(
        navigationStack: NavigationStack,
        projectManagementUseCase: ProjectManagementUseCase,
    ): ProjectViewModel {
        val factory = viewModelFactory {
            initializer { ProjectViewModel(projectId = null, projectManagementUseCase = projectManagementUseCase) }
        }
        return ViewModelProvider.create(navigationStack.currentEntry, factory)[ProjectViewModel::class]
    }
}
