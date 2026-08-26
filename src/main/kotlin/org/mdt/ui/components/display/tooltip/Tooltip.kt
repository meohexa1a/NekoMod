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
        background.color.set(Color(0.08f, 0.09f, 0.13f, 0.92f))
        border(width = 1f, color = Color(1f, 1f, 1f, 0.25f))
        radius(8f)
        shadow(color = Color(0f, 0f, 0f, 0.45f), blur = 12f, spread = 2f)
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

        val deltaTime = if (Core.graphics != null) Core.graphics.deltaTime else 0.016f
        hoverTimer += deltaTime
        if (hoverTimer < 0.25f) return

        val mouseX = if (Core.input != null) Core.input.mouseX().toFloat() else 0f
        val mouseY = if (Core.input != null) Core.input.mouseY().toFloat() else 0f
        val screenWidth = if (Core.graphics != null) Core.graphics.width.toFloat() else 1920f
        val screenHeight = if (Core.graphics != null) Core.graphics.height.toFloat() else 1080f

        val font = Fonts.def
        layoutHelper.setText(font, text)
        val textWidth = layoutHelper.width
        val textHeight = layoutHelper.height

        val padHorizontal = 12f
        val padVertical = 8f
        val boxWidth = textWidth + padHorizontal * 2f
        val boxHeight = textHeight + padVertical * 2f

        val rawPointerX = mouseX + 14f
        val rawPointerY = mouseY + 14f

        val positionX = rawPointerX.coerceIn(8f, maxOf(8f, screenWidth - boxWidth - 8f))
        val positionY = rawPointerY.coerceIn(8f, maxOf(8f, screenHeight - boxHeight - 8f))

        // Draw top-layer SDF rounded box with glowing border and shadow
        BoxRenderer.draw(positionX, positionY, boxWidth, boxHeight, tooltipVisuals)

        // Draw text at pixel-perfect scale 1.0f
        font.color = Color.white
        font.draw(text, positionX + padHorizontal, positionY + padVertical + font.data.capHeight)
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
