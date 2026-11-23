<!-- dte:C1,B15,B17,B18 -->
# Reworked Mobs: spiders (spec, design 2026-11-22, not built)

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

- Real designed webs, not just cobweb blocks: **large spans drawn as strands between distant anchor blocks**: radial spokes from a hub plus a spiral, a little sag, fine translucent white strands with a faint shine, torn and ragged in older webs. An entity type (not a block, so B15 doesn't apply; visual effects are fine as entities) with a custom renderer.
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

## Open

- Giant spider: a unique drop (maybe later), how many roam a forest, what it does by day, and whether webs it spins are ever cleared.
- Numbers: nest spacing, the trigger area under the nest, drop speed, latch odds, reel speed, nest refill rate.

## Milestone test objectives

- Nests generate only at the tops of tall cave rooms, made of cobwebs.
- A player walking under a nest makes a sleeper drop on its thread. A miss turns it into a normal spider. A hit latches and reels the player up into the nest.
- Giant spiders spawn only in old-growth taiga, at the larger scale. They spin webs between trees as they roam, snatch nearby livestock into their webs before going for players, and summon small, weak spiders when fought.

## Step 1 as built: cave nests (2026-11-22, agent 3)

Player-facing: `player-docs/Projects/Bubba's Reworked Mobs/Spiders.md`. Numbers in `spider/SpiderTuning`. The spun webs (thick diagonal strands between random points, owner) and giant spiders are step 2, not built yet.

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
| Grip | starts 50, +0.2 a tick, full 100; bites 3-8 % a tick, 2-4 damage (from a fresh grip to a full one); hit points as the drowned's (`DrownedMind.gripPoints`) |
| Refill | checked every 200 ticks, 4 % chance, only with no player within 12 blocks |

### Builder's choices

- **(builder) Sleepers are entities, not blocks.** The sleeping undead are blocks, but a spider variant would mean a new registered block (B15 needs the owner's go-ahead first) and a spider block renderer. Instead, a sleeper is a real spider entity with no AI, no gravity, persistent and asleep. It's cheap: it does nothing while asleep, and persistent mobs don't count against the mob cap. It's drawn upside down with vanilla's flip. If the owner wants block sleepers like the undead, that's a later swap.
- **(builder) A nest has an invisible "brain" entity** (`spider_nest`) at its middle. It watches the floor under it (one entity box query every 4 ticks), picks which sleeper drops, and refills. Nothing is drawn for it, and it can't be hit.
- **(builder) Trigger area and which sleeper drops:** the closest one above the player, horizontally.
- **(builder) Grip and escape rules** are copied from the drowned (start 50, rises, hits loosen it with the same points), with spider numbers: slower rise, bites instead of air loss. Riptide isn't a way out (no water).
- **(builder) After any let-go, a miss or a lost victim** the spider turns into a vanilla spider, persistent (it doesn't despawn), and targets the player it held.
- **(builder) Nest spiders drop vanilla spider loot.**
- **(builder) The victim hangs below the spider,** head just under it.
- **(builder) A sleeper saved mid-drop or mid-hold** goes back to sleep where its thread is tied, and the victim is not kept.
- **(builder) The thread** is a straight off-white line, drawn like vanilla's fishing line (B17).
- **(builder) Worldgen:** placed in `UNDERGROUND_DECORATION`, overworld only, every overworld biome (deep dark included).

### How it's built (for the spec)

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

