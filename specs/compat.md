<!-- dte:B20 -->
# Compatibility with other mods

Our mods should work alongside the mods below (B20 compatList). The list is fluid: add, drop or re-rate a mod whenever the owner's setup or player habits change. Easy wins come first; anything deeper is tested with the mod loaded before a release of the project it touches.

Status words: **ok** (checked), **easy win** (small change, queued or done), **test** (needs a run with the mod loaded), **later**, **n/a** (nothing to do).

## The owner's mods (Modrinth profile "porp", 26.3)

| Mod | What it means for us | Status |
|---|---|---|
| Classic Faithful 32x Jappa (resource pack) | Our 32x art matches its style (B16). Older 32x art in Faithful style gets restyled. | restyle over time |
| Mod Menu | Every mod has a name, player description, author, MIT licence and GitHub links, grouped under one "Bubba's Mods" parent; no icons yet (B14); config screens later. | ok (not yet seen in game) |
| Explorer's Compass | Dropped from the owner's setup (2026-11-23: the Totem of Seeking does its job). Our structures keep their names (`structure.<ns>.<path>`), which other locator mods read too. | n/a |
| Cristel Lib | Structure-spacing config library (used by Towns & Towers): could let players tune how often our camps, crypt yards, trader posts and relic vaults appear. | later |
| Lithium | Rewrites vanilla code some of our mixins hook (spawning, pathfinding, entity lookups): the most likely real break. | test |
| Sodium | Rendering: our custom renderers (spun webs, cocoons, gravestone text, statues, meters, beacon beams) must draw correctly. | test |
| Iris (shaders) | Custom rendering often breaks under shaders. | test |
| Voxy | Far-distance view: our code-built structures should appear in it. Voxy 0.2.20-beta crashed on world join in "mod testing" (2026-11-23): its `sodium.MixinDefaultChunkRenderer` found no target in Sodium 0.9.3 (a Voxy/Sodium version mismatch, none of our mods in the trace). Owner playtests without it for now. | blocked (Voxy vs Sodium) |
| Tectonic, Lithostitched | Taller, steeper terrain: our structures read ground height and skip steep ground, so they may be rarer or misplaced. | test |
| Towns & Towers, Explorify | More structures: ours could land on theirs. | test |
| Xaero's Minimap and World Map | Our helper entities (webs, cocoons, displays) may show as radar dots. | later |
| Continuity, Visuality, AmbientSounds | Visual or sound only. | n/a |
| Collective, CreativeCore, Cloth Config, Placeholder API, Silk, Fabric Language Kotlin, Villager Names, Status Effect Bars | Libraries or unrelated. | n/a |

## Widely used mods worth planning for

| Mod | What it means for us | Status |
|---|---|---|
| Jade / WTHIT | Every item, block, entity, effect, creative tab, enchantment, structure and item tag has an en_us name; the shared `NamesGameTest` keeps it so. | ok |
| EMI / JEI / REI | Vanilla-type recipes show on their own; our three special recipes (relic shreds, gem socketing, gem inversion) probably don't (see below). | test |
| Fabric convention tags (`c:`) | Tagged where a real tag fits (below). Infused Gems' gems are deliberately not `c:gems` (that would let other mods' recipes take them as diamonds). | ok |
| Distant Horizons | Like Voxy: structures in the far view. | test |
| FerriteCore, ModernFix, ImmediatelyFast, Entity Culling | Performance mods; Entity Culling can hide our block-entity renderers wrongly. | test |
| Terralith, YUNG's structures | Biome and structure mods: our biome tags and structure placement must cope with modded biomes. | test |
| JourneyMap | Like Xaero's. | later |
| AppleSkin | Food HUD; nothing of ours changes food. | n/a |

## What the easy wins did (2026-11-23)

All seven published mods (`bubbas_extra_recipes`, `bubbas_infused_gems`, `bubbas_reworked_beacons`, `bubbas_reworked_mining`, `bubbas_reworked_mobs`, `bubbas_totems_artifacts`, `bubbas_world_tweaks`):

