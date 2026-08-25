package org.mdt.ui.render

import arc.Core
import arc.graphics.Color
import arc.graphics.Gl
import arc.graphics.Texture
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.gl.Shader
import org.mdt.ui.components.layout.BoxVisuals

/**
 * ## BoxRenderer
 *
 * High-performance GPU SDF renderer for rounded rectangles, borders, shadows,
 * glow, and glassmorphic backdrop blur.
 *
 * See: docs/rendering-shaders/rendering_shaders_en.md
 */
object BoxRenderer {
    @JvmField
    var blurEnabled = false

    fun draw(
        x: Float, y: Float,
        w: Float, h: Float,
        visuals: BoxVisuals,
        blur: BoxBlur
    ) {
        if (w <= 0.001f || h <= 0.001f) return
        Shaders.ensure()
        val backdrop = if (blurEnabled) blur.getBlurredTexture(visuals) else null
        drawContent(visuals, x, y, w, h, backdrop, blur.screenWidth, blur.screenHeight)
    }

    private fun drawContent(
        visuals: BoxVisuals,
        x: Float, y: Float,
        w: Float, h: Float,
        backdrop: Texture?,
        screenWidth: Float,
        screenHeight: Float
    ) {
        Draw.flush()
        val prevShader = Draw.getShader()
        val s = Shaders.mainShader ?: return

        Draw.shader(s)
        s.bind()
        applyCommon(s, visuals, w, h)
        applyFill(s, visuals)
        applyBorder(s, visuals, w, h)
        applyInnerShadow(s, visuals)
        applyGlow(s, visuals)
        applyBackdrop(s, visuals, backdrop, x, y, w, h, screenWidth, screenHeight)
        applyFilter(s, visuals)

        Gl.activeTexture(Gl.texture0)
        Core.atlas.white().texture.bind()
        Draw.flush()

        val glowMargin = if (hasGlow(visuals)) (visuals.glowSpread + visuals.glowBlur) else 0f
        val margin = 1f + glowMargin
        val qx = x - margin
        val qy = y - margin
        val qw = w + margin * 2f
        val qh = h + margin * 2f

        val safeW = maxOf(w, 0.001f)
        val safeH = maxOf(h, 0.001f)
        val u0 = -margin / safeW
        val v0 = -margin / safeH
        val u1 = 1f + margin / safeW
        val v1 = 1f + margin / safeH

        Draw.color(Color.white)
        Fill.quad(
            Core.atlas.white().texture,
            qx, qy, Draw.getColor().toFloatBits(), u0, v0,
            qx, qy + qh, Draw.getColor().toFloatBits(), u0, v1,
            qx + qw, qy + qh, Draw.getColor().toFloatBits(), u1, v1,
            qx + qw, qy, Draw.getColor().toFloatBits(), u1, v0
        )
        Draw.flush()
        Draw.shader(prevShader)
        Gl.activeTexture(Gl.texture0)
    }

    private fun applyCommon(s: Shader, v: BoxVisuals, w: Float, h: Float) {
        s.setUniformf("u_size", w, h)
        s.setUniformf("u_opacity", v.opacity)
        s.setUniformf(
            "u_cornerRadii",
            v.topLeftRadius, v.topRightRadius,
            v.bottomRightRadius, v.bottomLeftRadius
        )
        s.setUniformf("u_edgeSoftness", 1f)
        s.setUniformf("u_fillMode", v.backgroundMode.fillMode.toFloat())
        s.setUniformf("u_fillColor", v.fillColor.r, v.fillColor.g, v.fillColor.b, v.fillColor.a)
    }

    private fun applyFill(s: Shader, v: BoxVisuals) {
        if (v.backgroundMode != BoxVisuals.BackgroundMode.TEXTURE || v.fillTexture == null) return
        s.setUniformf("u_uvScale", v.uvScaleX, v.uvScaleY)
        s.setUniformf("u_uvOffset", v.uvOffsetX, v.uvOffsetY)
        Gl.activeTexture(Gl.texture0 + Shaders.TEX_UNIT_FILL)
        v.fillTexture!!.bind()
    }

