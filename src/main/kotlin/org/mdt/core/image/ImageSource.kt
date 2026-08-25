package org.mdt.core.image

import arc.Core
import arc.graphics.g2d.TextureRegion
import okio.Path

/**
 * ## ImageSource
 *
 * Represents an image data origin (Remote URL, local file, classpath asset, or in-memory TextureRegion).
 */
sealed class ImageSource {
    data class Url(val url: String) : ImageSource()
    data class Asset(val path: String) : ImageSource()
    data class LocalFile(val path: Path) : ImageSource()
    data class Region(val region: TextureRegion) : ImageSource()

    companion object {
        fun of(source: Any): ImageSource = when (source) {
            is ImageSource -> source
            is TextureRegion -> Region(source)
            is Path -> LocalFile(source)
            is String -> when {
                source.startsWith("http://") || source.startsWith("https://") -> Url(source)
                source.startsWith("atlas:") -> {
                    val name = source.removePrefix("atlas:")
                    val reg = Core.atlas?.find(name) ?: Core.atlas?.white() ?: TextureRegion()
                    Region(reg)
                }
                Core.atlas != null && Core.atlas.has(source) -> Region(Core.atlas.find(source))
                else -> Asset(source)
            }
            else -> throw IllegalArgumentException("Unsupported image source: $source")
        }
    }
}
