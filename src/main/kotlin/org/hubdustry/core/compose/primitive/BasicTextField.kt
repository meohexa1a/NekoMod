package org.hubdustry.core.compose.primitive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import arc.Core
import arc.graphics.Color
import arc.graphics.g2d.Font
import arc.graphics.g2d.GlyphLayout
import arc.input.KeyCode
import kotlinx.coroutines.delay
import mindustry.ui.Fonts
import org.hubdustry.core.compose.foundation.text.TextFieldState
import org.hubdustry.core.compose.input.FocusInteraction
import org.hubdustry.core.compose.input.KeyboardInputHandler
import org.hubdustry.core.compose.input.MutableInteractionSource
import org.hubdustry.core.compose.input.collectIsFocusedAsState
import org.hubdustry.core.compose.input.gestures.detectTapGestures
import org.hubdustry.core.compose.input.ime.ImeCompositionListener
import org.hubdustry.core.compose.input.ime.SdlReflectionImeBridge
import org.hubdustry.core.compose.modifier.BoxScope
import org.hubdustry.core.compose.modifier.Modifier
import org.hubdustry.core.compose.modifier.background
import org.hubdustry.core.compose.modifier.hoverable
import org.hubdustry.core.compose.modifier.offset
import org.hubdustry.core.compose.modifier.pointerInput
import org.hubdustry.core.compose.modifier.size
import org.hubdustry.core.compose.view.ComposeView
import org.hubdustry.core.compose.view.LocalComposeView
import org.hubdustry.core.graphics.RoundedCorners
import org.hubdustry.core.layout.Alignment
import kotlin.math.abs

internal val DefaultSelectionColor: Color = Color(0.25f, 0.45f, 0.95f, 0.35f)

private const val DEFAULT_CURSOR_WIDTH = 2f
private const val DEFAULT_CURSOR_HEIGHT = 18f
private const val DEFAULT_SELECTION_HEIGHT = 20f
private const val DEFAULT_COMPOSITION_INDICATOR_HEIGHT = 2f
private const val DEFAULT_COMPOSITION_INDICATOR_OFFSET_Y = 10f
private const val DOUBLE_TAP_TIMEOUT_MILLIS = 300L
private const val CURSOR_BLINK_INTERVAL_MILLIS = 500L
private const val FALLBACK_CHAR_WIDTH = 8f

// ─── Public Basic Text Field Composable ───────────────────────────

/**
 * Primitive Composable nguyên thủy điều khiển nhập liệu văn bản [BasicTextField]:
 * - Đảm nhận ĐÚNG VAI TRÒ của node: Quản lý con trỏ nhấp nháy, vùng chọn, nhận diện phím bấm và kết nối bộ gõ IME.
 * - TUYỆT ĐỐI KHÔNG vẽ khung viền (border), không tô màu nền (background), không chừa lề (padding).
 * - Hỗ trợ [decorationBox] chuẩn Jetpack Compose AOSP để caller hoặc component cấp cao tự do trang trí khung bên ngoài.
 */
