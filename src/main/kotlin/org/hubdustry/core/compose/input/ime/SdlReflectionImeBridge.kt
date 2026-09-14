package org.hubdustry.core.compose.input.ime

import arc.Core
import arc.struct.Seq
import arc.util.Log
import java.lang.reflect.Field

/**
 * Interface đón nhận sự kiện gõ dở (IME candidate composition) từ hệ điều hành.
 */
interface ImeCompositionListener {
    /**
     * Được gọi khi người dùng đang gõ các ký tự ứng viên (ví dụ: gõ "w" trong Telex tạo "ư", hoặc Pinyin buffer).
     */
    fun onCompositionChanged(composition: String)

    /**
     * Được gọi khi chuỗi ứng viên bị hủy hoặc xóa rỗng.
     */
    fun onCompositionCleared()
}

/**
 * ## SdlReflectionImeBridge
 *
 * Cầu nối trung gian chặn bắt các sự kiện tiền soạn thảo IME (`SDL_EVENT_TEXT_EDIT`) trực tiếp từ
 * `SdlInput.stringEditEvents` thông qua Java Reflection.
 *
 * GIẢI PHÁP & TỐI ƯU V3:
 * 1. Bỏ qua hoàn toàn cơ chế hardcode `getKeyboardFocus() instanceof TextField` của Arc Scene2D.
 * 2. **Passthrough Minh Bạch**: Khi không có Composable TextField nào active, `proxySeq` hoạt động
 *    như `Seq` bình thường, giúp các ô nhập liệu gốc của Mindustry (như chat box, đổi tên map)
 *    vẫn nhận candidate bình thường (khắc phục lỗi nuốt candidate của v1).
 * 3. **Interception & Swallowing**: Khi có phiên IME của NekoMod active, `proxySeq` tóm lấy candidate,
 *    bắn sang [ImeCompositionListener] và nuốt sự kiện để không gây tác dụng phụ lên Arc.
 * 4. Tự động định vị khung ứng viên native (`SDL_SetTextInputRect`) qua [SdlNativeHelper].
 */
object SdlReflectionImeBridge {

    private var activeListener: ImeCompositionListener? = null
    private var isHookInstalled: Boolean = false
    private var isSessionActive: Boolean = false

    private var stringEditEventsField: Field? = null
    private var originalSeq: Seq<Any>? = null
    private var cachedEventTextField: Field? = null

    /**
     * Bắt đầu một phiên soạn thảo IME cho Composable Text Field tại vị trí màn hình chỉ định.
     */
    fun startSession(
        screenX: Float,
        screenY: Float,
        width: Float,
        height: Float,
        listener: ImeCompositionListener,
    ) {
        if (!SdlNativeHelper.isAvailable) return

        ensureHook()
        activeListener = listener
        isSessionActive = true

        SdlNativeHelper.startTextInput()
        updateTextInputRect(screenX, screenY, width, height)
    }

    /**
     * Cập nhật vị trí màn hình của khung nhập liệu để hệ điều hành dời cửa sổ candidate.
     */
    fun updateTextInputRect(screenX: Float, screenY: Float, width: Float, height: Float) {
        if (!isSessionActive) return
        SdlNativeHelper.setTextInputRect(screenX.toInt(), screenY.toInt(), width.toInt(), height.toInt())
    }

    /**
     * Dừng phiên soạn thảo IME hiện tại, ngắt kích hoạt candidate box của hệ điều hành.
     */
    fun stopSession() {
        if (!isSessionActive) return
        isSessionActive = false
        activeListener = null
        SdlNativeHelper.stopTextInput()
    }

    /**
     * Kiểm tra trạng thái đang có phiên nhập liệu nào hoạt động hay không.
     */
    val isActive: Boolean
        get() = isSessionActive

    // ─── 2. REFLECTION HOOK & INTERCEPTION ──────────────────────────────────

    private fun ensureHook() {
        if (isHookInstalled) return

        val input = Core.input ?: return
        try {
            val field = findField(input.javaClass, "stringEditEvents") ?: run {
                Log.warn("[NekoMod] Unable to locate 'stringEditEvents' field on SdlInput.")
                return
            }
            field.isAccessible = true

            @Suppress("UNCHECKED_CAST")
            val original = field.get(input) as? Seq<Any> ?: run {
                Log.warn("[NekoMod] 'stringEditEvents' field is null or incompatible.")
                return
            }

            this.stringEditEventsField = field
            this.originalSeq = original

            val proxySeq = ImeStringEditProxySeq(
                activeListenerProvider = { activeListener },
                textExtractor = ::extractEditText
            )

            field.set(input, proxySeq)
            isHookInstalled = true
            Log.info("[NekoMod] SdlReflectionImeBridge hook installed successfully.")
        } catch (throwable: Throwable) {
            Log.err("[NekoMod] Failed to install SdlReflectionImeBridge hook", throwable)
        }
    }

    /**
     * Gỡ bỏ hook và hoàn trả `Seq` nguyên bản của Arc khi mod bị unload.
     */
    fun uninstallHook() {
        if (!isHookInstalled) return
        val input = Core.input ?: return
        val field = stringEditEventsField ?: return
        val original = originalSeq ?: return

        try {
            field.set(input, original)
            isHookInstalled = false
            activeListener = null
            isSessionActive = false
            Log.info("[NekoMod] SdlReflectionImeBridge hook uninstalled.")
        } catch (throwable: Throwable) {
            Log.err("[NekoMod] Failed to uninstall SdlReflectionImeBridge hook", throwable)
        }
    }

    // ─── 3. REFLECTION EXTRACTION HELPERS ────────────────────────────────────

    private fun extractEditText(editEvent: Any): String? {
        val field = cachedEventTextField ?: findField(editEvent.javaClass, "text")?.also {
            it.isAccessible = true
            cachedEventTextField = it
        } ?: return null

        return try {
            field.get(editEvent) as? String
        } catch (_: Throwable) {
            null
        }
    }

    private fun findField(targetClass: Class<*>, name: String): Field? {
        var current: Class<*>? = targetClass
        while (current != null && current != Any::class.java) {
            try {
                return current.getDeclaredField(name)
            } catch (_: NoSuchFieldException) {
                current = current.superclass
            }
        }
        return null
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. PROXY SEQUENCE IMPLEMENTATION
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Lớp bọc ủy quyền cho `Seq<Any>` của Arc để chặn bắt và nuốt các sự kiện IME
 * khi có ô nhập liệu Compose đang active, hoặc passthrough cho Arc khi rảnh rỗi.
 */
private class ImeStringEditProxySeq(
    private val activeListenerProvider: () -> ImeCompositionListener?,
    private val textExtractor: (Any) -> String?
) : Seq<Any>() {
    override fun add(value: Any): Seq<Any> {
        val listener = activeListenerProvider()
        if (listener == null) {
            // Passthrough cho các ô nhập liệu gốc của Mindustry
            return super.add(value)
        }

        val text = textExtractor(value) ?: return super.add(value)
        if (text.isNotEmpty()) {
            listener.onCompositionChanged(text)
        } else {
            listener.onCompositionCleared()
        }

        // Nuốt sự kiện: không chuyển tiếp vào Seq gốc của Arc để tránh lỗi kiểm tra instanceof
        return this
    }
}
