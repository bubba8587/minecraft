<!-- dte:C1,B15,B17,B18,B19,D2 -->
<!-- covers: docs/research/undead-*, docs/research/camps-*, mods/bubbas_reworked_mobs/* -->
# Bubba's Reworked Mobs, part 1: undead (spec)

Part 1 of the C1 reworkedMobs direction (mod id `bubbas_reworked_mobs`; later parts have their own specs). Everything here is fluid: change it when a better or faster way turns up.

Background research:
- `docs/research/undead-prior-art.md`: nothing on any loader does "dormant until disturbed"; the closest mechanic is vanilla 26.3's copper golem statue.
- `docs/research/undead-feasibility.md`: 26.3 classes, code sketches, performance budget.

## Dormant undead are real blocks

Dormant undead ("sleepers") are real blocks, not entities or display-entity pseudo-blocks. They can be mined into items (see Mining and placing sleepers), cost nothing to tick in large numbers, and never count against mob caps. Waking one replaces the block with a real mob. This is a technical choice, so it lives here rather than in the tree; B15 fewNewBlocks is satisfied because the owner gave the reason for these blocks.

- **Model:** vanilla's copper golem statue: block + block entity, **no ticker**.
  - Blockstate: `facing`, `pose`, `waterlogged` (plus `resting`, see `specs/graveyards.md` "The resting dead").
  - Block entity: the gear as the vanilla `minecraft:container` component, a lazy gear loot table (rolled on first interaction or wake), a wake cooldown and who woke it.
- Lying sleepers are one block with an overhanging model (collision stays one block).
- **Code layout:** one block class, one block entity type and one renderer serve every variant. A variant is one `UndeadVariant` entry (block + item `<name>_sleeper`, entity `woken_<name>`) plus one entry in the renderer's client table.

### Rendering

- **Now (placeholder art):** the block entity renderer draws the whole sleeper with the vanilla mob model and texture, one baked model per pose.
  - Gear uses vanilla's own `HumanoidArmorLayer` and `ItemInHandLayer` (they only need a `RenderLayerParent`), so armor, trims, dye and glint come free.
  - Poses are part rotations applied after the vanilla model's `setupAnim` (`SleeperPoses`, the pose table), so armor models pose identically.
  - The block's own model is particle-only.
- **Planned before release (hybrid):**
  - The body becomes a baked block model per pose (part of the chunk mesh, free per frame; 26.3 block models allow free per-element rotation), with our own 16x/32x body textures.
  - A block entity renderer draws only armor and held items, only when the sleeper has gear and is within 32 blocks.
  - Pose models and renderer poses are generated from one Java pose table by datagen so they never drift apart.
  - Armor in the renderer needs one client accessor mixin for the equipment asset manager.
- **Item icons (owner):** vanilla's own mob head item models: `minecraft:special` with the `minecraft:head` model, base `item/template_skull` and vanilla's skull transform, generated in `ModDataGenerator`.
  - Skeleton and zombie use their own head kinds.
  - Husk: the zombie head with texture override `minecraft:zombie/husk`. Stray: the skeleton head with `minecraft:skeleton/stray`. Drowned: the zombie head with the drowned texture. The override resolves to `textures/entity/<path>.png`.
  - No texture of our own, so nothing for `docs/art-todo.md`.
- **Outer layers:** `PosedHumanoid` takes an optional outer layer (model layer and texture), posed like the body: drowned `ModelLayers.DROWNED_OUTER_LAYER` + `drowned_outer_layer.png` (seaweed hair and rags), stray `STRAY_OUTER_LAYER` + `stray_overlay.png` (ragged clothes).

## Hearing and waking

- **Listening:** the block entity implements `GameEventListener.Provider` (registered automatically for loaded full chunks, like sculk sensors), so hearing costs nothing between events. Vanilla vibration rules apply: sneaking silences steps, wool blocks, carpet muffles.
- **A disturbance meter, on the warden's model.** Owner's brief: "players have to do SOMETHING to wake them ... and not trigger them from a totally different hallway", but "don't make it too easy to get by them when being reckless".
  - Each heard noise adds points to the sleeper's meter: points x distance falloff x wall penalty x `sensitivity`.
  - The meter decays steadily.
  - At the wake threshold the sleeper wakes after a travel delay (a block tick of `floor(distance)` ticks); then the block goes and the real mob spawns with the same gear, facing the source.
  - No dice: the same noise always does the same thing.
- **Lazy decay, no ticker:** the meter is stored with the game time of its last change (block entity NBT `disturbance`, `disturbance_time`) and decayed on read.
- **Stirring:** crossing the stir threshold upward plays the mob's ambient sound quietly and low (volume 0.35, pitch 0.6-0.7) from the block, as the player's warning. Cheap: one sound, only on the crossing.
- **Chain waking:** a wake emits a custom `bubbas_reworked_mobs:awaken` game event. Other sleepers hear it through the same path (wool included). At most 4 wake per event (prior art capped cascades at 4), nearest first (`BY_DISTANCE` delivery with a budget set around the synchronous dispatch).
- If another mod cancels the spawn, the block stays (nothing is lost).

### The noise table

All values in `UndeadTuning` (starting values, owner to tune); code in `undead/sleeper/SleeperHearing` and `SleeperListener`.

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
| Ignored | see the list below | spectators, sneaking, non-targetable sources |

Ignored by sleepers:
- spectators;
- `dampensVibrations` sources;
- sneaking, for `#ignore_vibrations_sneaking` events (steps, landings, item use, shooting);
- `#dampens_vibrations` on the affected block (carpets);
- the sleeper's own block;
- sources in `#bubbas_reworked_mobs:does_not_wake_sleepers` (default `#minecraft:undead`, so a woken horde shuffling past doesn't cascade by footsteps; only awaken events chain);
- animals (mob category other than monster or misc), except when they are hurt or killed (a fight is loud);
- a sleeper with `sensitivity` 0.

### What it adds up to

Each of these is a deterministic game test in `SleeperNoiseGameTest`, at vanilla speeds (walking 0.216 blocks a tick, sprinting 0.28, a step every ~1.7 blocks):

- Sprinting in circles 6 blocks out wakes it in ~1.2 s (test: <= 30 ticks).
- Sprinting straight past 6 blocks out wakes it before the player is 3 blocks past.
- Walking right past 3 blocks out wakes it within ~2 s (test: <= 50 ticks from 6 blocks before).
- Walking past 5 blocks out stirs it (peak ~85) without waking, and it settles to 0 within 10 s.
- Walking past 7 out (peak ~28) or sprinting past 11 out doesn't wake it.
- One footstep at 5 blocks is ~20 points.
- Two blocks broken or placed 4 blocks away wake it; one doesn't.
- Three jump landings 3.5 blocks away wake it; two don't.
- Two hits on a pig 3 blocks away wake it.
- An explosion 8 blocks away wakes it at once; one 15 away does nothing.
- Sprinting past 5 out behind 2 blocks of stone doesn't wake it (in the open it does).
- One awaken event wakes default sleepers 2 and 5 blocks away, but not one 10.5 away.
- Sneaking past 2 out is silent.

Also:
- Walking slowly round a sleeper at 3 blocks still wakes it within ~10 s (the meter gains ~3 per step net). At 8+ blocks, walking never adds up.
- Opening a chest right next to a sleeper (80) plus a few steps wakes it; the player has to sneak to loot.

### Creative mode, sensitivity, the woken mob

- **Creative mode** (the owner tests in creative): vanilla's `Player.getMovementEmission` emits steps and landings for any player on foot, creative included, and none while flying (`abilities.flying`). A creative player's steps wake sleepers like a survival player's (tested), but a woken undead never targets a creative or spectator player. A player's landing is checked from its movement packets (`doCheckFallDamage`) and heard on the server like any step.
- **Sensitivity:** each sleeper has a `sensitivity` (block entity NBT, default 1) that multiplies every noise: 0 never wakes (decor, warded), large always wakes (traps). Strong light and keep-asleep areas (the `api.Sleepers` hook, used by Totems & Artifacts' full-pyramid wards) scale it at read time (see Calming sleepers).
- Robbing keeps its own per-theft chance (see Robbing), multiplied by `sensitivity`.
- **The woken mob** is persistent (never despawns, outside the mob cap), keeps the sleeper's custom name and always drops its gear. One that wakes empty-handed grabs what's lying within 3 blocks, weapons first (see "Grabbing what's lying around").
- **No wake-up violence** (owner: "the sleepers fight each other when they wake up!"). A woken mob takes its waker as its target only if the waker is a survival or adventure player. The awaken event's source is the mob that just woke (and footsteps can come from other mobs), so without this rule chain-woken undead targeted each other. A sleeper woken by a neighbour, a villager, a creeper or anything else wakes without a target and finds players with its normal AI.
  - **Retaliation stays vanilla** (owner: "undead can retaliate against arrows in the traditional vanilla sense"): a woken zombie hit by a skeleton's stray arrow may fight back through vanilla's `HurtByTargetGoal`.
  - Tests: `chainWokenUndeadDoNotFightEachOther`, `onlyASurvivalWakerIsTargeted`, `retaliationAgainstUndeadStaysVanilla`.

## Waking in tight spots

A sleeper lying somewhere lower than its mob is tall (a crypt wall niche under a slab, a shallow grave) would wake into a box overlapping blocks: not suffocating (its eyes clear a half slab) but stuck. Crypt niches are two blocks deep, so a lying sleeper (2 long, centred on its block) would otherwise lie half in the wall.

- `SleeperWaking.stepOutIfStuck` checks the mob's full bounding box against blocks only (`noBlockCollision`, so other mobs don't make a spot look blocked).
- If it's stuck, it moves to the nearest free spot with ground under it within 2 blocks, nearest first (`BlockPos.withinManhattan`, down included): beside it on the same level first (a niche's open side), then one up, then diagonals. With none, it stays.
- Milestone test: `BubbasReworkedMobsGameTest.sleeperInANicheStepsOutWhenItWakes`.

## Robbing

- Using a sleeper with an empty hand takes one item: held weapon first (main hand, then off hand), then armor from the top down.
- Wake chance per theft: weapon 10 %, armor 30 %, both multiplied by the sleeper's sensitivity.
- A failed roll wakes it 2 ticks later, angry at the thief.
- Opening a nearby chest makes the normal container-open noise, which sleepers hear.

## Mining and placing sleepers

- **Silk Touch pickaxe** (owner's call): gives the whole mob as an item (its "statue": a sleeper item of that undead type, no gear on it, pose kept); its armor and weapons drop as separate items. The tool is the item tag `#bubbas_reworked_mobs:sleeper_harvest_tools` (pickaxes) plus Silk Touch.
- **Anything else** (no Silk Touch, another tool, bare hands): leaves remains (a bone or rotten flesh) and the gear.
- The gear drops from the block entity whenever the block goes (mining, explosions, commands), except on waking, when the mob takes it.
- Sleeper items can be placed back as sleepers, and placed sleepers can come alive like generated ones (decor, traps).

### Sleeper items

- One item per mob kind: `skeleton_sleeper`, `zombie_sleeper`, `husk_sleeper`, `drowned_sleeper`, `stray_sleeper`; class `undead/sleeper/SleeperItem` (a BlockItem). One per kind because the kind is the block, and vanilla maps block to item one to one (loot, pick block, item models with the mob head icon).
- **The form** is the vanilla `minecraft:block_state` component: `pose` (`sitting` | `lying` | `prone`) and `resting` (`true` = statue, inert).
  - Vanilla BlockItem applies it on placing, `copy_state` loot copies it, and `SleeperBlock.getCloneItemStack` (pick block) copies both.
  - Gear stays in `minecraft:container`.
  - Chosen over a custom component because every vanilla path already understands it (B9).
- **Names** (`SleeperItem.getName`): "<base> (<pose>)", e.g. "Sleeping Zombie (Lying)", "Skeleton Statue (Lying)". Lang keys `item.bubbas_reworked_mobs.sleeper.form`, `...sleeper.pose.<pose>`, `block.bubbas_reworked_mobs.<kind>_sleeper.statue`. An anvil name wins.
- **Creative tab:** every kind in every pose (sitting, lying, face down), sleeping and statue, plus the gravestone.
- **Tooltip:** sleeper items carry a default `minecraft:tooltip_display` that hides `minecraft:block_state`, so the raw "pose: lying" / "resting: true" lines don't show (the name already says the form).

### Face-down sleepers

- `lying` is on the back; `SleeperPose.PRONE` (blockstate value `prone`, after `LYING`) is face down. Lang `item.bubbas_reworked_mobs.sleeper.pose.prone` = "Face Down" ("Sleeping Skeleton (Face Down)", "Skeleton Statue (Face Down)").
- Every `UndeadVariant` poses list ends with PRONE, so each kind's default (first) pose is unchanged. `SleeperPose.isFlat()` (lying or prone) drives the collision shape (the same 16x5 slab as lying) and BackToSleep's facing pick.
- **Client** (`SleeperPoses`): the prone placement is the lying placement turned 180 degrees about the body's long axis (same footprint and height, face to the floor). Part pose: head turned to the side (cheek down), both arms at the sides, the weapon arm twisted a quarter turn so a held weapon lies flat beside it, legs slightly apart.
- **Where they appear** (`UndeadTuning`):
  - `CRYPT_FACE_DOWN_SHARE = 0.3` of the lying sleepers in crypt niches and noble halls (every crypt `Builder.sleeper` call with LYING);
  - `SHIPWRECK_FACE_DOWN_SHARE = 0.4` of the lying shipwreck crew;
  - `RESTING_FACE_DOWN_SHARE = 0.15` of the resting dead in yard graves.
- **Tests:** `SleeperFormsGameTest.everyFormSurvivesPickUpAndPlace` loops over each kind's poses, prone included; milestone `SleeperFormsGameTest.faceDownSleepersAreNamedAndWake` (names, and a face-down zombie hears an explosion and wakes); client part `prone` (`ProneSleepersClientGameTest`: `prone-row`, `prone-low`, `prone-close`, `prone-armoured`, `prone-tooltip`).

## Undead variants

Owner's design:
- **Husks** (deserts) and **strays** (snow) are sleepers too, for crypts and camps. Husks lie buried in sand and burst out in a puff of sand that briefly blinds (particles plus a short effect). Strays lie frozen in ice or packed snow that cracks as a warning before they break free.
- **Bogged are not sleepers** (owner): they act like the reworked drowned (`specs/drowned.md`): lurking in swamp water and mud, moving unseen, ambushing from the reeds.
- **Drowned sleepers** only in shipwrecks (see Drowned sleepers in shipwrecks). Sunken graveyards: no (owner).
- Resting dead in yard graves never wake (`specs/graveyards.md`).

### Husks and strays

- **Husk sleepers** ("Sleeping Husk") lie buried in sand. When woken, the sand over and round them bursts in a cloud of sand and dust; any survival player within 4 blocks is **blinded for 2 seconds**. Woken husks don't burn by day, and their bare-handed hits give Hunger (as vanilla husks).
- **Stray sleepers** ("Sleeping Stray") lie frozen in ice (clear ice in crypt niches, so players can see them inside). When something wakes one, the ice **cracks first** (a glassy crack sound and ice shards: the warning) and the stray breaks free **1.5 seconds later**, shattering the ice round it. Woken strays shoot Slowness arrows (as vanilla strays) and wear the stray's ragged clothes.
- **Desert and snowy graveyards:** crypt yards take their climate from the biome (`specs/graveyards.md` "Desert and snowy yards"):
  - Desert (desert, badlands): husks in the crypts (70 % husks, 30 % skeletons), buried under sand in the wall niches, and resting husks in the yard graves.
  - Snowy (base temperature under 0.15): strays in the crypts (70 % strays, 30 % zombies), frozen in clear ice in the niches, and resting strays in the graves.
  - Everywhere else: skeletons and zombies.
  - Grave plots are coarse or rooted dirt in every climate (owner: no sand, snow or mud on grave tops).

### Bogged

A bogged lurks instead of sleeping:
- It notices a player within 32 blocks and slips to a hiding spot near them: water 1-2 deep over a floor (2 deep hides it fully), or mud beside reeds (sugar cane, tall grass, ferns). There it crouches, still, and holds its fire.
- While nobody looks toward it, it moves to a new spot now and then: about every 30 s, or every few seconds once the player has moved more than 12 blocks from its spot, so it follows.
- It **ambushes** when a player within 10 blocks turns their back on it (it's more than 100 degrees from where they look), when anyone comes within 4 blocks, or when it's hit. Then it fights normally (poison arrows) and goes back to hiding 10 s after losing its target.
- Bogged from trial spawners stay vanilla.

### Numbers (starting values, owner to playtest)

| What | Value | Where |
|---|---|---|
| Husk burst blinds within | 4 blocks | `UndeadTuning.HUSK_BLIND_RADIUS` |
| Blindness | 40 ticks (2 s) | `UndeadTuning.HUSK_BLIND_TICKS` |
| Stray crack warning | 30 ticks (1.5 s) | `UndeadTuning.STRAY_CRACK_TICKS` |
| Desert crypt sleepers | 70 % husk, 30 % skeleton | `YardKinds.Climate.crypt` |
| Snowy crypt sleepers | 70 % stray, 30 % zombie | same |
| Bogged notice range | 32 blocks | `BoggedTuning.NOTICE_RANGE` |
| Hiding spot distance from player | 5-14 blocks (best 8), within 24 of the bogged, ±6 in height | `BoggedTuning.SPOT_*` |
| Ambush: back turned within / too close | 10 / 4 blocks; "back turned" = more than 100° off their view | `BoggedTuning.AMBUSH_RANGE`, `TOO_CLOSE`, `BACK_TURNED_HALF_ANGLE` |
| Moves while unseen (nobody within 70° looking) | 1 in 600 a tick (~30 s); 1 in 80 (~4 s) once the player is 12+ blocks from its spot | `BoggedTuning.MOVE_ONE_IN*` |
| Back to hiding after a fight | 10 s without a target | `BoggedTuning.HUNT_LINGER` |

### Builder's choices

(The two the owner may want to flip are under Open.)

- Resting dead in desert/snowy yard graves are resting husks/strays (`YardKinds.Climate.resting()`).
- Snowy crypts mix in **zombies** and desert crypts mix in **skeletons**, so each crypt still has both melee and archers.
- Stray niches use **clear ice** (not packed ice) so players see the stray frozen inside. Ice can melt next to strong light, but crypts are candle-lit (owner: not too well lit), so it stays.
- The bogged hides fully underwater in 2-deep water (its shape is just visible).
- The bogged's lurk mind is not saved: after a reload it finds a spot again.
- What a husk or stray breaks out of: the block over the sleeper (and two up), its four sides, and the four sides of the block over it (so a lying one's foot end is freed). Only sand-like blocks (sand, red sand, sandstone slab, suspicious sand) for husks and ice-like blocks (ice, packed/blue ice, snow block, powder snow) for strays.

### Code

- `UndeadVariant` has HUSK, STRAY and DROWNED (block, item, woken entity each). `isSkeleton()` = SKELETON or STRAY (bows, robbing weapon choice in crypt gear).
- `WokenHusk extends WokenZombie` (no sun sensitivity, husk sounds, Hunger on bare-handed hit); `WokenStray extends WokenSkeleton` (stray sounds, Slowness arrows).
- Client: `WokenZombieRenderer(context, texture)` and `WokenSkeletonRenderer.stray(context)` (stray body/armor layers + `SkeletonClothingLayer`); `SleeperRenderer` has HUSK/STRAY posed models.
- Tags: woken husk/stray are in `#woken_undead`, `#grabs_loot`, `#zombies`/`#skeletons`. `#burn_in_daylight` lists skeleton, zombie, stray explicitly (not the whole woken tag), so husks don't burn.
- Breakout: `undead/sleeper/SleeperBreakout`. `breakOut` runs in `SleeperWaking.wake` before the mob is added; `crack` runs in `SleeperBlockEntity.scheduleWake` for strays and raises the delay to at least `STRAY_CRACK_TICKS`.
- Graveyards: `YardKinds.Climate` (TEMPERATE/DESERT/SNOWY) from the biome at the yard's centre in `CryptYardStructure`; saved in the piece tag as `Climate`; `CryptYardPiece.climate()`. `Build.encase` puts sand/ice over a husk/stray crypt niche; `Build.resting(…, variant, …)` places a yard grave's resting dead.
- Bogged:
  - `bogged/BoggedLurk`: a state machine IDLE → TRAVEL → LURK → HUNT, one mind per bogged in a WeakHashMap. Numbers in `bogged/BoggedTuning`.
  - `mixin/BoggedLurkMixin`: `Mob.customServerAiStep` HEAD, instanceof Bogged.
  - `MobFinalizeSpawnMixin` tags spawner bogged `bubbas_reworked_mobs.vanilla_bogged` so they stay vanilla.
  - `BoggedLurk.hideAt(level, x, z, aroundY)` scans down from 6 above the player's feet (not the heightmap, which mangrove canopies would stop).

### Tests

- `UndeadVariantsGameTest`: `huskBurstsOutOfSandAndBlinds` (core), `strayCracksItsIceBeforeBreakingFree` (still frozen 20 ticks in, free by 40), `boggedLurksWhileWatchedAndAmbushesAnAvertedBack` (milestone).
- The `GravesGameTest` yard core test expects the yard climate's resting kind.
- Client part `variants` (`VariantsClientGameTest`), screenshots: `variants-sleepers` (husk and stray sleepers sitting and lying), `variants-buried` (sand over a husk, clear ice over a stray), `variants-husk-burst` (sand cloud, husk rising out of its trench), `variants-stray-crack` (ice shards, still frozen), `variants-stray-free` (ice shattered, stray standing), `variants-woken` (woken husk and stray, the stray's ragged clothes showing), `variants-bogged` (one bogged crouched under a reedy pool's surface, one standing on the bank).
- Not shot: a naturally generated desert/snowy yard (covered by the server tests and the climate code; worth a look in the next test world tour).

## Drowned sleepers in shipwrecks

Owner's design: the ship's drowned crew, lying or sitting **inside** shipwreck interiors (cabins and hold), waking like other sleepers. They must be inside the boat, not outside on the sand (owner).

- **Drowned sleeper** (creative: "Sleeping Drowned"). Underwater sleepers are already waterlogged (`SleeperBlock.WATERLOGGED`).
- **Placement:** each empty or flooded spot on a wooden floor with wood overhead within 4 blocks has a 12 % chance of a sleeper. Full wrecks get a crew of several (a full ship in the test world had 7); broken fragments with little room inside mostly have none.
- **Gear:** 15 % a trident, 15 % a fishing rod, 10 % a nautilus shell in the off hand, 25 % a leather cap.
- **Woken drowned are vanilla** (owner): a drowned sleeper wakes as a plain vanilla `minecraft:drowned`, flagged vanilla like spawner drowned (`DrownedAccess.brm$makeVanilla`, saved under `VANILLA_KEY`), so none of the drowned rework applies. Gear and name are kept, and it targets by vanilla rules only (owner: one waking above water by day may stay confused).

| What | Value | Where |
|---|---|---|
| Chance per spot between decks | 12 % | `UndeadTuning.SHIPWRECK_CREW_CHANCE` |
| Sitting (if there's room above) | 30 % | `ShipwreckCrew.place` |
| Trident / rod / shell / cap | 15 % / 15 % / 10 % / 25 % | `ShipwreckCrew.place` |

Code:
- `UndeadVariant.DROWNED`: block, item, and woken entity type `woken_drowned` (`WokenDrowned extends Drowned`). Tags `#woken_undead`, `#grabs_loot`, `#zombies`, `#burn_in_daylight`; particle texture prismarine; vanilla `DrownedRenderer`.
- `WokenDrowned` is a one-tick shim: on its first server tick it converts itself (`Mob.convertTo`, keeping equipment) into a vanilla drowned. Old saves holding a woken drowned convert the same way when they load.
- `mixin/ShipwreckCrewMixin` runs at every RETURN of `ShipwreckPieces.ShipwreckPiece.postProcess` (it has two exits; TAIL alone missed every chunk after a wreck's first) and calls `undead/ShipwreckCrew.place` over the piece box clipped to the chunk.
- Spots: `ShipwreckCrew.inHull` = air or water, sturdy wooden floor, wood within 4 above. It checks one column only, so every chunk sees the same answer (a sideways check across chunk edges would read unbuilt neighbours).
- Per-spot roll from vanilla's positional random (`XoroshiroRandomSource(seed).forkPositional().at(pos)`, seed = world seed ^ piece centre).

Tests:
- Milestone `ShipwreckCrewGameTest`: `shipwrecksHaveADrownedCrew` (a vanilla full shipwreck piece built as worldgen builds it has drowned sleepers), `drownedSleeperWakesAsADrowned`.
- Client part `shipwrecks` (`ShipwreckClientGameTest`): checks the nearest natural shipwrecks of seed 20261116 and fails if any crew sleeper isn't between decks (one full wreck had 7, all inside; fragments 0). Screenshots: `drowned-compare` / `-close` (drowned sleepers sitting and lying beside a vanilla drowned, same look), `shipwreck-sleeper-0..2` (crew below deck). The variants part's `variants-sleepers` shows the stray sleepers with their clothes.

## Back to sleep at dawn

- Woken skeletons, zombies, husks and strays that were awake during the night and are still about at sunrise, with nothing to fight, go back to sleep. They walk to the nearest place out of the sun within **16 blocks** (4 up or down) and become a sleeper block again, keeping everything they carry (gear, picked-up loot, name).
- **Owner's rule: an undead can always sleep by sitting down in a pocket where it's safe to stand.** A place is any spot on a solid floor with a roof over it (something that blocks light: glass and barriers don't count) that is dim (block light 7 or less: a candle-lit crypt is fine, a torch-lit room isn't):
  - room to stand: it **sits** down there;
  - a one-high niche (a crypt wall niche): it **lies** down, from beside it (it has no room to step in first);
  - an **open grave** (a pit walled on three sides), roof or not: it lies down.
- Crypts refill themselves, and a cleared graveyard quietly fills up again if survivors are left.
- Only undead that saw the night: those woken by day (underground, say) don't go straight back to sleep. The resting dead are not involved.
- A soft sound (the sleeper's place sound) plays as it settles.

| What | Value | Where |
|---|---|---|
| Search range | 16 blocks across, 4 up/down | `UndeadTuning.SLEEP_SEARCH_RANGE`, `SLEEP_VERTICAL_RANGE` |
| Searches again every | 3 s (+ up to 1 s) | `UndeadTuning.SLEEP_RESEARCH_TICKS` |
| Dim enough | block light <= 7 | `UndeadTuning.SLEEP_MAX_LIGHT` |
| Walk speed there | 1.2x | `BackToSleep` |

Code:
- `undead/sleeper/BackToSleep` is a vanilla `MoveToBlockGoal` (vanilla subsystems over hand-rolled scans): the nearest-first search, throttling and walking are vanilla's.
  - `isValidTarget` = the floor under a valid place (`BackToSleep.pose`); it lies down when within 1.6 blocks (`acceptedDistance`).
  - Registered at priority 1 in `WokenZombie` and `WokenSkeleton` (so husks and strays too).
  - Runs only by day (`isBrightOutside`, overworld-like dimensions), with no target, not riding or leashed.
- "Saw the night" is the entity tag `bubbas_reworked_mobs.saw_night`, added in `customServerAiStep` while it's dark (`BackToSleep.noteNight`), so it survives a reload.
- Lying down: `BackToSleep.lieDown` places the variant's sleeper block (pose from the place, facing the open side for a niche, waterlogged in water), copies every equipment slot and the custom name into the block entity, then discards the mob.

Tests (milestone, `DawnGameTest`):
- `survivorsGoBackToSleepAtDawn`: at noon a woken zombie that saw the night lies down in a stone niche, as a sleeper with its helmet and shovel.
- `onlyThoseWhoSawTheNight`: one woken by day stays up.
- `sitsDownUnderAnyCover`: under a plain stone canopy with standing room it sits down.
- No screenshots (no new visuals: the sleeper poses are the existing ones).

## Fire as a weak ward

- A **lit campfire or soul campfire, fire or soul fire** keeps hostile mobs that fear fire about **6 blocks away** (counted along the ground's grid, up to 3 up or down): an idle zombie, skeleton, creeper or spider that comes near walks off again. An unlit campfire does nothing.
- It's a **weak ward**, a deterrent and not a wall, like a small, soft version of the Totem of Warding (`specs/totems-artifacts.md`).
- **Only mobs that would fear fire** (owner): goal-driven hostile mobs that fire hurts. **Never** fire-immune mobs (blazes, ghasts, magma cubes, wither skeletons, zombified piglins and the like), never the Nether natives that fire would hurt (piglins, piglin brutes, hoglins, zoglins), and not the warden.
- *Builder's choice:* **a mob already hunting someone pushes straight through.** Only mobs with nothing to fight keep off.
- Camps generate with an unlit campfire, so they don't ward themselves (and a lit fire also quiets a camp).
- Light already makes sleepers harder to wake (Calming sleepers), so a campfire also helps keep a crypt asleep.

| What | Value | Where |
|---|---|---|
| Keep-away distance | 6 (Manhattan, across) | `UndeadTuning.FIRE_WARD_RADIUS` |
| Up and down | 3 | `UndeadTuning.FIRE_WARD_VERTICAL` |
| Checked | every half second per mob (spread by entity id) | `FireWard.canUse` |
| Walks off to | a spot up to 9 away from the fire, at 1.1x speed | vanilla `DefaultRandomPos.getPosAway` |

Code:
- Built the way vanilla piglins and hoglins avoid their repellents (`specs/vanilla-subsystems.md`):
  - block tag `#bubbas_reworked_mobs:undead_repellents` (campfire, soul campfire, fire, soul fire);
  - a campfire counts only while lit (`CampfireBlock.isLitCampfire`, as `PiglinSpecificSensor.isValidRepellent`);
  - the nearest is found with `findBlocksInBoxByManhattanDistance(pos, 6, 3).filterState(FireWard::repels).findFirst()` (as `HoglinSpecificSensor`);
  - the mob walks to `DefaultRandomPos.getPosAway` from it.
- `undead/FireWard` is a goal (priority 1, MOVE flag), added in `Mob`'s constructor by `mixin/FireWardMixin` to every server-side mob for which `FireWard.fearsFire` holds: `Enemy`, `PathfinderMob`, not `fireImmune()`, not in entity tag `#bubbas_reworked_mobs:unafraid_of_fire` (piglin, piglin brute, hoglin, zoglin, warden). Runs only with no target.
- A goal and not a brain sensor because the mobs it covers (zombies, skeletons, creepers, spiders...) are goal-driven. Brain-driven hostile mobs (breeze, creaking and the like) don't get it; worth an owner call if any of those should.
- The tag is named `undead_repellents` although it repels every fire-fearing hostile mob, not only undead.

Tests (milestone, `FireWardGameTest`):
- `idleMobsKeepAwayFromALitCampfire`: an idle zombie 2 blocks from a lit campfire walks off to 4+ blocks.
- `onlyLitFireRepels`: lit campfire, lit soul campfire, fire and soul fire repel; an unlit campfire doesn't.
- `onlyFireFearingMobs`: zombie, skeleton, creeper, spider yes; blaze, wither skeleton, piglin, hoglin, zombified piglin, magma cube, warden, cow no.

## Loot on display

Weapons near sleepers, and other loot, are shown with a dedicated block rather than as dropped items or item frames (owner's choice: it looks better than grabbing swords off item frames, and later loot systems reuse it). It is a deliberate new block under B15 fewNewBlocks; the owner gave the reason.

- One block holding exactly one real item (any item; weapons and tools look best; armor later if useful) in a block entity. It lives in Bubba's Reworked Mobs' shared code (`display/` package, renderer in `client/display/`), not under undead; it may move to a loot mod later.
- Worldgen and structures place it next to sleepers, half-buried in graves, hung on crypt walls.

### Block

- Block `bubbas_reworked_mobs:loot_display` (`LootDisplayBlock`) + block entity (`LootDisplayBlockEntity`): no ticker, no block item, no loot table.
- The item is saved as a one-slot `Items` list (like a container) and carried as the vanilla `minecraft:container` component, so it survives save/load, structure templates and `/setblock ... {Items:[{Slot:0b,id:"minecraft:iron_sword",count:1}]}`. Middle-click (creative) gives the displayed item.
- Blockstate: `placement`, `facing`, `waterlogged`. More placements can be added.
  - `placement`: `ground` (lying flat), `buried` (mostly underground, grip showing), `wall` (hung flat against the wall it's attached to), `stuck` (planted point-down).
  - `facing` is the side the item shows its front to: for `wall`, away from the wall it hangs on; for the floor placements, toward whoever placed it (a `ground` item lies with its grip toward `facing`, point away; a `buried` grip leans toward `facing`).
- Shapes (outline only; there is **no collision**: players walk over and through displays): `ground` a 14x1.5 px slab, `buried` a 6x5 px stub, `stuck` a 6x13 px post, `wall` a 12x12x1.5 px plate on the wall.
- Support: floor placements need a centre-sturdy floor and `wall` a sturdy wall behind (like torches); when that goes, the block pops and drops the item. Pistons pop it too.

### Rendering

`LootDisplayRenderer` draws the item's own model, so any item (vanilla or modded, any resource pack) looks right with no extra art.
- The item is resolved through `ItemModelResolver` in the `FIXED` display context (what item frames use) at 0.75 scale, so a sword is about 0.93 blocks long.
- Per placement:
  - `ground`: flat on the floor, with a small per-position turn (up to 15 degrees) so rows don't look stamped;
  - `buried`: point-down, sunk so only the top 0.4 block shows, leaning 25 degrees;
  - `stuck`: upright, point 0.1 block into the ground;
  - `wall`: flat against the wall like a frameless item frame.
- The block model is invisible (particle only: iron block).
- Cost: one item per block, no stack copies; the render state is created per frame as in vanilla's shelf and item frame.
- Poses assume a corner-to-corner tool/weapon sprite (all vanilla swords, axes, tools, spears, bows, the trident's sprite); other items still render, posed the same way.

### Taking and breaking

`LootDisplays`:
- **Empty main hand** takes `ground`, `wall` and `stuck` displays: pickup sound and the quiet `minecraft:item_interact_finish` game event, which vanilla ignores while sneaking, so sneaking makes taking a displayed item silent (owner confirmed). Holding anything else does nothing to the display.
- **Buried** ignores an empty hand (an action-bar hint says to dig) and needs a shovel (`#minecraft:shovels`). Digging costs the shovel 1 durability and plays the ground's break sound plus `minecraft:block_destroy`, which sleepers hear three times as well as the quiet event (0.3 vs 0.1 base).
- **Breaking** drops the item. Other placements break at once like a flower; `buried` mines like packed dirt (hardness 0.8, fast with a shovel; the block is in `#minecraft:mineable/shovel`). A waterlogged display leaves its water behind.

### Grabbing what's lying around

Owner: "all weapon-carrying mobs should grab whatever's lying around". Shared code in the `loot/` package: `LootGrabbing` holds the rules and the take helpers both paths use; `GrabLootGoal` is the idle behaviour.

- **Who:** any mob in the entity tag `#bubbas_reworked_mobs:grabs_loot` (woken skeleton, zombie, husk, stray and drowned now; other weapon-carrying mobs as later parts rework them). It takes **any** item, not only weapons, so loot rooms and dungeons can put specific items in their hands.
- **Tags** (datagen): `#bubbas_reworked_mobs:grabs_loot` (entity types) and `#bubbas_reworked_mobs:preferred_on_wake` (items: `#minecraft:swords`, `#minecraft:axes`, `#minecraft:spears`, bow, crossbow, trident, mace; vanilla has no tag for the last four).
- **On waking** (`LootGrabbing.grabOnWake`, called by `SleeperWaking` after the mob is added, empty main hand only): the nearest `#preferred_on_wake` display within 3 blocks, else the nearest such loose item, else the nearest display of anything, else the nearest loose anything. One item goes into the main hand and always drops on death; a skeleton re-picks bow or melee attack through vanilla's `onEquipItem`. Runs after the mob is added, so a refused spawn takes nothing.
- **While idle** (`GrabLootGoal`, MOVE flag, priority 5 on our mobs: below their attack goals, above strolling):
  - Runs only with an empty main hand, **no target**, not riding, and the mob in `#grabs_loot` (so a datapack can switch it off, and a later mixin can add the goal to vanilla mobs without touching their behaviour until they're tagged). It stops the moment the mob gets a target.
  - Every 20 ticks (first search at a random offset so mobs loaded together spread out) it looks for the nearest display within 8 blocks, else the nearest loose item, paths there with vanilla navigation and takes it within 1.75 blocks (displays through the same helper as the waking grab, loose items with vanilla's pickup animation, one of a stack).
  - Displays come before loose items; any placement counts (undead don't need shovels); the display block goes.
  - Unreachable targets, and ones it gave up on after 15 s, are skipped for 10 s.
  - While the mob is in the air (just spawned, falling) it doesn't search, since ground navigation can't path then.
- **While chasing a player** it ignores displays and items entirely (the chase wins).
- **Contact pickup stays vanilla's:** woken mobs come out of `create` without vanilla's natural-spawn roll, so their vanilla "can pick up loot" flag is off and they don't scoop items up by touch. With the flag on (vanilla's rule) they do, even mid-chase (tested).
- **Cost:** a search walks the block entities of the loaded chunks in range (`LootDisplays.findNearest`, never loads chunks) plus one entity box query; one path per chosen target, a re-path at most once a second when navigation ends early. Nothing at all for mobs that hold something or have a target.
- **Tests** (`GrabLootGameTest`, real paths): each walking test runs in its own test environment (tests in a batch sit 5 blocks apart, inside the notice range) at night (in daylight they burn and flee the sun).

### Worldgen defaults

Skeleton sleepers get standard bows; zombie sleepers get tools/weapons at vanilla's normal zombie equipment rates. Loot rooms and dungeons will place specific items on display instead.

### Placing displays

- **Command** (ops, permission level 2): `/bubbas_reworked_mobs display <ground|buried|wall|stuck> <item> [<pos> [north|south|east|west]]`. Without a position it goes where the player looks (within 20 blocks): `wall` on the face looked at, the others on the floor looked at, turned toward the player. It refuses a spot that's taken or has nothing to hold the item up.
- **Laying a held item anywhere** (owner: sneak + middle click):
  - **Sneak + middle click** with an item in hand, aimed at a block, lays one of it on the ground as a `GROUND` display: on top of the block aimed at, or in front of a side face, wherever there's a floor. Plain middle click stays vanilla's pick block.
  - Survival and adventure respect spawn protection and adventure rules (`mayInteract`, `mayUseItemAt`), within block reach. In creative it lays a copy and keeps the stack.
  - Picking it up with an empty hand is the normal display rule.
  - Code: `display/HandPlacing`, shared by players and mobs (the cairn courier enderman uses it; `specs/endermen.md`). `lay(level, placer, target, stack)` splits one item, places a facing display, plays the item-frame add sound and emits `BLOCK_PLACE`; `canLayAt` = air or still water over a floor that holds a display. Client mixin `LayItemPickMixin` on `Minecraft.pickBlockOrEntity` sends the C2S payload `bubbas_reworked_mobs:lay_item`; the server checks reach and permissions.
  - Tests: milestone `CairnTradeGameTest.heldItemLaysOnTheGround` (one at a time, not on top of a display, not in mid-air); client part `trading` shot `laid-on-grass` (a plain middle click lays nothing).

## Calming sleepers (light and bells)

The Grave Talisman and the Totem of Warding live in Bubba's Totems & Artifacts (C6; `specs/totems-artifacts.md`: the talisman, the totem's pulse, wards, the totem statue; old saved items load there through registry aliases). What stays here is what calms or wakes sleepers on its own (light, bells) and the hook that mod uses.

Numbers in `UndeadTuning`: `LIGHT_CALM_LEVEL`, `LIGHT_CALM_FACTOR`, `BELL_POINTS`, `BELL_RANGE`.

**Light** (owner: strong light near a sleeper makes it a little harder to wake, like torches on spawners):
- `SleeperBlockEntity.effectiveSensitivity(level)` = stored sensitivity x 0.7 (`LIGHT_CALM_FACTOR`) at block light 12 or more (`LIGHT_CALM_LEVEL`, just above what spawners allow), and x 0 inside a keep-asleep area (`SleepAreas`, below). Used by hearing, bells and robbing.
- Sky light doesn't count. The light lookup only happens for noises in earshot. Placing the torch is itself a noise.
- *Agent's choice:* the step at 12 and the x0.7.
- Test note: the game test server's light engine lags (block light still 0 forty ticks after placing glowstone), so the light test waits up to 600 ticks.

**Bells** (owner: extra loud to sleepers, heard as far as players hear them):
- `BellBlockMixin` (RETURN of `attemptToRing(Entity, Level, BlockPos, Direction)`) calls `undead/sleeper/SleeperBells.rang`, which walks the loaded chunks' sleepers within 32 blocks (`BELL_RANGE`, no chunk loads; `SleeperFinder`) and feeds each through the listener's shared `hear` path (wool, walls, stir, wake, travel delay).
- A ring is 250 points at the bell (`BELL_POINTS`), falling off linearly to 0 at 32: it wakes sleepers within about 19 blocks and stirs them out to about 25. *Agent's choice:* the 250 and the linear falloff.
- Walls and wool muffle a bell like any noise (owner: a surface bell doesn't wake a crypt below).
- Any ring counts (a hit, a projectile, redstone). The listener ignores the bell's own BLOCK_CHANGE vibration so it isn't counted twice.

## Sleeper hook for other mods (`api/Sleepers`)

A small public class, `io.github.bubba8587.bubbas_reworked_mobs.api.Sleepers`, the same idea as Reworked Beacons' payload API. Stable: add methods, don't change them. Its user today is Bubba's Totems & Artifacts (optional both ways: this mod `suggests` it and has no compile dependency on it). The other API class for crypts is `api/Crypts` (`specs/graveyards.md`).

- `int calm(ServerLevel, Vec3 centre, double radius, float points)`: takes points off the disturbance meter of sleepers in range (wake 100; never below 0).
- `void keepAsleep(ServerLevel, Object source, AABB area, int lifetimeTicks)` / `void release(ServerLevel, Object source)`: sleepers inside the area never wake from noise, bells or robbing. Each source's area lapses unless refreshed, so callers need no cleanup hook. Backed by `undead/sleeper/SleepAreas`; `effectiveSensitivity` checks it.
- `boolean isKeptAsleep(Level, BlockPos)`, `boolean isSleeper(Level, BlockPos)`, `double nearestSqr(Level, Vec3, double range)` (client or server).

## Scope and variants

- **Only this mod's undead change:** our own entity types (woken sleepers and the rare natural spawns), tagged as undead. Vanilla zombies and skeletons elsewhere (other mods' spawners and structures) stay vanilla, and AI or animation mods that target vanilla classes don't override ours.
- **Built:** skeleton, zombie, husk, stray and drowned sleepers, each sitting, lying and face down (`UndeadVariant`). Drowned sleepers only in shipwrecks; elsewhere drowned are their own segment (`specs/drowned.md`). Bogged are not sleepers: they lurk (Undead variants).
- **Ideas, not designed:** parched, wither skeleton, zombie villager.
- **Not yet:** zombified piglin, skeleton/zombie horses, camel husk, zombie nautilus.
- One sleeper block per variant (or a variant property), each with its own 16x and 32x textures.
- Mob spawner tweaks: not now (owner).

## Natural spawning

Owner confirmed: the cut applies everywhere (surface, caves, Nether, End), monsters only; the older camps idea's "no surface spawns, vanilla underground" does not override it.

- One small mixin cuts the global monster cap to a game-rule percentage (`bubbas_reworked_mobs:monster_cap_percent`, default 15), with an off switch for spawn-control mods (`bubbas_reworked_mobs:cut_monster_spawns`).
- It sits at HEAD of `NaturalSpawner$SpawnState#canSpawn`, the per-mob test natural spawning runs after it has picked a mob and a spot, so it sees the type and the position. Biome spawns get the cut cap; spots whose monster list comes from a structure (witch huts, monuments, outposts, fortresses) keep vanilla's cap.
- One hook does both jobs, and it only ever says no, so it composes with other spawn mods.
- The per-player cap is left at vanilla's; in multiplayer the cut global cap is what binds.
- Other categories, light rules, spawners, trial spawners and raids are untouched.
- Wards (Totems & Artifacts) refuse natural monster spawns with their own mixin on the same method (`specs/totems-artifacts.md`).

## Tuning

The early game is meant to be quite a bit tougher: high health and armor-shredding hits, balanced by undead being slow and easy to escape. (Bubba's Reworked Mining slows bare tools: `specs/reworked-mining.md`.)

All values are **starting values, owner to tune**, in `UndeadTuning`. Code: `undead/UndeadCombat`, `undead/entity/DriftToHordeGoal`, client `client/undead/UndeadGaits`. Everything is scoped to our woken undead (`#bubbas_reworked_mobs:woken_undead` or our own entity/renderer classes); vanilla mobs are unchanged.

### Stats

Our entity types' default attributes (`FabricDefaultAttributeRegistry.register`):

| | Health | Speed | Fist damage |
|---|---|---|---|
| Woken skeleton (and stray) | 60 | 0.15 | 1 |
| Woken zombie (and husk) | 80 | 0.13 | 1 |
| Vanilla, for comparison | 20 | 0.25 skeleton / 0.23 zombie | 2-3 |

Zombies get no reinforcements and don't turn into vanilla drowned.

### Combat

- **Fists shred armor only in a streak (owner's change).** One fist hit does almost nothing; an undead that traps the player and keeps hitting tears their armor apart.
  - Each undead keeps its own streak against its current victim (several undead on one target each count separately). A hit within 3 s (60 ticks) of its last keeps the streak; a longer gap or a new victim restarts it.
  - Extra armor durability per hit by streak: 1st 0, 2nd 1, 3rd 3, 4th and later 6, spread over the worn armor pieces (head first gets any remainder). This is on top of vanilla's own wear (players only: max(1, damage/4) per piece).
  - Only bare-handed melee (empty main hand), only unblocked hits.
  - Applied in Fabric's `ServerLivingEntityEvents.AFTER_DAMAGE`; streaks live in a weak map keyed by the attacker (no NBT, they lapse on unload).
- **Smite kills outright by chance:** 4 % per level (Smite V = 20 %), with soul-fire and soul particles and the zombie-villager cure sound (pitch 1.4).
  - Added as a `post_attack` effect on Smite through Fabric's `EnchantmentEvents.MODIFY_WITH_LOOKUP` (composes with datapacks that change Smite; `smite.json` is not overridden). Requirements: `entity_properties` on `#woken_undead` plus `random_chance` with `enchantment_level` linear 0.04.
  - The kill is 10000 damage of `bubbas_reworked_mobs:purged` (in `#bypasses_armor`, `#bypasses_cooldown`, `#bypasses_enchantments`, `#bypasses_resistance`, `#bypasses_effects`, `#no_knockback`; the death message reuses vanilla's "magic"), credited to the player for loot and XP.
- **Bows:** our skeletons take 25 % of arrow damage (any `AbstractArrow`); our zombies take 2x from headshots.
  - A headshot is an arrow whose position, when it hurts, is in the top quarter of the hitbox (adult zombie: above 1.46 of 1.95 blocks, matching the 0.5-block head cube). Vanilla moves the arrow to the hit point on the (0.3-inflated) hitbox before calling hurt, so no mixin is needed.
  - Both are applied in our entity classes' `hurtServer`, before armor.

### Walk animations

- Owner's brief, in place of a generic stagger. Our own model subclasses (body and armor, so armor follows) in our own renderers; no mixin into the shared humanoid model, so vanilla mobs can't be affected. GeckoLib not needed.
- Skeletons walk with vanilla's legs and vanilla zombie arms (both reaching straight out, raised higher when aggressive), except while drawing a bow.
- Baby zombies keep vanilla's model and vanilla movement.

### Zombie shamble

**LOCKED: owner-approved in play ("the zombie animation looks AWESOME"); change it only on the owner's word.** A heavy, limp drag, then a fall onto the good leg. Owner's direction: "drag that back leg up, then push forward in a faster motion with a stiff-ish front leg", "more like a longer drag then a 'hop' with that front leg", but not "too lurchy".

Two phases per cycle, and the zombie's real movement pulses with them, gently: the jerk comes from the timing (long drag, quicker step), not from violent speeds. Code `undead/ZombieGait`; numbers in `UndeadTuning` and `UndeadGaits`.

- **Cycle:** 30 ticks.
- **DRAG** (the first 75 %): the body keeps creeping forward at 0.9x the average speed.
  - The stiff right leg is planted, in front at the start and behind at the end, sweeping back as far as the body moves (-0.45 to +0.4 rad).
  - The bad left leg is dragged up from behind (+0.55 rad) to under the hip (eased in and out, toe turned out, scraping, faster than the body).
- **STEP** (the last 25 %; called the lurch in code): speed `0.9 + k sin²`, with k chosen so the cycle averages exactly 1x (1.3x on average over the step, peak 1.7x). After the body's momentum the server measures 0.9x through the drag and about 1.5x at the peak, so escape difficulty is unchanged.
  - The body falls onto the good leg: the right leg swings through stiff (eased) and lands in front.
  - The torso pitches forward (+0.10 rad) and slowly rights itself through the drag; a slight dip (0.6/16 block) on landing, no bounce.
  - The now-planted left leg sweeps back by exactly the body's distance through the step.
- **Dead weight and overlap:**
  - The arms hang and swing like pendulums a beat behind the body, forward with the fall (up to 0.35 rad, never backward), drifting back to hanging through the drag; the left a little later and smaller.
  - The torso rocks onto the bad side as the good leg swings through, and back over the good leg through the drag (0.05 rad either way).
  - The head lolls after the torso and nods a little after the fall.
- **Server:** `WokenZombie.setSpeed` multiplies the speed the move control sets every walking tick by `sqrt(velocity factor)` (mobs accelerate by speed squared: the speed and the forward input both carry it). No attribute modifier, so nothing is synced. It applies whenever the move control walks the zombie, chasing included (that's the escape case). A zombie standing still (idle, or hitting at arm's reach) moves nothing and stands. Not for babies or in water.
- **Phase:** `(gameTime + murmurHash(entity id) mod 30) mod 30`, the same on client and server with no packets. Each zombie has its own offset, so a horde doesn't lurch in step.
- **Client:**
  - The renderer reads the same phase **4 ticks behind** (`WokenZombieRenderer.CLIENT_LAG_TICKS`): the body's momentum and the client's interpolated position put the visible motion 3-4 ticks behind the server's phase. `UndeadGaitClientGameTest` measures and prints it as `[gait-lag]`.
  - How much it shambles eases in over a few ticks once it moves and out once it stands (`WokenZombie.gaitWalk`, client only).
  - Spread straight legs lift the hips, so the body drops until the lower foot touches the ground.
  - The client sees positions every 3 ticks (vanilla zombie update interval), so the step shows as 2-3 steps of motion.
- **Tests:**
  - `ZombieGaitGameTest`: chasing with its own AI against a vanilla zombie of the same speed: same average within 15 %, slower drag ticks for most of the cycle, ticks over 1.25x in the step.
  - `UndeadGaitClientGameTest` screenshots one full cycle from the side (`gait-side-0..9`, 3 ticks apart from the start of a drag, so frames 0-7 are the drag and 8-9 the step), the front, and the whole file.

### Zombies drift into groups

- A goal at priority 6 (after attacking and grabbing loot, before strolling).
- Every 200 ticks (each zombie on its own random offset), a woken zombie with no target makes one entity query for other woken zombies within 24 blocks (6 up/down). It takes the nearest one and the zombies within 8 blocks of it as the group, and walks to the group's centre at normal speed, stopping within 3 blocks.
- A target always wins: the attack goals hold the move flag, and the goal stops when a target appears.

## Worldgen

Sleepers are plain block placement.
- **Crypt yards and crypts** are a code-built structure, not jigsaw: `specs/graveyards.md`. Yard graves hold the resting dead; the crypts hold niche sleepers (husks and strays in desert and snowy yards).
- **Shipwrecks** get a drowned crew (Drowned sleepers in shipwrecks).

## Libraries

Only Fabric API (+ MixinExtras bundled with Loader). GeckoLib and Polymer were considered and not needed. spark for profiling during development.

## Performance budget

- Sleeper on the server: 0 per tick.
- Hearing: under 0.05 ms per tick with 200 sleepers near a walking player.
- 100 armed sleepers in view: under 1 ms per frame.
- Measure with spark before release.

## Open

- **Playtest numbers:** the noise table, tuning, and husk/stray/bogged numbers are starting values.
- **Resting dead in desert/snowy yards:** husks/strays (built) or skeletons everywhere (owner said "the skeleton kind")?
- **Bogged hiding:** fully underwater in 2-deep water (built), or head showing (1-deep)?
- **Bury-break system** (BACKLOG): shovel-only vs by hand, noise, tools. Today a shovel digs loudly; by hand works, slowly.
- **Back to sleep at dawn:** 16 blocks across and 4 up or down; a place is a solid floor with a light-blocking roof at block light 7 or less, or an open grave. Owner to playtest.
- **Fire ward:** 6 blocks (Manhattan), 3 up or down. Builder's calls to confirm: a mob already hunting someone pushes straight through, and brain-driven hostile mobs (breeze, creaking) aren't covered.

## Milestone test objectives

- (objective, no test yet) A woken skeleton that grabs a bow or sword from a display or the ground never crashes the game: the item is equipped at the start of its next tick, outside goal ticking (a GoalSelector NPE happened when `Skeleton.reassessWeaponGoal` ran mid goal-tick), and a skeleton killed before that tick still drops it.

Test-lite policy (`specs/quality.md`, Test suites): `ReworkedMobsCoreGameTest` runs on every build and push; everything below is the milestone suite, run on release tags, manual CI dispatch and `-PmilestoneTests`. Each line says what the class proves.

- **Core** (`ReworkedMobsCoreGameTest`, 14 across the mod, one smoke test per main mechanic): walking near a sleeper wakes it; the `api.Sleepers` hook calms, senses and keeps sleepers asleep (the contract Totems & Artifacts relies on); a waking mob grabs a weapon first; a husk bursts out of sand and blinds; plus the graveyard test (`specs/graveyards.md`) and the others for creepers, endermen and cairn trading, drowned, Fabricators, spiders, camps and trader posts (in their specs).
- `BubbasReworkedMobsGameTest`: sleepers end to end through the real interaction paths: a placed sleeper item keeps its gear and pose; a sleeper in a niche steps out when it wakes; hearing (animals and wool don't wake; a placed sleeper hears like any other); a wake spawns a mob with its gear facing the noise, in `#minecraft:undead`, `#minecraft:skeletons` and `#woken_undead`; a refused spawn keeps the sleeper; robbing order and its wake chance; silk-touch mining gives the sleeper item, and bare hands, no silk touch or silk touch on another tool leave remains; the monster-cap rule.
- `SleeperNoiseGameTest`: the disturbance meter meets each owner target deterministically: sprinting circles and passes wake, two blocks broken or placed wake, jumps, fights and explosions are loud, walking at 3 wakes within two seconds, walking at 7 or sprinting at 11 doesn't, sneaking at 2 is silent (then walking the same path wakes), small noises settle, solid walls muffle, awaken wakes near neighbours and one awaken event wakes at most the cap, chain-woken undead don't infight, only a survival waker is targeted, retaliation against undead stays vanilla. Also `strongLightCalmsSleepersInWorld` (a glowstone in the world).
- `SleeperCalmGameTest`: light calming (`strongLightCalmsSleepers`), loud bells (`bellsAreLoudToSleepers`) and the hook (`apiCalmsSensesAndKeepsSleepersAsleep`). The Grave Talisman and Totem of Warding tests are in Totems & Artifacts (`specs/totems-artifacts.md`).
- `SleeperFormsGameTest`: every kind x pose x statue survives the form stack, pick block and Silk Touch drop, and names show the form (`everyFormSurvivesPickUpAndPlace`); a re-placed statue ignores explosion, bell and noise, and plain items place the default wakeable form (`statueStaysInertAfterReplaceAndOldItemsPlace`); face-down sleepers are named and wake like any other (`faceDownSleepersAreNamedAndWake`).
- `UndeadVariantsGameTest`: husks burst and blind, strays crack before breaking free, bogged lurk and ambush (Undead variants).
- `ShipwreckCrewGameTest`: shipwrecks have a drowned crew; a drowned sleeper wakes as a drowned.
- `DawnGameTest`: survivors go back to sleep at dawn (with their gear), only those that saw the night, sitting down under any cover.
- `FireWardGameTest`: idle mobs keep away from a lit campfire, only lit fire repels, only fire-fearing mobs.
- `LootDisplayGameTest`: loot on display keeps its item through save/load and templates; an empty hand takes it per placement (ground, wall, stuck; water stays); quiet taking doesn't wake a sleeper but digging buried loot does (a shovel's durability, an empty hand can't dig, digging counts louder); breaking drops the item; the display command places it; woken zombies and skeletons take a displayed weapon or bow over a loose one.
- `GrabLootGameTest`: the waking grab takes any item when no weapon is in reach; the idle grab goal walks to displays and loose items (zombies and skeletons), prefers a display over a closer loose item, ignores displays when the hand is full or the mob isn't in the tag (one test), ignores displays while chasing and goes back for them when idle; contact pickup while chasing stays vanilla's.
- `UndeadCombatGameTest`: fist streaks shred armor and a pause resets them, Smite kills only our undead outright, skeletons shrug off arrows, zombie headshots hurt more, idle zombies drift together and a zombie with a target doesn't (checked once it reaches its target).
- `ZombieGaitGameTest`: the shamble's drag-and-lurch covers the same ground as a steady walker of the same speed.
- **Client** (`ClientTests` names `sleepers`, `displays`, `gait`, `prone`, `variants`, `shipwrecks`; screenshots): sleepers from several angles, sleeper items, a placed sleeper, a woken armored zombie, loot on display; the gait cycle from the side and front and the client's motion lag; face-down sleepers; husks, strays and bogged; shipwreck crews.
