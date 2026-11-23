<!-- dte:C7 -->
# Reworked Mining: docs report (2026-11-22, builder: agent 3)

For the orchestrator or docs agent to merge into `specs/reworked-mining.md` and `player-docs/`. Every number lives in `MiningTuning.java` (`mods/bubbas_reworked_mining/src/main/java/io/github/bubba8587/bubbas_reworked_mining/MiningTuning.java`). **(builder)** marks the builder's own choices. Everything else is the owner's design.

## Step 1: the curve, rare finds, True Unbreaking (built, 8/8 server tests pass)

### Player-facing behaviour

- **Pickaxes on stone and deepslate:**
  - Wood is very slow on stone and wears out fast. It can't mine deepslate at all: you can still break it, slowly, but it drops nothing.
  - Stone is a bit slower on stone and incredibly slow on deepslate, with heavy wear there.
  - Copper and iron are about vanilla on stone, and slow on deepslate with heavy wear.
  - Diamond is vanilla. Netherite is faster than vanilla.
- **Other tools** (axes, shovels, hoes, and pickaxes on non-stone blocks): wood and stone are a little slower, iron and up are vanilla, and netherite is a little faster.
- **Efficiency is stronger relative to each tool:** it now multiplies the tool's speed, so it matters on every tier. An Efficiency V iron pickaxe mines deepslate a little faster than a bare diamond one, and Efficiency V diamond is as fast as in vanilla.
- **Unbreaking is stronger:** at level III a tool loses about 1 durability in 7 (vanilla is 1 in 4). Armor gets the same extra protection on top of its vanilla odds.
- **Haste (potion, beacon) and Infused Gems' Haste gems** still multiply on top of all of this.
- **Rare finds:** mining natural stone, granite, diorite, andesite or tuff sometimes drops iron or gold nuggets (1-3), rarely lapis, very rarely an emerald. Natural deepslate has slightly better odds and a tiny diamond chance. Each Fortune level doubles the odds. Silk Touch, blocks a player placed, and stone made by lava flowing onto water never give finds. Explosions can give finds (at the no-Fortune odds).
- **True Unbreaking** (new enchantment, max level I): the item never loses durability. It works on anything with durability (tools, weapons, armor, elytra, shields, ...) and is applied at an anvil. It can't combine with Mending. It's never offered by the enchanting table or villagers and never appears as a random enchantment on loot: it's found only as an enchanted book in ancient city, End city, bastion treasure and ominous trial vault (rare) chests.
- Tooltips: no new tooltip lines (minimal-tooltips preference). True Unbreaking shows as a normal enchantment name.

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

What that means in seconds (on the ground, no Haste): iron on deepslate is about 1.9 s per block (vanilla 0.75 s), stone pickaxe on deepslate about 9 s, wood on stone about 3.4 s (vanilla 1.15 s). An iron pickaxe lasts about 83 deepslate blocks bare, about 580 with Unbreaking III.

- Efficiency multiplier by level 0-V: 1.0, 1.5, 2.1, 2.8, 3.5, 4.25. Higher levels (commands, other mods) keep adding 0.75 per level.
- Extra Unbreaking cancel chance at I/II/III: 0.333, 0.40, 0.43. Tools then keep a point with odds 1/3, 1/5, 1/7.
- Find chance per natural block, at Fortune 0/I/II/III: stone-type 0.5 %, 1 %, 2 %, 4 %; deepslate 0.8 %, 1.6 %, 3.2 %, 6.4 %.
- Find weights: stone-type: iron nugget 60, gold nugget 30, lapis 7, emerald 3. Deepslate: iron nugget 50, gold nugget 35, lapis 10, emerald 4, diamond 1. So a bare pickaxe finds a diamond about once per 12,500 natural deepslate blocks.
- True Unbreaking book chance per chest: ancient city 4 %, End city treasure 3 %, bastion treasure 5 %, trial chambers `reward_ominous_rare` 5 %. Anvil cost 8, weight 1.

### Builder's choices (owner may want to look)

