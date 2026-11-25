# Credits

## Classic Faithful 32x Jappa (Faithful)

Some textures in this pack are recoloured from **Classic Faithful 32x Jappa** (Vattic's Classic Faithful, by Evorp and many others; release CF32J-2026r21, and the java-latest branch for Reworked Mobs) by the Faithful Resource Pack team. All other textures are our own.

### Bubba's Reworked Mining: quartz tools and armor

| Our texture (`assets/bubbas_reworked_mining/textures/`) | Derived from (CF32J, `assets/minecraft/textures/`) |
|---|---|
| `item/quartz_sword.png`, `quartz_shovel.png`, `quartz_pickaxe.png`, `quartz_axe.png`, `quartz_hoe.png` | `item/iron_sword.png`, `iron_shovel.png`, `iron_pickaxe.png`, `iron_axe.png`, `iron_hoe.png` |
| `item/quartz_helmet.png`, `quartz_chestplate.png`, `quartz_leggings.png`, `quartz_boots.png` | `item/iron_helmet.png`, `iron_chestplate.png`, `iron_leggings.png`, `iron_boots.png` |
| `entity/equipment/humanoid/quartz.png`, `entity/equipment/humanoid_leggings/quartz.png` | `entity/equipment/humanoid/iron.png`, `entity/equipment/humanoid_leggings/iron.png` |

What changed: each grey shade is mapped to a quartz shade, and thin veins are drawn over the armor; the pixels, outlines and shading are CF32J's.

### Bubba's World Tweaks: lava geyser soot plumes

| Our texture (`assets/bubbas_world_tweaks/textures/`) | Derived from (CF32J, `assets/minecraft/textures/`) |
|---|---|
| `particle/lava_geyser_plume_01.png` to `_08.png` | `particle/geyser_plume_01.png` to `_08.png` |

What changed: each grey shade is mapped to a soot shade. The pixels and shapes are CF32J's.

### Bubba's Reworked Mobs: biome creeper skins

From the `java-latest` branch (commit 00321eab, November 2026).

| Our texture (`assets/bubbas_reworked_mobs/textures/`) | Derived from (CF32J, `assets/minecraft/textures/`) |
|---|---|
| `entity/creeper/biome/*.png` (all 19) | `entity/creeper/creeper.png` |
| `entity/creeper/biome/{oak,birch,spruce,jungle,acacia,dark_oak,mangrove,cherry,pale_oak,poplar}.png` | colours of `block/<name>_log.png` |
| `entity/creeper/biome/plains.png`, `meadow.png` | colours of `block/grass_block_top.png` (biome grass tint applied) |
| `entity/creeper/biome/{sand,red_sand,snow,stone}.png`, `dripstone.png`, `mushroom_stem.png` | colours of `block/{sand,red_sand,snow,stone}.png`, `block/dripstone_block.png`, `block/mushroom_stem.png` |

What changed: each creeper pixel takes the closest colour (OKLch lightness match) from the block texture; the pixels and shading are CF32J's creeper's. `sulfur.png` uses vanilla's sulfur colours (CF32J has none).

### Bubba's Reworked Mobs: Soulguard effect icon

| Our texture (`assets/bubbas_reworked_mobs/textures/`) | Derived from (CF32J, `assets/minecraft/textures/`) |
|---|---|
| `mob_effect/soulguard.png` (also the mod's 16x) | `mob_effect/fire_resistance.png` (flame only at its own pixel size, soul colours; Scale2x for the 32x; our padlock) |

### Bubba's Reworked Mobs: Eye of the Soul

| Our texture (`assets/bubbas_reworked_mobs/textures/`) | Derived from (CF32J, `assets/minecraft/textures/`) |
|---|---|
| `item/eye_of_the_soul.png` | `item/ender_eye.png` (each colour mapped to a soul colour; our white soulfire flame over the pupil) |

### Bubba's Reworked Mobs: Boss Piglin head

| Our texture (`assets/bubbas_reworked_mobs/textures/`) | Derived from (CF32J, `assets/minecraft/textures/`) |
|---|---|
| `entity/boss_piglin.png` (head, ears, snout, brow and tusk regions only) | `entity/piglin/piglin.png` (head and ears as they are; snout widened and deepened, tusks stretched, brow from its forehead; our palette, our gold tusk caps and earring) |

Website: https://faithfulpack.net
Used under the Faithful License: https://faithfulpack.net/license
