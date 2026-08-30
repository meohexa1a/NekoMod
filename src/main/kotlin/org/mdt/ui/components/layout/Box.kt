package org.mdt.ui.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.mdt.core.ui.compose.NodeApplier
import org.mdt.core.ui.compose.UIModifier
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
    scrollbarStyle: ScrollbarStyle? = null,
    content: @Composable () -> Unit = {}
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
        content = content
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
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier,
        scrollable = true,
        scrollbarStyle = scrollbarStyle,
        content = content
    )
}

