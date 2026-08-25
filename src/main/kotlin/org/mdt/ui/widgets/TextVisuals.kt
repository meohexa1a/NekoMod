package org.mdt.ui.widgets

import arc.graphics.Color
import arc.util.Align

/**
 * ## TextVisuals
 *
 * Typography visual styling configuration for [TextNode].
 */
class TextVisuals {
    var color: Color = Color(Color.white)
    var labelAlign: Int = Align.left
    var lineAlign: Int = Align.left
    var wrap: Boolean = false
    var ellipsis: String? = null
    var fontScaleX: Float = 1.0f
    var fontScaleY: Float = 1.0f
    var baselineOffset: Float = 0f

    fun scale(all: Float) {
        fontScaleX = all
        fontScaleY = all
    }

    fun scale(sx: Float, sy: Float) {
        fontScaleX = sx
        fontScaleY = sy
    }

    fun align(align: Int) {
        labelAlign = align
        lineAlign = align
    }

    fun color(c: Color) {
        color.set(c)
    }

    fun copyFrom(other: TextVisuals) {
        color.set(other.color)
        labelAlign = other.labelAlign
        lineAlign = other.lineAlign
        wrap = other.wrap
        ellipsis = other.ellipsis
        fontScaleX = other.fontScaleX
        fontScaleY = other.fontScaleY
        baselineOffset = other.baselineOffset
    }
}
