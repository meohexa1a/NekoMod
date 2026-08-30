// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform.port

import arc.graphics.g2d.Font
import arc.graphics.g2d.TextureRegion

/**
 * ## AssetPort
 *
 * Defines the abstract contract for resolving sprite atlas regions, typography fonts, raw asset bytes, and shader sources.
 * Includes a built-in [NoOp] stub for headless execution or unit testing.
 *
 * @see org.mdt.core.platform.impl.MindustryAssetPort
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
