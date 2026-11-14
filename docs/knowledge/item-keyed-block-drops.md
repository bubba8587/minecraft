<!-- dte:A2,B15 -->
# Item-keyed block drops without a new block (26.3)

Scope: making a **placed vanilla block** use its own drops depending on the **item it was placed from** (a name such as "Synthetic Diamond Ore", `custom_data`, other components), while the same block placed or generated normally keeps vanilla drops. No new block is registered (B15 fewNewBlocks). Minecraft 26.3, Fabric API 0.166.2+26.3, data pack format 121. Verified 2026-11-14 against the decompiled source (`common`/`vanilla` = `build/mcsrc/26.3/<x>/`), Fabric API's `26.3` branch (`fapi:<module>`) and the game tests named below. Related: `no-new-blocks.md` (B15 toolbox), `datapacks.md` (loot JSON shape in 26.3), `mixins.md`, `systems-networking.md`.

## Summary and decision table

| Block has a block entity? | Mod allowed? | Approach | Status |
|---|---|---|---|
| yes (barrel, chest, furnace, sign, pot, ...; list below) | either | **A**: datapack only. The item's components are kept on the placed block entity; the block's loot table branches with `match_block` + `components`/`predicates` | works, tested |
| no (ores, stone, logs, most blocks) | no | **B**: nothing reliable exists in data alone | does not work, tested |
| no | yes | **C**: a tiny mod adds a boolean property (e.g. `synthetic`) to that vanilla block; then data does the rest (item `block_state` component, `match_block` on `state`) | works, tested |
| no, and the marker must be arbitrary data, or the block's states must not change | yes | **D**: per-position chunk attachment + code at break time | proven storage only (refmod 17b); fallback |

**Recommendation for Synthetic Diamond Ore:** approach C. Deepslate diamond ore has no block entity and no properties, so the item's name has nowhere to go once placed (B, proven). Adding `synthetic` to the ore's state is one small mixin; the recipe, the loot tables and the "drop nothing" rule stay in the datapack. The mark is part of the block state, so it survives pistons, `/clone`, structures, silk touch and chunk saves with no extra code, and old worlds load unchanged.

**General rule:** block entity → A (key on `item_name` or `custom_data`, not `custom_name`). No block entity → C if a mod is fine. D only when C can't express it.

---

**Reopen if** (owner, 2026-11-14): Minecraft adds more data-driven blocks, or lets blocks without a block entity carry data from their item. Then every use of approach C (an added property on a vanilla block) should move to the pure-data version, merging back toward vanilla, and the tiny mod parts and their mod-required-on-both-sides cost go away. Current uses: Synthetic Diamond Ore (`specs/crafting-tweaks-mod.md`). Check on every Minecraft release (CLAUDE.md, New Minecraft release).

## A. Blocks with a block entity: pure datapack (works)

