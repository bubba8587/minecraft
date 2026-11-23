# Undead follow-ups (undead-2): builder report (2026-11-22)

Branch `undead-2` (worktree `../mc-undead2`), from develop. One section per step; each step is committed with REPORT READY.

## 1. Back to sleep at dawn (specs/undead.md)

### Player-facing

- Woken skeletons, zombies, husks and strays that were awake during the night and are still about at sunrise, with nothing to fight, go back to sleep: they walk to the nearest place out of the sun within **16 blocks** (4 up or down) and become a sleeper block again, keeping everything they carry (gear, picked-up loot, name).
- **Owner's rule (2026-11-22): an undead can always sleep by sitting down in a pocket where it's safe to stand.** So a place is any spot on a solid floor with a roof over it (something that blocks light: glass and barriers don't count) and dim (block light 7 or less; a candle-lit crypt is fine, a torch-lit room isn't):
  - room to stand → it **sits** down there;
  - a one-high niche (a crypt wall niche) → it **lies** down (from beside it: it has no room to step in first);
  - an **open grave** (a pit walled on three sides) works too, roof or not → it lies down.
- Crypts refill themselves, and a cleared graveyard quietly fills up again if you leave survivors. Undead you wake by day (underground, say) don't go straight back to sleep: only those that saw the night. The resting dead are not involved.
- A soft sound (the sleeper's place sound) as it settles.

### Numbers

| What | Value | Where |
|---|---|---|
| Search range | 16 blocks across, 4 up/down | `UndeadTuning.SLEEP_SEARCH_RANGE`, `SLEEP_VERTICAL_RANGE` |
| Searches again every | 3 s (+ up to 1 s) | `UndeadTuning.SLEEP_RESEARCH_TICKS` |
| Dim enough | block light <= 7 | `UndeadTuning.SLEEP_MAX_LIGHT` |
| Walk speed there | 1.2x | `BackToSleep` |

### Spec facts

- `undead/sleeper/BackToSleep` is a vanilla `MoveToBlockGoal` (agent 1 / owner rule: vanilla subsystems over hand-rolled scans): nearest-first search, throttling and walking are vanilla's; `isValidTarget` = the floor under a valid place (`BackToSleep.pose`); it lies down when within 1.6 blocks (`acceptedDistance`). Registered at priority 1 in `WokenZombie` and `WokenSkeleton` (so husks and strays too); runs only by day (`isBrightOutside`, overworld-like dimensions) with no target, not riding or leashed.
- "Saw the night" is the entity tag `bubbas_reworked_mobs.saw_night`, added in `customServerAiStep` while it's dark (`BackToSleep.noteNight`), so it survives a reload.
- Lying down: `BackToSleep.lieDown` places the variant's sleeper block (pose from the place, facing the open side for a niche, waterlogged in water), copies every equipment slot into the block entity's gear and the custom name, then discards the mob.
- An earlier hand-rolled ring search was replaced by the goal before commit.

### Tests (milestone, `DawnGameTest`)

- `survivorsGoBackToSleepAtDawn`: at noon a woken zombie that saw the night lies down in a stone niche, as a sleeper with its helmet and shovel.
- `onlyThoseWhoSawTheNight`: one woken by day stays up.
- `sitsDownUnderAnyCover`: under a plain stone canopy with standing room it sits down.
- Full milestone suite 143/143.
- No screenshots for this step (no new visuals: the sleeper poses are the existing ones).

## 2. Fire as a weak ward (specs/undead.md)

### Player-facing

- A **lit campfire or soul campfire, fire or soul fire** keeps hostile mobs that fear fire about **6 blocks away** (counted along the ground's grid, up to 3 up or down): an idle zombie, skeleton, creeper or spider that comes near walks off again. An unlit campfire does nothing.
- It's a **weak ward**, a deterrent and not a wall. Builder's proposal for the spec's open detail: **a mob already hunting someone pushes straight through.** Only mobs with nothing to fight keep off.
- **Who fears it:** goal-driven hostile mobs that fire hurts. **Never** fire-immune mobs (blazes, ghasts, magma cubes, wither skeletons, zombified piglins and the like) and never the Nether natives that fire would hurt (piglins, piglin brutes, hoglins, zoglins), nor the warden.
- Camps generate with an unlit campfire, so they don't ward themselves (and a lit fire also quiets a camp, as before).

### Numbers

| What | Value | Where |
|---|---|---|
| Keep-away distance | 6 (Manhattan, across) | `UndeadTuning.FIRE_WARD_RADIUS` |
| Up and down | 3 | `UndeadTuning.FIRE_WARD_VERTICAL` |
| Checked | every half second per mob (spread by entity id) | `FireWard.canUse` |
| Walks off to | a spot up to 9 away from the fire, at 1.1x speed | vanilla `DefaultRandomPos.getPosAway` |

### Spec facts

- Built the way vanilla piglins and hoglins avoid their repellents (specs/vanilla-subsystems.md): block tag `#bubbas_reworked_mobs:undead_repellents` (campfire, soul campfire, fire, soul fire); a campfire counts only while lit (`CampfireBlock.isLitCampfire`, as `PiglinSpecificSensor.isValidRepellent`); the nearest is found with `findBlocksInBoxByManhattanDistance(pos, 6, 3).filterState(FireWard::repels).findFirst()` (as `HoglinSpecificSensor`); the mob walks to `DefaultRandomPos.getPosAway` from it.
- `undead/FireWard` is a goal (priority 1, MOVE flag), added in `Mob`'s constructor by `mixin/FireWardMixin` to every server-side mob for which `FireWard.fearsFire` holds: `Enemy`, `PathfinderMob`, not `fireImmune()`, not in entity tag `#bubbas_reworked_mobs:unafraid_of_fire` (piglin, piglin brute, hoglin, zoglin, warden). Runs only with no target.
- Why a goal and not a brain sensor: the mobs it covers (zombies, skeletons, creepers, spiders...) are goal-driven; brain-driven hostile mobs (breeze, creaking and the like) don't get it. Worth an owner call if any of those should.
- The tag name follows agent 1's brief (`undead_repellents`) although it repels every fire-fearing hostile mob, not only undead.

### Tests (milestone, `FireWardGameTest`)

- `idleMobsKeepAwayFromALitCampfire`: an idle zombie 2 blocks from a lit campfire walks off to 4+ blocks.
- `onlyLitFireRepels`: lit campfire, lit soul campfire, fire, soul fire repel; an unlit campfire doesn't.
- `onlyFireFearingMobs`: zombie, skeleton, creeper, spider yes; blaze, wither skeleton, piglin, hoglin, zombified piglin, magma cube, warden, cow no.
- Full milestone suite 146/146.

### Code comments

Per agent 1's relay of the owner rule (B38: comments are labels only), the comments and javadoc in the new undead-2 code (BackToSleep, FireWard, FireWardMixin, the UndeadTuning additions) were cut to short labels; their reasons are in this report.

## 3. Drowned sleepers in shipwrecks (specs/undead.md)

### Player-facing

- New **drowned sleeper** (creative: "Sleeping Drowned"): the ship's drowned crew. It wakes like other sleepers (noise meter, gear kept) into a drowned, which then behaves like the reworked drowned (lurks in water).
- **Shipwrecks** now have their crew lying or sitting **between decks**: each empty or flooded spot on a wooden floor with wood overhead within 4 blocks has a 12% chance of a sleeper (owner: they must be inside the boat, not outside on the sand). Full wrecks get a crew of several (a full ship in the test world had 7); broken fragments with little room inside mostly have none. Waterlogged where flooded.
- Gear: 15% a trident, 15% a fishing rod, 10% a nautilus shell in the off hand, 25% a leather cap.
- Sleepers now look like their mob: drowned sleepers wear the drowned's seaweed hair and rags, stray sleepers their ragged clothes (the outer layer was missing; owner spotted the drowned).
- Item icon: a drowned head with its eyes shut, 16x and 32x (art-todo row updated).

### Numbers

| What | Value | Where |
|---|---|---|
| Chance per spot between decks | 12% | `UndeadTuning.SHIPWRECK_CREW_CHANCE` |
| Trident / rod / shell / cap | 15% / 15% / 10% / 25% | `ShipwreckCrew.place` |

### Spec facts

- `UndeadVariant.DROWNED` (block, item, woken entity `WokenDrowned extends Drowned`, so the drowned mind mixins apply); tags `#woken_undead`, `#grabs_loot`, `#zombies`, `#burn_in_daylight`; particle texture prismarine; vanilla `DrownedRenderer` for the woken mob.
- Placement: `mixin/ShipwreckCrewMixin` at every RETURN of `ShipwreckPieces.ShipwreckPiece.postProcess` (it has two exits; injecting only at TAIL missed every chunk after a wreck's first) calls `undead/ShipwreckCrew.place` over the piece box clipped to the chunk. Spots: `ShipwreckCrew.inHull` = air or water, sturdy wooden floor, wood within 4 above; one column only, so every chunk sees the same answer (a sideways check across chunk edges read unbuilt neighbours). Per-spot roll from vanilla's positional random (`XoroshiroRandomSource(seed).forkPositional().at(pos)`, seed = world seed ^ piece centre).
- Sleeper outer layers: `PosedHumanoid` takes an optional outer layer (model layer and texture), posed like the body: drowned `ModelLayers.DROWNED_OUTER_LAYER` + `drowned_outer_layer.png`, stray `STRAY_OUTER_LAYER` + `stray_overlay.png`.

### Tests

- Milestone `ShipwreckCrewGameTest`: `shipwrecksHaveADrownedCrew` (a vanilla full shipwreck piece built as worldgen builds it has drowned sleepers), `drownedSleeperWakesAsADrowned`. Full milestone suite 148/148.
- Client part `shipwrecks` (`ShipwreckClientGameTest`): checks the nearest natural shipwrecks of seed 20261116 (fails if any crew sleeper isn't between decks: one full wreck had 7, all inside; fragments 0); screenshots looked at: `drowned-compare` / `-close` (drowned sleepers sitting and lying beside a vanilla drowned, same look), `shipwreck-sleeper-0..2` (crew below deck). The variants part's `variants-sleepers` shows the stray sleepers with their clothes.

## 4. Will-o'-wisps (specs/graveyards.md)

### Player-facing

- Faint, occasional **soul fire flames** (vanilla `soul_fire_flame`) lead you to crypt yards you haven't found yet:
  - **Hints:** while a yard is loaded within **96 blocks**, now and then (about one every 8 seconds per yard) a single wisp appears **6-12 blocks from you toward the yard**, drifting that way.
  - **Over the graves at night:** within **48 blocks**, wisps rise over the yard's graves now and then (about one every 2 seconds) and drift slowly toward the **crypt entrance** (the top of its stairs; the gate if the yard has no crypt).
- **Cleared for good once found:** walking into a yard (anywhere inside its footprint) adds it to your own saved list; its wisps stop showing **for you**. Other players who haven't found it still see them; the server sends wisps only to players who haven't found that yard. The list survives death.

### Numbers

| What | Value | Where |
|---|---|---|
| Checked | every second | `GraveTuning.WISP_TICKS` |
| Hint range / chance / distance | 96 blocks / 12% per check / 6-12 blocks from you | `WISP_RANGE`, `WISP_HINT_CHANCE`, `WISP_HINT_MIN`/`MAX` |
| Night range / chance | 48 blocks / 50% per check | `WISP_NIGHT_RANGE`, `WISP_NIGHT_CHANCE` |
| Yards looked for | 6 chunks round each player | `WISP_DISCOVER_CHUNKS` |

### Spec facts

- `grave/Wisps`: yards are found from the structure references of loaded chunks round each player (as camps are) and kept per level in memory (not saved: they're rediscovered from chunk data after a restart); the command build registers its yard by hand. Players: `level.getPlayers(EntitySelector.NO_SPECTATORS)`. Particles go only to the player concerned: `ServerLevel.sendParticles(ServerPlayer, ...)` with count 0 (the offsets give the drift).
- Found yards: persistent player attachment `bubbas_reworked_mobs:found_yards` (a list of yard ids = start chunk), copied on death.
- `CryptYardPiece.wispTarget()`: the top of the crypt stairs, or above the gate.

### Tests

- Milestone `WispsGameTest.walkingIntoAYardFindsIt`: a built yard is registered; a player outside hasn't found it; inside, it's found.
- Client part `wisps` (`WispsClientGameTest`): at night from outside a built yard, wisps forced over the graves plus hints; `wisps-1` looked at (blue soul flames over the yard and toward it). Natural ones are much sparser (one at a time).
- Full milestone suite 128/128 (develop's test cleanup merged several tests).

## 5. Noble dead in gemmed armor (specs/graveyards.md)

### Player-facing

- **With Bubba's Infused Gems installed**, the sleepers of **noble crypts** (the armoured ones in full iron, chain or gold) wear **gemmed** gear: 1-2 gems in their armor, 1 in their weapon, infusion 2-4, tier 2-3 (a little richer than ordinary gemmed mob gear). Woken, they fight with those effects; rob or kill them and the gemmed pieces are the prize.
- Without Infused Gems, nothing changes (no hard dependency).
- In a test noble crypt 11 of 13 armoured sleepers carried gems: Infused Gems leaves some pieces without (its own rules on what takes a gem).

### Spec facts

- Data, not code, decides the gems: item modifier `data/bubbas_reworked_mobs/item_modifier/noble_finery.json` uses Infused Gems' own `bubbas_infused_gems:socket_random_gems` (armor 1-2, weapon 1; infusion [2,3,3,4], tier [2,2,3], any variant) and loads only with the mod (`fabric:load_conditions` `fabric:all_mods_loaded`).
- `grave/NobleFinery.apply` runs it over a noble sleeper's gear when the crypt is built (`CryptYardPiece.Build.sleeper`, noble = armour chance 0.8 or more); if the modifier isn't loaded it does nothing. Loot context: chest param set with the sleeper's position; seeded per position.
- New build option `-PwithGems` (Reworked Mobs) puts Infused Gems on the test game's classpath, like `-PwithBeacons`.

### Tests

- Milestone `NobleFineryGameTest.nobleDeadWearGemsOnlyWithInfusedGems`: a built noble crypt; without Infused Gems no gear carries its components; with `-PwithGems` most noble sleepers' gear does (11 of 13). Both runs pass; full milestone suite 129/129.
- No screenshot: gemmed gear looks the same on a sleeper.

## 6. Maps (specs/monster-camps.md "Maps")

### Player-facing

- **Cartographers** sell three new explorer maps (a compass plus emeralds, like vanilla's):
  - **Graveyard Explorer Map** (journeyman, level 2): 8 emeralds, marks the nearest crypt yard with a red X.
  - **Monster Camp Explorer Map** (journeyman, level 2): 8 emeralds, marks the nearest camp with a white X.
  - **Fabricator Explorer Map** (expert, level 4): 20 emeralds, marks the **ancient city** of the nearest Ancient Fabricator; the Fabricator itself is about **150 blocks** from the X (every natural Fabricator sits that far from its city).
- **Fabricator office chests** hold a Fabricator Explorer Map to the **next** one (it skips the city next to this Fabricator, which is already generated).
- They join vanilla's trade pools for those levels (so a cartographer offers them now and then among its usual trades).

### Owner question (open)

- Owner (2026-11-22): "can we not have a vanilla map that points to a fabricator directly?" Not as things stand: vanilla explorer maps only find **structures**, and Fabricators are placed **features** beside their city. A marker structure sharing the city's placement would still point at the city chunk (vanilla locates by placement chunk). Choices: (1) X on the city, as built (pure vanilla); (2) make the Fabricator a real structure, after which vanilla maps, `/locate` and the Totem of Seeking point straight at it (bigger change, in the mob factory's code); (3) a small custom map function that points exactly (was built first, removed after the owner's "no new systems"). Builder leans to (2) if exact pointing matters.

### Spec facts

- All data, vanilla systems: structure tags `bubbas_reworked_mobs:on_graveyard_maps` (crypt_yard), `on_camp_maps` (monster_camp), `on_fabricator_maps` (minecraft:ancient_city); villager trades `data/bubbas_reworked_mobs/villager_trade/cartographer/{graveyard,camp,fabricator}_map.json` (vanilla `exploration_map` + `filtered` on map id, `filled_map` with an `item_name`); added to `#minecraft:cartographer/level_2` and `level_4` (`replace: false`); office chest pools (`chests/fabricator_office`, `fabricator_office_with_gems`) use `exploration_map` with `skip_existing_chunks: true`. Map names in the lang file. (26.3 has no map colour component, so the maps keep the default look.)

### Tests

- Client part `maps` (`MapsClientGameTest`, seed 20261116): each trade, offered by a cartographer, makes a named map with a target: graveyard -208,-368; camp 80,0; Fabricator map's X at a city with its Fabricator 148 blocks away (fails if no Fabricator within 400 of the X). `maps-graveyard` looked at: the Graveyard Explorer Map in hand with its red X.
- Full milestone suite 129/129.
