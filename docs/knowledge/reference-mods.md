# Reference mods, datapacks and tools (prior art map)

Scope: open-source Fabric mods and datapacks that already run on 26.x, chosen as real-world examples of each modding system, plus the dev tools to use instead of writing our own and a list of ideas that are already taken. Verified 2026-11-14 against MC 26.3. Method: Modrinth API v2 (`/v2/project/<slug>` for license and source; `/v2/project/<slug>/version?loaders=["fabric"]&game_versions=["26.3"]` for a real Fabric 26.3 file), `git ls-remote` for branches, and shallow clones (`--depth 1 --filter=blob:limit=150k`) of each branch to confirm the MC version in its build files and that every file path below exists. Anything not checked this way is marked UNVERIFIED.

Companion files: `libraries.md` covers libraries you depend on (GeckoLib, YACL, Cloth, Polymer as a dependency, and so on). This file covers mods you read. `tools/prior-art.py "<idea>"` searches Modrinth for duplicates (CLAUDE.md B8).

---

## 0. How to use this file

### Reading a reference mod cheaply
```bash
# blobs under 150 KB only (skips textures), sparse checkout of source files only
git clone -q --depth 1 --filter=blob:limit=150k --sparse -b <branch> https://github.com/<owner>/<repo> /tmp/.../scratchpad/ref/<repo>
cd /tmp/.../scratchpad/ref/<repo> && git sparse-checkout set --no-cone '*.java' '*.kt' '*.kts' '*.gradle' '*.toml' '*.properties' 'fabric.mod.json' '*mixins.json'
```
- Clone the branch named below, not the default branch. Many repos keep an old version as `HEAD`: TerraBlender's `HEAD` is 26.3, but Farmer's Delight Refabricated's is `fabric/1.21.1-3.0.0`, AppleSkin's is `1.16-forge`, Iris's is `26.1` and Tom's Storage's `master` is 1.21.1.
- `git grep` on a `--filter=blob:none` clone fetches each blob one by one and is very slow. Use `blob:limit` with a sparse checkout, then plain `grep -r`.
- Each mod's own classes still use its own names (Tech Reborn has `BuiltScreenHandler`, for example). Only `net.minecraft.*` and `net.fabricmc.*` imports tell you the 26.3 names, and you should still check those against `/tmp/claude-0/mcsrc`.

### What a license lets us do
Reading and learning from any public source is fine. Copying code is a separate question:

| License (code) | Copy code into our mod? | Obligations |
|---|---|---|
| MIT, BSD-3-Clause, Apache-2.0, Unlicense, ISC/0BSD-style (Lantern Load) | Yes | Keep the copyright and license notice (Apache: also state your changes and keep any NOTICE file). Unlicense and ISC-style need nothing, but credit anyway. |
| MPL-2.0 | Yes, file by file | Copied files stay MPL-2.0 with their header. The rest of our mod can use any license. |
| LGPL-3.0 / LGPL-2.1 | Only if the copied code stays LGPL | Practical rule: depend on it as a library, or keep copied files under LGPL in a clearly marked package. Never paste it into an MIT-licensed file. |
| GPL-3.0, AGPL-3.0 | Only if our whole mod is GPL/AGPL-compatible | Copying turns the whole mod GPL (AGPL adds network-use terms). Read only, unless the owner picks GPL. |
| CC-BY-NC-SA, CC-BY-NC-ND, Polyform Shield, "Protective"/custom, ARR (source-available) | No | Read to learn patterns, never copy, never ship assets. |

Our repo `LICENSE` is empty: the `license` inbox item is still open. Until it is decided, treat only MIT, BSD, Apache, Unlicense, ISC and MPL code as copyable.

**Assets are often licensed separately and more strictly.** Mutant Monsters, Easy Anvils and Air Hop ship `LICENSE-ASSETS.md` (ARR) next to code under MPL or AGPL. BetterEnd ships `LICENSE.ASSETS`. Never copy textures, models or sounds unless the asset license allows it.

**Modrinth and the repo can disagree. Trust the repo's LICENSE file, and if there isn't one, assume "read only".** Mismatches found:
- Sit!: Modrinth says LGPL-3.0, but the 26.3 branch `master` has no LICENSE file.
- Mythic Upgrades: Modrinth says MIT, but there is no LICENSE file on `26.3-dev`.
- Traveler's Backpack: Modrinth says LGPL-3.0, but there is no LICENSE file on `26.1-fabric`.
- Mutant Monsters: Modrinth says AGPL-3.0, and so does `LICENSE.md`, but the assets are ARR.
- Enchancement: the repo `LICENSE` says "All Rights Reserved", so the source is readable but not reusable.

---

## 1. Fabric mods by system

Format: **Name** (`modrinth-slug`): license, Fabric 26.3 build on Modrinth (version, date), repo at `branch`. Paths are relative to the repo root.

### 1.1 Custom blocks, block entities, GUIs and menus

**Farmer's Delight Refabricated** (`farmers-delight-refabricated`): MIT. Fabric 26.3 build `26.3-3.6.30` (2026-10-24). github.com/MehVahdJukaar/FarmersDelightRefabricated at `fabric/latest/26.3` (`minecraft_version=26.3`).
- `src/main/java/vectorwing/farmersdelight/common/block/entity/CookingPotBlockEntity.java`: a BE that implements Fabric `ExtendedMenuProvider<BlockPos>`, saves with 26.x `loadAdditional(ValueInput)`/`saveAdditional(ValueOutput)` (not NBT `CompoundTag`), and exposes inventory through Fabric Transfer `ItemStorage`/`Storage<ItemVariant>`.
- `src/main/java/vectorwing/farmersdelight/common/block/entity/container/CookingPotMenu.java` (+ `CookingPotResultSlot`, `CookingPotMealSlot`): an `AbstractContainerMenu` with custom slots and `ContainerData` progress sync.
- `src/main/java/vectorwing/farmersdelight/client/gui/CookingPotScreen.java` and `CookingPotRecipeBookComponent.java`: the screen, plus a recipe-book integration for a custom recipe type.
- `src/main/java/vectorwing/farmersdelight/common/registry/ModMenuTypes.java`: menu type registration.
- `src/main/java/vectorwing/farmersdelight/common/block/entity/SkilletBlockEntity.java` / `AbstractStoveBlockEntity.java`: a BE with no GUI that shows its items in the world, and heat sources.

**Extended Drawers** (`extended-drawers`): Apache-2.0. `5.2.0+mc.26.3` (2026-10-11). github.com/MattiDragon/ExtendedDrawers at `26.3` (`libs.versions.toml minecraft="26.3"`).
- `src/main/java/io/github/mattidragon/extendeddrawers/block/entity/DrawerBlockEntity.java`: a storage BE that registers `ItemStorage.SIDED`.
- `src/client/java/io/github/mattidragon/extendeddrawers/client/renderer/AbstractDrawerBlockEntityRenderer.java` (+ `DrawerBlockEntityRenderer`, `CompactingDrawerBlockEntityRenderer`): BE renderers that draw items and text on a block face.
- `src/main/java/io/github/mattidragon/extendeddrawers/component/ModDataComponents.java`: data components on block items, so contents survive breaking the block.
- `src/main/java/io/github/mattidragon/extendeddrawers/networking/SetLimiterLimitPayload.java`: a small C2S payload driven by a GUI.

**Tom's Simple Storage** (`toms-storage`): MIT. Fabric 26.3 build `26.3-2.12.2-fabric` (2026-10-15), but **the public source stops at the `26.2` branch** (`Fabric/gradle.properties minecraft_version=26.2`). It is the reference for a storage terminal and connector network.
- `Fabric/src/main/java/com/tom/storagemod/inventory/PlatformInventoryAccess.java` and `PlatformMultiInventoryAccess.java`: wraps Fabric Transfer to aggregate many inventories.
- `Fabric/src/main/java/com/tom/storagemod/platform/PlatformBlockEntity.java`, `screen/PlatformContainerScreen.java`: a thin platform layer for a two-loader mod.

### 1.2 Machines and item/fluid/energy transfer

