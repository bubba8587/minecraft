<!-- dte:C6,C1,B15 -->
# Bubba's Totems & Artifacts: docs to merge (2026-11-16)

Builder's report for the orchestrator or a docs agent. The new mod `bubbas_totems_artifacts` exists and now owns the Grave Talisman and the Totem of Warding, which left Bubba's Reworked Mobs. Merge this into `specs/totems-artifacts.md` (its "What moves here", "Independence", "Open questions" and "Milestone test objectives" sections; the header already has its `covers:` line), trim the Grave Talisman and Warding parts of `specs/undead.md` down to what stayed in Reworked Mobs (light calming, bells, the sleeper hook), point `specs/reworked-beacons.md`'s mention of `compat/WardingBeaconPayload` at the new mod, and do the player vault move below. No spec prose or player docs were edited on this branch.

## What moved

From Reworked Mobs to Totems & Artifacts, behavior unchanged unless listed under player-facing changes:

| Piece | Was (`bubbas_reworked_mobs`) | Now (`bubbas_totems_artifacts`) |
|---|---|---|
| Grave Talisman item | `bubbas_reworked_mobs:grave_talisman`, `undead/GraveTalisman` | `bubbas_totems_artifacts:grave_talisman`, `talisman/GraveTalisman` |
| Talisman glow (client) | `client/undead/GraveTalismanGlow`, model property `bubbas_reworked_mobs:grave_talisman_glow` | `client/talisman/GraveTalismanGlow`, property `bubbas_totems_artifacts:grave_talisman_glow` |
| Totem of Warding item, held pulse | `bubbas_reworked_mobs:totem_of_warding`, `warding/TotemOfWardingItem` | `bubbas_totems_artifacts:totem_of_warding`, `warding/TotemOfWardingItem` |
| Wards, barrier, flee goal, loot pools, tuning | `warding/Wards`, `WardBarrier`, `WardFleeGoal`, `WardingLoot`, `WardingTuning` | same names under `warding/` |
| Ward mixins | `MobTickMixin`, `PhantomSpawnerMixin`, `Walk/SwimNodeEvaluatorMixin`, `MobAccessor`, ward half of `SpawnStateMixin` | same names (prefix `bta$`), plus `WardSpawnStateMixin` (just the ward check); Reworked Mobs' `SpawnStateMixin` keeps only the monster-cap cut |
| Beacon payload | `compat/WardingBeaconPayload` (entrypoint in Reworked Mobs) | `compat/WardingBeaconPayload` (entrypoint `bubbas_reworked_beacons:payload` in Totems & Artifacts); lang keys `beacon.bubbas_totems_artifacts.ward*` |
| Textures 16x/32x, models, item definitions, lang | Reworked Mobs assets and 32x pack | Totems & Artifacts assets and its own 32x pack (`bubbas_totems_artifacts_32x`) |
| Texture scripts | `tools/warding-textures.py`, `mods/bubbas_reworked_mobs/tools-local/make_talisman_textures.py` | same script / moved to `mods/bubbas_totems_artifacts/tools-local/`, both now write into the new mod |
| Creative tab | Reworked Mobs tab | new tab "Bubba's Totems & Artifacts" |
| Tests | `GraveTalisman*GameTest`, `Warding*GameTest`, client parts `talisman`, `warding` | same, in the new mod (see Tests) |

Stayed in Reworked Mobs: sleepers, hearing, light calming sleepers and bells (their numbers moved from `WardingTuning` to `UndeadTuning`: `LIGHT_CALM_LEVEL`, `LIGHT_CALM_FACTOR`, `BELL_POINTS`, `BELL_RANGE`), loot displays, combat, the monster cap. Reworked Mobs no longer depends on Reworked Beacons at all (its `compileOnly` and `suggests` are gone; it now `suggests` Totems & Artifacts).

## The hook (Reworked Mobs `api/Sleepers`)

A small public class, `io.github.bubba8587.bubbas_reworked_mobs.api.Sleepers`, same idea as Reworked Beacons' payload API. Stable: add, don't change.

