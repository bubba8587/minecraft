"""Heuristic mixin target check: every targeted class, method, field and @At target must exist in the decompiled source."""
import pathlib, re, sys

ROOT = pathlib.Path(sys.argv[1])
SRC = pathlib.Path(sys.argv[2])
FAPI = pathlib.Path(sys.argv[3]) if len(sys.argv) > 3 else None

index = {}
for base in [SRC / "common", SRC / "client"]:
    for f in base.rglob("*.java"):
        index[str(f.relative_to(base).with_suffix("")).replace("\\", "/").replace("/", ".")] = f
if FAPI:
    for f in FAPI.rglob("*.java"):
        if "/src/" in f.as_posix() and "/java/" in f.as_posix():
            parts = f.as_posix().split("/java/", 1)[1]
            index.setdefault(parts[:-5].replace("/", "."), f)


def src_of(fqcn):
    top = fqcn.split("$")[0]
    while top and top not in index:
        if "." not in top:
            return None
        top = top.rsplit(".", 1)[0]
    return index.get(top)


def has_member(text, name):
    if name in ("<init>", "<clinit>"):
        return True
    return re.search(r"\b" + re.escape(name) + r"\b\s*[(=;,]", text) is not None or re.search(r"\b" + re.escape(name) + r"\b", text) is not None and f"{name}(" in text


problems = []
for f in ROOT.glob("mods/*/src/**/*.java"):
    t = f.read_text(encoding="utf8")
    if "@Mixin" not in t:
        continue
    imports = dict((m.group(2), m.group(1) + "." + m.group(2)) for m in re.finditer(r"^import ([\w.]+)\.(\w+);", t, re.M))
    pkg = re.search(r"^package ([\w.]+);", t, re.M).group(1)
    m = re.search(r"@Mixin\((.*?)\)\s*(?:public |abstract |final |interface |class )", t, re.S)
    if not m:
        problems.append(f"{f.name}: could not read @Mixin")
        continue
    targets = []
    for c in re.findall(r"([\w.]+)\.class", m.group(1)):
        head = c.split(".")[0]
        targets.append(imports.get(head, c if "." in c and c[0].islower() else imports.get(head, head)) + ("$" + c.split(".", 1)[1].replace(".", "$") if "." in c and head in imports else ""))
    for s in re.findall(r'targets\s*=\s*\{?\s*"([^"]+)"', m.group(1)):
        targets.append(s.replace("/", "."))
    texts = []
    for c in targets:
        p = src_of(c)
        if p is None:
            problems.append(f"{f.name}: target class not found: {c}")
        else:
            texts.append(p.read_text(encoding="utf8", errors="replace"))
    if not texts:
        continue
    alltext = "\n".join(texts)
    names = []
    for mm in re.finditer(r'method\s*=\s*(\{[^}]*\}|"[^"]*")', t):
        names += re.findall(r'"([^"]+)"', mm.group(1))
    for n in names:
        base = re.split(r"[(:]", n.split(";")[-1] if n.startswith("L") and ";" in n and "(" not in n.split(";")[0] else n)[0]
        base = base.split(";")[-1] if ";" in base else base
        if base and base not in ("<init>", "<clinit>") and not re.search(r"\b" + re.escape(base) + r"\s*\(", alltext):
            problems.append(f"{f.name}: method '{base}' not in {', '.join(x.split('.')[-1] for x in targets)}")
    for kind, n in re.findall(r'@(Accessor|Invoker)\(\s*(?:value\s*=\s*)?"([^"]+)"', t):
        if not re.search(r"\b" + re.escape(n) + r"\b", alltext):
            problems.append(f"{f.name}: @{kind} '{n}' not found")
    for at in re.findall(r'target\s*=\s*"(L[\w/$]+;[^"]*)"', t):
        owner = at[1:at.index(";")].replace("/", ".")
        rest = at[at.index(";") + 1:]
        member = re.split(r"[(:]", rest)[0]
        p = src_of(owner)
        if p is None:
            if not owner.startswith("java."):
                problems.append(f"{f.name}: @At owner not found: {owner}")
            continue
        if member and member != "<init>" and not re.search(r"\b" + re.escape(member) + r"\b", p.read_text(encoding="utf8", errors="replace")):
            problems.append(f"{f.name}: @At member '{member}' not in {owner.split('.')[-1]}")
print(len(problems), "possible problems")
print("\n".join(sorted(problems)))
