#!/usr/bin/env python3
"""Mob factory face walls (specs/mob-factory.md, F3): turn each mob's 8x8 face texture into vanilla blocks.

Reads the vanilla textures unpacked by tools/mcsrc.py (build/mcsrc/<version>/vanilla in the main checkout), maps
every face texel to the nearest block from a fixed palette by the block texture's average colour (in a perceptual
colour space), and writes the grids into FactoryFaces.java. Run after a Minecraft update or a palette change:

    python3 tools/factory-faces.py [--mcsrc /path/to/build/mcsrc/26.3/vanilla] [--preview out_dir]

--preview also writes a PNG per face made of the chosen blocks' real textures, to judge the look.
"""
import argparse
import math
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(HERE)
OUT = os.path.join(REPO, 'mods/bubbas_reworked_mobs/src/main/java/io/github/bubba8587/bubbas_reworked_mobs/factory/FactoryFaces.java')

# (mob id, texture, u, v of the head's front face, eye overlay texture or None)
FACES = [
	('zombie', 'zombie/zombie.png', 8, 8, None),
	('skeleton', 'skeleton/skeleton.png', 8, 8, None),
	('creeper', 'creeper/creeper.png', 8, 8, None),
	('spider', 'spider/spider.png', 40, 12, 'spider/spider_eyes.png'),
	('enderman', 'enderman/enderman.png', 8, 8, 'enderman/enderman_eyes.png'),
]

# Blocks the face may use: earthy, stony and sculk-friendly blocks first, concrete and terracotta for strong colours.
# Glowing blocks (shroomlight, redstone lamp, ochre froglight...) only for eye overlays (they're picked separately).
# Flat-looking blocks only (no bricks or cobbles: their seams fight the pixel shapes), so each face reads as clean
# areas of colour. Glowing blocks only for eye overlays (picked separately).
PALETTE = [
	'polished_deepslate', 'polished_blackstone', 'smooth_stone', 'calcite', 'bone_block_side', 'polished_diorite',
	'white_concrete', 'light_gray_concrete', 'gray_concrete', 'black_concrete',
	'green_concrete', 'lime_concrete', 'lime_terracotta', 'green_terracotta', 'moss_block', 'lime_wool',
	'cyan_terracotta', 'cyan_concrete', 'warped_wart_block', 'oxidized_cut_copper', 'weathered_cut_copper',
	'terracotta', 'light_gray_terracotta', 'gray_terracotta',
	'black_terracotta', 'white_terracotta', 'purple_concrete', 'purple_terracotta', 'obsidian',
	'red_concrete', 'red_terracotta', 'nether_wart_block', 'yellow_terracotta', 'orange_terracotta',
]
# Tones per face: few enough that the face reads as shapes, not noise.
# Blocks a face is limited to, where the nearest colour reads wrong (the spider's warm grey came out brown).
ONLY = {'spider': ['black_concrete', 'polished_blackstone', 'gray_concrete', 'polished_deepslate', 'smooth_stone']}
# Eye blocks picked by hand where the nearest glowing block reads wrong (spider eyes must be red).
EYES = {'spider': 'magma'}
TONES = {'zombie': 4, 'skeleton': 3, 'creeper': 3, 'spider': 3, 'enderman': 2}
GLOW = ['shroomlight', 'redstone_lamp_on', 'ochre_froglight_side', 'pearlescent_froglight_side', 'verdant_froglight_side',
	'crying_obsidian', 'magma', 'sea_lantern', 'glowstone']

# Texture name -> block id where they differ.
BLOCK_OF = {
	'basalt_side': 'basalt', 'bone_block_side': 'bone_block', 'sandstone_top': 'sandstone',
	'sculk_catalyst_side': 'sculk_catalyst', 'redstone_lamp_on': 'redstone_lamp[lit=true]', 'ochre_froglight_side': 'ochre_froglight',
	'pearlescent_froglight_side': 'pearlescent_froglight', 'verdant_froglight_side': 'verdant_froglight', 'magma': 'magma_block',
	'stripped_birch_log': 'stripped_birch_log',
}


def to_lab(rgb):
	def lin(c):
		c /= 255.0
		return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4
	r, g, b = (lin(c) for c in rgb)
	x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047
	y = 0.2126 * r + 0.7152 * g + 0.0722 * b
	z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883
	f = lambda t: t ** (1 / 3) if t > 0.008856 else 7.787 * t + 16 / 116
	fx, fy, fz = f(x), f(y), f(z)
	return (116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))


def average(path):
	im = Image.open(path).convert('RGBA')
	w = im.size[0]
	im = im.crop((0, 0, w, w))  # first frame of animated textures
	px = [p for p in im.get_flattened_data() if p[3] > 0]
	return tuple(sum(p[i] for p in px) / len(px) for i in range(3))


