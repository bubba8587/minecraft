# dte:C1,B16
"""Wither Captain art (specs/wither-captain.md): Soulfire Blaze, Nether Star Shard, Captain's Helm (icon, worn layer,
horns). 16x textures are palette swaps of vanilla art or hand-placed grids; 32x twins in Classic Faithful 32x style.

Usage: python3 make_captain_textures.py [vanilla_textures_dir]
(default: build/mcsrc/<version>/vanilla/assets/minecraft/textures of the main checkout). Needs Pillow.
"""
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools"))
import pixelart as pa  # noqa: E402

MOD = "bubbas_reworked_mobs"
RES = Path(__file__).resolve().parent.parent / "src/main/resources"
OUT16 = RES / f"assets/{MOD}/textures"
OUT32 = RES / f"resourcepacks/{MOD}_32x/assets/{MOD}/textures"


def vanilla_dir():
	if len(sys.argv) > 1:
		return Path(sys.argv[1])
	for base in (ROOT, Path("/home/user/minecraft")):
		hits = sorted((base / "build/mcsrc").glob("*/vanilla/assets/minecraft/textures"))
		if hits:
			return hits[-1]
	sys.exit("vanilla textures not found; pass the path")


def hx(s):
	return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def lum(c):
	return 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]


# --- palette swaps -------------------------------------------------------------------------------------------------

# Vanilla blaze ramp (dark to light) -> soul ramp: same OKLCh lightness per step, hue sweeping from the soul
# lantern's deep blue-violet (h 267) to soul fire cyan (h 198), chroma near vanilla's (see specs/wither-captain.md).
BLAZE_TO_SOUL = {
	"310e0b": "0f1934", "5f0201": "00305b", "6c3100": "004970", "8b3401": "015b7f", "ab7501": "0190ab",
	"d17800": "06a3b9", "fc9600": "11cad4", "fcb217": "0adce2", "ffd528": "14f6fc", "fff847": "5dfaff",
	"ffff84": "c8feff", "ffffff": "ffffff",
}

# Netherite ramp -> polished blackstone ramp (cooler, darker, blue-violet; blackstone's warm dark accent kept).
NETHERITE_TO_BLACKSTONE = {
	"0a0a0a": "09070a", "111111": "100c11", "171111": "130e14", "241f20": "1c1720", "322727": "28221f",
	"352d2d": "2b2629", "3f303b": "302b36", "473e3f": "36323d", "49393f": "393442", "4d494d": "3e3b48",
	"51444e": "43404c", "5d565d": "4f4c57", "766a76": "686572",
}


def swap(img, table):
	table = {hx(k): hx(v) for k, v in table.items()}
	out = img.convert("RGBA").copy()
	px = out.load()
	for y in range(out.height):
		for x in range(out.width):
			r, g, b, a = px[x, y]
			if a == 0:
				continue
			if (r, g, b) not in table:
				nearest = min(table, key=lambda c: sum((c[i] - (r, g, b)[i]) ** 2 for i in range(3)))
				table[(r, g, b)] = table[nearest]
			px[x, y] = table[(r, g, b)] + (a,)
	return out


# --- 32x for UV layouts ------------------------------------------------------------------------------------------

def smooth32(img, ramp, reach=2, side=0.19):
	"""Classic Faithful-style 32x twin of a UV-mapped texture: each pixel becomes 2x2, each sub-pixel leans a little
	toward its horizontal and vertical neighbours when they are close on the ramp (same material, same face), then
	snaps to the ramp with in-between shades added, so gradients come out smoother without blurring face edges."""
	ramp = sorted({hx(c) if isinstance(c, str) else c for c in ramp}, key=lum)
	fine = []
	for i, c in enumerate(ramp):
		fine.append(c)
		if i + 1 < len(ramp):
			fine.append(pa.mix(c, ramp[i + 1], 0.5))
	idx = {c: i for i, c in enumerate(ramp)}

	def rank(c):
		return idx.get(c, min(range(len(ramp)), key=lambda i: sum((ramp[i][k] - c[k]) ** 2 for k in range(3))))

	src = img.convert("RGBA")
	sp = src.load()
	w, h = src.size
	out = Image.new("RGBA", (w * 2, h * 2), (0, 0, 0, 0))
	op = out.load()
	for y in range(h):
		for x in range(w):
			p = sp[x, y]
			if p[3] == 0:
				continue
			rp = rank(p[:3])
			for sy in (0, 1):
				for sx in (0, 1):
					acc = [float(v) for v in p[:3]]
					wsum = 1.0
					for nx, ny in ((x + (sx * 2 - 1), y), (x, y + (sy * 2 - 1))):
						if 0 <= nx < w and 0 <= ny < h:
							q = sp[nx, ny]
							if q[3] and abs(rank(q[:3]) - rp) <= reach:
								for k in range(3):
									acc[k] += q[k] * side / (1 - side) * 1.0
								wsum += side / (1 - side)
					c = tuple(v / wsum for v in acc)
					c = min(fine, key=lambda f: sum((f[k] - c[k]) ** 2 for k in range(3)))
					op[2 * x + sx, 2 * y + sy] = c + (p[3],)
	return out


