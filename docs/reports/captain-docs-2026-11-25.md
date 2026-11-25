<!-- dte:C1,B15,B16,B17 -->
# Wither Captain build report (2026-11-25, implementation agent)

For the docs merge into `specs/wither-captain.md` and the Reworked Mobs player page. Art (pose, aura, particles,
horns, textures) is the art agent's and arrives separately; everything here was built against placeholders.

## Player-facing text (for the player vault)

**Wither Captain.** Deep in Nether fortresses, about one wither skeleton in fifty is a Wither Captain: a little
larger, four times as tough (80 health), wearing a horned blackstone helm. It hits only a little harder than a wither
skeleton and still withers you. A blue boss bar shows while you're within 32 blocks. When it first fights you, and
again after you've landed about four hits, it stops, raises its sword and calls up one to three Soulfire Blazes over a
few seconds; while it's doing that it takes only a quarter of the damage, so it's the moment to deal with the blazes.
Kill it for one or two Nether Star Shards (four craft a nether star), a one-in-four chance at its Captain's Helm, and a
wither skeleton's usual drops (bones, coal, sometimes its skull). Its summoned blazes burn out when it dies. Never two
natural captains within about 48 blocks of each other.

**Soulfire Blaze.** A blue blaze. It fights like a blaze, but its charges hit half again as hard (7.5 instead of 5)
and leave soul fire where they land on soul sand or soul soil (ordinary fire elsewhere). Rarely found in soul sand
valleys; drops blaze rods like a blaze.

**Captain's Helm.** A netherite helmet's protection (3 armour, 3 toughness, knockback resistance, netherite
durability, survives lava), repaired with netherite ingots. Whoever wears it takes no damage from soul fire or soul
campfires and isn't set alight by Soulfire Blaze charges (the charge's hit still hurts). Ordinary fire and lava still
burn.

**Nether Star Shard.** Four shards (anywhere in the crafting grid) make one nether star.

## Builder's choices and numbers (all in `captain/CaptainTuning`)

- Captain: `WitherSkeleton` subclass `bubbas_reworked_mobs:wither_captain`; health 80; attack damage base 5 (wither
  skeleton 4) plus the stone sword; scale attribute 1.1 (model and hitbox); knockback resistance 0.3; follow range 24;
  xp 50. Always wears the helm (drop chance 0: the loot table drops it), stone sword as a wither skeleton.
- Boss bar: blue, notched, players within 32 blocks (checked every 10 ticks), not spectators.
- Summoning phase: starts when it first targets a player within 16 blocks, or is first hit by anything living (the
  "first fight"; it then turns persistent so it won't despawn mid-fight); again after 4 hits in the normal phase.
  1-3 blazes (uniform); first at 30 ticks, then every 50; 60 ticks (3 s) after the last it ends. A whole phase is
  2.5-9.5 s. Takes 25 % damage. No more summons while 4 of its blazes are alive. Blazes appear 2-3.5 blocks out,
  1.5 up, with a soul fire burst; sounds: evoker prepare-summon (low pitch) at the start, blaze shoot per blaze.
  It stands still facing its target (a priority-0 goal holding move/look/jump) and doesn't attack.
- Synced state: one boolean entity data (`summoning`); the client blends it 0-1 over 10 ticks for the pose.
- Soulfire Blaze: `Blaze` subclass `bubbas_reworked_mobs:soulfire_blaze`, blaze attributes. Charges are their own
  entity `bubbas_reworked_mobs:soulfire_charge` (a `SmallFireball` subclass, 7.5 damage, 5 s burn, soul fire flame
  trail, drawn as a fire charge like a blaze fireball); the blaze's attack goal is vanilla's, a mixin swaps the
  fireball it creates. Summoned blazes remember their captain and burn out (puff of soul fire, no drops) when it dies
  or is gone (checked each second). The renderer hides the orange burning overlay a charged blaze shows; soul fire
  flame particles drift off it instead.
- Natural spawns: soul sand valley monster weight 2 (skeleton 20, ghast 50, enderman 1), groups of 1. Captains: a
  natural wither skeleton spawn standing in a fortress becomes a captain 1 in 50, if no captain is within 48 blocks
  (mixin on `NaturalSpawner.spawnCategoryForPosition`'s `getMobForSpawn`).
- Helm: own armour material with netherite's numbers and the asset `bubbas_reworked_mobs:captains_helm`
  (`assets/.../equipment/captains_helm.json`, humanoid layer only), fire-resistant item, rarity default. Worn horns:
  a render layer on every humanoid renderer (players, armour stands, zombies, skeletons, the captain) drawing
  `CaptainsHelmHorns` with the root then head transform, armour render type (glint when enchanted). Chosen over
  Fabric's `ArmorRenderer`, which would replace vanilla's helmet drawing (and trims) instead of adding to it.
- Soul fire immunity, how: vanilla's fire damage names no block (soul fire and fire both deal `in_fire`, both
  campfires `campfire`, the burning after `on_fire`). So for a wearer, `in_fire`/`campfire` damage is cancelled when
  the blocks inside their box hold soul fire or a lit soul campfire and no fire, lit campfire or lava; that also puts
  their flames out. A wearer last lit by soul fire has `on_fire` cancelled too until they touch ordinary fire or lava.
  Charges don't light a wearer at all. Lava damage always applies. (Fabric `ServerLivingEntityEvents.ALLOW_DAMAGE`.)
- Shard: rarity uncommon, fire-resistant. Recipe `nether_star_from_shards` shapeless, category misc; recipe unlock
  generated by `tools/recipe-unlocks.py`.
- Loot: `loot_table/entities/wither_captain.json` = vanilla wither skeleton table + shards 1-2 + helm 25 % (no
  killed-by-player condition, no looting bonus); `soulfire_blaze.json` = vanilla blaze table.
- Spawn eggs `wither_captain_spawn_egg`, `soulfire_blaze_spawn_egg` use vanilla's wither skeleton and blaze egg
  icons for now (an art-todo row if the owner wants their own). All four items are in the Reworked Mobs creative tab.
- Prior art (B9): no library; vanilla subsystems used throughout (wither skeleton AI, blaze AI and fireball,
  `BaseFireBlock.getState`, vanilla boss bar, `NaturalSpawner.isInNetherFortressBounds`).

## Tests

- Core: `captainSummonsWhenItFightsAPlayer` (body in `CaptainGameTest`).
- Milestone `CaptainGameTest`: phase cuts damage to a quarter, summons 1-3, ends, and comes back after 4 hits; melee
  within 1.5x; 400 loot rolls (1-2 shards each, helm 15-35 %, bones); four shards craft a star; a Soulfire Blaze
  shoots charges, never vanilla fireballs; a charge hits harder than a fireball; charges place soul fire on soul sand
  and fire on netherrack; a helm-wearing mob takes nothing in soul fire and is hurt in fire (pigs penned in glass:
  test players don't take block effects server-side); summoned blazes burn out on its death; no captain outside a
  fortress.
- Not tested (objective stays written): natural fortress replacement and the 48-block spacing in a real fortress;
  Soulfire Blazes appearing in soul sand valleys.
- Client part `captain` (`CaptainClientGameTest`): captain-summon-front/side, soulfire-blaze, helm-front/side/close
  (armour stand), captain-normal, helm-icon (hotbar). 32x shots wait for the art agent's 32x pack.
