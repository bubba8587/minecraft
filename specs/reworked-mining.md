<!-- dte:C7 -->
# Bubba's Reworked Mining (spec, design 2026-11-22, not built)

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
- Only from natural blocks: blocks placed by players never drop finds (or the rate would be farmable by place-and-mine). How to tell natural from placed is a technical choice (see `docs/knowledge/item-keyed-block-drops.md` and Reworked Mobs' `TerrainOracle`).
- Silk Touch gives the block, no find. **Fortune raises the odds gently** (owner, 2026-11-22): each level roughly doubles the chance (vanilla's `table_bonus` loot condition, as gravel's flint uses), so Fortune III makes finds noticeable but still rare.

## True Unbreaking (owner, 2026-11-22)

- A new enchantment, **True Unbreaking**: the item never loses durability. Data-driven (an `item_damage` effect set to zero).
- **Found only, rare:** in loot (proposed: ancient city, End city, bastion and trial vault chests; later Reworked Mobs' noble crypts and Fabricator offices). Never from the enchanting table or villagers.
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

## Open

- Exact numbers (above are targets; the owner tunes after playing).
- How each step shows to the player (tooltips stay minimal, per the owner).

## Milestone test objectives

- Mining time and wear per tool tier on stone and deepslate match the tuning table.
- Efficiency and Unbreaking levels give the stronger values.
- Finds drop only from natural stone and deepslate, at the tuned odds, never from placed blocks or with Silk Touch.
- True Unbreaking stops all durability loss, isn't offered by the enchanting table or villagers, and can't combine with Mending.
