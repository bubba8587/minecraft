# Relic maps and relic vaults: docs report (2026-11-24)

Builder report for Bubba's Totems & Artifacts (specs/totems-artifacts.md, C6). For the docs agent to merge into the spec
("Relic maps", "Relic vaults", "Milestone test objectives") and the player vault, then delete this file. Owner's design
(2026-11-24) is in the task; below is what was built and the choices made.

## Player-facing (for player-docs)

- **Relic Map Shreds** turn up in many chests: dungeons, mineshafts, shipwrecks, buried treasure, desert and jungle
  temples, igloos, strongholds, mansions, outposts, ancient cities, ruined portals, ocean ruins, villages (cartographers
  most), trial chamber corridors, bastions, and (with Reworked Mobs) crypt chests, monster camp chests, Fabricator
  offices and the caretaker endermen's cairn trades.
- Each shred is named like **"Wondrous Crab Relic Map (1/9)"** or **"Egregious Zealot Relic Map (2/4)"**. The tooltip
  reads like a found scrap: "A torn scrap of an old map, the ink faded but the crest still clear." / "2 of 4 pieces" /
  "Find the rest of the Egregious Zealot map (4 pieces) and put them together."
- Put shreds of the **same name and the same size** together in any crafting grid (the 2x2 in your inventory, a crafting
  table, or a crafter): their pieces add up (2/9 + 2/9 + 5/9 = 9/9). You can't go over the whole, mix names, or mix /4 and /9.
- A **whole** map turns into a real filled map (an explorer map with a red X) the moment it's in your inventory, named
  "Wondrous Crab Relic Map". It leads to that relic's vault.
- Every world has exactly one vault per name and size, fixed from the world seed when the world is made. Once you've
  found and emptied the Wondrous Crab vault, other Wondrous Crab maps (same size) lead to an empty room.
- **Relic vaults** are buried about 20 blocks under the ground, with no way in from the surface: an old stone stair
  climbs 7 steps from the vault's door and stops in solid rock. Dig down, or follow the map. The /9 vaults are bigger,
  farther out and much richer than the /4 ones.
- Inside: a sealed room behind an iron door (a lever inside opens it; nothing outside does), polished deepslate and
  quartz, sea-lantern lights, a red runner to a quartz pedestal with the vault's prime treasure (a totem) on show,
  stacked chests, a table with cushioned chairs, armour stands in gear. With Reworked Mobs a skeleton in diamond (or
  iron) sits by the pedestal, a sleeper that wakes like any other.
- Some **noble crypts** hide a smaller vault: its stair starts just behind the crypt's far wall (between the two family
  chests), so you only find it by breaking through. Those vaults have no name and no map.

## Design and numbers

### The item
- One item, `bubbas_totems_artifacts:relic_map_shred`, stacks to 16, uncommon. Data component
  `bubbas_totems_artifacts:relic_shred` = `{name, size, count}` (name like `wondrous_crab`, size 4 or 9, count 1..size).
- Names: 8 adjectives x 8 nouns = 64 names; with the two sizes, 128 maps per world.
  - Adjectives: Wondrous, Egregious, Forgotten, Gilded, Sunken, Whispering, Crooked, Radiant.
  - Nouns: Crab, Zealot, Lantern, Serpent, Crown, Owl, Anvil, Bell.
- Look: `custom_model_data` string picks the texture: `4` (a big torn corner), `9` (a small scrap, older paper), `whole`.
- Recipe type `bubbas_totems_artifacts:relic_shreds` (special recipe, `data/.../recipe/relic_shreds.json`), shapeless,
  2+ shreds. The crafting result is a whole shred; it becomes the filled map when it ticks in a player's inventory
  (recipes have no level to make a map in). A whole shred out of a crafter becomes a map once picked up.
- The finished map: vanilla filled map, scale 2 (like explorer maps), biome preview (water outlines), red X decoration
  on the vault's door, item name "<Name> Relic Map", and it keeps the relic_shred component.
- Loot: function `bubbas_totems_artifacts:random_relic_shred`: a random name of the 64; /9 35%, else /4; 1 piece 80%,
  2 pieces 15%, 3 pieces 5% (never a whole map).

### Shred odds per chest (one roll each)
| Table | Chance |
|---|---|
| simple_dungeon | 30% |
| abandoned_mineshaft | 20% |
| shipwreck_map / treasure / supply | 40% / 30% / 15% |
| buried_treasure | 40% |
| desert_pyramid, jungle_temple, igloo_chest | 30% |
| stronghold corridor / crossing / library | 25% / 25% / 40% |
| woodland_mansion | 30% |
| pillager_outpost, ancient_city | 25% |
| ruined_portal | 15% |
| underwater_ruin big / small | 30% / 15% |
| village cartographer / temple / weaponsmith, toolsmith / houses | 30% / 15% / 10% / 8% |
| trial_chambers/corridor, bastion_other | 10% |
| Reworked Mobs crypt noble / family / pauper | 50% / 35% / 15% |
| Reworked Mobs monster_camp, camp_room | 30% |
| Reworked Mobs fabricator_office (+_with_gems) | 25% |
| Reworked Mobs gameplay/enderman_trade (cairn trades) | 10% |

