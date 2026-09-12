package org.hubdustry.libs.compose.input

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resume

/**
 * Cấu hình ngưỡng cảm ứng và thời gian cử chỉ.
 */
interface ViewConfiguration {
    val touchSlop: Float get() = 12f
    val doubleTapTimeoutMillis: Long get() = 300L
    val longPressTimeoutMillis: Long get() = 500L
}

object DefaultViewConfiguration : ViewConfiguration

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

/**
 * Bộ lọc Pointer Input sử dụng Coroutine Continuation, mô phỏng theo SuspendingPointerInputFilter của AOSP.
 * Quản lý vòng đời Coroutine và điều phối sự kiện 3-pass mà không gây side-effect hay rò rỉ bộ nhớ.
 */
class SuspendingPointerInputFilter(
    override val viewConfiguration: ViewConfiguration = DefaultViewConfiguration
) : PointerInputScope {

    override var size: IntSize = IntSize.Zero

    var coroutineScope: CoroutineScope? = null

    internal val handlers = ArrayList<PointerEventHandlerCoroutine<*>>()
    private val dispatchingHandlers = ArrayList<PointerEventHandlerCoroutine<*>>()

    override suspend fun <R> awaitPointerEventScope(block: suspend AwaitPointerEventScope.() -> R): R =
        suspendCancellableCoroutine { continuation ->
            val handler = PointerEventHandlerCoroutine(this, continuation, block)
            synchronized(handlers) {
                handlers.add(handler)
            }
            continuation.invokeOnCancellation {
                handler.cancel(it)
                synchronized(handlers) {
                    handlers.remove(handler)
                }
            }
            handler.start()
        }

    /**
     * Điều phối sự kiện con trỏ tới các continuation đang chờ tại lượt [pass].
     * Hot-path Zero-GC: dùng vòng lặp chỉ mục thuần túy.
     */
    fun dispatchPointerEvent(event: PointerEvent, pass: PointerEventPass, bounds: IntSize = this.size) {
        this.size = bounds
        synchronized(handlers) {
            dispatchingHandlers.clear()
            val count = handlers.size
            for (i in 0 until count) {
                dispatchingHandlers.add(handlers[i])
            }
        }

        val dispatchCount = dispatchingHandlers.size
        for (i in 0 until dispatchCount) {
            dispatchingHandlers[i].dispatch(event, pass)
        }
        dispatchingHandlers.clear()
    }

    /**
     * Hủy bỏ toàn bộ handler đang chờ và giải phóng tài nguyên.
     */
    fun reset() {
        synchronized(handlers) {
            val count = handlers.size
            for (i in 0 until count) {
                handlers[i].cancel(null)
            }
            handlers.clear()
        }
    }
}

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
        synchronized(filter.handlers) {
            filter.handlers.remove(this)
        }
        completion.resumeWith(result)
    }

    fun start() {
        val coroutine = block.createCoroutine(this, this)
        coroutine.resume(Unit)
    }

    override suspend fun awaitPointerEvent(pass: PointerEventPass): PointerEvent {
        return suspendCancellableCoroutine { cont ->
            awaitingPass = pass
            awaitingContinuation = cont
            cont.invokeOnCancellation {
                if (awaitingContinuation === cont) {
                    awaitingContinuation = null
                    awaitingPass = null
                }
            }
        }
    }

    fun dispatch(event: PointerEvent, pass: PointerEventPass) {
        if (awaitingPass == pass) {
            val cont = awaitingContinuation
            awaitingPass = null
            awaitingContinuation = null
            currentEvent = event
            cont?.resume(event)
        }
    }

    fun cancel(cause: Throwable?) {
        val cont = awaitingContinuation
        awaitingPass = null
        awaitingContinuation = null
        cont?.cancel(cause)
    }
}
