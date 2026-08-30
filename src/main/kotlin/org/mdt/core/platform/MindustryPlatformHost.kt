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
 * ## MindustryPlatformHost [Default Mindustry / Arc Host Bridge]
 *
 * > 🤖 **AGENT DIRECTIVE:** When modifying this class or related logic, synchronously update this KDoc:
 * > 1) Keep Purpose accurate. 2) Update Key Rules & Checklist [x]/[ ]. 3) Maintain Related Files map.
 *
 * ### 1. Purpose
 * - Default [PlatformHost] implementation composing Mindustry and Arc platform ports.
 *
 * ### 2. Key Rules & Checklist
 * - [x] Composed purely by wiring sub-ports; contains zero monolithic inline logic.
 * - [x] Any sub-port can be individually overridden or mocked in constructor.
 *
 * ### 3. Related Files
 * - Platform Facade: `src/main/kotlin/org/mdt/core/platform/PlatformHost.kt`
 * - Window Port: `src/main/kotlin/org/mdt/core/platform/impl/MindustryWindowPort.kt`
 * - Input Port: `src/main/kotlin/org/mdt/core/platform/impl/MindustryInputPort.kt`
 * - Asset Port: `src/main/kotlin/org/mdt/core/platform/impl/MindustryAssetPort.kt`
 * - System Port: `src/main/kotlin/org/mdt/core/platform/impl/MindustrySystemPort.kt`
 * - IME Port: `src/main/kotlin/org/mdt/core/platform/ime/SdlReflectionImePort.kt`
 */
open class MindustryPlatformHost(
    override val window: WindowPort = MindustryWindowPort(),
    override val input: InputPort = MindustryInputPort(),
    override val assets: AssetPort = MindustryAssetPort(),
    override val system: SystemPort = MindustrySystemPort(),
    override val ime: ImePort = SdlReflectionImePort()
) : PlatformHost
