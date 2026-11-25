# Boss Piglin art: facts for the spec and player docs (art agent, 2026-11-25)

For `specs/boss-piglin.md` ("Look" and "The throne") and the Reworked Mobs player page.

## Look (owner feedback applied: Kingpin / Roadhog size, deep in z)
- Model `BossPiglinModel` (src/client) extends vanilla `AbstractPiglinModel` and keeps vanilla humanoid part names. Texture 128x128 (32x: 256x256).
- Render scale 1.4 (`BossPiglinModel.RENDER_SCALE`): about 2.55 blocks tall standing, the gut about 1.9 blocks wide and 2 deep, 2.8 wide with the arms. A normal piglin is 2 tall.
- Model units (scale 1): head 10x8x8 (vanilla piglin head) sunk into the shoulders (pivot 0,3,-1.5; no neck); torso 20x15x14, girth band 22x9x16, gut 18x11x3 plus a 14x8x2 front, humped back 16x9x2, rear 16x6x2, shoulder mounds 6x3x9; arms 6x12x6 (pivots x +-12, y 4); legs 7x7x7 (hips at y 17).
- Suggested hitbox: 1.8 wide x 2.6 tall (eye height about 2.2). It needs 2-wide gaps to waddle away.
- Poses: seated (lean back 0.1 rad, thighs forward -1.45 rad, spread 0.28, arms forward -0.8 rad resting by the gut, the model drops 3.5 px so the thighs rest on the entity position); waddle (half-length steps, splayed stance, body roll 0.13 rad and yaw 0.08 rad with the walk, a small bob); fleeing (forward lean 0.1 rad, arms flailing around -1.3 +-0.45 rad and out 0.55 +-0.2, head wobble).
- Gold armour (vanilla gold layers, vanilla trims) fitted as plates: a chest-and-shoulder plate, a belly plate, the arms, a hip band (leggings) over the rear, the legs, boots. Bare skin shows between the plates.
- Cape (`BossPiglinCapeLayer`, its own baked model): crimson with gold edging, a fringe hem and a crown-over-snout emblem, 20x20, from the top of the humped back; tilts back more when walking and fleeing, follows the body roll, lies flat and shortened against the backrest when seated.

## The throne (deviation from the 2 x 2 plan: 2 wide x 3 deep)
- The boss is about 2x2 seated, so the seat is the 2x2 and the backrest is a third row: `ThroneDesign.blocks(facing)` covers 2 wide x 3 deep x 4 high (`WIDTH`, `DEPTH`, `HEIGHT`).
- Seat: 4 polished blackstone brick slabs (bottom). Backrest row, bottom to top: polished blackstone bricks, gilded blackstone, chiseled polished blackstone, gold blocks.
- `ThroneDesign.seat(facing)`: seat surface 0.5 up, centred across, 0.98 behind the front face (the gut is flush with the front edge, the cape against the backrest).

## Milestone test objectives (client)
- Seated on the throne front, 3/4 and side; standing next to a normal piglin; waddle side; fleeing; cape from behind; at 16x and 32x.
