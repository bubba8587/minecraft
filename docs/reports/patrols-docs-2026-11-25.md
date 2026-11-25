<!-- dte:B15,B17,B21,C1 -->
# Camp patrols: docs report (builder, 2026-11-25)

The spec section "Camp patrols" in `specs/monster-camps.md` is already updated to as-built (numbers, choices, tests). This report carries what the player docs need and the builder's choices for review.

## Player-facing (for the Reworked Mobs camps page in player-docs)

- **Camps send out patrols.** Every armed camp (one of its banners standing, no lit campfire in it) watches the land around it, about 320 blocks out. At night, when you're in that land, a small band of the camp's monsters may come looking for you: 2-3 from a small camp, up to 4-5 from a large one. Expect about one a night. Desert (husk) camps patrol by day too.
- **Their leader carries the camp's banner on its head.** Kill it and it always drops a map centred on its camp, with the camp marked by an X. The banner itself doesn't drop.
- **Big camps ride.** Camps with a stable (for now: large camps) often send riders: zombies on zombie horses, skeletons and strays on skeleton horses, husks on camel husks with a parched behind. Zombie and husk riders sometimes carry iron spears. Bogged always walk.
- **How to make an area safe:** break a camp's banners (or keep a campfire lit in it) and it stops patrolling its land. A Totem of Warding area (beacon or statue) keeps patrols from appearing on you there. Patrols don't come in peaceful or with the spawn_patrols, spawn_monsters or spawn_mobs gamerules off.
- Patrols don't count as the camp's roster: killing one doesn't clear the camp.
- Command for ops: `/bubbas_reworked_mobs camp patrol [kind] [mounted|foot]`.

## Builder's choices (for review)

- Rate: a check per player every 30 s at 10 %, a 5-minute cooldown per camp and one live patrol per camp. Estimate: a patrol in about 85 % of nights in a territory, about 1.05 a night on average.
- Territory 320 kept: it covers 96.8 % of camp-biome land in seed 20261116 (88.8 % of all dry land); 384 would give 99.4 %.
- The leader is an ordinary member with the banner (no leader scaling), like a pillager captain.
- The leader's first target is where the player stood when the patrol appeared (so it comes your way), then points 32-64 blocks off within the territory.
- Spears only for zombie and husk riders (zombies have vanilla's spear AI; skeletons don't, so skeleton and stray riders keep bows).
- In a mounted patrol the leader always rides, each follower half the time; the husk camel's parched counts toward the patrol size.
- The map is centred exactly on the camp (vanilla maps snap to a grid; an invoker builds exact-centred map data), named like the cartographer's Monster Camp Explorer Map, white X.
- Vanilla's `spawn_patrols` gamerule turns our patrols off too.
- Spawn spot: block light at most 8 (vanilla patrol rule), not warded, no lit fire within the undead fire-ward reach; an unseen spot is preferred, a seen one used if none.
- Patrol mounts despawn like their riders (skeleton horses otherwise stay forever).
- `campHasStable` is a stand-in (large camps) until `CampSite.hasStable()` lands with stables.
- Patrol state (live patrol, cooldown) is in memory, not saved: a restart lets each camp patrol again at once.

## Tests

- Core: `ReworkedMobsCoreGameTest.campPatrolSpawnsWithABannerLeader`.
- Milestone `CampPatrolsGameTest`: disarmed/quiet camps, outside territory and ward, leader map, members stay together, mounted riders. All pass.
- Client part `patrols` (10 screenshots, looked at) and `campsurvey` (coverage).
