<!-- dte:C1,B15,B17,B19 -->
# Reworked Mobs: Ancient Fabricators (spec, design agreed 2026-11-16: first full build done 2026-11-17)

**Name (owner): Ancient Fabricators. Placement (owner): one next to every ancient city.**

Owner's idea (2026-11-16), to make mob farming possible again after the spawn cut, creeper watchers and cairn endermen: an **underground factory**. A drippy, spiky growth on the ceiling, mixing **nether, warped and bone** elements (like dripstone made of bone blocks, warped wart, basalt), **spits out mobs** onto a platform below built in an **ancient city + redstone** style; mobs that aren't dealt with are **teleported away after some time**. Earlier the owner mentioned doing "something really awesome with huge underground areas with our double paired beacons": the ceiling growth and the platform are natural halves of a paired beacon.

## Owner's design, round 2 (2026-11-16)

- **Mobs come out "deactivated":** they drop onto the platform limp, looking down, doing nothing, and then get **teleported away**. The factory is making mobs for something else; players farm it by killing them before they're sent off.
- **A passive Warden** watches the operation from an **observation area**. It is passive until it **hears you** trying to get to the machine or the beacon.
- **A paired beacon** over the factory gives several **debuffs in its area, especially Mining Fatigue**, and **sculk is everywhere**, so it's really hard to cheese your way in (tunnelling in is slow and loud).

## Owner answers, round 3 (2026-11-16)

- **F1:** a rare structure, **but not too rare**.
- **F2:** it runs on its own, **with some controls**: the **redstone controls the teleporter** (so a player who takes over can stop mobs being sent away).
- **F3:** **one mob type** per factory, with **a wall behind it showing that mob's face** in block art.
- **F4:** mobs vanish: they are really **teleported somewhere much farther up** (typically). They need **particles at their destination too**, where they then **"wake up"**.
- **F5:** rate needs playtesting.
- **F6:** taking it over, yes. A **hidden, inert spawner buried in the upper section** (the dripping ceiling growth) is what ultimately stops it: break that and the factory is done.
- F7, F8: not answered; the builder's suggestions stand (debuffs from a hidden core without Reworked Beacons, a real paired beacon with it; one vanilla Warden that's passive at its window until it hears you, no respawn).

- **F9 (owner):** they arrive **on the surface above and around the factory** (within ~96 blocks sideways), on dark ground at night like a natural spawn, in a burst of particles. So **seeing mobs teleport in with particles near you tells you a factory is under your feet**; stopping it thins the surface above.
- **F10 (owner):** once they wake they are **reworked mobs** (watcher creepers, caretaker endermen, reworked undead): the world's monsters, not spawner stock.

## Owner feedback on the first look (2026-11-16)