- `int calm(ServerLevel, Vec3 centre, double radius, float points)`: takes points off the disturbance meter of sleepers in range (wake 100). Used by the held totem's pulse.
- `void keepAsleep(ServerLevel, Object source, AABB area, int lifetimeTicks)` / `void release(ServerLevel, Object source)`: sleepers inside the area never wake from noise; each source's area lapses unless refreshed (no cleanup hook needed). Used by a full-pyramid ward. Backed by `undead/sleeper/SleepAreas`; `SleeperBlockEntity.effectiveSensitivity` checks it where it used to check `Wards`.
- `boolean isKeptAsleep(Level, BlockPos)`, `boolean isSleeper(Level, BlockPos)`, `double nearestSqr(Level, Vec3, double range)` (client or server; what the talisman senses).

Totems & Artifacts reaches it only through `compat/SleeperHook`: the hook is picked once (`FabricLoader.isModLoaded("bubbas_reworked_mobs")`); only `compat/ReworkedMobsSleepers` names Reworked Mobs' classes and it is created only when that mod is loaded. Build: `compileOnly` against both optional mods; `fabric.mod.json` `suggests` both. Neither mod requires the other.

Without Reworked Mobs: the talisman senses undead mobs (`#minecraft:undead`) only; the Totem of Warding still scares monsters, stops natural monster spawns in a ward, keeps monsters and happy ghasts out and pushes flyers out; it just has no sleepers to calm or keep asleep.

## Saved worlds (old item ids)

