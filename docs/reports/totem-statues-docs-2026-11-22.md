<!-- dte:C6,B15,B16 -->
# Totem statues for every totem: docs report (builder, 2026-11-22)

For merging into `specs/totems-artifacts.md` ("Totem statue") and the player docs (`Totem Statue.md`, each totem's page), then deletion. Branch `totem-statues`. Owner's request: "go ahead and generate initial statue for all totems."

## Player-facing

- Every totem can now be set down as a statue: sneak and use it on a block. That's the Totems of Warding, Keeping, Gathering, Fortification, Growth, Seeking, the Beginning and the End. The Grave Talisman isn't a totem and has none.
- Each statue shows its own totem, in the item's colours and motif. Mining one gives that totem back.
- Sneak-using places the statue *instead of* the totem's own use: a sneaking Totem of the End places its statue and doesn't teleport, a sneaking Totem of the Beginning places its statue and doesn't ripple, and a sneaking Totem of Seeking places its statue and doesn't choose a structure. Without sneaking, every totem works as before.
- The Beginning's and the End's eyes, and Fortification's visor, glow faintly at night (like Warding's eyes and flame).
- Statues already placed in a world (all Warding until now) stay Warding statues.
- Initial art: the owner will redesign them.

## How it's built

- `statue/TotemType`: WARDING (first, so it's the default and old statues load as warding), KEEPING, GATHERING, FORTIFICATION, GROWTH, SEEKING, BEGINNING, END, plus `forItem(Item)`. With more than one value, `TotemStatueBlock`'s `totem` property is now on.
- `statue/StatueTotemItem`: an `Item` whose `useOn` is `TotemStatueBlock.placeFrom(context, TotemType.forItem(this))` (PASS without sneaking, so `use()` still runs). Keeping and Fortification are `StatueTotemItem`s; the Gathering, Growth, Beginning and End item classes extend it. Seeking's `useOn` checks sneaking first. Warding keeps its own `useOn`.
- Art, models, blockstate and loot all come from `tools-local/make_statue_textures.py`. `main()` (Warding, unchanged) and `write_others()` (the seven new statues): a generic figure (body, arms, head, chest emblem, a topper per totem, glowing elements with `light_emission`) on Warding's plinth. Each figure texture is a 16x16 grid in the item's palette (imported from `tools/totems-textures.py`), with its 32x twin from `pixelart.render32`. The blockstate covers 8 totems x 4 facings.
- **Loot table in the 26.3 format:** one pool, an `alternatives` entry with one item per totem, each with `"condition": {"type": "minecraft:all_of", "terms": [survives_explosion, match_block {state: {totem}}]}`. The old style (`"conditions": [{"condition": ...}]`, `block_state_property`) is silently ignored by 26.3, which first made every statue drop all eight totems; a milestone test now catches that.

## Builder's choices

1. **One generic figure for all seven** (Warding keeps its own hand-made hooded figure), with per-totem heads, emblems and toppers: Fortification's helm brim, Growth's leaf crown and blossom, Seeking's gold band, the Beginning's grass cap, the End's purpur crown with four spikes. Gathering raises its hands. Keeping hugs a deeper chest.
2. **Glowing parts:** the Beginning's and the End's eyes (light 12) and Fortification's visor (10). Keeping, Gathering, Growth and Seeking have no glow.
3. **Sneak-use always places the statue,** even for the End (no teleport while sneaking at a block) and Seeking (no structure choice while sneaking).

## Tests

- Core `endStatuePlacesWithoutTeleportingAndDropsItsTotem` (body in `TotemStatueGameTest`): sneak-using a Totem of the End places an End statue, doesn't move the player, and the statue drops the Totem of the End.
- Milestone `TotemStatueGameTest.everyStatueDropsItsTotemAndOldOnesLoadAsWarding`: each of the 8 drops exactly its own totem; a statue saved with only `facing` (26.3 keys `id`/`properties`) loads as warding.
- `:bubbas_totems_artifacts:build` passes (17 core). `TotemsGameTest` (9) passes with Reworked Mobs and Beacons.
- Client part `statues` (`StatuesClientGameTest`): `statues-row`, `statues-close-0..3` (two at a time), `statues-night`, each also `-32x`. Looked at: row, all four close pairs, night, row-32x.
