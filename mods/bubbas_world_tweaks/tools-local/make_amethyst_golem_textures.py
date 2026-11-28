#!/usr/bin/env python3
# dte:C8,B16
"""Amethyst Golem entity texture and spawn egg, 16x (vanilla style) and 32x (Classic Faithful 32x Jappa style).

Palette: vanilla's amethyst cluster and bud crystal ramp (lilac to pink), all amethyst, plus deep geode purples for
the mouth and belly hole. The 32x is drawn at 32x from the same face plan, palette shades only, grain as two-pixel
clumps (specs/textures.md), not upscaled. The box list matches AmethystGolemModel.createBodyLayer().

Usage: python3 make_amethyst_golem_textures.py [--dark-eyes] [--preview DIR]
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
# Crystal ramp (amethyst cluster / buds)
CRY = [(100, 71, 158), (122, 91, 181), (141, 106, 204), (179, 142, 243), (207, 160, 243), (254, 203, 230),
	(255, 253, 213)]
RAMPS = {
	"ame": [(84, 57, 138)] + CRY[:5],
	"cry": [(84, 57, 138)] + CRY,
	"dark": [(30, 18, 52), (46, 26, 82), (70, 43, 125)],
}
BASE = {"ame": 4, "cry": 3, "dark": 1}
EYE = (27, 22, 44)
GLINT = (240, 245, 244)
EYE_WHITE = (240, 245, 244)
PUPIL = (46, 26, 82)
EYE_RIM = (100, 71, 158)
DARK_EYES = False

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
	"""Material grain: 16x per-pixel mottling, 32x two-pixel clumps one shade either way; no pink glints (they go beige in shade)."""
	base = BASE[mat] + bias
	if s == 1:
		n = pixelart.hash01(X, Y, seed + 11)
		m = pixelart.hash01(X // 2, Y // 2, seed + 5)
		off = 0
		if n < 0.14:
			off = -1
		elif n > 0.84:
			off = 1
		if m > 0.9 and mat == "ame" and off < 1:
			off += 1
		return shade(mat, base + off)
	n = pixelart.hash01(X // 2 + (Y % 2), Y, seed + 3)
	m = pixelart.hash01(X // 4, Y // 4, seed + 9)
	off = 0
	if n > 0.84:
		off = 1
	elif n < 0.15:
		off = -1
	if m > 0.85 and mat == "ame":
		off += 1
	return shade(mat, base + off)


def grid_at(rows16, rows32, s, x, y):
	rows = rows16 if s == 1 else rows32
	return rows[y][x]


# Face features: one character per pixel at 16x and at 32x (same layout, refined at 32x)
# a amethyst, A amethyst without dark grain (round the face), s amethyst one shade down, l one up, W eye white, P pupil, r eye rim, e dark eye, w dark-eye glint,
# k eye rim (dark eyes), d mouth/geode dark, D darkest, k/j geode rim, p crystal pink, q crystal lilac,
# K crystal light lilac, x budding pit, m mouth line, n mouth corner
FACE16 = [
	"llllllll",
	"AWPAAPWA",
	"AWPAAPWA",
	"AAAAAAAA",
]
FACE32 = [
	"llllllllllllllll",
	"aaaaaaaaaaaaaaaa",
	"aarWWraaaarWWraa",
	"arWWWWraarWWWWra",
	"arWWPPraarPPWWra",
	"arWWPPraarPPWWra",
	"aarWWraaaarWWraa",
	"aassssaaaassssaa",
]
FACE16_DARK = [
	"llllllll",
	"aweaawea",
	"aeeaaeea",
	"aaaaaaaa",
]
FACE32_DARK = [
	"llllllllllllllll",
	"llllllllllllllll",
	"aakeekaaaakeekaa",
	"aaeweeaaaaeweeaa",
	"aaeeeeaaaaeeeeaa",
	"aaeeeeaaaaeeeeaa",
	"aakeekaaaakeekaa",
	"aaaaaaaaaaaaaaaa",
]

# Smile on the jaw (drops open when singing); soft corners
JAW16 = [
	"cuuc",
	"AAAA",
]
JAW32 = [
	"naaaaaan",
	"anmmmmna",
	"aaaaaaaa",
	"aaaaaaaa",
]
CHEEK_R16 = ["AA", "AA"]
CHEEK_L16 = ["AA", "AA"]
CHEEK_R32 = ["aaaa", "aaaa", "aaaa", "aaaa"]
CHEEK_L32 = ["aaaa", "aaaa", "aaaa", "aaaa"]

SOUND16 = [
	"aaaaaaaa",
	"aaaaaaaa",
	"aaakkaaa",
	"aakDdkaa",
	"aakqpkaa",
	"aaakkaaa",
]


def sound32(x, y):
	"""The belly's geode hole at 32x: deep amethyst rim, dark inside with crystals growing up from the bottom."""
	if y < 2:
		return "a"
	dx, dy = x + 0.5 - 8.0, y + 0.5 - 7.6
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
	"aKaaaaaa",
	"axaaaaKa",
	"aaaaaaxa",
	"aaaKaaaa",
]
BACK32 = ["".join(ch * 2 for ch in row) for row in BACK16 for _ in (0, 1)]


