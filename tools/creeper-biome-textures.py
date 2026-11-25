#!/usr/bin/env python3
"""Biome creeper skins for Reworked Mobs (specs/creepers.md, biome creepers).

Recolours vanilla's creeper texture pixel by pixel once per skin, with the palette of the biome's tree trunk (log side) or, for
treeless biomes, its natural ground block, by OKLch lightness matching:

- both palettes go to OKLCh;
- distribution match by OKLch lightness: each creeper colour's rank among the body's pixels (dark to light, pixel-weighted)
  picks the source colour at the same rank among the source texture's pixels, so each source colour turns up about as
  often on the creeper as in the source (dirt stays mostly brown, its few grey pebbles stay few);
- every output pixel is a colour of the source palette, except on the face (owner): the base's black face pixels stay
  black, and its noticeably darker face pixels (below DARK16 / DARK32) take their own matched colour blended FACE_BLEND
  toward black; the face is left out of the range statistics;

The 16x set recolours vanilla's creeper with vanilla's log/ground textures. The 32x set runs the same recolour on
Classic Faithful 32x Jappa's own creeper with CF32J's own log/ground textures (credited in the mod's CREDITS.md and
resourcepacks/bubbas_32x/CREDITS.md); a texture CF32J doesn't have (sulfur) falls back to vanilla's palette. Grass
Grass (plains, meadow) is the grass block top times the biome's grass colour from vanilla's colormap, at both sizes.

A new base creeper regenerates every skin: --base16 (64x32) and --base32 (128x64). The face is only left out of the
range statistics; it is found in the head-front square (UV 8..16, 8..16) as its pixels darker than OKLab L 0.5.

Usage: python3 tools/creeper-biome-textures.py [--mcsrc DIR] [--cf32j ZIP_OR_DIR] [--base16 PNG] [--base32 PNG] [--sheet OUT.png]
"""
import argparse
import math
import os
import subprocess
import sys
import zipfile
from collections import Counter

from PIL import Image, ImageDraw

MOD = "bubbas_reworked_mobs"
OUT16 = f"mods/{MOD}/src/main/resources/assets/{MOD}/textures/entity/creeper/biome"
CF32J_ZIP = os.path.join(os.environ.get("APPDATA", ""), "ModrinthApp/profiles/porp/resourcepacks/CF32J-2026r21.zip")
OUT32 = f"mods/{MOD}/src/main/resources/resourcepacks/{MOD}_32x/assets/{MOD}/textures/entity/creeper/biome"

# skin -> (source block texture, tint (temperature, downfall) for grass-coloured blocks or None)
SKINS = {
	"oak": ("block/oak_log.png", None),
	"birch": ("block/birch_log.png", None),
	"spruce": ("block/spruce_log.png", None),
	"jungle": ("block/jungle_log.png", None),
	"acacia": ("block/acacia_log.png", None),
	"dark_oak": ("block/dark_oak_log.png", None),
	"mangrove": ("block/mangrove_log.png", None),
	"cherry": ("block/cherry_log.png", None),
	"pale_oak": ("block/pale_oak_log.png", None),
	"poplar": ("block/poplar_log.png", None),
	"plains": ("block/grass_block_top.png", (0.8, 0.4)),
	"meadow": ("block/grass_block_top.png", (0.5, 0.8)),
	"sand": ("block/sand.png", None),
	"red_sand": ("block/red_sand.png", None),
	"snow": ("block/snow.png", None),
	"stone": ("block/stone.png", None),
	"mushroom_stem": ("block/mushroom_stem.png", None),
	"dripstone": ("block/dripstone_block.png", None),
	"sulfur": ("block/sulfur.png", None),
}

FACE = (8, 8, 16, 16)  # head front in the 64x32 UV layout
DARK16 = 0.5  # vanilla face: black 0.00, darker 0.31-0.42 (eye top corners and mouth top-left 0.42); the rest 0.58 and up
DARK32 = 0.5  # CF32J face: black 0.00, darker 0.32-0.45 (eye and mouth rims); the rest 0.58 and up
FACE_BLEND = 0.5  # those pixels: their matched colour, this far toward black
BLACK = 0.05  # face pixels darker than this are black and stay black


