<!-- dte:C1,B15,B16,B17 -->
# Reworked Mobs: the Boss Piglin (spec)

**Status: to build** (owner idea, 2026-11-25; the orchestrator filled the open calls with defaults the owner may change: marked *(default)*).

A fat piglin boss on a throne in Nether outposts (`specs/monster-camps.md`, "Nether outposts"). It watches while its piglins fight, then waddles off to escape when they're all dead. Comic, and the outpost's treasure carrier.

## Look (owner)

- **Fat:** a wider, rounder piglin body (belly), a bit larger than a piglin; head and snout like a piglin's.
- **Trimmed gold armour:** vanilla gold helmet, chestplate, leggings and boots, each with a vanilla trim (snout, rib or eye pattern; gold/ netherite/ redstone materials), fitted to the fat body.
- **A cape**, like a player's: crimson or red with a gold trim or a piglin-banner motif.
- **Seated** on its throne while its outpost fights.
- **Waddle:** when it runs, a short-stepped, side-to-side waddle with arms flailing a little.

## The throne (owner)

- A **2 x 2 throne chair** of vanilla blocks (B15: no new block): e.g. polished blackstone brick stairs and slabs, gilded blackstone, gold blocks, a crimson or chain back; facing the outpost's fire.
- Placed by the outpost's footprint plan like its other pieces (no overlap), in **medium and large outposts** *(default)*, the boss seated on it facing out.

## Behaviour

- **While any of its outpost's roster lives:** it sits on the throne and doesn't move or attack. It may turn its head to follow the player and shout (piglin angry/ celebrate sounds) now and then.
- **When the last roster member dies** (the outpost's roster tracking, `CampSite` members): it gets up and **flees**: keeps away from players, and heads for the outpost's **portal room** *(default)*.
  - If the outpost's portal is **lit** and the boss reaches it, it **escapes**: it walks into the portal and is gone with its loot (no drops), with portal particles and sound.
  - If the portal is unlit or broken, it keeps running and hiding round the outpost.
- **Cornered** (a player within 2 blocks for a moment): a **weak slap** *(default)*: about a piglin's punch, knockback; then it keeps fleeing.
- **Stats:** more health than a piglin brute (builder: about 60-80), slower than a piglin when fleeing (a waddle; builder picks), knockback resistance a little higher.
- **Gold:** keeps vanilla piglin traits that fit (zombifies in the overworld: built outside the Nether it's made immune, like outpost piglins; not neutral to gold-wearing players once its outpost is attacked: builder's call).
- **Respawn:** it comes back with the outpost's roster on a reset *(default)*, seated again.

## Drops *(default)*

- Its trimmed gold armour pieces (each about 1 in 2, like a leader's gear but more generous), gold ingots and nuggets, a gold block sometimes, and the **snout armor trim template** (about 1 in 3). The netherite upgrade template stays in the outpost room chest.

## Milestone test objectives

- A medium/large outpost builds a throne (no overlap with other pieces) and its roster includes a seated boss; small outposts have none.
- The boss stays seated and harmless while any roster member lives; when the last dies it stands and moves away from a nearby player.
- With a lit portal, a fleeing boss that reaches it is removed without drops; with an unlit one it stays.
- A cornered boss deals about a piglin's punch.
- Drops roll as above over many kills.
- Client: seated on the throne (front, 3/4), waddling (side), the cape, the trimmed armour, at 16x and 32x.