### Verified claims
- `BlockItem#place` calls `blockEntity.applyComponentsFromItemStack(stack)` (src: common `world/item/BlockItem.java:111`). `BlockEntity#applyComponents` lets the block entity consume what it understands (`applyImplicitComponents`; `custom_name` for containers) and keeps **every other component of the stack's patch** (`item_name`, `custom_data`, `lore`, mod components, ...) in its `components` map, minus `block_entity_data` and `block_state` (src: common `world/level/block/entity/BlockEntity.java:279-296`). It is saved as `"components"` (same file `:100-102`, `:140`).
- `BlockEntity#collectComponents()` returns the kept map plus the implicit ones the block entity re-exports (`custom_name` for containers) (src: `BlockEntity.java:305-310`).
- The loot condition **`minecraft:match_block`** takes a full `BlockPredicate`: `blocks`, `state`, `nbt`, `components` (exact) and `predicates` (partial). With `components`/`predicates`/`nbt` it reads the **`BLOCK_ENTITY` loot parameter**, not the world (src: common `world/level/storage/loot/predicates/MatchBlock.java`, `advancements/predicates/BlockPredicate.java:85-97`, `DataComponentMatchers.java`). `block_state_property` no longer exists (`LootItemConditionTypes.java`).
- `location_check` with a `block` predicate reads the **world** at the origin. On a player break the block is already gone: `ServerPlayerGameMode#destroyBlock` captures the block entity, calls `level.removeBlock`, and only then `playerDestroy` → loot (src: common `server/level/ServerPlayerGameMode.java:286-326`). So `location_check` sees air there. Explosions run the loot with the block still present (`BlockBehaviour.java:167-185`), so `location_check` would behave differently by cause. **Use `match_block`.**
- The `BLOCK_ENTITY` parameter is supplied on player breaks (`Block#getDrops`, `Block.java:357-375`), explosions (`BlockBehaviour#onExplosionHit`) and `/loot ... mine`.
- Tested (packcheck, `mods/packcheck/src/gametest/.../ItemKeyedDropsGameTest.java`, all pass; refpack file `datapacks/refpack/data/minecraft/loot_table/blocks/barrel.json`):
  - `namedBarrelUsesItsOwnDrop`: a barrel placed by a survival player from an item named "Marked Barrel" has the name in `collectComponents()`, and mining it drops one diamond.
  - `customDataBarrelUsesItsOwnDropAndLocationCheckSeesAir`: an item with `custom_data {refpack_marker:1b}` keeps it in the block entity's `components()`; mining drops an emerald, **not** the gold ingot of the `location_check` branch listed first.
  - `unmarkedBarrelsKeepVanillaDrops`: a plain barrel drops a plain barrel; a barrel named "Some Other Name" drops a barrel with that name (vanilla `copy_components` kept).
  - `tools/packload.py refpack` loads the override clean on the vanilla server.

### JSON (refpack's barrel, the pattern to copy)
One pool, one `alternatives` entry: the first child whose condition passes wins; the last child is the vanilla entry unchanged.
```json
{
  "type": "minecraft:block",
  "pools": [ {
    "rolls": 1,
    "condition": { "type": "minecraft:survives_explosion" },
    "entries": [ { "type": "minecraft:alternatives", "children": [
      { "type": "minecraft:item", "name": "minecraft:diamond",
        "condition": { "type": "minecraft:match_block", "blocks": "minecraft:barrel",
                       "components": { "minecraft:custom_name": "Marked Barrel" } } },
      { "type": "minecraft:item", "name": "minecraft:emerald",
        "condition": { "type": "minecraft:match_block", "blocks": "minecraft:barrel",
                       "predicates": { "minecraft:custom_data": "{refpack_marker:1b}" } } },
      { "type": "minecraft:item", "name": "minecraft:barrel",
        "modifier": { "type": "minecraft:copy_components", "source": "block_entity", "include": [ "minecraft:custom_name" ] } }
    ] } ]
  } ],
  "random_sequence": "minecraft:blocks/barrel"
}
```
For a recipe-made item with a name, match its `item_name`: `"components": { "minecraft:item_name": "Synthetic Thing" }`. To branch to a whole table, use a child `{ "type": "minecraft:loot_table", "value": "<ns>:blocks/<name>", "condition": {...} }`; it counts as matched even if that table drops nothing.

### Gotchas
- **`custom_name` is forgeable**: any survival player can rename a plain item in an anvil. Key on `item_name` (only recipes, loot or commands set it) or `custom_data` for anything that matters.
- `components` is an **exact** match on the value: a name with colour or italics is a different text component. `predicates.custom_data` is a partial NBT match (extra keys are fine).
- Containers still spill their contents through their own code; the loot table only decides the block's own drop.
- Keep the vanilla entry last and unchanged, and re-check it on each Minecraft update (B10): an override replaces vanilla's file completely.
- Overriding a vanilla loot table clashes with any other pack that overrides the same file (the higher pack wins). Fabric mods can instead add to it with `LootTableEvents.MODIFY` (`fapi:fabric-loot-api-v3`).

