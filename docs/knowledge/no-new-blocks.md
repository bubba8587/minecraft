<!-- dte:A2,B15 -->
# Block-like features without new blocks (26.3)

B15 fewNewBlocks says: build from custom items, variants of existing blocks and deeper existing systems; custom textures and models on existing blocks are fine; a new registered block needs a reason given to the owner first. This note is the toolbox for that. It covers MC 26.3 and Fabric API 0.166.2+26.3. It was verified 2026-11-14 against the decompiled 26.3 source (`common`/`client`/`vanilla` below mean `build/mcsrc/26.3/<x>/`), Fabric API's `26.3` branch (`fapi:<module>`), Polymer `dev/26.3` (commit 2843277) and Filament `main` (eca6292, `minecraft_version=26.3`).

Two patterns are proven in `mods/refmod` (spec `specs/reference-projects.md` requirement 17), and their game tests pass:
- **Display-entity pseudo-block**: `pseudoblock/CharmPseudoBlock.java`, `item/LanternCharmItem.java`. Tests: `lanternCharmPlacesDisplayAndHitboxAndPicksUp`, `lanternCharmConsumesItemAndNeedsFreeSpace`, `lanternCharmEntitiesSaveAndLoad`, plus the client screenshot `refmod-lantern-charm`.
- **Per-position data on a vanilla block**: `blockdata/BarrelTallies.java`, `blockdata/BarrelTallyEvents.java`, `mixin/BarrelBlockMixin.java`. Tests: `barrelTallyLivesInChunkAttachmentAndSurvivesChunkSaveLoad`, `barrelTallyClearedWhenBarrelRemoved`.

Related notes (not repeated here): `client-models.md` (blockstate/model/item-definition JSON, Model Loading API, FRAPI), `resourcepacks.md` (pack formats), `systems-persistence.md` (attachment API), `systems-events.md` (event signatures), `content-items-blocks.md` (items, components, block transformers), `systems-resources-config-serverside.md` section 5 (what vanilla clients tolerate), `libraries.md` (Polymer modules), `reference-mods.md` 1.12 (Polymer-based mods), `mixins.md`.

---

## 1. Display and interaction entities as pseudo-blocks

