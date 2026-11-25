# dte:C1,B16
"""Soulfire art (specs/soulfire.md): Soul Blaze Rod, Soul Blaze Powder, Soul Fire Charge, the Soulguard effect icon.
16x textures are vanilla's blaze rod, blaze powder and fire charge recoloured to the soul ramp, and a hand-placed
effect icon; 32x twins in Classic Faithful 32x Jappa style (tools/pixelart.py, cf mode).

Usage: python3 make_soulfire_textures.py [vanilla_textures_dir]
(default: build/mcsrc/<version>/vanilla/assets/minecraft/textures of the main checkout). Needs Pillow.
"""
import math
import sys
from pathlib import Path

import os
import io
import zipfile
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import make_captain_textures as mc  # noqa: E402
import pixelart as pa  # noqa: E402  (tools/, on the path via make_captain_textures)


# --- OKLab / OKLCh ------------------------------------------------------------------------------------------------

def _s2l(c):
	c /= 255
	return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def _l2s(c):
	c = 12.92 * c if c <= 0.0031308 else 1.055 * max(c, 0) ** (1 / 2.4) - 0.055
	return max(0, min(255, round(c * 255)))


def lch(rgb):
	r, g, b = (_s2l(v) for v in rgb[:3])
	l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b
	m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b
	s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b
	l, m, s = (math.copysign(abs(v) ** (1 / 3), v) for v in (l, m, s))
	L = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
	a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
	bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
	return L, math.hypot(a, bb), math.degrees(math.atan2(bb, a)) % 360


def from_lch(L, C, h):
	"""The colour, chroma reduced until it fits sRGB."""
	while True:
		a, b = C * math.cos(math.radians(h)), C * math.sin(math.radians(h))
		l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3
		m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3
		s = (L - 0.0894841775 * a - 1.2914855480 * b) ** 3
		v = (4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
			-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
			-0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s)
		if C <= 0 or all(-1e-4 <= x <= 1 + 1e-4 for x in v):
			return tuple(_l2s(x) for x in v)
		C -= 0.002


# --- soul recolour -------------------------------------------------------------------------------------------------

def soul(c, chroma=1.0, lift=0.0):
	"""A blaze colour on the soul ramp (the rule behind make_captain_textures.BLAZE_TO_SOUL): same OKLCh lightness
	(plus lift), hue from the soul lantern's blue-violet (h 267) in the darks to soul fire cyan (h 198) from mid-light up."""
	L, C, _ = lch(c)
	L = min(0.97, L + lift)
	t = min(1.0, max(0.0, (L - 0.22) / (0.76 - 0.22)))
	return from_lch(L, C * chroma, 267 - 69 * t)


def cool(c, dark=0.82, chroma=0.022, hue=275):
	"""A warm rock colour as a cool, dark blackstone-like one (fire charge body)."""
	L, C, _ = lch(c)
	return from_lch(L * dark, min(C, chroma), hue)


def recolour(img, fn):
	out = img.convert("RGBA").copy()
	px = out.load()
	memo = {}
	for y in range(out.height):
		for x in range(out.width):
			p = px[x, y]
			if p[3]:
				if p[:3] not in memo:
					memo[p[:3]] = fn(p[:3])
				px[x, y] = memo[p[:3]] + (p[3],)
	return out


def soul_flame(c):
	"""Soulguard icon: vanilla Fire Resistance with its flame on the soul ramp; the shield kept."""
	L, C, h = lch(c)
	return soul(c) if C > 0.03 and 15 < h < 120 else c


LOCK = [  # the padlock: s shackle, o outline, b body, h highlight, k keyhole
	"..ooo..",
	".os.so.",
	".o...o.",
	"ooooooo",
	"ohhhhho",
	"obbkbbo",
	"obbkbbo",
	"ooooooo",
]
LOCK_COLOURS = {"s": (150, 150, 158), "o": (52, 52, 60), "b": (128, 128, 136), "h": (196, 196, 204), "k": (1, 167, 172)}


