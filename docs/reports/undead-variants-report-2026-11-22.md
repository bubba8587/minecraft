# Undead variants: builder report (2026-11-22)

Branch `camps` (worktree `../mc-camps`). Task: specs/undead.md "Undead variants". For the orchestrator to merge into `specs/undead.md`, `specs/graveyards.md`, `specs/drowned.md` (cross-link) and the player docs.

## Player-facing changes

- **Husk sleepers** (new): lie buried in sand. When woken, the sand over and round them bursts in a cloud of sand and dust; any survival player within 4 blocks is **blinded for 2 seconds**. Woken husks don't burn by day and their bare-handed hits give Hunger (as vanilla husks).
- **Stray sleepers** (new): lie frozen in ice (clear ice in crypt niches, so you can see them inside). When something wakes one, the ice **cracks first** (a glassy crack sound and ice shards: the warning) and the stray breaks free **1.5 seconds later**, shattering the ice round it. Woken strays shoot Slowness arrows (as vanilla strays) and wear the stray's ragged clothes.
- New creative items: **Sleeping Husk** and **Sleeping Stray** (each sleeper item now has an icon of the undead's head with its eyes shut; the skeleton and zombie icons were flat colour squares before).
- **Desert and snowy graveyards**: crypt yards take their climate from the biome. Desert (desert, badlands) yards have sand grave plots, husks in the crypts (mostly husks, some skeletons) buried under sand in the wall niches, and resting husks in the yard graves. Snowy yards (base temperature under 0.15) have snow plots, mostly strays (some zombies) frozen in ice niches, and resting strays in the graves. Everywhere else is as before.
- **Bogged lurk** (they are not sleepers): a bogged notices a player within 32 blocks and slips to a hiding spot near them: water 1-2 deep over a floor (2 deep hides it fully), or mud beside reeds (sugar cane, tall grass, ferns). There it crouches, still, and holds its fire. While nobody looks toward it, it now and then moves to a new spot (about every 30 s, every few seconds once you've moved more than 12 blocks off, so it follows). It **ambushes** when a player within 10 blocks turns their back on it (it's more than 100 degrees from where they look) or anyone comes within 4 blocks, or when it's hit; then it fights normally (poison arrows) and goes back to hiding 10 s after losing its target. Bogged from trial spawners stay vanilla.

## Numbers (builder's starting values, for the owner to playtest)

| What | Value | Where |
|---|---|---|
| Husk burst blinds within | 4 blocks | `UndeadTuning.HUSK_BLIND_RADIUS` |
| Blindness | 40 ticks (2 s) | `UndeadTuning.HUSK_BLIND_TICKS` |
| Stray crack warning | 30 ticks (1.5 s) | `UndeadTuning.STRAY_CRACK_TICKS` |
| Desert crypt sleepers | 70% husk, 30% skeleton | `YardKinds.Climate.crypt` |
| Snowy crypt sleepers | 70% stray, 30% zombie | same |
| Bogged notice range | 32 blocks | `BoggedTuning.NOTICE_RANGE` |
| Hiding spot distance from player | 5-14 blocks (best 8), within 24 of the bogged, ±6 in height | `BoggedTuning.SPOT_*` |
| Ambush: back turned within / too close | 10 / 4 blocks; "back turned" = more than 100° off their view | `BoggedTuning.AMBUSH_RANGE`, `TOO_CLOSE`, `BACK_TURNED_HALF_ANGLE` |
| Moves while unseen (nobody within 70° looking) | 1 in 600 a tick (~30 s), 1 in 80 (~4 s) once the player is 12+ blocks from its spot | `BoggedTuning.MOVE_ONE_IN*` |
| Back to hiding after a fight | 10 s without a target | `BoggedTuning.HUNT_LINGER` |

## Builder's choices (owner may want to weigh in)

- **Resting dead** in desert/snowy yard graves are resting husks/strays (the owner said resting dead are "the skeleton kind"; I read husk/stray as the biome's version of it). Easy to flip to skeletons everywhere: `YardKinds.Climate.resting()`.
- Snowy crypts mix in **zombies**, desert crypts mix in **skeletons**, so each crypt still has both melee and archers.
- Stray niches use **clear ice** (not packed ice) so players see the stray frozen inside; snow-yard grave plots are snow blocks. Ice can melt next to strong light, but crypts are candle-lit (owner: not too well lit), so it stays.
- The bogged hides **fully underwater** in 2-deep water (you can just make out its shape). If the owner wants its head showing, the spot rule changes to 1-deep water.
- Bogged's lurk mind is not saved: after a reload it finds a spot again.
- Cleared what the husk/stray was in: the block over the sleeper (and two up), its four sides, and the four sides of the block over it (so a lying one's foot end is freed). Only sand-like (sand, red sand, sandstone slab, suspicious sand) for husks and ice-like (ice, packed/blue ice, snow block, powder snow) for strays.

## Spec facts (specs/undead.md)

- `UndeadVariant` has HUSK and STRAY (block, item, woken entity each). `isSkeleton()` = SKELETON or STRAY (bows, robbing weapon choice in crypt gear).
- `WokenHusk extends WokenZombie` (no sun sensitivity, husk sounds, Hunger on bare-handed hit), `WokenStray extends WokenSkeleton` (stray sounds, Slowness arrows). Client: `WokenZombieRenderer(context, texture)` and `WokenSkeletonRenderer.stray(context)` (stray body/armor layers + `SkeletonClothingLayer`); `SleeperRenderer` has HUSK/STRAY posed models.
- Tags: woken husk/stray are in `#woken_undead`, `#grabs_loot`, `#zombies`/`#skeletons`; `#burn_in_daylight` now lists skeleton, zombie, stray explicitly (not the whole woken tag), so husks don't burn.
- Breakout: `undead/sleeper/SleeperBreakout` (`breakOut` runs in `SleeperWaking.wake` before the mob is added; `crack` runs in `SleeperBlockEntity.scheduleWake` for strays and raises the delay to at least `STRAY_CRACK_TICKS`).
- Graveyards: `YardKinds.Climate` (TEMPERATE/DESERT/SNOWY) from the biome at the yard's centre in `CryptYardStructure`; saved in the piece tag as `Climate`; `CryptYardPiece.climate()`. `Build.encase` puts sand/ice over a husk/stray crypt niche; `Build.resting(…, variant, …)` replaces `restingSkeleton`.
- Bogged: `bogged/BoggedLurk` (state machine IDLE → TRAVEL → LURK → HUNT, one mind per bogged in a WeakHashMap), `bogged/BoggedTuning`, `mixin/BoggedLurkMixin` (Mob.customServerAiStep HEAD, instanceof Bogged). `MobFinalizeSpawnMixin` tags spawner bogged `bubbas_reworked_mobs.vanilla_bogged` so they stay vanilla. `BoggedLurk.hideAt(level, x, z, aroundY)` scans down from 6 above the player's feet (not the heightmap, which mangrove canopies would stop).

## Tests

- Milestone server tests `UndeadVariantsGameTest`: `huskBurstsOutOfSandAndBlinds`, `strayCracksItsIceBeforeBreakingFree` (still frozen 20 ticks in, free by 40), `huskAndStrayTags`, `boggedLurksWhileWatchedAndAmbushesAnAvertedBack`. `GravesGameTest` yard core test now expects the yard climate's resting kind. Full milestone server suite: 114/114 pass.
- Client part `variants` (`VariantsClientGameTest`), screenshots looked at: `variants-sleepers` (husk and stray sleepers sitting and lying, correct textures), `variants-buried` (sand over a husk, clear ice over a stray), `variants-husk-burst` (sand cloud, husk rising out of its trench), `variants-stray-crack` (ice shards, still frozen), `variants-stray-free` (ice shattered, stray standing), `variants-woken` (woken husk and stray, stray's ragged clothes showing), `variants-bogged` (one bogged crouched under a reedy pool's surface, one standing on the bank).
- Not shot: a naturally generated desert/snowy yard (covered by the server tests and the climate code; worth a look in the next test world tour).

## Art

- `tools-local/make_sleeper_icons.py`: four sleeper item icons at 16x and 32x; row added to `docs/art-todo.md`.