- The warped growth should form more **alien "chains"/spurs** that **curve up around the cavern's walls, then curl in and bend toward the central drop spot**.
- The **teleporter has a redstone-visible activation timer and particle effects even with no mob on it**, so a player could **hop on it to escape, with the right timing** (it sends a player up too).
- **Warped first (owner):** warped is the primary material, bone only a minor accent, **no dripstone**. **One central "dripper"** hangs precisely above the teleporter pad; any other drippers are much smaller. (The first look's warped character was better than the bone-and-spike second look, minus its shapeless blobs.)
- **Owner on the built look (2026-11-16): "really close"**; add **streaks of crimson** through the warped, and **less sculk** (it's a bit heavy).
- **Look approved (owner, 2026-11-16): "I like the look so far."** The floor beacon keeps its light; the face wall stays dark until you bring light.
- **Dark like an ancient city (owner):** too much light in the first looks; the pad and dripper glow out of the gloom, you need a torch to explore.
- **Sculk detailing follows real ancient cities** (owner): the same palette and patterns (patches with veins creeping up walls and edges, sparse catalysts and shriekers, sensors by walkways, cracking).
- The "timer" is **lamps that light up** in sequence, not an actual redstone clock (owner).
- The Warden's observation area is its **office**: **chests** (loot), and a **jukebox with a weird music disc that's always playing** (owner). Looping: vanilla's infinite jukebox is in current snapshots (owner); plan for it when we move to that release, no workaround now (it plays once until then).
- **Right before it fires, whatever is on the pad gets a bit of Levitation**, lifting slightly off the pad, then it teleports.

## Independence (first thoughts)

Lives in Reworked Mobs, built from vanilla blocks (B15). With Bubba's Reworked Beacons installed its two halves could also be a real paired beacon (to be designed); without it, the factory still works.

## The look, as built (visual pass, 2026-11-16; no mechanics yet)

**Code-built, deterministic.** `factory/FactoryDesign.design(seed, mob)` is a pure function of a seed and the factory's mob: about 54,000 block placements in local coordinates (origin = first air above the middle of the platform; +z the entrance, -z the face wall, +x the Warden's tower), rotatable in quarter turns. Ready for worldgen to place chunk by chunk later (not wired yet). Vanilla blocks only (B15); no new blocks, items or entities.

**Parts** (all numbers are local blocks):
- *Cavern:* a squashed dome, radius 21 with a 3-wave wobble, floor 8 below the deck (rising at the walls), ceiling 22 above the deck in the middle, 13 at the edge. A 2-block shell of deepslate (cobbled deepslate 12 %, tuff 6 %) seals it, so caves and fluids don't break in; rock faces touching the air are **sculk** (floor: about 80 %, walls and ceiling: patches), with **sculk veins** creeping over 16 % of the open cells next to rock. A winding entrance tunnel climbs out the front (+z), 5 wide, about 40 blocks long.
- *The roof is the Fabricator (owner):* a living warped growth that makes the mobs; the ancient teleporter below harvests its drip. **Warped first, bone only a minor accent** (owner, after the bone/dripstone version). A 4-6 block **mass** over the middle (radius about 15.5) of warped wart with veins of **stripped warped hyphae** (the teal flesh; plain warped stems/hyphae read maroon on camera, so they're avoided), basalt seams, shroomlight accents (3.5 %), the odd bone knot; a gently lumpy underside.
- *The dripper (owner: one dominant one, precisely over the pad):* a stalk hanging from the mass over the teleporter pad's middle: a warped wart collar with shroomlights where it leaves the mass (radius ~3.8), tapering (radius 3.4 x (1-t)^1.4 + 0.35) as a stalk of stripped warped hyphae with **shroomlight glands** in its core every 4 blocks and on its skin, ending in a single **crying obsidian tip 8 above the deck**. Crying obsidian drips purple tears from its underside on its own (vanilla), so the drip is real and falls on the pad. Eight short wart strands (some ending in shroomlight) hang round the lower stalk like a membrane. *(Coordinator's suggestion, tried first; an earlier "bulb" version (a swollen glowing bulb over a nozzle) read as a blobby trunk and hid the face wall, so it was dropped.)*
- *Small drippers:* 34 of the same anatomy at a much smaller scale (2-5 long, a wart collar, a teal stalk, a crying obsidian tip on about 45 %), spread over the roof, none within 5.5 of the dripper; short over the face wall and in the Warden's line of sight.
- *Chains (owner):* 9-11 **alien chains/spurs** of warped wart (a little nether wart and shroomlight) with **stripped-hyphae link rings** every few blocks and warped-stem thorns, each starting low on the cavern wall, climbing it, running along the roof twisting (so they spiral in) and **curling in to end round the dripper**. None start on the face wall, the tower or the tunnel mouth. *(They still read weakly among the growth; see choices.)*
- *The hidden spawner:* an empty (inert) vanilla `spawner`, buried 2 blocks up inside the mass right above the dripper's root, wrapped in stripped hyphae on all six sides. The build command prints where it is.
- *Sculk, grown the ancient-city way (owner: match real ancient cities):* vanilla's city pieces contain **no sculk at all** (checked: barracks, tall ruin, city centre, walls; their palette is deepslate, tiles, bricks, polished, chiseled, gray wool and carpet, candles, soul fire/sand, soul lanterns, redstone lamps and wiring) and are degraded by `ancient_city_generic_degradation` (30 % of bricks and tiles cracked, 5 % of blocks rotted away, 5 % of soul lanterns gone). Their sculk is the `sculk_patch_ancient_city` feature run over them afterwards: vanilla's real sculk spreader (10 charges of 32, veins left at the edges), a catalyst at half of the patches and 1-3 shriekers. The Fabricator does exactly that: the build is placed clean with **30 % cracked bricks and tiles**, then **vanilla's own `sculk_patch_ancient_city` feature is grown** from about 17 spots (deck corners, by the face wall, the stairs' foot, the window ledge, the tunnel floor, the cavern floor). Its shriekers are then set to can't-summon (one Warden). The teleporter pad and every redstone lamp are restored clear of sculk afterwards (the working parts stay readable). The rock lining keeps a little painted sculk only (floor ~45 %, walls in rare patches) under the grown patches. Not done (yet): the 5 % block rot (it would hole the deck).
- *Platform (ancient city + redstone):* 19 x 19 deck of deepslate tiles (cracked 18 %, chiseled every 4th), polished deepslate border and underlayer, upside-down tile-stair corbels under the edge; 2 x 2 deepslate-brick pillars to the floor with an **observer** staring out of each outer pillar; low tile walls along the sides and front (gaps for the entrance and the tower stairs); corner posts with **soul lanterns**; six **lit redstone lamps** set flush in the deck (each on a redstone block); gray candles; two **sculk shriekers** (can_summon false) by the face wall, sensors and a calibrated sensor; then grown sculk over all of it, as above.
- *Teleporter pad (at the drop point):* a **lodestone** ringed by **crying obsidian**, then the **timer ring**: eight redstone lamps going round clockwise from the back, with reinforced deepslate between. The owner wants a redstone-visible activation timer that is **lamps lighting up one by one, not a redstone clock**: the look shows it mid-count (five of eight lit). Under the soul-fire pit below the deck, soul fire smoulders on soul soil.
- *Paired beacon halves:* the **floor beacon** stands on a 3 x 3 **iron** inlay framed by polished blackstone bricks (gray candles at the corners), on the deck's left (x -6, z -1; off the face wall's sightline). Straight above, set into the growth, the **ceiling beacon** with its 3 x 3 iron pyramid above it, ringed by crying obsidian and polished basalt. The column between is kept clear. With Bubba's Reworked Beacons installed, the ceiling beacon is placed **upside down** (its `upside_down` state, looked up by name: no compile or runtime link) and the pair shows its beam (seen in the screenshots); without it, both are plain vanilla beacons with no beam.
- *Face wall:* behind the deck, a 3-block-thick deepslate brick wall to the ceiling; the mob's face, **16 x 16 blocks (its 8 x 8 face texture at 2 blocks a pixel)**, in a polished deepslate frame with chiseled corners, **lit by redstone lamps set in the frame** (4 along the top, 3 down each side, powered from behind), a tile-slab shelf under it and soul lanterns on chains either side. Faces come from `tools/factory-faces.py`: it reads the vanilla mob texture, posterizes it into 2-4 tones (k-means, contrast stretched), maps each tone to the nearest flat-looking vanilla block (concrete, terracotta, polished stones; no bricks, whose seams fight the pixels), and gives the glowing eyes one glowing block. Output: `factory/FactoryFaces.java` (generated). Mobs: **zombie** (moss/green concrete/cut copper, black eyes), **skeleton** (calcite/polished diorite, polished deepslate eyes and mouth), **creeper** (lime/oxidized cut copper, black face), **spider** (polished deepslate and black, eight **magma block** eyes), **enderman** (black concrete, **crying obsidian** eye bars).
- *Warden's tower (observation area, "the office", owner):* a deepslate-brick tower from the floor to the ceiling on the +x side (7 x 9), tile bands; a room inside (5 x 7 x 5) laid out as the Warden's **office**: gray wool floor under gray carpet, **three chests** (filing) along the back wall with a candle, a **desk** (lectern, a polished deepslate slab desktop with a candle, a tile-stair chair), a **jukebox playing music disc 13** in the corner on and beside wool, a sculk catalyst grown in at one corner, soul lanterns on chains and a lit lamp in the floor; a **5-wide, 5-tall open window** framed in chiseled deepslate looking over the deck; a ledge on upside-down stairs with tile walls and soul lanterns; stairs down to a walkway beside the deck. The **Warden** stands just inside the window (local 15, 6, 0), looking out over the platform. For now the command spawns it with **NoAI**, persistent (look only).

