@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.display.tooltip

import arc.Core
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.GlyphLayout
import mindustry.ui.Fonts
import org.mdt.core.ui.UINode
import org.mdt.core.ui.compose.UIModifier
import org.mdt.core.ui.render.BoxRenderer
import org.mdt.core.ui.render.EngineRenderer
import org.mdt.ui.components.layout.BoxVisuals

/**
 * ## TooltipManager
 *
 * Top-layer overlay manager rendering floating Apple iOS-style Frosted Glass tooltips
 * with SDF rounded corners, specular highlight borders, and 1.0x BMFont clarity.
 *
 * Complies with Rule 8 (Top-layer overlay pass rendering).
 *
 * See: docs/ui-engine/ui_engine_en.md
 */
object TooltipManager {
    var activeText: String? = null
    var hoverTimer: Float = 0f
    private var lastTarget: Any? = null

    val tooltipVisuals = BoxVisuals().apply {
        fillColor.set(Color(0.08f, 0.09f, 0.13f, 0.92f))
        borderColor.set(Color(1f, 1f, 1f, 0.25f))
        borderWidth = 1f
        setRadius(8f)
        shadowColor.set(Color(0f, 0f, 0f, 0.45f))
        shadowBlur = 12f
        shadowSpread = 2f
    }

    private val layoutHelper = GlyphLayout()

    fun show(target: Any, text: String) {
        if (lastTarget != target) {
            lastTarget = target
            activeText = text
            hoverTimer = 0f
        }
    }

    fun hide(target: Any) {
        if (lastTarget == target) {
            lastTarget = null
            activeText = null
            hoverTimer = 0f
        }
    }

    fun drawTopLayer(renderer: EngineRenderer) {
        val text = activeText ?: return
        if (text.isEmpty()) return

        val dt = if (Core.graphics != null) Core.graphics.deltaTime else 0.016f
        hoverTimer += dt
        if (hoverTimer < 0.25f) return

        val mouseX = if (Core.input != null) Core.input.mouseX().toFloat() else 0f
        val mouseY = if (Core.input != null) Core.input.mouseY().toFloat() else 0f
        val screenW = if (Core.graphics != null) Core.graphics.width.toFloat() else 1920f
        val screenH = if (Core.graphics != null) Core.graphics.height.toFloat() else 1080f

        val font = Fonts.def
        layoutHelper.setText(font, text)
        val textW = layoutHelper.width
        val textH = layoutHelper.height

        val padH = 12f
        val padV = 8f
        val boxW = textW + padH * 2f
        val boxH = textH + padV * 2f

        val rawX = mouseX + 14f
        val rawY = mouseY + 14f

        val posX = rawX.coerceIn(8f, maxOf(8f, screenW - boxW - 8f))
        val posY = rawY.coerceIn(8f, maxOf(8f, screenH - boxH - 8f))

        // Draw top-layer SDF rounded box with glowing border and shadow
        BoxRenderer.draw(posX, posY, boxW, boxH, tooltipVisuals)

        // Draw text at pixel-perfect scale 1.0f
        font.color = Color.white
        font.draw(text, posX + padH, posY + padV + font.data.capHeight)
        Draw.color(Color.white)
    }
}

/**
 * Typed modifier element for attaching a top-layer tooltip.
 */
data class TooltipModifier(val text: String) : UIModifier.Element {
    override fun applyTo(node: UINode) {
        val prevEnter = node.onPointerEnter
        val prevExit = node.onPointerExit

        node.onPointerEnter = {
            prevEnter?.invoke()
            TooltipManager.show(node, text)
        }

        node.onPointerExit = {
            prevExit?.invoke()
            TooltipManager.hide(node)
        }
    }
}

/**
 * Attaches a top-layer floating tooltip to this UI component.
 */
fun UIModifier.tooltip(text: String): UIModifier = then(TooltipModifier(text))
