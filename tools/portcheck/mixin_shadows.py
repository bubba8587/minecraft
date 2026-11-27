"""List @Shadow members whose name no longer appears anywhere in the target class's source (or its superclass chain by name)."""
import pathlib, re, sys

ROOT = pathlib.Path(sys.argv[1]); SRC = pathlib.Path(sys.argv[2])
idx = {}
for base in [SRC / "common", SRC / "client"]:
    for f in base.rglob("*.java"):
        idx[str(f.relative_to(base).with_suffix("")).replace("\\", "/").replace("/", ".")] = f
simple = {}
for k, v in idx.items():
    simple.setdefault(k.rsplit(".", 1)[-1], v)


def chain(f, depth=0):
    if f is None or depth > 8:
        return ""
    t = f.read_text(encoding="utf8", errors="replace")
    m = re.search(r"\bclass \w+(?:<[^{]*?>)?\s+extends\s+([\w.]+)", t)
    return t + ("\n" + chain(simple.get(m.group(1).split(".")[-1]), depth + 1) if m else "")


for f in ROOT.glob("mods/*/src/**/*.java"):
    t = f.read_text(encoding="utf8")
    if "@Shadow" not in t:
        continue
    imports = dict((m.group(2), m.group(1) + "." + m.group(2)) for m in re.finditer(r"^import ([\w.]+)\.(\w+);", t, re.M))
    m = re.search(r"@Mixin\((.*?)\)", t, re.S)
    targets = [imports.get(c.split(".")[0], c) for c in re.findall(r"([\w.]+)\.class", m.group(1))] if m else []
    text = "\n".join(chain(idx.get(c)) for c in targets)
    if not text.strip():
        continue
    for sm in re.finditer(r"@Shadow(?:\([^)]*\))?\s+(?:@\w+\s+)*(?:(?:public|protected|private|static|final|abstract)\s+)*[\w<>\[\], .?]+?\s+(\w+)\s*[;(=]", t):
        name = sm.group(1)
        if not re.search(r"\b" + re.escape(name) + r"\b", text):
            print(f"{f.name}: @Shadow {name} not found in {[c.split('.')[-1] for c in targets]}")
