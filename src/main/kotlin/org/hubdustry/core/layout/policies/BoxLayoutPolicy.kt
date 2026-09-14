package org.hubdustry.core.layout.policies

import androidx.compose.ui.util.fastForEach
import org.hubdustry.core.layout.LayoutNode
import org.hubdustry.core.layout.SizeFlag
import org.hubdustry.core.layout.computeOffset

// ─── Box Layout Policy ───────────────────────────────────────────

/**
 * Bố cục tự do dạng Box. Hỗ trợ định vị theo Anchor/Preset hoặc căn chỉnh theo Alignment/SizeFlags.
 * Tinh gọn và thuần toán học (Pure Math).
 */
object BoxLayoutPolicy : LayoutPolicy {

    // ─── Intrinsic Measurement ────────────────────────────────────────

    override fun computeMinSize(node: LayoutNode) {
        var maxChildMinWidth = 0f
        var maxChildMinHeight = 0f

        val children = node.children
        val count = children.size
        for (i in 0 until count) {
            val child = children[i]
            if (!child.visible) continue
            child.policy.computeMinSize(child)
            if (child.anchor.isEnabled) continue

            val childRequiredWidth = child.minWidth + child.marginLeft + child.marginRight
            val childRequiredHeight = child.minHeight + child.marginTop + child.marginBottom
            if (childRequiredWidth > maxChildMinWidth) maxChildMinWidth = childRequiredWidth
            if (childRequiredHeight > maxChildMinHeight) maxChildMinHeight = childRequiredHeight
        }

        val totalCalculatedWidth = maxChildMinWidth + node.paddingLeft + node.paddingRight
        val totalCalculatedHeight = maxChildMinHeight + node.paddingTop + node.paddingBottom

        // contentWidth / contentHeight là kích thước thuần của nội dung bên trong inner bounds (tránh cộng đúp padding khi cuộn)
        node.contentWidth = maxChildMinWidth
        node.contentHeight = maxChildMinHeight

        val contentMinWidth = if (node.isScrollableHorizontal) node.paddingLeft + node.paddingRight else totalCalculatedWidth
        val contentMinHeight = if (node.isScrollableVertical) node.paddingTop + node.paddingBottom else totalCalculatedHeight

        // Cập nhật minWidth/minHeight nhưng tuyệt đối KHÔNG đục thủng giới hạn maxWidth/maxHeight tường minh
        node.minWidth = maxOf(node.minWidth, contentMinWidth).coerceAtMost(node.maxWidth)
        node.minHeight = maxOf(node.minHeight, contentMinHeight).coerceAtMost(node.maxHeight)
    }

    // ─── Children Placement ───────────────────────────────────────────

    override fun arrangeChildren(
        node: LayoutNode,
        innerX: Float,
        innerY: Float,
        innerWidth: Float,
        innerHeight: Float
    ) {
        val children = node.children
        val count = children.size
        for (i in 0 until count) {
            val child = children[i]
            if (!child.visible) continue

            if (child.anchor.isEnabled) {
                child.resolveAnchors(innerX, innerY, innerWidth, innerHeight)
                continue
            }

            val availableWidth = maxOf(0f, innerWidth - child.marginLeft - child.marginRight)
            val availableHeight = maxOf(0f, innerHeight - child.marginTop - child.marginBottom)

            val childWidth = when (child.sizeFlagHorizontal) {
                SizeFlag.FILL -> availableWidth
                SizeFlag.SHRINK -> child.minWidth
            }.coerceAtMost(child.maxWidth)

            val childHeight = when (child.sizeFlagVertical) {
                SizeFlag.FILL -> availableHeight
                SizeFlag.SHRINK -> child.minHeight
            }.coerceAtMost(child.maxHeight)

            val childX = innerX + child.marginLeft + child.alignHorizontal.computeOffset(availableWidth, childWidth)
            val childY = innerY + child.marginTop + child.alignVertical.computeOffset(availableHeight, childHeight)

            child.arrange(childX, childY, childWidth, childHeight)
        }
    }
}


