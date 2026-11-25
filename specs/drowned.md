<!-- dte:C1,B17,B18,B19 -->
# Reworked Mobs: drowned (spec)

**Status: built.** A segment of Bubba's Reworked Mobs (C1). "The lurker" and "Owner answers" are the owner's design as given; "How it's built" is what the code does. Open items at the end.

## The lurker

- **Fewer, much stronger.** Drowned are rare, and each one is a real threat.
- **They drown you instead of hitting you.** A drowned grabs you and **drags you downward** until you drown.
- **Breaking the grip:** some enchantments and weapons are better at breaking a drowned's grip than others.
- **Creepy, like creepers:** they do unsettling things. The core one: a drowned **stands in 2-deep water, just waiting**, for you to come close to the water's edge **and turn your back**.

## Prior art

`tools/prior-art.py "drowned"`: nothing does grab-and-drown. Existing drowned mods only change spawning (Conduits Prevent Drowned, No Drowned), trident drop rates, or give mobs armor (Armored Foes, on 26.3: check compatibility with stronger drowned before release).

## Owner answers

- **D1 Spawning:**
  - Natural spawns in water within range of a player, at the same low rate as other undead (the monster cap cut, `specs/undead.md`).
  - Zombies that drown still turn into drowned (zombie numbers are already controlled).
  - A drowned doesn't have to spawn at its lurk spot: it can spawn deeper and **swim to a lurk spot**. A lurk spot is a shallow spot near the shore where **a standing drowned's head just breaks the surface** (water about 2 deep over a floor, by the shore). There it **floats with just its head a little above the water**.
- **D2 Staring:** stared at, it **sinks right below the water** (the creeper peekaboo mechanic in reverse: it hides by sinking). It waits for you to come close to the water and turn your back.
- **Two poses:**
  - **Lying down** while crawling on land (a **sidewinding, staggered crawl**) and while swimming.
  - **Upright** while lurking in a lurk spot and while drowning a grabbed player.
- **The grab:** once it has you it **only moves backwards**, dragging you toward **the deepest water**. Now and then it hits you for a small amount of damage, and **that hit makes your breath drain faster**.
- **D3 Breaking the grip:** a visible meter (like the creeper's startle meter, B19: no hidden timers). **Lots of fast hits, or hits with the right things, loosen it**: axes and Impaling a lot, Knockback helps, Riptide yanks you free. Respiration, Water Breathing and Conduit Power only buy time.
- **D4 Who:** they grab **players and passive mobs**.
- **D6 Tridents:** no trident throwing; **tridents move into buried treasure loot** as a rare item.
- **D7 Vanilla:** zombie-to-drowned conversion stays; a low rate of random natural spawns stays (like the other undead).
- **D8 No drowned sleepers** in open water. The one exception is the shipwreck crew (owner), in `specs/undead.md` "Drowned sleepers in shipwrecks"; they wake as reworked drowned.
- **Drowned must be dangerous, and the grip meter visible** (owner): see "The grip" below.

## How it's built

### Setup

- **No new entity type.** Every vanilla `Drowned` gets the behavior through `mixin/DrownedMixin` (natural spawns, zombies that drowned, eggs, commands). Code: `drowned/` package; all numbers in `DrownedTuning`.
- **Goals:**
  - Vanilla's trident goal, melee chase (`DrownedAttackGoal`), night beach walk (`DrownedGoToBeachGoal`) and every target goal are wrapped in `VanillaOnlyGoal`: they run only on a drowned that stays vanilla (below).
  - `DrownedLurkGoal` (priority 0, MOVE/LOOK/JUMP) holds movement while the mind steers.
  - When the mind is idle, vanilla's daytime "go to water", swim-up, stroll and look-around goals run.
- **Spawner drowned stay vanilla** (owner, "for now"):
  - `DrownedAccess.brm$makeVanilla()` turns the rework off for good: vanilla goals and targeting, vanilla attributes (20 health, 2 armor, speed 0.23), vanilla gear (tridents included).
  - Called at the head of `Drowned.finalizeSpawn` when `EntitySpawnReason.isSpawner(reason)` (spawners and trial spawners). Saved as `bubbas_reworked_mobs:vanilla`.
  - It lives in `DrownedMixin`'s own hook, not the shared `MobFinalizeSpawnMixin`, because it must run before vanilla picks the drowned's gear.
- **Attributes** (via `Drowned.createAttributes`): 60 health, 6 armor, 0.6 knockback resistance, speed 0.23. On land a transient -40 % speed modifier (it crawls).
- **No tridents:**
  - Drowned don't spawn holding one (removed after vanilla's equipment roll; fishing rods stay).
  - One picked up off the ground is held, never thrown.
  - Buried treasure gets one extra pool rolling a trident at 10 % (Fabric `LootTableEvents.MODIFY`, skipped when a datapack replaced the table).

