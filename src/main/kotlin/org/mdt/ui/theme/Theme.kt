package org.mdt.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * ## NekoThemeData
 *
 * Immutable container encapsulating Apple-inspired colors, shapes, spacing grid, and typography.
 */
data class NekoThemeData(
    val colors: ColorTokens = ColorTokens(),
    val shapes: ShapeTokens = ShapeTokens(),
    val spacing: SpacingTokens = SpacingTokens(),
    val typography: TypographyTokens = TypographyTokens()
)

val LocalTheme = staticCompositionLocalOf { NekoThemeData() }

/**
 * Global accessor for current theme tokens in Compose contexts.
 */
object Theme {
    val colors: ColorTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalTheme.current.colors

    val shapes: ShapeTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalTheme.current.shapes

    val spacing: SpacingTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalTheme.current.spacing

    val typography: TypographyTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalTheme.current.typography
}

/**
 * ## NekoTheme
 *
 * Root theme provider composable wrapping the UI tree with Apple macOS / iOS design tokens.
 *
 * @param theme Custom theme definition (defaults to standard Apple dark acrylic glass).
 * @param content Declarative child UI content.
 */
@Composable
fun NekoTheme(
    theme: NekoThemeData = NekoThemeData(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalTheme provides theme) {
        content()
    }
}
