# Use vanilla subsystems (spec, owner 2026-11-22)

**The rule (owner): when vanilla already has a system for a job, our mods use it rather than hand-rolling their own.** This covers finding entities, players, blocks and places; mob movement; and cooldowns, sounds, loot and tags. Vanilla's systems are tuned, well tested and handle edge cases we'd miss (creative and spectator players, unloaded chunks, throttling). They also behave the way players already expect.

Builders: check this table before writing any search, scan or timer. A reviewer rejects a hand-rolled version of anything listed here unless the code comment says why vanilla's doesn't fit (B9 libsOverHandroll applies to vanilla too). Names are checked against the 26.3 source (`build/mcsrc/26.3/common`). Re-check them on a new release.

## Finding entities

| Need | Use | Not |
|---|---|---|
| Entities of a kind in an area | `level.getEntitiesOfClass(Cls.class, box, predicate)` | looping over all entities with distance checks |
| Common filters | `EntitySelector` constants: `NO_CREATIVE_OR_SPECTATOR`, `NO_SPECTATORS`, `ENTITY_STILL_ALIVE`, `LIVING_ENTITY_STILL_ALIVE`, `ENTITY_NOT_BEING_RIDDEN`, `CAN_BE_PICKED`; plus `withinDistance(x, y, z, d)` and `pushableBy(e)` | inline `e.isAlive() && !e.isSpectator()` |
| Nearest player for **hostile or ambush** behavior | `ServerLevel.getNearestPlayer(TargetingConditions.forCombat().range(r), mob)`, or `level.getNearestPlayer(x, y, z, r, true)` | `getNearestPlayer(entity, r)`, which **includes creative players** |
| Nearest player for **non-hostile** checks (is anyone near, did someone see it) | `TargetingConditions.forNonCombat()`, `hasNearbyAlivePlayer(x, y, z, r)` | `for (ServerPlayer p : level.players())` plus a distance check |
| Players in a box | `ServerLevel.getNearbyPlayers(conditions, source, box)` or `getEntitiesOfClass(ServerPlayer.class, box, EntitySelector.NO_SPECTATORS)` | a `level.players()` loop |
| What a mob notices over time | brain sensors and memories (`NearestLivingEntitySensor`, `PlayerSensor`, `NearestItemSensor`), or goal-based `TargetGoal`s | per-tick scans in a mixin |

**Creative and spectator:** monsters ignore creative and spectator players, as vanilla mobs do. Anything that ambushes, wakes, startles or targets uses the combat conditions above.

## Finding blocks and places

| Need | Use | Not |
|---|---|---|
| Nearest block of a type or tag | `level.findBlocksInBoxByManhattanDistance(pos, h, v).filterState(state -> state.is(TAG))` (how `HoglinSpecificSensor` finds repellents), or `BlockPos.findClosestMatch(pos, h, v, pred)` | nested dx/dy/dz loops |
| Visiting positions nearest-first | `BlockPos.withinManhattan(...)` or `BlockPos.spiralAround(...)` | building a list of positions and sorting it |
| Points of interest (bells, beds, workstations, lodestones, nether portals) | `level.getPoiManager().findClosest(...)`, `findAll(...)`, `getInRange(...)` | block scans |
| "Repels mob X" blocks | a **block tag** (like `BlockTags.PIGLIN_REPELLENTS` and `HOGLIN_REPELLENTS`) plus the search above | hard-coded block lists |
| A mob walking to a kind of block | a `MoveToBlockGoal` subclass with `isValidTarget` (nearest-first search within its home, throttled, with arrival handling) | hand searches plus `createPath` loops |
| A mob seeking shade or shelter | `FleeSunGoal` / `RestrictSunGoal`, or the same idea (`canSeeSky` plus a pathfinding check) | custom sun logic |
| A random spot to wander, flee or approach | `LandRandomPos` / `DefaultRandomPos` / `AirRandomPos` (`getPos`, `getPosTowards`, `getPosAway`) | random offsets plus ground checks |
| Surface height | `level.getHeight(Heightmap.Types..., x, z)` | scanning down from the build limit |
| Structures | `level.findNearestMapStructure(tag, pos, r, false)` and structure tags (as dolphins and explorer maps do) | our own position lists, unless the structure is ours and registered (camps and cairns keep site registries) |

## Other systems

| Need | Use |
|---|---|
| A visible cooldown | `player.getCooldowns()` (`ItemCooldowns`) and cooldown groups. B19: no hidden cooldowns. |
| Running something every N ticks | `level.getGameTime() % N`, spread across sites so they don't all fire on the same tick. Use scheduled block ticks (`scheduleTick`) for blocks. |
| Sounds and game events | vanilla `SoundEvents` where one fits; `GameEvent` and vibrations for "this is loud" (sculk already listens). |
| Data that should be tweakable | data-driven loot tables, tags, recipes, advancements and structures before Java code. Loot changes use `LootTableEvents.MODIFY` keyed on table ids. |
| Item behavior | data components (`DataComponents`) before NBT-style custom storage. |

## When vanilla doesn't fit

Write the reason in a comment where the hand-rolled code is, e.g. "vanilla has no beacon POI, so beacons are tracked in `BeaconRegistry` when they load". Mention it in the builder's report so the orchestrator can check it. The rule's reason, and examples found in our code, are in the 2026-11-22 code audit.
