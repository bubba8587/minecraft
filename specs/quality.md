<!-- dte:A4 -->
# Quality

## Every mod has automated game tests that must pass before release

Each mod ships with Fabric game tests covering its main behavior, and the build that publishes a release runs all of them (core and milestone, below) first. Packs get the equivalent: they are loaded on a headless server and must load clean.

## Test suites: core and milestone (test-lite, owner's standing instruction, 2026-11-15)

The owner, for speed: "For things that you are confident in, write down the test objectives and what they SHOULD achieve, but separate out those tests and do not run them except upon major release milestones. This should be an ongoing policy as part of speedy development." Every commit is local history, so a test that isn't run every day is still there to run (or write) later.

- **Core suite (every build, every push).** A small smoke set per mod: the mod loads and each main mechanic works at least once (2026-11-22: Extra Recipes 4, Reworked Beacons 4, Reworked Mining 5, Infused Gems 9, Totems & Artifacts 17, Reworked Mobs 34; the last two have grown past "small" and are worth trimming when their build feels slow). One class per mod, `<Mod>CoreGameTest`, listed under `"fabric-gametest"` in `src/gametest/resources/fabric.mod.json`. A core test usually just calls a test method that lives in a milestone class next to its helpers; that method then has no `@GameTest` of its own (the core class carries the annotation: environment, structure, ticks), so the full suite runs each test once.
- **Milestone suite (release milestones only).** Every other server game test class, listed under `"bubba-milestone-gametest"` in the same file (Fabric ignores that key; the build appends it to `"fabric-gametest"` with `-PmilestoneTests`), plus every client game test and the test world screenshot tour. Each mod's spec has a "Milestone test objectives" section: one line per milestone class saying what it proves.
- **Confident new work:** write the objective (one line, in the spec's "Milestone test objectives") instead of a test; writing the milestone test code is optional until the release. Not confident (new kind of mechanic, fiddly maths, a past bug area): write the test, as milestone unless it is the mechanic's first smoke test, which goes in core.
- **Bug fixes** ship with a milestone test that would have caught the bug, or at least a written objective for one.
- **Visual checks stay** (CLAUDE.md, Quality bar): when something visible changes, run only that client test part (`-PclientTests=<names>`) and look at the screenshots. Full client runs and the tour are milestone.
- **A release milestone** (before tagging `<project>/v<version>`): run everything (`./gradlew :<mod>:build :<mod>:runClientGameTest -PmilestoneTests`, plus the tour and the pack checks), write the tests for any objective that has none yet when it matters for the release, and fix whatever fails before tagging. CI runs the full suite again on the tag.
- **Mechanism** (`gradle/fabric-mod.gradle`): `processGametestResources` merges the milestone list into `"fabric-gametest"` when `-PmilestoneTests` is set. The pass fingerprint records which suite passed: a full pass also covers core, a core pass never marks the milestone suite as passed. `runClientGameTest` is skipped unless `-PclientTests=<names>` or `-PmilestoneTests` is given.

## Speed

The owner never runs builds or tests; agents keep this loop fast unprompted (CLAUDE.md). Timings measured in the cloud container, warm Gradle daemon:

| Step | Before | Now | How (2026-11-14) |
|---|---|---|---|
| No-op compile of one mod | 14 s | 2 s | configuration cache on (`gradle.properties`) |
| `:<mod>:build`, nothing changed | 17 s | 1 s | game tests skip when code/resources match the last passing run |
| `:<mod>:build` after a code change | 16-22 s | 16-22 s | game test server boot dominates (~10 s) |
| `:packcheck:build` after a datapack edit | 23 s | 23 s | same |
| Client game test (one mod) | 25-60 s | same | client boot on software Vulkan |
| One targeted client test (`-PclientTests=talisman`) | 56 s (all of Reworked Mobs') | 25-27 s | named filter: a skipped test doesn't even create its world (owner's PC, 2026-11-14) |
| `tools/packload.py` | 60-90 s | same | vanilla server start + world gen |
| Infused Gems: `:bubbas_infused_gems:build` after a change (20 server tests) | - | 10-14 s | the tests take 1.5 s; server boot dominates (owner PC, 2026-11-14) |
| Infused Gems: all client tests (5 parts, 13 screenshots, one world) | - | 25-37 s | one part (`-PclientTests=worn`) 22-29 s |
| Test world screenshot tour (`:testworldshots:runClientGameTest`, 128 shots, all mods) | - | ~50 s | spectator + `tp` per shot, waits only for chunk render + 5 ticks (owner PC, 2026-11-15) |
| Extra Recipes: server tests after a change (`-PrerunGametests`) | 11-13 s full (12 tests) | ~10 s core (4 now) | test-lite core suite; server boot (~9 s) is the floor (owner PC, 2026-11-15) |
| Infused Gems: server tests after a change | 12-15 s full (80 then, about 86 now) | ~10 s core (8 now) | same |
| Reworked Mobs: server tests after a change | 28.6 s core (34 tests, 2026-11-22) | 19.7 s core (12), full suite ~23-38 s (125) | core trimmed to one smoke test per mechanic (owner PC, 2026-11-22) |
| Reworked Beacons: server tests after a change | not measured yet | core (5) | |
| Infused Gems: full suite (`-PmilestoneTests`) | - | 23.8 s whole build (76), core 15.3 s (8) | core loot check is 8 rolls a table, not 16,000 (owner PC, 2026-11-22) |
| refmod: server tests on every build | 22 tests | 3 core | test-lite applied 2026-11-22 |
| Client tests on the default loop (per mod) | 32 s (Extra Recipes), 40 s+ (Infused Gems), 59 s (Reworked Mobs), 72 s (tour) | 0 s (skipped); one part 25 s | client tests and the tour are milestone; agents run only the parts they changed (owner PC, 2026-11-15) |
| Switching suites (`-PmilestoneTests` on or off) | - | +2-3 s once | configuration cache entry per property set, gametest resources reprocessed |
| `pack.py check` / `packload.py` / `packtest.py` | - | 0.4 s / 15 s / 14 s | fast enough to stay on every push (owner PC, 2026-11-15) |

Next candidates: run game tests of several mods in one server boot; a smaller/void test world for packload; reuse the packload world between runs; cache the client game test's world.

## CI on GitHub

- `.github/workflows/build.yml`, test-lite: every push and pull request runs mods (`texcheck.py`, `./gradlew build`: core server game tests) and packs (`pack.py check`, `packload.py`, `packtest.py`, under a minute together). Release tags (`<project>/v*`) and manual runs (workflow_dispatch) add `-PmilestoneTests` to the mods job (every server game test) and run the client game test job (every mod's client tests and the test world tour, on Xvfb with software Vulkan).
- Client game tests run one game client at a time (`--no-parallel`): clients started together crashed the JVM on GitHub runners (SIGBUS while loading native libraries, 2026-11-14).
- Pushes to a branch don't cancel each other's runs (only pull requests do), so every commit gets a real result; frequent pushes used to bury results under cancellations.
- Snapshot commits of unfinished agent work put the skip-CI marker in their message (CLAUDE.md, Style). Never put that marker text in any other commit message, even when describing it: GitHub skips CI for any commit whose message contains it.

## Test isolation (2026-11-16, from the flaky-test pass)

The game test server (vanilla) does not isolate tests: batches run in sequence on a grid 5 blocks apart, nothing is
cleared after a test, and the clock runs on while a test waits for its chunks. The test mod fixes that for every
mod's tests that load it (Reworked Mobs): each finished test's area is wiped and its mock players logged out
(`TestAreaCleanupMixin`), the clock and weather stand still (`FrozenClockMixin`: time is the environment's, else
morning, always clear), and a failure logs its surroundings (`FailureContextMixin`, "FAILURE CONTEXT"). `maxAttempts`
does nothing on the test server (only a test's first attempt counts): don't use it to paper over a flake.
Chase one test with `-PgametestFilter=<selector>`.

Still unexplained and not seen since these fixes: the light engine standing still (`strongLightCalmsSleepersInWorld`)
and a drowned freezing mid-water at path node 2 (`swimsToALurkSpotNearThePlayer`). The Fabricator flakes were two tests' sites overlapping
and one test un-forcing another's chunks: fixed 2026-11-22 (fixed site origins x 30000 + n·256, forced chunks counted,
`TestCleanup` runs on failure too). Open flakes are listed in `BACKLOG.md`.

### Causes found and fixed (2026-11-16)

| Test | Cause | Fix |
|---|---|---|
| `GrabLootGameTest.idleSkeletonTakesADisplayedSword` ("holds air on tick 402") | **Sunrise.** The `clock_time` environment sets 18000 only when the batch starts; the test then waits for its chunks to load while the test server ticks as fast as it can. Once 12000 ticks went by first (clock 30009 at test tick 0): the skeleton burned, `RestrictSunGoal` + `FleeSunGoal` ran, no grabbing. | Clock and weather frozen on the game test server (`FrozenClockMixin`: `advance_time` and `advance_weather` off, clear weather). Time of day is now exactly what the environment says, else the world start (morning). |
| `GrabLootGameTest.idleZombieWalksToADisplayAndTakesIt` (empty hand at tick 402) | Same symptom and causes as the skeleton above. | Frozen clock; not seen since. |
| `UndeadCombatGameTest.idleZombiesDriftTogether` ("16.8 apart") | Not reproduced alone. Same night/day drift and leftovers next door (below) are the candidates; never failed again after the isolation fixes. | Isolation + frozen clock. |
| `BubbasReworkedMobsGameTest.sneakingNearbyDoesNotWakeSleeper`, `woolWallBlocksHearing` (sleeper became air) | **Leftovers from earlier tests.** The vanilla test server never clears a finished test: batches run one after another along a grid 5 blocks apart; on a pass it only discards entities inside the area (not players, not blocks), on a fail nothing. Sleepers, displays, items, mobs and mock players of earlier tests sat right next to later ones (seen in failure dumps: leftover sleepers and woken mobs within 15 blocks). | `TestAreaCleanupMixin`: when a test finishes (pass or fail) its area is wiped (blocks, scheduled ticks, entities) and the mock players inside it logged out. The two tests also now record what woke the sleeper (`watchWake`: noise source and maker) and put it in the message. |
| `CreeperGameTest.oneWatcherSpawnsBehindCoverOutOfView` | Daylight (same clock drift); `forceNight` worked around it. | Frozen clock (`forceNight` kept, harmless). |
| `SleeperNoiseGameTest.strongLightCalmsSleepersInWorld` (block light 0 for 600 ticks) | Two things. (1) `maxAttempts = 3` never worked: the test server's tracker counts only a test's first attempt (the retry runs, but its pass never clears the failure; the server even stops before the retry ends). (2) The level's whole light engine stood still: an update scheduled (`ThreadedLevelLightEngine.scheduled` stuck true, 1 task queued, dispatcher idle). 5 times in ~45 runs before the clock freeze; 0 times in ~60 full runs since. Root cause in vanilla not pinned down. | `maxAttempts` removed. On timeout the message now reports the light engine's state and dumps worker threads to the log (`LIGHT ENGINE THREADS`) so a recurrence shows what holds the light executor. |
| `DrownedGameTest.swimsToALurkSpotNearThePlayer` (new: "state TRAVEL on tick 402") | **Real bug.** Swimming up the pit wall to its spot, the drowned's move control aims at the next node's height, which at the pit's lip is exactly the shallows' floor top; it eased up to 6.99 and hovered for good, pressed against the lip. Vanilla's ledge hop (`LivingEntity#jumpOutOfFluid`) needs open air above and vanilla drowned skip it while swimming. About 1 in 20-30 runs. | `DrownedMixin#brm$climbLedges`: while our mind steers a swim, blocked sideways, heading level or up, with room above: rise at `DrownedTuning.LEDGE_RISE_SPEED` (0.04/tick, ~0.4 block with drag). 45/45 single runs after. A second, rarer mode (frozen mid-water, path at node 2, no move target; 2 in ~90 single runs, before the fix) was not seen again and not explained. |
| `EndermanGameTest.siteSpawnsBuildersUpToItsCap` (new: "spawned 8.06 blocks out") | **Real bug.** `EndermanSpawning.trySpawnAt` measured the spawn ring from the origin block's corner, not its centre, so spawns sat half a block toward -x/-z and could land 8+ blocks out (bound is 4-7). | Ring measured from the block centre: every roll now lands within 3.3-7.7 blocks, so the test checks the rule, not a roll. |
