package org.hubdustry.core.compose.input.gestures

import androidx.compose.ui.util.fastFirstOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.hubdustry.core.compose.input.AwaitPointerEventScope
import org.hubdustry.core.compose.input.Offset
import org.hubdustry.core.compose.input.PointerButton
import org.hubdustry.core.compose.input.PointerEventPass
import org.hubdustry.core.compose.input.PointerId
import org.hubdustry.core.compose.input.PointerInputChange
import org.hubdustry.core.compose.input.PointerInputScope
import kotlin.time.Duration.Companion.milliseconds

private const val DOUBLE_TAP_SLOP_MULTIPLIER: Float = 2.0f

// ─── Public Tap Gesture Scope ─────────────────────────────────────

/**
 * Phạm vi điều khiển trạng thái cho callback onPress theo chuẩn AOSP Compose.
 */
interface PressGestureScope {
    /**
     * Chờ con trỏ được nhả ra hợp lệ. Ném [CancellationException] nếu cử chỉ bị hủy.
     */
    suspend fun awaitRelease()

    /**
     * Chờ con trỏ được nhả ra. Trả về true nếu nhả thành công, false nếu bị hủy.
     */
    suspend fun tryAwaitRelease(): Boolean

    /**
     * Khởi tạo lại trạng thái để tái sử dụng instance.
     */
    fun reset()
}

// ─── Public Tap Gesture API ───────────────────────────────────────

/**
 * Bộ máy nhận diện cử chỉ Tap theo chuẩn Jetpack Compose AOSP, hỗ trợ cả Touch và Desktop Mouse.
 *
 * ```
 *                     ┌─────────────┐
 *                     │ First Down  │
 *                     └──────┬──────┘
 *                            │
 *               ┌────────────┴────────────┐
 *               ▼                         ▼
 *         [Right Click]              [Left / Touch]
 *               │                         │
 *        onSecondaryTap()        ┌────────┴────────┐
 *                                │                 │
 *                       (Timeout > 500ms)     (Release Up)
 *                                │                 │
 *                          onLongPress()      [Wait 300ms]
 *                                             ┌────┴────┐
 *                                             ▼         ▼
 *                                        Second Down  Timeout
 *                                             │         │
 *                                       onDoubleTap() onTap()
 * ```
 *
 * Hỗ trợ:
 * - onPress: kích hoạt ngay khi con trỏ Down.
 * - Touch-Slop & Out-Of-Bounds: nếu con trỏ dịch chuyển quá slop hoặc rời khỏi bounds thì hủy tap.
 * - Long-Press: kích hoạt khi con trỏ giữ yên quá longPressTimeoutMillis (mặc định 500ms).
 * - Double-Tap: kích hoạt khi có lần tap thứ hai trong cửa sổ doubleTapTimeoutMillis (mặc định 300ms).
 * - onTap: kích hoạt khi nhả chuột hợp lệ trong bounds (chuột trái / touch).
 * - onSecondaryTap: kích hoạt khi click chuột phải trên Desktop.
 */
suspend fun PointerInputScope.detectTapGestures(
    onDoubleTap: ((Offset) -> Unit)? = null,
    onLongPress: ((Offset) -> Unit)? = null,
    onPress: (suspend PressGestureScope.(Offset) -> Unit)? = null,
    onTap: ((Offset) -> Unit)? = null,
    onSecondaryTap: ((Offset) -> Unit)? = null
) = coroutineScope {
    val pressScope = PressGestureScopeImpl()

    awaitEachGesture {
        val firstDown = awaitFirstDown()
        firstDown.consume()

        // 1. Nhận diện Secondary Click (Chuột phải trên PC / Desktop)
        if (firstDown.button == PointerButton.Secondary && onSecondaryTap != null) {
            val secondaryUp = waitForUpOrCancellation(firstDown.id) ?: return@awaitEachGesture
            secondaryUp.consume()
            onSecondaryTap(secondaryUp.position)
            return@awaitEachGesture
        }

        // 2. Kích hoạt onPress
        pressScope.reset()
        if (onPress != null) {
            this@coroutineScope.launch {
                pressScope.onPress(firstDown.position)
            }
        }

        val touchSlop = viewConfiguration.touchSlop
        var isLongPressed = false
        val longPressJob = if (onLongPress != null) {
            this@coroutineScope.launch {
                delay(viewConfiguration.longPressTimeoutMillis)
                isLongPressed = true
                onLongPress(firstDown.position)
            }
        } else null

        // 3. Theo dõi chuyển động
        var trackedUpOrCancel: PointerInputChange? = null
        while (trackedUpOrCancel == null) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val matched = event.changes.fastFirstOrNull { it.id == firstDown.id }

            if (matched == null || matched.isConsumed || matched.isOutOfBounds(size, touchSlop)) {
                break
            }

            val deltaX = matched.position.x - firstDown.position.x
            val deltaY = matched.position.y - firstDown.position.y
            if (deltaX * deltaX + deltaY * deltaY > touchSlop * touchSlop) {
                longPressJob?.cancel()
                break
            }

            if (matched.changedToUp) {
                trackedUpOrCancel = matched
                matched.consume()
                break
            }
        }

        longPressJob?.cancel()

        if (trackedUpOrCancel == null) {
            pressScope.cancel()
            return@awaitEachGesture
        }

        pressScope.release()

        if (isLongPressed) {
            return@awaitEachGesture
        }

        val firstUp = trackedUpOrCancel

        // 4. Phân định Single-tap vs Double-tap
        if (onDoubleTap == null) {
            onTap?.invoke(firstUp.position)
            return@awaitEachGesture
        }

        val secondDown = awaitSecondDownOrNull(
            firstUpPosition = firstUp.position,
            touchSlop = touchSlop,
            doubleTapTimeoutMillis = viewConfiguration.doubleTapTimeoutMillis
        )

        if (secondDown == null) {
            onTap?.invoke(firstUp.position)
            return@awaitEachGesture
        }

        val secondUp = waitForUpOrCancellation(secondDown.id) ?: return@awaitEachGesture
        secondUp.consume()
        onDoubleTap(secondDown.position)
    }
}

