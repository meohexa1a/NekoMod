package org.mdt.ui.components.input.textfield

import arc.Core
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.g2d.GlyphLayout
import mindustry.ui.Fonts
import org.mdt.ui.components.layout.BoxVisuals
import org.mdt.ui.components.layout.LayoutNode
import org.mdt.core.ui.input.PointerEvent
import org.mdt.core.ui.render.EngineRenderer

/**
 * ## TextFieldNode
 *
 * Virtual DOM node rendering an interactive text input with cursor,
 * selection box, placeholder text, and focus glow.
 *
 * See: docs/complex-challenges/complex_challenges_en.md
 */
open class TextFieldNode : LayoutNode() {

    val editState = TextEditState {
        onValueChange?.invoke(it)
        invalidateLayout()
    }

    var placeholder: String = ""
    var textColor: Color = Color(Color.valueOf("cad3f5"))
    var placeholderColor: Color = Color(Color.valueOf("5b6078"))
    var cursorColor: Color = Color(Color.valueOf("85c1dc"))
    var selectionColor: Color = Color(Color.valueOf("363a4f").a(0.8f))

    var normalBorderColor: Color = Color(Color.valueOf("363a4f"))
    var focusBorderColor: Color = Color(Color.valueOf("2563eb"))
    var focusGlowColor: Color = Color(Color.valueOf("2563eb").a(0.4f))

    var fontScale: Float = 1.0f
    var onValueChange: ((String) -> Unit)? = null

    val boxVisuals: BoxVisuals = ensureVisuals()

    init {
        isFocusable = true
        pad(left = 12f, right = 12f, top = 6f, bottom = 6f)
        boxVisuals.radius(6f)
        boxVisuals.background.color.set(Color.valueOf("181926"))
        boxVisuals.border(1f, normalBorderColor)
        minHeight = 32f

        onKeyTyped = { c ->
            val consumed = editState.onKeyTyped(c)
            if (consumed) invalidateLayout()
            consumed
        }

        onKeyDown = { key ->
            val consumed = editState.onKeyDown(key)
            if (consumed) invalidateLayout()
            consumed
        }

        onPointerDown = { event: PointerEvent ->
            requestFocus()
            val innerX = bounds.x + padL
            val clickLocalX = event.x - innerX
            val charIndex = getCharIndexAtX(clickLocalX)
            editState.moveCursor(charIndex, extendSelection = Core.input != null && Core.input.shift())
            invalidateLayout()
        }
    }

    private fun getCharIndexAtX(localX: Float): Int {
        val text = editState.text
        if (text.isEmpty()) return 0

        val font = Fonts.def
        val oldScaleX = font.scaleX
        val oldScaleY = font.scaleY
        font.data.setScale(fontScale, fontScale)

        var bestIndex = 0
        var minDiff = Float.MAX_VALUE

        for (i in 0..text.length) {
            val substring = text.substring(0, i)
            layoutHelper.setText(font, substring)
            val subWidth = layoutHelper.width
            val diff = kotlin.math.abs(localX - subWidth)
            if (diff < minDiff) {
                minDiff = diff
                bestIndex = i
            }
        }

        font.data.setScale(oldScaleX, oldScaleY)
        return bestIndex
    }

    override fun getPrefWidth(): Float {
        if (width >= 0f) return width

        val font = Fonts.def
        val oldScaleX = font.scaleX
        val oldScaleY = font.scaleY
        font.data.setScale(fontScale, fontScale)

        val displayText = editState.text.ifEmpty { placeholder }
        layoutHelper.setText(font, displayText)
        val textWidth = layoutHelper.width

        font.data.setScale(oldScaleX, oldScaleY)
        val baseWidth = if (minWidth >= 0f) maxOf(textWidth, minWidth) else maxOf(textWidth, 120f)
        return baseWidth + padL + padR
    }

    override fun drawSelf(renderer: EngineRenderer) {
        val delta = if (Core.graphics != null) Core.graphics.deltaTime else 0.016f
        editState.isFocused = isFocused
        editState.updateBlink(delta)

        // Dynamic visual feedback on focus
        if (isFocused) {
            boxVisuals.border.color.set(focusBorderColor)
            boxVisuals.glow(focusGlowColor, spread = 3f, blur = 6f)
        } else {
            boxVisuals.border.color.set(normalBorderColor)
            boxVisuals.glow(Color.clear, spread = 0f, blur = 0f)
        }

        // 1. Draw SDF background container
        super.drawSelf(renderer)

        val innerX = bounds.x + padL
        val innerY = bounds.y + padB
        val innerWidth = bounds.width - padL - padR
        val innerHeight = bounds.height - padT - padB

        if (innerWidth <= 0f || innerHeight <= 0f) return

        val font = Fonts.def
        val oldScaleX = font.scaleX
        val oldScaleY = font.scaleY
        font.data.setScale(fontScale, fontScale)

        val capHeight = font.data.capHeight
        val textY = innerY + (innerHeight + capHeight) * 0.5f

        val text = editState.text

        // 2. Draw Selection highlight quad
        if (editState.hasSelection()) {
            val range = editState.getSelectionRange()!!
            val selStartSub = text.substring(0, range.first)
            val selEndSub = text.substring(0, range.second)

            layoutHelper.setText(font, selStartSub)
            val selectionStartX = innerX + layoutHelper.width

            layoutHelper.setText(font, selEndSub)
            val selectionEndX = innerX + layoutHelper.width

            Draw.color(selectionColor)
            Fill.rect(
                (selectionStartX + selectionEndX) * 0.5f,
                innerY + innerHeight * 0.5f,
                selectionEndX - selectionStartX,
                innerHeight
            )
            Draw.color(Color.white)
        }

        // 3. Draw Text / Placeholder
        if (text.isEmpty()) {
            font.color = placeholderColor
            font.draw(placeholder, innerX, textY)
        } else {
            font.color = textColor
            font.draw(text, innerX, textY)
        }

        // 4. Draw Caret Cursor
        if (isFocused && editState.cursorVisible) {
            val cursorSub = text.substring(0, editState.cursor.coerceIn(0, text.length))
            layoutHelper.setText(font, cursorSub)
            val cursorX = innerX + layoutHelper.width

            Draw.color(cursorColor)
            val cursorHeight = capHeight * 1.3f
            Fill.rect(cursorX + 1f, textY - capHeight * 0.4f, 1.5f, cursorHeight)
            Draw.color(Color.white)
        }

        font.data.setScale(oldScaleX, oldScaleY)
    }

    companion object {
        private val layoutHelper = GlyphLayout()
    }
}
