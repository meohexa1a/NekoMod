package org.hubdustry.core.compose.view

import androidx.compose.ui.util.fastForEach
import arc.graphics.Color
import mindustry.ui.Fonts
import org.hubdustry.core.graphics.UIBatch
import org.hubdustry.core.layout.LayoutNode

private const val MIN_RENDER_EPSILON = 0.001f
private const val MIN_SCROLLBAR_CONTAINER_LENGTH = 10f
private const val MIN_SCROLLBAR_THUMB_LENGTH = 16f
private const val SCROLLBAR_THICKNESS = 4f
private const val SCROLLBAR_MARGIN = 2f
private const val SCROLLBAR_RADIUS = 2f
private const val SCROLLBAR_ALPHA = 0.35f

/**
 * ## NodeRenderer
 *
 * Thực thi việc dựng hình đệ quy Virtual DOM Node xuống GPU thông qua [UIBatch].
 * Tách biệt hoàn toàn trách nhiệm render khỏi [ComposeView].
 *
 * ĐẶC TÍNH KIẾN TRÚC:
 * 1. **Zero-GC Hot-Path**:
 *    - Sử dụng các scratchpad Color tĩnh tái sử dụng (`colorScratch`, `borderScratch`, `textScratch`, `scrollbarScratch`).
 *    - Tuyệt đối không cấp phát đối tượng mới hoặc lambda closure trong frame loop.
 * 2. **Chuyển đổi tọa độ Bức tường Berlin**:
 *    - Node Virtual DOM quản lý tọa độ cục bộ Top-Left ($Y$-down).
 *    - Tại duy nhất thời điểm phát lệnh vẽ, tính toán tọa độ thế giới Arc Viewport (Y-up):
 *      `arcY = viewY + viewHeight - (nodeLocalY + nodeHeight)`.
 *
 * ```
 *   NekoMod Virtual DOM (Y-down)                 Arc Scene2D / OpenGL (Y-up)
 *   (0, 0) Top-Left                             (viewX, viewY + viewHeight) Top-Left
 *     ┌────────────────────┐   BERLIN WALL        ┌────────────────────┐
 *     │ nodeY ↓            │  ═════════════►      │                    │
 *     │ ┌────────────────┐ │                      │ ┌────────────────┐ │
 *     │ │ Node           │ │                      │ │ Node           │ │ arcY ↑
 *     │ └────────────────┘ │                      │ └────────────────┘ │
 *     └────────────────────┘                      └────────────────────┘
 *     (width, height)                             (viewX, viewY) Bottom-Left
 *       Formula: arcY = viewY + viewHeight - (nodeLocalY + nodeHeight)
 * ```
 * 3. **Cắt gọt tự động (Auto-Clipping)**:
 *    - Tự động cắt gọt nội dung con và văn bản BMFont khi vượt quá đệm nội tại của node.
 */
