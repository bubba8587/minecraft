<!-- dte:C6 -->
# Totems & Artifacts, totems-2: docs report (builder: agent 3)

For `specs/totems-artifacts.md` and the player docs page. **(builder)** marks my choices. Code comments are labels only (B38), so the how and why are here.

## Player-facing

- **Gathering in a beacon: a ring** (replaces "into a chest below").
  - Dropped items and XP in the beacon's area fly to the beacon and circle it, weightless, just above its top. The ring is 1.25 blocks out from the beacon's middle, 1.05 above its base, and turns once every 5 s.
  - A hopper against any side of the beacon, pointing into a chest, collects the items as they come round. Its pickup space (the column from 11/16 to 2 blocks above it) is where the ring passes.
  - Items in the ring never despawn. XP orbs stay in the ring until a player comes within 2.5 blocks, then fly to them as usual.
  - At most 48 things in one ring. If the beacon stops, the ring lapses after about 5 s and everything falls.
  - Each pulse refreshes the ring; new things join every half second.
  - Beacon text: "Dropped items and experience circle the beacon".
- **Growth plants seeds** (Harvest folded in). Held, in a beacon, or as a statue:
  - Every 2 s, with a 35 % chance, one dropped plantable item in range is planted in a free spot within 3 blocks of it, where vanilla says it can grow.
  - Plantable means any item whose block Growth already speeds up (the `growth_totem_boosts` tag): seeds, carrots, potatoes, saplings, berries, nether wart, and so on.
  - It picks a random item, never sweeps the area. It never harvests or gathers. Items circling a Gathering ring count.
- **Totem of Herding:**
  - Held, livestock follow you as if you held their food. Every animal counts except tamed pets.
  - In a beacon, or as a statue, livestock in the area won't walk out of it, like a pasture without fences.
  - A lead still pulls them out, and so does a player within 10 blocks holding the totem, since they follow it out.
  - Found in village shepherd (12 %), butcher (8 %) and tannery (8 %) chests. Beacon text: "Animals stay in the area".
