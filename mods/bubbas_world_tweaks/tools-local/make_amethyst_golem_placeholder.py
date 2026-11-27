#!/usr/bin/env python3
# dte:C8,B16
"""Placeholder Amethyst Golem entity texture and spawn egg, 16x and 32x (32x is a plain 2x upscale).

Replaced by the art pass; the UV boxes match AmethystGolemModel.createBodyLayer().

Usage: python3 make_amethyst_golem_placeholder.py
"""
import random
from pathlib import Path
from PIL import Image

HERE = Path(__file__).resolve().parent
RES = HERE.parent / "src" / "main" / "resources"
PACK32 = RES / "resourcepacks" / "bubbas_world_tweaks_32x"
ENTITY = Path("assets/bubbas_world_tweaks/textures/entity/amethyst_golem/amethyst_golem.png")
EGG = Path("assets/bubbas_world_tweaks/textures/item/amethyst_golem_spawn_egg.png")

# Vanilla amethyst block colours, darkest to brightest.
BLOCK = [(93, 58, 154), (100, 71, 158), (122, 91, 181), (141, 106, 204), (166, 120, 241), (200, 144, 240)]
CLUSTER = [(122, 91, 181), (141, 106, 204), (179, 142, 243), (207, 160, 243), (254, 203, 230)]
EYE = (46, 26, 82)

# (u, v, w, h, d, palette) per cube, as in the model.
BOXES = [
	(0, 0, 8, 5, 8, BLOCK),     # head
	(32, 0, 3, 6, 3, CLUSTER),  # crystal, middle
	(44, 0, 2, 4, 2, CLUSTER),  # crystal, left
	(52, 0, 2, 3, 2, CLUSTER),  # crystal, right
	(0, 15, 8, 6, 6, BLOCK),    # body
	(36, 16, 3, 10, 4, BLOCK),  # right arm
	(50, 16, 3, 10, 4, BLOCK),  # left arm
	(0, 27, 4, 5, 4, BLOCK),    # right leg
	(16, 27, 4, 5, 4, BLOCK),   # left leg
]


def fill(img, rnd, x0, y0, w, h, palette, shade):
	for y in range(y0, y0 + h):
		for x in range(x0, x0 + w):
			i = max(0, min(len(palette) - 1, shade + rnd.choice((-1, 0, 0, 0, 1))))
			img.putpixel((x, y), palette[i] + (255,))


def entity():
	rnd = random.Random(7)
	img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
	for u, v, w, h, d, pal in BOXES:
		mid = len(pal) // 2
		fill(img, rnd, u + d, v, w, d, pal, mid + 1)           # top
		fill(img, rnd, u + d + w, v, w, d, pal, mid - 2)       # bottom
		fill(img, rnd, u, v + d, d, h, pal, mid - 1)           # right side
		fill(img, rnd, u + d, v + d, w, h, pal, mid)           # front
		fill(img, rnd, u + d + w, v + d, d, h, pal, mid - 1)   # left side
		fill(img, rnd, u + 2 * d + w, v + d, w, h, pal, mid - 1)  # back
	for x in (10, 13):
		img.putpixel((x, 10), EYE + (255,))
		img.putpixel((x, 11), EYE + (255,))
	return img


def egg():
	img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
	rnd = random.Random(3)
	cx, cy, rx, ry = 7.5, 8.5, 5.0, 6.5
	for y in range(16):
		for x in range(16):
			dx, dy = (x - cx) / rx, (y - cy) / ry
			r = dx * dx + dy * dy
			if r <= 1.0:
				edge = r > 0.72
				light = (-dx - dy) * 0.5
				i = 1 if edge else 3 + (1 if light > 0.25 else 0) - (1 if light < -0.3 else 0)
				img.putpixel((x, y), BLOCK[max(0, min(len(BLOCK) - 1, i))] + (255,))
	for x, y in ((6, 5), (9, 8), (5, 10), (8, 12), (10, 4)):
		img.putpixel((x, y), CLUSTER[4 if rnd.random() < 0.5 else 3] + (255,))
	return img


def save(img, rel):
	for root, scale in ((RES, 1), (PACK32, 2)):
		out = root / rel
		out.parent.mkdir(parents=True, exist_ok=True)
		img.resize((img.width * scale, img.height * scale), Image.NEAREST).save(out)
		print(out)


if __name__ == "__main__":
	save(entity(), ENTITY)
	save(egg(), EGG)
