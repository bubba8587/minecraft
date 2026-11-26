# Extra Recipes docs report, 2026-11-25: Glider art is now a recoloured elytra

Owner's request: "glider: switch to using a recolored Elytra using our bamboo/membrane/diamond/feather elements."

## What changed for players

- The Glider's item icon and its worn wings are now the elytra's shapes and shading, recoloured to match the recipe:
  - a bamboo-green outline and frame on the icon, and a bamboo spar along each wing's leading edge;
  - phantom membrane wing skin, cream fading to the membrane's mauve shadows;
  - a diamond clasp between the wings on the icon, and diamond caps on the wing tips;
  - white and grey feathers on the bottom tips of the wings.
- 32x: the same recolour of Classic Faithful 32x Jappa's elytra, in the Bubba's Mods 32x pack.

Player docs, `player-docs/Projects/Bubba's Extra Recipes/The Glider.md` line 62, replace with something like:
"The Glider looks like an elytra made from its recipe: phantom membrane wings on a bamboo frame, with diamond tips and feathered ends. The textures are temporary and will be redrawn; a 32x version comes in the **Bubba's Mods 32x** resource pack." Its credits section (if any) should name Classic Faithful 32x Jappa for the 32x textures (specs/textures.md: credit on the player docs page and the Modrinth listing).

## Spec facts (`specs/extra-recipes-mod.md`, "Look", lines 54-56)

Replace the placeholder description with:
- Agent-made art at 16x and 32x (B16), made by `tools-local/make_glider_textures.py` (renamed from `make_placeholder_textures.py`; no longer uses `tools/pixelart.py`): vanilla's elytra (16x) and CF32J's elytra (32x, read from the CF32J-2026r21 zip in the Modrinth profile `porp`; without it the committed 32x is kept) recoloured region by region. Each pixel's OKLab lightness, normalised within the pixels of its region, is placed on that material's vanilla ramp:
  - wings (64x32 elytra sheet): leading side strip and the leading column of the outer face → bamboo (`item/bamboo` + `block/bamboo_stalk`), the spar's last rows and the tip-end face → diamond (`item/diamond`), shoulder-end face → bamboo, the last two face rows (wingtips) → feather (`item/feather`), everything else → phantom membrane (`item/phantom_membrane`; upper 70 % of its ramp, so the shoulders are grey-mauve, not deep purple);
  - icon: the two darkest elytra shades (outline and yoke) → bamboo, the darkest shade at the centre of the yoke → diamond clasp, the bottom tips' inner pixels → feathers, the inside → phantom membrane (whole ramp).
- `specs/textures.md` line 45 and the colour-count table row (line 55, `extra_recipes | glider icon (wings)`) still name the old script and counts; the new counts come from the source (8 elytra icon shades, 10 wing shades) and are not palette-limited by `cf=True`.
- Credits: new `mods/bubbas_extra_recipes/CREDITS.md`; Glider section added to `resourcepacks/bubbas_32x/CREDITS.md`. `specs/textures.md` line 43's list of mods with a `CREDITS.md` can add Extra Recipes.
- `specs/STATUS.md` line 153 and spec "Open" (line 129): person-made Glider art is still parked; this is still agent-derived art (vanilla/CF32J derived, not human-made for B14).

## art-todo

`docs/art-todo.md` Glider row updated (script, description, "derived from vanilla/CF32J, not human-made (B14); CREDITS.md").

## Choices I made (owner may want to adjust)

- The brief said the phantom membrane palette is "pale grey-blue/teal". Vanilla's phantom membrane *item* is actually cream with mauve shadows (the grey-blue is the phantom mob's skin). I used the item's real colours, so the recipe ingredient reads; a grey-blue skin would be one ramp swap in the script (`MEMBRANE`).
- Feathers only on the last two rows of the wings and the bottom tips of the icon; diamonds only as the icon's clasp and the wing-tip caps. No bamboo nodes or battens added (kept to a recolour, "evolve, don't redesign").
