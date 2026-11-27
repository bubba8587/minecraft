# Porting Fabric mods: 1.21.x -> 26.1 -> 26.2 -> 26.3 (+ 26.4 snapshots)

Scope: breaking changes for Fabric modders across the 26.x line, the Fabric API module and class renames, and a Yarn -> Mojang name cheat sheet. Verified 2026-11-14 against the MC 26.3 decompiled source (`/tmp/claude-0/mcsrc/common`, `/client`), Fabric API 0.166.2+26.3 (`/tmp/claude-0/mcsrc/fabric-api`), fabric-docs (`develop/porting`, `versions/26.1.2/develop/porting/*`), the Fabric blog posts for 26.1 (2026-03-14), 26.2 (2026-06-15) and 26.3 (2026-09-15), the NeoForged migration primers (docs.neoforged.net/primer/docs/26.1, 26.2, 26.3), and minecraft.wiki "Java Edition 26.4" (snapshots 1-7).

Tags used below: **[src]** means the class or member was checked to exist in the 26.3 source. **[blog]** means Fabric blog. **[primer]** means NeoForged primer (vanilla changes only). Anything else not checked is marked UNVERIFIED.

See also `toolchain.md` (build script changes in detail) and `mixins.md`.

---

## 0. TL;DR porting order

1. **Still on Yarn (1.21.x)?** First migrate names on the *old* version, with `./gradlew migrateMappings --mappings "net.minecraft:mappings:<old mc>"` (Loom 1.13+, remap plugin) or the Ravel IntelliJ plugin. Neither tool renames Fabric API symbols or mixin strings.
2. Build script (toolchain.md section 3): use plugin id `net.fabricmc.fabric-loom`, delete `mappings`, change `mod*` configurations to plain configurations, `remapJar` to `jar`, Java 25, Gradle 9.x, Loom 1.19.x, Loader 0.20.1, FAPI `0.166.2+26.3`. Change the class tweaker header `named` to `official`. Remove mixin refmaps.
3. Replace every dependency with a 26.x build. 1.21.x jars cannot load, not even as `compileOnly`.
4. Fix the Fabric API renames (section 2). Use the IntelliJ migration map `fabric-api-26-1-migration-map.xml` linked from fabric-docs `versions/26.1.2/develop/porting/fabric-api.md`.
5. Fix vanilla changes version by version (sections 3-5). Recompile, then run the game and let `defaultRequire: 1` tell you which mixins lost their targets.
6. Re-run datagen, because many data formats changed (loot, features, recipes, trades, brewing, fuel).

---

## 1. Fabric API module changes (1.21.11 -> 26.3)

Diff of the `fabric-api` aggregate POM, `0.141.6+1.21.11` vs `0.166.2+26.3` (maven.fabricmc.net). Module ids changed because modules were renamed to Mojang vocabulary, so **`fabric.mod.json` `depends` on module ids and `fabricApi.module(...)` calls must be updated**.

| 1.21.11 module | 26.3 module | Main API package now |
|---|---|---|
| `fabric-key-binding-api-v1` | `fabric-key-mapping-api-v1` | `net.fabricmc.fabric.api.client.keymapping.v1` (`KeyMappingHelper.registerKeyMapping`) |
| `fabric-screen-handler-api-v1` | `fabric-menu-api-v1` | `net.fabricmc.fabric.api.menu.v1` (`ExtendedMenuType`, `ExtendedMenuProvider`, `FabricMenuProvider`) |
| `fabric-item-group-api-v1` | `fabric-creative-tab-api-v1` | `net.fabricmc.fabric.api.creativetab.v1` (`FabricCreativeModeTab`, `CreativeModeTabEvents`, `FabricCreativeModeTabOutput`); client: `...client.creativetab.v1.FabricCreativeModeInventoryScreen` |
| `fabric-block-view-api-v2` | `fabric-block-getter-api-v2` | `net.fabricmc.fabric.api.blockgetter.v2` (`FabricBlockGetter`, `RenderDataBlockEntity`) |
| - | `fabric-advancement-api-v1` (new) | `api.advancement.v1` (`AdvancementEvents`, `FabricAdvancementBuilder`, `AdvancementSource`) |
| - | `fabric-debug-api-v1` (new) | `api.debug.v1` (`EntityDebugSubscriptionRegistry`, `DebugValueFactory`); client `api.client.debug.v1` (`DebugRendererRegistry`, `ClientDebugSubscriptionRegistry`) |
| - | `fabric-permission-api-v1` (new) | `api.permission.v1` (`PermissionNode`, `PermissionContext`, `PermissionPredicates`, `PermissionEvents`) |
| `fabric-convention-tags-v1`, `fabric-loot-api-v2` (deprecated in 1.21.11) | **removed** in 26.1 [blog] | use `fabric-convention-tags-v2`, `fabric-loot-api-v3` |
| `fabric-resource-loader-v0` | still in `fabric-api-deprecated` (lifecycle `deprecated`) | use `fabric-resource-loader-v1` (`net.fabricmc.fabric.api.resource.v1.ResourceLoader`, `DataResourceLoader`) |

The `fabric` mod id (an alias for `fabric-api`) was removed in 26.1. Depend on `"fabric-api"` [blog 26.1].

The full 26.3 module list (47 POM entries): `fabric-advancement-api-v1, fabric-api-base, fabric-api-lookup-api-v1, fabric-biome-api-v1, fabric-block-api-v1, fabric-block-getter-api-v2, fabric-client-gametest-api-v1, fabric-command-api-v2, fabric-content-registries-v0, fabric-convention-tags-v2, fabric-crash-report-info-v1, fabric-creative-tab-api-v1, fabric-data-attachment-api-v1, fabric-data-generation-api-v1, fabric-debug-api-v1, fabric-dimensions-v1, fabric-entity-events-v1, fabric-events-interaction-v0, fabric-game-rule-api-v1, fabric-gametest-api-v1, fabric-item-api-v1, fabric-key-mapping-api-v1, fabric-lifecycle-events-v1, fabric-loot-api-v3, fabric-menu-api-v1, fabric-message-api-v1, fabric-model-loading-api-v1, fabric-networking-api-v1, fabric-object-builder-api-v1, fabric-particles-v1, fabric-permission-api-v1, fabric-recipe-api-v1, fabric-registry-sync-v0, fabric-renderer-api-v1, fabric-renderer-indigo, fabric-rendering-fluids-v1, fabric-rendering-v1, fabric-resource-conditions-api-v1, fabric-resource-loader-v1, fabric-screen-api-v1, fabric-serialization-api-v1, fabric-sound-api-v1, fabric-tag-api-v1, fabric-transfer-api-v1, fabric-transitive-access-wideners-v1` plus `fabric-api-deprecated` (`fabric-resource-loader-v0`).

---

## 2. Fabric API class and member renames (done in 26.1)

From fabric-docs `versions/26.1.2/develop/porting/fabric-api.md`; spot-checked against fapi 26.3. Most of these follow one rule: **Yarn vocabulary became Mojang vocabulary.** The word swaps are World to Level, ScreenHandler to Menu, ItemGroup to CreativeModeTab, KeyBinding to KeyMapping, C2S/S2C to Serverbound/Clientbound, PacketCodec to StreamCodec, Settings to Properties, RenderLayer to ChunkSectionLayer, and "Feature renderer" to RenderLayer.

### Events / lifecycle
| Old (1.21.x) | New (26.x) |
|---|---|
| `ServerWorldEvents` (`LOAD`/`UNLOAD`, `onWorldLoad`) | `ServerLevelEvents` (`onLevelLoad`, `onLevelUnload`) |
| `ServerTickEvents.START_WORLD_TICK/END_WORLD_TICK` | `START_LEVEL_TICK/END_LEVEL_TICK` (`StartLevelTick`, `EndLevelTick`) |
| `ClientTickEvents.START_WORLD_TICK/END_WORLD_TICK` | `START_LEVEL_TICK/END_LEVEL_TICK` |
| `ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE` | `ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE` (`afterLevelChange`) |
| `ServerEntityWorldChangeEvents` (`AFTER_PLAYER_CHANGE_WORLD`, ...) | `ServerEntityLevelChangeEvents` (`AFTER_PLAYER_CHANGE_LEVEL`, `afterChangeLevel`) |
| `ServerChunkEvents.CHUNK_LEVEL_TYPE_CHANGE` | `FULL_CHUNK_STATUS_CHANGE` (`FullChunkStatusChange#onFullChunkStatusChange`) |
| `ItemGroupEvents.modifyEntriesEvent(key)` / `MODIFY_ENTRIES_ALL` | `CreativeModeTabEvents.modifyOutputEvent(key)` / `MODIFY_OUTPUT_ALL`; `entries.addAfter/addBefore` becomes `output.insertAfter/insertBefore` |
| `FuelRegistryEvents` | `FuelValueEvents` (26.1). **Then removed in 26.3**, see section 5 |

