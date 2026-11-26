#!/usr/bin/env python3
# dte:B16
"""The Glider's textures: the item icon and the worn wings (agent-made, listed in docs/art-todo.md; derived from
vanilla's and Classic Faithful 32x Jappa's elytra, credited in CREDITS.md). Needs Pillow.

  python mods/bubbas_extra_recipes/tools-local/make_glider_textures.py [vanilla_textures_dir] [cf32j_zip]

Vanilla's elytra (16x) and CF32J's elytra (32x) recoloured onto the recipe's materials: every pixel keeps its place
and its shade, and each region of the elytra takes one material, the shade placed on that material's vanilla ramp by
OKLab lightness.
- Wings (64x32 sheet, ElytraModel's 10x20x2 box at 22,0): the leading side and the leading-edge column of the outer
  face are a bamboo spar ending in a diamond cap (the tip-end face too), the shoulder-end face is bamboo, the wingtip
  rows are feathers, the rest is phantom membrane.
- Icon: the dark outline and yoke are bamboo, the dark clasp between the wings is a diamond, the bottom tips are
  feathers, the inside is phantom membrane.
Without the CF32J zip the committed 32x textures are kept.
"""
import io
import os
import sys
import zipfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
RES = HERE.parent / "src/main/resources"
TEX16 = RES / "assets/bubbas_extra_recipes/textures"
TEX32 = RES / "resourcepacks/bubbas_extra_recipes_32x/assets/bubbas_extra_recipes/textures"
WINGS = "entity/equipment/wings"


def hx(s):
	return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


# Material ramps: vanilla's own colours (item/bamboo + block/bamboo_stalk, item/phantom_membrane, item/feather,
# item/diamond), dark to light.
BAMBOO = ["3d5a1f", "466d0c", "507516", "599003", "5e8a24", "609816", "79a842", "8dc24f"]
MEMBRANE = ["6f516c", "7e627b", "826d72", "8e7f7a", "9c8a83", "a99b87", "c3b9a1", "dcd9c0"]
FEATHER = ["969696", "bebebe", "d3d3d3", "ffffff"]
DIAMOND = ["11727a", "1c919a", "1aaaa7", "20c5b5", "4aedd9", "a1fbe8"]


# --- OKLab -----------------------------------------------------------------------------------------------------

def _lin(c):
	c /= 255.0
	return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def _gam(c):
	c = c * 12.92 if c <= 0.0031308 else 1.055 * c ** (1 / 2.4) - 0.055
	return max(0, min(255, round(c * 255)))


def oklab(rgb):
	r, g, b = (_lin(v) for v in rgb[:3])
	l = (0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b) ** (1 / 3)
	m = (0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b) ** (1 / 3)
	s = (0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b) ** (1 / 3)
	return (0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s, 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
			0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s)


def from_oklab(lab):
	L, a, b = lab
	l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3
	m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3
	s = (L - 0.0894841775 * a - 1.2914855480 * b) ** 3
	return (_gam(4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s), _gam(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s),
			_gam(-0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s))


def on_ramp(t, ramp):
	"""The colour at position t (0 darkest .. 1 lightest) along a ramp, by OKLab lightness between its two
	neighbouring colours."""
	labs = sorted((oklab(hx(c)) for c in ramp), key=lambda p: p[0])
	L = labs[0][0] + max(0.0, min(1.0, t)) * (labs[-1][0] - labs[0][0])
	for lo, hi in zip(labs, labs[1:]):
		if lo[0] <= L <= hi[0]:
			k = 0.0 if hi[0] == lo[0] else (L - lo[0]) / (hi[0] - lo[0])
			return from_oklab(tuple(lo[i] + (hi[i] - lo[i]) * k for i in range(3)))
	return from_oklab(labs[-1])


def recolour(img, material_at, span=None):
	"""Each opaque pixel onto the ramp material_at(x, y) names, by its lightness within the range of the pixels
	given that material (span: (lo, hi) of t to use on that ramp per material, default the whole ramp)."""
	img = img.convert("RGBA")
	out = img.copy()
	px, src = out.load(), img.load()
	cells = [(x, y, material_at(x, y), oklab(src[x, y])[0]) for y in range(img.height) for x in range(img.width)
		if src[x, y][3]]
	bounds = {}
	for _, _, ramp, L in cells:
		lo, hi = bounds.get(id(ramp), (L, L))
		bounds[id(ramp)] = (min(lo, L), max(hi, L))
	for x, y, ramp, L in cells:
		lo, hi = bounds[id(ramp)]
		t = 0.5 if hi == lo else (L - lo) / (hi - lo)
		a, b = (span or {}).get(id(ramp), (0.0, 1.0))
		px[x, y] = on_ramp(a + t * (b - a), ramp) + (src[x, y][3],)
	return out


