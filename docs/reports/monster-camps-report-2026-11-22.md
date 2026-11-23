# Monster camps: builder report (2026-11-22)

Branch `camps` (worktree `../mc-camps`). Task: build `specs/monster-camps.md`. For the orchestrator to merge into that spec (it says "not built"; the Open section can now be filled from here) and the player docs.

## Player-facing

- **Monster camps** generate across the overworld: a trodden clearing with a soul campfire ringed by log seats, a chest of loot, scrappy stilt lookout towers (log posts, a patchy plank platform, railings with gaps, a ladder, sometimes a slab roof), bone-and-skull rock outcrops, skulls on pikes and, in bigger camps, a lean-to over bedrolls.
- **Kinds** follow the biome: **husk** camps in deserts and badlands (acacia, sandstone), **stray** camps in snow (spruce, packed ice), **bogged** camps in swamps and mangroves (mangrove wood, mud bricks), and **skeleton** or **zombie** camps everywhere else (spruce or dark oak, stone or mossy cobble). Zombie and husk camps have zombie heads; the others skulls.
- **Sizes:** small (radius 6, 1 tower, 1 banner, 3 monsters + leader) 45%, medium (radius 8, 1 tower, 2 banners, 5 + leader) 35%, large (radius 11, 2 towers, 3 banners, 7 + leader) 20%.
- **Roster:** appears when you come within **48 blocks**. Archers (skeleton, stray, bogged) man the towers first; the rest stand round the fire. Camp monsters stay at their camp, **don't burn by day** and never despawn. Zombie camps have no babies. About half wear a leather or chain helmet; some zombies and husks carry stone swords or axes.
- **Leader:** one per camp, same kind, **1.25x size**, **2.5x health**, +3 attack damage (melee kinds), half knockback resistance, iron helmet and chestplate, an **iron axe** (zombie, husk) or a **Power II bow** (archer kinds). No banner. Drops its weapon half the time and each armour piece 1 time in 5.
- **Banners are the key.** Each kind has its own banner: skeleton black with a white skull and base stripe, zombie green with a black skull and jagged hem, husk brown with a yellow skull and hem, stray light blue with a white skull and icicle fringe, bogged grey with a green murk rising and a lime skull. One flies from a tower post; the rest stand on short posts round the camp.
  - Break every banner of the camp's kind in the camp and it **stops for good**.
  - While one stands, a camp whose roster is all dead **resets** about once a day with a 1 in 3 chance, only while nobody is within 96 blocks. Then its roster comes back the next time someone comes near.
  - **Put one of the kind's banners back** anywhere in the camp's area (camp banners drop as items with their pattern) and it's armed again; a re-armed camp resets at the next daily check for sure, so it's back the next day you're away. A banner of another kind doesn't count.
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

- Camp mobs don't burn in daylight (otherwise a camp found by day is a pile of burning skeletons). Mixin `CampMemberMixin`.
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
- Milestone `CampsGameTest`: `bannersAreTheKey` (break every banner → disarmed, roster removal counted, never resets; put the kind's banner back → re-armed, resets at the next check), `otherBannersDontCount`, `outpostWithoutBannersSpawnsNoPillagers` (registered outpost box; banner gone → disarmed and `getMobsAt` has no pillagers; banner back → armed). Full milestone server suite 118/118.
- Client part `camps` (`CampsClientGameTest`), screenshots looked at: one camp of each kind built on a flat world from above and close (camp-skeleton/zombie/husk/stray/bogged-above/-close), `camp-roster` (skeletons round the fire, a lookout on the tower, the leader), and the nearest natural camps in seed 20261116 (`camp-natural-0..2`: a skeleton camp in a forest clearing, a bogged camp in a swamp). The survey numbers above print from the same part.
- Not covered by a test: a natural outpost losing its banners in a generated world (the test uses a hand-registered outpost box; the mixin path and the outpost discovery are the same code the natural case uses).

## Undead variants

Already reported separately: `docs/reports/undead-variants-report-2026-11-22.md` (commit fc85113).
