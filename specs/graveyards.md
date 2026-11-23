<!-- dte:C1,B15 -->
# Reworked Mobs: graveyards, crypts and gravestones (spec)

**Status:** built: gravestones, crypt yards in four sizes and five states of breakdown, desert and snowy yards, the resting dead, pauper/family/noble crypts and their loot. In progress (agent 2): will-o'-wisps, noble dead in gemmed armor. Designed, not built: standalone crypts with a collapsed hillside entrance.

Owner's design (2026-11-22). Part 1's worldgen (`specs/undead.md` "Worldgen"): the places sleepers (dormant undead) are found, and where the Grave Talisman and Totem of Warding turn up as loot.

## Prior art

- **The Graveyard** (finallion, Fabric, on 26.3, GPL-3.0): graveyards in three sizes, biome graves, a crypt, ruins, a haunted house. Owner: **roll our own; use it for layout inspiration only.** It is GPL and our mods are MIT (B12), so we read it to learn and copy nothing. It also brings its own undead roster, a boss and overrun villages, which we don't want. Optional later: if it's installed, our sleepers and loot can turn up in its crypts too.
- Gravestone mods on Modrinth are all death graves that hold your items. The only decorative one (Craftable Tombstones) has no 26.3 build and no source.

## Gravestones (a new block, owner-approved 2026-11-22)

