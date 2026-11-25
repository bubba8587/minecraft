<!-- dte:C1,B15,B16,B17 -->
# Reworked Mobs: the Wither Captain and Soulfire Blazes (spec)

**Status: built.** A Nether mini-boss: a rare wither skeleton captain that summons Soulfire Blazes, and a second, slower route to nether stars (star shards) for Infused Gems' perfect gems. Code in `captain/` (server) and `captain/client/` (src/client); every tunable number in `captain/CaptainTuning`.

## The Wither Captain

- **Entity:** `bubbas_reworked_mobs:wither_captain`, a `WitherSkeleton` subclass (B15 is about blocks). Health 80 (wither skeleton 20); attack damage base 5 (wither skeleton 4) plus the stone sword; inflicts Wither like a wither skeleton; scale attribute 1.1 (model and hitbox); knockback resistance 0.3; follow range 24; 50 xp.
- **Gear:** stone sword, and the Captain's Helm always worn (equipment drop chance 0: the loot table drops it).
- **Boss bar:** vanilla boss bar, blue, notched; players (not spectators) within 32 blocks, checked every 10 ticks.
- **Normal phase:** plain wither skeleton AI.
- **Summoning phase:**
  - starts on its first fight (it targets a player within 16 blocks, or is first hit by any living thing; it then turns persistent so it won't despawn mid-fight), and again after 4 hits taken in the normal phase;
  - summons 1-3 Soulfire Blazes (uniform): the first at 30 ticks, then every 50; the phase ends 60 ticks after the last. A whole phase lasts 2.5-9.5 s;
  - no summons while 4 of its blazes are alive;
  - takes 25 % damage;
  - stands still facing its target and doesn't attack (a priority-0 goal holding move, look and jump);
  - blazes appear 2-3.5 blocks out and 1.5 up, with a soul fire burst; sounds: evoker prepare-summon (low pitch) at the start, blaze shoot per blaze;
  - synced as one boolean entity data (`summoning`); the client blends it 0-1 over 10 ticks.
- **Summoning look** (client):
  - **pose** (`CaptainPose`, blended over the vanilla pose by the summoning amount): head leaned back with a slow bob; both arms spread out to the sides, waving slowly up and down; the sword arm twisted so the blade points up from the outstretched hand, clear of the head;
  - **aura** (`CaptainAuraLayer`): the vanilla Wither's energy swirl over an inflated copy of the skeleton, posed like it. Soul-tinted by default (`CaptainAuraLayer.TINT = SOUL_TINT`; `VANILLA_TINT` is the Wither's own grey, a one-line switch);
  - **particles** (`CaptainParticles`): soul fire flames spiralling up round it, the odd soul.
