# dte:C1,B16
"""Eye of the Soul art (specs/soulfire.md "Eye of the Soul: phasing"): the item icon and the Phasing effect icon.
Both sizes are hand-placed character grids (the 32x is drawn on its own, not scaled); soul colours from the Soulfire
art (make_soulfire_textures.FLAME_RAMP).

Usage: python3 make_eye_of_the_soul_textures.py [preview_dir]
(with a preview dir, also writes zoomed previews beside the vanilla references and on the HUD effect frame). Needs Pillow.
"""
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import make_captain_textures as mc  # noqa: E402

# --- palette -----------------------------------------------------------------------------------------------------

PALETTE = {
	"O": "0f1934",  # outline (soul flame outline)
	"D": "1a2a4e",  # shell dark
	"M": "26426c",  # shell mid
	"L": "355f8a",  # shell light
	"H": "5a8db2",  # shell rim highlight
	"P": "070d1e",  # pupil
	"T": "00305b",  # wisp outline
	"J": "015b7f",  # iris rim / wisp dark
	"I": "06a3b9",  # iris
	"C": "11cad4",  # iris bright
	"K": "5dfaff",  # glow
	"W": "c8feff",  # white-hot
	"f": "5dfaff",  # faint ghost fill (translucent, ALPHA)
}
ALPHA = {"f": 48}

# --- item: Eye of the Soul ---------------------------------------------------------------------------------------

EYE_16 = [
	"................",
	"........OOOOO...",
	"......OOHHLMMOO.",
	".....OHLLJJJLMDO",
	".....OLLJIIIJMDO",
	".....OLJIWCCCJDO",
	".....OMJCCPPCJDO",
	".T..TOMJICPPKJDO",
	".TJTJCDMJKKKJDDO",
	"..TJICKDMJJJDDDO",
	"..TICKWKODDDDOO.",
	".TJICKCJTOOOO...",
	"TJICIKJT........",
	"TICJTCJT........",
	"TJT.TJT.........",
	".T...T..........",
]

EYE_32 = [
	"................................",
	"................................",
	"..................OOOOOOO.......",
	"................OOHHHHHLLOO.....",
	"...............OHHLHLLLLLMMO....",
	"..............OHLLLLLLLLMMMMO...",
	".............OHLLLLLLLLMMMMMMO..",
	"............OHLLLLLJJJJJMMMMDDO.",
	"............OHLLLLJIIIIIJMMDDDO.",
	"...........OHLLLLJIWWCCIIJDDDDDO",
	"...........OHLLLJIIWWPPCIIJDDDDO",
	"...........OHLLLJICPPPPPCIJDDDDO",
	"...........OLLLMJICPPPPPCIJDDDDO",
	"...T.......OLMMMJICPPPPPCIJDDDDO",
	"..TJT......OMMMMJKKCPPPCKKJDDDDO",
	"..TIJT.....CMMMMMJKKCCCKKJDDDDMO",
	"..TCIJT..TCKKMMMDDJKKKKKJDDDDMO.",
	"...TCKCJTTCKWMMDDDDJJJJJDDDDDMO.",
	"...TCKKCJCKKWKDDDDDDMDDDDDDDMO..",
	"...TICKKKWWWWWKDDDDDDDDDDDDMO...",
	"..TJICKKWWWWWKKKDDDDDDDDDDMO....",
	"..TJICKKKWWWKKCKCKDDDDDDDOO.....",
	".TJICCKKKKKKKCCIJTOOOOOOO.......",
	".TJIICCKKKKKCCIJT...............",
	"TJIICCCCKKCCIJJT................",
	"TJIICCCCCIICIJT.................",
	"TJICCCCIIJICCJT.................",
	"TJICCIIJTJCCJT..................",
	"TJICIIJT.TICJT..................",
	"TJIIJT...TIJT...................",
	"TJJT.....TJT....................",
	".TT.......TT....................",
]


# --- mob effect: Phasing (a dashed, ghostly player outline) -----------------------------------------------------
# K dash, T dark backing outside each dash, f faint translucent inside. The 32x has its own dash rhythm.

PHASING_16 = [
	"......TT..TT......",
	".....TKKffKKT.....",
	".....TKffffKT.....",
	"......ffffff......",
	".....TKffffKT.....",
	"...TTTKffffKTTT...",
	"..TKKfKffffKfKKT..",
	"..TKffffffffffKT..",
	"...ffffffffffff...",
	"..TKffffffffffKT..",
	"..TKfKffffffKfKT..",
	"...TTKffffffKTT...",
	".....ffKKKKff.....",
	"....TKff..ffKT....",
	"....TKfK..KfKT....",
	".....ffK..Kff.....",
	"....TKKK..KKKT....",
	".....TTT..TTT.....",
]

