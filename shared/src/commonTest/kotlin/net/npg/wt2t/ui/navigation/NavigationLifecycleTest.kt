package net.npg.wt2t.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationLifecycleTest {
    @Test
    fun `returning after day deletion replaces the cached overview destination`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val inputTracker = ObserverTracker()
            val navigationStack = NavigationStack(Screen.Booking)
            navigationStack.navigateTo(Screen.DailyOverview)
            val cachedOverviewViewModel = createTrackingViewModel(navigationStack, inputTracker)
            navigationStack.navigateTo(Screen.EditDay(LocalDate.parse("2026-08-18")))

            navigationStack.pop()
            navigationStack.replace(Screen.DailyOverview)
            val refreshedOverviewViewModel = createTrackingViewModel(navigationStack, inputTracker)

            assertEquals(Screen.DailyOverview, navigationStack.currentScreen)
            assertNotSame(cachedOverviewViewModel, refreshedOverviewViewModel)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `removing destination cancels its view model collectors`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val inputTracker = ObserverTracker()
            val navigationStack = NavigationStack(Screen.Booking)
            navigationStack.navigateTo(Screen.ProjectList)
            createTrackingViewModel(navigationStack, inputTracker)
            advanceUntilIdle()
            assertEquals(1, inputTracker.activeObservers)

            navigationStack.pop()
            advanceUntilIdle()

            assertEquals(0, inputTracker.activeObservers)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `revisiting destination creates exactly one active observer`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val inputTracker = ObserverTracker()
            val navigationStack = NavigationStack(Screen.Booking)

            navigationStack.navigateTo(Screen.ProjectList)
            val firstViewModel = createTrackingViewModel(navigationStack, inputTracker)
            advanceUntilIdle()
            navigationStack.pop()
            advanceUntilIdle()

            navigationStack.navigateTo(Screen.ProjectList)
            val revisitedViewModel = createTrackingViewModel(navigationStack, inputTracker)
            advanceUntilIdle()

            assertEquals(2, inputTracker.createdObservers)
            assertEquals(1, inputTracker.activeObservers)
            assertEquals(1, inputTracker.maximumActiveObservers)
            assertNotSame(firstViewModel, revisitedViewModel)

            navigationStack.dispose()
            advanceUntilIdle()
            assertEquals(0, inputTracker.activeObservers)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createTrackingViewModel(
        navigationStack: NavigationStack,
        tracker: ObserverTracker,
    ): TrackingViewModel {
        val factory = viewModelFactory {
            initializer { TrackingViewModel(tracker) }
        }
        return ViewModelProvider.create(navigationStack.currentEntry, factory)[TrackingViewModel::class]
    }

    private class TrackingViewModel(tracker: ObserverTracker) : ViewModel() {
        init {
            tracker.track().launchIn(viewModelScope)
        }
    }

    private class ObserverTracker {
        var createdObservers = 0
            private set
        var activeObservers = 0
            private set
        var maximumActiveObservers = 0
            private set

        fun track(): Flow<Unit> = flow {
            createdObservers++
            activeObservers++
            maximumActiveObservers = maxOf(maximumActiveObservers, activeObservers)
            try {
                awaitCancellation()
            } finally {
                activeObservers--
            }
        }
    }
}