# OKLab / OKLCh

def _lin(c):
	c /= 255.0
	return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def _gam(c):
	c = 12.92 * c if c <= 0.0031308 else 1.055 * c ** (1 / 2.4) - 0.055
	return max(0, min(255, round(c * 255)))


def oklab(rgb):
	r, g, b = (_lin(v) for v in rgb[:3])
	l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b
	m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b
	s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b
	l, m, s = (math.copysign(abs(v) ** (1 / 3), v) for v in (l, m, s))
	return (0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
			1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
			0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s)


def from_oklab(L, a, b):
	l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3
	m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3
	s = (L - 0.0894841775 * a - 1.2914855480 * b) ** 3
	return (_gam(4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s),
			_gam(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s),
			_gam(-0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s))


def oklch(rgb):
	L, a, b = oklab(rgb)
	return L, math.hypot(a, b), math.atan2(b, a)


def percentile(weighted, p):
	"""weighted: [(value, count)]."""
	items = sorted(weighted)
	total = sum(c for _, c in items)
	acc = 0
	for v, c in items:
		acc += c
		if acc >= total * p:
			return v
	return items[-1][0]


# Sources

class Source:
	"""A resource pack (folder or zip) or vanilla's assets folder: reads assets/minecraft/textures/<rel>."""

	def __init__(self, path, vanilla_assets=False):
		self.path = path
		self.zip = zipfile.ZipFile(path) if os.path.isfile(path) else None
		self.prefix = "textures/" if vanilla_assets else "assets/minecraft/textures/"

	def has(self, rel):
		if self.zip:
			return self.prefix + rel in self.zip.namelist()
		return os.path.isfile(os.path.join(self.path, self.prefix + rel))

	def open(self, rel):
		if self.zip:
			with self.zip.open(self.prefix + rel) as f:
				return Image.open(f).convert("RGBA")
		return Image.open(os.path.join(self.path, self.prefix + rel)).convert("RGBA")


