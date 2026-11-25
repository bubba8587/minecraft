# Infused Gems Commands

*Part of [[Bubba's Infused Gems]].*

> [!infobox] /gems
> | | |
> |---|---|
> | **Permission** | None (any player) |
> | **Operators** | May name another player |

**/gems** lists a player's combined gem effects.

## Syntax

- `/gems`: shows the player's own gem effects.
- `/gems <player>`: shows another player's (operators only).
- `/gems kit <effect> [material] [power]`: for testing (operators, or a world with cheats on). Puts a full armor set on the player, netherite unless a material is named (leather, chainmail, copper, iron, gold, diamond, netherite), with 3 gems of the effect in every piece, and gives a matching sword with 2. The gems are Enchant III at full strength, Base Power VI (perfect) unless a Base Power level from 1 to 6 is given, for example `/gems kit speed iron 4`. Armor already worn goes back to the inventory, or drops if it is full. The effect name completes with Tab, for example `/gems kit invisibility` or `/gems kit turtle_master diamond`.

## Output

Under *Gems on <player>:*, one line for every effect from all gems in the armor being worn, after stacking and the material and matching-set bonuses: what it does in game, and in brackets its strength as a share of the best possible, for example:

> Speed +26.2% (69%)
> Strength +1.97 damage (34%)
> Turtle Master -72.0% damage, -81.0% speed (100%)

Drawbacks are the same kind of line in red, for example *Hunger 1 extra food per 133 s (50%)*. Under *Target, on hit:* the command lists what each hit of the weapon in the main hand applies, for example *Poison 1.15 HP/s for 5.5 s (48%)*.

The output appears even when the `send_command_feedback` game rule is off.

*See also: [[Gem Effects]]*
