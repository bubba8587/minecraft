<!-- dte:C8 -->
# Bubba's World Tweaks: report (builder: agent 3)

New mod `bubbas_world_tweaks` on branch `world-tweaks` (worktree ../mc-worldtweaks). Core tests pass (3 new). Client screenshots of bubbles and the geyser at 16x and 32x checked. Numbers are in `WorldTweaksTuning.java`. **(builder)** marks my choices.

## Player-facing

- **Lava bubbles:** where a magma block sits under lava (up to 4 deep), the surface above it bubbles.
  - Each bubble is a round lava blister that swells for about a second, then bursts into a ring of droplets with a goopy "plop": vanilla's lava pop at half pitch. **(builder)**
  - Crisp pixel art in vanilla lava's own palette, with a cooled dark crust rim and a hot glint, lit like lava.
  - The particle faces the camera and is centred on the surface, so the lava hides its lower half from the side (a half-dome), and from above it shows as a whole round blister. **(builder: a dome that only turned round the vertical axis looked squashed from above.)**
  - Display only: driven by vanilla's random lava display ticks, so nothing runs on the server.
- **Lava geysers:** now and then a bubbling spot near a player erupts for 5 s. It throws a column of black smoke (vanilla's sulfur geyser plume, tinted to soot, plus large smoke), lava sparks, and a low rumble.
- **Lift:**
  - Over an erupting lava geyser, players are pushed up the same way vanilla's sulfur geyser pushes, in a column 14 high and about 1.6 wide.
  - Over an erupting lava **or sulfur** geyser, horizontal speed grows 6 % a tick up to 1.4 blocks a tick. Circle a geyser to climb and pick up speed.
  - Made with the Glider in mind, but nothing needs it: it works on foot, falling, with an elytra or the Glider.
  - Not for spectators or creative fliers (like vanilla).
- **No worldgen** (owner): vanilla's Nether magma blobs along the lava sea do it.

## Numbers (WorldTweaksTuning, all builder)

| What | Value |
|---|---|
| Magma depth under the surface | 4 |
| Bubble chance per lava display tick (over magma) | 18 % |
| Bubble life | swells 16-24 ticks, bursts over 5 |
| Plop | lava pop, pitch 0.5, volume 0.55 |
| Geyser check | each player every 40 ticks samples 6 columns within 24 blocks (y +8 to -16); a bubbling spot erupts with 8 % chance |
| Eruption | 100 ticks; none within 8 blocks of another |
| Lift column | 14 high, half-width 0.8; push +0.2/tick while rising slower than 0.9 |
| Boost | x1.06 horizontal per tick, cap 1.4; sulfur geysers within 16 below the player |

## How it's built

- `lava/LavaGeyser`: an invisible, never-saved marker entity at the surface for the eruption. The client draws its effects (`LavaGeyserEffects`).
- `lava/LavaGeysers`: the entity type, and the per-player sampling on `ServerTickEvents.END_LEVEL_TICK`.
- `lava/GeyserLift`: applied from a `Player.tick` mixin on both sides, because a player's own movement is decided on their client (vanilla's geyser launch works the same way).
  - The sulfur boost reads the erupting `PotentSulfurBlock.STATE` under the player. That avoids hooking vanilla's lambda ticker.
- `mixin/LavaFluidBubbleMixin`: tail of `LavaFluid.animateTick` (client display tick).
- Particles: `lava_bubble` (`client/LavaBubbleParticle`, 7 frames) and `lava_geyser_plume` (`client/LavaGeyserPlumeParticle`, vanilla's plume motion and sprites, soot tint).
- Textures: `tools-local/make_lava_bubble_textures.py` (16x and 32x); texcheck OK. Art-todo row added.

## Tests

- **Core:**
  - `magmaUnderLavaIsABubblingSpot`
  - `lavaGeyserLiftsAndBoosts` (nothing without an eruption; lift and boost with one; no second geyser on the same spot)
  - `sulfurGeyserBoosts` (boost, no extra lift)
- **Client (`runClientGameTest`):**
  - lava-bubbles, lava-bubbles-close, lava-geyser, each at 16x and 32x. All looked at.
  - The bubbles read as crusted half-dome blisters bursting into droplets; the geyser is a tall black smoke column.
- **Milestone objectives (no test yet):**
  - Bubbles appear only over magma.
  - Eruptions happen near players at about the tuned rate.
  - A gliding player circling a geyser climbs.

## Notes

- The mod icon is not made yet (B14: person-made). It's on the art-todo list.
