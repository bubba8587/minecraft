#!/usr/bin/env python3
# dte:B16
"""Check that every 16x texture has a 32x twin at exactly double size (specs/textures.md).

  python3 tools/texcheck.py [project ...]

Mods: assets/<ns>/textures/** vs resourcepacks/<modid>_32x/assets/<ns>/textures/** inside
src/main/resources. Resource pack projects: resourcepacks/<name>/ vs resourcepacks/<name>-32x/.
Mods with internal=true in gradle.properties are skipped. Standard library only.
"""
import struct, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def png_size(p):
    with open(p, "rb") as f:
        head = f.read(24)
    if head[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG")
    return struct.unpack(">II", head[16:24])


def props(d):
    f = d / "gradle.properties"
    out = {}
    if f.is_file():
        for line in f.read_text().splitlines():
            if "=" in line and not line.lstrip().startswith("#"):
                k, v = line.split("=", 1)
                out[k.strip()] = v.strip()
    return out


def compare(base16, base32, label):
    problems, count = [], 0
    for tex in sorted(base16.rglob("*.png")):
        rel = tex.relative_to(base16)
        twin = base32 / rel
        count += 1
        if not twin.is_file():
            problems.append(f"{label}: no 32x twin for {rel}")
            continue
        (w, h), (w2, h2) = png_size(tex), png_size(twin)
        if (w2, h2) != (2 * w, 2 * h):
            problems.append(f"{label}: {rel} is {w}x{h} but its 32x twin is {w2}x{h2} (want {2*w}x{2*h})")
        meta, meta2 = tex.with_name(tex.name + ".mcmeta"), twin.with_name(twin.name + ".mcmeta")
        if meta.is_file() != meta2.is_file():
            problems.append(f"{label}: {rel}.mcmeta exists on one side only")
        elif meta.is_file() and meta.read_text().split() != meta2.read_text().split():
            problems.append(f"{label}: {rel}.mcmeta differs between 16x and 32x (frames/timing must match)")
    for tex in sorted(base32.rglob("*.png")) if base32.is_dir() else []:
        if not (base16 / tex.relative_to(base32)).is_file():
            problems.append(f"{label}: 32x texture {tex.relative_to(base32)} has no 16x original")
    return problems, count


def main(names):
    problems, checked = [], []
    mods = ROOT / "mods"
    for d in sorted(mods.iterdir()) if mods.is_dir() else []:
        if names and d.name not in names:
            continue
        p = props(d)
        if p.get("internal") == "true" or p.get("disabled"):
            continue
        res = d / "src" / "main" / "resources"
        modid = p.get("mod_id", d.name)
        for ns in sorted((res / "assets").iterdir()) if (res / "assets").is_dir() else []:
            if (ns / "textures").is_dir():
                pr, n = compare(ns / "textures", res / "resourcepacks" / f"{modid}_32x" / "assets" / ns.name / "textures", f"mods/{d.name}")
                problems += pr
                checked.append(f"mods/{d.name} ({n} textures)")
    rps = ROOT / "resourcepacks"
    for d in sorted(rps.iterdir()) if rps.is_dir() else []:
        if d.name.endswith("-32x") or (names and d.name not in names) or (d / "DISABLED").is_file() or (d / "COMBINE").is_file():
            continue
        pr, n = compare(d, rps / f"{d.name}-32x", f"resourcepacks/{d.name}")
        problems += pr
        checked.append(f"resourcepacks/{d.name} ({n} textures)")
    for c in checked:
        print(f"checked {c}")
    if problems:
        print("FAILED")
        for p in problems:
            print(f"  {p}")
        sys.exit(1)
    print("OK" if checked else "OK (no projects with textures)")


if __name__ == "__main__":
    main(sys.argv[1:])
