@file:Suppress("FunctionName", "unused")

package org.mdt.ui.components.display.tooltip

import arc.Core
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.g2d.GlyphLayout
import mindustry.ui.Fonts
import org.mdt.ui.compose.CustomModifier
import org.mdt.ui.compose.UIModifier
import org.mdt.ui.core.UINode
import org.mdt.ui.render.EngineRenderer

/**
 * ## TooltipNode
 *
 * Floating tooltip overlay node displaying explanatory text on hover.
 */
class TooltipNode(var text: String) : UINode() {

    private var hoverTimer: Float = 0f
    private var isTargetHovered = false

    var backgroundColor: Color = Color(Color.valueOf("181926").a(0.95f))
    var borderColor: Color = Color(Color.valueOf("363a4f"))
    var textColor: Color = Color(Color.valueOf("cad3f5"))
    var fontScale: Float = 0.8f

    init {
        touchable = false
    }

    fun onTargetHoverChanged(hovered: Boolean) {
        if (isTargetHovered != hovered) {
            isTargetHovered = hovered
            if (!hovered) {
                hoverTimer = 0f
            }
        }
    }

    override fun draw(renderer: EngineRenderer) {
        val dt = if (Core.graphics != null) Core.graphics.deltaTime else 0.016f
        if (isTargetHovered) {
            hoverTimer += dt
        }

        if (isTargetHovered && hoverTimer >= 0.35f && text.isNotEmpty()) {
            drawTooltip()
        }
    }

    private fun drawTooltip() {
        val mouseX = if (Core.input != null) Core.input.mouseX().toFloat() else bounds.x
        val mouseY = if (Core.input != null) Core.input.mouseY().toFloat() else bounds.y

        val f = Fonts.def
        val oldSX = f.scaleX
        val oldSY = f.scaleY
        f.data.setScale(fontScale, fontScale)

        layoutHelper.setText(f, text)
        val textW = layoutHelper.width
        val textH = layoutHelper.height

        val padH = 8f
        val padV = 5f
        val boxW = textW + padH * 2f
        val boxH = textH + padV * 2f

        val posX = mouseX + 12f
        val posY = mouseY - boxH - 6f

        // Draw shadow
        Draw.color(Color.black.a(0.4f))
        Fill.rect(posX + boxW * 0.5f, posY + boxH * 0.5f - 2f, boxW + 2f, boxH + 2f)

        // Draw background
        Draw.color(backgroundColor)
        Fill.rect(posX + boxW * 0.5f, posY + boxH * 0.5f, boxW, boxH)

        // Draw text
        f.color = textColor
        f.draw(text, posX + padH, posY + padV + f.data.capHeight)

        f.data.setScale(oldSX, oldSY)
        Draw.color(Color.white)
    }

    companion object {
        private val layoutHelper = GlyphLayout()
    }
}

/**
 * Attaches a floating tooltip to this UI component.
 */
fun UIModifier.tooltip(text: String): UIModifier = then(CustomModifier { target ->
    val tooltipNode = TooltipNode(text)
    val prevEnter = target.onPointerEnter
    val prevExit = target.onPointerExit

    target.onPointerEnter = {
        prevEnter?.invoke()
        tooltipNode.onTargetHoverChanged(true)
    }

    target.onPointerExit = {
        prevExit?.invoke()
        tooltipNode.onTargetHoverChanged(false)
    }

    target.addChild(tooltipNode)
})
