<!-- dte:C1,B15,B19 -->
# Docs report: Ancient Fabricators, mechanics (2026-11-16)

Adds to the look reports (`factories-docs-`, `-look2-`, `-light-2026-11-16.md`); merge all into `specs/mob-factory.md`. Code: `factory/` (`FabricatorSites`, `FabricatorFeature`, `FabricatorSite`, `Fabricators`, `FabricatorMobs`, `FabricatorLink`, `FabricatorLoot`, `FabricatorTuning`, `FactoryDesign`, `FactoryCommand`), `compat/FabricatorBeaconPayload`, mixins `MobDeactivatedMixin`, `MobPersistenceAccessor`, `WardenJukeboxMixin`. Every number is in `FabricatorTuning` (owner playtests, F5).

## Spec facts (as built)

**Worldgen: one beside every ancient city.**
- *Where:* sites are worked out from the seed, never stored.
  - For each ancient-city grid cell (vanilla's own `ancient_cities` placement, read from the structure set, so datapacks that move cities move Fabricators), the candidate city chunk is checked with the city's biome test: the biome at the chunk corner at y -27 must be in the city structure's biome set. The vanilla test uses the centre piece's middle, so very rarely the two could disagree.
  - The Fabricator stands **150 blocks** from the city's centre (clear of its pieces, which reach 116) in a seeded direction, deck at **y -50** (city floors are about -51).
  - It is turned (quarter turns) so its front faces the city. Its mob is seeded from the five.
- *The tunnel:* replaces the old winding front tunnel. It runs straight (with a slight wander) toward the city's centre at deck level, easing to the city floor, and stops 70 blocks from the centre. It cuts its own gap through whatever of the city's outer ring is in its way (it is placed after the city).
  - A polished and chiseled deepslate **doorway** frames the passage 96 blocks from the city's centre.
  - The tunnel floor is deepslate tiles. Its sides are only touched where the world holds liquid (sealed with deepslate).
- *Placement:* a worldgen feature (`bubbas_reworked_mobs:ancient_fabricator`, every overworld biome, last decoration step, so after the city and the deep dark's sculk) places, per chunk, the part of the build inside that chunk.
  - Cost per chunk: a cheap seed hash and one biome lookup per nearby cell (cached), plus a rough distance test.
  - Only chunks near a cavern or its tunnel work out the design (about 54k blocks, cached for the last 6 sites).
  - Sculk patches grow from their seed chunk, as the cities' do. Chest loot tables are set at placement.
- *First run:* what worldgen can't do waits for the first time a player comes near (`Fabricators.prepare`, saved):
  - the patch shriekers are set to can't-summon;
  - the pad and its lamps are cleared of sculk;
  - the jukebox gets disc 13;
  - the Warden is spawned;
  - with Reworked Beacons, the floor beacon is loaded.
- *Verified in the client test (seed 20261116):* the Fabricator nearest spawn generated whole at (2515, -50, 850), and the structure manager confirms a real ancient city at its city spot. Screenshots `fabricator-worldgen`, `fabricator-city-door-nightvision`.
- *Command:* `ancient_fabricator locate` names the nearest one (natural or hand-placed) within 3000 blocks, its mob, and whether it still runs. Hand-placed ones (`here`/`build`) are **saved with the world** (`bubbas_reworked_mobs:fabricator_sites`) and run like natural ones. Per-site state (prepared, Warden spawned, beacon loaded, broken) is in `bubbas_reworked_mobs:fabricator_states`.

**Running** (while a non-spectator player is within 64 of the pad; checked every second; a site stops 2 s after its last player leaves). One count:
1. At the start of each count the dripper drops its mob. Particles (purple tears, sculk soul) and a bloom sound mark the tip.
2. The mob appears just under the crying obsidian tip, head clear of it, and falls about 7 blocks onto the pad, **deactivated**:
   - its whole server AI step is skipped: no goals, brain, navigation or look/move/jump control;
   - silent, head hanging (pitch 60), never hurt by the fall;
   - it can still be hit and pushed.
3. The 8 **timer lamps** light one by one, one every **2 s**, with a sculk click rising in pitch.
4. **Portal particles** over the pad thicken with the count, and run with nothing on the pad too.
5. With all 8 lit comes the **lift**: everything on the pad (5x5, 3 high; not spectators, not the Warden) gets **Levitation I for 1.5 s**, with a beacon hum. After 1 s it **fires** and the lamps go dark.
   - A deactivated mob goes to a **surface spot within 96 blocks sideways**: at least 16 above the pad, the top block of a loaded, ticking column, passing vanilla's natural-spawn rules for that mob (placement and light: dark ground, so in practice at night), no player within 24, room to stand. 24 tries.
   - With no spot (daytime), it stays on the pad for the next count.
   - Arrived, it lies limp **2 s** in a column of portal particles (plus a burst and the enderman-teleport sound), then **wakes**.
   - It was made with the `TRIGGERED` spawn reason, so `MobFinalizeSpawnMixin` leaves it reworked (watcher creeper, caretaker enderman, reworked undead); the game test checks a creeper wakes as a watcher.
   - A **player** (or any other living thing) who hopped on rides it up to a surface spot with standing room, any time of day, in the same particles.
6. **Rate:** one mob per count, 17 s, so about 3.5 a minute while a player is near.
7. Peaceful stops drops, and so does the `spawn_monsters` rule (except on the game test server).

**Redstone (F2): powered = stopped**, like a locked hopper.
- The control is the pad's **lodestone**: power it (a lever, a torch beside it, a redstone block under it). The count doesn't run and the lamps stay dark.
- The dripper keeps dropping one mob per 17 s, so they **pile up deactivated on the deck, up to 12** (the cap applies always).
- Deactivated mobs are **persistent** while deactivated (a pile stays when you step away) and lose that when they wake.

**The hidden spawner (F6).** Checked every half second while the site runs. When it's gone:
- the Fabricator is **stopped for good** (saved);
- the lamps go dark;
- everything deactivated on the deck **wakes up there**;
- the dripper shrieks with a burst of sculk soul.

The Warden and the debuffs carry on.

**The Warden (F8).**
- One per Fabricator, spawned the first time it runs, in its office window. Persistent, never respawned.
- **Passive** means no AI at all: it can't move, dig or hear vibrations (vanilla Wardens with no AI hear nothing, so its own jukebox and the dropped mobs don't stir it). Every second it turns to watch the pad, or whatever stands on it.
- It **wakes** when a survival/adventure player:
  - **goes for the machine or a beacon without sneaking**: within 6 of the hidden spawner, 5 of the ceiling beacon, 4 of the floor beacon, or up in the roof (9+ above the deck, within 16 of the pad sideways);
  - **breaks a block in that zone** (even sneaking);
  - **hits it**.
- Awake, it has its AI back and anger 100 at that player (angry is 80): a vanilla Warden that **never digs away** (dig cooldown refreshed every second).
- Wardens don't hear a Fabricator office's jukebox (`WardenJukeboxMixin`), so a woken one isn't pulled back to the office by the music.

**Debuffs (F7) and the beacons.**
- Both pyramids are now **2 layers** (3x3 + 5x5 iron each; the floor's lower layer is hidden in the deck): **68 iron blocks** in all (the owner said iron is fine).
- The debuff area is a 2-layer pair's: **90 sideways**, 10 past both pyramids.
- *Without Reworked Beacons:* the floor beacon is the "core". While it stands, every survival/adventure player in the area gets **Mining Fatigue III, Weakness I, Slowness I** (13 s, renewed every 4 s, ambient particles). Break it and they end.
- *With Reworked Beacons, without Infused Gems:* the floor beacon holds a **Fabricator Heart** (an echo shard, named, marked with custom data). Our payload (`compat/FabricatorBeaconPayload`, loaded only through that mod's entrypoint; `compileOnly`, `suggests`) gives the same three debuffs to players in the pair's area. Take it out or break the beacon and they end.
  - The heart works in any beacon: a player can make a debuff beacon with it (taking over).
  - New small Reworked Beacons API: `api/BeaconSlots.item/setItem` (fill a beacon's slot from another mod).
- *With Reworked Beacons and Infused Gems:* the floor beacon holds a real **perfect Mining Fatigue gem with a Slowness bonus** (Gem Power VI, capped to III by the 2-layer pyramid). It is built by name, with no link to Infused Gems, and Infused Gems' own payload gives it to everything in the area (B18). Take it out and that ends.

**Office loot.**
- `loot_table/chests/fabricator_office`, 3 chests:
  - 3-6 rolls of: paper, books, a book and quill, echo shards, disc fragments, gray candles, iron, redstone, sculk;
  - then one roll: nothing (12) / disc 13 (2) / disc 11 (1) / Swift Sneak book (3).
- **Spare parts (owner):** with Infused Gems installed the chests use `fabricator_office_with_gems` instead (it loads only with that mod, via a Fabric load condition; chosen in code by `isModLoaded`). It adds a **1-in-3** chance of a harmful gem: Mining Fatigue 3 / Slowness 2 / Weakness 2 (weights) at Gem Power III tier 2, or 1 the beacon's own perfect Mining Fatigue gem. Without Infused Gems, no gems.

## Builder's choices (owner may want to hear)
1. **Powered stops the teleporter** (like a hopper). The lodestone is the input.
2. **Starting rate:** one mob per 17 s count, pile cap 12, 2 s wake, 96-block reach.
3. **Placement:** 150 blocks from the city, deck at -50. The tunnel cuts into the city's outer ring; the doorway 96 from its centre is a guess at where the city's edge is. The first real one looks right in screenshots (a deepslate city wall beside the door), but the owner should walk one.
4. **Two-layer pyramids** so an Infused Gems beacon gem isn't stuck at Gem Power I: 68 iron blocks per Fabricator.
5. **Dropped mobs keep sculk quiet?** No: falls are vibrations, so deck sensors twitch. Shriekers only answer players; the passive Warden hears nothing.
6. **A deactivated mob in daylight:** it can't be sent up (no dark spot), so it waits on the pad. Undead deactivated on the deck are underground, so no burning.
7. **Players riding up** get no darkness rule and land on any solid surface spot within 96: an escape route.
8. **Natural Fabricators are generated only in new chunks.** In an existing world, Fabricators appear beside cities whose surroundings haven't generated yet.
9. **Office chests, the Warden and the beacon item** are set up when worldgen places them (loot) or the first time a player comes near (Warden, item, jukebox).
10. **Creative players** neither wake the Warden nor get debuffs. Spectators don't make it run.

## Milestone test objectives
- **Core** (`ReworkedMobsCoreGameTest`, 4; bodies in `MobFactoryGameTest`, each building a whole Fabricator 8000 blocks out with forced chunks and running it without a player):
  - `fabricatorDropsDeactivatedMob`: limp, looking down, unhurt, silent, no target;
  - `fabricatorSendsItUpAndItWakesReworked`: lamps count, lift, sent up, wakes a watcher creeper;
  - `fabricatorRedstoneStopsTheTeleporter`: count never starts, lamps dark, mob stays, nothing sent;
  - `fabricatorBreakingTheSpawnerStopsIt`: broken saved, deck mob wakes, no more drops.
- **Milestone** (`MobFactoryGameTest`):
  - `wardenWakesWhenItHearsAPlayerAtTheMachine`: one Warden, passive and persistent, wakes angry at a survival player at the floor beacon;
  - `debuffsWithoutReworkedBeacons`: Mining Fatigue III, Weakness, Slowness.
- **Client part `factories`:** the look shots plus:
  - `factory-countdown`: a deactivated zombie on the pad mid-count;
  - `factory-lift`: all lamps lit, lifted;
  - `factory-arrival`: the zombie limp on the surface at night in its particles;
  - `factory-worldgen` and `factory-city-door(-nightvision)`: the natural Fabricator nearest spawn and its doorway, with the "real ancient city there: true" check printed.
- **Not yet tested (objectives):**
  - a player riding the pad lands on the surface;
  - the Warden ignores its jukebox after waking;
  - a woken Warden never digs;
  - the heart / Infused Gems gem in the beacon (with `-PwithBeacons`, and with Infused Gems), and taking it out ends the debuffs;
  - office loot with and without Infused Gems;
  - a pile caps at 12;
  - the Fabricator survives a save and reload (sites, states, persistent deactivated mobs);
  - `ancient_fabricator locate`;
  - a natural Fabricator near every city in a large area.

## Player-facing text (draft)
Beside every ancient city lies an **Ancient Fabricator**. Its living warped roof drips monsters onto an ancient teleporter. They land limp and still, and as its eight lamps light one by one they're lifted and sent up to the surface, where they wake on dark ground at night. If you see one appear in a burst of portal light, a Fabricator is beneath you.
- Kill them on the pad before they're sent.
- Power the pad's lodestone with redstone to stop the teleporter and let them pile up.
- Step on the pad yourself to ride it up and out.
- Find the hidden spawner buried in the roof and break it to stop the Fabricator for good.
- A Warden watches from its office window. It stays still until it hears you going for the roof or the beacons.
- The beacons make the whole area sap your strength (Mining Fatigue, Weakness, Slowness).

## Other docs to touch
- `specs/reworked-beacons.md` (new `api/BeaconSlots`).
- `specs/effect-gems.md` (a structure loads a gem into a beacon; gems in Fabricator chests).
- `CLAUDE.md` client part list (`factories`).
- `BACKLOG.md`: the doorway's exact spot at the city's edge; vanilla's infinite jukebox.
