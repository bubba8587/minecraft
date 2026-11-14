# Persistent data on 26.3: Data Attachments, SavedData, player data, block entity sync

Scope: storing mod data on entities/players/block entities/chunks/levels/the server and syncing it, for MC 26.3 / Fabric API 0.166.2+26.3. Verified 2026-11-14 against fabric-api 26.3 (`fabric-data-attachment-api-v1`, `fabric-object-builder-api-v1`) and decompiled 26.3.

## Choosing
| Need | Use |
|---|---|
| Per-entity / per-player / per-block-entity / per-chunk / per-level value | Data Attachment API |
| Server-wide value not tied to a level | `GlobalAttachments` (`server.globalAttachments()`) or vanilla `SavedData` on `server.getDataStorage()` |
| Large structured world data (indexes, claims DB) | `SavedData` (one `.dat` per type) |
| Data on your own block entity | fields + `saveAdditional/loadAdditional` (systems-serialization.md) |
| Data on an ItemStack | data components (items doc), never attachments |

## 1. Data Attachment API (fabric-data-attachment-api-v1)

Targets (`AttachmentTarget`, interface-injected): `Entity` (incl. players), `BlockEntity`, `Level` (persistence only for `ServerLevel`), `ChunkAccess`, and `GlobalAttachments`. (src: fabric-api fabric-data-attachment-api-v1 api/attachment/v1/AttachmentTarget.java; decompiled `Level implements AttachmentTarget, GlobalAttachmentsProvider`, `MinecraftServer implements GlobalAttachmentsProvider`)

### Create (static init, class must be loaded from `onInitialize`)
```java
public final class ModAttachments {
    // transient, not saved
    public static final AttachmentType<String> LAST_ACTION = AttachmentRegistry.create(id("last_action"));
    // default value via getAttachedOrCreate(type)
    public static final AttachmentType<Integer> CLICKS = AttachmentRegistry.createDefaulted(id("clicks"), () -> 0);
    // persistent
    public static final AttachmentType<BlockPos> HOME = AttachmentRegistry.createPersistent(id("home"), BlockPos.CODEC);
    // full builder
    public static final AttachmentType<Integer> MANA = AttachmentRegistry.create(id("mana"), b -> b
            .initializer(() -> 100)
            .persistent(Codec.INT)
            .copyOnDeath()                                                // survive death / mob conversion
            .syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly())); // only the owning player
    public static void init() {}   // call from onInitialize to force class load
}
```
Builder methods (src: AttachmentRegistry.java): `persistent(Codec<A>)`, `copyOnDeath()`, `initializer(Supplier<A>)`, `syncWith(StreamCodec<? super RegistryFriendlyByteBuf, A>, AttachmentSyncPredicate)`, `syncWith(codec, predicate, int maxSyncSize)` (default about 1 MiB minus padding), `buildAndRegister(id)`. `AttachmentRegistry.builder()` is deprecated; use `create(id, consumer)`.

Sync predicates: `AttachmentSyncPredicate.all()`, `targetOnly()` (target must be a player), `allButTarget()`, or custom `(AttachmentTarget target, ServerPlayer player) -> boolean`.

### Read / write (injected methods)
```java
int mana = player.getAttachedOrCreate(ModAttachments.MANA);   // uses initializer
BlockPos home = player.getAttached(ModAttachments.HOME);       // @Nullable
player.setAttached(ModAttachments.MANA, mana - 10);            // returns previous; triggers save-dirty + sync
player.modifyAttached(ModAttachments.MANA, m -> m == null ? 100 : m + 1);
player.removeAttached(ModAttachments.HOME);
boolean has = player.hasAttached(ModAttachments.HOME);
level.getAttachedOrElse(TYPE, def); chunk.getAttachedOrGet(TYPE, () -> def); be.getAttachedOrThrow(TYPE);
player.onAttachedSet(ModAttachments.MANA).register((oldV, newV) -> { ... });   // per-target change event
server.globalAttachments().setAttached(TYPE, value);   // also level.globalAttachments()
```
Full `AttachmentTarget` API: `getAttached, getAttachedOrThrow, getAttachedOrSet(type, default), getAttachedOrCreate(type[, supplier]), getAttachedOrElse, getAttachedOrGet, setAttached, hasAttached, removeAttached, modifyAttached, onAttachedSet`.

