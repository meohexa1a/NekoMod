package org.mdt.ui.components.display.progress

import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.ui.render.EngineRenderer

/**
 * ## ProgressBarNode
 *
 * Virtual DOM node rendering a rounded progress bar with customizable track and fill colors.
 *
 * See: docs/roadmap/roadmap_en.md
 */
open class ProgressBarNode : LayoutNode() {

    /** Progress value clamped between 0.0f and 1.0f. */
    var progress: Float = 0.0f
        set(value) {
            val clamped = value.coerceIn(0.0f, 1.0f)
            if (field != clamped) {
                field = clamped
                invalidateLayout()
            }
        }

    var trackColor: Color = Color(Color.valueOf("181926"))
    var fillColor: Color = Color(Color.valueOf("2563eb"))
    var barHeight: Float = 6f

    init {
        minHeight = barHeight
        minWidth = 100f
    }

    override fun getPrefWidth(): Float = if (width >= 0f) width else maxOf(minWidth, 100f) + padL + padR
    override fun getPrefHeight(): Float = if (height >= 0f) height else maxOf(barHeight, minHeight) + padT + padB

    override fun drawSelf(renderer: EngineRenderer) {
        val w = bounds.width - padL - padR
        val h = bounds.height - padT - padB
        if (w <= 0f || h <= 0f) return

        val innerX = bounds.x + padL
        val innerY = bounds.y + padB
        val actualBarH = minOf(h, barHeight)
        val centerY = innerY + h * 0.5f

        // 1. Draw Track
        Draw.color(trackColor)
        Fill.rect(innerX + w * 0.5f, centerY, w, actualBarH)

        // 2. Draw Fill
        if (progress > 0.0f) {
            val fillW = w * progress
            Draw.color(fillColor)
            Fill.rect(innerX + fillW * 0.5f, centerY, fillW, actualBarH)
        }

        Draw.color(Color.white)
    }
}
