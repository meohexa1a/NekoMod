// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform

import androidx.compose.runtime.staticCompositionLocalOf
import arc.Graphics.Cursor
import arc.graphics.g2d.Font
import arc.graphics.g2d.TextureRegion
import arc.input.InputProcessor
import okio.Path
import org.mdt.core.platform.ime.ImePort
import org.mdt.core.platform.port.AssetPort
import org.mdt.core.platform.port.InputPort
import org.mdt.core.platform.port.SystemPort
import org.mdt.core.platform.port.WindowPort

/**
 * ## PlatformHost
 *
 * Master composite facade coordinating platform sub-ports ([window], [input], [assets], [system], and [ime]).
 * Keeps UI and rendering layers 100% agnostic of specific game engines or native OS backends.
 *
 * @property window Sub-port managing window sizing, resize listeners, and system cursor icons.
 * @property input Sub-port managing pointer coordinates, modifier keys, and Arc input processors.
 * @property assets Sub-port resolving textures, fonts, byte buffers, and shader texts.
 * @property system Sub-port managing monotonic frame ticks, clipboard, data paths, and main-thread dispatches.
 * @property ime Sub-port managing native on-screen/SDL Input Method Editor composition sessions.
 *
 * @see MindustryPlatformHost
 * @see WindowPort
 * @see InputPort
 * @see AssetPort
 * @see SystemPort
 * @see ImePort
 */
interface PlatformHost {

    // --- DOMAIN PORTS ---

    val window: WindowPort
    val input: InputPort
    val assets: AssetPort
    val system: SystemPort
    val ime: ImePort

    // --- CONVENIENCE FACADE DELEGATIONS ---

    val screenWidth: Float get() = window.width
    val screenHeight: Float get() = window.height
    fun onResize(block: (width: Float, height: Float) -> Unit) = window.onResize(block)
    fun removeResize(block: (width: Float, height: Float) -> Unit) = window.removeResize(block)
    fun setCursorHand() = window.setCursorHand()
    fun setCursor(cursor: Cursor?) = window.setCursor(cursor)
    fun restoreCursor() = window.restoreCursor()

    val mouseX: Float get() = input.mouseX
    val mouseY: Float get() = input.mouseY
    val isCtrlPressed: Boolean get() = input.isCtrlPressed
    val isShiftPressed: Boolean get() = input.isShiftPressed
    val isAltPressed: Boolean get() = input.isAltPressed
    fun addInputProcessor(processor: InputProcessor) = input.addInputProcessor(processor)
    fun removeInputProcessor(processor: InputProcessor) = input.removeInputProcessor(processor)

    fun resolveAtlasRegion(name: String): TextureRegion? = assets.resolveAtlasRegion(name)
    fun resolveDefaultFont(): Font? = assets.resolveDefaultFont()
    fun resolveAssetBytes(path: String): ByteArray? = assets.resolveAssetBytes(path)
    fun resolveAssetString(path: String): String? = assets.resolveAssetString(path)
    fun readShaderSource(path: String): String = assets.readShaderSource(path)
    fun resolveFallbackRegion(): TextureRegion = assets.resolveFallbackRegion()
    fun resolveWhiteRegion(): TextureRegion = assets.resolveWhiteRegion()

    val frameId: Long get() = system.frameId
    val deltaTime: Float get() = system.deltaTime
    fun nowMillis(): Long = system.nowMillis()
    fun postToMainThread(block: () -> Unit) = system.postToMainThread(block)
    fun onFrameEnd(block: () -> Unit) = system.onFrameEnd(block)
    fun removeFrameEnd(block: () -> Unit) = system.removeFrameEnd(block)
    fun resolveDefaultDataDir(appName: String): Path = system.resolveDefaultDataDir(appName)
    fun getClipboard(): String = system.getClipboard()
    fun setClipboard(text: String) = system.setClipboard(text)
    fun openURI(uri: String): Boolean = system.openURI(uri)

    fun startImeSession(
        globalX: Float,
        globalY: Float,
        width: Float,
        height: Float,
        initialText: String,
        cursorPosition: Int,
        onCompositionChanged: (composition: String) -> Unit,
        onCompositionCleared: () -> Unit
    ) = ime.startSession(globalX, globalY, width, height, initialText, cursorPosition, onCompositionChanged, onCompositionCleared)

    fun syncImeSession(
        globalX: Float,
        globalY: Float,
        width: Float,
        height: Float,
        text: String,
        cursorPosition: Int
    ) = ime.syncSession(globalX, globalY, width, height, text, cursorPosition)

    fun stopImeSession() = ime.stopSession()

    /** Stub [PlatformHost] implementation combining all [NoOp] sub-ports for testing. */
    object NoOp : PlatformHost {
        override val window: WindowPort get() = WindowPort.NoOp
        override val input: InputPort get() = InputPort.NoOp
        override val assets: AssetPort get() = AssetPort.NoOp
        override val system: SystemPort get() = SystemPort.NoOp
        override val ime: ImePort get() = ImePort.NoOp
    }
}

/**
 * ## LocalPlatformHost [CompositionLocal Bridge]
 *
 * CompositionLocal providing access to the current [PlatformHost].
 */
val LocalPlatformHost = staticCompositionLocalOf<PlatformHost> {
    error("No PlatformHost provided in the current CompositionLocal hierarchy.")
}
