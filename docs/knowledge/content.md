# Game content in code (Fabric, MC 26.3): index, registries, cross-cutting content types

Scope: adding content (registries, sounds, particles, effects, potions, enchantments, recipes, loot, tags, game rules, stats, damage types, trades) in Java with Fabric API. Verified 2026-11-14 against decompiled MC 26.3 (Mojang names), fabric-api branch 26.3 (0.166.x), Fabric blog posts for 26.1/26.2/26.3 and NeoForge 26.1/26.2/26.3 primers.

Split files:
- `content.md` (this file): registries, data-driven-only list, sounds, particles, effects, potions, brewing, enchantments, recipes, loot, tags, game rules, stats, damage types, villager trades.
- `content-items-blocks.md`: items, data components, tools/armor/food, creative tabs, blocks, block states, block entities, block transformers (strip/till/flatten), fuel/compost, flammability/oxidation, fluids.
- `content-entities.md`: entity types, attributes, mobs, AI (goals/Brain), spawning, projectiles, SynchedEntityData, saving, entity events.

Source path shorthand: `common/...` = `/tmp/claude-0/mcsrc/common/net/minecraft/...`, `client/...` = `/tmp/claude-0/mcsrc/client/net/minecraft/...`, `fapi/<module>` = `/tmp/claude-0/mcsrc/fabric-api/<module>`, `docsref/` = `/tmp/claude-0/mcsrc/fabric-docs/reference/latest/src/main/java/com/example/docs/` (NOTE: fabric-docs `reference/latest` targets **26.2**, not 26.3; several of its snippets are stale on 26.3, called out below).

---

## 0. The 10 things that bite people (1.21.x -> 26.3)

1. **No obfuscation, Mojang names only.** `ResourceLocation` is now `net.minecraft.resources.Identifier` (`Identifier.fromNamespaceAndPath(ns, path)`, `Identifier.withDefaultNamespace(path)`, `Identifier.parse("ns:path")`, `tryParse`, `tryBuild`). `ResourceKey#location()` is now `ResourceKey#identifier()`. Fabric API classes were renamed to Mojang terms in 26.1 (`ItemGroupEvents` -> `CreativeModeTabEvents`, `FabricItemGroup` -> `FabricCreativeModeTab`, `FabricItem.Settings` -> `FabricItem.Properties`, `FabricTrackedDataRegistry` -> `FabricEntityDataRegistry`, `PointOfInterestHelper` -> `PoiHelper`, `LandPathNodeTypesRegistry` -> `LandPathTypeRegistry`, `SculkSensorFrequencyRegistry` -> `VibrationFrequencyRegistry`, `ServerEntityWorldChangeEvents` -> `ServerEntityLevelChangeEvents`, `ComponentTooltipAppenderRegistry` -> `ItemComponentTooltipProviderRegistry`, `FabricBlockStateParticleEffect` -> `FabricBlockParticleOption`, `FabricRecipeManager` -> `FabricRecipeAccess`, `FabricServerRecipeManager` -> `FabricRecipeManager`). Full map: `/tmp/claude-0/mcsrc/fabric-docs/public/assets/develop/porting/fabric-api-26-1-migration-map.xml` (IntelliJ migration map).
2. **Every `Item.Properties` / `BlockBehaviour.Properties` must get `.setId(key)` before the constructor runs** (since 1.21.2; still true). Otherwise `NullPointerException: Item id not set` / `Block id not set` (src: common world/item/Item.java `itemIdOrThrow`, world/level/block/state/BlockBehaviour.java).
3. **Item components live on the registry `Holder`, bound at datapack load** (26.1 `DataComponentInitializers`). `new ItemStack(item)` / `item.getDefaultInstance()` / `item.components()` before a world/datapacks are loaded throws `Components not bound yet` (src: common core/Holder.java). Immutable stacks in static data use `ItemStackTemplate` (26.1). Use `Item.Properties#delayedComponent` for values needing dynamic registry entries.
4. **26.3 removed `AxeItem`, `ShovelItem`, `HoeItem`.** Use `new Item.Properties().axe(material, dmg, speed)` etc. with `Item::new`. Stripping/tilling/flattening is now the data-driven `block_transformer` registry + `DataComponents.BLOCK_TRANSFORMER`. Fabric `StrippableBlockRegistry`, `TillableBlockRegistry`, `FlattenableBlockRegistry` removed -> `BlockTransformerHelper` / `BlockTransformerEvents` (fapi fabric-item-api-v1).
5. **26.3 removed Fabric `FuelRegistry`/`FuelValueEvents`, `CompostingChanceRegistry`/`CompostableRegistry`, `FabricPotionBrewingBuilder`.** Fuel/compost/brewing-fuel are item components (`COOKING_FUEL`, `COMPOSTABLE`, `BREWING_FUEL`); brewing recipes are datapack recipes (`minecraft:brewing`). Modify other mods'/vanilla items with `DefaultItemComponentEvents.MODIFY`.
6. **26.3 removed `Block#codec()` and the `block_type` registry.** Delete `codec()` overrides / `simpleCodec` fields (they no longer compile; `BaseEntityBlock` has no abstract `codec()`).
7. **26.2 split IDs from objects**: vanilla keys live in `ItemIds`, `BlockIds`, `BlockItemIds` (`net.minecraft.references`), `EntityTypeIds`, `BlockEntityTypeIds`, `FluidIds`, `PotionIds`. `EntityType.*` constants moved to `EntityTypes`, `BlockEntityType.*` to `BlockEntityTypes`. Colored/copper variants are collections: `Blocks.WOOL.white()`, `Items.DYE`, `ColorCollection`, `WeatheringCopperCollection`.
8. **26.1 villager trades are data-driven** (`villager_trade`, `trade_set` registries + `tags/villager_trade/...`). Fabric `TradeOfferHelper` is gone.
9. **26.3 recipes, advancements, loot tables, predicates, item modifiers, number providers, slot sources are "reloadable" datapack registries** (`RegistryDataLoader.RELOADABLE_REGISTRIES`). Loot/recipe references are `Holder`s; datagen providers became registry bootstraps. `RecipeSerializer` is a record `(MapCodec, StreamCodec)` since 26.1.
10. **`Registry.register(registry, "name", v)` uses `Identifier.parse`**, so a bare `"name"` lands in `minecraft:`. Always pass a namespaced `Identifier` or `ResourceKey` (src: common core/Registry.java:106). The Fabric 26.1 blog's recipe-serializer snippet using `"mod_recipe"` would register `minecraft:mod_recipe`.

