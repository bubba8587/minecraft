# Events on 26.3 with Fabric API

Scope: Fabric API event callbacks (lifecycle, tick, level, chunk, entity, player, interaction, effects) and defining your own events, MC 26.3 / Fabric API 0.166.2+26.3. Verified 2026-11-14 against fabric-api branch 26.3 (`fabric-api-base`, `fabric-lifecycle-events-v1`, `fabric-entity-events-v1`, `fabric-events-interaction-v0`, `fabric-networking-api-v1`).

All events: `SomeEvents.EVENT_NAME.register(lambda)`; optional phase: `register(Identifier phase, listener)`. Register in `onInitialize` (common) or `onInitializeClient` (client-only events).

## 1. Lifecycle / tick / level / chunk (fabric-lifecycle-events-v1, `net.fabricmc.fabric.api.event.lifecycle.v1`)

| Event | Callback signature |
|---|---|
| `ServerLifecycleEvents.SERVER_STARTING` / `SERVER_STARTED` / `SERVER_STOPPING` / `SERVER_STOPPED` | `(MinecraftServer server)` |
| `ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS` | `(ServerPlayer player, boolean joined)`: send datapack-derived data to clients after join or `/reload` |
| `ServerLifecycleEvents.START_DATA_PACK_RELOAD` / `END_DATA_PACK_RELOAD` | `(server, CloseableResourceManager[, boolean success])` |
| `ServerLifecycleEvents.BEFORE_SAVE` / `AFTER_SAVE` | `(server, boolean flush, boolean force)` |
| `ServerTickEvents.START_SERVER_TICK` / `END_SERVER_TICK` | `(MinecraftServer)` |
| `ServerTickEvents.START_LEVEL_TICK` / `END_LEVEL_TICK` | `(ServerLevel)` (was `START_WORLD_TICK`) |
| `ServerLevelEvents.LOAD` / `UNLOAD` | `(MinecraftServer, ServerLevel)` (class was `ServerWorldEvents`) |
| `ServerChunkEvents.CHUNK_LOAD` | `(ServerLevel, LevelChunk, boolean generated)`; `CHUNK_GENERATE` is deprecated in favour of this flag |
| `ServerChunkEvents.CHUNK_UNLOAD` | `(ServerLevel, LevelChunk)` |
| `ServerChunkEvents.FULL_CHUNK_STATUS_CHANGE` | `(ServerLevel, LevelChunk, FullChunkStatus old, FullChunkStatus new)` (was `LevelTypeChange`) |
| `ServerEntityEvents.ENTITY_LOAD` / `ENTITY_UNLOAD` | `(Entity, ServerLevel)` |
| `ServerEntityEvents.ALLOW_LOAD` | `(Entity, ServerLevel, @Nullable EntitySpawnReason, boolean isLoadedFromDisk) -> boolean` (cancel = entity never loads) |
| `ServerEntityEvents.EQUIPMENT_CHANGE` | `(LivingEntity, EquipmentSlot, ItemStack previous, ItemStack current)` |
| `ServerBlockEntityEvents.BLOCK_ENTITY_LOAD` / `_UNLOAD` | `(BlockEntity, ServerLevel)` |
| `CommonLifecycleEvents.TAGS_LOADED` | `(RegistryAccess, boolean client)` |
Client (`net.fabricmc.fabric.api.client.event.lifecycle.v1`): `ClientLifecycleEvents.CLIENT_STARTED/CLIENT_STOPPING`, `ClientTickEvents.START/END_CLIENT_TICK (Minecraft)`, `START/END_LEVEL_TICK (ClientLevel)`, `ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE`, `ClientEntityEvents.ENTITY_LOAD/UNLOAD`, `ClientChunkEvents.CHUNK_LOAD/UNLOAD`, `ClientBlockEntityEvents.BLOCK_ENTITY_LOAD/UNLOAD`.

## 2. Player / entity events (fabric-entity-events-v1, `net.fabricmc.fabric.api.entity.event.v1`)

