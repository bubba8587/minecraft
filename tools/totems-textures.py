#!/usr/bin/env python3
# dte:B16,C6
"""Item textures for the totems of Bubba's Totems & Artifacts other than Warding (tools/warding-textures.py), 16x and
their Faithful-style 32x twins (specs/textures.md). Agent-made, listed in docs/art-todo.md for the owner to redo or
tweak; never AI-generated.

  python tools/totems-textures.py

Each is a small carved idol in the family of vanilla's Totem of Undying: head, arms out, body, a dark belt with a gem
and the Undying's tapered foot, cast in its own precious material with gem eyes (X light, Z dark), plus the head and
chest emblem of its power.

  Keeping        crying obsidian, magenta gem eyes, clutching a small gold-bound chest
  Gathering      polished copper, arms ending in upturned hands, an oxidised-copper ring on the chest, verdigris eyes
  Fortification  netherite, a gold-banded helm with a glowing visor slit, a gold shield with a lapis field
  Growth         resin amber, a leafy crown with a pink blossom, emerald eyes, a green sprout on the chest
  Beginning      emerald wearing a grass cap, amethyst eyes, a grass block (grass, dirt) on the chest
  End            amethyst, a pale end-stone crown, an enderman's black face and magenta eyes, an ender pearl
  Herding        ruby-red redstone, a woolly ram's head with ivory horns, gold eyes, a gold bell
  Clear Skies    quartz, a gold sun crown of rays, sky-blue gem eyes, blue sky with the sun on the chest
  North Wind     diamond, an icicle crown, cheeks puffed blowing a gust to the side, deep-blue eyes, a snowflake
  Seeking        lapis, a gold-rimmed hood, gold eyes, a compass dial on the chest; 16 needle frames
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


# Gem eyes and belt gems use X (the gem's light) and Z (its dark), like the Totem of Undying's emerald eyes.
def gem(light, dark):
	return {"X": light, "Z": dark}


GEM_MAT = {"X": "gem", "Z": "gem"}

# --- Keeping: crying obsidian, magenta gem eyes, clutching a small gold-bound chest -----------------
KEEPING_PAL = {**shades((12, 6, 22), (32, 16, 54), (50, 28, 82), (70, 42, 110), (94, 60, 144), (124, 86, 180), (164, 124, 216)),
	"a": (122, 76, 18), "b": (200, 146, 42), "c": (240, 202, 84), "d": (255, 244, 176), **gem((238, 132, 255), (150, 40, 214))}
KEEPING = check([
	"................",
	"................",
	".....oooooo.....",
	"....o566654o....",
	"...o56544443o...",
	"...o56X446X2o...",
	"...o5XZ54XZ2o...",
	"....o443321o....",
	".oooo5abba1oooo.",
	"o56655bccb22221o",
	"o54544adda32211o",
	".oooo4aaaa1oooo.",
	"....o11XZ11o....",
	".....o5432o.....",
	".....o4321o.....",
	"......oooo......",
])
KEEPING_MAT = {**{k: "stone" for k in "123456"}, **{k: "gold" for k in "abcd"}, **GEM_MAT}

# --- Gathering: polished copper, oxidised-copper ring and gem eyes ------------------------------------
GATHERING_PAL = {**shades((70, 26, 12), (122, 50, 26), (166, 74, 40), (204, 102, 58), (230, 134, 84), (246, 170, 118), (255, 212, 166)),
	"p": (40, 120, 98), "q": (78, 182, 148), "e": (170, 250, 220), **gem((120, 230, 190), (30, 118, 96))}
GATHERING = check([
	"................",
	"................",
	".....oooooo.....",
	"....o566543o....",
	"...o56654332o...",
	"...o56X436X2o...",
	"...o5XZ43XZ2o...",
	"oo..o443321o..oo",
	"o5ooo543321ooo2o",
	"o56655qqqq32221o",
	"o5454qpeepq2211o",
	".oooo4qqqq1oooo.",
	"....o11XZ11o....",
	".....o5432o.....",
	".....o4321o.....",
	"......oooo......",
])
GATHERING_MAT = {**{k: "metal" for k in "123456"}, **{k: "patina" for k in "pqe"}, **GEM_MAT}

# --- Fortification: netherite, a gold-banded helm with a glowing visor slit, a gold shield with a lapis field ---
FORT_PAL = {**shades((24, 20, 22), (52, 45, 47), (74, 66, 67), (96, 87, 87), (120, 110, 109), (148, 138, 135), (182, 172, 166)),
	"f": (240, 120, 30), "g": (255, 214, 110), "s": (30, 52, 140), "t": (200, 146, 42), "u": (250, 214, 100)}
FORTIFICATION = check([
	"................",
	"...oooooooooo...",
	"...o66655443o...",
	"...otuuuuuuto...",
	"...o1gf11fg1o...",
	"...o54433221o...",
	"...o44332211o...",
	"....oo4321oo....",
	".oooo5tuut1oooo.",
	"o56655tsst22221o",
	"o54544tsst32211o",
	".oooo43ts31oooo.",
	"....o54tt21o....",
	".....o5432o.....",
	".....o4321o.....",
	"......oooo......",
])
FORT_MAT = {**{k: "metal" for k in "123456"}, **{k: "glow" for k in "fg"}, **{k: "shield" for k in "stu"}}

# --- Growth: resin amber, a leafy crown with a pink blossom, emerald eyes, a sprout on the chest --------
GROWTH_PAL = {**shades((72, 22, 6), (120, 42, 10), (164, 66, 14), (206, 98, 22), (232, 132, 36), (248, 170, 62), (255, 212, 124)),
	"l": (30, 84, 22), "m": (58, 134, 38), "n": (112, 190, 62), "k": (246, 136, 178), "y": (255, 226, 120),
	**gem((65, 230, 127), (0, 150, 60))}
GROWTH = check([
	"......lmml......",
	"....lmnnmnl.....",
	"...lmnkynmml....",
	"...olmmllmmlo...",
	"...o56544332o...",
	"...o56X436X2o...",
	"...o5XZ43XZ2o...",
	"....o443321o....",
	".oooo54nm21oooo.",
	"o56655nmlm22221o",
	"o545443ln322211o",
	".oooo43l331oooo.",
	"....o11XZ11o....",
	".....o5432o.....",
	".....o4321o.....",
	"......oooo......",
])
GROWTH_MAT = {**{k: "resin" for k in "123456"}, **{k: "leaf" for k in "lmn"}, "k": "flower", "y": "flower", **GEM_MAT}

# --- Seeking: lapis, gold, gold gem eyes, a compass dial ---------------------------------------------
SEEK_PAL = {**shades((10, 16, 48), (22, 36, 96), (32, 54, 134), (46, 78, 172), (68, 108, 204), (106, 144, 228), (160, 188, 246)),
	"a": (130, 86, 16), "b": (210, 156, 48), "c": (250, 214, 100), "w": (150, 150, 158), "y": (226, 222, 210)}
SEEKING = check([
	"................",
	"................",
	".....oooooo.....",
	"....o566543o....",
	"...o5bbbbbb2o...",
	"...o5cb11cb2o...",
	"...o5ba11ba2o...",
	"....o443321o....",
	".oooo5abba1oooo.",
	"o5665ayyyyb2221o",
	"o5454ayyyyb2211o",
	".ooooayyyyboooo.",
	"....oayyyybo....",
	".....oabbbo.....",
	".....o4321o.....",
	"......oooo......",
])
SEEK_MAT = {**{k: "stone" for k in "123456"}, **{k: "gold" for k in "abc"}, "y": "dial", "w": "dial"}
NEEDLE_RED = (214, 36, 36)
NEEDLE_DARK = (70, 70, 78)

# --- Beginning: emerald, a grass cap, amethyst gem eyes, a grass block (grass, dirt) on the chest -----
BEGIN_PAL = {**shades((4, 44, 24), (8, 80, 40), (12, 114, 56), (20, 150, 74), (34, 184, 96), (84, 220, 140), (176, 248, 206)),
	"g": (60, 96, 22), "h": (104, 146, 40), "H": (150, 196, 70), "d": (100, 68, 40), "D": (138, 98, 60),
	**gem((206, 150, 255), (120, 60, 190))}
BEGINNING = check([
	"................",
	"......hHHh......",
	"....ghHhhHhg....",
	"...oghgghgggo...",
	"...o5g544g43o...",
	"...o56X436X2o...",
	"...o5XZ43XZ2o...",
	"....o443321o....",
	".oooo5HhhH1oooo.",
	"o56655hggh22221o",
	"o54544dDDd32211o",
	".oooo4dDdd1oooo.",
	"....o11XZ11o....",
	".....o5432o.....",
	".....o4321o.....",
	"......oooo......",
])
BEGIN_MAT = {**{k: "stone" for k in "123456"}, **{k: "grass" for k in "ghH"}, **{k: "dirt" for k in "dD"}, **GEM_MAT}

# --- End: amethyst, a pale end-stone crown, an enderman's black face and magenta eyes, an ender pearl ---
END_PAL = {**shades((34, 16, 60), (64, 36, 108), (94, 60, 148), (126, 88, 188), (156, 118, 216), (188, 154, 236), (224, 202, 254)),
	"p": (186, 182, 120), "q": (238, 236, 184), "k": (18, 10, 24), "m": (204, 0, 250), "M": (250, 150, 255),
	"t": (18, 80, 72), "T": (40, 160, 140), "w": (180, 250, 230), **gem((60, 190, 160), (16, 90, 80))}
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
	"....o11XZ11o....",
	".....o5432o.....",
	".....o4321o.....",
	"......oooo......",
])
END_MAT = {**{k: "stone" for k in "123456"}, **{k: "crown" for k in "pq"}, "k": "face", "m": "eye", "M": "eye",
	**{k: "pearl" for k in "tTw"}, **GEM_MAT}

# --- Herding: ruby-red redstone, a woolly ram's head with ivory horns, gold eyes, a gold bell ---------------
HERD_PAL = {**shades((46, 6, 10), (90, 12, 18), (132, 20, 26), (172, 32, 36), (208, 56, 54), (234, 98, 92), (252, 160, 150)),
	"W": (240, 238, 228), "w": (200, 196, 184), "h": (150, 128, 96), "H": (214, 198, 160),
	"a": (122, 76, 18), "b": (200, 146, 42), "B": (240, 202, 84), "c": (255, 244, 176), **gem((250, 214, 100), (170, 110, 24))}
HERDING = check([
	"................",
	"................",
	"...hH.WWWW.Hh...",
	"..hHHWWwwWWHHh..",
	"..hHoWWWWWwoHh..",
	"...ho5XZ4XZoh...",
	"....o544332o....",
	"....o443321o....",
	".oooo5bBBb1oooo.",
	"o56655bBcb22221o",
	"o54544bbbb32211o",
	".oooo4abba1oooo.",
	"....o11XZ11o....",
	".....o5432o.....",
	".....o4321o.....",
	"......oooo......",
])
HERD_MAT = {**{k: "ruby" for k in "123456"}, **{k: "wool" for k in "Ww"}, **{k: "horn" for k in "hH"},
	**{k: "gold" for k in "abBc"}, **GEM_MAT}

# --- Clear Skies: quartz, a gold sun crown of rays, sky-blue gem eyes, a patch of blue sky with the sun ----
SKY_PAL = {**shades((96, 78, 74), (150, 128, 120), (186, 170, 162), (212, 200, 192), (230, 224, 216), (243, 240, 234), (253, 252, 249)),
	"y": (226, 160, 34), "Y": (255, 222, 96), "k": (56, 116, 200), "s": (74, 146, 226), "S": (146, 204, 250),
	**gem((110, 190, 255), (30, 86, 190))}
CLEAR_SKIES = check([
	"......y..y......",
	"..y..yYYYYy..y..",
	"...yYooooooYy...",
	"....o566543o....",
	"...o56654332o...",
	"...o56X446X2o...",
	"...o5XZ54XZ2o...",
	"....o443321o....",
	".oooo5sSss1oooo.",
	"o56655sYYs22221o",
	"o54544sYYs32211o",
	".oooo4SssS1oooo.",
	"....o11yY11o....",
	".....o5432o.....",
	".....o4321o.....",
	"......oooo......",
])
SKY_MAT = {**{k: "quartz" for k in "123456"}, **{k: "gold" for k in "yY"}, **{k: "sky" for k in "sS"}, **GEM_MAT}

# --- North Wind: diamond, an icicle crown, puffed cheeks blowing a gust, deep-blue gem eyes, a snowflake ----
NORTH_PAL = {**shades((8, 56, 66), (20, 100, 112), (36, 146, 156), (68, 190, 196), (116, 222, 222), (172, 244, 238), (232, 255, 252)),
	"i": (176, 214, 240), "I": (240, 250, 255), "k": (24, 62, 128), "m": (16, 74, 84),
	"w": (196, 228, 250), "W": (255, 255, 255), "b": (30, 70, 150), "f": (186, 224, 252), "F": (255, 255, 255),
	**gem((64, 110, 220), (22, 44, 130))}
NORTH_WIND = check([
	"................",
	"....I..I..I.....",
	"....ioIiiIoi....",
	"....o566543o....",
	"...o56654332o.ww",
	"...o5XZ4XZ32o...",
	"...o5443355moWWw",
	"....o443321o....",
	".oooo5FbbF1oooo.",
	"o56655bffb22221o",
	"o54544bffb32211o",
	".oooo4FbbF1oooo.",
	"....o11XZ11o....",
	".....o5432o.....",
	".....o4321o.....",
	"......oooo......",
])
NORTH_MAT = {**{k: "ice" for k in "123456"}, **{k: "icicle" for k in "iI"}, "m": "mouth",
	**{k: "gust" for k in "wW"}, **{k: "flake" for k in "bfF"}, **GEM_MAT}


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
		("totem_of_gathering", GATHERING, GATHERING_PAL, GATHERING_MAT, (), 12),
		("totem_of_fortification", FORTIFICATION, FORT_PAL, FORT_MAT, ("metal",), 13),
		("totem_of_growth", GROWTH, GROWTH_PAL, GROWTH_MAT, ("leaf",), 14),
		("totem_of_the_beginning", BEGINNING, BEGIN_PAL, BEGIN_MAT, ("grass",), 16),
		("totem_of_the_end", END, END_PAL, END_MAT, (), 17),
		("totem_of_herding", HERDING, HERD_PAL, HERD_MAT, ("wool",), 18),
		("totem_of_clear_skies", CLEAR_SKIES, SKY_PAL, SKY_MAT, (), 19),
		("totem_of_north_wind", NORTH_WIND, NORTH_PAL, NORTH_MAT, (), 20),
	]
	for name, grid, pal, mat, grain, seed in plain:
		write(name, render(grid, pal), render32(grid, pal, mat, inner={"o": "1"}, bevel=0.3, seed=seed, snap=True))
	base16 = render(SEEKING, SEEK_PAL)
	base32 = render32(SEEKING, SEEK_PAL, SEEK_MAT, inner={"o": "1"}, bevel=0.3, seed=15, snap=True)
	centre = dial_centre(SEEKING)
	for frame in range(16):
		write("totem_of_seeking_%02d" % frame, draw_needle(base16.copy(), 1, centre, frame), draw_needle(base32.copy(), 2, centre, frame))


if __name__ == "__main__":
	main()
