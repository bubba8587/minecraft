# dte:C1,B16
"""Eye of the Soul art (specs/soulfire.md "Eye of the Soul: phasing"): the item icon and the Phasing effect icon.
Item: vanilla's Eye of Ender (16x) and Classic Faithful 32x Jappa's (32x, CF32J=<zip or folder>) recoloured to soul
colours, with a hand-placed white soulfire flame over the centre at each size. Phasing: hand-placed grids.

Usage: python3 make_eye_of_the_soul_textures.py [preview_dir]
(with a preview dir, also writes zoomed previews beside the vanilla references and on the HUD effect frame). Needs Pillow.
"""
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import make_captain_textures as mc  # noqa: E402
import make_soulfire_textures as sf  # noqa: E402

# --- palette -----------------------------------------------------------------------------------------------------

PALETTE = {
	"T": "00305b",  # wisp outline
	"K": "5dfaff",  # glow
	"f": "5dfaff",  # faint ghost fill (translucent, ALPHA)
}
ALPHA = {"f": 48}

# --- item: Eye of the Soul (vanilla / CF32J Eye of Ender, soul colours, white soulfire centre) ---------------------

EYE_RECOLOUR = {  # Eye of Ender colour -> soul colour
	"102c31": "0c1a33",  # outline darkest
	"1b2b10": "0c1a33",  # pupil
	"1e4835": "0f2c4a",  # outline / shadow
	"316364": "1b4a66",  # outline mid
	"407476": "246379",  # shell shadow (CF32J)
	"4e8386": "2c7a8c",  # shell
	"659b7d": "3b9aa6",  # shell light
	"cbfcdd": "86d6dc",  # shell highlight
	"579143": "014a6e",  # iris dark
	"71ac49": "026f92",  # iris
	"88c35a": "0a8aa6",  # iris bright
	"97d16b": "06a3b9",  # iris brighter
	"b5e45a": "11cad4",  # iris glow
}

FLAME = {  # centre overlay: soul fire ramp, white-hot core
	"O": "0c1a33",  # soot dark around the flame
	"T": "00305b",  # flame outline
	"J": "015b7f",  # flame edge dark
	"I": "06a3b9",  # flame edge
	"C": "11cad4",  # pale cyan edge
	"K": "8ff6ff",  # pale cyan
	"W": "dcffff",  # white-hot
	"X": "ffffff",  # white core
}

FLAME_16 = [  # over the iris; "." keeps the recoloured eye
	"................",
	"................",
	"................",
	"................",
	"................",
	"........K.......",
	".......TKT.T....",
	"......TCWKTK....",
	".....TCWXWCT....",
	"....TCKXXXKT....",
	".....TKXXXKT....",
	"......TCWCT.....",
	".......TTT......",
	"................",
	"................",
	"................",
]

FLAME_32 = [
	"................................",
	"................................",
	"................................",
	"................................",
	"................................",
	"................................",
	"................................",
	"................................",
	"................T...............",
	"...............TKT..............",
	"...............TWKT.............",
	"...........T...TWXKT.T..........",
	"..........TKT.TCWXKTTKT.........",
	"..........TKCTCKWXWKCKT.........",
	"...........TKCKWXXWKCKT.........",
	"...........TCKWXXXXWKCT.........",
	"...........TCKWXXXXXWKT.........",
	"...........TCKWXXXXXWKT.........",
	"...........TCKWXXXXXWKT.........",
	"............TCKWXXXWKCT.........",
	".............TCKWWWKCT..........",
	"..............TTCCCTT...........",
	"................................",
	"................................",
	"................................",
	"................................",
	"................................",
	"................................",
	"................................",
	"................................",
	"................................",
	"................................",
]


def soul_eye(src, flame):
	"""The Eye of Ender recoloured to soul colours, with the hand-placed white soulfire drawn over its centre."""
	size = src.width
	assert len(flame) == size and all(len(r) == size for r in flame)
	img = Image.new("RGBA", src.size, (0, 0, 0, 0))
	for y in range(size):
		for x in range(size):
			c = src.getpixel((x, y))
			if c[3] == 0:
				continue
			ch = flame[y][x]
			hexc = FLAME[ch] if ch != "." else EYE_RECOLOUR["%02x%02x%02x" % c[:3]]
			img.putpixel((x, y), mc.hx(hexc) + (255,))
	for y, row in enumerate(flame):
		for x, ch in enumerate(row):
			if ch != "." and src.getpixel((x, y))[3] == 0:
				sys.exit(f"flame pixel outside the eye at {x},{y}")
	return img


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
	sys.exit("vanilla textures not found")


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
	ender = Image.open(vanilla() / "item/ender_eye.png").convert("RGBA")
	eye16 = soul_eye(ender, FLAME_16)
	mc.save(eye16, "item/eye_of_the_soul.png")
	cf = sf.cf32j_open("assets/minecraft/textures/item/ender_eye.png")
	if cf is not None:
		eye32 = soul_eye(cf, FLAME_32)
		mc.save32(eye32, "item/eye_of_the_soul.png")
	else:
		print("CF32J not found (set CF32J=<zip or folder>); keeping the committed 32x Eye of the Soul")
		eye32 = Image.open(mc.OUT32 / "item/eye_of_the_soul.png").convert("RGBA")
	if len(sys.argv) > 1:
		d = Path(sys.argv[1])
		d.mkdir(parents=True, exist_ok=True)
		cfe = cf if cf is not None else eye32
		sheet([(ender, 16), (eye16, 16), (cfe, 8), (eye32, 8)], d / "eye-of-the-soul-zoom.png")
		sheet([(ender, 2), (eye16, 2), (cfe, 1), (eye32, 1), (ender, 1), (eye16, 1)], d / "eye-of-the-soul-small.png")
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