**Command (ops):** `/bubbas_reworked_mobs ancient_fabricator here <mob> [seed]` builds one with the platform's middle at your feet; `ancient_fabricator build <pos> <mob> <seed> <turns>` at a position, turned 0-3 quarter turns. Mobs: zombie, skeleton, creeper, spider, enderman. Not saved as a site yet (mechanics).

**Cost:** working out a design is one pass over a 69 x 48 x 87 grid plus the shell check; placing is ~54,000 `setBlock` calls with no neighbour updates (well under a second on the test machine), then a pass to connect walls.

### Look round 2 (crimson streaks, less sculk, chains)

Adds to `docs/reports/factories-docs-2026-11-16.md` (merge both into `specs/mob-factory.md`). Still look only, no mechanics.

## Owner's feedback this round
"That's really close actually." Two changes: crimson streaks through the warped, and less sculk. Also done: the chains now read as chains. A jukebox hopper loop was asked for and then cancelled by the owner, because vanilla's infinite jukebox is coming in current snapshots. Nothing was added for it, and the office jukebox stays as it was (disc 13 in it, playing when placed).

## Changes (as built)

**Crimson streaks:**
- Crimson runs as thin streaks through the warped roof mass, the dripper's skin and the chains.
- *How:* streaks sit where two seeded wavy sheets both come near zero. They cross in lines, so crimson shows as veins, never patches.
- *Blocks:* nether wart block (50 %), stripped crimson hyphae (35 %, the red flesh), crimson hyphae (15 %).
- Warped stays primary. The function is `FactoryDesign.crimsonStreak`.