- **(builder) Why Efficiency changed shape.** Vanilla Efficiency adds a flat +26 speed at level V. On a slowed curve that would erase the curve: an Efficiency V stone pickaxe would out-mine a bare diamond one on deepslate. So Efficiency became a multiplier on the tool's own speed, tuned to the spec's target ("Efficiency V iron about like bare diamond on deepslate") and capped so Efficiency V diamond matches vanilla. As a result, low-tier tools gain less from Efficiency than in vanilla in absolute terms, and the curve keeps its shape. If the owner wants "stronger" to mean faster than vanilla at the top, raise the last entries of `EFFICIENCY_MULTIPLIER`.
- **(builder) Gold tools** count as wood for harvesting (vanilla), so they can't mine deepslate either. They keep some speed on stone (0.67 of vanilla gold's 12).
- **(builder) Which blocks are "stone" and "deepslate"** (block tags `bubbas_reworked_mining:harsh_stone` / `harsh_deepslate`): stone, cobblestone, mossy cobblestone, granite, diorite, andesite, tuff, infested stone/cobblestone and the 8 stone ores; deepslate, cobbled deepslate, infested deepslate and the 8 deepslate ores. Crafted variants (bricks, polished, tiles, slabs) use the gentle "other" factor.
- **(builder) Which blocks give finds:** natural stone, granite, diorite, andesite, tuff (stone-type) and deepslate. Not cobblestone, calcite or the ores.
- **(builder) Which tools are on the curve:** the vanilla pickaxe, axe, shovel, hoe and sword of each tier, through item tags `bubbas_reworked_mining:tier/<wood|stone|copper|iron|gold|diamond|netherite>`. Other mods' tools stay vanilla unless a datapack adds them to a tier tag. Swords keep vanilla wear (2 per block). Spears, shears and maces aren't on the curve.
- **(builder) Wear on wood in deepslate** is 3 even though it drops nothing, so trying it is a bad deal.
- **(builder) Unbreaking on armor** gets the same extra roll as tools (the spec says Unbreaking is stronger; armor wasn't singled out).
- **(builder) True Unbreaking chest list and odds**, from the spec's proposal: ancient city, End city treasure, bastion treasure, ominous trial vault (rare pool). Noble crypts and Fabricator offices are for later (Reworked Mobs, no hard dependency).
- **(builder) Explosions** can give finds (at no-Fortune odds), since an exploded natural block is still natural.

### How it's built (for the spec, not for players)

- Speed: a mixin on `Player.getDestroySpeed` multiplies the held tool's speed by its tier factor and Efficiency multiplier, and takes vanilla's flat Efficiency modifier (`minecraft:enchantment.efficiency/mainhand` on `mining_efficiency`) back out for tools on the curve. Haste, Mining Fatigue, `block_break_speed` (gems), water and air apply after it, as in vanilla. It runs on both sides, and both sides read only synced data (tags, components).
- Wear: a mixin on `Item.mineBlock` replaces the Tool component's damage-per-block on harsh blocks. It still goes through `hurtAndBreak`, so Unbreaking rolls for every point.
- Wood/gold can't harvest deepslate: data only (`#harsh_deepslate` added to `minecraft:incorrect_for_wooden_tool` and `incorrect_for_gold_tool`).
- Stronger Unbreaking: Fabric `EnchantmentEvents.MODIFY_WITH_LOOKUP` appends a second `remove_binomial` item-damage effect to vanilla Unbreaking. Vanilla's file is untouched.
- True Unbreaking: data, `data/bubbas_reworked_mining/enchantment/true_unbreaking.json` (`minecraft:set` item damage to 0, `exclusive_set` Mending, in no enchantment tag).
- Natural vs placed: approach C from `docs/knowledge/item-keyed-block-drops.md`. Stone, granite, diorite, andesite, tuff and deepslate get a `bubba_placed` boolean (default false). Block items mark what they place, and the lava-onto-water stone is marked too. The mark survives pistons, `/clone`, structures and saves; old worlds and generated terrain count as natural. Removing the mod drops the mark (everything reads as natural again).
- Finds and books: `LootTableEvents.MODIFY` adds one pool per table (skipped when a datapack replaced the table). The find pool's conditions are `match_block` with `bubba_placed=false`, not `minecraft:tool/can_silk_touch`, `survives_explosion`, and a `table_bonus` on Fortune.
- **The mod must be on both sides.** Adding the property shifts block-state ids. In step 2 the quartz items make Fabric's registry sync turn away clients without the mod, so no extra handshake is needed. Step 1 alone should not go on a server without step 2.
- Known edge: Reworked Mobs' endermen restore natural terrain with `setBlock`, so stone they put back counts as natural again. That's slow and not a practical farm, but it's worth knowing.

### Tests

- Core (`BubbasReworkedMiningCoreGameTest`, every build):
  - `curveSlowsDeepslateAndEfficiencyMultiplies`: tier factors and the Efficiency V multiplier reach `Player.getDestroySpeed`.
  - `deepslateWearsToolsAndWoodCantHarvestIt`: iron wear on deepslate and on stone; wood can't harvest deepslate, stone can.
  - `findsOnlyFromNaturalStone`: 3000 rolls with Fortune III. Natural stone gives about the tuned rate; placed stone and Silk Touch give zero.
  - `unbreakingStrongerAndTrueUnbreakingIsTotal`: Unbreaking III keeps about 1/7. True Unbreaking takes zero damage on an elytra, is incompatible with Mending, and is in none of the enchanting table, tradeable or random-loot tags.
- Milestone (`MiningMilestoneGameTest`):
  - `playerPlacedStoneIsMarked`: a survival player placing stone or deepslate marks it.
  - `lavaMadeStoneIsMarked`: lava onto water makes marked stone.
  - `trueUnbreakingInChests`: 2000 bastion treasure rolls give about 5 % books; True Unbreaking takes zero damage on armor.
- Milestone objectives to add to the spec (no test yet):
  - Every tier's speed and wear on stone, deepslate and "other" match `MiningTuning` (the core test covers iron, wood and the wood axe).
  - Unbreaking I and II and armor odds.
  - Deepslate find odds and the diamond weight.
  - Explosions give finds only at no-Fortune odds.
  - Haste and gem `block_break_speed` stack on top.
