<!-- dte:A3 -->
# Backlog

Parked ideas and open items, newest first within each project. Agents add to it when the owner parks something; details live in the linked spec.

## Bubba's Reworked Mobs
- **Grave Talisman as loot:** found only (owner). Place it in grave/crypt loot when worldgen builds them; maybe a rare find on sleepers. Open tuning questions in `specs/undead.md`.
- **Bury-break system:** how buried loot comes out (shovel-only vs breaking by hand, noise, tools). Today: shovel digs it out loudly; breaking by hand also works, slowly like packed dirt. `specs/undead.md`, Loot on display.
- **Survival players placing items on display** (decor). Owner: "will need care and consideration." Ops-only command for now. `specs/undead.md`, Loot on display.
- Anti-waking magic (on the player, applied to sleepers). `specs/undead.md`.
- Mob spawner tweaks. `specs/undead.md`.
- Part 1 next pass: remaining undead variants, armor-shredding fists, Smite insta-kill, bow and headshot rules, stagger animation, zombie grouping, graves and crypts in worldgen, hybrid rendering, real 16x/32x art.
- Later parts: creepers; **monster camps and the blood moon** (structures with fixed rosters and key blocks, a 7-day blood moon that respawns uncleared structures, red-sky effects; owner's earlier idea with integration notes in `docs/research/camps-blood-moon-idea.md`).

## Bubba's Crafting Tweaks
- Person-made mod icon (none yet).
- Real Glider art (16x and 32x, made by a person).

## Effect gems
- **Drawbacks** (shelved by the owner): ideas in `specs/effect-gems.md`.

## Repo
- Owner to read and ratify (or rewrite) the goal drafts A1-A4 (`python3 tools/dte.py unratified`).
- A future mod drastically reducing unenchanted mining speed.
- Test world screenshots: a client game test run with both mods loaded that opens a copy of `testworlds/bubba-test-world` and screenshots the hub and each station (the per-mod client test harness loads one mod at a time). `specs/testworld.md`.
