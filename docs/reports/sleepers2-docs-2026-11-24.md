# Builder report: face-down sleepers, sleeper tooltip, statue radius, gathering statue ring, totem flakes (2026-11-24)

For the docs merge into `specs/undead.md`, `specs/totems-artifacts.md`, `specs/quality.md`, `BACKLOG.md` and `player-docs/`.

## Bubba's Reworked Mobs: face-down sleepers (C1)

**Player-facing**
- Sleepers now also lie **face down**, besides sitting and lying on their back. Every kind (skeleton, zombie, husk, drowned, stray) has it.
- Items: "Sleeping Skeleton (Face Down)", "Skeleton Statue (Face Down)" and so on; the creative tab lists the face-down forms (sleeper and statue) for every kind.
- Where they show up: crypt niches and noble halls (30% of the lying ones turn face down), drowned shipwreck crew (40% of the lying ones), and the resting dead in yard graves (15%).
- Item tooltips no longer show the raw block-state lines ("pose: lying", "resting: true"); the name already says the form.

**Spec facts (specs/undead.md)**
- `SleeperPose.PRONE` (blockstate value `prone`), after `LYING`. `SleeperPose.isFlat()` (lying or prone) drives the collision shape (same 16x5 slab as lying) and BackToSleep's facing pick.
- Every `UndeadVariant` poses list ends with PRONE, so the default (first) pose of each kind is unchanged.
- Client (`SleeperPoses`): prone placement is the lying placement turned 180 degrees about the body's long axis (same footprint and height, face to the floor). Part pose: head turned to the side (cheek down), both arms at the sides, the weapon arm twisted a quarter turn so a held weapon lies flat beside it, legs slightly apart.
- Shares in `UndeadTuning`: `CRYPT_FACE_DOWN_SHARE = 0.3` (all crypt `Builder.sleeper` calls with LYING), `SHIPWRECK_FACE_DOWN_SHARE = 0.4`, `RESTING_FACE_DOWN_SHARE = 0.15`.
- Tooltip: sleeper items carry a default `minecraft:tooltip_display` hiding `minecraft:block_state`.
- Lang: `item.bubbas_reworked_mobs.sleeper.pose.prone` = "Face Down".

**Tests**
- `SleeperFormsGameTest.everyFormSurvivesPickUpAndPlace` now covers prone (loops over each kind's poses).
- New milestone `SleeperFormsGameTest.faceDownSleepersAreNamedAndWake`: names, and a face-down zombie hears an explosion and wakes.
- New client part `prone` (`ProneSleepersClientGameTest`): prone-row, prone-low, prone-close, prone-armoured, prone-tooltip.
- Milestone objective line for the spec: "Face-down sleepers: every kind has the pose, round-trips through pick-block/Silk Touch, and wakes like any other."

## Bubba's Totems & Artifacts (C6)

**Universal statue radius (owner)**
- Every statue's area is 5 blocks each way now: one constant `TotemsTuning.STATUE_RADIUS = 5` (was 3; `WARD_STATUE_RADIUS` and `GATHER_STATUE_RADIUS` removed). `StatueEffects.area(pos, type)` is the same box for every type.
- Player-facing: all statues (Growth, Herding, Fortification, Clear Skies, Warding, Gathering) reach 5 blocks.
- Tests: `statuesPulseTheirArea` "too big" check moved to radius + 2; `CryptWardsGameTest` uses `STATUE_RADIUS`.

**Gathering statue ring (owner)**
- The gathering statue no longer puts items into a touching chest or barrel (`Gathering.intoChest`/`chestBeside` deleted; no chest insertion anywhere for Gathering).
- Instead each statue pulse refreshes a `GatheringRing` round the statue: dropped items and XP within the statue radius (5) fly to it and circle it weightless; a hopper beside the statue collects them, as with the beacon ring. One ring per item (the existing claiming) still holds.
- Statue ring size: radius 1.0 (`STATUE_RING_RADIUS`), height 1.1 above the statue's base (`STATUE_RING_HEIGHT`), so it passes over the blocks beside the statue (inside a side hopper's pickup area) and just clears a chest or hopper there. The beacon ring stays 1.25 / 1.05.
- `GatheringRing.put` has an overload taking radius and height; rings keep their own size.
- Player-facing: "A Gathering statue makes dropped items within 5 blocks circle it; put a hopper next to it to collect them."
- Tests: new milestone `NewTotemsGameTest.gatheringStatueRingsItemsForASideHopper` (in range ringed and collected by a side hopper into a chest, touching chest stays empty, item beyond the radius untouched). Client: `totems-gathering-statue-ring` shot in `TotemsClientGameTest` (part `totems`).

**Flaky core tests fixed (remove from BACKLOG)**
- `held_totem_pulses_and_scares_monsters`: cause was the flee goal taking any vanilla `getPosAway` position not closer to the holder, often one at right angles, so the zombie sidestepped instead of running (4.0 -> 4.9 blocks). `WardFleeGoal` now tries `FLEE_TRIES = 6` positions and runs to the farthest. Distance after 40 ticks went from 6.5-8.6 (with misses) to 7.2-8.9.
- `ward_stops_spawns_and_sends_monsters_out`: cause was one null `getPosTowards` roll marking the zombie "trapped" for `TRAPPED_RECHECK_TICKS` (200), longer than the rest of the test. Leaving a ward now tries up to 6 positions and is trapped only if none gives a path out. (The "3 blocks up" in the old note was the absolute y; the zombie was on the floor.)
- Verified: 10/10 each alone, 10/10 of the whole core suite, all with `-PrerunGametests`.
- Player-facing: warded monsters run straight away from a Totem of Warding holder, and walk out of wards reliably.
