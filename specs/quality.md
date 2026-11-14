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
| `tools/packload.py` | 60-90 s | same | vanilla server start + world gen |

Next candidates: run game tests of several mods in one server boot; a smaller/void test world for packload; reuse the packload world between runs; cache the client game test's world.
