package org.mdt.ui.navigation

/**
 * ## Route
 *
 * Universal, data-driven navigation destination descriptor.
 * Decoupled from hardcoded Kotlin classes to seamlessly support compiled Kotlin composables,
 * declarative NXML text schemas (`nxml:screens/menu.nxml`), and runtime hot-reloaded assets.
 *
 * See: docs/architecture/architecture_en.md
 */
data class Route(
    val path: String,
    val params: Map<String, Any> = emptyMap()
) {
    /** Whether this route points to a dynamic NXML markup schema. */
    val isNxml: Boolean
        get() = path.startsWith("nxml:") || path.endsWith(".nxml")

    /** Resolves a typed parameter from this route's parameter map. */
    @Suppress("UNCHECKED_CAST")
    fun <T> param(key: String, default: T): T = (params[key] as? T) ?: default

    companion object {
        val Editor = Route("editor")
        val MainMenu = Route("main_menu")
        val CampaignPlanet = Route("campaign_planet")
        val CustomSkirmish = Route("custom_skirmish")
        val JoinMultiplayer = Route("join_multiplayer")
        val Settings = Route("settings")
        val Schematics = Route("schematics")
        val ModManager = Route("mod_manager")
        val MapEditor = Route("map_editor")
        val Database = Route("database")

        /** Convenience builder for a route with key-value parameters. */
        fun of(path: String, vararg params: Pair<String, Any>): Route =
            Route(path, params.toMap())

        /** Creates a dynamic NXML schema route. */
        fun nxml(nxmlPath: String, vararg params: Pair<String, Any>): Route =
            Route("nxml:$nxmlPath", params.toMap())
    }
}