---

## 1. Registries

### 1.1 Static (built-in) registries
`net.minecraft.core.registries.BuiltInRegistries` (fields verified, src: common core/registries/BuiltInRegistries.java): `BLOCK, ITEM, ENTITY_TYPE, BLOCK_ENTITY_TYPE, FLUID, SOUND_EVENT, PARTICLE_TYPE, MOB_EFFECT, POTION, ATTRIBUTE, DATA_COMPONENT_TYPE, CREATIVE_MODE_TAB, RECIPE_TYPE, RECIPE_SERIALIZER, RECIPE_BOOK_CATEGORY, RECIPE_DISPLAY, SLOT_DISPLAY, MENU, GAME_EVENT, GAME_RULE, CUSTOM_STAT, STAT_TYPE, VILLAGER_PROFESSION, VILLAGER_TYPE, POINT_OF_INTEREST_TYPE, MEMORY_MODULE_TYPE, SENSOR_TYPE, ACTIVITY, LOOT_POOL_ENTRY_TYPE, LOOT_FUNCTION_TYPE, LOOT_CONDITION_TYPE, CONTEXT_FLOAT_PROVIDER_TYPE, CONTEXT_INT_PROVIDER_TYPE, FLOAT_PROVIDER_TYPE, INT_PROVIDER_TYPE, BLOCK_PREDICATE_TYPE, BLOCK_STATE_PROVIDER_TYPE, FEATURE_TYPE, CARVER_TYPE, PLACEMENT_MODIFIER_TYPE, STRUCTURE_TYPE, ..., ENCHANTMENT_EFFECT_COMPONENT_TYPE, ENCHANTMENT_ENTITY_EFFECT_TYPE, ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE, ENCHANTMENT_VALUE_EFFECT_TYPE, ENCHANTMENT_LEVEL_BASED_VALUE_TYPE, ENCHANTMENT_PROVIDER_TYPE, CONSUME_EFFECT_TYPE, DATA_COMPONENT_PREDICATE_TYPE, ENTITY_SUB_PREDICATE_TYPE, TRIGGER_TYPES, MAP_DECORATION_TYPE, DIALOG_TYPE, ENVIRONMENT_ATTRIBUTE, SLOT_SOURCE_TYPE, CONTEXT_KEY_SET, DATA_COMPONENT_INITIALIZERS, ...`. Gone: `BLOCK_TYPE` (26.3), `FEATURE` is now the dynamic configured-feature registry (`FEATURE_TYPE` holds feature codecs, 26.3).

Registry keys for `ResourceKey.create`: `net.minecraft.core.registries.Registries.ITEM`, `.BLOCK`, `.ENTITY_TYPE`, `.DAMAGE_TYPE`, `.ENCHANTMENT`, `.LOOT_TABLE`, `.RECIPE`, `.BLOCK_TRANSFORMER`, `.CONTEXT_INT_PROVIDER`, ...

```java
// src: common core/Registry.java
static <V, T extends V> T register(Registry<V> registry, ResourceKey<V> key, T value);
static <V, T extends V> T register(Registry<V> registry, Identifier id, T value);
static <T> T register(Registry<? super T> registry, String name, T value); // Identifier.parse(name)!
static <R, T extends R> Holder.Reference<T> registerForHolder(Registry<R> registry, ResourceKey<R> key, T value);
static <R, T extends R> Holder.Reference<T> registerForHolder(Registry<R> registry, Identifier id, T value);
```
Lookup: `registry.getValue(ResourceKey|Identifier)` (nullable), `getOptional(...)`, `getValueOrThrow(key)`, `get(Identifier)` -> `Optional<Holder.Reference<T>>`, `getKey(T)` -> Identifier, `getResourceKey(T)`, `containsKey(...)`, `wrapAsHolder(T)`. `HolderGetter#getOrThrow(ResourceKey)` / `getOrThrow(TagKey)` -> `HolderSet.Named`.

Canonical mod pattern (ids separate from objects, like vanilla since 26.2):
```java
public static final String MOD_ID = "example-mod";
public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }

public final class ModItemIds {
    public static final ResourceKey<Item> RUBY = ResourceKey.create(Registries.ITEM, ExampleMod.id("ruby"));
}
public final class ModBlockItemIds { // net.minecraft.references.BlockItemId(ResourceKey<Block> block, ResourceKey<Item> item)
    public static final BlockItemId RUBY_BLOCK = new BlockItemId(
        ResourceKey.create(Registries.BLOCK, ExampleMod.id("ruby_block")),
        ResourceKey.create(Registries.ITEM, ExampleMod.id("ruby_block")));
}
```
(`BlockItemId.create(String)` exists but uses the `minecraft` namespace; build the record yourself. src: common references/BlockItemId.java.)