def soulguard_flame_lock(v):
	"""Soulguard icon: vanilla Fire Resistance's flame alone (no shield) on the soul ramp, doubled to fill the icon, with
	a small padlock in front of its base."""
	src = Image.open(v / "mob_effect/fire_resistance.png").convert("RGBA")
	flame = Image.new("RGBA", src.size, (0, 0, 0, 0))
	for y in range(src.height):
		for x in range(src.width):
			p = src.getpixel((x, y))
			if p[3]:
				L, C, h = lch(p[:3])
				if C > 0.03 and 15 < h < 120:
					flame.putpixel((x, y), soul(p[:3]) + (255,))
	flame = flame.crop(flame.getbbox())
	flame = flame.resize((flame.width * 2, flame.height * 2), Image.NEAREST)
	icon = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
	icon.alpha_composite(flame, ((18 - flame.width) // 2, (18 - flame.height) // 2))
	for y, row in enumerate(LOCK):
		for x, ch in enumerate(row):
			if ch != ".":
				icon.putpixel((11 + x, 10 + y), LOCK_COLOURS[ch] + (255,))
	return icon


# Classic Faithful 32x Jappa: the zip (default the owner's Modrinth profile, or CF32J=<zip or unpacked folder>)
CF32J_ZIP = Path(os.environ.get("APPDATA", "")) / "ModrinthApp/profiles/porp/resourcepacks/CF32J-2026r21.zip"


def cf32j_open(member):
	"""A CF32J texture as an image, from the zip or an unpacked folder; None when the pack isn't here."""
	src = Path(os.environ.get("CF32J", CF32J_ZIP))
	if src.is_dir() and (src / member).exists():
		return Image.open(src / member).convert("RGBA")
	if src.is_file():
		with zipfile.ZipFile(src) as zf:
			if member in zf.namelist():
				return Image.open(io.BytesIO(zf.read(member))).convert("RGBA")
	return None


def flame_only(src):
	"""Just the flame of a Fire Resistance icon, on the soul ramp, cropped."""
	flame = Image.new("RGBA", src.size, (0, 0, 0, 0))
	for y in range(src.height):
		for x in range(src.width):
			p = src.getpixel((x, y))
			if p[3]:
				L, C, h = lch(p[:3])
				if C > 0.03 and 15 < h < 120:
					flame.putpixel((x, y), soul(p[:3]) + (p[3],))
	return flame.crop(flame.getbbox())


def soulguard_flame_lock_32(cf):
	"""32x Soulguard: CF32J's Fire Resistance flame alone on the soul ramp, doubled like the 16x, with the padlock doubled."""
	flame = flame_only(cf)
	flame = flame.resize((flame.width * 2, flame.height * 2), Image.NEAREST)
	icon = Image.new("RGBA", (36, 36), (0, 0, 0, 0))
	icon.alpha_composite(flame, ((36 - flame.width) // 2, (36 - flame.height) // 2))
	for y, row in enumerate(LOCK):
		for x, ch in enumerate(row):
			if ch != ".":
				for dy in range(2):
					for dx in range(2):
						icon.putpixel((22 + 2 * x + dx, 20 + 2 * y + dy), LOCK_COLOURS[ch] + (255,))
	return icon


LOCK_32 = [  # the padlock at 32x detail: s shackle, o outline, h highlight, b body, d shade, k keyhole
	"....oooooo....",
	"...osssssso...",
	"..oss....sso..",
	"..os......so..",
	"..os......so..",
	"..os......so..",
	"oooooooooooooo",
	"ohhhhhhhhhhhho",
	"ohbbbbbbbbbbdo",
	"obbbbkkkkbbbdo",
	"obbbbkkkkbbbdo",
	"obbbbbkkbbbbdo",
	"obbbbbkkbbbbdo",
	"obbbbbbbbbbbdo",
	"oddddddddddddo",
	"oooooooooooooo",
]
LOCK_32_COLOURS = dict(LOCK_COLOURS, d=(100, 100, 108))


SOULGUARD_FLAME_32 = [  # the 32x flame, hand-drawn after CF32J's Fire Resistance flame: a darkest .. f white core
	"..............de....................",
	".............dee....................",
	"............deeee...................",
	"............deeee...................",
	"...........deeeeee..................",
	"...........deeeeee..................",
	"...........deeeeeee.................",
	"...........deeeeeee.................",
	"...........deeeeeeee................",
	".dd.........deeeeeee................",
	".ded........deeeeeeee...............",
	".deed........deeeeeee...............",
	"..deed.......deeeeeeec..........cb..",
	"...deed.......deeeeeec.........cdb..",
	"...deeed......deeeeeggc........cddb.",
	"....deed......deeeegffc.......cddb..",
	"....deed......deeeegfffc......cddb..",
	"....deed......deeegffffc.....cdddb..",
	"...dgged.....deeeegffffgc....cddb...",
	"...dffged....deeeegfffffc...cdddb...",
	"..dffgeed...deeeeegfffffgc.cdddb....",
	"..dfffgeed.deeeeegffffffgeecdddb....",
	"..dfffgeddddeeeeegffffffgeeeecb.....",
	"..dffffgeddeeeeegffffffgeeeeecb.....",
	"..dgffffgeeeeeegfffffffgeeeecb......",
	"...dgffffgeeeegfffffffgeeeeecb......",
	"...degffffggegffffffffgeeeecb.......",
	"....cegfffffgffffffffgeeeecb........",
	".....cegggggegffffffgeeeecb.........",
	"......ceeeeeeegffffgeeeecb..........",
	".......bceeeeeeggggeeedcb...........",
	"........bceeeeeeeeeeedcb............",
	".........abcddddddddcba.............",
	"...........abccccccbaa..............",
	".............aaaaaaa................",
	"....................................",
]
SOULGUARD_FLAME_32_COLOURS = {"a": (6, 96, 122), "b": (8, 139, 168), "c": (0, 189, 200), "d": (2, 201, 210), "e": (9, 246, 252),
	"g": (140, 251, 255), "f": (224, 254, 255)}


def soulguard_icon_cf_32():
	"""32x Soulguard: the hand-drawn flame and the padlock drawn at 32x."""
	icon = Image.new("RGBA", (36, 36), (0, 0, 0, 0))
	for art, colours, ox, oy in ((SOULGUARD_FLAME_32, SOULGUARD_FLAME_32_COLOURS, 0, 0), (LOCK_32, LOCK_32_COLOURS, 22, 20)):
		for y, row in enumerate(art):
			for x, ch in enumerate(row):
				if ch != ".":
					icon.putpixel((ox + x, oy + y), colours[ch] + (255,))
	return icon


def flame_only_icon(cf):
	"""The 16x icon's flame alone, placed as in the icon."""
	flame = flame_only(cf)
	icon = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
	icon.alpha_composite(flame, ((18 - flame.width) // 2, (18 - flame.height) // 2))
	return icon


def soulguard_icon_cf(cf):
	"""Soulguard icon (16x; the 32x is it doubled): CF32J's Fire Resistance flame alone, at its own pixel size so the
	flame fills the icon with pixels the size of the padlock's, on the soul ramp, with the padlock in front."""
	flame = flame_only(cf)
	icon = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
	icon.alpha_composite(flame, ((18 - flame.width) // 2, (18 - flame.height) // 2))
	for y, row in enumerate(LOCK):
		for x, ch in enumerate(row):
			if ch != ".":
				icon.putpixel((11 + x, 10 + y), LOCK_COLOURS[ch] + (255,))
	return icon


def charge_colour(c):
	"""Fire charge: orange cracks (and the warm glow around them) to soul, the rock body to dark blackstone."""
	L, C, h = lch(c)
	if C > 0.07 and 30 < h < 110:  # orange / amber cracks and glow
		return soul(c, lift=0.08)
	return cool(c, dark=0.74)


# --- Soulguard effect icon (18x18 and 36x36, drawn from shapes) ----------------------------------------------------

# A three-tongued soul flame behind a small iron shield with a soul-blue ward line. Shapes are in 18-unit icon
# coordinates and drawn at either size, so the 32x twin keeps one-pixel outlines (Jappa style) instead of doubled ones.
FLAME_RAMP = ["0f1934", "00305b", "015b7f", "06a3b9", "11cad4", "5dfaff", "c8feff"]  # outline .. white-hot
SHIELD_RAMP = {"a": "17161d", "b": "4a4954", "c": "6c6b77", "d": "93929d", "e": "bdbcc5"}
WARD = ["11cad4", "5dfaff", "c8feff"]

# (tip x, tip y, base x, base y, half width at base)
TONGUES = [(9.8, 0.4, 8.5, 11.0, 4.4), (1.8, 5.0, 5.0, 12.0, 3.0), (15.6, 3.6, 12.2, 12.0, 3.0)]
BODY = (8.5, 11.6, 7.6, 4.5)  # flame foot ellipse: centre x, y, radius x, y
CORE = (8.5, 12.6)
SHIELD_TOP, SHIELD_SIDE, SHIELD_TIP = 8.0, 12.0, 16.4
SHIELD_X0, SHIELD_X1 = 4.0, 13.0


def _in_tongue(x, y, tx, ty, bx, by, hw):
	if y < ty or y > by + hw:
		return False
	if y > by:
		return (x - bx) ** 2 + (y - by) ** 2 <= hw * hw
	t = (y - ty) / (by - ty)
	cx = tx + (bx - tx) * t ** 0.6
	return abs(x - cx) <= hw * math.sin(t * math.pi / 2) ** 0.9


def _in_flame(x, y):
	cx, cy, rx, ry = BODY
	if y <= cy + ry and ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1:
		return True
	return any(_in_tongue(x, y, *t) for t in TONGUES)


def _shield_half(y):
	"""Half width of the shield at y (None above or below it)."""
	if y < SHIELD_TOP or y > SHIELD_TIP:
		return None
	half = (SHIELD_X1 - SHIELD_X0) / 2
	if y <= SHIELD_SIDE:
		return half
	t = (y - SHIELD_SIDE) / (SHIELD_TIP - SHIELD_SIDE)
	return half * (1 - t ** 1.6)


def soulguard_icon(k):
	"""The icon at k pixels per icon unit (1: 18x18, 2: 36x36)."""
	n = 18 * k
	sx = (SHIELD_X0 + SHIELD_X1) / 2
	shield = [[False] * n for _ in range(n)]
	flame = [[False] * n for _ in range(n)]
	for py in range(n):
		for px in range(n):
			x, y = (px + 0.5) / k, (py + 0.5) / k
			h = _shield_half(y)
			shield[py][px] = h is not None and abs(x - sx) <= h
			flame[py][px] = _in_flame(x, y)

	def get(g, px, py):
		return 0 <= px < n and 0 <= py < n and g[py][px]

	img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
	out = img.load()
	fr = [mc.hx(c) for c in FLAME_RAMP]
	for py in range(n):
		for px in range(n):
			x, y = (px + 0.5) / k, (py + 0.5) / k
			if shield[py][px]:
				edge = any(not get(shield, px + dx, py + dy) for dx in (-1, 0, 1) for dy in (-1, 0, 1))
				if edge:
					ch = "a"
				elif not get(shield, px, py - 1 - k) or y < SHIELD_TOP + 1.6:
					ch = "e"
				elif x < sx - (_shield_half(y) or 0) + 1.6:
					ch = "d"
				elif x > sx + (_shield_half(y) or 0) - 1.6 or y > SHIELD_SIDE + 2.2:
					ch = "b"
				else:
					ch = "c"
				col = mc.hx(SHIELD_RAMP[ch])
				# ward line down the middle of the face
				if ch == "c" and abs(x - sx) < 0.5 + 0.25 * (k - 1) and SHIELD_TOP + 2.0 < y < SHIELD_SIDE + 1.6:
					t = (y - SHIELD_TOP - 2.0) / (SHIELD_SIDE - SHIELD_TOP - 0.4)
					col = mc.hx(WARD[2 if t < 0.3 else 1 if t < 0.65 else 0])
				out[px, py] = col + (255,)
				continue
			if not flame[py][px]:
				continue
			edge = any(not get(flame, px + dx, py + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
			if edge:
				out[px, py] = fr[0] + (255,)
				continue
			# heat: close to the core and deep inside the flame
			depth = next((r for r in range(1, n) if not all(get(flame, px + dx * r, py + dy * r)
				for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))), n)
			dc = math.hypot((x - CORE[0]) / 6.0, (y - CORE[1]) / 12.0)
			heat = 0.65 * min(1.0, (depth - 1) / (3.0 * k)) + 0.35 * (1 - min(1.0, dc))
			lvl = 2 + min(4, int(heat * 5.0))
			out[px, py] = fr[lvl] + (255,)
	return img


# --- main ------------------------------------------------------------------------------------------------------------

def save_pair(img16, rel, outline_lum=48, material=None):
	mc.save(img16, rel)
	grid, pal = mc.grid_of(img16)
	dark = "".join(k for k, c in pal.items() if mc.lum(c) < outline_lum)
	mat = material(pal) if material else None
	mc.save32(pa.render32(grid, pal, mat, outline=dark, bevel=1, cf=True), rel)


def main():
	v = mc.vanilla_dir()

	rod = recolour(Image.open(v / "item/blaze_rod.png"), soul)
	save_pair(rod, "item/soul_blaze_rod.png", outline_lum=0, material=lambda pal: {k: "rod" for k in pal})

	powder = recolour(Image.open(v / "item/blaze_powder.png"), soul)
	save_pair(powder, "item/soul_blaze_powder.png", outline_lum=0, material=lambda pal: {k: "dust" for k in pal})

	charge = recolour(Image.open(v / "item/fire_charge.png"), charge_colour)
	save_pair(charge, "item/soul_fire_charge.png", outline_lum=0,
		material=lambda pal: {k: ("crack" if lch(c)[1] > 0.05 else "rock") for k, c in pal.items()})

	cf = cf32j_open("assets/minecraft/textures/mob_effect/fire_resistance.png")
	if cf is None:
		print("Classic Faithful 32x Jappa not found: kept the committed Soulguard icons")
	else:
		icon = soulguard_icon_cf(cf)
		mc.save(icon, "mob_effect/soulguard.png")
		mc.save32(soulguard_icon_cf_32(), "mob_effect/soulguard.png")


if __name__ == "__main__":
	main()
