<!-- dte:C8,B15,B17 -->
# Bubba's World Tweaks (spec)

**Status: built.** Player pages: `player-docs/Projects/Bubba's World Tweaks/`.

Mod id `bubbas_world_tweaks`. A small mod for world and environment features that aren't hostile mobs, recipes or tools (owner: "i'm sure we'll have others"). Golems live here too (owner, 2026-11-27): small built creatures that make the world feel alive, never a stand-in for another mod's utility.

## Lava bubbles and lava geysers (owner's design)

- **Lava bubbles:** a **magma block under lava** makes bubbles on the lava surface nearby: the lava version of magma making bubble columns in water (B17: reuse that idea).
  - Each bubble is a **little half-dome on the surface** that swells, then pops with a **goopy "plop"**. Crisp pixel art in a vanilla palette, lit like lava.
  - Owner: bubbles are **slow and sparse**.
  - No new blocks (B15): the magma block and the lava are vanilla.
- **Lava geysers:** now and then a bubbling spot releases **a stream of black smoke**, reusing the look of 26.3's **sulfur geyser** (`PotentSulfurBlockEntity` eruption and its geyser particles) in soot.
- **Lift:** **sulfur geysers and lava geysers both push players up and give a little speed boost.**
  - Vanilla sulfur geysers already launch entities upward (`GEYSER_BASE_LAUNCH_SPEED` 0.3, `GEYSER_LAUNCH_FORCE` 0.2). This mod adds the speed boost to both, and the same launch to lava geysers.
  - It's made for the Glider (Bubba's Extra Recipes): circle a geyser to climb, so the Glider works for Nether travel. It must work without the Glider installed too.
- **Worldgen: none needed** (owner). Vanilla's `ore_magma` already places magma blobs at y 27-36 in the Nether (4 per chunk), around the lava sea surface at y 31, so bubbles and geysers appear naturally along lava sea shores and shallows.
- **Prior art:** Lava Particles | More (client-only boiling bubbles, LGPL, no 26.3 build, no geysers or lift). Nothing does lift.

## As built (2026-11-22, agent 3)

**(builder)** marks the builder's choices. Numbers are in `WorldTweaksTuning`.

### Lava bubbles

- Where a magma block sits under lava (up to 4 deep, `MAGMA_DEPTH`), the surface above it bubbles.
- **Rate:** 6 % per lava display tick over magma (`BUBBLE_CHANCE` 0.06), only where there's air above the lava. Owner: 18 % was "much too fast and frequent", and 0.6 % read as no bubbles at all in play. The client shots spawn their own bubbles, so they don't show the rate; the owner judges that in play.
- **Life:** swells for 50 + 0-30 ticks (2.5-4 s; `BUBBLE_SWELL_TICKS`, `BUBBLE_LIFE_SPREAD`), then bursts over 6 ticks (`BUBBLE_POP_TICKS`) into a ring of droplets.
- **Sound (builder):** vanilla's lava pop at half pitch, volume 0.55 (`PLOP_PITCH`, `PLOP_VOLUME`).
- **Shape:** the particle faces the camera and is centred on the surface, so the lava hides its lower half from the side (a half-dome), and from above it reads as a round blister. **(builder):** a dome turned only around the vertical axis looked squashed from above.
- **Code:** display only. `mixin/LavaFluidBubbleMixin` at the tail of `LavaFluid.animateTick` (client display tick) spawns `lava_bubble` (`client/LavaBubbleParticle`, 7 frames). Nothing runs on the server.

### Lava geysers

The vent model, after vanilla's sulfur geysers (owner):

- **Every magma block under lava is a vent** (up to `MAGMA_DEPTH` + 1 = 5 lava blocks deep, air above). Each keeps its own steady rhythm, the way every vanilla potent sulfur block keeps its own timer (`PotentSulfurBlockEntity`, positional random from the world seed).
  - Owner: every magma block counts. (A grid with one candidate column per cell left magma outside the chosen column never erupting, so the owner rejected it.)
  - No spacing between vents (owner): neighbouring vents can erupt together; only one eruption per spot at a time.
