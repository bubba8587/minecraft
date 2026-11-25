<!-- dte:A3 -->
<!-- covers: testworlds/*, tools/build-testworld.py, tools/testworld-grounds.py, tools/nbt.py, mods/testworldshots/* -->
# Test world (spec)

A superflat world with a station for every playable feature, so the owner can try everything in one place (A3 ownerSteersByPlay). A second world, the natural world, shows our things where normal generation puts them.

## Pristine vs playable

- **Pristine:** `testworlds/bubba-test-world/` in the repo. Built only by `tools/build-testworld.py`, never opened in a game, committed to git. Its `BUILD_INFO.txt` holds the build date, the Minecraft version and the mod jar names.
- **Playable:** `tools/install-to-modrinth.ps1` copies the pristine world into the Modrinth profile's `saves/` as "Bubba Test World". The owner plays that copy; nothing in it flows back to the repo.
  - The copy is made when it's missing.
  - If the pristine world is newer than the copy, the script says so and leaves the copy alone.
  - `-ResetWorld` writes over the copy with a fresh one. No old copies are kept (owner); a reset also deletes any "Bubba Test World (old ...)" backups left by earlier versions of the script.
  - The install also copies the owner's own game settings (`tools/sync-minecraft-settings.ps1`).
- Rebuild the world whenever a feature changes what should be tested, and update the station list below in the same change.

## World settings

- Superflat (bedrock, dirt, grass), plains.
- Creative, cheats on.
- Time frozen at midnight (woken undead burn in daylight); clear weather, locked.
- Invisible light blocks so stations are lit.
- No random mob spawning; stations spawn what they need.
- Player spawn at the hub, facing the station signs.

## How it's built

**Run:** `python tools/build-testworld.py`.
- It builds all seven mods with Gradle first (`MODS`: Reworked Mobs, Extra Recipes, Infused Gems, Reworked Beacons, Totems & Artifacts, Reworked Mining, World Tweaks), and loads Stormie's Spiders (required by Reworked Mobs) from the Gradle cache.
- Options: `--no-build` uses the current jars; `--verify-only` re-checks the pristine world; `--keep` keeps `build/testworld-server/` and `build/testworld-verify/` with a `console.log` of every reply; `--verbose` prints every command.
- About 30 s on the owner's PC with Gradle up to date (server start 7 s, build 13 s, verify ~8 s). The first run also downloads the Fabric launcher, Fabric API and the game libraries.

**Steps:**
1. A real headless Fabric server (Fabric's server launcher, Fabric API and the repo's built mods) creates a fresh superflat world.
2. The tool runs the builder datapack's functions, which place every station with commands. Anything that fails is reported and fails the build.
3. The world is saved, copied, and `level.dat` is adjusted so it opens in singleplayer as intended.
4. A second server checks the result (Verify, below).

**Server:**
- Reuses `tools/packtest.py`'s download helpers and cache (`build/packtest/cache`, SHA-512 checked).
- `server.properties`: `level-type=minecraft:flat`, `generator-settings={"layers":[bedrock 1, dirt 2, grass_block 1],"biome":"minecraft:plains","lakes":false,"features":false}` (the 26.3 `FlatLevelGeneratorSettings` JSON; the ground's top is y=-61, so stations stand on y=-60), `generate-structures=false`, `gamemode=creative`, `difficulty=normal` (peaceful would delete woken undead).

**Builder datapack:** `testworlds/builder/` (namespace `bubba_testworld`, format 121; `tools/pack.py check` covers `testworlds/` packs).
- `build.mcfunction` calls one function per station: `setup`, `hub`, `crafting`, `tower`, `arena`, `loot`, `graveyard`, `gems`, `grounds`, `cairn` (the preview), `creepers`, `cairns`, `showcase`.
- `grounds` calls one `grounds_*` function per testing-grounds area, including `grounds_beacons` (beacon yard, poison trap, bell lane, light rings) and `grounds_paired` (paired beacons room and spawn test boxes).
- **The `grounds*.mcfunction` files are generated** by `tools/testworld-grounds.py` (owner: a regenerate from an old scratch copy once undid a rename). Edit the script, not the generated files, then run `python tools/testworld-grounds.py` and `python tools/build-testworld.py`.
  - Effect names on signs come from Infused Gems' `en_us.json` (falling back to vanilla's lang in `build/mcsrc/`), and the level label ("Base Power") from the gem tooltip's `tooltip.base_power` row, so a rename in a mod reaches the signs on the next generate.
  - Sign-only wording ("perfect set", the Base Power V twin lines) is in named constants at the top of the script.
  - Every sign line is checked against the 15/13 character limits.
- The pack is installed so the server parses every function at load: a bad line is a load ERROR, which fails the build.
- The tool then sends each command through the console itself, following `function` lines (so no macros), and checks every reply. Unknown commands, bad arguments, `Test failed`, `Could not set the block`, "No blocks were filled" and similar fail the build, as does any ERROR in the log.
- A `forceload add x1 z1 x2 z2` line makes the tool wait until those chunks are loaded (and 3 s more for entities).
- At the end it disables the pack, removes the forceloads, and saves.

**Area:** forceloaded x -48..47, z -48..95 (54 chunks, about 4.6 MB on disk). Only those chunks and their generation border are saved. The paired beacons room sits high in the same chunks. The creeper grove, the cairns yard and the Showcase have their own forceloads (below).

**Copy and level.dat:**
- `level.dat`, `data/` and `dimensions/` are copied (not `session.lock`, `level.dat_old`, `players/`, `datapacks/`).
- `level.dat` is edited with `tools/nbt.py`: `Data.LevelName` "Bubba Test World", `GameType` 1, `allowCommands` 1, `file/builder` dropped from `DataPacks`. (`python tools/nbt.py <file> [path]` also looks inside any NBT file.)
- 26.3 keeps game rules, weather and clocks in `data/minecraft/*.dat`, so the build commands set them, not level.dat.

**Verify:** a second server on a copy of the pristine world (no builder pack) runs `verify.mcfunction`, plus an automatic `execute if block <pos> <state>` for every `setblock` and display command in the build (the run prints the total, about 950). `verify.mcfunction` checks:
- game rules (via `execute store`), time 18000, clear weather;
- no woken undead; gear on sleepers; items in displays and chests;
- synthetic vs natural ore states; item frames; the glider in the tower chest;
- the beacon yard pyramids and gem chest; the trap beacon's Poison gem and pig; both paired beacons (upright and `upside_down=true`) and the clear line between them; the totem chest; the spawn boxes; the wardrobe totem sign and the Spawns on sign;
- that placing the lit ring's torches woke nobody;
- that a `cairn here` site is still known after a reload;
- every boss's summon command (health and held weapon).

**World rules (26.3 names):**
- `spawn_mobs`, `spawn_phantoms`, `spawn_patrols`, `spawn_wandering_traders`, `advance_time`, `advance_weather`: all false.
- `respawn_radius 0`; `keep_inventory true` (dying in the arena loses nothing); `command_block_output false` (the testing grounds' command block clocks would otherwise post to chat every tick).
- `time set midnight`; `weather clear`; `setworldspawn 0 -60 7 180 0` (facing north at the sign row).
- Invisible `minecraft:light[level=15]` every 6 blocks, 4 above the floor, over each station.

**Techniques that matter:**
- **Sleepers:** `setblock ... bubbas_reworked_mobs:<variant>_sleeper[facing=..,pose=..]{Items:[{Slot:<n>b,id:..,count:1}]}`. Slots: 0 main hand, 1 off hand, 2 head, 3 chest, 4 legs, 5 feet. Command-placed blocks don't wake them (checked: no woken undead after the build).
- **Displays** use the mod's `display` command.
- **Chests:** `setblock ... chest[facing=..]{CustomName:"..",Items:[...]}`, with item components in SNBT.
- **Signs** are waxed (`is_waxed:1b`) with SNBT text. Lines are kept to 15 characters and bold titles to 13 so they fit.
- **Item frames** need `Pos` and `block_pos` in the summon NBT: 26.3 checks that the attached block is within 16 blocks of the entity while loading, and without `Pos` the entity is at 0,0,0 and fails.

## Stations

Each station has a sign naming it and saying what to try.

1. **Graveyard (Reworked Mobs):** rows of skeleton and zombie sleepers in every pose and facing, some with full gear, some with only a weapon, some empty-handed; a wool-walled pen to test that wool blocks noise; a dense crypt-like cluster to see chain waking.
2. **Loot on display:** every placement (ground, buried, wall, stuck) with a sword, bow, trident and a non-weapon item; a sleeper next to displays to see a woken undead grab one.
3. **Combat arena:** fenced, with chests holding a Smite V sword, a bow and arrows, armor sets, and a few sleepers to wake.
4. **Extra Recipes:** chests with every recipe's ingredients, a crafting table, blast furnace and smoker; Synthetic Diamond Ore next to natural deepslate diamond ore, labelled; Bamboo Paper next to plain paper in item frames and a chest.
5. **Glider tower:** a tall tower with Gliders, elytras and firework rockets in a chest at the top, to compare flight.
6. **Infused Gems:** the whole gem workflow left to right (brew, enchant, socket, remove): base gems and effect items, potions in all three forms, brewing extras, a brewing stand, an enchanting table with 15 bookshelves, lapis and XP, a smithing table, a grindstone, armor of every material and weapons; a pen of mobs that never move (NoAI) to test weapon gems on.
7. **Gem Testing Grounds (Infused Gems; owner: "set up test world stuff for everything ... be creative"):** a wardrobe that puts on any gem's perfect set in one click; an arrow gallery and a live archer range (Ender, Breeze); drop pads (Wind Burst, Oozing, Slow Falling, Healing) and jump steps; a cobweb field (Weaving); a deep pool and a sky column (Water Breathing, Dolphin's Grace, Conduit Power, Levitation); a mob pen with spawn signs and target dummies with a health readout (weapon gems, undead inversion); villagers with fixed trades (Hero of the Village); a sculk hall with sleepers (Silence); a lava moat (Fire Resistance); a sealed dark room (Night Vision, Darkness, Blindness); a food bench (Hunger, Saturation); a mining wall (Haste, Mining Fatigue). Each station has kit signs for the gems it tests and, where damage matters, Survival/Creative signs.
8. **Boss arena (Infused Gems; owner: boss fights with intentional gem combos and counter kits):** a walled arena with one summon sign per boss (a tough vanilla mob in gemmed gear: Webmaster, Puppeteer, Blinker, Juggernaut, Gale, Plague, Phantom, Stalker), Clear arena and game mode signs, and a Counter kits wall with a synergy set for the player against each boss. Gem numbers may be rebalanced; the combos are the point.
9. **Beacons and Warding (Reworked Beacons, Totems & Artifacts, Reworked Mobs):** a beacon yard of sky beacons on 1- to 4-layer pyramids with gem chests; a Poison beacon trap zone; a paired-beacon room with Totems of Warding, sleepers, a bell and natural-spawn test boxes inside and outside its ward; a bell beside sleepers; a torch-lit sleeper ring beside a dark one; a Totem of Warding sign in the wardrobe.
10. **Creeper grove (Reworked Mobs, `specs/creepers.md`):** a lit clearing with small trees, a stone wall and boulders; one creeper among the trees to watch peeking and peekaboo, one on open ground to see it keep its distance; Throwables (snowballs, bow) for near-misses, Survival/Creative, Reset grove and Tidy up signs.
11. **Cairns and endermen (Reworked Mobs, `specs/endermen.md`):** three finished cairns from fixed seeds, a yard with an unfinished cairn and its builders, a repair corner (dirt pillar, cobblestone heap, pit, farmland) for caretakers, and a vigil ring at one finished cairn. 80+ blocks from everything else, so wandering caretakers can't reach other stations.
12. **Cairn preview** (x 8..16, z -11..-3, north-east of the hub): an inert hand-built cairn on a small hill (`cairn.mcfunction`, tour part `preview`).
13. **Showcase** (far south): newer features built with the mods' own commands: every totem, a Herding pen, World Tweaks lava and a sulfur geyser, crypt yards of every wealth and state, a camp of every kind, a spider nest and a giant spider.

**Not in this world:** drowned, Ancient Fabricators, trader posts and Nether outposts (all in the natural world, below); Reworked Mining (no station anywhere yet).

## Layout (x, z; floor y -60)

- **Hub** (0, 7): polished andesite plaza with dirt paths to every station.
  - Signs at z 3 (x -7..7, every other block): Gem Grounds (-7; see below), Extra Recipes (-5), Graveyard (-3), Test World info (-1), Loot Display (1), Combat Arena (3), Glider Tower (5), Infused Gems (7).
  - Signs at z 5: Cairns click sign (-7), Showcase (5), Fresh copy? (7, how to reset).
  - Gem Grounds and Wardrobe signs at (-2/2, 11), facing south.
  - **Known issue:** `hub.mcfunction` places the Creepers sign at (-7, -60, 3), but `grounds.mcfunction` runs later and places the Gem Grounds sign on the same spot, so the hub has no Creepers sign. Needs a free spot for one of them.
- **Graveyard** x -44..-14, z -44..-14, mossy cobblestone wall with a gap at x -20..-18.
  - Skeleton row z -18 and zombie row z -24, on coarse dirt: each sitting then lying, facing N, E, S, W (8 per row), with full gear / weapon only / nothing / weapon plus one armor piece.
  - A chest of sleeper items (both poses), a Silk Touch pick, wool, TNT.
  - A white wool pen (x -42..-36, z -36..-30, 3 high) with three sleepers.
  - A mossy stone brick crypt (x -32..-18, z -44..-34) packed with 24 sleepers.
- **Loot on display** x -6..6, z -31..-14.
  - Rows: wall (z -30, on a stone brick wall), stuck (-27), ground (-24), buried (-21), each with an iron sword, bow, trident and golden apple.
  - A shovel chest; an unarmed zombie sleeper at (6, -24) two blocks from an iron axe on the ground.
- **Combat arena** x 14..40, z -44..-14, oak fence, gate at (27, -14).
  - Chest at (24, -12): weapons (Smite V diamond and netherite swords and iron axe, plain sword, bows including Power V, crossbow, shield, arrows, spectral arrows).
  - Chest at (30, -12): iron, diamond and netherite armor, golden apples, steak.
  - 10 sleepers inside: 7 geared at z -40, 3 unarmed at z -30.
- **Extra Recipes** x -30..-14, z -4..12, oak floor.
  - Chest + crafting table, chest + blast furnace, chest + smoker (every recipe's ingredients, and coal).
  - Synthetic Diamond Ore column (-26, 2) next to natural ore (-26, 5); an ore chest (plain, Silk Touch and Fortune III picks; both ores as items).
  - Bamboo Paper and plain paper in item frames on a wall at (-26, 8 / 10), and in a chest.
- **Infused Gems** x 30..46, z 2..14, calcite floor, path from the arena path at (29..38, -1..1).
  - Signs at z 5 (readable from the north) over a row at z 6: chests at x 31 (base gems, effect items), 32/33/34 (21 potions each: normal, splash, lingering), 35 (blaze powder, gunpowder, dragon's breath...); brewing stand 36; lapis/XP chest 40; smithing table 43; grindstone 44; armor chest 45; weapons chest 46.
  - Enchanting table (40, 10) inside a 3-high bookshelf room x 38..42, z 8..12, with a 2-high doorway at (40, 8).
  - Fenced target pen x 30..37, z 9..14 (gate at 33, 9) with a NoAI zombie, skeleton, husk, pig, cow and villager.
  - Shortcut chests: x 37 (a netherite set with 3 Speed gems per piece, a sword and a bow with 2 Poison gems each) and x 38 (finished gems of the working effects, a bonus-effect gem, a drawback gem, a drawback-only gem, and three Infused Gems).
  - More sets in chests at x 45, z 9..13 (signs at x 44): a diamond Tank set, an iron Diver set, a netherite Healer set, a Run test (full netherite Jump Boost set vs helmet and boots only vs leather), and Weapons (every weapon kind with harmful gems, a sword with beneficial gems, a weak tier 1 sword). Drawback gear at (46, 9).
- **Glider tower** x 20..24, z 4..8: hollow stone brick, entrance at (20, 6), ladder inside, oak platform at y -1 (60 blocks up) with a chest of two Gliders, two elytras and rockets (flight 1 and 2).
- **Creeper grove** x 52..82, z -14..16 (own forceload `48 -16 82 16`).
  - Path along z -1..0 from the gems path (x 39..55); gravel viewing strip x 52..55, z -3..3.
  - Trees (3-log trunks, 3x3 crown): oak (63, -6), oak (67, 4), birch (62, 8), spruce (71, -2). Cobblestone wall x 66..69, z -11, 3 high. Boulders (59..60, 1..2) and andesite (72..73, 9). Open ground east of x 76.
  - Creepers `tw_creeper`, persistent, at (64.5, -7.5) and (80.5, 11.5).
  - Info signs face south at z -3 (x 52..55). Click signs face north at z 3: x 52 Reset grove, 53 Throwables, 54 Survival, 55 Creative, 56 Tidy up.
  - **Reset grove** clones a backup copy built at y 198..206 back over y -63..-55 (craters included), and kills and re-summons both creepers. (The builder pack is disabled in the finished world, so signs can't call functions.)
  - Watcher spawning is off here like all spawning (`spawn_mobs false`).
- **Cairns and endermen** x -232..-182, z -10..56 (own forceload `-236 -12 -180 56`).
  - Far west on purpose: endermen judge everything within ~16 blocks of where they stand against the superflat target (air above grass), and they wander and teleport, so every other station is kept 80+ blocks away (near one, they'd strip its paths and gravel). No fence: they teleport over fences.
  - Reached by the hub click sign at (-7, -60, 5): tp to (-206.5, -60, 26.5) facing north. A Back to hub sign at (-201, -60, 22).
  - Finished cairns: `cairn here` seeds 11, 22, 33 at (-222/-207/-192, -60, 42), signs at z 32 facing north.
  - Unfinished cairn: seed 44 at (-218, -60, 6), cut down to its footing and first trunk blocks (`fill -226 -58 -2 -210 -30 14 air`). Builders from `cairn builders` run at (-215.5, -60, 16.5): one per missing block, max 12, each carrying it.
  - Repair corner: dirt pillar (-196, 0) 4 high; cobblestone heap (-192..-191, -2..-1); a 3x3 pit two deep at (-201..-199, 9..11); farmland x -194..-190, z 10..14, kept wet by water at (-192, 12). Caretakers (tag `tw_enderman`) at (-197.5, 5.5) and (-189.5, 4.5).
  - Vigil: five endermen (tag `tw_vigil`) placed around the finished seed 22 cairn take up its ring. Vigil sign at (-199, -60, 32).
  - Signs at z 22 facing south: Endermen, Builders, Caretakers (info); Reset yard (clones the backup at y 198..205 over y -63..-56 for x -232..-182, z -10..22, clears y -55..-30 there, and kills every enderman and item in the yard; the vigil ring is outside it and stays); Endermen (builders plus two caretakers); Survival; Creative; Back to hub.
  - The four `cairn here` sites are saved with the world; verify checks one is still known after a reload.
- **Showcase** x -48..47, z 96..259 (own forceload `-48 96 47 259`, also in verify; `showcase.mcfunction`), far south past the boss arena; hub sign at (5, -60, 5). Built with the mods' own commands.
  - **Flat strip z 96..105:** an "Every totem" chest at (7, -60, 100) with seeds, saplings and sugar cane for Growth; a sheep and cow pen x -46..-36 for Herding; a lava pool over magma for World Tweaks, x 14..22; a sulfur geyser (potent sulfur under two water at (34, -63, 100)).
  - **Stone plateau** x -46..46, z 108..256, ground y -52, reached by stairs at x -2..2, z 100..107. Superflat ground is only 3 blocks over bedrock, too shallow for crypts (6 deep) and camp rooms (5 deep); on the plateau every crypt is whole (its lowest block is at y -59).
  - **Row A, a yard per crypt wealth** (gates at z 112): small kept pauper at x -32, medium overgrown family at x 0, large kept noble at x 30 (to z 155). Seeds are random, so the layouts differ per build.
  - **Row B, yard states without crypts** (gates at z 172): lone nearly gone at x -36, small collapsed at x -16, medium sunken at x 8, small overgrown at x 34.
  - **Row C, a camp of every kind** (centres at z 212, seeds 20261122..26): medium skeleton at x -34, then small zombie at -14, husk at 4, stray at 22 and bogged at 39. Rosters appear when a player comes near.
  - **Row D, spiders:** a dark stone cave x 19..35, z 239..253 (door on the north side) with a nest built by `/bubbas_reworked_mobs spider nest` (ceiling drop 13); a spruce grove of six 10-high trunks at x -38..-28, z 243..248 with a giant spider and its spun webs (`spider giant`).
  - Signs at each example, on its north side.

### Gem Testing Grounds (x -47..46, z 11..63, south of the hub)

- **Click signs (the technique):** a sign whose text lines carry `click_event:{action:"run_command",command:".."}` runs those commands, line by line, when right-clicked (any item in hand).
  - 26.3 runs them only if the sign has `allow_op_features:1b`; otherwise the player sees "click actions disabled".
  - They run as the clicking player (`@s`) at permission level 2, from the sign's position, and their chat output goes nowhere.
  - Click signs are left unwaxed (a waxed sign also plays the "can't edit" thunk on every click).
  - Up to four commands per sign; more goes through a command block chain (below).
  - Checked in the source: `SignBlock.useWithoutItem`, `SignBlockEntity.executeClickCommandsIfPresent`. Vanilla has nothing faster for armor: an armor stand swaps one slot per click into the hand, and the shelf block swaps hotbar items only.
- **Kit sign:**
  - line 1 `item override entity @s armor.* with minecraft:air` (takes the worn armor off; it is deleted, not returned);
  - line 2 `gems kit <effect> [material] [power]` (Infused Gems: a full worn set with 3 gems a piece, plus a sword with 2; Base Power 6 is perfect);
  - line 3 an equip sound.
  - Gems whose test needs a tool also give it (owner: Haste needs mining tools, not a sword), in one `loot give @s loot {pools:[...]}` command with an inline loot table: Haste a netherite pickaxe, shovel and axe; Mining Fatigue and Conduit Power a netherite pickaxe; Luck a fishing rod (the equip sound moves to line 4).
  - The verify step tries the take-off command on an armor stand; the screenshot test clicks the Speed sign as the player and checks the worn chestplate, then the Take off sign.
- **Command blocks:** 26.3's `command_blocks_work` game rule (default true) enables them, in singleplayer or on a server. They sit under the floor at y -62.
  - Arrow gallery: a lever on the floor at (-35, -60, 20) powers a repeating command block at (-35, -62, 20). Its chain (west to x -45) counts `#gallery tw_clock` and puts a redstone block behind each dispenser in turn (ticks 20, 40, 60: one arrow a second), refilling each to 64 arrows.
  - Dummies: an always-on repeating block at (9, -62, 37) stores each dummy's health in `tw_hp` and shows it on the action bar of players in x 7..25, z 35..46. The Reset dummies sign places a redstone block at (9, -61, 39) over an impulse chain (x 9..16, y -62, z 39) that kills and re-summons the five dummies and puts the grass back.
- **Perfect vs Base Power V (owner: perfect sets have extras, so test what is missing without them):** every wardrobe gem, and every station gem with a perfect-only extra, has two signs: the perfect netherite set on an oak sign ("perfect set") and a Base Power V twin (`gems kit <effect> netherite 5`, 88%) on a birch sign ("Base Power V 88%"). The lava moat's twin is warped like its other signs. Stations with twins: gallery and live range (Ender, Breeze), cobweb field (Weaving), pool (Water Breathing, Conduit Power), mob pen (Invisibility), lava moat (Fire Resistance), dark room (Night Vision), food bench (Saturation).
- **Wardrobe** x -4..4, z 18..32 (dark oak floor; walls at x -4 and x 4, z 19..31, 4 high). Entrance signs at (-3/3, -60, 18).
  - West wall, signs facing east: the brewable gems Speed..Healing perfect at y -57, their Base Power V twins below at y -58; then Wind Burst..Hero of the Village perfect at y -59, twins at y -60.
  - East wall: the 13 harmful gems perfect at y -57, twins at y -58; at y -59, Speed in all seven materials (z 19..25), then Take off, Empty bag, No effects, Survival, Creative (z 27..31).
  - **Wardrobe totem:** a Totem of Warding sign at (3, -60, 27) on the east wall's bottom row (gives one).
- **Armor stands** x -4..4, z 37 (facing north, no gravity): Speed netherite, Fire Resistance diamond, Water Breathing iron, Slow Falling gold, Jump Boost leather, every piece and the sword with perfect gems; a sign in front of each (z 35). Name tags are off on purpose: vanilla name tags show through walls, into the dark room too.
- **Arrow gallery** x -44..-30, z 19..31 (smooth stone): stand on the red block (-37, 22).
  - Dispensers at (-37, -59, 30) facing north, (-43, -59, 22) east, (-31, -59, 22) west.
  - A backstop wall x -40..-34, z 19: kit signs on its north face (Resistance x -40, Breeze twin and perfect -39/-38, Ender perfect and twin -36/-35, Turtle Master -34); the lever, Survival and Creative on its south face.
- **Live range** x -46..-32, z 37..47, walls 5 high, door in the west wall at (-46, 38) (path at x -47), so the archers can't see the avenues.
  - Two skeletons and a pillager in oak boats on a ledge (z 45..46, y -57), tagged `tw_turret`. Archers only shoot survival players.
  - Signs inside on the north wall: Turrets on (re-summons), Turrets off, Ender and Breeze (perfect and Base Power V), Survival, Creative.
- **Drop pads** x -22..-10, z 19..31: stone brick pillars at z 23 with ladders at z 22, tops running to z 25, heights 4, 8, 12, 16, 24 at x -22, -19, -16, -13, -10. Stepping off does height minus 3 damage; 24 kills at full health. Signs at z 21; kit signs (Wind Burst, Oozing, Slow Falling, Jump Boost, Healing), Heal me, Survival, Creative at z 19.
- **Jump steps** x -13..-7 at z 40, 1 to 4 high.
- **Cobweb field** x -23..-17, z 38..46: webs 2 high at x -23..-21, a stone brick wall at x -20, an open lane x -19..-17. Reset webs refills only air; a Shears sign.
- **Deep pool** x 8..18, z 20..30: stone brick tank, water y -60..-54 (7 deep, flush with the rim), glass windows in the north wall, stairs up the west side (x 7, z 22..28), deepslate on the floor (x 10..12, z 28) to mine. Signs at z 19, including a Pickaxe sign (netherite pick).
- **Sky column** x 22..25, z 20..24: a 3x3 water pit 2 deep, and a white pole (x 25, z 23) 40 blocks tall with a red block every 5 and signs at 10, 20, 30, 40.
- **Mob pen** x 30..44, z 19..31: a glass box with a glass roof, oak door at (30, 25). Signs at z 18: spawn signs (pig, cow, zombie, skeleton, spider, wither skeleton, slime, husk), Clear pen, Invisibility kit, Oozing and Weaving swords, a Wind Burst mace.
- **Target dummies** x 8..24, z 36..46: NoAI zombie, skeleton, wither skeleton, spider and pig (tags `tw_dummy`, `tw_d_<mob>`) at z 43.5 in an oak fence pen. Signs at z 37: weapon signs (netherite swords with 2 perfect gems of Poison, Wither, Harming, Healing, Slowness, Weakness, Levitation, Glowing, Nausea; a Poison bow with arrows), Heal dummies, Reset dummies.
- **Villagers** x 30..44, z 36..46: NoAI, invulnerable level 5 armorer, librarian and weaponsmith at x 33, 37, 41, with their work stations behind them and fixed trades of 10 to 32 emeralds. An Emeralds chest at (31, 38); a Hero of the Village kit sign.
- **Sculk hall** x -42..-38, z 52..63: sculk sensors (west wall) and calibrated sensors (east wall) at z 54..60 with redstone lamps above; two zombie sleepers lying at (-41/-39, -60, 61). Silence kit and Reset sleepers (kills woken zombies within 24 blocks, places both back) at z 52 beside the entrance.
- **Lava moat** x -28..-20, z 53..62: lava 2 deep (x -27..-21, z 55..60) in a deepslate brick rim, exit stairs at x -24 (z 59, 60). Warped signs (not flammable) at z 53, including Get me out (teleports past the moat with 10 s of Fire Resistance). Lava in a closed pit never flows, and with nothing flammable nearby it never lights fires.
- **Dark room** x -12..-4, z 53..61: a sealed polished blackstone brick box with no light. The sign at (-9, -60, 52) teleports in; a glowing Way out sign inside at (-8, -59, 54) teleports out. Inside: a NoAI zombie and three colored blocks. Night Vision, Darkness, Blindness kits and No effects at z 52.
- **Food bench** x 0..8, z 52..57: Hungry and Starving signs (a moment of Hunger 201), Fill up (Saturation), Saturation and Hunger kits, Survival, Creative; a food chest at (4, 55).
- **Mining wall** x 10..24, z 56, 3 high: dirt, stone, deepslate, obsidian, oak logs (3 wide each). Signs at z 53: Haste and Mining Fatigue kits, Tools (netherite pick), two Rebuild signs, Survival, Creative. A tools chest at (16, -60, 53) holds netherite, diamond and iron pickaxes, shovels and axes, plain (left) and with Efficiency V (right), to compare Haste with and without Efficiency.
- **Boss arena** x -16..16, z 66..92: stone brick walls 5 high, coarse dirt floor.
  - An oak fence gate at (-2, -60, 66), reached by a lane x -3..-1 from avenue C (between the dark room and the food bench) and a path along the north wall (z 64..65).
  - Bosses appear on the gold block at (0, 80); the Stalker on the gold block at (10, 86), next to two skeleton sleepers at (8/12, -60, 89).
  - Signs on the outside of the north wall (z 65, facing north): Boss Arena info (x 0); the eight boss signs (x 1..8: line 1 `kill @e[tag=tw_boss]`, line 2 the summon; the Stalker's lines 3-4 put its sleepers back); Clear arena (x 10, also kills drops inside); Survival (11); Creative (12); Counter Kits info (x -6) and the eight counter kits (x -15..-8).
  - **Bosses** are summoned with `Tags:["tw_boss"]`, a red custom name, raised `max_health` (attribute `base`) and matching `Health`, `PersistenceRequired`, `drop_chances` 0 for every slot (no gemmed loot), and `equipment` items carrying `bubbas_infused_gems:socketed_gems` (`/gems kit` only works on players).

    | Boss | Mob | Health | Gear |
    |---|---|---|---|
    | Webmaster | zombie | 120 | netherite with 3 Weaving each, Weaving sword |
    | Puppeteer | vindicator | 100 | iron with Resistance, axe with Levitation + Slow Falling |
    | Blinker | skeleton | 80 | perfect Ender netherite, bow |
    | Juggernaut | zombie | 200 | netherite with Turtle Master + Absorption + Regeneration in every piece, Slowness axe |
    | Gale | piglin brute (immune to zombification) | 120 | gold with Speed, Wind Burst mace |
    | Plague | wither skeleton | 120 | chainmail with Poison, sword with Poison + Hunger (plus its own Wither) |
    | Phantom | zombie | 100 | perfect Invisibility netherite, Darkness sword |
    | Stalker | husk | 100 | netherite with Silence, Silence, Invisibility; iron sword |

  - **Counter kits** (click signs; most take off worn armor first): Web Walker (Weaving set, shears), Crowd Ctrl (sword with Levitation + Slow Falling, bow with Levitation + Slowness, arrows), Deflector (perfect Breeze set, 64 cobblestone to box the Blinker in), Siege (Strength set, axe with Wither + Harming), Anchor (Wind Burst set, Slow Falling sword), Attrition (Regeneration set, milk, golden apples), Lantern (Night Vision set, Glowing sword), Hunter (Silence set, sword with Glowing + Slowness).

### Beacons and Warding

- **Beacon yard** x 20..37, z 64..86 (path x 27..28 from avenue C, z 52..68; info sign at (26, 64)). Sky beacons on iron pyramids, beacon on top, all starting empty:

  | Layers | Centre | Beacon | Sign | Chest |
  |---|---|---|---|---|
  | 1 | (23, 70) | (23, -59, 70) | (22, 67) | (24, 67) |
  | 2 | (31, 70) | (31, -58, 70) | (30, 66) | (32, 66) |
  | 3 | (23, 80) | (23, -57, 80) | (22, 75) | (24, 75) |
  | 4 | (33, 81) | (33, -56, 81) | (32, 75) | (34, 75) |

  - Each sign: layers, "up to Power I/III/V/VI", range 20/30/40/50.
  - Each chest: finished gems of Speed, Regeneration, Jump Boost, Night Vision, Strength, Resistance, Fire Resistance, Slow Falling, Haste, perfect (Base Power VI, top row) and Base Power V (second row); plus amethyst shards, quartz, diamonds and nether stars, the standalone payments. With Infused Gems installed those payments are switched off, so they show the "no" case.
- **Poison trap zone:** a 1-layer sky beacon at (-40, -59, 88) loaded with a perfect Poison gem (block entity NBT `"bubbas_reworked_beacons:item"`), capped to Base Power I.
  - Its area is x -60..-20, z 68..108, full height; nothing else is in it.
  - A NoAI pig, cow and zombie (tag `tw_trap`) in an oak fence pen x -44..-36, z 79..83 show it (the zombie, being undead, ignores Poison).
  - Warning signs at (-36, 67) (end of a path x -36..-35 from avenue C) and (-19, 84) (facing east).
- **Bell lane** x 31..46, z 54 (row 3 east): a floor bell at (31, -60, 54), zombie sleepers lying at x 34, 40, 46 (3, 9 and 15 blocks: all inside the ~19-block wake radius).
  - Signs at z 52: Bell info (30), Reset sleepers (32: kills woken zombies within 24 of (40, 54), places all three back).
  - A level-8 light block keeps it from being pitch black without calming anything.
  - Not at the graveyard edge (the owner's suggestion), because a bell there wakes the graveyard's own exhibits (rows and crypt within 19 blocks, through its non-solid walls). The nearest other sleepers are 26+ blocks away.
- **Light-calm rings** x 41..46: four sitting skeleton sleepers around (43, 80), each with a torch just outside it (block light 13 at the sleeper), and four around (43, 90) with no light nearby (block light under 12). Signs facing west at x 39: Lit Ring (z 80), Dark Ring (z 90), Reset rings (z 84: puts the dark ring back and kills woken skeletons within 16 of (43, 85); the lit ring's sleepers are re-placed by hand).
- **Paired beacons room**, high above the boss arena so its ward box misses the ground: a deepslate brick room x -9..9, y 23..34, z 70..88.
  - Floor beacon (0, 24, 79) on a 4-layer iron pyramid (y 20..23, top layer in the floor). Upside-down beacon `minecraft:beacon[upside_down=true]` at (0, 30, 79) under a 4-layer gold pyramid (y 31..34, part of the roof). Clear air between; both empty.
  - Ward box when a totem is in: 150 blocks out sideways, y 10..44.
  - Inside: a "Totems of Warding" chest (3 totems) at (-5, 24, 73); sleepers at (-6, 24, 85), (-3, 24, 86), (6, 24, 85); a bell at (4, 24, 82); light level 11 at most (below the light-calm 12). Wall signs on the north wall (z 71, y 25): Way down (tp to (15.5, -60, 60.5)), Paired Room info, Spawns on, Spawns off, Count spawns, Clear boxes. A sign at (-1, 24, 76): with 4 layers and a totem, the bell wakes nobody.
  - **Spawn test boxes:** two sealed, pitch-dark deepslate tile boxes, x -5..5, z 74..84. The warded one is at y 11..15 (interior 12..14, inside the ward box); the control at y 47..51 (interior 48..50, above the ward box). Both are 70+ blocks above the ground, so a player on the ground is past the 24-block no-spawn radius and within 128.
    - The world has `spawn_mobs false`; the Spawns on/off signs flip that game rule. On, monsters spawn in every dark place in the world, so turn it off after.
    - Count spawns sends a chat line with the number of mobs in each box (`tellraw`, scores `#in` / `#out` in `tw_hp`).
  - Ground signs at z 62 (south of the mining wall), facing north: Paired Room (tp up to (0.5, 24, 73.5)), Spawns on, Spawns off, Count spawns, Clear boxes.

### Paths

- Hub south x -1..1 to avenue A (z 16..17); avenue B z 33..34 (x -47..46); avenue C z 50..51; lanes x -27..-26 and 27..28 from z 18 to 49.

## Natural world

A second test world from normal generation, for seeing our things where the game puts them (owner).

- **Run:** `python tools/build-naturalworld.py` (`--no-build`, `--keep`, `--seed N`; default seed 20261122). About 90 s.
- **Pristine copy:** `testworlds/bubba-natural-world/`, **gitignored** (rebuilt from the seed). `tools/install-to-modrinth.ps1` installs it as the save "Bubba Natural World" when missing; `-ResetWorld` writes over it too.
- **Game rules:** vanilla throughout (owner: no odd game rules): time, weather and mob spawning all run, so the world plays like a real one and the Fabricator runs. It starts at morning (`time set 1000`) in clear weather. Creative, cheats on.
- **How it's built:** the same headless server as the flat world, on a normal world. The build server never pauses (`pause-when-empty-seconds=-1`).
  1. It finds a natural example of each thing:
     - crypt yards: the nearest from four spread-out starting points (vanilla `/locate structure`);
     - one camp of each kind (skeleton, zombie, husk, stray, bogged) with `/bubbas_reworked_mobs camp locate <kind>` (vanilla `/locate` gives the nearest camp of any kind);
     - the Nether outpost with `camp locate piglin`, run in the Nether;
     - the pillager outpost and a shipwreck (vanilla `/locate`);
     - a trader post with `trader_post locate`;
     - an Ancient Fabricator with `ancient_fabricator locate`, and a cairn with `cairn locate`;
     - old-growth spruce (or pine) taiga for giant spiders.
  2. It force-loads 24 blocks around each example (Nether chunks in the Nether).
  3. It re-runs `camp locate <kind>` from each camp's own centre and fails the build unless that returns the same camp ("camp sign <kind>: camp at x z confirmed <kind>"). `camp list` stays empty on a headless server, since camps register only when a player is near.
  4. It reads each spot's surface height while its chunks are loaded (a marker summoned `positioned over motion_blocking_no_leaves`), so every sign is a plain `tp @s X Y Z`. A heightmap lookup at click time did nothing, because the spot's chunks aren't loaded from spawn.
  5. It places one persistent giant spider (`spider giant`, 5 blocks east of the taiga example), so one is always there to see.
  6. It lays a polished andesite platform at world spawn with a click sign per example that teleports there.
  7. It saves, then keeps only fully generated chunks in the region files. `/locate` leaves about 10,000 part-generated chunks behind (about 85 % of the size), and they'd finish generating on a visit anyway: this took the world from 51 MB to 7.5 MB (829 chunks) at last measure.
  8. The found spots are written to `SPOTS.txt` in the world.
- **Where signs land:**
  - camps: 3 blocks south of the fire;
  - "Nether outpost (piglins)": into the Nether, 3 blocks south of the fire;
  - "Trader post (wandering trader)": the plaza centre, open ground between the stall and the fire;
  - the Fabricator: its pad height;
  - everything else: the surface.
- **Seed 20261122 spots:** camps skeleton 712 264, zombie 920 56, husk -1240 280, stray 232 2536, bogged -1704 1032; Nether outpost -1032 -213 (y 63); trader post -1672 -184 (y 63).
- **Not covered:** spider nests (a feature in 17 % of cave chunks, not locatable; the flat world's Showcase has one) and crypt wealth (rolled per yard, so the yards are whatever the seed gives).

## Screenshots

- **Run:** `./gradlew :testworldshots:runClientGameTest -PmilestoneTests` (Windows: `.\gradlew.bat`). It is a milestone test (test-lite, `specs/quality.md`) and is skipped without the flag.
  - About 50 s on the owner's PC with the mods already built (client start ~10 s, world open ~5 s, roughly 150 screenshots).
  - Screenshots land in `mods/testworldshots/build/run/clientGameTest/screenshots/NNNN_<station>_<view>.png`; the folder is wiped at the start of each run.
  - Look at them after any rebuild of the world or visible change to a station.
- **Parts (owner: split for time):** `-PclientTests=cairns,creepers` photographs only those stations. Each run still starts the client and loads the world (about 20-40 s of overhead); the `cairns` part alone took 74 s with its 30 s wait. `-PmilestoneTests` alone runs all of them. Run only the parts whose stations changed.
  - Parts: hub, preview (the hand-built cairn preview), creepers, cairns, graveyard, loot, arena, crafting, tower, gems, grounds (the testing grounds, beacons and warding), showcase.
- **How it works:** `mods/testworldshots` is an internal harness mod (never published, `internal=true`).
  - Its client game test loads the seven mods' built jars (`modsUnderTest` in its `build.gradle`; Gradle builds them first; Fabric Loader picks up every jar with a `fabric.mod.json` on the classpath) plus Fabric API.
  - The game loads a private copy of the jars (`snapshotModJars`, a Sync into `mods/testworldshots/build/modsUnderTest/`), not the jars in each mod's `build/libs`: another agent rebuilding a mod during a tour rewrote its jar in place and crashed the running game ("ZipFile invalid LOC header").
  - Chosen over a hand-made Modrinth-style launch because it reuses Loom's client game test run, Fabric's screenshot API and CI's existing `runClientGameTest` job unchanged. Its server game test task is switched off.
- **The world:** the test copies `testworlds/bubba-test-world` (path passed as `-Dbubba.testworld`) into the run's `saves/` and opens the copy with Fabric's `TestWorldSaveImpl`. (The public `worldBuilder()` only makes new worlds; this implementation class opens any save folder by name through `WorldOpenFlows`.) The pristine folder is never opened.
- **Camera:**
  - The player is switched to spectator (sleepers ignore spectators; no gravity; it can sit inside a wall to read signs), and the HUD is hidden (`gui.hud.toggle()`, the F1 key).
  - Each shot is a `tp` to an eye position, with yaw and pitch computed to look at a target, then waits until chunks have rendered. Night and frozen time come from the world itself.
  - Chest shots open the chest's menu on the server and take two pictures: the contents, and the first slot's tooltip (gems and effects).
  - Shots and coordinates are in `TestWorldShots.java`; update them with the Layout above when a station moves.
- **Checks during the tour** (any failure fails the run):
  - clicks the wardrobe's Speed, Haste (checks the three tools) and Take off signs as the player, with a third-person picture in the set;
  - flips the gallery lever and checks that arrows fly;
  - clicks Reset dummies and checks there are five again;
  - summons the Juggernaut for a picture;
  - waits up to 300 ticks for the trap beacon to hurt the pig (Poison gems hurt steadily rather than giving the Poison effect).
- **Shot names worth knowing:**
  - creepers: `creepers_signs_north`, `creepers_signs_south`, `creepers_grove`, `creepers_open`;
  - cairns: `cairns_arrival`, `cairns_signs`, `cairns_row`, `cairns_close_11`, `cairns_close_33`, `cairns_unfinished`, `cairns_repair_before`, then 30 s of work, `cairns_repair_after`, `cairns_unfinished_after`, `cairns_vigil`, `cairns_vigil_close`;
  - beacons and warding: `beacons_yard`, `beacons_yard_4layer_signs`, `beacons_gem_chest` (+ tooltip), `beacons_trap_zone`, `beacons_trap_poisoned`, `warding_bell_lane`, `warding_light_rings`, `warding_ground_signs`, `paired_room_beam`, `paired_room_signs`, `paired_room_sleepers`;
  - showcase: `showcase_strip`, `showcase_lava`, `showcase_geyser`, `showcase_crypt_row`, `showcase_noble_yard`, `showcase_pauper_yard`, `showcase_yard_states`, `showcase_camps`, `showcase_camp_skeleton`, `showcase_camp_bogged`, `showcase_nest_cave`, `showcase_giant_grove`.
