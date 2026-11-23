#!/usr/bin/env python3
# dte:B16,C1
"""The gravestone's art and models (Bubba's Reworked Mobs, specs/graveyards.md): a weathered stone headstone on a
plinth. Agent-made, listed in docs/art-todo.md for the owner to redo or tweak; never AI-generated.

  python tools/gravestone-textures.py

Writes
  textures/block/gravestone.png (16x) and its Faithful-style 32x twin in the built-in 32x pack;
  models/block/gravestone_rot_0..3.json (turned 0, 22.5, 45, 67.5 degrees, as vanilla's sign templates) and
  models/item/gravestone.json.

Shape (block pixels, front = south): plinth 14 x 2 x 6, body 12 x 10 x 4 on it, a shoulder 10 wide and a crown 6
wide stepping the top. The texture is drawn as the stone seen from the front (auto UVs: u = x, v = 16 - y), with a
carved inset panel round the text area; sides and tops sample its plain middle. Light from the top left. Needs Pillow.
"""
import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "mods" / "bubbas_reworked_mobs" / "src" / "main" / "resources"
ASSETS = RES / "assets" / "bubbas_reworked_mobs"
PACK32 = RES / "resourcepacks" / "bubbas_reworked_mobs_32x" / "assets" / "bubbas_reworked_mobs"

# Elements (from, to): plinth, body, shoulder, crown.
ELEMENTS = [
	([1, 0, 5], [15, 2, 11]),
	([2, 2, 6], [14, 12, 10]),
	([3, 12, 6], [13, 13, 10]),
	([5, 13, 6], [11, 14, 10]),
]

STONE = (138, 138, 134)  # weathered smooth stone, a touch warm
PLINTH = (112, 112, 108)
GROOVE = (92, 92, 90)
LIGHT = (168, 168, 162)
MOSS = (82, 104, 52)


def clamp(c):
	return tuple(max(0, min(255, int(v))) for v in c)


def shade(c, f):
	return clamp(tuple(v * f for v in c))


def stone_texture(scale, seed):
	"""The front view at 16 * scale pixels."""
	n = 16 * scale
	rnd = random.Random(seed)
	img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
	px = img.load()

	def cell(u, v):
		"""Which part a pixel (in 16x units) belongs to, seen from the front."""
		y = 16 - v
		if y < 2:
			return "plinth"
		if 2 <= u < 14 and y < 12:
			return "body"
		if 3 <= u < 13 and y < 13:
			return "body"
		if 5 <= u < 11 and y < 14:
			return "body"
		return "plain"  # outside the front silhouette: sampled only by sides and tops

	for py in range(n):
		for pxx in range(n):
			u = pxx / scale
			v = py / scale
			part = cell(u, v)
			base = PLINTH if part == "plinth" else STONE
			# Grain: soft per-pixel wobble, a little blotchy at 32x.
			f = 1.0 + rnd.uniform(-0.06, 0.06) + (rnd.uniform(-0.03, 0.03) if scale > 1 else 0)
			col = shade(base, f)
			if part == "plinth" and rnd.random() < 0.12:
				col = shade(MOSS, rnd.uniform(0.85, 1.1))
			px[pxx, py] = col + (255,)

	def put(u, v, colour):
		"""Paint one 16x pixel (a 2x2 block at 32x... unless thin)."""
		for dy in range(scale):
			for dx in range(scale):
				px[u * scale + dx, v * scale + dy] = colour + (255,)

	def line(u0, v0, u1, v1, colour, thin=True):
		"""A 1-pixel line at this resolution between 16x pixel corners (inclusive)."""
		if scale == 1 or not thin:
			for v in range(v0, v1 + 1):
				for u in range(u0, u1 + 1):
					put(u, v, colour)
			return
		for y in range(v0 * scale, (v1 + 1) * scale):
			for x in range(u0 * scale, (u1 + 1) * scale):
				px[x, y] = colour + (255,)

	# Carved inset panel round the text area (16x: x 3..12, rows 5..13): dark on the top and left edges (the cut's
	# shadow), light on the bottom and right (the lit lip).
	if scale == 1:
		line(3, 5, 12, 5, GROOVE)
		line(3, 5, 3, 13, GROOVE)
		line(4, 13, 12, 13, LIGHT)
		line(12, 6, 12, 13, LIGHT)
	else:
		for x in range(6, 26):
			px[x, 10] = GROOVE + (255,)
			px[x, 27] = LIGHT + (255,)
		for y in range(10, 28):
			px[6, y] = GROOVE + (255,)
			px[25, y] = LIGHT + (255,)
	# Weathering: a few chips and a crack.
	for _ in range(3 * scale):
		u = rnd.randrange(2 * scale, 14 * scale)
		v = rnd.randrange(3 * scale, 14 * scale)
		px[u, v] = shade(STONE, 0.78) + (255,)
	return img


def model(angle):
	elements = []
	for (frm, to) in ELEMENTS:
		faces = {}
		for face in ("north", "east", "south", "west", "up", "down"):
			f = {"texture": "#all"}
			if face == "down" and frm[1] == 0:
				f["cullface"] = "down"
			faces[face] = f
		e = {"from": frm, "to": to, "faces": faces}
		e["rotation"] = {"angle": angle if angle != 0 else 0.0001, "axis": "y", "origin": [8, 0, 8]}
		elements.append(e)
	return {
		"parent": "minecraft:block/block",
		"textures": {"all": "bubbas_reworked_mobs:block/gravestone", "particle": "bubbas_reworked_mobs:block/gravestone"},
		"elements": elements,
	}


def main():
	for scale, base in ((1, ASSETS), (2, PACK32)):
		out = base / "textures" / "block"
		out.mkdir(parents=True, exist_ok=True)
		stone_texture(scale, 2026 + scale).save(out / "gravestone.png")
	models = ASSETS / "models"
	(models / "block").mkdir(parents=True, exist_ok=True)
	(models / "item").mkdir(parents=True, exist_ok=True)
	for i, angle in enumerate((0, -22.5, -45, -67.5)):
		(models / "block" / f"gravestone_rot_{i}.json").write_text(json.dumps(model(angle), indent=2) + "\n")
	item = {"parent": "bubbas_reworked_mobs:block/gravestone_rot_0",
		"display": {
			"gui": {"rotation": [20, 200, 0], "translation": [0, 1, 0], "scale": [0.8, 0.8, 0.8]},
			"ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.4, 0.4, 0.4]},
			"fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.6, 0.6, 0.6]},
			"thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
			"firstperson_righthand": {"rotation": [0, 225, 0], "translation": [0, 2, 0], "scale": [0.4, 0.4, 0.4]},
			"firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 2, 0], "scale": [0.4, 0.4, 0.4]}}}
	(models / "item" / "gravestone.json").write_text(json.dumps(item, indent=2) + "\n")
	print("wrote gravestone textures (16x, 32x) and models")


if __name__ == "__main__":
	main()
