# World Tweaks: Amethyst Golem art pass (2026-11-27)

Builder's report for `specs/world-tweaks.md` (Amethyst Golem) and `docs/art-todo.md`. Governs: C8 (World Tweaks), B16 (16x + 32x).

## What it looks like (player-facing)

A small, chunky purple golem about one and a half blocks tall counting its crystals (the body itself is copper golem height). Made of amethyst, with a cluster of pale lilac crystals growing out of the top of its head, big dark eyes with a little glint, a small smile, dark stone (basalt) hands and feet, and a dark crystal-lined hole in its belly like a geode, or the sound hole of an instrument. Seen from above, a white calcite ring sits round the base of the crystals, like inside a real geode. The spawn egg is a round amethyst egg with the golem's face and a crystal cluster on top.

## Model (spec facts)

- Size: legs 4 px, body 6 px, head 6 px, crystals up to 7 px more. Head top at 16 px (the hitbox, 0.49 x 0.98, is unchanged and fits); the crystal tip reaches 23 px. Body 8 wide x 5 deep; head 8 x 7; arms 3 x 7 x 3 (hands reach 3 px off the ground); legs 3 x 4 x 4 with a 1 px gap.
- Part tree (names unchanged where they existed; new parts are public fields on `AmethystGolemModel`):
  - `root` > `body` > `head` > `jaw` (new, field `jaw`)
  - `head` > `crystals` > `crystal_center`, `crystal_left`, `crystal_right`, `crystal_back` (new, fields `crystalCenter`, `crystalLeft`, `crystalRight`, `crystalBack`)
  - `body` > `left_arm`, `right_arm`; `root` > `left_leg`, `right_leg`
- Pivots: body at the hips (y 20 from the model top, i.e. 4 px up), head at the top of the body (neck), arms at the shoulders, legs at the hips, `crystals` on the top of the head, each crystal at its base.
- **Jaw (for the singing animation):** `jaw` is the lower front of the head (4 wide, 2 tall, 4 deep), hinged at its back top edge. `jaw.xRot` positive opens the mouth (the chin drops); about 0.3 to 0.6 rad reads as singing. The head is built round it (skull above, cheeks either side, the back of the mouth behind), and every inside face is painted dark, so an open mouth shows a dark cavity. Checked open at 0.6 rad in a temporary run (not committed).
- Crystals: each is turned 45 degrees about its own axis so an edge faces out; the side ones tilt outward (left 0.6 rad, right 0.5) and the back one backward (0.55). They're separate parts so an animation can make them shimmer, wobble or bob to the music.
- Render type unchanged (`entityCutout`, the model default): the crystals are opaque like the vanilla cluster block.

## Textures

- Generator: `mods/bubbas_world_tweaks/tools-local/make_amethyst_golem_textures.py` (replaces the deleted placeholder script). Writes the 64x64 entity texture, its 128x128 32x twin, and the spawn egg 16x + 32x. `--preview DIR` writes enlarged previews.
- Palette, vanilla geode block colours only: amethyst block (body), amethyst cluster (crystals, pink tips), calcite (ring round the crystals on the head top), smooth basalt (hands, feet), plus three deep geode purples for the mouth and belly hole. No vanilla or CF32J pixels are copied, so no `CREDITS.md` entry.
- 16x: vanilla-style per-pixel mottling like the amethyst block, with its pink glints. 32x: drawn at 32x from the same face plan in Classic Faithful 32x Jappa style (palette shades only, grain in two-pixel clumps), with refined features: round eyes with a one-pixel glint, a curved smile, a round rimmed belly hole. The spawn egg's 32x uses `pixelart.render32(cf=True)`.
- `python3 tools/texcheck.py bubbas_world_tweaks`: OK.

## Choices the owner might want to tweak

- No calcite on the body: a white calcite ring round the belly hole read as a third eye or a second mouth from a distance, so the hole's rim is darker amethyst instead; calcite only shows on the head top.
- Basalt hands and feet ground it visually and stand for the geode's outer crust. They could be amethyst instead.
- The 16x smile is four pixels wide on two rows. Up close it reads a little like a cat's mouth; the 32x one is a clean curve.
- The crystal tips are the cluster's pale pink, which looks a little beige in shade.
- An earlier try put small crystal buds on the shoulders. They read as thumbs and were dropped.
- The spawn egg has no belly hole: on a 16 px egg it read as a second mouth.

## Client test shots (part "golem")

Added to `AmethystGolemClientGameTest` (new lines only): `golem-compare` (next to a vanilla copper golem and an amethyst cluster on an amethyst block), `golem-far` (about 9 blocks), `golem-top` (from above), `golem-face` (face close up), each at 16x and 32x.
