#!/usr/bin/env python3
# dte:B16
"""Quartz Shard icon, 16x and 32x (agent-made, docs/art-todo.md): one broken-off crystal in vanilla quartz's palette."""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools"))
from pixelart import check, render, render32  # noqa: E402

MOD = Path(__file__).resolve().parents[1] / "src/main/resources"

PALETTE = {
	"o": (111, 93, 90), "m": (137, 123, 115), "b": (179, 160, 145), "c": (182, 164, 142), "d": (212, 202, 186),
	"e": (221, 212, 198), "f": (229, 223, 214), "g": (234, 229, 222), "w": (247, 245, 242),
}
MATERIAL = {ch: "quartz" for ch in "cdefgw"}

ICON = [
	"................",
	"................",
	"................",
	"..........b.....",
	".........bwm....",
	"........bgwm....",
	"........bgfdm...",
	".......bgwfdm...",
	".......bwfedm...",
	"......bgfedcm...",
	"......bwfedco...",
	"......mfedcco...",
	".......mddcoo...",
	"........mcoo....",
	".........oo.....",
	"................",
]


def main():
	check(ICON, 16)
	out16 = MOD / "assets/bubbas_extra_recipes/textures/item/quartz_shard.png"
	out32 = MOD / "resourcepacks/bubbas_extra_recipes_32x/assets/bubbas_extra_recipes/textures/item/quartz_shard.png"
	render(ICON, PALETTE).save(out16)
	render32(ICON, PALETTE, MATERIAL, outline="obm", inner={"b": "d", "m": "c", "o": "c"}, bevel=1, cf=True).save(out32)
	print("wrote", out16.name, "at 16x and 32x")


if __name__ == "__main__":
	main()
