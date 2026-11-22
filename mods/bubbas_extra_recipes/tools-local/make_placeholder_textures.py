#!/usr/bin/env python3
# dte:B16
"""The Glider's textures: the item icon and the worn wings, 16x and their Faithful-style 32x twins (agent-made, listed
in docs/art-todo.md for the owner to redo or tweak; never AI-generated; a person's art replaces it before any Modrinth
release, specs/extra-recipes-mod.md). Needs Pillow.

  python mods/bubbas_extra_recipes/tools-local/make_placeholder_textures.py

It is built from its recipe, so it reads as a home-made hang glider rather than an elytra: phantom membrane stretched
over a bamboo frame, diamond caps on the spar ends, feathers on the trim.
- Item icon: a hang glider seen from the front: a swept chevron wing (bamboo leading edge, membrane sail with a
  scalloped trailing edge) and the triangle control frame hanging under it, feather tassels on the bar. The 16x grid is
  hand-placed; the 32x twin comes from tools/pixelart.py.
- Wings (64x32, the elytra's UV layout: ElytraModel's 10x20x2 box at texture offset 22,0, mirrored for the right
  wing): each wing is a sail panel drawn as a function of chord (0 at the leading edge, 10 at the trailing edge) and
  span (0 at the shoulder, 20 at the tip; the tip points out sideways when gliding). A bamboo spar runs along the
  leading edge with a diamond cap at the tip, bamboo battens cross the sail, the trailing edge is scalloped between
  them and swept back toward the tip, which ends in feathers. The 32x set evaluates the same function at twice the
  resolution with finer detail (bamboo nodes, membrane veins), not a scale-up.
"""
import math
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent.parent.parent / "tools"))
from pixelart import check, render, render32  # noqa: E402

MOD = HERE.parent / "src/main/resources"
TEX16 = MOD / "assets/bubbas_extra_recipes/textures"
TEX32 = MOD / "resourcepacks/bubbas_extra_recipes_32x/assets/bubbas_extra_recipes/textures"

# Vanilla's own ramps: bamboo (item/bamboo, block/bamboo_stalk), phantom membrane (item/phantom_membrane), feather,
# diamond.
BAMBOO = {"k": (52, 80, 18), "b": (70, 109, 12), "B": (96, 152, 22), "H": (141, 194, 79)}
MEMBRANE = {"m": (220, 217, 192), "n": (195, 185, 161), "p": (169, 155, 135), "v": (126, 98, 123), "V": (111, 81, 108)}
FEATHER = {"f": (255, 255, 255), "F": (211, 211, 211), "G": (150, 150, 150)}
DIAMOND = {"D": (92, 219, 213), "E": (161, 251, 232), "d": (44, 152, 145)}
PALETTE = {**BAMBOO, **MEMBRANE, **FEATHER, **DIAMOND}
MATERIAL = {**{k: "bamboo" for k in BAMBOO}, **{k: "membrane" for k in MEMBRANE}, **{k: "feather" for k in FEATHER},
	**{k: "diamond" for k in DIAMOND}}

ICON = check([
	"................",
	".......HB.......",
	".....HBmmBb.....",
	"...HBmmmmnnBb...",
	".HBmmmmmnnnppBb.",
	"EmmmmmnnnnnpppbD",
	"dmmmnnnnnnnpppVd",
	".VnnVVnbbnVVppV.",
	"..VV..VbbV..VV..",
	"......b..b......",
	".....b....b.....",
	"....b......b....",
	"...HBBBBBBBBb...",
	"...f........f...",
	"...F........F...",
	"...G........G...",
])


