<!-- dte:C8 -->
# Lava bubble art redo: report (builder: agent 3)

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

## Slower and sparser (owner: "much too fast and frequent. lava bubbles should be slow and sparse")

| What | Was | Now |
|---|---|---|
| `BUBBLE_CHANCE` per lava display tick over magma | 0.18 (about 12 bubbles a second over a 9x9 pool) | 0.006 (about one every 2 s over the same pool) |
| Swell `BUBBLE_SWELL_TICKS` + `BUBBLE_LIFE_SPREAD` | 16 + 0-8 ticks (about 1 s) | 50 + 0-30 ticks (2.5-4 s) |
| Pop `BUBBLE_POP_TICKS` | 5 | 6 |

The client shots spawn their own bubbles, so they don't show the rate; the owner judges that in play. Core 4/4.
