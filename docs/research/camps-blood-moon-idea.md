# Camps and blood moon: the owner's earlier idea (unreviewed)

Shared by the owner on 2026-11-14 from a conversation several months earlier; the owner had not reviewed it against the current repo. Kept here as source material. The agent's integration notes are at the end.

## The idea (owner's words, condensed)

Inspired by Breath of the Wild: enemies spawn at predetermined spots and come back every blood moon (every 7 in-game days). A Minecraft mod that:
- restricts hostile natural spawning **above sea level**; underground spawning stays normal;
- except near structures (camps, graveyards, etc., an undetermined number of kinds), each of which spawns a single set of mobs, sometimes with a slightly stronger miniboss;
- has a **key block** in each structure: breaking it permanently stops that structure spawning;
- if the mobs are killed but the key block stands, they respawn at the next blood moon;
- the blood moon has effects: red atmosphere, sounds;
- uses a fairly wide proximity trigger for the initial spawn, for performance.

Earlier answers: Fabric (then "26.1"); structure generation undecided (hand-built NBT was suggested to start); blood moon with visual effects; proximity trigger yes.

## Integration notes (agent, 2026-11-14)

- It is the "monster camps" part the owner named for Bubba's Reworked Mobs (C1 reworkedMobs).
- Graveyards and crypts can use the dormant undead (sleeper blocks) as their roster: zero cost until woken, noise is the trigger, and a blood moon respawn re-places sleeper blocks in uncleared structures. No proximity spawning needed for those.
- Camps of awake mobs (pillager-style, minibosses) still need a roster spawned on approach and restored at blood moons.
- Key block: likely no new block needed: a vanilla block with a block entity (skull, banner) or the loot display block can carry a marker the clear logic reads (docs/knowledge/item-keyed-block-drops.md, approach A).
- Conflicts to resolve with the owner: spawning rule (old: none above sea level, vanilla underground; current: 15 % monster cap everywhere, rare surface ambience spawns); structure authoring (old: hand-built NBT; current spec: code-generated templates).
- The old code plan (Yarn names, 1.21.x) is obsolete; 26.3 uses Mojang names (NaturalSpawner, SavedData, etc.).
