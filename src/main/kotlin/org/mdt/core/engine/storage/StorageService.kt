package org.mdt.core.engine.storage

import kotlinx.serialization.ExperimentalSerializationApi
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
 * See: docs/core-subsystems/core_subsystems_en.md
 */
@OptIn(ExperimentalSerializationApi::class)
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

    /** Pretty-printed JSON configuration serializer. */
    val prettyJson: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    /** Root directory of the active context / mod project. */
    val rootDir: Path by lazy {
        rootDirOverride ?: resolveDefaultModDir()
    }

    /** Dedicated persistent cache directory. */
    val cacheDir: Path by lazy {
        val dir = cacheDirOverride ?: (rootDir / "cache")
        if (!fileSystem.exists(dir)) {
            try {
                fileSystem.createDirectories(dir)
            } catch (_: IOException) {}
        }
        dir
    }

    private fun resolveDefaultModDir(): Path {
        val name = context.name.ifEmpty { "default" }
        val hostDir = context.host.resolveDefaultDataDir(name)
        if (hostDir != null) {
            if (!fileSystem.exists(hostDir)) {
                try {
                    fileSystem.createDirectories(hostDir)
                } catch (_: IOException) {}
            }
            return hostDir
        }

        val userHome = System.getProperty("user.home") ?: "."
        val fallback = userHome.toPath() / ".$name"
        if (!fileSystem.exists(fallback)) {
            try {
                fileSystem.createDirectories(fallback)
            } catch (_: IOException) {}
        }
        return fallback
    }

    /** Resolves a relative subpath against [rootDir]. */
    fun resolve(subpath: String): Path = rootDir / subpath

    /** Resolves a relative subpath against [cacheDir], creating parent directories if missing. */
    fun resolveCache(subpath: String): Path {
        val path = cacheDir / subpath
        val parent = path.parent
        if (parent != null && !fileSystem.exists(parent)) {
            try {
                fileSystem.createDirectories(parent)
            } catch (_: IOException) {}
        }
        return path
    }

    /**
     * Executes an atomic write operation using unique nanosecond-staged files.
     * Complies with Rule 16 Windows NTFS file locking protection.
     */
    fun atomicWrite(target: Path, writeAction: (BufferedSink) -> Unit) {
        val parent = target.parent
        if (parent != null && !fileSystem.exists(parent)) {
            fileSystem.createDirectories(parent)
        }
        val temp = (target.toString() + ".tmp." + System.nanoTime()).toPath()
        try {
            fileSystem.write(temp, mustCreate = false) {
                writeAction(this)
                flush()
            }
            fileSystem.atomicMove(temp, target)
        } catch (e: Throwable) {
            try {
                fileSystem.delete(temp, mustExist = false)
            } catch (_: Throwable) {}
            throw e
        }
    }

    /** Reads raw byte content from a file, or returns `null` if absent. */
    fun readBytes(path: Path): ByteArray? {
        if (!fileSystem.exists(path)) return null
        return try {
            fileSystem.read(path) { readByteArray() }
        } catch (_: IOException) {
            null
        }
    }

    /** Atomically writes byte array content to a file. */
    fun writeBytes(path: Path, bytes: ByteArray) = atomicWrite(path) { it.write(bytes) }

    /** Reads UTF-8 string content from a file, or returns `null` if absent. */
    fun readString(path: Path): String? {
        if (!fileSystem.exists(path)) return null
        return try {
            fileSystem.read(path) { readUtf8() }
        } catch (_: IOException) {
            null
        }
    }

    /** Atomically writes UTF-8 string content to a file. */
    fun writeString(path: Path, content: String) = atomicWrite(path) { it.writeUtf8(content) }

    /** Reads and deserializes a JSON payload from a file into type [T]. */
    inline fun <reified T> readJson(path: Path, serializer: Json = json): T? {
        val content = readString(path) ?: return null
        return try {
            serializer.decodeFromString<T>(content)
        } catch (_: Throwable) {
            null
        }
    }

    /** Serializes and atomically writes value [T] as JSON to a file. */
    inline fun <reified T> writeJson(path: Path, value: T, pretty: Boolean = false) {
        val serializer = if (pretty) prettyJson else json
        val content = serializer.encodeToString(value)
        writeString(path, content)
    }
}
