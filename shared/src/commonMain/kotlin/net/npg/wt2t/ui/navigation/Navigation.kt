package net.npg.wt2t.ui.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Represents the different screens available in the application.
 */
sealed class Screen {
    /** Represents the booking navigation destination. */
    object Booking : Screen()
    /** Represents the end of day navigation destination. */
    object EndOfDay : Screen()
    /** Represents the project list navigation destination. */
    object ProjectList : Screen()
    /** Represents the task list navigation destination. */
    data class TaskList(val projectId: String?) : Screen()
    /** Represents the project edit navigation destination. */
    data class ProjectEdit(val projectId: String?) : Screen()
    /** Represents the task edit navigation destination. */
    data class TaskEdit(val taskId: String?, val projectId: String?) : Screen()
    /** Represents the config navigation destination. */
    object Config : Screen()
    /** Represents the help navigation destination. */
    object Help : Screen()
    /** Represents the impressum navigation destination. */
    object Impressum : Screen()
    /** Represents the library navigation destination. */
    object Library : Screen()
    /** Represents the daily overview navigation destination. */
    object DailyOverview : Screen()
    /** Represents the edit day navigation destination. */
    data class EditDay(val date: kotlinx.datetime.LocalDate) : Screen()
    /** Represents the report navigation destination. */
    object Report : Screen()
    /** Represents the menu navigation destination. */
    object Menu : Screen()
}

/**
 * Manages the navigation stack for the application UI.
 * 
 * @param initialScreen The screen to display initially.
 */
internal class NavigationEntry(
    val screen: Screen,
) : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()

    fun clear() {
        viewModelStore.clear()
    }
}

class NavigationStack(initialScreen: Screen) {
    private val stack = mutableStateListOf(NavigationEntry(initialScreen))

    internal val currentEntry: NavigationEntry
        get() = stack.last()

    val currentScreen: Screen
        get() = currentEntry.screen

    /** Whether the current destination has navigation history to return to. */
    internal val canNavigateBack: Boolean
        get() = stack.size > 1

    /** Navigates to the supplied destination. */
    fun navigateTo(screen: Screen) {
        stack.add(NavigationEntry(screen))
    }

    /** Removes the current destination when navigation history is available. */
    fun pop(): Boolean {
        if (canNavigateBack) {
            stack.removeAt(stack.size - 1).clear()
            return true
        }
        return false
    }

    /** Handles a Back request and reports whether the application consumed it. */
    internal fun handleBack(canLeaveCurrentDestination: Boolean = true): Boolean {
        if (!canNavigateBack) return false
        if (canLeaveCurrentDestination) pop()
        return true
    }

    /** Replaces the current navigation destination. */
    fun replace(screen: Screen) {
        val removedEntry = stack.last()
        stack[stack.lastIndex] = NavigationEntry(screen)
        removedEntry.clear()
    }

    /** Clears the navigation history and opens the supplied destination. */
    fun clearAndNavigate(screen: Screen) {
        val removedEntries = stack.toList()
        stack.clear()
        stack.add(NavigationEntry(screen))
        removedEntries.forEach(NavigationEntry::clear)
    }

    /** Clears every destination-scoped view model when the navigation host is disposed. */
    internal fun dispose() {
        val removedEntries = stack.toList()
        stack.clear()
        removedEntries.forEach(NavigationEntry::clear)
    }
}
