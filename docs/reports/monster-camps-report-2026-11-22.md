# Monster camps: builder report (2026-11-22)

Branch `camps` (worktree `../mc-camps`). Task: build `specs/monster-camps.md`. For the orchestrator to merge into that spec (it says "not built"; the Open section can now be filled from here) and the player docs.

## Player-facing

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
- Command: `/bubbas_reworked_mobs camp build <kind> <size> [seed]`, `camp reset` (runs the daily reset now, sure, whoever is near), `camp list`.

## Frequency (orchestrator's owner note: not crazy frequent, but enough to replace grinding spawns)

Structure set `monster_camps`: random spread, **spacing 10 chunks, separation 3**, kept **4 chunks clear of villages** (vanilla outposts do the same; a first run had a camp cutting through a village house). A camp needs fairly flat ground (rise of at most 6 across it), a dry centre and its biome at the centre and four corners, so only about 15% of tries make a camp.

Measured by the client part `camps` (seed 20261116, every placement cell within 2048 blocks of spawn, 16.8 km²):
- **5.9 camps per 1000 x 1000 blocks**: skeleton 2.6, zombie 2.7 per km²; husk, stray and bogged 0.2 each (they depend on how much desert, snow and swamp there is; this seed has little near spawn).
- **Typical distance to the nearest camp of a kind** (median over a grid of points): skeleton **~360 blocks**, zombie **~340 blocks**. Husk/stray/bogged ~1700-2200 here, i.e. "go find a desert/snowfield/swamp, then there are a few".
- Earlier tries for comparison: spacing 16 gave 2.3/km², nearest ~500-580; spacing 12 gave 4.5/km², ~350-400. Spacing is one number in the data file to tune after playtests.

## Builder's choices (owner may want to weigh in)

