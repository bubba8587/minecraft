<!-- dte:C1,B17,B18,B19,D2 -->
# Reworked Mobs: endermen (spec, first build done 2026-11-16)

Owner's design (2026-11-16), being worked out in conversation. Endermen are the world's **caretakers and builders**.

## Two jobs

1. **Repair:** they pick up and put back blocks so the land drifts back toward how it was first generated. The game can work out, from the world seed, what block originally generated at any position, so endermen know what "right" looks like without tracking every block anyone placed.
   - Example (owner): they intentionally jump on and break **farmland back to dirt** if the original block there was dirt.
2. **Build:** they complete **enderman cairns**: strange structures whose full design is fixed when the world generates (deterministic templates), so endermen always know what to build. They bring in **End blocks** for them (end stone, purpur, etc.).
   - Endermen choose between the two jobs.
   - **Look (owner):** strange, twisty, **chorus-plant-like** spires rather than stacked stones: purpur pillar trunks that step sideways as they climb, branches ending in end-rod 'flowers', on an end stone brick footing (preview in the test world east of the hub).
   - **Cairns point to the stronghold** (owner): a few blocks in each cairn's design, not too obvious (e.g. a leaning top stone, a line of end stone along the base), point toward the nearest stronghold. Observant players can follow cairns there.

## The target world (owner, 2026-11-16)

