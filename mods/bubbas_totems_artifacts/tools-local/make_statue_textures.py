#!/usr/bin/env python3
# dte:C6,B16
"""Totem statues. The Totem of Warding (hand-placed, main) and, since 2026-11-22, every other totem (a generic figure in
each item palette, write_others, with the blockstate and loot). Block models and textures, 16x and the Classic Faithful 32x Jappa-style 32x twins (pixelart.py cf mode) (agent-made, listed
in docs/art-todo.md for the owner to redo or tweak; never AI-generated).

  python mods/bubbas_totems_artifacts/tools-local/make_statue_textures.py

The statue is the Totem of Warding item (tools/warding-textures.py) cast life-size in silver and set on a polished
deepslate plinth: a robed figure with stub arms held out, a deep hood with two soul-fire eyes in its shadow, a ward
rune glowing on its chest, and a soul flame burning from the crown of the hood. Eyes, rune and flame are small elements
with light_emission, so they glow at night like the item's. Fits 10x10 and 16 high (TotemStatueBlock.SHAPE).

Writes models/block/totem_statue_warding.json and, at 16x and 32x:
  textures/block/totem_statue_plinth.png  plinth and step (layout: PLINTH below)
  textures/block/totem_statue_warding.png the figure (layout: FIGURE below)
Texture grids are hand-placed at 16x; the 32x twins come from tools/pixelart.py. Needs Pillow.
"""
import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent.parent
sys.path.insert(0, str(ROOT / "tools"))
from pixelart import check, render, render32  # noqa: E402

RES = HERE.parent / "src/main/resources"
MOD = "bubbas_totems_artifacts"

# Polished deepslate, vanilla's own ramp (block/polished_deepslate.png).
DEEP = {"0": (36, 36, 36), "1": (45, 45, 45), "2": (56, 55, 55), "3": (65, 65, 65), "4": (75, 76, 79),
	"5": (88, 88, 88), "6": (110, 110, 110)}
# Silver, the item's ramp (tools/warding-textures.py), on letters here: o (darkest) .. u (lightest).
SOUL = {"o": (30, 32, 42), "p": (58, 62, 76), "q": (96, 102, 118), "r": (138, 144, 158), "s": (176, 182, 194),
	"t": (210, 214, 224), "u": (240, 242, 248)}
# Soul fire, vanilla's soul fire ramp.
FIRE = {"a": (1, 128, 133), "b": (10, 206, 212), "c": (106, 247, 252), "d": (207, 253, 255)}
PALETTE = {**DEEP, **SOUL, **FIRE}
MATERIAL = {**{k: "deep" for k in DEEP}, **{k: "soul" for k in SOUL}, **{k: "fire" for k in FIRE}}

# Plinth texture. Regions (u0,v0)-(u1,v1) in 16x pixels:
#   plinth side  (0,0)-(10,3)   chamfered top edge, a band of carved ward notches, shadowed foot
#   plinth top   (0,3)-(10,13)  bevelled rim around a sunken square; the step's top reuses the inner (1,4)-(9,12)
#   step side    (0,13)-(8,14)
# The rest is spare and filled with plain polished deepslate (it shows as block-breaking particles).
PLINTH = check([
	"6655555554" + "455554",
	"4240424042" + "544455",
	"2111111110" + "455545",
	"6666666664" + "545554",
	"6" + "66555554" + "3" + "455455",
	"5" + "65333342" + "3" + "554545",
	"5" + "63444432" + "3" + "545554",
	"5" + "63455432" + "3" + "455455",
	"5" + "63455432" + "3" + "554545",
	"5" + "63444432" + "3" + "545554",
	"5" + "64333322" + "3" + "455455",
	"5" + "42222221" + "2" + "554545",
	"4222222221" + "545554",
	"54444443" + "45455455",
	"5455454545455455",
	"4545545454545545",
])