### Networking
| Old | New |
|---|---|
| `PayloadTypeRegistry.playC2S()/playS2C()/configurationC2S()/configurationS2C()` | `serverboundPlay()/clientboundPlay()/serverboundConfiguration()/clientboundConfiguration()` [src] |
| `PacketByteBufs` (`EMPTY_PACKET_BYTE_BUF`) | `FriendlyByteBufs` (`EMPTY_FRIENDLY_BYTE_BUF`) |
| `ServerPlayNetworking.createS2CPacket` / `ClientPlayNetworking.createC2SPacket` | `createClientboundPacket` / `createServerboundPacket` |
| `S2CPlayChannelEvents`, `S2CConfigurationChannelEvents` | `ClientboundPlayChannelEvents`, `ClientboundConfigurationChannelEvents` |
| `C2SPlayChannelEvents`, `C2SConfigurationChannelEvents` (client) | `ServerboundPlayChannelEvents`, `ServerboundConfigurationChannelEvents` |
| `*ConfigurationNetworking.Context#networkHandler` | `packetListener` |
| `FabricServerConfigurationNetworkHandler` | `FabricServerConfigurationPacketListenerImpl` |
| `PlayerLookup.world(...)` | `PlayerLookup.level(...)` |

### Registries, items, blocks, content
| Old | New |
|---|---|
| `FabricItemGroup.builder()` | `FabricCreativeModeTab.builder()` |
| `FabricItem.Settings` / `FabricItem#getRecipeRemainder` | `FabricItem.Properties` / `getCraftingRemainder` |
| `CustomDamageHandler#damage` | `hurtAndBreak` |
| `EquipmentSlotProvider#getPreferredEquipmentSlot` | `getEquipmentSlotForItem` |
| `ComponentTooltipAppenderRegistry` | `ItemComponentTooltipProviderRegistry` |
| `FabricBlockEntityType#addSupportedBlock` | `addValidBlock` |
| `FabricEntityTypeBuilder...spawnGroup/spawnRestriction` | `mobCategory/spawnPlacement` |
| `FabricTrackedDataRegistry` | `FabricEntityDataRegistry` |
| `PointOfInterestHelper` | `PoiHelper` |
| `FabricRegistryBuilder.createSimple` | `create` |
| `DynamicRegistryView#asDynamicRegistryManager` | `asRegistryAccess` |
| `FabricGameRuleVisitor` | `FabricGameRuleTypeVisitor` |
| `CompostingChanceRegistry` | `CompostableRegistry` (26.1), then **removed in 26.3** |
| `FabricBrewingRecipeRegistryBuilder` | `FabricPotionBrewingBuilder` (26.1), then **removed in 26.3** |
| `FlammableBlockRegistry.Entry#getBurnChance/getSpreadChance` | `getIgniteOdds/getBurnOdds` |
| `LandPathNodeTypesRegistry` | `LandPathTypeRegistry` (`getPathType`) |
| `OxidizableBlocksRegistry.registerOxidizableBlockPair/registerWaxableBlockPair/registerCopperBlockSet` | `registerNextStage/registerWaxable/registerWeatheringCopperBlocks` |
| `SculkSensorFrequencyRegistry` | `VibrationFrequencyRegistry` |
| `VillagerInteractionRegistries.registerCollectable` | `registerGatherableItem` |
| `TradeOfferHelper` | **gone**: villager trades are data-driven in 26.1 (`data/<ns>/villager_trade`, `trade_set`, tags `data/minecraft/tags/villager_trade/<profession>`) [blog] |
| `FabricRecipeManager` / `FabricServerRecipeManager` | `FabricRecipeAccess` / `FabricRecipeManager` (note the swap!) |
| `CustomIngredient#getMatchingItems/toDisplay`, `CustomIngredientSerializer#getPacketCodec` | `items/display`, `getStreamCodec` |
| `FabricLootPoolBuilder#with/conditionally` | `add/when` |
| `BiomeModificationContext.SpawnSettingsContext` | `MobSpawnSettingsContext` (`getMobs`, `addMobCharge`); `getSpawnSettings` becomes `getMobSpawnSettings` |
| `BiomeSelectionContext#getBiomeRegistryEntry` | `getBiomeHolder` |
| `FabricBlockStateParticleEffect` | `FabricBlockParticleOption` |
| Transfer: `InventoryStorage`; `*Variant#getRegistryEntry/withComponentChanges`; `StorageUtil.calculateComparatorOutput`; `SingleVariantStorage#readData/writeData` | `ContainerStorage`; `typeHolder/withComponents`; `getRedstoneSignal`; `readValue/writeValue` |
| Serialization `FabricReadView/FabricWriteView` (`api.serialization.v1.view`) | `FabricValueInput/FabricValueOutput` (`api.serialization.v1.value`); `keys` becomes `keySet` |
| `ExtendedScreenHandlerType`/`ExtendedScreenHandlerFactory`/`FabricScreenHandlerFactory` (`api.screenhandler.v1`) | `ExtendedMenuType`/`ExtendedMenuProvider`/`FabricMenuProvider` (`api.menu.v1`); `getPacketCodec` becomes `getStreamCodec` |
| `ResourceLoader#registerReloader/addReloaderOrdering`; `SimpleResourceReloader` | `registerReloadListener/addListenerOrdering`; `SimpleReloadListener` |
| `FabricEntitySelectorReader` | `FabricEntitySelectorParser` |
| `ClientCommandManager` | `ClientCommands`; `FabricClientCommandSource#getWorld` becomes `getLevel` |

### Datagen
`FabricDataOutput` became `FabricPackOutput`. `FabricTagProvider<T>` became `FabricTagsProvider<T>`, with inner classes `BlockTagsProvider`, `ItemTagsProvider`, `FluidTagsProvider`, `EntityTypeTagsProvider` and `BlockEntityTypeTagsProvider` [src]. `FabricTagProvider.FabricValueLookupTagProvider` (later `FabricIntrinsicHolderTagsProvider`) is **gone** in 26.2, because `valueLookupBuilder` was removed when tags moved to `ResourceKey`s [blog 26.2]. `FabricProvidedTagBuilder` became `FabricTagAppender`. `FabricBlockLootTableProvider` became `FabricBlockLootSubProvider`, `FabricEntityLootTableProvider` became `FabricEntityLootSubProvider`, `FabricLootTableProvider` became `FabricLootTableSubProvider`, and `SimpleFabricLootTableProvider` became `SimpleFabricLootTableSubProvider`. `FabricRecipeExporter` became `FabricRecipeOutput`. `FabricLanguageProvider#dataOutput` became `packOutput`. `FabricCodecDataProvider/FabricAdvancementProvider#pathResolver` became `pathProvider`. The 26.3 providers directory [src] contains `FabricAdvancementProvider, FabricBlockLootSubProvider, FabricBrewingProvider, FabricCodecDataProvider, FabricDynamicRegistryProvider, FabricEntityLootSubProvider, FabricLanguageProvider, FabricLootTableSubProvider, FabricRecipeProvider, FabricTagAppender, FabricTagBuilder, FabricTagsProvider, SimpleFabricLootTableSubProvider`. `DataGeneratorEntrypoint` gained `buildReloadableRegistry(RegistrySetBuilder)` in 26.3 [src].