def palette(src, rel, tint, colormap):
	im = src.open(rel)
	if tint:
		t, d = tint
		d *= t
		tc = colormap.getpixel((int((1 - t) * 255), int((1 - d) * 255)))[:3]
	cnt = Counter()
	for px in im.get_flattened_data():
		if px[3] < 128:
			continue
		rgb = px[:3]
		if tint:
			rgb = tuple(v * w // 255 for v, w in zip(rgb, tc))
		cnt[rgb] += 1
	return cnt


def is_face(x, y, L):
	x0, y0, x1, y1 = FACE
	return x0 <= x < x1 and y0 <= y < y1 and L < 0.5


def dark_face_pixels(base, threshold):
	"""The face's noticeably darker pixels, not black: head front (UV 8..16, 8..16), OKLab L from BLACK to threshold."""
	sc = base.size[0] // 64
	px = base.load()
	return [(x, y) for y in range(8 * sc, 16 * sc) for x in range(8 * sc, 16 * sc)
			if px[x, y][3] and BLACK <= oklch(px[x, y])[0] < threshold]


def black_face_pixels(base):
	"""The face's black pixels."""
	sc = base.size[0] // 64
	px = base.load()
	return [(x, y) for y in range(8 * sc, 16 * sc) for x in range(8 * sc, 16 * sc) if px[x, y][3] and oklch(px[x, y])[0] < BLACK]


def recolor(base, pal, dark=(), black=()):
	"""base: RGBA creeper image (any size, 64:32 layout). pal: Counter of rgb -> count. Returns (image, mapping)."""
	w, h = base.size
	sc = w // 64
	px = base.load()
	body = Counter()
	for y in range(h):
		for x in range(w):
			c = px[x, y]
			if c[3] and not is_face(x // sc, y // sc, oklch(c)[0]):
				body[c[:3]] += 1
	bl = {c: oklch(c) for c in body}
	pl = {c: oklch(c) for c in pal}
	cl0 = percentile([(bl[c][0], n) for c, n in body.items()], 0.03)
	cl1 = percentile([(bl[c][0], n) for c, n in body.items()], 0.97)
	pl0 = percentile([(pl[c][0], n) for c, n in pal.items()], 0.03)
	pl1 = percentile([(pl[c][0], n) for c, n in pal.items()], 0.97)
	cc0 = min(v[1] for v in bl.values())
	cc1 = max(v[1] for v in bl.values())
	pc0 = min(v[1] for v in pl.values())
	pc1 = max(v[1] for v in pl.values()) or 1
	# Palette colours used: single-pixel highlights above the stretched range are left out.
	cand = [c for c in pal if pl[c][0] <= pl1 + 0.02] or list(pal)
	span = max(pl1 - pl0, 1e-6)

	# Distribution match: a creeper colour's lightness rank among the body's pixels picks the source colour at the same
	# rank among the source's pixels, so each source colour turns up about as often as it does in the source texture.
	body_sorted = sorted(((bl[c][0], n) for c, n in body.items()))
	body_total = sum(n for _, n in body_sorted)
	src_sorted = sorted(((pl[c][0], n, c) for c, n in pal.items()))
	src_total = sum(n for _, n, _ in src_sorted)

	def rank(L):
		below = sum(n for l, n in body_sorted if l < L)
		same = sum(n for l, n in body_sorted if l == L)
		return (below + same / 2) / body_total

	def pick(c):
		q = rank(oklch(c)[0])
		acc = 0
		for _, n, sc in src_sorted:
			acc += n
			if acc / src_total >= q:
				return sc
		return src_sorted[-1][2]

	out = Image.new("RGBA", base.size)
	op = out.load()
	cache = {}
	for y in range(h):
		for x in range(w):
			c = px[x, y]
			if c[3]:
				if c[:3] not in cache:
					cache[c[:3]] = pick(c[:3])
				op[x, y] = cache[c[:3]] + (c[3],)
	# Partly dark face pixels: ranked among themselves by lightness, spread over the source's colours in that order,
	# then blended toward black, so they keep vanilla's dark-to-less-dark variety instead of one flat colour.
	face_sorted = sorted(oklch(px[x, y][:3])[0] for x, y in dark)
	for x, y in dark:
		L = oklch(px[x, y][:3])[0]
		below = sum(1 for v in face_sorted if v < L)
		same = sum(1 for v in face_sorted if v == L)
		q = (below + same / 2) / len(face_sorted)
		acc = 0
		sc = src_sorted[-1][2]
		for _, n, cand_c in src_sorted:
			acc += n
			if acc / src_total >= q:
				sc = cand_c
				break
		op[x, y] = tuple(round(v * (1 - FACE_BLEND)) for v in sc) + (px[x, y][3],)
	for x, y in black:
		op[x, y] = px[x, y]
	return out, (cl0, cl1, pl0, pl1)


def contact_sheet(rows, path):
	"""rows: [(name, img16, img32)]; each shown as head front, body front and the whole texture."""
	cell = 192
	pad = 8
	cols = 4
	W = cols * (cell * 2 + pad * 3)
	H = math.ceil(len(rows) / cols) * (cell + 24 + pad) + pad
	sheet = Image.new("RGB", (W, H), (40, 44, 52))
	d = ImageDraw.Draw(sheet)
	for k, (name, a, b) in enumerate(rows):
		cx = (k % cols) * (cell * 2 + pad * 3) + pad
		cy = (k // cols) * (cell + 24 + pad) + pad
		for j, im in enumerate((a, b)):
			s = im.size[0] // 64
			head = im.crop((8 * s, 8 * s, 16 * s, 16 * s)).resize((cell // 2, cell // 2), Image.NEAREST)
			bodyf = im.crop((20 * s, 20 * s, 28 * s, 32 * s)).resize((cell // 2, int(cell * 0.75)), Image.NEAREST)
			whole = im.resize((cell, cell // 2), Image.NEAREST)
			ox = cx + j * (cell + pad)
			bg = Image.new("RGBA", (cell, cell), (90, 96, 110, 255))
			bg.alpha_composite(head.convert("RGBA"), (cell // 4, 0))
			bg.alpha_composite(bodyf.convert("RGBA"), (cell // 4, cell // 2 - 4))
			sheet.paste(bg.convert("RGB"), (ox, cy))
			sheet.paste(Image.alpha_composite(Image.new("RGBA", whole.size, (60, 64, 74, 255)), whole.convert("RGBA")).convert("RGB")
					.resize((cell // 2, cell // 4)), (ox + cell // 2, cy + cell - cell // 4))
		d.text((cx, cy + cell + 4), f"{name} (16x | 32x)", fill=(230, 230, 230))
	sheet.save(path)


def main():
	root = subprocess.check_output(["git", "rev-parse", "--show-toplevel"], text=True).strip()
	common = os.path.dirname(subprocess.check_output(["git", "rev-parse", "--path-format=absolute", "--git-common-dir"], text=True).strip())
	ap = argparse.ArgumentParser()
	ap.add_argument("--mcsrc", default=None, help="vanilla assets folder (build/mcsrc/<ver>/vanilla/assets/minecraft)")
	ap.add_argument("--sheet", default=None, help="write a contact sheet PNG here")
	ap.add_argument("--cf32j", default=CF32J_ZIP, help="Classic Faithful 32x Jappa zip or folder (default: the owner's Modrinth copy)")
	ap.add_argument("--base16", default=None, help="base creeper, 64x32 (default: vanilla's creeper.png)")
	ap.add_argument("--base32", default=None, help="base creeper, 128x64 (default: CF32J's creeper.png)")
	args = ap.parse_args()
	mcsrc = args.mcsrc
	if not mcsrc:
		props = open(os.path.join(root, "gradle.properties")).read()
		ver = [l.split("=", 1)[1].strip() for l in props.splitlines() if l.startswith("minecraft_version=")][0]
		for base in (root, common):
			p = os.path.join(base, "build", "mcsrc", ver, "vanilla", "assets", "minecraft")
			if os.path.isdir(p):
				mcsrc = p
				break
	if not mcsrc:
		sys.exit("no decompiled vanilla assets: run ./gradlew :refmod:genSources && python3 tools/mcsrc.py")
	if not os.path.exists(args.cf32j):
		sys.exit(f"no Classic Faithful 32x Jappa at {args.cf32j}: pass --cf32j <zip or folder>")
	van = Source(mcsrc, vanilla_assets=True)
	cf = Source(args.cf32j)
	colormap = van.open("colormap/grass.png")
	creeper = "entity/creeper/creeper.png"
	v16 = Image.open(args.base16).convert("RGBA") if args.base16 else van.open(creeper)
	v32 = Image.open(args.base32).convert("RGBA") if args.base32 else cf.open(creeper)
	if v16.size != (64, 32) or v32.size != (128, 64):
		sys.exit(f"base creepers must be 64x32 and 128x64, got {v16.size} and {v32.size}")
	dark16 = dark_face_pixels(v16, DARK16)
	dark32 = dark_face_pixels(v32, DARK32)
	black16 = black_face_pixels(v16)
	black32 = black_face_pixels(v32)
	print(f"dark face pixels 16x ({len(dark16)}): {dark16}")
	print(f"dark face pixels 32x ({len(dark32)}): {dark32}")
	os.makedirs(os.path.join(root, OUT16), exist_ok=True)
	os.makedirs(os.path.join(root, OUT32), exist_ok=True)
	rows = [("vanilla", v16, v32)]
	for name, (src, tint) in SKINS.items():
		rel = src.removeprefix("textures/")
		pal16 = palette(van, rel, tint, colormap)
		a, info = recolor(v16, pal16, dark16, black16)
		src32 = cf if cf.has(rel) else van
		pal32 = palette(src32, rel, tint, colormap)
		b, _ = recolor(v32, pal32, dark32, black32)
		a.save(os.path.join(root, OUT16, name + ".png"))
		b.save(os.path.join(root, OUT32, name + ".png"))
		rows.append((name, a, b))
		print(f"{name:10s} {rel:24s} 32x from {'CF32J' if src32 is cf else 'vanilla'}; creeper L {info[0]:.2f}-{info[1]:.2f} -> "
				f"palette L {info[2]:.2f}-{info[3]:.2f}")
	if args.sheet:
		contact_sheet(rows, args.sheet)


if __name__ == "__main__":
	main()
