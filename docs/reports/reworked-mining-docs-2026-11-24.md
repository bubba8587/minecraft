# Reworked Mining docs report: fist breaking speed (2026-11-24)

Builder report for the docs agent to merge into `specs/reworked-mining.md` and the player vault. Backlog item "Nerf fist breaking speed" is built and removed from `BACKLOG.md`.

## Player-facing change

- Bare hands mine slower than vanilla, a step below wooden tools.
  - Logs, planks, dirt, sand, gravel, leaves and other soft blocks: about 1.5x slower than vanilla by hand. The first log takes about 4.7 s instead of 3 s; a dirt or sand block about 1.2 s instead of 0.75 s.
  - Stone and deepslate by hand are very slow (they still drop nothing, as in vanilla): stone about 47 s, deepslate about 94 s.
  - Things that break instantly in vanilla (grass, flowers, torches, crops, saplings and so on) still break instantly.
  - Holding a non-tool item (a stick, a block, food) counts as bare hands, as in vanilla.

## Spec facts (for "The curve" / "Numbers (MiningTuning)")

- New row in the Numbers table: **Fist (builder)**: stone 0.16, deepslate 0.16, other 0.65 (Nether blocks use "other"); no wear.
- How the numbers were picked (builder): each fist factor is wood's factor times the wood/stone ratio on that kind of block, so fists sit one step below wood the same way wood sits below stone. Stone: 0.33 x (0.33 / 0.67) ~ 0.16. Other: 0.70 x (0.70 / 0.75) ~ 0.65. Deepslate copies stone (wood's own deepslate row is "can't mine it", so there is no ratio to follow).
- Timings, on the ground, no Haste (vanilla -> now):
  - oak log or planks (hardness 2): 3.0 s -> 4.65 s (60 -> 93 ticks); for comparison our wooden axe takes 2.15 s (vanilla 1.5 s);
  - dirt, sand (0.5): 0.75 s -> 1.2 s; gravel (0.6): 0.9 s -> 1.4 s; leaves (0.2): 0.3 s -> 0.5 s;
  - stone (1.5, not harvestable by hand): 7.5 s -> about 47 s; deepslate (3.0): 15 s -> about 94 s;
  - hardness-0 blocks: instant before and after (vanilla's progress is speed / hardness, so any factor stays instant at hardness 0).
- What counts as a fist (builder): an empty hand or any item without a `minecraft:tool` component. A tool used on a block it isn't meant for (a pickaxe on a log) keeps vanilla's speed 1, so it is a little faster than a fist there. Owner may want that changed.

## How it's built (for "How it's built" > Speed)

- The same `Player.getDestroySpeed` mixin: `MiningCurve.speed` multiplies the held item's speed by `MiningTuning.FIST`'s factor for the block kind when `MiningCurve.isFist(stack)`. Haste, Mining Fatigue, `block_break_speed` (gems), water and air still apply after it.

## Tests

- Core `curveSlowsDeepslateAndEfficiencyMultiplies` now also checks: empty hand on an oak log = `FIST.other()`; a stick on stone = `FIST.stone()`; a wooden axe beats a fist on a log; short grass still breaks instantly by hand (`getDestroyProgress >= 1`).
- Milestone objective to add to the spec: fist factors on deepslate and a Nether block match `MiningTuning.FIST`; Haste and gem `block_break_speed` stack on fists.

## Open for the owner

- Playtest the fist numbers (all builder's): 0.65 on soft blocks, 0.16 on stone and deepslate.
- Poster copy (`specs/copy-strings.md`) says fists are "slower and weaker". Only mining speed changed. For wood and stone pickaxes "weaker" is their faster wear; fists have no durability, so "weaker" could be read as fist attack damage, which is not changed (vanilla 1). Say if fist attack should be nerfed too, or if "weaker" is fine as poster wording.
- Whether a tool on the wrong block (pickaxe on a log) should also count as a fist.