# --- Wings -----------------------------------------------------------------------------------------------------

def wing_materials(img):
	s = img.width // 64
	a = img.load()
	opaque = lambda x, y: 0 <= x < img.width and 0 <= y < img.height and a[x, y][3] > 0
	face_rows = [y for y in range(2 * s, 22 * s) if any(opaque(x, y) for x in range(36 * s, 46 * s))]
	last = max(face_rows)

	def material(x, y):
		u, v = x / s, y / s
		if v < 2:
			return BAMBOO if u < 34 else DIAMOND
		if u < 24:
			return MEMBRANE
		if u < 36:
			return DIAMOND if y > last - s else BAMBOO
		first = next(i for i in range(36 * s, 46 * s) if opaque(i, y))
		if x < first + s:
			return DIAMOND if y > last - 2 * s else BAMBOO
		if v >= 20:
			return FEATHER
		return MEMBRANE
	return material


# --- Icon ------------------------------------------------------------------------------------------------------

def icon_materials(img):
	s = img.width // 16
	a = img.load()
	rows = [y for y in range(img.height) if any(a[x, y][3] for x in range(img.width))]
	top, bottom = min(rows), max(rows)
	darkest = min((p for p in img.getdata() if p[3]), key=sum)
	outline = sorted({p for p in img.getdata() if p[3]}, key=sum)[:2]

	def material(x, y):
		p = a[x, y]
		mid = abs(x + 0.5 - img.width / 2) <= 2 * s
		if p == darkest and mid and y < top + s:
			return DIAMOND
		if p in outline:
			return BAMBOO
		if y > bottom - 2 * s - 1:
			return FEATHER
		return MEMBRANE
	return material


SPAN = {id(DIAMOND): (0.35, 1.0), id(FEATHER): (0.2, 1.0), id(BAMBOO): (0.05, 0.7)}
WING_SPAN = {**SPAN, id(MEMBRANE): (0.3, 1.0)}


def make(item, wings):
	return (recolour(item, icon_materials(item.convert("RGBA")), SPAN),
		recolour(wings, wing_materials(wings.convert("RGBA")), WING_SPAN))


def vanilla_dir(arg):
	if arg:
		return Path(arg)
	for base in (ROOT, ROOT.parents[2] if ROOT.parent.name == "worktrees" else ROOT):
		hits = sorted((base / "build/mcsrc").glob("*/vanilla/assets/minecraft/textures"))
		if hits:
			return hits[-1]
	sys.exit("vanilla textures not found; pass the path")


def cf32j_zip(arg):
	cands = [Path(arg)] if arg else []
	if os.environ.get("APPDATA"):
		cands.append(Path(os.environ["APPDATA"]) / "ModrinthApp/profiles/porp/resourcepacks/CF32J-2026r21.zip")
	return next((c for c in cands if c.is_file()), None)


def main():
	args = sys.argv[1:] + [None, None]
	v = vanilla_dir(args[0])
	sets = [(TEX16, Image.open(v / "item/elytra.png"), Image.open(v / WINGS / "elytra.png"))]
	z = cf32j_zip(args[1])
	if z:
		with zipfile.ZipFile(z) as zf:
			read = lambda e: Image.open(io.BytesIO(zf.read("assets/minecraft/textures/" + e)))
			sets.append((TEX32, read("item/elytra.png"), read(WINGS + "/elytra.png")))
	else:
		print("CF32J not found: kept the committed 32x textures")
	for base, item, wings in sets:
		icon, sheet = make(item, wings)
		(base / "item").mkdir(parents=True, exist_ok=True)
		(base / WINGS).mkdir(parents=True, exist_ok=True)
		icon.save(base / "item/glider.png")
		sheet.save(base / WINGS / "glider.png")
		print("wrote", base)


if __name__ == "__main__":
	main()
