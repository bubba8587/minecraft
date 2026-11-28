# World Tweaks: Amethyst Golem art pass (2026-11-27)

Builder's report for `specs/world-tweaks.md` (Amethyst Golem) and `docs/art-todo.md`. Governs: C8 (World Tweaks), B16 (16x + 32x).

## What it looks like (player-facing)

A small, chunky lilac golem about one and a half blocks tall counting its crystals (the body itself is copper golem height). Made all of amethyst in the light lilac of amethyst clusters, with a cluster of lilac crystals (pink-tipped on top) growing out of the top of its head, big white eyes with dark pupils, a small smile, hands and feet a shade deeper, and a dark crystal-lined hole in its belly like a geode, or the sound hole of an instrument. The spawn egg is a round lilac egg with the golem's white-eyed face and a crystal cluster on top.

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
- Palette (recoloured 2026-11-27 on owner feedback: "all amethyst, maybe white eyes, palette more like the crystal than the block"): vanilla's amethyst cluster / bud crystal ramp only, body based on its light lilac (179, 142, 243) with mottling one shade either way, edges and shadows in the cluster's darker purples; three deep geode purples for the mouth, the belly hole and the crystal bed on the head top. No basalt, no calcite, no amethyst block colours. Eyes: white (240, 245, 244) with dark purple pupils (and a deep amethyst rim at 32x). No vanilla or CF32J pixels are copied, so no `CREDITS.md` entry.
- `--dark-eyes` writes (or with `--preview`, previews) the earlier dark eyes with a glint on the new palette, for comparison. The committed textures are the white-eyed ones.
- 16x: vanilla-style per-pixel mottling, one shade either way, no pink glints. 32x: drawn at 32x from the same face plan in Classic Faithful 32x Jappa style (palette shades only, grain in two-pixel clumps), with refined features: round white eyes with 2x2 pupils, a curved smile, a round rimmed belly hole. The spawn egg's 32x uses `pixelart.render32(cf=True)`.
- `python3 tools/texcheck.py bubbas_world_tweaks`: OK.

## Choices the owner might want to tweak

