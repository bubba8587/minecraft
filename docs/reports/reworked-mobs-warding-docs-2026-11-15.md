<!-- dte:C1 -->
# Reworked Mobs, Warding: docs to merge (2026-11-15)

Written by the Warding builder under the owner's policy (builders don't edit `player-docs/` or spec prose). For the orchestrator or a docs agent to merge into `specs/undead.md` (a new "Warding" section) and the player vault (`player-docs/Projects/Bubba's Reworked Mobs/`: a new **Totem of Warding** page, updates to **Noise and Waking**, **Monster Spawning**, the overview page's links). Already in the spec: the milestone test objectives. The owner's design is in `docs/research/area-gems-and-warding.md` ("Owner decisions"); everything below marked *(builder's choice)* is not from the owner and is open to change.

## Player-facing

### Totem of Warding (new page)

- An uncommon item (yellow name), stacks to 1, like the Totem of Undying. Not craftable: found only. Not a gem, never goes on armor or weapons. Placeholder art (a small blue-grey stone idol with a soul flame, soul-fire eyes and a diamond rune), 16x and 32x.
- **Obtaining** (each source adds a totem on top of the normal loot):

| Source | Chance |
|---|---|
| Evoker (killed by anything) | 100 %, alongside its Totem of Undying |
| Woodland mansion chest | 20 % per chest |
| Ancient city chest | 12 % per chest |
| Trial chambers vault | 8 % per opening |
| Ominous trial chambers vault | 15 % per opening |
| Graveyard and crypt chests | planned, once those structures exist |

- **Held** in either hand, it pulses every 10 seconds. The pulse shows as the normal item cooldown sweep on its hotbar slot (as after an ender pearl), a ring of soul fire spreading out at shin height, rising souls and a low amethyst chime. The first pulse comes the moment it's held (if it isn't on cooldown).
- Each pulse: every monster within 8 blocks runs from the holder for about 5 seconds, the way creepers run from cats (it keeps running while within 12 blocks of the holder, faster within 7), then comes back to what it was doing. Running monsters don't attack. Piglins and hoglins run through their own vanilla fleeing. Monsters without legs to run on (slimes, ghasts, phantoms, vexes) and the warden and breezes are not affected *(limitation)*.
- Each pulse also settles sleeping undead within 8 blocks a little (see Noise and Waking).
- Held, it does **not** stop monsters spawning.
- Mobs holding a totem pulse too, on the same 10 s rhythm (equality). *(builder's choice; a woken undead can pick one up off the ground.)*
- **In a beacon** (needs Bubba's Reworked Beacons): see "Wards" below. The beacon screen shows "No monsters spawn or enter" and either "Sleeping undead stay asleep" (4 layers) or "4 layers: sleeping undead stay asleep" (greyed, fewer layers).

### Wards (Totem of Warding in a Reworked Beacons beacon)

- The ward covers the beacon's own area (sky beacon: vanilla's column, 20/30/40/50 blocks out; paired beacons: the paired box).
- Any pyramid: **no monsters spawn naturally** inside it (phantoms included). Spawners, trial spawners, raids, bosses and commands still spawn as normal.
- Any pyramid: **monsters don't walk in**. Every monster treats the warded area like a wall when finding a path, so a monster chasing a player inside stops at the edge. Monsters already inside (spawned by a spawner, knocked in, there when the ward went up) walk out. Ranged monsters outside can still shoot in. Monsters that don't walk by paths (slimes, ghasts, phantoms, vexes, flying/teleporting cases) aren't kept out *(limitation)*; brain-driven ones (piglins, hoglins) are kept from pathing in but aren't pushed out.
- 4-layer pyramid: **sleeping undead inside stay asleep**: no noise, bell or robbing wakes them.
- The ward ends about 5 seconds after the beacon stops (taken out, pyramid broken, unloaded).

### Noise and Waking (updates)

- **Light:** a sleeper whose block has a block light level of **12 or more** (a torch next to it, a lantern, glowstone) hears every noise at **70 %** and is that much less likely to wake when robbed. Daylight doesn't count. Placing the torch is itself a noise (block placement, like any block). This follows the "torches near a spawner" idea: monster spawners stop at block light above 11.
- **Bells:** sleepers hear a bell as far as players do, **32 blocks**. A ring counts as **250 points at the bell**, falling off evenly to nothing at 32 blocks (the wake threshold is 100, stirring 50): it wakes sleepers within about **19 blocks** and stirs those out to about **25 blocks**. Walls and wool muffle it like any other noise. Any ring counts: hitting it, a projectile, redstone.
- **Totem of Warding pulse:** each pulse takes **40 points** off the meter of every sleeper within 8 blocks (never below zero).
- **Full-pyramid ward:** sleepers inside never wake.

### Monster Spawning (update)

- Natural monster spawns never happen inside a Totem of Warding beacon's area (with Reworked Beacons installed).

### Compatibility (overview page)

- Optional: Bubba's Reworked Beacons. With it, the Totem of Warding goes in a beacon. Without it everything else works the same.

## Spec facts (for `specs/undead.md`, new "Warding" section)

- Code: package `warding/` (`TotemOfWardingItem`, `WardFleeGoal`, `Wards`, `WardingLoot`, `WardingTuning` (every number, loot rates included)); `undead/sleeper/SleeperBells`, `SleeperFinder`; `compat/WardingBeaconPayload` (the only class touching the beacons API); mixins `BellBlockMixin` (RETURN of `attemptToRing(Entity, Level, BlockPos, Direction)`), `WalkNodeEvaluatorMixin` and `SwimNodeEvaluatorMixin` (HEAD of `getPathTypeOfMob`: BLOCKED for a monster's node inside a ward it isn't in; walk covers fly, amphibious and climb evaluators), `PhantomSpawnerMixin` (WrapOperation on `isValidEmptySpawnBlock`), `MobAccessor` (goal selector), `SpawnStateMixin` (now also refuses monsters inside a ward).
- **Held pulse:** `Item.inventoryTick` with slot MAINHAND or OFFHAND; players use `ItemCooldowns.addCooldown(stack, 200)` (vanilla sweep, synced by vanilla); other holders use a weak map of next pulse times. Monsters = `instanceof Enemy`. Goal-driven `PathfinderMob`s get `WardFleeGoal` (priority 0, MOVE flag) added lazily the first time they're scared (no goal added to every mob); its picks copy `AvoidEntityGoal` (DefaultRandomPos.getPosAway 16/7, walk 1.0, sprint 1.2 within 7 blocks). AvoidEntityGoal itself isn't reused because its targeting skips creative players and non-player holders. Brain mobs with the `AVOID_TARGET` memory get it set for 100 ticks. Sleepers: `SleeperBlockEntity.calm`.
- **Sensitivity:** `SleeperBlockEntity.effectiveSensitivity(level)` = stored sensitivity x 0.7 at block light >= 12, x 0 in a full-pyramid ward. Used by hearing, bells and robbing. The light lookup only happens for noises that are in earshot.
- **Bells:** `SleeperBells.rang` walks the loaded chunks' sleepers within 32 (no chunk loads) and feeds each through the listener's shared `hear` path (wool, walls, stir, wake, travel delay). The listener ignores the bell's own BLOCK_CHANGE vibration so it isn't counted twice.
- **Wards:** `Wards.put(level, key, AABB, keepsSleepersAsleep, lifetimeTicks)`; lapses unless refreshed (beacon payload refreshes on every 80-tick pulse for 100 ticks). Queries are one map lookup when no ward exists. A sweep every 20 ticks finds goal-driven monsters inside each ward and sets them walking out through the nearest side (`DefaultRandomPos.getPosTowards` to a point 3 blocks outside).
- **Beacon wiring:** `compileOnly`/`gametestCompileOnly project(':bubbas_reworked_beacons')`, entrypoint `bubbas_reworked_beacons:payload`, `"suggests"`; `-PwithBeacons` puts the beacons jar on the game test classpath (same pattern as Infused Gems). Payload: accepts the totem, power 1, required layers 1, `apply` = `Wards.put(level, context.pos(), context.area(), context.levels() >= 4, 100)`.
- **Loot:** Fabric `LootTableEvents.MODIFY`, one extra pool per listed table, `random_chance` below 1; tables replaced through Fabric are skipped. TODO: crypt/graveyard chest tables join `WardingTuning.LOOT` once worldgen builds them.
- **Art:** `tools/warding-textures.py` (16x + 32x, placeholder).
- **Test note:** the game test server's light engine lags (block light still 0 forty ticks after placing glowstone), so the light test waits up to 200 ticks.

## Decisions I made (open to the owner)

1. Evokers drop the Totem of Warding **every time** (100 %), like their Totem of Undying; chest rates as in the table. Together it's clearly more common than the Totem of Undying, which only evokers drop.
2. Light calm is a step: block light 12+ (above what spawners allow) = x0.7. Sky light doesn't count.
3. Bell: 250 points at the bell, linear falloff to 32 blocks (sound in Minecraft fades linearly too). Walls still muffle it.
4. Pulse calm: 40 points per pulse.
5. Flee: from the holder, for 100 ticks, while within 12 blocks.
6. Monsters held by a ward leave through the nearest side; they don't despawn or take damage.
7. Phantoms count as natural spawns for wards.
8. The pulse chime is the amethyst chime at low pitch.

## Questions for the owner

1. Is 100 % from evokers too generous (raid farms give unlimited totems)? 50 % would still beat the Totem of Undying once chests are counted.
2. Should the bell ignore walls (players hear bells through walls), or keep the wall muffling so a bell on the surface doesn't wake a crypt below?
3. Should slimes, ghasts, phantoms and vexes also be kept out of wards (they don't path, so it would need a push at the edge)?
4. Should a mob holding a Totem of Warding pulse (it does now), and should woken undead be allowed to pick one up?