**Tech Reborn** (`techreborn`): MIT. `6.2.1` (2026-10-23). github.com/TechReborn/TechReborn at `26.3` (default branch). Contains RebornCore.
- `RebornCore/src/main/java/reborncore/common/powerSystem/PowerAcceptorBlockEntity.java`: energy through Team Reborn Energy (`team.reborn.energy.api.EnergyStorage`, `SimpleSidedEnergyContainer`) and items through Fabric `ContainerItemContext`/`ContainerStorage`.
- `RebornCore/src/main/java/reborncore/common/blockentity/SlotConfiguration.java` and `FluidConfiguration.java`: per-side IO configuration, auto-input and auto-output through `ItemStorage.SIDED`/`FluidStorage.SIDED`.
- `RebornCore/src/main/java/reborncore/common/screen/builder/ScreenHandlerBuilder.java` (+ `BlockEntityScreenHandlerBuilder`): a fluent menu builder that syncs fields with no hand-written `ContainerData`.
- `src/main/java/techreborn/blockentity/machine/tier1/AlloySmelterBlockEntity.java`: a typical recipe machine.
- `src/main/java/techreborn/init/TRBlockEntities.java`, `src/main/java/techreborn/events/ModRegistry.java`: BE type registration and lookup registration.

**PolyFactory** (`polyfactory`): LGPL-3.0. `0.13.1+26.3` (2026-09-28). github.com/patbox/PolyFactory at `master` (`minecraft_version=26.3`). A server-side machine mod: rotation networks, conveyors and fluids, all built on Polymer (see 1.12).
- `src/main/java/eu/pb4/polyfactory/block/mechanical/machines/crafting/GrinderBlock.java` / `GrinderBlockEntity.java`: a machine block with an `ElementHolder` model, built on `eu.pb4.factorytools.api.block.FactoryBlock`.
- `src/main/java/eu/pb4/polyfactory/nodes/`: graph-based power and fluid networks.
- `src/main/java/eu/pb4/polyfactory/models/fluid/TopFluidViewModel.java`: shows fluid levels with display entities.
- `src/main/java/eu/pb4/polyfactory/item/FactoryDataComponents.java`, `FactoryEnchantmentEffectComponents.java`: custom data components and a custom **enchantment effect component** type.

**Storage Drawers** (`storagedrawers`): MIT. `26.3.0.1+fabric` (2026-09-29). github.com/jaquadro/StorageDrawers at `26.3` (the version lives in `fabric/build.gradle.kts`; root `gradle.properties` still says 1.21). Fabric, Forge and NeoForge, through its own "chameleon" platform layer.
- `fabric/src/main/java/com/jaquadro/minecraft/storagedrawers/capabilities/PlatformCapabilities.java`: `ItemStorage.SIDED` registration on Fabric.
- `fabric/src/main/java/com/texelsaurus/minecraft/chameleon/service/FabricNetworking.java`: a cross-loader payload layer.
- `common/src/main/java/com/jaquadro/minecraft/storagedrawers/core/ModDataComponents.java`.

Not on Fabric 26.3: Modern Industrialization (last 1.21.1), Oritech (last 26.1.2, CC-BY-4.0), Modern Dynamics (NeoForge only for 26.3), Create Fabric (1.20.1), Simple Pipes (1.21.1), AE2 and Mekanism (no Fabric builds).

### 1.3 Custom entities, mob AI, renderers and models (vanilla model system)

**Mutant Monsters** (`mutant-monsters`): code AGPL-3.0-or-later (read only unless we go GPL), assets ARR. `26.3.1` (2026-10-06). github.com/Fuzss/mutantmonsters at `26.3.x`. Fabric and NeoForge through Fuzss's Puzzles Lib (`project.libs=26.3-SNAPSHOT`).
- `Common/src/main/java/fuzs/mutantmonsters/common/world/entity/mutant/MutantZombie.java`: a large hostile mob with `registerGoals()` and custom attack goals.
- `Common/src/main/java/fuzs/mutantmonsters/common/world/entity/ai/goal/`: reusable goals (`AnimationGoal`, `MutantMeleeAttackGoal`, `AvoidDamageGoal`, `HurtByNearestTargetGoal`).
- `Common/src/main/java/fuzs/mutantmonsters/common/client/model/MutantZombieModel.java`: `extends EntityModel<MutantZombieRenderState>`, with `static LayerDefinition createBodyLayer()` and `setupAnim(state)`, the 26.x render-state pattern.
- `Common/src/main/java/fuzs/mutantmonsters/common/client/renderer/entity/MutantZombieRenderer.java` + `renderer/entity/state/MutantZombieRenderState.java`, `client/model/geom/ModModelLayers.java`.
- `Common/src/main/java/fuzs/mutantmonsters/common/client/renderer/rendertype/ModRenderTypes.java`: a custom `RenderPipeline`/render type.
- `Common/src/main/java/fuzs/mutantmonsters/common/network/ClientboundAnimationMessage.java`: server-triggered animation sync.

**BetterEnd** (`betterend`): MIT (code; assets under `LICENSE.ASSETS`). `26.302.1` (2026-11-01). github.com/quiqueck/BetterEnd at `release/26.3`. Depends on BCLib (MIT, `bclib`) and WunderLib.
- `src/main/java/org/betterx/betterend/registry/EndEntities.java` and `EndEntitiesRenders.java`: entity type, attribute and renderer registration.
- `src/main/java/org/betterx/betterend/entity/SilkMothEntity.java`, `entity/model/SilkMothEntityModel.java`, `entity/render/SilkMothEntityRenderer.java`: a small flying ambient mob end to end.
- `src/main/java/org/betterx/betterend/entity/model/EndFishEntityModel.java`, `CubozoaEntityModel.java`: water mobs.

**Boids** (`boids`): MIT. `2.0.0+26.3` (2026-09-16). github.com/Tomate0613/boids at `26.3`. Server-only flocking AI added to vanilla fish.
- `src/main/java/dev/doublekekse/boids/goals/BoidGoal.java`: a custom `Goal` with steering math.
- `src/main/java/dev/doublekekse/boids/mixin/MobMixin.java`: injects goals into existing vanilla mobs.
- `src/main/java/dev/doublekekse/boids/asm/BoidsMixinPlugin.java` + `IfMod.java`: conditional mixins (apply only if another mod is present).

**SmartBrainLib** (`smartbrainlib`): MPL-2.0. `2.0.3` (2026-10-04). github.com/Tslat/SmartBrainLib at `Main` (`libs.versions.toml minecraft="26.3"`). A library that replaces Goals with Brain-based behaviours. Its examples are the best documentation of the vanilla `Brain`.
- `common/src/main/java/net/tslat/smartbrainlib/example/SBLSkeleton.java`: a full mob built on SBL.
- `common/src/main/java/net/tslat/smartbrainlib/api/SmartBrainOwner.java`, `api/core/behaviour/custom/attack/AnimatableMeleeAttack.java`.
- `fabric/src/main/java/net/tslat/smartbrainlib/example/boilerplate/SBLExampleCommon.java`: Fabric registration boilerplate.

Not on Fabric 26.3: Cobblemon (1.21.1), Bosses of Mass Destruction (1.21.1), Creeper Overhaul (ARR). Friends&Foes is on 26.3 but CC-BY-NC-ND (read only). Naturalist has no public source.

### 1.4 Animated entities (GeckoLib)

**The Graveyard** (`the-graveyard-fabric`): GPL-3.0 (read only). `4.0.0+mc26.3-fabric` (2026-10-27). github.com/finallion/The_Graveyard at `26.3` (default branch is `1.21.8`). Uses `com.geckolib:geckolib-fabric-26.3:5.5.7` (`fabric/build.gradle`).
- `common/src/main/java/com/lion/graveyard/entities/LichEntity.java`: `extends Monster implements GeoEntity`, with `registerControllers(AnimatableManager.ControllerRegistrar)`, `RawAnimation` and `GeckoLibUtil` cache. GeckoLib 5 packages are `com.geckolib.*`, not `software.bernie.geckolib.*`.
- `common/src/main/java/com/lion/graveyard/entities/models/LichModel.java`: `extends GeoModel<LichEntity>`.
- `common/src/main/java/com/lion/graveyard/entities/renders/LichRenderer.java`: `GeoEntityRenderer<LichEntity, R extends LivingEntityRenderState>`, using `GeoRenderState`/`DataTickets`/`BoneSnapshots` (the GeckoLib 5 render-state API).
- `common/src/main/java/com/lion/graveyard/init/TGEntityRenderers.java`; `entities/ai/goals/LichMeleeGoal.java`: animation-timed attack goal.
- `common/src/main/java/com/lion/graveyard/blockentities/renders/SarcophagusBlockEntityRenderer.java`: an animated BE.

