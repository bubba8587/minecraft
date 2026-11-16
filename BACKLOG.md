<!-- dte:A3 -->
# Backlog

Parked ideas and open items, newest first within each project. Agents add to it when the owner parks something; details live in the linked spec.

## Bubba's Reworked Mobs
- **Bury-break system:** how buried loot comes out (shovel-only vs breaking by hand, noise, tools). Today: shovel digs it out loudly; breaking by hand also works, slowly like packed dirt. `specs/undead.md`, Loot on display.
- **Survival players placing items on display** (decor). Owner: "will need care and consideration." Ops-only command for now. `specs/undead.md`, Loot on display.
- Calming sleepers (owner, 2026-11-15): soul fire and a "lull" game event tag that calm sleepers (light calming and loud bells are built: `specs/undead.md`, Calming sleepers).
- Mob spawner tweaks. `specs/undead.md`.
- Part 1 next pass: remaining undead variants, graves and crypts in worldgen (with Grave Talisman and Totem of Warding loot), hybrid rendering, real 16x/32x art.
- Later parts: creepers; **monster camps and the blood moon** (structures with fixed rosters and key blocks, a 7-day blood moon that respawns uncleared structures, red-sky effects; owner's earlier idea with integration notes in `docs/research/camps-blood-moon-idea.md`).

## Bubba's Totems & Artifacts (C6)
- Built (0.1.0, not released): Grave Talisman, Totem of Warding, totem statue. `specs/totems-artifacts.md`.
- **Grave Talisman as loot:** found only (owner). Place it in grave/crypt loot when Reworked Mobs' worldgen builds them; maybe a rare find on sleepers.
- Warding open question (builder): should mobs holding a Totem of Warding pulse, and may woken undead pick one up (built as yes). `specs/totems-artifacts.md`.
- Person-made statue design and mod icon (placeholders now, `docs/art-todo.md`).
- **Totems** (owner, 2026-11-16): artifact items for things potions and enchantments don't cover, like the Totem of Warding. Many can work both held (personal, small) and in a Reworked Beacons beacon (area). Liked:
  - **Keeping**: used up on death; you keep your inventory that once (a Totem of Undying for your items).
  - **Gathering**: pulls nearby dropped items and XP to you; in a beacon, into a container under it.
  - **Fortification**: explosions in the area don't break blocks (creeper/ghast-proof). NOT via the mobGriefing rule (that also stops villager farming, endermen etc.): only explosion block damage.
  - **Growth** (name TBD, a verb-like word): crops and saplings grow faster (random tick boost), held around you or in a beacon area.
  - **Seeking**: points toward the nearest structure of a chosen type, like an explorer map.
  - Dropped: Fold (animal breeding), Stillness (no despawning).

## Bubba's Crafting Tweaks
- Person-made mod icon (none yet).
- Real Glider art (16x and 32x, made by a person).

## Bubba's Infused Gems
- **Per-weapon tuning for gems in bows, crossbows, tridents and maces** (owner, 2026-11-14): they have no material, so they count as diamond level for now; each probably needs its own factor.
- **Drawback sizes** (owner will playtest): currently 10% each, under 20% stacked.
- **Tipped arrow vs gem arrow of the same effect** (owner: come back to it): now the higher level wins (a Poison II tipped arrow beats a longer, near-perfect Poison gem arrow); alternative is comparing total effect (level x time).
- **Turtle Master gem's Slowness stacking** (owner: undecided, playtest): its Slowness part adds to other Slowness (a drunk Turtle Master potion, Slowness drawbacks) instead of following "stronger applies" like its Resistance part.
- **Perfect Invisibility set detection range** (owner: fine for now, revisit): real invisibility and the gems' stealth multiply, so mobs notice the wearer at about 11% of normal range (armor still shows).
- **Frost gem** (powder snow/strays): weapon hits slowly freeze the target; worn, walk on powder snow without freezing. Owner (2026-11-15): the next Minecraft drop adds a real freezing effect, so build Frost on that effect (and its potion, if it gets one) once we move to that version, instead of a custom one. (The other "gems beyond potions", Wind Burst, Weaving, Oozing, Silence, Ender and Breeze, are built: `specs/effect-gems.md`.)
- **Balance review, open design calls** (2026-11-15, see the review notes in specs/effect-gems.md): (1) PvP lock-down: cheap item gems at tier 3 put real Blindness, Levitation, Nausea or Darkness on every hit; ideas: per-target cooldown or diminishing returns, short Levitation. (2) Gem bows tip every arrow with the full melee hit duration (the model's tipped_arrow_scale is unused), so they act like infinite long tipped arrows with Infinity; idea: scale arrow durations. Also from the review: inverting can craft found-only effects as mains (minor); worn Infested is permanent (minor).
- **Silence on a weapon** (owner, 2026-11-16): 'quiet kills' idea (target makes no sounds/vibrations, can't call reinforcements) and deafening players are parked; Silence hits unchanged for now. See docs/research/effect-gems-strength-proposal.md Q20/Q21.
- **Revisit Breeze on a weapon** (owner): built as 'the target's own projectiles veer off'; revisit after playtesting.
- **Hidden timers to review** (owner: no hidden cooldowns, B19): Absorption's 30 s refill after damage and 30 s equip-fill throttle (still in code).
- **Gem strength in beacons** (owner: approved for now): half a perfect armor set (`BEACON_SHARE` 0.5); revisit after playtesting.

## Bubba's Reworked Beacons (C5)
- Built (payload API, standalone payments, pyramid cap, upside-down pairs); not released. Builder's choices awaiting the owner's look are marked in `specs/reworked-beacons.md`.
- Player docs: a gem-in-beacon screen screenshot (client test) once the beacon screen is final.

- **Compatibility tests before releases** (prior-art research, docs/research/prior-art-all-projects-2026-11-16.md): Reworked Beacons vs Beyond Basic Beacons / Potion Beacons (likely mark incompatible) and Underground Beacons / Extended Beacon Range (test); Warding pathing vs Lithium; Infused Gems' enchanting hooks vs Easy Magic; Glider turning vs Do a Barrel Roll; Totem of Keeping vs grave mods (dupes/loss).
- **Name clash: 'Crafting Tweaks'** is also a 10.7M-download mod and a 26.3 datapack; consider renaming Bubba's Crafting Tweaks before its first release (owner's call).

## Repo
- Test world stations for Warding and Reworked Beacons (a Totem of Warding, a bell near sleepers, lit vs dark sleepers, sky vs paired beacons), when the owner wants to playtest them (`specs/testworld.md`).
- Test world: 7 signs in `testworlds/builder/` (`grounds_wardrobe`, `grounds_pool`, `grounds_lava`) still read "Inf. V 88% +"; rename them (e.g. "GP V 88% +", 15 characters max) and rebuild the world.
- Owner to read and ratify (or rewrite) the goal drafts A1-A4 (`python3 tools/dte.py unratified`).
- A future mod drastically reducing unenchanted mining speed.