Reworked Mobs tables are keyed by id (`LootTableEvents.MODIFY`), so no dependency.

### Where the named vaults are (seeded at world start)
- `RelicSites`: from the world seed alone (Xoroshiro random, seed xor a fixed salt), every (name, size) pair in a fixed
  order gets one chunk: a random angle and a distance from the world's middle, /4 between 400 and 2400 blocks, /9
  between 1600 and 4800. A candidate within 20 chunks of an earlier site is re-rolled (up to 48 tries). Same seed,
  same sites, always; nothing is generated or stored when a map is made.
- A custom structure placement `bubbas_totems_artifacts:relic_sites` (structure set `relic_vaults`) says "this chunk"
  exactly for those 128 chunks, so the vaults are real worldgen structures, built when their chunk generates.
  Structure `bubbas_totems_artifacts:relic_vault`, Overworld biomes, step underground_structures.
- The vault's door is at the chunk's middle, floor 20 below the ground there (ocean floor under water), never lower
  than 12 above the world's bottom.
- Vanilla `/locate structure` and explorer-map searches only know random-spread and ring placements, so they don't find
  these; the maps (and `/bubbas_totems_artifacts relic_map`) do.

### The vault (RelicVaultPiece, code-built like crypts)
| | /4 (small) | /9 (grand) | Crypt vault |
|---|---|---|---|
| Room inside (w x l x h) | 9 x 9 x 4 | 13 x 11 x 4 | 7 x 7 x 4 |
| Floor depth | 20 under ground | 20 under ground | 12 under ground (7 under the crypt floor) |
| Chests (stacked two high) | 3 | 6 | 2 |
| Armour stands | 2 (iron or gold sets) | 4 (iron, diamond or gold) | 1 (chain) |
| Tables with cushioned chairs | 1 | 2 | 1 |
| Prime loot on the pedestal | a random totem | a random totem incl. Beginning/End | a totem or an enchanted golden apple (50/50) |
| Sitting guard (Reworked Mobs) | iron set | diamond set | none |
| Loot table | chests/relic_vault_small | chests/relic_vault_grand | chests/relic_vault_crypt |

- Stair: 3 wide, stone bricks (cracked and mossy), 7 steps from the door's landing; a soul lantern at its foot. Named
  vaults: a stone wall after the top landing. Crypt vaults: the top landing is just south of the noble crypt's far wall,
  at the crypt's floor height.
- Room: deepslate tile walls with polished deepslate pillars, polished deepslate border and smooth quartz / polished
  diorite checker floor, sea lanterns set in the ceiling every 3 blocks, two pearlescent froglights in the floor beside
  the chiseled quartz pedestal, a red carpet runner, an iron door with a lever inside.
- Prime loot shown with Reworked Mobs' loot display (`api/Crypts.placeDisplay`); without Reworked Mobs, in a glow item
  frame on the pedestal. Guard: a sitting skeleton sleeper (`api/Crypts.placeSittingSleeper`) only with Reworked Mobs;
  with Infused Gems its chestplate and sword are gemmed (`loot_table/relic_vault/guard_gear.json`, loads only with Gems).
- Cushions: vanilla 26.3 Cushion entities on the stair chairs (one colour per table: red, cyan, purple or orange).

### Vault chest loot (per chest; tables written by `mods/bubbas_totems_artifacts/tools-local/make_relic_loot.py`)
| | Treasure rolls | Shred rolls | Totem chance | Rare chance | Infused Gems rare rolls |
|---|---|---|---|---|---|
| Grand | 4-7 (diamonds 1-4, enchants 25-39) | 2-4 | 45% | 40% | 1-2 |
| Small | 3-5 (diamonds 1-2, enchants 15-30) | 1-3 | 30% | 15% | 0-2 |
| Crypt | 2-4 | 1-2 | 20% | none | 0-1 |

Totem weights: Keeping, Gathering, Fortification, Growth, Seeking, Herding, Clear Skies 3 each; Warding and Grave
Talisman 2; the Beginning and the End 1. Rare: enchanted golden apple, netherite scrap, totem of undying, Pigstep, heart
of the sea. Infused Gems: its `gems/rare` table rolled into the drops at open time (`LootTableEvents.MODIFY_DROPS`),
only with that mod loaded.

