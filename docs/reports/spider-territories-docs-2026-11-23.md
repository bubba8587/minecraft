# Spider territories: docs report (2026-11-23)

Builder report for `specs/spiders.md` (new section "Territories") and the player page `player-docs/Projects/Bubba's Reworked Mobs/Spiders.md`. Governing leaf: C1.

## Owner's request (2026-11-23)

"spiders should look to create 4 more subsidiary webs within the range of their home web. then, other spiders don't create webs anywhere within that range (you can probably establish a simple chunk grid, one spider gets its home web chunk plus 3 more chunks in a 2x2 square? keep it simple). other smaller spiders spawned in that chunkset can hang out in the subsidiary webs. really want these giant spiders to be able to take over a chunk of forests."

## Player-facing behaviour (for the player page)

- A giant spider's first web is its **home web**, and it claims the forest round it as its **territory**: a 2x2 square of chunks (32x32 blocks) with the home web near the middle.
- It then spins up to **4 more webs** inside its territory, spread out between the trees, so it ends up with 5 webs in its patch of forest. While it still has webs to spin it roams the whole territory; once all 5 are up it stays round its home web as before.
- **No other giant spins webs in a claimed territory.** A giant that can't find free land keeps roaming and spinning only on unclaimed ground, and claims a territory as soon as it spins a web somewhere free.
- If its webs are cut, it spins replacements (up to 5 again). If its home web is cut, its oldest remaining web becomes its home.
- **Smaller spiders settle in the subsidiary webs:** spiders, cave spiders and the giant's little spiders that are inside a territory go to one of its subsidiary webs (not the home web), at most 2 per web, and hang around under it. Every so often one climbs up and **rests in the middle of the web**, then lets itself back down. Hitting it (or it spotting a target) makes it drop out and fight.
- When the giant dies, its territory is freed and another giant can take the forest.

## How it works (spec facts)

