#!/usr/bin/env python3
# dte:C8,B16
"""Lava bubble particle frames and the lava geyser's soot plume sprites, 16x and 32x.

32x (Classic Faithful 32x Jappa style): the bubbles use a few flat lava shades (the shade is snapped to LEVELS32, no
per-pixel grain), and the soot plumes are CF32J's own geyser plume sprites recoloured to soot (credited in the mod's
CREDITS.md, Faithful License, https://faithfulpack.net/license).

Usage: python3 make_lava_bubble_textures.py <path to build/mcsrc/26.3/vanilla> [<path to the CF32J zip>]
"""
import io
import math
import os
import sys
import zipfile
from collections import Counter
from pathlib import Path
from PIL import Image

HERE = Path(__file__).resolve().parent
MOD = HERE.parent / "src" / "main" / "resources"
OUT16 = MOD / "assets" / "bubbas_world_tweaks" / "textures" / "particle"
OUT32 = MOD / "resourcepacks" / "bubbas_world_tweaks_32x" / "assets" / "bubbas_world_tweaks" / "textures" / "particle"

# Shade ramp ends, as frequency quantiles of vanilla lava's own pixels.
BASE_Q = 0.10
TOP_Q = 0.96
HIGHLIGHT_Q = 1.0
GRAIN = 0.10
RADII = [2.5, 3.5, 4.5, 5.5, 6.5]
# The 32x bubbles' shade levels (ramp quantiles).
LEVELS32 = [0.10, 0.35, 0.6, 0.8, 0.92, 0.985, 1.0]
CF32J_ZIP = Path(os.environ.get("APPDATA", "")) / "ModrinthApp/profiles/porp/resourcepacks/CF32J-2026r21.zip"


class Ramp:
	"""Vanilla lava colours, darkest to brightest, weighted by how often each appears."""

	def __init__(self, image):
		counts = Counter(image.getdata())
		self.cols = sorted(counts, key=lambda c: (sum(c), c))
		total = sum(counts.values())
		self.cum = []
		acc = 0
		for col in self.cols:
			acc += counts[col]
			self.cum.append(acc / total)

	def at(self, q):
		q = min(max(q, 0.0), 1.0)
		for col, c in zip(self.cols, self.cum):
			if c >= q:
				return col
		return self.cols[-1]


def grain(lava, size):
	"""Per-pixel brightness rank of one lava frame at this size, -0.5..0.5."""
	tile = lava.crop((0, 0, lava.width, lava.width)).resize((size, size), Image.Resampling.NEAREST)
	vals = [sum(tile.getpixel((x, y))) for y in range(size) for x in range(size)]
	order = sorted(vals)
	rank = {v: i / (len(order) - 1) for i, v in enumerate(order)}
	return [[rank[sum(tile.getpixel((x, y)))] - 0.5 for x in range(size)] for y in range(size)]


def dome(size, ramp, noise, r, grain_amount=GRAIN, top=TOP_Q, levels=None):
	"""A swelling of the lava surface: lit from above, brightest at the top, a little darker where it meets the lava."""
	img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
	px = img.load()
	c = size / 2.0
	for y in range(size):
		for x in range(size):
			dx = (x + 0.5 - c) / r
			dy = (y + 0.5 - c) / r
			d2 = dx * dx + dy * dy
			if d2 > 1.0:
				continue
			nz = math.sqrt(1.0 - d2)
			up = ((1.0 - dy) / 2.0) ** 1.2
			t = BASE_Q + (top - BASE_Q) * up * (0.7 + 0.3 * nz) + grain_amount * noise[y][x]
			hx = (dx + 0.18) / max(0.38, 1.3 / r)
			hy = (dy + 0.5) / max(0.24, 0.9 / r)
			e = hx * hx + hy * hy
			if e < 1.0:
				t = max(t, top + (HIGHLIGHT_Q - top) * (1.0 - e))
			if levels:
				t = min(levels, key=lambda q: abs(q - t))
			px[x, y] = ramp.at(t) + (255,)
	return img


def burst(size, ramp, i):
	"""The bubble bursts: lava droplets flung out in a broken ring, then falling."""
	s = size / 16.0
	img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
	px = img.load()
	c = size / 2.0
	spread = (5.0 if i == 5 else 6.5) * s
	drop = max(1, int(round(s)))
	for k in range(10):
		a = 2 * math.pi * k / 10.0 + (0.31 if i == 6 else 0.0)
		x = int(c + math.cos(a) * spread)
		y = int(c + math.sin(a) * spread * 0.7 + (s if i == 6 else 0.0) - 1.5 * s * max(0.0, -math.sin(a)))
		col = ramp.at([1.0, 0.985, 0.95][k % 3] - (0.04 if i == 6 else 0.0))
		for oy in range(drop):
			for ox in range(drop):
				if 0 <= x + ox < size and 0 <= y + oy < size:
					px[x + ox, y + oy] = col + (255,)
	return img


# Vanilla geyser plume shades to soot shades, darkest to lightest.
SOOT = {(193, 193, 193): (36, 33, 32), (195, 195, 195): (36, 33, 32), (198, 198, 198): (36, 33, 32), (210, 210, 210): (52, 48, 46),
	(224, 224, 224): (70, 65, 62)}


def soot16(src):
	"""Vanilla's geyser plume sprite with its three shades recoloured to soot."""
	img = src.convert("RGBA")
	px = img.load()
	for y in range(img.height):
		for x in range(img.width):
			r, g, b, a = px[x, y]
			if a:
				px[x, y] = SOOT.get((r, g, b), (r // 4, g // 4, b // 4)) + (a,)
	return img


def soot32(src):
	"""CF32J's geyser plume sprite with its shades recoloured to soot."""
	img = src.convert("RGBA")
	px = img.load()
	for y in range(img.height):
		for x in range(img.width):
			r, g, b, a = px[x, y]
			if a:
				if (r, g, b) not in SOOT:
					raise ValueError(f"unmapped CF32J plume shade {(r, g, b)}")
				px[x, y] = SOOT[(r, g, b)] + (a,)
	return img


def main():
	vanilla = Path(sys.argv[1])
	still = Image.open(vanilla / "assets/minecraft/textures/block/lava_still.png").convert("RGB")
	flow = Image.open(vanilla / "assets/minecraft/textures/block/lava_flow.png").convert("RGB")
	for out, size, src in ((OUT16, 16, still), (OUT32, 32, flow)):
		out.mkdir(parents=True, exist_ok=True)
		ramp = Ramp(src)
		noise = grain(src, size)
		s = size / 16.0
		for i, r in enumerate(RADII):
			if size == 16:
				dome(size, ramp, noise, r * s).save(out / f"lava_bubble_{i}.png")
			else:
				dome(size, ramp, noise, r * s, grain_amount=0.0, levels=LEVELS32).save(out / f"lava_bubble_{i}.png")
		for i in (5, 6):
			burst(size, ramp, i).save(out / f"lava_bubble_{i}.png")
	zf = zipfile.ZipFile(sys.argv[2] if len(sys.argv) > 2 else CF32J_ZIP)
	for i in range(1, 9):
		plume = soot16(Image.open(vanilla / f"assets/minecraft/textures/particle/geyser_plume_0{i}.png"))
		plume.save(OUT16 / f"lava_geyser_plume_0{i}.png")
		cf = Image.open(io.BytesIO(zf.read(f"assets/minecraft/textures/particle/geyser_plume_0{i}.png")))
		soot32(cf).save(OUT32 / f"lava_geyser_plume_0{i}.png")
	print("wrote", OUT16, OUT32)


if __name__ == "__main__":
	main()
