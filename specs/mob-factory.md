<!-- dte:C1,B15,B17,B19 -->
# Reworked Mobs: Ancient Fabricators (spec)

**Status: built** (look, mechanics, worldgen, secrets and lower city, office loot). Not built:
- a test world station;
- the looping office jukebox (waits for vanilla's infinite jukebox);
- Fabricators as real structures (designed, below).

Open items are at the end.

## The idea (owner)

- **Name:** Ancient Fabricators. **Placement:** one next to every ancient city.
- **Why:** to make mob farming possible again after the spawn cut, watcher creepers and cairn endermen.
- **What:** an **underground factory**. A living growth on the ceiling **spits out mobs** onto a platform below, built in an **ancient city + redstone** style. Mobs that aren't dealt with are **teleported away after a while**. The factory is making mobs for something else; players farm it by killing them before they're sent off.
- The ceiling growth and the platform are the two halves of a paired beacon (the owner wanted "something really awesome with huge underground areas with our double paired beacons").

### Owner's design calls

- **Deactivated mobs:** they drop onto the platform limp, looking down, doing nothing, and are then teleported away.
- **A passive Warden** watches from an **observation area** (its office). It stays passive until it **hears** a player going for the machine or the beacon.
- **A paired beacon** over the factory gives **debuffs in its area, especially Mining Fatigue**, and **sculk is everywhere**, so it's hard to cheese a way in (tunnelling in is slow and loud).
- **F1:** a rare structure, **but not too rare**.
- **F2:** it runs on its own, **with some controls**: **redstone controls the teleporter**, so a player who takes it over can stop mobs being sent away.
- **F3:** **one mob type** per factory, with **a wall behind it showing that mob's face** in block art.
- **F4:** mobs are **teleported somewhere much farther up**, with **particles at their destination**, where they **wake up**.
- **F5:** the rate needs playtesting.
- **F6:** it can be taken over. A **hidden, inert spawner buried in the ceiling growth** is what ultimately stops it: break that and the factory is done.
- **F7, F8:** not answered; the builder's suggestions stand: debuffs from a hidden core without Reworked Beacons, a real paired beacon with it; one vanilla Warden, passive at its window until it hears a player, never respawned.
- **F9:** mobs arrive **on the surface above and around the factory** (within about 96 blocks sideways), on dark ground like a natural spawn, in a burst of particles. Mobs teleporting in near a player are the sign a factory is underfoot; stopping it thins the surface above.
- **F10:** once awake they are **reworked mobs** (watcher creepers, caretaker endermen, reworked undead): the world's monsters, not spawner stock.
- **Teleporter timer:** a visible activation timer made of **lamps lighting up in sequence** (not a redstone clock), with particle effects even with no mob on it, so a player can **hop on to escape with the right timing** (it sends a player up too).
- **Lift:** right before it fires, whatever is on the pad gets a little **Levitation** and rises off the pad.
- **Look:**
  - **warped first**, bone only a minor accent, **no dripstone**;
  - **streaks of crimson** through the warped;
  - **one central dripper** precisely over the teleporter pad; other drippers much smaller;
  - **alien chains** that curve up the cavern walls, then curl in toward the central drop spot;
  - **less sculk** than the first look, and sculk detailing that follows **real ancient cities** (patches with veins creeping up walls and edges, sparse catalysts and shriekers, sensors by walkways, cracking);
  - **dark like an ancient city:** the pad and the dripper glow out of the gloom; a torch is needed to explore. The floor beacon keeps its light; the face wall stays dark until a player brings light.
  - Owner on the result: "I like the look so far."
- **The office:** **chests** (loot) and a **jukebox with a weird music disc that's always playing**. Looping waits for vanilla's infinite jukebox (in current snapshots); no workaround now, so it plays once.
- **Loot:** fine as built, then buffed ("there's a whole-ass warden, it should be rewarding"). The Warden itself stays vanilla.
- **Spare parts:** the office chests have a chance of the gems that power the debuff beacon.
- **Iron and beacons as loot:** the 68 iron blocks and two beacons are fine.

## Independence

Lives in Reworked Mobs, built from vanilla blocks (B15). With Bubba's Reworked Beacons installed, its two halves are a real paired beacon; without it, the factory still works (Debuffs, below).

## The look

**Code-built and deterministic.** `factory/FactoryDesign.design(seed, mob)` is a pure function of a seed and the factory's mob. It returns about 55,000 block placements (55,186 for the zombie test Fabricator, seed 1000) in local coordinates, rotatable in quarter turns:
- origin = the first air above the middle of the platform;
- +z is the entrance, -z the face wall, +x the Warden's tower.

Worldgen places it chunk by chunk (Mechanics, below). Vanilla blocks only (B15); no new blocks, items or entities.

**Cost:** working out a design is one pass over a 69 x 48 x 87 grid plus the shell check. Placing is about 55,000 `setBlock` calls with no neighbour updates (well under a second on the test machine), then a pass to connect walls.

### Parts

All numbers are local blocks.

- **Cavern:**
  - a squashed dome, radius 21 with a 3-wave wobble;
  - floor 8 below the deck (rising at the walls); ceiling 22 above the deck in the middle, 13 at the edge;
  - a 2-block shell of deepslate (cobbled deepslate 12 %, tuff 6 %) seals it, so caves and fluids don't break in;
  - rock faces touching the air carry a little **painted sculk** (floor about 30 %, walls rare), with **sculk veins** creeping over 16 % of the open cells next to rock.
- **The roof is the Fabricator (owner):** a living warped growth that makes the mobs; the teleporter below harvests its drip.
  - A 4-6 block **mass** over the middle (radius about 15.5) of warped wart, with veins of **stripped warped hyphae** (the teal flesh; plain warped stems and hyphae look maroon on camera, so they're avoided), basalt seams, rare shroomlight (0.3 %), the odd bone knot, and a gently lumpy underside.
  - **Crimson streaks** run thinly through the mass, the dripper's skin and the chains. They sit where two seeded wavy sheets both come near zero; those cross in lines, so crimson shows as veins, never patches. Blocks: nether wart block 50 %, stripped crimson hyphae 35 % (the red flesh), crimson hyphae 15 %. `FactoryDesign.crimsonStreak`.
- **The dripper (owner: one dominant one, precisely over the pad):** a stalk hanging from the mass over the middle of the teleporter pad.
  - Where it leaves the mass: a warped wart collar (radius about 3.8).
  - It tapers (radius 3.4 x (1-t)^1.4 + 0.35) as a stalk of stripped warped hyphae. The thick upper part has rings of shroomlight glands on its skin (40 %) and a core gland every 4 blocks; the thin lower half is a beaded spine with a shroomlight every other block.
  - It ends in a single **crying obsidian tip 8 above the deck**. Crying obsidian drips purple tears from its underside (vanilla), so the drip is real and falls on the pad.
  - Eight short wart strands hang round the lower stalk like a membrane.
- **Small drippers:** 34 of the same anatomy, much smaller (2-5 long, a wart collar, a teal stalk, a crying obsidian tip on about 20 %), spread over the roof, none within 5.5 of the dripper. Short over the face wall and in the Warden's line of sight.
- **Chains (owner):** 11-13 of them.
  - Each is a **thin strand** (about one block thick) of warped wart, threaded every ~3 blocks with a **knot** (a ball of stripped warped hyphae about 2.5 blocks across), so it reads as links on a chain. **Every fourth knot is crimson.**
  - Each starts low on the cavern wall and climbs it, then hangs **free under the roof** (at least 1.5 below it, sagging up to 2.5 more between wall and dripper), twisting as it goes, and ends just short of the dripper's stalk (a 1.9-block clear cylinder keeps them from burying it).
  - None start on the face wall, the tower or the tunnel mouth.
- **The hidden spawner:** an empty (inert) vanilla `spawner`, buried 2 blocks up inside the mass right above the dripper's root, wrapped in stripped hyphae on all six sides. The build command prints where it is.
- **Sculk, grown the ancient-city way (owner: match real ancient cities):**
  - How vanilla does it: the city pieces themselves contain **no sculk** (checked: barracks, tall ruin, city centre, walls). They are built clean, degraded by `ancient_city_generic_degradation` (30 % of bricks and tiles cracked, 5 % of blocks rotted away, 5 % of soul lanterns gone), and then the `sculk_patch_ancient_city` feature grows sculk over them: vanilla's real sculk spreader (10 charges of 32, veins left at the edges), a catalyst at half of the patches and 1-3 shriekers.
  - The Fabricator does the same: it is placed clean with **30 % cracked bricks and tiles**, then **vanilla's own `sculk_patch_ancient_city` feature is grown from 7 spots**: 2 on the deck (back-left corner, front-right), 4 on the cavern floor, 1 in the tunnel.
  - Its shriekers are then set to can't-summon (one Warden).
  - The teleporter pad and every redstone lamp are cleared of sculk afterwards, so the working parts stay readable.
  - The deck reads as ancient-city stone with sculk creeping in from the edges.
  - Not done: the 5 % block rot (it would hole the deck).
- **Platform (ancient city + redstone):**
  - a 19 x 19 deck of deepslate tiles (30 % cracked, chiseled every 4th), polished deepslate border and underlayer, upside-down tile-stair corbels under the edge;
  - 2 x 2 deepslate-brick pillars to the floor, with an **observer** staring out of each outer pillar;
  - low tile walls along the sides and front, with gaps for the entrance, the tower stairs and the lower-city stairs (west z 2..4, east z -9..-8);
  - corner posts, the two front ones with **soul lanterns**;
  - six **redstone lamps** set flush in the deck (unlit, nothing powering them);
  - gray candles; two **sculk shriekers** (can't summon) by the face wall, sensors and a calibrated sensor;
  - then the grown sculk, as above.
- **Teleporter pad (at the drop point, the deck's middle, a little forward):** a **lodestone** ringed by **crying obsidian**, then the **timer ring**: eight redstone lamps going round clockwise from the back, with reinforced deepslate between. Built showing five of eight lit (mid-count). Under the deck, soul fire smoulders on soul soil in a pit.
- **Paired beacon halves:**
  - The **floor beacon** stands on a 3 x 3 **iron** inlay framed by polished blackstone bricks (gray candles at the corners), on the deck's left (x -6, z -1, off the face wall's sightline).
  - Straight above, set into the growth, the **ceiling beacon**, with its 3 x 3 iron pyramid above it, ringed by crying obsidian and polished basalt.
  - Both pyramids have **2 layers** (3 x 3 + 5 x 5; the floor's lower layer is hidden in the deck): **68 iron blocks** in all.
  - The column between them is kept clear.
  - With Bubba's Reworked Beacons installed, the ceiling beacon is placed **upside down** (its `upside_down` state, looked up by name: no compile or runtime link) and the pair shows its beam. Without it, both are plain vanilla beacons with no beam.
- **Face wall:** behind the deck, a 3-block-thick deepslate brick wall up to the ceiling.
  - The mob's face is **16 x 16 blocks** (its 8 x 8 face texture at 2 blocks a pixel), in a polished deepslate frame with chiseled corners, with a tile-slab shelf under it. The frame holds 10 redstone lamps (4 along the top, 3 down each side), unlit, so the face stays dark until a player brings light.
  - Faces come from `tools/factory-faces.py`: it reads the vanilla mob texture, posterizes it into 2-4 tones (k-means, contrast stretched), maps each tone to the nearest flat-looking vanilla block (concrete, terracotta, polished stones; no bricks, whose seams fight the pixels), and gives glowing eyes one glowing block. Output: `factory/FactoryFaces.java` (generated).
  - Mobs:
    - **zombie:** moss, green concrete, cut copper, black eyes;
    - **skeleton:** calcite, polished diorite, polished deepslate eyes and mouth;
    - **creeper:** lime, oxidized cut copper, black face;
    - **spider:** polished deepslate and black, eight **magma block** eyes;
    - **enderman:** black concrete, **crying obsidian** eye bars.
- **The Warden's tower (the office, owner):** a deepslate-brick tower from the floor to the ceiling on the +x side (x 14..23, set into the cavern wall), with tile bands.
  - **Split-level office:** a double-height main floor by the window; a sunken **archive** (x 20..22, floor y 4) with the **three filing chests** in wall nooks (x 23, z -2/0/2; they hold the office loot); a **loft** (floor y 9) with a rail, wool, carpet and a lectern, reached by a stair along the north wall.
  - Furnishings: gray wool and carpet; a **desk** (a lectern, a polished deepslate slab desktop with a candle, a tile-stair chair); a **jukebox with music disc 13**; a sculk catalyst grown in at one corner; one soul lantern.
  - A **5-wide, 5-tall open window** framed in chiseled deepslate looks over the deck; under it a ledge on upside-down stairs with tile walls and a soul lantern; stairs lead down to a walkway beside the deck.
  - The **Warden** stands just inside the window, looking out over the platform (passive until woken: The Warden, below).

### Lighting (dark like an ancient city, owner)

**Light sources:**
- **The dripper** (the main glow above): its shroomlight glands and spine, and the crying obsidian tip (light 10) over the pad.
- **The teleporter pad** (the main glow on the deck): the lamp ring (5 of 8 lit, light 15 each) and its crying obsidian ring (light 10).
- **The paired beacons:** light 15 each (vanilla). The floor beacon lights its own corner of the deck.
- **Faint, sparse accents:**
  - roof mass shroomlight 0.3 %; none in the chains or the dripper's collar and strands;
  - small dripper tips (crying obsidian on about 20 %);
  - soul lanterns: the deck's 2 front corner posts, 1 in the office, 1 on the window ledge;
  - candles: 2 by the floor beacon, 2 by the entrance, 2 in the office;
  - the soul fire pit under the deck;
  - sculk's own faint glow.
- **Unlit:** the deck's 6 redstone lamps, the face frame's 10 lamps, and the lamp under the Warden. No lanterns hang by the face wall.

**Block light measured** (client test prints `[fabricator-light]` and `[fabricator] design blocks ...`; zombie Fabricator, seed 1000):

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
| intake chamber (soul fire, catalyst) | 6 |
| lower-city terrace (one soul lantern, one lit candle group) | 4 |
| causeway | 2 |

- The deck's middle and the pad are bright.
- The face wall, the cavern floor, the tunnel and most of the roof are 0-5, so hostile mobs could spawn in the darkest spots (vanilla needs block light 0).
- The face reads faintly from the deck and properly with a torch.
- Everything else in the secrets and lower city is unlit (unlit candles).

**Screenshots:** shots in `docs/previews/factories/` are taken **without night vision**, as in play, except those labelled `-nightvision` (detail views of dark spots). Best views: crimson streaks `fabricator-overview.png`, `fabricator-side.png`; chains `fabricator-chains.png`; sculk `fabricator-platform.png`, `fabricator-sculk-deck.png`.

### Builder's choices (look)

1. **Light:** the floor beacon's own light (15) keeps one corner of the deck bright (it's part of the paired beacon, so it stays). The face wall stays dark until a player brings light; two lit lamps at the top of its frame would make it loom out of the dark, if wanted.
2. **Dripper tip:** crying obsidian (real purple drips). Alternatives if it doesn't read: a shroomlight tip (glowing, no drip) or a sculk catalyst tip (ties it to the sculk; blooms when things die nearby).
3. **Mob types:** zombie, skeleton, creeper, spider, enderman (their faces read best). Witch, husk and stray are easy to add with the tool.
4. **The floor beacon sits off to the left of the deck** (not in front of the face), so the beam never blocks the face.
5. **The drop point is the teleporter pad:** mobs fall onto the pad and are sent from it, so the dripper hangs precisely over it and the chains curl in to it.
6. **Reinforced deepslate in the pad ring** (unbreakable, like an ancient city's portal frame). Taking over doesn't need to break it.
7. **Shriekers can't summon** (one Warden per factory, F8).
8. **Rare blocks used as decoration** that a player could mine: lodestone (1), crying obsidian, iron blocks. Fine for a dangerous structure, in the builder's view.

## Mechanics
<a id="mechanics-as-built-2026-11-17"></a>

### Worldgen: one beside every ancient city

- **Where:** sites are worked out from the seed, never stored.
  - For each ancient-city grid cell (vanilla's own `ancient_cities` placement, read from the structure set, so datapacks that move cities move Fabricators), the candidate city chunk is checked with the city's biome test: the biome at the chunk corner at y -27 must be in the city structure's biome set. Vanilla's test uses the centre piece's middle, so very rarely the two could disagree.
  - The Fabricator stands **150 blocks** from the city's centre (clear of the city's pieces, which reach 116) in a seeded direction, with the deck at **y -50** (city floors are about -51).
  - It is turned in quarter turns so its front faces the city. Its mob is seeded from the five.
- **The tunnel** runs from the front of the cavern straight (with a slight wander) toward the city's centre at deck level, easing down to the city floor, and stops 70 blocks from the centre.
  - It cuts its own gap through whatever of the city's outer ring is in its way (it is placed after the city).
  - A polished and chiseled deepslate **doorway** frames the passage 96 blocks from the city's centre.
  - The tunnel floor is deepslate tiles. Its sides are only touched where the world holds liquid (sealed with deepslate).
  - A hand-placed Fabricator (no city) has a winding entrance tunnel climbing out the front (+z), 5 wide, about 40 blocks long.
- **Placement:** a worldgen feature (`bubbas_reworked_mobs:ancient_fabricator`, every overworld biome, last decoration step, so after the city and the deep dark's sculk) places, per chunk, the part of the build inside that chunk.
  - Cost per chunk: a cheap seed hash and one biome lookup per nearby cell (cached), plus a rough distance test.
  - Only chunks near a cavern or its tunnel work out the design (cached for the last 6 sites).
  - Sculk patches grow from their seed chunk, as the cities' do. Chest loot tables are set at placement.
- **First run:** what worldgen can't do waits for the first time a player comes near (`Fabricators.prepare`, saved):
  - the patch shriekers are set to can't-summon;
  - the pad and its lamps are cleared of sculk;
  - the jukebox gets disc 13;
  - the Warden is spawned;
  - with Reworked Beacons, the floor beacon is loaded.
- **Verified in the client test (seed 20261116):** the Fabricator nearest spawn generated whole at (2515, -50, 850), and the structure manager confirms a real ancient city at its city spot. Screenshots `fabricator-worldgen`, `fabricator-city-door-nightvision`.
- **Saved data:** hand-placed Fabricators (`here`/`build`) are **saved with the world** (`bubbas_reworked_mobs:fabricator_sites`) and run like natural ones. Per-site state (prepared, Warden spawned, beacon loaded, broken) is in `bubbas_reworked_mobs:fabricator_states`.

### Commands (ops)

- `/bubbas_reworked_mobs ancient_fabricator here <mob> [seed]` builds one with the platform's middle at the player's feet.
- `ancient_fabricator build <pos> <mob> <seed> <turns>` builds one at a position, turned 0-3 quarter turns.
- Mobs: zombie, skeleton, creeper, spider, enderman.
- `ancient_fabricator locate` names the nearest one (natural or hand-placed) within 3000 blocks, its mob, and whether it still runs.

### Running

**When it runs:**
- while a non-spectator player is within 64 of the pad (checked every second; a site stops 2 s after its last player leaves);
- only while the `spawner_blocks_work` game rule is on and not on Peaceful, like vanilla spawner blocks. The `spawn_mobs` and `spawn_monsters` rules don't stop it, so it runs in worlds with mob spawning off, such as the natural test world.

**One cycle:**
1. **Rest** (owner: "a much longer cooldown, not a constant stream, so people don't immediately know what it does"): lamps dark, a faint shimmer over the pad, nothing drops. Each second it wakes with a 1-in-60 chance (about a minute on average; B19: a chance, not a fixed cooldown). A site starts resting when a player arrives, and rests again after every firing. Forced sites (game tests, client shots) never rest.
2. **Drop:** at the start of each count the dripper drops its mob, with particles (purple tears, sculk soul) and a bloom sound at the tip.
   - A ward over the drop point (a Totem of Warding, e.g. in the floor beacon) stops the drops. The teleporter keeps running, and wards leave deactivated mobs alone (owner: "shut down spawn, not teleporter"; Totems & Artifacts asks `api/Fabricators.isDeactivated`; every ward reaches the check through `api/Endermen.keepOut`).
   - The dripper stops dropping while 12 deactivated mobs are on the deck (the pile cap).
3. **The mob falls** from just under the crying obsidian tip (head clear of it), about 7 blocks onto the pad, **deactivated**:
   - its whole server AI step is skipped: no goals, brain, navigation or look/move/jump control;
   - silent, head hanging (pitch 60), never hurt by the fall;
   - it can still be hit and pushed.
4. **Count:** the 8 **timer lamps** light one by one, one every **2 s**, with a sculk click rising in pitch. **Portal particles** over the pad thicken with the count, and run with nothing on the pad too.
5. **Lift:** with all 8 lit, everything on the pad (5 x 5, 3 high; not spectators, not the Warden, nothing in `#bubbas_reworked_mobs:fabricator_ignores`) gets **Levitation I for 1.5 s**, with a beacon hum. After 1 s it **fires** and the lamps go dark.
6. **Where they go:**
   - **A deactivated mob** goes to a **surface spot within 96 blocks sideways**: at least 16 above the pad, the top block of a loaded, ticking column, passing vanilla's natural-spawn rules for that mob (placement and light: dark ground, so in practice at night), no player within 24, room to stand. 24 tries.
   - **No dark surface (daytime; owner: "send to dark spots"):** it goes to a dark spot under the surface in the same columns, still at least 16 above the pad (a cave, an overhang). Each tried column is scanned down from the surface for air over a solid block that passes the same spawn and player rules (`FabricatorMobs.darkSpotForMob`, 24 columns). Only with no spot at all does it stay on the pad for the next count. Undead waiting on the deck are underground, so they don't burn.
   - **A player** (or any other living thing) who hopped on rides up to a surface spot with standing room, any time of day, with no darkness rule: an escape route.
7. **Departure and arrival effects** (owner: the first version was "underwhelming"): a dense portal cloud over the body, a ring of reverse-portal particles blown out along the ground, witch sparkle and sculk souls, 24 end-rod streaks shooting up toward the dripper (16 falling in on arrival), the enderman teleport sound pitched low and a high beacon-off sound.
8. **Waking:** the arrived mob lies limp for **2 s** in a column of portal particles, then **wakes**. It was made with the `TRIGGERED` spawn reason, so `MobFinalizeSpawnMixin` leaves it reworked (watcher creeper, caretaker enderman, reworked undead).

**Rate:** about one mob per 77 s while a player is near (a minute's rest on average plus the 17 s count and lift).

### Redstone (F2): powered = stopped

Like a locked hopper.
- The control is the pad's **lodestone**: power it (a lever, a torch beside it, a redstone block under it). The count doesn't run and the lamps stay dark.
- The dripper keeps dropping at the resting chance (1 in 60 a second), so mobs **pile up deactivated on the deck, up to 12**.
- Deactivated mobs are **persistent** while deactivated (a pile stays when the player steps away) and lose that when they wake.

### The hidden spawner (F6)

Checked every half second while the site runs. When it's gone:
- the Fabricator is **stopped for good** (saved);
- the lamps go dark;
- everything deactivated on the deck **wakes up there**;
- the dripper shrieks with a burst of sculk soul.

The Warden and the debuffs carry on.

### The Warden (F8)

- One per Fabricator, spawned the first time it runs, in its office window. Persistent, never respawned.
- **Passive** means no AI at all: it can't move, dig or hear vibrations (vanilla Wardens with no AI hear nothing, so its own jukebox and the dropped mobs don't stir it). Every second it turns to watch the pad, or whatever stands on it.
- It **wakes** when a survival or adventure player:
  - **goes for the machine or a beacon without sneaking:** within 6 of the hidden spawner, 5 of the ceiling beacon, 4 of the floor beacon, or up in the roof (9+ above the deck, within 16 of the pad sideways);
  - **breaks a block in that zone** (even sneaking);
  - **hits it**.
- Awake, it has its AI back and anger 100 at that player (angry is 80): a vanilla Warden that **never digs away** (its dig cooldown is refreshed every second).
- Wardens don't hear a Fabricator office's jukebox (`WardenJukeboxMixin`), so a woken one isn't drawn back to the office by the music.

### Debuffs (F7) and the beacons

- The debuff area is a 2-layer pair's: **90 sideways**, 10 past both pyramids. Debuffs: **Mining Fatigue III, Weakness I, Slowness I**, 13 s, renewed every 4 s, ambient particles.
- **Without Reworked Beacons:** the floor beacon is the "core". While it stands, every survival or adventure player in the area gets the debuffs. Break it and they end.
- **With Reworked Beacons, without Infused Gems:** the floor beacon holds a **Fabricator Heart** (an echo shard, named, marked with custom data). Our payload (`compat/FabricatorBeaconPayload`, loaded only through that mod's entrypoint; `compileOnly`, `suggests`) gives the same three debuffs to players in the pair's area. Take it out or break the beacon and they end.
  - The heart works in any beacon, so a player can make a debuff beacon with it (taking over).
  - Small Reworked Beacons API it needed: `api/BeaconSlots.item/setItem` (fill a beacon's slot from another mod).
- **With Reworked Beacons and Infused Gems:** the floor beacon holds a real **perfect Mining Fatigue gem with a Slowness bonus** (Gem Power VI, capped to III by the 2-layer pyramid). It is built by name, with no link to Infused Gems, and Infused Gems' own payload gives it to everything in the area (B18). Take it out and that ends.

### Office loot

`loot_table/chests/fabricator_office`, in the 3 archive chests:
- **paperwork, 3-5 rolls:** paper, books, a book and quill, gray candles, redstone, sculk, disc fragment 5;
- **valuables, 2-4 rolls:** diamonds 1-4, emeralds 2-6, gold 2-6, iron 3-8, echo shards 2-4, experience bottles 3-8;
- **one treasure, always** (weights): Swift Sneak book (5), level 30-39 enchanted book (4), diamond chestplate, leggings, sword or pickaxe (3 each), enchanted golden apple, totem of undying, netherite upgrade template, recovery compass, ward trim (2 each), silence trim (1), disc 13 (2), disc 11 (1), Otherside (1).

**Spare parts (owner):** with Infused Gems installed, the chests use `fabricator_office_with_gems` instead. It loads only with that mod (a Fabric load condition) and is chosen in code by `isModLoaded`. It adds a **1-in-3** chance of a harmful gem: Mining Fatigue 3 / Slowness 2 / Weakness 2 (weights) at Gem Power III tier 2, or 1 the beacon's own perfect Mining Fatigue gem. Without Infused Gems, no gems.
- The pool's chance uses 26.3's form, `"condition": {"type": "minecraft:random_chance", "chance": 0.3333}`. The pre-26.x `"conditions"` list is ignored by 26.3 (the pool would roll every time); `lootFilesUseCurrentKeys` guards against it.

Office chests hold **no** Fabricator Explorer Map (owner); the map is only sold by cartographers (`specs/monster-camps.md` "Maps").

### Builder's choices (mechanics)

1. **Powered stops the teleporter** (like a hopper). The lodestone is the input.
2. **Starting numbers:** a rest of about a minute (1 in 60 a second), then a 17 s count; pile cap 12; 2 s wake; 96-block reach.
3. **Placement:** 150 blocks from the city, deck at -50. The tunnel cuts into the city's outer ring; the doorway 96 from the city's centre is a guess at where the city's edge is. The first real one looks right in screenshots (a deepslate city wall beside the door), but the owner should walk one.
4. **Two-layer pyramids**, so an Infused Gems beacon gem isn't stuck at Gem Power I: 68 iron blocks per Fabricator.
5. **Dropped mobs and the sculk:** falls are vibrations, so deck sensors twitch. Shriekers only answer players; the passive Warden hears nothing.
6. **Natural Fabricators generate only in new chunks.** In an existing world, Fabricators appear beside cities whose surroundings haven't generated yet.
7. **Set-up timing:** office chests get their loot when worldgen places them; the Warden, the beacon item and the jukebox disc are set up the first time a player comes near.
8. **Creative players** neither wake the Warden nor get debuffs. Spectators don't make it run.

## Secrets and the lower city
<a id="secrets-and-the-lower-city-owner-2026-11-16"></a>

The owner asked for "additional secrets and complexity (not to main mechanic, but to the generated area)": a reject pit, an intake chamber, buried chests, real ancient-city detail below the deck (it had been "3 large square platforms and some stairs"), and "a more dynamic Office, more stairs". The machine is unchanged.

- **Reject pit:** one chiseled deepslate tile off the deck's chiseled pattern (the tell) over a ladder into a sealed vault under the deck: bone blocks, soul soil, heads of the site's mob (zombie or creeper heads, else skeleton skulls; cobwebs at spider sites, obsidian at enderman sites) and a rejects chest.
- **Intake chamber:** over the dripper's root, inside the roof mass: a floor of soul soil, soul sand, sculk and bone; warped-stem roots running in to a teal hyphae knot right over the hidden spawner (still wrapped on all six sides); bone heaps, a sculk catalyst, one soul fire. Reached by twisting vines up a throat from the chain that runs closest under the roof near the dripper.
- **Lower city:**
  - a 5-wide causeway on piers from the tunnel's mouth to the deck's front gap;
  - a west terrace 4 below the deck (a ruined arch with a soul lantern, a broken wall with candles, a wool corner);
  - a small ruined hut;
  - a broken arch behind the face wall;
  - broken pillars, rubble, sculk veins;
  - stair flights of different widths;
  - three chests buried to their lids.
- **Split-level office:** see the Warden's tower, above.

**How it's built:**
- Passes in `design()`: `lowerCity()` (causeway, terrace, hut, behind the face wall, ruined pillars, rubble, `flights`, veins) and `rejectPit()` after `tower()`; `intake()` after `beams()`. All are hash-based (no draws from the design's random), so the sculk patch spots don't shift.
- Positions (local):
  - reject vault x 2..7, z -8..-2, floor y -7, interior y -6..-3; the tell at (3, -1, -5) with the ladder below it; reject chest at (6, -6, -3);
  - intake floor = spawner y + 1 (the knot is the top of the wrap); chamber radius about 6.5 (wobbled), 2-5 high, kept 4.2 from the beam column;
  - throat: the chain block 4.5-9 from the dripper with the smallest gap to the roof; vines run from it to the chamber floor (+1);
  - terrace top y -5, x -19..-14, z -6..7.
- Accessors: `rejectTell()`, `rejectChests()`, `buriedChests()`, `throat()`, `intakeFloor()`, `hut()`.
- **Loot** (`FabricatorLoot.REJECTS`, `FabricatorLoot.BURIED`; 26.3 `modifier` format):
  - `loot_table/chests/fabricator_rejects.json`: rotten flesh, bones, bone meal, string, spider eyes, gunpowder, arrows, an ender pearl, sculk; 1-2 damaged iron, chain or leather pieces, an iron sword or bow. Rare roll: nothing (4), heads, a name tag, golden apples, diamonds, enchanted damaged iron gear and bow, a worn diamond sword.
  - `loot_table/chests/fabricator_buried.json`: coal, bones, candles, soul torches, amethyst, glow berries, sculk, iron, echo shards, disc fragment 5, experience bottles; plus diamonds, emeralds, gold; then one of nothing (3), Swift Sneak book (3), level 20-30 book (2), enchanted damaged iron leggings (2), diamond leggings (1), golden apple (2), ward trim (1), disc 13 (1).

**Builder's choices (owner may change):**
1. **The tell is subtle:** one off-pattern chiseled tile. Grown sculk can creep near it but never replaces chiseled deepslate. If it's too hidden, a crack of sculk veins pointing at it would help.
2. **The causeway** turns the deck from "drop in, pillar out" into a walk-in. Easy to break it in the middle if the owner wants the jump back.
3. **Intake access is by chain and vines;** sneaking matters (the Warden's roof zone is unchanged).
4. **The reject and buried loot tables are provisional**, kept apart (`fabricator_rejects`, `fabricator_buried`) so they can be swapped once the owner decides what a Fabricator rewards.
5. **Prize spots,** if the owner wants one: the intake chamber (hardest to reach: chain climb, Warden-sensitive, right over the spawner), then the reject vault (secret), then the office loft (a lectern, nothing on it yet).

## Milestone test objectives

**Test isolation:** each test builds a whole Fabricator far from the test grid with forced chunks and runs it without a player. Every site has its own absolute spot (x 30000 + n x 256), forced chunks are counted per chunk, and the site, chunks, entities and players are cleaned up when the test is done, pass or fail (`TestCleanup`, `TestAreaCleanupMixin`).

- **Core** (`ReworkedMobsCoreGameTest`, body in `MobFactoryGameTest`): `fabricatorSendsItUpAndItWakesReworked`: the drop lands limp on the pad (looking down, unhurt, silent, no target), the lamps count, the lift, it's sent up, and it wakes as a watcher creeper.
- **Milestone** (`MobFactoryGameTest`):
  - `redstoneStopsTheTeleporter`: the count never starts, lamps dark, the mob stays, nothing sent;
  - `breakingTheHiddenSpawnerStopsItForGood`: once a mob has dropped, the spawner is broken; broken is saved, the deck mob wakes, no more drops;
  - `wardenWakesWhenItHearsAPlayerAtTheMachine`: one Warden, passive and persistent, wakes angry at a survival player at the floor beacon;
  - `debuffsWithoutReworkedBeacons`: Mining Fatigue III, Weakness, Slowness. Its Fabricator also carries the secrets checks: the tell is chiseled over a ladder; reject and buried chests carry their tables and the buried ones can open; the throat holds twisting vines; the intake is open; the spawner is where the design says and hidden on all six sides;
  - `officeGemPoolRollsOneInThree`: the office gem pool rolls about 1 in 3 (900 rolls, 25-42 %; the real table with Infused Gems, else the pool's `condition` decoded from the file);
  - `officeGemsCarryTheirEffect`: with Infused Gems, office-chest gems carry their effect component (they came out blank when the entries used the pre-26.x `functions` key);
  - `lootFilesUseCurrentKeys`: no `bubbas_*` loot table or item modifier on the server uses an old-form key (`"function"`, `"functions"` outside a sequence, a `"conditions"` list);
  - `chestTablesRollTheirLoot` (no Fabricator built): reject and buried tables roll items, reject gear comes out damaged, every office chest holds valuables and a treasure.
- **Client part `factories`** (`FactoriesClientGameTest`):
  - the look shots, one per camera: the overview, intake and lower-front shots as in play only; the city door with night vision only;
  - the secrets shots (`secrets(...)`), printing `[fabricator] design blocks ...` with the intake, terrace and causeway light;
  - `factory-countdown`: a deactivated zombie on the pad mid-count;
  - `factory-lift`: all lamps lit, lifted;
  - `factory-burst`: the moment it fires, the departure burst over the pad (preview `docs/previews/factories/fabricator-burst.png`);
  - `factory-arrival`: the zombie limp on the surface at night in its particles;
  - `factory-worldgen` and `factory-city-door-nightvision`: the natural Fabricator nearest spawn and its doorway, with the "real ancient city there: true" check printed.
- **Not yet tested (objectives):**
  - with the surface lit and a dark cave pocket 20 above the pad, a deactivated zombie fired from the pad arrives in the pocket;
  - a natural (unforced) site rests before its first drop and between counts;
  - a ward over the drop point stops drops while a deactivated mob already on the pad is still lifted and sent (needs Totems & Artifacts + Reworked Beacons in the test run, or a stand-in ward through `api/Endermen.keepOut`);
  - a player riding the pad lands on the surface;
  - the Warden ignores its jukebox after waking;
  - a woken Warden never digs;
  - the heart / Infused Gems gem in the beacon (with `-PwithBeacons`, and with Infused Gems), and taking it out ends the debuffs;
  - office loot with and without Infused Gems;
  - a pile caps at 12;
  - the Fabricator survives a save and reload (sites, states, persistent deactivated mobs);
  - `ancient_fabricator locate`;
  - a natural Fabricator near every city in a large area;
  - a natural (city-turned) Fabricator's causeway meets its city tunnel;
  - the hut lands on the side away from a leaning causeway.

## Fabricators as real structures (designed, not built)
<a id="fabricators-as-real-structures-owner-2026-11-22-designed-not-built"></a>

- **Owner:** "fabricators are supposed to be real structures, it would be neat if they were near ancient cities but its not a requirement." So: a Fabricator structure with **its own placement grid**; being near an ancient city is a nice-to-have, not a rule. Then vanilla explorer maps, `/locate` and the Totem of Seeking point straight at it, and the map trade could go back to vanilla's `exploration_map` (it now uses `bubbas_reworked_mobs:fabricator_map`, which already marks the Fabricator itself).
- **Builder's proposed shape:**
  - a `Structure` type `ancient_fabricator` with a random-spread structure set (spacing about the ancient cities' 24, its own salt) and biomes `#minecraft:has_structure/ancient_city` (deep dark), so they still tend to sit near cities;
  - its piece builds today's `FactoryDesign`; when a city happens to be within reach (`findNearestMapStructure` on ancient cities at generation), the tunnel heads to it, else the Fabricator stands alone;
  - `FabricatorSites` (cell-from-city computation, saved states, the locate command, the tick) moves to the structure starts, as camps did (`specs/vanilla-subsystems.md`: structure references round players, saved state per start).
- **Owner call:** worlds already generated keep their old Fabricators only if the old feature stays registered for them.
- **For the map X:** vanilla explorer maps and `/locate` mark a random-spread structure's **placement chunk** plus one fixed `locate_offset`, so the Fabricator piece must start at (or a fixed offset from) its own placement chunk for the X to land on it.

## Parked

- A "totem of spawning" or lure for beacons (Totems & Artifacts, held or in a beacon like wards; keeps mod independence): maybe later.

## Open

- **Why challenge a Fabricator?** (owner raised it, options skipped for now; `BACKLOG.md`): it feeds its region's surface spawns of its mob type (recommended); capture it as a farm; a prize only found there (the intake chamber); or office paperwork leading to the next Fabricator.
- Owner to playtest the rate (about one mob per 77 s), the doorway spot (walk a natural one) and the Warden.
- Owner to review the builder's choices (look, mechanics, secrets), in particular whether the causeway should be broken in the middle and whether the reject-pit tell is too hidden.
- **Test world station:** not built. Idea: a zombie Fabricator built with `ancient_fabricator build` under the hub, reached by a ladder shaft with a sign; the tour photographs the overview and the face wall.
- **Flaky tests** (`BACKLOG.md`): `breakingTheHiddenSpawnerStopsItForGood`, `fabricatorSendsItUpAndItWakesReworked` and `wardenWakesWhenItHearsAPlayerAtTheMachine` failed now and then before the per-site test isolation; watch whether it's fixed.
- **Office jukebox loop:** when we move to the release with vanilla's infinite jukebox.
