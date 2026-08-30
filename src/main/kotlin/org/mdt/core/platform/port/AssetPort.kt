package org.mdt.core.platform.port

import arc.graphics.g2d.Font
import arc.graphics.g2d.TextureRegion

/**
 * ## AssetPort [Asset & Resource Resolution Contract]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Defines the abstract contract for resolving sprite atlas regions, typography fonts, raw asset bytes, and shader sources.
 * - Includes a built-in [NoOp] stub for headless execution or unit testing.
 *
 * ### 2. Key Rules & Checklist
 * - [x] `readShaderSource` automatically strips UTF-8 BOM (`\uFEFF`) to prevent GLSL compiler errors.
 * - [x] `resolveFallbackRegion` and `resolveWhiteRegion` must always return a non-null [TextureRegion].
 *
 * ### 3. Related Files
 * - Implementation: `src/main/kotlin/org/mdt/core/platform/impl/MindustryAssetPort.kt`
 * - Platform Host: `src/main/kotlin/org/mdt/core/platform/PlatformHost.kt`
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
