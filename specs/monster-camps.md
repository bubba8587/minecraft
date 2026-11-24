<!-- dte:C1,B15,B17 -->
# Reworked Mobs: monster camps (spec)

**Status:** built 2026-11-22: camps, banner keys, leaders, daily resets, a lit fire quiets a camp, shade and the underground room, pillager outposts, maps from cartographers. Built 2026-11-24: Nether outposts (the piglin kind, below) and trader posts (a wandering trader's market square, not a monster camp, below: it shares the camps' structure machinery).

Owner's design (2026-11-22), building on the earlier idea in `docs/research/camps-blood-moon-idea.md`. Decisions made since win where they differ (the 15 % spawn cut stays; **no blood moon**, owner 2026-11-22).

## What a camp is

- A structure with a **fixed roster** of monsters. It appears when a player comes near (a wide proximity trigger, so idle camps cost nothing).
- **Camp types (owner): skeletons and zombies.** Biome versions use the same rules with other mobs and materials: husks in deserts, strays in snow, bogged in swamps.
- **A leader** in each camp (owner): the same mob type, **slightly larger** (vanilla `scale` attribute), **tougher**, with **better gear**. It does **not** wear a banner.

## Banners are the key (owner)

- **The camp's banners are its key blocks.** Vanilla banners, no new block (B15). **Each monster type has its own banner pattern**, readable from a distance. A camp may have more than one.
- **Break every banner in the camp and it stops for good.** Kill the roster but leave a banner standing, and the camp comes back.
- **Put a banner back** (the camp's type's pattern) **within the camp's area, and the camp returns.** Players can re-arm a camp on purpose, for a farm or a fight.
- So camps are **registered per world, like cairns** (`specs/endermen.md`, CairnSites): location, type and area saved, fixed at worldgen. The game checks "a matching banner stands in this area?" and never scans for banners.

## Lighting a fire quiets a camp (owner, 2026-11-22)

- **A lit campfire (or soul campfire) inside a camp's area disables it** while it burns: the roster doesn't spawn and the camp doesn't reset. It's the fire ward idea (`specs/undead.md`, in progress) applied to the whole camp, and the way to shut a camp down **without breaking its banners**. Put the fire out (water, shovel) or break it, and the camp works again. It only quiets camps whose mobs fear fire (all of ours do).

## Alarms: dropped (owner, 2026-11-22)

- Camp alarm bells, and the raid horn for outposts, were designed and then dropped by the owner ("i dont think we should do camp alarm bells"). Camps have no alarm.

## Refilling (owner, 2026-11-22)

- **No blood moon.** A camp whose roster is dead but whose banner still stands has a **random chance to reset while you're away**: checked about once per in-game day while no player is near (outside the trigger range), with a chance per check (builder's starting value about 1 in 3, so usually within a few days). It never refills while someone is there.

## Pillager outposts become camps too (owner)

- Vanilla outposts spawn pillagers anywhere in their area forever (`spawn_overrides` on `minecraft:pillager_outpost`, full bounding box) and ignore banners. New rule: **when every banner in an outpost is broken, its pillagers stop spawning**, and putting one back brings them back.

## Look (owner)

- Started from images of Breath of the Wild's monster camps for the feel (owner: "look at images of it to start"; Minecraft's blocks make ours different anyway): **scant wooden lookout towers, ladders, bone-and-skull rock outcrops** built from Minecraft blocks (bone blocks, skulls, stone).
- **No campfires or other fire, and no light at all (owner, 2026-11-22): camps are dark.** Fire would ward their own monsters (`specs/undead.md` "Fire as a weak ward") and would make camps too easy to find and see. Dead (unlit) campfires as decoration are fine.
- Sizes and layouts vary. All deterministic from the seed.

## Layout variety (owner, 2026-11-22; parameters built 2026-11-22, skull domes designed)

- **Built:** the plan seed is mixed first (`RandomSupport.mixStafford13`), so close seeds no longer share a layout. Per camp: 0 to `maxTowers` towers (small 1, medium 2, large 3) spread round the clearing; 0 to outcrops+1 outcrops; 0 to outcrops+2 pikes; 1-2 lean-tos (1 in small camps); the stairs on any side but the first tower's, turning either way; the room 1 to `maxRoomHalf` half-wide across (small 2, medium 3, large 4), keeping the old reach toward the stairs (2, large 3) so the tunnel still surfaces, shifted up to a block off centre (never toward the stairs). With no tower, the first banner stands on the ground. The tunnel's mouth is cleared of trees back to the clearing. Room floors and the stair landing are always full blocks (mud becomes packed mud). Each room has a small chest (`chests/camp_room`: bones, arrows, flesh, coal, nuggets, string, sometimes an ingot, emerald, golden carrots or a book).
- **Highland biomes** added to `has_structure/monster_camp`: windswept gravelly hills and savanna, snowy slopes, stony, jagged and frozen peaks, stony shore. Rough ground is still skipped (`MAX_SLOPE` 6); levelling it is open.


- **Rock and bone caves: large skull domes** as a camp form, inspired by Breath of the Wild's skull-rock camps. Use it as a starting point, not something to copy (owner: "you wouldn't be able to copy them directly anyway"). Build it from Minecraft blocks: a big dome shaped like a skull, from bone blocks, calcite, stone and tuff, with eye-socket openings and a mouth entrance. The roster lives inside, dark, as camps are.
- **Varying numbers of every element:** towers, lean-tos, outcrops, pikes and the rest. Each camp picks its own count of each (zero is allowed where it reads well), so no two camps share a layout. Large camps can have several towers.
- Builder: show the spread with screenshots of several seeds per kind. Seeds are mixed first (BACKLOG, camp variety).

## How common (owner, 2026-11-22)

- **Not crazy frequent, but frequent enough to replace natural spawns for grinding mobs.** Natural surface spawning is cut to 15 %, so re-armable camps are where players farm skeletons and zombies. A player should usually find a camp of each type within a reasonable walk. Builder states the spacing (camps per 1000x1000 blocks, typical distance to the nearest of each type).

## Maps (owner, 2026-11-22; built)

