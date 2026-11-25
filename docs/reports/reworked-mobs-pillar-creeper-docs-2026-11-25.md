# Reworked Mobs: pillar creepers (docs report, 2026-11-25)

Builder's facts for `specs/creepers.md` and the Reworked Mobs player docs page. Owner's design (all decided) as given in the task; builder's choices marked.

## Player-facing

- A watching creeper that finds no cover at all (the case that used to go straight to the open-ground stance) now makes its own: it digs up two surface blocks near it, one at a time, carries each balanced on its head, and stacks them into a 2-high pillar, then hides half behind it and peeks like it does behind a tree (peekaboo, creeping closer, running off all work as usual).
- Only with vanilla's `mobGriefing` game rule on (as endermen). Off: the old open-ground stance, nothing changes.
- Blocks move exactly as they are, no drops: a grass block stays a grass block (the lower one of a grass pillar may later turn to dirt the vanilla way, since a block sits on it).
- It never digs a hole deeper than 1: each block is the top block of its own column, on level ground, never two side by side.
- If a lone natural block already sticks up out of level ground at a good spot, it uses that and adds only one block on top.
- Looked at while building out in the open, it freezes and stares until you look away, then carries on. Walking at it makes it run off as usual; whatever it carries goes back into the dent it came from.
- One pillar per creeper every 5 minutes; later it just uses the pillar it built like any other cover.

## Spec facts (for "How it's built", new subsection "Making its own cover")

