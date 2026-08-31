// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform

import arc.Core
import arc.Events
import arc.graphics.g2d.Font
import arc.graphics.g2d.TextureRegion
import arc.util.Log
import arc.util.Time
import java.util.concurrent.CopyOnWriteArrayList
import mindustry.game.EventType.Trigger
import mindustry.ui.Fonts
import okio.Path
import okio.Path.Companion.toPath

/**
 * ## SystemPort
 *
 * Defines the abstract contract for frame timing, main thread dispatch, clipboard, URI opening, and data persistence paths.
 * Includes a built-in [NoOp] stub for headless execution or unit testing.
 *
 * @property frameId Monotonically increasing frame counter for frame-indexing and caching.
 * @property deltaTime Time elapsed between the previous frame and current frame in seconds.
 *
 * @see MindustrySystemPort
 * @see org.mdt.core.platform.PlatformHost
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



/**
 * ## MindustrySystemPort
 *
 * Implements [SystemPort] by bridging to Arc's `Core.app`, `Core.settings`, and `Events.run(Trigger.uiDrawEnd)`.
 *
 * @see SystemPort
 * @see org.mdt.core.platform.PlatformHost
 */
class MindustrySystemPort : SystemPort {

    override val frameId: Long
        get() = Core.graphics?.frameId ?: 0L

    override val deltaTime: Float
        get() = Core.graphics?.deltaTime ?: 0.0166667f

    override fun nowMillis(): Long {
        return Time.millis()
    }

    override fun postToMainThread(block: () -> Unit) {
        val app = Core.app
        when {
            app != null -> app.post(block)
            else -> block()
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

    override fun resolveDefaultDataDir(appName: String): Path {
        val settingsDir = Core.settings?.dataDirectory
        if (settingsDir != null) {
            return settingsDir.child("mods/$appName").file().absolutePath.toPath()
        }

        val userHome = System.getProperty("user.home") ?: "."
        return userHome.toPath() / ".$appName"
    }

    override fun getClipboard(): String {
        return Core.app?.clipboardText ?: ""
    }

    override fun setClipboard(text: String) {
        Core.app?.clipboardText = text
    }

    override fun openURI(uri: String): Boolean {
        return try {
            Core.app?.openURI(uri) ?: false
        } catch (uriError: Throwable) {
            Log.warn("[NekoMod] Failed to open external URI: $uri", uriError)
            false
        }
    }

    companion object {
        private val frameEndListeners by lazy {
            val list = CopyOnWriteArrayList<() -> Unit>()

            Events.run(Trigger.uiDrawEnd) {
                for (listener in list) {
                    listener()
                }
            }

            list
        }
    }
}



/**
 * ## AssetPort
 *
 * Defines the abstract contract for resolving sprite atlas regions, typography fonts, raw asset bytes, and shader sources.
 * Includes a built-in [NoOp] stub for headless execution or unit testing.
 *
 * @see MindustryAssetPort
 * @see org.mdt.core.platform.PlatformHost
 */
interface AssetPort {

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

    /** Stub [AssetPort] implementation for headless or testing environments. */
    object NoOp : AssetPort {
        private val dummyRegion by lazy { TextureRegion() }

        override fun resolveAtlasRegion(name: String): TextureRegion? = null
        override fun resolveDefaultFont(): Font? = null
        override fun resolveAssetBytes(path: String): ByteArray? = null
        override fun resolveAssetString(path: String): String? = null
        override fun readShaderSource(path: String): String = ""
        override fun resolveFallbackRegion(): TextureRegion = dummyRegion
        override fun resolveWhiteRegion(): TextureRegion = dummyRegion
    }
}



/**
 * ## MindustryAssetPort
 *
 * Implements [AssetPort] by resolving textures from `Core.atlas`, fonts from `mindustry.ui.Fonts`, and shader sources from classpath / mod tree.
 * Caches static UI texture regions via [lazy] to avoid repeated hashmap lookups per frame/draw call.
 *
 * @see AssetPort
 * @see org.mdt.core.platform.PlatformHost
 */
class MindustryAssetPort : AssetPort {

    private val cachedFallbackRegion by lazy {
        val atlas = Core.atlas ?: return@lazy TextureRegion()
        when {
            atlas.has("ohno") -> atlas.find("ohno")
            atlas.has("error") -> atlas.find("error")
            atlas.has("white") -> atlas.find("white")
            else -> atlas.white() ?: TextureRegion()
        }
    }

    private val cachedWhiteRegion by lazy {
        val atlas = Core.atlas ?: return@lazy cachedFallbackRegion
        atlas.white() ?: cachedFallbackRegion
    }

    override fun resolveAtlasRegion(name: String): TextureRegion? {
        val atlas = Core.atlas ?: return null
        return when {
            atlas.has(name) -> atlas.find(name)
            else -> null
        }
    }

    override fun resolveDefaultFont(): Font? {
        return Fonts.def
    }

    override fun resolveAssetBytes(path: String): ByteArray? {
        val normalizedPath = path.removePrefix("/")
        val stream = MindustryAssetPort::class.java.classLoader.getResourceAsStream(normalizedPath)
            ?: Thread.currentThread().contextClassLoader?.getResourceAsStream(normalizedPath)
        return stream?.use { it.readBytes() }
    }

    override fun resolveAssetString(path: String): String? {
        return resolveAssetBytes(path)?.decodeToString()
    }

    override fun readShaderSource(path: String): String {
        val raw = resolveAssetString(path)
        if (raw == null) {
            Log.err("[NekoMod] Shader resource not found: @", path)
            return ""
        }

        // Strip UTF-8 Byte Order Mark (BOM \uFEFF) which causes GLSL compiler syntax failures
        return raw.replace("\uFEFF", "").trim()
    }

    override fun resolveFallbackRegion(): TextureRegion = cachedFallbackRegion

    override fun resolveWhiteRegion(): TextureRegion = cachedWhiteRegion
}
