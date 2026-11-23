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

## Open (owner)

- How often a magma spot bubbles and erupts, and the boost numbers (all builder's starting values above).

## Milestone test objectives

- Magma under lava makes bubbles on the surface nearby. They pop after about a second with the plop sound. (Core test covers the bubbling spot; no test yet that bubbles appear only over magma.)
- A lava geyser erupts now and then with black smoke and lifts a player standing or gliding over it, with a speed boost. A sulfur geyser gives the same boost. (Core tests cover lift and boost; no test yet for eruptions near players at about the tuned rate, or a gliding player circling a geyser to climb.)
