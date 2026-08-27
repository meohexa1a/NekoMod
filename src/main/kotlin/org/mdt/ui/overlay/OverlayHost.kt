package org.mdt.ui.overlay

import androidx.compose.runtime.*
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.LayoutPreset
import org.mdt.ui.components.display.Tooltip
import org.mdt.ui.components.layout.Box

/**
 * ## ActiveTooltipData
 *
 * Data model for an active floating tooltip anchor.
 */
data class ActiveTooltipData(
    val text: String,
    val anchorX: Float,
    val anchorY: Float,
    val anchorW: Float,
    val anchorH: Float
)

/**
 * ## OverlayHostState
 *
 * State manager coordinating reactive top-layer presentation:
 * floating tooltips, context menus, and modal dialog overlays with zero static singletons.
 */
class OverlayHostState {

    var activeTooltip by mutableStateOf<ActiveTooltipData?>(null)
        private set

    var activeModal by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set

    fun showTooltip(text: String, x: Float = 0f, y: Float = 0f, w: Float = 0f, h: Float = 0f) {
        activeTooltip = ActiveTooltipData(text, x, y, w, h)
    }

    fun hideTooltip() {
        activeTooltip = null
    }

    fun showModal(content: @Composable () -> Unit) {
        activeModal = content
    }

    fun dismissModal() {
        activeModal = null
    }
}

/**
 * CompositionLocal providing access to the ambient [OverlayHostState].
 */
val LocalOverlayHost = staticCompositionLocalOf<OverlayHostState> {
    error("No LocalOverlayHost provided in the active composition tree.")
}

/**
 * ## OverlayHost
 *
 * Top-layer container rendering the main UI tree alongside reactive floating tooltips
 * and modal popover dialogs at the highest Z-index.
 *
 * See: docs/architecture/architecture_en.md
 */
@Composable
fun OverlayHost(
    state: OverlayHostState = remember { OverlayHostState() },
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalOverlayHost provides state) {
        Box(modifier = Modifier.anchor(LayoutPreset.FULL_RECT).fillMaxSize()) {

            // 1. Primary UI tree content
            content()

            // 2. Modal Overlay Layer
            state.activeModal?.let { it() }

            // 3. Floating Tooltip Top-Layer Pass
            state.activeTooltip?.let { tip ->
                val tooltipMargin = 8f
                val tipX = maxOf(8f, tip.anchorX + (tip.anchorW * 0.5f) - 60f)
                val tipY = tip.anchorY + tip.anchorH + tooltipMargin

                Box(
                    modifier = Modifier
                        .anchor(LayoutPreset.BOTTOM_LEFT)
                        .margin(left = tipX, bottom = tipY)
                ) {
                    Tooltip(text = tip.text)
                }
            }
        }
    }
}
