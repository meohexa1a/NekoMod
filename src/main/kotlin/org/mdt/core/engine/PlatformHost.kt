package org.mdt.core.engine

import arc.Core
import arc.Events
import arc.graphics.g2d.TextureRegion
import mindustry.Vars
import mindustry.game.EventType.Trigger
import okio.Path
import okio.Path.Companion.toPath

/**
 * ## PlatformHost
 *
 * Abstraction interface bridging platform-specific engine capabilities:
 * main thread dispatching, frame lifecycle hooks, texture atlas lookups, and asset resolution.
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

    /** Resolves the default persistent application data directory for the host environment. */
    fun resolveDefaultDataDir(appName: String): Path?
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

    override fun resolveDefaultDataDir(appName: String): Path? {
        try {
            val settingsDir = Core.settings?.dataDirectory
            if (settingsDir != null) {
                return settingsDir.child("mods/$appName").file().absolutePath.toPath()
            }
        } catch (_: Throwable) {}
        return null
    }
}
