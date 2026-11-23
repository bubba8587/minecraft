#!/usr/bin/env python3
# dte:C1,B16
"""The spun web's pixel texture (specs/spiders.md "Spun webs"), 16x and its 32x twin.

The renderer draws each strand as a stepped line of square pixels snapped to the world's texel grid and colours each
pixel from vanilla's cobweb palette (white, pale and cool grey) itself, so this texture is just a crisp, opaque white
square: nothing soft, nothing blended, so the strands sit next to the cobweb block's pixel art. Agent-made, listed in
docs/art-todo.md.
"""
from pathlib import Path
from PIL import Image

HERE = Path(__file__).resolve().parent
MOD = HERE.parent / "src" / "main" / "resources"
OUT16 = MOD / "assets" / "bubbas_reworked_mobs" / "textures" / "entity" / "spun_web_strand.png"
OUT32 = MOD / "resourcepacks" / "bubbas_reworked_mobs_32x" / "assets" / "bubbas_reworked_mobs" / "textures" / "entity" / "spun_web_strand.png"

for out, size in ((OUT16, 16), (OUT32, 32)):
	out.parent.mkdir(parents=True, exist_ok=True)
	Image.new("RGBA", (size, size), (255, 255, 255, 255)).save(out)
	print("wrote", out)