@Composable
fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    textColor: Color = Color.white,
    font: Font? = null,
    cursorColor: Color = Color.royal,
    selectionColor: Color = DefaultSelectionColor,
    decorationBox: @Composable BoxScope.(innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() }
) {
    val currentInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isExternalFocused by currentInteractionSource.collectIsFocusedAsState()
    val composeView = LocalComposeView.current

    val editState = remember {
        TextFieldState(value, onValueChange).apply {
            this.isSingleLine = singleLine
        }
    }

    if (isExternalFocused && !editState.isFocused) {
        editState.isFocused = true
    }

    val isFocused = editState.isFocused

    // Đồng bộ giá trị từ bên ngoài nếu text thay đổi và không đang focus
    if (!isFocused && editState.text != value) {
        editState.setText(value)
    }
    editState.onTextChange = onValueChange
    editState.isSingleLine = singleLine

    var currentFocusInteraction: FocusInteraction.Focus? by remember { mutableStateOf(null) }

    val keyHandler = rememberKeyboardInputHandler(
        editState = editState,
        composeView = composeView,
        currentInteractionSource = currentInteractionSource,
        onEscapeOrFocusLost = { handler ->
            clearFocusAndStopIme(
                editState = editState,
                composeView = composeView,
                keyHandler = handler,
                source = currentInteractionSource,
                focusInteraction = currentFocusInteraction,
                onCleared = { currentFocusInteraction = null }
            )
        }
    )

    LaunchedEffect(enabled) {
        if (!enabled && editState.isFocused) {
            clearFocusAndStopIme(
                editState = editState,
                composeView = composeView,
                keyHandler = keyHandler,
                source = currentInteractionSource,
                focusInteraction = currentFocusInteraction,
                onCleared = { currentFocusInteraction = null }
            )
        }
    }

    // Coroutine nhấp nháy con trỏ khi nhận focus (cập nhật snapshot state editState.cursorVisible)
    LaunchedEffect(isFocused) {
        if (!isFocused) return@LaunchedEffect
        editState.resetBlink()
        while (true) {
            delay(CURSOR_BLINK_INTERVAL_MILLIS)
            editState.updateBlink(0.5f)
        }
    }

    // Đăng ký bàn phím và IME session khi nhận / mất focus
    DisposableEffect(isFocused, composeView) {
        if (isFocused && composeView != null) {
            composeView.requestKeyboardFocus(keyHandler)

            // Tính toán tọa độ màn hình chuẩn (Y-down) cho SDL IME Candidate Rect
            val screenHeight = Core.graphics?.height?.toFloat() ?: 1080f
            val screenX = composeView.x
            val screenY = (screenHeight - (composeView.y + composeView.height)).coerceAtLeast(0f)

            // Bắt đầu phiên IME Reflection Hook
            SdlReflectionImeBridge.startSession(
                screenX = screenX,
                screenY = screenY,
                width = composeView.width,
                height = composeView.height,
                listener = object : ImeCompositionListener {
                    override fun onCompositionChanged(composition: String) {
                        editState.setComposition(composition)
                    }

                    override fun onCompositionCleared() {
                        editState.clearComposition()
                    }
                }
            )

            onDispose {
                clearFocusAndStopIme(
                    editState = editState,
                    composeView = composeView,
                    keyHandler = keyHandler,
                    source = currentInteractionSource,
                    focusInteraction = currentFocusInteraction,
                    onCleared = { currentFocusInteraction = null }
                )
            }
        } else {
            onDispose {}
        }
    }

    val displayText = editState.getDisplayText()
    val activeFont = font ?: Fonts.def
    var lastTapTime by remember { mutableLongStateOf(0L) }

    // Container điều khiển tương tác văn bản - Thuần túy không nền, không viền, không padding
    val coreInputModifier = Modifier
        .then(modifier)
        .hoverable(currentInteractionSource, enabled)
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onTap = { offset ->
                    val clickedChar = calculateCharIndexAtX(editState.getDisplayText(), offset.x, activeFont)
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime < DOUBLE_TAP_TIMEOUT_MILLIS) {
                        editState.selectWordAt(clickedChar)
                    } else {
                        editState.setCursor(clickedChar)
                    }
                    lastTapTime = now

                    if (!editState.isFocused) {
                        editState.isFocused = true
                        composeView?.requestKeyboardFocus(keyHandler)
                        val focus = FocusInteraction.Focus()
                        currentFocusInteraction = focus
                        currentInteractionSource.tryEmit(focus)
                    }
                }
            )
        }

    val innerTextField: @Composable () -> Unit = {
        BasicTextFieldVisualContent(
            editState = editState,
            displayText = displayText,
            activeFont = activeFont,
            isFocused = isFocused,
            textColor = textColor,
            cursorColor = cursorColor,
            selectionColor = selectionColor
        )
    }

    Box(modifier = coreInputModifier) {
        decorationBox(innerTextField)
    }
}

// ─── Visual Decoration Content ────────────────────────────────────

@Composable
private fun BasicTextFieldVisualContent(
    editState: TextFieldState,
    displayText: String,
    activeFont: Font?,
    isFocused: Boolean,
    textColor: Color,
    cursorColor: Color,
    selectionColor: Color
) {
    Box {
        // 1. Vẽ vùng bôi đen (Selection highlight) nếu có
        if (editState.hasSelection()) {
            val selectionMin = editState.selection.min
            val selectionMax = editState.selection.max
            val selectionStartX = calculateTextWidth(displayText, activeFont, 0, selectionMin)
            val selectionEndX = calculateTextWidth(displayText, activeFont, 0, selectionMax)
            val selectionWidth = (selectionEndX - selectionStartX).coerceAtLeast(DEFAULT_CURSOR_WIDTH)

            Box(
                modifier = Modifier
                    .offset(x = selectionStartX, y = 0f)
                    .size(width = selectionWidth, height = DEFAULT_SELECTION_HEIGHT)
                    .background(selectionColor, RoundedCorners(2f))
                    .align(Alignment.START, Alignment.CENTER)
            )
        }

        // 2. Vẽ gạch chân candidate khi IME đang soạn thảo (Composition Indicator)
        val compositionRange = editState.getCompositionRange()
        if (compositionRange != null) {
            val compositionStartX = calculateTextWidth(displayText, activeFont, 0, compositionRange.start)
            val compositionEndX = calculateTextWidth(displayText, activeFont, 0, compositionRange.end)
            val compositionWidth = (compositionEndX - compositionStartX).coerceAtLeast(DEFAULT_CURSOR_WIDTH)

            Box(
                modifier = Modifier
                    .offset(x = compositionStartX, y = DEFAULT_COMPOSITION_INDICATOR_OFFSET_Y)
                    .size(width = compositionWidth, height = DEFAULT_COMPOSITION_INDICATOR_HEIGHT)
                    .background(cursorColor)
                    .align(Alignment.START, Alignment.CENTER)
            )
        }

        // 3. Hiển thị chữ chính
        if (displayText.isNotEmpty()) {
            Text(
                text = displayText,
                textColor = textColor,
                font = activeFont,
                modifier = Modifier.align(Alignment.START, Alignment.CENTER)
            )
        }

        // 4. Vẽ con trỏ chuột nhấp nháy (Blinking Cursor Bar)
        if (isFocused && editState.cursorVisible) {
            val cursorCharIndex = editState.getEffectiveCursor()
            val cursorX = calculateTextWidth(displayText, activeFont, 0, cursorCharIndex.coerceIn(0, displayText.length))

            Box(
                modifier = Modifier
                    .offset(x = cursorX, y = 0f)
                    .size(width = DEFAULT_CURSOR_WIDTH, height = DEFAULT_CURSOR_HEIGHT)
                    .background(cursorColor)
                    .align(Alignment.START, Alignment.CENTER)
            )
        }
    }
}

