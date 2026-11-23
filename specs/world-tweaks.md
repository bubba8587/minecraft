<!-- dte:C8,B15,B17 -->
# Bubba's World Tweaks (spec)

**Status (2026-11-22): built on the `world-tweaks` branch, awaiting merge into `develop`.** Nothing of it is in `develop` yet, so it has no player pages. Its builder's report (`docs/reports/world-tweaks-report-2026-11-22.md` on that branch) is merged into this spec after the branch merges.

Mod id `bubbas_world_tweaks`. A small mod for world and environment features that aren't mobs, recipes or tools (owner: "i'm sure we'll have others").

## Lava bubbles and lava geysers (owner, 2026-11-22)

- **Lava bubbles:** a **magma block under lava** makes bubbles on the lava surface nearby, the lava version of magma making bubble columns in water (B17: reuse that idea).
  - Each bubble is a **little half-dome on the surface** that lasts **about a second**, then pops with a **goopy "plop"**. A custom particle or model, in crisp pixel art with a vanilla palette, lit like lava. The sound is new, or vanilla's lava pop pitched down.
  - No new blocks (B15): the magma block and the lava are vanilla.
- **Lava geysers:** now and then a bubbling spot releases **a stream of black smoke**, reusing vanilla's **sulfur geyser** look (26.3's `PotentSulfurBlockEntity` eruption and its geyser particles, recoloured black or smoky).
- **Lift:** **sulfur geysers and lava geysers both push players up and give a little speed boost.** Vanilla sulfur geysers already launch entities upward (`GEYSER_BASE_LAUNCH_SPEED` 0.3, `GEYSER_LAUNCH_FORCE` 0.2); this mod adds the speed boost to both and the same launch to lava geysers. It's made for the Glider (Bubba's Extra Recipes): circle a geyser to climb, so the Glider works for Nether travel. It must not stop working without the Glider installed.
- Prior art: Lava Particles | More (client-only boiling bubbles, LGPL, no 26.3 build, no geysers or lift). Nothing does lift.
- **Worldgen: none needed** (owner, 2026-11-22). Vanilla's `ore_magma` already places magma blobs at y 27-36 in the Nether (4 per chunk), around the lava sea surface at y 31, so bubbles and geysers appear naturally along lava sea shores and shallows.

## Open (owner)

- How often a magma spot bubbles and erupts, and the boost numbers (first values are the builder's, on the branch).

## Milestone test objectives

- Magma under lava makes bubbles on the surface nearby. They pop after about a second with the plop sound.
- A lava geyser erupts now and then with black smoke and lifts a player standing or gliding over it, with a speed boost. A sulfur geyser gives the same boost.
