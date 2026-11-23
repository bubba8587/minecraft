# Monster camps: footprint plan (BACKLOG Playtest fixes item 20) — docs report, 2026-11-23

For merging into `specs/monster-camps.md` and the Reworked Mobs player page. Fixes the overlapping pieces from the variety change (8956912); the variety ranges are kept.

## Player-facing
- Camp pieces no longer draw over each other: towers, rock-and-bone outcrops, skull pikes, lean-tos and banners never sit on the stair tunnel, the fire or each other. A piece with no free spot is left out (so a camp can have fewer towers/outcrops/pikes than it rolled).

## Spec facts (CampPiece.Plan)
- Planning order: room and stair side first (no longer chosen to avoid the first tower), then reserved footprints: the fire (centre ±2: ring, seats, chest) and the stair tunnel where it opens to the surface (from step 3 of the tunnel to its cleared mouth, step 9, plus a wall column either side).
- Then pieces in order lean-tos, towers, outcrops, pikes, banners. Each claims a rectangle of ground cells: tower 6x6 (4x4 plus ladder ring), lean-to 3x3, outcrop (2m+3)², pike and banner 1 cell.
- A claim needs a gap of at least 1 cell (MARGIN) from every earlier footprint; a lean-to may sit right against the fire's footprint (it opens onto the seats, as before). Each piece tries 16 spots (first at its planned angle, then random angles; lean-tos alternate distance r-3 and r-2), then is dropped.
- Lean-tos go first, so the guaranteed lean-to always fits.
- `CampPiece.footprints()` lists the plan's footprints (tests); `CampPiece.roomSpot()` is the room's middle floor cell (camera, path checks).
- Shade fix found on the way: CampShade treated carpet (lean-to bedrolls) as blocking, so most lean-to shade spots never resolved; carpet now counts as open.

## Tests
- New milestone server test `CampsGameTest.campPiecesNeverOverlap`: 32 seeds x every kind x every size, no two footprints share a cell. (Milestone objective line: "camp pieces never overlap, 32 seeds per kind and size".)
- `campsHaveShadeAndARoom` path target now `roomSpot()` (the room can be offset).
- Client part `camps`: three extra seeds per kind (small, medium, large) from above (`camp-<kind>-seed<n>-<size>`); `camp-room`/`camp-room-back` now stand in the actual room (`roomSpot()`), not the old fixed centre; the natural room path check targets `roomSpot()` with accuracy 2.
