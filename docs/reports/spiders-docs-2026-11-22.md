<!-- dte:C1 -->
# Spiders: docs report (2026-11-22, builder: agent 3)

For the orchestrator or docs agent to merge into `specs/spiders.md` and `player-docs/`. All numbers are in `mods/bubbas_reworked_mobs/src/main/java/io/github/bubba8587/bubbas_reworked_mobs/spider/SpiderTuning.java`. **(builder)** marks the builder's choices.

## Step 1: cave nests (built; spider tests and the 31 core tests pass; screenshots checked)

### Player-facing behaviour

- **Nests:** clusters of cobwebs (9-15) clinging to the ceiling at the top of tall cave rooms, with 2-3 spiders sleeping upside down in them.
- **The drop:** walk anywhere under a nest (within 2.5 blocks of its middle) and the sleeper hanging closest above you lets go. It falls fast, head first, straight down, on a white thread tied to the nest. It doesn't steer, so keep moving and it will probably miss.
- **A miss:** it stops just above the floor (or level with you, if it fell past you) and hangs on its thread for about a second (owner, 2026-11-22). Then the thread snaps and it's an ordinary spider.
- **A hit:** a bite (4 damage), and it latches on. It climbs its thread back up into the nest with you hanging below it, biting now and then (more often and harder the longer it holds).
- **Getting free:** the same rules as a drowned's grab. Its grip starts at 50 and slowly tightens. Every hit on it loosens it (anyone's hit counts; axes, Impaling and Knockback count extra). At 0 it lets go and becomes an ordinary spider. Let go on the way up and you fall. Let go in the nest and the cobwebs catch you. Killing it frees you too. Sneaking doesn't get you off; the hint says "A spider has you! Hit it hard and fast to break free". With the `show_meters` game rule on, the grip shows as the blue bar, as for drowned.
- **Shooting a sleeper** wakes it: it drops on whoever shot it.
- **Refill:** now and then (only with no player within 12 blocks) a nest hangs a new sleeper, up to 4.
- **A nest dies** when fewer than 3 cobwebs are left around it (mined out). Its sleepers stay until they drop.
- Animals can be dropped on and reeled up too (B18). Creative and spectator players set off drops but can't be latched (the same rule as the drowned).

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

## Step 2: giant spiders and spun webs (built; 11 spider tests and 32 core tests pass; screenshots checked; the owner saw the webs)

### Owner decisions made during the build (2026-11-22, for the spec)

- **Spun webs replace the plain cobweb spans** round the cave nests and between trees. The nests keep their cobweb core.
- **Not an orb web** (no hub, spokes or spiral): "for minecraft fitting-in reasons", a tangle of thick strings stretched diagonally between many random anchor points, criss-crossing through the space. Seeded per site, so it's stable.
- **Big webs thick and clearly visible**, then "probably too thick", so they were trimmed.
- **Pixelated, not smooth:**
  - Smooth strands "look out of place next to vanilla webs" and went "much yellower in shadow".
  - The pixel version: "MUCH better, keep at this angle".
  - This replaces the spec's "fine translucent strands with a faint shine" wording.

### Player-facing behaviour

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
  - **In play:** walking through strands slows you a little (less than a cobweb); spiders aren't slowed. 3 hits bring a web down, shears cut it cleanly for 2 string, and fire burns it away.
  - **Ageing:** webs tear over time and turn ragged after about 5 in-game days. Torn strands hang loose from where they're tied.

### Numbers (SpiderTuning)

| What | Value |
|---|---|
| Giant | scale 1.75, health 40, attack 5, speed 0.3, step height 1, spawn weight 12 (monster), spacing 48 |
| Snatch and lure | sees animals 16 blocks off; lure radius 10; lure ignored for 100 ticks after a player hurt it |
| Haul and feed | larder search 14; web 4-7 above the trunk base; climb 0.15/tick; gives up hauling after 300 ticks; feeds 300 ticks, a 2-damage bite every 30 |
| Spinning | every 400-800 ticks; trunks 3-7 apart, at least 8 tall, within 8 blocks; cap 5 webs within 12 |
| Little spiders | 2 per call, cap 4, cooldown 200 ticks, scale 0.5, health 6, attack 1 |
| Tree web | 22 rays, reach 7, up to 14 strands, 3 cobweb tie-offs |
| Nest web | 1 per nest; 26 rays (mostly up), reach 11, up to 16 strands, anchored in the cobweb core |
| Strands | at least 2.5 blocks long; slow movement x0.7; 3 hits cut; 2 string from shears; ragged up to 45% over 5 days; +25% torn per hit |

### Builder's choices

- **(builder) Spun webs are an entity (`spun_web`), not blocks (B15).** The client draws each strand as a stepped line of opaque square pixels:
  - Snapped to the world grid at the loaded cobweb texture's texel size: 1/16 with vanilla, 1/32 with a 32x pack such as Faithful, so they always match the cobweb blocks beside them.
  - Coloured with the three colours of vanilla's cobweb texture.
  - Lit per pixel (sky and block light), refreshed every 2 s, with no glow floor. That fixed the yellow.
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

### Tests

- **Core:** `giantSpiderIsLargeAndSpinsBetweenTrees`.
- **Milestone (`GiantSpiderGameTest`):**
  - `snatchesLivestockIntoItsWeb`: a pig is hauled up into a cobweb larder and the giant feeds.
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