- **Cartographer villagers sell maps to our places**, the way they sell ocean monument maps: the nearest graveyard, camp or Fabricator (Fabricators might need a high trade level). **Fabricator office chests hold a map to the next Fabricator.** (`specs/graveyards.md` and `specs/mob-factory.md` share this.)


### As built (2026-11-22, agent 2)

#### Player-facing

- **Cartographers** sell three new explorer maps (a compass plus emeralds, like vanilla's):
  - **Graveyard Explorer Map** (journeyman, level 2): 8 emeralds, marks the nearest crypt yard with a red X.
  - **Monster Camp Explorer Map** (journeyman, level 2): 8 emeralds, marks the nearest camp with a white X.
  - **Fabricator Explorer Map** (expert, level 4): 20 emeralds, marks the nearest **Ancient Fabricator** itself (its teleporter pad), within 3000 blocks.
- Fabricator office chests hold **no** Fabricator Explorer Map (owner, 2026-11-23: removed; the map is only sold by cartographers).
- They join vanilla's trade pools for those levels (so a cartographer offers them now and then among its usual trades).

#### The Fabricator map's X

- The Fabricator map is made by our own item modifier `bubbas_reworked_mobs:fabricator_map` (`factory/FabricatorMapFunction`, owner 2026-11-23: the city X was wrong): it finds the site with `FabricatorSites.nearest` (as `ancient_fabricator locate` does; `FabricatorTuning.MAP_SEARCH_RADIUS` 3000), then does what vanilla's `exploration_map` does (`MapItem.applyNewSavedData`, zoom 2, `renderBiomePreviewMap`, a trial-chambers X on the pad). The `on_fabricator_maps` tag is gone. `MapsClientGameTest` checks the X is on a Fabricator's pad.

#### Spec facts

- All data, vanilla systems: structure tags `bubbas_reworked_mobs:on_graveyard_maps` (crypt_yard), `on_camp_maps` (monster_camp); villager trades `data/bubbas_reworked_mobs/villager_trade/cartographer/{graveyard,camp,fabricator}_map.json` (vanilla `exploration_map` + `filtered` on map id, `filled_map` with an `item_name`); added to `#minecraft:cartographer/level_2` and `level_4` (`replace: false`). Map names in the lang file. (26.3 has no map colour component, so the maps keep the default look.)

#### Tests

- Client part `maps` (`MapsClientGameTest`, seed 20261116): each trade, offered by a cartographer, makes a named map with a target: graveyard -208,-368; camp 80,0; Fabricator map's X at a city with its Fabricator 148 blocks away (fails if no Fabricator within 400 of the X). `maps-graveyard` looked at: the Graveyard Explorer Map in hand with its red X.
- Full milestone suite 129/129.

## Milestone test objectives

- A camp's roster appears when a player comes within range, with one larger, tougher, better-geared leader.
- Breaking every banner in a camp stops it for good. Placing a matching banner back in its area brings it back.
- An outpost with every banner broken spawns no pillagers. Putting one back restores them.
- Camp pieces never overlap, 32 seeds per kind and size (`CampsGameTest.campPiecesNeverOverlap`).
- A camp's kind follows the biome at its centre: desert and badlands husk, swamp and mangrove bogged, snowy biomes stray, everything else skeleton or zombie (`CampsGameTest.campKindFollowsTheBiome`).
- Skull rocks (full and half-buried), winding towers, ramps, bridges and ribcage arches: every block supported, and a zombie can walk each end to end (`skullRockCanBeWalkedInto`, `buriedSkullCanBeWalkedInto`, `windingTowerCanBeClimbed`, `towerRampCanBeClimbed`, `bridgeJoinsTwoTowers`, `ribcagePathRunsUnderTheRibs`).
- Camp fences and chains end cleanly: every fence side matches its neighbour, no lone fence posts, every chain end meets a chain on its axis, a fence, a wall or a sturdy face (`campFencesEndCleanly`, all kinds, large camps).
- Nether outposts generate only in crimson forests and the Nether wastes (`CampsGameTest.netherOutpostBiomes`).
- An outpost's roster is piglins with a piglin brute leader; its hoglins stand in their pen, the first leashed to the post, none huntable (`outpostRosterAndPennedHoglins`).
- A piglin can walk through an outpost's gate, climb its ramps and reach its portal room (`outpostGateCanBeWalkedThrough`, `outpostRampCanBeClimbed`, `outpostPortalRoomCanBeReached`).
- An outpost's portal matches its seeded state: lit, whole and unlit, or broken (`outpostPortalsMatchTheirState`).
- Natural outposts in a real Nether (seed 20261116): in an allowed biome, no lava within radius + 1 from 8 below to 3 above, a piglin can path from the fire to the ramp's platform, the gate and down the tunnel, hoglins penned and leashed; shot from above, close, side, tunnel, gate and portal (client part `outposts`, `OutpostsClientGameTest`).

## As built (2026-11-22)

- **Monster camps** generate across the overworld: a dark trodden clearing (no light at all, owner) round a cold, unlit soul campfire ringed by log seats, a chest of loot, scrappy stilt lookout towers (log posts, a patchy plank platform, railings with gaps, a ladder, sometimes a slab roof), bone-and-skull rock outcrops, skulls on pikes and, in bigger camps, a lean-to over bedrolls.
- **Kinds** follow the biome: **husk** camps in deserts and badlands (acacia, sandstone), **stray** camps in snow (spruce, packed ice), **bogged** camps in swamps and mangroves (mangrove wood, mud bricks), and **skeleton** or **zombie** camps everywhere else (spruce or dark oak, stone or mossy cobble). Zombie and husk camps have zombie heads; the others skulls.
- **Sizes:** small (radius 6, 1 tower, 1 banner, 3 monsters + leader) 45%, medium (radius 8, 1 tower, 2 banners, 5 + leader) 35%, large (radius 11, 2 towers, 3 banners, 7 + leader) 20%.
- **Roster:** appears when you come within **48 blocks**. Archers (skeleton, stray, bogged) man the towers first; the rest stand round the fire (by day, in the shade). Camp monsters stay at their camp and never despawn. Zombie camps have no babies. About half wear a leather or chain helmet; some zombies and husks carry stone swords or axes.
- **Shade and the underground room** (owner, 2026-11-22: guaranteed shade spots and one small-to-medium underground room their mobs can go in; they burn otherwise). Camp mobs burn in daylight like any others. Every camp has:
  - a **lean-to** (all sizes): open toward the fire, 3 wide and 2 deep, roof four blocks up (three blocks of headroom; owner asked for it taller after mobs got stuck), fence posts, plank back wall, bedrolls;
  - **roofed towers**: every tower has a full slab roof, raised a block so the tower's banner clears it (owner); lookouts on the platform and mobs under it are shaded;
  - **one underground room** under the fire: 5x5 in small and medium camps, 7x7 in large, 3 high, dark, rock walls, beamed plank ceiling, cobwebs in the corners, a few bones, reached by a 2-wide stair tunnel that surfaces on the side away from the first tower.
  - Taking cover is **vanilla's own behaviour** (owner: don't reinvent it): skeletons, strays and bogged already have it (RestrictSunGoal, FleeSunGoal: they keep out of the sun and run for shade once burning); camp zombies are given the same two vanilla goals. If the roster appears by day it appears in the shade (the room first). Husks don't burn. A helmet keeps the sun off, as in vanilla.