| Event | Signature / notes |
|---|---|
| `ServerPlayerEvents.JOIN` / `LEAVE` | `(ServerPlayer)` |
| `ServerPlayerEvents.COPY_FROM` | `(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive)`: copy custom data on respawn/End exit |
| `ServerPlayerEvents.AFTER_RESPAWN` | `(old, new, alive)` |
| `ServerPlayerEvents.ALLOW_DEATH` | `(ServerPlayer, DamageSource, float) -> boolean` |
| `ServerLivingEntityEvents.ALLOW_DAMAGE` | `(LivingEntity, DamageSource, float amount) -> boolean` (from `hurtServer`, before armor) |
| `ServerLivingEntityEvents.AFTER_DAMAGE` | `(LivingEntity, DamageSource, float baseTaken, float taken, boolean blocked)` (not fired if killed) |
| `ServerLivingEntityEvents.ALLOW_DEATH` / `AFTER_DEATH` | `(LivingEntity, DamageSource[, float])` |
| `ServerLivingEntityEvents.MOB_CONVERSION` | `(Mob previous, Mob converted, ConversionParams)` |
| `ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY` | `(ServerLevel, Entity killer, LivingEntity killed, DamageSource)` |
| `ServerEntityLevelChangeEvents.AFTER_ENTITY_CHANGE_LEVEL` | `(Entity original, Entity newEntity, ServerLevel origin, ServerLevel dest)` |
| `ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL` | `(ServerPlayer, origin, dest)` |
| `EntitySleepEvents.ALLOW_SLEEPING`, `START_SLEEPING`, `STOP_SLEEPING`, `ALLOW_BED`, `ALLOW_NEARBY_MONSTERS`, `ALLOW_RESETTING_TIME`, `MODIFY_SLEEPING_DIRECTION`, `ALLOW_SETTING_SPAWN`, `SET_BED_OCCUPATION_STATE`, `MODIFY_WAKE_UP_POSITION` | several return `EventResult` (`allowBed`, `allowSleepTime`, `allowNearbyMonsters`) |
| `EntityElytraEvents.ALLOW` / `CUSTOM` | elytra flight control |
| `effect.ServerMobEffectEvents.ALLOW_ADD / BEFORE_ADD / AFTER_ADD / ALLOW_EARLY_REMOVE / BEFORE_REMOVE / AFTER_REMOVE` | `(MobEffectInstance, LivingEntity, EffectEventContext)` |
Networking module: `ServerPlayConnectionEvents.JOIN/DISCONNECT`, `EntityTrackingEvents.START_TRACKING/STOP_TRACKING (Entity, ServerPlayer)` (see systems-networking.md).

## 3. Interaction events (fabric-events-interaction-v0, `net.fabricmc.fabric.api.event.player`)

Fired on BOTH logical sides unless noted (check `level.isClientSide()`), before the spectator check (check `player.isSpectator()`).

| Event | Signature | Return semantics |
|---|---|---|
| `UseBlockCallback.EVENT` | `(Player, Level, InteractionHand, BlockHitResult) -> InteractionResult` | non-PASS stops processing; on client SUCCESS sends packet, FAIL does not |
| `UseItemCallback.EVENT` | `(Player, Level, InteractionHand) -> InteractionResult` | same |
| `UseEntityCallback.EVENT` | `(Player, Level, InteractionHand, Entity, EntityHitResult) -> InteractionResult` | same |
| `AttackBlockCallback.EVENT` | `(Player, Level, InteractionHand, BlockPos, Direction) -> InteractionResult` | client: SUCCESS swings + packet, CONSUME packet no swing |
| `AttackEntityCallback.EVENT` | `(Player, Level, InteractionHand, Entity, @Nullable EntityHitResult) -> InteractionResult` | |
| `PlayerBlockBreakEvents.BEFORE` | `(Level, Player, BlockPos, BlockState, @Nullable BlockEntity) -> boolean` | SERVER only; false cancels then fires `CANCELED` |
| `PlayerBlockBreakEvents.AFTER` / `CANCELED` | same args, void | server only |
| `PlayerPickItemEvents.BLOCK` / `ENTITY` | `(ServerPlayer, BlockPos/Entity, ..., boolean includeData) -> ItemStack` | server-side pick block |
| `BlockEvents.USE_ITEM_ON` | `(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult) -> @Nullable InteractionResult` | runs at `Block#useItemOn`; non-null replaces it (new 26.1) |
| `BlockEvents.USE_WITHOUT_ITEM` | `(BlockState, Level, BlockPos, Player, BlockHitResult) -> @Nullable InteractionResult` | at `Block#useWithoutItem` |
| `ItemEvents.USE_ON` | `(UseOnContext) -> @Nullable InteractionResult` | at `Item#useOn` |
| `ItemEvents.USE` | `(Level, Player, InteractionHand) -> @Nullable InteractionResult` | at `Item#use` |

`BlockEvents`/`ItemEvents` keep vanilla's surrounding logic (cooldowns, stats, criteria) and are narrower than `UseBlockCallback`/`UseItemCallback`, which short-circuit everything.

`InteractionResult` in 26.x is a sealed interface (src: common net/minecraft/world/InteractionResult.java): constants `SUCCESS` (swing predicted on client), `SUCCESS_SERVER` (swing from server only), `CONSUME` (no swing), `FAIL`, `PASS`, `TRY_WITH_EMPTY_HAND`; records `Success(SwingSource, ItemContext)`, `Fail`, `Pass`, `TryEmptyHandInteraction`. Item-transform results: `InteractionResult.SUCCESS.heldItemTransformedTo(stack)` (returns `Success`; replaces the held item, e.g. bucket filling), `.withoutItem()`. `consumesAction()` is true only for `Success`.

