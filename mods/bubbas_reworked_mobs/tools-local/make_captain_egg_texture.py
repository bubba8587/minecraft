# dte:C1,B16
"""Wither Captain and Soulfire Blaze spawn eggs (specs/wither-captain.md). Captain: vanilla's wither skeleton spawn egg with the Captain's Helm horns
on top. 16x = vanilla's egg + hand-placed horns; 32x = Classic Faithful 32x Jappa's egg + hand-placed horns. The egg
moves down to make room; the horns use the Captain's Helm horn palette (make_captain_textures.py). Soulfire Blaze:
vanilla's (16x) and CF32J's (32x) blaze egg, each pixel placed on the Soulfire Blaze's soul ramp by OKLab lightness
between the two vanilla blaze colours around it (BLAZE_TO_SOUL in make_captain_textures.py).

Usage: python3 make_captain_egg_texture.py [vanilla_textures_dir] [cf32j]
cf32j: the CF32J zip or its item/wither_skeleton_spawn_egg.png (default: the Modrinth profile zip, then the scratch
copy). Without it the committed 32x PNG is kept. Needs Pillow.
"""
import io
import os
import sys
import zipfile
from pathlib import Path

from PIL import Image

MOD = "bubbas_reworked_mobs"
ROOT = Path(__file__).resolve().parents[3]
RES = Path(__file__).resolve().parent.parent / "src/main/resources"
OUT16 = RES / f"assets/{MOD}/textures/item/wither_captain_spawn_egg.png"
OUT32 = RES / f"resourcepacks/{MOD}_32x/assets/{MOD}/textures/item/wither_captain_spawn_egg.png"
CF_ENTRY = "assets/minecraft/textures/item/wither_skeleton_spawn_egg.png"


def hx(s):
	return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


# Captain's Helm horn palette (dark to light), with in-between shades for 32x
HORN = {
	"D": "141017", "H": "26212a", "h": "322c38", "M": "3e3b48", "m": "4c4957", "L": "5b5866", "l": "6d6a78",
	"T": "807d8a",
}

# --- 16x: left horn; the right horn is its mirror. Egg moved down 1 row. ---------------------------------------
HORN16 = [
	".T....",
	"LTD...",
	"LMD...",
	"LMD...",
	".LMD..",
	"..DMD.",
]

# --- 32x: left horn; the right horn is its mirror. Egg moved down 2 rows. --------------------------------------
HORN32 = [
	".....T......",
	"....lTD.....",
	"...LlD......",
	"..LlmD......",
	"..LlMD......",
	".LlmMD......",
	".LmhMhD.....",
	"LlmMMhD.....",
	"LlmMMhD.....",
	"LmhMhhhD....",
	".LmmMhhHD...",
	".DLmMMhHHD..",
	"..DDmMhhHD..",
	"....DDDDD...",
]


def stamp(egg, horn, shift, darken_right=True):
	w, h = egg.size
	out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
	out.paste(egg.crop((0, 0, w, h - shift)), (0, shift))
	px = out.load()
	ramp = "DHhMmLlT"
	for y, row in enumerate(horn):
		for x, ch in enumerate(row):
			if ch == ".":
				continue
			px[x, y] = hx(HORN[ch])
			rch = ramp[max(0, ramp.index(ch) - 1)] if darken_right and ch != "D" else ch
			px[w - 1 - x, y] = hx(HORN[rch])
	return out


def load_cf32j(arg, entry=CF_ENTRY):
	cands = [Path(arg)] if arg else []
	appdata = os.environ.get("APPDATA")
	if appdata:
		cands.append(Path(appdata) / "ModrinthApp/profiles/porp/resourcepacks/CF32J-2026r21.zip")
	cands.append(Path("/tmp/claude-0/-home-user-minecraft/5a3074f0-4b40-5f16-9911-ec930d6c875b/scratchpad/cf32j") / entry)
	for c in cands:
		if c.is_file() and c.suffix == ".zip":
			with zipfile.ZipFile(c) as z:
				return Image.open(io.BytesIO(z.read(entry))).convert("RGBA")
		if c.is_file():
			return Image.open(c).convert("RGBA")
	return None