### Rules and gotchas
- Values must be IMMUTABLE (records, boxed primitives, immutable lists). Mutating an attached object in place will not mark it dirty or sync. If you must mutate: call `blockEntity.setChanged()` / `chunk.markUnsaved()` yourself (sync still will not happen).
- Changing one field means replacing the whole value -> full resync. Split frequently-changing values into separate attachment types (fabric-docs "Larger Attachments").
- Synced attachments require the client to have the mod; during configuration the server asks the client which attachment ids it supports (`fabric:accepted_attachments_v1`), and only syncs those. Vanilla clients just do not receive them (no disconnect). (src: impl/attachment/sync/AttachmentSync.java)
- Initial sync happens on player join (global + level + own player data), on level change, and on `EntityTrackingEvents.START_TRACKING` for entities.
- Persistent data is stored in NBT under key `fabric:attachments` (`AttachmentTarget.NBT_ATTACHMENT_KEY`); level + global attachments are stored in SavedData `data/fabric/attachments.dat`. (src: impl/attachment/AttachmentSavedData.java)
- `copyOnDeath()` handles player respawn and mob conversion. Without it, a respawned `ServerPlayer` is a new object and loses the attachment. (For non-attachment data, use `ServerPlayerEvents.COPY_FROM`.) The copy runs in a `ServerPlayerEvents.AFTER_RESPAWN` listener (fired from `PlayerList#respawn`), **not** in `ServerPlayer#restoreFrom`/`COPY_FROM`, so calling `newPlayer.restoreFrom(old, false)` by hand copies nothing. To test it, really respawn: `server.getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED)` on a `helper.makeMockServerPlayerInLevel()` player returns the new player with the attachment (src: fapi fabric-data-attachment-api-v1 impl/attachment/AttachmentEntrypoint.java; verified by refmod gametest `lampTogglesAttachmentPersistsAndSurvivesDeath`).
- Block entities: Fabric strips attachments from `BlockEntity#getUpdateTag` so persistent attachments do not leak to clients via vanilla BE sync; modded BEs that sync differently may still leak.
- Chunks with `ChunkStatus.EMPTY` are not saved until at least `STRUCTURE_STARTS`; `setAttached` logs a warning.
- Chunk attachments are written into the chunk NBT under `fabric:attachments` by `SerializableChunkData.copyOf(...).write()` and read back by `parse(...).read(...)`. A `LevelChunk` read from disk arrives wrapped in an `ImposterProtoChunk`, which forwards attachment calls to it. Proven by refmod gametest `barrelTallyLivesInChunkAttachmentAndSurvivesChunkSaveLoad`; per-`BlockPos` data on existing blocks: `no-new-blocks.md` section 4.
- Registering in a static initializer that never runs = attachments silently never registered = data dropped on load.
- System property `fabric.attachment.disconnect_on_unknown_targets` makes the client disconnect on unknown sync targets (debug).

## 2. SavedData (vanilla world-level storage)

(src: common net/minecraft/world/level/saveddata/SavedData.java, SavedDataType.java, net/minecraft/world/level/storage/SavedDataStorage.java)

26.x shape: `SavedDataType<T extends SavedData>(Identifier id, Supplier<T> constructor, Codec<T> codec, DataFixTypes dataFixType)`. Storage class is `SavedDataStorage` (was `DimensionDataStorage`/`PersistentStateManager`). File: `<storage folder>/<namespace>/<path>.dat` (id resolved with `Identifier#resolveAgainst`), data under the `"data"` key, gzip NBT.

