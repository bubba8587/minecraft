<!-- dte:C2 -->
<!-- covers: datapacks/bubbas_crafting_tweaks/*, resourcepacks/bubbas_crafting_tweaks/* -->
# Bubba's Crafting Tweaks (spec)

The core recipe datapack. Owner-authored recipe set (originally for 26.1), ported to 26.3.

- Lives in its own namespace, `bubbas_crafting_tweaks`, so it never replaces a vanilla recipe by file name. The original pack used the `minecraft` namespace. Its `saddle.json` would have silently replaced vanilla's saddle recipe (added in 1.21.6); the owner chose vanilla's, so the pack has no saddle recipe.
- The original pack's elytra recipe moved out: it now makes the Glider in the gear mod (`specs/gear.md`).
- Bamboo Paper keeps its own name (owner's call) and carries a green tint color in `custom_model_data.colors[0]` (0xD8F0C0). The companion resource pack `resourcepacks/bubbas_crafting_tweaks` overrides vanilla's paper item definition with a `custom_model_data` tint whose default is white, so ordinary paper is unchanged, the tint works on top of any paper texture (16x or 32x), and players without the resource pack just see paper. No new texture files.
- Custom names and lore on results (`minecraft:item_name`, `minecraft:lore`) are plain-string text components.
- pack.mcmeta targets data pack format 121 (26.3). Bump with each Minecraft release.
- Tests: loads clean in `tools/packload.py`; each recipe is looked up, matched and assembled in `mods/packcheck` game tests, with a client screenshot of the results (`specs/pack-testing.md`).
- Player page: `player-docs/Projects/Bubba's Crafting Tweaks.md`, updated whenever a recipe changes.
