---
type: mod
minecraft: "26.3"
status: active
modrinth:
requires: [Fabric API]
side: client and server
---
# Bubba's Infused Gems

> [!infobox] Bubba's Infused Gems
> | | |
> |---|---|
> | **Type** | Fabric mod |
> | **Minecraft** | 26.3 |
> | **Version** | 0.1.0 (early test) |
> | **Requires** | Fabric API |
> | **Side** | Client and server |
> | **Status** | Active |

**Bubba's Infused Gems** is a Fabric mod that lets the player brew a potion into a gem, enchant the gem, and set it into armor or a weapon, giving a permanent version of the potion's effect.

- Armor gems affect the wearer all the time; weapon gems affect whatever the weapon hits.
- Every gem effect works in armor and in weapons.
- Gems below the very best are weaker than a real potion; only a perfect set matches the strongest potion (and goes past it for movement effects).
- Gems of the same effect stack, and some gems carry a drawback.
- The gem textures are simple placeholders.

## Obtaining

*Main article: [[Finding Gems]]*

- Gems are brewed, enchanted and socketed by the player (below).
- Finished Gems and gemmed armor are also found rarely in chests, vaults, fishing treasure and hero gifts, and on mobs that spawn in armor.
- Six effects come only from loot: Haste, Luck, Health Boost, Hero of the Village, Mining Fatigue and Resistance.

## Items

- **Infused Gem**: a brewed gem that carries a potion's effect. *Main article: [[Making Infused Gems]]*
- **Gem**: an enchanted Infused Gem, ready to be socketed. *Main article: [[Enchanting Gems]]*

## Process

| Step | Station | Input | Result |
|---|---|---|---|
| 1 | Brewing stand | Potion (or an item that carries an effect, such as a golden apple) + base gem | Infused Gem |
| 2 | Enchanting table | Infused Gem + lapis lazuli + experience | Gem |
| 3 | Smithing table | Armor or weapon + Gem (no template) | Gear with the gem socketed |

*Main articles: [[Making Infused Gems]], [[Enchanting Gems]], [[Socketing Gems]], [[Gem Effects]], [[Finding Gems]]*

## Commands

*Main article: [[Infused Gems Commands]]*

- `/gems` lists the player's combined gem effects.

## Installation

- Installed like any Fabric mod (see [[Installing Fabric Mods]]).
- Needed on both the server and every player's game.
- Ready-made Infused Gems and Gems are in the creative inventory under **Bubba's Infused Gems**.
- The [[Bubba Test World]] has a Gems station.

## Textures

- The mod comes with vanilla-style 16x textures.
- Its 32x textures, in the style of Classic Faithful 32x Jappa, come in a separate resource pack, **Bubba's Mods 32x**, which covers all of Bubba's mods (see [[Installing Resource Packs]]).
- **Bubba's Mods 32x** goes above Classic Faithful 32x Jappa when both are used.

## Compatibility

- Requires Fabric API.
- Mods that also give armor effects (such as trim-effect mods) can be installed alongside it; how their effects combine with gem effects has not been checked.
- [[Bubba's Reworked Beacons]]: a Gem can be placed in a beacon, which gives the gem's effects to every living thing in its area; the beacon then does not take the plain base-gem payments. *Main article: [[Gem Effects#In beacons]]*
- [[Bubba's Reworked Mobs]]: its Potion of [[Soulguard]] brews a Soulguard gem, which protects its wearer fully from [[Soulfire]]. *Main article: [[Gem Effects#Gems beyond potions]]*
- Without either mod, Infused Gems works the same.

## See also

- [[Finding Gems]]
- [[Installing Fabric Mods]]
- [[Bubba Test World]]
