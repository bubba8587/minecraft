<!-- dte:C1,B15 -->
# Reworked Mobs: graveyards, crypts and gravestones (spec)

**Status:**
- Built: gravestones; crypt yards in four sizes and five states of breakdown; desert and snowy yards; the resting dead; pauper, family and noble crypts and their loot; will-o'-wisps; noble dead in gemmed armor (with Infused Gems); warding statues and relic vaults (with Totems & Artifacts).
- Designed, not built: standalone crypts with a collapsed hillside entrance.

Owner's design. Part 1's worldgen (`specs/undead.md` "Worldgen"): the places sleepers (dormant undead) are found, and where the Grave Talisman and Totem of Warding turn up as loot. Player-facing pages: `player-docs/Projects/Bubba's Reworked Mobs/Crypt Yards.md`, `Gravestone.md`, `Will-o'-Wisps.md`, `Noble Finery.md`.

## Prior art

- **The Graveyard** (finallion, Fabric, on 26.3, GPL-3.0): graveyards in three sizes, biome graves, a crypt, ruins, a haunted house. Owner: **roll our own; use it for layout inspiration only.** It is GPL and our mods are MIT (B12), so we read it to learn and copy nothing. It also brings its own undead roster, a boss and overrun villages, which we don't want. Optional later: if it's installed, our sleepers and loot could turn up in its crypts too.
- Gravestone mods on Modrinth are all death graves that hold the player's items. The only decorative one (Craftable Tombstones) has no 26.3 build and no source.

## Gravestones

A new block, owner-approved. **B15 reason:** no vanilla block has a headstone shape that carries carved text.