internal object NodeRenderer {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. ZERO-GC SCRATCHPADS & PUBLIC ENTRY POINT
    // ─────────────────────────────────────────────────────────────────────────

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
        viewHeight: Float
    ) {
        renderNodeRecursive(
            node = rootNode,
            parentLocalX = 0f,
            parentLocalY = 0f,
            parentEffectiveAlpha = 1f,
            viewX = viewX,
            viewY = viewY,
            viewHeight = viewHeight
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. RECURSIVE TREE TRAVERSAL & CLIPPING
    // ─────────────────────────────────────────────────────────────────────────

    private fun renderNodeRecursive(
        node: LayoutNode,
        parentLocalX: Float,
        parentLocalY: Float,
        parentEffectiveAlpha: Float,
        viewX: Float,
        viewY: Float,
        viewHeight: Float
    ) {
        if (!node.visible) return

        val effectiveAlpha = (parentEffectiveAlpha * node.alpha).coerceIn(0f, 1f)
        if (effectiveAlpha <= 0f) return

        val nodeLocalX = parentLocalX + node.x + node.offsetX
        val nodeLocalY = parentLocalY + node.y + node.offsetY
        val nodeWidth = node.width
        val nodeHeight = node.height

        val arcX = viewX + nodeLocalX
        val arcY = viewY + viewHeight - (nodeLocalY + nodeHeight)

        if (node.clip) {
            UIBatch.pushClip(
                x = arcX,
                y = arcY,
                width = nodeWidth,
                height = nodeHeight,
                clipHorizontal = node.clipHorizontal,
                clipVertical = node.clipVertical,
                radiusTopStart = node.clipRadiusTopStart,
                radiusTopEnd = node.clipRadiusTopEnd,
                radiusBottomEnd = node.clipRadiusBottomEnd,
                radiusBottomStart = node.clipRadiusBottomStart
            )
        }

        renderBackgroundAndBorder(node, arcX, arcY, nodeWidth, nodeHeight, effectiveAlpha)
        renderText(node, arcX, arcY, nodeWidth, nodeHeight, effectiveAlpha)

        // Duyệt con bằng fastForEach (Zero-GC inline), bù trừ độ cuộn (scrollX, scrollY)
        val childParentLocalX = nodeLocalX - node.scrollX
        val childParentLocalY = nodeLocalY - node.scrollY
        node.children.fastForEach { child ->
            renderNodeRecursive(
                node = child,
                parentLocalX = childParentLocalX,
                parentLocalY = childParentLocalY,
                parentEffectiveAlpha = effectiveAlpha,
                viewX = viewX,
                viewY = viewY,
                viewHeight = viewHeight
            )
        }

        renderScrollbars(node, arcX, arcY, nodeWidth, nodeHeight, effectiveAlpha)

        if (node.clip) UIBatch.popClip()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. VISUAL TOKENS RENDERING: BACKGROUND & SDF BORDER
    // ─────────────────────────────────────────────────────────────────────────

    private fun renderBackgroundAndBorder(
        node: LayoutNode,
        arcX: Float,
        arcY: Float,
        nodeWidth: Float,
        nodeHeight: Float,
        effectiveAlpha: Float
    ) {
        if (node.backgroundColor == null && !node.hasBorder) return

        val backgroundColor = node.backgroundColor
        val fillColor = when {
            backgroundColor == null -> Color.clear
            effectiveAlpha < 1f -> colorScratch.set(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a * effectiveAlpha)
            else -> backgroundColor
        }

        val borderColor = when {
            !node.hasBorder -> Color.clear
            effectiveAlpha < 1f -> borderScratch.set(node.borderColor.r, node.borderColor.g, node.borderColor.b, node.borderColor.a * effectiveAlpha)
            else -> node.borderColor
        }

        UIBatch.drawBox(
            x = arcX,
            y = arcY,
            width = nodeWidth,
            height = nodeHeight,
            radiusTopStart = node.cornerRadiusTopStart,
            radiusTopEnd = node.cornerRadiusTopEnd,
            radiusBottomEnd = node.cornerRadiusBottomEnd,
            radiusBottomStart = node.cornerRadiusBottomStart,
            color = fillColor,
            borderWidth = if (node.hasBorder) node.borderWidth else 0f,
            borderColor = borderColor
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. BMFONT TEXT AUTO-CLIP & RENDERING
    // ─────────────────────────────────────────────────────────────────────────

    private fun renderText(
        node: LayoutNode,
        arcX: Float,
        arcY: Float,
        nodeWidth: Float,
        nodeHeight: Float,
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
            val innerHeight = maxOf(0f, nodeHeight - node.paddingTop - node.paddingBottom)
            val innerWidth = maxOf(0f, nodeWidth - node.paddingLeft - node.paddingRight)
            if (innerWidth <= MIN_RENDER_EPSILON || innerHeight <= MIN_RENDER_EPSILON) return
            val drawX = arcX + node.paddingLeft
            val drawY = if (innerHeight > capHeight) (arcY + node.paddingBottom) + (innerHeight + capHeight) * 0.5f else arcY + nodeHeight - node.paddingTop

            // Tự động cắt gọt (auto-clip) nội dung văn bản theo khung đệm nội tại của node
            val textClipX = arcX + node.paddingLeft
            val textClipY = arcY + node.paddingBottom
            UIBatch.pushClip(
                x = textClipX,
                y = textClipY,
                width = innerWidth,
                height = innerHeight
            )

            UIBatch.drawText(
                font = font,
                text = textContent,
                x = drawX,
                y = drawY,
                targetWidth = innerWidth,
                color = textColor
            )

            UIBatch.popClip()
        } catch (_: Throwable) {
            // Headless test environment fallback
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. SCROLLBAR VISUALIZATION: VERTICAL & HORIZONTAL
    // ─────────────────────────────────────────────────────────────────────────

    private fun renderScrollbars(
        node: LayoutNode,
        arcX: Float,
        arcY: Float,
        nodeWidth: Float,
        nodeHeight: Float,
        effectiveAlpha: Float
    ) {
        renderVerticalScrollbar(node, arcX, arcY, nodeWidth, nodeHeight, effectiveAlpha)
        renderHorizontalScrollbar(node, arcX, arcY, nodeWidth, nodeHeight, effectiveAlpha)
    }

    private fun renderVerticalScrollbar(
        node: LayoutNode,
        arcX: Float,
        arcY: Float,
        nodeWidth: Float,
        nodeHeight: Float,
        effectiveAlpha: Float
    ) {
        if (!node.isScrollableVertical || node.maxScrollY <= MIN_RENDER_EPSILON || nodeHeight <= MIN_SCROLLBAR_CONTAINER_LENGTH) return

        val thumbHeight = maxOf(MIN_SCROLLBAR_THUMB_LENGTH, (nodeHeight / (nodeHeight + node.maxScrollY)) * nodeHeight)
        val availableTrack = nodeHeight - thumbHeight
        val progress = (node.scrollY / node.maxScrollY).coerceIn(0f, 1f)
        val thumbTop = progress * availableTrack
        val arcThumbY = arcY + nodeHeight - thumbTop - thumbHeight
        val scrollbarWidth = SCROLLBAR_THICKNESS
        val arcScrollbarX = arcX + nodeWidth - scrollbarWidth - SCROLLBAR_MARGIN

        scrollbarScratch.set(1f, 1f, 1f, SCROLLBAR_ALPHA * effectiveAlpha)
        UIBatch.drawBox(
            x = arcScrollbarX,
            y = arcThumbY,
            width = scrollbarWidth,
            height = thumbHeight,
            radius = SCROLLBAR_RADIUS,
            color = scrollbarScratch
        )
    }

    private fun renderHorizontalScrollbar(
        node: LayoutNode,
        arcX: Float,
        arcY: Float,
        nodeWidth: Float,
        nodeHeight: Float,
        effectiveAlpha: Float
    ) {
        if (!node.isScrollableHorizontal || node.maxScrollX <= MIN_RENDER_EPSILON || nodeWidth <= MIN_SCROLLBAR_CONTAINER_LENGTH) return

        val thumbWidth = maxOf(MIN_SCROLLBAR_THUMB_LENGTH, (nodeWidth / (nodeWidth + node.maxScrollX)) * nodeWidth)
        val availableTrack = nodeWidth - thumbWidth
        val progress = (node.scrollX / node.maxScrollX).coerceIn(0f, 1f)
        val thumbLeft = progress * availableTrack
        val arcThumbX = arcX + thumbLeft
        val scrollbarHeight = SCROLLBAR_THICKNESS
        val arcScrollbarY = arcY + SCROLLBAR_MARGIN

        scrollbarScratch.set(1f, 1f, 1f, SCROLLBAR_ALPHA * effectiveAlpha)
        UIBatch.drawBox(
            x = arcThumbX,
            y = arcScrollbarY,
            width = thumbWidth,
            height = scrollbarHeight,
            radius = SCROLLBAR_RADIUS,
            color = scrollbarScratch
        )
    }
}
