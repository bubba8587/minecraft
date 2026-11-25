<!-- dte:C1,B17,B18,B19,D2 -->
# Reworked Mobs: endermen (spec)

**Status: built.** Caretakers, cairns (seven forms), cairn spawning, the vigil, trading at cairns, sneak + middle-click placing, and the ripple and pulse for Totems & Artifacts. Open items at the end.

Endermen are the world's **caretakers and builders** (owner's design).

## Two jobs

1. **Repair:** they pick up and put back blocks so the land drifts back toward how it was first generated. The game can work out from the world seed what block originally generated at any position, so endermen know what "right" looks like without tracking every block anyone placed.
   - Example (owner): they deliberately jump on **farmland** and break it **back to dirt** if the original block there was dirt.
2. **Build:** they complete **enderman cairns**: strange structures whose full design is fixed when the world generates (deterministic templates), so endermen always know what to build. They bring in **End blocks** for them (end stone, purpur, etc.).
   - Endermen choose between the two jobs.
   - **Look (owner):** strange, twisty, **chorus-plant-like** structures rather than stacked stones. The original "chorus" look (purpur pillar trunks that step sideways as they climb, branches ending in end-rod "flowers", an end stone brick footing) is now one of seven forms ("Cairn forms" below).
   - **Cairns point to the stronghold** (owner): the tallest end rod tilts toward the nearest stronghold, so observant players can follow cairns there.

## The target world (owner)

- Endermen's **ideal world is fixed at world generation: natural terrain plus every cairn** (cairns decided at worldgen, offerings included), maybe plus a few other deliberate things later. It is computed from the seed, never stored. That keeps it cheap and deterministic: an enderman only ever compares one block (what's here) with another (what the target says).
- **How they work:** an enderman either **spawns already carrying a block** whose destination is known and delivers it, or **spawns empty, picks up a block that doesn't match the target, and carries it to a spot that needs that block**. If it finds no such spot, it **vanishes with the block when it despawns**.
- So there are no exemptions in the design: a cairn's offering flower is "correct", a stray flower elsewhere gets moved (but see builder's choice 1).

## Cairn look (owner ideas)

