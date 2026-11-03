<div align="center">

# MMCR EMI Compat

**Compatibility fixes for [Modular Machinery: Community Refoxed](https://modrinth.com/mod/modular-machinery-community-refoxed) running with [EMI](https://modrinth.com/mod/emi).**

<img src="logo/mmcr-emi-wordmark.png" width="384" alt="MMCR × EMI COMPAT">

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)
![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-orange)
![EMI + JEI](https://img.shields.io/badge/viewer-EMI%20%2B%20JEI-8a2be2)
![Version](https://img.shields.io/badge/version-1.2.0-blue)
![License](https://img.shields.io/badge/license-MIT-green)

</div>

---

## Why

MMCR ships a **JEI** plugin but no native **EMI** plugin, so under EMI its recipes are read through
EMI's JEMI bridge (EMI's re-implementation of the JEI plugin API). JEMI diverges from JEI in four
places, and MMCR's recipe pages sit exactly on those seams:

| # | Symptom | Root cause |
|---|---------|------------|
| 1 | `Exception adding JEMI extras` — a `NullPointerException` on **every** MMCR recipe page | JEMI builds extras with `new JemiRecipeExtrasBuilder(null)`, but MMCR dereferences `getRecipeSlots()` unconditionally |
| 2 | MMCR machine recipe categories **completely missing** from EMI | JEMI skips every JEI category whose id namespace belongs to a mod that ships an EMI plugin (`because mod is already handled`). MMCR machine categories are named `<recipe-pool namespace>:recipe_pool/<machine>`; packs normally define pools through **KubeJS**, so the namespace becomes `kubejs` — a mod that has an EMI plugin registering *no* categories. The categories are then neither registered natively nor bridged. |
| 3 | Multiblock structure preview renders but **cannot be rotated or zoomed** | JEMI collects JEI's `IJeiInputHandler`s and never uses them; EMI widgets only expose `mouseClicked`/`keyPressed`, and `RecipeScreen.mouseDragged/mouseScrolled` never reach ordinary widgets |
| 4 | Preview drawn **offset up-left** (4, 23 px), layer buttons clipped, hit-tests shifted | JEMI wraps every `IRecipeWidget` in a `0×0` drawable anchored at `(0,0)` and passes page-absolute mouse coords, while JEI's contract is *translate the pose to the widget origin, pass widget-local coordinates* |

<details>
<summary>Raw symptoms (log excerpts)</summary>

```text
[EMI/]: Exception adding JEMI extras
java.lang.NullPointerException: Cannot invoke
  "IRecipeSlotDrawablesView.getSlots(RecipeIngredientRole)" because the return value of
  "IRecipeExtrasBuilder.getRecipeSlots()" is null
    at cn.howxu.mmcr.compat.jei.MachineStructureCategory.createRecipeExtras(MachineStructureCategory.java:120)
    at dev.emi.emi.jemi.JemiRecipe.addWidgets(JemiRecipe.java:148)
```

```text
[JEMI] Skipping recipe category kubejs:recipe_pool/iceiceice because mod is already handled
[JEMI] Skipping recipe category kubejs:recipe_pool/electric_blast_furnace because mod is already handled
```

</details>

## What it does

Six narrowly-scoped mixins — **under real JEI nothing changes**:

| Mixin | Target | Effect |
|-------|--------|--------|
| `MachineStructureCategoryMixin` | MMCR `createRecipeExtras` | Skips the slot-dependent material-paging widget when `getRecipeSlots()` is null or not 9 slots (preview already added); also dodges MMCR's `IllegalArgumentException` guard |
| `MachineRecipeCategoryMixin` | MMCR `createRecipeExtras` | Same guard for the metadata-detail widget; process arrows are kept |
| `JemiPluginMixin` | EMI `JemiPlugin.register` | Only for `cn.howxu.mmcr.*` categories, bypasses the "namespace already handled" skip and re-registers the lost machine categories |
| `JemiRecipeMixin` | EMI `JemiRecipe.addWidgets` | Resets the input-handler collector per recipe page and records the page origin |
| `JemiRecipeExtrasBuilderMixin` | EMI `JemiRecipeExtrasBuilder` | ① places extras at their declared `getScreenRectangle()` position/size with widget-local mouse coordinates; ② captures MMCR's input handlers (JEMI drops them) |
| `RecipeScreenMixin` | EMI `RecipeScreen` | Forwards click / release / drag / scroll to the captured handlers (page coords → widget-local coords), consuming only events that land inside the preview |

Every new injection uses `require = 0`: if MMCR or EMI refactors the targeted bytecode, the patch
fails **soft** (silently inactive) instead of breaking game startup.

### Result matrix

| | JEI only | EMI + JEI (before) | EMI + JEI (with this mod) |
|---|---|---|---|
| `Exception adding JEMI extras` spam | – | ✗ (16×/session) | **✓ none** |
| MMCR machine categories in EMI | – | ✗ missing | **✓ present** |
| Machine recipe layout / arrows | ✓ | ✓ | **✓** |
| Structure preview position & size | ✓ | ✗ offset | **✓** |
| Preview rotate / zoom / layer buttons | ✓ | ✗ | **✓** |
| Material paging overlay, recipe metadata pages | ✓ | ✗ | ✗ *(see limitations)* |

## Installation

1. Remove any older `mmcr_emi_compat-*.jar` (same mod id cannot be duplicated).
2. Drop `mmcr_emi_compat-1.2.0.jar` into your `mods` folder.
3. Restart and check:
   - EMI's category list shows your MMCR machines (e.g. `kubejs:recipe_pool/<machine>`);
   - the log prints `[mmcr_emi_compat] re-registered MMCR JEI category ...`;
   - the multiblock structure page: preview sits below the title, layer buttons visible,
     drag rotates, scroll zooms.

Rollback: delete the jar.

## Requirements

| Component | Required | Tested |
|-----------|----------|--------|
| Minecraft | 1.21.1 | 1.21.1 |
| NeoForge | ≥ 21.1.0 | 21.1.x |
| [MMCR](https://modrinth.com/mod/modular-machinery-community-refoxed) | ≥ 0.1.0 | 0.1.4 |
| [EMI](https://modrinth.com/mod/emi) | 1.1.x | 1.1.24 |
| [JEI](https://modrinth.com/mod/jei) | 19.x *(needed by EMI's JEMI bridge)* | 19.56 |

No configuration files; server and client both accept the jar (the input bridge is client-side).

## Known limitations

1. **Material paging overlay & recipe metadata pages are absent under EMI.** Both UIs require JEI's
   `IRecipeSlotDrawable`, which JEMI supplies as `null`. The mod skips them cleanly instead of
   faking slots (fake slots would page wrong content). The structure page still shows its first 9
   materials and the machine recipe layout is complete.
2. **Without JEI installed, EMI cannot see MMCR recipes at all** — JEMI needs JEI as its data source
   and MMCR has no native `EmiPlugin`. A native EMI plugin for MMCR would be a separate project.
3. **Version binding:** injection targets correspond to MMCR 0.1.4 / EMI 1.1.24 (JEI 19.56).
   After upgrading either, check the log — failures are intentionally silent (`require = 0`).
4. An `AbstractMethodError` involving `JemiTextWidget` (missing `ITextWidget#setPosition`) seen in
   some logs is an EMI-vs-JEI version mismatch inside JEI's own tag pages — unrelated to this mod.

## Building

**Gradle** (requires access to NeoForged Maven; Gradle 8.10+ — NeoGradle 7.0.171 supports neither
Gradle 8.8 nor 9.x):

```bash
# put the MMCR and JEI jars into libs/ first (see libs/README.txt)
gradle build
```

**Offline fallback** (`tools\build.ps1`) — compiles with plain `javac` against a NeoGradle-produced
dev-merged Minecraft + NeoForge jar and local mod jars, no network needed. The default paths inside
the script are machine-specific; adjust the `$deobf` / `$libs` / `-Instance` variables for your
machine. The script also verifies the jar layout and that every injection target still exists in
the upstream bytecode.

## Repository layout

```
├─ src/main/java/mmcremi/        mod entry point, input bridge, 6 mixins
├─ src/main/resources/           neoforge.mods.toml (logoFile), mixin config, mod icon
├─ tools/build.ps1               offline javac build + injection-target verification
├─ logo/                         branding: PNG/SVG art + pixel-font generation scripts
├─ libs/README.txt               jars to drop in for the Gradle build
└─ _analysis/                    decompiled upstream sources & evidence behind each fix
```

The logo itself is generated from `logo/gen_wordmark.py` — pixel glyphs, colours and even the
background texture are derived from measurements of EMI's own icon (see the script header).

## 中文

**让 MMCR 的配方页在 EMI 下正常工作的小型修复模组。**

MMCR 只有 JEI 插件，在 EMI 下要经过 JEMI 桥接；JEMI 与 JEI 有四处行为差异，恰好全被 MMCR 踩中：
每页配方抛 NPE、机器配方类别因「命名空间已存在 EMI 插件」被整体跳过（KubeJS 命名空间误伤）、
结构预览收不到拖拽/滚轮、附加部件坐标系错位。本模组用 6 个精准注入修掉这四点，
**原生 JEI 环境下行为完全不变**；所有注入 `require = 0`，上游改版时安静失效而非崩启动。

限制：结构页材料翻页高亮与配方元数据详情依赖 JEI 的可绘制 slot（JEMI 给 null），选择跳过不伪造；
不装 JEI 时 EMI 读不到 MMCR 配方（JEMI 需要 JEI 做数据源）。

## License & credits

- **MIT** — see [LICENSE](LICENSE).
- Derivative context: [MMCR](https://github.com/Nibelungorum/ModularMachinery-Community-Refoxed) (GPL-3.0),
  [EMI](https://modrinth.com/mod/emi), [JEI](https://modrinth.com/mod/jei) — their code is not bundled,
  only referenced via mixins at runtime.
- Logo built in EMI's visual language; glyph metrics measured from EMI's icon.
