<!-- dte:A4 -->
# Quality

## Every mod has automated game tests that must pass before release

- Each mod ships with Fabric game tests covering its main behavior.
- The build that publishes a release runs all of them first (core and milestone, below).
- Packs get the equivalent: they are loaded on a headless server and must load clean.

## Test suites: core and milestone (test-lite, owner's standing instruction)

The owner, for speed: "For things that you are confident in, write down the test objectives and what they SHOULD achieve, but separate out those tests and do not run them except upon major release milestones. This should be an ongoing policy as part of speedy development." Every commit stays in history, so a test that isn't run every day is still there to run (or write) later.

- **Core suite (every build, every push).** A small smoke set per mod: the mod loads, and each main mechanic works at least once.
  - Current sizes: Extra Recipes 3, World Tweaks 3, Reworked Beacons 4, Reworked Mining 5, Infused Gems 8, Reworked Mobs 14, Totems & Artifacts 17 (refmod 3). Totems & Artifacts has grown past "small" and is worth trimming when its build feels slow.
  - One class per mod, `<Mod>CoreGameTest`, listed under `"fabric-gametest"` in `src/gametest/resources/fabric.mod.json`.
  - A core test usually just calls a test method that lives in a milestone class next to its helpers. That method has no `@GameTest` of its own (the core class carries the annotation: environment, structure, ticks), so the full suite runs each test once.
- **Milestone suite (release milestones only).** Every other server game test class, plus every client game test and the test world screenshot tour.
  - Milestone classes are listed under `"bubba-milestone-gametest"` in the same `fabric.mod.json`. Fabric ignores that key; with `-PmilestoneTests` the build appends it to `"fabric-gametest"`.
  - Each mod's spec has a "Milestone test objectives" section: one line per milestone class saying what it proves.
