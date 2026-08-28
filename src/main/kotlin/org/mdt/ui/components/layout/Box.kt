@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.mdt.core.ui.compose.BoxScope
import org.mdt.core.ui.compose.NodeApplier
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.layout.BoxMeasurePolicy
import org.mdt.core.ui.node.LayoutNode
import org.mdt.ui.theme.LocalScrollbarStyle
import org.mdt.ui.theme.ScrollbarStyle

/**
 * ## Box [Declarative Box & Stack Layout Container]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Fundamental multi-child layout container stacking children along the Z-axis with anchor and alignment positioning.
 * - Supports built-in 2D scrolling ([scrollable]) with customizable floating scrollbars ([scrollbarStyle]).
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Zero-Cost Default):** When `scrollable = false`, behaves as a lightweight static container without clipping overhead.
 * - **Rule 2 (Ambient Theme Inheritance):** Inherits [LocalScrollbarStyle] unless explicitly overridden.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - 🌲 **Virtual Node:** `src/main/kotlin/org/mdt/core/ui/node/LayoutNode.kt`
 * - 🎨 **Scrollbar Style:** `src/main/kotlin/org/mdt/ui/theme/ScrollbarStyle.kt`
 * - 🎨 **ScrollBox Helper:** `src/main/kotlin/org/mdt/ui/components/layout/ScrollBox.kt`
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] Applying `scrollable = true` activates native scrolling and auto-hiding scrollbars.
 * - [x] Inherits [LocalScrollbarStyle] from the ambient CompositionLocal hierarchy.
 */
@Composable
fun Box(
    modifier: UIModifier = UIModifier,
    scrollable: Boolean = false,
    scrollbarStyle: ScrollbarStyle? = null,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val activeStyle = scrollbarStyle ?: LocalScrollbarStyle.current

    ComposeNode<LayoutNode, NodeApplier>(
        factory = {
            val node = LayoutNode()
            node.measurePolicy = BoxMeasurePolicy
            node.scrollable = scrollable
            node.scrollbarThumbColor = activeStyle.thumbColor
            node.scrollbarTrackColor = activeStyle.trackColor
            node.scrollbarThickness = activeStyle.thickness
            node.scrollbarRadius = activeStyle.radius
            node.scrollbarAutoHide = activeStyle.autoHide
            node.scrollbarIdleTimeoutMs = activeStyle.idleTimeoutMs
            node.scrollbarFadeDurationMs = activeStyle.fadeDurationMs
            node.scrollSpeed = activeStyle.scrollSpeed
            modifier.applyTo(node)
            node
        },
        update = {
            set(scrollable) {
                this.scrollable = it
                invalidateLayout()
            }
            set(activeStyle) {
                this.scrollbarThumbColor = it.thumbColor
                this.scrollbarTrackColor = it.trackColor
                this.scrollbarThickness = it.thickness
                this.scrollbarRadius = it.radius
                this.scrollbarAutoHide = it.autoHide
                this.scrollbarIdleTimeoutMs = it.idleTimeoutMs
                this.scrollbarFadeDurationMs = it.fadeDurationMs
                this.scrollSpeed = it.scrollSpeed
                invalidateLayout()
            }
            set(modifier) {
                it.applyTo(this)
                invalidateLayout()
            }
        },
        content = {
            BoxScope.content()
        }
    )
}
