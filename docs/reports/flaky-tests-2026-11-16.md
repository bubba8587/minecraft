# Flaky tests, Bubba's Reworked Mobs server suite (2026-11-16)

Builder report for the orchestrator / docs agent (merge into `specs/quality.md` and `BACKLOG.md`, then delete).
Leaf: C1 (test infrastructure only, plus two small mod bug fixes found by the flakes).

## Before

`./gradlew :bubbas_reworked_mobs:build -PmilestoneTests -PrerunGametests` failed in about 1 run of 4-6
(14 baseline/diagnostic runs: 3 red). Failures collected from ~150 runs (full and single-test):

## Causes and fixes, per test

| Test | Cause | Fix |
|---|---|---|
| `GrabLootGameTest.idleSkeletonTakesADisplayedSword` ("holds air on tick 402") | **Sunrise.** The `clock_time` environment sets 18000 only when the batch starts; the test then waits for its chunks to load while the test server ticks as fast as it can. Once 12000 ticks went by first (clock 30009 at test tick 0): the skeleton burned, `RestrictSunGoal` + `FleeSunGoal` ran, no grabbing. | Clock and weather frozen on the game test server (`FrozenClockMixin`: `advance_time` and `advance_weather` off, clear weather). Time of day is now exactly what the environment says, else the world start (morning). |
| `UndeadCombatGameTest.idleZombiesDriftTogether` ("16.8 apart") | Not reproduced alone. Same night/day drift and leftovers next door (below) are the candidates; never failed again after the isolation fixes. | Isolation + frozen clock. |
| `BubbasReworkedMobsGameTest.sneakingNearbyDoesNotWakeSleeper`, `woolWallBlocksHearing` (sleeper became air) | **Leftovers from earlier tests.** The vanilla test server never clears a finished test: batches run one after another along a grid 5 blocks apart; on a pass it only discards entities inside the area (not players, not blocks), on a fail nothing. Sleepers, displays, items, mobs and mock players of earlier tests sat right next to later ones (seen in failure dumps: leftover sleepers and woken mobs within 15 blocks). | `TestAreaCleanupMixin`: when a test finishes (pass or fail) its area is wiped (blocks, scheduled ticks, entities) and the mock players inside it logged out. The two tests also now record what woke the sleeper (`watchWake`: noise source and maker) and put it in the message. |
| `CreeperGameTest.oneWatcherSpawnsBehindCoverOutOfView` | Daylight (same clock drift); `forceNight` worked around it. | Frozen clock (`forceNight` kept, harmless). |
| `SleeperNoiseGameTest.strongLightCalmsSleepersInWorld` (block light 0 for 600 ticks) | Two things. (1) `maxAttempts = 3` never worked: the test server's tracker counts only a test's first attempt (the retry runs, but its pass never clears the failure; the server even stops before the retry ends). (2) The level's whole light engine stood still: an update scheduled (`ThreadedLevelLightEngine.scheduled` stuck true, 1 task queued, dispatcher idle). 5 times in ~45 runs before the clock freeze; 0 times in ~60 full runs since. Root cause in vanilla not pinned down. | `maxAttempts` removed. On timeout the message now reports the light engine's state and dumps worker threads to the log (`LIGHT ENGINE THREADS`) so a recurrence shows what holds the light executor. |
| `DrownedGameTest.swimsToALurkSpotNearThePlayer` (new: "state TRAVEL on tick 402") | **Real bug.** Swimming up the pit wall to its spot, the drowned's move control aims at the next node's height, which at the pit's lip is exactly the shallows' floor top; it eased up to 6.99 and hovered for good, pressed against the lip. Vanilla's ledge hop (`LivingEntity#jumpOutOfFluid`) needs open air above and vanilla drowned skip it while swimming. About 1 in 20-30 runs. | `DrownedMixin#brm$climbLedges`: while our mind steers a swim, blocked sideways, heading level or up, with room above: rise at `DrownedTuning.LEDGE_RISE_SPEED` (0.04/tick, ~0.4 block with drag). 45/45 single runs after. A second, rarer mode (frozen mid-water, path at node 2, no move target; 2 in ~90 single runs, before the fix) was not seen again and not explained. |
| `EndermanGameTest.siteSpawnsBuildersUpToItsCap` (new: "spawned 8.06 blocks out") | **Real bug.** `EndermanSpawning.trySpawnAt` measured the spawn ring from the origin block's corner, not its centre, so spawns sat half a block toward -x/-z and could land 8+ blocks out (bound is 4-7). | Ring measured from the block centre: every roll now lands within 3.3-7.7 blocks, so the test checks the rule, not a roll. |

## Other changes

- `FailureContextMixin` (test mod): on any failure, one `FAILURE CONTEXT` log line with every entity within 40 blocks
  (inside/outside the area, target, running goals, hand, path state), block entities within 10, night or day, players
  logged in. This is what found the causes above. `MobAccessor` gives it the goal selector.
- `-PgametestFilter=<selector>` (`gradle/fabric-mod.gradle`): run only matching server tests, e.g.
  `-PgametestFilter='bubbas_reworked_mobs-test:drowned_game_test_*'` (vanilla selector, wildcards). ~16 s per run
  for chasing one test; a filtered pass records no pass marker.
- Messages of the drowned travel and enderman spawn tests now say where the mob was.

## Spec text for `specs/quality.md` (new section "Test isolation")

The game test server (vanilla) does not isolate tests: batches run in sequence on a grid 5 blocks apart, nothing is
cleared after a test, and the clock runs on while a test waits for its chunks. The test mod fixes that for every
mod's tests that load it (Reworked Mobs): each finished test's area is wiped and its mock players logged out
(`TestAreaCleanupMixin`), the clock and weather stand still (`FrozenClockMixin`: time is the environment's, else
morning, always clear), and a failure logs its surroundings (`FailureContextMixin`, "FAILURE CONTEXT"). `maxAttempts`
does nothing on the test server (only a test's first attempt counts): don't use it to paper over a flake.
Chase one test with `-PgametestFilter=<selector>`.

Also replace the "Known flaky: `idleZombieWalksToADisplayAndTakesIt`" line under CI: same symptom as the skeleton
(empty hand at tick 402 in a night environment), same causes, not seen in the runs since.

Milestone test objective (drowned spec): a drowned swimming out of a pit onto a 2-deep shallows gets over the lip
(covered statistically by `swimsToALurkSpotNearThePlayer`; a deterministic start under the lip did not reproduce the
hover, so no separate test).

## Timing

Game test phase of the full run (95 tests before, 101 after the merge) 12.5-14.5 s on a quiet machine before and after
(noisy container: up to 22 s under load either way); full `build -PmilestoneTests -PrerunGametests` wall time ~26 s.
No measurable cost from wiping areas.

## Run counts

- Before the merge (95 tests), all fixes in: 11 consecutive full runs green (plus 15 + 15 earlier full runs with one
  drowned failure each batch before the ledge fix and one enderman failure before the ring fix).
- After merging `claude/optimistic-tesla-dhtgzb` (101 tests, incl. the Fabricator tests): **12 consecutive full runs
  green**, game test phase 17.5-18.6 s (the six new tests account for the rise; not compared separately).
- Single-test loops: drowned travel 45/45 after the ledge fix, skeleton sword 40 (failures all explained by the clock),
  light test 21/21 alone.
