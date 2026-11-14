<!-- dte:C3,B15,B16 -->
<!-- covers: mods/bubbas_crafting_tweaks/* -->
# Bubba's Crafting Tweaks companion mod (spec)

Builds the C3 craftingTweaksMod direction. Mod id `bubbas_crafting_tweaks` (same namespace as the datapack). The mod bundles `datapacks/bubbas_crafting_tweaks` as a built-in data pack and `resourcepacks/bubbas_crafting_tweaks` as a built-in resource pack, so mod users install one file; datapack-only players keep using the zip. On Modrinth: one project with both the datapack zip and the mod jar.

## Glider

- Its own registered item, `glider` (not a modified elytra), so it can have its own model, wings and flight rules. Worn in the chest slot with the vanilla `glider` and `equippable` components; its `equippable` asset points at our own wing model.
- **No durability.** The owner dislikes durability; the Glider's nerf is entirely in how it flies. (Easy to add later if wanted.)
- **Handling** (tunable, starting values): turns more slowly toward where the player looks (vanilla blends 10% per tick; the Glider about 5%), and firework rocket boosts are weaker. Done with a small mixin on the elytra flight code that applies only when the worn glider item is ours. Lift and sink rate stay vanilla to start.
- **Recipe:** the owner's original elytra recipe from Crafting Tweaks: diamond, bamboo, diamond / phantom membrane, empty, phantom membrane / feather, empty, feather. Result: one Glider.
- **Look:** its own item model and wing texture at 16x and 32x (B16), made by a person. Placeholder art until then; nothing ships to Modrinth with placeholder art.
- **Tests:** game tests compare a Glider flight against an elytra flight from the same start (it turns slower, rockets add less speed) and check the recipe; client screenshots of the item and the wings on a player, front and back.

## Synthetic Diamond Ore

- The datapack's blasting recipe (block of coal → "Synthetic Diamond Ore") also sets `minecraft:block_state: {synthetic: "true"}` on the result. Without the mod that component is ignored and placed ore behaves like natural ore (today's behavior).
- With the mod: a boolean `synthetic` property is added to vanilla `deepslate_diamond_ore` (default false, so natural and existing ore is unchanged); placing the item sets it. The mod's own data (not the datapack) overrides the ore's loot table to branch on the property with `match_block`: natural keeps vanilla drops exactly; synthetic uses `bubbas_crafting_tweaks:blocks/synthetic_diamond_ore`.
- That table (owner: "may not actually drop anything, so it feels much more rare"): 25 % chance of 1 diamond, Fortune doesn't help, no XP. Silk Touch returns the Synthetic Diamond Ore item unchanged. Starting numbers; tune by playtest.
- Technique, tests and tradeoffs: `docs/knowledge/item-keyed-block-drops.md` (approach C), proven first in refmod (requirement 18). Removing the mod turns synthetic ore into natural ore. The mod is required on client and server (a configuration-phase handshake turns away clients without it).

## As built (2026-11-14)

Code: `mods/bubbas_crafting_tweaks` (package `io.github.bubba8587.bubbas_crafting_tweaks`). Requires Fabric API; `environment: "*"`, required on both sides.

### Bundled packs
- `build.gradle` task `bundlePacks` copies `datapacks/bubbas_crafting_tweaks` to `bundled/datapack/` and `resourcepacks/bubbas_crafting_tweaks` to `bundled/resourcepack/` in the jar at build time (`VERSION`, `DISABLED`, `packtest.commands` and PackTest functions left out). Nothing is duplicated in the mod's source; edit the repo packs.
- `BubbasCraftingTweaks#registerBundledPacks`: the datapack as built-in pack `bubbas_crafting_tweaks:crafting_tweaks_datapack`, `ALWAYS_ENABLED`; the resource pack as `bubbas_crafting_tweaks:crafting_tweaks_resources`, `DEFAULT_ENABLED` (players can switch it off). The 32x texture pack `bubbas_crafting_tweaks:bubbas_crafting_tweaks_32x` is `NORMAL` (opt in), as `specs/textures.md` says.

### Glider
- `registry/ModItems.GLIDER`: plain `Item`, stack size 1, uncommon rarity (yellow name, below the elytra's purple: owner confirmed), components `minecraft:glider` and `minecraft:equippable` (chest, elytra equip sound, asset `bubbas_crafting_tweaks:glider`, no damage on hurt). No `max_damage`. In the Tools & Utilities creative tab after the elytra.
- Handling constants in `glider/GliderHandling` (apply only while the chest slot holds our Glider; everything else, including an elytra worn by the same player, is vanilla):
  - `LivingEntityGliderMixin`: `@ModifyExpressionValue` on the two `0.1` constants in `LivingEntity#updateFallFlyingMovement` (the per-tick blend of horizontal velocity toward the look direction). Glider: `0.05`. Lift, gravity and drag untouched.
  - `FireworkRocketGliderMixin`: `@WrapOperation` on the first `Vec3#add(DDD)` in `FireworkRocketEntity#tick`, the boost to the attached fall-flying entity (`look*0.1 + (look*1.5 - v)*0.5`). Glider: the 0.1 push and the 1.5 target speed are scaled by `GLIDER_ROCKET_POWER = 0.6`, so a rocket both accelerates less and tops out at about 60 % of an elytra's rocket speed (about 1.0 instead of 1.7 blocks/tick). Owner confirmed this starting strength.
  - Movement runs on the client for the local player and on the server for everything else; both mixins are common code, so both sides agree.
- Recipe `data/bubbas_crafting_tweaks/recipe/glider.json` (mod data, not the datapack): the old `elytra2` shape, result `bubbas_crafting_tweaks:glider`.
- Look: item model `items/glider.json` → `item/glider` (generated); wings `equipment/glider.json` (layer `wings`) → `textures/entity/equipment/wings/glider.png` (elytra UV layout, 64x32). **Placeholder art** at 16x and 32x, drawn in code by `tools-local/make_placeholder_textures.py` (flat bamboo-green spar, membrane panels, feather fringe). A person replaces both before any Modrinth release.

### Synthetic Diamond Ore
- Port of refmod's approach C (refmod keeps its copy as the reference): `blockstate/SyntheticOre`, `mixin/BlockExtraPropertyMixin` (adds `synthetic`, default false), `mixin/BlockPropertiesAccessor`, `mixin/DropExperienceBlockMixin` (no XP from synthetic ore), `network/ExtraStatesHandshake` (configuration-phase check; the client registers the receiver in `BubbasCraftingTweaksClient`).
- The datapack's `blast2diamond` result carries `minecraft:block_state {synthetic:"true"}` next to its `item_name` (packcheck's table row checks it; the datapack alone still loads clean and the ore still mines like natural ore without the mod: packcheck `ItemKeyedDropsGameTest.syntheticOreFromBlastingPlacesPlainOre`).
- Mod data: `data/minecraft/loot_table/blocks/deepslate_diamond_ore.json` (a `match_block` `synthetic=true` child sends to the synthetic table; the other two children are vanilla's unchanged; re-check on each Minecraft update) and `data/bubbas_crafting_tweaks/loot_table/blocks/synthetic_diamond_ore.json` (one `alternatives`: silk touch → the ore item with the same `item_name` and `block_state` as the recipe result; otherwise `random_chance 0.25` → one diamond with `explosion_decay` only, so Fortune adds nothing).
- Pick-block on synthetic ore gives plain ore (as refmod; creative only).
- **Reopen if:** Minecraft adds more data-driven blocks, or lets blocks without a block entity carry item data. Then the added `synthetic` property moves to pure data and merges back toward vanilla (see `docs/knowledge/item-keyed-block-drops.md`, Reopen if).

### Tests
Server (`./gradlew :bubbas_crafting_tweaks:build`):
- `BundledPacksGameTest.bundledDatapackIsEnabledAndItsRecipesExist`: the built-in datapack is selected and every recipe file in the bundled copy is loaded, with only this mod installed.
- `GliderGameTest.gliderHasGliderAndEquippableAndNoDurability`, `gliderRecipeCraftsOneGlider`, `gliderTurnsSlowerThanElytra` (same start, east at 1 block/tick looking north, 20 steps of the game's `updateFallFlyingMovement`: the Glider is still well off its heading and keeps ~0.29 of its sideways speed vs ~0.10 for the elytra; vertical speed identical), `rocketAddsLessSpeedToGlider` (same start, 8 ticks of an attached rocket: elytra > 1.4 blocks/tick, Glider < 75 % of that).
- `SyntheticOreGameTest` (ported from refmod): `syntheticPropertyIsAddedAndNaturalIsTheDefault`, `naturalOreKeepsVanillaDropsAndXp` (drops, Fortune III 1..4, silk touch, XP), `syntheticOreUsesItsOwnTableAndGivesNoXp` (survival player places the recipe result and mines it six times with plain and Fortune III picks: at most one diamond, never XP; silk touch gives back an item identical to the recipe result, which places synthetic ore again), `syntheticOreDropsADiamondAboutOneTimeInFour` (2000 rolls of the ore's loot table with a fixed seed, plain and Fortune III: 420..580 diamonds, the rest empty; natural ore always drops one), `pickBlockOnSyntheticGivesPlainOre`, `savesWithAndWithoutThePropertyLoad`.

Client (`runClientGameTest`, screenshots in `build/run/clientGameTest/screenshots/`): `glider-tooltip`, `glider-standing-back`, `glider-standing-front`, `glider-gliding-back`, `glider-gliding-front` (gliding started with the jump key in mid-air, which also proves the client and server agree the Glider can glide), `synthetic-ore-left-natural-right`, and with the 32x pack on `glider-tooltip-32x`, `glider-gliding-back-32x`. The test also checks the bundled resource pack is on by default and that a modded client passes the handshake.