Class-loading: static-final registration fields only run when the class is loaded, so call an empty `ModItems.initialize()` from `ModInitializer#onInitialize`. Register everything during mod init; static registries are frozen after init and registering later throws.

### 1.2 Dynamic (datapack) registries
Loaded per world from `data/<ns>/<registry path>/...json`. List (src: common resources/RegistryDataLoader.java):
- `WORLD_REGISTRIES`: dimension_type, worldgen/biome, chat_type, worldgen/configured_carver, worldgen/feature (26.3), worldgen/placed_feature, worldgen/structure, structure_set, processor_list, template_pool, noise_settings, noise, density_function, worldgen/material_rule (26.3), worldgen/material_condition (26.3), world_preset, flat_level_generator_preset, trim_pattern, trim_material, trial_spawner, wolf/pig/frog/cat/cow/chicken/zombie_nautilus `_variant`, wolf/pig/cat/cow/chicken `_sound_variant`, sulfur_cube_archetype (26.2), painting_variant, damage_type, multi_noise_biome_source_parameter_list, banner_pattern, enchantment, enchantment_provider, jukebox_song, instrument, test_environment, test_instance, dialog, world_clock, timeline, **villager_trade, trade_set** (26.1), **decorated_pot_pattern**, **block_transformer** (26.3), **worldgen/block_state_provider** (26.3).
- `DIMENSION_REGISTRIES`: dimension (level stems).
- `RELOADABLE_REGISTRIES` (26.3, reloaded by `/reload`, NOT synced to clients): loot_table, predicate, context_float_provider, context_int_provider, item_modifier, slot_source, advancement, recipe.
- `SYNCHRONIZED_REGISTRIES`: subset sent to clients (biome, chat_type, trims, variants, painting, damage_type, banner_pattern, enchantment, jukebox_song, instrument, dialog, ...).

Code never `Registry.register`s into these; you ship JSON (or datagen via `FabricDynamicRegistryProvider`) and reference by `ResourceKey`. Resolve at runtime: `level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key)`; in item/datagen code use the `HolderLookup.Provider` you are handed.

Custom dynamic registries (fapi fabric-registry-sync-v0 `DynamicRegistries`):
```java
public static final ResourceKey<Registry<MyData>> MY_DATA = ResourceKey.createRegistryKey(ExampleMod.id("my_data"));
DynamicRegistries.register(MY_DATA, MyData.CODEC);                  // not synced
DynamicRegistries.registerSynced(MY_DATA, MyData.CODEC);            // synced (same codec)
DynamicRegistries.registerSynced(MY_DATA, serverCodec, clientCodec, DynamicRegistries.SyncOption.SKIP_WHEN_EMPTY);
DynamicRegistries.registerReloadable(MY_DATA, MyData.CODEC);        // 26.3: reloadable, never synced
```
Files for a modded registry key `example-mod:my_data` live at `data/<entry ns>/example-mod/my_data/<entry>.json` and tags at `data/<ns>/tags/example-mod/my_data/`: Fabric's `RegistriesMixin` prefixes non-`minecraft` registry namespaces in `Registries.elementsDirPath`/`tagsDirPath` (vanilla registries keep the bare path, e.g. `item`, `tags/item`) (src: fapi fabric-registry-sync-v0 mixin/registry/sync/RegistriesMixin.java, test RegistryKeysTest asserts `registry-keys/test`). `DynamicRegistrySetupCallback.EVENT` lets you hook `registerEntryAdded` on dynamic registries. Example: `docsref/dynamic_registries/`.

### 1.3 Custom static registries + registry sync
```java
// src: fapi fabric-registry-sync-v0 api/event/registry/FabricRegistryBuilder.java
public static final ResourceKey<Registry<Spell>> SPELL_KEY = ResourceKey.createRegistryKey(ExampleMod.id("spell"));
public static final MappedRegistry<Spell> SPELLS = FabricRegistryBuilder.create(SPELL_KEY)
        .attribute(RegistryAttribute.SYNCED)   // SYNCED | MODDED | OPTIONAL | SAVE_DATA_VALIDATED
        .buildAndRegister();
// createDefaulted(key, defaultId) -> DefaultedMappedRegistry
```
(Javadoc in that file says `createSimple`; the method is `create`.)

Registry sync: Fabric remaps numeric IDs of modded static registries (items, blocks, ...) between server and client on join. A client missing a registered entry from a synced registry is disconnected unless the registry is `OPTIONAL`. Events: `RegistryEntryAddedCallback.event(registry)`, `RegistryIdRemapCallback`. Don't persist raw int IDs; persist `Identifier`s/codecs.

### 1.4 Content that is data-driven only (no code registration path)
Ship JSON (see the datapack doc) and reference by `ResourceKey`: painting variants (+ `tags/painting_variant/placeable`), jukebox songs (`Item.Properties#jukeboxPlayable(ResourceKey<JukeboxSong>)`), banner patterns, armor trim materials/patterns (`Item.Properties#trimMaterial(key)`), instruments (goat horns), decorated pot patterns (`Item.Properties#potPattern(key)`), enchantments, enchantment providers, damage types, all mob variants and sound variants, sulfur cube archetypes, chat types, dialogs, world clocks/timelines, trial spawner configs, villager trades/trade sets, block transformers, block state providers, number providers (`context_int_provider`/`context_float_provider`), predicates, item modifiers, loot tables, recipes (incl. brewing), advancements, biomes/worldgen, dimension types, test instances. Code can still register the **types/codecs** these use (e.g. a new enchantment effect type, a new block state provider type, a new loot function type).

