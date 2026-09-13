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

internal class PressGestureScopeImpl : PressGestureScope {
    private var deferred = CompletableDeferred<Boolean>()

    override suspend fun awaitRelease() {
        if (!tryAwaitRelease()) {
            throw CancellationException("The press gesture was canceled.")
        }
    }

    override suspend fun tryAwaitRelease(): Boolean {
        return deferred.await()
    }

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

/**
 * Chờ sự kiện con trỏ đầu tiên chuyển sang trạng thái Down theo chuẩn AOSP Compose.
 */
suspend fun AwaitPointerEventScope.awaitFirstDown(
    requireUnconsumed: Boolean = true,
    pass: PointerEventPass = PointerEventPass.Main
): PointerInputChange {
    while (true) {
        val event = awaitPointerEvent(pass)
        val down = event.changes.fastFirstOrNull { c ->
            if (requireUnconsumed) c.changedToDown else c.changedToDownIgnoreConsumed
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

/**
 * Bộ máy nhận diện cử chỉ Tap theo chuẩn Jetpack Compose AOSP, hỗ trợ cả Touch và Desktop Mouse.
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
        val down = awaitFirstDown()
        down.consume()

        // 1. Nhận diện Secondary Click (Chuột phải trên PC / Desktop)
        if (down.button == PointerButton.Secondary && onSecondaryTap != null) {
            val up = waitForUpOrCancellation(down.id) ?: return@awaitEachGesture
            up.consume()
            onSecondaryTap(up.position)
            return@awaitEachGesture
        }

        // 2. Kích hoạt onPress
        pressScope.reset()
        if (onPress != null) {
            this@coroutineScope.launch {
                pressScope.onPress(down.position)
            }
        }

        val touchSlop = viewConfiguration.touchSlop
        var isLongPressed = false
        val longPressJob = if (onLongPress != null) {
            this@coroutineScope.launch {
                delay(viewConfiguration.longPressTimeoutMillis)
                isLongPressed = true
                onLongPress(down.position)
            }
        } else null

        // 3. Theo dõi chuyển động
        var upOrCancel: PointerInputChange? = null
        while (upOrCancel == null) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val matched = event.changes.fastFirstOrNull { it.id == down.id }

            if (matched == null || matched.isConsumed || matched.isOutOfBounds(size, touchSlop)) {
                break
            }

            val dx = matched.position.x - down.position.x
            val dy = matched.position.y - down.position.y
            if (dx * dx + dy * dy > touchSlop * touchSlop) {
                longPressJob?.cancel()
                break
            }

            if (matched.changedToUp) {
                upOrCancel = matched
                matched.consume()
                break
            }
        }

        longPressJob?.cancel()

        if (upOrCancel == null) {
            pressScope.cancel()
            return@awaitEachGesture
        }

        pressScope.release()

        if (isLongPressed) {
            return@awaitEachGesture
        }

        val up = upOrCancel

        // 4. Phân định Single-tap vs Double-tap
        if (onDoubleTap == null) {
            onTap?.invoke(up.position)
            return@awaitEachGesture
        }

        var secondDown: PointerInputChange? = null
        try {
            withTimeout(viewConfiguration.doubleTapTimeoutMillis.milliseconds) {
                while (secondDown == null) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val c = event.changes.fastFirstOrNull { it.changedToDown } ?: continue
                    val ddx = c.position.x - up.position.x
                    val ddy = c.position.y - up.position.y
                    val maxDoubleTapDist = touchSlop * 2f
                    if (ddx * ddx + ddy * ddy <= maxDoubleTapDist * maxDoubleTapDist) {
                        secondDown = c
                        c.consume()
                    }
                    break
                }
            }
        } catch (_: TimeoutCancellationException) {
            onTap?.invoke(up.position)
            return@awaitEachGesture
        }

        val validSecondDown = secondDown ?: return@awaitEachGesture
        val secondUp = waitForUpOrCancellation(validSecondDown.id) ?: return@awaitEachGesture
        secondUp.consume()
        onDoubleTap(validSecondDown.position)
    }
}
