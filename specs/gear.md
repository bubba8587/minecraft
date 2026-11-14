<!-- dte:C3,B15,B16 -->
# Gear mod (spec)

Builds the C3 gearMod direction. Name not decided yet; mod id chosen with it.

## Glider

- Its own registered item, `glider` (not a modified elytra), so it can have its own model, wings and flight rules. Worn in the chest slot with the vanilla `glider` and `equippable` components; its `equippable` asset points at our own wing model.
- **No durability.** The owner dislikes durability; the Glider's nerf is entirely in how it flies. (Easy to add later if wanted.)
- **Handling** (tunable, starting values): turns more slowly toward where the player looks (vanilla blends 10% per tick; the Glider about 5%), and firework rocket boosts are weaker. Done with a small mixin on the elytra flight code that applies only when the worn glider item is ours. Lift and sink rate stay vanilla to start.
- **Recipe:** the owner's original elytra recipe from Crafting Tweaks: diamond, bamboo, diamond / phantom membrane, empty, phantom membrane / feather, empty, feather. Result: one Glider.
- **Look:** its own item model and wing texture at 16x and 32x (B16), made by a person. Placeholder art until then; nothing ships to Modrinth with placeholder art.
- **Tests:** game tests compare a Glider flight against an elytra flight from the same start (it turns slower, rockets add less speed) and check the recipe; client screenshots of the item and the wings on a player, front and back.
