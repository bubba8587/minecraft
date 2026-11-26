<!-- dte:C8,B15,B17 -->
# Bubba's World Tweaks (spec)

**Status: built.** Player pages: `player-docs/Projects/Bubba's World Tweaks/`.

Mod id `bubbas_world_tweaks`. A small mod for world and environment features that aren't mobs, recipes or tools (owner: "i'm sure we'll have others").

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
## Deep-sea fortresses (owner's idea, 2026-11-25; to build)

Flooded fortresses on the floor of deep oceans: a Nether fortress's layout, built mostly of stone bricks with prismarine worked in, guarded by drowned, the occasional guardian and a guardian spawner, with barrels instead of chests (owner). *(default)* marks the agent's picks for what the owner left open.

- **Layout (owner):** vanilla's Nether fortress generator itself (`NetherFortressPieces`, code-built pieces, not templates), run with a block swap while our structure's pieces place (no copied vanilla code). Full vanilla fortress size and piece mix *(default)*.
- **Blocks:** nether bricks → stone bricks, with mossy and cracked stone bricks mixed in by position (stable per block). **Prismarine is placed intentionally, not at random (owner):** as trim on chosen features of each piece (e.g. floor borders, arch and doorway frames, pillar caps, window sills, the spawner room) *(default which features)*. **Lights (owner):** sea lanterns (and the like) set in on purpose, e.g. in ceilings and pillar tops at regular spacing *(default placement)*; nether brick stairs → stone brick stairs; nether brick fences → prismarine walls, waterlogged *(default)*; the lava well → a magma block (a bubble column) *(default)*; soul sand and nether wart (the stair-room garden) → sand with sea pickles *(default)*. No new blocks (B15).
- **Flooded (owner):** every block the generator would set to air inside the fortress is water; anything that can be waterlogged is.
- **Barrels (owner):** chests become barrels (facing out of their wall) with our own loot table `bubbas_world_tweaks:chests/deepsea_fortress`: **ocean loot, not the Nether-flavoured fortress table (owner):** drawn from vanilla's shipwreck (treasure, supply, map), buried treasure and ocean-monument rewards (gold blocks, prismarine, sponges), plus nautilus shells and a rare Heart of the Sea *(default contents and weights; builder proposes numbers)*.
- **Spawner (owner):** the blaze spawner becomes a **guardian spawner** ("there's not really a lot of available hostile mobs in the sea").
- **Spawns (owner):** inside the fortress (its pieces' boxes, like vanilla's fortress spawn override) monsters are mostly **drowned** with the **occasional guardian** *(default weights, e.g. drowned 10 : guardian 1)*.
- **Where (owner):** deep ocean biomes only (deep ocean, deep lukewarm, deep cold, deep frozen), rare: about as rare as ocean monuments *(default: a structure set spaced like monuments)*. Seated on the sea floor, its base fortified into the floor (the generator's pillar fill reaches down to solid ground), and kept below sea level.
- **Tectonic (owner, 2026-11-25: "compat with Tectonic, which can offer deeper seas"):** placement reads the real ocean-floor height from the chunk generator (no assumed vanilla depths), so on Tectonic's deeper floors the fortress sits deeper and its pillars reach the floor; it stays below sea level. Tectonic keeps vanilla's biomes, so the deep-ocean biome tag still applies. Verified on a headless server with Tectonic (locate and look at real placements), not only in flat game-test worlds.
- **Round 2 (owner picked, 2026-11-25; build after the first version merges):**
  - **Conduit shrine:** in the central courtyard (the fortress's big crossing) a prismarine conduit frame with an empty heart. A player who places a conduit there activates it (vanilla conduit: Conduit Power round it, and it attacks hostile mobs in range, so drowned near it take damage). The Heart of the Sea in the loot is the way to make one. Frame size and spot *(default)*: the smallest frame that activates (16 blocks), centred over the crossing.
  - **Bubble lifts:** the tall stair shafts become bubble columns: soul sand at the foot of up-shafts, magma at the top of down-shafts *(default which shafts)*, so the levels connect quickly. Vanilla bubble columns.
  - **Dolphins lead to it:** add the structure to vanilla's `#minecraft:dolphin_located` structure tag, so a dolphin fed fish can guide a player to the nearest one.
  - **Treasure maps point to it:** shipwreck map chests sometimes hold an explorer map to the nearest deep-sea fortress instead of a buried treasure map *(default odds, e.g. 1 in 4 of the maps)*.
- `/locate structure bubbas_world_tweaks:deepsea_fortress` works like any structure. Test world: a station or a natural-world spot *(default)*.
## Open (owner)

- How often a magma spot bubbles and erupts, and the boost numbers (all builder's starting values above).
- Whether the bubbles should be brighter (they're subtle from a medium distance).

## Milestone test objectives

- Magma under lava makes bubbles on the surface nearby; they swell for a few seconds, then pop with the plop sound. The core test covers the bubbling spot; no test yet that bubbles appear only over magma.
- A lava geyser erupts now and then with black smoke and lifts a player standing or gliding over it, with a speed boost. A sulfur geyser gives the same boost. Core tests cover lift and boost, and `VentRhythmGameTest` the rhythm; no test yet for eruptions near players at about the tuned rate, or a gliding player circling a geyser to climb.
- Parasitic (`ParasiticGameTest`, core `parasiticBiteTradesHealthForDurability`): a level III bite takes 0.5-2 health and mends about 27.5 a point; it can't sit with Mending; an undamaged item doesn't bite. Objectives, no test yet: the cooldown holds between bites; a bite can kill; creative players are never bitten.