**GeckoLib itself** (`geckolib`): MIT. `5.5.7` (2026-09-22). github.com/bernie-g/geckolib at `main` (`minecraft strictly [26.3,)`). The 26.3 branch has **no testmod or examples**. Read `common/src/main/java/com/geckolib/renderer/` and `fabric/src/main/java/com/geckolib/network/GeckoLibNetworkingFabric.java`. The wiki is UNVERIFIED for 26.x.

Other GeckoLib 26.3 mods (Naturalist and others) are closed source. AzureLib is the alternative (`libraries.md`).

### 1.5 Custom items, tools and armor with data components

**Mythic Upgrades** (`mythic-upgrades`): Modrinth says MIT, but the repo has **no LICENSE file** (read only until confirmed). `5.1.1` (2026-09-28). github.com/TriQue06/mythic-upgrades-multiloader at `26.3-dev`.
- `common/src/main/java/net/trique/mythicupgrades/item/MythicToolMaterials.java`: the 26.x `ToolMaterial` record, `new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, durability, speed, attackBonus, enchantability, repairTag)`. Repair items are a `TagKey<Item>`.
- `common/src/main/java/net/trique/mythicupgrades/item/MythicArmorMaterials.java`: data-driven armor materials.
- `common/src/main/java/net/trique/mythicupgrades/item/MythicUpgradeTemplateItem.java`: smithing templates.
- `neoforge/src/main/java/net/trique/mythicupgrades/datagen/MythicTrimMaterialProvider.java`: trim materials (datagen sits on the NeoForge side in this repo).

**Construction Wand (Fabric)** (`construction-wand-fabric`): MIT. `2.1.3+26.3` (2026-09-20). github.com/JongWasTaken/ConstructionWand-Fabric at `master` (`minecraft_version=26.3`).
- `src/main/java/dev/smto/constructionwand/basics/option/WandOptions.java`: per-stack settings stored as data components.
- `src/main/java/dev/smto/constructionwand/items/wand/WandItem.java`, `wand/WandJob.java`: an item with block-placement logic and undo (`wand/undo/`).
- `src/main/java/dev/smto/constructionwand/containers/handlers/HandlerBundleComponent.java`: reads items from a bundle `DataComponents.BUNDLE_CONTENTS`.
- `src/main/java/dev/smto/constructionwand/integrations/polymer/`: optional server-side mode (the same items as `PolymerItem`).

**Tech Reborn** `src/main/java/techreborn/component/TRDataComponentTypes.java`, **Extended Drawers** `component/ModDataComponents.java` and **Universal Graves** `registry/GraveCompassComponent.java` are smaller `DataComponentType.builder()` examples with codecs.

### 1.6 Enchantments and data-driven content from a mod

**Enchantments-Plus** (`enchantments-plus`): MIT. `1.2.6+26.3` (2026-10-26). github.com/RobertHoueland/enchantments-plus at `26.3`.
- `src/main/resources/data/enchantmentsplus/enchantment/lifesteal.json` (and 16 more): hand-written 26.3 enchantment JSON (`effects."minecraft:post_attack"`, `exclusive_set`, `supported_items`, `primary_items`, `slots`, `min_cost`/`max_cost`).
- `src/main/java/com/robdog777/enchantmentsplus/enchants/EnchantmentEffects.java`: a custom `EnchantmentEntityEffect` type (`MapCodec` with an `"action"` field), referenced from the JSON as `"type": "enchantmentsplus:custom"`.
- `src/main/resources/data/minecraft/tags/enchantment/{tradeable,treasure,on_random_loot,non_treasure}.json`: hooking into vanilla enchantment tags.
- `src/main/java/com/robdog777/enchantmentsplus/network/EnchantmentConfigPayload.java`: syncing the server config to clients.

**Air Hop** (`air-hop`): MPL-2.0 (code), assets ARR. `26.3.0` (2026-09-26). github.com/Fuzss/airhop at `26.3.x`.
- `Common/src/main/java/fuzs/airhop/common/init/ModRegistry.java`: registers an enchantment in a datagen `bootstrapEnchantments(BootstrapContext<Enchantment>)` with `Enchantment.enchantment(Enchantment.definition(...))` and `Enchantment.dynamicCost`.
- `Common/src/main/java/fuzs/airhop/common/data/ModEnchantmentTagProvider.java`; `network/client/ServerboundAirHopMessage.java`.

**Enchancement** (`enchancement`): **ARR, source-available (read only)**. `26.3-r4`. github.com/MoriyaShiine/enchancement at `main`. The biggest real-world set of custom enchantment effect component types.
- `common/src/main/java/moriyashiine/enchancement/common/init/EnchancementEnchantmentEntityEffectTypes.java`, `EnchancementEnchantments.java`, `EnchancementDataComponents.java`.
- `common/src/main/java/moriyashiine/enchancement/mixin/enchantmententityeffecttype/`: how each custom effect is wired in with mixins.
- `common/src/main/java/moriyashiine/enchancement/client/EnchancementClient.java`: `HudElementRegistry.attachElementBefore/After` usage.

Also see **PolyFactory** `item/FactoryEnchantmentEffectComponents.java` (LGPL) and Polymer's testmod `TestEnchantmentEntityEffect.java`. For a datapack-only take, see Enchantments Encore (2.1).

### 1.7 Worldgen: biomes, features, structures, dimensions

**Lithostitched** (`lithostitched`): MIT. `2.0.6-fabric-26.3` (2026-10-13). github.com/Apollounknowndev/lithostitched at `26.3` (Stonecutter, `mod.mc_compat = ">=26.2"`). A worldgen library: datapack-driven "worldgen modifiers" (add or remove features, structures and spawns from JSON), custom processors, placement modifiers and density functions.
- `src/main/java/dev/worldgen/lithostitched/api/worldgen/modifier/WorldgenModifier.java`: the modifier registry type and its codecs.
- `src/main/java/dev/worldgen/lithostitched/api/worldgen/biomeinjector/BiomeInjector.java` + `ParameterBuilder.java`: injecting biomes into the multi-noise source.
- `src/main/java/dev/worldgen/lithostitched/api/worldgen/processor/LithostitchedProcessors.java`, `placementmodifier/LithostitchedPlacementModifiers.java`, `densityfunction/LithostitchedDensityFunctions.java`: registering custom worldgen codec types.
- `src/main/java/dev/worldgen/lithostitched/impl/worldgen/structure/AlternateJigsawStructure.java`: a custom jigsaw `StructureType`.
- `src/main/java/dev/worldgen/lithostitched/mixin/common/StructureTemplatePoolMixin.java` and `processor/*PieceMixin.java`: retrofitting processors into vanilla structures.

**Tectonic** (`tectonic`): MIT. `3.1.0+beta2-fabric-26.3` (beta, 2026-11-07). The datapack edition is `3.0.29-datapack`. github.com/Apollounknowndev/tectonic at **`stonecutter`** (the default branch `rewrite-squared` is stale).
- `src/main/java/dev/worldgen/tectonic/worldgen/densityfunction/ConfigNoise.java`, `ConfigConstant.java`, `ConfigClamp.java`: custom `DensityFunction` types whose values come from the config.
- `src/main/java/dev/worldgen/tectonic/worldgen/placementmodifier/HeightStabilizedCount.java`, `TileAcrossChunk.java`: custom `PlacementModifier`s.
- `src/main/java/dev/worldgen/tectonic/platform/fabric/ConfigResourceCondition.java`: a Fabric resource condition that switches datapack JSON on and off from config.
- `src/main/java/dev/worldgen/tectonic/client/gui/PresetSelectorScreen.java`: a world-creation screen.

**TerraBlender** (`terrablender`): LGPL-3.0. `26.3.0.0.9` (beta, 2026-10-02). github.com/Glitchfiend/TerraBlender at `26.3`.
- `common/src/main/java/terrablender/api/Region.java`, `Regions.java`: overworld and nether biome-region API.
- `example/Fabric/src/main/java/terrablender/example/TestRegion1.java`, `TestBiomes.java`, `TestSurfaceRuleData.java`, `datagen/DataGen.java`: a complete working example mod, datagen included.

