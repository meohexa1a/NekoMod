package org.mdt.ui.screens.editor.components

import androidx.compose.runtime.Composable
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.surface.Toggle
import org.mdt.ui.components.layout.Box
import org.mdt.ui.components.layout.Column
import org.mdt.ui.components.layout.Row
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.Theme

/**
 * ## EditorToggleRow
 *
 * Standardized setting row combining title, description, and interactive boolean toggle switch.
 *
 * @param title Primary setting name.
 * @param description Optional explanatory subtitle.
 * @param checked Current switch state.
 * @param onToggle Callback invoked on state change.
 * @param modifier Chainable [UIModifier].
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun EditorToggleRow(
    title: String,
    description: String? = null,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: UIModifier = UIModifier
) {
    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing
    val typography = Theme.typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .radius(shapes.sm)
            .background(colors.surfacePrimary)
            .border(1f, colors.borderHairline)
            .pad(spacing.md)
            .then(modifier)
    ) {
        Row(
            arrangement = Arrangement.spacedBy(spacing.md),
            alignment = Alignment.CenterStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                arrangement = Arrangement.spacedBy(2f),
                modifier = Modifier.weight(1.0f)
            ) {
                Text(
                    text = title,
                    font = typography.body,
                    color = colors.textPrimary
                )
                if (description != null) {
                    Text(
                        text = description,
                        font = typography.caption,
                        color = colors.textSecondary
                    )
                }
            }

            Toggle(
                checked = checked,
                onToggle = onToggle
            )
        }
    }
}
