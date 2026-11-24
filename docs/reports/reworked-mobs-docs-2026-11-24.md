# Reworked Mobs docs report, 2026-11-24: endermen leave beacon bases alone

Builder report for the BACKLOG item "Enderman broke a Beginning beacon's pyramid" (C6 section; the code is C1, Reworked Mobs). Merge into `specs/endermen.md` and the Reworked Mobs player page, then delete this file.

## What was already there

- Caretakers already skipped every `#minecraft:beacon_base_blocks` block for terrain mending and cairn-footprint clearing (`TerrainKinds.liftable` / `clearable`, commit 15725ca, 2026-11-22, spec "Caretakers leave builds alone"). The owner's playtest (2026-11-23) likely ran a build from before that fix.
- Two gaps remained: (1) vanilla's random `EndermanTakeBlockGoal` (Nether and End endermen, and overworld endermen saved as vanilla) had no beacon rule, which matters when a datapack or mod makes a beacon base block holdable; (2) a caretaker fetching a block an unfinished cairn wants (`EndermanWork.readyCairnBlock`) had no beacon rule, which matters when a datapack or mod makes a cairn block (purpur, end stone) a beacon base block (prior art: PurpurPack bases).

## What changed (spec facts for `specs/endermen.md`)

New section "Beacon bases" (next to "Caretakers leave builds alone"):

- **No enderman ever takes a block of a working beacon's base, anywhere** (owner). Covers vanilla's take-block goal (mixin `EndermanTakeBlockMixin`, wrapping its holdable check), caretakers' take step (`EndermanJobGoal.take`, a final guard for every caretaker take) and cairn-wanted fetches (`EndermanWork.readyCairnBlock`). The blanket caretaker rule (never any beacon base block) stays as it was.
- **Working base** (`enderman/BeaconBases.inWorkingBase`): the block is in `#minecraft:beacon_base_blocks` and sits in a full pyramid layer of a beacon, counted from the beacon down to the block's layer (layer N is the (2N+1)x(2N+1) square N below the beacon; layers 1 to N all base blocks). Same shape and tag as vanilla's `BeaconBlockEntity.updateBase` and Reworked Beacons' `BeaconLogic.pyramidLevels`. A full pyramid **above** a beacon counts too, for Reworked Beacons' upside-down pairs; no Reworked Beacons API or optional-mod check is needed (that mod uses the same tag and layer shape).
- It does not ask whether the beam is clear or the beacon is paid: a covered or paused beacon's full pyramid is still a build. A pyramid block outside the beacon's complete layers (an unfinished layer, or loose base blocks lying around) can still be taken by vanilla endermen if holdable.
- **Cost:** a block not in the base tag returns at once (the normal case). For a base block it reads at most 164 block states per direction to find a beacon (layers 1-4: 3x3 + 5x5 + 7x7 + 9x9), plus the found beacon's layers; only when an enderman is about to take that block. Unloaded positions never count.
- **Not vanilla, and why** (for the spec's list): vanilla has no beacon POI and no lookup from a block to the beacon it serves, so the check reads the pyramid's block states the way vanilla's own `updateBase` does. Block-entity lookups would need the same position scan to find the beacon and would miss nothing more.

## Milestone test objective (add to "Milestone, `EndermanGameTest`")

- `endermenNeverTakeWorkingBeaconBases`: test data makes iron blocks holdable (`data/minecraft/tags/block/enderman_holdable.json` in the gametest source set). A vanilla enderman's take-block goal, ticked 3000 times beside a 1-layer iron pyramid under a beacon, takes all three stray iron blocks beside it and no pyramid block; with the beacon removed it takes pyramid blocks. `BeaconBases` also counts a full gold layer above a beacon (upside-down pair) and stops counting once that layer has a gap. Verified to fail with the guard inverted.

## Player-facing (Reworked Mobs page, endermen section)

- Endermen never pick up blocks from a working beacon's pyramid, any beacon, anywhere (also beacons hung upside down with Reworked Beacons).

## Owner decisions / builder's choices

- Owner (BACKLOG, 2026-11-23): endermen should never take beacon pyramid blocks, or any block of a working beacon base, anywhere.
- Builder's choice: "working" means a full pyramid layer under (or over) a beacon block, not "the beacon is lit"; this also protects underground paired pyramids and beacons with a blocked beam.
