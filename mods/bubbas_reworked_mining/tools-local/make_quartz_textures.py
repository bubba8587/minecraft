#!/usr/bin/env python3
# dte:C7,B16
"""Quartz tools and armor textures, 16x and 32x (specs/textures.md, specs/reworked-mining.md "Quartz tools and armor").

The look the owner asked for is "a sick white look": bright quartz white with subtle veining, vanilla-styled. The
shapes come from vanilla's iron sprites (same silhouettes and shading bands, so the set sits next to vanilla gear);
every grey band is remapped to a warm quartz palette, the sticks keep vanilla's wood colours, and thin rose-grey veins
are drawn across the quartz. The 32x twins are made with tools/pixelart.py's render32 from the same grid (Faithful
style: smoothed silhouette, thin outlines, bevel, grain), with finer veins drawn at 32x.

Agent-made: listed in docs/art-todo.md for the owner to redo or tweak.

Usage: python3 make_quartz_textures.py <path to build/mcsrc/26.3/vanilla>
(worktrees lack build/, so pass the main checkout's copy).
"""
import random
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
sys.path.insert(0, str(REPO / "tools"))
from PIL import Image  # noqa: E402
from pixelart import render, render32  # noqa: E402

NS = "bubbas_reworked_mining"
MOD = REPO / "mods" / NS / "src" / "main" / "resources"
OUT16 = MOD / "assets" / NS / "textures"
OUT32 = MOD / "resourcepacks" / f"{NS}_32x" / "assets" / NS / "textures"

# Quartz: warm white, darkest band a soft brown-grey (never black, like nether quartz), highlights pure white.
PAL = {
	"o": (60, 50, 52),     # outline
	"p": (122, 110, 110),  # inner dark edge
	"d": (190, 179, 174),  # shadow
	"m": (224, 217, 211),  # mid
	"l": (242, 238, 233),  # light
	"h": (250, 248, 245),  # bright
	"w": (255, 255, 255),  # highlight
	"v": (222, 205, 204),  # vein on light quartz (rose-grey)
	"u": (200, 182, 181),  # vein on mid quartz
	# vanilla stick colours
	"A": (137, 103, 39), "B": (104, 78, 30), "C": (73, 54, 21), "E": (40, 30, 11),
}
QUARTZ = set("opdmlhwvu")
MATERIAL = {c: "quartz" for c in QUARTZ - {"o"}}
MATERIAL.update({c: "wood" for c in "ABCE"})
STICK = {(137, 103, 39): "A", (104, 78, 30): "B", (73, 54, 21): "C", (40, 30, 11): "E"}


def band(v):
	if v <= 30:
		return "o"
	if v <= 75:
		return "p"
	if v <= 120:
		return "d"
	if v <= 165:
		return "m"
	if v <= 205:
		return "l"
	if v <= 235:
		return "h"
	return "w"


def to_grid(img):
	img = img.convert("RGBA")
	w, h = img.size
	px = img.load()
	grid = []
	for y in range(h):
		row = ""
		for x in range(w):
			r, g, b, a = px[x, y]
			if a == 0:
				row += "."
			elif (r, g, b) in STICK:
				row += STICK[(r, g, b)]
			else:
				row += band((r + g + b) // 3)
		grid.append(row)
	return grid


def vein_paths(w, h, count, seed, step=1):
	"""Wandering diagonal streaks, as sets of points."""
	rnd = random.Random(seed)
	paths = []
	for _ in range(count):
		x, y = rnd.uniform(0, w), rnd.uniform(0, h * 0.4)
		dx = rnd.choice((-1, 1))
		pts = set()
		for _ in range(int((w + h) * 0.6)):
			pts.add((int(x), int(y)))
			x += dx * step * rnd.choice((0, 1, 1))
			y += step
			if rnd.random() < 0.15:
				dx = -dx
		paths.append(pts)
	return paths


def vein_grid(grid, count, seed):
	h, w = len(grid), len(grid[0])
	rows = [list(r) for r in grid]
	for pts in vein_paths(w, h, count, seed):
		for x, y in pts:
			if 0 <= x < w and 0 <= y < h:
				ch = rows[y][x]
				if ch in "lh":
					rows[y][x] = "v"
				elif ch == "m":
					rows[y][x] = "u"
	return ["".join(r) for r in rows]


def vein_32(img, count, seed):
	"""Finer veins at 32x, only on quartz-white pixels (not sticks, not outlines)."""
	px = img.load()
	w, h = img.size
	for pts in vein_paths(w, h, count, seed):
		for x, y in pts:
			if 0 <= x < w and 0 <= y < h:
				r, g, b, a = px[x, y]
				if a and r > 190 and abs(r - b) < 30:
					px[x, y] = (int(r * 0.91), int(g * 0.87), int(b * 0.88), a)
	return img


def make(src, rel, smooth, veins16, veins32, seed):
	grid = vein_grid(to_grid(Image.open(src)), veins16, seed)
	img16 = render(grid, PAL)
	img32 = render32(grid, PAL, material=MATERIAL, outline="o", inner={"o": "p"}, grain=("quartz",), grain_amount=0.035,
			seed=seed, smooth=smooth)
	img32 = vein_32(img32, veins32, seed + 100)
	for out, img in ((OUT16, img16), (OUT32, img32)):
		path = out / rel
		path.parent.mkdir(parents=True, exist_ok=True)
		img.save(path)
	print("wrote", rel)


def main():
	van = Path(sys.argv[1]) / "assets" / "minecraft" / "textures"
	seed = 7
	for part in ("sword", "shovel", "pickaxe", "axe", "hoe", "helmet", "chestplate", "leggings", "boots"):
		seed += 1
		make(van / "item" / f"iron_{part}.png", f"item/quartz_{part}.png", True, 1, 2, seed)
	for layer in ("humanoid", "humanoid_leggings"):
		seed += 1
		make(van / "entity" / "equipment" / layer / "iron.png", f"entity/equipment/{layer}/quartz.png", False, 4, 10, seed)


if __name__ == "__main__":
	main()
