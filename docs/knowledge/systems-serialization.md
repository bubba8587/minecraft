# Serialization on 26.3: Codec, StreamCodec, ValueInput/ValueOutput, NBT

Scope: data (de)serialization for Fabric mods on MC 26.3 / Fabric API 0.166.2+26.3. Verified 2026-11-14 against decompiled 26.3 source + DFU 10.0.21 (`com.mojang:datafixerupper`, javap) + fabric-api branch 26.3.

Three separate systems, do not mix them up:

| System | Purpose | Key types |
|---|---|---|
| DFU `Codec` / `MapCodec` | JSON (datapacks, config), NBT (saves), registry contents, item components | `com.mojang.serialization.Codec`, `MapCodec`, `RecordCodecBuilder`, `DataResult`, `DynamicOps` (`JsonOps.INSTANCE`, `NbtOps.INSTANCE`, `RegistryOps`) |
| `StreamCodec` | Network byte streams (packets, synced attachments, menu opening data, component sync) | `net.minecraft.network.codec.StreamCodec`, `ByteBufCodecs`, `FriendlyByteBuf`, `RegistryFriendlyByteBuf` |
| `ValueInput` / `ValueOutput` | NBT-backed save/load of block entities, entities, etc. (wraps Codecs) | `net.minecraft.world.level.storage.ValueInput`, `ValueOutput`, `TagValueInput`, `TagValueOutput` |

## 1. Codec basics

Primitive codecs (DFU): `Codec.BOOL, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, STRING, BYTE_BUFFER, INT_STREAM, LONG_STREAM, PASSTHROUGH`, `Codec.EMPTY` (MapCodec<Unit>).

Combinators on `Codec<A>` (DFU 10.0.21, verified by javap):
- `listOf()`, `listOf(min, max)`, `sizeLimitedListOf(max)`
- `xmap(to, from)`, `comapFlatMap(DataResult-to, from)`, `flatComapMap`, `flatXmap`
- `fieldOf(name)` -> `MapCodec<A>`; `optionalFieldOf(name)` -> `MapCodec<Optional<A>>`; `optionalFieldOf(name, default)`; `lenientOptionalFieldOf(...)` (errors in the field become "absent" instead of failing)
- `validate(a -> DataResult<A>)`, `orElse(default)`, `orElseGet`, `promotePartial`
- `dispatch(typeKey, A::type, type -> MapCodec)`, `dispatchStable`, `partialDispatch`, `dispatchMap`
- `withAlternative(otherCodec)` / static `Codec.withAlternative(primary, alt, converter)`
- statics: `Codec.pair`, `either`, `xor`, `list`, `unboundedMap(k, v)`, `simpleMap(k, v, Keyable)`, `dispatchedMap`, `recursive(name, self -> codec)`, `lazyInitialized(supplier)`, `intRange(min,max)`, `floatRange`, `doubleRange`, `string(min,max)`, `sizeLimitedString`, `stringResolver`

### RecordCodecBuilder
```java
public record Waypoint(String name, BlockPos pos, Optional<Identifier> icon, int color) {
    public static final Codec<Waypoint> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(Waypoint::name),
            BlockPos.CODEC.fieldOf("pos").forGetter(Waypoint::pos),
            Identifier.CODEC.optionalFieldOf("icon").forGetter(Waypoint::icon),
            Codec.INT.optionalFieldOf("color", 0xFFFFFF).forGetter(Waypoint::color)
    ).apply(i, Waypoint::new));

    // MapCodec form (for dispatch subtypes, ValueOutput.store(MapCodec,..), recipe serializers, etc.)
    public static final MapCodec<Waypoint> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(/* same */).apply(i, Waypoint::new));
}
```
- Max 16 fields per `group(...)`; nest records or use `MapCodec` composition beyond that.
- `MapCodec#codec()` turns a MapCodec into a Codec; `Codec#fieldOf` turns a Codec into a MapCodec with one key.

