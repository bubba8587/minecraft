#!/usr/bin/env python3
"""Death-time model for a player held by drowned (specs/drowned.md, "danger"). Monte Carlo over vanilla's rules:
air 300, -1 a tick under water (Respiration: skipped with chance bonus/(bonus+1)), at -20 two drowning damage (bypasses
armor) and air back to 0; damage within the 10-tick hurt window only counts above the last hit. The drowned's hits
escalate with the grip: it starts at GRIP_START and rises every tick (faster with extra holders); its value
above GRIP_START (start values at 50 or less, full at 100) sets the hit chance, damage and air per hit (numbers mirror DrownedTuning; keep them in step). Nobody hits back
here: this is the "panics" time. Usage: python3 tools/drowned-danger.py
"""
import random

GRIP_START = 50.0
GRIP_RISE = 0.25
GRIP_RISE_PER_HELPER = 0.5
CHANCE = (1 / 50, 1 / 10)
DAMAGE = (2.0, 4.0)
AIR = (45, 90)


def lerp(p, a):
	return a[0] + (a[1] - a[0]) * p


def armor_reduce(dmg, armor, tough):
	eff = min(20.0, max(armor / 5.0, armor - dmg / (2.0 + tough / 4.0)))
	return dmg * (1.0 - eff / 25.0)


def run(armor=0, tough=0, oxygen=0, holders=1, seed=None):
	rnd = random.Random(seed)
	hp, air, t = 20.0, 300, 0
	invul, last = 0, 0.0

	def hurt(amount):
		nonlocal hp, invul, last
		if invul > 10:
			if amount <= last:
				return
			hp -= amount - last
			last = amount
		else:
			hp -= amount
			last = amount
			invul = 20

	while hp > 0 and t < 2400:
		t += 1
		if invul > 0:
			invul -= 1
		if not (oxygen > 0 and rnd.random() >= 1.0 / (oxygen + 1)):
			air -= 1
		if air <= -20:
			air = 0
			hurt(2.0)
		grip = min(100.0, GRIP_START + GRIP_RISE * (1 + GRIP_RISE_PER_HELPER * (holders - 1)) * t)
		p = max(0.0, (grip - GRIP_START) / (100.0 - GRIP_START))
		for _ in range(holders):
			if rnd.random() < lerp(p, CHANCE):
				hurt(armor_reduce(lerp(p, DAMAGE), armor, tough))
				air = max(0, air - round(lerp(p, AIR) / (1 + oxygen)))
	return t / 20.0


def median(**kw):
	times = sorted(run(seed=i, **kw) for i in range(400))
	return times[len(times) // 2], times[len(times) // 10], times[len(times) * 9 // 10]


if __name__ == "__main__":
	cases = [
		("unarmored", dict()),
		("iron armor", dict(armor=15)),
		("diamond armor", dict(armor=20, tough=8)),
		("unarmored, Respiration III", dict(oxygen=3)),
		("diamond, Respiration III", dict(armor=20, tough=8, oxygen=3)),
		("unarmored, 2 drowned", dict(holders=2)),
		("unarmored, 3 drowned", dict(holders=3)),
	]
	for name, kw in cases:
		m, lo, hi = median(**kw)
		print(f"{name:30s} median {m:5.1f} s  (10-90 %: {lo:.1f}-{hi:.1f} s)")