**Less sculk:**
- Vanilla's `sculk_patch_ancient_city` now grows from **7 spots** instead of about 17:
  - 2 on the deck (back-left corner, front-right)
  - 4 on the cavern floor
  - 1 in the tunnel
- No patch on the stairs or the window ledge any more.
- The painted sculk on the rock lining is thinner too: floor about 30 % instead of 45 %, walls rare.
- The deck now reads as ancient-city stone with sculk creeping in from the edges.
- The pad and the lamps are still kept clear.

**Chains:**
- 11-13 of them. Each is now a **thin strand** (about one block thick) of warped wart, threaded every ~3 blocks with a **knot** (a ball of stripped warped hyphae about 2.5 blocks across). They read as links on a chain.
- **Every fourth knot is crimson.**
- They start low on the cavern wall and climb it. Then they hang **free under the roof**, at least 1.5 below it and sagging up to 2.5 more between wall and dripper. They twist as they go and end at the dripper's stalk.
- The thorn spurs are gone, since the beaded links read better.

## Screenshots (`docs/previews/factories/`, refreshed)
The same set as round 1 (the cavern-floor close-up is still left out). Best views of this round's changes:
- crimson streaks: `fabricator-overview-lit.png`, `fabricator-side-lit.png`
- chains: `fabricator-chains-lit.png`
- lighter sculk: `fabricator-platform.png`, `fabricator-sculk-deck.png`

## For the mechanics phase
- **Office jukebox:** plan on vanilla's infinite jukebox (in current snapshots) instead of a workaround. Until then, the disc plays once when the Fabricator is placed.

### Lighting pass (dark like an ancient city)

Adds to `factories-docs-2026-11-16.md` and `factories-look2-2026-11-16.md`. Look only.

## Owner's feedback
Too much light. A Fabricator should be dark like an ancient city: the pad and the dripper glow out of the gloom, and you need a torch to explore.

