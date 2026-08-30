// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.ui.theme

import androidx.compose.runtime.compositionLocalOf
import org.mdt.core.ui.unit.Color

/**
 * ## ScrollbarStyle
 *
 * Visual and timing configuration for auto-hiding floating scrollbars.
 * Controls thumb/track colors, thickness, corner radii, idle fade-out timers, and scroll speeds.
 *
 * @property thumbColor Fill color for the interactive draggable scrollbar thumb bar.
 * @property trackColor Fill color for the background scrollbar track gutter.
 * @property thickness Scrollbar width/thickness in pixels.
 * @property radius Corner radius in pixels for rounded scrollbar thumb ends.
 * @property autoHide Whether the scrollbar automatically fades out during idle periods.
 * @property idleTimeoutMs Inactivity delay in milliseconds before fade-out initiates.
 * @property fadeDurationMs Transition duration in milliseconds for the alpha fade animation.
 * @property scrollSpeed Scroll step distance multiplier in pixels per wheel notch.
 *
 * @see LocalScrollbarStyle
 * @see org.mdt.core.ui.node.LayoutNode
 */
data class ScrollbarStyle(
    val thumbColor: Color = Color(0.65f, 0.65f, 0.68f, 0.50f),
    val trackColor: Color = Color.Clear,
    val thickness: Float = 4.0f,
    val radius: Float = 2.0f,
    val autoHide: Boolean = true,
    val idleTimeoutMs: Long = 1200L,
    val fadeDurationMs: Long = 350L,
    val scrollSpeed: Float = 36.0f
) {
    companion object {
        val Default = ScrollbarStyle()
    }
}

/**
 * CompositionLocal distributing the ambient [ScrollbarStyle] across the UI tree.
 */
val LocalScrollbarStyle = compositionLocalOf { ScrollbarStyle.Default }