### Vanilla blocks with a block entity (26.3)
From `BlockEntityTypes` (src: common `world/level/block/entity/BlockEntityTypes.java`): furnace, blast furnace, smoker, chest (and copper chests), trapped chest, ender chest, barrel, shulker boxes, hopper, dispenser, dropper, crafter, brewing stand, beacon, conduit, jukebox, lectern, chiseled bookshelf, shelf, enchanting table, all signs and hanging signs, banners, skulls/heads, bell, campfire/soul campfire, beehive/bee nest, decorated pot, suspicious sand/gravel (brushable), sculk sensor, calibrated sculk sensor, sculk catalyst, sculk shrieker, daylight detector, comparator, creaking heart, copper golem statue, potent sulfur, vault, trial spawner, spawner, end portal, end gateway, piston head (moving), command/structure/jigsaw/test blocks. Every other block (ores, stone, logs, planks, glass, wool, and in 26.3 also beds) has none: use C.

---

## B. Blocks without a block entity, data only (does not work)

- An ore keeps nothing of the item but its block state. The only placement component that reaches a plain block is `minecraft:block_state`, and `BlockItemStateProperties#apply` silently **skips property names the block doesn't have** (src: common `world/item/component/BlockItemStateProperties.java:48-63`). Deepslate diamond ore has no properties, so there is nothing to set.
- No loot condition can see where the item came from: `match_block` sees the state (and block entity, none here); `location_check` sees air (A above).
- Tested (packcheck `ItemKeyedDropsGameTest.syntheticOreFromBlastingPlacesPlainOre`): bubbas_crafting_tweaks' `blast2diamond` result ("Synthetic Diamond Ore", an `item_name`) placed by a survival player and mined with an iron pickaxe drops one diamond, like natural ore. The same with `block_state {synthetic:"true"}` added (no mod loaded) also drops a diamond.
- The only data-only workaround would be a hack: a `placed_block` advancement trigger on the named item, a function that finds the placed position and summons a marker entity there, and a tick function that deletes diamonds dropped next to a marker whose block is gone. It misses pistons, explosions and many edge cases, costs a tick function, and was not tried. Not recommended (UNVERIFIED).

---

## C. Tiny mod: add a property to the vanilla block (works)

### How it works
1. A mixin adds `BooleanProperty synthetic` to the chosen vanilla block **while it is constructed**, and makes `synthetic=false` its default state.
2. Data does the rest: the recipe result carries `"minecraft:block_state": {"synthetic": "true"}`; placing it sets the property; the block's loot table branches with `match_block` + `state`.
3. Optional code: anything that isn't loot data (here, XP).

