#!/usr/bin/env python3
# dte:A3
"""Build the pristine test world, testworlds/bubba-test-world/ (specs/testworld.md).

  python tools/build-testworld.py              build both mods, then build and verify the world
  options: --no-build     use the mod jars already in mods/*/build/libs
           --verify-only  only re-check the pristine world that is already there
           --keep         keep the server folders in build/ for inspection (console.log has every reply)
           --verbose      print every command and its reply
           --timeout S    seconds to wait for a server to start (default 300)

How: a headless Fabric server (launcher, Fabric API and checksums as in tools/packtest.py, sharing its download
cache) with Fabric API and the repo's mods creates a fresh superflat world. The builder datapack
(testworlds/builder/) is installed so the server parses its functions, and every command of its
bubba_testworld:build function (following `function` lines) is sent through the console one by one so each reply
can be checked; any error fails the build. Then save, stop, copy the world (only the files a singleplayer world
needs) to testworlds/bubba-test-world/, fix level.dat for singleplayer, and start a second server on a copy of the
result to run bubba_testworld:verify (`execute if ...` checks) the same way.
Needs Java 25 (JAVA_HOME, PATH, or the Modrinth App's Java 25 runtime on Windows). Standard library only.
"""
import datetime, os, queue, re, shutil, subprocess, sys, threading, time
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
sys.path.insert(0, str(TOOLS))
import nbt  # noqa: E402
import packtest  # noqa: E402  (download helpers and cache)

ROOT = TOOLS.parent
WORK = ROOT / "build" / "testworld-server"
VERIFY = ROOT / "build" / "testworld-verify"
BUILDER = ROOT / "testworlds" / "builder"
NS = "bubba_testworld"
OUT = ROOT / "testworlds" / "bubba-test-world"
WORLD_NAME = "Bubba Test World"
VERBOSE = False  # --verbose: print every command and its reply
MODS = ["bubbas_reworked_mobs", "bubbas_crafting_tweaks"]
# Bedrock, two dirt, grass: the classic superflat. The ground's top is y=-61, so stations stand on y=-60.
FLAT = ('{"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},'
        '{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains","lakes":false,"features":false}')
# Console replies that mean a command failed (tools/packload.py's list plus a few).
CMD_ERRORS = re.compile(r"Unknown or incomplete command|Incorrect argument|Unknown |Expected |Invalid |"
                        r"No player was found|No entity was found|Can't find element|<--\[HERE\]|Test failed|"
                        r"That position is not loaded|Could not|Couldn't|Failed to|No blocks were filled|"
                        r"Too many blocks|Nothing changed|is not a container")
# World files a singleplayer world needs; everything else (session.lock, level.dat_old, logs) stays behind.
KEEP = ("level.dat", "data", "dimensions")


def java():
    home = os.environ.get("JAVA_HOME")
    if not home and os.name == "nt":  # the Modrinth App's Java 25 runtime (tools/install-to-modrinth.ps1 does the same)
        base = Path(os.environ.get("APPDATA", "")) / "ModrinthApp" / "meta" / "java_versions"
        found = sorted(d for d in base.glob("*25*") if d.is_dir()) if base.is_dir() else []
        if found:
            home = str(found[0])
            os.environ["JAVA_HOME"] = home
    return str(Path(home) / "bin" / "java") if home else "java"


def gradle_build():
    java()  # sets JAVA_HOME for Gradle if it can
    gw = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    tasks = [f":{m}:assemble" for m in MODS]
    print(f"building {', '.join(MODS)}")
    if subprocess.run([str(gw), *tasks, "--console=plain", "-q"], cwd=ROOT).returncode:
        sys.exit("mod build failed")


def mod_jars():
    jars = []
    for m in MODS:
        found = [j for j in (ROOT / "mods" / m / "build" / "libs").glob(f"{m}-*.jar") if "sources" not in j.name]
        if not found:
            sys.exit(f"no jar for {m}; run without --no-build")
        jars.append(max(found, key=os.path.getmtime))
    return jars


