<!-- dte:C6,B15,B17,B18,B19 -->
<!-- covers: mods/bubbas_totems_artifacts/* -->
# Bubba's Totems & Artifacts (spec)

A separate mod for **artifacts**: found items that do things potions and enchantments don't. Grave Talisman and all totems live here (owner).

- Mod id `bubbas_totems_artifacts`, code in `mods/bubbas_totems_artifacts/`, Java package `io.github.bubba8587.bubbas_totems_artifacts`.
- Version 0.1.0, built, unreleased.
- Player pages: `player-docs/Projects/Bubba's Totems & Artifacts/` (one page per artifact, plus Totem Statue, Relic Maps, Relic Vaults).

## What lives here

| Artifact | Section |
|---|---|
| Grave Talisman | Grave Talisman |
| Totem of Warding | Totem of Warding |
| Totems of Keeping, Gathering, Fortification, Growth, Seeking | one section each |
| Totems of Herding, Clear Skies, the North Wind | one section each |
| Totems of the Beginning and the End | one section each |
| Totem statue for every totem, each giving a small area effect | Totem statue |
| Relic map shreds, relic maps, relic vaults (named, seeded) and crypt vaults | Relic maps and relic vaults |

What stays in Reworked Mobs (`specs/undead.md`): sleepers, the hearing/disturbance meter, light calming sleepers, bells being loud to sleepers, loot displays, combat, the monster cap, and the `api.Sleepers` hook (see Independence).

Previews (client test screenshots, still current): `docs/previews/totems/`
- `textures-16x-32x.png`, `hotbar-16x-and-32x.png`: the Warding, Keeping, Gathering, Fortification, Growth and Seeking icons at 16x and 32x.
- `gathering-pulling.png`: a held Gathering totem pulling items.
- `seeking-ahead-turned-turned32x.png`: the Seeking needle facing the found village, turned 90 degrees, and at 32x.

## Design rules for totems (owner)

- Many totems work both **held** (personal, small) and **in a beacon** (area). Most also work as a **statue** (a small area).
- Any rhythm (e.g. a held pulse) shows with vanilla's item cooldown sweep (B19 noHiddenCooldowns); vanilla mechanics first (B17); mobs and players treated the same (B18).
- **No overlap with Infused Gems:** a totem never does what a gem effect already does (e.g. no Totem of Silence: the Silence gem exists). Check `specs/effect-gems.md` before proposing a totem.
- Found, never crafted.
- Fortification stops explosion block damage only, never through the mobGriefing rule (that would also stop villager farming).

## Shared mechanics

- **Items:** every totem is `stacksTo(1)`, found only (no recipes), in the mod's creative tab "Bubba's Totems & Artifacts". Rarity `UNCOMMON`, except the Beginning and the End (`RARE`). No new blocks except the statue (B15).
- **Held** = main or off hand of any living thing, players and mobs alike (B18; `totems/Held`). Keeping and Seeking also work anywhere in the inventory.
- **Numbers** live in `totems/TotemsTuning` (Warding: `warding/WardingTuning`; relics: `relic/RelicTuning`). All are starting values for the owner to tune.
- **Areas:** beacon and statue areas live in `totems/TotemAreas` (one `Kind` per totem; each area lapses unless refreshed, like `Wards`). `TotemAreas` doesn't depend on Reworked Beacons.
- **Beacon payloads** (Reworked Beacons optional): one class per totem under the entrypoint `bubbas_reworked_beacons:payload` in `fabric.mod.json`: `compat/WardingBeaconPayload`, `GatheringBeaconPayload`, `FortificationBeaconPayload`, `GrowthBeaconPayload`, `SeekingBeaconPayload`, `BeginningBeaconPayload`, `HerdingBeaconPayload`, `ClearSkiesBeaconPayload`, `NorthWindBeaconPayload`. Each accepts only its own totem; power 1, 1 layer needed. Each pulse refreshes the area for `AREA_LIFETIME_TICKS` (100), so it lapses about 5 s after the beacon stops. Only these classes touch the beacons API. Keeping and the End have no payload.
- **Sneak-use on a block always places the totem's statue** (see Totem statue).

### How totems are found (`totems/TotemsLoot`)

One extra pool per table and totem, added on top of the normal loot, through `LootTableEvents.MODIFY`. Tables a datapack replaced through Fabric are skipped. Chests only (plus Warding's evoker drop): archaeology tables give a single item, so a second pool would break brushing. Every totem's tables, Warding's included, are in `TotemsLoot.all()`. Reworked Mobs tables are keyed by id, so there is no dependency.

| Totem | Chance per chest |
|---|---|
| Warding | woodland mansion 20 %, ancient city 12 %, trial chambers vault 8 %, ominous vault 15 %, Reworked Mobs noble crypt 12 %, evoker 5 % (a rare bonus, owner) *(chest rates: agent's choice)* |
| Keeping | ancient city 6 %, end city treasure 8 %, stronghold library 10 %, woodland mansion 8 % |
| Gathering | abandoned mineshaft 6 %, simple dungeon 8 %, pillager outpost 12 % |
| Fortification | desert pyramid 15 % (the TNT trap), bastion "other" 8 %, stronghold crossing 8 % |
| Growth | jungle temple 20 %, shipwreck supply 6 %, igloo 15 % |
| Seeking | shipwreck map 12 %, buried treasure 10 %, big underwater ruin 8 %, village cartographer 10 % |
| Herding | village shepherd 12 %, butcher 8 %, tannery 8 % |
| Clear Skies | igloo 12 %, desert village house 6 %, shipwreck supply 5 % |
| North Wind | trial chamber vaults only: 3 % normal, 6 % ominous *(builder's choice)* |
| The End | end city treasure 5 % |
| The Beginning | no chest table: a once-ever cairn offering (see Totem of the Beginning) |
| Grave Talisman | Reworked Mobs family vault 15 %, noble crypt 25 % (`TALISMAN_LOOT`) |

Relic vault chests also hold totems (see Relic maps and relic vaults). Loot placement and chances are the agent's proposal, for the owner to review.

## Grave Talisman

Owner's idea, deliberately loose: "a talisman that faintly glows near undead." Item `bubbas_totems_artifacts:grave_talisman`; working name, the owner may rename it.

- **Getting one (owner): found only, no recipe.** See the loot table above; also relic vault chests (weight 2) and the creative tab. A rare find on sleepers is an unbuilt idea.
- **What it does:** glows when undead are near, brighter the closer; nothing else. Undead = Reworked Mobs' sleeper blocks (through the hook) and any entity in `#minecraft:undead`.
- **Levels (`talisman/GraveTalisman`):** nearest undead within 40 blocks of the holder's eyes: faint; within 24: bright; within 12: brightest; none: off. The owner found shorter ranges too short; at sprint speed (about 5.6 blocks/s) faint gives about 7 s of warning.
- **Where it shows:** in either hand and as the icon in hotbar and inventory, for the local player's own talismans only. Held by others, dropped, in item frames: dark.
- **How (client only, no packets):** `client/talisman/GraveTalismanGlow` runs every 5 ticks while the player carries a talisman: sleepers via the hook (loaded chunks only) plus one entity box query for `#minecraft:undead`, keeping the nearest. Nothing runs without a talisman. The search is on plain `Level`, so a server game test can check it.
- **Visual:** `items/grave_talisman.json` is a `range_dispatch` on the numeric item model property `bubbas_totems_artifacts:grave_talisman_glow` (registered into `RangeSelectItemModelProperties.ID_MAPPER` via Fabric's transitive access wideners).
  - Levels 1-3 are a `composite` of the flat item and a gem cuboid (`models/item/grave_talisman_glow_<n>.json`, `light_emission` 5 / 10 / 15), because a flat `item/generated` layer can't carry `light_emission`.
  - All share `item/grave_talisman_held`, which raises the first-person pose so the gem is on screen. No particle or sound.
- **Art:** placeholder textures from `tools-local/make_talisman_textures.py` (16x and 32x); to be replaced by hand-drawn art.
- **Prior art:** nothing on Modrinth senses undead with an item. Closest: Slime Amulet (MIT, glows in slime chunks), Hostile Awareness, Player Radar. The idea is Sting from The Hobbit.

## Totem of Warding

Owner's goal: mob-free zones without spamming torches. Owner decisions: `docs/research/area-gems-and-warding.md` ("Owner decisions"); the beacon side: `specs/reworked-beacons.md`. Numbers in `warding/WardingTuning`. *(agent's choice)* marks the builder's picks. Follows B17 (vanilla flee behaviour, the cooldown sweep, the spawner light rule), B18 (mobs holding a totem pulse too) and B19 (the pulse rhythm shows as the cooldown sweep).

**The item** (`bubbas_totems_artifacts:totem_of_warding`, `warding/TotemOfWardingItem`): an artifact like the Totem of Undying (uncommon, stacks to 1), found only (owner: a mix of places, more common than the Totem of Undying), never a gem. Placeholder art from `tools/warding-textures.py` (16x and 32x).

### Held

- **Rhythm (owner):** in either hand (`Item.inventoryTick`, MAINHAND or OFFHAND) it pulses every 10 s (`PULSE_INTERVAL_TICKS` 200), the first as soon as it is held off cooldown. Players get vanilla's cooldown sweep (`ItemCooldowns.addCooldown`); other holders use a weak map of next pulse times. Held, it doesn't stop spawns (owner).
- **The cooldown doesn't block setting a statue down:** `StatuePlaceCooldownMixin` (server `ServerPlayerGameMode.useItemOn`) and `ClientStatuePlaceCooldownMixin` (client `MultiPlayerGameMode.performUseItemOn`) skip the vanilla cooldown check for any `StatueTotemItem` used on a block; right-click in the air still respects it.
- **Visuals:** soul-fire ring at shin height (below a player's eyes), rising souls, low amethyst chime *(agent's choice)*.
- **Pulse:** every `Enemy` within 8 blocks (`PULSE_RADIUS`) flees the holder for 100 ticks (`FLEE_TICKS`), keeping away while within 12 (`FLEE_KEEP_AWAY`), speeds 1.0 / 1.2 within 7, like a creeper from a cat *(keep-away: agent's choice)*.
  - Goal-driven `PathfinderMob`s get `WardFleeGoal` (priority 0, MOVE flag, added on first scare). Vanilla's `AvoidEntityGoal` can't be reused: it only flees entities a monster could target, which leaves out creative players and monsters holding a totem; its picks are copied.
  - The goal tries `FLEE_TRIES` (6) vanilla `getPosAway` positions and runs to the one farthest from the holder, so it runs straight away instead of sidestepping.
  - Brain mobs with `AVOID_TARGET` (piglins, hoglins) get that memory for 100 ticks.
  - Not affected (limitation): slimes, ghasts, phantoms, vexes, the warden, breezes.
  - Sleepers within 8 blocks lose 40 points (`PULSE_CALM_POINTS`, via `Sleepers.calm`) *(agent's choice)*.
- **Mob holders:** a mob holding a totem pulses on the same rhythm (B18), and a woken undead can pick one up *(agent's choice; open question)*.

### Wards (a totem in a Reworked Beacons beacon; owner)

`compat/WardingBeaconPayload` accepts the totem, power 1, 1 layer. `apply` = `Wards.put(level, pos, area, levels >= 4, 100)`. Screen lines: "No monsters spawn or enter", and "Sleeping undead stay asleep" (4 layers) or, greyed, "4 layers: sleeping undead stay asleep". The sleeper line shows only when Reworked Mobs is installed *(agent's choice: without it the line describes nothing)*. `-PwithBeacons` puts the beacons jar on the game test classpath.

- **Any pyramid: no natural monster spawns** inside (`WardSpawnStateMixin`; `PhantomSpawnerMixin` wraps `isValidEmptySpawnBlock`, so phantoms count *(agent's choice)*). Spawners, trial spawners, raids, bosses and commands spawn as normal (owner).
- **Any pyramid: monsters don't walk in.** `WalkNodeEvaluatorMixin` / `SwimNodeEvaluatorMixin` (HEAD of `getPathTypeOfMob`) return BLOCKED for a kept-out mob's node inside a ward it isn't in. Every 20 ticks (`WARD_SWEEP_TICKS`) goal-driven monsters inside walk out through the nearest side; no despawn or damage *(agent's choice)*. Ranged monsters outside can still shoot in (limitation).
- **Any pyramid: an invisible wall for every kept-out mob** (owner: every monster, including ones that don't path, and happy ghasts, wild or ridden).
  - "Kept out" = `Enemy` or a happy ghast (`WardBarrier.keptOut`). The ward sweep skips what `keptOut` skips (deactivated Fabricator mobs).
  - `WardBarrier` runs at the TAIL of each such mob's `LivingEntity.tick` (`MobTickMixin`, server) within 3 blocks of a ward. `Wards.blocksEntry` checks "any wards in this level?" first, so it costs one map check per mob tick when no ward exists. No damage; mobs slide along the edge.
  - At the edge moving in: inward motion dropped. Crossed in since last tick: put back on the edge (the barrier keeps its own "last seen outside" per mob in a weak map, because the mob's old-position fields weren't reliable).
  - Already inside: pushed 0.06 blocks/tick outward through the nearest side (top or bottom for no-gravity mobs); walkers also walk out. The nearest-face exit is computed once (`WardBarrier.exitNormal`).
  - Steered vehicle (a ridden happy ghast): moved out by min(depth + 0.6, 1) blocks a tick, with `ClientboundMoveVehiclePacket` to the rider; a small rubber-band on lag (limitation). Passengers are skipped.
- **4 layers: sleepers inside stay asleep** (`Sleepers.keepAsleep` for the ward's area).
- **Lifetime:** `Wards.put(level, key, AABB, keepsSleepersAsleep, lifetimeTicks)` lapses unless refreshed. Each 80-tick beacon pulse refreshes it for 100 ticks (`WARD_DEFAULT_LIFETIME_TICKS`), so a ward ends about 5 s after its beacon stops. One map lookup when no ward exists.
- **Trapped inside a ward** (owner reported a skeleton in an enclosed ward "stuck running/jittering"): a monster inside plans its walk out (`WardFleeGoal`, up to 6 `getPosTowards` tries). When none gives a walk that ends outside (an enclosed room), it counts as trapped for 10 s (`TRAPPED_RECHECK_TICKS` 200): it stops trying to leave, the outward push skips it, and it behaves normally (fights), then looks for a way out again. *Builder's choice the owner may change: trapped monsters fight as normal rather than cower.*

## Totem of Keeping (`keeping/Keeping`, `mixin/PlayerKeepingMixin`)

A Totem of Undying for the player's items. Held only in the sense of carried; no beacon use, no statue effect.

- **Where it counts (agent's choice): anywhere in the player's inventory** (hotbar, backpack, off hand), not only in a hand. It's insurance and deaths come unannounced; "must be in hand" would make it a juggling act.
- **On death with `keep_inventory` off:** one Keeping totem is used up and the death goes exactly as under vanilla's keep_inventory (B17): whole inventory kept, **XP levels kept** (agent's choice, because keep_inventory does that), score kept, nothing drops, no XP orbs, curse-of-vanishing items stay. With `keep_inventory` on, the totem isn't used. Mobs: not applicable (mobs don't respawn).
- **How:**
  - At the start of the death drops (`Player.dropEquipment`, HEAD) the totem is taken and the dead player gets the entity tag `bubbas_totems_artifacts.kept` (saved, so logging out on the death screen still works).
  - The tag stops the drop and the XP orbs (`getBaseExperienceReward`).
  - On respawn, Fabric's `ServerPlayerEvents.COPY_FROM` copies inventory, XP and score the way vanilla's `transferInventoryXpAndScore` does; `AFTER_RESPAWN` clears the tag (vanilla copies entity tags to the new player).
- **Effect:** at the place of death everyone sees the Totem of Undying's particle burst and hears its sound. On respawn the player gets the Totem of Undying animation with the Totem of Keeping filling the screen (custom S2C payload `bubbas_totems_artifacts:kept` → `LocalPlayer.displayItemActivation`), the totem sound, and the action-bar line "Your Totem of Keeping held on to your things".
- **Prior art:** keep-inventory mods (Keep Inventory Always, Nice Keep Inventory, Keep Inventory Orbs; MIT/GPL), soulbound enchantments, and grave mods (Universal Graves, Gravestones, You're in Grave Danger). With a grave mod installed, a kept death simply has nothing to put in the grave (they compose). None offers a single-use found totem. Hand-rolled (two small injections; no library does it, B9).

## Totem of Gathering (`gathering/Gathering`, `TotemOfGatheringItem`, `compat/GatheringBeaconPayload`)

### Held

- Every tick, dropped items within **8 blocks** (`GATHER_RADIUS`) fly to the holder at **0.3 blocks/tick** (`GATHER_SPEED`; about 1.5 s from 8 blocks), aimed at the holder's middle like XP orbs, then are picked up by the usual rules. At most 64 items per tick (`GATHER_MAX_ITEMS_PER_TICK`).
- Skipped: items the holder itself threw that are still on their pickup delay (so Q-dropping works), and items that can never be picked up (pickup delay 32767: displays and props). Someone else's fresh throw is pulled.
- Why a steady pull (B17 note): vanilla's own XP-orb pull formula was tried first, but on the ground item friction eats it (items 6 blocks out moved less than a block in 5 s).
- **XP:** vanilla XP orbs already fly to any player within 8 blocks, so the held totem leaves orbs to vanilla (no double pull).

### In a beacon: a floating ring (`gathering/GatheringRing`)

- Dropped items and XP in the beacon's area fly to the beacon and **circle it, weightless**, 1.25 blocks out from its middle (`RING_RADIUS`) and 1.05 above its base (`RING_HEIGHT`), turning once every 5 s (`RING_PERIOD_TICKS` 100). Rings are keyed by beacon position; each member's velocity is set toward its slot every tick, and `needsSync` keeps clients smooth.
- **A hopper against any side of the beacon,** into a chest, collects the items as they come round: its pickup space (11/16 to 2 blocks above it) is where the ring passes. Vanilla's hopper does the collecting. No chest insertion anywhere for Gathering.
- Items in the ring never despawn. XP orbs stay until a player comes within 2.5 blocks (`RING_XP_RELEASE_DISTANCE`), then fly to them as usual.
- At most 48 members (`RING_MAX_MEMBERS`); new ones join every half second (`RING_COLLECT_TICKS` 10) at `RING_FLY_SPEED` 0.4. When the beacon stops, the ring lapses after about 5 s and everything falls.
- Beacon text: "Dropped items and experience circle the beacon".
- The ring's slot motion is hand-rolled: vanilla has nothing that orbits entities (`specs/vanilla-subsystems.md`).

### Statue ring

- Each Gathering statue pulse (owner) refreshes a `GatheringRing` round the statue: dropped items and XP within the statue radius (5) fly to it and circle it weightless; a hopper beside the statue collects them, as with the beacon ring. One ring per item (the existing claiming) still holds.
- Size: radius 1.0 (`STATUE_RING_RADIUS`), 1.1 above the statue's base (`STATUE_RING_HEIGHT`), so it passes over the blocks beside the statue (inside a side hopper's pickup area) and just clears a chest or hopper there. `GatheringRing.put` has an overload taking radius and height; each ring keeps its own size.
- A chest touching the statue gets nothing.

**Prior art:** item magnets (Simple Magnets ARR, magnetic GPL, Magnetic Enchantment AGPL, Sticky Fingers MIT, davdeo's Item Magnet MIT). None ties into a beacon; hand-rolled (tiny; no library does it).

## Totem of Fortification (`fortification/Fortification`, `mixin/ServerExplosionMixin`, `compat/FortificationBeaconPayload`)

- **Held:** explosions break **no block within 8 blocks** (`FORTIFY_RADIUS`) of anyone holding one.
- **In a beacon (any pyramid):** no explosion breaks blocks anywhere in the beacon's area.
- **As a statue:** no explosion breaks blocks within the statue radius.
- **Explosion block damage only:** the hook filters the explosion's own list of blocks to break, right where `ServerExplosion.explode` works it out. So the explosion still bangs, hurts and knocks back, blocks outside the protected zone still break, and explosion-lit fire (ghast fireballs, beds) doesn't start on protected blocks. The **mobGriefing rule is never touched** (owner: it would stop villager farming). Wind charges (which only press buttons and open doors) are left alone.
- **Cost:** one entity box query per explosion; nothing when no holder or area is near.
- **Feedback** when it saved blocks: a dull shield-block clang and a few wax-on glints at the blast.
- **Limitation:** item frames, paintings and armor stands are entities and still get hurt by the blast.
- Screen line: "Explosions break no blocks".
- **Prior art:** no Modrinth hit for explosion-protection items on 26.3; anti-creeper-grief mods change the mobGriefing rule or the explosion type, which the owner ruled out. Hand-rolled (one injection).

## Totem of Growth (working name; `growth/Growth`, `TotemOfGrowthItem`, `compat/GrowthBeaconPayload`)

The owner wanted a verb-like name; still open. The Harvest idea was folded into Growth's planting (owner).

### Faster growth

- **What grows faster:** block tag `bubbas_totems_artifacts:growth_totem_boosts` (datapack-editable): `#minecraft:bee_growables` (crops, sweet berries, glow berry vines), `#minecraft:saplings`, sugar cane, cactus, bamboo (and its shoot), cocoa, nether wart, kelp, twisting and weeping vines, chorus flower.
- **How (B17):** extra **random ticks**, vanilla's own growth step, so light, farmland, room for a tree and so on stay vanilla's. Extra ticks are a multiple of vanilla's rate and follow the `random_tick_speed` rule. Sampling is vanilla's (random positions per chunk section, skipping sections with nothing that ticks and chunks vanilla isn't ticking).
- **Held:** a box 8 blocks each way from the holder (17x17x17, `GROWTH_RADIUS`), **+2x vanilla's rate (grows about 3x as fast;** `GROWTH_HELD_EXTRA`). Cost: a few hundred block lookups a tick at most.
- **In a beacon:** the beacon's whole area, **+1x per pyramid layer** (`GROWTH_BEACON_EXTRA_PER_LAYER`; 1 layer: 2x as fast ... 4 layers: 5x), every tick while the area lives; at most 2048 samples per area per tick (`GROWTH_MAX_SAMPLES_PER_TICK`; a full 4-layer sky beacon needs about 2000).
- **As a statue:** +1x (`GROWTH_STATUE_EXTRA`; twice as fast) within the statue radius.
- A plant that grows from an extra tick puffs two bone-meal sparkles.
- Screen line: "Crops and saplings grow Nx as fast".

### Planting dropped seeds

- Held, in a beacon or as a statue: every 2 s (`PLANT_CHECK_TICKS` 40), with a 35 % chance (`PLANT_CHANCE`), one random dropped plantable item in range is planted within 3 blocks (`PLANT_REACH`) of where it lies. Plantable = any item whose block is in `growth_totem_boosts` (seeds, carrots, potatoes, saplings, berries, nether wart...). Items circling a Gathering ring count.
- `Growth.plant`: 32 tries of `BlockPos.randomBetweenClosed`, y -3..+1, air plus `canSurvive`.
- **It needs room (owner):** a seed is planted only where vanilla's survival rules pass against the real neighbours (`canSurvive`), the spot above is open, and no wall face, log or (for non-crops) other plant is beside it, so what Growth plants can grow. Crops may sit in rows.
- **Never on top of another plant** (owner: it stacked sugar cane on sugar cane, and would do the same to bamboo and cactus): the block below must not be a plant (`Growth.isPlant`: the `growth_totem_boosts` tag, `#minecraft:flowers`, or any vanilla `VegetationBlock`).
- Never sweeps, harvests or gathers.

**Prior art:** growth accelerators and twerk mods (Crop Growth Modifier, Twerk Crop Growth, Sprinklerz); nothing on 26.3 as an item or beacon; hand-rolled (no library).

## Totem of Seeking (`seeking/TotemOfSeekingItem`, `SeekingTypes`, `StructureSeeker`, `SeekingBeacon`, `mixin/CompassItemMixin`, `compat/SeekingBeaconPayload`)

### Choosing what to seek

Use (right-click) the totem on a structure's **signature block** (placed anywhere). Lodestone-lock sound and action bar "Seeking: Jungle Temple"; tooltip "Seeks: Jungle Temple", or "Use on a structure's block to choose what to seek" when nothing is chosen. Other blocks: nothing happens. The list is the builder's proposal; the owner may prefer another way (open question).

| Structure | Signature block(s) |
|---|---|
| Village (any) | hay bale, bell |
| Desert Pyramid | chiseled sandstone |
| Jungle Temple | mossy cobblestone |
| Swamp Hut | cauldron |
| Igloo | snow block |
| Pillager Outpost | dark oak log |
| Woodland Mansion | dark oak planks |
| Ocean Monument | prismarine bricks, dark prismarine, prismarine, sea lantern |
| Ruined Portal (any) | crying obsidian |
| Mineshaft (any) | rail |
| Abandoned Camp (any) | campfire |
| Trail Ruins | decorated pot |
| Stronghold | cracked stone bricks, chiseled stone bricks |
| Trial Chambers | tuff bricks, chiseled tuff bricks |
| Ancient City | reinforced deepslate, sculk shrieker |
| Nether Fortress | nether bricks, nether brick fence |
| Bastion Remnant | gilded blackstone, polished blackstone bricks |
| End City | purpur block, purpur pillar |

Not seekable (no good signature block): shipwrecks, ocean ruins, buried treasure (explorer maps cover those), nether fossils, relic vaults.

### Pointing (B17)

- Vanilla's lodestone compass: the totem carries an untracked `minecraft:lodestone_tracker` aimed at the structure; its item model is a `minecraft:compass` (target `lodestone`) dispatch over 16 needle frames.
- No target (nothing chosen, first search still running, nothing in this dimension, e.g. a fortress in the Overworld): the needle spins like a compass in the Nether.
- Works anywhere in the inventory, like a compass; checks every 20 ticks (`SEEK_UPDATE_TICKS`).
- Once the player reaches the structure, the totem keeps pointing at it, like an explorer map ("point to the next one" would need a "found" memory; open question, not built).

### The search, never freezing the server

- `StructureSeeker` is vanilla's /locate search (`ChunkGenerator.findNearestMapStructure`: concentric rings for strongholds, then the random-spread grid ring by ring to 100 rings (`SEEK_MAX_RINGS`), stopping at the first ring with a hit), cut into steps.
- **2 ms per tick budget** for all searches together (`SEEK_BUDGET_NANOS`).
- It **never loads or generates chunks:** a candidate counts when vanilla's quick structure check says a start is there or would be generated there. (Vanilla confirms that by generating the chunk's structure starts, so in rare cases a structure that fails to fit still counts.)
- Results are cached per dimension, type and 256-block cell of the asker's position (`SEEK_CELL_BLOCKS`; shared by everyone in the cell; 512 entries, `SEEK_CACHE_SIZE`, oldest dropped; cleared on server stop). While a new search runs, the needle keeps its old target.
- In the client test's world the result matched vanilla's /locate exactly (village at 240, 0, 128).

### In a beacon

- Every **vanilla compass held** (either hand, anyone, B18) in the beacon's area points to the nearest structure of the totem's type, searched from the beacon.
- The compass gets an untracked lodestone target plus `bubbas_totems_artifacts:seeking_beacon_until`; it shimmers like a lodestone compass but keeps the name "Compass". 100 ticks after it was last reached (`SEEK_COMPASS_LIFETIME_TICKS`) it turns plain again. Lodestone-bound compasses are left alone.
- Screen line: "Compasses point to the nearest Jungle Temple". A totem with no type does nothing; its screen line says "Choose a structure first: use the totem on its block".

**Prior art:** Explorer's Compass (CC-BY-NC-SA, no 26.3), OmniCompass (MIT, 26.1.2), Luna's Structure Compass (MIT, 1.21.8): structure compasses that pick from a GUI list and run a locate. Read for ideas only. Hand-rolled because none has a 26.3 build to depend on (B9); our block-choosing and no-chunk-load stepped search is different anyway.

<a id="totem-of-herding-herdingherding-built-2026-11-22"></a>
## Totem of Herding (`herding/Herding`)

- **Held:** livestock follow the holder as if they held their food. Every animal counts except tamed pets (builder's choice; bees and axolotls included). Hooks vanilla's temptation: `TemptGoalMixin.shouldFollow` (goal mobs) and `TemptingSensorMixin.playerHoldingTemptation` (brain mobs such as goats).
- **In a beacon or as a statue: a pasture without fences.** Livestock in the area won't walk out of it (`TotemAreas.Kind.HERDING`; `Herding.keepsIn` uses the wards' node-evaluator mixins: nodes outside are BLOCKED for livestock inside).
  - A lead still pulls them out, and so does a player holding the totem within 10 blocks (`HERD_LEAD_OUT_RANGE`; `getNearestPlayer` with `EntitySelector.NO_SPECTATORS`).
  - Beacon text: "Animals stay in the area".
- **Art:** a spruce ram-headed idol with a gold bell (item and statue are builder art).
- Doesn't overlap a gem effect.

<a id="totem-of-clear-skies-skiesclearskies-built-2026-11-22"></a>
## Totem of Clear Skies (`skies/ClearSkies`)

- **Used in rain or a storm,** it ends the weather like `/weather clear` (`MinecraftServer.setWeatherParameters(RAIN_DELAY.sample, 0, false, false)`). It isn't used up and has no cooldown (owner: weather doesn't come back that fast). In clear weather nothing happens (PASS).
- **In a beacon or as a statue:** no rain, snow or lightning on the area's columns, sky to ground. No snow builds up and cauldrons don't fill; ice still forms in cold biomes. Beacon text: "No rain, snow or lightning in the area".
  - Server: `TotemAreas.Kind.CLEAR_SKIES`, flattened each tick to column rectangles. Mixins on `Level.precipitationAt` (covers `isRainingAt`: lightning, wetness, rain putting out fire) and on `ServerLevel.tickPrecipitation`'s `isRaining` (snow layers, cauldrons).
  - Client: the rectangles are sent once a second and when the areas end (payload `bubbas_totems_artifacts:clear_skies`, with the dimension). `ClientLevelPrecipitationMixin` on `ClientLevel.getPrecipitationAt` stops rain drawing, splashes and sounds there. A copy older than 3 s, or from another dimension, is ignored.
- **Art:** calcite with a gold sun crown and a sky emblem (item and statue are builder art).
- Doesn't overlap a gem effect.

## Totem of the North Wind (`northwind/TotemOfNorthWindItem`, `northwind/NorthWind`, `compat/NorthWindBeaconPayload`)

Owner's design; *(builder's choice)* marks what the owner didn't pick.

- **Item:** `bubbas_totems_artifacts:totem_of_north_wind`, one per stack, uncommon, found only. Look: a pale packed-ice idol with an icicle crown, blowing a gust from puffed cheeks, a snowflake on the chest. Tooltip: "Use while gliding: a gust of wind".
- **Used while gliding** (anything that makes the player fall-fly: Elytra, or Bubba's Extra Recipes' Glider): a firework rocket boost, then a 5 s item cooldown sweep (owner set the cooldown). Not used up. Not gliding: PASS, nothing happens, no cooldown.
  - The gust is vanilla's rocket boost: an attached `FireworkRocketEntity(level, stack, player)` spawned with `Projectile.spawnProjectile`, as `FireworkRocketItem.use` does. The stack is a rocket with `Fireworks(NORTH_WIND_FLIGHT, no explosions)`, so it never explodes or hurts; the entity is silent.
  - On the Glider the push is the Glider's weaker rocket boost: Extra Recipes' `FireworkRocketGliderMixin` scales any attached rocket, so no compat code is needed here.
  - Sound and look *(builder's choice)*: the wind charge burst sound (quieter, higher pitch) instead of the rocket's bang, a puff of snowflakes and a small gust; the rocket's spark trail stays. A held leash breaks, as with a rocket.
- **In a beacon** (Reworked Beacons): every player gliding anywhere in the beacon's area (vanilla's beacon box, full height) gets the same gust, then a 5 s cooldown before the next, so a glider is kept aloft with a gust every 5 s. Beacon text: "Gliders in the area get a gust of wind".
- **As a statue:** the same, within the statue radius (5). *(Builder's choice, not yet confirmed by the owner: it follows the rule that a statue gives its totem's area effect in a small radius, making a statue a launch pad or boost ring. The owner may prefer a decorative-only statue.)*
- **Cooldown (B19 noHiddenCooldowns):** hand use, beacon and statue share one timer: the item cooldown group of the totem's id (`ItemCooldowns`). So a North Wind totem anywhere in the inventory shows the sweep, and there are no double gusts. Without a totem on the player, the gusts themselves are the rhythm (one fires the moment the cooldown ends), like the other beacon totems. Only players glide, so there is no mob path.
- **Area tick:** beacon pulses (`TotemAreas.put`, `AREA_LIFETIME_TICKS`) and statue pulses (`StatueEffects`) set a `TotemAreas.Kind.NORTH_WIND` area. `ServerTickEvents.END_LEVEL_TICK` walks `level.players()` only while the level has a live North Wind area: fall-flying, not spectator, off cooldown, inside a box → gust and `NORTH_WIND_AREA_COOLDOWN_TICKS`.
- **Found** *(builder's choice of numbers)*: trial chamber vaults only, 3 % per normal vault opening, 6 % per ominous one (`chests/trial_chambers/reward`, `reward_ominous`, through `TotemsLoot`). Vaults roll once per player per vault, so a full trial chamber gives roughly a 15 to 25 % chance. Not in the chambers' ordinary chests or in relic vault chests; a relic vault pedestal can hold its statue.
- **Numbers** (`TotemsTuning`): `NORTH_WIND_COOLDOWN_TICKS` 100, `NORTH_WIND_AREA_COOLDOWN_TICKS` 100 (5 s, owner: "nerf CD by like 2s"), `NORTH_WIND_FLIGHT` 1, `NORTH_WIND_LOOT` 0.03 / 0.06.
- **Tests:**
  - Core `BubbasTotemsArtifactsCoreGameTest.northWindGustsGlidersOnly` (body `NorthWindGameTest.usedWhileGlidingBoosts`): not gliding → PASS, no cooldown, no rocket; gliding → success, cooldown on, the totem still in hand, one rocket owned by the player, the player pushed along their look (> 0.5 blocks/tick).
  - Milestone `NorthWindGameTest.statueAreaGustsGliders` (a glider inside gets a gust and the cooldown; outside the radius, or not gliding, nothing) and `beaconAreaGustsGliders` (`-PwithBeacons`: the payload accepts only this totem; a 1-layer beacon's area reaches 15 out and 80 up; a glider in it gets a gust). Each runs in its own test environment (`north_wind_statue`, `north_wind_beacon`), so in its own batch: an area gusts every glider in the level, which would hit other tests' players.
  - Also covered by `totemsAreFoundInLoot` (vault odds), `totemsAreArtifacts` and the statue place/drop loop over every `TotemType`.
  - Client part `northwind` (`NorthWindClientGameTest`): held and in the hotbar, the cooldown sweep, the statue from the front and at 3/4 view, at 16x and 32x. Part `statues` lays out 11 statues.
- **Art:** item by `tools/totems-textures.py`, statue by `tools-local/make_statue_textures.py`; rows in `docs/art-todo.md`.

<a id="totems-of-the-beginning-and-the-end-built-2026-11-22"></a>
<a id="totem-of-the-beginning-rework-owner-2026-11-24-built-2026-11-24"></a>
## Totem of the Beginning (`beginning/TotemOfTheBeginningItem`, `beginning/BeginningAreas`, `compat/BeginningBeaconPayload`)

Owner's design (C6 detail, spec not leaf). Player page: `Totem of the Beginning.md`. The ground-reverting itself lives in Reworked Mobs: `specs/endermen.md`, "The pulse" and "Cairn gifts, the ripple and the nexus".

- **Item:** `bubbas_totems_artifacts:totem_of_the_beginning`, `stacksTo(1)`, `Rarity.RARE` (aqua name, a step above the other totems: it is a once-ever cairn gift), in the creative tab after Seeking. Never used up. Tooltip: "Use: the ground nearby slowly remembers how it began".
- **Use:** the ground within 8 blocks ripples back toward how it generated over about 3 seconds (7 ticks a block), then a 60-second item cooldown sweep.
  - The pulse is a cylinder, as far up and down as out, from the player's feet.
  - Without Reworked Mobs, or outside the overworld, it does nothing (PASS) and sets no cooldown, with no message. The client shows the use animation only in the overworld with Reworked Mobs installed.
  - Item use sound: `SCULK_CATALYST_BLOOM` at pitch 0.5.
- **Odds per pulse:** each eligible block goes back with a 12 % chance; a mined-out ore spot gets its ore back far more rarely (1.2 %), and only into empty space. A spot can be reverted again on the very next use: there is no hidden "recently reverted" window (B19 noHiddenCooldowns).
- **Anti-ore-farming** rests on the low ore chance (a tenth of terrain's), ore only ever into air, and the visible 60 s cooldown.
- **Never touched:** builds, unbreakable blocks, spawn-protected ground.
- **Effect:** each changed block gives a dark soul puff with purple portal motes and a low, soft sculk sound (at most about 3 sounds a second).
- **Why these numbers (builder):** at 12 % a fully dug patch is about 70 % back after 10 pulses (10 minutes), so healing a crater is a session's background chore, not instant, while one pulse visibly changes something. Ore at 1.2 % means about 80 pulses (80 minutes of standing and clicking) per ore spot on average; mining normally is far faster, so no ore farm. 60 s cooldown and radius 8 as the owner suggested.

### In a beacon or as a statue

- Both revert blocks directly with the same area pulse as the hand version: same chances, same puff and throttled sound, builds untouched. There is no player, so there is no spawn-protection check.
- **Rhythm** (`BeginningAreas`): once every 15 s on the world clock (`BEGINNING_AREA_PULSE_TICKS` 300; a source is due when `gameTime % 300` falls in its call interval, so each pulses exactly once per 15 s, all in step), spreading 2 ticks per block (`BEGINNING_AREA_TICKS_PER_BLOCK`).
- **Beacon:** the pulse covers the beacon's whole area (radius half the area's larger horizontal size, a cylinder as far up and down), centred on the beacon. Beacon text: "Every 15 s the ground here slowly remembers how it began". Accepted only when Reworked Mobs is installed *(builder's choice: without it the totem would do nothing, so the beacon refuses it)*.
- **Statue:** `StatueEffects.pulses` includes BEGINNING; radius `STATUE_RADIUS` (5).
- Beginning beacons don't draw caretaker endermen; cairns keep their own nexus (Reworked Mobs' `Endermen.drawIn` stays in the API, unused by Totems).

### What the ripple changes (builder's choices, owner may change)

1. **Build protection is per column, radius 2, the whole cylinder's height.** Safe rather than thorough: a torch at the bottom of a mine keeps the crater above it too, and a mine with rails or torches is only partly filled (pockets round each torch).
2. **Cobblestone, mossy cobblestone, cobbled deepslate, smooth stone and dirt paths count as built** (terrain generation never makes them on its own), so cobblestone houses are safe. Dirt and plain stone builds are *not* recognised: a pure dirt hut standing on the ground can be taken away like a dirt pillar.
3. **Geodes' insides are left alone** (their shell protects the column), and so are lava pools.
4. **Water and lava are never filled over or put back;** a crater that flooded stays a pond.
5. **A strip mine the player stands in fills round them but not where they stand,** so they can end up in a 1x2 pocket of stone. No block goes where a mob or player is.
6. **The top-layer swap only happens away from water** (like the endermen), so natural sand and gravel by rivers and lakes stay.
7. Each column is done in one go, filling bottom up, so sand and gravel always land on a floor.

### Getting one

Found only as a once-ever offering at some complete cairns (Reworked Mobs; `specs/endermen.md`), and in relic vault chests (weight 1) and grand relic vault pedestals. Never crafted. There is no reward for completing a cairn (owner).

### Numbers

| | Value | Where |
|---|---|---|
| Pulse radius | 8 blocks, a cylinder as far up and down | `TotemsTuning.BEGINNING_PULSE_RADIUS` |
| Pulse speed | 7 ticks per block (about 3 s to the edge) | `BEGINNING_PULSE_TICKS_PER_BLOCK` |
| Cooldown | 1200 ticks (60 s) | `BEGINNING_PULSE_COOLDOWN_TICKS` |
| Chance per block | 0.12 | `BEGINNING_PULSE_CHANCE` |
| Chance per ore spot | 0.012 | `BEGINNING_PULSE_ORE_CHANCE` |
| Beacon/statue rhythm | every 300 ticks (15 s), 2 ticks per block | `BEGINNING_AREA_PULSE_TICKS`, `BEGINNING_AREA_TICKS_PER_BLOCK` |
| Build protection | a column is skipped if it or any column within 2 holds a built/placed block | `EndermanTuning.RIPPLE_PROTECT_RADIUS` |
| Waiting for generated terrain | a column not worked out yet is retried for up to 30 s | `EndermanTuning.RIPPLE_PENDING_TICKS` |
| Cairns that leave an offering | 0.15, picked from each site's seed (owner) | `EndermanTuning.OFFERING_SHARE` |
| Offering timing | checked every 5 s while a player is within 96; 1 in 12 per check (about a minute) once complete with one of its endermen within 16 of the centre | `OFFERING_CHECK_TICKS`, `OFFERING_ONE_IN`, `OFFERING_WITNESS_RANGE` |
| Offering spot | 2 blocks outside the cairn's footprint, side picked by the seed; appears with an enderman's teleport sound and portal particles (no enderman walks over to set it down) | `OFFERING_GAP` |

`TotemsTuning.BEGINNING_RADIUS` (24) and `BEGINNING_TICKS_PER_BLOCK` (2) are no longer used by any code; remove them.

### Code

- `compat/SleeperHook` has `restoreAround` and `setUpEndermen(offering, kin)`; `ReworkedMobsSleepers` calls the Reworked Mobs api methods and ignores a `LinkageError` from an older Reworked Mobs. `BubbasTotemsArtifacts.onInitialize` calls `setUpEndermen` once.
- Reworked Mobs' `pulseAround` does the reverting (`specs/endermen.md`, "The pulse").
- Textures: `tools/totems-textures.py`; rows in `docs/art-todo.md`.

## Totem of the End (`end/TotemOfTheEndItem`, `end/EndTeleport`, `mixin/EndermanKinMixin`)

Owner's design. Player page: `Totem of the End.md`.

- **Item:** `bubbas_totems_artifacts:totem_of_the_end`, `stacksTo(1)`, `Rarity.RARE`, in the creative tab after the Beginning. Never used up.
- **Found:** End city treasure chests only, 5 % per chest (`END_LOOT`; the Keeping totem is 8 % in the same table), plus relic vault chests (weight 1) and grand relic vault pedestals. No cairn reward (owner).
- **Held (either hand): endermen treat the holder as kin.** `EndermanKinMixin`: HEAD of `Enderman.isBeingStaredBy` returns false (vanilla's own disguise check is the carved pumpkin on the head; this is the same check for the hand).
  - Covers eye contact for every enderman (vanilla, Nether and End too) and Reworked Mobs' cairn anger *(builder's choice)*.
  - Hitting an enderman still makes that one fight (vanilla); the others near a cairn stay calm.
- **Used: teleports to the aimed block** within 32 blocks (`END_RANGE`), then a 4 s cooldown sweep (`END_COOLDOWN_TICKS` 80, `ItemCooldowns.addCooldown`, B19). Each teleport deals an ender pearl's damage (owner): 5 (`END_TELEPORT_DAMAGE`, `damageSources().enderPearl()`). No hit or no safe landing: nothing happens, no cooldown.
  - **Landing rules (owner):** top face: stand on it. Side face: on top if the 2 blocks above are open, else straight down from in front of it to the nearest safe footing. Bottom face: straight down to the nearest safe footing.
  - *Builder's choices:* top face with no room or unsafe → nothing (no fallback downward). Fire, campfire, powder snow, magma, cactus, berry bush, wither rose and lava count as unsafe. Aims through grass, flowers and water, so it can land on a lake bed, as an ender pearl can.
  - **Aim:** `level.clip` from the eyes, `ClipContext.Block.COLLIDER` (through grass and flowers) and `Fluid.NONE` (through water). Landing uses vanilla's `noCollision` for the entity's box (as `randomTeleport` does).
  - **Teleport like the ender pearl:** `stopRiding`, `ServerPlayer.teleport(TeleportTransition(..., Vec3.ZERO, Relative.ROTATION, DO_NOTHING))`, `resetFallDistance`, `resetCurrentImpulseContext`, `GameEvent.TELEPORT`; sound `ENDERMAN_TELEPORT` and `PORTAL` particles at both ends.
- No beacon use and no statue effect.

## Totem statue (block, B15 exception)

- **Block:** `bubbas_totems_artifacts:totem_statue` ("Totem Statue"). **B15 reason (owner-approved):** one shared block for every totem, so a found totem can be set down. No block entity.
- **Placing (owner-approved):** sneak and use any totem on a block. Sneaking is required so that holding a totem for its pulse never places it by accident; without sneaking the click does nothing. The statue faces the player and the totem is used up (creative keeps it). The Grave Talisman is not a totem and has no statue.
  - `statue/StatueTotemItem` is an `Item` whose `useOn` places the statue when sneaking (PASS otherwise, so `use()` still runs). Every totem item is or extends `StatueTotemItem`, so sneak-to-place lives in one place.
  - **Sneak-use on a block always places the statue** (builder's choice): a sneaking Totem of the End doesn't teleport, the Beginning doesn't ripple, Seeking doesn't pick a structure.
  - A totem's item cooldown doesn't block placing (see Totem of Warding, Held).
- **Breaking** (any tool, pickaxe fastest, strength 1) drops the totem back; pistons pop it off as its totem. Shape: 10x10 footprint, 14 high (plinth and column).
- **Which totem:** `statue/TotemType`, in order: WARDING, KEEPING, GATHERING, FORTIFICATION, GROWTH, SEEKING, BEGINNING, END, HERDING, CLEAR_SKIES, NORTH_WIND, with `forItem(Item)`. WARDING is first, so it is the default, and statues saved before the `totem` property existed load as Warding.
- **Loot (26.3 format):** one pool, an `alternatives` entry with one item per totem, each with `"condition": {"type": "minecraft:all_of", "terms": [survives_explosion, match_block {state: {totem}}]}`. The pre-26.x `conditions` / `block_state_property` keys are silently ignored by 26.3 (they made every statue drop every totem).
- **Art (initial; owner will redesign, B14 person-made designs):** generated by `tools-local/make_statue_textures.py` (`main()`: Warding's hooded figure; `write_others()`: the rest). 16x and 32x (`pixelart.render32`); rows in `docs/art-todo.md`.
  - Builder's choices: one generic figure (body, arms, head, chest emblem) on Warding's plinth, told apart by per-totem tops and emblems in each item's palette: Fortification's helm brim, Growth's leaf crown and blossom, Seeking's gold band, the Beginning's grass cap, the End's purpur crown with four spikes; Gathering raises its hands, Keeping hugs a deeper chest.
  - Glowing parts: the Beginning's and the End's eyes (light 12), Fortification's visor (10).
- **Modelling rule** (owner asked for no overly small details):
  - No model element under 1 px thick or wide.
  - Glow overlays (eyes, visor) sit 0.25 px in front of the face; closer ones z-fight at range.
  - Emblems stand 1 px proud of the chest; Keeping's chest box is 2 px deep so it still stands out.
  - Topper faces sample a UV the same size as the face (1 texel = 1 px), not one patch squeezed onto every face.
  - Head fronts carry no random grain, so eyes, cheeks and mouths read.
  - Generators: `tools-local/make_statue_textures.py` (statues), `tools/totems-textures.py` (items). Totem items are flat `item/generated` sprites; there are no 3D held totem models.
- **Per-statue shapes:**
  - Warding: a 2 x 2 x 1 px diamond rune on the chest; eyes in the hood's shadow.
  - Fortification: helm brim 1.5 px high; visor glows.
  - Growth: leaf crown with a blossom on top, no specks on its sides.
  - Seeking: gold band 2 px high.
  - The Beginning: grass cap in two bands; glowing eyes.
  - The End: crown band 1 px, four spikes of 1 x 2 x 1 px; glowing eyes.
  - Herding: 2 px horns and curls on whole pixels with a ridged texture; wool. The item's horn curls are solid.
  - Clear Skies: crown band 1 px, gold in bands; rays on whole pixels, none overhanging the band: 2 px broad at the front (tallest), back and sides, 1 px at the corners.
  - North Wind: crown band 1 px, four icicles lit at the tip, two 1 x 1 px gust bars, cheeks 2 px wide.
  - *Builder's design calls, not yet confirmed by the owner:* North Wind's four icicles and two gust bars, and Clear Skies' regrouped rays (broad front/back/side rays, narrow corners).

<a id="statues-give-their-totems-effect-built-2026-11-22"></a>
### Statues give their totem's effect

A statue gives a weaker form of its totem's area effect in an **11x11x11 box** round it: radius 5 for every statue (owner; one constant `TotemsTuning.STATUE_RADIUS`, so `StatueEffects.area(pos, type)` is the same box for every type).

- **Rhythm** (`statue/StatueEffects`): a scheduled block tick every 20 ticks (`STATUE_PULSE_TICKS`), no block entity. `onPlace` starts it; statues from older saves restart from a random tick. Each pulse refreshes its area for 40 ticks under a `Source(pos)` key, so it never collides with a beacon's.

| Statue | Effect within 5 blocks |
|---|---|
| Warding | monsters keep out, and sleepers never wake (`Wards.put(..., keepsSleepersAsleep = true, ...)`), like a full-pyramid beacon ward (owner). Reworked Mobs crypts place these statues (`specs/graveyards.md`, "Warding statues in crypts"). |
| Gathering | dropped items and XP circle the statue in a ring a side hopper collects (see Totem of Gathering, "Statue ring") |
| Fortification | no blast damage to blocks |
| Growth | plants grow twice as fast, plus planting |
| Herding | a small pasture |
| Clear Skies | no rain, snow or lightning |
| North Wind | gliders get a gust every 5 s (builder's choice, see Totem of the North Wind) |
| The Beginning | an area pulse every 15 s (see Totem of the Beginning) |
| Keeping, Seeking, the End | nothing (no area form) |

<a id="relic-maps-and-relic-vaults-owner-2026-11-24-built-2026-11-24"></a>
### Totem art: precious-material idols (owner, 2026-11-23)

Owner: "more totem art variety ... something in between what we have now and the Undying one ... precious materials". Materials are the builder's picks (owner to swap any: one palette edit in `tools/totems-textures.py`; the statue follows).

| Totem | Material (was) | Gem eyes | Kept motif |
|---|---|---|---|
| Warding | silver, cool polished iron (soul stone) | soul-fire slits (now 2 px tall) | hood, soul flame, glowing ward rune |
| Keeping | crying obsidian (purple obsidian) | magenta | gold-bound chest held to the chest |
| Gathering | polished copper (copper with patina) | verdigris | upturned hands, oxidised-copper ring |
| Fortification | netherite (iron) | glowing visor | helm (now gold-banded), shield (now gold with a lapis field) |
| Growth | resin amber (oak) | emerald | leafy crown with blossom, sprout |
| Seeking | lapis (lapis, richer) | gold | gold-rimmed hood, compass dial with needle |
| the Beginning | emerald (weathered stone) | amethyst | grass cap, grass block on the chest |
| the End | amethyst (end stone) | enderman face, magenta eyes | crown (now pale end stone, was purpur), ender pearl |
| Herding | ruby-red redstone (spruce) | gold | woolly ram's head, horns (now ivory), gold bell |
| Clear Skies | quartz (calcite) | sky blue | gold sun crown, sky-and-sun emblem |
| North Wind | diamond (packed ice) | deep blue | icicle crown, gust, snowflake |

Shared changes to every icon (agent's choice, to land "in between"): 2x2 gem eyes in vanilla's pattern (highlight, gem,
gem, dark gem) where the face allows; a dark belt row with a 2 px gem; the lower body tapers like the Undying's
(8 wide belt, 6 wide skirt, 4 wide foot) instead of the old 10 wide pedestal. Heads, arms and emblems are unchanged in
shape. Statues: same shapes, recoloured from the item palettes, gem eyes, and a dark belt with a gem on the robe's
bottom row.

### Totem of Undying statue (owner: "give Totem of Undying a statue")

Decorative only (no area effect; owner may want one).

- `TotemType.UNDYING` (`totem=undying`), item `minecraft:totem_of_undying`. Decorative: `StatueEffects.pulses` is
  false for it (no area effect, no ticking).
- Placement: `statue/UndyingStatue` registers a Fabric `UseBlockCallback`. It acts only when the player is sneaking
  (`isSecondaryUseActive`), isn't a spectator, may build, and holds a Totem of Undying in the hand used. It then runs
  the same `TotemStatueBlock.placeFrom` as our totems (faces the player, uses one totem in survival). Everything else
  passes through, so vanilla's death protection and a plain right-click are untouched.
- Loot: the statue loot table's undying entry drops `minecraft:totem_of_undying` (the generator now accepts a
  namespaced item in `ITEMS`).
- Relic vaults never pick the undying statue for their pedestal (`RelicVaultPiece.primeStatue` drops UNDYING from the
  list first, so existing vault seeds keep their old picks).
- Model: `write_undying` in `tools-local/make_statue_textures.py` makes an 8x6x8 head with a 2x1x3 nose box, a crossed-arms
  band (8x2x2, one pixel proud at the front and sides), a 6x5x4 robe, and the shared plinth. All on whole pixels, with
  textures sized to each face. It uses vanilla's own totem colours.
- Warding's item art still comes from `tools/warding-textures.py` (palette and eyes changed there).

Tests: - `TotemStatueGameTest.undyingStatuePlacesFromVanillaTotemAndDropsIt`: a plain right-click with a Totem of Undying on a
  floor places nothing and keeps the totem. Sneak-using it through `ServerPlayerGameMode.useItemOn` places the undying
  statue facing the player and uses the totem up. The statue drops the vanilla Totem of Undying.
- Client part `totems` now also takes `totems-hotbar2` / `totems-hotbar2-32x`: Warding, Seeking and North Wind beside
  vanilla's Totem of Undying. Part `statues` fits 12 statues (floor widened, row shot pulled back a block).

## Relic maps and relic vaults

Owner's design (C6 detail, spec not leaf): torn **relic map shreds** turn up in loot; shreds of one relic combine into a real filled map; each map leads to that relic's one **relic vault**, a sealed treasure room buried under the world. Player pages: `Relic Maps.md` and `Relic Vaults.md`.

**Prior art (B9):** `tools/prior-art.py` found nothing for "map fragments", "treasure map pieces", "buried vault structure" (it may have been offline). Known: Quark's Pathfinder's Quills (maps to biomes), Explorer's Compass / Nature's Compass (locate structures), vanilla explorer and buried-treasure maps, YUNG's structure mods. None splits a map into named shreds that lead to one fixed seeded site per name; ours adds the collect-and-combine loop and the one-place-per-name rule.
- Hand-rolled: no library places structures at seed-derived fixed sites, and vanilla's placements are a grid (random_spread) or rings, neither of which gives "exactly one site per name". Code-built pieces match the crypts.
- Vanilla subsystems used: structure / structure set / `StructurePlacement` registry, `StructurePiece`, `CustomRecipe` + `RecipeSerializer`, `LootItemConditionalFunction`, `MapItem`/`MapItemSavedData` explorer-map decorations, Cushion and armour stand entities, chest loot tables.

### The shred item

- One item, `bubbas_totems_artifacts:relic_map_shred`, stacks to 16, uncommon. Data component `bubbas_totems_artifacts:relic_shred` = `{name, size, count}` (name like `wondrous_crab`, size 4 or 9, count 1..size).
- Item name "<Name> Relic Map (<count>/<size>)", e.g. "Wondrous Crab Relic Map (1/9)". Tooltip: "2 of 4 pieces" only (owner: no lore or hint lines); a whole one says "Whole again. Carry it to read where it leads."
- **Names:** 8 adjectives x 8 nouns = 64 names; with the two sizes, 128 maps per world.
  - Adjectives: Wondrous, Egregious, Forgotten, Gilded, Sunken, Whispering, Crooked, Radiant.
  - Nouns: Crab, Zealot, Lantern, Serpent, Crown, Owl, Anvil, Bell.
- **Look:** the `custom_model_data` string picks the texture: `4` (a big torn corner), `9` (a small scrap, older paper), `whole`.
- **Combining:** recipe type `bubbas_totems_artifacts:relic_shreds` (special recipe, `data/.../recipe/relic_shreds.json`), shapeless: 2+ shreds of the same name and size whose counts add up to at most the whole, in any grid (2x2, crafting table, crafter). The result is a combined shred.
- **Becoming a map:** a whole shred becomes the filled map when it ticks in a player's inventory (recipes have no level to make a map in). A whole shred out of a crafter becomes a map once picked up.
- **The finished map:** vanilla filled map, scale 2 (like explorer maps), biome preview (water outlines), red X decoration on the vault's door, item name "<Name> Relic Map". It keeps the `relic_shred` component.
- **Creative tab (owner: generic):** one unnamed shred per size (`RelicShred` with an empty name, shown as "Relic Map Shred (1/4)"). In a player's inventory it picks a random relic name on its first tick (`RelicMapShredItem.inventoryTick`); an unnamed shred never combines. Checked in `completeMapPointsAtVault`.
- **Loot function** `bubbas_totems_artifacts:random_relic_shred`: a random name of the 64; /9 35 %, else /4; 1 piece 80 %, 2 pieces 15 %, 3 pieces 5 % (never a whole map).

Shred odds per chest (one roll each; Reworked Mobs tables keyed by id through `LootTableEvents.MODIFY`, so no dependency):

| Table | Chance |
|---|---|
| simple_dungeon | 30 % |
| abandoned_mineshaft | 20 % |
| shipwreck_map / treasure / supply | 40 % / 30 % / 15 % |
| buried_treasure | 40 % |
| desert_pyramid, jungle_temple, igloo_chest | 30 % |
| stronghold corridor / crossing / library | 25 % / 25 % / 40 % |
| woodland_mansion | 30 % |
| pillager_outpost, ancient_city | 25 % |
| ruined_portal | 15 % |
| underwater_ruin big / small | 30 % / 15 % |
| village cartographer / temple / weaponsmith, toolsmith / houses | 30 % / 15 % / 10 % / 8 % |
| trial_chambers/corridor, bastion_other | 10 % |
| Reworked Mobs crypt noble / family / pauper | 50 % / 35 % / 15 % |
| Reworked Mobs monster_camp, camp_room | 30 % |
| Reworked Mobs fabricator_office (+ `_with_gems`) | 25 % |
| Reworked Mobs gameplay/enderman_trade (cairn trades) | 10 % |

### Where the named vaults are (seeded at world start)

- **Sites** (`RelicSites`): from the world seed alone (Xoroshiro random, seed xor a fixed salt), every (name, size) pair in a fixed order gets one chunk at a random angle and distance from the world's middle: /4 between 400 and 2400 blocks, /9 between 1600 and 4800. A candidate within 20 chunks of an earlier site is re-rolled (up to 48 tries). Same seed, same sites, always; nothing is generated or stored when a map is made.
- **Never under an ocean (owner):** site picking skips a candidate chunk whose middle has an ocean biome (`#minecraft:is_ocean`) at sea level: one biome lookup, via `BiomeSource.createUncachedResolver(randomState)`. The rule is passed in as `RelicSites.Land` (`Land.of(level)` for maps; `notOcean(...)` from the generation context and, through `StructureStateAccessor`, the placement); tests use `Land.ANY`. If all 48 tries land in ocean, the last try is used.
- **Real worldgen structures:** a custom structure placement `bubbas_totems_artifacts:relic_sites` (structure set `relic_vaults`) says "this chunk" for exactly those 128 chunks, so the vaults are built when their chunk generates. Structure `bubbas_totems_artifacts:relic_vault`, Overworld biomes, step `underground_structures`.
- **Depth:** the vault's door is at the chunk's middle, floor 12 below the ground there (owner: 5 for a crypt floor plus the 7-step stair, so the stair's top sits where a crypt's floor would), never lower than 12 above the world's bottom.
- **One vault per name and size:** once emptied, other maps of that name and size lead to an empty room.
- **Not findable with vanilla `/locate` (owner: skip; maps are the way).** Vanilla `/locate structure` and explorer-map searches only know random-spread and ring placements, so they don't find these. The maps and `/bubbas_totems_artifacts relic_map` do. No own locate command and no Totem of Seeking target.

### Relic vaults: the room (`RelicVaultPiece`, code-built like crypts)

| | /4 (small) | /9 (grand) | Crypt vault |
|---|---|---|---|
| Room inside (w x l x h) | 9 x 9 x 4 | 13 x 11 x 4 | 7 x 7 x 4 |
| Floor depth | 12 under ground | 12 under ground | 12 under ground (7 under the crypt floor) |
| Chests (stacked two high) | 3 | 6 | 2 |
| Armour stands | 2 (iron or gold sets) | 4 (iron, diamond or gold) | 1 (chain) |
| Tables with cushioned chairs | 1 | 2 | 1 |
| Pedestal: statue | any totem except the Beginning and the End | any totem, the Beginning and the End included | as small |
| Pedestal: weapon | diamond sword or axe, enchanted 20-30 | netherite sword or axe, enchanted 30-39 | diamond sword or axe, enchanted 15-25 |
| Sitting guard (Reworked Mobs) | iron set | diamond set | none |
| Loot table | chests/relic_vault_small | chests/relic_vault_grand | chests/relic_vault_crypt |

- **Stair:** 3 wide, stone bricks (cracked and mossy), 7 steps from the door's landing; a soul lantern at its foot. Named vaults: a stone wall after the top landing (no way in from the surface). Crypt vaults: the top landing is just south of the noble crypt's far wall, at the crypt's floor height.
- **Room:** deepslate tile walls with polished deepslate pillars, polished deepslate border and smooth quartz / polished diorite checker floor, sea lanterns set in the ceiling every 3 blocks, two pearlescent froglights in the floor beside the chiseled quartz pedestal, a red carpet runner, an iron door with a lever inside (nothing outside opens it).
- **Pedestal prize** (owner: a totem item lying on the pedestal isn't cool): `RelicVaultPiece.prime()` (STATUE or WEAPON, `RelicTuning.PRIME_STATUE_CHANCE` 0.5) and `primeStatue()` come from the vault's seed alone (their own random), so tests and commands can tell what a vault holds before it's built.
  - Statue: `TotemStatueBlock.stateFor(type)` facing north (toward the door), on the pedestal's top cell. Breaking it gives the totem; left standing it works as a statue.
  - Weapon: loot tables `loot_table/relic_vault/prime_weapon_<small|grand|crypt>.json` (`enchant_with_levels`, options `#minecraft:on_random_loot`, levels as in the table above). With Infused Gems loaded, `RelicLoot.primeWeapon` picks the `_gems` twins instead (Infused Gems' `socket_random_gems`, gear weapon, `prefer_harmful` 0.75; two gems on a grand vault's weapon, one otherwise; these tables load only with Infused Gems).
  - Weapon display with Reworked Mobs: planted point-down as a stuck loot display (`api/Crypts.placeStuckDisplay(level, pos, facing, stack)`, through `SleeperHook.placeStuckDisplay`), placed by the chunk holding the pedestal.
  - Weapon display without Reworked Mobs: a glow item frame on the north face of the pedestal block, placed by the chunk holding the frame's block (`pedestal().below().north()`). `SleeperHook.hasDisplays()` decides which, so a chunk border between the two doesn't drop it.
- **Guard:** a sitting skeleton sleeper (`api/Crypts.placeSittingSleeper`), only with Reworked Mobs; with Infused Gems its chestplate and sword are gemmed (`loot_table/relic_vault/guard_gear.json`, loads only with Gems).
- **Cushions:** vanilla 26.3 Cushion entities on the stair chairs (one colour per table: red, cyan, purple or orange).

Vault chest loot, per chest (tables written by `mods/bubbas_totems_artifacts/tools-local/make_relic_loot.py`, which also writes the six prime weapon tables):

| | Treasure rolls | Shred rolls | Totem chance | Rare chance | Infused Gems rare rolls |
|---|---|---|---|---|---|
| Grand | 4-7 (diamonds 1-4, enchants 25-39) | 2-4 | 45 % | 40 % | 1-2 |
| Small | 3-5 (diamonds 1-2, enchants 15-30) | 1-3 | 30 % | 15 % | 0-2 |
| Crypt | 2-4 | 1-2 | 20 % | none | 0-1 |

- **Totem weights** (the same in every vault kind): Keeping, Gathering, Fortification, Growth, Seeking, Herding, Clear Skies 3 each; Warding and Grave Talisman 2; the Beginning and the End 1. The North Wind isn't in vault chests.
- **Rare:** enchanted golden apple, netherite scrap, totem of undying, Pigstep, heart of the sea.
- **Infused Gems:** its `gems/rare` table rolled into the drops at open time (`LootTableEvents.MODIFY_DROPS`), only with that mod loaded.

### Noble crypt vaults (no hard dependency either way)

- Reworked Mobs has `api/Crypts` (stable, add-don't-change): `setNobleAttachment(...)`, `attachments(piece)`, `placeDisplay`, `placeStuckDisplay`, `placeSittingSleeper`. `CryptYardStructure` and `GraveyardCommand.build` add whatever the attachment returns; nothing is registered unless Totems & Artifacts is loaded. `CryptYardPiece` has `seed()` and `nobleFarWall()`.
- Totems & Artifacts registers the attachment through its `SleeperHook` (only when Reworked Mobs is loaded): 35 % of noble crypts (rolled from the crypt's seed) get a crypt vault, its stair starting just behind the crypt's far wall, between the two family chests.
- Crypt vaults are **unnamed** (builder's choice; no map leads there), so each name keeps exactly one place (open question).

### Commands (op)

- `/bubbas_totems_artifacts relic_map <name> <4|9>`: the finished map for that relic.
- `/bubbas_totems_artifacts relic_shred <name> <4|9> <count>`: a shred.
- `/bubbas_totems_artifacts relic_vault <small|grand|crypt>`: builds a vault with its door at the player's feet (room to the south).

### Code

Package `relic/`: `RelicNames`, `RelicShred` (component), `RelicMapShredItem`, `RelicMaps`, `RelicShredRecipe`, `RelicShredFunction`, `RelicSites`, `RelicVaultPlacement`, `RelicVaultStructure`, `RelicVaultPiece`, `RelicLoot`, `RelicTuning`, `RelicCommand`, `Relics` (wiring). All numbers in `RelicTuning` (and the loot script).

## Moved from Reworked Mobs

The Grave Talisman and the Totem of Warding started in Reworked Mobs. Behaviour is unchanged; the names moved:

| Piece | In `bubbas_reworked_mobs` | Now in `bubbas_totems_artifacts` |
|---|---|---|
| Grave Talisman | `:grave_talisman`, `undead/GraveTalisman`, `client/undead/GraveTalismanGlow`, property `bubbas_reworked_mobs:grave_talisman_glow` | `:grave_talisman`, `talisman/GraveTalisman`, `client/talisman/GraveTalismanGlow`, property `bubbas_totems_artifacts:grave_talisman_glow` |
| Totem of Warding | `:totem_of_warding`, `warding/*` (`TotemOfWardingItem`, `Wards`, `WardBarrier`, `WardFleeGoal`, `WardingLoot`, `WardingTuning`) | same names under `warding/` |
| Ward mixins | `MobTickMixin`, `PhantomSpawnerMixin`, `Walk/SwimNodeEvaluatorMixin`, `MobAccessor`, ward half of `SpawnStateMixin` | same names (prefix `bta$`), plus `WardSpawnStateMixin` (the ward check only); Reworked Mobs' `SpawnStateMixin` keeps only the monster-cap cut |
| Beacon payload | `compat/WardingBeaconPayload` | same class here; lang keys `beacon.bubbas_totems_artifacts.ward*` |
| Assets | Reworked Mobs assets and 32x pack | this mod's assets and its own 32x pack `bubbas_totems_artifacts_32x` |
| Texture scripts | `tools/warding-textures.py`, Reworked Mobs `tools-local/make_talisman_textures.py` | same script (writes here) / `mods/bubbas_totems_artifacts/tools-local/` |
| Creative tab | Reworked Mobs tab | its own tab "Bubba's Totems & Artifacts" |

Reworked Mobs doesn't depend on Reworked Beacons at all; it `suggests` Totems & Artifacts.

**Saved worlds (old item ids):** `ModItems` registers a Fabric registry alias per moved item (`BuiltInRegistries.ITEM.addAlias(old, new)`). A lookup of `bubbas_reworked_mobs:grave_talisman` / `:totem_of_warding` that isn't registered falls back to the new item, so old stacks (chests, inventories, item frames, commands, the test world) load as the new items and are saved under the new id.
- Chosen because vanilla has no item-id alias, and a DataFixer is heavier and only runs on version bumps.
- The alias lives here, so it works with or without Reworked Mobs. Without this mod the old stacks have no item and Minecraft drops them as unknown (vanilla behaviour for a removed item); there is deliberately no fallback in Reworked Mobs (owner: fine as is). Players updating Reworked Mobs should install this mod alongside.
- The newer totems have new ids, so they need no alias.

## Independence

Same pattern as Reworked Beacons. `fabric.mod.json` `depends` on Fabric API only and `suggests` `bubbas_reworked_mobs` and `bubbas_reworked_beacons`; the build has `compileOnly` against both. Neither mod requires the other. Owner's principle: "mods should work standalone, but that doesn't mean fallbacks everywhere": each mod runs alone, but removing a mod doesn't need a stand-in for its items.

**The sleeper hook** (Reworked Mobs `io.github.bubba8587.bubbas_reworked_mobs.api.Sleepers`; stable: add, don't change):

- `int calm(ServerLevel, Vec3 centre, double radius, float points)`: takes points off sleepers' disturbance meters in range (wake is 100). The held Warding totem's pulse.
- `void keepAsleep(ServerLevel, Object source, AABB area, int lifetimeTicks)` / `void release(ServerLevel, Object source)`: sleepers in the area never wake from noise; each source's area lapses unless refreshed. A full-pyramid ward. Backed by Reworked Mobs' `undead/sleeper/SleepAreas`.
- `boolean isKeptAsleep(Level, BlockPos)`, `boolean isSleeper(Level, BlockPos)`, `double nearestSqr(Level, Vec3, double range)` (client or server; what the talisman senses).

**The endermen hook** (Reworked Mobs `api.Endermen`; stable: add, don't change): `keepOut(ServerLevel, Object source, AABB area, int lifetimeTicks)` / `release(ServerLevel, Object source)` / `isKeptOut(Level, BlockPos)`. Every ward also keeps Reworked Mobs' endermen (their jobs and cairn spawning) out, through `SleeperHook.keepEndermenOut` / `releaseEndermen`.

**How this mod reaches Reworked Mobs:** only through `compat/SleeperHook`, picked once by `FabricLoader.isModLoaded("bubbas_reworked_mobs")`. Only `compat/ReworkedMobsSleepers` names Reworked Mobs' classes, and it is created only when that mod is loaded.

**Without Reworked Mobs:**
- The talisman senses `#minecraft:undead` mobs only.
- The Totem of Warding still scares monsters, stops natural spawns in a ward, keeps monsters and happy ghasts out and pushes flyers out; it just has no sleepers to calm or keep asleep.
- The Totem of the Beginning does nothing, and beacons refuse it.
- No relic shreds in Reworked Mobs' chests, no crypt vaults, no vault guards; vault weapons hang in glow item frames.

## Tooling

`tools/install-to-modrinth.ps1`, `tools/build-testworld.py` (`MODS`) and `mods/testworldshots` (`modsUnderTest`) include this mod. The test world builder names `bubbas_totems_artifacts:totem_of_warding` (the saved world's old-id totems load through the alias). No mod icon yet; it must be person-made (B14).

## Open

Owner decisions still pending (built as described above until decided):

- **Mob holders:** should mobs holding a Totem of Warding pulse, and may woken undead pick one up? (built as yes, agent's choice)
- **Names:** the Grave Talisman's name; the Totem of Growth's name (owner wanted a verb-like word).
- **Seeking:** signature blocks (built) or a picker screen; keep pointing at a found structure (built) or "point to the next one".
- **Builder's calls to review:** Keeping counts anywhere in the inventory and keeps XP levels; the totem loot tables and chances; Gathering's beacon ring; Growth's strength; the Beginning's ripple choices (what counts as built, protection radius) and the End's landing choices and End city 5 %; the RARE rarity of both; trapped monsters in a ward fight rather than cower.
- **Herding** counts every animal except tamed pets, so bees, axolotls and the like too (builder).
- **North Wind statue:** gusts gliders within 5 blocks (builder's choice), or decorative only?
- **Gathering ring look:** functional but plain (items orbit at the beacon's top). Slower, or a second ring for XP? Statue ring size (radius 1.0, 1.1 up) to playtest.
- **Art:** person-made statue designs and mod icon (B14); the Gathering texture reads like gingerbread; the Herding and Clear Skies items and statues are builder art (`docs/art-todo.md`).
- **Numbers to playtest:** every value in `TotemsTuning` and `WardingTuning`.
- **Relic vaults:** should some crypt vaults carry a name too (two places per name)? The whole shred becomes a map only once it's in a player's inventory: OK, or unfold on right-click? Tune 35 % of noble crypts with a vault and 45 % totem chance per grand vault chest (about 3 totems plus the pedestal's prize per grand vault).

## Milestone test objectives

Test-lite (`specs/quality.md`, "Test suites").

- **Run:** `./gradlew :bubbas_totems_artifacts:build` (standalone core); `:bubbas_totems_artifacts:runGameTest -PwithMobs -PwithBeacons -PmilestoneTests` for every test for real.
- `-PwithMobs` puts Reworked Mobs on the test classpath and `-PwithBeacons` Reworked Beacons. Without them, sleeper tests and beacon tests pass trivially.

### Core (`BubbasTotemsArtifactsCoreGameTest`, 17, one per mechanic)

Most bodies live in the milestone classes below and are called from the core class.

- `sensesUndeadMobs`: the talisman senses undead mobs at 29/17/11 blocks.
- `heldTotemPulsesAndScaresMonsters`: a held Warding totem pulses onto the cooldown and scares a zombie away.
- `wardStopsSpawnsAndSendsMonstersOut`: a ward stops spawns and sends monsters out.
- `sneakPlacesStatueAndBreakingGivesTotemBack`: sneaking places a statue facing the player; breaking it drops the totem.
- `keepingKeepsEverythingOnce`: with keep_inventory off, a player with the totem in the backpack dies: no item or XP drops, one totem used, tagged; respawned with the dirt and 5 levels, tag gone; the next death drops.
- `gatheringPullsNearbyItems`: an armor stand holding it pulls a dirt item from 6 blocks; an item 12 blocks away stays.
- `fortificationStopsBlockDamage`: a TNT-type blast inside a 5x3x5 dirt cube next to a holder breaks nothing.
- `growthSpeedsUpCrops`: 40 boosted ticks in the held box grow a 3x3 wheat patch; one outside the box doesn't grow.
- `seekingChoosesAndPoints`: mossy cobblestone chooses jungle temple, stone chooses nothing; a village search finishes and the totem points at the result (spins in the test world, which has no structures).
- `endLandingRules` (body in `BeginningEndGameTest`): top face, side with room, side without room → floor in front, slab top, fire on top → nowhere, bottom face → floor under it, miss → nowhere.
- `gatheringRingFeedsASideHopper`: an item 6 blocks out ends up in a chest through a side hopper.
- `herdingPastureKeepsLivestockIn`: a cow is blocked at the pasture edge, not inside; a zombie never.
- `clearSkiesKeepsRainOff`: an area column gets no precipitation while it rains; outside does.
- `statuesPulseTheirArea`: Growth and Herding statues set `STATUE_RADIUS` areas, nothing past radius + 2; Keeping none.
- `northWindGustsGlidersOnly`: see Totem of the North Wind.
- `relicShredsCombineOnlySameNameAndSize`: shreds combine only with the same name and size, totals no more than the whole; 2x2 and 3x3; the recipe loads and is found for the inventory grid.
- `relicVaultBuildsSealedWithPartwayStair`: every vault kind builds sealed (flood fill stays inside), iron door, the stair climbs 7 and (named) stops at stone, chests, stands, cushions, the pedestal prize the seed says.

### Milestone

- **`TotemsGameTest`:** keep_inventory on doesn't use the Keeping totem (`keepingNotUsedWithKeepInventory`); own fresh throws and never-pickup items aren't pulled, others' throws are (`gatheringSkipsOwnThrowsAndDisplays`); the same blast without a holder breaks blocks (`explosionWithoutTotemBreaksBlocks`); only blocks within 8 of the holder are spared (`fortificationOnlyNearTheHolder`); a real held Growth totem (armor stand, random_tick_speed 100, 400 ticks) beats vanilla (`heldGrowthOutpacesVanilla`); the stepped search matches vanilla /locate for every type that exists (`seekingMatchesVanillaLocate`; none in the server test world, so the client test checks it in a world with villages); the ring walk covers every cell once (`seekingRingCoversEveryCell`); every totem's loot (`totemsAreFoundInLoot`: chest tables rolled as chests, the evoker as entity loot, about 5 %; each at about its chance over 300 rolls); `totemsAreArtifacts` (every statue totem one per stack and not common; the talisman one per stack); old Reworked Mobs ids load as these items (`oldReworkedMobsIdsLoadAsTheseItems`).
- **`NewTotemsGameTest`:** `gatheringStatueRingsItemsForASideHopper` (an item in range is ringed and collected by a side hopper into a chest; a touching chest stays empty; an item beyond the radius is untouched); `growthPlantsDroppedSeeds` (a wheat seed is planted on farmland, exactly one); `growthNeedsRoomAround`; `growthNeverStacksPlants` (200 tries each: sugar cane, bamboo and cactus items never planted on top of their own kind; crops, saplings and flowers count as plants; an oak sapling is still planted on dirt); plus the bodies of the core ring, pasture, Clear Skies and statue-area tests.
- **`TotemsBeaconGameTest`** (real only with `-PwithBeacons`): `gatheringBeaconStartsTheRing`; `fortificationBeaconProtectsItsArea` (the area survives a blast); `growthBeaconSetsItsArea` (a 4-layer area has strength 4 and reaches 45 blocks); `seekingBeaconPointsCompasses` (a held compass points at the search result, keeps the name "Compass", and turns plain after its time); `payloadsAcceptTheirTotems` (each payload accepts only its own totem).
- **`BeginningEndGameTest`:** `endTeleportsWhereAimedWithCooldown` (real use: unsafe aim → FAIL, no cooldown, no move; aim at a block 10 away → there, cooldown on, not used up); `endHeldMakesEyeContactHarmless` (vanilla `isBeingStaredBy`: true without, false with the totem in the off hand); `beginningPulsesOnlyWithReworkedMobs` (with `-PwithMobs`: SUCCESS, count stays 1, cooldown full; without: PASS and no cooldown). Not tested: outside the overworld (`Ripples.supported` covers it with the same check).
- **`BeginningAreaGameTest`** (needs `-PwithMobs`, beacon part `-PwithBeacons`): `beginningBeaconRevertsAHole` and `beginningStatueRevertsAHole` (each reverts most of a dug hole over one rhythm window; ore spots only get ore).
- **Reworked Mobs side** (`specs/endermen.md`): core `ReworkedMobsCoreGameTest.endermanRippleRestoresGroundButNotBuilds` (body in `EndermanTotemsGameTest`: a hole fills with grass on stone, a dirt pillar goes, sand on the top layer turns to grass, a 3x3 pit with a plank floor keeps its air); milestone `EndermanTotemsGameTest.offeringLaidOncePerCairn` (laid once on display; a player takes it with an empty hand; never laid again, also after it's taken), `pulseOddsTerrainOverOre`, `pulseRepeatsAndBuilds`.
- **`NorthWindGameTest`:** see Totem of the North Wind.
- **`TotemStatueGameTest`:** sneak-place, facing, no place without sneaking; `wardingStatuePlacesWhileOnCooldown`; `endStatuePlacesWithoutTeleportingAndDropsItsTotem`; `everyStatueDropsItsTotemAndOldOnesLoadAsWarding` (every statue drops its own totem; statues saved before the `totem` property load as Warding).
- **`GraveTalismanGameTest`:** undead mobs and sleepers sensed at each level.
- **`WardingGameTest`:** the core warding checks; the pulse calms sleepers; the off-hand totem pulses and a backpack one doesn't; a full ward keeps sleepers asleep; a ward pushes back mobs that don't path; a zombie can't path into a ward; a ridden happy ghast is put back outside; `trappedInAnEnclosedWardItStillFights`.
- **`WardingBeaconGameTest`** (real only with `-PwithBeacons`): a totem beacon wards its range (1 layer: spawns only, 20 blocks; 4 layers: 50 blocks; sleeper sensitivity 0 under 4 layers, 1 under 1 layer with Reworked Mobs).
- **`CryptWardsGameTest`** (`-PwithMobs`): `wardStatueKeepsNearSleepersAsleep` (a Warding statue keeps sleepers within `STATUE_RADIUS` asleep, not farther); `cryptStatuesStandInEveryWing` (crypt statues stand on every wing's spot facing the right way and never cut off part of a crypt; test structure `crypt_field`).
- **`RelicMapsGameTest`:**
  - A crafter combines two shreds.
  - Sites: same seed same sites, two seeds differ, 128 sites in unique chunks, in their distance bands, 20+ chunks apart; the structure set uses `relic_sites`; lookup by chunk.
  - A whole shred carried turns into a filled map whose X is on its name's site; /4 and /9 of one name lead to different places.
  - Loot: dungeon shred rate about 30 %; grand vault chests have a totem about 45 % and 2+ shreds each.
  - `pedestalHoldsStatueOrWeapon`: about half of 400 seeds give a statue per kind; Beginning/End statues only in grand vaults; a built statue vault has the statue facing north and breaking it drops the totem; a built weapon vault shows an enchanted netherite (grand) or diamond weapon (display with `-PwithMobs`, glow frame without); weapon vaults are also built split into two chunks meeting at the pedestal (catches a dropped glow frame).
  - (with Reworked Mobs) A noble crypt's vault: stair top just behind the far wall, the wall intact, the vault under the crypt and sealed; about 35 % of noble crypts have one.

### Client parts (`ClientTests` names; screenshots)

- `talisman`: the glow at each distance in hand and hotbar (vanilla husk, skeleton, zombie, so it runs without Reworked Mobs).
- `warding`: the Totem of Warding in the hotbar, its pulse ring and cooldown sweep in first and third person.
- `totems` (`TotemsClientGameTest`): every totem in the hotbar at 16x and 32x; Gathering before/pulling/after; the beacon ring (`totems-gathering-ring`, `-later`: items orbiting, one entering the side hopper) and the statue ring (`totems-gathering-statue-ring`); Seeking held facing the found village (needle up) and turned 90 degrees (needle to the side), 16x and 32x, with the result checked against vanilla /locate in that world; the Beginning and the End in the hotbar and held in first person (`totems-hotbar`, `totems-held-beginning`, `totems-held-end`, and their `-32x`); with `-PwithMobs`, `totems-beginning-pulse` (revert puffs on grass).
- `statues` (`StatuesClientGameTest`): all 11 statues in a row, close pairs and at night, each at 32x too.
- `northwind`: see Totem of the North Wind.
- `relics` (`RelicsClientGameTest`): `relics-inventory(-32x)`, `relics-tooltip(-32x)`, `relics-map` (finished map in hand), `relics-vault-grand-door/-back/-side/-pedestal/-pedestal-close/-stair` (grand built with a weapon seed), `relics-vault-small-door/-pedestal/-pedestal-close` (statue seed), `relics-crypt-wall/-stair/-vault` (with `-PwithMobs`), and the pedestal with Totems alone (glow item frame).
- `crypts` (`CryptWardsClientGameTest`, `-PwithMobs`): `crypt-wards-family`, `-noble`, `-noble-west`, `-noble-east`.
- Reworked Mobs part `endermen` (real default-preset world, seed 20261116): a ripple dry run (`Ripples.dryRun`, changes nothing) at spawn and 40 blocks east of each of the three nearest cairns, radius 24: **0 changes on untouched ground at all four**. Control: after digging a 3x3x3 hole and stacking a 3-high dirt pillar at spawn it finds exactly 30 (9 air→grass, 18 air→dirt, 3 dirt→air). Printed as `[endermen] ripple dry run ...`.

### Objectives without a test yet

- A cow follows a player holding the Totem of Herding, and a goat (brain-driven) does too.
- Using Clear Skies in rain clears the weather and the totem isn't used up; in clear weather nothing happens.
- The client draws no rain over a Clear Skies area.
- XP leaves the Gathering ring for a nearby player.
- With Reworked Mobs, a player holding the Totem of the End breaks a cairn block and no enderman turns on them.
- Ten Beginning pulses over a dug field bring back most terrain but far less ore, and never touch a build (partly covered by Reworked Mobs' `pulseOddsTerrainOverOre` and `pulseRepeatsAndBuilds`).
- A relic vault actually generating at its site in a real (non-flat) world (game test worlds are flat).
