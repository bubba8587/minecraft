# Resource loading, dynamic registries, resource conditions, config, server-side-only mods (26.3)

Scope: fabric-resource-loader-v1 (reload listeners, built-in packs, data resource store), data-driven registries (`DynamicRegistries`), fabric-resource-conditions-api-v1, a hand-rolled Codec config, and what makes a mod work with vanilla clients, for MC 26.3 / Fabric API 0.166.2+26.3. Verified 2026-11-14 against fabric-api 26.3 sources/testmods, decompiled 26.3, Modrinth/maven for library versions.

## 1. fabric-resource-loader-v1

Package `net.fabricmc.fabric.api.resource.v1` (src: fabric-api fabric-resource-loader-v1). The old `fabric-resource-loader-v0` (`ResourceManagerHelper`, `IdentifiableResourceReloadListener`, `SimpleSynchronousResourceReloadListener`) is in `deprecated/`; do not use it for new code.

### Bundled vs built-in packs
- Bundled: everything under your jar's `assets/` and `data/` is always loaded (hidden per-mod pack after vanilla). Programmer Art / High Contrast variants: `resources/programmer_art/assets/`, `resources/high_contrast/assets/`.
- Built-in optional pack: put files in `resources/resourcepacks/<path>/{assets,data,pack.mcmeta}` and register:
```java
FabricLoader.getInstance().getModContainer("mymod").ifPresent(c ->
        ResourceLoader.registerBuiltinPack(Identifier.fromNamespaceAndPath("mymod", "dark_ui"), c,
                Component.literal("MyMod Dark UI"), PackActivationType.DEFAULT_ENABLED));   // NORMAL, DEFAULT_ENABLED, ALWAYS_ENABLED
```
  Overloads: `(id, container, activation)`, `(id, container, displayName, activation)`, `(id, String path, container, [displayName,] activation)`. Returns false on failure.

### Reload listeners
```java
// server data (datapacks): DataResourceLoader
DataResourceLoader.get().registerReloadListener(id("skills"), new SkillLoader());
DataResourceLoader.get().registerReloadListener(id("needs_registries"), registries -> new MyListener(registries)); // factory gets HolderLookup.Provider
DataResourceLoader.get().addListenerOrdering(ResourceReloaderKeys.Server.FUNCTIONS, id("skills"));
// client assets: ResourceLoader.get(PackType.CLIENT_RESOURCES)
ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(id("tints"),
        (ResourceManagerReloadListener) manager -> reloadTints(manager));
ResourceLoader.get(PackType.CLIENT_RESOURCES).addListenerOrdering(ResourceReloaderKeys.Client.MODELS, id("tints"));
```
- `ResourceLoader.get(PackType)`: `registerReloadListener(Identifier, PreparableReloadListener)`, `addListenerOrdering(first, second)`. Keys: `ResourceReloaderKeys.BEFORE_VANILLA`, `AFTER_VANILLA`, client keys (`MODELS, TEXTURES, SOUNDS, LANGUAGES, FONTS, SHADERS, ATLAS, PARTICLES, ...`), server `ResourceReloaderKeys.Server.FUNCTIONS` (recipes/advancements/loot are no longer separate reload listeners in 26.3: they load as reloadable registries).
- 26.x `PreparableReloadListener.reload(SharedState state, Executor prepare, PreparationBarrier barrier, Executor apply)`; shared state values via `state.get(key)`: `ResourceLoader.REGISTRY_LOOKUP_KEY` (HolderLookup.Provider), `ResourceLoader.FEATURE_FLAG_SET_KEY`, `DataResourceLoader.DATA_RESOURCE_STORE_KEY`. (src: common net/minecraft/server/packs/resources/PreparableReloadListener.java)
- Fabric `SimpleReloadListener<T>` (was SimpleResourceReloader): override `T prepare(SharedState)` (any thread, no game state) and `void apply(T, SharedState)` (game thread).
- Vanilla JSON loader: `extends SimpleJsonResourceReloadListener<T>` with ctor `(Codec<T>, FileToIdConverter.json("mymod/skills"))`, override `apply(Map<Identifier, T>, ResourceManager, ProfilerFiller)` (abstract in `SimplePreparableReloadListener`). Files at `data/<ns>/mymod/skills/*.json`; parse errors are logged, not thrown. Fabric resource conditions apply automatically to it.

