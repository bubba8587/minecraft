<!-- dte:C8,B15,B17 -->
# Bubba's World Tweaks (spec)

**Status: built (merged 2026-11-22).** Player pages: `player-docs/Projects/Bubba's World Tweaks/`.

Mod id `bubbas_world_tweaks`. A small mod for world and environment features that aren't mobs, recipes or tools (owner: "i'm sure we'll have others").

## Lava bubbles and lava geysers (owner, 2026-11-22)

- **Lava bubbles:** a **magma block under lava** makes bubbles on the lava surface nearby, the lava version of magma making bubble columns in water (B17: reuse that idea).
  - Each bubble is a **little half-dome on the surface** that lasts **about a second**, then pops with a **goopy "plop"**. A custom particle or model, in crisp pixel art with a vanilla palette, lit like lava. The sound is new, or vanilla's lava pop pitched down.
  - No new blocks (B15): the magma block and the lava are vanilla.
- **Lava geysers:** now and then a bubbling spot releases **a stream of black smoke**, reusing vanilla's **sulfur geyser** look (26.3's `PotentSulfurBlockEntity` eruption and its geyser particles, recoloured black or smoky).
- **Lift:** **sulfur geysers and lava geysers both push players up and give a little speed boost.** Vanilla sulfur geysers already launch entities upward (`GEYSER_BASE_LAUNCH_SPEED` 0.3, `GEYSER_LAUNCH_FORCE` 0.2); this mod adds the speed boost to both and the same launch to lava geysers. It's made for the Glider (Bubba's Extra Recipes): circle a geyser to climb, so the Glider works for Nether travel. It must not stop working without the Glider installed.
- Prior art: Lava Particles | More (client-only boiling bubbles, LGPL, no 26.3 build, no geysers or lift). Nothing does lift.
- **Worldgen: none needed** (owner, 2026-11-22). Vanilla's `ore_magma` already places magma blobs at y 27-36 in the Nether (4 per chunk), around the lava sea surface at y 31, so bubbles and geysers appear naturally along lava sea shores and shallows.

## As built (2026-11-22, agent 3)

**(builder)** marks the builder's choices. Numbers are in `WorldTweaksTuning`.

- **Lava bubbles:** where a magma block sits under lava (up to 4 deep), the surface above it bubbles.
  - Each bubble is a round lava blister that swells for about a second, then bursts into a ring of droplets with a goopy plop: vanilla's lava pop at half pitch, volume 0.55 **(builder)**.
  - Pixel art in vanilla lava's palette, with a cooled dark crust rim and a hot glint, lit like lava. 7 frames (`lava_bubble`, `client/LavaBubbleParticle`), 16x and 32x, made by `tools-local/make_lava_bubble_textures.py`.
  - The particle faces the camera and is centred on the surface, so the lava hides its lower half from the side (a half-dome), and from above it reads as a round blister **(builder: a dome turned only round the vertical axis looked squashed from above)**.
  - Display only: `mixin/LavaFluidBubbleMixin` at the tail of `LavaFluid.animateTick` (client display tick), 18 % per display tick over magma. Nothing runs on the server.
- **Lava geysers:** every 40 ticks each player samples 6 columns within 24 blocks (y +8 to -16), and a bubbling spot erupts with an 8 % chance. An eruption lasts 100 ticks, with none within 8 blocks of another.
  - It throws a column of black smoke (`lava_geyser_plume`, `client/LavaGeyserPlumeParticle`: vanilla's sulfur geyser plume motion and sprites, tinted to soot, plus large smoke), lava sparks and a low rumble.
  - `lava/LavaGeyser` is an invisible, never-saved marker entity at the surface; the client draws its effects (`LavaGeyserEffects`). `lava/LavaGeysers` holds the entity type and the per-player sampling on `ServerTickEvents.END_LEVEL_TICK`.
- **Lift:** over an erupting lava geyser, players are pushed up the way vanilla's sulfur geyser pushes: a column 14 high and half-width 0.8, +0.2 a tick while rising slower than 0.9.
- **Boost:** over an erupting lava **or sulfur** geyser (sulfur within 16 blocks below), horizontal speed grows 6 % a tick, up to 1.4 blocks a tick. Circle a geyser to climb and gain speed. It works on foot, falling, with an elytra or the Glider, and nothing needs the Glider. Not for spectators or creative fliers (like vanilla).
  - `lava/GeyserLift` runs from a `Player.tick` mixin on both sides, because a player's own movement is decided on their client (vanilla's geyser launch works the same way). The sulfur boost reads the erupting `PotentSulfurBlock.STATE` under the player instead of hooking vanilla's lambda ticker.
