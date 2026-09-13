package org.hubdustry.core.compose.input.gestures

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import org.hubdustry.core.compose.input.AwaitPointerEventScope
import org.hubdustry.core.compose.input.PointerEventPass
import org.hubdustry.core.compose.input.PointerInputScope

/**
 * Lặp lại việc lắng nghe các cử chỉ con trỏ độc lập theo chuẩn AOSP Compose.
 * Tự động đảm bảo dọn dẹp và chờ toàn bộ con trỏ nhả ra ([awaitAllPointersUp])
 * khi khối xử lý [block] hoàn thành hoặc bị hủy.
 */
suspend fun PointerInputScope.awaitEachGesture(
    block: suspend AwaitPointerEventScope.() -> Unit
) {
    val currentContext = currentCoroutineContext()
    while (currentContext.isActive) {
        try {
            awaitPointerEventScope {
                block()
            }
        } catch (c: CancellationException) {
            if (currentContext.isActive) {
                // Khối cử chỉ bị hủy cục bộ, chờ tất cả con trỏ nhả ra trước khi lặp lại
                awaitPointerEventScope {
                    awaitAllPointersUp()
                }
            } else {
                throw c
            }
        }
    }
}

/**
 * Chờ cho đến khi tất cả các con trỏ được nhả ra hoàn toàn (All pointers up).
 * Hot-path Zero-GC: sử dụng thuộc tính hasPressed của PointerEvent.
 */
suspend fun AwaitPointerEventScope.awaitAllPointersUp() {
    if (!currentEvent.hasPressed) return
    while (awaitPointerEvent(PointerEventPass.Final).hasPressed) {
        // Tiếp tục chờ cho đến khi tất cả các con trỏ được nhả hoàn toàn
    }
}
