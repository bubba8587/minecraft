<!-- dte:B5,B8 -->
# Knowledge base

Reference notes for building Fabric mods, datapacks and resource packs on **Minecraft 26.3** (Fabric Loader 0.20.1, Fabric API 0.166.2+26.3, Loom 1.19.4, Java 25, data pack format 121.0, resource pack format 97.1). Written 2026-11-14 by research agents working from the decompiled 26.3 source, Fabric API's 26.3 branch, vanilla data files and current docs. Claims carry `(src: ...)` pointers. `UNVERIFIED` marks the few that couldn't be checked.

These notes are a map, not the territory. Check names against the source before relying on them (B5 sourceVerified), and fix a note when the source disagrees.

## Start here

| If you're... | Read |
|---|---|
| New to 26.x or porting from 1.21 | `porting-26x.md` (breaking changes + Yarn to Mojang name cheat sheet) |
| Setting up a build, fabric.mod.json, class tweakers | `toolchain.md` |
| Changing vanilla behavior | `mixins.md` |
| Looking for prior art or a library | `reference-mods.md`, `libraries.md` (B8 priorArtFirst) |
| Wanting a block-like feature (B15 fewNewBlocks) | `no-new-blocks.md` (display entities, retextures, borrowed states, data on existing blocks, decision guide) |

## Mods

- `content.md`: registries, sounds, particles, effects, potions, enchantments, recipes, loot, tags, game rules, trades, what's data-driven only. Then `content-items-blocks.md` (items, data components, tools, blocks, block entities, fluids) and `content-entities.md` (entities, AI, spawning, projectiles).
- `systems.md`: index for `systems-serialization.md` (Codec, StreamCodec, ValueInput/Output), `systems-networking.md`, `systems-persistence.md` (attachments, SavedData), `systems-commands-text.md` (Brigadier, permissions, Components), `systems-events.md`, `systems-menus-transfer.md`, `systems-world.md` (dimensions, biomes, worldgen), `systems-resources-config-serverside.md` (resource loading, dynamic registries, config, server-side-only mods).
- `client.md`: key mappings, HUD, GUI drawing (`GuiGraphicsExtractor`), screens, client events, sounds, toasts. `client-rendering.md`: the 26.x renderer (OpenGL/Vulkan backends, extract vs submit, RenderPipeline, shaders, `LevelRenderEvents`), entity and block entity renderers, tints, particles, Sodium/Iris compatibility. `client-models.md`: blockstate, model and item-model-definition JSON, Model Loading API, FRAPI meshes. `client-gametest.md`: client game tests and the headless recipe.

## Packs

- `datapacks.md`: pack.mcmeta, layout, functions and macros, every command, predicates, loot, recipes, advancements, tests, tooling, headless testing.
- `datapack-registries.md`: JSON shape of every data-driven registry with vanilla examples.
- `resourcepacks.md`: models, item model definitions, blockstates, equipment, atlases, fonts, shaders, post effects, shader packs (Iris), tooling.

## Shipping

- `publishing.md`: Modrinth (project setup, API, **content rules incl. the generative-AI rule**), versioning, CI workflows, multi-version.
- `testing-debugging.md`: server and client game tests, unit tests, headless servers, logs, crash reports, profiling.

## Keeping this current

When the target Minecraft version changes: bump `gradle.properties`, run `./gradlew :refmod:genSources` and `python3 tools/mcsrc.py`, read the Fabric blog post for the release and the `develop/porting/` page in fabric-docs, then sweep `porting-26x.md` and every note that names a changed API. `mods/refmod` and `datapacks/refpack` must still build and pass their tests.
