<!-- dte:C1,B15 -->
<!-- covers: docs/research/undead-*, mods/bubbas_reworked_mobs/* -->
# Bubba's Reworked Mobs, part 1: undead (spec)

Builds part 1 of the C1 reworkedMobs direction (mod id `bubbas_reworked_mobs`; later parts get their own specs). Background research: `docs/research/undead-prior-art.md` (nothing on any loader does "dormant until disturbed"; closest mechanic is vanilla 26.3's copper golem statue) and `docs/research/undead-feasibility.md` (26.3 classes, code sketches, performance budget). Everything here is fluid: change it when a better or faster way turns up.

## Dormant undead are real blocks

Dormant undead are real blocks, not entities or display-entity pseudo-blocks. They can be mined into items (see Mining and placing sleepers), cost nothing to tick in large numbers, and never count against mob caps. Waking one replaces the block with a real mob. (Moved here from the tree on the owner's word: it is a technical choice. B15 fewNewBlocks is satisfied because the owner gave the reason for these blocks.)

- Modeled on vanilla's copper golem statue: block + block entity, **no ticker**. Blockstate: `facing`, `pose`, `waterlogged`. Block entity holds the gear as the vanilla `minecraft:container` component, a lazy gear loot table (rolled on first interaction or wake), a wake cooldown and who woke it.
- Rendering (hybrid): the body is a baked block model per pose (part of the chunk mesh, free per frame; 26.3 block models allow free per-element rotation). A block entity renderer draws only armor and held items, only when the sleeper has gear and is within 32 blocks. Pose models and renderer poses are generated from one Java pose table by datagen so they never drift apart. Armor in the renderer needs one client accessor mixin for the equipment asset manager.
- Lying sleepers are one block with an overhanging model (collision stays one block).
- First playable version (built): the block entity renderer draws the whole sleeper with the vanilla mob model and texture as placeholder art, one baked model per pose, and reuses vanilla's own `HumanoidArmorLayer` and `ItemInHandLayer` for gear (they only need a `RenderLayerParent`, so armor, trims, dye and glint come free). Poses are part rotations applied after the vanilla model's `setupAnim` (`SleeperPoses`, the pose table), so armor models pose identically. The block's own model is particle-only. The hybrid baked-body version and our own 16x/32x textures come before release; item icons are solid-colour placeholders until then.
- Code layout: one block class, one block entity type and one renderer serve every variant; a variant is one `UndeadVariant` entry (block + item `<name>_sleeper`, entity `woken_<name>`) plus one entry in the renderer's client table.

## Hearing and waking

- The block entity implements `GameEventListener.Provider` (registered automatically for loaded full chunks, like sculk sensors), so hearing costs nothing between events. Vanilla vibration rules apply: sneaking silences steps, wool blocks, carpet muffles.
- A heard event rolls a wake chance (by event type and distance), then schedules a block tick as the travel delay; the tick removes the block and spawns the real mob with the same gear, facing the source.
- A wake emits a custom `bubbas_reworked_mobs:awaken` game event; other sleepers hear it through the same path (wool included). Chains are throttled by a per-sleeper cooldown and capped per event (prior art capped cascades at 4).
- If another mod cancels the spawn, the block stays (nothing is lost).
- Details as built: the listener copies vanilla's `VibrationSystem` checks (spectators, sneaking on `#ignore_vibrations_sneaking`, `#dampens_vibrations`, the 6-ray wool occlusion) and hears `#bubbas_reworked_mobs:sleepers_hear` (= `#minecraft:vibrations` + awaken). Order: tag, cooldown, source filter, chance, raycast. Loudness is a Java table (`SleeperHearing`; explosion 1.0, block break/place 0.3, chest 0.2, step 0.04 ... with linear falloff to 25 % at 16 blocks). After any roll a sleeper ignores sounds for 1 s. A sleeper ignores noise from its own block (its own placement).
- Noise from `#bubbas_reworked_mobs:does_not_wake_sleepers` (default `#minecraft:undead`) never wakes sleepers, so a woken horde shuffling past doesn't cascade by footsteps; only the awaken event chains. Animals never wake them either (owner's call): any entity whose mob category isn't monster or misc (passive animals, bats, fish, axolotls) is ignored. Players, other monsters, villagers, golems and projectiles still do. Woken undead never despawn (owner confirmed).
- The awaken cap is per dispatch: game events are delivered synchronously, nearest listener first (`BY_DISTANCE`), so a budget set around the dispatch gives the nearest 4 the chance.
- Each sleeper has a `sensitivity` (block entity NBT, default 1) that multiplies every wake chance: 0 never wakes (decor, warded), large always wakes (traps). It is the hook for the planned anti-waking magic, and tests use it instead of seeding randomness.
- The woken mob is persistent (never despawns, outside the mob cap), keeps the sleeper's custom name, always drops its gear, targets the waker if it is a survival player, and an undead that wakes empty-handed arms itself from a loot display within 3 blocks (see below), or, zombies only, grabs a sword/axe/spear lying loose within 3 blocks.

## Loot on display (shared block)

Weapons near sleepers, and later other loot, are shown with a dedicated block rather than as dropped items or item frames (owner's choice: it looks better than grabbing swords off item frames, and later loot systems reuse it). It is a deliberate new block under B15 fewNewBlocks; the owner gave the reason.

- One block holding exactly one real item (any weapon or tool; armor later if useful) in a block entity. Lives in Bubba's Reworked Mobs' shared code (not under undead); may move to a loot mod later.
- Blockstate `placement`: `ground` (lying flat), `buried` (mostly underground, grip showing), `wall` (hung or leaning against the wall it's attached to), `stuck` (planted point-down). Plus `facing`, `waterlogged`. More placements can be added.
- Rendering: the item's own model, posed per placement, so any item (vanilla or modded, any resource pack) looks right with no extra art.
- Taking: use with an empty hand takes the item and removes the block. `buried` needs a shovel and makes more noise (sleepers hear it). Breaking the block drops the item.
- Worldgen and structures place it next to sleepers, half-buried in graves, hung on crypt walls.
- Player placement (decor): backlogged, needs care (BACKLOG.md).

As built (`display/` package; renderer in `client/display/`):

- Block `bubbas_reworked_mobs:loot_display` (`LootDisplayBlock`) + block entity (`LootDisplayBlockEntity`), no ticker, no block item, no loot table. The item is saved as a one-slot `Items` list (like a container) and carried as the vanilla `minecraft:container` component, so it survives save/load, structure templates and `/setblock ... {Items:[{Slot:0b,id:"minecraft:iron_sword",count:1}]}`. Middle-click (creative) gives the displayed item.
- `facing` is the side the item shows its front to: for `wall`, away from the wall it hangs on; for the floor placements, toward whoever placed it (a `ground` item lies with its grip toward `facing`, point away; a `buried` grip leans toward `facing`).
- Shapes (outline only; there is **no collision**, you walk over and through displays): `ground` a 14x1.5 px slab, `buried` a 6x5 px stub, `stuck` a 6x13 px post, `wall` a 12x12x1.5 px plate on the wall. Floor placements need a centre-sturdy floor and `wall` a sturdy wall behind (like torches); when that goes the block pops and drops the item. Pistons pop it too.
- Rendering (`LootDisplayRenderer`): the item resolved through `ItemModelResolver` in the `FIXED` display context (what item frames use) at 0.75 scale, so a sword is about 0.93 blocks long; then per placement: `ground` flat on the floor with a small per-position turn (up to 15 degrees) so rows don't look stamped; `buried` point-down, sunk so only the top 0.4 block shows, leaning 25 degrees; `stuck` upright, point 0.1 block into the ground; `wall` flat against the wall like a frameless item frame. The block model is invisible (particle only: iron block). Cost: one item per block, no stack copies; the render state is created per frame as in vanilla's shelf and item frame. Poses assume a corner-to-corner tool/weapon sprite (all vanilla swords, axes, tools, spears, bows, the trident's sprite); other items still render, just posed the same way.
- Taking (`LootDisplays`): an empty main hand takes `ground`, `wall` and `stuck` (pickup sound, the quiet `minecraft:item_interact_finish` game event, which vanilla ignores while sneaking). Holding anything else does nothing to the display. `buried` ignores an empty hand (an action-bar hint says to dig) and needs a shovel (`#minecraft:shovels`): digging costs the shovel 1 durability and plays the ground's break sound plus `minecraft:block_destroy`, which sleepers hear three times as well as the quiet event (0.3 vs 0.1 base). Breaking drops the item: other placements break at once like a flower; `buried` mines like packed dirt (hardness 0.8, fast with a shovel; the block is in `#minecraft:mineable/shovel`). A waterlogged display leaves its water behind.
- **Grabbing what's lying around (owner: "all weapon-carrying mobs should grab whatever's lying around").** Any mob in the entity tag `#bubbas_reworked_mobs:grabs_loot` (our woken undead now; other weapon-carrying mobs as later parts rework them) takes **any** item, not only weapons, so loot rooms and dungeons can put specific items in their hands: on waking (immediately, nearest within 3 blocks) and afterwards as an ongoing behavior (an empty-handed mob notices a display block or loose item within a short range, walks to it and takes it). Displays come before loose items; any placement (undead don't need shovels); the display block goes. Runs after the mob is added, so a refused spawn takes nothing. Shared code, not under undead.
- Worldgen defaults: skeleton sleepers get standard bows; zombie sleepers get tools/weapons at vanilla's normal zombie equipment rates. Loot rooms and dungeons will place specific items on display instead.
- Placing for building and tests: `/bubbas_reworked_mobs display <ground|buried|wall|stuck> <item> [<pos> [north|south|east|west]]` (ops, permission level 2). Without a position it goes where you look (within 20 blocks): `wall` on the face you look at, the others on the floor you look at, turned toward you. It refuses a spot that's taken or has nothing to hold the item up. How survival players place displays (if at all) is still the owner's call; nothing in survival places them yet.

## Robbing

Using a sleeper with an empty hand takes one item: held weapon first (main hand, then off hand), then armor from the top down. Weapon theft rolls a small wake chance (10 %); armor theft always rolls a larger one (30 %); both times the sleeper's sensitivity. A failed roll wakes it 2 ticks later, angry at the thief. Opening a nearby chest makes the normal container-open noise, which sleepers hear.

## Mining and placing sleepers

- Mining a sleeper with a **Silk Touch pickaxe** gives the whole mob as an item (its "statue": a sleeper item of that undead type, no gear on it, pose kept); its armor and weapons drop as separate items (owner's call). The tool is the item tag `#bubbas_reworked_mobs:sleeper_harvest_tools` (pickaxes) plus Silk Touch. Anything else (no Silk Touch, another tool, bare hands) leaves remains (a bone or rotten flesh) and the gear. The gear drops from the block entity whenever the block goes (mining, explosions, commands), except on waking, when the mob takes it.
- Sleeper items can be placed back as sleepers, and placed sleepers can come alive like generated ones (decor, traps).
- Planned later: magic that prevents waking (on the player, applied to sleepers, etc.).

## Scope and variants

- Only this mod's undead change: our own entity types (woken sleepers and the rare natural spawns), tagged as undead. Vanilla zombies and skeletons elsewhere (other mods' spawners and structures) stay vanilla, and AI or animation mods that target vanilla classes don't override ours. Mob spawners will be tweaked as well (design to come).
- Every humanoid undead variant sleeps, for now: skeleton, stray, bogged, parched, wither skeleton; zombie, husk, drowned, zombie villager. Some variants may get unique behavior later. Not yet: zombified piglin, skeleton/zombie horses, camel husk, zombie nautilus.
- One sleeper block per variant (or a variant property), each with its own 16x and 32x textures.

## Natural spawning

One small mixin cuts the global monster cap to a game-rule percentage (`bubbas_reworked_mobs:monster_cap_percent`, default 15) with an off switch for spawn-control mods (`bubbas_reworked_mobs:cut_monster_spawns`). It sits at HEAD of `NaturalSpawner$SpawnState#canSpawn`, the per-mob test natural spawning runs after it has picked a mob and a spot, so it sees the type and the position: biome spawns get the cut cap, while spots whose monster list comes from a structure (witch huts, monuments, outposts, fortresses) keep vanilla's cap. This replaced the earlier two-mixin plan (cap scaling + a biome-only gate) because one hook does both, and it only ever says no, so it composes with other spawn mods. The per-player cap is left at vanilla's; in multiplayer the cut global cap is what binds. Other categories, light rules, spawners, trial spawners and raids are untouched.

## Tuning

- Speed, health and damage are our entity types' default attributes (`FabricDefaultAttributeRegistry.register`; values in `UndeadTuning`): skeleton 60 health, speed 0.15; zombie 80 health, speed 0.13; fists 1 damage (vanilla: 20 health, 0.25/0.23, 2-3 damage). Zombies get no reinforcements and don't turn into vanilla drowned.
- Fist hits deal low damage but extra armor durability damage, applied in Fabric's after-damage event.
- The early game is meant to be quite a bit tougher: high health and armor-shredding hits, balanced by undead being slow and easy to escape. (A separate future mod will drastically reduce unenchanted mining speed.)
- Bows: zombies take extra damage from headshots; skeletons take much less damage from arrows.
- Smite: chance per level to kill instantly with particles and a sound, using a damage type that bypasses armor and hit cooldown; added through Fabric's enchantment modify event (composes with other packs) rather than overriding `smite.json`.
- Staggering: a client mixin at the end of the shared humanoid model setup adds the wobble (armor follows). No GeckoLib.
- Zombies: a low-priority goal checks for nearby zombies about every 10 seconds and drifts toward them.

## Worldgen

Sleepers are plain block placement. Shallow graves and sitting skeletons come from a feature added to biomes; crypts and dungeon rooms are jigsaw structures whose templates hold sleeper blocks with their gear. Templates are generated by code (reproducible) rather than hand-built.

## Libraries

Only Fabric API (+ MixinExtras bundled with Loader). GeckoLib and Polymer were considered and not needed. spark for profiling during development.

## Performance budget

Sleeper on the server: 0 per tick. Hearing: under 0.05 ms per tick with 200 sleepers near a walking player. 100 armed sleepers in view: under 1 ms per frame. Measure with spark before release.

## Open questions for the owner

- None open. (Name: Bubba's Reworked Mobs, mod id `bubbas_reworked_mobs`.)
