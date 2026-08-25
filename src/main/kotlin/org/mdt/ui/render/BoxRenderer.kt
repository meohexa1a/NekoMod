package org.mdt.ui.render

import arc.Core
import arc.graphics.Color
import arc.graphics.Gl
import arc.graphics.Texture
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.gl.Shader
import org.mdt.ui.widgets.BoxVisuals

object BoxRenderer {
    @JvmField
    var blurEnabled = true

    fun draw(
        x: Float, y: Float,
        w: Float, h: Float,
        visuals: BoxVisuals,
        blur: BoxBlur
    ) {
        if (w <= 0 || h <= 0) return
        Shaders.ensure()
        val backdrop = if (blurEnabled) blur.capture(visuals, x, y, w, h) else null
        drawContent(visuals, x, y, w, h, backdrop)
    }

    private fun drawContent(visuals: BoxVisuals, x: Float, y: Float, w: Float, h: Float, backdrop: Texture?) {
        Draw.flush()
        val prev = Draw.getShader()
        Draw.shader(Shaders.mainShader)
        Shaders.mainShader!!.let { s ->
            s.bind()
            applyCommon(s, visuals, w, h)
            applyFill(s, visuals)
            applyBorder(s, visuals, w, h)
            applyInnerShadow(s, visuals)
            applyGlow(s, visuals)
            applyBackdrop(s, visuals, backdrop)
            applyFilter(s, visuals)
        }

        Gl.activeTexture(Gl.texture0)
        Core.atlas.white().texture.bind()
        Draw.flush()

        val margin = 1f + (if (hasGlow(visuals)) (visuals.glowSpread + visuals.glowBlur).toFloat() else 0f)
        val qx = x - margin
        val qy = y - margin
        val qw = w + margin * 2f
        val qh = h + margin * 2f
        val u0 = -margin / w
        val v0 = -margin / h
        val u1 = 1f + margin / w
        val v1 = 1f + margin / h

        Draw.color(Color.white)
        Fill.quad(
            Core.atlas.white().texture,
            qx, qy, Draw.getColor().toFloatBits(), u0, v0,
            qx, qy + qh, Draw.getColor().toFloatBits(), u0, v1,
            qx + qw, qy + qh, Draw.getColor().toFloatBits(), u1, v1,
            qx + qw, qy, Draw.getColor().toFloatBits(), u1, v0
        )
        Draw.flush()
        Draw.shader(prev)
    }

    private fun applyCommon(s: Shader, v: BoxVisuals, w: Float, h: Float) {
        s.setUniformf("u_size", w, h)
        s.setUniformf("u_opacity", v.opacity.toFloat())
        s.setUniformf("u_cornerRadii",
            v.topLeftRadius.toFloat(), v.topRightRadius.toFloat(),
            v.bottomRightRadius.toFloat(), v.bottomLeftRadius.toFloat())
        s.setUniformf("u_edgeSoftness", 1f)
        s.setUniformf("u_fillMode", v.backgroundMode.fillMode.toFloat())
        s.setUniformf("u_fillColor", v.fillColor.r, v.fillColor.g, v.fillColor.b, v.fillColor.a)
    }

    private fun applyFill(s: Shader, v: BoxVisuals) {
        if (v.backgroundMode != BoxVisuals.BackgroundMode.TEXTURE || v.fillTexture == null) return
        s.setUniformf("u_uvScale", v.uvScaleX.toFloat(), v.uvScaleY.toFloat())
        s.setUniformf("u_uvOffset", v.uvOffsetX.toFloat(), v.uvOffsetY.toFloat())
        Gl.activeTexture(Gl.texture2d + Shaders.TEX_UNIT_FILL)
        v.fillTexture!!.bind()
    }

    private fun applyBorder(s: Shader, v: BoxVisuals, w: Float, h: Float) {
        if (v.borderWidth > 0.001) {
            s.setUniformf("u_borderWidth", minOf(v.borderWidth.toFloat(), minOf(w, h) * 0.5f))
            s.setUniformf("u_borderColor", v.borderColor.r, v.borderColor.g, v.borderColor.b, v.borderColor.a)
            s.setUniformf("u_borderStyle", v.borderStyle.value.toFloat())
            if (v.borderStyle != BoxVisuals.BorderStyle.SOLID) {
                s.setUniformf("u_dashLength", v.dashLength.toFloat())
                s.setUniformf("u_dashRatio", v.dashRatio.toFloat())
            }
        } else {
            s.setUniformf("u_borderWidth", 0f)
        }
    }

    private fun applyInnerShadow(s: Shader, v: BoxVisuals) {
        if (v.innerShadowColor.a > 0.001f && (v.innerShadowSpread > 0.001 || v.innerShadowBlur > 0.001)) {
            s.setUniformf("u_innerShadowColor", v.innerShadowColor.r, v.innerShadowColor.g, v.innerShadowColor.b, v.innerShadowColor.a)
            s.setUniformf("u_innerShadowSpread", v.innerShadowSpread.toFloat())
            s.setUniformf("u_innerShadowBlur", v.innerShadowBlur.toFloat())
        } else {
            s.setUniformf("u_innerShadowColor", 0f, 0f, 0f, 0f)
        }
    }

    private fun hasGlow(v: BoxVisuals): Boolean =
        v.glowColor.a > 0.001f && (v.glowSpread > 0.001 || v.glowBlur > 0.001)

    private fun applyGlow(s: Shader, v: BoxVisuals) {
        if (hasGlow(v)) {
            s.setUniformf("u_glowColor", v.glowColor.r, v.glowColor.g, v.glowColor.b, v.glowColor.a)
            s.setUniformf("u_glowSpread", v.glowSpread.toFloat())
            s.setUniformf("u_glowBlur", v.glowBlur.toFloat())
        } else {
            s.setUniformf("u_glowColor", 0f, 0f, 0f, 0f)
        }
    }

    private fun applyBackdrop(s: Shader, v: BoxVisuals, backdrop: Texture?) {
        if (backdrop != null && v.backgroundMode == BoxVisuals.BackgroundMode.BACKDROP) {
            s.setUniformf("u_backdropWeight", v.backdropWeight.toFloat())
            s.setUniformf("u_backdropCoords", 0f, 0f, 1f, 1f)
            s.setUniformf("u_backdropBlend", v.backdropBlend.toFloat())
            s.setUniformf("u_backdropMinAlpha", v.backdropMinAlpha.toFloat())
            Gl.activeTexture(Gl.texture2d + Shaders.TEX_UNIT_BACKDROP)
            backdrop.bind()
        } else {
            s.setUniformf("u_backdropWeight", 0f)
        }
    }

    private fun applyFilter(s: Shader, v: BoxVisuals) {
        if (v.filterMode != BoxVisuals.FilterMode.NONE && v.filterAmount > 0.001) {
            s.setUniformf("u_colorFilter", v.filterMode.value.toFloat(), v.filterAmount.toFloat(), 0f, 0f)
        } else {
            s.setUniformf("u_colorFilter", 0f, 0f, 0f, 0f)
        }
        s.setUniformf("u_noiseAmount", v.noiseAmount.toFloat())
    }
}