### Noble crypt attachment (no hard dependency either way)
- Reworked Mobs has a new `api/Crypts` (stable, add-don't-change): `setNobleAttachment(...)`, `attachments(piece)`,
  `placeDisplay`, `placeSittingSleeper`. `CryptYardStructure` and `GraveyardCommand.build` add whatever the attachment
  returns; nothing is registered unless Totems & Artifacts is loaded.
- Totems & Artifacts registers the attachment through its SleeperHook (only when Reworked Mobs is loaded): 35% of noble
  crypts (roll from the crypt's seed) get a crypt vault.
- Decision: crypt vaults are **unnamed** (no map leads there), so each name keeps exactly one place. (Open question
  below.)
- `CryptYardPiece` gained `seed()` and `nobleFarWall()`.

### Commands (op)
- `/bubbas_totems_artifacts relic_map <name> <4|9>`: the finished map for that relic.
- `/bubbas_totems_artifacts relic_shred <name> <4|9> <count>`: a shred.
- `/bubbas_totems_artifacts relic_vault <small|grand|crypt>`: builds a vault with its door at your feet (room to the south).

## Spec facts (HOW, for specs/totems-artifacts.md)
- Package `relic/`: RelicNames, RelicShred (component), RelicMapShredItem, RelicMaps, RelicShredRecipe,
  RelicShredFunction, RelicSites, RelicVaultPlacement, RelicVaultStructure, RelicVaultPiece, RelicLoot, RelicTuning,
  RelicCommand, Relics (wiring). All numbers in RelicTuning (and the loot script).
- Hand-rolled vs library (B9): no library places structures at seed-derived fixed sites; vanilla's placements are
  grid (random_spread) or rings, and neither can give "exactly one site per name". Code-built pieces match the crypts.
- Vanilla subsystems used: structure/structure set/StructurePlacement registry, StructurePiece, CustomRecipe +
  RecipeSerializer, LootItemConditionalFunction, MapItem/MapItemSavedData explorer-map decorations, Cushion and armour
  stand entities, chest loot tables.

## Milestone test objectives (RelicMapsGameTest; two run in the core suite)
- (core) shreds combine only with the same name and size, totals no more than the whole; 2x2 and 3x3; the recipe loads and is found for the inventory grid.
- (core) every vault kind builds sealed (flood fill stays inside), iron door, stair climbs 7 and (named) stops at stone, chests, stands, cushions, prime loot shown.
- a crafter combines two shreds.
- sites: same seed same sites, two seeds differ, 128 sites in unique chunks, in their distance bands, 20+ chunks apart; the structure set uses relic_sites; lookup by chunk.
- a whole shred carried turns into a filled map whose X is on its name's site; /4 and /9 of one name lead to different places.
- loot: dungeon shred rate about 30%; grand vault chests have a totem about 45% and 2+ shreds each.
- (with Reworked Mobs) a noble crypt's vault: stair top just behind the far wall, the wall intact, the vault under the crypt and sealed; about 35% of noble crypts have one.
- Not covered by a test: a vault actually generating at its site in a real (non-flat) world (game test worlds are flat); with Infused Gems loaded (no -PwithGems runtime flag yet).

## Screenshots (client part `relics`; copies in the session scratchpad `relics/`)
relics-inventory(-32x), relics-tooltip(-32x), relics-map (finished map in hand), relics-vault-grand-door/-back/-side/
-pedestal/-stair, relics-vault-small-door, relics-crypt-wall/-stair/-vault (with -PwithMobs), and the pedestal with
Totems alone (glow item frame).

## Prior art
`tools/prior-art.py` found nothing for "map fragments", "treasure map pieces", "buried vault structure" (it may have
been offline). From memory: Quark's Pathfinder's Quills (maps to biomes), Explorer's Compass / Nature's Compass
(locate structures), vanilla explorer and buried-treasure maps, YUNG's structure mods. None split a map into named
shreds that lead to one fixed seeded site per name; ours adds the collect-and-combine loop and the one-place-per-name rule.

## Found along the way
- Reworked Mobs' `loot_table/chests/fabricator_office_with_gems.json` has its gem pool condition as
  `"conditions": [{"condition": ...}]`, the pre-26.x form; 26.3 reads `"condition": {"type": ...}`, so that 1-in-3
  harmful-gem pool rolls every time. Not touched here.
- `CryptWardsGameTest.cryptStatuesStandInEveryWing` fails with -PwithMobs on develop's code too ("PAUPER: the statues
  split the crypt floor into 2 walkable areas"), likely from the two-deep pauper niches; not touched here.

## Open questions for the owner
1. Crypt vaults are unnamed (no map leads to them). Should some carry a name too (that would make two places per name)?
2. The map appears only once the whole shred is in your inventory (crafting results can't make maps). OK, or should
   right-clicking the whole shred "unfold" it instead?
3. 35% of noble crypts with a vault, and 45% totem chance per grand vault chest (about 3 totems plus the prime one per
   grand vault): tune after play.
4. Vanilla `/locate` can't find relic vaults; add our own locate command, or have the Totem of Seeking learn them?
