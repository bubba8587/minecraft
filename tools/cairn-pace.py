#!/usr/bin/env python3
# dte:C1,D2
"""How long cairns take to build (specs/endermen.md, Spawning): a quick model of the spawn rules, night by night.

The real game runs about one game day per 40 minutes in a client game test (the cairnsim part), so tune here first,
then confirm in game. Mirrors EndermanTuning; change both together.

Model: a night is ~110 spawn checks (every 5 s while it's dark enough for monsters). Each check, if the site is under
its cap, it spawns one enderman with the current chance (times the snowball); a share of newcomers carry a spire
block and place it within about half a minute. Endermen linger the rest of the night (they count toward the cap) and
are gone by the next night. Empty-handed endermen are ignored (offerings and clearing don't grow the spire).

  python3 tools/cairn-pace.py            # table + nights for cairns of 30, 45 and 60 End blocks

'spire' is all of a cairn's End blocks, footing included (the worldgen stub is only natural offerings).
"""
import random
import statistics

CHECKS_PER_NIGHT = 110
CHANCE_EMPTY, CHANCE_ALMOST, CURVE = 0.04, 0.35, 3.0
SNOWBALL = 0.3
CAP_EMPTY, CAP_ALMOST = 2, 8
BUILDERS_EMPTY, BUILDERS_ALMOST = 0.5, 0.9


def chance(g): return CHANCE_EMPTY + (CHANCE_ALMOST - CHANCE_EMPTY) * g ** CURVE
def cap(g): return CAP_EMPTY + round((CAP_ALMOST - CAP_EMPTY) * g * g)
def builders(g): return BUILDERS_EMPTY + (BUILDERS_ALMOST - BUILDERS_EMPTY) * g


def build(spire, rng, stub=0):
    built, nights = stub, []
    while built < spire and len(nights) < 300:
        present = carrying = 0
        before = built
        for _ in range(CHECKS_PER_NIGHT):
            if built >= spire:
                break
            g = built / spire
            if present < cap(g) and rng.random() < min(1.0, chance(g) * (1 + SNOWBALL * present)):
                present += 1
                if rng.random() < builders(g):
                    carrying += 1
            if carrying and rng.random() < 0.2:
                carrying -= 1
                built += 1
        nights.append(built - before)
    return nights


def main():
    print("spire grown  chance/check  cap  builders")
    for g in (0, 0.2, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 0.97):
        print(f"   {g * 100:4.0f} %      {chance(g) * 100:4.1f} %      {cap(g)}    {builders(g) * 100:3.0f} %")
    for spire in (30, 45, 60):
        rng = random.Random(4)
        runs = sorted((build(spire, rng) for _ in range(400)), key=len)
        n = [len(r) for r in runs]
        print(f"{spire}-block spire: median {statistics.median(n):.0f} nights (10-90 %: {n[40]}-{n[360]}); "
              f"typical blocks per night {runs[200]}")


if __name__ == "__main__":
    main()
