package org.mdt.ui.widgets

import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import org.mdt.ui.core.PointerEvent
import org.mdt.ui.core.ScrollEvent
import org.mdt.ui.layout.GodotLayout
import org.mdt.ui.render.EngineRenderer

/**
 * ## ScrollContainerNode
 *
 * Smooth viewport container supporting mouse-wheel scrolling, drag scrolling,
 * hardware ScissorStack clipping, and sleek modern scrollbar indicators.
 *
 * See: docs/complex-challenges/complex_challenges_en.md
 */
open class ScrollContainerNode : BoxNode() {

    var scrollX: Float = 0f
    var scrollY: Float = 0f

    var enableHorizontal: Boolean = false
    var enableVertical: Boolean = true

    var scrollbarColor: Color = Color(Color.valueOf("60a5fa").a(0.55f))
    var scrollbarTrackColor: Color = Color(Color.valueOf("181926").a(0.3f))
    var scrollbarWidth: Float = 4f

    private var contentWidth: Float = 0f
    private var contentHeight: Float = 0f

    private var isDraggingScrollbar = false
    private var dragStartY = 0f
    private var dragStartScrollY = 0f

    init {
        clip = true

        onScroll = { event: ScrollEvent ->
            if (enableVertical && contentHeight > getAvailHeight()) {
                val maxScroll = contentHeight - getAvailHeight()
                scrollY = (scrollY + event.amountY * 28f).coerceIn(0f, maxScroll)
                event.isConsumed = true
                invalidateLayout()
            }
        }

        onPointerDown = { event: PointerEvent ->
            if (enableVertical && isOverScrollbar(event.x, event.y)) {
                isDraggingScrollbar = true
                dragStartY = event.y
                dragStartScrollY = scrollY
            }
        }

        onPointerUp = {
            isDraggingScrollbar = false
        }
    }

    private fun getAvailWidth(): Float = maxOf(0f, bounds.width - padL - padR)
    private fun getAvailHeight(): Float = maxOf(0f, bounds.height - padT - padB)

    private fun isOverScrollbar(px: Float, py: Float): Boolean {
        val barX = bounds.x + bounds.width - padR - scrollbarWidth - 2f
        return px in barX..(barX + scrollbarWidth + 4f) && py in bounds.y..(bounds.y + bounds.height)
    }

    override fun layout() {
        val w = if (bounds.width > 0f) bounds.width else getPrefWidth()
        val h = if (bounds.height > 0f) bounds.height else getPrefHeight()
        if (bounds.width != w || bounds.height != h) {
            setSize(w, h)
        }

        val availW = getAvailWidth()
        val availH = getAvailHeight()

        // 1. Measure total child content bounding size
        var maxChildW = 0f
        var maxChildH = 0f
        for (child in children) {
            if (!child.visible) continue
            maxChildW = maxOf(maxChildW, GodotLayout.getChildMinWidth(child))
            maxChildH = maxOf(maxChildH, GodotLayout.getChildMinHeight(child))
        }

        contentWidth = maxOf(availW, maxChildW)
        contentHeight = maxOf(availH, maxChildH)

        val maxScrollX = maxOf(0f, contentWidth - availW)
        val maxScrollY = maxOf(0f, contentHeight - availH)

        scrollX = scrollX.coerceIn(0f, maxScrollX)
        scrollY = scrollY.coerceIn(0f, maxScrollY)

        val innerX = bounds.x + padL - scrollX
        // In OpenGL coordinates (y=0 is bottom): scrolling down shifts content UP (+scrollY)
        val innerY = bounds.y + padB + scrollY

        for (child in children) {
            if (!child.visible) continue
            val anchor = child.anchorData
            if (anchor.anchorLeft != 0f || anchor.anchorTop != 0f || anchor.anchorRight != 0f || anchor.anchorBottom != 0f) {
                GodotLayout.layoutSingleAnchor(child, innerX, innerY, availW, availH)
            } else {
                GodotLayout.fitChildInRect(child, innerX, innerY, availW, availH)
            }
        }

        isLayoutDirty = false
        for (child in children) {
            if (child.visible) {
                child.layout()
            }
        }
    }

    override fun draw(renderer: EngineRenderer) {
        super.draw(renderer)

        // Draw vertical scrollbar if content overflows
        val availH = getAvailHeight()
        if (enableVertical && contentHeight > availH && availH > 0f) {
            val maxScroll = contentHeight - availH
            val thumbRatio = (availH / contentHeight).coerceIn(0.1f, 1.0f)
            val thumbHeight = availH * thumbRatio
            val scrollProgress = (scrollY / maxScroll).coerceIn(0f, 1f)

            // Calculate thumb position (OpenGL bottom-to-top)
            val trackY = bounds.y + padB
            val thumbY = trackY + (availH - thumbHeight) * (1f - scrollProgress)
            val thumbX = bounds.x + bounds.width - padR - scrollbarWidth - 2f

            // Draw track
            Draw.color(scrollbarTrackColor)
            Fill.rect(thumbX + scrollbarWidth * 0.5f, trackY + availH * 0.5f, scrollbarWidth, availH)

            // Draw thumb
            Draw.color(scrollbarColor)
            Fill.rect(thumbX + scrollbarWidth * 0.5f, thumbY + thumbHeight * 0.5f, scrollbarWidth, thumbHeight)
            Draw.color(Color.white)
        }
    }
}
