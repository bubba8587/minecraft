# Totems & Artifacts: the five new totems (builder report, 2026-11-16)

For the orchestrator / docs agent to merge into `specs/totems-artifacts.md` (replace "being built" in the "What lives here" table and add an "As built" section per totem) and the player docs page. No tree leaves made. Governing leaf: C6. Principles touched: B15 (no new blocks), B16 (16x + 32x), B17, B18, B19.

## What was built

Five items, all `stacksTo(1)`, `Rarity.UNCOMMON`, found only (no recipes), in the mod's creative tab after the Totem of Warding:
`bubbas_totems_artifacts:totem_of_keeping`, `:totem_of_gathering`, `:totem_of_fortification`, `:totem_of_growth`, `:totem_of_seeking`.
No new blocks (B15). New ids, so no old-id alias. Numbers all in `totems/TotemsTuning` (starting values, owner to tune).
"Held" = main or off hand of any living thing, players and mobs alike (B18; `totems/Held`), as with Warding. None of the five has a rhythm, so no cooldown sweep is needed (B19).

### Totem of Keeping (`keeping/Keeping`, `mixin/PlayerKeepingMixin`)

- **Where it counts (agent's choice): anywhere in the player's inventory** (hotbar, backpack, off hand), not only in a hand. Reason: it's insurance, and deaths come unannounced; the Totem of Undying's "must be in hand" would make it a juggling act.
- On death with `keep_inventory` off: one Keeping totem is used up and **the death goes exactly as under vanilla's keep_inventory (B17)**: whole inventory kept, **XP levels kept** (agent's choice, because that is what keep_inventory does), score kept, nothing drops, no XP orbs, curse-of-vanishing items stay. With `keep_inventory` on the totem isn't used. Spectators: n/a.
- How: at the start of the death drops (`Player.dropEquipment`, HEAD) the totem is taken and the dead player gets the entity tag `bubbas_totems_artifacts.kept` (saved, so logging out on the death screen still works); the tag stops the drop and the XP orbs (`getBaseExperienceReward`); on respawn Fabric's `ServerPlayerEvents.COPY_FROM` copies inventory, XP and score the way vanilla's `transferInventoryXpAndScore` does; `AFTER_RESPAWN` clears the tag (vanilla copies entity tags to the new player).
- Effect: at the place of death everyone sees the Totem of Undying's particle burst and hears its sound; on respawn the player gets the Totem of Undying animation with the **Totem of Keeping** filling the screen (custom S2C payload `bubbas_totems_artifacts:kept` → `LocalPlayer.displayItemActivation`), the totem sound, and the action-bar line "Your Totem of Keeping held on to your things".
- Mobs: not applicable (mobs don't respawn).
- No beacon use (per design).
- **Prior art / overlap:** keep-inventory mods (Keep Inventory Always, Nice Keep Inventory, Keep Inventory Orbs; MIT/GPL), soulbound enchantments, and grave mods (Universal Graves, Gravestones, You're in Grave Danger). Graves are the closest in purpose: with a grave mod installed, a kept death simply has nothing to put in the grave (they compose). None offers a single-use found totem; no library needed (two small injections, hand-rolled because no library does it, B9).

### Totem of Gathering (`gathering/Gathering`, `TotemOfGatheringItem`, `compat/GatheringBeaconPayload`)

- **Held:** every tick, dropped items within **8 blocks** (`GATHER_RADIUS`) fly to the holder at **0.3 blocks/tick** (`GATHER_SPEED`; about 1.5 s from 8 blocks), aimed at the holder's middle like XP orbs, then are picked up by the usual rules. At most 64 items per tick. Skipped: items the holder itself threw that are still on their pickup delay (so Q-dropping works), and items that can never be picked up (pickup delay 32767, displays/props). Someone else's fresh throw is pulled.
  - *B17 note:* vanilla's own XP-orb pull formula was tried first: on the ground item friction eats it, and items 6 blocks out moved less than a block in 5 s (seen in the client test). So the speed is a steady pull instead.
  - **XP:** vanilla XP orbs already fly to any player within 8 blocks, so the held totem leaves orbs to vanilla (no double pull).
- **In a beacon (each 80-tick pulse, any pyramid):** dropped items anywhere in the beacon's area go into **the nearest chest or barrel "under" the beacon**, inserted as a hopper would (double chests count whole; partial stacks if full). **"Under" (agent's definition):** the 3x3 column directly below the beacon, from 1 block below it down through the pyramid to 6 blocks below its bottom layer (`GATHER_CHEST_DEPTH_BELOW_PYRAMID`); nearest first (straight below, then sides, then corners, layer by layer). Chests (incl. trapped) and barrels only. **XP orbs** in the area are gathered **onto the top of the beacon** (agent's choice: XP can't go in a chest), where they wait to be collected (they still despawn after 5 minutes like any orb). **No chest or barrel: nothing at all happens** (items and XP stay put). Bounded: 256 items and 256 orbs per pulse. Effects: reverse-portal puff where an item vanished, a soft pickup sound at the chest.
  - Screen lines: "Dropped items go into a chest or barrel below", "Experience gathers on the beacon".
- **Prior art:** item magnets (Simple Magnets ARR, magnetic GPL, Magnetic Enchantment AGPL, Sticky Fingers MIT, davdeo's Item Magnet MIT). None ties into a beacon/chest; hand-rolled (tiny; no library does it).

### Totem of Fortification (`fortification/Fortification`, `mixin/ServerExplosionMixin`, `compat/FortificationBeaconPayload`)

- **Held:** explosions break **no block within 8 blocks** (`FORTIFY_RADIUS`) of anyone holding one. **In a beacon (any pyramid):** no explosion breaks blocks anywhere in the beacon's area (area refreshed each pulse for 100 ticks, so it lapses ~5 s after the beacon stops).
- **Explosion block damage only:** the hook filters the explosion's own list of blocks to break, right where `ServerExplosion.explode` works it out, so the explosion still bangs, hurts and knocks back, blocks outside the protected zone still break, and explosion-lit fire (ghast fireballs, beds) doesn't start on protected blocks. The **mobGriefing rule is never touched** (owner). Wind charges (which only press buttons/open doors) are left alone. Cost: one entity box query per explosion; nothing when no holder or area is near.
- Feedback when it saved blocks: a dull shield-block clang and a few wax-on glints at the blast.
- Limitation: item frames, paintings and armor stands are entities and still get hurt by the blast (only blocks are protected).
- Screen line: "Explosions break no blocks".
- **Prior art:** no Modrinth hit for explosion-protection items on 26.3; anti-creeper-grief mods change the mobGriefing rule or explosion type, which the owner ruled out. Hand-rolled (one injection).

### Totem of Growth (working name; `growth/Growth`, `TotemOfGrowthItem`, `compat/GrowthBeaconPayload`)

- **What grows faster:** block tag `bubbas_totems_artifacts:growth_totem_boosts` (datapack-editable): `#minecraft:bee_growables` (crops, sweet berries, glow berry vines), `#minecraft:saplings`, sugar cane, cactus, bamboo (+ shoot), cocoa, nether wart, kelp, twisting/weeping vines, chorus flower.
- **How (B17):** extra **random ticks**, vanilla's own growth step, so light, farmland, room for a tree etc. stay vanilla's. Extra ticks are a multiple of vanilla's rate and follow the `random_tick_speed` rule; sampling is vanilla's (random positions per chunk section, skipping sections with nothing that ticks and chunks vanilla isn't ticking).
- **Held:** a box 8 blocks each way from the holder (17x17x17), **+2x vanilla's rate (grows about 3x as fast)**. Cost: a few hundred block lookups a tick at most.
- **In a beacon:** the beacon's whole area, **+1x per pyramid layer** (1 layer: 2x as fast ... 4 layers: 5x), every tick while the area lives; at most 2048 samples per area per tick (a full 4-layer sky beacon needs ~2000).
- A plant that grows from an extra tick puffs two bone-meal sparkles.
- Screen line: "Crops and saplings grow Nx as fast".
- **Prior art:** growth accelerators/twerk mods (Crop Growth Modifier, Twerk Crop Growth, Sprinklerz); nothing on 26.3 as an item/beacon; hand-rolled (no library).

### Totem of Seeking (`seeking/TotemOfSeekingItem`, `SeekingTypes`, `StructureSeeker`, `SeekingBeacon`, `mixin/CompassItemMixin`, `compat/SeekingBeaconPayload`)

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

## How they're found (agent's proposal, implemented in `totems/TotemsLoot`)

One extra pool per table and totem, additive, Fabric-replaced tables skipped (same as Warding). Chests only (archaeology tables give one item, so an extra pool there would break brushing).

| Totem | Chest (chance per chest) |
|---|---|
| Keeping | ancient city 6 %, end city treasure 8 %, stronghold library 10 %, woodland mansion 8 % |
| Gathering | abandoned mineshaft 6 %, simple dungeon 8 %, pillager outpost 12 % |
| Fortification | desert pyramid 15 % (the TNT trap), bastion "other" 8 %, stronghold crossing 8 % |
| Growth | jungle temple 20 %, shipwreck supply 6 %, igloo 15 % |
| Seeking | shipwreck map 12 %, buried treasure 10 %, big underwater ruin 8 %, village cartographer 10 % |

## Beacon payloads (Reworked Beacons optional, as before)

Four new payload classes, one per totem, all under the existing entrypoint `bubbas_reworked_beacons:payload` in `fabric.mod.json`: `compat/GatheringBeaconPayload`, `FortificationBeaconPayload`, `GrowthBeaconPayload`, `SeekingBeaconPayload`. Power 1, 1 layer needed. Only these classes touch the beacons API. The areas they set live in `totems/TotemAreas` (Fortification, Growth; same lapse-unless-refreshed idea as `Wards`), which doesn't depend on Reworked Beacons. Keeping has no payload.

## Not built / left for later

- **Totem statues for the new totems:** the shared statue block still supports Warding only (sneak-use of a new totem places nothing). Adding them = switch on `TotemType`'s `totem` property and five statue models; worth doing together with the owner's statue designs.
- Seeking "next structure" (see above); a GUI picker instead of signature blocks if the owner prefers.
- Name for Growth still open (owner).

## Player-facing text (for `player-docs/`)

- **Totem of Keeping:** Carry it anywhere in your inventory. If you die, it's used up and you keep everything: items, armor, experience. Like a Totem of Undying, but for your things. Found in ancient cities, end cities, stronghold libraries and woodland mansions.
- **Totem of Gathering:** Hold it and dropped items within 8 blocks fly to you. Put it in a reworked beacon with a chest or barrel under the beacon (below the pyramid), and every dropped item in the beacon's range is sent into that chest; experience gathers on top of the beacon. Found in mineshafts, dungeons and pillager outposts.
- **Totem of Fortification:** Hold it and explosions near you (8 blocks) don't break blocks; they still hurt. In a reworked beacon, nothing explodes blocks anywhere in its range. Found in desert pyramids, bastions and strongholds.
- **Totem of Growth:** Hold it and crops, saplings and other growing plants within 8 blocks grow about three times as fast. In a reworked beacon, plants across its range grow faster, more with a bigger pyramid (up to five times). Found in jungle temples, igloos and shipwrecks.
- **Totem of Seeking:** Use it on a structure's typical block (mossy cobblestone for a jungle temple, a hay bale for a village...; table above) and it points like a compass to the nearest one. In a reworked beacon, every compass held in its range points there too. Found in shipwreck map rooms, buried treasure, ocean ruins and cartographers' houses.

## Milestone test objectives (for the spec)

Core (`BubbasTotemsArtifactsCoreGameTest`, now 14; bodies in `TotemsGameTest`):
- Keeping: with keep_inventory off a player with the totem in the backpack dies: no item or XP drops, one totem used, tagged; respawned with the dirt and 5 levels, tag gone; the next death drops.
- Gathering: an armor stand holding it pulls a dirt item from 6 blocks; an item 12 blocks away stays.
- Fortification: a TNT-type blast inside a 5x3x5 dirt cube next to a holder breaks nothing.
- Growth: 40 boosted ticks in the held box grow a 3x3 wheat patch; one outside the box doesn't grow.
- Seeking: mossy cobblestone chooses jungle temple, stone chooses nothing; a village search finishes and the totem points at the result (spins in the test world, which has no structures).

Milestone (`TotemsGameTest`): keep_inventory on doesn't use the totem; own fresh throws and never-pickup items aren't pulled, others' throws are; the same blast without a holder breaks blocks (control); only blocks within 8 of the holder are spared; a real held Growth totem (armor stand, random_tick_speed 100, 400 ticks) beats vanilla; the stepped search matches vanilla /locate for every type that exists (none in the server test world; the client test checks it in a world with villages); the ring walk covers every cell once; each loot table gives its totem at about its chance (300 rolls); all are single-stack.

Milestone (`TotemsBeaconGameTest`, real only with `-PwithBeacons`): Gathering fills the chest two below the beacon and does nothing without one; Fortification's area survives a blast; Growth's 4-layer area has strength 4 and reaches 45 blocks; Seeking points a held compass at the search result, keeps it named "Compass", and it turns plain after its time; each payload accepts only its own totem.

Client part `totems` (`TotemsClientGameTest`): the five (and Warding) in the hotbar at 16x and 32x; Gathering before/pulling/after; Seeking held facing the found village (needle up) and turned 90 degrees (needle to the side), 16x and 32x; the seek result checked against vanilla /locate in that world.

## Art rows

Added to `docs/art-todo.md` (five rows): all five item textures from `tools/totems-textures.py` (new script, hand-placed 16x grids, 32x via `tools/pixelart.py`, Seeking's needle drawn at each resolution), plus `models/item/totem_of_seeking_held.json` (raises the held Seeking totem so its dial is on screen).
