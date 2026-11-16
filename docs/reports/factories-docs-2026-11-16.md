<!-- dte:C1,B15 -->
# Docs report: Reworked Mobs mob factories, look only (2026-11-16)

Builder's report for merging into `specs/mob-factory.md` (and `specs/testworld.md`, `docs/art-todo.md` already has its row). Phase 1 only, per the owner: **the look of the whole factory, no mechanics**. Nothing here is in the live spec yet.

## Spec facts (as built)

**Code-built, deterministic.** `factory/FactoryDesign.design(seed, mob)` is a pure function of a seed and the factory's mob: about 54,000 block placements in local coordinates (origin = first air above the middle of the platform; +z the entrance, -z the face wall, +x the Warden's tower), rotatable in quarter turns. Ready for worldgen to place chunk by chunk later (not wired yet). Vanilla blocks only (B15); no new blocks, items or entities.

**Parts** (all numbers are local blocks):
- *Cavern:* a squashed dome, radius 21 with a 3-wave wobble, floor 8 below the deck (rising at the walls), ceiling 22 above the deck in the middle, 13 at the edge. A 2-block shell of deepslate (cobbled deepslate 12 %, tuff 6 %) seals it, so caves and fluids don't break in; rock faces touching the air are **sculk** (floor: about 80 %, walls and ceiling: patches), with **sculk veins** creeping over 16 % of the open cells next to rock. A winding entrance tunnel climbs out the front (+z), 5 wide, about 40 blocks long.
- *Ceiling growth (the upper section):* a 4-6 block **mass** over the middle (radius about 15.5): mostly basalt, polished basalt and bone, warped wart spreading in patches (with shroomlight knots), a little nether wart. From it hang **60-70 spikes** like dripstone made of bone: a bone core banded by basalt rings every 4 blocks (vertebrae), warped wart draped over the upper part, bone at the tip, and a **pointed dripstone point** (1-4 long, shaped like vanilla's base/middle/frustum/tip). Spikes are densest and longest over the **drop point** (the teleporter pad, z = 3): up to about 15 long, tips never closer than 4 above the deck; short (3-5) over the face wall and in the Warden's line of sight. Bigger ones carry a shroomlight knot near the root. **40 thin strands** (single bone-and-basalt drips with dripstone points) hang between them, and **weeping vines** hang off spike shoulders.
- *Chains (owner, 2026-11-16):* 9-11 **alien chains/spurs** of warped wart (bits of nether wart and shroomlight) with **bone vertebra rings** every few blocks and **bone thorns tipped with dripstone** sticking out and down. Each starts low on the cavern wall, climbs it, runs along the ceiling twisting (so they spiral in), and **curls down to a point hanging over the drop point**. None start on the face wall, the tower or the tunnel mouth.
- *The hidden spawner:* an empty (inert) vanilla `spawner`, buried 2 blocks up inside the mass over the biggest spike, wrapped in bone on all six sides. The build command prints where it is.
- *Platform (ancient city + redstone):* 19 x 19 deck of deepslate tiles (cracked 18 %, chiseled every 4th), polished deepslate border and underlayer, sculk spreading over a third of it; upside-down tile-stair corbels under the edge; 2 x 2 deepslate-brick pillars to the floor with an **observer** staring out of each outer pillar; low tile walls along the sides and front (gaps for the entrance and the tower stairs); corner posts with **soul lanterns**; six **lit redstone lamps** set flush in the deck (each on a redstone block); gray candles; two **sculk shriekers** (can_summon false) by the face wall, sensors and a calibrated sensor.
- *Teleporter pad (at the drop point):* a **lodestone** ringed by **crying obsidian**, then the **timer ring**: eight redstone lamps going round clockwise from the back, with reinforced deepslate between. The owner wants a redstone-visible activation timer that is **lamps lighting up one by one, not a redstone clock**: the look shows it mid-count (five of eight lit). Under the soul-fire pit below the deck, soul fire smoulders on soul soil.
- *Paired beacon halves:* the **floor beacon** stands on a 3 x 3 **iron** inlay framed by polished blackstone bricks (gray candles at the corners), on the deck's left (x -6, z -1; off the face wall's sightline). Straight above, set into the growth, the **ceiling beacon** with its 3 x 3 iron pyramid above it, ringed by crying obsidian and polished basalt. The column between is kept clear. With Bubba's Reworked Beacons installed, the ceiling beacon is placed **upside down** (its `upside_down` state, looked up by name: no compile or runtime link) and the pair shows its beam (seen in the screenshots); without it, both are plain vanilla beacons with no beam.
- *Face wall:* behind the deck, a 3-block-thick deepslate brick wall to the ceiling; the mob's face, **16 x 16 blocks (its 8 x 8 face texture at 2 blocks a pixel)**, in a polished deepslate frame with chiseled corners, **lit by redstone lamps set in the frame** (4 along the top, 3 down each side, powered from behind), a tile-slab shelf under it and soul lanterns on chains either side. Faces come from `tools/factory-faces.py`: it reads the vanilla mob texture, posterizes it into 2-4 tones (k-means, contrast stretched), maps each tone to the nearest flat-looking vanilla block (concrete, terracotta, polished stones; no bricks, whose seams fight the pixels), and gives the glowing eyes one glowing block. Output: `factory/FactoryFaces.java` (generated). Mobs: **zombie** (moss/green concrete/cut copper, black eyes), **skeleton** (calcite/polished diorite, polished deepslate eyes and mouth), **creeper** (lime/oxidized cut copper, black face), **spider** (polished deepslate and black, eight **magma block** eyes), **enderman** (black concrete, **crying obsidian** eye bars).
- *Warden's tower (observation area):* a deepslate-brick tower from the floor to the ceiling on the +x side (7 x 9), tile bands; a room inside (5 x 7 x 5) with a sculk floor, a catalyst, a sensor, soul lanterns on chains and a lit lamp in the floor; a **5-wide, 5-tall open window** framed in chiseled deepslate looking over the deck; a ledge on upside-down stairs with tile walls and soul lanterns; stairs down to a walkway beside the deck. The **Warden** stands just inside the window (local 15, 6, 0), looking out over the platform. For now the command spawns it with **NoAI**, persistent (look only).

