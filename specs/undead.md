<!-- dte:C1,B15,B17,B18,B19,D2 -->
<!-- covers: docs/research/undead-*, docs/research/camps-*, mods/bubbas_reworked_mobs/* -->
# Bubba's Reworked Mobs, part 1: undead (spec)

Builds part 1 of the C1 reworkedMobs direction (mod id `bubbas_reworked_mobs`; later parts get their own specs). Background research: `docs/research/undead-prior-art.md` (nothing on any loader does "dormant until disturbed"; closest mechanic is vanilla 26.3's copper golem statue) and `docs/research/undead-feasibility.md` (26.3 classes, code sketches, performance budget). Everything here is fluid: change it when a better or faster way turns up.

## Dormant undead are real blocks

Dormant undead are real blocks, not entities or display-entity pseudo-blocks. They can be mined into items (see Mining and placing sleepers), cost nothing to tick in large numbers, and never count against mob caps. Waking one replaces the block with a real mob. (Moved here from the tree on the owner's word: it is a technical choice. B15 fewNewBlocks is satisfied because the owner gave the reason for these blocks.)

- Modeled on vanilla's copper golem statue: block + block entity, **no ticker**. Blockstate: `facing`, `pose`, `waterlogged`. Block entity holds the gear as the vanilla `minecraft:container` component, a lazy gear loot table (rolled on first interaction or wake), a wake cooldown and who woke it.
- Rendering, planned before release (hybrid): the body is a baked block model per pose (part of the chunk mesh, free per frame; 26.3 block models allow free per-element rotation). A block entity renderer draws only armor and held items, only when the sleeper has gear and is within 32 blocks. Pose models and renderer poses are generated from one Java pose table by datagen so they never drift apart. Armor in the renderer needs one client accessor mixin for the equipment asset manager.
- Lying sleepers are one block with an overhanging model (collision stays one block).
- First playable version (built): the block entity renderer draws the whole sleeper with the vanilla mob model and texture as placeholder art, one baked model per pose, and reuses vanilla's own `HumanoidArmorLayer` and `ItemInHandLayer` for gear (they only need a `RenderLayerParent`, so armor, trims, dye and glint come free). Poses are part rotations applied after the vanilla model's `setupAnim` (`SleeperPoses`, the pose table), so armor models pose identically. The block's own model is particle-only. The hybrid baked-body version and our own 16x/32x body textures come before release. Sleeper item icons are agent-made heads with shut eyes (`tools-local/make_sleeper_icons.py`, listed in `docs/art-todo.md`).
- Code layout: one block class, one block entity type and one renderer serve every variant; a variant is one `UndeadVariant` entry (block + item `<name>_sleeper`, entity `woken_<name>`) plus one entry in the renderer's client table.

## Hearing and waking

- The block entity implements `GameEventListener.Provider` (registered automatically for loaded full chunks, like sculk sensors), so hearing costs nothing between events. Vanilla vibration rules apply: sneaking silences steps, wool blocks, carpet muffles.
- **A disturbance meter, on the warden's model (owner: "players have to do SOMETHING to wake them ... and not trigger them from a totally different hallway", but "don't make it too easy to get by them when being reckless").** Each heard noise adds points to the sleeper's meter (points x distance falloff x wall penalty x `sensitivity`); the meter decays steadily; at the wake threshold the sleeper wakes (after the travel delay, a block tick of `floor(distance)` ticks; then the block goes and the real mob spawns with the same gear, facing the source). No dice: the same noise always does the same thing.
- **Lazy decay, no ticker:** the meter is stored with the game time of its last change (block entity NBT `disturbance`, `disturbance_time`) and decayed on read.
- **Stirring:** crossing the stir threshold upward plays the mob's ambient sound quietly and low (volume 0.35, pitch 0.6-0.7) from the block: the player's warning. Cheap (one sound, only on the crossing).
- A wake emits a custom `bubbas_reworked_mobs:awaken` game event; other sleepers hear it through the same path (wool included), at most 4 wake per event (prior art capped cascades at 4; nearest first, `BY_DISTANCE` delivery with a budget set around the synchronous dispatch).
- If another mod cancels the spawn, the block stays (nothing is lost).

**The noise table** (all in `UndeadTuning`, starting values, owner to tune; code `undead/sleeper/SleeperHearing` and `SleeperListener`):

| | Value | Warden, for comparison (26.3 source) |
|---|---|---|
| Events heard | `#bubbas_reworked_mobs:sleepers_hear` = `#minecraft:vibrations` + awaken | `#minecraft:warden_can_listen` (vibrations + shriek) |
| Listener radius | 16 | 16 |
| Footstep (step, swim, flap: vanilla frequency 1) | 40 points walking, x2 sprinting (the source's `isSprinting`; faster movement also means more steps per second) | every vibration: +35 anger |
| Every other vibration | 55 + 2.5 x its vanilla vibration frequency (`VibrationSystem.getGameEventFrequency`): landing / projectile land / splash 60, item use 62.5, entity action / equip 65-67.5, hit (entity damage) 72.5, eat / drink 75, chest open 80, block break 85, place 87.5, death 92.5 | +35 each; a projectile +10 the first time |
| Explosion, lightning | 1000 (wakes anything within ~10 blocks) | +35 |
| Awaken (a neighbour waking) | 300 (wakes sleepers within ~6 blocks at once) | n/a |
| Distance falloff | 1 within 2 blocks, then `((12 - d) / 10)^2`: 3 blocks 0.81, 4: 0.64, 5: 0.49, 6: 0.36, 8: 0.16, 10: 0.04, 12+: 0 | none (only a 2 s cooldown between vibrations) |
| Walls | x0.3 per opaque full block (`isSolidRender`) on the straight line from the noise's block centre to the sleeper's, ends excluded, at most 3 counted. One `traverseBlocks` walk, after the cheap checks | vibrations ignore walls |
| Wool | the vanilla 6-ray `#occludes_vibration_signals` check: fully blocked | same |
| Decay | 0.5 per tick (10 a second): a half-disturbed sleeper settles in 5 s, a nearly woken one in 10 s | -1 anger per second |
| Thresholds | stir 50, wake 100 | agitated 40, angry 80, max 150 |
| Ignored | spectators; `dampensVibrations` sources; sneaking for `#ignore_vibrations_sneaking` (steps, landings, item use, shooting); `#dampens_vibrations` on the affected block (carpets); its own block; sources in `#bubbas_reworked_mobs:does_not_wake_sleepers` (default `#minecraft:undead`, so a woken horde shuffling past doesn't cascade by footsteps; only awaken chains); animals (mob category other than monster or misc) except when they are hurt or killed (a fight is loud); `sensitivity` 0 | spectators, sneaking, non-targetable sources |

- What it adds up to (each is a game test in `SleeperNoiseGameTest`, deterministic, at vanilla speeds: walking 0.216 blocks a tick, sprinting 0.28, a step every ~1.7 blocks): sprinting circles 6 blocks out wake it in ~1.2 s (test: <= 30 ticks); sprinting straight past 6 out wakes it before you're 3 blocks past; walking right past 3 out wakes it within ~2 s (test: <= 50 ticks from 6 blocks before); walking past 5 out stirs it (peak ~85) without waking, and it settles to 0 within 10 s; walking past 7 out (peak ~28) or sprinting past 11 out doesn't wake it; one footstep at 5 blocks is ~20 points; two blocks broken or placed 4 blocks away wake it, one doesn't; three jump landings 3.5 blocks away wake it, two don't; two hits on a pig 3 blocks away wake it; an explosion 8 blocks away wakes it at once and one 15 away does nothing; sprinting past 5 out behind 2 blocks of stone doesn't wake it (in the open it does); one awaken wakes default sleepers 2 and 5 blocks away but not one 10.5 away; sneaking past 2 out is silent.
- Walking slowly round a sleeper at 3 blocks still wakes it within ~10 s (the meter gains ~3 per step net); at 8+ blocks walking never adds up. Opening a chest right next to a sleeper (80) plus a few steps wakes it; sneak to loot.
- **Creative mode** (the owner tests in creative): vanilla's `Player.getMovementEmission` emits steps and landings for any player on foot, creative included, and none while flying (`abilities.flying`). A creative player's steps wake sleepers like a survival player's (tested), but a woken undead never targets a creative or spectator player. A player's landing is checked from its movement packets (`doCheckFallDamage`) and heard on the server like any step.
- Robbing keeps its own per-theft chance (see Robbing), multiplied by `sensitivity`.
- Each sleeper has a `sensitivity` (block entity NBT, default 1) that multiplies every noise: 0 never wakes (decor, warded), large always wakes (traps). Strong light and keep-asleep areas (the `api.Sleepers` hook, used by Totems & Artifacts' full-pyramid wards) scale it at read time (see Calming sleepers).
- The woken mob is persistent (never despawns, outside the mob cap), keeps the sleeper's custom name, always drops its gear, and an undead that wakes empty-handed grabs what's lying within 3 blocks, weapons first (see "Grabbing what's lying around" below).
- **No wake-up violence (owner: "the sleepers fight each other when they wake up!").** A woken mob takes its waker as its target only if the waker is a survival or adventure player. Cause found: the awaken event's source is the mob that just woke (and footsteps can come from other mobs), so a chain-woken undead targeted the undead that woke it and the group fought itself. Now a sleeper woken by a neighbour, a villager, a creeper or anything else wakes without a target and finds players with its normal AI. **Retaliation stays vanilla (owner: "undead can retaliate against arrows in the traditional vanilla sense")**: a woken zombie hit by a skeleton's stray arrow may fight back through vanilla's `HurtByTargetGoal`. Tests: `chainWokenUndeadDoNotFightEachOther`, `onlyASurvivalWakerIsTargeted`, `retaliationAgainstUndeadStaysVanilla`.
- History: first built as a dice roll per heard event (step 4 %, block break 30 %, linear falloff, 1 s deafness after every roll); owner: running past sleepers didn't reliably wake them, so it was rebuilt as the meter above.

## Waking in tight spots (2026-11-22)

A sleeper lying somewhere lower than its mob is tall (a crypt wall niche under a slab, a shallow grave) used to wake into a box overlapping blocks: no suffocation (eyes clear a half slab) but stuck. `SleeperWaking.stepOutIfStuck` now moves it to the nearest free spot: beside it on the same level first (a niche's open side), then one up, then diagonals; with none it stays. Milestone test `BubbasReworkedMobsGameTest.sleeperInANicheStepsOutWhenItWakes`.

## Drowned sleepers in shipwrecks (owner, 2026-11-22; in progress, agent 2)

- A **drowned sleeper** variant, placed inside **shipwreck interiors** (the cabins and hold): the ship's drowned crew, waking like other sleepers. Underwater sleepers are waterlogged already (`SleeperBlock.WATERLOGGED`). Shipwrecks are vanilla structures, so they're added by a structure processor or a placement hook. (Sunken graveyards: no, owner.)

## Back to sleep at dawn (owner, 2026-11-22; in progress, agent 2)

- Woken skeletons and zombies (and their variants) that survive the night look for shade at sunrise: an open grave, a crypt niche or a dark corner nearby. They lie down there and **become sleepers again** (a sleeper block with the gear they carry). Crypts refill themselves, and a cleared graveyard quietly repopulates if you leave survivors. Builder proposes the search range and what counts as shade; the resting dead (never-waking grave skeletons) are not involved.

## Fire as a weak ward (owner idea, 2026-11-22; in progress, agent 2)

- A lit **campfire** (and soul campfire) or plain **fire** works as a **weak ward**: hostile mobs are reluctant to come close (a few blocks), like a small, soft version of the Totem of Warding (`specs/totems-artifacts.md`). It is a deterrent, not a wall. **Only mobs that would fear fire** (owner, 2026-11-22): nothing that's immune to fire or at home in it, so no Nether mobs (blazes, ghasts, magma cubes, piglins, hoglins, wither skeletons, striders...) and no other fire-immune mob. In practice, a mob that takes fire damage and isn't from the Nether (for example `fireImmune()` false and not in a Nether-mob tag). Details (radius, whether a hunting mob still pushes through) to be proposed.
- Light already makes sleepers harder to wake (above), so a campfire also helps keep a crypt asleep.

## Undead variants (owner, 2026-11-22)

- **Husks** (deserts) and **strays** (snow) become sleepers too, for crypts and camps. Husks lie buried in sand and burst out in a puff of sand that briefly blinds you (particles plus a short effect). Strays lie frozen in ice or packed snow that cracks as a warning before they break free.
- **Bogged are not sleepers** (owner): they act like the reworked drowned (`specs/drowned.md`): lurking in swamp water and mud, moving unseen, ambushing from the reeds.
- Resting dead in yard graves never wake (`specs/graveyards.md`).

### As built (2026-11-22)

- **Husk sleepers** (new): lie buried in sand. When woken, the sand over and round them bursts in a cloud of sand and dust; any survival player within 4 blocks is **blinded for 2 seconds**. Woken husks don't burn by day and their bare-handed hits give Hunger (as vanilla husks).
- **Stray sleepers** (new): lie frozen in ice (clear ice in crypt niches, so you can see them inside). When something wakes one, the ice **cracks first** (a glassy crack sound and ice shards: the warning) and the stray breaks free **1.5 seconds later**, shattering the ice round it. Woken strays shoot Slowness arrows (as vanilla strays) and wear the stray's ragged clothes.
- New creative items: **Sleeping Husk** and **Sleeping Stray** (each sleeper item now has an icon of the undead's head with its eyes shut; the skeleton and zombie icons were flat colour squares before).
- **Desert and snowy graveyards**: crypt yards take their climate from the biome. Desert (desert, badlands) yards have sand grave plots, husks in the crypts (mostly husks, some skeletons) buried under sand in the wall niches, and resting husks in the yard graves. Snowy yards (base temperature under 0.15) have snow plots, mostly strays (some zombies) frozen in ice niches, and resting strays in the graves. Everywhere else is as before.
- **Bogged lurk** (they are not sleepers): a bogged notices a player within 32 blocks and slips to a hiding spot near them: water 1-2 deep over a floor (2 deep hides it fully), or mud beside reeds (sugar cane, tall grass, ferns). There it crouches, still, and holds its fire. While nobody looks toward it, it now and then moves to a new spot (about every 30 s, every few seconds once you've moved more than 12 blocks off, so it follows). It **ambushes** when a player within 10 blocks turns their back on it (it's more than 100 degrees from where they look) or anyone comes within 4 blocks, or when it's hit; then it fights normally (poison arrows) and goes back to hiding 10 s after losing its target. Bogged from trial spawners stay vanilla.

#### Numbers (starting values, owner to playtest)

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

#### Builder's choices

(The two the owner may want to flip are under Open.)

- Resting dead in desert/snowy yard graves are resting husks/strays (`YardKinds.Climate.resting()`).
- Snowy crypts mix in **zombies**, desert crypts mix in **skeletons**, so each crypt still has both melee and archers.
- Stray niches use **clear ice** (not packed ice) so players see the stray frozen inside; snow-yard grave plots are snow blocks. Ice can melt next to strong light, but crypts are candle-lit (owner: not too well lit), so it stays.
- The bogged hides fully underwater in 2-deep water (you can just make out its shape).
- Bogged's lurk mind is not saved: after a reload it finds a spot again.
- Cleared what the husk/stray was in: the block over the sleeper (and two up), its four sides, and the four sides of the block over it (so a lying one's foot end is freed). Only sand-like (sand, red sand, sandstone slab, suspicious sand) for husks and ice-like (ice, packed/blue ice, snow block, powder snow) for strays.

#### Code

- `UndeadVariant` has HUSK and STRAY (block, item, woken entity each). `isSkeleton()` = SKELETON or STRAY (bows, robbing weapon choice in crypt gear).
- `WokenHusk extends WokenZombie` (no sun sensitivity, husk sounds, Hunger on bare-handed hit), `WokenStray extends WokenSkeleton` (stray sounds, Slowness arrows). Client: `WokenZombieRenderer(context, texture)` and `WokenSkeletonRenderer.stray(context)` (stray body/armor layers + `SkeletonClothingLayer`); `SleeperRenderer` has HUSK/STRAY posed models.
- Tags: woken husk/stray are in `#woken_undead`, `#grabs_loot`, `#zombies`/`#skeletons`; `#burn_in_daylight` now lists skeleton, zombie, stray explicitly (not the whole woken tag), so husks don't burn.
- Breakout: `undead/sleeper/SleeperBreakout` (`breakOut` runs in `SleeperWaking.wake` before the mob is added; `crack` runs in `SleeperBlockEntity.scheduleWake` for strays and raises the delay to at least `STRAY_CRACK_TICKS`).
- Graveyards: `YardKinds.Climate` (TEMPERATE/DESERT/SNOWY) from the biome at the yard's centre in `CryptYardStructure`; saved in the piece tag as `Climate`; `CryptYardPiece.climate()`. `Build.encase` puts sand/ice over a husk/stray crypt niche; `Build.resting(…, variant, …)` replaces `restingSkeleton`.
- Bogged: `bogged/BoggedLurk` (state machine IDLE → TRAVEL → LURK → HUNT, one mind per bogged in a WeakHashMap), `bogged/BoggedTuning`, `mixin/BoggedLurkMixin` (Mob.customServerAiStep HEAD, instanceof Bogged). `MobFinalizeSpawnMixin` tags spawner bogged `bubbas_reworked_mobs.vanilla_bogged` so they stay vanilla. `BoggedLurk.hideAt(level, x, z, aroundY)` scans down from 6 above the player's feet (not the heightmap, which mangrove canopies would stop).

#### Tests

- Milestone server tests `UndeadVariantsGameTest`: `huskBurstsOutOfSandAndBlinds` (core since 2026-11-22), `strayCracksItsIceBeforeBreakingFree` (still frozen 20 ticks in, free by 40), `boggedLurksWhileWatchedAndAmbushesAnAvertedBack`. (`huskAndStrayTags`, a plain tag check, was dropped in the 2026-11-22 test cleanup.) `GravesGameTest` yard core test now expects the yard climate's resting kind. Full milestone server suite: 114/114 pass.
- Client part `variants` (`VariantsClientGameTest`), screenshots looked at: `variants-sleepers` (husk and stray sleepers sitting and lying, correct textures), `variants-buried` (sand over a husk, clear ice over a stray), `variants-husk-burst` (sand cloud, husk rising out of its trench), `variants-stray-crack` (ice shards, still frozen), `variants-stray-free` (ice shattered, stray standing), `variants-woken` (woken husk and stray, stray's ragged clothes showing), `variants-bogged` (one bogged crouched under a reedy pool's surface, one standing on the bank).
- Not shot: a naturally generated desert/snowy yard (covered by the server tests and the climate code; worth a look in the next test world tour).

#### Art

- `tools-local/make_sleeper_icons.py`: four sleeper item icons at 16x and 32x; row added to `docs/art-todo.md`.

## Loot on display (shared block)

Weapons near sleepers, and later other loot, are shown with a dedicated block rather than as dropped items or item frames (owner's choice: it looks better than grabbing swords off item frames, and later loot systems reuse it). It is a deliberate new block under B15 fewNewBlocks; the owner gave the reason.

- One block holding exactly one real item (any item: weapons and tools look best; armor later if useful) in a block entity. Lives in Bubba's Reworked Mobs' shared code (not under undead); may move to a loot mod later.
- Blockstate `placement`: `ground` (lying flat), `buried` (mostly underground, grip showing), `wall` (hung or leaning against the wall it's attached to), `stuck` (planted point-down). Plus `facing`, `waterlogged`. More placements can be added.
- Rendering: the item's own model, posed per placement, so any item (vanilla or modded, any resource pack) looks right with no extra art.
- Taking: use with an empty hand takes the item and removes the block. `buried` needs a shovel and makes more noise (sleepers hear it). Breaking the block drops the item.
- Worldgen and structures place it next to sleepers, half-buried in graves, hung on crypt walls.
- Player placement: designed, not built: sneak + middle click places the held item on the ground anywhere (owner, 2026-11-22; built together with endermen trading, `specs/endermen.md`).

As built (`display/` package; renderer in `client/display/`):

- Block `bubbas_reworked_mobs:loot_display` (`LootDisplayBlock`) + block entity (`LootDisplayBlockEntity`), no ticker, no block item, no loot table. The item is saved as a one-slot `Items` list (like a container) and carried as the vanilla `minecraft:container` component, so it survives save/load, structure templates and `/setblock ... {Items:[{Slot:0b,id:"minecraft:iron_sword",count:1}]}`. Middle-click (creative) gives the displayed item.
- `facing` is the side the item shows its front to: for `wall`, away from the wall it hangs on; for the floor placements, toward whoever placed it (a `ground` item lies with its grip toward `facing`, point away; a `buried` grip leans toward `facing`).
- Shapes (outline only; there is **no collision**, you walk over and through displays): `ground` a 14x1.5 px slab, `buried` a 6x5 px stub, `stuck` a 6x13 px post, `wall` a 12x12x1.5 px plate on the wall. Floor placements need a centre-sturdy floor and `wall` a sturdy wall behind (like torches); when that goes the block pops and drops the item. Pistons pop it too.
- Rendering (`LootDisplayRenderer`): the item resolved through `ItemModelResolver` in the `FIXED` display context (what item frames use) at 0.75 scale, so a sword is about 0.93 blocks long; then per placement: `ground` flat on the floor with a small per-position turn (up to 15 degrees) so rows don't look stamped; `buried` point-down, sunk so only the top 0.4 block shows, leaning 25 degrees; `stuck` upright, point 0.1 block into the ground; `wall` flat against the wall like a frameless item frame. The block model is invisible (particle only: iron block). Cost: one item per block, no stack copies; the render state is created per frame as in vanilla's shelf and item frame. Poses assume a corner-to-corner tool/weapon sprite (all vanilla swords, axes, tools, spears, bows, the trident's sprite); other items still render, just posed the same way.
- Taking (`LootDisplays`): an empty main hand takes `ground`, `wall` and `stuck` (pickup sound, the quiet `minecraft:item_interact_finish` game event, which vanilla ignores while sneaking, so sneaking makes taking a displayed item silent: owner confirmed). Holding anything else does nothing to the display. `buried` ignores an empty hand (an action-bar hint says to dig) and needs a shovel (`#minecraft:shovels`): digging costs the shovel 1 durability and plays the ground's break sound plus `minecraft:block_destroy`, which sleepers hear three times as well as the quiet event (0.3 vs 0.1 base). Breaking drops the item: other placements break at once like a flower; `buried` mines like packed dirt (hardness 0.8, fast with a shovel; the block is in `#minecraft:mineable/shovel`). A waterlogged display leaves its water behind.
- **Grabbing what's lying around (owner: "all weapon-carrying mobs should grab whatever's lying around").** Any mob in the entity tag `#bubbas_reworked_mobs:grabs_loot` (our woken undead now; other weapon-carrying mobs as later parts rework them) takes **any** item, not only weapons, so loot rooms and dungeons can put specific items in their hands. On waking: weapons first (nearest within 3 blocks), then any other item if no weapon is in reach. Afterwards, while chasing a player, it ignores displays and items entirely (the chase wins); it still picks up loose items it touches, like vanilla. Only when idle (no target) does it walk to a display block or loose item within a short range and take it. Displays come before loose items; any placement (undead don't need shovels); the display block goes. Runs after the mob is added, so a refused spawn takes nothing. Shared code, not under undead.
  As built (`loot/` package, shared code): `LootGrabbing` holds the rules and the take helpers both paths use; `GrabLootGoal` is the idle behavior.
  - Tags (datagen): `#bubbas_reworked_mobs:grabs_loot` (entity types; woken skeleton, zombie, husk and stray) and `#bubbas_reworked_mobs:preferred_on_wake` (items: `#minecraft:swords`, `#minecraft:axes`, `#minecraft:spears`, bow, crossbow, trident, mace; vanilla has no tag for the last four). The old `#sleeper_weapons` tag is gone. Skeletons now take any of these too, and loose items as well as displays (before, only zombies took loose weapons).
  - Waking grab (`LootGrabbing.grabOnWake`, called by `SleeperWaking` after the mob is added, empty main hand only): nearest `#preferred_on_wake` display within 3 blocks, else nearest such loose item, else nearest display of anything, else nearest loose anything. One item goes into the main hand and always drops on death; a skeleton re-picks bow or melee attack through vanilla's `onEquipItem`.
  - Idle goal (`GrabLootGoal`, MOVE flag, priority 5 on our mobs: below their attack goals, above strolling): runs only with an empty main hand, **no target**, not riding, and the mob in `#grabs_loot` (so a datapack can switch it off, and a later mixin can add the goal to vanilla mobs without touching their behavior until they're tagged). It stops the moment the mob gets a target. Every 20 ticks (first search at a random offset so mobs loaded together spread out) it looks for the nearest display within 8 blocks, else the nearest loose item, paths there with vanilla navigation and takes it within 1.75 blocks (displays through the same helper as the waking grab, loose items with vanilla's pickup animation, one of a stack). Unreachable targets, and ones it gave up on after 15 s, are skipped for 10 s. While the mob is in the air (just spawned, falling) it doesn't search, since ground navigation can't path then.
  - Cost: a search walks the block entities of the loaded chunks in range (`LootDisplays.findNearest`, never loads chunks) plus one entity box query; one path per chosen target, a re-path at most once a second when navigation ends early. Nothing at all for mobs that hold something or have a target.
  - Contact pickup stays vanilla's: woken mobs come out of `create` without vanilla's natural-spawn roll, so their vanilla "can pick up loot" flag is off and they don't scoop items up by touch; with the flag on (vanilla's rule) they do, even mid-chase (tested).
  - Tests (`GrabLootGameTest`, real paths): each walking test runs in its own test environment (tests in a batch sit 5 blocks apart, inside the notice range) at night (in daylight they burn and flee the sun).
- Worldgen defaults: skeleton sleepers get standard bows; zombie sleepers get tools/weapons at vanilla's normal zombie equipment rates. Loot rooms and dungeons will place specific items on display instead.
- Placing for building and tests: `/bubbas_reworked_mobs display <ground|buried|wall|stuck> <item> [<pos> [north|south|east|west]]` (ops, permission level 2). Without a position it goes where you look (within 20 blocks): `wall` on the face you look at, the others on the floor you look at, turned toward you. It refuses a spot that's taken or has nothing to hold the item up. Survival placement (sneak + middle click) is designed, not built (above).

## Robbing

Using a sleeper with an empty hand takes one item: held weapon first (main hand, then off hand), then armor from the top down. Weapon theft rolls a small wake chance (10 %); armor theft always rolls a larger one (30 %); both times the sleeper's sensitivity. A failed roll wakes it 2 ticks later, angry at the thief. Opening a nearby chest makes the normal container-open noise, which sleepers hear.

## Mining and placing sleepers

- Mining a sleeper with a **Silk Touch pickaxe** gives the whole mob as an item (its "statue": a sleeper item of that undead type, no gear on it, pose kept); its armor and weapons drop as separate items (owner's call). The tool is the item tag `#bubbas_reworked_mobs:sleeper_harvest_tools` (pickaxes) plus Silk Touch. Anything else (no Silk Touch, another tool, bare hands) leaves remains (a bone or rotten flesh) and the gear. The gear drops from the block entity whenever the block goes (mining, explosions, commands), except on waking, when the mob takes it.
- Sleeper items can be placed back as sleepers, and placed sleepers can come alive like generated ones (decor, traps).
- Magic that prevents waking: the Totem of Warding, now in Bubba's Totems & Artifacts (`specs/totems-artifacts.md`), through the sleeper hook.

## Scope and variants

- Only this mod's undead change: our own entity types (woken sleepers and the rare natural spawns), tagged as undead. Vanilla zombies and skeletons elsewhere (other mods' spawners and structures) stay vanilla, and AI or animation mods that target vanilla classes don't override ours. Mob spawner tweaks are parked (BACKLOG.md).
- Built: skeleton, zombie, husk and stray sleepers, each sitting and lying (`UndeadVariant`). Bogged are not sleepers: they lurk (Undead variants, above). Drowned sleepers only in shipwrecks (in progress); elsewhere drowned are their own segment (`specs/drowned.md`). Ideas, not designed: parched, wither skeleton, zombie villager. Not yet: zombified piglin, skeleton/zombie horses, camel husk, zombie nautilus.
- One sleeper block per variant (or a variant property), each with its own 16x and 32x textures.

## Natural spawning

Owner confirmed (2026-11-14): the cut applies everywhere (surface, caves, Nether, End), monsters only; the older camps idea's "no surface spawns, vanilla underground" does not override it.

One small mixin cuts the global monster cap to a game-rule percentage (`bubbas_reworked_mobs:monster_cap_percent`, default 15) with an off switch for spawn-control mods (`bubbas_reworked_mobs:cut_monster_spawns`). It sits at HEAD of `NaturalSpawner$SpawnState#canSpawn`, the per-mob test natural spawning runs after it has picked a mob and a spot, so it sees the type and the position: biome spawns get the cut cap, while spots whose monster list comes from a structure (witch huts, monuments, outposts, fortresses) keep vanilla's cap. This replaced the earlier two-mixin plan (cap scaling + a biome-only gate) because one hook does both, and it only ever says no, so it composes with other spawn mods. The per-player cap is left at vanilla's; in multiplayer the cut global cap is what binds. Other categories, light rules, spawners, trial spawners and raids are untouched. Wards (Totems & Artifacts) refuse natural monster spawns with their own mixin on the same method (`specs/totems-artifacts.md`).

## Tuning

- Speed, health and damage are our entity types' default attributes (`FabricDefaultAttributeRegistry.register`; values in `UndeadTuning`): skeleton 60 health, speed 0.15; zombie 80 health, speed 0.13; fists 1 damage (vanilla: 20 health, 0.25/0.23, 2-3 damage). Zombies get no reinforcements and don't turn into vanilla drowned.
- The early game is meant to be quite a bit tougher: high health and armor-shredding hits, balanced by undead being slow and easy to escape. (Bubba's Reworked Mining slows bare tools: `specs/reworked-mining.md`.)

As built (`undead/UndeadCombat`, `undead/entity/DriftToHordeGoal`, client `client/undead/UndeadGaits`; numbers in `UndeadTuning`). All values are **starting values, owner to tune**. Everything is scoped to our woken undead (`#bubbas_reworked_mobs:woken_undead` or our own entity/renderer classes); vanilla mobs are unchanged.

- **Fists shred armor only in a streak (owner's change).** One fist hit does almost nothing; an undead that traps you and keeps hitting tears your armor apart. Each undead keeps its own streak against its current victim (several undead on one target each count separately); a hit within 3 s (60 ticks) of its last keeps the streak, a longer gap or a new victim restarts it. Extra armor durability per hit by streak: 1st 0, 2nd 1, 3rd 3, 4th and later 6, spread over the worn armor pieces (head first gets any remainder). On top of vanilla's own wear (players only: max(1, damage/4) per piece). Only bare-handed melee (empty main hand), only unblocked hits. Applied in Fabric's `ServerLivingEntityEvents.AFTER_DAMAGE`; streaks live in a weak map keyed by the attacker (no NBT, they lapse on unload).
- **Smite kills outright by chance:** 4 % per level (Smite V = 20 %), with soul-fire and soul particles and the zombie-villager cure sound (pitch 1.4). Added as a `post_attack` effect on Smite through Fabric's `EnchantmentEvents.MODIFY_WITH_LOOKUP` (composes with datapacks that change Smite; `smite.json` is not overridden): requirements `entity_properties` on `#woken_undead` plus `random_chance` with `enchantment_level` linear 0.04. The kill is 10000 damage of `bubbas_reworked_mobs:purged` (in `#bypasses_armor`, `#bypasses_cooldown`, `#bypasses_enchantments`, `#bypasses_resistance`, `#bypasses_effects`, `#no_knockback`; death message reuses vanilla's "magic"), credited to the player for loot and XP.
- **Bows:** our skeletons take 25 % of arrow damage (any `AbstractArrow`); our zombies take 2x from headshots. A headshot is an arrow whose position, when it hurts, is in the top quarter of the hitbox (adult zombie: above 1.46 of 1.95 blocks, matching the 0.5-block head cube). Vanilla moves the arrow to the hit point on the (0.3-inflated) hitbox before calling hurt, so no mixin is needed. Both are applied in our entity classes' `hurtServer`, before armor.
- **Walk animations (owner's brief, replaces the generic stagger):** our own model subclasses (body and armor, so armor follows) in our own renderers; no mixin into the shared humanoid model, so vanilla mobs can't be affected. Skeletons walk with vanilla's legs and vanilla zombie arms (both reaching straight out, raised higher when aggressive), except while drawing a bow. Baby zombies keep vanilla's model and vanilla movement. GeckoLib not needed.
- **Zombie shamble: a heavy, limp drag, then a fall onto the good leg. LOCKED: owner-approved in play ("the zombie animation looks AWESOME"); change it only on the owner's word. (owner: "drag that back leg up, then push forward in a faster motion with a stiff-ish front leg", "more like a longer drag then a 'hop' with that front leg", then "too lurchy"; this version owner-approved by eye: "pretty good").** Two phases per cycle, and the zombie's real movement pulses with them, gently: the jerk comes from the timing (long drag, quicker step), not from violent speeds (`undead/ZombieGait`, numbers in `UndeadTuning` and `UndeadGaits`, all starting values):
  - Cycle **30 ticks**. **DRAG** is the first **75 %**: the body keeps creeping forward at **0.9x** the average speed; the stiff right leg is planted, in front at the start and behind at the end, sweeping back as far as the body moves (-0.45 to +0.4 rad); the bad left leg is dragged up from behind (+0.55 rad) to under the hip (eased in and out, toe turned out, scraping, faster than the body). **STEP** is the last **25 %** (called the lurch in code): speed `0.9 + k sin²` with k chosen so the cycle averages exactly 1x (1.3x on average over the step, peak 1.7x; after the body's momentum the server measures 0.9x through the drag and about 1.5x at the peak), so escape difficulty is unchanged. The body falls onto the good leg: the right leg swings through stiff (eased) and lands in front, the torso pitches forward (+0.10 rad) and slowly rights itself through the drag, a slight dip (0.6/16 block) on landing, no bounce; the now-planted left leg sweeps back by exactly the body's distance through the step.
  - Dead weight and overlap: the arms hang and swing like pendulums a beat behind the body, forward with the fall (up to 0.35 rad, never backward), drifting back to hanging through the drag, the left a little later and smaller; the torso rocks onto the bad side as the good leg swings through and back over the good leg through the drag (0.05 rad either way); the head lolls after the torso and nods a little after the fall.
  - History: first built as drag 65 % at 0.3x and a lurch up to 4.3x (owner: lurch too fast); a longer lurch was tried and dropped; then drag 75 % at 0.8x and a hop with an upward bob (owner: still too lurchy, arms went backward); now the gentle fall above.
  - Server: `WokenZombie.setSpeed` multiplies the speed the move control sets every walking tick by `sqrt(velocity factor)` (mobs accelerate by speed squared: the speed and the forward input both carry it). No attribute modifier, so nothing is synced. It applies whenever the move control walks the zombie, chasing included (that's the escape case); a zombie standing still (idle, or hitting at arm's reach) moves nothing and stands. Not for babies or in water.
  - Phase: `(gameTime + murmurHash(entity id) mod 30) mod 30`, the same on client and server with no packets; each zombie on its own offset so a horde doesn't lurch in step.
  - Client: the renderer reads the same phase **4 ticks behind** (`WokenZombieRenderer.CLIENT_LAG_TICKS`: the body's momentum and the client's interpolated position put the visible motion 3-4 ticks behind the server's phase; `UndeadGaitClientGameTest` measures and prints it as `[gait-lag]`). How much it shambles eases in over a few ticks once it moves and out once it stands (`WokenZombie.gaitWalk`, client only). Spread straight legs lift the hips, so the body drops until the lower foot touches the ground. The client sees positions every 3 ticks (vanilla zombie update interval), so the step shows as 2-3 steps of motion.
  - Tests: `ZombieGaitGameTest` (chasing with its own AI against a vanilla zombie of the same speed: same average within 15 %, slower drag ticks for most of the cycle, ticks over 1.25x in the step); `UndeadGaitClientGameTest` screenshots one full cycle from the side (`gait-side-0..9`, 3 ticks apart from the start of a drag, so frames 0-7 are the drag and 8-9 the step), the front, and the whole file.
- **Zombies drift into groups:** a goal at priority 6 (after attacking and grabbing loot, before strolling). Every 200 ticks (each zombie on its own random offset), a woken zombie with no target makes one entity query for other woken zombies within 24 blocks (6 up/down), takes the nearest one and the zombies within 8 blocks of it as the group, and walks to the group's centre at normal speed, stopping within 3 blocks. A target always wins (the attack goals hold the move flag and the goal stops when a target appears).

## Grave Talisman and Totem of Warding (moved)

Both moved to Bubba's Totems & Artifacts (C6, 2026-11-16): `specs/totems-artifacts.md`, which now describes the talisman, the totem's pulse, wards and the totem statue. Old saved items load there through registry aliases. What stays here is what calms or wakes sleepers on its own (light, bells) and the hook that mod uses.

## Calming sleepers (light and bells)

Numbers in `UndeadTuning` (`LIGHT_CALM_LEVEL`, `LIGHT_CALM_FACTOR`, `BELL_POINTS`, `BELL_RANGE`; they used to be in `WardingTuning`).

**Light (owner: strong light near a sleeper makes it a little harder to wake, like torches on spawners).** `SleeperBlockEntity.effectiveSensitivity(level)` = stored sensitivity x 0.7 (`LIGHT_CALM_FACTOR`) at block light 12 or more (`LIGHT_CALM_LEVEL`, just above what spawners allow), x 0 inside a keep-asleep area (`SleepAreas`, below); used by hearing, bells and robbing. Sky light doesn't count. The light lookup only happens for noises in earshot. Placing the torch is itself a noise. *(The step at 12 and x0.7: agent's choice.)* Test note: the game test server's light engine lags (block light still 0 forty ticks after placing glowstone), so the light test waits up to 600 ticks.

**Bells (owner: extra loud to sleepers, heard as far as players hear them).** `BellBlockMixin` (RETURN of `attemptToRing(Entity, Level, BlockPos, Direction)`) calls `undead/sleeper/SleeperBells.rang`, which walks the loaded chunks' sleepers within 32 blocks (`BELL_RANGE`, no chunk loads; `SleeperFinder`) and feeds each through the listener's shared `hear` path (wool, walls, stir, wake, travel delay). A ring is 250 points at the bell (`BELL_POINTS`), falling off linearly to 0 at 32: it wakes sleepers within about 19 blocks and stirs them out to about 25 *(250 and the linear falloff: agent's choice)*. Walls and wool muffle a bell like any noise (owner, 2026-11-16: a surface bell doesn't wake a crypt below). Any ring counts (a hit, a projectile, redstone). The listener ignores the bell's own BLOCK_CHANGE vibration so it isn't counted twice.

## Sleeper hook for other mods (`api/Sleepers`)

A small public class, `io.github.bubba8587.bubbas_reworked_mobs.api.Sleepers`, the same idea as Reworked Beacons' payload API; nothing else in this mod is API. Stable: add methods, don't change them. Its user today is Bubba's Totems & Artifacts (optional both ways: this mod `suggests` it and has no compile dependency on it).

- `int calm(ServerLevel, Vec3 centre, double radius, float points)`: takes points off the disturbance meter of sleepers in range (wake 100; never below 0).
- `void keepAsleep(ServerLevel, Object source, AABB area, int lifetimeTicks)` / `void release(ServerLevel, Object source)`: sleepers inside the area never wake from noise, bells or robbing. Each source's area lapses unless refreshed, so callers need no cleanup hook. Backed by `undead/sleeper/SleepAreas`; `effectiveSensitivity` checks it.
- `boolean isKeptAsleep(Level, BlockPos)`, `boolean isSleeper(Level, BlockPos)`, `double nearestSqr(Level, Vec3, double range)` (client or server).

## Worldgen

Sleepers are plain block placement. **Crypt yards and crypts (built 2026-11-22, step 1) are a code-built structure, not jigsaw:** `specs/graveyards.md`. Yard graves hold resting dead and the crypts hold niche sleepers, husks and strays in desert and snowy yards.

## Libraries

Only Fabric API (+ MixinExtras bundled with Loader). GeckoLib and Polymer were considered and not needed. spark for profiling during development.

## Performance budget

Sleeper on the server: 0 per tick. Hearing: under 0.05 ms per tick with 200 sleepers near a walking player. 100 armed sleepers in view: under 1 ms per frame. Measure with spark before release.

## Open

- **Playtest numbers:** the noise table, tuning, husk/stray/bogged numbers above are starting values.
- **Resting dead in desert/snowy yards:** husks/strays (built) or skeletons everywhere (owner said "the skeleton kind")?
- **Bogged hiding:** fully underwater in 2-deep water (built), or head showing (1-deep)?
- **Bury-break system** (BACKLOG): shovel-only vs by hand, noise, tools. Today a shovel digs loudly; by hand works, slowly.
- **Back to sleep at dawn:** search range and what counts as shade (builder proposes).
- **Fire ward:** radius, and whether a hunting mob pushes through (builder proposes).
- **Mob spawner tweaks:** parked, no design yet.

## Milestone test objectives
- (objective, no test yet) A woken skeleton that grabs a bow or sword from a display or the ground never crashes the game: the item is equipped at the start of its next tick, outside goal ticking (crash 2026-11-15: GoalSelector NPE when Skeleton.reassessWeaponGoal ran mid goal-tick), and a skeleton killed before that tick still drops it.

Test-lite policy (`specs/quality.md`, Test suites): `ReworkedMobsCoreGameTest` runs on every build and push; everything below is the milestone suite, run on release tags, manual CI dispatch and `-PmilestoneTests`. Each line says what the class proves.

- Core (`ReworkedMobsCoreGameTest`, 11 across the mod, one smoke test per main mechanic, trimmed 2026-11-22 from 34): walking near a sleeper wakes it; the `api.Sleepers` hook calms, senses and keeps sleepers asleep (the contract Totems & Artifacts relies on); a waking mob grabs a weapon first; a husk bursts out of sand and blinds; plus one each for creepers, endermen, graveyards, drowned, Fabricators, spiders and camps (in their specs). Everything else here is milestone.
- `DawnGameTest`: survivors go back to sleep at dawn (with their gear), only those that saw the night, sitting down under any cover. `FireWardGameTest`: idle mobs keep away from a lit campfire, only lit fire repels, only fire-fearing mobs.
- `SleeperCalmGameTest` (was `WardingGameTest`): light calming (the rule, then a glowstone in the world, run as `SleeperNoiseGameTest.strongLightCalmsSleepersInWorld`), loud bells and the hook.
- The Grave Talisman and Totem of Warding tests moved to Totems & Artifacts (`specs/totems-artifacts.md`).
- `BubbasReworkedMobsGameTest`: sleepers end to end through the real interaction paths: a placed sleeper item keeps its gear and pose; a sleeper in a niche steps out when it wakes; hearing (animals and wool don't wake; a placed sleeper hears like any other); a wake spawns a mob with its gear facing the noise, in `#minecraft:undead`, `#minecraft:skeletons` and `#woken_undead`; a refused spawn keeps the sleeper; robbing order and its wake chance; silk-touch mining gives the sleeper item, and bare hands, no silk touch or silk touch on another tool leave remains; the monster-cap rule.
- `SleeperNoiseGameTest`: the disturbance meter meets each owner target deterministically: sprinting circles and passes wake, two blocks broken or placed wake, jumps, fights and explosions are loud, walking at 3 wakes within two seconds, walking at 7 or sprinting at 11 doesn't, sneaking at 2 is silent (then walking the same path wakes), small noises settle, solid walls muffle, awaken wakes near neighbours and one awaken event wakes at most the cap, chain-woken undead don't infight, only a survival waker is targeted, retaliation against undead stays vanilla.
- `LootDisplayGameTest`: loot on display keeps its item through save/load and templates; an empty hand takes it per placement (ground, wall, stuck; water stays); quiet taking doesn't wake a sleeper but digging buried loot does (a shovel's durability, an empty hand can't dig, digging counts louder); breaking drops the item; the display command places it; woken zombies and skeletons take a displayed weapon or bow over a loose one.
- `GrabLootGameTest`: the waking grab takes any item when no weapon is in reach; the idle grab goal walks to displays and loose items (zombies and skeletons), prefers a display over a closer loose item, ignores displays when the hand is full or the mob isn't in the tag (one test), ignores displays while chasing and goes back for them when idle; contact pickup while chasing stays vanilla's.
- `UndeadCombatGameTest`: fist streaks shred armor and a pause resets them, Smite kills only our undead outright, skeletons shrug off arrows, zombie headshots hurt more, idle zombies drift together and a zombie with a target doesn't (checked once it reaches its target).
- `ZombieGaitGameTest`: the shamble's drag-and-lurch covers the same ground as a steady walker of the same speed.
- Client (`ClientTests` names `sleepers`, `displays`, `gait`; screenshots): sleepers from several angles, sleeper items, a placed sleeper, a woken armored zombie, loot on display; the gait cycle from the side and front and the client's motion lag.
