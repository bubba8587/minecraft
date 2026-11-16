<!-- dte:A3 -->
# Backlog

Parked ideas and open items, newest first within each project. Agents add to it when the owner parks something; details live in the linked spec.

## Bubba's Reworked Mobs
- **Grave Talisman as loot:** found only (owner). Place it in grave/crypt loot when worldgen builds them; maybe a rare find on sleepers. Open tuning questions in `specs/undead.md`.
- **Bury-break system:** how buried loot comes out (shovel-only vs breaking by hand, noise, tools). Today: shovel digs it out loudly; breaking by hand also works, slowly like packed dirt. `specs/undead.md`, Loot on display.
- **Survival players placing items on display** (decor). Owner: "will need care and consideration." Ops-only command for now. `specs/undead.md`, Loot on display.
- Anti-waking magic (on the player, applied to sleepers). `specs/undead.md`.
- Warding and sleepers (owner, 2026-11-15): light, soul fire and a "lull" game event tag that calm sleepers, plus the Warding bell's toll from Infused Gems (data-only link). See `docs/research/area-gems-and-warding.md`.
- Mob spawner tweaks. `specs/undead.md`.
- Part 1 next pass: remaining undead variants, armor-shredding fists, Smite insta-kill, bow and headshot rules, stagger animation, zombie grouping, graves and crypts in worldgen, hybrid rendering, real 16x/32x art.
- Later parts: creepers; **monster camps and the blood moon** (structures with fixed rosters and key blocks, a 7-day blood moon that respawns uncleared structures, red-sky effects; owner's earlier idea with integration notes in `docs/research/camps-blood-moon-idea.md`).

## Bubba's Crafting Tweaks
- Person-made mod icon (none yet).
- Real Glider art (16x and 32x, made by a person).

## Effect gems
- **Area gems and Warding** (owner ideas, 2026-11-15): mount gems in a vanilla block for an area effect (recommended: bells first, beacons later) and a Warding gem that stops spawns, makes mobs avoid the area and calms sleepers. Options, prior art and owner questions: `docs/research/area-gems-and-warding.md`.
- **Drawbacks** (shelved by the owner): ideas in `specs/effect-gems.md`.
- **Per-weapon tuning for gems in bows, crossbows, tridents and maces** (owner, 2026-11-14): they have no material, so they count as diamond level for now; each probably needs its own factor.
- **Drawback sizes** (owner will playtest): currently 10% each, under 20% stacked.
- **Tipped arrow vs gem arrow of the same effect** (owner: come back to it): now the higher level wins (a Poison II tipped arrow beats a longer, near-perfect Poison gem arrow); alternative is comparing total effect (level x time).
- **Turtle Master gem's Slowness stacking** (owner: undecided, playtest): its Slowness part adds to other Slowness (a drunk Turtle Master potion, Slowness drawbacks) instead of following "stronger applies" like its Resistance part.
- **Perfect Invisibility set detection range** (owner: fine for now, revisit): real invisibility and the gems' stealth multiply, so mobs notice the wearer at about 11% of normal range (armor still shows).
- **Frost gem** (powder snow/strays): weapon hits slowly freeze the target; worn, walk on powder snow without freezing. Owner (2026-11-15): the next Minecraft drop adds a real freezing effect, so build Frost on that effect (and its potion, if it gets one) once we move to that version, instead of a custom one. (The other "gems beyond potions", Wind Burst, Weaving, Oozing, Silence, Ender and Breeze, are built: `specs/effect-gems.md`.)
- **Balance review, open design calls** (2026-11-15, see the review notes in specs/effect-gems.md): (1) PvP lock-down: cheap item gems at tier 3 put real Blindness, Levitation, Nausea or Darkness on every hit; ideas: per-target cooldown or diminishing returns, short Levitation. (2) Gem bows tip every arrow with the full melee hit duration (the model's tipped_arrow_scale is unused), so they act like infinite long tipped arrows with Infinity; idea: scale arrow durations. Also from the review: inverting can craft found-only effects as mains (minor); worn Infested is permanent (minor).
- **Silence on a weapon** (owner, 2026-11-16): 'quiet kills' idea (target makes no sounds/vibrations, can't call reinforcements) and deafening players are parked; Silence hits unchanged for now. See docs/research/effect-gems-strength-proposal.md Q20/Q21.
- **Revisit Breeze on a weapon** (owner): built as 'the target's own projectiles veer off'; revisit after playtesting.
- **Hidden timers to review** (owner: no hidden cooldowns): Absorption's 30 s refill after damage and 30 s equip-fill throttle.
- **Gem inversion** (owner idea, 2026-11-14): some way to turn a gem into its opposite (e.g. Mining Fatigue into Haste), so odd harmful gems have another use. Not designed yet.

## Bubba's Reworked Beacons (planned, C5)
- Design in specs/reworked-beacons.md; open questions listed there. Not built yet.

## Repo
- Owner to read and ratify (or rewrite) the goal drafts A1-A4 (`python3 tools/dte.py unratified`).
- A future mod drastically reducing unenchanted mining speed.
