# Enchanting Gems

*Part of [[Bubba's Infused Gems]].*

> [!infobox] Gem
> | | |
> |---|---|
> | **Obtained by** | Enchanting an Infused Gem |
> | **Stackable** | Yes (16) |
> | **Used for** | [[Socketing Gems\|socketing]] into armor and weapons |

**Enchanting** an Infused Gem at an enchanting table turns it into a finished **Gem**, rolling its final effects by enchant level.


Finished gems shimmer with the enchantment glint, so they stand out from infused gems.

## Usage

The player places an Infused Gem and lapis lazuli in an enchanting table. The three rows are three enchant levels, **Enchant I, II and III**.

- **Cost:** as with gear, bookshelves around the table set each row's level cost, and the full cost shown is paid (about 10, 20 and 30 levels with 15 bookshelves), plus 1, 2 or 3 lapis lazuli.
- **Bookshelves:** Enchant II needs at least 8 bookshelves and Enchant III all 15; without them the row is unavailable.

Rows above the gem's form are disabled:

| Infused Gem form | Enchant levels available |
|---|---|
| Normal | I |
| Splash | I, II |
| Lingering | I, II, III |
| From an item (golden apple, wither rose, stew...) | I, II, III |

The Infused Gem's tooltip names its potion and its highest level, for example *Strong potion, up to Enchant III* (or *Up to Enchant III* for a gem brewed from an item).

Hovering over a row shows a hint of the result, like the enchantment clue for gear: the main effect and its strength, and if there is an extra roll, only its kind and size, not which effect. For example *Speed 100%*, *Speed 100% + minor bonus...?*, *Speed 100% + drawback...?* or *minor drawback only...?* ("minor" is a weak roll; a medium roll has no size word). The hint is exactly what the row will give. The offered results stay the same until the player enchants something, so enchanting a cheap item first gives new rolls, as in vanilla.

## Results

| Enchant | Result | Chance |
|---|---|---|
| I | Weak version of the effect | 75% |
| I | Weak, plus a weak drawback | 20% |
| I | Weak drawback only | 5% |
| II | Medium version of the effect | 55% |
| II | Medium, plus a weak bonus effect | 30% |
| II | Medium, plus a weak drawback | 15% |
| III | Strongest version | 50% |
| III | Strongest, plus a weak bonus effect | 25% |
| III | Strongest, plus a medium bonus effect | 10% |
| III | Strongest, plus a weak drawback | 10% |
| III | Strongest, plus a medium drawback | 5% |

The plain result is the most common at every enchant level.

- A **bonus effect** is a different effect of the same kind: a helpful gem gets another helpful effect, a harmful gem another harmful one. Any effect of that kind can come up, apart from Healing, Harming and Hero of the Village, which are only ever a gem's main effect.
- A **drawback** is a mild effect of the opposite kind, never the gem's direct opposite: a Speed gem can get Hunger, Weakness, Mining Fatigue, Bad Luck, or more rarely Poison or Wither, but never Slowness. A harmful gem's drawback can be almost any helpful effect (it lands on whatever the weapon hits, or on the wearer in armor).
- A drawback-only gem is named like any other Gem; its tooltip shows its levels and only the drawback.

### Tooltip

A finished Gem is named just *Gem*. Its tooltip lists three levels in grey, then its effects:

| Line | Meaning |
|---|---|
| *Base Power V* | The base gem it was brewed with, I (amethyst) to VI (nether star). *See also: [[Making Infused Gems]]* |
| *Infusion III* | The potion it was brewed with: normal I, long II, strong III. Gems brewed from an item count as III. |
| *Enchant III* | The enchanting row it came from: I weak, II medium, III strongest. An inverted Gem's Enchant is one lower. |
| *+ Speed* | The main effect |
| *+ Minor Haste* | A bonus effect. *Minor* means a weak roll; a medium roll has no word. |
| *- Minor Poison* (red) | A drawback. *Minor* means a weak one. |

The exact strength of each effect, as a percentage of a perfect gem, shows only with advanced tooltips on (F3+H). A perfect gem (100%) requires the strongest potion in lingering form (or the strongest source item), a nether star and Enchant III.

## Inverting gems

A finished Gem with a drawback can be **inverted** at a brewing stand: the Gem goes in a bottle slot and a fermented spider eye in the ingredient slot. The drawback becomes the Gem's main effect, and the old main effect becomes its drawback.

- **Cost:** the new main effect is one step weaker than the old one (strongest to medium, medium to weak), and the Gem drops one Base Power level.
- **Bonus effect:** it stays as it is, so after inverting it works against the new main effect. It is a bonus again if the Gem is inverted back.
- **Inverting back** is allowed, with the same cost again.

A Gem with only a drawback inverts too: the drawback becomes a weak main effect and the Gem drops a Base Power level, with no drawback afterwards.

The brewing stand does not take a Gem that has no drawback, a Gem whose main effect is already weak, a Base Power I Gem, or a Gem whose drawback is Bad Luck. Gems cannot be combined or merged in any way.

| Before | After inverting |
|---|---|
| Base Power VI, Enchant III, + Speed, + Strength, - Hunger | Base Power V, Enchant II, + Hunger, + Strength, - Speed |

## Which effects come up

Bonus effects and drawbacks are not all equally likely:

- Effects that are easy to brew come up most often.
- Harder ones (from rare ingredients, the End, the deep ocean or trial chambers, and Poison and Wither as drawbacks) come up about half as often.
- Effects that are only found in the world come up a quarter as often.
