# Undead mod: technical feasibility study (MC 26.3, Fabric API 0.166.2+26.3)

Written 2026-11-14 from the decompiled 26.3 source (`common`/`client`/`vanilla` = `build/mcsrc/26.3/<x>/`), Fabric API 26.3 (`fapi:<module>`) and `docs/knowledge/`. Claims carry `(src: ...)` pointers. **UNVERIFIED** marks things read in source but not run in game. Nothing here was compiled or run.

Owner decisions folded in, received during the study:
- **Dormant (static) undead are real blocks** with a block entity. They are not entities or display-entity pseudo-blocks. Reasons: they must be Silk-Touch-mineable with their gear, a chunk full of them must not lag, and they must not count against mob caps. This is a deliberate B15 exception. The reason is recorded in section 2.0.
- **Third-party resource-pack compatibility (Faithful) is not a criterion.** The repo draws its own 16x and 32x textures for everything it adds, sleepers included (B16). Rendering options are judged on cost, quality and effort only.

---

## 1. Reducing natural spawns while keeping the light rules

### 1.1 How 26.3 spawns (verified)
- Per tick: `ServerChunkCache#tickChunks` builds `NaturalSpawner.createState(...)` (counts every non-persistent, non-MISC mob per category), filters the categories with `getFilteredSpawningCategories` (global cap, `spawnEnemies`, persistent categories only every 400 ticks), then runs `NaturalSpawner.spawnForChunk` per spawning chunk. After that come `level.tickCustomSpawners` (phantoms, patrols, cats, sieges, wandering traders) (src: common `server/level/ServerChunkCache.java:364-399`).
- **Global cap**: `SpawnState#canSpawnForCategoryGlobal`: `count < MobCategory.getMaxInstancesPerChunk() * spawnableChunks / 289`. **Local (per-player) cap**: `LocalMobCapCalculator.MobCounts#canSpawn`: `count < category.getMaxInstancesPerChunk()` (src: common `world/level/NaturalSpawner.java:518-525`, `LocalMobCapCalculator.java:51-52`). Caps are hard-coded in the `MobCategory` enum (MONSTER 70, CREATURE 10, AMBIENT 15, AXOLOTLS 5, UWC 5, WATER_CREATURE 5, WATER_AMBIENT 20). Data cannot change them.
- **Persistent mobs are not counted** (`createState` skips `isPersistenceRequired() || requiresCustomPersistence()`), and they add no spawn-potential charge (src: `NaturalSpawner.java:76`).
- **Which mob spawns where**: `ChunkGenerator#getMobsAt` first returns a structure's `spawn_overrides` (witch hut, monument, outpost, fortress through `NaturalSpawner.mobsAt`). Otherwise it reads the environment attribute **`minecraft:gameplay/natural_mob_spawns`** (`EnvironmentAttributes.NATURAL_MOB_SPAWNS`, type `MobSpawnSettings`) at the position (src: common `world/level/chunk/ChunkGenerator.java:456-481`, `world/attribute/EnvironmentAttributes.java:168`). So in 26.3 biome spawns really live in that attribute: vanilla biome JSON has `attributes."minecraft:gameplay/natural_mob_spawns" = {modifier:"overlay", argument:{spawns_by_category, spawn_costs}}`.
- Attribute layers: dimension type (constant) is applied first, then the biome (positional), then timelines (src: `world/attribute/EnvironmentAttributeSystem.java:61-84`). The only modifier is `overlay` (`MobSpawnSettingsModifier.Overlay`): **a category the higher layer defines replaces the lower one's list for that category**, and `spawn_costs` maps are merged (src: `world/attribute/modifier/MobSpawnSettingsModifier.java`).
- **Light rules** live in `SpawnPlacements.checkSpawnRules` → `Monster::checkMonsterSpawnRules` (`isDarkEnoughToSpawn`, block light per dimension `monster_spawn_block_light_limit`). They run after a spawn entry is picked. Every option below keeps them because none touches the placement predicates.
- **Spawners, trial spawners, raids and structure-gen mobs bypass all of this.** `BaseSpawner` checks only peaceful; `TrialSpawner` checks `spawner_blocks_work`, peaceful and `spawn_mobs`, never the caps (src: `world/level/BaseSpawner.java:127`, `block/entity/trialspawner/TrialSpawner.java:152-157`).
- Game rules: `spawn_mobs`, `spawn_monsters` (feeds `spawnEnemies` via `ServerLevel#isSpawningMonsters`), `spawn_patrols`, `spawn_phantoms`, `spawn_wardens`, `spawner_blocks_work`. These are on/off only, no scaling (src: `world/level/gamerules/GameRules.java:76-82`).

