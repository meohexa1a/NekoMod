package org.mdt.ui.components.surface

import androidx.compose.runtime.Composable
import org.mdt.core.ui.compose.*
import org.mdt.ui.components.layout.Box
import org.mdt.ui.theme.GlassMaterialPreset
import org.mdt.ui.theme.Theme
import org.mdt.ui.theme.glassMaterial

/**
 * ## CardVariant
 *
 * Surface styling variants for cards and containers.
 */
enum class CardVariant {
    /** Apple Frosted Glass with 2-pass Gaussian backdrop blur, specular border, and ambient shadow. */
    GLASS,

    /** Opaque elevated surface with prominent drop shadow. */
    ELEVATED,

    /** Translucent surface with subtle hairline border and zero blur. */
    OUTLINED,

    /** Flat solid colored card. */
    SOLID
}

/**
 * ## Card
 *
 * Apple iOS/macOS-style surface container featuring multi-pass Frosted Glass blur,
 * specular highlight borders, and soft diffuse drop shadows.
 *
 * @param modifier Chainable [UIModifier]. Use `Modifier.pad(...)` or `Modifier.radius(...)` to customize bounds.
 * @param variant Visual style variant ([CardVariant.GLASS], [CardVariant.ELEVATED], etc.).
 * @param content Declarative child UI tree.
 *
 * See: docs/design-system/design_system_en.md
 */
@Composable
fun Card(
    modifier: UIModifier = UIModifier,
    variant: CardVariant = CardVariant.GLASS,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = Theme.colors
    val shapes = Theme.shapes

    val baseModifier = when (variant) {
        CardVariant.GLASS -> Modifier.glassMaterial(
            preset = GlassMaterialPreset.REGULAR,
            radius = shapes.lg,
            tint = colors.surfaceSecondary,
            border = colors.borderRegular
        )
        CardVariant.ELEVATED -> Modifier
            .radius(shapes.lg)
            .background(colors.surfaceElevated)
            .border(1f, colors.borderHairline)
            .shadow(colors.shadowKey, offsetX = 0f, offsetY = -4f, blur = 16f, spread = 2f)
        CardVariant.OUTLINED -> Modifier
            .radius(shapes.lg)
            .background(colors.surfacePrimary)
            .border(1f, colors.borderRegular)
        CardVariant.SOLID -> Modifier
            .radius(shapes.lg)
            .background(colors.surfacePrimary)
    }

    Box(
        modifier = baseModifier
            .pad(16f)
            .then(modifier),
        content = content
    )
}
