package org.mdt.core.engine.storage

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.mdt.core.engine.EngineContext
import java.io.IOException

/**
 * ## StorageService
 *
 * Modular storage subsystem managing multiplatform directories, Okio [FileSystem],
 * atomic file persistence, and Kotlinx JSON serialization.
 *
 * ### Architectural Features:
 * 1. **Windows NTFS Lock Immunity (Rule 16):** Employs unique nano-timestamped temporary staging files
 *    with atomic filesystem moves to prevent Windows file lock conflicts (`process cannot access the file`).
 * 2. **Context-Bound Isolation:** Dynamically resolves data directories via [EngineContext.host].
 * 3. **Structured Caching:** Provides dedicated, auto-created persistent cache directory resolution.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
open class StorageService(
    val context: EngineContext,
    val fileSystem: FileSystem = FileSystem.SYSTEM,
    val rootDirOverride: Path? = null,
    val cacheDirOverride: Path? = null
) {
    /** Compact, lenient JSON configuration serializer. */
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
    }

    /** Pretty-printed JSON configuration serializer for human-readable files. */
    val prettyJson: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
        prettyPrint = true
    }

    /** Root directory of the active context / project. */
    val rootDir: Path by lazy {
        rootDirOverride ?: resolveDefaultModDir()
    }

    /** Dedicated persistent cache directory. */
    val cacheDir: Path by lazy {
        val directory = cacheDirOverride ?: (rootDir / "cache")
        if (!fileSystem.exists(directory)) {
            try {
                fileSystem.createDirectories(directory)
            } catch (_: IOException) {}
        }
        directory
    }

    // =========================================================================
    // I. Path Resolution
    // =========================================================================

    /**
     * Resolves a relative subpath against [rootDir].
     *
     * @param subpath Relative path string.
     * @return Absolute [Path] inside the root directory.
     */
    fun resolve(subpath: String): Path = rootDir / subpath

    /**
     * Resolves a relative subpath against [cacheDir], creating parent directories if missing.
     *
     * @param subpath Relative path string inside cache.
     * @return Absolute [Path] inside the cache directory.
     */
    fun resolveCache(subpath: String): Path {
        val targetPath = cacheDir / subpath
        val parentDirectory = targetPath.parent
        if (parentDirectory != null && !fileSystem.exists(parentDirectory)) {
            try {
                fileSystem.createDirectories(parentDirectory)
            } catch (_: IOException) {}
        }
        return targetPath
    }

    // =========================================================================
    // II. Atomic Write Operations (Write API)
    // =========================================================================

    /**
     * Executes an atomic write operation using unique nanosecond-staged files.
     * Complies with Rule 16 Windows NTFS file locking protection.
     *
     * @param target Destination file path.
     * @param writeAction Block writing data into the temporary [BufferedSink].
     */
    fun atomicWrite(target: Path, writeAction: (BufferedSink) -> Unit) {
        val parentDirectory = target.parent
        if (parentDirectory != null && !fileSystem.exists(parentDirectory)) {
            fileSystem.createDirectories(parentDirectory)
        }

        val stagingTempFile = (target.toString() + ".tmp." + System.nanoTime()).toPath()
        try {
            fileSystem.write(stagingTempFile, mustCreate = false) {
                writeAction(this)
                flush()
            }
            fileSystem.atomicMove(stagingTempFile, target)
        } catch (error: Throwable) {
            try {
                fileSystem.delete(stagingTempFile, mustExist = false)
            } catch (_: Throwable) {}
            throw error
        }
    }

    /**
     * Atomically writes byte array content to a file.
     *
     * @param path Destination file path.
     * @param bytes Raw byte array to write.
     */
    fun writeBytes(path: Path, bytes: ByteArray) = atomicWrite(path) { sink -> sink.write(bytes) }

    /**
     * Atomically writes UTF-8 string content to a file.
     *
     * @param path Destination file path.
     * @param content String content to write.
     */
    fun writeString(path: Path, content: String) = atomicWrite(path) { sink -> sink.writeUtf8(content) }

    /**
     * Serializes and atomically writes value [T] as JSON to a file.
     *
     * @param path Destination file path.
     * @param value Serializable data object.
     * @param pretty Whether to format with indentation.
     */
    inline fun <reified T> writeJson(path: Path, value: T, pretty: Boolean = false) {
        val serializer = if (pretty) prettyJson else json
        val serializedContent = serializer.encodeToString(value)
        writeString(path, serializedContent)
    }

    // =========================================================================
    // III. File Reading & Deserialization (Read API)
    // =========================================================================

    /**
     * Reads raw byte content from a file, or returns `null` if absent or unreadable.
     *
     * @param path Target file path.
     * @return Byte array, or `null` if the file does not exist.
     */
    fun readBytes(path: Path): ByteArray? {
        if (!fileSystem.exists(path)) return null
        return try {
            fileSystem.read(path) { readByteArray() }
        } catch (_: IOException) {
            null
        }
    }

    /**
     * Reads UTF-8 string content from a file, or returns `null` if absent or unreadable.
     *
     * @param path Target file path.
     * @return Decoded string, or `null` if the file does not exist.
     */
    fun readString(path: Path): String? {
        if (!fileSystem.exists(path)) return null
        return try {
            fileSystem.read(path) { readUtf8() }
        } catch (_: IOException) {
            null
        }
    }

    /**
     * Reads and deserializes a JSON payload from a file into type [T].
     *
     * @param path Target file path.
     * @param serializer Custom [Json] serializer (defaults to [json]).
     * @return Deserialized object, or `null` on failure.
     */
    inline fun <reified T> readJson(path: Path, serializer: Json = json): T? {
        val content = readString(path) ?: return null
        return try {
            serializer.decodeFromString<T>(content)
        } catch (_: Throwable) {
            null
        }
    }

    // =========================================================================
    // IV. File Metadata & State Queries
    // =========================================================================

    /**
     * Checks whether a file or directory exists on the filesystem.
     *
     * @param path Target path to verify.
     * @return `true` if the path exists.
     */
    fun exists(path: Path): Boolean = fileSystem.exists(path)

    /**
     * Deletes a file or directory from the filesystem.
     *
     * @param path Target path to delete.
     * @param mustExist Whether to throw if the target is absent.
     */
    fun delete(path: Path, mustExist: Boolean = false) {
        fileSystem.delete(path, mustExist = mustExist)
    }

    /**
     * Returns the size of a file in bytes, or 0 if absent.
     *
     * @param path Target file path.
     * @return File size in bytes.
     */
    fun size(path: Path): Long = fileSystem.metadataOrNull(path)?.size ?: 0L

    // =========================================================================
    // V. Internal Host Directory Resolution
    // =========================================================================

    private fun resolveDefaultModDir(): Path {
        val appName = context.name.ifEmpty { "default" }
        val hostDir = context.host.resolveDefaultDataDir(appName)
        if (!fileSystem.exists(hostDir)) {
            try {
                fileSystem.createDirectories(hostDir)
            } catch (_: IOException) {}
        }
        return hostDir
    }
}