def grid_of(img, chars="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"):
	"""An image as a pixelart grid + palette (chars assigned dark to light)."""
	img = img.convert("RGBA")
	cols = sorted({img.getpixel((x, y))[:3] for y in range(img.height) for x in range(img.width)
		if img.getpixel((x, y))[3]}, key=lum)
	key = {c: chars[i] for i, c in enumerate(cols)}
	grid = ["".join("." if img.getpixel((x, y))[3] == 0 else key[img.getpixel((x, y))[:3]]
		for x in range(img.width)) for y in range(img.height)]
	return grid, {v: k for k, v in key.items()}


def save(img, rel):
	p = OUT16 / rel
	p.parent.mkdir(parents=True, exist_ok=True)
	img.save(p)


def save32(img, rel):
	p = OUT32 / rel
	p.parent.mkdir(parents=True, exist_ok=True)
	img.save(p)


# --- Nether Star Shard (hand-placed; vanilla nether star palette) ------------------------------------------------

STAR = {
	"c": "556b6b", "a": "649090", "b": "88a4a4", "e": "b9c9c9", "g": "cbd6d6", "d": "dae2e2",
	"f": "fdffa8", "i": "e0e277", "h": "d2d200",
}
# One arm of the star snapped off: a faceted point (top right), lit facet left of the ridge, shaded facet right of it,
# a jagged broken base (bottom left) with a sliver of the yellow core still in it. Drawn as a polygon so the edges
# stay straight.
SHARD_POLY = [(13.4, 0.8), (14.6, 4.0), (10.6, 13.0), (9.2, 13.9), (8.0, 12.6), (6.6, 14.4), (4.4, 12.8), (2.0, 11.6), (6.2, 5.4)]
SHARD_RIDGE = ((13.6, 1.4), (6.6, 12.8))
SHARD_CORE = (6.2, 11.8)


def _inside(px_, py_, poly):
	hit = False
	for i in range(len(poly)):
		(x1, y1), (x2, y2) = poly[i], poly[(i + 1) % len(poly)]
		if (y1 > py_) != (y2 > py_) and px_ < x1 + (py_ - y1) * (x2 - x1) / (y2 - y1):
			hit = not hit
	return hit


def shard_grid():
	inside = [[_inside(x + 0.5, y + 0.5, SHARD_POLY) for x in range(16)] for y in range(16)]
	(rx1, ry1), (rx2, ry2) = SHARD_RIDGE
	dx, dy = rx2 - rx1, ry2 - ry1
	ln = (dx * dx + dy * dy) ** 0.5
	rows = []
	for y in range(16):
		row = ""
		for x in range(16):
			if not inside[y][x]:
				row += "."
				continue
			cx, cy = x + 0.5, y + 0.5
			out = lambda ox, oy: not (0 <= x + ox < 16 and 0 <= y + oy < 16 and inside[y + oy][x + ox])
			side = ((cx - rx1) * dy - (cy - ry1) * dx) / ln  # > 0: right of the ridge (shaded facet)
			core = ((cx - SHARD_CORE[0]) ** 2 + (cy - SHARD_CORE[1]) ** 2) ** 0.5
			if out(0, -1) or out(-1, 0):
				ch = "a" if not (out(0, 1) or out(1, 0)) or side < 0 else "c"
			elif out(0, 1) or out(1, 0):
				ch = "c"
			elif core < 0.7:
				ch = "h"
			elif core < 1.3:
				ch = "i"
			elif core < 1.9:
				ch = "f"
			elif abs(side) < 0.55:
				ch = "d"
			elif side < 0:
				ch = "g"
			elif side < 1.0:
				ch = "e"
			else:
				ch = "b"
			row += ch
		rows.append(row)
	return rows


SHARD = shard_grid()


# --- Captain's Helm icon (netherite helmet swapped, with horns) --------------------------------------------------

