package org.mdt.core.platform.port

import okio.Path
import okio.Path.Companion.toPath

/**
 * ## SystemPort [OS, Timing & Dispatch Contract]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Defines the abstract contract for frame timing, main thread dispatch, clipboard, URI opening, and data persistence paths.
 * - Includes a built-in [NoOp] stub for headless execution or unit testing.
 *
 * ### 2. Key Rules & Checklist
 * - [x] `frameId` must be monotonically increasing.
 * - [x] `deltaTime` must return a non-zero positive Float during active rendering.
 *
 * ### 3. Related Files
 * - Implementation: `src/main/kotlin/org/mdt/core/platform/impl/MindustrySystemPort.kt`
 * - Platform Host: `src/main/kotlin/org/mdt/core/platform/PlatformHost.kt`
 */
interface SystemPort {

    /** Monotonically increasing frame counter for frame-indexing and caching. */
    val frameId: Long

    /** Time elapsed between the previous frame and the current frame in seconds. */
    val deltaTime: Float

    /** Current timestamp in milliseconds. */
    fun nowMillis(): Long

    /** Posts a callback to the platform's main / render thread. */
    fun postToMainThread(block: () -> Unit)

    /** Registers a hook executed at the end of every render frame. */
    fun onFrameEnd(block: () -> Unit)

    /** Removes a hook executed at the end of every render frame. */
    fun removeFrameEnd(block: () -> Unit)

    /** Resolves the default persistent application data directory for the host environment. */
    fun resolveDefaultDataDir(appName: String): Path

    /** Retrieves the current system clipboard text content. */
    fun getClipboard(): String

    /** Sets the system clipboard text content. */
    fun setClipboard(text: String)

    /** Opens a URI in the default external system web browser or application. */
    fun openURI(uri: String): Boolean

    /** Stub [SystemPort] implementation for headless or testing environments. */
    object NoOp : SystemPort {
        override val frameId: Long get() = 1L
        override val deltaTime: Float get() = 0.0166667f
        override fun nowMillis(): Long = System.currentTimeMillis()
        override fun postToMainThread(block: () -> Unit) = block()
        override fun onFrameEnd(block: () -> Unit) = Unit
        override fun removeFrameEnd(block: () -> Unit) = Unit
        override fun resolveDefaultDataDir(appName: String): Path = "./.$appName".toPath()
        override fun getClipboard(): String = ""
        override fun setClipboard(text: String) = Unit
        override fun openURI(uri: String): Boolean = false
    }
}
