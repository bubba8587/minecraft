<!-- dte:A3 -->
# Backlog

Parked ideas and open items, newest first within each project. Agents add to it when the owner parks something; details live in the linked spec. What is built, being built or designed and unassigned is on the status board, `specs/STATUS.md`; once an item is built, delete it here.

## Bubba's Reworked Mobs
- **Biome creeper skins (built 2026-11-24, owner to confirm; `specs/creepers.md` "Biome skins"):** snow creepers come out near-plain white; the 32x sulfur skin uses vanilla sulfur's colours (CF32J has no sulfur texture); mixed-biome calls: swamp -> oak, windswept hills -> stone, wooded badlands -> red sand, frozen river -> snow, lush caves and deep dark stay vanilla, poplar for dappled forest. Before release: the Classic Faithful 32x Jappa credit and links on the Reworked Mobs Modrinth listing and player docs page.
- **Creeper ideas (owner, 2026-11-24, parked):** (A) per-biome creepers: built 2026-11-24 (`specs/creepers.md` "Biome skins"; look only; skins recoloured from the biome's tree trunk, or its ground block where there are no trees, by OKLch lightness match). (B) on flat ground with no cover, a creeper digs up to 2 natural blocks, stacks them into a little pillar and hides behind it. Only with `mobGriefing` on; it moves the blocks as they are, like an enderman or silk touch (grass stays grass, never dropped as dirt), which is why plains creepers get a grass skin (owner). Skins are vanilla's creeper recoloured pixel for pixel (`tools/creeper-biome-textures.py`), no redraw (owner). Build B after the cover-hugging rework lands.
- **26.4 Frostbites (owner, 2026-11-24, for the 26.4 port):** Frostbites as Sleeping Undead, frozen in ice in Ice Caves, and in Monster Camps in cold biomes. Already promised on posters as [Coming in 26.4] (`specs/copy-strings.md`).
- **Trader posts (owner to decide, `specs/monster-camps.md` "Trader posts"):** rarity (spacing 18 chunks, about 0.08 per km² near spawn in seed 20261116; commoner?); is the llama tether mixin OK (vanilla never despawns a trader llama tied to a fence, so the post's tether counts as the trader's lead)?; a village bell within 48 beats a post (vanilla): keep?; lit campfire (unlike dark camps): keep?
- **Flaky or failing tests seen 2026-11-24 (untouched):** `CryptWardsGameTest.cryptStatuesStandInEveryWing` failed with `-PwithMobs` in two builders' worktrees ("PAUPER: the statues split the crypt floor into 2 walkable areas") but passed on merged develop with `-PmilestoneTests -PwithMobs` (2026-11-24): flaky, watch it. Passed on rerun: `GiantSpiderGameTest.leavesItsPerchAndComesBackToFeed` ("only wandered 1.0"), and once with `-PwithGems` `CampsGameTest.exposedCampMobsSeekShade`.
- **Face-down sleeper shares** (builder's numbers, owner to playtest): 30% of crypt lying sleepers, 40% of lying shipwreck crew, 15% of resting dead (`specs/undead.md` "Face-down sleepers").
- **Creeper audit (2026-11-24, code-read only, not built), waiting on the owner's pick:** bugs: (1) sneaking or shooting from 7+ blocks kills every creeper safely, since the hit that starts the hiss doesn't count as "hit during the hiss" (`CreeperMind.attacked`); (2) the hiss target can switch mid-hiss to an animal or a mob; (3) ~~night watchers never despawn by day, which blocks the next night's watcher~~ fixed 2026-11-24 by the cover-hugging rework (they leave out of sight in the morning); (4) a walking jump or Speed I counts as "fast" (cutoff 0.24); (5) a projectile that hits counts twice; (6) creeper blasts chain-startle other creepers; (7) spawner creepers with custom data come out reworked; (8) charged creepers path 32 blocks, not 16; (9) a switched-off fabricator creeper wakes up hissing; tests fill the meter directly instead of hitting. Ideas: cats calm creepers, a short lunge during the hiss, a gunpowder source now that random spawns are off, bored watchers wander off, a tell at about 75 %.
- **Flaky test seen once (2026-11-24):** `enderman_game_test_out_of_order_builders_finish_half_built_cairn` (92 of 93 standing).
- **Flaky tests seen 2026-11-23 (owner's PC, passed on rerun):** camps fence-fault tests (`outpost_portal_room_can_be_reached`, `camp_fences_end_cleanly`, `outpost_ramp_can_be_climbed`) and `outpost_roster_and_penned_hoglins` (one leader found 1).
- **Giant spider pathing is erratic (owner playtest, 2026-11-23; parked by owner):** rapid turning on the spot and struggling to get round, worst at a low web between a short and a tall tree. Stopping partway down a trunk is fine (owner). Found so far: Stormie's Spiders can't path to a point on a wall (one-node dead-end paths → turning in place; fixed by wandering only to standable spots), and Stormie gives every spider a fixed 0.95 x 0.85 hitbox whatever its scale. Next ideas, unmeasured: accept a wander target only if its path has more than one node; wander less often (STROLL_INTERVAL 60 → vanilla 120); check whether LookAtPlayer/RandomLookAround turn the body. Measure turning per block moved in a trunk grid (hearing_field) before and after: giant was ~290°/block once, ~50-60°/block other runs, vanilla spider ~54.
- **Spiders** (`specs/spiders.md` Open): a unique giant drop, what giants do by day, a Stormie's wall/ceiling screenshot, milestone objectives without tests (giants only in old-growth taiga, spin cap, fire burns webs, grip break drops the animal, webs tear with age).
- **Orphaned block entities after a command-built Fabricator (2026-11-22):** the Reworked Mobs `factories` client part logs about 45 vanilla warnings ("Tried to load a DUMMY block entity ... found air", from `LevelChunk` loading) when the player teleports to sites built with `/ancient_fabricator place` (x 320, 480). It's still there after the audit fixes. Along with it come "mark a block for post processing ... not supported", from vanilla's sculk patch feature running on a `ServerLevel`. It looks like the command path only (`site.design().place(ServerLevel)` over chunks that aren't loaded yet, then those chunks generating with the site already registered), not natural worldgen. Harmless (the test passes and nothing looks wrong). To confirm: load a natural Fabricator in a fresh world and grep the log.
- **Bury-break system:** how buried loot comes out (shovel-only vs breaking by hand, noise, tools). Today: shovel digs it out loudly; breaking by hand also works, slowly like packed dirt. `specs/undead.md`, Loot on display.
- Undead before release: hybrid rendering of sleeper bodies and real 16x/32x body art (`specs/undead.md`).
- **Reworked Mobs creeper cover-hugging, owner questions (2026-11-24, `specs/creepers.md` builder's choice 11 and Numbers "Closest peek spot"):** (1) should a creeper need to see the player to notice them approaching? Right now it reacts and runs off even when fully behind its cover or through a wall. (2) Is 3 blocks the right minimum distance for creeping closer, given a player standing at a window is often closer than that?
- **Creepers** (first build done): owner to playtest the numbers and review the 10 builder's choices in `specs/creepers.md`; try them in the test world's creeper grove.
- **Endermen** (first build done, `specs/endermen.md`): owner to playtest and review the 13 builder's choices and the 4 cairn-form choices; owner, 2026-11-22: explosions don't anger them, no banners, Nether/End endermen stay vanilla; seven cairn forms are built. Try them in the test world's Cairns and endermen station (hub click sign).
- **Flaky tests (2026-11-16, fixed; `specs/quality.md` "Test isolation"):** still unexplained, not seen since the fixes: the level's light engine standing still (`strongLightCalmsSleepersInWorld`; its failure message now dumps the light engine and worker threads), and a drowned freezing mid-water at path node 2 (`swimsToALurkSpotNearThePlayer`, 2 in ~90 single runs). Investigate if either shows up again.
- **Drowned** (first build done, `specs/drowned.md`): owner to playtest numbers and review the builder's choices; test world "drowned shore" station idea there.
- **Ancient Fabricators** (built 2026-11-17: owner to playtest the rate, the doorway spot and the Warden; test world station still to add; `specs/mob-factory.md`; one beside every ancient city): underground nether/warped/bone ceiling growth drops mobs onto an ancient-city redstone platform; open questions F1-F6. Also structures for farming: crypts, war camps, bigger dungeons with spawners (owner, 2026-11-16).
- **Infinite jukebox** (owner, 2026-11-16): in current snapshots; when we move to that release, make the Ancient Fabricator office jukebox loop with it.
- **Slime merging** (owner: decent idea, not now): slimes that bump into each other merge.
- **Blood moon:** dropped (owner, 2026-11-22: "we won't do blood moon").
- **Nether outposts (owner to decide, `specs/monster-camps.md`):** rarer or more common (now spacing 22 plus terrain rejection)? Let piglins hunt the penned hoglins (more vanilla, but the livestock dies; now they can't)? Keep netherite scrap in the room chest (weight 1 of about 90)?
- **Giant spiders (owner to decide, `specs/spiders.md`):** a prowling giant leaves its low cocoon unguarded (a player can cut the catch free while it's away): intended, or should it stay closer or guard? Giant tasks aren't saved: a giant reloaded mid-prowl roams again.
- **Monster camps** (`specs/monster-camps.md`): Owner to playtest spacing (10 chunks, ~6 camps per km²) and leader numbers. Natural forest camps sit among uncleared trees (leaves crowd the towers); worth a look. Builder's calls to review: resting dead follow the yard climate (husks/strays), bogged hide fully underwater.

## Bubba's Totems & Artifacts (C6)
- **Relic vaults (owner to decide, `specs/totems-artifacts.md` "Relic maps and relic vaults"):** crypt vaults are unnamed (no map leads there): should some carry a name (two places per name)?; the whole shred becomes a map only once in a player's inventory: OK, or unfold on right-click?; tuning: 35 % of noble crypts with a vault, 45 % totem chance per grand vault chest. Decided (owner, 2026-11-24): no vanilla `/locate` for vaults (skip; maps are the way).
- **Gathering statue ring size** (builder's numbers, owner to playtest): radius 1.0, 1.1 above the statue's base, so a side hopper catches it and a chest or hopper beside the statue is just cleared (`specs/totems-artifacts.md` "Statue ring").
- Warding open question (builder): should mobs holding a Totem of Warding pulse, and may woken undead pick one up (built as yes). `specs/totems-artifacts.md`.
- **Totem icon borders (owner, 2026-11-24: "better, but some of the borders are still borked"):** the precious-material icons' outlines are uneven on some totems (sheet: `build/art-review/now-totem-icons-16x-top-32x-bottom.png`). Look at each icon's outline at 16x and 32x against vanilla's and Classic Faithful's Totem of Undying (one clean dark outline shade, no gaps or doubled pixels where arms, crowns and emblems meet the body); fix in `tools/totems-textures.py` / `tools/warding-textures.py` and `tools/pixelart.py` (`render32` snap/inner outline). Evolve, don't redesign.
- **Statues:** every totem has an agent-made statue; person-made statue designs and the mod icon later (`docs/art-todo.md`).
- **Totems built 2026-11-16** (Keeping, Gathering, Fortification, Growth, Seeking; `specs/totems-artifacts.md`): owner to review the builder's calls (Seeking signature blocks vs a picker screen, Gathering in a beacon, Growth strength and name, loot); the Gathering texture reads like gingerbread (art row).
- **Totem ideas dropped** (owner, 2026-11-16): Fold (animal breeding), Stillness (no despawning).

## Bubba's Extra Recipes
- **Renewability (owner, 2026-11-24):** trades and barters don't count. Still open: a deepslate recipe (owner undecided). Candidates noted: lapis, crying obsidian, gilded blackstone, end stone/netherrack (dimension-only). Horse armor and sponge: skipped. The andesite quartz loop (+1 quartz per cycle) stays as is (owner).
- Person-made mod icon (none yet).
- Real Glider art (16x and 32x, made by a person).
- **Glider durability:** playtest 324 (elytra 432). No broken icon (owner, 2026-11-24: skip).

## Bubba's Infused Gems
- **26.4 Freezing gem (owner, 2026-11-24, for the 26.4 port):** 26.4 adds a Freezing potion; make a craftable (brewed) Freezing gem from it. Already promised on posters as [Coming in 26.4] (`specs/copy-strings.md`).
- **Per-weapon tuning for gems in bows, crossbows, tridents and maces** (owner, 2026-11-14): they have no material, so they count as diamond level for now; each probably needs its own factor.
- **Drawback sizes** (owner will playtest): currently 10% each, under 20% stacked.
- **Tipped arrow vs gem arrow of the same effect** (owner: come back to it): now the higher level wins (a Poison II tipped arrow beats a longer, near-perfect Poison gem arrow); alternative is comparing total effect (level x time).
- **Turtle Master's Slowness stacking** (undecided, playtest): its Slowness part adds to other Slowness (a drunk Turtle Master potion, Slowness drawbacks) instead of following "stronger applies" like its Resistance part.
- **Perfect Invisibility set detection range** (owner: fine for now, revisit): real invisibility and the gems' stealth multiply, so mobs notice the wearer at about 11% of normal range (armor still shows).
- **Frost gem** (powder snow/strays): weapon hits slowly freeze the target; worn, walk on powder snow without freezing. Owner (2026-11-15): the next Minecraft drop adds a real freezing effect, so build Frost on that effect (and its potion, if it gets one) once we move to that version, instead of a custom one. (The other "gems beyond potions", Wind Burst, Weaving, Oozing, Silence, Ender and Breeze, are built: `specs/effect-gems.md`.)
- **Balance review, open design calls** (2026-11-15, see the review notes in specs/effect-gems.md): (1) PvP lock-down: decided (owner, 2026-11-22): behaviour stays as is (every hit applies); if cheap control gems (Blindness, Levitation, Nausea, Darkness) lock fights down in play, tune their duration/magnitude curves instead. (2) Gem bows: decided (owner, 2026-11-22): leave it (arrows keep the melee hit duration, at most 8 s, within vanilla tipped arrow range; tune only if bows play too strong). Also from the review: inverting can craft found-only effects as mains (minor); worn Infested is permanent (minor).
- **Silence on a weapon** (owner, 2026-11-16): 'quiet kills' idea (target makes no sounds/vibrations, can't call reinforcements) and deafening players are parked; Silence hits unchanged for now. See docs/research/effect-gems-strength-proposal.md Q20/Q21.
- **Revisit Breeze on a weapon** (owner): built as 'the target's own projectiles veer off'; revisit after playtesting.
- **Gem strength in beacons** (owner: approved for now): half a perfect armor set (`BEACON_SHARE` 0.5); revisit after playtesting.

## Bubba's Reworked Beacons (C5)
- Builder's choices awaiting the owner's look are marked in `specs/reworked-beacons.md`.
- Player docs: a gem-in-beacon screen screenshot (client test) once the beacon screen is final.

## Playtest fixes (owner, 2026-11-22)

Done 2026-11-23 (installed; light testing only: full server suites pass, few new tests): 1 camp room loot (one shared table), 2 full-block stair landings, 3 waking checks the whole body, 8 caretakers only lift natural blocks, 9 Clear Skies not used up, 10 Growth checks neighbours, 12 Fabricator ignore tag, 13 Fabricator runs like vanilla spawners and the natural world keeps mob spawning on, 14 natural world signs with fixed heights, 15 woken drowned become vanilla drowned, 16 camps about half as common, 18 crypt niches 2 deep, 4 worn Wither and other harmful worn rolls stay on gemmed mobs in normal play (owner); the test world Plague boss now wears Resistance instead of Poison. Done 2026-11-24: 5 (giants prowl off the perch and walk back to feed), 6 (giants haul to the centre of their own web). Partly done: 7 (mixed design seeds; tower, outcrop, pike and lean-to counts, stair side and turn, room width and offset vary), 17 (highland biomes added).

Left for next session:
- **7 rest:** skull rocks have two forms now (full and half-buried) plus ribcage arches, winding towers, ramps and bridges (2026-11-24). Still open: two-chamber rooms, camp loot themed per kind, screenshots of 6+ seeds per kind. The `camps` client part's `camp-room` camera still points at the old fixed room; the ribcage-bogged-front camera ends inside the camp's skull.
- **Camp pieces next** (`specs/monster-camps.md` "Big pieces"): bridges to outcrops or skull tops, ramps up rock outcrops, nether fossil templates as bone dressing near skulls, check ramps and towers on rough ground.
- **11:** the cairn yard and creeper grove backup copies still float at y 198. The quick fixes don't work: a far-away copy isn't loaded when the sign is clicked, and the builder pack is off in the finished world. Options: (a) structure templates (the yard is 51 wide, so two templates, plus copying the world's `generated/` folder), or (b) a small reset datapack left enabled.
- **17 rest:** level rough high ground for camps (cut and fill or `terrain_adaptation`) instead of skipping anything over 6 blocks of slope.
- **20 follow-up:** camp footprint plan done (2026-11-23); towers sometimes bunch on one side of a camp.
- **21 follow-up:** web hits follow the strands (done 2026-11-23); F3+B debug outline still shows the whole span box.
- **22 follow-up:** gravestone edit screen matches the stone (done 2026-11-23); its title still reads "Edit Sign Message".
- **23, lava geysers only near the player (owner, 2026-11-23):** vents erupt only within 24 blocks sideways, and from 16 below to 8 above a player (`GEYSER_SEARCH_RADIUS`, `LavaGeysers.checkAround`). So a player gliding high over a lava sea never sees eruptions to steer to. Proposal, awaiting the owner's yes: about 64 blocks sideways and the full height from the player down to the lava sea (section skipping keeps it cheap). Lava bubbles stay on vanilla's ambient particle range: within 32 blocks, denser within 16.
- **Tests:** milestone tests for 8, 12 and 13; a client check that clicks every natural-world sign; a screenshot of a crypt niche with the sleeper inside it.
- **Side effect to know:** mixing design seeds changed the form of every cairn, camp, crypt yard and Fabricator that isn't built yet, including half-built cairns in existing worlds.

## Not now (owner, 2026-11-24)
- World Tweaks environment ideas: rain puddles, mudslides, sandstorms, mountain wind, avalanches.
- Calming sleepers with a "lull" game event tag (light, bells and fire already calm them).
- Grave Talisman as a rare find on sleepers (it stays crypt-chest loot).
- Mob spawner tweaks.

## Repo
- **Flaky tests, unfinished (2026-11-24):** camps "fence faults" (ClippedBuild.tidyFences at chunk edges), Totems `CryptWardsGameTest.cryptStatuesStandInEveryWing`, Reworked Mobs `outOfOrderBuildersFinishHalfBuiltCairn` (92/93 once). A builder ran out of time (1 h); its unverified half-finished diff is in `docs/patches/flaky-tests-wip-2026-11-24.patch`. Next try: one test at a time, short loops.
- **Multiplayer check (owner, 2026-11-22: "make sure these mods work in multiplayer"; light scan done, the rest is here).** Scan results:
  - No client classes are used in common code.
  - All the per-thing state is keyed by level or entity (WeakHashMaps), not by a single player. The Seeking cache clears on server stop.
  - Every mod has an `environment: "*"` main entrypoint and a client-only client entrypoint.
  - The test world build already runs every mod together on a dedicated Fabric server, so server-side loading is proven.

  Still to do:
  1. **Client joining a dedicated server:** never tested. Add a client game test that starts a dedicated server with all our mods and joins it, or have a playtest on a LAN or real server with two players.
  2. **Two players at once:** check the per-player features with two real players:
     - one watcher creeper per player, each player's own meters;
     - Fabricator debuffs and the Warden;
     - Keeping and Seeking totems;
     - camp and nest triggers, a drowned or spider holding one player while another hits it;
     - endermen trading at cairns (one player's offering, another player nearby).
  3. **Threads:** the static WeakHashMaps aren't thread-safe. Confirm that each is only touched on the server thread, and that none is touched from the client thread in single player (e.g. `WornEffects.LAST_SNEAK` if it is ever reached from client-side entity ticks).
  4. **Player docs:** say which mods every player needs installed. All of them register content, so all are needed on client and server; vanilla clients can't join.
- **Compatibility tests before releases** (prior-art research, docs/research/prior-art-all-projects-2026-11-16.md): Reworked Beacons vs Beyond Basic Beacons / Potion Beacons (likely mark incompatible) and Underground Beacons / Extended Beacon Range (test); Warding pathing vs Lithium; Infused Gems' enchanting hooks vs Easy Magic; Glider turning vs Do a Barrel Roll; Totem of Keeping vs grave mods (dupes/loss).
- Test world stations for features that have none yet (list in `specs/testworld.md`, "No station yet"), when the owner wants to playtest them.
- Owner to read and ratify (or rewrite) the goal drafts A1-A4 (`python3 tools/dte.py unratified`).

## Bubba's Reworked Mining (C7)
- Built (`specs/reworked-mining.md`). Owner to playtest every number in `MiningTuning`, decide whether stronger Efficiency should beat vanilla at Efficiency V diamond, how progress shows to the player, and review the builder's calls (spec "Open").
- **Fist speed (built 2026-11-24):** playtest the builder's fist numbers (0.65 on soft blocks, 0.16 on stone and deepslate).
- **Wrong tool vs fist:** a tool on a block it isn't meant for (pickaxe on a log) mines at vanilla hand speed, a little faster than our slowed fist. Should it count as a fist too?

## Bubba's World Tweaks (C8)
- Built (`specs/world-tweaks.md`). Owner to playtest bubble and eruption frequency and the boost numbers.
