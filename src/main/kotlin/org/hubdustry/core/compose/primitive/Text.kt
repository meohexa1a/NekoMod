package org.hubdustry.core.compose.primitive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import arc.graphics.Color
import arc.graphics.g2d.Font
import arc.graphics.g2d.GlyphLayout
import mindustry.ui.Fonts
import org.hubdustry.core.compose.LayoutNodeApplier
import org.hubdustry.core.compose.Modifier
import org.hubdustry.core.compose.applyTo
import org.hubdustry.core.layout.LayoutNode
import org.hubdustry.core.layout.SizeFlag

/**
 * Bộ đo kích thước BMFont cho các [Text] composable node.
 */
object TextMeasurer {
    private val glyphLayout by lazy { GlyphLayout() }

    fun measure(node: LayoutNode, text: String) {
        node.text = text
        if (text.isEmpty()) {
            return
        }

        var measuredW: Float
        var measuredH: Float
        try {
            val font = node.font ?: Fonts.def
            if (font != null) {
                glyphLayout.setText(font, text)
                measuredW = glyphLayout.width
                measuredH = maxOf(font.lineHeight, glyphLayout.height)
            } else {
                measuredW = text.length * 8f
                measuredH = 16f
            }
        } catch (_: Throwable) {
            measuredW = text.length * 8f
            measuredH = 16f
        }

        // Intrinsic measurement tôn trọng giới hạn maxWidth / maxHeight đã ấn định tường minh
        val targetMinW = maxOf(node.minWidth, measuredW)
        val targetMinH = maxOf(node.minHeight, measuredH)

        node.minWidth = targetMinW.coerceAtMost(node.maxWidth)
        node.minHeight = targetMinH.coerceAtMost(node.maxHeight)
    }
}

/**
 * [Text] composable hiển thị văn bản BMFont.
 * Hỗ trợ cấu hình màu chữ và font thông qua cả tham số trực tiếp lẫn qua [Modifier.textColor] / [Modifier.font].
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier.Companion,
    textColor: Color? = null,
    font: Font? = null
) {
    ComposeNode<LayoutNode, LayoutNodeApplier>(
        factory = { LayoutNode() },
        update = {
            set(modifier) {
                this.resetModifierState(SizeFlag.SHRINK, SizeFlag.SHRINK)
                it.applyTo(this)
                if (textColor != null) this.textColor = textColor
                if (font != null) this.font = font
                TextMeasurer.measure(this, text)
            }
            set(text) {
                this.resetModifierState(SizeFlag.SHRINK, SizeFlag.SHRINK)
                modifier.applyTo(this)
                if (textColor != null) this.textColor = textColor
                if (font != null) this.font = font
                TextMeasurer.measure(this, it)
            }
            set(textColor) {
                if (it != null) {
                    this.textColor = it
                }
            }
            set(font) {
                if (it != null) {
                    this.font = it
                    this.resetModifierState(SizeFlag.SHRINK, SizeFlag.SHRINK)
                    modifier.applyTo(this)
                    if (textColor != null) this.textColor = textColor
                    this.font = it
                    TextMeasurer.measure(this, text)
                }
            }
        }
    )
}
