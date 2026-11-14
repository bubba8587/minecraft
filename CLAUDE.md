<!-- dte:A2,B1,B4,B5,B6,B7,B8,B9,B10,B11,B12,B13,B14 -->
# Working in this repo

A home for a series of Minecraft mods, datapacks and resource packs, made with Fabric and released on Modrinth. The owner is technically minded but not a programmer: explain in plain language and hand over things they can run or look at (A3 ownerSteersByPlay).

## Decision tree (read first)

This repo keeps its decisions as a Decision Tree Engineering tree. Follow `vendor/dte/PROTOCOL.md` (rules in `vendor/dte/SPEC.md`). The tool is `python3 tools/dte.py`.

- **Your ring until the owner says otherwise: B.** The owner holds A. Subagents get C or deeper.
- **Rings here:** `A` the project's goals (owner's). `B` repo-wide calls: platform, versions, layout, quality bar, release flow. `C` per-project calls (one mod or pack's design choices worth asking the owner about). Deeper rings for sub-features if a project needs them.
- Start each session with `python3 tools/dte.py outbox` and `python3 tools/dte.py inbox`. Open inbox questions block what they say they block (publishing, notably).
- Finish with `DTE_RING=B python3 tools/dte.py validate --as B`.

## Before designing anything: prior art (B8 priorArtFirst, B9 libsOverHandroll)

**Reminder from the owner (B9 libsOverHandroll): use outside libraries and mods aggressively; hand-roll only what's necessary for a well-designed, performant mod.** Hand-rolling is justified when no library does it, the library has no build for our Minecraft version, or it would cost real performance or design quality. Say which of those applies when you hand-roll something a library could have done. Every dependency goes in `fabric.mod.json` `depends` and in the Modrinth version's dependency list.

Lots of mods already exist. Before designing a mod, pack or tool:

1. `python3 tools/prior-art.py "<idea>" [--type mod|datapack|resourcepack]` and a GitHub search. If something already does most of it, tell the owner before building and say what ours would add.
2. Use an existing library or tool instead of writing one (`docs/knowledge/libraries.md`), and check `docs/knowledge/reference-mods.md` for open-source mods that solve the same problem: read how they did it (mind their license; learning from code is fine, copying needs a compatible license and credit).
3. Fabric API's testmods and `FabricMC/fabric-docs` reference code come before anything written from scratch.

## Before writing any Minecraft code (B5 sourceVerified)

Minecraft renames things every release and memory from older versions is wrong more often than right. Target is **26.3**, the latest non-snapshot release (B10 alwaysLatest): unobfuscated, Mojang names, no Yarn, no remapping.

1. Read the relevant file in `docs/knowledge/` (index: `docs/knowledge/README.md`).
2. Check every class and method you use against the decompiled source. Run `./gradlew :<mod>:genSources` once per Minecraft version, then `python3 tools/mcsrc.py` to unpack it into `build/mcsrc/` (common, client, vanilla data/assets) for grepping. Fabric API source: clone `FabricMC/fabric-api` at the branch named after the Minecraft version. Its testmods are the best examples.
3. When the docs and the source disagree, the source wins; fix the doc in the same change.

## Layout (B4 oneRepo)

- `mods/<name>/`: one Fabric mod per folder, auto-included by `settings.gradle`. Shared build logic in `gradle/fabric-mod.gradle`; toolchain versions in root `gradle.properties`. New mod: `python3 tools/new-mod.py <modid> "<Display Name>"`.
- `mods/<name>/src/gametest/`: game tests (separate source set and fabric.mod.json).
- `datapacks/<name>/`, `resourcepacks/<name>/`: one pack per folder with `pack.mcmeta`, optional `VERSION`, PackTest tests in `data/<ns>/test/*.mcfunction`, and optional `packtest.commands` for the vanilla load check.
- `player-docs/`: Obsidian vault of **player-facing** docs, separate from the tree and code (B11 playerVault, rules in `specs/player-docs.md`). Any change players would notice updates the project's page there in the same change; no `dte:` tokens or code detail inside it.
- `docs/knowledge/`: the reference knowledge base. `decisions/`: the tree. `vendor/dte/`: DTE rule text, do not edit.
- `mods/refmod`, `datapacks/refpack`: internal reference projects that prove the toolchain. Never published.

## Commands

```
export JAVA_HOME=/opt/jdk25                      # Java 25 is required for 26.x
./gradlew :<mod>:build                           # compile + server game tests
./gradlew :<mod>:runClientGameTest               # client game tests (see container notes)
./gradlew build                                  # every mod
python3 tools/pack.py check|build [name]         # static checks / zip packs into build/packs/
python3 tools/packload.py [name]                 # load datapacks on the vanilla server (no mods)
python3 tools/packtest.py [name]                 # run PackTest tests on a Fabric server (third-party jars: needs owner OK to run locally)
```

## New Minecraft release (B10 alwaysLatest)

The repo always targets the latest non-snapshot release and moves promptly when one comes out, even if some things have to be switched off for a while. On a new release:

1. Bump `minecraft_version`, `loader_version`, `fabric_api_version` (and Loom if needed) in root `gradle.properties` from https://fabricmc.net/develop, and the pack formats in each `pack.mcmeta` from the new jar's `version.json`.
2. `./gradlew :refmod:genSources && python3 tools/mcsrc.py`, then read the Fabric blog post and fabric-docs `develop/porting/` for the release; update `docs/knowledge/porting-26x.md` and any note that names a changed API.
3. Port `refmod` and `refpack` first (they prove the notes), then every project.
4. A project that can't move yet (its library has no build, or the port is big) is **disabled, not left behind**: `disabled=<reason>` in the mod's `gradle.properties`, or a `DISABLED` file with the reason in a pack folder. Inside a mod, a feature that depends on a missing optional library is switched off behind `FabricLoader.getInstance().isModLoaded(...)`. Tell the owner what's disabled and why; re-enable as soon as the blocker clears.
5. The player docs vault (B11 playerVault) says which projects are on the new version and which are paused.

## Quality bar (B6 gametestGate)

Every mod has server game tests for its main behavior; anything visual gets a client game test with a screenshot you look at before saying it works. Every datapack has PackTest test functions (misode's PackTest, see `docs/knowledge/datapacks.md`) covering its behavior, loads clean with `tools/packload.py`, and passes `tools/packtest.py` in CI. A bug fix ships with the test that would have caught it.

## Releasing (B14 modrinthAiCompliant, B7 tagPublishes)

Releases go out from CI on a tag `<project>/v<version>`. A project goes to **Modrinth** only when it meets Modrinth's generative-AI rules (`docs/knowledge/publishing.md`): the "Contains AI-generated content" disclosure is always on, no icon, gallery or page image is AI-generated, and a person made a substantial part of the project (design, textures, models or code). Otherwise it is released on **GitHub Releases** until it qualifies. Before the first Modrinth release of a project, say to the owner which human-made parts carry it over the bar. Never make AI-generated images for a project page.

## Container notes (cloud sessions)

- JDK 25: Temurin from api.adoptium.net into `/opt/jdk25` if missing.
- Maven Central rate-limits the cloud IP; `~/.gradle/init.d/central-mirror.gradle` rewrites it to Google's Central mirror. Recreate it from `tools/container-setup.sh` if missing.
- Client game tests: Xvfb's GLX visual doesn't suit Minecraft's OpenGL backend, so the game falls back to its Vulkan backend. That needs `mesa-vulkan-drivers` and `libvulkan1`. Run `xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render -noreset" ./gradlew :<mod>:runClientGameTest`. Screenshots land in `mods/<mod>/build/run/clientGameTest/screenshots/`.
- `tools/container-setup.sh` does all of the above.

## Style

- Java: tabs, Mojang names, one class per registered concept group (`ModItems`, `ModBlocks`...). Mod id lowercase with underscores; author `bubba8587`, Java packages `io.github.bubba8587.<modid>` (B13 ownerNamespace); license MIT (B12 mitLicense).
- Commit and push as you go on the session's branch. Messages say what changed and which leaf governs it.
