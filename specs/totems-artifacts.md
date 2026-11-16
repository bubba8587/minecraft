<!-- dte:C6,B15,B17,B18,B19 -->
<!-- covers: mods/bubbas_totems_artifacts/* -->
# Bubba's Totems & Artifacts (spec)

A separate mod for **artifacts**: found items that do things potions and enchantments don't. Mod id `bubbas_totems_artifacts`, code in `mods/bubbas_totems_artifacts/`, Java package `io.github.bubba8587.bubbas_totems_artifacts`. Built (0.1.0, unreleased). Owner's call (2026-11-16): "this could be its own mod, Bubba's Totems & Artifacts. Grave Talisman and all totems go in here."

## What lives here

| Artifact | Status |
|---|---|
| Grave Talisman | built (moved from Reworked Mobs) |
| Totem of Warding | built (moved from Reworked Mobs), plus its totem statue |
| Totem of Keeping, Gathering, Fortification, Growth, Seeking | designed 2026-11-16 (below), being built |

What stays in Reworked Mobs (`specs/undead.md`): sleepers, the hearing/disturbance meter, light calming sleepers, bells being loud to sleepers, loot displays, combat, the monster cap, and the `api.Sleepers` hook below.

## Design rules for totems (owner)

- Many totems work both **held** (personal, small) and **in a beacon** (area).
- Any rhythm (e.g. a held pulse) shows with vanilla's item cooldown sweep (B19 noHiddenCooldowns); vanilla mechanics first (B17); mobs and players treated the same (B18).
- Fortification must stop explosion block damage only, never via the mobGriefing rule (that would also stop villager farming).

## The five new totems (design agreed with the owner, 2026-11-16)

Owner's ideas (BACKLOG) plus the orchestrator's detail calls, accepted by the owner ("yes, start the totems builder"). Each works held (personal, small) and, where it makes sense, in a Reworked Beacons beacon (area) through the payload API, like Warding.

- **Keeping:** held only; used up on death, and you keep your whole inventory that once (a Totem of Undying for your items). Nothing in a beacon.
- **Gathering:** held, pulls dropped items and XP within ~8 blocks to you; in a beacon, pulls them into the nearest chest or barrel under the beacon.
- **Fortification:** held, explosions near you don't break blocks; in a beacon, no explosion block damage in its area. Only explosion block damage, never through the mobGriefing rule (owner).
- **Growth** (working name; owner wanted a verb-like word, still open): faster crop and sapling growth (random tick boost) within ~8 blocks held, across the area in a beacon.
- **Seeking:** held, points like a compass to the nearest structure of one chosen type (like an explorer map); the type is chosen by using it on a structure's signature block (builder to propose the block list; owner may prefer another way); in a beacon, it points every compass held in its area.
- Found, not crafted, like the other artifacts (loot placement to be proposed). B19: any rhythm shows with the item cooldown sweep.

## As built (0.1.0)

### What moved from Reworked Mobs

Behavior unchanged unless noted below.

| Piece | Was (`bubbas_reworked_mobs`) | Now (`bubbas_totems_artifacts`) |
|---|---|---|
| Grave Talisman | `:grave_talisman`, `undead/GraveTalisman`, `client/undead/GraveTalismanGlow`, property `bubbas_reworked_mobs:grave_talisman_glow` | `:grave_talisman`, `talisman/GraveTalisman`, `client/talisman/GraveTalismanGlow`, property `bubbas_totems_artifacts:grave_talisman_glow` |
| Totem of Warding | `:totem_of_warding`, `warding/*` (`TotemOfWardingItem`, `Wards`, `WardBarrier`, `WardFleeGoal`, `WardingLoot`, `WardingTuning`) | same names under `warding/` |
| Ward mixins | `MobTickMixin`, `PhantomSpawnerMixin`, `Walk/SwimNodeEvaluatorMixin`, `MobAccessor`, ward half of `SpawnStateMixin` | same names (prefix `bta$`), plus `WardSpawnStateMixin` (the ward check only); Reworked Mobs' `SpawnStateMixin` keeps only the monster-cap cut |
| Beacon payload | `compat/WardingBeaconPayload` | same class here (entrypoint `bubbas_reworked_beacons:payload`); lang keys `beacon.bubbas_totems_artifacts.ward*` |
| Assets | Reworked Mobs assets and 32x pack | this mod's assets and its own 32x pack `bubbas_totems_artifacts_32x` |
| Texture scripts | `tools/warding-textures.py`, Reworked Mobs `tools-local/make_talisman_textures.py` | same script (writes here) / moved to `mods/bubbas_totems_artifacts/tools-local/` |
| Creative tab | Reworked Mobs tab | its own tab "Bubba's Totems & Artifacts" |

Reworked Mobs no longer depends on Reworked Beacons at all (its `compileOnly` and `suggests` are gone; it now `suggests` Totems & Artifacts).

### Independence (same pattern as Reworked Beacons)

`fabric.mod.json` `depends` on Fabric API only and `suggests` `bubbas_reworked_mobs` and `bubbas_reworked_beacons`; the build has `compileOnly` against both. Neither mod requires the other. Owner's principle (2026-11-16): "mods should work standalone, but that doesn't mean fallbacks everywhere": each mod runs alone, but removing a mod doesn't need a stand-in for its items.

**The sleeper hook (Reworked Mobs `io.github.bubba8587.bubbas_reworked_mobs.api.Sleepers`; stable: add, don't change):**

- `int calm(ServerLevel, Vec3 centre, double radius, float points)`: takes points off sleepers' disturbance meters in range (wake is 100). The held totem's pulse.
- `void keepAsleep(ServerLevel, Object source, AABB area, int lifetimeTicks)` / `void release(ServerLevel, Object source)`: sleepers in the area never wake from noise; each source's area lapses unless refreshed. A full-pyramid ward. Backed by Reworked Mobs' `undead/sleeper/SleepAreas`.
- `boolean isKeptAsleep(Level, BlockPos)`, `boolean isSleeper(Level, BlockPos)`, `double nearestSqr(Level, Vec3, double range)` (client or server; what the talisman senses).

**The endermen hook (Reworked Mobs `api.Endermen`; stable: add, don't change):** `keepOut(ServerLevel, Object source, AABB area, int lifetimeTicks)` / `release(ServerLevel, Object source)` / `isKeptOut(Level, BlockPos)`. Every ward also keeps Reworked Mobs' endermen (their jobs and cairn spawning) out, through `SleeperHook.keepEndermenOut` / `releaseEndermen`.

This mod reaches it only through `compat/SleeperHook`, picked once by `FabricLoader.isModLoaded("bubbas_reworked_mobs")`; only `compat/ReworkedMobsSleepers` names Reworked Mobs' classes, and it is created only when that mod is loaded. Without Reworked Mobs the talisman senses `#minecraft:undead` mobs only, and the totem still scares monsters, stops natural spawns in a ward, keeps monsters and happy ghasts out and pushes flyers out; it just has no sleepers to calm or keep asleep. On a beacon screen the "sleeping undead stay asleep" line shows only when Reworked Mobs is installed *(agent's choice: without it the line describes nothing)*.

### Saved worlds (old item ids)

`ModItems` registers a Fabric registry alias per item (`BuiltInRegistries.ITEM.addAlias(old, new)`): a lookup of `bubbas_reworked_mobs:grave_talisman` / `:totem_of_warding` that isn't registered falls back to the new item, so old stacks (chests, inventories, item frames, commands, the test world) load as the new items and are saved under the new id. Chosen because vanilla has no item-id alias and a DataFixer is heavier and only runs on version bumps. The alias lives here, so it works with or without Reworked Mobs. Without this mod installed the old stacks have no item and Minecraft drops them as unknown (vanilla behavior for a removed item); there is deliberately no fallback in Reworked Mobs (owner, 2026-11-16: fine as is). Players updating Reworked Mobs should install this mod alongside.

### Grave Talisman

Owner's idea, deliberately loose: "a talisman that faintly glows near undead." Item `bubbas_totems_artifacts:grave_talisman`; working name, the owner may rename it.

- **Getting one (owner, 2026-11-14): found only, no recipe.** Planned as grave and crypt loot once Reworked Mobs' worldgen builds them, possibly a rare find on sleepers. Until then creative tab only.
- **What it does:** glows when undead are near, brighter the closer; nothing else. Undead = Reworked Mobs' sleeper blocks (through the hook) and any entity in `#minecraft:undead`.
- **Levels (`talisman/GraveTalisman`):** nearest undead within 40 blocks of the holder's eyes: faint; within 24: bright; within 12: brightest; none: off. (Was 16/8/4; owner: too short. At sprint speed, ~5.6 blocks/s, faint gives about 7 s of warning.)
- **Where it shows:** in either hand and as the icon in hotbar and inventory, for the local player's own talismans only. Held by others, dropped, in item frames: dark.
- **How (client only, no packets):** `client/talisman/GraveTalismanGlow` runs every 5 ticks while the player carries a talisman: sleepers via the hook (loaded chunks only) plus one entity box query for `#minecraft:undead`, keeping the nearest. Nothing runs without a talisman. The search is on plain `Level`, so a server game test checks it.
- **Visual:** `items/grave_talisman.json` is a `range_dispatch` on the numeric item model property `bubbas_totems_artifacts:grave_talisman_glow` (registered into `RangeSelectItemModelProperties.ID_MAPPER` via Fabric's transitive access wideners). Levels 1-3 are a `composite` of the flat item and a gem cuboid (`models/item/grave_talisman_glow_<n>.json`, `light_emission` 5 / 10 / 15), because a flat `item/generated` layer can't carry `light_emission`. All share `item/grave_talisman_held`, which raises the first-person pose so the gem is on screen. No particle or sound.
- **Art:** placeholder textures from `tools-local/make_talisman_textures.py` (16x and 32x); to be replaced by hand-drawn art.
- **Prior art:** nothing on Modrinth senses undead with an item. Closest: Slime Amulet (MIT, glows in slime chunks), Hostile Awareness, Player Radar. The idea is Sting from The Hobbit.

### Totem of Warding

Owner's goal (2026-11-15): mob-free zones without spamming torches. Owner decisions: `docs/research/area-gems-and-warding.md` ("Owner decisions", 2026-11-16); the beacon side: `specs/reworked-beacons.md`. Numbers in `warding/WardingTuning` (starting values, owner to tune); *(agent's choice)* marks the builder's picks. Follows B17 (vanilla flee behavior, the cooldown sweep, the spawner light rule), B18 (mobs holding a totem pulse too) and B19 (the pulse rhythm shows as the cooldown sweep).

**The item** (`bubbas_totems_artifacts:totem_of_warding`, `warding/TotemOfWardingItem`): an artifact like the Totem of Undying (uncommon, stacks to 1), found only, never a gem. Placeholder art from `tools/warding-textures.py` (16x and 32x).

- **Found (owner: a mix of places, more common than the Totem of Undying).** One extra pool per table via `LootTableEvents.MODIFY` (`WardingLoot`; Fabric-replaced tables skipped): evoker 5 % (owner, 2026-11-16: a rare bonus), woodland mansion chest 20 %, ancient city chest 12 %, trial chambers vault 8 %, ominous vault 15 % *(chest rates: agent's choice)*. Graveyard and crypt chests join `WardingTuning.LOOT` once worldgen builds them.
- **Held (owner):** in either hand (`Item.inventoryTick`, MAINHAND or OFFHAND) it pulses every 10 s (`PULSE_INTERVAL_TICKS` 200), the first as soon as it is held off cooldown. Players get vanilla's cooldown sweep (`ItemCooldowns.addCooldown`); other holders use a weak map of next pulse times. Visuals: soul-fire ring at shin height, rising souls, low amethyst chime *(agent's choice)*. Held, it doesn't stop spawns (owner).
- **Pulse:** every `Enemy` within 8 blocks (`PULSE_RADIUS`) flees the holder for 100 ticks (`FLEE_TICKS`), keeping away while within 12 (`FLEE_KEEP_AWAY`), speeds 1.0 / 1.2 within 7, like a creeper from a cat *(keep-away: agent's choice)*. Goal-driven `PathfinderMob`s get `WardFleeGoal` (priority 0, MOVE, added lazily; picks copy `AvoidEntityGoal`, which isn't reused because its targeting skips creative players and non-player holders). Brain mobs with `AVOID_TARGET` (piglins, hoglins) get it for 100 ticks. Not affected (limitation): slimes, ghasts, phantoms, vexes, the warden, breezes. Sleepers within 8 blocks lose 40 points (`PULSE_CALM_POINTS`, via `Sleepers.calm`) *(agent's choice)*.
- **Mob holders:** a mob holding a totem pulses on the same rhythm (B18) *(agent's choice)*; a woken undead can pick one up.

**Wards (a totem in a Reworked Beacons beacon; owner).** `compat/WardingBeaconPayload` is the only class touching the beacons API (`-PwithBeacons` puts the beacons jar on the game test classpath). It accepts the totem, power 1, required layers 1; `apply` = `Wards.put(level, pos, area, levels >= 4, 100)`; screen lines "No monsters spawn or enter" and "Sleeping undead stay asleep" (4 layers) or, greyed, "4 layers: sleeping undead stay asleep".

- Any pyramid: **no natural monster spawns** inside (`WardSpawnStateMixin`; `PhantomSpawnerMixin` wraps `isValidEmptySpawnBlock`, so phantoms count *(agent's choice)*). Spawners, trial spawners, raids, bosses and commands spawn as normal (owner).
- Any pyramid: **monsters don't walk in.** `WalkNodeEvaluatorMixin` / `SwimNodeEvaluatorMixin` (HEAD of `getPathTypeOfMob`) return BLOCKED for a kept-out mob's node inside a ward it isn't in. Every 20 ticks (`WARD_SWEEP_TICKS`) goal-driven monsters inside walk out through the nearest side; no despawn or damage *(agent's choice)*. Ranged monsters outside can still shoot in (limitation).
- Any pyramid: **an invisible wall for every kept-out mob** (owner, 2026-11-16: every monster, including non-pathing ones, and happy ghasts wild or ridden). "Kept out" = `Enemy` or a happy ghast (`WardBarrier.keptOut`). `WardBarrier` runs at the TAIL of each such mob's `LivingEntity.tick` (`MobTickMixin`, server) within 3 blocks of a ward; one map check per mob tick when no ward exists. No damage; mobs slide along the edge.
  - At the edge moving in: inward motion dropped. Crossed in since last tick: put back on the edge (the barrier keeps its own "last seen outside" per mob, weak map; the mob's old-position fields weren't reliable).
  - Already inside: 0.06 blocks/tick outward through the nearest side (top or bottom for no-gravity mobs); walkers also walk out.
  - Steered vehicle (a ridden happy ghast): moved out by min(depth + 0.6, 1) blocks a tick with `ClientboundMoveVehiclePacket` to the rider; small rubber-band on lag (limitation). Passengers are skipped.
- 4 layers: **sleepers inside stay asleep** (`Sleepers.keepAsleep` for the ward's area).
- `Wards.put(level, key, AABB, keepsSleepersAsleep, lifetimeTicks)` lapses unless refreshed: refreshed every 80-tick beacon pulse for 100 ticks (`WARD_DEFAULT_LIFETIME_TICKS`), so a ward ends about 5 s after its beacon stops. One map lookup when no ward exists.

### Totem statue (block, B15 exception)

- `bubbas_totems_artifacts:totem_statue` ("Totem Statue"). **B15 reason (owner-approved, 2026-11-16):** one shared block for every totem, so a found totem can be set down as decoration.
- **Placing (owner-approved, 2026-11-16):** sneak and use a Totem of Warding on a block (sneaking, so holding a totem for its pulse never places it by accident; without sneaking the click does nothing). The statue faces the player and the totem is used up (creative keeps it). The Grave Talisman is not a totem and has no statue.
- **Inert:** no pulse, ward or beacon effect; no block entity, no ticking.
- **Breaking** (any tool, pickaxe fastest, strength 1) drops the totem back; pistons pop it off as its totem. Shape: 10x10 footprint, 14 high (plinth and column).
- **Which totem:** `statue/TotemType` (an extensible enum). Minecraft refuses a one-value block-state property, so while Warding is the only totem the `totem` property is left off and every statue is a warding statue. With a second totem the property switches on, existing statues load as warding (the default), and blockstate variants and loot conditions get one entry per type (see `TotemStatueBlock`'s javadoc).
- **Art:** scripted placeholder at 16x and 32x (`tools-local/make_statue_textures.py`): stone plinth and column with the totem's sprite carved on the front and a glowing rune line on the sides. Listed in `docs/art-todo.md`; the owner will design the statues.

### Tooling

`tools/install-to-modrinth.ps1`, `tools/build-testworld.py` (`MODS`) and `mods/testworldshots` (`modsUnderTest`) include this mod; the test world builder names `bubbas_totems_artifacts:totem_of_warding` (the saved world's old-id totems load through the alias). No mod icon yet; it must be person-made (B14).

## Open questions (owner)

- Mob holders: should mobs holding a totem pulse, and may woken undead pick one up? (built as yes, agent's choice)
- The talisman's name and how it's found (grave/crypt loot once worldgen exists).

## Milestone test objectives

Test-lite (`specs/quality.md`, Test suites). Run: `./gradlew :bubbas_totems_artifacts:build` (standalone core); `:bubbas_totems_artifacts:runGameTest -PwithMobs -PwithBeacons -PmilestoneTests` for every test for real (the sleeper tests pass trivially without `-PwithMobs`, the beacon test without `-PwithBeacons`).

- Core (`BubbasTotemsArtifactsCoreGameTest`, 9): old Reworked Mobs ids load as these items (registry lookup and the item stack codec, re-saved under the new id); sneaking places a warding statue facing the player and breaking it drops the totem (not sneaking places nothing); the talisman senses undead mobs at 29/17/11 blocks; it senses sleepers; a held totem pulses onto the cooldown and scares a zombie away; the pulse calms sleepers; a ward stops spawns and sends monsters out; a full ward keeps sleepers asleep and releases them on removal; a ward pushes back mobs that don't path.
- `GraveTalismanGameTest`: the glow-level table; undead mobs and sleepers sensed at each level.
- `WardingGameTest`: the core warding checks plus the off-hand totem pulses and a backpack one doesn't; a zombie can't path into a ward; a ridden happy ghast is put back outside; mansion chests (about 20 %) and evokers (about 5 %) give a totem.
- `WardingBeaconGameTest`: a totem beacon wards its range (1 layer: spawns only, 20 blocks; 4 layers: 50 blocks and sleepers kept asleep). Real only with `-PwithBeacons`.
- `TotemStatueGameTest`: sneak-place, facing, no place without sneaking, break drops the totem.
- Client (`ClientTests` names `talisman`, `warding`; screenshots): the talisman's glow at each distance in hand and hotbar (vanilla husk, skeleton, zombie, so it runs without Reworked Mobs); the Totem of Warding in the hotbar, its pulse ring and cooldown sweep in first and third person; the statue from three angles at 16x and 32x.
