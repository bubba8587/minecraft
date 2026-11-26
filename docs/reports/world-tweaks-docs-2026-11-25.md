# World Tweaks docs report: deep-sea fortresses (2026-11-25)

For the orchestrator/docs agent to merge into `specs/world-tweaks.md` and the World Tweaks page in `player-docs/`, then delete.

## Player-facing (player docs)

**Deep-sea fortresses.** On the floor of deep oceans (deep, deep lukewarm, deep cold and deep frozen ocean) there are now flooded fortresses: the same sprawling layout as a Nether fortress, built of stone bricks (some mossy, some cracked) with prismarine trim and sea lanterns lighting the corridors, bridges and halls. They are about as rare as ocean monuments, sit on the sea floor with their pillars reaching down to it, and are always fully underwater.

- Inside are **drowned**, now and then a **guardian**, and a **guardian spawner** in the spawner room (dark prismarine floor).
- **Barrels** (instead of chests) hold ocean treasure: iron, gold, emeralds, the odd diamond, prismarine shards and crystals, sponges, a gold block now and then, cooked fish, maps to buried treasure, coast armor trims, nautilus armor, nautilus shells, and rarely a **Heart of the Sea**.
- **Conduit shrine:** in the middle of the central bridge crossing is a ring of prismarine and sea lanterns around an empty spot on a little pedestal. Put a conduit there (build one from a Heart of the Sea and nautilus shells) and it switches on: Conduit Power around it, and it attacks nearby drowned.
- **Bubble lifts:** the tall stair rooms have two bubble columns up through the landing: soul sand lifts you up, magma pulls you down. A sea lantern marks the top of each.
- **Finding one:** dolphins you feed fish can lead you to one, and about one in four buried-treasure maps from shipwreck map chests is a **Deep-Sea Fortress Map** instead. `/locate structure bubbas_world_tweaks:deepsea_fortress` works too.
- Works with Tectonic's deeper oceans: the fortress sits on the deeper floor.

## Spec facts as built (specs/world-tweaks.md, "Deep-sea fortresses")

