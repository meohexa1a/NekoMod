// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform.impl

import arc.Core
import arc.Events
import arc.util.Log
import arc.util.Time
import java.util.concurrent.CopyOnWriteArrayList
import mindustry.game.EventType.Trigger
import okio.Path
import okio.Path.Companion.toPath
import org.mdt.core.platform.port.SystemPort

/**
 * ## MindustrySystemPort
 *
 * Implements [SystemPort] by bridging to Arc's `Core.app`, `Core.settings`, and `Events.run(Trigger.uiDrawEnd)`.
 *
 * @see SystemPort
 * @see org.mdt.core.platform.PlatformHost
 */
class MindustrySystemPort : SystemPort {

    override val frameId: Long
        get() = Core.graphics?.frameId ?: 0L

    override val deltaTime: Float
        get() = Core.graphics?.deltaTime ?: 0.0166667f

    override fun nowMillis(): Long {
        return Time.millis()
    }

    override fun postToMainThread(block: () -> Unit) {
        val app = Core.app
        when {
            app != null -> app.post(block)
            else -> block()
        }
    }

    override fun onFrameEnd(block: () -> Unit) {
        if (!frameEndListeners.contains(block)) {
            frameEndListeners.add(block)
        }
    }

    override fun removeFrameEnd(block: () -> Unit) {
        frameEndListeners.remove(block)
    }

    override fun resolveDefaultDataDir(appName: String): Path {
        val settingsDir = Core.settings?.dataDirectory
        if (settingsDir != null) {
            return settingsDir.child("mods/$appName").file().absolutePath.toPath()
        }

        val userHome = System.getProperty("user.home") ?: "."
        return userHome.toPath() / ".$appName"
    }

    override fun getClipboard(): String {
        return Core.app?.clipboardText ?: ""
    }

    override fun setClipboard(text: String) {
        Core.app?.clipboardText = text
    }

    override fun openURI(uri: String): Boolean {
        return try {
            Core.app?.openURI(uri) ?: false
        } catch (uriError: Throwable) {
            Log.warn("[NekoMod] Failed to open external URI: $uri", uriError)
            false
        }
    }

    companion object {
        private val frameEndListeners by lazy {
            val list = CopyOnWriteArrayList<() -> Unit>()

            Events.run(Trigger.uiDrawEnd) {
                for (listener in list) {
                    listener()
                }
            }

            list
        }
    }
}
