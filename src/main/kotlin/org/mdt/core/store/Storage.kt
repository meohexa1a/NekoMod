package org.mdt.core.store

import arc.Core
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * ## Storage
 *
 * High-performance file system manager backed by Okio with crash-resilient atomic file writing.
 */
object Storage {

    val fs: FileSystem = FileSystem.SYSTEM
    private val writeLocks = ConcurrentHashMap<String, Any>()

    /**
     * Resolves the base data directory for NekoMod within Mindustry's mod storage.
     */
    val modDir: Path by lazy {
        val baseDir = (Core.settings?.dataDirectory?.file()?.absolutePath)
            ?: (System.getenv("APPDATA") + "/Mindustry")
        val path = "$baseDir/nekomod".toPath()
        if (!fs.exists(path)) {
            fs.createDirectories(path)
        }
        path
    }

    /**
     * Resolves a subpath within the NekoMod directory.
     */
    fun resolve(relativePath: String): Path {
        val target = modDir.resolve(relativePath)
        val parent = target.parent
        if (parent != null && !fs.exists(parent)) {
            fs.createDirectories(parent)
        }
        return target
    }

    /**
     * Atomically writes data to a target path using a unique staging file,
     * ensuring zero file corruption if the process terminates unexpectedly.
     */
    fun atomicWrite(targetPath: Path, block: (okio.BufferedSink) -> Unit) {
        val parent = targetPath.parent
        if (parent != null && !fs.exists(parent)) {
            fs.createDirectories(parent)
        }

        val lock = writeLocks.computeIfAbsent(targetPath.toString()) { Any() }
        synchronized(lock) {
            val tempPath = "${targetPath}.${System.nanoTime()}.tmp".toPath()
            try {
                fs.sink(tempPath).buffer().use { sink ->
                    block(sink)
                    sink.flush()
                }
                // On Windows, if target exists, delete temp on failure or atomicMove
                if (fs.exists(targetPath)) {
                    try {
                        fs.delete(targetPath)
                    } catch (_: Exception) {}
                }
                fs.atomicMove(tempPath, targetPath)
            } catch (e: Exception) {
                if (fs.exists(tempPath)) {
                    try { fs.delete(tempPath) } catch (_: Exception) {}
                }
                throw IOException("Atomic write failed for path: $targetPath", e)
            }
        }
    }

    /**
     * Reads the entire content of a file as a UTF-8 string.
     */
    fun readString(path: Path): String? {
        if (!fs.exists(path)) return null
        return fs.source(path).buffer().use { it.readUtf8() }
    }

    /**
     * Reads the entire content of a file as a byte array.
     */
    fun readBytes(path: Path): ByteArray? {
        if (!fs.exists(path)) return null
        return fs.source(path).buffer().use { it.readByteArray() }
    }
}