- **Structure:** type `bubbas_world_tweaks:deepsea_fortress` (`DeepseaFortressStructure`), structure `bubbas_world_tweaks:deepsea_fortress`, step `surface_structures`, biomes `#bubbas_world_tweaks:has_structure/deepsea_fortress` = `#minecraft:is_deep_ocean`. Structure set `deepsea_fortresses`: random_spread spacing 32, separation 5, triangular (monument values), salt 726315904, exclusion zone 10 chunks from `minecraft:ocean_monuments`.
- **Layout:** `findGenerationPoint` runs vanilla's `NetherFortressPieces.StartPiece` + `addChildren` loop itself (same as `NetherFortressStructure`, no copied piece code) into a builder and returns the stub with the built builder. Floor = `ChunkGenerator.getFirstOccupiedHeight(OCEAN_FLOOR_WG)` at the start crossing's centre (real height, works on Tectonic). It skips the spot if that floor or any of four samples 40 blocks diagonally out is shallower than 14 below sea level. Pieces are moved so the start crossing's base is 1 above the floor; if the top would come within 2 of sea level the whole fortress moves down, at most 3 blocks into the floor (deeper than that and the spot is skipped). Vanilla's layout rises 13-37 blocks above the start crossing (mostly 15-25) and drops 0-21 below it (parts below dig into the floor, flooded inside), so on vanilla's deep-ocean floors (about y 35-45) many fortresses sink 1-3 blocks; on Tectonic's deeper floors they sit fully on the floor. Vanilla's pillar fill (`fillColumnDown`) runs down through water to solid ground.
- **The swap:** a mixin on `StructureStart.placeInChunk` (`@WrapMethod`) turns on a thread-local flag when the start's structure is ours (also covers `/place structure`, chunk reloads, any chunk order). Mixins on `StructurePiece` wrap the `setBlock` in `placeBlock` and `fillColumnDown`, and cancel `createChest` into a barrel; a mixin on `NetherFortressPieces.MonsterThrone` swaps the spawner's entity. All do nothing unless the flag is on, so real Nether fortresses (and every other structure) are untouched (milestone test `netherFortressPiecesUntouched`).
- **Block mapping:** air → water (any vanilla "clear to air" inside the fortress is water; nothing else is air); nether bricks → stone bricks, mossy 20 %, cracked 10 % by a position hash (stable per block); nether brick stairs → stone brick stairs (25 % mossy), properties copied, waterlogged; nether brick fence → prismarine wall, waterlogged (shape fixed by vanilla's post-processing, which the fence already asks for); lava (the entrance well) → magma block (a drag-down bubble column over the well); soul sand → sand; nether wart → sea pickles (1 to 4 by position); anything waterloggable is waterlogged. Barrels face out of their wall (vanilla's chest reorient) with `bubbas_world_tweaks:chests/deepsea_fortress`.
- **Intentional trim (owner: prismarine on purpose, not random).** Decided per vanilla piece class and the piece-local coordinates the generator is drawing (`FortressTrim`), so it lands on the same feature of every copy of a piece whatever its rotation:
  - every pillar/foundation: a prismarine brick band at the top of the pillar fill (local y -1, under arches and floors; was dark prismarine, which read as a gap from outside);
  - bridges (crossing, straight, end): parapet in prismarine bricks with a sea lantern every 6 blocks;
  - small corridors (straight, turns, crossing): dark prismarine runner down the floor centre, one sea lantern in the ceiling per piece;
  - T balcony: runner, ceiling lantern, prismarine brick balcony sill;
  - corridor stairs: two ceiling lanterns over the flight;
  - entrance hall and garden hall: prismarine brick floor border, prismarine door jambs with dark prismarine lintels, four sea lanterns in the ceiling, prismarine merlons on the roof; the entrance's well has a prismarine rim;
  - spawner room: whole floor dark prismarine, prismarine rim, sea lanterns in the two back corners;
  - room crossing: prismarine floor border, sea lantern set in the floor centre and on the four corner pillar tops, dark prismarine door lintels; stairs room: floor border, a ceiling lantern.
