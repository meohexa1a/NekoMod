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
        boxVisuals.fillColor.set(Color.valueOf("181926"))
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

        val f = Fonts.def
        val oldSX = f.scaleX
        val oldSY = f.scaleY
        f.data.setScale(fontScale, fontScale)

        var bestIndex = 0
        var minDiff = Float.MAX_VALUE

        for (i in 0..text.length) {
            val sub = text.substring(0, i)
            layoutHelper.setText(f, sub)
            val w = layoutHelper.width
            val diff = kotlin.math.abs(localX - w)
            if (diff < minDiff) {
                minDiff = diff
                bestIndex = i
            }
        }

        f.data.setScale(oldSX, oldSY)
        return bestIndex
    }

    override fun getPrefWidth(): Float {
        if (width >= 0f) return width
        val f = Fonts.def
        val oldSX = f.scaleX
        val oldSY = f.scaleY
        f.data.setScale(fontScale, fontScale)

        val displayText = editState.text.ifEmpty { placeholder }
        layoutHelper.setText(f, displayText)
        val textW = layoutHelper.width

        f.data.setScale(oldSX, oldSY)
        val baseW = if (minWidth >= 0f) maxOf(textW, minWidth) else maxOf(textW, 120f)
        return baseW + padL + padR
    }

    override fun drawSelf(renderer: EngineRenderer) {
        val delta = if (Core.graphics != null) Core.graphics.deltaTime else 0.016f
        editState.isFocused = isFocused
        editState.updateBlink(delta)

        // Dynamic visual feedback on focus
        if (isFocused) {
            boxVisuals.borderColor.set(focusBorderColor)
            boxVisuals.glow(focusGlowColor, spread = 3f, blur = 6f)
        } else {
            boxVisuals.borderColor.set(normalBorderColor)
            boxVisuals.glow(Color.clear, spread = 0f, blur = 0f)
        }

        // 1. Draw SDF background container
        super.drawSelf(renderer)

        val innerX = bounds.x + padL
        val innerY = bounds.y + padB
        val innerW = bounds.width - padL - padR
        val innerH = bounds.height - padT - padB

        if (innerW <= 0f || innerH <= 0f) return

        val f = Fonts.def
        val oldSX = f.scaleX
        val oldSY = f.scaleY
        f.data.setScale(fontScale, fontScale)

        val capH = f.data.capHeight
        val textY = innerY + (innerH + capH) * 0.5f

        val text = editState.text

        // 2. Draw Selection highlight quad
        if (editState.hasSelection()) {
            val range = editState.getSelectionRange()!!
            val selStartSub = text.substring(0, range.first)
            val selEndSub = text.substring(0, range.second)

            layoutHelper.setText(f, selStartSub)
            val selX1 = innerX + layoutHelper.width

            layoutHelper.setText(f, selEndSub)
            val selX2 = innerX + layoutHelper.width

            Draw.color(selectionColor)
            Fill.rect(
                (selX1 + selX2) * 0.5f,
                innerY + innerH * 0.5f,
                selX2 - selX1,
                innerH
            )
            Draw.color(Color.white)
        }

        // 3. Draw Text / Placeholder
        if (text.isEmpty()) {
            f.color = placeholderColor
            f.draw(placeholder, innerX, textY)
        } else {
            f.color = textColor
            f.draw(text, innerX, textY)
        }

        // 4. Draw Caret Cursor
        if (isFocused && editState.cursorVisible) {
            val cursorSub = text.substring(0, editState.cursor.coerceIn(0, text.length))
            layoutHelper.setText(f, cursorSub)
            val cursorX = innerX + layoutHelper.width

            Draw.color(cursorColor)
            val cursorH = capH * 1.3f
            Fill.rect(cursorX + 1f, textY - capH * 0.4f, 1.5f, cursorH)
            Draw.color(Color.white)
        }

        f.data.setScale(oldSX, oldSY)
    }

    companion object {
        private val layoutHelper = GlyphLayout()
    }
}
