package org.hubdustry.core.compose.input.ime

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SdlReflectionImeBridgeTest {

    @Test
    fun testSdlNativeHelperSafety() {
        // Kiểm tra không văng ngoại lệ dù SDL native có mặt hay không
        val available = SdlNativeHelper.isAvailable
        if (!available) {
            assertFalse(SdlNativeHelper.startTextInput())
            assertFalse(SdlNativeHelper.stopTextInput())
            assertFalse(SdlNativeHelper.setTextInputRect(0, 0, 100, 20))
        }
    }

    @Test
    fun testImeSessionLifecycle() {
        var compositionResult = ""
        var cleared = false

        val listener = object : ImeCompositionListener {
            override fun onCompositionChanged(composition: String) {
                compositionResult = composition
            }

            override fun onCompositionCleared() {
                cleared = true
            }
        }

        // Bắt đầu phiên (nếu không có SDL JNI, startSession graceful no-op)
        SdlReflectionImeBridge.startSession(
            screenX = 100f,
            screenY = 200f,
            width = 300f,
            height = 36f,
            listener = listener
        )

        // Cập nhật vị trí khung nhập
        SdlReflectionImeBridge.updateTextInputRect(150f, 250f, 300f, 36f)

        // Dừng phiên
        SdlReflectionImeBridge.stopSession()
        assertFalse(SdlReflectionImeBridge.isActive)

        // Gỡ hook an toàn
        SdlReflectionImeBridge.uninstallHook()
    }

    @Test
    fun testImeCompositionListenerCallbacks() {
        var lastComposition = ""
        var isCleared = false

        val listener = object : ImeCompositionListener {
            override fun onCompositionChanged(composition: String) {
                lastComposition = composition
            }

            override fun onCompositionCleared() {
                isCleared = true
            }
        }

        listener.onCompositionChanged("viet")
        kotlin.test.assertEquals("viet", lastComposition)

        listener.onCompositionCleared()
        assertTrue(isCleared)
    }
}
