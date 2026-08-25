package org.mdt.ui.components.input.slider

import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.GlyphLayout
import arc.math.Mathf
import mindustry.ui.Fonts
import org.mdt.ui.components.layout.BoxVisuals
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.core.PointerEvent
import org.mdt.ui.core.Rect
import org.mdt.ui.render.BoxRenderer
import org.mdt.ui.render.EngineRenderer
import org.mdt.ui.render.ScissorStack

/**
 * ## SliderNode
 *
 * Modern capsule pill slider node (iOS & Material 3 inspired) supporting full-width
 * smooth drag adjustment, integrated left title/label, and right value readout.
 *
 * See: docs/roadmap/roadmap_en.md
 */
open class SliderNode : LayoutNode() {

    var value: Float = 0.0f
        set(v) {
            val clamped = v.coerceIn(valueRange.start, valueRange.endInclusive)
            val finalVal = if (step > 0f) Mathf.round(clamped / step) * step else clamped

            if (field != finalVal) {
                field = finalVal
                invalidateLayout()
            }
        }

    var valueRange: ClosedFloatingPointRange<Float> = 0f..1f
    var step: Float = 0f
    var onValueChange: ((Float) -> Unit)? = null

    var label: String? = null
        set(v) {
            if (field != v) {
                field = v
                invalidateLayout()
            }
        }

    var valueText: String? = null
        set(v) {
            if (field != v) {
                field = v
                invalidateLayout()
            }
        }

    var trackColor: Color = Color(Color.valueOf("181926"))
    var activeTrackColor: Color = Color(Color.valueOf("2563eb"))
    var borderColor: Color = Color(Color.valueOf("363a4f"))
    var labelColor: Color = Color(Color.white)
    var valueColor: Color = Color(Color.valueOf("cad3f5"))

    private var isDragging = false

    private val trackVisuals = BoxVisuals()
    private val fillVisuals = BoxVisuals()

    init {
        minHeight = 30f
        minWidth = 140f

        onPointerDown = { event: PointerEvent ->
            isDragging = true
            updateValueFromScreenX(event.x)
        }

        onPointerDrag = { event: PointerEvent ->
            if (isDragging) {
                updateValueFromScreenX(event.x)
            }
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
        val innerX = bounds.x + padL
        val innerW = bounds.width - padL - padR
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

    override fun getPrefWidth(): Float = if (width >= 0f) width else maxOf(minWidth, 180f) + padL + padR
    override fun getPrefHeight(): Float = if (height >= 0f) height else maxOf(minHeight, 30f) + padT + padB

    override fun drawSelf(renderer: EngineRenderer) {
        val w = bounds.width - padL - padR
        val h = bounds.height - padT - padB
        if (w <= 0f || h <= 0f) return

        val innerX = bounds.x + padL
        val innerY = bounds.y + padB
        val radius = h * 0.5f
        val progress = getProgress()

        // 1. Draw Background Capsule Track
        trackVisuals.radius(radius)
        trackVisuals.fillColor.set(trackColor)
        trackVisuals.border(1f, borderColor)
        BoxRenderer.draw(innerX, innerY, w, h, trackVisuals, renderer.blurProcessor)

        // 2. Draw Active Fill Capsule (Clipped smoothly to progress)
        if (progress > 0.001f) {
            val fillW = w * progress
            val pushed = ScissorStack.push(Rect(innerX, innerY, fillW, h))
            if (pushed) {
                fillVisuals.radius(radius)
                fillVisuals.fillColor.set(activeTrackColor)
                BoxRenderer.draw(innerX, innerY, w, h, fillVisuals, renderer.blurProcessor)
                ScissorStack.pop()
            }
        }

        // 3. Draw Integrated Text Overlays (Pixel-Perfect 1.0f BMFont)
        val f = Fonts.def
        val textY = innerY + (h + f.data.capHeight) * 0.5f

        // Left Label
        val currentLabel = label
        if (!currentLabel.isNullOrEmpty()) {
            f.color = labelColor
            f.draw(currentLabel, innerX + 12f, textY)
        }

        // Right Value
        val valText = valueText ?: "${(value * 100f).toInt()}%"
        if (valText.isNotEmpty()) {
            glyphLayout.setText(f, valText)
            f.color = valueColor
            f.draw(valText, innerX + w - 12f - glyphLayout.width, textY)
        }

        Draw.color(Color.white)
    }

    companion object {
        private val glyphLayout = GlyphLayout()
    }
}
