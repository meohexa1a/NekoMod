package org.mdt.ui.components.input.textfield

import arc.Core
import arc.graphics.g2d.Draw
import arc.graphics.g2d.Fill
import arc.graphics.g2d.GlyphLayout
import arc.util.Tmp
import mindustry.ui.Fonts
import org.mdt.core.ui.graphics.Color
import org.mdt.core.ui.input.ImeNativeBridge
import org.mdt.core.ui.input.PointerEvent
import org.mdt.core.ui.render.EngineRenderer
import org.mdt.ui.components.layout.BoxVisuals
import org.mdt.ui.components.layout.LayoutNode

/**
 * ## TextFieldNode
 *
 * Virtual DOM node rendering an interactive text input with cursor,
 * selection box, placeholder text, focus glow, and zero-GC native IME bridging.
 *
 * See: docs/complex-challenges/complex_challenges_en.md
 */
open class TextFieldNode : LayoutNode() {

    val editState = TextEditState {
        onValueChange?.invoke(it)
        invalidateLayout()
    }

    var placeholder: String = ""
    var textColor: Color = Color.valueOf("cad3f5")
    var placeholderColor: Color = Color.valueOf("5b6078")
    var cursorColor: Color = Color.valueOf("85c1dc")
    var selectionColor: Color = Color.valueOf("363a4f").withAlpha(0.8f)

    var normalBorderColor: Color = Color.valueOf("363a4f")
    var focusBorderColor: Color = Color.valueOf("2563eb")
    var focusGlowColor: Color = Color.valueOf("2563eb").withAlpha(0.4f)

    var fontScale: Float = 1.0f
    var onValueChange: ((String) -> Unit)? = null

    var isMultiline: Boolean
        get() = editState.isMultiline
        set(value) {
            editState.isMultiline = value
        }

    val boxVisuals: BoxVisuals = ensureVisuals()

    private var dragSelectionAnchor: Int = -1
    private var isDraggingSelection: Boolean = false
    private var lastClickTime: Long = 0L
    private var clickCount: Int = 0

    init {
        isFocusable = true
        cursor = arc.Graphics.Cursor.SystemCursor.ibeam
        pad(left = 12f, right = 12f, top = 6f, bottom = 6f)
        boxVisuals.radius(6f)
        boxVisuals.background.color = Color.valueOf("181926")
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
            ImeNativeBridge.attach(this)
            val currentTime = arc.util.Time.millis()
            if (currentTime - lastClickTime < 350L) {
                clickCount++
            } else {
                clickCount = 1
            }
            lastClickTime = currentTime

            val innerX = bounds.x + padL
            val clickLocalX = event.x - innerX
            val charIndex = getCharIndexAtX(clickLocalX)

            when (clickCount) {
                2 -> {
                    editState.selectWordAt(charIndex)
                }
                3 -> {
                    editState.selectAll()
                }
                else -> {
                    val isShift = Core.input != null && Core.input.shift()
                    dragSelectionAnchor = if (isShift && editState.selectionStart != -1) editState.selectionStart else charIndex
                    isDraggingSelection = true
                    editState.moveCursor(charIndex, extendSelection = isShift)
                }
            }
            invalidateLayout()
        }

        onPointerDrag = { event: PointerEvent ->
            if (isDraggingSelection && dragSelectionAnchor != -1) {
                val innerX = bounds.x + padL
                val dragLocalX = event.x - innerX
                val targetIndex = getCharIndexAtX(dragLocalX)
                editState.setSelection(dragSelectionAnchor, targetIndex)
                invalidateLayout()
                event.isConsumed = true
            }
        }

