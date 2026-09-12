package org.hubdustry.libs.layout.policies

import org.hubdustry.libs.layout.LayoutNode
import org.hubdustry.libs.layout.LayoutPolicy
import org.hubdustry.libs.layout.SizeFlag
import org.hubdustry.libs.layout.computeOffset

/**
 * Bố cục tự do dạng Box. Hỗ trợ định vị theo Anchor/Preset hoặc căn chỉnh theo Alignment/SizeFlags.
 * Tinh gọn và thuần toán học (Pure Math).
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
                val childRequiredW = child.minWidth + child.marginLeft + child.marginRight
                val childRequiredH = child.minHeight + child.marginTop + child.marginBottom
                if (childRequiredW > maxChildMinWidth) maxChildMinWidth = childRequiredW
                if (childRequiredH > maxChildMinHeight) maxChildMinHeight = childRequiredH
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
                child.resolveAnchors(innerX, innerY, innerWidth, innerHeight)
            } else {
                val availableW = maxOf(0f, innerWidth - child.marginLeft - child.marginRight)
                val availableH = maxOf(0f, innerHeight - child.marginTop - child.marginBottom)

                val childW = when (child.sizeFlagHorizontal) {
                    SizeFlag.FILL, SizeFlag.EXPAND -> availableW
                    SizeFlag.SHRINK -> child.minWidth
                }.coerceAtMost(child.maxWidth)

                val childH = when (child.sizeFlagVertical) {
                    SizeFlag.FILL, SizeFlag.EXPAND -> availableH
                    SizeFlag.SHRINK -> child.minHeight
                }.coerceAtMost(child.maxHeight)

                val childX = innerX + child.marginLeft + child.alignHorizontal.computeOffset(availableW, childW)
                val childY = innerY + child.marginTop + child.alignVertical.computeOffset(availableH, childH)

                child.arrange(childX, childY, childW, childH)
            }
        }
    }
}

