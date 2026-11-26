<!-- dte:C3,B15,B16 -->
<!-- covers: mods/bubbas_extra_recipes/* -->
# Bubba's Extra Recipes companion mod (spec)

**Status: built** (mod 0.1.0). Builds C3 (craftingTweaksMod). Mod id `bubbas_extra_recipes`, the same namespace as the datapack.

- Code: `mods/bubbas_extra_recipes` (package `io.github.bubba8587.bubbas_extra_recipes`).
- Requires Fabric API; `environment: "*"`, required on both sides.
- Mod users install one file; datapack-only players keep using the zip. On Modrinth: one project with both the datapack zip and the mod jar.

## Bundled packs

- The jar carries `datapacks/bubbas_extra_recipes` as a built-in data pack and `resourcepacks/bubbas_extra_recipes` as a built-in resource pack.
- `build.gradle` task `bundlePacks` copies them to `bundled/datapack/` and `bundled/resourcepack/` at build time, leaving out `VERSION`, `DISABLED`, `packtest.commands` and the PackTest functions. Nothing is duplicated in the mod's source: edit the repo packs.
- `BubbasExtraRecipes#registerBundledPacks`:
  - the datapack as `bubbas_extra_recipes:extra_recipes_datapack`, `ALWAYS_ENABLED`;
  - the resource pack as `bubbas_extra_recipes:extra_recipes_resources`, `DEFAULT_ENABLED` (players can switch it off);
  - the 32x texture pack `bubbas_extra_recipes:bubbas_extra_recipes_32x`, `NORMAL` (opt in), as `specs/textures.md` says.

## Glider

A craftable, weaker cousin of the elytra.

### The item

- Its own registered item, `registry/ModItems.GLIDER` (not a modified elytra), so it can have its own model, wings and flight rules.
- Plain `Item`, stack size 1 (set by `durability`), uncommon rarity (yellow name, below the elytra's purple: owner confirmed).
- Components `minecraft:glider` and `minecraft:equippable`: chest slot, elytra equip sound, asset `bubbas_extra_recipes:glider` (our own wing model), no damage on hurt.
- **Durability 324** (`ModItems.GLIDER_DURABILITY`), via `Item.Properties#durability` (components `max_damage` 324, `damage` 0, `max_stack_size` 1; no explicit `stacksTo(1)`, `durability` sets it). **Repairable** with phantom membrane (`minecraft:repairable`, `Properties#repairable(Items.PHANTOM_MEMBRANE)`), same as the elytra.
  - 324 is three quarters of the elytra's 432 (builder's number, owner to playtest; one constant): the Glider is far cheaper and craftable any time, so it wears out sooner, but it flies slower and turns worse, so trips take longer; half (216, about 3.6 minutes of gliding) felt too punishing.
  - One phantom membrane restores 25 % (81 points, about 81 seconds of gliding).
- **No wear code of our own:** vanilla `LivingEntity#updateFallFlying` damages a random worn `glider` item by 1 every 20 gliding ticks (server side), and `LivingEntity#canGlideUsing` refuses any glider item whose next damage would break it (`ItemStack#nextDamageWillBreak`), so gliding stops at 1 durability and the item never breaks in flight. A creative player (infinite materials) takes no wear. Armor hits don't wear it (no damage on hurt).
- **Enchanting:** item tags (mod data) `data/minecraft/tags/item/enchantable/durability.json` (Mending, Unbreaking, Curse of Vanishing) and `enchantable/equippable.json` (Curse of Binding) list the Glider, matching the elytra's tag memberships. Not in `enchantable/armor`, so Unbreaking uses the non-armor chance, as on the elytra. No `enchantable` component, so no enchanting table, as on the elytra (books at an anvil only).
- **No broken-look icon:** the elytra swaps to a "broken" item model at 1 durability; the Glider keeps its normal icon (the durability bar shows the state). A broken icon is open for the owner (`BACKLOG.md`).
- In the Tools & Utilities creative tab after the elytra (owner confirmed).
- **Recipe** `data/bubbas_extra_recipes/recipe/glider.json` (mod data, not the datapack): the owner's original elytra recipe. Diamond, bamboo, diamond / phantom membrane, empty, phantom membrane / feather, empty, feather. Result: one Glider.

### Handling

Constants in `glider/GliderHandling`. They apply only while the chest slot holds our Glider; everything else, including an elytra worn by the same player, is vanilla. Lift, gravity, drag and sink rate stay vanilla.

- **Turning:** vanilla blends horizontal velocity 10 % per tick toward where the player looks; the Glider blends 3 % (`GLIDER_TURN_BLEND` 0.03). Owner, after a playtest: "nerf the glider's handling even more. speed is fine."
  - `LivingEntityGliderMixin`: `@ModifyExpressionValue` on the two `0.1` constants (used once each for x and z) in `LivingEntity#updateFallFlyingMovement`.
- **Rockets:** weaker. `GLIDER_ROCKET_POWER` = 0.6 (owner confirmed this starting strength).
  - `FireworkRocketGliderMixin`: `@WrapOperation` on the first `Vec3#add(DDD)` in `FireworkRocketEntity#tick`, the boost to the attached fall-flying entity (`look*0.1 + (look*1.5 - v)*0.5`).
  - The 0.1 push and the 1.5 target speed are both scaled by 0.6, so a rocket accelerates less and tops out at about 60 % of an elytra's rocket speed (about 1.0 instead of 1.7 blocks a tick).
  - **Never a brake (owner playtest, 2026-11-25):** vanilla's boost pulls velocity toward the target speed from either side, so a Glider diving faster than its ~1.0 rocket speed was slowed by a rocket (and by the Totem of the North Wind, which fires one). `GliderHandling.rocketBoost` splits the velocity along the look from the sideways part: along the look it only adds (the push, plus half the shortfall when below the target); sideways it is vanilla's (halved each tick), so rockets still straighten the Glider out. Milestone `GliderGameTest.rocketNeverSlowsAFastGlider`.
- Movement runs on the client for the local player and on the server for everything else; both mixins are common code, so both sides agree.

### Look

- Item model `items/glider.json` → `item/glider` (generated).
- Wings `equipment/glider.json` (layer `wings`) → `textures/entity/equipment/wings/glider.png` (elytra UV layout, 64x32).
- Agent-made art at 16x and 32x (B16), owner 2026-11-25: "switch to using a recolored Elytra using our bamboo/membrane/diamond/feather elements". `tools-local/make_glider_textures.py` recolours vanilla's elytra (16x) and Classic Faithful 32x Jappa's (32x, from the CF32J zip in the Modrinth profile `porp`; without it the committed 32x is kept) region by region: each pixel's OKLab lightness, normalised within its region, is placed on that material's vanilla ramp.
  - Wings (64x32 elytra sheet): the leading side strip and the outer face's leading column are bamboo (`item/bamboo`, `block/bamboo_stalk`); the spar's last rows and the tip-end face are diamond; the shoulder-end face is bamboo; the last two rows are feathers; the rest is phantom membrane (the item's cream-to-mauve, upper 70 % of its ramp so the shoulders are grey-mauve).
  - Icon: the outline and yoke are bamboo, the yoke's darkest centre a diamond clasp, the bottom tips feathers, the inside membrane.
  - Kept to a recolour (no added nodes or battens). The membrane is the phantom membrane item's colours, not the phantom mob's grey-blue (one ramp, `MEMBRANE`, to swap).
  - 32x credited to CF32J in `mods/bubbas_extra_recipes/CREDITS.md` and `resourcepacks/bubbas_32x/CREDITS.md`.
- Listed in `docs/art-todo.md`. A person replaces both before any Modrinth release; nothing ships to Modrinth with the agent-made art.

## Quartz Shard

Owner, 2026-11-24: quartz from andesite, diorite and granite cost a quarter as much as before (there is far too much of that stone), replacing the datapack's direct blasting recipe.

- Item `registry/ModItems.QUARTZ_SHARD` (plain item, stacks to 64), in the Ingredients tab after nether quartz. Lang "Quartz Shard".
- Recipes (mod data): `blast_quartz_shard` (blasting; andesite, diorite or granite → 1 shard, 200 ticks, 0.05 XP, builder's number) and `quartz_from_shards` (shaped 2x2 of shards → 1 nether quartz, like amethyst shards into a block).
- Art: agent-made 16x and 32x (`tools-local/make_quartz_shard.py`, one broken-off crystal in vanilla quartz's palette); in `docs/art-todo.md`.
- Test: `QuartzShardGameTest` (milestone): each stone blasts into one shard, four craft one quartz, the old `blast_quartz` recipe is gone. Client part `glider` shows shards in the inventory shot.

## Synthetic Diamond Ore

The datapack's blasting recipe (coal block → "Synthetic Diamond Ore") sets `minecraft:block_state {synthetic:"true"}` on the result, next to its `item_name`. Without the mod that component is ignored and placed ore behaves like natural ore.

### Behaviour with the mod

- A boolean `synthetic` property is added to vanilla `deepslate_diamond_ore` (default false, so natural and existing ore is unchanged). Placing the item sets it.
- The mod's own data (not the datapack) overrides the ore's loot table to branch on the property with `match_block`: natural ore keeps vanilla drops exactly; synthetic ore uses `bubbas_extra_recipes:blocks/synthetic_diamond_ore`.
- **That table** (owner: "may not actually drop anything, so it feels much more rare"): 25 % chance of 1 diamond, Fortune doesn't help, no XP. Silk Touch returns the Synthetic Diamond Ore item unchanged. Starting numbers; tune by playtest.
- Pick-block on synthetic ore gives plain ore (as in refmod; creative only).
- Removing the mod turns synthetic ore into natural ore.
- The mod is required on client and server: a configuration-phase handshake turns away clients without it.

### How it's built

Technique, tests and tradeoffs: `docs/knowledge/item-keyed-block-drops.md` (approach C), proven first in refmod (requirement 18). This is a port of refmod's approach C; refmod keeps its copy as the reference.

- **Classes:**
  - `blockstate/SyntheticOre`;
  - `mixin/BlockExtraPropertyMixin` (adds `synthetic`, default false);
  - `mixin/BlockPropertiesAccessor`;
  - `mixin/DropExperienceBlockMixin` (no XP from synthetic ore; XP is code, not loot data);
  - `network/ExtraStatesHandshake` (configuration-phase check; the client registers the receiver in `BubbasExtraRecipesClient`).
- **Mod data:**
  - `data/minecraft/loot_table/blocks/deepslate_diamond_ore.json`: a `match_block` `synthetic=true` child sends to the synthetic table; the other two children are vanilla's, unchanged. Re-check on each Minecraft update.
  - `data/bubbas_extra_recipes/loot_table/blocks/synthetic_diamond_ore.json`: one `alternatives`. Silk Touch gives the ore item with the same `item_name` and `block_state` as the recipe result; otherwise `random_chance 0.25` gives one diamond, with `explosion_decay` only, so Fortune adds nothing.
- **The datapack side:** packcheck's recipe table row checks the `block_state` on the `blast2diamond` result. The datapack alone still loads clean and the ore still mines like natural ore without the mod (packcheck `ItemKeyedDropsGameTest.syntheticOreFromBlastingPlacesPlainOre`).
- **Technique notes:**
  - `BlockExtraPropertyMixin` wraps the `createBlockStateDefinition` call in the `Block` constructor rather than injecting into the method, so it also works for blocks whose override doesn't call super. It recognises the block by the id in its `Properties`, which `Blocks.register` sets before construction (`BlockPropertiesAccessor`).
  - `StateDefinition#any()` is the first state and `BooleanProperty` lists `true` first, so the mixin sets `synthetic=false` as the default explicitly. Otherwise worldgen, `/setblock` and old saves would give synthetic ore.
  - `SyntheticOre` is loaded while `Blocks` is initialising, so it must not touch any registry.
  - Why the handshake: an added property shifts every block state id after that block. Fabric's registry sync rebuilds state ids from each side's own blocks and sends nothing when no registry has modded entries, so a client without the property would join and decode chunks with shifted ids.
- **Reopen if:** Minecraft adds more data-driven blocks, or lets blocks without a block entity carry item data. Then the added `synthetic` property moves to pure data and merges back toward vanilla (see `docs/knowledge/item-keyed-block-drops.md`, Reopen if).

## Milestone test objectives

All objectives have tests. Test-lite (`specs/quality.md`, "Test suites"): `ExtraRecipesCoreGameTest` runs on every build (`./gradlew :bubbas_extra_recipes:build`); everything else is milestone (`-PmilestoneTests`).

- **Core** (`ExtraRecipesCoreGameTest`, 3 tests): the bundled datapack is on and its recipes load; the Glider turns slower than an elytra; synthetic ore uses its own loot table and gives no XP.
- **`BundledPacksGameTest.bundledDatapackIsEnabledAndItsRecipesExist`:** the built-in datapack is selected and every recipe file in the bundled copy is loaded, with only this mod installed.
- **`GliderGameTest`:**
  - `gliderHasGliderEquippableAndDurability`: max damage 324, damage 0, repaired by phantom membrane (not diamond), in `enchantable/durability` and `enchantable/equippable`, glides from the chest slot, stack size 1.
  - `gliderWearsWhileGlidingAndStopsAtOneDurability`: a survival mock player gliding; 200 steps of the game's `updateFallFlying` cost exactly 10 durability; a Glider two points from breaking drops to its last point, then gliding stops, the Glider is still there with 1 durability left and `canGlideUsing` is false.
  - `gliderRecipeCraftsOneGlider`
  - `gliderTurnsSlowerThanElytra`: same start, east at 1 block a tick looking north, 20 steps of the game's `updateFallFlyingMovement`. The Glider is still well off its heading and keeps ~0.29 of its sideways speed, against ~0.10 for the elytra; vertical speed identical.
  - `rocketAddsLessSpeedToGlider`: same start, 8 ticks of an attached rocket. Elytra above 1.4 blocks a tick, Glider below 75 % of that.
- **`SyntheticOreGameTest`** (ported from refmod):
  - `syntheticPropertyIsAddedAndNaturalIsTheDefault`
  - `naturalOreKeepsVanillaDropsAndXp`: drops, Fortune III 1..4, Silk Touch, XP.
  - `syntheticOreUsesItsOwnTableAndGivesNoXp`: a survival player places the recipe result and mines it six times with plain and Fortune III picks: at most one diamond, never XP. Silk Touch gives back an item identical to the recipe result, which places synthetic ore again.
  - `syntheticOreDropsADiamondAboutOneTimeInFour`: 2000 rolls of the ore's loot table with a fixed seed, plain and Fortune III: 420..580 diamonds, the rest empty; natural ore always drops one.
  - `pickBlockOnSyntheticGivesPlainOre`
  - `savesWithAndWithoutThePropertyLoad`
- **Client** (`BubbasExtraRecipesClientGameTest`, milestone, `runClientGameTest -PmilestoneTests`; screenshots in `build/run/clientGameTest/screenshots/`):
  - `glider-tooltip`, `glider-standing-back`, `glider-standing-front`, `glider-gliding-back`, `glider-gliding-front`. Gliding starts with the jump key in mid-air, which also proves the client and server agree the Glider can glide.
  - `synthetic-ore-left-natural-right`.
  - With the 32x pack on: `glider-tooltip-32x`, `glider-gliding-back-32x`.
  - The test also checks the bundled resource pack is on by default and that a modded client passes the handshake.

## Open

- Real Glider art (item and wings, 16x and 32x) and a mod icon, both made by a person (`BACKLOG.md`).
- Whether the Glider gets a "broken" icon at 1 durability like the elytra (needs art; would go on `docs/art-todo.md`).
- Glider numbers (turn blend 0.03, rocket power 0.6, durability 324) and Synthetic Diamond Ore's 25 % are starting values to tune by playtest.
