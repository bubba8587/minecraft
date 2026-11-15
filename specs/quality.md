<!-- dte:A4 -->
# Quality

## Every mod has automated game tests that must pass before release

Each mod ships with Fabric game tests covering its main behavior, and the build that publishes a release runs them on a headless server first. Packs get the equivalent: they are loaded on a headless server and must load clean.

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

Next candidates: run game tests of several mods in one server boot; a smaller/void test world for packload; reuse the packload world between runs; cache the client game test's world.

## CI on GitHub

- `.github/workflows/build.yml` runs three jobs on every push and pull request: mods (`./gradlew build`, server game tests), client game tests on Xvfb with software Vulkan, and packs (`pack.py check`, `packload.py`, `packtest.py`).
- Client game tests run one game client at a time (`--no-parallel`): clients started together crashed the JVM on GitHub runners (SIGBUS while loading native libraries, 2026-11-14).
- Pushes to a branch don't cancel each other's runs (only pull requests do), so every commit gets a real result; frequent pushes used to bury results under cancellations.
- Snapshot commits of unfinished agent work put the skip-CI marker in their message (CLAUDE.md, Style). Never put that marker text in any other commit message, even when describing it: GitHub skips CI for any commit whose message contains it.
- Known flaky: `GrabLootGameTest.idleZombieWalksToADisplayAndTakesIt` failed 1 in 24 forced runs (zombie still empty-handed at tick 402; not reproduced with logging). If CI shows it, investigate rather than rerun blindly.
