<!-- dte:C1,B15,B16,B17 -->
# Reworked Mobs: the Wither Captain and Soulfire Blazes (spec)

**Status: to build** (owner design, 2026-11-25). A Nether boss-tier mob: a rare wither skeleton captain that summons Soulfire Blazes, and a second, slower route to nether stars (star shards) for Infused Gems' perfect gems.

## The Wither Captain (owner)

- **What it is:** a wither skeleton captain. Its own entity (builder's choice: a `WitherSkeleton` subclass registered as a new entity type is fine; B15 is about blocks). Wither skeleton size or slightly larger (builder: up to 1.15x), more health (a mini-boss, builder picks; around 3-4x a wither skeleton), a vanilla boss bar while a player is near (builder's choice).
- **Damage:** not much higher than a wither skeleton's (owner: those are already tough). Inflicts Wither like them.
- **Gear:** a stone sword (as wither skeletons) and the **Captain's Helm** (below), always worn.
- **Normal phase:** plain wither skeleton AI.
- **Summoning phase** (owner):
  - starts when the captain first fights a player, and again after it has been hit a few times (builder: about 4 hits) in its normal phase;
  - **pose:** it raises its sword overhead, its head leans back, its arms are out to its sides and wave slowly, slightly up and down (so it isn't static);
  - **aura:** the vanilla Wither's "wind" aura (the swirling armour overlay the Wither shows under half health: reuse vanilla's own layer and texture), with **soul fire particles** layered on;
  - it stands still while summoning and summons **1-3 Soulfire Blazes** over time, then stays in the phase a little while after the last one (builder: a few seconds);
  - **takes much less damage** in this phase (builder: about a quarter);
  - then it returns to the normal phase.
- **Drops:**
  - **Nether Star Shards:** 1-2 per kill (builder may tune). Four shards craft one nether star (new item, below).
  - **Captain's Helm:** 1 in 4.
  - its normal wither skeleton drops (bones, coal, the skull's usual chance).
- **Spawning (owner: rare natural spawn):** a natural wither skeleton spawn in a Nether fortress is sometimes a captain instead (builder: about 1 in 50), never with another captain within about 48 blocks. Spawners and commands as normal. No fixed spot, no respawn rules.

## Soulfire Blazes (owner)

- **A blue blaze:** blaze behaviour and stats, recoloured to soul fire blue (texture, its rods' glow, its particles). Its own entity (builder's choice; a `Blaze` subclass).
- **Soulfire charges:** like a blaze's small fireballs, **a simple damage buff** (builder: about 1.5x a blaze fireball's). No change to water or other fire rules (owner). Where a charge hits the ground it places **soul fire**, as a blaze places fire; vanilla soul fire only survives on soul sand and soul soil, so elsewhere it places normal fire (vanilla `BaseFireBlock.getState`; no new block, B15).
- **Where they come from:** summoned by a Wither Captain (they burn out when it dies: builder's choice, recommended), and **a rare natural spawn** in soul sand valleys (builder: a low weight in the biome's monster spawns, or a small share of blaze-like spawns there).
- **Drops:** blaze rods as a blaze (builder may add a small chance of soul-themed extras; none required).

## Captain's Helm (owner)

- **Look:** a netherite helmet with its palette swapped to **polished blackstone**, plus **two horns**, one each side: each a simple rectangular prism, bent up, then bent up again (three short segments). Item icon and worn model.
  - Worn horns are 3D geometry outside the armour layer: use Fabric's armour rendering hook for a custom helmet model (builder checks the 26.3 API), not a texture trick.
  - 16x vanilla-style and 32x Classic Faithful style textures (B16); a row in `docs/art-todo.md` for each new texture and model.
- **Stats:** a netherite helmet's (armour, toughness, knockback resistance, durability, fire-proof item); repaired with netherite ingots, like netherite.
- **Perk (owner): soul fire immunity:** the wearer takes no damage from soul fire (soul fire blocks and soul campfires) and from Soulfire Blaze charges' fire. Ordinary fire, lava and the charge's impact damage still hurt.

## Nether Star Shard (owner)

- A new item. 1-2 drop from each Wither Captain. Four craft one nether star (shapeless or a 2x2, builder's choice), so stars still come mainly from the Wither.
- Texture 16x and 32x (B16), art-todo row.

## Prior art

`tools/prior-art.py "wither skeleton boss soul fire blaze"` found nothing. Builder: a quick search for "soul blaze" mods for look references only.

## Milestone test objectives

- A captain enters the summoning phase when it starts fighting, summons 1-3 Soulfire Blazes over time, takes about a quarter damage in the phase, returns to normal, and re-enters after about 4 hits.
- A captain's melee damage is within about 1.5x a wither skeleton's.
- Captain drops: 1-2 shards; the helm about 1 in 4 over many kills (loot table roll).
- Four shards craft a nether star.
- A Soulfire Blaze charge does more damage than a blaze fireball, places soul fire on soul sand and fire on netherrack.
- A player wearing the Captain's Helm takes no damage standing in soul fire, and still takes damage in normal fire.
- Natural spawning: in a fortress, some wither skeleton spawns become captains, never two within 48 blocks; Soulfire Blazes appear in soul sand valleys.
- Client: the summoning pose (sword up, head back, arms out and moving), the wind aura and soul particles; a Soulfire Blaze; the helm worn (front, side) and as an icon, at 16x and 32x.
