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
 * Supports rich CDN icon prefixes: `lucide:`, `sf:`, `apple:`, `fluent:`, `material:`,
 * `tabler:`, `ph:`, `remix:`, `icons8:`, `cdn:`, `jsdelivr:`, `unpkg:`, and `github:`.
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
         * - [String] -> Parses prefixes or defaults to [Atlas].
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

                // -------------------------------------------------------------
                // CDN Icon Resolvers
                // -------------------------------------------------------------
                source.startsWith("lucide:") ->
                    Url("https://img.icons8.com/ios-glyphs/64/ffffff/${source.removePrefix("lucide:")}.png")

                source.startsWith("sf:") || source.startsWith("apple:") -> {
                    val name = if (source.startsWith("sf:")) source.removePrefix("sf:") else source.removePrefix("apple:")
                    Url("https://img.icons8.com/sf-regular-filled/64/ffffff/$name.png")
                }

                source.startsWith("fluent:") ->
                    Url("https://img.icons8.com/fluency-systems-filled/64/ffffff/${source.removePrefix("fluent:")}.png")

                source.startsWith("material:") ->
                    Url("https://img.icons8.com/material-rounded/64/ffffff/${source.removePrefix("material:")}.png")

                source.startsWith("tabler:") ->
                    Url("https://img.icons8.com/ios-filled/64/ffffff/${source.removePrefix("tabler:")}.png")

                source.startsWith("ph:") || source.startsWith("phosphor:") -> {
                    val name = if (source.startsWith("ph:")) source.removePrefix("ph:") else source.removePrefix("phosphor:")
                    Url("https://img.icons8.com/material-outlined/64/ffffff/$name.png")
                }

                source.startsWith("remix:") || source.startsWith("ri:") -> {
                    val name = if (source.startsWith("remix:")) source.removePrefix("remix:") else source.removePrefix("ri:")
                    Url("https://img.icons8.com/material-two-tone/64/ffffff/$name.png")
                }

                source.startsWith("icons8:") -> {
                    val parts = source.removePrefix("icons8:").split(":", limit = 2)
                    if (parts.size == 2) {
                        Url("https://img.icons8.com/${parts[0]}/64/ffffff/${parts[1]}.png")
                    } else {
                        Url("https://img.icons8.com/material-rounded/64/ffffff/${parts[0]}.png")
                    }
                }

                source.startsWith("cdn:") || source.startsWith("jsdelivr:") -> {
                    val path = if (source.startsWith("cdn:")) source.removePrefix("cdn:") else source.removePrefix("jsdelivr:")
                    Url("https://cdn.jsdelivr.net/$path")
                }

                source.startsWith("unpkg:") ->
                    Url("https://unpkg.com/${source.removePrefix("unpkg:")}")

                source.startsWith("github:") ->
                    Url("https://raw.githubusercontent.com/${source.removePrefix("github:")}")

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