**Repurposed Structures** (`repurposed-structures-fabric`): LGPL-3.0. `7.8.4+26.3-fabric` (2026-10-11). Modrinth lists no source URL, but the repo is github.com/TelepathicGrunt/RepurposedStructures at `26.3` (`minecraft_version=26.3`, with fabric and neoforge modules).
- `common/src/main/java/com/telepathicgrunt/repurposedstructures/world/structures/CityNetherStructure.java`, `StrongholdEndStructure.java`: custom `Structure` subclasses.
- `common/src/main/java/com/telepathicgrunt/repurposedstructures/world/structures/pieces/PieceLimitedJigsawManager.java`: a jigsaw assembler with per-piece limits.

**The Lost Cities** (`the-lost-cities`): MIT. `26.3-12.0.0-fabric` (2026-10-21). github.com/McJtyMods/LostCities at `26.3_fabric`.
- `src/main/resources/data/lostcities/dimension/lostcity.json` + `dimension_type/lostcity.json`: a datapack-defined custom dimension shipped by a mod.
- `src/main/java/mcjty/lostcities/worldgen/LostCityFeature.java`, `LostCityTerrainFeature.java`: whole-chunk generation run as a `Feature`.
- `src/main/java/mcjty/lostcities/setup/CustomRegistries.java`: custom datapack registries (city styles, buildings).

