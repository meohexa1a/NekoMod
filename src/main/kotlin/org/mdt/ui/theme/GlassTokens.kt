package org.mdt.ui.theme

import arc.graphics.Color
import org.mdt.core.ui.compose.*
import org.mdt.ui.components.layout.BoxVisuals

/**
 * ## GlassMaterialPreset
 *
 * Predefined multi-pass Gaussian backdrop blur and vibrancy tints matching Apple iOS Material design.
 */
enum class GlassMaterialPreset(
    val blurRadius: Float,
    val weight: Float,
    val blend: Float,
    val tintAlpha: Float,
    val iterations: Int
) {
    /** Ultra-thin translucent surface for navigation bars and floating toolbars. */
    ULTRA_THIN(blurRadius = 8f, weight = 0.7f, blend = 0.7f, tintAlpha = 0.08f, iterations = 2),

    /** Standard thin glass for segmented controls and cards. */
    THIN(blurRadius = 12f, weight = 0.85f, blend = 0.80f, tintAlpha = 0.14f, iterations = 2),

    /** Regular frosted glass for prominent panels and sidebars. */
    REGULAR(blurRadius = 16f, weight = 0.95f, blend = 0.88f, tintAlpha = 0.20f, iterations = 2),

    /** Thick opaque glass for modal dialogs and alert popups. */
    THICK(blurRadius = 24f, weight = 1.0f, blend = 0.95f, tintAlpha = 0.30f, iterations = 3)
}

/**
 * Applies Apple-style Frosted Glass backdrop blur with hairline specular border.
 *
 * @param preset Material blur and weight preset.
 * @param radius Corner radius in pixels (default 16f).
 * @param tint Custom glass tint color (defaults to Apple dark glass tone).
 * @param border Custom specular highlight border color.
 * @param shadow Ambient drop shadow color.
 */
fun UIModifier.glassMaterial(
    preset: GlassMaterialPreset = GlassMaterialPreset.REGULAR,
    radius: Float = 16f,
    tint: Color = Color(0.08f, 0.09f, 0.13f, preset.tintAlpha),
    border: Color = Color(1f, 1f, 1f, 0.12f),
    shadow: Color = Color(0f, 0f, 0f, 0.35f)
): UIModifier = this
    .radius(radius)
    .background(tint)
    .backdrop(
        blur = true,
        blurRadius = preset.blurRadius,
        weight = preset.weight,
        blend = preset.blend,
        tint = tint,
        iterations = preset.iterations
    )
    .border(width = 1f, color = border, style = BoxVisuals.BorderStyle.SOLID)
    .shadow(
        color = shadow,
        offsetX = 0f,
        offsetY = -2f,
        blur = 12f,
        spread = 2f
    )