Design:
- **"Keep it simple, it should lift Signs" (owner):** a gravestone is a sign in all but looks. It reuses vanilla's sign behaviour: text front and back, editing on placement, dyes, glow ink, honeycomb waxing, and 16-way standing rotation like a standing sign. Mojang's sign code is reused, not rewritten.
- Smaller than a full block: a headstone on a plinth, with its own model and stone textures at 16x and 32x (B16).
- Players get it from a stonecutter (builder's choice, below).
- **No real names (owner):** worldgen gravestones never carry names. At most they carry scattered glyphs: a few characters in the enchanting-table alphabet (`minecraft:alt` font) or worn marks. Many are blank.

How it's built (`grave/GravestoneBlock`, `GravestoneBlockEntity`; client `client/grave/GravestoneRenderer`):
- **Block:** `GravestoneBlock extends StandingSignBlock` with a stone `WoodType` named `bubbas_gravestone`.
  - Not registered with `WoodType.register`, so vanilla builds no sign sprite materials for it.
  - Stone sounds (`BlockSetType.STONE`, `SoundType.STONE`); strength 1.5 / 6, pickaxe required, no occlusion; can't respawn in it.
  - Its own block entity type `bubbas_reworked_mobs:gravestone` and its own ticker.
  - Shapes: cardinal rotations get the exact stone and plinth (12 x 14 x 4 on 14 x 2 x 6); diagonal rotations a 9 x 9 column.
- **Block entity:** `GravestoneBlockEntity extends SignBlockEntity`: max line width 60 (a sign's is 90), line height 10. `carve(SignText, registries)` sets the front text by loading it like a save, because vanilla's `setText` notifies the level and worldgen's block entities have none yet.
- **Rendering:** 26.3 signs are ordinary block models with four rotation templates (0, -22.5, -45, -67.5 degrees) turned by quarter turns. The gravestone does the same: `models/block/gravestone_rot_0..3` and a datagen blockstate dispatch over `rotation`. The renderer is vanilla's `AbstractSignRenderer` (colours, glow, outline, filtering) with its own text transform: on the south face 2 px in front of centre, centred at y 7.5, sign scale 1/96.
- **Edit screen:** `GravestoneEditScreen` (client, subclass of vanilla `AbstractSignEditScreen`, like `HangingSignEditScreen`), opened via client mixin `GravestoneEditScreenMixin` on `LocalPlayer.openTextEdit` (gravestone block entities only; other signs keep vanilla's screen).
  - It draws the stone's front silhouette (plinth, body, shoulder, crown) from `bubbas_reworked_mobs:textures/block/gravestone.png` (u = x, v = 16 - y, as the model's auto UVs), 9 GUI px per block px, tinted 0.8 like a lit north/south face, text centre at GUI y 115.
  - One set of layout constants, `grave/GraveTextLayout` (main), is shared by `GravestoneRenderer`, `GravestoneBlockEntity` and the screen: 6 font px per block px (1/96 block), text centre y 7.5, face z 10, line width 60, line height 10, face rectangles.
- **Data:** loot drops itself (datagen), `#minecraft:mineable/pickaxe`, lang "Gravestone", stonecutting recipe `gravestone_from_stone_stonecutting`, creative tab. Stacks to 16.

## Crypt yards (surface)

Design (owner):
- Graveyards on the surface in a **variety of states of breakdown**: kept, overgrown (moss, vines, grass through the paths), sunken and tilted stones, collapsed (fallen stones, broken fences, open graves), and nearly gone (a few stones in the grass).
- **Iron bar fences** around them, with gaps, bent runs and missing sections as they decay.
- **One soul lantern per yard**, outside, on a gate post. Soul torches mark the crypt stairs (builder's choice). No soul lights inside crypts.
- Rows of gravestones over shallow graves holding the resting dead (husks in deserts, strays in snow). Some graves have loot on display (`specs/undead.md` "Loot on display").
- Sizes from a lone family plot to a large yard. Some yards sit over a crypt entrance; some don't.
- **Trees and plants are placed on purpose:** wild trees are cleared out of a yard, and the yard plants its own (one or two, by biome) plus flowers on some graves.
- **No yards in the sea:** biome rules, like vanilla's structures, rather than scanning for water.
- **Plots are coarse or rooted dirt**, so grass doesn't grow over them, in every climate and state (no sand, snow or mud on grave tops).
- **Gravestones stand straight;** only a few are turned askew (2 % kept, 5 % overgrown, 10-12 % in the worse states).
- **A grave lies on one level:** the sleeper lies under both plot blocks, so the plot blocks, the sleeper and the stone's footing share a y.

### How it's built

Code: `grave/CryptYardStructure`, `CryptYardPiece`, `YardKinds`, `Graveyards`, `YardTrees`, `GraveText`, `GraveTuning`, `GraveyardCommand`.

- **Code-built, not jigsaw (builder's choice, as this spec asked):** a vanilla `Structure` (type `bubbas_reworked_mobs:crypt_yard`) with one code-built piece (`crypt_yard`). The states of breakdown, sizes and contents are parameters of one builder, with no template files to keep in step. Being a real structure, it works with `/locate` and the Totem of Seeking's search, and caretaker endermen and the Totem of the Beginning leave it alone (the target world skips structure boxes).
- **Placement:** `data/bubbas_reworked_mobs/worldgen/structure/crypt_yard.json` (biome tag `#bubbas_reworked_mobs:has_structure/crypt_yard`, step `top_layer_modification`, no terrain adaptation) and `structure_set/crypt_yards.json` (random spread, spacing 36, separation 12, salt 20261122).
  - The biome tag has no oceans, rivers or beaches: plains, sunflower plains, meadow, forest, birch forest, dark forest, old growth birch forest, taiga, old growth pine and spruce taiga, snowy plains, snowy taiga, savanna, desert, swamp, windswept hills, pale garden.
  - `findGenerationPoint` picks the size, crypt, wealth and state (`YardKinds`, below), then requires: corner heights within 3 of each other, no water on them, not below sea level, and **the biome at every corner in the yard's tag** (vanilla checks only the start chunk's middle).
  - The yard's trees and climate come from the biome at its middle.
- **After trees:** the step is `top_layer_modification`, so the yard is built after vanilla's trees. It clears trees and plants (logs, leaves, replaceables, vines, flowers, mushroom blocks, berry bushes, pumpkins) from its columns, 24 up, before building. The ground is read past trees and plants.
- **Same yard in every chunk:** the piece builds from its own seed and only writes inside the chunk being generated. Every random roll is made before looking at the world, so every chunk draws the same sequence. Each column reads its own ground.
- **Saved:** `CryptYardPiece` saves `State`, `Wealth` and `Climate` in the piece tag (pieces saved without them read as overgrown, family and temperate). The piece's box is sized by wealth (pauper reaches 3 either side of the gate's line and 12 long; noble 12 either side and 14 long).
- **Graves:** rows every 4 blocks, every other column, keeping 2 blocks either side of the path.
  - A grave is a stone at its south end facing north over a two-block plot.
  - The level is the plot's first block's ground; dirt is filled two below and the space cleared three above. The sleeper lies one under the plot, facing north.
  - Graves that would cross a chunk border along z are left out (their level is read from one column, which only one chunk can see).
  - A lone family plot (9 wide or less) has no path between its graves.
- **States** change the build: see the per-state table under Numbers. Sunken plots settle one block into a hollow (the sleeper a block lower); collapsed yards have open graves; nearly-gone yards have grassed-over plots and no lantern.

### Sizes, states and wealth (`YardKinds`)

- `Size`: LONE 7-9, SMALL 11-13, MEDIUM 15-19, LARGE 21-27 blocks (odd). Picked 20 / 30 / 35 / 15 %. Crypt chance 0 / 50 / 60 / 80 %.
- `State`: picked kept 15 %, overgrown 30 %, sunken 20 %, collapsed 20 %, nearly gone 15 %.
- `Wealth` (PAUPER, FAMILY, NOBLE), by size: lone/small 50/50 pauper or family; medium 20 % pauper, 60 % family, 20 % noble; large 40 % family, 60 % noble.

### Command

Ops only. Both build a yard straight into the world with its gate at the player's feet, the yard running south. It is not a structure: `/locate` doesn't find it, and caretaker endermen treat it as ordinary blocks. The command build registers its yard for wisps by hand.
- `/bubbas_reworked_mobs graveyard place [crypt|nocrypt] [seed]`: size, state and wealth from the seed.
- `/bubbas_reworked_mobs graveyard build <lone|small|medium|large> <kept|overgrown|sunken|collapsed|nearly_gone> <none|pauper|family|noble>`: that kind at the size's largest, random seed.

## The resting dead

Owner's design:
- **A class of sleepers that never wake up.** Sleepers in **proper graves** (the plots under gravestones in crypt yards) are always the skeleton kind (skeletons; husks and strays in desert and snowy yards) and **never wake**: noise, bells, robbing and the rest leave them be. They are the yard's buried dead, not a trap.
- They are **more likely to wear dyed leather armor** (burial clothes), in muted colours.
- **Free to rob and dig (owner):** players can take their armor and items, and mine the skeleton itself, at no risk. A mined one stays a never-waking sleeper wherever it's placed, so it works as decoration.
- Crypt sleepers (ossuaries, vaults, noble crypts) wake as normal. The danger lives underground.

How it's built:
- `SleeperBlock.RESTING` (block state, default false). `SleeperBlockEntity.disturb` returns 0 and `scheduleWake` does nothing for a resting sleeper, and `SleeperWaking.wake` refuses one. That covers noise, bells, robbing, and anything that schedules a wake.
- The Silk Touch loot copies `resting` with `pose` (datagen), so the item's `block_state` carries it.
- Yard graves place them via `Build.resting(…, variant, …)` (variant = `YardKinds.Climate.resting()`): 75 % of graves have one (`GraveTuning.GRAVE_SLEEPER_CHANCE`); leather 70 % (`RESTING_LEATHER_CHANCE`: helmet and chest, legs 60 %, boots 40 %, one of eight colours); a bow 15 % (skeleton kinds); 15 % lie face down (`specs/undead.md` "Face-down sleepers").
- An unused roll stands where the old skeleton/zombie roll was, so later rolls don't shift.

## Crypts (underground)

Design:
- **Different crypts at different wealth levels** (owner). The layouts below are the builder's, for the owner to adjust.
- Reached by stairs from a crypt yard (built), or standalone with a collapsed entrance in a hillside (designed, not built).
- **Lit with candles only (owner)**, and dark enough that sleepers stay asleep until disturbed (light calms them: `specs/undead.md`). No soul lantern or soul torch inside a crypt.
- A sleeper in a niche wakes standing in front of it (`specs/undead.md` "Waking in tight spots").

Common to all crypts:
- Floor 5 below the yard's ground (`CRYPT_DEPTH` 6 to the floor block).
- **Stairs:** stone brick stairs, one step per block, brick walls, a low parapet only round the top two steps, a brick roof once below ground, a soul torch on each side wall at the top (outside).
- **Sleepers** (`Build.sleeper`): skeleton or zombie 60/40 (climate-swapped in desert and snowy yards: `specs/undead.md` "Undead variants"). Skeleton kinds get a bow half the time; zombies a stone sword or iron shovel a quarter of the time. 30 % of lying sleepers lie face down.

The three kinds:
- **Pauper's ossuary** (small and medium yards): a cramped tunnel dug through the ground, 3 wide and 3 high, 11 long, walled in cobblestone, packed mud and coarse dirt with bone blocks and mossy cobblestone set in (`ossuaryWall`). Niches nearly every block on both sides, nearly all holding a sleeper with little or nothing on them; some niches stack a second sleeper two above the floor niche. A candle or two, most out; one chest of poor things at the end (`chests/crypt_pauper`, 2-5 rolls).
- **Family vault:** 7 wide, 9 long, 4 high inside. Stone brick shell (cracked and mossy mixed in), polished andesite and cobblestone floor.
  - Niches every other block on both long walls, with a slab over each; 80 % hold a lying sleeper. Vault sleepers wear a chain or iron helmet 30 % of the time.
  - Altar at the far end (chiseled stone bricks, 3 lit candles) and the chest (`chests/crypt_family`, 3-6 rolls). More candles along the walls, about a quarter of them lit; cobwebs.
- **Noble crypt** (medium and large yards): polished and chiseled stone, an entry hall, a pillared chamber with a sarcophagus holding an armoured sleeper under a stone lid, and two side halls lined with niches. Chests: `chests/crypt_noble` (3-6 rolls, plus one burial-goods roll; see Noble crypt loot).
  - **Noble armour**: 45 % of hall sleepers (`GraveTuning.NOBLE_HALL_ARMOUR`; 85 % until the owner found crypts too rich, 2026-11-24) and the sarcophagus's (100 %) wear a full set: 15 % gold, 40 % chain, 45 % iron. Zombies carry iron swords or axes 60 % of the time. No enchanted gear on them (that's in the chests).

Loot tables are in 26.3's format (`modifier` objects: `set_count`, `enchant_randomly` with `#minecraft:on_random_loot`; the older `functions` key is silently ignored). Totems & Artifacts adds to them by table id, so there's no dependency (Totems' loot test skips tables that aren't installed):
- Grave Talisman (`TotemsTuning.TALISMAN_LOOT`): family 15 %, noble 25 %.
- Totem of Warding (`WardingTuning.LOOT`): noble 12 %.

## Desert and snowy yards

Crypt yards take a climate from the biome at their centre (`YardKinds.Climate`), saved in the piece tag as `Climate`:
- **DESERT** (desert and badlands; badlands isn't in the yard's biome tag, so in practice deserts): crypts of 70 % husks and 30 % skeletons, the husks buried under sand in the niches (`Build.encase`); resting husks in the graves; no trees (dead bushes on the plots instead of grass and ferns).
- **SNOWY** (base temperature under 0.15): crypts of 70 % strays and 30 % zombies, the strays frozen in clear ice in the niches; resting strays in the graves.
- **TEMPERATE:** everything else.
- Plots stay coarse or rooted dirt in every climate.
- Resting dead follow the climate (builder's call, see Open). Details in `specs/undead.md` "Undead variants".

## Warding statues in crypts

With Totems & Artifacts loaded, a Totem of Warding statue stands among each wing's sleepers, facing the way in.
- Odds per wing (`GraveTuning.CRYPT_STATUE_NOBLE / _FAMILY / _PAUPER`): noble 85 % for the chamber and 85 % for each side hall, family 65 %, pauper 35 %.
- Rolled from the piece's own seed (`mixStafford13(seed) ^ 0x3A2D5`), separate from the build random, so layouts are unchanged. Chest loot unchanged.
- Without Totems & Artifacts nothing is placed (builder: no stand-in decoration).
- Breaking the statue drops the totem, and the sleepers can wake again.
- **Spots** (`CryptYardPiece.wardSpots()`, record `WardSpot(pos, facing)`, on the floor). Every crypt sleeper lies within 5 of its wing's spot.
  - Family: the vault's middle (gate x, `start + VAULT_LENGTH/2`), facing north.
  - Pauper: one west of the tunnel's middle (`gate - 1`, `start + PAUPER_LENGTH/2`), facing north; the cell east of it is cleared so the tunnel stays open.
  - Noble chamber: (gate, `chamber + 1`), just inside the entry, facing north. Each side hall: its middle (`gate ± 8`, `mid`), facing the chamber.
- **Placement** (`CryptYardPiece.wardStatues`): the block is looked up by id (`bubbas_totems_artifacts:totem_statue`, `BuiltInRegistries.BLOCK.getOptional`), `totem=warding` and `facing` set by property name (no compile dependency), and a block tick scheduled so it pulses at once (worldgen `setBlock` doesn't call `onPlace`).

## Crypt vaults behind noble crypts

With Totems & Artifacts loaded, 35 % of noble crypts (rolled from the crypt's seed) get a small relic vault whose stair starts just behind the crypt's far wall, between the two family chests. The vault itself: `specs/totems-artifacts.md`, "Relic maps and relic vaults".

- Reworked Mobs only offers the hook: `api/Crypts` (`setNobleAttachment`, `attachments(piece)`, `placeDisplay`, `placeStuckDisplay`, `placeSittingSleeper`; stable, add-don't-change), which `CryptYardStructure` and `GraveyardCommand.build` call.
- `CryptYardPiece` has `seed()` and `nobleFarWall()` for it.
- Nothing is registered without Totems.

## Noble crypt loot

Agent's call (owner: "do whatever you like with the noble crypt loot"). The noble crypts are the endgame dungeon find.
- **Base table** (3-6 rolls; 4-8 until 2026-11-25, when golden apples went from weight 5 to 3 and diamonds 3 to 2): gold, emeralds, iron, a golden apple, diamonds, enchanted books and gear.
- **Burial goods** (a second pool, 1 roll; 1-2 until the owner cut it, 2026-11-25): a level 25-35 enchanted book, a worn but strongly enchanted diamond sword or chestplate, an enchanted golden helmet or chestplate; rarely an enchanted golden apple, a totem of undying or disc 13.
- **True Unbreaking books** with Bubba's Reworked Mining: 20 % per noble chest (its main source; ancient cities 4 %, End cities 3 %, bastions and ominous vaults 5 %).
- With Totems & Artifacts: Grave Talisman 25 % and Totem of Warding 12 % per noble chest.

## Noble dead in gemmed armor

Owner's design: with Bubba's Infused Gems installed, the armor and weapons of noble crypt sleepers carry gems (their burial finery). Woken, they fight with those effects; robbed, their gemmed gear is the prize. No hard dependency.

- **What players get:** the sarcophagus's sleeper and 35 % of the hall sleepers (`GraveTuning.NOBLE_GEM_CHANCE`, rolled from the sleeper's position so the build's other rolls don't shift) wear gemmed gear: 1 gem per armor piece, 1 in the weapon, infusion 2-4, tier 2-3 (a little richer than ordinary gemmed mob gear). Infused Gems leaves some pieces without (its own rules on what takes a gem). Until 2026-11-24 every noble sleeper was gemmed with 1-2 per armor piece; the owner robbed a warded noble crypt and came away with barrels of gems and armor, so both were cut two steps. Without Infused Gems, nothing changes.
- **Data, not code, decides the gems:** item modifier `data/bubbas_reworked_mobs/item_modifier/noble_finery.json` uses Infused Gems' own `bubbas_infused_gems:socket_random_gems` (armor 1, weapon 1; infusion [2,3,3,4], tier [2,2,3], any variant). It loads only with that mod (`fabric:load_conditions` `fabric:all_mods_loaded`).
- `grave/NobleFinery.apply` runs it over a noble sleeper's gear when the crypt is built (`CryptYardPiece.Build.sleeper`, its `noble` flag). If the modifier isn't loaded it does nothing. Loot context: chest param set with the sleeper's position; seeded per position.
- Build option `-PwithGems` (Reworked Mobs) puts Infused Gems on the test game's classpath, like `-PwithBeacons`.

## Will-o'-wisps

Owner's design:
- **Just soul fire particles** (vanilla `soul_fire_flame`), **faint and only occasional**.
- **Over graveyards at night:** a few drift over the graves and slowly lead toward the yard's crypt entrance.
- **Near the player:** whenever a graveyard is loaded nearby, the odd wisp appears **between the player and the yard**, a faint hint at which way it lies.
- **Cleared for good after discovery:** once a player has walked into a yard, its wisps stop showing for that player, both the hints and the ones over its graves. Other players who haven't found it still see them.

How it works:
- **Hints:** while a yard is loaded within **96 blocks**, now and then (about one every 8 seconds per yard) a single wisp appears **6-12 blocks from the player toward the yard**, drifting that way.
- **Over the graves at night:** within **48 blocks**, wisps rise over the yard's graves now and then (about one every 2 seconds) and drift slowly toward the **crypt entrance** (`CryptYardPiece.wispTarget()`: the top of its stairs, or above the gate if the yard has no crypt).
- **Found yards:** walking anywhere inside a yard's footprint adds it to the player's saved list, a persistent player attachment `bubbas_reworked_mobs:found_yards` (a list of yard ids = start chunk), copied on death. The server sends wisps only to players who haven't found that yard.
- **Finding yards cheaply** (`grave/Wisps`): yards are found from the structure references of loaded chunks round each player (as camps are) and kept per level in memory (not saved: they're rediscovered from chunk data after a restart). The command build registers its yard by hand. Players: `level.getPlayers(EntitySelector.NO_SPECTATORS)`.
- **Particles** go only to the player concerned: `ServerLevel.sendParticles(ServerPlayer, ...)` with count 0 (the offsets give the drift).

| What | Value | Where |
|---|---|---|
| Checked | every second | `GraveTuning.WISP_TICKS` |
| Hint range / chance / distance | 96 blocks / 12 % per check / 6-12 blocks from the player | `WISP_RANGE`, `WISP_HINT_CHANCE`, `WISP_HINT_MIN`/`MAX` |
| Night range / chance | 48 blocks / 50 % per check | `WISP_NIGHT_RANGE`, `WISP_NIGHT_CHANCE` |
| Yards looked for | 6 chunks round each player | `WISP_DISCOVER_CHUNKS` |

## Numbers

Builder's starting values, owner to playtest. In `GraveTuning` unless noted.

| | Value |
|---|---|
| Yard size | by size class, 7-27 (`YardKinds.Size`) |
| Max corner slope | 3 (`YARD_MAX_SLOPE`) |
| Yards with a crypt | by size: 0 / 50 / 60 / 80 % |
| Crypt depth | 6 (floor 5 below ground) |
| Blank stones | 40 % (the rest 1-3 lines of glyph words) |
| Buried loot | 12 % of graves |
| Graves with a sleeper | 75 % |
| Resting dead in leather | 70 % |
| Trees per yard | 2 at 35 %, else 1 at 75 % (none in deserts) |
| Weeds on a plot block | 25 % (after flowers, below) |
| Structure spacing / separation | 36 / 12 chunks |
| Grave Talisman | family 15 %, noble 25 % (Totems & Artifacts) |
| Warding statue per wing | noble 85 %, family 65 %, pauper 35 % (Totems & Artifacts) |

Per state of breakdown (`YardKinds.State`):

| | Kept | Overgrown | Sunken | Collapsed | Nearly gone |
|---|---|---|---|---|---|
| Fence bars gone | 0 | 12 % | 20 % | 45 % | 85 % |
| Fence one high | 0 | 18 % | 30 % | 35 % | 50 % |
| Mossy posts | 10 % | 35 % | 40 % | 30 % | 50 % |
| Broken posts | 0 | 0 | 10 % | 40 % | 50 % |
| Posts gone | 0 | 0 | 0 | 10 % | 50 % |
| Stones askew | 2 % | 5 % | 10 % | 10 % | 12 % |
| Stones fallen | 0 | 10 % | 12 % | 35 % | 30 % |
| Stones gone | 0 | 0 | 0 | 10 % | 60 % |
| Open graves | 0 | 0 | 0 | 30 % | 0 |
| Flowers on a plot block | 35 % | 15 % | 10 % | 5 % | 0 |

## Builder's choices (owner may change)

1. **Code-built structure, not jigsaw.** Reasons under Crypt yards.
2. **Gate in the north fence, yard running south;** stones face north over their plots, toward someone walking in.
3. **The yard plants its own trees** (oak, birch, spruce, dark oak or pale oak by biome, code-built, not vanilla's tree features). A vanilla tree can't be clipped to the chunk being built, so chunks would disagree about it.
4. **Graves crossing a chunk border are left out** (a few per yard).
5. **Soul torches at the top of the crypt stairs** (two, outside). The owner asked for candles only *inside* and one lantern in the yard; these torches mark the entrance. Easy to drop.
6. **Stonecutter recipe only** (1 stone → 1 gravestone); no crafting-table recipe.
7. **Pickaxe required** to get a gravestone back, like stone.
8. **Worldgen stones aren't waxed:** players can carve their own words on a found stone.
9. **Fallen stones are stone brick slabs** lying in the grass.
10. **Buried loot** is an iron sword, stone axe, iron shovel or bow, at the plot's foot.
11. **Shares** of sizes, states and wealths (above); lone plots never have a crypt (too small for the stairs).
12. **Sunken** = plots settled one block into a hollow, the sleeper a block lower; stones lean (rotation) rather than sink (a gravestone sunk one block would be hidden).
13. **Open graves** (collapsed yards) show the resting dead; buried loot isn't placed in open or sunken plots.
14. **Nearly-gone yards keep their dead** (75 % of graves) but lose their lantern.
15. **Ossuary niches** sometimes stack a second sleeper two above the floor niche.
16. **Noble armour** mix (under Crypts).
17. **The resting dead** keep the normal sleeper name as items ("Sleeping Skeleton" and so on; no separate name or tooltip).

## Tests

- **Core** `ReworkedMobsCoreGameTest.graveyardStonesWithoutNamesAndSleepers` (body in `GravesGameTest.yardHasStonesWithoutNamesAndSleepers`): a built 13 x 13 yard has at least 4 gravestones, all front text glyphs or blank and no back text, and at least one sleeper; every yard-grave sleeper is resting and of the yard climate's resting kind.
- **Milestone** `GravesGameTest`:
  - `generatedTextIsGlyphsOrBlank`: 2000 stones, only glyphs or blank, about 40 % blank; a readable name fails the check.
  - `gravestoneIsASign`: all 16 rotations; front and back text, dye and glow survive save and load; drops itself to a pickaxe.
  - `restingDeadNeverWake`: an explosion three blocks off wakes an ordinary sleeper (control) but not a resting one; neither does a bell rung beside it, nor robbing by a player (at sensitivity 10, where an ordinary robbery wake is certain). Mined with Silk Touch, the item keeps `resting`; placed again it is still resting and sleeps through another explosion and 40 more ticks.
  - `everyYardBlockIsSupported`: in a built overgrown yard with each kind of crypt, no candle, lantern, sign or plant is left without support.
  - `cryptWardStatueOdds`: statues per wing at the stated odds over 4000 seeds per kind (within 3 points); without Totems & Artifacts the spot stays air.
- **Milestone** `WispsGameTest.walkingIntoAYardFindsIt`: a built yard is registered; a player outside hasn't found it; inside, it's found.
- **Milestone** `NobleFineryGameTest.nobleDeadWearGemsOnlyWithInfusedGems`: a built noble crypt; without Infused Gems no gear carries gem components; with `-PwithGems` some do, at most half the armoured ones; under 70 % of the crypt's sleepers wear a full set. No screenshot: gemmed gear looks the same on a sleeper.
- **Totems:** `totemsAreFoundInLoot` passes standalone (crypt table skipped) and with `-PwithMobs`.
- **Client part `graves`** (shots in `build/run/clientGameTest/screenshots/`): `graves-front/angle/close/close-glow/night` at 16x and 32x (four stones: epitaph, glyphs, blank, glow ink); `graves-edit` and `graves-edit-stone` (plus -32x); `yard-natural-above/gate` (the nearest natural yard, seed 20261116, at 624, 304).
- **Client part `yards`** (`YardKindsClientGameTest`): every state, a lone plot, and pauper/family/noble yards on sky platforms: `yard-<kind>-above`, `yard-<kind>-gate`, `crypt-<wealth>` (night vision), `crypt-<wealth>-dark`, `crypt-noble-hall`.
- **Client part `wisps`** (`WispsClientGameTest`): at night from outside a built yard, wisps forced over the graves plus hints (`wisps-1`: blue soul flames over the yard and toward it). Natural ones are much sparser (one at a time).

## Milestone test objectives

Covered by tests above:
- A gravestone places with any of the 16 rotations, takes text front and back, keeps it through save and load, and drops itself (`gravestoneIsASign`).
- The gravestone edit screen opens on placement and shows the lines where the placed stone draws them (client part `graves`: `graves-edit`, `graves-edit-stone`, plus -32x).
- Worldgen gravestones carry no real names: only blank text or glyphs (`generatedTextIsGlyphsOrBlank`).
- A crypt with Totems & Artifacts gets statues per wing at the stated odds; without it the spot stays air (`cryptWardStatueOdds`).

Objectives with no test yet (checked by eye in the client parts):
- A crypt generates under a natural yard with its chest and niche sleepers (needs a deeper test field).
- Each crypt wealth level generates with sleepers and its own loot table, and each wealth's chest rolls its own table in generated worlds. The noble crypt's table can give a Grave Talisman and a Totem of Warding (with Totems & Artifacts).
- Crypt yards generate naturally in each breakdown state and size, with iron bar fences and soul lights (shares are code, not measured).

## Open

- **Builder's choices to review:** the list above (stonecutter-only recipe, soul torches at the crypt stairs, shares of sizes/states/wealths, noble armour mix).
- **Resting dead in desert/snowy yards:** husks/strays (built) or skeletons everywhere (owner said "the skeleton kind")? Flip with `YardKinds.Climate.resting()`.
- **Standalone hillside crypts:** designed, not built; still wanted?
- **Gravestone edit screen title:** still vanilla's "Edit Sign Message".