---

## 2. Sounds
```java
// docsref/sound/CustomSounds.java, src: common sounds/SoundEvent.java
public static final SoundEvent METAL_WHISTLE = register("metal_whistle");
private static SoundEvent register(String name) {
    Identifier id = ExampleMod.id(name);
    return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    // or SoundEvent.createFixedRangeEvent(id, 16f)
}
```
Holder form for components (`Consumable.sound`, `Equippable.setEquipSound`): `Registry.registerForHolder(...)` or `BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ev)`. Play: `level.playSound(@Nullable Player except, BlockPos, SoundEvent, SoundSource, vol, pitch)` (server: pass `null` to send to everyone; client-predicted: pass the acting player). Assets: `assets/<ns>/sounds.json` + `assets/<ns>/sounds/*.ogg`; `SoundType` for blocks is a plain constructor `new SoundType(vol, pitch, break, step, place, hit, fall)`. Client sound instances: fabric-sound-api-v1 (see client doc).

## 3. Particles
```java
// docsref/ExampleMod.java, src: fapi fabric-particles-v1 FabricParticleTypes
public static final SimpleParticleType SPARKLE = FabricParticleTypes.simple();        // simple(alwaysSpawn)
Registry.register(BuiltInRegistries.PARTICLE_TYPE, ExampleMod.id("sparkle"), SPARKLE);
// Parameterized: FabricParticleTypes.complex(MapCodec<T>, StreamCodec<? super RegistryFriendlyByteBuf, T>) or the Function<ParticleType<T>, ...> overloads
```
Spawn server-side: `serverLevel.sendParticles(type, x, y, z, count, dx, dy, dz, speed)`; client: `level.addParticle(...)`. Client needs `assets/<ns>/particles/<id>.json` + a provider via `ParticleProviderRegistry` (renamed from `ParticleFactoryRegistry`; client doc). `FabricBlockParticleOption` for block-state particles.

## 4. Status effects (MobEffect)
```java
// docsref/effect/TaterEffect.java, src: common world/effect/MobEffect.java
public class TaterEffect extends MobEffect {
    public TaterEffect() { super(MobEffectCategory.BENEFICIAL, 0xe9b8b3); } // (category, color[, ParticleOptions])
    @Override public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) { return true; }
    @Override public boolean applyEffectTick(ServerLevel level, LivingEntity e, int amp) { /*...*/ return true; } // false = remove effect
    // hooks: onEffectStarted(LivingEntity,int), onEffectAdded(LivingEntity,int),
    //        onMobRemoved(ServerLevel,LivingEntity,int,Entity.RemovalReason), onMobHurt(ServerLevel,LivingEntity,int,DamageSource,float)
}
public static final Holder<MobEffect> TATER = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, ExampleMod.id("tater"), new TaterEffect());
// attribute effects: new X().addAttributeModifier(Attributes.MOVEMENT_SPEED, id, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
entity.addEffect(new MobEffectInstance(TATER, 200, 0));  // MobEffectInstance takes Holder<MobEffect>
```
Effects are referenced as `Holder<MobEffect>` everywhere (`MobEffects.POISON` is a Holder). Lang key `effect.<ns>.<path>`; icon `assets/<ns>/textures/mob_effect/<path>.png`. Fabric hooks: `ServerMobEffectEvents` (6 events: allow/before/after add & remove) and `FabricMobEffect` (fapi fabric-entity-events-v1 `entity/event/v1/effect/`).

## 5. Potions and brewing
```java
// docsref/potion/ExampleModPotions.java, src: common world/item/alchemy/Potion.java
public static final ResourceKey<Potion> TATER_KEY = ResourceKey.create(Registries.POTION, ExampleMod.id("tater"));
public static final Holder<Potion> TATER = Registry.registerForHolder(BuiltInRegistries.POTION, TATER_KEY,
        new Potion("tater", new MobEffectInstance(ExampleModEffects.TATER, 3600, 0)));
```
- **26.3: brewing recipes are datapack recipes** (`RecipeType.BREWING`, `BrewingRecipe`, `BrewingInput`). `FabricPotionBrewingBuilder` is removed (docsref still shows it: stale). Vanilla shape, one file per container (`data/minecraft/recipe/brewing/*.json`, 279 files):
```json
{ "type": "minecraft:brewing",
  "input":   { "item": "minecraft:lingering_potion", "potion_contents": { "potions": "minecraft:awkward" } },
  "reagent": { "item": "minecraft:blaze_powder" },
  "output":  { "id": "minecraft:lingering_potion", "components": { "minecraft:potion_contents": { "potion": "minecraft:strength" } } } }
```
- Datagen: `FabricBrewingProvider` (extends vanilla `BrewingProvider`; already registers vanilla containers potion/splash/lingering) with `buildMix(Holder<Potion> in, Item reagent, Holder<Potion> out)` / `buildStartMix(...)`, invoked inside a `FabricRecipeProvider` (src: fapi fabric-data-generation-api-v1 testmod `DataGeneratorTestEntrypoint` line ~238).
- Brewing stand fuel is `DataComponents.BREWING_FUEL` (`Item.Properties#brewingFuel(ResourceKey<ContextIntProvider> uses)`), see items doc.

