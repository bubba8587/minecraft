<!-- dte:C1 -->
# Docs report addendum: Ancient Fabricators, lighting pass (2026-11-16)

Adds to `factories-docs-2026-11-16.md` and `factories-look2-2026-11-16.md`. Look only.

## Owner's feedback
Too much light. A Fabricator should be dark like an ancient city: the pad and the dripper glow out of the gloom, and you need a torch to explore.

## Light sources now (as built)
- **Dripper (main glow above):**
  - its thin lower half is a beaded spine with a shroomlight every other block;
  - rings of shroomlight glands (40 %) on the thick upper skin, and a core gland every 4 blocks;
  - a crying obsidian tip (light 10) over the pad.
  - The chains now stop just short of the stalk (a 1.9-block clear cylinder) so they don't bury it.
- **Teleporter pad (main glow on the deck):** the lamp-ring timer (5 of 8 lit, light 15 each), plus its crying obsidian ring (light 10).
- **The paired beacons:** each is light 15 (vanilla). The floor beacon lights its own corner of the deck.
- **Accents, kept faint and sparse:**
  - roof mass: shroomlight 3.5 % → 0.3 %; none in the chains or the dripper's collar and strands;
  - small drippers: crying obsidian tips 45 % → 20 %;
  - soul lanterns: only the deck's 2 front corner posts, 1 in the office, 1 on the window ledge (was 10 in all);
  - candles: 2 by the floor beacon, 2 by the entrance, 2 in the office;
  - soul fire in the pit under the deck: about 25 % less;
  - sculk's own faint glow.
- **Switched off:** the deck's 6 redstone lamps and the face frame's 10 lamps (still placed, unlit, no power under them), the 2 lanterns hanging by the face wall, the lamp under the Warden.

## Block light measured (client test prints `[fabricator-light]`, zombie Fabricator, seed 1000)

| Spot | Block light |
|---|---|
| pad middle | 12 |
| deck centre | 13 |
| beside the floor beacon | 14 |
| deck front edge (by the entrance candles and lanterns) | 11 |
| deck back-left / front-right | 7 / 7 |
| under the dripper tip | 11 |
| window ledge | 7 |
| office | 6 |
| face wall middle | 2 |
| cavern floor west (soul fire nearby) / east | 5 / 0 |
| tunnel | 1 |

The deck's middle and the pad are bright. The face wall, the cavern floor, the tunnel and most of the roof are 0-5, so hostile mobs could spawn there (vanilla needs block light 0). The face reads faintly from the deck and properly with a torch.

## Screenshots
- All shots in `docs/previews/factories/` are now taken **without night vision**, as in play.
- Exception: one detail shot, labelled `fabricator-overview-nightvision.png`.
- The old `-lit` shots are gone; `fabricator-side.png` and `fabricator-chains.png` replace them.

## Choices to hear about
- The **floor beacon's own light (15)** keeps one corner of the deck bright. It's part of the paired beacon (the debuff source), so I left it.
- **The face wall is dark until you bring light.** If the owner wants it to loom out of the dark, two lit lamps at the top of its frame would do it.
