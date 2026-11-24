#!/usr/bin/env python3
# dte:B16,C4
"""Placeholder textures for Bubba's Infused Gems, drawn from simple shapes at 16x and 32x (specs/textures.md).

  python tools/gem-textures.py

Each gem is two layers:
- a grayscale body, tinted in game by the effect's potion color (pushed toward full saturation in GemColorSource).
  It holds all the shading, including a solid dark rim, so the edge is a deep shade of the gem's own color like vanilla
  gems, not a black line;
- an untinted shine layer: white glints (never on the rim, which stays solid), so gems read as polished jewels.
A second-effect highlight facet was tried and dropped (owner). The 32x set is drawn at 32 pixels with finer shading,
not scaled up. Placeholders until a person draws the real art; never AI-generated. Needs Pillow.
"""
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "mods" / "bubbas_infused_gems" / "src" / "main" / "resources"
OUT = {16: RES / "assets" / "bubbas_infused_gems" / "textures" / "item",
	32: RES / "resourcepacks" / "bubbas_infused_gems_32x" / "assets" / "bubbas_infused_gems" / "textures" / "item"}
LIGHT = (-0.7071, -0.7071)  # light from the top left


def inside(poly, x, y):
	c = False
	for i in range(len(poly)):
		(x1, y1), (x2, y2) = poly[i], poly[(i + 1) % len(poly)]
		if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
			c = not c
	return c


def mask(n, test):
	return [[test((x + 0.5) / n, (y + 0.5) / n) for x in range(n)] for y in range(n)]


def edge(m, x, y):
	n = len(m)
	return m[y][x] and any(not (0 <= x + dx < n and 0 <= y + dy < n and m[y + dy][x + dx]) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))


def quant(v, n):
	steps = 6 if n == 16 else 10  # 32x: more shading levels (Faithful style)
	return int(round(max(0.0, min(1.0, v)) * steps) / steps * 255)


def octagon(hw, hh, c):
	"""An octagon centred on (0.5, 0.5): half width hw, half height hh, corners cut by c."""
	return [(0.5 - hw + c, 0.5 - hh), (0.5 + hw - c, 0.5 - hh), (0.5 + hw, 0.5 - hh + c), (0.5 + hw, 0.5 + hh - c),
		(0.5 + hw - c, 0.5 + hh), (0.5 - hw + c, 0.5 + hh), (0.5 - hw, 0.5 + hh - c), (0.5 - hw, 0.5 - hh + c)]


REG = 2 / (2 + 2 ** 0.5)  # corner cut of a regular octagon, as a fraction of the half width
OUTER = octagon(0.36, 0.36, 0.36 * REG)  # finished gem: a regular octagon seen from above, emerald-like, not an emerald copy (owner)
TABLE = octagon(0.17, 0.17, 0.17 * REG)  # the flat top facet


def put(img, x, y, rgba):
	n = img.size[0]
	if 0 <= x < n and 0 <= y < n:
		img.putpixel((x, y), rgba)


def cut_gem(n):
	m = mask(n, lambda u, v: inside(OUTER, u, v))
	body, shine = (Image.new("RGBA", (n, n), (0, 0, 0, 0)) for _ in range(2))
	edges = []  # (outward normal, offset) for each side of the outer octagon
	for i in range(8):
		(x1, y1), (x2, y2) = OUTER[i], OUTER[(i + 1) % 8]
		nx, ny = y2 - y1, x1 - x2
		ln = math.hypot(nx, ny)
		edges.append((nx / ln, ny / ln, (nx * x1 + ny * y1) / ln))
	for y in range(n):
		for x in range(n):
			if not m[y][x]:
				continue
			u, v = (x + 0.5) / n, (y + 0.5) / n
			nx, ny, _ = min(edges, key=lambda e: e[2] - (e[0] * u + e[1] * v))
			lit = LIGHT[0] * nx + LIGHT[1] * ny  # -1 facing away from the light, 1 facing it
			if edge(m, x, y):
				g = quant(0.40, n)  # rim: one solid, opaque mid-dark shade of the gem color all the way round
				body.putpixel((x, y), (g, g, g, 255))
				continue
			if inside(TABLE, u, v):  # table: the gem's full color, darkening toward the bottom right
				shade = 0.92 - 0.45 * ((u - 0.5) + (v - 0.5))
			else:  # bevels: strong light and dark, like a cut stone
				shade = 0.62 + 0.38 * lit
			g = quant(shade, n)
			body.putpixel((x, y), (g, g, g, 255))
	glint(shine, n, 0.40, 0.38)
	return body, shine


