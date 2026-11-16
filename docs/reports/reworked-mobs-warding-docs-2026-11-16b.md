<!-- dte:C1 -->
# Reworked Mobs, Warding round 2: docs to merge (2026-11-16)

Builder's report on the owner's answers to the first Warding round. For the orchestrator or a docs agent to merge into `specs/undead.md` ("Warding") and the player vault (Totem of Warding page, Noise and Waking if it mentions wards, Monster Spawning). The spec's milestone objectives are already updated.

## Player-facing changes

- **Evokers** now drop a Totem of Warding only rarely: **5 %** per evoker (was every evoker). Chests stay the main source: woodland mansion 20 %, ancient city 12 %, trial vault 8 %, ominous vault 15 % (unchanged).
- **Bells** are unchanged: walls still muffle them for sleepers (owner).
- **Wards keep out every monster**, including ones that don't walk by paths: phantoms, ghasts, vexes, slimes, magma cubes, blazes, endermen teleporting or monsters knocked in. At the ward's edge they stop as if at an invisible wall (no damage; they can slide along it). Any of them already inside (there when the ward went up, spawned by a spawner) drifts out through the nearest side.
- **Happy ghasts are kept out too**, wild or ridden. A ghast a player is steering is pushed back outside the edge (up to a block a tick); the rider can get off and walk in.
- (Unchanged, owner pending: mobs holding a totem pulse; woken undead may pick one up.)

## Spec facts

- `WardingTuning.LOOT`: evoker 0.05.
- `warding/WardBarrier` (new): runs at the end of every kept-out mob's own tick (mixin `MobTickMixin`, TAIL of `LivingEntity.tick`, server only) when the mob is within 3 blocks of an active ward; one map check per mob tick when no ward exists anywhere. "Kept out" = `instanceof Enemy` or a happy ghast (`WardBarrier.keptOut`, also used by path blocking, so happy ghasts don't path in either).
  - Outside at the edge and moving in: the inward part of its motion is dropped.
  - Crossed in since the last tick (it was seen outside within 3 blocks the tick before): moved back onto the edge along the exit side, inward motion dropped. The mob's own old-position fields weren't reliable for this, so the barrier keeps its own "last seen outside" note per mob (weak map).
  - Already inside: 0.06 blocks per tick of outward speed each tick, through the nearest side (top or bottom too for mobs without gravity); walkers also walk out (`WardFleeGoal`, unchanged).
  - Steered vehicle (a mob whose controlling passenger is a player, i.e. a harnessed, ridden happy ghast): it moves on the player's client, so the server moves it out by min(depth + 0.6, 1) blocks a tick and sends `ClientboundMoveVehiclePacket` to the rider (vanilla's own correction for a vehicle that moved wrongly).
  - Passengers are skipped (their vehicle is moved).
- The Totem of Warding code stays self-contained in `warding/` (plus `compat/WardingBeaconPayload`, the bell and sleeper hooks in `undead/sleeper/`, and the mixins), ready for the planned move to Bubba's Totems & Artifacts.

## Tests

- Core: `wardPushesBackMobsThatDoNotPath`: a slime pushed at the ward every tick never gets in; a slime and a ghastling that start inside drift out. (A ghastling, not an adult happy ghast: a 4-block adult hits the test area's walls.)
- Milestone: `riddenHappyGhastIsPushedOut` (a server-side player steering a harnessed happy ghast 0.5 blocks inside is put back outside within 2 ticks); the loot test now checks evokers at about 5 % over 1000 rolls.
- Test note: the light test (`strongLightCalmsSleepers`) now waits up to 600 ticks. The game test server's light engine sometimes hadn't lit the glowstone after 200 ticks when the full suite ran.

## Limitations to mention to the owner

- Ranged monsters outside a ward can still shoot in (unchanged).
- A ridden happy ghast's push-back is server-corrected, so on a laggy connection the rider may see a small rubber-band at the edge.