### Verified claims
- The `Block` constructor builds its state definition by calling `createBlockStateDefinition(builder)`, then `registerDefaultState(stateDefinition.any())` (src: common `world/level/block/Block.java:232-237`). `Blocks.register` calls `properties.setId(key)` **before** the factory runs, so the block's id is known inside the constructor from its `Properties` (private field `id`, read with an `@Accessor`) (src: `Blocks.java:5655-5658`, `state/BlockBehaviour.java:1004,1305`).
- **Default trap:** `any()` is the first state, and `BooleanProperty` lists `true` first (src: `state/properties/BooleanProperty.java:7`, `StateDefinition.java` `any()`). Without a fix, every generated or `/setblock` ore would be synthetic. Many blocks also call `registerDefaultState(any().setValue(...))` in their own constructor, so the fix goes on `registerDefaultState` itself (refmod `@ModifyVariable` at HEAD), not only after `Block.<init>`.
- `StateDefinition.Builder#add` throws on a duplicate property name and on names not matching `[a-z0-9_]+` (src: `StateDefinition.java:184-215`). Two mods adding the same name to the same block crash at startup; pick a name unlikely to clash.
- Placement: `BlockItem#updateBlockStateFromTag` applies the `block_state` component (src: `BlockItem.java:84,121`).
- Loot: `match_block` with `"state": {"synthetic": "true"}`. Without the mod the property is missing and the matcher returns false (src: `advancements/predicates/StatePropertiesPredicate.java:119-120`); its `checkState` is never called in 26.3, so the table still loads: **the datapack degrades to vanilla drops without the mod.**
- Saves (26.3 palette format): `BlockState.CODEC` writes the default state as a bare block id and other states as `{id, properties}` (src: `state/BlockState.java:11-16`). Natural ore is the default, so it is written exactly as vanilla writes it. Reading: a missing property gets the default value (`StateDefinition#appendPropertyCodec`, `:133-137`); for a block with a single state (the mod removed) the dispatch codec is `MapCodec.unit` and ignores `properties` (`StateHolder.java:148-160`). `NbtUtils#readBlockState` likewise skips unknown properties (`nbt/NbtUtils.java:124-140`). So **old worlds load as natural ore, and a world saved with synthetic ore loads as natural ore after the mod is removed** (players then get real diamonds from it; tell them).
- Client models: vanilla `blockstates/deepslate_diamond_ore.json` has one `""` variant, which matches every state, so both states render the vanilla model with no blockstate override and no log warnings (screenshot below). A resource pack could give `synthetic=true` its own look with a variant key, since blockstate files stack per state (`no-new-blocks.md` 2.1).
- Pistons: ores are pushable (`PushReaction.PUSH_PULL` default, `BlockBehaviour.java:1017`); a piston moves the whole state, so the mark moves with it. `/clone`, structure blocks and silk touch (with the table below) keep it too.
- XP is code, not loot: `DropExperienceBlock#spawnAfterBreak` calls `tryDropExperience` (src: `DropExperienceBlock.java:19-24`). refmod wraps that call with `@WrapWithCondition` to skip synthetic ore.
- Pick-block: `BlockBehaviour#getCloneItemStack` returns `new ItemStack(asItem())` (`BlockBehaviour.java:407`), so picking synthetic ore gives **plain** ore. Decided: acceptable (it only matters in creative, where any item is free anyway); a mixin there could add the components if wanted.

### Network and registry implications
- Adding a state shifts the numeric id of **every block state registered after that block**. Fabric's `StateIdTracker` rebuilds state ids on each side from that side's own blocks (src: `fapi:fabric-registry-sync-v0` `impl/registry/sync/trackers/StateIdTracker.java`), and `RegistrySyncManager#configureClient` sends nothing, and lets vanilla clients in, when no synced registry has modded entries (`RegistrySyncManager.java:68-90`, "Don't send when there is nothing to map"). A property-only mod has no modded entries, so **a client without the mod would join and see scrambled chunks**. The mod must be required on both sides and must turn such clients away itself. refmod does it in `network/ExtraStatesHandshake.java`: the client registers a configuration-channel receiver, and the server disconnects clients that can't receive it (`ServerConfigurationConnectionEvents.CONFIGURE` + `ServerConfigurationNetworking.canSend`). The client game test proves a modded client still connects; the rejection path itself was not run (UNVERIFIED in game; same pattern as Fabric's registry sync).
- Vanilla clients cannot join a server running this mod (with the handshake they get a clear message). Declare the mod `"environment": "*"` and list it as required on both sides on Modrinth.
- Performance: one extra state for one block, created once at startup; the mixins run once per block at startup and once per ore break. No per-tick cost. Nothing measurable expected (not profiled).

