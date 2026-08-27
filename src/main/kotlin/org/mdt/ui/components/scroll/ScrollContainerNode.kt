package org.mdt.ui.components.scroll

import arc.util.Tmp
import org.mdt.core.ui.UINode
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.input.PointerEvent
import org.mdt.core.ui.input.ScrollEvent
import org.mdt.core.ui.render.BoxRenderer
import org.mdt.core.ui.render.EngineRenderer
import org.mdt.ui.components.layout.BackgroundFill
import org.mdt.ui.components.layout.BoxVisuals
import org.mdt.ui.components.layout.LayoutNode

/**
 * ## ScrollContainerNode
 *
 * Virtual DOM container managing a scrollable viewport with mouse wheel scrolling,
 * touch drag inertia, hardware scissor clipping, and smart hit testing.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
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

    var scrollbarColor: Color = Color.valueOf("8e8e93").withAlpha(0.50f)
    var scrollbarThickness: Float = 4f
    var autoHideScrollbars: Boolean = true

    private val scrollbarThumbVisuals = BoxVisuals().apply {
        background.mode = BackgroundFill.Mode.COLOR
        radii.set(2f)
    }

    private var lastDragX = 0f
    private var lastDragY = 0f
    private var lastActivityTime = 0L
    private var isDraggingPointer = false

    init {
        clip = true

        onScroll = { event: ScrollEvent ->
            var consumed = false
            val isShift = arc.Core.input != null && arc.Core.input.shift()
            lastActivityTime = arc.util.Time.millis()

            if (isShift || (!enableVertical && enableHorizontal)) {
                if (enableHorizontal && maxScrollX > 0f) {
                    val delta = if (event.amountX != 0f) event.amountX else event.amountY
                    scrollX = (scrollX + delta * 36f).coerceIn(0f, maxScrollX)
                    consumed = true
                }
            } else {
                if (enableVertical && maxScrollY > 0f) {
                    scrollY = (scrollY + event.amountY * 36f).coerceIn(0f, maxScrollY)
                    consumed = true
                }
                if (enableHorizontal && maxScrollX > 0f && event.amountX != 0f) {
                    scrollX = (scrollX + event.amountX * 36f).coerceIn(0f, maxScrollX)
                    consumed = true
                }
            }

            if (consumed) {
                event.isConsumed = true
                invalidateLayout()
            }
        }

        onPointerDown = { event: PointerEvent ->
            lastDragX = event.x
            lastDragY = event.y
            isDraggingPointer = true
            lastActivityTime = arc.util.Time.millis()
        }

        onPointerDrag = { event: PointerEvent ->
            var consumed = false
            lastActivityTime = arc.util.Time.millis()

            if (enableVertical && maxScrollY > 0f) {
                val dy = event.y - lastDragY
                scrollY = (scrollY + dy).coerceIn(0f, maxScrollY)
                lastDragY = event.y
                consumed = true
            }
            if (enableHorizontal && maxScrollX > 0f) {
                val dx = event.x - lastDragX
                scrollX = (scrollX - dx).coerceIn(0f, maxScrollX)
                lastDragX = event.x
                consumed = true
            }
            if (consumed) {
                event.isConsumed = true
                invalidateLayout()
            }
        }

        onPointerUp = {
            isDraggingPointer = false
            lastActivityTime = arc.util.Time.millis()
        }
    }

    override fun hitTest(px: Float, py: Float): UINode? {
        if (!visible || !touchable) return null
        // Scissor viewport boundary check: ignore touches outside scroll container bounds
        if (!bounds.contains(px, py)) return null

        for (i in children.indices.reversed()) {
            val child = children[i]
            val hit = child.hitTest(px, py)
            if (hit != null) return hit
        }

        return this
    }

    override fun layout() {
        val w = if (bounds.width > 0f) bounds.width else getPrefWidth()
        val h = if (bounds.height > 0f) bounds.height else getPrefHeight()
        if (bounds.width != w || bounds.height != h) setSize(w, h)

        val availW = maxOf(0f, bounds.width - padL - padR)
        val availH = maxOf(0f, bounds.height - padT - padB)

        val contentW = measurePolicy.measureWidth(this)
        val contentH = measurePolicy.measureHeight(this)

        maxScrollX = maxOf(0f, contentW - availW)
        maxScrollY = maxOf(0f, contentH - availH)
        scrollX = scrollX.coerceIn(0f, maxScrollX)
        scrollY = scrollY.coerceIn(0f, maxScrollY)

        val innerX = bounds.x + padL - scrollX
        val innerY = bounds.y + padB + scrollY

        measurePolicy.layout(this, innerX, innerY, availW, availH)

        for (child in children) {
            if (child.visible) child.layout()
        }

        isLayoutDirty = false
    }

    override fun draw(renderer: EngineRenderer) {
        super.draw(renderer)

        // Overlay auto-hiding floating scrollbars
        val currentTime = arc.util.Time.millis()
        val timeSinceActivity = currentTime - lastActivityTime
        val scrollbarAlpha: Float = if (autoHideScrollbars) {
            if (isDraggingPointer || timeSinceActivity < 1200L) {
                1.0f
            } else {
                (1.0f - ((timeSinceActivity - 1200L) / 350f)).coerceIn(0f, 1f)
            }
        } else {
            1.0f
        }

        if (scrollbarAlpha <= 0.001f) return

        val availW = bounds.width - padL - padR
        val availH = bounds.height - padT - padB
        val innerX = bounds.x + padL
        val innerY = bounds.y + padB
        val effectiveThumbColor = scrollbarColor.withAlpha(scrollbarColor.a * scrollbarAlpha)

        scrollbarThumbVisuals.background.color = effectiveThumbColor
        scrollbarThumbVisuals.radii.set(scrollbarThickness * 0.5f)

        if (enableVertical && maxScrollY > 0f) {
            val contentH = availH + maxScrollY
            val thumbH = maxOf(20f, (availH / contentH) * availH)
            val scrollRatio = if (maxScrollY > 0f) scrollY / maxScrollY else 0f
            val thumbY = innerY + availH - thumbH - scrollRatio * (availH - thumbH)
            val trackX = innerX + availW - scrollbarThickness - 2f

            BoxRenderer.draw(trackX, thumbY, scrollbarThickness, thumbH, scrollbarThumbVisuals)
        }

        if (enableHorizontal && maxScrollX > 0f) {
            val contentW = maxOf(0.001f, availW + maxScrollX)
            val thumbW = maxOf(24f, (availW / contentW) * availW)
            val scrollRatio = if (maxScrollX > 0f) scrollX / maxScrollX else 0f
            val thumbX = innerX + scrollRatio * (availW - thumbW)
            val trackY = innerY + 2f

            BoxRenderer.draw(thumbX, trackY, thumbW, scrollbarThickness, scrollbarThumbVisuals)
        }
    }
}