`ModItems` registers a Fabric registry alias for each item (`BuiltInRegistries.ITEM.addAlias(old, new)`, Fabric API's `FabricRegistry`, the cleanest option on 26.3: vanilla has no item-id alias, and a DataFixer would be heavier and only run on version bumps). Any lookup of `bubbas_reworked_mobs:grave_talisman` / `:totem_of_warding` that isn't registered falls back to the new item, so old stacks in chests, inventories, item frames, the test world and commands load as the new items; saved again they are written under the new id. The alias lives in Totems & Artifacts, so it works whether or not Reworked Mobs is installed. If Totems & Artifacts isn't installed, those old stacks have no item to become: Minecraft drops them as unknown items when the chunk or player loads (vanilla behavior for a removed item). Players updating Reworked Mobs should install Totems & Artifacts alongside to keep their totems and talismans.

## New block: totem statue (owner addition, B15 exception)

- `bubbas_totems_artifacts:totem_statue`. B15 reason, for the record: one shared block for every totem, the owner's request (2026-11-16), so a found totem can be set down as decoration.
- **Placing:** sneak and use a Totem of Warding on a block *(agent's choice: sneaking, so holding a totem for its pulse never places it by accident; without sneaking the click does nothing)*. The statue faces the player and the totem is used up (creative keeps it). The Grave Talisman is not a totem and has no statue.
- **Inert:** no pulse, no ward, no beacon effect, no block entity, no ticking.
- **Breaking** (any tool, pickaxe fastest; survives-explosion check like most blocks) drops the totem back. Pistons pop it off as its totem.
- Shape: 10x10 footprint, 14 high (plinth and column).
- Which totem: `statue/TotemType` (an enum, extensible). Minecraft refuses a block-state property with only one value, so while Warding is the only totem the `totem` property is left off the block and every statue is a warding statue. When the second totem is added the property switches on by itself, existing statues load as warding (the default), and the blockstate variants and loot conditions get one entry per type (noted in `TotemStatueBlock`'s javadoc).
- Art: scripted placeholder at 16x and 32x (`tools-local/make_statue_textures.py`): a stone plinth and column with the totem's own item sprite carved on the front and a glowing rune line on the sides. Listed in `docs/art-todo.md`; the owner will design the statues.

## Player-facing changes

- The Grave Talisman and Totem of Warding are now in **Bubba's Totems & Artifacts**. Items from older worlds carry over when it is installed.
- **Totem statues:** sneak + use a Totem of Warding on a block to set it down as a statue; mine it to get the totem back. Statues do nothing.
- **New creative tab** "Bubba's Totems & Artifacts" with both items (they left the Reworked Mobs tab).
- On a Reworked Beacons beacon screen, the Totem of Warding's "sleeping undead stay asleep" line shows only when Reworked Mobs is installed *(agent's choice: without it the line would describe nothing)*.
- Without Reworked Mobs, the talisman reacts to undead mobs only, and the totem has no sleepers to calm (everything else unchanged).

## Player docs plan (new folder)

Create `player-docs/Projects/Bubba's Totems & Artifacts/` with:

- `Bubba's Totems & Artifacts.md`: overview page (what artifacts are, works alone, optional links to Reworked Mobs and Reworked Beacons, upgrading from Reworked Mobs: install this mod too to keep your items).
- `Grave Talisman.md`: moved from Reworked Mobs; add "senses undead mobs; with Reworked Mobs also sleeping undead".
- `Totem of Warding.md`: moved from Reworked Mobs; add the "Without Reworked Mobs" note and the statue.
- `Totem Statues.md` (or a section on the totem pages): sneak to place, mine to take back, decorative only.

Then: replace the two pages in the Reworked Mobs folder with a one-line pointer (or delete and fix links in `Home.md`, `Bubba's Reworked Mobs.md`, `Fighting the Undead.md`, `Monster Spawning.md`, `Noise and Waking.md`, `Sleeping Undead.md`, Reworked Beacons' `Beacon.md` and `Bubba's Reworked Beacons.md`, Infused Gems' `Gem Effects.md`, which all mention them), keep light and bells under Noise and Waking, and add the project to `Home.md` and `Version Status.md` (26.3, not released yet).

## Tests

Totems & Artifacts (`BubbasTotemsArtifactsCoreGameTest`, core, 10 tests): old ids load as the new items (registry lookup and the item stack codec, re-saved under the new id); sneak places a warding statue facing the player and breaking it drops the totem (not sneaking places nothing); talisman senses undead mobs at 29/17/11 blocks; talisman senses sleepers; held totem pulses (cooldown sweep) and scares a zombie away; pulse calms sleepers; ward stops spawns and sends monsters out; full ward keeps sleepers asleep (and releases on remove); ward pushes back mobs that don't path. The sleeper tests pass at once without Reworked Mobs and run for real with `-PwithMobs`.
Milestone (6 more): glow-level table, off hand / backpack pulse, ridden happy ghast, no path into a ward, loot (mansion about 20 %, evoker about 5 %), beacon payload (`-PwithBeacons`).
Run: `./gradlew :bubbas_totems_artifacts:build` (standalone); `:bubbas_totems_artifacts:runGameTest -PwithMobs -PwithBeacons -PmilestoneTests` (all 16 for real).
Client parts: `talisman` (now uses vanilla husk/skeleton/zombie, so it runs without Reworked Mobs), `warding` (item, pulse, cooldown sweep, plus the statue from three angles at 16x and 32x). The old "sleeper near torches" shot stayed out: it shows Reworked Mobs' light calming, which has no visual.

Reworked Mobs: `SleeperCalmGameTest` (was `WardingGameTest`): light and bells unchanged, plus core `apiCalmsSensesAndKeepsSleepersAsleep` for the hook. Core 11, full 62, all pass.

Milestone test objectives to add to `specs/totems-artifacts.md`: one line per class above (`GraveTalismanGameTest`, `WardingGameTest`, `WardingBeaconGameTest`, `TotemStatueGameTest`), and remove the moved ones from `specs/undead.md`.

## Tooling

- `tools/install-to-modrinth.ps1` default mod list, `tools/build-testworld.py` `MODS`, and `mods/testworldshots` `modsUnderTest` include the new mod.
- Test world builder text (`testworlds/builder/.../grounds_paired`, `grounds_wardrobe`, `verify`, `tools/testworld-grounds.py`) now names `bubbas_totems_artifacts:totem_of_warding`. The world was not rebuilt on this branch (worktree rule); the saved world's old-id totems load through the alias anyway.
- No mod icon yet (none of the other mods ship one either); must be person-made (B14).
