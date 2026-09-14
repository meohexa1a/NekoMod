package org.hubdustry.core.compose.runtime

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import arc.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * [CompositionManager] — Điều phối nhịp đập khung hình (Frame Clock) và Recomposition cho NekoMod.
 *
 * TẠI SAO CẦN FILE NÀY?
 * Jetpack Compose không tự chạy được trong Mindustry nếu không có ai đập nhịp.
 * Mỗi frame của game loop, hàm [frame] sẽ được gọi để báo cho Compose tính toán lại giao diện.
 *
 * LƯU Ý VÒNG ĐỜI (Rule 0.5):
 * Đây là Singleton toàn cục của tiến trình, sống vĩnh viễn suốt phiên game.
 * Tuyệt đối không có hàm stop() hay dispose() để ngăn chặn việc bị hủy lén từ bên ngoài.
 */
object CompositionManager {

    /** Đồng hồ nhịp khung hình cung cấp cho luồng Coroutine của Compose. */
    val clock = BroadcastFrameClock()

    private val scope = CoroutineScope(
        Dispatchers.Unconfined + SupervisorJob() + clock + CoroutineExceptionHandler { _, err ->
            Log.err("[CompositionManager] Lỗi runtime trong Compose Recomposer", err)
        }
    )

    internal val recomposer = Recomposer(scope.coroutineContext)
    private var isStarted = false
    private var lastFrameId = -1L

    /**
     * Khởi động bộ máy Recomposer (được gọi tự động khi mở ComposeView đầu tiên).
     */
    fun start() {
        if (isStarted) return
        isStarted = true

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            recomposer.runRecomposeAndApplyChanges()
        }

        Snapshot.registerGlobalWriteObserver {
            Snapshot.sendApplyNotifications()
        }
    }

    /**
     * Đập nhịp khung hình đồng bộ từ Game Loop của Mindustry (mỗi frame gọi 1 lần).
     *
     * Cơ chế phòng vệ chống trùng lặp (Frame De-duplication):
     * Nếu [arc.Core.graphics] khả dụng (môi trường game runtime thực tế), kiểm tra [frameId] để đảm bảo
     * chỉ bơm nhịp 1 lần duy nhất cho mỗi khung hình GPU, dù có nhiều [ComposeView] cùng gọi
     * trong cùng 1 game tick.
     * Trong môi trường Headless Unit Test ([arc.Core.graphics] == null), bỏ qua de-duplication để
     * cho phép test harness tự do tua nhịp.
     */
    fun frame() {
        if (!isStarted) return

        val graphics = try { arc.Core.graphics } catch (_: Throwable) { null }
        val isRealApp = try { arc.Core.app != null } catch (_: Throwable) { false }
        val isMock = try { graphics is arc.mock.MockGraphics } catch (_: Throwable) { false }
        if (isRealApp && !isMock && graphics != null) {
            val currentFrameId = graphics.frameId
            if (currentFrameId > 0 && currentFrameId == lastFrameId) return
            lastFrameId = currentFrameId
        }

        Snapshot.sendApplyNotifications()
        clock.sendFrame(System.nanoTime())
    }
}
