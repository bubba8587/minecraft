# dte:A3
"""Renders the promo posters (specs/posters.md): python3 tools/posters/make.py [name ...] -> build/posters/<name>.png.
Shots come from the poster camera (mods/postershots/build/run/posters/shots), icons from tools/posters/icons.py."""
import sys
from pathlib import Path
from PIL import Image, ImageFilter, ImageEnhance
from layouts import *

SHOTS = ROOT / "mods/postershots/build/run/posters/shots"
ICONS = ROOT / "build/posters/icons"
OUT = ROOT / "build/posters"
POSTERS = {}


def S(name):
	p = SHOTS / f"{name}.png"
	if p.exists():
		return p
	print("  missing shot:", name)
	return Image.new("RGB", (1920, 1080), (60, 20, 60))


def I(name):
	"""A mod item icon cut from the camera's chest renders, or a vanilla item texture."""
	p = ICONS / f"{name}.png"
	if p.exists():
		return Image.open(p).convert("RGBA")
	return vanilla(f"textures/item/{name}.png").crop((0, 0, 16, 16))


def poster(fn):
	POSTERS[fn.__name__] = fn
	return fn


@poster
def p0_title(out):
	"""Title-screen parody: panorama, logo, splash, the mods as menu buttons."""
	c = Image.new("RGBA", (W, H))
	bg = cover(S("cairnF6"), W, H, focus=(0.45, 0.5)).filter(ImageFilter.GaussianBlur(2.5))
	c.alpha_composite(bg)
	c.alpha_composite(fade(H, 0, H, color=(0, 0, 0), max_alpha=110))
	L1 = logo("BUBBA'S", 12, block("gold_block"))
	L2 = logo("MODS", 17, block("cobblestone"))
	c.alpha_composite(L1, ((W - L1.width) // 2, 80))
	y2 = 80 + L1.height - 10
	c.alpha_composite(L2, ((W - L2.width) // 2, y2))
	sp = splash("Also try vanilla!", 4)
	c.alpha_composite(sp, (min(W - sp.width - 12, (W + L2.width) // 2 - 90), y2 + L2.height // 2 - sp.height // 2 + 30))
	names = ["Reworked Mobs", "Infused Gems", "Totems & Artifacts", "Reworked Beacons", "Reworked Mining",
		"Extra Recipes", "World Tweaks"]
	y = 600
	for n in names:
		b = button(n, 4, 720)
		c.alpha_composite(b, ((W - b.width) // 2, y))
		y += b.height + 16
	draw_text(c, (20, H - 34), "Minecraft 26.3 (Fabric)", 2, (255, 255, 255))
	draw_text(c, (W - 20, H - 34), "7 mods, 1 pack", 2, (255, 255, 255), anchor="r")
	c.convert("RGB").save(out, quality=95)


PREV = ROOT / "docs/previews"


def gem_grid(c, cx, y, scale=6):
	from icons import GEMS
	rows = [[I("gem_" + g) for g in GEMS[r * 9:(r + 1) * 9]] for r in range(3)]
	h = 0
	for r, row in enumerate(rows):
		strip = slot_row(row, scale)
		c.alpha_composite(strip, (cx - strip.width // 2, y + r * 18 * scale))
		h = (r + 1) * 18 * scale
	return h


def icon_row(c, names, cx, y, scale=5):
	strip = slot_row([I(n) for n in names], scale)
	c.alpha_composite(strip, (cx - strip.width // 2, y))
	return strip


@poster
def p1_undead(out):
	hero(out, S("ysun_270"), "REWORKED MOBS", "mossy_stone_bricks", "deepslate_bricks",
		[(S("crypt1"), "Noble crypts"), (S("hall3"), "...until they wake")],
		splash_text="Shhh, they're sleeping!", blurb="The dead sleep in crypt yards. Make noise and they get up.",
		icons=[I("skeleton_sleeper"), I("zombie_sleeper"), I("gravestone"), I("grave_talisman"), I("bone"),
			I("iron_sword"), I("bow"), I("rotten_flesh"), I("lantern")],
		hero_kw={"focus": (0.5, 0.55)}, hero_h=760)


@poster
def p2_wild(out):
	c = textured_bg(W, H, block("deepslate_tiles"), px=6)
	header(c)
	y = title_block(c, "REWORKED MOBS", block("oxidized_copper"), 128, 9, "It's watching you...",
		"Creepers watch. Endermen build. Something hums below.")
	top = y + 12
	place_photo(c, S("fabC"), 40, top, W - 80, 420, "Ancient Fabricators", label_pos="bl")
	gw = (W - 80 - 26) // 2
	gh = (H - 80 - (top + 420 + 30) - 26) // 2
	y2 = top + 420 + 30
	place_photo(c, S("crN"), 40, y2, gw, gh, "Creepers", focus=(0.4, 0.6))
	place_photo(c, S("cairnF3"), 40 + gw + 26, y2, gw, gh, "Enderman cairns")
	place_photo(c, S("spZ4"), 40, y2 + gh + 26, gw, gh, "Giant spiders", focus=(0.5, 0.45))
	place_photo(c, PREV / "drowned/drowned-grab-land.png", 40 + gw + 26, y2 + gh + 26, gw, gh, "Drowned", label_pos="tr", crop=(170, 110, 700, 420))
	footer(c)
	c.convert("RGB").save(out, quality=95)


@poster
def p3_gems(out):
	c = textured_bg(W, H, block("deepslate_tiles"), px=6, tint=(70, 30, 110))
	header(c)
	y = title_block(c, "INFUSED GEMS", block("amethyst_block"), 128, 10, "Shiny!",
		"Brew a potion into a gem. Socket it. Keep the effect.")
	gh = gem_grid(c, W // 2, y + 16, 6)
	y3 = y + 16 + gh + 40
	pw = 520
	place_photo(c, S("bcC"), 40, y3, pw, H - 90 - y3, "Gems power beacons", label_pos="bl")
	ty = y3 - 6
	for shot in ("gear_slot0", "gear_slot1"):
		tt = tooltip_crop(SHOTS / f"{shot}.png", 3)
		if tt:
			c.alpha_composite(tt, (40 + pw + 40, ty))
			ty += tt.height + 24
	footer(c)
	c.convert("RGB").save(out, quality=95)


@poster
def p4_totems(out):
	hero(out, S("totT3"), "TOTEMS & ARTIFACTS", "gold_block", "polished_blackstone_bricks",
		[(S("totT2"), "Totem statues", {"focus": (0.45, 0.5)}), (PREV / "totems/gathering-pulling.png", "Totem of Gathering", {"crop": (60, 30, 820, 372)})],
		splash_text="Found, not crafted!", blurb="Rare finds that do what potions and enchantments can't.",
		icons=[I(n) for n in ["totem_of_warding", "totem_of_keeping", "totem_of_gathering", "totem_of_growth",
			"totem_of_seeking", "totem_of_the_beginning", "totem_of_the_end", "totem_of_herding", "totem_of_clear_skies"]],
		hero_h=740, title_scale=8)


@poster
def p5_more(out):
	"""Extra Recipes, Reworked Mining and World Tweaks: glider hero, quartz in the Nether, real tooltips."""
	hero_h = 700
	c = textured_bg(W, H, block("spruce_planks"), px=6)
	c.alpha_composite(cover(S("glideK"), W, hero_h, focus=(0.5, 0.35)), (0, 0))
	c.alpha_composite(fade(hero_h, hero_h - 300, hero_h, color=(8, 8, 10)), (0, 0))
	c.alpha_composite(fade(170, 170, 0, color=(0, 0, 0), max_alpha=140), (0, 0))
	header(c)
	lab = tooltip([("The Glider", (255, 255, 85))], 3)
	c.alpha_composite(lab, (600, 250))
	y = title_block(c, "AND MORE!", block("oak_planks"), hero_h - 110, 11, "No elytra needed!",
		"Glide on crafted wings. Mine quartz. Watch the lava boil.")
	top = y + 14
	pw = 600
	ph = H - 170 - top
	place_photo(c, S("qtzD0"), 40, top, pw, ph, "Quartz gear, lava geysers", focus=(0.45, 0.5))
	ty = top - 4
	for shot in ("items_other_slot24", "items_other_slot26", "items_other_slot18", "gear_slot2"):
		tt = tooltip_crop(SHOTS / f"{shot}.png", 3 if shot in ("items_other_slot24", "items_other_slot26") else 2)
		if tt:
			c.alpha_composite(tt, (40 + pw + 30, ty))
			ty += tt.height + 18
	hb = hotbar([I(n) for n in ["glider", "quartz_sword", "quartz_pickaxe", "quartz_axe", "quartz_shovel",
		"quartz_helmet", "quartz_chestplate", "true_unbreaking_book", "synthetic_diamond_ore"]], 4)
	c.alpha_composite(hb, ((W - hb.width) // 2, H - 140))
	footer(c)
	c.convert("RGB").save(out, quality=95)


@poster
def p6_story(out):
	"""Story-format overview (1080x1920): the logo, then one tile per mod with a small block-letter name."""
	SW, SH = 1080, 1920
	c = textured_bg(SW, SH, block("deepslate_tiles"), px=6)
	bg = cover(S("cairnF6"), SW, 560, focus=(0.45, 0.45)).filter(ImageFilter.GaussianBlur(1.5))
	c.alpha_composite(bg, (0, 0))
	c.alpha_composite(fade(560, 300, 560, color=(14, 14, 18), w=SW), (0, 0))
	L1 = logo("BUBBA'S", 11, block("gold_block"))
	L2 = logo("MODS", 16, block("cobblestone"))
	c.alpha_composite(L1, ((SW - L1.width) // 2, 70))
	y2 = 70 + L1.height - 8
	c.alpha_composite(L2, ((SW - L2.width) // 2, y2))
	sp = splash("Swipe for more!", 4)
	c.alpha_composite(sp, ((SW + L2.width) // 2 - 90, y2 + L2.height // 2 - sp.height // 2 + 30))
	tiles = [
		(S("ysun_270"), "REWORKED MOBS", "mossy_stone_bricks", {"focus": (0.5, 0.6)}),
		("GEMS", "INFUSED GEMS", "amethyst_block", {}),
		(S("totT3"), "TOTEMS", "gold_block", {"focus": (0.3, 0.6)}),
		(S("bcA"), "BEACONS", "diamond_block", {"focus": (0.5, 0.4)}),
		(S("glideK"), "EXTRA RECIPES", "oak_planks", {"focus": (0.5, 0.35)}),
		(S("qtzD0"), "MINING & TWEAKS", "quartz_block_side", {}),
	]
	top = 480
	gap = 24
	tw = (SW - 80 - gap) // 2
	th = (SH - 110 - top - 2 * gap) // 3
	for i, (ph, name, tex, kw) in enumerate(tiles):
		x = 40 + (i % 2) * (tw + gap)
		y = top + (i // 2) * (th + gap)
		if ph == "GEMS":
			from icons import GEMS
			grid = Image.new("RGBA", (9 * 18 * 3, 3 * 18 * 3))
			for r in range(3):
				grid.alpha_composite(slot_row([I("gem_" + g) for g in GEMS[r * 9:(r + 1) * 9]], 3), (0, r * 54))
			im = textured_bg(tw, th, block("amethyst_block"), px=5, dark=0.28)
			im.alpha_composite(grid, ((tw - grid.width) // 2, (th - grid.height) // 2 - 50))
		else:
			im = cover(ph, tw, th, **kw)
		im.alpha_composite(fade(th, th - 170, th, color=(0, 0, 0), max_alpha=200, w=tw), (0, 0))
		f, off = framed(im)
		c.alpha_composite(f, (x - off, y - off))
		s = 5
		L = logo(name, s, block(tex))
		while L.width > tw - 30 and s > 2:
			s -= 1
			L = logo(name, s, block(tex))
		c.alpha_composite(L, (x + (tw - L.width) // 2, y + th - L.height - 14))
	draw_text(c, (SW // 2, SH - 60), "Fabric mods for Minecraft 26.3", 3, DIM, anchor="c")
	c.convert("RGB").save(out, quality=95)


if __name__ == "__main__":
	OUT.mkdir(parents=True, exist_ok=True)
	for n in sys.argv[1:] or list(POSTERS):
		print("poster", n)
		POSTERS[n](OUT / f"{n}.png")