### Tests (refmod `mods/refmod/src/gametest/.../SyntheticOreGameTest.java`, spec `reference-projects.md` requirement 18, all pass)
- `syntheticPropertyIsAddedAndNaturalIsTheDefault`: the property exists, default `false`, two states, plain diamond ore untouched, `asItem()` unchanged.
- `naturalOreKeepsVanillaDropsAndXp`: natural ore placed from the plain item and mined by a survival player: one diamond and XP orbs; fortune III gives 1..4 diamonds; silk touch gives the plain ore item and no XP.
- `syntheticOreUsesItsOwnTableAndGivesNoXp`: the recipe result placed by a survival player is `synthetic=true`; an iron pickaxe and fortune III give nothing and no XP; silk touch gives back an item identical to the recipe result (same components, so they stack).
- `pickBlockOnSyntheticGivesPlainOre`: documents the pick-block behaviour above.
- `savesWithAndWithoutThePropertyLoad`: natural ore encodes as the bare id `"minecraft:deepslate_diamond_ore"`; synthetic as `{id, properties:{synthetic:"true"}}`; a bare id and an `{id}` without properties decode as natural; `{id:"minecraft:stone", properties:{synthetic:"true"}}` decodes to plain stone through both `BlockState.CODEC` and `NbtUtils` (the "mod removed" case: a single-state block); a whole chunk written with `SerializableChunkData` reads back both ores correctly, and again as natural after the `properties` entry is stripped from the palette.
- Client: `RefModClientGameTest` places natural and `[synthetic=true]` ore side by side with `/setblock` (which proves the command syntax and that the client knows the state) and takes `refmod-synthetic-ore`. Checked by eye 2026-11-14: both blocks show the same vanilla deepslate diamond ore texture, no missing-model checkerboard.

### Java (refmod; copy into a real mod and rename)
```java
// blockstate/SyntheticOre.java — loaded during Blocks' static init: no registry access here.
public static final BooleanProperty SYNTHETIC = BooleanProperty.create("synthetic");
public static final Set<String> BLOCK_IDS = Set.of("minecraft:deepslate_diamond_ore");
public static boolean isSynthetic(BlockState state) { return state.getValueOrElse(SYNTHETIC, false); }

// mixin/BlockPropertiesAccessor.java
@Mixin(BlockBehaviour.Properties.class)
public interface BlockPropertiesAccessor {
	@Accessor("id") @Nullable ResourceKey<Block> refmod$id();
}

// mixin/BlockExtraPropertyMixin.java
@Mixin(Block.class)
public abstract class BlockExtraPropertyMixin {
	@WrapOperation(method = "<init>", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/Block;createBlockStateDefinition(Lnet/minecraft/world/level/block/state/StateDefinition$Builder;)V"))
	private void refmod$addSyntheticProperty(Block block, StateDefinition.Builder<Block, BlockState> builder, Operation<Void> original,
			@Local(argsOnly = true) BlockBehaviour.Properties properties) {
		original.call(block, builder);
		ResourceKey<Block> id = ((BlockPropertiesAccessor) properties).refmod$id();
		if (id != null && SyntheticOre.BLOCK_IDS.contains(id.identifier().toString())) builder.add(SyntheticOre.SYNTHETIC);
	}

	@ModifyVariable(method = "registerDefaultState", at = @At("HEAD"), argsOnly = true)
	private BlockState refmod$naturalByDefault(BlockState state) {
		return state.hasProperty(SyntheticOre.SYNTHETIC) ? state.setValue(SyntheticOre.SYNTHETIC, false) : state;
	}
}

// mixin/DropExperienceBlockMixin.java (optional: no XP from synthetic ore)
@Mixin(DropExperienceBlock.class)
public abstract class DropExperienceBlockMixin {
	@WrapWithCondition(method = "spawnAfterBreak", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/DropExperienceBlock;tryDropExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/util/valueproviders/IntProvider;)V"))
	private boolean refmod$noXpFromSynthetic(DropExperienceBlock block, ServerLevel level, BlockPos pos, ItemStack tool, IntProvider xpRange,
			@Local(argsOnly = true) BlockState state) {
		return !SyntheticOre.isSynthetic(state);
	}
}

// network/ExtraStatesHandshake.java (common init) + one line in the client initializer
public record ExtraStatesHandshake() implements CustomPacketPayload {
	public static final Type<ExtraStatesHandshake> TYPE = new Type<>(RefMod.id("extra_block_states"));
	public static final StreamCodec<FriendlyByteBuf, ExtraStatesHandshake> CODEC = StreamCodec.unit(new ExtraStatesHandshake());
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
	public static void initialize() {
		PayloadTypeRegistry.clientboundConfiguration().register(TYPE, CODEC);
		ServerConfigurationConnectionEvents.CONFIGURE.register((listener, server) -> {
			if (!ServerConfigurationNetworking.canSend(listener, TYPE)) {
				listener.disconnect(Component.literal("This server adds block states: install <mod> on your client."));
			}
		});
	}
}
// client: ClientConfigurationNetworking.registerGlobalReceiver(ExtraStatesHandshake.TYPE, (payload, context) -> { });
```
List the mixins in the mod's mixins JSON (`"mixins": [..., "BlockExtraPropertyMixin", "BlockPropertiesAccessor", "DropExperienceBlockMixin"]`); MixinExtras ships with Fabric Loader. To reuse for more blocks, add ids to `BLOCK_IDS`; each doubles that block's state count, so prefer blocks with few states.

