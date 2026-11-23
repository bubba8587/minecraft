# dte:A3
"""Cuts exact 16x16 item icons out of the poster camera's chest screenshots (GUI scale 4, 1920x1080, 3-row chest)
into build/posters/icons/<name>.png (specs/posters.md)."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SHOTS = ROOT / "mods/postershots/build/run/posters/shots"
OUT = ROOT / "build/posters/icons"
SLOT_BG = (139, 139, 139)

GEMS = ["speed", "strength", "regeneration", "fire_resistance", "water_breathing", "night_vision", "jump_boost",
	"invisibility", "slow_falling", "resistance", "haste", "health_boost", "luck", "dolphins_grace", "conduit_power",
	"absorption", "poison", "weakness", "wither", "levitation", "glowing", "ender", "breeze", "silence",
	"hero_of_the_village", "saturation", "darkness"]
OTHER = ["totem_of_warding", "totem_of_keeping", "totem_of_gathering", "totem_of_fortification", "totem_of_growth",
	"totem_of_seeking", "totem_of_the_beginning", "totem_of_the_end", "totem_of_herding", "totem_of_clear_skies",
	"grave_talisman", "glider", "quartz_sword", "quartz_pickaxe", "quartz_axe", "quartz_shovel", "quartz_hoe",
	"quartz_helmet", "quartz_chestplate", "quartz_leggings", "quartz_boots", "skeleton_sleeper", "zombie_sleeper",
	"gravestone", "synthetic_diamond_ore", "infused_gem", "true_unbreaking_book"]


def cut(shot, names, prefix=""):
	im = Image.open(SHOTS / shot).convert("RGBA")
	left, top, scale = (480 - 176) // 2, (270 - 168) // 2, 4
	for i, n in enumerate(names):
		x = (left + 8 + (i % 9) * 18) * scale
		y = (top + 18 + (i // 9) * 18) * scale
		ic = Image.new("RGBA", (16, 16))
		for py in range(16):
			for px in range(16):
				c = im.getpixel((x + px * scale + 1, y + py * scale + 1))
				if c[:3] != SLOT_BG:
					ic.putpixel((px, py), c)
		ic.save(OUT / f"{prefix}{n}.png")


if __name__ == "__main__":
	OUT.mkdir(parents=True, exist_ok=True)
	cut("items_gems.png", GEMS, "gem_")
	cut("items_other.png", OTHER)
	print(len(list(OUT.glob("*.png"))), "icons in", OUT)
