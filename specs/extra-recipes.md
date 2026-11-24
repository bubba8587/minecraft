<!-- dte:C2 -->
<!-- covers: datapacks/bubbas_extra_recipes/*, resourcepacks/bubbas_extra_recipes/* -->
# Bubba's Extra Recipes (spec)

**Status: built** (datapack 1.1.0, 16 recipes). The core recipe datapack: the owner's own recipe set, on 26.3. Player pages: `player-docs/Projects/Bubba's Extra Recipes/` (overview, Recipe List, The Glider, Synthetic Diamond Ore, Extra Recipes Companion Mod), updated whenever a recipe changes.

## Rules

- **Own namespace,** `bubbas_extra_recipes`, so it never replaces a vanilla recipe by file name.
- **No saddle recipe:** vanilla has its own (since 1.21.6), and the owner chose vanilla's.
- **No elytra recipe:** that recipe makes the Glider in the companion mod (`specs/extra-recipes-mod.md`).
- **New recipes only:** every recipe was checked against vanilla 26.3 data; none has a vanilla recipe of the same type.
- **Names and lore** on results (`minecraft:item_name`, `minecraft:lore`) are plain-string text components.
- **Pack format:** `pack.mcmeta` targets data pack format 121 (26.3). Bump with each Minecraft release.

## Recipes

Block blasting recipes use 200 ticks and 0.1 XP unless the table says otherwise (the `blast_quartz` standard).

| File | Type | Ingredients | Result | Time, XP | Notes |
|---|---|---|---|---|---|
| `bamboo_paper` | shaped | 3 bamboo in a row | 2 paper named "Bamboo Paper", green tint | | see Bamboo Paper below |
| `blackdye` | shapeless | 1 coal or charcoal | 1 black dye | | |
| `charcoal_block` | shapeless | 9 charcoal | 1 coal block named "Charcoal Block" | | owner: keeps its name |
| `mud2clay` | shapeless | 1 mud + 1 sand or red sand | 4 clay balls | | see below |
| `sand2redsand` | shaped | 8 sand around 1 iron nugget | 8 red sand | | shaped like dyeing |
| `trident` | shaped | 3 prismarine shards on top; prismarine crystal, stick, crystal on each of the two rows below | 1 trident | | owner: kept, tridents are too rare otherwise |
| `blast2sand` | blasting | gravel or glass | sand | 200, 0 | |
| `cobble2gravel` | blasting | cobblestone | gravel | 200, 0 | |
| `blast_quartz` | blasting | andesite, diorite or granite | nether quartz | 200, 0.1 | owner: the andesite quartz loop (+1 quartz per cycle) stays |
| `ironnugget` | blasting | stone | iron nugget | 500, 0.1 | |
| `bone2calcite` | blasting | bone block | calcite | 200, 0.1 | |
| `deepslate2tuff` | blasting | cobbled deepslate | tuff | 200, 0.1 | vanilla's cobbled deepslate → deepslate is smelting, so the two never compete |
| `basalt2smooth` | blasting | basalt | smooth basalt | 200, 0.1 | vanilla already smelts this in a furnace; this adds the blast furnace route |
| `blast2diamond` | blasting | coal block | deepslate diamond ore named "Synthetic Diamond Ore" | 4000, 5 | see Synthetic Diamond Ore below |
| `orgglo` | smoking | glow berries, glow lichen or glow ink sac | glowstone dust named "Organic Glowstone Dust" | 200, 0.1 | |
| `rotleather` | smoking | rotten flesh | leather, lore "Cured zombie flesh. Gross..." | 300, 0.1 | owner: 0.1 XP, lore kept |

Owner: Organic Glowstone Dust, black dye, sand, gravel and iron nugget stay as originally written.

### Clay, red sand, calcite, tuff, smooth basalt

Recipes for blocks that are otherwise scarce or non-renewable (owner's additions in 1.1.0).

- `mud2clay` is generous on purpose (4 balls, a full clay block's worth): clay is already renewable in vanilla (mud dries over pointed dripstone), and sand is renewable through `blast2sand`.
- Its sand choice is an explicit list, not `#minecraft:sand`, because that tag includes suspicious sand.

### Bamboo Paper

- Keeps its own name (owner) and carries a green tint in `custom_model_data.colors[0]` (0xD8F0C0).
- The companion resource pack `resourcepacks/bubbas_extra_recipes` overrides vanilla's paper item definition with a `custom_model_data` tint whose default is white. So:
  - ordinary paper is unchanged;
  - the tint works on top of any paper texture (16x or 32x);
  - players without the resource pack just see paper.
- No new texture files.

### Synthetic Diamond Ore

- The `blast2diamond` result carries `minecraft:block_state {synthetic:"true"}`.
- With the companion mod, placed ore uses its own loot table: 25 % one diamond, no Fortune, no XP (`specs/extra-recipes-mod.md`).
- Without the mod the component is ignored and the ore mines like natural ore.

## Tests

- Loads clean in `tools/packload.py`.
- PackTest functions in `data/bubbas_extra_recipes/test/` run the five 1.1.0 recipes end to end: a powered crafter for the crafting ones, a fuelled blast furnace for the blasting ones, and suspicious sand refused for clay.
- `mods/packcheck` game tests look up, match and assemble each recipe, with a client screenshot of the results (`specs/pack-testing.md`).
