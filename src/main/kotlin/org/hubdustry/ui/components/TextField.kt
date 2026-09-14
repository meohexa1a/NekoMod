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

// ─── TextField Defaults ──────────────────────────────────────────────────────

/**
 * Các giá trị cấu hình mặc định cho Composable [TextField].
 */
object TextFieldDefaults {
    const val MIN_WIDTH: Float = 140f
    const val MIN_HEIGHT: Float = 36f
    const val BORDER_WIDTH_FOCUSED: Float = 1.5f
    const val BORDER_WIDTH_DEFAULT: Float = 1f
    const val DISABLED_ALPHA: Float = 0.5f

    val shape: RoundedCorners = RoundedCorners(6f)
    val backgroundColor: Color = Color.valueOf("181926")
    val borderColor: Color = Color.valueOf("363a4f")
    val focusedBorderColor: Color = Color.royal
    val selectionColor: Color = DefaultSelectionColor
    val textColor: Color = Color.white
    val placeholderColor: Color = Color.gray
    val cursorColor: Color = Color.royal
}

// ─── Composable TextField ────────────────────────────────────────────────────

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
    shape: RoundedCorners = TextFieldDefaults.shape,
    backgroundColor: Color = TextFieldDefaults.backgroundColor,
    borderColor: Color = TextFieldDefaults.borderColor,
    focusedBorderColor: Color = TextFieldDefaults.focusedBorderColor,
    textColor: Color = TextFieldDefaults.textColor,
    placeholderColor: Color = TextFieldDefaults.placeholderColor,
    cursorColor: Color = TextFieldDefaults.cursorColor,
    selectionColor: Color = TextFieldDefaults.selectionColor,
    font: Font? = null
) {
    val currentInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by currentInteractionSource.collectIsFocusedAsState()
    val currentBorderColor = if (isFocused) focusedBorderColor else borderColor
    val currentBorderWidth = if (isFocused) TextFieldDefaults.BORDER_WIDTH_FOCUSED else TextFieldDefaults.BORDER_WIDTH_DEFAULT

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .sizeIn(minWidth = TextFieldDefaults.MIN_WIDTH, minHeight = TextFieldDefaults.MIN_HEIGHT)
            .then(if (!enabled) Modifier.alpha(TextFieldDefaults.DISABLED_ALPHA) else Modifier)
            .background(color = backgroundColor, corners = shape)
            .border(width = currentBorderWidth, color = currentBorderColor, corners = shape)
            .padding(horizontal = 12f, vertical = 8f),
        enabled = enabled,
        singleLine = singleLine,
        interactionSource = currentInteractionSource,
        textColor = textColor,
        font = font,
        cursorColor = cursorColor,
        selectionColor = selectionColor,
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
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