### Client / rendering
| Old | New |
|---|---|
| `KeyBindingHelper.registerKeyBinding` | `KeyMappingHelper.registerKeyMapping` [src] |
| `BlockRenderLayerMap.putBlock(...)` | `ChunkSectionLayerMap`. Usually unnecessary: since 26.1 the layer is **computed from sprite transparency** [blog 26.1] |
| `ColorProviderRegistry.BLOCK.register(...)` | `BlockColorRegistry.register(List<BlockTintSource>, Block...)` or `register(BlockTintsFactory, Block...)` [src] |
| `HudRenderCallback` | **removed** in 26.1. Use `HudElementRegistry` [blog] |
| `VanillaHudElements.STATUS_EFFECTS` | `MOB_EFFECTS` |
| `WorldRenderEvents`, `WorldRenderContext` (`api.client.rendering.v1.world`) | `LevelRenderEvents`, `LevelRenderContext` (`...v1.level`); `matrices` becomes `poseStack`, `consumers` becomes `bufferSource`, `commandQueue` becomes `submitNodeCollector` |
| `EntityModelLayerRegistry` (+ `TexturedModelDataProvider`) | `ModelLayerRegistry` (`TexturedLayerDefinitionProvider#createLayerDefinition`) |
| `LivingEntityFeatureRendererRegistrationCallback` | `LivingEntityRenderLayerRegistrationCallback` (`registerLayers`) |
| `SpecialGuiElementRegistry` | `PictureInPictureRendererRegistry` |
| `TooltipComponentCallback` | `ClientTooltipComponentCallback` |
| `DrawItemStackOverlayCallback` | `RenderItemDecorationsCallback` |
| `ParticleFactoryRegistry`, `FabricSpriteProvider`, `ParticleRendererRegistry` | `ParticleProviderRegistry`, `FabricSpriteSet`, `ParticleGroupRegistry` |
| `AtlasSourceRegistry` | `SpriteSourceRegistry` |
| `Screens.getButtons/getTextRenderer/getClient` | `getWidgets/getFont/getMinecraft` |
| `FabricBakedModelManager` | `FabricModelManager` |
| Renderer API: `QuadView#renderLayer/glint`; `FabricBlockModels`; `MeshBakedGeometry`; `FabricBlockRenderManager`; `FabricRenderCommandQueue` | `chunkLayer/foilType`; `FabricBlockModelShaper`; `MeshQuadCollection`; `FabricBlockRenderDispatcher`; `FabricOrderedSubmitNodeCollector` |
| `FluidRenderHandler` (most uses) | vanilla `FluidModel.Unbaked` registered with `FluidRenderingRegistry.register` [blog 26.1] |

Since `MultiBufferSource` itself is gone from vanilla in 26.2+ (see 4.1), the FAPI members still named `bufferSource` hand you whatever the 26.3 equivalent is. Check the fapi source of the specific context class (`fabric-rendering-v1/src/client/java/net/fabricmc/fabric/api/client/rendering/v1/level/LevelRenderContext.java`) before using them.

---

## 3. 1.21.11 -> 26.1 (released 2026-03; Fabric blog 2026-03-14)

### Platform / toolchain
- **Unobfuscated**: runtime names are Mojang names, and parameter and local names are included. Yarn is discontinued (it ended at 1.21.11). Intermediary is gone for 26.x. **No 1.21.x mod jar works**, even as compileOnly.
- **Java 25** is required (vanilla uses JEP 447 statements-before-super). IntelliJ must be 2025.3+ for mixins [blog]. Gradle runs on JDK 25.
- Loom uses the plugin id `net.fabricmc.fabric-loom`, with no `mappings`, no `mod*` configurations and no `remapJar` (toolchain.md section 3).
- Fabric Model Loading API and Renderer/Indigo shipped late for 26.1 [blog]. Both are present in 0.166.2+26.3 [src].
- 26.1 was the last OpenGL-only release. 26.2 added a Vulkan backend. Raw GL calls must move to the Blaze3D / Renderpearl APIs [blog].

### Notable vanilla changes [primer 26.1 unless noted]
- **`ItemStackTemplate`** (record `(Holder<Item> item, int count, DataComponentPatch components)` [src `world/item/ItemStackTemplate.java`]): an immutable stack used by recipes, advancements, loot results and trades. Convert with `template.create()` and `ItemStackTemplate.fromNonEmptyStack(stack)`. Both implement `ItemInstance` (`typeHolder()`, `is(...)`, `count()`, `get(DataComponentType)`). **An `ItemStack` cannot be created before a world is loaded**, and a template's components throw if accessed before binding [blog].
- **Data component initializers**: item components are bound to the `Holder` at resource reload. `Item.Properties` has `delayedComponent(type, HolderLookup.Provider -> value)` and `delayedHolderComponent(...)` for components that need datapack registries. `Recipe#assemble` no longer takes `HolderLookup.Provider`.
- **`RecipeSerializer` is now a record** `RecipeSerializer<T>(MapCodec<T> codec, @Deprecated StreamCodec<RegistryFriendlyByteBuf,T> streamCodec)` [src]. Delete your `Serializer` inner classes and register with `Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id, new RecipeSerializer<>(CODEC, STREAM_CODEC))`.
- **Loot type unrolling**: loot entries, functions, conditions, NBT, number, score, int and float providers register their `MapCodec` directly. The `*Type` wrapper classes are gone, and `getType()` becomes `codec()`.
- **Villager trades are data-driven**: `VillagerTrade`/`TradeSet` datapack registries (`Registries.VILLAGER_TRADE`, `Registries.TRADE_SET` [src]). Fabric's `TradeOfferHelper` was removed.
- **World clocks and timelines** (`world_clock` registry). Game rules, weather, wandering trader state, boss events, the ender dragon fight and worldgen settings moved into `SavedData`. `DimensionDataStorage` was renamed `SavedDataStorage`, and `SavedDataType` is identified by an `Identifier` (subdirectories allowed). `GameRules` lives in `net.minecraft.world.level.gamerules` [src].
- **Dye component** `DataComponents.DYE`. `ArmorDyeRecipe` was renamed `DyeRecipe`.
- `Level#random` is now protected. Use `getRandom()`.
- **`ChunkPos` is a record** [src]: `new ChunkPos(BlockPos)` becomes `ChunkPos.containing(pos)`, `new ChunkPos(long)` becomes `ChunkPos.unpack(l)`, and `toLong()/asLong()` become `pack()`.
- **`Entity#interactAt` is removed**. `Entity#interact(Player, InteractionHand, Vec3 location)` [src `Entity.java:2317`] now gets the hit location.
- **Rendering**: `RenderType`/`ChunkSectionLayer` are computed per quad from texture transparency, so `ItemBlockRenderTypes` and manual layer registration are gone. `BlockColor` became `BlockTintSource`. `BlockRenderDispatcher` and `ItemRenderer` were removed (feature submission pipeline). `QuadInstance` was added. `FluidModel` replaced `LiquidBlockRenderer`'s texture logic. `EntityRenderer#submitNameTag` became `submitNameDisplay`. `BlockAndTintGetter` is client-only (common code uses `BlockAndLightGetter`). The camera is extracted to `CameraRenderState` (now `net.minecraft.client.renderer.state.level.CameraRenderState` [src]).
- **GUI "extract" rename**: `GuiGraphics` became `GuiGraphicsExtractor` [src `client/gui/GuiGraphicsExtractor.java`]. `draw*`/`render*` methods became `extract*` / `*RenderState`, e.g. `Renderable#render` became `extractRenderState` and `AbstractContainerScreen#renderLabels` became `extractLabels`. Overrides of `Screen#render(...)` must become `extractRenderState(GuiGraphicsExtractor, int, int, float)` [src `Screen.java:121`]. `extractBackground` exists too.
- `AbstractContainerScreen`: `imageWidth/imageHeight` are final and passed to the constructor. `render` already calls `renderTooltip` (extract variant) at the end.
- Entity textures were reorganised into per-entity subfolders (`textures/entity/pig/pig_cold.png` style). Resource packs that override them break.
- `TypedInstance` interface (`Entity`, `BlockState`, ...) with `typeHolder()` and `is(...)`.
- Brains: `ActivityData`. Cauldron interactions moved to `CauldronInteractions` with a dispatcher. Random patch feature removed. Plantable support moved to `support_*` tags. Chat permissions (`Permissions#CHAT_*`). IME support (`GuiEventListener#preeditUpdated`).

