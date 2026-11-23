# Totem of the Beginning follow-up: docs report (2026-11-24)

For specs/endermen.md, specs/totems-artifacts.md and the player vault. Governing leaf: C6 (Totems), B19 noHiddenCooldowns.

## Owner decisions
- The hidden 10-minute "recently reverted" window is gone (hidden cooldown, B19). Anti-ore-farming rests on: the low per-block chance (ore 1/10 of terrain: 12% / 1.2%), ore only ever into air, and the visible 60 s item cooldown on the hand totem.
- A Beginning totem in a beacon or as a statue reverts blocks directly with the same area pulse as the hand version. The caretaker-endermen draw for Beginning beacons is removed (cairns keep their own nexus; Reworked Mobs' `Endermen.drawIn` API is left in place, now unused by Totems).

## Spec facts
- Reworked Mobs `Ripples` / `api/Endermen.pulseAround(level, centre, radius, ticksPerBlock, chance, oreChance, player)`: the `recentTicks` parameter and the RECENT map are deleted (API signature change; Totems' SleeperHook updated; an older Totems against a newer Mobs hits the LinkageError fallback and does nothing).
- Rhythm (new, `beginning/BeginningAreas`): a Beginning beacon or statue pulses once every 15 s on the world clock (`TotemsTuning.BEGINNING_AREA_PULSE_TICKS = 300`; due when `gameTime % 300` falls in the source's call interval, so each source pulses exactly once per 15 s, all in step). Spread 2 ticks per block (`BEGINNING_AREA_TICKS_PER_BLOCK`). No player: no protection check; builds untouched; same puff + throttled sound as the hand pulse; same chances.
- Beacon: the pulse covers the beacon's whole area (radius = half the area's larger horizontal size, a cylinder as far up and down), centred on the beacon. Beacon text: "Every 15 s the ground here slowly remembers how it began".
- Statue: Beginning statues now pulse (StatueEffects.pulses includes BEGINNING), radius `TotemsTuning.STATUE_RADIUS` (picks up the universal statue radius when that lands).
- Removed: `TotemsTuning.BEGINNING_PULSE_RECENT_TICKS`, `SleeperHook.drawEndermen`.

## Player-facing
- Totem of the Beginning in a beacon: no longer calls endermen; every 15 s some of the dug-out ground in the whole beacon area quietly comes back (puffs of particles), ore rarely and only into open air; builds are never touched.
- As a statue: the same in the statue's small radius.
- Hand use: a spot can be reverted again on the next use (no hidden 10-minute lock); still a visible 60 s cooldown.

## Tests
- Reworked Mobs: `pulseRecentWindowAndBuilds` renamed `pulseRepeatsAndBuilds`: re-dug blocks come back on the next pulse; stone in an ore spot stays; a plank keeps its columns.
- Totems: new milestone `BeginningAreaGameTest` (needs -PwithMobs, beacon part -PwithBeacons): a Beginning beacon and a Beginning statue each revert most of a dug hole over one rhythm window; ore spots only get ore.
- Test gotcha for the knowledge base: `GameTestHelper.runAtTickTime/runAfterDelay` keys scheduled runs by the Runnable object, so scheduling the same Runnable at many ticks runs it only once. Wrap it in a fresh lambda per tick.