def function_lines(fid, seen=()):
    """Commands of function ns:path in order, with `function` lines expanded (no macros, no execute ... function)."""
    ns, path = fid.split(":", 1)
    f = BUILDER / "data" / ns / "function" / f"{path}.mcfunction"
    if not f.is_file():
        sys.exit(f"no function {fid} ({f})")
    if fid in seen:
        sys.exit(f"function loop at {fid}")
    out = []
    for n, raw in enumerate(f.read_text(encoding="utf-8").splitlines(), 1):
        s = raw.strip()
        if not s or s.startswith("#"):
            continue
        if s.startswith("$"):
            sys.exit(f"{f.name}:{n}: macros can't be sent through the console")
        m = re.fullmatch(r"function (\S+)", s)
        if m:
            out += function_lines(m.group(1), (*seen, fid))
        else:
            out.append((f"{path}.mcfunction:{n}", s))
    return out


def placed_checks():
    """An `execute if block` check for every setblock and loot display command of the build."""
    out = []
    for where, c in function_lines(f"{NS}:build"):
        m = re.match(r"setblock (\S+ \S+ \S+) ([^{\s]+)", c)
        if m:
            out.append((where, f"execute if block {m.group(1)} {m.group(2)}"))
        m = re.match(r"bubbas_reworked_mobs display (\w+) \S+ (\S+ \S+ \S+) (\w+)$", c)
        if m:
            out.append((where, f"execute if block {m.group(2)} bubbas_reworked_mobs:loot_display"
                               f"[placement={m.group(1)},facing={m.group(3)}]"))
    return out


def prepare_server(srv, jars, with_builder):
    p = packtest.props()
    mc, loader = p["minecraft_version"], p["loader_version"]
    launcher = packtest.fabric_server(mc, loader)
    fapi = packtest.modrinth_file(packtest.FABRIC_API, mc, p["fabric_api_version"])
    # Keep downloaded game libraries between runs; reset everything else.
    for child in (srv.iterdir() if srv.is_dir() else []):
        if child.name not in ("libraries", "versions", ".fabric"):
            shutil.rmtree(child) if child.is_dir() else child.unlink()
    (srv / "mods").mkdir(parents=True, exist_ok=True)
    for j in [fapi, *jars]:
        shutil.copy(j, srv / "mods" / j.name)
    if with_builder:
        shutil.copytree(BUILDER, srv / "world" / "datapacks" / "builder")
    (srv / "eula.txt").write_text("eula=true\n")
    (srv / "server.properties").write_text(
        "online-mode=false\nwhite-list=false\nserver-port=25598\nspawn-protection=0\n"
        "level-name=world\nlevel-type=minecraft\\:flat\n"
        f"generator-settings={FLAT.replace(':', chr(92) + ':')}\n"
        "generate-structures=false\ngamemode=creative\ndifficulty=normal\n"
        "view-distance=3\nsimulation-distance=3\nsync-chunk-writes=false\n")
    return mc, launcher, fapi


