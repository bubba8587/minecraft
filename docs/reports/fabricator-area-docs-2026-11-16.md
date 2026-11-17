# Ancient Fabricator: secrets and a richer generated area (builder report, 2026-11-16)

For `specs/mob-factory.md` and the Ancient Fabricators player page. Look only: the machine (dripper, teleporter, lamps, Warden, beacons, debuffs, spawner position) is unchanged.

## Owner's request
"Fabricators also just need some additional secrets and complexity (not to main mechanic, but to the generated area)." Chosen: a reject pit under the deck, an intake chamber, buried chests and real ancient-city detail in the lower area ("right now it's 3 large square platforms and some stairs"; the top part looks good, left alone). Later: "a more dynamic Office, more stairs, etc."

## Player-facing (for the player page)
- **Reject pit (secret).** One tile in the deck looks wrong: a chiseled deepslate block off the deck's chiseled pattern (back-right quarter, between the pad and the shriekers). Mine it and a ladder drops into a sealed vault under the deck heaped with failed products: bone blocks, heads of the Fabricator's mob (zombie and creeper heads; skeleton skulls for the others, plus cobwebs at spider Fabricators and obsidian at endermen), soul soil, and a chest of rejects. From the pit below, the vault shows as a bricked-up doorway.
- **The intake (secret).** Over the dripper's root, inside the roof, a low sculk-choked chamber that feeds the machine: soul soil, soul sand, sculk and bone underfoot, warped stems running in like roots to a knot of teal hyphae in the middle of the floor, bone heaps, a sculk catalyst, one soul fire. The hidden spawner is right under that knot. **Getting there:** climb a chain; on one chain, close under the roof near the dripper, twisting vines grow up a throat into the chamber. (Climbing up there without sneaking wakes the Warden, as before.)
- **A way across.** A 5-wide causeway on piers now runs from where the tunnel enters the cavern to the deck's front gap (it used to be a 7-block drop into the pit).
- **The lower city.** A lower terrace west of the deck (stairs down from the deck's west side, a ruined arch with a soul lantern, a broken back wall with candles, a gray and light blue wool corner, stairs on down to the floor); a small ruined hut on the cavern floor (front corner, away from the causeway) with a fallen-in roof; a broken arch behind the face wall; broken pillars, rubble, sculk veins.
- **More stairs.** From the deck's back corners: east, a 2-wide flight to a landing (a post with candles) then a broken flight down; west, a narrow broken flight behind the terrace; and a 2-wide flight off the causeway's side into the pit.
- **Buried chests.** Three chests sunk to their lids in floors and rubble: on the terrace, in the hut, and under the arch behind the face wall.
- **The Warden's office is split-level now.** The main floor by the window is double height (a soul lantern on a chain); a step down leads to a sunken archive where the filing chests sit in nooks in the back wall; over the archive a loft (a mezzanine with a rail, wool and carpet, a lectern) reached by a stair along the north wall. The Warden's window, its spot, the desk and the jukebox are where they were.

## Spec facts (as built, `FactoryDesign`)
- New passes in `design()`: `lowerCity()` (causeway, terrace, hut, behind the face wall, ruined pillars, rubble, `flights`, veins) and `rejectPit()` after `tower()`; `intake()` after `beams()`. All hash-based (no draws from the design's random), so the sculk patch spots don't shift.
- Positions (local): reject vault x 2..7, z -8..-2, floor y -7, interior y -6..-3; the tell at (3, -1, -5), ladder below it; reject chest at (6, -6, -3). Intake floor = spawner y + 1 (the knot is the wrap's top), chamber radius about 6.5 (wobbled), 2-5 high, kept 4.2 from the beam column; throat picked from the chains (the chain block 4.5-9 from the dripper with the smallest gap to the roof), vines from it to the chamber floor (+1). Terrace top y -5, x -19..-14, z -6..7. Office tower now x 14..23 (was 14..20): archive x 20..22 floor y 4, loft floor y 9, nook chests at x 23, z -2/0/2 (they keep the office loot table).
- Accessors: `rejectTell()`, `rejectChests()`, `buriedChests()`, `throat()`, `intakeFloor()`, `hut()`.
- The deck rim lost its walls at the new stair gaps (west z 2..4, east z -9..-8).
- **Loot (provisional; the owner is deciding Fabricator rewards):** `loot_table/chests/fabricator_rejects.json` (rotten flesh, bones, bone meal, string, spider eyes, gunpowder, arrows, an ender pearl, sculk; 1-2 damaged iron/chain/leather pieces, an iron sword or bow; rarely a mob head or name tag) and `loot_table/chests/fabricator_buried.json` (coal, bones, candles, soul torches, amethyst, glow berries, sculk, iron, echo shards, disc fragment 5, experience bottles; then nothing (10) / Swift Sneak book (2) / enchanted damaged iron leggings (2) / disc 13 (1)). Keys `FabricatorLoot.REJECTS`, `FabricatorLoot.BURIED`. 26.3 `modifier` format.
- **Block count:** 55,186 placements for the zombie test Fabricator (seed 1000), up about 1,000-1,200 from roughly 54,000 (the spec's figure). Design cost unchanged in kind (one extra pass over a few small boxes).
- **Light:** unchanged at the existing spots (office 6, window ledge 7). New: intake 6 (soul fire, catalyst), terrace 4 (one soul lantern on the arch, one lit candle group), causeway 2. Everything else new is unlit (unlit candles).

## Screenshots (`docs/previews/factories/`)
New: `fabricator-reject-tell-nightvision`, `-reject-open-nightvision`, `-reject-pit-nightvision`, `-intake`, `-intake-nightvision`, `-intake-roots-nightvision`, `-throat-nightvision`, `-office-split-nightvision`, `-office-loft-nightvision`, `-office-archive-nightvision`, `-lower-front`, `-lower-front-nightvision`, `-lower-terrace-nightvision`, `-lower-hut-nightvision`, `-lower-back-nightvision`, `-flight-east-nightvision`, `-lower-overview-nightvision`, `-lower-overview2-nightvision`. Refreshed: `fabricator-office`, `fabricator-overview`, `fabricator-overview-nightvision`.

## Milestone test objectives
- New milestone server test `MobFactoryGameTest.secretsAreBuilt`: the tell is chiseled over a ladder; reject and buried chests carry their tables and the buried ones can open; the throat holds twisting vines; the intake is open; the spawner is where the design says and hidden on all six sides; both new tables roll items and reject gear comes out damaged.
- Client part `factories` takes the shots above (`secrets(...)` in `FactoriesClientGameTest`) and prints `[fabricator] design blocks ...` with the intake, terrace and causeway light.
- Not tested (objectives): a natural (city-turned) Fabricator's causeway meets its city tunnel; the hut lands on the side away from a leaning causeway.

## Builder's choices (for the owner)
1. **The tell is subtle** (one off-pattern chiseled tile; grown sculk can creep near it but never replaces chiseled deepslate). If it's too hidden, a crack of sculk veins pointing at it would help.
2. **The causeway** turns the deck from "drop in, pillar out" into a walk-in. Easy to make it broken in the middle if the owner wants the jump back.
3. **Intake access is by chain + vines;** sneaking matters (the Warden's roof zone is unchanged).
4. **Loot tables are new and provisional**, kept apart (`fabricator_rejects`, `fabricator_buried`) so they can be swapped once the owner decides what a Fabricator rewards.
5. **Prize spots** if the owner wants one: the intake chamber (hardest to reach: chain climb, Warden-sensitive, right over the spawner), then the reject vault (secret), then the office loft (lectern, nothing on it yet).
6. Office tower grew 3 blocks deeper into the cavern wall (outside unchanged from the deck side).
