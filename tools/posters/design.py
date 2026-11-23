# dte:A3
"""Drawing kit for promo posters (specs/posters.md): Minecraft-font text, block-textured logo titles, vanilla-style
tooltips, splash text, hotbars and framed screenshots. Vanilla textures come straight from the game jar Loom caches;
mod textures from the repo. Used by tools/posters/make.py."""
import io, math, zipfile
from functools import lru_cache
from pathlib import Path
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageChops

ROOT = Path(__file__).resolve().parents[2]
JAR = Path.home() / ".gradle/caches/fabric-loom/26.3/minecraft-client.jar"


@lru_cache(maxsize=None)
def _jar():
	return zipfile.ZipFile(JAR)


@lru_cache(maxsize=None)
def vanilla(path):
	"""A vanilla asset, e.g. 'textures/block/stone.png'."""
	return Image.open(io.BytesIO(_jar().read("assets/minecraft/" + path))).convert("RGBA")


def block(name):
	im = vanilla(f"textures/block/{name}.png")
	return im.crop((0, 0, 16, 16)) if im.height > 16 else im


def mod_texture(mod, path):
	return Image.open(ROOT / "mods" / mod / "src/main/resources/assets" / mod / "textures" / path).convert("RGBA")


# --- Minecraft font --------------------------------------------------------------------------------------------

