#!/usr/bin/env python3
# dte:A3
"""Build the natural test world, testworlds/bubba-natural-world/ (specs/testworld.md "Natural world").

  python tools/build-naturalworld.py            build the mods, then generate and save the world
  options: --no-build   use the mod jars already in mods/*/build/libs
           --keep       keep build/naturalworld-server for inspection (console.log has every reply)
           --seed N     world seed (default SEED below)

How: the same headless Fabric server as tools/build-testworld.py, on a normal (not flat) world with a fixed seed.
It locates a natural example of each of our things with /locate and the mods' own locate commands, loads the
chunks around each, lays a stone platform at spawn with a click sign per example that teleports there, then saves
and copies only the files a singleplayer world needs. The spots it found are written to SPOTS.txt in the world.
"""
import importlib.util, re, shutil, struct, sys, time, zlib
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
_spec = importlib.util.spec_from_file_location("buildtw", TOOLS / "build-testworld.py")
tw = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(tw)
nbt, packtest = tw.nbt, tw.packtest

ROOT = tw.ROOT
WORK = ROOT / "build" / "naturalworld-server"
OUT = ROOT / "testworlds" / "bubba-natural-world"
WORLD_NAME = "Bubba Natural World"
SEED = 20261122
LOAD_RADIUS = 24  # blocks loaded (and saved) round each example


def prepare(seed, jars):
    p = packtest.props()
    mc, loader = p["minecraft_version"], p["loader_version"]
    launcher = packtest.fabric_server(mc, loader)
    fapi = packtest.modrinth_file(packtest.FABRIC_API, mc, p["fabric_api_version"])
    for child in (WORK.iterdir() if WORK.is_dir() else []):
        if child.name not in ("libraries", "versions", ".fabric"):
            shutil.rmtree(child) if child.is_dir() else child.unlink()
    (WORK / "mods").mkdir(parents=True, exist_ok=True)
    for j in [fapi, *jars]:
        shutil.copy(j, WORK / "mods" / j.name)
    (WORK / "eula.txt").write_text("eula=true\n")
    (WORK / "server.properties").write_text(
        "online-mode=false\nwhite-list=false\nserver-port=25599\nspawn-protection=0\n"
        f"level-name=world\nlevel-seed={seed}\nlevel-type=minecraft\\:normal\n"
        "generate-structures=true\ngamemode=creative\ndifficulty=normal\n"
        "view-distance=3\nsimulation-distance=3\nsync-chunk-writes=false\n")
    return mc, launcher, fapi


def ask(s, cmd, secs=180):
    """Sends one command and returns the console lines of its reply."""
    start = len(s.log)
    s.raw(cmd)
    s.raw("say nwmark")
    if not s.read_until(lambda l: "nwmark" in l, secs):
        s.fail(f"no reply to: {cmd}")
    return [l.split("]: ", 1)[-1] for l in s.log[start:] if "nwmark" not in l]


XZ = re.compile(r"\[(-?\d+), (?:~|-?\d+), (-?\d+)\]")


def locate_at(s, x, z, what):
    """`locate <what>` from (x, z): the found (x, z), or None."""
    for l in ask(s, f"execute positioned {x} 0 {z} run locate {what}"):
        m = XZ.search(l)
        if m:
            return int(m.group(1)), int(m.group(2))
    return None


def mod_locate(s, sub, pattern, x=0, z=0):
    """Our own `locate` subcommands: (x, z), or (x, z, y) when the pattern also captures a height."""
    for l in ask(s, f"execute positioned {x} 0 {z} run bubbas_reworked_mobs {sub} locate"):
        m = re.search(pattern, l)
        if m:
            g = [int(v) for v in m.groups()]
            return tuple(g)
    return None


