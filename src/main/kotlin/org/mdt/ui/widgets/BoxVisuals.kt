package org.mdt.ui.widgets

import arc.graphics.Color
import arc.graphics.Texture
import mindustry.graphics.Pal

/**
 * ## BoxVisuals
 *
 * Visual styling data container for [BoxNode].
 * Manages background colors, textures, borders, shadows, glow, and parameterized backdrop blur.
 *
 * See: docs/rendering-shaders/rendering_shaders_en.md
 */
class BoxVisuals {
    enum class BackgroundMode(val fillMode: Int) {
        SOLID(0), TEXTURE(1), BACKDROP(2)
    }

    enum class BorderStyle(val value: Int) {
        SOLID(0), DASHED(1), DOTTED(2)
    }

    enum class FilterMode(val value: Int) {
        NONE(0), GRAYSCALE(1), SEPIA(2), BRIGHTNESS(3), INVERT(4)
    }

    /** Visual opacity (0.0 = transparent, 1.0 = fully opaque). */
    var opacity: Double = 1.0

    /** Top-left corner radius in pixels. */
    var topLeftRadius: Double = 8.0
    /** Top-right corner radius in pixels. */
    var topRightRadius: Double = 8.0
    /** Bottom-right corner radius in pixels. */
    var bottomRightRadius: Double = 8.0
    /** Bottom-left corner radius in pixels. */
    var bottomLeftRadius: Double = 8.0

    /** Border stroke width in pixels. */
    var borderWidth: Double = 0.0
    /** Border stroke color. */
    var borderColor: Color = Color(Color.white)
    /** Border stroke pattern style. */
    var borderStyle: BorderStyle = BorderStyle.SOLID
    /** Dash length for dashed border style. */
    var dashLength: Double = 10.0
    /** Dash to gap ratio for dashed border style. */
    var dashRatio: Double = 0.5

    // ==========================================
    // BACKDROP BLUR PARAMETERS
    // ==========================================

    /** Whether backdrop blur is enabled for this box. */
    var blur: Boolean = false
    /** Blur kernel radius spread (pixels). */
    var blurRadius: Float = 4f
    /** Number of ping-pong blur convolution passes (1 = subtle, 4 = heavy frosted glass). */
    var blurIterations: Int = 2
    /** Alpha blending weight of the blurred backdrop over background color (0.0 to 1.0). */
    var backdropWeight: Double = 0.8
    /** Blending curve exponent (0.0 to 1.0). */
    var backdropBlend: Double = 0.8
    /** Minimum alpha threshold for backdrop rendering. */
    var backdropMinAlpha: Double = 0.8
    /** Tint overlay color applied to the blurred backdrop texture. */
    var backdropTint: Color = Color(Color.white)

    // ==========================================
    // INNER SHADOW & GLOW
    // ==========================================

    var innerShadowColor: Color = Color(Pal.shadow)
    var innerShadowSpread: Double = 0.0
    var innerShadowBlur: Double = 0.0

    var glowColor: Color = Color(Pal.shadow)
    var glowSpread: Double = 0.0
    var glowBlur: Double = 0.0

    var filterMode: FilterMode = FilterMode.NONE
    var filterAmount: Double = 0.0
    var noiseAmount: Double = 0.0

    var uvScaleX: Double = 1.0
    var uvScaleY: Double = 1.0
    var uvOffsetX: Double = 0.0
    var uvOffsetY: Double = 0.0

    var fillColor: Color = Color(Color.darkGray)
    var fillTexture: Texture? = null
    var backgroundMode: BackgroundMode = BackgroundMode.SOLID

    // ==========================================
    // FLUENT BUILDER HELPERS
    // ==========================================

    fun radius(all: Double) {
        topLeftRadius = all; topRightRadius = all
        bottomLeftRadius = all; bottomRightRadius = all
    }

    fun radius(tl: Double, tr: Double, br: Double, bl: Double) {
        topLeftRadius = tl; topRightRadius = tr
        bottomRightRadius = br; bottomLeftRadius = bl
    }

    fun color(color: Color) {
        fillColor.set(color)
        backgroundMode = BackgroundMode.SOLID
    }

    fun border(width: Double, color: Color = Color.white, style: BorderStyle = BorderStyle.SOLID) {
        borderWidth = width
        borderColor.set(color)
        borderStyle = style
    }

    fun shadow(color: Color = Pal.shadow, spread: Double = 4.0, blur: Double = 8.0) {
        innerShadowColor.set(color)
        innerShadowSpread = spread
        innerShadowBlur = blur
    }

    fun glow(color: Color, spread: Double = 6.0, blur: Double = 12.0) {
        glowColor.set(color)
        glowSpread = spread
        glowBlur = blur
    }

    fun backdrop(
        blur: Boolean = true,
        radius: Float = 4f,
        weight: Double = 0.8,
        blend: Double = 0.8,
        tint: Color = Color.white,
        iterations: Int = 2
    ) {
        this.blur = blur
        this.blurRadius = radius
        this.blurIterations = iterations
        this.backdropWeight = weight
        this.backdropBlend = blend
        this.backdropTint.set(tint)
        this.backgroundMode = BackgroundMode.BACKDROP
    }

    fun texture(tex: Texture, scaleX: Double = 1.0, scaleY: Double = 1.0) {
        fillTexture = tex
        uvScaleX = scaleX
        uvScaleY = scaleY
        backgroundMode = BackgroundMode.TEXTURE
    }

    fun copyFrom(other: BoxVisuals) {
        opacity = other.opacity
        topLeftRadius = other.topLeftRadius
        topRightRadius = other.topRightRadius
        bottomRightRadius = other.bottomRightRadius
        bottomLeftRadius = other.bottomLeftRadius
        borderWidth = other.borderWidth
        borderColor.set(other.borderColor)
        borderStyle = other.borderStyle
        dashLength = other.dashLength
        dashRatio = other.dashRatio
        blur = other.blur
        blurRadius = other.blurRadius
        blurIterations = other.blurIterations
        backdropWeight = other.backdropWeight
        backdropBlend = other.backdropBlend
        backdropMinAlpha = other.backdropMinAlpha
        backdropTint.set(other.backdropTint)
        innerShadowColor.set(other.innerShadowColor)
        innerShadowSpread = other.innerShadowSpread
        innerShadowBlur = other.innerShadowBlur
        glowColor.set(other.glowColor)
        glowSpread = other.glowSpread
        glowBlur = other.glowBlur
        filterMode = other.filterMode
        filterAmount = other.filterAmount
        noiseAmount = other.noiseAmount
        uvScaleX = other.uvScaleX
        uvScaleY = other.uvScaleY
        uvOffsetX = other.uvOffsetX
        uvOffsetY = other.uvOffsetY
        fillColor.set(other.fillColor)
        fillTexture = other.fillTexture
        backgroundMode = other.backgroundMode
    }
}
