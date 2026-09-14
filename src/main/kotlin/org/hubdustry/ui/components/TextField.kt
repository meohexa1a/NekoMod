package org.hubdustry.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import arc.Core
import arc.graphics.Color
import arc.graphics.g2d.Font
import arc.graphics.g2d.GlyphLayout
import arc.input.KeyCode
import mindustry.ui.Fonts
import kotlinx.coroutines.delay
import org.hubdustry.core.compose.Modifier
import org.hubdustry.core.compose.foundation.text.TextFieldState
import org.hubdustry.core.compose.input.FocusInteraction
import org.hubdustry.core.compose.input.HoverInteraction
import org.hubdustry.core.compose.input.MutableInteractionSource
import org.hubdustry.core.compose.input.collectIsFocusedAsState
import org.hubdustry.core.compose.input.gestures.detectTapGestures
import org.hubdustry.core.compose.input.hoverable
import org.hubdustry.core.compose.input.ime.ImeCompositionListener
import org.hubdustry.core.compose.input.ime.SdlReflectionImeBridge
import org.hubdustry.core.compose.input.pointerInput
import org.hubdustry.core.compose.modifier.alpha
import org.hubdustry.core.compose.modifier.background
import org.hubdustry.core.compose.modifier.border
import org.hubdustry.core.compose.modifier.offset
import org.hubdustry.core.compose.modifier.padding
import org.hubdustry.core.compose.modifier.size
import org.hubdustry.core.compose.modifier.sizeIn
import org.hubdustry.core.compose.primitive.Box
import org.hubdustry.core.compose.primitive.Text
import org.hubdustry.core.compose.view.KeyboardInputHandler
import org.hubdustry.core.compose.view.LocalComposeView
import org.hubdustry.core.graphics.RoundedCorners
import org.hubdustry.core.layout.Alignment
import kotlin.math.abs

internal val DefaultTextFieldBgColor = Color.valueOf("181926")
internal val DefaultTextFieldBorderColor = Color.valueOf("363a4f")
internal val DefaultTextFieldFocusBorderColor = Color.royal
internal val DefaultTextFieldSelectionColor = Color(0.25f, 0.45f, 0.95f, 0.35f)
internal val DefaultTextFieldShape = RoundedCorners(6f)

private val measureGlyphLayout by lazy { GlyphLayout() }

/**
 * Đo đạc chiều rộng của văn bản bằng font BMFont mà không cấp phát GlyphLayout mới (Zero-GC Rule 9.2).
 */
internal fun calculateTextWidth(text: String, font: Font?, start: Int = 0, end: Int = text.length): Float {
    val s = start.coerceIn(0, text.length)
    val e = end.coerceIn(s, text.length)
    if (s >= e) return 0f
    val f = font ?: Fonts.def ?: return (e - s) * 8f
    return try {
        measureGlyphLayout.setText(f, text, s, e, f.color, 0f, arc.util.Align.left, false, null)
        measureGlyphLayout.width
    } catch (_: Throwable) {
        (e - s) * 8f
    }
}

/**
 * Xác định chỉ số ký tự gần nhất với tọa độ ngang [localX] bằng Binary Search (O(log N)).
 */
internal fun calculateCharIndexAtX(text: String, localX: Float, font: Font?): Int {
    if (text.isEmpty() || localX <= 0f) return 0
    val f = font ?: Fonts.def ?: return 0
    var low = 0
    var high = text.length
    var bestIndex = 0
    var minDiff = Float.MAX_VALUE

    while (low <= high) {
        val mid = (low + high) ushr 1
        val w = calculateTextWidth(text, f, 0, mid)
        val diff = abs(localX - w)
        if (diff < minDiff) {
            minDiff = diff
            bestIndex = mid
        }
        if (w < localX) {
            low = mid + 1
        } else if (w > localX) {
            high = mid - 1
        } else {
            return mid
        }
    }
    return bestIndex
}

