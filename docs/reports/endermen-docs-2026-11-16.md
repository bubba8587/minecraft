<!-- dte:C1,D2 -->
# Docs report: Reworked Mobs endermen, first build (2026-11-16)

Builder's report for merging into `specs/endermen.md` (and `specs/totems-artifacts.md`, `specs/quality.md`, `CLAUDE.md`, the player vault). Nothing here is in the live docs yet. Design: `specs/endermen.md` (owner answers E1-E4 followed as written).

## Spec facts (as built)

**No new entity type, no new blocks (B15).** Every enderman created in the overworld becomes a caretaker through `mixin/EndermanMixin`: vanilla's `EndermanTakeBlockGoal` and `EndermanLeaveBlockGoal` are removed and `EndermanJobGoal` (priority 6) added. Nether and End endermen (made in those levels) stay vanilla (*builder's choice*: the target world is overworld terrain; one that walks through a portal is a new entity there and vanilla, and becomes a caretaker again if it comes back). Water weakness, eye-contact anger, daylight teleporting, attacks and drops stay vanilla. Code: `enderman/` package; every number in `EndermanTuning`.

**The target world (`TargetWorld`, `TerrainOracle`, `TerrainKinds`).** What endermen think belongs at a block: a cairn's design where a cairn site covers it, else natural terrain as first generated. Never stored.
- *Natural terrain* is regenerated per chunk from the seed by running the world's own generator into a scratch `ProtoChunk`: biomes (`fillBiomesFromNoise` with a caching resolver), then `NoiseBasedChunkGenerator.buildTerrain` (noise fill with aquifers, surface rules, carvers). Structures' terrain adaptation (the "beard" around villages and the like) is reproduced: the structure starts referenced by the real chunk are read on the main thread and handed to the generator. Superflat worlds use their layer column; other generators have no target (endermen leave terrain alone).
- Runs on one low-priority worker thread (`brm-target-world`), never blocks the server: a lookup in a chunk not ready yet answers "no opinion" and queues it. Per-level LRU cache of 96 chunks (~50-100 KB each).
- *Not reproducible:* features (trees, ores, flowers, lakes, geodes...) and structures. So the rules (E1) only revert terrain materials, and every structure piece's box (+2 blocks) is left alone.
- *Kinds of block (E1):* terrain **families** (soil: dirt, grass, podzol, mycelium, coarse/rooted dirt, dirt path; stone: stone, cobblestone, mossy cobblestone, andesite, diorite, granite, tuff, calcite, smooth stone; deepslate and cobbled deepslate; sand and sandstone; red sand and red sandstone; gravel; clay; mud; snow block; terracotta) are freely reverted; other **natural** blocks (logs, leaves, flowers, ores, ice, pumpkins, melons, mushrooms, moss, bedrock, obsidian...) are never touched unless a nearby cairn wants exactly that block; everything else is **unnatural** (planks, glass, bricks, End blocks...) and is taken only with a 5 % chance each time an enderman finds one where it doesn't belong.
- Liftable at all: no block entity (chests, sleepers, loot displays are safe), breakable, not wither/dragon-immune, and vanilla's `#enderman_holdable` or a full cube (no doors, beds, rails, torches).

