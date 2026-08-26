package org.mdt.core.engine

import org.mdt.core.engine.i18n.I18nService
import org.mdt.core.engine.image.ImageService
import org.mdt.core.engine.settings.AppSettingsService
import org.mdt.core.engine.settings.SettingsService
import org.mdt.core.engine.storage.StorageService

/**
 * ## EngineContext
 *
 * Universal host environment and modular subsystem hub for NekoMod.
 * Provides abstract storage, image pipeline, localization registry, platform adapters,
 * and context-bound settings service.
 *
 * See: docs/core-subsystems/core_subsystems_en.md
 */
open class EngineContext(
    val name: String = "neko-mod"
) {
    /** Platform host adapter bridging engine capabilities. */
    open val host: PlatformHost by lazy { createPlatformHost() }

    /** Lazy-initialized Storage & FileSystem subsystem. */
    open val storage: StorageService by lazy { createStorageService() }

    /** Lazy-initialized Image Pipeline & Cache subsystem. */
    open val image: ImageService by lazy { createImageService() }

    /** Lazy-initialized Localization subsystem. */
    open val i18n: I18nService by lazy { createI18nService() }

    /** Lazy-initialized universal settings service. */
    open val settings: SettingsService<*> by lazy { createSettingsService() }

    // Service Factories (overridable for Standalone/Editor custom environments)
    protected open fun createPlatformHost(): PlatformHost = MindustryPlatformHost()
    protected open fun createStorageService(): StorageService = StorageService(this)
    protected open fun createImageService(): ImageService = ImageService(this)
    protected open fun createI18nService(): I18nService = I18nService(this)
    protected open fun createSettingsService(): SettingsService<*> = AppSettingsService(this)

    companion object {
        /** Master global context instance for the default NekoMod game environment. */
        val default: ModEngineContext by lazy { ModEngineContext() }

        /** Backward-compatible facade reference to [default]. */
        val current: ModEngineContext inline get() = default
    }
}

/**
 * ## ModEngineContext
 *
 * Specialized [EngineContext] implementation for the Mindustry Mod environment.
 * Strongly typed to provide domain-specific [AppSettingsService].
 */
open class ModEngineContext : EngineContext("neko-mod") {
    override val settings: AppSettingsService by lazy { createSettingsService() }
    override fun createSettingsService(): AppSettingsService = AppSettingsService(this)
}
