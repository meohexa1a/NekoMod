// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform.impl

import arc.Core
import arc.graphics.g2d.Font
import arc.graphics.g2d.TextureRegion
import mindustry.Vars
import mindustry.ui.Fonts
import org.mdt.core.platform.port.AssetPort

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
        val treeFile = Vars.tree?.get(path)
        if (treeFile != null && treeFile.exists()) return treeFile.readBytes()

        val internalFile = Core.files?.internal(path)
        if (internalFile != null && internalFile.exists()) return internalFile.readBytes()

        val stream = MindustryAssetPort::class.java.classLoader.getResourceAsStream(path)
        return stream?.use { it.readBytes() }
    }

    override fun resolveAssetString(path: String): String? {
        return resolveAssetBytes(path)?.decodeToString()
    }

    override fun readShaderSource(path: String): String {
        val raw = resolveAssetString(path) ?: throw IllegalStateException("Shader resource not found: $path")

        // Strip UTF-8 Byte Order Mark (BOM \uFEFF) which causes GLSL compiler syntax failures
        return raw.replace("\uFEFF", "").trim()
    }

    override fun resolveFallbackRegion(): TextureRegion = cachedFallbackRegion

    override fun resolveWhiteRegion(): TextureRegion = cachedWhiteRegion
}
