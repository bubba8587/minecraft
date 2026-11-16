<!-- dte:A2,B1,A4,A1,B9,B10,B11,B12,B14,B15,B16 -->
# Working in this repo

A home for a series of Minecraft mods, datapacks and resource packs, made with Fabric and released on Modrinth. The owner is technically minded but not a programmer: explain in plain language and hand over things they can run or look at (A3 ownerSteersByPlay).

## Decision tree (read first)

This repo keeps its decisions as a Decision Tree Engineering tree. Follow `vendor/dte/PROTOCOL.md` (rules in `vendor/dte/SPEC.md`). The tool is `python3 tools/dte.py`.

### Use the tree sparingly (the owner's standing instruction, and very important to them)

**The tree is for decisions with stakes: critical, product-direction-altering calls that must be preserved through all the tweaking around them. Everything that describes how a system works, or how something is built, is a spec.**

- Most work creates **no leaf at all**. Technical choices go in the relevant spec under `specs/` and can change whenever a better or faster way is found.
- The owner will usually say when something should be a leaf. When they haven't, the default is spec.
- The test: would flipping it change what the project *is* or *is for*, for players or for the owner? A leaf. Would it only change how the code does it? A spec, even if the choice is significant. Example from the owner: "are static undead a real block or not" totally depends on what the best, most performant code system is. Switching it is technical, so it's a spec (`specs/undead.md`), not a leaf.
- Don't record the owner's design bullets one leaf per bullet. A project gets one leaf for its direction (e.g. C1 reworkedMobs); its details live in its spec.
- When in doubt, write the spec and mention in chat that it could be a leaf if the owner wants it preserved.

### Mechanics