- **Round 2:**
  - Conduit shrine (`ConduitShrine`, from `Structure.afterPlace`): over the start crossing's centre, heart 7 above the crossing's base (3 above the deck); 16-block ring in the heart's horizontal plane (5x5 border: sea lanterns at the 4 corners, dark prismarine and prismarine bricks alternating), the 3x3x3 round the heart set to water, and a two-high waterlogged prismarine wall pedestal under the heart so a player can place the conduit (a waterlogged block still counts as water for the conduit's check). 16 blocks is vanilla's minimum (`ConduitBlockEntity.updateShape`); range 32 blocks. Milestone test `conduitShrineActivates` places a conduit and waits for `isActive()`.
  - Bubble lifts: in every stairs room (the only tall vertical room; the corridor stairs are a slope), two columns from the ground floor through holes in the upper landing: up at local (2, z 2) over soul sand, down at (4, z 2) over magma, each capped by a sea lantern just above landing level (so the up lift stops at the landing instead of carrying you to the surface, and the down lift doesn't make a whirlpool from the sea surface). The swap schedules a water tick over each soul sand/magma so the column forms both in worldgen and in a live world. Milestone test `stairsRoomLiftsForm`.
  - Dolphins: `data/minecraft/tags/worldgen/structure/dolphin_located.json` adds the structure (replace false).
  - Shipwreck maps: Fabric `LootTableEvents.MODIFY_DROPS` on `minecraft:chests/shipwreck_map`: with chance 0.25 (`DeepseaFortresses.SHIPWRECK_MAP_SHARE`) the buried-treasure map is replaced by one roll of `bubbas_world_tweaks:items/deepsea_fortress_map` (item `minecraft:ocean_monument_map`, item name "Deep-Sea Fortress Map", exploration_map to `#bubbas_world_tweaks:on_deepsea_fortress_maps`, monument marker, zoom 2, search radius 100, skip_existing_chunks false; discarded if none is found, and then the treasure map stays).
- **Spawns:** spawn_overrides monster, bounding_box `piece`: drowned weight 10 (1-3), guardian weight 1 (1-2); axolotls empty.
- **Lang:** `structure.bubbas_world_tweaks.deepsea_fortress` "Deep-Sea Fortress", `filled_map.bubbas_world_tweaks.deepsea_fortress` "Deep-Sea Fortress Map".
- No new blocks (B15). No new textures (all vanilla blocks), so nothing for `docs/art-todo.md` or the 32x pack.

### Barrel loot table `bubbas_world_tweaks:chests/deepsea_fortress` (builder's numbers)

| Pool | Rolls | Entries (weight, count) |
|---|---|---|
| Treasure | 3-6 | iron ingot 30 (1-5), gold ingot 20 (1-5), emerald 20 (1-5), prismarine shard 20 (2-8), prismarine crystals 15 (2-6), experience bottle 6 (1-3), diamond 5 (1-2), wet sponge 4 (1-2), gold block 3, TNT 3 (1-2) |
| Supplies | 2-4 | iron nugget 30 (1-10), gold nugget 20 (1-10), lapis 15 (1-10), cooked cod 10 (2-4), cooked salmon 10 (2-4), paper 8 (1-6), feather 5 (1-4) |
| Shells | 1 | empty 3, nautilus shell 2 (1-2): 40 % |
| Map | 1 | empty 3, buried treasure map 1: 25 % |
| Trim | 1 | empty 5, coast armor trim 1: about 17 % |
| Nautilus armor | 1 | empty 120, copper 20, iron 10, golden 6, diamond 3: about 21 % |
| Heart | 1 | empty 24, Heart of the Sea 1: 4 % |

Sources: shipwreck treasure/supply/map pools, buried treasure, ocean monument rewards (gold blocks, prismarine, sponges). No Nether items (owner).

## Builder's default choices (owner may change)

- Full vanilla fortress size and piece mix.
- Brick mix 20 % mossy / 10 % cracked; stairs 25 % mossy.
- Fences → waterlogged prismarine walls; lava well → magma (drag-down column above the well); soul sand → sand; nether wart → sea pickles (glow underwater).
- Trim and lantern placement as listed above.
- Spawn weights drowned 10 : guardian 1.
- Placement: base on the floor at the crossing centre, at least 14 deep at the centre and four samples 40 out, top at least 2 below sea level, sinking at most 3 into the floor; exclusion 10 chunks from monuments.
- Shrine: smallest frame (16), over the crossing, pedestal so the conduit can be placed without a helper block.
- Lifts: both directions in every stairs room, capped by sea lanterns (no lift to the sea surface). Magma is at the **bottom** of the down lift, since a vanilla drag-down column forms above magma (the spec's "magma at the top" can't work).
- Fortress map: 1 in 4 shipwreck treasure maps, monument marker, ocean explorer map item renamed.

## Milestone test objectives (for the spec's list)

- Core `deepseaSwapBuildsASeaRoom`: a spawner room and a chest corridor placed with the swap: no Nether blocks, no air in the room, stone-brick walls, dark prismarine floor, sea lantern, waterlogged prismarine walls, guardian spawner, deep-sea barrel, corridor runner and lantern, flooded corridor.
- `DeepseaFortressGameTest`: `netherFortressPiecesUntouched` (same pieces without the scope stay Nether, blaze spawner, nether-bridge chest), `barrelLootLoads`, `wholeFortressIsASeaFortress` (a full layout placed chunk by chunk over a sea built block by block: stone floor 10 below the measured floor, water up to every piece box grown by 1; asserts no Nether blocks or chests, guardian spawner, top 2+ below sea level, crossing on the measured floor, the crossing's four arm-end pillars reach the stone, and no air anywhere in, beside or under any piece down to the stone), `stairsRoomLiftsForm`, `conduitShrineActivates`, `dolphinsAndMaps` (tag membership and the map table loads).
- Objective, no test yet: a shipwreck map chest gives a fortress map about 1 in 4 times when a fortress is within range (checked by hand on a headless server, see below); drowned and guardians spawn inside the pieces' boxes.
- Client part `deepsea` (`DeepseaFortressClientGameTest`): a normal world with seed 20261122, the naturally generated fortress nearest the origin (crossing at 338..356, 36..45, 162..180): outside, under the crossing, a corridor, a stairs room lift, the shrine with a conduit placed. Camera spots are moved up to open water first (a camera inside a block shows an x-ray view that looks like air). The world tweaks lava test is now part `lava`.

## Headless server checks (real terrain, seed 20261122)

Headless Fabric server, normal world, `/locate structure` from (0,0), (6000,0), (0,6000); 15x15 chunks round two fortresses force-generated, saved, and the region files read offline (every column with a fortress block: lowest fortress block, ground under it, any air from the ground to one above the top).

| World | Fortress (chunk corner) | Top y | Ground under fortress columns | Columns on ground / over water | Air |
|---|---|---|---|---|---|
| Vanilla | 336, 160 | 61 | 8..47; crossing arm-end pillars on sand/stone at y 34-35, deck 39-40 | 3844 / 1203 | 0 |
| Vanilla | 6672, 400 | 61 | -23..43; arm-end pillars on gravel/stone at 35-37 | 5655 / 1481 | 0 |
| Tectonic 3.1.0-beta2 + Lithostitched 2.0.6 | -2880, 3616 | 19 | -16..12; arm ends to y 0-1 | 5786 / 704 | 0 |
| Tectonic | 7552, 1024 | 33 | -7..27; arm ends on gravel/stone at 12-15 | 4765 / 550 | 0 |

"Over water" columns are bridge spans and open arches between pillars (vanilla's fortress has pillars only at piece ends and under castle floors); every pillar column reaches the ground. On Tectonic the fortresses sit 30-45 blocks below sea level.

Shipwreck map chests (24 `/loot insert` of `minecraft:chests/shipwreck_map`, vanilla world): 21 buried treasure maps, 3 Deep-Sea Fortress Maps; `/loot spawn ... bubbas_world_tweaks:items/deepsea_fortress_map` next to a fortress gives a Deep-Sea Fortress Map. On the Tectonic world the shipwreck map rolls gave no treasure maps at all near that spot (vanilla discards the buried-treasure map when none is in range), so no swap happened there.

## The owner's "air under the sections" (cause)

Not the mod: the first client screenshots were taken in the client test's flat world, where I faked a sea floor out of sand over empty space. The sand fell (and lagged the PC), leaving open air under the fortress, and the water I'd filled sat over it, so the sections looked like they hung over nothing. Real worldgen (table above, with and without Tectonic) has no air in or under the fortress and every pillar reaches the floor. Fixes: the client test now uses a seeded normal world and shoots a naturally generated fortress; the server game test builds its sea block by block (stone floor, water) and checks for air and pillars; the dark prismarine pillar band is now prismarine bricks so it no longer reads as a gap.

## Open items

- Bridge spans have water under them between pillars, as in vanilla's fortress; if the owner wants solid foundations or extra pillars under bridges, that's a design change.
- Many vanilla-terrain spots sink the fortress 1-3 blocks into the floor (lower rooms dig in); spots needing more are skipped, so vanilla oceans get somewhat fewer fortresses than monuments; Tectonic gets the full rate.
- No automated test yet that a shipwreck chest yields a fortress map about 1 in 4 times (checked by hand above), or for drowned/guardian spawns in the boxes.
