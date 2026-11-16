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

## Blindness

A **blinded** creeper can't see motion, so all of the above is disabled while it's blind (e.g. from a Blindness gem hit).

## Charged creepers

Charged creepers keep vanilla's current suicide-bomber behavior.

## Open questions (owner)

- See the conversation; answers land here.

## Milestone test objectives

None yet (not built).
