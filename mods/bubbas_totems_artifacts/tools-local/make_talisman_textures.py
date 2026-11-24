#!/usr/bin/env python3
# dte:B16,C6
"""The Grave Talisman's item textures, 16x and their Classic Faithful 32x Jappa-style 32x twins (pixelart.py cf mode) (agent-made, listed in docs/art-todo.md
for the owner to redo or tweak; never AI-generated). Needs Pillow.

  python mods/bubbas_totems_artifacts/tools-local/make_talisman_textures.py

A little gravestone on a leather cord: an arched slab of weathered stone with a touch of moss at its foot, a gem set in
its face. grave_talisman.png is the item with the gem unlit. grave_talisman_glow_<1..3>.png are the same picture with
the gem lit brighter at each level; the glow models (models/item/grave_talisman_glow_*.json) show only the gem and its
setting (pixels 5..10, rows 8..13, the same place at 32x) on a small cuboid that gives off light, so the gem must stay
in that square. The 16x grid is hand-placed; the 32x twins come from tools/pixelart.py.
"""
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent.parent.parent / "tools"))
from pixelart import check, render, render32  # noqa: E402

RES = HERE.parent / "src/main/resources"
MOD = "bubbas_totems_artifacts"

# o..5: weathered stone, outline to lightest. k/K/L: the leather cord. m/M: moss.
# R: the gem's setting. x/g/G/h/w: the gem, darkest to its highlight (per level, below).
BASE = {"o": (40, 40, 44), "1": (70, 70, 72), "2": (94, 94, 94), "3": (118, 118, 116), "4": (142, 142, 138),
	"5": (168, 168, 162), "k": (52, 34, 22), "K": (88, 60, 37), "L": (124, 88, 55), "m": (60, 82, 38),
	"M": (92, 120, 52)}
# Per glow level (0 = unlit): setting, then the gem from darkest to highlight.
GEMS = [
	{"R": (48, 52, 54), "x": (20, 32, 30), "g": (30, 48, 44), "G": (42, 66, 60), "h": (60, 88, 80), "w": (84, 114, 104)},
	{"R": (44, 72, 64), "x": (22, 70, 58), "g": (36, 102, 84), "G": (56, 138, 112), "h": (94, 178, 146), "w": (150, 214, 186)},
	{"R": (66, 118, 98), "x": (38, 138, 108), "g": (62, 184, 144), "G": (102, 220, 176), "h": (162, 244, 210), "w": (220, 255, 240)},
	{"R": (116, 188, 158), "x": (96, 208, 168), "g": (146, 238, 204), "G": (194, 255, 230), "h": (232, 255, 246), "w": (255, 255, 255)},
]
MATERIAL = {**{k: "stone" for k in "12345"}, **{k: "cord" for k in "kKL"}, **{k: "moss" for k in "mM"},
	**{k: "gem" for k in "xgGhw"}}

TALISMAN = check([
	"......kLLk......",
	".....kL..Lk.....",
	".....K....K.....",
	"......K..K......",
	"......oLKo......",
	".....oooooo.....",
	"....o555443o....",
	"...o55444332o...",
	"...o54RRRR32o...",
	"...o4RhwGGR2o...",
	"...o4RwGGgR2o...",
	"...o3RGGggR1o...",
	"...o3RGgxxR1o...",
	"...o32RRRR21o...",
	"...o3211112mo...",
	"....oooooMmo....",
])


def main():
	out16 = RES / f"assets/{MOD}/textures/item"
	out32 = RES / f"resourcepacks/{MOD}_32x/assets/{MOD}/textures/item"
	out16.mkdir(parents=True, exist_ok=True)
	out32.mkdir(parents=True, exist_ok=True)
	for level, gem in enumerate(GEMS):
		name = "grave_talisman" if level == 0 else f"grave_talisman_glow_{level}"
		pal = {**BASE, **gem}
		render(TALISMAN, pal).save(out16 / f"{name}.png")
		render32(TALISMAN, pal, MATERIAL, inner={"o": "1"}, bevel=1, grain=("stone",), seed=11, cf=True, grain_p=0.1).save(out32 / f"{name}.png")
		print(name)


if __name__ == "__main__":
	main()
