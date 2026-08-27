package org.mdt.core.ui.render

import arc.Core
import arc.graphics.Gl
import arc.graphics.Texture
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.gl.Shader
import org.mdt.core.ui.graphics.Color
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
    var blurEnabled = true

    fun draw(
        x: Float, y: Float,
        w: Float, h: Float,
        visuals: BoxVisuals
    ) {
        if (w <= 0.001f || h <= 0.001f) return
        Shaders.ensure()
        val backdrop = if (blurEnabled) BoxBlur.getBlurredTexture(visuals) else null
        val screenW = if (Core.graphics != null && Core.graphics.width > 0) Core.graphics.width.toFloat() else 1920f
        val screenH = if (Core.graphics != null && Core.graphics.height > 0) Core.graphics.height.toFloat() else 1080f
        drawContent(visuals, x, y, w, h, backdrop, screenW, screenH)
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
        applyFill(s, visuals, w, h)
        applyBorder(s, visuals, w, h)
        applyInnerShadow(s, visuals)
        applyGlow(s, visuals)
        applyBackdrop(s, visuals, backdrop, x, y, w, h, screenWidth, screenHeight)
        applyFilter(s, visuals)

        Gl.activeTexture(Gl.texture0)
        Core.atlas.white().texture.bind()
        Draw.flush()

        val glowMargin = if (hasGlow(visuals)) (visuals.glow.spread + visuals.glow.blur) else 0f
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

        Draw.color()
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
            v.radii.topStart, v.radii.topEnd,
            v.radii.bottomEnd, v.radii.bottomStart
        )
        s.setUniformf("u_edgeSoftness", 1f)
        s.setUniformf("u_fillMode", v.background.mode.fillMode.toFloat())
        s.setUniformf("u_fillColor", v.background.color.r, v.background.color.g, v.background.color.b, v.background.color.a)
        s.setUniformf("u_progress", v.progress.fraction)
        s.setUniformf("u_progressColor", v.progress.color.r, v.progress.color.g, v.progress.color.b, v.progress.color.a)
    }

    private fun applyFill(s: Shader, v: BoxVisuals, w: Float, h: Float) {
        if (v.background.mode != org.mdt.ui.components.layout.BackgroundFill.Mode.TEXTURE) return
        val tex = v.background.region?.texture ?: v.background.texture ?: return
        v.background.computeUVs(w, h)
        s.setUniformf("u_uvScale", v.background.uvScaleX, v.background.uvScaleY)
        s.setUniformf("u_uvOffset", v.background.uvOffsetX, v.background.uvOffsetY)
        s.setUniformf("u_uvBounds", v.background.uvMinX, v.background.uvMinY, v.background.uvMaxX, v.background.uvMaxY)
        Gl.activeTexture(Gl.texture0 + Shaders.TEX_UNIT_FILL)
        tex.bind()
    }

    private fun applyBorder(s: Shader, v: BoxVisuals, w: Float, h: Float) {
        val border = v.border
        if (border.isVisible) {
            s.setUniformf("u_borderWidth", minOf(border.width, minOf(w, h) * 0.5f))
            s.setUniformf("u_borderColor", border.color.r, border.color.g, border.color.b, border.color.a)
            s.setUniformf("u_borderStyle", border.style.value.toFloat())
            if (border.style != org.mdt.ui.components.layout.Border.Style.SOLID) {
                s.setUniformf("u_dashLength", maxOf(border.dashLength, 0.1f))
                s.setUniformf("u_dashRatio", border.dashRatio)
            }
        } else {
            s.setUniformf("u_borderWidth", 0f)
        }
    }

    private fun applyInnerShadow(s: Shader, v: BoxVisuals) {
        val inner = v.innerShadow
        if (inner.isVisible) {
            s.setUniformf(
                "u_innerShadowColor",
                inner.color.r, inner.color.g, inner.color.b, inner.color.a
            )
            s.setUniformf("u_innerShadowSpread", inner.spread)
            s.setUniformf("u_innerShadowBlur", inner.blur)
        } else {
            s.setUniformf("u_innerShadowColor", 0f, 0f, 0f, 0f)
        }
    }

    private fun hasGlow(v: BoxVisuals): Boolean = v.glow.isVisible

    private fun applyGlow(s: Shader, v: BoxVisuals) {
        val glow = v.glow
        if (glow.isVisible) {
            s.setUniformf("u_glowColor", glow.color.r, glow.color.g, glow.color.b, glow.color.a)
            s.setUniformf("u_glowSpread", glow.spread)
            s.setUniformf("u_glowBlur", glow.blur)
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
        val filter = v.backdrop
        if (backdrop != null && v.background.mode == org.mdt.ui.components.layout.BackgroundFill.Mode.BACKDROP && screenWidth > 0f && screenHeight > 0f) {
            s.setUniformf("u_backdropWeight", filter.weight)
            val u0 = (x / screenWidth).coerceIn(0f, 1f)
            val v0 = (y / screenHeight).coerceIn(0f, 1f)
            val u1 = ((x + w) / screenWidth).coerceIn(0f, 1f)
            val v1 = ((y + h) / screenHeight).coerceIn(0f, 1f)
            s.setUniformf("u_backdropCoords", u0, v0, u1, v1)
            s.setUniformf("u_backdropBlend", filter.blend)
            s.setUniformf("u_backdropMinAlpha", filter.minAlpha)
            Gl.activeTexture(Gl.texture0 + Shaders.TEX_UNIT_BACKDROP)
            backdrop.bind()
        } else {
            s.setUniformf("u_backdropWeight", 0f)
        }
    }

    private fun applyFilter(s: Shader, v: BoxVisuals) {
        val filter = v.filter
        if (filter.isVisible) {
            s.setUniformf("u_colorFilter", filter.mode.value.toFloat(), filter.amount, 0f, 0f)
        } else {
            s.setUniformf("u_colorFilter", 0f, 0f, 0f, 0f)
        }
        s.setUniformf("u_noiseAmount", filter.noiseAmount)
    }
}