### New Fabric API in 26.1 [blog]
- `DimensionEvents.MODIFY_ATTRIBUTES` (`(dimension, attributes, registries) -> attributes.set(EnvironmentAttributes.X, v)`) [src `fabric-dimensions-v1/.../DimensionEvents.java`].
- `BlockEvents.USE_ITEM_ON`, `BlockEvents.USE_WITHOUT_ITEM`, `ItemEvents.USE_ON`, `ItemEvents.USE` (in `net.fabricmc.fabric.api.event.player`, module events-interaction [src]). These run right before the vanilla method and replace it for non-null returns.
- `BlockColorRegistry` (see above).

---

## 4. 26.1 -> 26.2 (Fabric blog 2026-06-15; Loom 1.17, Loader 0.19.3 at the time)

### Fabric
- **Enum extension** (Loader 0.19.0+, Loom 1.16/1.17): enum mixins plus `extend-enum` in a v2 class tweaker (toolchain.md section 10).
- **Tag entry removal** in tag JSON (FAPI 0.150.1+): `"fabric:remove": ["minecraft:brick"]` alongside `values`.
- **Fluid interaction API** (experimental in 26.2, stable in 26.3): `EntityFluidInteractionRegistry.register(fluidTag, FluidBehaviour...)`. The blog snippet is UNVERIFIED; check `fabric-content-registries-v0` or `fabric-block-api-v1` sources for exact names.
- **Attended client commands**: `.requires(FabricClientCommandSource::attended)` [src].

### Vanilla [primer 26.2 / blog]
- **Vulkan backend** (`com.mojang.blaze3d.vulkan` parallel to `opengl`). `BlendFactor`/`BlendOp`, the `GpuFormat` enum (replacing `TextureFormat` and vertex element formats), dynamic `VertexFormat` builder, `BindGroupLayout`, multiple color targets, and render areas.
- **"Feature rendering takeover"**: `MultiBufferSource` and direct vertex uploads outside chunk rendering were **removed**. Everything submits via `SubmitNodeCollector`/`OrderedSubmitNodeCollector` [src both exist; `MultiBufferSource` absent in 26.3]. PiP renderers get `FeatureRenderDispatcher`/`SubmitNodeCollector`. Shape outlines use `OrderedSubmitNodeCollector#submitShapeOutline` (replacing `ShapeRenderer`). `Font` draw methods were removed (`submitText` or `$PreparedText`). Gizmos use `GizmoFeatureRenderer`.
- **`ChatFormatting` mostly gutted** in favour of `Style` (`withColor`, `withBold`, ...). The class still exists [src].
- **GUI reorganisation**: the current screen and chat listener moved from `Minecraft` to `Gui`, and the HUD moved to a new `Hud` class. `Minecraft.getInstance().setScreen(s)` becomes `Minecraft.getInstance().gui.setScreen(s)`, and the current screen is `...gui.screen()` [src `Minecraft.java:308 public final Gui gui`, `Gui.java:230/234`].
- **Separate id classes**: `ResourceKey`s for blocks, items, entity types, block entity types, fluids and potions live in `*Ids` classes: `net.minecraft.references.BlockIds`, `ItemIds`, `BlockItemIds` (a pair of block and item keys) [src], and `net.minecraft.world.entity.EntityTypeIds` [src]. **Vanilla `EntityType` and `BlockEntityType` constants moved to `EntityTypes` / `BlockEntityTypes`**: `EntityType.ZOMBIE` becomes `EntityTypes.ZOMBIE` [src: `EntityType.java` has 0 `public static final EntityType<` fields, `EntityTypes.java` has 161]. Vanilla now registers with `Items.registerItem(ItemIds.DIAMOND_SWORD, new Item.Properties()...)`. Datagen uses keys rather than objects.
- **Tags providers**: `HolderTagProvider`, `IntrinsicHolderTagsProvider` and `KeyTagProvider` were removed. `TagAppender` takes only `ResourceKey`/`TagKey`.
- **Object collections**: variant sets condensed, e.g. `Items.DYE` is a `ColorCollection<Item>` [src `Items.java:1996`] instead of 16 `*_DYE` fields (check the specific item before porting).
- **Advancements reorganised**: triggers moved to `net.minecraft.advancements.triggers` (`CriteriaTriggers`, `SimpleCriterionTrigger`, `ConsumeItemTrigger`, `InventoryChangeTrigger` [src]) and predicates to `net.minecraft.advancements.predicates` (`ItemPredicate`, `MinMaxBounds`, `DataComponentMatchers` [src]), with entity predicates in `...predicates.entity` (`EntityPredicate` [src]).
- Structure processors register their `MapCodec` directly (`StructureProcessor` is an interface; `getType` becomes `codec`). Game-test entity builders: `GameTestHelper#spawnEntity/spawnMob` return `GameTestEntityBuilder`/`GameTestMobBuilder`. Shears breaking speed is tag-driven. Sulfur cube archetypes registry. Xbox friends list.
- `Player` now extends `Avatar` (`net.minecraft.world.entity.Avatar extends LivingEntity` [src]). UNVERIFIED in which version this happened.

---

## 5. 26.2 -> 26.3 "Wilderness Bound" (released 2026-09-15; data pack 121.0, resource pack 97.1)

### Fabric API removals [blog 26.3; src confirms absence]
- `FuelRegistry`/`FuelValueEvents` and `CompostingChanceRegistry`/`CompostableRegistry` are gone. Use the item components `DataComponents.COOKING_FUEL` (`CookingFuel(ResolvableInt, ResolvableFloat)`), `DataComponents.COMPOSTABLE` (`Compostable(ResolvableInt)`) and `DataComponents.BREWING_FUEL`.
  - Your own items: `new Item.Properties().component(DataComponents.COOKING_FUEL, ...)`.
  - Other mods' or vanilla items: `DefaultItemComponentEvents.MODIFY.register(ctx -> ctx.modify(item, b -> b.set(...)))` [src `fabric-item-api-v1/.../DefaultItemComponentEvents.java`].
- `FabricPotionBrewingBuilder` is gone. **Brewing recipes are data-driven** (`RecipeType#BREWING`, `BrewingRecipe` with `PotionIngredient`), and there is a datagen helper `FabricBrewingProvider` [src].
- `StrippableBlockRegistry`, `TillableBlockRegistry` and `FlattenableBlockRegistry` are gone, replaced by **vanilla block transformers** (`Registries.BLOCK_TRANSFORMER`, data folder `block_transformer`, item component `DataComponents.BLOCK_TRANSFORMER`). Helpers: `BlockTransformerHelper.registerStripping/registerTilling/registerFlattening(Block|BlockPredicate, BlockStateProvider)` and `registerAxe/Hoe/Shovel(BlockTransformer | BlockTransformData)` [src `fabric-item-api-v1/.../BlockTransformerHelper.java`]. `BlockTransformerEvents.MODIFY` modifies other sources' transformers [src].
- `FluidVariantAttributes#enableColoredVanillaFluidNames` was replaced by `getColoredName`.
- Current content-registries-v0 `api.registry` [src] contains only `FlammableBlockRegistry, LandPathTypeRegistry, OxidizableBlocksRegistry, VibrationFrequencyRegistry, VillagerInteractionRegistries`.

### Fabric API additions
- Fluid flow hook: the blog shows `FluidFlowCallback.EVENT`, but **the real 26.3 API is `net.fabricmc.fabric.api.block.v1.FluidFlowEvents.ALLOW`** with `boolean allowFlow(FluidState fluid, LevelAccessor level, BlockPos fluidPosition)` [src `fabric-block-api-v1/.../FluidFlowEvents.java`]. The fluid behaviour API is now stable (`isInFluid`, `onFluidEntered`, `onFluidExited`).
- `TooltipFlag#shouldDisplayAllInformation` (injected via `FabricTooltipFlag`).
- `Block#getProvidedEnchantmentPower` for enchanting power providers (via `FabricBlock`).
- `DynamicRegistries.registerReloadable(key, codec)` for mod reloadable registries [src `fabric-registry-sync-v0/.../DynamicRegistries.java:212`].

