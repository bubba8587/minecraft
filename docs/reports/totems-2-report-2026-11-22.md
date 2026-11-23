<!-- dte:C6 -->
# totems-2: report (builder: agent 3)

Branch `totems-2` (worktree ../mc-totems2), off develop c327016. Docs for the spec and player page: `docs/reports/totems-2-docs-2026-11-22.md`.

- Built: the Gathering beacon ring (hopper-collectable), Growth planting dropped seeds, the Totems of Herding and Clear Skies (items, statues, beacon payloads, loot, art at 16x and 32x), and tiny-radius statue effects.
- Core: 22 tests pass (5 new). Client parts `totems` and `statues` were re-shot and looked at; the ring shot shows items orbiting and one entering the side hopper.
- Checked against specs/vanilla-subsystems.md; the hand-rolled bits and their reasons are in the docs report. Comments are labels (B38).
- **Flaky on develop, not mine:** `ward_stops_spawns_and_sends_monsters_out` fails about 2 runs in 10 on plain develop (measured on e2fe7e2). The zombie is left stuck at y+3 inside the ward. `held_totem_pulses_and_scares_monsters` failed once here too. The audit commit follows separately.
