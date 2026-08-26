package org.mdt.core.store

import arc.Core
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import java.io.IOException

/**
 * ## Storage
 *
 * High-performance, cross-platform file system facade powered by Okio and Kotlinx Serialization.
 * Provides atomic file writing, streaming JSON serialization, and safe multiplatform directory resolution.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
object Storage {

    val fs: FileSystem = FileSystem.SYSTEM

    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        prettyPrint = false
    }

    val prettyJson: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        prettyPrint = true
    }

    /**
     * Base storage directory for NekoMod with cross-platform fallback (Windows, Linux, macOS, Android).
     */
    val modDir: Path by lazy {
        val base = Core.settings?.dataDirectory?.file()?.absolutePath
            ?: (System.getProperty("user.home") + "/.nekomod")
        "$base/nekomod".toPath().also { fs.createDirectories(it) }
    }

    /**
     * Dedicated persistent disk cache directory for images, schemas, and temporary assets.
     */
    val cacheDir: Path by lazy {
        modDir.resolve("cache").also { fs.createDirectories(it) }
    }

    /** Resolves a relative subpath within [modDir], auto-creating parent directories. */
    fun resolve(relativePath: String): Path =
        modDir.resolve(relativePath).also { it.parent?.let(fs::createDirectories) }

    /** Resolves a relative subpath within [cacheDir], auto-creating parent directories. */
    fun resolveCache(relativePath: String): Path =
        cacheDir.resolve(relativePath).also { it.parent?.let(fs::createDirectories) }

    /**
     * Atomically writes data to [targetPath] using a temporary staging file and [FileSystem.atomicMove].
     */
    fun atomicWrite(targetPath: Path, block: (BufferedSink) -> Unit) {
        val parent = targetPath.parent
        if (parent != null && !fs.exists(parent)) fs.createDirectories(parent)

        val tempPath = "${targetPath}.${System.nanoTime()}.tmp".toPath()
        try {
            fs.write(tempPath) { block(this) }
            fs.atomicMove(tempPath, targetPath)
        } catch (e: Exception) {
            fs.delete(tempPath, mustExist = false)
            throw IOException("Atomic write failed for path: $targetPath", e)
        }
    }

    // --- PRIMITIVE I/O ---

    fun exists(path: Path): Boolean = fs.exists(path)

    fun delete(path: Path, mustExist: Boolean = false): Unit = fs.delete(path, mustExist = mustExist)

    fun size(path: Path): Long = fs.metadataOrNull(path)?.size ?: 0L

    fun readString(path: Path): String? = if (fs.exists(path)) fs.read(path) { readUtf8() } else null

    fun readBytes(path: Path): ByteArray? = if (fs.exists(path)) fs.read(path) { readByteArray() } else null

    fun writeString(path: Path, content: String) = atomicWrite(path) { it.writeUtf8(content) }

    fun writeBytes(path: Path, bytes: ByteArray) = atomicWrite(path) { it.write(bytes) }

    // --- STRUCTURED JSON SERIALIZATION ---

    inline fun <reified T> readJson(path: Path, serializer: Json = json): T? {
        val content = readString(path) ?: return null
        return try {
            serializer.decodeFromString<T>(content)
        } catch (_: Exception) {
            null
        }
    }

    inline fun <reified T> writeJson(path: Path, value: T, pretty: Boolean = false) {
        val serializer = if (pretty) prettyJson else json
        val content = serializer.encodeToString(value)
        writeString(path, content)
    }
}
