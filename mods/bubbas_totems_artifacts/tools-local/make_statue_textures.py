#!/usr/bin/env python3
# dte:C6,B16
"""Totem statues. The Totem of Warding (hand-placed, main) and, since 2026-11-22, every other totem (a generic figure in
each item palette, write_others, with the blockstate and loot). Block models and textures, 16x and the Faithful-style 32x twins (agent-made, listed
in docs/art-todo.md for the owner to redo or tweak; never AI-generated).

  python mods/bubbas_totems_artifacts/tools-local/make_statue_textures.py

The statue is the Totem of Warding item (tools/warding-textures.py) carved life-size in soul stone and set on a polished
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
# Soul stone, the item's ramp (tools/warding-textures.py), on letters here: o (darkest) .. u (lightest).
SOUL = {"o": (24, 26, 38), "p": (40, 46, 64), "q": (57, 66, 88), "r": (78, 90, 116), "s": (104, 118, 146),
	"t": (136, 150, 176), "u": (170, 182, 204)}
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
		el.append(box([x, 10, 6.4], [x + 1, 11, 6.5], {"north": face([12, 9, 13, 10], F)}, light_emission=15))
	rune = face([13, 9, 15, 11], F)
	# The rune is a small square turned 45 degrees: a diamond, like the item's.
	el.append(box([7.25, 5.75, 5.8], [8.75, 7.25, 6], {"north": rune, "east": face([13, 9, 13.2, 11], F),
		"west": face([13, 9, 13.2, 11], F), "up": face([13, 9, 15, 9.2], F), "down": face([13, 10.8, 15, 11], F)},
		rotation={"origin": [8, 6.5, 5.9], "axis": "z", "angle": 45}, light_emission=12))
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
			render32(PLINTH, PALETTE, MATERIAL, smooth=False, bevel=0, grain=("deep",), seed=3).save(out / "totem_statue_plinth.png")
			render32(fig, PALETTE, MATERIAL, smooth=False, bevel=0, grain=("soul",), seed=5).save(out / "totem_statue_warding.png")
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


def _shade_grid(seed):
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
				level += rnd.choice((0, 0, 0, 1, -1))
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


def _statues():
	"""name -> (palette, materials, grain, grid, topper rows (4 x 6) or None, glowing parts)."""
	defs = {}
	# Keeping: purple obsidian, gold eyes, a gold-bound chest held to the chest.
	g = _shade_grid(21); _eyes(g, "c"); _put(g, R_EMBLEM, ["abb", "bdb", "aaa"])
	defs["keeping"] = (TT.KEEPING_PAL, TT.KEEPING_MAT, ("stone",), g, None, [])
	# Gathering: copper with patina, patina eyes, a patina ring (a pull); hands raised to gather.
	g = _shade_grid(22); _eyes(g, "e"); _put(g, R_EMBLEM, ["qpq", "p4p", "qpq"])
	for v in range(0, 5):
		for u in (1, 4):
			if (u + v) % 3 == 0:
				g[v][u] = "q"  # patina streaks down the front
	defs["gathering"] = (TT.GATHERING_PAL, TT.GATHERING_MAT, ("metal",), g, None, [])
	# Fortification: iron, a flat-topped helm with a glowing visor slit, an obsidian shield on the chest.
	g = _shade_grid(23)
	for u in range(1, 5):
		g[EYE_ROW][u] = "f" if u in (1, 4) else "g"
	_put(g, R_EMBLEM, ["tut", "sts", "tst"])
	defs["fortification"] = (TT.FORT_PAL, TT.FORT_MAT, ("metal",), g, ["6655", "5544", "4433", "5544", "4433", "3322"], ["visor"])
	# Growth: oak wood, green eyes, a sprout on the chest, a leafy crown with a pink blossom.
	g = _shade_grid(24); _eyes(g, "n"); _put(g, R_EMBLEM, ["n4n", "4m4", "4l4"])
	defs["growth"] = (TT.GROWTH_PAL, TT.GROWTH_MAT, ("wood", "leaf"), g, ["mnml", "nkyn", "lmnm", "mlnm", "nmlm", "lmml"], [])
	# Seeking: lapis, gold eyes, a compass dial on the chest (needle to the north), a gold-rimmed hood band.
	g = _shade_grid(25); _eyes(g, "c"); _put(g, R_EMBLEM, ["bRb", "bwb", "bbb"])
	defs["seeking"] = (dict(TT.SEEK_PAL, R=TT.NEEDLE_RED), dict(TT.SEEK_MAT, R="dial"), ("stone",), g,
		["cbba", "bbaa", "cbba", "bbaa", "cbba", "bbaa"], [])
	# The Beginning: weathered stone, purple ender eyes (glowing), strata on the chest, a cap of grass.
	g = _shade_grid(26); _eyes(g, "e"); _put(g, R_EMBLEM, ["HhH", "dDd", "343"])
	defs["beginning"] = (TT.BEGIN_PAL, TT.BEGIN_MAT, ("stone", "grass"), g, ["HhHh", "hgHh", "ghhg", "dDdD", "DdDd", "dddD"], ["eyes"])
	# The End: end stone, an enderman's black face with magenta eyes (glowing), an ender pearl, a purpur crown.
	g = _shade_grid(27)
	_put(g, R_HEAD_FRONT, ["kkkkkk", "kMkkMk", "kkkkkk", "kkkkkk"])
	_put(g, R_EMBLEM, ["tTt", "TwT", "tTt"])
	defs["end"] = (TT.END_PAL, TT.END_MAT, ("stone",), g, ["qppq", "pqqp", "qppq", "pqqp", "qppq", "pqqp"], ["eyes"])
	# Herding: spruce, a woolly ram's head (wool cap, curled horns either side), a gold bell on the chest.
	g = _shade_grid(28); _eyes(g, "e"); _put(g, R_EMBLEM, ["bBb", "BcB", "aba"])
	defs["herding"] = (TT.HERD_PAL, TT.HERD_MAT, ("wood", "wool"), g, ["WWwW", "WwWW", "wWWw", "hHhH", "HhhH", "hhHh"], [])
	# Clear Skies: pale calcite, blue eyes, blue sky with the sun on the chest, a gold sun crown of rays.
	g = _shade_grid(29); _eyes(g, "k"); _put(g, R_EMBLEM, ["sSs", "SYS", "sSs"])
	defs["clear_skies"] = (TT.SKY_PAL, TT.SKY_MAT, ("stone",), g, ["YyYy", "yYyY", "YyYy", "yYyY", "YyYy", "yYyY"], [])
	for name, (pal, mat, grain, grid, topper, glow) in defs.items():
		if topper:
			_put(grid, R_TOPPER, topper)
	return defs


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
	# Chest emblem, standing proud of the robe (Keeping's chest further: a small box hugged to the chest).
	depth = 5.0 if name == "keeping" else 5.6
	edge = face([12, 7, 13, 10], F)
	el.append(box([6.5, 5.5, depth], [9.5, 8.5, 6], {"north": face(_uv(R_EMBLEM), F), "east": edge, "west": edge,
		"up": face([12, 7, 15, 8], F), "down": face([12, 9, 15, 10], F)}))
	# Toppers.
	tf = face([12, 10, 16, 13], F)
	ts = face([12, 13, 16, 16], F)
	ring = {d: tf for d in sides}
	if name == "fortification":  # a flat-topped helm brim
		el.append(box([4.5, 12, 4.5], [11.5, 13.5, 11.5], {**ring, "up": ts, "down": ts}))
	elif name == "growth":  # a leafy crown, a blossom on top
		el.append(box([4.5, 13, 4.5], [11.5, 15, 11.5], {**ring, "up": ts}))
		el.append(box([7, 15, 7], [9, 16, 9], {d: face([13, 11, 14, 12], F) for d in (*sides, "up")}))
	elif name == "seeking":  # a gold-rimmed hood band
		el.append(box([4.5, 11.5, 4.5], [11.5, 13.5, 11.5], {**ring, "up": ts}))
	elif name == "beginning":  # a cap of grass
		el.append(box([4.5, 13, 4.5], [11.5, 14.5, 11.5], {**ring, "up": face([12, 10, 16, 12], F)}))
	elif name == "end":  # a purpur crown: a band and four corner spikes
		el.append(box([5, 13, 5], [11, 13.5, 11], {**ring, "up": ts}))
		for x, z in ((5, 5), (10, 5), (5, 10), (10, 10)):
			el.append(box([x, 13.5, z], [x + 1, 15.5, z + 1], {d: tf for d in (*sides, "up")}))
	elif name == "herding":  # a wool cap and a curled ram's horn either side of the head
		el.append(box([5, 13, 5], [11, 14, 11], {**{d: face([12, 10, 16, 11], F) for d in sides}, "up": face([12, 10, 16, 13], F)}))
		hf = face([12, 13, 16, 16], F)
		for x0, x1 in ((3.5, 5), (11, 12.5)):
			el.append(box([x0, 10, 6.5], [x1, 13, 9.5], {d: hf for d in (*sides, "up", "down")}))
			el.append(box([x0, 9, 5.5], [x1, 10.5, 7], {d: hf for d in (*sides, "up", "down")}))
	elif name == "clear_skies":  # a gold sun crown: a band and rays all round
		el.append(box([5, 13, 5], [11, 13.5, 11], {**ring, "up": ts}))
		for x, z, h in ((7.5, 4.5, 3), (7.5, 10.5, 2.5), (4.5, 7.5, 2.5), (10.5, 7.5, 2.5), (5, 5, 2), (10, 5, 2), (5, 10, 2), (10, 10, 2)):
			el.append(box([x, 13.5, z], [x + 1, 13.5 + h, z + 1], {d: tf for d in (*sides, "up")}))
	# Glowing parts, just proud of the face.
	if "eyes" in glow:
		for x, u in ((6, 1), (9, 4)):
			el.append(box([x, 11, 4.95], [x + 1, 12, 5], {"north": face([u, EYE_ROW, u + 1, EYE_ROW + 1], F)}, light_emission=12))
	if "visor" in glow:
		el.append(box([6, 11, 4.95], [10, 12, 5], {"north": face([1, EYE_ROW, 5, EYE_ROW + 1], F)}, light_emission=10))
	return {
		"parent": "minecraft:block/block",
		"textures": {"particle": f"{MOD}:block/totem_statue_plinth", "plinth": f"{MOD}:block/totem_statue_plinth",
			"figure": f"{MOD}:block/totem_statue_{name}"},
		"elements": el,
	}


ORDER = ["warding", "keeping", "gathering", "fortification", "growth", "seeking", "beginning", "end", "herding", "clear_skies"]
ITEMS = {"warding": "totem_of_warding", "keeping": "totem_of_keeping", "gathering": "totem_of_gathering",
	"fortification": "totem_of_fortification", "growth": "totem_of_growth", "seeking": "totem_of_seeking",
	"beginning": "totem_of_the_beginning", "end": "totem_of_the_end", "herding": "totem_of_herding",
	"clear_skies": "totem_of_clear_skies"}


def write_others():
	for name, (pal, mat, grain, grid, topper, glow) in _statues().items():
		rows = check(["".join(r) for r in grid])
		for s, base in ((1, RES / f"assets/{MOD}"), (2, RES / f"resourcepacks/{MOD}_32x/assets/{MOD}")):
			out = base / "textures/block"
			out.mkdir(parents=True, exist_ok=True)
			img = render(rows, pal) if s == 1 else render32(rows, pal, mat, smooth=False, bevel=0, grain=grain, seed=31 + len(name))
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
		entries.append({"type": "minecraft:item", "name": f"{MOD}:{ITEMS[name]}",
			"condition": {"type": "minecraft:all_of", "terms": [{"type": "minecraft:survives_explosion"},
				{"type": "minecraft:match_block", "blocks": f"{MOD}:totem_statue", "state": {"totem": name}}]}})
	loot = {"type": "minecraft:block", "pools": [{"entries": [{"type": "minecraft:alternatives", "children": entries}], "rolls": 1}],
		"random_sequence": f"{MOD}:blocks/totem_statue"}
	(RES / f"data/{MOD}/loot_table/blocks/totem_statue.json").write_text(json.dumps(loot, indent="\t") + "\n", encoding="utf-8")
	print("wrote blockstate and loot")


if __name__ == "__main__":
	main()
	write_others()
