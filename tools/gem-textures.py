#!/usr/bin/env python3
# dte:B16,C4
"""Placeholder textures for Bubba's Infused Gems, drawn from facet geometry at 16x and 32x (specs/textures.md).

  python tools/gem-textures.py

Each gem is two layers:
- a grayscale body, tinted in game by the effect's potion color (pushed toward full saturation in GemColorSource).
  It holds the facet value contrast: every facet is one flat grey level (G_*), lit from the top left, with a solid 1-px
  dark rim, so the edge is a deep shade of the gem's own color like vanilla gems, not a black line;
- an untinted shine layer (`<name>_outline.png`, the name the models use): opaque white glints and facet-edge
  highlights, plus the iridescence, translucent cool and warm tints over a few facets and a short rainbow along the
  lit edge, which shift the hue whatever the effect color is.
Shapes:
- gem (finished): a cut gemstone seen from the side, like vanilla's diamond: a flat table on top, five crown facets
  catching the light, a bright girdle line, four pavilion facets narrowing to the culet below.
- infused_gem: a round brilliant seen from above: an octagonal table, sixteen star and kite facets around it
  alternating light and dark.
The 32x set is drawn from the same geometry at 32 pixels, not scaled up, with Classic Faithful 32x Jappa touches (checker
dither on the table, facet-edge highlight lines). Placeholders until a person draws the real art; never AI-generated.
Needs Pillow.
"""
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "mods" / "bubbas_infused_gems" / "src" / "main" / "resources"
OUT = {16: RES / "assets" / "bubbas_infused_gems" / "textures" / "item",
	32: RES / "resourcepacks" / "bubbas_infused_gems_32x" / "assets" / "bubbas_infused_gems" / "textures" / "item"}

# Grey levels of the body (tinted in game), darkest to brightest.
G_OUT, G_DARK, G_MIDDARK, G_MID, G_LIGHT, G_BRIGHT = 40, 78, 120, 168, 218, 255
WHITE = (255, 255, 255, 255)
# Iridescence (untinted overlay): a cool and a warm sheen, and the rainbow along the lit edge.
COOL = (150, 230, 255, 80)
WARM = (255, 170, 230, 70)
RAINBOW = [(255, 90, 90, 150), (255, 200, 80, 150), (140, 255, 120, 150), (90, 220, 255, 150), (150, 130, 255, 150),
	(255, 110, 230, 150)]


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


def grey(g):
	return (g, g, g, 255)


def put(img, x, y, rgba):
	n = img.size[0]
	if 0 <= x < n and 0 <= y < n:
		img.putpixel((x, y), rgba)


# --- the finished gem: side view --------------------------------------------------------------------------------------
TOP, GIRDLE, CULET = 0.13, 0.40, 0.92
A, M, B = (0.30, TOP), (0.50, TOP), (0.70, TOP)
G = {k: (k / 100, GIRDLE) for k in (8, 30, 50, 70, 92)}
G[8], G[92] = (0.11, GIRDLE), (0.89, GIRDLE)
P = (0.50, CULET)
OUTLINE = [A, B, G[92], P, G[8]]
# (polygon, grey level, overlay) per facet; overlay "cool"/"warm" puts the iridescent sheen on it.
CUT_FACETS = [
	([A, G[8], G[30]], G_BRIGHT, "cool"),
	([A, G[30], M], G_LIGHT, None),
	([M, G[30], G[70]], G_LIGHT, "table"),
	([M, G[70], B], G_MID, "warm"),
	([B, G[70], G[92]], G_MIDDARK, None),
	([G[8], G[30], P], G_MID, None),
	([G[30], G[50], P], G_LIGHT, None),
	([G[50], G[70], P], G_MIDDARK, None),
	([G[70], G[92], P], G_DARK, None),
]