# Figure texture. Regions:
#   body front  (0,0)-(6,5)    robe under the hood's shadow, the rune element in front
#   body side   (6,0)-(10,5)   robe side with a fold
#   body back   (10,0)-(16,5)  robe back, the hood's drape
#   hood side   (0,6)-(6,10)   top row: the hood's crown band; rows 7-9: the hood's cloth (also its back)
#   hood front  (6,6)-(12,10)  the rim around the face: band on top, a column each side
#   face        (12,6)-(16,9)  the dark recess inside the hood
#   eye         (12,9)-(13,10) and rune (13,9)-(15,11): glowing parts
#   arm end     (6,10)-(8,12), arm side (8,10)-(10,12)
#   hood top    (0,10)-(6,16)  with the ember opening the flame rises from
#   flame       (10,12)-(14,16) the soul flame, on two crossed planes
FIGURE = check([
	"qqqqqp" + "qqqp" + "rrrqqp",
	"srrrrq" + "srqq" + "srrqqp",
	"srqqrq" + "srqp" + "srqrqp",
	"srqqrq" + "rrqp" + "srqrqp",
	"rrqrqp" + "rqqp" + "rqqqpp",
	"qqqqqqqqqqqqqqqq",
	"uuttss" + "uuttss" + "oooo",
	"tttssr" + "tqqqqr" + "oppo",
	"tssrrq" + "sqqqqq" + "pppp",
	"srrqqp" + "sqqqqp" + "c" + "cd" + "q",
	"uuttts" + "ts" + "ts" + "qqq" + "bc" + "q",
	"utoosr" + "sr" + "rq" + "qqqqqq",
	"tobcor" + "qqqq" + "..a." + "qq",
	"tocbor" + "qqqq" + ".bc." + "qq",
	"sroorq" + "qqqq" + "bcdb" + "qq",
	"srrqqq" + "qqqq" + "cddc" + "qq",
])


def face(uv, tex, **extra):
	return {"uv": uv, "texture": tex, **extra}


def box(frm, to, faces, **extra):
	return {"from": frm, "to": to, **extra, "faces": faces}


def model():
	P, F = "#plinth", "#figure"
	side4 = lambda uv: {d: face(uv, P) for d in ("north", "east", "south", "west")}
	el = []
	# Plinth and step.
	el.append(box([3, 0, 3], [13, 3, 13], {**side4([0, 0, 10, 3]), "up": face([0, 3, 10, 13], P),
		"down": face([0, 3, 10, 13], P, cullface="down")}))
	el.append(box([4, 3, 4], [12, 4, 12], {**side4([0, 13, 8, 14]), "up": face([1, 4, 9, 12], P)}))
	# Body (robe).
	el.append(box([5, 4, 6], [11, 9, 10], {"north": face([0, 0, 6, 5], F), "south": face([10, 0, 16, 5], F),
		"east": face([6, 0, 10, 5], F), "west": face([6, 0, 10, 5], F), "up": face([12, 6, 16, 9], F)}))
	# Arms held out to the sides.
	for x0, x1, end in ((3, 5, "west"), (11, 13, "east")):
		faces = {d: face([8, 10, 10, 12], F) for d in ("north", "south", "up", "down")}
		faces[end] = face([6, 10, 8, 12], F)
		el.append(box([x0, 6, 7], [x1, 8, 9], faces))
	# The hood: a crown band on top of two side walls, open at the front over a recessed face.
	el.append(box([5, 12, 5], [11, 13, 11], {"north": face([6, 6, 12, 7], F), "south": face([0, 6, 6, 7], F),
		"east": face([0, 6, 6, 7], F), "west": face([0, 6, 6, 7], F), "up": face([0, 10, 6, 16], F),
		"down": face([12, 6, 16, 7], F)}))
	for x0, x1, front, inner_side in ((5, 6, [11, 7, 12, 10], "east"), (10, 11, [6, 7, 7, 10], "west")):
		outer = "west" if inner_side == "east" else "east"
		el.append(box([x0, 9, 5], [x1, 12, 11], {"north": face(front, F),
			"south": face([5, 7, 6, 10] if x0 == 5 else [0, 7, 1, 10], F),
			outer: face([0, 7, 6, 10], F), inner_side: face([12, 6, 13, 9], F)}))
	el.append(box([6, 9, 6.5], [10, 12, 11], {"north": face([12, 6, 16, 9], F), "south": face([1, 7, 5, 10], F)}))
	# Soul-fire eyes in the hood's shadow, the rune on the chest, the flame on the crown: they glow.
	for x in (6.5, 8.5):
		el.append(box([x, 10, 6.25], [x + 1, 11, 6.5], {"north": face([12, 9, 13, 10], F)}, light_emission=15))
	rune = face([13, 9, 15, 11], F)
	# The rune is a small square turned 45 degrees: a diamond, like the item's.
	el.append(box([7, 5.5, 5], [9, 7.5, 6], {"north": rune, "east": face([13, 9, 14, 11], F),
		"west": face([13, 9, 14, 11], F), "up": face([13, 9, 15, 10], F), "down": face([13, 10, 15, 11], F)},
		rotation={"origin": [8, 6.5, 5.5], "axis": "z", "angle": 45}, light_emission=12))
	# The flame stands on the hood's crown; its tip reaches a pixel above the block, like a torch flame.
	flame = face([10, 12, 14, 16], F)
	el.append(box([6, 13, 8], [10, 17, 8], {"north": flame, "south": flame},
		rotation={"origin": [8, 13, 8], "axis": "y", "angle": 45}, light_emission=15))
	el.append(box([8, 13, 6], [8, 17, 10], {"east": flame, "west": flame},
		rotation={"origin": [8, 13, 8], "axis": "y", "angle": 45}, light_emission=15))
	return {
		"parent": "minecraft:block/block",
		"textures": {"particle": f"{MOD}:block/totem_statue_plinth", "plinth": f"{MOD}:block/totem_statue_plinth",
			"figure": f"{MOD}:block/totem_statue_warding"},
		"elements": el,
	}