**BetterEnd** `src/main/java/org/betterx/betterend/world/biome/land/*.java` shows code-defined End biomes through BCLib. Terralith, Incendium and Nullscape are Stardust Labs License: read only, and their public repos are stale (Terralith's GitHub default branch is `1.20`, last commit 2024-05).

### 1.8 Networking-heavy mods

The 26.3 Fabric API calls are `PayloadTypeRegistry.clientboundPlay()` / `serverboundPlay()` (not `playS2C`/`playC2S`) (src: fabric-api `fabric-networking-api-v1/src/main/java/net/fabricmc/fabric/api/networking/v1/PayloadTypeRegistry.java` lines 172-179).

**AppleSkin** (`appleskin`): Unlicense (public domain, copy freely). `3.0.10+mc26.3` (2026-09-17). github.com/squeek502/AppleSkin at `26.3-fabric`.
- `java/squeek/appleskin/network/SyncHandler.java`: registers three S2C payloads with `PayloadTypeRegistry.clientboundPlay().register(ID, CODEC)` and sends them on change with `ServerPlayNetworking.send`. This is the minimal "sync hidden server state to the HUD" pattern.
- `java/squeek/appleskin/network/SaturationSyncPayload.java`: a record payload with a `StreamCodec`.
- `java/squeek/appleskin/network/ClientSyncHandler.java`: the client receiver.

**Construction Wand** `src/main/java/dev/smto/constructionwand/Network.java`: a hand-written `StreamCodec<ByteBuf, List<BlockPos>>` and `StreamCodec.composite(...)` for payloads.

**Tech Reborn** `RebornCore/src/main/java/reborncore/common/network/Packets.java` and `src/main/java/techreborn/packets/Packets.java`: many C2S GUI packets in one place.

**Carry On** (`carry-on`): LGPL-3.0. `2.12.2` (2026-11-09). github.com/Tschipp/CarryOn at `26.3`. Fabric and NeoForge. Picks up blocks and entities and syncs the carried state: `Fabric/src/main/java/tschipp/carryon/platform/FabricPlatformHelper.java` (payload registration). Other paths UNVERIFIED.

**Ledger testmod** `src/testmod/kotlin/com/github/quiltservertools/ledger/testmod/commands/packet/HandshakeC2SPacket.kt`: a versioned handshake protocol between client and server.

Others on 26.3 but not inspected: Plasmo Voice (LGPL-3.0, `plasmo-voice`, a Kotlin multi-platform build with a separate UDP voice protocol), Do a Barrel Roll (GPL-3.0, codeberg.org/enjarai/do-a-barrel-roll), Simple Voice Chat (ARR).

### 1.9 Config screens

**YetAnotherConfigLib** (`yacl`): LGPL-3.0-or-later. `3.9.7+26.3-fabric`. github.com/isXander/YetAnotherConfigLib at `main` (Stonecutter: `mc("26.1")`, `mc("26.2")`, `mc("26.3")`). The `port/26.3` branch is older.
- `src/testmod/java/dev/isxander/yacl3/test/GuiTest.java`, `ConfigTest.java`, `AutogenConfigTest.java`: builder API and annotation-driven autogen.

**Zoomify** (`zoomify`): LGPL-3.0. `2.17.0+mc26.3`. github.com/isXander/Zoomify at `main` (Stonecutter `versions/versions.json`: 26.1, 26.2, 26.3). Kotlin.
- `src/main/kotlin/dev/isxander/zoomify/config/ZoomifySettings.kt`: a real YACL config with presets (`Presets.kt`) and a live demo image (`config/demo/ZoomDemoImageRenderer.kt`).
- `src/fabric/kotlin/dev.isxander.zoomify.fabric/integrations/ModMenuIntegration.kt`.

**MidnightLib** (`midnightlib`): MIT. `1.10.0+26.3-fabric`. github.com/TeamMidnightDust/MidnightLib at `multiversion`. One annotation-based config class, so no screen code to write.
- `src/main/java/eu/midnightdust/lib/config/MidnightConfig.java`; `src/test/java/eu/midnightdust/test/config/MidnightConfigExample.java`.

**Cloth Config** (`cloth-config`): LGPL-3.0. github.com/shedaniel/ClothConfig at `v26.3`: `common/src/main/java/me/shedaniel/autoconfig/example/ExampleConfig.java`. **Enchantments-Plus** `config/ClothConfigIntegration.java` + `ModMenuIntegration.java` show real-world Cloth with Mod Menu. Mod Menu (`modmenu`, MIT, `26.3` branch) is the entrypoint every config screen hangs off.

### 1.10 HUD overlays and client-only QoL

26.x client rename to know: `GuiGraphics` is now **`GuiGraphicsExtractor`** and widget `render(...)` is `extractRenderState(...)` (src: client `net/minecraft/client/gui/GuiGraphicsExtractor.java`, `components/AbstractWidget.java`). MidnightLib's `stonecutter.gradle.kts` replacement list is a handy rename table (`GuiGraphics`→`GuiGraphicsExtractor`, `renderContent`→`extractContent`, `renderListSeparators`→`extractListSeparators`, `ResourceLocation`→`Identifier` at >=1.21.11-rc2).

- **AppleSkin** (Unlicense, see 1.8): `java/squeek/appleskin/client/HUDOverlayHandler.java` (food and saturation overlay, through `mixin/HudMixin.java`), `client/TooltipOverlayHandler.java` + `mixin/ClientTooltipComponentMixin.java` (custom tooltip component), `client/DebugInfoHudEntry.java` (an F3 debug entry), `client/REITooltipPlugin.java`.
- **Tech Reborn** `RebornCore/src/client/java/reborncore/client/RebornCoreClient.java`: `HudElementRegistry.addLast(Identifier, HudElement)`. Enchancement's `EnchancementClient.java` uses `attachElementBefore(VanillaHudElements.HELD_ITEM_TOOLTIP, ...)`/`attachElementAfter(VanillaHudElements.CROSSHAIR, ...)`. Farmer's Delight `client/gui/HUDOverlays.java` still draws through a mixin, and has the `HudElementRegistry` calls commented out "until AppleSkin swaps".
- **BetterF3** (`betterf3`): MIT. `20.0.0`. github.com/TreyRuffy/BetterF3 at `architectury/26.3`. `common/src/main/java/me/cominixo/betterf3/modules/BaseModule.java` + `CoordsModule.java` and others (modular debug-screen lines), `common/.../mixin/DebugScreenEntryListMixin.java` (hooks the 26.x debug screen entry list), and unit tests (`modules/BaseModuleCachingTest.java`).
- **Shulker Box Tooltip** (`shulkerboxtooltip`): MIT. `5.4.3+26.3-fabric`. github.com/MisterPeModder/ShulkerBoxTooltip at `26.3.x`. `fabric/src/main/java/com/misterpemodder/shulkerboxtooltip/fabric/ShulkerBoxTooltipClientImpl.java` (`ClientTooltipComponentCallback`), `common/.../impl/renderer/VanillaPreviewRenderer.java`, `api/renderer/PreviewRenderer.java` (a public API other mods implement).
- **Status Effect Bars** (`status-effect-bars`): LGPL-3.0. `1.0.12` is listed for 26.3 but was published 2026-06-18, and `main` builds against `minecraft_version=26.2`. A tiny example of a Hud mixin plus a Mod Menu/config screen: `src/main/java/io/github/a5b84/statuseffectbars/mixin/HudMixin.java`, `StatusEffectBarRenderer.java`.

### 1.11 Shaders and rendering-heavy mods

- **Iris** (`iris`): LGPL-3.0. `1.11.7+26.3-fabric`. github.com/IrisShaders/Iris at `26.3` (there is also `26.3-new`; the default branch is `26.1`). Paths: `common/src/main/java/net/irisshaders/iris/pipeline/IrisRenderingPipeline.java`, `pipeline/IrisPipelines.java` (mapping vanilla `RenderPipeline`s to shader programs), `mixin/MixinLevelRenderer.java`, `mixin/MixinRenderPipeline.java`/`MixinGlRenderPipeline.java` (hooks into the 26.x pipeline and GL backend), `shaderpack/ShaderPack.java`.
- **Continuity** (`continuity`): LGPL-3.0. `3.0.1+26.3`. github.com/PepperCode1/Continuity at `26.3/dev`. Connected textures through Fabric's model API: `src/main/java/me/pepperbell/continuity/client/model/CtmBlockStateModel.java` (wraps `BlockStateModel`), `model/QuadProcessors.java`, `model/EmissiveItemGeometry.java`, `resource/CtmResourceReloader.java`, `mixin/SpriteLoaderMixin.java` (adding sprites to atlases).
- **ImmediatelyFast** (`immediatelyfast`): LGPL-3.0-or-later. `1.17.1+26.3-fabric`. github.com/RaphiMC/ImmediatelyFast at `26.3`. Paths: `common/src/main/java/net/raphimc/immediatelyfast/feature/map_atlas_generation/MapAtlas.java`, `feature/sign_text_buffering/SignAtlasRenderTarget.java` (render targets), `feature/resource_pack_conflict_handling/CoreShaderBlacklist.java`.
- **Mutant Monsters** `client/renderer/rendertype/ModRenderTypes.java` (1.3) is the smallest custom-pipeline example. Debugify `src/client/java/dev/isxander/debugify/client/mixins/basic/mc165305/RenderPipelinesMixin.java` patches a vanilla pipeline.
- Sodium (`sodium`) is LicenseRef-Polyform-Shield-1.0.0: read only, no copying, no forks. Entity Culling and LambDynamicLights use custom licenses: read only. Entity Model Features (`entity-model-features`) is LGPL-3.0 (not inspected).

### 1.12 Server-side-only mods (Polymer)

All by Patbox (`eu.pb4.*`), LGPL-3.0 unless noted, on `main`/`master` with `minecraft_version=26.3-rc-1` or `26.3` and a 26.3 Fabric file on Modrinth. Polymer itself: github.com/Patbox/polymer at `dev/26.3`, `0.18.7+26.3` (2026-11-03).
- **Polymer testmod** `polymer-core/src/testmod/java/eu/pb4/polymertest/`: `TestBlock.java` (`extends Block implements PolymerBlock`), `TestEntity.java` (`extends Creeper implements PolymerEntity`), `TestBowItem.java` (`PolymerItem`), `AnimatedBlock.java`, `TestEnchantmentEntityEffect.java`, `TestMod.java` (registration of everything). Docs: `docs/polymer-core/`, `docs/polymer-virtual-entity/`, `docs/polymer-resource-pack/`.
- **Universal Graves** (`universal-graves`): `src/main/java/eu/pb4/graves/registry/AbstractGraveBlock.java` (`PolymerBlock` + `BlockWithElementHolder`), `registry/GraveCompassItem.java` (`PolymerItem`), `ui/GraveGui.java`/`PagedGui.java` (server-side chest GUIs with sgui), `model/GraveModel.java` (display-entity models), `compat/TrinketsUpdatedCompat.java`.
- **PolyFactory** (1.2): `ModInit.java` calls `PolymerResourcePackUtils.addModAssets(ID)` and `markAsRequired()` (a server resource pack for custom models). `ui/SimpleInputGui.java` uses sgui's `AnvilInputGui`.
- **Styled Chat** (`styled-chat`): `src/main/java/eu/pb4/styledchat/StyledChatUtils.java`, `parser/`, `config/` (chat formatting with Placeholder API and Text Placeholder API).
- **Get Off My Lawn ReServed** (`goml-reserved`, MIT): land claims (paths not inspected).
- **Sit!** (`sit`): Modrinth says LGPL, but there is no LICENSE file in the repo (`Oth3r/Sit` `master`, `minecraft_version=26.3`). `src/main/java/one/oth3r/sit/utl/Events.java` registers payloads for an optional client part.

### 1.13 Commands and admin utilities

- **Essential Commands** (`essential-commands`): MIT. `0.42.0-mc26.3`. github.com/John-Paul-R/Essential-Commands at `26.x` (`minecraft_version=26.3`). `src/main/java/com/fibermc/essentialcommands/EssentialCommandRegistry.java` (registering dozens of Brigadier commands), `ECPerms.java` (uses the **vanilla 26.x permission system** `net.minecraft.server.permissions.Permission`/`PermissionLevel`, src: common `net/minecraft/server/permissions/`), `commands/TeleportAcceptCommand.java`, `playerdata/PlayerDataManager.java` (per-player persistent data), `teleportation/PlayerTeleporter.java`.
- **Ledger** (`ledger`): LGPL-3.0, Kotlin. `1.3.25`. github.com/QuiltServerTools/Ledger at `master` (`libs.versions.toml minecraft="26.3"`). `src/main/kotlin/com/github/quiltservertools/ledger/commands/arguments/SearchParamArgument.kt` (a custom Brigadier argument parser), `commands/subcommands/RestoreCommand.kt`, `database/DatabaseManager.kt` (an SQL backend), `listeners/BlockEventListener.kt` (event hooks), `api/CommandExtension.kt`.
- **LuckPerms** (`luckperms`): MIT. `v5.5.85-fabric`. github.com/lucko/LuckPerms at `master` (`minecraftVersion=26.3`). `fabric/src/main/java/me/lucko/luckperms/fabric/listeners/FabricPermissionsListener.java` (bridges into vanilla `LevelBasedPermissionSet`), `listeners/FabricPermissionsApiV1Listener.java` (the fabric-permissions-api bridge), `mixin/CommandsMixin.java`.
- **Carpet** (`carpet`): MIT. `26.3`. github.com/gnembon/fabric-carpet: **use tag `v26.3`**, because `master` is at `26.4-snapshot-8`. Paths (checked on master): `src/main/java/carpet/api/settings/SettingsManager.java` + `Rule.java` (annotation-driven `/carpet` rules), `CarpetSettings.java`, `commands/PlayerCommand.java` + `patches/EntityPlayerMPFake.java` (fake players), `mixins/ServerLevel_tickMixin.java`.
- spark (`spark`, GPL-3.0) for profiling and WorldEdit (`worldedit`, GPL-3.0, `7.4.6`) are on 26.3. Use them, don't rebuild them.

### 1.14 Inventory, storage and backpacks

- **Traveler's Backpack** (`travelersbackpack`): Modrinth says LGPL-3.0, but there is no LICENSE file in the repo. Fabric 26.3 `26.3-11.4.5` (2026-11-14), but **the newest public Fabric source is branch `26.1-fabric`** (`minecraft_version=26.1.2`, commit 2026-11-13). `src/main/java/com/tiviacz/travelersbackpack/inventory/BackpackWrapper.java` (item-backed inventory), `inventory/BackpackContainer.java`, `component/Slots.java`/`Fluids.java` (data components holding inventory and fluids), `client/screens/BackpackScreen.java`, `compat/trinkets/BackpackTrinket.java`, `compat/accessories/TravelersBackpackAccessory.java`.
- **Inventory Sorting** (`inventory-sorting`): MIT. `3.0.1+fabric-26.3`. github.com/meza/Inventory-Sorter at `main` (Stonecutter with `gg.meza.stonecraft`: 26.1, 26.2, 26.3 × fabric and neoforge). Server-authoritative sorting. Notable for **JUnit coverage of sort logic and payload codecs** (`src/test/java/net/kyrptonaught/inventorysorter/network/PacketCodecTest.java`, `sort/SortTypeTest.java`). Also `src/main/java/net/kyrptonaught/inventorysorter/platform/fabric/FabricNetworkingPlatform.java`, `permissions/CommandPermission.java`.
- **Client Sort** (`clientsort`): Apache-2.0. `3.104.1+26.3`. github.com/TerminalMC/ClientSort at `mc26.3`. Client-only sorting by simulated clicks, so it works on vanilla servers: `common/src/main/java/dev/terminalmc/clientsort/mixin/client/AbstractContainerScreenMixin.java`, `CreativeModeInventoryScreenMixin.java`, `fabric/.../client/compat/modmenu/ModMenuImpl.java`.
- **Extended Drawers**, **Storage Drawers** and **Tom's Storage** (1.1, 1.2) for block storage. Mouse Tweaks (`mouse-tweaks`, BSD-3-Clause, `26.3-2.31-fabric`) is not inspected.

### 1.15 Recipe viewer plugins (REI / JEI)

Status on 26.3 Fabric: **REI** `26.3.824+fabric` (MIT), **JEI** `31.17.0.113` (MIT, **beta** on Fabric), **EMI has no 26.x build** (latest 1.1.24 targets 1.21.1).
- **Farmer's Delight Refabricated**: all three side by side. `src/main/java/vectorwing/farmersdelight/integration/rei/CommonREIPlugin.java`, `ServerREIPlugin.java`, `rei/client/ClientREIPlugin.java`, `rei/client/categories/CookingPotCategory.java`; `integration/jei/JEIPlugin.java` + `jei/category/CookingRecipeCategory.java`; `integration/emi/EMIPlugin.java`. Entrypoints in `fabric.mod.json`: `rei_client`, `rei_common`, `jei_mod_plugin`. Note: its `gradle.properties` compiles against `rei_version=26.2.820`, `jei_version=29.5.0.26` and `emi_version=1.1.12+1.21`.
- **Tech Reborn** `src/client/java/techreborn/client/compat/rei/ReiPlugin.java`: `registerCategories`, `registerScreens` and `ExclusionZones` for a custom GUI base class. `client/compat/jei/recipe/transfer/BuiltScreenHandlerTransferInfo.java` is the JEI recipe-transfer handler.
- **Construction Wand** `integrations/recipe/ConstructionWandReiPlugin.java` / `ConstructionWandJeiPlugin.java`: minimal plugins that only add info pages.
- Upstream APIs: REI `api/src/main/java/me/shedaniel/rei/api/client/plugins/REIClientPlugin.java`, `default-plugin/.../plugin/client/DefaultClientPlugin.java` (the vanilla categories, the best example) at branch `26.3`; JEI `Common/src/api/java/mezz/jei/api/IModPlugin.java`, `Library/src/main/java/mezz/jei/library/plugins/vanilla/VanillaPlugin.java` at branch `26.3`.

### 1.16 Mixin-heavy gameplay tweaks

- **Lithium** (`lithium`): LGPL-3.0. `mc26.3-0.26.3-fabric`. github.com/caffeinemc/lithium-fabric at `develop` (`MINECRAFT_COMPILE_VERSION = "26.3"`). `common/src/main/java/net/caffeinemc/mods/lithium/common/config/LithiumConfig.java` + `lithium-mixin-config.md` (per-mixin toggles generated by `components/mixin-config-plugin/`), `mixin/ai/pathing/PathNavigationRegionMixin.java`, `mixin/world/block_entity_ticking/chunk_tickable/LevelMixin.java`, `fabric/src/gametest/java/.../LithiumFabricGameTest.java` (GameTests for the mixins).
- **Debugify** (`debugify`): LGPL-3.0. `26.3.0.0`. github.com/isXander/Debugify at `main` (`libs.versions.toml minecraft="26.3"`). One mixin package per Mojira bug: `src/main/java/dev/isxander/debugify/mixins/basic/mc134110/ChestBlockMixin.java`, `fixes/BugFix.java` + `BugFixData.java` (bug registry), `mixinplugin/MixinPlugin.java` (turning fixes off at runtime).
- **Easy Anvils** (`easy-anvils`): MPL-2.0 (code), assets ARR. `26.3.1`. github.com/Fuzss/easyanvils at `26.3.x`. `Common/src/main/java/fuzs/easyanvils/common/mixin/AnvilMenuMixin.java`, `AnvilBlockMixin.java`, `world/inventory/ModAnvilMenu.java` (replacing a vanilla menu), `Fabric/.../world/inventory/FabricAnvilMenu.java`.
- **Carpet** (1.13) and **Boids** `asm/BoidsMixinPlugin.java` (conditional mixins). Tweakeroo (`tweakeroo`, LGPL-3.0, `0.30.1`) and MiniHUD are on 26.3 but not inspected.

### 1.17 Datagen-heavy mods

The Fabric datagen module is `fabric-data-generation-api-v1` (src: `/tmp/claude-0/mcsrc/fabric-api/fabric-data-generation-api-v1/`).
- **Extended Drawers** (Apache-2.0): a separate `src/datagen` source set, so datagen classes stay out of the shipped jar (`build.gradle.kts`: `sourceSets["datagen"]`, `datagenJar`). Files: `src/datagen/java/io/github/mattidragon/extendeddrawers/datagen/ExtendedDrawersDatagen.java`, `DrawersModelProvider.java`, `DrawersRecipeGenerator.java` + `DrawersRecipeProvider.java` (the RecipeGenerator / RecipeProvider split, which dates from 1.21.2 and still applies), `DrawersBlockLootTableProvider.java`, `DrawersBlockTagProvider.java`, `ReadmeDataProvider.java` (a custom `DataProvider` that writes docs).
- **Farmer's Delight Refabricated** (MIT): `src/main/java/vectorwing/farmersdelight/data/DataGenerators.java` (`implements DataGeneratorEntrypoint`, with an inner `FabricDynamicRegistryProvider` for dynamic registries), `Recipes.java`, `BlockStates.java`, `ItemModels.java`. Entrypoint `"fabric-datagen"` in `fabric.mod.json`.
- **BetterEnd** (MIT): `src/main/java/org/betterx/datagen/betterend/BetterEndDatagen.java`, `EndModelProvider.java`, `recipes/EndEnchantmentProvider.java` (enchantments through datagen), `recipes/EndChestLootTableProvider.java`. Output is committed in `src/main/generated/`.
- **Tech Reborn** (MIT): datagen written in **Groovy** (`src/datagen/groovy/techreborn/datagen/`), with machine-recipe JSON factories (`recipes/machine/MachineRecipeJsonFactory.groovy`) and `TRConventionalTags.groovy`.
- **TerraBlender example** `example/Fabric/src/main/java/terrablender/example/datagen/DataGen.java`: worldgen datagen (biomes, features).

### 1.18 Multiloader and multiversion setups (Stonecutter)

Background and versions: `publishing.md` §8.2 (Stonecutter 0.10, `loom-back-compat`). Real 26.x users found:
- **MidnightLib** (MIT, `multiversion`): one source tree for **1.21.1, 1.21.5, 1.21.8, 1.21.10, 1.21.11, 26.1, 26.2, 26.3** across the obfuscated/unobfuscated boundary. `stonecutter.gradle.kts` (0.9.7 + `dev.kikugie.loom-back-compat` 0.4.2) has the `replacements { string { direction = eval(current.version, ">=26.1"); replace(...) } }` rename table. Per-version values are in `stonecutter.properties.yaml`. `build.fabric.gradle.kts` and `build.neoforge.gradle.kts` are per-loader buildscripts.
- **Lithostitched** / **Tectonic** (MIT): `stonecutter.gradle.kts` (0.9.6 + loom-back-compat 0.3), `stonecutter.properties.toml` with `["26.3"]` tables (`mod.mc_compat`, `deps.fabric_api`, `loomx.loom_version = "1.17-SNAPSHOT"`), `version("fabric", "26.3").buildscript("build.fabric.gradle.kts")`.
- **Zoomify** (LGPL): `stonecutter.gradle.kts` 0.9.8 driven by `versions/versions.json` (`"vcs": "26.3"`, branches 26.1, 26.2, 26.3).
- **YACL** (LGPL): Stonecutter 0.8.2 with `mc("26.1", loaders = listOf("fabric","neoforge"))` style declarations.
- **Inventory Sorting** (MIT): Stonecutter plus the `gg.meza.stonecraft` 1.14.5 convention plugin.
- Non-Stonecutter multiloader layouts (Common/Fabric/NeoForge subprojects): Fuzss mods (Puzzles Lib), StorageDrawers (`common/`, `fabric/`, `neoforge/`, `forge/`), Iris and Lithium (`common/`, `fabric/`, `neoforge/`), Tom's Storage (`Fabric/src/platform-shared`). Architectury: BetterF3 (`architectury/26.3`).

---

## 2. Datapack side

### 2.1 Well-made open-source datapacks on 26.3

| Pack | License | 26.3 status (Modrinth datapack file) | Source | Read for |
|---|---|---|---|---|
| **Gamemode 4** (`gm4-*` modules) | GPL-3.0 (read only) | `gm4/plugins/manifest.py`: `SUPPORTED_GAME_VERSIONS = ["26.3"]`; e.g. `gm4-guidebook` 3.4.1 (2026-10-18) | github.com/Gamemode4Dev/GM4_Datapacks `master` (2026-10-18) | ~100 modules built with beet (`beet-release.yaml`, per-module `beet.yaml`). `base/data/load/` vendors **Lantern Load**. Modules ship **PackTest tests** at `data/<module>/test/*.mcfunction` (e.g. `gm4_bat_grenades/data/gm4_bat_grenades/test/boom.mcfunction`). Shared libs: `lib_machines`, `lib_lore`, `lib_trees`, `lib_player_death`, `lib_forceload`. Custom items: `gm4_boots_of_ostara/` |
| **Enchantments Encore** (`enchantments-encore`) | GPL-3.0-or-later (read only) | datapack `6.1` (2026-11-03); `pack.mcmeta` `min_format`/`max_format` `121.0` | github.com/Explorers-Eden/Enchantments-Encore `main` | 150 files in `data/enchantencore/enchantment/` (data-driven enchantments), 270 functions, `item_modifier/`, `predicate/`, and the 26.x **`villager_trade/`** registry (vanilla has `data/minecraft/villager_trade` and `trade_set`) |
| **Sleep** (`sleep`) | MIT | datapack `v4.4.0.0` (2026-09-28); `pack.mcmeta` `"min_format": [121, 0]`, `"max_format": [999, 0]` | codeberg.org/jodeks-datapacks/sleep `main` | A small, clean function pack: `data/minecraft/tags/function/{load,tick}.json`, `data/sleep/function/app/`, `config/`, `uninstall.mcfunction`, Forgejo release workflow |
| **Armor Stand Arms** (`armor-stand-arms`) | MIT | `v2.7.1` (2026-10-14) | codeberg.org/jodeks-datapacks/armor-stand-arms | An interaction triggered by an advancement (`data/armor_stand_arms/advancement/right_click.json`), same layout as Sleep |
| **Tectonic** datapack (`tectonic`) | MIT | `3.0.29-datapack` (2026-09-22) | github.com/Apollounknowndev/tectonic `stonecutter` | Density functions and noise settings as pure JSON (the mod adds config on top) |
| **Bookshelf** example packs | MPL-2.0 | see 2.2 | github.com/mcbookshelf/bookshelf `examples/` | How to consume the library |
| Terralith / Incendium / Nullscape | Stardust Labs License (read only) | `2.6.5+26.3`, `5.5.4+26.3`, `2.0.1+26.3` | GitHub repos are stale (Terralith default branch `1.20`, 2024-05) | Unpack the Modrinth zip to study 26.3 worldgen JSON. Don't copy. |
| Hopo Better Mineshaft / Underwater Ruins | LGPL-3.0-or-later | `1.3.8-datapack` (2026-09-29) | Modrinth has no source URL (UNVERIFIED repo) | Structure and jigsaw replacement packs |

### 2.2 Datapack libraries and conventions

| Library | License | 26.3 status | Notes |
|---|---|---|---|
| **Lantern Load** (github.com/LanternMC/load, `master`, last commit 2025-12-14) | ISC-style permissive ("Permission to use, copy, modify, and/or distribute ... is hereby granted") | Works on 26.3: it is only tags plus one function, using the 1.21+ singular folders (README: "for Minecraft 1.21 or higher") | The load-order convention. `data/minecraft/tags/function/load.json` → `#load:_private/load` → `#load:_private/init` (resets the `load.status` scoreboard), then `#load:pre_load`, `#load:load`, `#load:post_load` (each `"required": false`). Packs add their init to `#load:load` and set `scoreboard players set <pack> load.status <version>` so dependents can check it. Vendor it by copying the `data/load/` tree (GM4 does this in `base/`). |
| **Smithed Libraries** (github.com/Smithed-MC/Libraries, `main`, tag `v0.19.0`, last commit 2026-11-08) | MIT | `beet.yaml`: `minecraft: "26.3"`. CHANGELOG v0.16.6 (2026-10-02): "Update loot, predicate and advancement JSON to the 26.3 formats" | Packs under `src/smithed_libraries/packs/`: `crafter` (custom crafting table with its own recipe format), `custom-block`, `actionbar` and `title` (priority queues so packs don't overwrite each other), `interaction`, `prevent-aggression`. Built with beet. Not on Modrinth: distributed through smithed.net (download path UNVERIFIED). Smithed "Weld" (pack merger) repo not found under `Smithed-MC/Weld`: UNVERIFIED. |
| **Bookshelf** (github.com/mcbookshelf/bookshelf, `master`, tag `v4.2.1`) | MPL-2.0 | Modrinth `bookshelf-suite` 4.2.1 (2026-09-19) lists only `26.3`. Individual modules are on Modrinth as `bookshelf-<module>` (`bookshelf-raycast`, `bookshelf-block`, `bookshelf-math`, `bookshelf-view`, ...) | Modules (`modules/bs.*`): animation, bitwise, block, collection, color, dump, environment, fsm, generation, health, hitbox, id, interaction, link, log, math, move, position, random, raycast, schedule, sidebar, spline, string, time, tree, vector, view, xp. Its beet plugins (`modules/config.json` pipeline: `include_deps`, `gen_load`, ...) also wire up load. Has a `.spyglassrc.json`. |
| **GM4 shared libs** (`lib_*` in GM4_Datapacks) | GPL-3.0 | 26.3 (see 2.1) | Can be consumed as packs. Copying code means GPL. |
| **PackTest** (`packtest`, misode) | MIT | `2.8.0` for 26.3 (2026-10-17) | Already adopted (`tools/packtest.py`). GM4 is the largest real user of `data/<ns>/test/*.mcfunction`. |

---

## 3. Dev tools to use instead of writing our own

| Tool | Status (checked 2026-11-14) | Use it for |
|---|---|---|
| **Fabric template generator** fabricmc.net/develop/template | Live. Source `FabricMC/fabricmc.net` `scripts/src/lib/template/` (last commit 2026-11-07): `minecraftIsUnobfuscated()` is true for major >= 26 and then emits plugin `net.fabricmc.fabric-loom` + `implementation`. Pre-26 versions get `net.fabricmc.fabric-loom-remap` + `modImplementation`. | Starting a new mod skeleton |
| **Fabric CLI** `npx @fabricmc/cli` (npm `@fabricmc/cli` 0.1.500, 2026-11-07; MIT; Node >= 22) | `fabric init [dir] -n <name> -m <modid> -p <package> -v <mcversion> -y`; `fabric versions` | The same template, scriptable for an agent |
| **Stonecutter template** `stonecutter-versioning/stonecutter-template-fabric` | See `publishing.md` §8.2 | Multiversion |
| **Minecraft Development** IntelliJ plugin (JetBrains id 8327, github.com/minecraft-dev/MinecraftDev `dev`, commit 2026-11-14) | `2026.3-1.9.1` (2026-11-10) for IDEA 263.*, also builds for 2026.1 and 2026.2 | Mixin target completion and validation, access widener support |
| **mcsrc.dev** | Up (HTTP 200, SPA titled "mcsrc") | Browse decompiled vanilla in a browser. Right-click copies mixin targets (`mixins.md`). Locally we have `/tmp/claude-0/mcsrc` and `tools/mcsrc.py`. |
| **Linkie** (linkie.shedaniel.dev; `linkie/linkie-core` last commit 2024-02) | The site answers. Mostly irrelevant on 26.x because names are Mojang's and there is no Yarn. Still useful for mapping **old** Yarn/intermediary names when porting 1.21.x code (`porting-26x.md` has a cheat sheet). | Porting from Yarn-era sources only |
| **Blockbench** (github.com/JannisX11/blockbench, tag `v5.2.2`, last commit 2026-10-18; web.blockbench.net up) | Current | Java block/item models, entity models (export a Java `LayerDefinition` for vanilla models, or GeckoLib `.geo.json` + `.animation.json` with the GeckoLib plugin). Don't hand-write model JSON for anything non-trivial. GeckoLib plugin version for 26.x: UNVERIFIED. |
| **Spyglass** (VS Code "Data-pack Helper Plus"; github.com/SpyglassMC/Spyglass tag `v2026.10.13+ddffc6`) | 26.3 supported (`datapacks.md`) | Datapack linting and completion. Schemas in `SpyglassMC/vanilla-mcdoc`. |
| **misode generators** misode.github.io (repo last commit 2026-11-09) | 26.3 and 26.4 (`datapacks.md`) | Generating worldgen, loot, predicate, recipe and enchantment JSON. Changelog and version diff browser. `misode/mcmeta` has per-version data branches (`<version>-summary`, `-registries`, `-diff`). |
| **beet** (tag `v0.121.2`, 2026-10-22) / **bolt** (tag `v0.49.2`, last commit 2025-12-08) | beet: `LATEST_MINECRAFT_VERSION = "26.3"` (`datapacks.md`). bolt: no 2026 commits, so 26.3-specific syntax support is UNVERIFIED (bolt is mostly version-agnostic). | Building packs programmatically (Smithed, Bookshelf and GM4 all use beet) |
| **PackTest** | Adopted | Datapack tests |
| **PackSquash** (github.com/ComunidadAylas/PackSquash, tag `v0.4.1`, last commit 2026-10-19; AGPL-3.0, used as a tool so no license impact) | Source understands the 26.2-era `min_format`/`max_format` pack metadata (`packages/packsquash/src/pack_metadata/manifest.rs`). Explicit 26.3 (resource 97.1) support: UNVERIFIED. | Shrinking resource packs before release |
| **NBT tools** | See `datapacks.md` (Misode's VS Code NBT Viewer; `nbtlib` for Python) | Reading `.nbt` structures and `level.dat` |
| **Modrinth CLIs** | `packwiz` (last commit 2026-10-14, no tags), `ferium` (tag `v4.7.1`, last commit 2026-05-16), `mrpack-install` (tag `v0.23.0`, last commit 2026-11-12) | Building or installing test modpacks and servers (`mrpack-install` installs a `.mrpack` into a server dir). Publishing goes through Minotaur or `mod-publish-plugin` (`publishing.md`). |
| **Fabric meta** `meta.fabricmc.net/v2/versions/game` | Stable list starts `26.3, 26.2, 26.1.2` | Picking loader and game versions in scripts |

---

## 4. Saturated ideas (leading 26.3 projects)

From Modrinth search (`versions:26.3`, `categories:fabric` or `datapack`) plus a per-project check that a Fabric 26.3 file exists. Downloads are all-time totals on Modrinth. A new project in one of these spaces needs a clear angle the leader lacks. Pitch "what ours adds" to the owner first (B8).

| Concept | Leader on 26.3 (Fabric file verified) | Also present |
|---|---|---|
| Minimap / world map | Xaero's Minimap + World Map (ARR, 121M / 106M) | JourneyMap (ARR), VoxelMap-Updated (ARR), BlueMap web map (MIT, server), Open Parties and Claims (LGPL, claims) |
| Backpacks | Traveler's Backpack (LGPL, 22M) | Packed Up, yyz's backpack. Sophisticated Backpacks has no Fabric 26.3 build. |
| Waystones / teleport network | Waystones (ARR, 28M) | KawaMood's Waystones datapack (CC-BY-NC-SA), Server-Side Waystones (MIT). Wraith Waystones stops at 26.1. |
| Inventory sorting | Inventory Profiles Next (AGPL, 42M), Client Sort (Apache, 6.5M), Inventory Sorting (MIT, 3.7M) | Mouse Tweaks (BSD, 65M) for drag and scroll, Item Scroller (LGPL) |
| Tree felling | FallingTree (LGPL, 13M) | Tree Vein Miner (AGPL), Chopt (MIT) |
| Vein mining | VeinMiner (AGPL, 104M) + VeinMiner Enchantment (LGPL, 18M) | Ore Vein Miner (AGPL) |
| Villager tweaks | Villager Names (ARR, 13M), VillagersPlus (GPL), Trading Post (MPL, 9.6M), Tom's Trading Network (MIT) | Easy Villagers has no 26.3 build |
| Graves / death | Universal Graves (LGPL, 5.2M, server-side) | Gravestones by Pneumono (MIT), Where Did I Die? Corpse has no 26.3 build. |
| Zoom | Zoomify (LGPL, 73M) | Just Zoom, Ok Zoomer |
| Block/entity info tooltip (WAILA) | Jade (CC-BY-NC-SA, 74M) | WTHIT (CC-BY-NC-SA) |
| Food / saturation HUD | AppleSkin (Unlicense, 99M) | |
| Armor / durability HUD | uku's Armor HUD (MIT, 10M) | Durability Tooltip (ARR, 15M) |
| Shulker box preview | Shulker Box Tooltip (MIT, 41M) | |
| Recipe viewer | JEI (MIT, 86M, Fabric beta), REI (MIT, 27M) | RRV (MIT). EMI has no 26.x build. |
| Voice chat | Simple Voice Chat (ARR, 77M) | Plasmo Voice (LGPL) |
| Dynamic lights | LambDynamicLights (custom license, 64M) | |
| Right-click harvest | RightClickHarvest (MIT, 13M) | |
| Fast leaf decay | Leaves Be Gone (MPL, 14M) | |
| Double doors | Double Doors (ARR, 9.5M) | Open Together (MPL) |
| Pick up blocks / mobs | Carry On (LGPL, 28M) | |
| Sitting | Sit! (LGPL, 0.4M) | GM4 Chairs datapack |
| Sleep voting / one-player sleep | Sleep datapack (MIT) | Better Server Sleep |
| Chat heads / chat QoL | Chat Heads (MPL, 53M) | No Chat Reports (WTFPL, 62M) |
| Freecam | Freecam (MIT, 23M) | |
| Light level overlay | MiniHUD (LGPL, 6.9M), Lighty (Apache, 4M) | |
| Elevators | ElevatorMod (MIT, 2.4M, beta on 26.3) | Moving Elevators (ARR) |
| Building / schematics | Litematica (LGPL, 28M), WorldEdit (GPL, 12M), Construction Wand (MIT) | Axiom (ARR) |
| Pre-generation / profiling | Chunky (GPL, 20M), spark (GPL, 24M) | |
| Mob health bars / damage numbers | YDM's MobHealthBar (ARR, 2M), JeremySeq's Damage Indicator (MIT) | |
| Cooking / farming expansion | Farmer's Delight Refabricated (MIT, 17M) and a large add-on ecosystem | |
| Overworld terrain | Tectonic (MIT, 18M), Terralith (Stardust, 25M) | Lithostitched as the shared library |
| Seasons | Serene Seasons (ARR, beta on 26.3) | Fabric Seasons stops at 1.21.1 |
| Storage networks | Tom's Simple Storage (MIT, 21M) | Extended Drawers (Apache), Storage Drawers (MIT) |

Gaps worth noting (no Fabric 26.3 build at the time of checking): EMI, Create (Fabric), Modern Industrialization, Cobblemon, Sophisticated Backpacks, Easy Villagers, Corpse, Fabric Seasons, owo-lib (see `libraries.md`), Wraith Waystones (26.1 only), Regions Unexplored (26.1 beta only), Traverse (26.2 beta only), Artifacts (26.1 only).

---

## 5. Unverified or not inspected

- Not cloned, so no file paths: Tweakeroo, MiniHUD, Mouse Tweaks, GOML, Do a Barrel Roll (Codeberg), Plasmo Voice MC modules, Entity Model Features, Carry On beyond the platform helper.
- Carpet paths were checked on `master` (26.4 snapshot), not on tag `v26.3`.
- The GeckoLib Blockbench plugin version for 26.x, the Smithed download and Weld locations, the Hopo packs' source repos, explicit 26.3 support in PackSquash and bolt.
- Download counts and versions change daily. Re-run `tools/prior-art.py` before acting on section 4.