class Server:
    """A server process driven through its console; send() checks each command's reply."""

    def __init__(self, srv, launcher, timeout):
        self.srv, self.log, self.problems = srv, [], []
        self.proc = subprocess.Popen([java(), "-Xmx2G", "-jar", str(launcher), "nogui"], cwd=srv,
                                     stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                     text=True, encoding="utf-8", errors="replace", bufsize=1)
        self.lines = queue.Queue()
        threading.Thread(target=self._pump, daemon=True).start()
        if not self.read_until(lambda l: "Done (" in l and "For help" in l, timeout):
            self.fail(f"server did not finish starting (exit code {self.proc.poll()})")

    def _pump(self):
        for l in self.proc.stdout:
            self.lines.put(l)
        self.lines.put(None)

    def read_until(self, pred, secs):
        end = time.time() + secs
        while time.time() < end:
            try:
                l = self.lines.get(timeout=0.5)
            except queue.Empty:
                continue
            if l is None:
                return False
            self.log.append(l.rstrip())
            if pred(l):
                return True
        return False

    def fail(self, msg):
        self.problems.append(msg)
        self.stop()
        self.report_and_exit()

    def raw(self, cmd):
        try:
            self.proc.stdin.write(cmd + "\n")
            self.proc.stdin.flush()
        except (BrokenPipeError, OSError):
            self.fail(f"server exited before command: {cmd}")

    def run(self, cmds, quiet=None, batch=40):
        """Send (where, command) pairs, each followed by a marker, and check the replies. After
        `forceload add x1 z1 x2 z2` it waits until those chunks (and their entities) are loaded."""
        for i, (where, c) in enumerate(cmds):
            m = re.fullmatch(r"forceload add (-?\d+) (-?\d+) (-?\d+) (-?\d+)", c)
            if m:
                self._run(cmds[:i + 1], quiet, batch)
                self.wait_loaded(*map(int, m.groups()))
                return self.run(cmds[i + 1:], quiet, batch)
        self._run(cmds, quiet, batch)

    def _run(self, cmds, quiet, batch):
        quiet = not VERBOSE if quiet is None else quiet
        replies = {}
        for b in range(0, len(cmds), batch):
            chunk = cmds[b:b + batch]
            start = len(self.log)
            for i, (_, c) in enumerate(chunk, b):
                self.raw(c)
                self.raw(f"say twmark-{i}")
            if not self.read_until(lambda l, m=f"twmark-{b + len(chunk) - 1}": m in l, 120):
                self.fail(f"no reply to commands from {chunk[0][0]}")
            seg, i = self.log[start:], b
            cur = []
            for l in seg:
                m = re.search(r"twmark-(\d+)\b", l)
                if m and "[Server]" in l:
                    replies[int(m.group(1))] = cur
                    cur = []
                else:
                    cur.append(l)
            for i, (where, c) in enumerate(chunk, b):
                reply = [r for r in replies.get(i, []) if "/INFO]" in r or "/WARN]" in r or "/ERROR]" in r or "HERE" in r]
                bad = [r for r in reply if CMD_ERRORS.search(r.split("]: ", 1)[-1]) and "Can't keep up" not in r]
                if not quiet:
                    print(f"> {c[:150]}")
                    for r in reply:
                        print(f"    {r.split(']: ', 1)[-1][:200]}")
                if bad:
                    self.problems.append(f"{where}: {c}\n      -> {bad[0].split(']: ', 1)[-1]}")

    def wait_loaded(self, x1, z1, x2, z2, secs=60):
        end = time.time() + secs
        probe = [("forceload", f"execute if loaded {x} 0 {z}") for x in (x1, x2) for z in (z1, z2)]
        while time.time() < end:
            start = len(self.log)
            for _, c in probe:
                self.raw(c)
            self.raw("say twmark-loaded")
            self.read_until(lambda l: "twmark-loaded" in l, 30)
            if sum("Test passed" in l for l in self.log[start:]) == len(probe):
                self.read_until(lambda l: False, 3)  # entities in new chunks show up a moment later
                return
            self.read_until(lambda l: False, 1)
        self.fail("forceloaded chunks never finished loading")

    def stop(self):
        if self.proc.poll() is None:
            self.raw("stop")
            self.read_until(lambda l: False, 1)
            try:
                self.proc.wait(90)
            except subprocess.TimeoutExpired:
                self.proc.kill()
            time.sleep(0.5)
            while True:
                try:
                    l = self.lines.get_nowait()
                except queue.Empty:
                    break
                if l is not None:
                    self.log.append(l.rstrip())

    def save_log(self):
        (self.srv / "console.log").write_text("\n".join(self.log) + "\n", encoding="utf-8")

    def errors(self):
        return [l for l in self.log if "/ERROR]" in l]

    def report_and_exit(self):
        self.save_log()
        print("\nFAILED")
        for p in self.problems:
            print(f"  {p}")
        print(f"console log: {(self.srv / 'console.log').relative_to(ROOT)}")
        sys.exit(1)


