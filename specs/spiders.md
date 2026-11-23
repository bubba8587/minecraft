<!-- dte:C1,B15,B17,B18 -->
# Reworked Mobs: spiders (spec, design 2026-11-22, not built)

Owner's design (2026-11-22). Ordinary spiders stay **mostly vanilla**. What's new is a ceiling ambush in caves, and a giant variant in old forests.

## Cave nests

- **A web nest clings to the ceiling of tall cave rooms** (worldgen). It's **decently common**, and only at the **top of tall rooms**, so there's a long drop under it. Cobweb blocks for the sticky core (owner: string looks bad in game), plus the spun webs below.
- **Sleeper spiders** hang still in the nest, like the sleeping undead (`specs/undead.md`). The nest occasionally gains a new one over time (owner: "should occasionally spawn from it"), capped per nest.
- **The drop:** when a player walks underneath, a sleeper **drops fast, vertically oriented (head down), with a thread attached** up to the nest, to hit them once.
  - **It misses:** it **hangs on its thread for about a second** at the bottom of the drop (owner, 2026-11-22: the animation looks better), then the thread breaks and it becomes a **normal spider** on the ground.
  - **It lands the hit:** it **latches on** (owner: not expected to be very likely, since it has to actually hit). It reuses the drowned grab (`specs/drowned.md`, `drowned/DrownedMind`), but pulls **upward**: it reels the victim **up the thread into the nest**, where it keeps attacking among the cobwebs.
- **The thread:** a visible line from the spider to the nest while it drops and while it reels. How it's drawn is technical; vanilla's lead and fishing-line rendering are the obvious models (B17).
- What escapes a latch: the same rules as the drowned grab (hurting it enough, etc.). Builder adapts them and lists the choices.

## Spun webs (owner, 2026-11-22: "we should try, at least, to make something really outstanding looking there")

- Real designed webs, not just cobweb blocks: **large spans drawn as strands between distant anchor blocks**: radial spokes from a hub plus a spiral, a little sag, fine translucent white strands with a faint shine, torn and ragged in older webs. An entity type (not a block, so B15 doesn't apply; visual effects are fine as entities) with a custom renderer.
- Used for the **cave nests** (spanning the top of the room round the cobweb core) and the **giant spider's webs between trees** (and the webs it spins as it roams).
- Gameplay (builder proposes): walking through strands slows you a little (lighter than a cobweb block), and attacks, shears or fire cut them.
- It must stay cheap: culled when out of view, simple geometry, bounded strand counts.
- Checked from several angles, in daylight and in a dark cave, at 16x and 32x. This is a showpiece: iterate on the look until it is genuinely striking.

## Giant spiders (owner, 2026-11-22)

- **A large variant:** an **extra-large spider** (vanilla's `scale` attribute, so vanilla size properties), in **old-growth taiga** (owner: "the tall forest biome with the brown grass").
- **Webs between the trees:** cobweb spans strung between trunks in the canopy and between trees, marking their territory.
- Owner's reference: a giant spider lair in a dark forest (the classic fantasy giant-spider lair). Inspiration only, not a recreation of anything.
- **Size (owner): "not too giant."** Roughly 1.5-2x a vanilla spider (builder tunes; it must still fit between the trees).
- **It roams** the old forest. It doesn't sit in one lair.
- **It spins new webs** as it goes: cobweb spans between nearby trunks and in the canopy, so its territory fills in over time (capped, so a forest isn't buried).
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
