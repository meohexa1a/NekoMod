package org.mdt.core.engine

import androidx.compose.runtime.staticCompositionLocalOf
import arc.Core
import arc.Events
import arc.Graphics.Cursor
import arc.graphics.g2d.Font
import arc.graphics.g2d.TextureRegion
import arc.input.InputMultiplexer
import arc.input.InputProcessor
import arc.util.Time
import java.util.concurrent.CopyOnWriteArrayList
import mindustry.Vars
import mindustry.game.EventType.ResizeEvent
import mindustry.game.EventType.Trigger
import mindustry.ui.Fonts
import okio.Path
import okio.Path.Companion.toPath

/**
 * ## PlatformHost
 *
 * Abstraction interface bridging platform-specific engine capabilities:
 * main thread dispatching, frame lifecycle hooks, texture atlas lookups, asset resolution,
 * typography font resolution, shader source loading, screen viewport dimensions,
 * input processor chains, cursor management, clipboard, and OS integrations.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
interface PlatformHost {

    /** Posts a callback to the platform's main / render thread. */
    fun postToMainThread(block: () -> Unit)

    /** Registers a hook executed at the end of every render frame. */
    fun onFrameEnd(block: () -> Unit)

    /** Removes a hook executed at the end of every render frame. */
    fun removeFrameEnd(block: () -> Unit)

    /** Monotonically increasing frame counter for frame-indexing and caching. */
    val frameId: Long

    /** Time elapsed between the previous frame and the current frame in seconds. */
    val deltaTime: Float

    /** Current timestamp in milliseconds. */
    fun nowMillis(): Long

    /** Resolves a named sprite region from the game's texture atlas. */
    fun resolveAtlasRegion(name: String): TextureRegion?

    /** Resolves the platform's default typography BMFont. */
    fun resolveDefaultFont(): Font?

    /** Resolves the raw byte content of an internal or classpath asset. */
    fun resolveAssetBytes(path: String): ByteArray?

    /** Resolves the raw text content of an internal or classpath asset. */
    fun resolveAssetString(path: String): String?

    /** Resolves the raw GLSL shader source string from classpath, mod tree, or internal files. */
    fun readShaderSource(path: String): String

    /** Resolves the default placeholder/error [TextureRegion] when image loading fails. */
    fun resolveFallbackRegion(): TextureRegion

    /** Resolves the default solid white [TextureRegion] used for solid quads and shapes. */
    fun resolveWhiteRegion(): TextureRegion

    /** Resolves the default persistent application data directory for the host environment. */
    fun resolveDefaultDataDir(appName: String): Path

    /** Current display surface width in pixels. */
    val screenWidth: Float

    /** Current display surface height in pixels. */
    val screenHeight: Float

    /** Current pointer X position in screen coordinates. */
    val mouseX: Float

    /** Current pointer Y position in screen coordinates. */
    val mouseY: Float

    /** Sets the system cursor to a hand/pointer cursor for clickable elements. */
    fun setCursorHand()

    /** Sets the active hardware/software cursor. */
    fun setCursor(cursor: Cursor?)

    /** Restores the hardware/software cursor to default. */
    fun restoreCursor()

    /** Registers a listener invoked when the display surface is resized. */
    fun onResize(block: (width: Float, height: Float) -> Unit)

    /** Removes a listener invoked when the display surface is resized. */
    fun removeResize(block: (width: Float, height: Float) -> Unit)

    /** Adds an input processor to the top of the input dispatch chain. */
    fun addInputProcessor(processor: InputProcessor)

    /** Removes an input processor from the input dispatch chain. */
    fun removeInputProcessor(processor: InputProcessor)

    /** Retrieves the current system clipboard text content. */
    fun getClipboard(): String

    /** Sets the system clipboard text content. */
    fun setClipboard(text: String)

    /** Opens a URI in the default external system web browser or application. */
    fun openURI(uri: String): Boolean
}

