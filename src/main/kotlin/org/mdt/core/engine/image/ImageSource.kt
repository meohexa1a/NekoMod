package org.mdt.core.engine.image

import arc.graphics.Texture
import arc.graphics.g2d.TextureRegion
import arc.scene.style.Drawable
import arc.scene.style.TextureRegionDrawable
import okio.Path
import okio.Path.Companion.toPath
import org.mdt.core.common.RequestBuilder

/**
 * ## ImageSource
 *
 * Pure sealed algebraic data type representing an image origin descriptor.
 * Decoupled from runtime texture caches, network engines, and GPU graphics pipelines.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
sealed class ImageSource {

    /** Remote network URL image with optional HTTP request builder. */
    data class Url(
        val url: String,
        val configureRequest: (RequestBuilder.() -> Unit)? = null
    ) : ImageSource()

    /** Mindustry Sprite Atlas texture region name (e.g. "router", "ohno", "error"). */
    data class Atlas(val name: String) : ImageSource()

    /** Classpath or internal asset file path (e.g. "sprites/icon.png"). */
    data class Asset(val path: String) : ImageSource()

    /** Local filesystem file path (Okio Path). */
    data class LocalFile(val path: Path) : ImageSource()

    /** Direct in-memory [TextureRegion]. */
    data class Region(val region: TextureRegion) : ImageSource()

    companion object {

        /**
         * Resolves any supported input object into a strongly-typed [ImageSource].
         *
         * Supported types:
         * - [ImageSource] -> Returns directly.
         * - [TextureRegion] -> Wraps in [Region].
         * - [TextureRegionDrawable] / [Drawable] -> Extracts [TextureRegion] and wraps in [Region].
         * - [Texture] -> Wraps in [Region].
         * - [Path] -> Wraps in [LocalFile].
         * - [String] -> Parses prefixes (`http://`, `https://`, `atlas:`, `asset:`, `file:`) or defaults to [Atlas].
         * - `null` or others -> Fallback [Atlas] ("ohno").
         *
         * @param source Raw input object or URI string.
         * @return Structured [ImageSource] descriptor.
         */
        fun from(source: Any?): ImageSource = when (source) {
            null -> Atlas("ohno")
            is ImageSource -> source
            is TextureRegion -> Region(source)
            is TextureRegionDrawable -> Region(source.region)
            is Drawable -> {
                val reg = (source as? TextureRegionDrawable)?.region
                if (reg != null) Region(reg) else Atlas("ohno")
            }
            is Texture -> Region(TextureRegion(source))
            is Path -> LocalFile(source)
            is String -> when {
                source.startsWith("http://") || source.startsWith("https://") -> Url(source)
                source.startsWith("atlas:") -> Atlas(source.removePrefix("atlas:"))
                source.startsWith("asset:") -> Asset(source.removePrefix("asset:"))
                source.startsWith("file:") -> LocalFile(source.removePrefix("file:").toPath())
                else -> Atlas(source)
            }
            else -> Atlas("ohno")
        }

        /** Alias for [from]. */
        fun of(source: Any?): ImageSource = from(source)

        /** Creates a remote network URL image source with custom request configuration. */
        fun url(url: String, configureRequest: (RequestBuilder.() -> Unit)? = null): Url =
            Url(url, configureRequest)

        /** Creates a remote network URL image source with Bearer authorization header. */
        fun bearerTokenUrl(url: String, bearerToken: String): Url =
            Url(url) { header("Authorization", "Bearer $bearerToken") }

        /** Creates an Atlas sprite image source. */
        fun atlas(spriteName: String): Atlas = Atlas(spriteName)

        /** Creates a classpath or internal asset image source. */
        fun asset(assetPath: String): Asset = Asset(assetPath)

        /** Creates a local filesystem image source. */
        fun file(filePath: Path): LocalFile = LocalFile(filePath)

        /** Creates an in-memory TextureRegion image source. */
        fun region(textureRegion: TextureRegion): Region = Region(textureRegion)
    }
}