- **Overworld meets End:** End blocks with end-city touches (purpur stairs and slabs as branch knuckles, stained glass), plus **offerings** at the foot taken from what endermen carry in vanilla (`#minecraft:enderman_holdable`: small flowers, dirt, grass, moss, mud, sand, red sand, gravel, clay, mushrooms, pumpkins, melons, cactus and its flower, crimson/warped fungus, roots and nylium, TNT), varying by biome. Rare **TNT** offerings are allowed (owner: yes).
- **Variety (owner: "sure add as much variety as you like"):** shapes, sizes, branch layouts and more, as much as the builder likes, all deterministic from the seed.
- **No banners** (owner: skip them; the preview's banner looked bad).
- **Stronghold pointer:** only the tallest end rod points; other pointer ideas were dropped.

## Cairns as their home (D2: deliberate spawning)

- Endermen spawn near cairns instead of randomly. At a finished cairn they stand in a circle around it and stare at it (owner).
- Building is slow and speeds up as the cairn fills in: the more of it stands, the more endermen come to finish it (owner).
- **Left alone, they snowball:** more endermen spawn, faster, and they finish their work faster. Interfering (killing them, taking the cairns apart) slows or stops it.

## Owner answers

- **Water:** endermen keep their vanilla weakness to water.
- **E1 what they touch:** no record of placed blocks. Endermen cheaply sort each spot by **"does this block generate naturally"**:
  - natural block types (dirt, stone, grass, sand...) are freely reverted toward the original, **including air where ground used to be** (dig out an area and they slowly fill it back; they might eventually bury a house);
  - **unnatural** blocks (planks, glass, bricks, anything crafted) have only a **much smaller chance** of being touched. As built, builds are left alone except where they stand in a cairn's footprint ("Caretakers leave builds alone", below). **Confirmed (owner, 2026-11-24):** players can decorate and build around a cairn (stands for the endermen and such); only blocks literally in the cairn's way are cleared.
- **E2 protection, both:** cairn sites are fixed at worldgen and act as a **nexus**: a higher enderman spawn rate there, **significantly higher once the cairn is complete**. Killing endermen thins them but doesn't stop the flow from a nexus. **Wards definitely work**: a Totem of Warding area keeps endermen and their work out.
- **E3 shutting a nexus down:**
  - (a) **Tearing a cairn down** drops its site back to the lower, unfinished rate, and endermen start rebuilding it. It works in reverse too: a player who repairs a cairn they broke brings the higher rate back ("complete" = matches its template).
  - Cairns are made **only of blocks that don't generate naturally in the overworld** (end stone, end stone bricks, purpur, end rods...), so the repair job never treats a cairn as terrain. **Never obsidian or crying obsidian** (owner): both generate naturally, and a cairn would become a free obsidian source.
  - (b) A **ward over a site** stops it producing.
- **E4 hostility:** vanilla rules (eye contact angers them), plus they turn hostile if a player breaks their cairn or attacks one of them nearby. **An enderman busy on a job (repairing or building) ignores eye contact.**
- **Explosions** breaking a cairn don't anger endermen (owner: no). Only a player breaking it or attacking nearby does.
- **Nether and End endermen** stay vanilla for now.
- **No reward for completing a cairn** (owner): the Totem of the End's cairn completion gift was removed because it was farmable (break one block of a finished cairn and put it back). The End is found only in the End and in relic vaults. Totem of the Beginning offerings come from 0.30 of cairns.

## How it's built

**No new entity type, no new blocks (B15).**
- Every enderman created in the overworld becomes a caretaker through `mixin/EndermanMixin`: vanilla's `EndermanTakeBlockGoal` and `EndermanLeaveBlockGoal` are removed and `EndermanJobGoal` (priority 6) is added.
- Nether and End endermen (created in those levels) stay vanilla (*builder's choice*: the target world is overworld terrain). One that walks through a portal is a new entity there and vanilla; it becomes a caretaker again if it comes back.
- Water weakness, eye-contact anger, daylight teleporting, attacks and drops stay vanilla.
- Code: `enderman/` package; every number in `EndermanTuning`.

### The target world (`TargetWorld`, `TerrainOracle`, `TerrainKinds`)

What endermen think belongs at a block: a cairn's design where a cairn site covers it, else natural terrain as first generated. Never stored.
- **Natural terrain** is regenerated per chunk from the seed by running the world's own generator into a scratch `ProtoChunk`:
  - biomes (`fillBiomesFromNoise` with a caching resolver), then `NoiseBasedChunkGenerator.buildTerrain` (noise fill with aquifers, surface rules, carvers);
  - structures' terrain adaptation (the "beard" around villages and the like) is reproduced: the structure starts referenced by the real chunk are read on the main thread and handed to the generator;
  - superflat worlds use their layer column; other generators have no target (endermen leave terrain alone).
- Runs on one low-priority worker thread (`brm-target-world`) and never blocks the server: a lookup in a chunk that isn't ready yet answers "no opinion" and queues it. Per-level LRU cache of 96 chunks (~50-100 KB each).
- **Not reproducible:** features (trees, ores, flowers, lakes, geodes...) and structures. So the rules (E1) only revert terrain materials, and every structure piece's box (+2 blocks) is left alone.
- **Kinds of block (E1):**
  - terrain **families** are freely reverted: soil (dirt, grass, podzol, mycelium, coarse/rooted dirt, dirt path); stone (stone, cobblestone, mossy cobblestone, andesite, diorite, granite, tuff, calcite, smooth stone); deepslate and cobbled deepslate; sand and sandstone; red sand and red sandstone; gravel; clay; mud; snow block; terracotta;
  - other **natural** blocks (logs, leaves, flowers, ores, ice, pumpkins, melons, mushrooms, moss, bedrock, obsidian...) are never touched unless a nearby cairn wants exactly that block;
  - everything else is **unnatural** (planks, glass, bricks, End blocks...).
- **Liftable at all** (repair pickups): in the block tag `#bubbas_reworked_mobs:caretaker_liftable` (vanilla's `#enderman_holdable`, dirt, sand, overworld and Nether base stones, terracotta, nylium, snow, gravel, clay, cobblestone kinds, sandstone and red sandstone); no block entity (chests, sleepers, loot displays are safe), no fluid, breakable, not wither/dragon-immune. So of the unnatural blocks only a few in the tag (TNT, carved pumpkins, Nether stone, nylium) can be taken, at 5 % each time an enderman finds one where it doesn't belong.

### Caretakers leave builds alone (owner)

- Terrain mending lifts only natural terrain: the `caretaker_liftable` tag above, never anything with a block entity.
- **No beacon exception (owner, 2026-11-24):** beacon bases get no extra protection from endermen. Don't build near a cairn or they may take your stuff.
- Clearing a cairn's footprint keeps the broader rule (`TerrainKinds.clearable`: holdable or a full cube, never a block entity), so trees and player blocks in the cairn's way still go.

### Jobs (`EndermanJobGoal`, `EndermanWork`)

Off while angry, riding, or when `mobGriefing` is off (vanilla's rule for enderman blocks).
- **Empty-handed (repair):** every 20-30 ticks it checks every column within 5 blocks, then 16 random columns within 12. Each column is read from its real top (the motion-blocking heightmap, ignoring leaves; at most 8 above the original ground) down to one below the original ground, so end rods and flowers lying on the ground are seen. It takes:
  - terrain-family blocks where the target is air (piles, pillars, a dirt house);
  - a block of a different family on the top layer (sand where grass was; not next to water);
  - anything in the way of a cairn's design (grass where its footing goes, a tree in its way);
  - blocks a nearby unfinished cairn wants (a stray poppy for a poppy offering, a player's end stone; see "Builders set blocks down" for when);
  - liftable unnatural blocks near or above the original ground, with an open face, at 5 %.
  - Farmland where the original block is soil: it walks onto it and jumps until it breaks back to dirt (vanilla trampling, the owner's example).
- **Carrying:** it looks for a spot that needs its block:
  - a missing block of a cairn within 48 (lowest placeable first);
  - else a remembered hole within 32 that its block fills ("Fillers" below);
  - else a hole within 5 (all columns) or 16 (16 random columns): air above a sturdy floor, at most 12 below the original surface, whose original block is in the carried block's family. It puts the original block back (cobblestone fills a stone hole with stone; dirt fills a grass hole with grass);
  - nothing needs it: a cairn block is set down by its cairn ("Builders set blocks down", "Leftover blocks"); any other block it leaves with ("Leaving with a block").
- **Getting there:** it walks. Farther than 20 blocks, or no closer for 3 s, it teleports to a free standing spot within reach (4 blocks from its eyes), onto the cairn itself for high blocks. A job not done in 30 s is dropped (the block is kept).
- **Leaving by day (owner):** in daylight a caretaker under open sky vanishes in portal particles, on average after about 20 s (1 in 400 per tick, `DAY_LEAVE_ONE_IN`), carried block and all. Name-tagged, angry, ridden or leashed ones stay. Persistence alone does not keep one (`cairn builders` builders are persistent and still leave). Off on the game test server.
- **Despawning:** a caretaker carrying a block still despawns (vanilla keeps every block-carrying enderman forever), so a block it couldn't place vanishes with it.
- **Errands (owner: "should endermen occasionally pick up a new task? I think yes"):** an empty-handed caretaker with nothing to do near it (including one whose vigil just ended) has a 1-in-4 chance per look round to check round a random spot 12-40 blocks off for something to take or farmland to trample, and goes there if it finds any. It never roams without a job.
- **Where they work (owner):** return to nature is relative to the enderman: it judges blocks around wherever it stands, so one pushed off its cairn onto farmland or a path restores that. Nothing guarantees it notices anything: every column within 5 blocks is checked each time, farther ones (to 12) only by a few random samples.

### Cairn sites (`CairnSites`, `CairnSite`; E2, D2)

- Fixed from the seed like structure placement: the overworld is cut into 24 x 24-chunk regions (384 blocks). Each has one candidate spot at least 4 chunks from its edges (so sites are at least 128 blocks apart).
- The candidate is a site when the ground there is dry (above sea level, no water on it) and fairly flat (8 points 4 blocks out within 2 of the centre).
- Worked out on demand off the main thread and cached per region.
- In seed 20261116: 8 sites within 1500 blocks of spawn (about 1 per km²).
- **Hand-placed sites are saved** with the level: `cairn here` stores origin and seed in a persistent Fabric attachment on the `ServerLevel` (`bubbas_reworked_mobs:cairn_sites`), loaded the first time a level's sites are asked for. Without it a placed cairn would be forgotten on reload, and its blocks would become stray unnatural blocks. Game tests' `CairnSites.addSite` stays in memory only.

### Cairn designs (`CairnDesign`)

A pure function of the site seed, the biome at its centre and the stronghold direction. Seven forms ("Cairn forms" below); the chorus form is described here, and the parts shared by all forms are marked.
- **Footing:** end stone bricks (some end stone) sunk into the ground, radius 1 or 2; worn slabs (purpur or end stone brick) around the rim.
- **Chorus trunks:** 1-3 chorus-like trunks: purpur pillar runs of 2-3 that step sideways at purpur-block knuckles with upside-down purpur stairs. Main trunk 8-14 tall, others 40-75 % of it, staying within 2 of their start.
- **Chorus branches:** out of knuckles (60 % above the third block): an arm of 1-2 ending in a purpur block, rising 0-2, topped with an end rod "flower". One stained glass pane may hang under a branch.
- **The pointer (all forms):** the tallest end rod lies on its side at the very top, pointing toward the nearest stronghold (4 directions; from the seed's stronghold ring positions; seed-picked when the world has none). Nothing else is as tall.
- **Offerings (all forms):** 3-6, 2.6-4 blocks out (or beyond the form's reach), by biome:
  - temperate: flowers on moss, pumpkins, carved pumpkins facing out, mushrooms on podzol, clay;
  - flowery biomes: allium, lily of the valley, tulips...;
  - jungle: melons, orchids;
  - swamp: orchids on moss, mushrooms, clay, mud;
  - desert and beach: cactus on sand with cactus flowers, sand, gravel;
  - badlands: red sand, cactus;
  - snowy: gravel, clay, carved pumpkins, white flowers;
  - dark forest, taiga, pale garden: mushrooms, eyeblossoms, pumpkins;
  - mushroom fields: mushrooms on mycelium.
  - Flowers stand on their own moss or podzol block so they survive anywhere.
  - **TNT** replaces one offering in 16 % of cairns (34 % in deserts and badlands).
- **Blocks (all forms):** the structure is only End blocks that never generate in the overworld (no obsidian, E3). Offerings are vanilla holdable blocks by design (the target includes them, so a cairn's poppy is "correct").
- **The trade pad** ("Trading at cairns" below) is added last.

### What a site looks like before endermen arrive (owner: nothing unnatural a player could already notice)

- A worldgen feature (`CairnFootingFeature`, `bubbas_reworked_mobs:cairn_footing`, every overworld biome) places only the site's **natural offerings** when its chunk generates: flowers on moss, mushrooms on podzol, sand, clay or mud, cactus, melons, plain pumpkins (`CairnSite.isStubBlock`: enderman-holdable, natural, not TNT; carved pumpkins don't count as natural).
- No End blocks: endermen bring every one, footing first (empty-handed ones dig the grass out where the footing sinks in; builders set the end stone).
- So a site starts as a slightly-too-neat patch of nature, and the first unnatural sign is end stone appearing in the grass.
- Chunks generated before the mod was installed get no stub.

### Spawning (`EndermanSpawning`; D2, E2, E3)

- **Vanilla's random natural enderman spawns stay**, at the monster cap's low rate (owner: a small natural rate), and those endermen **keep vanilla behavior** (owner): random block pickup, no caretaking. `MobFinalizeSpawnMixin`: a NATURAL spawn the cairn spawner didn't mark drops its caretaker mind and gets vanilla's block goals back; saved as `bubbas_reworked_mobs:vanilla`.
- Caretakers come from cairn spawns, spawn eggs and commands. Endermen from spawners and trial spawners are vanilla (owner: "spawners should spawn vanilla mobs, for now"). Nether and End endermen are vanilla.
- **Spawn check:** every 5 s, each site with a player within 96 blocks and its centre loaded may spawn one enderman.
- **Pace (owner):** about 1-2 blocks a night for a while, so players are intrigued and have to invest to see what happens; once it's obvious the endermen are building something, it speeds up. So spawning follows the **cairn's growth** (`CairnSite.growth`: the share of its End blocks standing, footing included, offerings not; a fresh site is 0):
  - chance 4 % + 31 % x growth³ (4 % at a stub, ~8 % half up, ~27 % at 90 %); 35 % once the whole design stands (E2);
  - times (1 + 0.3 per enderman of its own already there): the snowball;
  - cap 2 + 6 x growth² (2 until about a third up: endermen linger all night, so the cap is what keeps early nights to a block or two); 8 when complete.
  - Modelled pace (`tools/cairn-pace.py`, a model of the rules): 30 End blocks take about 14 nights with a player nearby (1-2 blocks a night for the first week or so, 5-6 at the end), 45 about 22, 60 about 30. Clearing grass for the footing isn't in the model and slows the start a little more.
- **Spots:** 12-28 blocks from the centre (out of the end rods' light), within 6 blocks up or down of the site's ground, passing vanilla's rules (the biome lists endermen, spawn placement, darkness), never within 24 blocks of a player. So in practice at night.
- **Newcomers at an unfinished site**, in order:
  1. **Builders** (50 % rising to 90 % with growth): it arrives carrying the lowest missing block that no enderman in the territory already carries (`CairnSite.nextUnclaimed`: carried blocks are matched to missing spots of the same block, bottom up, so builders split the work instead of all bringing the same block). If the block can go in now it heads for it; if not, it sets it down near the cairn ("Builders set blocks down").
  2. **Fillers for a remembered hole** near the site ("Fillers" below).
  3. **Clearers:** an empty-handed newcomer **arrives knowing one thing in the cairn's way** (`CairnSite.nextObstacle`: the lowest design spot holding something clearable, such as grass or dirt where the footing sinks in, or a tree) and comes to clear it (owner: "if the cairn site calls for removal of dirt/trees/etc, then a block-less enderman can remove one").
  4. Otherwise, half the time a filler for a hole it finds near the site; else an empty-handed repairer. Repairers clear what's in the cairn's way and may fetch a loose block it wants (a flower, a pumpkin) if they happen to see one; they never dig ground out for an offering.
- **At the cap,** a site still calls one builder when every enderman there is a builder whose block can't go in, and the next block that could go in is carried by nobody (its holder died or despawned).
- **Complete site:** newcomers bring a remembered hole's block if one waits, else arrive empty-handed and stand vigil.
- **Rules:** `spawn_monsters` and Peaceful turn it off; `spawn_mobs` doesn't (the cairn rule stands in for it, so a world can keep vanilla's random spawns off and cairns on, as the test world does). Game rule `bubbas_reworked_mobs:cairn_endermen` (default true, "Overworld endermen spawn at cairns"): false stops cairn spawning. It doesn't switch the jobs off (`mobGriefing` does, like vanilla).

### Home cairn

- Every enderman a site spawns, and every builder `cairn builders` summons, remembers its site (saved, `bubbas_reworked_mobs:cairn_home`).
- A site's cap and snowball count only its own: endermen whose home it is, plus homeless caretakers whose nearest site it is. (Counting every enderman within 64 blocks made sites near each other count each other's endermen, so they all sat at their cap.)
- Claims on missing blocks still count every enderman near the site.
- A vigil is kept at its home cairn first.

### Builders set blocks down

- A builder whose block can't go in yet takes job `SET_DOWN` (counts as work, so eye contact doesn't anger it): it puts the block on the ground near the cairn and carries on.
- **Set-down spot:** a random angle at `radius + SET_DOWN_GAP_MIN..MAX` (2..4) from the origin, on the ground (heightmap top), within 4 blocks of the cairn origin's height. It must be air with a sturdy floor, the block must be able to survive there, the target world must say air there, it must not be a design block of any cairn or rest on one, and it must not be within 3 blocks of the trade pad. 12 tries per plan; if none fits it tries again next look round.
- **Picking it up again:** a block a nearby cairn wants, lying where the target world is air (above the original ground), is taken only when some unwarded cairn within `SITE_WORK_RADIUS` has a placeable spot for it (`EndermanWork.readyCairnBlock`). So a set-down block that doesn't fit yet is left lying. This also works for blocks endermen can't normally lift (end rods, slabs, glass).
- Endermen standing where a block has to go teleport a few blocks off so it can go in.
- `CairnSite.canPlace`: a design block can go in when any face neighbour or any of its 12 edge neighbours is solid (edge neighbours let diagonal arms be placed).
- **Leftover blocks (owner):** an enderman carrying one of a cairn's own blocks that no cairn wants any more (its cairn was finished another way, e.g. `cairn build` after `cairn builders`) sets it down by the nearest cairn within the work radius whose design uses that block, the same way.
- **Leaving with a block (owner, 2026-11-25):** any other block an enderman picked up and has nowhere to put (dirt from a cairn's footing with no hole for it, logs and leaves of a tree in a cairn's way, a player's planks): it teleports 16-32 blocks away with it (`LEAVE_DISTANCE_MIN/MAX`) and vanishes in portal particles 3-8 s later (`LEAVE_MIN_TICKS` + up to `LEAVE_SPREAD_TICKS`), block and all (job `LEAVE`, busy). Name-tagged, leashed and ridden ones keep holding it instead. So no stray piles by the cairn and no holders taking cap slots all night. Spot it jumps to may be outside ticking range; it then vanishes when its chunk ticks again (and no longer counts toward its cairn, 64 blocks). Test: `leftoverBlockLeavesWithIt` (milestone, environment `enderman_leftover`).

### Cairn blocks are left alone

- Judging a block inside a cairn's design, an enderman takes it only if it isn't the designed block. Otherwise a finished cairn's own offerings (moss, podzol, pumpkins, flowers: natural blocks endermen can carry) would look like rubbish to clear, and the cairn would never stay complete.
- Where hand-placed cairns' boxes overlap, the cairn with a design block at that spot answers what belongs there (`CairnSites.covering`).

### Fillers bring what's missing (owner: "more endermen to spawn naturally already having missing-from-natural-generation blocks, predetermined by what's missing, not just for cairns but for more of the surrounding area")

- **Finding holes** (`EndermanHoles`, up to 512 per level): ground missing within 48 blocks of a cairn site is found by comparing against natural generation (the target world). Each site with a player within 96 sweeps the chunks round it, nearest first, one chunk a second (about 45 chunks, so a full pass takes under a minute), and notes every block of air where natural ground was, down to the first real block.
  - So holes are found however they were made (digging, explosions, anything), and the list rebuilds itself after a reload.
  - A block a player breaks is also noted at once, as a head start.
  - A chunk whose target world is still being worked out is retried next second (up to 30 s).
- **While a hole waits near a site:**
  - a newcomer there arrives carrying that hole's original block and heads for it (at finished cairns too, before standing vigil; at unfinished ones after the builder roll);
  - the site's spawn chance is at least 35 %;
  - it may spawn up to 4 past its cap for them.
- Holes fill bottom up (an upper one waits for its floor).
- An enderman carrying a block it picked up also goes to a remembered hole within 32 blocks that its block fills before searching at random.

### Vigil (owner)

- An enderman with nothing to do near a complete cairn walks to its place in a ring around it: 3 blocks outside the footprint, at an angle fixed by its id so the ring spreads out (`vigilPlace`).
- It stands there staring at the cairn, its gaze drifting to a new point on it every 2-6 s (`VIGIL_GAZE_*`; owner: they looked frozen), for a minute at a time, then looks for work and most likely takes its place again.
- Newcomers at a complete cairn arrive empty-handed, so a finished cairn gathers a growing ring of silent watchers.
- A vigil isn't work: eye contact still angers them.
- If the cairn is damaged the vigil breaks up (and the site spawns builders again).
- The vigil ring stays (owner); builders no longer wait in a ring, they set blocks down.

### Ender pearls (owner)

Finished cairns are the overworld's pearl source: the vigil ring is there to be farmed. It is dangerous: hitting one near a cairn angers all within 24. The cap of 8 is how many stand at once, and a complete site refills at 35 % per 5-second check while a player is near, roughly 4 a minute at night while they're being killed. The owner keeps this refill rate.

### Particles on complete cairns (owner; `CairnGlow`)

- A complete cairn within 96 blocks of a player shimmers: every 4 ticks a few purple portal particles around two random blocks of it, and one end rod sparkle drifting up from its highest block.
- The sparkle is sent long-distance, so a finished cairn shows from far off at night.
- Completeness is re-checked every 2 s. Vanilla particles, no assets.

### Hostility (E4)

- Vanilla eye contact still angers an idle enderman. One busy on a job (repairing or building, not standing vigil) ignores it (`isBeingStaredBy` returns false).
- Breaking a block of a cairn that stood as designed, or hitting a caretaker within 64 blocks of a site, angers every caretaker within 24 blocks of it at that player (vanilla's anger timer).
- Creative and spectator players don't anger them this way. Explosions don't count.

### Wards (E2)

- Reworked Mobs API `api/Endermen` (`keepOut`, `release`, `isKeptOut`, `isCairnBlock`; like `api/Sleepers`, it lapses unless refreshed).
- Inside a kept-out area endermen judge nothing (no repair, no building there), and a site whose centre is inside spawns nothing.
- Bubba's Totems & Artifacts calls it for **every** ward (any pyramid) through its `compat/SleeperHook` (`keepEndermenOut` / `releaseEndermen`; `ReworkedMobsSleepers` calls the API and ignores a `LinkageError` from an older Reworked Mobs).
- The Totem of Warding's barrier also keeps endermen (monsters) from walking in.

### Commands (ops)

- `/bubbas_reworked_mobs cairn locate`: the nearest site within 1024, its progress and pointer.
- `cairn build`: puts up the nearest site's whole design, within 128.
- `cairn here [seed]`: adds a site at the player's feet, saved with the world, and builds it (for previews).
- `cairn builders`: one persistent enderman per missing block of the nearest site within 64 (at most 12, in build order), each carrying its block. A carried block is only delivered to a spot that wants exactly that block and touches something already standing, so builders carrying later blocks set them down until the lower ones are in. Natural builders follow the same rule one at a time (`nextUnclaimed`).

### Cost (measured in the cloud container; the owner's PC is faster)

- **Target world:** one regenerated chunk takes ~50 ms on the worker thread (biomes ~5, terrain ~45), once per chunk while it stays in the 96-chunk cache. Nothing on the main thread except gathering the chunk's structure starts. An enderman working one area keeps hitting the same few chunks.
- **Accuracy** against a real world (seed 20261116, 25 chunks around spawn, 24 blocks below to 2 above the surface): solid/air agree 94 % (the rest: trees and plants), same block 82 % (ores, granite/diorite blobs, grass plants, snow layers). **An untouched world gave endermen 0 blocks to take, 0 farmland and 0 holes in 6400 columns** (no false repairs).
- **Per enderman:** a job search every 20-30 ticks while idle: ~137 columns (all 121 within 5, 16 random), about 2 block reads each for untouched ground, plus a pass over nearby sites' designs (~40-110 reads each). Walking is vanilla pathfinding.
- **Per site:** every 5 s while a player is within 96: one entity box query (64 blocks), one pass over its design (~40-110 reads), and when it spawns up to 8 column probes of up to 13 reads.

## Cairn gifts and the ripple (built 2026-11-22, for Totems & Artifacts)

Reworked Mobs' half of the Totems of the Beginning and the End (`specs/totems-artifacts.md`). Totems & Artifacts reaches it only through `api/Endermen`; without that mod nothing here is used (no supplier: nothing given, nothing marked).

**API (`api/Endermen`; add, don't change):**
- `boolean restoreAround(ServerLevel, BlockPos centre, int radius, int ticksPerBlock)` → `enderman/Ripples`. False outside the overworld or with a generator the target world can't reproduce. The radius and speed are the caller's.
- `drawIn` / `stopDrawing`: kept for older callers, do nothing (the nexus spawner was removed 2026-11-25; nothing called it).
- `void setKin(Predicate<Player>)` → `enderman/Endermen.anger` skips kin (creative and spectator are skipped anyway).
- `void setCairnGifts(Supplier<ItemStack> offering)` → `enderman/CairnGifts`.
- `pulseAround(...)` and `revertEffect(...)`: see "The pulse" below.

**The ripple (`enderman/Ripples`).** Per column, nearest first as the ring passes:
1. top down, terrain-family blocks above the generated surface where the target is air are removed;
2. bottom up, air where the target is a family block is filled with the target (falling blocks only on a solid floor; never into a cell an entity's box touches, items and XP orbs excepted);
3. a family block of the wrong family at or one below the generated surface, not touching a fluid, becomes the target.
- **Protective blocks:** any of these anywhere in the column's height protects that column and every column within 2: anything not `TerrainKinds.isNatural`, any block entity, lava, cobblestone, mossy cobblestone, cobbled deepslate, smooth stone, dirt path, and geode blocks (amethyst block, budding amethyst, amethyst buds and clusters, smooth basalt).
- Structures and wards: the target world has no opinion there, so they're skipped. Columns not worked out yet wait (up to 600 ticks).
- Effects: reverse-portal particles on changed blocks (at most 48 a tick) and the block's place sound now and then. Updates with `Block.UPDATE_ALL`.
- `Ripples.dryRun` counts what a ripple would change without changing it (for a debug run).


**Cairn offerings (`enderman/CairnGifts`).**
- Saved level attachment `bubbas_reworked_mobs:cairn_gifts` (`offered`: a list of site origins; a `granted` list in old saves, from the removed completion gift, is ignored).
- Ticked every 100 ticks for sites within 96 of a player (not on the game test server).
- Needs: a supplier, the site picked by its seed (`OFFERING_SHARE`), not yet offered, its centre loaded and not warded, the cairn complete, and one of its own endermen within 16. Then 1 in 12 per check.
- The site is marked before the item is placed, as a Reworked Mobs loot display (`GROUND`, facing the cairn), with portal particles and the enderman teleport sound.
- Spot: 16 angles round the cairn starting at a seed-picked one, `radius() + 2` out, y from origin+3 down to origin-3, air on a floor that holds a display up, not on or above a design block.

**Numbers (in `EndermanTuning`):**
- offering share 0.30 of sites (`OFFERING_SHARE`, owner), checked every 100 ticks (`OFFERING_CHECK_TICKS`), 1 in 12 per check (`OFFERING_ONE_IN`), witness within 16 (`OFFERING_WITNESS_RANGE`), spot 2 outside the footprint (`OFFERING_GAP`);
- ripple columns wait up to 600 ticks for their terrain (`RIPPLE_PENDING_TICKS`), protection radius 2 (`RIPPLE_PROTECT_RADIUS`), at most 48 particles a tick (`RIPPLE_PARTICLES_PER_TICK`).

### The pulse (Totem of the Beginning)

The Totem of the Beginning is a reusable area pulse (`specs/totems-artifacts.md`). Reworked Mobs' side:
- **API:** `api/Endermen.pulseAround(level, centre, radius, ticksPerBlock, chance, oreChance, player)` and `revertEffect(level, pos)`; `restoreAround` is unchanged. Totems reaches them through `SleeperHook.pulseAround`. An older Totems against a newer Mobs (the old signature had a `recentTicks` parameter) hits the `LinkageError` fallback and does nothing.
- **Pulse rules** (`Ripples.pulse` / `Odds`): the same ripple and build protection as the full ripple, plus per change:
  - skip unbreakable blocks (destroy speed < 0);
  - skip where `level.mayInteract(player, pos)` fails (spawn protection);
  - then roll the chance (the ore chance when the generated block is in `c:ores`).
  - Ore targets are allowed only in pulses and only into air (never replacing a placed block).
  - Pile removal goes top down and stops at the first failed roll in a column, so piles shrink from the top; fills roll per block independently.
  - No memory of recent reverts (a hidden cooldown would break B19), so a block can revert again on the next pulse.
- **Revert effect:** `REVERSE_PORTAL` x6 + `SCULK_SOUL` x1 per changed block; `SCULK_CATALYST_BLOOM` at volume 0.35, pitch 0.5-0.6, at most once per `EndermanTuning.REVERT_SOUND_GAP_TICKS` (7) per level.
- **Beginning beacons and statues** don't work through endermen: Totems' `beginning/BeginningAreas` calls `pulseAround` directly every 15 s, with no player (no protection check).

## Cairn forms (built 2026-11-22, agent 2; owner: "sure add as much variety as you like")

Seven forms picked by seed: chorus, grove, spire, arch, canopy, henge, leaning (player page `Endermen.md`).

### How it's built

- `enderman/CairnDesign`: `Form` enum (CHORUS, GROVE, SPIRE, ARCH, CANOPY, HENGE, LEANING), picked by the design's first random roll with cumulative weights 26/14/14/12/12/11/11 %. `formOf(seed)` for tests and previews. Still a pure function of (seed, offerings, pointer, groundAt).
- Structural blocks besides the chorus set (none generate in the overworld; `TerrainKinds.isNatural` false): end stone brick stairs (spire footing), end stone brick walls (henge posts), magenta, purple and pink stained glass. Purpur slabs are also branches (canopy) and post caps (henge).
- `growTrunk` takes `wander` (how far a trunk may step from its start: 1 grove and henge, 2 chorus, 4 leaning) and an optional `lean` direction (tried first 70 % of the time).
- `placePointer(top, pointer)` is shared by every form: the rod on its side beside the top block facing the stronghold, else on top. Afterwards every other rod at or above the pointer's height is removed.
- `reach` (the structure's farthest block) sets how far out offerings go; if no offering spot is free, one is placed just beyond the reach on the first free side.
- Forms don't affect site placement, growth and completeness (complete = matches the design), offerings by biome, the TNT chance, the worldgen stub, or the offering gift.
- `cairn here` sites in saved worlds keep their seed but get the new forms' design. Blocks of an old design that the new one doesn't want count as stray.
- Seeds: the design uses `new LegacyRandomSource(seed)`, whose first roll barely changes between nearby seeds, so small hand-picked seeds tend to get the same form (11, 22, 33 and 44 are all canopies). Natural sites use full random seeds, so they're fine. For the test world, use large or mixed seeds to get variety. Left as is, because mixing the seed would redesign every existing cairn.

### Numbers (2000 designs, checked in `EndermanGameTest.cairnDesignsVaryAndUseOnlyEndBlocks`)

| Form | Share | End blocks, average | At most |
|---|---|---|---|
| Chorus | 25 % | 41 | 78 |
| Grove | 14 % | 60 | 99 |
| Spire | 15 % | 50 | 72 |
| Arch | 11 % | 56 | 88 |
| Canopy | 12 % | 82 | 106 |
| Henge | 11 % | 49 | 71 |
| Leaning | 12 % | 42 | 67 |

0.026 ms a design (worked out once per site, off the main thread). The spawn check's pass over the design grows with the block count (about 40-110 reads every 5 s per active site): negligible.

### Builder's choices (owner may change)

1. **Seven forms with these shares.** Chorus is the most common.
2. **Some cairns are bigger** (canopy about 82 End blocks on average, at most 106), so they take longer to finish: by the pace model (`tools/cairn-pace.py`, ~0.5 nights a block late on) a canopy takes roughly 40 nights against 14-30 for the smaller ones. Say if building time should be capped (e.g. shorter canopies).
3. **Hand-placed test-world cairns changed shape** with the forms (same seed, new design).
4. **Offering lines** point in a random direction, not toward the stronghold (the rod is the only pointer).

## Trading at cairns (owner, 2026-11-22; built)

### Design (owner)

- **Endermen trade at their cairns.** Each cairn has **an obvious trade spot** as part of its design, where a player leaves something and the endermen answer with something they've gathered.
- **Like piglin bartering:** the return is random from a table, **sometimes good and sometimes weird as hell**.
- **They take anything at all:** a way to get rid of junk and maybe get something better back, or not.
- **Pure random:** what the player gives doesn't shape what comes back.
- **The trade spot uses our loot-on-display:** the item lies right on the ground, the way the Totem of the Beginning offering does (`display/LootDisplays`, `DisplayPlacement.GROUND`; `specs/undead.md` "Loot on display"). Players otherwise can't place displays in survival (ops only), so this spot is the exception. The endermen's answer appears on the same spot.
- **Timing:** as soon as no player is nearby, an enderman comes, stares at the item for 2-4 s, takes it and vanishes (teleport particles and sound), then **returns shortly after** (4-10 s; owner: "return quicker") with the answer and leaves it on the pad.
- **Each cairn's pad sits in its own place** around it (owner).
- **Placing control:** **sneak + middle mouse button** lays the held item on the ground as a display; plain middle click stays vanilla's pick block. It works **anywhere** (owner), not just on trade pads, so survival players can set items down as decoration; picking one back up with an empty hand already works. Part of the shared loot-on-display system (`specs/undead.md`).

### Player-facing (as built; **(builder)** marks the builder's choices)

- **Every cairn has a trade pad** **(builder: design)**: a purpur block plinth one block above the ground, skirted by four end stone brick slabs, just outside the cairn's offerings.
  - The endermen build and repair it like the rest of the cairn.
  - Existing cairns get one too: their endermen add it, so a finished cairn counts as unfinished until the pad is in.
  - The direction and the distance (3-5 blocks past the cairn's reach) come from the cairn's seed.
- **Trading:**
  - The player lays any item on the plinth (sneak + middle click). Once no player is within 16 blocks, an enderman teleports in beside the pad and **stands staring at the item for 2-4 s**, then takes it: it vanishes with portal particles and the teleport sound.
  - **4-10 s later** an enderman teleports in beside the pad, holding its answer, and lays it on the plinth.
  - The answer stays until picked up; it is never taken back as a new trade.
  - No trading while the cairn is warded.
- **The answer table (builder)**, `data/bubbas_reworked_mobs/loot_table/gameplay/enderman_trade.json` (a vanilla barter table, datapack-editable), weights out of 133:
  - **Good (about 40 %):** ender pearls 1-3 (10), chorus fruit 2-6 (8), emeralds 1-3 (6), bottles o' enchanting 1-4 (6), end rods 1-2 (4), popped chorus fruit 1-4 (4), an enchanted book (3), a diamond (2), a golden apple (2), a name tag (2), a disc fragment (2), a nautilus shell (2), a shulker shell (1), an echo shard (1).
  - **Weird:** a poppy (8), dirt 1-4 (6), end stone 1-4 (5), a poisonous potato (5), a carved pumpkin (4), a dead bush (4), a bone (4), string 1-3 (4), and 3 each of a fermented spider eye, ink sacs, a glass bottle, a sea pickle, snowballs, a lily pad, a cactus, a brown mushroom, clay balls, feathers, rotten flesh and flint. Also a pufferfish (2), TNT (1) and a cake (1).
- **Sneak + middle click** with an item in hand, aiming at a block, lays one of it on the ground as a display, anywhere: on top of the block aimed at, or in front of a side face, wherever there's a floor.
  - Plain middle click is still vanilla's pick block.
  - Works in survival and adventure where the player may use items there (spawn protection and adventure rules respected), within block reach.
  - In creative it lays a copy and keeps the stack.

### How it's built

- **Pad:** `CairnDesign.buildTradePad`, run last with its own random stream, so cairns' forms and offerings don't depend on it.
  - The stream is `XoroshiroRandomSource(seed ^ PAD_SALT)`, which mixes the seed, so nearby small seeds (the test world's 11, 22, 33) still get pads in different places.
  - It starts at a random ring (3, 4 or 5 past the reach) and tries 16 angles from a random start, then the other rings, on flat ground (the centre and its four neighbours at the same height, within 1 of the origin's), clear of every design block.
  - The position comes out as `CairnDesign.designed(...)` → `Designed(blocks, tradeSpot)`; `CairnSite.tradeSpot()` returns it. `CairnDesign.design(...)` is unchanged for its other callers.
- **Trades:** `enderman/CairnTrades`.
  - Saved level attachment `bubbas_reworked_mobs:cairn_trades`: a map from site origin to the answer's due time, or "answered".
  - Ticked every 20 ticks for sites near players (`CairnSites.nearPlayers`), not on the game test server.
  - "No player near" is `getNearestPlayer(..., EntitySelector.NO_SPECTATORS)` within 16.
  - **The stare:** the first check that finds an item with nobody near spawns a "taker" enderman (`NaturalSpawns.create`, a cairn spawn homed on the site) on a spawn cell 2 out from the pad. It has no AI, carries the entity tag `bubbas_reworked_mobs.trade_taker`, and is turned with vanilla's `Entity.lookAt` to look at the item. The take time is saved in the level attachment `bubbas_reworked_mobs:cairn_trade_stares`. When the time comes, the taker is discarded with portal particles and the item is taken (`LootDisplays.takeForMob`). If the item is gone before then (a player took it back), the taker leaves and nothing is traded. With no spawn cell free, the item is taken at once.
  - **The answer:** a courier enderman (`NaturalSpawns.create`) on a spawn cell 2 out from the pad, marked as a cairn spawn with the site as home, holds the rolled item in its main hand and lays it with `HandPlacing.lay`, the same path players use (owner: "the enderman should place it using our place-item mod mechanism"). With no spawn cell free, the item is laid without a courier.
  - The roll is `LootContextParamSets.PIGLIN_BARTER` with the enderman as `THIS_ENTITY`; an empty roll gives a poppy.
- **Laying:** `display/HandPlacing`, shared by players and mobs.
  - `lay(level, placer, target, stack)` splits one item off, places a `GROUND` display facing the placer, plays the item-frame add sound and emits a `BLOCK_PLACE` game event.
  - `canLayAt`: air or still water, over a floor that holds a display.
  - Players: client mixin `LayItemPickMixin` on `Minecraft.pickBlockOrEntity` (sneaking, item in hand, a block aimed at) sends the C2S payload `bubbas_reworked_mobs:lay_item`. The server checks block reach, `mayInteract` and `mayUseItemAt`.
- **Tuning (`EndermanTuning`):** `TRADE_CHECK_TICKS` 20, `TRADE_QUIET_RANGE` 16, `TRADE_STARE_MIN_TICKS` 40, `TRADE_STARE_SPREAD_TICKS` 40 (a 2-4 s stare), `TRADE_RETURN_MIN_TICKS` 80, `TRADE_RETURN_SPREAD_TICKS` 120 (back in 4-10 s).
- **Pad moves:** when the pad's random stream changed, existing cairns' pads moved: their endermen build the new pad, and the old one stays as decoration.

## Numbers (all in `EndermanTuning`; builder's starting values, owner to playtest)

| | Value |
|---|---|
| Site regions / separation | 24 x 24 chunks (384 blocks) / 4 chunks from edges; about 1 in 6 regions passes the dry-flat test (8 sites within 1500 blocks of spawn on seed 20261116): roughly one cairn per 900 x 900 blocks, about 3x rarer than villages |
| Site ground | above sea level, dry, 8 points 4 out within 2 blocks |
| Main trunk height (chorus) | 8-14 |
| TNT offering | 16 % of cairns (34 % desert/badlands) |
| Job search | every 20-30 ticks; all columns within 5, + 16 random within 12 (pickups) / 16 within 16 (holes) |
| Pickup band | real top (max 8 above original ground) down to 1 below original ground |
| Hole depth | up to 12 below the original surface |
| Unnatural blocks | 5 % each time found (only those in `caretaker_liftable`) |
| Cairn work radius | 48 |
| Reach / teleport if farther / stuck | 4 / 20 blocks / 3 s |
| Job timeout | 30 s |
| Spawn check | every 5 s, player within 96 of the site |
| Spawn chance | 4 % + 31 % x growth³; 35 % complete; x (1 + 0.3 per own enderman present) |
| Cap | 2 + 6 x growth²; 8 complete (+1 builder when all present are builders whose blocks can't go in and the next block is unclaimed) |
| Counting radius | 64 blocks around a site |
| Vigil | ring 3 outside the footprint, 60 s at a time, gaze moves every 2-6 s |
| Set-down | 2 to 4 outside the footprint (`SET_DOWN_GAP_MIN`/`MAX`), within 4 of the origin's height, 3 clear of the trade pad, 12 tries per plan |
| Errand | 25 % per idle look round, 12-40 blocks off, only if there is work there |
| Leaves by day | 1 in 400 a tick under open sky (~20 s); name tags keep one |
| Fillers | broken ground within 48 of a site remembered (cap 512); while one waits: chance at least 35 %, up to 4 past the cap |
| Pace (model) | 30 End blocks ~14 nights, 45 ~22, 60 ~30, with a player nearby; 1-2 blocks a night for the first week or so |
| Spawn ring | 12-28 blocks out, within 6 up/down of site ground, no player within 24 |
| Builders among newcomers (unfinished) | 50 % rising to 90 % with growth |
| Anger radius | 24 |
| Glow | within 96 of a player, every 4 ticks, completeness re-checked every 2 s |
| Terrain cache | 96 chunks per level; structure margin 2 |

## Builder's choices (the owner may want to change these)

1. **Stray flowers and other natural features are not moved unless a cairn wants them.** The design says "a stray flower elsewhere gets moved", but with no record of placed blocks every natural flower, pumpkin and mushroom would count as stray (features can't be reproduced), and endermen would strip meadows. So natural non-terrain blocks are only fetched for a nearby unfinished cairn that wants exactly that block (stray flowers do get carried to offering spots). Say if you want more.
2. **The worldgen stub is natural offerings only** (owner: nothing unnatural a player could notice).
3. **No banners** on cairns (owner); stained glass is the only end-city decoration besides stairs and slabs.
4. **The stronghold pointer is one of four directions** (the rod lies flat facing N/E/S/W).
5. **Overworld only.** Nether and End endermen stay vanilla.
6. **The game rule switches the spawning only**, like `watcher_creepers`; the jobs follow `mobGriefing`.
7. **Snowballing is stateless:** "more endermen about" raises the rate, so nothing is saved per site. Killing them slows it; it never stops a nexus (E2).
8. **Holes are only refilled within 12 blocks of the original surface,** and only from the bottom up (so mines, deep pits and caves are left; open pits fill slowly). Water and lava are never put back.
9. Endermen also clear natural blocks out of the cairn's way (grass under the footing, a tree grown into its spot).
10. Unnatural blocks are only touched near or above the original ground with an open face, never in mines, and only the few in `caretaker_liftable`.
11. Breaking a cairn block in creative doesn't anger them (easier for the owner to build and test).
12. Endermen are not saved with a job: after a reload each picks one again (vanilla saves the carried block).
13. **Builders are a slow End-block source** (vanilla drops a carried block on death): killing the builders of an unfinished cairn gives purpur, end stone and end rods before the End. Left as is; say if it should be curbed (e.g. builders' blocks vanish on death).

Cairn forms have their own list (above).

## Test world station

"Cairns and endermen" (`specs/testworld.md`, Layout): three finished cairns from fixed seeds, a yard with an unfinished cairn and its `cairn builders` builders, and a repair corner. In the test world the builders finished the trunk within 40 s.

## Milestone test objectives

**Core** (every build, `ReworkedMobsCoreGameTest`):
- `endermanBuilderDeliversCairnBlock`: a builder carrying a missing block completes the cairn.
- `cairnTradePadTakesAndAnswers`: a built cairn has a pad (purpur under the spot, the spot not a design block). An item on it brings one enderman that looks at it and doesn't take it while it stares; it's taken once the stare is over and the enderman leaves. It isn't answered early, is answered once due, and the answer isn't taken back. An empty pad does nothing.

**Milestone, `EndermanGameTest`:**
- `terrainOracleAnswersFlatLayersAndCairns`: a regenerated superflat column equals the generator's; a cairn's design is the target inside its site.
- `cairnDesignsVaryAndUseOnlyEndBlocks`: 108 designs are deterministic per seed, with varied heights, sizes and trunk counts; only End blocks in the structure (including end stone brick stairs and walls and magenta, purple and pink glass), none natural, never obsidian; every cairn has offerings and rods; the tallest rod is the pointer, faces the stronghold and is the only one at that height; TNT present but rare. Then 2000 more designs: every form turns up, they average under 1 ms, and none has more than 120 End blocks.
- `endermanTakesStrayBlockAndFillsHole`: cobblestone on open ground is taken and fills a stone hole as stone.
- `busyEndermanIgnoresEyeContact`.
- `trampleFarmlandBackToDirt`.
- `breakingCairnAngersEndermen`: a floor block doesn't; a cairn block angers all around; hitting one near a site angers the others.
- `siteSpawnsBuildersUpToItsCap`: random natural spawns and spawner endermen and creepers stay vanilla, cairn spawns are caretakers; an unfinished site spawns exactly up to its cap (or one past it for a bottleneck builder), close to the site; most newcomers carry a missing block destined for its spot.
- `wardStopsSpawningAndWork`.
- `carryingCaretakerStillDespawns`.
- `outOfOrderBuildersFinishHalfBuiltCairn` (environment `enderman_relay`, night): builders arriving out of order at a half-built cairn set blocks down and others bring them in; the cairn finishes within a night with no builder idle. Setup: a 93-block cairn, seed 1234, radius 8, built to one above its origin, 49 builders every 2 s carrying the missing blocks top first. Finished about tick 2200, 30 to 36 set-downs, longest idle carry 15 ticks. It turns the cairn spawner rule off and force-loads the field's chunks while it runs, then restores both.
- The game test world is superflat, so live tests stand in a target for their field (`TargetWorld.overrideTerrain`), and endermen judge nothing elsewhere on the test server.

**Milestone, `EndermanTotemsGameTest`:**
- `rippleRestoresGroundButNotBuilds`.
- `offeringLaidOncePerCairn`.
- `pulseOddsTerrainOverOre`: ten pulses at 0.12/0.012 over a dug-out field with ore two below the surface. The first pulse fills 5-22 % of 243 terrain cells; after ten, terrain > 50 %, ore < 30 % and under a third of terrain; ore spots only ever air or ore.
- `pulseRepeatsAndBuilds`: chance 1: re-dug blocks come back on the next pulse; stone in an ore spot stays stone; a plank keeps its columns untouched.

**Milestone, `CairnTradeGameTest`:**
- `tradePadsSitInDifferentPlaces`: ten cairns, including the test world's seeds 11, 22, 33 and 44, all have pads at different offsets, and all seven cairn forms get a pad.
- `tradeTableAlwaysAnswers`: 400 rolls, never empty, more than 15 kinds.
- `heldItemLaysOnTheGround`: one at a time, not on top of a display, not in mid-air.

**Client parts:**
- `endermen` (`EndermenClientGameTest`, a default-preset world, seed 20261116): `cairn-1-stub` to `cairn-3-stub` (the three sites nearest spawn as generated) and `enderman-carrying` (an enderman carrying a purpur pillar).
- `cairnforms` (`CairnFormsClientGameTest`): one finished cairn of each form plus three more seeds on a flat world, from the north-west: `cairn-form-<form>-<seed>` and `cairn-forms-row`; prints each one's block counts.
- `trading` (`CairnTradeClientGameTest`, `-PclientTests=trading`, real key presses):
  - `trade-pad-cairn-a-above`, `trade-pad-cairn-b-above`: two cairns (seeds 2026 and 11) from above, each with its pad in its own spot;
  - `trade-pad`;
  - `trade-enderman-staring`: the enderman beside the pad, its head bowed toward the offered rotten flesh;
  - `trade-offered`: a survival player sneaks and middle-clicks rotten flesh onto the plinth, 5 → 4 in hand;
  - `trade-answered`: the courier enderman and its answer on the plinth;
  - `laid-on-grass`: an apple laid on grass, and a plain middle click lays nothing.

**Not yet tested (objectives):**
- Day leave: a caretaker under open sky at noon is gone within 60 s; a name-tagged one and one in the dark stay.
- Cairn spawning still runs with `spawn_mobs` false and stops with `spawn_monsters` false.
- A `cairn here` site is still a site after the level is saved and reloaded (the test world's verify step checks it with `cairn locate`); `cairn builders` builders complete a cut-down cairn.
- A high cairn block is reached by teleporting onto the cairn.
- A stray flower is fetched for an offering.
- A liftable unnatural block is taken only sometimes; planks and glass never.
- A torn-down cairn drops back to the unfinished rate and a repaired one returns to the high rate (spawning numbers checked as functions only).
- The vigil ring forms around a complete cairn and breaks up when it's damaged (checked in the test world tour, `cairns_vigil`).
- The footing feature places only natural offerings, no End blocks.
- Endermen keep out of a ward placed by Totems & Artifacts (needs both mods; `WardingGameTest` could call `api/Endermen.isKeptOut`).

## Open

- Owner to playtest the numbers (all in `EndermanTuning`), the cairn pace and the vigil refill.
- Owner to review the builder's choices (1-13, cairn forms 1-4); in particular whether builders' carried End blocks should vanish on death (choice 13) and whether canopy cairns are too slow to finish (forms choice 2).
- Trading at cairns (builder's choices to review): the answer table, the pad's look (purpur plinth with end stone brick slabs), the 2-4 s stare and the 4-10 s return, the 16-block "nobody near" range, and existing finished cairns counting as unfinished until their pad is built.
- **E1 versus "leave builds alone":** the owner's E1 answer says unnatural blocks (planks, glass, bricks) are touched at a small chance. Since caretakers were made to leave builds alone, they never take planks, glass or bricks outside a cairn's footprint; only the few unnatural blocks in `caretaker_liftable` (TNT, carved pumpkins, Nether stone, nylium) get the 5 % chance. Owner to confirm this is the intended end state.
