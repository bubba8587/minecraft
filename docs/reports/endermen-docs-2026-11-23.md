# Endermen docs report, 2026-11-23 (BACKLOG item 19: builders standing still all night)

For the docs merge into `specs/endermen.md` and the Reworked Mobs player page. Governing leaf: C1.

## What changed (player-facing)

- A builder enderman whose block can't go in yet no longer stands in a ring by the cairn waiting. It sets the block down on the ground near the cairn (2 to 4 blocks outside the cairn's footprint, never on the cairn or within 3 blocks of the trade pad) and goes on with other work.
- Endermen pick those set-down blocks up once the cairn has a spot they fit, and carry them in. A set-down block that doesn't fit yet is left lying (no endless pick-up, put-down).
- Endermen standing where a block has to go now step aside (teleport a few blocks off) so the block can go in.
- Cairns with diagonal "arms" (blocks that only touch the rest of the cairn at an edge) can now be finished. Before, those blocks could never be placed, so their builders waited forever: this was likely the main cause of the all-night standing.

## Spec facts (for `specs/endermen.md`, "Two jobs" and tuning)

- The WAIT job is gone, and with it `WAIT_TICKS`. New job `SET_DOWN` (counts as work, so eye contact doesn't anger it while doing it).
- Ring places are now only used for the vigil around a complete cairn (`ringPlace` renamed back to `vigilPlace`). I kept the vigil: the owner's "ring places can go" read as the waiting ring, and the vigil is a separate owner feature. Flag for the owner if he meant the vigil too.
- Set-down spot: a random angle, at `radius + SET_DOWN_GAP_MIN..MAX` (2..4) from the origin, on the ground (heightmap top), within 4 blocks of the cairn's height, air with a sturdy floor, the block can survive there, the target world says air there, and it isn't a design block of any cairn or resting on one. 12 tries per plan; if none fits it tries again next look round.
- Take side: a block a nearby cairn wants, lying where the target world is air (above the original ground), is taken only when some unwarded cairn within `SITE_WORK_RADIUS` has a placeable spot for it (`EndermanWork.readyCairnBlock`). This works for non-liftable cairn blocks too (end rods, slabs, glass), which the old rules would never pick up.
- Pickup scan now reads one block higher per column (from the motion-blocking heightmap, not one below it), so blocks that don't block motion on top of the ground (end rods, flowers) are seen. Cost: one extra block read per column.
- `CairnSite.canPlace`: a design block can go in when any face neighbour or any of its 12 edge neighbours is solid (was faces only).

## Test

- New milestone server game test `EndermanGameTest.outOfOrderBuildersFinishHalfBuiltCairn` (environment `enderman_relay`, night): a 93-block cairn (seed 1234, radius 8) built up to one block above its origin, 49 builders arriving every 2 seconds carrying the missing blocks top first. It checks the cairn gets finished, that some builders set blocks down, and that no enderman holds a block with no job for more than 100 ticks.
- Result: finished at about tick 2200 (under 2 in-game minutes, well within one night), about 30 to 36 set-downs, longest idle carry 15 ticks.
- The test turns the cairn spawner rule off and force-loads the chunks round the field while it runs (endermen teleporting outside the test area froze otherwise), and puts back both afterwards.
- Milestone test objective line for the spec: "Builders arriving out of order at a half-built cairn set blocks down and others bring them in; the cairn finishes within a night with no builder idle."

## Numbers

- `SET_DOWN_GAP_MIN` 2, `SET_DOWN_GAP_MAX` 4 (blocks outside the footprint); trade pad keep-clear 3 blocks; set-down tries per plan 12.
- Full milestone server suite for Reworked Mobs: 134 tests, all passed.
