package org.mdt.core.engine.image

import arc.graphics.g2d.TextureRegion
import okio.Path
import okio.Path.Companion.toPath
import org.mdt.core.common.RequestBuilder

/**
 * ## ImageSource
 *
 * Pure sealed algebraic data type representing an image data origin.
 * Decoupled from runtime texture caches and rendering engines.
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
         * Converts any supported object (String, Path, TextureRegion, ImageSource) into a typed [ImageSource].
         */
        fun of(source: Any?): ImageSource = when (source) {
            null -> Atlas("ohno")
            is ImageSource -> source
            is TextureRegion -> Region(source)
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

        fun url(url: String, configure: RequestBuilder.() -> Unit): Url = Url(url, configure)

        fun authUrl(url: String, bearerToken: String): Url =
            Url(url) { header("Authorization", "Bearer $bearerToken") }

        fun atlas(name: String): Atlas = Atlas(name)

        fun asset(path: String): Asset = Asset(path)

        fun file(path: Path): LocalFile = LocalFile(path)

        fun region(region: TextureRegion): Region = Region(region)
    }
}