- **Confident new work:** write the objective (one line in the spec's "Milestone test objectives") instead of a test. Writing the milestone test code is optional until the release.
- **Not confident** (a new kind of mechanic, fiddly maths, an area with past bugs): write the test. It is a milestone test, unless it is the mechanic's first smoke test, which goes in core.
- **Bug fixes** ship with a milestone test that would have caught the bug, or at least a written objective for one.
- **Visual checks stay** (CLAUDE.md, Quality bar): when something visible changes, run only that client test part (`-PclientTests=<names>`) and look at the screenshots. Full client runs and the tour are milestone.
- **A release milestone** (before tagging `<project>/v<version>`):
  1. run everything: `./gradlew :<mod>:build :<mod>:runClientGameTest -PmilestoneTests`, plus the tour and the pack checks;
  2. write the tests for any objective that has none yet, where it matters for the release;
  3. fix whatever fails before tagging. CI runs the full suite again on the tag.

### How the build does it (`gradle/fabric-mod.gradle`)

- `processGametestResources` merges the milestone list into `"fabric-gametest"` when `-PmilestoneTests` is set.
- **Skipping unchanged runs:** server game tests skip themselves when the mod's compiled code and resources match the fingerprint of the last passing run. `-PrerunGametests` forces a run.
  - The fingerprint records which suite passed: a full pass also covers core; a core pass never counts as a full one.
  - It also includes the `-Pwith*` flags (`-PwithMobs`, `-PwithBeacons`, `-PwithGems`, any `with<Capital>` property), since they put other mods on the test runtime. A run with different flags is never skipped as unchanged (a Totems run without `-PwithMobs` was once skipped after a passing `-PwithMobs` run).
  - A filtered run (`-PgametestFilter`) never records a pass.
- `-PgametestFilter=<selector>` runs only the matching tests (vanilla's selector, wildcards allowed).
- `runClientGameTest` is skipped unless `-PclientTests=<names>` or `-PmilestoneTests` is given.

## Speed

The owner never runs builds or tests; agents keep this loop fast unprompted (CLAUDE.md). Timings with a warm Gradle daemon; "cloud" is the cloud container, "PC" the owner's PC.

| Step | Before | Now | How (where measured) |
|---|---|---|---|
| No-op compile of one mod | 14 s | 2 s | configuration cache on (`gradle.properties`) (cloud) |
| `:<mod>:build`, nothing changed | 17 s | 1 s | game tests skip when code and resources match the last passing run (cloud) |
| `:<mod>:build` after a code change | 16-22 s | 16-22 s | game test server boot dominates, about 10 s (cloud) |
| `:packcheck:build` after a datapack edit | 23 s | 23 s | same (cloud) |
| Client game test (one mod) | 25-60 s | same | client boot on software Vulkan (cloud) |
| One targeted client test (`-PclientTests=talisman`) | 56 s (all of Reworked Mobs') | 25-27 s | named filter: a skipped test doesn't even create its world (PC) |
| `tools/packload.py` | 60-90 s | same | vanilla server start and world generation (cloud) |
| Infused Gems: all client tests (5 parts, 13 screenshots, one world) | - | 25-37 s | one part (`-PclientTests=worn`) 22-29 s |
| Test world screenshot tour (`:testworldshots:runClientGameTest`, all mods) | - | ~50 s | spectator plus `tp` per shot, waits only for chunk render and 5 ticks (PC) |
| Extra Recipes: server tests after a change (`-PrerunGametests`) | 11-13 s full (12 tests) | ~10 s core | test-lite core suite; server boot (~9 s) is the floor (PC) |
| Infused Gems: server tests after a change | 12-15 s full (about 80 tests) | ~10 s core (8) | same (PC) |
| Infused Gems: full suite (`-PmilestoneTests`) | - | 23.8 s whole build (76 tests); core 15.3 s (8) | the core loot check is 8 rolls a table, not 16,000 (PC) |
| Reworked Mobs: server tests after a change | 28.6 s core (34 tests) | 19.7 s core (12 tests at the time); full suite ~23-38 s (125) | core trimmed to one smoke test per mechanic (PC) |
| Reworked Beacons: server tests after a change | not measured yet | core (4) | |
| refmod: server tests on every build | 22 tests | 3 core | test-lite |
| Client tests on the default loop (per mod) | 32 s (Extra Recipes), 40 s+ (Infused Gems), 59 s (Reworked Mobs), 72 s (tour) | 0 s (skipped); one part 25 s | client tests and the tour are milestone; agents run only the parts they changed (PC) |
| Switching suites (`-PmilestoneTests` on or off) | - | +2-3 s once | one configuration cache entry per property set; gametest resources reprocessed |
| `pack.py check` / `packload.py` / `packtest.py` | - | 0.4 s / 15 s / 14 s | fast enough to stay on every push (PC) |

**Next candidates:**
- run the game tests of several mods in one server boot;
- a smaller or void test world for packload;
- reuse the packload world between runs;
- cache the client game test's world.

## CI on GitHub

`.github/workflows/build.yml`, test-lite:

- **Every push and pull request:**
  - mods: `texcheck.py`, then `./gradlew build` (core server game tests);
  - packs: `pack.py check`, `packload.py`, `packtest.py` (under a minute together).
- **Release tags (`<project>/v*`) and manual runs (workflow_dispatch):** the mods job adds `-PmilestoneTests` (every server game test), and the client game test job runs every mod's client tests and the test world tour, on Xvfb with software Vulkan.
- Client game tests run one game client at a time (`--no-parallel`): clients started together crashed the JVM on GitHub runners (SIGBUS while loading native libraries).
- Pushes to a branch don't cancel each other's runs (only pull requests do), so every commit gets a real result.
- Snapshot commits of unfinished agent work put the skip-CI marker in their message (CLAUDE.md, Style). Never put that marker text in any other commit message, even when describing it: GitHub skips CI for any commit whose message contains it.

## Test setups

Build test terrain directly (`setBlock` / fill in a small area), not by physics such as falling sand; check correctness in server game tests and keep client screenshot runs to the end.

## Test isolation

The vanilla game test server does not isolate tests:
- batches run one after another on a grid 5 blocks apart;
- nothing is cleared after a test (on a pass it only removes entities inside the area, not players or blocks; on a fail nothing);
- the clock keeps running while a test waits for its chunks.

Reworked Mobs' test mod fixes that for every test that loads it:
- `TestAreaCleanupMixin`: when a test finishes (pass or fail), its area is wiped (blocks, scheduled ticks, entities) and the mock players in it are logged out.
- `FrozenClockMixin`: clock and weather stand still (`advance_time` and `advance_weather` off). The time is the test environment's, else the world start (morning); the weather is always clear.
- `FailureContextMixin`: a failure logs its surroundings ("FAILURE CONTEXT").
- `TestCleanup`: cleanup a test registers for whatever it put outside its own area (the Fabricator tests' far-off sites, forced chunks and players). It runs when the test is done, pass or fail. Fabricator test sites also have fixed origins (x 30000 + n·256) and count their forced chunks, so one test can't overlap another's site or un-force its chunks.

Rules:
- `maxAttempts` does nothing on the test server (only a test's first attempt counts). Don't use it to paper over a flake.
- Chase one test with `-PgametestFilter=<selector>`.
- Open flakes are listed in `BACKLOG.md`.

Still unexplained, and not seen since the fixes below: the light engine standing still (`strongLightCalmsSleepersInWorld`), and a drowned freezing mid-water at path node 2 (`swimsToALurkSpotNearThePlayer`).

### Flakes found and fixed: Reworked Mobs

| Test | Cause | Fix |
|---|---|---|
| `GrabLootGameTest.idleSkeletonTakesADisplayedSword` ("holds air on tick 402") | **Sunrise.** The `clock_time` environment sets 18000 only when the batch starts; the test then waits for its chunks while the server ticks as fast as it can. Once, 12000 ticks went by first (clock 30009 at test tick 0): the skeleton burned, `RestrictSunGoal` and `FleeSunGoal` ran, and it never grabbed. | Frozen clock and weather (`FrozenClockMixin`). |
| `GrabLootGameTest.idleZombieWalksToADisplayAndTakesIt` (empty hand at tick 402) | Same as the skeleton above. | Frozen clock; not seen since. |
| `UndeadCombatGameTest.idleZombiesDriftTogether` ("16.8 apart") | Never reproduced alone. Clock drift and leftovers from earlier tests (below) are the likely causes. | Isolation and frozen clock; never failed again. |
| `BubbasReworkedMobsGameTest.sneakingNearbyDoesNotWakeSleeper`, `woolWallBlocksHearing` (sleeper became air) | **Leftovers from earlier tests.** Sleepers, displays, items, mobs and mock players of earlier tests sat right next to later ones (failure dumps showed leftover sleepers and woken mobs within 15 blocks). | `TestAreaCleanupMixin`. Both tests also record what woke the sleeper (`watchWake`: noise source and maker) and put it in the message. |
| `CreeperGameTest.oneWatcherSpawnsBehindCoverOutOfView` | Daylight from the same clock drift. | Frozen clock (`forceNight` kept, harmless). |
| `SleeperNoiseGameTest.strongLightCalmsSleepersInWorld` (block light 0 for 600 ticks) | (1) `maxAttempts = 3` never worked: the tracker counts only a test's first attempt (the retry runs, but its pass never clears the failure, and the server may stop before the retry ends). (2) The level's whole light engine stood still: an update was scheduled (`ThreadedLevelLightEngine.scheduled` stuck true, 1 task queued, dispatcher idle). 5 times in ~45 runs before the clock freeze; 0 in ~60 full runs since. Root cause in vanilla not found. | `maxAttempts` removed. On timeout the message reports the light engine's state and dumps worker threads to the log (`LIGHT ENGINE THREADS`), so a recurrence shows what holds the light executor. |
| `DrownedGameTest.swimsToALurkSpotNearThePlayer` ("state TRAVEL on tick 402") | **Real bug.** Swimming up the pit wall, the drowned's move control aims at the next node's height, which at the pit's lip is exactly the top of the shallows' floor; it eased up to 6.99 and hovered there for good, pressed against the lip. Vanilla's ledge hop (`LivingEntity#jumpOutOfFluid`) needs open air above, and vanilla drowned skip it while swimming. About 1 in 20-30 runs. | `DrownedMixin#brm$climbLedges`: while our mind steers a swim, blocked sideways, heading level or up, with room above, the drowned rises at `DrownedTuning.LEDGE_RISE_SPEED` (0.04 a tick, about 0.4 block with drag). 45/45 single runs after. A second, rarer mode (frozen mid-water at path node 2, no move target; 2 in ~90 single runs, before the fix) has not been seen again and is not explained. |
| `EndermanGameTest.siteSpawnsBuildersUpToItsCap` ("spawned 8.06 blocks out") | **Real bug.** `EndermanSpawning.trySpawnAt` measured the spawn ring from the origin block's corner, not its centre, so spawns sat half a block toward -x/-z and could land 8+ blocks out (the bound is 4-7). | The ring is measured from the block centre: every roll lands within 3.3-7.7 blocks, so the test checks the rule, not a roll. |

### Flakes found and fixed: Totems & Artifacts core

| Test | Cause | Fix |
|---|---|---|
| `heldTotemPulsesAndScaresMonsters` (zombie only 4.0 -> 4.9 blocks away) | The flee goal took any vanilla `getPosAway` position not closer to the holder, often one at right angles, so the zombie sidestepped instead of running. | `WardFleeGoal` tries `FLEE_TRIES` (6) positions and runs to the farthest. Distance after 40 ticks: 6.5-8.6 with misses before, 7.2-8.9 after. |
| `wardStopsSpawnsAndSendsMonstersOut` (zombie "stuck" inside; about 2 in 10) | One null `getPosTowards` roll marked the zombie trapped for `TRAPPED_RECHECK_TICKS` (200), longer than the rest of the test. | Leaving a ward tries up to 6 positions and counts as trapped only if none gives a path out. |

Both verified with `-PrerunGametests`: 10/10 each alone and 10/10 of the whole core suite.

## Shared name check (B20)

- **Shared core test `NamesGameTest`** (`gradle/shared-gametest/java/io/github/bubba8587/shared/test/NamesGameTest.java`).
  - `gradle/fabric-mod.gradle` adds it to the gametest source set and core `fabric-gametest` entrypoints of every published mod (no `internal=true`), so it runs in every mod's core suite with no per-mod setup.
  - It fails when an item, block, entity type, mob effect, creative tab, enchantment, structure (`structure.<ns>.<path>`) or item tag (`tag.item.<ns>.<path>`, the Fabric convention that EMI and others use) in a loaded `bubbas_*` namespace has no en_us entry. It reads the server's language table, which Fabric fills with every mod's `en_us.json`.
  - Its first run found 11 missing names (structures and item tags, listed above); all are fixed now.
- Mobs' lang is generated: add names in `ModDataGenerator.EnglishLang` and run `./gradlew :bubbas_reworked_mobs:runDatagen`. Datagen rewrites every generated file with LF line endings, so commit only the files whose content changed.