def write_model(path):
	"""Pretty JSON with each face on one line, tab-indented like the rest of the repo's models."""
	text = json.dumps(model(), indent="\t")
	path.write_text(text + "\n", encoding="utf-8")


def main():
	fig = FIGURE
	for s, base in ((1, RES / f"assets/{MOD}"), (2, RES / f"resourcepacks/{MOD}_32x/assets/{MOD}")):
		out = base / "textures/block"
		out.mkdir(parents=True, exist_ok=True)
		if s == 1:
			render(PLINTH, PALETTE).save(out / "totem_statue_plinth.png")
			render(fig, PALETTE).save(out / "totem_statue_warding.png")
		else:
			render32(PLINTH, PALETTE, MATERIAL, smooth=False, bevel=0, grain=("deep",), seed=3, cf=True).save(out / "totem_statue_plinth.png")
			render32(fig, PALETTE, MATERIAL, smooth=False, bevel=0, grain=("soul",), seed=5, cf=True, grain_p=0.05).save(out / "totem_statue_warding.png")
		print("wrote", out)
	write_model(RES / f"assets/{MOD}/models/block/totem_statue_warding.json")
	print("wrote model")


# --- the other totems' statues (owner, 2026-11-22: "generate initial statue for all totems") --------------------------
# Each is its item (tools/totems-textures.py) carved as a figure on the same plinth: the item's material and colours,
# its head (eyes, a helm, a crown, a cap...), its chest emblem, and its arms held out. One generic figure, a topper
# per totem; glowing parts (eyes, a visor) are small elements with light_emission, like Warding's.
import importlib.util
import random

_spec = importlib.util.spec_from_file_location("totems_textures", ROOT / "tools" / "totems-textures.py")
TT = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(TT)

# Figure texture regions (u0, v0, u1, v1) in 16x pixels.
R_BODY_FRONT, R_BODY_SIDE, R_BODY_BACK = (0, 0, 6, 5), (6, 0, 10, 5), (10, 0, 16, 5)
R_HEAD_FRONT, R_HEAD_SIDE = (0, 5, 6, 9), (6, 5, 12, 9)
R_ARM_END, R_ARM_SIDE, R_EMBLEM = (12, 5, 14, 7), (14, 5, 16, 7), (12, 7, 15, 10)
R_HEAD_BACK, R_HEAD_TOP, R_TOPPER = (0, 9, 6, 13), (6, 9, 12, 15), (12, 10, 16, 16)
EYE_ROW = 6  # v of the eyes, in the head front


def _shade_grid(seed, speckle=True):
	"""A 16x16 grid of the material's shades '2'..'6', lit from the top left, with a little grain."""
	rnd = random.Random(seed)
	grid = [["4"] * 16 for _ in range(16)]
	for region in (R_BODY_FRONT, R_BODY_SIDE, R_BODY_BACK, R_HEAD_FRONT, R_HEAD_SIDE, R_ARM_END, R_ARM_SIDE, R_HEAD_BACK, R_HEAD_TOP,
			(0, 13, 6, 16), (15, 7, 16, 10)):
		u0, v0, u1, v1 = region
		for v in range(v0, v1):
			for u in range(u0, u1):
				level = 4
				if v == v0:
					level += 1
				if u == u0:
					level += 1
				if u == u1 - 1 or v == v1 - 1:
					level -= 1
				grain = rnd.choice((0, 0, 0, 1, -1))
				if speckle and region != R_HEAD_FRONT:  # the face stays clean so its features read
					level += grain
				grid[v][u] = str(max(2, min(6, level)))
	return grid