### Vanilla helper codecs (26.3)
- `Identifier.CODEC`, `BlockPos.CODEC`, `Vec3.CODEC`, `UUIDUtil.CODEC` (int-array form), `UUIDUtil.STRING_CODEC`, `UUIDUtil.LENIENT_CODEC`, `ComponentSerialization.CODEC`, `ItemStack.CODEC` (non-empty), `ItemStack.OPTIONAL_CODEC`, `ItemStack.MAP_CODEC`, `ItemStackTemplate.CODEC`, `CompoundTag.CODEC`, `CustomData.CODEC`.
- Registry entries: `BuiltInRegistries.ITEM.byNameCodec()` (value by id), `.holderByNameCodec()` (Holder by id).
- Registry-aware (need `RegistryOps`): `RegistryCodecs.holder(Registries.X)` (reference only, = `RegistryFixedCodec`), `RegistryCodecs.holder(key, elementCodec)` (reference or inline, = `RegistryFileCodec`), `RegistryCodecs.holderSet(key)` (accepts `"#tag"`, id, or list). Package moved to `net.minecraft.core.registries.codec` in 26.x (`RegistryCodecs`, `RegistryFixedCodec`, `RegistryFileCodec`, `HolderSetCodec`). (src: common net/minecraft/core/registries/codec/)
- `ResourceKey.codec(Registries.X)`, `TagKey.codec(Registries.X)` (plain id, no `#`), `TagKey.hashedCodec(Registries.X)` (expects `#ns:path`).
- `ExtraCodecs` (src: common net/minecraft/util/ExtraCodecs.java): `JSON, NBT, NON_NEGATIVE_INT, POSITIVE_INT, NON_NEGATIVE_FLOAT, POSITIVE_FLOAT, NON_EMPTY_STRING, RGB_COLOR_CODEC, ARGB_COLOR_CODEC, STRING_RGB_COLOR, PATTERN, INSTANT_ISO8601, CODEPOINT, PLAYER_NAME, UNTRUSTED_URI, QUATERNIONF, BIT_SET`, methods `intRange`, `floatRange`, `nonEmptyList`, `nonEmptyHolderSet`, `strictUnboundedMap`, `compactListCodec`, `idResolverCodec`, `orCompressed`, `optionalEmptyMap`, `legacyEnum`.
- Enums: `implements StringRepresentable` + `StringRepresentable.fromEnum(MyEnum::values)` (returns `EnumCodec<E>`). Example: `PermissionLevel.CODEC`.

### Encoding / decoding and DataResult
```java
JsonElement json = Waypoint.CODEC.encodeStart(JsonOps.INSTANCE, wp).getOrThrow();
Waypoint back = Waypoint.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
// Registry-aware (holders, tags, registry-backed components like ItemStack):
RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE); // HolderLookup.Provider
Tag nbt = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
```
`DataResult<R>` (DFU 10.0.21) API: `result()` Optional, `error()` Optional<Error>, `isSuccess()`, `isError()`, `getOrThrow()` (throws IllegalStateException), `getOrThrow(Function<String,E>)`, `getPartialOrThrow()`, `resultOrPartial(Consumer<String> onError)`, `resultOrPartial()`, `hasResultOrPartial()`, `ifSuccess(c)`, `ifError(c)`, `map`, `flatMap`, `mapOrElse`, `mapError`, `promotePartial`, `setPartial`, `apply2`, `lifecycle()`. Factories `DataResult.success(x)`, `DataResult.error(() -> "msg")`, `DataResult.error(() -> "msg", partial)`.

Idiom used by vanilla for tolerant loading (src: SavedDataStorage.readSavedData):
```java
codec.parse(ops, tag).resultOrPartial(err -> LOGGER.error("Failed to parse {}: {}", id, err)).orElse(null);
```
Gotcha: `error(...)` takes a `Supplier<String>`, not a String.

