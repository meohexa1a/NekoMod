package org.mdt.ui.navigation

import androidx.compose.runtime.Composable
import org.mdt.ui.components.text.Text

/**
 * ## ScreenRegistry
 *
 * Dynamic, open-ended screen registry and factory mapping [Route] paths to declarative composables.
 * Eliminates rigid compile-time `when (screen)` branching, enabling runtime plugin registration,
 * dynamic NXML schema loading, and live hot-reloading.
 *
 * See: docs/architecture/architecture_en.md
 */
object ScreenRegistry {

    private val screens = HashMap<String, @Composable (Route) -> Unit>()

    /**
     * Fallback dynamic route resolver for unmapped routes (e.g. NXML schemas, file routes).
     */
    var dynamicResolver: (@Composable (Route) -> Unit)? = null

    /**
     * Registers a screen composable handler for the specified [path].
     *
     * @param path Unique route path identifier (e.g. `"main_menu"`, `"settings"`).
     * @param content Composable renderer receiving the active [Route].
     */
    fun register(path: String, content: @Composable (Route) -> Unit) {
        screens[path] = content
    }

    /**
     * Unregisters the screen handler associated with [path].
     */
    fun unregister(path: String) {
        screens.remove(path)
    }

    /**
     * Resolves and renders the composable associated with [route].
     */
    @Composable
    fun resolve(route: Route) {
        val handler = screens[route.path]
        if (handler != null) {
            handler(route)
            return
        }

        // Delegate to dynamic NXML/Schema resolver if available
        val dynamic = dynamicResolver
        if (dynamic != null) {
            dynamic(route)
            return
        }

        // Fallback placeholder for unregistered routes
        Text(text = "Unregistered Screen Route: ${route.path}")
    }

    /**
     * Clears all registered screen handlers.
     */
    fun clear() {
        screens.clear()
        dynamicResolver = null
    }
}