```java
public class SkillLoader extends SimpleJsonResourceReloadListener<Skill> {
    public static volatile Map<Identifier, Skill> SKILLS = Map.of();
    public SkillLoader() { super(Skill.CODEC, FileToIdConverter.json("mymod/skills")); }
    @Override protected void apply(Map<Identifier, Skill> data, ResourceManager rm, ProfilerFiller profiler) { SKILLS = Map.copyOf(data); }
}
```
- Data resource store (attach reload results to the server instead of statics):
```java
static final DataResourceStore.Key<Map<Identifier, Skill>> SKILLS_KEY = new DataResourceStore.Key<>();
// inside a reload listener's apply step:
state.get(DataResourceLoader.DATA_RESOURCE_STORE_KEY).put(SKILLS_KEY, map);
// later:
Map<Identifier, Skill> skills = server.getOrThrow(SKILLS_KEY);   // MinecraftServer implements DataResourceStore (interface injection)
```
  (src: testmod ResourceReloaderTestMod.java)
- Clients do not get server data: sync what clients need with a packet in `ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS (player, joined)`, or use a synced dynamic registry.

## 2. Data-driven custom registries (DynamicRegistries, fabric-registry-sync-v0)

`net.fabricmc.fabric.api.event.registry.DynamicRegistries` (src: fabric-api fabric-registry-sync-v0 api/event/registry/DynamicRegistries.java):
```java
public static final ResourceKey<Registry<Skill>> SKILLS = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("mymod", "skills"));
// onInitialize:
DynamicRegistries.register(SKILLS, Skill.CODEC);                                   // world registry, server only
DynamicRegistries.registerSynced(SKILLS, Skill.CODEC);                             // also sent to clients (needs the mod on client)
DynamicRegistries.registerSynced(SKILLS, Skill.CODEC, Skill.CLIENT_CODEC);         // lighter network codec (must read server-encoded data)
DynamicRegistries.registerSynced(SKILLS, Skill.CODEC, DynamicRegistries.SyncOption.SKIP_WHEN_EMPTY); // vanilla-client friendly when empty
DynamicRegistries.registerReloadable(SKILL_TUNING, Tuning.CODEC);                  // NEW 26.3: reloads on /reload, NOT synced
```
- File path: `data/<entry namespace>/<registry namespace>/<registry path>/<entry path>.json`, e.g. `data/mymod/mymod/skills/fireball.json` (vanilla registries omit the `minecraft` segment). Tags: `data/<ns>/tags/<registry namespace>/<registry path>/<tag>.json` (`Registries.tagsDirPath` = `"tags/" + registryDirPath`, and `registryDirPath` drops only the `minecraft` namespace).
- Access world/synced registries: `server.registryAccess().lookupOrThrow(SKILLS).getOrThrow(key)`; `level.registryAccess()` on either side; `registryAccess.get(ResourceKey<T>)` -> `Optional<Holder.Reference<T>>`; tags `holder.is(TagKey)`.
- Access reloadable registries: `server.reloadableRegistries().lookup().lookupOrThrow(SKILL_TUNING)` (vanilla `ReloadableServerRegistries.Holder`; also `getLootTable(key)`). Vanilla reloadable set in 26.3: `loot_table, predicate, context_float_provider, context_int_provider, item_modifier, slot_source, advancement, recipe`. (src: common net/minecraft/resources/RegistryDataLoader.java:154)
- Introspection: `DynamicRegistries.getAllDynamicRegistries()`, `getWorldRegistries()`, `getReloadableRegistries()`; `DynamicRegistrySetupCallback.EVENT.register(view -> view.registerEntryAdded(key, (rawId, id, obj) -> ...))`.
- Datagen: `FabricDynamicRegistryProvider` + `buildRegistry(RegistrySetBuilder)` in your `DataGeneratorEntrypoint`.
- Static (code-registered) custom registries: `FabricRegistryBuilder` (see registries doc); attributes `RegistryAttribute.SYNCED` / `OPTIONAL` control sync.

## 3. Resource conditions (fabric-resource-conditions-api-v1)

