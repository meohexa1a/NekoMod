// [AGENT INVARIANT] Synchronously update @property, @param, and @see KDocs when modifying this file.

package org.mdt.core.platform

import org.mdt.core.platform.ime.ImePort
import org.mdt.core.platform.ime.SdlReflectionImePort
import org.mdt.core.platform.impl.MindustryAssetPort
import org.mdt.core.platform.impl.MindustryInputPort
import org.mdt.core.platform.impl.MindustrySystemPort
import org.mdt.core.platform.impl.MindustryWindowPort
import org.mdt.core.platform.port.AssetPort
import org.mdt.core.platform.port.InputPort
import org.mdt.core.platform.port.SystemPort
import org.mdt.core.platform.port.WindowPort

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
 *
 * @see PlatformHost
 * @see MindustryWindowPort
 * @see MindustryInputPort
 * @see MindustryAssetPort
 * @see MindustrySystemPort
 * @see SdlReflectionImePort
 */
open class MindustryPlatformHost(
    override val window: WindowPort = MindustryWindowPort(),
    override val input: InputPort = MindustryInputPort(),
    override val assets: AssetPort = MindustryAssetPort(),
    override val system: SystemPort = MindustrySystemPort(),
    override val ime: ImePort = SdlReflectionImePort()
) : PlatformHost
