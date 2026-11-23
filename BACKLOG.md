<!-- dte:A3 -->
# Backlog

Parked ideas and open items, newest first within each project. Agents add to it when the owner parks something; details live in the linked spec. What is built, being built or designed and unassigned is on the status board, `specs/STATUS.md`; once an item is built, delete it here.

## Bubba's Reworked Mobs
- **Endermen trading at cairns** (owner, 2026-11-22; designed, unassigned; `specs/endermen.md` "Trading at cairns"): lay anything on a cairn's trade pad, an enderman takes it once you're away and returns with something random, sometimes good, sometimes weird. To be built with it: sneak + middle click places the held item on the ground anywhere (survival players placing items on display). Still open: the return table, the pad design, the return delay.
- **Spiders** (`specs/spiders.md` Open): a unique giant drop, what giants do by day, a Stormie's wall/ceiling screenshot, milestone objectives without tests (giants only in old-growth taiga, spin cap, fire burns webs, grip break drops the animal, webs tear with age).
- **Why challenge a Fabricator? (owner raised 2026-11-16, then skipped the options for now).** Today: two beacons, iron pyramids, office loot, the perfect gem in the floor beacon, farming limp mobs. Options offered: it feeds its region's surface spawns of its mob type (break it, that mob gets rare round there; recommended), capture it as a farm (powered lodestone), a prize found only there (at the intake chamber), office paperwork leading to the next Fabricator. Loot buffed 2026-11-22 (owner: rewarding, Warden stays vanilla). `specs/mob-factory.md`.
- **Orphaned block entities after a command-built Fabricator (2026-11-22):** the Reworked Mobs `factories` client part logs about 45 vanilla warnings ("Tried to load a DUMMY block entity ... found air", from `LevelChunk` loading) when the player teleports to sites built with `/ancient_fabricator place` (x 320, 480). It's still there after the audit fixes. Along with it come "mark a block for post processing ... not supported", from vanilla's sculk patch feature running on a `ServerLevel`. It looks like the command path only (`site.design().place(ServerLevel)` over chunks that aren't loaded yet, then those chunks generating with the site already registered), not natural worldgen. Harmless (the test passes and nothing looks wrong). To confirm: load a natural Fabricator in a fresh world and grep the log.
- **Totems ward test flaky (2026-11-22):** `ward_stops_spawns_and_sends_monsters_out` fails about 2 in 10 runs (a zombie stuck 3 blocks up inside the ward), `held_totem_pulses_and_scares_monsters` failed once. The Fabricator flakes were fixed 2026-11-22 (fixed site origins, counted forced chunks, cleanup on failure); watch that they stay gone.
- **Bury-break system:** how buried loot comes out (shovel-only vs breaking by hand, noise, tools). Today: shovel digs it out loudly; breaking by hand also works, slowly like packed dirt. `specs/undead.md`, Loot on display.
- Calming sleepers (owner, 2026-11-15): a "lull" game event tag that calms sleepers (light calming and loud bells are built, fire as a weak ward is being built: `specs/undead.md`).
- Mob spawner tweaks. `specs/undead.md`.
- Undead before release: hybrid rendering of sleeper bodies and real 16x/32x body art (`specs/undead.md`).
- **Creepers** (first build done): owner to playtest the numbers and review the 10 builder's choices in `specs/creepers.md`; try them in the test world's creeper grove.
- **Endermen** (first build done, `specs/endermen.md`): owner to playtest and review the 13 builder's choices and the 4 cairn-form choices; owner, 2026-11-22: explosions don't anger them, no banners, Nether/End endermen stay vanilla; seven cairn forms are built. Try them in the test world's Cairns and endermen station (hub click sign).
- **Flaky tests (2026-11-16, fixed; `specs/quality.md` "Test isolation"):** still unexplained, not seen since the fixes: the level's light engine standing still (`strongLightCalmsSleepersInWorld`; its failure message now dumps the light engine and worker threads), and a drowned freezing mid-water at path node 2 (`swimsToALurkSpotNearThePlayer`, 2 in ~90 single runs). Investigate if either shows up again.
- **Drowned** (first build done, `specs/drowned.md`): owner to playtest numbers and review the builder's choices; test world "drowned shore" station idea there.
- **Ancient Fabricators** (built 2026-11-17: owner to playtest the rate, the doorway spot and the Warden; test world station still to add; `specs/mob-factory.md`; one beside every ancient city): underground nether/warped/bone ceiling growth drops mobs onto an ancient-city redstone platform; open questions F1-F6. Also structures for farming: crypts, war camps, bigger dungeons with spawners (owner, 2026-11-16).
- **Infinite jukebox** (owner, 2026-11-16): in current snapshots; when we move to that release, make the Ancient Fabricator office jukebox loop with it.
- **Slime merging** (owner: decent idea, not now): slimes that bump into each other merge.
- **Blood moon:** dropped (owner, 2026-11-22: "we won't do blood moon").
- **Monster camps** (`specs/monster-camps.md`): Owner to playtest spacing (10 chunks, ~6 camps per km²) and leader numbers. Natural forest camps sit among uncleared trees (leaves crowd the towers); worth a look. Builder's calls to review: resting dead follow the yard climate (husks/strays), bogged hide fully underwater.

## Bubba's Totems & Artifacts (C6)
- **Grave Talisman as loot:** found only (owner); in crypt chests now. Idea: a rare find on sleepers too.
- Warding open question (builder): should mobs holding a Totem of Warding pulse, and may woken undead pick one up (built as yes). `specs/totems-artifacts.md`.
- **Statues:** every totem has an agent-made statue; person-made statue designs and the mod icon later (`docs/art-todo.md`).
- **Totems built 2026-11-16** (Keeping, Gathering, Fortification, Growth, Seeking; `specs/totems-artifacts.md`): owner to review the builder's calls (Seeking signature blocks vs a picker screen, Gathering in a beacon, Growth strength and name, loot); the Gathering texture reads like gingerbread (art row).
- **Totem ideas dropped** (owner, 2026-11-16): Fold (animal breeding), Stillness (no despawning).

## Bubba's Extra Recipes
- Person-made mod icon (none yet).
- Real Glider art (16x and 32x, made by a person).

## Bubba's Infused Gems
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

## Playtest fixes queued (owner, 2026-11-22; plan only until the owner finishes playtesting)

1. **Camp lower room loot:** a small chest in each camp's underground room. Use a new loot table `bubbas_reworked_mobs:chests/camp_room`, modest and themed per camp kind (arrows and bones for skeletons, rotten flesh and iron bits for zombies, plus a little gold and an occasional book). Add it to `CampPiece` and the camp spec.
2. **Camp stairs end on a full block:** the bottom step of the room stairs must land on a full collision block. The bogged camp puts mud there, which is 14/16 tall, and mobs get stuck on it. Fix: whatever block the camp kind uses as its floor material, the landing under the last stair is always a full block (mud bricks or packed mud for bogged). Check every camp kind, and the crypt stairs too.
3. **Crypt wake-ups spawn mobs into roofs:** the wake spot checks the block at the feet but not the head. Fix: when a sleeper wakes (and in `SleeperWaking.stepOutIfStuck`), test the mob's whole bounding box with `level.noCollision(mob, box)` at the target, not one block. A lying sleeper in a one-high niche must step out to a spot with two clear blocks. Add a milestone test: a sleeper in a one-high crypt niche wakes standing in open air, never inside the ceiling.
4. **Worn Wither hurts its wearer every tick:** the owner noticed it on gemmed mobs. It's "not wrong, just really annoying for the test world." Vanilla Wither hurts every `40 >> amplifier` ticks, so a strong gem means damage every tick. Plan:
   - check that the worn Wither amplifier follows the gem curve as intended;
   - for mob gear in the test world, and probably for noble finery, roll only effects that don't harm the wearer (no worn Wither, Poison, Hunger, Instant Damage), keeping those for weapons.

   Owner to confirm whether that goes beyond the test world.
5. **A spider waiting on cocooned prey keeps moving:** while a giant spider (and any spider that cocoons) waits to come back and feed, it wanders around near the cocoon and its webs, instead of standing still. Use vanilla's random-stroll goal kept within a home radius around the cocoon (`WaterAvoidingRandomStrollGoal` plus `restrictTo`, specs/vanilla-subsystems.md), on walls and ceilings too through Stormie's Spiders. It still returns to feed on time.
6. **Spiders haul prey to the middle of their web:** a giant spider carries its catch to the centre of its own web, the middle of the spun-web span it made (between the trees it anchored to), and hangs the cocoon there. Today it uses the nearest web block. If it has no web of its own, the nearest web's centre; with none at all, as today. It needs the spider to remember its webs again: the 2026-11-22 cleanup removed an unused `sites` list from `GiantSpider`, which was what this needed. The cocoon hanging in the middle of the web should read clearly from below; check it in a screenshot.
7. **Camp variety (owner, 2026-11-22):** the test world's five camps look nearly identical.
   - **Cause:** `CampPiece` (and `FactoryDesign`, and the cairn design) seed vanilla's `LegacyRandomSource` directly (`RandomSource.create(seed)`), so close seeds (20261122..26 in the Showcase) give almost the same first rolls: tower angle, outcrops, pikes, lean-to. It's the same weakness the trading-pad fix solved. Natural camps get unrelated seeds, so they vary more, but they still share one kit of parts.
   - **Plan:**
     - (a) mix the seed before use (e.g. `RandomSupport.mixStafford13`), wherever a design is built from a site seed: camps, Fabricator designs, cairn designs. Note that this changes the layout of camps already registered but not yet built, which is fine in development.
     - (b) **owner's direction for more variety:**
       - **rock and bone caves:** large skull domes, inspired by Breath of the Wild's skull-rock camps; an original take in Minecraft blocks, not a copy;
       - **varying counts of every element:** towers, lean-tos, outcrops, pikes and so on.
       See specs/monster-camps.md "Layout variety".
     - (c) the test world then uses well-spread seeds.
8. **A Beginning beacon's endermen wrecked the beacon (owner, 2026-11-22):** the Totem of the Beginning in a beacon draws caretaker endermen to the area (`BeginningBeaconPayload` → `SleeperHook.drawEndermen`). One took a block from the beacon and broke it. Caretakers mend natural terrain against the worldgen target, so in the superflat test world a pyramid counts as "wrong" terrain.
   - **Fix:** caretakers never take or move:
     - a beacon or anything in `#minecraft:beacon_base_blocks`;
     - any block with a block entity;
     - blocks outside a "natural terrain" block tag (dirt, stone, sand, gravel and so on; a vanilla tag where one fits).
   - Also: the endermen's taking rules should never touch the beacon whose area drew them.
   - **Test:** a Beginning beacon on an iron pyramid in a superflat world keeps every block after its endermen have worked the area.
9. **Totem of Clear Skies shouldn't be used up (owner, 2026-11-22):** today, using it in rain ends the storm and consumes it (`TotemOfClearSkiesItem` `stack.consume`). That's too costly for what it does. Plan: it isn't consumed and gets a visible item cooldown instead (B19, the vanilla cooldown sweep), a few in-game minutes, so it can't hold the weather off forever. The builder proposes the length.

## Repo
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
     - endermen trading at cairns once it's built.
  3. **Threads:** the static WeakHashMaps aren't thread-safe. Confirm that each is only touched on the server thread, and that none is touched from the client thread in single player (e.g. `WornEffects.LAST_SNEAK` if it is ever reached from client-side entity ticks).
  4. **Player docs:** say which mods every player needs installed. All of them register content, so all are needed on client and server; vanilla clients can't join.
- **Compatibility tests before releases** (prior-art research, docs/research/prior-art-all-projects-2026-11-16.md): Reworked Beacons vs Beyond Basic Beacons / Potion Beacons (likely mark incompatible) and Underground Beacons / Extended Beacon Range (test); Warding pathing vs Lithium; Infused Gems' enchanting hooks vs Easy Magic; Glider turning vs Do a Barrel Roll; Totem of Keeping vs grave mods (dupes/loss).
- Test world stations for features that have none yet (list in `specs/testworld.md`, "No station yet"), when the owner wants to playtest them.
- Owner to read and ratify (or rewrite) the goal drafts A1-A4 (`python3 tools/dte.py unratified`).

## Bubba's Reworked Mining (C7)
- Built (`specs/reworked-mining.md`). Owner to playtest every number in `MiningTuning`, decide whether stronger Efficiency should beat vanilla at Efficiency V diamond, how progress shows to the player, and review the builder's calls (spec "Open").

## Bubba's World Tweaks (C8)
- Built (`specs/world-tweaks.md`). Owner to playtest bubble and eruption frequency and the boost numbers. More environment ideas (rain puddles, mudslides, sandstorms, mountain wind, avalanches) offered 2026-11-22, not picked yet.
