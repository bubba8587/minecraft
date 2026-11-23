<!-- dte:C1,B15,B17 -->
# Reworked Mobs: monster camps (spec, design 2026-11-22, not built)

Owner's design (2026-11-22), building on the earlier idea in `docs/research/camps-blood-moon-idea.md`. Decisions made since win where they differ (the 15 % spawn cut stays; the blood moon is on hold).

## What a camp is

- A structure with a **fixed roster** of monsters. It appears when a player comes near (a wide proximity trigger, so idle camps cost nothing).
- **Camp types (owner): skeletons and zombies.** Biome versions use the same rules with other mobs and materials: husks in deserts, strays in snow, bogged in swamps.
- **A leader** in each camp (owner): the same mob type, **slightly larger** (vanilla `scale` attribute), **tougher**, with **better gear**. It does **not** wear a banner.

## Banners are the key (owner)

- **The camp's banners are its key blocks.** Vanilla banners, no new block (B15). **Each monster type has its own banner pattern**, readable from a distance. A camp may have more than one.
- **Break every banner in the camp and it stops for good.** Kill the roster but leave a banner standing, and the camp comes back.
- **Put a banner back** (the camp's type's pattern) **within the camp's area, and the camp returns.** Players can re-arm a camp on purpose, for a farm or a fight.
- So camps are **registered per world, like cairns** (`specs/endermen.md`, CairnSites): location, type and area saved, fixed at worldgen. The game checks "a matching banner stands in this area?" and never scans for banners.

## Pillager outposts become camps too (owner)

- Vanilla outposts spawn pillagers anywhere in their area forever (`spawn_overrides` on `minecraft:pillager_outpost`, full bounding box) and ignore banners. New rule: **when every banner in an outpost is broken, its pillagers stop spawning**, and putting one back brings them back.

## Look (owner)

- Started from images of Breath of the Wild's monster camps for the feel (owner: "look at images of it to start"; Minecraft's blocks make ours different anyway): **scant wooden lookout towers, ladders, campfires (soul campfires fit), bone-and-skull rock outcrops** built from Minecraft blocks (bone blocks, skulls, stone).
- Sizes and layouts vary. All deterministic from the seed.

## Open

- **When an uncleared camp refills** (banner still standing, roster dead): at the blood moon once it exists; until then, owner to choose (agent's suggestion: a slow restock after a few in-game days while no player is near).
- Camp loot, spacing and biomes, the trigger range, roster sizes, the leader's numbers.

## Milestone test objectives

- A camp's roster appears when a player comes within range, with one larger, tougher, better-geared leader.
- Breaking every banner in a camp stops it for good. Placing a matching banner back in its area brings it back.
- An outpost with every banner broken spawns no pillagers. Putting one back restores them.