- **Drops** (`loot_table/entities/wither_captain.json`): the vanilla wither skeleton table (bones, coal, the skull's usual chance) + 1-2 Nether Star Shards + the helm at 25 %. No killed-by-player condition, no looting bonus.
- **Natural spawning:** a natural wither skeleton spawn standing in a fortress becomes a captain 1 in 50, if no captain is within 48 blocks (mixin on `NaturalSpawner.spawnCategoryForPosition`'s `getMobForSpawn`, using vanilla's `NaturalSpawner.isInNetherFortressBounds`). Spawners and commands as normal; no fixed spot, no respawn rules.

## Soulfire Blazes

- **Entity:** `bubbas_reworked_mobs:soulfire_blaze`, a `Blaze` subclass with blaze attributes and vanilla blaze AI. Texture: the vanilla blaze recoloured to the soul fire ramp. The renderer hides the orange burning overlay a charged blaze shows; soul fire flame particles drift off it instead.
- **Soulfire charges:** their own entity `bubbas_reworked_mobs:soulfire_charge`, a `SmallFireball` subclass: 7.5 damage (blaze fireball 5), 5 s burn, soul fire flame trail, drawn as a fire charge like a blaze fireball. The blaze's attack goal is vanilla's; a mixin swaps the fireball it creates. Where a charge lands it places fire through vanilla `BaseFireBlock.getState`, so soul fire on soul sand and soul soil and ordinary fire elsewhere (no new block, B15). No change to water or other fire rules.
- **Summoned blazes** remember their captain (looked for within 48 blocks) and burn out (a puff of soul fire, no drops) when it dies or is gone, checked every 20 ticks.
- **Natural spawns:** soul sand valley monster spawns, weight 2 (skeleton 20, ghast 50, enderman 1), groups of 1.
- **Drops:** the vanilla blaze table (`loot_table/entities/soulfire_blaze.json`).

## Captain's Helm

- **Item:** `captains_helm`, its own armour material with netherite's numbers (3 armour, 3 toughness, knockback resistance, netherite durability), repaired with netherite ingots, fire-resistant item, default rarity. Equipment asset `bubbas_reworked_mobs:captains_helm` (`assets/.../equipment/captains_helm.json`, humanoid layer only).
- **Look:** a netherite helmet palette-swapped to polished blackstone (icon and worn layer), plus two horns, each three box segments bent up twice (`CaptainsHelmHorns`).
- **Worn horns:** `CaptainsHelmHornsLayer`, a render layer added to every humanoid renderer (players, armour stands, zombies, skeletons, the captain), drawing the horns with the root then head transform, armour render type (glint when enchanted). Chosen over Fabric's `ArmorRenderer`, which replaces vanilla's helmet drawing (and trims) instead of adding to it.
- **Soul fire immunity** (Fabric `ServerLivingEntityEvents.ALLOW_DAMAGE`): vanilla's fire damage names no block (soul fire and fire both deal `in_fire`, both campfires `campfire`, the burning after `on_fire`). So for a wearer:
  - `in_fire` and `campfire` damage is cancelled when the blocks inside their box hold soul fire or a lit soul campfire and no fire, lit campfire or lava; that also puts their flames out;
  - a wearer last lit by soul fire has `on_fire` cancelled too, until they touch ordinary fire or lava;
  - Soulfire charges don't light a wearer at all (the hit still hurts);
  - lava damage always applies; ordinary fire still burns.

## Nether Star Shard

- Item `nether_star_shard`, uncommon, fire-resistant. Recipe `nether_star_from_shards`: four shards, shapeless, category misc; unlock generated by `tools/recipe-unlocks.py`.

## Items and art

- Spawn eggs `wither_captain_spawn_egg` and `soulfire_blaze_spawn_egg` use vanilla's wither skeleton and blaze egg icons. All four items are in the Reworked Mobs creative tab.
- Textures (16x and 32x, B16) come from `mods/bubbas_reworked_mobs/tools-local/make_captain_textures.py`: Soulfire Blaze, Nether Star Shard, helm icon, helm worn layer, horn texture. Rows in `docs/art-todo.md`.

## Prior art

`tools/prior-art.py "wither skeleton boss soul fire blaze"` found nothing; no library. Vanilla subsystems throughout: wither skeleton AI, blaze AI and fireball, `BaseFireBlock.getState`, vanilla boss bar, `NaturalSpawner.isInNetherFortressBounds`.

## Tests

- **Core:** `captainSummonsWhenItFightsAPlayer` (body in `CaptainGameTest`).
- **Milestone `CaptainGameTest`:** the phase cuts damage to a quarter, summons 1-3, ends, and comes back after 4 hits; melee within 1.5x a wither skeleton's; 400 loot rolls (1-2 shards each, helm 15-35 %, bones); four shards craft a star; a Soulfire Blaze shoots charges, never vanilla fireballs; a charge hits harder than a fireball; charges place soul fire on soul sand and fire on netherrack; a helm-wearing mob takes nothing in soul fire and is hurt in fire; summoned blazes burn out on the captain's death; no captain outside a fortress.
- **Client part `captain`** (`CaptainClientGameTest`): captain-summon-front/side, soulfire-blaze, helm-front/side/close (armour stand), captain-normal, helm-icon (hotbar).

## Milestone test objectives (no test yet)

- Natural spawning in a real fortress: some wither skeleton spawns become captains, never two within 48 blocks.
- Soulfire Blazes appear naturally in soul sand valleys.
- A player (not a mob) wearing the helm takes no damage in soul fire and is hurt in fire. The current test uses pigs penned in glass, because test players don't take block effects server-side.
- Client: the summoning pose, aura and particles, a Soulfire Blaze, and the helm worn and as an icon, at 32x too.

## Open

- Spawn eggs reuse vanilla icons (row in `docs/art-todo.md`; own icons if the owner wants them).
- Tests missing for fortress captain spawning, the 48-block spacing and soul sand valley blazes (above).
- The helm immunity test uses pigs, not a player.
- A few javadocs in `captain/` are longer than labels (B38): trim.
