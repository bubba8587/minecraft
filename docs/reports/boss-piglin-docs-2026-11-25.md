# Boss Piglin: facts for the spec and player docs (implementation agent, 2026-11-25)

For `player-docs/` (Reworked Mobs page, Nether outposts) and `specs/boss-piglin.md` (already updated to as-built in this branch). Art facts are in `boss-piglin-art-docs-2026-11-25.md`.

## Player-facing text (draft)

**The Boss Piglin.** Medium and large Nether outposts have a throne by the fire, and on it sits the outpost's boss: a huge, fat piglin in trimmed gold armour and a crimson cape. While any of the outpost's piglins are alive he just sits there, watching you and shouting now and then. He doesn't fight.

Kill the last of his piglins and he gets up and runs. He waddles away from you and heads for the outpost's portal room: if the portal is lit and he gets to it, he steps through and is gone, loot and all. He is too fat for the stair tunnel down to the portal room, though, so mostly he just gives up with an angry grunt and keeps running round the outpost. If the portal is unlit or broken he hides round the outpost too. Corner him (stand right next to him) and he slaps you, about as hard as a piglin's punch, and knocks you back, then runs again.

Hitting him makes the outpost's piglins angry at you, even if you wear gold. When the outpost resets (its banner still standing, a day with nobody near), he is back on his throne with the new piglins; if he is still alive somewhere he goes back and sits down.

Drops: each of his four trimmed gold armour pieces about half the time (helmet: snout trim, netherite; chestplate: rib, redstone; leggings: eye, netherite; boots: rib, gold), 2-5 gold ingots and 6-15 gold nuggets every time, a gold block 1 in 5, and a snout armor trim template 1 in 3. There's a gold snack on each armrest of his throne.

Spawn egg: Boss Piglin Spawn Egg (vanilla piglin egg icon), in the Reworked Mobs creative tab. A boss made by egg has no throne and just flees; like any piglin it zombifies outside the Nether.

## Numbers (`bosspiglin/BossPiglinTuning`)

| What | Value | Compare |
|---|---|---|
| Health | 70 | piglin 16, brute 50 |
| Movement speed | 0.25 | piglin 0.35 |
| Flee speed (walk / player within 7) | x1.0 / x1.2 | |
| Keeps away from players | 10 blocks | |
| To the portal | x1.1 | |
| Slap damage | 5 | a piglin's punch |
| Slap knockback (attack knockback) | 1.2 | piglin 0 |
| Cornered | a player within 2 blocks for 20 ticks; 40 ticks between slaps | |
| Knockback resistance | 0.3 | piglin 0 |
| Follow range (path length) | 48 | |
| XP | 20 | |
| Hitbox | 1.8 wide x 2.6 tall, eyes 2.2 | piglin 0.6 x 1.95 |
| Shouts | every 160 ticks on average, pitch x0.75 | |
| Portal: give up | no path that fits him, or 80 ticks without getting 0.5 closer; tries again after 600 ticks | |
| Steps through a portal | within 1 block of a portal block | |
| Seated and fleeing looks blend | over 10 ticks | |

Throne placement (`CampPiece`): medium and large outposts only, the front seat cell 3 to radius-1 from the fire, facing it; 4 wide x 3 deep plus 2 clear rows in front; planned after the gate and hoglin pen, before the lean-tos, from its own random stream (so overworld camps and small outposts are unchanged; medium and large outposts get new layouts).

## Builder's choices

- **AI:** the boss is an `AbstractPiglin` driven by goals, not a piglin brain. Its brain registers only the memories vanilla piglin code reads or writes on any nearby piglin (anger broadcasts, hunting checks), so those calls don't crash, and is never ticked. Goals: float; sit (holds the seat, head follows the nearest player within 16); slap; escape (path to the lit portal); vanilla `AvoidEntityGoal` from players (only while fleeing); vanilla wandering within the outpost (radius + 4); look around. Simpler than a custom brain, and every piece of movement is vanilla navigation.
- **Seated:** held at the seat each tick with no gravity, no knockback, not pushable, immune to suffocation and fall damage while seated. Hurt while seated, he stays seated.
- **Standing up** steps him 2 blocks forward off the seat (clear rows in front of the throne).
- **Escape:** discard with portal particles and the portal whoosh, no drops; he never uses portals the vanilla way (he would otherwise travel to the overworld).
- **Too fat for the tunnel (owner, intended):** the outpost's 2-wide stair tunnel doesn't fit a 1.8-wide pathing mob, so a throne boss normally can't reach the portal room; he gives up with a `piglin angry` grunt and flees round the outpost. Tunnels are not widened. A boss that is somehow in the portal room escapes through a lit portal (tested).
- **Hitting him angers nearby piglins** (vanilla `PiglinAi.angerNearbyPiglins`), gold or not.
- **Not on the roster:** he isn't counted for clearing or resets. He is re-made with the roster when none of his is alive and at least half of his throne's blocks still stand; a living one is sat back down.
- **Zombification:** immune when his outpost is outside the Nether (command, tests), like the outpost's piglins; an egg boss zombifies like any piglin.
- **Trims:** fixed per piece (above), so the loot table drops exactly what he wears; equipment drop chance 0.
- **No boss bar** (the spec doesn't ask for one).
- Saved: outpost id, seat, seat yaw, portal cell, seated, fleeing. The camp site saves the throne (anchor, facing, portal cell).

## Tests

- Core: `ReworkedMobsCoreGameTest.outpostThroneHasASeatedBoss`.
- Milestone `BossPiglinGameTest`: thrones in medium/large outposts only (64 seeds per kind and size); seated and harmless while the roster lives, stands and moves away once it's gone; heads for a lit portal (gives up when no path fits him, else escapes); escapes when he reaches a lit portal (no drops); stays with an unlit portal; cornered slap about a piglin's punch; comes back with the roster (same boss sat down, or a new one); drops over 400 rolls; wears the trimmed gold.
- Client part `bosspiglin` (`BossPiglinClientGameTest`): seated front / 3/4, empty throne, armour close, cape from behind, waddling side, seated front and cape at 32x.

## Open

- The boss can't reach the portal room from his throne (tunnel too narrow, intended). If the owner wants escapes to happen in play, the tunnel would need a 2-wide clear path for a 1.8 mob (3 wide) or a smaller hitbox.
- Natural outposts in a real Nether: not shot or tested with the throne yet (the `outposts` client part was not rerun).
