# dte:B16
"""Writes the Grave Talisman's PLACEHOLDER textures (16x and their 32x twins) from simple shapes, until people draw
the real ones. Never replace these with AI art (specs/textures.md). Needs Pillow.

  python mods/bubbas_reworked_mobs/tools-local/make_talisman_textures.py

grave_talisman.png is the item (stone tablet on a cord, dark gem). grave_talisman_glow_<1..3>.png are the same picture
with the gem lit; the glow models (models/item/grave_talisman_glow_*.json) show only their gem square (pixels 5..10,
rows 8..13) on a small cuboid that gives off light.
"""
from pathlib import Path

from PIL import Image

RES = Path(__file__).resolve().parent.parent / "src/main/resources"
MOD = "bubbas_totems_artifacts"

CORD_DARK = (58, 38, 24, 255)
CORD_LIGHT = (104, 72, 44, 255)
OUTLINE = (44, 44, 48, 255)
STONE_LIGHT = (150, 148, 140, 255)
STONE = (118, 116, 110, 255)
STONE_DARK = (88, 86, 82, 255)
RIM = (36, 36, 40, 255)
# Gem colours per glow level: (body, highlight). Level 0 is the unlit gem.
GEMS = [
    ((40, 58, 54, 255), (62, 84, 78, 255)),
    ((64, 128, 106, 255), (104, 170, 146, 255)),
    ((110, 210, 168, 255), (170, 240, 210, 255)),
    ((190, 255, 226, 255), (248, 255, 252, 255)),
]
# Rim tint per level (the stone around the gem catches the light).
RIMS = [RIM, (40, 64, 58, 255), (70, 120, 100, 255), (120, 190, 160, 255)]


def draw16(level):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    # Cord: a loop over the top.
    for x, y in [(6, 0), (9, 0), (5, 1), (10, 1), (4, 2), (11, 2), (4, 3), (11, 3), (5, 4), (10, 4), (6, 5), (9, 5)]:
        px[x, y] = CORD_DARK
    for x, y in [(7, 0), (8, 0), (7, 5), (8, 5)]:
        px[x, y] = CORD_LIGHT
    # Tablet: rounded rectangle x 3..12, y 6..15.
    for y in range(6, 16):
        for x in range(3, 13):
            corner = (x in (3, 12)) and (y in (6, 15))
            if corner:
                continue
            edge = x in (3, 12) or y in (6, 15) or ((x in (4, 11)) and (y in (6, 15)))
            if edge:
                px[x, y] = OUTLINE
            elif x + y < 14:
                px[x, y] = STONE_LIGHT
            elif x + y > 20:
                px[x, y] = STONE_DARK
            else:
                px[x, y] = STONE
    # Small carved marks above and below the gem.
    for x in (6, 9):
        px[x, 7] = STONE_DARK
    px[7, 14] = STONE_DARK
    px[8, 14] = STONE_DARK
    # Gem setting: rim x 5..10, y 8..13; gem x 6..9, y 9..12.
    body, highlight = GEMS[level]
    for y in range(8, 14):
        for x in range(5, 11):
            if x in (5, 10) or y in (8, 13):
                px[x, y] = RIMS[level]
            else:
                px[x, y] = body
    px[6, 9] = highlight
    px[7, 9] = highlight
    px[6, 10] = highlight
    return img


def draw32(level):
    """Twice the size, with a softer bevel on the stone and a facet in the gem (Faithful-style detail)."""
    small = draw16(level)
    img = small.resize((32, 32), Image.NEAREST)
    px = img.load()
    for y in range(32):
        for x in range(32):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            sx, sy = x // 2, y // 2
            if small.getpixel((sx, sy)) in (STONE_LIGHT, STONE, STONE_DARK):
                # Speckle: every other sub-pixel a touch darker, so the stone reads as stone at 32x.
                if (x * 7 + y * 3) % 5 == 0:
                    px[x, y] = (max(r - 10, 0), max(g - 10, 0), max(b - 10, 0), a)
    body, highlight = GEMS[level]
    # Facet line across the gem.
    for i in range(6):  # the gem spans x 12..19, y 18..25 at 32x
        px[13 + i, 24 - i] = tuple(min(c + 14, 255) for c in body[:3]) + (255,)
    px[13, 19] = (255, 255, 255, 255) if level == 3 else highlight
    return img


def main():
    out16 = RES / f"assets/{MOD}/textures/item"
    out32 = RES / f"resourcepacks/{MOD}_32x/assets/{MOD}/textures/item"
    out16.mkdir(parents=True, exist_ok=True)
    out32.mkdir(parents=True, exist_ok=True)
    for level in range(4):
        name = "grave_talisman" if level == 0 else f"grave_talisman_glow_{level}"
        draw16(level).save(out16 / f"{name}.png")
        draw32(level).save(out32 / f"{name}.png")
        print(name)


if __name__ == "__main__":
    main()
