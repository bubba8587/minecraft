# Entities in code (Fabric, MC 26.3)

Scope: entity types, attributes, mob classes, AI (goals and Brain), spawning (biome modifications, spawn placements), projectiles, synced data, saving, Fabric entity events, pointers to client rendering. Verified 2026-11-14 against decompiled MC 26.3 + fabric-api 26.3. Path shorthand as in `content.md`.

## 1. What changed (1.21.x -> 26.3)
- Vanilla `EntityType.ZOMBIE` etc. moved to **`EntityTypes`** (26.2); keys in `EntityTypeIds` (src: common world/entity/EntityTypes.java, EntityTypeIds.java). Use `EntityTypes.LIGHTNING_BOLT`, `EntityTypes.COW`.
- `EntityType.Builder#build(ResourceKey<EntityType<?>>)` requires the key (1.21.2+).
- Save/load uses `ValueOutput`/`ValueInput` (1.21.6+): `addAdditionalSaveData(ValueOutput)`, `readAdditionalSaveData(ValueInput)`.
- Damage is split by side: `hurtServer(ServerLevel, DamageSource, float)` (abstract on Entity, returns boolean) / `hurtClient(DamageSource)`; `hurtOrSimulate(...)` when side is unknown. `Entity#hurt(DamageSource, float)` still exists but is `@Deprecated public final void` and only forwards to `hurtServer` when the level is a `ServerLevel` (vanilla `Snowball#onHitEntity` still calls it); new code should call `hurtServer` (src: common world/entity/Entity.java:1957-1969).
- `Entity#interactAt` removed (26.1); `interact(Player, InteractionHand, Vec3 location)` now receives the hit location.
- Brain rework (26.1): `ActivityData`, `Brain.Provider` stored as a static on the entity, `makeBrain(Brain.Packed)`; `brainProvider()` removed.
- Projectile classes moved to subpackages, e.g. `net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile`, `...projectile.arrow.*`, `...projectile.hurtingprojectile.*`.
- Natural spawn settings moved from `Biome` to the environment attribute `minecraft:gameplay/natural_mob_spawns` (26.3; definable on dimension type, biome, timeline). Fabric `BiomeModifications.addSpawn` still works.
- `Level#random` is protected (26.1): use `level.getRandom()`; entities have `this.random` (protected field) / `getRandom()`.

