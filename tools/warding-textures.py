#!/usr/bin/env python3
# dte:B16,C1
"""Placeholder texture for the Totem of Warding (Bubba's Reworked Mobs), drawn from simple shapes at 16x and 32x
(specs/textures.md).

  python tools/warding-textures.py

A small carved idol of blue-grey soul stone: a hooded figure with short arms on a wide base, two soul-fire eyes, a
ward rune (a diamond eye) on its chest and a soul flame on its head. Shaded with light from the top left and a dark rim, like vanilla
items. The 32x set is drawn at 32 pixels with finer shading, not scaled up. A placeholder until a person draws the real
art; never AI-generated. Needs Pillow.
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "mods" / "bubbas_totems_artifacts" / "src" / "main" / "resources"
OUT = {16: RES / "assets" / "bubbas_totems_artifacts" / "textures" / "item",
	32: RES / "resourcepacks" / "bubbas_totems_artifacts_32x" / "assets" / "bubbas_totems_artifacts" / "textures" / "item"}

STONE = [(28, 33, 46), (46, 55, 74), (66, 78, 102), (88, 104, 130), (112, 130, 156), (140, 158, 182)]
FLAME = [(20, 90, 110), (40, 160, 175), (95, 215, 220), (190, 250, 245)]
EYE = (130, 245, 240)


def rect(u, v, x0, y0, x1, y1):
	return x0 <= u <= x1 and y0 <= v <= y1


def ellipse(u, v, cx, cy, rx, ry):
	return ((u - cx) / rx) ** 2 + ((v - cy) / ry) ** 2 <= 1.0


def stone(u, v):
	return (ellipse(u, v, 0.5, 0.36, 0.17, 0.14)  # hooded head
		or rect(u, v, 0.33, 0.36, 0.67, 0.84)  # body
		or ellipse(u, v, 0.27, 0.58, 0.09, 0.11) or ellipse(u, v, 0.73, 0.58, 0.09, 0.11)  # arms
		or rect(u, v, 0.22, 0.80, 0.78, 0.93))  # base


def flame(u, v):
	if not (0.05 <= v <= 0.24):
		return False
	half = 0.03 + (v - 0.05) / 0.19 * 0.09  # widens downward to the head
	return abs(u - 0.5) <= half


def draw(n):
	img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
	m = [[stone((x + 0.5) / n, (y + 0.5) / n) for x in range(n)] for y in range(n)]
	f = [[flame((x + 0.5) / n, (y + 0.5) / n) and not m[y][x] for x in range(n)] for y in range(n)]
	levels = len(STONE)
	for y in range(n):
		for x in range(n):
			u, v = (x + 0.5) / n, (y + 0.5) / n
			if m[y][x]:
				rim = any(not (0 <= x + dx < n and 0 <= y + dy < n and m[y + dy][x + dx]) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
				if rim:
					img.putpixel((x, y), STONE[0] + (255,))
					continue
				# light from the top left; the 32x set gets every shade, the 16x set every other one
				shade = 1.0 - 0.55 * u - 0.45 * v + 0.35
				idx = max(1, min(levels - 1, int(round(shade * (levels - 1)))))
				if n == 16:
					idx = max(1, min(levels - 2, idx))
				img.putpixel((x, y), STONE[idx] + (255,))
			elif f[y][x]:
				t = (v - 0.05) / 0.19  # 0 at the tip, 1 at the head
				core = abs(u - 0.5) < 0.04 + t * 0.03
				idx = 3 if core and t > 0.35 else (2 if core or t > 0.6 else 1)
				if t < 0.15:
					idx = 1
				img.putpixel((x, y), FLAME[idx] + (255,))
	s = n / 16
	# soul-fire eyes under the hood
	for ex in (0.42, 0.58):
		for dy in range(int(s)):
			for dx in range(int(s)):
				img.putpixel((int(ex * n) + dx - (0 if n == 16 else 1), int(0.38 * n) + dy), EYE + (255,))
	# the ward rune on the chest: a diamond (an eye) with a bright centre
	r = 2 if n == 16 else 4
	cx, cy = n // 2 - (1 if n == 16 else 0), int(0.64 * n)
	for y in range(cy - r, cy + r + 1):
		for x in range(cx - r, cx + r + 1):
			d = abs(x - cx) + abs(y - cy)
			if d == r or (n == 32 and d == r - 1):
				img.putpixel((x, y), FLAME[2] + (255,))
	img.putpixel((cx, cy), EYE + (255,))
	return img


def main():
	for n, out in OUT.items():
		out.mkdir(parents=True, exist_ok=True)
		draw(n).save(out / "totem_of_warding.png")
		print("wrote", out / "totem_of_warding.png")


if __name__ == "__main__":
	main()
