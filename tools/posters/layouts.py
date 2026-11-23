# dte:A3
"""Poster layouts (specs/posters.md). Each takes photos (paths or images) and draws a 1080x1350 poster."""
from design import *

W, H = 1080, 1350
GOLD = (255, 170, 0)
GRAY = (200, 200, 200)
DIM = (120, 120, 120)


def _label(canvas, x, y, text, scale=3, anchor="l"):
	t = tooltip([(text, (255, 255, 255))], scale)
	if anchor == "r":
		x -= t.width
	elif anchor == "c":
		x -= t.width // 2
	canvas.alpha_composite(t, (x, y))
	return t


def place_photo(canvas, photo, x, y, w, h, label=None, label_pos="bl", **kw):
	"""A framed photo with its top-left content corner at (x, y), and a tooltip label in a corner."""
	im = cover(photo, w, h, **kw)
	f, off = framed(im)
	canvas.alpha_composite(f, (x - off, y - off))
	if label:
		t = tooltip([(label, (255, 255, 255))], 3)
		lx = x + 12 if "l" in label_pos else x + w - t.width - 12
		ly = y + h - t.height - 12 if "b" in label_pos else y + 12
		canvas.alpha_composite(t, (lx, ly))


def header(canvas, y=26, text="BUBBA'S"):
	draw_text(canvas, (W // 2, y), text, 4, GOLD, anchor="c")


def footer(canvas, text="Fabric mods for Minecraft 26.3"):
	draw_text(canvas, (W // 2, H - 38), text, 2, DIM, anchor="c")


def title_block(canvas, title, tex, cy, scale=9, splash_text=None, blurb=None, max_w=W - 60):
	"""Logo centred on cy, splash on its top right, blurb under it. Returns the y under the block."""
	s = scale
	L = logo(title, s, tex)
	while L.width > max_w and s > 4:
		s -= 1
		L = logo(title, s, tex)
	x = (W - L.width) // 2
	y = cy - L.height // 2
	canvas.alpha_composite(L, (x, y))
	if splash_text:
		for sc, ang in ((4, 20), (3, 20), (3, 14), (3, 9), (2, 9)):
			sp = splash(splash_text, sc, ang)
			if y - sp.height + 26 >= 4:
				break
		sx = min(W - sp.width - 8, x + L.width - sp.width + 40)
		canvas.alpha_composite(sp, (sx, max(4, y - sp.height + 26)))
	bottom = y + L.height
	if blurb:
		for line in wrap(blurb, 3, W - 120):
			draw_text(canvas, (W // 2, bottom + 10), line, 3, GRAY, anchor="c")
			bottom += 30
		bottom += 10
	return bottom


def hero(out, hero_photo, title, tex, bg_tex, insets, splash_text=None, blurb=None, icons=None, hero_h=780,
		hero_kw=None, title_scale=9, bg_px=6, inset_h=None, sub="BUBBA'S"):
	"""Full-bleed hero shot on top fading into a block-textured background; logo over the seam; a row of framed
	insets; a hotbar of the mod's items."""
	c = textured_bg(W, H, block(bg_tex), px=bg_px)
	c.alpha_composite(cover(hero_photo, W, hero_h, **(hero_kw or {})), (0, 0))
	c.alpha_composite(fade(hero_h, hero_h - 330, hero_h, color=(8, 8, 10)), (0, 0))
	c.alpha_composite(fade(170, 170, 0, color=(0, 0, 0), max_alpha=150), (0, 0))
	header(c, text=sub)
	y = title_block(c, title, block(tex), hero_h - 120, title_scale, splash_text, blurb)
	bottom = H - (150 if icons else 80)
	n = len(insets)
	gap = 26
	iw = (W - 80 - gap * (n - 1)) // n
	ih = inset_h or (bottom - y - 30)
	top = y + 14
	for i, ins in enumerate(insets):
		p, lab = ins[0], ins[1]
		kw = ins[2] if len(ins) > 2 else {}
		place_photo(c, p, 40 + i * (iw + gap), top, iw, ih, lab, **kw)
	if icons:
		hb = hotbar(icons, 4)
		c.alpha_composite(hb, ((W - hb.width) // 2, H - 140))
	footer(c)
	c.convert("RGB").save(out, quality=95)
	return c


def mosaic(out, title, tex, bg_tex, big, smalls, splash_text=None, blurb=None, icons=None, title_scale=9,
		bg_px=6, sub="BUBBA'S", big_h=520):
	"""Title on top over the textured background, a big framed shot, then a row of smaller ones."""
	c = textured_bg(W, H, block(bg_tex), px=bg_px)
	header(c, text=sub)
	y = title_block(c, title, block(tex), 130, title_scale, splash_text, blurb)
	p, lab = big[0], big[1]
	kw = big[2] if len(big) > 2 else {}
	place_photo(c, p, 40, y + 14, W - 80, big_h, lab, **kw)
	y2 = y + 14 + big_h + 30
	bottom = H - (150 if icons else 80)
	n = len(smalls)
	gap = 26
	iw = (W - 80 - gap * (n - 1)) // n
	for i, ins in enumerate(smalls):
		p, lab = ins[0], ins[1]
		kw = ins[2] if len(ins) > 2 else {}
		place_photo(c, p, 40 + i * (iw + gap), y2, iw, bottom - y2 - 10, lab, **kw)
	if icons:
		hb = hotbar(icons, 4)
		c.alpha_composite(hb, ((W - hb.width) // 2, H - 140))
	footer(c)
	c.convert("RGB").save(out, quality=95)
	return c