- **Totem of Clear Skies:**
  - Used while it rains or storms, it ends the weather (vanilla's clear spell) and is used up. In clear weather nothing happens.
  - In a beacon, or as a statue, no rain, snow or lightning on the area's columns, sky to ground. No snow builds up and cauldrons don't fill. Ice still forms in cold biomes.
  - Found in igloos (12 %), desert village houses (6 %) and shipwreck supply chests (5 %). Beacon text: "No rain, snow or lightning in the area".
- **Statues give a tiny-radius effect** (a 7x7x7 box round the statue, every second):
  - Warding: monsters keep out; sleepers are not kept asleep.
  - Gathering: drops go into a chest or barrel touching the statue.
  - Fortification: no blast damage to blocks.
  - Growth: twice as fast, plus planting.
  - Herding: a small pasture.
  - Clear Skies: dry.
  - Keeping, Seeking, the Beginning and the End: nothing (no area form).
  - **(builder)** radius 3, and Warding's statue doesn't keep sleepers asleep.
- **Art (initial, builder; owner will redo):**
  - Herding: a spruce idol with a woolly ram's head and curled horns, and a gold bell on the chest.
  - Clear Skies: pale calcite, blue eyes, a gold sun crown of rays, and a patch of blue sky with the sun on the chest.
  - Items and statues, 16x and 32x.

## How it's built (for the spec)

- `gathering/GatheringRing`: rings per level, keyed by beacon position.
  - Every tick each member's velocity is set toward its slot; slots are evenly spaced and turning. `needsSync` keeps clients smooth.
  - Collection uses `getEntities` on the beacon area, spread across ticks by the beacon position.
  - The ring only steers vanilla entities; vanilla's hopper does the collecting.
- `Gathering.intoChest(level, statue)` / `chestBeside`: the old chest-under-beacon code, now the statue's effect.
  - `chestUnder` and the XP-onto-beacon step are gone, along with the `GATHER_BEACON_MAX_PER_PULSE`/`GATHER_CHEST_DEPTH_BELOW_PYRAMID` users. The constants are still in TotemsTuning: the audit commit removes them.
- `Growth.plant(level, box)`: candidate spots come from `BlockPos.randomBetweenClosed` (32 tries, y −3..+1); a spot is valid if it's air and `canSurvive`.
  - Run from the held totem (every 40 ticks, spread by holder id) and from GROWTH areas (spread by area).
- `herding/Herding`:
  - Following: `TemptGoalMixin.shouldFollow` and `TemptingSensorMixin.playerHoldingTemptation` return true for livestock when the player holds the totem.
  - Pasture: `TotemAreas.Kind.HERDING`. `Herding.keepsIn` hooks the same node-evaluator mixins as wards. Nodes outside the pasture are BLOCKED for livestock standing inside it, unless the animal is leashed or a herder is near (`getNearestPlayer` with `EntitySelector.NO_SPECTATORS`).
- `skies/ClearSkies`:
  - Server: `TotemAreas.Kind.CLEAR_SKIES`, flattened per tick to column rectangles.
  - Mixins: `Level.precipitationAt` (covers `isRainingAt`: lightning, wetness, rain putting out fire) and `ServerLevel.tickPrecipitation`'s `isRaining` (snow layers, cauldrons; freezing untouched).
  - Clients get the rectangles once a second (payload `bubbas_totems_artifacts:clear_skies`, with the dimension) and once more when the areas end. `ClientLevel.getPrecipitationAt` (rain and snow drawing, splashes, rain sounds) asks them. A client copy older than 3 s, or from another dimension, is ignored.
- `statue/StatueEffects` and `TotemStatueBlock`:
  - A pulse is a scheduled block tick every 20 ticks, with no block entity; `onPlace` starts it.
  - Statues saved before this restart from a random tick (`isRandomlyTicking` only for types with an effect).
  - Each pulse refreshes its area for 40 ticks, keyed by a `Source(pos)` record so it never collides with a beacon's key.
- Weather end: `MinecraftServer.setWeatherParameters(RAIN_DELAY.sample, 0, false, false)`, as `/weather clear` does.

### Vanilla-subsystems check (specs/vanilla-subsystems.md)

- Entity finding: `getEntitiesOfClass` / `getEntities` with `EntitySelector` predicates.
- Players: `getNearestPlayer(x, y, z, r, NO_SPECTATORS…)`; no `players()` loops.
- Planting spots: `BlockPos.randomBetweenClosed`. Statue pulses: `scheduleTick`. Following: vanilla's temptation.
- Hand-rolled, with the reason:
  - `chestBeside` looks at the 6 neighbours. That's a fixed adjacency check, not a search.
  - The ring's slot motion: vanilla has nothing that orbits entities.

## Numbers (TotemsTuning)

| What | Value |
|---|---|
| Ring radius / height / period | 1.25 / 1.05 / 100 ticks |
| Ring fly-in speed / max members / join check / XP release | 0.4 / 48 / 10 ticks / 2.5 blocks |
| Planting check / chance / reach | 40 ticks / 35 % / 3 |
| Herder lead-out range | 10 |
| Statue radius / pulse / Growth statue extra | 3 / 20 ticks / 1 |
| Herding loot | shepherd 12 %, butcher 8 %, tannery 8 % |
| Clear Skies loot | igloo 12 %, desert house 6 %, shipwreck supply 5 % |

## Tests

- **Core (new):**
  - `gatheringRingFeedsASideHopper`: an item 6 blocks out ends up in a chest through a side hopper.
  - `growthPlantsDroppedSeeds`: a wheat seed is planted on farmland, exactly one.
  - `herdingPastureKeepsLivestockIn`: a cow is blocked at the pasture edge, not inside; a zombie never.
  - `clearSkiesKeepsRainOff`: an area column gets no precipitation while it rains; outside does.
  - `statuesPulseTheirArea`: Growth and Herding statues set areas of radius 3; Keeping none.
- **Milestone:** `TotemsBeaconGameTest.gatheringBeaconStartsTheRing` (with `-PwithBeacons`) replaces `gatheringBeaconFillsTheChestBelow`.
- **Milestone objectives, no test yet:**
  - A cow follows a player holding the Totem of Herding, and a goat (brain-based) does too.
  - Using Clear Skies in rain clears the weather and uses it up; in clear weather it isn't used.
  - The client draws no rain over a Clear Skies area.
  - XP leaves the ring for a nearby player.
- **Client (part `totems`):** `totems-gathering-ring` and `-later` (the ring circling, an item entering the hopper); the hotbar now shows Herding and Clear Skies, 16x and 32x.
- **Client (part `statues`):** 10 statues in the row, 5 close-up pairs, 16x and 32x. All looked at.

## Open for the owner

- The ring look: it's functional (items orbit at the beacon's top) but plain. Maybe slower, or a second ring for XP.
- Statue radius 3 and Warding statue not keeping sleepers asleep (builder).
- Herding counts every animal but tamed pets, so it includes bees, axolotls and the like.