## 6. Enchantments
Enchantments are a dynamic registry (JSON in `data/<ns>/enchantment/`). There is no code `Enchantment` class to subclass. What code can do:
- Reference: `ResourceKey<Enchantment>`; resolve `registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key)`; level on a stack: `EnchantmentHelper.getItemEnchantmentLevel(Holder<Enchantment>, ItemInstance)`.
- Register new **effect types** referenced from enchantment JSON:
```java
// docsref/enchantment/ModEnchantmentEffects.java + effect/LightningEnchantmentEffect.java
public record LightningEnchantmentEffect(LevelBasedValue amount) implements EnchantmentEntityEffect {
    public static final MapCodec<LightningEnchantmentEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        LevelBasedValue.CODEC.fieldOf("amount").forGetter(LightningEnchantmentEffect::amount)).apply(i, LightningEnchantmentEffect::new));
    @Override public void apply(ServerLevel level, int enchLevel, EnchantedItemInUse ctx, Entity target, Vec3 pos) {
        EntityTypes.LIGHTNING_BOLT.spawn(level, target.blockPosition(), EntitySpawnReason.TRIGGERED);
    }
    @Override public MapCodec<? extends EnchantmentEntityEffect> codec() { return CODEC; }
}
Registry.register(BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE, ExampleMod.id("lightning_effect"), LightningEnchantmentEffect.CODEC);
```
  Also `ENCHANTMENT_LOCATION_BASED_EFFECT_TYPE`, `ENCHANTMENT_VALUE_EFFECT_TYPE`, `ENCHANTMENT_LEVEL_BASED_VALUE_TYPE`, `ENCHANTMENT_PROVIDER_TYPE`, and new effect **component types** in `ENCHANTMENT_EFFECT_COMPONENT_TYPE` (registered as `DataComponentType`s; vanilla pattern in common world/item/enchantment/EnchantmentEffectComponents.java). Then use `"effects": { "minecraft:post_attack": [ { "effect": { "type": "example-mod:lightning_effect", ... } } ] }` in JSON.
- Fabric hooks (fapi fabric-item-api-v1 `EnchantmentEvents`): `ALLOW_ENCHANTING` (`TriState allowEnchanting(Holder<Enchantment>, ItemStack, EnchantingContext)`), `MODIFY_WITH_LOOKUP` (`(ResourceKey<Enchantment>, Enchantment.Builder, ResourceSource, RegistryOps.RegistryInfoLookup)`); `MODIFY` is deprecated. Per-item: override `FabricItem#canBeEnchantedWith(ItemStack, Holder<Enchantment>, EnchantingContext)`.
- Item enchantability: `Item.Properties#enchantable(int)` + item tags `minecraft:enchantable/*` (data).
- Enchanting-table power from blocks: override `FabricBlock#getProvidedEnchantmentPower(BlockState, BlockGetter, BlockPos)` (26.3).
- Datagen: `docsref/` client `datagen/ExampleModEnchantmentGenerator.java`.

## 7. Recipes
### 7.1 Model (26.1+)
```java
// src: common world/item/crafting/Recipe.java
public interface Recipe<T extends RecipeInput> {
    boolean matches(T input, Level level);
    ItemStack assemble(T input);                       // 26.1: no HolderLookup.Provider param
    boolean showNotification();  String group();       // no longer default (26.1)
    RecipeSerializer<? extends Recipe<T>> getSerializer();
    RecipeType<? extends Recipe<T>> getType();
    PlacementInfo placementInfo();                     // PlacementInfo.NOT_PLACEABLE / create(ingredients)
    RecipeBookCategory recipeBookCategory();
    default boolean isSpecial() { return false; }      // true = not shown in recipe book
    default List<RecipeDisplay> display() { return List.of(); }
}
public record RecipeSerializer<T extends Recipe<?>>(MapCodec<T> codec, @Deprecated StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {}
```
Helper bases: `NormalCraftingRecipe` (takes `Recipe.CommonInfo` + `CraftingRecipe.CraftingBookInfo`, implement `createPlacementInfo()`), `CustomRecipe` (special crafting, no-arg ctor), `SimpleSmithingRecipe`, `SingleItemRecipe`, `AbstractCookingRecipe` (`CookingBookInfo`). Shared info records `Recipe.CommonInfo(boolean showNotification)` with `MAP_CODEC`/`STREAM_CODEC`, `Recipe.BookInfo.mapCodec(...)`. Results are `ItemStackTemplate` (codec `ItemStackTemplate.CODEC`, net `ItemStackTemplate.STREAM_CODEC`), `template.create()` -> ItemStack. Vanilla serializers live in `RecipeSerializers` (not `RecipeSerializer.*`).

Inputs: implement `RecipeInput` (`ItemStack getItem(int)`, `int size()`); vanilla `CraftingInput`, `SingleRecipeInput`, `SmithingRecipeInput`, `BrewingInput`.

