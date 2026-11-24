# Reworked Mobs: biome creepers (docs report, 2026-11-24)

For the orchestrator / docs agent to merge into `specs/creepers.md` and the player docs, then delete.

## Player-facing

- Every creeper now wears a skin from the biome it first appeared in: tree biomes take the colours of their tree's bark, treeless biomes the colours of their ground. Look only: no behaviour change, no new mob.
- Skins: oak (forest, flower forest, swamp), birch (birch forest, old growth birch forest), spruce (all taigas, grove, windswept forest), jungle (all jungles), acacia (all savannas), dark oak (dark forest), mangrove (mangrove swamp), cherry (cherry grove), pale oak (pale garden), poplar (dappled forest), plains grass (plains, sunflower plains), meadow grass (meadow), sand (desert, beach), red sand (all badlands), snow (snowy plains, ice spikes, snowy slopes, snowy beach, frozen peaks, jagged peaks, frozen river), stone (stony peaks, stony shore, windswept hills, windswept gravelly hills), mycelium (mushroom fields), dripstone (dripstone caves), sulfur (sulfur caves).
- Everywhere else (rivers, oceans, lush caves, deep dark, the Nether, the End, modded biomes): the normal creeper look.
- A creeper keeps its skin for life, even if it walks into another biome. Creepers from older worlds get a skin from where they stand the first time they load.
- Snow creepers are almost plain white (the snow texture's own colours).
- Charged creepers keep the normal blue charge glow on top of their skin.
- The 32x pack ("Bubba's Mods 32x") has 32x versions of every skin.
- Packs can extend it: add biomes to the biome tag `bubbas_reworked_mobs:creeper_skin/<skin>`; a new tag `<ns>:creeper_skin/<name>` plus a texture `<ns>:textures/entity/creeper/biome/<name>.png` makes a new skin.

## Spec facts (specs/creepers.md, new section "Biome skins")

- Skin = attachment `bubbas_reworked_mobs:creeper_skin` (an Identifier) on the creeper: persistent (Fabric data attachment, saved with the entity) and synced to all clients (`AttachmentSyncPredicate.all()`). Value `bubbas_reworked_mobs:vanilla` means vanilla look.
- Picked in `ServerEntityEvents.ENTITY_LOAD` when the creeper has no skin yet: biome at its block position. This covers every spawn path (natural, spawner, egg, command with or without NBT, structure, conversion) because finalizeSpawn always runs before the entity is added, and it covers creepers from old saves the first time they load. No mixin on finalizeSpawn was needed.
- Mapping: biome tags `data/<ns>/tags/worldgen/biome/creeper_skin/<skin>.json`. A biome in several such tags takes the first by tag id (alphabetical). Skin id = tag namespace + the path after `creeper_skin/`.
- Client: `CreeperRendererSkinMixin` stores the texture in the render state (`CreeperRenderStateSkinMixin`) at extract time and swaps `getTextureLocation`'s result only when it is still vanilla's `creeper.png`, so another mod that changes the creeper texture wins. `CreeperPowerLayer` (charged overlay) untouched.
- Code: `creeper/look/CreeperSkins.java` (main), `client/creeper/CreeperSkinState.java`, `client/mixin/CreeperRenderer{,State}SkinMixin.java`. One line in `BubbasReworkedMobs` init.
- Textures: `tools/creeper-biome-textures.py` (method below). Rerun with `--base16`/`--base32` for a new base creeper.

### Art method (tools/creeper-biome-textures.py)

- Palettes: log side texture for tree biomes; ground block for treeless ones (sand, red_sand, snow, stone, mycelium_top, dripstone_block, sulfur); `grass_block_top` tinted by the grass colormap at the biome's temperature/downfall for plains (0.8/0.4) and meadow (0.5/0.8).
- OKLch match, per creeper colour: the palette's lightness range (3rd to 97th percentile, pixel-weighted) is stretched onto the creeper body's range (0.41 to 0.89); each creeper colour takes the palette colour whose lightness is nearest its stretched lightness; cost = dL^2 + 0.04 * dC^2 in range-normalised units, so near-ties go to the palette colour with the matching relative chroma (vanilla's greyish speckles take the palette's greyest shades). - Exact colours only (owner, 2026-11-24): every output pixel is a colour that exists in the source texture (after the grass tint for plains/meadow), with one exception (owner): the face's clearly darkest pixels take the source texture's darkest colour blended 50 % toward black (`FACE_BLEND`). The rest of the face maps like any other pixel. The face is left out of the range statistics.
- Dark face pixels, found by value on the head front (UV 8..16, 8..16): 16x vanilla, OKLab L below 0.405 (values 0.00 to 0.40; the rest of the face is 0.42 and up), 17 pixels: left eye (9,10) (9,11) (10,11); right eye (13,10) (13,11) (14,11); mouth top (11,12) (12,12); next row (11,13) (12,13) (13,13); full row (10,14) (11,14) (12,14) (13,14); bottom (10,15) (13,15). 32x CF32J, L below 0.395 (its eye and mouth rims are 0.40 and up), 53 pixels: eyes x 19-21 and 26-28, y 21-23 (3x3 each); mouth (22-25,25), (22-26,26), (20-27,27), (20-27,28), (20-27,29), (27,30), (27,31). Owner to confirm (row 13's right pixel (13,13), L 0.40, is in; (10,13), L 0.42, is out).
- Goal (owner): near-perfect camouflage against the biome's trunk or ground, with a readable face. Checked in game: a birch creeper peeking from behind a birch trunk, and a sand creeper against a sand wall.
- 32x (owner): the same recolour run on Classic Faithful 32x Jappa's own creeper (128x64) with CF32J's own log/ground textures (java-latest branch, commit 00321eab; script default is the owner's CF32J-2026r21 zip, `--cf32j <zip or folder>` to override). Grass at 32x: CF32J's grass_block_top with vanilla's colormap tint. Sulfur: CF32J has no sulfur texture, so the 32x sulfur skin uses vanilla sulfur's colours on CF32J's creeper. Credited in `mods/bubbas_reworked_mobs/CREDITS.md` (new) and `resourcepacks/bubbas_32x/CREDITS.md`; before release the Reworked Mobs Modrinth listing and player docs page need the CF32J credit and links too (specs/textures.md Credit). The 16x set stays vanilla creeper + vanilla textures.
- Script inputs: `--base16`, `--base32` (base creepers), `--mcsrc` (vanilla assets), `--cf32j` (CF32J zip or folder), `--sheet` (contact sheet).

## Milestone test objectives

- BiomeCreeperGameTest (core: `biomeCreepersKeepTheirSkins`): creepers spawned after `fillbiome` to birch forest, desert and nether wastes get birch, sand and vanilla, and keep them after a save/load round trip. Passed.
- BiomeCreepersClientGameTest (client part `biomecreepers`): every skin in a row, four per shot left to right, plus charged vanilla and charged birch, a birch creeper half behind a birch trunk and a sand creeper against sand, at 16x and 32x. Passed; screenshots looked at.

## Builder's choices (owner to review)

- Poplar (new in 26.3, dappled forest) added to the trunk list.
- Plains and meadow get separate grass skins (different grass tints).
- Windswept hills -> stone (mostly grass and stone, few trees); windswept forest -> spruce; swamp -> oak; wooded badlands -> red sand; frozen river -> snow; lush caves and deep dark stay vanilla.
- ENTITY_LOAD instead of a finalizeSpawn mixin (same result, also covers old saves and NBT summons).
