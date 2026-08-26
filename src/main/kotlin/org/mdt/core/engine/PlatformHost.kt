package org.mdt.core.engine

import arc.Core
import arc.Events
import arc.graphics.g2d.TextureRegion
import arc.input.InputProcessor
import mindustry.game.EventType.ResizeEvent
import mindustry.game.EventType.Trigger
import mindustry.Vars
import okio.Path
import okio.Path.Companion.toPath

/**
 * ## PlatformHost
 *
 * Abstraction interface bridging platform-specific engine capabilities:
 * main thread dispatching, frame lifecycle hooks, texture atlas lookups, asset resolution,
 * fallback error textures, screen viewport dimensions, and input processor chains.
 *
 * Enables NekoMod to run in Mindustry, Standalone UI Editor, GUI Launcher, or Headless Test environments.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
interface PlatformHost {

    /** Posts a callback to the platform's main / render thread. */
    fun postToMainThread(block: () -> Unit)

    /** Registers a hook executed at the end of every render frame. */
    fun onFrameEnd(block: () -> Unit)

    /** Resolves a named sprite region from the game's texture atlas. */
    fun resolveAtlasRegion(name: String): TextureRegion?

    /** Resolves the raw byte content of an internal or classpath asset. */
    fun resolveAssetBytes(path: String): ByteArray?

    /** Resolves the default placeholder/error [TextureRegion] when image loading fails. */
    fun resolveFallbackRegion(): TextureRegion

    /** Resolves the default persistent application data directory for the host environment. */
    fun resolveDefaultDataDir(appName: String): Path

    /** Current display surface width in pixels. */
    val screenWidth: Float

    /** Current display surface height in pixels. */
    val screenHeight: Float

    /** Registers a listener invoked when the display surface is resized. */
    fun onResize(block: (width: Float, height: Float) -> Unit)

    /** Adds an input processor to the top of the input dispatch chain. */
    fun addInputProcessor(processor: InputProcessor)

    /** Removes an input processor from the input dispatch chain. */
    fun removeInputProcessor(processor: InputProcessor)
}

/**
 * Default [PlatformHost] implementation connecting to Mindustry / Arc runtime singletons.
 */
open class MindustryPlatformHost : PlatformHost {

    override fun postToMainThread(block: () -> Unit) {
        if (Core.app != null) Core.app.post(block) else block()
    }

    override fun onFrameEnd(block: () -> Unit) {
        try {
            Events.run(Trigger.uiDrawEnd, block)
        } catch (_: Throwable) {
            // Standalone or headless environment without Mindustry event loop
        }
    }

    override fun resolveAtlasRegion(name: String): TextureRegion? {
        val atlas = Core.atlas ?: return null
        return if (atlas.has(name)) atlas.find(name) else null
    }

    override fun resolveAssetBytes(path: String): ByteArray? {
        val treeFi = Vars.tree?.get(path)
        if (treeFi != null && treeFi.exists()) return treeFi.readBytes()

        val internalFi = Core.files?.internal(path)
        if (internalFi != null && internalFi.exists()) return internalFi.readBytes()

        val stream = MindustryPlatformHost::class.java.classLoader.getResourceAsStream(path)
        return stream?.use { it.readBytes() }
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

    override val screenWidth: Float
        get() = Core.graphics?.width?.toFloat() ?: 0f

    override val screenHeight: Float
        get() = Core.graphics?.height?.toFloat() ?: 0f

    override fun onResize(block: (width: Float, height: Float) -> Unit) {
        try {
            Events.on(ResizeEvent::class.java) {
                block(screenWidth, screenHeight)
            }
        } catch (_: Throwable) {}
    }

    override fun addInputProcessor(processor: InputProcessor) {
        try {
            val processors = Core.input?.inputProcessors
            if (processors != null && !processors.contains(processor)) {
                processors.insert(0, processor)
            }
        } catch (_: Throwable) {}
    }

    override fun removeInputProcessor(processor: InputProcessor) {
        try {
            Core.input?.removeProcessor(processor)
        } catch (_: Throwable) {}
    }
}
