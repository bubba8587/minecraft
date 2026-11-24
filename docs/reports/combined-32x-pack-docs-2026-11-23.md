# Docs report: combined 32x resource pack (B16), 2026-11-23

For the orchestrator / docs agent to merge into `specs/` and `player-docs/`, then delete.

## What changed (facts)

- New resource pack project `resourcepacks/bubbas_32x/` ("Bubba's Mods 32x"): `pack.mcmeta` (format 97/97, like every other pack), `VERSION` 0.1.0, `CREDITS.md` (merged Classic Faithful 32x Jappa credits of Reworked Mining and World Tweaks, with the faithfulpack.net and license links), `README.md` (not shipped in the zip), and a `COMBINE` file.
- **No art is committed in `resourcepacks/bubbas_32x/`.** The art stays in each mod's `src/main/resources/resourcepacks/<modid>_32x/assets/` (generators and `tools/texcheck.py` unchanged). `COMBINE` lists globs (here `mods/*/src/main/resources/resourcepacks/*_32x`) whose `assets/` are merged when `tools/pack.py` checks or zips the pack. Internal and disabled mods are left out. A file path present in two sources fails the check ("in both X and Y"). A committed `assets/` folder in a combined pack also fails the check. The zip is written straight from the sources, so there is no generated copy to gitignore.
- `python3 tools/pack.py build bubbas_32x` -> `build/packs/resourcepacks/bubbas_32x-<VERSION>.zip`; check and build print the file count per source. First build: 82 files (pack.mcmeta, CREDITS.md, 80 assets: extra_recipes 2, infused_gems 4, reworked_mining 11, reworked_mobs 2, totems_artifacts 46, world_tweaks 15).
- Mods no longer register built-in 32x packs (registerBuiltinPack removed from the client entrypoints of Extra Recipes, Infused Gems, Reworked Mining, Reworked Mobs, Totems & Artifacts, World Tweaks). Extra Recipes' bundled datapack/resource pack registration (`bundled/...`) is untouched.
- `gradle/fabric-mod.gradle` excludes `resourcepacks/**` from every published mod's jar and sources jar (internal mods such as packcheck keep theirs). Dev and test runs still read `build/resources/main/resourcepacks/`.
- Test runs: new shared class `gradle/shared-gametest/java/io/github/bubba8587/shared/test/Dev32xPacks.java`, added as a `client` entrypoint to every published mod's generated gametest `fabric.mod.json` (in `processGametestResources`). It registers `<modid>:<modid>_32x` for each loaded mod that has `resourcepacks/<modid>_32x/pack.mcmeta`, with the same ids, so every existing `-32x` screenshot test works unchanged. Only test runs (gametest source set) load it; players never see these built-in packs.
- `mods/packcheck/build.gradle` skips combined packs (COMBINE file) when bundling `resourcepacks/*` as always-on packs. `tools/texcheck.py` skips combined packs (their textures are checked in the mods).
- `tools/new-mod.py`: new mods still get `resourcepacks/<modid>_32x/` (pack.mcmeta + empty textures folder) but an empty client entrypoint, no registerBuiltinPack.
- `tools/install-to-modrinth.ps1`: after installing the jars it builds the pack (`python`, falling back to `py -3`), removes older `bubbas_32x-*.zip` from the profile's `resourcepacks/` (nothing else), copies the new zip, and rewrites the `resourcePacks:` line of the profile's `options.txt`: drops old `bubbas_<mod>:bubbas_<mod>_32x` entries and old `file/bubbas_32x-*.zip` entries, then adds `file/bubbas_32x-<version>.zip` once, right after the last `file/CF32J...` entry (so it sits above CF32J) or at the end. Skips the options edit (with a message) if Minecraft is running on the profile, or if there's no options.txt yet. `sync-minecraft-settings.ps1` already keeps the profile's resource pack lines, so it doesn't undo this. Tested on sample options files (idempotent); the script itself was not run.
- CI (`.github/workflows/build.yml`): new `pack-release` job on tags. For a tag `<name>/v<version>` where `<name>` is a folder in `datapacks/` or `resourcepacks/` and not a mod, after `mods`, `client-gametests` and `datapacks` pass, it checks the tag version against the pack's `VERSION`, runs `tools/pack.py build <name>` and creates a GitHub Release with the zip(s) (`contents: write` for that job only). Mod tags are ignored by this job. There was no publish step for anything before; this is the first.

## Spec text to update

- `specs/textures.md` "Where they live / Mods": replace the built-in pack bullet: the 32x set lives at `src/main/resources/resourcepacks/<modid>_32x/` (own pack.mcmeta, same `assets/<modid>/textures/...` paths) but is not in the jar; players get it from the combined pack `resourcepacks/bubbas_32x` ("Bubba's Mods 32x"), placed above Classic Faithful 32x Jappa. Test runs register each as built-in pack `<modid>:<modid>_32x` (Dev32xPacks). The "display names still say (Faithful style)" note under the restyle section is obsolete (no player-facing built-in packs any more).
- `specs/textures.md` "Credit": the combined pack's `CREDITS.md` carries every CF32J credit; keep it in sync with the mods' `CREDITS.md` when derived art changes.
- `specs/releasing.md` Status: pack projects (`datapacks/`, `resourcepacks/`) now publish to GitHub Releases from CI on `<name>/v<version>`; mods still have no publish step. `bubbas_32x` goes to GitHub Releases first: it cannot go on Modrinth until it meets B14 (its textures are agent-made or CF32J-derived; derived art doesn't count as human-made).
- `specs/build.md` / CLAUDE.md layout line for `resourcepacks/`: combined packs (COMBINE file) exist; `tools/pack.py` docstring explains them.
- `specs/pack-testing.md`: packcheck leaves combined packs out.
- `specs/quality.md` (client tests): the -32x screenshots rely on Dev32xPacks, not on the mod's own client code.

## Player-facing changes (player-docs)

- Each mod's page that mentions its built-in "32x (Classic Faithful style)" pack (World Tweaks, Totems & Artifacts, Reworked Mobs + Spiders, Reworked Mining + Quartz Tools and Armor, Infused Gems, Extra Recipes' Glider, Templates/Mod.md): the 32x textures now come from one separate resource pack, "Bubba's Mods 32x", downloaded from GitHub Releases, put above Classic Faithful 32x Jappa in the Resource Packs screen. The mods no longer list their own 32x pack.
- `Getting Started/Installing Resource Packs.md`: add Bubba's Mods 32x.
- Credit for CF32J-derived textures (quartz gear, soot plumes) now ships in the pack's CREDITS.md as well.

## Builder's choices (owner may review)

- Combined-pack mechanism: a `COMBINE` glob file read by `tools/pack.py`, rather than a Gradle task or a generated folder, because pack.py already owns pack checking/zipping and CI's datapacks job already runs `pack.py check`; no copy on disk means nothing to gitignore or go stale.
- Tests: registered from shared gametest code (one class for all mods) instead of loading the combined pack, since it keeps pack ids and every test unchanged.
- Pack version starts at 0.1.0.