## 2. Registering an entity type
```java
// docsref/entity/ModEntityTypes.java (valid on 26.3)
public static final ResourceKey<EntityType<?>> MINI_GOLEM_KEY = ResourceKey.create(Registries.ENTITY_TYPE, ExampleMod.id("mini_golem"));

public static final EntityType<MiniGolemEntity> MINI_GOLEM = register(MINI_GOLEM_KEY,
    EntityType.Builder.<MiniGolemEntity>of(MiniGolemEntity::new, MobCategory.CREATURE).sized(0.75f, 1.75f).eyeHeight(1.5f).clientTrackingRange(8),
    MiniGolemEntity.createAttributes());

private static <T extends Entity> EntityType<T> register(ResourceKey<EntityType<?>> key, EntityType.Builder<T> b) {
    return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, b.build(key));
}
private static <T extends LivingEntity> EntityType<T> register(ResourceKey<EntityType<?>> key, EntityType.Builder<T> b, AttributeSupplier.Builder attrs) {
    EntityType<T> type = register(key, b);
    FabricDefaultAttributeRegistry.register(type, attrs);       // REQUIRED for every LivingEntity type, else crash on spawn
    return type;
}
```
`EntityType.Builder` methods (src: common world/entity/EntityType.java:464-636): `of(factory, MobCategory)`, `createNothing(category)`, `sized(w, h)`, `spawnDimensionsScale`, `eyeHeight`, `passengerAttachments(float...|Vec3...)`, `vehicleAttachment(Vec3)`, `ridingOffset`, `nameTagOffset`, `attach(EntityAttachment, x, y, z)`, `noSummon()`, `noSave()`, `fireImmune()`, `immuneTo(TagKey<Block>)`, `canSpawnFarFromPlayer()`, `clientTrackingRange(chunks)`, `updateInterval(ticks)`, `noUpdateInterval()`, `requiredFeatures(...)`, `noLootTable()`, `notInPeaceful()`, `dontTrackDeltas()`, `build(key)`. Fabric additions (`FabricEntityType.Builder`, interface-injected): `alwaysUpdateVelocity(boolean)`, `canPotentiallyExecuteCommands(boolean)`, and static factories that also wire attributes and spawn placement:
```java
// fapi fabric-object-builder-api-v1 testmod EntityDataAccessorTest
EntityType<MyMob> T = FabricEntityType.Builder.createMob(MyMob::new, MobCategory.MONSTER, mob -> mob
        .defaultAttributes(MyMob::createAttributes)                                    // Supplier<AttributeSupplier.Builder>
        .spawnPlacement(SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules))
    .sized(0.6f, 1.95f).build(KEY);
Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY, T);
// FabricEntityType.Builder.createLiving(factory, category, living -> living.defaultAttributes(...)) for non-Mob LivingEntity
```
`MobCategory`: `MONSTER, CREATURE, AMBIENT, AXOLOTLS, UNDERGROUND_WATER_CREATURE, WATER_CREATURE, WATER_AMBIENT, MISC` (MISC cannot be used for natural spawns; Fabric's `addSpawn` throws). Lang key `entity.<ns>.<path>`; default loot table `<ns>:entities/<path>`.

## 3. Attributes
```java
// docsref/entity/MiniGolemEntity.java
public static AttributeSupplier.Builder createAttributes() {
    return PathfinderMob.createMobAttributes()           // static Mob.createMobAttributes(); Monster.createMonsterAttributes(); LivingEntity.createLivingAttributes()
        .add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.25)
        .add(Attributes.ATTACK_DAMAGE, 3).add(Attributes.FOLLOW_RANGE, 16).add(Attributes.TEMPT_RANGE, 10)
        .add(ModAttributes.AGGRO_RANGE, 8.0);             // custom attribute
}
// Custom attribute (docsref/entity/attribute/ModAttributes.java)
public static final Holder<Attribute> AGGRO_RANGE = Registry.registerForHolder(BuiltInRegistries.ATTRIBUTE, ExampleMod.id("aggro_range"),
        new RangedAttribute("attribute.name.example-mod.aggro_range", 8.0, 0.0, Double.MAX_VALUE).setSyncable(true));
// Read: mob.getAttributeValue(Attributes.MOVEMENT_SPEED); mob.getAttribute(attr).addTransientModifier(new AttributeModifier(id, amt, Operation.ADD_VALUE))
```
`Attributes.*` are `Holder<Attribute>`. Add/modify attributes of existing entity types: `FabricDefaultAttributeRegistry.MODIFY.register(ctx -> ctx.modify(EntityTypes.ZOMBIE, builder -> builder.add(ModAttributes.AGGRO_RANGE, 4.0)))` (also `modify(Collection, ...)`, `modifyAll(...)`; src: fapi fabric-object-builder-api-v1 FabricDefaultAttributeRegistry.java:62-132). Players have attributes too: modify `EntityTypes.PLAYER`.

## 4. Entity class hierarchy and required overrides
- `Entity` (abstract): must implement `defineSynchedData(SynchedEntityData.Builder)`, `readAdditionalSaveData(ValueInput)`, `addAdditionalSaveData(ValueOutput)`, `hurtServer(ServerLevel, DamageSource, float)`. Constructor `(EntityType<?>, Level)`.
- `LivingEntity` -> `Mob` (goals, navigation, `finalizeSpawn(ServerLevelAccessor, DifficultyInstance, EntitySpawnReason, @Nullable SpawnGroupData)`) -> `PathfinderMob` (walk targets) -> `AgeableMob` (`getBreedOffspring(ServerLevel, AgeableMob)`) -> `Animal` (`isFood(ItemStack)` abstract, breeding/love). Hostiles: `Monster`. Tameable: `TamableAnimal`. Water: `animal.fish.WaterAnimal` / `animal.AgeableWaterCreature`. Constructors are `protected (EntityType<? extends X>, Level)`.
- Common overrides: `tick()`, `aiStep()`, `customServerAiStep(ServerLevel)`, `getAmbientSound()`, `getHurtSound(DamageSource)`, `getDeathSound()`, `mobInteract(Player, InteractionHand)` (Mob) / `interact(Player, InteractionHand, Vec3)` (Entity), `doHurtTarget(ServerLevel, Entity)`, `die(DamageSource)`, `dropCustomDeathLoot(ServerLevel, DamageSource, boolean killedByPlayer)`, `removeWhenFarAway(double)`, `checkSpawnRules`.

```java
public class MiniGolemEntity extends PathfinderMob {
    public MiniGolemEntity(EntityType<? extends MiniGolemEntity> type, Level level) { super(type, level); }
    @Override protected void registerGoals() {          // called from Mob constructor
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TemptGoal(this, 1.0, Ingredient.of(Items.WHEAT), false)); // Predicate<ItemStack>
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
}
```
Breeding: `Animal` subclass + `BreedGoal(this, 1.0)`, `FollowParentGoal`, implement `isFood` and `getBreedOffspring` (return `ModEntityTypes.X.create(level, EntitySpawnReason.BREEDING)`).

### 4.1 Goals vs Brain
- Goals (`goalSelector`/`targetSelector`, `net.minecraft.world.entity.ai.goal`) are simplest for mods.
- Brain (villagers, piglins, axolotls, frogs, camels, armadillos, ...): 26.1 API:
```java
// 26.1 primer "Activities and Brains"; src: common world/entity/LivingEntity.java:316 makeBrain(Brain.Packed)
private static final Brain.Provider<MyMob> BRAIN_PROVIDER = Brain.provider(
    ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES),                       // sensors
    mob -> List.of(new ActivityData(Activity.CORE, ImmutableList.of(Pair.of(0, new MoveToTargetSink())),
        ImmutableSet.of(), ImmutableSet.of())));                               // (activity, behaviours, memory conditions, memories to erase)
@Override protected Brain<MyMob> makeBrain(Brain.Packed packed) { return BRAIN_PROVIDER.makeBrain(this, packed); }
@Override protected void customServerAiStep(ServerLevel level) { this.getBrain().tick(level, this); super.customServerAiStep(level); }
```
  Verified against common world/entity/animal/armadillo/Armadillo.java (`BRAIN_PROVIDER = Brain.provider(List.of(SensorType...), mob -> ArmadilloAi.getActivities())`, `protected Brain<Armadillo> makeBrain(Brain.Packed)`, `getBrain().tick(level, this)` in `customServerAiStep`); vanilla keeps activity lists in `*Ai` helper classes. Custom `MemoryModuleType`/`SensorType`/`Activity` register in `BuiltInRegistries.MEMORY_MODULE_TYPE` / `SENSOR_TYPE` / `ACTIVITY`.

## 5. Synced data (SynchedEntityData)
```java
private static final EntityDataAccessor<Boolean> DANCING = SynchedEntityData.defineId(MiniGolemEntity.class, EntityDataSerializers.BOOLEAN);
@Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(DANCING, false); }
public boolean isDancing() { return entityData.get(DANCING); }
private void setDancing(boolean v) { entityData.set(DANCING, v); }                          // server side; auto-synced to trackers
@Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) { super.onSyncedDataUpdated(key); if (key == DANCING) dancingAnimationState.animateWhen(isDancing(), tickCount); }
```
- `defineId` must use the class that declares the field (ids are per-class-hierarchy counters).
- Custom serializer: `EntityDataSerializer.forValueType(StreamCodec<? super RegistryFriendlyByteBuf, T>)`, then **register it with `FabricEntityDataRegistry.register(Identifier, serializer)`** (renamed from `FabricTrackedDataRegistry`) so IDs match between client and server regardless of registration order (src: fapi fabric-object-builder-api-v1 FabricEntityDataRegistry.java, testmod EntityDataAccessorTest).
- `AnimationState` fields + `animateWhen/start/stop` for keyframe animations on the client (docsref/entity/MiniGolemEntity.java).

## 6. Saving
```java
@Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putInt("dancing_time_left", dancingTimeLeft); }
@Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); dancingTimeLeft = in.getIntOr("dancing_time_left", 0); }
// complex values: out.store("Owner", EntityReference.codec(), ref); in.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY)
```
Same `ValueInput`/`ValueOutput` API as block entities (content-items-blocks.md section 14). Attaching data to vanilla entities without mixins: fabric-data-attachment-api-v1 (`AttachmentRegistry`, `entity.getAttachedOrCreate(type)`; docsref/attachment/), including player-copy-on-respawn and sync options.

## 7. Spawning
### 7.1 Manual
```java
MyMob mob = ModEntityTypes.MY_MOB.create(level, EntitySpawnReason.TRIGGERED);   // nullable
mob.snapTo(x, y, z, yRot, xRot);  level.addFreshEntity(mob);           // snapTo replaced moveTo (also snapTo(Vec3), snapTo(BlockPos, yRot, xRot))
ModEntityTypes.MY_MOB.spawn(serverLevel, pos, EntitySpawnReason.COMMAND);         // create + finalizeSpawn + add
```

### 7.2 Natural spawns (Fabric biome API)
```java
// src: fapi fabric-biome-api-v1 BiomeModifications.addSpawn
BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.CREATURE, ModEntityTypes.MINI_GOLEM, 10, 1, 3); // weight, min, max
// selectors: all(), vanilla(), foundInOverworld(), foundInTheNether(), foundInTheEnd(), tag(TagKey<Biome>), includeByKey(...), excludeByKey(...), spawnsOneOf(...)
// full API: BiomeModifications.create(id).add(ModificationPhase.ADDITIONS, selector, ctx -> ctx.getMobSpawnSettings().addSpawn(category, new MobSpawnSettings.SpawnerData(type, IntProvider count), weight))
```
Data-driven alternative (fabric biome modifiers, registry `fabric:biome_modifier`): `BiomeModifiers.addSpawns(HolderSet<Biome>, List<Weighted<MobSpawnSettings.SpawnerData>>)`, `removeSpawns(biomes, HolderSet<EntityType<?>>)` generated via datagen (fapi testmod WorldgenProvider). Vanilla 26.3 also lets datapacks set spawns through the `minecraft:gameplay/natural_mob_spawns` environment attribute (worldgen/datapack doc).

### 7.3 Spawn placement (where a natural spawn may be placed)
Either `FabricEntityType.Builder.createMob(...).spawnPlacement(type, heightmap, predicate)` (section 2) or:
```java
SpawnPlacements.register(ModEntityTypes.MINI_GOLEM, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
        Animal::checkAnimalSpawnRules);   // widened to public by fabric-transitive-access-wideners-v1
// SpawnPlacementTypes: NO_RESTRICTIONS, IN_WATER, IN_LAVA, ON_GROUND; predicates: Mob::checkMobSpawnRules, Monster::checkMonsterSpawnRules, Animal::checkAnimalSpawnRules
```
Animals also need the spawn block in `#minecraft:animals_spawnable_on` (data) for `checkAnimalSpawnRules`.

### 7.4 Spawn egg
`new Item.Properties().spawnEgg(ModEntityTypes.MINI_GOLEM)` with `SpawnEggItem::new`, add to `CreativeModeTabs.SPAWN_EGGS` (content-items-blocks.md section 9).

## 8. Projectiles
```java
// docsref/projectile/HotTaterEntity.java + HotTaterItem.java (valid on 26.3)
public class HotTaterEntity extends ThrowableItemProjectile {   // ...projectile.throwableitemprojectile
    public HotTaterEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) { super(type, level); }
    public HotTaterEntity(Level level, LivingEntity owner, ItemStack stack) { super(ModEntityTypes.HOT_TATER, owner, level, stack); }
    public HotTaterEntity(Level level, double x, double y, double z, ItemStack stack) { super(ModEntityTypes.HOT_TATER, x, y, z, level, stack); }
    @Override protected Item getDefaultItem() { return ModItems.HOT_TATER; }
    @Override protected void onHitEntity(EntityHitResult hit) { super.onHitEntity(hit); if (!level().isClientSide()) hit.getEntity().igniteForSeconds(5); }
    @Override protected void onHitBlock(BlockHitResult hit) { super.onHitBlock(hit); }
    @Override protected void onHit(HitResult hit) { super.onHit(hit); if (!level().isClientSide()) discard(); }   // else it lies there forever
}
// type: EntityType.Builder.<HotTaterEntity>of(HotTaterEntity::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10)

public class HotTaterItem extends Item implements ProjectileItem {            // ProjectileItem => dispenser support
    @Override public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction dir) {
        return new HotTaterEntity(level, pos.x(), pos.y(), pos.z(), stack);
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel sl)
            Projectile.spawnProjectileFromRotation(HotTaterEntity::new, sl, stack, player, 0.0F, 1.5F, 1.0F); // (factory, level, stack, shooter, yOffset, power, uncertainty)
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
```
Other bases: `ThrowableProjectile`, `AbstractArrow` (`projectile.arrow`), `AbstractHurtingProjectile` (`projectile.hurtingprojectile`, fireballs). `Projectile.spawnProjectileUsingShoot(...)`, `spawnProjectile(projectile, level, stack[, Consumer])`. Client: `EntityRenderers.register(ModEntityTypes.HOT_TATER, ThrownItemRenderer::new)` (client/renderer/entity/EntityRenderers.java, access-widened by Fabric).

## 9. Fabric entity events (fapi fabric-entity-events-v1)
- `ServerLivingEntityEvents`: `ALLOW_DAMAGE` (`boolean allowDamage(LivingEntity, DamageSource, float)`), `AFTER_DAMAGE` (`(entity, source, baseDamageTaken, damageTaken, blocked)`), `ALLOW_DEATH`, `AFTER_DEATH` (`(entity, source)`), `MOB_CONVERSION`.
- `ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY`.
- `ServerEntityLevelChangeEvents.AFTER_ENTITY_CHANGE_LEVEL` / `AFTER_PLAYER_CHANGE_LEVEL` (renamed from `ServerEntityWorldChangeEvents`).
- `ServerPlayerEvents`: `COPY_FROM`, `AFTER_RESPAWN`, `JOIN`, `LEAVE`, `ALLOW_DEATH`.
- `EntitySleepEvents` (10 events: `ALLOW_SLEEPING`, `START_SLEEPING`, `STOP_SLEEPING`, `ALLOW_BED`, `ALLOW_NEARBY_MONSTERS`, `ALLOW_RESETTING_TIME`, `MODIFY_SLEEPING_DIRECTION`, `ALLOW_SETTING_SPAWN`, `SET_BED_OCCUPATION_STATE`, `MODIFY_WAKE_UP_POSITION`), `EntityElytraEvents.ALLOW` / `CUSTOM`.
- `effect/ServerMobEffectEvents`: `ALLOW_ADD`, `BEFORE_ADD`, `AFTER_ADD`, `ALLOW_EARLY_REMOVE`, `BEFORE_REMOVE`, `AFTER_REMOVE`.
- Interaction: `UseEntityCallback`, `AttackEntityCallback` (fabric-events-interaction-v0). Lifecycle load/unload: `ServerEntityEvents.ENTITY_LOAD` / `ENTITY_UNLOAD` `(entity, level)` (fabric-lifecycle-events-v1).

## 10. Client rendering pointers (see client doc)
- `EntityRenderers.register(type, Renderer::new)` in a `ClientModInitializer`.
- Model layers: `ModelLayerRegistry.registerModelLayer(ModelLayerLocation, TexturedLayerDefinitionProvider)` (renamed from `EntityModelLayerRegistry` in 26.1).
- Renderers use render states: `MobRenderer<E, S extends LivingEntityRenderState, M>` with `createRenderState()`, `extractRenderState(entity, state, partialTick)`, `getTextureLocation(S)` (docsref client `entity/renderer/MiniGolemEntityRenderer.java`). Baby/adult model split changed in 26.1 (primer "Entity Textures and Adult/Baby Models").
- Textures `assets/<ns>/textures/entity/...`; extra layers: `LivingEntityRenderLayerRegistrationCallback` (renamed from `LivingEntityFeatureRendererRegistrationCallback`).

## 11. Data-driven entity content (no code)
Mob variants (`wolf_variant`, `pig_variant`, `cat_variant`, `cow_variant`, `chicken_variant`, `frog_variant`, `zombie_nautilus_variant`), sound variants (`*_sound_variant`, 26.1), painting variants, sulfur cube archetypes (26.2), trial spawner configs, damage types, loot tables, entity tags. Adding a variant for a vanilla mob = JSON only (datapack doc). A custom mob with its own variant registry = `DynamicRegistries.registerSynced(...)` + a data component/synced data holding `Holder<YourVariant>`.

## 12. Gotchas
- No `FabricDefaultAttributeRegistry.register` for a `LivingEntity` type -> crash when the entity is created.
- Logic that changes state must run server-side (`!level().isClientSide()` or `level() instanceof ServerLevel`); `hurtServer` needs a `ServerLevel`.
- Spawn eggs/`addSpawn` referencing an entity type before its class is loaded -> NPE / "Unregistered entity type".
- `MobCategory.MISC` mobs cannot be added to biome spawns; `MISC` also ignores mob caps.
- Entity `sized(...)` also sets the client hitbox; `eyeHeight` defaults to 0.85*height.
- Custom `EntityDataSerializer` without `FabricEntityDataRegistry.register` desyncs when registration order differs between client and server.