- **Tests:** core `magmaUnderLavaIsABubblingSpot`, `lavaGeyserLiftsAndBoosts` (nothing without an eruption; lift and boost with one; no second geyser on the same spot), `sulfurGeyserBoosts` (boost, no extra lift). Client: `lava-bubbles`, `lava-bubbles-close`, `lava-geyser`, each at 16x and 32x (the bubble scene fills a whole pool with magma, so it's denser than a natural lava sea).
- The mod icon is not made yet (B14: person-made); it's on `docs/art-todo.md`.

### Art redo and slower bubbles (2026-11-22, agent 3, owner feedback)

Branch `lava-bubbles-art`, off develop. This was owner feedback: the bubbles looked far off from lava's texture and had outlines they didn't need.

- **Bubbles:** redrawn as a swelling of the lava surface.
  - Every pixel comes from vanilla lava's own colour ramp: `lava_still.png` for 16x and `lava_flow.png` for 32x, picked by how often each colour appears.
  - Shading is brightest on top, with a small soft highlight at lava's brightest yellow, and slightly darker toward the base.
  - The lava texture's own grain is mixed in. There's no outline and no dark crust.
  - Burst frames are droplets in lava's bright yellows.
  - Still a crisp pixel-art half-dome, lit full bright; pop timing unchanged.
- **32x:** the same method, on the 32px lava ramp and grain. A second 32x variant (less grain, brighter top) was tried and dropped: the owner preferred the first.
- **Smoke:** the geyser's black smoke was off the same way. Vanilla's plume sprites have three clear shades (193/210/224), but we multiplied them nearly to black, which flattened them into featureless shapes.
  - Now: our own `lava_geyser_plume_01..08`, which are vanilla's sprites with the three shades recoloured to soot (36/52/70), drawn untinted and lit by the world like vanilla's. 32x adds an in-between shade at edges.
  - Next to a vanilla geyser it reads as the same kind of puffy, shaded plume, in soot.
- **Client shots** (`runClientGameTest -PmilestoneTests`), each at 16x and 32x, all looked at:
  - `lava-bubble-close`, `lava-bubble-side` (half-domes at surface level), `lava-bubbles-patch` (medium);
  - `lava-geyser-vs-sulfur`: our geyser beside vanilla's geyser particle over a water pit. A `potent_sulfur[state=continuous]` block alone didn't erupt in the test, so the shot spawns vanilla's `GEYSER` particle there.
- **Rows in `docs/art-todo.md` updated:** the bubble row now describes the new method, and a soot plume row was added. texcheck OK.
- **Still open:** the bubbles now blend in so well that from a medium distance they're subtle. That's by design, per the feedback; it's easy to brighten if the owner wants them more visible.

#### Slower and sparser (owner: "much too fast and frequent. lava bubbles should be slow and sparse")

| What | Was | Now |
|---|---|---|
| `BUBBLE_CHANCE` per lava display tick over magma | 0.18 (about 12 bubbles a second over a 9x9 pool) | 0.006 (about one every 2 s over the same pool) |
| Swell `BUBBLE_SWELL_TICKS` + `BUBBLE_LIFE_SPREAD` | 16 + 0-8 ticks (about 1 s) | 50 + 0-30 ticks (2.5-4 s) |
| Pop `BUBBLE_POP_TICKS` | 5 | 6 |

The client shots spawn their own bubbles, so they don't show the rate; the owner judges that in play. Core 4/4.

## Open (owner)

- How often a magma spot bubbles and erupts, and the boost numbers (all builder's starting values above).

## Milestone test objectives

- Magma under lava makes bubbles on the surface nearby. They pop after about a second with the plop sound. (Core test covers the bubbling spot; no test yet that bubbles appear only over magma.)
- A lava geyser erupts now and then with black smoke and lifts a player standing or gliding over it, with a speed boost. A sulfur geyser gives the same boost. (Core tests cover lift and boost; no test yet for eruptions near players at about the tuned rate, or a gliding player circling a geyser to climb.)
