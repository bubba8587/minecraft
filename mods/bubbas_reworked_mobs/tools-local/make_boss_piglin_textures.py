# dte:C1,B16,B17
"""Boss Piglin art (specs/boss-piglin.md): the fat piglin skin and its cape, 16x and 32x.

The box layout here mirrors BossPiglinModel / BossPiglinCapeLayer (src/client); `--java` prints the cube code
for BossPiglinModel.createBodyLayer so the two stay in step. UVs are packed here, deterministically.
The skin is painted in 3D: every texel is mapped back to its point on the model, so vest, belt, belly and
shading line up across faces. The head is painted the same way (piglin palette), at 16x and properly at 32x.

Usage: python3 make_boss_piglin_textures.py [--java] [vanilla_textures_dir]
(default: build/mcsrc/<version>/vanilla/assets/minecraft/textures of the main checkout). Needs Pillow.
"""
import math
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
MOD = "bubbas_reworked_mobs"
RES = Path(__file__).resolve().parent.parent / "src/main/resources"
OUT16 = RES / f"assets/{MOD}/textures"
OUT32 = RES / f"resourcepacks/{MOD}_32x/assets/{MOD}/textures"

# --- model layout (model units; y down; standing feet at y=24) --------------------------------------------------------

TEX_W, TEX_H = 128, 128
HIP = 17
ARM_X = 12
# part: (pivot, rest rotation, [(name, x, y, z, w, h, d)...]) relative to the pivot; UVs fixed or packed below
LAYOUT = {
	"head": ((0, 3, -1.5), (0, 0, 0), [
		("head", -5, -8, -4, 10, 8, 8),
		("snout", -3.5, -4, -6, 7, 4, 2),
		("brow", -4, -6, -5, 8, 1, 1),
		("tusk_r", -4.5, -3, -6, 1, 3, 1),
		("tusk_l", 3.5, -3, -6, 1, 3, 1),
	]),
	"left_ear": ((4.5, -6, 0), (0, 0, -0.35), [("ear", 0, 0, -2, 1, 5, 4)]),
	"right_ear": ((-4.5, -6, 0), (0, 0, 0.35), [("ear", -1, 0, -2, 1, 5, 4)]),
	"body": ((0, HIP, 0), (0, 0, 0), [
		("torso", -10, -15, -7, 20, 15, 14),
		("girth", -11, -11, -8, 22, 9, 16),
		("belly", -9, -12, -10, 18, 11, 3),
		("belly_front", -7, -10.5, -12, 14, 8, 2),
		("rear", -8, -6, 8, 16, 6, 2),
		("back", -8, -14, 7, 16, 9, 2),
		("shoulder_r", -10, -16, -4.5, 6, 3, 9),
		("shoulder_l", 4, -16, -4.5, 6, 3, 9),
	]),
	"right_arm": ((-ARM_X, 4, 0), (0, 0, 0), [("arm", -4, -2, -3, 6, 12, 6)]),
	"left_arm": ((ARM_X, 4, 0), (0, 0, 0), [("arm", -2, -2, -3, 6, 12, 6)]),
	"right_leg": ((-5, HIP, 0), (0, 0, 0), [("leg", -3.5, 0, -3.5, 7, 7, 7)]),
	"left_leg": ((5, HIP, 0), (0, 0, 0), [("leg", -3.5, 0, -3.5, 7, 7, 7)]),
}
FIXED_UV = {
	("head", "head"): (0, 0), ("left_ear", "ear"): (51, 6), ("right_ear", "ear"): (39, 6),
	("body", "torso"): (0, 16), ("body", "girth"): (0, 45), ("body", "belly"): (68, 16), ("body", "belly_front"): (68, 30),
	("body", "rear"): (0, 70), ("body", "back"): (36, 70), ("body", "shoulder_r"): (64, 0), ("body", "shoulder_l"): (94, 0),
	("right_arm", "arm"): (0, 81), ("left_arm", "arm"): (24, 81), ("right_leg", "leg"): (48, 81), ("left_leg", "leg"): (76, 81),
}


