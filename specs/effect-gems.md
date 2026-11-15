<!-- dte:C4,B9,B15 -->
# Effect gems (spec, early design)

Builds the C4 effectGems direction. Mod name and id not decided. Independent of the other mods.

## Owner's brief (2026-11-14)

Gems are a class of items, each embodying a **permanent** potion effect, added to and removed from **armor**. Very strong, so **hard to get**; they **can't give full potion strength** with infinite duration, and they need **drawbacks**. Undecided: extra GUI slots / a new GUI tab, or data tagged onto the armor.

## Prior art (checked 2026-11-14 with tools/prior-art.py)

- **Effect Gems** (datapack, 26.3, CC-BY-NC-ND: inspiration only): gems held in the off hand or main hand give effects; dropped by rare "Gem Zombies". Same name, different mechanic: our mod needs another name.
- **TrimEffects** (26.3, MIT): each armor trim grants +1 level of an effect per piece, capped at vanilla level II. **Trim Resonance** / **BetterTrims** (26.3): trim material and pattern give stacking abilities. Closest to ours; also shows that vanilla's smithing table and armor trims already "put something into armor" as data on the item, with no new GUI. Compatibility note: these mods also give armor effects, so stacking with them needs a look.
- Apotheosis/Zenith-style socket systems (gems in sockets) exist only on old versions or other loaders.

## Open design questions

Asked one at a time; answers land here.

1. How gems attach: data on the armor via a vanilla station (smithing table to add, grindstone to remove) vs. new slots/GUI.
2. Strength cap and stacking (one gem per piece? same effect on several pieces?).
3. Drawbacks.
4. How gems are obtained (and lost).
