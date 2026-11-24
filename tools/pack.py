#!/usr/bin/env python3
# dte:A4
"""Build and statically check datapacks and resource packs.

  python tools/pack.py check [NAME ...]   check packs (all if none named)
  python tools/pack.py build [NAME ...]   check, then zip into build/packs/<datapacks|resourcepacks>/<name>-<version>.zip

A pack is a folder under datapacks/, resourcepacks/ or testworlds/ (world builder packs) with a pack.mcmeta.
Its version comes from an optional file `VERSION` (one line); default 0.1.0.
A pack with a `DISABLED` file (one line: why) is skipped by every pack tool.
A combined pack has a `COMBINE` file instead of its own assets/ (or data/): one glob per line, relative to the repo
root, each matching pack folders whose assets/ (or data/) are merged into it when checked or zipped (resourcepacks/
bubbas_32x collects every mod's resourcepacks/<modid>_32x/). Sources inside an internal or disabled mod are left out.
Two sources with the same file path fail the check. Nothing is copied into the source tree: the zip is written
straight from the sources.
Standard library only.
"""
import json, re, sys, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
KINDS = {"datapacks": "data", "resourcepacks": "assets", "testworlds": "data"}  # testworlds/: world builder packs
NAMESPACE = re.compile(r"^[a-z0-9_.-]+$")
PATH_PART = re.compile(r"^[a-z0-9_./-]+$")
# Files that never ship in a pack zip.
SKIP = {"VERSION", "README.md", "COMBINE", ".DS_Store", "Thumbs.db"}


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


def mod_props(mod_dir):
    f = mod_dir / "gradle.properties"
    if not f.is_file():
        return {}
    lines = f.read_text(encoding="utf-8").splitlines()
    return dict(l.split("=", 1) for l in lines if "=" in l and not l.lstrip().startswith("#"))


def sources(kind, d):
    """Pack d's top-folder files (assets/ or data/) as ({path inside it: file}, problems).

    A plain pack's files are its own; a combined pack's (COMBINE file) are collected from its source packs."""
    top = KINDS[kind]
    combine = d / "COMBINE"
    if not combine.is_file():
        base = d / top
        return {f.relative_to(base).as_posix(): f for f in sorted(base.rglob("*")) if f.is_file()}, []
    files, owners, problems = {}, {}, []
    if (d / top).exists():
        problems.append(f"{top}/ must not exist in a combined pack (it is collected from the COMBINE sources)")
    for pattern in (l.strip() for l in combine.read_text(encoding="utf-8").splitlines()):
        if not pattern or pattern.startswith("#"):
            continue
        for src in sorted(ROOT.glob(pattern)):
            parts = src.relative_to(ROOT).parts
            if parts[0] == "mods":
                props = mod_props(ROOT / "mods" / parts[1])
                if props.get("internal") == "true" or props.get("disabled"):
                    continue
            base = src / top
            if not base.is_dir():
                continue
            label = src.relative_to(ROOT).as_posix()
            for f in sorted(base.rglob("*")):
                if not f.is_file() or f.name in SKIP:
                    continue
                path = f.relative_to(base).as_posix()
                if path in files:
                    problems.append(f"{top}/{path}: in both {owners[path]} and {label}")
                    continue
                files[path], owners[path] = f, label
    if not files:
        problems.append("COMBINE: no source files found")
    return files, problems


def contributions(kind, d):
    """For a combined pack: {source pack folder: file count}."""
    counts = {}
    for f in sources(kind, d)[0].values():
        src = f.relative_to(ROOT).as_posix().split(f"/{KINDS[kind]}/", 1)[0]
        counts[src] = counts.get(src, 0) + 1
    return counts


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
    if not (d / "COMBINE").is_file() and not (d / KINDS[kind]).is_dir():
        problems.append(f"missing {KINDS[kind]}/ folder")
        return problems
    files, collect_problems = sources(kind, d)
    problems += collect_problems
    for rel, f in files.items():
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
    combined = (d / "COMBINE").is_file()
    with zipfile.ZipFile(z, "w", zipfile.ZIP_DEFLATED) as zf:
        for f in sorted(d.rglob("*")):
            if f.is_file() and f.name not in SKIP:
                zf.write(f, f.relative_to(d).as_posix())
        if combined:
            for rel, f in sources(kind, d)[0].items():
                zf.write(f, f"{KINDS[kind]}/{rel}")
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
        if (d / "COMBINE").is_file():
            for src, n in contributions(kind, d).items():
                print(f"       {n:4d} files from {src}")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main(sys.argv[1:])