```java
public class ClaimsData extends SavedData {
    public static final Codec<ClaimsData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, BlockPos.CODEC.listOf()).fieldOf("claims").forGetter(d -> d.claims)
    ).apply(i, ClaimsData::new));
    public static final SavedDataType<ClaimsData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("mymod", "claims"), ClaimsData::new, CODEC, null); // null DataFixTypes OK with Fabric API
    private final Map<UUID, List<BlockPos>> claims;
    public ClaimsData() { this(new HashMap<>()); }
    private ClaimsData(Map<UUID, List<BlockPos>> m) { this.claims = new HashMap<>(m); }
    public void add(UUID u, BlockPos p) { claims.computeIfAbsent(u, k -> new ArrayList<>()).add(p); setDirty(); }

    public static ClaimsData get(MinecraftServer server) { return server.getDataStorage().computeIfAbsent(TYPE); }   // global: <world>/data/mymod/claims.dat
    public static ClaimsData get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(TYPE); }       // per dimension
}
```
- `SavedDataStorage` API: `computeIfAbsent(type)`, `get(type)` (nullable), `set(type, data)` (marks dirty), `scheduleSave()`, `saveAndJoin()`.
- `MinecraftServer#getDataStorage()` -> `<world>/data/` (global; new in 26.x, vanilla stores weather, clocks, scoreboard there). `ServerLevel#getDataStorage()` -> per-dimension data folder.
- ALWAYS call `setDirty()` after mutating, or nothing is saved.
- Passing `null` as `DataFixTypes` would NPE in vanilla `readTagFromDisk`; Fabric API (`fabric-object-builder-api-v1` `SavedDataStorageMixin`) makes `null` safe. Without Fabric API use a real `DataFixTypes` value.
- The codec is decoded with `RegistryOps` (registry-aware), so Holder/ItemStack codecs work.
- Parse errors are logged and the data is treated as absent (a fresh instance replaces it on next `computeIfAbsent`), so a codec bug can wipe data: test migrations.
- 1.21.4- style `SavedData.Factory`, `save(CompoundTag, Provider)` and string ids are gone.

## 3. Player data

- Preferred: persistent attachments on `ServerPlayer` (+ `copyOnDeath()` if it should survive death; data always survives logout because it is written into the player NBT).
- Vanilla player file `<world>/playerdata/<uuid>.dat` is handled by `PlayerDataStorage` (src: common net/minecraft/world/level/storage/PlayerDataStorage.java); custom fields there require a mixin into `ServerPlayer#addAdditionalSaveData/readAdditionalSaveData` (ValueOutput/ValueInput).
- Offline player data: not loaded; for offline queries keep a server-level index in `SavedData` keyed by UUID.
- Respawn copy hook: `ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> ...)` (`alive` = whether the old player is still alive, i.e. true when returning from the End, false on death respawn). `ServerPlayerEvents.AFTER_RESPAWN` after.
- There is no NeoForge-style `getPersistentData()` on entities in vanilla/Fabric.

## 4. Block entity save + client sync (vanilla)

```java
@Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); out.putInt("progress", progress); }
@Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); progress = in.getIntOr("progress", 0); }

// client sync of BE data (needed for renderers / client logic)
@Override public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
@Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }

void setProgress(int p) {
    progress = p;
    setChanged();                                                      // mark chunk dirty (+ comparator update)
    if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
}
```
(src: common BlockEntity.java, SignBlockEntity.java getUpdateTag, ClientboundBlockEntityDataPacket.java)
- Default `getUpdateTag` returns an empty tag: no sync unless overridden.
- `saveCustomOnly` excludes components and id/pos metadata; `saveWithoutMetadata` includes components.
- Data components on a BE: `applyImplicitComponents(DataComponentGetter)` / `collectImplicitComponents(DataComponentMap.Builder)` for item <-> BE transfer.

## Where to look
- fabric-api `fabric-data-attachment-api-v1/src/testmod/.../AttachmentTestMod.java`, `gametest/AttachmentCopyTests.java`, `testmodClient/.../gametest/SyncGametest.java`.
- fabric-docs `develop/serialization/data-attachments.md`, `develop/serialization/saved-data.md` (accurate for 26.x).