        onPointerUp = {
            isDraggingSelection = false
        }
    }

    override fun onDetached() {
        super.onDetached()
        ImeNativeBridge.detach(this)
    }

    private fun getCharIndexAtX(localX: Float): Int {
        val text = editState.getDisplayText()
        if (text.isEmpty() || localX <= 0f) return 0

        val font = Fonts.def
        val oldScaleX = font.scaleX
        val oldScaleY = font.scaleY
        font.data.setScale(fontScale, fontScale)

        var bestIndex = text.length
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

        val displayText = editState.getDisplayText().ifEmpty { placeholder }
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

        // Dynamic visual feedback on focus & OS IME Candidate Synchronization
        if (isFocused) {
            boxVisuals.border.color = focusBorderColor
            boxVisuals.glow(focusGlowColor, spread = 3f, blur = 6f)

            ImeNativeBridge.sync(this)
        } else {
            boxVisuals.border.color = normalBorderColor
            boxVisuals.glow(Color.Clear, spread = 0f, blur = 0f)
            ImeNativeBridge.detach(this)
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
        val textY = if (isMultiline) {
            innerY + innerHeight - 2f
        } else {
            innerY + (innerHeight + capHeight) * 0.5f
        }

        // Unified typography vertical alignment: selection box and caret share identical height & baseline center
        val lineHeight = capHeight * 1.55f
        val lineCenterY = if (isMultiline) textY - capHeight * 0.5f else textY - capHeight * 0.45f

        val displayText = editState.getDisplayText()

        // 2. Draw Selection highlight quad (Matching line height)
        if (editState.hasSelection()) {
            val range = editState.getSelectionRange()!!
            val selStartSub = editState.text.substring(0, range.first)
            val selEndSub = editState.text.substring(0, range.second)

            layoutHelper.setText(font, selStartSub)
            val selectionStartX = innerX + layoutHelper.width

            layoutHelper.setText(font, selEndSub)
            val selectionEndX = innerX + layoutHelper.width
            val selWidth = maxOf(2f, selectionEndX - selectionStartX)

            Draw.color(selectionColor.toArcColor(Tmp.c1))
            Fill.rect(
                selectionStartX + selWidth * 0.5f,
                lineCenterY,
                selWidth,
                lineHeight
            )
            Draw.color()
        }

        // 3. Draw Pre-edit / IME Composition Region with subtle highlight & underline
        if (editState.hasComposition()) {
            val compRange = editState.getCompositionRange()!!
            val compStartSub = displayText.substring(0, compRange.first)
            val compEndSub = displayText.substring(0, compRange.second)

            layoutHelper.setText(font, compStartSub)
            val compStartX = innerX + layoutHelper.width

            layoutHelper.setText(font, compEndSub)
            val compEndX = innerX + layoutHelper.width
            val compWidth = maxOf(2f, compEndX - compStartX)

            // Composition background highlight
            Draw.color(focusBorderColor.withAlpha(0.18f).toArcColor(Tmp.c1))
            Fill.rect(compStartX + compWidth * 0.5f, lineCenterY, compWidth, lineHeight)

            // Composition underline
            Draw.color(focusBorderColor.toArcColor(Tmp.c1))
            Fill.rect(compStartX + compWidth * 0.5f, innerY + 3f, compWidth, 2f)
            Draw.color()
        }

        // 4. Draw Text / Placeholder
        if (displayText.isEmpty()) {
            font.color = placeholderColor.toArcColor(Tmp.c1)
            if (isMultiline) {
                font.draw(placeholder, innerX, textY, innerWidth, arc.util.Align.topLeft, true)
            } else {
                font.draw(placeholder, innerX, textY)
            }
        } else {
            font.color = textColor.toArcColor(Tmp.c1)
            if (isMultiline) {
                font.draw(displayText, innerX, textY, innerWidth, arc.util.Align.topLeft, true)
            } else {
                font.draw(displayText, innerX, textY)
            }
        }

        // 5. Draw Caret Cursor (Synchronized with line height and lineCenterY)
        if (isFocused && editState.cursorVisible) {
            val effCursor = editState.getEffectiveCursor()
            val cursorSub = displayText.substring(0, effCursor.coerceIn(0, displayText.length))
            layoutHelper.setText(font, cursorSub)
            val cursorX = innerX + layoutHelper.width

            Draw.color(cursorColor.toArcColor(Tmp.c1))
            Fill.rect(cursorX + 1f, lineCenterY, 2f, lineHeight)
            Draw.color()
        }

        font.data.setScale(oldScaleX, oldScaleY)
    }

    companion object {
        private val layoutHelper = GlyphLayout()
    }
}