def cut_gem(n):
	m = mask(n, lambda u, v: inside(OUTLINE, u, v))
	body, shine = (Image.new("RGBA", (n, n), (0, 0, 0, 0)) for _ in range(2))
	girdle_y = int(GIRDLE * n)
	for y in range(n):
		for x in range(n):
			if not m[y][x]:
				continue
			u, v = (x + 0.5) / n, (y + 0.5) / n
			if edge(m, x, y):
				body.putpixel((x, y), grey(G_OUT))
				continue
			g, over = G_MID, None
			for poly, level, o in CUT_FACETS:
				if inside(poly, u, v):
					g, over = level, o
					break
			if over == "table" and n > 16 and (x + y) % 2 and v > (TOP + GIRDLE) / 2:
				g = G_MID  # checker dither on the shaded half of the table facet
			if y == girdle_y:  # the girdle catches the light on the lit side
				g = G_BRIGHT if u < 0.5 else G_MID
			body.putpixel((x, y), grey(g))
			if over == "cool":
				shine.putpixel((x, y), COOL)
			elif over == "warm":
				shine.putpixel((x, y), WARM)
	# Rainbow along the lit upper-left edge, just inside the rim (32x: the whole edge, 16x: a few pixels).
	edge_pts = [(x, y) for y in range(n) for x in range(n) if m[y][x] and not edge(m, x, y)
		and edge(m, x - 1, y) and (x + 0.5) / n < 0.32 and (y + 0.5) / n < GIRDLE]
	edge_pts.sort(key=lambda p: p[1])
	for i, (x, y) in enumerate(edge_pts):
		shine.putpixel((x, y), RAINBOW[(i * 6) // max(1, len(edge_pts))])
	# White glints: the table's lit corner and a sparkle on the crown, a small star on the pavilion at 32x.
	if n == 16:
		pts = [(5, 4), (6, 4), (4, 5), (10, 9)]
	else:
		pts = [(10, 7), (11, 7), (12, 7), (10, 8), (9, 9), (8, 10), (12, 13), (16, 18), (16, 19), (15, 19), (17, 19), (16, 20)]
	for x, y in pts:
		if m[y][x] and not edge(m, x, y):
			put(shine, x, y, WHITE)
	return body, shine


# --- the infused gem: a round brilliant seen from above ----------------------------------------------------------------
R_OUT, R_TABLE = 0.34, 0.15
LIGHT_ANGLE = math.atan2(-1, -1)  # light from the top left


def raw_gem(n):
	cx = cy = 0.5
	m = mask(n, lambda u, v: (u - cx) ** 2 + (v - cy) ** 2 <= R_OUT ** 2)
	body, shine = (Image.new("RGBA", (n, n), (0, 0, 0, 0)) for _ in range(2))
	table = [(cx + R_TABLE * math.cos(math.pi / 8 + k * math.pi / 4), cy + R_TABLE * math.sin(math.pi / 8 + k * math.pi / 4)) for k in range(8)]
	for y in range(n):
		for x in range(n):
			if not m[y][x]:
				continue
			u, v = (x + 0.5) / n, (y + 0.5) / n
			if edge(m, x, y):
				body.putpixel((x, y), grey(G_OUT))
				continue
			if inside(table, u, v):
				g = G_LIGHT
				if n > 16 and (x + y) % 2 and (u - cx) + (v - cy) > 0.02:
					g = G_MID
				body.putpixel((x, y), grey(g))
				continue
			a = math.atan2(v - cy, u - cx)
			k = int(((a - math.pi / 8) % (2 * math.pi)) // (math.pi / 8))  # 16 slices, two per table side
			mid = math.pi / 8 + (k + 0.5) * math.pi / 8
			lit = math.cos(mid - LIGHT_ANGLE)  # 1 facing the light, -1 away
			level = [G_DARK, G_MIDDARK, G_MID, G_LIGHT, G_BRIGHT][min(4, int((lit + 1) / 2 * 5))]
			if k % 2:  # kite facets a step darker than the star facets beside them
				level = {G_BRIGHT: G_LIGHT, G_LIGHT: G_MID, G_MID: G_MIDDARK, G_MIDDARK: G_DARK}.get(level, level)
			body.putpixel((x, y), grey(level))
			if k == 10:
				shine.putpixel((x, y), COOL)
			elif k == 13:
				shine.putpixel((x, y), WARM)
	# Rainbow round the lit upper-left rim, just inside the outline.
	rim = [(x, y) for y in range(n) for x in range(n) if m[y][x] and not edge(m, x, y)
		and any(edge(m, x + dx, y + dy) for dx, dy in ((-1, 0), (0, -1)) if 0 <= x + dx < n and 0 <= y + dy < n)
		and (x + 0.5) / n + (y + 0.5) / n < 0.86]
	rim.sort(key=lambda p: math.atan2(p[1] + 0.5 - cy * n, p[0] + 0.5 - cx * n))
	for i, (x, y) in enumerate(rim):
		shine.putpixel((x, y), RAINBOW[(i * 6) // max(1, len(rim))])
	if n == 16:
		pts = [(6, 6), (7, 6), (6, 7), (10, 10)]
	else:
		pts = [(12, 12), (13, 12), (14, 12), (12, 13), (12, 14), (11, 11), (20, 20), (20, 21), (21, 20)]
	for x, y in pts:
		put(shine, x, y, WHITE)
	return body, shine


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
