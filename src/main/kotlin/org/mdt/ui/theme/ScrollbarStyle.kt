package org.mdt.ui.theme

import androidx.compose.runtime.compositionLocalOf
import org.mdt.core.ui.unit.Color

/**
 * ## ScrollbarStyle [Floating Scrollbar Visual Configuration]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Design tokens and behavioral settings for auto-hiding floating scrollbars.
 * - Controls thumb and track colors, thickness, corner radii, idle fade-out timing, and mouse wheel scroll speed.
 * - Ambiently distributed throughout the Compose hierarchy via [LocalScrollbarStyle].
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Zero-GC Float Geometry):** Dimensions and timing use standard Float and Long primitives.
 * - **Rule 2 (Fail-Safe Defaults):** [Default] provides a sleek semi-transparent gray thumb matching modern desktop & game UIs.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - 🌲 **Virtual Container Node:** src/main/kotlin/org/mdt/core/ui/node/LayoutNode.kt
 * - 🎨 **Composable Box:** src/main/kotlin/org/mdt/ui/components/layout/Box.kt
 * - 🎨 **Composable ScrollBox:** src/main/kotlin/org/mdt/ui/components/layout/ScrollBox.kt
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] [autoHide] fades out thumb alpha smoothly after [idleTimeoutMs] of pointer/scroll inactivity.
 * - [x] Provides ambient default instance via [LocalScrollbarStyle].
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