JSON key `"fabric:load_conditions"` at the root of a data file; array (AND) or single object; each object has `"condition"`. Applies to `SimpleJsonResourceReloadListener` data and to registry-loaded data (recipes, advancements, loot tables, predicates, item modifiers, and other dynamic registry entries incl. Fabric biome modifiers). Unknown condition ids are skipped and count as success. Pack-level: `"fabric:overlays"` in `pack.mcmeta` (`FabricOverlayMetadataSection`). (src: fabric-api fabric-resource-conditions-api-v1 api/resource/conditions/v1/*)

Built-in conditions:
```json
{"condition": "fabric:true"} {"condition": "fabric:false"}
{"condition": "fabric:not", "value": {...}}
{"condition": "fabric:and", "values": [{...}, {...}]}   {"condition": "fabric:or", "values": [...]}
{"condition": "fabric:all_mods_loaded", "values": ["modid", "othermod"]}
{"condition": "fabric:any_mods_loaded", "values": ["modid"]}
{"condition": "fabric:tags_populated", "registry": "minecraft:item", "values": ["c:ingots/tin"]}
{"condition": "fabric:features_enabled", "features": ["minecraft:minecart_improvements"]}
{"condition": "fabric:registry_contains", "registry": "minecraft:block", "values": ["othermod:fancy_block"]}
```
(`registry` defaults to `minecraft:item` for `tags_populated`; src: impl TagsPopulatedResourceCondition.java:40.) Java: `ResourceConditions.alwaysTrue/alwaysFalse/not/and/or/allModsLoaded/anyModsLoaded/tagsPopulated/featuresEnabled/registryContains` -> `ResourceCondition`, usable in datagen (`withConditions(...)` / `entries.add(key, value, condition)`).

Custom condition:
```java
public record DateMatches(int month, int day) implements ResourceCondition {
    public static final MapCodec<DateMatches> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("month").forGetter(DateMatches::month),
            ExtraCodecs.POSITIVE_INT.fieldOf("day").forGetter(DateMatches::day)).apply(i, DateMatches::new));
    public static final ResourceConditionType<DateMatches> TYPE = ResourceConditionType.create(id("date_matches"), CODEC);
    @Override public ResourceConditionType<?> getType() { return TYPE; }
    @Override public boolean test(RegistryOps.@Nullable RegistryInfoLookup registryInfo) { return SpecialDates.dayNow().equals(MonthDay.of(month, day)); }
}
ResourceConditions.register(DateMatches.TYPE);   // onInitialize
```
(src: fabric-docs reference conditions/*.java, verified against ResourceCondition/ResourceConditionType 26.3)

## 4. Config (no vanilla/Fabric config API)

Fabric API has no config API. Libraries (Cloth Config, YACL, MidnightLib, owo-lib, ...) are covered in libraries.md; note several show only NeoForge builds tagged 26.3 on Modrinth at time of writing, so check Fabric availability first. Minimal dependency-free approach with a Codec + Gson (bundled with MC):
```java
public record ModConfig(boolean enableFeature, int maxHomes, List<String> bannedItems) {
    public static final ModConfig DEFAULT = new ModConfig(true, 3, List.of());
    public static final Codec<ModConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("enable_feature", DEFAULT.enableFeature).forGetter(ModConfig::enableFeature),
            Codec.intRange(0, 100).optionalFieldOf("max_homes", DEFAULT.maxHomes).forGetter(ModConfig::maxHomes),
            Codec.STRING.listOf().optionalFieldOf("banned_items", DEFAULT.bannedItems).forGetter(ModConfig::bannedItems)
    ).apply(i, ModConfig::new));

    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("mymod.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static volatile ModConfig INSTANCE = DEFAULT;

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                try (Reader r = Files.newBufferedReader(PATH)) {
                    INSTANCE = CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(r))
                            .resultOrPartial(err -> LOGGER.warn("mymod.json: {}", err)).orElse(DEFAULT);
                }
            }
            save(INSTANCE);   // writes back defaults for missing keys
        } catch (IOException | JsonParseException e) { LOGGER.error("Failed to load config", e); }
    }
    public static void save(ModConfig cfg) throws IOException {
        Files.createDirectories(PATH.getParent());
        Files.writeString(PATH, GSON.toJson(CODEC.encodeStart(JsonOps.INSTANCE, cfg).getOrThrow()));
    }
}
```
- Load in `onInitialize` (or `SERVER_STARTING` for per-server reload); offer a `/mymod reload` command that calls `load()`.
- `optionalFieldOf(name, default)` makes old config files forward-compatible; with partial results, a bad value falls back to default for that field only when combined with `lenientOptionalFieldOf` (use it for user-edited files).
- Server-authoritative settings that clients need must be synced (configuration-phase payload), never read from the client's own file.

## 5. Server-side-only mods (vanilla clients can join)

What breaks vanilla clients:
1. Any new entry in a SYNCED registry: blocks, items, entity types, block entity types (raw ids in packets), menu types, particle types, sound events, mob effects, data component types that reach the client, command argument types (`BuiltInRegistries.COMMAND_ARGUMENT_TYPE` is marked `RegistryAttribute.SYNCED`), etc. Fabric registry sync then disconnects the vanilla client in configuration with "This server requires Fabric Loader and Fabric API installed on your client!", listing your namespace, unless every affected registry is `RegistryAttribute.OPTIONAL`. (src: fabric-api fabric-registry-sync-v0 impl/registry/sync/RegistrySyncManager.java:80-130, FabricRegistryInit.java)
2. Synced dynamic registries with entries (`DynamicRegistries.registerSynced` without `SKIP_WHEN_EMPTY`), and new entries in vanilla synced data registries the client cannot decode (custom codec types); vanilla data-only entries (e.g. a new `minecraft:wolf_variant` JSON, a new `enchantment` JSON, a `dialog`) are fine because the vanilla client knows the codec.
3. Custom payloads the client must answer (configuration tasks you wait on): check `canSend` and skip.
4. Custom `ArgumentType`s in commands; custom `MenuType`s; custom particles/sounds without a resource pack.

What is safe server-side:
- Commands built from vanilla argument types (incl. server-side `.suggests`), events, mixins into server logic, SavedData, data attachments (non-synced, or synced: vanilla clients just do not receive them), datapack content using vanilla types (recipes, loot tables, advancements, tags, worldgen JSON, structures, dimensions, dialogs, enchantments, trade sets, villager trades), vanilla menus (`MenuType.GENERIC_9x*` + `ChestMenu`), text with `Component.literal` / `translatableWithFallback`, boss bars, scoreboards, display entities, `ClickEvent.ShowDialog` / `ClickEvent.Custom` (handle `MinecraftServer#handleCustomClickAction` via mixin).
- Server resource pack for custom textures/models/sounds: vanilla `server.properties` `resource-pack` or send `ClientboundResourcePackPushPacket` (`net.minecraft.network.protocol.common`); item looks via `DataComponents.ITEM_MODEL` / `CUSTOM_MODEL_DATA` on vanilla items.
- `fabric.mod.json` `"environment": "*"` is fine; use `"server"` only if the jar must never load on clients.

Polymer (eu.pb4, maven `https://maven.nucleoid.xyz`, `eu.pb4:polymer-core:0.18.7+26.3` latest at 2026-11-03): lets you register real custom blocks/items/entities server-side and sends vanilla look-alikes (`PolymerItem`, `PolymerBlock`, `PolymerEntity`, resource pack generation via polymer-resource-pack / autohost). Companion libs: `eu.pb4:sgui:2.2.1+26.3` (server GUIs on vanilla menus), placeholder-api `3.2.0+26.3`. Polymer module list and API: see libraries.md (Polymer section).

Testing: run a dedicated server and join with a vanilla 26.3 client; a log line from Fabric registry sync listing your namespaces means you leaked a synced registry entry.

## Where to look
- fabric-api `fabric-resource-loader-v1/src/testmod/.../ResourceReloaderTestMod.java`, `BuiltinPackTestMod.java`; `fabric-registry-sync-v0/src/testmod/.../CustomDynamicRegistryTest.java`; `fabric-resource-conditions-api-v1/src/testmod`.
- fabric-docs `develop/resource-conditions.md`, `develop/registries/` (dynamic registries page).