def wing_face(c, r, fine):
	"""Colour of a wing face at chord c (0 leading .. 10 trailing) and span r (0 shoulder .. 20 tip), or None.
	fine: drawing at 32x (add the small details)."""
	px = 0.5 if fine else 1.0  # one texture pixel, in 16x units
	# Trailing edge: scalloped between battens, swept back over the last third.
	battens = (0.0, 6.0, 12.0, 18.0)
	seg = min((b for b in battens if b <= r), default=0.0)
	frac = (r - seg) / 6.0
	trail = 10.0 - 1.6 * math.sin(math.pi * min(frac, 1.0))
	if r > 13.0:
		trail = min(trail, 10.0 - (r - 13.0) * 1.05)
	# Leading-edge spar.
	if c < 2.0:
		if r >= 18.0:  # diamond cap
			if c < px or r < 18.0 + px:
				return DIAMOND["E"]
			return DIAMOND["D"] if c < 2.0 - px else DIAMOND["d"]
		node = (r % 6.0) >= 6.0 - px  # bamboo node rings
		if node:
			return BAMBOO["b"] if c < 2.0 - px else BAMBOO["k"]
		if c < px:
			return BAMBOO["H"]
		if fine and c < 1.0 and (r % 6.0) < 3.0:
			return BAMBOO["H"]
		return BAMBOO["B"] if c < 2.0 - px else BAMBOO["b"]
	if c >= trail:
		return None
	# Feathered tip.
	if r >= 17.0:
		barb = int((c - 2.0) / px) % 2 == 0
		if c >= trail - px:
			return FEATHER["G"]
		return FEATHER["f"] if barb else FEATHER["F"]
	# Battens across the sail.
	for b in battens[1:]:
		if b - px <= r < b:
			return BAMBOO["B"] if c < trail - 2 * px else BAMBOO["b"]
	# Sail: membrane lit from the spar side and the top, a purple rim along the trailing edge.
	if c >= trail - px:
		return MEMBRANE["V"] if fine else MEMBRANE["v"]
	if fine and c >= trail - 2 * px:
		return MEMBRANE["v"]
	shade = (c - 2.0) / 8.0 + ((r - seg) / 6.0) * 0.35
	if fine:
		# A faint vein from the spar to the trailing edge in each panel.
		vein_r = seg + 1.0 + (c - 2.0) * 0.45
		if abs(r - vein_r) < 0.25 and 3.0 <= c:
			return MEMBRANE["p"]
	if shade < 0.35:
		return MEMBRANE["m"]
	if shade < 0.75:
		return MEMBRANE["n"]
	return MEMBRANE["p"]


def wings(scale):
	"""The 64x32 wing sheet (elytra layout) at 16x (scale 1) or 32x (scale 2)."""
	img = Image.new("RGBA", (64 * scale, 32 * scale), (0, 0, 0, 0))
	px = img.load()
	fine = scale > 1
	step = 1.0 / scale

	def put(u, v, col):
		if col is not None:
			px[u, v] = col + (255,)

	for j in range(20 * scale):
		r = (j + 0.5) * step
		for i in range(10 * scale):
			x = (i + 0.5) * step
			# Front face (24..34): leading edge at the right (u 34 meets the leading side). Back face (36..46):
			# leading edge at the left.
			put(24 * scale + i, 2 * scale + j, wing_face(10.0 - x, r, fine))
			put(36 * scale + i, 2 * scale + j, wing_face(x, r, fine))
		for i in range(2 * scale):
			# Leading side (34..36): the spar seen edge-on. Trailing side (22..24): only along the sail's full edge.
			c = (i + 0.5) * step
			put(34 * scale + i, 2 * scale + j, BAMBOO["b"] if (r % 6.0) >= 6.0 - step else
				(DIAMOND["D"] if r >= 18.0 else BAMBOO["B"]))
			if wing_face(9.9, r, fine) is not None:
				put(22 * scale + i, 2 * scale + j, MEMBRANE["v"])
	for j in range(2 * scale):
		for i in range(10 * scale):
			x = (i + 0.5) * step
			# Top (24..34): the shoulder end; bottom (34..44): the tip end.
			put(24 * scale + i, j, BAMBOO["B"] if x > 8.0 else MEMBRANE["n"])
			put(34 * scale + i, j, DIAMOND["D"] if x < 2.0 else None)
	return img


def main():
	for base, scale in ((TEX16, 1), (TEX32, 2)):
		(base / "item").mkdir(parents=True, exist_ok=True)
		(base / "entity/equipment/wings").mkdir(parents=True, exist_ok=True)
		icon = render(ICON, PALETTE) if scale == 1 else render32(ICON, PALETTE, MATERIAL, outline="", bevel=0.08)
		icon.save(base / "item/glider.png")
		wings(scale).save(base / "entity/equipment/wings/glider.png")
	print("wrote", TEX16, TEX32)


if __name__ == "__main__":
	main()