### Vanilla [primer 26.3 / blog 26.3]
- **SDL replaces GLFW** (LWJGL with SDL). Input key and mouse codes changed values. **Always use `com.mojang.blaze3d.platform.InputConstants`**, never `GLFW.*` constants. Custom text inputs must call `Minecraft.getInstance().textInputManager().onTextInputFocusChange(focused)` or `Minecraft#onTextInputFocusChange(listener, focused)`, otherwise typing breaks (`com.mojang.blaze3d.platform.TextInputManager` [src]).
- **Renderpearl**: the render API moved out of Blaze3D into `com.mojang.renderpearl.{api,backend,frontend,util}` [src dirs exist]. `com.mojang.blaze3d` still holds `vertex` (`PoseStack`, `BufferBuilder`, `VertexConsumer`, `DefaultVertexFormat`), `platform`, `systems` and others [src]. `Tesselator` is **gone** [src]. `RenderSystem#outputColorTextureOverride` was removed. Shaders are compiled with shaderc: `#moj_import` becomes `#include`, and explicit layout locations are required. OIT ("improved transparency"). Palette PNG metadata. `ItemQuads`. Crumbling overlay is submitted separately.
- **Reloadable datapack registries**: loot tables, loot conditions and functions, `ContextFloatProvider`/`ContextIntProvider`, `SlotSource`, advancements and **recipes** are now datapack registries reloaded by `/reload`. Data providers for them implement `SingleRegistryBootstrap`/`MultiRegistryBootstrap` and feed `RegistriesDatapackGenerator#forReloadableLayer`.
- **`ContextAwarePredicate` is removed**. Trigger instances use `Optional<Holder<LootItemCondition>> player` [src `advancements/triggers/ConsumeItemTrigger.java`].
- **Number providers split** into `ContextFloatProvider` / `ContextIntProvider` (registries `Registries.CONTEXT_FLOAT_PROVIDER`/`CONTEXT_INT_PROVIDER`, data folders `context_float_provider`/`context_int_provider` [src]). Loot number classes moved to `net.minecraft.world.level.storage.loot.providers.number.ints.*` and `.floats.*` (`ConstantValue`, `UniformGenerator`, `BinomialDistributionGenerator` in `ints`) [src]. `ResolvableInt`/`ResolvableFloat` hold a constant or a registered provider reference.
- **`Feature` + `ConfiguredFeature` + `FeatureConfiguration` merged** into a `Feature` interface. The datapack registry is `Registries.FEATURE` (`worldgen/feature` data folder), and types are `BuiltInRegistries.FEATURE_TYPE` (`MapCodec<? extends Feature>`) [src]. The `config` object was removed from JSON. `ConfiguredFeature`, `OreConfiguration` and `TreeConfiguration` no longer exist [src]. The same merge happened for carvers (`WorldCarver`, `BuiltInRegistries.CARVER_TYPE` [src]). Placement modifiers were renamed. `BlockStateProvider`, `StructurePlacement` and `PlacementModifier` register `MapCodec`s directly (`*Type` classes removed). `BlockStateProvider` and `StructurePlacement` are interfaces, and `BlockStateProvider#getState` takes a `LevelAccessor`. Block state providers are registrable (`worldgen/block_state_provider`).
- **Surface rules are now "material" rules** (`SurfaceSystem` becomes `MaterialSystem`, `SurfaceRules` becomes `MaterialRules`). They are registrable: `worldgen/material_rule`, `worldgen/material_condition`, `BuiltInRegistries.MATERIAL_RULE_TYPE` [src]. Density functions were split into `DensityFunction` (data) and `DensitySampler`. Noise classes were reworked.
- **`Block#codec()` and the `block_type` registry are removed**. Delete your `CODEC`/`codec()` overrides on blocks.
- **`AxeItem`, `HoeItem` and `ShovelItem` are removed** [src: no such files]. Tools are plain `Item`s configured via `Item.Properties#axe(ToolMaterial, attackDamage, attackSpeed)` and similar (e.g. `Items.DIAMOND_AXE = registerItem(ItemIds.DIAMOND_AXE, new Item.Properties().axe(ToolMaterial.DIAMOND, 5.0F, -3.0F))` [src `Items.java:1637`]) plus a `BLOCK_TRANSFORMER` component. `AxeItem.STRIPPABLES` became `BlockTransformers#AXE` (`SHOVEL`, `HOE` likewise).
- New components: `VILLAGER_FOOD` (replacing `Villager#FOOD_POINTS`), `MOB_VISIBILITY`. `MobSpawnSettings` moved off `Biome` to the environment attribute `EnvironmentAttributes#NATURAL_MOB_SPAWNS`.
- `CommandResponseTracker` (command feedback helper) and `ConversionTracker` (mob conversions) were added. New `RuleTest`s: `AllOf/AnyOf/NotRuleTest`. `ContextKeySet` registry.

---

## 6. 26.4 (Deep Freeze; ported on branch `deep_freeze` against 26.4-pre-1)