`FakePlayer.get(ServerLevel[, GameProfile])` (`net.fabricmc.fabric.api.entity.FakePlayer`) for machines acting as players; `FakePlayer.DEFAULT_UUID`.

### Server-only mod gotcha (desync)
Cancelling on the server only (e.g. claim protection with `UseBlockCallback` returning `FAIL` when `!level.isClientSide()`) leaves the client's prediction (placed block, opened door, consumed item) visible until corrected. Resync manually: send the real block state (`player.connection.send(new ClientboundBlockUpdatePacket(level, pos))` (ctor `(BlockGetter, BlockPos)` exists), also the adjacent pos for placements) and the inventory (`player.containerMenu.sendAllDataToRemote()`). Multi-block blocks (doors, beds) need every part resent; this resync recipe is common practice, not something Fabric does for you.

## 4. Other event-ish modules (pointers)
- Chat: `ServerMessageEvents`, `ServerMessageDecoratorEvent` (systems-networking.md section 6).
- Commands: `CommandRegistrationCallback`, `ClientCommandRegistrationCallback` (systems-commands-text.md).
- Loot: `LootTableEvents` (fabric-loot-api-v3; covered in loot/datagen docs).
- Items: `DefaultItemComponentEvents.MODIFY` (26.3 fuel/compost now components), `CreativeModeTabEvents` (was ItemGroupEvents).
- Block transformers: `BlockTransformerEvents.MODIFY` (26.3, replaces Strippable/Tillable/FlattenableBlockRegistry).
- Fluids: `FluidFlowCallback.EVENT` (26.3) `(fluid, level, pos) -> boolean`.
- Dimensions: `DimensionEvents.MODIFY_ATTRIBUTES` (systems-world.md).
- Permissions: `PermissionEvents.ON_REQUEST` (systems-commands-text.md).

## 5. Defining your own event (fabric-api-base)

`EventFactory` (src: fabric-api fabric-api-base api/event/EventFactory.java): `createArrayBacked(Class<? super T>, Function<T[], T> invokerFactory)`, `createArrayBacked(Class<T>, T emptyInvoker, Function<T[], T>)`, `createWithPhases(Class<? super T>, Function<T[], T>, Identifier... defaultPhases)` (must include `Event.DEFAULT_PHASE` = `fabric:default`). `Event<T>`: `invoker()`, `register(T)`, `register(Identifier phase, T)`, `addPhaseOrdering(first, second)`.

```java
@FunctionalInterface
public interface SheepShearCallback {
    Event<SheepShearCallback> EVENT = EventFactory.createArrayBacked(SheepShearCallback.class,
        listeners -> (player, sheep) -> {
            for (SheepShearCallback l : listeners) {
                InteractionResult r = l.interact(player, sheep);
                if (r != InteractionResult.PASS) return r;   // first non-PASS wins
            }
            return InteractionResult.PASS;
        });
    InteractionResult interact(Player player, Sheep sheep);
}

// fire it (usually from a mixin into vanilla code)
InteractionResult r = SheepShearCallback.EVENT.invoker().interact(player, (Sheep)(Object)this);
if (r == InteractionResult.FAIL) { cir.setReturnValue(InteractionResult.FAIL); }
```
(pattern from fabric-docs reference `event/SheepShearCallback.java`)
Invoker idioms: void broadcast (loop all); boolean "allow" (return false on first false); `EventResult` (`net.fabricmc.fabric.api.util.EventResult`: `ALLOW`, `PASS`, `DENY`; `allowAction()`, `allowAction(boolean passResult)`); `TriState` (`net.fabricmc.fabric.api.util.TriState`); nullable-result "first non-null wins" (as `BlockEvents`).

Phases: `Identifier EARLY = Identifier.fromNamespaceAndPath("mymod", "early"); EVENT.addPhaseOrdering(EARLY, Event.DEFAULT_PHASE); EVENT.register(EARLY, listener);`

Gotchas:
- Keep the invoker allocation-free on hot paths (called every tick for tick events).
- `EventFactory.invalidate()` is deprecated for removal; `getHandlerName`/`isProfilingEnabled` deprecated.
- Client-only parameter types (e.g. `LocalPlayer`) in a common event interface will crash dedicated servers on class load.

## Where to look
- fabric-api `fabric-lifecycle-events-v1/src/testmod`, `fabric-entity-events-v1/src/testmod/.../entity/event/EntityEventTests.java`, `fabric-events-interaction-v0/src/testmod`.
- fabric-docs `develop/events/index.md`.