- **B15 reason (owner approved):** no vanilla block has a headstone shape that carries carved text.
- **"Keep it simple, it should lift Signs" (owner):** a gravestone is a sign in all but looks. It reuses vanilla's sign behaviour: text front and back, editing on placement, dyes, glow ink, honeycomb waxing, and 16-way standing rotation like a standing sign. It is smaller than a full block (a headstone slab, roughly a 12 x 14 x 3 pixel stone), with its own model and stone textures at 16x and 32x (B16). Mojang's sign code is reused, not rewritten. How it hooks in (a stone `WoodType`, extending the sign block and block entity classes, the renderer) is a technical choice, recorded here once built.
- Crafted by players (recipe: builder's proposal, to show the owner).
- **No real names (owner):** worldgen gravestones never carry names. At most they carry scattered glyphs: a few characters in the enchanting-table alphabet (`minecraft:alt` font) or worn marks. Many are blank.

## Crypt yards (surface)

- Graveyards on the surface in a **variety of states of breakdown** (owner): kept, overgrown (moss, vines, grass through the paths), sunken and tilted stones, collapsed (fallen stones, broken fences, open graves), and nearly gone (a few stones in the grass).
- **Iron bar fences** (owner) around them, with gaps, bent runs and missing sections as they decay.
- **One soul lantern per yard** (owner, 2026-11-22), on a gate post; soul torches mark the crypt stairs (builder's choice). No soul lights inside crypts.
- Rows of gravestones over shallow graves holding the resting dead (below; husks in deserts, strays in snow). Some graves have loot on display (`specs/undead.md` "Loot on display").
- Sizes from a lone family plot to a large yard. Some yards sit over a crypt entrance; some don't.

## The resting dead (owner, 2026-11-22)

- **A class of sleepers that never wake up.** Sleepers in **proper graves** (the plots under gravestones in crypt yards) are **always skeletons** (husks and strays in desert and snowy yards, see below) and **never wake**: noise, bells, robbing and the rest leave them be. They are the yard's buried dead, not a trap.
- They are **more likely to wear dyed leather armor** (burial clothes), in muted colours.
- **Free to rob and dig (owner):** players can take their armor and items, and mine the skeleton itself, at no risk. A mined one stays a never-waking sleeper wherever it's placed, so it works as decoration.
- Crypt sleepers (vaults, ossuaries, noble crypts) are unchanged: they wake as before. The danger lives underground.
- Replaces the earlier yard rule (skeletons and zombies 50/50 in graves).

## Crypts (underground)

- **Different crypts at different wealth levels** (owner). Builder's starting proposal, for the owner to adjust:
  - *Pauper's ossuary:* a cramped dug tunnel and niches, bone blocks, cobble and dirt, many sleepers with little gear, poor loot.
  - *Family vault:* stone-brick chambers with a few coffin niches, candles, modest loot, a chance of a Grave Talisman.
  - *Noble crypt:* polished and chiseled stone, a central sarcophagus room, side halls, armoured sleepers, the best loot (Grave Talisman, Totem of Warding, enchanted gear).
- Reached by stairs from a crypt yard (built), or standalone with a collapsed entrance in a hillside (designed, not built).
- **Lit with candles only (owner, 2026-11-22)**, and dark enough that sleepers stay asleep until disturbed (light calms them: `specs/undead.md`).

## Owner decisions during the build (2026-11-22)

- **Crypts are dim:** candles only. No soul lantern or soul torch inside a crypt.
- **One soul lantern per yard,** outside: on one gate post.
- **Trees and plants belong in yards, placed on purpose:** wild trees are cleared out of a yard, and the yard plants its own (one or two, by biome) plus flowers on some graves.
- **No yards in the sea:** biome rules, like vanilla's structures, rather than scanning for water. The yard's biome tag has no oceans, rivers or beaches, and every corner must be in it. (The yard the owner saw in water was one a test built by command on a river; the test now picks dry ground.)
- **Plots are coarse or rooted dirt,** so grass doesn't grow over them, in every climate and state (owner, 2026-11-23: no sand, snow or mud on grave tops).
- **Gravestones stand straight;** only a few are turned askew (2 % kept, 5 % overgrown, 10-12 % in the worse states; owner, 2026-11-23).
- **A grave lies on one level:** the sleeper under both plot blocks, so the plot blocks, the sleeper and the stone's footing share a y (the first build followed each column's ground and left some plots one up, one down).

## How it's built (step 1, 2026-11-22; step 2 changes below)

**Gravestone (`grave/GravestoneBlock`, `GravestoneBlockEntity`; client `client/grave/GravestoneRenderer`).** Vanilla's sign code reused, as the owner asked:
- `GravestoneBlock extends StandingSignBlock` with a stone `WoodType` named `bubbas_gravestone`. It is not registered with `WoodType.register`, so vanilla builds no sign sprite materials for it; its sounds are stone (`BlockSetType.STONE`, `SoundType.STONE`). Its own block entity type `bubbas_reworked_mobs:gravestone` and its own ticker. Shapes: cardinal rotations get the exact stone and plinth (12 x 14 x 4 on 14 x 2 x 6); diagonal rotations a 9 x 9 column. Can't respawn in it. Properties: strength 1.5 / 6, pickaxe required, no occlusion.
- `GravestoneBlockEntity extends SignBlockEntity`: max line width 60 (a sign's is 90), line height 10. `carve(SignText, registries)` sets the front text by loading it like a save, because vanilla's `setText` notifies the level and worldgen's block entities have none yet (that crashed the first run).
- Rendering: 26.3 signs are ordinary block models with four rotation templates (0, -22.5, -45, -67.5 degrees) turned by quarter turns. The gravestone does the same: `models/block/gravestone_rot_0..3` and a datagen blockstate dispatch over `rotation`. The renderer is vanilla's `AbstractSignRenderer` (colours, glow, outline, filtering) with its own text transform: on the south face 2 px in front of centre, centred at y 7.5, sign scale 1/96.
- Edit screen (2026-11-23): gravestones open `GravestoneEditScreen` (client, subclass of vanilla `AbstractSignEditScreen`, like `HangingSignEditScreen`), via client mixin `GravestoneEditScreenMixin` on `LocalPlayer.openTextEdit` (gravestone block entities only; other signs keep vanilla's screen). It draws the stone's front silhouette (plinth, body, shoulder, crown) from `bubbas_reworked_mobs:textures/block/gravestone.png` (u = x, v = 16 - y, as the model's auto UVs), 9 GUI px per block px, tinted 0.8 like a lit north/south face, text centre at GUI y 115. One set of layout constants, `grave/GraveTextLayout` (main): 6 font px per block px (1/96 block), text centre y 7.5, face z 10, line width 60, line height 10, face rectangles; used by `GravestoneRenderer`, `GravestoneBlockEntity` and the screen. The old GUI background `bubbas_gravestone.png` (16x and 32x) is deleted and `tools/gravestone-textures.py` no longer writes it. Open: the screen title is still vanilla's "Edit Sign Message".
- Data: loot drops itself (datagen), `#minecraft:mineable/pickaxe`, lang "Gravestone", stonecutting recipe `gravestone_from_stone_stonecutting`. Creative tab.

**Crypt yards (`grave/CryptYardStructure`, `CryptYardPiece`, `Graveyards`, `YardTrees`, `GraveText`, `GraveTuning`, `GraveyardCommand`).**
- **Code-built, not jigsaw (builder's choice, as the spec asked):** a vanilla `Structure` (type `bubbas_reworked_mobs:crypt_yard`) with one code-built piece (`crypt_yard`). The states of breakdown, sizes and contents are parameters of one builder, with no template files to keep in step. Being a real structure, it works with `/locate` and the Totem of Seeking's search, and caretaker endermen and the Totem of the Beginning leave it alone (the target world skips structure boxes).
- **Placement:** `data/bubbas_reworked_mobs/worldgen/structure/crypt_yard.json` (biome tag `#bubbas_reworked_mobs:has_structure/crypt_yard`, step `top_layer_modification`, no terrain adaptation) and `structure_set/crypt_yards.json` (random spread, spacing 36, separation 12, salt 20261122). `findGenerationPoint`: size 13-19 x 13-19 (odd), corner heights within 3 of each other, no water on them, not below sea level, and **the biome at every corner in the yard's tag** (vanilla checks only the start chunk's middle; owner, 2026-11-22: no yards in the ocean, preferring biome rules to a water scan). The yard's trees come from the biome at its middle.
- **After trees:** the step is `top_layer_modification` so the yard is built after vanilla's trees, and it clears trees and plants (logs, leaves, replaceables, vines, flowers, mushroom blocks, berry bushes, pumpkins) from its columns, 24 up, before building. The ground is read past trees and plants.
- **Same yard in every chunk:** the piece builds from its own seed and only writes inside the chunk being generated. Every random roll is made before looking at the world, so every chunk draws the same sequence. Each column reads its own ground.
- **Graves:** rows every 4 blocks, every other column, keeping 2 blocks either side of the path. A grave is a stone at its south end facing north over a two-block plot. The level is the plot's first block's ground; dirt is filled two below and the space cleared three above. The sleeper lies one under the plot, facing north. Graves that would cross a chunk border along z are left out (their level is read from one column, which only one chunk can see).
- **Sleepers:** yard graves hold the resting dead (step 2, below); vault niches skeleton or zombie 60/40 (climate-swapped in desert and snowy yards). Gear: skeletons a bow half the time, zombies a stone sword or iron shovel a quarter of the time; vault sleepers a chain or iron helmet 30 %.
- **Vault:** 7 wide, 9 long, 4 high inside, floor 5 below the yard's ground (`CRYPT_DEPTH` 6 to the floor block). Stone brick shell (cracked and mossy mixed in), polished andesite and cobblestone floor. Niches every other block on both long walls with a slab over each. Altar (chiseled stone bricks, 3 lit candles), chest (`bubbas_reworked_mobs:chests/crypt_family`), more candles along the walls, about a quarter of them lit, cobwebs. Stairs: stone brick stairs one step per block, brick walls, a low parapet only round the top two steps, a brick roof once below ground, a soul torch on each side wall at the top.
- **Loot:** `data/bubbas_reworked_mobs/loot_table/chests/crypt_family.json`, 3-6 rolls, in 26.3's format (`modifier` objects: `set_count`, `enchant_randomly` with `#minecraft:on_random_loot`; the older `functions` key is silently ignored). The Grave Talisman joins it from Totems & Artifacts (`TotemsTuning.TALISMAN_LOOT`, 15 %, keyed by the table's id so there's no dependency; Totems' loot test skips tables that aren't installed).

### Numbers (`GraveTuning`)

| | Value |
|---|---|
| Yard size | by size class, 7-27 (step 2, `YardKinds.Size`) |
| Max corner slope | 3 |
| Yards with a crypt | by size: 0 / 50 / 60 / 80 % |
| Crypt depth | 6 (floor 5 below ground) |
| Blank stones | 40 % (the rest 1-3 lines of glyph words) |
| Buried loot | 12 % of graves |
| Graves with a sleeper | 75 % |
| Resting dead in leather | 70 % |
| Trees per yard | 2 at 35 %, else 1 at 75 % (none in deserts) |
| Flowers / weeds on a plot block | 15 % / 25 % |
| Missing fence bars | 12 %; one high 18 % |
| Stones askew / fallen | 25 % / 10 % |
| Structure spacing / separation | 36 / 12 chunks |
| Grave Talisman | family 15 %, noble 25 % (Totems & Artifacts) |

### Builder's choices (owner may change)

1. **Code-built structure, not jigsaw.** Reasons above.
2. **Gate in the north fence, yard running south;** stones face north over their plots, toward someone walking in.
3. **The yard plants its own trees** (oak, birch, spruce, dark oak or pale oak by biome, code-built, not vanilla's tree features). A vanilla tree can't be clipped to the chunk being built, so chunks would disagree about it.
4. **Graves crossing a chunk border are left out** (a few per yard).
5. **Soul torches at the top of the crypt stairs** (two, outside). The owner asked for candles only *inside* and one lantern in the yard; these torches mark the entrance. Easy to drop.
6. **Stonecutter recipe only** (1 stone → 1 gravestone); no crafting-table recipe.
7. **Pickaxe required** to get a gravestone back, like stone.
8. **Worldgen stones aren't waxed:** players can carve their own words on a found stone.
9. **Fallen stones are stone brick slabs** lying in the grass.
10. **Buried loot** is an iron sword, stone axe, iron shovel or bow, at the plot's foot.

**Command (ops):** `/bubbas_reworked_mobs graveyard place [crypt|nocrypt] [seed]` builds a yard with its gate at your feet, the yard running south, straight into the world (not a structure: `/locate` doesn't find it, and caretaker endermen treat it as ordinary blocks).

### Tests

- Core `ReworkedMobsCoreGameTest.graveyardStonesWithoutNamesAndSleepers` (body in `GravesGameTest.yardHasStonesWithoutNamesAndSleepers`): a built 13 x 13 yard has at least 4 gravestones, all front text glyphs or blank and no back text, and at least one sleeper.
- Milestone `GravesGameTest`: `generatedTextIsGlyphsOrBlank` (2000 stones: only glyphs or blank, about 40 % blank, a readable name fails the check), `gravestoneIsASign` (all 16 rotations; front and back text, dye and glow survive save and load; drops itself to a pickaxe).
- Client part `graves` (shots in `build/run/clientGameTest/screenshots/`): `graves-front/angle/close/close-glow/night` at 16x and 32x (four stones: epitaph, glyphs, blank, glow ink); `yard-natural-above/gate` (the nearest natural yard, seed 20261116, at 624, 304). Built yards of every kind and wealth and their crypts (lit and dark) are the `yards` part (`YardKindsClientGameTest`); the built-on-plains yard and its vault were dropped from `graves` as duplicates (2026-11-22).
- Totems: `totemsAreFoundInLoot` passes standalone (crypt table skipped) and with `-PwithMobs`.
- Not yet tested (objectives): a crypt generates under a natural yard with its chest and niche sleepers (needs a deeper test field; checked by eye in the client part); crypt yards generate in each breakdown state (step 2); the gravestone's edit screen opens on placement (vanilla path, not exercised by a test).

## Will-o'-wisps (owner, 2026-11-22; built)

- **Just soul fire particles** (vanilla `soul_fire_flame`), **faint and only occasional**.
- **Over graveyards at night:** a few drift over the graves and slowly lead toward the yard's crypt entrance.
- **Near the player:** whenever a graveyard is loaded nearby, the odd wisp appears **between the player and the yard**, a faint hint at which way it lies.
- **Cleared for good after discovery:** each player has a saved list of the yards they've reached (a persistent player attachment). Once you've walked into a yard, its wisps stop showing for you, both the hints and the ones over its graves. Other players who haven't found it still see them. The server sends them only to players who haven't found that yard.
- Needs a cheap way to find yards near a player (a per-world registry of yards, like the cairn sites, rather than structure searches).

### As built (2026-11-22, agent 2)

#### Player-facing

- Faint, occasional **soul fire flames** (vanilla `soul_fire_flame`) lead you to crypt yards you haven't found yet:
  - **Hints:** while a yard is loaded within **96 blocks**, now and then (about one every 8 seconds per yard) a single wisp appears **6-12 blocks from you toward the yard**, drifting that way.
  - **Over the graves at night:** within **48 blocks**, wisps rise over the yard's graves now and then (about one every 2 seconds) and drift slowly toward the **crypt entrance** (the top of its stairs; the gate if the yard has no crypt).
- **Cleared for good once found:** walking into a yard (anywhere inside its footprint) adds it to your own saved list; its wisps stop showing **for you**. Other players who haven't found it still see them; the server sends wisps only to players who haven't found that yard. The list survives death.

#### Numbers

| What | Value | Where |
|---|---|---|
| Checked | every second | `GraveTuning.WISP_TICKS` |
| Hint range / chance / distance | 96 blocks / 12% per check / 6-12 blocks from you | `WISP_RANGE`, `WISP_HINT_CHANCE`, `WISP_HINT_MIN`/`MAX` |
| Night range / chance | 48 blocks / 50% per check | `WISP_NIGHT_RANGE`, `WISP_NIGHT_CHANCE` |
| Yards looked for | 6 chunks round each player | `WISP_DISCOVER_CHUNKS` |

#### Spec facts

- `grave/Wisps`: yards are found from the structure references of loaded chunks round each player (as camps are) and kept per level in memory (not saved: they're rediscovered from chunk data after a restart); the command build registers its yard by hand. Players: `level.getPlayers(EntitySelector.NO_SPECTATORS)`. Particles go only to the player concerned: `ServerLevel.sendParticles(ServerPlayer, ...)` with count 0 (the offsets give the drift).
- Found yards: persistent player attachment `bubbas_reworked_mobs:found_yards` (a list of yard ids = start chunk), copied on death.
- `CryptYardPiece.wispTarget()`: the top of the crypt stairs, or above the gate.

#### Tests

- Milestone `WispsGameTest.walkingIntoAYardFindsIt`: a built yard is registered; a player outside hasn't found it; inside, it's found.
- Client part `wisps` (`WispsClientGameTest`): at night from outside a built yard, wisps forced over the graves plus hints; `wisps-1` looked at (blue soul flames over the yard and toward it). Natural ones are much sparser (one at a time).
- Full milestone suite 128/128 (develop's test cleanup merged several tests).

## Noble dead in gemmed armor (owner, 2026-11-22; built)

- With Bubba's Infused Gems installed, the armor and weapons of noble crypt sleepers carry gems (their burial finery). Woken, they fight with those effects; robbed, their gemmed gear is the prize. No hard dependency: the gems come through Infused Gems' own item modifier or api only when it's loaded.

### As built (2026-11-22, agent 2)

#### Player-facing

- **With Bubba's Infused Gems installed**, the sleepers of **noble crypts** (the armoured ones in full iron, chain or gold) wear **gemmed** gear: 1-2 gems in their armor, 1 in their weapon, infusion 2-4, tier 2-3 (a little richer than ordinary gemmed mob gear). Woken, they fight with those effects; rob or kill them and the gemmed pieces are the prize.
- Without Infused Gems, nothing changes (no hard dependency).
- In a test noble crypt 11 of 13 armoured sleepers carried gems: Infused Gems leaves some pieces without (its own rules on what takes a gem).

#### Spec facts

- Data, not code, decides the gems: item modifier `data/bubbas_reworked_mobs/item_modifier/noble_finery.json` uses Infused Gems' own `bubbas_infused_gems:socket_random_gems` (armor 1-2, weapon 1; infusion [2,3,3,4], tier [2,2,3], any variant) and loads only with the mod (`fabric:load_conditions` `fabric:all_mods_loaded`).
- `grave/NobleFinery.apply` runs it over a noble sleeper's gear when the crypt is built (`CryptYardPiece.Build.sleeper`, noble = armour chance 0.8 or more); if the modifier isn't loaded it does nothing. Loot context: chest param set with the sleeper's position; seeded per position.
- New build option `-PwithGems` (Reworked Mobs) puts Infused Gems on the test game's classpath, like `-PwithBeacons`.

#### Tests

- Milestone `NobleFineryGameTest.nobleDeadWearGemsOnlyWithInfusedGems`: a built noble crypt; without Infused Gems no gear carries its components; with `-PwithGems` most noble sleepers' gear does (11 of 13). Both runs pass; full milestone suite 129/129.
- No screenshot: gemmed gear looks the same on a sleeper.

## Noble crypt loot (built; agent's call, owner 2026-11-22: "do whatever you like with the noble crypt loot")

- The noble crypts are the endgame dungeon find. On top of the base table (gold, emeralds, iron, a golden apple, diamonds, enchanted books and gear) a second pool gives **1-2 burial goods**: a level 25-35 enchanted book, a worn but strongly enchanted diamond sword or chestplate, an enchanted golden helmet or chestplate, rarely an enchanted golden apple, a totem of undying or disc 13.
- **True Unbreaking books: 20 % per noble chest** with Bubba's Reworked Mining (its main source; ancient cities 4 %, End cities 3 %, bastions and ominous vaults 5 %).
- Grave Talisman 25 % and Totem of Warding 12 % per noble chest with Totems & Artifacts (unchanged).

## Milestone test objectives

- A gravestone places with any of the 16 rotations, takes text front and back, keeps it through save and load, and drops itself.
- The gravestone edit screen opens on placement and shows the lines where the placed stone draws them (client part `graves`: shots graves-edit and graves-edit-stone, plus -32x).
- Worldgen gravestones carry no real names: only blank text or glyphs.
- Each crypt wealth level generates with sleepers and its own loot table. The noble crypt's table can give a Grave Talisman and a Totem of Warding (with Totems & Artifacts).
- Crypt yards generate in each breakdown state, with iron bar fences and soul lights.

## Step 2 as built (2026-11-22): sizes, states of breakdown, the three crypts, the resting dead

Player-facing: `player-docs/Projects/Bubba's Reworked Mobs/Crypt Yards.md`. A sleeper in a niche wakes standing in front of it (`specs/undead.md`, Waking in tight spots).

### How it's built

- `grave/YardKinds`: `Size` (LONE 7-9, SMALL 11-13, MEDIUM 15-19, LARGE 21-27; crypt chance 0 / 0.5 / 0.6 / 0.8; picked 20/30/35/15 %), `State` (the numbers per state: bars gone, one high, mossy and broken posts, posts gone, stones askew, fallen and gone, open graves, flowers; picked kept 15 %, overgrown 30, sunken 20, collapsed 20, nearly gone 15), `Wealth` (PAUPER, FAMILY, NOBLE; by size: lone/small 50/50 pauper or family; medium 20 % pauper, 60 % family, 20 % noble; large 40 % family, 60 % noble).
- `CryptYardStructure` picks size, crypt, wealth and state; `CryptYardPiece` saves `State` and `Wealth` (pieces saved before step 2 read as overgrown and family). The piece's box is sized by wealth (pauper reaches 3 either side of the gate's line and 12 long; noble 12 either side and 14 long). Every roll is still made before reading the world.
- **The resting dead:** `SleeperBlock.RESTING` (block state, default false). `SleeperBlockEntity.disturb` returns 0 and `scheduleWake` does nothing for a resting sleeper, and `SleeperWaking.wake` refuses one: that covers noise, bells and robbing, and anything that schedules a wake. The Silk Touch loot copies `resting` with `pose` (datagen), so the item's `block_state` carries it. Yard graves place resting skeletons via `Build.restingSkeleton` (leather 70 %, `GraveTuning.RESTING_LEATHER_CHANCE`: helmet and chest, legs 60 %, boots 40 %, one of eight colours; a bow 15 %). The old skeleton/zombie roll is kept as an unused roll so later rolls don't shift.
- Loot (26.3 format): `chests/crypt_pauper` (2-5 rolls), `chests/crypt_noble` (4-8 rolls); Totems: `TotemsTuning.TALISMAN_LOOT` adds crypt_noble 25 %, `WardingTuning.LOOT` adds crypt_noble 12 % (keyed by id, no dependency; the old TODO there is resolved).
- Removed now-unused `GraveTuning.YARD_MAX_*`, `CRYPT_CHANCE`, `SKELETON_SHARE`.

### Builder's choices (owner may change)

1. **Shares** of sizes, states and wealths above; lone plots never have a crypt (too small for the stairs).
2. **Sunken** = plots settled one block into a hollow, the sleeper a block lower; stones lean (rotation), not sunk (a gravestone sunk one block would be hidden).
3. **Open graves** (collapsed yards) show the resting dead; buried loot isn't placed in open or sunken plots.
4. **Nearly-gone yards keep their dead** (75 % of graves) but lose their lantern.
5. **Ossuary niches** sometimes stack a second sleeper two above the floor niche.
6. **Noble armour:** 85 % of hall sleepers and the sarcophagus's (100 %) wear a full set: 15 % gold, 40 % chain, 45 % iron; zombies carry iron swords or axes 60 %. No enchanted gear on them (that's in the chests).
7. **The resting dead** are skeletons only and keep the "Sleeping Skeleton" name as items (no separate name or tooltip).
8. **Not built:** standalone crypts with a collapsed hillside entrance (the spec's other way in).

### Tests

- The core yard test now also checks every yard-grave sleeper is a resting skeleton.
- Milestone `GravesGameTest.restingDeadNeverWake` (new): an explosion three blocks off wakes an ordinary sleeper (control) but not a resting one; neither does a bell rung beside it, nor robbing by a player (at sensitivity 10, where an ordinary robbery wake is certain); mined with Silk Touch the item keeps `resting`, placed again it is still resting and sleeps through another explosion and 40 more ticks. `generatedTextIsGlyphsOrBlank` and `gravestoneIsASign` pass.
- Client part `yards` (new, `YardKindsClientGameTest`): every state, a lone plot, and pauper/family/noble yards on sky platforms: `yard-<kind>-above`, `yard-<kind>-gate`, `crypt-<wealth>` (night vision), `crypt-<wealth>-dark`, `crypt-noble-hall`. Looked at: kept, collapsed, nearly-gone (above), collapsed and noble (gate), pauper, noble and noble-hall crypts.
- Not yet tested (objectives): each wealth's chest rolls its own table in generated worlds (checked by eye); natural generation picks every state and size (shares are code, not measured).

## Desert and snowy yards (built 2026-11-22)

Crypt yards take a climate from the biome at their centre (`YardKinds.Climate`: TEMPERATE, DESERT for desert and badlands, SNOWY for base temperature under 0.15). Desert yards have sand plots, crypts of 70 % husks (30 % skeletons) buried under sand in the niches, and resting husks in the graves; snowy yards have snow plots, crypts of 70 % strays (30 % zombies) frozen in clear ice, and resting strays. Saved in the piece tag as `Climate`. Resting dead follow the climate (builder's call, see Open). Details in `specs/undead.md` "Undead variants".

## Open

- **Builder's choices to review:** the step 1 and step 2 lists above (stonecutter-only recipe, soul torches at the crypt stairs, shares of sizes/states/wealths, noble armour mix).
- **Resting dead in desert/snowy yards:** husks/strays (built) or skeletons everywhere (owner said "the skeleton kind")? Flip with `YardKinds.Climate.resting()`.
- **Standalone hillside crypts:** designed, not built; still wanted?