### 7.2 Custom recipe type (docsref/recipe/, verified against 26.3 APIs)
```java
public record UpgradingRecipeInput(ItemStack base, ItemStack upgrade) implements RecipeInput {
    @Override public ItemStack getItem(int i) { return i == 0 ? base : i == 1 ? upgrade : ItemStack.EMPTY; }
    @Override public int size() { return 2; }
}
public class UpgradingRecipe implements Recipe<UpgradingRecipeInput> {
    public static final MapCodec<UpgradingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result),
        Ingredient.CODEC.fieldOf("base").forGetter(r -> r.base),
        Ingredient.CODEC.fieldOf("upgrade").forGetter(r -> r.upgrade)).apply(i, UpgradingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpgradingRecipe> STREAM_CODEC = StreamCodec.composite(
        ItemStackTemplate.STREAM_CODEC, r -> r.result, Ingredient.CONTENTS_STREAM_CODEC, r -> r.base,
        Ingredient.CONTENTS_STREAM_CODEC, r -> r.upgrade, UpgradingRecipe::new);
    // matches -> base.test(in.base()) && upgrade.test(in.upgrade()); assemble -> result.create()
    // placementInfo -> PlacementInfo.NOT_PLACEABLE; isSpecial -> true; recipeBookCategory -> a registered category
}
public static final RecipeSerializer<UpgradingRecipe> UPGRADING_SERIALIZER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
        ExampleMod.id("upgrading"), new RecipeSerializer<>(UpgradingRecipe.CODEC, UpgradingRecipe.STREAM_CODEC));
public static final RecipeType<UpgradingRecipe> UPGRADING_TYPE = Registry.register(BuiltInRegistries.RECIPE_TYPE,
        ExampleMod.id("upgrading"), new RecipeType<UpgradingRecipe>() {});
public static final RecipeBookCategory UPGRADING_BOOK = Registry.register(BuiltInRegistries.RECIPE_BOOK_CATEGORY,
        ExampleMod.id("upgrading"), new RecipeBookCategory());   // empty class implementing ExtendedRecipeBookCategory
```
Query (server only): `serverLevel.recipeAccess()` returns `RecipeManager`; `getRecipeFor(RecipeType<T>, I input, Level)` -> `Optional<RecipeHolder<T>>`; `byKey(ResourceKey<Recipe<?>>)`. `RecipeHolder` is a record `(ResourceKey<Recipe<?>> id, T value)`. `Level#recipeAccess()` on the client is a limited `RecipeAccess` (recipes are **not** synced to clients since 1.21.2).

Client sync of full recipes: `RecipeSynchronization.synchronizeRecipeSerializer(UPGRADING_SERIALIZER)` (fapi fabric-recipe-api-v1 `recipe/v1/sync/`) then read them client-side via `SynchronizedRecipes`. Custom ingredients: `CustomIngredient`/`CustomIngredientSerializer`, built-ins in `DefaultCustomIngredients` (`all`, `any`, `difference`, `components(Ingredient, DataComponentPatch)`, `customData`).

Ingredient from a tag at init: `Ingredient.of(BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ITEM).getOrThrow(tag))`; in datagen use the provider's `HolderGetter<Item>`. `Ingredient.of(ItemLike...)`.

### 7.3 Recipe book categories
`RecipeBookCategory` is a registry object (`BuiltInRegistries.RECIPE_BOOK_CATEGORY`, vanilla in `RecipeBookCategories`). Showing a custom tab in a vanilla-style recipe book UI needs enum extension of `RecipeBookType`/client screen work; fabric-docs leaves this as TODO (docsref `mixin/class_tweakers/RecipeBookTypeMixin.java`). For non-book recipes return any registered category and `isSpecial() = true`.

### 7.4 Recipe datagen (26.3)
```java
// src: fapi fabric-data-generation-api-v1 FabricRecipeProvider + testmod DataGeneratorTestEntrypoint
public class ModRecipeProvider extends FabricRecipeProvider {
    public ModRecipeProvider(FabricPackOutput out, CompletableFuture<HolderLookup.Provider> reg) { super(out, reg); }
    @Override protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries,
            BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
        return new RecipeProvider(recipes, advancements) {
            @Override public void buildRecipes() {
                shapeless(RecipeCategory.MISC, ModItems.RUBY, 9).requires(ModBlocks.RUBY_BLOCK)
                    .unlockedBy("has_ruby_block", has(ModBlocks.RUBY_BLOCK)).save(this.output);
                shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.RUBY_BLOCK).define('#', ModItems.RUBY)
                    .pattern("###").pattern("###").pattern("###").unlockedBy("has_ruby", has(ModItems.RUBY)).save(this.output);
            }
        };
    }
    @Override public String getName() { return "Recipes"; }
}
```
`withConditions(output, ResourceCondition...)` for conditional recipes. Builders take `ItemStackTemplate` for results (26.1), `oreSmelting/oreBlasting` take a `CookingBookCategory`, cooking builders no longer take a `RecipeSerializer`. Full datagen setup: toolchain.md section 7 "Datagen".

