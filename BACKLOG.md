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
- **Per-weapon tuning for gems in bows, crossbows, tridents and maces** (owner, 2026-11-14): they have no material, so they count as diamond level for now; each probably needs its own factor.
- **Drawback sizes** (owner will playtest): currently 10% each, under 20% stacked.
- **Tipped arrow vs gem arrow of the same effect** (owner: come back to it): now the higher level wins (a Poison II tipped arrow beats a longer, near-perfect Poison gem arrow); alternative is comparing total effect (level x time).
- **Turtle Master gem's Slowness stacking** (owner: undecided, playtest): its Slowness part adds to other Slowness (a drunk Turtle Master potion, Slowness drawbacks) instead of following "stronger applies" like its Resistance part.
- **Perfect Invisibility set detection range** (owner: fine for now, revisit): real invisibility and the gems' stealth multiply, so mobs notice the wearer at about 11% of normal range (armor still shows).
- **Gems beyond potions** (owner liked these, 2026-11-15): gems do what the *thing* does, not just apply its potion. Replaces the current Wind Charged / Weaving / Oozing gems (worn, those are a useless negative except on mob armor).
  - **Wind Burst** (Wind Charged): weapon hits launch the target up and away like a wind charge; worn, landing from a fall gives a small mace-style burst that bounces you and cancels the fall damage. Owner idea: on a mace that also has the Wind Burst enchantment, Wind Burst gems stack past vanilla into an extra crazy >100% version (a deliberate exception to the 90% rule, earned by the pairing).
  - **Weaving**: worn, walk through cobwebs at full speed; weapon, a chance to web the target in place for a moment.
  - **Oozing**: worn, bounce like on a slime block (no fall damage); weapon, a kill splits off small **neutral slimes** that belong to the wearer (vanilla's allied check, stored on the slime rather than scoreboard teams, so they never target their owner) and dissolve after a while. Fighting for the owner would need new AI: owner chose neutral.
  - **Silence** (sculk/echo): worn, footsteps make no vibrations; ties into Reworked Mobs' hearing and the warden.
  - **Frost** (powder snow/strays): weapon hits slowly freeze the target; worn, walk on powder snow without freezing. Owner (2026-11-15): the next Minecraft drop adds a real freezing effect, so build Frost on that effect (and its potion, if it gets one) once we move to that version, instead of a custom one.
  - **Ender**: worn, a chance to blink away like chorus fruit when a projectile would hit you; strength scales the chance and distance (orchestrator's recommendation over an aimed teleport, which would be an active ability).
  - **Breeze**: worn, a chance to deflect incoming projectiles.
  - Dropped: Glow (owner: nothing Glowing doesn't already do). Avoid overlap with enchantments (Fire Aspect, Knockback) and the no-debuff-on-attackers rule.
- **Gem inversion** (owner idea, 2026-11-14): some way to turn a gem into its opposite (e.g. Mining Fatigue into Haste), so odd harmful gems have another use. Not designed yet.

## Repo
- Owner to read and ratify (or rewrite) the goal drafts A1-A4 (`python3 tools/dte.py unratified`).
- A future mod drastically reducing unenchanted mining speed.
- Test world screenshots: a client game test run with both mods loaded that opens a copy of `testworlds/bubba-test-world` and screenshots the hub and each station (the per-mod client test harness loads one mod at a time). `specs/testworld.md`.