def pack():
	"""Shelf-pack every cube's UV net into the texture; returns {(part, name): (u, v)}."""
	used = [[False] * TEX_W for _ in range(TEX_H)]

	def free(u, v, w, h):
		if u + w > TEX_W or v + h > TEX_H:
			return False
		return all(not used[y][x] for y in range(v, v + h) for x in range(u, u + w))

	def take(u, v, w, h):
		for y in range(v, v + h):
			for x in range(u, u + w):
				used[y][x] = True

	out = {}
	jobs = []
	for part, (_p, _r, cubes) in LAYOUT.items():
		for (name, _x, _y, _z, w, h, d) in cubes:
			nw, nh = 2 * (w + d), d + h
			if (part, name) in FIXED_UV:
				u, v = FIXED_UV[(part, name)]
				take(u, v, nw, nh)
				out[(part, name)] = (u, v)
			else:
				jobs.append((nh, nw, part, name))
	jobs.sort(key=lambda j: (-j[0], -j[1], j[2], j[3]))
	for nh, nw, part, name in jobs:
		for v in range(TEX_H):
			hit = next((u for u in range(TEX_W) if free(u, v, nw, nh)), None)
			if hit is not None:
				take(hit, v, nw, nh)
				out[(part, name)] = (hit, v)
				break
		else:
			raise SystemExit(f"texture full: {part}/{name}")
	return out


UV = pack()
PARTS = {part: (pivot, rot, [UV[(part, c[0])] + tuple(c[1:]) for c in cubes]) for part, (pivot, rot, cubes) in LAYOUT.items()}
NAMES = {part: [c[0] for c in cubes] for part, (_p, _r, cubes) in LAYOUT.items()}

CAPE_W, CAPE_H = 64, 32
CAPE_CUBE = (0, 0, -10, 0, -1, 20, 20, 1)


def java():
	"""The createBodyLayer cube code for BossPiglinModel."""
	def num(f):
		return f"{float(f)}F"

	lines = []
	for part, (_p, _r, cubes) in PARTS.items():
		key = "left_ear" if part == "left_ear" else part
		lines.append(f'\t\t\tcase "{key}" -> CubeListBuilder.create()')
		for i, (u, v, x, y, z, w, h, d) in enumerate(cubes):
			end = ";" if i == len(cubes) - 1 else ""
			lines.append(f"\t\t\t\t.texOffs({u}, {v}).addBox({num(x)}, {num(y)}, {num(z)}, {num(w)}, {num(h)}, {num(d)}){end}  // {NAMES[part][i]}")
	return "\n".join(lines)


def faces(cube, grow=(0, 0, 0), mirror=False):
	"""ModelPart.Cube's six polygons: (direction, [(x, y, z, u, v)] * 4), u/v in texels."""
	u, v, x, y, z, w, h, d = cube
	minx, miny, minz = x - grow[0], y - grow[1], z - grow[2]
	maxx, maxy, maxz = x + w + grow[0], y + h + grow[1], z + d + grow[2]
	if mirror:
		minx, maxx = maxx, minx
	t0, t1, t2, t3 = (minx, miny, minz), (maxx, miny, minz), (maxx, maxy, minz), (minx, maxy, minz)
	l0, l1, l2, l3 = (minx, miny, maxz), (maxx, miny, maxz), (maxx, maxy, maxz), (minx, maxy, maxz)
	u0, u1, u2, u22, u3, u4 = u, u + d, u + d + w, u + d + w + w, u + d + w + d, u + d + w + d + w
	v0, v1, v2 = v, v + d, v + d + h
	polys = [
		("down", [l1, l0, t0, t1], (u1, v0, u2, v1)),
		("up", [t2, t3, l3, l2], (u2, v1, u22, v0)),
		("west", [t0, l0, l3, t3], (u0, v1, u1, v2)),
		("north", [t1, t0, t3, t2], (u1, v1, u2, v2)),
		("east", [l1, t1, t2, l2], (u2, v1, u3, v2)),
		("south", [l0, l1, l2, l3], (u3, v1, u4, v2)),
	]
	out = []
	for name, vs, (a0, b0, a1, b1) in polys:
		uvs = [(a1, b0), (a0, b0), (a0, b1), (a1, b1)]
		quad = [vs[i] + uvs[i] for i in range(4)]
		if mirror:
			quad = quad[::-1]
		out.append((name, quad))
	return out


NORMALS = {"down": (0, -1, 0), "up": (0, 1, 0), "west": (-1, 0, 0), "east": (1, 0, 0), "north": (0, 0, -1),
		"south": (0, 0, 1)}