## 8. Loot tables
- Block drops default to `data/<ns>/loot_table/blocks/<path>.json` (key from block id; `BlockBehaviour.Properties#noLootTable()`, `overrideLootTable(Optional<ResourceKey<LootTable>>)`). Entities: `entities/<path>`, `EntityType.Builder#noLootTable()`.
- Loot tables are a reloadable registry in 26.3; references are `Holder<LootTable>`/`ResourceKey<LootTable>`; conditions/functions can be registered (`predicate`, `item_modifier`) and referenced by id; `NumberProvider` split into `ContextIntProvider`/`ContextFloatProvider` (helpers `ContextIntProviders.exactly(n)`). Custom loot condition/function types: `LOOT_CONDITION_TYPE`/`LOOT_FUNCTION_TYPE` with a `MapCodec`; `LootItemConditionalFunction` ctor now takes a single `Optional<Holder<LootItemCondition>>` (26.3 primer).
- Modify vanilla/other-mod loot (fapi fabric-loot-api-v3 `LootTableEvents`; v2 removed in 26.1):
```java
// src: fapi fabric-loot-api-v3 testmod LootTest.java
LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
    if (source.isBuiltin() && Blocks.WOOL.white().getLootTable().orElse(null) == key) {
        tableBuilder.pool(LootPool.lootPool().add(LootItem.lootTableItem(Items.GOLD_INGOT))
            .when(ExplosionCondition.survivesExplosion()).build());
    }
    // add entries to existing pools:
    tableBuilder.modifyPools(pool -> pool.add(LootItem.lootTableItem(Items.EMERALD)));
    // nested table reference now takes a Holder:
    // NestedLootTable.lootTableReference(registries.getOrThrow(BuiltInLootTables.ARMADILLO_SHED))
});
LootTableEvents.REPLACE.register((key, original, source, registries) -> null /* or replacement LootTable */);
LootTableEvents.ALL_LOADED.register((resourceManager, lootRegistry) -> {});
LootTableEvents.MODIFY_DROPS.register((Holder<LootTable> table, LootContext ctx, List<ItemStack> drops) -> {});
```
  `LootTableSource`: `VANILLA`, `MOD`, `DATA_PACK`, `REPLACED` (check `isBuiltin()`; only modify builtin tables to respect datapack overrides). Entity loot keys: `EntityTypes.X.getDefaultLootTable()`.
- Datagen: `FabricBlockLootSubProvider` / `FabricEntityLootSubProvider` / `SimpleFabricLootTableSubProvider` (renamed from `*LootTableProvider`/`*Generator` in 26.1). Vanilla 26.3 loot providers take a `LootTableSubProvider.Context` instead of a `HolderLookup.Provider`.

## 9. Tags
```java
public static final TagKey<Item> GEMS = TagKey.create(Registries.ITEM, ExampleMod.id("gems"));
stack.is(GEMS); state.is(BlockTags.MINEABLE_WITH_PICKAXE); entity.getType().is(EntityTypeTags.UNDEAD);
holder.is(tag); BuiltInRegistries.ITEM.getTagOrEmpty(tag)  // iterate
```
- JSON at `data/<ns>/tags/<registry path>/<name>.json` (`tags/item`, `tags/block`, `tags/entity_type`, `tags/enchantment`, `tags/villager_trade/<profession>/...`, ...).
- Convention tags `c:` (fapi fabric-convention-tags-v2): `ConventionalItemTags` (e.g. `TOOLS`, `MELEE_WEAPON_TOOLS`, `WRENCH_TOOLS`, ingots/gems/ores...), `ConventionalBlockTags`, `ConventionalEntityTypeTags`, `ConventionalBiomeTags`, `ConventionalFluidTags`, `ConventionalEnchantmentTags`, `ConventionalPotionTags`, `ConventionalStructureTags`, `ConventionalBlockItemTags`; `TagUtil.isIn(tag, entry)`, `TagUtil.C_TAG_NAMESPACE = "c"`. Convention-tags v1 removed in 26.1, deprecated tags slated for 1.22 removed.
- Tag entry removal (fapi fabric-tag-api-v1, since 0.150.1/26.2): `"fabric:remove": ["minecraft:brick"]` next to `"values"`.
- Datagen 26.2: tag providers take `ResourceKey`s/`TagKey`s only (no raw objects); `FabricTagsProvider.ItemTagsProvider` etc. (renamed from `FabricTagProvider.*`).

## 10. Game rules (fapi fabric-game-rule-api-v1)
```java
// docsref/gamerule/ExampleModGameRules.java
public static final GameRule<Boolean> BAD_VISION = GameRuleBuilder.forBoolean(false)
        .category(GameRuleCategory.MISC).buildAndRegister(ExampleMod.id("bad_vision"));      // registers to BuiltInRegistries.GAME_RULE
public static final GameRule<Integer> MY_INT = GameRuleBuilder.forInteger(1).range(0, 10).buildAndRegister(ExampleMod.id("my_int"));
// forDouble(...), forEnum(E default); .codec(...), .argumentType(...), .requiredFeatures(...)
boolean v = serverLevel.getGameRules().get(BAD_VISION);     // GameRules#get(GameRule<T>) -> T
GameRuleEvents.changeCallback(BAD_VISION).register(...);    // ValueUpdate<T>
```
Game rules are registry entries keyed by `Identifier` (vanilla ids are snake_case, e.g. `minecraft:mob_griefing`, `GameRules.MOB_GRIEFING`), package `net.minecraft.world.level.gamerules`.

## 11. Statistics
```java
// docsref/stats/ModStats.java, mirrors common stats/Stats.java makeCustomStat
public static final Identifier FRIENDSHIPS_MADE = register("friendships_made", StatFormatter.DEFAULT);
private static Identifier register(String name, StatFormatter f) {
    Identifier id = ExampleMod.id(name);
    Registry.register(BuiltInRegistries.CUSTOM_STAT, id, id);
    Stats.CUSTOM.get(id, f);       // creates the Stat
    return id;
}
player.awardStat(FRIENDSHIPS_MADE);  player.awardStat(Stats.ITEM_USED.get(item));
```
Lang: `stat.<ns>.<path>`.

