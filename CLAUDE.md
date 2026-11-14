<!-- dte:A2,B1,B3,B4,B5,B6,B7 -->
# Working in this repo

A home for a series of Minecraft mods, datapacks and resource packs, made with Fabric and released on Modrinth. The owner is technically minded but not a programmer: explain in plain language and hand over things they can run or look at (A3 ownerSteersByPlay).

## Decision tree (read first)

This repo keeps its decisions as a Decision Tree Engineering tree. Follow `vendor/dte/PROTOCOL.md` (rules in `vendor/dte/SPEC.md`). The tool is `python3 tools/dte.py`.

- **Your ring until the owner says otherwise: B.** The owner holds A. Subagents get C or deeper.
- **Rings here:** `A` the project's goals (owner's). `B` repo-wide calls: platform, versions, layout, quality bar, release flow. `C` per-project calls (one mod or pack's design choices worth asking the owner about). Deeper rings for sub-features if a project needs them.
- Start each session with `python3 tools/dte.py outbox` and `python3 tools/dte.py inbox`. Open inbox questions block what they say they block (publishing, notably).
- Finish with `DTE_RING=B python3 tools/dte.py validate --as B`.

## Before writing any Minecraft code (B5 sourceVerified)

Minecraft renames things every release and memory from older versions is wrong more often than right. Target is **26.3** (B3 latestRelease): unobfuscated, Mojang names, no Yarn, no remapping.

1. Read the relevant file in `docs/knowledge/` (index: `docs/knowledge/README.md`).
2. Check every class and method you use against the decompiled source. Run `./gradlew :<mod>:genSources` once per Minecraft version, then `python3 tools/mcsrc.py` to unpack it into `build/mcsrc/` (common, client, vanilla data/assets) for grepping. Fabric API source: clone `FabricMC/fabric-api` at the branch named after the Minecraft version. Its testmods are the best examples.
3. When the docs and the source disagree, the source wins; fix the doc in the same change.

## Layout (B4 oneRepo)

- `mods/<name>/`: one Fabric mod per folder, auto-included by `settings.gradle`. Shared build logic in `gradle/fabric-mod.gradle`; toolchain versions in root `gradle.properties`. New mod: `python3 tools/new-mod.py <modid> "<Display Name>"`.
- `mods/<name>/src/gametest/`: game tests (separate source set and fabric.mod.json).
- `datapacks/<name>/`, `resourcepacks/<name>/`: one pack per folder with `pack.mcmeta`, optional `VERSION` and `packtest.commands`.
- `docs/knowledge/`: the reference knowledge base. `decisions/`: the tree. `vendor/dte/`: DTE rule text, do not edit.
- `mods/refmod`, `datapacks/refpack`: internal reference projects that prove the toolchain. Never published.

## Commands

```
export JAVA_HOME=/opt/jdk25                      # Java 25 is required for 26.x
./gradlew :<mod>:build                           # compile + server game tests
./gradlew :<mod>:runClientGameTest               # client game tests (see container notes)
./gradlew build                                  # every mod
python3 tools/pack.py check|build [name]         # static checks / zip packs into build/packs/
python3 tools/packtest.py [name]                 # load datapacks on a real headless server
```

## Quality bar (B6 gametestGate)

Every mod has server game tests for its main behavior; anything visual gets a client game test with a screenshot you look at before saying it works. Every datapack has a `packtest.commands` exercising its functions and passes `tools/packtest.py`. A bug fix ships with the test that would have caught it.

## Releasing (B2 modrinthReleases, B7 tagPublishes)

Releases go out from CI on a tag `<project>/v<version>`. Nothing is published while the inbox items `license`, `namespace` and `modrinth-ai-rule` are open. Never make AI-generated images (icons, gallery, textures) for a Modrinth page: Modrinth's rules ban them (see `docs/knowledge/publishing.md`).

## Container notes (cloud sessions)

- JDK 25: Temurin from api.adoptium.net into `/opt/jdk25` if missing.
- Maven Central rate-limits the cloud IP; `~/.gradle/init.d/central-mirror.gradle` rewrites it to Google's Central mirror. Recreate it from `tools/container-setup.sh` if missing.
- Client game tests: Xvfb's GLX visual doesn't suit Minecraft's OpenGL backend, so the game falls back to its Vulkan backend. That needs `mesa-vulkan-drivers` and `libvulkan1`. Run `xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render -noreset" ./gradlew :<mod>:runClientGameTest`. Screenshots land in `mods/<mod>/build/run/clientGameTest/screenshots/`.
- `tools/container-setup.sh` does all of the above.

## Style

- Java: tabs, Mojang names, one class per registered concept group (`ModItems`, `ModBlocks`...). Mod id lowercase with underscores; Java packages `io.github.bubba8587.<modid>` until the `namespace` inbox item is answered.
- Commit and push as you go on the session's branch. Messages say what changed and which leaf governs it.