/**
 * ## LocalPlatformHost
 *
 * CompositionLocal providing access to the current [PlatformHost].
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
val LocalPlatformHost = staticCompositionLocalOf<PlatformHost> {
    error("No PlatformHost provided in the current CompositionLocal hierarchy.")
}

/**
 * ## MindustryPlatformHost
 *
 * Default [PlatformHost] implementation connecting to Mindustry / Arc runtime singletons.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
open class MindustryPlatformHost : PlatformHost {

    // --- LIFECYCLE & DISPATCH ---

    override fun postToMainThread(block: () -> Unit) {
        if (Core.app != null) {
            Core.app.post(block)
        } else {
            block()
        }
    }

    override fun onFrameEnd(block: () -> Unit) {
        if (!frameEndListeners.contains(block)) {
            frameEndListeners.add(block)
        }
    }

    override fun removeFrameEnd(block: () -> Unit) {
        frameEndListeners.remove(block)
    }

    override val frameId: Long
        get() = Core.graphics?.frameId ?: 0L

    override val deltaTime: Float
        get() = Core.graphics?.deltaTime ?: 0.0166667f

    override fun nowMillis(): Long {
        return Time.millis()
    }

    // --- ASSET & ATLAS RESOLUTION ---

    override fun resolveAtlasRegion(name: String): TextureRegion? {
        val atlas = Core.atlas ?: return null

        return if (atlas.has(name)) atlas.find(name) else null
    }

    override fun resolveDefaultFont(): Font? {
        return Fonts.def
    }

    override fun resolveAssetBytes(path: String): ByteArray? {
        val treeFile = Vars.tree?.get(path)
        if (treeFile != null && treeFile.exists()) return treeFile.readBytes()

        val internalFile = Core.files?.internal(path)
        if (internalFile != null && internalFile.exists()) return internalFile.readBytes()

        val stream = MindustryPlatformHost::class.java.classLoader.getResourceAsStream(path)
        return stream?.use { it.readBytes() }
    }

    override fun resolveAssetString(path: String): String? {
        return resolveAssetBytes(path)?.decodeToString()
    }

    override fun readShaderSource(path: String): String {
        val raw = MindustryPlatformHost::class.java.classLoader.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
            ?: (if (Vars.tree?.get(path)?.exists() == true) Vars.tree?.get(path)?.readString() else null)
            ?: (if (Core.files?.internal(path)?.exists() == true) Core.files?.internal(path)?.readString() else null)
            ?: throw IllegalStateException("Shader resource not found: $path")

        // Strip UTF-8 Byte Order Mark (BOM \uFEFF) which causes GLSL compiler syntax failures
        return raw.replace("\uFEFF", "").trim()
    }

    override fun resolveFallbackRegion(): TextureRegion {
        val atlas = Core.atlas ?: return TextureRegion()

        return when {
            atlas.has("ohno") -> atlas.find("ohno")
            atlas.has("error") -> atlas.find("error")
            atlas.has("white") -> atlas.find("white")
            else -> atlas.white() ?: TextureRegion()
        }
    }

    override fun resolveWhiteRegion(): TextureRegion {
        return Core.atlas?.white() ?: resolveFallbackRegion()
    }

    // --- STORAGE & PERSISTENCE ---

    override fun resolveDefaultDataDir(appName: String): Path {
        try {
            val settingsDir = Core.settings?.dataDirectory
            if (settingsDir != null) {
                return settingsDir.child("mods/$appName").file().absolutePath.toPath()
            }
        } catch (_: Throwable) {}

        val userHome = System.getProperty("user.home") ?: "."
        return userHome.toPath() / ".$appName"
    }

    // --- DISPLAY & VIEWPORT ---

    override val screenWidth: Float
        get() = Core.graphics?.width?.toFloat() ?: 0.0f

    override val screenHeight: Float
        get() = Core.graphics?.height?.toFloat() ?: 0.0f

    override fun onResize(block: (width: Float, height: Float) -> Unit) {
        if (!resizeListeners.contains(block)) {
            resizeListeners.add(block)
        }
    }

    override fun removeResize(block: (width: Float, height: Float) -> Unit) {
        resizeListeners.remove(block)
    }

    // --- INPUT & CURSOR SUBSYSTEM ---

    override val mouseX: Float
        get() = Core.input?.mouseX()?.toFloat() ?: 0.0f

    override val mouseY: Float
        get() = Core.input?.mouseY()?.toFloat() ?: 0.0f

    override fun setCursorHand() {
        try {
            Core.graphics?.cursor(Cursor.SystemCursor.hand)
        } catch (_: Throwable) {}
    }

    override fun setCursor(cursor: Cursor?) {
        try {
            if (cursor != null) {
                Core.graphics?.cursor(cursor)
            } else {
                Core.graphics?.restoreCursor()
            }
        } catch (_: Throwable) {}
    }

    override fun restoreCursor() {
        try {
            Core.graphics?.restoreCursor()
        } catch (_: Throwable) {}
    }

    override fun addInputProcessor(processor: InputProcessor) {
        try {
            val inputProcessors = Core.input?.inputProcessors
            if (inputProcessors != null && !inputProcessors.contains(localMultiplexer)) {
                inputProcessors.insert(0, localMultiplexer)
            }
            if (!localMultiplexer.processors.contains(processor)) {
                localMultiplexer.addProcessor(0, processor)
            }
        } catch (_: Throwable) {}
    }

    override fun removeInputProcessor(processor: InputProcessor) {
        try {
            localMultiplexer.removeProcessor(processor)
        } catch (_: Throwable) {}
    }

    // --- OS, CLIPBOARD & SYSTEM ---

    override fun getClipboard(): String {
        return try {
            Core.app?.clipboardText ?: ""
        } catch (_: Throwable) {
            ""
        }
    }

    override fun setClipboard(text: String) {
        try {
            Core.app?.clipboardText = text
        } catch (_: Throwable) {}
    }

    override fun openURI(uri: String): Boolean {
        return try {
            Core.app?.openURI(uri) ?: false
        } catch (_: Throwable) {
            false
        }
    }

    // --- COMPANION OBJECT ---

    companion object {
        private val localMultiplexer by lazy {
            InputMultiplexer().also { multiplexer ->
                try {
                    val inputProcessors = Core.input?.inputProcessors
                    if (inputProcessors != null && !inputProcessors.contains(multiplexer)) {
                        inputProcessors.insert(0, multiplexer)
                    }
                } catch (_: Throwable) {}
            }
        }

        private val frameEndListeners by lazy {
            CopyOnWriteArrayList<() -> Unit>().also { listeners ->
                try {
                    Events.run(Trigger.uiDrawEnd) {
                        for (listener in listeners) {
                            try {
                                listener()
                            } catch (_: Throwable) {}
                        }
                    }
                } catch (_: Throwable) {}
            }
        }

        private val resizeListeners by lazy {
            CopyOnWriteArrayList<(width: Float, height: Float) -> Unit>().also { listeners ->
                try {
                    Events.on(ResizeEvent::class.java) {
                        val currentWidth = Core.graphics?.width?.toFloat() ?: 0.0f
                        val currentHeight = Core.graphics?.height?.toFloat() ?: 0.0f
                        for (listener in listeners) {
                            try {
                                listener(currentWidth, currentHeight)
                            } catch (_: Throwable) {}
                        }
                    }
                } catch (_: Throwable) {}
            }
        }
    }
}
