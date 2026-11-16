#!/usr/bin/env python3
# dte:B16,C6
"""The Totem of Warding's item texture (Bubba's Totems & Artifacts), 16x and its Faithful-style 32x twin
(specs/textures.md). Agent-made, listed in docs/art-todo.md for the owner to redo or tweak; never AI-generated.

  python tools/warding-textures.py

A small idol carved from blue-grey soul stone, drawn in the Totem of Undying's idiom (head, arms out, body, foot): a
hooded figure whose hood is crowned with a soul-fire flame, two soul-fire eyes in the dark of the hood, and a glowing
ward rune (a slit eye) on its chest. Light from the top left; the outline is the stone's own darkest shade, as vanilla
items do. The statue block (mods/bubbas_totems_artifacts/tools-local/make_statue_textures.py) carves the same figure.
The 16x grid is hand-placed; the 32x twin is made from it by tools/pixelart.py. Needs Pillow.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from pixelart import check, render, render32  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "mods" / "bubbas_totems_artifacts" / "src" / "main" / "resources"
OUT = {16: RES / "assets" / "bubbas_totems_artifacts" / "textures" / "item",
	32: RES / "resourcepacks" / "bubbas_totems_artifacts_32x" / "assets" / "bubbas_totems_artifacts" / "textures" / "item"}

# Soul stone, darkest (outline) to lightest. Shared with the statue.
STONE = {"o": (24, 26, 38), "1": (40, 46, 64), "2": (57, 66, 88), "3": (78, 90, 116), "4": (104, 118, 146),
	"5": (136, 150, 176), "6": (170, 182, 204)}
# Soul fire, like vanilla's soul fire and soul lantern: deep teal to near-white.
FIRE = {"a": (10, 84, 98), "b": (22, 150, 162), "c": (88, 218, 222), "d": (196, 255, 246)}
PALETTE = {**STONE, **FIRE}
MATERIAL = {**{k: "stone" for k in STONE if k != "o"}, **{k: "fire" for k in FIRE}}

ICON = check([
	"........b.......",
	".......bcb......",
	"......acdc......",
	"......cddc......",
	".....obddbo.....",
	"....o566543o....",
	"...o56111132o...",
	"...o55c11c22o...",
	".oooo5431121oooo",
	"o56655433332221o",
	"o545443bb322211o",
	".oooo43cd221oooo",
	"...o5433bb21o...",
	"....o543321o....",
	"...o65544321o...",
	"...oooooooooo...",
])


def main():
	for n, out in OUT.items():
		out.mkdir(parents=True, exist_ok=True)
		if n == 16:
			img = render(ICON, PALETTE)
		else:
			img = render32(ICON, PALETTE, MATERIAL, inner={"o": "1"}, grain=("stone",), seed=7)
		img.save(out / "totem_of_warding.png")
		print("wrote", out / "totem_of_warding.png")


if __name__ == "__main__":
	main()