PHASING_32 = [
	"....................................",
	"............TTT..TT..TTT............",
	"...........TKKKffKKffKKKT...........",
	"...........TKffffffffffKT...........",
	"............ffffffffffff............",
	"............ffffffffffff............",
	"...........TKffffffffffKT...........",
	"...........TKffffffffffKT...........",
	"............ffffffffffff............",
	"............ffffffffffff............",
	"...........TKffffffffffKT...........",
	"......TT..TTKffffffffffKTT..TT......",
	".....TKKffKKffffffffffffKKffKKT.....",
	".....TKffffffffffffffffffffffKT.....",
	"......ffffffffffffffffffffffff......",
	"......ffffffffffffffffffffffff......",
	".....TKffffffffffffffffffffffKT.....",
	".....TKffffffffffffffffffffffKT.....",
	"......ffffffffffffffffffffffff......",
	"......ffffffffffffffffffffffff......",
	".....TKffffffffffffffffffffffKT.....",
	".....TKKfKffffffffffffffffKfKKT.....",
	"......TT.TKffffffffffffffKT.TT......",
	"..........ffffffKKKKffffff..........",
	".........TKffffKTTTTKffffKT.........",
	".........TKffffKT..TKffffKT.........",
	"..........ffffff....ffffff..........",
	"..........ffffff....ffffff..........",
	".........TKffffKT..TKffffKT.........",
	".........TKffffKT..TKffffKT.........",
	"..........ffffff....ffffff..........",
	"..........ffffff....ffffff..........",
	".........TKffffKT..TKffffKT.........",
	".........TKKffKKT..TKKffKKT.........",
	"..........TT..TT....TT..TT..........",
	"....................................",
]


def draw(grid, size):
	assert len(grid) == size and all(len(r) == size for r in grid), [len(r) for r in grid]
	img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
	for y, row in enumerate(grid):
		for x, ch in enumerate(row):
			if ch != ".":
				img.putpixel((x, y), mc.hx(PALETTE[ch]) + (ALPHA.get(ch, 255),))
	return img


def vanilla():
	for base in (mc.ROOT, Path("/home/user/minecraft")):
		hits = sorted((base / "build/mcsrc").glob("*/vanilla/assets/minecraft/textures"))
		if hits:
			return hits[-1]
	sys.exit("vanilla textures not found (needed for previews only)")


def sheet(pairs, out):
	"""Previews side by side: each (image, zoom) drawn nearest-neighbour at its zoom, on an inventory-grey card."""
	tiles = [img.resize((img.width * z, img.height * z), Image.NEAREST) for img, z in pairs]
	w = sum(t.width for t in tiles) + 16 * (len(tiles) + 1)
	h = max(t.height for t in tiles) + 32
	card = Image.new("RGBA", (w, h), (139, 139, 139, 255))
	x = 16
	for t in tiles:
		card.alpha_composite(t, (x, 16))
		x += t.width + 16
	card.save(out)


def main():
	eye16, eye32 = draw(EYE_16, 16), draw(EYE_32, 32)
	mc.save(eye16, "item/eye_of_the_soul.png")
	mc.save32(eye32, "item/eye_of_the_soul.png")
	if len(sys.argv) > 1:
		d = Path(sys.argv[1])
		d.mkdir(parents=True, exist_ok=True)
		ender = Image.open(vanilla() / "item/ender_eye.png").convert("RGBA")
		sheet([(ender, 16), (eye16, 16), (eye32, 8)], d / "eye-of-the-soul-zoom.png")
		sheet([(ender, 2), (eye16, 2), (eye32, 1), (ender, 1), (eye16, 1)], d / "eye-of-the-soul-small.png")
	ph16, ph32 = draw(PHASING_16, 18), draw(PHASING_32, 36)
	mc.save(ph16, "mob_effect/phasing.png")
	mc.save32(ph32, "mob_effect/phasing.png")
	if len(sys.argv) > 1:
		v = vanilla()
		refs = [Image.open(v / f"mob_effect/{n}.png").convert("RGBA") for n in ("invisibility", "darkness")]
		sheet([(refs[0], 16), (ph16, 16), (ph32, 8)], d / "phasing-zoom.png")
		bg = Image.open(v / "gui/sprites/hud/effect_background.png").convert("RGBA")
		tiles = []
		for icon in (refs[0], refs[1], ph16, ph32):
			t = bg.copy()
			small = icon if icon.width == 18 else icon.resize((18, 18), Image.BOX)
			t.alpha_composite(small, ((t.width - 18) // 2, (t.height - 18) // 2))
			tiles.append((t, 4))
		big = Image.new("RGBA", (bg.width * 2, bg.height * 2))
		big.alpha_composite(bg.resize(big.size, Image.NEAREST))
		big.alpha_composite(ph32, ((big.width - 36) // 2, (big.height - 36) // 2))
		tiles.append((big, 2))
		sheet(tiles, d / "phasing-hud.png")


if __name__ == "__main__":
	main()