- Endermen's **ideal world state is fixed at world generation: natural terrain + every cairn** (cairns decided at worldgen, offerings included), maybe plus a few other deliberate things later. It's computed from the seed, never stored. Cheap and deterministic: an enderman only ever checks one block (what's here) against another (what the target says).
- **How they work:** an enderman either **spawns already carrying a block** whose final destination is known and delivers it, or **spawns empty, picks up a block that doesn't match the target, and carries it to a spot that needs that block**. If it can't find one, it **vanishes with the block when it despawns**.
- So there are no exemptions: a cairn's offering flower is 'correct', a stray flower elsewhere gets moved.

## Cairn look, details (owner ideas, 2026-11-16)

- **Overworld meets End:** the twisting purpur spire with end-city touches (purpur stairs/slabs as branch knuckles, magenta stained glass, a magenta banner), plus **offerings** at its foot from what endermen carry in vanilla (#minecraft:enderman_holdable: small flowers, dirt, grass, moss, mud, sand, red sand, gravel, clay, mushrooms, pumpkins, melons, cactus and its flower, crimson/warped fungus, roots and nylium, TNT), varying by biome. Rare **TNT** offerings are allowed (owner: yes).
- **Variety (owner, 2026-11-22: "sure add as much variety as you like"):** cairns must vary a lot: shapes, sizes, branch layouts and more, as much as the builder likes, all still deterministic from the seed. **No banners** (owner, 2026-11-22: skip them).
- **Stronghold pointer simplified:** the **tallest end rod tilts toward the nearest stronghold**; the other pointer bits are dropped.

## Cairns as their home (D2: deliberate spawning)

- Endermen spawn near cairns instead of spawning randomly. At a finished cairn they stand in a circle around it and stare at it (owner, 2026-11-16; earlier: stand on top of completed ones).
- Building is slow and speeds up as the cairn fills in: the more of it stands, the more endermen come to finish it (owner, 2026-11-16).
- **Left alone, they snowball:** more endermen spawn, faster, and they finish their work faster. Interfering (killing them, taking the cairns apart) slows or stops it.

## Owner answers

- **Water:** endermen keep their vanilla weakness to water (owner).

- **E1 what they touch:** no record of placed blocks. Endermen cheaply bucket each spot by **'does this block generate naturally'**: natural block types (dirt, stone, grass, sand...) are freely reverted toward the original, **including air where ground used to be** (dig out an area and they slowly fill it back; they might eventually bury your house); **unnatural** blocks (planks, glass, bricks, anything crafted) have only a **much smaller chance** of being touched.
- **E2 protection, both:** cairn sites are predetermined at worldgen and act as a **nexus**: a higher endermen spawn rate there, **significantly higher once the cairn is complete**. Killing endermen thins them but doesn't stop the flow from a nexus. **Wards definitely work**: a Totem of Warding area keeps endermen (and their work) out.
- **E3 shutting a nexus down:** (a) **tearing a cairn down** drops its site back to the lower, unfinished rate and endermen start rebuilding it; it works in reverse too: if a player repairs a cairn they broke, the higher spawn rate comes back ('complete' = matches its template). Cairns are made **only of blocks that don't generate naturally in the overworld** (end stone, end stone bricks, purpur, end rods...; **never obsidian or crying obsidian**: both generate naturally and a cairn would become a free obsidian source, owner), so the repair job never treats a cairn as terrain. (b) A **ward over a site** stops it producing.
- **E4 hostility:** vanilla rules (eye contact angers them), plus they turn hostile if you break their cairn or attack one nearby; **but an enderman busy on a job (repairing or building) ignores eye contact**.

## How it's built

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
- *Leaving by day (owner, 2026-11-16):* in daylight a caretaker under open sky vanishes in portal particles, on average after about 20 s (1 in 400 per tick, `DAY_LEAVE_ONE_IN`), carried block and all. Name-tagged, angry, ridden or leashed ones stay; persistence alone does not keep one (the `cairn builders` builders are persistent and were the ones that never left: audit 2026-11-16). Off on the game test server. Before this they stayed all day, teleporting out of the sun and walking back to their vigil.
- *Despawning:* a caretaker carrying a block still despawns (vanilla keeps every block-carrying enderman forever), so a block it couldn't place vanishes with it.

**Cairn sites (`CairnSites`, `CairnSite`; E2, D2).** Fixed from the seed like structure placement: the overworld is cut into 24 x 24-chunk regions (384 blocks); each has one candidate spot at least 4 chunks from its edges (sites at least 128 blocks apart), and the candidate is a site when the ground there is dry (above sea level, no water on it) and fairly flat (8 points 4 blocks out within 2 of the centre). Worked out on demand off the main thread and cached per region. In seed 20261116, 8 sites within 1500 blocks of spawn (about 1 per km^2).

**Cairn designs (`CairnDesign`).** A pure function of the site seed, the biome at its centre and the stronghold direction; 40-60 blocks:
- footing of end stone bricks (some end stone) sunk into the ground, radius 1 or 2; worn slabs (purpur or end stone brick) around the rim;
- 1-3 chorus-like trunks: purpur pillar runs of 2-3 that step sideways at purpur-block knuckles with upside-down purpur stairs, main trunk 8-14 tall, others 40-75 % of it, staying within 2 of their start;
- branches out of knuckles (60 % above the third block): an arm of 1-2 ending in a purpur block, rising 0-2, topped with an end rod 'flower'; one magenta stained glass pane may hang under a branch;
- **the tallest end rod** lies on its side at the very top of the main trunk, pointing toward the nearest stronghold (4 directions; from the seed's stronghold ring positions; seed-picked when the world has none). Nothing else is as tall;
- 3-6 **offerings** 2.6-4 blocks out, by biome: temperate (flowers on moss, pumpkins, carved pumpkins facing out, mushrooms on podzol, clay), flowery biomes (allium, lily of the valley, tulips...), jungle (melons, orchids), swamp (orchids on moss, mushrooms, clay, mud), desert/beach (cactus on sand with cactus flowers, sand, gravel), badlands (red sand, cactus), snowy (gravel, clay, carved pumpkins, white flowers), dark forest/taiga/pale garden (mushrooms, eyeblossoms, pumpkins), mushroom fields (mushrooms on mycelium). Flowers stand on their own moss/podzol block so they survive anywhere. **TNT** replaces one offering in 16 % of cairns (34 % in deserts and badlands).
- Structure blocks are only End blocks that never generate in the overworld (no obsidian, E3); offerings are vanilla holdable blocks by design (the target includes them, so a cairn's poppy is 'correct').
- **No banners** (the preview's was bad, owner): left out for now.

**What a site looks like before endermen arrive (owner, 2026-11-16: nothing unnatural a player could already notice).** A worldgen feature (`CairnFootingFeature`, `bubbas_reworked_mobs:cairn_footing`, every overworld biome) places only the site's **natural offerings** when its chunk generates: flowers on moss, mushrooms on podzol, sand, clay or mud, cactus, melons, plain pumpkins (`CairnSite.isStubBlock`: enderman-holdable, natural, not TNT; carved pumpkins don't count as natural). No End blocks: endermen bring every one, footing first (empty-handed ones dig the grass out where the footing sinks in, builders set the end stone). So a site starts as a slightly-too-neat patch of nature, and the first unnatural sign is end stone appearing in the grass. Chunks generated before the mod was installed get no stub. (The first build placed the footing and first trunk blocks at worldgen: a visible purple stump.)

**Spawning (`EndermanSpawning`; D2, E2, E3).**
- Vanilla's random natural enderman spawns **stay, at the monster cap's low rate** (owner, 2026-11-16: a small natural rate), and those endermen **keep vanilla behavior** (owner): random block pickup, no caretaking (`MobFinalizeSpawnMixin`: a NATURAL spawn the cairn spawner didn't mark drops its caretaker mind and gets vanilla's block goals back; saved as `bubbas_reworked_mobs:vanilla`). Caretakers are cairn spawns, spawn eggs and commands; endermen from spawners and trial spawners are vanilla too (owner: "spawners should spawn vanilla mobs, for now"). Nether and End endermen are vanilla. (The first build blocked vanilla overworld spawns entirely.)
- Every 5 s, each site with a player within 96 blocks and its centre loaded may spawn one enderman. Owner (2026-11-16): about 1-2 blocks a night for a while, so players are intrigued and have to invest to see what happens; once it's obvious the endermen are building something, it speeds up. So spawning follows the **cairn's growth** (`CairnSite.growth`: the share standing of its End blocks, footing included, not the offerings; a fresh site is 0): chance 4 % + 31 % x growth^3 (4 % at a stub, ~8 % half up, ~27 % at 90 %), 35 % once the whole design stands (E2), times (1 + 0.3 per enderman already within 64 blocks) (the snowball). Cap 2 + 6 x growth^2 (2 until about a third up: endermen linger all night, so the cap is what keeps early nights to a block or two), 8 when complete. Builders among newcomers 50 % rising to 90 % with growth. Pace (`tools/cairn-pace.py`, a model of the rules; cairns have about 30-60 End blocks): 30 take about 14 nights with a player nearby (1-2 blocks a night for the first week or so, 5-6 at the end), 45 about 22, 60 about 30. Clearing grass for the footing isn't in the model and slows the start a little more. The first rules (finished share of all blocks, 12 %/35 %) built a stub 53 % → 70 % → 89 % in two simulated nights (`cairnsim`), much too fast.
- Spots: 12-28 blocks from the centre (out of the end rods' light), within 6 blocks up or down of the site's ground, passing vanilla's rules (the biome lists endermen, spawn placement, darkness), never within 24 blocks of a player. So in practice at night.
- Newcomers at an unfinished site: half arrive **carrying a missing block** (builders): the lowest missing block that no enderman in the territory already carries (`CairnSite.nextUnclaimed`: carried blocks are matched to missing spots of the same block, bottom up, so builders split the work instead of all bringing the same block). If it can go in now they head for it; if not (the blocks under it are still missing) they **wait by the cairn** (within 8 blocks; one that wanders farther teleports back) until it can; the rest half the time carry the original block for a hole near the site (fillers), else arrive empty-handed. An empty-handed newcomer at an unfinished cairn **arrives knowing one thing in the cairn's way** (`CairnSite.nextObstacle`: the lowest design spot holding something liftable, such as grass or dirt where the footing sinks in, or a tree) and comes to clear it (owner: "if the cairn site calls for removal of dirt/trees/etc, then a block-less enderman can remove one"; without it a fresh site's footing waited on endermen happening to notice the grass, and a simulated first night built nothing). Otherwise they are repairers: they clear what's in the cairn's way, grass, dirt, trees, and may fetch a loose block it wants, a flower or a pumpkin, if they happen to see one; they never dig ground out for an offering). At the cap, a site still calls one builder when every enderman there is a builder whose block must wait and the next block that could go in is carried by nobody (its holder died or despawned). Complete site: empty-handed, they stand vigil.
- `spawn_monsters` and Peaceful turn it off; `spawn_mobs` doesn't (2026-11-16: the cairn rule stands in for it, so a world can keep vanilla's random spawns off and cairns on, as the test world does). Game rule `bubbas_reworked_mobs:cairn_endermen` (default true, "Overworld endermen spawn at cairns"): false stops cairn spawning. It doesn't switch the jobs off (`mobGriefing` does, like vanilla).

**Home cairn (audit 2026-11-16).** Every enderman a site spawns, and every builder `cairn builders` summons, remembers its site (saved, `bubbas_reworked_mobs:cairn_home`). A site's cap and snowball count only its own: endermen whose home it is, plus homeless caretakers whose nearest site it is. Counting every enderman within 64 blocks made sites near each other (the test world's row, 15 blocks apart) count each other's, so they all sat at their cap and none spawned. Claims on missing blocks still count every enderman near the site. A vigil is kept at its home cairn first.

**Waiting builders (audit 2026-11-16).** A builder whose block can't go in yet takes its place in the ring round its cairn (the vigil's place) and stands there facing it (job WAIT), checking every second whether its block can go in, and plans afresh every 20 s. Before, it had no job: vanilla's stroll walked it off and it teleported back, over and over.

