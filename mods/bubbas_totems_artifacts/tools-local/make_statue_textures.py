#!/usr/bin/env python3
# dte:C6,B16
"""Placeholder totem statue textures (agent-made, scripted; the owner will design the statues: docs/art-todo.md).

  python mods/bubbas_totems_artifacts/tools-local/make_statue_textures.py

Writes, at 16x and 32x (the 32x set into the built-in Faithful-style pack):
  textures/block/totem_statue_plinth.png   shared plinth: side strip (0..10 x 0..2), top (0..10 x 2..12)
  textures/block/totem_statue_<totem>.png  the column: front (0..8 x 0..12) carved with the totem's own item sprite,
                                           side (8..16 x 0..12) plain carved stone with a glowing rune line
Coordinates are in 16x pixels; the 32x copy draws everything at double size from the 32x item sprite.
"""
from pathlib import Path

from PIL import Image

RES = Path(__file__).resolve().parent.parent / "src/main/resources"
MOD = "bubbas_totems_artifacts"
# Deepslate-blue stone, darkest to lightest (the Totem of Warding's own stone colors).
STONE = [(28, 33, 46), (46, 55, 74), (66, 78, 102), (88, 104, 130), (112, 130, 156), (140, 158, 182)]
# Per totem: the glow color used for the side rune line.
TOTEMS = {"warding": ("totem_of_warding", (95, 215, 220))}


def noise(x, y, seed):
    return ((x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)) & 0xFF


def stone(img, x0, y0, w, h, s, seed, base=2):
    """Fill a box with speckled stone, a light top edge and a dark bottom edge (block-style)."""
    for y in range(y0 * s, (y0 + h) * s):
        for x in range(x0 * s, (x0 + w) * s):
            n = noise(x // max(1, s // 2) if s > 1 else x, y // max(1, s // 2) if s > 1 else y, seed)
            shade = base + (1 if n < 60 else 0) - (1 if n > 215 else 0)
            if y < y0 * s + max(1, s // 16):
                shade += 1
            if y >= (y0 + h) * s - max(1, s // 16):
                shade -= 1
            img.putpixel((x, y), STONE[max(0, min(len(STONE) - 1, shade))] + (255,))


def plinth(s):
    img = Image.new("RGBA", (16 * s, 16 * s), (0, 0, 0, 0))
    stone(img, 0, 0, 10, 2, s, 1, base=1)   # side strip
    stone(img, 0, 2, 10, 10, s, 2, base=2)  # top
    # A carved border on the top.
    for i in range(10 * s):
        for (x, y) in ((i, 2 * s), (i, 12 * s - 1), (0, 2 * s + i), (10 * s - 1, 2 * s + i)):
            img.putpixel((x, y), STONE[1] + (255,))
    return img


def column(s, item_png, glow):
    img = Image.new("RGBA", (16 * s, 16 * s), (0, 0, 0, 0))
    stone(img, 0, 0, 8, 12, s, 3, base=2)  # front background
    stone(img, 8, 0, 8, 12, s, 4, base=2)  # side
    # Front: the totem's item sprite as a relief, cropped to its outline and fitted to the face (1 px margin).
    sprite = Image.open(item_png).convert("RGBA")
    sprite = sprite.crop(sprite.getbbox()).resize((7 * s, 11 * s), Image.NEAREST)
    ox, oy = s // 2, s // 2
    for y in range(sprite.height):
        for x in range(sprite.width):
            c = sprite.getpixel((x, y))
            if c[3]:
                img.putpixel((ox + x, oy + y), c)
    # Side: a vertical rune line in the totem's glow color, with notches.
    gx = 8 * s + 4 * s - max(1, s // 2)
    for y in range(2 * s, 10 * s):
        for w in range(max(1, s // 2) + (0 if s == 1 else 0)):
            img.putpixel((gx + w, y), glow + (255,))
        if (y // s) % 3 == 0:
            for w in range(-s, s + max(1, s // 2)):
                img.putpixel((gx + w, y), glow + (255,))
    return img


def main():
    for s, base in ((1, RES / f"assets/{MOD}"), (2, RES / f"resourcepacks/{MOD}_32x/assets/{MOD}")):
        out = base / "textures/block"
        out.mkdir(parents=True, exist_ok=True)
        plinth(s).save(out / "totem_statue_plinth.png")
        for name, (item, glow) in TOTEMS.items():
            column(s, base / f"textures/item/{item}.png", glow).save(out / f"totem_statue_{name}.png")
        print("wrote", out)


if __name__ == "__main__":
    main()
