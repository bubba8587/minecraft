# Reworked Mobs Commands

*Part of [[Bubba's Reworked Mobs]].*

**Reworked Mobs commands** are the game rules and commands added by [[Bubba's Reworked Mobs]].

## Game rules

| Game rule | Values | Default | Effect |
|---|---|---|---|
| `bubbas_reworked_mobs:monster_cap_percent` | 0 to 100 | 15 | How many monsters spawn naturally, as a percentage of normal. See [[Monster Spawning]]. |
| `bubbas_reworked_mobs:cut_monster_spawns` | `true` or `false` | `true` | Set to `false` to turn the spawn cut off completely, for example when another mod controls spawning. |
| `bubbas_reworked_mobs:watcher_creepers` | `true` or `false` | `true` | Each player has one watching creeper at night and random creeper spawns are off. Set to `false` for normal creeper spawning. See [[Creepers]]. |
| `bubbas_reworked_mobs:show_meters` | `true` or `false` | `false` | Shows mob meters: a blue bar for a drowned's grip (above it, and under the crosshair while one holds you) and a red bar for how startled a creeper is. |
| `bubbas_reworked_mobs:cairn_endermen` | `true` or `false` | `true` | Endermen spawn near enderman cairns. Set to `false` to stop that (the few ordinary endermen that spawn anywhere are unaffected). See [[Endermen]]. |

Game rules are set with `/gamerule`, for example `/gamerule bubbas_reworked_mobs:monster_cap_percent 30`.

## Commands

### display

*Main article: [[Loot on Display]]*

Puts an item on display. Requires operator permission; intended for building and testing.

**Syntax:** `/bubbas_reworked_mobs display <ground|buried|wall|stuck> <item> [<position> [north|south|east|west]]`

- Without a position, the item goes where the player is looking (within 20 blocks), turned toward the player. `wall` hangs it on the side of the block the player is looking at.
- With a position, and optionally a direction, it is placed exactly there.
- The command fails if the spot is taken or has nothing to hold the item up.

### cairn

*Main article: [[Endermen]]*

Finds and builds enderman cairns. Requires operator permission; intended for building and testing.

**Syntax:** `/bubbas_reworked_mobs cairn <locate|build|builders|here> [<seed>]`

- `locate` names the nearest cairn site within 1024 blocks, how complete it is and which way it points.
- `build` finishes the nearest cairn within 128 blocks.
- `builders` summons one enderman for each missing block of the nearest cairn within 64 blocks (at most 12), each carrying its block, and they build it.
- `here` adds a cairn site at the player's feet and builds it; a seed picks its design. The site is saved with the world.
