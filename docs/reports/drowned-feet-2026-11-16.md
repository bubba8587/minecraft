<!-- dte:C1 -->
# Docs addendum: drowned hold by the ankles, lower lurk (2026-11-16)

Builder's addendum to the drowned report (already merged into `specs/drowned.md`). Two owner changes, both binding.

## Held by the ankles (owner)

- The victim is held by the feet, not the shoulders. Upright (lurking spot, dragging in water): the victim's soles sit at the drowned's outstretched hands (vanilla's zombie arm pose, about 80 degrees forward from hanging, hands at ~1.25 above its feet), 0.72 in front of it and 1.2 above its feet. The victim floats above and in front of it and goes down feet first as it walks backwards into deep water.
- Lying (crawling on land): the victim's feet are at its hands on the ground, 1.2 in front of it (its arms reach past its head). Between the two postures the hold follows the drowned's swim amount (the posture easing in and out), which both client and server tick, so it still needs no syncing.
- Numbers in `DrownedTuning`: `HOLD_FORWARD` 0.72, `HOLD_UP` 1.2, `HOLD_LYING_FORWARD` 1.2, `HOLD_LYING_UP` 0 (code: `DrownedMixin.getPassengerAttachmentPoint`). No arm-pose code was needed: vanilla's drowned arm pose already reaches to the ankles, and the crawl's arms already reach ahead.
- Crosshair targeting still works for a held player: the drowned is now below and behind them; they look down at it (the pick fix is unchanged; the drowned's box is clear of their eyes).
- In the 2-deep shallows a freshly grabbed player's head is above the water for a moment (their feet are at the drowned's hands); they go under as it backs into deeper water. In 3-deep or deeper water they're under at once.
- Not done: the victim keeps a standing pose (no flailing arms, no lying-on-the-back pose when dragged over land).

## Lower lurk (owner)

- Lurking, only its eyes and the top of its head show; the waterline is just under the eyes. `LURK_EYES_ABOVE_SURFACE` 0.25 → **0.12** (entity eye height; the drawn eyes sit right at the surface, the scalp about 0.33 above it).

## Tests and screenshots

- `./gradlew :bubbas_reworked_mobs:build -PmilestoneTests -PrerunGametests`: all 93 pass (no test numbers changed; the drag test's "eyes under water" still holds over the 8-deep pit).
- Client part `drowned` adds `drowned-lurk-bank` (from the bank at water level) and `drowned-grab-land` (crawling back to the water dragging a villager by the ankles, at night); `drowned-held-player` is now third person from in front and below with the HUD hidden (the drowned under the player holding their ankles), in deep water; `drowned-held-player-hud` keeps the grabbed hint.
- Previews updated in `docs/previews/drowned/`: `drowned-lurk-bank.png`, `drowned-lurk-low.png`, `drowned-sunk.png`, `drowned-drag-deep.png`, `drowned-grab-land.png`, `drowned-held-player.png` (plus the unchanged `drowned-swim.png`, `drowned-crawl-side.png`).

## Player-facing text changes

- "It lunges out, grabs you by the ankles and drags you backwards, feet first, into the deepest water nearby."
- "They wait in shallow water by the shore with only their eyes and the top of the head above the water."
