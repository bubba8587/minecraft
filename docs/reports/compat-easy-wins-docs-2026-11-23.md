# Compat easy wins: docs report (2026-11-23)

Builder report for the orchestrator to merge into `specs/compat.md`, `specs/quality.md`, the mod specs and the player docs, then delete. Governing leaf: B20 compatList.

## specs/compat.md status changes

| Row | New status | Note for the table |
|---|---|---|
| Mod Menu | ok (not yet seen in game) | Every mod has name, player description, author, MIT, GitHub links (homepage, sources, issues) and groups under one "Bubba's Mods" parent. No icons (B14). Config screens later. |
| Explorer's Compass | ok | It names a structure by the lang key `structure.<ns>.<path>` (falling back to the capitalised path) and its "source" column by the mod's `fabric.mod.json` name. All five of our structures now have names. It hides structures tagged `c:hidden_from_locator_selection`. |
| Jade / WTHIT | ok | Every registered item, block, entity, effect, creative tab, enchantment, structure and item tag has an en_us name; a shared core game test keeps it that way. |
| Fabric convention tags (`c:`) | ok | See "c: tags" below. |
| EMI / JEI / REI | test | Vanilla-type recipes show by themselves. Our three special recipes probably don't (see below); needs a run with a viewer loaded, then maybe a small plugin. |

Add to "Open": **Explorer's Compass and Relic Vaults (owner's call).** Explorer's Compass lists every structure, so it can point straight at a Relic Vault, skipping the relic map shred hunt. Tagging `relic_vault` as `c:hidden_from_locator_selection` (a Fabric convention tag) hides it from Explorer's Compass and other locator mods. Same question for crypt yards, monster camps and trader posts, which have their own explorer maps. Not done: it's a design call.

## Per mod

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

## Recipe viewers (EMI / JEI / REI)

None is in the owner's profile, so nothing could be run. From reading our recipes:

- `bubbas_totems_artifacts:relic_shreds` (`RelicShredRecipe`, a `CustomRecipe`: combining relic map shreds) is a special crafting recipe. Recipe viewers generally skip special recipes, so it won't show.
- `bubbas_infused_gems:socket` (`SocketRecipe`, smithing with no template, `isSpecial` true, output worked out from the inputs) probably shows badly or not at all.
- `bubbas_infused_gems:invert_gem` (`InvertGemRecipe`, a 26.3 `BrewingRecipe` whose result depends on the gem) probably shows a gem going to a gem.
- **Later:** run with EMI (and JEI) loaded. If they're missing, add a small soft-dependency plugin per viewer, or EMI's info pages, showing an example. That's more than an easy win without the viewer APIs at hand.

## Spec facts (specs/quality.md, "Test suites")

- **Shared core test `NamesGameTest`** (`gradle/shared-gametest/java/io/github/bubba8587/shared/test/NamesGameTest.java`).
  - `gradle/fabric-mod.gradle` adds it to the gametest source set and core `fabric-gametest` entrypoints of every published mod (no `internal=true`), so it runs in every mod's core suite with no per-mod setup.
  - It fails when an item, block, entity type, mob effect, creative tab, enchantment, structure (`structure.<ns>.<path>`) or item tag (`tag.item.<ns>.<path>`, the Fabric convention that EMI and others use) in a loaded `bubbas_*` namespace has no en_us entry. It reads the server's language table, which Fabric fills with every mod's `en_us.json`.
  - Its first run found 11 missing names (structures and item tags, listed above); all are fixed now.
- Mobs' lang is generated: add names in `ModDataGenerator.EnglishLang` and run `./gradlew :bubbas_reworked_mobs:runDatagen`. Datagen rewrites every generated file with LF line endings, so commit only the files whose content changed.

## Player docs

Nothing new for players to learn about our own mods. Possible line on each project page under compatibility: "Shows up grouped as Bubba's Mods in Mod Menu; structures have proper names in Explorer's Compass."