- **Your ring until the owner says otherwise: B.** The owner holds A. Subagents get C or deeper.
- **Rings here:** `A` the project's goals (owner's). `B` repo-wide product calls (platform, target version, licensing, release channel, design rules the owner sets). `C` one leaf per project for its direction. Deeper rings only for a project-direction call the owner wants preserved.
- Start each session with `python3 tools/dte.py outbox` and `python3 tools/dte.py inbox`.
- Turning a leaf that turned out to be mechanics into spec text: `python3 tools/dte.py to-spec <ID> specs/<file>.md --by <you>` (a human-held leaf also needs the owner's go-ahead).
- Finish with `DTE_RING=B python3 tools/dte.py validate --as B`.

## Before designing anything: prior art (B9 libsOverHandroll; how: `specs/prior-art.md`)

**Reminder from the owner (B9 libsOverHandroll): use outside libraries and mods aggressively; hand-roll only what's necessary for a well-designed, performant mod.** Hand-rolling is justified when no library does it, the library has no build for our Minecraft version, or it would cost real performance or design quality. Say which of those applies when you hand-roll something a library could have done. Every dependency goes in `fabric.mod.json` `depends` and in the Modrinth version's dependency list.

**Few new blocks (B15 fewNewBlocks):** build features from custom items, variants of existing blocks and deeper existing systems. Custom textures and models on existing blocks are fine. A new registered block needs a stated reason, given to the owner before building it. Techniques: `docs/knowledge/no-new-blocks.md`.

**Art during development (owner, 2026-11-16):** agents may make any in-game art they need (scripted or AI-made), and **give every texture and model their best shot** (owner's standing rule: real, vanilla-fitting art, not bare placeholders: shading, readable silhouettes, proper 3D models where a block or held item calls for one, checked in screenshots at 16x and 32x), but every agent-made asset gets a row in `docs/art-todo.md` so the owner can redo or tweak it later. Mod icons and project page images still must never be AI-made (B14); they stay on that list until a person makes them.

**Textures at 16x and 32x (B16 textures16and32):** every custom texture ships at 16x in vanilla style and 32x in Faithful 32x style; the 32x set is a built-in resource pack in the jar. Layout, credit rules and the checker (`python3 tools/texcheck.py`) are in `specs/textures.md`; the scaffolder sets it up.

Lots of mods already exist. Before designing a mod, pack or tool:

1. `python3 tools/prior-art.py "<idea>" [--type mod|datapack|resourcepack]` and a GitHub search. If something already does most of it, tell the owner before building and say what ours would add.
2. Use an existing library or tool instead of writing one (`docs/knowledge/libraries.md`), and check `docs/knowledge/reference-mods.md` for open-source mods that solve the same problem: read how they did it (mind their license; learning from code is fine, copying needs a compatible license and credit).
3. Fabric API's testmods and `FabricMC/fabric-docs` reference code come before anything written from scratch.

## Before writing any Minecraft code (`specs/knowledge-base.md`)

Minecraft renames things every release and memory from older versions is wrong more often than right. Target is **26.3**, the latest non-snapshot release (B10 alwaysLatest): unobfuscated, Mojang names, no Yarn, no remapping.

1. Read the relevant file in `docs/knowledge/` (index: `docs/knowledge/README.md`).
2. Check every class and method you use against the decompiled source. Run `./gradlew :<mod>:genSources` once per Minecraft version, then `python3 tools/mcsrc.py` to unpack it into `build/mcsrc/` (common, client, vanilla data/assets) for grepping. Fabric API source: clone `FabricMC/fabric-api` at the branch named after the Minecraft version. Its testmods are the best examples.
3. When the docs and the source disagree, the source wins; fix the doc in the same change.

## Layout (`specs/build.md`)

- `mods/<name>/`: one Fabric mod per folder, auto-included by `settings.gradle`. Shared build logic in `gradle/fabric-mod.gradle`; toolchain versions in root `gradle.properties`. New mod: `python3 tools/new-mod.py <modid> "<Display Name>"`.
- `mods/<name>/src/gametest/`: game tests (separate source set and fabric.mod.json).
- `datapacks/<name>/`, `resourcepacks/<name>/`: one pack per folder with `pack.mcmeta`, optional `VERSION`, PackTest tests in `data/<ns>/test/*.mcfunction`, and optional `packtest.commands` for the vanilla load check.
- `player-docs/`: Obsidian vault of **player-facing** docs, separate from the tree and code (B11 playerVault, rules in `specs/player-docs.md`). Any change players would notice ends up on the project's page there; no `dte:` tokens or code detail inside it.
- **Parallel builders work in their own git worktrees (owner's instruction).** When more than one agent builds at a time, each gets an isolated worktree (the Agent tool's `isolation: "worktree"`), commits there, and the orchestrator merges finished branches into the main checkout, verifies, then pushes and installs from the main checkout only. Worktrees don't contain gitignored folders: read the decompiled Minecraft source from the main checkout's `build/mcsrc/`, and never run the install script or the test world build from a worktree. **Clean up (owner):** once a worktree's branch is merged (or abandoned), remove the worktree and delete its branch (`git worktree remove <path>`, `git branch -d <branch>`, `git worktree prune`); worktrees hold full build folders and fill the disk otherwise. Check `git worktree list` at the end of each batch.
- **Builders build; docs are merged separately (owner's instruction).** A builder agent doesn't spend time on live player docs or spec prose: it writes what the docs need (player-facing changes, spec facts, numbers, owner decisions, its own choices) into `docs/reports/<project>-docs-<date>.md` and commits that. The orchestrator or a docs agent merges reports into `specs/` and `player-docs/`, then deletes merged reports. Exceptions a builder does write itself: API contracts other builders need, `covers:` lines, and generated data/reports (e.g. `tools/gem-balance.py` output).
- `BACKLOG.md`: parked ideas and open items. Add to it whenever the owner parks something.
- `docs/knowledge/`: the reference knowledge base. `decisions/`: the tree. `vendor/dte/`: DTE rule text, do not edit.
- `mods/refmod`, `datapacks/refpack`: internal reference projects that prove the toolchain. Never published.

## Commands

```
export JAVA_HOME=/opt/jdk25                      # Java 25 is required for 26.x
./gradlew :<mod>:build                           # compile + core server game tests (the default loop)
./gradlew :<mod>:build -PmilestoneTests          # + every milestone server game test (full server suite)
./gradlew :<mod>:runClientGameTest -PclientTests=<names>  # only the client test parts you changed (see container notes)
./gradlew :<mod>:build :<mod>:runClientGameTest -PmilestoneTests  # full: release milestone (all server + client tests)
./gradlew build                                  # every mod, core suites
./gradlew :packcheck:build                       # datapack recipe/content game tests
python3 tools/pack.py check|build [name]         # static checks / zip packs into build/packs/
python3 tools/texcheck.py [project]              # every 16x texture has a 32x twin (B16)
python3 tools/packload.py [name]                 # load datapacks on the vanilla server (no mods)
python3 tools/packtest.py [name]                 # run PackTest tests on a Fabric server (owner approved)
pwsh tools/install-to-modrinth.ps1               # owner's Windows PC: build + install into Modrinth profile "mod testing"
python tools/build-testworld.py                  # rebuild the pristine test world
./gradlew :testworldshots:runClientGameTest -PmilestoneTests  # screenshot every test world station, all mods (~70 s)
pwsh tools/add-modrinth-mod.ps1 <slug>           # owner's PC: add an outside Modrinth mod (+ required deps) to "mod testing" only
```

## New Minecraft release (B10 alwaysLatest)

The repo always targets the latest non-snapshot release and moves promptly when one comes out, even if some things have to be switched off for a while. On a new release:

1. Bump `minecraft_version`, `loader_version`, `fabric_api_version` (and Loom if needed) in root `gradle.properties` from https://fabricmc.net/develop, and the pack formats in each `pack.mcmeta` from the new jar's `version.json`.
2. `./gradlew :refmod:genSources && python3 tools/mcsrc.py`, then read the Fabric blog post and fabric-docs `develop/porting/` for the release; update `docs/knowledge/porting-26x.md` and any note that names a changed API.
3. Port `refmod` and `refpack` first (they prove the notes), then every project.
   Also read the release's changelog for anything that reopens a workaround: data-driven blocks, or blocks carrying item data without a block entity, reopen B15 fewNewBlocks' exceptions and every use of the added-property technique in `docs/knowledge/item-keyed-block-drops.md` (move them to pure data and merge back toward vanilla). Tell the owner when one reopens.
4. A project that can't move yet (its library has no build, or the port is big) is **disabled, not left behind**: `disabled=<reason>` in the mod's `gradle.properties`, or a `DISABLED` file with the reason in a pack folder. Inside a mod, a feature that depends on a missing optional library is switched off behind `FabricLoader.getInstance().isModLoaded(...)`. Tell the owner what's disabled and why; re-enable as soon as the blocker clears.
5. The player docs vault (B11 playerVault) says which projects are on the new version and which are paused.

## Speed of the build and test loop (owner's standing instruction)

Agents do all building and testing here; the owner never touches it. So keeping the suite and harness fast is the agent's job, **without being asked**: always be looking for ways to speed up development (building, testing, tooling), measure, fix, and record what changed.

**The goal is development at the speed of the owner's thought (owner's standing instruction).** Every step between an idea and the owner playing it is a cost: building, testing, docs, installing, waiting on answers. Keep looking for ways to shrink that loop (parallel work that doesn't collide, generated content with names from one source, scripts instead of hand edits, fewer round trips for questions), and fix the friction you notice even when nobody asked.

- **Test-lite (owner's standing instruction, `specs/quality.md` "Test suites").** Each mod has a small **core** server game test suite (`<Mod>CoreGameTest`, a smoke test per main mechanic) that runs on every build and push; every other test is **milestone**: run only with `-PmilestoneTests`, on release tags and manual CI runs. Client game tests and the test world tour are milestone too (`runClientGameTest` skips itself without `-PclientTests` or `-PmilestoneTests`). For work you're confident in, write the test objective in the spec's "Milestone test objectives" section instead of a test; milestone test code is optional until a release.
- Build and test only what you touched: `./gradlew :<mod>:build`, not `./gradlew build`. Run client game tests only when something visible changed, and only the parts you need: `-PclientTests=talisman,gait` (names in each mod's `ClientTests` helper; Reworked Mobs: sleepers, displays, gait, creepers; Totems & Artifacts: talisman, warding; Reworked Beacons: beacons).
- Configuration cache is on, and server game tests skip themselves when a mod's code and resources match the last passing run of that suite (a full pass covers core; a core pass never counts as a full one) (`gradle/fabric-mod.gradle`). `-PrerunGametests` forces them. Don't undo either without measuring.
- When a step feels slow, time it, find the cause, fix it, and update the timings table in `specs/quality.md`. Mention speedups (and slowdowns) to the owner in a line.
- Slow things worth attacking next are listed in `specs/quality.md` under Speed.
- **Don't fill the owner's disk (owner's instruction).** Every run starts clean: screenshot folders are wiped per run, and `gradle/fabric-mod.gradle` deletes the game test worlds (`build/run/gameTest/world`, client `saves/`) before each run, since they otherwise grow every run. Delete your own copies of screenshots once you've looked at them, and never leave per-run output (worlds, logs, copies) accumulating anywhere.

## Quality bar (`specs/quality.md`)

Every mod has server game tests for its main behavior: a core suite that runs on every build, and a milestone suite plus client tests run at release milestones (test-lite, `specs/quality.md` "Test suites"). Each mod's spec lists its milestone test objectives, one line per milestone class.

**Release milestone:** before tagging `<project>/v<version>`, run everything for that project (`./gradlew :<mod>:build :<mod>:runClientGameTest -PmilestoneTests`, the test world tour if the release changes what it shows, and the pack checks), write tests for objectives the release depends on that have none yet, and fix whatever fails before tagging. CI runs the full suite again on the tag.

**Check yourself visually as you go (owner's instruction).** Every time something visible is added or changed (blocks, models, poses, mobs, items and icons, GUI, HUD, particles, textures at both 16x and 32x), add or update a client game test screenshot of it and look at the PNG (Read tool) before moving on, not just at the end. Use more than one angle when it matters, get close enough to see details, and fix what looks wrong (floating, clipping, wrong facing, missing or purple-black textures, offsets) before continuing. Subagents get the same instruction and list their screenshots in their report; the orchestrator looks at them too before telling the owner something works. Every datapack has PackTest test functions (misode's PackTest, see `docs/knowledge/datapacks.md`) covering its behavior, loads clean with `tools/packload.py`, and passes `tools/packtest.py` in CI. A bug fix ships with a milestone test that would have caught it, or at least a written objective for one in the spec. The visual check above stays on every visible change, scoped to the client test parts that changed (`-PclientTests=<names>`).

## Releasing (B14 modrinthAiCompliant; how: `specs/releasing.md`)

Releases go out from CI on a tag `<project>/v<version>`. A project goes to **Modrinth** only when it meets Modrinth's generative-AI rules (`docs/knowledge/publishing.md`): the "Contains AI-generated content" disclosure is always on, no icon, gallery or page image is AI-generated, and a person made a substantial part of the project (design, textures, models or code). Otherwise it is released on **GitHub Releases** until it qualifies. Before the first Modrinth release of a project, say to the owner which human-made parts carry it over the bar. Never make AI-generated images for a project page.

## Container notes (cloud sessions)

- JDK 25: Temurin from api.adoptium.net into `/opt/jdk25` if missing.
- Maven Central rate-limits the cloud IP; `~/.gradle/init.d/central-mirror.gradle` rewrites it to Google's Central mirror. Recreate it from `tools/container-setup.sh` if missing.
- Client game tests: Xvfb's GLX visual doesn't suit Minecraft's OpenGL backend, so the game falls back to its Vulkan backend. That needs `mesa-vulkan-drivers` and `libvulkan1`. Run `xvfb-run -a -s "-screen 0 1280x720x24 +extension GLX +render -noreset" ./gradlew :<mod>:runClientGameTest`. Screenshots land in `mods/<mod>/build/run/clientGameTest/screenshots/`.
- `tools/container-setup.sh` does all of the above.

## Style

- Java: tabs, Mojang names, one class per registered concept group (`ModItems`, `ModBlocks`...). Mod id lowercase with underscores; author `bubba8587`, Java packages `io.github.bubba8587.<modid>` (`specs/build.md`); license MIT (B12 mitLicense).
- **On the owner's PC: commit locally as you go, but push only after the builds and the relevant tests pass** (owner's instruction), in verified batches; subagents never push, the orchestrator does after checking. In cloud sessions, commit and push as you go on the session's branch. Messages say what changed and which leaf governs it. A snapshot commit of unfinished work (an agent mid-task) ends its message with the skip-CI marker (`[skip ci]`; never write that text in other commit messages, or GitHub skips CI for them), so half-written code doesn't turn CI red; finished work never skips CI.