## Light sources now (as built)
- **Dripper (main glow above):**
  - its thin lower half is a beaded spine with a shroomlight every other block;
  - rings of shroomlight glands (40 %) on the thick upper skin, and a core gland every 4 blocks;
  - a crying obsidian tip (light 10) over the pad.
  - The chains now stop just short of the stalk (a 1.9-block clear cylinder) so they don't bury it.
- **Teleporter pad (main glow on the deck):** the lamp-ring timer (5 of 8 lit, light 15 each), plus its crying obsidian ring (light 10).
- **The paired beacons:** each is light 15 (vanilla). The floor beacon lights its own corner of the deck.
- **Accents, kept faint and sparse:**
  - roof mass: shroomlight 3.5 % → 0.3 %; none in the chains or the dripper's collar and strands;
  - small drippers: crying obsidian tips 45 % → 20 %;
  - soul lanterns: only the deck's 2 front corner posts, 1 in the office, 1 on the window ledge (was 10 in all);
  - candles: 2 by the floor beacon, 2 by the entrance, 2 in the office;
  - soul fire in the pit under the deck: about 25 % less;
  - sculk's own faint glow.
- **Switched off:** the deck's 6 redstone lamps and the face frame's 10 lamps (still placed, unlit, no power under them), the 2 lanterns hanging by the face wall, the lamp under the Warden.

## Block light measured (client test prints `[fabricator-light]`, zombie Fabricator, seed 1000)

| Spot | Block light |
|---|---|
| pad middle | 12 |
| deck centre | 13 |
| beside the floor beacon | 14 |
| deck front edge (by the entrance candles and lanterns) | 11 |
| deck back-left / front-right | 7 / 7 |
| under the dripper tip | 11 |
| window ledge | 7 |
| office | 6 |
| face wall middle | 2 |
| cavern floor west (soul fire nearby) / east | 5 / 0 |
| tunnel | 1 |

The deck's middle and the pad are bright. The face wall, the cavern floor, the tunnel and most of the roof are 0-5, so hostile mobs could spawn there (vanilla needs block light 0). The face reads faintly from the deck and properly with a torch.

## Screenshots
- All shots in `docs/previews/factories/` are now taken **without night vision**, as in play.
- Exception: one detail shot, labelled `fabricator-overview-nightvision.png`.
- The old `-lit` shots are gone; `fabricator-side.png` and `fabricator-chains.png` replace them.

## Choices to hear about
- The **floor beacon's own light (15)** keeps one corner of the deck bright. It's part of the paired beacon (the debuff source), so I left it.
- **The face wall is dark until you bring light.** If the owner wants it to loom out of the dark, two lit lamps at the top of its frame would do it.

### Builder's choices

