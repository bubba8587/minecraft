<!-- dte:A3 -->
# Test world: beacons and warding stations, docs to merge (2026-11-16)

Written by the test world builder under the docs policy (builders don't edit `specs/` or `player-docs/` while the docs agent works). For a docs agent to merge into `specs/testworld.md` (station list, Layout, As built) and `player-docs/Getting Started/Bubba Test World.md`.

## Spec facts (`specs/testworld.md`)

- **Mods:** `tools/build-testworld.py` `MODS` and `mods/testworldshots/build.gradle` `modsUnderTest` now include `bubbas_reworked_beacons` (the world is built and screenshotted with all four mods).
- **The `grounds*.mcfunction` files are generated** by `tools/testworld-grounds.py` (owner: after a regenerate from an old scratch copy undid the "Gem Power" rename). Edit the script, not the generated files, then run `python tools/testworld-grounds.py` and `python tools/build-testworld.py`. Effect names on signs come from Infused Gems' `en_us.json` (falling back to vanilla's lang in `build/mcsrc/`), and the level label ("Gem Power") from the gem item's name pattern, so a rename in a mod reaches the signs on the next generate. Sign-only wording ("perfect set", the Gem Power V twin lines) is in named constants at the top of the script, and every sign line is checked against the 15/13 character limits. Replace the spec's sentence "they were generated once by a throwaway script, so edit them directly".
- **New builder functions** (called from `grounds.mcfunction`): `grounds_beacons` (beacon yard, poison trap, bell lane, light rings) and `grounds_paired` (paired beacons room and spawn test boxes). The wardrobe gets a Totem of Warding sign.
- **Station list:** add "9. **Beacons and Warding (Reworked Beacons, Reworked Mobs):** a beacon yard of sky beacons on 1 to 4-layer pyramids with gem chests; a Poison beacon trap zone; a paired-beacon room with Totems of Warding, sleepers, a bell and natural-spawn test boxes inside and outside its ward; a bell beside sleepers; a torch-lit sleeper ring beside a dark one."

### Layout (x, z; ground stations stand on y -60)

- **Beacon yard** x 20..37, z 64..86 (path x 27..28 from avenue C, z 52..68; info sign at (26, 64)). Sky beacons, iron pyramids, beacon on top:
  - 1 layer: centre (23, 70), beacon (23, -59, 70); sign (22, 67), chest (24, 67).
  - 2 layers: centre (31, 70), beacon (31, -58, 70); sign (30, 66), chest (32, 66).
  - 3 layers: centre (23, 80), beacon (23, -57, 80); sign (22, 75), chest (24, 75).
  - 4 layers: centre (33, 81), beacon (33, -56, 81); sign (32, 75), chest (34, 75).
  - Each sign: layers, "up to Power I/III/V/VI", range 20/30/40/50. Each chest: finished gems of Speed, Regeneration, Jump Boost, Night Vision, Strength, Resistance, Fire Resistance, Slow Falling, Haste, perfect (Gem Power VI, top row) and Gem Power V (second row), plus amethyst shards, quartz, diamonds and nether stars (the standalone payments; with Infused Gems installed they are switched off, so they show the "no" case). The yard beacons start empty.