def _put(grid, region, rows):
	u0, v0 = region[0], region[1]
	for dv, row in enumerate(rows):
		for du, ch in enumerate(row):
			if ch != " ":
				grid[v0 + dv][u0 + du] = ch


def _eyes(grid, ch):
	grid[EYE_ROW][1] = ch
	grid[EYE_ROW][4] = ch


def _statues(speckle=True):
	"""name -> (palette, materials, grain, grid, topper rows (4 x 6) or None, glowing parts)."""
	defs = {}
	# Keeping: purple obsidian, gold eyes, a gold-bound chest held to the chest.
	g = _shade_grid(21, speckle); _eyes(g, "X"); _put(g, R_EMBLEM, ["abb", "bdb", "aaa"])
	defs["keeping"] = (TT.KEEPING_PAL, TT.KEEPING_MAT, ("stone",), g, None, [])
	# Gathering: copper with patina, patina eyes, a patina ring (a pull); hands raised to gather.
	g = _shade_grid(22, speckle); _eyes(g, "X"); _put(g, R_EMBLEM, ["qpq", "p4p", "qpq"])
	defs["gathering"] = (TT.GATHERING_PAL, TT.GATHERING_MAT, ("metal",), g, None, [])
	# Fortification: iron, a flat-topped helm with a glowing visor slit, an obsidian shield on the chest.
	g = _shade_grid(23, speckle)
	for u in range(1, 5):
		g[EYE_ROW][u] = "f" if u in (1, 4) else "g"
	_put(g, R_EMBLEM, ["tut", "sts", "tst"])
	defs["fortification"] = (TT.FORT_PAL, TT.FORT_MAT, ("metal",), g, ["6655", "4433", "4433", "5554", "5444", "4433"], ["visor"])
	# Growth: oak wood, green eyes, a sprout on the chest, a leafy crown with a pink blossom.
	g = _shade_grid(24, speckle); _eyes(g, "X"); _put(g, R_EMBLEM, ["n4n", "4m4", "4l4"])
	defs["growth"] = (TT.GROWTH_PAL, TT.GROWTH_MAT, ("wood", "leaf"), g, ["mnmn", "lmml", "lkyl", "mnmm", "nmnm", "mmnm"], [])
	# Seeking: lapis, gold eyes, a compass dial on the chest (needle to the north), a gold-rimmed hood band.
	g = _shade_grid(25, speckle); _eyes(g, "c"); _put(g, R_EMBLEM, ["bRb", "bwb", "bbb"])
	defs["seeking"] = (dict(TT.SEEK_PAL, R=TT.NEEDLE_RED), dict(TT.SEEK_MAT, R="dial"), ("stone",), g,
		["cbba", "bbaa", "cbba", "bbaa", "cbba", "bbaa"], [])
	# The Beginning: weathered stone, purple ender eyes (glowing), strata on the chest, a cap of grass.
	g = _shade_grid(26, speckle); _eyes(g, "X"); _put(g, R_EMBLEM, ["HhH", "dDd", "343"])
	defs["beginning"] = (TT.BEGIN_PAL, TT.BEGIN_MAT, ("stone", "grass"), g, ["HHhH", "hghh", "hhhh", "dDdD", "DdDd", "dddD"], ["eyes"])
	# The End: end stone, an enderman's black face with magenta eyes (glowing), an ender pearl, a purpur crown.
	g = _shade_grid(27, speckle)
	_put(g, R_HEAD_FRONT, ["kkkkkk", "kMkkMk", "kkkkkk", "kkkkkk"])
	_put(g, R_EMBLEM, ["tTt", "TwT", "tTt"])
	defs["end"] = (TT.END_PAL, TT.END_MAT, ("stone",), g, ["qqqq", "pppp", "pppp", "pppp", "pqqp", "pppp"], ["eyes"])
	# Herding: spruce, a woolly ram's head (wool cap, curled horns either side), a gold bell on the chest.
	g = _shade_grid(28, speckle); _eyes(g, "X"); _put(g, R_EMBLEM, ["bBb", "BcB", "aba"])
	defs["herding"] = (TT.HERD_PAL, TT.HERD_MAT, ("wood", "wool"), g, ["WWWW", "WwWW", "WWwW", "HHHH", "hhhh", "HHHH"], [])
	# Clear Skies: pale calcite, blue eyes, blue sky with the sun on the chest, a gold sun crown of rays.
	g = _shade_grid(29, speckle); _eyes(g, "X"); _put(g, R_EMBLEM, ["sSs", "SYS", "sSs"])
	defs["clear_skies"] = (TT.SKY_PAL, TT.SKY_MAT, ("stone",), g, ["YYYY", "YYYY", "yyyy", "yyyy", "yYYy", "yyyy"], [])
	# North Wind: packed ice, deep blue eyes, puffed cheeks and a blowing mouth, a snowflake on the chest, an icicle crown
	# swept back by the wind.
	g = _shade_grid(30, speckle); _eyes(g, "X"); _put(g, R_EMBLEM, ["bFb", "FfF", "bFb"])
	g[EYE_ROW + 1][0] = g[EYE_ROW + 1][1] = g[EYE_ROW + 1][4] = g[EYE_ROW + 1][5] = "6"; g[EYE_ROW + 2][2] = "m"; g[EYE_ROW + 2][3] = "m"
	defs["north_wind"] = (TT.NORTH_PAL, TT.NORTH_MAT, ("ice",), g, ["IIII", "IIII", "iiii", "iiii", "iIIi", "iiii"], [])
	belts = {"keeping": "XZ", "gathering": "XZ", "fortification": "ut", "growth": "XZ", "seeking": "cb", "beginning": "XZ",
		"end": "XZ", "herding": "XZ", "clear_skies": "Yy", "north_wind": "XZ"}
	for name, (pal, mat, grain, grid, topper, glow) in defs.items():
		if topper:
			_put(grid, R_TOPPER, topper)
		_belt(grid, belts[name])
	return defs