- (Replaced by the owner's shade rule: camp mobs burn, and seek shade; `CampMemberMixin` now drives `camp/CampShade`.)
- Camp mobs are vanilla mobs (skeleton, zombie, husk, stray, bogged) with a home radius at the camp; camp bogged don't lurk.
- A camp's banner check reads the block entities of the chunks its box touches (only while a player is within 96 blocks), not the world blocks; nothing is scanned.
- Roster spawns even with a creative player near (only spectators don't count), so building/inspecting a camp shows it.
- Re-arming makes the next daily check certain rather than spawning the roster instantly in front of you (the "never refills while someone is there" rule).
- Leaders and helmets as above; no names, no glow.

## Spec facts (specs/monster-camps.md)

- Code: `camp/` — `CampKind` (mob, banner, materials, `forBiome`), `CampTuning` (sizes and numbers), `CampStructure` (placement checks), `CampPiece` (code-built layout; plan from the seed before any world read; `towerSpots()`, `groundSpots()`), `CampBanners` (patterns, `paint`, `matches`, `standsIn`), `CampSite` (saved state: id = start chunk, kind or null for outposts, box, centre, size, tower/ground spots, spawned, armed, rearmed, members), `CampSites` (registry as a Fabric level attachment `bubbas_reworked_mobs:camp_sites`; discovery from structure references of loaded chunks within 4 chunks of each player every second; arming; roster trigger; daily reset; outposts; `disarmedOutpostAt`), `CampRoster` (spawning, leader), `CampCommand`, `Camps` (registration, tick and entity-unload hooks). Shared `worldgen/ClippedBuild` (chunk-clipped placing, now also used by `CryptYardPiece`).
- Mixins: `CampMemberMixin` (Mob.burnUndead HEAD), `OutpostSpawnMixin` (ChunkGenerator.getMobsAt HEAD: inside a disarmed outpost, return the biome's spawns), `BoggedLurk` skips camp mobs.
- Roster deaths: Fabric `ServerEntityEvents.ENTITY_UNLOAD` with a destroying removal reason (killed or discarded, so conversions count too) removes the mob from its camp (entity tags `bubbas_reworked_mobs.camp` and `bubbas_reworked_mobs.camp:<id>`; leader also `bubbas_reworked_mobs.camp.leader`).
- Data: `worldgen/structure/monster_camp.json` (step top_layer_modification), `worldgen/structure_set/monster_camps.json`, `tags/worldgen/biome/has_structure/monster_camp.json` (26 land biomes incl. desert, badlands, snowy, swamp, mangrove, cherry grove, sparse jungle; no oceans, rivers, beaches, mushroom fields, deep dark, pale garden), `loot_table/chests/monster_camp.json`.

## Tests

- Core: `ReworkedMobsCoreGameTest.campRosterAppearsWithALeader` (body in `CampsGameTest.rosterAppearsWithALeader`): a survival player near a small skeleton camp makes 3 + 1 appear, exactly one leader, larger, tougher, iron chestplate, no banner.
- Milestone `CampsGameTest`: `bannersAreTheKey` (break every banner → disarmed, roster removal counted, never resets; put the kind's banner back → re-armed, resets at the next check), `otherBannersDontCount`, `aLitFireQuietsTheCamp` (lit campfire: no roster; out: roster appears; lit again after clearing: no reset), `campsHaveShadeAndARoom` (all 25 room cells and at least 4 surface spots are usable shade; a zombie can walk down the tunnel into the room), `exposedCampMobsSeekShade` (at noon the roster appears in shade; a skeleton put in the sun walks into shade), `outpostWithoutBannersSpawnsNoPillagers` (registered outpost box; banner gone → disarmed and `getMobsAt` has no pillagers; banner back → armed). Full milestone server suite 121/121; camp tests stable over 5 reruns. The client part also checks that a zombie on the surface outside a natural camp can path down into its room (reachable). One core build run failed once and passed on three reruns (could not see which test; likely one of the known flaky ones).
- Client part `camps` (`CampsClientGameTest`), screenshots looked at: one camp of each kind built on a flat world from above and close (camp-skeleton/zombie/husk/stray/bogged-above/-close), `camp-roster` (skeletons round the fire, a lookout on the tower, the leader), `camp-leanto` (a skeleton standing in its shade at noon), `camp-roster-night`, `camp-room` / `camp-room-back` (inside a natural camp's room, night vision), and the nearest natural camps in seed 20261116 (`camp-natural-0..2`: a skeleton camp in a forest clearing, a bogged camp in a swamp). The survey numbers above print from the same part.
- Not covered by a test: a natural outpost losing its banners in a generated world (the test uses a hand-registered outpost box; the mixin path and the outpost discovery are the same code the natural case uses).

## Undead variants

Already reported separately: `docs/reports/undead-variants-report-2026-11-22.md` (commit fc85113).

## Spec facts added for shade (specs/monster-camps.md)

- `camp/CampShade`: by day (`isBrightOutside` and the `monsters_burn` environment attribute), for camp mobs tagged burn-in-daylight: avoid-sun pathing on; when the eye sees the sky and there's no target (or it's on fire), path to the nearest free shade spot. `resolve` finds the standable, sky-less height of a spot (exact for the room, near the ground for the rest). Sky visibility is the sky-light level (15), so it settles with the light engine a moment after blocks change.
- `CampPiece.shadeSpots()` (saved on the site as `shade`), `tunnelStep(i)`, `leanToSpot()`; the camp box now reaches 10 (12 for large) from the centre so the tunnel is built; the room is skipped where the world isn't deep enough (superflat).

## Fixes after owner review (2026-11-22)

- **Ladders** stopped one rung short of the top: the top rung had no post behind it and would pop off on a block update (owner). They now end at the post's top; you step from the top rung onto the open post top.
- **Weeds** are only planted where they can grow (grass on gravel would pop); **dirt path** is gone from camp ground (it turns back to dirt under anything built on it).
- **Crypts:** the noble sarcophagus lid is now an upper slab, so the candle on it stands (it floated on a lower slab).
- New milestone tests `CampsGameTest.everyCampBlockIsSupported` (one camp of each kind) and `GravesGameTest.everyYardBlockIsSupported` (a crypt of each wealth): every built block passes `canSurvive`, so nothing pops off on an update.
- Shade checks read the blocks overhead (`CampShade.covered`), not sky light, so they hold right after a camp is built; shade spots need three blocks of headroom (leaders are 1.25x).
- Camp and crypt tests now run on their own deep 32 x 48 test field (`camp_field`): the camp box (out to 10 from the centre, room 5 down) had spilled into neighbouring tests and made two Mob Factory tests fail. Full milestone suite 123/123, twice.
