// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.ui.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.mdt.core.ui.compose.NodeApplier
import org.mdt.core.ui.modifier.UIModifier
import org.mdt.core.ui.layout.BoxMeasurePolicy
import org.mdt.core.ui.node.LayoutNode
import org.mdt.ui.theme.LocalScrollbarStyle
import org.mdt.ui.theme.ScrollbarStyle

/**
 * ## Box
 *
 * Fundamental multi-child layout container stacking children along the Z-axis with anchor/alignment positioning and optional 2D scrolling.
 */
@Composable
fun Box(
    modifier: UIModifier = UIModifier,
    scrollable: Boolean = false,
    enableVerticalScroll: Boolean = true,
    enableHorizontalScroll: Boolean = false,
    scrollbarStyle: ScrollbarStyle? = null,
    content: @Composable org.mdt.core.ui.layout.BoxScope.() -> Unit = {}
) {
    val activeStyle = scrollbarStyle ?: LocalScrollbarStyle.current

    ComposeNode<LayoutNode, NodeApplier>(
        factory = {
            val node = LayoutNode()
            node.measurePolicy = BoxMeasurePolicy
            node.scrollable = scrollable
            node.enableVerticalScroll = enableVerticalScroll
            node.enableHorizontalScroll = enableHorizontalScroll
            node.scrollbarStyle = activeStyle
            node.modifier = modifier
            node
        },
        update = {
            set(modifier) { this.modifier = it }
            set(scrollable) {
                this.scrollable = it
                invalidateLayout()
            }
            set(enableVerticalScroll) {
                this.enableVerticalScroll = it
                invalidateLayout()
            }
            set(enableHorizontalScroll) {
                this.enableHorizontalScroll = it
                invalidateLayout()
            }
            set(activeStyle) {
                this.scrollbarStyle = it
                invalidateLayout()
            }
        },
        content = {
            org.mdt.core.ui.layout.BoxScopeInstance.content()
        }
    )
}

/**
 * ## ScrollBox
 *
 * Dedicated scrollable container wrapper around [Box] supporting 2D scrolling and floating scrollbars.
 */
@Composable
fun ScrollBox(
    modifier: UIModifier = UIModifier,
    enableVertical: Boolean = true,
    enableHorizontal: Boolean = false,
    scrollbarStyle: ScrollbarStyle? = null,
    content: @Composable org.mdt.core.ui.layout.BoxScope.() -> Unit
) {
    Box(
        modifier = modifier,
        scrollable = true,
        enableVerticalScroll = enableVertical,
        enableHorizontalScroll = enableHorizontal,
        scrollbarStyle = scrollbarStyle,
        content = content
    )
}

