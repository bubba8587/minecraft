import sys, re
L = open(sys.argv[1], encoding='utf-8-sig', errors='replace').read().splitlines()
seen = set(); out = []
for i, l in enumerate(L):
    m = re.match(r'^(C:\\.*?\\mods\\(\w+)\\src\\(\w+)\\.*\\(\w+\.java)):(\d+): error: (.*)', l)
    if not m:
        continue
    key = (m.group(1), m.group(5))
    if key in seen:
        continue
    seen.add(key)
    ctx = [x.strip() for x in L[i + 1:i + 6] if 'symbol:' in x or 'required:' in x or 'found:' in x]
    out.append(f"{m.group(2)}/{m.group(3)}/{m.group(4)}:{m.group(5)} {m.group(6)} | {' | '.join(ctx)}")
print(len(out))
print("\n".join(out))
