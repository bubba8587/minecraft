<!-- dte:C1,B15,B17,B21 -->
# Reworked Mobs: monster camps (spec)

**Status: built.** Camps, banner keys, leaders, daily resets, a lit fire quiets a camp, shade and the underground room, layout variety and big pieces, pillager outposts, maps from cartographers, Nether outposts (the piglin kind, below) and trader posts (a wandering trader's market square: not a monster camp, but it shares the camps' structure machinery, below).

The owner's design started from the idea in `docs/research/camps-blood-moon-idea.md`; decisions made since win where they differ: the 15 % spawn cut stays, and there is **no blood moon** (owner).

## What a camp is
<a id="as-built-2026-11-22"></a>

- **A structure with a fixed roster** of monsters. The roster appears when a player comes near (a wide proximity trigger, so idle camps cost nothing).
- **Camp types (owner): skeletons and zombies.** Biome versions use the same rules with other mobs and materials. The kind follows the biome:
  - **husk** in deserts and badlands (acacia, sandstone);
  - **stray** in snowy biomes (spruce, packed ice);
  - **bogged** in swamps and mangroves (mangrove wood, mud bricks);
  - **skeleton** or **zombie** everywhere else (spruce or dark oak, stone or mossy cobble).
  - Zombie and husk camps have zombie heads; the others skulls.
- **Sizes** (`CampTuning.Size`):

| Size | Chance | Clearing radius | Roster + leader | Banners | Towers | Lean-tos |
|---|---|---|---|---|---|---|
| Small | 45 % | 6 | 3 + 1 | 1 | 0-1 | 1 |
| Medium | 35 % | 8 | 5 + 1 | 2 | 0-2 | 1-2 |
| Large | 20 % | 11 | 7 + 1 | 3 | 0-3 | 1-2 |

- **Roster:** appears when a player comes within **48 blocks**, day or night (owner, 2026-11-24, reversing the earlier night-only rule); by day it appears in the shade. Trigger tests still run in the `camp_night` test environment.
  - Archers (skeleton, stray, bogged) man the towers first; the rest stand round the fire (by day, in the shade).
  - Camp monsters stay at their camp (a home radius) and never despawn. Zombie camps have no babies.
  - Armour is vanilla's own spawn roll only (owner: roughly vanilla frequency; `Mob.populateDefaultEquipmentSlots`, at most 15 % times the regional difficulty multiplier, helmet first). No extra helmet roll, so most members burn by day. Some zombies and husks (40 %) carry stone swords or axes.
  - **Leash (owner):** a camp mob more than `CampTuning.LEASH_RANGE` (32) blocks from its home spot drops its target and walks home, ignoring players until back inside its home radius (`CampMemberMixin`; not saved, so a reload lets it chase again).
- **Leader (owner):** one per camp, the same mob type, **slightly larger, tougher, better gear**, and **no banner**.
  - **1.25x size** (vanilla `scale` attribute), **2.5x health**, +3 attack damage (melee kinds), half knockback resistance.
  - Iron helmet and chestplate; an **iron axe** (zombie, husk) or a **Power II bow** (archer kinds).
  - Drops its weapon half the time and each armour piece 1 time in 5.
- **Loot chest** (`chests/monster_camp`, by the fire): the lowest of four chest tiers (Chest tiers, below).
- **Alarms: none (owner).** Camp alarm bells, and a raid horn for outposts, were designed and dropped ("i dont think we should do camp alarm bells").

## Look and layout

**Feel (owner):** started from images of Breath of the Wild's monster camps ("look at images of it to start"; Minecraft's blocks make ours different anyway): **scant wooden lookout towers, ladders, bone-and-skull rock outcrops** from Minecraft blocks (bone blocks, skulls, stone). Sizes and layouts vary, all deterministic from the seed. Vanilla blocks only (B15).

**Camps are dark (owner):** no campfires or other fire and **no light at all**. Fire would ward their own monsters (`specs/undead.md` "Fire as a weak ward") and make camps too easy to find. Dead (unlit) campfires as decoration are fine.

