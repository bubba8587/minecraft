"""Keys (and string values that look like ids) vanilla stopped using between two versions, per folder; then which of our JSON files still use them."""
import json, pathlib, re, sys
from collections import defaultdict

OLD = pathlib.Path(sys.argv[1]); NEW = pathlib.Path(sys.argv[2]); OURS = pathlib.Path(sys.argv[3])


def walk(o, keys, vals):
    if isinstance(o, dict):
        for k, v in o.items():
            keys.add(k); walk(v, keys, vals)
    elif isinstance(o, list):
        for v in o:
            walk(v, keys, vals)
    elif isinstance(o, str) and re.fullmatch(r"#?minecraft:[a-z0-9_/.]+", o):
        vals.add(o)


def folder(p, root):
    parts = p.relative_to(root).parts
    # data/<ns>/<type...>/file or assets/<ns>/<type...>/file
    if len(parts) < 4:
        return None
    t = parts[2]
    if t in ("worldgen", "tags") and len(parts) > 4:
        t += "/" + parts[3]
    return parts[0] + ":" + t


def collect(root):
    K = defaultdict(set); V = defaultdict(set)
    for p in root.rglob("*.json"):
        f = folder(p, root)
        if not f:
            continue
        try:
            walk(json.loads(p.read_text(encoding="utf8")), K[f], V[f])
        except Exception:
            pass
    return K, V


ko, vo = collect(OLD / "vanilla"); kn, vn = collect(NEW / "vanilla")
allnew_vals = set().union(*vn.values())
gone_keys = {f: ko[f] - kn.get(f, set()) for f in ko}
gone_vals = set().union(*vo.values()) - allnew_vals
print("vanilla id strings no longer used anywhere in 26.4 data/assets:", len(gone_vals))
print(" ", sorted(gone_vals)[:80])
hits = []
for p in list(OURS.glob("mods/*/src/main/resources/**/*.json")) + list(OURS.glob("mods/*/src/main/generated/**/*.json")) + list(OURS.glob("datapacks/**/*.json")) + list(OURS.glob("resourcepacks/**/*.json")):
    rel = p.as_posix()
    m = re.search(r"/(data|assets)/[^/]+/(.*)", rel)
    if not m:
        continue
    parts = m.group(2).split("/")
    t = parts[0] + ("/" + parts[1] if parts[0] in ("worldgen", "tags") and len(parts) > 2 else "")
    f = m.group(1) + ":" + t
    try:
        o = json.loads(p.read_text(encoding="utf8"))
    except Exception as e:
        hits.append(f"{rel}: unreadable {e}"); continue
    keys, vals = set(), set()
    walk(o, keys, vals)
    bad_k = keys & gone_keys.get(f, set())
    bad_v = vals & gone_vals
    if bad_k or bad_v:
        hits.append(f"{p.relative_to(OURS)}: keys {sorted(bad_k)} ids {sorted(bad_v)}")
print("our files using removed keys/ids:", len(hits))
print("\n".join(hits))
