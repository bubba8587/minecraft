<!-- dte:C1,B17,B18,B19 -->
# Reworked Mobs: creepers (spec, design: not built yet)

The next segment of Bubba's Reworked Mobs (C1), after the undead. Owner's design (2026-11-16), recorded as given.

## The watcher

A creeper is a **nervous watcher**, not a hunter:

1. **Motion draws it.** It notices things that **move in its field of view**, walks up to them and looks at them.
2. **It picks a moving entity to watch** (players have priority), gets close, **hides halfway behind a wall** if it can (peeking around cover), then **just watches** the entity for a while.
3. **If the entity moves out of view**, the creeper **repositions** to watch it again (players first).
4. **Sneaking up on it:** if you approach while sneaking, it **stares at you but tries to back away behind a wall and hide**.

## Startle meter (what makes it explode)

- Things that **attack it** or **move quickly near it** startle it, and startles fill an **explosion meter**. A full meter means it explodes.
- **One attack** fills it partway and it can **calm down** from that; **repeated attacks** keep building it until it blows.
- **Repeated speed** near it (sprinting, fast movement) builds the meter the same way.
- So you can approach a creeper calmly; rushing it, sprinting around it or hitting it repeatedly sets it off.

## Spawning (owner, 2026-11-16)

- **Direction for the whole mobs mod:** almost eliminate random spawning; mobs come from deliberate placement (sleepers, structures, camps), not vanilla's random spawns.
- **Creepers:** vanilla's random natural creeper spawns are off. The mod spawns its own 'natural' creepers at night, **behind cover just outside your view**, at a watching distance; **at most one at a time** (per player or per world: open question). At any point at night you could turn around fast and see one, half behind a tree in the distance, looking at you. Creepers from spawners, structures and commands are unaffected.
- **Peekaboo:** a creeper in half-hiding that you look at moves about **95% behind its cover** until you look away, then eases back out and keeps watching.

## Blindness

A **blinded** creeper can't see motion, so all of the above is disabled while it's blind (e.g. from a Blindness gem hit).

## Charged creepers

Charged creepers keep vanilla's current suicide-bomber behavior.

## Owner answers

- **C1 fuse:** a full meter starts vanilla's 1.5 s hiss and swell. During the hiss, if nothing attacks it and the target either **sneaks** or **moves outside its trigger range**, it **defuses**. Hearing the hiss, crouching and backing up keeps you safe.
- **C2 watching:** it **never stops watching a player** within aggro range (vanilla's 16 blocks). The meter still drains when nothing startles it (that's the calming down).
- **C3 fast movement:** **any living thing moving faster than a walk within about 6 blocks** startles it: panicking animals, hopping rabbits, running horses and mobs, and players sprinting, gliding (elytra/Glider) or riding; being knocked toward it (e.g. Wind Burst) counts too (B18: same rule for everyone).
- **C4 projectiles:** projectiles flying past close (near-misses) startle it, not just hits. So rapid things like snowballs set it off remotely; to kill one without an explosion, slay it in melee or one-shot it with a strong bow.
- **C5 no cover:** on open ground with nothing to hide behind, it creeps to an **uncomfortably close** distance and stares: just outside the range where you could sprint and hit it in one motion, so dashing toward it makes it back up (owner: numbers to be playtested).

## Milestone test objectives

None yet (not built).