// ─── Internal Tap & Pointer Detection Helpers ─────────────────────

/**
 * Chờ sự kiện con trỏ đầu tiên chuyển sang trạng thái Down theo chuẩn AOSP Compose.
 */
suspend fun AwaitPointerEventScope.awaitFirstDown(
    requireUnconsumed: Boolean = true,
    pass: PointerEventPass = PointerEventPass.Main
): PointerInputChange {
    while (true) {
        val event = awaitPointerEvent(pass)
        val down = event.changes.fastFirstOrNull { change ->
            if (requireUnconsumed) change.changedToDown else change.changedToDownIgnoreConsumed
        }
        if (down != null) return down
    }
}

/**
 * Chờ cho đến khi con trỏ [pointerId] được nhả ra hợp lệ (Up) hoặc cử chỉ bị hủy (Move quá slop / out of bounds / consumed).
 * Trả về [PointerInputChange] của cú Up thành công, hoặc null nếu bị hủy.
 */
suspend fun AwaitPointerEventScope.waitForUpOrCancellation(
    pointerId: PointerId? = null,
    pass: PointerEventPass = PointerEventPass.Main
): PointerInputChange? {
    while (true) {
        val event = awaitPointerEvent(pass)
        val changes = event.changes

        val matched = if (pointerId != null) {
            changes.fastFirstOrNull { it.id == pointerId }
        } else {
            if (changes.isNotEmpty()) changes[0] else null
        }

        if (matched == null || matched.isConsumed) return null
        if (matched.isOutOfBounds(size, viewConfiguration.touchSlop)) return null
        if (matched.changedToUp) return matched
    }
}

private suspend fun AwaitPointerEventScope.awaitSecondDownOrNull(
    firstUpPosition: Offset,
    touchSlop: Float,
    doubleTapTimeoutMillis: Long
): PointerInputChange? = try {
    withTimeout(doubleTapTimeoutMillis.milliseconds) {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val change = event.changes.fastFirstOrNull { it.changedToDown } ?: continue
            val deltaTapX = change.position.x - firstUpPosition.x
            val deltaTapY = change.position.y - firstUpPosition.y
            val maxDoubleTapDist = touchSlop * DOUBLE_TAP_SLOP_MULTIPLIER
            if (deltaTapX * deltaTapX + deltaTapY * deltaTapY <= maxDoubleTapDist * maxDoubleTapDist) {
                change.consume()
                return@withTimeout change
            }
            return@withTimeout null
        }
        @Suppress("UNREACHABLE_CODE")
        null
    }
} catch (_: TimeoutCancellationException) {
    null
}

// ─── Press Gesture Scope Implementation ───────────────────────────

internal class PressGestureScopeImpl : PressGestureScope {
    private var deferred = CompletableDeferred<Boolean>()

    override suspend fun awaitRelease() {
        if (!tryAwaitRelease()) {
            throw CancellationException("The press gesture was canceled.")
        }
    }

    override suspend fun tryAwaitRelease(): Boolean = deferred.await()

    fun release() {
        deferred.complete(true)
    }

    fun cancel() {
        deferred.complete(false)
    }

    override fun reset() {
        deferred.cancel()
        deferred = CompletableDeferred()
    }
}