The full official changelog for every 26.4 snapshot and pre-release is saved in `changelogs/26.4/` (from Mojang's launcher feed `https://launchercontent.mojang.com/v2/javaPatchNotes.json`, then `v2/<contentPath>` per version; the wiki page as raw wikitext via `?action=raw`). The Java changes below were found by compiling every mod against 26.4-pre-1 (src: `build/mcsrc/26.4-pre-1`, Fabric API branch `26.4`).

- Toolchain: `minecraft_version=26.4-pre-1`, Fabric API `0.167.1+26.4`, same Loom 1.19.4 and Loader 0.20.1. Data pack **128.1**, resource pack **104.0** (`version.json`).
- **Mod dependency range:** Fabric normalises snapshot ids (`26.4-pre-1` becomes `26.4-pre.1`), so `"~${minecraft_version}"` refuses to load every mod. `fabric.mod.json` uses `${minecraft_dependency}` (`gradle/fabric-mod.gradle`, default `~<minecraft_version>`); set `minecraft_dependency=~26.4-` in `gradle.properties` while on a pre-release (Fabric API's own range).
- **Block sounds:** `SoundType` is gone. `BlockBehaviour.Properties.sound(ResourceKey<BlockSoundSet>)` takes `BlockSoundSets.X`; `WoodType` takes the same keys. A state's sounds: `state.getSounds(level)` returns a `BlockSoundSet` record (`volume()`, `pitch()`, `breakSound()`/`placeSound()`... as `Optional<Holder<SoundEvent>>`). Block overrides `getSounds(BlockState)` returning `Optional<ResourceKey<BlockSoundSet>>`. Block sound events are collections: `SoundEvents.WOOL.breakSound()` instead of `SoundEvents.WOOL_BREAK`.
- **Client registries:** renderers and screens register into `BuiltInClientRegistries`: `EntityRenderers.register(BuiltInClientRegistries.ENTITY_RENDERER, type, provider)`, `BlockEntityRenderers.register((WritableRegistry<...>) BuiltInClientRegistries.BLOCK_ENTITY_RENDERER, ...)`, `MenuScreens.register((WritableRegistry<...>) BuiltInClientRegistries.MENU_SCREEN, ...)`. Item tint sources and range-select item properties: `Registry.register(BuiltInClientRegistries.ITEM_TINT_SOURCE_TYPE / RANGE_SELECT_ITEM_MODEL_PROPERTY_TYPE, id, codec)` (the `ID_MAPPER`s are gone).
- **GUI:** render extraction lost its mouse and partial-tick arguments (`extractBackground(GuiGraphicsExtractor)`, `extractLabels(GuiGraphicsExtractor)`; mouse from `graphics.mouseX()`). HUD elements are `(GuiGraphicsExtractor)` only. `RenderPipelines.GUI_TEXTURED` is `GuiPipeline.TEXTURED` (`net.minecraft.client.gui`); `RenderPipelines` moved to `client.renderer.pipeline`. Sign edit screens lay text out as widgets: `getSignTextYOffset()` and `getSignTextSpacing()` replace `getSignYOffset()`/`getSignTextScale()`. The enchanting table builds its hover text from `EnchantmentScreen.getClue(int)` into widget tooltips.
- **Menus:** `new Slot(container, index, x, y, Slot.Section.X)` (a section for keyboard navigation and narration).
- **Commands:** `Commands.LEVEL_*` moved to `CommandPermissionLevels`. `SharedSuggestionProvider.suggest` is `SuggestionContext.suggest` (`commands.suggestions`). `ItemArgument.item()` takes no build context. `Commands.getDispatcher()` is gone (parsing goes through `parseBox()`); run a command with `getCommands().performPrefixedCommand(source, command)`.
- **Loot:** `LootItemConditionalFunction.validate` is final; override `validateCustom(ValidationContext)` (may be empty).
- **Worldgen:** `getStructureAt` returns `@Nullable StructureStart` (no `isValid()`/`INVALID_START`). `Structure.GenerationContext.noiseBiomeResolver()` replaces `biomeResolver()`; the quart-resolution resolver type is `NoiseBiomeResolver`, and `BiomeResolver` is now block-resolution (`getBiome(x, y, z)`). Chunks hold per-block biomes: `chunk.fillBiomes(new CachedChunkBiomeResolver(chunk, noiseResolver, BiomeManager.obfuscateSeed(seed)))`; `level.getUncachedBiome(x, y, z)` takes block coordinates. `ChunkGenerator.buildTerrain(chunk, blender, randomState, structures, @Nullable WorldGenRegion, Set<Holder<Biome>> possibleBiomes)` (surface rules read the chunk's own biomes; `chunk.collectBiomesInPalette(set)`). `getBaseHeight` is `getFirstFreeHeight`. `NoiseColumn` moved to `levelgen`. `GenerationContext.registryAccess()` is a `HolderGetter.Provider` (`get(key)`, no `getValue`).
- **Misc renames:** `Mth.clamp` is gone (use `Math.clamp`, which throws when min > max). `EntityTypeTags.IGNORES_POISON_AND_REGEN` split into `IMMUNE_TO_POISON` and `IMMUNE_TO_REGENERATION`. `BlockEntity.getUpdateTag(HolderGetter.Provider)`. `FlowingFluid.getSource()` takes no argument (no flowing fluid types). `ByteBufCodecs.idMapper(byId, toId)` (`Block.BLOCK_STATE_REGISTRY::byIdOrThrow, ::getIdOrThrow`). `StructureTemplate.createFromWorld(level, pos, size, author, entities, ignore)`. `Block.getDrops(state, level, pos, blockEntity, breaker, tool)`. `ServerPlayer.doCheckFallDamage` is `updateFallDistanceAndCheckFallDamageForClientControlledEntity(Vec3, onGround)`. Mob effect ids live in `MobEffectIds`.
- **Dispensers are data-driven:** `DispenserBlock.registerProjectileBehavior` is gone; an item carries `Item.Properties().dispensed(DispenseBehaviorIds.X)` (`DEFAULT_PROJECTILE`, `SPAWN_EGG`...). Spawn eggs: `Properties.spawns(type).dispensed(DispenseBehaviorIds.SPAWN_EGG)` (`spawnEgg(type)` is gone).
- **Mixin targets that moved:** `DropExperienceBlock.spawnAfterBreak` gained an `Entity breaker`; `FogRenderer.setupFog` gained `boolean shouldCreateBossFog`; snowball damage is `Snowball.getDamage(EntityHitResult)`. Zombies convert to Frostbites in powder snow (`Zombie.convertsWhenFreezing()`).
- New `minecraft:block_sound_set` registry (data-driven block sound sets: `volume`, `pitch`, `break_sound`, `step_sound`, `place_sound`, `hit_sound`, `fall_sound`). Sound set `minecraft:amethyst` was renamed `amethyst_block`, and `twisting_vines` was removed.
- New feature type `crevasse`. Predicate `below_heightmap`. `matching_fluids` block predicate gained `state`. Environment attribute `visual/has_sky_occluder`.
- Many new tags, e.g. `block/pathfinding/*` (pathfinding moved toward tags), `fluid/swimmable`, `entity_type/immune_to_weakness`, `immune_to_wither`, `item/sheep_wool_dyes`, `item/knocks_back_players_even_with_zero_damage`.
- `/effect` accepts mob effect tags. `/fillbiome` is block-accurate. The command report format changed. There is a new `pattern` (regex) argument type.
- `minecraft:consumable` `sound` and `minecraft:equippable` `equip_sound`/`shearing_sound` became truly optional.
- Rendering: terrain vertex data is now read from a texel buffer (new packing in `terrain.vsh`). OIT uses subpasses. `screenquad.vsh` was renamed `screentriangle.vsh`. Shader packs and core-shader mods will break.
- Content: ice caves biome, frostbite mob, Freezing effect, calcite block set, ice and snow bricks, jukebox playlist containers.

---

## 7. Yarn -> Mojang cheat sheet (all Mojang names verified to exist in 26.3 source)

Yarn names are given for searching old code. Paths are relative to `net/minecraft/` unless they start with `com/`. `(c)` means client-only (in `/tmp/claude-0/mcsrc/client`). **`Identifier` is the Mojang name in 26.3** (`resources/Identifier.java`); `ResourceLocation` no longer exists (Mojang renamed it in 1.21.11).

### Core, registries, ids
| Yarn | Mojang 26.3 | Path |
|---|---|---|
| `Identifier` | `Identifier` (`fromNamespaceAndPath`, `parse`, `withDefaultNamespace`, `getNamespace`, `getPath`) | `resources/Identifier` |
| `RegistryKey` | `ResourceKey` (`create(registryKey, id)`, **`identifier()`** returns the id, `registry()`) | `resources/ResourceKey` |
| `Registry` | `Registry` | `core/Registry` |
| `Registries` (static registries) | `BuiltInRegistries` | `core/registries/BuiltInRegistries` |
| `RegistryKeys` | `Registries` | `core/registries/Registries` |
| `RegistryEntry` | `Holder` | `core/Holder` |
| `RegistryEntryList` | `HolderSet` | `core/HolderSet` |
| `RegistryWrapper.WrapperLookup` | `HolderLookup.Provider` | `core/HolderLookup` |
| `DynamicRegistryManager` | `RegistryAccess` | `core/RegistryAccess` |
| `TagKey` | `TagKey` | `tags/TagKey` |
| `BlockTags` / `ItemTags` | same | `tags/` |
| `Util` | `Util` | `util/Util` (note: package `net.minecraft.util`) |
| `MathHelper` | `Mth` | `util/Mth` |
| `Random` (MC) | `RandomSource` | `util/RandomSource` |
| `StringIdentifiable` | `StringRepresentable` | `util/StringRepresentable` |
| `Uuids` | `UUIDUtil` | `core/UUIDUtil` |
| `Formatting` | `ChatFormatting` (mostly superseded by `Style`) | `ChatFormatting` |

### World, positions, math
| Yarn | Mojang | Path |
|---|---|---|
| `World` | `Level` | `world/level/Level` |
| `ServerWorld` | `ServerLevel` | `server/level/ServerLevel` |
| `ClientWorld` | `ClientLevel` (c) | `client/multiplayer/ClientLevel` |
| `WorldView` | `LevelReader` | `world/level/LevelReader` |
| `WorldAccess` | `LevelAccessor` | `world/level/LevelAccessor` |
| `BlockView` | `BlockGetter` | `world/level/BlockGetter` |
| `BlockPos` | `BlockPos` (`above/below/relative/offset/north`) | `core/BlockPos` |
| `Vec3d` | `Vec3` | `world/phys/Vec3` |
| `Box` | `AABB` | `world/phys/AABB` |
| `Direction` | `Direction` | `core/Direction` |
| `ChunkPos` | `ChunkPos` (a **record** since 26.1) | `world/level/ChunkPos` |
| `WorldChunk` | `LevelChunk` | `world/level/chunk/LevelChunk` |
| `Heightmap` | `Heightmap` | `world/level/levelgen/Heightmap` |
| `GameMode` | `GameType` | `world/level/GameType` |
| `GameRules` | `GameRules` (moved package) | `world/level/gamerules/GameRules` |
| `PersistentState` | `SavedData` | `world/level/saveddata/SavedData` |
| `WorldSavePath` | `LevelResource` | `world/level/storage/LevelResource` |
| `Biome` / `DimensionType` | same | `world/level/biome/Biome`, `world/level/dimension/DimensionType` |
| `BlockHitResult` | `BlockHitResult` | `world/phys/BlockHitResult` |
| `RaycastContext` | `ClipContext` | `world/level/ClipContext` |
| `VoxelShape` / `VoxelShapes` | `VoxelShape` / `Shapes` | `world/phys/shapes/` |
| `ShapeContext` | `CollisionContext` | `world/phys/shapes/CollisionContext` |

### Entities
| Yarn | Mojang | Path |
|---|---|---|
| `Entity` / `LivingEntity` | same | `world/entity/` |
| `MobEntity` | `Mob` | `world/entity/Mob` |
| `PathAwareEntity` | `PathfinderMob` | `world/entity/PathfinderMob` |
| `PlayerEntity` | `Player` (extends `Avatar`) | `world/entity/player/Player` |
| `ServerPlayerEntity` | `ServerPlayer` | `server/level/ServerPlayer` |
| `ClientPlayerEntity` | `LocalPlayer` (c) | `client/player/LocalPlayer` |
| `EntityType` | `EntityType`. **Vanilla constants live in `EntityTypes`** | `world/entity/EntityType`, `world/entity/EntityTypes` |
| `SpawnGroup` | `MobCategory` | `world/entity/MobCategory` |
| `EntityAttributes` | `Attributes` | `world/entity/ai/attributes/Attributes` |
| `StatusEffect` / `StatusEffectInstance` | `MobEffect` / `MobEffectInstance` | `world/effect/` |
| `DamageSource` | `DamageSource` | `world/damagesource/DamageSource` |
| `EquipmentSlot` | `EquipmentSlot` | `world/entity/EquipmentSlot` |
| `ProjectileEntity` | `Projectile` | `world/entity/projectile/Projectile` |
| `ItemEntity` | `ItemEntity` | `world/entity/item/ItemEntity` |
| `ExperienceOrbEntity` | `ExperienceOrb` | `world/entity/ExperienceOrb` |

### Items, blocks, components
| Yarn | Mojang | Path |
|---|---|---|
| `Item` / `Item.Settings` | `Item` / `Item.Properties` | `world/item/Item` |
| `ItemStack` | `ItemStack`; immutable twin `ItemStackTemplate` | `world/item/` |
| `ItemConvertible` | `ItemLike` | `world/level/ItemLike` |
| `Items` / `BlockItem` | same | `world/item/` |
| `ItemGroup` / `ItemGroups` | `CreativeModeTab` / `CreativeModeTabs` | `world/item/` |
| `ItemUsageContext` | `UseOnContext` | `world/item/context/UseOnContext` |
| `ItemPlacementContext` | `BlockPlaceContext` | `world/item/context/BlockPlaceContext` |
| `ActionResult` | `InteractionResult` (sealed interface) | `world/InteractionResult` |
| `Hand` | `InteractionHand` | `world/InteractionHand` |
| `ToolMaterial` | `ToolMaterial` (record) | `world/item/ToolMaterial` |
| `TooltipType` | `TooltipFlag` | `world/item/TooltipFlag` |
| `Rarity` | `Rarity` | `world/item/Rarity` |
| `ComponentType` / `DataComponentTypes` | `DataComponentType` / `DataComponents` | `core/component/` |
| `FoodComponent` | `FoodProperties` | `world/food/FoodProperties` |
| `ConsumableComponent` | `Consumable` | `world/item/component/Consumable` |
| `EquippableComponent` | `Equippable` | `world/item/equipment/Equippable` |
| `AttributeModifiersComponent` | `ItemAttributeModifiers` | `world/item/component/ItemAttributeModifiers` |
| `Block` / `Blocks` | same | `world/level/block/` |
| `AbstractBlock.Settings` | `BlockBehaviour.Properties` | `world/level/block/state/BlockBehaviour` |
| `BlockState` | `BlockState` (`getValue`, `setValue`) | `world/level/block/state/BlockState` |
| `StateManager` | `StateDefinition` | `world/level/block/state/StateDefinition` |
| `Properties` (block state) | `BlockStateProperties` | `world/level/block/state/properties/BlockStateProperties` |
| `BooleanProperty` / `IntProperty` | `BooleanProperty` / `IntegerProperty` | same dir |
| `BlockSoundGroup` | `SoundType` | `world/level/block/SoundType` |
| `BlockWithEntity` | `BaseEntityBlock` | `world/level/block/BaseEntityBlock` |
| `BlockEntity` / `BlockEntityType` | same; **vanilla constants in `BlockEntityTypes`** | `world/level/block/entity/` |
| `FluidState` / `Fluids` | same | `world/level/material/` |
| `Enchantment` | `Enchantment` | `world/item/enchantment/Enchantment` |
| `Recipe` / `RecipeType` / `Ingredient` / `RecipeSerializer` | same (`RecipeSerializer` is a record) | `world/item/crafting/` |
| `LootTable` / `LootPool` / `ItemEntry` | `LootTable` / `LootPool` / `LootItem` | `world/level/storage/loot/...` |
| `LootContextParameters` | `LootContextParams` | `world/level/storage/loot/parameters/LootContextParams` |

### Data, NBT, network, commands
| Yarn | Mojang | Path |
|---|---|---|
| `NbtCompound` / `NbtList` / `NbtElement` | `CompoundTag` / `ListTag` / `Tag` | `nbt/` |
| `NbtOps` | `NbtOps` | `nbt/NbtOps` |
| `ReadView` / `WriteView` | `ValueInput` / `ValueOutput` | `world/level/storage/` |
| `PacketByteBuf` / `RegistryByteBuf` | `FriendlyByteBuf` / `RegistryFriendlyByteBuf` | `network/` |
| `PacketCodec` / `PacketCodecs` | `StreamCodec` / `ByteBufCodecs` | `network/codec/` |
| `CustomPayload` | `CustomPacketPayload` | `network/protocol/common/custom/CustomPacketPayload` |
| `ClientConnection` | `Connection` | `network/Connection` |
| `ServerPlayNetworkHandler` | `ServerGamePacketListenerImpl` | `server/network/` |
| `ClientPlayNetworkHandler` | `ClientPacketListener` (c) | `client/multiplayer/` |
| `PlayerManager` | `PlayerList` | `server/players/PlayerList` |
| `ServerChunkManager` | `ServerChunkCache` | `server/level/ServerChunkCache` |
| `ServerCommandSource` / `CommandManager` | `CommandSourceStack` / `Commands` | `commands/` |
| `Text` / `MutableText` / `Style` | `Component` / `MutableComponent` / `Style` | `network/chat/` |
| `ScoreboardObjective` | `Objective` | `world/scores/Objective` |
| `ResourceManager` | `ResourceManager` | `server/packs/resources/ResourceManager` |
| `TestContext` | `GameTestHelper` | `gametest/framework/GameTestHelper` |
| `RecipeExporter` / `ShapedRecipeJsonBuilder` | `RecipeOutput` / `ShapedRecipeBuilder` | `data/recipes/` |
| `BlockStateModelGenerator` / `ItemModelGenerator` | `BlockModelGenerators` / `ItemModelGenerators` (c) | `client/data/models/` |

### Menus and GUI (client)
| Yarn | Mojang | Path |
|---|---|---|
| `ScreenHandler` / `ScreenHandlerType` | `AbstractContainerMenu` / `MenuType` | `world/inventory/` |
| `NamedScreenHandlerFactory` | `MenuProvider` | `world/MenuProvider` |
| `Slot` | `Slot` | `world/inventory/Slot` |
| `Inventory` / `SimpleInventory` | `Container` / `SimpleContainer` | `world/` |
| `PlayerInventory` | `Inventory` | `world/entity/player/Inventory` |
| `MinecraftClient` | `Minecraft` (c) (`gui` field holds the screen since 26.2) | `client/Minecraft` |
| `MinecraftServer` | `MinecraftServer` | `server/MinecraftServer` |
| `Screen` / `HandledScreen` | `Screen` / `AbstractContainerScreen` (c) | `client/gui/screens/...` |
| `DrawContext` | **`GuiGraphicsExtractor`** (c). `GuiGraphics` does not exist in 26.x | `client/gui/GuiGraphicsExtractor` |
| `TextRenderer` | `Font` (c) | `client/gui/Font` |
| `ButtonWidget` / `TextFieldWidget` / `ClickableWidget` | `Button` / `EditBox` / `AbstractWidget` (c) | `client/gui/components/` |
| `Drawable` / `Element` | `Renderable` / `GuiEventListener` (c) | `client/gui/components/...` |
| `KeyBinding` | `KeyMapping` (c) | `client/KeyMapping` |
| `InputUtil` | `InputConstants` (c) | `com/mojang/blaze3d/platform/InputConstants` |

### Rendering (client)
| Yarn | Mojang | Path |
|---|---|---|
| `MatrixStack` | `PoseStack` | `com/mojang/blaze3d/vertex/PoseStack` |
| `VertexConsumerProvider` | (was `MultiBufferSource`; **removed in 26.2**) use `SubmitNodeCollector` / `OrderedSubmitNodeCollector` | `client/renderer/SubmitNodeCollector` |
| `VertexConsumer` / `BufferBuilder` | same | `com/mojang/blaze3d/vertex/` |
| `Tessellator` | (was `Tesselator`; **removed**) | - |
| `RenderLayer` | `RenderType` | `client/renderer/rendertype/RenderType` |
| `BlockRenderLayer` | `ChunkSectionLayer` | `client/renderer/chunk/ChunkSectionLayer` |
| `EntityRenderer` / `EntityRendererFactory.Context` | `EntityRenderer` / `EntityRendererProvider.Context` | `client/renderer/entity/` |
| `BlockEntityRenderer` | `BlockEntityRenderer` | `client/renderer/blockentity/` |
| `EntityModel` / `ModelPart` / `EntityModelLayer` | `EntityModel` / `ModelPart` / `ModelLayerLocation` | `client/model/...` |
| `Camera` / `GameRenderer` / `WorldRenderer` | `Camera` / `GameRenderer` / `LevelRenderer` | `client/...` |
| `Sprite` / `SpriteAtlasTexture` | `TextureAtlasSprite` / `TextureAtlas` | `client/renderer/texture/` |
| `ParticleManager` | `ParticleEngine` | `client/particle/ParticleEngine` |
| `ParticleEffect` / `ParticleTypes` | `ParticleOptions` / `ParticleTypes` | `core/particles/` |
| `SoundEvent` / `SoundEvents` / `SoundCategory` | `SoundEvent` / `SoundEvents` / `SoundSource` | `sounds/` |
| `PositionedSoundInstance` | `SimpleSoundInstance` | `client/resources/sounds/SimpleSoundInstance` |

### Frequently used methods (verified in 26.3)
| Yarn | Mojang 26.3 |
|---|---|
| `world.isClient` | `level.isClientSide()` (`Level`) |
| `world.spawnEntity(e)` | `level.addFreshEntity(e)` (`LevelWriter`) |
| `world.setBlockState(pos, s)` | `level.setBlockAndUpdate(pos, s)` / `setBlock(pos, s, flags)` |
| `world.getTime()` | `level.getGameTime()` (`LevelAccessor`) |
| `world.getRegistryKey()` | `level.dimension()` |
| `entity.getWorld()` / `getPos()` / `getBlockPos()` / `getVelocity()` | `entity.level()` / `position()` / `blockPosition()` / `getDeltaMovement()` |
| `entity.damage(world, src, amt)` | `entity.hurtServer(ServerLevel, DamageSource, float)` |
| `entity.discard()` | `discard()` |
| `player.getStackInHand(hand)` | `getItemInHand(hand)` (`LivingEntity`) |
| `player.sendMessage(text, false)` | `serverPlayer.sendSystemMessage(Component)` |
| `player.sendMessage(text, true)` | `sendOverlayMessage(Component)` (`Player`); `displayClientMessage` no longer exists |
| `stack.isOf(item)` / `decrement(n)` / `increment(n)` / `getName()` | `stack.is(item)` / `shrink(n)` / `grow(n)` / `getHoverName()` |
| `stack.get(DataComponentTypes.X)` | `stack.get(DataComponents.X)` (`DataComponentHolder`) |
| `block.getDefaultState()` | `defaultBlockState()` |
| `appendProperties(StateManager.Builder)` | `createBlockStateDefinition(StateDefinition.Builder)` |
| `state.get(P)` / `state.with(P, v)` | `getValue(P)` / `setValue(P, v)` |
| `Block#onUse` / `onUseWithItem` | `useWithoutItem` / `useItemOn` (`BlockBehaviour`) |
| `Block#onStateReplaced` | `affectNeighborsAfterRemoval(BlockState, ServerLevel, BlockPos, boolean)` |
| `Item#use(World, PlayerEntity, Hand)` / `useOnBlock(ItemUsageContext)` | `use(Level, Player, InteractionHand)` / `useOn(UseOnContext)` |
| `server.getPlayerManager()` / `getOverworld()` / `getWorld(key)` | `getPlayerList()` / `overworld()` / `getLevel(key)` |
| `MinecraftClient.getInstance().setScreen(s)` | `Minecraft.getInstance().gui.setScreen(s)` (26.2+) |
| `Screen#render(DrawContext, ...)` | `extractRenderState(GuiGraphicsExtractor, int, int, float)` |
| `Text.literal/translatable` | `Component.literal/translatable` |
| `Identifier.of(ns, path)` | `Identifier.fromNamespaceAndPath(ns, path)` / `Identifier.parse("ns:path")` |
| `RegistryKey#getValue()` | `ResourceKey#identifier()` |
| `Registry.register(Registries.ITEM, id, item)` | `Registry.register(BuiltInRegistries.ITEM, id, item)` (vanilla itself uses `Items.registerItem(ItemIds.X, ...)`) |

---

## 8. Things that are gone (search your code for these)

`GuiGraphics`, `MultiBufferSource`, `Tesselator`, `AxeItem`, `HoeItem`, `ShovelItem`, `ConfiguredFeature`, `OreConfiguration`, `TreeConfiguration`, `ContextAwarePredicate`, `advancements.criterion.*` (became `triggers`/`predicates`), flat `loot.providers.number.ConstantValue`/`UniformGenerator` (became `.ints`/`.floats`), `Block#codec`, `ItemBlockRenderTypes`, `BlockRenderDispatcher`, `ItemRenderer`, `Entity#interactAt`, `EntityType.<CONSTANT>` (moved to `EntityTypes`), `Minecraft#setScreen`, `FabricPotionBrewingBuilder`, `FuelRegistry`, `CompostingChanceRegistry`, `Strippable/Tillable/FlattenableBlockRegistry`, `TradeOfferHelper`, `HudRenderCallback`, `fabric-convention-tags-v1`, `fabric-loot-api-v2`, the `fabric` mod id, GLFW input constants. The class-level items were verified absent in the 26.3 source with `find`; the FAPI items were verified absent in fapi 26.3.

Method to check whether a symbol still exists:

```sh
find /tmp/claude-0/mcsrc/common /tmp/claude-0/mcsrc/client -name 'ClassName.java'
grep -rn "methodName(" /tmp/claude-0/mcsrc/common/net/minecraft/path/To/Class.java
grep -rln "OldFabricName" /tmp/claude-0/mcsrc/fabric-api --include=*.java
```

The fabric-docs reference mods for each version (`/tmp/claude-0/mcsrc/fabric-docs/reference/{1.21.11,26.1.2,latest}`) are good diffs. `latest` targets 26.2 (`build.gradle: minecraftVersion = "26.2"`), so its imports of `AxeItem`, `HoeItem`, `ShovelItem`, `ConfiguredFeature` and loot `ConstantValue` are already stale for 26.3.
