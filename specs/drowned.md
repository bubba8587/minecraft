<!-- dte:C1,B17,B18,B19 -->
# Reworked Mobs: drowned (spec, design: not built yet)

The next segment of Bubba's Reworked Mobs (C1), after creepers and endermen. Owner's design (2026-11-16), recorded as given; open questions below.

## The lurker

- **Fewer, much stronger.** Drowned are rare, and each one is a real threat.
- **They drown you instead of hitting you.** A drowned grabs you and **drags you downward** until you drown.
- **Breaking the grip:** some enchantments and weapons are better at breaking a drowned's grip than others.
- **Creepy, like creepers:** they do unsettling things. The core one: a drowned **stands in 2-deep water, just waiting**, for you to come close to the water's edge **and turn your back**.

## Prior art

`tools/prior-art.py "drowned"` (2026-11-16): nothing does grab-and-drown. Existing drowned mods only change spawning (Conduits Prevent Drowned, No Drowned), trident drop rates, or give mobs armor (Armored Foes, on 26.3: check compatibility with stronger drowned before release).

## Open questions (owner)

Builder's suggestions in brackets; nothing below is decided.

- **D1 Where they come from** (the mod's direction: deliberate spawning, not random). [Suggestion: fixed lurk spots picked at worldgen, like cairn sites but much denser: shallow edges of rivers, lakes and beaches, a drowned waiting at each one at night. Alternatives: one per player at night near water, like watcher creepers; or vanilla's water spawns, just rarer.]
- **D2 The wait and the grab.** [Suggestion: it stands still underwater, head just below the surface, facing the shore. When a player (or anything, B18) is within about 3 blocks of the water and looking away from it, it surges and grabs. If you're looking at it, it doesn't move, and it may turn its back on you (the inverse of creeper peekaboo). What should it do while you stare at it?]
- **D3 The grip.** [Suggestion: a held player can't swim up and is pulled down and away from the shore, about a block a second; air runs out at the normal rate, then drowning damage. Breaking free uses a grip meter like the creeper's startle meter (B19: no hidden timers): damage to the drowned loosens it. Better at it: axes and Impaling (big), a Riptide trident (yanks you free outright), Knockback. Only buy time: Respiration, Water Breathing, Conduit Power.]
- **D4 Same rule for everyone (B18).** Do they drag villagers, animals drinking at the shore, other mobs? [Suggestion: yes, anything that comes to the edge. A cow vanishing into the river is a good warning sign.]
- **D5 Out of the water.** Do they ever come ashore? [Suggestion: never. Stay away from the edge and they can't touch you, but they follow along the shoreline underwater, keeping pace.]
- **D6 Tridents.** Keep vanilla's trident throwing? [Suggestion: no ranged attacks; tridents become their rare drop, held but not thrown.]
- **D7 Vanilla drowned spawns and conversion** (ocean and river spawns, zombies turning into drowned underwater). [Suggestion: off; a zombie that drowns just dies.]
- **D8 Drowned sleepers.** `specs/undead.md` lists drowned as a planned sleeper variant. Keep that, or drop it now that drowned are their own thing?

## Milestone test objectives

None yet (not built).
