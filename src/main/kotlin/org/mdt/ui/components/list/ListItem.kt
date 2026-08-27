package org.mdt.ui.components.list

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import org.mdt.core.engine.image.ImageSource
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.display.Image
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.Theme

/**
 * ## ListItem (Slot-based)
 *
 * Highly flexible Apple iOS/macOS TableView row supporting arbitrary custom composables
 * in leading, title, subtitle, and trailing positions.
 *
 * @param headlineContent Primary headline composable.
 * @param modifier Chainable [UIModifier].
 * @param supportingContent Optional secondary subtitle composable.
 * @param leadingContent Optional leading widget slot (Avatar, Icon, Checkbox, Badge).
 * @param trailingContent Optional trailing action slot (Toggle, Chevron, Readout, Button).
 * @param onClick Optional click action callback.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun ListItem(
    headlineContent: @Composable () -> Unit,
    modifier: UIModifier = UIModifier,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = Theme.colors
    var isHovered by remember { mutableStateOf(false) }

    val hoverAnim by animateFloatAsState(
        targetValue = if (isHovered && onClick != null) 1f else 0f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.glassThin.withAlpha(colors.glassThin.a * hoverAnim))
            .pad(horizontal = 14f, vertical = 10f)
            .hoverable { isHovered = it }
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .then(modifier)
    ) {
        Row(
            arrangement = Arrangement.spacedBy(12f),
            alignment = Alignment.CenterStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Leading Slot
            if (leadingContent != null) {
                leadingContent()
            }

            // Central Headline & Supporting Stack
            Column(
                arrangement = Arrangement.spacedBy(2f),
                alignment = Alignment.TopStart,
                modifier = Modifier.weight(1f)
            ) {
                headlineContent()
                if (supportingContent != null) {
                    supportingContent()
                }
            }

            // Trailing Slot
            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

/**
 * ## ListItem (Convenience Overload)
 *
 * Standard text & icon convenience overload for simple list rows.
 */
@Composable
fun ListItem(
    title: String,
    modifier: UIModifier = UIModifier,
    subtitle: String? = null,
    icon: Any? = null,
    iconTint: Color = Theme.colors.textPrimary,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = Theme.colors

    ListItem(
        headlineContent = {
            Text(text = title, color = colors.textPrimary)
        },
        supportingContent = if (subtitle != null) {
            { Text(text = subtitle, color = colors.textTertiary) }
        } else null,
        leadingContent = if (icon != null) {
            {
                Box(
                    modifier = Modifier
                        .size(28f)
                        .radius(Theme.shapes.md)
                        .background(iconTint.withAlpha(0.20f))
                ) {
                    Image(
                        source = ImageSource.from(icon),
                        tint = iconTint,
                        modifier = Modifier
                            .size(16f)
                            .align(Alignment.Center)
                    )
                }
            }
        } else null,
        trailingContent = trailing,
        onClick = onClick,
        modifier = modifier
    )
}
