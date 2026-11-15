<!-- dte:A4 -->
# Quality

## Every mod has automated game tests that must pass before release

Each mod ships with Fabric game tests covering its main behavior, and the build that publishes a release runs all of them (core and milestone, below) first. Packs get the equivalent: they are loaded on a headless server and must load clean.

## Test suites: core and milestone (test-lite, owner's standing instruction, 2026-11-15)

The owner, for speed: "For things that you are confident in, write down the test objectives and what they SHOULD achieve, but separate out those tests and do not run them except upon major release milestones. This should be an ongoing policy as part of speedy development." Every commit is local history, so a test that isn't run every day is still there to run (or write) later.

- **Core suite (every build, every push).** A small smoke set per mod, about 3-8 server game tests: the mod loads and each main mechanic works at least once. One class per mod, `<Mod>CoreGameTest`, listed under `"fabric-gametest"` in `src/gametest/resources/fabric.mod.json`. A core test usually just calls a test method that lives in a milestone class next to its helpers; that method then has no `@GameTest` of its own (the core class carries the annotation: environment, structure, ticks), so the full suite runs each test once.
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
| Test world screenshot tour (`:testworldshots:runClientGameTest`, 73 shots, all mods) | - | ~50 s | spectator + `tp` per shot, waits only for chunk render + 5 ticks (owner PC, 2026-11-15) |
| Crafting Tweaks: server tests after a change (`-PrerunGametests`) | 11-13 s full (12 tests) | ~10 s core (5) | test-lite core suite; server boot (~9 s) is the floor (owner PC, 2026-11-15) |
| Infused Gems: server tests after a change | 12-15 s full (80) | ~10 s core (9) | same |
| Reworked Mobs: server tests after a change | 14-15 s full (61) | ~10 s core (9) | same |
| Client tests on the default loop (per mod) | 32 s (Crafting Tweaks), 40 s+ (Infused Gems), 59 s (Reworked Mobs), 72 s (tour) | 0 s (skipped); one part 25 s | client tests and the tour are milestone; agents run only the parts they changed (owner PC, 2026-11-15) |
| Switching suites (`-PmilestoneTests` on or off) | - | +2-3 s once | configuration cache entry per property set, gametest resources reprocessed |
| `pack.py check` / `packload.py` / `packtest.py` | - | 0.4 s / 15 s / 14 s | fast enough to stay on every push (owner PC, 2026-11-15) |

Next candidates: run game tests of several mods in one server boot; a smaller/void test world for packload; reuse the packload world between runs; cache the client game test's world.

## CI on GitHub

- `.github/workflows/build.yml`, test-lite: every push and pull request runs mods (`texcheck.py`, `./gradlew build`: core server game tests) and packs (`pack.py check`, `packload.py`, `packtest.py`, under a minute together). Release tags (`<project>/v*`) and manual runs (workflow_dispatch) add `-PmilestoneTests` to the mods job (every server game test) and run the client game test job (every mod's client tests and the test world tour, on Xvfb with software Vulkan).
- Client game tests run one game client at a time (`--no-parallel`): clients started together crashed the JVM on GitHub runners (SIGBUS while loading native libraries, 2026-11-14).
- Pushes to a branch don't cancel each other's runs (only pull requests do), so every commit gets a real result; frequent pushes used to bury results under cancellations.
- Snapshot commits of unfinished agent work put the skip-CI marker in their message (CLAUDE.md, Style). Never put that marker text in any other commit message, even when describing it: GitHub skips CI for any commit whose message contains it.
- Known flaky: `GrabLootGameTest.idleZombieWalksToADisplayAndTakesIt` failed 1 in 24 forced runs (zombie still empty-handed at tick 402; not reproduced with logging). If CI shows it, investigate rather than rerun blindly.