**Cairn blocks are left alone (audit 2026-11-16).** Judging a block inside a cairn's design, an enderman takes it only if it isn't the designed block. Before, a finished cairn's own offerings (moss, podzol, pumpkins, flowers: natural blocks endermen can carry) looked like rubbish to clear, so they were taken and put back over and over and the cairn never stayed complete (no vigil). Where hand-placed cairns' boxes overlap, the cairn with a design block at that spot answers what belongs there (`CairnSites.covering`).

**Fillers bring what's missing (owner, 2026-11-16: "more endermen to spawn naturally already having missing-from-natural-generation blocks, predetermined by what's missing, not just for cairns but for more of the surrounding area").** Ground missing within 48 blocks of a cairn site is found by comparing against natural generation (the target world): each site with a player within 96 sweeps the chunks round it, nearest first, one chunk a second (about 45 chunks, so a full pass takes under a minute), and notes every block of air where natural ground was, down to the first real block (`EndermanHoles`, up to 512 per level). So holes are found however they were made (digging, explosions, anything) and the list rebuilds itself after a reload; a block a player breaks is also noted at once as a head start. A chunk whose target world is still being worked out is retried next second (up to 30 s). Owner, 2026-11-16: "is the system not just comparing against natural generation?" (the first version only noted player breaks). While one waits near a site: a newcomer there arrives carrying that hole's original block and heads for it (finished cairns too, before standing vigil; at unfinished ones after the builder roll); the site's spawn chance is at least 35 %; and it may spawn up to 4 past its cap for them. Holes fill bottom up (an upper one waits for its floor). An enderman carrying a block it picked up also goes to a remembered hole within 32 blocks that its block fills before searching at random.

