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
