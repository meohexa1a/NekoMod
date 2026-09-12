package org.hubdustry.libs.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import arc.graphics.Color
import arc.graphics.g2d.GlyphLayout
import mindustry.ui.Fonts
import org.hubdustry.libs.compose.input.MutableInteractionSource
import org.hubdustry.libs.compose.input.clickable
import org.hubdustry.libs.compose.input.collectIsHoveredAsState
import org.hubdustry.libs.compose.input.collectIsPressedAsState
import org.hubdustry.libs.compose.input.hoverable
import org.hubdustry.libs.compose.modifier.BoxScope
import org.hubdustry.libs.compose.modifier.BoxScopeInstance
import org.hubdustry.libs.compose.modifier.ColumnScope
import org.hubdustry.libs.compose.modifier.ColumnScopeInstance
import org.hubdustry.libs.compose.modifier.RowScope
import org.hubdustry.libs.compose.modifier.RowScopeInstance
import org.hubdustry.libs.compose.modifier.background
import org.hubdustry.libs.layout.LayoutNode
import org.hubdustry.libs.layout.SizeFlag
import org.hubdustry.libs.layout.policies.BoxLayoutPolicy
import org.hubdustry.libs.layout.policies.ColumnPolicy
import org.hubdustry.libs.layout.policies.RowPolicy

/**
 * Các hàm Composable cơ bản ánh xạ trực tiếp vào [LayoutNode],
 * cấu hình hoàn toàn thông qua [Modifier] theo chuẩn Jetpack Compose.
 */
@Composable
inline fun Box(
    modifier: Modifier = Modifier,
    crossinline content: @Composable BoxScope.() -> Unit = {}
) {
    ComposeNode<LayoutNode, LayoutNodeApplier>(
        factory = { LayoutNode().apply { policy = BoxLayoutPolicy } },
        update = {
            set(modifier) {
                this.resetModifierState(SizeFlag.FILL, SizeFlag.FILL)
                it.applyTo(this)
            }
        },
        content = { BoxScopeInstance.content() }
    )
}

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
            val font = Fonts.def
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

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.white
) {
    ComposeNode<LayoutNode, LayoutNodeApplier>(
        factory = { LayoutNode() },
        update = {
            set(modifier) {
                this.resetModifierState(SizeFlag.SHRINK, SizeFlag.SHRINK)
                it.applyTo(this)
                TextMeasurer.measure(this, text)
            }
            set(text) { TextMeasurer.measure(this, it) }
            set(textColor) { this.textColor = it }
        }
    )
}

@Composable
inline fun Row(
    modifier: Modifier = Modifier,
    gap: Float = 0f,
    crossinline content: @Composable RowScope.() -> Unit = {}
) {
    ComposeNode<LayoutNode, LayoutNodeApplier>(
        factory = { LayoutNode().apply { policy = RowPolicy(gap) } },
        update = {
            set(modifier) {
                this.resetModifierState(SizeFlag.FILL, SizeFlag.FILL)
                it.applyTo(this)
            }
            set(gap) { this.policy = RowPolicy(it) }
        },
        content = { RowScopeInstance.content() }
    )
}

@Composable
inline fun Column(
    modifier: Modifier = Modifier,
    gap: Float = 0f,
    crossinline content: @Composable ColumnScope.() -> Unit = {}
) {
    ComposeNode<LayoutNode, LayoutNodeApplier>(
        factory = { LayoutNode().apply { policy = ColumnPolicy(gap) } },
        update = {
            set(modifier) {
                this.resetModifierState(SizeFlag.FILL, SizeFlag.FILL)
                it.applyTo(this)
            }
            set(gap) { this.policy = ColumnPolicy(it) }
        },
        content = { ColumnScopeInstance.content() }
    )
}

private val DefaultButtonPressedColor: Color by lazy { Color.royal.cpy().mul(0.75f) }

/**
 * Nút bấm chuẩn của NekoMod Compose:
 * - Tự động đổi màu khi bị nhấn (pressed) và khi rê chuột qua (hovered) dựa trên [MutableInteractionSource].
 * - Kết nối trực tiếp cử chỉ click/tap mà không dùng Arc ClickListener.
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
    backgroundColor: Color = Color.royal,
    pressedColor: Color = DefaultButtonPressedColor,
    hoverColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val isHovered by source.collectIsHoveredAsState()

    val effectiveHoverColor = hoverColor ?: remember(backgroundColor) { backgroundColor.cpy().mul(1.2f) }
    val currentColor = when {
        isPressed -> pressedColor
        isHovered -> effectiveHoverColor
        else -> backgroundColor
    }

    Box(
        modifier = modifier
            .background(currentColor)
            .hoverable(interactionSource = source, enabled = enabled)
            .clickable(interactionSource = source, enabled = enabled, onClick = onClick),
        content = content
    )
}