Registry dispatch pattern (custom polymorphic types): create a registry of `MapCodec<? extends T>` and `Codec<T> CODEC = REGISTRY.byNameCodec().dispatch("type", T::codec, c -> c)`. This is exactly how 26.3 vanilla does `Feature.DIRECT_CODEC = BuiltInRegistries.FEATURE_TYPE.byNameCodec().dispatch(Feature::codec, t -> t)`. (src: common net/minecraft/world/level/levelgen/feature/Feature.java)

## 2. StreamCodec (network)

`StreamCodec<B, V>` where `B` is the buffer type: `ByteBuf` (context-free), `FriendlyByteBuf`, or `RegistryFriendlyByteBuf` (carries `RegistryAccess`; needed for items, components, holders). Play-phase payloads use `RegistryFriendlyByteBuf`; configuration-phase payloads use `FriendlyByteBuf` (no registries yet).

Statics (src: common net/minecraft/network/codec/StreamCodec.java): `StreamCodec.of(encoder, decoder)`, `ofMember(memberEncoder, decoder)`, `unit(instance)`, `composite(c1, getter1, ..., ctor)` (up to 14 fields), `recursive(f)`. Defaults: `map(to, from)`, `mapStream`, `dispatch(typeFn, codecFn)`, `apply(CodecOperation)`, `cast()`.

`ByteBufCodecs` constants (src: common net/minecraft/network/codec/ByteBufCodecs.java): `BOOL, BYTE, ROTATION_BYTE, SHORT, UNSIGNED_SHORT, INT, VAR_INT, OPTIONAL_VAR_INT, LONG, VAR_LONG, FLOAT, DOUBLE, BYTE_ARRAY, LONG_ARRAY, BIT_SET, STRING_UTF8, TAG, TRUSTED_TAG, COMPOUND_TAG, TRUSTED_COMPOUND_TAG, VECTOR3F, QUATERNIONF, CONTAINER_ID, GAME_PROFILE, PLAYER_NAME, RGB_COLOR, INSTANT, PUBLIC_KEY`.
Methods: `stringUtf8(max)`, `byteArray(max)`, `optional(c)`, `collection(ctor, c)`/`collection(ctor, c, max)`, `list()` / `list(max)` (as `CodecOperation`, use `c.apply(ByteBufCodecs.list())`), `map(ctor, k, v[, max])`, `either(l, r)`, `idMapper(byId, toId)`, `registry(Registries.X)` (value by network id), `holderRegistry(key)`, `holder(key, directCodec)`, `holderSet(key)`, `fromCodec(codec)` (NBT-encodes a Codec, context-free), `fromCodecWithRegistries(codec)` (for registry-dependent codecs), `fromCodecTrusted`, `lengthPrefixed(max)`.

Vanilla stream codecs: `BlockPos.STREAM_CODEC` (ByteBuf), `Vec3.STREAM_CODEC`, `UUIDUtil.STREAM_CODEC`, `Identifier.STREAM_CODEC`, `ComponentSerialization.STREAM_CODEC` (RegistryFriendlyByteBuf), `ItemStack.STREAM_CODEC` (non-empty), `ItemStack.OPTIONAL_STREAM_CODEC`, `ItemStack.OPTIONAL_LIST_STREAM_CODEC`, `ItemStackTemplate.STREAM_CODEC`, `ResourceKey.streamCodec(Registries.X)`. Transfer API: `ItemVariant.PACKET_CODEC`, `FluidVariant.PACKET_CODEC`.

```java
public record SetHomePayload(BlockPos pos, Optional<Component> label, List<UUID> sharedWith) implements CustomPacketPayload {
    public static final Type<SetHomePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("mymod", "set_home"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetHomePayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetHomePayload::pos,
            ComponentSerialization.OPTIONAL_STREAM_CODEC, SetHomePayload::label,
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list(16)), SetHomePayload::sharedWith,
            SetHomePayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
```
Gotchas:
- A `StreamCodec<ByteBuf, X>` can be used where `StreamCodec<? super RegistryFriendlyByteBuf, X>` is expected; use `.cast()` when the generic does not line up (Fabric testmod: `BlockPos.STREAM_CODEC.cast()`).
- Always bound collections/strings coming from clients (`list(max)`, `stringUtf8(max)`) for serverbound packets.
- Hand-written codec: `CustomPacketPayload.codec(MyPayload::write, MyPayload::new)` with `write(RegistryFriendlyByteBuf)` member and a `(buf)` constructor.

