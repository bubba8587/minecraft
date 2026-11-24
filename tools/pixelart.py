#!/usr/bin/env python3
# dte:B16
"""Small helpers shared by the texture scripts (specs/textures.md): hand-placed pixel grids drawn at 16x, and a
Faithful-style 32x twin made from the same grid.

A grid is a list of equal-length strings, one character per pixel; '.' is transparent and every other character is a
key into a palette {char: (r, g, b[, a])}. The 16x texture is the grid as drawn.

The 32x twin (`render32`) is not a plain 2x scale-up. It
  1. doubles the grid, smoothing only the silhouette with Scale2x (EPX) so outer diagonals and curves come out
     smooth instead of stair-stepped, while details inside keep the shape they were drawn with;
  2. thins outlines back to one 32x pixel: an outline pixel that doesn't touch transparency takes the colour given by
     `inner` (usually the material's darkest shade), so edges keep vanilla's weight instead of turning chunky;
  3. bevels each material with light from the top left: a pixel whose upper or left neighbour is a different material
     is lifted, one whose lower or right neighbour is different is dropped;
  4. adds fine texture (`grain`) to the materials named, a gentle per-pixel brightness wobble like Faithful's stone.
Needs Pillow.
"""
from PIL import Image


def check(grid, width=None):
	w = width or len(grid[0])
	for i, row in enumerate(grid):
		if len(row) != w:
			raise ValueError(f"row {i} is {len(row)} wide, expected {w}: {row!r}")
	return grid


def rgba(c):
	return tuple(c) + (255,) if len(c) == 3 else tuple(c)


def render(grid, pal):
	"""The grid as an image, one pixel per character."""
	check(grid)
	img = Image.new("RGBA", (len(grid[0]), len(grid)), (0, 0, 0, 0))
	px = img.load()
	for y, row in enumerate(grid):
		for x, ch in enumerate(row):
			if ch != ".":
				px[x, y] = rgba(pal[ch])
	return img


def scale2x(grid, key=None):
	"""EPX / Scale2x on a character grid. With key (char -> group), the corner rule compares groups (materials)
	and each new pixel copies the character of the source pixel it came from, so shading inside a material stays
	where it was drawn and only the material's silhouette is smoothed."""
	h, w = len(grid), len(grid[0])
	key = key or (lambda ch: ch)

	def at(x, y):
		if 0 <= x < w and 0 <= y < h:
			return grid[y][x]
		return "."

	out = [[None] * (2 * w) for _ in range(2 * h)]
	for y in range(h):
		for x in range(w):
			p = at(x, y)
			A, B, C, D = (x, y - 1), (x + 1, y), (x - 1, y), (x, y + 1)
			a, b, c, d = (key(at(*q)) for q in (A, B, C, D))
			e0 = at(*C) if (c == a and c != d and a != b and c != key(p)) else p
			e1 = at(*B) if (a == b and a != c and b != d and b != key(p)) else p
			e2 = at(*C) if (d == c and d != b and c != a and c != key(p)) else p
			e3 = at(*B) if (b == d and b != a and d != c and b != key(p)) else p
			out[2 * y][2 * x], out[2 * y][2 * x + 1] = e0, e1
			out[2 * y + 1][2 * x], out[2 * y + 1][2 * x + 1] = e2, e3
	return ["".join(r) for r in out]


def mix(c, t, k):
	"""Blend colour c toward colour t by k (0..1)."""
	return tuple(int(round(c[i] + (t[i] - c[i]) * k)) for i in range(3))


def lighten(c, k):
	return mix(c, (255, 255, 255), k)


def darken(c, k):
	return mix(c, (0, 0, 0), k)


def hash01(x, y, seed=0):
	n = (x * 374761393 + y * 668265263 + seed * 2147483647) & 0xFFFFFFFF
	n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
	return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def luma(c):
	return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def ramps(pal, material, outline=""):
	"""{material: [colours darkest..lightest]} from the palette chars of each material (outline chars left out)."""
	out = {}
	for ch, c in pal.items():
		if ch in outline:
			continue
		out.setdefault(material.get(ch, ch), set()).add(rgba(c)[:3])
	return {m: sorted(cs, key=luma) for m, cs in out.items()}


def step(ramp, col, n):
	"""The colour n shades lighter (n > 0) or darker (n < 0) than col on its material's ramp."""
	i = ramp.index(col) if col in ramp else min(range(len(ramp)), key=lambda k: abs(luma(ramp[k]) - luma(col)))
	return ramp[max(0, min(len(ramp) - 1, i + n))]


