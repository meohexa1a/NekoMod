package org.hubdustry.ui

import androidx.compose.runtime.Composable
import arc.graphics.Color
import arc.graphics.g2d.Font
import arc.scene.ui.layout.Cell
import arc.scene.ui.layout.Table
import org.hubdustry.core.compose.Modifier
import org.hubdustry.core.compose.foundation.ScrollState
import org.hubdustry.core.compose.foundation.rememberScrollState
import org.hubdustry.core.compose.input.IntSize
import org.hubdustry.core.compose.input.InteractionSource
import org.hubdustry.core.compose.input.MutableInteractionSource
import org.hubdustry.core.compose.input.Offset
import org.hubdustry.core.compose.input.PointerInputScope
import org.hubdustry.core.compose.input.clickable
import org.hubdustry.core.compose.input.hoverable
import org.hubdustry.core.compose.input.pointerInput
import org.hubdustry.core.compose.modifier.*
import org.hubdustry.core.compose.view.ComposeView
import org.hubdustry.core.graphics.RoundedCorners
import org.hubdustry.core.layout.Alignment
import org.hubdustry.core.layout.AnchorPreset
import org.hubdustry.core.layout.Orientation
import org.hubdustry.core.layout.SizeFlag
import org.hubdustry.ui.components.Button
import org.hubdustry.ui.components.Checkbox
import org.hubdustry.ui.components.DefaultButtonPressedColor
import org.hubdustry.ui.components.Switch

// --- 1. TYPEALIASES & CORE EXPORTS ---

typealias Modifier = org.hubdustry.core.compose.Modifier
typealias RoundedCorners = org.hubdustry.core.graphics.RoundedCorners
typealias Alignment = org.hubdustry.core.layout.Alignment
typealias AnchorPreset = org.hubdustry.core.layout.AnchorPreset
typealias Orientation = org.hubdustry.core.layout.Orientation
typealias SizeFlag = org.hubdustry.core.layout.SizeFlag
typealias ScrollState = org.hubdustry.core.compose.foundation.ScrollState
typealias Offset = org.hubdustry.core.compose.input.Offset
typealias IntSize = org.hubdustry.core.compose.input.IntSize
typealias InteractionSource = org.hubdustry.core.compose.input.InteractionSource
typealias MutableInteractionSource = org.hubdustry.core.compose.input.MutableInteractionSource
typealias ComposeView = org.hubdustry.core.compose.view.ComposeView
typealias BoxScope = org.hubdustry.core.compose.modifier.BoxScope
typealias RowScope = org.hubdustry.core.compose.modifier.RowScope
typealias ColumnScope = org.hubdustry.core.compose.modifier.ColumnScope

// --- 2. CORE COMPOSABLES & HOST DSL ---

/**
 * Cú pháp DSL thuận tiện để nhúng trực tiếp [ComposeView] vào bất kỳ [Table] nào của Arc Scene2D.
 */
fun Table.compose(content: @Composable () -> Unit): Cell<ComposeView> {
    val view = ComposeView().apply {
        setContent(content)
    }
    return this.add(view)
}

/**
 * Container Box (FrameLayout) tự do theo chuẩn Jetpack Compose.
 */
@Composable
fun Box(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) = org.hubdustry.core.compose.primitive.Box(modifier, content)

/**
 * Container Row (HBox) xếp ngang theo chuẩn Jetpack Compose.
 */
@Composable
fun Row(
    modifier: Modifier = Modifier,
    gap: Float = 0f,
    content: @Composable RowScope.() -> Unit
) = org.hubdustry.core.compose.primitive.Row(modifier, gap, content)

/**
 * Container Column (VBox) xếp dọc theo chuẩn Jetpack Compose.
 */
@Composable
fun Column(
    modifier: Modifier = Modifier,
    gap: Float = 0f,
    content: @Composable ColumnScope.() -> Unit
) = org.hubdustry.core.compose.primitive.Column(modifier, gap, content)

/**
 * Composable hiển thị văn bản BMFont.
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color? = null,
    font: Font? = null
) = org.hubdustry.core.compose.primitive.Text(text, modifier, textColor, font)

/**
 * Nút bấm chuẩn của NekoMod UI:
 * - Tự động đổi màu khi bị nhấn (pressed) và khi rê chuột qua (hovered) dựa trên [MutableInteractionSource].
 * - Kết nối trực tiếp cử chỉ click/tap mà không dùng Arc ClickListener.
 * - Hỗ trợ hình dạng bo góc SDF tự nhiên qua [corners].
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
    corners: RoundedCorners = org.hubdustry.ui.components.DefaultButtonCorners,
    backgroundColor: Color = Color.royal,
    pressedColor: Color = DefaultButtonPressedColor,
    hoverColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) = org.hubdustry.ui.components.Button(onClick, modifier, interactionSource, enabled, corners, backgroundColor, pressedColor, hoverColor, content)

/**
 * Hộp chọn (Checkbox) chuẩn của NekoMod UI:
 * - Kích thước mặc định 20x20 px với bo góc SDF 4px.
 * - Hiển thị ký tự "✓" căn chính giữa khi được chọn ([checked] = true).
 * - Kết nối trực tiếp cử chỉ click/tap mà không dùng Arc ClickListener.
 * - Tự động giảm độ mờ (alpha 0.5f) và vô hiệu hóa tương tác khi [enabled] = false.
 */
@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    checkedColor: Color = Color.royal,
    uncheckedColor: Color = Color.darkGray,
    checkmarkColor: Color = Color.white,
    borderColor: Color = Color.lightGray
) = org.hubdustry.ui.components.Checkbox(
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = modifier,
    enabled = enabled,
    interactionSource = interactionSource,
    checkedColor = checkedColor,
    uncheckedColor = uncheckedColor,
    checkmarkColor = checkmarkColor,
    borderColor = borderColor
)

/**
 * Công tắc trượt (Switch) chuẩn của NekoMod UI:
 * - Khung ray (Track) kích thước mặc định 40x22 px với bo góc SDF 11px (viên thuốc).
 * - Nút trượt tròn (Thumb) kích thước 16x16 px với bo góc SDF 8px.
 * - Tự động chuyển vị trí thumb giữa START và END theo trạng thái [checked].
 * - Kết nối trực tiếp cử chỉ click/tap mà không dùng Arc ClickListener.
 * - Tự động giảm độ mờ (alpha 0.5f) và vô hiệu hóa tương tác khi [enabled] = false.
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    trackCheckedColor: Color = Color.royal,
    trackUncheckedColor: Color = Color.darkGray,
    thumbColor: Color = Color.white
) = org.hubdustry.ui.components.Switch(
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = modifier,
    enabled = enabled,
    interactionSource = interactionSource,
    trackCheckedColor = trackCheckedColor,
    trackUncheckedColor = trackUncheckedColor,
    thumbColor = thumbColor
)

/**
 * Factory tạo [MutableInteractionSource] quản lý tương tác người dùng.
 */
fun MutableInteractionSource(): MutableInteractionSource =
    org.hubdustry.core.compose.input.MutableInteractionSource()

/**
 * Nhớ trạng thái cuộn [ScrollState] qua Recomposition.
 */
@Composable
fun rememberScrollState(initial: Float = 0f): ScrollState =
    org.hubdustry.core.compose.foundation.rememberScrollState(initial)