## 12. Damage types
Data-driven (`data/<ns>/damage_type/<path>.json`: `message_id`, `scaling`, `exhaustion`, optional `effects`, `death_message_type`) + tags (`tags/damage_type/...`, e.g. `minecraft:bypasses_armor`, `is_fire`). Code:
```java
public static final ResourceKey<DamageType> TATER = ResourceKey.create(Registries.DAMAGE_TYPE, ExampleMod.id("tater"));
DamageSource src = level.damageSources().source(TATER);            // also source(key, cause) / source(key, direct, causing)
entity.hurtServer(serverLevel, src, 5.0f);                          // hurt() is gone; hurtServer is server-only
```
(src: common world/damagesource/DamageSources.java:84; docsref/damage/TaterBlock.java.) Datagen: `FabricDynamicRegistryProvider` (docsref client `datagen/ExampleModDamageTypesProvider.java`). Lang: `death.attack.<message_id>`.

## 13. Villager trades and professions (26.1+, data-driven)
- Trade: `data/<ns>/villager_trade/<path>.json`:
```json
{ "wants": { "id": "minecraft:emerald", "count": 3 }, "gives": { "id": "minecraft:kelp" }, "max_uses": 12, "reputation_discount": 0.05 }
```
- Trade set per level: `data/minecraft/trade_set/<profession>/level_1.json` = `{ "amount": 2, "random_sequence": "...", "trades": "#minecraft:<profession>/level_1" }`.
- Add a trade to a vanilla profession: add your trade id to tag `data/minecraft/tags/villager_trade/<profession>/level_N.json` (`"replace": false`). Wandering trader trades live under `villager_trade/wandering_trader/`.
- Fabric `TradeOfferHelper` removed. Remaining code hooks (fapi fabric-content-registries-v0 `VillagerInteractionRegistries`): `registerGatherableItem(ItemLike)`, `registerCompostable(ItemLike)` (farmer composts it), `registerGiftLootTable(ResourceKey<VillagerProfession>, ResourceKey<LootTable>)` (hero of the village). Villager food value is `DataComponents.VILLAGER_FOOD` / `Item.Properties#villagerFood(int nutrition)` (26.3). Extra trade cost: `DataComponents.ADDITIONAL_TRADE_COST`.
- New profession: register a POI with `PoiHelper.register(Identifier id, int ticketCount, int searchDistance, Block... blocks)` (fapi fabric-object-builder-api-v1 `world/poi/PoiHelper.java`), then a `VillagerProfession` in `BuiltInRegistries.VILLAGER_PROFESSION`. It is a record `(Component name, Predicate<Holder<PoiType>> heldJobSite, Predicate<Holder<PoiType>> acquirableJobSite, ImmutableSet<Item> requestedItems, ImmutableSet<Block> secondaryPoi, @Nullable SoundEvent workSound, Int2ObjectMap<ResourceKey<TradeSet>> tradeSetsByLevel)` (src: common world/entity/npc/villager/VillagerProfession.java). Trade sets are referenced per level, so ship `trade_set` JSON for your keys. UNVERIFIED end-to-end (no testmod).

## 14. Other registrable things (quick pointers)
- **Attributes**: `Registry.registerForHolder(BuiltInRegistries.ATTRIBUTE, id, new RangedAttribute(id.toLanguageKey(), default, min, max).setSyncable(true))` (docsref/entity/attribute/ModAttributes.java). See entities doc.
- **Game events**: `BuiltInRegistries.GAME_EVENT`; vibration frequency via `VibrationFrequencyRegistry.register(ResourceKey<GameEvent>, int)`.
- **Criteria/advancement triggers**: `BuiltInRegistries.TRIGGER_TYPES`; 26.3 `ContextAwarePredicate` removed, instances take `Optional<Holder<LootItemCondition>>` (docsref/advancement/).
- **Menus**: `BuiltInRegistries.MENU` (fapi fabric-menu-api-v1; menu doc).
- **Saved data / attachments**: fabric-data-attachment-api-v1 to attach data to vanilla entities, block entities, chunks, levels (docsref/attachment/).
- **Number providers / block state providers / block predicates**: register `MapCodec`s to `CONTEXT_INT_PROVIDER_TYPE`, `CONTEXT_FLOAT_PROVIDER_TYPE`, `BLOCK_STATE_PROVIDER_TYPE` (26.3: registries hold the `MapCodec` directly; `BlockStateProviderType`, `PlacementModifierType`, `StructurePlacementType` removed; method `type()` renamed `codec()`), `BLOCK_PREDICATE_TYPE`.

## 15. Where to look
- Vanilla registration patterns: common world/item/Items.java (`registerItem`, `registerBlock`, `registerSpawnEgg` at bottom), world/level/block/Blocks.java (`register` ~line 5648), world/entity/EntityTypes.java (`register` ~1155), world/level/block/entity/BlockEntityTypes.java.
- Fabric testmods: `fapi/<module>/src/testmod/java/...` (item: `CustomBlockTransformsTest`, `DefaultItemComponentTest`, `ModifyComponentsInPropertiesTestSetup`; loot: `LootTest`; object-builder: `BlockEntityTypeBuilderTest`, `EntityDataAccessorTest`; content-registries: `ContentRegistryTest`; biome: `WorldgenProvider`).
- Porting notes: NeoForge primers https://docs.neoforged.net/primer/docs/26.1/ , /26.2/ , /26.3/ ; Fabric blog https://fabricmc.net/2026/03/14/261.html , /2026/06/15/262.html , /2026/09/15/263.html .