**Errands (owner, 2026-11-16: "should endermen occasionally pick up a new task? I think yes").** An empty-handed caretaker with nothing to do near it (including one whose vigil just ended) has a 1-in-4 chance per look round to check round a random spot 12-40 blocks off for something to take or farmland to trample, and goes there if it finds any. It never roams without a job.

**Vigil (owner, 2026-11-16; replaces the earlier "perch on top").** An enderman with nothing to do near a complete cairn walks to its place in a ring around it (3 blocks outside the footprint, at an angle fixed by its id so the ring spreads out) and stands there staring at the cairn's middle, for a minute at a time, then looks for work and most likely takes its place again. Newcomers at a complete cairn arrive empty-handed, so a finished cairn gathers a growing ring of silent watchers. A vigil isn't work: eye contact still angers them. If the cairn is damaged the vigil breaks up (and the site spawns builders again).

**Ender pearls (owner, 2026-11-16).** Finished cairns are the overworld's pearl source: the vigil ring is there to be farmed (dangerous: hitting one near a cairn angers all within 24). As built, the cap of 8 is how many stand at once and a complete site refills at 35 % per 5-second check while a player is near, roughly 4 a minute at night when they're being killed; the owner keeps it: the vigil refills (2026-11-16).

**Particles on complete cairns (owner, 2026-11-16; `CairnGlow`).** A complete cairn within 96 blocks of a player shimmers: every 4 ticks a few purple portal particles around two random blocks of it, and one end rod sparkle drifting up from its highest block (sent long-distance, so a finished cairn shows from far off at night). Completeness is re-checked every 2 s. Vanilla particles, no assets.

