# Extra Recipes docs report: Glider durability (2026-11-24)

Builder report for the docs merge. Source: BACKLOG "Glider durability" (owner, 2026-11-24). Governing leaf: C3.

## Player-facing change (player-docs)

The Glider now has durability, like the elytra. Pages to update:

- `player-docs/Projects/Bubba's Extra Recipes/The Glider.md`:
  - infobox `Durability`: was "None (never breaks)"; now **324** (the elytra has 432).
  - intro sentence "It never breaks." goes. Replace with something like: "It wears out while gliding, like an elytra, and is repaired with phantom membrane."
  - comparison table row `Durability`: Glider "324; wears out while gliding, repaired with phantom membrane" vs Elytra "432; repaired with phantom membrane".
  - New short section (or bullets) on wear, all of it the elytra's own rules:
    - loses 1 durability per second of gliding (so about 5 minutes 24 seconds of gliding from new; the elytra gets 7 minutes 12 seconds);
    - stops gliding when 1 durability is left and never breaks: the player drops out of the glide and falls, exactly as with a worn-out elytra; repair it to fly again;
    - repaired at an anvil with phantom membrane (each membrane restores a quarter), or by combining two Gliders in a crafting grid or grindstone;
    - takes Mending, Unbreaking, Curse of Binding and Curse of Vanishing from books at an anvil (not at the enchanting table, same as the elytra). Unbreaking works the same as on the elytra (Unbreaking III: about a quarter of the wear);
    - armor hits don't wear it (as before, same as the elytra).
- `player-docs/Projects/Bubba's Extra Recipes/Bubba's Extra Recipes.md` line "a craftable, weaker relative of the elytra that never breaks": drop "that never breaks".
- `player-docs/Projects/Bubba's Extra Recipes/Extra Recipes Companion Mod.md` line "a craftable glider that never breaks but handles worse than an elytra": now "a craftable glider that handles worse than an elytra and wears out a little sooner".
- `True Unbreaking.md` (Reworked Mining) already lists the Glider among things that wear out; now true, no change.
- `specs/copy-strings.md` line 177 ("Never breaks" for the Glider, pending durability) can be resolved: the Glider has durability now.

## Spec facts (`specs/extra-recipes-mod.md`, Glider > The item)

Replace the "No durability" bullet with:

- **Durability 324** (`ModItems.GLIDER_DURABILITY`), via `Item.Properties#durability` (components `max_damage` 324, `damage` 0, `max_stack_size` 1; the old explicit `stacksTo(1)` is gone, `durability` sets it). **Repairable** with phantom membrane (`minecraft:repairable`, `Properties#repairable(Items.PHANTOM_MEMBRANE)`), same as the elytra.
- No wear code of our own: vanilla `LivingEntity#updateFallFlying` damages a random worn `glider` item by 1 every 20 gliding ticks (server side), and `LivingEntity#canGlideUsing` refuses any glider item whose next damage would break it (`ItemStack#nextDamageWillBreak`), so gliding stops at 1 durability and the item never breaks in flight. A creative player (infinite materials) takes no wear.
- Item tags (mod data): `data/minecraft/tags/item/enchantable/durability.json` (Mending, Unbreaking, Curse of Vanishing) and `enchantable/equippable.json` (Curse of Binding) list the Glider, matching the elytra's tag memberships. Not in `enchantable/armor`, so Unbreaking uses the non-armor chance, as on the elytra. No `enchantable` component, so no enchanting table, as on the elytra.
- No broken-look texture: the elytra swaps to a "broken" item model at 1 durability; the Glider keeps its normal icon (the durability bar shows the state). A broken icon is a possible art item for later.

Milestone test list (`GliderGameTest`):

- `gliderHasGliderAndEquippableAndNoDurability` is renamed **`gliderHasGliderEquippableAndDurability`**: max damage 324, damage 0, repaired by phantom membrane (not diamond), in `enchantable/durability` and `enchantable/equippable`, glides from the chest slot, stack size 1.
- New **`gliderWearsWhileGlidingAndStopsAtOneDurability`**: a survival mock player gliding; 200 steps of the game's `updateFallFlying` cost exactly 10 durability; a Glider two points from breaking drops to its last point, then gliding stops, the Glider is still there with 1 durability left and `canGlideUsing` is false.
- Core suite unchanged (3 tests). Full server suite: 14/14 passed with `-PmilestoneTests`.

## Numbers (builder's, for the owner to playtest)

- **Max durability 324** = three quarters of the elytra's 432. Reasons: the Glider is far cheaper (2 diamonds, 2 phantom membranes, 1 bamboo, 2 feathers, crafted any time, against an End City elytra), so it wears out sooner; but it flies slower and turns worse, so trips take longer, and halving it (216, about 3.6 minutes of gliding) felt too punishing. Easy to tune: one constant.
- **Repair: phantom membrane**, the elytra's repair item and already the Glider's sail material in the recipe. One membrane restores 25% (81 points, about 81 seconds of gliding).

## Open / for the owner

- Whether the Glider should get a "broken" icon at 1 durability like the elytra (needs art; would go on `docs/art-todo.md`).
