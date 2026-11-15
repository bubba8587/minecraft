# Gem Effects

*Part of [[Bubba's Infused Gems]].*

**Gem effects** are the permanent, partial potion effects that socketed Gems give: to the wearer from armor, and to the target from weapons. Every gem effect works in both.

## Behavior

Any Gem fits armor or a weapon; **where it is set decides who gets its effects**. In armor, the wearer gets them all the time while wearing it. In a weapon, whatever the weapon hits gets them. Bonus effects and drawbacks follow the same rule, so a Poison gem in armor poisons its wearer, and a Regeneration gem on a sword heals the target. Mobs wearing gemmed armor or holding gemmed weapons are affected the same way.

### Strength

Gem effects are always weaker than a real potion. The best possible build reaches 90% of the strongest potion level, and only one build does: twelve perfect gems of one effect in a full set of netherite armor (or two in a netherite weapon).

- **Stacking:** gems of the same effect add up, each extra one counting a little less than the one before.
- **Material:** the same gem is stronger in better armor or weapons, from leather and wood up to netherite. Gold counts like diamond, for its strong enchanting. Bows, crossbows, tridents and maces count like diamond.
- **Matching set:** armor pieces of the same material next to each other (helmet, chestplate, leggings, boots) add strength. A matching helmet and chestplate beat a matching helmet and boots; a full matching set is best.
- **Drawbacks** are small and fixed, about a tenth of the effect's strongest potion level. Several of the same drawback add up a little, but stay weaker than the weakest real potion.

### Gems and potions together

A gem and a potion (or beacon, or other source) of the same effect do not add up: the stronger one applies. A Speed gem set worn while a Speed II potion is active does nothing extra; under a Speed I potion it only adds the part above Speed I. The same goes for an effect a weapon gem puts on a target that already has the real effect.

### Undead

As with potions, Healing and Harming are reversed on undead, in armor and on weapons: a Healing gem worn by a zombie makes hits on it hurt more, and a Harming weapon heals undead targets. Undead ignore Poison and Regeneration gems, spiders ignore Poison, and wither skeletons ignore Wither.

The `/gems` command shows the player's combined totals. *Main article: [[Infused Gems Commands]]*

## In armor

Values are for the best build (90%); weaker builds give less.

| Gem | Effect on the wearer |
|---|---|
| Speed | Faster movement (up to +36%) |
| Strength | More attack damage (up to +5.4) |
| Jump Boost | Higher jumps and a longer safe fall |
| Haste | Faster mining and attacks (found gems only) |
| Regeneration | Slow healing when hurt |
| Resistance | Less damage taken (up to 36% less; found gems only) |
| Turtle Master | Much less damage taken (up to 72% less) and much slower movement (up to 81% slower), together |
| Fire Resistance | Less fire and lava damage, and fire goes out sooner |
| Water Breathing | Air lasts longer underwater |
| Night Vision | The world is partly lit in the dark |
| Invisibility | Mobs notice the wearer from closer the stronger the gems (at the best, 16% of the usual range); from about half strength the wearer is hidden on the locator bar. Only the perfect complete set makes the wearer truly invisible; armor still shows, as with the potion. |
| Slow Falling | Slower falling and less fall damage |
| Healing | Each survived hit or fall returns part of the lost health (up to 25%) |
| Absorption | A shield of extra health (up to 14.4) that fills 30 seconds after the last hit; a newly worn shield starts empty |
| Health Boost | More maximum health (up to +7.2; found gems only) |
| Dolphin's Grace | Faster swimming |
| Saturation | Hunger drains more slowly (up to 90% slower) |
| Conduit Power | In water or rain: air lasts longer, faster mining underwater, clearer sight underwater |
| Hero of the Village | Cheaper villager trades (up to 20% cheaper; found gems only) |
| Luck | A little more luck (mostly found; a Luck potion also brews one) |

Harmful gems in armor affect the wearer:

| Gem | Effect on the wearer |
|---|---|
| Poison | Steady damage, never below 1 health |
| Wither | Steady damage that can kill |
| Slowness, Weakness, Mining Fatigue, Hunger, Bad Luck | The harmful effect, partial |
| Harming | More damage from each hit taken (up to 25% more) |
| Glowing | Glowing, on all the time |
| Darkness | The wearer's screen dims steadily, more the stronger the gems (a single weak gem only slightly; the best build almost as dark as the potion), with no pulsing |
| Blindness | Dark fog closes in on the wearer's screen, more the stronger the gems |
| Nausea | The wearer's screen wobbles, more the stronger the gems |
| Levitation | A slow drift upward (at most about a third of a block per second) |
| Wind Charged, Weaving, Oozing | A chance of the effect's burst, cobwebs or slimes when the wearer dies |
| Infested | A chance of silverfish each time the wearer is hurt |

## In weapons

Each hit gives the target a partial effect for a few seconds, refreshed (never added up) by every hit. Its strength is measured against the strongest potion of the effect: the best weapon comes close to a Poison II or Slowness IV potion, but only for 8 seconds; weaker weapons give a weaker effect for a shorter time. Every hit lasts long enough to be noticed: Poison and Wither at least 2.5 seconds (enough for several damage ticks), Darkness 3 seconds, Hunger 4 seconds, Nausea 5 seconds, Regeneration long enough to heal a heart, everything else 2 seconds. The effect appears in the target's effects as a percentage, for example *Poison 46%*.

| Gem | Effect on the target |
|---|---|
| Poison, Wither, Slowness, Weakness, Mining Fatigue | The harmful effect, partial |
| Hunger | Drains food twice as fast as Hunger III; every hit costs a player at least half a drumstick |
| Glowing, Levitation, Blindness, Nausea, Darkness, Wind Charged, Weaving, Oozing, Infested | The effect itself, for longer the stronger the gem |
| Harming | A little extra magic damage on every hit (up to 2.25) |
| Healing | Heals the target a little on every hit (up to 1.8) |
| Turtle Master | Its Resistance and Slowness together |
| Any other helpful gem | The helpful effect on the target (allowed, but pointless) |

| Weapon | Poison | Slowness |
|---|---|---|
| Wooden sword, one weak gem | 0.57 health per second for 2.9 s | 20% slower for 2.9 s |
| Diamond sword, two good splash gems | 1.24 health per second for 6.3 s | 45% slower for 6.3 s |
| Netherite sword, two perfect gems (the best) | 1.58 health per second for 8 s | 57% slower for 8 s |

Swords, axes, spears, maces and tridents apply their gems on every hit, including sweeps and thrown tridents.

### Bows and crossbows

Bows and crossbows with gems turn every arrow they fire into a tipped arrow with the gems' effects. The arrow flies and trails like a tipped arrow, but picked up again it is a plain arrow; an arrow in flight keeps its gems when the world is saved. When a real tipped arrow of the same effect is fired, the stronger effect applies; they do not add up.

## Found gems

Found-only gems (Haste, Resistance, Health Boost, Hero of the Village, Mining Fatigue) and most Luck gems come from loot. Mobs that spawn in gemmed armor get its effects. *Main article: [[Finding Gems]]*
