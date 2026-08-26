package org.mdt.ui.navigation

import androidx.compose.runtime.Composable
import org.mdt.core.ui.compose.Modifier
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.compose.anchor
import org.mdt.core.ui.compose.fillMaxSize
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.layout.Box

/**
 * ## NavHost
 *
 * Declarative screen container observing [Navigator.currentRoute] and dynamically
 * rendering the destination via [ScreenRegistry].
 *
 * See: docs/architecture/architecture_en.md
 */
@Composable
fun NavHost(
    navigator: Navigator = LocalNavigator.current,
    modifier: UIModifier = UIModifier
) {
    Box(modifier = Modifier.anchor(LayoutPreset.FULL_RECT).fillMaxSize().then(modifier)) {
        ScreenRegistry.resolve(navigator.currentRoute)
    }
}
