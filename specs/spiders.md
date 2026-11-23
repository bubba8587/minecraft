<!-- dte:C1,B15,B17,B18 -->
# Reworked Mobs: spiders (spec)

**Status:** all built 2026-11-22: cave nests with dropping sleeper spiders, giant spiders, spun webs, cocoons, walls and ceilings through Stormie's Spiders. Open items below.

Owner's design (2026-11-22). Ordinary spiders stay **mostly vanilla**. What's new is a ceiling ambush in caves, and a giant variant in old forests.

## Walls and ceilings (owner, 2026-11-22)

- **Spiders walk on walls and ceilings** through **Stormie's Spiders** (Modrinth `stormies-spiders`, LGPL-2.1+, on 26.3, a fork of Spiders 2.0): a **required dependency** of Reworked Mobs (owner chose the dependency over building our own, B9). It goes in `fabric.mod.json` `depends` and the Modrinth version's dependency list.
- Our spiders must play well with it: nest sleepers stay still until they drop; a spider that becomes normal after a missed drop scuttles off up the walls or across the ceiling; giant spiders climb trunks and along branches to their webs.

## Cave nests

- **A web nest clings to the ceiling of tall cave rooms** (worldgen). It's **decently common**, and only at the **top of tall rooms**, so there's a long drop under it. Cobweb blocks for the sticky core (owner: string looks bad in game), plus the spun webs below.
- **Sleeper spiders** hang still in the nest, like the sleeping undead (`specs/undead.md`). The nest occasionally gains a new one over time (owner: "should occasionally spawn from it"), capped per nest.
- **The drop:** when a player walks underneath, a sleeper **drops fast, vertically oriented (head down), with a thread attached** up to the nest, to hit them once.
  - **It misses:** it **hangs on its thread for about a second** at the bottom of the drop (owner, 2026-11-22: the animation looks better), then the thread breaks and it becomes a **normal spider** on the ground.
  - **It lands the hit:** it **latches on** (owner: not expected to be very likely, since it has to actually hit). It reuses the drowned grab (`specs/drowned.md`, `drowned/DrownedMind`), but pulls **upward**: it reels the victim **up the thread into the nest**, where it keeps attacking among the cobwebs.
- **Reeling backwards (owner, 2026-11-22):** a spider hauling a victim up climbs **rear-first**, facing down at its victim, and the **thread comes out of its abdomen** ("the string comes out of their butt"). The same goes for the drop and the dangle, and for a giant spider hauling livestock.
- **The thread:** a visible line from the spider to the nest while it drops and while it reels. How it's drawn is technical; vanilla's lead and fishing-line rendering are the obvious models (B17).
- What escapes a latch: the same rules as the drowned grab (hurting it enough, etc.). Builder adapts them and lists the choices.

## Spun webs (owner, 2026-11-22: "we should try, at least, to make something really outstanding looking there")

- Real designed webs, not just cobweb blocks: **a tangle of thick strings stretched diagonally between many random anchor points** (owner: not an orb web, for Minecraft fitting-in reasons), drawn as **crisp pixel art in the cobweb texture's own palette, lit by world light** (owner: smooth strands looked out of place; the pixel version "MUCH better, keep at this angle"), a little sag, torn and ragged in older webs. An entity type (not a block, so B15 doesn't apply; visual effects are fine as entities) with a custom renderer.
- Used for the **cave nests** (spanning the top of the room round the cobweb core) and the **giant spider's webs between trees** (and the webs it spins as it roams).
- Gameplay: **strands slow you like a vanilla cobweb** (owner, 2026-11-22: same stuck movement), and attacks, shears or fire cut them.
- It must stay cheap: culled when out of view, simple geometry, bounded strand counts.
- Checked from several angles, in daylight and in a dark cave, at 16x and 32x. This is a showpiece: iterate on the look until it is genuinely striking.

## Cocoons (owner, 2026-11-22)