// ─── Keyboard Handler Helper ──────────────────────────────────────

@Composable
private fun rememberKeyboardInputHandler(
    editState: TextFieldState,
    composeView: ComposeView?,
    currentInteractionSource: MutableInteractionSource,
    onEscapeOrFocusLost: (KeyboardInputHandler) -> Unit
): KeyboardInputHandler = remember(editState, composeView, currentInteractionSource) {
    object : KeyboardInputHandler {
        override fun onKeyTyped(character: Char): Boolean =
            editState.onKeyTyped(character)

        override fun onKeyDown(keyCode: KeyCode?): Boolean {
            if (keyCode == KeyCode.escape) {
                onEscapeOrFocusLost(this)
                return true
            }
            return editState.onKeyDown(keyCode)
        }

        override fun onKeyUp(keyCode: KeyCode?): Boolean = false

        override fun onFocusLost() {
            onEscapeOrFocusLost(this)
        }
    }
}

// ─── Text Layout & Binary Search Helpers ──────────────────────────

private val measureGlyphLayout by lazy { GlyphLayout() }

/**
 * Đo đạc chiều rộng của văn bản bằng font BMFont mà không cấp phát GlyphLayout mới (Zero-GC Rule 9.2).
 */
internal fun calculateTextWidth(text: String, font: Font?, start: Int = 0, end: Int = text.length): Float {
    val clampedStart = start.coerceIn(0, text.length)
    val clampedEnd = end.coerceIn(clampedStart, text.length)
    if (clampedStart >= clampedEnd) return 0f
    val activeFont = font ?: Fonts.def ?: return (clampedEnd - clampedStart) * FALLBACK_CHAR_WIDTH
    return try {
        measureGlyphLayout.setText(activeFont, text, clampedStart, clampedEnd, activeFont.color, 0f, arc.util.Align.left, false, null)
        measureGlyphLayout.width
    } catch (_: Throwable) {
        (clampedEnd - clampedStart) * FALLBACK_CHAR_WIDTH
    }
}

/**
 * Xác định chỉ số ký tự gần nhất với tọa độ ngang [localX] bằng Binary Search (O(log N)).
 */
internal fun calculateCharIndexAtX(text: String, localX: Float, font: Font?): Int {
    if (text.isEmpty() || localX <= 0f) return 0
    val activeFont = font ?: Fonts.def ?: return 0
    var low = 0
    var high = text.length
    var bestIndex = 0
    var minDiff = Float.MAX_VALUE

    while (low <= high) {
        val mid = (low + high) ushr 1
        val prefixWidth = calculateTextWidth(text, activeFont, 0, mid)
        val diff = abs(localX - prefixWidth)
        if (diff < minDiff) {
            minDiff = diff
            bestIndex = mid
        }
        when {
            prefixWidth < localX -> low = mid + 1
            prefixWidth > localX -> high = mid - 1
            else -> return mid
        }
    }
    return bestIndex
}

// ─── Focus & IME Cleanup Helpers ──────────────────────────────────

private fun clearFocusAndStopIme(
    editState: TextFieldState,
    composeView: ComposeView?,
    keyHandler: KeyboardInputHandler,
    source: MutableInteractionSource,
    focusInteraction: FocusInteraction.Focus?,
    onCleared: () -> Unit
) {
    editState.isFocused = false
    composeView?.clearKeyboardFocus(keyHandler)
    focusInteraction?.let { focus ->
        source.tryEmit(FocusInteraction.Unfocus(focus))
    }
    onCleared()
    SdlReflectionImeBridge.stopSession()
}

