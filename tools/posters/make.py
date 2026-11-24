# dte:A3
"""Renders the promo posters (specs/posters.md): python3 tools/posters/make.py [name ...] -> build/posters/<name>.png.
Every line of text comes from specs/copy-strings.md through copytext.line (the owner's exact wording).
Shots come from the poster camera (mods/postershots/build/run/posters/shots), icons from tools/posters/icons.py."""
import sys
from PIL import Image, ImageDraw, ImageFilter
from layouts import *
from copytext import line

SHOTS = ROOT / "mods/postershots/build/run/posters/shots"
ICONS = ROOT / "build/posters/icons"
OUT = ROOT / "build/posters"
PREV = ROOT / "docs/previews"
POSTERS = {}
TEXT = (235, 235, 235)
NOTE = (85, 255, 255)
SOON = (255, 170, 0)
MOD_TEX = {"totem_of_north_wind": "bubbas_totems_artifacts", "relic_map_shred_whole": "bubbas_totems_artifacts"}


def S(name):
	p = SHOTS / f"{name}.png"
	if p.exists():
		return p
	print("  missing shot:", name)
	return Image.new("RGB", (1920, 1080), (60, 20, 60))


def I(name):
	"""A mod item icon cut from the camera's chest renders, a mod texture, or a vanilla item texture."""
	p = ICONS / f"{name}.png"
	if p.exists():
		return Image.open(p).convert("RGBA")
	if name in MOD_TEX:
		return mod_texture(MOD_TEX[name], f"item/{name}.png").crop((0, 0, 16, 16))
	return vanilla(f"textures/item/{name}.png").crop((0, 0, 16, 16))


def poster(fn):
	POSTERS[fn.__name__] = fn
	return fn


# --- Text blocks -----------------------------------------------------------------------------------------------

def text_block(mod, key, maxw, scale):
	"""One approved string (and its requirement note) wrapped to maxw at that scale."""
	text, note = line(mod, key)
	soon = None
	if text.startswith("[Coming in 26.4]:"):
		soon, text = "[Coming in 26.4]:", text[len("[Coming in 26.4]:"):].strip()
	lines = wrap(text, scale, maxw)
	lh = 10 * scale + 2
	h = len(lines) * lh + (lh if soon else 0) + ((10 * 2 + 8) if note else 0)
	im = Image.new("RGBA", (maxw, h))
	y = 0
	if soon:
		draw_text(im, (0, y), soon, scale, SOON)
		y += lh
	for ln in lines:
		draw_text(im, (0, y), ln, scale, TEXT)
		y += lh
	if note:
		draw_text(im, (0, y + 6), note, 2, NOTE)
	return im


def panel(w, h):
	"""A dark, slightly see-through card with a thin light edge."""
	p = Image.new("RGBA", (w, h), (0, 0, 0, 0))
	d = ImageDraw.Draw(p)
	d.rectangle((0, 0, w - 1, h - 1), fill=(10, 8, 12, 200), outline=(255, 255, 255, 40), width=2)
	return p


PAD, GUT = 16, 22


