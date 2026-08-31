// [AGENT ARCHITECTURE & INVARIANTS]
// - Domain Role: Interactive Text Input Virtual DOM Node.
// - Operating Mechanism: Manages caret blinking, text selection, IME composition highlights, and keyboard routing via [TextEditState].
// - Invariants: Backspace handled exclusively in `onKeyDown`; analytical scissor clipping pushed/popped during text/selection draw.
// - Dependencies: [TextEditState], [FontRenderer], [UIBatch], [LayoutNode].
// - Directive: Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.ui.node

import arc.graphics.g2d.Font
import arc.util.Align
import org.mdt.core.ui.input.PointerEvent
import org.mdt.core.ui.input.TextEditState
import org.mdt.core.ui.layout.SizeFlags
import org.mdt.core.platform.PlatformHost
import org.mdt.core.platform.render.UIBatch
import org.mdt.core.platform.render.FontMeasurer
import org.mdt.core.platform.unit.Color

/**
 * ## InputNode
 *
 * Virtual DOM node rendering interactive single-line and multi-line text input fields.
 * Handles selection boxes, horizontal scrolling, IME composition markers, and blinking cursor rendering.
 *
 * @property editState Underlying text editing state machine ([TextEditState]).
 * @property placeholder Placeholder hint string displayed when text is empty.
 * @property placeholderColor Fill color for placeholder text.
 * @property textColor Text rendering color.
 * @property cursorColor Caret vertical bar indicator color.
 * @property selectionColor Highlight selection background box color.
 * @property compositionColor IME composition background highlight color.
 * @property compositionUnderlineColor IME composition underline bar color.
 * @property font BMFont instance used for measurement and layout.
 * @property isMultiline Whether multi-line text wrapping and vertical expansion are enabled.
 *
 * @see LayoutNode
 * @see TextEditState
 * @see org.mdt.ui.components.input.TextField
 * @see FontMeasurer
 */