### Spawning

- Owner's aim: "a handful of them if you're travelling near the ocean".
- Vanilla's rules apply: water, depth, darkness, 1 in 40 (1 in 15 in river-type biomes).
- Drowned are `MONSTER`, so the natural monster cap cut (`SpawnStateMixin` → `MonsterCap.allows`) applies to their natural spawns. The cut checks `type.getCategory() == MONSTER`, with no type list.
- **Their own allowance past the cut:** once the cut cap is full, a natural drowned may still spawn while fewer than 5 drowned are within 48 blocks of the spot (`MonsterCap.allows`, `DrownedTuning.NATURAL_NEAR_*`). Without it, land monsters fill the cut cap at night and drowned are very rare.
- Zombie-to-drowned conversion is untouched (vanilla zombies only; our woken zombies never convert).

### The mind

`DrownedMind`: one per drowned, server only, not saved, ticked from `Drowned.tick`. States:

- **IDLE:** vanilla. With a non-spectator player within 32 blocks it looks for a lurk spot (at most once a second, phase-spread).
- **Finding a lurk spot (`LurkSpots.find`):**
  - 40 random columns 3-14 blocks around the nearest player and within 32 of the drowned.
  - A column qualifies with water exactly 2 deep (owner: "a standing drowned's head just breaks the surface") over a solid floor and open air above.
  - It also needs land within 3 blocks (level with the water's surface or one block up) in one of 8 directions (straight ones win ties). That is the shore it faces.
  - Best spot: nearest to 6 blocks from the player, then nearest the drowned.
  - A spot whose player goes more than 20 blocks away is dropped.
- **TRAVEL:** swims there lying down (vanilla's drowned swim control).
- **LURK:**
  - Upright, held by a spring at the spot's centre, eyes 0.12 above the surface. Only its eyes and the top of its head show: the waterline is just under the drawn eyes, the scalp about 0.33 above it.
  - In 2-deep water it floats a little off the floor.
  - Body faces the shore; head turns to the nearest player.
- **Stared at, it sinks (D2):**
  - Every 4 ticks, one 16-block box query.
  - Staring means: anyone it could grab, plus creative players, looking within 25 degrees (owner, 2026-11-25: was 40, which caught a drowned at the edge of the screen) of any of six points, with a clear line of sight to that point (`sense/Gaze`; owner: a cone, not a direct line). The six points are where its head shows at the spot centre just above the surface, 0.4 higher, and 0.3 to each side.
  - Using the spot's points rather than its eyes means it doesn't bob in and out of a watcher's sight at the bank's edge.
  - Stared at, it sinks until its head is 0.1 below the surface or it stands on the floor, and crouches (vanilla's synced crouch pose; the model lowers its head about a quarter block). In 2-deep water its head ends just under the surface.
  - Looked away from, it rises again.
- **SURGE:** it lunges when all of these hold:
  - nobody is staring;
  - a grabbable thing is within 4.5 blocks (horizontal), standing between 1 block below and 2.5 above the surface, not under water, in its line of sight;
  - the drowned is more than 100 degrees from where the victim faces (its back is turned).

  The lunge is 0.3 blocks a tick, rising out of the water and climbing the bank. It grabs when within 0.8 blocks (box to box). It gives up if the victim gets 7 blocks away or it is 4.5 blocks from its spot (so it leaves the water at most about 3.5 blocks).
- **Grabbable (D4, B18):** players (not spectators, not creative) and passive mobs (animals, villagers) that can drown. Nothing invisible, riding or ridden.
- **DRAG:**
  - The victim rides the drowned (see "Why the victim rides"), held by the ankles in front of it (see "Held by the ankles").
  - It moves backwards: faces away from the deepest water nearby and moves toward it (0.07 blocks a tick in water, 0.06 crawling on land), sinking to the floor.
  - Deepest water: columns every 2 blocks in a 21 x 21 square, depth measured to 24 blocks. Re-searched each time it arrives; when nothing is deeper it stays put and looks again after 10 ticks.
  - Upright in water, lying (crawling back) on land.
  - Its small hits and the grip: see "The grip".
- **Breaking free (D3, B19):**
  - Hits from anyone with an attacker (the victim, a friend, an arrow: B18) loosen the grip.
  - At 0 it lets go and recoils (pushed 0.5 a tick away from the victim; the victim nudged up 0.3), then looks for a new spot (not the one it just used).
  - A Riptide trident released by its victim (charged, in water or rain) frees them outright and launches them the vanilla way (`TridentRiptideMixin`, both sides).
  - Respiration, Water Breathing and Conduit Power only buy time (vanilla).
  - Hit while lurking or travelling (no victim), it abandons that spot.
- **Ending a drag otherwise:** a drowned that dies lets go. A victim that leaves another way (ender pearl, commands, death) ends the drag; the drowned goes back to seeking.

### Restless lurking

Owner: "you have them fully parked in one position forever".

- **Unseen, it moves.** While nobody looks within 70 degrees of it (about the screen's edge; same points and sight test as the stare), it moves to another lurk spot:
  - with a 1-in-800 chance a tick (about every 40 s);
  - or 1 in 80 (about every 4 s) once its nearest player is more than 10 blocks from its spot, so it keeps up with someone walking along the shore.
  - The new spot is the usual search around that player, not the one it leaves, at most 16 blocks away. It goes under and swims there lying down. You notice it has moved when you look back.
- **Never quite still:** lurking, it drifts up to 0.15 blocks around its spot's centre on two slow waves, and with a 1-in-600 chance a tick (about every 30 s) dips under for 1-3 s.
- Pinned drowned (test station, client shots) and ones set lurking by tests (`lurkAt`) hold still.

### Postures

- Owner: upright or lying. Vanilla's synced swimming flag is the posture: the server sets it (lying unless lurking or dragging in water).
- Client (`DrownedRendererMixin`, `DrownedModelMixin`, `client/drowned/DrownedPoses`): vanilla's 10-degree swim tilt is replaced by lying flat like a swimming player (85 degrees in water, 90 on land).
- **The crawl on land:** a sidewinding, staggered drag, a fast pull then a slow drag in each cycle, 0.55 cycles per block.
  - The body yaws 22 degrees side to side, rolls 12 degrees onto each shoulder and shifts its hips 0.12 blocks.
  - The arms reach ahead and pull alternately, the legs drag splayed, the head looks ahead.
  - Armor and the outer layer use the same model, so they follow.
- A crawling drowned makes no step sounds or step vibrations (vanilla skips both for swimming entities).

### Held by the ankles

Owner: the victim is held by the feet, not the shoulders.

- **Upright** (lurk spot, dragging in water): the victim's soles sit at the drowned's outstretched hands, 0.72 in front of it and 1.2 above its feet. That is vanilla's zombie arm pose, about 80 degrees forward from hanging, hands at about 1.25 above its feet. The victim floats above and in front of it and goes down feet first as it walks backwards into deep water.
- **Lying** (crawling on land): the victim's feet are at its hands on the ground, 1.2 in front of it (its arms reach past its head).
- Between the two postures the hold follows the drowned's swim amount (the posture easing in and out). Client and server both tick it, so it needs no syncing.
- Numbers in `DrownedTuning`: `HOLD_FORWARD` 0.72, `HOLD_UP` 1.2, `HOLD_LYING_FORWARD` 1.2, `HOLD_LYING_UP` 0 (code: `DrownedMixin.getPassengerAttachmentPoint`). No arm-pose code: vanilla's drowned arm pose already reaches the ankles, and the crawl's arms already reach ahead.
- Crosshair targeting still works for a held player: the drowned is below and behind them, and they look down at it (the drowned's box is clear of their eyes).
- In the 2-deep shallows a freshly grabbed player's head is above the water for a moment; they go under as it backs into deeper water. In 3-deep or deeper water they're under at once.
- Not done: the victim keeps a standing pose (no flailing arms, no lying-on-the-back pose when dragged over land).

### Why the victim rides the drowned

Player movement is client-authoritative. Riding makes the position exact on both sides with no rubber-banding: each side computes the passenger's place from the vehicle (`getPassengerAttachmentPoint` override, no syncing), the passenger can't swim, and vanilla's air rules keep running. The alternative, server-sent motion every tick, is a spring the client fights and lags at high ping. What riding needs:

- `PlayerGripMixin`: sneaking doesn't dismount from a drowned (`Player.wantsToStopRiding`).
- `ProjectileUtilGripMixin`: vanilla's crosshair pick skips your own vehicle; a drowned is never "your ride", so a held player can hit it.
- `DrownedMixin`:
  - no controlling passenger (a held villager would otherwise take over its movement goals);
  - lets go where the victim is (not on top of its head);
  - throws off a passenger it doesn't hold (from a save).
- Client:
  - a held player or humanoid isn't drawn sitting (`HeldVictimPoseMixin`);
  - the "Press Shift to dismount" hint becomes "A drowned has you! Hit it hard and fast to break free" (`GrabbedHintMixin`, lang key `bubbas_reworked_mobs.drowned.grabbed`).
- Logging out while held saves you with the drowned (vanilla saves a player's vehicle); on return it lets go at once.

### The grip

Owner: drowned must be dangerous, and the grip meter visible.

- **Grip, 0-100 (`DrownedMind.grip`):**
  - Starts at **50** when a drowned grabs.
  - **Rises 0.25 a tick** while it drags (5 a second: half to full in 10 s if nobody fights).
  - **At 0 you're free:** every holder lets go and recoils. Riptide frees at once.
- **Hits loosen it** by 6 + 3 per point of damage (before armor):
  - axes x1.5;
  - +12 per level of Impaling, +10 per level of Knockback;
  - a hit on any holder counts, and so does anyone's hit (B18).
- **The danger follows the grip** (B19: the grip is the visible state; no drag timer). At 50 or less its hits use the start values; at 100, the full values; in between, a straight line. Its hits are a chance each tick (no fixed rhythm, B19):

| | grip 50 or less | grip 100 |
|---|---|---|
| Hit chance per tick | 1/50 (about every 2.5 s) | 1/10 (about every 0.5 s) |
| Damage per hit | 2 | 4 |
| Air knocked out per hit | 45 (1.5 bubbles) | 90 (3 bubbles) |

- **Air loss:** Respiration divides it by 1 + the victim's oxygen bonus. Water Breathing, Conduit Power and Breath of the Nautilus stop it (vanilla's "can't lose air" test). Otherwise drowning is vanilla's air rule: the victim's eyes are under water and they can't swim up.
- **What players see and hear as it tightens:**
  - more and harder hits;
  - a squeeze sound with each (the drowned's water ambient) that gets louder and lower;
  - bubbles stream off a victim under water every 2 ticks: 1, rising to 7 at full grip;
  - with the game rule on, the grip itself as a blue bar ("Meter look").
- Numbers in `DrownedTuning`: `GRIP_START`, `GRIP_RISE_PER_TICK`, `GRIP_RISE_PER_HELPER`, `HIT_CHANCE_PER_TICK`/`_FULL`, `HIT_DAMAGE`/`_FULL`, `HIT_AIR_LOSS`/`_FULL`, `ESCALATION_BUBBLES`.

### Death times

If the victim panics (nobody hits back), from `tools/drowned-danger.py`, a Monte Carlo over vanilla's air, drowning, armor and hurt-time rules with the numbers above. Medians, with the middle 80 % in brackets:

| Victim | 1 drowned | 2 drowned | 3 drowned |
|---|---|---|---|
| Unarmored | 8.5 s (7.2-9.8) | 6.0 s (5.2-6.9) | 4.9 s (4.2-5.5) |
| Full iron | 12.7 s (11.6-14.1) | | |
| Full diamond | 17.6 s (15.9-19.6) | | |
| Unarmored, Respiration III | 8.9 s (7.5-11.0) | | |
| Diamond, Respiration III | 24.3 s (21.4-27.5) | | |

The model assumes the victim is under water from the start; in 2-deep shallows their head is above water for the first second or two.

**Getting out** (fresh grab):

- One drowned: two iron axe hits (49.5 each) about a second apart; three or four iron sword hits (24 each, 1.6 a second) in about 2 s; fists (9 a hit, at most 2 a second through vanilla's hurt time) in about 4 s. All well inside the 8.5 s.
- Two holding: an axe takes about 4 hits (3.5 s), a sword about 3 s; fists can't win alone.
- Three holding: an axe takes about 5 s, against about 5 s to death. You need a friend.

### Others join in

Owner's call.

- **Joining:** a dragging drowned looks once a second (one box query, 16 blocks) for drowned already there and free: not vanilla, not riding, and idle, travelling or lurking. The nearest swim over (state JOIN: lying, 0.15 blocks a tick) and grab the other leg (state HELP).
  - At most 2 extras, so 3 holders in all.
  - No drowned are spawned for this.
  - Works on any victim the same way (passive mobs too, B18).
- **How it fits the riding hold (one vehicle):**
  - The first drowned stays the vehicle and is the only one that moves the victim.
  - Each extra holds a grip point at the victim's feet from the side (90 degrees round from the first one's grip, one on each side), level with the first. A spring keeps it there, facing the victim, upright in water, following as the first one drags.
  - Pulled more than 3 blocks off its point, it swims back.
- **Each extra holder:**
  - makes the grip rise 50 % faster (+0.125 a tick per extra);
  - divides hit points by 1 + 0.6 per extra (two holders: /1.6, three: /2.2);
  - makes the drag 35 % faster;
  - rolls its own hits on the same grip value, so more hits land.
- **Letting go:** there's one shared meter, held by the first drowned. When it reaches 0 (or Riptide fires, or the first one dies) everyone lets go and the extras recoil too. An extra lets go on its own if the first one stops dragging, the victim is gone, or it gives up swimming over (24 blocks away). Killing an extra removes it.
- Numbers in `DrownedTuning`: `DRAG_SPEED_PER_HELPER`, `GRIP_EXTRA_HOLDER_DIVISOR`, `JOIN_*`.

### Meter look

Owner: like the XP bar.

- Vanilla's boss-bar sprites, blue for the drowned grip and red for the creeper startle, with the 20-notch overlay: the XP bar's notched shape in vanilla colours, no new textures.
- Shown only with the game rule `bubbas_reworked_mobs:show_meters` on (default off).
- HUD bar under the crosshair (drawn like the boss bar).
- Above-head bars: small camera-facing quads (1 x 0.1 block, full-bright, within 32 blocks). A holding drowned's bar sits over its victim's head.

### Cost (per drowned)

- Idle: a players-list lookup per tick.
- Seeking: one spot search a second at most (40 columns x up to 10 block reads; shore check only on qualifying columns).
- Lurking: one 16-block entity box query every 4 ticks. Per thing found: a dot product, a block raycast only for those looking its way, and a line-of-sight check only for grabbable things within 4.5 blocks.
- Dragging: the deep-water search (121 columns x up to 24 block reads) once per grab and per arrival; no entity queries.

## Numbers (all in `DrownedTuning`; builder's starting values, owner to playtest)

| | Value |
|---|---|
| Health / armor / knockback resistance (vanilla 20 / 2 / 0) | 60 / 6 / 0.6 |
| Crawl speed on land | -40 % |
| Natural spawns past the cut cap | while fewer than 5 drowned within 48 blocks |
| Wakes (looks for a spot) with a player within | 32 blocks |
| Lurk spot | water exactly 2 deep, land within 3, 3-14 blocks from the player (best ~6) |
| Lurking: eyes above the surface | 0.12 |
| Sunk: head below the surface | 0.1, or on the floor and crouched |
| Stare cone / sense range | 25 degrees round 6 points / 16 blocks, every 4 ticks |
| Restless: moves when unseen (70 degrees) / following | 1 in 800 a tick (~40 s) / 1 in 80 (~4 s) past 10 blocks; new spot within 16 |
| Sway / dip | 0.15 blocks / 1 in 600 a tick for 1-3 s |
| Surge reach | 4.5 blocks; victim between 1 below and 2.5 above the surface |
| Back turned | drowned more than 100 degrees from the victim's facing |
| Lunge speed / grab reach / leaves the water up to | 0.3 a tick / 0.8 / ~3.5 blocks |
| Drag speed (water / land) | 0.07 / 0.06 a tick |
| Deep water search | 21 x 21 blocks every 2nd column, 24 deep |
| Small hits | 1/50 a tick, 2 damage, 45 air at grip 50 or less; up to 1/10, 4 damage, 90 air at grip 100 |
| Grip | starts 50, free at 0, rises 0.25 a tick (+50 % per extra holder) |
| Per hit (loosens) | 6 + 3 x damage; axe x1.5; Impaling +12/level; Knockback +10/level; divided by 1 + 0.6 per extra holder; Riptide frees outright |
| Extra holders | up to 2 more, joining from 16 blocks; drag 35 % faster per extra |
| Recoil / victim lift | 0.5 / 0.3 a tick |
| Buried treasure trident | 10 % per chest |

Time to drown with nobody fighting: see "Death times" (about 8.5 s unarmored with one drowned).

## Builder's choices (the owner may want to change these)

1. **The victim rides the drowned** (see "Why the victim rides the drowned"), and sneaking can't get you off. The only ways out are the grip meter, Riptide, its death, or ender pearls/commands.
2. **When it leaves the water:** only to reach a victim at the edge (about 3.5 blocks from its spot at most), then it crawls back dragging them. Otherwise it stays in the water while a player is near; vanilla's own behavior (daytime "back to water", swimming up) runs when nobody is.
3. **Someone staring keeps everyone safe:** while anything is staring at it, it won't surge at anyone (`WATCHED_BLOCKS_SURGE`). "Watch the water for your friend."
4. **Passive mobs count as watchers too** (B18): a cow looking at it makes it sink, a cow with its back turned gets grabbed. Creative players' stares count (so the owner can try the sinking); creative players are never grabbed (they can't drown).
5. **Sunk = on the floor and crouched** in 2-deep water: a standing drowned is 1.95 tall and 2-deep water's surface is 1.89 above the floor, so standing alone leaves its scalp at the surface. The crouch is the vanilla pose (its hitbox doesn't change).
6. **It lurks day and night** (vanilla drowned are only aggressive at night or when you're in water). In daylight a drowned that comes up the bank burns (vanilla).
7. **Tridents:** none at spawn; one it picks up is held, never thrown. Spawner drowned keep vanilla tridents.
8. **Hit from the shore while lurking, it moves to another spot** (no free hits on a sitting target), rather than charging.
9. **Hits are a chance per tick, not a timer** (B19).
10. Spectators don't wake drowned, stare, or get grabbed.
11. The mind (spot, grip) isn't saved; a reloaded drowned looks for a spot again and lets go of anything riding it.
12. Lying uses vanilla's swimming flag, so a lying drowned is silent to sculk (no step vibrations) and its hitbox stays upright-sized while it crawls.
13. **The danger starts counting at grip 50**, not 0, so a fresh grab has the start values. Hitting it below 50 doesn't make its hits weaker than the start values, but brings it closer to letting go.
14. **Extra holders make it harder both ways:** the grip rises faster and your hits count for less, and the extras add their own hits. Fists can't beat two holders alone.
15. **Axes x1.5, not x2:** a fresh grab still needs two axe hits (with x2, one axe hit from 50 would free you at once).
16. **The extras hold the sides of the feet, not the vehicle slot:** the riding hold has room for one vehicle, so only the first drowned moves the victim; the extras follow it.
17. **Meters show for creative players and spectators too** (it's a debug and playtest view behind a rule that's off by default).

## Test world station idea (designed, not built)

"The drowned shore":

- A small pond with a 2-deep shelf by a grass bank and a deep middle.
- One drowned pinned to a lurk spot (`DrownedMind.pinTo`: keeps its spot with no player near).
- A villager on the bank behind a fence, facing the water, so the drowned sinks while the villager watches.
- Shots: from the bank (head just showing), and from the side at water level.

## Milestone test objectives

- **Core** (every build, `ReworkedMobsCoreGameTest`): `drownedGrabsAndDragsToDeepWater`. A survival player at the edge with their back turned is grabbed within 4 s, held the whole way despite sneaking, and dragged backwards out over the pit, eyes under water, drowned upright; done as soon as it gets there.
- **Milestone** (`DrownedGameTest`):
  - `gripLoosensWithHitsAndTightensWhileDragging`
  - `secondDrownedJoinsAndHoldsHarder`
  - `staredAtItSinks`: stared at, sunk and crouched with its head at or under the surface; looked away from, eyes above the surface and upright.
  - `gripTightensToFullAndHitsHarder`: starts from a second short of full grip, not 10 s of dragging.
  - `swimsToALurkSpotNearThePlayer`: from the deep pit to the shallows by the shore, facing it. This also covers, statistically, a drowned swimming out of a pit getting over the lip onto 2-deep shallows (a deterministic start under the lip didn't reproduce the hover, so there's no separate test).
  - `onlyBacksTurnedAreGrabbed`: facing it, or creative, not grabbed.
  - `hitsKnockAirOutUnlessWaterBreathing`
  - `gripPointsFavourAxesImpalingKnockback`
  - `strongerAndNoTridents`: 200 natural spawns, tuned health and armor, no tridents.
  - `spawnerDrownedStayVanilla`: spawner and trial spawner drowned get the vanilla flag, 20 health, tridents possible; the flag survives a save.
  - `buriedTreasureCanHoldATrident`: 10-90 in 400 rolls.
  - Tests use their own structure `drowned_pool` (land, 2-deep shallows, an 8-deep pit), not one dug under the shared field: digging below a shared structure leaves blocks the runner never clears and made `SleeperNoiseGameTest.strongLightCalmsSleepersInWorld` fail every run.
- **Client** (`DrownedClientGameTest`, client part `drowned`). Screenshots (previews kept in `docs/previews/drowned/`):
  - `drowned-lurk`, `drowned-lurk-low`: upright, head above water;
  - `drowned-sunk`: a villager stares, under the surface;
  - `drowned-grab`: surged at a villager with its back turned;
  - `drowned-drag-deep`: upright under water holding it;
  - `drowned-swim`: lying, swimming to its spot;
  - `drowned-crawl-side`, `drowned-crawl-front`: lying, crawling on land, at night;
  - `drowned-held-player`: the camera player held, third person, with the grabbed hint.
- **Not yet tested (objectives):**
  - Riptide frees a held player and launches them.
  - Knockback and Impaling hits in a live grab.
  - A held player can target the drowned with the crosshair (client).
  - It crawls back down the bank dragging a victim.
  - It abandons a spot when hit from the shore.
  - A reloaded drowned throws off a saved passenger.
  - The held player isn't drawn sitting.

## Bogged

Bogged lurk and ambush like the drowned, in swamp water and mud by reeds, but never grab: `bogged/BoggedLurk`. Spec and numbers in `specs/undead.md` "Undead variants".

## Open

- Owner to playtest the numbers (all in `DrownedTuning`): health and armor, drag speed, the grip's rise and the per-hit points, the death times.
- Owner to review the builder's choices above, in particular: when it leaves the water (only to reach a victim at the edge), someone staring keeps everyone safe, it lurks day and night, and the danger counting from grip 50.
- Owner: grab by the feet is built; a flailing or lying-on-the-back pose for the held victim is not (say if wanted).
- Test world "drowned shore" station: not built; build it when the owner wants to playtest drowned there.
- Compatibility: Armored Foes (gives mobs armor) with the stronger drowned, before release.