def _belt(grid, gem):
	"""The bottom row of the robe is a dark belt, its gem at the front."""
	for region in (R_BODY_FRONT, R_BODY_SIDE, R_BODY_BACK):
		u0, v0, u1, v1 = region
		for u in range(u0, u1):
			grid[v1 - 1][u] = "1"
	grid[R_BODY_FRONT[3] - 1][2] = gem[0]
	grid[R_BODY_FRONT[3] - 1][3] = gem[1]


def _uv(region):
	return [region[0], region[1], region[2], region[3]]


def statue_model(name, glow):
	P, F = "#plinth", "#figure"
	side4 = lambda uv: {d: face(uv, P) for d in ("north", "east", "south", "west")}
	sides = ("north", "east", "south", "west")
	el = []
	el.append(box([3, 0, 3], [13, 3, 13], {**side4([0, 0, 10, 3]), "up": face([0, 3, 10, 13], P),
		"down": face([0, 3, 10, 13], P, cullface="down")}))
	el.append(box([4, 3, 4], [12, 4, 12], {**side4([0, 13, 8, 14]), "up": face([1, 4, 9, 12], P)}))
	# Body.
	el.append(box([5, 4, 6], [11, 9, 10], {"north": face(_uv(R_BODY_FRONT), F), "south": face(_uv(R_BODY_BACK), F),
		"east": face(_uv(R_BODY_SIDE), F), "west": face(_uv(R_BODY_SIDE), F), "up": face([0, 13, 6, 16], F)}))
	# Arms held out (Gathering: hands raised to gather).
	for x0, x1, end in ((3, 5, "west"), (11, 13, "east")):
		faces = {d: face(_uv(R_ARM_SIDE), F) for d in ("north", "south", "up", "down")}
		faces[end] = face(_uv(R_ARM_END), F)
		el.append(box([x0, 6, 7], [x1, 8, 9], faces))
		if name == "gathering":
			hand = {d: face(_uv(R_ARM_SIDE), F) for d in ("north", "south", "east", "west")}
			hand["up"] = face(_uv(R_ARM_END), F)
			el.append(box([x0, 8, 7], [x1, 10, 9], hand))
	# Head (the face looks north, like Warding's hood).
	el.append(box([5, 9, 5], [11, 13, 11], {"north": face(_uv(R_HEAD_FRONT), F), "south": face(_uv(R_HEAD_BACK), F),
		"east": face(_uv(R_HEAD_SIDE), F), "west": face(_uv(R_HEAD_SIDE), F), "up": face(_uv(R_HEAD_TOP), F),
		"down": face(_uv(R_HEAD_TOP), F)}))
	# Chest emblem, a pixel proud of the robe (Keeping's chest two: a small box hugged to the chest).
	depth = 4.0 if name == "keeping" else 5.0
	edge = face([12, 7, 12 + (6 - depth), 10], F)
	el.append(box([6.5, 5.5, depth], [9.5, 8.5, 6], {"north": face(_uv(R_EMBLEM), F), "east": edge, "west": edge,
		"up": face([12, 7, 15, 8], F), "down": face([12, 9, 15, 10], F)}))
	# Toppers. Topper texture: rows 10-12 the sides of bands, spikes and rays (lit on top), rows 13-15 the top of the head.
	def tf(w, h):
		return face([12, 10, 12 + min(w, 4), 10 + min(h, 3)], F)
	ts = face([12, 13, 16, 16], F)

	def ring(h):
		return {d: tf(4, h) for d in sides}

	def post(x, z, w, d, y0, h, **extra):
		return box([x, y0, z], [x + w, y0 + h, z + d], {"north": tf(w, h), "south": tf(w, h), "east": tf(d, h), "west": tf(d, h),
			"up": tf(w, d)}, **extra)
	if name == "fortification":  # a flat-topped helm brim
		el.append(box([4.5, 12, 4.5], [11.5, 13.5, 11.5], {**ring(1.5), "up": ts, "down": ts}))
	elif name == "growth":  # a leafy crown, a blossom on top
		el.append(box([4.5, 13, 4.5], [11.5, 15, 11.5], {**ring(2), "up": ts}))
		el.append(box([7, 15, 7], [9, 16, 9], {d: face([13, 12, 14, 13], F) for d in (*sides, "up")}))
	elif name == "seeking":  # a gold-rimmed hood band
		el.append(box([4.5, 11.5, 4.5], [11.5, 13.5, 11.5], {**ring(2), "up": ts}))
	elif name == "beginning":  # a cap of grass
		el.append(box([4.5, 13, 4.5], [11.5, 14.5, 11.5], {**ring(1.5), "up": face([12, 10, 16, 12], F)}))
	elif name == "end":  # a purpur crown: a band and four corner spikes
		el.append(box([5, 13, 5], [11, 14, 11], {**ring(1), "up": ts}))
		for x, z in ((5, 5), (10, 5), (5, 10), (10, 10)):
			el.append(post(x, z, 1, 1, 14, 2))
	elif name == "herding":  # a wool cap and a curled ram's horn either side of the head
		el.append(box([5, 13, 5], [11, 14, 11], {**ring(1), "up": face([12, 10, 16, 13], F)}))
		for x0 in (3, 11):
			el.append(box([x0, 10, 6], [x0 + 2, 13, 9], {"north": face([12, 13, 14, 16], F), "south": face([12, 13, 14, 16], F),
				"east": face([12, 13, 15, 16], F), "west": face([12, 13, 15, 16], F), "up": face([12, 13, 14, 16], F),
				"down": face([12, 13, 14, 16], F)}))
			curl = {d: face([12, 13, 14, 14], F) for d in sides}
			el.append(box([x0, 9, 5], [x0 + 2, 10, 7], {**curl, "up": face([12, 13, 14, 15], F), "down": face([12, 13, 14, 15], F)}))
	elif name == "clear_skies":  # a gold sun crown: a band, broad rays at the sides, narrow ones at the corners
		el.append(box([5, 13, 5], [11, 14, 11], {**ring(1), "up": ts}))
		for x, z, w, d, h in ((7, 5, 2, 1, 3), (7, 10, 2, 1, 2), (5, 7, 1, 2, 2), (10, 7, 1, 2, 2),
				(5, 5, 1, 1, 2), (10, 5, 1, 1, 2), (5, 10, 1, 1, 2), (10, 10, 1, 1, 2)):
			el.append(post(x, z, w, d, 14, h))
	elif name == "north_wind":  # an icicle crown, swept back by the wind; a gust from the mouth
		el.append(box([5, 13, 5], [11, 14, 11], {**ring(1), "up": ts}))
		for x, z, h in ((5.5, 5.5, 3), (7.5, 5, 4), (9.5, 5.5, 3), (7.5, 8, 2.5)):
			el.append(post(x, z, 1, 1, 14, h, rotation={"origin": [8, 14, 8], "axis": "x", "angle": 22.5}))
		for x0, x1, y in ((9, 15, 9), (11, 15, 11)):
			el.append(box([x0, y, 4], [x1, y + 1, 5], {"north": tf(4, 1), "south": tf(4, 1), "up": tf(4, 1), "down": tf(4, 1),
				"east": tf(1, 1), "west": tf(1, 1)}))
	# Glowing parts, a quarter pixel proud of the face.
	if "eyes" in glow:
		for x, u in ((6, 1), (9, 4)):
			el.append(box([x, 11, 4.75], [x + 1, 12, 5], {"north": face([u, EYE_ROW, u + 1, EYE_ROW + 1], F)}, light_emission=12))
	if "visor" in glow:
		el.append(box([6, 11, 4.75], [10, 12, 5], {"north": face([1, EYE_ROW, 5, EYE_ROW + 1], F)}, light_emission=10))
	return {
		"parent": "minecraft:block/block",
		"textures": {"particle": f"{MOD}:block/totem_statue_plinth", "plinth": f"{MOD}:block/totem_statue_plinth",
			"figure": f"{MOD}:block/totem_statue_{name}"},
		"elements": el,
	}


