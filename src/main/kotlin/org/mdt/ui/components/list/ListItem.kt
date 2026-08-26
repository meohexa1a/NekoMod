@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.list

import androidx.compose.runtime.*
import arc.graphics.Color
import org.mdt.core.engine.image.ImageSource
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.display.image.Image
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.surface.Divider
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.Theme

/**
 * ## ListItem
 *
 * Single row item within an Apple iOS [GroupedList].
 * Supports optional leading icon, title, subtitle, trailing action/switch/badge, and bottom divider.
 *
 * @param title Primary item title text.
 * @param subtitle Optional secondary subtitle or description.
 * @param icon Optional leading icon source.
 * @param iconTint Tint color for the leading icon.
 * @param showDivider Whether to render a subtle bottom separator line.
 * @param onClick Optional row click listener.
 * @param modifier Chainable [UIModifier].
 * @param trailing Optional trailing component slot (e.g. Toggle, Badge, Chevron).
 */
@Composable
fun ListItem(
    title: String,
    subtitle: String? = null,
    icon: Any? = null,
    iconTint: Color = Theme.colors.systemBlue,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: UIModifier = UIModifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = Theme.colors
    var isHovered by remember { mutableStateOf(false) }

    val rowBg = if (isHovered && onClick != null) colors.glassThin else Color.clear

    Column(modifier = Modifier.fillMaxWidth().then(modifier)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(rowBg)
                .pad(horizontal = 14f, vertical = 10f)
                .hoverable { isHovered = it }
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        ) {
            Row(
                arrangement = Arrangement.spacedBy(12f),
                alignment = Alignment.CenterStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Leading Icon
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(28f)
                            .radius(Theme.shapes.medium)
                            .background(iconTint.cpy().apply { a = 0.20f })
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

                // Title & Subtitle Stack
                Column(
                    arrangement = Arrangement.spacedBy(2f),
                    alignment = Alignment.TopStart,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        color = colors.textPrimary
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = colors.textTertiary
                        )
                    }
                }

                // Trailing Slot
                if (trailing != null) {
                    trailing()
                }
            }
        }

        if (showDivider) {
            Divider(
                color = colors.divider,
                modifier = Modifier.pad(left = if (icon != null) 54f else 14f)
            )
        }
    }
}
