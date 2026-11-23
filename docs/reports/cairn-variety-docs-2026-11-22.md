<!-- dte:C1 -->
# Cairn variety: docs report (builder, 2026-11-22)

For merging into `specs/endermen.md` ("Cairn designs", Numbers, tests) and the player page `Endermen.md`, then deletion. Branch `cairn-variety`. Owner: "sure add as much variety as you like" (no banners).

## Player-facing

Enderman cairns now come in seven forms, each fixed by the cairn's seed:
- **Chorus:** the twisting purpur spire of 1-3 trunks (the first design).
- **Grove:** a thicket of 3-5 short twisting trunks on a wide footing.
- **Spire:** one tall straight column with tiers of short arms tipped with end rods, on a stepped footing.
- **Arch:** two columns joined across the top like a gateway, sometimes with glass hanging under the span, the tallest rod on a short stalk above the middle.
- **Canopy:** a thick trunk opening into a crown of branches with rods, like a purpur tree.
- **Henge:** a ring of end stone posts capped with purpur round a small spire.
- **Leaning:** a tall stalk leaning far to one side, with a shorter one leaning the other way.

Footings are round, square, cross-shaped or diamond-shaped, 1-3 blocks out, sometimes with purpur set in. Glass is magenta, purple or pink. Offerings lie in a ring, a cluster to one side, or a line leading away. Every cairn's tallest end rod still lies on its side pointing toward the nearest stronghold; cairns still use only End blocks; offerings still come from the biome.

## How it's built (spec facts)

- `enderman/CairnDesign`: `Form` enum (CHORUS, GROVE, SPIRE, ARCH, CANOPY, HENGE, LEANING), picked by the design's first random roll with cumulative weights 26/14/14/12/12/11/11 %. `formOf(seed)` for tests and previews. Still a pure function of (seed, offerings, pointer, groundAt).
- New structural blocks (none generate in the overworld; `TerrainKinds.isNatural` false): end stone brick stairs (spire footing), end stone brick walls (henge posts), purple and pink stained glass besides magenta. Purpur slabs are now also branches (canopy) and post caps (henge).
- `growTrunk` takes `wander` (how far a trunk may step from its start: 1 grove and henge, 2 chorus, 4 leaning) and an optional `lean` direction (tried first 70 % of the time).
- `placePointer(top, pointer)` is shared by every form: the rod on its side beside the top block facing the stronghold, else on top. Afterwards every rod at or above the pointer's height is removed, as before.
- `reach` (the structure's farthest block) sets how far out offerings go; if no offering spot is free, one is placed just beyond the reach on the first free side.
- Untouched: CairnSites (placement), CairnSite (growth, complete = matches the design), offerings by biome, the TNT chance, the worldgen stub (natural offerings only), and the offering gift (by site seed, `OFFERING_SHARE` 0.15 on develop, saved per site, unaffected by the design).
- Hand-placed sites in saved worlds (`cairn here`) keep their seed but are redesigned with the new forms. Endermen treat any blocks of the old design that the new one doesn't want as stray (slowly taken away, unnatural at 5 %).

## Numbers (2000 designs, `EndermanGameTest.cairnDesignsAreCheapAndBounded`)

| Form | Share | End blocks, average | At most |
|---|---|---|---|
| Chorus | 25 % | 41 | 78 |
| Grove | 14 % | 60 | 99 |
| Spire | 15 % | 50 | 72 |
| Arch | 11 % | 56 | 88 |
| Canopy | 12 % | 82 | 106 |
| Henge | 11 % | 49 | 71 |
| Leaning | 12 % | 42 | 67 |

0.026 ms a design (worked out once per site, off the main thread). The spawn check's pass over the design grows with the block count (about 40-110 reads every 5 s per active site, against about 50 before): negligible.

## Builder's choices (owner may change)

1. **Seven forms with these shares.** Chorus stays the most common.
2. **Some cairns are bigger** (canopy about 82 End blocks on average, at most 106; the first design ran 30-60), so they take longer to finish: by the pace model (`tools/cairn-pace.py`, ~0.5 nights a block late on) a canopy takes roughly 40 nights against 14-30 before. Say if building time should be capped (e.g. shorter canopies).
3. **Hand-placed test-world cairns change shape** (same seed, new design).
4. **Offering lines** point in a random direction, not toward the stronghold (the rod is the only pointer, as the spec says).

## Tests

- Core `endermanCairnDesignsVaryAndUseOnlyEndBlocks` (updated): the allowed End blocks now include end stone brick stairs and walls and purple and pink glass; every form turns up in 400 seeds; all earlier checks (determinism, varied heights and sizes, multi-trunk share, only unnatural structural blocks, never obsidian, offerings and rods in every cairn, the tallest rod is the pointer facing the stronghold and the only one at that height, rare TNT) pass.
- Milestone `cairnDesignsAreCheapAndBounded` (new): 2000 designs average under 1 ms and none exceeds 120 End blocks.
- `:bubbas_reworked_mobs:build` passes (30 core); EndermanGameTest and EndermanTotemsGameTest milestone pass.
- Client part `cairnforms` (`CairnFormsClientGameTest`): one finished cairn of each form plus three more seeds on a flat world, from the north-west: `cairn-form-<form>-<seed>` and `cairn-forms-row`, and it prints each one's block counts. I looked at all seven forms and the extra arch.
