# NXML Schema & Declarative UI Specification

---

## 🧭 I. Core Philosophy & Architectural Foundations

**NXML (Neko XML/Markup)** is a high-performance, data-driven declarative UI schema designed to replace ~95% of boilerplate Kotlin UI code in NekoMod. It enables hot-reloading in $<30\text{ms}$, eliminates compilation wait times, and provides a clean separation between UI layout and game logic.

### 🏛️ 1. Single Master File Architecture (App Bundle)
* Instead of loading dozens of individual `.nxml` files with continuous disk I/O overhead, the entire application UI and its scenes are packaged into a single master bundle (`assets/ui/app.nxml`).
* Parsed once into memory on startup with zero runtime disk reads, guaranteeing $100\%$ stability and instant scene transitions.

### 🧱 2. Minimal Primitive Node Invariant (5 Engine Primitives)
In accordance with Rule 27, the low-level engine parser recognizes **ONLY 5 primitive rendering nodes**:
1. **`<Box>`**: The universal GPU SDF primitive (rounded corners, background colors, frosted glass backdrop blur, borders, glow, inner/outer shadows, insets, sizing, clipping, click handlers).
2. **`<Text>`**: 1.0x BMFont typography renderer (word wrapping, alignment, color tokens).
3. **`<Row>`**: Horizontal flex container (gap spacing, flex weights, cross-axis alignment).
4. **`<Column>`**: Vertical flex container (gap spacing, flex weights, cross-axis alignment).
5. **`<Image>`**: Texture & Mindustry Sprite Atlas region renderer (`ScaleMode.FIT`, `CROP`, `STRETCH`).

All higher-level widgets (`<Card>`, `<Button>`, `<Toggle>`, `<Slider>`, `<SegmentedControl>`) are **NOT** hardcoded in Kotlin; they are declared as reusable **`<template>`** definitions composed purely of these 5 primitives.

---

## 🏷️ II. Concrete Tag Taxonomy & Specification

### 1. Document Root & Global Blocks

```xml
<nxml app="NekoMod" version="1.0"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:noNamespaceSchemaLocation="nxml.xsd">

    <!-- I. Reactive State Variables -->
    <state>
        <var name="selectedTab" type="string" default="graphics" />
        <var name="musicVol"    type="float"  default="1.0" />
        <var name="sfxVol"      type="float"  default="1.0" />
        <var name="particles"   type="bool"   default="true" />
        <var name="serverList"  type="list"   default="[]" />
    </state>

    <!-- II. Internationalization & Interpolation -->
    <i18n>
        <locale id="en">
            <string id="welcome">Welcome, {player}!</string>
            <string id="play">Play Campaign</string>
        </locale>
        <locale id="vi">
            <string id="welcome">Chào mừng, {player}!</string>
            <string id="play">Chơi Chiến Dịch</string>
        </locale>
    </i18n>

    <!-- III. Sandboxed HTTP Fetching Pipeline -->
    <http id="fetchReleases"
          url="https://api.github.com/repos/Anuken/Mindustry/releases"
          method="GET"
          auto="true"
          cache="10m"
          target="@serverList" />

    <!-- IV. Reusable Component Templates -->
    <template id="Card">
        <Box radius="16" background="$glassRegular" border="1" borderColor="$borderSubtle" padding="16">
            <slot />
        </Box>
    </template>

    <!-- V. Independent Application Scenes -->
    <scene id="main_menu">
        ...
    </scene>

</nxml>
```

---

## ⚛️ III. React-Inspired Text & Expression Grammar

Following modern React / JSX standards:
* **Raw Literal Text:** Permitted only for trivial constants (`<Text text="OK" />`).
* **Long Paragraphs & Dynamic Strings:** Must reside in `<i18n>` or `@state` and referenced via `{...}` or `$t(...)`.

### Prefixes & Expression Syntax:
1. **`@variable`**: Reactive two-way state binding (e.g. `value="@sfxVol"`).
2. **`$colorToken`**: Design system tokens (e.g. `$accent`, `$textPrimary`, `$surfaceElevated`).
3. **`$t(id, key=value)`**: Multi-language string interpolation (e.g. `$t(welcome, player=@username)`).
4. **`onClick="action"`**: Declarative action piping:
   * `nav:sceneId`: Route to destination scene.
   * `nav:back`: Pop navigation backstack.
   * `set:var=val`: Mutate state variable directly.
   * `http:requestId`: Trigger declarative network call.
   * `sound:soundId`: Play game SFX.

---

## 🚧 IV. Open Architectural Decisions (Pending Review)

The following architectural points are documented for further iteration and testing:

| # | Topic | Option A | Option B | Current Recommendation |
| :--- | :--- | :--- | :--- | :--- |
| **1** | **Expression Evaluator Engine** | **Janino with Strict AST Whitelist**<br>- Native Java expression speed<br>- Full Java `Math.*` capabilities<br>- Requires security filter | **Tiny Pure Kotlin Pratt Parser**<br>- 150 lines, 0 external deps<br>- $100\%$ safe sandbox<br>- Only handles basic `+ - * / ? :` | Start with **Tiny Pure Kotlin Parser** for zero-dependency safety, upgrade to Sandboxed Janino if complex scripts are needed. |
| **2** | **Hot-Reload File Watcher** | **Java NIO WatchService**<br>- Native OS file system events<br>- $<30\text{ms}$ response | **Frame-Polling Timestamp Check**<br>- Checks `file.lastModified()` once every 500ms<br>- Highly compatible across platforms | **NIO WatchService** on Desktop with timestamp fallback for mobile. |
| **3** | **Standard Component Library (`std_components.nxml`)** | **Embedded in Mod JAR Classpath**<br>- Auto-loaded at boot<br>- Mods can override templates | **Inlined in `app.nxml`**<br>- Everything in one file<br>- Simpler parsing pipeline | **Embedded in Classpath** with local project override capability. |
| **4** | **IDE Tooling & Schema Distribution** | **Static `nxml.xsd` File**<br>- Placed in project root<br>- Works in IntelliJ & VS Code out-of-the-box | **Custom IntelliJ Plugin**<br>- Full syntax colorization<br>- Live embedded preview window | **Static `nxml.xsd`** first for immediate zero-friction autocomplete. |

---

## 🔗 Related Documentation
* [Architecture Overview (EN)](../architecture/architecture_en.md)
* [Design System Standards (EN)](../coding-standards/coding_standards_en.md)
* [Rendering Shaders & Blur (EN)](../rendering-shaders/rendering_shaders_en.md)
