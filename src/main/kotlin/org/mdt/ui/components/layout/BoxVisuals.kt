package org.mdt.ui.components.layout

import arc.graphics.Color
import arc.graphics.Texture
import arc.graphics.g2d.TextureRegion

/**
 * ## BoxVisuals
 *
 * Visual configuration data class storing background color, gradient,
 * texture fill with [ScaleMode], corner radii, border thickness, drop shadows, glow, and backdrop filters.
 *
 * See: docs/rendering-shaders/rendering_shaders_en.md
 */
class BoxVisuals {
    enum class BackgroundMode(val fillMode: Int) {
        COLOR(0),
        TEXTURE(1),
        BACKDROP(2)
    }

    enum class BorderStyle(val value: Int) {
        SOLID(0),
        DASHED(1),
        DOTTED(2)
    }

    enum class FilterMode(val value: Int) {
        NONE(0),
        GRAYSCALE(1),
        SEPIA(2),
        INVERT(3)
    }

    var backgroundMode: BackgroundMode = BackgroundMode.COLOR
    var fillColor: Color = Color(Color.clear)
    var fillTexture: Texture? = null
    var fillRegion: TextureRegion? = null
    var scaleMode: ScaleMode = ScaleMode.FIT
    var uvScaleX: Float = 1f
    var uvScaleY: Float = 1f
    var uvOffsetX: Float = 0f
    var uvOffsetY: Float = 0f

    // Radii
    var topLeftRadius: Float = 0f
    var topRightRadius: Float = 0f
    var bottomRightRadius: Float = 0f
    var bottomLeftRadius: Float = 0f

    fun setRadius(r: Float) {
        topLeftRadius = r
        topRightRadius = r
        bottomRightRadius = r
        bottomLeftRadius = r
    }

    // Border
    var borderWidth: Float = 0f
    var borderColor: Color = Color(Color.clear)
    var borderStyle: BorderStyle = BorderStyle.SOLID
    var dashLength: Float = 8f
    var dashRatio: Float = 0.5f

    // Outer Shadow
    var shadowColor: Color = Color(Color.clear)
    var shadowOffsetX: Float = 0f
    var shadowOffsetY: Float = 0f
    var shadowBlur: Float = 0f
    var shadowSpread: Float = 0f

    // Inner Shadow
    var innerShadowColor: Color = Color(Color.clear)
    var innerShadowSpread: Float = 0f
    var innerShadowBlur: Float = 0f

    // Glow
    var glowColor: Color = Color(Color.clear)
    var glowSpread: Float = 0f
    var glowBlur: Float = 0f

    // Backdrop
    var blur: Boolean = false
    var blurRadius: Float = 12f
    var blurIterations: Int = 2
    var backdropTint: Color = Color(Color.white)
    var backdropWeight: Float = 1.0f
    var backdropBlend: Float = 0.5f
    var backdropMinAlpha: Float = 0.1f

    // Filter
    var filterMode: FilterMode = FilterMode.NONE
    var filterAmount: Float = 0f
    var noiseAmount: Float = 0f

    // Master opacity
    var opacity: Float = 1.0f

    fun isVisible(): Boolean {
        if (opacity <= 0.001f) return false
        if (fillColor.a > 0.001f) return true
        if (fillTexture != null || fillRegion != null) return true
        if (borderWidth > 0f && borderColor.a > 0.001f) return true
        if (shadowColor.a > 0.001f && (shadowBlur > 0f || shadowSpread > 0f)) return true
        if (innerShadowColor.a > 0.001f) return true
        if (glowColor.a > 0.001f && (glowSpread > 0f || glowBlur > 0f)) return true
        if (blur || backgroundMode == BackgroundMode.BACKDROP) return true
        return false
    }

    fun texture(
        region: TextureRegion,
        scaleMode: ScaleMode = ScaleMode.FIT,
        tint: Color = Color.white
    ): BoxVisuals {
        this.backgroundMode = BackgroundMode.TEXTURE
        this.fillRegion = region
        this.fillTexture = region.texture
        this.scaleMode = scaleMode
        this.fillColor.set(tint)
        return this
    }

    fun texture(
        tex: Texture,
        scaleMode: ScaleMode = ScaleMode.FIT,
        tint: Color = Color.white
    ): BoxVisuals {
        return texture(TextureRegion(tex), scaleMode, tint)
    }