### 1.2 Options
| Option | How | Scales | Keeps structure overrides? | Invasiveness / compat |
|---|---|---|---|---|
| A. **Cap scaling mixin** (recommended) | `@ModifyExpressionValue` on `MobCategory.getMaxInstancesPerChunk()` inside `NaturalSpawner$SpawnState#canSpawnForCategoryGlobal` **and** `LocalMobCapCalculator$MobCounts#canSpawn`; multiply by a per-category factor | per category, density | caps also limit structure-override spawns proportionally | 2 tiny mixins on private methods, MixinExtras composes with other mods. Mods that rewrite the spawner wholesale (rare) clash |
| B. Biome-only rate gate | `@WrapOperation` around `.getMobsToSpawn(category)` on the last line of `ChunkGenerator#getMobsAt` (the biome fallback, reached only when no structure override matched); return `EMPTY_MOB_LIST` with probability 1-f | per category, rate | **yes, exactly** | 1 mixin. `getMobsAt` is called twice per attempt (pick + `canSpawnMobAt` validate), so the effective rate is about f². Account for that or memoise per (tick, pos) |
| C. Per-type density limit, no mixin | `BiomeModifications.create(id).add(ModificationPhase.POST_PROCESSING, BiomeSelectors.foundInOverworld(), ctx -> ctx.getMobSpawnSettings().addMobCharge(EntityTypes.ZOMBIE, charge, budget))` (spawn costs, as soul sand valley does) | per **entity type**, local density | applies inside structures too (it's the `canSpawn` extra test) | Fabric API, data-level. Only limits clustering, so it's a weak tool for a "drastic" cut (src: fapi `BiomeModificationContext.java:520-560`, `NaturalSpawner.java:476-489`) |
| D. Reweight / remove entries | `ctx.getMobSpawnSettings().removeSpawnsOfEntityType(...)` + `addSpawn` with a lower weight, or datapack `fabric:biome_modifier` `remove_spawns`/`add_spawns` | changes the **mix**, not the amount: the cap refills with creepers/spiders | yes | no mixin; alone it doesn't reduce spawns |
| E. Overwrite biome or dimension JSON | datapack replaces `natural_mob_spawns` | anything | yes | **worst compat**: replaces vanilla files and other mods' biome additions; must track every vanilla biome each release (B10) |
| F. Game rule off | `spawn_monsters false` | all-or-nothing | | no |

**Recommendation**: **A + B**, both driven by mod game rules (fabric-game-rule-api-v1, `GameRuleBuilder.forDouble(0.15).range(0.0, 1.0).buildAndRegister(id)`, src: fapi `GameRuleBuilder.java:54-157`), for example `undead:monster_cap_factor` (default 0.15) and `undead:creature_cap_factor` (1.0). Use A for density (the drastic cut). Use B only if the owner wants structure spawns (witch huts, fortresses, monuments) kept at full strength. Then B runs at factor f for biome entries and A stays at 1.0 for those categories. Optionally add **D** to shift the monster mix away from zombies and skeletons, since the world's undead should come mainly from sleepers. Spawners, trial spawners and dungeons are untouched by A, B, C and D.

```java
@Mixin(targets = "net.minecraft.world.level.NaturalSpawner$SpawnState")
abstract class SpawnStateMixin {
    @ModifyExpressionValue(method = "canSpawnForCategoryGlobal",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/MobCategory;getMaxInstancesPerChunk()I"))
    private int undead$scaleCap(int max, MobCategory cat) { return UndeadSpawnRules.scaledCap(cat, max); } // reads game rule cached per tick
}
// same on LocalMobCapCalculator$MobCounts#canSpawn
```
Cost: none measurable (a multiply per check). Risk: low. The private-method targets can move between releases (B10 port check). Game-rule lookups must be cached once per tick, because `canSpawn` runs often.

---

## 2. Dormant undead as blocks (owner decision)

### 2.0 Why blocks (B15 justification, for the record)
- **Entity option, measured from source, rejected.** A `NoAI` mob skips only `serverAiStep` (goals, sensing, navigation) and `travel`. It still runs `baseTick` (fire, fluids, effects), `LivingEntity#tick` (`detectEquipmentUpdates`, combat tracker, head turn), `pushEntities` (an entity box query **every tick**), freezing, `Mob#aiStep` → `burnUndead` (**dormant skeletons in sunlight would burn**) and loot pickup scans, plus `checkDespawn` and membership in `ServerLevel.navigatingMobs`, which is iterated on every collision-changing block update (src: common `LivingEntity.java:2732-2830, 3009-3130, 3196`; `Mob.java:342-480, 669-745`; `ServerLevel.java:1144-1165, 2040`). There is also network tracking per player. Persistent mobs don't count toward caps, but each one is still a full ticking entity. With hundreds per dungeon this is real lag, which matches the owner's reasoning.
- **Display pseudo-mob**: dropped by the owner.
- **Block + block entity without a ticker**: zero tick cost. It's stored in the chunk, Silk-Touch-able through loot tables, never counted by `NaturalSpawner`, and placed by worldgen like any block. Vanilla's own precedent is **`CopperGolemStatueBlock`**: a block with a block entity, `FACING` + `POSE` blockstate, rendered by a `BlockEntityRenderer` with an entity model, and converting into a living mob (`CopperGolemStatueBlockEntity#removeStatue` creates and positions a `CopperGolem`) (src: common `world/level/block/CopperGolemStatueBlock.java`, `block/entity/CopperGolemStatueBlockEntity.java`; client `renderer/blockentity/CopperGolemStatueBlockRenderer.java`).
- New registrations: 2 blocks (`dormant_skeleton`, `dormant_zombie`; husk/stray/drowned variants either as extra blocks or as a `variant` property), 1 block entity type, 2 block items. Each must be ported every release (B10), and removing the mod leaves unknown blocks in worlds.

### 2.1 Block design
```java
public class DormantUndeadBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<SleeperPose> POSE = EnumProperty.create("pose", SleeperPose.class); // SITTING, SLUMPED, LYING, LYING_BURIED, KNEELING ...
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private final EntityType<? extends Mob> wakesAs;                 // EntityTypes.SKELETON / ZOMBIE / HUSK ...
    // NO getTicker(): never ticks. Wake uses a scheduled tick (level.scheduleTick(pos, this, delay)) -> tick(...)
    @Override protected void tick(BlockState s, ServerLevel level, BlockPos pos, RandomSource r) { Wake.wake(level, pos, s, this); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; } // MODEL if body is baked (2.3 hybrid); INVISIBLE if BER-only
}
```
- Shapes: sitting is about `Block.box(2,0,2,14,10,14)` and lying is slab-high. Use `noOcclusion()` and `isSuffocating`/`isViewBlocking` false. Model on the copper statue: `Block.column(10.0, 0.0, 14.0)`, waterlogged, `FACING` from placement.
- **Lying is 2 blocks long** visually (a humanoid is 32 px tall). Choose (owner decision D3): (a) a single block whose model overhangs into the next space (the element `from`/`to` range is −16..32, so it fits; collision only in one space), or (b) a two-part block like `BedBlock` (`PART=HEAD/FOOT`, more code, correct collision, both halves break together).
- Hardness about bone block (2.0, pickaxe), sound `BONE_BLOCK`/`MUD`, `PushReaction.POPPED` (26.3's name for the old DESTROY; or wake on piston: owner).
- Hooks: `wasExploded`/`onExplosionHit` → wake (src: `Block.java:445`, `BlockBehaviour.java:167`). `AttackBlockCallback` or `playerWillDestroy` (start of mining) → wake chance (D4).

### 2.2 Block entity: gear and state
```java
public class DormantUndeadBlockEntity extends BlockEntity implements GameEventListener.Provider<SleeperListener> {
    private NonNullList<ItemStack> gear = NonNullList.withSize(6, ItemStack.EMPTY); // fixed slot order: MAINHAND, OFFHAND, FEET, LEGS, CHEST, HEAD
    private @Nullable ResourceKey<LootTable> gearTable; private long gearSeed;     // lazy gear for structure/feature placement
    private long cooldownUntil;                                                     // ignore vibrations until gameTime
    private final SleeperListener listener = new SleeperListener(this);
    @Override public SleeperListener getListener() { return listener; }
    @Override protected void saveAdditional(ValueOutput out) { ContainerHelper.saveAllItems(out, gear); ... }
    @Override protected void loadAdditional(ValueInput in) { ... }
    // Silk Touch round-trip: gear <-> minecraft:container (same as DecoratedPotBlockEntity)
    @Override protected void collectImplicitComponents(DataComponentMap.Builder c) { super.collectImplicitComponents(c); c.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(gear)); }
    @Override protected void applyImplicitComponents(DataComponentGetter c) { super.applyImplicitComponents(c); c.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(gear); }
    @Override public void removeComponentsFromTag(ValueOutput out) { out.discard("Items"); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveCustomOnly(r); } // client needs gear for rendering
}
```
(src for the component round-trip pattern: common `block/entity/DecoratedPotBlockEntity.java:112-130`, `BlockEntity.java:275-308`.) Reusing `minecraft:container` gives the item a vanilla contents tooltip for free. A custom data component `undead:sleeper` (pose, variant, wake count) is the alternative if more state is needed.
- After changing gear on the server: `setChanged()` + `level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS)`.
- Lazy gear: structure `.nbt` files and simple features can store `gearTable` instead of fixed items. Resolve it on the server in `ServerBlockEntityEvents.BLOCK_ENTITY_LOAD` (fapi lifecycle), the way chests resolve `LootTable` lazily, then sync. UNVERIFIED whether the update reaches the client before the first frame; a one-frame "naked" render is harmless.

**Silk Touch loot table** (`data/undead/loot_table/blocks/dormant_skeleton.json`, shape from vanilla `glass.json` and `copper_golem_statue.json`):
```json
{ "type": "minecraft:block", "pools": [
  { "rolls": 1, "entries": [ { "type": "minecraft:alternatives", "children": [
    { "type": "minecraft:item", "name": "undead:dormant_skeleton", "condition": "minecraft:tool/can_silk_touch",
      "modifier": [ { "type": "minecraft:copy_components", "source": "block_entity", "include": ["minecraft:container", "minecraft:custom_name"] },
                    { "type": "minecraft:copy_state", "block": "undead:dormant_skeleton", "properties": ["pose"] } ] },
    { "type": "minecraft:loot_table", "value": "undead:blocks/dormant_skeleton_broken" } ] } ] } ] }
```
26.3 loot JSON spelling, verified in vanilla files: functions are `"modifier": [{ "type": ... }]` (`copper_golem_statue.json`), and a condition is a single `"condition"`, either a predicate reference like `"minecraft:tool/can_silk_touch"` (`glass.json`) or an object `{ "type": ... }`. Whether `condition` is accepted on an `alternatives` child entry (as opposed to the pool) is UNVERIFIED; check with `tools/packload.py`. Without Silk Touch, the owner decides (D4): bones/rotten flesh plus the gear (`ContainerHelper`-style drop in `affectNeighborsAfterRemoval`/`preRemoveSideEffects`), or wake instead of breaking. Placing a silk-touched sleeper uses vanilla `BlockItem` behaviour: `applyImplicitComponents` restores the gear (`BlockItem#updateCustomBlockEntityTag` path; src: common `world/item/BlockItem.java:55-160`).

### 2.3 Rendering (client; judged on cost, quality and effort)
Facts:
- A BER is extracted and submitted **every frame** for each block entity in a visible render section (`LevelExtractor`, loop over `section.getSectionMesh().getRenderableBlockEntities()`). The dispatcher calls `renderer.createRenderState()` **per BE per frame** and culls by `shouldRender` = within `getViewDistance()` (default 64) (src: client `renderer/extract/LevelExtractor.java:306-327`, `renderer/blockentity/BlockEntityRenderDispatcher.java:69-90`, `BlockEntityRenderer.java:23-32`). Culling is per section, not per block.
- The BER context has `entityModelSet()` (bake `ModelLayers.SKELETON`, `ModelLayers.ZOMBIE`, `SKELETON_ARMOR`, `ZOMBIE_ARMOR`), `itemModelResolver()`, `entityRenderer()` (src: client `BlockEntityRendererProvider.java`, `model/geom/ModelLayers.java:219-292`).
- **Baked block models can do arbitrary poses.** In 26.3 an element rotation is `CuboidRotation.EulerXYZRotation(x,y,z)` or a single axis with any angle (src: client `resources/model/cuboid/CuboidRotation.java:44-65`), so each humanoid part can be one element at any angle. Blockstate `y` rotation gives the 4 facings.

| Option | Body | Gear | Cost | Quality | Effort |
|---|---|---|---|---|---|
| R1. BER only, own model instance | `new SkeletonModel<>(ctx.bakeLayer(ModelLayers.SKELETON))` (or our own `LayerDefinition`), set `ModelPart` rotations per `POSE` (like `CopperGolemStatueBlockRenderer`, which keeps one model per pose), `submitNodeCollector.submitModel(model, state, poseStack, OUR_TEXTURE, light, NO_OVERLAY, 0)` | same BER: armor through `EquipmentLayerRenderer` + baked armor `ArmorModelSet`; held items through `ItemModelResolver` + `ItemStackRenderState.submit` (as `ShelfRenderer` does) | every sleeper costs per-frame CPU (render state alloc + 6 to 12 cubes + armor) | full entity look, crumbling overlay supported | medium |
| R2. BER with a client-only dummy vanilla mob (like `SpawnerRenderer`) | keep a client `Skeleton` per BE, set pose and equipment, `entityRenderer.submit(renderState, ...)` (src: client `renderer/blockentity/SpawnerRenderer.java`) | free (vanilla layers) | **highest** (full entity extract per frame) | vanilla mob, but **vanilla texture** (swapping it for our sleeper texture needs a renderer mixin) and entity-only poses | low |
| R3. **Hybrid (recommended)** | **baked block model** per `POSE` (chunk mesh, our texture added to the block atlas or simply under `textures/block/`) | BER draws **only** armor and held items; `shouldRender` returns false when the gear is empty; `getViewDistance()` about 32 | body costs nothing per frame; per-frame cost only for armed sleepers within 32 blocks | statue-like (no animation needed: they're asleep); armor must line up with the baked body | medium-high: the poses exist twice (JSON + Java). **Generate both from one Java pose table with datagen** (`FabricModelProvider`) so they can't drift |

Hybrid details:
- Body texture: our own sleeper textures (B16, 16x + 32x) in `assets/undead/textures/block/` are in the block atlas automatically. For an entity-layout texture outside `block/`, add an atlas source the way vanilla puts `entity/bell/bell_body` into `atlases/blocks.json`; atlas source lists stack across packs (`SpriteSourceList` uses `getResourceStack`) (src: vanilla `assets/minecraft/atlases/blocks.json`, client `renderer/texture/atlas/SpriteSourceList.java:69`). Fabric also has `AtlasRegistry`/`SpriteSourceRegistry` (fapi rendering).
- Armor in the BER: `EquipmentLayerRenderer` needs the `EquipmentAssetManager`. 26.3 has no public getter for it (it's a private field in `EntityRenderDispatcher`; `Minecraft#getPalettedTextureManager()` is public), so you need **one accessor mixin** (src: client `renderer/entity/EntityRendererProvider.java:61`, `Minecraft.java:607-633, 2875`). Then use `equipmentRenderer.renderLayers(LayerType.HUMANOID, assetKey, armorModel, state, stack, poseStack, collector, light, ...)` as `HumanoidArmorLayer` does. Fabric's `ArmorRenderer` covers custom armor items only.
- Item form (inventory icon): an ordinary item model if the body is baked (R3). For R1 use a special item model: register a `SpecialModelRenderer` with `SpecialModelRenderers.ID_MAPPER.put(id, Unbaked.MAP_CODEC)` (fapi rendering testmod `SpecialBlockRendererTest.java`; vanilla precedent `CopperGolemStatueSpecialRenderer`).
- Cost rule of thumb (UNVERIFIED, measure with the F3 profiler/spark): vanilla chests were BERs for years and hundreds in view were fine, thousands hurt. Under R3 a dungeon of 50 sleepers is about 50 chunk-mesh models (free) plus a BER for the armed ones only.

### 2.4 Hearing: vibration listener on the block entity, no ticker
- Block entities that implement `GameEventListener.Provider<T>` are registered automatically. `EntityBlock#getListener` returns `provider.getListener()`, and `LevelChunk#addAndRegisterBlockEntity`/`addGameEventListener` registers it in the section's `EuclideanGameEventListenerRegistry`, unregistering on removal (src: common `world/level/block/EntityBlock.java:20-22`, `chunk/LevelChunk.java:392-398, 485-495, 687-697`). **No ticker needed for registration.**
- The sculk sensor *does* tick, but only to run `VibrationSystem.Ticker` (selection plus travel time) (src: `SculkSensorBlock.java:150-157`). We avoid that: implement `GameEventListener` directly and turn the delay into a **scheduled block tick**.

```java
final class SleeperListener implements GameEventListener {
    private final DormantUndeadBlockEntity be; private final PositionSource src;
    public PositionSource getListenerSource() { return src; }                 // new BlockPositionSource(be.getBlockPos())
    public int getListenerRadius() { return 12; }                              // <= GameEvent.notificationRadius (16 default)
    public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> ev, GameEvent.Context ctx, Vec3 from) {
        if (!ev.is(UndeadTags.SLEEPERS_HEAR)) return false;                    // our game_event tag (start from #minecraft:vibrations, minus noise we don't want)
        if (level.getGameTime() < be.cooldownUntil()) return false;
        Entity e = ctx.sourceEntity();
        if (e != null && (e.isSpectator() || e.dampensVibrations() || e.is(UndeadTags.DOES_NOT_WAKE))) return false;
        if (e != null && e.isSteppingCarefully() && ev.is(GameEventTags.IGNORE_VIBRATIONS_SNEAKING)) return false; // sneaking = silent steps
        if (ctx.affectedState() != null && ctx.affectedState().is(BlockTags.DAMPENS_VIBRATIONS)) return false;     // walking on wool/carpet
        double d = from.distanceTo(Vec3.atCenterOf(be.getBlockPos()));
        if (level.random.nextFloat() > Loudness.wakeChance(ev, d)) { be.setCooldown(level, 20); return false; } // cheap roll BEFORE raycast
        if (Occlusion.isOccluded(level, from, Vec3.atCenterOf(be.getBlockPos()))) return false; // copy of VibrationSystem.Listener#isOccluded (private static): wool blocks
        level.sendParticles(new VibrationParticleOption(src, (int) d), from.x, from.y, from.z, 1, 0, 0, 0, 0);   // sculk-style "it heard you" cue
        level.scheduleTick(be.getBlockPos(), be.getBlockState().getBlock(), Math.max(1, (int) d));             // travel time, then Block#tick wakes it
        be.setWaker(e); be.setCooldown(level, 40);
        return true;
    }
}
```
- Mirrors vanilla rules exactly (src: `VibrationSystem.java:210-270, 400-425`): spectators ignored; `isSteppingCarefully()` (sneak) suppresses `#ignore_vibrations_sneaking` = `step, swim, hit_ground, projectile_shoot, item_interact_start/finish`; `#dampens_vibrations` (wool, carpet) on the affected block; `#occludes_vibration_signals` (wool) blocks the line (6 nudged raycasts).
- What players emit (vanilla `tags/game_event/vibrations.json`): `step` (from `Entity#vibrationAndSoundEffectsFromBlock`, about once per block walked; **every walking entity emits it, mobs and animals too**, so filter the source: D6), `hit_ground`, `swim`, `splash`, `block_place/destroy/open/close/change/activate`, `container_open/close` (**robbing a chest wakes nearby sleepers; sneaking does not hide it**), `eat`, `drink`, `item_interact_finish`, `equip`/`unequip`, `entity_damage`, `entity_die`, `projectile_shoot/land`, `explode`, `prime_fuse`, `instrument_play`, `note_block_play`, `teleport`, `elytra_glide`, `shear`, `fluid_pickup/place`, `entity_interact`, `entity_mount/dismount`, `lightning_strike`. `item_interact_start` is not in `vibrations` (wardens only). A per-event **loudness table** (an explosion wakes at 16, a step at 4 to 6) goes in `Loudness`, or data-drive it with a JSON reload listener.
- Robbing the sleeper itself: no game event is needed. The interaction rolls its own wake chance (2.6).
- **Cost**: `GameEventDispatcher#post` visits up to 3x3x3 sections around each event and iterates each section's listeners with a distance check; vanilla already does this for every sculk sensor (src: `GameEventDispatcher.java`, `EuclideanGameEventListenerRegistry.java:77-125`). 100 sleepers near a walking player cost about 100 distance checks per step event. Keep the order **tag → cooldown → source filter → random roll → raycast** so the 6 raycasts run rarely. Listeners exist only while the chunk is loaded at FULL status and never tick. A scheduled tick in a non-ticking chunk waits until the chunk ticks, which is fine because players are near when events happen.
- Gotcha: `getListenerRadius` is capped in practice by the event's `notificationRadius` (16 for most, `shriek` 32, `jukebox_play` 10); sections farther out are never visited.

### 2.5 Waking: block → real mob, chain to neighbours
```java
static void wake(ServerLevel level, BlockPos pos, BlockState state, DormantUndeadBlock block) {
    if (!(level.getBlockEntity(pos) instanceof DormantUndeadBlockEntity be)) return;
    Mob mob = block.wakesAs().create(level, EntitySpawnReason.TRIGGERED);    // like CopperGolemStatueBlockEntity#removeStatue
    if (mob == null) return;
    mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, state.getValue(FACING).toYRot(), 0);
    mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.TRIGGERED, null); // optional: it rolls vanilla gear,
    for (EquipmentSlot s : SLOTS) { mob.setItemSlot(s, be.gear(s)); mob.setDropChance(s, 2.0F); } // ...so overwrite after it; 2.0 = always drops (picked-up rule)
    mob.setPersistenceRequired();                    // D7: persistent = no despawn AND not counted in caps (NaturalSpawner.java:76)
    level.setBlock(pos, state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    level.addFreshEntity(mob);
    if (be.waker() instanceof LivingEntity t && mob.canAttack(t)) mob.setTarget(t);
    level.gameEvent(UndeadGameEvents.AWAKEN, mob.position(), GameEvent.Context.of(mob)); // chain
    level.playSound(null, pos, SoundEvents.SKELETON_AMBIENT, SoundSource.HOSTILE, 1, 0.6F);
}
```
- **Chain waking**: register a custom game event with `Registry.registerForHolder(BuiltInRegistries.GAME_EVENT, id("awaken"), new GameEvent(10))` and put it in `#undead:sleepers_hear` only. Neighbouring sleepers then hear it through the same listener path: wool occlusion, chance roll and travel delay included, so a dungeon wakes in a ripple. Vanilla sculk sensors ignore it unless it's added to `#minecraft:vibrations` (owner option: it is thematic). This beats a `BlockPos.betweenClosed` cube search (13³ = 2197 lookups per wake at r=6) and needs no index.
- Waking mob AI: vanilla goals take over at once. The waker is passed as target; zombies' `HurtByTargetGoal`/`NearestAttackableTargetGoal` keep it.

### 2.6 Robbing a sleeper
- Handle it in the block (`useWithoutItem`), or in `UseBlockCallback` if it must pre-empt other mods. **Empty hand required**: `ServerPlayerGameMode#useItemOn` skips block use only when sneaking **and** holding something (`suppressUsingBlock = isSecondaryUseActive() && haveSomethingInOurHands`, src: common `server/level/ServerPlayerGameMode.java:405`). So with empty hands, sneak-use reaches the block.
- Suggested rule (D5): plain use takes the weapon (mainhand/offhand), wake chance p₁. Sneak-use takes one armor piece (top-down: head, chest, legs, feet), wake chance p₂ > p₁, scaled down by Swift Sneak/leggings or similar. Give the item with `player.getInventory().placeItemBackInInventory(stack)`, `be.setGear(slot, EMPTY)`, sync, sound `ARMOR_EQUIP_GENERIC`. A failed roll calls `level.scheduleTick(pos, block, 2)` (wakes angry at the thief).
- Return `InteractionResult.SUCCESS` on the client only when it can predict (it knows the gear through the update tag), else PASS, and SUCCESS_SERVER on the server (pattern in `no-new-blocks.md` 4.2).
- Nearby chest: no code. Opening it emits `container_open` (2.4).

### 2.7 Cost summary per dormant sleeper
| | Server tick | Memory/save | Client per frame |
|---|---|---|---|
| Block + BE (R3, no gear) | 0 | 1 block state + small BE NBT + 1 listener object | 0 (chunk mesh) |
| Block + BE (R3, armed, < 32 blocks) | 0 | + item stacks | 1 BER: render state + armor/item submits |
| (rejected) NoAI mob | `baseTick` + `LivingEntity#tick` + push query + despawn check + tracking | full entity NBT | full entity render |

---

## 3. Vibration/noise wakeups (summary; details in 2.4)
- Classes: `net.minecraft.world.level.gameevent.{GameEvent, GameEventListener (+ .Provider, .DeliveryMode), GameEventDispatcher, EuclideanGameEventListenerRegistry, DynamicGameEventListener, BlockPositionSource, EntityPositionSource}`, `...gameevent.vibrations.{VibrationSystem (.Listener/.User/.Data/.Ticker), VibrationSelector, VibrationInfo}`; tags `GameEventTags.VIBRATIONS`, `IGNORE_VIBRATIONS_SNEAKING`, `WARDEN_CAN_LISTEN`, `ALLAY_CAN_LISTEN`; block tags `OCCLUDES_VIBRATION_SIGNALS`, `DAMPENS_VIBRATIONS`.
- Entities (Warden, Allay) use `DynamicGameEventListener`, re-registered when the entity changes section through `Entity#updateDynamicGameEventListener` (called from `ServerLevel.EntityCallbacks.onTrackingStart/End/onSectionChange`), and tick `VibrationSystem.Ticker.tick` in `tick()` (src: `Warden.java:116-125, 245-249, 381-385`; `ServerLevel.java:2050-2083`). Woken mobs need no hearing, since they are vanilla mobs once awake.
- Hearing rules, cost and chain: see 2.4 and 2.5. Wool is the player's counter-play (walls of wool occlude; carpet/wool floors dampen steps); sneaking silences steps but not chests, eating or block breaking.

---

## 4. Worldgen placement (now plain block placement)
- **Features (graves, loose sleepers)**: a Java `Feature<Config>` (`Registry.register(BuiltInRegistries.FEATURE, id, new SleeperGraveFeature(Codec))`) + `configured_feature` and `placed_feature` JSON (counts, `rarity_filter`, `heightmap`, `biome`) + `BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.TOP_LAYER_MODIFICATION or LOCAL_MODIFICATIONS, placedKey)`. In `place(ctx)`: dig the pit (`level.setBlock(pos, AIR, 2)`), set `dormant_zombie[pose=lying_buried]`, then `if (level.getBlockEntity(p) instanceof DormantUndeadBlockEntity be) be.setGearTable(table, random.nextLong())`. This is exactly how `MonsterRoomFeature` configures its spawner and chest (`level.getBlockEntity(origin) instanceof SpawnerBlockEntity spawner`, `RandomizableContainer.setBlockEntityLootTable`) (src: common `levelgen/feature/MonsterRoomFeature.java:107-117`). Graves: coarse dirt/podzol mound, a gravestone (cobblestone wall / stone brick slab), sometimes a chest. A purely data-driven grave is possible with vanilla `simple_block` + `block_column`, but it can't set BE data; that's fine if gear comes from the lazy `gearTable` default on the BE.
- **Dungeon rooms / crypts**: a data-driven **jigsaw structure** (`worldgen/structure/*.json` type `minecraft:jigsaw`, `template_pool`, `structure_set`, `processor_list`) with `.nbt` templates. **Block entities are saved in templates**: our sleeper with its gear (or `gearTable`) is just a block + BE NBT in the `.nbt`. Processors (`minecraft:rule` with `block_entity_modifier` `append_loot`/`passthrough`) can randomise further. Spawners, chests and trial-spawner-style rooms keep their vanilla behaviour.
- Authoring `.nbt`: build in-game, save with a structure block (`/give @s structure_block`, SAVE mode, it writes `generated/<ns>/structure/*.nbt` in the world folder), copy into `src/main/resources/data/undead/structure/`. Or generate by code: `StructureTemplate#fillFromWorld` in a game test or a datagen step, which makes them reproducible and diffable (owner decision D9). Vanilla references: `data/minecraft/structure/{ancient_city,trial_chambers,...}`.
- Sitting setups in templates: a sleeper `pose=sitting` against a wall, `facing` set; the template rotation rotates `FACING` through `rotate()` (implement `rotate/mirror` like the copper statue).

---

## 5. Undead tuning
- **Attributes**: `FabricDefaultAttributeRegistry.MODIFY.register(ctx -> ctx.modify(EntityTypes.ZOMBIE, b -> b.add(Attributes.MAX_HEALTH, 60).add(Attributes.MOVEMENT_SPEED, 0.17).add(Attributes.ATTACK_DAMAGE, 1.5)))`, also for SKELETON, HUSK, STRAY, DROWNED, ZOMBIE_VILLAGER, BOGGED, PARCHED as wanted (src: fapi `fabric-object-builder-api-v1/.../FabricDefaultAttributeRegistry.java:62`). Vanilla bases: zombie speed 0.23, attack 3, armor 2, follow 35; skeleton speed 0.25 (src: common `monster/zombie/Zombie.java:143-150`, `monster/skeleton/AbstractSkeleton.java:89-91`). This affects every instance (spawners and structures too) and keeps vanilla clients happy (attributes sync). Datapack-only alternative: none for defaults; per-spawn modifiers from `ServerEntityEvents.ENTITY_LOAD` add NBT noise and double-apply risk.
- **Weak fists, high armor wear**: `ServerLivingEntityEvents.AFTER_DAMAGE` (fapi entity-events, `(entity, source, baseDamageTaken, damageTaken, blocked)`). If `source.getEntity()` is tagged `#undead:tuned`, call `stack.hurtAndBreak(extra, victim, slot)` for each worn armor slot. Vanilla already does `max(1, damage/4)` per piece in `LivingEntity#doHurtEquipment` (src: common `LivingEntity.java:1876-1906`). No mixin is needed. A mixin alternative is `@ModifyExpressionValue` on the `Math.max` in `doHurtEquipment`.
- **Smite insta-kill, fully data-driven** (enchantment effect components, src: common `world/item/enchantment/EnchantmentEffectComponents.java:45`, `effects/EnchantmentEntityEffect.java:19-33`, `effects/DamageEntity.java`): add a `minecraft:post_attack` entry to Smite:
```json
"minecraft:post_attack": [ { "enchanted": "attacker", "affected": "victim",
  "requirements": { "type": "minecraft:all_of", "terms": [
     { "type": "minecraft:entity_properties", "entity": "this", "predicate": { "minecraft:entity_type": "#minecraft:sensitive_to_smite" } },
     { "type": "minecraft:random_chance", "chance": { "type": "minecraft:enchantment_level", "amount": { "type": "minecraft:linear", "base": 0.04, "per_level_above_first": 0.03 } } } ] },
  "effect": { "type": "minecraft:all_of", "effects": [
     { "type": "minecraft:spawn_particles", "particle": { "type": "minecraft:soul_fire_flame" }, "horizontal_position": { "type": "in_bounding_box" }, "vertical_position": { "type": "in_bounding_box" }, "horizontal_velocity": {}, "vertical_velocity": { "movement_scale": 0.1 } },
     { "type": "minecraft:play_sound", "sound": "minecraft:entity.zombie_villager.cure", "volume": 1, "pitch": 1.4 },
     { "type": "minecraft:damage_entity", "damage_type": "undead:purged", "min_damage": 10000, "max_damage": 10000 } ] } } ]
```
  - `undead:purged` damage type in `#bypasses_armor`, `#bypasses_cooldown` (the hit just set invulnerability frames), `#bypasses_enchantments`, `#bypasses_resistance`, `#no_knockback`. `DamageEntity` uses `new DamageSource(type, item.owner())`, so player-kill loot and XP still count. The `requirements` shape follows vanilla 26.3 `smite.json` (`type` + `minecraft:entity_type` predicate key, verified). The `all_of` `terms` key, `random_chance` with an `enchantment_level` number provider, and the `spawn_particles` fields are UNVERIFIED: check against `SpawnParticlesEffect`/`LootItemRandomChanceCondition` codecs and with `tools/packload.py`.
  - Apply it without overwriting `smite.json` with **Fabric `EnchantmentEvents.MODIFY_WITH_LOOKUP`** (fapi `fabric-item-api-v1/.../EnchantmentEvents.java`), which appends the effect in Java and composes with other packs. Or ship an override of `data/minecraft/enchantment/smite.json` in the mod's built-in datapack (simplest; clashes with other smite overrides).
- **Bow equivalent** (engine-grounded): arrows run `EnchantmentHelper.doPostAttackEffectsWithItemSource(serverLevel, mob, damageSource, getWeaponItem())`, so **bow enchantments' `post_attack` fires on arrow hits** (src: common `projectile/arrow/AbstractArrow.java:424-468`). Options: (a) a new data-only bow enchantment "Consecration" with the same chance-kill block; (b) add it to Power at a low chance; (c) **headshots**: in `ServerLivingEntityEvents.ALLOW_DAMAGE`, when `source.getDirectEntity() instanceof AbstractArrow a` and `a.getY() >= victim.getEyeY() - 0.3` → bonus damage or a kill chance. For an exact hit point, a small mixin on `AbstractArrow#onHitEntity` captures `EntityHitResult#getLocation`; Flame plus the undead tag can also cause extra burning. All server-side.

---

## 6. Staggering animation (client)
- 26.3 model pipeline: `EntityRenderer#extractRenderState(entity, state, partialTicks)` → `Model#setupAnim(state)` → submit. `HumanoidModel#setupAnim` computes limb swings from `state.walkAnimationPos/Speed`, `isPassenger`, `isCrouching`, arm poses. `ZombieModel#setupAttackAnimation` → `AnimationUtils.animateZombieArms`; `SkeletonModel#setupAnim` adds aggressive arms (src: client `model/HumanoidModel.java:198-260`, `model/monster/zombie/ZombieModel.java`, `model/monster/skeleton/SkeletonModel.java:91-107`). Keyframe animations (`KeyframeAnimation` + entity `AnimationState`) are used by the warden, camel and others, not by humanoids.
- **Layering a stagger onto vanilla models** (recommended): attach data with Fabric's `RenderStateDataKey` / `FabricRenderState#setData` (fapi `fabric-rendering-v1` client API), filled by a mixin at TAIL of `AbstractZombieRenderer#extractRenderState` / `AbstractSkeletonRenderer#extractRenderState` (stagger phase from `entity.getId()`, intensity from walk speed). Then add a mixin at TAIL of **`HumanoidModel#setupAnim`** that, when the key is present, adds a body/head roll (`zRot` wobble), a limp (asymmetric leg amplitude) and arm sway. Because armor models are also `HumanoidModel`s set up from the same state, **armor follows the stagger automatically**. The cost is a few sin/cos per mob per frame. Mob-agnostic and compatible with other model mixins as long as they don't overwrite the same parts afterwards.
- A server-side half (works on vanilla clients too): lower speed (section 5) plus a goal that occasionally nudges `MoveControl`/applies small lateral impulses gives a visible stagger in movement, without the body roll.
- **GeckoLib**: replaces the mob's model and renderer wholesale with a Bedrock-style geo model + animation JSON. That means a new model to author and maintain, conflicts with other mods touching vanilla zombie/skeleton rendering, and extra library weight for one wobble. Not worth it unless the owner wants fully new mob animations. (Texture-pack considerations dropped per the owner.)

---

## 7. Zombies drifting into groups
```java
final class DriftToHordeGoal extends Goal {           // added to Zombie via mixin at TAIL of Zombie#addBehaviourGoals (goalSelector is protected; no Fabric AW for it)
    DriftToHordeGoal(Zombie z) { this.z = z; setFlags(EnumSet.of(Flag.MOVE)); }
    public boolean canUse() {
        if (z.getTarget() != null || z.isPassenger() || z.getRandom().nextInt(reducedTickDelay(200)) != 0) return false; // about every 10 s per zombie
        List<Zombie> near = z.level().getEntitiesOfClass(Zombie.class, z.getBoundingBox().inflate(24, 6, 24), o -> o != z && o.isAlive());
        if (near.isEmpty() || near.size() >= 6) return false;            // already grouped
        goal = centroid(near); return z.distanceToSqr(goal) > 16;
    }
    public void start() { z.getNavigation().moveTo(goal.x, goal.y, goal.z, 0.6); }
    public boolean canContinueToUse() { return !z.getNavigation().isDone() && z.getTarget() == null; }
}
```
- Priority 7 (with `WaterAvoidingRandomStrollGoal` at 7, src: `Zombie.java:131-139`). Vanilla zombie goals: `SpearUseGoal`(2), `ZombieAttackGoal`(3), `MoveThroughVillageGoal`(6), stroll(7). The goal shares the `MOVE` flag, so attacking always wins.
- Cost: one entity-section AABB query (48x12x48 box ≈ 3x1x3 sections) per zombie per ~200 ticks, plus one path request; negligible next to vanilla stroll pathing. "Slowly": speed modifier 0.6 combined with the lowered base speed. Optional anti-clumping: stop at `near.size() >= N`. Server-side only; vanilla clients see it.

---

## 8. Server-side-only possibility
- **Blocks force a client mod.** The sleeper blocks, block entity and BER are client content: a vanilla client can't decode unknown block states, and Fabric registry sync will not admit a client missing them. The only server-side route would be Polymer (`PolymerBlock` mapped to a vanilla look plus display-entity models), which the owner has ruled out.
- Works with vanilla clients if the mod is ever split: spawn reduction (1), attribute tuning, armor wear, Smite/bow effects (5), drift goal (7), the server half of staggering (6), vibration hearing logic (2.4/3), structures and features (4, once the blocks exist).
- Needs the client mod: sleeper blocks and items and their rendering (2.3), the stagger body animation (6), the Silk Touch item icon.
- Recommendation: ship one mod with `environment: *`, client entrypoint for renderers and animation mixins. A server running it requires clients to have it, the normal Fabric situation.

---

## 9. Recommended architecture
**Java (code)**
- `block/DormantUndeadBlock` (+ variants), `block/DormantUndeadBlockEntity` (gear, lazy gear table, cooldown, waker), `block/SleeperListener` (`GameEventListener`), `wake/Wake` (conversion + chain), `registry/*` (blocks, BE type, items, `undead:awaken` game event, game rules, damage type key).
- `spawn/` mixins A (+ B): `NaturalSpawner$SpawnState`, `LocalMobCapCalculator$MobCounts`, optionally `ChunkGenerator#getMobsAt`.
- `tuning/`: `FabricDefaultAttributeRegistry.MODIFY`, `AFTER_DAMAGE` armor wear, `ALLOW_DAMAGE` headshots, `EnchantmentEvents.MODIFY_WITH_LOOKUP` for Smite.
- `ai/DriftToHordeGoal` + `ZombieMixin` (addBehaviourGoals TAIL).
- `worldgen/SleeperGraveFeature` + `BiomeModifications.addFeature`.
- Client: `DormantUndeadGearRenderer` (BER: armor + held items; accessor mixin for `EquipmentAssetManager`), stagger mixins (`Abstract{Zombie,Skeleton}Renderer#extractRenderState`, `HumanoidModel#setupAnim`), datagen generating baked pose models + blockstates from one pose table.

**Data (built-in datapack/resources in the jar)**
- `loot_table/blocks/dormant_*.json` (Silk Touch + copy_components), `loot_table/sleeper_gear/*.json` (gear tables), `damage_type/purged.json` + damage-type tags, `tags/game_event/sleepers_hear.json`, `tags/entity_type/{tuned,does_not_wake}.json`, `tags/block/mineable/pickaxe.json`.
- Worldgen: `configured_feature`/`placed_feature` for graves; `structure`, `structure_set`, `template_pool`, `processor_list` + `structure/*.nbt` for crypts and dungeon rooms.
- Bow enchantment JSON (if new) + `#minecraft:enchantable/bow` membership; optionally a Smite override if MODIFY isn't used.
- Assets: blockstates, generated pose models, our textures 16x and 32x (B16), item models, lang.

**Libraries (B9)**: none required beyond Fabric API (+ MixinExtras bundled with Loader). Polymer and GeckoLib are not recommended (sections 6, 8). spark for profiling during development.

---

## 10. Decisions for the owner
| # | Decision | Options and tradeoffs |
|---|---|---|
| D1 | How drastic, and which categories | Default proposal: monsters 15 % cap, others 100 %. Lower is emptier nights; sleepers then provide most undead |
| D2 | Keep structure-specific spawns (witch huts, fortress, monument, outposts) at full strength? | Yes = add mixin B (biome-only gate) and keep caps; No = cap scaling alone (simpler) |
| D3 | Lying sleeper footprint | single block with overhanging model (simple; collision 1 block) vs. two-part bed-style block (correct collision, more code) |
| D4 | Mining without Silk Touch | drops bones + gear / wakes the sleeper / both (chance). Also: does starting to mine (punch) roll a wake? |
| D5 | Robbing rules | which slots per action, wake chances, does sneaking lower them, do Swift Sneak / leather boots help |
| D6 | Who wakes sleepers | players only, or any entity's steps (animals wandering through graveyards will wake them; woken undead wake others = cascades) |
| D7 | Woken mobs persistent? | persistent = never despawn, don't count toward caps (can pile up) vs. normal despawn (a woken horde may vanish when you run) |
| D8 | Body rendering | R3 hybrid (fast, more work) vs. R1 BER-only (simpler, per-frame cost per sleeper) |
| D9 | Structure authoring | hand-built `.nbt` from structure blocks vs. code-generated templates in a game test/datagen (reproducible, B5-friendly) |
| D10 | Smite delivery | Fabric `EnchantmentEvents.MODIFY_WITH_LOOKUP` (composable) vs. overriding `smite.json` (simple); kill chance per level |
| D11 | Bow mechanic | new bow enchantment / Power add-on / headshots / several |
| D12 | Which undead types sleep | skeleton + zombie only, or husk/stray/drowned/bogged/parched variants (more blocks or a `variant` property; more textures under B16) |
| D13 | Sleepers placeable by players after Silk Touch? | yes (decor and traps, the item keeps gear) vs. no (drops a "remains" item instead) |

---

## 11. Performance budget
| System | Budget | Basis |
|---|---|---|
| Dormant sleeper (server) | **0 per tick**; ~1 listener object + BE NBT each | no ticker; listeners only in loaded FULL chunks (2.4) |
| Hearing | ≤ N distance checks per game event within 16 blocks; raycasts only after tag/cooldown/roll | `GameEventDispatcher` loop (2.4). Aim for < 0.05 ms/tick with 200 sleepers around a walking player; measure with spark |
| Waking | one entity spawn + one `awaken` event per wake; chains throttled by cooldown and roll | rare |
| Sleeper rendering (R3) | body: chunk mesh (0 per frame); gear BER only when armed and within 32 blocks | 2.3. Target: 100 armed sleepers in view < 1 ms/frame (UNVERIFIED, profile with F3 + spark) |
| Spawn mixins | O(1) per cap check | 1.2 |
| Drift goal | 1 section query per zombie per ~200 ticks | 7 |
| Stagger | a few trig ops per humanoid per frame | 6 |
| Armor wear / Smite / headshot | per hit only | 5 |

## 12. Blockers and risks
- **No hard blockers.** Everything above maps to 26.3 classes that exist.
- Risk: no public `EquipmentAssetManager` getter for armor in a BER → one client accessor mixin (`EntityRenderDispatcher.equipmentAssets`).
- Risk: baked-pose JSON and BER armor alignment → generate both from one source (datagen).
- Risk: mixin targets `NaturalSpawner$SpawnState#canSpawnForCategoryGlobal`, `LocalMobCapCalculator$MobCounts#canSpawn`, `HumanoidModel#setupAnim`, `Zombie#addBehaviourGoals` are private or inner and may move each release (B10).
- UNVERIFIED JSON shapes (entry-level `condition` in loot alternatives, enchantment `spawn_particles` / `random_chance` fields; 26.3 uses `modifier`/`type`/`condition`, so older-format examples are wrong): validate with `tools/packload.py` before shipping.
- Two-plus new blocks under B15: accepted by the owner; the reason is recorded in 2.0.
