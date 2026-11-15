#!/usr/bin/env python3
# dte:B16,C4
"""Placeholder textures for Bubba's Infused Gems, drawn from simple shapes at 16x and 32x (specs/textures.md).

  python tools/gem-textures.py

Each gem is three layers: a grayscale body (tinted in game by the main effect's potion color), an untinted outline
and sparkle, and a small highlight facet (tinted by a second effect: the owner's experiment). The 32x set is drawn at
32 pixels with finer shading, not scaled up. Placeholders until a person draws the real art; never AI-generated.
Needs Pillow.
"""
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "mods" / "bubbas_infused_gems" / "src" / "main" / "resources"
OUT = {16: RES / "assets" / "bubbas_infused_gems" / "textures" / "item",
	32: RES / "resourcepacks" / "bubbas_infused_gems_32x" / "assets" / "bubbas_infused_gems" / "textures" / "item"}

CUT = [(0.06, 0.38), (0.28, 0.10), (0.72, 0.10), (0.94, 0.38), (0.50, 0.94)]  # finished gem: crown + pavilion
CROWN_V = 0.38


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
	steps = 5 if n == 16 else 9  # 32x: more shading levels (Faithful style)
	return int(round(v * steps) / steps * 255)


def octagon(hw, hh, c):
	"""A tall octagon centred on (0.5, 0.5): half width hw, half height hh, corners cut by c."""
	return [(0.5 - hw + c, 0.5 - hh), (0.5 + hw - c, 0.5 - hh), (0.5 + hw, 0.5 - hh + c), (0.5 + hw, 0.5 + hh - c),
		(0.5 + hw - c, 0.5 + hh), (0.5 - hw + c, 0.5 + hh), (0.5 - hw, 0.5 + hh - c), (0.5 - hw, 0.5 - hh + c)]


OUTER = octagon(0.31, 0.45, 0.20)  # finished gem: an emerald-style step cut seen from above (owner)
TABLE = octagon(0.13, 0.25, 0.07)  # the flat top facet


def cut_gem(n):
	m = mask(n, lambda u, v: inside(OUTER, u, v))
	body, line, high = (Image.new("RGBA", (n, n), (0, 0, 0, 0)) for _ in range(3))
	edges = []  # (outward normal, offset) for each side of the outer octagon
	for i in range(8):
		(x1, y1), (x2, y2) = OUTER[i], OUTER[(i + 1) % 8]
		nx, ny = y2 - y1, x1 - x2
		ln = math.hypot(nx, ny)
		nx, ny = nx / ln, ny / ln
		edges.append((nx, ny, nx * x1 + ny * y1))
	for y in range(n):
		for x in range(n):
			if not m[y][x]:
				continue
			u, v = (x + 0.5) / n, (y + 0.5) / n
			if edge(m, x, y):
				line.putpixel((x, y), (34, 30, 44, 255))
				continue
			if inside(TABLE, u, v):  # table: bright and flat, a little darker toward the bottom right
				shade = 0.88 - 0.25 * ((u - 0.5) + (v - 0.5))
			else:  # bevel: the facet is the side the pixel is closest to, lit from the top left
				nx, ny, _ = min(edges, key=lambda e: e[2] - (e[0] * u + e[1] * v))
				shade = 0.66 + 0.30 * (-0.7071 * nx - 0.7071 * ny)
				if nx < -0.5 and ny < -0.5:  # the upper-left corner facet carries the second effect's color
					high.putpixel((x, y), (235, 235, 235, 255))
			g = quant(max(0.3, min(1.0, shade)), n)
			body.putpixel((x, y), (g, g, g, 255))
	sparkle(line, n, 0.40, 0.30)
	return body, line, high


def raw_gem(n):
	def test(u, v):
		return ((u - 0.5) / 0.36) ** 2 + ((v - 0.55) / 0.38) ** 2 <= 1.0 and v > 0.14
	m = mask(n, test)
	body, line, high = (Image.new("RGBA", (n, n), (0, 0, 0, 0)) for _ in range(3))
	for y in range(n):
		for x in range(n):
			if not m[y][x]:
				continue
			u, v = (x + 0.5) / n, (y + 0.5) / n
			if edge(m, x, y):
				line.putpixel((x, y), (34, 30, 44, 255))
				continue
			d = math.hypot(u - 0.38, v - 0.38)
			swirl = 0.08 * math.sin(10 * math.atan2(v - 0.55, u - 0.5) + 14 * math.hypot(u - 0.5, v - 0.55))
			g = quant(max(0.4, min(1.0, 1.0 - 0.9 * d + swirl)), n)
			body.putpixel((x, y), (g, g, g, 255))
	sparkle(line, n, 0.34, 0.32)
	return body, line, high


def sparkle(img, n, u, v):
	x, y = int(u * n), int(v * n)
	pts = [(0, 0)] + ([(1, 0), (0, 1)] if n == 16 else [(1, 0), (0, 1), (-1, 0), (0, -1), (2, 0), (0, 2)])
	for dx, dy in pts:
		img.putpixel((x + dx, y + dy), (255, 255, 255, 255 if (dx, dy) == (0, 0) else 200))


def main():
	for n, out in OUT.items():
		out.mkdir(parents=True, exist_ok=True)
		for name, fn in (("gem", cut_gem), ("infused_gem", raw_gem)):
			body, line, high = fn(n)
			body.save(out / f"{name}.png")
			line.save(out / f"{name}_outline.png")
			if name == "gem":
				high.save(out / f"{name}_highlight.png")
		print(f"wrote {n}x textures to {out.relative_to(ROOT)}")
	icon()


def tint(img, rgb):
	out = img.copy()
	px = out.load()
	for y in range(out.size[1]):
		for x in range(out.size[0]):
			r, g, b, a = px[x, y]
			px[x, y] = (r * rgb[0] // 255, g * rgb[1] // 255, b * rgb[2] // 255, a)
	return out


def icon():
	"""The mod icon: the 32x gem in Speed blue with a Strength highlight, scaled to 128 (a placeholder too)."""
	body, line, high = cut_gem(32)
	img = Image.alpha_composite(Image.alpha_composite(tint(body, (51, 172, 220)), tint(high, (255, 199, 0))), line)
	img.resize((128, 128), Image.Resampling.NEAREST).save(RES / "assets" / "bubbas_infused_gems" / "icon.png")


if __name__ == "__main__":
	main()
