package org.hubdustry.core.compose.input.ime

import java.lang.reflect.Method

/**
 * ## SdlNativeHelper
 *
 * Cung cấp cầu nối phản chiếu (Reflection) an toàn đến các API JNI của SDL3/SDL2 (`arc.backend.sdl.jni.SDL`).
 * Cho phép điều khiển kích hoạt IME và định vị khung ứng viên (candidate window) theo vị trí con trỏ màn hình.
 *
 * Tự động vô hiệu hóa mà không ném ngoại lệ khi chạy trên Android, iOS hoặc môi trường JVM test không có SDL.
 */
object SdlNativeHelper {

    val isAvailable: Boolean by lazy {
        try {
            Class.forName("arc.backend.sdl.jni.SDL")
            true
        } catch (_: Throwable) {
            false
        }
    }

    private val startTextInputMethod: Method? by lazy {
        resolveMethod("SDL_StartTextInput")
    }

    private val stopTextInputMethod: Method? by lazy {
        resolveMethod("SDL_StopTextInput")
    }

    private val setTextInputRectMethod: Method? by lazy {
        resolveMethod(
            "SDL_SetTextInputRect",
            Integer.TYPE,
            Integer.TYPE,
            Integer.TYPE,
            Integer.TYPE
        )
    }

    private fun resolveMethod(name: String, vararg parameterTypes: Class<*>): Method? =
        try {
            Class.forName("arc.backend.sdl.jni.SDL").getMethod(name, *parameterTypes)
        } catch (_: Throwable) {
            null
        }

    /**
     * Bắt đầu phiên nhập văn bản native của hệ điều hành (hiển thị IME candidate box nếu có).
     */
    fun startTextInput(): Boolean {
        val method = startTextInputMethod ?: return false
        return try {
            method.invoke(null)
            true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Kết thúc phiên nhập văn bản native của hệ điều hành.
     */
    fun stopTextInput(): Boolean {
        val method = stopTextInputMethod ?: return false
        return try {
            method.invoke(null)
            true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Cập nhật tọa độ và kích thước khung nhập liệu cho hệ điều hành định vị cửa sổ ứng viên IME.
     * Lưu ý: SDL sử dụng hệ tọa độ Top-Left ($Y$-down).
     */
    fun setTextInputRect(x: Int, y: Int, width: Int, height: Int): Boolean {
        val method = setTextInputRectMethod ?: return false
        return try {
            method.invoke(null, x, y, width, height)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
