# Totem of the Beginning rework: docs report (2026-11-24)

Owner decisions 2026-11-24 (C6 detail, spec not leaf): the totem is never used up; right-click is an area pulse with a
cooldown and per-block odds; hardened against ore farming; beacon/statue reverts get the same puff and sound.

## Player-facing (player-docs Totems & Artifacts page)

- Totem of the Beginning is no longer single-use. Right-click: the ground within 8 blocks ripples back toward how it
  generated over about 3 seconds, then a 60-second cooldown (shown by the item's cooldown sweep).
- Each pulse only brings back some of the ground (about 1 block in 8); standing in one spot and pulsing once a minute
  slowly heals it. Mined-out ore comes back far more rarely (about 1 in 80 per pulse), only into empty space, and a
  block that just came back can't come back again for 10 minutes.
- Never touches builds (as before), unbreakable blocks, or spawn-protected ground.
- Each block that changes gives a dark soul puff with purple portal motes and a low, soft sculk sound (at most about
  3 sounds a second however many blocks change). Endermen tending a beacon's or statue's Beginning area give the same
  effect on each block they take or set down.
- Tooltip: "Use: the ground nearby slowly remembers how it began". Rarity stays Rare.
- Still does nothing without Reworked Mobs or outside the Overworld (no cooldown either).

## Spec facts (specs/totems-artifacts.md "Totem of the Beginning"; specs/endermen.md ripple section)

- Totems: `TotemsTuning.BEGINNING_PULSE_*`: radius 8, 7 ticks/block, cooldown 1200 ticks, chance 0.12, ore chance
  0.012, recent window 12000 ticks. Old `BEGINNING_RADIUS`/`BEGINNING_TICKS_PER_BLOCK` (24/2) are now unused by the item
  (left in place to keep TotemsTuning edits additive; remove when the other builder's edits are merged).
- Reworked Mobs api/Endermen added `pulseAround(level, centre, radius, ticksPerBlock, chance, oreChance, recentTicks,
  player)` and `revertEffect(level, pos)`; `restoreAround` unchanged. Totems reaches them through
  `SleeperHook.pulseAround`.
- Pulse rules (Ripples.pulse / Odds): same ripple and protection as the full ripple, plus per change: skip unbreakable
  (destroy speed < 0), skip where `level.mayInteract(player, pos)` fails, skip a position reverted within the window
  (per-level map pos to game time, pruned at each pulse start), then roll the chance (ore chance when the generated
  block is in `c:ores`). Ore targets are allowed only in pulses and only into air (never replacing a placed block).
  Pile removal goes top down and stops at the first failed roll in a column, so piles shrink from the top; fills roll
  per block independently.
- Effect: REVERSE_PORTAL x6 + SCULK_SOUL x1 per block; SCULK_CATALYST_BLOOM at volume 0.35, pitch 0.5-0.6, at most
  once per `EndermanTuning.REVERT_SOUND_GAP_TICKS` (7) per level. Beacon/statue mode: EndermanJobGoal plays it on
  take/deliver when the position is inside an EndermanNexus area (only Beginning beacons/statues create those).
- Item use sound: SCULK_CATALYST_BLOOM at pitch 0.5 (was the totem-pop sound).

## Why these numbers

- 12%: a fully dug spot of terrain is ~70% back after 10 pulses (10 minutes), so healing a crater is a session's
  background chore, not instant; one pulse visibly changes something.
- Ore 1.2% plus the 10-minute window: an ore spot needs ~80 pulses (~80 minutes of standing and clicking) on average,
  and a block once refilled and re-mined can't refill for 10 minutes, so at best one ore per spot per 10 min even with
  lucky rolls. Mining normally is far faster: no ore farm.
- 60 s cooldown and radius 8 as the owner suggested.

## Tests

- Reworked Mobs milestone `EndermanTotemsGameTest.pulseOddsTerrainOverOre` (ten pulses at 0.12/0.012 over a dug-out
  field with ore two below the surface: first pulse fills 5-22% of 243 terrain cells; after ten, terrain > 50%, ore
  < 30% and < a third of terrain; ore spots only ever air or ore) and `pulseRecentWindowAndBuilds` (chance 1: re-dug
  reverted blocks stay empty on the next pulse, a cleared one refills; stone in an ore spot stays stone; a plank keeps
  its columns untouched).
- Totems milestone `BeginningEndGameTest.beginningPulsesOnlyWithReworkedMobs` (renamed from
  `beginningUsedOnlyWithReworkedMobs`): with -PwithMobs it is SUCCESS, count stays 1, cooldown full; without, PASS and
  no cooldown. Update the spec's milestone line.
- Client part `totems` (with -PwithMobs): screenshot `totems-beginning-pulse` (revert puffs on grass).
- Not tested: outside the overworld (Ripples.supported is unchanged and already covered by that check).
