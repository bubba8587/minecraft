#!/usr/bin/env python3
# dte:C7,B16
"""Quartz tools and armor textures, 16x and 32x (specs/textures.md, specs/reworked-mining.md "Quartz tools and armor").

The look the owner asked for is "a sick white look": bright quartz white with subtle veining, vanilla-styled. The
shapes come from vanilla's iron sprites (same silhouettes and shading bands, so the set sits next to vanilla gear);
every grey band is remapped to a warm quartz palette, the sticks keep vanilla's wood colours, and thin rose-grey veins
are drawn across the quartz. The 32x twins are Classic Faithful 32x Jappa's own iron sprites recoloured: every grey
shade maps to one quartz shade (CF32J's pixels, outline and shading kept exactly), its stick colours stay, and a few
thin veins in the palette's vein shades cross the armor. CF32J-derived: credited in the mod's CREDITS.md (Faithful
License, https://faithfulpack.net/license).

Agent-made: listed in docs/art-todo.md for the owner to redo or tweak.

Usage: python3 make_quartz_textures.py <path to build/mcsrc/26.3/vanilla> [<path to the CF32J zip>]
(worktrees lack build/, so pass the main checkout's copy; the zip defaults to the owner's Modrinth copy).
"""
import io
import os
import random
import sys
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
sys.path.insert(0, str(REPO / "tools"))
from PIL import Image  # noqa: E402
from pixelart import render  # noqa: E402

NS = "bubbas_reworked_mining"
MOD = REPO / "mods" / NS / "src" / "main" / "resources"
OUT16 = MOD / "assets" / NS / "textures"
OUT32 = MOD / "resourcepacks" / f"{NS}_32x" / "assets" / NS / "textures"

# Quartz: warm white, darkest band a soft brown-grey (never black, like nether quartz), highlights pure white.
PAL = {
	"o": (60, 50, 52),     # outline
	"q": (132, 122, 121),  # tool-head outline: soft, so thin heads read white (orchestrator, 2026-11-22)
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
QUARTZ = set("opqdmlhwvu")
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


# CF32J iron greys -> quartz shades (warm white, darkest a soft brown-grey, never black).
CF_QUARTZ = {
	24: (58, 48, 50), 25: (58, 48, 50), 40: (90, 78, 79), 45: (90, 78, 79), 68: (124, 112, 111), 107: (164, 152, 149),
	130: (182, 171, 167), 150: (198, 188, 183), 183: (214, 206, 201), 190: (222, 215, 209), 193: (224, 217, 211),
	194: (224, 217, 211), 198: (228, 221, 215), 209: (236, 231, 226), 216: (242, 238, 233), 217: (242, 238, 233),
	229: (248, 245, 241), 255: (255, 255, 255),
}
CF_VEIN = {(236, 231, 226): PAL["v"], (242, 238, 233): PAL["v"], (248, 245, 241): PAL["v"], (228, 221, 215): PAL["u"],
	(224, 217, 211): PAL["u"]}
CF32J_ZIP = Path(os.environ.get("APPDATA", "")) / "ModrinthApp/profiles/porp/resourcepacks/CF32J-2026r21.zip"


def cf_recolour(img):
	"""A CF32J iron sprite in quartz: greys mapped through CF_QUARTZ, everything else (the sticks) kept."""
	img = img.convert("RGBA")
	px = img.load()
	for y in range(img.height):
		for x in range(img.width):
			r, g, b, a = px[x, y]
			if a and r == g == b:
				if r not in CF_QUARTZ:
					raise ValueError(f"unmapped CF32J grey {r}")
				px[x, y] = CF_QUARTZ[r] + (a,)
	return img


def cf_veins(img, count, seed):
	"""Thin veins over light quartz only."""
	px = img.load()
	w, h = img.size
	for pts in vein_paths(w, h, count, seed):
		for x, y in pts:
			if 0 <= x < w and 0 <= y < h:
				r, g, b, a = px[x, y]
				if a and (r, g, b) in CF_VEIN:
					px[x, y] = CF_VEIN[(r, g, b)] + (a,)
	return img


def brighten_tool(grid):
	"""Tool heads are thin: vanilla's near-black outline and dark inner edge made them read mid-grey. Soften the
	outline and lift the inner edge to the quartz shadow tone (tools only; the armor reads white as it is)."""
	return ["".join({"o": "q", "p": "d"}.get(ch, ch) for ch in row) for row in grid]


def make(src, cf, rel, veins16, veins32, seed, tool=False):
	base = to_grid(Image.open(src))
	grid = vein_grid(brighten_tool(base) if tool else base, veins16, seed)
	img16 = render(grid, PAL)
	img32 = cf_recolour(cf)
	if veins32:
		img32 = cf_veins(img32, veins32, seed + 100)
	for out, img in ((OUT16, img16), (OUT32, img32)):
		path = out / rel
		path.parent.mkdir(parents=True, exist_ok=True)
		img.save(path)
	print("wrote", rel)


def main():
	van = Path(sys.argv[1]) / "assets" / "minecraft" / "textures"
	zf = zipfile.ZipFile(sys.argv[2] if len(sys.argv) > 2 else CF32J_ZIP)

	def cf(rel):
		return Image.open(io.BytesIO(zf.read(f"assets/minecraft/textures/{rel}")))
	seed = 7
	for part in ("sword", "shovel", "pickaxe", "axe", "hoe", "helmet", "chestplate", "leggings", "boots"):
		seed += 1
		tool = part in ("sword", "shovel", "pickaxe", "axe", "hoe")
		make(van / "item" / f"iron_{part}.png", cf(f"item/iron_{part}.png"), f"item/quartz_{part}.png", 1, 0 if tool else 2, seed,
				tool=tool)
	for layer in ("humanoid", "humanoid_leggings"):
		seed += 1
		make(van / "entity" / "equipment" / layer / "iron.png", cf(f"entity/equipment/{layer}/iron.png"),
				f"entity/equipment/{layer}/quartz.png", 4, 10, seed)


if __name__ == "__main__":
	main()