def helm_icon(v):
	base = swap(Image.open(v / "item/netherite_helmet.png"), NETHERITE_TO_BLACKSTONE)
	g, pal = grid_of(base)
	# Helmet moved down two rows to make room for the horns.
	g = ["." * 16] * 2 + g[:14]
	hp = {"H": "26212a", "M": "3e3b48", "L": "5b5866", "T": "807d8a", "D": "141017"}
	pal.update({k: hx(v) for k, v in hp.items()})
	# Left horn (the right horn is its mirror): out from the helmet, bent up, bent up again.
	horn = [
		"................",
		"T...............",
		"LM..............",
		"LMD.............",
		"LMD.............",
		"LMD.............",
		"LMMD............",
		".DMMD...........",
		"..DDD...........",
	]
	rows = [list(r) for r in g]
	for y, r in enumerate(horn):
		for x, ch in enumerate(r):
			if ch != ".":
				for hx_ in (x, 15 - x):
					if rows[y][hx_] == ".":
						rows[y][hx_] = ch
	return ["".join(r) for r in rows], pal


# --- Horn texture (UV layout for CaptainsHelmHorns, 16x16) -------------------------------------------------------

HORN_SEGMENTS = [  # (u, v, length w, height h, depth d, base colour)
	(0, 0, 3, 2, 2, "302b36"),
	(0, 4, 3, 2, 2, "3e3b48"),
	(0, 8, 3, 1, 1, "57545f"),
]


def horn_texture():
	img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
	px = img.load()
	for n, (u, v, w, h, d, col) in enumerate(HORN_SEGMENTS):
		c = hx(col)

		def put(x, y, k, s=n):
			wob = pa.hash01(x, y, 7 + s) - 0.5
			cc = pa.lighten(c, k + wob * 0.12) if k + wob * 0.12 > 0 else pa.darken(c, -(k + wob * 0.12))
			px[x, y] = cc + (255,)

		for x in range(u + d, u + d + w):  # top (lit) and bottom (shadow)
			for y in range(v, v + d):
				put(x, y, 0.22)
			for y in range(v, v + d):
				put(x + w, y, -0.35)
		for y in range(v + d, v + d + h):  # sides: tip end, front, root end, back
			t = (y - v - d) / max(1, h - 1) if h > 1 else 0.5
			k = 0.08 - 0.3 * t
			for x in range(u, u + d):
				put(x, y, k + 0.12)
			for x in range(u + d, u + d + w):
				put(x, y, k)
			for x in range(u + d + w, u + 2 * d + w):
				put(x, y, k - 0.1)
			for x in range(u + 2 * d + w, u + 2 * d + 2 * w):
				put(x, y, k - 0.05)
	return img


def ramp_of(img):
	return {img.getpixel((x, y))[:3] for y in range(img.height) for x in range(img.width) if img.getpixel((x, y))[3]}


def main():
	v = vanilla_dir()

	blaze = swap(Image.open(v / "entity/blaze/blaze.png"), BLAZE_TO_SOUL)
	save(blaze, "entity/soulfire_blaze.png")
	save32(smooth32(blaze, BLAZE_TO_SOUL.values()), "entity/soulfire_blaze.png")

	worn = swap(Image.open(v / "entity/equipment/humanoid/netherite.png"), NETHERITE_TO_BLACKSTONE)
	save(worn, "entity/equipment/humanoid/captains_helm.png")
	save32(smooth32(worn, NETHERITE_TO_BLACKSTONE.values()), "entity/equipment/humanoid/captains_helm.png")

	star = {k: hx(c) for k, c in STAR.items()}
	save(pa.render(SHARD, star), "item/nether_star_shard.png")
	save32(pa.render32(SHARD, star, outline="ca", inner={"c": "b", "a": "e"}, bevel=0.06,
		material={k: ("core" if k in "fih" else "glass") for k in star}), "item/nether_star_shard.png")

	grid, pal = helm_icon(v)
	save(pa.render(grid, pal), "item/captains_helm.png")
	dark = "".join(k for k, c in pal.items() if lum(c) < 26)
	save32(pa.render32(grid, pal, outline=dark, bevel=0.10, grain=("helm",), grain_amount=0.05,
		material={k: ("horn" if k in "HMLTD" else "helm") for k in pal}), "item/captains_helm.png")

	horns = horn_texture()
	save(horns, "entity/captains_helm_horns.png")
	save32(smooth32(horns, ramp_of(horns), reach=3), "entity/captains_helm_horns.png")


if __name__ == "__main__":
	main()
