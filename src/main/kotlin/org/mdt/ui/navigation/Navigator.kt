package org.mdt.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * ## Navigator
 *
 * Universal data-driven state machine managing the application's route backstack.
 * Supports Kotlin composables, dynamic NXML text schemas, and route parameters.
 *
 * See: docs/architecture/architecture_en.md
 */
class Navigator(
    initialRoute: Route = Route.MainMenu
) {
    /** Reactive route backstack. */
    val backstack = mutableStateListOf<Route>(initialRoute)

    /** Currently active visible route at the top of the stack. */
    val currentRoute: Route
        get() = backstack.lastOrNull() ?: Route.MainMenu

    /** Whether the navigator has previous routes to pop back to. */
    val canPop: Boolean
        get() = backstack.size > 1

    /**
     * Navigates to a new destination by path and optional parameters.
     */
    fun navigate(path: String, params: Map<String, Any> = emptyMap()) {
        push(Route(path, params))
    }

    /**
     * Navigates to a dynamic NXML markup schema.
     */
    fun navigateNxml(nxmlPath: String, params: Map<String, Any> = emptyMap()) {
        push(Route.nxml(nxmlPath, *params.toList().toTypedArray()))
    }

    /**
     * Pushes a new [route] onto the navigation stack.
     */
    fun push(route: Route) {
        if (currentRoute != route) {
            backstack.add(route)
        }
    }

    /**
     * Pops the current route off the stack, returning to the previous destination.
     *
     * @return `true` if a route was popped, `false` if already at the root route.
     */
    fun pop(): Boolean {
        if (canPop) {
            backstack.removeAt(backstack.size - 1)
            return true
        }
        return false
    }

    /**
     * Replaces the current route with [route] without growing the stack depth.
     */
    fun replace(route: Route) {
        if (backstack.isNotEmpty()) {
            backstack[backstack.size - 1] = route
        } else {
            backstack.add(route)
        }
    }

    /**
     * Pops all routes until only the root route remains.
     */
    fun popToRoot() {
        while (backstack.size > 1) {
            backstack.removeAt(backstack.size - 1)
        }
    }

    /**
     * Clears the entire stack and resets to [route].
     */
    fun reset(route: Route) {
        backstack.clear()
        backstack.add(route)
    }
}

/**
 * CompositionLocal providing access to the ambient [Navigator] anywhere in the UI tree.
 */
val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("No LocalNavigator provided in the active composition tree.")
}

/**
 * Convenience helper to provide an ambient [Navigator].
 */
@Composable
fun ProvideNavigator(
    navigator: Navigator = remember { Navigator(Route.MainMenu) },
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalNavigator provides navigator) {
        content()
    }
}
