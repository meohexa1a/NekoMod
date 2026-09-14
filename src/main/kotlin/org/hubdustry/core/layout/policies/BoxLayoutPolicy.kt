package org.hubdustry.core.layout.policies

import androidx.compose.ui.util.fastForEach
import org.hubdustry.core.layout.LayoutNode
import org.hubdustry.core.layout.SizeFlag
import org.hubdustry.core.layout.computeOffset

/**
 * Bố cục tự do dạng Box. Hỗ trợ định vị theo Anchor/Preset hoặc căn chỉnh theo Alignment/SizeFlags.
 * Tinh gọn và thuần toán học (Pure Math).
 */
object BoxLayoutPolicy : LayoutPolicy {
    override fun computeMinSize(node: LayoutNode) {
        var maxChildMinWidth = 0f
        var maxChildMinHeight = 0f

        node.children.fastForEach { child ->
            if (!child.visible) return@fastForEach
            child.policy.computeMinSize(child)
            if (child.anchor.isEnabled) return@fastForEach

            val childRequiredW = child.minWidth + child.marginLeft + child.marginRight
            val childRequiredH = child.minHeight + child.marginTop + child.marginBottom
            if (childRequiredW > maxChildMinWidth) maxChildMinWidth = childRequiredW
            if (childRequiredH > maxChildMinHeight) maxChildMinHeight = childRequiredH
        }

        val totalCalculatedW = maxChildMinWidth + node.paddingLeft + node.paddingRight
        val totalCalculatedH = maxChildMinHeight + node.paddingTop + node.paddingBottom

        // contentWidth / contentHeight là kích thước thuần của nội dung bên trong inner bounds (tránh cộng đúp padding khi cuộn)
        node.contentWidth = maxChildMinWidth
        node.contentHeight = maxChildMinHeight

        val contentMinWidth = if (node.isScrollableHorizontal) node.paddingLeft + node.paddingRight else totalCalculatedW
        val contentMinHeight = if (node.isScrollableVertical) node.paddingTop + node.paddingBottom else totalCalculatedH

        // Cập nhật minWidth/minHeight nhưng tuyệt đối KHÔNG đục thủng giới hạn maxWidth/maxHeight tường minh
        node.minWidth = maxOf(node.minWidth, contentMinWidth).coerceAtMost(node.maxWidth)
        node.minHeight = maxOf(node.minHeight, contentMinHeight).coerceAtMost(node.maxHeight)
    }

    override fun arrangeChildren(
        node: LayoutNode,
        innerX: Float,
        innerY: Float,
        innerWidth: Float,
        innerHeight: Float
    ) {
        node.children.fastForEach { child ->
            if (!child.visible) return@fastForEach

            if (child.anchor.isEnabled) {
                child.resolveAnchors(innerX, innerY, innerWidth, innerHeight)
                return@fastForEach
            }

            val availableW = maxOf(0f, innerWidth - child.marginLeft - child.marginRight)
            val availableH = maxOf(0f, innerHeight - child.marginTop - child.marginBottom)

            val childW = when (child.sizeFlagHorizontal) {
                SizeFlag.FILL -> availableW
                SizeFlag.SHRINK -> child.minWidth
            }.coerceAtMost(child.maxWidth)

            val childH = when (child.sizeFlagVertical) {
                SizeFlag.FILL -> availableH
                SizeFlag.SHRINK -> child.minHeight
            }.coerceAtMost(child.maxHeight)

            val childX = innerX + child.marginLeft + child.alignHorizontal.computeOffset(availableW, childW)
            val childY = innerY + child.marginTop + child.alignVertical.computeOffset(availableH, childH)

            child.arrange(childX, childY, childW, childH)
        }
    }
}

