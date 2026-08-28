package org.mdt.core.ui.node

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * ## OverlayHost
 *
 * Contract for top-layer overlay management (Tooltips, Popups, Modals, Dropdowns).
 * Elements registered here are rendered on top of the regular UI tree in the final pass of the canvas.
 *
 * See: docs/ui-engine/ui_engine_en.md
 */
interface OverlayHost {

    /** Registers a node to be rendered in the top-layer overlay pass. */
    fun registerOverlay(node: UINode)

    /** Unregisters a top-layer overlay node. */
    fun unregisterOverlay(node: UINode)

    /** Clears all registered top-layer overlays. */
    fun clearOverlays()
}

/**
 * ## LocalOverlayHost
 *
 * CompositionLocal providing access to the nearest [OverlayHost] (usually the root CanvasNode).
 *
 * See: docs/ui-engine/ui_engine_en.md
 */
val LocalOverlayHost = staticCompositionLocalOf<OverlayHost> {
    error("No OverlayHost provided in the current CompositionLocal hierarchy.")
}
