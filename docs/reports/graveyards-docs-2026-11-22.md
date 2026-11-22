<!-- dte:C1,B15,B16 -->
# Graveyards, crypts and gravestones, step 1: docs report (builder, 2026-11-22)

For merging into `specs/graveyards.md` (its "How it's built" section), `specs/undead.md` (Worldgen), `specs/totems-artifacts.md` (loot) and the player docs, then deletion. Branch `graveyards`. Step 1 = the gravestone block, one crypt yard and one crypt (the family vault). Step 2 (the rest of the variety) is still to come.

## Owner decisions during the build (2026-11-22)

- **Crypts are dim:** candles only. No soul lantern or soul torch inside a crypt.
- **One soul lantern per yard,** outside: on one gate post.
- **Trees and plants belong in yards, placed on purpose:** wild trees are cleared out of a yard, and the yard plants its own (one or two, by biome) plus flowers on some graves.
- **No yards in the sea:** biome rules, like vanilla's structures, rather than scanning for water. The yard's biome tag has no oceans, rivers or beaches, and every corner must be in it. (The yard the owner saw in water was one a test built by command on a river; the test now picks dry ground.)
- **Plots are coarse or rooted dirt,** so grass doesn't grow over them.
- **A grave lies on one level:** the sleeper under both plot blocks, so the plot blocks, the sleeper and the stone's footing share a y (the first build followed each column's ground and left some plots one up, one down).

## Player-facing (for player-docs)