- **Leader:** one per camp, same kind, **1.25x size**, **2.5x health**, +3 attack damage (melee kinds), half knockback resistance, iron helmet and chestplate, an **iron axe** (zombie, husk) or a **Power II bow** (archer kinds). No banner. Drops its weapon half the time and each armour piece 1 time in 5.
- **Banners are the key.** Each kind has its own banner: skeleton black with a white skull and base stripe, zombie green with a black skull and jagged hem, husk brown with a yellow skull and hem, stray light blue with a white skull and icicle fringe, bogged grey with a green murk rising and a lime skull. One flies from a tower post; the rest stand on short posts round the camp.
  - Break every banner of the camp's kind in the camp and it **stops for good**.
  - While one stands, a camp whose roster is all dead **resets** about once a day with a 1 in 3 chance, only while nobody is within 96 blocks. Then its roster comes back the next time someone comes near.
  - **Put one of the kind's banners back** anywhere in the camp's area (camp banners drop as items with their pattern) and it's armed again; a re-armed camp resets at the next daily check for sure, so it's back the next day you're away. A banner of another kind doesn't count.
- **Lighting a fire quiets a camp** (owner, 2026-11-22): a lit campfire or soul campfire anywhere in a camp's area disables it while it burns: the roster doesn't appear and the camp doesn't reset. Put the fire out or break it and the camp works again. Camps generate with their own campfire **unlit** and no light at all (owner: camps are dark), so they never quiet themselves. Checked from the area's block entities like the banners; saved as `quiet` on the camp.
- **Pillager outposts:** break every banner in an outpost (the watchtower's ominous banners) and its pillagers stop spawning there; the area spawns as its biome normally would. Put any banner back in the outpost and they return. (Patrols are unchanged.)
- **Loot chest:** 3-6 of bones, arrows, rotten flesh, string, leather, bread, iron and gold ingots, emeralds, gunpowder; plus one roll (35% nothing) of an enchanted iron sword, axe or bow, an enchanted book, chain helmet or chestplate, saddle, name tag, golden apple or 1-2 diamonds.
- Command: `/bubbas_reworked_mobs camp build <kind> <size> [seed]`, `camp reset` (runs the daily reset now, sure, whoever is near), `camp list`, `camp locate <kind>` (nearest camp of that kind: walks the camp structure set's spread grid ring by ring from where it runs, up to 6144 blocks, reading only each start chunk's structure start; "Nearest <kind> camp at x y z, N blocks away."; `CampCommand.nearest`). Vanilla `/locate structure` returns the nearest camp of any kind, so it can't be used to find a kind.

### Frequency

Structure set `monster_camps`: random spread, **spacing 14 chunks, separation 5** (was 10/3; owner 2026-11-22: "two bumps down", about half as many), kept **4 chunks clear of villages** (vanilla outposts do the same; a first run had a camp cutting through a village house). A camp needs fairly flat ground (rise of at most 6 across it), a dry centre and its biome at the centre and four corners, so only about 15% of tries make a camp.

Measured by the client part `camps` (seed 20261116, every placement cell within 2048 blocks of spawn, 16.8 km²):
- **5.9 camps per 1000 x 1000 blocks**: skeleton 2.6, zombie 2.7 per km²; husk, stray and bogged 0.2 each (they depend on how much desert, snow and swamp there is; this seed has little near spawn).
- **Typical distance to the nearest camp of a kind** (median over a grid of points): skeleton **~360 blocks**, zombie **~340 blocks**. Husk/stray/bogged ~1700-2200 here, i.e. "go find a desert/snowfield/swamp, then there are a few".
- Earlier tries for comparison: spacing 16 gave 2.3/km², nearest ~500-580; spacing 12 gave 4.5/km², ~350-400. Spacing is one number in the data file to tune after playtests.

### Builder's choices

- Camp mobs are vanilla mobs (skeleton, zombie, husk, stray, bogged) with a home radius at the camp; camp bogged don't lurk.
- A camp's banner check reads the block entities of the chunks its box touches (only while a player is within 96 blocks), not the world blocks; nothing is scanned.
- Roster spawns even with a creative player near (only spectators don't count), so building/inspecting a camp shows it.
- Re-arming makes the next daily check certain rather than spawning the roster instantly in front of you (the "never refills while someone is there" rule).
- Leaders and helmets as above; no names, no glow.

### Code

- Code: `camp/` — `CampKind` (mob, banner, materials, `forBiome`), `CampTuning` (sizes and numbers), `CampStructure` (placement checks), `CampPiece` (code-built layout; plan from the seed before any world read; `towerSpots()`, `groundSpots()`), `CampBanners` (patterns, `paint`, `matches`, `standsIn`), `CampSite` (saved state: id = start chunk, kind or null for outposts, box, centre, size, tower/ground spots, spawned, armed, rearmed, members), `CampSites` (registry as a Fabric level attachment `bubbas_reworked_mobs:camp_sites`; discovery from structure references of loaded chunks within 4 chunks of each player every second; arming; roster trigger; daily reset; outposts; `disarmedOutpostAt`), `CampRoster` (spawning, leader), `CampCommand`, `Camps` (registration, tick and entity-unload hooks). Shared `worldgen/ClippedBuild` (chunk-clipped placing, now also used by `CryptYardPiece`).
- Mixins: `CampMemberMixin` (Mob.customServerAiStep HEAD: gives camp zombies vanilla's `RestrictSunGoal` and `FleeSunGoal` once), `OutpostSpawnMixin` (ChunkGenerator.getMobsAt HEAD: inside a disarmed outpost, return the biome's spawns), `BoggedLurk` skips camp mobs.
- Roster deaths: Fabric `ServerEntityEvents.ENTITY_UNLOAD` with a destroying removal reason (killed or discarded, so conversions count too) removes the mob from its camp (entity tags `bubbas_reworked_mobs.camp` and `bubbas_reworked_mobs.camp:<id>`; leader also `bubbas_reworked_mobs.camp.leader`).
- Data: `worldgen/structure/monster_camp.json` (step top_layer_modification), `worldgen/structure_set/monster_camps.json`, `tags/worldgen/biome/has_structure/monster_camp.json` (26 land biomes incl. desert, badlands, snowy, swamp, mangrove, cherry grove, sparse jungle; no oceans, rivers, beaches, mushroom fields, deep dark, pale garden), `loot_table/chests/monster_camp.json`.

### Tests

- Core: `ReworkedMobsCoreGameTest.campRosterAppearsWithALeader` (body in `CampsGameTest.rosterAppearsWithALeader`): a survival player near a small skeleton camp makes 3 + 1 appear, exactly one leader, larger, tougher, iron chestplate, no banner.
- Milestone `CampsGameTest`: `bannersAreTheKey` (break every banner → disarmed, roster removal counted, never resets; another kind's banner doesn't arm it; put the kind's banner back → re-armed, resets at the next check), `aLitFireQuietsTheCamp` (lit campfire: no roster; out: roster appears; lit again after clearing: no reset), `campsHaveShadeAndARoom` (all 25 room cells and at least 4 surface spots are usable shade; a zombie can walk down the tunnel into the room), `exposedCampMobsSeekShade` (at noon the roster appears in shade; a skeleton put in the sun walks into shade), `outpostWithoutBannersSpawnsNoPillagers` (registered outpost box; banner gone → disarmed and `getMobsAt` has no pillagers; banner back → armed), `everyCampBlockIsSupported` (nothing pops off on a block update, and no block in a camp gives off light: camps are dark). Full milestone server suite 121/121; camp tests stable over 5 reruns. The client part also checks that a zombie on the surface outside a natural camp can path down into its room (reachable). One core build run failed once and passed on three reruns (could not see which test; likely one of the known flaky ones).
- Client part `camps` (`CampsClientGameTest`), screenshots looked at: one camp of each kind built on a flat world from above and close (camp-skeleton/zombie/husk/stray/bogged-above/-close), `camp-roster` (skeletons round the fire, a lookout on the tower, the leader), `camp-leanto` (a skeleton standing in its shade at noon), `camp-roster-night`, `camp-room` / `camp-room-back` (inside a natural camp's room, night vision), and the nearest natural camp in seed 20261116 (`camp-natural-0`, a skeleton camp in a forest clearing; one is enough since the built camps show every kind, 2026-11-22). The survey numbers above print from the same part.
- Not covered by a test: a natural outpost losing its banners in a generated world (the test uses a hand-registered outpost box; the mixin path and the outpost discovery are the same code the natural case uses).

### Shade code

- `camp/CampShade` only answers where the shade is: `sunBurns` (vanilla's rule: `isBrightOutside` and the `monsters_burn` environment attribute), `resolve` (the standable, covered height at a spot, three blocks of headroom), `covered` (blocks overhead). `CampRoster` uses it so a roster that appears by day appears in the shade. Taking cover itself is vanilla's sun goals (`CampMemberMixin`).
- `CampPiece.shadeSpots()` (saved on the site as `shade`), `tunnelStep(i)`, `leanToSpot()`; the camp box now reaches 10 (12 for large) from the centre so the tunnel is built; the room is skipped where the world isn't deep enough (superflat).

### Fixes after owner review (2026-11-22)

- **Ladders** end at the post's top (the top rung had no post behind it and popped off on a block update, owner); you step from the top rung onto the open post top.
- **Weeds** only where they can grow; **no dirt path** on camp ground (it turns to dirt under builds).
- **Crypts:** the noble sarcophagus lid is an upper slab, so its candle stands.
- Milestone tests `CampsGameTest.everyCampBlockIsSupported` (each kind) and `GravesGameTest.everyYardBlockIsSupported` (each wealth): every built block passes `canSurvive`.
- Shade checks read the blocks overhead (`CampShade.covered`), not sky light; shade spots need three blocks of headroom (leaders are 1.25x). The custom walk-to-shade code is replaced by vanilla's sun goals (above).
- Camp and crypt tests run on their own deep 32 x 48 field (`camp_field`); the camp box had spilled into neighbouring tests and broken two Mob Factory tests.

## Open

- **Nether outposts (owner to decide):** rarity (spacing 22), whether piglins may hunt the penned hoglins (now they can't), whether netherite scrap stays in the room chest. In `BACKLOG.md`.
- **Playtest numbers:** spacing (14 chunks, roughly half the old 3.2 camps per km²), trigger range, roster sizes, the leader, camp loot: starting values above.
- **Natural forest camps** sit among uncleared trees (leaves crowd the towers); worth a look.
- **Builder's calls to review:** the Builder's choices list above (camp bogged don't lurk, re-arming waits for the next daily check, rosters show for creative players).

## Footprint plan (2026-11-23, fixes overlapping pieces from the variety change)

The variety ranges are kept; pieces are laid out in a footprint plan (`CampPiece.Plan`) before building.

- Planning order: room and stair side first (no longer chosen to avoid the first tower), then reserved footprints: the fire (centre ±2: ring, seats, chest) and the stair tunnel where it opens to the surface (from step 3 of the tunnel to its cleared mouth, step 9, plus a wall column either side).
- Then pieces in order lean-tos, towers, outcrops, pikes, banners. Each claims a rectangle of ground cells: tower 6x6 (4x4 plus ladder ring), lean-to 3x3, outcrop (2m+3)², pike and banner 1 cell.
- A claim needs a gap of at least 1 cell (`MARGIN`) from every earlier footprint; a lean-to may sit right against the fire's footprint (it opens onto the seats). Each piece tries 16 spots (first at its planned angle, then random angles; lean-tos alternate distance r-3 and r-2), then is dropped, so a camp can have fewer pieces than it rolled. Lean-tos go first, so the guaranteed lean-to always fits.
- `CampPiece.footprints()` lists the plan's footprints (tests); `CampPiece.roomSpot()` is the room's middle floor cell (camera, path checks).
- Shade fix: `CampShade` treated carpet (lean-to bedrolls) as blocking, so most lean-to shade spots never resolved; carpet now counts as open.

Tests: milestone `CampsGameTest.campPiecesNeverOverlap` (32 seeds x every kind x every size, no two footprints share a cell); `campsHaveShadeAndARoom` targets `roomSpot()`. Client part `camps`: three extra seeds per kind (small, medium, large) from above (`camp-<kind>-seed<n>-<size>`); `camp-room`/`camp-room-back` stand in the actual room (`roomSpot()`); the natural room path check targets `roomSpot()` with accuracy 2.

Open: towers sometimes bunch on one side of the camp.

## Big pieces: skull rocks, winding towers, ramps, bridges, ribcages (2026-11-23/24)

Prior art taken: Breath of the Wild bokoblin camps (skull rocks entered by the mouth, stilt lookouts, rickety bridges), Valheim fuling villages (large camps spread out and linked), Skyrim bandit and giant camps (plank ramps, bones and skulls round a fire), Elden Ring forts (towers with a stair winding round the outside, archers up high), Far Cry outposts (height variety and several ways up, so a camp reads as a plan from afar).

All go through the footprint plan. Plan order: fire, stairs tunnel, skull rock, winding tower, ribcage, lean-tos, towers (+ ramp, + twin and bridge), outcrops, pikes, banners. Bounding box: radius + 5 out and 22 up. Kinds keep their themes (`CampKind.woodStairs()`: spruce, dark oak, acacia, mangrove). Vanilla blocks only (B15).

- **Skull rock** (large camps, 1 in 3; footprint 13 x 13, 10 blocks from the fire, facing it): drawn from a hand-made 13 x 13 front grid (`CampPiece.SKULL_FACE`: bone, eye, nose, mouth), pushed 4 deep (5 at the brow), with an ellipsoid back half (radii 6.6 across, 6.2 deep, 7.0 high, centred 6.5 up); a 1-block shell. Detail-lite (owner): bold features only, a big cranium over a narrower jaw, a brow ledge one out over two 3 x 3 eye sockets, a nose hole, and the mouth (5 wide, 2 high) as the door. Eyes and nose are plain holes into the hollow. Materials (`skullBone`): brow (dy 9, w 5) quartz pillar across the face; otherwise 70 % bone block, 15 % calcite, 10 % smooth quartz, 5 % quartz block; base ring bone block. Inside: trodden floor, a mob skull; the inside floor cells are shade spots.
- **Half-buried skull:** half of all skull rocks (from the skull's own seed bit). Sunk 4: cranium, brow and eyes show. The left eye (from the front) is the way in, with a bone-block step; the other eye stays dark.
- **Winding tower** (large camps, 1 in 4; footprint 7 x 7): a 5 x 5 log-and-plank core 12 to 14 high, a 1-wide stair winding clockwise round the outside (corner landings on log posts), log bands every 5 blocks, fence window slits, doorways off the stair, a railed top with a slab roof on posts. Archer spot at the top; the top 3 x 3 are shade spots.
- **Ribcage arch** (large camps, 1 in 3; footprint 9 x 7, 7 from the centre): a bone spine 5 up, 4 ribs each side curling in at the ground, a 3-wide path under it.
- **Ramps** (any tower, 35 %; footprint `ramp`, may touch its tower): a 2-wide stair ramp from the ground to the platform on a side whose foot stays in the clearing, log stilts under every other step, the railing open where it arrives.
- **Bridges** (medium and large camps, 40 % per tower): a same-height twin tower 4 to 7 off one side, a 2-wide plank bridge whose middle half sags to slabs. Railing (`CampPiece.bridge`), cells beside the deck: a log beam along the bridge at deck height; at deck + 1 log posts at both ends and every 4 blocks on long bridges, horizontal chains between. If the bridge doesn't fit, the twin is dropped too. Ladders never come down the bridge side.
- **Fence tidy** (`ClippedBuild.tidyFences(pieceBox)`, after each chunk's build): removes lone fences (no side joined, nothing on top, no fence below), sets every fence's sides from its real neighbours, and fixes fences just over the chunk edge an earlier chunk built, including removing a ring fence left joining nothing and unjoining the fence beside it (fixed a chunk-edge flake).

Client part `camps`: `camp-skull-*`, `camp-buried-*`, `camp-ribcage-*`, `camp-winding-*`, `camp-ramp-*`, `camp-bridge-*` (front, 3/4, side, far 34 blocks). Framing to fix: the ribcage bogged front camera ends up inside that camp's skull; the ramp `-side` camera sits close among towers.

Open: bridges to outcrops or skull tops, ramps up rock outcrops; nether fossil templates (`nether_fossils/fossil_*` via StructureTemplate through the plan) as bone dressing near skulls or outcrops; ramps and towers on rough ground only checked on flat ground and the natural camp.

## Nether outposts (owner, 2026-11-24; built 2026-11-24)

Owner's asks: Nether outposts as a kind of monster camp, less common than overworld camps; piglins and piglin brutes, with hoglins kept as livestock (penned and tethered); blackstone, polished blackstone bricks, gilded blackstone, gold, crimson wood, chains, soul and regular lanterns, with the bastion as reference; crimson forest and Nether wastes only; a lower room with a Nether portal, ruined-portal style (some lit, some unlit, some broken, crying obsidian), with layout ideas from bastions and fortresses.

**Overworld piglin outposts: scrapped (owner, 2026-11-24).** Piglins zombify in the overworld (vanilla), so outposts are Nether only.

### Kind and structure

- `CampKind.PIGLIN` (`camp locate piglin`, `camp build piglin <size> [seed]`). Same `CampPiece`, footprint plan, roster, refill and banner rules as overworld camps.
- Structure type `bubbas_reworked_mobs:nether_outpost` (`NetherOutpostStructure`): finds its own floor under the ceiling and builds the PIGLIN kind of `CampPiece`.
- Structure set `nether_outposts`: random spread, spacing 22, separation 8 chunks, salt 20261124, kept 3 chunks from `minecraft:nether_complexes` (bastions and fortresses). Overworld camps use 14/5. Terrain rejects many candidates, so outposts are much rarer: seed 20261116 had 5 outposts within 2000 blocks of 5 points.
- Biome tag `has_structure/nether_outpost`: crimson_forest and nether_wastes. Not basalt deltas (too jagged for a level clearing); not soul sand valley or warped forest (piglins don't spawn there in vanilla and warped fungus repels them).

### Placement under a ceiling

All from noise columns, before any block exists:

- The centre column is scanned for floors at least 10 above the lava sea (y >= 42): solid ground with 12 air above it, below the generation depth (so never the bedrock roof). One floor is picked at random.
- Columns every 3 blocks out to radius + 4 (gate, rampart and pen stand at the edge) each need a floor within ±5 with 5 air above it.
- Floor spread across the outpost at most 5.
- No lava from 9 below to 2 above any sampled floor.

### Building under a ceiling

`NetherBuild`, a `ClippedBuild` whose ground is the surface nearest the outpost's height (a lava surface counts as no ground):

- The clearing is levelled to the outpost's height (fill under, cut over, natural floor blocks kept); headroom carved 12 up.
- Lava within radius + 1.5, from 9 below to 12 above, is sealed to blackstone (lava springs come before our generation step).
- A level walkway runs round the stair tunnel to its mouth, and a 5-wide approach leads out of the gate.

### Materials (PIGLIN)

| Part | Block |
| --- | --- |
| Posts | crimson stem |
| Platforms | crimson planks |
| Roofs | polished blackstone brick slabs |
| Railings | nether brick fence (fortress) |
| Ramps | crimson stairs |
| Tunnel stairs | polished blackstone brick stairs |
| Ground | blackstone, netherrack, soul soil, polished blackstone |
| Outcrops | polished/cracked blackstone bricks, blackstone, gilded blackstone, gold blocks jutting out instead of bones |
| Fire | unlit regular campfire on a gilded blackstone ring |
| Pikes | piglin heads, or lanterns and soul lanterns |
| Bridge deck | nether bricks, nether brick slab sag (chains still end on posts) |
| Banner | black with a yellow piglin snout and an orange triangles-bottom |

### Pieces

- **From bastions:** a gilded gate in a stretch of rampart (thick polished blackstone brick pillars capped with gilded blackstone and gold, a chiselled keystone and gold over a 3-wide arched opening, cracked brick rampart wings with blackstone wall tops, soul lanterns under the arch); the hoglin pen (a nod to bastion hoglin stables).
- **From fortresses:** nether brick fence railings, nether brick bridge decks.
- **Not in outposts:** the skull rock, winding tower and ribcage (overworld only).
- **Hoglin pen:** a closed 5x5 ring of crimson fence on crimson nylium and soul soil, with a crimson fungus or two; the tether post in the middle is a fence with a lantern on top. Hoglins per pen: 1 small, 2 medium, 3 large (`CampTuning.OUTPOST_HOGLINS`); the first is leashed to the post. All are tagged livestock and set "cannot be hunted", as in a bastion stable (mixin `HoglinHuntInvoker`). They are not on the roster and not counted for clearing; re-made with the roster only when the pen has none left and its post still stands.
- **Portal room:** the overworld room, deeper (floor 7 down, 5 high, always 7x7 inside), reached by a longer stair tunnel. A 4x5 obsidian frame stands at the back facing the stairs, its whole frame inside one chunk. State from the outpost's seed: 40 % lit; 30 % whole but unlit with 1 to 3 crying obsidian; 30 % broken (1 or 2 frame blocks gone, crying obsidian, unlit). Netherrack and magma at its foot, magma only beside the frame, not in the walkway. A lit portal works as vanilla (using it may create an overworld portal).

### Roster, leader, light, loot

- **Roster:** piglins; ground members are a piglin brute 25 % of the time (`CampTuning.OUTPOST_BRUTE`). Tower lookouts hold crossbows; other piglins a crossbow or golden sword, brutes a golden axe. Piglins keep vanilla behaviour (neutral to players wearing gold). Built outside the Nether (command, tests), piglins and hoglins are made immune to zombification.
- **Leader:** a piglin brute, scaled and toughened like overworld leaders, in golden helmet, chestplate and leggings with Protection II and a golden axe with Sharpness III; drop chances as overworld leaders.
- **Light:** outposts are lit (lanterns, a lit portal). Overworld camps stay dark; the "camps are dark" check in `everyCampBlockIsSupported` skips the piglin kind.
- **Loot:** `chests/nether_outpost` (by the fire): gold nuggets and ingots, crimson fungus, raw and cooked porkchops, leather, quartz, arrows, string, golden carrots, a worn crossbow, 1 in 4 the outpost's banner. `chests/nether_outpost_room`: gold, gilded blackstone, enchanted golden sword/helmet/chestplate, obsidian, crying obsidian, flint and steel, fire charges, porkchops, a gold block, the piglin banner pattern, a snout trim template, and rarely netherite scrap (weight 1 of about 90). Both tables use 26.3's `"modifier": {"type": "minecraft:set_count"}` form (2026-11-24: they were written with the pre-26.x `"functions"` list, which 26.3 ignores, so every stack came out as 1); `MobFactoryGameTest.lootFilesUseCurrentKeys` checks every `bubbas_*` loot table and item modifier for old-form keys.

### Numbers for the owner to playtest

In `CampTuning`, `NetherOutpostStructure` and the structure set: spacing 22 / separation 8; floor at least 10 above the lava sea, headroom 12, slope 5; brute chance 25 %; hoglins 1/2/3; portal 40/30/30.

### Tests

- Milestone `CampsGameTest`: `netherOutpostBiomes`, `outpostRosterAndPennedHoglins`, `outpostGateCanBeWalkedThrough`, `outpostRampCanBeClimbed`, `outpostPortalRoomCanBeReached`, `outpostPortalsMatchTheirState`. Existing camp tests include the piglin kind automatically: `campPiecesNeverOverlap` (gate and pen are new footprints), `campFencesEndCleanly`, `everyCampBlockIsSupported` (minus the dark check).
- Client part `outposts` (`OutpostsClientGameTest`, seed 20261116): see Milestone test objectives.
- Natural test world: `tools/build-naturalworld.py` has a "Nether outpost (piglins)" sign (found with `camp locate piglin` run in the Nether, chunks force-loaded there, the sign teleports into the Nether, Nether chunks pruned like overworld ones). Seed 20261122: -1032 -213 (y 63).

## Trader posts (built 2026-11-24)

A wandering trader's market square: a striped wool stall, llama pens, a hitching rail, a signpost, a lit campfire and often a cart. Vanilla's daily wandering-trader spawn is sent to a nearby post, and its llamas arrive tied up in the pens. Governing leaf: C1 (Reworked Mobs' direction); no new leaf. B15: all vanilla blocks (wool stairs from 26.3), no new blocks.

**Home:** Reworked Mobs, next to camps, crypt yards and Nether outposts: it shares their code-built structure machinery (`worldgen/ClippedBuild`, the chunk-clipped build and fence pass, the footprint plan, the locate-by-placement-grid search) and it is about a mob. World Tweaks has no worldgen structure code.

**Prior art (B9):** `tools/prior-art.py` ("wandering trader post structure", "wandering trader camp", "trader camp", "wandering trader spawn"): no mod adds wandering-trader posts. Ocean/Desert Villager Trader add their own traders with structures (a different idea); "Wanderful Trades" adds trades; the rest change spawn rates or block traders. Built on vanilla's `WanderingTraderSpawner` (one injection) and vanilla blocks; only the choice of place is hand-rolled (the odds, timing, trader, llamas and despawning are vanilla's).

### Placement

- Structure `bubbas_reworked_mobs:trader_post` (step `top_layer_modification`, like camps), set `trader_posts` (random spread, spacing 18, separation 6, salt 20261124; exclusion zone 3 chunks from villages).
- Biomes `#bubbas_reworked_mobs:has_structure/trader_post`: plains, sunflower plains, meadow, savanna, savanna plateau, desert. A desert post needs a non-desert biome 48 blocks out in one of the four directions (desert edges only).
- `TraderPostStructure`: nine samples (centre, corners and edge middles 10 out) must be dry and in the tag; ground may rise or fall at most 4 (6 was tried: posts cut deep into hillsides); the floor is the mean of the nine sample heights, at or above sea level. Kept 2 chunks clear of camp and crypt-yard placement-grid candidates (checked from their sets' `getPotentialStructureChunk`, before their own terrain checks, so conservative): a camp's skeletons next to the trader would spoil it.
- Density in seed 20261116: about 0.08 posts per km² near spawn (a forest-heavy area), typically 1.5-2.5 km between posts; open plains and savanna get more. Rejections are mostly biome (13 of ~100 grid candidates near spawn were in post biomes); of those in biome, about a third pass the terrain checks.

### Layout (`TraderPostPiece`)

- Local frame round the centre: u across, w toward the front. The seed picks the front (4 ways), mirror, colours (primary blue or red; stripes primary/white/secondary/white, primary/secondary, or light blue/white/red/white), pens (one on the left, 60 % also one on the right) and cart (60 %).
- Plaza u -9..9, w -8..10, levelled to the post's floor height (rocky columns topped with coarse dirt); a 4-block skirt slopes a block per block back to the land; trees and plants cleared (14 up over the plaza). Soil plaza: coarse dirt patches (busier near the stall and fire), a dirt path out the front (w 4 to 14) and in front of the counter; sand plazas stay sand. Pen floors coarse dirt and grass.
- Footprints (local, inclusive): stall u -3..3 w -7..-3; fire u -2..2 w 1..3; pens u ±5..±9 w -3..3; cargo pile (one pen) u 5..7 w -2..0; rail u 4..8 w 6..7; signpost u 2 w 6..8; cart u -8..-4 w 5..8; entrance banners u ±2 w 10. All keep a gap of at least 1 and stay inside the plaza.
- Stall: spruce fence posts at the corners (3 high); roof: back eave wool stairs (y+4, w -7), ridge wool (y+5, w -6/-5), front eave stairs (y+4, w -4), overhang stairs (y+3, w -3), wool gable ends; striped by u. Back wall wool (y+2..3) over barrels (one with `chests/trader_post`) and a hay bale. Counter (w -4): barrel, three stripped spruce logs, barrel; on it a potted blue orchid, two carpets, three lit dyed candles, a lantern. Rug of carpets (w -6), a hanging lantern under the ridge, wall banners on the front posts and back wall. No displayed goods on the counter (owner, 2026-11-24: players could just take them).
- Pens: spruce fence ring 5 x 7, gate toward the plaza with lanterns on the fences either side, hay (a stack of two plus one) in a back corner, two water cauldrons in the other. A post with one pen has a cargo pile of hay and barrels on the other side.
- Rail: two 2-high posts, a fence top bar, two water cauldrons and a hay bale in front. Signpost: stripped log 2 high, fence, lantern, waxed spruce wall signs front and back ("Trader Post / All welcome", translatable `sign.bubbas_reworked_mobs.trader_post.line1/line2` with English fallback). Cart: spruce top slabs 4 x 2, dark oak trapdoor wheels (open, on the sides), barrel (`chests/trader_post_cart`), hay, barrel, carpet; fence shaft.
- Campfire lit, with two log benches (builder: unlike dark monster camps, it is a person's camp, and the light keeps the night quiet).
- Trader banner: base colour, yellow rhombus, zigzag hem (triangles bottom) in the other colour; red banners also a zigzag top.
- Blocks with entities are only painted when the chunk being built holds them (worldgen block entities have no level: sign text is loaded as NBT, not `setText`).
- Loot: stall barrel 3-5 stacks (wheat, dried kelp, bread, string, dyes, paper, now and then a lead; emerald weight 4 of 93, one at most per pick); cart barrel 2-4 stacks of food (wheat, kelp, carrots, potatoes, maybe a hay bale or a lead). Nothing that undercuts trading.

### The trader's arrival (`TraderPosts`, `WanderingTraderSpawnerMixin`, `TraderLlamaTetherMixin`)

- One injection in `WanderingTraderSpawner.spawn`, at its meeting-point lookup, i.e. after vanilla picked a player and passed its 1-in-10 roll (vanilla's check every 20 minutes and 25-75 % chance are unchanged). `TraderPosts.redirect`: a village meeting point (bell) within 48 of the player wins (vanilla); else the nearest post whose centre is within 96 (found from the set's placement grid and structure starts, a handful of chunk lookups, only on a successful roll). `arrive` needs the post's trader spot and llama spots loaded and clear, and the biome not in `#without_wandering_trader_spawns`; otherwise vanilla places the trader as usual.
- Trader at the spot behind the counter (u 0, w -5): despawn delay 48000 (vanilla), wander target and home the post centre, home radius 12 (builder: it wanders round the post like vanilla traders round a bell, not tied to the counter). Llamas: two, `TRADER_LLAMA.spawn(EVENT)`, one per pen (or both in the one pen), leashed to a lead knot on the pen's back fence; tagged `bubbas_reworked_mobs.trader_post` and `bubbas_reworked_mobs.tether:<fence pos>`, despawn delay 47999.
- Vanilla keeps any trader llama leashed to something other than its trader forever. `TraderLlamaTetherMixin` makes the post's own tether count like the trader's lead, so a tethered llama counts down and leaves when its stay is up (and its knot goes with it); otherwise penned llamas would pile up after every visit. A llama a player leads away (or ties up elsewhere) follows vanilla's rule again.
- No griefing: `spawn_overrides` monster category, `piece` box, empty list (vanilla's mechanism): no natural monster spawns inside. Light from lanterns, candles and the fire. Endermen already leave structure pieces alone (`EndermanTuning.STRUCTURE_MARGIN`).
- Between visits the post stands empty and intact.

### Commands

`/bubbas_reworked_mobs trader_post build [seed]`, `trader_post locate` (the plaza centre and its height), `trader_post arrive` (bring a trader to the nearest post within 96 blocks now). `/locate structure bubbas_reworked_mobs:trader_post` works.

### Code

`traderpost/`: `TraderPosts` (registration, keys, `nearest`, `redirect`, `redirectTo`, `arrive`, `tetheredAtPost`), `TraderPostStructure`, `TraderPostPiece` (plan, footprints, spots: `traderSpot`, `llamaSpots`, `tetherPosts`, `gates`, `gateFronts`, `entrance`, `penInsides`, `plaza`, `at(u, dy, w)`), `TraderPostCommand`. Mixins `WanderingTraderSpawnerMixin`, `TraderLlamaTetherMixin`. Data: `worldgen/structure/trader_post.json`, `worldgen/structure_set/trader_posts.json`, `tags/worldgen/biome/has_structure/trader_post.json`, `loot_table/chests/trader_post.json`, `trader_post_cart.json`.

### Numbers for the owner to playtest

Spacing 18 chunks, separation 6 (a candidate every 288 blocks); if they should be commoner, lower the spacing or allow a slope of 5. Draw range 96 blocks from the player; village bells within 48 still win; the trader stays within 12 of the post. Loot as above.

### Milestone test objectives

- `TraderPostGameTest` (milestone): `traderPostBiomes` (in: plains, sunflower plains, meadow, savanna, savanna plateau, desert; out: forest, dark forest, taiga, jungle, swamp, river, ocean, beach, snowy plains, badlands, mushroom fields, windswept hills); `postPiecesNeverOverlap` (300 seeds, gap 1, inside the plaza); `everyPostBlockIsSupportedAndFencesEndCleanly` (four seeds: one or two pens, with and without a cart); `postCanBeWalkedThrough` (a trader from the entrance reaches the counter and each pen gate); `tetheredLlamasLeaveWhenTheirStayIsUp` (post llamas and their knots go; a trader llama tied up elsewhere stays, vanilla); `vanillaPlacementWithoutANearbyPost` (no post: vanilla; a post over 96 blocks off: vanilla; a village bell within 48: vanilla).
- Core: `ReworkedMobsCoreGameTest.traderArrivesAtAPostWithLlamasPenned` (body `TraderPostGameTest.traderArrivesWithLlamasPenned`): with two pens and one, the trader arrives behind the counter with vanilla's stay, two llamas penned (one per pen when two), tethered to the pen fence and counted as tethered at the post.
- Client part `traderposts` (`TraderPostsClientGameTest`): natural posts in seed 20261116, each checked for biome, flat dry floor, sound blocks, clean fences, no monster spawns, a trader's walk to the counter and gates; shots `traderpost-<i>-above/-front/-left/-backright`, `traderpost-0-counter/-pen`; the daily-spawn redirect from a player 30 blocks off (`traderpost-arrival-front/-pen/-above`), a night shot (`traderpost-night`); prints the density.
- Not covered by a test: the injection point itself firing inside vanilla's daily tick (the test calls the same `TraderPosts.redirect` the mixin calls; the mixin would fail to apply at startup if its target moved).
- Natural test world: sign "Trader post (wandering trader)" (`specs/testworld.md`).
