# Trader posts: docs report (2026-11-24)

Builder's report for the docs merge: a new section for `specs/monster-camps.md` (or its own spec if the orchestrator prefers),
a player-docs entry for Bubba's Reworked Mobs, and a natural-world sign. Governing leaf: C1 (Reworked Mobs' direction);
no new leaf. B15: no new blocks (all vanilla blocks; wool stairs from 26.3).

## Home

Bubba's Reworked Mobs, next to camps, crypt yards and Nether outposts: it shares their code-built structure machinery
(`worldgen/ClippedBuild`, the chunk-clipped build and fence pass, the footprint-plan idea, the locate-by-placement-grid
search) and it is about a mob (the wandering trader). No other project fits better: World Tweaks has no worldgen
structure code, and a separate mod would duplicate all of that.

## Prior art

`tools/prior-art.py` ("wandering trader post structure", "wandering trader camp", "trader camp", "wandering trader spawn"):
no mod adds wandering-trader posts. Ocean/Desert Villager Trader add their own traders with structures (a different idea);
"Wanderful Trades" adds trades; the rest change spawn rates or block traders. Nothing to reuse; built on vanilla's
`WanderingTraderSpawner` (one injection) and vanilla blocks.

## Player-facing (player docs: Reworked Mobs, new "Trader posts" entry)

- **Trader posts** appear now and then on open plains, sunflower plains, meadows and savannas, and on the edges of
  deserts: a little market square on levelled ground with a dirt path out the front.
- A **striped wool stall** (blue, red and white; three colour schemes) with a gabled roof and an overhang over a counter
  of logs and barrels dressed with a flower pot, cloth, candles and a lantern; a rug and a lantern inside; the trader's
  blue-and-red banners (gold diamond, zigzag hem) on the posts, the back wall and either side of the entrance.
- **One or two llama pens** of spruce fence with a gate and lanterns, hay bales and a water trough. A post with one pen
  has a pile of hay and barrels on the other side.
- A **hitching rail** with a water trough and hay, a **signpost** ("Trader Post / All welcome") with a lantern, a lit
  **campfire** with two log benches, and often an **open cart** on dark wheels with cargo.
- Two barrels hold a little: wheat, dried kelp, bread, string, dyes, paper, now and then a lead or one emerald (stall);
  wheat, kelp, carrots, potatoes, maybe a hay bale or a lead (cart). Nothing that undercuts trading.
- **The wandering trader comes to the post.** Vanilla's timing and odds are unchanged (a check every 20 minutes, 25-75 %
  chance, 1 in 10 per attempt). When it would spawn near a player and a trader post is within 96 blocks of that player
  (and no village bell is within 48, which vanilla already uses), it arrives **behind the post's counter** instead, and
  its two llamas arrive **tied up in the pens** (one in each, or both in the only pen). It stays round the post (within
  12 blocks) for vanilla's two days, then leaves with its llamas. Between visits the post stands empty and intact.
- **Safe ground:** no monsters spawn naturally inside a post, it is lit, and endermen leave structures alone.
- Commands (operators): `/bubbas_reworked_mobs trader_post build [seed]`, `trader_post locate`, `trader_post arrive`
  (bring a trader to the nearest post within 96 blocks now). `/locate structure bubbas_reworked_mobs:trader_post` works.

## Spec section (for `specs/monster-camps.md`, "Trader posts")

### Placement

- Structure `bubbas_reworked_mobs:trader_post` (step `top_layer_modification`, like camps), set `trader_posts`
  (random spread, spacing 18, separation 6, salt 20261124; exclusion zone: 3 chunks from villages).
- Biomes `#bubbas_reworked_mobs:has_structure/trader_post`: plains, sunflower plains, meadow, savanna, savanna plateau,
  desert. A desert post needs a non-desert biome 48 blocks out in one of the four directions (desert edges only).
