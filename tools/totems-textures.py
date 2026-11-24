#!/usr/bin/env python3
# dte:B16,C6
"""Item textures for the five totems of 2026-11-16 in Bubba's Totems & Artifacts (Keeping, Gathering, Fortification,
Growth, Seeking), 16x and their Faithful-style 32x twins (specs/textures.md). Agent-made, listed in docs/art-todo.md
for the owner to redo or tweak; never AI-generated.

  python tools/totems-textures.py

Each is a small carved idol in the Totem of Undying's idiom (head, arms out, body, foot), drawn like the Totem of
Warding (tools/warding-textures.py): same body and pose, its own material, head and chest emblem.

  Keeping        purple obsidian stone, gold eyes, clutching a small gold-bound chest
  Gathering      copper with green patina, arms ending in upturned hands, a patina ring (a pull) on the chest
  Fortification  iron, a flat-topped helm with a glowing visor slit, an obsidian shield on the chest
  Growth         oak wood, a leafy crown with a pink blossom, a green sprout on the chest
  Beginning      weathered stone wearing a grass cap, purple ender eyes, a strata emblem (grass, dirt, stone)
  End            end stone with a purpur crown, an enderman's black face and magenta eyes, an ender pearl on the chest
  Seeking        lapis stone, a gold-rimmed hood, a compass dial on the chest; 16 needle frames
                 (totem_of_seeking_00..15, frame i = needle turned i/16 clockwise from up, as the item model's
                 minecraft:compass dispatch expects). At 32x the needle is drawn at 32x, not scaled up.

Light from the top left; the outline is the material's darkest shade. The 16x grids are hand-placed; the 32x twins come
from tools/pixelart.py. Needs Pillow.
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from pixelart import check, render, render32, rgba  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "mods" / "bubbas_totems_artifacts" / "src" / "main" / "resources"
OUT = {16: RES / "assets" / "bubbas_totems_artifacts" / "textures" / "item",
	32: RES / "resourcepacks" / "bubbas_totems_artifacts_32x" / "assets" / "bubbas_totems_artifacts" / "textures" / "item"}


def shades(*cols):
	"""Outline 'o' then '1'..'6', darkest to lightest."""
	keys = "o123456"
	return {k: c for k, c in zip(keys, cols)}


# --- Keeping: purple obsidian, gold ---------------------------------------------------------------
KEEPING_PAL = {**shades((22, 12, 36), (44, 24, 70), (64, 38, 100), (88, 56, 132), (114, 80, 164), (146, 110, 194), (184, 150, 222)),
	"a": (122, 76, 18), "b": (200, 146, 42), "c": (240, 202, 84), "d": (255, 244, 176)}
KEEPING = check([
	"................",
	"................",
	".....oooooo.....",
	"....o566654o....",
	"...o56544443o...",
	"...o54d44d32o...",
	"...o54433332o...",
	"....o443321o....",
	".oooo5abba1oooo.",
	"o56655bccb22221o",
	"o54544adda32211o",
	".oooo4aaaa1oooo.",
	"...o54433221o...",
	"....o543321o....",
	"...o65544321o...",
	"...oooooooooo...",
])
KEEPING_MAT = {**{k: "stone" for k in "123456"}, **{k: "gold" for k in "abcd"}}

# --- Gathering: copper and patina ------------------------------------------------------------------
GATHERING_PAL = {**shades((58, 24, 14), (108, 48, 28), (150, 72, 44), (188, 100, 62), (216, 130, 88), (234, 162, 118), (250, 200, 158)),
	"p": (52, 132, 108), "q": (94, 190, 158), "e": (170, 250, 220)}
GATHERING = check([
	"................",
	"................",
	".....oooooo.....",
	"....o566543o....",
	"...o56654332o...",
	"...o54e44e22o...",
	"...o54433322o...",
	"oo..o443321o..oo",
	"o5ooo543321ooo2o",
	"o56655qqqq32221o",
	"o5454qpeepq2211o",
	".oooo4qqqq1oooo.",
	"...o54433p21o...",
	"....o543321o....",
	"...o65544321o...",
	"...oooooooooo...",
])
GATHERING_MAT = {**{k: "metal" for k in "123456"}, **{k: "patina" for k in "pqe"}}

# --- Fortification: iron, obsidian shield ----------------------------------------------------------
FORT_PAL = {**shades((26, 26, 32), (56, 58, 66), (86, 88, 98), (118, 120, 130), (150, 152, 162), (184, 186, 194), (222, 224, 230)),
	"f": (240, 120, 30), "g": (255, 214, 110), "s": (30, 16, 48), "t": (66, 42, 100), "u": (130, 92, 184)}
FORTIFICATION = check([
	"................",
	"...oooooooooo...",
	"...o66655443o...",
	"...o65544332o...",
	"...o1gf11fg1o...",
	"...o54433221o...",
	"...o44332211o...",
	"....oo4321oo....",
	".oooo5tuut1oooo.",
	"o56655tsst22221o",
	"o54544tsst32211o",
	".oooo43ts31oooo.",
	"...o544tt321o...",
	"....o543321o....",
	"...o65544321o...",
	"...oooooooooo...",
])
FORT_MAT = {**{k: "metal" for k in "123456"}, **{k: "glow" for k in "fg"}, **{k: "shield" for k in "stu"}}

# --- Growth: oak, leaves, blossom ------------------------------------------------------------------
GROWTH_PAL = {**shades((42, 26, 12), (72, 46, 22), (102, 68, 34), (134, 94, 52), (164, 122, 72), (192, 150, 94), (218, 182, 126)),
	"l": (30, 84, 22), "m": (58, 134, 38), "n": (112, 190, 62), "k": (246, 136, 178), "y": (255, 226, 120)}
GROWTH = check([
	"......lmml......",
	"....lmnnmnl.....",
	"...lmnkynmml....",
	"...olmmllmmlo...",
	"...o56544332o...",
	"...o54n44n22o...",
	"...o54433322o...",
	"....o443321o....",
	".oooo54nm21oooo.",
	"o56655nmlm22221o",
	"o545443ln322211o",
	".oooo43l331oooo.",
	"...o5443l221o...",
	"....o543321o....",
	"...o65544321o...",
	"...oooooooooo...",
])
GROWTH_MAT = {**{k: "wood" for k in "123456"}, **{k: "leaf" for k in "lmn"}, "k": "flower", "y": "flower"}

# --- Seeking: lapis, gold, a compass dial ---------------------------------------------------------
SEEK_PAL = {**shades((12, 18, 46), (24, 38, 90), (34, 56, 126), (50, 78, 162), (72, 106, 194), (108, 140, 218), (158, 182, 238)),
	"a": (130, 86, 16), "b": (210, 156, 48), "c": (250, 214, 100), "w": (150, 150, 158), "y": (226, 222, 210)}
SEEKING = check([
	"................",
	"................",
	".....oooooo.....",
	"....o566543o....",
	"...o5bbbbbb2o...",
	"...o51c11c12o...",
	"...o51111112o...",
	"....o443321o....",
	".oooo5abba1oooo.",
	"o5665ayyyyb2221o",
	"o5454ayyyyb2211o",
	".ooooayyyyb1oooo",
	"...o5ayyyyb1o...",
	"....o5abbb2o....",
	"...o65544321o...",
	"...oooooooooo...",
])
SEEK_MAT = {**{k: "stone" for k in "123456"}, **{k: "gold" for k in "abc"}, "y": "dial", "w": "dial"}
NEEDLE_RED = (214, 36, 36)
NEEDLE_DARK = (70, 70, 78)

# --- Beginning: weathered stone, a grass cap, strata on the chest (2026-11-22) ------------------------
BEGIN_PAL = {**shades((24, 24, 26), (52, 52, 56), (78, 78, 82), (104, 104, 108), (128, 128, 132), (152, 152, 156), (182, 182, 186)),
	"g": (52, 98, 28), "h": (86, 140, 44), "H": (124, 180, 66), "d": (100, 68, 40), "D": (134, 96, 60), "e": (200, 90, 230)}
BEGINNING = check([
	"................",
	"......hHHh......",
	"....ghHhhHhg....",
	"...oghgghgggo...",
	"...o5g544g43o...",
	"...o54e44e32o...",
	"...o54433332o...",
	"....o443321o....",
	".oooo5HhhH1oooo.",
	"o56655hggh22221o",
	"o54544dDDd32211o",
	".oooo4d43d1oooo.",
	"...o54433221o...",
	"....o543321o....",
	"...o65544321o...",
	"...oooooooooo...",
])
BEGIN_MAT = {**{k: "stone" for k in "123456"}, **{k: "grass" for k in "ghH"}, **{k: "dirt" for k in "dD"}, "e": "eye"}

# --- End: end stone, purpur crown, an enderman's face, an ender pearl (2026-11-22) -------------------
END_PAL = {**shades((40, 38, 24), (110, 108, 70), (150, 148, 100), (190, 188, 136), (214, 212, 160), (228, 228, 176), (244, 244, 200)),
	"p": (122, 74, 122), "q": (170, 124, 170), "k": (18, 10, 24), "m": (204, 0, 250), "M": (250, 150, 255),
	"t": (18, 80, 72), "T": (40, 160, 140), "w": (180, 250, 230)}
END = check([
	"................",
	"....p.pqqp.p....",
	"....pqqqqqqp....",
	"....okkkkkko....",
	"...okkkkkkkko...",
	"...okmMkkmMko...",
	"...okkkkkkkko...",
	"....o443321o....",
	".oooo5tTTt1oooo.",
	"o56655TwTt22221o",
	"o54544tTTt32211o",
	".oooo4tttt1oooo.",
	"...o54433221o...",
	"....o543321o....",
	"...o65544321o...",
	"...oooooooooo...",
])
END_MAT = {**{k: "stone" for k in "123456"}, **{k: "purpur" for k in "pq"}, "k": "face", "m": "eye", "M": "eye", **{k: "pearl" for k in "tTw"}}

# --- Herding: spruce wood, a woolly ram's head with curled horns, a gold bell on the chest (2026-11-22) ------
HERD_PAL = {**shades((30, 20, 10), (58, 40, 22), (82, 58, 32), (106, 76, 44), (130, 96, 58), (156, 118, 74), (184, 144, 96)),
	"W": (236, 234, 224), "w": (196, 192, 180), "h": (120, 100, 76), "H": (168, 146, 114), "e": (26, 18, 12),
	"a": (122, 76, 18), "b": (200, 146, 42), "B": (240, 202, 84), "c": (255, 244, 176)}
HERDING = check([
	"................",
	"................",
	"...hH.WWWW.Hh...",
	"..h..WWwwWW..h..",
	"..H.oWWWWWwo.H..",
	"...ho54e4e2oh...",
	"....o544332o....",
	"....o443321o....",
	".oooo5bBBb1oooo.",
	"o56655bBcb22221o",
	"o54544bbbb32211o",
	".oooo4abba1oooo.",
	"...o54433221o...",
	"....o543321o....",
	"...o65544321o...",
	"...oooooooooo...",
])
HERD_MAT = {**{k: "wood" for k in "123456"}, **{k: "wool" for k in "Ww"}, **{k: "horn" for k in "hH"}, "e": "eye",
	**{k: "gold" for k in "abBc"}}

# --- Clear Skies: pale calcite, a gold sun crown of rays, a patch of blue sky on the chest (2026-11-22) -------
SKY_PAL = {**shades((58, 60, 72), (124, 126, 138), (156, 158, 170), (186, 188, 198), (210, 212, 220), (230, 232, 238), (250, 250, 252)),
	"y": (226, 160, 34), "Y": (255, 222, 96), "k": (56, 116, 200), "s": (74, 146, 226), "S": (146, 204, 250)}
CLEAR_SKIES = check([
	"......y..y......",
	"..y..yYYYYy..y..",
	"...yYooooooYy...",
	"....o566543o....",
	"...o56654332o...",
	"...o54k44k22o...",
	"...o54433322o...",
	"....o443321o....",
	".oooo5sSss1oooo.",
	"o56655sYYs22221o",
	"o54544sYYs32211o",
	".oooo4SssS1oooo.",
	"...o54433221o...",
	"....o543321o....",
	"...o65544321o...",
	"...oooooooooo...",
])
SKY_MAT = {**{k: "stone" for k in "123456"}, **{k: "gold" for k in "yY"}, "k": "eye", **{k: "sky" for k in "sS"}}

# --- North Wind: packed-ice blue, an icicle crown, puffed cheeks blowing a gust, a snowflake on the chest (2026-11-23) --
NORTH_PAL = {**shades((26, 44, 76), (56, 92, 138), (86, 128, 176), (120, 164, 208), (156, 196, 232), (194, 222, 246), (232, 244, 255)),
	"i": (150, 200, 236), "I": (236, 248, 255), "k": (24, 62, 128), "m": (44, 66, 104),
	"w": (196, 228, 250), "W": (255, 255, 255), "b": (40, 86, 156), "f": (186, 224, 252), "F": (255, 255, 255)}
NORTH_WIND = check([
	"................",
	"....I..I..I.....",
	"....ioIiiIoi....",
	"....o566543o....",
	"...o56654332o.ww",
	"...o5k44k332o...",
	"...o5443355moWWw",
	"....o443321o....",
	".oooo5FbbF1oooo.",
	"o56655bffb22221o",
	"o54544bffb32211o",
	".oooo4FbbF1oooo.",
	"...o54433221o...",
	"....o543321o....",
	"...o65544321o...",
	"...oooooooooo...",
])
NORTH_MAT = {**{k: "ice" for k in "123456"}, **{k: "icicle" for k in "iI"}, "k": "eye", "m": "mouth",
	**{k: "gust" for k in "wW"}, **{k: "flake" for k in "bfF"}}


def dial_centre(grid):
	"""Centre of the dial face (the 'y' pixels), in 16x pixel units (edges, so 8.0 is between pixels 7 and 8)."""
	pts = [(x, y) for y, row in enumerate(grid) for x, ch in enumerate(row) if ch == "y"]
	return (min(p[0] for p in pts) + max(p[0] for p in pts) + 1) / 2.0, (min(p[1] for p in pts) + max(p[1] for p in pts) + 1) / 2.0


def draw_needle(img, scale, centre, frame):
	"""Needle turned frame/16 clockwise from up: red tip toward the target, dark tail behind, at this image's own scale."""
	px = img.load()
	cx, cy = centre[0] * scale, centre[1] * scale
	angle = 2 * math.pi * frame / 16
	dx, dy = math.sin(angle), -math.cos(angle)

	def put(t, colour):
		x = int(math.floor(cx + dx * t))
		y = int(math.floor(cy + dy * t))
		if SEEKING[y // scale][x // scale] == "y":  # on the dial face only
			px[x, y] = rgba(colour)

	# Tail (half length) first, then the red tip from the centre out; many samples so no pixel on the line is skipped.
	reach = 2.0 * scale
	for i in range(1, 40):
		put(-reach * 0.5 * i / 40, NEEDLE_DARK)
	for i in range(0, 60):
		put(reach * i / 60, NEEDLE_RED)
	return img


def write(name, img16, img32):
	for n, img in ((16, img16), (32, img32)):
		OUT[n].mkdir(parents=True, exist_ok=True)
		img.save(OUT[n] / (name + ".png"))
	print("wrote", name)


def main():
	plain = [
		("totem_of_keeping", KEEPING, KEEPING_PAL, KEEPING_MAT, ("stone",), 11),
		("totem_of_gathering", GATHERING, GATHERING_PAL, GATHERING_MAT, ("metal",), 12),
		("totem_of_fortification", FORTIFICATION, FORT_PAL, FORT_MAT, ("metal",), 13),
		("totem_of_growth", GROWTH, GROWTH_PAL, GROWTH_MAT, ("wood", "leaf"), 14),
		("totem_of_the_beginning", BEGINNING, BEGIN_PAL, BEGIN_MAT, ("stone", "grass"), 16),
		("totem_of_the_end", END, END_PAL, END_MAT, ("stone",), 17),
		("totem_of_herding", HERDING, HERD_PAL, HERD_MAT, ("wood", "wool"), 18),
		("totem_of_clear_skies", CLEAR_SKIES, SKY_PAL, SKY_MAT, ("stone",), 19),
		("totem_of_north_wind", NORTH_WIND, NORTH_PAL, NORTH_MAT, ("ice",), 20),
	]
	for name, grid, pal, mat, grain, seed in plain:
		write(name, render(grid, pal), render32(grid, pal, mat, inner={"o": "1"}, grain=grain, seed=seed))
	base16 = render(SEEKING, SEEK_PAL)
	base32 = render32(SEEKING, SEEK_PAL, SEEK_MAT, inner={"o": "1"}, grain=("stone",), seed=15)
	centre = dial_centre(SEEKING)
	for frame in range(16):
		write("totem_of_seeking_%02d" % frame, draw_needle(base16.copy(), 1, centre, frame), draw_needle(base32.copy(), 2, centre, frame))


if __name__ == "__main__":
	main()
