#!/usr/bin/env python3
# dte:C8,B16
"""Lava bubble particle frames (specs/world-tweaks.md "Lava bubbles"), 16x and 32x.

A round lava blister swelling, then bursting into a ring of droplets. Crisp pixel art in vanilla lava's own palette
(sampled from textures/block/lava_still.png) with a cooled dark crust at the rim and a hot glint, so it stands out on
the lava. The particle faces the camera and is centred on the surface, so the lava hides its lower half from the side
(a half-dome) and from above it shows as a whole blister. Frames 0-4 grow, 5-6 burst. Agent-made, listed in docs/art-todo.md.

Usage: python3 make_lava_bubble_textures.py <path to build/mcsrc/26.3/vanilla>
"""
import math
import sys
from pathlib import Path
from PIL import Image

HERE = Path(__file__).resolve().parent
MOD = HERE.parent / "src" / "main" / "resources"
OUT16 = MOD / "assets" / "bubbas_world_tweaks" / "textures" / "particle"
OUT32 = MOD / "resourcepacks" / "bubbas_world_tweaks_32x" / "assets" / "bubbas_world_tweaks" / "textures" / "particle"


def palette(vanilla):
	im = Image.open(Path(vanilla) / "assets/minecraft/textures/block/lava_still.png").convert("RGB")
	cols = sorted(set(im.getdata()), key=lambda c: sum(c))
	pick = lambda f: cols[int(f * (len(cols) - 1))]
	dark = pick(0.0)
	# A crust darker than any lava pixel (lava cooling on the skin of the bubble), and a near-white hot highlight.
	crust = tuple(int(c * 0.62) for c in dark)
	return {"crust": crust, "dark": dark, "mid": pick(0.45), "light": pick(0.8), "hot": pick(1.0),
			"glint": (255, 236, 170)}


def frame(size, pal, i):
	"""A round blister: the particle faces the camera and is centred on the surface, so the lava hides its lower half
	from the side (a dome) and from above it reads as a whole round blister."""
	s = size / 16.0
	img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
	px = img.load()
	c = size / 2.0
	if i <= 4:
		r = [2.5, 3.5, 4.5, 5.5, 6.5][i] * s
		for y in range(size):
			for x in range(size):
				dx = x + 0.5 - c
				dy = y + 0.5 - c
				d = math.sqrt(dx * dx + dy * dy)
				if d > r:
					continue
				if d > r - 1.0 * s:
					col = pal["crust"]
				elif d > r - 2.0 * s and dy > 0:
					col = pal["dark"]
				elif -dx - dy > r * 0.9 and d < r * 0.72:
					col = pal["glint"]
				elif -dx - dy > r * 0.3:
					col = pal["hot"]
				elif dx + dy > r * 0.5:
					col = pal["mid"]
				else:
					col = pal["light"]
				px[x, y] = col + (255,)
	else:
		# Burst: a broken ring of droplets thrown outward and a few flung up.
		spread = (6.0 if i == 5 else 7.2) * s
		for k in range(12):
			a = 2 * math.pi * k / 12.0 + (0.2 if i == 6 else 0.0)
			x = int(c + math.cos(a) * spread)
			y = int(c + math.sin(a) * spread * 0.8 - (1.5 * s if i == 6 else 0))
			col = pal["glint"] if k % 4 == 0 else (pal["hot"] if k % 2 == 0 else pal["light"])
			for oy in range(max(1, int(s))):
				for ox in range(max(1, int(s))):
					if 0 <= x + ox < size and 0 <= y + oy < size:
						px[x + ox, y + oy] = col + (255,)
		if i == 5:
			for x in range(int(c - 2 * s), int(c + 2 * s)):
				for y in range(int(c - 1 * s), int(c + 1 * s)):
					px[x, y] = pal["crust"] + (255,)
	return img


def main():
	pal = palette(sys.argv[1])
	for out, size in ((OUT16, 16), (OUT32, 32)):
		out.mkdir(parents=True, exist_ok=True)
		for i in range(7):
			frame(size, pal, i).save(out / f"lava_bubble_{i}.png")
	print("wrote", OUT16, OUT32)


if __name__ == "__main__":
	main()
