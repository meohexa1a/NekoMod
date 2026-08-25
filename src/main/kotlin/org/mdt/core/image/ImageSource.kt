package org.mdt.core.image

import arc.Core
import arc.graphics.g2d.TextureRegion
import okio.Path
import org.mdt.core.net.RequestBuilder

/**
 * ## ImageSource
 *
 * Represents an image data origin (Remote URL, local file, classpath asset, or in-memory TextureRegion).
 * Supports HTTP request templates and graceful fallback to Mindustry's iconic 'ohno' / 'error' texture.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
sealed class ImageSource {

    /**
     * Remote URL image source with optional HTTP request builder template.
     */
    data class Url(
        val url: String,
        val configureRequest: (RequestBuilder.() -> Unit)? = null
    ) : ImageSource()

    data class Asset(val path: String) : ImageSource()
    data class LocalFile(val path: Path) : ImageSource()
    data class Region(val region: TextureRegion) : ImageSource()

    companion object {

        /**
         * Returns Mindustry's iconic 'ohno' error fallback texture region.
         */
        fun fallbackRegion(): TextureRegion {
            val atlas = Core.atlas ?: return TextureRegion()
            return when {
                atlas.has("ohno") -> atlas.find("ohno")
                atlas.has("error") -> atlas.find("error")
                else -> atlas.error() ?: atlas.white() ?: TextureRegion()
            }
        }

        /**
         * Safely converts any object into an [ImageSource], falling back to 'ohno' instead of throwing.
         */
        fun of(source: Any?): ImageSource = when (source) {
            null -> Region(fallbackRegion())
            is ImageSource -> source
            is TextureRegion -> Region(source)
            is Path -> LocalFile(source)
            is String -> when {
                source.startsWith("http://") || source.startsWith("https://") -> Url(source)
                source.startsWith("atlas:") -> {
                    val name = source.removePrefix("atlas:")
                    val reg = if (Core.atlas != null && Core.atlas.has(name)) Core.atlas.find(name) else fallbackRegion()
                    Region(reg)
                }
                Core.atlas != null && Core.atlas.has(source) -> Region(Core.atlas.find(source))
                else -> Asset(source)
            }
            else -> Region(fallbackRegion())
        }

        fun url(url: String, configure: RequestBuilder.() -> Unit): Url =
            Url(url, configure)

        fun authUrl(url: String, bearerToken: String): Url =
            Url(url) { header("Authorization", "Bearer $bearerToken") }
    }
}
