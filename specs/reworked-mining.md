<!-- dte:C7 -->
# Bubba's Reworked Mining (spec)

**Status: built** (mod 0.1.0, not released): the curve for every vanilla tool, stronger Efficiency and Unbreaking, rare finds, True Unbreaking, the quartz set. Numbers are first-pass, for the owner to tune by playing. Every number is in `MiningTuning`. **(builder)** marks the builder's own choices; the owner may change any of them.

Mod id `bubbas_reworked_mining`. Owner's design: "really just establishing a curve here and making enchantments (& gems) very worthwhile, such that you can go from 'tough' to 'highly powered machine' and get a real sense of progression." Slow the early game **a bit**, and the midgame **a lot**, through deepslate.

## Prior art

Nothing on 26.3 does this. The nearest mods go the other way (Deepslate Tweaks: deepslate as fast as stone) or rework tool tiers with new materials (Spelunkery, Harry's Armor and Tools, First Steps; none on 26.3). Nothing to build on, so it's built here, mostly from data (tool components, enchantment JSON, loot modifiers) where 26.3 allows.

## The curve (draft numbers, owner to tune by playing)

### Owner's targets

| Tool | Stone | Deepslate | Wear on deepslate |
|---|---|---|---|
| Wood | very slow (~3x) and wears out fast; "barely does anything" (owner) | can't mine it | n/a |
| Stone | a bit slower (~1.5x) | "incredibly slow" (owner, ~8x) | heavy |
| Copper / iron | about vanilla | slow (~2-3x) | "significant" (owner): ~3-5 durability per block |
| Diamond | about vanilla | "about normal/vanilla" (owner) | 1 per block |
| Netherite | faster than vanilla | faster than vanilla | 1 per block |

- **Efficiency is stronger** (owner): a steeper per-level curve. Target: Efficiency V iron mines deepslate about like bare diamond; Efficiency V diamond is the "machine".
- **Unbreaking is stronger** (owner). Target: Unbreaking III cancels most of the extra deepslate wear, so an enchanted iron pick is a real midgame tool.
- **Haste gems** (Bubba's Infused Gems) stack on top as the next step. No hard dependency.
- **Axes, shovels, hoes and swords: the same idea, gentler** (owner). Bare wood and stone tools feel weak on everything, and Efficiency matters on every tool, but chopping wood and digging dirt slow down far less than stone and deepslate. Target: wood and stone tools ~1.3-1.5x slower on their blocks, iron and up about vanilla.

### What players get

- **Pickaxes on stone and deepslate:**
  - Wood is very slow on stone and wears out fast. It can't mine deepslate: it still breaks it, slowly, but nothing drops.
  - Stone is a bit slower on stone and incredibly slow on deepslate, with heavy wear there.
  - Copper and iron are about vanilla on stone, and slow on deepslate with heavy wear.
  - Diamond is vanilla. Netherite is faster than vanilla.
- **Other tools** (axes, shovels, hoes, and pickaxes on non-stone blocks): wood and stone are a little slower, iron and up are vanilla, netherite a little faster.
- **Efficiency multiplies the tool's own speed**, so it matters on every tier. An Efficiency V iron pickaxe mines deepslate a little faster than a bare diamond one; Efficiency V diamond is as fast as in vanilla.
- **Unbreaking:** at level III a tool loses about 1 durability in 7 (vanilla 1 in 4). Armor gets the same extra protection on top of its vanilla odds.
- **Bare hands** mine slower than vanilla, a step below wooden tools: soft blocks (logs, planks, dirt, sand, gravel, leaves) about 1.5x slower, stone and deepslate very slow (and still no drops, as in vanilla). Blocks that break instantly in vanilla still do. Holding a non-tool item counts as bare hands.
- **Haste** (potion, beacon) and Infused Gems' Haste gems still multiply on top of all of this.
- No new tooltip lines (owner's minimal-tooltips preference).

### Numbers (MiningTuning)

Speed factor multiplies the tool's vanilla speed (0.5 = takes twice as long). Wear is the total durability lost per block (vanilla 1).

| Tier | Stone factor | Deepslate factor | Other factor | Wear on stone | Wear on deepslate |
|---|---|---|---|---|---|
| Wood | 0.33 (~3x slower) | 0.33, no drops | 0.70 | 2 | 3 |
| Stone | 0.67 (~1.5x) | 0.125 (~8x) | 0.75 | 1 | 3 |
| Copper | 1.0 | 0.40 (~2.5x) | 1.0 | 1 | 4 |
| Iron | 1.0 | 0.40 (~2.5x) | 1.0 | 1 | 3 |
| Gold **(builder)** | 0.67 | 0.33, no drops | 1.0 | 1 | 3 |
| Diamond | 1.0 | 1.0 | 1.0 | 1 | 1 |
| Netherite | 1.2 | 1.2 | 1.1 | 1 | 1 |
| Fist **(builder)** | 0.16 | 0.16 | 0.65 | none | none |

- In seconds (on the ground, no Haste): iron on deepslate about 1.9 s per block (vanilla 0.75 s); stone pickaxe on deepslate about 9 s; wood on stone about 3.4 s (vanilla 1.15 s).
- Fists (`MiningTuning.FIST`; Nether blocks use "other"), on the ground, no Haste, vanilla -> now: oak log or planks (hardness 2) 3.0 s -> 4.65 s (60 -> 93 ticks; our wooden axe takes 2.15 s, vanilla 1.5 s); dirt, sand 0.75 s -> 1.2 s; gravel 0.9 s -> 1.4 s; leaves 0.3 s -> 0.5 s; stone (not harvestable by hand) 7.5 s -> about 47 s; deepslate 15 s -> about 94 s. Hardness-0 blocks stay instant (vanilla's progress is speed / hardness).
- An iron pickaxe lasts about 83 deepslate blocks bare, about 580 with Unbreaking III.
- Efficiency multiplier by level 0-V: 1.0, 1.5, 2.1, 2.8, 3.5, 4.25. Higher levels (commands, other mods) keep adding 0.75 per level.
- Extra Unbreaking cancel chance at I/II/III: 0.333, 0.40, 0.43. Tools then keep a point with odds 1/3, 1/5, 1/7.

## Rare finds in plain stone (owner, 2026-11-22)

- **Stone-type blocks** (stone, granite, diorite, andesite, tuff) sometimes drop a find at very low odds: iron or gold nuggets (1-3) most of the time, rarely lapis, very rarely an emerald.
- **Deepslate:** slightly better odds, with a tiny chance of a diamond.
- **Placed blocks count the same as natural ones** (owner). So placing and re-mining stone farms finds at the tuned odds (0.5 % bare, 4 % with Fortune III); the owner accepted that.
- **Fortune raises the odds gently** (owner): each level doubles the chance (vanilla's `table_bonus` loot condition, as gravel's flint uses), so Fortune III makes finds noticeable but still rare.
- Silk Touch gives the block, never a find.
- **(builder)** Explosions can give finds, at the no-Fortune odds.

Numbers (`MiningTuning`):

- Find chance per block at Fortune 0/I/II/III: stone-type 0.5 %, 1 %, 2 %, 4 %; deepslate 0.8 %, 1.6 %, 3.2 %, 6.4 %.
- Find weights, stone-type: iron nugget 60, gold nugget 30, lapis 7, emerald 3.
- Find weights, deepslate: iron nugget 50, gold nugget 35, lapis 10, emerald 4, diamond 1. A bare pickaxe finds a diamond about once per 12,500 deepslate blocks.

## True Unbreaking (owner, 2026-11-22)

- A new enchantment, max level I: **the item never loses durability**.
- Works on anything with durability (tools, weapons, armor, elytra, the Glider, shields). Applied at an anvil.
- Can't combine with Mending (pointless together). Plain Unbreaking stays, stronger, as the everyday route.
- **Found only, rare:** as an enchanted book in chest loot. Never from the enchanting table or villagers, and never as a random enchantment on loot. Shows as a normal enchantment name.
- Book chance per chest:

| Chest | Chance |
|---|---|
| Ancient city | 4 % |
| End city treasure | 3 % |
| Bastion treasure | 5 % |
| Ominous trial vault, rare pool (`trial_chambers/reward_ominous_rare`) | 5 % |
| Reworked Mobs' noble crypt (`chests/crypt_noble`, only when that mod is installed) | 20 % (owner: its main source when both mods are installed) |

- Anvil cost 8, weight 1.

## Quartz tools and armor (owner, 2026-11-22)

Owner: a full quartz set "for use in the nether". These are new items (B15 only limits new blocks).

- **Quartz Sword, Shovel, Pickaxe, Axe, Hoe, Helmet, Chestplate, Leggings and Boots.**
- **Crafting (builder):** vanilla's iron shapes with **blocks of quartz** where the iron ingots go (sticks as usual). That's 4 quartz per block, so a pickaxe costs 12 nether quartz. Repaired at an anvil with blocks of quartz. Tag `bubbas_reworked_mining:quartz_tool_materials` (crafting and repair).
- **Iron level** (owner: "roughly iron/copper level"):
  - They harvest what iron harvests (so they mine diamond ore) and mine deepslate like iron: slow, with heavy wear.
  - **Ancient debris needs diamond** (owner), so quartz doesn't skip netherite progression.
  - Damage and attack speed are iron's. Armor protects like iron, lasts a little less, and sounds like iron when put on.
- **Faster on the Nether's natural blocks** (owner), 1.6x **(builder)**. Tag `bubbas_reworked_mining:nether_blocks` **(builder)**: netherrack, nether bricks (plain, cracked, chiseled, red) and the nether brick fence, slab, stairs and wall, basalt (plain, polished, smooth), blackstone and gilded blackstone, soul sand and soil, both nyliums, magma, both wart blocks, glowstone, shroomlight, nether quartz and gold ore, crimson and warped stems and hyphae (stripped too), bone blocks. Ancient debris and obsidian are left out.
- **Look** (owner: "a sick white look"): bright quartz white with faint rose-grey veins, vanilla-styled, at 16x and 32x (B16; 32x recoloured from Classic Faithful 32x Jappa's iron gear, shipped in the combined "Bubba's Mods 32x" pack). Agent-made until a person redoes it (`docs/art-todo.md`).
- **Creative tabs:** the tools right after the iron tools, the armor right after the iron boots.
- No new tooltip lines.
- **Prior art:** Quartz Tools (fatlard1993, MIT, on 26.3) has smooth-quartz tools but no armor and no Nether bonus. Ours is built here because quartz has to sit on this mod's curve; its code can be read, and reused with credit since it's MIT.

### Quartz numbers (MiningTuning)

- `QUARTZ` tier: stone 1.0, deepslate 0.40, other 1.0, **Nether 1.6**, wear 1 on stone and 3 on deepslate (iron's).
- Tools **(builder):** durability 220 (copper 190, iron 250), speed 6 and damage bonus +2 (iron's), enchantability 14 (iron's). Per-tool damage and attack speed are iron's.
- Armor **(builder):** durability multiplier 13 (copper 11, iron 15), so helmet 143, chestplate 208, leggings 195, boots 169. Protection 2/6/5/2 (iron's), toughness 0, enchantability 9 (iron's).

## Builder's choices (owner may want to look)

- **Efficiency is a multiplier, capped at vanilla for Efficiency V diamond.** Vanilla Efficiency adds a flat +26 speed at level V; on a slowed curve that would erase the curve (an Efficiency V stone pickaxe would out-mine a bare diamond one on deepslate). So it multiplies the tool's own speed, tuned to "Efficiency V iron about like bare diamond on deepslate". Low-tier tools gain less from Efficiency than in vanilla in absolute terms, and the curve keeps its shape. If "stronger" should mean faster than vanilla at the top, raise the last entries of `EFFICIENCY_MULTIPLIER`.
- **Gold tools** count as wood for harvesting (vanilla), so they can't mine deepslate either. They keep some speed on stone (0.67 of vanilla gold's 12).
- **Which blocks are "stone" and "deepslate"** (block tags `bubbas_reworked_mining:harsh_stone` / `harsh_deepslate`):
  - stone: stone, cobblestone, mossy cobblestone, granite, diorite, andesite, tuff, infested stone and cobblestone, and the 8 stone ores;
  - deepslate: deepslate, cobbled deepslate, infested deepslate and the 8 deepslate ores;
  - crafted variants (bricks, polished, tiles, slabs) use the gentle "other" factor.
- **Which blocks give finds:** stone, granite, diorite, andesite, tuff and deepslate. Not cobblestone, calcite or the ores.
- **Which tools are on the curve:** the vanilla pickaxe, axe, shovel, hoe and sword of each tier, through item tags `bubbas_reworked_mining:tier/<wood|stone|copper|iron|gold|diamond|netherite|quartz>`.
  - Other mods' tools stay vanilla unless a datapack adds them to a tier tag.
  - Swords keep vanilla wear (2 per block).
  - Spears, shears and maces aren't on the curve.
- **Fist factors:** each is wood's factor times the wood/stone ratio on that kind of block, so fists sit one step below wood the way wood sits below stone. Stone: 0.33 x (0.33 / 0.67) ~ 0.16. Other: 0.70 x (0.70 / 0.75) ~ 0.65. Deepslate copies stone (wood can't mine deepslate, so there is no ratio to follow).
- **What counts as a fist:** an empty hand or any item without a `minecraft:tool` component. A tool on a block it isn't meant for (a pickaxe on a log) keeps vanilla's speed 1, so it is a little faster than a fist there.
- **Wood wears 3 per deepslate block** even though nothing drops, so trying it is a bad deal.
- **Unbreaking on armor** gets the same extra roll as tools (the owner said Unbreaking is stronger; armor wasn't singled out).
- **True Unbreaking chest list and odds** (table above; the noble crypt 20 % is the owner's). Fabricator offices don't have it.
- **Explosions give finds** at no-Fortune odds.
- **Quartz:** the recipe, Nether speed 1.6x and block list, durability between copper and iron.
- **No armor trims on quartz armor:** none on the item icons (vanilla needs a per-item trim model) and none when worn (the armor isn't in `#minecraft:trimmable_armor`). Easy to add if the owner wants it.
- **Quartz art:** vanilla iron's silhouettes and shading bands, recoloured to quartz white with veins (script `mods/bubbas_reworked_mining/tools-local/make_quartz_textures.py`, listed in `docs/art-todo.md`). For the tools, the script maps iron's near-black outline and dark inner edge to a soft warm grey outline (132,122,121) and the quartz shadow tone (190,179,174) at 16x and 32x, so the heads read white beside iron's.

## How it's built

- **Speed:** a mixin on `Player.getDestroySpeed` multiplies the held tool's speed by its tier factor and Efficiency multiplier, and takes vanilla's flat Efficiency modifier (`minecraft:enchantment.efficiency/mainhand` on `mining_efficiency`) back out for tools on the curve.
  - Haste, Mining Fatigue, `block_break_speed` (gems), water and air apply after it, as in vanilla.
  - It runs on both sides, and both sides read only synced data (tags, components).
  - Like vanilla's Efficiency, the curve leaves the speed alone when the tool isn't meant for the block (speed 1, the hand's). A tool in no tier tag stays exactly vanilla.
  - Fists: when `MiningCurve.isFist(stack)`, `MiningCurve.speed` multiplies the held item's speed by `MiningTuning.FIST`'s factor for the block kind, in the same mixin.
- **Wear:** a mixin on `Item.mineBlock` replaces the Tool component's damage-per-block on harsh blocks. It still goes through `hurtAndBreak`, so Unbreaking rolls for every point.
- **Wood and gold can't harvest deepslate:** data only (`#harsh_deepslate` added to `minecraft:incorrect_for_wooden_tool` and `incorrect_for_gold_tool`).
- **Stronger Unbreaking:** Fabric `EnchantmentEvents.MODIFY_WITH_LOOKUP` appends a second `remove_binomial` item-damage effect to vanilla Unbreaking. Vanilla's file is untouched.
- **True Unbreaking:** data, `data/bubbas_reworked_mining/enchantment/true_unbreaking.json` (`minecraft:set` item damage to 0, `exclusive_set` Mending, in no enchantment tag).
- **Finds and books:** `LootTableEvents.MODIFY` adds one pool per table (skipped when a datapack replaced the table). The find pool's conditions: not `minecraft:tool/can_silk_touch`, `survives_explosion`, and a `table_bonus` on Fortune.
- **No block states changed**, so the mod changes no vanilla block. The quartz items still make Fabric's registry sync require the mod on both sides.

## Tests

- **Core** (`BubbasReworkedMiningCoreGameTest`, every build):
  - `curveSlowsDeepslateAndEfficiencyMultiplies`: tier factors and the Efficiency V multiplier reach `Player.getDestroySpeed`. Fists: an empty hand on an oak log gets `FIST.other()`, a stick on stone `FIST.stone()`, a wooden axe beats a fist on a log, and short grass still breaks instantly by hand (`getDestroyProgress >= 1`).
  - `deepslateWearsToolsAndWoodCantHarvestIt`: iron wear on deepslate and on stone; wood can't harvest deepslate, stone can.
  - `findsFromStoneButNotWithSilkTouch`: 3000 rolls with Fortune III give about the tuned rate; Silk Touch gives zero.
  - `unbreakingStrongerAndTrueUnbreakingIsTotal`: Unbreaking III keeps about 1/7. True Unbreaking takes zero damage on an elytra, is incompatible with Mending, and is in none of the enchanting table, tradeable or random-loot tags.
  - `quartzIsIronLevelAndFasterInTheNether`: quartz on netherrack is 1.6x iron; on deepslate it equals iron; no ancient debris; harvests diamond ore; in `#pickaxes` and `#chest_armor`.
- **Milestone** (`MiningMilestoneGameTest`): `trueUnbreakingInChests`: 2000 bastion treasure rolls give about 5 % books; True Unbreaking takes zero damage on armor.
- **Client part `quartz`** (`-PclientTests=quartz`), each at 16x and 32x:
  - two armor stands (quartz next to iron) with pickaxes;
  - the quartz pickaxe and sword held in first person, with the whole set in the hotbar;
  - the player wearing the set in third person.

  Checked by eye: textures load (no missing-texture checkerboard), the quartz armor is clearly whiter than iron with soft veins, the tool heads read white beside iron's, and the 32x pack switches on. Items held by armor stands render dark for iron and quartz alike (lighting, not the texture).

## Milestone test objectives

- Every tier's speed and wear on stone, deepslate and "other" match `MiningTuning` (the core test covers iron, wood and the wood axe).
- Efficiency and Unbreaking levels give the stronger values, including Unbreaking I and II and the armor odds.
- Finds drop from stone and deepslate (placed or natural) at the tuned odds, with the deepslate diamond weight, never with Silk Touch; explosions give them only at no-Fortune odds.
- Haste and gem `block_break_speed` stack on top.
- Fist factors on deepslate and a Nether block match `MiningTuning.FIST`; Haste and gem `block_break_speed` stack on fists.
- True Unbreaking stops all durability loss, isn't offered by the enchanting table or villagers, and can't combine with Mending (core); books in noble crypts at about 20 % with Reworked Mobs.
- Every quartz recipe crafts; repairing with a quartz block works; quartz on each Nether block is 1.6x; quartz armor protection values.

## Open (owner)

- Exact numbers: speeds, wear, Efficiency and Unbreaking curves, find odds, book odds (tune by playing; all in `MiningTuning`).
- How progress shows to the player (tooltips stay minimal, per the owner; today there are no new tooltip lines).
- Builder's calls to review (see "Builder's choices"):
  - Efficiency as a multiplier capped at vanilla for Efficiency V diamond (raise it if "stronger" should beat vanilla at the top);
  - gold can't mine deepslate;
  - which blocks count as stone or deepslate, and which give finds;
  - swords, spears, shears and maces off the curve;
  - fist factors (0.65 soft, 0.16 stone and deepslate), and whether a tool on the wrong block (pickaxe on a log) should count as a fist;
  - Unbreaking's extra roll on armor;
  - explosions give finds;
  - the quartz recipe (12 quartz per pickaxe), Nether speed 1.6x and block list, quartz durability 220 and armor multiplier 13;
  - no armor trims on quartz armor.

- **Fists "weaker" (owner, 2026-11-24):** the poster's "slower and weaker" means mining only; fist attack damage stays vanilla.