def main():
	ap = argparse.ArgumentParser()
	ap.add_argument('--mcsrc', default='/home/user/minecraft/build/mcsrc/26.3/vanilla')
	ap.add_argument('--preview')
	args = ap.parse_args()
	tex = os.path.join(args.mcsrc, 'assets/minecraft/textures')
	palette = [(name, to_lab(average(os.path.join(tex, 'block', name + '.png')))) for name in PALETTE]
	glow = [(name, to_lab(average(os.path.join(tex, 'block', name + '.png')))) for name in GLOW]

	def nearest(rgb, choices):
		lab = to_lab(rgb)
		return min(choices, key=lambda c: math.dist(lab, c[1]))[0]

	grids = {}
	for mob, path, u, v, eyes in FACES:
		face = Image.open(os.path.join(tex, 'entity', path)).convert('RGBA').crop((u, v, u + 8, v + 8))
		eye = Image.open(os.path.join(tex, 'entity', eyes)).convert('RGBA').crop((u, v, u + 8, v + 8)) if eyes else None
		# Posterize: k-means the face's (non-eye) texels into a few tones, stretch them apart in lightness for
		# contrast, then give each tone its nearest block. Eyes (the glowing overlay) all take one glowing block.
		pixels = {}
		eye_rgb = None
		for y in range(8):
			for x in range(8):
				if eye is not None and eye.getpixel((x, y))[3] > 0 and max(eye.getpixel((x, y))[:3]) > 40:
					if eye_rgb is None or max(eye.getpixel((x, y))[:3]) > max(eye_rgb):
						eye_rgb = eye.getpixel((x, y))[:3]
				else:
					pixels[(x, y)] = to_lab(face.getpixel((x, y))[:3])
		k = TONES[mob]
		labs = sorted(pixels.values())
		centres = [labs[int((i + 0.5) * len(labs) / k)] for i in range(k)]
		for _ in range(20):
			groups = [[] for _ in centres]
			for lab in pixels.values():
				groups[min(range(k), key=lambda i: math.dist(lab, centres[i]))].append(lab)
			centres = [tuple(sum(c[j] for c in g) / len(g) for j in range(3)) if g else centres[i] for i, g in enumerate(groups)]
		mean_l = sum(c[0] for c in centres) / k
		stretched = [(max(0.0, min(100.0, mean_l + (c[0] - mean_l) * 1.35)), c[1] * 1.15, c[2] * 1.15) for c in centres]
		block_of_tone = [min([b for b in palette if mob not in ONLY or b[0] in ONLY[mob]], key=lambda b: math.dist(c, b[1]))[0] for c in stretched]
		eye_block = EYES.get(mob) or (nearest(eye_rgb, glow) if eye_rgb else None)
		rows = []
		for y in range(8):
			row = []
			for x in range(8):
				if (x, y) not in pixels:
					row.append(eye_block)
				else:
					lab = pixels[(x, y)]
					row.append(block_of_tone[min(range(k), key=lambda i: math.dist(lab, centres[i]))])
			rows.append(row)
		grids[mob] = rows
		print(mob)
		for row in rows:
			print('  ' + ' '.join('%-22s' % r for r in row))

	if args.preview:
		os.makedirs(args.preview, exist_ok=True)
		for mob, rows in grids.items():
			img = Image.new('RGBA', (8 * 32, 8 * 32))
			for y, row in enumerate(rows):
				for x, name in enumerate(row):
					t = Image.open(os.path.join(tex, 'block', name + '.png')).convert('RGBA')
					t = t.crop((0, 0, t.size[0], t.size[0])).resize((16, 16), Image.NEAREST)
					for dy in (0, 16):
						for dx in (0, 16):
							img.paste(t, (x * 32 + dx, y * 32 + dy))
			img.save(os.path.join(args.preview, mob + '-wall.png'))

	names = sorted({n for rows in grids.values() for row in rows for n in row})
	key = {n: chr(ord('A') + i) if i < 26 else chr(ord('a') + i - 26) for i, n in enumerate(names)}
	lines = []
	lines.append('// dte:C1')
	lines.append('// GENERATED by tools/factory-faces.py from the vanilla mob textures; do not edit by hand.')
	lines.append('package io.github.bubba8587.bubbas_reworked_mobs.factory;')
	lines.append('')
	lines.append('import java.util.Map;')
	lines.append('')
	lines.append('/** Each factory mob\'s face as an 8 x 8 grid of vanilla blocks, top row first (specs/mob-factory.md, F3). */')
	lines.append('public final class FactoryFaces {')
	lines.append('\t/** Letter to block id (with block state where needed). */')
	lines.append('\tpublic static final Map<Character, String> BLOCKS = Map.ofEntries(')
	lines.append(',\n'.join('\t\t\tMap.entry(\'%s\', "minecraft:%s")' % (key[n], BLOCK_OF.get(n, n)) for n in names) + ');')
	lines.append('')
	lines.append('\t/** Mob id path to its face rows. */')
	lines.append('\tpublic static final Map<String, String[]> FACES = Map.of(')
	entries = []
	for mob, rows in grids.items():
		entries.append('\t\t\t"%s", new String[] {\n%s }' % (mob, ',\n'.join('\t\t\t\t\t"%s"' % ''.join(key[n] for n in row) for row in rows)))
	lines.append(',\n'.join(entries) + ');')
	lines.append('')
	lines.append('\tprivate FactoryFaces() {')
	lines.append('\t}')
	lines.append('}')
	os.makedirs(os.path.dirname(OUT), exist_ok=True)
	with open(OUT, 'w') as f:
		f.write('\n'.join(lines) + '\n')
	print('wrote', OUT)


if __name__ == '__main__':
	sys.exit(main())
