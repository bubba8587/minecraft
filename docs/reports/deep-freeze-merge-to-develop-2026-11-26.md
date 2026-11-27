# What develop (26.3) can take from deep_freeze now

Most of the 26.4 port can't go to `develop`: it uses 26.4-only Minecraft code (new registries, renamed methods, Frostbites, the Freezing effect) and would not compile on 26.3. The pieces below work the same on 26.3, so merging them now shrinks the release-day merge. All are on branch `deep_freeze` (worktree `../minecraft-deep_freeze`).

## Safe to merge now

1. **The saved 26.4 changelogs:** `docs/knowledge/changelogs/26.4/` (Mojang's official notes for every snapshot and pre-1, plus the raw wiki page). Docs only.
2. **26.4 porting notes:** section 6 of `docs/knowledge/porting-26x.md` (what changed in 26.4 and how each mod was ported). Docs only; it describes 26.4, nothing on 26.3 changes.
3. **Port-check tools:** `tools/portcheck/` (compile error digest, mixin target/signature/`@Shadow` checks, removed vanilla JSON). Plain Python, runs on any version.
4. **Separate Minecraft range in `fabric.mod.json`:** `gradle/fabric-mod.gradle` gains a `minecraft_dependency` property (default `~<minecraft_version>`), the ten `fabric.mod.json` files and the `tools/new-mod.py` template use `${minecraft_dependency}`. On 26.3 it still expands to `~26.3`, so nothing changes; on a snapshot it fixes "no mod loads". Take the `gradle/fabric-mod.gradle`, `fabric.mod.json` and `tools/new-mod.py` hunks only, **not** the `gradle.properties` lines (those set 26.4).
5. **Version-aware gem balance tool:** in `tools/gem-balance.py`, the `_mc_version()`, `_mcsrc()` and `mob_effects_source()` helpers and the `MC`/`MCSRC` uses. It then works from a worktree and on both versions. Leave out the Freezing gem: the `effect("freezing", ...)` block and the `"freezing"` line in `WORN` (on 26.3 its check fails, since 26.3 has no Freezing effect).

## Wait for 26.4

Everything else: the toolchain and pack format bump, all Java changes, the mixin retargets, the Freezing gem and its recipes, Frostbite sleepers, Frostbite camps, the Ice Caves feature and the looping office jukebox chest.

One habit worth taking now: camp kinds are saved by their position in `CampKind`, so a new kind always goes at the end (see `deep-freeze-docs-2026-11-26.md`).

## How

Items 1 to 3 are whole new files: `git checkout deep_freeze -- docs/knowledge/changelogs tools/portcheck` on `develop`, plus section 6 of the porting notes. Items 4 and 5 are hunks: `git checkout -p deep_freeze -- gradle/fabric-mod.gradle tools/new-mod.py tools/gem-balance.py 'mods/*/src/main/resources/fabric.mod.json'` and pick the parts above. Then build one mod on 26.3 (`./gradlew :refmod:build`) and run `python tools/gem-balance.py --check`.