def feature(ch):
	return {
		"e": EYE, "w": GLINT, "k": RAMPS["ame"][1], "j": RAMPS["ame"][2], "d": RAMPS["dark"][1], "D": RAMPS["dark"][0],
		"m": RAMPS["dark"][1], "n": RAMPS["dark"][2], "c": RAMPS["ame"][2], "u": RAMPS["ame"][1], "p": RAMPS["cry"][6], "q": RAMPS["cry"][4],
		"W": EYE_WHITE, "P": PUPIL, "r": EYE_RIM, "x": RAMPS["cry"][1], "K": RAMPS["cry"][5],
	}.get(ch)


def featured(rows16, rows32, s, seed=0):
	def fn(x, y, W, H, X, Y):
		ch = grid_at(rows16, rows32, s, x, y)
		if ch == "A":
			c = fill("ame", X, Y, s, 0, seed)
			return c if sum(c) >= sum(shade("ame", BASE["ame"])) else shade("ame", BASE["ame"])
		if ch in "asl":
			return fill("ame", X, Y, s, {"a": 0, "s": -1, "l": 1}[ch], seed)
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
		nxt = rows[r + 1] if r + 1 < len(rows) else None
		if nxt is not None and nxt != (mat, b) and y % S == S - 1:
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
	# budding-dark bed where the crystals grow out, a shaded ring round it (cx, cz in face texels)
	def fn(x, y, W, H, X, Y):
		fx, fy = (x + 0.5) / S, (y + 0.5) / S
		dx, dy = (fx - 4.0) / 3.3, (fy - 3.6) / 2.9
		r = (dx * dx + dy * dy) ** 0.5
		wob = (pixelart.hash01(int(fx * 2), int(fy * 2), 31) - 0.5) * 0.25
		if r < 0.45:
			return RAMPS["dark"][2] if pixelart.hash01(X // S, Y // S, 8) > 0.4 else RAMPS["ame"][1]
		if r < 0.8 + wob:
			return fill("ame", X, Y, S, -2, 2)
		return fill("ame", X, Y, S, 1, 2)
	return fn


# Whites: cream-white on up-facing faces (full light), cool near-white on shaded faces (cream goes khaki there,
# pink-white goes mauve); pink-white only as a mid-tone between white and lilac
CREAM = (255, 251, 236)
ICE = (250, 248, 255)
PINKW = CRY[5]


# In-game light on each crystal face (measured in client shots; the game's entity shading, 1.0 on top faces):
# white where it stays white, lilac where shading would turn white grey
CRYSTAL_LIGHT = {
	"center": {"front": 0.71, "back": 0.71, "left": 0.57, "right": 0.57},
	"left": {"front": 0.89, "right": 0.78, "back": 0.51, "left": 0.40},
	"right": {"back": 0.86, "left": 0.74, "front": 0.53, "right": 0.42},
	"back": {"front": 0.90, "left": 0.71, "right": 0.71, "back": 0.40},
}


def prism(crystal, face, tip=False, mid=False):
	"""Crystal shaft like the vanilla cluster: white point and top, white ridge up well-lit faces, pink-white mid-tone."""
	light = CRYSTAL_LIGHT[crystal].get(face, 0)
	bright, lit = light >= 0.78, light >= 0.68

	def fn(x, y, W, H, X, Y):
		if face == "top":
			return CREAM
		if face == "bottom":
			return shade("cry", 2)
		if tip:
			return ICE if lit else shade("cry", 4)
		t = y / max(1, H - 1)
		col, ncol = x // S, W // S
		if mid:
			if y < H // 2:
				return ICE if lit and (col == 0 or bright) else (PINKW if lit else shade("cry", 4))
			return shade("cry", 4 if col == 0 else 3)
		if col == 0:
			if lit and t <= (0.75 if bright else 0.5):
				return ICE
			if t <= (0.85 if bright else 0.6):
				return PINKW if lit else shade("cry", 4)
			return shade("cry", 3)
		if col == ncol - 1:
			return shade("cry", (4 if lit else 3) - (2 if t > 0.6 else (1 if t > 0.3 else 0)))
		if t <= (0.5 if bright else 0.3):
			return PINKW if lit else shade("cry", 4)
		return shade("cry", (4 if t <= 0.75 else 3))
	return fn


# Facet stamps: chunky white facets, a light lilac rim and a dark facet edge below. C brightest, W white,
# l light lilac, s shadow facet, . untouched. The 32x versions are drawn in two-pixel clumps (CF32J style).
STAMPS = {
	"big": (["CWl", "Wls", "ls."], [
		"CCWWll",
		"CCWWll",
		"WWWlss",
		"WWlls.",
		"llss..",
		"lss...",
	]),
	"small": (["WW", "ls"], [
		"CWWl",
		"WWWl",
		"llss",
		"lss.",
	]),
	"tall": (["Wl", "Ws", "s."], [
		"CWl.",
		"WWll",
		"WWls",
		"Wlss",
		"lss.",
		"ss..",
	]),
	"cap": (["CWW", "WWl", "Wls"], [
		"CCWWWW",
		"CCCWWW",
		"WWWWll",
		"WWWlll",
		"WWllss",
		"Wllss.",
	]),
}

# (box, face) -> [(stamp, x, y, mirrored)] in 16x face pixels; none on the head front so the eyes stay clear
FACETS = {
	("skull", "top"): [("big", 0, 0, False), ("big", 5, 0, True), ("small", 0, 5, False), ("small", 6, 5, True)],
	("skull", "back"): [("big", 0, 0, False), ("small", 6, 0, True)],
	("skull", "right"): [("small", 1, 0, False), ("small", 4, 2, False)],
	("skull", "left"): [("small", 4, 0, True), ("small", 1, 2, True)],
	("body", "front"): [("big", 0, 0, False), ("small", 6, 0, True), ("tall", 6, 3, True)],
	("body", "back"): [("big", 5, 0, True), ("small", 0, 0, False), ("small", 3, 2, False)],
	("body", "right"): [("small", 1, 0, False), ("small", 2, 3, False)],
	("body", "left"): [("small", 2, 0, True), ("small", 1, 3, True)],
	("arm_r", "top"): [("cap", 0, 0, False)],
	("arm_l", "top"): [("cap", 0, 0, True)],
	("arm_r", "front"): [("small", 0, 0, False), ("small", 1, 4, False)],
	("arm_l", "front"): [("small", 1, 0, True), ("small", 0, 4, True)],
	("arm_r", "right"): [("tall", 0, 0, False), ("small", 1, 4, False)],
	("arm_l", "left"): [("tall", 1, 0, True), ("small", 0, 4, True)],
	("arm_r", "back"): [("small", 1, 0, True)],
	("arm_l", "back"): [("small", 0, 0, False)],
	("leg_r", "front"): [("small", 0, 0, False)],
	("leg_l", "front"): [("small", 1, 0, True)],
	("leg_r", "right"): [("small", 1, 0, False)],
	("leg_l", "left"): [("small", 1, 0, True)],
	("cheek_r", "right"): [("small", 1, 0, False)],
	("cheek_l", "left"): [("small", 1, 0, True)],
}


# Side faces (half light in game): lilac-white, which keeps its hue where white would go grey
SIDE_WHITE = ((242, 230, 255), (228, 208, 255))


def facets(cv, rect, stamps, top, side):
	x0, y0, w, h = rect
	s = cv.s
	if side:
		pal = {"C": SIDE_WHITE[0], "W": SIDE_WHITE[1], "l": CRY[4], "s": RAMPS["ame"][2]}
	else:
		pal = {"C": (255, 255, 250) if top else (255, 255, 255), "W": CREAM if top else ICE, "l": CRY[4], "s": RAMPS["ame"][2]}
	for name, sx, sy, mirror in stamps:
		grid = STAMPS[name][0 if s == 1 else 1]
		for gy, row in enumerate(grid):
			if mirror:
				row = row[::-1]
			for gx, ch in enumerate(row):
				x, y = sx * s + gx, sy * s + gy
				if ch == "." or not (0 <= x < w * s and 0 <= y < h * s):
					continue
				cv.px[x0 * s + x, y0 * s + y] = pal[ch] + (255,)


def paint(s):
	global S
	S = s
	cv = Canvas(s)
	F = {name: faces(u, v, w, h, d) for name, u, v, w, h, d in BOXES}

	# Head
	sk = F["skull"]
	cv.face(sk["top"], top_skull())
	cv.face(sk["bottom"], dark_inside())
	face16, face32 = (FACE16_DARK, FACE32_DARK) if DARK_EYES else (FACE16, FACE32)
	cv.face(sk["front"], featured(face16, face32, s, 1))
	for side in ("right", "left", "back"):
		cv.face(sk[side], mat_face("ame", 0, False, 1))
	for name, inner in (("cheek_r", "left"), ("cheek_l", "right")):
		for side, rect in F[name].items():
			if side == inner or side == "top":
				cv.face(rect, dark_inside())
			elif side == "front":
				r16, r32 = (CHEEK_R16, CHEEK_R32) if name == "cheek_r" else (CHEEK_L16, CHEEK_L32)
				cv.face(rect, featured(r16, r32, s, 1))
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

	# Arms: amethyst, hands a shade deeper
	for name in ("arm_r", "arm_l"):
		rows = [("ame", 1)] + [("ame", 0)] * 4 + [("ame", -1), ("ame", -1)]
		for side, rect in F[name].items():
			if side == "top":
				cv.face(rect, mat_face("ame", 1, False, 5))
			elif side == "bottom":
				cv.face(rect, mat_face("ame", -2, False, 5))
			else:
				cv.face(rect, banded(rows, 5))

	# Legs: amethyst, feet a shade deeper
	for name in ("leg_r", "leg_l"):
		rows = [("ame", 0), ("ame", 0), ("ame", -1), ("ame", -1)]
		for side, rect in F[name].items():
			if side == "top":
				cv.face(rect, mat_face("ame", -1, False, 7))
			elif side == "bottom":
				cv.face(rect, mat_face("ame", -3, False, 7))
			else:
				cv.face(rect, banded(rows, 7))

	# Crystals
	for name in ("crystal_center", "mid_center", "crystal_left", "crystal_right", "crystal_back"):
		crystal = "center" if name == "mid_center" else name.split("_")[1]
		for side, rect in F[name].items():
			cv.face(rect, prism(crystal, side, mid=name == "mid_center"))
	for name in ("tip_center", "tip_left", "tip_right", "tip_back"):
		for side, rect in F[name].items():
			cv.face(rect, prism(name.split("_")[1], side, tip=True))

	for (name, side), stamps in FACETS.items():
		facets(cv, F[name][side], stamps, side == "top", side in ("left", "right"))
	return cv.img


# Spawn egg: amethyst egg, crystal cluster crown, golem face, amethyst feet
EGG_GRID = [
	"........c.......",
	".......hK.......",
	".......hKS......",
	"....c..tKS..c...",
	"....hq.qKS.hS...",
	".....tKqKSKS....",
	".....oSSSSSo....",
	"....ohhglllo....",
	"...ohLlllllao...",
	"...oLWPaaPWso...",
	"...olWPaaPWso...",
	"...olanaanaso...",
	"...oaaannasso...",
	"....oaaaasso....",
	"....bboooobb....",
	"....bb....bb....",
]
EGG_PAL = {
	"o": CRY[0], "L": CRY[3], "l": CRY[3], "a": CRY[3], "s": CRY[2],
	"t": CRY[5], "c": CREAM, "h": ICE, "g": CRY[5], "K": CRY[4], "q": CRY[3], "S": CRY[1],
	"W": EYE_WHITE, "P": PUPIL, "w": GLINT, "e": EYE, "m": (46, 26, 82), "n": (70, 43, 125), "b": CRY[1],
}
EGG_MAT = {"L": "ame", "l": "ame", "a": "ame", "s": "ame", "t": "cry", "c": "cry", "K": "cry", "q": "cry", "S": "cry"}


def egg(s):
	grid = EGG_GRID
	if DARK_EYES:
		grid = [r.replace("LWPaaPWs", "Lweaawes").replace("lWPaaPWs", "leeaaees") for r in EGG_GRID]
	if s == 1:
		return pixelart.render(grid, EGG_PAL)
	return pixelart.render32(grid, EGG_PAL, material=EGG_MAT, outline="o", cf=True, grain=("ame",), seed=4)


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
	args = sys.argv[1:]
	if "--dark-eyes" in args:
		DARK_EYES = True
		args.remove("--dark-eyes")
	if len(args) > 1 and args[0] == "--preview":
		preview(args[1])
	else:
		save(paint(1), RES, ENTITY)
		save(paint(2), PACK32, ENTITY)
		save(egg(1), RES, EGG)
		save(egg(2), PACK32, EGG)
