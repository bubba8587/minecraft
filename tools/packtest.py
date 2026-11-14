#!/usr/bin/env python3
# dte:B6,B8
"""Run datapack tests with misode's PackTest on a real headless Fabric server.

  python tools/packtest.py [NAME ...]     test packs in datapacks/ (all if none named)
  options: --keep         keep the server folder (build/packtest/server) for inspection
           --timeout S    seconds before giving up (default 600)

Tests are PackTest test functions: data/<ns>/test/*.mcfunction inside a pack, using
assert / await / fail / succeed / dummy (see docs/knowledge/datapacks.md and
https://github.com/misode/packtest). This script only wires things up: it builds the
packs with tools/pack.py, downloads Fabric's server launcher, Fabric API and PackTest
(SHA-512 checked against Modrinth), installs the packs in a fresh world and starts the
server with -Dpacktest.auto, which runs every test and exits with the failure count.
A pack with no tests still has to load: any ERROR in the log fails the run.

Versions come from gradle.properties (minecraft_version, loader_version, fabric_api_version).
Needs Java matching the Minecraft version (JAVA_HOME or PATH). Standard library only.
"""
import hashlib, json, os, shutil, subprocess, sys, urllib.parse, urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WORK = ROOT / "build" / "packtest"
CACHE = WORK / "cache"
UA = {"User-Agent": "minecraft-repo-packtest"}
FABRIC_API = "P7dR8mSH"  # Modrinth project id of Fabric API
PACKTEST = "XsKUhp45"    # Modrinth project id of PackTest


def fetch(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=300) as r:
        return r.read()


def props():
    out = {}
    for line in (ROOT / "gradle.properties").read_text().splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            k, v = line.split("=", 1)
            out[k.strip()] = v.strip()
    return out


def modrinth_file(project, mc, version_number=None):
    """Download a Fabric version of a Modrinth project for mc; return its cached path."""
    q = urllib.parse.urlencode({"game_versions": json.dumps([mc]), "loaders": json.dumps(["fabric"])})
    versions = json.loads(fetch(f"https://api.modrinth.com/v2/project/{project}/version?{q}"))
    if version_number:
        versions = [v for v in versions if v["version_number"] == version_number]
    if not versions:
        sys.exit(f"Modrinth project {project} has no Fabric build for {mc}"
                 + (f" numbered {version_number}" if version_number else ""))
    f = next(x for x in versions[0]["files"] if x["primary"]) if any(x["primary"] for x in versions[0]["files"]) else versions[0]["files"][0]
    path = CACHE / f["filename"]
    if not path.is_file():
        data = fetch(f["url"])
        if hashlib.sha512(data).hexdigest() != f["hashes"]["sha512"]:
            sys.exit(f"SHA-512 mismatch for {f['filename']}; refusing to use it")
        CACHE.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    return path


def fabric_server(mc, loader):
    installer = next(v["version"] for v in json.loads(fetch("https://meta.fabricmc.net/v2/versions/installer")) if v["stable"])
    path = CACHE / f"fabric-server-{mc}-{loader}-{installer}.jar"
    if not path.is_file():
        CACHE.mkdir(parents=True, exist_ok=True)
        path.write_bytes(fetch(f"https://meta.fabricmc.net/v2/versions/loader/{mc}/{loader}/{installer}/server/jar"))
    return path


def java():
    home = os.environ.get("JAVA_HOME")
    return str(Path(home) / "bin" / "java") if home else "java"


def main(argv):
    keep, timeout, names = False, 600, []
    it = iter(argv)
    for a in it:
        if a == "--keep": keep = True
        elif a == "--timeout": timeout = int(next(it))
        else: names.append(a)

    if subprocess.run([sys.executable, str(ROOT / "tools" / "pack.py"), "build", *names]).returncode:
        sys.exit("static checks failed; fix those first")
    dps = sorted(p for p in (ROOT / "datapacks").iterdir()
                 if (p / "pack.mcmeta").is_file() and (not names or p.name in names))
    if not dps:
        sys.exit("no datapacks to test")
    zips = [max((ROOT / "build" / "packs").glob(f"{p.name}-*.zip"), key=os.path.getmtime) for p in dps]
    tests = sum(len(list(p.glob("data/*/test/**/*.mcfunction"))) for p in dps)

    p = props()
    mc, loader = p["minecraft_version"], p["loader_version"]
    server = fabric_server(mc, loader)
    mods = [modrinth_file(FABRIC_API, mc, p["fabric_api_version"]), modrinth_file(PACKTEST, mc)]

    srv = WORK / "server"
    # Keep downloaded game libraries between runs; reset everything else.
    for child in (srv.iterdir() if srv.is_dir() else []):
        if child.name not in ("libraries", "versions", ".fabric"):
            shutil.rmtree(child) if child.is_dir() else child.unlink()
    (srv / "mods").mkdir(parents=True, exist_ok=True)
    (srv / "world" / "datapacks").mkdir(parents=True, exist_ok=True)
    for m in mods:
        shutil.copy(m, srv / "mods" / m.name)
    for z in zips:
        shutil.copy(z, srv / "world" / "datapacks" / z.name)
    (srv / "eula.txt").write_text("eula=true\n")
    (srv / "server.properties").write_text("online-mode=false\nwhite-list=false\nserver-port=25599\n"
                                           "view-distance=4\nsimulation-distance=4\n")

    print(f"Minecraft {mc}, Fabric {loader}, {', '.join(m.name for m in mods)}")
    print(f"packs: {', '.join(z.name for z in zips)}; {tests} test function(s)")
    cmd = [java(), "-Xmx2G", "-Dpacktest.auto"]
    if os.environ.get("GITHUB_ACTIONS"):
        cmd.append("-Dpacktest.auto.annotations")
    cmd += ["-jar", str(server), "nogui"]
    try:
        run = subprocess.run(cmd, cwd=srv, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                             text=True, timeout=timeout)
        log, code = run.stdout, run.returncode
    except subprocess.TimeoutExpired as e:
        log, code = (e.stdout or b"").decode() if isinstance(e.stdout, bytes) else (e.stdout or ""), "timeout"
    (WORK / "latest.log").write_text(log)

    lines = log.splitlines()
    errors = [l for l in lines if "/ERROR]" in l]
    results = [l for l in lines if "tests" in l.lower() and ("passed" in l.lower() or "failed" in l.lower())]
    for l in results[-3:]:
        print(f"  {l.strip()}")
    for l in lines:
        if "[FAIL]" in l or "failed!" in l.lower() or "assertion" in l.lower():
            print(f"  {l.strip()}")
    if not keep:
        for child in srv.iterdir():
            if child.name not in ("libraries", "versions", ".fabric"):
                shutil.rmtree(child) if child.is_dir() else child.unlink()
    if code != 0 or errors:
        print(f"\nFAILED (exit {code})")
        for e in errors[:20]:
            print(f"  {e.strip()}")
        print(f"full log: {(WORK / 'latest.log').relative_to(ROOT)}")
        sys.exit(1)
    print("\nOK: packs loaded clean and every test passed")


if __name__ == "__main__":
    main(sys.argv[1:])