    /**
     * Dynamically computes shader UV scale and offsets based on [fillRegion] bounds and [scaleMode].
     */
    fun computeUVs(w: Float, h: Float) {
        val reg = fillRegion
        if (reg == null) {
            uvScaleX = 1f
            uvScaleY = 1f
            uvOffsetX = 0f
            uvOffsetY = 0f
            return
        }

        val rw = reg.width.toFloat()
        val rh = reg.height.toFloat()
        if (rw <= 0f || rh <= 0f || w <= 0f || h <= 0f) return

        val u1 = reg.u
        val u2 = reg.u2
        val vTop = reg.v
        val vBottom = reg.v2

        val du = u2 - u1
        val dv = vTop - vBottom // In Arc, vTop is top, vBottom is bottom

        when (scaleMode) {
            ScaleMode.STRETCH -> {
                uvScaleX = du
                uvScaleY = dv
                uvOffsetX = u1
                uvOffsetY = vBottom
            }
            ScaleMode.CROP -> {
                val boxRatio = w / h
                val texRatio = rw / rh
                if (boxRatio > texRatio) {
                    // Box is wider: fill width, crop height
                    val s = (h / w) / (rh / rw)
                    val cropV = dv * s
                    val offsetV = vBottom + dv * (1f - s) * 0.5f
                    uvScaleX = du
                    uvScaleY = cropV
                    uvOffsetX = u1
                    uvOffsetY = offsetV
                } else {
                    // Box is taller: fill height, crop width
                    val s = (w / h) / (rw / rh)
                    val cropU = du * s
                    val offsetU = u1 + du * (1f - s) * 0.5f
                    uvScaleX = cropU
                    uvScaleY = dv
                    uvOffsetX = offsetU
                    uvOffsetY = vBottom
                }
            }
            ScaleMode.FIT -> {
                val boxRatio = w / h
                val texRatio = rw / rh
                if (boxRatio > texRatio) {
                    // Box is wider: fit height, letterbox width
                    val s = (w / h) / (rw / rh)
                    val scaledU = du * s
                    val offsetU = u1 - du * (s - 1f) * 0.5f
                    uvScaleX = scaledU
                    uvScaleY = dv
                    uvOffsetX = offsetU
                    uvOffsetY = vBottom
                } else {
                    // Box is taller: fit width, letterbox height
                    val s = (h / w) / (rh / rw)
                    val scaledV = dv * s
                    val offsetV = vBottom - dv * (s - 1f) * 0.5f
                    uvScaleX = du
                    uvScaleY = scaledV
                    uvOffsetX = u1
                    uvOffsetY = offsetV
                }
            }
            ScaleMode.CENTER -> {
                val scaleX = w / rw
                val scaleY = h / rh
                val scaledU = du * scaleX
                val scaledV = dv * scaleY
                val offsetU = u1 - du * (scaleX - 1f) * 0.5f
                val offsetV = vBottom - dv * (scaleY - 1f) * 0.5f
                uvScaleX = scaledU
                uvScaleY = scaledV
                uvOffsetX = offsetU
                uvOffsetY = offsetV
            }
        }
    }

    fun radius(uniformRadius: Float): BoxVisuals {
        this.topLeftRadius = uniformRadius
        this.topRightRadius = uniformRadius
        this.bottomRightRadius = uniformRadius
        this.bottomLeftRadius = uniformRadius
        return this
    }

    fun radius(tl: Float, tr: Float, br: Float, bl: Float): BoxVisuals {
        this.topLeftRadius = tl
        this.topRightRadius = tr
        this.bottomRightRadius = br
        this.bottomLeftRadius = bl
        return this
    }

    fun border(width: Float, color: Color, style: BorderStyle = BorderStyle.SOLID): BoxVisuals {
        this.borderWidth = width
        this.borderColor.set(color)
        this.borderStyle = style
        return this
    }

    fun shadow(
        color: Color,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        blur: Float = 8f,
        spread: Float = 0f
    ): BoxVisuals {
        this.shadowColor.set(color)
        this.shadowOffsetX = offsetX
        this.shadowOffsetY = offsetY
        this.shadowBlur = blur
        this.shadowSpread = spread
        return this
    }

    fun innerShadow(
        color: Color,
        blur: Float = 8f,
        spread: Float = 0f
    ): BoxVisuals {
        this.innerShadowColor.set(color)
        this.innerShadowBlur = blur
        this.innerShadowSpread = spread
        return this
    }

    fun glow(color: Color, spread: Float = 6f, blur: Float = 12f): BoxVisuals {
        this.glowColor.set(color)
        this.glowSpread = spread
        this.glowBlur = blur
        return this
    }

    fun backdrop(blur: Boolean = true, blurRadius: Float = 12f): BoxVisuals {
        this.blur = blur
        this.backgroundMode = BackgroundMode.BACKDROP
        return this
    }
}
