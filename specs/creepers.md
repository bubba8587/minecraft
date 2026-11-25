<!-- dte:C1,B17,B18,B19,D2 -->
# Reworked Mobs: creepers (spec)

**Status: built.** A segment of Bubba's Reworked Mobs (C1). The design sections below are the owner's design as given; "How it's built" is what the code does.

## The watcher

A creeper is a **nervous watcher**, not a hunter:

1. **Motion draws it.** It notices things that **move in its field of view**, walks up to them and looks at them.
2. **It picks a moving entity to watch** (players first), gets close, **hides halfway behind a wall** if it can (peeking around cover), then **just watches** it for a while.
3. **If the entity moves out of view**, the creeper **repositions** to watch it again (players first).
4. **Sneaking up on it:** a player who approaches while sneaking makes it **stare at them while backing away behind a wall to hide**.
5. **Cover to cover (owner, 2026-11-24):** instead of approaching in an empty field it **always hides behind a wall unless there is literally zero cover**, creeping to **closer and closer walls** while not looked at. **If you approach it, it runs to the next farther wall.** Owner's picture: "behind a tree out your window, look away, it walks up to your wall and looks through the window; run at it, even from inside the house, and it might run away toward the tree again".
6. **Leaving by day (owner, 2026-11-24):** it **walks behind a wall before despawning** in the daytime, and is **more likely to despawn if you don't look at it**.

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
- **C5 no cover:** only when there is no cover at all (watcher item 5): on open ground with nothing to hide behind, it creeps to an **uncomfortably close** distance and stares: just outside the range where a player could sprint and hit it in one motion, so dashing toward it makes it back up (owner: numbers to be playtested).

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
- *Plans:* WATCH (hold a spot, peekaboo), APPROACH (walking to a closer spot), FLEE (running to a farther spot), HIDE (the sneak rule), LEAVE (heading out of sight by day).
- *Peek spots ("halfway behind a wall"):* worked out from the watched entity's eyes. A spot is a floor cell where, sliding sideways across the line of sight, the view of the creeper's eyes goes from clear to blocked (trunks, walls, boulders; leaves count, like vanilla line of sight).
  - Line of sight for cover uses `ClipContext.Block.VISUAL`: in vanilla 26.3 `TransparentBlock` (glass, stained, tinted) and `IronBarsBlock` (panes, bars) have an empty visual shape, so it sees and peeks through glass, panes and bars; leaves and doors use their collision shape. The startle and watching senses still use vanilla `Sensing` (collision), so glass still blocks those.
  - Half-hidden: its middle 0.12 blocks on the open side of that edge (eyes and face show round a window frame; at 0.05 a pane's frame hid it completely).
  - Hidden: 0.27 past the edge (body 0.6 wide: about 95 % behind).
- *Spot search* (owner: they seek out walls much farther off):
  - Candidates: every cell within 5 blocks (11 x 11), plus 72 random cells. Half of the random ones are within 14 blocks of the creeper; half are on its side of the ring of wanted distances round the target (within 75 degrees of its bearing, so it never crosses in front). At most 22 blocks' walk.
  - *Cover sweep:* 32 level rays from the target's eyes over the creeper's side (±75 degrees of its bearing); where neighbouring rays end more than a block apart there is a cover edge, pinned with 2 more rays (bisections); cells 0.7 and 1.5 blocks behind each edge become candidates. Random samples alone missed a lone trunk, and cells at the foot of a 1-block step used up the whole budget.
  - A cell qualifies if it is within the search's distance band from the target with something solid at head height within 2 cells on the target's side.
  - Ranked by "distance near the preferred one, then a short walk (x0.3)", then raycast-tested (7 + 3 rays each): edge cells get their own budget of 24 evaluations, other cells 16. `evaluate` rejects a cell after its 2 outermost rays when both agree (no edge across it), so most rejects cost 2 rays.
  - *First spot:* 3-22 blocks from the target, best near min(current distance, 15). A held spot is dropped when the target is outside 2-24 blocks; it is re-checked every 10 ticks (the target moves).
  - Runs at most once a second while it has no spot.
  - Its navigation plans paths up to 32 blocks (`setRequiredPathLength`; vanilla uses its 16-block follow range; targeting range unchanged).
- *Peekaboo (owner):* while it sits at its spot and the watched player looks within 45 degrees of any of five points on its body or its half-hidden place, it slides into the hidden place; when they look away it eases back out to half. `sense/Gaze` uses a cone round several points, not one line to its eyes, and no sight check, so once tucked away it stays hidden while the player looks its way. Slides are 0.08 blocks a tick, sideways, still staring.
- *Creeping closer (APPROACH):* at a reached spot, once it hasn't been looked at (peekaboo cone) for 3 s, it tries every 100-160 ticks: `CoverFinder.findAll` for up to 4 spots between 2 blocks and (distance - 2), best near (distance - 6). Each gets a path (`PathNavigation.createPath`), scored by the share of 6 points along the route the player can see, plus 1 if its hidden place is in view; the lowest wins. So it prefers spots whose hiding place and route are out of view, and can come right up to a house wall and peek in round a window frame or doorway (closest 2 blocks, owner). Caught walking up in the open (looked at while it can be seen), it goes back to the spot it came from.
- *Running off (FLEE):* every 10 ticks it compares the player's own position now and 20 ticks ago against its current position (its own movement doesn't count). A player within 20 blocks, not sneaking, who closed by 2+ blocks in 20 ticks (0.1 a tick; walk 0.216, sneak 0.065) makes it run: flee spot (distance + 3) to max(22, distance + 14), capped at 23, best near distance + 7, at speed x1.4 (0.35 blocks/tick, faster than a sprint). No spot: it backs away (`DefaultRandomPos.getPosAway`, 10 blocks) at that speed until 3 blocks farther. No new approach for 10 s after. Only when it can see the player at that moment (owner): `CreeperWatchGoal.noticeApproach`, every 10 ticks, `hasLineOfSight` with `ClipContext.Block.VISUAL` from its eyes to the player's eyes or middle, so glass, panes and bars don't block it but solid blocks and leaves do; tucked fully behind its cover it doesn't notice. A player walking straight at it while looking makes it duck (peekaboo), and once ducked it can't see them, so it may not run (playtest). Its own running startles nothing, since creepers never count on the startle meter (builder's choice 5).
- *Sneaking up (HIDE):* a sneaking player within 18 blocks who closed by 0.3+ blocks in 20 ticks makes it look for cover farther away (1.5-8 blocks beyond its current distance) and sit hidden there, staring, at a walk. Same sight rule as running off. With no cover it backs away.
- *Open ground (C5; owner reworked the distances: "far too close"):* only when it finds no cover at all in 3 searches in a row in WATCH (`CreeperMind.openGround()`); any later find clears it. With a tree or wall around it always holds a cover spot. In the open it holds about 12 blocks off and stares.
  - Looked at (the cone above) within 16 blocks: it edges away.
  - Closer than 10: it backs away (speed x1.2), so dashing at it makes it back up. With cover, coming at it makes it run to farther cover instead.
  - Out of sight for 1 s (and no spot): it walks toward the target to see it again.
  - With `mobGriefing` on, it first tries to make its own cover (Pillars, below); the open-ground stance is the fallback when it can't.
- *Pillars: making its own cover (owner's design, decided 2026-11-25):* on open ground it digs up two surface blocks near it, one at a time, carries each on its head and stacks them into a 2-high pillar, then peeks from behind it like any other cover. Blocks move exactly as they are, never as drops (a grass block stays grass; the lower block of a grass pillar may later turn to dirt by vanilla's rule, since a block sits on it). Never a hole deeper than 1. If a lone natural block already sticks up out of level ground at a good spot, it builds on that and adds only one. Code: `creeper/CreeperPillar.java` (the job), run by `CreeperWatchGoal`; numbers in `CreeperTuning` (`PILLAR_*`).
  - *Trigger:* in WATCH, after 3 empty cover searches in a row (`OPEN_GROUND_AFTER_FAILED_SEARCHES`), `CreeperPillar.tryStart`: needs `mobGriefing` (as endermen; off: the open-ground stance, nothing else changes), not leaving (day), no block carried, and game time past `CreeperMind.nextPillarTime`. A plan sets that to now + 6000 ticks (5 min); no site found: now + 600 (30 s) and the open-ground stance as before. Not saved (watchers aren't persistent).
  - *Site search* (block lookups only, once per try): every column within 6 blocks of the creeper (13 x 13), 6-18 blocks from the target, scored |distance - clamp(current distance, 8, 12)| + 0.3 x walk; -3 for a lone block it can build on; +6 when the watched player's look cone (45 degrees, `Gaze`) covers the site. A site needs:
    - fresh: an empty standable cell with a sturdy top under it (fills that cell and the one above); or a lone block: a `creeper_pillar` full cube with at least 3 of 4 sides open and a sturdy block under it (fills the cell on top);
    - a stand cell 1.4 blocks behind the pillar (away from the target), with the cells either side of it standable (its peek cells);
    - the view from the target's eyes to that stand point open now (else there was cover already);
    - enough dig blocks (2 or 1, plus 2 spares).
  - *Dig blocks:* columns within 4 blocks of the pillar, nearest first, not the pillar's or stand cells' columns, never next to another chosen one. The top block of the column (within 1 up / 2 down of the floor), in block tag `#bubbas_reworked_mobs:creeper_pillar` (data: grass, dirt kinds, mycelium, podzol, mud, packed mud, moss, sand, red sand, gravel, clay, snow block, `#minecraft:base_stone_overworld`, cobblestone, mossy cobblestone, cobbled deepslate, sandstone, red sandstone, calcite, dripstone block, `#minecraft:terracotta`), a full collision cube, no block entity, breakable, not wither-immune, open above, sturdy below, and no neighbour column higher than it (so every dent is 1 deep).
  - *The job:* walk to the dig block (within 1.6, or 2.6 once the walk ends), a 12-tick pause looking at it, take it (`removeBlock`, vanilla break particles and sound via level event 2001, `BLOCK_DESTROY` game event), walk to the stand cell, an 8-tick pause looking at the target cell, place the same state (`setBlock` with full updates, the block's place sound, `BLOCK_PLACE` game event). It steps aside if it stands in the cell and waits while something else does. Gives up after 600 ticks (frozen time not counted) or 4 failed walks to one block (then the next spare). About 3-4 s for a 2-block pillar on flat ground.
  - *Caught building:* looked at (peekaboo cone) while the target can see its eyes, navigation stops and it stands and stares; it resumes when looked away. Walking at it makes it run off as usual.
  - *Abort* (plan leaves WATCH, a spot is taken, target changes, goal stops: fuse, blindness, charged): the carried block goes back into its dent if still open, else to a free cell beside it, else drops as an item. On death the same (`ServerLivingEntityEvents.AFTER_DEATH`).
  - *Carried block:* attachment `bubbas_reworked_mobs:creeper_carried` (BlockState, persistent, synced to all clients). A creeper loaded with one but no job sets it down beside itself.
  - *Look:* `client/creeper/CreeperCarriedBlockLayer` (Fabric `LivingEntityRenderLayerRegistrationCallback`) draws the carried block at 0.55 scale on top of its head, turning and tilting with the head; the render state field is added by `CreeperRenderStateSkinMixin` and filled in `CreeperRendererSkinMixin`'s extract hook with the renderer context's `BlockModelResolver` (the enderman's display context).
  - *Afterwards:* `CreeperMind.pillar()` holds its base, and the ordinary cover search finds the peek spot behind it; later it uses the pillar like any other cover.

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
- *Leaving by day:* `CreeperMind.tickLeaving` runs once a second, only for creepers with the `watcher_for` attachment, only while `Level.isBrightOutside()` (dark again resets it). Spawner, structure, egg and command creepers never leave.
  - A random countdown of 200-3000 ticks (10 s to 2.5 min), ticking down 2x while its player hasn't looked toward it (peekaboo cone) for 600 ticks; then it is leaving.
  - The goal's LEAVE plan picks a peek spot (3-23 blocks, best distance + 7) whose hidden place is out of the player's sight and sits hidden there; none found: it walks away 16 blocks at a time.
  - While leaving, not hissing, and no non-spectator player within 128 blocks has a clear view (visual clip) of its eyes, middle or top: `discard()` with chance 0.2 + 0.05 per second since its player last looked toward it, per second. Hitting or startling it while it leaves works as usual.
  - `CreeperMind.leaveIn(ticks)` starts the countdown (tests).
- Vanilla's `spawn_mobs` and `spawn_monsters` rules and Peaceful turn it off.
- Game rule `bubbas_reworked_mobs:watcher_creepers` (default true, "Creepers spawn as watchers"): false brings vanilla's random spawns back and stops ours (for spawn-control mods). Off on the game test server (tests call the spawner directly).

**Visible meter (owner):** with the game rule `bubbas_reworked_mobs:show_meters` on (default off), the startle meter shows as a red 20-mark bar above the creeper (`meter/Meters`, shared with the drowned's grip bars; synced only while the rule is on and the value changes). See `specs/drowned.md`.

**Cost.**
- Per creeper: one 24-block entity box query every 4 ticks, one 8-block projectile query every 2 ticks (class-filtered), line-of-sight checks only for things that move.
- Cover search: at most once a second while it has no spot; the sweep's 32 rays + 2 per edge, a few block lookups per cell, and the raycast tests (mostly 2 rays per reject).
- Creeping closer: a try every 5-8 s, up to 4 path searches and 7 rays each. Flee and leave searches run once when those start.
- Leaving: up to 3 rays per nearby player once a second, only while leaving.
- Pillars: one 13 x 13 column scan (a few block lookups each) per try, at most every 30 s (5 min after a build); during a job, one path per walk.
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
| Open ground | only after 3 empty searches in a row; holds / backs off under / edges back to when looked at: 12 / 10 / 16 blocks |
| Pillar site | 6-18 blocks from target, best near clamp(distance, 8, 12); every column within 6 of the creeper; lone-block bonus 3, looked-at penalty 6 |
| Pillar dig blocks | within 4 of the pillar, 2 spares; reach 1.6 (2.6 once the walk ends); pauses: take 12 ticks, place 8 ticks |
| Pillar job | gives up after 600 ticks unfrozen or 4 walks per block; next pillar 6000 ticks (5 min) after a build starts, 600 (30 s) after no site found |
| First peek spot | 3-22 blocks from target (best near min(distance, 15)); dropped outside 2-24; every cell within 5 of the creeper plus 72 random ones up to 22 blocks' walk |
| Cover sweep | 32 rays, ±75 degrees, 2 bisections; 24 edge + 16 other evaluations |
| Half-hide inset | 0.12 blocks |
| Closest peek spot | 2 blocks (`APPROACH_MIN_DISTANCE`, owner) |
| Creeping closer | not looked at 3 s; tries every 5-8 s; at least 2 closer, prefers 6 closer; 4 candidates; 6 route points |
| Approach noticed | 2 blocks closer in 1 s (not sneaking), within 20; sneaking 0.3 in 1 s within 18 |
| Running off | x1.4 speed (0.35 b/t); farther by 3-14 (best 7), max 23; no new approach for 10 s after |
| Leaving | day only; starts 10 s-2.5 min after daylight (2x faster if not looked at for 30 s); gone with 20 % + 5 % per second since last looked at, per second, when no player within 128 sees it |
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
11. *(Decided by the owner: approaching needs sight; see Running off.)*
12. Only players trigger running off and creeping closer's peekaboo; a watched animal never makes it flee.
13. A teleport toward it counts as an approach.
14. "Seen by a player" for vanishing is a clear line from any player's eyes, whatever they face; "looked at" for the leave timing is the peekaboo cone.
15. Leaving creepers that lose their player (beyond 24 blocks) still vanish once unseen.
16. The half-hide inset is 0.12 (was 0.05) so it is visible at a window.
17. *Pillars:* the carried block sits on top of its head at 0.55 scale, turning and tilting with the head (in screenshots it can make the creeper look bent mid-carry).
18. *Pillars:* looked at mid-build, it freezes and stares until looked away (only while the player can see it).
19. *Pillars:* one pillar per creeper per 5 minutes; with no site, it tries again after 30 s.
20. *Pillars:* its own block tag `creeper_pillar` (natural terrain full cubes) instead of `caretaker_liftable`, which holds TNT, pumpkins and melons.
21. *Pillars:* dig spots are the nearest suitable ones (often right beside the pillar, often on the player's side), each in its own column, never side by side.

## Biome skins

Look only: every creeper wears a skin from the biome it first appeared in, recoloured from vanilla's creeper. Tree biomes take their trunk's bark colours, treeless biomes their ground block's colours. No behaviour change and no new mob. Goal (owner): near-perfect camouflage against the biome's trunk or ground, with a readable face.

### Skins

| Skin id | Palette source | Biomes |
|---|---|---|
| oak | oak log | forest, flower forest, swamp |
| birch | birch log | birch forest, old growth birch forest |
| spruce | spruce log | all taigas, grove, windswept forest |
| jungle | jungle log | all jungles |
| acacia | acacia log | all savannas |
| dark_oak | dark oak log | dark forest |
| mangrove | mangrove log | mangrove swamp |
| cherry | cherry log | cherry grove |
| pale_oak | pale oak log | pale garden |
| poplar | poplar log | dappled forest |
| plains | `grass_block_top`, colormap 0.8/0.4 | plains, sunflower plains |
| meadow | `grass_block_top`, colormap 0.5/0.8 | meadow |
| sand | sand | desert, beach |
| red_sand | red sand | all badlands |
| snow | snow | snowy plains, ice spikes, snowy slopes, snowy beach, frozen peaks, jagged peaks, frozen river |
| stone | stone | stony peaks, stony shore, windswept hills, windswept gravelly hills |
| mushroom_stem | `mushroom_stem` | mushroom fields (owner: mooshroom island creepers are mushroom stems) |
| dripstone | `dripstone_block` | dripstone caves |
| sulfur | sulfur | sulfur caves |

Everywhere else (rivers, oceans, lush caves, deep dark, the Nether, the End, modded biomes) a creeper keeps the vanilla look. Snow creepers come out almost plain white (the snow texture's own colours). Charged creepers keep the normal charge overlay on top of their skin.

### How it's built

- The skin is the attachment `bubbas_reworked_mobs:creeper_skin` (an Identifier) on the creeper: a persistent Fabric data attachment, saved with the entity and synced to all clients (`AttachmentSyncPredicate.all()`). The value `bubbas_reworked_mobs:vanilla` means the vanilla look. A creeper keeps its skin for life, even in another biome.
- Picked in `ServerEntityEvents.ENTITY_LOAD` when the creeper has no skin yet, from the biome at its block position. That covers every spawn path (natural, spawner, egg, command with or without NBT, structure, conversion), because `finalizeSpawn` always runs before the entity is added, and creepers from old saves get a skin the first time they load. No mixin on `finalizeSpawn`.
- Mapping: biome tags `data/<ns>/tags/worldgen/biome/creeper_skin/<skin>.json`. A biome in several such tags takes the first by tag id (alphabetical). Skin id = tag namespace + the path after `creeper_skin/`. Packs extend it: add biomes to `bubbas_reworked_mobs:creeper_skin/<skin>`, or add a tag `<ns>:creeper_skin/<name>` plus a texture `<ns>:textures/entity/creeper/biome/<name>.png` for a new skin.
- Client: `CreeperRendererSkinMixin` stores the texture in the render state (`CreeperRenderStateSkinMixin`) at extract time and swaps `getTextureLocation`'s result only while it is still vanilla's `creeper.png`, so another mod that changes the creeper texture wins. `CreeperPowerLayer` (charged overlay) is untouched.
- Code: `creeper/look/CreeperSkins.java` (main), `client/creeper/CreeperSkinState.java`, `client/mixin/CreeperRenderer{,State}SkinMixin.java`, one line in the `BubbasReworkedMobs` init.

### Art method (`tools/creeper-biome-textures.py`)

- Skins are vanilla's creeper recoloured pixel for pixel, no redraw (owner). Rerun the script with `--base16`/`--base32` for a new base creeper. Inputs: `--base16`, `--base32` (base creepers), `--mcsrc` (vanilla assets), `--cf32j` (CF32J zip or folder), `--sheet` (contact sheet).
- Palettes: the log side texture for tree biomes; the ground block for treeless ones (sand, red_sand, snow, stone, dripstone_block, sulfur); `grass_block_top` tinted by the grass colormap for plains (0.8/0.4) and meadow (0.5/0.8) (owner, 2026-11-25: back from dirt, since creepers there will usually find real cover); mushroom stem for mushroom fields.
- Distribution match by OKLch lightness (owner asked for a better match, 2026-11-25): each creeper colour's rank among the body's pixels (dark to light, pixel-weighted, mid-rank) picks the source colour at the same rank among the source texture's pixels. So each source colour turns up on the creeper about as often as in the source (birch mostly white with few dark marks). The earlier nearest-lightness match with a chroma tie-break over-used rare colours (a dirt test skin came out grey).
- Exact colours only (owner): every output pixel is a colour that exists in the source texture (after the grass tint for plains and meadow), except on the face (owner, 2026-11-24): the base's **black** face pixels stay black, and its **noticeably darker** face pixels are ranked among themselves by lightness, take the source colour at that rank of the source's distribution, and are blended 50 % toward black (`FACE_BLEND`), so they keep vanilla's dark-to-less-dark variety instead of one flat colour (owner, 2026-11-25). The rest of the face maps like any other pixel. The face is left out of the range statistics.
- Face pixels, found by value on the head front (UV 8..16, 8..16): black below OKLab L 0.05 (`BLACK`); noticeably darker from 0.05 to 0.5 (`DARK16`, `DARK32`; owner, 2026-11-25: the eyes' top corners and the mouth's top-left, vanilla L 0.42, count too; the next lightest face pixel is 0.58).
  - 16x vanilla: black (10,11) (13,11) (11,13) (12,13) (10..13,14), the inner bottom of each eye and the middle of the mouth; darker (9,10) (10,10) (13,10) (14,10) (9,11) (14,11) (11,12) (12,12) (10,13) (13,13) (10,15) (13,15).
  - 32x CF32J: its black eye and mouth centres stay black; 42 darker pixels round them (CF32J's eye and mouth rims, L 0.32-0.45) are blended.
- 32x (owner): the same recolour run on Classic Faithful 32x Jappa's own creeper (128x64) with CF32J's own log and ground textures (java-latest branch, commit 00321eab; the script's default input is the owner's CF32J-2026r21 zip, `--cf32j` overrides). CF32J has no sulfur texture, so the 32x sulfur skin uses vanilla sulfur's colours on CF32J's creeper. The 16x set stays vanilla creeper plus vanilla textures.
- Credit: `mods/bubbas_reworked_mobs/CREDITS.md` and `resourcepacks/bubbas_32x/CREDITS.md`. Before release, the Reworked Mobs Modrinth listing and player docs page need the CF32J credit and links too (`specs/textures.md` Credit).
- Checked in game: a birch creeper peeking from behind a birch trunk, and a sand creeper against a sand wall.

### Builder's choices (biome skins; owner to review)

1. Poplar (new in 26.3, dappled forest) is on the trunk list.
2. Plains and meadow get separate grass skins (different grass tints).
3. Mixed biomes: windswept hills -> stone (mostly grass and stone, few trees); windswept forest -> spruce; swamp -> oak; wooded badlands -> red sand; frozen river -> snow; lush caves and deep dark stay vanilla.
4. `ENTITY_LOAD` instead of a `finalizeSpawn` mixin (same result, also covers old saves and NBT summons).

## Test world station

The creeper grove (`specs/testworld.md`, Layout).

## Milestone test objectives

- **Core** (every build, `ReworkedMobsCoreGameTest`): `creeperSprintingNearbyStartlesWalkingDoesNot`.
- **Core** (`ReworkedMobsCoreGameTest`): `biomeCreepersKeepTheirSkins`: creepers spawned after `fillbiome` to birch forest, desert and nether wastes get birch, sand and vanilla, and keep them after a save/load round trip.
- **`CreeperGameTest`** (milestone):
  - `hitsFillTheMeterAndItDrains`;
  - `fullMeterHissesAndDefuses` (sneak, then backing out of range; then a hit during the hiss means sneaking no longer saves the player);
  - `blindedCreeperIgnoresMotion`;
  - `chargedCreeperStaysVanilla`;
  - `nearMissesStartle` (a snowball 1 block off counts, one 7.5 off doesn't);
  - `hidesWhenLookedAt` (behind a wall it walks to a half-hidden spot with its eyes in view; peekaboo: hidden when looked at, half out again when looked away);
  - `oneWatcherSpawnsBehindCoverOutOfView` (spawns out of sight, outside the view cone, 15-21 blocks away, watching; one per player; vanilla random spawns blocked). It failed once in about 12 runs early on and never again (maxTicks 200); watch it;
  - `creepsToCloserCoverWhenUnseen` (far wall spot first, over 14 blocks, then a spot 3+ blocks closer, never closer than 3);
  - `runsToFartherCoverWhenApproached` (player walks at it for 24 ticks; it notices; meter stays 0; it reaches a spot 3+ blocks farther);
  - `approachThroughSolidWallGoesUnnoticed` (stone wall between: no reaction; fails with the sight check off) and `approachThroughGlassIsNoticed`;
  - client part `pillarmock` (`PillarMockClientGameTest`, named-only, never in the full suite): staged mock-ups of the planned pillar creeper, six biomes; no feature code;
  - `neverOpenGroundWithCoverAround` (one 3-tall log 9 blocks out; never open ground for 240 ticks, and it reaches a spot);
  - `watcherLeavesOutOfSightByDay` (day, watcher with `leaveIn(0)`; seen while leaving for 20+ ticks without vanishing, then vanishes where no player can see it).
- **`CreeperPillarGameTest`** (milestone; environments `creeper_pillar`, `creeper_pillar_off`, `creeper_pillar_bump`, with game rules `mob_griefing` and `random_tick_speed` 0 so grass doesn't decay mid-test):
  - `buildsPillarOfGroundBlocksAndPeeks` (grass field, mobGriefing on, no cover: a 2-high pillar of exactly the ground's grass block state, exactly two 1-deep dents and no 2-deep hole, then it holds a peek spot beside the pillar);
  - `noPillarWithoutMobGriefing` (no block changes, never carries, open-ground stance);
  - `addsOneBlockOnALoneBlock` (a lone grass block 10.5 blocks from the player: it builds on it, adds exactly one, one dent).
- **`PillarClientGameTest`** (client part `pillar`, in the full suite): plains and desert, a creeper watching an armor stand with real AI (held still with NoAI only for each shot): `pillar-<biome>-carry`, `-second`, `-peek`, `-peek-near`, `-side`.
- **`CreeperClientGameTest`** (client part `creepers`): screenshots `creeper-peek` (from the watched armor stand: half behind a one-log tree), `creeper-peek-side`, `creeper-hidden` (player looking at it), `creeper-caught` (looked away, then straight back), `creeper-startle-bar` (the `show_meters` bar after two hits), `creeper-window` (from inside a small plank house, it peeks round the pane's frame) and `creeper-window-outside`. `creeper-caught` and `creeper-window` wait 10 ticks after it settles, since the client shows mob movement a few ticks late.
- **`BiomeCreepersClientGameTest`** (client part `biomecreepers`): every skin in a row, four per shot left to right, plus charged vanilla and charged birch, a birch creeper half behind a birch trunk and a sand creeper against sand, at 16x and 32x.
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
  - the game rule off brings vanilla spawns back;
  - it goes back to its old spot when caught walking up in the open;
  - the leave countdown runs 2x when not looked at;
  - a hissing leaving creeper doesn't vanish;
  - pillars: looked at mid-build it freezes and resumes when looked away;
  - pillars: a startle or approach mid-carry puts the block back in its dent;
  - pillars: the 5-minute cooldown;
  - pillars: death while carrying puts the block down.

## Open

- Owner to playtest the numbers (all in `CreeperTuning`) and C5's open-ground distances.
- Owner to review the 21 builder's choices above (17-21: pillars; questions in `BACKLOG.md`)
- Owner to review the 4 biome skin choices (Biome skins).