# model space is y-down: the "up" face points to -y
MODEL_N = {"down": (0, 1, 0), "up": (0, -1, 0), "west": (-1, 0, 0), "east": (1, 0, 0), "north": (0, 0, -1),
		"south": (0, 0, 1)}


def texels(quad, r):
	"""Every texel (at resolution factor r) of a face with the model point at its centre."""
	us = [q[3] for q in quad]
	vs = [q[4] for q in quad]
	umin, umax, vmin, vmax = min(us), max(us), min(vs), max(vs)

	def corner(cu, cv):
		return next(q[:3] for q in quad if q[3] == cu and q[4] == cv)

	a, b, c, d = corner(umin, vmin), corner(umax, vmin), corner(umin, vmax), corner(umax, vmax)
	for iv in range(int(round(vmin * r)), int(round(vmax * r))):
		for iu in range(int(round(umin * r)), int(round(umax * r))):
			s = ((iu + 0.5) / r - umin) / (umax - umin)
			t = ((iv + 0.5) / r - vmin) / (vmax - vmin)
			p = tuple(a[k] * (1 - s) * (1 - t) + b[k] * s * (1 - t) + c[k] * (1 - s) * t + d[k] * s * t for k in range(3))
			yield iu, iv, p, s, t


# --- palette ----------------------------------------------------------------------------------------------------------


def hx(s):
	return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


SKIN = [hx(c) for c in ("f2ba86", "e8a074", "d5957a", "c98265", "ba6645", "ad5532", "974322")]
LEATHER = [hx(c) for c in ("a2593a", "844b33", "6b3924", "532b1a", "3a1a0c")]
GOLD = [hx(c) for c in ("fffbb0", "f5da2a", "de8a1c", "a5590c")]
HOOF = [hx(c) for c in ("4a463a", "3c392e", "2b2824", "211e1c")]
IVORY = [hx(c) for c in ("fff4d6", "e8dcb8", "c9bb94")]
SCAR = hx("f6cfa4")
RED = [hx(c) for c in ("d1453b", "b02e25", "8e1f1a", "6e1511", "4c0c0a")]
EYE = hx("fff4d6")
NOSTRIL = [hx(c) for c in ("7a2621", "a13e38", "5a1714")]


