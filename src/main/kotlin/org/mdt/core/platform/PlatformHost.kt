// [AGENT ARCHITECTURE & INVARIANTS]
// - Domain Role: Master Port Facade for Engine & Native Platform Interop.
// - Operating Mechanism: Unifies [WindowPort], [InputPort], [AssetPort], [SystemPort], and [ImePort] into a cohesive interface.
// - Invariants: UI and rendering code must communicate with platform/OS only through PlatformHost.
// - Dependencies: [WindowPort], [InputPort], [AssetPort], [SystemPort], [ImePort], [MindustryPlatformHost].
// - Directive: Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * ## PlatformHost
 *
 * Master composite facade coordinating platform sub-ports ([window], [input], [assets], [system], [ime], and [render]).
 * Keeps UI and rendering layers 100% agnostic of specific game engines or native OS backends.
 *
 * @property window Sub-port managing window sizing, resize listeners, and system cursor icons.
 * @property input Sub-port managing pointer coordinates, modifier keys, and Arc input processors.
 * @property assets Sub-port resolving textures, fonts, byte buffers, and shader texts.
 * @property system Sub-port managing monotonic frame ticks, clipboard, data paths, and main-thread dispatches.
 * @property ime Sub-port managing native on-screen/SDL Input Method Editor composition sessions.
 * @property render Sub-port managing 2D GPU batch rendering, shader compilation, background blur, and font rendering.
 *
 * @see MindustryPlatformHost
 * @see WindowPort
 * @see InputPort
 * @see AssetPort
 * @see SystemPort
 * @see ImePort
 * @see RenderPort
 */
interface PlatformHost {

    // --- DOMAIN PORTS ---

    val window: WindowPort
    val input: InputPort
    val assets: AssetPort
    val system: SystemPort
    val ime: ImePort
    val render: RenderPort

    /** Stub [PlatformHost] implementation combining all [NoOp] sub-ports for testing. */
    object NoOp : PlatformHost {
        override val window: WindowPort get() = WindowPort.NoOp
        override val input: InputPort get() = InputPort.NoOp
        override val assets: AssetPort get() = AssetPort.NoOp
        override val system: SystemPort get() = SystemPort.NoOp
        override val ime: ImePort get() = ImePort.NoOp
        override val render: RenderPort get() = RenderPort.NoOp
    }
}

/**
 * ## LocalPlatformHost [CompositionLocal Bridge]
 *
 * CompositionLocal providing access to the current [PlatformHost].
 */
val LocalPlatformHost = staticCompositionLocalOf<PlatformHost> {
    error("No PlatformHost provided in the current CompositionLocal hierarchy.")
}