def raw_gem(n):
	def test(u, v):
		return (u - 0.5) ** 2 + (v - 0.5) ** 2 <= 0.33 ** 2  # round and centred in the slot
	m = mask(n, test)
	body, shine = (Image.new("RGBA", (n, n), (0, 0, 0, 0)) for _ in range(2))
	for y in range(n):
		for x in range(n):
			if not m[y][x]:
				continue
			u, v = (x + 0.5) / n, (y + 0.5) / n
			if edge(m, x, y):
				g = quant(0.40, n)
				body.putpixel((x, y), (g, g, g, 255))
				continue
			d = math.hypot(u - 0.40, v - 0.40)
			swirl = 0.08 * math.sin(10 * math.atan2(v - 0.5, u - 0.5) + 14 * math.hypot(u - 0.5, v - 0.5))
			g = quant(1.05 - 1.1 * d + swirl, n)
			body.putpixel((x, y), (g, g, g, 255))
	glint(shine, n, 0.40, 0.38)
	return body, shine


def glint(img, n, u, v):
	"""A white specular spot with a short diagonal streak, plus a small twinkle lower right."""
	x, y = int(u * n), int(v * n)
	if n == 16:
		pts = [((0, 0), 255), ((1, 0), 220), ((0, 1), 220), ((-1, -1), 160)]
		twinkle = [((0, 0), 190)]
	else:
		pts = [((0, 0), 255), ((1, 0), 255), ((0, 1), 255), ((1, 1), 230), ((2, 0), 170), ((0, 2), 170),
			((-1, -1), 200), ((-2, -2), 140), ((-1, 0), 150), ((0, -1), 150)]
		twinkle = [((0, 0), 230), ((1, 0), 140), ((-1, 0), 140), ((0, 1), 140), ((0, -1), 140)]
	for (dx, dy), a in pts:
		put(img, x + dx, y + dy, (255, 255, 255, a))
	tx, ty = int(0.62 * n), int(0.62 * n)
	for (dx, dy), a in twinkle:
		put(img, tx + dx, ty + dy, (255, 255, 255, a))


def main():
	for n, out in OUT.items():
		out.mkdir(parents=True, exist_ok=True)
		for name, fn in (("gem", cut_gem), ("infused_gem", raw_gem)):
			body, shine = fn(n)
			body.save(out / f"{name}.png")
			shine.save(out / f"{name}_outline.png")  # file name kept: the models point at it
		(out / "gem_highlight.png").unlink(missing_ok=True)
		print(f"wrote {n}x textures to {out.relative_to(ROOT)}")
	icon()


def jewel(rgb, sat=0.55, bright=0.35):
	"""Same as GemColorSource.jewel: more saturation and brightness, same hue."""
	import colorsys
	h, s, v = colorsys.rgb_to_hsv(*(c / 255 for c in rgb))
	s = s if s < 0.08 else s + (1 - s) * sat
	v = v + (1 - v) * bright
	return tuple(round(c * 255) for c in colorsys.hsv_to_rgb(h, s, v))


def tint(img, rgb):
	out = img.copy()
	px = out.load()
	for y in range(out.size[1]):
		for x in range(out.size[0]):
			r, g, b, a = px[x, y]
			px[x, y] = (r * rgb[0] // 255, g * rgb[1] // 255, b * rgb[2] // 255, a)
	return out


def icon():
	"""The mod icon: the 32x gem in Speed's jewel color, scaled to 128 (a placeholder too)."""
	body, shine = cut_gem(32)
	img = Image.alpha_composite(tint(body, jewel((51, 172, 220))), shine)
	img.resize((128, 128), Image.Resampling.NEAREST).save(RES / "assets" / "bubbas_infused_gems" / "icon.png")


if __name__ == "__main__":
	main()