def render32(grid, pal, material=None, outline="o", inner=None, bevel=0.12, grain=(), grain_amount=0.06, seed=1,
		smooth=True, snap=False, cf=False, grain_p=0.14, thin=None):
	"""The Faithful-style 32x twin of a 16x grid (see the module notes).

	material: {char: name} groups shades of one material so bevels only fall on edges between materials (default:
	every char is its own material). inner: {outline_char: char} colour for outline pixels that don't touch
	transparency (default: leave them). grain: material names that get texture. snap: every pixel snaps to the
	nearest colour already in the icon's palette (Classic Faithful 32x Jappa style: few colours, flat bands, crisp
	edges), so bevels step to a neighbouring shade instead of inventing in-between colours.
	cf: Classic Faithful 32x Jappa mode. Every pixel stays a palette colour: bevels step one shade up (top/left edges)
	or down (bottom/right edges) the material's own ramp, and grain is two-pixel clumps stepped one shade up or down
	(a share grain_p of each), never an in-between colour. thin: {char: char} (cf mode) dark inner-edge shades kept
	one 32x pixel wide: a pixel of that char not touching the outline or transparency takes the mapped char.
	"""
	material = material or {}
	if cf:
		return _render32_cf(grid, pal, material, outline, inner, bevel, grain, seed, smooth, grain_p, thin or {})
	g = scale2x(grid, lambda ch: "." if ch == "." else ("o" if ch in outline else "x")) if smooth else upscale_grid(grid)
	h, w = len(g), len(g[0])

	def at(x, y):
		if 0 <= x < w and 0 <= y < h:
			return g[y][x]
		return "."

	def mat(ch):
		return material.get(ch, ch)

	# Thin outlines that sit against the inside of the shape.
	inner = inner or {}
	g2 = [list(r) for r in g]
	for y in range(h):
		for x in range(w):
			ch = g[y][x]
			if ch in outline and ch in inner:
				touches = any(at(x + dx, y + dy) == "." for dx in (-1, 0, 1) for dy in (-1, 0, 1) if dx or dy)
				if not touches:
					g2[y][x] = inner[ch]
	g = ["".join(r) for r in g2]

	img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
	px = img.load()
	for y in range(h):
		for x in range(w):
			ch = g[y][x]
			if ch == ".":
				continue
			c = rgba(pal[ch])
			col, a = c[:3], c[3]
			if ch not in outline and bevel:
				m = mat(ch)
				up, left = mat(at(x, y - 1)), mat(at(x - 1, y))
				down, right = mat(at(x, y + 1)), mat(at(x + 1, y))
				if up != m or left != m:
					col = lighten(col, bevel)
				elif down != m or right != m:
					col = darken(col, bevel)
			if mat(ch) in grain:
				# Two-pixel clumps, so the texture reads as stone grain rather than noise.
				n = hash01(x // 2 + (y % 2), y, seed) - 0.5
				col = lighten(col, n * 2 * grain_amount) if n > 0 else darken(col, -n * 2 * grain_amount)
			px[x, y] = col + (a,)
	if snap:
		used = sorted({rgba(pal[ch])[:3] for row in g for ch in row if ch != "."})
		for y in range(h):
			for x in range(w):
				c = px[x, y]
				if c[3]:
					near = min(used, key=lambda u: (u[0] - c[0]) ** 2 + (u[1] - c[1]) ** 2 + (u[2] - c[2]) ** 2)
					px[x, y] = near + (c[3],)
	return img


def _render32_cf(grid, pal, material, outline, inner, bevel, grain, seed, smooth, grain_p, thin):
	g = scale2x(grid, lambda ch: "." if ch == "." else ("o" if ch in outline else "x")) if smooth else upscale_grid(grid)
	h, w = len(g), len(g[0])

	def at(x, y):
		return g[y][x] if 0 <= x < w and 0 <= y < h else "."

	def mat(ch):
		return material.get(ch, ch)

	inner = inner or {}
	g2 = [list(r) for r in g]
	for y in range(h):
		for x in range(w):
			ch = g[y][x]
			if ch in outline and ch in inner:
				if not any(at(x + dx, y + dy) == "." for dx in (-1, 0, 1) for dy in (-1, 0, 1) if dx or dy):
					g2[y][x] = inner[ch]
	g = ["".join(r) for r in g2]
	if thin:
		g2 = [list(r) for r in g]
		for y in range(h):
			for x in range(w):
				ch = g[y][x]
				if ch not in thin:
					continue
				near = [at(x + dx, y + dy) for dx in (-1, 0, 1) for dy in (-1, 0, 1) if dx or dy]
				if not any(n == "." or n in outline for n in near) and any(n not in thin for n in near):
					g2[y][x] = thin[ch]
		g = ["".join(r) for r in g2]
	rp = ramps(pal, material, outline)
	img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
	px = img.load()
	for y in range(h):
		for x in range(w):
			ch = g[y][x]
			if ch == ".":
				continue
			c = rgba(pal[ch])
			col, a = c[:3], c[3]
			if ch not in outline:
				m = mat(ch)
				ramp = rp[m]
				if bevel:
					if mat(at(x, y - 1)) != m or mat(at(x - 1, y)) != m:
						col = step(ramp, col, 1)
					elif mat(at(x, y + 1)) != m or mat(at(x + 1, y)) != m:
						col = step(ramp, col, -1)
				if m in grain:
					n = hash01(x // 2 + (y % 2), y, seed)
					if n > 1 - grain_p:
						col = step(ramp, col, 1)
					elif n < grain_p:
						col = step(ramp, col, -1)
			px[x, y] = col + (a,)
	return img


def upscale_grid(grid):
	"""Each pixel as a 2x2 block (for grids that are already pixel-exact at 32x, like UV layouts)."""
	return ["".join(ch * 2 for ch in row) for row in grid for _ in (0, 1)]