- **Poison trap zone:** a 1-layer sky beacon at (-40, -59, 88) loaded with a perfect Poison gem (block entity NBT `"bubbas_reworked_beacons:item"`), capped to Gem Power I. Its area is x -60..-20, z 68..108, full height; nothing else is in it. A NoAI pig, cow and zombie (tag `tw_trap`) in an oak fence pen x -44..-36, z 79..83 show it (the zombie, undead, ignores Poison). Warning signs at (-36, 67) (end of a path x -36..-35 from avenue C) and (-19, 84) (facing east).
- **Bell lane** x 31..46, z 54 (row 3 east): a floor bell at (31, -60, 54), zombie sleepers lying at x 34, 40, 46 (3, 9 and 15 blocks: all inside the ~19-block wake radius). Signs at z 52: Bell info (30), Reset sleepers (32: kills woken zombies within 24 of (40, 54), places all three back). A level-8 light block keeps it from being black without calming anything. Not at the graveyard edge (the owner's suggestion) because a bell there wakes the graveyard's own exhibits (rows and crypt within 19 blocks, through its non-solid walls); the nearest other sleepers are 26+ blocks away.
- **Light-calm rings** x 41..46: four sitting skeleton sleepers around (43, 80), each with a torch just outside it (block light 13 at the sleeper), and four around (43, 90) with no light nearby (block light under 12). Signs facing west at x 39: Lit Ring (z 80), Dark Ring (z 90), Reset rings (z 84; puts the dark ring back, kills woken skeletons within 16 of (43, 85); the lit ring's sleepers are re-placed by hand).
- **Paired beacons room**, high above the boss arena so its ward box misses the ground: deepslate brick room x -9..9, y 23..34, z 70..88. Floor beacon (0, 24, 79) on a 4-layer iron pyramid (y 20..23, top layer in the floor); upside-down beacon `minecraft:beacon[upside_down=true]` at (0, 30, 79) under a 4-layer gold pyramid (y 31..34, part of the roof), clear air between. Both empty. Ward box when a totem is in: 150 blocks out sideways, y 10..44.
  - Inside: a "Totems of Warding" chest (3 totems) at (-5, 24, 73); sleepers at (-6, 24, 85), (-3, 24, 86), (6, 24, 85); a bell at (4, 24, 82); light level 11 at most (below the light-calm 12). Wall signs on the north wall (z 71, y 25): Way down (tp to (15.5, -60, 60.5)), Paired Room info, Spawns on, Spawns off, Count spawns, Clear boxes. A sign at (-1, 24, 76): with 4 layers and a totem the bell wakes nobody.
  - **Spawn test boxes:** two sealed, pitch-dark deepslate tile boxes, x -5..5, z 74..84: the warded one y 11..15 (interior 12..14, inside the ward box) and the control y 47..51 (interior 48..50, above the ward box). Both are 70+ blocks above the ground, so a player on the ground is past the 24-block no-spawn radius and within 128. The world has `spawn_mobs false`; the Spawns on/off signs flip that game rule (on: monsters spawn in every dark place in the world, so turn it off after). Count spawns sends a chat line with the number of mobs in each box (`tellraw`, scores `#in` / `#out` in `tw_hp`).
  - Ground signs at z 62 (south of the mining wall), facing north: Paired Room (tp up to (0.5, 24, 73.5)), Spawns on, Spawns off, Count spawns, Clear boxes.
- **Wardrobe:** Totem of Warding sign at (3, -60, 27) on the east wall's bottom row (gives one).
- **Area:** unchanged (x -48..47, z -48..95); the paired room is high in the same chunks.

### Verify and screenshots

- Verify adds checks for the pyramids, gem chest, the trap beacon's Poison gem, the trap pig, both paired beacons (upright and `upside_down=true`) and the clear line between them, the totem chest, the spawn boxes, the wardrobe totem sign and Spawns on sign, and that placing the lit ring's torches woke nobody (955 checks).
- Tour shots: `beacons_yard`, `beacons_yard_4layer_signs`, `beacons_gem_chest` (+ tooltip), `beacons_trap_zone`, `beacons_trap_poisoned`, `warding_bell_lane`, `warding_light_rings`, `warding_ground_signs`, `paired_room_beam`, `paired_room_signs`, `paired_room_sleepers`. The tour also waits up to 300 ticks for the trap beacon to hurt the pig (Poison gems hurt steadily rather than giving the Poison effect) and fails if it doesn't.

## Player page (`player-docs/Getting Started/Bubba Test World.md`)

A new section, "Beacons and warding", in the page's tutorial style:

| Station | What to try |
|---|---|
| Beacon yard | Four sky beacons on 1, 2, 3 and 4-layer pyramids. Put a gem from the chest beside each into the beacon: the pyramid caps its Gem Power (I, III, V, VI) and sets the range (20 to 50 blocks). Each chest has perfect and Gem Power V gems. |
| Poison trap | A beacon holding a Poison gem poisons everything around it, the player included; the pig and cow show it, the zombie is immune. Signs mark the edge. |
| Paired beacons | A sign south of the mining wall goes up to a closed room with a floor beacon and an upside-down beacon above it. Put a Totem of Warding from the chest into the floor beacon: the area 150 blocks around is warded at that height, and with the 4-layer pyramids the sleepers in the room stay asleep even when the bell rings. |
| Spawn test | Two dark boxes, one inside the ward and one above it. Turn natural spawns on with the sign, wait on the ground for a while, count the mobs in each box with the Count sign, then turn spawns off again. |
| Bell | Ring the bell: sleepers 3, 9 and 15 blocks away wake. A sign puts them back. |
| Light rings | Sleepers lit by torches (light 12 or more) are harder to wake than the same sleepers in the dark. |
| Totem of Warding | A sign in the wardrobe gives one; held, it scares monsters away every 10 seconds. |

## Harness change (for the spec's Screenshots section)

- `mods/testworldshots/build.gradle`: the tour's game now loads a private copy of the mod jars (`snapshotModJars`, a Sync into `mods/testworldshots/build/modsUnderTest/`) instead of the jars in each mod's `build/libs`. Another agent rebuilding a mod during a tour rewrote its jar in place and the running game crashed ("ZipFile invalid LOC header") three runs in a row; with the copy the tour passes (140 shots).
