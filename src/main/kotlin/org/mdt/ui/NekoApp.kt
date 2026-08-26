package org.mdt.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.mdt.ui.navigation.*
import org.mdt.ui.overlay.OverlayHost
import org.mdt.ui.screens.EditorScreen
import org.mdt.ui.screens.MainMenuScreen
import org.mdt.ui.theme.NekoTheme

/**
 * ## NekoApp
 *
 * Root Application Composable uniting the Apple iOS Theme, Context-Aware Overlay Layer,
 * and Open-Ended Data-Driven Route Navigation.
 *
 * Boots directly into [EditorScreen] for rapid live visual design and NXML prototyping.
 *
 * Mounted once at startup via [EngineRuntime.setContent].
 *
 * See: docs/architecture/architecture_en.md
 */
@Composable
fun NekoApp() {
    val navigator = remember {
        // Register default screens
        ScreenRegistry.register(Route.Editor.path) { EditorScreen() }
        ScreenRegistry.register(Route.MainMenu.path) { MainMenuScreen() }

        // Start directly in the NXML Visual Editor Workbench
        Navigator(Route.Editor)
    }

    NekoTheme {
        OverlayHost {
            ProvideNavigator(navigator) {
                NavHost(navigator)
            }
        }
    }
}