ORDER = ["warding", "keeping", "gathering", "fortification", "growth", "seeking", "beginning", "end", "herding", "clear_skies", "north_wind",
	"undying"]
ITEMS = {"warding": "totem_of_warding", "keeping": "totem_of_keeping", "gathering": "totem_of_gathering",
	"fortification": "totem_of_fortification", "growth": "totem_of_growth", "seeking": "totem_of_seeking",
	"beginning": "totem_of_the_beginning", "end": "totem_of_the_end", "herding": "totem_of_herding",
	"clear_skies": "totem_of_clear_skies", "north_wind": "totem_of_north_wind", "undying": "minecraft:totem_of_undying"}


def write_others():
	flat = _statues(speckle=False)
	for name, (pal, mat, grain, grid, topper, glow) in _statues().items():
		rows = check(["".join(r) for r in grid])
		rows32 = check(["".join(r) for r in flat[name][3]])
		for s, base in ((1, RES / f"assets/{MOD}"), (2, RES / f"resourcepacks/{MOD}_32x/assets/{MOD}")):
			out = base / "textures/block"
			out.mkdir(parents=True, exist_ok=True)
			img = render(rows, pal) if s == 1 else render32(rows32, pal, mat, smooth=False, bevel=0, grain=grain, seed=31 + len(name), cf=True,
				grain_p=0.1)
			img.save(out / f"totem_statue_{name}.png")
		text = json.dumps(statue_model(name, glow), indent="\t")
		(RES / f"assets/{MOD}/models/block/totem_statue_{name}.json").write_text(text + "\n", encoding="utf-8")
		print("wrote statue", name)
	# Blockstate: every totem x facing.
	variants = {}
	for name in ORDER:
		for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
			v = {"model": f"{MOD}:block/totem_statue_{name}"}
			if y:
				v["y"] = y
			variants[f"facing={facing},totem={name}"] = v
	(RES / f"assets/{MOD}/blockstates/totem_statue.json").write_text(json.dumps({"variants": variants}, indent="\t") + "\n", encoding="utf-8")
	# Loot: each statue drops its own totem (26.3 format: an entry's "condition" object; match_block on the state).
	entries = []
	for name in ORDER:
		item = ITEMS[name] if ":" in ITEMS[name] else f"{MOD}:{ITEMS[name]}"
		entries.append({"type": "minecraft:item", "name": item,
			"condition": {"type": "minecraft:all_of", "terms": [{"type": "minecraft:survives_explosion"},
				{"type": "minecraft:match_block", "blocks": f"{MOD}:totem_statue", "state": {"totem": name}}]}})
	loot = {"type": "minecraft:block", "pools": [{"entries": [{"type": "minecraft:alternatives", "children": entries}], "rolls": 1}],
		"random_sequence": f"{MOD}:blocks/totem_statue"}
	(RES / f"data/{MOD}/loot_table/blocks/totem_statue.json").write_text(json.dumps(loot, indent="\t") + "\n", encoding="utf-8")
	print("wrote blockstate and loot")


