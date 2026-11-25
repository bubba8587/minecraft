# Socketing Gems

*Part of [[Bubba's Infused Gems]].*

**Socketing** sets a finished Gem into armor or a weapon at a smithing table, where it gives its effects permanently.

## Usage

### Smithing table

| Template slot | Base slot | Addition slot | Result |
|---|---|---|---|
| *(empty)* | Armor or weapon | Gem | The same gear with the gem socketed |

The template slot is left empty. The gear's tooltip lists its gems under *Gems (n):*, one row per gem with its main effect, and its bonus effect and drawback on indented rows under it:

> Gems (2):
>  ◆ Speed
>      + Minor Haste
>      - Minor Poison
>  ◆ Strength

Levels are not shown on gear. The combined effect of all worn gems, with real values, is shown by the `/gems` command. *See also: [[Infused Gems Commands]]*

| Gear | Gem slots |
|---|---|
| Helmets, chestplates, leggings, boots (any material) | 3 |
| Swords, axes, spears, maces, tridents, bows, crossbows | 2 |

Tools, elytras, shields and the turtle shell cannot take gems.

### Grindstone

Placing socketed gear in a grindstone gives the same item without its gems (and, as always with a grindstone, without its enchantments, if it had any). Taking the result returns every gem to the player's inventory.

## Behavior

When socketed armor or a weapon breaks, its gems drop on the ground where it broke. Gems are never lost.

*Main article: [[Gem Effects]]*