**Command (ops):** `/bubbas_reworked_mobs factory here <mob> [seed]` builds a factory with the platform's middle at your feet; `factory build <pos> <mob> <seed> <turns>` at a position, turned 0-3 quarter turns. Mobs: zombie, skeleton, creeper, spider, enderman. Not saved as a site yet (mechanics).

**Cost:** working out a design is one pass over a 69 x 48 x 87 grid plus the shell check; placing is ~54,000 `setBlock` calls with no neighbour updates (well under a second on the test machine), then a pass to connect walls.

## Builder's choices (owner may want to hear)

1. **Mob types:** zombie, skeleton, creeper, spider, enderman (the faces read best; witch, husk, stray are easy to add with the tool).
2. **Iron pyramids and two beacons are real loot** (18 iron blocks, 2 beacons = 2 nether stars) for whoever conquers a factory. Could be swapped for something cheaper, or the beacons made to drop nothing, if that's too rich.
3. **The floor beacon sits off to the left of the deck** (not in front of the face), so the beam never blocks the face.
4. **The drop point is the teleporter pad** (deck middle, a little forward): mobs will fall onto the pad and be sent from it, so the long spikes and the chains' ends hang over it.
5. **Reinforced deepslate in the pad ring** (unbreakable, as in ancient cities' portal frame). Taking over doesn't need to break it.
6. **Shriekers can't summon** (one Warden per factory, F8).
7. **Factories sit with the deck at y -38** in the screenshots; real placement depth is for the worldgen step.
8. **Rare blocks used as decoration** that a player could mine: lodestone (1), crying obsidian, redstone blocks (under lit lamps), iron blocks. Fine for a dangerous structure, in my view.

## Owner's notes recorded for the mechanics phase

- The teleporter's **activation timer is lamps lighting up** (the eight-lamp ring), not a redstone clock.
- Its **particles run even with nothing on the pad**, so a player could hop on and ride it up to escape with the right timing.
- **Right before it fires, whatever is on the pad gets a bit of Levitation** (lifted slightly off the pad), then teleports: a deactivated mob, or a player who hopped on, rises a little as the timer completes.

## Left (mechanics, not started on purpose)

Worldgen placement (sites from the seed, the feature that builds a factory chunk by chunk, saved command sites, `factory locate`), dropping deactivated mobs, the teleporter (timer lamps, particles, levitation, sending up to the surface, waking reworked), redstone control, breaking the spawner, the passive Warden (hearing, no digging away), debuffs (payload through Reworked Beacons' API, or a core without it). Server game tests for those (`ReworkedMobsCoreGameTest` smoke tests, `MobFactoryGameTest`) come with the mechanics.

## Player-facing text (draft, for when it ships)

Deep underground, some caverns hold a **mob factory**: a ceiling of bone and warped growth dripping over an ancient platform, a huge block portrait of the monster it makes, a beacon beam joining floor and ceiling, and a Warden watching from its tower window. Sculk covers everything.

## Milestone test objectives

- (client part `factories`, built) Builds five factories (one per mob) under a default world and photographs them: overview (dark and with night vision), side, ceiling, ceiling close, platform, pad with particles, face (per mob), Warden window close and wide, beam, tower, chains. Run with `-PwithBeacons` to see the pair's beam.
- (objective) A design is deterministic: the same seed and mob give the same blocks; the spawner is fully enclosed; the beam column between the beacons is clear; nothing of the growth comes within 4 blocks of the deck.

## Other docs to touch

- `specs/mob-factory.md`: a "Look (as built)" section from the above; status "look built, mechanics not started".
- `specs/testworld.md`: station idea below.
- `CLAUDE.md` client test part list: Reworked Mobs gains `factories` (and `cairnsim` was missing from the `ClientTests` comment; fixed there).
- `docs/knowledge/`: `tools/factory-faces.py` is a reusable "texture to block art" tool.

## Test world station idea

A zombie factory built with `factory build` under the hub (deck at y -38), reached by a ladder shaft with a sign; the tour photographs the overview and the face wall.
