// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.node

import arc.graphics.g2d.Font
import arc.util.Align
import org.mdt.core.platform.PlatformHost
import org.mdt.core.platform.render.FontMeasurer
import org.mdt.core.platform.render.UIBatch
import org.mdt.core.ui.input.Key
import org.mdt.core.ui.input.PointerEvent
import org.mdt.core.ui.input.TextEditState
import org.mdt.core.ui.unit.Color
import org.mdt.core.ui.unit.SizeFlags
import org.mdt.ui.theme.InputStyle

/**
 * ## InputNode
 *
 * Virtual DOM node rendering interactive single-line and multi-line text input fields.
 * Handles selection boxes, horizontal scrolling, IME composition markers, and blinking cursor rendering.
 *
 * @property editState Underlying text editing state machine ([TextEditState]).
 * @property placeholder Placeholder hint string displayed when text is empty.
 * @property style Native visual configuration for text, placeholder, cursor, selection, and IME composition ([InputStyle]).
 * @property font BMFont instance used for measurement and layout.
 * @property isMultiline Whether multi-line text wrapping and vertical expansion are enabled.
 *
 * @see TextEditState
 * @see InputStyle
 * @see UIBatch
 */
open class InputNode(
    text: String = "",
    private val hostProvider: () -> PlatformHost = { PlatformHost.NoOp },
) : LayoutNode() {

    val fontMeasurer: FontMeasurer get() = hostProvider().render.fontMeasurer

    val editState = TextEditState(
        hostProvider = hostProvider,
    ) {
        onValueChange?.invoke(it)
        invalidateLayout()
    }

    var placeholder: String = ""
    var style: InputStyle = InputStyle.Default

    var scrollOffset: Float = 0.0f
        private set

    var font: Font? = null
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    val activeFont: Font? get() = font ?: host.assets.resolveDefaultFont()

    var onValueChange: ((String) -> Unit)? = null

    var isMultiline: Boolean
        get() = editState.isMultiline
        set(value) {
            editState.isMultiline = value
            invalidateLayout()
        }

    private var dragSelectionAnchor: Int = -1
    private var isDraggingSelection: Boolean = false
    private var lastClickTime: Long = 0L
    private var clickCount: Int = 0

    init {
        isFocusable = true
        hitTestBehavior = HitTestBehavior.OPAQUE
        cursor = org.mdt.core.ui.input.CursorIcon.IBEAM
        minHeight = 24.0f
        minWidth = 40.0f
        sizeFlagsHorizontal = SizeFlags.FILL
    }

    override fun handleKeyTyped(character: Char): Boolean {
        editState.isFocused = isFocused
        val consumed = editState.onKeyTyped(character)
        if (consumed) {
            if (isFocused) {
                activeInputProcessor?.syncIme(this)
            }
            invalidateLayout()
        }
        return consumed
    }

    override fun handleKeyDown(key: Key): Boolean {
        editState.isFocused = isFocused
        val consumed = editState.onKeyDown(key)
        if (consumed) {
            if (isFocused) {
                activeInputProcessor?.syncIme(this)
            }
            invalidateLayout()
        }
        return consumed
    }

    override fun handlePointerDown(event: PointerEvent): Boolean {
        requestFocus()
        editState.isFocused = true
        val currentTime = host.system.nowMillis()
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
            2 -> editState.selectWordAt(charIndex)
            3 -> editState.selectAll()
            else -> {
                val isShift = host.input.isShiftPressed
                dragSelectionAnchor =
                    if (isShift && editState.selectionStart != -1) editState.selectionStart else charIndex
                isDraggingSelection = true
                editState.moveCursor(charIndex, extendSelection = isShift)
            }
        }
        invalidateLayout()
        return true
    }

    override fun handlePointerDrag(event: PointerEvent): Boolean {
        if (isDraggingSelection && dragSelectionAnchor != -1) {
            val innerX = bounds.x + padL
            val dragLocalX = event.x - innerX
            val targetIndex = getCharIndexAtX(dragLocalX)
            editState.setSelection(dragSelectionAnchor, targetIndex)
            invalidateLayout()
            event.consume()
            return true
        }
        return false
    }

    override fun handlePointerUp(event: PointerEvent): Boolean {
        isDraggingSelection = false
        return true
    }

    override fun onFocusChanged(focused: Boolean) {
        super.onFocusChanged(focused)
        editState.isFocused = focused
        invalidateLayout()
    }

    override fun onDetached() {
        super.onDetached()
        if (isFocused) {
            clearFocus()
        }
    }

    private fun getCharIndexAtX(localX: Float): Int {
        val currentFont = activeFont ?: return 0
        val text = editState.getDisplayText()
        if (text.isEmpty()) return 0

        val targetLocalX = if (!isMultiline) localX + scrollOffset else localX
        if (targetLocalX <= 0.0f) return 0

        val fontData = currentFont.data
        val scaleX = fontData.scaleX
        var accumulatedWidth = 0.0f
        var bestIndex = 0

        for (i in 0 until text.length) {
            val char = text[i]
            val glyph = fontData.getGlyph(char)
            val advance = if (glyph != null) glyph.xadvance * scaleX else 0.0f
            val halfAdvance = advance * 0.5f

            if (targetLocalX < accumulatedWidth + halfAdvance) {
                return i
            }
            accumulatedWidth += advance
            bestIndex = i + 1
        }

        return bestIndex
    }

    override fun getPrefWidth(): Float {
        if (width >= 0.0f) return width

        val baseWidth = when {
            minWidth >= 0.0f -> minWidth
            else -> 40.0f
        }
        return baseWidth + padL + padR
    }

    override fun getPrefHeight(availableWidth: Float): Float {
        if (height >= 0.0f) return height

        val currentFont = activeFont ?: return padT + padB
        val lineHeight = currentFont.lineHeight
        val baseHeight = if (minHeight >= 0.0f) maxOf(lineHeight, minHeight) else maxOf(lineHeight, 24.0f)
        return baseHeight + padT + padB
    }

    fun setText(newText: String) {
        editState.setText(newText)
        if (isFocused) {
            activeInputProcessor?.syncIme(this)
        }
        invalidateLayout()
    }

    override fun layout() {
        super.layout()
        if (isFocused) {
            activeInputProcessor?.syncIme(this)
        }
    }

    override fun drawSelf(batch: UIBatch) {
        // 1. Render optional background & borders
        super.drawSelf(batch)

        val currentFont = activeFont ?: return
        val delta = host.system.deltaTime
        editState.isFocused = isFocused
        editState.updateBlink(delta)

        val innerX = bounds.x + padL
        val innerY = bounds.y + padB
        val innerWidth = maxOf(0.0f, bounds.width - padL - padR)
        val innerHeight = maxOf(0.0f, bounds.height - padT - padB)

        val displayText = editState.getDisplayText()
        val capHeight = currentFont.data.capHeight
        val lineHeight = currentFont.lineHeight

        val textColor = style.textColor
        val placeholderColor = style.placeholderColor
        val cursorColor = style.cursorColor
        val selectionColor = style.selectionColor
        val compositionColor = style.compositionColor
        val compositionUnderlineColor = style.compositionUnderlineColor

        // 1. Calculate base text vertical baseline & cursor coordinates
        val cursorIndex = editState.getEffectiveCursor().coerceIn(0, displayText.length)

        val cursorRelX: Float
        val cursorY: Float
        val lineCenterY: Float
        val textY: Float

        when {
            isMultiline -> {
                val upToCursor = displayText.substring(0, cursorIndex)
                val lineIndex = upToCursor.count { it == '\n' }
                val lastBreak = upToCursor.lastIndexOf('\n')
                val lineSub = if (lastBreak >= 0) upToCursor.substring(lastBreak + 1) else upToCursor
                val relX =
                    if (lineSub.isNotEmpty()) fontMeasurer.getPrefWidth(currentFont, lineSub, 0.0f, false) else 0.0f
                val baselineY = innerY + innerHeight - (lineIndex + 1) * lineHeight
                cursorRelX = relX
                cursorY = baselineY
                lineCenterY = baselineY + lineHeight * 0.5f
                textY = innerY + innerHeight - currentFont.data.ascent
            }

            else -> {
                val cursorSub = displayText.substring(0, cursorIndex)
                val relX =
                    if (cursorSub.isNotEmpty()) fontMeasurer.getPrefWidth(currentFont, cursorSub, 0.0f, false) else 0.0f
                val cCenterY = innerY + innerHeight * 0.5f
                cursorRelX = relX
                cursorY = cCenterY - lineHeight * 0.5f
                lineCenterY = cCenterY
                textY = innerY + (innerHeight + capHeight) * 0.5f
            }
        }

        // Auto-scroll horizontal single-line text to keep caret visible
        scrollOffset = when {
            isMultiline -> 0.0f
            cursorRelX - scrollOffset > innerWidth - 8.0f -> maxOf(0.0f, cursorRelX - innerWidth + 8.0f)
            cursorRelX - scrollOffset < 0.0f -> maxOf(0.0f, cursorRelX)
            else -> scrollOffset
        }

        // 3. Scissor clip text inside the input box
        batch.pushClip(innerX, innerY, innerWidth, innerHeight)

        // 4. Draw Selection highlight quad
        if (editState.hasSelection()) {
            val selStart = editState.selectionStartRange
            val selEnd = editState.selectionEndRange
            val selStartSub = editState.text.substring(0, selStart)
            val selEndSub = editState.text.substring(0, selEnd)

            val selStartX = innerX + fontMeasurer.getPrefWidth(currentFont, selStartSub, 0.0f, false) - scrollOffset
            val selEndX = innerX + fontMeasurer.getPrefWidth(currentFont, selEndSub, 0.0f, false) - scrollOffset

            batch.drawBox(
                x = minOf(selStartX, selEndX),
                y = lineCenterY - lineHeight * 0.5f,
                width = maxOf(2.0f, kotlin.math.abs(selEndX - selStartX)),
                height = lineHeight,
                color = selectionColor,
                radius = 2.0f,
            )
        }

        // 5. Draw Pre-edit / IME Composition Region
        if (editState.hasComposition()) {
            val compStart = editState.compositionStartRange
            val compEnd = editState.compositionEndRange
            val compStartSub = displayText.substring(0, compStart)
            val compEndSub = displayText.substring(0, compEnd)

            val compStartX = innerX + fontMeasurer.getPrefWidth(currentFont, compStartSub, 0.0f, false) - scrollOffset
            val compEndX = innerX + fontMeasurer.getPrefWidth(currentFont, compEndSub, 0.0f, false) - scrollOffset
            val compositionWidth = maxOf(2.0f, compEndX - compStartX)

            // Composition background highlight
            batch.drawBox(
                x = compStartX,
                y = lineCenterY - lineHeight * 0.5f,
                width = compositionWidth,
                height = lineHeight,
                color = compositionColor,
                radius = 2.0f,
            )

            // Composition underline
            batch.drawBox(
                x = compStartX,
                y = innerY + 2.0f,
                width = compositionWidth,
                height = 2.0f,
                color = compositionUnderlineColor,
                radius = 1.0f,
            )
        }

        // 6. Draw Text or Placeholder
        when {
            displayText.isNotEmpty() -> {
                batch.drawText(
                    font = currentFont,
                    text = displayText,
                    x = innerX - scrollOffset,
                    y = textY,
                    targetWidth = if (isMultiline) innerWidth else 0.0f,
                    align = if (isMultiline) Align.topLeft else Align.left,
                    wrap = isMultiline,
                    color = textColor,
                )
            }

            placeholder.isNotEmpty() -> {
                batch.drawText(
                    font = currentFont,
                    text = placeholder,
                    x = innerX,
                    y = textY,
                    targetWidth = innerWidth,
                    align = if (isMultiline) Align.topLeft else Align.left,
                    wrap = isMultiline,
                    color = placeholderColor,
                )
            }
        }

        // 7. Draw Caret Cursor
        if (isFocused && editState.cursorVisible) {
            val cursorX = innerX + cursorRelX - scrollOffset

            batch.drawBox(
                x = cursorX,
                y = cursorY,
                width = 2.0f,
                height = lineHeight,
                color = cursorColor,
                radius = 1.0f,
            )
        }

        batch.popClip()
    }

    override fun resetModifiers() {
        super.resetModifiers()
        isFocusable = true
        hitTestBehavior = HitTestBehavior.OPAQUE
        cursor = org.mdt.core.ui.input.CursorIcon.IBEAM
        minHeight = 24.0f
        minWidth = 40.0f
        sizeFlagsHorizontal = SizeFlags.FILL
    }
}