- Code: `creeper/CreeperPillar.java` (the job), run by `CreeperWatchGoal`; numbers in `CreeperTuning` (PILLAR_*).
- Trigger: in WATCH, after 3 empty cover searches in a row (`OPEN_GROUND_AFTER_FAILED_SEARCHES`), `CreeperPillar.tryStart`: needs `mobGriefing`, not leaving (day), no block carried, and game time past `CreeperMind.nextPillarTime`. A plan sets that to now + 6000 ticks (5 min); no site found: now + 600 (30 s), and the open-ground stance as before. Not saved (watchers aren't persistent).
- Site search (block lookups only, once per try): every column within 6 blocks of the creeper (13 x 13), 6-18 blocks from the target, scored |distance - clamp(current distance, 8, 12)| + 0.3 x walk; -3 for a lone block it can build on; +6 when the watched player's look cone (45 degrees, `Gaze`) covers the site. A site needs:
  - fresh: an empty standable cell with a sturdy top under it (fills that cell and the one above); or a lone block: a `creeper_pillar` full cube with at least 3 of 4 sides open and a sturdy block under it (fills the cell on top);
  - a stand cell 1.4 blocks behind the pillar (away from the target) and the cells either side of it standable (its peek cells);
  - the view from the target's eyes to that stand point open now (else there was cover already);
  - enough dig blocks (2 or 1, plus 2 spares).
- Dig blocks (`sources`): columns within 4 blocks of the pillar, nearest first, not the pillar's or stand cells' columns, never next to another chosen one. The top block of the column (within 1 up / 2 down of the floor), in tag `#bubbas_reworked_mobs:creeper_pillar` (data: grass, dirt kinds, mycelium, podzol, mud, packed mud, moss, sand, red sand, gravel, clay, snow block, `#minecraft:base_stone_overworld`, cobblestone, mossy cobblestone, cobbled deepslate, sandstone, red sandstone, calcite, dripstone block, `#minecraft:terracotta`), full collision cube, no block entity, breakable, not wither-immune, open above, sturdy below, and no neighbour column higher than it (so every dent is 1 deep). Builder's choice: a new tag instead of `caretaker_liftable`, which holds TNT, pumpkins and melons.
- The job: walk to the dig block (within 1.6, or 2.6 once the walk ends), 12-tick pause looking at it, take it (`removeBlock`, vanilla block-break particles and sound via level event 2001, `BLOCK_DESTROY` game event), walk to the stand cell, 8-tick pause looking at the target cell, place the same state (`setBlock` with full updates, the block's place sound, `BLOCK_PLACE` game event). Steps aside if it stands in the cell; waits while something else does. Gives up after 600 ticks (frozen time not counted) or 4 failed walks to one block (then the next spare). About 3-4 s for a 2-block pillar on flat ground.
- Looked at (peekaboo cone) while the target can see its eyes: navigation stops, it stands and stares; resumes when looked away (builder's choice: freeze).
- Abort (plan leaves WATCH, a spot is taken, target changes, goal stops: fuse, blindness, charged): the carried block goes back into its dent if still open, else to a free cell beside it, else drops as an item. On death the same (`ServerLivingEntityEvents.AFTER_DEATH`).
- Carried block: attachment `bubbas_reworked_mobs:creeper_carried` (BlockState, persistent, synced to all clients). A creeper loaded with one but no job sets it down beside itself.
- Look (builder's choice): `client/creeper/CreeperCarriedBlockLayer` (Fabric `LivingEntityRenderLayerRegistrationCallback`) draws the carried block at 0.55 scale on top of its head, turning and tilting with the head; render state field added by `CreeperRenderStateSkinMixin`, filled in `CreeperRendererSkinMixin`'s extract hook with the renderer context's `BlockModelResolver` (enderman's display context).
- After the pillar stands, `CreeperMind.pillar()` holds its base, and the ordinary cover search finds the peek spot behind it.
- Cost: one 13 x 13 column scan (a few block lookups each) per try, at most every 30 s (5 min after a build); during a job, one path per walk.
- Vanilla's grass rule: the lower block of a grass pillar may turn to dirt on a random tick (a block sits on it).

## Numbers (CreeperTuning)

| | Value |
|---|---|
| Site distance from target | 6-18, best near clamp(distance, 8, 12) |
| Site search | every column within 6 of the creeper |
| Lone block bonus / looked-at penalty | 3 / 6 |
| Dig radius / spares | 4 / 2 |
| Reach | 1.6 (2.6 once the walk ends) |
| Pauses | take 12 ticks, place 8 ticks |
| Job limit | 600 ticks unfrozen; 4 walks per block |
| Next pillar | 6000 ticks after a build starts; 600 after no site found |

## Milestone test objectives

- `CreeperPillarGameTest` (milestone):
  - `buildsPillarOfGroundBlocksAndPeeks`: grass field, mobGriefing on, no cover: a 2-high pillar of exactly the ground's grass block state, exactly two 1-deep dents (no 2-deep hole), then it holds a peek spot beside the pillar;
  - `noPillarWithoutMobGriefing`: mobGriefing off: no block changes, never carries, open-ground stance;
  - `addsOneBlockOnALoneBlock`: a lone grass block 10.5 blocks from the player: it builds on it, adds exactly one, one dent.
  - Environments `creeper_pillar`, `creeper_pillar_off`, `creeper_pillar_bump` (game_rules: mob_griefing, random_tick_speed 0 so grass doesn't decay mid-test).
- Client part `pillar` (`PillarClientGameTest`, in the full suite): plains and desert, a creeper watching an armor stand, real AI (held still with NoAI only for each shot): `pillar-<biome>-carry`, `-second`, `-peek`, `-peek-near`, `-side`.
- Not yet tested (objectives): looked at mid-build it freezes and resumes; a startle or approach mid-carry puts the block back in its dent; the 5-minute cooldown; death while carrying puts the block down.

## Builder's choices (owner may change)

1. Carry look: the block sits on top of its head at a bit over half size, tilting with its head.
2. Looked at mid-build: it freezes and stares until you look away (only while you can see it).
3. How often: one pillar per creeper per 5 minutes; no site: tries again after 30 s.
4. Own tag `creeper_pillar` (natural terrain full cubes) instead of `caretaker_liftable`.
5. Dig spots are the nearest suitable ones (often right beside the pillar), each in its own column, never side by side.