**Where they work (owner, 2026-11-16).** Return to nature is relative to the enderman: it judges blocks around wherever it stands, so one pushed off its cairn onto farmland or a path restores that. Nothing guarantees it notices anything: every column within 5 blocks is checked each time, farther ones (to 12) only by a few random samples. (Roaming hops around a cairn's territory were tried the same day and dropped for this model.)

**Hostility (E4).** Vanilla eye contact still angers an idle enderman; one busy on a job (repairing or building, not standing vigil) ignores it (`isBeingStaredBy` returns false). Breaking a block of a cairn that stood as designed, or hitting a caretaker within 64 blocks of a site, angers every caretaker within 24 blocks of it at that player (vanilla's anger timer). Creative and spectator players don't anger them this way. Explosions don't count.

**Wards (E2).** New Reworked Mobs API `api/Endermen` (`keepOut`, `release`, `isKeptOut`, `isCairnBlock`; like `api/Sleepers`, lapses unless refreshed). Inside a kept-out area endermen judge nothing (no repair, no building there) and a site whose centre is inside spawns nothing. Bubba's Totems & Artifacts calls it for **every** ward (any pyramid), through its `compat/SleeperHook` (new methods `keepEndermenOut` / `releaseEndermen`; `ReworkedMobsSleepers` calls the API and ignores a `LinkageError` from an older Reworked Mobs). The Totem of Warding's existing barrier already keeps endermen (monsters) from walking in.

**Commands (ops).** `/bubbas_reworked_mobs cairn locate` (nearest site within 1024, its progress and pointer), `cairn build` (puts up the nearest site's whole design, within 128), `cairn here [seed]` (adds a site at your feet, saved with the world, and builds it; for previews), `cairn builders` (see Test world station).

**Cost (measured, this container; the owner's PC is faster).**
- Target world: one regenerated chunk takes ~50 ms on the worker thread (biomes ~5, terrain ~45), once per chunk while it stays in the 96-chunk cache; nothing on the main thread except gathering the chunk's structure starts. An enderman working one area keeps hitting the same few chunks.
- Accuracy against a real world (seed 20261116, 25 chunks around spawn, 24 blocks below to 2 above the surface): solid/air agree 94 % (the rest: trees and plants), same block 82 % (ores, granite/diorite blobs, grass plants, snow layers); **an untouched world gave endermen 0 blocks to take, 0 farmland and 0 holes in 6400 columns** (no false repairs).
- Per enderman: a job search every 20-30 ticks while idle: ~137 columns (all 121 within 5, 16 random), about 2 block reads each for untouched ground, plus a pass over nearby sites' designs (~50 reads each). Walking is vanilla pathfinding.
- Per site: every 5 s while a player is within 96: one entity box query (64 blocks), one pass over its design (~50 reads), and when it spawns up to 8 column probes of up to 13 reads.

## Cairn gifts, the ripple and the nexus (built 2026-11-22, for Totems & Artifacts)

Reworked Mobs' half of the Totems of the Beginning and the End (`specs/totems-artifacts.md`). Totems & Artifacts reaches it only through `api/Endermen`; without that mod nothing here is used (no supplier: nothing given, nothing marked).

New in `api/Endermen` (add, don't change):
- `boolean restoreAround(ServerLevel, BlockPos centre, int radius, int ticksPerBlock)` → `enderman/Ripples`. False outside the overworld or with a generator the target world can't reproduce.
- `void drawIn(ServerLevel, Object source, AABB, int lifetimeTicks)` / `stopDrawing(ServerLevel, Object source)` → `enderman/EndermanNexus` (lapses unless refreshed, like `EndermanWards`).
- `void setKin(Predicate<Player>)` → `enderman/Endermen.anger` skips kin (creative and spectator were already skipped).
- `void setCairnGifts(Supplier<ItemStack> offering, Supplier<ItemStack> completionGift)` → `enderman/CairnGifts`.

`enderman/Ripples`: per column, nearest first as the ring passes: (1) top down, terrain-family blocks above the generated surface where the target is air are removed; (2) bottom up, air where the target is a family block is filled with the target (falling blocks only on a solid floor; never into a cell an entity's box touches, items and XP orbs excepted); a family block of the wrong family at or one below the generated surface, not touching a fluid, becomes the target. Protective blocks (any of them anywhere in the cylinder's height protects its column and every column within 2): anything not `TerrainKinds.isNatural`, any block entity, lava, cobblestone, mossy cobblestone, cobbled deepslate, smooth stone, dirt path, and geode blocks (amethyst block, budding amethyst, amethyst buds and clusters, smooth basalt). Structures and wards: the target world has no opinion, so they're skipped. Columns not worked out yet wait. Effects: reverse-portal particles on changed blocks (at most 48 a tick) and the block's place sound now and then. Updates with `Block.UPDATE_ALL`.

`enderman/EndermanNexus`: per area, every `SPAWN_INTERVAL` (5 s) in the overworld, when cairn spawning is allowed (game rule, spawn_monsters, not Peaceful): below the cap, 35 % to spawn a caretaker (marked as a cairn spawn, no home) at a random column inside the area, on a spawn cell (vanilla spawn placement and darkness rules, not warded, not within 16 of a player). It arrives carrying the original block of a hole it finds in the area (16 random columns, 12 deep, like a cairn filler), else empty-handed.

`enderman/CairnGifts`: saved level attachment `bubbas_reworked_mobs:cairn_gifts` (`offered` list of site origins; `granted` is left over from the removed completion gift and only read from old saves). Offering: ticked every 100 ticks for sites within 96 of a player (not on the game test server); needs a supplier, the site picked by its seed, not offered, its centre loaded and not warded, the cairn complete, and one of its own endermen within 16. The site is marked before the item is placed, as a Reworked Mobs loot display (`GROUND`, facing the cairn), with portal particles and the enderman teleport sound. Spot: 16 angles round the cairn starting at a seed-picked one, `radius() + 2` out, y from origin+3 down to origin-3, air on a floor that holds a display up and not on or above a design block. Completion gift: `mixin/BlockItemPlaceMixin` (RETURN of `BlockItem.placeBlock`, success only, server player) calls `CairnGifts.placedBy`: the placed block is a design block standing as designed, the site is complete and not marked; then mark, `placeItemBackInInventory`, portal particles and `END_PORTAL_FRAME_FILL`. Nothing is marked while no supplier is set.

Numbers (in `EndermanTuning`): offering share 0.15 of sites (`OFFERING_SHARE`; owner, 2026-11-22: nerfed from 1 in 2), checked every 100 ticks (`OFFERING_CHECK_TICKS`), 1 in 12 per check (`OFFERING_ONE_IN`), witness within 16 (`OFFERING_WITNESS_RANGE`), spot 2 outside the footprint (`OFFERING_GAP`); nexus 35 % per spawn check (`NEXUS_SPAWN_CHANCE`), cap 6 (`NEXUS_CAP`) counted within the area + 8 (`NEXUS_MARGIN`), not within 16 of a player (`NEXUS_PLAYER_DISTANCE`), 8 tries (`NEXUS_TRIES`); ripple columns wait up to 600 ticks for their terrain (`RIPPLE_PENDING_TICKS`), protection radius 2 (`RIPPLE_PROTECT_RADIUS`), at most 48 particles a tick (`RIPPLE_PARTICLES_PER_TICK`). The ripple's radius and speed are the caller's (Totems: 24, 2 ticks a block).

Tests: core `endermanRippleRestoresGroundButNotBuilds`; milestone `EndermanTotemsGameTest` (`offeringLaidOncePerCairn`); client part `endermen` prints a ripple dry run (`Ripples.dryRun`) at spawn and beside the three nearest cairns: 0 changes on untouched ground, and exactly 30 for the control hole and pillar.

## Numbers (all in `EndermanTuning`; builder's starting values, owner to playtest)

| | Value |
|---|---|
| Site regions / separation | 24 x 24 chunks (384 blocks) / 4 chunks from edges; about 1 in 6 regions passes the dry-flat test (8 sites within 1500 blocks of spawn on seed 20261116): roughly one cairn per 900 x 900 blocks, about 3x rarer than villages |
| Site ground | above sea level, dry, 8 points 4 out within 2 blocks |
| Main trunk height | 8-14 |
| TNT offering | 16 % of cairns (34 % desert/badlands) |
| Job search | every 20-30 ticks; all columns within 5, + 16 random within 12 (pickups) / 16 within 16 (holes) |
| Pickup band | real top (max 8 above original ground) down to 1 below original ground |
| Hole depth | up to 12 below the original surface |
| Unnatural blocks | 5 % each time found |
| Cairn work radius | 48 |
| Reach / teleport if farther / stuck | 4 / 20 blocks / 3 s |
| Job timeout | 30 s |
| Spawn check | every 5 s, player within 96 of the site |
| Spawn chance | 4 % + 31 % x growth^3 (spire's growth); 35 % complete; x (1 + 0.3 per enderman within 64) |
| Cap | 2 + 6 x growth^2; 8 complete (+1 builder when all present are waiting builders and the next block is unclaimed) |
| Counting radius | 64 blocks around a site |
| Waiting builder | stays within 8 of its cairn |
| Vigil | ring 3 outside the footprint, 60 s at a time |
| Waiting builder | in the ring, checks each second, replans after 20 s |
| Errand | 25 % per idle look round, 12-40 blocks off, only if there is work there |
| Leaves by day | 1 in 400 a tick under open sky (~20 s); name tags keep one |
| Fillers | broken ground within 48 of a site remembered (cap 512); while one waits: chance at least 35 %, up to 4 past the cap |
| Pace (model) | 30 End blocks ~14 nights, 45 ~22, 60 ~30, with a player nearby; 1-2 blocks a night for the first week or so |
| Spawn ring | 12-28 blocks out, within 6 up/down of site ground, no player within 24 |
| Builders among newcomers (unfinished) | 50 % rising to 90 % with growth |
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
13. **Builders are a slow End-block source** (vanilla drops a carried block on death): killing the builders of an unfinished cairn gives purpur, end stone and end rods before the End. Left as is; say if it should be curbed (e.g. builders' blocks vanish on death).

## Test world station

Built as "Cairns and endermen" (`specs/testworld.md`, Layout). Two code changes came with it:
- **Hand-placed sites are saved** with the level: `cairn here` stores origin and seed in a persistent Fabric attachment on the `ServerLevel` (`bubbas_reworked_mobs:cairn_sites`), loaded the first time a level's sites are asked for. Without it a placed cairn is forgotten on reload and its blocks become stray unnatural blocks endermen slowly take. Game tests' `CairnSites.addSite` stays in memory only.
- **`cairn builders`** (op): one persistent enderman per missing block of the nearest site within 64 (at most 12, in build order), each carrying its block. A carried block is only delivered to a spot that wants exactly that block and touches something already standing, so builders carrying later blocks wait until the lower ones are in. In the test world the trunk was finished within 40 s. Natural builders follow the same rule one at a time (`nextUnclaimed`).

## Milestone test objectives

- Day leave: a caretaker under open sky at noon is gone within 60 s; a name-tagged one and one in the dark stay. Cairn spawning still runs with `spawn_mobs` false and stops with `spawn_monsters` false.
- (not yet tested) A `cairn here` site is still a site after the level is saved and reloaded (the test world's verify step checks it with `cairn locate`); `cairn builders` builders complete a cut-down cairn.

Core (every build, `ReworkedMobsCoreGameTest`): `endermanTerrainOracleAnswersFlatLayersAndCairns` (regenerated superflat column equals the generator's; a cairn's design is the target inside its site); `endermanCairnDesignsVaryAndUseOnlyEndBlocks` (108 designs: deterministic per seed, varied heights/sizes/trunk counts, only End blocks in the structure, none natural, never obsidian, every cairn has offerings and rods, the tallest rod is the pointer and faces the stronghold, TNT present but rare); `endermanTakesStrayBlockAndFillsHole` (cobblestone on open ground is taken and fills a stone hole as stone); `endermanBuilderDeliversCairnBlock` (a builder carrying a missing block completes the cairn); `endermanBusyIgnoresEyeContact`.

`EndermanGameTest` (milestone): `trampleFarmlandBackToDirt`; `breakingCairnAngersEndermen` (a floor block doesn't; a cairn block angers all around; hitting one near a site angers the others); `siteSpawnsBuildersUpToItsCap` (vanilla overworld spawns blocked, the rule unblocks them; complete > unfinished chance, snowball; an unfinished site spawns exactly up to its cap; about two thirds of newcomers carry a missing block destined for its spot); `wardStopsSpawningAndWork`; `carryingCaretakerStillDespawns`. The game test world is superflat, so live tests stand in a target for their field (`TargetWorld.overrideTerrain`) and endermen judge nothing elsewhere on the test server.

`EndermenClientGameTest` (client part `endermen`, a default-preset world, seed 20261116): prints the oracle's accuracy and timing and the "untouched world" repair counts; screenshots of the three sites nearest spawn as generated (stub) and built (close, far, top), and an enderman carrying a purpur pillar.

Not yet tested (objectives): a high cairn block is reached by teleporting onto the cairn; a stray flower is fetched for an offering; an unnatural block is taken only sometimes; a torn-down cairn drops back to the unfinished rate and a repaired one returns to the high rate (spawning numbers checked as functions only); the vigil ring forms around a complete cairn and breaks up when it's damaged (checked in the test world tour, `cairns_vigil`); the footing feature places only End blocks up to one above the ground; endermen keep out of a ward placed by Totems & Artifacts (needs both mods; `WardingGameTest` could call `api/Endermen.isKeptOut`).

## Owner answers, 2026-11-22

- **Explosions breaking a cairn don't anger endermen** (owner: no). Only a player breaking one or attacking nearby does (E4).
- **Banners:** skipped.
- **Variety:** as much as the builder likes (above).
- **Nether and End endermen** stay vanilla for now.
- **No reward for completing a cairn** (owner, 2026-11-22): the Totem of the End's cairn completion gift was removed (it was farmable: break one block of a finished cairn and put it back). The End is found only in the End. Beginning offerings nerfed to 0.15 of cairns.

## Cairn forms (built 2026-11-22, agent 2; owner: "sure add as much variety as you like")

Seven forms picked by seed: chorus, grove, spire, arch, canopy, henge, leaning (player page `Endermen.md`).

### How it's built

- `enderman/CairnDesign`: `Form` enum (CHORUS, GROVE, SPIRE, ARCH, CANOPY, HENGE, LEANING), picked by the design's first random roll with cumulative weights 26/14/14/12/12/11/11 %. `formOf(seed)` for tests and previews. Still a pure function of (seed, offerings, pointer, groundAt).
- New structural blocks (none generate in the overworld; `TerrainKinds.isNatural` false): end stone brick stairs (spire footing), end stone brick walls (henge posts), purple and pink stained glass besides magenta. Purpur slabs are now also branches (canopy) and post caps (henge).
- `growTrunk` takes `wander` (how far a trunk may step from its start: 1 grove and henge, 2 chorus, 4 leaning) and an optional `lean` direction (tried first 70 % of the time).
- `placePointer(top, pointer)` is shared by every form: the rod on its side beside the top block facing the stronghold, else on top. Afterwards every rod at or above the pointer's height is removed, as before.
- `reach` (the structure's farthest block) sets how far out offerings go; if no offering spot is free, one is placed just beyond the reach on the first free side.
- Untouched: CairnSites (placement), CairnSite (growth, complete = matches the design), offerings by biome, the TNT chance, the worldgen stub (natural offerings only), and the offering gift (by site seed, `OFFERING_SHARE` 0.15 on develop, saved per site, unaffected by the design).
- Hand-placed sites in saved worlds (`cairn here`) keep their seed but are redesigned with the new forms. Endermen treat any blocks of the old design that the new one doesn't want as stray (slowly taken away, unnatural at 5 %).

### Numbers (2000 designs, `EndermanGameTest.cairnDesignsAreCheapAndBounded`)

| Form | Share | End blocks, average | At most |
|---|---|---|---|
| Chorus | 25 % | 41 | 78 |
| Grove | 14 % | 60 | 99 |
| Spire | 15 % | 50 | 72 |
| Arch | 11 % | 56 | 88 |
| Canopy | 12 % | 82 | 106 |
| Henge | 11 % | 49 | 71 |
| Leaning | 12 % | 42 | 67 |

0.026 ms a design (worked out once per site, off the main thread). The spawn check's pass over the design grows with the block count (about 40-110 reads every 5 s per active site, against about 50 before): negligible.

### Builder's choices (owner may change)

1. **Seven forms with these shares.** Chorus stays the most common.
2. **Some cairns are bigger** (canopy about 82 End blocks on average, at most 106; the first design ran 30-60), so they take longer to finish: by the pace model (`tools/cairn-pace.py`, ~0.5 nights a block late on) a canopy takes roughly 40 nights against 14-30 before. Say if building time should be capped (e.g. shorter canopies).
3. **Hand-placed test-world cairns change shape** (same seed, new design).
4. **Offering lines** point in a random direction, not toward the stronghold (the rod is the only pointer, as the spec says).

### Tests

- Core `endermanCairnDesignsVaryAndUseOnlyEndBlocks` (updated): the allowed End blocks now include end stone brick stairs and walls and purple and pink glass; every form turns up in 400 seeds; all earlier checks (determinism, varied heights and sizes, multi-trunk share, only unnatural structural blocks, never obsidian, offerings and rods in every cairn, the tallest rod is the pointer facing the stronghold and the only one at that height, rare TNT) pass.
- Milestone `cairnDesignsAreCheapAndBounded` (new): 2000 designs average under 1 ms and none exceeds 120 End blocks.
- `:bubbas_reworked_mobs:build` passes (30 core); EndermanGameTest and EndermanTotemsGameTest milestone pass.
- Client part `cairnforms` (`CairnFormsClientGameTest`): one finished cairn of each form plus three more seeds on a flat world, from the north-west: `cairn-form-<form>-<seed>` and `cairn-forms-row`, and it prints each one's block counts. I looked at all seven forms and the extra arch.

## Trading at cairns (owner, 2026-11-22; design in progress, not built)

- **Endermen trade at their cairns.** Each cairn has **an obvious trade block** as part of its design, where a player leaves something and the endermen answer with something they've gathered.
- **Like piglin bartering:** the return is random from a table, **sometimes good and sometimes weird as hell** (owner).
- **They take anything at all** (owner): a way to get rid of junk and maybe get something better back, or not.
- Open: what the trade block is, how the answer arrives, whether what you give shapes what comes back.