def find_examples(s):
    """(label line 1, line 2, x, z) for each example found."""
    out = []

    def add(l1, l2, at):
        """at: (x, z) from locate, or (x, y, z) when the height is known (then the sign goes to that height)."""
        if at:
            x, y, z = (at[0], None, at[1]) if len(at) == 2 else at
            if all(abs(x - e[2]) + abs(z - e[3]) > 16 for e in out if e[0] == l1):
                out.append((l1, l2, x, z, y))
                print(f"  {l1} {l2}: {x} {z}" + (f" (y {y})" if y is not None else ""))
        else:
            print(f"  {l1} {l2}: not found")

    # Crypt yards: the nearest three from spread-out starting points (each yard's crypt wealth is rolled per yard).
    for i, (x, z) in enumerate([(0, 0), (1200, 0), (-1200, 1200), (0, -1200)]):
        add("Crypt yard", f"#{i + 1}", locate_at(s, x, z, "structure bubbas_reworked_mobs:crypt_yard"))
    # Camps: kind follows the climate, so start from a biome of each.
    for kind, biome in [("skeleton/zombie", "minecraft:plains"), ("skeleton/zombie", "minecraft:forest"),
                        ("husk", "minecraft:desert"), ("stray", "minecraft:snowy_plains"), ("bogged", "minecraft:swamp")]:
        b = locate_at(s, 0, 0, f"biome {biome}")
        add("Camp", kind, b and locate_at(s, b[0], b[1], "structure bubbas_reworked_mobs:monster_camp"))
    add("Pillager", "outpost", locate_at(s, 0, 0, "structure minecraft:pillager_outpost"))
    add("Shipwreck", "with crew", locate_at(s, 0, 0, "structure minecraft:shipwreck"))
    add("Fabricator", "(ancient city)", mod_locate(s, "ancient_fabricator", r"pad at (-?\d+) (-?\d+) (-?\d+)"))
    add("Cairn", "(endermen)", mod_locate(s, "cairn", r"Cairn site at (-?\d+) -?\d+ (-?\d+)"))
    add("Giant spiders", "old growth taiga", locate_at(s, 0, 0, "biome minecraft:old_growth_spruce_taiga")
        or locate_at(s, 0, 0, "biome minecraft:old_growth_pine_taiga"))
    return out


def load_around(s, examples):
    for _, _, x, z, _ in examples:
        box = (x - LOAD_RADIUS, z - LOAD_RADIUS, x + LOAD_RADIUS, z + LOAD_RADIUS)
        s._run([("load", "forceload add %d %d %d %d" % box)], True, 40)
        s.wait_loaded(*box, secs=300)  # fresh terrain far out generates slowly on a busy PC
    # Camps report their kind once their chunks have generated.
    return ask(s, "bubbas_reworked_mobs camp list")


def sign_cmd(dx, l1, l2, x, z, y=None):
    tp = (f"tp @s {x} {y + 1} {z}" if y is not None
          else f"execute positioned {x} 0 {z} positioned over motion_blocking_no_leaves run tp @s ~ ~1 ~")
    return (f"execute positioned 0 0 0 positioned over motion_blocking_no_leaves run setblock ~{dx} ~ ~3 "
            f"minecraft:oak_sign[rotation=8]{{allow_op_features:1b,front_text:{{messages:["
            f"{{text:\"{l1}\",bold:true,color:\"dark_purple\",click_event:{{action:\"run_command\",command:\"{tp}\"}}}},"
            f"{{text:\"{l2}\"}},{{text:\"{x} {z}\",color:\"gray\"}},{{text:\"click to go\",color:\"gray\"}}]}}}}")


def build(seed, jars, timeout):
    mc, launcher, fapi = prepare(seed, jars)
    print(f"Minecraft {mc}; seed {seed}; mods: {', '.join(j.name for j in [fapi, *jars])}")
    t0 = time.time()
    s = tw.Server(WORK, launcher, timeout)
    print(f"server up in {time.time() - t0:.0f}s; locating")
    s.run([("rules", c) for c in ("gamerule spawn_mobs false", "gamerule advance_time false", "time set 1000",
                                  "gamerule advance_weather false", "weather clear")], quiet=True)
    examples = find_examples(s)
    camps = load_around(s, examples)
    width = len(examples)
    # Spawn: a stone platform at world spawn (0, 0) with a sign per example in a row to the south.
    half = width + 1
    base = "execute positioned 0 0 0 positioned over motion_blocking_no_leaves run "
    s._run([("spawn", "forceload add -16 -16 16 16")], True, 40)
    s.read_until(lambda l: False, 15)  # the spawn area is kept loaded anyway; its corner probe can stall
    # "positioned over" is the first free block above the surface at (0, 0); after the platform it is the block on it.
    cmds = [("spawn", base + f"fill ~-{half} ~ ~-2 ~{half} ~ ~5 minecraft:polished_andesite"),
            ("spawn", base + "setworldspawn ~ ~ ~"),
            ("spawn", base + f"setblock ~{half} ~2 ~5 minecraft:glass"),  # so the clearing fill always changes a block
            ("spawn", base + f"fill ~-{half} ~ ~-2 ~{half} ~5 ~5 minecraft:air")]
    for i, (l1, l2, x, z, y) in enumerate(examples):
        cmds.append(("sign", sign_cmd(-width + 1 + 2 * i, l1, l2, x, z, y)))
    s.run(cmds)
    s.run([("teardown", c) for c in ("forceload remove all", "save-all flush")], quiet=True)
    s.stop()
    s.save_log()
    s.problems += s.errors()
    if s.problems:
        s.report_and_exit()
    print(f"world generated in {time.time() - t0:.0f}s of server time")
    return mc, examples, camps


