<!-- dte:A3 -->
<!-- covers: testworlds/*, tools/build-testworld.py, tools/nbt.py -->
# Test world (spec)

A superflat world with a station for every playable feature, so the owner can try everything in one place (A3 ownerSteersByPlay).

## Pristine vs playable

- **Pristine:** `testworlds/bubba-test-world/` in the repo. Built only by `tools/build-testworld.py`, never opened in a game, committed to git. Holds a `BUILD_INFO.txt` with the date and the mod versions it was built with.
- **Playable:** `tools/install-to-modrinth.ps1` copies the pristine world into the Modrinth profile's `saves/` as "Bubba Test World". The owner plays that copy; nothing in it flows back to the repo.
  - The copy is made when it's missing. If the pristine world is newer than the copy, the script says so and leaves the copy alone; `-ResetWorld` writes over it with a fresh copy (owner: no old copies are kept; a reset also deletes any "Bubba Test World (old ...)" backups from earlier versions of the script).
  - The install also copies the owner's own settings, see `tools/sync-minecraft-settings.ps1`.

## How it's built

- A real headless Fabric server (Fabric's server launcher, Fabric API and the repo's built mods, the same downloads `tools/packtest.py` uses) creates a fresh superflat world and runs a builder datapack's function that places every station with commands, then saves and stops. Anything that fails is reported and fails the build.
- `level.dat` is then adjusted so it opens in singleplayer as intended: name "Bubba Test World", creative, cheats allowed.
- Rebuild whenever a feature changes what should be tested; the station list below is updated in the same change.

## World settings

Superflat (bedrock, dirt, grass), plains. Creative, cheats on. Time frozen at night (woken undead burn in daylight), clear weather locked, invisible light blocks so stations are lit. No random mob spawning (stations spawn what they need). Player spawn at the hub, facing the station signs.

## Stations

Each station has a sign naming it and saying what to try.

1. **Graveyard (Reworked Mobs):** rows of skeleton and zombie sleepers in every pose and facing, some with full gear, some with only a weapon, some empty-handed; a wool-walled pen to test that wool blocks noise; a dense crypt-like cluster to see chain waking.
2. **Loot on display:** every placement (ground, buried, wall, stuck) with a sword, bow, trident and a non-weapon item; a sleeper next to displays to see a woken undead grab one.
3. **Combat arena:** fenced, with chests holding a Smite V sword, a bow and arrows, armor sets, and a few sleepers to wake.
4. **Crafting Tweaks:** chests with every recipe's ingredients, a crafting table, blast furnace and smoker; Synthetic Diamond Ore next to natural deepslate diamond ore, labelled; Bamboo Paper next to plain paper in an item frame or chest.
5. **Glider tower:** a tall tower with a Glider, an elytra and firework rockets in a chest at the top, to compare flight.
6. **Infused Gems:** the whole gem workflow left to right (brew, enchant, socket, remove): base gems and effect items, potions in all three forms, brewing extras, a brewing stand, an enchanting table with 15 bookshelves, lapis and XP, a smithing table, a grindstone, armor of every material and weapons; a pen of mobs that never move (NoAI) to test weapon gems on.

## As built

- **Run:** `python tools/build-testworld.py` (builds all three mods with Gradle first; `--no-build` uses the current jars, `--verify-only` re-checks the pristine world, `--keep` keeps `build/testworld-server/` and `build/testworld-verify/` with a `console.log` of every reply, `--verbose` prints every command). About 30 s on the owner's PC with Gradle up to date (server start 7 s, build 13 s, verify ~8 s); the first run also downloads the Fabric launcher, Fabric API and the game libraries.
- **Server:** reuses `tools/packtest.py`'s download helpers and cache (`build/packtest/cache`, SHA-512 checked). `server.properties`: `level-type=minecraft:flat`, `generator-settings={"layers":[bedrock 1, dirt 2, grass_block 1],"biome":"minecraft:plains","lakes":false,"features":false}` (the 26.3 `FlatLevelGeneratorSettings` JSON; ground top at y=-61, stations stand on y=-60), `generate-structures=false`, `gamemode=creative`, `difficulty=normal` (peaceful would delete woken undead).
- **Builder datapack:** `testworlds/builder/` (namespace `bubba_testworld`, format 121; `tools/pack.py check` covers `testworlds/` packs). `build.mcfunction` calls one function per station: `setup`, `hub`, `crafting`, `tower`, `arena`, `loot`, `graveyard`, `gems`. The pack is installed so the server parses every function at load (a bad line is a load ERROR, which fails the build), but the tool sends each command through the console itself, following `function` lines (no macros), so every reply is checked: unknown commands, bad arguments, `Test failed`, `Could not set the block`, "No blocks were filled" and similar fail the build, as does any ERROR in the log. `forceload add x1 z1 x2 z2` lines make the tool wait until those chunks are loaded (and 3 s more for entities). At the end it disables the pack, removes the forceload, and saves.
- **Area:** forceloaded x -48..47, z -48..15 (24 chunks, about 4 MB on disk). Only those chunks and their generation border are saved.
- **Copy and level.dat:** `level.dat`, `data/` and `dimensions/` are copied (not `session.lock`, `level.dat_old`, `players/`, `datapacks/`). `level.dat` (`Data.LevelName` "Bubba Test World", `GameType` 1, `allowCommands` 1, `file/builder` dropped from `DataPacks`) is edited with `tools/nbt.py` (also `python tools/nbt.py <file> [path]` to look inside any NBT file). 26.3 keeps game rules, weather and clocks in `data/minecraft/*.dat`, so the commands set them, not level.dat. `BUILD_INFO.txt` holds date, Minecraft version and mod jar names.
- **Verify:** a second server on a copy of the pristine world (no builder pack) runs `verify.mcfunction` (game rules via `execute store`, time 18000, clear weather, no woken undead, gear on sleepers, items in displays and chests, synthetic vs natural ore states, item frames, glider in the tower chest) plus an automatic `execute if block <pos> <state>` for every `setblock` and display command in the build.
- **World rules (26.3 names):** `spawn_mobs`, `spawn_phantoms`, `spawn_patrols`, `spawn_wandering_traders`, `advance_time`, `advance_weather` all false; `respawn_radius 0`; `keep_inventory true` (so dying in the arena loses nothing); `time set midnight`; `weather clear`; `setworldspawn 0 -60 7 180 0` (facing north at the sign row). Invisible `minecraft:light[level=15]` every 6 blocks, 4 above the floor, over each station.
- **Techniques that matter:** sleepers are `setblock ... bubbas_reworked_mobs:<variant>_sleeper[facing=..,pose=..]{Items:[{Slot:<n>b,id:..,count:1}]}` (slots 0 main hand, 1 off hand, 2 head, 3 chest, 4 legs, 5 feet); command-placed blocks don't wake them (checked: no woken undead after the build). Displays use the mod's `display` command. Chests are `setblock ... chest[facing=..]{CustomName:"..",Items:[...]}` with item components in SNBT. Signs are waxed (`is_waxed:1b`) with SNBT text; lines are kept to 15 characters and bold titles to 13 so they fit. Item frames need `Pos` and `block_pos` in the summon NBT (26.3 checks the attached block is within 16 blocks of the entity while loading; without `Pos` it is at 0,0,0 and fails).

### Layout (x, z; floor y -60)

- **Hub** (0, 7): polished andesite plaza, seven signs at z 3 to 5, dirt paths to every station.
- **Graveyard** x -44..-14, z -44..-14, mossy cobblestone wall with a gap at x -20..-18: skeleton row z -18 and zombie row z -24, each sitting then lying facing N, E, S, W (8 per row) with full gear / weapon only / nothing / weapon + one armor piece, on coarse dirt; a chest of sleeper items (both poses), a Silk Touch pick, wool, TNT; a white wool pen (x -42..-36, z -36..-30, 3 high) with three sleepers; a mossy stone brick crypt (x -32..-18, z -44..-34) packed with 24 sleepers.
- **Loot on display** x -6..6, z -31..-14: rows wall (z -30, on a stone brick wall), stuck (-27), ground (-24), buried (-21), each with iron sword, bow, trident, golden apple; a shovel chest; an unarmed zombie sleeper at (6, -24) two blocks from an iron axe on the ground.
- **Combat arena** x 14..40, z -44..-14, oak fence, gate at (27, -14): chests at (24, -12) weapons (Smite V diamond and netherite swords and iron axe, plain sword, bows incl. Power V, crossbow, shield, arrows, spectral arrows) and (30, -12) iron/diamond/netherite armor, golden apples, steak; 10 sleepers inside (7 geared at z -40, 3 unarmed at z -30).
- **Crafting Tweaks** x -30..-14, z -4..12, oak floor: chest + crafting table, chest + blast furnace, chest + smoker (every recipe's ingredients and coal), Synthetic Diamond Ore column (-26, 2) next to natural ore (-26, 5), an ore chest (plain, Silk Touch, Fortune III picks; both ores as items), Bamboo Paper and plain paper in item frames on a wall at (-26, 8 / 10) and in a chest.
- **Infused Gems** x 30..46, z 2..14, calcite floor, path from the arena path at (29..38, -1..1): signs at z 5 (readable from the north) over a row at z 6: chests at x 31 (base gems, effect items), 32/33/34 (21 potions each: normal, splash, lingering), 35 (blaze powder, gunpowder, dragon's breath...), brewing stand 36, lapis/XP chest 40, smithing table 43, grindstone 44, armor chest 45, weapons chest 46; enchanting table (40, 10) inside a 3-high bookshelf room x 38..42, z 8..12 with a 2-high doorway at (40, 8); fenced target pen x 30..37, z 9..14 (gate at 33, 9) with NoAI zombie, skeleton, husk, pig, cow and villager. Shortcut chests at x 37 (a netherite set with 3 Speed gems per piece, a sword and bow with 2 Poison gems each) and 38 (finished gems of the working effects, a bonus-effect gem, a drawback gem, a Flawed Gem, and three Infused Gems). More sets in chests at x 45, z 9..13 (signs at x 44): a diamond Tank set, an iron Diver set, a netherite Healer set, a Run test (full netherite Jump Boost set vs helmet + boots only vs leather), and Weapons (every weapon kind with harmful gems, a sword with beneficial gems, a weak tier 1 sword); Drawback gear at (46, 9).
- **Glider tower** x 20..24, z 4..8: hollow stone brick, entrance at (20, 6), ladder inside, oak platform at y -1 (60 blocks up) with a chest of two Gliders, two elytras and rockets (flight 1 and 2).

Not done: a visual check (screenshots) of the built world. The client game test harness runs one mod at a time and the world needs both mods; a combined screenshot run is in BACKLOG.md.
