#!/usr/bin/env python3
# dte:B5
"""Unpack the decompiled Minecraft source and vanilla data into build/mcsrc/ for grepping.

  ./gradlew :refmod:genSources      (once per Minecraft version, any mod works)
  python3 tools/mcsrc.py            -> build/mcsrc/<version>/{common,client,vanilla}

common/ and client/ are the decompiled Java sources (Mojang names). vanilla/ is the client jar
without classes: data/minecraft/** (every vanilla datapack file), assets/minecraft/**, version.json.
"""
import sys, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CACHE = ROOT / ".gradle" / "loom-cache" / "minecraftMaven" / "net" / "minecraft"


def version():
    for line in (ROOT / "gradle.properties").read_text().splitlines():
        if line.startswith("minecraft_version="):
            return line.split("=", 1)[1].strip()


def main():
    mc = sys.argv[1] if len(sys.argv) > 1 else version()
    out = ROOT / "build" / "mcsrc" / mc
    found = False
    for part, glob in (("common", "minecraft-common-*"), ("client", "minecraft-clientOnly-*")):
        jars = list(CACHE.glob(f"{glob}/{mc}/*-{mc}-sources.jar"))
        if jars:
            found = True
            zipfile.ZipFile(jars[0]).extractall(out / part)
            print(f"{part}: {out / part}")
    if not found:
        sys.exit(f"no decompiled sources for {mc}; run ./gradlew :refmod:genSources first")
    client_jar = Path.home() / ".gradle" / "caches" / "fabric-loom" / mc / "minecraft-client.jar"
    if client_jar.is_file():
        with zipfile.ZipFile(client_jar) as z:
            for n in z.namelist():
                if not n.endswith(".class"):
                    z.extract(n, out / "vanilla")
        print(f"vanilla: {out / 'vanilla'}")


if __name__ == "__main__":
    main()
