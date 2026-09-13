package org.hubdustry.core.compose.view

import arc.graphics.Color
import mindustry.ui.Fonts
import org.hubdustry.core.graphics.UIBatch
import org.hubdustry.core.layout.LayoutNode

/**
 * Trình dựng hình đệ quy Virtual DOM ([LayoutNode]) phát lệnh vẽ ra [UIBatch].
 *
 * TÁCH BIỆT TRÁCH NHIỆM (Đợt 2 Refactor):
 * 1. Chịu trách nhiệm 100% việc chuyển đổi không gian Top-Left (Y-down) sang Arc Scene2D (Y-up) tại thời điểm vẽ.
 * 2. Hot-Path Zero-GC: Sở hữu các bộ đệm [Color] scratchpad tái sử dụng, cấm cấp phát object trong frame loop.
 * 3. Hỗ trợ đầy đủ background, bo góc SDF shader, viền ngoài, văn bản BMFont có padding, và thanh cuộn.
 */
object NodeRenderer {

    private val colorScratch = Color()
    private val borderScratch = Color()
    private val textScratch = Color()
    private val scrollbarScratch = Color()

    /**
     * Bắt đầu dựng hình cây Virtual DOM từ [rootNode].
     */
    fun render(
        rootNode: LayoutNode,
        viewX: Float,
        viewY: Float,
        viewH: Float
    ) {
        renderNodeRecursive(
            node = rootNode,
            parentLocalX = 0f,
            parentLocalY = 0f,
            parentEffectiveAlpha = 1f,
            viewX = viewX,
            viewY = viewY,
            viewH = viewH
        )
    }

    private fun renderNodeRecursive(
        node: LayoutNode,
        parentLocalX: Float,
        parentLocalY: Float,
        parentEffectiveAlpha: Float,
        viewX: Float,
        viewY: Float,
        viewH: Float
    ) {
        if (!node.visible) return

        val effectiveAlpha = (parentEffectiveAlpha * node.alpha).coerceIn(0f, 1f)
        if (effectiveAlpha <= 0f) return

        val nodeLocalX = parentLocalX + node.x + node.offsetX
        val nodeLocalY = parentLocalY + node.y + node.offsetY
        val nodeW = node.width
        val nodeH = node.height

        val arcX = viewX + nodeLocalX
        val arcY = viewY + viewH - (nodeLocalY + nodeH)

        if (node.clip) UIBatch.pushClip(arcX, arcY, nodeW, nodeH)

        renderBackgroundAndBorder(node, arcX, arcY, nodeW, nodeH, effectiveAlpha)
        renderText(node, arcX, arcY, nodeW, nodeH, effectiveAlpha)

        // Duyệt con bằng vòng lặp chỉ mục thuần túy (Zero-GC), bù trừ độ cuộn (scrollX, scrollY)
        val childParentLocalX = nodeLocalX - node.scrollX
        val childParentLocalY = nodeLocalY - node.scrollY
        val children = node.children
        val count = children.size
        for (i in 0 until count) {
            renderNodeRecursive(
                node = children[i],
                parentLocalX = childParentLocalX,
                parentLocalY = childParentLocalY,
                parentEffectiveAlpha = effectiveAlpha,
                viewX = viewX,
                viewY = viewY,
                viewH = viewH
            )
        }

        renderScrollbars(node, arcX, arcY, nodeW, nodeH, effectiveAlpha)

        if (node.clip) UIBatch.popClip()
    }

