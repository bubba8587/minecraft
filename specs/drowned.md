<!-- dte:C1,B17,B18,B19 -->
# Reworked Mobs: drowned (spec, design agreed 2026-11-16: not built yet)

The next segment of Bubba's Reworked Mobs (C1), after creepers and endermen. Owner's design (2026-11-16), recorded as given; open questions below.

## The lurker

- **Fewer, much stronger.** Drowned are rare, and each one is a real threat.
- **They drown you instead of hitting you.** A drowned grabs you and **drags you downward** until you drown.
- **Breaking the grip:** some enchantments and weapons are better at breaking a drowned's grip than others.
- **Creepy, like creepers:** they do unsettling things. The core one: a drowned **stands in 2-deep water, just waiting**, for you to come close to the water's edge **and turn your back**.

## Prior art

`tools/prior-art.py "drowned"` (2026-11-16): nothing does grab-and-drown. Existing drowned mods only change spawning (Conduits Prevent Drowned, No Drowned), trident drop rates, or give mobs armor (Armored Foes, on 26.3: check compatibility with stronger drowned before release).

## Owner answers (2026-11-16)

- **D1 Spawning: natural spawns** in water within range of a player, at the same low rate as other undead (the monster cap cut, `specs/undead.md`), plus **zombies that drown still turn into drowned** (we control zombie numbers already). A drowned doesn't have to spawn at its lurk spot: it can spawn deeper and **swim to a lurk spot**, a shallow spot near the shore where **a standing drowned's head just breaks the surface** (owner: "a 2*1-deep spot, or a spot 1*2 deep"; builder's reading: water 2 deep over a floor, by the shore; the owner may correct). There it **floats with just its head a little above the water**.
- **D2 Staring:** if you stare at it, it **sinks right below the water** (the creeper peekaboo mechanic, in reverse: it hides by sinking). It waits for you to come close to the water and turn your back.
- **On land:** a **sidewinding, staggered crawl** animation.
- **The grab:** once it has you it **only moves backwards**, dragging you toward **the deepest water**. Now and then it hits you for a small amount of damage, and **that hit makes your breath drain faster**.
- **D3 Breaking the grip:** a grip-break meter (like the creeper's startle meter, B19: no hidden timers). **Lots of fast hits, or hits with the right things, build it**: axes and Impaling big, Riptide yanks you free, Knockback helps; Respiration, Water Breathing and Conduit Power only buy time.
- **D4 Who:** they grab **players and passive mobs**.
- **D6 Tridents:** no trident throwing; **tridents move into buried treasure loot** as a rare item.
- **D7 Vanilla:** zombie-to-drowned conversion stays; a low rate of random natural spawns stays (like the other undead).
- **D8:** **no drowned sleepers** (dropped from the undead plan).

## Still open (builder decides, owner may change)

- When a drowned leaves the water: [builder: only to reach something it's grabbing near the edge, a few blocks up the bank, then it crawls back dragging it].
- How much stronger: health, armor, drag speed, the grip-break meter's numbers.

## Milestone test objectives

None yet (not built).
