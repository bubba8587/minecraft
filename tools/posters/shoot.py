#!/usr/bin/env python3
# dte:A3
"""Send a scene script to the live poster camera and wait for it (specs/posters.md).

  python3 tools/posters/shoot.py <scene.txt> [more.txt ...]
  python3 tools/posters/shoot.py --quit

The camera is `./gradlew :postershots:runClientGameTest -PclientTests=posters` running in the background. Each script
is copied into its queue; this waits until the camera renames it .done, then prints the log. Screenshots land in
mods/postershots/build/run/posters/shots/.
"""
import shutil, sys, time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DIR = ROOT / "mods" / "postershots" / "build" / "run" / "posters"
Q = DIR / "queue"


def wait_ready(limit=900):
    t = time.time()
    while time.time() - t < limit:
        s = (DIR / "STATUS").read_text().strip() if (DIR / "STATUS").exists() else "starting"
        if s == "ready" or s.startswith("running"):
            return
        if s == "stopped":
            sys.exit("camera stopped")
        time.sleep(2)
    sys.exit("camera not ready")


def run(script: Path, limit=1800):
    wait_ready()
    target = Q / script.name
    for ext in (".done", ".log"):
        (Q / (script.stem + ext)).unlink(missing_ok=True)
    shutil.copy(script, Q / (script.stem + ".tmp"))
    (Q / (script.stem + ".tmp")).rename(target)
    t = time.time()
    while not (Q / (script.stem + ".done")).exists():
        if time.time() - t > limit:
            sys.exit(f"{script.name}: timed out")
        time.sleep(1)
    log = Q / (script.stem + ".log")
    print(f"== {script.name} ({time.time() - t:.0f}s)")
    if log.exists():
        print(log.read_text())


if __name__ == "__main__":
    if sys.argv[1:] == ["--quit"]:
        Q.mkdir(parents=True, exist_ok=True)
        (Q / "quit.txt").write_text("")
    else:
        for a in sys.argv[1:]:
            run(Path(a))
