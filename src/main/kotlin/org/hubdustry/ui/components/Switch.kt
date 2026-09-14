package org.hubdustry.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import arc.graphics.Color
import org.hubdustry.core.compose.Modifier
import org.hubdustry.core.compose.input.MutableInteractionSource
import org.hubdustry.core.compose.input.clickable
import org.hubdustry.core.compose.input.hoverable
import org.hubdustry.core.compose.modifier.alpha
import org.hubdustry.core.compose.modifier.background
import org.hubdustry.core.compose.modifier.padding
import org.hubdustry.core.compose.modifier.size
import org.hubdustry.core.compose.primitive.Box
import org.hubdustry.core.graphics.RoundedCorners
import org.hubdustry.core.layout.Alignment

internal val DefaultSwitchTrackCorners = RoundedCorners(11f)
internal val DefaultSwitchThumbCorners = RoundedCorners(8f)

/**
 * Công tắc trượt (Switch) chuẩn Jetpack Compose cho NekoMod UI:
 * - Khung ray (Track): Kích thước mặc định 40x22 px, bo góc 11px ([DefaultSwitchTrackCorners] - dạng viên thuốc).
 * - Nút trượt tròn (Thumb): Kích thước chuẩn 16x16 px, bo góc 8px ([DefaultSwitchThumbCorners] - hình tròn).
 * - Khi [checked] = true: Track màu [trackCheckedColor], thumb nằm sát mép phải ([Alignment.END]).
 * - Khi [checked] = false: Track màu [trackUncheckedColor], thumb nằm sát mép trái ([Alignment.START]).
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
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }

    val interactiveModifier = if (enabled && onCheckedChange != null) {
        Modifier
            .hoverable(interactionSource = source, enabled = enabled)
            .clickable(interactionSource = source, enabled = enabled) {
                onCheckedChange(!checked)
            }
    } else {
        Modifier
    }

    val trackColor = if (checked) trackCheckedColor else trackUncheckedColor

    Box(
        modifier = Modifier
            .size(40f, 22f)
            .then(modifier)
            .then(if (!enabled) Modifier.alpha(0.5f) else Modifier)
            .background(trackColor, DefaultSwitchTrackCorners)
            .padding(3f)
            .then(interactiveModifier)
    ) {
        Box(
            modifier = Modifier
                .size(16f, 16f)
                .align(if (checked) Alignment.END else Alignment.START, Alignment.CENTER)
                .background(thumbColor, DefaultSwitchThumbCorners)
        )
    }
}