- **Rhythm:** quiet for 80 s + 0-100 s (fixed per vent from the world seed and its position) + 5 s per extra lava block above it, then erupts for 3 s (`VENT_QUIET_BASE`, `VENT_QUIET_SPREAD`, `VENT_QUIET_PER_DEPTH`, `GEYSER_DURATION_TICKS` 60). Owner asked for these slower, shorter eruptions ("two bumps down").
- **Nothing is stored:** the phase comes from the seed, the position and the game time.
- **Checks:** every 40 ticks, each player's loaded chunks within 24 blocks, from 21 blocks below the player to 8 above (16 + `MAGMA_DEPTH` + 1 down), skipping chunk sections that hold no magma (vanilla `LevelChunkSection.maybeHas`). So vents only erupt near players.
- **Effects:** a column of black smoke (`lava_geyser_plume`, `client/LavaGeyserPlumeParticle`: vanilla's sulfur geyser plume motion, our soot sprites, plus large smoke), lava sparks and a low rumble.
- **Code:**
  - `lava/LavaGeyser`: an invisible, never-saved marker entity at the surface; the client draws its effects (`LavaGeyserEffects`).
  - `lava/LavaGeysers`: the entity type and the per-player check on `ServerTickEvents.END_LEVEL_TICK`.

### Lift and boost

- **Lift:** over an erupting lava geyser, players are pushed up the way vanilla's sulfur geyser pushes: a column 14 high and half-width 0.8, +0.2 a tick while rising slower than 0.9 (`GEYSER_HEIGHT`, `GEYSER_RADIUS`, `LIFT_PUSH`, `LIFT_MAX_RISE`).
- **Boost:** over an erupting lava **or sulfur** geyser (sulfur within 16 blocks below), horizontal speed grows 6 % a tick, up to 1.4 blocks a tick (`BOOST_FACTOR`, `BOOST_MAX`, `SULFUR_SEARCH_DEPTH`).
  - Circle a geyser to climb and gain speed.
  - Works on foot, falling, with an elytra or the Glider; nothing needs the Glider.
  - Not for spectators or creative fliers (like vanilla).
- **Code:** `lava/GeyserLift` runs from a `Player.tick` mixin (`PlayerGeyserMixin`) on both sides, because a player's own movement is decided on their client (vanilla's geyser launch works the same way). The sulfur boost reads the erupting `PotentSulfurBlock.STATE` under the player instead of hooking vanilla's lambda ticker.

### Tests

- **Core:**
  - `magmaUnderLavaIsABubblingSpot`
  - `lavaGeyserLiftsAndBoosts`: nothing without an eruption; lift and boost with one; no second geyser on the same spot.
  - `sulfurGeyserBoosts`: boost, no extra lift.
- **Milestone** (`VentRhythmGameTest`): `everyVentKeepsItsOwnSteadyRhythm` (steady per-vent rhythms, slower under deeper lava); `aLoneMagmaBlockErupts` (a lone magma block under a pool erupts through the real check).
- **Client** (`BubbasWorldTweaksClientGameTest`): the shots under "Art redo" below. The bubble scene fills a whole pool with magma, so it's denser than a natural lava sea.

### Art redo and slower bubbles (2026-11-22, agent 3, owner feedback)

The art, after owner feedback that the first bubbles looked far off from lava's texture and had outlines they didn't need. Made by `tools-local/make_lava_bubble_textures.py` (both rows in `docs/art-todo.md`; texcheck OK). The mod icon isn't made yet (B14: person-made; on `docs/art-todo.md`).

- **Bubbles** (`lava_bubble`, 7 frames, 16x and 32x): a swelling of the lava surface.
  - Every pixel comes from vanilla lava's own colour ramp: `lava_still.png` for 16x and `lava_flow.png` for 32x, picked by how often each colour appears.
  - Brightest on top, with a small soft highlight at lava's brightest yellow, slightly darker toward the base.
  - The lava texture's own grain is mixed in. No outline, no dark crust.
  - Burst frames are droplets in lava's bright yellows.
  - A crisp pixel-art half-dome, lit full bright.
  - 32x: the same method, on the 32px lava ramp and grain (owner picked this over a less grainy, brighter-topped variant).
