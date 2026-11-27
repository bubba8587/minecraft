#!/usr/bin/env python3
# dte:C8,B16
"""Amethyst Golem entity texture and spawn egg, 16x (vanilla style) and 32x (Classic Faithful 32x Jappa style).

Palette: vanilla's geode blocks (amethyst block, amethyst cluster, calcite, smooth basalt). The 32x is drawn at 32x
from the same face plan, palette shades only, grain as two-pixel clumps (specs/textures.md), not upscaled.
The box list matches AmethystGolemModel.createBodyLayer().

Usage: python3 make_amethyst_golem_textures.py [--preview DIR]
"""
import sys
from pathlib import Path
from PIL import Image

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
sys.path.insert(0, str(REPO / "tools"))
import pixelart  # noqa: E402

RES = HERE.parent / "src" / "main" / "resources"
PACK32 = RES / "resourcepacks" / "bubbas_world_tweaks_32x"
ENTITY = Path("assets/bubbas_world_tweaks/textures/entity/amethyst_golem/amethyst_golem.png")
EGG = Path("assets/bubbas_world_tweaks/textures/item/amethyst_golem_spawn_egg.png")

# Material ramps, darkest to lightest (vanilla block colours)
RAMPS = {
	"ame": [(70, 43, 125), (93, 58, 154), (100, 71, 158), (122, 91, 181), (141, 106, 204), (166, 120, 241),
			(200, 144, 240), (254, 203, 230)],
	"cry": [(84, 57, 138), (100, 71, 158), (122, 91, 181), (141, 106, 204), (179, 142, 243), (207, 160, 243),
			(254, 203, 230), (255, 253, 213)],
	"cal": [(179, 179, 179), (201, 201, 196), (217, 219, 215), (237, 236, 230), (240, 245, 244), (255, 255, 255)],
	"bas": [(27, 38, 50), (50, 51, 61), (58, 59, 72), (79, 75, 79), (92, 92, 92), (105, 105, 105)],
	"dark": [(30, 18, 52), (46, 26, 82), (70, 43, 125)],
}
BASE = {"ame": 4, "cry": 3, "cal": 3, "bas": 3, "dark": 1}
EYE = (27, 22, 44)
GLINT = (240, 245, 244)

# (name, u, v, w, h, d) per cube, as in the model
BOXES = [
	("skull", 0, 0, 8, 4, 7),
	("cheek_r", 48, 0, 2, 2, 4),
	("cheek_l", 26, 11, 2, 2, 4),
	("throat", 32, 6, 8, 2, 3),
	("jaw", 32, 0, 4, 2, 4),
	("body", 0, 11, 8, 6, 5),
	("arm_r", 0, 22, 3, 7, 3),
	("arm_l", 12, 22, 3, 7, 3),
	("leg_r", 24, 22, 3, 4, 4),
	("leg_l", 38, 22, 3, 4, 4),
	("crystal_center", 0, 32, 3, 5, 3),
	("mid_center", 12, 32, 2, 2, 2),
	("tip_center", 12, 36, 1, 1, 1),
	("crystal_left", 20, 32, 2, 4, 2),
	("tip_left", 16, 36, 1, 2, 1),
	("crystal_right", 28, 32, 2, 4, 2),
	("crystal_back", 36, 32, 2, 4, 2),
	("tip_right", 44, 32, 1, 1, 1),
	("tip_back", 48, 32, 1, 1, 1),
]


def faces(u, v, w, h, d):
	"""Face rects (x, y, w, h) in 16x texels. right = -x side, left = +x side, front = -z."""
	return {
		"top": (u + d, v, w, d),
		"bottom": (u + d + w, v, w, d),
		"right": (u, v + d, d, h),
		"front": (u + d, v + d, w, h),
		"left": (u + d + w, v + d, d, h),
		"back": (u + 2 * d + w, v + d, w, h),
	}


class Canvas:
	def __init__(self, s):
		self.s = s
		self.img = Image.new("RGBA", (64 * s, 64 * s), (0, 0, 0, 0))
		self.px = self.img.load()

	def face(self, rect, fn):
		"""fn(x, y, W, H, X, Y) -> colour, x/y local and X/Y absolute output pixels."""
		x0, y0, w, h = (c * self.s for c in rect)
		for y in range(h):
			for x in range(w):
				c = fn(x, y, w, h, x0 + x, y0 + y)
				if c is not None:
					self.px[x0 + x, y0 + y] = tuple(c) + (255,)


