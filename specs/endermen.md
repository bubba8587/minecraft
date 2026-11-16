<!-- dte:C1,B17,B18,B19,D2 -->
# Reworked Mobs: endermen (spec, design: not built yet)

Owner's design (2026-11-16), being worked out in conversation. Endermen are the world's **caretakers and builders**.

## Two jobs

1. **Repair:** they pick up and put back blocks so the land drifts back toward how it was first generated. The game can work out, from the world seed, what block originally generated at any position, so endermen know what "right" looks like without tracking every block anyone placed.
   - Example (owner): they intentionally jump on and break **farmland back to dirt** if the original block there was dirt.
2. **Build:** they complete **enderman cairns**: strange structures whose full design is fixed when the world generates (deterministic templates), so endermen always know what to build. They bring in **End blocks** for them (end stone, purpur, etc.).
   - Endermen choose between the two jobs.
   - **Look (owner):** strange, twisty, **chorus-plant-like** spires rather than stacked stones: purpur pillar trunks that step sideways as they climb, branches ending in end-rod 'flowers', on an end stone brick footing (preview in the test world east of the hub).
   - **Cairns point to the stronghold** (owner): a few blocks in each cairn's design, not too obvious (e.g. a leaning top stone, a line of end stone along the base), point toward the nearest stronghold. Observant players can follow cairns there.

## The target world (owner, 2026-11-16)

- Endermen's **ideal world state is fixed at world generation: natural terrain + every cairn** (cairns decided at worldgen, offerings included), maybe plus a few other deliberate things later. It's computed from the seed, never stored. Cheap and deterministic: an enderman only ever checks one block (what's here) against another (what the target says).
- **How they work:** an enderman either **spawns already carrying a block** whose final destination is known and delivers it, or **spawns empty, picks up a block that doesn't match the target, and carries it to a spot that needs that block**. If it can't find one, it **vanishes with the block when it despawns**.
- So there are no exemptions: a cairn's offering flower is 'correct', a stray flower elsewhere gets moved.

## Cairn look, details (owner ideas, 2026-11-16)

- **Overworld meets End:** the twisting purpur spire with end-city touches (purpur stairs/slabs as branch knuckles, magenta stained glass, a magenta banner), plus **offerings** at its foot from what endermen carry in vanilla (#minecraft:enderman_holdable: small flowers, dirt, grass, moss, mud, sand, red sand, gravel, clay, mushrooms, pumpkins, melons, cactus and its flower, crimson/warped fungus, roots and nylium, TNT), varying by biome. Rare **TNT** offerings are allowed (owner: yes).
- **Variety (owner, to pick up later):** cairns must be somewhat random overall, not all the same: various shapes, sizes and branch layouts (still deterministic from the seed). The preview's banner placement is bad; revisit where banners hang.
- **Stronghold pointer simplified:** the **tallest end rod tilts toward the nearest stronghold**; the other pointer bits are dropped.

## Cairns as their home (D2: deliberate spawning)

- Endermen spawn near cairns and tend to stand on top of completed ones, instead of spawning randomly.
- **Left alone, they snowball:** more endermen spawn, faster, and they finish their work faster. Interfering (killing them, taking the cairns apart) slows or stops it.

## Owner answers

- **Water:** endermen keep their vanilla weakness to water (owner).

- **E1 what they touch:** no record of placed blocks. Endermen cheaply bucket each spot by **'does this block generate naturally'**: natural block types (dirt, stone, grass, sand...) are freely reverted toward the original, **including air where ground used to be** (dig out an area and they slowly fill it back; they might eventually bury your house); **unnatural** blocks (planks, glass, bricks, anything crafted) have only a **much smaller chance** of being touched.
- **E2 protection, both:** cairn sites are predetermined at worldgen and act as a **nexus**: a higher endermen spawn rate there, **significantly higher once the cairn is complete**. Killing endermen thins them but doesn't stop the flow from a nexus. **Wards definitely work**: a Totem of Warding area keeps endermen (and their work) out.
- **E3 shutting a nexus down:** (a) **tearing a cairn down** drops its site back to the lower, unfinished rate and endermen start rebuilding it; it works in reverse too: if a player repairs a cairn they broke, the higher spawn rate comes back ('complete' = matches its template). Cairns are made **only of blocks that don't generate naturally in the overworld** (end stone, end stone bricks, purpur, end rods...; **never obsidian or crying obsidian**: both generate naturally and a cairn would become a free obsidian source, owner), so the repair job never treats a cairn as terrain. (b) A **ward over a site** stops it producing.
- **E4 hostility:** vanilla rules (eye contact angers them), plus they turn hostile if you break their cairn or attack one nearby; **but an enderman busy on a job (repairing or building) ignores eye contact**.

## Open questions (owner)

- See the conversation; answers land here.

## Milestone test objectives

None yet (not built).
