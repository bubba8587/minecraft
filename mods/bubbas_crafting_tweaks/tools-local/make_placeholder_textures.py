#!/usr/bin/env python3
# dte:B16
"""Placeholder textures for the Glider (item icon and wings), 16x and matching 32x.

Simple flat shapes drawn in code, NOT final art: a person replaces them before any Modrinth release
(specs/crafting-tweaks-mod.md). Run from anywhere: python3 mods/bubbas_crafting_tweaks/tools-local/make_placeholder_textures.py
"""
from pathlib import Path
from PIL import Image

MOD = Path(__file__).resolve().parent.parent / "src/main/resources"
TEX16 = MOD / "assets/bubbas_crafting_tweaks/textures"
TEX32 = MOD / "resourcepacks/bubbas_crafting_tweaks_32x/assets/bubbas_crafting_tweaks/textures"

BAMBOO = (118, 152, 52, 255)
BAMBOO_DARK = (80, 108, 34, 255)
MEMBRANE = (206, 198, 172, 255)
MEMBRANE_DARK = (168, 158, 132, 255)
FEATHER = (246, 246, 240, 255)
DIAMOND = (92, 219, 213, 255)

# Item icon, 16x16: a bamboo spar with diamond caps, a membrane sail below it, feather tips.
ICON = [
    "................",
    "................",
    "D##############D",
    ".#mmmmmmmmmmmm#.",
    "..mmmmmmmmmmmm..",
    "..Mmmmmm#mmmmM..",
    "...mmmmm#mmmm...",
    "...Mmmmm#mmmM...",
    "....mmmm#mmm....",
    "....Mmmm#mmM....",
    ".....mmm#mm.....",
    ".....ffm#mff....",
    "......ff.ff.....",
    "......f...f.....",
    "................",
    "................",
]
COLORS = {"#": BAMBOO, "D": DIAMOND, "m": MEMBRANE, "M": MEMBRANE_DARK, "f": FEATHER}


def icon(scale):
    img = Image.new("RGBA", (16 * scale, 16 * scale), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(ICON):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            for dy in range(scale):
                for dx in range(scale):
                    c = COLORS[ch]
                    # 32x: shade the lower-right sub-pixel of bamboo and membrane for a little depth.
                    if scale > 1 and dx == scale - 1 and dy == scale - 1 and ch in "#m":
                        c = BAMBOO_DARK if ch == "#" else MEMBRANE_DARK
                    px[x * scale + dx, y * scale + dy] = c
    return img


def wings(scale):
    """Elytra wing layout (64x32 at 16x): ElytraModel's 10x20x2 box at texture offset (22, 0)."""
    img = Image.new("RGBA", (64 * scale, 32 * scale), (0, 0, 0, 0))
    px = img.load()

    def rect(x0, y0, x1, y1, fill):
        for y in range(y0 * scale, y1 * scale):
            for x in range(x0 * scale, x1 * scale):
                px[x, y] = fill(x / scale, y / scale)

    def face(x, y):
        # x, y in 16x texel units within the face region; bamboo spar along the top, ribs, feather fringe at the bottom.
        if y < 4:
            return BAMBOO if y >= 2.5 or int(y * 2) % 2 == 0 else BAMBOO_DARK
        if y >= 19:
            return FEATHER
        if int(x) % 3 == 0:
            return MEMBRANE_DARK
        return MEMBRANE

    rect(24, 0, 34, 2, lambda x, y: BAMBOO)   # top
    rect(34, 0, 44, 2, lambda x, y: FEATHER)  # bottom
    rect(22, 2, 24, 22, lambda x, y: BAMBOO_DARK)  # side
    rect(34, 2, 36, 22, lambda x, y: BAMBOO_DARK)  # side
    rect(24, 2, 34, 22, lambda x, y: face(x - 24, y))  # front
    rect(36, 2, 46, 22, lambda x, y: face(x - 36, y))  # back
    return img


def main():
    for base, scale in ((TEX16, 1), (TEX32, 2)):
        (base / "item").mkdir(parents=True, exist_ok=True)
        (base / "entity/equipment/wings").mkdir(parents=True, exist_ok=True)
        icon(scale).save(base / "item/glider.png")
        wings(scale).save(base / "entity/equipment/wings/glider.png")
    print("wrote", TEX16, TEX32)


if __name__ == "__main__":
    main()