0. **Chains read weakly:** they're there (warped columns with teal link rings climbing the side walls and arching in to the dripper), but in the warped roof they read as more growth rather than distinct chains. A dedicated pass could make them thinner and longer with clearer links.
0. **Dripper block:** crying obsidian tip (real purple drips). Alternatives if it doesn't read: a shroomlight tip (glowing, no drip), or a sculk catalyst tip (ties it to the sculk; blooms when things die nearby).
1. **Mob types:** zombie, skeleton, creeper, spider, enderman (the faces read best; witch, husk, stray are easy to add with the tool).
2. **Iron pyramids and two beacons are real loot** (18 iron blocks, 2 beacons = 2 nether stars) for whoever conquers a factory. Could be swapped for something cheaper, or the beacons made to drop nothing, if that's too rich.
3. **The floor beacon sits off to the left of the deck** (not in front of the face), so the beam never blocks the face.
4. **The drop point is the teleporter pad** (deck middle, a little forward): mobs will fall onto the pad and be sent from it, so the dripper hangs precisely over it and the chains curl in to it.
5. **Reinforced deepslate in the pad ring** (unbreakable, as in ancient cities' portal frame). Taking over doesn't need to break it.
6. **Shriekers can't summon** (one Warden per factory, F8).
7. **Factories sit with the deck at y -38** in the screenshots; real placement depth is for the worldgen step.
8. **Rare blocks used as decoration** that a player could mine: lodestone (1), crying obsidian, redstone blocks (under lit lamps), iron blocks. Fine for a dangerous structure, in my view.

### Recorded for the mechanics phase

- **Placement: one next to every ancient city** (owner, 2026-11-16). Plan: a worldgen step after structures finds each ancient city start in the chunk's region (vanilla `minecraft:ancient_city` starts, so `/locate` for cities finds fabricators too), picks a side of the city's bounding box away from its centre, and places the Fabricator there with its deck at the city's floor level (city floors are at y -51; the cavern is ~32 deep and ~44 wide, so it sits beside the city, not over it). Its entrance tunnel is turned to face the city and runs to the city's outer wall, where the build cuts a doorway-sized **gap in the city's wall** (deepslate brick frame, like the city's own arches). Built chunk by chunk from the pure design (it's already deterministic). In the look pass the tunnel winds out the front; turning it toward a city is a parameter, not a redesign.
- **Office chests' loot (plan):** "the factory's paperwork": paper, books and a book and quill, a map to the nearest other Fabricator's city, echo shards, sculk, disc fragments 5 and a chance of a music disc (13 or 11), candles, a few iron and redstone, and sometimes an enchanted book with Swift Sneak (ancient-city flavour). Loot tables come with mechanics.
- **The jukebox (vibrations):** a playing jukebox gives off vibrations (vanilla's `jukebox_play`, and sensors near one keep resonating) that sculk sensors, shriekers and a Warden react to. As built, it stands on wool with wool beside it and the office floor is wool under carpet, and the office's own sculk sensor was removed (wool doesn't stop vibrations through the air in vanilla; it only blocks them as a block in the path). For mechanics: the passive Warden's "hearing" filter ignores vibrations whose source is its own jukebox (or any non-player source while passive, which is the plan anyway); shriekers only answer players in vanilla, so the music can't set them off. If sensors on the deck twitch to it that's ambience; if it's too much, the jukebox gets "always playing" by our code restarting it silently (`setSongItemWithoutPlaying` + a client-side loop) rather than vanilla's play event.
- **Music disc 13** chosen (eerie ambient cave sounds, long, loops naturally); vanilla stops it at the end, so "always playing" is a mechanics item (restart it when it ends, while a player is near).

- The teleporter's **activation timer is lamps lighting up** (the eight-lamp ring), not a redstone clock.
- Its **particles run even with nothing on the pad**, so a player could hop on and ride it up to escape with the right timing.
- **Right before it fires, whatever is on the pad gets a bit of Levitation** (lifted slightly off the pad), then teleports: a deactivated mob, or a player who hopped on, rises a little as the timer completes.

### Left (mechanics)

Worldgen placement (sites from the seed, the feature that builds a factory chunk by chunk, saved command sites, `factory locate`), dropping deactivated mobs, the teleporter (timer lamps, particles, levitation, sending up to the surface, waking reworked), redstone control, breaking the spawner, the passive Warden (hearing, no digging away), debuffs (payload through Reworked Beacons' API, or a core without it). Server game tests for those (`ReworkedMobsCoreGameTest` smoke tests, `MobFactoryGameTest`) come with the mechanics.

### Test world station idea

A zombie Ancient Fabricator built with `ancient_fabricator build` under the hub (deck at y -38), reached by a ladder shaft with a sign; the tour photographs the overview and the face wall.

**Spare parts (owner, 2026-11-16):** the office chests have a chance of the gems that power the debuff beacon. With Infused Gems installed, the paired beacon runs on a real harmful gem (Mining Fatigue first; taking it out ends that debuff); without it, a hidden core does the debuffs and there are no gems.

**Loot (owner, 2026-11-16): fine as built** (18 iron blocks and 2 beacons per Fabricator).

## Mechanics, as built (2026-11-17)

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
0. **It rests between counts** (owner, 2026-11-16: "a much longer cooldown, not a constant stream, so people don't immediately know what it does"): lamps dark, a faint shimmer over the pad, nothing drops. Each second it wakes with a 1-in-60 chance (about a minute on average; B19: a chance, not a fixed cooldown). A site starts resting when a player arrives, and rests again after every firing. Forced sites (game tests, client shots) never rest.
1. At the start of each count the dripper drops its mob. A ward over the drop point (a Totem of Warding, e.g. in the floor beacon) stops the drops; the teleporter keeps running, and wards leave deactivated mobs alone (owner, 2026-11-16: "shut down spawn, not teleporter"; Totems & Artifacts asks `api/Fabricators.isDeactivated`). Particles (purple tears, sculk soul) and a bloom sound mark the tip.
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
6. **Rate:** about one mob per 77 s (a minute's rest on average plus the 17 s count) while a player is near.
   Leaving and arriving (owner: "underwhelming"): a dense portal cloud over the body, a ring of reverse-portal particles blown out along the ground, witch sparkle and sculk souls, 24 end-rod streaks shooting up toward the dripper (16 falling in on arrival), the enderman teleport sound low and a high beacon-off sound.
7. Peaceful stops drops, and so does the `spawn_monsters` rule (except on the game test server).

**Redstone (F2): powered = stopped**, like a locked hopper.
- The control is the pad's **lodestone**: power it (a lever, a torch beside it, a redstone block under it). The count doesn't run and the lamps stay dark.
- The dripper keeps dropping at the resting chance (1 in 60 a second), so they **pile up deactivated on the deck, up to 12** (the cap applies always).
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

### Builder's choices (mechanics)

1. **Powered stops the teleporter** (like a hopper). The lodestone is the input.
2. **Starting rate:** a rest of about a minute (1 in 60 a second) then a 17 s count, pile cap 12, 2 s wake, 96-block reach.
3. **Placement:** 150 blocks from the city, deck at -50. The tunnel cuts into the city's outer ring; the doorway 96 from its centre is a guess at where the city's edge is. The first real one looks right in screenshots (a deepslate city wall beside the door), but the owner should walk one.
4. **Two-layer pyramids** so an Infused Gems beacon gem isn't stuck at Gem Power I: 68 iron blocks per Fabricator.
5. **Dropped mobs keep sculk quiet?** No: falls are vibrations, so deck sensors twitch. Shriekers only answer players; the passive Warden hears nothing.
6. **A deactivated mob in daylight:** it can't be sent up (no dark spot), so it waits on the pad. Undead deactivated on the deck are underground, so no burning.
7. **Players riding up** get no darkness rule and land on any solid surface spot within 96: an escape route.
8. **Natural Fabricators are generated only in new chunks.** In an existing world, Fabricators appear beside cities whose surroundings haven't generated yet.
9. **Office chests, the Warden and the beacon item** are set up when worldgen places them (loot) or the first time a player comes near (Warden, item, jukebox).
10. **Creative players** neither wake the Warden nor get debuffs. Spectators don't make it run.

## Milestone test objectives (mechanics)

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
- `MobFactoryGameTest.officeGemsCarryTheirEffect` (milestone): with Infused Gems, office-chest gems carry their effect component (bug 2026-11-16: the gem entries used the pre-26.x `functions` key, which 26.3 ignores, so they came out blank).
- **Not yet tested (objectives):**
  - a natural (unforced) site rests before its first drop and between counts; a ward over the drop point stops drops while a deactivated mob already on the pad is still lifted and sent (needs Totems & Artifacts + Reworked Beacons in the test run, or a stand-in ward through `api/Endermen.keepOut`);
  - a player riding the pad lands on the surface;
  - the Warden ignores its jukebox after waking;
  - a woken Warden never digs;
  - the heart / Infused Gems gem in the beacon (with `-PwithBeacons`, and with Infused Gems), and taking it out ends the debuffs;
  - office loot with and without Infused Gems;
  - a pile caps at 12;
  - the Fabricator survives a save and reload (sites, states, persistent deactivated mobs);
  - `ancient_fabricator locate`;
  - a natural Fabricator near every city in a large area.

## Parked

- A "totem of spawning"/lure for beacons (Totems & Artifacts, held or in a beacon like wards; keeps mod independence): maybe later.

## Milestone test objectives

None yet (not built).
