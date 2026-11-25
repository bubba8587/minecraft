# Credits

## Classic Faithful 32x Jappa (Faithful)

The 32x biome creeper skins in the 32x resource pack (`src/main/resources/resourcepacks/bubbas_reworked_mobs_32x/assets/bubbas_reworked_mobs/textures/entity/creeper/biome/*.png`) are recoloured from **Classic Faithful 32x Jappa** (Vattic's Classic Faithful, by Evorp and many others; `java-latest` branch, commit 00321eab, November 2026) by the Faithful Resource Pack team. Each one is CF32J's creeper with every pixel replaced by the closest colour (OKLch lightness match) from one CF32J block texture; the pixels and shading are CF32J's creeper's, the colours are CF32J's block's.

| Our texture (`textures/entity/creeper/biome/`) | Derived from (CF32J, `assets/minecraft/textures/`) |
|---|---|
| all 19 skins | `entity/creeper/creeper.png` |
| `oak.png`, `birch.png`, `spruce.png`, `jungle.png`, `acacia.png`, `dark_oak.png`, `mangrove.png`, `cherry.png`, `pale_oak.png`, `poplar.png` | colours of `block/<same name>_log.png` |
| `plains.png`, `meadow.png` | colours of `block/grass_block_top.png`, tinted with vanilla's grass colour for plains / meadow |
| `sand.png`, `red_sand.png`, `snow.png`, `stone.png`, `dripstone.png` | colours of `block/sand.png`, `red_sand.png`, `snow.png`, `stone.png`, `dripstone_block.png` |
| `mushroom_stem.png` | colours of `block/mushroom_stem.png` |
| `sulfur.png` | CF32J's creeper with the colours of vanilla's `block/sulfur.png` (CF32J has no sulfur texture) |

Made by `tools/creeper-biome-textures.py`.

### Soulguard effect icon (16x and 32x)

`assets/bubbas_reworked_mobs/textures/mob_effect/soulguard.png` and its 32x twin are derived from **Classic Faithful 32x Jappa**'s `assets/minecraft/textures/mob_effect/fire_resistance.png` (`java-latest` branch, November 2026): the flame alone (the shield removed) at its own pixel size, recoloured to soul fire colours, with our own padlock; the 32x is that flame through Scale2x (half-size pixels along its edges) with a padlock drawn at 32x. Made by `tools-local/make_soulfire_textures.py`.

### Eye of the Soul item (32x)

`resourcepacks/bubbas_reworked_mobs_32x/assets/bubbas_reworked_mobs/textures/item/eye_of_the_soul.png` is derived from **Classic Faithful 32x Jappa**'s `assets/minecraft/textures/item/ender_eye.png` (`java-latest` branch, November 2026): each colour mapped to a soul colour, with our own white soulfire flame drawn over the slit pupil. (The 16x is vanilla's Eye of Ender treated the same way.) Made by `tools-local/make_eye_of_the_soul_textures.py`.

### Boss Piglin head (32x)

The head of `resourcepacks/bubbas_reworked_mobs_32x/assets/bubbas_reworked_mobs/textures/entity/boss_piglin.png` (its head, ears, snout, brow and tusk regions; the body is our own) is derived from **Classic Faithful 32x Jappa**'s `assets/minecraft/textures/entity/piglin/piglin.png` (`java-latest` branch, November 2026): head and ears as they are, the snout widened and deepened to our bigger snout, the tusks stretched taller, the brow taken from its forehead, recoloured to our palette, with our own gold tusk caps and earring. Made by `tools-local/make_boss_piglin_textures.py`.

Website: https://faithfulpack.net
Used under the Faithful License: https://faithfulpack.net/license
