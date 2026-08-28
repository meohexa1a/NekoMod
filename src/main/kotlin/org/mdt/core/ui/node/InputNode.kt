package org.mdt.core.ui.node

import arc.graphics.g2d.Font
import arc.util.Align
import org.mdt.core.ui.EngineRuntime
import org.mdt.core.ui.input.PointerEvent
import org.mdt.core.ui.input.TextEditState
import org.mdt.core.ui.layout.SizeFlags
import org.mdt.core.ui.render.UIBatch
import org.mdt.core.ui.render.UIFontDrawer
import org.mdt.core.ui.unit.Color

/**
 * ## InputNode [Virtual DOM Interactive Text Input Node]
 *
 * ### 1. 📖 Feature Specification & Core Architecture:
 * - Primitive Virtual DOM node providing high-performance interactive single-line & multi-line text editing.
 * - Coordinates seamlessly with [EngineInputProcessor] and [org.mdt.core.engine.PlatformHost] for OS Native IME candidate positioning.
 * - Handles horizontal viewport scrolling for single-line inputs with analytical scissor clipping.
 * - Renders selection highlight quads, pre-edit composition buffers, text glyphs, and blinking caret via 1-Draw-Call [UIBatch].
 * - Unstyled by default (zero default background or border styling).
 *
 * ### 2. ⚡ Invariants & Non-Negotiable Rules:
 * - **Rule 1 (Zero Platform Bypass):** Frame delta, shift key state, cursor, and fonts MUST be resolved through [EngineRuntime.host].
 * - **Rule 2 (IME Double-Backspace Protection):** Backspace is processed only in `onKeyDown`.
 * - **Rule 3 (OpenGL Bottom-Left Math):** Caret and selection quads align to baseline in bottom-left coordinates.
 *
 * ### 3. 🔗 Related Files & Subsystem Map:
 * - 🎨 **Composable UI:** `src/main/kotlin/org/mdt/ui/components/input/TextField.kt`
 * - 🎛️ **State Machine:** `src/main/kotlin/org/mdt/core/ui/input/TextEditState.kt`
 * - 🎮 **Focus Coordinator:** `src/main/kotlin/org/mdt/core/ui/input/EngineInputProcessor.kt`
 * - ⚡ **GPU Batcher:** `src/main/kotlin/org/mdt/core/ui/render/UIBatch.kt`
 * - 🔤 **Font Drawer:** `src/main/kotlin/org/mdt/core/ui/render/UIFontDrawer.kt`
 * - 🔌 **Platform Host:** `src/main/kotlin/org/mdt/core/engine/PlatformHost.kt`
 *
 * ### 4. ✅ Behavioral Verification Checklist:
 * - [x] Caret blinks at 0.32s intervals when focused; pauses/resets blink on typing or cursor navigation.
 * - [x] Requesting focus triggers native IME session via [EngineInputProcessor]; losing focus or detaching cleans up session.
 * - [x] Double-click selects word; triple-click selects all text.
 * - [x] Shift+Click / Pointer drag updates selection range dynamically.
 * - [x] Horizontal scroll offset keeps caret within visible viewport bounds minus 4px padding.
 * - [x] Analytical scissor clip is pushed before rendering selection/text/caret and popped immediately after.
 */
open class InputNode : LayoutNode() {

    val editState = TextEditState {
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

    val activeFont: Font? get() = font ?: EngineRuntime.host.resolveDefaultFont()

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
            if (consumed) invalidateLayout()
            consumed
        }

