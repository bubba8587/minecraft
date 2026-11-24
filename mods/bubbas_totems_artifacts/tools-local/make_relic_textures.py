#!/usr/bin/env python3
# dte:B16,C6
"""The Relic Map Shred's item textures, 16x and their Classic Faithful 32x Jappa-style 32x twins (pixelart.py cf mode) (agent-made, listed in docs/art-todo.md
for the owner to redo or tweak; never AI-generated). Needs Pillow.

  python3 mods/bubbas_totems_artifacts/tools-local/make_relic_textures.py

relic_map_shred_4: a big torn corner of an old map (a /4 piece): straight top and left edges, ragged right and foot,
water, land and an inked trail. relic_map_shred_9: a small scrap torn on every side (a /9 piece), older and yellower,
with the corner of a red X. relic_map_shred_whole: the pieces put back together, the tear lines showing, a red X.
"""
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent.parent.parent / "tools"))
from pixelart import check, render, render32  # noqa: E402

RES = HERE.parent / "src/main/resources"
MOD = "bubbas_totems_artifacts"

PAPER = {"o": (70, 50, 30), "1": (150, 122, 84), "2": (196, 172, 124), "3": (226, 212, 172), "i": (96, 70, 46),
	"b": (104, 132, 156), "g": (122, 142, 82), "r": (176, 36, 28), "s": (150, 124, 86)}
OLD = {**PAPER, "1": (140, 108, 62), "2": (190, 160, 100), "3": (218, 196, 140)}
MATERIAL = {**{k: "paper" for k in "123s"}, "b": "water", "g": "land", "i": "ink", "r": "mark"}

SHRED_4 = check([
	"................",
	".ooooooooooo....",
	".o33333333332o..",
	".o3bbb33i3332o..",
	".o3bbbg3i33332oo",
	".o3bbgg33i33332o",
	".o33gggg33i3322o",
	".o3333gg333i332o",
	".o3i3333333i32o.",
	".o33i33333i332o.",
	".o333iii3i33322o",
	".o333333i333321o",
	".o2333333332oo..",
	".o222o23332o....",
	".oo1o.o222o.....",
	"..o....ooo......",
])

SHRED_9 = check([
	"................",
	"................",
	"....oo...oo.....",
	"...o33o.o23oo...",
	"..o3333o33333o..",
	"..o33i33333g2o..",
	".o333i333gg32o..",
	".o3333i33gg332o.",
	"..o3333i33g332o.",
	"..o2333i333332o.",
	"..o23333ir3r32o.",
	".o22333333r332o.",
	".o1223333r3r2o..",
	"..oo2223322oo...",
	"....oo22oo......",
	"......oo........",
])

WHOLE = check([
	"................",
	".oooooooooooooo.",
	".o33bb333s3332o.",
	".o3bbbb33s3i32o.",
	".o3bbgg33s33i2o.",
	".o33ggg33s333io.",
	".osssssssssssso.",
	".o3i33333s3gg2o.",
	".o33i3333sggggo.",
	".o333i333s3gg2o.",
	".o3333iiis3r3ro.",
	".o2333333s33r2o.",
	".o2233333s3r3ro.",
	".o1222222s2222o.",
	".oooooooooooooo.",
	"................",
])


def main():
	out16 = RES / f"assets/{MOD}/textures/item"
	out32 = RES / f"resourcepacks/{MOD}_32x/assets/{MOD}/textures/item"
	out16.mkdir(parents=True, exist_ok=True)
	out32.mkdir(parents=True, exist_ok=True)
	for name, grid, pal in (("relic_map_shred_4", SHRED_4, PAPER), ("relic_map_shred_9", SHRED_9, OLD), ("relic_map_shred_whole", WHOLE, PAPER)):
		render(grid, pal).save(out16 / f"{name}.png")
		render32(grid, pal, material=MATERIAL, outline="o", inner={"o": "1"}, bevel=1, grain=("paper",), cf=True, grain_p=0.08).save(out32 / f"{name}.png")


if __name__ == "__main__":
	main()