### JSON (refmod resources; the same files work from a datapack)
`data/minecraft/loot_table/blocks/deepslate_diamond_ore.json` (synthetic first, then vanilla's two children unchanged):
```json
{
  "type": "minecraft:block",
  "pools": [ { "rolls": 1, "entries": [ { "type": "minecraft:alternatives", "children": [
    { "type": "minecraft:loot_table", "value": "refmod:blocks/synthetic_diamond_ore",
      "condition": { "type": "minecraft:match_block", "blocks": "minecraft:deepslate_diamond_ore", "state": { "synthetic": "true" } } },
    { "type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": "minecraft:deepslate_diamond_ore" },
    { "type": "minecraft:item", "name": "minecraft:diamond", "modifier": [
      { "type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops" },
      { "type": "minecraft:explosion_decay" } ] }
  ] } ] } ],
  "random_sequence": "minecraft:blocks/deepslate_diamond_ore"
}
```
`data/refmod/loot_table/blocks/synthetic_diamond_ore.json` (silk touch returns the synthetic item, otherwise nothing; delete the pool to always drop nothing):
```json
{
  "type": "minecraft:block",
  "pools": [ { "rolls": 1, "condition": "minecraft:tool/can_silk_touch", "entries": [ {
    "type": "minecraft:item", "name": "minecraft:deepslate_diamond_ore",
    "modifier": { "type": "minecraft:set_components", "components": {
      "minecraft:item_name": "Synthetic Diamond Ore", "minecraft:block_state": { "synthetic": "true" } } } } ] } ],
  "random_sequence": "refmod:blocks/synthetic_diamond_ore"
}
```
`data/refmod/recipe/synthetic_diamond_ore.json`:
```json
{ "type": "minecraft:blasting", "category": "blocks", "ingredient": "minecraft:coal_block",
  "result": { "id": "minecraft:deepslate_diamond_ore", "components": {
    "minecraft:item_name": "Synthetic Diamond Ore", "minecraft:block_state": { "synthetic": "true" } } },
  "experience": 5, "cookingtime": 4000 }
```
Silk touch matters: with vanilla's table, silk-touching synthetic ore would give the **plain** ore item, which then places natural ore and mines into diamonds. The synthetic table must give back the synthetic item (or nothing).

### Gotchas
- Without the mod, everything still loads: the `block_state` component is skipped and `match_block` on the missing property is false, so synthetic ore acts exactly like today (real diamonds). The pack can ship before the mod.
- F3 shows `synthetic: true` on the looked-at block, and `/setblock ...[synthetic=true]` makes synthetic ore without the item.
- Code that compares against `defaultBlockState()` with `==` (rare) treats synthetic ore as a different state; `state.is(Blocks.X)`, tags and `/fill ... replace deepslate_diamond_ore` treat both as the same block.
- Other mods hard-coding raw state ids (rare) would break; Fabric remaps by registry order, not by fixed numbers.

---

## D. Position memory (fallback)

Store "synthetic" per position in a chunk attachment when the item is placed (`no-new-blocks.md` 4.1, proven by refmod `barrelTallyLivesInChunkAttachmentAndSurvivesChunkSaveLoad` and `barrelTallyClearedWhenBarrelRemoved`), then change drops in code (`LootTableEvents.MODIFY_DROPS` checking the attachment at the loot origin, or a mixin) and clean up on removal (4.3).
- Works for any block and any data (a whole component map, an owner, a timestamp), with no state change and no client requirement: vanilla clients can join if nothing else needs the mod there.
- Costs: more code (placement hook, drop hook, cleanup, validate-on-read). The mark is **lost when the block moves**: pistons (ores are pushable), `/clone`, structure blocks, WorldEdit-style tools, falling blocks. Explosions and non-player removals need the `affectNeighborsAfterRemoval` mixin pattern, and removals without neighbour updates leave stale entries (validate on read). Silk touch must be handled in code to give the marked item back.
- Use it when C can't express the data or the block's states must stay untouched. For Synthetic Diamond Ore, C is simpler and more robust.

---

## Exact files for bubbas_crafting_tweaks

Applied 2026-11-14, with one change: the loot override and synthetic table live in the companion mod's own data (`mods/bubbas_crafting_tweaks/src/main/resources/data/`), not the datapack, and the synthetic table drops one diamond 25 % of the time (as built: `specs/crafting-tweaks-mod.md`). The plan as first written:


With approach C the datapack needs only these (the property itself comes from the mod; without it the pack behaves as today):
1. `data/bubbas_crafting_tweaks/recipe/blast2diamond.json`: add `"minecraft:block_state": { "synthetic": "true" }` to `result.components` (keep the `item_name`).
2. `data/minecraft/loot_table/blocks/deepslate_diamond_ore.json`: the C loot table above with `"value": "bubbas_crafting_tweaks:blocks/synthetic_diamond_ore"`.
3. `data/bubbas_crafting_tweaks/loot_table/blocks/synthetic_diamond_ore.json`: the C synthetic table above with `"random_sequence": "bubbas_crafting_tweaks:blocks/synthetic_diamond_ore"` (or an empty `"pools": []` for "always nothing").
4. A mod (new, or inside an existing one of the owner's) carrying the three mixins and the handshake above, required on server and client.
5. Tests: packcheck's `syntheticDiamondOre` row then expects the `block_state` component too; `ItemKeyedDropsGameTest.syntheticOreFromBlastingPlacesPlainOre` keeps passing without the mod (it proves the fallback); the mod's own game tests take over refmod's `SyntheticOreGameTest`. Update the player docs page for the pack (B11 playerVault).

## Where to look
- Loot: common `world/level/storage/loot/predicates/{MatchBlock,LocationCheck,LootItemConditionTypes}.java`, `advancements/predicates/{BlockPredicate,DataComponentMatchers,StatePropertiesPredicate,LocationPredicate}.java`, `world/level/storage/loot/entries/NestedLootTable.java`.
- Placement and block entity components: `world/item/BlockItem.java`, `world/item/component/BlockItemStateProperties.java`, `world/level/block/entity/BlockEntity.java`.
- Breaking: `server/level/ServerPlayerGameMode.java#destroyBlock`, `world/level/block/Block.java#getDrops/dropResources`, `DropExperienceBlock.java`.
- States: `world/level/block/Block.java` (constructor), `state/StateDefinition.java`, `state/StateHolder.java#codec`, `state/BlockState.java`, `nbt/NbtUtils.java#readBlockState`.
- Sync: `fapi:fabric-registry-sync-v0` `StateIdTracker`, `RegistrySyncManager#configureClient`; `fapi:fabric-networking-api-v1` `ServerConfigurationConnectionEvents`, `ServerConfigurationNetworking`.
- Proof: `datapacks/refpack/data/minecraft/loot_table/blocks/barrel.json`, `mods/packcheck/src/gametest/java/io/github/bubba8587/packcheck/test/ItemKeyedDropsGameTest.java`, `mods/refmod/src/main/java/io/github/bubba8587/refmod/{blockstate,mixin,network}/`, `mods/refmod/src/main/resources/data/`, `mods/refmod/src/gametest/java/io/github/bubba8587/refmod/test/SyntheticOreGameTest.java`.