## 3. ValueInput / ValueOutput (NBT save/load since 1.21.6)

(src: common net/minecraft/world/level/storage/ValueInput.java, ValueOutput.java, TagValueInput.java, TagValueOutput.java)

`ValueOutput`: `store(name, Codec<T>, T)`, `storeNullable(name, codec, @Nullable T)`, `store(MapCodec<T>, T)` (deprecated, merges fields into current object), `putBoolean/putByte/putShort/putInt/putLong/putFloat/putDouble/putString/putIntArray`, `child(name)` -> nested `ValueOutput`, `childrenList(name)` -> `ValueOutputList.addChild()`, `list(name, codec)` -> `TypedOutputList.add(v)`, `discard(name)`, `isEmpty()`. Fabric adds (fabric-serialization-api-v1, interface-injected): `putLongArray`, `putByteArray`.

`ValueInput`: `read(name, Codec<T>)` -> `Optional<T>`, `read(MapCodec)` (deprecated), `child(name)` Optional / `childOrEmpty(name)`, `childrenList(name)` / `childrenListOrEmpty`, `list(name, codec)` / `listOrEmpty`, `getBooleanOr`, `getByteOr`, `getShortOr` (returns int), `getInt` Optional / `getIntOr`, `getLong` / `getLongOr`, `getFloatOr`, `getDoubleOr`, `getString` / `getStringOr`, `getIntArray`, `lookup()` (deprecated HolderLookup.Provider). Fabric adds: `keySet()`, `contains(key)`, `getOptionalLongArray`, `getOptionalByteArray`.

Codec-based reads/writes are registry-aware automatically (the TagValueInput/Output carry a `HolderLookup.Provider` and use `RegistryOps`), so `output.store("item", ItemStack.CODEC, stack)` just works.

```java
public class ChargerBlockEntity extends BlockEntity {
    private int energy;
    private ItemStack stack = ItemStack.EMPTY;
    private @Nullable UUID owner;
    private final NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);

    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("energy", energy);
        if (!stack.isEmpty()) out.store("stack", ItemStack.CODEC, stack);
        out.storeNullable("owner", UUIDUtil.CODEC, owner);
        ContainerHelper.saveAllItems(out, items);          // writes "Items"
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        energy = in.getIntOr("energy", 0);
        stack = in.read("stack", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        owner = in.read("owner", UUIDUtil.CODEC).orElse(null);
        ContainerHelper.loadAllItems(in, items);
    }
}
```
Entities: `protected abstract void readAdditionalSaveData(ValueInput)` / `addAdditionalSaveData(ValueOutput)` on `Entity`. (src: common net/minecraft/world/entity/Entity.java:2268)

Bridging to a raw `CompoundTag` (e.g. your own file, a packet):
```java
try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(problemPath, LOGGER)) {
    TagValueOutput out = TagValueOutput.createWithContext(reporter, registries); // or createWithoutContext(reporter)
    thing.save(out);
    CompoundTag tag = out.buildResult();
}
ValueInput in = TagValueInput.create(ProblemReporter.DISCARDING, registries, tag);
```
`BlockEntity` convenience: `saveWithFullMetadata(registries)`, `saveWithoutMetadata(registries)`, `saveCustomOnly(registries)` return `CompoundTag`; `BlockEntity.loadStatic(pos, state, tag, registries)`.

Decode failures do not throw: `TagValueInput` reports `DecodeFromFieldFailedProblem` to the `ProblemReporter` and returns `Optional.empty()`.

