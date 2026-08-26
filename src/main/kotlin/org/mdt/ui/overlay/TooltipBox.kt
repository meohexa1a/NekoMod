package org.mdt.ui.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import org.mdt.core.ui.compose.BoxScope
import org.mdt.core.ui.compose.Modifier
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.compose.onHover
import org.mdt.ui.components.layout.Box

/**
 * ## TooltipBox
 *
 * Declarative tooltip container wrapping an interactive UI element.
 * Automatically coordinates with [LocalOverlayHost] to display a floating tooltip
 * when hovered, with automatic cleanup on unmount.
 *
 * See: docs/architecture/architecture_en.md
 */
@Composable
fun TooltipBox(
    text: String,
    modifier: UIModifier = UIModifier,
    content: @Composable BoxScope.() -> Unit
) {
    val overlayHost = LocalOverlayHost.current

    DisposableEffect(text) {
        onDispose {
            overlayHost.hideTooltip()
        }
    }

    Box(
        modifier = Modifier
            .onHover { isHovered ->
                if (isHovered) {
                    overlayHost.showTooltip(text, 0f, 0f, 100f, 40f)
                } else {
                    overlayHost.hideTooltip()
                }
            }
            .then(modifier)
    ) {
        content()
    }
}