- All amethyst (owner): the calcite ring on the head top was dropped (it was the only non-amethyst left and didn't help the silhouette); the crystals now grow from a budding-dark bed with a shaded ring, which separates the crown from the head. Hands and feet are amethyst one shade deeper.
- No pink glints on the body and pink only on up-facing crystal faces: pink and cream on side faces go beige under the game's side shading (that was the beige-tips problem), so crystal sides stay lilac.
- The 16x face: white 2x2 eyes with a one-pixel pupil at the inner bottom, and a two-pixel mouth (the earlier four-pixel smile's corners sat right under the pupils and read as a second pair of eyes). The 32x face has round eyes with 2x2 pupils and a curved smile.
- Owner pick pending: white eyes (default) vs the old dark eyes on the new palette (`--dark-eyes`); comparison face shots were handed over.
- An earlier try put small crystal buds on the shoulders. They read as thumbs and were dropped.
- The spawn egg has no belly hole: on a 16 px egg it read as a second mouth.

## Client test shots (part "golem")

Added to `AmethystGolemClientGameTest` (new lines only): `golem-compare` (next to a vanilla copper golem and an amethyst cluster on an amethyst block), `golem-far` (about 9 blocks), `golem-top` (from above), `golem-face` (face close up), each at 16x and 32x.

## Second pass: white highlights and a calmer 16x face (2026-11-27)

Owner: "texture needs to have more white bits in it to match the crystal." Also the 16x face read as a surprised "oh".

- Reference: vanilla's `amethyst_cluster.png` has 17 near-white pixels of 140 (pink-white 254,203,230 and cream 255,253,213), placed as a ridge up the lit edge of each shard and at the points, never as speckle. The buds follow the same rule.
- Crystals: each shard face has a pink-white ridge up its lit edge (top 70%), next to the shard's dark edge; the points (tip boxes) are pink-white all round, the centre shard's upper segment pink-white on its lit half, and every crystal top face pink-white. No cream on crystals: under the game's side shading cream reads khaki (measured 145,116,131 on a shaded tip top).
- Body glints: hand-placed facet glints (`GLINTS` in the generator: box, face, streak pixels) on the head top, head sides and back, body front/sides/back, shoulders, arms, legs and cheek sides; none on the head front, so the eyes stay the only white there. 16x: pink-white pixels with a one-shade-darker facet pixel beside them. 32x: one-pixel diagonal pink-white ridges with light lilac on the lit side and one shade down on the shadow side; cream cores only on up-facing faces (head top, shoulders). The belly crystals and back pits keep their pink-white.
- 16x face: eyes are 2x2 with a tall 1x2 pupil on the inner side (no more white bar with a 1-px pupil), no dark row under them; the smile is a small soft U on the jaw (corners in the lighter mouth purple 70,43,125 under the pupils, two-pixel bottom), right below the eyes, with the grain round the face kept from going darker (`A` in the face grids). The jaw's lower row and the body's top row are plain amethyst now (the old shadow row there merged with the mouth into an open-mouth look), and the belly hole sits one row lower, so mouth and hole are clearly apart.
- 32x face: same eyes; the smile moved up one row on the jaw (corners, then a six-pixel line), the belly hole 1.6 px lower to match.
- Spawn egg: cream crystal points, pink-white lit edges on the shards, two pink-white glints on the egg's upper left, tall pupils, and a small smile like the golem's.
- `--dark-eyes` still works (eyes only; mouth, hole and glints are shared).
- Checked in client shots (part "golem"): face 16x/32x, front, side, back, compare 16x/32x, far, top, singing-note-close, waiting, egg 16x/32x. Body glints in shade read pale mauve rather than white (the game's side shading), crown ridges and points read pink-white like the cluster, most clearly from above.

## Third pass: bolder whites, a cluster-like crown, a closed 16x mouth (2026-11-28)

Owner: "keep working on the art". In game the second pass's glints read pale mauve, not white, and at 32x the one-pixel diagonal ridges read as scratches.

- Why the whites looked grey: the game shades entity faces by direction and the cluster block isn't shaded at all. Measured in the client shots (part "golem"): top faces render at 100%, front and back faces at 74%, side faces at 50%; crystal faces between 40% and 90% depending on their tilt (recorded per face in `CRYSTAL_LIGHT` in the generator). The vanilla cluster renders its texture colours unchanged. So pink-white (254,203,230) on a front face renders as mauve (188,150,170), and no texture colour can render brighter than about 189 grey on a front face.
- Measured highlight colours after this pass: shoulder caps and crystal tops (up-facing) render (255,251,236) and (255,255,250), the same brightness as the cluster's cream (255,253,213) and pink-white (254,203,230); white ridges on the best-lit crystal faces render (221,220,226); body front/back facets render (185,183,189) to (189,189,189), a clear light grey-white against the shaded lilac (132,105,180), no longer mauve. Truly cluster-white fronts would need the highlights drawn unshaded (e.g. an emissive overlay layer in the renderer, which is Java and not part of this pass).
- Body facets: chunky stamps (`STAMPS` and `FACETS` in the generator) instead of single-pixel glints: 2-3 white pixels at 16x with a light lilac rim and a darker facet edge below, the same shapes drawn as blocky two-pixel clumps at 32x (CF32J style). Cream-white on up-facing faces (shoulder caps are almost all white, the head top has four facets round the crystal bed), cool near-white (250,248,255) on front and back faces (cream goes khaki and pink goes mauve there), lilac-white (242,230,255 / 228,208,255) on side faces, where white would render mid grey. On: head top, back and sides, body front (upper corners and right side, clear of the belly hole), body back and sides, arm tops, fronts, outer sides and backs, leg fronts and outer sides, cheek sides. None on the head front: the eyes stay the only white there and read clearly without a rim.
- Crystal crown: cream-white top faces, white points on well-lit faces, a white ridge up the lit edge of each well-lit face (most of its height on the brightest faces), pink-white only as the mid-tone below the white and across the middle column, lilac lower down and on the dim faces (no white or pink there, since it renders grey or muddy). From above the crown reads white like the cluster; from the front it reads lilac with white ridges and points.
- 16x mouth: a plain closed line on the top row of the jaw, right under the eyes: two darker middle pixels (100,71,158) with softer ends (122,91,181), the row below it plain. The earlier soft U (corners one row up, under the pupils) still read as an open mouth. The cheek fronts beside it no longer get dark grain, which had widened the shape. The belly hole stays three rows lower. The 32x smile is unchanged.
- Spawn egg: white crystal points and lit edges (the shaded edges keep pink-white), a white rim shine on the egg's upper left (unshaded in the 32x so it stays white).
- `--dark-eyes` still works (checked in a face preview shot, not committed).
- Checked in client shots (part "golem"): face 16x/32x, close 16x/32x, compare 16x/32x, far, top, singing-note-close, egg 16x/32x, dark-eyes face.
