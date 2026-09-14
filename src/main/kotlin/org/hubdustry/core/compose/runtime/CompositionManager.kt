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
     */
    fun frame() {
        if (!isStarted) return
        Snapshot.sendApplyNotifications()
        clock.sendFrame(System.nanoTime())
    }
}
