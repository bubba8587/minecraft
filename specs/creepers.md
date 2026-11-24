<!-- dte:C1,B17,B18,B19,D2 -->
# Reworked Mobs: creepers (spec)

**Status: built.** A segment of Bubba's Reworked Mobs (C1). The design sections below are the owner's design as given; "How it's built" is what the code does.

## The watcher

A creeper is a **nervous watcher**, not a hunter:

1. **Motion draws it.** It notices things that **move in its field of view**, walks up to them and looks at them.
2. **It picks a moving entity to watch** (players first), gets close, **hides halfway behind a wall** if it can (peeking around cover), then **just watches** it for a while.
3. **If the entity moves out of view**, the creeper **repositions** to watch it again (players first).
4. **Sneaking up on it:** a player who approaches while sneaking makes it **stare at them while backing away behind a wall to hide**.

## Startle meter (what makes it explode)

- Things that **attack it** or **move quickly near it** startle it. Startles fill an **explosion meter**; a full meter means it explodes.
- **One attack** fills it partway, and it **calms down** from that. **Repeated attacks** keep building it until it blows.
- **Repeated speed** near it (sprinting, fast movement) builds the meter the same way.
- So a player can approach a creeper calmly; rushing it, sprinting around it or hitting it repeatedly sets it off.

## Spawning (owner)

- **Direction for the whole mobs mod:** almost no random spawning. Mobs come from deliberate placement (sleepers, structures, camps), not vanilla's random spawns.
- **Creepers:** vanilla's random natural creeper spawns are off. The mod spawns its own "natural" creepers at night:
  - **behind cover, just outside the player's view**, at a watching distance;
  - **at most one at a time per player** (owner: each player gets their own watcher).
  - At any point at night the player could turn around fast and see one, half behind a tree in the distance, looking at them.
  - Creepers from spawners, structures and commands are unaffected.
- **Peekaboo:** a half-hidden creeper that the player looks at moves about **95 % behind its cover** until they look away, then eases back out and keeps watching.

## Blindness

A **blinded** creeper can't see motion, so all of the above is off while it's blind (e.g. after a Blindness gem hit).

## Charged creepers

Charged creepers keep vanilla's suicide-bomber behavior.

## Owner answers