- **Smoke** (`lava_geyser_plume_01..08`): vanilla's plume sprites with their three shades (193/210/224) recoloured to soot (36/52/70), drawn untinted and lit by the world like vanilla's. 32x adds an in-between shade at edges. Next to a vanilla geyser it reads as the same kind of puffy, shaded plume, in soot. (Multiplying vanilla's sprites nearly to black flattened them into featureless shapes.)
- **Art client shots** (`runClientGameTest -PmilestoneTests`, each at 16x and 32x, all looked at):
  - `lava-bubble-close`, `lava-bubble-side` (half-domes at surface level), `lava-bubbles-patch` (medium distance);
  - `lava-geyser-vs-sulfur`: our geyser beside vanilla's geyser particle over a water pit. A `potent_sulfur[state=continuous]` block alone didn't erupt in the test, so the shot spawns vanilla's `GEYSER` particle there.
- The bubbles blend in so well that from a medium distance they're subtle. That's by design, per the owner's feedback; easy to brighten if the owner wants them more visible.

## Parasitic (owner's design, 2026-11-25; built 2026-11-25)

An enchantment like Mending, but it feeds on its wearer instead of XP: now and then it hurts you and uses that health to mend its item. Higher levels take a little more health and give proportionally more durability for it (owner). World Tweaks is the catch-all home (owner).

- **Found (owner):** at the enchanting table, in villager trades and on random loot, like ordinary enchantments (in `#minecraft:non_treasure`): it's weaker than Mending and comes with a drawback, so it isn't treasure. Weight 2, as rare at the table as Fortune and Looting *(default)*; levels cost 10 / 19 / 28 to 60 / 69 / 78 *(default)*; anvil cost 4 (Mending's).
- **Levels:** I-III *(default; owner's example went to II)*. Each bite takes a random share (25-100 %, `PARASITIC_MIN_SHARE`) of the level's caps, health and durability alike, so the ratio is fixed per level:

| Level | Health, at most | Durability, at most | Durability per health |
|---|---|---|---|
| I | 1 (half a heart) | 20 | 20 |
| II | 1.5 | 35 | ~23 |
| III | 2 | 55 | 27.5 |

  Nerfed by the owner (2026-11-25: "nerf parasitic"; the durability caps cut by about a third, health caps kept *(default amount)*). Before: 30 / 55 / 85, the owner's example numbers for I and II, III continuing the ladder. The caps are data: the enchantment's `bubbas_world_tweaks:parasitic` effect component (`max_health`, `max_repair`, level-based values) in `data/bubbas_world_tweaks/enchantment/parasitic.json`.
- **Exclusive with Mending (owner):** `exclusive_set: minecraft:mending` (vanilla checks both ways).
- **Can kill you (owner):** no health floor. Damage type `bubbas_world_tweaks:parasitic` (in the same tags as vanilla starvation: bypasses armour, effects and wolf armour, no knockback); death message "was eaten alive by their own gear".
- **When:** checked once a second per survival or adventure player (`Parasitic`, a level tick). After a bite, a cooldown of 30-90 s (`PARASITIC_COOLDOWN_*`), then a 5 % chance each second (`PARASITIC_BITE_CHANCE`, about 20 s more on average) *(defaults; owner: random-ish frequency with a cooldown)*. Only a **damaged** Parasitic item bites, worn or held in any slot (`slots: any`, as Mending); with several, one at random. A bite that would over-mend takes only the health the repair needs. If the health doesn't come off (the player was just hit and is briefly invulnerable), nothing is mended and it tries again next second. Creative and spectator players are never bitten.
- **How:** like vanilla's Mending, the enchantment carries an effect component (a custom one, `Parasitic.EFFECT`) and the code walks the player's equipment with `EnchantmentHelper.runIterationOnEquipment`, so slot rules and levels come from the enchantment data.
## Deep-sea fortresses (owner's idea, 2026-11-25; built 2026-11-26)

Flooded fortresses on the floor of deep oceans: a Nether fortress's layout, built mostly of stone bricks with prismarine worked in, guarded by drowned and a guardian spawner (no natural guardian spawns: owner, 2026-11-26, "guardians are really annoying"), with barrels instead of chests (owner). *(default)* marks the agent's picks for what the owner left open.

- **Layout (owner):** vanilla's Nether fortress generator itself (`NetherFortressPieces`, code-built pieces, not templates), run with a block swap while our structure's pieces place (no copied vanilla code). Full vanilla fortress size and piece mix *(default)*.
- **Blocks:** nether bricks → stone bricks, with mossy and cracked stone bricks mixed in by position (stable per block). **Prismarine is placed intentionally, not at random (owner):** as trim on chosen features of each piece (e.g. floor borders, arch and doorway frames, pillar caps, window sills, the spawner room) *(default which features)*. **Lights (owner):** sea lanterns (and the like) set in on purpose, e.g. in ceilings and pillar tops at regular spacing *(default placement)*; nether brick stairs → stone brick stairs; nether brick fences → **windows where they sit in a wall** (owner, 2026-11-26: "not walls in the windows, windows in the windows"): cyan stained glass panes, now and then missing (water), and prismarine walls, waterlogged, where they are railings (owner, 2026-11-26); **sand piled round the support pillars' feet** on the sea floor (owner, 2026-11-26); **wear and tear (owner, 2026-11-26):** a couple of diagonal erosion tears carved through each fortress after it is built; **coral clumps** on the fortress in deep lukewarm ocean only (owner, 2026-11-26: "warmer ones only", matching vanilla's coral climate); sea lanterns about half as many as first built (owner, 2026-11-26); the lava well → a magma block (a bubble column) *(default)*; soul sand and nether wart (the stair-room garden) → sand with sea pickles *(default)*. No new blocks (B15).
- **Flooded (owner):** every block the generator would set to air inside the fortress is water; anything that can be waterlogged is.
- **Barrels (owner):** chests become barrels (facing out of their wall) with our own loot table `bubbas_world_tweaks:chests/deepsea_fortress`: **ocean loot, not the Nether-flavoured fortress table (owner):** drawn from vanilla's shipwreck (treasure, supply, map), buried treasure and ocean-monument rewards (gold blocks, prismarine, sponges), plus nautilus shells and a rare Heart of the Sea *(default contents and weights; builder proposes numbers)*.
- **Spawner (owner):** the blaze spawner becomes a **guardian spawner** ("there's not really a lot of available hostile mobs in the sea").
- **Spawns (owner):** inside the fortress (its pieces' boxes, like vanilla's fortress spawn override) monsters are **drowned** only; guardians come only from the spawner (owner, 2026-11-26). The spawner keeps vanilla's spawner defaults, the same as a Nether fortress blaze spawner.
- **Where (owner):** deep ocean biomes only (deep ocean, deep lukewarm, deep cold, deep frozen), rare: about as rare as ocean monuments *(default: a structure set spaced like monuments)*. Seated on the sea floor, its base fortified into the floor (the generator's pillar fill reaches down to solid ground), and kept below sea level.
- **Tectonic (owner, 2026-11-25: "compat with Tectonic, which can offer deeper seas"):** placement reads the real ocean-floor height from the chunk generator (no assumed vanilla depths), so on Tectonic's deeper floors the fortress sits deeper and its pillars reach the floor; it stays below sea level. Tectonic keeps vanilla's biomes, so the deep-ocean biome tag still applies. Verified on a headless server with Tectonic (locate and look at real placements), not only in flat game-test worlds.
- **Round 2 (owner picked, 2026-11-25; build after the first version merges):**
  - **Conduit shrine:** in the central courtyard (the fortress's big crossing) a prismarine conduit frame with an empty heart. A player who places a conduit there activates it (vanilla conduit: Conduit Power round it, and it attacks hostile mobs in range, so drowned near it take damage). The Heart of the Sea in the loot is the way to make one. Frame size and spot *(default)*: the smallest frame that activates (16 blocks), centred over the crossing.
  - **Bubble lifts:** the tall stair shafts become bubble columns: soul sand at the foot of up-shafts, magma at the top of down-shafts *(default which shafts)*, so the levels connect quickly. Vanilla bubble columns.
  - **Dolphins lead to it:** add the structure to vanilla's `#minecraft:dolphin_located` structure tag, so a dolphin fed fish can guide a player to the nearest one.
  - **Treasure maps point to it:** shipwreck map chests sometimes hold an explorer map to the nearest deep-sea fortress instead of a buried treasure map *(default odds, e.g. 1 in 4 of the maps)*.
- `/locate structure bubbas_world_tweaks:deepsea_fortress` works like any structure. Test world: a station or a natural-world spot *(default)*.
### As built (builder's report, 2026-11-26)

Where this differs from the design above, this wins (e.g. a down lift has its magma at the bottom: vanilla's pull-down column forms above magma).

### Spec facts as built (specs/world-tweaks.md, "Deep-sea fortresses")

- **Structure:** type `bubbas_world_tweaks:deepsea_fortress` (`DeepseaFortressStructure`), structure `bubbas_world_tweaks:deepsea_fortress`, step `surface_structures`, biomes `#bubbas_world_tweaks:has_structure/deepsea_fortress` = `#minecraft:is_deep_ocean`. Structure set `deepsea_fortresses`: random_spread spacing 32, separation 5, triangular (monument values), salt 726315904, exclusion zone 10 chunks from `minecraft:ocean_monuments`.
- **Layout:** `findGenerationPoint` runs vanilla's `NetherFortressPieces.StartPiece` + `addChildren` loop itself (same as `NetherFortressStructure`, no copied piece code) into a builder and returns the stub with the built builder. Floor = `ChunkGenerator.getFirstOccupiedHeight(OCEAN_FLOOR_WG)` at the start crossing's centre (real height, works on Tectonic). It skips the spot if that floor is shallower than 22 below sea level (`MIN_DEPTH`; centre only, no edge samples: owner, 2026-11-26). Pieces are moved so the start crossing's base is 1 above the floor; if the top would come within 6 of sea level (`TOP_GAP`, owner, 2026-11-26: deeper than the old 2) the whole fortress moves down **as far as it needs** into the floor (no sink limit: owner, 2026-11-26, so tall many-layered layouts aren't skipped). Vanilla's layout rises 13-37 blocks above the start crossing (mostly 15-25) and drops 0-21 below it; with 22+ of water and a 6 gap, up to about 15 rise above the floor, so tall layouts bury their lower layers: buried pieces are flooded tunnels and rooms in the rock (each piece fills its own box with walls and water; no terrain carving, since vanilla's beard adaptation would heap ground under the bridges and hide the pillars). The conduit shrine can end up below the floor in a tall one. On Tectonic's deeper floors they mostly sit on the floor. Milestone test `tallFortressesSinkToFit`. Vanilla's pillar fill (`fillColumnDown`) runs down through water to solid ground.
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
  - Bubble lifts: in every stairs room (the only tall vertical room; the corridor stairs are a slope), two columns from the ground floor through holes in the upper landing: up at local (2, z 2) over soul sand, down at (4, z 2) over magma, each capped by a sea lantern with two water blocks of headroom above the landing (owner, 2026-11-26; local y 10, the room's top row; columns run y 2-9) (so the up lift stops at the landing instead of carrying you to the surface, and the down lift doesn't make a whirlpool from the sea surface). The swap schedules a water tick over each soul sand/magma so the column forms both in worldgen and in a live world. Milestone test `stairsRoomLiftsForm`.
  - Dolphins: `data/minecraft/tags/worldgen/structure/dolphin_located.json` adds the structure (replace false).
  - Shipwreck maps: Fabric `LootTableEvents.MODIFY_DROPS` on `minecraft:chests/shipwreck_map`: with chance 0.25 (`DeepseaFortresses.SHIPWRECK_MAP_SHARE`) the buried-treasure map is replaced by one roll of `bubbas_world_tweaks:items/deepsea_fortress_map` (item `minecraft:ocean_monument_map`, item name "Deep-Sea Fortress Map", exploration_map to `#bubbas_world_tweaks:on_deepsea_fortress_maps`, monument marker, zoom 2, search radius 100, skip_existing_chunks false; discarded if none is found, and then the treasure map stays).
- **Spawns:** spawn_overrides monster, bounding_box `piece`: drowned weight 10 (1-3) only; axolotls empty. The guardian spawner is vanilla's `BaseSpawner` with its defaults (delay 200-800 ticks, 4 per try, at most 6 nearby, player within 16), exactly as vanilla's blaze spawner; only the entity is swapped.
- **Windows and glazed tiles:** `FortressTrim.isWindow` picks which vanilla fences are windows: every fence in small corridors, corridor stairs, room crossings and stairs rooms; y 10-11 in the entrance and garden halls (the wall windows; their y 13 parapet and gallery rails stay walls); in the T balcony, its wall openings (z 0 or y 4+), not the balcony rail; never on bridges or in the spawner room. A window is water 10 % by position hash (`FortressBlocks.BROKEN_WINDOW`), else a waterlogged cyan stained glass pane with the fence's connections (vanilla's post-processing fixes its shape, as for the fence). No glazed terracotta (tried, removed: owner, 2026-11-26).
- **Sand piles:** the `fillColumnDown` wrap notes each pillar block whose block below is solid (a foot); after the chunk's pieces are placed, `Structure.afterPlace` runs `SandPiles`: round each foot, out to 2 blocks, sand on the floor (the lowest water block on solid ground within 2 up or down) at 85 % next to the pillar and 35 % one further, plus a second layer at 30 % on the four sides next to it. Only water is replaced, never inside a piece's box, only inside the chunk being placed (so a pile can be cut at a chunk edge). Done after the pillars so sand never stops a pillar short. Checked in `wholeFortressIsASeaFortress`.
- **Erosion tears:** `addTears` (after `layOut` in `findGenerationPoint`) adds 2-3 `TearPiece`s (registered piece type `bubbas_world_tweaks:deepsea_tear`, saved with the start) at the end of the piece list, so in each chunk they run after the fortress pieces. Each is a straight segment 28-48 long, sloped 25-50 degrees from level, centred on a piece that stands at least 3 above the floor (12 tries, else any piece), radius 2-3, tapered toward the ends with a ragged edge (up to 1.2 more by position hash). Along it every block `FortressBlocks.erodes` (stone bricks and their stairs, prismarine, dark prismarine, prismarine walls, sea lanterns, cyan panes, sea pickles, magma, soul sand) becomes water; spawners, barrels and the natural seabed are untouched, and walls, panes and stairs beside a cut are marked for a shape fix-up. Test `tearErodesStonework`.
- **Coral:** in `afterPlace`, 6 random columns per chunk over a fortress piece, each 60 % to get vanilla's `minecraft:warm_ocean_vegetation` feature (the random coral tree, claw or block clump of warm oceans) placed on the highest sturdy fortress top in that column, if the biome there is deep lukewarm ocean. Placed after the shrine and sand, so coral can grow on eroded spots.
- **Lights (halved, owner 2026-11-26):** bridge parapet lanterns every 12 (was 6); small corridors and T balconies keep their ceiling lantern on about every other piece (`FortressTrim.lit`, by the piece box's position hash); corridor stairs 1 (was 2); halls 2 diagonal ceiling lanterns (was 4); spawner room 1 in the middle of the back wall (was the 2 back corners); room crossing pillar tops 2 diagonal corners (was 4). Kept: the room crossing's floor centre, the stairs room landing lantern, lift caps and the shrine.
- **Lang:** `structure.bubbas_world_tweaks.deepsea_fortress` "Deep-Sea Fortress", `filled_map.bubbas_world_tweaks.deepsea_fortress` "Deep-Sea Fortress Map".
- No new blocks (B15). No new textures (all vanilla blocks), so nothing for `docs/art-todo.md` or the 32x pack.

#### Barrel loot table `bubbas_world_tweaks:chests/deepsea_fortress` (builder's numbers)

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

### Builder's default choices (owner may change)

- Full vanilla fortress size and piece mix.
- Brick mix 20 % mossy / 10 % cracked; stairs 25 % mossy.
- Fences → cyan stained glass panes (some missing) in walls, waterlogged prismarine walls as railings; sand piles round pillar feet; lava well → magma (drag-down column above the well); soul sand → sand; nether wart → sea pickles (glow underwater).
- Trim and lantern placement as listed above.
- Natural spawns drowned only (guardians from the spawner).
- Placement: base on the floor at the crossing centre, at least 22 deep at the centre (no edge samples), top at least 6 below sea level, sinking into the floor as far as that needs; exclusion 10 chunks from monuments.
- Shrine: smallest frame (16), over the crossing, pedestal so the conduit can be placed without a helper block.
- Lifts: both directions in every stairs room, capped by sea lanterns (no lift to the sea surface). Magma is at the **bottom** of the down lift, since a vanilla drag-down column forms above magma (the spec's "magma at the top" can't work).
- Fortress map: 1 in 4 shipwreck treasure maps, monument marker, ocean explorer map item renamed.

### Milestone test objectives (for the spec's list)

- Core `deepseaSwapBuildsASeaRoom`: a spawner room and a chest corridor placed with the swap: no Nether blocks, no air in the room, stone-brick walls, dark prismarine floor, sea lantern, waterlogged prismarine walls, guardian spawner, deep-sea barrel, corridor runner and lantern, flooded corridor.
- `DeepseaFortressGameTest`: `netherFortressPiecesUntouched` (same pieces without the scope stay Nether, blaze spawner, nether-bridge chest), `barrelLootLoads`, `wholeFortressIsASeaFortress` (a full layout placed chunk by chunk over a sea built block by block: stone floor 10 below the measured floor, water up to every piece box grown by 1; asserts no Nether blocks or chests, guardian spawner, top 2+ below sea level, crossing on the measured floor, the crossing's four arm-end pillars reach the stone, and no air anywhere in, beside or under any piece down to the stone), `stairsRoomLiftsForm`, `conduitShrineActivates`, `dolphinsAndMaps` (tag membership and the map table loads).
- Objective, no test yet: a shipwreck map chest gives a fortress map about 1 in 4 times when a fortress is within range (checked by hand on a headless server, see below); drowned and guardians spawn inside the pieces' boxes.
- Client part `deepsea` (`DeepseaFortressClientGameTest`): a normal world with seed 20261122, the naturally generated fortress nearest the origin (crossing at 338..356, 36..45, 162..180): outside, under the crossing, a corridor, a stairs room lift, the shrine with a conduit placed. Camera spots are moved up to open water first (a camera inside a block shows an x-ray view that looks like air). The world tweaks lava test is now part `lava`.

### Headless server checks (real terrain, seed 20261122)

Headless Fabric server, normal world, `/locate structure` from (0,0), (6000,0), (0,6000); 15x15 chunks round two fortresses force-generated, saved, and the region files read offline (every column with a fortress block: lowest fortress block, ground under it, any air from the ground to one above the top).

| World | Fortress (chunk corner) | Top y | Ground under fortress columns | Columns on ground / over water | Air |
|---|---|---|---|---|---|
| Vanilla | 336, 160 | 61 | 8..47; crossing arm-end pillars on sand/stone at y 34-35, deck 39-40 | 3844 / 1203 | 0 |
| Vanilla | 6672, 400 | 61 | -23..43; arm-end pillars on gravel/stone at 35-37 | 5655 / 1481 | 0 |
| Tectonic 3.1.0-beta2 + Lithostitched 2.0.6 | -2880, 3616 | 19 | -16..12; arm ends to y 0-1 | 5786 / 704 | 0 |
| Tectonic | 7552, 1024 | 33 | -7..27; arm ends on gravel/stone at 12-15 | 4765 / 550 | 0 |

"Over water" columns are bridge spans and open arches between pillars (vanilla's fortress has pillars only at piece ends and under castle floors); every pillar column reaches the ground. On Tectonic the fortresses sit 30-45 blocks below sea level.

Shipwreck map chests (24 `/loot insert` of `minecraft:chests/shipwreck_map`, vanilla world): 21 buried treasure maps, 3 Deep-Sea Fortress Maps; `/loot spawn ... bubbas_world_tweaks:items/deepsea_fortress_map` next to a fortress gives a Deep-Sea Fortress Map. On the Tectonic world the shipwreck map rolls gave no treasure maps at all near that spot (vanilla discards the buried-treasure map when none is in range), so no swap happened there.

### The owner's "air under the sections" (cause)

Not the mod: the first client screenshots were taken in the client test's flat world, where I faked a sea floor out of sand over empty space. The sand fell (and lagged the PC), leaving open air under the fortress, and the water I'd filled sat over it, so the sections looked like they hung over nothing. Real worldgen (table above, with and without Tectonic) has no air in or under the fortress and every pillar reaches the floor. Fixes: the client test now uses a seeded normal world and shoots a naturally generated fortress; the server game test builds its sea block by block (stone floor, water) and checks for air and pillars; the dark prismarine pillar band is now prismarine bricks so it no longer reads as a gap.

### Open items

- Bridge spans have water under them between pillars, as in vanilla's fortress; if the owner wants solid foundations or extra pillars under bridges, that's a design change.
- Many vanilla-terrain spots sink the fortress 1-3 blocks into the floor (lower rooms dig in); spots needing more are skipped, so vanilla oceans get somewhat fewer fortresses than monuments; Tectonic gets the full rate.
- No automated test yet that a shipwreck chest yields a fortress map about 1 in 4 times (checked by hand above), or for drowned/guardian spawns in the boxes.

## Golems (owner, 2026-11-27)

Golems in World Tweaks are flavor and play, not tools. **Rule (owner):** a golem must not supplant or fill gaps in another mod's utility, above all Totems & Artifacts, Reworked Beacons and Infused Gems. So no buffs, no warding or fighting for the player, no item gathering, farming, herding, terrain repair, light sources or locating. Amethyst is a gem and beacon material, so an amethyst golem is never a source of shards.

Other golem ideas pitched to the owner, not yet picked: magma golem riding lava geysers, grave-tender iron golem at crypt yards, storm-chasing copper golems, snowball-fight snow golems, ice sled golem.

### Amethyst Golem (designed, not built)

A small built golem that records note block tunes and sings them back in amethyst chimes.

- **Built only (owner):** never spawns naturally. Built like every vanilla golem: a carved pumpkin placed on **one amethyst block** (copper golem size, owner). No new blocks (B15).
- **Recording (owner):** right-click it and it starts listening. Every note block played nearby is stored (pitch and timing) until it's right-clicked again. Time cap: long (builder's start: 5 minutes); notes are a few bytes each, so storage is cheap.
  - If it heard nothing between the two clicks, the recording is wiped.
- **Singing in amethyst (owner):** it plays its recording back with amethyst chime sounds at the recorded pitches, not the note block instruments.
  - **Loudness:** its own sound event `bubbas_world_tweaks:entity.amethyst_golem.sing` plays vanilla's `block/amethyst/shimmer` at volume 1.0. Vanilla's `block.amethyst_block.chime` plays the same file at 0.2, which the owner found nearly inaudible.
- **Tuning (owner):** right-click with an amethyst shard (not used up) to step the whole song up a semitone, wrapping around like a note block.
- **Jukebox as workstation (owner):** the nearest jukebox is its assigned spot, like a villager's job block.
  - If a jukebox nearby is playing a disc, it dances to it (like an allay).
  - Otherwise it stands on its jukebox and sings its recorded song **non-stop** (owner).
  - **One golem per jukebox (owner).** For a choir, put jukeboxes side by side and record the same tune into each golem.
- **Synced starts (owner):** a song only ever starts on a fixed beat of the day clock (every second, day time a multiple of 20 ticks; owner: a 1000-tick hour beat left golems silent most of the time). When a song ends it waits for the next beat. Golems with the same song stay in step, and a new one falls in at the next beat.
- **Wind chime (owner):** a golem with no recording echoes the world instead: every vibration at or above a **numeric floor** on vanilla's vibration frequency (1-15) becomes an amethyst chime, pitched by that frequency. Owner: the floor is the only filter, no other exceptions. Builder's start: 2 (footsteps are 1).
- **Noise (owner):** its sounds are ordinary noise. Nothing special for Reworked Mobs; if sleepers there hear it like any other noise, that's fine. Don't special-case it.
- **Drops:** never more amethyst than it was built from.

**How (builder's plan, to check against the 26.3 source):**

- Listening uses vanilla's game event system, the way the allay hears note blocks (`GameEvent.NOTE_BLOCK_PLAY`), reading pitch from the note block's `NOTE` state at the event position. A plain listener rather than a vibration listener, so distance doesn't add a travel delay that would bend the timing.
- Dancing uses `GameEvent.JUKEBOX_PLAY` / `JUKEBOX_STOP_PLAY` like the allay.
- The jukebox spot: a POI search (`specs/vanilla-subsystems.md`), one golem per jukebox like a claimed job site.
- The build pattern: hook the carved pumpkin's golem check, next to vanilla's snow, iron and copper golem patterns.
- Recording stored on the entity as (tick offset, pitch) pairs.

**Open (owner):** the numbers: recording cap (5 min), beat spacing (1 s), wind chime floor (2).

## Open (owner)

- How often a magma spot bubbles and erupts, and the boost numbers (all builder's starting values above).
- Whether the bubbles should be brighter (they're subtle from a medium distance).

## Milestone test objectives

- Magma under lava makes bubbles on the surface nearby; they swell for a few seconds, then pop with the plop sound. The core test covers the bubbling spot; no test yet that bubbles appear only over magma.
- A lava geyser erupts now and then with black smoke and lifts a player standing or gliding over it, with a speed boost. A sulfur geyser gives the same boost. Core tests cover lift and boost, and `VentRhythmGameTest` the rhythm; no test yet for eruptions near players at about the tuned rate, or a gliding player circling a geyser to climb.
- Parasitic (`ParasiticGameTest`, core `parasiticBiteTradesHealthForDurability`): a level III bite takes 0.5-2 health and mends about 27.5 a point; it can't sit with Mending; an undamaged item doesn't bite. Objectives, no test yet: the cooldown holds between bites; a bite can kill; creative players are never bitten.