# --- the Totem of Undying's statue: vanilla's item carved as a chunky illager idol in gold, emerald eyes ------------
# Vanilla's own item colours (textures/item/totem_of_undying.png), darkest to lightest, and its emerald.
UNDYING_PAL = {"o": (133, 64, 15), "1": (160, 91, 35), "2": (197, 135, 66), "3": (209, 167, 93), "4": (234, 219, 132),
	"5": (248, 238, 165), "6": (253, 251, 238), "g": (65, 230, 127), "h": (0, 187, 70)}
UNDYING_MAT = {**{k: "gold" for k in "o123456"}, "g": "gem", "h": "gem"}
# Regions: head front (0,0)-(8,6) with a unibrow, 2x2 emerald eyes either side of the nose; head side (8,0)-(16,6), also
# its back; head top (0,6)-(8,14), also its bottom; body front (8,6)-(14,11) with a belt and emerald; body back
# (8,11)-(14,16), its first four columns the body side; nose (14,6)-(16,9); arm band side (14,9)-(16,11); arm band
# front and top (0,14)-(8,16).
UNDYING_FIG = check([
	"45555554" + "55555554",
	"43333332" + "44444433",
	"36g336g3" + "44443333",
	"3gh44gh2" + "43333332",
	"33444332" + "33333222",
	"22333221" + "22222211",
	"66555554" + "433332" + "56",
	"65555543" + "433332" + "45",
	"55555443" + "433322" + "34",
	"55554443" + "332222" + "54",
	"55544433" + "11gh11" + "32",
	"55444333" + "433332" + "33",
	"54443332" + "433322" + "33",
	"44433322" + "332222" + "33",
	"56555543" + "322221" + "33",
	"44433322" + "111111" + "33",
])


