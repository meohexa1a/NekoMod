package org.mdt.core.ui.node

import arc.graphics.g2d.TextureRegion
import org.mdt.core.ui.EngineRuntime
import org.mdt.core.ui.input.PointerEvent
import org.mdt.core.ui.input.ScrollEvent
import org.mdt.core.ui.layout.BoxMeasurePolicy
import org.mdt.core.ui.layout.MeasurePolicy
import org.mdt.core.ui.render.UIBatch
import org.mdt.core.ui.unit.Color

/**
 * ## LayoutNode [Primitive Container Virtual DOM Node with Native Scrolling]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Foundational container Virtual DOM node unifying flex/box layout delegation, GPU batch rendering, and native scrolling.
 * - Delegates intrinsic sizing and child positioning to interchangeable [MeasurePolicy] strategies ([BoxMeasurePolicy], [ColumnMeasurePolicy], [RowMeasurePolicy], [GridMeasurePolicy]).
 * - Supports built-in 2D scrolling ([scrollable], [enableVerticalScroll], [enableHorizontalScroll]):
 *   - Auto-clips children to inner padding bounds via [UIBatch.pushClip].
 *   - Translates child layout origin seamlessly: `(innerX - scrollX, innerY + scrollY)`.
 *   - Features auto-hiding floating scrollbars with smooth alpha fade-out and customizable styling.
 *   - Supports mouse wheel scrolling (with Shift key for horizontal) and touch drag gestures.
 * - Employs [UIBatch.drawBox] for 1-Draw-Call rendering of backgrounds, rounded SDF corners, borders, textures, and frosted glass.
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Zero-Cost when Inactive):** When [scrollable] is false, scrolling math and clipping incur zero overhead.
 * - **Rule 2 (OpenGL Bottom-Left Math):** Caret, viewport, and scrollbar thumb origins map strictly to bottom-left coordinates.
 * - **Rule 3 (Scissor Boundary Hit-Testing):** Touches outside scrollable viewport boundaries are rejected in [hitTest].
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - 🎨 **Composable Containers:** `src/main/kotlin/org/mdt/ui/components/layout/Box.kt`, `src/main/kotlin/org/mdt/ui/components/layout/ScrollBox.kt`
 * - 🎨 **Scrollbar Tokens:** `src/main/kotlin/org/mdt/ui/theme/ScrollbarStyle.kt`
 * - 📐 **Measure Policies:** `src/main/kotlin/org/mdt/core/ui/layout/MeasurePolicy.kt`
 * - ⚡ **GPU Batcher:** `src/main/kotlin/org/mdt/core/ui/render/UIBatch.kt`
 * - 🔌 **Platform Host:** `src/main/kotlin/org/mdt/core/engine/PlatformHost.kt`
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] Changing `measurePolicy` or scroll properties automatically invalidates node layout.
 * - [x] `layout()` calculates inner available dimensions and updates `maxScrollX` / `maxScrollY`.
 * - [x] `draw()` applies scissor clipping and renders floating scrollbar thumb when active.
 * - [x] Mouse wheel events and pointer drag gestures update scroll offsets within `[0..maxScroll]`.
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

    // --- NATIVE SCROLLING & FLOATING SCROLLBAR STATE ---

    /** Whether scrolling is active for this container. */
    var scrollable: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
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

    init {
        onScroll = { event: ScrollEvent ->
            if (scrollable) {
                var consumed = false
                val isShift = EngineRuntime.host.isShiftPressed
                lastActivityTime = EngineRuntime.host.nowMillis()

                if (isShift || (!enableVerticalScroll && enableHorizontalScroll)) {
                    if (enableHorizontalScroll && maxScrollX > 0.0f) {
                        val delta = if (event.amountX != 0.0f) event.amountX else event.amountY
                        scrollX = (scrollX + delta * scrollSpeed).coerceIn(0.0f, maxScrollX)
                        consumed = true
                    }
                } else {
                    if (enableVerticalScroll && maxScrollY > 0.0f) {
                        scrollY = (scrollY + event.amountY * scrollSpeed).coerceIn(0.0f, maxScrollY)
                        consumed = true
                    }
                    if (enableHorizontalScroll && maxScrollX > 0.0f && event.amountX != 0.0f) {
                        scrollX = (scrollX + event.amountX * scrollSpeed).coerceIn(0.0f, maxScrollX)
                        consumed = true
                    }
                }

                if (consumed) {
                    event.isConsumed = true
                    invalidateLayout()
                }
            }
        }

        onPointerDown = { event: PointerEvent ->
            if (scrollable) {
                lastDragX = event.x
                lastDragY = event.y
                isDraggingPointer = true
                lastActivityTime = EngineRuntime.host.nowMillis()
            }
        }

        onPointerDrag = { event: PointerEvent ->
            if (scrollable) {
                var consumed = false
                lastActivityTime = EngineRuntime.host.nowMillis()

                if (enableVerticalScroll && maxScrollY > 0.0f) {
                    val dy = event.y - lastDragY
                    scrollY = (scrollY + dy).coerceIn(0.0f, maxScrollY)
                    lastDragY = event.y
                    consumed = true
                }
                if (enableHorizontalScroll && maxScrollX > 0.0f) {
                    val dx = event.x - lastDragX
                    scrollX = (scrollX - dx).coerceIn(0.0f, maxScrollX)
                    lastDragX = event.x
                    consumed = true
                }

                if (consumed) {
                    event.isConsumed = true
                    invalidateLayout()
                }
            }
        }

        onPointerUp = {
            if (scrollable) {
                isDraggingPointer = false
                lastActivityTime = EngineRuntime.host.nowMillis()
            }
        }
    }

    // --- HIT TESTING (Scissor Boundary Protection) ---

    override fun hitTest(pointX: Float, pointY: Float): UINode? {
        if (!visible || !touchable) return null
        if ((clip || scrollable) && !bounds.contains(pointX, pointY)) return null

        for (i in children.indices.reversed()) {
            val child = children[i]
            val hit = child.hitTest(pointX, pointY)
            if (hit != null) return hit
        }

        return if (bounds.contains(pointX, pointY)) this else null
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
        val computedWidth = if (bounds.width > 0.0f) bounds.width else getPrefWidth()
        val computedHeight = if (bounds.height > 0.0f) bounds.height else getPrefHeight()

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

            // In bottom-left coordinates, scrolling DOWN increases scrollY, so children move UP (+scrollY)
            val innerX = bounds.x + padL - scrollX
            val innerY = bounds.y + padB + scrollY

            measurePolicy.layout(this, innerX, innerY, availableWidth, availableHeight)
        } else {
            val innerX = bounds.x + padL
            val innerY = bounds.y + padB
            measurePolicy.layout(this, innerX, innerY, availableWidth, availableHeight)
        }

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
            val innerW = maxOf(0.0f, bounds.width - padL - padR)
            val innerH = maxOf(0.0f, bounds.height - padT - padB)
            UIBatch.pushClip(innerX, innerY, innerW, innerH)
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

        val scrollbarAlpha: Float = if (scrollbarAutoHide) {
            if (isDraggingPointer || timeSinceActivity < scrollbarIdleTimeoutMs) {
                1.0f
            } else {
                (1.0f - (timeSinceActivity - scrollbarIdleTimeoutMs).toFloat() / scrollbarFadeDurationMs.toFloat()).coerceIn(0.0f, 1.0f)
            }
        } else {
            1.0f
        }

        if (scrollbarAlpha <= 0.001f) return

        val availW = maxOf(0.0f, bounds.width - padL - padR)
        val availH = maxOf(0.0f, bounds.height - padT - padB)
        val innerX = bounds.x + padL
        val innerY = bounds.y + padB
        val effectiveThumbColor = scrollbarThumbColor.withAlpha(scrollbarThumbColor.alpha * scrollbarAlpha)

        // 1. Draw Vertical Floating Scrollbar
        if (enableVerticalScroll && maxScrollY > 0.0f) {
            val contentH = availH + maxScrollY
            val thumbH = maxOf(20.0f, (availH / contentH) * availH)
            val scrollRatio = if (maxScrollY > 0.0f) scrollY / maxScrollY else 0.0f
            val thumbY = innerY + availH - thumbH - scrollRatio * (availH - thumbH)
            val thumbX = innerX + availW - scrollbarThickness - 2.0f

            if (scrollbarTrackColor.alpha > 0.001f) {
                UIBatch.drawBox(
                    x = thumbX,
                    y = innerY,
                    width = scrollbarThickness,
                    height = availH,
                    color = scrollbarTrackColor.withAlpha(scrollbarTrackColor.alpha * scrollbarAlpha),
                    radius = scrollbarRadius
                )
            }

            UIBatch.drawBox(
                x = thumbX,
                y = thumbY,
                width = scrollbarThickness,
                height = thumbH,
                color = effectiveThumbColor,
                radius = scrollbarRadius
            )
        }

        // 2. Draw Horizontal Floating Scrollbar
        if (enableHorizontalScroll && maxScrollX > 0.0f) {
            val contentW = maxOf(0.001f, availW + maxScrollX)
            val thumbW = maxOf(24.0f, (availW / contentW) * availW)
            val scrollRatio = if (maxScrollX > 0.0f) scrollX / maxScrollX else 0.0f
            val thumbX = innerX + scrollRatio * (availW - thumbW)
            val thumbY = innerY + 2.0f

            if (scrollbarTrackColor.alpha > 0.001f) {
                UIBatch.drawBox(
                    x = innerX,
                    y = thumbY,
                    width = availW,
                    height = scrollbarThickness,
                    color = scrollbarTrackColor.withAlpha(scrollbarTrackColor.alpha * scrollbarAlpha),
                    radius = scrollbarRadius
                )
            }

            UIBatch.drawBox(
                x = thumbX,
                y = thumbY,
                width = thumbW,
                height = scrollbarThickness,
                color = effectiveThumbColor,
                radius = scrollbarRadius
            )
        }
    }
}