def mid(a, b):
	return tuple((a[k] + b[k]) // 2 for k in range(3))


def fine(r):
	"""A ramp with half steps (the 32x set's softer, Classic-Faithful-style gradients)."""
	out = []
	for i, c in enumerate(r):
		out.append(c)
		if i + 1 < len(r):
			out.append(mid(c, r[i + 1]))
	return out


def ramp(rp, i, res=1):
	if res == 2:
		f = fine(rp)
		return f[max(0, min(len(f) - 1, int(round(i * 2))))]
	return rp[max(0, min(len(rp) - 1, int(round(i))))]


def h01(x, y, seed=0):
	n = (x * 374761393 + y * 668265263 + seed * 2147483647) & 0xFFFFFFFF
	n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
	return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def grain(iu, iv, r, seed):
	"""-1/0/+1 mottling; two-pixel clumps at 32x (Classic Faithful style)."""
	if r == 2:
		iu, iv = (iu + (iv // 2) % 2) // 2, iv
	n = h01(iu, iv, seed)
	return -1 if n < 0.18 else (1 if n > 0.80 else 0)


# --- lighting: shading for volume -------------------------------------------------------------------------------------

LIGHT = (-0.3, -0.75, -0.6)  # from above, front, a little left (model space, y down)
_ln = math.sqrt(sum(c * c for c in LIGHT))
LIGHT = tuple(c / _ln for c in LIGHT)
GUT = ((0.0, 11.0, -1.0), (12.0, 6.5, 13.0))  # centre and radii of the belly's ellipsoid (world model units)


def lit(n):
	return sum(n[k] * LIGHT[k] for k in range(3))


def gut_normal(p, face):
	"""Normal of the belly's ellipsoid under this point (so the stepped boxes shade as one round mass)."""
	c, rr = GUT
	dx, dy = (p[0] - c[0]) / rr[0], (p[1] - c[1]) / rr[1]
	q = 1 - dx * dx - dy * dy
	fn = MODEL_N[face]
	if q <= 0.02 or fn[1] != 0:
		g = [dx, dy, -math.sqrt(max(q, 0.02))] if fn[2] <= 0 else [dx, dy, math.sqrt(max(q, 0.02))]
	else:
		g = [dx, dy, -math.sqrt(q) if p[2] < c[2] else math.sqrt(q)]
	ln = math.sqrt(sum(x * x for x in g)) or 1
	g = [x / ln for x in g]
	return tuple(0.8 * g[k] + 0.2 * fn[k] for k in range(3))


# --- the skin ---------------------------------------------------------------------------------------------------------


def body_skin(p, face, g, res, base=2.0):
	"""Bare skin on the torso and gut, shaded as one round mass."""
	n = gut_normal(p, face)
	return ramp(SKIN, base - 3.0 * lit(n) + max(0.0, p[1] - 12.5) * 0.3 + g * 0.5, res)


def skin_colour(part, name, face, p, iu, iv, r):
	"""Colour of one texel; p is the point in model space (standing rest pose)."""
	x, y, z = p
	nx, ny, nz = NORMALS[face]
	g = grain(iu, iv, r, 7 + BODY_SEED.get(name, 0))
	px = 1.0 / r  # one texel in model units

	if part in ("right_leg", "left_leg"):
		if face == "up":
			return ramp(LEATHER, 2 + g * 0.6)
		if face == "down" or y >= 23:
			cleft = face == "north" and abs(x - (5 if x > 0 else -5)) < 0.5
			return ramp(HOOF, 3 if cleft or (face != "down" and g < 0) else 1)
		if y < 19.5:  # short pants
			edge = y > 19.5 - px
			return ramp(LEATHER, (3 if edge else 1.6) + g * 0.6 + (0.6 if nz > 0 else 0))
		cx = 5 if x > 0 else -5
		inner = nx != 0 and nx * (x - cx) < 0 and nx * x < 0
		shade = 2 + (y - 19.5) * 0.3 + (0.8 if inner else 0)
		return ramp(SKIN, shade + g * 0.7)

	if part in ("right_arm", "left_arm"):
		ax = -ARM_X if part == "right_arm" else ARM_X
		inner = nx != 0 and nx * x < 0
		if face == "down":  # curled fingers
			lines = int((x - ax + 10) * r) % (2 if r == 1 else 3) == 0 and z < 0.5
			return ramp(SKIN, (5 if lines else 4) + g * 0.4)
		if face == "up" or y < 5:  # leather shoulder pad
			edge = 5 - px <= y < 5
			return ramp(LEATHER, (3 if edge else 1.3) + g * 0.6 + (0.5 if inner else 0))
		band = (7.0, 8.0) if part == "right_arm" else (12.0, 13.0)
		if band[0] <= y < band[1]:  # gold arm ring / bracelet
			hi = y < band[0] + px
			return ramp(GOLD, (1 if hi else 2) + (1 if inner else 0) + (0.6 if nz > 0 else 0))
		if face == "north" and y >= 12:  # knuckles: light bumps, dark gaps
			kx = x - (ax - 3)
			gap = int(kx) % 2 == 1 if r == 1 else int(kx * 2) % 3 == 2
			if y < 13:
				return ramp(SKIN, 3.2 if gap else (1.0 if r == 1 or y < 12.5 else 1.6))
			return ramp(SKIN, 3.0 + (0.6 if gap else 0))
		shade = 1.8 + (y - 5) * 0.18 + (1.2 if inner else 0) + (0.5 if nz > 0 else 0)
		return ramp(SKIN, shade + g * 0.7)

	if name in ("belly", "belly_front"):  # bare, round, belly button
		if face == "down":
			return ramp(SKIN, 4.6 + g * 0.5)
		if name == "belly_front" and nz < 0 and abs(x) < 0.5 * (2 - r) + 0.5 * (r - 1) * 1.0 and 11 <= y < 12:
			if r == 1 and x == 0 or r == 2:
				return SKIN[6] if (r == 1 or y < 11.5) else SKIN[5]
		if nz < 0:
			bristle = h01(iu, iv, 99) < (0.010 if r == 1 else 0.005) and abs(x) > 3
			if bristle:
				return LEATHER[0]
		return body_skin(p, face, g, 1, 2.0)

	if name == "rear":  # loincloth over the seat
		if face == "down":
			return ramp(LEATHER, 3.5)
		seam = nz > 0 and abs(x) < px * 0.6
		return ramp(LEATHER, (3 if seam else 1.6) + (y - 13) * 0.2 + g * 0.6)

	if name.startswith("shoulder"):  # vest over the shoulders
		return ramp(LEATHER, (1.2 if face == "up" else 1.8) + (0.6 if nx * x < 0 else 0) + g * 0.6)

	if face == "down":
		return ramp(LEATHER, 3.5 + g * 0.4)
	# belt and loincloth
	if y >= 15.5:
		return ramp(LEATHER, 1.6 + (y - 15.5) * 0.3 + g * 0.6 + (0.5 if nz > 0 else 0))
	if 14 <= y < 15.5:
		stud = nx != 0 and abs(abs(z) - 3) < px and y < 14 + px * 2
		if stud:
			return GOLD[1]
		return ramp(LEATHER, 3 if y < 14 + px else 2.6 + g * 0.3)
	# vest: top, back and sides; open at the front over chest and belly
	front_open = nz < 0 and abs(x) < 6.5
	vest = (y < 10.5 or nz > 0) and not front_open
	if face == "up":
		vest = abs(x) > 4.5 or name in ("girth", "back")
	if vest:
		edge_x = nz < 0 and 6.5 <= abs(x) < 6.5 + px
		edge_y = 10.5 - px <= y < 10.5 and nz <= 0
		button = nz < 0 and 6.5 + px <= abs(x) < 6.5 + 2 * px and abs(((y - 3) % 2.5) - 0.5) < px * 0.6 and y < 10
		if button:
			return GOLD[1] if r == 1 or y % 1 < 0.5 else GOLD[2]
		lace = nz > 0 and abs(x) < px and y < 10
		i = 1.4 + (0.6 if nz > 0 else 0) + (0.4 if nx != 0 else 0)
		if edge_x or edge_y or lace:
			i = 3
		return ramp(LEATHER, i + g * 0.6)
	# bare skin: chest, girth front, flanks under the vest
	if nz < 0 and name == "torso" and y < 8 + px:  # chest with a cleft between the pecs and an old scar
		if r == 1 and (x, y) in ((-4.5, 4.5), (-3.5, 5.5), (-2.5, 6.5)) or r == 2 and abs((x + 4.5) - (y - 4.5)) < 0.3 and -5 < x < -2:
			return SCAR
		if abs(x) < px * 0.6 and 3 < y < 7.5:
			return ramp(SKIN, 3.4)
		if 7.5 <= y < 8 + px * 0.5 and 1 < abs(x) < 5.5:
			return ramp(SKIN, 3.0)  # pec underline
		return ramp(SKIN, 1.2 + (abs(x) / 6.5) * 0.8 + g * 0.6)
	bristle = nz < 0 and h01(iu, iv, 31) < (0.04 if r == 1 else 0.02) and abs(x) > 4
	if bristle:
		return LEATHER[0]
	return body_skin(p, face, g, 1, 2.2)


BODY_SEED = {"torso": 0, "girth": 1, "belly": 2, "belly_front": 3, "rear": 4, "back": 5, "shoulder_r": 6, "shoulder_l": 7}


def head_colour(name, face, p, iu, iv, r):
	"""Head, snout, brow and tusks; p in head space (pivot removed)."""
	x, y, z = p
	px = 1.0 / r
	g = grain(iu, iv, r, 5)
	mn = MODEL_N[face]
	lf = lit(mn)
	if name in ("tusk_r", "tusk_l"):
		if y < -2:  # gold cap
			if face == "up":
				return GOLD[0]
			return ramp(GOLD, (1 if y < -2.5 or r == 1 else 2) - lf * 0.6 + (0.6 if face in ("east", "west") else 0))
		return ramp(IVORY, (0 if face == "north" else 1) + (y + 2) * 0.6 + (0.5 if face == "down" else 0), r)
	if name == "snout":
		if face == "north":
			for cx in (-2, 2):  # nostrils
				dx, dy = x - cx, y + 2
				if abs(dx) < 0.5 and -1 <= dy < 1:
					if r == 1:
						return NOSTRIL[0] if dy < 0 else NOSTRIL[1]
					return NOSTRIL[0] if dy < -0.5 else (NOSTRIL[2] if dy < 0.5 else NOSTRIL[1])
				if r == 2 and abs(dx) < 0.5 and -1.5 <= dy < -1:
					return ramp(SKIN, 2.6, r)  # rim above the nostril
			if y >= -px:
				return ramp(SKIN, 3.4, r)
			if y < -4 + px:
				return ramp(SKIN, 1.0, r)
			return ramp(SKIN, 1.2 + (y + 4) * 0.15 + g * 0.4, r)
		if face == "up":
			return ramp(SKIN, 0.6 + g * 0.5, r)
		if face == "down":
			return ramp(SKIN, 4.6, r)
		return ramp(SKIN, 2.6 + (y + 4) * 0.2 + g * 0.4, r)
	if name == "brow":
		if face == "down":
			return ramp(SKIN, 5, r)
		if face == "up":
			return ramp(SKIN, 1.2 + g * 0.4, r)
		if face == "north":
			furrow = abs(x) < 1  # knotted between the eyes
			slant = r == 2 and y >= -5.5 and abs(x) > 1  # lower edge darker toward the middle
			return ramp(SKIN, (3.2 if furrow else 1.8) + (0.8 if slant and abs(x) < 3 else (0.4 if slant else 0)) + g * 0.4, r)
		return ramp(SKIN, 3.2, r)
	# the head box
	if face == "north":
		# eyes under the brow
		for cx in (-3, 3):
			dx = x - cx
			if -5 <= y < -4 and abs(dx) < 1:
				if r == 1:
					inner = dx * cx < 0  # toward the nose
					return EYE if inner else RED[2]
				ey = y + 5
				inner = dx * cx < 0
				if ey < 0.5:
					return ramp(SKIN, 4.6, r)  # brow shadow
				if inner and abs(dx) < 0.5:
					return EYE
				if abs(dx) < 0.5:
					return RED[1]
				return RED[3] if inner else ramp(SKIN, 4, r)
			if -4 <= y < -4 + px and abs(dx) < 1.5:
				return ramp(SKIN, 3.4, r)  # lower lid
		if -5 <= y < -4 + (0 if r == 1 else 0) and abs(x) < 1.5:
			return ramp(SKIN, 2.6, r)
		# brow shadow band and forehead
		if -5 <= y < -5 + px and abs(x) < 4:
			return ramp(SKIN, 3.8, r)
		# mouth line between the tusks and cheeks
		if -1 <= y < -1 + px and abs(x) < 4.5:
			return ramp(SKIN, 5.6, r)
		if y >= -1 + px:
			return ramp(SKIN, 4.6, r)
		cheek = y >= -4
		shade = 0.6 + (y + 8) * 0.15 + (abs(x) / 5) * 0.8 + (0.5 if cheek else 0)
		return ramp(SKIN, shade + g * 0.5, r)
	if face == "up":
		stripe = abs(x) < 1.5
		return ramp(SKIN, (0.3 if stripe else 0.9) + (z + 4) * 0.1 + g * 0.6, r)
	if face == "down":
		return ramp(SKIN, 4.4 + g * 0.3, r)
	if face == "south":
		return ramp(SKIN, 2.6 + (y + 8) * 0.15 + g * 0.6, r)
	# sides: jowls darker low and toward the back
	jowl = y > -3 and z < 1
	return ramp(SKIN, 1.4 + (y + 8) * 0.15 + (z + 4) * 0.08 + (0.4 if jowl else 0) + g * 0.6, r)


def ear_colour(side, face, p, iu, iv, r):
	x, y, z = p
	g = grain(iu, iv, r, 11)
	outward = (face == "east") if side > 0 else (face == "west")
	inward = (face == "west") if side > 0 else (face == "east")
	if outward:
		if side > 0 and 3.5 <= y < 4.5 and -1.5 <= z < -0.5:  # gold earring
			return GOLD[1] if (r == 1 or y < 4) else GOLD[2]
		return ramp(SKIN, 1.6 + y * 0.25 + g * 0.5, r)
	if inward:
		inner = 0.5 < y < 4.5 and abs(z) < 1.5
		return ramp(SKIN, (4.2 if inner else 3) + (0.6 if inner and r == 2 and abs(z) < 0.5 else 0) + g * 0.3, r)
	return ramp(SKIN, 2.6 + y * 0.2 + g * 0.4, r)


def paint_skin(r):
	img = Image.new("RGBA", (TEX_W * r, TEX_H * r), (0, 0, 0, 0))
	px = img.load()
	used = {}
	for part, (pivot, _rot, cubes) in PARTS.items():
		for ci, cube in enumerate(cubes):
			name = NAMES[part][ci]
			for face, quad in faces(cube):
				for iu, iv, p, s, t in texels(quad, r):
					key = (iu, iv)
					assert key not in used, f"UV overlap {part}#{ci} with {used[key]} at {key}"
					used[key] = (part, ci)
					if part == "head":
						c = head_colour(name, face, p, iu, iv, r)
					elif part.endswith("ear"):
						c = ear_colour(1 if part == "left_ear" else -1, face, p, iu, iv, r)
					else:
						pt = (p[0] + pivot[0], p[1] + pivot[1], p[2] + pivot[2])
						c = skin_colour(part, name, face, pt, iu, iv, r)
					px[iu, iv] = c + (255,)
	return img


# --- the cape ---------------------------------------------------------------------------------------------------------

EMBLEM = [  # a gold crown over a piglin snout with its tusks
	"G.GG.G",
	"GGGGGG",
	"......",
	".GGGG.",
	"GdGGdG",
	".GGGG.",
	".w..w.",
]


def cape_outer(cx, cy, r, iu, iv):
	"""Outside of the cape: 20 x 20 cells, cx/cy in cells (floats at 32x)."""
	W, H = 20, 20
	g = grain(iu, iv, r, 41)
	# gold edging down both sides, along the hem and the collar
	if cx < 1 or cx >= W - 1:
		return ramp(GOLD, (1 if cx < 1 else 2) + (1 if cy > H - 3 else 0))
	if cy < 1:
		return ramp(GOLD, 1 if (r == 1 or cy < 0.5) else 2)
	if cy >= H - 1:
		return ramp(GOLD, 2 if (r == 1 or cy >= H - 0.5) else 1)
	if cy >= H - 2:  # hem fringe
		return ramp(GOLD, 2) if int(cx) % 2 == 0 else ramp(RED, 3.5)
	# emblem
	ex, ey = cx - 7, cy - 4
	if 0 <= ex < 6 and 0 <= ey < len(EMBLEM):
		ch = EMBLEM[int(ey)][int(ex)]
		if ch == "G":
			above = int(ey) == 0 or EMBLEM[int(ey) - 1][int(ex)] != "G"
			below = int(ey) == len(EMBLEM) - 1 or EMBLEM[int(ey) + 1][int(ex)] != "G"
			if r == 2:
				return ramp(GOLD, 0 if (above and ey % 1 < 0.5) else (2 if (below and ey % 1 >= 0.5) else 1))
			return ramp(GOLD, 2 if (below and not above) else 1)
		if ch == "d":
			return RED[4]
		if ch == "w":
			return hx("fff4d6") if (r == 1 or ey % 1 < 0.5) else hx("d8cfae")
	# folds: soft vertical creases, wider toward the hem
	fold = [0, 0.6, 1.2, 0.6, 0, -0.5, 0, 0.7, 1.3, 0.7, 0, -0.5, 0, 0.6, 1.2, 0.6, 0, -0.5, 0, 0.6]
	i = 1.3 + fold[min(W - 1, int(cx))] * (0.5 + cy / H) + cy * 0.04
	return ramp(RED, i + g * 0.5)


def paint_cape(r):
	img = Image.new("RGBA", (CAPE_W * r, CAPE_H * r), (0, 0, 0, 0))
	px = img.load()
	u, v, x, y, z, w, h, d = CAPE_CUBE
	for face, quad in faces(CAPE_CUBE):
		for iu, iv, p, s, t in texels(quad, r):
			g = grain(iu, iv, r, 43)
			if face == "north":  # the outside once the cape is turned round (as vanilla's)
				cx = (iu / r) - (u + d)
				cy = (iv / r) - (v + d)
				c = cape_outer(cx, cy, r, iu, iv)
			elif face == "south":  # lining
				c = ramp(RED, 3.3 + g * 0.5 + ((iv / r) - 1) * 0.03)
			else:  # edges and top/bottom: gold trim
				c = ramp(GOLD, 2)
			px[iu, iv] = c + (255,)
	return img


def main():
	if "--java" in sys.argv:
		print(java())
		return
	for r, out in ((1, OUT16), (2, OUT32)):
		(out / "entity").mkdir(parents=True, exist_ok=True)
		paint_skin(r).save(out / "entity/boss_piglin.png")
		paint_cape(r).save(out / "entity/boss_piglin_cape.png")
	print("wrote boss_piglin.png and boss_piglin_cape.png (16x and 32x)")


if __name__ == "__main__":
	main()
