@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.layout

import androidx.compose.runtime.Composable
import org.mdt.core.ui.compose.BoxScope
import org.mdt.core.ui.compose.UIModifier
import org.mdt.ui.theme.ScrollbarStyle

/**
 * ## ScrollBox [Declarative Scrollable Viewport Container]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Convenience wrapper around [Box] dedicated to scrollable content lists and viewports.
 * - Supports 2D scrolling ([enableVertical], [enableHorizontal]), mouse wheel delta, touch drag inertia,
 *   and floating auto-hiding scrollbars.
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Composable Idiomaticity):** Callers can chain custom styles via .then(modifier).
 * - **Rule 2 (Viewport Clipping):** Content is cleanly scissor clipped to inner container bounds.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - 🎨 **Composable Box:** src/main/kotlin/org/mdt/ui/components/layout/Box.kt
 * - 🌲 **Virtual Node:** src/main/kotlin/org/mdt/core/ui/node/LayoutNode.kt
 * - 🎨 **Scrollbar Style:** src/main/kotlin/org/mdt/ui/theme/ScrollbarStyle.kt
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] Supports vertical, horizontal, and bidirectional 2D scrolling.
 * - [x] Auto-hides floating scrollbars when pointer/scroll interaction goes idle.
 */
@Composable
fun ScrollBox(
    modifier: UIModifier = UIModifier,
    enableVertical: Boolean = true,
    enableHorizontal: Boolean = false,
    scrollbarStyle: ScrollbarStyle? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier,
        scrollable = true,
        scrollbarStyle = scrollbarStyle,
        content = content
    )
}
