# dte:C1,B16
"""Wither Captain spawn egg (specs/wither-captain.md): vanilla's wither skeleton spawn egg with the Captain's Helm horns
on top. 16x = vanilla's egg + hand-placed horns; 32x = Classic Faithful 32x Jappa's egg + hand-placed horns. The egg
moves down to make room; the horns use the Captain's Helm horn palette (make_captain_textures.py).

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


def load_cf32j(arg):
	cands = [Path(arg)] if arg else []
	appdata = os.environ.get("APPDATA")
	if appdata:
		cands.append(Path(appdata) / "ModrinthApp/profiles/porp/resourcepacks/CF32J-2026r21.zip")
	cands.append(Path("/tmp/claude-0/-home-user-minecraft/5a3074f0-4b40-5f16-9911-ec930d6c875b/scratchpad/cf32j") / CF_ENTRY)
	for c in cands:
		if c.is_file() and c.suffix == ".zip":
			with zipfile.ZipFile(c) as z:
				return Image.open(io.BytesIO(z.read(CF_ENTRY))).convert("RGBA")
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


def main():
	args = sys.argv[1:] + [None, None]
	egg16 = Image.open(vanilla_dir(args[0]) / "item/wither_skeleton_spawn_egg.png").convert("RGBA")
	OUT16.parent.mkdir(parents=True, exist_ok=True)
	stamp(egg16, HORN16, 1).save(OUT16)
	egg32 = load_cf32j(args[1])
	if egg32 is None:
		print("CF32J not found: kept the committed 32x egg")
		return
	OUT32.parent.mkdir(parents=True, exist_ok=True)
	stamp(egg32, HORN32, 2).save(OUT32)


if __name__ == "__main__":
	main()