def undying_model():
	P, F = "#plinth", "#figure"
	side4 = lambda uv: {d: face(uv, P) for d in ("north", "east", "south", "west")}
	head_side = face([8, 0, 16, 6], F)
	band = face([0, 14, 8, 16], F)
	el = [
		box([3, 0, 3], [13, 3, 13], {**side4([0, 0, 10, 3]), "up": face([0, 3, 10, 13], P), "down": face([0, 3, 10, 13], P, cullface="down")}),
		box([4, 3, 4], [12, 4, 12], {**side4([0, 13, 8, 14]), "up": face([1, 4, 9, 12], P)}),
		# Robe.
		box([5, 4, 6], [11, 9, 10], {"north": face([8, 6, 14, 11], F), "south": face([8, 11, 14, 16], F),
			"east": face([8, 11, 12, 16], F), "west": face([8, 11, 12, 16], F)}),
		# Crossed arms, a band across the chest standing out at the sides.
		box([4, 6, 5], [12, 8, 7], {"north": band, "south": band, "up": band, "down": band,
			"east": face([14, 9, 16, 11], F), "west": face([14, 9, 16, 11], F)}),
		# Head.
		box([4, 9, 4], [12, 15, 12], {"north": face([0, 0, 8, 6], F), "south": head_side, "east": head_side, "west": head_side,
			"up": face([0, 6, 8, 14], F), "down": face([0, 6, 8, 14], F)}),
		# The illager's long nose.
		box([7, 9, 3], [9, 12, 4], {"north": face([14, 6, 16, 9], F), "east": face([14, 6, 15, 9], F), "west": face([14, 6, 15, 9], F),
			"up": face([14, 6, 16, 7], F), "down": face([14, 8, 16, 9], F)}),
	]
	return {
		"parent": "minecraft:block/block",
		"textures": {"particle": f"{MOD}:block/totem_statue_plinth", "plinth": f"{MOD}:block/totem_statue_plinth",
			"figure": f"{MOD}:block/totem_statue_undying"},
		"elements": el,
	}


def write_undying():
	for s, base in ((1, RES / f"assets/{MOD}"), (2, RES / f"resourcepacks/{MOD}_32x/assets/{MOD}")):
		out = base / "textures/block"
		img = render(UNDYING_FIG, UNDYING_PAL) if s == 1 else render32(UNDYING_FIG, UNDYING_PAL, UNDYING_MAT, smooth=False, bevel=0,
			grain=("gold",), seed=41, cf=True, grain_p=0.05)
		img.save(out / "totem_statue_undying.png")
	text = json.dumps(undying_model(), indent="\t")
	(RES / f"assets/{MOD}/models/block/totem_statue_undying.json").write_text(text + "\n", encoding="utf-8")
	print("wrote statue undying")


if __name__ == "__main__":
	main()
	write_others()
	write_undying()