> **Not used for blocks in this repo.** The owner doesn't like display-entity "fake blocks" (B15 fewNewBlocks, corrected 2026-11-14). When something needs to behave like a placed block, use an existing block (sections 2-5) or, with the owner's stated reason, a real block (the undead mod's dormant-undead blocks are the first such case, `specs/undead.md`). This section stays as reference for display entities used as visual effects (floating labels, held-item displays, animations), which is still fine.

### 1.1 The entities
All four are plain vanilla entity types, so they are safe for vanilla clients (`EntityTypes.BLOCK_DISPLAY`, `ITEM_DISPLAY`, `TEXT_DISPLAY`, `INTERACTION`). (src: common `world/entity/Display.java`, `Interaction.java`, `EntityTypes.java:222,547,562,938`)

| | `block_display` | `item_display` | `text_display` | `interaction` |
|---|---|---|---|---|
| Shows | a `BlockState` through its blockstate model (so only states that exist) | an `ItemStack` through its item model definition (any model, per-stack) | a `Component` | nothing; an invisible pickable box |
| Origin | model corner at the entity position (0..1 cube grows +x+y+z) | model centred on the position (and turned 180° on Y by the renderer) | text centred | box bottom-centre at the position |
| Size on the server | 0x0 bounding box | 0x0 | 0x0 | `width` x `height` (default 1x1) |
| Server `tick()` | almost empty: no `baseTick`, no physics | same | same | empty |
| Tracking | range 10 chunks, `updateInterval(1)` | same | same | range 10 chunks, default interval |

Renderer details: client `renderer/entity/DisplayRenderer.java` (`BlockDisplayRenderer` resolves the state with `BlockModelResolver`, `ItemDisplayRenderer` uses `ItemModelResolver.updateForNonLiving` then `rotate(Axis.YP, PI)`).

Shared `Display` fields: NBT key, then setter. Fabric API's `fabric-transitive-access-wideners-v1` makes these setters public. In vanilla they are private. (src: fapi `fabric-transitive-access-wideners-v1.classtweaker:265-311`)
- `transformation` (`setTransformation(Transformation)`): `{translation:[x,y,z], left_rotation:<quat>, scale:[x,y,z], right_rotation:<quat>}` or a 16-float matrix. A quaternion is `[x,y,z,w]` or `{angle, axis:[x,y,z]}`. (src: common `com/mojang/math/Transformation.java:22-33`, `util/ExtraCodecs.java:115-119`)
- `interpolation_duration` and `start_interpolation` (`setTransformationInterpolationDuration`/`Delay`): the client blends the transformation and the other render state (colour, opacity, shadow) from the old values to the new ones over N ticks, starting after the delay. `teleport_duration` (`setPosRotInterpolationDuration`, clamped 0..59) smooths moves.
- `billboard` (`FIXED`, `VERTICAL`, `HORIZONTAL`, `CENTER`). With `FIXED` the entity's yaw and pitch rotate the model, so `snapTo(x, y, z, yRot, 0)` turns a pseudo-block to face the player.
- `brightness` (`setBrightnessOverride(new Brightness(block, sky))`, `-1`/null means use the world light): fakes the lighting of the model only. **It emits no light.** For real light, place a vanilla `minecraft:light` block (invisible, no collision, `level` 0..15, waterloggable; src: common `world/level/block/LightBlock.java`).
- `view_range` (x 64 blocks x the entity-distance setting), `shadow_radius`, `shadow_strength`, `glow_color_override`.
- `width`/`height`: **the culling box. Both default to 0, and then the entity is never frustum-culled** (`Display#updateCulling` sets `noCulling = width == 0 || height == 0`). Always set them on pseudo-blocks.
- `item_display` adds `item` (`setItemStack`) and `item_display` (`setItemTransform(ItemDisplayContext)`, default `none`; `fixed` applies the model's `display.fixed` transform like an item frame). `block_display` adds `block_state`. `text_display` adds `text`, `line_width`, `background`, `text_opacity`, `shadow`, `see_through`, `default_background`, `alignment`.
- `interaction` has `width`, `height`, `response` (`setResponse(true)` makes the client swing the arm on click), and records the last `interaction`/`attack` as `{player: UUID, timestamp}`.

### 1.2 Spawning from Java (what refmod does)
```java
// refmod pseudoblock/CharmPseudoBlock.java (trimmed)
Vec3 base = Vec3.atBottomCenterOf(below.above());
Display.ItemDisplay display = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, level);
ItemStack look = new ItemStack(ModItems.LANTERN_CHARM);
look.set(DataComponents.ITEM_MODEL, RefMod.id("lantern_charm_placed")); // assets/refmod/items/lantern_charm_placed.json; no such item exists
display.setItemStack(look);
display.setItemTransform(ItemDisplayContext.FIXED);
display.setTransformation(new Transformation(new Vector3f(0, 0.5F, 0), null, null, null)); // item models are centred: lift half a block
display.setBrightnessOverride(new Brightness(15, 15));
display.setWidth(0.5F); display.setHeight(0.5F);                 // culling box
display.snapTo(base.x, base.y, base.z, yRot, 0.0F);

Interaction hitbox = new Interaction(EntityTypes.INTERACTION, level);
hitbox.setWidth(0.5F); hitbox.setHeight(0.5F); hitbox.setResponse(true);
hitbox.snapTo(base.x, base.y, base.z, yRot, 0.0F);
hitbox.setAttached(ModAttachments.CHARM_DISPLAY, display.getUUID()); // persistent UUID attachment links the pair
level.addFreshEntity(display);
level.addFreshEntity(hitbox);
```
The look JSON (refmod `src/main/resources/assets/refmod/items/lantern_charm_placed.json` and `models/block/lantern_charm_placed.json`) is an ordinary item model definition pointing at a block-style model with `elements`. `"light_emission": 15` on an element makes that element render full-bright (the integer 0..15 is checked: client `resources/model/cuboid/CuboidModelElement.java:61-69`).
```json
{ "model": { "type": "minecraft:model", "model": "refmod:block/lantern_charm_placed" } }
```
Gotcha: a display entity's bounding box has zero size, so `level.getEntitiesOfClass(Display.ItemDisplay.class, new AABB(pos))` misses one that sits exactly on the box's bottom face. Inflate the box (refmod client test) or search for the `Interaction`, which has a real box.

### 1.3 Clicks: hitbox handling
- **Right-click** arrives as `ServerboundInteractPacket(entityId, hand, location, usingSecondaryAction)`. `ServerGamePacketListenerImpl#handleInteract` checks `hasClientLoaded()` and reach (`isWithinEntityInteractionRange(box, 3.0)`), then calls `player.interactOn(...)`. Fabric's `UseEntityCallback` is injected just before that, so a non-PASS result cancels vanilla. (src: common `server/network/ServerGamePacketListenerImpl.java:1875-1902`; fapi `fabric-events-interaction-v0` `mixin/event/interaction/ServerGamePacketListenerImplMixin.java:83-93`)
- So handle the click **server-side in `UseEntityCallback`**. Return PASS on the client: the client usually can't tell which interaction entities are yours, because a non-synced attachment isn't there, and PASS lets vanilla send the packet. Vanilla clients work the same way.
- **Left-click** is a separate `ServerboundAttackPacket` in 26.x. `Player#attack` fires `AttackEntityCallback` (fapi `PlayerMixin`). Vanilla `Interaction#skipAttackInteraction` records the attack and fires `player_hurt_entity`. Use this for "punch to break". There's no mining progress, so it breaks instantly.
- Testing without a client (refmod `lanternCharmPlacesDisplayAndHitboxAndPicksUp`): mark the mock player loaded with `player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket())`, stand it within reach, then call `player.connection.handleInteract(new ServerboundInteractPacket(hitbox.getId(), MAIN_HAND, Vec3.ZERO, false))`. This runs the real packet path, Fabric hook included. Calling `interactOn` directly would skip `UseEntityCallback`. Polymer's `InteractionHandler.redirect` re-dispatches clicks the same way (polymer `virtualentity/api/elements/VirtualElement.java:61-77`).
- Likewise for items: `GameTestHelper#useBlock` calls `BlockState#useItemOn` directly and **skips `UseBlockCallback`**. To include it, use `player.gameMode.useItemOn(player, level, stack, hand, hit)` (refmod tests). (src: common `gametest/framework/GameTestHelper.java:333-345`; fapi `ServerPlayerGameModeMixin.java:86-94`)

### 1.4 Persistence and linking
- Display and interaction entities save like any entity: in the region's **entity** storage (`entities/*.mca`), with their own NBT (`Display#addAdditionalSaveData`). The look survives restarts with no extra code. refmod `lanternCharmEntitiesSaveAndLoad` round-trips both, including the `item_model` component and the link attachment.
- Link the parts with a persistent entity attachment (UUID), entity tags (`Tags`), or riding (`Passengers`). refmod stores the display's UUID on the hitbox (`ModAttachments.CHARM_DISPLAY`). Its presence also marks the interaction entity as "ours", so other mods' interaction entities are left alone.
- Entity chunks load separately from block chunks (asynchronously). Code that runs on chunk load may not see the entities yet. Look them up lazily, as refmod does when the hitbox is clicked.
- These are entities, so `/kill @e`, entity-clearing mods and `/execute ... run tp` affect them. No block-update path ever touches them: no pistons (`Display` and `Interaction` return `PushReaction.IGNORE_ENTITY`; src: common `world/entity/Display.java:270`, `Interaction.java:93`), no explosions, no fluids.

### 1.5 What a pseudo-block does not do (each fix uses an existing block, so B15 holds)
| Missing | Why | No-new-block fix |
|---|---|---|
| Collision | display has no box; `Interaction` has `noPhysics` and `canBeCollidedWith` is false (Entity default) | place vanilla `minecraft:barrier` in the space (invisible, waterloggable, unbreakable in survival, `PushReaction.IMMOVEABLE`; src: common `world/level/block/Blocks.java:2999`); left-click handling must then remove the barrier too |
| Light | `brightness` only fakes the model's light | vanilla `minecraft:light` block (`level` 0..15). It has no collision and is `replaceable()`, and one space holds one block, so a pseudo-block gets **either** barrier collision **or** a light block in its own space; put the light in a neighbouring air space if it needs both |
| Stops blocks being placed into it | `Entity.blocksBuilding` defaults to false, and `Interaction` doesn't set it (src: common `world/entity/Entity.java:221`) | a barrier in the space; a light block does not help (replaceable) |
| Break particles, mining time, pick-block, redstone, comparator output, block tags, structure-block saving without "include entities" | it isn't a block | handle in your callback (sound, particles with `level.sendParticles`); or accept |
| Part of the chunk mesh (ambient occlusion, cheap rendering) | entities render every frame | none: this is the main cost (1.6) |

Filament (below) uses exactly this split for "decorations": a real but invisible-to-client collision block plus display elements.

### 1.6 Costs and limits
Verified from source:
- Server tick per display is nearly free. `Display#tick` does no `baseTick`, and `Interaction#tick` is empty. The real per-entity costs are the entity section index and the `ChunkMap.TrackedEntity` per-player checks. `ServerEntity#sendChanges` sends only when synced data is dirty (`ServerEntity.java:132`).
- Each display is drawn every frame through the entity renderer (extract and submit), not baked into the chunk mesh. Culling only applies when `width`/`height` are set.
- Every pseudo-block is two or more entities. A big build of them adds up.

Not measured (UNVERIFIED rules of thumb from server-admin practice; measure with `spark` or the F3 profiler before relying on them):
- Hundreds of item displays in view are fine on a normal PC. Many thousands in view cost noticeable FPS.
- Prefer one `item_display` with a combined model over several displays per pseudo-block. Prefer `item_display` over `block_display` when you need a custom model; a block display can only show states that exist.
- Use real blocks (retextured, section 2) for anything placed by the thousand, like terrain or building material. Display entities suit furniture-density objects.

### 1.7 How others do it
- **Polymer virtual entities** (`eu.pb4:polymer-virtual-entity:0.18.7+26.3`; see `libraries.md`): the elements (`ItemDisplayElement`, `BlockDisplayElement`, `TextDisplayElement`, `InteractionElement`, `MarkerElement`) exist **only as packets**. No server entity, no save, no tick, and `/kill` can't remove them. An `ElementHolder` groups them and an attachment decides where and how long they live: `ChunkAttachment.of/ofTicking(holder, level, pos)`, `EntityAttachment`, `BlockBoundAttachment`, `ManualAttachment`. `BlockWithElementHolder` puts a holder on every instance of a (Polymer) block. Your code has to recreate the holders, for example from a chunk attachment on chunk load, because nothing is saved. (src: polymer `docs/polymer-virtual-entity/basics.md`, `polymer-virtual-entity/src/main/java/eu/pb4/polymer/virtualentity/api/**`)
- **Filament** (tomalbrc, LGPL-3.0, Modrinth `filament` 1.8.12+26.3, data-driven server-side items, blocks and "decorations"): a decoration is a **server-registered Polymer block**, sent to clients as `barrier` (collision) or `air`, with an `ElementHolder` of item displays carrying `item_model` and `custom_model_data` (`decoration/block/SimpleDecorationBlock.java`, `DecorationBlock.java:82`). That counts as a new block under B15. Read it for the display and seat code, not the registration.
- Reading order for code: Polymer testmod, then Universal Graves `model/GraveModel.java`, then PolyFactory `models/` (`reference-mods.md` 1.12).

### 1.8 Datapack equivalent
These field names come from the codecs above. The command shapes weren't run in-game (UNVERIFIED; check with `tools/packload.py`):
```mcfunction
summon minecraft:item_display ~ ~ ~ {Tags:["mypack.lamp"],item:{id:"minecraft:paper",count:1,components:{"minecraft:item_model":"mypack:lamp_placed"}},item_display:"fixed",transformation:{translation:[0f,0.5f,0f],left_rotation:[0f,0f,0f,1f],scale:[1f,1f,1f],right_rotation:[0f,0f,0f,1f]},brightness:{block:15,sky:15},width:0.5f,height:0.5f}
summon minecraft:interaction ~ ~ ~ {Tags:["mypack.lamp"],width:0.5f,height:0.5f,response:true}
```
- Detect a right-click with an advancement trigger `minecraft:player_interacted_with_entity` (`player`, `item`, and an `entity` predicate on type and tag) whose reward function revokes itself. Or poll the `interaction` NBT field (`{player, timestamp}`) and remove it after handling. Left-click: `minecraft:player_hurt_entity` and the `attack` field. (src: common `advancements/triggers/CriteriaTriggers.java:15,54`, `PlayerInteractTrigger.java`)
- Placement by survival players: a spawn egg item whose `minecraft:entity_data` is `{id:"minecraft:item_display", ...}` spawns that entity on use. Non-ops can set custom data on every type except `falling_block`, `command_block_minecart` and `spawner_minecart` (src: common `world/item/SpawnEggItem.java:42-66`, `EntityTypes.java:1137`). Add the interaction as a `Passengers` entry, or from a tick function. End to end, this is UNVERIFIED.

---

## 2. New looks on existing blocks and items (resource packs)

### 2.1 How blockstate files combine (verified)
- **Blockstate files stack across packs, per state.** The loader instantiates every pack's `blockstates/<id>.json` from bottom to top and `putAll`s the results, so a higher pack replaces only the states its file matches (src: client `resources/model/BlockStateModelLoader.java:35-95`). To give `note_block[instrument=banjo,note=7,powered=false]` its own model, ship a file with just that variant key. Vanilla's `""` entry keeps covering every other state. Two packs touching different states of the same block both apply. Packs touching the same state: the higher one wins.
- Within one file, `variants` are applied first and `multipart` fills only the states the variants didn't match (`putIfAbsent`). Two `variants` keys that match the same state make that state's entry fail with a logged "Overlapping definition" warning. (src: client `renderer/block/dispatch/BlockStateModelDispatcher.java:40-57,80-97`). Careful: a `multipart` in a higher pack covers **every** state of the block and so replaces the lower packs' look for all of them.
- Hard-wired special renderers (chests, shulker boxes, banners, heads, bells, pots and so on) can't be replaced through blockstates (`resourcepacks.md` 3.3).
- Variants can differ only by **state properties**, which is a fixed set per block. A resource pack has no per-position or per-NBT look for an ordinary placed block.

### 2.2 Per-instance looks for items (verified)
Any stack can look like anything:
- `minecraft:item_model` (`DataComponents.ITEM_MODEL`, an Identifier) picks `assets/<ns>/items/<path>.json`. The definition needs no item with that id (refmod `lantern_charm_placed`). Each stack can carry its own.
- `minecraft:custom_model_data` (`CustomModelData(floats, flags, strings, colors)`) drives `range_dispatch`/`condition`/`select` with `"property": "minecraft:custom_model_data"` and `index`, and tints (`custom_model_data` tint source). One definition covers many looks. Filament sets both components on the displayed stack (`SimpleDecorationBlock.java` `visualItemStack`).
- `select` on `display_context` can show a 3D model when the stack is `fixed` (in an item display or item frame) and a flat icon in the GUI, with no second id. Format details: `client-models.md` 3, `resourcepacks.md` 5.

### 2.3 Per-instance looks for placed blocks (no new block)
| Way | Per-instance? | Client mod needed? | Notes |
|---|---|---|---|
| Blockstate properties of the existing block | only as many looks as the block has states | no (resource pack) | cheap, part of the chunk mesh; see section 3 for borrowing states |
| Display entity on or in the block space | yes, any item model | no (resource pack) | section 1; per-frame entity render |
| `minecraft:player_head` with a `minecraft:profile` component `{texture: "<ns>:<path>"}` | yes, any skin-layout texture from a resource pack (`textures/<path>.png`) | no | the shape is fixed (an 8x8x8 head), and the profile is kept when placed (src: common `world/item/component/ResolvableProfile.java:28-31`, `world/entity/player/PlayerSkin.java:42-45`, `core/ClientAsset.java:19-27`). Polymer's `BlockModelType.HEAD` / `PolymerTexturedHeadBlock` builds on this. In-game render UNVERIFIED |
| Data-driven decor registries: `banner_pattern`, `decorated_pot_pattern` (pot sherd faces), `painting_variant` (`width`/`height` 1..16, `asset_id`) | yes, per placed instance | no (datapack + resource pack) | a painting is an entity, not a block. Shapes: `datapack-registries.md` |
| Fabric Model Loading API `modifyBlockModelAfterBake` wrapping a vanilla block's model, with an `emitQuads` override that reads per-position data | yes | **yes**, clients without it see vanilla | API verified (`client-models.md` 5.1, 5.3). Reading attachment data from the meshing thread safely, and re-meshing the section when it changes (`LevelRenderer` section dirtying), is UNVERIFIED. Treat this as research before relying on it |

---

## 3. Borrowing unused or redundant vanilla blockstates

### 3.1 Candidates
These states all render the same in vanilla (one `""` model) or can be told apart only by behaviour. Checked in vanilla `assets/minecraft/blockstates/*.json` and the state definitions; 556 of 1288 vanilla blockstate files use a single `""` variant.
| Block | Spare states | What breaks if you repurpose them |
|---|---|---|
| `note_block` | 27 instruments x 25 notes x 2 powered = 1350, one model | `NoteBlock#updateShape`/`neighborChanged` recompute `instrument` from the block below and `powered` from redstone, and right-click cycles `note`; you must mixin those away for your states, and every vanilla note block's tuning still uses the same states |
| leaves (`*_leaves`) | `distance` 1..7 x `persistent` x `waterlogged` = 28 each, one model | decay logic reads `distance`/`persistent` |
| `target` | `power` 0..15 | redstone output |
| `tripwire` | 128 (`attached`, `disarmed`, `powered`, 4 directions) | tripwire logic and its own model cases |
| `brown_mushroom_block`, `red_mushroom_block`, `mushroom_stem` | 64 each (six face booleans; multipart) | worldgen uses many combinations, and neighbours update the faces |
| `chorus_plant` | 64 (multipart) | connection logic |
| `infested_*`, waxed vs unwaxed copper | states that look like another block | Polymer remaps them to the look-alike (3.2) |
| beehive/bee nest `honey_level` 1..4, dispenser/dropper `triggered`, creaking heart `natural` | look-identical states | their own logic |

### 3.2 How Polymer's pool works (polymer-blocks 0.18.7+26.3)
- `DefaultModelData.USABLE_STATES` holds lists of spare states per `BlockModelType`: `FULL_BLOCK` (note block and target states except the default, triggered dispensers, beehive honey levels, infested blocks, waxed copper, ...), `LEAVES`, `TRIPWIRE`, `SLAB_*`, `TRAPDOOR_*`, `HEAD`, and more. `SPECIAL_REMAPS` maps each spare state to the vanilla state that looks the same. (src: polymer `polymer-blocks/src/main/java/eu/pb4/polymer/blocks/impl/DefaultModelData.java`, `api/BlockModelType.java`)
- `PolymerBlockResourceUtils.requestBlock(BlockModelType, PolymerBlockModel...)` takes one state out of the pool and registers a remap: whenever a **real vanilla** block in that state is sent to a client, the client receives the default or look-alike state instead (`BlockExtBlockMapper#toClientSideState`). It then writes a blockstate file giving the spare state your model (`api/BlockResourceCreator.java:125-160` and resource generation). So vanilla note blocks still look and behave normally, and only your Polymer block ever shows the spare state on the client.
- The catch for B15: the thing in the world is a **server-registered Polymer block** (`implements PolymerTexturedBlock`). It's a new block in the save file and the server registry, just hidden from clients. It counts as a new block and needs the stated reason.
- The pool is finite and shared by every Polymer mod on the server. `requestBlock` returns null when it's empty. `/polymer blocks_module_state_report` (op level 3) shows what's left.

### 3.3 The pure "reuse" approach (no registration at all)
This is what Paper plugins like Nexo/Oraxen/ItemsAdder do: place actual `note_block[instrument=X,note=Y,powered=Z]` blocks in the world, ship a resource pack that gives those states your model, and mixin `NoteBlock` so the states never change (no tuning, no instrument recompute, no redstone).
- Pros: zero new blocks; vanilla clients see it with the pack; removing the mod leaves ordinary note blocks behind instead of breaking the world.
- Cons: real vanilla note blocks can't use those states any more. Players' tuned note blocks can turn into your block (or the reverse) unless you also remap on the network, which is what Polymer does. Two mods choosing the same states clash silently: the higher resource pack wins per state (2.1), and nothing reserves states across mods. Only full-cube-ish looks with the host block's collision, hardness, sound and tool. Each block id needs one state; behaviour has to be bolted on with mixins and events.
- **When acceptable**: a single-mod modpack or server where you control every mod, the states you take are ones players rarely produce (for example `powered=true` with an uncommon instrument and note), and you document which states are used. Otherwise prefer a retexture of a property the block already has (section 2), a display entity (section 1), or tell the owner a real block is the better choice (section 6).

---

## 4. Data and behaviour on existing blocks

### 4.1 Where to store per-block data
Fabric Data Attachment targets in 26.3: `Entity`, `BlockEntity`, `ServerLevel` (`Level`), `ChunkAccess`, and `GlobalAttachments` (src: fapi `fabric-data-attachment-api-v1` `api/attachment/v1/AttachmentTarget.java` javadoc, mixins `ChunkAccessMixin`, `LevelChunkMixin`, `BlockEntityMixin`, `LevelMixin`). There's no per-`BlockPos` target, so key by position yourself.
| The block... | Store on | Saved with | Cleanup |
|---|---|---|---|
| has a vanilla block entity (barrel, chest, furnace, sign, ...) | **the block entity** (`be.setAttached(...)`) | the block entity (`fabric:attachments` in its NBT) | automatic, since the data goes when the block entity does. Simplest choice. Fabric strips attachments from `getUpdateTag`; use `syncWith` to sync |
| has no block entity (stone, note block, ...) | **the chunk** (`level.getChunkAt(pos)`), value = immutable map keyed by `BlockPos` | the chunk NBT, key `fabric:attachments` (`SerializableChunkDataMixin`) | yours (4.3) |
| needs a world-wide index (ticking, lookup by owner, cross-chunk) | `ServerLevel` attachment or `SavedData` | `data/fabric/attachments.dat` / your `.dat` | yours |

refmod uses the chunk on purpose for a barrel, to prove the chunk path (spec 17b). For a real barrel feature, the block entity attachment is the better choice.

Chunk attachment code (refmod `registry/ModAttachments.java`, `blockdata/BarrelTallies.java`):
```java
public static final AttachmentType<BarrelTallies> BARREL_TALLIES = AttachmentRegistry.create(RefMod.id("barrel_tallies"), b -> b
        .persistent(BarrelTallies.CODEC)                                   // Codec map keys must be strings: save as a list of {pos, count}
        .syncWith(BarrelTallies.STREAM_CODEC, AttachmentSyncPredicate.all())); // to players tracking the chunk

LevelChunk chunk = level.getChunkAt(pos);
chunk.setAttached(BARREL_TALLIES, chunk.getAttachedOrElse(BARREL_TALLIES, BarrelTallies.EMPTY).with(pos, next)); // replace, never mutate
```
- `setAttached` on a chunk calls `markUnsaved()`. It warns if the chunk is still `ChunkStatus.EMPTY` (`ChunkAccessMixin.fabric_markChanged`).
- Chunk sync goes to `PlayerLookup.tracking(level, chunkPos)` and, for initial data, after the chunk packet (`LevelChunkMixin.fabric_syncChange`, `PlayerChunkSenderMixin`). The refmod tests don't check sync on the receiving client (UNVERIFIED there; the code path is read, not run).
- Proven save/load: refmod `barrelTallyLivesInChunkAttachmentAndSurvivesChunkSaveLoad` writes the live chunk with `SerializableChunkData.copyOf(level, chunk).write()` (as `ChunkMap` does), checks `fabric:attachments.refmod:barrel_tallies` in the NBT, then `SerializableChunkData.parse(...).read(level, poi, chunkMap.storageInfo(), pos)` into a fresh chunk and reads the value back. It strips `BlockLight`/`SkyLight` first so reading doesn't queue light updates for the live chunk.
- Ownership: a `BlockPos` key works, but keep each entry in the chunk that contains it. `level.getChunkAt(pos)` does that.

### 4.2 Behaviour on existing blocks
- **Events first** (`systems-events.md` 3): `UseBlockCallback` (runs from `ServerPlayerGameMode#useItemOn` and the client's prediction, before everything else, even when sneaking; refmod's barrel uses it), `BlockEvents.USE_ITEM_ON`/`USE_WITHOUT_ITEM` (inside `BlockState#useItemOn`/`useWithoutItem`; narrower, keeps vanilla's surrounding logic, and also fires for callers such as `GameTestHelper#useBlock`), `AttackBlockCallback`, `PlayerBlockBreakEvents.BEFORE/AFTER/CANCELED`.
- Return non-PASS on **both** sides when the client can decide too (it knows the held item and the block). SUCCESS on the client swings and sends the packet; on the server it stops the barrel menu opening. refmod checks that the menu stays closed.
- **Mixins on vanilla `Block` classes** for anything without an event. Inject into the class that **declares** the method. `BarrelBlock` overrides `affectNeighborsAfterRemoval`, so refmod targets `BarrelBlock`. For a block that only inherits a method, target `BlockBehaviour` and filter with `instanceof`/`state.is(...)`, and keep the check cheap: it runs for every block. Keep mixins small, for compatibility (`mixins.md` 10).

### 4.3 Cleaning up when the block goes
| Hook | Fires for | Misses |
|---|---|---|
| `PlayerBlockBreakEvents.AFTER` | players breaking it | explosions, `/setblock`, `/fill`, endermen, fluids, other mods' `setBlock` |
| Mixin on `<Block>#affectNeighborsAfterRemoval` (refmod `BarrelBlockMixin`) | any change to a different block **with `UPDATE_NEIGHBORS` or a piston move**: players, explosions, `/setblock`, `Level#destroyBlock` (src: common `world/level/chunk/LevelChunk.java:303-319`) | `setBlock` without flag 1. Not called on state-only changes, which is what you want |
| `BlockEntity#preRemoveSideEffects` (block-entity blocks) | block changes without the 256 "no side effects" flag | flag 256 |
| `ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD` | removal **and** chunk unload | can't tell the two apart: don't delete data here |
| **Validate on read** | every read | nothing: always do this as well |

Validate on read means: treat an entry whose block no longer matches as absent (`BarrelTallies.get` checks `state.is(Blocks.BARREL)`), and drop it the next time you write. refmod `barrelTallyClearedWhenBarrelRemoved` breaks the barrel with `level.destroyBlock` (not a player) and checks the entry is gone. That test fails if the mixin is removed.

### 4.4 Ticking without a block entity
- `level.scheduleTick(pos, block, delay)` runs **that block's own** `tick()` (`ServerLevel#tickBlock`: `if (state.is(type)) state.tick(...)`, src: common `server/level/ServerLevel.java:822-827`). On a vanilla block that runs vanilla's code, not yours. To get your logic, mixin the block's `tick` (and keep vanilla's path for vanilla's own scheduled ticks) or use the next option.
- An index plus a level tick: keep the positions that need ticking in a `ServerLevel` attachment, or rebuild them from chunk attachments on `ServerChunkEvents.CHUNK_LOAD` and drop them on `CHUNK_UNLOAD`. Tick them from `ServerTickEvents.END_LEVEL_TICK` (fapi `fabric-lifecycle-events-v1`) and check `level.isLoaded(pos)`. Spread work over ticks for big sets.
- Random ticks of a vanilla block (`randomTick`) need a mixin, and the block must already be random-ticking (`isRandomlyTicking(state)`).

---

## 5. Variants of existing blocks

### 5.1 A vanilla item with components is already a variant (datapack-only, verified from source)
- `minecraft:block_state` (`BlockItemStateProperties`) is applied to the placed state (`BlockItem#updateBlockStateFromTag`). `minecraft:block_entity_data` is loaded into the block entity, and only ops can set it for types where `onlyOpCanSetNbt()` is true (`BlockItem#updateCustomBlockEntityTag`).
- **Any other components on the stack** (custom name, `custom_data`, your own data component types, `item_model`) are kept in the placed block entity's `components` map (saved as `"components"`; `BlockEntity#applyComponents` keeps everything the block entity didn't consume). The vanilla loot function `minecraft:copy_components` with `"source": "block_entity"` puts them back on the drop. Vanilla's barrel loot copies only `custom_name`, so a pack overriding `loot_table/blocks/barrel.json` with `include` set to your components (or no `include`) makes a "fancy barrel" survive breaking. Ctrl-pick also copies them (`ServerGamePacketListenerImpl.java:728`). (src: common `world/item/BlockItem.java:55-160`, `world/level/block/entity/BlockEntity.java:275-320`, `world/level/storage/loot/functions/CopyComponentsFunction.java`, vanilla `data/minecraft/loot_table/blocks/barrel.json`.) UNVERIFIED in game: run it before shipping.
- Its inventory look comes from `item_model` and name. The placed block looks vanilla unless you add a display entity or blockstate trick.

### 5.2 Your own item that places a vanilla block
- **Don't register `new BlockItem(Blocks.X, ...)` as is.** Fabric's `BlockItemTracker` calls `registerBlocks(Item.BY_BLOCK, item)` for every registered `BlockItem`, so `Blocks.X.asItem()` becomes **your** item (pick-block, `asItem()` users). (src: fapi `fabric-registry-sync-v0` `impl/registry/sync/trackers/vanilla/BlockItemTracker.java`.) Either override `registerBlocks(Map<Block, Item>, Item)` to do nothing (it's public and not final in 26.3, `BlockItem.java:190`), or use a plain `Item` whose `useOn` delegates to `((BlockItem) Items.X).place(new BlockPlaceContext(ctx))` and then writes your attachment. The `BlockPlaceContext` and `UseOnContext` constructors are made accessible by `fabric-transitive-access-wideners-v1`.
- After placing, write your data (block entity attachment or chunk attachment) and let the cleanup in 4.3 take it away. Drops are vanilla's unless you change the loot table (5.1) or handle `PlayerBlockBreakEvents.AFTER`.

### 5.3 New interactions through data-driven systems
- **Block tags** decide a lot of behaviour: add existing blocks to vanilla tags such as `climbable`, `mineable/*`, `dragon_immune`, `wither_immune`, `enderman_holdable`, `fall_damage_resetting`, `infiniburn_*`, `dampens_vibrations`, `enchantment_power_provider` (vanilla `data/minecraft/tags/block/`). That's a datapack change, no code.
- **Block transformers** (`data/<ns>/block_transformer/*.json`, `DataComponents.BLOCK_TRANSFORMER` on the item): "use item X on block A to get block B or state B'" with sound, particles, loot, durability. It works with any item and turns existing blocks into existing blocks or other states, which suits chisels, brushes and wrench-style rotation. Details and `BlockTransformerHelper`/`BlockTransformerEvents.MODIFY`: `content-items-blocks.md` 13.1.
- Other existing systems worth extending instead of adding blocks: loot tables (`loot_table/blocks/*`), enchantment effects, `FlammableBlockRegistry`/`OxidizableBlocksRegistry`/`LandPathTypeRegistry` for existing blocks (`content-items-blocks.md` 13), villager trades, `painting_variant`, `banner_pattern`, `decorated_pot_pattern`, `jukebox_song`, `instrument`.

---

## 6. Decision guide

| I want... | Recommended without a new block |
|---|---|
| A new look for a vanilla block everywhere | Resource-pack retexture or remodel (2.1). |
| A few look variants of a vanilla block | Its existing properties, or a variant item that sets `block_state` (5.1). If the block has no spare properties, look at 3.3, then at a display entity. |
| Furniture, decorations, a lamp, a statue | Display entity + interaction (1). Add a vanilla `barrier` for collision or a `light` block for light (one per space, 1.5). Polymer virtual entities if the mod already uses Polymer. |
| A "special" version of a vanilla container or workstation | The vanilla block plus components on the item (5.1), data on its block entity (4.1), and events (4.2). |
| Per-block counters, ownership, upgrades, timers | Attachment: on the block entity if there is one, otherwise on the chunk keyed by position (4.1), with cleanup (4.3) and validate on read. |
| Block-like logic that ticks | An index plus `END_LEVEL_TICK` (4.4), or a display entity with its own logic. |
| New item-on-block behaviour | `block_transformer` JSON or `UseBlockCallback` (5.3, 4.2). |
| New wall art | `painting_variant` (datapack + resource pack). |
| A per-instance custom look on a block | Display entity, or a player head with a `profile.texture` (2.3). |
| Something players place by the thousand (building blocks, terrain) | A retexture of existing states. If it must be a genuinely new material, a real block is the honest answer (below). |

### When a real block is the better choice (say this to the owner, per B15)
- **It has to be part of the terrain**: placed in large numbers, generated in worldgen, mined with normal mining time and tool tiers, burned by fire, pushed by pistons, part of the chunk mesh for performance. Display entities don't scale to that, and borrowed states (section 3) break vanilla and clash between mods.
- **It needs real block physics**: collision with a custom shape (a barrier is only a full cube), being climbable by shape, fluids interacting with its shape, redstone power and comparator output tied to its own state, light emission that varies with its own state.
- **It needs to be reliable against everyday commands and other mods**: entities die to `/kill @e[type=...]` and lag-clear mods; blocks don't.
- **Borrowing states would hurt players**: the only free states belong to a block players use for its real job (note blocks in music builds, leaves in tree farms).
- **The block needs a block entity with real logic and inventory semantics** (hoppers, comparators, menus) that a vanilla block entity can't host through attachments and events.
The reason to give in that case: what the feature needs (from the list above), which no-new-block approach was tried or considered, and why it falls short. A removed mod with a new block leaves holes or unknown blocks in worlds, and each registered block has to be ported every release (B10).

---

## 7. Status
- Verified by refmod game tests (server, `./gradlew :refmod:build`): spawning and linking an item display and interaction, the `item_model` component on the displayed stack, culling size, brightness, the right-click packet path with `UseEntityCallback`, entity save/load, chunk-attachment write and read through `SerializableChunkData`, `UseBlockCallback` blocking the barrel menu, mixin cleanup on a non-player break. Client game test screenshot `refmod-lantern-charm` shows the charm model sitting on a stone block (checked by eye 2026-11-14).
- Verified from source only (not run): sections 2.1, 3.2, 4.3 hook coverage, 4.4, 5.1, 5.2, the Polymer and Filament descriptions.
- UNVERIFIED: performance numbers (1.6), datapack command and spawn-egg flow (1.8), player-head `profile.texture` rendering, the client FRAPI per-position wrapper (2.3), and chunk-attachment sync arriving on the client.

## Where to look
- Display/interaction: common `world/entity/Display.java`, `Interaction.java`; client `renderer/entity/DisplayRenderer.java`; fapi `fabric-transitive-access-wideners-v1.classtweaker:265-311`.
- Interaction plumbing: common `server/network/ServerGamePacketListenerImpl.java` (`handleInteract`, `handleAttack`); fapi `fabric-events-interaction-v0` mixins.
- Attachments: fapi `fabric-data-attachment-api-v1` (`ChunkAccessMixin`, `LevelChunkMixin`, `SerializableChunkDataMixin`, `ImposterProtoChunkMixin`, testmod `gametest/BlockEntityTests.java`).
- Blockstate stacking: client `resources/model/BlockStateModelLoader.java`, `renderer/block/dispatch/BlockStateModelDispatcher.java`.
- Polymer: github.com/Patbox/polymer `dev/26.3`, `polymer-blocks` and `polymer-virtual-entity` (+ `docs/`). Filament: github.com/tomalbrc/filament.
- refmod: `mods/refmod/src/main/java/io/github/bubba8587/refmod/{pseudoblock,blockdata}/`, `mixin/BarrelBlockMixin.java`, tests in `src/gametest/.../RefModGameTest.java` (section "17a/17b").