def prune_regions(world):
    """Keeps only fully generated chunks: /locate leaves part-generated chunks (structure starts) all over the map,
    and they are most of the size. They'd finish generating if a player went there, so dropping them loses nothing."""
    kept = dropped = 0
    for folder in ("region", "entities", "poi"):
        d = world / "dimensions" / "minecraft" / "overworld" / folder
        full_region = {}
        for mca in sorted(d.glob("*.mca")) if d.is_dir() else []:
            raw = mca.read_bytes()
            if len(raw) < 8192:
                mca.unlink()
                continue
            chunks = []
            for i in range(1024):
                off = int.from_bytes(raw[i * 4:i * 4 + 3], "big")
                if not off:
                    continue
                at = off * 4096
                length = struct.unpack(">i", raw[at:at + 4])[0]
                body = raw[at + 4:at + 4 + length]
                ts = raw[4096 + i * 4:4096 + i * 4 + 4]
                if folder == "region":
                    try:
                        _, root = nbt.loads(zlib.decompress(body[1:]) if body[0] == 2 else body[1:])
                        st = root.value.get("Status")
                        full = st is not None and st.value == "minecraft:full"
                    except Exception:
                        full = False
                    full_region.setdefault(mca.name, set())
                    if full:
                        full_region[mca.name].add(i)
                    keep = full
                else:  # entities and POIs only for chunks kept in region/
                    keep = i in prune_regions.full.get(mca.name, set())
                if keep:
                    chunks.append((i, ts, body))
                    kept += folder == "region"
                else:
                    dropped += folder == "region"
            if folder == "region":
                prune_regions.full = full_region
            if not chunks:
                mca.unlink()
                continue
            header, ts_table, data = bytearray(4096), bytearray(4096), bytearray()
            for i, ts, body in chunks:
                sector = 2 + len(data) // 4096
                blob = struct.pack(">i", len(body)) + body
                blob += b"\0" * (-len(blob) % 4096)
                header[i * 4:i * 4 + 4] = sector.to_bytes(3, "big") + bytes([len(blob) // 4096])
                ts_table[i * 4:i * 4 + 4] = ts
                data += blob
            mca.write_bytes(bytes(header) + bytes(ts_table) + bytes(data))
    print(f"kept {kept} full chunks, dropped {dropped} part-generated ones")


prune_regions.full = {}


def copy_world(mc, jars, seed, examples, camps):
    src = WORK / "world"
    if OUT.exists():
        shutil.rmtree(OUT)
    OUT.mkdir(parents=True)
    for name in tw.KEEP:
        p = src / name
        if p.is_dir():
            shutil.copytree(p, OUT / name)
        elif p.is_file():
            shutil.copy2(p, OUT / name)
    root_name, root = nbt.read_file(OUT / "level.dat")
    data = root.value["Data"].value
    data["LevelName"] = nbt.string(WORLD_NAME)
    data["GameType"] = nbt.int_(1)
    data["allowCommands"] = nbt.byte(1)
    nbt.write_file(OUT / "level.dat", root_name, root)
    prune_regions(OUT)
    lines = [f"seed: {seed}", f"minecraft: {mc}", f"mods: {', '.join(j.name for j in jars)}",
             "builder: python tools/build-naturalworld.py; spec: specs/testworld.md (Natural world)",
             "Pristine copy: never open this folder in the game. tools/install-to-modrinth.ps1 copies it into saves/.", "",
             "Examples (signs at spawn teleport to each):"]
    lines += [f"  {l1} {l2}: {x} {z}" + (f" (y {y})" if y is not None else "") for l1, l2, x, z, y in examples]
    lines += ["", "Camps registered once loaded (/bubbas_reworked_mobs camp list):"] + [f"  {l}" for l in camps if l.strip()]
    (OUT / "SPOTS.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")
    size = sum(f.stat().st_size for f in OUT.rglob("*") if f.is_file())
    print(f"natural world -> {OUT.relative_to(ROOT)} ({size // 1024} KiB)")


def main(argv):
    do_build, keep, seed, timeout = True, False, SEED, 300
    it = iter(argv)
    for a in it:
        if a == "--no-build": do_build = False
        elif a == "--keep": keep = True
        elif a == "--seed": seed = int(next(it))
        else: sys.exit(__doc__)
    t0 = time.time()
    tw.java()
    if do_build:
        tw.gradle_build()
    jars = tw.mod_jars()
    mc, examples, camps = build(seed, jars, timeout)
    copy_world(mc, jars, seed, examples, camps)
    if not keep:
        for child in (WORK.iterdir() if WORK.is_dir() else []):
            if child.name not in ("libraries", "versions", ".fabric"):
                shutil.rmtree(child) if child.is_dir() else child.unlink()
    print(f"done in {time.time() - t0:.0f}s")


if __name__ == "__main__":
    main(sys.argv[1:])
