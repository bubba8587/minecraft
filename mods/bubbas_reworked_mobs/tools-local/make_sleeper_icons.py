#!/usr/bin/env python3
# dte:B16,C1
"""Item icons for the sleeper items (Sleeping Skeleton, Zombie, Husk, Stray): the undead's head, eyes shut, at 16x
and Faithful-style 32x (specs/textures.md). Agent-made, listed in docs/art-todo.md; never AI-generated.

  python mods/bubbas_reworked_mobs/tools-local/make_sleeper_icons.py

Each is a head in vanilla's style seen from the front with the eyes closed (a sleeper): the skeleton's skull, the
zombie's green face, the husk's sand-dried face, the stray's frosted skull under a ragged hood. Light from the top left.
Replaces the flat placeholder squares (tools-local/make_placeholder_textures.py). Needs Pillow.
"""
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent.parent
sys.path.insert(0, str(ROOT / "tools"))
from pixelart import check, render, render32  # noqa: E402

RES = HERE.parent / "src/main/resources"
MOD = "bubbas_reworked_mobs"


def shades(*cols):
	return {k: c for k, c in zip("o123456", cols)}


SKULL = check([
	"................",
	"................",
	"...oooooooooo...",
	"..o5666666665o..",
	"..o5666666654o..",
	"..o5566665544o..",
	"..o5oooo4ooo4o..",
	"..o4o11o4o11oo..",
	"..o4444443343o..",
	"..o4444o4o333o..",
	"..o3434oo4332o..",
	"...o3o3o3o32o...",
	"...o3o3o3o3o....",
	"....oooooooo....",
	"................",
	"................",
])
# Eyes shut: dark slits ('1' in the sockets) instead of open holes.
SKELETON_PAL = shades((40, 40, 40), (72, 72, 72), (120, 120, 116), (160, 160, 152), (196, 196, 186), (218, 218, 208), (238, 238, 228))

FACE = check([
	"................",
	"...oooooooooo...",
	"..o6655555544o..",
	"..o5555555443o..",
	"..o5544554433o..",
	"..o5444444333o..",
	"..o4oo44oo333o..",
	"..o4444444332o..",
	"..o4433443322o..",
	"..o4444444322o..",
	"..o44o1111o22o..",
	"..o3444444322o..",
	"..o3333333222o..",
	"...oooooooooo...",
	"................",
	"................",
])
ZOMBIE_PAL = shades((24, 40, 20), (42, 70, 36), (58, 96, 48), (76, 122, 62), (94, 146, 76), (114, 168, 92), (138, 190, 112))
HUSK_PAL = shades((48, 38, 24), (90, 72, 48), (122, 100, 66), (150, 126, 86), (176, 150, 104), (198, 174, 124), (220, 198, 150))

HOODED = check([
	"....oooooooo....",
	"...oaaabbbaaao..",
	"..oaab666666ba..",
	"..oab56666665bo.",
	"..oab56666654bo.",
	"..oab55666554ao.",
	"..oab5oooo4oo4o.",
	"..oaao11o3o1o3o.",
	"..oab444443343o.",
	"..oab444o4o33ao.",
	"..obb3434oo43ao.",
	"..obba3o3o3o2ao.",
	"..ob.a3o3o3oaao.",
	"..o..aoooooo.ao.",
	"..............o.",
	"................",
])
STRAY_PAL = {**shades((30, 36, 44), (70, 82, 92), (118, 132, 142), (158, 172, 180), (190, 204, 210), (210, 222, 228), (232, 240, 244)),
	"a": (64, 74, 72), "b": (92, 104, 100)}


def write(name, grid, pal, mat, grain, seed):
	for s, base in ((1, RES / f"assets/{MOD}"), (2, RES / f"resourcepacks/{MOD}_32x/assets/{MOD}")):
		out = base / "textures/item"
		out.mkdir(parents=True, exist_ok=True)
		img = render(grid, pal) if s == 1 else render32(grid, pal, mat, inner={"o": "1"}, grain=grain, seed=seed)
		img.save(out / f"{name}.png")
	print("wrote", name)


def main():
	bone = {k: "bone" for k in "123456"}
	flesh = {k: "flesh" for k in "123456"}
	write("skeleton_sleeper", SKULL, SKELETON_PAL, bone, ("bone",), 41)
	write("zombie_sleeper", FACE, ZOMBIE_PAL, flesh, ("flesh",), 42)
	write("husk_sleeper", FACE, HUSK_PAL, flesh, ("flesh",), 43)
	write("stray_sleeper", HOODED, STRAY_PAL, {**bone, "a": "cloth", "b": "cloth"}, ("bone", "cloth"), 44)


if __name__ == "__main__":
	main()
