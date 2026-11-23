<!-- dte:C6,B15,B17,B18,B19 -->
<!-- covers: mods/bubbas_totems_artifacts/* -->
# Bubba's Totems & Artifacts (spec)

A separate mod for **artifacts**: found items that do things potions and enchantments don't. Mod id `bubbas_totems_artifacts`, code in `mods/bubbas_totems_artifacts/`, Java package `io.github.bubba8587.bubbas_totems_artifacts`. Built (0.1.0, unreleased). Owner's call (2026-11-16): "this could be its own mod, Bubba's Totems & Artifacts. Grave Talisman and all totems go in here."

## What lives here

| Artifact | Status |
|---|---|
| Grave Talisman | built (moved from Reworked Mobs) |
| Totem of Warding | built (moved from Reworked Mobs), plus its totem statue |
| Totem of Keeping, Gathering, Fortification, Growth, Seeking | built 2026-11-16 (below; unreleased) |
| Totem of the Beginning, Totem of the End | built 2026-11-22 (below; unreleased) |
| Totem statue for every totem (decorative) | built 2026-11-22 |
| Totems of Herding and Clear Skies | in progress (agent 3, `totems-2` branch; not in develop) |
| Gathering beacon ring, Growth seed planting, statues giving a tiny-radius effect | in progress (agent 3, `totems-2` branch; develop still has the chest-below Gathering beacon and inert statues) |

Previews (client test screenshots, 2026-11-16, still current for these totems): `docs/previews/totems/` (`textures-16x-32x.png` and `hotbar-16x-and-32x.png`: the Warding, Keeping, Gathering, Fortification, Growth and Seeking icons at 16x and 32x; `gathering-pulling.png`: a held Gathering totem pulling items; `seeking-ahead-turned-turned32x.png`: the Seeking needle facing the found village, turned 90 degrees, and at 32x).

What stays in Reworked Mobs (`specs/undead.md`): sleepers, the hearing/disturbance meter, light calming sleepers, bells being loud to sleepers, loot displays, combat, the monster cap, and the `api.Sleepers` hook below.

## Design rules for totems (owner)

- Many totems work both **held** (personal, small) and **in a beacon** (area).
- Any rhythm (e.g. a held pulse) shows with vanilla's item cooldown sweep (B19 noHiddenCooldowns); vanilla mechanics first (B17); mobs and players treated the same (B18).
- **No overlap with Infused Gems (owner, 2026-11-22):** a totem never does what a gem effect already does (e.g. no Totem of Silence: the Silence gem exists). Check `specs/effect-gems.md` before proposing a totem.
- Fortification must stop explosion block damage only, never via the mobGriefing rule (that would also stop villager farming).

## The five new totems (design agreed with the owner, 2026-11-16)

Owner's ideas (BACKLOG) plus the orchestrator's detail calls, accepted by the owner ("yes, start the totems builder"). Each works held (personal, small) and, where it makes sense, in a Reworked Beacons beacon (area) through the payload API, like Warding.

- **Keeping:** held only; used up on death, and you keep your whole inventory that once (a Totem of Undying for your items). Nothing in a beacon.
- **Gathering:** held, pulls dropped items and XP within ~8 blocks to you; in a beacon, pulls them into the nearest chest or barrel under the beacon.
- **Fortification:** held, explosions near you don't break blocks; in a beacon, no explosion block damage in its area. Only explosion block damage, never through the mobGriefing rule (owner).
- **Growth** (working name; owner wanted a verb-like word, still open): faster crop and sapling growth (random tick boost) within ~8 blocks held, across the area in a beacon.
- **Seeking:** held, points like a compass to the nearest structure of one chosen type (like an explorer map); the type is chosen by using it on a structure's signature block (builder to propose the block list; owner may prefer another way); in a beacon, it points every compass held in its area.
- Found, not crafted, like the other artifacts (loot placement to be proposed). B19: any rhythm shows with the item cooldown sweep.

## The five new totems, as built (2026-11-16)

### What was built

Five items, all `stacksTo(1)`, `Rarity.UNCOMMON`, found only (no recipes), in the mod's creative tab after the Totem of Warding:
`bubbas_totems_artifacts:totem_of_keeping`, `:totem_of_gathering`, `:totem_of_fortification`, `:totem_of_growth`, `:totem_of_seeking`.
No new blocks (B15). New ids, so no old-id alias. Numbers all in `totems/TotemsTuning` (starting values, owner to tune).
"Held" = main or off hand of any living thing, players and mobs alike (B18; `totems/Held`), as with Warding. None of the five has a rhythm, so no cooldown sweep is needed (B19).

#### Totem of Keeping (`keeping/Keeping`, `mixin/PlayerKeepingMixin`)

- **Where it counts (agent's choice): anywhere in the player's inventory** (hotbar, backpack, off hand), not only in a hand. Reason: it's insurance, and deaths come unannounced; the Totem of Undying's "must be in hand" would make it a juggling act.
- On death with `keep_inventory` off: one Keeping totem is used up and **the death goes exactly as under vanilla's keep_inventory (B17)**: whole inventory kept, **XP levels kept** (agent's choice, because that is what keep_inventory does), score kept, nothing drops, no XP orbs, curse-of-vanishing items stay. With `keep_inventory` on the totem isn't used. Spectators: n/a.
- How: at the start of the death drops (`Player.dropEquipment`, HEAD) the totem is taken and the dead player gets the entity tag `bubbas_totems_artifacts.kept` (saved, so logging out on the death screen still works); the tag stops the drop and the XP orbs (`getBaseExperienceReward`); on respawn Fabric's `ServerPlayerEvents.COPY_FROM` copies inventory, XP and score the way vanilla's `transferInventoryXpAndScore` does; `AFTER_RESPAWN` clears the tag (vanilla copies entity tags to the new player).
- Effect: at the place of death everyone sees the Totem of Undying's particle burst and hears its sound; on respawn the player gets the Totem of Undying animation with the **Totem of Keeping** filling the screen (custom S2C payload `bubbas_totems_artifacts:kept` → `LocalPlayer.displayItemActivation`), the totem sound, and the action-bar line "Your Totem of Keeping held on to your things".
- Mobs: not applicable (mobs don't respawn).
- No beacon use (per design).
- **Prior art / overlap:** keep-inventory mods (Keep Inventory Always, Nice Keep Inventory, Keep Inventory Orbs; MIT/GPL), soulbound enchantments, and grave mods (Universal Graves, Gravestones, You're in Grave Danger). Graves are the closest in purpose: with a grave mod installed, a kept death simply has nothing to put in the grave (they compose). None offers a single-use found totem; no library needed (two small injections, hand-rolled because no library does it, B9).

#### Totems of Herding and Clear Skies

*Owner, 2026-11-22. In progress on `totems-2`, not in develop.*

- **Totem of Herding:** held, nearby livestock of every kind (cows, sheep, pigs, chickens, goats, horses, llamas, rabbits...) follow you as if you were holding their food, so moving a herd is easy. In a beacon, animals in the area don't wander out of it, like a pasture without fences. Found, not crafted, like the others.
- **Totem of Clear Skies:** in a beacon, no rain, snow or lightning in the area (it stays dry and lightning-safe; weather elsewhere is unchanged). Held, it's used up to end a storm (clear weather, like vanilla's `/weather clear`). Found, not crafted.
- Neither overlaps a gem effect. Each gets an initial statue like the rest (statues give their effect in a tiny radius: Herding's statue keeps animals by it, Clear Skies' statue keeps rain off a few blocks).

#### Growth also plants seeds

*Owner, 2026-11-22: Harvest folded into Growth. In progress on `totems-2`, not in develop.*

- The Totem of Growth (held and in a beacon) now and then **takes a dropped or floating seed item nearby and plants it** in a valid spot close by: wheat, beetroot, melon and pumpkin seeds, carrots, potatoes, saplings, and so on, on farmland or the right soil. It's a random chance per check, never a sweep. It doesn't gather or harvest anything (owner: use a separate Gathering totem for that). Seeds floating in a Gathering ring count as dropped items.

#### Gathering in a beacon: a floating ring

*Owner, 2026-11-22, replaces "into a chest below". In progress on `totems-2`; develop still has the chest-below version described next.*

- In a beacon, gathered items **float in a slowly rotating ring around the beacon** (no gravity, held in orbit at about the beacon's height, a radius of 1-2 blocks), instead of going into a chest below. It's meant to look cool.
- Players collect them with **a simple sideways hopper into a chest**: a hopper next to the beacon whose pickup space the ring passes through takes items as they come round. Builder sets the ring's height and radius so one hopper placed beside the beacon works.
- **XP orbs join the ring too** (owner), orbiting with the items (a player walking up collects them as usual). Ring size is capped (extra items wait where they lie until there's room).

#### Totem of Gathering (`gathering/Gathering`, `TotemOfGatheringItem`, `compat/GatheringBeaconPayload`)

- **Held:** every tick, dropped items within **8 blocks** (`GATHER_RADIUS`) fly to the holder at **0.3 blocks/tick** (`GATHER_SPEED`; about 1.5 s from 8 blocks), aimed at the holder's middle like XP orbs, then are picked up by the usual rules. At most 64 items per tick. Skipped: items the holder itself threw that are still on their pickup delay (so Q-dropping works), and items that can never be picked up (pickup delay 32767, displays/props). Someone else's fresh throw is pulled.
  - *B17 note:* vanilla's own XP-orb pull formula was tried first: on the ground item friction eats it, and items 6 blocks out moved less than a block in 5 s (seen in the client test). So the speed is a steady pull instead.
  - **XP:** vanilla XP orbs already fly to any player within 8 blocks, so the held totem leaves orbs to vanilla (no double pull).
- **In a beacon (each 80-tick pulse, any pyramid):** dropped items anywhere in the beacon's area go into **the nearest chest or barrel "under" the beacon**, inserted as a hopper would (double chests count whole; partial stacks if full). **"Under" (agent's definition):** the 3x3 column directly below the beacon, from 1 block below it down through the pyramid to 6 blocks below its bottom layer (`GATHER_CHEST_DEPTH_BELOW_PYRAMID`); nearest first (straight below, then sides, then corners, layer by layer). Chests (incl. trapped) and barrels only. **XP orbs** in the area are gathered **onto the top of the beacon** (agent's choice: XP can't go in a chest), where they wait to be collected (they still despawn after 5 minutes like any orb). **No chest or barrel: nothing at all happens** (items and XP stay put). Bounded: 256 items and 256 orbs per pulse. Effects: reverse-portal puff where an item vanished, a soft pickup sound at the chest.
  - Screen lines: "Dropped items go into a chest or barrel below", "Experience gathers on the beacon".
- **Prior art:** item magnets (Simple Magnets ARR, magnetic GPL, Magnetic Enchantment AGPL, Sticky Fingers MIT, davdeo's Item Magnet MIT). None ties into a beacon/chest; hand-rolled (tiny; no library does it).

#### Totem of Fortification (`fortification/Fortification`, `mixin/ServerExplosionMixin`, `compat/FortificationBeaconPayload`)

- **Held:** explosions break **no block within 8 blocks** (`FORTIFY_RADIUS`) of anyone holding one. **In a beacon (any pyramid):** no explosion breaks blocks anywhere in the beacon's area (area refreshed each pulse for 100 ticks, so it lapses ~5 s after the beacon stops).
- **Explosion block damage only:** the hook filters the explosion's own list of blocks to break, right where `ServerExplosion.explode` works it out, so the explosion still bangs, hurts and knocks back, blocks outside the protected zone still break, and explosion-lit fire (ghast fireballs, beds) doesn't start on protected blocks. The **mobGriefing rule is never touched** (owner). Wind charges (which only press buttons/open doors) are left alone. Cost: one entity box query per explosion; nothing when no holder or area is near.
- Feedback when it saved blocks: a dull shield-block clang and a few wax-on glints at the blast.
- Limitation: item frames, paintings and armor stands are entities and still get hurt by the blast (only blocks are protected).
- Screen line: "Explosions break no blocks".
- **Prior art:** no Modrinth hit for explosion-protection items on 26.3; anti-creeper-grief mods change the mobGriefing rule or explosion type, which the owner ruled out. Hand-rolled (one injection).

#### Totem of Growth (working name; `growth/Growth`, `TotemOfGrowthItem`, `compat/GrowthBeaconPayload`)

- **What grows faster:** block tag `bubbas_totems_artifacts:growth_totem_boosts` (datapack-editable): `#minecraft:bee_growables` (crops, sweet berries, glow berry vines), `#minecraft:saplings`, sugar cane, cactus, bamboo (+ shoot), cocoa, nether wart, kelp, twisting/weeping vines, chorus flower.
- **How (B17):** extra **random ticks**, vanilla's own growth step, so light, farmland, room for a tree etc. stay vanilla's. Extra ticks are a multiple of vanilla's rate and follow the `random_tick_speed` rule; sampling is vanilla's (random positions per chunk section, skipping sections with nothing that ticks and chunks vanilla isn't ticking).
- **Held:** a box 8 blocks each way from the holder (17x17x17), **+2x vanilla's rate (grows about 3x as fast)**. Cost: a few hundred block lookups a tick at most.
- **In a beacon:** the beacon's whole area, **+1x per pyramid layer** (1 layer: 2x as fast ... 4 layers: 5x), every tick while the area lives; at most 2048 samples per area per tick (a full 4-layer sky beacon needs ~2000).
- A plant that grows from an extra tick puffs two bone-meal sparkles.
- Screen line: "Crops and saplings grow Nx as fast".
- **Prior art:** growth accelerators/twerk mods (Crop Growth Modifier, Twerk Crop Growth, Sprinklerz); nothing on 26.3 as an item/beacon; hand-rolled (no library).

#### Totem of Seeking (`seeking/TotemOfSeekingItem`, `SeekingTypes`, `StructureSeeker`, `SeekingBeacon`, `mixin/CompassItemMixin`, `compat/SeekingBeaconPayload`)

- **Choosing what to seek:** use (right-click) the totem on a structure's **signature block** (placed anywhere). Lodestone-lock sound and action bar "Seeking: Jungle Temple"; tooltip "Seeks: Jungle Temple" or "Use on a structure's block to choose what to seek". Other blocks: nothing happens. **Proposed list (owner may prefer another way):**

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

  Not seekable (no good signature block): shipwrecks, ocean ruins, buried treasure (explorer maps), nether fossils.
- **Pointing (B17):** vanilla's lodestone compass: the totem carries an untracked `minecraft:lodestone_tracker` aimed at the structure; its item model is a `minecraft:compass` (target `lodestone`) dispatch over 16 needle frames. No target (nothing chosen, first search still running, nothing in this dimension, e.g. a fortress in the Overworld) = the needle spins like a compass in the Nether. Works anywhere in the inventory, like a compass; checks every 20 ticks (`SEEK_UPDATE_TICKS`).
- **Search, never freezing the server:** `StructureSeeker` is vanilla's /locate search (`ChunkGenerator.findNearestMapStructure`: concentric rings for strongholds, then the random-spread grid ring by ring to 100 rings, stopping at the first ring with a hit) cut into steps with a **2 ms per tick budget** for all searches together, and it **never loads or generates chunks**: a candidate counts when vanilla's quick structure check says a start is there or would be generated there (vanilla confirms that by generating the chunk's structure starts; in rare cases a structure that fails to fit still counts). Results cached per dimension, type and 256-block cell of the asker's position (shared by everyone in the cell; 512 entries, oldest dropped; cleared on server stop). While a new search runs, the needle keeps its old target. In the client test's world the result matched vanilla's /locate exactly (village at 240, 0, 128).
- **In a beacon:** every **vanilla compass held** (either hand, anyone) in the beacon's area points to the nearest structure of the totem's type, searched from the beacon. The compass gets an untracked lodestone target plus `bubbas_totems_artifacts:seeking_beacon_until`; it shimmers like a lodestone compass but keeps the name "Compass"; 100 ticks after it was last reached (`SEEK_COMPASS_LIFETIME_TICKS`) it turns plain again. Lodestone-bound compasses are left alone. A totem with no type does nothing; its screen line says "Choose a structure first: use the totem on its block".
  - Screen line: "Compasses point to the nearest Jungle Temple".
- Open question for the owner: once you reach the structure, the totem keeps pointing at it (like an explorer map). "Point to the next one" would need a "found" memory; not built.
- **Prior art:** Explorer's Compass (CC-BY-NC-SA, no 26.3), OmniCompass (MIT, 26.1.2), Luna's Structure Compass (MIT, 1.21.8): structure compasses picking from a GUI list and running a locate. Read for ideas only; none has a 26.3 build to depend on (B9), and our block-choosing + no-chunk-load stepped search is different. Hand-rolled because no library builds for 26.3.

### How they're found (agent's proposal, implemented in `totems/TotemsLoot`)

One extra pool per table and totem, additive, Fabric-replaced tables skipped (same as Warding). Chests only (archaeology tables give one item, so an extra pool there would break brushing).

| Totem | Chest (chance per chest) |
|---|---|
| Keeping | ancient city 6 %, end city treasure 8 %, stronghold library 10 %, woodland mansion 8 % |
| Gathering | abandoned mineshaft 6 %, simple dungeon 8 %, pillager outpost 12 % |
| Fortification | desert pyramid 15 % (the TNT trap), bastion "other" 8 %, stronghold crossing 8 % |
| Growth | jungle temple 20 %, shipwreck supply 6 %, igloo 15 % |
| Seeking | shipwreck map 12 %, buried treasure 10 %, big underwater ruin 8 %, village cartographer 10 % |

### Beacon payloads (Reworked Beacons optional, as before)

Four new payload classes, one per totem, all under the existing entrypoint `bubbas_reworked_beacons:payload` in `fabric.mod.json`: `compat/GatheringBeaconPayload`, `FortificationBeaconPayload`, `GrowthBeaconPayload`, `SeekingBeaconPayload`. Power 1, 1 layer needed. Only these classes touch the beacons API. The areas they set live in `totems/TotemAreas` (Fortification, Growth; same lapse-unless-refreshed idea as `Wards`), which doesn't depend on Reworked Beacons. Keeping has no payload.

### Not built / left for later

- Seeking "next structure" (see above); a GUI picker instead of signature blocks if the owner prefers (both in Open, below).


### Milestone test objectives (new totems)

Core (`BubbasTotemsArtifactsCoreGameTest`, 16 in all; these five bodies in `TotemsGameTest`):
- Keeping: with keep_inventory off a player with the totem in the backpack dies: no item or XP drops, one totem used, tagged; respawned with the dirt and 5 levels, tag gone; the next death drops.
- Gathering: an armor stand holding it pulls a dirt item from 6 blocks; an item 12 blocks away stays.
- Fortification: a TNT-type blast inside a 5x3x5 dirt cube next to a holder breaks nothing.
- Growth: 40 boosted ticks in the held box grow a 3x3 wheat patch; one outside the box doesn't grow.
- Seeking: mossy cobblestone chooses jungle temple, stone chooses nothing; a village search finishes and the totem points at the result (spins in the test world, which has no structures).

Milestone (`TotemsGameTest`): keep_inventory on doesn't use the totem; own fresh throws and never-pickup items aren't pulled, others' throws are; the same blast without a holder breaks blocks (control); only blocks within 8 of the holder are spared; a real held Growth totem (armor stand, random_tick_speed 100, 400 ticks) beats vanilla; the stepped search matches vanilla /locate for every type that exists (none in the server test world; the client test checks it in a world with villages); the ring walk covers every cell once; each loot table gives its totem at about its chance (300 rolls); all are single-stack.

Milestone (`TotemsBeaconGameTest`, real only with `-PwithBeacons`): Gathering fills the chest two below the beacon and does nothing without one; Fortification's area survives a blast; Growth's 4-layer area has strength 4 and reaches 45 blocks; Seeking points a held compass at the search result, keeps it named "Compass", and it turns plain after its time; each payload accepts only its own totem.

Client part `totems` (`TotemsClientGameTest`): the five (and Warding) in the hotbar at 16x and 32x; Gathering before/pulling/after; Seeking held facing the found village (needle up) and turned 90 degrees (needle to the side), 16x and 32x; the seek result checked against vanilla /locate in that world.


## Totems of the Beginning and the End (built 2026-11-22)

Owner-approved design, handed over by the orchestrator. Player-facing description: `player-docs/Projects/Bubba's Totems & Artifacts/Totem of the Beginning.md` and `Totem of the End.md`. The cairn side (ripple, offerings, beacon nexus) lives in Reworked Mobs: `specs/endermen.md`, "Cairn gifts, the ripple and the nexus".

- **Totem of the Beginning:** used up on use; the ground within 24 blocks ripples back, outward, to how it generated (Reworked Mobs' target world), never harming builds. Without Reworked Mobs or outside the overworld it does nothing and isn't used up. In a beacon the area draws caretaker endermen like a cairn nexus. Found only as a once-ever offering at some complete cairns. Never crafted.
- **Totem of the End:** held (either hand), endermen treat you as kin (eye contact, and Reworked Mobs' cairn anger). Used, it teleports to the aimed block within range by the owner's landing rules (top face: stand on it; side face: on top if the 2 above are open, else down to the nearest safe footing; bottom face: down), with a cooldown sweep; no hit, no cooldown. Found only in End city chests (owner, 2026-11-22: no cairn reward).

### As built

**Totems & Artifacts** (`mods/bubbas_totems_artifacts`)
- Items `bubbas_totems_artifacts:totem_of_the_beginning` (`beginning/TotemOfTheBeginningItem`) and `:totem_of_the_end` (`end/TotemOfTheEndItem`), `stacksTo(1)`, `Rarity.RARE`, in the creative tab after Seeking. No new blocks (B15). Statues for both came later (Totem statue, below).
- `end/EndTeleport`: aim = `level.clip` from the eyes, `ClipContext.Block.COLLIDER` (aims through grass and flowers) and `Fluid.NONE` (through water). Landing uses vanilla's `noCollision` for the entity's box (as `randomTeleport` does). Teleport like the ender pearl: `stopRiding`, `ServerPlayer.teleport(TeleportTransition(..., Vec3.ZERO, Relative.ROTATION, DO_NOTHING))`, `resetFallDistance`, `resetCurrentImpulseContext`, `GameEvent.TELEPORT`; sound `ENDERMAN_TELEPORT` and `PORTAL` particles at both ends; cooldown via `ItemCooldowns.addCooldown` (B19).
- `mixin/EndermanKinMixin`: HEAD of `Enderman.isBeingStaredBy` returns false when the player holds the totem in either hand (vanilla's own disguise check is the carved pumpkin on the head; this is the same check for the hand).
- `compat/BeginningBeaconPayload` (entrypoint `bubbas_reworked_beacons:payload`): accepts the totem only when Reworked Mobs is installed; power 1, 1 layer; refreshes the area each pulse for `AREA_LIFETIME_TICKS` (100).
- `compat/SleeperHook` gained `restoreAround`, `drawEndermen`, `setUpEndermen(offering, kin)`; `ReworkedMobsSleepers` calls the new api methods and ignores a `LinkageError` from an older Reworked Mobs. `BubbasTotemsArtifacts.onInitialize` calls `setUpEndermen` once.
- Loot: `TotemsTuning.END_LOOT` joins `TotemsLoot` (same additive-pool pattern).
- Textures: `tools/totems-textures.py` (two new grids), rows in `docs/art-todo.md`.

#### Numbers (builder's starting values, owner to playtest)

| | Value | Where |
|---|---|---|
| Beginning ripple radius | 24 blocks, a cylinder 24 up and 24 down from your feet | `TotemsTuning.BEGINNING_RADIUS` |
| Beginning ripple speed | 1 block every 2 ticks (about 2.4 s to the edge) | `TotemsTuning.BEGINNING_TICKS_PER_BLOCK` |
| Build protection | a column is skipped if it or any column within 2 holds a built/placed block | `EndermanTuning.RIPPLE_PROTECT_RADIUS` |
| Waiting for generated terrain | a column not worked out yet is retried for up to 30 s | `EndermanTuning.RIPPLE_PENDING_TICKS` |
| End teleport range | 32 blocks | `TotemsTuning.END_RANGE` |
| End cooldown | 80 ticks (4 s) | `TotemsTuning.END_COOLDOWN_TICKS` |
| End in End city treasure | 5 % per chest | `TotemsTuning.END_LOOT` |
| Cairns that leave an offering | 0.15 (picked from each site's seed; owner nerfed from 1 in 2, 2026-11-22) | `EndermanTuning.OFFERING_SHARE` |
| Offering timing | checked every 5 s while a player is within 96; 1 in 12 per check (about a minute) once complete with one of its endermen within 16 of the centre | `OFFERING_CHECK_TICKS`, `OFFERING_ONE_IN`, `OFFERING_WITNESS_RANGE` |
| Offering spot | 2 blocks outside the cairn's footprint, side picked by the seed | `OFFERING_GAP` |
| Beacon area draw | 35 % per 5 s, up to 6 caretakers in the area (+8 round it), never within 16 of a player | `NEXUS_SPAWN_CHANCE`, `NEXUS_CAP`, `NEXUS_MARGIN`, `NEXUS_PLAYER_DISTANCE` |

#### Builder's choices (owner may change)

1. **Ripple radius 24 and speed 2 ticks a block** (about 2.4 s). A cylinder 24 up and down, not a sphere, so each column is done in one go (fills bottom up, so sand and gravel always land on a floor).
2. **Build protection is per column, radius 2, the whole cylinder's height.** Safe rather than thorough: a torch at the bottom of a mine keeps the crater above it too. A mine with rails or torches is only partly filled (pockets round each torch).
3. **Cobblestone, mossy cobblestone, cobbled deepslate, smooth stone and dirt paths count as built** (terrain generation never makes them on its own), so cobblestone houses are safe. Dirt and plain stone builds are *not* recognised: a pure dirt hut standing on the ground can be taken away like a dirt pillar. Say if that should change.
4. **Geodes' insides are left alone** (their shell protects the column), and so are lava pools.
5. **Water and lava are never filled over or put back**; a crater that flooded stays a pond.
6. **A strip mine you're standing in fills round you but not where you stand**, so you can end up in a 1x2 pocket of stone. No block goes where a mob or player is.
7. **The top-layer swap only happens away from water** (like the endermen), so natural sand and gravel by rivers and lakes stay.
8. **The Beginning is used up only when the ripple starts**; the client shows the use animation only in the overworld with Reworked Mobs installed. No message when it does nothing.
9. **Beacon: the totem isn't accepted without Reworked Mobs** (there are no caretakers to draw), rather than taking it and doing nothing.
10. **Beacon area spawns** follow the cairn rules (night, game rule, Peaceful...) but keep only 16 blocks from players, not 24 (beacon areas are often small).
11. **Offerings: 0.15 of cairns (owner, 2026-11-22; was 1 in 2), about a minute after completion with one of its endermen present.** The offering appears with an enderman's teleport sound and portal particles; no enderman walks over and sets it down.
12. ~~Completion gift~~ **removed (owner, 2026-11-22): no reward for completing a cairn.** The Totem of the End is found only in End cities.
13. **Totem of the End landing:** top face with no room or unsafe → nothing (no fallback downward). Aims through grass, flowers and water (lands on a lake bed, as an ender pearl can). Fire, campfire, powder snow, magma, cactus, berry bush, wither rose and lava count as unsafe. Range 32, cooldown 4 s.
14. **Kin covers eye contact for every enderman** (vanilla, Nether and End too), and Reworked Mobs' cairn anger. Hitting an enderman still makes that one fight you (vanilla); the others near a cairn stay calm.
15. **Rarity RARE** (aqua name), one step above the other totems' UNCOMMON, since one is a once-ever cairn gift and the other is single-use terrain repair.
16. **End city chance 5 %** (the Totem of Keeping is 8 % in the same table).
17. ~~No totem statue~~ Statues added for both (2026-11-22, see Totem statue).

#### Tests and milestone objectives

- Reworked Mobs core `ReworkedMobsCoreGameTest.endermanRippleRestoresGroundButNotBuilds` (body in `EndermanTotemsGameTest`): a hole fills with grass on stone, a dirt pillar goes, sand on the top layer turns to grass, a 3x3 pit with a plank floor keeps its air.
- Reworked Mobs milestone `EndermanTotemsGameTest`: `offeringLaidOncePerCairn` (laid once on display; a player takes it with an empty hand; never laid again, also after it's taken).
- Totems core `BubbasTotemsArtifactsCoreGameTest.endLandingRules` (body in `BeginningEndGameTest`): top face, side with room, side without room → floor in front, slab top, fire on top → nowhere, bottom face → floor under it, miss → nowhere.
- Totems milestone `BeginningEndGameTest`: `endTeleportsWhereAimedWithCooldown` (real use: unsafe aim → FAIL, no cooldown, no move; aim at a block 10 away → there, cooldown on, not used up), `endHeldMakesEyeContactHarmless` (vanilla `isBeingStaredBy`: true without, false with the totem in the off hand), `beginningUsedOnlyWithReworkedMobs` (used up with `-PwithMobs`, PASS and kept without), `endFoundInEndCitiesAndSingleStack`.
- Reworked Mobs client part `endermen` (real default-preset world, seed 20261116): a ripple dry run (`Ripples.dryRun`, changes nothing) at spawn and 40 blocks east of each of the three nearest cairns, radius 24: **0 changes on untouched ground at all four**. Control: after digging a 3x3x3 hole and stacking a 3-high dirt pillar at spawn it finds exactly 30 (9 air→grass, 18 air→dirt, 3 dirt→air). Printed as `[endermen] ripple dry run ...`.
- Client part `totems`: both in the hotbar and held in first person, 16x and 32x (`totems-hotbar`, `totems-held-beginning`, `totems-held-end`, and their `-32x`).

Milestone test objectives not yet written as tests:
- A beacon with the Totem of the Beginning draws caretakers into its area at night and they repair a hole there (needs `-PwithMobs -PwithBeacons`).
- With Reworked Mobs, a player holding the Totem of the End breaks a cairn block and no enderman turns on them.

## As built (0.1.0)

### What moved from Reworked Mobs

Behavior unchanged unless noted below.

| Piece | Was (`bubbas_reworked_mobs`) | Now (`bubbas_totems_artifacts`) |
|---|---|---|
| Grave Talisman | `:grave_talisman`, `undead/GraveTalisman`, `client/undead/GraveTalismanGlow`, property `bubbas_reworked_mobs:grave_talisman_glow` | `:grave_talisman`, `talisman/GraveTalisman`, `client/talisman/GraveTalismanGlow`, property `bubbas_totems_artifacts:grave_talisman_glow` |
| Totem of Warding | `:totem_of_warding`, `warding/*` (`TotemOfWardingItem`, `Wards`, `WardBarrier`, `WardFleeGoal`, `WardingLoot`, `WardingTuning`) | same names under `warding/` |
| Ward mixins | `MobTickMixin`, `PhantomSpawnerMixin`, `Walk/SwimNodeEvaluatorMixin`, `MobAccessor`, ward half of `SpawnStateMixin` | same names (prefix `bta$`), plus `WardSpawnStateMixin` (the ward check only); Reworked Mobs' `SpawnStateMixin` keeps only the monster-cap cut |
| Beacon payload | `compat/WardingBeaconPayload` | same class here (entrypoint `bubbas_reworked_beacons:payload`); lang keys `beacon.bubbas_totems_artifacts.ward*` |
| Assets | Reworked Mobs assets and 32x pack | this mod's assets and its own 32x pack `bubbas_totems_artifacts_32x` |
| Texture scripts | `tools/warding-textures.py`, Reworked Mobs `tools-local/make_talisman_textures.py` | same script (writes here) / moved to `mods/bubbas_totems_artifacts/tools-local/` |
| Creative tab | Reworked Mobs tab | its own tab "Bubba's Totems & Artifacts" |

Reworked Mobs no longer depends on Reworked Beacons at all (its `compileOnly` and `suggests` are gone; it now `suggests` Totems & Artifacts).

### Independence (same pattern as Reworked Beacons)

`fabric.mod.json` `depends` on Fabric API only and `suggests` `bubbas_reworked_mobs` and `bubbas_reworked_beacons`; the build has `compileOnly` against both. Neither mod requires the other. Owner's principle (2026-11-16): "mods should work standalone, but that doesn't mean fallbacks everywhere": each mod runs alone, but removing a mod doesn't need a stand-in for its items.

**The sleeper hook (Reworked Mobs `io.github.bubba8587.bubbas_reworked_mobs.api.Sleepers`; stable: add, don't change):**

- `int calm(ServerLevel, Vec3 centre, double radius, float points)`: takes points off sleepers' disturbance meters in range (wake is 100). The held totem's pulse.
- `void keepAsleep(ServerLevel, Object source, AABB area, int lifetimeTicks)` / `void release(ServerLevel, Object source)`: sleepers in the area never wake from noise; each source's area lapses unless refreshed. A full-pyramid ward. Backed by Reworked Mobs' `undead/sleeper/SleepAreas`.
- `boolean isKeptAsleep(Level, BlockPos)`, `boolean isSleeper(Level, BlockPos)`, `double nearestSqr(Level, Vec3, double range)` (client or server; what the talisman senses).

**The endermen hook (Reworked Mobs `api.Endermen`; stable: add, don't change):** `keepOut(ServerLevel, Object source, AABB area, int lifetimeTicks)` / `release(ServerLevel, Object source)` / `isKeptOut(Level, BlockPos)`. Every ward also keeps Reworked Mobs' endermen (their jobs and cairn spawning) out, through `SleeperHook.keepEndermenOut` / `releaseEndermen`.

This mod reaches it only through `compat/SleeperHook`, picked once by `FabricLoader.isModLoaded("bubbas_reworked_mobs")`; only `compat/ReworkedMobsSleepers` names Reworked Mobs' classes, and it is created only when that mod is loaded. Without Reworked Mobs the talisman senses `#minecraft:undead` mobs only, and the totem still scares monsters, stops natural spawns in a ward, keeps monsters and happy ghasts out and pushes flyers out; it just has no sleepers to calm or keep asleep. On a beacon screen the "sleeping undead stay asleep" line shows only when Reworked Mobs is installed *(agent's choice: without it the line describes nothing)*.

### Saved worlds (old item ids)

`ModItems` registers a Fabric registry alias per item (`BuiltInRegistries.ITEM.addAlias(old, new)`): a lookup of `bubbas_reworked_mobs:grave_talisman` / `:totem_of_warding` that isn't registered falls back to the new item, so old stacks (chests, inventories, item frames, commands, the test world) load as the new items and are saved under the new id. Chosen because vanilla has no item-id alias and a DataFixer is heavier and only runs on version bumps. The alias lives here, so it works with or without Reworked Mobs. Without this mod installed the old stacks have no item and Minecraft drops them as unknown (vanilla behavior for a removed item); there is deliberately no fallback in Reworked Mobs (owner, 2026-11-16: fine as is). Players updating Reworked Mobs should install this mod alongside.

### Grave Talisman

Owner's idea, deliberately loose: "a talisman that faintly glows near undead." Item `bubbas_totems_artifacts:grave_talisman`; working name, the owner may rename it.

- **Getting one (owner, 2026-11-14): found only, no recipe.** With Reworked Mobs: family vault chests (`bubbas_reworked_mobs:chests/crypt_family`) 15 % and noble crypt chests (`chests/crypt_noble`) 25 % (`TotemsTuning.TALISMAN_LOOT`, keyed by table id, no dependency). Also in the creative tab. A rare find on sleepers is an unbuilt idea.
- **What it does:** glows when undead are near, brighter the closer; nothing else. Undead = Reworked Mobs' sleeper blocks (through the hook) and any entity in `#minecraft:undead`.
- **Levels (`talisman/GraveTalisman`):** nearest undead within 40 blocks of the holder's eyes: faint; within 24: bright; within 12: brightest; none: off. (Was 16/8/4; owner: too short. At sprint speed, ~5.6 blocks/s, faint gives about 7 s of warning.)
- **Where it shows:** in either hand and as the icon in hotbar and inventory, for the local player's own talismans only. Held by others, dropped, in item frames: dark.
- **How (client only, no packets):** `client/talisman/GraveTalismanGlow` runs every 5 ticks while the player carries a talisman: sleepers via the hook (loaded chunks only) plus one entity box query for `#minecraft:undead`, keeping the nearest. Nothing runs without a talisman. The search is on plain `Level`, so a server game test checks it.
- **Visual:** `items/grave_talisman.json` is a `range_dispatch` on the numeric item model property `bubbas_totems_artifacts:grave_talisman_glow` (registered into `RangeSelectItemModelProperties.ID_MAPPER` via Fabric's transitive access wideners). Levels 1-3 are a `composite` of the flat item and a gem cuboid (`models/item/grave_talisman_glow_<n>.json`, `light_emission` 5 / 10 / 15), because a flat `item/generated` layer can't carry `light_emission`. All share `item/grave_talisman_held`, which raises the first-person pose so the gem is on screen. No particle or sound.
- **Art:** placeholder textures from `tools-local/make_talisman_textures.py` (16x and 32x); to be replaced by hand-drawn art.
- **Prior art:** nothing on Modrinth senses undead with an item. Closest: Slime Amulet (MIT, glows in slime chunks), Hostile Awareness, Player Radar. The idea is Sting from The Hobbit.

### Totem of Warding

Owner's goal (2026-11-15): mob-free zones without spamming torches. Owner decisions: `docs/research/area-gems-and-warding.md` ("Owner decisions", 2026-11-16); the beacon side: `specs/reworked-beacons.md`. Numbers in `warding/WardingTuning` (starting values, owner to tune); *(agent's choice)* marks the builder's picks. Follows B17 (vanilla flee behavior, the cooldown sweep, the spawner light rule), B18 (mobs holding a totem pulse too) and B19 (the pulse rhythm shows as the cooldown sweep).

**The item** (`bubbas_totems_artifacts:totem_of_warding`, `warding/TotemOfWardingItem`): an artifact like the Totem of Undying (uncommon, stacks to 1), found only, never a gem. Placeholder art from `tools/warding-textures.py` (16x and 32x).

- **Found (owner: a mix of places, more common than the Totem of Undying).** One extra pool per table via `LootTableEvents.MODIFY` (`WardingLoot`; Fabric-replaced tables skipped): evoker 5 % (owner, 2026-11-16: a rare bonus), woodland mansion chest 20 %, ancient city chest 12 %, trial chambers vault 8 %, ominous vault 15 % *(chest rates: agent's choice)*, and Reworked Mobs' noble crypt chests 12 % (keyed by table id, so no dependency).
- **Held (owner):** in either hand (`Item.inventoryTick`, MAINHAND or OFFHAND) it pulses every 10 s (`PULSE_INTERVAL_TICKS` 200), the first as soon as it is held off cooldown. Players get vanilla's cooldown sweep (`ItemCooldowns.addCooldown`); other holders use a weak map of next pulse times. Visuals: soul-fire ring at shin height, rising souls, low amethyst chime *(agent's choice)*. Held, it doesn't stop spawns (owner).
- **Pulse:** every `Enemy` within 8 blocks (`PULSE_RADIUS`) flees the holder for 100 ticks (`FLEE_TICKS`), keeping away while within 12 (`FLEE_KEEP_AWAY`), speeds 1.0 / 1.2 within 7, like a creeper from a cat *(keep-away: agent's choice)*. Goal-driven `PathfinderMob`s get `WardFleeGoal` (priority 0, MOVE, added lazily; picks copy `AvoidEntityGoal`, which isn't reused because its targeting skips creative players and non-player holders). Brain mobs with `AVOID_TARGET` (piglins, hoglins) get it for 100 ticks. Not affected (limitation): slimes, ghasts, phantoms, vexes, the warden, breezes. Sleepers within 8 blocks lose 40 points (`PULSE_CALM_POINTS`, via `Sleepers.calm`) *(agent's choice)*.
- **Mob holders:** a mob holding a totem pulses on the same rhythm (B18) *(agent's choice)*; a woken undead can pick one up.

**Wards (a totem in a Reworked Beacons beacon; owner).** `compat/WardingBeaconPayload` is the only class touching the beacons API (`-PwithBeacons` puts the beacons jar on the game test classpath). It accepts the totem, power 1, required layers 1; `apply` = `Wards.put(level, pos, area, levels >= 4, 100)`; screen lines "No monsters spawn or enter" and "Sleeping undead stay asleep" (4 layers) or, greyed, "4 layers: sleeping undead stay asleep".

- Any pyramid: **no natural monster spawns** inside (`WardSpawnStateMixin`; `PhantomSpawnerMixin` wraps `isValidEmptySpawnBlock`, so phantoms count *(agent's choice)*). Spawners, trial spawners, raids, bosses and commands spawn as normal (owner).
- Any pyramid: **monsters don't walk in.** `WalkNodeEvaluatorMixin` / `SwimNodeEvaluatorMixin` (HEAD of `getPathTypeOfMob`) return BLOCKED for a kept-out mob's node inside a ward it isn't in. Every 20 ticks (`WARD_SWEEP_TICKS`) goal-driven monsters inside walk out through the nearest side; no despawn or damage *(agent's choice)*. Ranged monsters outside can still shoot in (limitation).
- Any pyramid: **an invisible wall for every kept-out mob** (owner, 2026-11-16: every monster, including non-pathing ones, and happy ghasts wild or ridden). "Kept out" = `Enemy` or a happy ghast (`WardBarrier.keptOut`). `WardBarrier` runs at the TAIL of each such mob's `LivingEntity.tick` (`MobTickMixin`, server) within 3 blocks of a ward; one map check per mob tick when no ward exists. No damage; mobs slide along the edge.
  - At the edge moving in: inward motion dropped. Crossed in since last tick: put back on the edge (the barrier keeps its own "last seen outside" per mob, weak map; the mob's old-position fields weren't reliable).
  - Already inside: 0.06 blocks/tick outward through the nearest side (top or bottom for no-gravity mobs); walkers also walk out.
  - Steered vehicle (a ridden happy ghast): moved out by min(depth + 0.6, 1) blocks a tick with `ClientboundMoveVehiclePacket` to the rider; small rubber-band on lag (limitation). Passengers are skipped.
- 4 layers: **sleepers inside stay asleep** (`Sleepers.keepAsleep` for the ward's area).
- `Wards.put(level, key, AABB, keepsSleepersAsleep, lifetimeTicks)` lapses unless refreshed: refreshed every 80-tick beacon pulse for 100 ticks (`WARD_DEFAULT_LIFETIME_TICKS`), so a ward ends about 5 s after its beacon stops. One map lookup when no ward exists.

### Statues give their totem's effect

*Owner, 2026-11-22. In progress on `totems-2`, not in develop.*

- A placed totem statue gives its totem's area effect in a **very small radius** (a few blocks), weaker than the same totem in a beacon: a Warding statue keeps a doorway clear, a Growth statue a garden bed, a Gathering statue pulls drops into a chest beside it, and so on. Builder proposes each statue's version and radius. Totems with no area form (Keeping, the End's teleport) give nothing as statues.

### Totem statue (block, B15 exception)

- `bubbas_totems_artifacts:totem_statue` ("Totem Statue"). **B15 reason (owner-approved, 2026-11-16):** one shared block for every totem, so a found totem can be set down as decoration.
- **Placing (owner-approved, 2026-11-16; every totem since 2026-11-22):** sneak and use any totem on a block (sneaking, so holding a totem for its pulse never places it by accident; without sneaking the click does nothing). The statue faces the player and the totem is used up (creative keeps it). The Grave Talisman is not a totem and has no statue.
- **Inert in develop:** no pulse, ward or beacon effect; no block entity, no ticking (tiny-radius effects are in progress, above).
- **Breaking** (any tool, pickaxe fastest, strength 1) drops the totem back; pistons pop it off as its totem. Shape: 10x10 footprint, 14 high (plinth and column).
- **Which totem:** `statue/TotemType`: WARDING (first, so it's the default and statues saved before the `totem` property load as warding), KEEPING, GATHERING, FORTIFICATION, GROWTH, SEEKING, BEGINNING, END, with `forItem(Item)`. `statue/StatueTotemItem` is an `Item` whose `useOn` places the statue when sneaking (PASS otherwise, so `use()` still runs); Keeping and Fortification are `StatueTotemItem`s, Gathering, Growth, the Beginning and the End extend it, Seeking checks sneaking first, Warding keeps its own `useOn`. **Sneak-use always places the statue** (builder's choice): a sneaking Totem of the End doesn't teleport, the Beginning doesn't ripple, Seeking doesn't pick a structure.
- **Loot (26.3 format):** one pool, an `alternatives` entry with one item per totem, each with `"condition": {"type": "minecraft:all_of", "terms": [survives_explosion, match_block {state: {totem}}]}`. The pre-26.x `conditions` / `block_state_property` keys are silently ignored by 26.3 (they made every statue drop all eight totems).
- **Art (initial, 2026-11-22; owner will redesign):** generated by `tools-local/make_statue_textures.py` (`main()` Warding's hooded figure, unchanged; `write_others()` the seven new ones). Builder's choices: one generic figure for the seven (body, arms, head, chest emblem) on Warding's plinth, told apart by per-totem tops and emblems in each item's palette: Fortification's helm brim, Growth's leaf crown and blossom, Seeking's gold band, the Beginning's grass cap, the End's purpur crown with four spikes; Gathering raises its hands, Keeping hugs a deeper chest. Glowing parts: the Beginning's and the End's eyes (light 12), Fortification's visor (10). 16x and 32x (`pixelart.render32`); rows in `docs/art-todo.md`.
- **Tests:** core `endStatuePlacesWithoutTeleportingAndDropsItsTotem`; milestone `TotemStatueGameTest.everyStatueDropsItsTotemAndOldOnesLoadAsWarding`; client part `statues` (row, close pairs, night, each at 32x too).
### Tooling

`tools/install-to-modrinth.ps1`, `tools/build-testworld.py` (`MODS`) and `mods/testworldshots` (`modsUnderTest`) include this mod; the test world builder names `bubbas_totems_artifacts:totem_of_warding` (the saved world's old-id totems load through the alias). No mod icon yet; it must be person-made (B14).

## Open

Owner decisions still pending (built as described above until decided):

- **Mob holders:** should mobs holding a Totem of Warding pulse, and may woken undead pick one up? (built as yes, agent's choice)
- **Names:** the Grave Talisman's name; the Totem of Growth's name (owner wanted a verb-like word).
- **Seeking:** signature blocks (built) or a picker screen; keep pointing at a found structure (built) or "point to the next one".
- **Builder's calls to review:** Keeping counts anywhere in the inventory and keeps XP levels; Gathering's "under the beacon" chest rule (being replaced by the ring); the totem loot tables and chances; the Beginning/End builder's choices 1-16 (ripple size, what counts as built, landing rules, rarity, End city 5 %); trapped monsters in a ward fight rather than cower.
- **Art:** person-made statue designs and mod icon (B14); the Gathering texture reads like gingerbread (`docs/art-todo.md`).
- **Numbers to playtest:** every value in `TotemsTuning` and `WardingTuning`.

## Milestone test objectives

Test-lite (`specs/quality.md`, Test suites). Run: `./gradlew :bubbas_totems_artifacts:build` (standalone core); `:bubbas_totems_artifacts:runGameTest -PwithMobs -PwithBeacons -PmilestoneTests` for every test for real (the sleeper tests pass trivially without `-PwithMobs`, the beacon test without `-PwithBeacons`).

- Core (`BubbasTotemsArtifactsCoreGameTest`, 16: the nine below, the five new totems' (above), `endLandingRules` and `endStatuePlacesWithoutTeleportingAndDropsItsTotem`): old Reworked Mobs ids load as these items (registry lookup and the item stack codec, re-saved under the new id); sneaking places a warding statue facing the player and breaking it drops the totem (not sneaking places nothing); the talisman senses undead mobs at 29/17/11 blocks; it senses sleepers; a held totem pulses onto the cooldown and scares a zombie away; the pulse calms sleepers; a ward stops spawns and sends monsters out; a full ward keeps sleepers asleep and releases them on removal; a ward pushes back mobs that don't path.
- `GraveTalismanGameTest`: the glow-level table; undead mobs and sleepers sensed at each level.
- `WardingGameTest`: the core warding checks plus the off-hand totem pulses and a backpack one doesn't; a zombie can't path into a ward; a ridden happy ghast is put back outside; mansion chests (about 20 %) and evokers (about 5 %) give a totem.
- `WardingBeaconGameTest`: a totem beacon wards its range (1 layer: spawns only, 20 blocks; 4 layers: 50 blocks and sleepers kept asleep). Real only with `-PwithBeacons`.
- `TotemStatueGameTest`: sneak-place, facing, no place without sneaking, break drops the totem (core); the End statue places without teleporting (core); every statue drops its own totem and statues saved before the `totem` property load as Warding.
- Client (`ClientTests` names `talisman`, `warding`; screenshots): the talisman's glow at each distance in hand and hotbar (vanilla husk, skeleton, zombie, so it runs without Reworked Mobs); the Totem of Warding in the hotbar, its pulse ring and cooldown sweep in first and third person; the statue from three angles at 16x and 32x.

**Trapped inside a ward (bug fix 2026-11-16, owner: a skeleton in an enclosed beacon ward was "stuck running/jittering").** A monster inside a ward plans its walk out (`WardFleeGoal`); when the walk can't end outside the ward (an enclosed room) it counts as trapped for 10 s (`TRAPPED_RECHECK_TICKS`): it stops trying to leave, the ward's outward push skips it (it only pressed it into the wall), and it behaves normally (fights), then looks for a way out again. Milestone test `WardingGameTest.trappedInAnEnclosedWardItStillFights`. Builder's choice the owner may change: trapped monsters fight as normal rather than cower.