@lru_cache(maxsize=None)
def _glyphs():
	font = vanilla("textures/font/ascii.png")
	out = {}
	for code in range(32, 127):
		g = font.crop(((code % 16) * 8, (code // 16) * 8, (code % 16) * 8 + 8, (code // 16) * 8 + 8))
		a = g.getchannel("A")
		w = 0
		for x in range(8):
			if any(a.getpixel((x, y)) for y in range(8)):
				w = x + 1
		out[chr(code)] = (a, 3 if code == 32 else w)
	return out


def text_mask(s, bold=False):
	"""The text as a 1-bit-ish L mask at font resolution (8 px tall), and its width."""
	gl = _glyphs()
	w = sum(gl.get(c, gl["?"])[1] + 1 + (1 if bold else 0) for c in s)
	m = Image.new("L", (max(w, 1), 8), 0)
	x = 0
	for c in s:
		a, gw = gl.get(c, gl["?"])
		m.paste(255, (x, 0), a)
		if bold:
			m.paste(255, (x + 1, 0), a)
		x += gw + 1 + (1 if bold else 0)
	return m, w


def text_width(s, scale, bold=False):
	return text_mask(s, bold)[1] * scale


def draw_text(img, xy, s, scale, color=(255, 255, 255), shadow=True, bold=False, anchor="l", alpha=255):
	m, w = text_mask(s, bold)
	m = m.resize((m.width * scale, m.height * scale), Image.NEAREST)
	x, y = xy
	if anchor == "c":
		x -= m.width // 2
	elif anchor == "r":
		x -= m.width
	if shadow:
		sc = tuple(int(c * 0.25) for c in color[:3])
		img.paste(Image.new("RGBA", m.size, sc + (alpha,)), (x + scale, y + scale), m)
	img.paste(Image.new("RGBA", m.size, tuple(color[:3]) + (alpha,)), (x, y), m)
	return m.width


def wrap(s, scale, maxw):
	lines, cur = [], ""
	for word in s.split():
		t = (cur + " " + word).strip()
		if text_width(t, scale) > maxw and cur:
			lines.append(cur)
			cur = word
		else:
			cur = t
	if cur:
		lines.append(cur)
	return lines


# --- Logo titles -----------------------------------------------------------------------------------------------

def _tile(tex, size, px):
	"""Tile a 16x16 texture so each texture pixel is px screen pixels."""
	t = tex.resize((16 * px, 16 * px), Image.NEAREST)
	out = Image.new("RGBA", size)
	for x in range(0, size[0], t.width):
		for y in range(0, size[1], t.height):
			out.paste(t, (x, y))
	return out


def _shift(m, dx, dy):
	out = Image.new("L", m.size, 0)
	out.paste(m, (dx, dy))
	return out


def _dilate(m, r):
	return m.filter(ImageFilter.MaxFilter(r * 2 + 1)) if r > 0 else m


def logo(s, scale, tex, depth=None, face_bright=1.1, side_bright=0.45, outline=(12, 10, 14), bold=False):
	"""A Minecraft-logo style title: every pixel of the Minecraft-font letters is a textured block with a bevel,
	extruded downward and outlined. scale: screen pixels per letter pixel (one block). Returns an RGBA image."""
	m, _ = text_mask(s, bold=bold)
	pad = 3
	small = Image.new("L", (m.width + pad * 2, m.height + pad * 2 + 3), 0)
	small.paste(m, (pad, pad))
	big = small.resize((small.width * scale, small.height * scale), Image.NEAREST)
	depth = depth if depth is not None else max(3, int(scale * 0.7))
	step = max(1, scale // 5)
	side = Image.new("L", big.size, 0)
	for k in range(step, depth + 1, step):
		side = ImageChops.lighter(side, _shift(big, 0, k))
	shape = ImageChops.lighter(big, side)
	ol = _dilate(shape, max(2, scale // 4))
	w, h = big.size
	out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
	sh = _shift(ol, scale // 2, scale // 2).filter(ImageFilter.GaussianBlur(scale * 0.8))
	out.paste(Image.new("RGBA", (w, h), (0, 0, 0, 170)), (0, 0), sh)
	out.paste(Image.new("RGBA", (w, h), outline + (255,)), (0, 0), ol)
	# one texture per block, aligned to the letter grid
	t = tex.resize((scale, scale), Image.LANCZOS if scale < 16 else Image.NEAREST).convert("RGB")
	t = ImageEnhance.Contrast(t).enhance(1.15)
	ftex = Image.new("RGB", (w, h))
	for x in range(0, w, scale):
		for y in range(0, h, scale):
			ftex.paste(t, (x, y))
	stex = ImageEnhance.Brightness(ftex).enhance(side_bright).convert("RGBA")
	out.paste(stex, (0, 0), side)
	face = ImageEnhance.Brightness(ftex).enhance(face_bright).convert("RGBA")
	out.paste(face, (0, 0), big)
	# per-block bevel: light top/left edges, dark bottom/right edges
	e = max(1, scale // 7)
	bev = Image.new("RGBA", (w, h), (0, 0, 0, 0))
	d = ImageDraw.Draw(bev)
	px = small.load()
	for by in range(small.height):
		for bx in range(small.width):
			if px[bx, by] > 127:
				x0, y0 = bx * scale, by * scale
				d.rectangle((x0, y0, x0 + scale - 1, y0 + e - 1), fill=(255, 255, 255, 80))
				d.rectangle((x0, y0, x0 + e - 1, y0 + scale - 1), fill=(255, 255, 255, 55))
				d.rectangle((x0, y0 + scale - e, x0 + scale - 1, y0 + scale - 1), fill=(0, 0, 0, 70))
				d.rectangle((x0 + scale - e, y0, x0 + scale - 1, y0 + scale - 1), fill=(0, 0, 0, 55))
	out.alpha_composite(bev)
	return out.crop(out.getbbox())


def splash(s, scale, angle=20, color=(255, 255, 0)):
	"""Title-screen splash text: yellow with a dark shadow, tilted."""
	w = text_width(s, scale) + scale * 2
	im = Image.new("RGBA", (w, 10 * scale), (0, 0, 0, 0))
	draw_text(im, (0, 0), s, scale, color, shadow=True)
	return im.rotate(angle, resample=Image.NEAREST, expand=True)


# --- GUI pieces ------------------------------------------------------------------------------------------------

def tooltip(lines, scale, width=None, pad=None):
	"""A vanilla tooltip box: dark purple-black with the purple gradient border. lines: [(text, color), ...]."""
	pad = pad or 3
	tw = max(text_width(t, 1) for t, _ in lines)
	inner_w = width // scale if width else tw + pad * 2
	inner_h = len(lines) * 10 + pad * 2 - 2 + (2 if len(lines) > 1 else 0)
	W, H = inner_w + 2, inner_h + 2
	small = Image.new("RGBA", (W, H), (0, 0, 0, 0))
	d = ImageDraw.Draw(small)
	bg = (16, 0, 16, 240)
	d.rectangle((1, 0, W - 2, H - 1), fill=bg)
	d.rectangle((0, 1, W - 1, H - 2), fill=bg)
	for y in range(1, H - 1):
		t = (y - 1) / max(1, H - 3)
		c = (int(0x50 + (0x28 - 0x50) * t), 0, int(0xFF + (0x7F - 0xFF) * t), 255)
		d.point((1, y), fill=c)
		d.point((W - 2, y), fill=c)
	d.line((1, 1, W - 2, 1), fill=(0x50, 0, 0xFF, 255))
	d.line((1, H - 2, W - 2, H - 2), fill=(0x28, 0, 0x7F, 255))
	im = small.resize((W * scale, H * scale), Image.NEAREST)
	y = (pad + 1) * scale
	for i, (t, c) in enumerate(lines):
		draw_text(im, ((pad + 1) * scale, y), t, scale, c)
		y += 10 * scale + (2 * scale if i == 0 and len(lines) > 1 else 0)
	return im


def button(s, scale, width_px):
	"""A vanilla button with centred label."""
	b = vanilla("textures/gui/sprites/widget/button.png")
	w = width_px // scale
	small = Image.new("RGBA", (w, 20))
	small.paste(b.crop((0, 0, 100, 20)), (0, 0))
	small.paste(b.crop((200 - (w - 100), 0, 200, 20)) if w > 100 else b.crop((200 - w // 2, 0, 200, 20)), (max(100, w // 2), 0))
	im = small.resize((w * scale, 20 * scale), Image.NEAREST)
	draw_text(im, (im.width // 2, 6 * scale), s, scale, (255, 255, 255), anchor="c")
	return im


def hotbar(icons, scale, selected=None):
	"""The vanilla hotbar with up to 9 item icons (16x16 RGBA) in it."""
	hb = vanilla("textures/gui/sprites/hud/hotbar.png")
	small = hb.copy()
	for i, ic in enumerate(icons[:9]):
		if ic is not None:
			small.alpha_composite(ic.resize((16, 16), Image.NEAREST), (3 + 20 * i, 3))
	if selected is not None:
		sel = vanilla("textures/gui/sprites/hud/hotbar_selection.png")
		pad = Image.new("RGBA", (small.width + 2, small.height + 2))
		pad.paste(small, (1, 1))
		pad.alpha_composite(sel, (20 * selected, 0))
		small = pad
	return small.resize((small.width * scale, small.height * scale), Image.NEAREST)


def slot_row(icons, scale, gap=0):
	"""Item icons in vanilla inventory slots (18x18 each)."""
	sl = vanilla("textures/gui/sprites/container/slot.png")
	n = len(icons)
	small = Image.new("RGBA", (n * 18 + (n - 1) * gap, 18))
	for i, ic in enumerate(icons):
		small.paste(sl, (i * (18 + gap), 0))
		if ic is not None:
			small.alpha_composite(ic.resize((16, 16), Image.NEAREST), (i * (18 + gap) + 1, 1))
	return small.resize((small.width * scale, small.height * scale), Image.NEAREST)


def icon(im, size):
	return im.resize((size, size), Image.NEAREST)


# --- Photos ----------------------------------------------------------------------------------------------------

def cover(path_or_im, w, h, focus=(0.5, 0.5), crop=None, bright=1.0, contrast=1.0, sat=1.0):
	im = path_or_im if isinstance(path_or_im, Image.Image) else Image.open(path_or_im)
	im = im.convert("RGB")
	if crop:
		im = im.crop(crop)
	r = max(w / im.width, h / im.height)
	im = im.resize((max(w, round(im.width * r)), max(h, round(im.height * r))), Image.LANCZOS)
	l = int((im.width - w) * focus[0])
	t = int((im.height - h) * focus[1])
	im = im.crop((l, t, l + w, t + h))
	if bright != 1.0:
		im = ImageEnhance.Brightness(im).enhance(bright)
	if contrast != 1.0:
		im = ImageEnhance.Contrast(im).enhance(contrast)
	if sat != 1.0:
		im = ImageEnhance.Color(im).enhance(sat)
	return im.convert("RGBA")


def framed(photo, border=5, shadow=14):
	"""A photo with a crisp dark frame, a thin light inner edge and a soft drop shadow (returns image, offset)."""
	w, h = photo.size
	W, H = w + border * 2 + shadow * 2, h + border * 2 + shadow * 2
	out = Image.new("RGBA", (W, H), (0, 0, 0, 0))
	sh = Image.new("L", (W, H), 0)
	ImageDraw.Draw(sh).rectangle((shadow + 4, shadow + 8, W - shadow + 4, H - shadow + 8), fill=180)
	sh = sh.filter(ImageFilter.GaussianBlur(shadow * 0.6))
	out.paste(Image.new("RGBA", (W, H), (0, 0, 0, 255)), (0, 0), sh)
	d = ImageDraw.Draw(out)
	d.rectangle((shadow, shadow, W - shadow - 1, H - shadow - 1), fill=(14, 12, 16, 255))
	out.paste(photo, (shadow + border, shadow + border))
	d.rectangle((shadow + border, shadow + border, shadow + border + w - 1, shadow + border + h - 1), outline=(255, 255, 255, 46), width=2)
	return out, shadow + border


def fade(h, start, end, top=True, color=(0, 0, 0), max_alpha=255, w=1080):
	"""A vertical gradient overlay: transparent at start, color at end."""
	g = Image.new("RGBA", (w, h), color + (0,))
	px = g.load()
	for y in range(h):
		if (y - start) * (1 if end > start else -1) <= 0:
			a = 0
		else:
			t = min(1.0, abs(y - start) / max(1, abs(end - start)))
			a = int(max_alpha * (t * t * (3 - 2 * t)))
		for x in range(w):
			px[x, y] = color + (a,)
	return g


def textured_bg(w, h, tex, px=6, dark=0.32, tint=None):
	bg = _tile(tex, (w, h), px).convert("RGB")
	bg = ImageEnhance.Brightness(bg).enhance(dark)
	if tint:
		bg = Image.blend(bg, Image.new("RGB", (w, h), tint), 0.25)
	v = Image.new("L", (w, h), 0)
	ImageDraw.Draw(v).ellipse((-w * 0.4, -h * 0.25, w * 1.4, h * 1.25), fill=255)
	v = v.filter(ImageFilter.GaussianBlur(160))
	return Image.composite(bg, Image.new("RGB", (w, h), (0, 0, 0)), v).convert("RGBA")


def tooltip_crop(path, scale_to=None):
	"""Cuts the in-game tooltip out of a screenshot (GUI scale 4): finds its purple border lines, then redraws a
	clean vanilla frame around the text."""
	im = Image.open(path).convert("RGB")
	w, h = im.size
	px = im.load()

	def border(c):
		r, g, b = c
		return g < 14 and b >= 48 and 16 <= r <= 96 and b > 1.6 * r

	runs = []
	for y in range(h):
		x = 0
		while x < w:
			if border(px[x, y]):
				s = x
				while x < w and border(px[x, y]):
					x += 1
				if x - s >= 60:
					runs.append((y, s, x))
			else:
				x += 1
	if not runs:
		return None
	y0, y1 = min(r[0] for r in runs), max(r[0] for r in runs)
	x0, x1 = min(r[1] for r in runs), max(r[2] for r in runs)
	g = 4
	x0, y0 = x0 // g * g - g, y0 // g * g - g
	x1, y1 = -(-x1 // g) * g + g, -(-(y1 + 1) // g) * g + g
	out = im.crop((x0, y0, x1, y1)).convert("RGBA")
	out = out.resize((out.width // g, out.height // g), Image.NEAREST)
	inner = out.crop((3, 3, out.width - 3, out.height - 3))
	Wd, Hd = inner.width + 6, inner.height + 6
	f = Image.new("RGBA", (Wd, Hd), (0, 0, 0, 0))
	d = ImageDraw.Draw(f)
	bg = (16, 0, 16, 245)
	d.rectangle((1, 0, Wd - 2, Hd - 1), fill=bg)
	d.rectangle((0, 1, Wd - 1, Hd - 2), fill=bg)
	for y in range(1, Hd - 1):
		k = (y - 1) / max(1, Hd - 3)
		col = (int(0x50 + (0x28 - 0x50) * k), 0, int(0xFF + (0x7F - 0xFF) * k), 255)
		d.point((1, y), fill=col)
		d.point((Wd - 2, y), fill=col)
	d.line((1, 1, Wd - 2, 1), fill=(0x50, 0, 0xFF, 255))
	d.line((1, Hd - 2, Wd - 2, Hd - 2), fill=(0x28, 0, 0x7F, 255))
	f.paste(inner, (3, 3))
	return f.resize((f.width * scale_to, f.height * scale_to), Image.NEAREST) if scale_to else f
