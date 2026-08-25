package org.mdt.ui.widgets

import arc.Core
import arc.graphics.Color
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.g2d.Font
import arc.graphics.g2d.GlyphLayout
import mindustry.ui.Fonts
import org.mdt.ui.input.TextEditState
import org.mdt.ui.render.EngineRenderer

/**
 * ## TextFieldNode
 *
 * Interactive text input field rendering SDF rounded boxes, focus glows,
 * BMFont glyphs, selection highlighting, and animated blinking cursor.
 *
 * See: docs/complex-challenges/complex_challenges_en.md
 */
open class TextFieldNode : BoxNode() {

    val editState = TextEditState { newText ->
        onValueChange?.invoke(newText)
        invalidateLayout()
    }

    var placeholder: String = ""
    var placeholderColor: Color = Color(Color.valueOf("6e738d"))
    var textColor: Color = Color(Color.white)
    var selectionColor: Color = Color(Color.valueOf("2563eb").a(0.45f))
    var cursorColor: Color = Color(Color.valueOf("60a5fa"))

    var normalBorderColor: Color = Color(Color.valueOf("363a4f"))
    var focusBorderColor: Color = Color(Color.valueOf("2563eb"))
    var focusGlowColor: Color = Color(Color.valueOf("2563eb").a(0.4f))

    var fontScale: Float = 0.9f
    var onValueChange: ((String) -> Unit)? = null

    init {
        isFocusable = true
        pad(left = 12f, right = 12f, top = 6f, bottom = 6f)
        visuals.radius(6f)
        visuals.fillColor.set(Color.valueOf("181926"))
        visuals.border(1f, normalBorderColor)
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

        onPointerDown = { event ->
            val localX = event.x - (bounds.x + padL)
            val idx = hitTestCursor(localX)
            editState.moveCursor(idx)
        }
    }

    private fun font(): Font = Fonts.def

    /**
     * Hit tests a local X coordinate against the BMFont glyphs to find the nearest cursor index.
     */
    private fun hitTestCursor(localX: Float): Int {
        val text = editState.text
        if (text.isEmpty() || localX <= 0f) return 0

        val f = font()
        val oldSX = f.scaleX
        val oldSY = f.scaleY
        f.data.setScale(fontScale, fontScale)

        layoutHelper.setText(f, text)
        var accumulatedX = 0f
        var bestIndex = text.length

        if (layoutHelper.runs.size > 0) {
            val run = layoutHelper.runs.first()
            val xAdvances = run.xAdvances
            for (i in text.indices) {
                val advance = if (i + 1 < xAdvances.size) xAdvances.get(i + 1) else 0f
                if (localX < accumulatedX + advance * 0.5f) {
                    bestIndex = i
                    break
                }
                accumulatedX += advance
            }
        }

        f.data.setScale(oldSX, oldSY)
        return bestIndex
    }

    private fun getCursorPixelX(cursorIndex: Int): Float {
        val text = editState.text
        if (text.isEmpty() || cursorIndex <= 0) return 0f

        val sub = text.substring(0, cursorIndex.coerceIn(0, text.length))
        val f = font()
        val oldSX = f.scaleX
        val oldSY = f.scaleY
        f.data.setScale(fontScale, fontScale)

        layoutHelper.setText(f, sub)
        val w = layoutHelper.width

        f.data.setScale(oldSX, oldSY)
        return w
    }

    override fun getPrefWidth(): Float {
        if (width >= 0f) return width
        val f = font()
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
            visuals.borderColor.set(focusBorderColor)
            visuals.glow(focusGlowColor, spread = 3f, blur = 6f)
        } else {
            visuals.borderColor.set(normalBorderColor)
            visuals.glow(Color.clear, spread = 0f, blur = 0f)
        }

        // 1. Draw SDF background container
        super.drawSelf(renderer)

        val innerX = bounds.x + padL
        val innerY = bounds.y + padB
        val innerW = bounds.width - padL - padR
        val innerH = bounds.height - padT - padB
        if (innerW <= 0f || innerH <= 0f) return

        val f = font()
        val oldSX = f.scaleX
        val oldSY = f.scaleY
        f.data.setScale(fontScale, fontScale)

        val text = editState.text
        val displayText = text.ifEmpty { placeholder }
        val isPlaceholder = text.isEmpty()

        layoutHelper.setText(f, displayText)
        val textH = layoutHelper.height
        val baselineY = innerY + (innerH - textH) * 0.5f + f.data.capHeight

        // 2. Draw Selection Highlight
        if (isFocused && editState.hasSelection()) {
            val range = editState.getSelectionRange()!!
            val selStartX = innerX + getCursorPixelX(range.first)
            val selEndX = innerX + getCursorPixelX(range.second)
            val selW = maxOf(0f, selEndX - selStartX)

            Draw.color(selectionColor)
            Fill.rect(selStartX + selW * 0.5f, innerY + innerH * 0.5f, selW, innerH - 4f)
        }

        // 3. Draw Text / Placeholder
        f.color = if (isPlaceholder) placeholderColor else textColor
        f.draw(displayText, innerX, baselineY)

        // 4. Draw Blinking Cursor
        if (isFocused && editState.cursorVisible) {
            val cursorPixelX = innerX + getCursorPixelX(editState.cursor)
            val cursorH = f.data.capHeight + 4f
            val cursorY = innerY + (innerH - cursorH) * 0.5f + cursorH * 0.5f

            Draw.color(cursorColor)
            Fill.rect(cursorPixelX, cursorY, 2f, cursorH)
        }

        f.data.setScale(oldSX, oldSY)
        Draw.color(Color.white)
    }

    companion object {
        private val layoutHelper = GlyphLayout()
    }
}
