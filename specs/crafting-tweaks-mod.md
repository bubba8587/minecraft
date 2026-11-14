<!-- dte:C3,B15,B16 -->
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
