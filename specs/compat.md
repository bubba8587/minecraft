<!-- dte:B20 -->
# Compatibility with other mods

Our mods should work alongside the mods below (B20 compatList). The list is fluid: add, drop or re-rate a mod whenever the owner's setup or player habits change. Easy wins come first; anything deeper is tested with the mod loaded before a release of the project it touches.

Status words: **ok** (checked), **easy win** (small change, queued or done), **test** (needs a run with the mod loaded), **later**, **n/a** (nothing to do).

## The owner's mods (Modrinth profile "porp", 26.3)

| Mod | What it means for us | Status |
|---|---|---|
| Classic Faithful 32x Jappa (resource pack) | Our 32x art matches its style (B16). Older 32x art in Faithful style gets restyled. | restyle over time |
| Mod Menu | Our mods should show with proper names, descriptions, links and grouped under one parent; config screens later. | easy win |
| Explorer's Compass | Finds structures by type: ours should show with readable names. | easy win |
| Cristel Lib | Structure-spacing config library (used by Towns & Towers): could let players tune how often our camps, crypt yards, trader posts and relic vaults appear. | later |
| Lithium | Rewrites vanilla code some of our mixins hook (spawning, pathfinding, entity lookups): the most likely real break. | test |
| Sodium | Rendering: our custom renderers (spun webs, cocoons, gravestone text, statues, meters, beacon beams) must draw correctly. | test |
| Iris (shaders) | Custom rendering often breaks under shaders. | test |
| Voxy | Far-distance view: our code-built structures should appear in it. | test |
| Tectonic, Lithostitched | Taller, steeper terrain: our structures read ground height and skip steep ground, so they may be rarer or misplaced. | test |
| Towns & Towers, Explorify | More structures: ours could land on theirs. | test |
| Xaero's Minimap and World Map | Our helper entities (webs, cocoons, displays) may show as radar dots. | later |
| Continuity, Visuality, AmbientSounds | Visual or sound only. | n/a |
| Collective, CreativeCore, Cloth Config, Placeholder API, Silk, Fabric Language Kotlin, Villager Names, Status Effect Bars | Libraries or unrelated. | n/a |

## Widely used mods worth planning for

| Mod | What it means for us | Status |
|---|---|---|
| Jade / WTHIT | Look-at tooltips: our blocks and entities need readable names (lang entries) so they don't show raw ids. | easy win |
| EMI / JEI / REI | Recipe viewers: vanilla recipe types show on their own; our special recipes (relic shred combining) may need a note or plugin. | easy win (check) |
| Fabric convention tags (`c:`) | Other mods find items and blocks by `c:` tags (e.g. gems, ores): tag ours where it fits. | easy win |
| Distant Horizons | Like Voxy: structures in the far view. | test |
| FerriteCore, ModernFix, ImmediatelyFast, Entity Culling | Performance mods; Entity Culling can hide our block-entity renderers wrongly. | test |
| Terralith, YUNG's structures | Biome and structure mods: our biome tags and structure placement must cope with modded biomes. | test |
| JourneyMap | Like Xaero's. | later |
| AppleSkin | Food HUD; nothing of ours changes food. | n/a |

## Open

- A compatibility test setup: an option that loads chosen Modrinth mods into our game test runs (as Stormie's Spiders is loaded today), so the "test" rows above get real runs and screenshots.
