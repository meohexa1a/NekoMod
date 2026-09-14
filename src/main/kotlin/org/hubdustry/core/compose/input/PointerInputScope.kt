package org.hubdustry.core.compose.input

import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resume

// ─── Configuration Contracts ──────────────────────────────────────

/**
 * Cấu hình ngưỡng cảm ứng và thời gian cử chỉ.
 */
interface ViewConfiguration {
    val touchSlop: Float get() = 12f
    val doubleTapTimeoutMillis: Long get() = 300L
    val longPressTimeoutMillis: Long get() = 500L
}

object DefaultViewConfiguration : ViewConfiguration

// ─── Pointer Input Scope Contracts ────────────────────────────────

/**
 * Phạm vi coroutine nhận diện cử chỉ con trỏ tại một node.
 */
interface PointerInputScope {
    val size: IntSize
    val viewConfiguration: ViewConfiguration get() = DefaultViewConfiguration

    /**
     * Bắt đầu một phạm vi await các sự kiện con trỏ theo từng pass ([PointerEventPass]).
     */
    suspend fun <R> awaitPointerEventScope(block: suspend AwaitPointerEventScope.() -> R): R
}

/**
 * Phạm vi con bên trong [awaitPointerEventScope], cho phép suspend và chờ từng [PointerEvent].
 */
interface AwaitPointerEventScope {
    val size: IntSize
    val viewConfiguration: ViewConfiguration
    val currentEvent: PointerEvent

    /**
     * Tạm dừng coroutine cho đến khi có sự kiện con trỏ mới tại lượt [pass] được chỉ định.
     */
    suspend fun awaitPointerEvent(pass: PointerEventPass = PointerEventPass.Main): PointerEvent
}

// ─── Suspending Pointer Input Filter ──────────────────────────────

/**
 * Bộ lọc Pointer Input sử dụng Coroutine Continuation, mô phỏng theo SuspendingPointerInputFilter của AOSP.
 * Quản lý vòng đời Coroutine và điều phối sự kiện 3-pass mà không gây side-effect hay rò rỉ bộ nhớ.
 */
class SuspendingPointerInputFilter(
    override val viewConfiguration: ViewConfiguration = DefaultViewConfiguration
) : PointerInputScope {

    override var size: IntSize = IntSize.Zero

    internal val handlers = ArrayList<PointerEventHandlerCoroutine<*>>()
    private val dispatchingHandlers = ArrayList<PointerEventHandlerCoroutine<*>>()

    override suspend fun <R> awaitPointerEventScope(block: suspend AwaitPointerEventScope.() -> R): R =
        suspendCancellableCoroutine { continuation ->
            val handler = PointerEventHandlerCoroutine(this, continuation, block)
            handlers.add(handler)
            continuation.invokeOnCancellation {
                handler.cancel(it)
                handlers.remove(handler)
            }
            handler.start()
        }

    /**
     * Điều phối sự kiện con trỏ tới các continuation đang chờ tại lượt [pass].
     * Hot-path Zero-GC: dùng vòng lặp chỉ mục thuần túy.
     */
    fun dispatchPointerEvent(event: PointerEvent, pass: PointerEventPass, bounds: IntSize = this.size) {
        this.size = bounds
        dispatchingHandlers.clear()
        handlers.fastForEach { dispatchingHandlers.add(it) }

        dispatchingHandlers.fastForEach { it.dispatch(event, pass) }
        dispatchingHandlers.clear()
    }

    /**
     * Hủy bỏ toàn bộ handler đang chờ và giải phóng tài nguyên.
     */
    fun reset() {
        handlers.fastForEach { it.cancel(null) }
        handlers.clear()
    }
}

// ─── Coroutine Handler Implementation ─────────────────────────────

internal class PointerEventHandlerCoroutine<R>(
    private val filter: SuspendingPointerInputFilter,
    private val completion: CancellableContinuation<R>,
    private val block: suspend AwaitPointerEventScope.() -> R
) : AwaitPointerEventScope, Continuation<R> {

    override val size: IntSize get() = filter.size
    override val viewConfiguration: ViewConfiguration get() = filter.viewConfiguration
    override var currentEvent: PointerEvent = PointerEvent(emptyList())

    var awaitingPass: PointerEventPass? = null
    var awaitingContinuation: CancellableContinuation<PointerEvent>? = null

    override val context: CoroutineContext = completion.context

    override fun resumeWith(result: Result<R>) {
        filter.handlers.remove(this)
        completion.resumeWith(result)
    }

    fun start() {
        val coroutine = block.createCoroutine(this, this)
        coroutine.resume(Unit)
    }

    override suspend fun awaitPointerEvent(pass: PointerEventPass): PointerEvent =
        suspendCancellableCoroutine { continuation ->
            awaitingPass = pass
            awaitingContinuation = continuation
            continuation.invokeOnCancellation {
                if (awaitingContinuation === continuation) {
                    awaitingContinuation = null
                    awaitingPass = null
                }
            }
        }

    fun dispatch(event: PointerEvent, pass: PointerEventPass) {
        if (awaitingPass != pass) return
        val continuation = awaitingContinuation ?: return

        awaitingPass = null
        awaitingContinuation = null
        currentEvent = event
        continuation.resume(event)
    }

    fun cancel(cause: Throwable?) {
        val continuation = awaitingContinuation ?: return
        awaitingPass = null
        awaitingContinuation = null
        continuation.cancel(cause)
    }
}

