# World systems on 26.3: dimensions, teleport, biomes, worldgen, Level operations

Scope: dimensions (fabric-dimensions-v1), teleporting, biome modification (fabric-biome-api-v1, code and data-driven), 26.3 worldgen features/structures from code + datagen, chunk generators, common `Level`/`ServerLevel` operations, raycasts, entity queries, scheduling, for MC 26.3 / Fabric API 0.166.2+26.3. Verified 2026-11-14 against decompiled 26.3, vanilla data in the 26.3 jar, fabric-api 26.3 testmods, Fabric 26.1/26.3 blog posts.

## 1. Dimensions

There is no code API to create a dimension: ship JSON in your mod's data pack (`src/main/resources/data/<ns>/...`) and the world loads it.
- `data/<ns>/dimension/<name>.json` (LevelStem: `{"type": "<ns>:<dimension_type>", "generator": {...}}`), level key = `ResourceKey.create(Registries.DIMENSION, id)`, stem key `Registries.LEVEL_STEM`.
- `data/<ns>/dimension_type/<name>.json`. 26.3 format (vanilla `the_end.json`): `ambient_light, attributes{"minecraft:visual/sky_color": "#000000", "minecraft:gameplay/bed_rule": {...}, "minecraft:audio/background_music": {...}, ...}, coordinate_scale, default_clock, has_ceiling, has_ender_dragon_fight, has_fixed_time, has_skylight, height, infiniburn, logical_height, min_y, monster_spawn_block_light_limit, monster_spawn_light_level, skybox ("overworld"|"end"|...), cardinal_light, timelines`. Old fields `ultrawarm`, `natural`, `piglin_safe`, `bed_works`, `respawn_anchor_works`, `has_raids`, `effects`, `fixed_time` are gone (moved to environment attributes / timelines). Required: `has_skylight, has_ceiling, has_ender_dragon_fight, coordinate_scale, min_y, height, logical_height, infiniburn, ambient_light, monster_spawn_light_level, monster_spawn_block_light_limit`. (src: common net/minecraft/world/level/dimension/DimensionType.java:85-133; vanilla-jar data/minecraft/dimension_type/*.json). The fabric-dimensions testmod `void_type.json` still uses the old field set; unknown fields are ignored by the codec, do not copy it.
- Datagen: `registryBuilder.add(Registries.LEVEL_STEM, ctx -> ctx.register(KEY, new LevelStem(dimensionTypes.getOrThrow(BuiltinDimensionTypes.OVERWORLD), new FlatLevelSource(FlatLevelGeneratorSettings.getDefault(biomes, structureSets, placedFeatures)))))` (src: fabric-api biome testmod DataGeneratorEntrypoint.java).
- Get the level: `ServerLevel lvl = server.getLevel(ResourceKey.create(Registries.DIMENSION, id))` (nullable), `server.overworld()`, `server.getAllLevels()`, `Level.OVERWORLD / NETHER / END`.
- Custom chunk generator: `Registry.register(BuiltInRegistries.CHUNK_GENERATOR, id, MyGen.CODEC)` where `CODEC` is a `MapCodec<MyGen>`; override `codec()`, `buildTerrain(...)` (26.x: abstract `CompletableFuture<ChunkAccess> buildTerrain(ChunkAccess, Blender, RandomState, StructureManager, BiomeManager, @Nullable WorldGenRegion, Set<Holder<Biome>>)`, was `fillFromNoise`; src ChunkGenerator.java:648), `spawnOriginalMobs`, `getGenDepth`, `getSeaLevel`, `getMinY`, `getBaseHeight`, `getBaseColumn`, `addDebugScreenInfo`. Biome source via `RegistryOps.retrieveGetter(Registries.BIOME)` in the codec. (src: fabric-api fabric-dimensions-v1 testmod VoidChunkGenerator.java). Biome sources: `BuiltInRegistries.BIOME_SOURCE`.
- `DimensionEvents.MODIFY_ATTRIBUTES.register((Holder<DimensionType> dim, EnvironmentAttributeMap.Builder attrs, HolderLookup.Provider regs) -> { if (dim.is(BuiltinDimensionTypes.OVERWORLD)) attrs.set(EnvironmentAttributes.CLOUD_COLOR, ...); })` (new 26.1; whole fabric-dimensions-v1 API is just this event now). Dimension-level attributes rank below biome-specific ones.
- Old `FabricDimensions.teleport(...)` is gone; vanilla `TeleportTransition` covers it.

## 2. Teleporting

(src: common net/minecraft/world/level/portal/TeleportTransition.java, Entity.java:3142/3428, ServerPlayer.java:1124)
```java
ServerLevel dest = server.getLevel(MY_DIM);
player.teleport(new TeleportTransition(dest, new Vec3(x, y, z), Vec3.ZERO, player.getYRot(), player.getXRot(),
        TeleportTransition.DO_NOTHING));                         // also PLAY_PORTAL_SOUND, PLACE_PORTAL_TICKET, .then(...)
// with relative flags
entity.teleportTo(dest, x, y, z, Set.of(), yRot, xRot, /*resetCamera*/ true);   // returns boolean
entity.teleportTo(x, y, z);                                                      // same level
```
- `TeleportTransition(ServerLevel newLevel, Vec3 position, Vec3 deltaMovement, float yRot, float xRot, boolean missingRespawnBlock, boolean asPassenger, Set<Relative> relatives, PostTeleportTransition post)` + shorter ctors.
- `Entity#teleport` returns the (possibly new) entity: non-player entities are re-created in the new level; use the return value.
- Pre-load chunks for far teleports if needed: `level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, new ChunkPos(pos), 3)` or `level.setChunkForced(cx, cz, true)` (persistent force-load).

