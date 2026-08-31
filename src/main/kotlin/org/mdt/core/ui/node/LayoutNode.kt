// [AGENT ARCHITECTURE & INVARIANTS]
// - Domain Role: Measure & Layout Virtual DOM Node with Visual Background & Scroll Support.
// - Operating Mechanism: Delegates sizing to [MeasurePolicy] ([Row], [Column], [Box]); manages padding, borders, corner radii, frosted glass, and floating scrollbars.
// - Invariants: Intrinsic auto-layout (HUG CONTENT) by default; Zero-GC measure/layout loops.
// - Dependencies: [MeasurePolicy], [BoxMeasurePolicy], [UIBatch], [ScrollbarStyle], [UINode].
// - Directive: Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.node

import arc.graphics.g2d.TextureRegion
import org.mdt.core.platform.render.UIBatch
import org.mdt.core.ui.unit.Color
import org.mdt.core.ui.input.PointerEvent
import org.mdt.core.ui.input.PointerEventPass
import org.mdt.core.ui.input.PointerEventType
import org.mdt.core.ui.input.PointerInputFilter
import org.mdt.core.ui.input.ScrollEvent
import org.mdt.core.ui.layout.BoxMeasurePolicy
import org.mdt.core.ui.layout.MeasurePolicy
import org.mdt.ui.theme.ScrollbarStyle

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

    // Scrollbar Visual Style Configuration
    var scrollbarStyle: ScrollbarStyle = ScrollbarStyle.Default

    private var lastActivityTime: Long = 0L
    private var lastDragX: Float = 0.0f
    private var lastDragY: Float = 0.0f
    private var isDraggingPointer: Boolean = false

    private val scrollPointerFilter = object : PointerInputFilter {
        override fun onPointerEvent(event: PointerEvent, pass: PointerEventPass, node: UINode) {
            if (!scrollable) return
            if (pass == PointerEventPass.MAIN) {
                when (event.type) {
                    PointerEventType.Press -> {
                        lastDragX = event.x
                        lastDragY = event.y
                        isDraggingPointer = true
                        lastActivityTime = host.system.nowMillis()
                    }

                    PointerEventType.Drag -> {
                        if (isDraggingPointer) {
                            var consumed = false
                            lastActivityTime = host.system.nowMillis()

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
                    }

                    PointerEventType.Release, PointerEventType.Cancel -> {
                        isDraggingPointer = false
                        lastActivityTime = host.system.nowMillis()
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun attachScrollPointerHandlers() {
        if (!pointerFilters.contains(scrollPointerFilter)) {
            pointerFilters.add(scrollPointerFilter)
        }
    }

    private fun detachScrollPointerHandlers() {
        pointerFilters.remove(scrollPointerFilter)
        isDraggingPointer = false
    }

    fun handleScrollEvent(event: ScrollEvent): Boolean {
        if (!scrollable) return false
        var consumed = false
        val isShift = host.input.isShiftPressed
        lastActivityTime = host.system.nowMillis()
        val speed = scrollbarStyle.scrollSpeed

        val isHorizontalOnly = isShift || (!enableVerticalScroll && enableHorizontalScroll)
        when {
            isHorizontalOnly -> {
                if (enableHorizontalScroll && maxScrollX > 0.0f) {
                    val delta = if (event.amountX != 0.0f) event.amountX else event.amountY
                    scrollX = (scrollX + delta * speed).coerceIn(0.0f, maxScrollX)
                    consumed = true
                }
            }

            else -> {
                if (enableVerticalScroll && maxScrollY > 0.0f) {
                    scrollY = (scrollY + event.amountY * speed).coerceIn(0.0f, maxScrollY)
                    consumed = true
                }
                if (enableHorizontalScroll && maxScrollX > 0.0f && event.amountX != 0.0f) {
                    scrollX = (scrollX + event.amountX * speed).coerceIn(0.0f, maxScrollX)
                    consumed = true
                }
            }
        }

        if (consumed) {
            event.isConsumed = true
            invalidateLayout()
        }
        return consumed
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
        if (parent == null) {
            val computedWidth = when {
                width >= 0.0f -> width
                else -> getPrefWidth()
            }
            val computedHeight = when {
                height >= 0.0f -> height
                else -> getPrefHeight()
            }

            if (bounds.width != computedWidth || bounds.height != computedHeight) {
                setSize(computedWidth, computedHeight)
            }
        }

        val availableWidth = maxOf(0.0f, bounds.width - padL - padR)
        val availableHeight = maxOf(0.0f, bounds.height - padT - padB)

        if (scrollable) {
            val contentWidth = measurePolicy.measureWidth(this)
            val contentHeight = measurePolicy.measureHeight(this, availableWidth)

            maxScrollX = maxOf(0.0f, contentWidth - availableWidth)
            maxScrollY = maxOf(0.0f, contentHeight - availableHeight)

            if (!enableHorizontalScroll) {
                scrollX = 0.0f
                maxScrollX = 0.0f
            }
            if (!enableVerticalScroll) {
                scrollY = 0.0f
                maxScrollY = 0.0f
            }
            scrollX = scrollX.coerceIn(0.0f, maxScrollX)
            scrollY = scrollY.coerceIn(0.0f, maxScrollY)
        }

        val innerX = if (scrollable) bounds.x + padL - scrollX else bounds.x + padL
        val innerY = if (scrollable) bounds.y + padB + scrollY else bounds.y + padB
        measurePolicy.layout(this, innerX, innerY, availableWidth, availableHeight)
        isLayoutDirty = false

        for (i in children.indices) {
            val child = children[i]
            child.layout()
        }
    }

    override fun resetModifiers() {
        super.resetModifiers()
        color = Color.Clear
        borderColor = Color.Clear
        borderWidth = 0.0f
        radius = 0.0f
        region = null
        isGlass = false
        shadowRadius = 0.0f
        shadowColor = Color.Clear
        shadowOffsetX = 0.0f
        shadowOffsetY = 0.0f
    }

    // --- DRAWING & SCROLLBAR RENDERING ---

    override fun draw(batch: UIBatch) {
        if (!visible) return

        if (clip || scrollable) {
            val innerX = bounds.x + padL
            val innerY = bounds.y + padB
            val innerWidth = maxOf(0.0f, bounds.width - padL - padR)
            val innerHeight = maxOf(0.0f, bounds.height - padT - padB)

            drawSelf(batch)

            batch.pushClip(innerX, innerY, innerWidth, innerHeight)
            drawChildren(batch)
            batch.popClip()
        } else {
            drawSelf(batch)
            drawChildren(batch)
        }

        if (scrollable) {
            drawScrollbars(batch)
        }
    }

    override fun drawSelf(batch: UIBatch) {
        // 1. Draw optional drop shadow behind node background
        if (shadowRadius > 0.001f && shadowColor.alpha > 0.001f) {
            val shadowExpansion = shadowRadius * 0.5f
            batch.drawBox(
                x = bounds.x + shadowOffsetX - shadowExpansion,
                y = bounds.y + shadowOffsetY - shadowExpansion,
                width = bounds.width + shadowExpansion * 2.0f,
                height = bounds.height + shadowExpansion * 2.0f,
                radius = radius + shadowExpansion,
                color = shadowColor,
            )
        }

        // 2. Draw node background / borders / glass
        if (color.alpha > 0.001f || borderWidth > 0.001f || region != null || isGlass) {
            batch.drawBox(
                x = bounds.x,
                y = bounds.y,
                width = bounds.width,
                height = bounds.height,
                region = region,
                radius = radius,
                color = color,
                borderWidth = borderWidth,
                borderColor = borderColor,
                isGlass = isGlass,
            )
        }
    }

    private fun drawScrollbars(batch: UIBatch) {
        val currentTime = host.system.nowMillis()
        val timeSinceActivity = currentTime - lastActivityTime

        val autoHide = scrollbarStyle.autoHide
        val idleTimeoutMs = scrollbarStyle.idleTimeoutMs
        val fadeDurationMs = scrollbarStyle.fadeDurationMs
        val thickness = scrollbarStyle.thickness
        val radius = scrollbarStyle.radius
        val thumbColor = scrollbarStyle.thumbColor
        val trackColor = scrollbarStyle.trackColor

        val scrollbarAlpha: Float = when {
            !autoHide || isDraggingPointer || timeSinceActivity < idleTimeoutMs -> 1.0f
            else -> (1.0f - (timeSinceActivity - idleTimeoutMs).toFloat() / fadeDurationMs.toFloat()).coerceIn(
                0.0f,
                1.0f,
            )
        }

        if (scrollbarAlpha <= 0.001f) return

        val availableWidth = maxOf(0.0f, bounds.width - padL - padR)
        val availableHeight = maxOf(0.0f, bounds.height - padT - padB)
        val innerX = bounds.x + padL
        val innerY = bounds.y + padB
        val effectiveThumbColor = thumbColor.withAlpha(thumbColor.alpha * scrollbarAlpha)

        // 1. Draw Vertical Floating Scrollbar
        if (enableVerticalScroll && maxScrollY > 0.0f) {
            val contentHeight = availableHeight + maxScrollY
            val thumbHeight = maxOf(20.0f, (availableHeight / contentHeight) * availableHeight)
            val scrollRatio = if (maxScrollY > 0.0f) scrollY / maxScrollY else 0.0f
            val thumbY = innerY + availableHeight - thumbHeight - scrollRatio * (availableHeight - thumbHeight)
            val thumbX = innerX + availableWidth - thickness - 2.0f

            if (trackColor.alpha > 0.001f) {
                batch.drawBox(
                    x = thumbX,
                    y = innerY,
                    width = thickness,
                    height = availableHeight,
                    color = trackColor.withAlpha(trackColor.alpha * scrollbarAlpha),
                    radius = radius,
                )
            }

            batch.drawBox(
                x = thumbX,
                y = thumbY,
                width = thickness,
                height = thumbHeight,
                color = effectiveThumbColor,
                radius = radius,
            )
        }

        // 2. Draw Horizontal Floating Scrollbar
        if (enableHorizontalScroll && maxScrollX > 0.0f) {
            val contentWidth = maxOf(0.001f, availableWidth + maxScrollX)
            val thumbWidth = maxOf(24.0f, (availableWidth / contentWidth) * availableWidth)
            val scrollRatio = if (maxScrollX > 0.0f) scrollX / maxScrollX else 0.0f
            val thumbX = innerX + scrollRatio * (availableWidth - thumbWidth)
            val thumbY = innerY + 2.0f

            if (trackColor.alpha > 0.001f) {
                batch.drawBox(
                    x = innerX,
                    y = thumbY,
                    width = availableWidth,
                    height = thickness,
                    color = trackColor.withAlpha(trackColor.alpha * scrollbarAlpha),
                    radius = radius,
                )
            }

            batch.drawBox(
                x = thumbX,
                y = thumbY,
                width = thumbWidth,
                height = thickness,
                color = effectiveThumbColor,
                radius = radius,
            )
        }
    }
}
