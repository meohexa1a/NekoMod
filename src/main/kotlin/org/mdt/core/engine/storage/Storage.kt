package org.mdt.core.engine.storage

import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import org.mdt.core.engine.EngineContext

/**
 * ## Storage
 *
 * High-performance, cross-platform file system facade delegating to [EngineContext.current.storage].
 * Provides atomic file persistence, streaming JSON serialization, and safe directory resolution.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
object Storage {

    /** Active storage service instance bound to the current execution context. */
    val service: StorageService
        inline get() = EngineContext.current.storage

    /** Underlying Okio [FileSystem]. */
    val fs: FileSystem
        inline get() = service.fileSystem

    /** Standard compact JSON serializer. */
    val json: Json
        inline get() = service.json

    /** Pretty-printed JSON serializer for human-readable configuration files. */
    val prettyJson: Json
        inline get() = service.prettyJson

    /** Root directory of the active context / project. */
    val rootDir: Path
        inline get() = service.rootDir

    /** Backward-compatible alias for [rootDir]. */
    val modDir: Path
        inline get() = service.rootDir

    /** Dedicated persistent cache directory. */
    val cacheDir: Path
        inline get() = service.cacheDir

    /** Resolves a relative subpath against [rootDir]. */
    fun resolve(relativePath: String): Path = service.resolve(relativePath)

    /** Resolves a relative subpath against [cacheDir]. */
    fun resolveCache(relativePath: String): Path = service.resolveCache(relativePath)

    /** Executes an atomic write operation using unique staging files. */
    fun atomicWrite(targetPath: Path, block: (BufferedSink) -> Unit) = service.atomicWrite(targetPath, block)

    /** Checks whether a path exists. */
    fun exists(path: Path): Boolean = service.fileSystem.exists(path)

    /** Deletes a file or directory. */
    fun delete(path: Path, mustExist: Boolean = false) {
        service.fileSystem.delete(path, mustExist = mustExist)
    }

    /** Returns file size in bytes, or 0 if absent. */
    fun size(path: Path): Long = service.fileSystem.metadataOrNull(path)?.size ?: 0L

    /** Reads UTF-8 string from a file, or returns `null` if absent. */
    fun readString(path: Path): String? = service.readString(path)

    /** Reads raw byte array from a file, or returns `null` if absent. */
    fun readBytes(path: Path): ByteArray? = service.readBytes(path)

    /** Atomically writes UTF-8 string content to a file. */
    fun writeString(path: Path, content: String) = service.writeString(path, content)

    /** Atomically writes byte array content to a file. */
    fun writeBytes(path: Path, bytes: ByteArray) = service.writeBytes(path, bytes)

    /** Reads and deserializes a JSON payload into type [T]. */
    inline fun <reified T> readJson(path: Path, serializer: Json = json): T? =
        service.readJson<T>(path, serializer)

    /** Serializes and atomically writes value [T] as JSON. */
    inline fun <reified T> writeJson(path: Path, value: T, pretty: Boolean = false) =
        service.writeJson<T>(path, value, pretty)
}
