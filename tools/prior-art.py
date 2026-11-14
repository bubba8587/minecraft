#!/usr/bin/env python3
# dte:B8
"""Search Modrinth for existing projects before building something (B8 priorArtFirst).

  python3 tools/prior-art.py "<idea in a few words>" [--type mod|datapack|resourcepack|shader] [--limit N] [--all-versions]

Prints the most relevant projects with downloads, whether they have a build for our
Minecraft version, license and source link. Projects with source code are the ones to
study or depend on. Standard library only.
"""
import json, sys, urllib.parse, urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
UA = {"User-Agent": "minecraft-repo-prior-art"}


def get(path, **params):
    url = "https://api.modrinth.com/v2/" + path + ("?" + urllib.parse.urlencode(params) if params else "")
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60) as r:
        return json.loads(r.read())


def mc_version():
    for line in (ROOT / "gradle.properties").read_text().splitlines():
        if line.startswith("minecraft_version="):
            return line.split("=", 1)[1].strip()


def main(argv):
    if not argv:
        sys.exit(__doc__)
    query, ptype, limit, all_versions = argv[0], "mod", 15, "--all-versions" in argv
    if "--type" in argv: ptype = argv[argv.index("--type") + 1]
    if "--limit" in argv: limit = int(argv[argv.index("--limit") + 1])
    mc = mc_version()
    facets = [[f"project_type:{ptype}"]]
    if ptype in ("mod", "shader"):
        facets.append(["categories:fabric"] if ptype == "mod" else [])
    facets = [f for f in facets if f]
    if not all_versions:
        pass  # show everything, mark 26.3 support below; old-only projects are still prior art
    hits = get("search", query=query, facets=json.dumps(facets), limit=limit, index="relevance")["hits"]
    if not hits:
        print("nothing found; try other words")
        return
    ids = [h["project_id"] for h in hits]
    projects = {p["id"]: p for p in get("projects", ids=json.dumps(ids))}
    print(f"{len(hits)} {ptype} result(s) for '{query}' (target Minecraft {mc}):\n")
    for h in hits:
        p = projects.get(h["project_id"], {})
        versions = p.get("game_versions", [])
        has = "yes" if mc in versions else f"no (latest {versions[-1] if versions else '?'})"
        src = p.get("source_url") or "no public source"
        lic = (p.get("license") or {}).get("id", "?")
        print(f"- {h['title']}  [{h['downloads']:,} downloads]  {mc}: {has}  license: {lic}")
        print(f"  {h['description'][:150]}")
        print(f"  https://modrinth.com/{ptype}/{h['slug']}   source: {src}")


if __name__ == "__main__":
    main(sys.argv[1:])
