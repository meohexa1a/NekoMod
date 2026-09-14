package org.hubdustry.core.compose.primitive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import arc.graphics.Color
import arc.graphics.g2d.Font
import arc.graphics.g2d.GlyphLayout
import mindustry.ui.Fonts
import org.hubdustry.core.compose.modifier.Modifier
import org.hubdustry.core.compose.modifier.applyTo
import org.hubdustry.core.compose.runtime.LayoutNodeApplier
import org.hubdustry.core.layout.LayoutNode
import org.hubdustry.core.layout.SizeFlag

private const val FALLBACK_CHAR_WIDTH = 8f
private const val FALLBACK_LINE_HEIGHT = 16f

// ─── Public Text Composable ───────────────────────────────────────

/**
 * [Text] composable hiển thị văn bản BMFont.
 * Hỗ trợ cấu hình màu chữ và font thông qua cả tham số trực tiếp lẫn qua [Modifier.textColor] / [Modifier.font].
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color? = null,
    font: Font? = null
) {
    ComposeNode<LayoutNode, LayoutNodeApplier>(
        factory = { LayoutNode().apply { clip = true } },
        update = {
            set(modifier) {
                this.resetModifierState(SizeFlag.SHRINK, SizeFlag.SHRINK)
                this.clip = true
                it.applyTo(this)
                if (textColor != null) this.textColor = textColor
                if (font != null) this.font = font
                TextMeasurer.measure(this, text)
            }
            set(text) {
                this.resetModifierState(SizeFlag.SHRINK, SizeFlag.SHRINK)
                this.clip = true
                modifier.applyTo(this)
                if (textColor != null) this.textColor = textColor
                if (font != null) this.font = font
                TextMeasurer.measure(this, it)
            }
            set(textColor) {
                val color = it ?: return@set
                this.textColor = color
            }
            set(font) {
                val targetFont = it ?: return@set
                this.font = targetFont
                this.resetModifierState(SizeFlag.SHRINK, SizeFlag.SHRINK)
                this.clip = true
                modifier.applyTo(this)
                if (textColor != null) this.textColor = textColor
                this.font = targetFont
                TextMeasurer.measure(this, text)
            }
        }
    )
}

// ─── Internal Text Measurement Engine ─────────────────────────────

/**
 * Bộ đo kích thước BMFont cho các [Text] composable node.
 */
internal object TextMeasurer {
    private val glyphLayout by lazy { GlyphLayout() }

    fun measure(node: LayoutNode, text: String) {
        node.text = text
        if (text.isEmpty()) {
            return
        }

        var measuredWidth: Float
        var measuredHeight: Float
        try {
            val font = node.font ?: Fonts.def
            if (font != null) {
                glyphLayout.setText(font, text)
                measuredWidth = glyphLayout.width
                measuredHeight = maxOf(font.lineHeight, glyphLayout.height)
            } else {
                measuredWidth = text.length * FALLBACK_CHAR_WIDTH
                measuredHeight = FALLBACK_LINE_HEIGHT
            }
        } catch (_: Throwable) {
            measuredWidth = text.length * FALLBACK_CHAR_WIDTH
            measuredHeight = FALLBACK_LINE_HEIGHT
        }

        // Intrinsic measurement tôn trọng giới hạn maxWidth / maxHeight đã ấn định tường minh
        val targetMinWidth = maxOf(node.minWidth, measuredWidth)
        val targetMinHeight = maxOf(node.minHeight, measuredHeight)

        node.minWidth = targetMinWidth.coerceAtMost(node.maxWidth)
        node.minHeight = targetMinHeight.coerceAtMost(node.maxHeight)
    }
}