**Gravestone** (new block, Bubba's Reworked Mobs)
- A stone headstone on a small plinth. It works exactly like a standing sign: text on the front and back, edited right after placing, dyes, glow ink, honeycomb to wax it, and 16 directions. It has a solid shape (you can't walk through it), stone sounds, and is mined with a pickaxe (it drops itself). Stacks to 16.
- Crafted in a stonecutter: 1 stone → 1 gravestone.
- Gravestones found in the world never carry names: they're blank or carved with a few enchanting-table glyphs.

**Crypt yards** (new structure)
- Fenced graveyards on fairly flat, dry ground in plains, forests, taigas, snowy plains, savannas, deserts, swamps, meadows, windswept hills and the pale garden. About one per 36 x 36 chunks (spacing 36, separation 12).
- An iron-bar fence with stone brick posts and a gate; one soul lantern on a gate post; a dirt path from the gate; rows of graves with stones (blank or glyphs), coarse or rooted dirt plots, sometimes flowers or weeds; a sleeping skeleton or zombie under most graves; now and then a weapon or tool half-buried in a plot (dig it out with a shovel). One or two old trees of the local wood stand where a grave would be (none in deserts, where dead bushes grow on the graves).
- Overgrown: some bars missing or one high, some stones turned askew, a few fallen.
- About 3 in 5 yards have stairs at the far end down to a crypt.

**Family vault** (the crypt under a yard)
- A stone brick room about 9 x 7 under the ground beyond the yard, lit only by candles. Sleeping skeletons and zombies lie in niches along both walls, some with helmets. At the far end, a small altar with candles and a chest of the family's things (bones, candles, gold nuggets, some iron, sometimes an emerald, a name tag or an enchanted book; with Bubba's Totems & Artifacts, sometimes a Grave Talisman).

**Command (ops):** `/bubbas_reworked_mobs graveyard place [crypt|nocrypt] [seed]` builds a yard with its gate at your feet, the yard running south. Built straight into the world: not a structure, so `/locate` doesn't find it and caretaker endermen treat it as ordinary blocks.

## How it's built (spec facts)

**Gravestone (`grave/GravestoneBlock`, `GravestoneBlockEntity`; client `client/grave/GravestoneRenderer`).** Vanilla's sign code reused, as the owner asked:
- `GravestoneBlock extends StandingSignBlock` with a stone `WoodType` named `bubbas_gravestone`. It is not registered with `WoodType.register`, so vanilla builds no sign sprite materials for it; its sounds are stone (`BlockSetType.STONE`, `SoundType.STONE`). Its own block entity type `bubbas_reworked_mobs:gravestone` and its own ticker. Shapes: cardinal rotations get the exact stone and plinth (12 x 14 x 4 on 14 x 2 x 6); diagonal rotations a 9 x 9 column. Can't respawn in it. Properties: strength 1.5 / 6, pickaxe required, no occlusion.
- `GravestoneBlockEntity extends SignBlockEntity`: max line width 60 (a sign's is 90), line height 10. `carve(SignText, registries)` sets the front text by loading it like a save, because vanilla's `setText` notifies the level and worldgen's block entities have none yet (that crashed the first run).
- Rendering: 26.3 signs are ordinary block models with four rotation templates (0, -22.5, -45, -67.5 degrees) turned by quarter turns. The gravestone does the same: `models/block/gravestone_rot_0..3` and a datagen blockstate dispatch over `rotation`. The renderer is vanilla's `AbstractSignRenderer` (colours, glow, outline, filtering) with its own text transform: on the south face 2 px in front of centre, centred at y 7.5, sign scale 1/96.
- Edit screen: vanilla's `SignEditScreen` reads `minecraft:textures/gui/signs/<wood type name>.png`, so the mod ships `bubbas_gravestone.png` (24 x 26, 32x twin 48 x 52).
- Data: loot drops itself (datagen), `#minecraft:mineable/pickaxe`, lang "Gravestone", stonecutting recipe `gravestone_from_stone_stonecutting`. Creative tab.

**Crypt yards (`grave/CryptYardStructure`, `CryptYardPiece`, `Graveyards`, `YardTrees`, `GraveText`, `GraveTuning`, `GraveyardCommand`).**
- **Code-built, not jigsaw (builder's choice, as the spec asked):** a vanilla `Structure` (type `bubbas_reworked_mobs:crypt_yard`) with one code-built piece (`crypt_yard`). The states of breakdown, sizes and contents are parameters of one builder, with no template files to keep in step. Being a real structure, it works with `/locate` and the Totem of Seeking's search, and caretaker endermen and the Totem of the Beginning leave it alone (the target world skips structure boxes).
- **Placement:** `data/bubbas_reworked_mobs/worldgen/structure/crypt_yard.json` (biome tag `#bubbas_reworked_mobs:has_structure/crypt_yard`, step `top_layer_modification`, no terrain adaptation) and `structure_set/crypt_yards.json` (random spread, spacing 36, separation 12, salt 20261122). `findGenerationPoint`: size 13-19 x 13-19 (odd), corner heights within 3 of each other, no water on them, not below sea level, and **the biome at every corner in the yard's tag** (vanilla checks only the start chunk's middle; owner, 2026-11-22: no yards in the ocean, preferring biome rules to a water scan). The yard's trees come from the biome at its middle.
- **After trees:** the step is `top_layer_modification` so the yard is built after vanilla's trees, and it clears trees and plants (logs, leaves, replaceables, vines, flowers, mushroom blocks, berry bushes, pumpkins) from its columns, 24 up, before building. The ground is read past trees and plants.
- **Same yard in every chunk:** the piece builds from its own seed and only writes inside the chunk being generated. Every random roll is made before looking at the world, so every chunk draws the same sequence. Each column reads its own ground.
- **Graves:** rows every 4 blocks, every other column, keeping 2 blocks either side of the path. A grave is a stone at its south end facing north over a two-block plot. The level is the plot's first block's ground; dirt is filled two below and the space cleared three above. The sleeper lies one under the plot, facing north. Graves that would cross a chunk border along z are left out (their level is read from one column, which only one chunk can see).
- **Sleepers:** skeleton or zombie, 50/50 in graves, 60/40 in the vault (only these two exist so far; husks and strays wait for their sleeper variants). Gear: skeletons a bow half the time, zombies a stone sword or iron shovel a quarter of the time; vault sleepers a chain or iron helmet 30 %.
- **Vault:** 7 wide, 9 long, 4 high inside, floor 5 below the yard's ground (`CRYPT_DEPTH` 6 to the floor block). Stone brick shell (cracked and mossy mixed in), polished andesite and cobblestone floor. Niches every other block on both long walls with a slab over each. Altar (chiseled stone bricks, 3 lit candles), chest (`bubbas_reworked_mobs:chests/crypt_family`), more candles along the walls, about a quarter of them lit, cobwebs. Stairs: stone brick stairs one step per block, brick walls, a low parapet only round the top two steps, a brick roof once below ground, a soul torch on each side wall at the top.
- **Loot:** `data/bubbas_reworked_mobs/loot_table/chests/crypt_family.json`, 3-6 rolls. The Grave Talisman joins it from Totems & Artifacts (`TotemsTuning.TALISMAN_LOOT`, 15 %, keyed by the table's id so there's no dependency; Totems' loot test skips tables that aren't installed).

## Numbers (`GraveTuning`)

| | Value |
|---|---|
| Yard size | 13-19 x 13-19 |
| Max corner slope | 3 |
| Yards with a crypt | 60 % |
| Crypt depth | 6 (floor 5 below ground) |
| Blank stones | 40 % (the rest 1-3 lines of glyph words) |
| Buried loot | 12 % of graves |
| Graves with a sleeper | 75 % |
| Skeletons in yard graves | 50 % |
| Trees per yard | 2 at 35 %, else 1 at 75 % (none in deserts) |
| Flowers / weeds on a plot block | 15 % / 25 % |
| Missing fence bars | 12 %; one high 18 % |
| Stones askew / fallen | 25 % / 10 % |
| Structure spacing / separation | 36 / 12 chunks |
| Grave Talisman in family vaults | 15 % (Totems & Artifacts) |

## Builder's choices (owner may change)

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
11. **Yard sleepers are skeletons and zombies only** for now (no husks or strays yet: those sleeper variants don't exist).
12. **One state of breakdown so far (overgrown).** Kept, sunken, collapsed and nearly gone come in step 2, with the other crypt wealth levels.

## Tests

- Core `ReworkedMobsCoreGameTest.graveyardStonesWithoutNamesAndSleepers` (body in `GravesGameTest.yardHasStonesWithoutNamesAndSleepers`): a built 13 x 13 yard has at least 4 gravestones, all front text glyphs or blank and no back text, and at least one sleeper.
- Milestone `GravesGameTest`: `generatedTextIsGlyphsOrBlank` (2000 stones: only glyphs or blank, about 40 % blank, a readable name fails the check), `gravestoneIsASign` (all 16 rotations; front and back text, dye and glow survive save and load; drops itself to a pickaxe).
- Client part `graves` (shots in `build/run/clientGameTest/screenshots/`): `graves-front/angle/close/close-glow/night` at 16x and 32x (four stones: epitaph, glyphs, blank, glow ink); `yard-natural-above/gate` (the nearest natural yard, seed 20261116, at 624, 304); `yard-built-above/gate/inside/rows` (a yard with a crypt built on flat plains); `crypt-vault`, `crypt-vault-back` (night vision) and `crypt-vault-dark` (candlelight only).
- Totems: `totemsAreFoundInLoot` passes standalone (crypt table skipped) and with `-PwithMobs`.
- Not yet tested (objectives): a crypt generates under a natural yard with its chest and niche sleepers (needs a deeper test field; checked by eye in the client part); crypt yards generate in each breakdown state (step 2); the gravestone's edit screen opens on placement (vanilla path, not exercised by a test).
