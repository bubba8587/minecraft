# Infused Gems Commands

*Part of [[Bubba's Infused Gems]].*

> [!info] /gems
> **Permission:** none (any player)
> **Operators:** may name another player

**/gems** lists a player's combined gem effects.

## Syntax

- `/gems`: shows the player's own gem effects.
- `/gems <player>`: shows another player's (operators only).

## Output

For every effect from all gems in the armor being worn, after stacking and the material and matching-set bonuses, the command shows what it does in game and its share of the best possible total, for example:

> Speed: +21.4% speed (56% of max)

Drawbacks are shown in red and marked, for example *Hunger: extra hunger: 1 food point per 133 s (drawback, 50% of max)*. A second part lists what each hit of the weapon in the main hand applies, for example *Poison: 1.12 HP/s poison for 1.8 s*.

The output appears even when the `send_command_feedback` game rule is off.

*See also: [[Gem Effects]]*
