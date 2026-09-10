package org.hubdustry.libs.layout.policies

import org.hubdustry.libs.layout.AnchorMath
import org.hubdustry.libs.layout.LayoutNode
import org.hubdustry.libs.layout.LayoutPolicy
import org.hubdustry.libs.layout.SizeFlag
import org.hubdustry.libs.layout.computeOffset

/**
 * Bố cục tự do dạng Box. Hỗ trợ định vị theo Anchor/Preset hoặc căn chỉnh theo Alignment/SizeFlags.
 */
object BoxLayoutPolicy : LayoutPolicy {
    override fun computeMinSize(node: LayoutNode) {
        var maxChildMinWidth = 0f
        var maxChildMinHeight = 0f

        val count = node.children.size
        for (i in 0 until count) {
            val child = node.children[i]
            if (!child.visible) continue
            child.policy.computeMinSize(child)
            if (!child.anchor.isEnabled) {
                if (child.minWidth > maxChildMinWidth) {
                    maxChildMinWidth = child.minWidth
                }
                if (child.minHeight > maxChildMinHeight) {
                    maxChildMinHeight = child.minHeight
                }
            }
        }

        node.minWidth = maxOf(node.minWidth, maxChildMinWidth + node.paddingLeft + node.paddingRight)
        node.minHeight = maxOf(node.minHeight, maxChildMinHeight + node.paddingTop + node.paddingBottom)
    }

    override fun arrangeChildren(
        node: LayoutNode,
        innerX: Float,
        innerY: Float,
        innerWidth: Float,
        innerHeight: Float
    ) {
        val count = node.children.size
        for (i in 0 until count) {
            val child = node.children[i]
            if (!child.visible) continue

            if (child.anchor.isEnabled) {
                AnchorMath.resolve(child, innerX, innerY, innerWidth, innerHeight)
            } else {
                val childW = when (child.sizeFlagHorizontal) {
                    SizeFlag.FILL, SizeFlag.EXPAND -> innerWidth
                    SizeFlag.SHRINK -> child.minWidth
                }.coerceIn(child.minWidth, child.maxWidth)

                val childH = when (child.sizeFlagVertical) {
                    SizeFlag.FILL, SizeFlag.EXPAND -> innerHeight
                    SizeFlag.SHRINK -> child.minHeight
                }.coerceIn(child.minHeight, child.maxHeight)

                child.x = innerX + child.alignHorizontal.computeOffset(innerWidth, childW)
                child.y = innerY + child.alignVertical.computeOffset(innerHeight, childH)
                child.width = childW
                child.height = childH
            }

            val childInnerX = child.paddingLeft
            val childInnerY = child.paddingTop
            val childInnerW = maxOf(0f, child.width - child.paddingLeft - child.paddingRight)
            val childInnerH = maxOf(0f, child.height - child.paddingTop - child.paddingBottom)
            child.policy.arrangeChildren(child, childInnerX, childInnerY, childInnerW, childInnerH)
        }
    }
}