/**
 * Thành phần soạn thảo văn bản Composable [TextField] chuẩn Jetpack Compose cho NekoMod v3:
 * - Hỗ trợ đầy đủ bộ gõ tiếng Việt (Telex, VNI) và IME tiền soạn thảo CJK thông qua [SdlReflectionImeBridge].
 * - Kết nối hệ thống tương tác không side-effect qua [MutableInteractionSource] và [FocusInteraction].
 * - Quản lý con trỏ nhấp nháy, lựa chọn vùng văn bản, phím xóa lùi, xóa tiến và thao tác Clipboard.
 * - Hiển thị chuỗi ứng viên gạch chân nét đứt/sáng khi đang trong quá trình gõ IME.
 */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "",
    singleLine: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    shape: RoundedCorners = DefaultTextFieldShape,
    backgroundColor: Color = DefaultTextFieldBgColor,
    borderColor: Color = DefaultTextFieldBorderColor,
    focusedBorderColor: Color = DefaultTextFieldFocusBorderColor,
    textColor: Color = Color.white,
    placeholderColor: Color = Color.gray,
    cursorColor: Color = Color.royal,
    selectionColor: Color = DefaultTextFieldSelectionColor,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isExternalFocused by source.collectIsFocusedAsState()
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

    val keyHandler = remember(editState, composeView, source) {
        object : KeyboardInputHandler {
            override fun onKeyTyped(character: Char): Boolean =
                editState.onKeyTyped(character)

            override fun onKeyDown(keycode: KeyCode?): Boolean {
                if (keycode == KeyCode.escape) {
                    editState.isFocused = false
                    composeView?.clearKeyboardFocus(this)
                    currentFocusInteraction?.let { focus ->
                        source.tryEmit(FocusInteraction.Unfocus(focus))
                        currentFocusInteraction = null
                    }
                    return true
                }
                return editState.onKeyDown(keycode)
            }

            override fun onKeyUp(keycode: KeyCode?): Boolean = false

            override fun onFocusLost() {
                editState.isFocused = false
                currentFocusInteraction?.let { focus ->
                    source.tryEmit(FocusInteraction.Unfocus(focus))
                    currentFocusInteraction = null
                }
                SdlReflectionImeBridge.stopSession()
            }
        }
    }

    LaunchedEffect(enabled) {
        if (!enabled && editState.isFocused) {
            editState.isFocused = false
            composeView?.clearKeyboardFocus(keyHandler)
            currentFocusInteraction?.let { focus ->
                source.tryEmit(FocusInteraction.Unfocus(focus))
                currentFocusInteraction = null
            }
            SdlReflectionImeBridge.stopSession()
        }
    }

    // Coroutine nhấp nháy con trỏ khi nhận focus (cập nhật snapshot state editState.cursorVisible)
    LaunchedEffect(isFocused) {
        if (!isFocused) return@LaunchedEffect
        editState.resetBlink()
        while (true) {
            delay(500)
            editState.updateBlink(0.5f)
        }
    }

    // Đăng ký bàn phím và IME session khi nhận / mất focus
    DisposableEffect(isFocused, composeView) {
        if (isFocused && composeView != null) {
            composeView.requestKeyboardFocus(keyHandler)

            // Tính toán tọa độ màn hình chuẩn (Y-down) cho SDL IME Candidate Rect
            val screenH = Core.graphics?.height?.toFloat() ?: 1080f
            val screenX = composeView.x
            val screenY = (screenH - (composeView.y + composeView.height)).coerceAtLeast(0f)

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
                composeView.clearKeyboardFocus(keyHandler)
                currentFocusInteraction?.let { focus ->
                    source.tryEmit(FocusInteraction.Unfocus(focus))
                    currentFocusInteraction = null
                }
                SdlReflectionImeBridge.stopSession()
            }
        } else {
            onDispose {}
        }
    }

    val currentBorderColor = if (isFocused) focusedBorderColor else borderColor
    val currentBorderWidth = if (isFocused) 1.5f else 1f

    val displayText = editState.getDisplayText()
    val font = Fonts.def
    val horizontalPadding = 12f
    val verticalPadding = 8f

    var lastTapTime by remember { mutableStateOf(0L) }

    Box(
        modifier = Modifier
            .sizeIn(minWidth = 140f, minHeight = 36f)
            .then(modifier)
            .then(if (!enabled) Modifier.alpha(0.5f) else Modifier)
            .background(backgroundColor, shape)
            .border(currentBorderWidth, currentBorderColor, shape)
            .hoverable(source, enabled)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onTap = { offset ->
                        val localX = (offset.x - horizontalPadding).coerceAtLeast(0f)
                        val clickedChar = calculateCharIndexAtX(editState.getDisplayText(), localX, font)
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 300L) {
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
                            source.tryEmit(focus)
                        }
                    }
                )
            }
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
        // 1. Vẽ vùng bôi đen (Selection highlight) nếu có
        if (editState.hasSelection()) {
            val selMin = editState.selection.min
            val selMax = editState.selection.max
            val selStartX = calculateTextWidth(displayText, font, 0, selMin)
            val selEndX = calculateTextWidth(displayText, font, 0, selMax)
            val selW = (selEndX - selStartX).coerceAtLeast(2f)

            Box(
                modifier = Modifier
                    .offset(x = selStartX, y = 0f)
                    .size(width = selW, height = 20f)
                    .background(selectionColor, RoundedCorners(2f))
                    .align(Alignment.START, Alignment.CENTER)
            )
        }

        // 2. Vẽ gạch chân candidate khi IME đang soạn thảo (Composition Indicator)
        val compRange = editState.getCompositionRange()
        if (compRange != null) {
            val compStartX = calculateTextWidth(displayText, font, 0, compRange.start)
            val compEndX = calculateTextWidth(displayText, font, 0, compRange.end)
            val compW = (compEndX - compStartX).coerceAtLeast(2f)

            Box(
                modifier = Modifier
                    .offset(x = compStartX, y = 10f)
                    .size(width = compW, height = 2f)
                    .background(focusedBorderColor)
                    .align(Alignment.START, Alignment.CENTER)
            )
        }

        // 3. Hiển thị chữ chính hoặc placeholder
        if (displayText.isEmpty() && !isFocused) {
            Text(
                text = placeholder,
                textColor = placeholderColor,
                modifier = Modifier.align(Alignment.START, Alignment.CENTER)
            )
        } else {
            Text(
                text = displayText,
                textColor = textColor,
                modifier = Modifier.align(Alignment.START, Alignment.CENTER)
            )
        }

        // 4. Vẽ con trỏ chuột nhấp nháy (Blinking Cursor Bar)
        if (isFocused && editState.cursorVisible) {
            val cursorCharIndex = editState.getEffectiveCursor()
            val cursorX = calculateTextWidth(displayText, font, 0, cursorCharIndex.coerceIn(0, displayText.length))

            Box(
                modifier = Modifier
                    .offset(x = cursorX, y = 0f)
                    .size(width = 2f, height = 18f)
                    .background(cursorColor)
                    .align(Alignment.START, Alignment.CENTER)
            )
        }
    }
}