- **Claim:** `spider/SpiderTerritories`. When a giant without a territory spins a web, the web's chunk plus 3 more form the square: the quarter of the chunk the hub sits in picks the direction (hub in the low-x half of its chunk: the square extends to the chunk at x-1, else x+1; same for z), so the web is at least 8 blocks from every edge. If that square overlaps another giant's claim, the other three 2x2 squares holding the web's chunk are tried; if all overlap, no territory.
- **Retry:** a giant with webs but no territory tries to claim round its newest web every 200 ticks (`TERRITORY_CLAIM_RETRY_TICKS`), and again each time it spins a new web. This also covers giants from saves made before territories.
- **Spinning with a territory:** `GiantSpider.spin` (the existing trunk-pair search) only accepts pairs whose hub is inside its own territory and at least 6 blocks sideways from its other territory webs (`TERRITORY_WEB_SPACING`). It stops at 5 webs (`TERRITORY_WEBS`). The old area cap (5 webs within 12) applies only to giants without a territory. While it has fewer than 5 webs it spins every 200-400 ticks (`TERRITORY_SPIN_INTERVAL_MIN/MAX`) instead of 400-800.
- **Exclusive:** a hub inside anyone else's territory is refused (`SpiderTerritories.mayWeb`). A giant without a territory spins only on unclaimed chunks.
- **Wandering:** with a territory and fewer than 5 webs (or no standing home web), its `WanderGoal` picks standing spots anywhere in the territory's box (y from 16 below to 3 above its home web). With all 5, it wanders round its home web as before. Without a territory it wanders round itself (`ROAM_RANGE`), so a giant boxed in by claims drifts off instead of circling a web it can't claim from. (Changed: before, its newest web was always its home.)
- **Home web:** the territory's web list is saved home first; webs that are gone are dropped from it (only when their chunk's entities are loaded, so unloaded webs still count). `GiantSpider.rememberWeb` never forgets the home web when trimming to `OWN_WEBS_MAX`.
- **Saved:** a Fabric persistent level attachment `bubbas_reworked_mobs:spider_territories` (list of owner UUID, square's min chunk x/z, webs as UUID + hub pos). In memory: a map by owner and a `Long2ObjectMap` by packed chunk, so every lookup is a hash lookup.
- **Release:** (builder's rule) when the giant is removed for good (death, despawn, `/kill`, discard: Fabric `ENTITY_UNLOAD` with a removal reason that `shouldDestroy()`, as `CampSites` does for camp mobs). Also a stale check: every 200 ticks (`TERRITORY_CHECK_TICKS`), a territory whose 4 chunks all have their entities loaded but whose giant isn't in the level (`Level.getEntity(UUID)`) for 6000 ticks (5 min, `TERRITORY_STALE_TICKS`) is released. Unloading with the chunk does not release it. A giant that comes back to a released territory re-claims on its next retry if the land is still free.
- **Settling small spiders:** `spider/WebSettleGoal`, added to every vanilla `Spider` (so cave spiders and the giant's little spiders too) by `mixin/SpiderSettleMixin` at the end of `Spider.registerGoals`, priority 5, flags MOVE and JUMP. Skipped for `GiantSpider` and `NestSpider` (nest sleepers pose themselves).
  - It checks once every 100 ticks (`SETTLE_CHECK_TICKS`), and only with no target: the chunk it stands in (`SpiderTerritories.at`), then that territory's subsidiary webs (entity lookups by UUID), nearest first, with fewer than 2 settlers (`SETTLERS_PER_WEB`; counted with one small entity box query round the web).
  - Settled: tag `bubbas_reworked_mobs.web_settler`, vanilla home restriction `setHomeTo(web hub, 8)` (`SETTLE_HOME_RADIUS`), so its vanilla stroll keeps it near. It walks to a standing spot under the hub (the giant's `standingSpotIn`, now static and shared). If it has strayed more than 4 blocks it walks back.
  - Resting: 50 % per check (`REST_CHANCE`): at the spot under the hub it climbs straight up at 0.1 blocks a tick (`REST_CLIMB_SPEED`) to the hub, rests there 300-700 ticks (`REST_TICKS_MIN/MAX`), then climbs straight back down. Only if its body fits at the hub and no other spider is there. While up (tag `bubbas_reworked_mobs.web_resting`) it has no gravity and Stormie's Spiders' `onTravel` steps aside (`StormieDeactivatedSpiderMixin`, same hook as the Fabricator's limp spiders). A spider saved while resting gets its gravity back on load. A target ends the rest at once and it drops.
  - Unsettles (home cleared, tag removed) when its web is gone or its territory is released.
- **Hand-rolled (why):** the claim map (no vanilla per-chunk ownership registry; POI is block-based and the owner asked for no new blocks), and the straight climb up to the web (vanilla and Stormie's have no rope climb; Stormie's navigator can't path to a point in mid-air or on a wall, spec "Wander"). The rest uses vanilla's home restriction and stroll.
- **No world scanning added:** chunk-keyed lookups; the giant's existing trunk search is unchanged; the settle check is a hash lookup plus at most 5 entity lookups and one small box query per spider every 5 s.

## Numbers (SpiderTuning)

| What | Value |
|---|---|
| Territory | 2x2 chunks; home web + 4 subsidiary (`TERRITORY_WEBS` 5); webs at least 6 apart sideways |
| Spinning while building | every 200-400 ticks (normal 400-800) |
| Claim retry | every 200 ticks |
| Release | on death/removal; or giant missing 6000 ticks with all 4 chunks loaded (checked every 200) |
| Settlers | checked every 100 ticks; 2 per subsidiary web; home radius 8 round the hub; walk gives up after 300 ticks |
| Resting | 50 % per check; 300-700 ticks; climb 0.1 blocks a tick |

## Builder's choices (marked for the owner)

- (builder) Territory released on death/removal, plus the 5-minute stale rule above.
- (builder) A giant with no free territory keeps roaming (round itself, not its web) and spinning only on unclaimed land; it claims as soon as one of its webs is on free ground.
- (builder) Home web cut: the oldest remaining territory web becomes home; the giant spins replacements up to 5.
- (builder) Settlers use subsidiary webs only, not the home web; 2 per web.
- (builder) Resting spiders sit upright in the middle of the web (Stormie's doesn't orient them in mid-air). Looks like the spider sits on the strands; checked in screenshots.
- (builder) Giant spawning is unchanged: a giant can still spawn inside a claimed territory (the 48-block spacing from other giants still applies). It then has to find free land for its own webs. Could be tightened to "no giant spawns in a claimed territory" if the owner wants.
- (builder) The `webs` client scenes release the giant's territory after each spin so the approved screenshots keep their three close-set webs.

## Tests

- Core: `ReworkedMobsCoreGameTest.giantClaimsATerritoryOthersCantWebIn` (body `SpiderTerritoryGameTest.claimsATerritoryOthersCantWebIn`): the home web's chunk and 3 neighbours (4 chunks) are claimed, the home web is first; a second giant spins 0 strands there and has no territory.
- Milestone `SpiderTerritoryGameTest`:
  - `homeWebAndFourSubsidiaryWebs`: home web, then a subsidiary on the other trunk row (both inside the territory); with 5 webs it spins nothing though a free trunk pair is in reach; after one goes it spins the 5th; 4 subsidiary webs.
  - `aSpiderSettlesByASubsidiaryWeb`: a vanilla spider in the territory takes the subsidiary web's hub as its home (not the home web's) and walks to within 4 blocks of it.
  - `deathReleasesTheTerritory`: killed giant's claim is gone once it's removed; another giant claims there.
  - `savedAcrossAReload`: the attachment encodes and decodes with owner, square and webs; after dropping the in-memory claims, they're read back from the attachment with the chunk lookup intact.
- Each territory test runs in its own test environment (`spider_territory_claim/webs/settle/death/save`, empty like `spiders`): environments run as separate batches, so no neighbouring test's giant sits in its 2x2 chunks. Other giant tests (`GiantSpiderGameTest`) use `spinFree`, which releases the claim right after spinning, for the same reason.
- New test structure `territory_field` (20x16x20, stone floor, open).
- Client part `territories` (`TerritoriesClientGameTest`): a spruce grove, a giant with home + 4 subsidiary webs, a vanilla spider that settles and rests in a subsidiary web. Shots: territory-overview, territory-home-web, territory-spider-resting, territory-spider-from-below, territory-spider-resting-32x.
- Milestone objectives without a test yet:
  - A territory whose giant is missing 5 minutes with its chunks loaded is released.
  - A resting spider that's hit drops and fights; one saved while resting has gravity after load.
  - A giant blocked by claims on all sides spins only on free land and claims there.
  - Nest sleepers and Fabricator spiders never settle.

## Open (for the owner)

- Should giants be kept from spawning inside another giant's territory?
- Numbers to playtest: 2 settlers per web, rest chance and length, the 6-block web spacing.