def _art(w, h, photo=None, icons=None, image=None, photo_kw=None):
	"""The left-hand picture of a card at height h: a framed photo, an image fitted as is, or item slots."""
	if photo is not None:
		pw = min(int(h * 16 / 9), w // 2 - 20)
		f, off = framed(cover(photo, pw, h, **(photo_kw or {})), border=3, shadow=8)
		return f, off, pw
	if image is not None:
		im = image if isinstance(image, Image.Image) else Image.open(image).convert("RGBA")
		r = min(h / im.height, (w // 2 - 20) / im.width)
		im = im.resize((int(im.width * r), int(im.height * r)), Image.NEAREST)
		return im, 0, im.width
	if icons:
		strip = slot_row(icons, 5)
		return strip, 0, strip.width
	return None, 0, 0


def plan(mod, key, w, art):
	"""Smallest card height for this string at text scale 3: beside the art, or under it when that is shorter."""
	has_art = any(art.get(k) is not None for k in ("photo", "icons", "image"))
	best = None
	for scale in (3, 2):
		opts = []
		if has_art:
			_, _, aw = _art(w - 2 * PAD, 200, **art)
			tw = w - 2 * PAD - aw - GUT
			if tw > 260:
				th = text_block(mod, key, tw, scale).height
				opts.append((max(th, 150 if art.get("photo") is not None else 90) + 2 * PAD, "side", scale))
			if not art.get("photo") is not None:
				th = text_block(mod, key, w - 2 * PAD, scale).height
				opts.append((90 + 14 + th + 2 * PAD, "stack", scale))
		else:
			opts.append((text_block(mod, key, w - 2 * PAD, scale).height + 2 * PAD, "text", scale))
		o = min(opts)
		if best is None:
			best = o
		if scale == 3:
			full = o
	return full, best


def card(c, mod, key, x, y, w, h, mode="side", scale=3, **art):
	c.alpha_composite(panel(w, h), (x, y))
	if mode == "side":
		im, off, aw = _art(w - 2 * PAD, h - 2 * PAD if art.get("photo") is not None or art.get("image") is not None else 90, **art)
		c.alpha_composite(im, (x + PAD - off, y + (h - im.height + 2 * off) // 2 - off))
		tx = x + PAD + aw + GUT
		tb = text_block(mod, key, x + w - PAD - tx, scale)
		c.alpha_composite(tb, (tx, y + (h - tb.height) // 2))
	elif mode == "stack":
		im, off, aw = _art(w - 2 * PAD, 90, **art)
		tb = text_block(mod, key, w - 2 * PAD, scale)
		top = y + (h - (im.height + 14 + tb.height)) // 2
		c.alpha_composite(im, (x + PAD, top))
		c.alpha_composite(tb, (x + PAD, top + im.height + 14))
	else:
		tb = text_block(mod, key, w - 2 * PAD, scale)
		c.alpha_composite(tb, (x + PAD, y + (h - tb.height) // 2))


def layout_cards(c, mod, rows, y, bottom, x=40, w=W - 80, gap=18):
	"""Cards stacked from y to bottom; each gets the height its text needs at scale 3, plus an equal share of what's left."""
	plans = [plan(mod, k, w, kw) for k, kw in rows]
	need = sum(p[0][0] for p in plans) + gap * (len(rows) - 1)
	chosen = [p[0] for p in plans]
	if need > bottom - y:
		print(f"  {mod}: cards need {need}px, have {bottom - y}: falling back to scale 2 where it helps")
		order = sorted(range(len(rows)), key=lambda i: plans[i][1][0] - plans[i][0][0])
		for i in order:
			if need <= bottom - y:
				break
			need += plans[i][1][0] - chosen[i][0]
			chosen[i] = plans[i][1]
	extra = max(0, (bottom - y - need) // len(rows))
	for (k, kw), (h, mode, sc) in zip(rows, chosen):
		card(c, mod, k, x, y, w, h + extra, mode, sc, **kw)
		y += h + extra + gap


def title_area(c, title, tex, hero_photo=None, hero_h=360, scale=9, hero_kw=None):
	"""Hero shot fading into the background with the block-letter title on the seam. Returns the y under it."""
	if hero_photo is not None:
		c.alpha_composite(cover(hero_photo, W, hero_h, **(hero_kw or {})), (0, 0))
		c.alpha_composite(fade(hero_h, hero_h - 200, hero_h, color=(8, 8, 10)), (0, 0))
	s = scale
	L = logo(title, s, block(tex))
	while L.width > W - 60 and s > 4:
		s -= 1
		L = logo(title, s, block(tex))
	top = (hero_h - L.height // 2 - 40) if hero_photo is not None else 40
	c.alpha_composite(L, ((W - L.width) // 2, top))
	return top + L.height + 16


def cards(out, title, tex, bg_tex, mod, rows, hero_photo=None, hero_h=360, hero_kw=None, scale=9, tint=None, extra=None):
	"""Title area, then one card per row: (key, dict(photo=..., icons=[...], image=..., photo_kw=...))."""
	c = textured_bg(W, H, block(bg_tex), px=6, tint=tint)
	y = title_area(c, title, tex, hero_photo, hero_h, scale, hero_kw)
	if extra:
		y = extra(c, y)
	layout_cards(c, mod, rows, y, H - 40)
	c.convert("RGB").save(out, quality=95)


# --- Posters ---------------------------------------------------------------------------------------------------

@poster
def p00_title(out):
	"""Title-screen look: panorama, logo, the mods as menu buttons."""
	c = Image.new("RGBA", (W, H))
	c.alpha_composite(cover(S("cairnF6"), W, H, focus=(0.45, 0.5)).filter(ImageFilter.GaussianBlur(2.5)))
	c.alpha_composite(fade(H, 0, H, color=(0, 0, 0), max_alpha=110))
	L1 = logo("BUBBA'S", 12, block("gold_block"))
	L2 = logo("MODS", 17, block("cobblestone"))
	c.alpha_composite(L1, ((W - L1.width) // 2, 80))
	c.alpha_composite(L2, ((W - L2.width) // 2, 80 + L1.height - 10))
	names = ["Reworked Mobs", "Infused Gems", "Totems & Artifacts", "Reworked Beacons", "Reworked Mining",
		"Extra Recipes", "World Tweaks"]
	y = 620
	for n in names:
		b = button(n, 4, 720)
		c.alpha_composite(b, ((W - b.width) // 2, y))
		y += b.height + 16
	c.convert("RGB").save(out, quality=95)


M = "Reworked Mobs"


@poster
def p01_mobs_graveyards(out):
	cards(out, "REWORKED MOBS", "mossy_stone_bricks", "deepslate_bricks", M, [
		("New Graveyard structures", dict(photo=S("gyL3"), photo_kw={"crop": (100, 120, 1800, 1076)})),
		("woken by sound", dict(photo=S("wake1"), photo_kw={"crop": (380, 250, 1460, 858)})),
		("You can loot the sleeping undead", dict(photo=S("row5"), photo_kw={"crop": (300, 250, 1400, 869)})),
		("pick up nearby weapons", dict(photo=S("woke4"), photo_kw={"crop": (500, 200, 1500, 763)})),
	], hero_photo=S("sitA"), hero_h=460, hero_kw={"crop": (240, 60, 1680, 700)})


@poster
def p02_mobs_world(out):
	cards(out, "REWORKED MOBS", "mossy_stone_bricks", "deepslate_bricks", M, [
		("random natural mob spawning", dict(photo=S("skG_195"), photo_kw={"crop": (300, 150, 1700, 937)})),
		("Ancient Fabricators", dict(photo=S("fbV3"), photo_kw={"crop": (300, 150, 1600, 881), "bright": 1.25})),
		("Will-o-wisps", dict(photo=S("wispD_0"), photo_kw={"crop": (300, 100, 1400, 719)})),
		("Frostbites", dict(icons=[block_icon("packed_ice"), block_icon("blue_ice"), block_icon("snow")])),
	], hero_photo=S("skG_185"), hero_kw={"focus": (0.5, 0.45)})


@poster
def p03_mobs_creatures(out):
	cards(out, "REWORKED MOBS", "oxidized_copper", "deepslate_tiles", M, [
		("Creepers are creepy", dict(photo=S("crO"), photo_kw={"crop": (520, 260, 1360, 733)})),
		("Endermen build cairns", dict(photo=S("cnX_70"), photo_kw={"crop": (400, 100, 1500, 719), "bright": 1.5})),
		("Drowned lie in wait", dict(photo=S("drn_3"), photo_kw={"crop": (300, 150, 1500, 825)})),
		("Spiders have real webs", dict(photo=S("rnL_60"))),
		("giant spiders", dict(photo=S("gnC"), photo_kw={"crop": (300, 100, 1600, 831), "bright": 1.3})),
	], hero_photo=S("cnX_40"), hero_h=300, scale=8, hero_kw={"bright": 1.5, "focus": (0.5, 0.3)})


T = "Totems & Artifacts"


def totem_icons(*names):
	return dict(icons=[I(n) for n in names])


@poster
def p04_totems(out):
	cards(out, "TOTEMS & ARTIFACTS", "gold_block", "polished_blackstone_bricks", T, [
		("New Totems", dict(photo=S("totT3"))),
		("Grave Talisman", totem_icons("grave_talisman")),
		("Totem of Warding", totem_icons("totem_of_warding")),
		("Totem of Keeping", totem_icons("totem_of_keeping")),
		("Totem of Gathering", totem_icons("totem_of_gathering")),
		("Totem of Fortification", totem_icons("totem_of_fortification")),
		("Totem of Growth", totem_icons("totem_of_growth")),
	], hero_photo=S("vaultE"), hero_h=250, scale=7, hero_kw={"focus": (0.5, 0.55)})


@poster
def p05_totems_more(out):
	cards(out, "TOTEMS & ARTIFACTS", "gold_block", "polished_blackstone_bricks", T, [
		("Totem of Seeking", totem_icons("totem_of_seeking")),
		("Totem of the Beginning", totem_icons("totem_of_the_beginning")),
		("Totem of Herding", totem_icons("totem_of_herding")),
		("Totem of the End", totem_icons("totem_of_the_end")),
		("Totem of Clear Skies", totem_icons("totem_of_clear_skies")),
		("Totem of the North Wind", totem_icons("totem_of_north_wind")),
		("Relic Map shreds", totem_icons("relic_map_shred_whole")),
	], hero_photo=S("totT2"), hero_h=250, scale=7, hero_kw={"focus": (0.45, 0.5)})


G = "Infused Gems"


def gem_grid(c, y, scale=5):
	from icons import GEMS
	for r in range(3):
		strip = slot_row([I("gem_" + g) for g in GEMS[r * 9:(r + 1) * 9]], scale)
		c.alpha_composite(strip, ((W - strip.width) // 2, y + r * 18 * scale))
	return y + 3 * 18 * scale + 24


@poster
def p06_gems(out):
	cards(out, "INFUSED GEMS", "amethyst_block", "deepslate_tiles", G, [
		("Say goodbye", dict(icons=[I("potion"), I("infused_gem"), I("gem_speed")])),
		("12 perfect", dict(icons=[I("nether_star"), I("netherite_chestplate")])),
		("secondary or tertiary", dict(image=tooltip_crop(SHOTS / "gear_slot0.png", 2))),
	], tint=(70, 30, 110), extra=lambda c, y: gem_grid(c, y))


@poster
def p07_gems_more(out):
	cards(out, "INFUSED GEMS", "amethyst_block", "deepslate_tiles", G, [
		("rare items, not potions", dict(icons=[I("golden_apple"), I("gem_absorption")])),
		("can only be found", dict(icons=[I("gem_haste"), I("gem_luck"), I("gem_resistance")])),
		("Place a Gem in a Beacon", dict(photo=S("bcC"))),
		("Freezing", dict(icons=[I("infused_gem")])),
	], hero_photo=S("bcB"), hero_h=300, tint=(70, 30, 110))


@poster
def p08_beacons(out):
	cards(out, "REWORKED BEACONS", "diamond_block", "deepslate_tiles", "Reworked Beacons", [
		("un-consumed set of gems", dict(icons=[I("amethyst_shard"), I("quartz"), I("ender_pearl"), I("diamond"),
			I("prismarine_shard"), I("nether_star")])),
		("apply equally", dict(photo=S("bcE"))),
		("Paired Beacons", dict(photo=S("pcF"))),
		("Highly recommended", dict(icons=[I("gem_speed"), I("totem_of_warding")])),
	], hero_photo=S("pcD"), hero_h=320, scale=8, hero_kw={"focus": (0.5, 0.45)})


@poster
def p09_mining(out):
	cards(out, "REWORKED MINING", "deepslate", "deepslate_tiles", "Reworked Mining", [
		("Overhauled tool progression", dict(icons=[I("wooden_pickaxe"), I("stone_pickaxe"), I("diamond_pickaxe")])),
		("nuggets", dict(icons=[I("iron_nugget"), I("gold_nugget"), I("emerald"), I("diamond")])),
		("True Unbreaking", dict(icons=[I("true_unbreaking_book")])),
		("Quartz tools", dict(photo=S("qtzC0"), photo_kw={"focus": (0.5, 0.6)})),
	], hero_photo=S("mineB_3"), hero_h=420, scale=8, hero_kw={"crop": (420, 260, 1500, 860)})


def _quartz_hotbar(c, y):
	hb = hotbar([I(n) for n in ["quartz_sword", "quartz_pickaxe", "quartz_axe", "quartz_shovel", "quartz_hoe",
		"quartz_helmet", "quartz_chestplate", "quartz_leggings", "quartz_boots"]], 4)
	c.alpha_composite(hb, ((W - hb.width) // 2, y))
	return y + hb.height + 24


@poster
def p10_world_tweaks(out):
	c = textured_bg(W, H, block("netherrack"), px=6)
	hero_h = 1000
	c.alpha_composite(cover(S("gyy_3"), W, hero_h, focus=(0.5, 0.4)), (0, 0))
	c.alpha_composite(fade(hero_h, hero_h - 300, hero_h, color=(8, 8, 10)), (0, 0))
	title_area(c, "WORLD TWEAKS", "magma", None, scale=9)
	layout_cards(c, "World Tweaks", [("Magma blocks", dict(icons=[block_icon("magma"), block_icon("lava_still")]))], H - 260, H - 40)
	c.convert("RGB").save(out, quality=95)


@poster
def p11_extra_recipes(out):
	cards(out, "EXTRA RECIPES", "oak_planks", "spruce_planks", "Extra Recipes", [
		("vanilla-esque", dict(icons=[block_icon("crafting_table_front"), block_icon("blast_furnace_front"), block_icon("smoker_front")])),
		("Renewable blocks", dict(icons=[block_icon("sand"), block_icon("red_sand"), block_icon("clay")])),
		("Tools, equipment", dict(icons=[I("trident"), I("leather"), I("glider")])),
		("Materials", dict(icons=[I("quartz"), I("iron_nugget"), I("glowstone_dust"), I("synthetic_diamond_ore")])),
		("Mod-only", dict(photo=S("glideK"), photo_kw={"focus": (0.5, 0.35)})),
	], hero_photo=_blast(), hero_h=290, scale=9)


def _blast():
	"""The blast furnace screen mid-blast (diorite to quartz), cut round the furnace panel."""
	return Image.open(S("blast")).convert("RGB").crop((600, 170, 1320, 590))


def block_icon(name):
	return block(name).resize((16, 16), Image.NEAREST)


@poster
def p12_story(out):
	"""Story-format overview (1080x1920): the logo, then one tile per mod with a small block-letter name."""
	SW, SH = 1080, 1920
	c = textured_bg(SW, SH, block("deepslate_tiles"), px=6)
	bg = cover(S("cairnF6"), SW, 560, focus=(0.45, 0.45)).filter(ImageFilter.GaussianBlur(1.5))
	c.alpha_composite(bg, (0, 0))
	c.alpha_composite(fade(560, 300, 560, color=(14, 14, 18), w=SW), (0, 0))
	L1 = logo("BUBBA'S", 11, block("gold_block"))
	L2 = logo("MODS", 16, block("cobblestone"))
	c.alpha_composite(L1, ((SW - L1.width) // 2, 70))
	c.alpha_composite(L2, ((SW - L2.width) // 2, 70 + L1.height - 8))
	tiles = [
		(S("skG_195"), "REWORKED MOBS", "mossy_stone_bricks", {}),
		("GEMS", "INFUSED GEMS", "amethyst_block", {}),
		(S("vaultE"), "TOTEMS & ARTIFACTS", "gold_block", {}),
		(S("pcD"), "REWORKED BEACONS", "diamond_block", {}),
		(S("glideK"), "EXTRA RECIPES", "oak_planks", {"focus": (0.5, 0.35)}),
		(S("mineB_3"), "REWORKED MINING", "quartz_block_side", {}),
		(S("gyy_3"), "WORLD TWEAKS", "magma", {}),
	]
	top, gap = 480, 24
	tw = (SW - 80 - gap) // 2
	rows = (len(tiles) + 1) // 2
	th = (SH - 60 - top - (rows - 1) * gap) // rows
	for i, (ph, name, tex, kw) in enumerate(tiles):
		wide = i == len(tiles) - 1 and len(tiles) % 2
		w = SW - 80 if wide else tw
		x = 40 + (0 if wide else (i % 2) * (tw + gap))
		y = top + (i // 2) * (th + gap)
		if ph == "GEMS":
			from icons import GEMS
			grid = Image.new("RGBA", (9 * 18 * 3, 3 * 18 * 3))
			for r in range(3):
				grid.alpha_composite(slot_row([I("gem_" + g) for g in GEMS[r * 9:(r + 1) * 9]], 3), (0, r * 54))
			im = textured_bg(w, th, block("amethyst_block"), px=5, dark=0.28)
			im.alpha_composite(grid, ((w - grid.width) // 2, (th - grid.height) // 2 - 40))
		else:
			im = cover(ph, w, th, **kw)
		im.alpha_composite(fade(th, th - 150, th, color=(0, 0, 0), max_alpha=200, w=w), (0, 0))
		f, off = framed(im)
		c.alpha_composite(f, (x - off, y - off))
		s = 5
		L = logo(name, s, block(tex))
		while L.width > w - 30 and s > 2:
			s -= 1
			L = logo(name, s, block(tex))
		c.alpha_composite(L, (x + (w - L.width) // 2, y + th - L.height - 12))
	c.convert("RGB").save(out, quality=95)


if __name__ == "__main__":
	OUT.mkdir(parents=True, exist_ok=True)
	if len(sys.argv) == 1:
		for old in OUT.glob("p*.png"):
			old.unlink()
	for n in sys.argv[1:] or list(POSTERS):
		print("poster", n)
		POSTERS[n](OUT / f"{n}.png")
