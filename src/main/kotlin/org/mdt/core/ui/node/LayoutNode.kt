// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.node

import arc.graphics.g2d.TextureRegion
import org.mdt.core.ui.EngineRuntime
import org.mdt.core.ui.input.PointerEvent
import org.mdt.core.ui.input.ScrollEvent
import org.mdt.core.ui.layout.BoxMeasurePolicy
import org.mdt.core.ui.layout.MeasurePolicy
import org.mdt.core.platform.render.UIBatch
import org.mdt.core.ui.unit.Color

/**
 * ## LayoutNode
 *
 * Core layout container node delegating sizing and layout to [MeasurePolicy] strategies ([BoxMeasurePolicy], [RowMeasurePolicy], etc.).
 * Supports visual styling (background, border, corner radius, drop-shadow, frosted glass) and native 2D scrolling.
 *
 * @property measurePolicy Active measurement and layout strategy for arranging children.
 * @property radius Corner radius in pixels for rounded SDF rendering.
 * @property color Background fill color.
 * @property borderWidth Outline border stroke width in pixels.
 * @property borderColor Outline border stroke color.
 * @property isGlass Whether Dual-Kawase frosted glass background blur sampling is active.
 * @property region Texture region rendered inside container bounds.
 * @property shadowRadius Drop-shadow blur radius in pixels.
 * @property shadowColor Drop-shadow color and opacity.
 * @property shadowOffsetX Horizontal drop-shadow offset in pixels.
 * @property shadowOffsetY Vertical drop-shadow offset in pixels.
 * @property scrollable Whether interactive 2D scrolling and viewport clipping are enabled.
 * @property enableVerticalScroll Whether vertical scroll gesture is enabled.
 * @property enableHorizontalScroll Whether horizontal scroll gesture is enabled.
 * @property scrollX Current horizontal scroll offset in pixels.
 * @property scrollY Current vertical scroll offset in pixels.
 * @property maxScrollX Maximum horizontal scrollable overflow range in pixels.
 * @property maxScrollY Maximum vertical scrollable overflow range in pixels.
 *
 * @see MeasurePolicy
 * @see BoxMeasurePolicy
 * @see UINode
 * @see InputNode
 */
open class LayoutNode : UINode() {

    // --- PROPERTIES & VISUAL STYLING (Unstyled by default) ---

    var measurePolicy: MeasurePolicy = BoxMeasurePolicy
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    // Direct visual styling fields (Set strictly via UIModifiers or explicit composable properties)
    var radius: Float = 0.0f
    var color: Color = Color.Clear
    var borderWidth: Float = 0.0f
    var borderColor: Color = Color.Clear
    var isGlass: Boolean = false
    var region: TextureRegion? = null

    // Drop Shadow Properties
    var shadowRadius: Float = 0.0f
    var shadowColor: Color = Color.Clear
    var shadowOffsetX: Float = 0.0f
    var shadowOffsetY: Float = 0.0f

    // --- NATIVE SCROLLING & FLOATING SCROLLBAR STATE ---