def vanilla_dir(arg):
	if arg:
		return Path(arg)
	for base in (ROOT, Path("/home/user/minecraft")):
		hits = sorted((base / "build/mcsrc").glob("*/vanilla/assets/minecraft/textures"))
		if hits:
			return hits[-1]
	sys.exit("vanilla textures not found; pass the path")


# --- Soulfire Blaze egg: vanilla's blaze egg on the Soulfire Blaze's soul ramp ------------------------------------

def _lin(c):
	c /= 255.0
	return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def _gam(c):
	c = c * 12.92 if c <= 0.0031308 else 1.055 * c ** (1 / 2.4) - 0.055
	return max(0, min(255, round(c * 255)))


def oklab(rgb):
	r, g, b = (_lin(v) for v in rgb[:3])
	l = (0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b) ** (1 / 3)
	m = (0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b) ** (1 / 3)
	s = (0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b) ** (1 / 3)
	return (0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s, 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
			0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s)


def from_oklab(lab):
	L, a, b = lab
	l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3
	m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3
	s = (L - 0.0894841775 * a - 1.2914855480 * b) ** 3
	return (_gam(4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s), _gam(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s),
			_gam(-0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s))


def soul_ramp():
	sys.path.insert(0, str(Path(__file__).resolve().parent))
	from make_captain_textures import BLAZE_TO_SOUL
	pairs = sorted(((oklab(hx(k)), oklab(hx(v))) for k, v in BLAZE_TO_SOUL.items()), key=lambda p: p[0][0])
	return pairs


def to_soul(img, pairs):
	out = img.convert("RGBA").copy()
	px = out.load()
	for y in range(out.height):
		for x in range(out.width):
			r, g, b, a = px[x, y]
			if a == 0:
				continue
			L = oklab((r, g, b))[0]
			lo = max((p for p in pairs if p[0][0] <= L), key=lambda p: p[0][0], default=pairs[0])
			hi = min((p for p in pairs if p[0][0] >= L), key=lambda p: p[0][0], default=pairs[-1])
			t = 0.0 if hi[0][0] == lo[0][0] else (L - lo[0][0]) / (hi[0][0] - lo[0][0])
			lab = tuple(lo[1][i] + (hi[1][i] - lo[1][i]) * t for i in range(3))
			px[x, y] = from_oklab(lab) + (a,)
	return out


SOUL16 = RES / f"assets/{MOD}/textures/item/soulfire_blaze_spawn_egg.png"
SOUL32 = RES / f"resourcepacks/{MOD}_32x/assets/{MOD}/textures/item/soulfire_blaze_spawn_egg.png"
CF_BLAZE_ENTRY = "assets/minecraft/textures/item/blaze_spawn_egg.png"


def main():
	args = sys.argv[1:] + [None, None]
	v = vanilla_dir(args[0])
	egg16 = Image.open(v / "item/wither_skeleton_spawn_egg.png").convert("RGBA")
	OUT16.parent.mkdir(parents=True, exist_ok=True)
	stamp(egg16, HORN16, 1).save(OUT16)
	pairs = soul_ramp()
	to_soul(Image.open(v / "item/blaze_spawn_egg.png"), pairs).save(SOUL16)
	egg32 = load_cf32j(args[1])
	if egg32 is None:
		print("CF32J not found: kept the committed 32x eggs")
		return
	OUT32.parent.mkdir(parents=True, exist_ok=True)
	stamp(egg32, HORN32, 2).save(OUT32)
	blaze32 = load_cf32j(args[1], CF_BLAZE_ENTRY)
	to_soul(blaze32, pairs).save(SOUL32)


if __name__ == "__main__":
	main()