## 4. CompoundTag today

(src: common net/minecraft/nbt/CompoundTag.java) Typed getters return `Optional`: `getInt(name)` -> `Optional<Integer>`, `getString`, `getCompound`, `getList`, `getBoolean`, `getByteArray`, `getIntArray`, `getLongArray`... plus defaulting forms `getIntOr(name, def)`, `getStringOr`, `getBooleanOr`, `getCompoundOrEmpty(name)`, `getListOrEmpty(name)`. No `getInt(name)` returning 0 and no `contains(name, type)` type-id checks like 1.21.4-. Codec helpers directly on the tag: `tag.store(name, codec, value)`, `tag.storeNullable(...)`, `tag.store(mapCodec, value)`, `tag.read(name, codec)` (uses `NbtOps.INSTANCE`; pass a `RegistryOps` overload `read(name, codec, ops)` for registry-dependent data). Others: `keySet()`, `entrySet()`, `merge(other)`, `copy()`, `remove(name)`, `put(name, Tag)`.

File IO: `NbtIo.readCompressed(Path, NbtAccounter.unlimitedHeap())`, `NbtIo.writeCompressed(tag, Path)`, `NbtIo.read(Path)`, `NbtIo.write(tag, Path)`. SNBT: `TagParser` (e.g. `TagParser.FLATTENED_CODEC`).

Items have no NBT since 1.20.5: use data components. Arbitrary NBT on an item goes in `DataComponents.CUSTOM_DATA` (`CustomData.of(tag)`, `CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> ...)`, `customData.copyTag()`); prefer a custom `DataComponentType` with a Codec (see items doc).

`net.fabricmc.fabric.api.util.NbtType` (fabric-api-base) still exists for numeric tag-type constants.

## 5. fabric-serialization-api-v1 (what it is)

Small module (stable) that interface-injects helpers onto vanilla `ValueInput`/`ValueOutput`, plus `MoreCodecs`:
- `FabricValueInput`: `keySet()`, `contains(String)`, `getOptionalLongArray(String)`, `getOptionalByteArray(String)`.
- `FabricValueOutput`: `putLongArray(String, long[])`, `putByteArray(String, byte[])`.
- `MoreCodecs.LONG_ARRAY`, `MoreCodecs.BYTE_ARRAY` (codecs that produce NBT long/byte arrays).
(src: fabric-api fabric-serialization-api-v1/src/main/java/net/fabricmc/fabric/api/serialization/v1/) It replaced Yarn-era `FabricReadView`/`FabricWriteView`.

## 6. What changed (1.21.x -> 26.x) that trips people up
- `ResourceLocation` -> `Identifier`; `ResourceLocation.fromNamespaceAndPath` -> `Identifier.fromNamespaceAndPath`; `ResourceKey#location()` -> `identifier()`.
- `readNbt/writeNbt(CompoundTag, WrapperLookup)` (Yarn) / `load/saveAdditional(CompoundTag, HolderLookup.Provider)` (Mojang <=1.21.5) -> `loadAdditional(ValueInput)` / `saveAdditional(ValueOutput)`.
- `RegistryCodecs`, `RegistryFixedCodec`, `RegistryFileCodec`, `HolderSetCodec` moved to `net.minecraft.core.registries.codec`.
- 26.3 removed `Block` codecs and the `block_type` registry (Fabric 26.3 blog), so do not look for `Block#codec()`.
- Recipe serializers are `new RecipeSerializer<>(MapCodec, StreamCodec)` since 26.1 (Fabric 26.1 blog).

## Where to look
- `net/minecraft/util/ExtraCodecs.java`, `net/minecraft/network/codec/ByteBufCodecs.java`, `net/minecraft/world/level/storage/TagValueInput.java`, `net/minecraft/nbt/CompoundTag.java`, `net/minecraft/util/ProblemReporter.java`.
- fabric-docs `develop/serialization/codecs.md` (general Codec tutorial; still accurate for DFU).