    /** Whether scrolling is active for this container. */
    var scrollable: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
                if (value) {
                    attachScrollPointerHandlers()
                } else {
                    detachScrollPointerHandlers()
                }
            }
        }

    var enableVerticalScroll: Boolean = true
    var enableHorizontalScroll: Boolean = false

    var scrollX: Float = 0.0f
    var scrollY: Float = 0.0f

    var maxScrollX: Float = 0.0f
        private set
    var maxScrollY: Float = 0.0f
        private set

    // Scrollbar Visual Style Tokens
    var scrollbarThumbColor: Color = Color(0.65f, 0.65f, 0.68f, 0.50f)
    var scrollbarTrackColor: Color = Color.Clear
    var scrollbarThickness: Float = 4.0f
    var scrollbarRadius: Float = 2.0f
    var scrollbarAutoHide: Boolean = true
    var scrollbarIdleTimeoutMs: Long = 1200L
    var scrollbarFadeDurationMs: Long = 350L
    var scrollSpeed: Float = 36.0f

    private var lastActivityTime: Long = 0L
    private var lastDragX: Float = 0.0f
    private var lastDragY: Float = 0.0f
    private var isDraggingPointer: Boolean = false

    // --- SCROLL POINTER HANDLER LIFECYCLE ---

    private fun attachScrollPointerHandlers() {
        onScroll = { event: ScrollEvent ->
            var consumed = false
            val isShift = EngineRuntime.host.isShiftPressed
            lastActivityTime = EngineRuntime.host.nowMillis()

            val isHorizontalOnly = isShift || (!enableVerticalScroll && enableHorizontalScroll)
            when {
                isHorizontalOnly -> {
                    if (enableHorizontalScroll && maxScrollX > 0.0f) {
                        val delta = if (event.amountX != 0.0f) event.amountX else event.amountY
                        scrollX = (scrollX + delta * scrollSpeed).coerceIn(0.0f, maxScrollX)
                        consumed = true
                    }
                }
                else -> {
                    if (enableVerticalScroll && maxScrollY > 0.0f) {
                        scrollY = (scrollY + event.amountY * scrollSpeed).coerceIn(0.0f, maxScrollY)
                        consumed = true
                    }
                    if (enableHorizontalScroll && maxScrollX > 0.0f && event.amountX != 0.0f) {
                        scrollX = (scrollX + event.amountX * scrollSpeed).coerceIn(0.0f, maxScrollX)
                        consumed = true
                    }
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
            lastActivityTime = EngineRuntime.host.nowMillis()
        }

        onPointerDrag = { event: PointerEvent ->
            var consumed = false
            lastActivityTime = EngineRuntime.host.nowMillis()

            if (enableVerticalScroll && maxScrollY > 0.0f) {
                val deltaY = event.y - lastDragY
                scrollY = (scrollY + deltaY).coerceIn(0.0f, maxScrollY)
                lastDragY = event.y
                consumed = true
            }
            if (enableHorizontalScroll && maxScrollX > 0.0f) {
                val deltaX = event.x - lastDragX
                scrollX = (scrollX - deltaX).coerceIn(0.0f, maxScrollX)
                lastDragX = event.x
                consumed = true
            }

            if (consumed) {
                event.consume()
                invalidateLayout()
            }
        }

        onPointerUp = {
            isDraggingPointer = false
            lastActivityTime = EngineRuntime.host.nowMillis()
        }
    }

    private fun detachScrollPointerHandlers() {
        onScroll = null
        onPointerDown = null
        onPointerDrag = null
        onPointerUp = null
    }

    override fun buildHitPath(pointX: Float, pointY: Float, path: ArrayList<UINode>): Boolean {
        if ((clip || scrollable) && !bounds.contains(pointX, pointY)) return false
        return super.buildHitPath(pointX, pointY, path)
    }

    override fun hitTest(pointX: Float, pointY: Float): UINode? {
        if ((clip || scrollable) && !bounds.contains(pointX, pointY)) return null
        return super.hitTest(pointX, pointY)
    }

    // --- SIZING & INTRINSIC MEASUREMENT ---

    override fun getPrefWidth(): Float {
        if (width >= 0.0f) return width

        val measuredContentWidth = measurePolicy.measureWidth(this)
        val baseWidth = if (minWidth >= 0.0f) maxOf(measuredContentWidth, minWidth) else measuredContentWidth
        return baseWidth + padL + padR
    }

    override fun getPrefHeight(availableWidth: Float): Float {
        if (height >= 0.0f) return height

        val availableInnerWidth = when {
            availableWidth >= 0.0f -> maxOf(0.0f, availableWidth - padL - padR)
            width >= 0.0f -> maxOf(0.0f, width - padL - padR)
            bounds.width > 0.0f -> maxOf(0.0f, bounds.width - padL - padR)
            else -> -1.0f
        }

        val measuredContentHeight = measurePolicy.measureHeight(this, availableInnerWidth)
        val baseHeight = if (minHeight >= 0.0f) maxOf(measuredContentHeight, minHeight) else measuredContentHeight
        return baseHeight + padT + padB
    }

    // --- LAYOUT ENGINE ---

    override fun layout() {
        val computedWidth = when {
            width >= 0.0f -> width
            bounds.width > 0.0f -> bounds.width
            else -> getPrefWidth()
        }
        val computedHeight = when {
            height >= 0.0f -> height
            bounds.height > 0.0f -> bounds.height
            else -> getPrefHeight()
        }

        if (bounds.width != computedWidth || bounds.height != computedHeight) {
            setSize(computedWidth, computedHeight)
        }

        val availableWidth = maxOf(0.0f, bounds.width - padL - padR)
        val availableHeight = maxOf(0.0f, bounds.height - padT - padB)

        if (scrollable) {
            val contentWidth = measurePolicy.measureWidth(this)
            val contentHeight = measurePolicy.measureHeight(this, availableWidth)

            maxScrollX = maxOf(0.0f, contentWidth - availableWidth)
            maxScrollY = maxOf(0.0f, contentHeight - availableHeight)
            scrollX = scrollX.coerceIn(0.0f, maxScrollX)
            scrollY = scrollY.coerceIn(0.0f, maxScrollY)
        }

        val innerX = if (scrollable) bounds.x + padL - scrollX else bounds.x + padL
        val innerY = if (scrollable) bounds.y + padB + scrollY else bounds.y + padB
        measurePolicy.layout(this, innerX, innerY, availableWidth, availableHeight)

        isLayoutDirty = false
        for (i in children.indices) {
            val child = children[i]
            if (child.visible) child.layout()
        }
    }

    // --- RENDERING & FLOATING SCROLLBARS ---

    override fun draw() {
        if (!visible) return

        val shouldClip = (clip || scrollable) && bounds.width > 0.0f && bounds.height > 0.0f
        if (shouldClip) {
            val innerX = bounds.x + padL
            val innerY = bounds.y + padB
            val innerWidth = maxOf(0.0f, bounds.width - padL - padR)
            val innerHeight = maxOf(0.0f, bounds.height - padT - padB)
            UIBatch.pushClip(innerX, innerY, innerWidth, innerHeight)
        }

        drawSelf()
        drawChildren()

        if (shouldClip) {
            UIBatch.popClip()
        }

        if (scrollable) {
            drawScrollbars()
        }
    }

    override fun drawSelf() {
        if (color.alpha > 0.001f || borderWidth > 0.001f || region != null || isGlass) {
            UIBatch.drawBox(
                x = bounds.x,
                y = bounds.y,
                width = bounds.width,
                height = bounds.height,
                region = region,
                radius = radius,
                color = color,
                borderWidth = borderWidth,
                borderColor = borderColor,
                isGlass = isGlass
            )
        }
    }

    private fun drawScrollbars() {
        val currentTime = EngineRuntime.host.nowMillis()
        val timeSinceActivity = currentTime - lastActivityTime

        val scrollbarAlpha: Float = when {
            !scrollbarAutoHide || isDraggingPointer || timeSinceActivity < scrollbarIdleTimeoutMs -> 1.0f
            else -> (1.0f - (timeSinceActivity - scrollbarIdleTimeoutMs).toFloat() / scrollbarFadeDurationMs.toFloat()).coerceIn(0.0f, 1.0f)
        }

        if (scrollbarAlpha <= 0.001f) return

        val availableWidth = maxOf(0.0f, bounds.width - padL - padR)
        val availableHeight = maxOf(0.0f, bounds.height - padT - padB)
        val innerX = bounds.x + padL
        val innerY = bounds.y + padB
        val effectiveThumbColor = scrollbarThumbColor.withAlpha(scrollbarThumbColor.alpha * scrollbarAlpha)

        // 1. Draw Vertical Floating Scrollbar
        if (enableVerticalScroll && maxScrollY > 0.0f) {
            val contentHeight = availableHeight + maxScrollY
            val thumbHeight = maxOf(20.0f, (availableHeight / contentHeight) * availableHeight)
            val scrollRatio = if (maxScrollY > 0.0f) scrollY / maxScrollY else 0.0f
            val thumbY = innerY + availableHeight - thumbHeight - scrollRatio * (availableHeight - thumbHeight)
            val thumbX = innerX + availableWidth - scrollbarThickness - 2.0f

            if (scrollbarTrackColor.alpha > 0.001f) {
                UIBatch.drawBox(
                    x = thumbX,
                    y = innerY,
                    width = scrollbarThickness,
                    height = availableHeight,
                    color = scrollbarTrackColor.withAlpha(scrollbarTrackColor.alpha * scrollbarAlpha),
                    radius = scrollbarRadius
                )
            }

            UIBatch.drawBox(
                x = thumbX,
                y = thumbY,
                width = scrollbarThickness,
                height = thumbHeight,
                color = effectiveThumbColor,
                radius = scrollbarRadius
            )
        }

        // 2. Draw Horizontal Floating Scrollbar
        if (enableHorizontalScroll && maxScrollX > 0.0f) {
            val contentWidth = maxOf(0.001f, availableWidth + maxScrollX)
            val thumbWidth = maxOf(24.0f, (availableWidth / contentWidth) * availableWidth)
            val scrollRatio = if (maxScrollX > 0.0f) scrollX / maxScrollX else 0.0f
            val thumbX = innerX + scrollRatio * (availableWidth - thumbWidth)
            val thumbY = innerY + 2.0f

            if (scrollbarTrackColor.alpha > 0.001f) {
                UIBatch.drawBox(
                    x = innerX,
                    y = thumbY,
                    width = availableWidth,
                    height = scrollbarThickness,
                    color = scrollbarTrackColor.withAlpha(scrollbarTrackColor.alpha * scrollbarAlpha),
                    radius = scrollbarRadius
                )
            }

            UIBatch.drawBox(
                x = thumbX,
                y = thumbY,
                width = thumbWidth,
                height = scrollbarThickness,
                color = effectiveThumbColor,
                radius = scrollbarRadius
            )
        }
    }
}
