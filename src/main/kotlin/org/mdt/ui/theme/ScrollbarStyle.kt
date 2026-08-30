package org.mdt.ui.theme

import androidx.compose.runtime.compositionLocalOf
import org.mdt.core.ui.unit.Color

/**
 * ## ScrollbarStyle [Scrollbar Configuration]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Visual and timing configuration for auto-hiding floating scrollbars.
 * - Controls thumb/track colors, thickness, corner radii, idle fade-out timers, and scroll speeds.
 *
 * ### 2. Key Rules & Checklist
 * - [x] All dimensions, corner radii, and speeds must use `Float`.
 * - [x] Fades out thumb alpha smoothly after `idleTimeoutMs` of inactivity when `autoHide = true`.
 * - [x] Ambient default instance is accessible via [LocalScrollbarStyle].
 *
 * ### 3. Related Files
 * - Container Node: `src/main/kotlin/org/mdt/core/ui/node/LayoutNode.kt`
 * - Box Component: `src/main/kotlin/org/mdt/ui/components/layout/Box.kt`
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
