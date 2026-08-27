package org.mdt.ui.screens.editor.inspector

import androidx.compose.runtime.Composable
import org.mdt.core.ui.compose.*
import org.mdt.core.ui.layout.Alignment
import org.mdt.core.ui.layout.Arrangement
import org.mdt.ui.components.display.image.Image
import org.mdt.ui.components.layout.*
import org.mdt.ui.components.surface.Divider
import org.mdt.ui.components.text.MonoText
import org.mdt.ui.components.text.Text
import org.mdt.ui.theme.StudioIcons
import org.mdt.ui.theme.Theme

/**
 * ## ComponentPropSchema
 *
 * Parametric prop definition for Component Schema Editor.
 */
data class ComponentPropSchema(
    val name: String,
    val type: String,
    val defaultValue: String
)

/**
 * ## InspectorComponentSchemaTab
 *
 * Inspector panel displaying the parametric contract and schema for a Component.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun InspectorComponentSchemaTab(
    selectedComponent: String = "PrimaryButton"
) {
    val colors = Theme.colors
    val shapes = Theme.shapes
    val spacing = Theme.spacing
    val typography = Theme.typography

    Column(
        arrangement = Arrangement.spacedBy(spacing.md),
        alignment = Alignment.CenterStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            arrangement = Arrangement.spacedBy(spacing.sm),
            alignment = Alignment.CenterStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                source = StudioIcons.COMPONENTS,
                modifier = Modifier.size(16f),
                tint = colors.blue
            )
            Text(text = selectedComponent, color = colors.textPrimary, font = typography.body)
            Spacer(modifier = Modifier.weight(1.0f))
            MonoText(text = "<Component>", color = colors.textTertiary, scale = 0.85f)
        }

        Divider(modifier = Modifier.fillMaxWidth().height(1f))

        Text(text = "Exposed Props Schema", color = colors.textSecondary, font = typography.title)

        val sampleProps = listOf(
            ComponentPropSchema("text", "String", "Button"),
            ComponentPropSchema("radius", "Float", "8f"),
            ComponentPropSchema("background", "Color", "#0a84ff")
        )

        for (prop in sampleProps) {
            Row(
                arrangement = Arrangement.spacedBy(spacing.xs),
                alignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .radius(shapes.sm)
                    .background(colors.surfaceSecondary)
                    .border(1f, colors.borderHairline)
                    .pad(spacing.sm)
            ) {
                MonoText(text = "@${prop.name}", color = colors.blue, scale = 0.9f)
                Spacer(modifier = Modifier.weight(1.0f))
                MonoText(text = "${prop.type} = \"${prop.defaultValue}\"", color = colors.textTertiary, scale = 0.85f)
            }
        }
    }
}
