<!-- dte:C1,B15,B16,B17 -->
# Reworked Mobs: the Boss Piglin (spec)

**Status: built** (owner idea, 2026-11-25; the orchestrator filled the open calls with defaults the owner may change: marked *(default)*). As built: "As built" below; numbers in `bosspiglin/BossPiglinTuning`.

A fat piglin boss on a throne in Nether outposts (`specs/monster-camps.md`, "Nether outposts"). It watches while its piglins fight, then waddles off to escape when they're all dead. Comic, and the outpost's treasure carrier.

## Look (owner)

- **Fat:** a wider, rounder piglin body (belly), a bit larger than a piglin; head and snout like a piglin's.
- **Trimmed gold armour:** vanilla gold helmet, chestplate, leggings and boots, each with a vanilla trim (snout, rib or eye pattern; gold/ netherite/ redstone materials), fitted to the fat body.
- **A cape**, like a player's: crimson or red with a gold trim or a piglin-banner motif.
- **Seated** on its throne while its outpost fights.
- **Waddle:** when it runs, a short-stepped, side-to-side waddle with arms flailing a little.

## The throne (owner)

- A **2 x 2 throne chair** of vanilla blocks (B15: no new block): e.g. polished blackstone brick stairs and slabs, gilded blackstone, gold blocks, a crimson or chain back; facing the outpost's fire.
- Placed by the outpost's footprint plan like its other pieces (no overlap), in **medium and large outposts** *(default)*, the boss seated on it facing out.

## Behaviour

