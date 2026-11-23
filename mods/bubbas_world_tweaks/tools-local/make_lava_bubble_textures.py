#!/usr/bin/env python3
# dte:C8,B16
"""Lava bubble particle frames and the lava geyser's soot plume sprites, 16x and 32x.

Usage: python3 make_lava_bubble_textures.py <path to build/mcsrc/26.3/vanilla>
"""
import math
import sys
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


def dome(size, ramp, noise, r, grain_amount=GRAIN, top=TOP_Q):
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
SOOT = {(193, 193, 193): (36, 33, 32), (210, 210, 210): (52, 48, 46), (224, 224, 224): (70, 65, 62)}


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


def soot32(img16):
	"""2x, with an in-between shade where two soot shades meet, for a softer Faithful-style edge."""
	big = img16.resize((img16.width * 2, img16.height * 2), Image.Resampling.NEAREST)
	src = big.copy().load()
	px = big.load()
	for y in range(big.height):
		for x in range(big.width):
			r, g, b, a = src[x, y]
			if not a or (x + y) % 2:
				continue
			for nx, ny in ((x + 1, y), (x, y + 1), (x - 1, y), (x, y - 1)):
				if 0 <= nx < big.width and 0 <= ny < big.height:
					r2, g2, b2, a2 = src[nx, ny]
					if a2 and r2 > r:
						px[x, y] = ((r + r2) // 2, (g + g2) // 2, (b + b2) // 2, a)
						break
	return big


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
			dome(size, ramp, noise, r * s).save(out / f"lava_bubble_{i}.png")
		for i in (5, 6):
			burst(size, ramp, i).save(out / f"lava_bubble_{i}.png")
	for i in range(1, 9):
		plume = soot16(Image.open(vanilla / f"assets/minecraft/textures/particle/geyser_plume_0{i}.png"))
		plume.save(OUT16 / f"lava_geyser_plume_0{i}.png")
		soot32(plume).save(OUT32 / f"lava_geyser_plume_0{i}.png")
	print("wrote", OUT16, OUT32)


if __name__ == "__main__":
	main()
