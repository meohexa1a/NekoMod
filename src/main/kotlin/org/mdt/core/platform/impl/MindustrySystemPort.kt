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
 * ## MindustrySystemPort [Mindustry / Arc System Implementation]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Implements [SystemPort] by bridging to Arc's `Core.app`, `Core.settings`, and `Events.run(Trigger.uiDrawEnd)`.
 *
 * ### 2. Key Rules & Checklist
 * - [x] `frameEndListeners` must use thread-safe [CopyOnWriteArrayList].
 * - [x] Handles uninitialized `Core.app` safely without throwing null pointer exceptions.
 * - [x] Keep initialization flat and explicit without excessive inline scope functions.
 *
 * ### 3. Related Files
 * - System Port: `src/main/kotlin/org/mdt/core/platform/port/SystemPort.kt`
 * - Platform Host: `src/main/kotlin/org/mdt/core/platform/PlatformHost.kt`
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
        val app = Core.app
        if (app != null) {
            app.clipboardText = text
        }
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