- **While any of its outpost's roster lives:** it sits on the throne and doesn't move or attack. It may turn its head to follow the player and shout (piglin angry/ celebrate sounds) now and then.
- **When the last roster member dies** (the outpost's roster tracking, `CampSite` members): it gets up and **flees**: keeps away from players, and heads for the outpost's **portal room** *(default)*.
  - If the outpost's portal is **lit** and the boss reaches it, it **escapes**: it walks into the portal and is gone with its loot (no drops), with portal particles and sound.
  - If the portal is unlit or broken, it keeps running and hiding round the outpost.
  - **Too fat for the tunnel (owner, 2026-11-25: intended, "that's funny"):** when no path to the lit portal fits him (the outpost's 2-wide stair tunnel doesn't fit his 1.8-wide hitbox), or he stops getting closer for a few seconds, he gives up on the portal with a frustrated piglin grunt and just flees round the outpost; he tries again later. Tunnels are not widened for him.
- **Cornered** (a player within 2 blocks for a moment): a **weak slap** *(default)*: about a piglin's punch, knockback; then it keeps fleeing.
- **Stats:** more health than a piglin brute (builder: about 60-80), slower than a piglin when fleeing (a waddle; builder picks), knockback resistance a little higher.
- **Gold:** keeps vanilla piglin traits that fit (zombifies in the overworld: built outside the Nether it's made immune, like outpost piglins; not neutral to gold-wearing players once its outpost is attacked: builder's call).
- **Respawn:** it comes back with the outpost's roster on a reset *(default)*, seated again.

## Drops *(default)*

- Its trimmed gold armour pieces (each about 1 in 2, like a leader's gear but more generous), gold ingots and nuggets, a gold block sometimes, and the **snout armor trim template** (about 1 in 3). The netherite upgrade template stays in the outpost room chest.

## Milestone test objectives

- A medium/large outpost builds a throne (no overlap with other pieces) and its roster includes a seated boss; small outposts have none.
- The boss stays seated and harmless while any roster member lives; when the last dies it stands and moves away from a nearby player.
- With a lit portal, a fleeing boss that reaches it is removed without drops; with an unlit one it stays.
- A cornered boss deals about a piglin's punch.
- Drops roll as above over many kills.
- Client: seated on the throne (front, 3/4), waddling (side), the cape, the trimmed armour, at 16x and 32x.

## As built

- **Code:** `bosspiglin/` (server): `BossPiglin` (entity), `BossPiglins` (type, attributes, `seat(level, site)` with the roster), `BossPiglinTuning`, `ThroneDesign` (the throne's blocks and seat); `bosspiglin/client/` (renderer at `BossPiglinModel.RENDER_SCALE` 1.4, model, cape layer, render state). The outpost glue: `CampPiece` plans and builds the throne, `CampSite.Throne` saves it (anchor = front seat cell on the seated boss's left, facing, portal cell), `CampRoster.spawn` seats the boss.
- **Entity:** `bubbas_reworked_mobs:boss_piglin`, an `AbstractPiglin`. Hitbox 1.8 x 2.6 (eyes 2.2). Health 70, movement speed 0.25, attack damage 5 (a piglin's punch, empty hands), attack knockback 1.2, knockback resistance 0.3, follow range 48, 20 xp. Synced `seated` and `fleeing`; the client blends each 0..1 over 10 ticks. Spawn egg `boss_piglin_spawn_egg` (vanilla piglin egg icon), creative tab.
- **AI:** goals, not a piglin brain. The brain only registers the memories vanilla piglin code reads or writes on nearby piglins (anger broadcasts, hunting checks) and is never ticked. Goals, by priority: float; sit (held at the seat each tick, no gravity, no knockback, not pushable, no suffocation or fall damage; head follows the nearest player within 16); slap (a survival player within 2 blocks for 20 ticks; 40-tick cooldown; `doHurtTarget`); escape (path to the lit portal cell, accuracy 1, re-planned every 20 ticks; gives up for 600 ticks with a `piglin angry` grunt when no path reaches or it isn't 0.5 closer in 80 ticks); vanilla `AvoidEntityGoal` from players, 10 blocks, x1.0 / x1.2 (only while fleeing); vanilla wandering, home = outpost radius + 4; look around. While fleeing he plans paths over any drop (`getMaxFallDistance` = `BossPiglinTuning.FLEE_MAX_FALL` 64, owner: he can waddle off a cliff by accident). Stands when its outpost's members list is empty (checked every 10 ticks), stepping 2 blocks forward off the seat. Escapes when within 1 block of a portal block: discarded with portal particles and the portal trigger sound, no drops; `canUsePortal` is false so it never travels. Shouts: seated angry/celebrate, fleeing retreat, every 160 ticks on average, pitch x0.75. Hitting it angers nearby piglins (vanilla `PiglinAi.angerNearbyPiglins`). Egg/command bosses (no outpost) just flee.
- **Gear:** gold helmet (snout, netherite), chestplate (rib, redstone), leggings (eye, netherite), boots (rib, gold); equipment drop chance 0. Zombification immune when its outpost is outside the Nether.
- **Throne in the plan:** medium and large outposts; footprint from `ThroneDesign.blocks` (4 wide x 3 deep with the shelf armrests) plus 2 clear rows in front; front seat cell 3 to radius - 1 from the fire, facing it; planned after the gate and hoglin pen, before the lean-tos, from its own random stream (overworld camps and small outposts unchanged). Built last (after the fence tidy) on levelled ground with 4 clear above; four red cushions (vanilla cushion entities, `BossPiglins.placeCushions`) on the slab seat, so the seat height is 0.75 (owner); crimson shelf armrests (backs inward) and stripped crimson stem posts with fence tops framing the backrest (owner); a snack (gold ingot, golden carrot, gold nugget or cooked porkchop) on each armrest shelf.
- **With the roster:** not a member (doesn't count for clearing or resets). On each roster spawn a living boss of the outpost is sat back down; otherwise a new one is made if at least half the throne's blocks stand.
- **Drops** (`loot_table/entities/boss_piglin.json`): each armour piece 1 in 2 with its trim, 2-5 gold ingots, 6-15 gold nuggets, a gold block 1 in 5, the snout template 1 in 3. No killed-by-player condition.
- **Tests:** core `ReworkedMobsCoreGameTest.outpostThroneHasASeatedBoss`; milestone `BossPiglinGameTest` (thrones only in medium/large outposts; seated and harmless then flees; heads for a lit portal or gives up; escapes at a lit portal without drops; stays with an unlit one; slap about a piglin's punch; comes back with the roster; drops; trimmed gear); client part `bosspiglin` (`BossPiglinClientGameTest`).
- **Not covered:** a natural outpost's throne in a real Nether (the `outposts` client part wasn't rerun with it).