- **C1 fuse:** a full meter starts vanilla's 1.5 s hiss and swell. During the hiss it **defuses** if nothing attacks it and the target either **sneaks** or **moves outside its trigger range**. A player who hears the hiss, crouches and backs up is safe.
- **C2 watching:** it **never stops watching a player** within aggro range (24 blocks, widened by the owner from vanilla's 16; see Numbers). The meter still drains when nothing startles it (that is the calming down).
- **C3 fast movement:** **any living thing moving faster than a walk within about 6 blocks** startles it: panicking animals, hopping rabbits, running horses and mobs, and players sprinting, gliding (elytra or Glider) or riding. Being knocked toward it (e.g. by Wind Burst) counts too (B18: the same rule for everyone).
- **C4 projectiles:** projectiles flying close past it (near-misses) startle it, not just hits. So rapid things like snowballs set it off from afar; to kill one without an explosion, slay it in melee or one-shot it with a strong bow.
- **C5 no cover:** on open ground with nothing to hide behind, it creeps to an **uncomfortably close** distance and stares: just outside the range where a player could sprint and hit it in one motion, so dashing toward it makes it back up (owner: numbers to be playtested).

## How it's built

**No new entity type.** `mixin/CreeperMixin` gives every vanilla `Creeper` the behavior (natural spawns, eggs, commands).
- **Except spawner creepers:** creepers from spawners and trial spawners stay vanilla (owner: "spawners should spawn vanilla mobs, for now"). `MobFinalizeSpawnMixin` unwraps the hunting goals and drops the watcher's; saved as `bubbas_reworked_mobs:vanilla`.
- Vanilla's hunting goals (both target goals, `MeleeAttackGoal`, `SwellGoal`) are wrapped in `ChargedOnlyGoal`, so they run only while the creeper is charged. A charged creeper's goal list is exactly vanilla's.
- Uncharged creepers get `CreeperFuseGoal` (priority 2, vanilla SwellGoal's slot) and `CreeperWatchGoal` (priority 4, the melee slot).
- Floating, cat/ocelot avoidance, strolling and looking around stay vanilla (they run when it has nothing to watch).
- Code: `creeper/` package; all numbers in `CreeperTuning`.

**Senses (`CreeperMind`, one per creeper, server only, not saved).** Ticked from `Creeper.tick`.
- Every 4 ticks (phase spread by entity id): one box query of 24 blocks (`WATCH_RANGE`) for living things. Speed = distance moved since the last scan / 4 (horizontal, plus vertical at half weight).
- Every 2 ticks: a box query of 8 blocks for moving projectiles.
- *Watching:* moving things (>= 0.03 blocks/tick) in its 120-degree field of view and in line of sight catch its eye; players first (nearest), else the nearest other mob. A player is watched while within 24 blocks, whatever they do (C2); anything else for 15 s, then it may pick something new. Never other creepers, spectators or invisible things.
- *Startle meter:* 0-100, drains 0.5 a tick (10/s) when nothing startles it; frozen while hissing.
  - An attack (any damage with an attacking entity: hits, arrows, thrown things): +40 (Fabric `ServerLivingEntityEvents.AFTER_DAMAGE`). Falls, cactus and fire don't startle it (vanilla's own fall-swell still shows).
  - Anything living moving faster than 0.24 blocks/tick within 6 blocks and in line of sight (field of view not needed): +8 per scan, per mover. Covers sprinting, gliding, riding, panicking or hopping animals, running mobs and being knocked back (B18, one rule). A rider counts through its mount, once.
  - A projectile whose path passes within 2 blocks of its middle: +25, once per projectile (near-misses, C4).
- *Full meter:* the hiss is vanilla's swell (30 ticks, primed sound, swelling). It stands still and stares at whoever startled it last.
  - It defuses (meter drops to 60, swell runs back down) when nothing has hit it since the hiss began and that one sneaks (shift held), is beyond 7 blocks (vanilla SwellGoal's range) or is gone (C1).
  - A hit during the hiss means sneaking no longer saves the player.
  - Flint and steel lights any creeper the vanilla way.

**Watching (`CreeperWatchGoal`, `CoverFinder`).**
- *Peek spots ("halfway behind a wall"):* worked out from the watched entity's eyes. A spot is a floor cell where, sliding sideways across the line of sight, the view of the creeper's eyes goes from clear to blocked by any collision shape (trunks, walls, boulders; leaves count, like vanilla line of sight).
  - Half-hidden: its middle 0.05 blocks on the open side of that edge (eyes peek, about half the body is behind cover).
  - Hidden: 0.27 past the edge (body 0.6 wide: about 95 % behind).
- *Spot search* (owner: they seek out walls much farther off and stay farther away):
  - Candidates: every cell within 5 blocks (11 x 11), plus 72 random cells. Half of the random ones are within 14 blocks of the creeper; half are on its side of the ring of wanted distances round the target (within 75 degrees of its bearing, so it never crosses in front). At most 22 blocks' walk.
  - A cell qualifies if it is 10-22 blocks from the target with something solid at head height within 2 cells on the target's side.
  - The best 16, ranked by "distance near 15, then a short walk (x0.3)", get the raycast test (7 + 3 rays each).
  - Runs at most once a second, only while it has no spot. A held spot is re-checked every 10 ticks (the target moves).
  - Its navigation plans paths up to 32 blocks (`setRequiredPathLength`; vanilla uses its 16-block follow range; targeting range unchanged).
- *Peekaboo (owner):* while it sits at its spot and the watched player looks within 45 degrees of any of five points on its body or its half-hidden place, it slides into the hidden place; when they look away it eases back out to half. `sense/Gaze` uses a cone round several points, not one line to its eyes, and no sight check, so once tucked away it stays hidden while the player looks its way. Slides are 0.08 blocks a tick, sideways, still staring.
- *Open ground (C5; owner reworked the distances: "far too close"):* with no cover in reach it holds about 12 blocks off and stares.
  - Looked at (the cone above) within 16 blocks: it edges away.
  - Closer than 10: it backs away (speed x1.2), so dashing at it makes it back up.
  - Out of sight for 1 s (and no spot): it walks toward the target to see it again.
- *Sneaking up:* a sneaking player within 18 blocks who is getting closer makes it look for cover farther away (1.5-8 blocks beyond its current distance) and sit hidden there, staring. With no cover it backs away.

**Blindness:** a creeper with the vanilla Blindness effect senses nothing: the meter only drains, it watches nothing, it ignores hits, and a hiss in progress defuses. Infused Gems' Blindness gems reach it only when they apply the vanilla effect (no gem-specific code; nothing to do when that mod is absent).

**Charged:** vanilla in every way (vanilla's goals run; our meter stays empty).

**Spawning (owner).**
- Vanilla's random natural creeper spawns are blocked (`SpawnStateMixin`, the same hook as the monster cap cut). Creepers from a structure's own spawn list, spawners, trial spawners, eggs and commands are untouched.
- Instead each player gets **one** "natural" watcher (owner's answer):
  - every 10 s at night (`isDarkOutside`), for each player (not spectators) with no watcher of their own within 48 blocks;
  - up to 16 random tries for a spot 15-21 blocks away and outside 70 degrees of where they're looking;
  - the spot must pass vanilla's own rules (the biome's natural monster list includes creepers, the creeper's spawn placement, the light/darkness rule) and be a peek spot from the player's eyes whose hidden place is out of their sight.
  - It spawns there hidden, already watching the player, then eases out to half-hidden. A player who turns round fast can catch it half behind the tree; it ducks back when looked at.
- A watcher remembers whose it is (persistent attachment `bubbas_reworked_mobs:watcher_for`), so reloads keep the one-per-player count. One more than 48 blocks from its player no longer counts as theirs. Watchers despawn like any vanilla creeper (not persistent).
- Vanilla's `spawn_mobs` and `spawn_monsters` rules and Peaceful turn it off.
- Game rule `bubbas_reworked_mobs:watcher_creepers` (default true, "Creepers spawn as watchers"): false brings vanilla's random spawns back and stops ours (for spawn-control mods). Off on the game test server (tests call the spawner directly).

**Visible meter (owner):** with the game rule `bubbas_reworked_mobs:show_meters` on (default off), the startle meter shows as a red 20-mark bar above the creeper (`meter/Meters`, shared with the drowned's grip bars; synced only while the rule is on and the value changes). See `specs/drowned.md`.

**Cost.**
- Per creeper: one 24-block entity box query every 4 ticks, one 8-block projectile query every 2 ticks (class-filtered), line-of-sight checks only for things that move.
- Cover search: at most once a second while it has no spot; a few block lookups per cell plus at most ~120 short raycasts.
- Spawner: every 10 s per player without a watcher, at most 16 column probes and a few raycasts.

## Numbers (all in `CreeperTuning`; builder's starting values, owner to playtest)

| | Value |
|---|---|
| Meter full / decay | 100 / 0.5 a tick (10/s) |
| One attack | +40 (three sword hits 0.6 s apart set it off; one or two calm in ~4 s) |
| Fast movement | faster than 0.24 blocks/tick (walk 0.216, sprint 0.28) within 6 blocks, +8 per 4-tick scan per mover (sprinting circles set it off in ~3.3 s) |
| Near-miss | path within 2 blocks, +25 once per projectile (four snowballs) |
| After defusing | meter 60 |
| Defuse range | 7 blocks (vanilla) |
| Field of view / notice speed / watch range | 120 degrees / 0.03 blocks/tick / 24 blocks (paths up to 32) |
| Non-player watched for | 15 s |
| Open ground: holds / backs off under / edges back to when looked at | 12 / 10 / 16 blocks |
| Peek spots | 10-22 blocks from target (best 15); every cell within 5 of the creeper plus 72 random ones up to 22 blocks' walk |
| Peekaboo look cone | 45 degrees round any of 5 body points |
| Slide speed | 0.08 blocks a tick |
| Sneak notice | 18 blocks |
| Watcher spawning | 1 per player, every 10 s at night, 15-21 blocks, outside 70 degrees of view, released beyond 48 blocks |

## Builder's choices (the owner may change these)

1. **Creative players are watched and startle creepers** (`IGNORE_CREATIVE_PLAYERS = false`), so the owner can try it in creative; vanilla's targeting ignores creative players. Flying fast in creative near a creeper startles it (it can't hurt the player there).
2. Fast movement needs line of sight but not the field of view (a sprint behind it still startles it); noticing something to watch needs both.
3. Only damage with an attacker startles; environmental damage doesn't.
4. A blinded creeper ignores hits too, and blinding it mid-hiss defuses it ("all of the above is disabled").
5. Creepers never watch or startle each other (no chain reactions from a backing-up creeper).
6. Defusing drops the meter to 60 rather than 0, so a defused creeper is still jumpy (no cooldown, B19: just a meter value).
7. The meter isn't saved; a reloaded creeper starts calm.
8. The fuse target is whoever startled it last (the last attacker, mover or shooter), else what it watches.
9. Sneaking means holding sneak (`isShiftKeyDown`), checked every tick during the hiss.
10. Spawns only at night (`isDarkOutside`), above ground or in caves, as long as vanilla's darkness rule passes at the spot; every 10 s.

## Test world station

The creeper grove (`specs/testworld.md`, Layout).

## Milestone test objectives

- **Core** (every build, `ReworkedMobsCoreGameTest`): `creeperSprintingNearbyStartlesWalkingDoesNot`.
- **`CreeperGameTest`** (milestone):
  - `hitsFillTheMeterAndItDrains`;
  - `fullMeterHissesAndDefuses` (sneak, then backing out of range; then a hit during the hiss means sneaking no longer saves the player);
  - `blindedCreeperIgnoresMotion`;
  - `chargedCreeperStaysVanilla`;
  - `nearMissesStartle` (a snowball 1 block off counts, one 7.5 off doesn't);
  - `hidesWhenLookedAt` (behind a wall it walks to a half-hidden spot with its eyes in view; peekaboo: hidden when looked at, half out again when looked away);
  - `oneWatcherSpawnsBehindCoverOutOfView` (spawns out of sight, outside the view cone, 15-21 blocks away, watching; one per player; vanilla random spawns blocked). It failed once in about 12 runs early on and never again (maxTicks 200); watch it.
- **`CreeperClientGameTest`** (client part `creepers`): screenshots `creeper-peek` (from the watched armor stand: half behind a one-log tree), `creeper-peek-side`, `creeper-hidden` (player looking at it), `creeper-caught` (looked away, then straight back), `creeper-startle-bar` (the `show_meters` bar after two hits).
- **Not yet tested (objectives):**
  - with cover 18 blocks off and none nearer, it walks there (far cover);
  - in the open it holds 12 and edges back while looked at;
  - it backs off when dashed at in the open;
  - peekaboo triggers when the player looks within 45 degrees of its edge, not only its eyes;
  - riders and mounts are counted once;
  - knockback toward it startles;
  - gliding with elytra startles;
  - a sneaking player closing in makes it back behind cover;
  - it repositions when the target leaves its view;
  - the game rule off brings vanilla spawns back.

## Open

- Owner to playtest the numbers (all in `CreeperTuning`) and C5's open-ground distances.
- Owner to review the 10 builder's choices above.