**Every camp:** a dark trodden clearing round a cold, unlit soul campfire ringed by log seats and the loot chest; plus some of:
- **stilt lookout towers:** log posts, a patchy plank platform, railings with gaps, a ladder (ending at the post's top: the player steps from the top rung onto the open post top), and a full slab roof raised a block so the tower's banner clears it (owner);
- **bone-and-skull rock outcrops**;
- **skulls on pikes**;
- **lean-tos** over bedrolls (below);
- the **underground room** (below).

Weeds grow only where they can survive; there is no dirt path on camp ground (it turns to dirt under builds).

### Variety (owner: "varying numbers of every element")

Each camp picks its own count of each element (zero is allowed where it reads well), so no two camps share a layout. The plan seed is mixed first (`RandomSupport.mixStafford13`), so close seeds don't share a layout.
- **Towers:** 0 to `maxTowers` (small 1, medium 2, large 3), spread round the clearing. With no tower, the first banner stands on the ground; otherwise it flies from the first tower's post and the rest stand on short posts round the camp.
- **Outcrops:** 0 to outcrops + 1 (outcrops = 1/2/3 by size). **Pikes:** 0 to outcrops + 2.
- **Lean-tos:** 1-2 (1 in small camps).
- **The stair tunnel** opens on any side, turning either way.
- **The room:** each half-width picked separately. Across the stairs: 1 to `maxRoomHalf` (small 2, medium 3, large 4); toward the stairs: 1 to 2 (large 3), so the tunnel still surfaces. Shifted up to a block off centre (never toward the stairs).
- The tunnel's mouth is cleared of trees back to the clearing. Room floors and the stair landing are always full blocks (mud becomes packed mud).
- Builder: shows the spread with screenshots of several seeds per kind (client part `camps`).

### Footprint plan (`CampPiece.Plan`)

Pieces are laid out in a footprint plan before building, so nothing overlaps.
- **Order:**
  1. the room and its stair side;
  2. reserved footprints: the fire (centre ±2: ring, seats, chest) and the stair tunnel where it opens to the surface (from step 1 of the tunnel to its cleared mouth at step 9, plus a wall column either side);
  3. big pieces: skull rock, winding tower, ribcage (Nether outposts: gate, hoglin pen);
  4. lean-tos, the stable (its own random stream), towers (+ ramp, + twin and bridge), outcrops, pikes, banners.
- **Footprints:** tower 6 x 6 (4 x 4 plus a ladder ring), lean-to 3 x 3, stable 7 x 5, outcrop (2m+3)², pike and banner 1 cell.
- A claim needs a gap of at least 1 cell (`MARGIN`) from every earlier footprint; a lean-to may sit right against the fire's (it opens onto the seats).
- Each piece tries 16 spots (first at its planned angle, then random angles; lean-tos alternate distance r-3 and r-2), then is dropped, so a camp can have fewer pieces than it rolled. Lean-tos go first, so the guaranteed lean-to always fits.
- `CampPiece.footprints()` lists the plan's footprints (tests); `CampPiece.roomSpot()` is the room's middle floor cell (camera, path checks).
- **Bounding box:** reaches the larger of radius + 5 and `maxRoomHalf` + 10 from the centre (so the stair tunnel fits; small 12, medium 13, large 16; Nether outposts 3 more), and 22 up.

### Big pieces

Prior art taken: Breath of the Wild bokoblin camps (skull rocks entered by the mouth, stilt lookouts, rickety bridges), Valheim fuling villages (large camps spread out and linked), Skyrim bandit and giant camps (plank ramps, bones and skulls round a fire), Elden Ring forts (towers with a stair winding round the outside, archers up high), Far Cry outposts (height variety and several ways up, so a camp reads as a plan from afar). The skull rock follows the owner's "rock and bone caves: large skull domes" idea, as a starting point, not a copy ("you wouldn't be able to copy them directly anyway"). Kinds keep their themes (`CampKind.woodStairs()`: spruce, dark oak, acacia, mangrove).

- **Skull rock** (large camps, 1 in 3; footprint 13 x 13, 10 blocks from the fire, facing it):
  - drawn from a hand-made 13 x 13 front grid (`CampPiece.SKULL_FACE`: bone, eye, nose, mouth), pushed 4 deep (5 at the brow), with an ellipsoid back half (radii 6.6 across, 6.2 deep, 7.0 high, centred 6.5 up); a 1-block shell;
  - detail-lite (owner): bold features only. A big cranium over a narrower jaw, a brow ledge one out over two 3 x 3 eye sockets, a nose hole, and the mouth (5 wide, 2 high) as the door. Eyes and nose are plain holes into the hollow;
  - materials (`skullBone`): the brow (dy 9, w 5) is quartz pillar across the face; otherwise 70 % bone block, 15 % calcite, 10 % smooth quartz, 5 % quartz block; base ring bone block;
  - inside: a trodden floor and a mob skull, dark; the inside floor cells are shade spots.
- **Skull yard (owner: stuff on all sides of the skull):** a skull rock gets 4-6 pieces round its sides and back (70°-290° from its front), 8-9 blocks from its middle: the first a banner, the rest skull pikes or small outcrops (`Plan.skullYard`, `SKULL_YARD_*`). Trodden ground spreads 10 round the skull's middle too, and large overworld camps' bounding box reaches 21 out to hold it.
- **Half-buried skull:** half of all skull rocks (from the skull's own seed bit), sunk 4: the cranium, brow and eyes show. The left eye (seen from the front) is the way in, with a bone-block step; the other eye stays dark.
- **Winding tower** (large camps, 1 in 4; footprint 7 x 7): a 5 x 5 log-and-plank core 12 to 14 high, a 1-wide stair winding clockwise round the outside (corner landings on log posts), log bands every 5 blocks, fence window slits, doorways off the stair, a railed top with a slab roof on posts. Archer spot at the top; the top 3 x 3 are shade spots.
- **Ribcage arch** (large camps, 1 in 3; footprint 9 x 7, 7 from the centre): a bone spine 5 up, 4 ribs each side curling in at the ground, a 3-wide path under it.
- **Ramps** (any tower, 35 %; footprint `ramp`, may touch its tower): a 2-wide stair ramp from the ground to the platform, on a side whose foot stays in the clearing; log stilts under every other step; the railing open where it arrives.
- **Bridges** (medium and large camps, 40 % per tower): a same-height twin tower 4 to 7 off one side, joined by a 2-wide plank bridge whose middle half sags to slabs.
  - Railing (`CampPiece.bridge`), in the cells beside the deck: a log beam along the bridge at deck height; at deck + 1, log posts at both ends and every 4 blocks on long bridges, with horizontal chains between.
  - If the bridge doesn't fit, the twin is dropped too. Ladders never come down the bridge side.
- **Fence tidy** (`ClippedBuild.tidyFences(pieceBox)`, after each chunk's build): removes only fences that touch no block at all (owner, 2026-11-25: a lone post next to any block is fine; the fault to avoid is a side that claims a neighbour that isn't there), sets every fence's sides from its real neighbours, and fixes fences just over the chunk edge that an earlier chunk built, including removing a ring fence left joining nothing and unjoining the fence beside it.

### Shade and the underground room (owner)

The owner asked for guaranteed shade spots and one small-to-medium underground room the mobs can go in; otherwise they burn. Camp mobs burn in daylight like any others. Every camp has:
- a **lean-to:** open toward the fire, 3 wide and 2 deep, the roof four blocks up (three blocks of headroom; the owner asked for it taller after mobs got stuck), fence posts, a plank back wall, bedrolls;
- **roofed towers:** lookouts on the platform and mobs under it are shaded;
- **one underground room** under the fire: 3 high, dark, rock walls, a beamed plank ceiling, cobwebs in the corners, a few bones, and a small chest (`chests/camp_room`: bones, arrows, rotten flesh, coal, iron and gold nuggets, string, candles; usually also an iron or gold ingot, emeralds, golden carrots, an enchanted book, sword or bow, or a golden apple; Chest tiers, below). Its size varies (Variety, above). It is reached by a 2-wide stair tunnel, and skipped where the world isn't deep enough (superflat).

**Taking cover is vanilla's own behaviour** (owner: don't reinvent it):
- skeletons, strays and bogged already have `RestrictSunGoal` and `FleeSunGoal` (they keep out of the sun and run for shade once burning); camp zombies are given the same two goals;
- husks don't burn; a helmet keeps the sun off, as in vanilla;
- a roster that appears by day appears in the shade (the room first).

**Shade code:**
- `camp/CampShade` only answers where the shade is: `sunBurns` (vanilla's rule: `isBrightOutside` and the `monsters_burn` environment attribute), `resolve` (the standable, covered height at a spot, with three blocks of headroom, since leaders are 1.25x), `covered` (blocks overhead, not sky light). Carpet (the lean-to bedrolls) counts as open.
- `CampPiece.shadeSpots()` (saved on the site as `shade`), `tunnelStep(i)`, `leanToSpot()`.

## Banners are the key (owner)

- **The camp's banners are its key blocks.** Vanilla banners, no new block (B15). **Each monster type has its own banner pattern**, readable from a distance:
  - skeleton: black with a white skull and base stripe;
  - zombie: green with a black skull and jagged hem;
  - husk: brown with a yellow skull and hem;
  - stray: light blue with a white skull and icicle fringe;
  - bogged: grey with a green murk rising and a lime skull;
  - piglin: Nether outposts, below.
- **Break every banner of the camp's kind and it stops for good.** Kill the roster but leave a banner standing, and the camp comes back.
- **Put one of the kind's banners back** anywhere in the camp's area (camp banners drop as items with their pattern) **and it's armed again**, so players can re-arm a camp on purpose, for a farm or a fight. A banner of another kind doesn't count.
- Camps are **registered per world, like cairns** (`specs/endermen.md`, CairnSites): location, type and area saved, fixed at worldgen. The game asks "does a matching banner stand in this area?" and never scans the world for banners.

## Refilling (owner)

- **No blood moon.** A camp whose roster is dead but whose banner still stands has a **random chance to reset while players are away**: checked about once per in-game day, only while nobody is within **96 blocks**, with a **1 in 3** chance per check (so usually within a few days). It never refills while someone is there.
- After a reset, the roster comes back the next time someone comes near.
- A re-armed camp resets at the next daily check for sure, so it's back the next day the player is away.

## Lighting a fire quiets a camp (owner)
<a id="lighting-a-fire-quiets-a-camp-owner-2026-11-22"></a>

- **A lit campfire (or soul campfire) anywhere in a camp's area disables it** while it burns: the roster doesn't appear and the camp doesn't reset. This is the fire ward idea (`specs/undead.md`) applied to the whole camp, and the way to shut a camp down **without breaking its banners**.
- Put the fire out (water, shovel) or break it, and the camp works again.
- It only quiets camps whose mobs fear fire (all of ours do).
- Camps generate with their own campfire **unlit**, so they never quiet themselves.
- Checked from the area's block entities, like the banners; saved as `quiet` on the camp.

## Camp patrols (owner, 2026-11-25; B21 earnedSafety; built 2026-11-25)

**Why they exist (B21):** the map stays about as dangerous as vanilla, and clearing camps or warding is how a player makes an area safer. Patrols carry a camp's danger out across the land around it, so clearing a camp quiets its whole territory. **Rate target:** a player out at night in an unsafe area (inside an armed camp's territory, not warded) should usually meet about one patrol a night.

- **Territory:** each armed camp (a banner of its kind standing, no lit fire quieting it) owns the land within `CampTuning.PATROL_TERRITORY` (**320**) blocks of its centre. Disarmed and quieted camps own none. Nether outposts and pillager outposts send no patrols (vanilla pillager patrols are unchanged).
  - **Measured** (client part `campsurvey`, or `camps`; seed 20261116, land points every 64 blocks within 1536 of spawn, every camp armed as generated): 320 covers **96.8 %** of camp-biome land and 88.8 % of all dry land (not ocean, river or beach). 256: 89.2 / 81.7 %; 384: 99.4 / 91.8 %; 448: 99.7 / 92.4 %.
  - Camps a player has never been near aren't registered yet, so the spawner reads the camp structure starts of the spread-grid cells within the territory radius of the player (once per cell per session, `CampPatrols.registerAround`) and registers them, as `camp locate` reads them. A cell whose chunk already has its structure starts is read at once; otherwise the chunk is requested with `getChunkFuture` (generated on the worldgen threads, not the main thread) and the camp registers when it arrives, so flying into new land causes no main-thread hitch; such a camp counts from the next patrol check.
- **Spawning** (our own `camp/CampPatrols`, modelled on vanilla's `PatrolSpawner`; vanilla's only drives `PatrollingMonster`s, which our mobs aren't):
  - every `PATROL_CHECK_TICKS` (600, 30 s) each non-spectator player has a `PATROL_CHANCE` (10 %) roll;
  - the player's nearest camp whose territory holds them, if its last patrol is gone and `PATROL_COOLDOWN` (6000 ticks, 5 min) has passed since it appeared;
  - burning kinds (skeleton, zombie, stray, bogged) only while the sun doesn't burn (night, `CampShade.sunBurns`); husk patrols day or night;
  - the spot: one of `PATROL_SPOT_TRIES` (10) points 24-48 blocks from the player on valid spawn ground for the kind (vanilla spawn placement), block light at most 8 (vanilla's patrol rule), not warded and no lit fire within the undead fire ward's reach (`UndeadTuning.FIRE_WARD_RADIUS`); a spot the player can't see wins, else the first seen one;
  - **wards:** no patrol for a player standing in a ward, and no member spawns in one. Totems & Artifacts only stops natural spawns, but every ward also calls `api/Endermen.keepOut`, so the check is `EndermanWards.isWarded` (no dependency on that mod). Walking into a ward is already blocked by its node-evaluator mixins;
  - gamerules: none in peaceful, or with `spawn_mobs`, `spawn_monsters` or `spawn_patrols` off (builder's choice: vanilla's patrol switch turns ours off too). `mobGriefing` doesn't matter. The game test server never runs the timer (tests call `trySpawn`).
- **Encounter estimate** from these numbers: the burning kinds' night (sky dark enough that the sun doesn't burn, roughly tick 12,500 to 23,500) is about 11,000 ticks, 18 checks; a patrol in a night with chance 1 - 0.9^18 ≈ **85 %**, a second one (after the 5-minute cooldown) about 20 %, so about **one patrol a night** on average (≈1.05), the first after about 5 minutes. A player crossing into another camp's territory can meet that camp's too. Husk land sees about twice that over a whole day.
- **Size** (leader included): small camp 2-3, medium 3-4, large 4-5 (`PATROL_SIZE_MIN` 2 + size step + 0-1). Members spread round the leader like vanilla's (each a few blocks off the last).
- **Members:** the camp's own mob type, vanilla spawn gear rolls (reason `PATROL`), no babies or chicken jockeys; tagged `bubbas_reworked_mobs.patrol` and `.patrol:<camp id>`, not on the camp roster (killing a patrol doesn't clear or count against the camp). They despawn like normal monsters; their mounts carry `.patrol.mount` and despawn too (`PatrolMountDespawnMixin`, since skeleton horses otherwise never do).
- **The banner carrier (leader, `.patrol.leader`):** an ordinary member (no leader scaling, builder's choice: like a pillager captain) carrying the camp kind's banner in its off-hand (`CampBanners.item`; owner, 2026-11-25: not on the head, where vanilla counts it as a helmet and an undead leader never burns), drop chance 0, no loot pickup (so it never swaps the banner). On death (with `mob_drops` on) it **always drops a vanilla filled map** named Monster Camp Explorer Map: centred exactly on the camp (`MapCentreInvoker`; vanilla only makes grid-aligned maps), zoom `CampPatrols.mapZoom()` = 3 for 320 (1024 blocks across), biome preview like explorer maps, the camp marked with the white X the cartographer's camp map uses. No new item.
- **Moving together** (`camp/CampPatrolGoal`, priority 4, MOVE; vanilla's `LongDistancePatrolGoal` logic): the leader's first target is the player's spot when it appeared, then a point 32-64 blocks off, pulled back inside the territory. It walks in 10-block steps (aimed a little to the side, as vanilla does; each step's spot comes from vanilla's `LandRandomPos.getPosTowards`, which never picks water, so undead patrols don't walk the sea floor) at 0.85 speed, handing each step to the members within 16 blocks, who walk at 1.0; a member with no companions left stops patrolling. A failed step tries the target directly, then a random step and a 200-tick wait (vanilla). Steps whose surface is more than 4 above or below stay at the mob's height (roofs, overhangs). They attack players they see (their vanilla targeting); no walking home. The undead fire ward (priority 1) outranks the goal, so an idle patrol veers round a lit fire.
- **Riders:** camps with a stable (`CampPatrols.campHasStable`, from `CampSite.hasStable()`: every large camp, about half of medium) send mounted patrols half the time (`PATROL_MOUNTED_CHANCE`): the leader always rides, each follower with chance `PATROL_RIDER_SHARE` (50 %). Zombie on zombie horse; skeleton and stray on skeleton horse; husk on camel husk with a parched passenger behind (vanilla 26.3's camel husk jockey; the parched counts toward the size); bogged on foot. Zombie and husk riders carry an **iron spear** half the time (`PATROL_SPEAR_CHANCE`), using vanilla's `SpearUseGoal` (zombies have it); skeleton and stray riders keep their bows (skeletons have no spear AI, builder's choice). Riders drive their mounts (client test: a mounted leader rode 8.4 blocks in 100 ticks).
- **Command:** `/bubbas_reworked_mobs camp patrol [kind] [mounted|foot]` spawns a patrol 12-20 blocks from the player now, from the nearest camp of the kind (or the camp whose territory holds the player), ignoring the timer, cooldown and time of day.
- **Code:** `camp/CampPatrols` (timer, territory, spot, spawn, map), `camp/CampPatrolGoal`, numbers in `CampTuning` (`PATROL_*`); the goal is added in `CampMemberMixin`; `Camps` registers the tick and the leader's death drop (`ServerLivingEntityEvents.AFTER_DEATH`).

## Stables (owner, 2026-11-25; built 2026-11-25)

- **Bigger camps get a stable:** every large camp and about half of medium camps (`CampTuning.MEDIUM_STABLE_CHANCE` 0.5, from the plan seed); none in small camps or Nether outposts.
  - Planned after the lean-tos and before the towers, from its own random stream (`seed ^ 0x57AB1E`), so the rest of the layout stream is unchanged; footprint `stable` 7 x 5 (`CampPiece.STABLE_HALF`, `STABLE_DEPTH`), 80 tries, its front `r - 4` to `r` from the centre, facing the fire. Every large plan in 160 sampled has one.
  - Built (`CampPiece.stable`) in the kind's wood: log corner posts, a plank back wall, a fence rail round the sides and front with a fence gate (`CampKind.fenceGate()`) in the middle facing the fire, a slab roof 4 up over the whole footprint, a levelled trodden floor, a hay bale (sometimes two) in the back corners and the tether post (a fence against the back wall) between them. The rails and post carry their sides preset, so the fence tidy keeps them across a chunk edge.
- **In it:** 1 to `STABLE_MOUNTS_MAX` (2) mounts, stocked with the roster (`CampRoster.mounts`) only when the stable has none and its tether post stands, like the hoglin pen. Each is the kind's undead mount half the time (`STABLE_UNDEAD`; `CampKind.undeadMount()`: zombie horse; skeleton horse for skeleton, stray and bogged; camel husk for husk), else a captive donkey (`STABLE_DONKEY` 0.3) or horse.
  - All are leashed to the post's knot (a player takes every lead at once by clicking the knot, vanilla), persistent, tagged livestock (`CampRoster.LIVESTOCK_TAG`), not on the roster and not counted for clearing.
  - Undead mounts don't burn: the roof keeps the sky light under it below 15 (vanilla's burn rule). Led out into the sun, a zombie horse burns as vanilla's.
- `CampSite.hasStable()` (saved as `stable`) tells patrols the camp can send riders; the stable's post is the last of `CampPiece.tetherPosts()` (saved as `pens`).

## Chest tiers (owner, 2026-11-25; built 2026-11-25)

Four chests, rising: **fire < lower room < tower < skull**. Don't overdo the top end (owner).
- **Expected values per chest** (rolls x weight share x mean count): fire: arrows 3.75, bones 2.5, iron ingot 0.38, gold ingot 0.19, emerald 0.09, enchanted bow 0.09, enchanted iron sword 0.075, golden apple 0.009, no diamonds (was 0.05). Room: iron nuggets 1.6, gold nuggets 1.3, iron ingot 0.31, emerald 0.15, enchanted book 0.077, enchanted sword or bow 0.051 each, golden apple 0.013. Tower: arrows 12, tipped arrows 2.0, iron ingot 0.8, gold ingot 0.6, emerald 0.45, bow 0.3, enchanted bow 0.2, crossbow 0.2, enchanted crossbow 0.15, spyglass 0.25. Skull: gold nuggets 3.8, gold ingot 1.6, iron ingot 1.3, emerald 1.1, one enchanted piece (sure), enchanted book 0.40, golden apple 0.27, diamond 0.10, music disc 0.08. Winding towers are in 1 large camp in 4 (about 5 % of camps), skull rocks in 1 in 3 (about 7 %), so diamonds per average camp drop from about 0.05 to about 0.007.
- Relic map shreds (Totems & Artifacts `RelicTuning`) go in the fire and room chests only, not tower or skull (owner: either is fine, left as is).
- **Fire chest** (`chests/monster_camp`): 2-4 stacks (was 3-6) of bones, arrows, rotten flesh, string, leather, bread, iron and gold ingots, an emerald, gunpowder; plus one roll (nothing 60 of 107, about 56 %) of an enchanted iron sword, axe or bow, a chain helmet or chestplate, a saddle, a name tag, an enchanted book or a golden apple. No diamonds now.
- **Lower room** (`chests/camp_room`): moved up a little to sit above the fire: its bonus roll is nothing 30 of 78 (was 50 of 86), emeralds 1-2, plus an enchanted iron sword or bow and a rare golden apple.
- **Tower chest** (`chests/camp_tower`, `CampPiece.windingChest()`): on the **winding tower's** sealed ground floor (a corner by the tower's seed, facing in), seen through the window slits; break in. Arrows, tipped arrows (slowness, poison, weakness), string, feathers, flint, iron, gold, emeralds, bread; a bow or crossbow 85 % (sometimes enchanted); a spyglass, golden carrots, an enchanted book or a name tag 58 %.
- **Skull chest** (`chests/camp_skull`, `CampPiece.skullChest()`): at the back inside the skull rock (full and half-buried), on the furthest-back hollow floor cell beside the middle path, facing the mouth; not a shade spot. Bones, arrows, gold, iron, emeralds, golden carrots; always one iron weapon, armour piece or bow enchanted at levels 5-15; and 73 % an enchanted book (levels 5-15), a golden apple, or (1 in 15) one or two diamonds. No gems. Plus, in its own roll, **8 % a music disc** (owner, 2026-11-25), one of 13 at equal odds: 13, cat, blocks, chirp, far, mall, mellohi, stal, strad, ward, 11, wait and Lava Chicken (the creeper discs plus Lava Chicken). Pigstep, Otherside, Relic, Creator and Precipice stay rarer (cairn trades, vanilla's own places).

## Room stairs fix (owner playtest, 2026-11-25; built 2026-11-25)

Mobs had trouble climbing the stair tunnel out of the lower room. Reproduced with real walks and path checks: a leader-sized zombie found no path out in about half of all camps, and normal zombies stalled when they hopped up the steps. Causes and fixes (`CampPiece.room`):
- **Headroom:** the tunnel cleared 3 blocks above each step, and its first steps were under the room wall's top and the untouched ground. A mob hops up stairs with its back half still under the lower ceiling of the step behind, so it bonked; a 1.25x leader could not step up at all. Now 4 above every step and above the doorway cell (`TUNNEL_HEADROOM`), so the tunnel opens to the sky from its first step. A room one wide toward the stairs is no longer shifted away from them, and the reserved `stairs` footprint starts at the first step, so the opening stays clear of the fire's seats.
- **Falling ground:** sand or gravel left over a tunnel cell (trodden ground over the doorway) becomes the kind's rock, so nothing falls into the doorway.
- **Bones by the door:** no bone blocks in the room's row next to the stairs.
- **The way out on uneven land:** past the top step the ground is cut or filled flush with it for 3 cells (`MOUTH_LEVEL`), then may rise at most a block a cell over the next 6 (`MOUTH_SLOPE`) back up to the land. Only earth is cut, and never under something built; the camp's own weeds there are cleared.
- Small camps whose banner found no spot in 16 tries now try 48 more a block further out (a camp must always have a banner).

## Pillager outposts become camps too (owner)
<a id="pillager-outposts-become-camps-too-owner"></a>

- Vanilla outposts spawn pillagers anywhere in their area forever (`spawn_overrides` on `minecraft:pillager_outpost`, full bounding box) and ignore banners.
- New rule: **when every banner in an outpost is broken** (the watchtower's ominous banners), **its pillagers stop spawning there**, and the area spawns as its biome normally would. **Putting any banner back** in the outpost brings them back.
- Patrols are unchanged.

## How common (owner)

**Owner's aim:** not crazy frequent, but frequent enough to replace natural spawns for grinding mobs. Natural surface spawning is cut to 15 %, so re-armable camps are where players farm skeletons and zombies. A player should usually find a camp of each type within a reasonable walk.

**Structure set `monster_camps`:** random spread, **spacing 12 chunks, separation 4** (owner, 2026-11-24: between the first 10/3 and the "two bumps down" 14/5; about 4.5 camps per km²), kept **4 chunks clear of villages** (vanilla outposts do the same; a camp once cut through a village house). A camp needs fairly flat ground (a rise of at most 6 across it, `MAX_SLOPE`), a dry centre, and its biome at the centre and four corners, so only about 15 % of tries make a camp.

**Biomes** (`has_structure/monster_camp`, 33 land biomes): plains, forests, taigas, snowy biomes, savannas, desert, badlands, swamp, mangrove, windswept hills and forest, sparse jungle, cherry grove, and highlands (windswept gravelly hills and savanna, snowy slopes, stony, jagged and frozen peaks, stony shore). No oceans, rivers, beaches, mushroom fields, deep dark or pale garden. Rough highland ground is still skipped by the slope rule; levelling it is open.

**Measured** by the client part `camps` (seed 20261116, every placement cell within 2048 blocks of spawn, 16.8 km²) **at the earlier spacing of 10**:
- 5.9 camps per km²: skeleton 2.6, zombie 2.7; husk, stray and bogged 0.2 each (they depend on how much desert, snow and swamp there is; this seed has little near spawn).
- Typical distance to the nearest camp of a kind (median over a grid of points): skeleton ~360 blocks, zombie ~340. Husk, stray and bogged ~1700-2200 ("find a desert, snowfield or swamp, then there are a few").
- Other spacings tried: 12 gave 4.5/km² (nearest ~350-400), 16 gave 2.3/km² (nearest ~500-580). Spacing 14 (2026-11-23 to 24) should have given about 3 per km². Spacing is one number in the data file to tune after playtests.

## Maps (owner)
<a id="maps-owner-2026-11-22-built"></a>

**Cartographer villagers sell maps to our places**, the way they sell ocean monument maps (shared with `specs/graveyards.md` and `specs/mob-factory.md`). Each costs a compass plus emeralds, like vanilla's:

| Map | Level | Emeralds | Marks |
|---|---|---|---|
| Graveyard Explorer Map | journeyman (2) | 8 | the nearest crypt yard, red X |
| Monster Camp Explorer Map | journeyman (2) | 8 | the nearest camp, white X |
| Fabricator Explorer Map | expert (4) | 20 | the nearest **Ancient Fabricator** itself (its teleporter pad), within 3000 blocks |

- They join vanilla's trade pools for those levels, so a cartographer offers them now and then among its usual trades.
- Fabricator office chests hold **no** Fabricator Explorer Map (owner); it is only sold by cartographers.
- **Data, vanilla systems:** structure tags `bubbas_reworked_mobs:on_graveyard_maps` (crypt_yard) and `on_camp_maps` (monster_camp); villager trades `data/bubbas_reworked_mobs/villager_trade/cartographer/{graveyard,camp,fabricator}_map.json` (vanilla `exploration_map` + `filtered` on map id; `filled_map` with an `item_name`); added to `#minecraft:cartographer/level_2` and `level_4` (`replace: false`). Map names are in the lang file. 26.3 has no map colour component, so the maps keep the default look.
- **The Fabricator map's X** (owner: a city X was wrong): our own item modifier `bubbas_reworked_mobs:fabricator_map` (`factory/FabricatorMapFunction`) finds the site with `FabricatorSites.nearest`, as `ancient_fabricator locate` does (`FabricatorTuning.MAP_SEARCH_RADIUS` 3000), then does what vanilla's `exploration_map` does (`MapItem.applyNewSavedData`, zoom 2, `renderBiomePreviewMap`, a trial-chambers X on the pad).

## Commands

- `/bubbas_reworked_mobs camp build <kind> <size> [seed]`: kinds skeleton, zombie, husk, stray, bogged, piglin; sizes small, medium, large.
- `camp reset`: runs the daily reset now, for sure, whoever is near.
- `camp list`: every known camp and outpost, armed or not, spawned or waiting, members alive.
- `camp patrol [kind] [mounted|foot]`: a patrol at you now (Camp patrols, above).
- `camp locate <kind>`: the nearest camp of that kind ("Nearest <kind> camp at x y z, N blocks away."). It walks the camp structure set's spread grid ring by ring from where it runs, up to 6144 blocks, reading only each start chunk's structure start (`CampCommand.nearest`). Vanilla `/locate structure` returns the nearest camp of any kind, so it can't find a kind.

## Builder's choices

- Camp mobs are vanilla mobs (skeleton, zombie, husk, stray, bogged) with a home radius at the camp; camp bogged don't lurk.
- A camp's banner check reads the block entities of the chunks its box touches (only while a player is within 96 blocks), not the world blocks; nothing is scanned.
- The roster spawns even with a creative player near (only spectators don't count), so building or inspecting a camp shows it.
- Re-arming makes the next daily check certain rather than spawning the roster instantly in front of the player (the "never refills while someone is there" rule).
- Leaders and helmets as above; no names, no glow.
- Patrol state (live patrol, cooldown) is kept in memory, not saved: after a restart every camp can patrol at once.
- Stable mounts: the mix is rolled per animal when stocked (level random, not the plan seed); every mount is leashed to the one post (the hoglin pen leashes only the first), so none wander out of the shade.
- The tower chest is "hidden" as "break in": the sealed ground floor has no doorway, only window slits to see it through.

## Code

- `camp/`:
  - `CampKind` (mob, banner, materials, `forBiome`), `CampTuning` (sizes and numbers), `CampStructure` (placement checks);
  - `CampPiece` (code-built layout; the plan from the seed before any world read; `towerSpots()`, `groundSpots()`);
  - `CampBanners` (patterns, `paint`, `matches`, `standsIn`);
  - `CampSite` (saved state: id = start chunk, kind or null for outposts, box, centre, size, tower and ground spots, spawned, armed, rearmed, members);
  - `CampSites` (registry as a Fabric level attachment `bubbas_reworked_mobs:camp_sites`; discovery from structure references of loaded chunks within 4 chunks of each player every second; arming; roster trigger; daily reset; outposts; `disarmedOutpostAt`);
  - `CampRoster` (spawning, leader), `CampCommand`, `Camps` (registration, tick and entity-unload hooks), `CampShade`.
  - Shared `worldgen/ClippedBuild` (chunk-clipped placing, also used by `CryptYardPiece` and trader posts).
- **Mixins:** `CampMemberMixin` (`Mob.customServerAiStep` HEAD: gives camp zombies vanilla's `RestrictSunGoal` and `FleeSunGoal` once), `OutpostSpawnMixin` (`ChunkGenerator.getMobsAt` HEAD: inside a disarmed outpost, return the biome's spawns). `BoggedLurk` skips camp mobs.
- **Roster deaths:** Fabric `ServerEntityEvents.ENTITY_UNLOAD` with a destroying removal reason (killed or discarded, so conversions count too) removes the mob from its camp. Entity tags: `bubbas_reworked_mobs.camp` and `bubbas_reworked_mobs.camp:<id>`; the leader also `bubbas_reworked_mobs.camp.leader`.
- **Data:** `worldgen/structure/monster_camp.json` (step `top_layer_modification`), `worldgen/structure_set/monster_camps.json`, `tags/worldgen/biome/has_structure/monster_camp.json`, `loot_table/chests/monster_camp.json`, `camp_room.json`, `camp_tower.json`, `camp_skull.json`.

## Milestone test objectives

- A camp's roster appears when a player comes within range, by day or night, with one larger, tougher, better-geared leader.
- A camp mob lured more than 32 blocks from home drops its target and walks back inside its home radius.
- Breaking every banner in a camp stops it for good. Placing a matching banner back in its area brings it back.
- An outpost with every banner broken spawns no pillagers. Putting one back restores them.
- Camp pieces never overlap, 32 seeds per kind and size (`CampsGameTest.campPiecesNeverOverlap`).
- A camp's kind follows the biome at its centre: desert and badlands husk, swamp and mangrove bogged, snowy biomes stray, everything else skeleton or zombie (`CampsGameTest.campKindFollowsTheBiome`).
- Skull rocks (full and half-buried), winding towers, ramps, bridges and ribcage arches: every block supported, and a zombie can walk each end to end (`skullRockCanBeWalkedInto`, `buriedSkullCanBeWalkedInto`, `windingTowerCanBeClimbed`, `towerRampCanBeClimbed`, `bridgeJoinsTwoTowers`, `ribcagePathRunsUnderTheRibs`).
- Camp fences and chains end cleanly: every fence side matches its neighbour, no fence touching no block at all, every chain end meets a chain on its axis, a fence, a wall or a sturdy face (`campFencesEndCleanly`, all kinds, large camps).
- Camp patrols (`CampPatrolsGameTest`): at night a player in an armed camp's territory gets a patrol of its kind with one banner-wearing leader (core); no patrol from a disarmed or fire-quieted camp; none for a player outside every territory or standing in a ward (`api/Endermen.keepOut`); the leader drops a filled map centred on the camp at the territory's zoom, marked, and no banner; members stay within 16 blocks of the leader while it walks (own environment `patrol_walk`, so no other test's players draw them off); a mounted patrol's leader rides its kind's mount (zombie horse, skeleton horse, camel husk with a parched). Not covered: patrols avoiding spawn spots near a lit fire (the same `FireWard.repels` check the ward uses).
- Every camp plan has a banner (on a tower or the ground) and a lean-to (`campPiecesNeverOverlap`).
- A zombie and a leader-sized zombie in a camp's room can path up the tunnel to the clearing by the fire, every kind and size (3 seeds each, and once on land rising round the camp); the two cells past the top step are ground with open air above (`zombieCanClimbOutOfTheRoom`).
- Stables: in every large plan, 30-70 % of medium plans, never small or Nether; built, they hold 1-2 mounts of the kind's undead mount or a horse or donkey, leashed to the post, persistent, under the roof, off the roster; restocked only when empty and while the post stands (`stablesHoldTetheredMounts`).
- The winding tower's chest and the skull rock's (full and half-buried) stand where planned with `chests/camp_tower` / `chests/camp_skull` and can open (`towerAndSkullChestsHoldTheirLoot`).
- 1000 rolls each: skull chests hold a music disc 40-130 times (about 80), Nether outpost rooms a netherite upgrade template 100-210 times (about 150) (`campChestsHoldRareItemsAtStatedOdds`).
- Nether outposts generate only in crimson forests and the Nether wastes (`CampsGameTest.netherOutpostBiomes`).
- An outpost's roster is piglins with a piglin brute leader; its hoglins stand in their pen, the first leashed to the post, none huntable (`outpostRosterAndPennedHoglins`).
- A piglin can walk through an outpost's gate, climb its ramps and reach its portal room (`outpostGateCanBeWalkedThrough`, `outpostRampCanBeClimbed`, `outpostPortalRoomCanBeReached`).
- An outpost's portal matches its seeded state: lit, whole and unlit, or broken (`outpostPortalsMatchTheirState`).
- Natural outposts in a real Nether (seed 20261116): in an allowed biome, no lava within radius + 1 from 8 below to 3 above, a piglin can path from the fire to the ramp's platform, the gate and down the tunnel, hoglins penned and leashed; shot from above, close, side, tunnel, gate and portal (client part `outposts`, `OutpostsClientGameTest`).

### Tests

- **Core:** `ReworkedMobsCoreGameTest.campPatrolSpawnsWithABannerLeader` (body `CampPatrolsGameTest.patrolSpawnsFromAnArmedCamp`).
- Objectives, no test yet (owner playtest, 2026-11-25): a patrol beside a lake never takes a step into the water; a skeleton patrol leader burns in full daylight like its followers.
- **Client part `patrols`** (`PatrolsClientGameTest`): at night, foot patrols (`patrol-foot-zombie`, `-skeleton`) and mounted ones (`patrol-mounted-zombie`, `-skeleton`, `-husk`), front and `-side`, close enough to see the banner and spears; prints how far a mounted leader rides in 100 ticks. Part `campsurvey` (only when named) prints just the frequency survey and patrol territory coverage.
- **Core:** `ReworkedMobsCoreGameTest.campRosterAppearsWithALeader` (body in `CampsGameTest.rosterAppearsWithALeader`): a survival player near a small skeleton camp makes 3 + 1 appear: exactly one leader, larger, tougher, iron chestplate, no banner.
- **Milestone `CampsGameTest`** (on their own deep 32 x 48 field, `camp_field`, so the camp box doesn't spill into neighbouring tests):
  - `bannersAreTheKey`: break every banner → disarmed, roster removal counted, never resets; another kind's banner doesn't arm it; put the kind's banner back → re-armed, resets at the next check;
  - `aLitFireQuietsTheCamp`: lit campfire, no roster; fire out, roster appears; lit again after clearing, no reset;
  - `campsHaveShadeAndARoom`: every shade spot is roofed, at least 4 are in the room and 2 above ground; a zombie on the tunnel's stairs can walk down into the room (`roomSpot()`);
  - `exposedCampMobsSeekShade`: at noon the roster appears in shade; a skeleton put in the sun walks into shade;
  - `outpostWithoutBannersSpawnsNoPillagers`: a registered outpost box; banner gone → disarmed and `getMobsAt` has no pillagers; banner back → armed;
  - `everyCampBlockIsSupported` (each kind): every built block passes `canSurvive` (nothing pops off on a block update), and no block in an overworld camp gives off light;
  - plus the objectives above (`campPiecesNeverOverlap`, `campKindFollowsTheBiome`, the big-piece walks, `campFencesEndCleanly`, the outpost tests).
  - Shared with crypt yards: `GravesGameTest.everyYardBlockIsSupported` (each wealth); the noble sarcophagus lid is an upper slab, so its candle stands.
- **Client part `camps`** (`CampsClientGameTest`), screenshots looked at:
  - one camp of each kind built on a flat world, from above and close (`camp-<kind>-above`/`-close`);
  - three extra seeds per kind (small, medium, large) from above (`camp-<kind>-seed<n>-<size>`);
  - `camp-roster` (skeletons round the fire, a lookout on the tower, the leader), `camp-leanto` (a skeleton in its shade at noon), `camp-roster-night`;
  - `camp-room` / `camp-room-back`: standing in a natural camp's room (`roomSpot()`), night vision;
  - stables, chests and stairs (also alone as the part `campextras`): `camp-stable-<zombie|husk|skeleton>-close` / `-clearing` (mounts stocked once the light under the new roof has settled), `camp-skull-chest`, `camp-buried-chest`, `camp-tower-chest` (night vision), `camp-stairs-up-<skeleton|husk>` (from the room's doorway, on a stone block since superflat ground is too shallow for the room);
  - the nearest natural camp in seed 20261116 (`camp-natural-0`, a skeleton camp in a forest clearing); also checks that a zombie on the surface outside it can path down into its room (target `roomSpot()`, accuracy 2);
  - the big pieces: `camp-skull-*`, `camp-buried-*`, `camp-ribcage-*`, `camp-winding-*`, `camp-ramp-*`, `camp-bridge-*` (front, 3/4, side, far 34 blocks);
  - prints the frequency survey.
- **Not covered by a test:** a natural outpost losing its banners in a generated world. The test uses a hand-registered outpost box; the mixin path and the outpost discovery are the same code the natural case uses.

## Open

- **Playtest numbers:** spacing (12 chunks), trigger range, roster sizes, the leader, camp loot: starting values above. Spacing 12 was measured at 4.5/km² (with separation 3).
- **Natural forest camps** sit among uncleared trees (leaves crowd the towers); worth a look.
- **Towers** sometimes bunch on one side of the camp.
- **Camera framing to fix:** the ribcage bogged front camera ends up inside that camp's skull; the ramp `-side` camera sits close among towers.
- **Ideas:** bridges to outcrops or skull tops, ramps up rock outcrops; nether fossil templates (`nether_fossils/fossil_*` via StructureTemplate through the plan) as bone dressing near skulls or outcrops; levelling rough ground. Ramps and towers on rough ground are only checked on flat ground and the natural camp.
- **Builder's calls to review:** the Builder's choices list above (camp bogged don't lurk, re-arming waits for the next daily check, rosters show for creative players).
- **Patrols, playtest:** the spawner reads camp structure starts within 320 blocks (forcing chunks to structure starts, as vanilla locate does); watch for hitches flying fast into new land. Patrol state isn't saved (see Builder's choices).
- **Bogged camp weeds:** brown mushrooms can fail to survive in daylight on 26.3; no fix: 26.4 stops light from breaking mushrooms (owner).
- **Nether outposts (owner to decide):** rarity (spacing 22), whether piglins may hunt the penned hoglins (now they can't), whether netherite scrap stays in the room chest. In `BACKLOG.md`.

## Nether outposts
<a id="nether-outposts-owner-2026-11-24-built-2026-11-24"></a>

**Owner's asks:**
- Nether outposts as a kind of monster camp, less common than overworld camps;
- piglins and piglin brutes, with hoglins kept as livestock (penned and tethered);
- blackstone, polished blackstone bricks, gilded blackstone, gold, crimson wood, chains, soul and regular lanterns, with the bastion as reference;
- crimson forest and Nether wastes only;
- a lower room with a Nether portal, ruined-portal style (some lit, some unlit, some broken, crying obsidian), with layout ideas from bastions and fortresses.
- **Nether only (owner):** piglins zombify in the overworld (vanilla), so there are no overworld piglin outposts.

### Kind and structure

- `CampKind.PIGLIN` (`camp locate piglin`, `camp build piglin <size> [seed]`). The same `CampPiece`, footprint plan, roster, refill and banner rules as overworld camps.
- Structure type `bubbas_reworked_mobs:nether_outpost` (`NetherOutpostStructure`): finds its own floor under the ceiling and builds the PIGLIN kind of `CampPiece`.
- Structure set `nether_outposts`: random spread, spacing 22, separation 8 chunks, salt 20261124, kept 3 chunks from `minecraft:nether_complexes` (bastions and fortresses). Terrain rejects many candidates, so outposts are much rarer than camps: seed 20261116 had 5 outposts within 2000 blocks of 5 points.
- Biome tag `has_structure/nether_outpost`: crimson_forest and nether_wastes. Not basalt deltas (too jagged for a level clearing); not soul sand valley or warped forest (piglins don't spawn there in vanilla, and warped fungus repels them).

### Placement under a ceiling

All from noise columns, before any block exists:
- The centre column is scanned for floors at least 10 above the lava sea (y >= 42): solid ground with 12 air above it, below the generation depth (so never the bedrock roof). One floor is picked at random.
- Columns every 3 blocks out to radius + 4 (the gate, rampart and pen stand at the edge) each need a floor within ±5 with 5 air above it.
- Floor spread across the outpost at most 5 (`MAX_SLOPE`).
- No lava from 9 below to 2 above any sampled floor.

### Building under a ceiling

`NetherBuild`, a `ClippedBuild` whose ground is the surface nearest the outpost's height (a lava surface counts as no ground):
- The clearing is levelled to the outpost's height (fill under, cut over, natural floor blocks kept), with headroom carved 12 up.
- Lava within radius + 1.5, from 9 below to 12 above, is sealed to blackstone (lava springs come before our generation step).
- A level walkway runs round the stair tunnel to its mouth, and a 5-wide approach leads out of the gate.

### Materials (PIGLIN)

| Part | Block |
| --- | --- |
| Posts | crimson stem |
| Platforms | crimson planks |
| Roofs | polished blackstone brick slabs |
| Railings | nether brick fence (fortress) |
| Ramps | crimson stairs |
| Tunnel stairs | polished blackstone brick stairs |
| Ground | blackstone, netherrack, soul soil, polished blackstone |
| Outcrops | polished/cracked blackstone bricks, blackstone, gilded blackstone, gold blocks jutting out instead of bones |
| Fire | unlit regular campfire on a gilded blackstone ring |
| Pikes | piglin heads, or lanterns and soul lanterns |
| Bridge deck | nether bricks, nether brick slab sag (chains still end on posts) |
| Banner | black with a yellow piglin snout and an orange triangles-bottom |

### Pieces

- **From bastions:** a gilded gate in a stretch of rampart (thick polished blackstone brick pillars capped with gilded blackstone and gold, a chiselled keystone and gold over a 3-wide arched opening, cracked brick rampart wings with blackstone wall tops, soul lanterns under the arch); the hoglin pen (a nod to bastion hoglin stables).
- **From fortresses:** nether brick fence railings, nether brick bridge decks.
- **Not in outposts:** the skull rock, winding tower and ribcage (overworld only).
- **Hoglin pen:** a closed 5 x 5 ring of crimson fence on crimson nylium and soul soil, with a crimson fungus or two; the tether post in the middle is a fence with a lantern on top.
  - Hoglins per pen: 1 small, 2 medium, 3 large (`CampTuning.OUTPOST_HOGLINS`); the first is leashed to the post.
  - All are tagged livestock and set "cannot be hunted", as in a bastion stable (mixin `HoglinHuntInvoker`).
  - They are not on the roster and don't count for clearing. They are re-made with the roster only when the pen has none left and its post still stands.
- **Portal room:** the overworld room, deeper (floor 7 down, 5 high, always 7 x 7 inside), reached by a longer stair tunnel.
  - A 4 x 5 obsidian frame stands at the back facing the stairs, the whole frame inside one chunk.
  - State from the outpost's seed: 40 % lit; 30 % whole but unlit with 1 to 3 crying obsidian; 30 % broken (1 or 2 frame blocks gone, crying obsidian, unlit).
  - Netherrack and magma at its foot; magma only beside the frame, not in the walkway.
  - A lit portal works as vanilla (using it may create an overworld portal).

### Roster, leader, light, loot

- **Roster:** piglin brutes only (owner, 2026-11-25: they attack no matter what and don't trade), each with a golden axe, lookouts included. Vanilla brute behaviour (hostile even to players wearing gold). Built outside the Nether (command, tests), piglins and hoglins are made immune to zombification.
- **Leader:** a piglin brute, scaled and toughened like overworld leaders, in golden helmet, chestplate and leggings with Protection II and a golden axe with Sharpness III; drop chances as overworld leaders.
- **Light:** outposts are lit (lanterns, a lit portal). Overworld camps stay dark; the "camps are dark" check in `everyCampBlockIsSupported` skips the piglin kind.
- **Loot:**
  - `chests/nether_outpost` (by the fire): gold nuggets and ingots, crimson fungus, raw and cooked porkchops, leather, quartz, arrows, string, golden carrots, a worn crossbow, and 1 in 4 the outpost's banner.
  - `chests/nether_outpost_room`: gold, gilded blackstone, an enchanted golden sword, helmet or chestplate, obsidian, crying obsidian, flint and steel, fire charges, porkchops, a gold block, the piglin banner pattern, a snout trim template, and rarely netherite scrap (weight 1 of about 90). Plus, in its own roll, a **netherite upgrade smithing template at 15 %** per chest (owner, 2026-11-25: another way to the template than bastions).
  - Both use 26.3's `"modifier": {"type": "minecraft:set_count"}` form (the pre-26.x `"functions"` list is ignored, so every stack would be 1); `MobFactoryGameTest.lootFilesUseCurrentKeys` checks every `bubbas_*` loot table and item modifier for old-form keys.

### Numbers for the owner to playtest

In `CampTuning`, `NetherOutpostStructure` and the structure set: spacing 22 / separation 8; floor at least 10 above the lava sea, headroom 12, slope 5; brute chance 25 %; hoglins 1/2/3; portal 40/30/30.

### Tests

- Milestone `CampsGameTest`: `netherOutpostBiomes`, `outpostRosterAndPennedHoglins`, `outpostGateCanBeWalkedThrough`, `outpostRampCanBeClimbed`, `outpostPortalRoomCanBeReached`, `outpostPortalsMatchTheirState`. The other camp tests include the piglin kind automatically: `campPiecesNeverOverlap` (gate and pen are footprints too), `campFencesEndCleanly`, `everyCampBlockIsSupported` (minus the dark check).
- Client part `outposts` (`OutpostsClientGameTest`, seed 20261116): see Milestone test objectives.
- Natural test world: `tools/build-naturalworld.py` has a "Nether outpost (piglins)" sign. It is found with `camp locate piglin` run in the Nether, its chunks are force-loaded there, the sign teleports into the Nether, and Nether chunks are pruned like overworld ones. Seed 20261122: -1032 -213 (y 63).

## Trader posts
<a id="trader-posts-built-2026-11-24"></a>

A wandering trader's market square: a striped wool stall, llama pens, a hitching rail, a signpost, a lit campfire and often a cart. Vanilla's daily wandering-trader spawn is sent to a nearby post, and its llamas arrive tied up in the pens.
- **Governing leaf:** C1 (Reworked Mobs' direction); no new leaf.
- **B15:** all vanilla blocks (wool stairs from 26.3), no new blocks.
- **Home:** Reworked Mobs, next to camps, crypt yards and Nether outposts. It shares their code-built structure machinery (`worldgen/ClippedBuild`, the chunk-clipped build and fence pass, the footprint plan, the locate-by-placement-grid search), and it is about a mob. World Tweaks has no worldgen structure code.
- **Prior art (B9):** `tools/prior-art.py` ("wandering trader post structure", "wandering trader camp", "trader camp", "wandering trader spawn"): no mod adds wandering-trader posts. Ocean/Desert Villager Trader add their own traders with structures (a different idea); "Wanderful Trades" adds trades; the rest change spawn rates or block traders. Built on vanilla's `WanderingTraderSpawner` (one injection) and vanilla blocks; only the choice of place is hand-rolled (the odds, timing, trader, llamas and despawning are vanilla's).

### Placement

- Structure `bubbas_reworked_mobs:trader_post` (step `top_layer_modification`, like camps); set `trader_posts` (random spread, spacing 18, separation 6, salt 20261124; exclusion zone 3 chunks from villages).
- Biomes `#bubbas_reworked_mobs:has_structure/trader_post`: plains, sunflower plains, meadow, savanna, savanna plateau, desert. A desert post needs a non-desert biome 48 blocks out in one of the four directions (desert edges only).
- `TraderPostStructure`:
  - nine samples (centre, corners and edge middles 10 out) must be dry and in the tag;
  - the ground may rise or fall at most 4 (`MAX_SLOPE`; at 6, posts cut deep into hillsides);
  - the floor is the mean of the nine sample heights, at or above sea level;
  - kept 2 chunks clear of camp and crypt-yard placement-grid candidates (checked from their sets' `getPotentialStructureChunk`, before their own terrain checks, so conservative): a camp's skeletons next to the trader would spoil it.
- **Density** in seed 20261116: about 0.08 posts per km² near spawn (a forest-heavy area), typically 1.5-2.5 km between posts; open plains and savanna get more. Rejections are mostly biome (13 of ~100 grid candidates near spawn were in post biomes); of those in biome, about a third pass the terrain checks.

### Layout (`TraderPostPiece`)

- **Local frame** round the centre: u across, w toward the front. The seed picks:
  - the front (4 ways) and mirror;
  - colours: primary blue or red; stripes primary/white/secondary/white, primary/secondary, or light blue/white/red/white;
  - pens: one on the left, 60 % also one on the right;
  - a cart (60 %).
- **Plaza** u -9..9, w -8..10, levelled to the post's floor height (rocky columns topped with coarse dirt); a 4-block skirt slopes a block per block back to the land; trees and plants cleared (14 up over the plaza). Soil plazas get coarse dirt patches (busier near the stall and fire), a dirt path out the front (w 4 to 14) and in front of the counter; sand plazas stay sand. Pen floors are coarse dirt and grass.
- **Footprints** (local, inclusive), all keeping a gap of at least 1 and inside the plaza:
  - stall u -3..3, w -7..-3;
  - fire u -2..2, w 1..3;
  - pens u ±5..±9, w -3..3;
  - cargo pile (one-pen posts) u 5..7, w -2..0;
  - rail u 4..8, w 6..7;
  - signpost u 2, w 6..8;
  - cart u -8..-4, w 5..8;
  - entrance banners u ±2, w 10.
- **Stall:**
  - spruce fence posts at the corners (3 high);
  - roof: back eave wool stairs (y+4, w -7), ridge wool (y+5, w -6/-5), front eave stairs (y+4, w -4), overhang stairs (y+3, w -3), wool gable ends; striped by u;
  - back wall wool (y+2..3) over barrels (one with `chests/trader_post`) and a hay bale;
  - counter (w -4): barrel, three stripped spruce logs, barrel; on it a potted blue orchid, two carpets, three lit dyed candles, a lantern;
  - a rug of carpets (w -6), a hanging lantern under the ridge, wall banners on the front posts and back wall;
  - no displayed goods on the counter (owner: players could just take them).
- **Pens:** a spruce fence ring 5 x 7, a gate toward the plaza with lanterns on the fences either side, hay (a stack of two plus one) in a back corner, two water cauldrons in the other. A post with one pen has a cargo pile of hay and barrels on the other side.
- **Rail:** two 2-high posts, a fence top bar, two water cauldrons and a hay bale in front.
- **Signpost:** stripped log 2 high, fence, lantern; waxed spruce wall signs front and back ("Trader Post / All welcome", translatable `sign.bubbas_reworked_mobs.trader_post.line1/line2` with English fallback).
- **Cart:** spruce top slabs 4 x 2, dark oak trapdoor wheels (open, on the sides), barrel (`chests/trader_post_cart`), hay, barrel, carpet; fence shaft.
- **Campfire:** lit, with two log benches (builder: unlike dark monster camps, it is a person's camp, and the light keeps the night quiet).
- **Trader banner:** base colour, yellow rhombus, zigzag hem (triangles bottom) in the other colour; red banners also a zigzag top.
- Blocks with entities are only painted when the chunk being built holds them (worldgen block entities have no level, so sign text is loaded as NBT, not `setText`).
- **Loot** (nothing that undercuts trading):
  - stall barrel: 3-5 stacks of wheat, dried kelp, bread, string, dyes, paper, now and then a lead; emeralds weight 4 of 93, one at most per pick;
  - cart barrel: 2-4 stacks of food (wheat, kelp, carrots, potatoes), maybe a hay bale or a lead.

### The trader's arrival (`TraderPosts`, `WanderingTraderSpawnerMixin`, `TraderLlamaTetherMixin`)

- **One injection** in `WanderingTraderSpawner.spawn`, at its meeting-point lookup: after vanilla has picked a player and passed its 1-in-10 roll. Vanilla's check every 20 minutes and its 25-75 % chance are unchanged.
- `TraderPosts.redirect`:
  - a village meeting point (bell) within 48 of the player wins (vanilla);
  - else the nearest post whose centre is within 96 (found from the set's placement grid and structure starts: a handful of chunk lookups, only on a successful roll);
  - `arrive` needs the post's trader spot and llama spots loaded and clear, and the biome not in `#without_wandering_trader_spawns`; otherwise vanilla places the trader as usual.
- **The trader** appears at the spot behind the counter (u 0, w -5): despawn delay 48000 (vanilla); wander target and home the post centre, home radius 12 (builder: it wanders round the post like vanilla traders round a bell, not tied to the counter).
- **Llamas:** two, `TRADER_LLAMA.spawn(EVENT)`, one per pen (or both in the one pen), leashed to a lead knot on the pen's back fence; tagged `bubbas_reworked_mobs.trader_post` and `bubbas_reworked_mobs.tether:<fence pos>`; despawn delay 47999.
- **Leaving:** vanilla keeps any trader llama leashed to something other than its trader forever. `TraderLlamaTetherMixin` makes the post's own tether count like the trader's lead, so a tethered llama counts down and leaves when its stay is up (its knot goes with it); otherwise penned llamas would pile up after every visit. A llama a player leads away (or ties up elsewhere) follows vanilla's rule again.
- **No griefing:** `spawn_overrides` for the monster category, `piece` box, empty list (vanilla's mechanism): no natural monster spawns inside. Light from lanterns, candles and the fire. Endermen already leave structure pieces alone (`EndermanTuning.STRUCTURE_MARGIN`).
- Between visits the post stands empty and intact.

### Commands

- `/bubbas_reworked_mobs trader_post build [seed]`
- `trader_post locate`: the nearest post's plaza centre and height, within 3000 blocks.
- `trader_post arrive`: brings a trader to the nearest post within 96 blocks now.
- `/locate structure bubbas_reworked_mobs:trader_post` works.

### Code

`traderpost/`: `TraderPosts` (registration, keys, `nearest`, `redirect`, `redirectTo`, `arrive`, `tetheredAtPost`), `TraderPostStructure`, `TraderPostPiece` (plan, footprints, spots: `traderSpot`, `llamaSpots`, `tetherPosts`, `gates`, `gateFronts`, `entrance`, `penInsides`, `plaza`, `at(u, dy, w)`), `TraderPostCommand`. Mixins `WanderingTraderSpawnerMixin`, `TraderLlamaTetherMixin`. Data: `worldgen/structure/trader_post.json`, `worldgen/structure_set/trader_posts.json`, `tags/worldgen/biome/has_structure/trader_post.json`, `loot_table/chests/trader_post.json`, `trader_post_cart.json`.

### Numbers for the owner to playtest

- Spacing 18 chunks, separation 6 (a candidate every 288 blocks). To make them commoner, lower the spacing or allow a slope of 5.
- Draw range 96 blocks from the player; village bells within 48 still win; the trader stays within 12 of the post.
- Loot as above.

### Milestone test objectives

- `TraderPostGameTest` (milestone):
  - `traderPostBiomes`: in: plains, sunflower plains, meadow, savanna, savanna plateau, desert; out: forest, dark forest, taiga, jungle, swamp, river, ocean, beach, snowy plains, badlands, mushroom fields, windswept hills;
  - `postPiecesNeverOverlap`: 300 seeds, gap 1, inside the plaza;
  - `everyPostBlockIsSupportedAndFencesEndCleanly`: four seeds (one or two pens, with and without a cart);
  - `postCanBeWalkedThrough`: a trader from the entrance reaches the counter and each pen gate;
  - `tetheredLlamasLeaveWhenTheirStayIsUp`: post llamas and their knots go; a trader llama tied up elsewhere stays (vanilla);
  - `vanillaPlacementWithoutANearbyPost`: no post: vanilla; a post over 96 blocks off: vanilla; a village bell within 48: vanilla.
- **Core:** `ReworkedMobsCoreGameTest.traderArrivesAtAPostWithLlamasPenned` (body `TraderPostGameTest.traderArrivesWithLlamasPenned`): with two pens and with one, the trader arrives behind the counter with vanilla's stay, two llamas penned (one per pen when two), tethered to the pen fence and counted as tethered at the post.
- **Client part `traderposts`** (`TraderPostsClientGameTest`): natural posts in seed 20261116, each checked for biome, flat dry floor, sound blocks, clean fences, no monster spawns, and a trader's walk to the counter and gates; shots `traderpost-<i>-above/-front/-left/-backright`, `traderpost-0-counter/-pen`; the daily-spawn redirect from a player 30 blocks off (`traderpost-arrival-front/-pen/-above`); a night shot (`traderpost-night`); prints the density.
- **Not covered by a test:** the injection point itself firing inside vanilla's daily tick. The test calls the same `TraderPosts.redirect` the mixin calls, and the mixin would fail to apply at startup if its target moved.
- **Natural test world:** sign "Trader post (wandering trader)" (`specs/testworld.md`).