## 3. Biome modification (fabric-biome-api-v1)

Code API (runs when biomes load from data packs, i.e. at world load):
```java
BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES,
        ResourceKey.create(Registries.PLACED_FEATURE, id("ruby_ore")));
BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_FOREST), MobCategory.CREATURE, ModEntities.DEER, 8, 2, 4);
BiomeModifications.addCarver(BiomeSelectors.foundInTheNether(), ResourceKey.create(Registries.CARVER, id("rift")));
BiomeModifications.create(id("tweaks"))
    .add(ModificationPhase.REMOVALS, BiomeSelectors.includeByKey(Biomes.PLAINS),
         ctx -> ctx.getMobSpawnSettings().removeSpawnsOfEntityType(EntityTypes.ZOMBIE))
    .add(ModificationPhase.ADDITIONS, BiomeSelectors.foundInOverworld(),
         ctx -> ctx.getAttributes().set(EnvironmentAttributes.SKY_COLOR, ARGB.vector3fFromRGB24(0x770000)));
NetherBiomes.addNetherBiome(MY_BIOME, Climate.parameters(t, h, c, e, d, w, offset));
TheEndBiomes.addHighlandsBiome(MY_BIOME, 10.0);   // also addMainIslandBiome, addSmallIslandsBiome, addMidlandsBiome, addBarrensBiome
```
(src: fabric-api fabric-biome-api-v1 api/biome/v1/*, testmod FabricBiomeTest.java)
- Selectors: `BiomeSelectors.all(), vanilla(), foundInOverworld(), foundInTheNether(), foundInTheEnd(), tag(TagKey), includeByKey(...), excludeByKey(...), spawnsOneOf(EntityType...)`; `BiomeSelectionContext`: `getBiomeKey, getBiome, getBiomeHolder, getRegistryAccess, hasTag, canGenerateIn(ResourceKey<LevelStem>), validForStructure, getFeatureKey, getPlacedFeatureKey, getStructureKey`.
- Phases: `ADDITIONS`, `REMOVALS`, `REPLACEMENTS`, `POST_PROCESSING`.
- `BiomeModificationContext`: `getWeather()` (precipitation/temperature/downfall), `getAttributes()` (26.x environment attributes: `set`, `setModifier`, `addAll`, `get`), `getEffects()` (fog/water/sky/grass/foliage colors, music volume), `getGenerationSettings()` (`addFeature/removeFeature/addCarver/removeCarver/hasFeature`), `getMobSpawnSettings()` (`addSpawn(category, new MobSpawnSettings.SpawnerData(type, IntProvider count), weight)`, `removeSpawns(BiPredicate<MobCategory, SpawnerData>)`, `setCreatureGenerationProbability`, `addMobCharge`). `SpawnSettingsContext` was renamed `MobSpawnSettingsContext`.
- `addSpawn` rejects `MobCategory.MISC` and unregistered entity types.
- Biome colours/sky/fog are environment attributes in 26.x (`EnvironmentAttributes.SKY_COLOR`, `CLOUD_COLOR`, `FOG_COLOR`...), not only `BiomeSpecialEffects`.

### Data-driven Fabric biome modifiers (new)
Files at `data/<ns>/fabric/biome_modifier/<name>.json`, registry key `BiomeModifiers.REGISTRY_KEY` (`fabric:biome_modifier`). Applied at world load (no hot reload). Support `fabric:load_conditions`. (src: api/biome/v1/data/BiomeModifiers.java, testmod generated/data/.../fabric/biome_modifier/*.json)
```json
{ "type": "fabric:add_features", "biomes": "#minecraft:is_overworld", "features": "mymod:ruby_ore", "step": "underground_ores" }
{ "type": "fabric:remove_features", "biomes": "minecraft:plains", "features": "minecraft:ore_iron_upper", "steps": ["underground_ores"] }
{ "type": "fabric:add_spawns", "biomes": "mymod:glade", "spawners": [ { "type": "minecraft:cow", "count": 2, "weight": 10 } ] }
{ "type": "fabric:remove_spawns", "biomes": "#minecraft:is_forest", "entity_types": "minecraft:zombie" }
{ "type": "fabric:add_carvers", "biomes": "...", "carvers": "minecraft:cave" }
{ "type": "fabric:remove_carvers", "biomes": "...", "carvers": "minecraft:canyon" }
{ "type": "fabric:set_attributes", "biomes": "#mymod:targets", "attributes": { "minecraft:visual/sky_color": "#123456" } }
{ "type": "fabric:none" }
```
Datagen: in a `FabricDynamicRegistryProvider.configure`, `entries.add(ResourceKey.create(BiomeModifiers.REGISTRY_KEY, id), BiomeModifiers.addFeatures(biomeHolderSet, featureHolderSet, step))`; custom types `BiomeModifiers.registerType(Identifier, MapCodec)` implementing `BiomeModifier` (`codec()`, `getPhase()`, `matches(ctx)`, `modify(selection, ctx)`).

## 4. Worldgen features and structures (26.3 refactor)

26.3 merged `ConfiguredFeature` + `Feature<FC>` + `FeatureConfiguration` into one `Feature` interface (src: common net/minecraft/world/level/levelgen/feature/Feature.java):
```java
public interface Feature {
    Codec<Feature> DIRECT_CODEC = BuiltInRegistries.FEATURE_TYPE.byNameCodec().dispatch(Feature::codec, t -> t);
    Codec<Holder<Feature>> CODEC = RegistryCodecs.holder(Registries.FEATURE, DIRECT_CODEC);
    MapCodec<? extends Feature> codec();
    boolean place(WorldGenLevel level, ChunkGenerator gen, RandomSource random, BlockPos origin);
    // helpers: setBlock, safeSetBlock, markAboveForPostProcessing, getSubFeatures
}
```
- Registries: `Registries.FEATURE` = `worldgen/feature` (data `data/<ns>/worldgen/feature/*.json`, was `worldgen/configured_feature`), `Registries.PLACED_FEATURE` = `worldgen/placed_feature`, `BuiltInRegistries.FEATURE_TYPE` (= `worldgen/feature_type`) holds the `MapCodec`s of feature implementations. JSON has no `"config"` wrapper: `{"type": "minecraft:ore", "size": 4, "discard_chance_on_air_exposure": 0.5, "targets": [...]}`.
- Custom feature type:
```java
public record PillarFeature(Holder<BlockStateProvider> block, int height) implements Feature {
    public static final MapCodec<PillarFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockStateProvider.CODEC.fieldOf("block").forGetter(PillarFeature::block),   // 26.3: CODEC is Codec<Holder<BlockStateProvider>> (id ref or inline); DIRECT_CODEC = inline only, accepts a bare BlockState
            Codec.intRange(1, 32).fieldOf("height").forGetter(PillarFeature::height)
    ).apply(i, PillarFeature::new));
    @Override public MapCodec<PillarFeature> codec() { return CODEC; }
    @Override public boolean place(WorldGenLevel level, ChunkGenerator gen, RandomSource rnd, BlockPos origin) {
        for (int y = 0; y < height; y++) level.setBlock(origin.above(y), block.value().getState(level, rnd, origin.above(y)), Block.UPDATE_CLIENTS); // getState(LevelAccessor, RandomSource, BlockPos)
        return true;
    }
}
Registry.register(BuiltInRegistries.FEATURE_TYPE, id("pillar"), PillarFeature.CODEC);
```
  Vanilla example to copy: `BlueIceFeature` (record + `MapCodec.unit`), `OreFeature(RuleTest, BlockState, int size)`.
- Datagen (src: fabric-api biome testmod DataGeneratorEntrypoint.java, WorldgenProvider.java):
```java
@Override public void buildRegistry(RegistrySetBuilder b) {
    b.add(Registries.FEATURE, ctx -> ctx.register(RUBY_ORE,
            new OreFeature(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), ModBlocks.RUBY_ORE.defaultBlockState(), 5)));
    b.add(Registries.PLACED_FEATURE, ctx -> PlacementUtils.register(ctx, RUBY_ORE_PLACED,
            ctx.lookup(Registries.FEATURE).getOrThrow(RUBY_ORE),
            CountPlacement.of(8), InSquarePlacement.spread(),
            HeightRangePlacement.uniform(VerticalAnchor.absolute(-32), VerticalAnchor.absolute(32)), BiomeFilter.biome()));
}
// provider: entries.addAll(registries.lookupOrThrow(Registries.FEATURE)); entries.addAll(registries.lookupOrThrow(Registries.PLACED_FEATURE));
```
  `PlacementUtils` is in `net.minecraft.data.worldgen.placement`. Keys: `ResourceKey<Feature>` (not `ResourceKey<ConfiguredFeature<?,?>>`).
- 26.3 placement modifier type ids: `block_predicate_filter, rarity_filter, random_chance, surface_relative_threshold_filter, surface_water_depth_filter, biome, count, noise_based_count, noise_threshold_count, count_on_every_layer, cuboid, environment_scan, heightmap, height_range, in_square, offset, randomly_selected, fixed_placement` (src: PlacementModifierTypes.java). Feature type ids include `ore, scattered_ore, tree, simple_block, random_selector, weighted_random_selector, block_column, disk, lake, geode, vegetation_patch, template, sequence, no_op, ...` (src: FeatureTypes.java).
- Other newly registrable worldgen pieces in 26.3: `worldgen/block_state_provider` (`BuiltInRegistries.BLOCK_STATE_PROVIDER_TYPE` for types), `worldgen/material_rule` and `worldgen/material_condition` (surface rules renamed "material rules"; types in `MATERIAL_RULE_TYPE` / `MATERIAL_CONDITION_TYPE`), `context_int_provider` / `context_float_provider` number providers.
- fabric-docs `reference/latest` worldgen classes (`ExampleModWorldConfiguredFeatures`, `Registries.CONFIGURED_FEATURE`) are 26.2 and do not compile on 26.3.
- Structures: implement `Structure` with a `MapCodec`, register a `StructureType<S>` (functional interface `MapCodec<S> codec()`): `Registry.register(BuiltInRegistries.STRUCTURE_TYPE, id, () -> MyStructure.CODEC)`; pieces via `BuiltInRegistries.STRUCTURE_PIECE` (`StructurePieceType`). Most mods use the vanilla `minecraft:jigsaw` type with JSON `worldgen/structure`, `worldgen/structure_set`, `worldgen/template_pool`, `worldgen/processor_list` + NBT templates in `data/<ns>/structure/*.nbt`. (src: common levelgen/structure/StructureType.java)

## 5. Chunk hooks
- `ServerChunkEvents.CHUNK_LOAD (level, chunk, boolean generated)` to post-process freshly generated chunks (runs on server thread after the chunk is in the level; heavy work here stalls the server).
- Prefer worldgen (features/structures) for anything that should be deterministic and threaded.
- Chunk data: `ChunkAccess` is an `AttachmentTarget` (persistent attachments per chunk).

## 6. Level / ServerLevel common operations

(src: common net/minecraft/world/level/Level.java, LevelWriter.java, block/Block.java, server/level/ServerLevel.java)
- Read: `level.getBlockState(pos)`, `getFluidState(pos)`, `getBlockEntity(pos)` (also typed `getBlockEntity(pos, BlockEntityType<T>)` -> `Optional<T>`), `isLoaded(pos)`, `getBiome(pos)` (Holder<Biome>), `getHeight(Heightmap.Types, x, z)`, `isClientSide()` (method, not field), `getGameTime()`, `getRandom()`, `registryAccess()`, `dimension()` (ResourceKey<Level>).
- Write: `setBlock(pos, state, flags)`, `setBlockAndUpdate(pos, state)` (= `Block.UPDATE_ALL`), `removeBlock(pos, movedByPiston)`, `destroyBlock(pos, dropResources[, breaker, updateLimit])`, `sendBlockUpdated(pos, old, new, flags)`, `updateNeighborsAt(pos, block, @Nullable Orientation)`, `blockEntityChanged(pos)`.
- Flags (`Block.`): `UPDATE_NEIGHBORS=1, UPDATE_CLIENTS=2, UPDATE_INVISIBLE=4, UPDATE_IMMEDIATE=8, UPDATE_KNOWN_SHAPE=16, UPDATE_SUPPRESS_DROPS=32, UPDATE_MOVE_BY_PISTON=64, UPDATE_SKIP_SHAPE_UPDATE_ON_WIRE=128, UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS=256, UPDATE_SKIP_ON_PLACE=512`; combos `UPDATE_ALL=3`, `UPDATE_ALL_IMMEDIATE=11`, `UPDATE_NONE=260`, `UPDATE_SKIP_ALL_SIDEEFFECTS=816`; `UPDATE_LIMIT=512`.
- Sound (server-side, plays for everyone near except `except`): `level.playSound(@Nullable Entity except, BlockPos pos, SoundEvent, SoundSource, float volume, float pitch)`; also `(except, x, y, z, SoundEvent|Holder<SoundEvent>, source, vol, pitch)`, `(except, Entity source, ...)`; pass `null` as `except` from server code so the triggering player hears it too (passing the player means "client already played it").
- Particles (server -> clients): `serverLevel.sendParticles(ParticleOptions, x, y, z, int count, double dx, double dy, double dz, double speed)` returns count sent; overloads with `boolean overrideLimiter, boolean alwaysShow` and `ClientboundLevelParticlesPacket.RandomizationType`. `level.addParticle(...)` on the server does nothing visible.
- Explosions: `level.explode(@Nullable Entity source, x, y, z, float radius, Level.ExplosionInteraction)` and overloads with `boolean fire`, `DamageSource`, `ExplosionDamageCalculator`; `ExplosionInteraction`: `NONE, BLOCK, MOB, TNT, TRIGGER`.
- Entities: `serverLevel.addFreshEntity(entity)`; `EntityTypes.ZOMBIE.spawn(serverLevel, pos, EntitySpawnReason.COMMAND)` (nullable); `EntityType#create(level, EntitySpawnReason)`; `serverLevel.getEntity(int id)` / `getEntity(UUID)`; `level.getEntitiesOfClass(LivingEntity.class, aabb, e -> ...)`, `getEntities(@Nullable Entity except, AABB, Predicate)`, `getEntities(EntityTypeTest, AABB, Predicate)`, `level.getNearestPlayer(entity, maxDist)` / `getNearestPlayer(x, y, z, range, Predicate<Entity>)`; `server.getPlayerList().getPlayer(UUID)`, `getPlayerByName(String)`, `getPlayers()`. Entity type constants are in `EntityTypes` (26.x), ids in `EntityTypeIds`.
- Game events / level events: `level.gameEvent(...)`, `level.levelEvent(@Nullable Player, int type, BlockPos, int data)` (vanilla effect ids, e.g. 2001 block break particles).
- Chunk forcing: `serverLevel.setChunkForced(chunkX, chunkZ, true)`; tickets via `serverLevel.getChunkSource().addTicketWithRadius(TicketType.X, chunkPos, radius)` (`TicketType.FORCED, PORTAL, PLAYER_SPAWN, ...`).

### Raycasting
```java
HitResult hit = player.pick(5.0, 1.0f, /*fluids*/ false);                  // block (or MISS) along view
BlockHitResult bhr = level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity));
EntityHitResult ehr = ProjectileUtil.getEntityHitResult(player, from, to, aabb, e -> !e.isSpectator() && e.isPickable(), maxDistSqr); // (Entity except, Vec3 from, Vec3 to, AABB, Predicate, double); other overloads at ProjectileUtil.java:147-157
HitResult any = ProjectileUtil.getHitResultOnViewVector(player, e -> true, 20.0);
```
Check `hit.getType()` (`HitResult.Type.MISS/BLOCK/ENTITY`) before casting.

### Scheduling / threading
- Scheduled block ticks: `level.scheduleTick(pos, block, delayTicks[, TickPriority])`, fluid overload; handle in `Block#tick(BlockState, ServerLevel, BlockPos, RandomSource)`.
- Run on server thread: `server.execute(runnable)` (queued when called off-thread or during task processing; may run inline when already on the thread and idle), `server.submit(supplier)` -> CompletableFuture.
- Delays ("run in N ticks"): there is no vanilla/Fabric delay scheduler; keep a queue and drain it in `ServerTickEvents.END_SERVER_TICK` against `server.getTickCount()`.
- Never touch the level from netty threads (configuration/login handlers) or async futures without hopping to the server thread.

## Where to look
- Vanilla features: `net/minecraft/world/level/levelgen/feature/*Feature.java`; vanilla worldgen bootstrap: `net/minecraft/data/worldgen/**`.
- Vanilla worldgen JSON: `vanilla-jar/data/minecraft/worldgen/{feature,placed_feature,structure,structure_set,template_pool,biome,noise_settings,material_rule,block_state_provider}`.
- fabric-api `fabric-biome-api-v1/src/testmod` (code + datagen + generated JSON), `fabric-dimensions-v1/src/testmod`.