- `fabric.mod.json`: `contact` (homepage and sources `https://github.com/bubba8587/minecraft`, issues `.../issues`) and `custom.modmenu.parent` = `{ id: "bubbas_mods", name: "Bubba's Mods", description: ... }`. This is Mod Menu's "dummy parent" form: every mod carries the same definition, so the group shows whichever of our mods are installed. No badges (none fit: `library` and `deprecated` don't apply). Mod Menu is not in `depends` or `suggests`.
- New player descriptions (shown in Mod Menu):
  - Reworked Mining (was empty): "Mining gets a real progression curve: early tools are slow, deepslate is tough, and Efficiency and Unbreaking matter much more."
  - World Tweaks (was empty): "Small world touches: magma under lava makes bubbling lava and smoking lava geysers, and geysers lift you into the air."
  - Reworked Mobs (was "Part 1: undead lie dormant as blocks"): "Reworks Minecraft's mobs: dormant undead, giant spiders and nests, watcher creepers, cairn endermen, grabbing drowned, monster camps and more."
  - Others unchanged.
- Note: Reworked Mining, Reworked Mobs, Totems & Artifacts and World Tweaks name an `icon` file that doesn't exist yet (`assets/<modid>/icon.png`). Mod Menu shows its default icon. Left alone: that path is where a person-made icon goes (B14, `docs/art-todo.md`).

Names added:

- **Reworked Mobs** (through the datagen lang provider, `ModDataGenerator.EnglishLang`): structures Monster Camp, Crypt Yard, Nether Outpost, Trader Post; item tags `preferred_on_wake` "Weapons Woken Undead Prefer", `sleeper_harvest_tools` "Sleeper Harvest Tools". The Ancient Fabricator is a feature, not a structure, so Explorer's Compass can't list it.
- **Totems & Artifacts**: structure Relic Vault (one line at the top of `lang/en_us.json`).
- **Infused Gems**: item tags `socketable` "Socketable Gear", `socketable_armor` "Socketable Armor", `socketable_weapons` "Socketable Weapons", `not_socketable` "Not Socketable".
- Every entity and block already had a name (giant spider, nest spider, spider nest, spun web, silk cocoon, woken undead, sleepers, loot display, gravestone, totem statue, lava geyser).

`c:` tags added (only existing Fabric convention tags):

- **Reworked Mining**: `c:tools/mining_tool` += quartz pickaxe; `c:tools/melee_weapon` += quartz sword and axe (Fabric lists vanilla pickaxes, swords and axes there by name, not by tag, so ours had to be added). Quartz armor was already covered: `c:armors/humanoid` takes vanilla's armor slot tags, which the mod already fills.
- **Reworked Mobs**: `c:capturing_not_supported` and `c:teleporting_not_supported` += spider nest, spun web, silk cocoon (helper entities that mob-catching or teleporting mods shouldn't pick up).
- **World Tweaks**: the same two tags += lava geyser (a helper entity).
- Not tagged, and why:
  - Infused Gems' gems aren't `c:gems`: that tag means crafting-material gems (diamond, emerald), and other mods' recipes would then take a socketing gem as a diamond-like material.
  - Extra Recipes' Synthetic Diamond Ore is vanilla diamond ore, which already carries `c:ores/diamond`.
  - Totems: nothing fits.
  - No block `c:relocation_not_supported`: none of our blocks is multi-block.

### Recipe viewers

None is in the owner's profile, so nothing could be run. From reading our recipes:

- `bubbas_totems_artifacts:relic_shreds` (`RelicShredRecipe`, a `CustomRecipe`: combining relic map shreds) is a special crafting recipe. Recipe viewers generally skip special recipes, so it won't show.
- `bubbas_infused_gems:socket` (`SocketRecipe`, smithing with no template, `isSpecial` true, output worked out from the inputs) probably shows badly or not at all.
- `bubbas_infused_gems:invert_gem` (`InvertGemRecipe`, a 26.3 `BrewingRecipe` whose result depends on the gem) probably shows a gem going to a gem.
- **Later:** run with EMI (and JEI) loaded. If they're missing, add a small soft-dependency plugin per viewer, or EMI's info pages, showing an example. That's more than an easy win without the viewer APIs at hand.

## Open

- **Locator mods and our structures:** any locator mod could point straight at a Relic Vault (skipping the relic map hunt) or at crypt yards, camps and trader posts. Tagging a structure `c:hidden_from_locator_selection` hides it. Not urgent now that the owner doesn't use Explorer's Compass; decide before a release.

- A compatibility test setup: an option that loads chosen Modrinth mods into our game test runs (as Stormie's Spiders is loaded today), so the "test" rows above get real runs and screenshots.
