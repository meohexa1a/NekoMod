package org.mdt.ui.components.text

import arc.graphics.Color
import arc.graphics.g2d.Font
import arc.util.Align
import mindustry.ui.Fonts

/**
 * ## TextVisuals
 *
 * Typography parameters for text styling, font selection, and layout measurement.
 *
 * See: docs/architecture/architecture_en.md
 */
class TextVisuals {
    var text: String = ""
    var font: Font = Fonts.def
    var color: Color = Color(Color.white)
    var fontScaleX: Float = 1.0f
    var fontScaleY: Float = 1.0f
    var labelAlign: Int = Align.left
    var lineAlign: Int = Align.left
    var wrap: Boolean = false
    var ellipsis: String? = null
    var baselineOffset: Float = 0f

    var fontScale: Float
        get() = fontScaleX
        set(value) {
            fontScaleX = value
            fontScaleY = value
        }

    var align: Int
        get() = labelAlign
        set(value) {
            labelAlign = value
            lineAlign = value
        }

    fun font(font: Font): TextVisuals {
        this.font = font
        return this
    }

    fun color(color: Color): TextVisuals {
        this.color.set(color)
        return this
    }

    fun scale(scale: Float): TextVisuals {
        this.fontScale = scale
        return this
    }

    fun align(align: Int): TextVisuals {
        this.align = align
        return this
    }

    fun wrap(wrap: Boolean): TextVisuals {
        this.wrap = wrap
        return this
    }
}
