// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform

/**
 * ## MindustryPlatformHost
 *
 * Default [PlatformHost] implementation composing standard Mindustry and Arc platform ports.
 *
 * @property window Mindustry window and viewport port ([MindustryWindowPort]).
 * @property input Mindustry input multiplexer port ([MindustryInputPort]).
 * @property assets Mindustry asset atlas and font port ([MindustryAssetPort]).
 * @property system Mindustry time, settings, and main thread port ([MindustrySystemPort]).
 * @property ime SDL native IME reflection port ([SdlReflectionImePort]).
 * @property render Arc and OpenGL 2D batch render port ([MindustryRenderPort]).
 *
 * @see PlatformHost
 * @see MindustryWindowPort
 * @see MindustryInputPort
 * @see MindustryAssetPort
 * @see MindustrySystemPort
 * @see SdlReflectionImePort
 * @see MindustryRenderPort
 */
class MindustryPlatformHost(
    override val window: WindowPort = MindustryWindowPort(),
    override val input: InputPort = MindustryInputPort(),
    override val assets: AssetPort = MindustryAssetPort(),
    override val system: SystemPort = MindustrySystemPort(),
    ime: ImePort? = null,
    render: RenderPort? = null,
) : PlatformHost {
    override val ime: ImePort = ime ?: SdlReflectionImePort { this }
    override val render: RenderPort = render ?: MindustryRenderPort { this }
}
