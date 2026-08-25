package org.mdt.ui.components.input.slider

import arc.Core
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.math.Mathf
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.core.PointerEvent
import org.mdt.ui.render.EngineRenderer

/**
 * ## SliderNode
 *
 * Interactive draggable slider node supporting custom value ranges, step increments,
 * and sleek circular thumb knob rendering.
 *
 * See: docs/roadmap/roadmap_en.md
 */
open class SliderNode : LayoutNode() {

    var value: Float = 0.0f
        set(v) {
            val clamped = v.coerceIn(valueRange.start, valueRange.endInclusive)
            val finalVal = if (step > 0f) {
                Mathf.round(clamped / step) * step
            } else clamped

            if (field != finalVal) {
                field = finalVal
                invalidateLayout()
            }
        }

    var valueRange: ClosedFloatingPointRange<Float> = 0f..1f
    var step: Float = 0f
    var onValueChange: ((Float) -> Unit)? = null

    var trackColor: Color = Color(Color.valueOf("181926"))
    var activeTrackColor: Color = Color(Color.valueOf("2563eb"))
    var thumbColor: Color = Color(Color.white)
    var thumbRadius: Float = 6f
    var trackHeight: Float = 4f

    private var isDragging = false

    init {
        minHeight = 24f
        minWidth = 100f

        onPointerDown = { event: PointerEvent ->
            isDragging = true
            updateValueFromScreenX(event.x)
        }

        onPointerUp = {
            isDragging = false
        }
    }

    private fun getProgress(): Float {
        val span = valueRange.endInclusive - valueRange.start
        if (span <= 0f) return 0f
        return ((value - valueRange.start) / span).coerceIn(0f, 1f)
    }

    private fun updateValueFromScreenX(screenX: Float) {
        val innerX = bounds.x + padL + thumbRadius
        val innerW = bounds.width - padL - padR - thumbRadius * 2f
        if (innerW <= 0f) return

        val ratio = ((screenX - innerX) / innerW).coerceIn(0f, 1f)
        val span = valueRange.endInclusive - valueRange.start
        val rawValue = valueRange.start + ratio * span
        val stepped = if (step > 0f) Mathf.round(rawValue / step) * step else rawValue
        val finalVal = stepped.coerceIn(valueRange.start, valueRange.endInclusive)

        if (value != finalVal) {
            value = finalVal
            onValueChange?.invoke(finalVal)
            invalidateLayout()
        }
    }

    override fun draw(renderer: EngineRenderer) {
        if (isDragging && Core.input != null) {
            val mouseX = Core.input.mouseX().toFloat()
            updateValueFromScreenX(mouseX)
        }
        super.draw(renderer)
    }

    override fun getPrefWidth(): Float = if (width >= 0f) width else maxOf(minWidth, 120f) + padL + padR
    override fun getPrefHeight(): Float = if (height >= 0f) height else maxOf(minHeight, 24f) + padT + padB

    override fun drawSelf(renderer: EngineRenderer) {
        val w = bounds.width - padL - padR
        val h = bounds.height - padT - padB
        if (w <= 0f || h <= 0f) return

        val innerX = bounds.x + padL
        val innerY = bounds.y + padB
        val centerY = innerY + h * 0.5f

        val trackLeft = innerX + thumbRadius
        val trackWidth = maxOf(0f, w - thumbRadius * 2f)

        // 1. Draw Inactive Track
        Draw.color(trackColor)
        Fill.rect(trackLeft + trackWidth * 0.5f, centerY, trackWidth, trackHeight)

        // 2. Draw Active Track
        val progress = getProgress()
        val activeW = trackWidth * progress
        if (activeW > 0f) {
            Draw.color(activeTrackColor)
            Fill.rect(trackLeft + activeW * 0.5f, centerY, activeW, trackHeight)
        }

        // 3. Draw Thumb Knob
        val thumbX = trackLeft + activeW
        val currentRadius = if (isDragging) thumbRadius * 1.25f else thumbRadius

        // Subtle knob shadow
        Draw.color(Color.black.a(0.35f))
        Fill.circle(thumbX, centerY - 1f, currentRadius + 1f)

        // Knob circle
        Draw.color(thumbColor)
        Fill.circle(thumbX, centerY, currentRadius)

        Draw.color(Color.white)
    }
}
