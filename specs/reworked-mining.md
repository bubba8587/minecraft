<!-- dte:C7 -->
# Bubba's Reworked Mining (spec)

**Status (2026-11-22): everything below is built** (mod 0.1.0, not released): the curve for every vanilla tool, stronger Efficiency and Unbreaking, rare finds, True Unbreaking, the quartz set. Numbers are first-pass, for the owner to tune by playing.

Mod id `bubbas_reworked_mining`. Owner's design (2026-11-22): "really just establishing a curve here and making enchantments (& gems) very worthwhile, such that you can go from 'tough' to 'highly powered machine' and get a real sense of progression." Slow the early game **a bit**, and the midgame **a lot**, through deepslate.

## Prior art (2026-11-22)

Nothing on 26.3 does this. The nearest mods go the other way (Deepslate Tweaks: deepslate as fast as stone) or rework tool tiers with new materials (Spelunkery, Harry's Armor and Tools, First Steps; none on 26.3). Nothing to build on, so it's built here, mostly from data (tool components, enchantment JSON, loot modifiers) where 26.3 allows.

## The curve (draft numbers, owner to tune by playing)

Speeds and wear compared to vanilla:

| Tool | Stone | Deepslate | Wear on deepslate |
|---|---|---|---|
| Wood | very slow (~3x) and wears out fast; "barely does anything" (owner) | can't mine it | n/a |
| Stone | a bit slower (~1.5x) | "incredibly slow" (owner, ~8x) | heavy |
| Copper / iron | about vanilla | slow (~2-3x) | "significant" (owner): ~3-5 durability per block |
| Diamond | about vanilla | "about normal/vanilla" (owner) | 1 per block |
| Netherite | faster than vanilla | faster than vanilla | 1 per block |

- **Efficiency is stronger** (owner): a steeper per-level curve. Target: Efficiency V iron mines deepslate about like bare diamond; Efficiency V diamond is the "machine".
- **Unbreaking is stronger** (owner): target: Unbreaking III cancels most of the extra deepslate wear, so an enchanted iron pick is a real midgame tool.
- **Haste gems** (Bubba's Infused Gems) stack on top as the next step. No hard dependency.
- **Axes, shovels, hoes and swords (owner, 2026-11-22): the same idea, gentler.** Bare wood and stone tools feel weak on everything, and Efficiency matters on every tool, but chopping wood and digging dirt slow down far less than stone and deepslate (target: wood/stone tools ~1.3-1.5x slower on their blocks, iron and up about vanilla).

## Rare finds in plain stone (owner, 2026-11-22)

- Mining plain natural **stone** (and its natural variants: builder lists them) sometimes drops a find at very low odds: iron or gold nuggets most of the time, very rarely lapis or an emerald.
- **Deepslate:** slightly better odds, with a tiny chance of a diamond.
- **From any stone and deepslate, placed or natural (owner, 2026-11-22):** the natural/placed distinction was built, then removed at the owner's call. Placing and re-mining stone therefore farms finds at the tuned odds (0.5 % bare, 4 % with Fortune III); the owner accepted that.
- Silk Touch gives the block, no find. **Fortune raises the odds gently** (owner, 2026-11-22): each level roughly doubles the chance (vanilla's `table_bonus` loot condition, as gravel's flint uses), so Fortune III makes finds noticeable but still rare.

## True Unbreaking (owner, 2026-11-22)

- A new enchantment, **True Unbreaking**: the item never loses durability. Data-driven (an `item_damage` effect set to zero).
- **Found only, rare:** in loot: ancient city, End city, bastion and ominous trial vault chests, and Reworked Mobs' noble crypts (its main source when both mods are installed). Never from the enchanting table or villagers.
- Works on anything with durability (tools, weapons, armor, elytra, the Glider, shields). Applied at an anvil.
- Incompatible with Mending (pointless together). Plain Unbreaking stays, stronger, as the everyday route.

## Quartz tools and armor (owner, 2026-11-22)

- A full quartz set: pickaxe, axe, shovel, hoe, sword and armor, "for use in the nether".
- **Roughly iron/copper level** in durability, damage and protection, on the curve above. They count as iron-level for what they can harvest, so they mine deepslate like iron does.
- **Faster on all Nether blocks** (owner): netherrack, nether bricks, basalt, blackstone, soul sand and soil, nylium, magma, wart blocks, nether quartz and gold ore, and the rest of the Nether's natural blocks (a block tag the builder fills). **Ancient debris stays diamond-only** (owner, 2026-11-22), so quartz doesn't skip netherite progression.
- **Look (owner): "a sick white look"**: bright quartz white with subtle veining, vanilla-styled, at 16x and 32x (B16). Agent-made until a person redoes it (`docs/art-todo.md`).
- Made from nether quartz (recipe: builder's proposal, probably quartz blocks like iron ingots, on a crafting table).
- Prior art: Quartz Tools (fatlard1993, MIT, on 26.3, 611 downloads) has smooth-quartz tools but no armor and no Nether bonus. We build our own, since quartz has to sit on this mod's curve. Its code can be read, and reused with credit since it's MIT.
- These are new items (B15 only limits new blocks).

## As built (2026-11-22, builder: agent 3)

Every number is in `MiningTuning`. **(builder)** marks the builder's own choices; the owner may change any of them.

### Step 1: the curve, rare finds, True Unbreaking

#### Player-facing behaviour

- **Pickaxes on stone and deepslate:**
  - Wood is very slow on stone and wears out fast. It can't mine deepslate at all: you can still break it, slowly, but it drops nothing.
  - Stone is a bit slower on stone and incredibly slow on deepslate, with heavy wear there.
  - Copper and iron are about vanilla on stone, and slow on deepslate with heavy wear.
  - Diamond is vanilla. Netherite is faster than vanilla.
- **Other tools** (axes, shovels, hoes, and pickaxes on non-stone blocks): wood and stone are a little slower, iron and up are vanilla, and netherite is a little faster.
- **Efficiency is stronger relative to each tool:** it now multiplies the tool's speed, so it matters on every tier. An Efficiency V iron pickaxe mines deepslate a little faster than a bare diamond one, and Efficiency V diamond is as fast as in vanilla.
- **Unbreaking is stronger:** at level III a tool loses about 1 durability in 7 (vanilla is 1 in 4). Armor gets the same extra protection on top of its vanilla odds.
- **Haste (potion, beacon) and Infused Gems' Haste gems** still multiply on top of all of this.
- **Rare finds:** mining stone, granite, diorite, andesite or tuff sometimes drops iron or gold nuggets (1-3), rarely lapis, very rarely an emerald. Deepslate has slightly better odds and a tiny diamond chance. Each Fortune level doubles the odds. Silk Touch never gives finds. Placed blocks give finds like natural ones (**owner, 2026-11-22: no natural/placed distinction**; this replaces the spec's "only from natural blocks" bullet). Explosions can give finds (at the no-Fortune odds).
- **True Unbreaking** (new enchantment, max level I): the item never loses durability. It works on anything with durability (tools, weapons, armor, elytra, shields, ...) and is applied at an anvil. It can't combine with Mending. It's never offered by the enchanting table or villagers and never appears as a random enchantment on loot: it's found only as an enchanted book in ancient city, End city, bastion treasure and ominous trial vault (rare) chests, and in Reworked Mobs' noble crypt chests.
- Tooltips: no new tooltip lines (minimal-tooltips preference). True Unbreaking shows as a normal enchantment name.

#### Numbers (MiningTuning)

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

What that means in seconds (on the ground, no Haste): iron on deepslate is about 1.9 s per block (vanilla 0.75 s), stone pickaxe on deepslate about 9 s, wood on stone about 3.4 s (vanilla 1.15 s). An iron pickaxe lasts about 83 deepslate blocks bare, about 580 with Unbreaking III.

- Efficiency multiplier by level 0-V: 1.0, 1.5, 2.1, 2.8, 3.5, 4.25. Higher levels (commands, other mods) keep adding 0.75 per level.
- Extra Unbreaking cancel chance at I/II/III: 0.333, 0.40, 0.43. Tools then keep a point with odds 1/3, 1/5, 1/7.
- Find chance per block, at Fortune 0/I/II/III: stone-type 0.5 %, 1 %, 2 %, 4 %; deepslate 0.8 %, 1.6 %, 3.2 %, 6.4 %.
- Find weights: stone-type: iron nugget 60, gold nugget 30, lapis 7, emerald 3. Deepslate: iron nugget 50, gold nugget 35, lapis 10, emerald 4, diamond 1. So a bare pickaxe finds a diamond about once per 12,500 deepslate blocks.
- True Unbreaking book chance per chest: ancient city 4 %, End city treasure 3 %, bastion treasure 5 %, trial chambers `reward_ominous_rare` 5 %, Reworked Mobs' `chests/crypt_noble` 20 % (applies only when that mod is installed). Anvil cost 8, weight 1.

#### Builder's choices (owner may want to look)

- **(builder) Why Efficiency changed shape.** Vanilla Efficiency adds a flat +26 speed at level V. On a slowed curve that would erase the curve: an Efficiency V stone pickaxe would out-mine a bare diamond one on deepslate. So Efficiency became a multiplier on the tool's own speed, tuned to the spec's target ("Efficiency V iron about like bare diamond on deepslate") and capped so Efficiency V diamond matches vanilla. As a result, low-tier tools gain less from Efficiency than in vanilla in absolute terms, and the curve keeps its shape. If the owner wants "stronger" to mean faster than vanilla at the top, raise the last entries of `EFFICIENCY_MULTIPLIER`.
- **(builder) Gold tools** count as wood for harvesting (vanilla), so they can't mine deepslate either. They keep some speed on stone (0.67 of vanilla gold's 12).
- **(builder) Which blocks are "stone" and "deepslate"** (block tags `bubbas_reworked_mining:harsh_stone` / `harsh_deepslate`): stone, cobblestone, mossy cobblestone, granite, diorite, andesite, tuff, infested stone/cobblestone and the 8 stone ores; deepslate, cobbled deepslate, infested deepslate and the 8 deepslate ores. Crafted variants (bricks, polished, tiles, slabs) use the gentle "other" factor.
- **(builder) Which blocks give finds:** stone, granite, diorite, andesite, tuff (stone-type) and deepslate. Not cobblestone, calcite or the ores.
- **(builder) Which tools are on the curve:** the vanilla pickaxe, axe, shovel, hoe and sword of each tier, through item tags `bubbas_reworked_mining:tier/<wood|stone|copper|iron|gold|diamond|netherite>`. Other mods' tools stay vanilla unless a datapack adds them to a tier tag. Swords keep vanilla wear (2 per block). Spears, shears and maces aren't on the curve.
- **(builder) Wear on wood in deepslate** is 3 even though it drops nothing, so trying it is a bad deal.
- **(builder) Unbreaking on armor** gets the same extra roll as tools (the spec says Unbreaking is stronger; armor wasn't singled out).
- **(builder) True Unbreaking chest list and odds**, from the spec's proposal: ancient city, End city treasure, bastion treasure, ominous trial vault (rare pool), and Reworked Mobs' noble crypts at 20 % (owner, 2026-11-22; no hard dependency). Fabricator offices don't have it.
- **(builder) Explosions** can give finds (at no-Fortune odds).

#### How it's built (for the spec, not for players)

- Speed: a mixin on `Player.getDestroySpeed` multiplies the held tool's speed by its tier factor and Efficiency multiplier, and takes vanilla's flat Efficiency modifier (`minecraft:enchantment.efficiency/mainhand` on `mining_efficiency`) back out for tools on the curve. Haste, Mining Fatigue, `block_break_speed` (gems), water and air apply after it, as in vanilla. It runs on both sides, and both sides read only synced data (tags, components).
- Wear: a mixin on `Item.mineBlock` replaces the Tool component's damage-per-block on harsh blocks. It still goes through `hurtAndBreak`, so Unbreaking rolls for every point.
- Wood/gold can't harvest deepslate: data only (`#harsh_deepslate` added to `minecraft:incorrect_for_wooden_tool` and `incorrect_for_gold_tool`).
- Stronger Unbreaking: Fabric `EnchantmentEvents.MODIFY_WITH_LOOKUP` appends a second `remove_binomial` item-damage effect to vanilla Unbreaking. Vanilla's file is untouched.
- True Unbreaking: data, `data/bubbas_reworked_mining/enchantment/true_unbreaking.json` (`minecraft:set` item damage to 0, `exclusive_set` Mending, in no enchantment tag).
- Finds and books: `LootTableEvents.MODIFY` adds one pool per table (skipped when a datapack replaced the table). The find pool's conditions are: not `minecraft:tool/can_silk_touch`, `survives_explosion`, and a `table_bonus` on Fortune.
- No block states are changed (the natural/placed mark was built, then removed at the owner's call), so the mod changes no vanilla block. The quartz items still make Fabric's registry sync require the mod on both sides.
- Known edge: placing and re-mining stone farms finds at the tuned odds (0.5 % bare, 4 % with Fortune III). Accepted by the owner (2026-11-22).

#### Tests

- Core (`BubbasReworkedMiningCoreGameTest`, every build):
  - `curveSlowsDeepslateAndEfficiencyMultiplies`: tier factors and the Efficiency V multiplier reach `Player.getDestroySpeed`.
  - `deepslateWearsToolsAndWoodCantHarvestIt`: iron wear on deepslate and on stone; wood can't harvest deepslate, stone can.
  - `findsFromStoneButNotWithSilkTouch`: 3000 rolls with Fortune III give about the tuned rate; Silk Touch gives zero.
  - `unbreakingStrongerAndTrueUnbreakingIsTotal`: Unbreaking III keeps about 1/7. True Unbreaking takes zero damage on an elytra, is incompatible with Mending, and is in none of the enchanting table, tradeable or random-loot tags.
- Milestone (`MiningMilestoneGameTest`):
  - `trueUnbreakingInChests`: 2000 bastion treasure rolls give about 5 % books; True Unbreaking takes zero damage on armor.
- The rest is in Milestone test objectives below (no test yet).

### Step 2: the quartz set

#### Player-facing behaviour

- **Quartz Sword, Shovel, Pickaxe, Axe, Hoe, Helmet, Chestplate, Leggings and Boots.** Crafted like iron gear, with **blocks of quartz** where the iron ingots go (sticks as usual), and repaired at an anvil with blocks of quartz.
- **Iron level.** They harvest what iron harvests, so they can mine diamonds. They mine deepslate like iron does: slow, with heavy wear. **Ancient debris needs diamond**, so quartz doesn't skip netherite.
- **Faster on the Nether's natural blocks:** netherrack, nether bricks and their slabs, stairs, walls and fences, basalt, blackstone, soul sand and soil, nylium, magma, wart blocks, glowstone, shroomlight, nether quartz and gold ore, crimson and warped stems and hyphae, bone blocks.
- Damage and attack speed are iron's. Armor protects like iron, lasts a little less, and sounds like iron when you put it on.
- Look: bright quartz white with faint rose-grey veins, with a 32x version in the built-in "Bubba's Reworked Mining 32x (Faithful style)" pack.
- In the creative tabs, the tools come right after the iron tools and the armor right after the iron boots.
- No new tooltip lines.

#### Numbers (MiningTuning)

- `QUARTZ` tier: stone 1.0, deepslate 0.40, other 1.0, **Nether 1.6** (builder), wear 1 on stone and 3 on deepslate (iron's).
- Tools **(builder):** durability 220 (copper 190, iron 250), speed 6 and damage bonus +2 (iron's), enchantability 14 (iron's). Per-tool damage and attack speed are iron's.
- Armor **(builder):** durability multiplier 13 (copper 11, iron 15, so helmet 143, chestplate 208, leggings 195, boots 169). Protection 2/6/5/2 (iron's), toughness 0, enchantability 9 (iron's).

#### Builder's choices

- **(builder) Recipe:** blocks of quartz in place of iron ingots, in vanilla's iron shapes (tag `bubbas_reworked_mining:quartz_tool_materials`, which also repairs). That's 4 quartz per block, so a pickaxe costs 12 nether quartz.
- **(builder) Nether speed 1.6x** of the tool's normal speed, and the Nether block list above (tag `bubbas_reworked_mining:nether_blocks`). Ancient debris and obsidian are left out.
- **(builder) Durability between copper and iron.** The owner said "roughly iron/copper level".
- **(builder) No armor trims shown on the item icons** (vanilla needs a per-item trim model for that). Worn trims weren't enabled either: the armor isn't in `#minecraft:trimmable_armor`. Easy to add if the owner wants it.
- **(builder) Art:** vanilla iron's silhouettes and shading bands, recoloured to quartz white with veins (script `mods/bubbas_reworked_mining/tools-local/make_quartz_textures.py`, listed in `docs/art-todo.md`).

#### Tests

- Core: `quartzIsIronLevelAndFasterInTheNether` (quartz on netherrack is 1.6x iron; on deepslate it equals iron; no ancient debris; harvests diamond ore; in `#pickaxes` and `#chest_armor`).
- Client part `quartz` (`-PclientTests=quartz`): two armor stands (quartz next to iron) with pickaxes, the quartz pickaxe and sword held in first person with the whole set in the hotbar, and the player wearing the set in third person, each at 16x and 32x. Checked by eye on 2026-11-22: textures load (no missing-texture checkerboard), the quartz armor is clearly whiter than iron with soft veins, and the 32x pack switches on. Items held by armor stands render dark for iron and quartz alike (lighting, not the texture).
- The rest is in Milestone test objectives below (no test yet).

## Milestone test objectives

- Every tier's speed and wear on stone, deepslate and "other" match `MiningTuning` (the core test covers iron, wood and the wood axe).
- Efficiency and Unbreaking levels give the stronger values, including Unbreaking I and II and the armor odds.
- Finds drop from stone and deepslate (placed or natural) at the tuned odds, with the deepslate diamond weight, never with Silk Touch; explosions give them only at no-Fortune odds.
- Haste and gem `block_break_speed` stack on top.
- True Unbreaking stops all durability loss, isn't offered by the enchanting table or villagers, and can't combine with Mending (core); books in noble crypts at about 20 % with Reworked Mobs.
- Every quartz recipe crafts; repairing with a quartz block works; quartz on each Nether block is 1.6x; quartz armor protection values.

## Open (owner)

- Exact numbers: speeds, wear, Efficiency and Unbreaking curves, find odds, book odds (tune by playing; all in `MiningTuning`).
- How each step shows to the player (tooltips stay minimal, per the owner; today there are no new tooltip lines).
- Builder's calls to review (marked **(builder)** above): Efficiency as a multiplier capped at vanilla for Efficiency V diamond (raise it if "stronger" should beat vanilla at the top); gold can't mine deepslate; which blocks count as stone/deepslate and which give finds; swords, spears, shears and maces off the curve; Unbreaking's extra roll on armor; explosions give finds; the quartz recipe (12 quartz per pickaxe), Nether speed 1.6x and block list, quartz durability 220 / armor 13; no armor trims on quartz armor.

## Quartz tool textures fixed (2026-11-22, agent 3)

The tool heads read mid-grey because vanilla's iron heads are half near-black outline and dark inner edge. `tools-local/make_quartz_textures.py` now maps them, for the tools only, to a soft warm grey outline (132,122,121) and the quartz shadow tone (190,179,174) at 16x and 32x. Checked in client part `quartz`: the heads read white beside iron's.
