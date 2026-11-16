#!/usr/bin/env python3
# dte:C6,B16
"""The Totem of Warding statue: its block model and textures, 16x and the Faithful-style 32x twins (agent-made, listed
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


if __name__ == "__main__":
	main()
