#!/usr/bin/env python3
# dte:A4
"""Build and statically check datapacks and resource packs.

  python tools/pack.py check [NAME ...]   check packs (all if none named)
  python tools/pack.py build [NAME ...]   check, then zip into build/packs/<datapacks|resourcepacks>/<name>-<version>.zip

A pack is a folder under datapacks/, resourcepacks/ or testworlds/ (world builder packs) with a pack.mcmeta.
Its version comes from an optional file `VERSION` (one line); default 0.1.0.
A pack with a `DISABLED` file (one line: why) is skipped by every pack tool.
Standard library only.
"""
import json, re, sys, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
KINDS = {"datapacks": "data", "resourcepacks": "assets", "testworlds": "data"}  # testworlds/: world builder packs
NAMESPACE = re.compile(r"^[a-z0-9_.-]+$")
PATH_PART = re.compile(r"^[a-z0-9_./-]+$")
# Files that never ship in a pack zip.
SKIP = {"VERSION", "README.md", ".DS_Store", "Thumbs.db"}


def packs(names):
    found = []
    for kind in KINDS:
        base = ROOT / kind
        if not base.is_dir():
            continue
        for d in sorted(base.iterdir()):
            if (d / "pack.mcmeta").is_file() and (not names or d.name in names):
                if (d / "DISABLED").is_file():  # B10 alwaysLatest: parked until it works on this version
                    print(f"skip {kind}/{d.name}: DISABLED: {(d / 'DISABLED').read_text().strip()}")
                    continue
                found.append((kind, d))
    disabled = {d.name for k in KINDS if (ROOT / k).is_dir() for d in (ROOT / k).iterdir() if (d / "DISABLED").is_file()}
    missing = set(names) - {d.name for _, d in found} - disabled
    if missing:
        sys.exit(f"no such pack: {', '.join(sorted(missing))}")
    return found


def check(kind, d):
    """Return a list of problems with the pack at d."""
    problems = []
    try:
        meta = json.loads((d / "pack.mcmeta").read_text(encoding="utf-8"))
        p = meta["pack"]
        if "description" not in p:
            problems.append("pack.mcmeta: pack.description missing")
        if "min_format" not in p or "max_format" not in p:
            problems.append("pack.mcmeta: pack.min_format and pack.max_format are required (26.x format)")
    except (ValueError, KeyError) as e:
        problems.append(f"pack.mcmeta: {e}")
    top = d / KINDS[kind]
    if not top.is_dir():
        problems.append(f"missing {KINDS[kind]}/ folder")
        return problems
    for f in sorted(top.rglob("*")):
        if f.is_dir():
            continue
        rel = f.relative_to(top).as_posix()
        ns = rel.split("/", 1)[0]
        if not NAMESPACE.match(ns):
            problems.append(f"{rel}: namespace '{ns}' must be lowercase a-z 0-9 _ . -")
        if not PATH_PART.match(rel):
            problems.append(f"{rel}: path must be lowercase a-z 0-9 _ . - /")
        if f.suffix in (".json", ".mcmeta"):
            try:
                json.loads(f.read_text(encoding="utf-8"))
            except ValueError as e:
                problems.append(f"{rel}: invalid JSON: {e}")
        if f.suffix == ".mcfunction":
            for n, line in enumerate(f.read_text(encoding="utf-8").splitlines(), 1):
                s = line.strip()
                if s.startswith("/"):
                    problems.append(f"{rel}:{n}: commands in .mcfunction must not start with '/'")
    return problems


def version(d):
    v = d / "VERSION"
    return v.read_text().strip() if v.is_file() else "0.1.0"


def build(kind, d):
    out = ROOT / "build" / "packs" / kind  # datapacks/ and resourcepacks/ apart: a pack may have the same name in both
    out.mkdir(parents=True, exist_ok=True)
    z = out / f"{d.name}-{version(d)}.zip"
    with zipfile.ZipFile(z, "w", zipfile.ZIP_DEFLATED) as zf:
        for f in sorted(d.rglob("*")):
            if f.is_file() and f.name not in SKIP:
                zf.write(f, f.relative_to(d).as_posix())
    return z


def main(argv):
    if not argv or argv[0] not in ("check", "build"):
        sys.exit(__doc__)
    cmd, names = argv[0], argv[1:]
    failed = False
    for kind, d in packs(names):
        problems = check(kind, d)
        label = f"{kind}/{d.name}"
        if problems:
            failed = True
            print(f"FAIL {label}")
            for p in problems:
                print(f"  {p}")
            continue
        if cmd == "build":
            print(f"ok   {label} -> {build(kind, d).relative_to(ROOT)}")
        else:
            print(f"ok   {label}")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main(sys.argv[1:])