def build_world(jars, timeout):
    mc, launcher, fapi = prepare_server(WORK, jars, with_builder=True)
    print(f"Minecraft {mc}; mods: {', '.join(j.name for j in [fapi, *jars])}")
    cmds = function_lines(f"{NS}:build")
    t0 = time.time()
    s = Server(WORK, launcher, timeout)
    print(f"server up in {time.time() - t0:.0f}s; {len(cmds)} build commands")
    start = len(s.log)
    s.run([("check", "datapack list enabled")], quiet=True)
    if not any("file/builder" in l for l in s.log[start:]):
        s.problems.append("builder datapack was not enabled (see the console log for why it didn't load)")
    s.run(cmds)
    s.run([("teardown", c) for c in ('datapack disable "file/builder"', "forceload remove all", "save-all flush")])
    s.stop()
    s.save_log()
    s.problems += s.errors()
    if s.problems:
        s.report_and_exit()
    print(f"world built in {time.time() - t0:.0f}s of server time")
    return mc


def copy_world(mc, jars):
    src = WORK / "world"
    if OUT.exists():
        shutil.rmtree(OUT)
    OUT.mkdir(parents=True)
    for name in KEEP:
        p = src / name
        if p.is_dir():
            shutil.copytree(p, OUT / name)
        elif p.is_file():
            shutil.copy2(p, OUT / name)
    # level.dat: singleplayer name, creative, cheats on; forget the builder pack.
    root_name, root = nbt.read_file(OUT / "level.dat")
    data = root.value["Data"].value
    data["LevelName"] = nbt.string(WORLD_NAME)
    data["GameType"] = nbt.int_(1)
    data["allowCommands"] = nbt.byte(1)
    packs = data.get("DataPacks")
    if packs:
        for key in ("Enabled", "Disabled"):
            if key in packs.value:
                et, items = packs.value[key].value
                packs.value[key] = nbt.Tag(nbt.LIST, (et, [t for t in items if t.value != "file/builder"]))
    nbt.write_file(OUT / "level.dat", root_name, root)
    stamp = datetime.datetime.now().astimezone().replace(microsecond=0).isoformat()
    (OUT / "BUILD_INFO.txt").write_text(
        f"built: {stamp}\nminecraft: {mc}\nmods: {', '.join(j.name for j in jars)}\n"
        "builder: testworlds/builder (python tools/build-testworld.py); spec: specs/testworld.md\n"
        "Pristine copy: never open this folder in the game. tools/install-to-modrinth.ps1 copies it into saves/.\n",
        encoding="utf-8")
    size = sum(f.stat().st_size for f in OUT.rglob("*") if f.is_file())
    print(f"pristine world -> {OUT.relative_to(ROOT)} ({size // 1024} KiB)")


def verify(jars, timeout):
    if not (OUT / "level.dat").is_file():
        sys.exit("no pristine world to verify")
    mc, launcher, _ = prepare_server(VERIFY, jars, with_builder=False)
    shutil.copytree(OUT, VERIFY / "world")
    cmds = function_lines(f"{NS}:verify") + placed_checks()
    s = Server(VERIFY, launcher, timeout)
    s.run(cmds)
    s.stop()
    s.save_log()
    s.problems += s.errors()
    if s.problems:
        s.report_and_exit()
    print(f"verified: {len(cmds)} checks passed on a copy of the pristine world, no errors in the log")


def main(argv):
    do_build, verify_only, keep, timeout = True, False, False, 300
    it = iter(argv)
    for a in it:
        if a == "--no-build": do_build = False
        elif a == "--verify-only": verify_only, do_build = True, False
        elif a == "--keep": keep = True
        elif a == "--verbose":
            global VERBOSE
            VERBOSE = True
        elif a == "--timeout": timeout = int(next(it))
        else: sys.exit(__doc__)
    t0 = time.time()
    java()
    if subprocess.run([sys.executable, str(TOOLS / "pack.py"), "check", "builder"]).returncode:
        sys.exit("builder datapack has static problems")
    if do_build:
        gradle_build()
    jars = mod_jars()
    if not verify_only:
        mc = build_world(jars, timeout)
        copy_world(mc, jars)
    verify(jars, timeout)
    if not keep:
        for d in (WORK, VERIFY):
            for child in (d.iterdir() if d.is_dir() else []):
                if child.name not in ("libraries", "versions", ".fabric"):
                    shutil.rmtree(child) if child.is_dir() else child.unlink()
    print(f"done in {time.time() - t0:.0f}s")


if __name__ == "__main__":
    main(sys.argv[1:])
