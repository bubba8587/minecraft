<!-- dte:C1,B15 -->
<!-- covers: docs/research/undead-* -->
# Bubba's Reworked Mobs, part 1: undead (spec)

Builds part 1 of the C1 reworkedMobs direction (mod id `bubbas_reworked_mobs`; later parts get their own specs). Background research: `docs/research/undead-prior-art.md` (nothing on any loader does "dormant until disturbed"; closest mechanic is vanilla 26.3's copper golem statue) and `docs/research/undead-feasibility.md` (26.3 classes, code sketches, performance budget). Everything here is fluid: change it when a better or faster way turns up.

## Dormant undead are real blocks

Dormant undead are real blocks, not entities or display-entity pseudo-blocks. They can be mined into items (see Mining and placing sleepers), cost nothing to tick in large numbers, and never count against mob caps. Waking one replaces the block with a real mob. (Moved here from the tree on the owner's word: it is a technical choice. B15 fewNewBlocks is satisfied because the owner gave the reason for these blocks.)

- Modeled on vanilla's copper golem statue: block + block entity, **no ticker**. Blockstate: `facing`, `pose`, `waterlogged`. Block entity holds the gear as the vanilla `minecraft:container` component, a lazy gear loot table (rolled on first interaction or wake), a wake cooldown and who woke it.
- Rendering (hybrid): the body is a baked block model per pose (part of the chunk mesh, free per frame; 26.3 block models allow free per-element rotation). A block entity renderer draws only armor and held items, only when the sleeper has gear and is within 32 blocks. Pose models and renderer poses are generated from one Java pose table by datagen so they never drift apart. Armor in the renderer needs one client accessor mixin for the equipment asset manager.
- Lying sleepers are one block with an overhanging model (collision stays one block).
- First playable version: the block entity renderer draws the whole sleeper with the vanilla mob model and texture as placeholder art. The hybrid baked-body version and our own 16x/32x textures come before release.

## Hearing and waking

- The block entity implements `GameEventListener.Provider` (registered automatically for loaded full chunks, like sculk sensors), so hearing costs nothing between events. Vanilla vibration rules apply: sneaking silences steps, wool blocks, carpet muffles.
- A heard event rolls a wake chance (by event type and distance), then schedules a block tick as the travel delay; the tick removes the block and spawns the real mob with the same gear, facing the source.
- A wake emits a custom `undead:awaken` game event; other sleepers hear it through the same path (wool included). Chains are throttled by a per-sleeper cooldown and capped per event (prior art capped cascades at 4).
- If another mod cancels the spawn, the block stays (nothing is lost).

## Robbing

Using a sleeper with an empty hand takes one item: held weapon first, then armor from the top down. Weapon theft rolls a small wake chance; armor theft always rolls a larger one. Opening a nearby chest makes the normal container-open noise, which sleepers hear.

## Mining and placing sleepers

- Mining a sleeper with any tool gives the whole mob as an item (a "sleeper" item of that undead type, no gear on it); its armor and weapons drop as separate items.
- Sleeper items can be placed back as sleepers, and placed sleepers can come alive like generated ones (decor, traps).
- Planned later: magic that prevents waking (on the player, applied to sleepers, etc.).

## Scope and variants

- Only this mod's undead change: our own entity types (woken sleepers and the rare natural spawns), tagged as undead. Vanilla zombies and skeletons elsewhere (other mods' spawners and structures) stay vanilla, and AI or animation mods that target vanilla classes don't override ours. Mob spawners will be tweaked as well (design to come).
- Every humanoid undead variant sleeps, for now: skeleton, stray, bogged, parched, wither skeleton; zombie, husk, drowned, zombie villager. Some variants may get unique behavior later. Not yet: zombified piglin, skeleton/zombie horses, camel husk, zombie nautilus.
- One sleeper block per variant (or a variant property), each with its own 16x and 32x textures.

## Natural spawning

A small mixin scales the global and per-player monster caps by a game rule (default 15 % of vanilla). Other categories are untouched; light rules, spawners, trial spawners and structure spawns (witch huts, fortresses, monuments, outposts) stay at full strength (second mixin gates biome spawn entries only). Config to turn the cut off for compatibility with spawn-control mods.

## Tuning

- Speed, health and damage through Fabric's default-attribute modify event.
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