    private fun renderBackgroundAndBorder(
        node: LayoutNode,
        arcX: Float,
        arcY: Float,
        nodeW: Float,
        nodeH: Float,
        effectiveAlpha: Float
    ) {
        if (node.backgroundColor == null && !node.hasBorder) return

        val bg = node.backgroundColor
        val fillColor = if (bg != null) {
            if (effectiveAlpha < 1f) colorScratch.set(bg.r, bg.g, bg.b, bg.a * effectiveAlpha) else bg
        } else Color.clear

        val borderColor = if (node.hasBorder) {
            if (effectiveAlpha < 1f) borderScratch.set(node.borderColor.r, node.borderColor.g, node.borderColor.b, node.borderColor.a * effectiveAlpha) else node.borderColor
        } else Color.clear

        val radius = maxOf(node.cornerRadiusTopStart, node.cornerRadiusTopEnd, node.cornerRadiusBottomEnd, node.cornerRadiusBottomStart)

        UIBatch.drawBox(
            x = arcX,
            y = arcY,
            width = nodeW,
            height = nodeH,
            radius = radius,
            color = fillColor,
            borderWidth = if (node.hasBorder) node.borderWidth else 0f,
            borderColor = borderColor
        )
    }

    private fun renderText(
        node: LayoutNode,
        arcX: Float,
        arcY: Float,
        nodeW: Float,
        nodeH: Float,
        effectiveAlpha: Float
    ) {
        val textContent = node.text ?: return
        if (textContent.isEmpty()) return

        try {
            val font = node.font ?: Fonts.def ?: return
            val textColor = if (effectiveAlpha < 1f) {
                textScratch.set(node.textColor.r, node.textColor.g, node.textColor.b, node.textColor.a * effectiveAlpha)
            } else node.textColor

            val capHeight = font.data.capHeight
            val innerH = maxOf(0f, nodeH - node.paddingTop - node.paddingBottom)
            val innerW = maxOf(0f, nodeW - node.paddingLeft - node.paddingRight)
            val drawX = arcX + node.paddingLeft
            val drawY = if (innerH > capHeight) (arcY + node.paddingBottom) + (innerH + capHeight) * 0.5f else arcY + nodeH - node.paddingTop

            UIBatch.drawText(
                font = font,
                text = textContent,
                x = drawX,
                y = drawY,
                targetWidth = innerW,
                color = textColor
            )
        } catch (_: Throwable) {
            // Headless test environment fallback
        }
    }

    private fun renderScrollbars(
        node: LayoutNode,
        arcX: Float,
        arcY: Float,
        nodeW: Float,
        nodeH: Float,
        effectiveAlpha: Float
    ) {
        if (node.isScrollableVertical && node.maxScrollY > 0.001f && nodeH > 10f) {
            val thumbH = maxOf(16f, (nodeH / (nodeH + node.maxScrollY)) * nodeH)
            val availableTrack = nodeH - thumbH
            val progress = (node.scrollY / node.maxScrollY).coerceIn(0f, 1f)
            val thumbTop = progress * availableTrack
            val arcThumbY = arcY + nodeH - thumbTop - thumbH
            val scrollbarW = 4f
            val arcScrollbarX = arcX + nodeW - scrollbarW - 2f

            scrollbarScratch.set(1f, 1f, 1f, 0.35f * effectiveAlpha)
            UIBatch.drawBox(
                x = arcScrollbarX,
                y = arcThumbY,
                width = scrollbarW,
                height = thumbH,
                radius = 2f,
                color = scrollbarScratch
            )
        }

        if (node.isScrollableHorizontal && node.maxScrollX > 0.001f && nodeW > 10f) {
            val thumbW = maxOf(16f, (nodeW / (nodeW + node.maxScrollX)) * nodeW)
            val availableTrack = nodeW - thumbW
            val progress = (node.scrollX / node.maxScrollX).coerceIn(0f, 1f)
            val thumbLeft = progress * availableTrack
            val arcThumbX = arcX + thumbLeft
            val scrollbarH = 4f
            val arcScrollbarY = arcY + 2f

            scrollbarScratch.set(1f, 1f, 1f, 0.35f * effectiveAlpha)
            UIBatch.drawBox(
                x = arcThumbX,
                y = arcScrollbarY,
                width = thumbW,
                height = scrollbarH,
                radius = 2f,
                color = scrollbarScratch
            )
        }
    }
}