## Audit fixes (separate commit)

What changed, for the spec's "As built" and test sections:

- **Wards:**
  - `blocksEntry` checks "any wards in this level?" before `WardBarrier.keptOut`.
  - The ward sweep now skips what `keptOut` skips, such as deactivated Fabricator mobs.
  - The nearest-face exit is computed once (`WardBarrier.exitNormal`); `WardFleeGoal.exitPoint` uses it.
- **Removed (dead):** `Wards.active`, `Wards.keepsSleepersAsleep`, `TotemAreas.contains`, `SeekingTypes.allBlocks`, `TotemStatueBlock.HAS_TYPES`.
  - `WardingBeaconGameTest` now checks sleeper sensitivity (0 under a 4-layer ward, 1 under a 1-layer ward, with Reworked Mobs).
- **Kept:** the client mixin config. It was empty, but now holds `ClientLevelPrecipitationMixin` (Clear Skies).
- **Loot in one place:** `WardingLoot` is gone; Warding's tables (`WardingTuning.LOOT`, the evoker included) are in `TotemsLoot.all()`. `TotemsGameTest.totemsAreFoundInLoot` rolls chest tables as chests and checks the evoker as entity loot (about 5 %).
- **Statues:** Warding and Seeking extend `StatueTotemItem`, so sneak-to-place lives in one place.
- **Vanilla selectors:** `ENTITY_STILL_ALIVE` (GraveTalisman, ward sweep), `LIVING_ENTITY_STILL_ALIVE.and(NO_SPECTATORS)` (Fortification), `withinDistance` (Warding pulse, Gathering pull).
- **Tests:**
  - Core is now 15, one per mechanic, including the 5 new mechanics of this branch.
  - Moved to milestone: oldReworkedMobsIdsLoadAsTheseItems (now in TotemsGameTest), endStatuePlaces…, sensesSleepers, pulseCalmsSleepers, fullWardKeepsSleepersAsleep, wardPushesBackMobsThatDoNotPath, growthPlantsDroppedSeeds.
  - Deleted: endFoundInEndCitiesAndSingleStack, glowLevelTable, WardingGameTest.totemIsFoundInLoot (merged), and the `warding_loot` environment.
  - `totemsAreArtifacts` checks every statue totem, the Beginning included, for one per stack and not common; the talisman for one per stack.
  - The statue placement tests no longer check drops; `everyStatueDropsItsTotem…` covers that.
- Full server suite: 44/44 pass, also with `-PwithBeacons -PwithMobs`. Core: 15/15.

### Design notes moved out of code comments (B38), for the spec if it lacks them

- **Gathering, held:**
  - Items aim at the holder's middle like XP orbs, slow down in the last block, and get a small lift against gravity so they glide instead of scraping.
  - Vanilla's orb pull was tried first (B17), but ground friction ate it.
  - XP is left to vanilla's own orb pull.
  - The holder's own throws come back only once their pickup delay ends; never-pickup items (displays) are skipped.
- **Fortification:** only block damage is stopped. The blast still hurts and knocks back, and fire from the blast doesn't start on protected blocks. Wind charges are untouched, and the mobGriefing rule is never changed (owner: it would stop villager farming). A dull clang and a glint of wax show where a blast was turned aside.
- **Loot:** additive pools on chest tables only. Archaeology tables give a single item, so a second pool would break brushing. A table a datapack replaced through Fabric is left alone.
- **Grave Talisman:** the undead search lives on plain `Level`, so server tests can check it. The client works out the glow for its own player.
- **Warding pulse:**
  - The soul-fire ring is drawn low, below a player's eyes, so it doesn't fill the first-person view.
  - Brain-driven monsters (piglins, hoglins) flee through their vanilla AVOID_TARGET memory; goal-driven ones get WardFleeGoal.
- **WardFleeGoal:** vanilla's AvoidEntityGoal can't be reused. It only flees from entities a monster could target, which leaves out creative players and other monsters holding a totem.
  - It's added on first scare at priority 0 with the MOVE flag.
  - Trapped in an enclosed ward, a mob stops trying to leave for a while; this fixed the "stuck running/jittering" bug.
