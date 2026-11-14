#!/usr/bin/env python3
# dte:A4
"""Load datapacks on the official vanilla server (no mods) and fail on any load error.

Use this for a fast load check with no third-party code; tools/packtest.py runs the
PackTest test functions.

  python tools/packload.py [NAME ...]        load packs in datapacks/ (all if none named)
  options: --mc VERSION   Minecraft version (default: minecraft_version in gradle.properties)
           --keep         keep the server folder (build/packload/server) for inspection
           --timeout S    seconds to wait for the server (default 300)

Builds each pack with tools/pack.py, downloads the official server jar from Mojang (SHA-1
checked, cached in build/packtest/), starts it in a fresh world with the packs installed,
runs each pack's optional `packtest.commands` (one console command per line, # comments,
`wait <seconds>` to pause; failures inside functions print nothing, so check results directly),
and fails if the log shows an error or a warning that mentions a tested pack's namespace,
or if a command answers with an error. Needs Java matching the Minecraft version on PATH
or in JAVA_HOME. Standard library only.
"""
import hashlib, json, os, queue, re, shutil, subprocess, sys, threading, time, urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WORK = ROOT / "build" / "packload"
MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
UA = {"User-Agent": "minecraft-repo-packtest"}
# Console replies that mean a command failed.
CMD_ERRORS = re.compile(r"Unknown or incomplete command|Incorrect argument|Unknown function|"
                        r"Expected |Invalid (?:name|UUID|position|entity|block|item|number|integer|float)|"
                        r"No player was found|No entity was found|Can't find element|<--\[HERE\]|"
                        r"Test failed|That position is not loaded|Could not set the block|"
                        r"Unknown (?:item|block|entity|dimension|predicate|loot table)")


def fetch(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=120) as r:
        return r.read()


def default_mc():
    for line in (ROOT / "gradle.properties").read_text().splitlines():
        if line.startswith("minecraft_version="):
            return line.split("=", 1)[1].strip()
    sys.exit("minecraft_version not found in gradle.properties")


def server_jar(mc):
    jar = WORK / f"server-{mc}.jar"
    if jar.is_file():
        return jar
    versions = json.loads(fetch(MANIFEST))["versions"]
    entry = next((v for v in versions if v["id"] == mc), None)
    if not entry:
        sys.exit(f"Minecraft {mc} not in Mojang's version manifest")
    info = json.loads(fetch(entry["url"]))["downloads"]["server"]
    data = fetch(info["url"])
    if hashlib.sha1(data).hexdigest() != info["sha1"]:
        sys.exit("server jar SHA-1 mismatch; refusing to run it")
    WORK.mkdir(parents=True, exist_ok=True)
    jar.write_bytes(data)
    return jar


def java():
    home = os.environ.get("JAVA_HOME")
    return str(Path(home) / "bin" / "java") if home else "java"


def main(argv):
    mc, keep, timeout, names = default_mc(), False, 300, []
    it = iter(argv)
    for a in it:
        if a == "--mc": mc = next(it)
        elif a == "--keep": keep = True
        elif a == "--timeout": timeout = int(next(it))
        else: names.append(a)

    if subprocess.run([sys.executable, str(ROOT / "tools" / "pack.py"), "build", *names]).returncode:
        sys.exit("static checks failed; fix those first")
    dps = sorted(p for p in (ROOT / "datapacks").iterdir()
                 if (p / "pack.mcmeta").is_file() and not (p / "DISABLED").is_file()
                 and (not names or p.name in names))
    if not dps:
        sys.exit("no datapacks to test")
    namespaces = set()
    for p in dps:
        data = p / "data"
        namespaces |= {d.name for d in data.iterdir() if d.is_dir() and d.name not in ("minecraft", "c")}
    zips = [max((ROOT / "build" / "packs" / "datapacks").glob(f"{p.name}-*.zip"), key=os.path.getmtime) for p in dps]

    jar = server_jar(mc)
    srv = WORK / "server"
    shutil.rmtree(srv, ignore_errors=True)
    (srv / "world" / "datapacks").mkdir(parents=True)
    for z in zips:
        shutil.copy(z, srv / "world" / "datapacks" / z.name)
    (srv / "eula.txt").write_text("eula=true\n")
    (srv / "server.properties").write_text(
        "online-mode=false\nspawn-protection=0\n"
        "view-distance=4\nsimulation-distance=4\nsync-chunk-writes=false\nserver-port=25599\n"
        "initial-enabled-packs=vanilla\n")

    commands = ["datapack list"]
    for p in dps:
        f = p / "packtest.commands"
        if f.is_file():
            commands += [l.strip() for l in f.read_text().splitlines() if l.strip() and not l.strip().startswith("#")]

    print(f"starting Minecraft {mc} server with {', '.join(z.name for z in zips)}")
    proc = subprocess.Popen([java(), "-Xmx2G", "-jar", str(jar), "--nogui"], cwd=srv,
                            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            text=True, bufsize=1)
    lines = queue.Queue()
    threading.Thread(target=lambda: [lines.put(l) for l in proc.stdout] and lines.put(None), daemon=True).start()
    log, problems = [], []

    def read_until(pred, secs):
        end = time.time() + secs
        while time.time() < end:
            try:
                l = lines.get(timeout=1)
            except queue.Empty:
                continue
            if l is None:
                return False
            log.append(l.rstrip())
            if pred(l):
                return True
        return False

    def send(cmd):
        try:
            proc.stdin.write(cmd + "\n"); proc.stdin.flush()
        except (BrokenPipeError, OSError):
            problems.append(f"server exited before command: {cmd}")

    try:
        if not read_until(lambda l: "Done (" in l and "For help" in l, timeout):
            problems.append(f"server did not finish starting (exit code {proc.poll()})")
        else:
            for c in commands:
                if c.startswith("wait "):  # packtest.commands directive: wait N seconds
                    read_until(lambda l: False, float(c.split()[1]))
                    continue
                start = len(log)
                send(c)
                marker = f"packtest-marker-{start}"
                send(f"say {marker}")
                read_until(lambda l, m=marker: m in l, 60)
                reply = log[start:-1]
                print(f"> {c}")
                for r in reply:
                    print(f"  {r}")
                if any(CMD_ERRORS.search(r) for r in reply):
                    problems.append(f"command failed: {c}")
            enabled = next((l for l in log if "data pack(s) enabled" in l), "")
            for z in zips:
                if z.name not in enabled:
                    problems.append(f"{z.name} is not in the enabled pack list")
        if proc.poll() is None:
            send("stop")
            read_until(lambda l: False, 60)
    finally:
        if proc.poll() is None:
            proc.kill()

    ns_re = re.compile("|".join(re.escape(n) + ":" for n in namespaces)) if namespaces else None
    for l in log:
        if "/ERROR]" in l or (("/WARN]" in l) and ns_re and ns_re.search(l)):
            problems.append(l)
    (WORK / "latest.log").write_text("\n".join(log) + "\n")
    if not keep:
        shutil.rmtree(srv, ignore_errors=True)
    if problems:
        print("\nFAILED")
        for p in problems:
            print(f"  {p}")
        print(f"full log: {(WORK / 'latest.log').relative_to(ROOT)}")
        sys.exit(1)
    print("\nOK: all packs loaded clean")


if __name__ == "__main__":
    main(sys.argv[1:])
