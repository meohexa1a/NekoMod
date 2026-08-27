package org.mdt.ui.components.input

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.theme.Theme

/**
 * ## SegmentedControl
 *
 * Apple iOS/macOS-style segmented pill tab selector with tactile feedback,
 * high-contrast selection highlights, and auto-sizing chips.
 *
 * @param items List of string labels for each segment.
 * @param selectedIndex Currently selected segment index (0-based).
 * @param onSelect Callback invoked with the newly selected index.
 * @param modifier Chainable [UIModifier].
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun SegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: UIModifier = UIModifier
) {
    val colors = Theme.colors
    val shapes = Theme.shapes

    val dynamicItemWidth = remember(items) {
        val layout = arc.graphics.g2d.GlyphLayout()
        val font = mindustry.ui.Fonts.def
        val maxTextWidth = items.maxOfOrNull { label ->
            layout.setText(font, label)
            layout.width
        } ?: 40f
        maxTextWidth + 28f
    }

    // Outer inset capsule track
    Box(
        modifier = Modifier
            .radius(shapes.xs)
            .background(colors.surfacePrimary)
            .border(width = 1f, color = colors.borderHairline)
            .pad(3f)
            .then(modifier)
    ) {
        Row(
            arrangement = Arrangement.spacedBy(4f),
            alignment = Alignment.CenterStart
        ) {
            for ((index, label) in items.withIndex()) {
                val isSelected = index == selectedIndex
                SegmentItem(
                    label = label,
                    itemWidth = dynamicItemWidth,
                    isSelected = isSelected,
                    onClick = { onSelect(index) }
                )
            }
        }
    }
}

@Composable
private fun SegmentItem(
    label: String,
    itemWidth: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = Theme.colors
    val shapes = Theme.shapes

    var isHovered by remember { mutableStateOf(false) }

    val activeAnim by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
    )
    val hoverAnim by animateFloatAsState(
        targetValue = if (isHovered && !isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
    )

    val idleBg = colors.surfaceSecondary
    val hoverBg = colors.surfaceTertiary
    val activeBg = colors.blue

    val currentBg = idleBg.lerp(hoverBg, hoverAnim).lerp(activeBg, activeAnim)
    val currentBorder = colors.borderHairline.lerp(colors.borderActive, hoverAnim).lerp(colors.blue, activeAnim)
    val textColor = colors.textSecondary.lerp(colors.textPrimary, hoverAnim).lerp(Color.White, activeAnim)

    Box(
        modifier = Modifier
            .width(itemWidth)
            .radius(shapes.xs)
            .background(currentBg)
            .border(1f, currentBorder)
            .hoverable { isHovered = it }
            .clickable { onClick() }
            .pad(horizontal = 4f, vertical = 5f)
    ) {
        MonoText(
            text = label,
            color = textColor,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