def shade(mat, level):
	r = RAMPS[mat]
	return r[max(0, min(len(r) - 1, level))]


def fill(mat, X, Y, s, bias=0, seed=0):
	"""Material grain: 16x per-pixel mottling like the vanilla block, 32x two-pixel clumps one shade either way."""
	base = BASE[mat] + bias
	if s == 1:
		n = pixelart.hash01(X, Y, seed + 11)
		m = pixelart.hash01(X // 2, Y // 2, seed + 5)
		off = 0
		if n < 0.2:
			off = -1
		elif n > 0.78:
			off = 1
		if m > 0.86 and mat in ("ame", "cal"):
			off += 1
		if mat == "ame" and n > 0.975:
			off = 4
		return shade(mat, base + off)
	n = pixelart.hash01(X // 2 + (Y % 2), Y, seed + 3)
	m = pixelart.hash01(X // 4, Y // 4, seed + 9)
	off = 0
	if n > 0.84:
		off = 1
	elif n < 0.15:
		off = -1
	if m > 0.85 and mat in ("ame", "cal"):
		off += 1
	if mat == "ame" and pixelart.hash01(X // 2, Y // 2, seed + 21) > 0.975:
		off = 4
	return shade(mat, base + off)


def grid_at(rows16, rows32, s, x, y):
	rows = rows16 if s == 1 else rows32
	return rows[y][x]


# Face features: one character per pixel at 16x and at 32x (same layout, refined at 32x)
# a amethyst, s amethyst one shade down, l one up, e eye, w eye glint, k eye rim, d mouth/geode dark, D darkest,
# c calcite, C calcite shadow, k/j geode rim, p crystal pink, q crystal lilac, m mouth line
FACE16 = [
	"llllllll",
	"aweaawea",
	"aeeaaeea",
	"aaaaaaaa",
]
FACE32 = [
	"llllllllllllllll",
	"llllllllllllllll",
	"aakeekaaaakeekaa",
	"aaeweeaaaaeweeaa",
	"aaeeeeaaaaeeeeaa",
	"aaeeeeaaaaeeeeaa",
	"aakeekaaaakeekaa",
	"aaaaaaaaaaaaaaaa",
]

JAW16 = [
	"maam",
	"amma",
]
JAW32 = [
	"maaaaaam",
	"amaaaama",
	"aammmmaa",
	"ssssssss",
]

SOUND16 = [
	"ssssssss",
	"aaakkaaa",
	"aakDdkaa",
	"aakqpkaa",
	"aaakkaaa",
	"aaaaaaaa",
]


def sound32(x, y):
	"""The belly's geode hole at 32x: calcite rim, dark inside with crystals growing up from the bottom."""
	if y < 2:
		return "s"
	dx, dy = x + 0.5 - 8.0, y + 0.5 - 6.0
	r = (dx * dx + dy * dy) ** 0.5
	if r > 3.9:
		return "a"
	if r > 2.4:
		return "k" if dx + dy < -1.0 else "j"
	if dy > 0.6 and abs(dx) < 1.6:
		return "p" if (x, y) == (8, 7) else "q"
	if dx + dy < 0:
		return "D"
	return "d"


SOUND32 = ["".join(sound32(x, y) for x in range(16)) for y in range(12)]


# Budding-amethyst pits on the back
BACK16 = [
	"ssssssss",
	"aaaaaaaa",
	"aqaaaaaa",
	"ajaaaaqa",
	"aaaaaaja",
	"aaaqaaaa",
]
BACK32 = ["".join(ch * 2 for ch in row) for row in BACK16 for _ in (0, 1)]


def feature(ch):
	return {
		"e": EYE, "w": GLINT, "k": RAMPS["ame"][1], "j": RAMPS["ame"][2], "d": RAMPS["dark"][1], "D": RAMPS["dark"][0],
		"m": RAMPS["dark"][1], "p": RAMPS["cry"][6], "q": RAMPS["cry"][4],
	}.get(ch)


def featured(rows16, rows32, s, seed=0):
	def fn(x, y, W, H, X, Y):
		ch = grid_at(rows16, rows32, s, x, y)
		if ch in "asl":
			return fill("ame", X, Y, s, {"a": 0, "s": -1, "l": 1}[ch], seed)
		if ch in "cC":
			return shade("cal", 2 if ch == "c" else 0)
		return feature(ch)
	return fn


S = 1  # current scale while painting (set per canvas)


def mat_face(mat, bias=0, bottom_dark=True, seed=0):
	def fn(x, y, W, H, X, Y):
		b = bias
		if bottom_dark and y >= H - S:
			b -= 1
		return fill(mat, X, Y, S, b, seed)
	return fn


def banded(rows, seed=0):
	"""rows: list of (material, bias) per 16x row, top to bottom; the last S rows of each band step one darker."""
	def fn(x, y, W, H, X, Y):
		r = y // S
		mat, b = rows[r]
		nxt = rows[r + 1][0] if r + 1 < len(rows) else None
		if mat == "bas" and r > 0 and rows[r - 1][0] != "bas" and y % S == 0:
			b += 2
		if nxt != mat and y % S == S - 1:
			b -= 1
		return fill(mat, X, Y, S, b, seed)
	return fn


def dark_inside():
	def fn(x, y, W, H, X, Y):
		n = pixelart.hash01(X // S, Y // S, 4)
		return RAMPS["dark"][0] if n < 0.35 else RAMPS["dark"][1]
	return fn


def jaw_top():
	# inside of the lower jaw: dark, a lip one shade lighter along the front edge
	def fn(x, y, W, H, X, Y):
		return RAMPS["dark"][2] if y >= H - S else RAMPS["dark"][1]
	return fn


def top_skull():
	# calcite ring round the crystal bases, budding-dark where they grow out (cx, cz in face texels)
	def fn(x, y, W, H, X, Y):
		fx, fy = (x + 0.5) / S, (y + 0.5) / S
		dx, dy = (fx - 4.0) / 3.3, (fy - 3.6) / 2.9
		r = (dx * dx + dy * dy) ** 0.5
		wob = (pixelart.hash01(int(fx * 2), int(fy * 2), 31) - 0.5) * 0.25
		if r < 0.45:
			return RAMPS["dark"][2] if pixelart.hash01(X // S, Y // S, 8) > 0.4 else RAMPS["ame"][1]
		if r < 1.0 + wob:
			b = 0 if r > 0.62 else -1
			return fill("cal", X, Y, S, b, 2)
		return fill("ame", X, Y, S, 1, 2)
	return fn


def prism(face, tip=False):
	"""Crystal shaft: lit left edge, darker right edge, lighter toward the point."""
	lv = {"front": 4, "left": 3, "right": 4, "back": 3, "top": 5, "bottom": 1}[face]

	def fn(x, y, W, H, X, Y):
		if face in ("top", "bottom"):
			return shade("cry", lv + (1 if tip else 0))
		level = lv + (1 if tip else 0)
		if not tip:
			t = y / max(1, H - 1)
			level += 1 if t < 0.3 else (-1 if t > 0.75 else 0)
		if x < S and W > S:
			level += 1
		elif x >= W - S and W > 2 * S:
			level -= 1
		return shade("cry", level)
	return fn


def paint(s):
	global S
	S = s
	cv = Canvas(s)
	F = {name: faces(u, v, w, h, d) for name, u, v, w, h, d in BOXES}

	# Head
	sk = F["skull"]
	cv.face(sk["top"], top_skull())
	cv.face(sk["bottom"], dark_inside())
	cv.face(sk["front"], featured(FACE16, FACE32, s, 1))
	for side in ("right", "left", "back"):
		cv.face(sk[side], mat_face("ame", 0, False, 1))
	for name, inner in (("cheek_r", "left"), ("cheek_l", "right")):
		for side, rect in F[name].items():
			if side == inner or side == "top":
				cv.face(rect, dark_inside())
			else:
				cv.face(rect, mat_face("ame", -1 if side == "bottom" else 0, side != "bottom", 1))
	for side, rect in F["throat"].items():
		if side in ("front", "top"):
			cv.face(rect, dark_inside())
		else:
			cv.face(rect, mat_face("ame", -1 if side == "bottom" else 0, side != "bottom", 1))
	for side, rect in F["jaw"].items():
		if side == "front":
			cv.face(rect, featured(JAW16, JAW32, s, 1))
		elif side in ("top", "back"):
			cv.face(rect, dark_inside() if side == "back" else jaw_top())
		else:
			cv.face(rect, mat_face("ame", -1, True, 1))

	# Body: geode sound hole on the belly, shadow under the head
	bd = F["body"]
	cv.face(bd["front"], featured(SOUND16, SOUND32, s, 3))
	cv.face(bd["back"], featured(BACK16, BACK32, s, 3))
	for side in ("right", "left"):
		rows = [("ame", -1)] + [("ame", 0)] * 5
		cv.face(bd[side], banded(rows, 3))
	cv.face(bd["top"], mat_face("ame", -1, False, 3))
	cv.face(bd["bottom"], mat_face("ame", -2, False, 3))

	# Arms: amethyst with basalt hands
	for name in ("arm_r", "arm_l"):
		rows = [("ame", 1)] + [("ame", 0)] * 4 + [("bas", 0), ("bas", -1)]
		for side, rect in F[name].items():
			if side == "top":
				cv.face(rect, mat_face("ame", 1, False, 5))
			elif side == "bottom":
				cv.face(rect, mat_face("bas", -1, False, 5))
			else:
				cv.face(rect, banded(rows, 5))

	# Legs: basalt feet
	for name in ("leg_r", "leg_l"):
		rows = [("ame", -1), ("ame", -1), ("bas", 0), ("bas", -1)]
		for side, rect in F[name].items():
			if side == "top":
				cv.face(rect, mat_face("ame", -1, False, 7))
			elif side == "bottom":
				cv.face(rect, mat_face("bas", -2, False, 7))
			else:
				cv.face(rect, banded(rows, 7))

	# Crystals
	for name in ("crystal_center", "mid_center", "crystal_left", "crystal_right", "crystal_back"):
		for side, rect in F[name].items():
			cv.face(rect, prism(side))
	for name in ("tip_center", "tip_left", "tip_right", "tip_back"):
		for side, rect in F[name].items():
			cv.face(rect, prism(side, tip=True))
	return cv.img


# Spawn egg: amethyst egg, crystal cluster crown, golem face, calcite-ringed geode belly, basalt feet
EGG_GRID = [
	"........t.......",
	".......tK.......",
	".......KKS......",
	"....t..qKS..t...",
	"....Kq.qKS.KS...",
	".....qKqKSKS....",
	".....oqqKSSo....",
	"....oLllllao....",
	"...oLllllllao...",
	"...oLweaaweso...",
	"...oleeaaeeso...",
	"...olaammaaso...",
	"...oaaaaaasso...",
	"....oaaaasso....",
	"....bboooobb....",
	"....bb....bb....",
]
EGG_PAL = {
	"o": (70, 43, 125), "L": (200, 144, 240), "l": (166, 120, 241), "a": (141, 106, 204), "s": (122, 91, 181),
	"t": (254, 203, 230), "K": (207, 160, 243), "q": (179, 142, 243), "S": (141, 106, 204),
	"w": GLINT, "e": EYE, "m": (46, 26, 82), "b": (50, 51, 61),
}
EGG_MAT = {"L": "ame", "l": "ame", "a": "ame", "s": "ame", "t": "cry", "K": "cry", "q": "cry", "S": "cry"}


def egg(s):
	if s == 1:
		return pixelart.render(EGG_GRID, EGG_PAL)
	return pixelart.render32(EGG_GRID, EGG_PAL, material=EGG_MAT, outline="o", cf=True, grain=("ame",), seed=4)


def save(img, root, rel):
	out = root / rel
	out.parent.mkdir(parents=True, exist_ok=True)
	img.save(out)
	print(out)


def preview(dirpath):
	d = Path(dirpath)
	d.mkdir(parents=True, exist_ok=True)
	for s in (1, 2):
		paint(s).resize((512, 512), Image.NEAREST).save(d / f"golem-uv-{16 * s}.png")
		egg(s).resize((256, 256), Image.NEAREST).save(d / f"egg-{16 * s}.png")


if __name__ == "__main__":
	if len(sys.argv) > 2 and sys.argv[1] == "--preview":
		preview(sys.argv[2])
	else:
		save(paint(1), RES, ENTITY)
		save(paint(2), PACK32, ENTITY)
		save(egg(1), RES, EGG)
		save(egg(2), PACK32, EGG)