    private fun applyBorder(s: Shader, v: BoxVisuals, w: Float, h: Float) {
        if (v.borderWidth > 0.001f) {
            s.setUniformf("u_borderWidth", minOf(v.borderWidth, minOf(w, h) * 0.5f))
            s.setUniformf("u_borderColor", v.borderColor.r, v.borderColor.g, v.borderColor.b, v.borderColor.a)
            s.setUniformf("u_borderStyle", v.borderStyle.value.toFloat())
            if (v.borderStyle != BoxVisuals.BorderStyle.SOLID) {
                s.setUniformf("u_dashLength", maxOf(v.dashLength, 0.1f))
                s.setUniformf("u_dashRatio", v.dashRatio)
            }
        } else {
            s.setUniformf("u_borderWidth", 0f)
        }
    }

    private fun applyInnerShadow(s: Shader, v: BoxVisuals) {
        if (v.innerShadowColor.a > 0.001f && (v.innerShadowSpread > 0.001f || v.innerShadowBlur > 0.001f)) {
            s.setUniformf(
                "u_innerShadowColor",
                v.innerShadowColor.r, v.innerShadowColor.g, v.innerShadowColor.b, v.innerShadowColor.a
            )
            s.setUniformf("u_innerShadowSpread", v.innerShadowSpread)
            s.setUniformf("u_innerShadowBlur", v.innerShadowBlur)
        } else {
            s.setUniformf("u_innerShadowColor", 0f, 0f, 0f, 0f)
        }
    }

    private fun hasGlow(v: BoxVisuals): Boolean =
        v.glowColor.a > 0.001f && (v.glowSpread > 0.001f || v.glowBlur > 0.001f)

    private fun applyGlow(s: Shader, v: BoxVisuals) {
        if (hasGlow(v)) {
            s.setUniformf("u_glowColor", v.glowColor.r, v.glowColor.g, v.glowColor.b, v.glowColor.a)
            s.setUniformf("u_glowSpread", v.glowSpread)
            s.setUniformf("u_glowBlur", v.glowBlur)
        } else {
            s.setUniformf("u_glowColor", 0f, 0f, 0f, 0f)
        }
    }

    private fun applyBackdrop(
        s: Shader,
        v: BoxVisuals,
        backdrop: Texture?,
        x: Float, y: Float,
        w: Float, h: Float,
        screenWidth: Float,
        screenHeight: Float
    ) {
        if (backdrop != null && v.backgroundMode == BoxVisuals.BackgroundMode.BACKDROP && screenWidth > 0f && screenHeight > 0f) {
            s.setUniformf("u_backdropWeight", v.backdropWeight)
            val u0 = (x / screenWidth).coerceIn(0f, 1f)
            val v0 = (y / screenHeight).coerceIn(0f, 1f)
            val u1 = ((x + w) / screenWidth).coerceIn(0f, 1f)
            val v1 = ((y + h) / screenHeight).coerceIn(0f, 1f)
            s.setUniformf("u_backdropCoords", u0, v0, u1, v1)
            s.setUniformf("u_backdropBlend", v.backdropBlend)
            s.setUniformf("u_backdropMinAlpha", v.backdropMinAlpha)
            Gl.activeTexture(Gl.texture0 + Shaders.TEX_UNIT_BACKDROP)
            backdrop.bind()
        } else {
            s.setUniformf("u_backdropWeight", 0f)
        }
    }

    private fun applyFilter(s: Shader, v: BoxVisuals) {
        if (v.filterMode != BoxVisuals.FilterMode.NONE && v.filterAmount > 0.001f) {
            s.setUniformf("u_colorFilter", v.filterMode.value.toFloat(), v.filterAmount, 0f, 0f)
        } else {
            s.setUniformf("u_colorFilter", 0f, 0f, 0f, 0f)
        }
        s.setUniformf("u_noiseAmount", v.noiseAmount)
    }
}