**Jobs (`EndermanJobGoal`, `EndermanWork`).** Off while angry, riding, or when `mobGriefing` is off (vanilla's rule for enderman blocks).
- *Empty-handed (repair):* every 20-30 ticks it checks every column within 3 blocks, then 8 random columns within 12, from its real top (under leaves; at most 8 above the original ground) down to one below the original ground. It takes: terrain-family blocks where the target is air (piles, pillars, a dirt house); a block of a different family on the top layer (sand where grass was; not next to water); anything in the way of a cairn's design (grass where its footing goes, a tree in its way); blocks a nearby unfinished cairn wants (a stray poppy for a poppy offering, a player's end stone); unnatural blocks above the original ground at 5 %. Farmland where the original block is soil: it walks onto it and jumps until it breaks back to dirt (vanilla trampling, owner's example).
- *Carrying:* it looks for a spot that needs its block: a missing block of a cairn within 48 (lowest placeable first), else a hole within 3 (all columns) or 16 (16 random columns): air above a sturdy floor, at most 12 below the original surface, whose original block is the carried block's family. It puts the original block back (cobblestone fills a stone hole with stone; dirt fills a grass hole with grass). Nothing needs it: it keeps it and wanders.
- *Getting there:* walks; farther than 20 blocks or no closer for 3 s, it teleports to a free standing spot within reach (4 blocks from its eyes), on the cairn itself for high blocks. A job not done in 30 s is dropped (the block is kept).
- *Despawning:* a caretaker carrying a block still despawns (vanilla keeps every block-carrying enderman forever), so a block it couldn't place vanishes with it.
- *Perching:* idle near a complete cairn, 8 % of its job searches send it to stand on top of the cairn for 15 s ("they stand on top of completed ones").

**Cairn sites (`CairnSites`, `CairnSite`; E2, D2).** Fixed from the seed like structure placement: the overworld is cut into 24 x 24-chunk regions (384 blocks); each has one candidate spot at least 4 chunks from its edges (sites at least 128 blocks apart), and the candidate is a site when the ground there is dry (above sea level, no water on it) and fairly flat (8 points 4 blocks out within 2 of the centre). Worked out on demand off the main thread and cached per region. In seed 20261116, 8 sites within 1500 blocks of spawn (about 1 per km^2).

**Cairn designs (`CairnDesign`).** A pure function of the site seed, the biome at its centre and the stronghold direction; 40-60 blocks:
- footing of end stone bricks (some end stone) sunk into the ground, radius 1 or 2; worn slabs (purpur or end stone brick) around the rim;
- 1-3 chorus-like trunks: purpur pillar runs of 2-3 that step sideways at purpur-block knuckles with upside-down purpur stairs, main trunk 8-14 tall, others 40-75 % of it, staying within 2 of their start;
- branches out of knuckles (60 % above the third block): an arm of 1-2 ending in a purpur block, rising 0-2, topped with an end rod 'flower'; one magenta stained glass pane may hang under a branch;
- **the tallest end rod** lies on its side at the very top of the main trunk, pointing toward the nearest stronghold (4 directions; from the seed's stronghold ring positions; seed-picked when the world has none). Nothing else is as tall;
- 3-6 **offerings** 2.6-4 blocks out, by biome: temperate (flowers on moss, pumpkins, carved pumpkins facing out, mushrooms on podzol, clay), flowery biomes (allium, lily of the valley, tulips...), jungle (melons, orchids), swamp (orchids on moss, mushrooms, clay, mud), desert/beach (cactus on sand with cactus flowers, sand, gravel), badlands (red sand, cactus), snowy (gravel, clay, carved pumpkins, white flowers), dark forest/taiga/pale garden (mushrooms, eyeblossoms, pumpkins), mushroom fields (mushrooms on mycelium). Flowers stand on their own moss/podzol block so they survive anywhere. **TNT** replaces one offering in 16 % of cairns (34 % in deserts and badlands).
- Structure blocks are only End blocks that never generate in the overworld (no obsidian, E3); offerings are vanilla holdable blocks by design (the target includes them, so a cairn's poppy is 'correct').
- **No banners** (the preview's was bad, owner): left out for now.

**What a site looks like before endermen arrive (builder's choice): the footing stub.** A worldgen feature (`CairnFootingFeature`, `bubbas_reworked_mobs:cairn_footing`, added to every overworld biome at `surface_structures`) places the design's End blocks up to one block above the ground when the site's chunk generates: the footing, rim slabs and the first block or two of each trunk (14-28 of ~45 blocks in the test seed). Players can find sites; endermen have something to grow. Chunks generated before the mod was added get no stub. Costs one seed hash per chunk unless the site is in it.

**Spawning (`EndermanSpawning`; D2, E2, E3).**
- Vanilla's random natural enderman spawns are blocked in the overworld (`SpawnStateMixin`, same hook as creepers). Nether and End spawns, spawners and commands are untouched.
- Every 5 s, each site with a player within 96 blocks and its centre loaded may spawn one enderman: chance 12 % while its cairn is unfinished, 35 % once it matches its design (E2: significantly higher), times (1 + 0.3 per enderman already within 40 blocks) (the snowball: left alone, more come faster; killing them slows it). Cap: 3 at an unfinished site, 8 at a complete one. Tearing the cairn down drops it to the unfinished rate; repairing it brings the high rate back (E3, "complete" = every block matches).
- Spots: 12-28 blocks from the centre (out of the end rods' light), within 6 blocks up or down of the site's ground, passing vanilla's rules (the biome lists endermen, spawn placement, darkness), never within 24 blocks of a player. So in practice at night.
- Newcomers at an unfinished site: 65 % arrive **carrying one of its missing blocks** destined for its spot (builders); the rest half the time carry the original block for a hole near the site (fillers), else arrive empty-handed (repairers). Complete site: fillers or repairers.
- `spawn_mobs`, `spawn_monsters` and Peaceful turn it off. New game rule `bubbas_reworked_mobs:cairn_endermen` (default true, "Overworld endermen spawn at cairns"): false brings vanilla's random spawns back and stops ours. It doesn't switch the jobs off (`mobGriefing` does, like vanilla).

**Hostility (E4).** Vanilla eye contact still angers an idle enderman; one busy on a job (repairing or building, not perching) ignores it (`isBeingStaredBy` returns false). Breaking a block of a cairn that stood as designed, or hitting a caretaker within 40 blocks of a site, angers every caretaker within 24 blocks of it at that player (vanilla's anger timer). Creative and spectator players don't anger them this way. Explosions don't count.

**Wards (E2).** New Reworked Mobs API `api/Endermen` (`keepOut`, `release`, `isKeptOut`, `isCairnBlock`; like `api/Sleepers`, lapses unless refreshed). Inside a kept-out area endermen judge nothing (no repair, no building there) and a site whose centre is inside spawns nothing. Bubba's Totems & Artifacts calls it for **every** ward (any pyramid), through its `compat/SleeperHook` (new methods `keepEndermenOut` / `releaseEndermen`; `ReworkedMobsSleepers` calls the API and ignores a `LinkageError` from an older Reworked Mobs). The Totem of Warding's existing barrier already keeps endermen (monsters) from walking in.

**Commands (ops).** `/bubbas_reworked_mobs cairn locate` (nearest site within 1024, its progress and pointer), `cairn build` (puts up the nearest site's whole design, within 128), `cairn here [seed]` (adds a site at your feet and builds it; for previews).

**Cost (measured, this container; the owner's PC is faster).**
- Target world: one regenerated chunk takes ~50 ms on the worker thread (biomes ~5, terrain ~45), once per chunk while it stays in the 96-chunk cache; nothing on the main thread except gathering the chunk's structure starts. An enderman working one area keeps hitting the same few chunks.
- Accuracy against a real world (seed 20261116, 25 chunks around spawn, 24 blocks below to 2 above the surface): solid/air agree 94 % (the rest: trees and plants), same block 82 % (ores, granite/diorite blobs, grass plants, snow layers); **an untouched world gave endermen 0 blocks to take, 0 farmland and 0 holes in 6400 columns** (no false repairs).
- Per enderman: a job search every 20-30 ticks while idle: ~57 columns, about 2 block reads each for untouched ground, plus a pass over nearby sites' designs (~50 reads each). Walking is vanilla pathfinding.
- Per site: every 5 s while a player is within 96: one entity box query (40 blocks), one pass over its design (~50 reads), and when it spawns up to 8 column probes of up to 13 reads.

## Numbers (all in `EndermanTuning`; builder's starting values, owner to playtest)

| | Value |
|---|---|
| Site regions / separation | 24 x 24 chunks (384 blocks) / 4 chunks from edges |
| Site ground | above sea level, dry, 8 points 4 out within 2 blocks |
| Main trunk height | 8-14 |
| TNT offering | 16 % of cairns (34 % desert/badlands) |
| Job search | every 20-30 ticks; all columns within 3, + 8 random within 12 (pickups) / 16 within 16 (holes) |
| Pickup band | real top (max 8 above original ground) down to 1 below original ground |
| Hole depth | up to 12 below the original surface |
| Unnatural blocks | 5 % each time found |
| Cairn work radius | 48 |
| Reach / teleport if farther / stuck | 4 / 20 blocks / 3 s |
| Job timeout | 30 s |
| Perch | 8 % of idle searches near a complete cairn, 15 s |
| Spawn check | every 5 s, player within 96 of the site |
| Spawn chance | 12 % unfinished / 35 % complete, x (1 + 0.3 per enderman within 40) |
| Cap | 3 unfinished / 8 complete |
| Spawn ring | 12-28 blocks out, within 6 up/down of site ground, no player within 24 |
| Builders among newcomers (unfinished) | 65 % |
| Anger radius | 24 |
| Terrain cache | 96 chunks per level; structure margin 2 |

## Builder's choices (the owner may want to change these)

1. **Stray flowers and other natural features are not moved unless a cairn wants them.** The design says "a stray flower elsewhere gets moved", but with no record of placed blocks every natural flower, pumpkin and mushroom would count as stray (features can't be reproduced), and endermen would strip meadows. So natural non-terrain blocks are only fetched for a nearby unfinished cairn that wants exactly that block (stray flowers do get carried to offering spots). Say if you want more.
2. **The footing stub at worldgen** (footing, rim, first trunk blocks), so sites are findable before endermen come.
3. **No banners** on cairns for now (the preview's were bad); the magenta glass pane is the only end-city decoration besides stairs and slabs.
4. **The stronghold pointer is one of four directions** (the rod lies flat facing N/E/S/W).
5. **Overworld only.** Nether and End endermen stay vanilla.
6. **The game rule switches the spawning only**, like `watcher_creepers`; the jobs follow `mobGriefing`.
7. **Snowballing is stateless:** "more endermen about" raises the rate, so nothing is saved per site. Killing them slows it; it never stops a nexus (E2).
8. **Holes are only refilled within 12 blocks of the original surface,** and only from the bottom up (so mines, deep pits and caves are left; open pits fill slowly). Water and lava are never put back.
9. Endermen also clear natural blocks out of the cairn's way (grass under the footing, a tree grown into its spot).
10. Unnatural blocks are only touched at or above the original ground (house walls, paths), never in mines.
11. Breaking a cairn block in creative doesn't anger them (easier for the owner to build and test).
12. Endermen are not saved with a job: after a reload each picks one again (vanilla saves the carried block).

## Player-facing text (for the vault page)

> **Endermen** are the world's caretakers now. They mend the land: a pile of dirt you left, a pillar, a patch of farmland where grass used to be, all slowly go back to how the world first was, and the holes you dig slowly fill in again. What you build from planks, glass and stone bricks they mostly leave alone, but not always.
>
> They also build. Strange purple spires, **enderman cairns**, stand here and there across the overworld: twisting stalks of purpur with end-rod flowers, on a footing of end stone, with little offerings at their foot (flowers, pumpkins, mushrooms, cactus... and sometimes TNT). At first you'll only find a stub of end stone. Endermen arrive at night carrying End blocks and grow it. The top end rod of every cairn lies on its side: it points toward the nearest stronghold.
>
> Endermen no longer appear just anywhere in the overworld: they come from cairns. A finished cairn draws many more of them, and the longer one is left alone, the more come. Tear a cairn down and fewer come, until they rebuild it. A Totem of Warding keeps them away.
>
> They still hate being stared at, unless they're busy working. Break a cairn, or hit an enderman near one, and every enderman around turns on you.

## Milestone test objectives (for the spec)

Core (every build, `ReworkedMobsCoreGameTest`): `endermanTerrainOracleAnswersFlatLayersAndCairns` (regenerated superflat column equals the generator's; a cairn's design is the target inside its site); `endermanCairnDesignsVaryAndUseOnlyEndBlocks` (108 designs: deterministic per seed, varied heights/sizes/trunk counts, only End blocks in the structure, none natural, never obsidian, every cairn has offerings and rods, the tallest rod is the pointer and faces the stronghold, TNT present but rare); `endermanTakesStrayBlockAndFillsHole` (cobblestone on open ground is taken and fills a stone hole as stone); `endermanBuilderDeliversCairnBlock` (a builder carrying a missing block completes the cairn); `endermanBusyIgnoresEyeContact`.

`EndermanGameTest` (milestone): `trampleFarmlandBackToDirt`; `breakingCairnAngersEndermen` (a floor block doesn't; a cairn block angers all around; hitting one near a site angers the others); `siteSpawnsBuildersUpToItsCap` (vanilla overworld spawns blocked, the rule unblocks them; complete > unfinished chance, snowball; an unfinished site spawns exactly up to its cap; about two thirds of newcomers carry a missing block destined for its spot); `wardStopsSpawningAndWork`; `carryingCaretakerStillDespawns`. The game test world is superflat, so live tests stand in a target for their field (`TargetWorld.overrideTerrain`) and endermen judge nothing elsewhere on the test server.

`EndermenClientGameTest` (client part `endermen`, a default-preset world, seed 20261116): prints the oracle's accuracy and timing and the "untouched world" repair counts; screenshots of the three sites nearest spawn as generated (stub) and built (close, far, top), and an enderman carrying a purpur pillar.

Not yet tested (objectives): a high cairn block is reached by teleporting onto the cairn; a stray flower is fetched for an offering; an unnatural block is taken only sometimes; a torn-down cairn drops back to the unfinished rate and a repaired one returns to the high rate (spawning numbers checked as functions only); perching; the footing feature places only End blocks up to one above the ground; endermen keep out of a ward placed by Totems & Artifacts (needs both mods; `WardingGameTest` could call `api/Endermen.isKeptOut`).

## Other docs to touch

- `specs/endermen.md`: status "built (first pass)"; the sections above; owner questions answered by builder's choices.
- `specs/totems-artifacts.md` "Independence"/Wards: wards also keep Reworked Mobs' endermen out, through `SleeperHook.keepEndermenOut` / `releaseEndermen` and `api/Endermen`.
- `CLAUDE.md` / `ClientTests`: Reworked Mobs client test names now sleepers, displays, gait, creepers, endermen.
- `specs/quality.md` timings: full server suite now 82 tests (~30 s here); the `endermen` client part ~90 s (it generates a default world and visits three sites). Known flakes seen while building, **also on the untouched base commit**: `CreeperGameTest.oneWatcherSpawnsBehindCoverOutOfView` (about 1 in 4 runs here; probably the night environment's sky darkness not applied on the first tick, the same thing that made the new enderman spawn test flaky until it waited 5 ticks) and `GrabLootGameTest.idleSkeletonTakesADisplayedSword`. `SleeperCalmGameTest.strongLightCalmsSleepers` (core: "block light 0" after 600 ticks) failed in 2 of 5 core runs here and 2 of 6 on the base commit: pre-existing, not endermen (the light engine falls behind the fast test ticks?). Worth fixing soon: it is core, so it hits every build.
- Art: nothing new (vanilla blocks and enderman); no `docs/art-todo.md` rows.
- `BACKLOG.md` (Endermen line): banners; variety beyond this pass; Nether/End endermen; explosions breaking cairns.

## Test world station idea

A "cairn" station replacing the hand-built preview: `/bubbas_reworked_mobs cairn here <seed>` for three seeds side by side on grass, plus a dug pit and a farmland patch with a few caretaker endermen behind glass (night locked, `spawn_mobs false`), so the owner can watch them fill the pit and trample the farmland.
