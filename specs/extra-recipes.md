<!-- dte:C2 -->
<!-- covers: datapacks/bubbas_extra_recipes/*, resourcepacks/bubbas_extra_recipes/* -->
# Bubba's Extra Recipes (spec)

The core recipe datapack (built: datapack 1.0.0, 11 recipes). Owner-authored recipe set (originally for 26.1), ported to 26.3.

- Lives in its own namespace, `bubbas_extra_recipes`, so it never replaces a vanilla recipe by file name. The original pack used the `minecraft` namespace. Its `saddle.json` would have silently replaced vanilla's saddle recipe (added in 1.21.6); the owner chose vanilla's, so the pack has no saddle recipe.
- The original pack's elytra recipe moved out: it now makes the Glider in the companion mod (`specs/extra-recipes-mod.md`).
- Bamboo Paper keeps its own name (owner's call) and carries a green tint color in `custom_model_data.colors[0]` (0xD8F0C0). The companion resource pack `resourcepacks/bubbas_extra_recipes` overrides vanilla's paper item definition with a `custom_model_data` tint whose default is white, so ordinary paper is unchanged, the tint works on top of any paper texture (16x or 32x), and players without the resource pack just see paper. No new texture files.
- Custom names and lore on results (`minecraft:item_name`, `minecraft:lore`) are plain-string text components.
- pack.mcmeta targets data pack format 121 (26.3). Bump with each Minecraft release.
- Tests: loads clean in `tools/packload.py`; each recipe is looked up, matched and assembled in `mods/packcheck` game tests, with a client screenshot of the results (`specs/pack-testing.md`).
- Player pages: `player-docs/Projects/Bubba's Extra Recipes/` (overview, Recipe List, The Glider, Synthetic Diamond Ore, Extra Recipes Companion Mod), updated whenever a recipe changes.

## Owner's review (2026-11-14)

- Bamboo Paper: keeps its name, light green tint (done).
- Charcoal Block: keeps its name.
- Andesite/diorite/granite → quartz (blasting): normal 200-tick cook time (was 100).
- Rotten flesh → leather (smoker): 0.1 XP (was 1.0); lore kept.
- Trident recipe: kept as is (tridents are too rare otherwise).
- Organic Glowstone Dust, black dye, sand, gravel, iron nugget: unchanged.
- Synthetic Diamond Ore: its result carries `minecraft:block_state {synthetic:"true"}`; with the companion mod placed ore uses its own loot table (25 % one diamond, no Fortune, no XP; `specs/extra-recipes-mod.md`). Without the mod the component is ignored and it mines like natural ore.