- **A victim reeled into a nest is cocooned, not bitten to death.** The spider wraps it and **poisons** it while it does (vanilla Poison can't kill: it stops at half a heart). It applies to players and livestock alike, and to the giant spider's livestock too.
- A cocooned victim can't move. The spider **backs off and waits**, then comes back later to feed (attack).
- **Breaking free (owner): attack the cocoon from inside.** Each swing weakens it, and a sword or shears cut faster than a bare hand.
- **Others can cut a victim out** from outside, with attacks or shears, freeing a friend or a cocooned animal alive.
- **Cocooned livestock make their distress sounds loudly and often**, so you can hear a snatched animal from a distance and go after it.
- **Two cocoon shapes (owner, 2026-11-22):** **vertical** for upright victims (players, villagers, zombies), hanging head-down, and **horizontal** for four-legged ones (pigs, sheep, cows), slung lengthwise.
- The cocoon's look: wrapped in the same crisp, pixel-art silk as the spun webs, hanging from the nest or the giant spider's web.

## Giant spiders (owner, 2026-11-22)

- **A large variant:** an **extra-large spider** (vanilla's `scale` attribute, so vanilla size properties), in **old-growth taiga** (owner: "the tall forest biome with the brown grass").
- **Webs between the trees:** cobweb spans strung between trunks in the canopy and between trees, marking their territory.
- Owner's reference: a giant spider lair in a dark forest (the classic fantasy giant-spider lair). Inspiration only, not a recreation of anything.
- **Size (owner): "not too giant."** Roughly 1.5-2x a vanilla spider (builder tunes; it must still fit between the trees).
- **It roams** the old forest. It doesn't sit in one lair.
- **It spins new webs** as it goes: cobweb spans between nearby trunks and in the canopy, so its territory fills in over time (capped, so a forest isn't buried).
- **Holding pose (owner, 2026-11-22):** hauling or holding a catch, a spider stays oriented to its surface: **upside down** under a branch, canopy or web anchor (preferred stopping spots), **sideways** on a trunk. It never flips upright while holding.
- **It snatches livestock** (passive mobs: sheep, pigs, cows, chickens, goats...) and hauls them up into its webs, where they're stuck and it feeds. This reuses the grab, pulling up into the web.
- **Livestock is a distraction (owner):** it goes for a nearby animal before a player, so players can bait or lure it with livestock.
- **In a fight it summons little spiders**: smaller and weaker than vanilla spiders (owner). Builder tunes how many and how often, with a cap.
- **Drops (default until the owner decides, 2026-11-22: "idk"):** vanilla spider drops, more of them (string and spider eyes). Revisit if it wants a unique drop.

## Milestone test objectives

- Nests generate only at the tops of tall cave rooms, made of cobwebs.
- A player walking under a nest makes a sleeper drop on its thread. A miss turns it into a normal spider. A hit latches and reels the player up into the nest.
- Spun webs are hit only at their strands (`GiantSpiderGameTest.webHitsOnlyTheStrands`, also core `spunWebHitsOnlyTheStrands`): a crosshair pick through a gap, from inside or outside the span, hits nothing; aiming at a strand hits the web there; a pig behind a gap gets hit; an arrow through a gap misses and one along a strand's line hits.
- Giant spiders spawn only in old-growth taiga, at the larger scale. They spin webs between trees as they roam, snatch nearby livestock into their webs before going for players, and summon small, weak spiders when fought.

## Step 1 as built: cave nests (2026-11-22)

Player-facing: `player-docs/Projects/Bubba's Reworked Mobs/Spiders.md`. Numbers in `spider/SpiderTuning`.

### Numbers (SpiderTuning)

| What | Value |
|---|---|
| Chance a chunk has a nest | 17 % (6 tries per chunk to find a tall room) |
| Height band | y -52 to 56 |
| "Tall room" | at least 10 blocks of air under the ceiling, and open air 3 blocks out on all four sides, 2 below the ceiling |
| Cobwebs per nest | 9-15 within 3 blocks of the middle (a few hang a second block down near the middle) |
| Sleepers | 2-3 at worldgen, cap 4 |
| Trigger | a non-spectator player within 2.5 blocks horizontally, anywhere below the nest; checked every 4 ticks; 40-tick gap between drops |
| Drop | 0.9 blocks a tick, straight down; bite 4 |
| Miss | stops 1 block above the floor (or level with the target), hangs 20 ticks, then snaps |
| Reel | 0.12 blocks a tick up the thread (a 13-block room takes about 5 s) |
| Grip | starts 50, +0.2 a tick, full 100; bites 3-8 % a tick, 2-4 damage (from a fresh grip to a full one); a hit loosens it by the drowned's points (`DrownedMind.gripPoints`) x 0.45 (`GRIP_HIT_FACTOR`, see Harder to escape) |
| Refill | checked every 200 ticks, 4 % chance, only with no player within 12 blocks |

### Builder's choices

- **(builder) Sleepers are entities, not blocks.** The sleeping undead are blocks, but a spider variant would mean a new registered block (B15 needs the owner's go-ahead first) and a spider block renderer. Instead, a sleeper is a real spider entity with no AI, no gravity, persistent and asleep. It's cheap: it does nothing while asleep, and persistent mobs don't count against the mob cap. It's drawn upside down with vanilla's flip. If the owner wants block sleepers like the undead, that's a later swap.
- **(builder) A nest has an invisible "brain" entity** (`spider_nest`) at its middle. It watches the floor under it (one entity box query every 4 ticks), picks which sleeper drops, and refills. Nothing is drawn for it, and it can't be hit.
- **(builder) Trigger area and which sleeper drops:** the closest one above the player, horizontally.
- **(builder) Grip and escape rules** are copied from the drowned (start 50, rises, hits loosen it), with spider numbers: slower rise, bites instead of air loss, hits count for less (x0.45). Riptide isn't a way out (no water).
- **(builder) After any let-go, a miss or a lost victim** the spider turns into a vanilla spider, persistent (it doesn't despawn), and targets the player it held.
- **(builder) Nest spiders drop vanilla spider loot.**
- **(builder) The victim hangs below the spider,** head just under it.
- **(builder) A sleeper saved mid-drop or mid-hold** goes back to sleep where its thread is tied, and the victim is not kept.
- **(builder) The thread** is a straight off-white line, drawn like vanilla's fishing line (B17).
- **(builder) Worldgen:** placed in `UNDERGROUND_DECORATION`, overworld only, every overworld biome (deep dark included).

### How it's built

- `spider/NestSpider` (extends vanilla `Spider`, implements `grip/Gripper`): phases SLEEPING, DROPPING, DANGLING, REELING and HOLDING, synced to the client with the thread's anchor. It moves itself during the drop and reel (no AI means no physics of its own).
- `spider/SpiderNest`: the brain entity.
- `spider/SpiderNestFeature`: worldgen; it searches columns top-down for a ceiling with a long drop under it.
- `grip/Gripper`: a marker interface, so the shared grip mixins apply to spiders as well as drowned: `PlayerGripMixin`, `ProjectileUtilGripMixin`, `HeldVictimPoseMixin` and `GrabbedHintMixin`. These four drowned-era mixins now check `Drowned || Gripper`.
- Client: `client/spider/NestSpiderRenderer` (vanilla spider model; upside down asleep; head down dropping and dangling; head up reeling; the thread line). The brain uses `NoopRenderer`.
- Entity types `bubbas_reworked_mobs:nest_spider` (name "Spider") and `bubbas_reworked_mobs:spider_nest`. Loot table `entities/nest_spider` is a copy of vanilla's spider table.

### Tests

- Core: `spiderSleeperDropsLatchesAndReelsUp` (a survival player under a nest is dropped on, latched, bitten and reeled up more than 8 blocks into the nest).
- Milestone (`SpiderGameTest`):
  - `aMissTurnsItIntoAVanillaSpider` (hangs on its thread first, then becomes a vanilla spider with AI).
  - `hitsBreakTheGrip` (four iron axe hits free you; it turns vanilla).
  - `nestsGenerateAtTheTopOfTallRooms` (cobwebs, one brain and at least 2 sleepers in a 14-high room; none under a 5-high ceiling).
- Client part `spiders`: spider-nest, spider-nest-close, spider-drop, spider-dangle, spider-latch, spider-held, spider-held-below. Checked by eye on 2026-11-22:
  - Sleepers hang upside down in the cobwebs.
  - The dropping spider points head down, and the reeling one head up, with the thread to the nest.
  - The missed spider dangles above the floor.
  - The pig hangs below the spider as it's reeled up, and is held in the nest among the webs.
- Milestone objectives (no test yet):
  - Refill adds a sleeper only with no player near, up to the cap.
  - A mined-out nest's brain goes away.
  - A shot sleeper drops on its shooter.

## Step 2 as built (2026-11-22): giant spiders, spun webs, Stormie's, cocoons

#### Owner decisions made during the build (2026-11-22)

- **Spun webs replace the plain cobweb spans** round the cave nests and between trees. The nests keep their cobweb core.
- **Not an orb web** (no hub, spokes or spiral): "for minecraft fitting-in reasons", a tangle of thick strings stretched diagonally between many random anchor points, criss-crossing through the space. Seeded per site, so it's stable.
- **Big webs thick and clearly visible**, then "probably too thick", so they were trimmed.
- **Pixelated, not smooth:**
  - Smooth strands "look out of place next to vanilla webs" and went "much yellower in shadow".
  - The pixel version: "MUCH better, keep at this angle".

#### Player-facing behaviour

- **Giant spiders** live in old-growth pine and spruce taiga. About 1.75x a normal spider (vanilla's scale attribute); they still fit between the trees. They:
  - spawn like other monsters, in the dark, and never within 48 blocks of another giant;
  - roam the forest;
  - every 20-40 s while roaming, spin a web between two nearby tree trunks. They stop once 5 webs are within 12 blocks;
  - snatch livestock: any passive animal they reach (pig, sheep, cow, chicken, goat; not tamed pets) is grabbed and hauled up a trunk into a web. It's stuck there in a cobweb while the giant feeds;
  - go for nearby livestock before players, and turn from a player to an animal close by. Not if that player hurt them in the last 5 s;
  - call 2 little spiders when hurt: half size, 3 hearts, a weak bite. At most 4 alive, and a 10 s wait between calls;
  - take no fall damage (they come down on silk);
  - drop more string (3-6) and spider eyes (1-3) than a vanilla spider. This is the default until the owner decides on a unique drop.
- While a giant hauls an animal, hitting it loosens its grip (the drowned rules). At 0 it drops the animal.
- **Spun webs:**
  - **What they are:** a tangle of strands between points on trunks, branches, leaves, the ground, and cave ceilings and walls.
  - **How they look:** drawn in pixels in the cobweb texture's own white and greys, lit like the cobwebs around them. A few strands are heavier guy-lines, two pixels wide.
  - **Tie-offs:** where a giant's strands meet a trunk, it ties them off with a real cobweb block. A cave nest's tangle runs out from its cobweb core to the ceiling and walls.
  - **In play:** walking through strands slows you exactly like a cobweb block; spiders aren't slowed. 3 hits bring a web down, shears cut it cleanly for 2 string, and fire burns it away.
  - **Ageing:** webs tear over time and turn ragged after about 5 in-game days. Torn strands hang loose from where they're tied.

#### Numbers (SpiderTuning)

| What | Value |
|---|---|
| Giant | scale 1.75, health 40, attack 5, speed 0.3, step height 1, spawn weight 12 (monster), spacing 48 |
| Snatch and lure | sees animals 16 blocks off; lure radius 10; lure ignored for 100 ticks after a player hurt it |
| Haul and feed | larder search 14; web 4-7 above the trunk base; climb 0.15/tick; gives up hauling after 300 ticks; feeds 300 ticks, a 2-damage bite every 30 |
| Spinning | every 400-800 ticks; trunks 3-7 apart, at least 8 tall, within 8 blocks; cap 5 webs within 12 |
| Little spiders | 2 per call, cap 4, cooldown 200 ticks, scale 0.5, health 6, attack 1 |
| Tree web | 22 rays, reach 7, up to 14 strands, 3 cobweb tie-offs |
| Nest web | 1 per nest; 26 rays (mostly up), reach 11, up to 16 strands, anchored in the cobweb core |
| Strands | at least 2.5 blocks long; slow like a vanilla cobweb (`WEB_STUCK`, Weaving case too); 3 hits cut; 2 string from shears; ragged up to 45% over 5 days; +25% torn per hit |

#### Builder's choices

- **(builder) Spun webs are an entity (`spun_web`), not blocks (B15).** The client draws each strand as a stepped line of opaque square pixels:
  - Snapped to the world grid at the loaded cobweb texture's texel size: 1/16 with vanilla, 1/32 with a 32x pack such as Faithful, so they always match the cobweb blocks beside them.
  - Coloured with the three colours of vanilla's cobweb texture.
  - Lit per pixel (sky and block light), refreshed every 2 s, with no glow floor. That fixed the yellow.
- **The drop thread** (orchestrator, passing on the owner's rule): drawn the same way as the webs, as a crisp one-texel pixel line in the cobweb palette, lit by world light, taut from the spider's back to where it's tied in the nest. It replaces the smooth thin line. Shared code: `client/spider/PixelStrands`.
- **(builder) How strands are chosen:**
  - Random rays from the site's centre find anchors on blocks.
  - Strands join pairs of anchors at least 2.5 apart, with a clear line between them, passing near the centre, so they cross each other.
  - Each anchor holds at most 3 strands; 30% are heavy two-pixel strands.
  - Strands sag about 6% of their length, less when steep.
- **(builder) Larders:** with no web of its own in reach, a giant makes a cobweb by the nearest trunk, 4-7 blocks up. The catch sits in that cobweb block, stuck in it (vanilla behaviour).
- **(builder) Trunk finding** scans for log columns from the spider's level up, with no heightmaps, so overhangs and canopies don't fool it.
- **(builder) Little spiders** are vanilla spiders with smaller scale, health and attack attributes, tagged so the cap can count them. They despawn like normal mobs.
- **(builder) By day,** giants attack players only in the dark, like vanilla spiders, but take livestock any time. The spec's "what it does by day" stays open.
- **(builder) Webs are never cleared** except by players, fire or explosions. The area cap keeps them from piling up.

#### Tests

- **Milestone** (moved out of core 2026-11-22): `giantIsLargeAndSpinsBetweenTrees`.
- **Milestone (`GiantSpiderGameTest`):**
  - The snatch is checked in `CocoonGameTest.spiderComesBackToFeed`: a pig is hauled up into a cobweb larder and cocooned there, the giant waits, then feeds.
  - `livestockLuresItOffAPlayer`: hunting a player, it switches to a pig close by (or snatches it).
  - `livestockLuresItOffAPlayer`.
  - `hurtCallsLittleSpiders`: 2, then 0 during the cooldown; small and weak.
  - `websSlowAndCut`: touching a strand slows; 3 hits cut it; shears give 2 string at the player.
- **Client part `webs` (`-PclientTests=webs`):**
  - Scenes: a five-trunk spruce clearing with three giant webs (close, far, under the canopy, at an angle, torn) and a dark cave nest (torchlight, night vision, close), at 16x and 32x.
  - All screenshots looked at. The owner saw trees close, trees far and the torch-lit cave.
- **Milestone objectives (no test yet):**
  - Giants spawn only in old-growth taiga.
  - Spinning stops at the area cap.
  - Fire burns a web.
  - A broken grip during a haul drops the animal.
  - Webs tear with age.

### Stormie's Spiders, rear-first reel, fall fix (spiders branch)

- **Fall-damage fix:** the released spider takes no fall damage (fall distance reset on the thread and at release); checked in `aMissTurnsItIntoAVanillaSpider` (the released spider lands at full health).
- **Stormie's Spiders is wired in.**
  - Reworked Mobs now declares `"depends": {"stormiespiders": ">=3.3.1"}`, and the build compiles against it.
  - `gradle/stormies-spiders.gradle` is pinned to the Modrinth version ID `jTOAPFV5`. Pinning matters because `3.3.1-fabric` is also the version number of a 1.21.1 build, and Modrinth's maven serves that one.
  - Dev runs need a workaround. The jar carries a root `log4j2.xml`, so on the runtime classpath Fabric Loader treats it as a game library and then reports "needs fabricloader, which is missing". So the script keeps it off the classpath and loads it with `-Dfabric.addMods`.
  - The script is applied by Reworked Mobs, by Totems with `-PwithMobs`, and by `testworldshots`.
  - A normal mods folder doesn't need any of this.
- **Nest sleepers stay ours until they turn vanilla.** NestSpider overrides Stormie's climber methods to report upright with no attachment, so Stormie's doesn't move or rotate them. Checked in screenshots: they hang still and upside down, and the drop, dangle, latch and reel are unchanged.
- **Hitbox note:** with Stormie's loaded, a spider's hitbox is narrower, about 0.95 wide instead of 1.4. So drops are a bit harder to land.
- **Rear-first reel (owner):**
  - The nest spider stays head-down for its whole time off the ceiling. The thread leaves its abdomen, and the victim hangs below its head. Checked in screenshots.
  - A giant hauling a catch up a trunk is posed the same way, with a pixel thread to the web. Its task and larder are now synced to the client.
  - A client mixin into Stormie's `ClientEventHandlers.storeClimberDataForRenderState` skips Stormie's pose during that climb only.
- **Strands slow you like a cobweb block (owner):** exactly vanilla's `WebBlock` multiplier, including the Weaving effect case.

### Cocoons, giant perch pose, two cocoon shapes (spiders branch)

- **Cocoons (owner).** New entity `silk_cocoon`. A nest spider that reels its victim up, and a giant that hauls a catch to its perch, wraps it instead of biting it to death.
  - The victim rides the cocoon and can't move or sneak off. It shows the hint "Cocooned!" (trimmed per the owner).
  - Wrapping poisons the victim. Vanilla Poison never takes the last half heart.
  - The spider backs off and waits 600 ticks, then comes back and bites: 3 damage every 40 ticks. Those bites can kill.
    - The nest spider waits upside down on the ceiling, then hangs head down to feed.
    - The giant backs off along the underside, or up the trunk, then returns.
  - Cutting: the victim hits from inside, or anyone hits from outside. Silk is 300 (raised from 100, see Harder to escape).
    - Points per hit: hand 13 (about 24 hits), other items 18, axe 28, sword 35 (9 hits), shears 40. Shears used from outside cut 50.
    - At 0 the victim drops out alive with the poison cleared.
    - Fire and explosions burn or blow the cocoon open.
  - Cocooned livestock cry their ambient sound at volume 2.5 every 30-70 ticks.
  - A nest spider whose victim gets away turns vanilla if a player is near, else goes back to sleep. A giant whose victim gets away goes back to roaming. Hitting a waiting or feeding nest spider makes it drop and fight.
- **Two cocoon shapes (owner).** Animals, and anything wider than it is tall, are slung lengthwise in a horizontal cocoon from two threads. Upright victims (players, villagers, zombies) hang head down in a vertical cocoon tied at the feet.
  - Both are drawn in the same pixel silk as the webs, and the silk thins as it's cut.
- **Giant pose (owner).**
  - The perch prefers an underside: the highest spot beside the trunk with leaves or wood above and room below for the cocoon. There the giant hangs upside down with the cocoon below. Without one, it clings head down on the trunk, belly to the bark.
  - It never flips upright while climbing, waiting or feeding. We pose it ourselves then, with Stormie's render pose skipped.
  - Busy giants no longer despawn (`requiresCustomPersistence` while it has a task), so they never leave a cocoon behind.
- **Stormie's Spiders and the Fabricator.**
  - With Stormie's loaded, a Fabricator's deactivated spider stuck to the walls instead of dropping onto its pad.
  - New mixin `StormieDeactivatedSpiderMixin` (Spider, priority 1500) makes Stormie's merged `onTravel` step aside for deactivated mobs.
  - That fixed `fabricatorDropsDeactivatedMob`.
- **Shear string** now falls where the web was cut, not at the player.
- **Tests.**
  - New milestone class `CocoonGameTest`:
    - `swordCutsFreeFasterThanAHand`
    - `anotherPlayerCutsAPigFree`
    - `poisonNeverKills`
    - `spiderComesBackToFeed`
  - The latch and snatch tests now expect a cocoon.
  - Spider tests run in their own test environment `bubbas_reworked_mobs-test:spiders`, so they don't shift the big Fabricator tests' layout.
- **Screenshots.** Client parts `spiders` and `webs`, all looked at:
  - cocoon-player-outside and cocoon-player-outside-back: a player head down in a vertical cocoon;
  - spider-held: a pig slung horizontally under its nest;
  - giant-haul-rear-first: on the trunk, head down, sheep below;
  - giant-cocoon-sheep-under-canopy and giant-cocoon-sheep-from-below: upside down under the canopy, sheep slung horizontally below.

### Harder to escape (owner, 2026-11-22)

- **Owner's words:** "spiders should be a lot harder to break free of (unless you kill them) as should cocoons, the reason being that they don't kill you until after a little while. so if you can break free and survive the fall, that's where the skill comes into play."
- **The grip.** Each hit on a spider holding you now loosens its grip by the drowned's points × `SpiderTuning.GRIP_HIT_FACTOR` = 0.45. The grip still tightens while it reels you up. This applies to nest spiders and to a giant hauling its catch.
  - From a fresh grip, an iron axe takes 3 full hits and a sword about 6 quick ones. A drowned lets go after 1-2 axe hits.
  - You come loose well up the thread. The test checks you're freed more than 5 blocks up.
  - Killing the spider is unchanged: an axe kills a nest spider in 2 hits.
- **Cocoons.** Silk went from 100 to 300: about 24 fist hits or 9 sword hits; shears are still fastest. The feeding wait stays 30 s.
- **Tests.** `hitsBreakTheGrip` now swings a full iron axe at its real pace against a spider made tough enough not to die first. It checks that it takes at least 3 hits and that the victim is freed high.

## Open

- **Giant spider drop:** vanilla drops, more of them (default); a unique drop later? (owner: "idk").
- **Giants by day:** builder made them attack players only in the dark (like vanilla) but take livestock any time; owner to confirm.
- **Block vs entity sleepers:** nest sleepers are entities (builder's call, avoids a new block under B15); fine?
- **Playtest numbers:** nest frequency, trigger area, drop speed, reel speed, refill rate, grip and cocoon toughness (tables above).
- **Still to do:** a screenshot of Stormie's wall and ceiling climbing (its navigator refuses scripted paths; bait it with prey at night).
- **Milestone objectives without tests:** listed under each step's Tests.

## Waiting by the cocoon (owner, 2026-11-22)

- While a giant spider waits to come back and feed, it no longer sits still. Every `WAIT_PACE_TICKS` (50) it creeps to a new spot up to `WAIT_PACE_RADIUS` (2 blocks) from where it backed off to: along the overhang when perched under one, up the trunk otherwise. It only moves where its body fits (`noCollision`). It still feeds on time.
- Not yet: true wandering off the perch (vanilla stroll plus `restrictTo`), and hauling the catch to the centre of its own web (BACKLOG item 6).

## Strand hitboxes (2026-11-23)

- The `SpunWeb` entity box still spans all its strands, but only for culling and entity searches (`getEntities`). Hits never use the box.
- Hit test: `SpunWeb.strandClip(from, to)` finds the closest points between the ray segment and each strand segment; a hit within `SpiderTuning.WEB_PICK_RADIUS` (0.3), hit point where the ray enters that radius.
- `mixin/ProjectileUtilWebMixin` hooks vanilla's three entity picks in `ProjectileUtil`: the view pick (`getEntityHitResult(Entity, ...)`, used by the client crosshair in `LocalPlayer.pick`: melee, shears, use); the move pick (`getEntityHitResult(Level, ...)`: arrows, throwables, fireballs, rockets); and `getManyEntityHitResult` (attack-range weapons). For a web the box clip is replaced by the strand clip, and both the pick-from-inside-the-box check and the margin hit are off.
- Not multipart: 26.3's parts are specific to the ender dragon (`EnderDragonPart`, the level's dragon-part list), not a general API, and up to 16 strand parts per web would add entities.
- Open (cosmetic): F3+B debug hitboxes still draw the whole span box.
