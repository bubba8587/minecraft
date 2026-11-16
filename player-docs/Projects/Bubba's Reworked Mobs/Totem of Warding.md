# Totem of Warding

*Part of [[Bubba's Reworked Mobs]].*

> [!infobox] Totem of Warding
> | | |
> |---|---|
> | **Type** | Item |
> | **Rarity** | Uncommon |
> | **Stackable** | No |
> | **Renewable** | Yes (evokers) |
> | **Obtained** | Found only: chests, vaults, evokers |
> | **Held** | Monsters within 8 blocks flee every 10 seconds |
> | **In a beacon** | Wards the beacon's area (needs [[Bubba's Reworked Beacons]]) |

The **Totem of Warding** is a found item that keeps monsters away. Held, it pulses and makes nearby monsters run from the holder; placed in a beacon from [[Bubba's Reworked Beacons]], it wards the beacon's whole area against monsters. It is not a gem and cannot be set into armor or weapons. Its texture is a placeholder in this early test version.

## Obtaining

The Totem of Warding cannot be crafted. Each source adds a totem on top of its normal loot:

| Source | Chance |
|---|---|
| Woodland mansion chest | 20% per chest |
| Ancient city chest | 12% per chest |
| Trial chambers vault | 8% per opening |
| Ominous trial chambers vault | 15% per opening |
| Evoker | 5%, a rare bonus drop |

Graveyard and crypt chests are planned to hold them once those structures exist.

## Usage

### Held

In either hand, the totem pulses every 10 seconds. The pulse shows as the normal item cooldown sweep over its hotbar slot (as after throwing an ender pearl), a ring of soul fire spreading out at shin height, rising souls and a low chime. The first pulse comes as soon as the totem is held, unless it is still on cooldown.

At each pulse:

- Every monster within 8 blocks runs away from the holder for about 5 seconds, the way creepers run from cats, then goes back to what it was doing. Running monsters do not attack. Piglins and hoglins flee in their own way.
- [[Sleeping Undead|Sleeping undead]] within 8 blocks settle a little. *See also: [[Noise and Waking]]*

Slimes, ghasts, phantoms, vexes, breezes and the warden are not scared off. A held totem does not stop monsters spawning. Mobs that pick up a Totem of Warding and hold it pulse in the same way.

### In a beacon

*See also: [[Beacon]]*

With [[Bubba's Reworked Beacons]] installed, a Totem of Warding placed in a beacon wards the beacon's area (a sky beacon's column, or the box of a pair of beacons underground). The beacon screen lists what the ward does.

| Pyramid | Effect in the warded area |
|---|---|
| Any (1 to 4 layers) | No monsters spawn naturally, phantoms included. No monster gets in: a monster chasing a player stops at the edge, and monsters already inside leave. |
| 4 layers | Also: sleeping undead inside never wake, whatever the noise. |

The edge of a ward works like an invisible wall for every monster, including those that do not walk in on foot: phantoms, ghasts, vexes, slimes, magma cubes, blazes, endermen teleporting and monsters knocked toward it all stop at the edge and can slide along it. The wall does no damage. Any monster already inside, such as one there when the ward went up or one from a spawner, drifts out through the nearest side.

Happy ghasts are kept out too, whether wild or ridden. A happy ghast that a player steers into the ward is pushed back outside the edge; the rider can get off and walk in. On a slow connection the rider may see the ghast jump back slightly at the edge.

Spawners, trial spawners, raids, bosses and commands still spawn monsters as normal, but those monsters do not stay in the ward either. Monsters outside can still shoot in. The ward ends about 5 seconds after the beacon stops working.

## History

| Version | Changes |
|---|---|
| 0.1.0 | Added. |

## See also

- [[Noise and Waking]]
- [[Monster Spawning]]
- [[Bubba's Reworked Beacons]]
