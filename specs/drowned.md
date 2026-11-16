<!-- dte:C1,B17,B18,B19 -->
# Reworked Mobs: drowned (spec, first build done 2026-11-16)

The next segment of Bubba's Reworked Mobs (C1), after creepers and endermen. Owner's design (2026-11-16), recorded as given; open questions below.

## The lurker

- **Fewer, much stronger.** Drowned are rare, and each one is a real threat.
- **They drown you instead of hitting you.** A drowned grabs you and **drags you downward** until you drown.
- **Breaking the grip:** some enchantments and weapons are better at breaking a drowned's grip than others.
- **Creepy, like creepers:** they do unsettling things. The core one: a drowned **stands in 2-deep water, just waiting**, for you to come close to the water's edge **and turn your back**.

## Prior art

`tools/prior-art.py "drowned"` (2026-11-16): nothing does grab-and-drown. Existing drowned mods only change spawning (Conduits Prevent Drowned, No Drowned), trident drop rates, or give mobs armor (Armored Foes, on 26.3: check compatibility with stronger drowned before release).

## Owner answers (2026-11-16)

- **D1 Spawning: natural spawns** in water within range of a player, at the same low rate as other undead (the monster cap cut, `specs/undead.md`), plus **zombies that drown still turn into drowned** (we control zombie numbers already). A drowned doesn't have to spawn at its lurk spot: it can spawn deeper and **swim to a lurk spot**, a shallow spot near the shore where **a standing drowned's head just breaks the surface** (water about 2 deep over a floor, by the shore: owner confirmed). There it **floats with just its head a little above the water**.
- **D2 Staring:** if you stare at it, it **sinks right below the water** (the creeper peekaboo mechanic, in reverse: it hides by sinking). It waits for you to come close to the water and turn your back.
- **Two poses (owner):** **lying down** while crawling on land (a **sidewinding, staggered crawl**) and while swimming; **upright** while lurking in a lurk spot and while drowning a grabbed player.
- **The grab:** once it has you it **only moves backwards**, dragging you toward **the deepest water**. Now and then it hits you for a small amount of damage, and **that hit makes your breath drain faster**.
- **D3 Breaking the grip:** a grip-break meter (like the creeper's startle meter, B19: no hidden timers). **Lots of fast hits, or hits with the right things, build it**: axes and Impaling big, Riptide yanks you free, Knockback helps; Respiration, Water Breathing and Conduit Power only buy time.
- **D4 Who:** they grab **players and passive mobs**.
- **D6 Tridents:** no trident throwing; **tridents move into buried treasure loot** as a rare item.
- **D7 Vanilla:** zombie-to-drowned conversion stays; a low rate of random natural spawns stays (like the other undead).
- **D8:** **no drowned sleepers** (dropped from the undead plan).

## Still open (builder decides, owner may change)

- When a drowned leaves the water: [builder: only to reach something it's grabbing near the edge, a few blocks up the bank, then it crawls back dragging it].
- How much stronger: health, armor, drag speed, the grip-break meter's numbers.

## How it's built

**No new entity type.** Every vanilla `Drowned` gets the behavior through `mixin/DrownedMixin` (natural spawns, zombies that drowned, eggs, commands). Code: `drowned/` package; all numbers in `DrownedTuning`.
- Goals: vanilla's trident goal, melee chase (`DrownedAttackGoal`), night beach walk (`DrownedGoToBeachGoal`) and every target goal are wrapped in `VanillaOnlyGoal` (they run only on a drowned that stays vanilla, below). `DrownedLurkGoal` (priority 0, MOVE/LOOK/JUMP) holds movement while the mind steers; when the mind is idle, vanilla's daytime "go to water", swim-up, stroll and look-around goals run.
- **Spawners stay vanilla (owner, 2026-11-16, "for now").** `DrownedAccess.brm$makeVanilla()` turns the rework off for good: vanilla goals and targeting, vanilla attributes (20 health, 2 armor, speed 0.23), vanilla gear (tridents included). Called at the head of `Drowned.finalizeSpawn` when `EntitySpawnReason.isSpawner(reason)` (spawners and trial spawners); saved as `bubbas_reworked_mobs:vanilla`. To fold into main's `MobFinalizeSpawnMixin`: drop `brm$spawnerStaysVanilla` from `DrownedMixin` and call `((DrownedAccess) drowned).brm$makeVanilla()` there.
- Attributes (via `Drowned.createAttributes`): 60 health, 6 armor, 0.6 knockback resistance, speed 0.23; on land a transient -40 % speed modifier (it crawls).
- No tridents: drowned no longer spawn holding one (removed after vanilla's equipment roll; fishing rods stay). One picked up off the ground is held, never thrown. Buried treasure gets one extra pool rolling a trident at 10 % (Fabric `LootTableEvents.MODIFY`, skipped when a datapack replaced the table).
- Spawning: unchanged from vanilla. Drowned are `MONSTER`, so the natural monster cap cut (`SpawnStateMixin` → `MonsterCap.allows`) already applies to their natural spawns (verified in the code path: the cut checks `type.getCategory() == MONSTER`, no type list). Zombie-to-drowned conversion is untouched (vanilla zombies only; our woken zombies never convert).

**The mind (`DrownedMind`, one per drowned, server only, not saved).** Ticked from `Drowned.tick`. States:
- *IDLE:* vanilla. With a non-spectator player within 32 blocks it looks for a lurk spot (at most once a second, phase-spread).
- *Lurk spots (`LurkSpots.find`):* 40 random columns 3-14 blocks around the nearest player and within 32 of the drowned. A column qualifies with water exactly 2 deep (owner: "a standing drowned's head just breaks the surface") over a solid floor, open air above, and land within 3 blocks (level with the water's surface or one block up) in one of 8 directions (straight ones win ties): the shore it faces. Best: nearest to 6 blocks from the player, then nearest the drowned. A spot whose player goes more than 20 blocks away is dropped.
- *TRAVEL:* swims there (vanilla's drowned swim control, lying down).
- *LURK:* upright, held by a spring at the spot's centre, its eyes 0.25 above the surface (in 2-deep water it floats a little off the floor), body facing the shore, head turning to the nearest player.
- *Stared at → sinks (D2):* every 4 ticks one 16-block box query. Anyone it could grab, plus creative players, looking within 30 degrees of the point where its head shows (spot centre, just above the surface) with a clear line of sight to that point, is staring. Stared at, it sinks until its head is 0.1 below the surface or it stands on the floor, and crouches (vanilla's synced crouch pose; the model lowers its head about a quarter block), so in 2-deep water its head ends just under the surface. Looked away from, it rises again. Using the spot's point rather than its eyes means it doesn't bob in and out of a watcher's sight at the bank's edge.
- *SURGE:* nobody staring, and a grabbable thing within 4.5 blocks (horizontal), standing between 1 block below and 2.5 above the surface, not under water, in its line of sight, with the drowned more than 100 degrees from where it faces (its back turned): it lunges (0.3 blocks a tick, rising out of the water, climbing the bank) and grabs when within 0.8 blocks (box to box). It gives up if the victim gets 7 blocks away or it is 4.5 blocks from its spot (it leaves the water at most ~3.5 blocks).
- *Grabbable (D4, B18):* players (not spectators, not creative) and passive mobs (animals, villagers) that can drown; nothing invisible, riding or ridden.
- *DRAG:* the victim rides the drowned (below), held in front of it, feet level with its own. It moves backwards: faces away from the deepest water nearby and moves toward it (0.07 blocks a tick in water, 0.06 crawling on land), sinking to the floor. Deepest water: columns every 2 blocks in a 21 x 21 square, depth measured to 24 blocks; re-searched each time it arrives (it stays put when nothing is deeper). Upright in water, lying (crawling back) on land.
- *Small hits:* a 1-in-50 chance each tick (no fixed rhythm, B19; about every 2.5 s): 2 damage, and 45 air knocked out (1.5 bubbles), divided by 1 + the victim's oxygen bonus (Respiration), none under Water Breathing, Conduit Power or Breath of the Nautilus (vanilla's "can't lose air" test). Otherwise drowning is vanilla's air rule: the victim's eyes are under water, they can't swim up.
- *Grip-break meter (D3, B19):* 0-100, drains 0.25 a tick (5 a second). Every hit on it with an attacker (the victim's, a friend's, an arrow: B18) adds 6 + 3 per point of damage (before armor); axes double it; +12 per level of Impaling and +10 per level of Knockback on the weapon. Full: it lets go and recoils (pushed 0.5 a tick away from the victim; the victim nudged up 0.3), then looks for a new spot (not the one it just used). A Riptide trident released by its victim (charged, in water or rain) frees them outright and launches them the vanilla way (`TridentRiptideMixin`, both sides). Respiration, Water Breathing and Conduit Power only buy time (vanilla). Hit while lurking or travelling (no victim), it abandons that spot.
- A drowned that dies lets go. A victim that leaves another way (ender pearl, commands, death) ends the drag; it goes back to seeking.

**Why the victim rides the drowned.** Player movement is client-authoritative. Riding makes the position exact on both sides with no rubber-banding: the passenger's place is computed from the vehicle on each side (`getPassengerAttachmentPoint` override, needs no syncing), they can't swim, and vanilla's air rules keep running. Server-sent motion every tick was the alternative (a spring the client fights, laggy at high ping). What riding needed:
- `PlayerGripMixin`: sneaking doesn't dismount from a drowned (`Player.wantsToStopRiding`).
- `ProjectileUtilGripMixin`: vanilla's crosshair pick skips your own vehicle; a drowned is never "your ride", so a held player can hit it.
- `DrownedMixin`: no controlling passenger (a held villager would otherwise take over its movement goals); let go where you are (not on top of its head); a passenger it doesn't hold (from a save) is thrown off.
- Client: a held player or humanoid isn't drawn sitting (`HeldVictimPoseMixin`), and the "Press Shift to dismount" hint becomes "A drowned has you! Hit it hard and fast to break free" (`GrabbedHintMixin`, lang key `bubbas_reworked_mobs.drowned.grabbed`).
- Logging out while held saves you with the drowned (vanilla saves a player's vehicle); on return it lets go at once.

**Postures (owner, 2026-11-16: upright or lying).** Vanilla's synced swimming flag is the posture: the server sets it (lying unless lurking, or dragging in water). Client (`DrownedRendererMixin`, `DrownedModelMixin`, `client/drowned/DrownedPoses`): vanilla's 10-degree swim tilt is replaced by lying flat like a swimming player (85 degrees in water, 90 on land). On land it crawls: a sidewinding, staggered drag (fast pull, slow drag in each cycle, 0.55 cycles per block): the body yaws 22 degrees side to side, rolls 12 degrees onto each shoulder and shifts its hips 0.12 blocks; the arms reach ahead and pull alternately, the legs drag splayed, the head looks ahead. Armor and the outer layer use the same model, so they follow. A crawling drowned makes no step sounds or step vibrations (vanilla skips both for swimming entities).

**Cost (per drowned).** Idle: a players-list lookup per tick. Seeking: one spot search a second at most (40 columns x up to 10 block reads, shore check only on qualifying columns). Lurking: one 16-block entity box query every 4 ticks; per thing found a dot product, a block raycast only for those looking its way, and a line-of-sight check only for grabbable things within 4.5 blocks. Dragging: the deep-water search (121 columns x up to 24 block reads) once per grab and per arrival; no entity queries.

**Held by the ankles (owner, 2026-11-16).**

- The victim is held by the feet, not the shoulders. Upright (lurking spot, dragging in water): the victim's soles sit at the drowned's outstretched hands (vanilla's zombie arm pose, about 80 degrees forward from hanging, hands at ~1.25 above its feet), 0.72 in front of it and 1.2 above its feet. The victim floats above and in front of it and goes down feet first as it walks backwards into deep water.
- Lying (crawling on land): the victim's feet are at its hands on the ground, 1.2 in front of it (its arms reach past its head). Between the two postures the hold follows the drowned's swim amount (the posture easing in and out), which both client and server tick, so it still needs no syncing.
- Numbers in `DrownedTuning`: `HOLD_FORWARD` 0.72, `HOLD_UP` 1.2, `HOLD_LYING_FORWARD` 1.2, `HOLD_LYING_UP` 0 (code: `DrownedMixin.getPassengerAttachmentPoint`). No arm-pose code was needed: vanilla's drowned arm pose already reaches to the ankles, and the crawl's arms already reach ahead.
- Crosshair targeting still works for a held player: the drowned is now below and behind them; they look down at it (the pick fix is unchanged; the drowned's box is clear of their eyes).
- In the 2-deep shallows a freshly grabbed player's head is above the water for a moment (their feet are at the drowned's hands); they go under as it backs into deeper water. In 3-deep or deeper water they're under at once.
- Not done: the victim keeps a standing pose (no flailing arms, no lying-on-the-back pose when dragged over land).

**Lower lurk (owner, 2026-11-16).**

- Lurking, only its eyes and the top of its head show; the waterline is just under the eyes. `LURK_EYES_ABOVE_SURFACE` 0.25 → **0.12** (entity eye height; the drawn eyes sit right at the surface, the scalp about 0.33 above it).


## Numbers (all in `DrownedTuning`; builder's starting values, owner to playtest)

| | Value |
|---|---|
| Health / armor / knockback resistance (vanilla 20 / 2 / 0) | 60 / 6 / 0.6 |
| Crawl speed on land | -40 % |
| Wakes (looks for a spot) with a player within | 32 blocks |
| Lurk spot | water exactly 2 deep, land within 3, 3-14 blocks from the player (best ~6) |
| Lurking: eyes above the surface | 0.25 |
| Sunk: head below the surface | 0.1, or on the floor and crouched |
| Stare cone / sense range | 30 degrees / 16 blocks, every 4 ticks |
| Surge reach | 4.5 blocks; victim between 1 below and 2.5 above the surface |
| Back turned | drowned more than 100 degrees from the victim's facing |
| Lunge speed / grab reach / leaves the water up to | 0.3 a tick / 0.8 / ~3.5 blocks |
| Drag speed (water / land) | 0.07 / 0.06 a tick |
| Deep water search | 21 x 21 blocks every 2nd column, 24 deep |
| Small hits | 1/50 chance a tick (~2.5 s), 2 damage, 45 air (1.5 bubbles) |
| Grip meter full / drain | 100 / 0.25 a tick (5 a second) |
| Per hit | 6 + 3 x damage; axe x2; Impaling +12/level; Knockback +10/level; Riptide frees outright |
| Examples | iron axe (9): 66, two fast hits free you; iron sword (6): 24, about 5 hits in 3 s; fists: 9, at most 2 a second through vanilla's hurt time, about 8 s |
| Recoil / victim lift | 0.5 / 0.3 a tick |
| Buried treasure trident | 10 % per chest |

Time to drown with nobody fighting: about 15-16 s for a full-health player (air gone in ~8.5 s with the knocks, then vanilla drowning plus the hits).

## Builder's choices (the owner may want to change these)

1. **The victim rides the drowned** (see above), and sneaking can't get you off. The only way out is the meter, Riptide, its death, or ender pearls/commands.
2. **When it leaves the water:** only to reach a victim at the edge (about 3.5 blocks from its spot at most), then it crawls back dragging them. Otherwise it stays in the water while a player is near; vanilla's own behavior (daytime "back to water", swimming up) runs when nobody is.
3. **Someone staring keeps everyone safe:** while anything is staring at it, it won't surge at anyone (`WATCHED_BLOCKS_SURGE`). "Watch the water for your friend."
4. **Passive mobs count as watchers too** (B18): a cow looking at it makes it sink, a cow with its back turned gets grabbed. Creative players' stares count (so the owner can try the sinking); creative players are never grabbed (they can't drown).
5. **Sunk = on the floor and crouched** in 2-deep water: a standing drowned is 1.95 tall and 2-deep water's surface is 1.89 above the floor, so standing alone leaves its scalp at the surface. The crouch is the vanilla pose (its hitbox doesn't change).
6. **It lurks day and night** (vanilla drowned are only aggressive at night or when you're in water). In daylight a drowned that comes up the bank burns (vanilla).
7. **Tridents:** none at spawn; one it picks up is held, never thrown. Vanilla drowned (spawners) keep vanilla tridents.
8. **Hit from the shore while lurking, it moves to another spot** (no free hits on a sitting target), rather than charging.
9. **Hits are a chance per tick, not a timer** (B19).
10. Spectators don't wake drowned, stare, or get grabbed.
11. The mind (spot, meter) isn't saved; a reloaded drowned looks for a spot again and lets go of anything riding it.
12. Lying uses vanilla's swimming flag, so a lying drowned is silent to sculk (no step vibrations) and its hitbox stays upright-sized while it crawls.

## Test world station idea

"The drowned shore": a small pond with a 2-deep shelf by a grass bank and a deep middle, one drowned pinned to a lurk spot (`DrownedMind.pinTo`, keeps its spot with no player near), a villager on the bank behind a fence facing the water (so it sinks while the villager watches). Shots: from the bank (head just showing), and from the side at water level.

## Milestone test objectives

Core (every build, `ReworkedMobsCoreGameTest`): `drownedGrabsAndDragsToDeepWater` (a survival player at the edge with their back turned is grabbed within 4 s, sneaking doesn't free them, dragged backwards out over the pit, eyes under water, drowned upright), `drownedGripBreakMeterBreaksTheHold` (a fist hit adds ~9 and drains away; two fast axe hits free a villager), `drownedStaredAtItSinks` (stared at: sunk and crouched, head at or under the surface; looked away from: eyes above the surface, upright).

`DrownedGameTest` (milestone): `swimsToALurkSpotNearThePlayer` (from the deep pit to the shallows by the shore, facing it); `onlyBacksTurnedAreGrabbed` (facing it, or creative: not grabbed); `hitsKnockAirOutUnlessWaterBreathing`; `gripPointsFavourAxesImpalingKnockback`; `strongerAndNoTridents` (200 natural spawns: tuned health and armor, no tridents); `spawnerDrownedStayVanilla` (spawner and trial spawner: vanilla flag, 20 health, tridents possible, flag survives a save); `buriedTreasureCanHoldATrident` (10-90 in 400 rolls). Tests use their own structure `drowned_pool` (land, 2-deep shallows, an 8-deep pit), not dug under the shared field: digging below a shared structure left blocks the runner never clears and made `SleeperNoiseGameTest.strongLightCalmsSleepersInWorld` fail every run.

`DrownedClientGameTest` (client part `drowned`): screenshots drowned-lurk, drowned-lurk-low (upright, head above water), drowned-sunk (a villager stares: under the surface), drowned-grab (surged at a villager with its back turned), drowned-drag-deep (upright under water holding it), drowned-swim (lying, swimming to its spot), drowned-crawl-side, drowned-crawl-front (lying, crawling on land, at night), drowned-held-player (the camera player held, third person, with the grabbed hint). Previews kept in `docs/previews/drowned/`.

Not yet tested (objectives): Riptide frees a held player and launches them; Knockback/Impaling hits in a live grab; a held player can target the drowned with the crosshair (client); it crawls back down the bank dragging a victim; it abandons a spot when hit from the shore; a reloaded drowned throws off a saved passenger; the "held" player isn't drawn sitting.
