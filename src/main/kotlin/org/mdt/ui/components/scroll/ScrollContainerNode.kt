package org.mdt.ui.components.scroll

import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.math.Mathf
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.core.PointerEvent
import org.mdt.ui.core.ScrollEvent
import org.mdt.ui.render.EngineRenderer

/**
 * ## ScrollContainerNode
 *
 * Virtual DOM container managing a scrollable viewport with inertia,
 * mouse drag, mouse wheel scrolling, and hardware Scissor clipping.
 *
 * See: docs/complex-challenges/complex_challenges_en.md
 */
open class ScrollContainerNode : LayoutNode() {

    var scrollX: Float = 0f
    var scrollY: Float = 0f

    var enableVertical: Boolean = true
    var enableHorizontal: Boolean = false

    var maxScrollX: Float = 0f
        private set
    var maxScrollY: Float = 0f
        private set

    var scrollbarColor: Color = Color(Color.valueOf("5b6078").a(0.6f))
    var scrollbarThickness: Float = 4f

    private var isDragging = false
    private var lastDragX = 0f
    private var lastDragY = 0f

    init {
        clip = true

        onScroll = { event: ScrollEvent ->
            var consumed = false
            if (enableVertical && maxScrollY > 0f) {
                scrollY = (scrollY - event.amountY * 24f).coerceIn(0f, maxScrollY)
                consumed = true
            }
            if (enableHorizontal && maxScrollX > 0f) {
                scrollX = (scrollX + event.amountX * 24f).coerceIn(0f, maxScrollX)
                consumed = true
            }
            if (consumed) {
                invalidateLayout()
            }
            consumed
        }

        onPointerDown = { event: PointerEvent ->
            isDragging = true
            lastDragX = event.x
            lastDragY = event.y
        }

        onPointerUp = {
            isDragging = false
        }
    }

    override fun layout() {
        val w = if (bounds.width > 0f) bounds.width else getPrefWidth()
        val h = if (bounds.height > 0f) bounds.height else getPrefHeight()
        if (bounds.width != w || bounds.height != h) {
            setSize(w, h)
        }

        val availW = maxOf(0f, bounds.width - padL - padR)
        val availH = maxOf(0f, bounds.height - padT - padB)
        val innerX = bounds.x + padL - scrollX
        val innerY = bounds.y + padB + scrollY

        measurePolicy.layout(this, innerX, innerY, availW, availH)

        var totalW = 0f
        var totalH = 0f
        for (child in children) {
            if (child.visible) {
                child.layout()
                totalW = maxOf(totalW, child.bounds.x + child.bounds.width - innerX)
                totalH = maxOf(totalH, child.bounds.y + child.bounds.height - innerY)
            }
        }

        maxScrollX = maxOf(0f, totalW - availW)
        maxScrollY = maxOf(0f, totalH - availH)
        scrollX = scrollX.coerceIn(0f, maxScrollX)
        scrollY = scrollY.coerceIn(0f, maxScrollY)

        isLayoutDirty = false
    }

    override fun draw(renderer: EngineRenderer) {
        super.draw(renderer)

        // Draw modern slim scrollbars
        val availW = bounds.width - padL - padR
        val availH = bounds.height - padT - padB
        val innerX = bounds.x + padL
        val innerY = bounds.y + padB

        if (enableVertical && maxScrollY > 0f) {
            val contentH = availH + maxScrollY
            val thumbH = maxOf(16f, (availH / contentH) * availH)
            val scrollRatio = if (maxScrollY > 0f) scrollY / maxScrollY else 0f
            val thumbY = innerY + availH - thumbH - scrollRatio * (availH - thumbH)

            Draw.color(scrollbarColor)
            Fill.rect(
                innerX + availW - scrollbarThickness * 0.5f - 2f,
                thumbY + thumbH * 0.5f,
                scrollbarThickness,
                thumbH
            )
            Draw.color(Color.white)
        }

        if (enableHorizontal && maxScrollX > 0f) {
            val contentW = availW + maxScrollX
            val thumbW = maxOf(16f, (availW / contentW) * availW)
            val scrollRatio = if (maxScrollX > 0f) scrollX / maxScrollX else 0f
            val thumbX = innerX + scrollRatio * (availW - thumbW)

            Draw.color(scrollbarColor)
            Fill.rect(
                thumbX + thumbW * 0.5f,
                innerY + scrollbarThickness * 0.5f + 2f,
                thumbW,
                scrollbarThickness
            )
            Draw.color(Color.white)
        }
    }
}
