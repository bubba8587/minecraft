# Beacon

*Part of [[Bubba's Reworked Beacons]].*

> [!infobox] Beacon (reworked)
> | | |
> |---|---|
> | **Slot** | One item, kept in the beacon |
> | **Affects** | Every living thing in range: players and mobs |
> | **Pulse** | Every 4 seconds |
> | **Effect time** | 9 seconds plus 2 per pyramid layer |
> | **Sky range** | 20 / 30 / 40 / 50 blocks (1 to 4 layers) |
> | **Paired range** | 60 / 90 / 120 / 150 blocks (1 to 4 layers) |

A **beacon** in [[Bubba's Reworked Beacons]] holds one item that decides what it does, instead of vanilla's power buttons and payment. Its effect reaches every living thing in its area, players and mobs alike, including the player who built it. Beacons can also hang upside down, and a pair of beacons works underground.

## Usage

### The screen

The beacon screen has one item slot, with no power buttons and no payment row. The item **stays in the beacon** and can be taken out again; breaking the beacon drops it. Hoppers cannot put items into beacons.

The screen shows the beacon's state, then the item's name and what it does, in the item's own tooltip words:

| Line | Meaning |
|---|---|
| *Sky beacon: N-layer pyramid, R blocks out* | Working as a normal beacon with a clear sky |
| *Paired beacons: N-layer pyramids, R blocks out* | Working as a pair (below) |
| *Idle: needs a pyramid below* | No pyramid |
| *Idle: needs a clear sky above, or an upside-down beacon to pair with* | The beam is blocked |
| *Needs a N-layer pyramid* (red) | The item needs a bigger pyramid to work at all |
| *Only outputting Gem Power N* (yellow) | The item is stronger than the pyramid allows; it works at the limit |

### Effects

Every 4 seconds the beacon gives its effect to every living thing in its area, players and mobs (not spectators). The effect lasts 9 seconds plus 2 seconds per pyramid layer, as in vanilla, so it fades soon after leaving the area.

The pyramid limits the item's Gem Power (for items from other mods that have one):

| Pyramid layers | Highest Gem Power |
|---|---|
| 1 | I |
| 2 | III |
| 3 | V |
| 4 | VI |

### Payments

Without other mods, the beacon takes six base-gem items, and the item chosen is the power. A 4-layer pyramid raises the power to level II.

| Item | Power | Pyramid needed | With a 4-layer pyramid |
|---|---|---|---|
| Amethyst Shard | Speed | 1 | Speed II |
| Nether Quartz | Haste | 1 | Haste II |
| Ender Pearl | Resistance | 2 | Resistance II |
| Diamond | Jump Boost | 2 | Jump Boost II |
| Prismarine Shard | Strength | 3 | Strength II |
| Nether Star | Regeneration | 3 | Regeneration II |

Emeralds, iron, gold and netherite ingots are no longer accepted. With [[Bubba's Infused Gems]] installed these payments are switched off, and finished Gems go in the slot instead (*see [[Gem Effects#In beacons]]*). With [[Bubba's Totems & Artifacts]], the [[Totem of Warding]] also fits.

## Range

### Sky beacon

A beacon with a pyramid below and the usual clear beam to the sky works over vanilla's area: 20, 30, 40 or 50 blocks out to each side for 1 to 4 layers, from that far below the beacon up to the top of the world.

### Upside-down beacon

Placing a beacon against the underside of a block hangs it upside down, like a hanging lantern; it stays when that block is removed. On its own an upside-down beacon does nothing: no beam, no effect and no item. Its pyramid is built upside down above it.

### Paired beacons

A normal beacon with its pyramid below and an upside-down beacon straight above it, with its pyramid above, form a pair when only clear blocks (air, glass, anything a beacon beam passes through) are between them. There is no limit on the gap. The item goes in the bottom beacon.

- The smaller of the two pyramids sets the range: **60, 90, 120 or 150 blocks** out to each side (three times a sky beacon's), from 10 blocks below the floor pyramid to 10 blocks above the ceiling pyramid.
- A white beam joins the two beacons while the pair works. A block in the beam turns the pair off: no beam and no effect.
- The usual beacon sounds play for pairs.
- A beacon that could pair and also sees the sky works as a pair.

Paired beacons trade the sky beacon's full-height column for a wide, low box that fits mines, bases and crypts.

## History

| Version | Changes |
|---|---|
| 0.1.0 | Added. |

## See also

- [[Totem of Warding]]
- [[Gem Effects]]