open class InputNode(
    text: String = "",
    private val hostProvider: () -> PlatformHost = { PlatformHost.NoOp }
) : LayoutNode() {

    val fontMeasurer: FontMeasurer get() = hostProvider().render.fontMeasurer

    val editState = TextEditState(
        hostProvider = hostProvider
    ) {
        onValueChange?.invoke(it)
        invalidateLayout()
    }

    var placeholder: String = ""
    var placeholderColor: Color = Color(1.0f, 1.0f, 1.0f, 0.40f)

    var textColor: Color = Color.White
    var cursorColor: Color = Color(0.52f, 0.75f, 0.86f, 1.0f)
    var selectionColor: Color = Color(0.52f, 0.75f, 0.86f, 0.35f)
    var compositionColor: Color = Color(0.52f, 0.75f, 0.86f, 0.20f)
    var compositionUnderlineColor: Color = Color(0.52f, 0.75f, 0.86f, 0.90f)

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
        cursor = arc.Graphics.Cursor.SystemCursor.ibeam
        minHeight = 24.0f
        minWidth = 40.0f
        sizeFlagsHorizontal = SizeFlags.FILL

        onKeyTyped = { character ->
            val consumed = editState.onKeyTyped(character)
            if (consumed) {
                invalidateLayout()
            }
            consumed
        }

        onKeyDown = { keyCode ->
            val consumed = editState.onKeyDown(keyCode)
            if (consumed) {
                invalidateLayout()
            }
            consumed
        }

        onPointerDown = { event: PointerEvent ->
            requestFocus()
            val currentTime = System.currentTimeMillis()
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
                event.consume()
            }
        }

        onPointerUp = {
            isDraggingSelection = false
        }
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

        var bestIndex = text.length
        var minDiff = Float.MAX_VALUE

        for (i in 0..text.length) {
            val substring = text.substring(0, i)
            val subWidth = fontMeasurer.getPrefWidth(currentFont, substring, 0.0f, false)
            val diff = kotlin.math.abs(targetLocalX - subWidth)
            if (diff < minDiff) {
                minDiff = diff
                bestIndex = i
            }
        }

        return bestIndex
    }

    override fun getPrefWidth(): Float {
        if (width >= 0.0f) return width

        val currentFont = activeFont ?: return padL + padR
        val text = editState.getDisplayText().ifEmpty { placeholder }
        val textWidth = if (text.isNotEmpty()) fontMeasurer.getPrefWidth(currentFont, text, 0.0f, false) else 0.0f
        val baseWidth = if (minWidth >= 0.0f) maxOf(textWidth, minWidth) else maxOf(textWidth, 40.0f)
        return baseWidth + padL + padR
    }

    override fun getPrefHeight(availableWidth: Float): Float {
        if (height >= 0.0f) return height

        val currentFont = activeFont ?: return padT + padB
        val lineHeight = currentFont.lineHeight
        val baseHeight = if (minHeight >= 0.0f) maxOf(lineHeight, minHeight) else maxOf(lineHeight, 24.0f)
        return baseHeight + padT + padB
    }

    override fun drawSelf(batch: UIBatch) {
        // 1. Render optional background & borders
        super.drawSelf(batch)

        val currentFont = activeFont ?: return
        val delta = host.system.deltaTime
        editState.isFocused = isFocused
        editState.updateBlink(delta)

        if (isFocused) {
            activeInputProcessor?.syncIme(this)
        }

        val innerX = bounds.x + padL
        val innerY = bounds.y + padB
        val innerWidth = maxOf(0.0f, bounds.width - padL - padR)
        val innerHeight = maxOf(0.0f, bounds.height - padT - padB)

        val displayText = editState.getDisplayText()
        val capHeight = currentFont.data.capHeight
        val lineHeight = currentFont.lineHeight

        // 1. Calculate base text vertical baseline
        val textY = when {
            isMultiline -> innerY + innerHeight - currentFont.data.ascent
            else -> innerY + (innerHeight + capHeight) * 0.5f
        }
        val lineCenterY = innerY + innerHeight * 0.5f

        // 2. Cursor horizontal offset calculation
        val cursorIndex = editState.getEffectiveCursor().coerceIn(0, displayText.length)
        val cursorSub = displayText.substring(0, cursorIndex)
        val cursorRelX = if (cursorSub.isNotEmpty()) fontMeasurer.getPrefWidth(currentFont, cursorSub, 0.0f, false) else 0.0f

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
            val range = editState.getSelectionRange()!!
            val selStartSub = editState.text.substring(0, range.first)
            val selEndSub = editState.text.substring(0, range.second)

            val selectionStartX = innerX + fontMeasurer.getPrefWidth(currentFont, selStartSub, 0.0f, false) - scrollOffset
            val selectionEndX = innerX + fontMeasurer.getPrefWidth(currentFont, selEndSub, 0.0f, false) - scrollOffset
            val selectionWidth = maxOf(2.0f, selectionEndX - selectionStartX)

            batch.drawBox(
                x = selectionStartX,
                y = lineCenterY - lineHeight * 0.5f,
                width = selectionWidth,
                height = lineHeight,
                color = selectionColor,
                radius = 2.0f
            )
        }

        // 5. Draw Pre-edit / IME Composition Region
        if (editState.hasComposition()) {
            val compRange = editState.getCompositionRange()!!
            val compStartSub = displayText.substring(0, compRange.first)
            val compEndSub = displayText.substring(0, compRange.second)

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
                radius = 2.0f
            )

            // Composition underline
            batch.drawBox(
                x = compStartX,
                y = innerY + 2.0f,
                width = compositionWidth,
                height = 2.0f,
                color = compositionUnderlineColor,
                radius = 1.0f
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
                    color = textColor
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
                    color = placeholderColor
                )
            }
        }

        // 7. Draw Caret Cursor
        if (isFocused && editState.cursorVisible) {
            val cursorX = innerX + cursorRelX - scrollOffset

            batch.drawBox(
                x = cursorX,
                y = lineCenterY - lineHeight * 0.5f,
                width = 2.0f,
                height = lineHeight,
                color = cursorColor,
                radius = 1.0f
            )
        }

        batch.popClip()
    }
}
