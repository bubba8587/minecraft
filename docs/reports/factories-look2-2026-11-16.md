<!-- dte:C1 -->
# Docs report addendum: Ancient Fabricators, look round 2 (2026-11-16)

Adds to `docs/reports/factories-docs-2026-11-16.md` (merge both into `specs/mob-factory.md`). Still look only, no mechanics.

## Owner's feedback this round
"That's really close actually." Two changes: crimson streaks through the warped, and less sculk. Also done: the chains now read as chains. A jukebox hopper loop was asked for and then cancelled by the owner, because vanilla's infinite jukebox is coming in current snapshots. Nothing was added for it, and the office jukebox stays as it was (disc 13 in it, playing when placed).

## Changes (as built)

**Crimson streaks:**
- Crimson runs as thin streaks through the warped roof mass, the dripper's skin and the chains.
- *How:* streaks sit where two seeded wavy sheets both come near zero. They cross in lines, so crimson shows as veins, never patches.
- *Blocks:* nether wart block (50 %), stripped crimson hyphae (35 %, the red flesh), crimson hyphae (15 %).
- Warped stays primary. The function is `FactoryDesign.crimsonStreak`.

**Less sculk:**
- Vanilla's `sculk_patch_ancient_city` now grows from **7 spots** instead of about 17:
  - 2 on the deck (back-left corner, front-right)
  - 4 on the cavern floor
  - 1 in the tunnel
- No patch on the stairs or the window ledge any more.
- The painted sculk on the rock lining is thinner too: floor about 30 % instead of 45 %, walls rare.
- The deck now reads as ancient-city stone with sculk creeping in from the edges.
- The pad and the lamps are still kept clear.

**Chains:**
- 11-13 of them. Each is now a **thin strand** (about one block thick) of warped wart, threaded every ~3 blocks with a **knot** (a ball of stripped warped hyphae about 2.5 blocks across). They read as links on a chain.
- **Every fourth knot is crimson.**
- They start low on the cavern wall and climb it. Then they hang **free under the roof**, at least 1.5 below it and sagging up to 2.5 more between wall and dripper. They twist as they go and end at the dripper's stalk.
- The thorn spurs are gone, since the beaded links read better.

## Screenshots (`docs/previews/factories/`, refreshed)
The same set as round 1 (the cavern-floor close-up is still left out). Best views of this round's changes:
- crimson streaks: `fabricator-overview-lit.png`, `fabricator-side-lit.png`
- chains: `fabricator-chains-lit.png`
- lighter sculk: `fabricator-platform.png`, `fabricator-sculk-deck.png`

## For the mechanics phase
- **Office jukebox:** plan on vanilla's infinite jukebox (in current snapshots) instead of a workaround. Until then, the disc plays once when the Fabricator is placed.
