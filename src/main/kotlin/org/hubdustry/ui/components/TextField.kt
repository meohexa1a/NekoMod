package org.hubdustry.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import arc.graphics.Color
import arc.graphics.g2d.Font
import org.hubdustry.core.compose.input.MutableInteractionSource
import org.hubdustry.core.compose.input.collectIsFocusedAsState
import org.hubdustry.core.compose.modifier.Modifier
import org.hubdustry.core.compose.modifier.alpha
import org.hubdustry.core.compose.modifier.background
import org.hubdustry.core.compose.modifier.border
import org.hubdustry.core.compose.modifier.padding
import org.hubdustry.core.compose.modifier.sizeIn
import org.hubdustry.core.compose.primitive.BasicTextField
import org.hubdustry.core.compose.primitive.DefaultSelectionColor
import org.hubdustry.core.compose.primitive.Text
import org.hubdustry.core.graphics.RoundedCorners
import org.hubdustry.core.layout.Alignment

val DefaultTextFieldBgColor: Color = Color.valueOf("181926")
val DefaultTextFieldBorderColor: Color = Color.valueOf("363a4f")
val DefaultTextFieldFocusBorderColor: Color = Color.royal
val DefaultTextFieldSelectionColor: Color = DefaultSelectionColor
val DefaultTextFieldShape: RoundedCorners = RoundedCorners(6f)

/**
 * Thành phần soạn thảo văn bản Composable [TextField] hoàn chỉnh chuẩn NekoMod v3:
 * - Đóng vai trò là Decorator bọc quanh Primitive [BasicTextField] nguyên thủy.
 * - Trang trí nền bo góc SDF, viền đổi màu khi focus, lề đệm và chuỗi gợi ý placeholder.
 */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "",
    singleLine: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    shape: RoundedCorners = DefaultTextFieldShape,
    backgroundColor: Color = DefaultTextFieldBgColor,
    borderColor: Color = DefaultTextFieldBorderColor,
    focusedBorderColor: Color = DefaultTextFieldFocusBorderColor,
    textColor: Color = Color.white,
    placeholderColor: Color = Color.gray,
    cursorColor: Color = Color.royal,
    selectionColor: Color = DefaultTextFieldSelectionColor,
    font: Font? = null
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by source.collectIsFocusedAsState()
    val currentBorderColor = if (isFocused) focusedBorderColor else borderColor
    val currentBorderWidth = if (isFocused) 1.5f else 1f

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .sizeIn(minWidth = 140f, minHeight = 36f)
            .then(if (!enabled) Modifier.alpha(0.5f) else Modifier)
            .background(backgroundColor, shape)
            .border(currentBorderWidth, currentBorderColor, shape)
            .padding(horizontal = 12f, vertical = 8f),
        enabled = enabled,
        singleLine = singleLine,
        interactionSource = source,
        textColor = textColor,
        font = font,
        cursorColor = cursorColor,
        selectionColor = selectionColor,
        decorationBox = { innerTextField ->
            if (value.isEmpty() && !isFocused) {
                Text(
                    text = placeholder,
                    textColor = placeholderColor,
                    font = font,
                    modifier = Modifier.align(Alignment.START, Alignment.CENTER)
                )
            }
            innerTextField()
        }
    )
}