        onKeyDown = { keyCode ->
            val consumed = editState.onKeyDown(keyCode)
            if (consumed) invalidateLayout()
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
                    val isShift = EngineRuntime.host.isShiftPressed
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
            val subWidth = UIFontDrawer.getPrefWidth(currentFont, substring, 0.0f, false)
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
        val textWidth = if (text.isNotEmpty()) UIFontDrawer.getPrefWidth(currentFont, text, 0.0f, false) else 0.0f
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

    override fun drawSelf() {
        // 1. Render optional background & borders
        super.drawSelf()

        val currentFont = activeFont ?: return
        val delta = EngineRuntime.host.deltaTime
        editState.isFocused = isFocused
        editState.updateBlink(delta)

        if (isFocused) {
            EngineRuntime.inputProcessor.syncIme(this)
        }

        val innerX = bounds.x + padL
        val innerY = bounds.y + padB
        val innerWidth = maxOf(0.0f, bounds.width - padL - padR)
        val innerHeight = maxOf(0.0f, bounds.height - padT - padB)

        if (innerWidth <= 0.0f || innerHeight <= 0.0f) return

        val capHeight = currentFont.data.capHeight
        val textY = if (isMultiline) {
            innerY + innerHeight - currentFont.data.ascent
        } else {
            innerY + (innerHeight + capHeight) * 0.5f
        }

        val lineHeight = capHeight * 1.55f
        val lineCenterY = if (isMultiline) textY - capHeight * 0.5f else textY - capHeight * 0.45f
        val displayText = editState.getDisplayText()

        // 2. Compute horizontal scroll offset for single-line inputs
        val effCursor = editState.getEffectiveCursor()
        val cursorSub = displayText.substring(0, effCursor.coerceIn(0, displayText.length))
        val cursorRelX = if (cursorSub.isNotEmpty()) UIFontDrawer.getPrefWidth(currentFont, cursorSub, 0.0f, false) else 0.0f

        if (!isMultiline) {
            if (displayText.isEmpty()) {
                scrollOffset = 0.0f
            } else {
                if (cursorRelX - scrollOffset > innerWidth - 4.0f) {
                    scrollOffset = maxOf(0.0f, cursorRelX - innerWidth + 4.0f)
                } else if (cursorRelX - scrollOffset < 0.0f) {
                    scrollOffset = maxOf(0.0f, cursorRelX)
                }
            }
        } else {
            scrollOffset = 0.0f
        }

        // 3. Scissor clip text inside the input box
        UIBatch.pushClip(innerX, innerY, innerWidth, innerHeight)

        // 4. Draw Selection highlight quad
        if (editState.hasSelection()) {
            val range = editState.getSelectionRange()!!
            val selStartSub = editState.text.substring(0, range.first)
            val selEndSub = editState.text.substring(0, range.second)

            val selectionStartX = innerX + UIFontDrawer.getPrefWidth(currentFont, selStartSub, 0.0f, false) - scrollOffset
            val selectionEndX = innerX + UIFontDrawer.getPrefWidth(currentFont, selEndSub, 0.0f, false) - scrollOffset
            val selWidth = maxOf(2.0f, selectionEndX - selectionStartX)

            UIBatch.drawBox(
                x = selectionStartX,
                y = lineCenterY - lineHeight * 0.5f,
                width = selWidth,
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

            val compStartX = innerX + UIFontDrawer.getPrefWidth(currentFont, compStartSub, 0.0f, false) - scrollOffset
            val compEndX = innerX + UIFontDrawer.getPrefWidth(currentFont, compEndSub, 0.0f, false) - scrollOffset
            val compWidth = maxOf(2.0f, compEndX - compStartX)

            // Composition background highlight
            UIBatch.drawBox(
                x = compStartX,
                y = lineCenterY - lineHeight * 0.5f,
                width = compWidth,
                height = lineHeight,
                color = compositionColor,
                radius = 2.0f
            )

            // Composition underline
            UIBatch.drawBox(
                x = compStartX,
                y = innerY + 2.0f,
                width = compWidth,
                height = 2.0f,
                color = compositionUnderlineColor,
                radius = 1.0f
            )
        }

        // 6. Draw Text or Placeholder
        if (displayText.isEmpty()) {
            // Only draw placeholder when displayText is truly empty (hidden immediately on typing/composition)
            if (placeholder.isNotEmpty()) {
                UIFontDrawer.draw(
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
        } else {
            UIFontDrawer.draw(
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

        // 7. Draw Caret Cursor
        if (isFocused && editState.cursorVisible) {
            val cursorX = innerX + cursorRelX - scrollOffset

            UIBatch.drawBox(
                x = cursorX,
                y = lineCenterY - lineHeight * 0.5f,
                width = 2.0f,
                height = lineHeight,
                color = cursorColor,
                radius = 1.0f
            )
        }

        UIBatch.popClip()
    }
}
