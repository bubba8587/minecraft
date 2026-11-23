# Camp kinds report (2026-11-24, C1)

## Bug
Owner playtest: natural test world signs for husk, stray and bogged camps led to plain zombie/skeleton camps.

## Root cause
Not the kind rule. `tools/build-naturalworld.py` found a biome of each kind (desert, snowy plains, swamp) and then ran
vanilla `/locate structure bubbas_reworked_mobs:monster_camp` from that point. Vanilla locate returns the nearest camp of
*any* kind, usually outside that biome (desert/snow/swamp camps are ~1700-2200 blocks apart, ordinary camps ~350), so
the sign's label was a guess. The camp's kind itself is decided correctly from the biome at its centre
(`CampStructure` -> `CampKind.forBiome`), stored in the piece, and registration reads it from the piece.

## Fix
- New command `/bubbas_reworked_mobs camp locate <kind>`: walks the camp structure set's spread grid ring by ring from
  where it runs (up to 6144 blocks), reads each start chunk's structure start (`STRUCTURE_STARTS` only, cheap) and returns
  the nearest camp of that kind: "Nearest <kind> camp at x y z, N blocks away." Code: `CampCommand.nearest`.
- Natural world builder: one sign per kind (skeleton, zombie, husk, stray, bogged), each from `camp locate <kind>`. The
  sign teleports 3 blocks south of the fire (not onto it). After loading, the builder re-runs `camp locate <kind>` from each camp's own centre (its chunks now fully generated; `camp list` stays empty headless, since registration needs a nearby player)
  and fails the build unless it returns that same camp; it logs "camp sign <kind>: camp at x z confirmed <kind>". Seed 20261122: skeleton 712 264, zombie 920 56, husk -1240 280, stray 232 2536, bogged -1704 1032, all confirmed.
- Spec facts for specs/monster-camps.md: add `camp locate <kind>` to the command line; add the milestone test below.
- specs/testworld.md "Natural world": camp signs are one per kind, located by kind, verified against registration.

## Tests
- Milestone `CampsGameTest.campKindFollowsTheBiome`: desert and the three badlands -> husk; swamp, mangrove -> bogged;
  snowy plains, snowy taiga, ice spikes, grove, snowy slopes, jagged and frozen peaks -> stray; plains, forest, taiga,
  savanna, windswept hills, stony peaks, cherry grove -> skeleton or zombie (8 rolls each).
- `./gradlew :bubbas_reworked_mobs:build -PmilestoneTests`: 142/142.

## Player-facing
Natural test world: every camp sign now leads to a camp of the kind it names. (`camp locate` is an operator command.)