- `TraderPostStructure`: nine samples (centre, corners and edge middles 10 out) must be dry and in the tag; ground may
  rise or fall at most 4 (6 was tried: posts cut deep into hillsides); the floor is the mean of the nine sample heights, at or above sea level. Kept 2 chunks clear of camp and crypt-yard placement-grid
  candidates (checked from their sets' `getPotentialStructureChunk`, before their own terrain checks, so conservative).
- Density in seed 20261116: see "Numbers". Rejections there are mostly biome (13 of ~100 grid candidates near spawn were in post biomes); of those in biome, about a third pass the terrain checks.

### Layout (`TraderPostPiece`)

- Local frame round the centre: u across, w toward the front. Seed picks front (4 ways), mirror, colours
  (primary blue or red; stripes primary/white/secondary/white, primary/secondary, or light blue/white/red/white), pens
  (one on the left, 60 % also one on the right), cart (60 %).
- Plaza u -9..9, w -8..10, levelled to the post's floor height (rocky columns topped with coarse dirt); 4-block skirt slopes a block per block back to the land; trees
  and plants cleared (14 up over the plaza). Soil plaza: coarse dirt patches (busier near the stall and fire), dirt path
  out the front (w 4 to 14) and in front of the counter; sand plazas stay sand. Pen floors coarse dirt and grass.
- Footprints (local, inclusive): stall u -3..3 w -7..-3; fire u -2..2 w 1..3; pens u ±5..±9 w -3..3; cargo pile (one
  pen) u 5..7 w -2..0; rail u 4..8 w 6..7; signpost u 2 w 6..8; cart u -8..-4 w 5..8; entrance banners u ±2 w 10. All
  keep a gap of at least 1 and stay inside the plaza (milestone test over 300 seeds).
- Stall: spruce fence posts at the corners (3 high); roof: back eave wool stairs (y+4, w -7), ridge wool (y+5, w -6/-5),
  front eave stairs (y+4, w -4), overhang stairs (y+3, w -3), wool gable ends; striped by u. Back wall wool (y+2..3) over
  barrels (one with `chests/trader_post`), a hay bale. Counter (w -4): barrel, three stripped spruce logs, barrel; on it a
  potted blue orchid, two carpets, three lit dyed candles, a lantern. Rug of carpets (w -6), hanging lantern under the
  ridge, wall banners on the front posts and back wall.
- Pens: spruce fence ring 5 x 7, gate toward the plaza with lanterns on the fences either side, hay (stack of two plus
  one) in a back corner, two water cauldrons in the other.
- Rail: two 2-high posts, a fence top bar, two water cauldrons and a hay bale in front. Signpost: stripped log 2 high,
  fence, lantern, waxed spruce wall signs front and back (translatable text with English fallback). Cart: spruce top slabs
  4 x 2, dark oak trapdoor wheels (open, on the sides), barrel (`chests/trader_post_cart`), hay, barrel, carpet; fence shaft.
- Trader banner: base colour, yellow rhombus, zigzag hem (triangles bottom) in the other colour; red banners also a
  zigzag top.
- Blocks with entities are only painted when the chunk being built holds them (worldgen block entities have no level:
  sign text is loaded as NBT, not `setText`).

### The trader's arrival (`TraderPosts`, `WanderingTraderSpawnerMixin`, `TraderLlamaTetherMixin`)

- One injection in `WanderingTraderSpawner.spawn`, at its meeting-point lookup, i.e. after vanilla picked a player and
  passed its 1-in-10 roll. `TraderPosts.redirect`: a village meeting point within 48 of the player wins (vanilla);
  else the nearest post whose centre is within 96 (found from the set's placement grid and structure starts, a handful
  of chunk lookups, only on a successful roll). `arrive` needs the post's trader spot and llama spots loaded and clear,
  and the biome not in `#without_wandering_trader_spawns`; otherwise vanilla places the trader as usual.
- Trader at the spot behind the counter (u 0, w -5): despawn delay 48000 (vanilla), wander target and home the post
  centre, home radius 12. Llamas: two, `TRADER_LLAMA.spawn(EVENT)`, one per pen (or both in the one pen), leashed to a
  lead knot on the pen's back fence; tagged `bubbas_reworked_mobs.trader_post` and `bubbas_reworked_mobs.tether:<fence
  pos>`, despawn delay 47999.
- Vanilla keeps any trader llama leashed to something other than its trader forever. `TraderLlamaTetherMixin` makes the
  post's own tether count like the trader's lead, so a tethered llama counts down and leaves when its stay is up (and
  its knot goes with it). A llama a player leads away (or ties up elsewhere) follows vanilla's rule again.
- Spawning hand-rolled? Only the choice of place; the odds, timing, trader, llamas and despawning are vanilla's.
- No griefing: `spawn_overrides` monster category, `piece` box, empty list (vanilla's mechanism): no natural monster
  spawns inside. Light from lanterns, candles and the fire. Endermen already leave structure pieces alone
  (`EndermanTuning.STRUCTURE_MARGIN`).

### Code

`traderpost/`: `TraderPosts` (registration, keys, `nearest`, `redirect`, `redirectTo`, `arrive`, `tetheredAtPost`),
`TraderPostStructure`, `TraderPostPiece` (plan, footprints, spots: `traderSpot`, `llamaSpots`, `tetherPosts`, `gates`,
`gateFronts`, `entrance`, `penInsides`, `plaza`, `at(u, dy, w)`), `TraderPostCommand`. Mixins `WanderingTraderSpawnerMixin`,
`TraderLlamaTetherMixin`. Data: `worldgen/structure/trader_post.json`, `worldgen/structure_set/trader_posts.json`,
`tags/worldgen/biome/has_structure/trader_post.json`, `loot_table/chests/trader_post.json`, `trader_post_cart.json`.
Lang: `sign.bubbas_reworked_mobs.trader_post.line1/line2`.

### Milestone test objectives

- `TraderPostGameTest` (milestone): `traderPostBiomes` (in: plains, sunflower plains, meadow, savanna, savanna plateau,
  desert; out: forest, dark forest, taiga, jungle, swamp, river, ocean, beach, snowy plains, badlands, mushroom fields,
  windswept hills); `postPiecesNeverOverlap` (300 seeds, gap 1, inside the plaza); `everyPostBlockIsSupportedAndFencesEndCleanly`
  (four seeds: one or two pens, with and without a cart); `postCanBeWalkedThrough` (a trader from the entrance reaches
  the counter and each pen gate); `tetheredLlamasLeaveWhenTheirStayIsUp` (post llamas and their knots go; a trader llama
  tied up elsewhere stays, vanilla); `vanillaPlacementWithoutANearbyPost` (no natural post: vanilla; a post over 96
  blocks off: vanilla; a village bell within 48: vanilla).
- Core: `ReworkedMobsCoreGameTest.traderArrivesAtAPostWithLlamasPenned` (body `TraderPostGameTest.traderArrivesWithLlamasPenned`):
  with two pens and one, the trader arrives behind the counter with vanilla's stay, two llamas penned (one per pen when
  two), tethered to the pen fence and counted as tethered at the post.
- Client part `traderposts` (`TraderPostsClientGameTest`): natural posts in seed 20261116, each checked for biome, flat
  dry floor, sound blocks, clean fences, no monster spawns, a trader's walk to the counter and gates; shots
  `traderpost-<i>-above/-front/-left/-backright`, `traderpost-0-counter/-pen`; the daily-spawn redirect from a player 30
  blocks off (`traderpost-arrival-front/-pen/-above`), a night shot (`traderpost-night`); prints the density.
- Not covered by a test: the injection point itself firing inside vanilla's daily tick (the test calls the same
  `TraderPosts.redirect` the mixin calls; the mixin would fail to apply at startup if its target moved).

### Natural world

`tools/build-naturalworld.py`: new sign "Trader post (wandering trader)" from `trader_post locate` (lands on the plaza
centre, open ground between the stall and the fire). Not run from the worktree; the orchestrator rebuilds the world.
Add the line to `specs/testworld.md` "Natural world" list.

## Numbers for the owner to playtest

- Spacing 18 chunks, separation 6 (a candidate every 288 blocks). In seed 20261116 that gave about 0.08 posts per km² near spawn (a forest-heavy area), typically 1.5-2.5 km between posts; open plains/savanna regions get more. If the owner wants them commoner: lower the spacing, or allow MAX_SLOPE 5.
- Draw range 96 blocks from the player; village bells within 48 still win; trader stays near the post (12 blocks).
- Loot: stall barrel 3-5 stacks, emerald weight 4 of 93 (one at most per pick); cart barrel 2-4 stacks of food.

## Builder's calls to review

- **Tethered, not free in the pens:** the owner asked for llamas leashed in the pens. Vanilla never despawns a trader
  llama tied to a fence, so a small mixin lets the post's own tether count as the trader's lead; otherwise penned llamas
  would pile up after every visit.
- Village bells win over posts (vanilla already sends traders to a bell within 48 of the player).
- The trader is not tied to the counter: it wanders round the post (home radius 12) like vanilla traders round a bell.
- No displayed goods on the counter (owner, 2026-11-24: players could just take them); the counter is dressed with blocks.
- Posts stay 2 chunks clear of where camps and crypt yards could start (a camp's skeletons next to the trader would
  spoil it); villages via the set's exclusion zone (3 chunks).
- Lit campfire (unlike dark monster camps): it is a person's camp, and the light keeps the night quiet.

## Art

No new textures or models (all vanilla blocks): nothing for `docs/art-todo.md`.

## CLAUDE.md

Add `traderposts` to the Reworked Mobs client test part names in "Speed of the build and test loop".
