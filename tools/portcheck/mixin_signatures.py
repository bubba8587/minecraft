"""For every mixin-targeted method, compare its declared signatures between two decompiled versions."""
import pathlib, re, sys

ROOT = pathlib.Path(sys.argv[1]); OLD = pathlib.Path(sys.argv[2]); NEW = pathlib.Path(sys.argv[3])


def index(src):
    idx = {}
    for base in [src / "common", src / "client"]:
        for f in base.rglob("*.java"):
            idx[str(f.relative_to(base).with_suffix("")).replace("\\", "/").replace("/", ".")] = f
    return idx


IO, IN = index(OLD), index(NEW)


def sigs(idx, fqcn, name):
    f = idx.get(fqcn.split("$")[0])
    if not f:
        return None
    t = f.read_text(encoding="utf8", errors="replace")
    out = set()
    for m in re.finditer(r"^\s*(?:@\w+(?:\([^)]*\))?\s+)*((?:public|protected|private|static|final|abstract|synchronized|default|\s)*[\w<>\[\], .?@]+?\s+" + re.escape(name) + r"\s*\(([^)]*)\))", t, re.M):
        params = re.sub(r"\s+", " ", m.group(2)).strip()
        params = re.sub(r"@\w+(\([^)]*\))? ", "", params).replace("final ", "")
        types = ",".join(p.rsplit(" ", 1)[0].strip() for p in params.split(",") if p.strip())
        out.add(types)
    return out


for f in ROOT.glob("mods/*/src/**/*.java"):
    t = f.read_text(encoding="utf8")
    if "@Mixin" not in t:
        continue
    imports = dict((m.group(2), m.group(1) + "." + m.group(2)) for m in re.finditer(r"^import ([\w.]+)\.(\w+);", t, re.M))
    m = re.search(r"@Mixin\((.*?)\)\s*(?:public |abstract |final |interface |class )", t, re.S)
    if not m:
        continue
    targets = [imports.get(c.split(".")[0], c) for c in re.findall(r"([\w.]+)\.class", m.group(1))]
    names = []
    for mm in re.finditer(r'method\s*=\s*(\{[^}]*\}|"[^"]*")', t):
        names += [re.split(r"[(:]", n)[0] for n in re.findall(r'"([^"]+)"', mm.group(1))]
    for c in targets:
        for n in set(names):
            if n in ("<init>", "<clinit>") or n.startswith("@"):
                continue
            a, b = sigs(IO, c, n), sigs(IN, c, n)
            if a is None or b is None or not a:
                continue
            if a != b:
                print(f"{f.name}: {c.split('.')[-1]}.{n}\n   26.3: {sorted(a)}\n   26.4: {sorted(b)}")
