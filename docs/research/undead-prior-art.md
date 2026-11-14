# Prior art: dormant undead mod (MC 26.3, Fabric)

Researched 2026-11-14. Sources: `tools/prior-art.py`, Modrinth API v2 (search sorted by relevance and by downloads, with and without the fabric facet, mods and datapacks), `/v2/project/<slug>/version?loaders=["fabric"]&game_versions=["26.3"]` for real 26.3 files, web search (CurseForge-only and older mods), shallow sparse clones in `/tmp/claude-0/undead/src/<name>` (read only, not run), and vanilla 26.3 source in `/tmp/claude-0/mcsrc`.
This builds on `docs/knowledge/libraries.md` and `reference-mods.md` and doesn't repeat them: GeckoLib/AzureLib/SBL/Lithostitched/Graveyard basics, and "YUNG's API has no Fabric 26.3 build", are covered there.

**Owner corrections applied (mid-research):**
1. Compatibility with third-party texture packs (Faithful 32x) is **not** a criterion. The repo makes its own 16x and 32x art.
2. **Dormant undead are real blocks**: Silk Touch minable, no tick cost, outside mob caps. No display-entity fake blocks.

So the most relevant prior art is the "block that turns into a mob" pattern, not the "mob with a sleeping AI state" pattern.

---

## 1. Verdict: does "dormant until disturbed" already exist?

**No polished version exists, on 26.3 or any other version.** The closest projects each cover one slice of the idea:

| Project | What it covers | How close | 26.3 | License / source |
|---|---|---|---|---|
| **Lorespawn** (lilypuree, "SpawnerMod", Forge 1.16.3, abandoned 2020, v0.0.4) | Replaces random spawning with world-generated spawner *blocks*. Player noise (block breaking, sprinting) wakes them in a radius. Egg sacs hatch spiders, connected sacs cascade. A `BoneDepositBlock` that turns into a skeleton was written but **commented out**. | **Closest in concept**: a block-based dormant spawn plus noise wake plus chain wake. It's a prototype, though, with no visuals, no stealing and no poses. | no (1.16) | Repo `LICENSE` is **Unlicense** (CurseForge says MIT), so it's copyable, but the code is 1.16 MCP-era. github.com/lilypuree/SpawnerMod `1.16.x` |
| **Vanilla Copper Golem Statue** (26.3) | A real block made from a mob (`CopperGolem.turnToStatue`), with a pose blockstate, a block entity that stores the mob, and an entity model rendered by a BER. The axe turns it back into a living mob (`CopperGolemStatueBlockEntity.removeStatue`). The item keeps its pose through the `BLOCK_STATE` component. | **Closest in mechanics** to "dormant undead are real blocks". It's the template to copy structurally. | yes | vanilla |
| **Forsaken Corpses** (`forsaken-corpses`, mod id `lying_skeletons`, 59k downloads) | Worldgen skeleton corpse **blocks** in caves, with 4+ poses, variants holding iron/gold/stone pickaxes, bone drops, Nether variants. Planned: "backpacks near corpses containing treasure". | **Closest visually.** They are inert, though: they never wake. | no (26.2 latest) | ARR. Read only. github.com/bacondragon0/lying_skeletons_1.20.1 (public code is 1.20.1 Yarn) |
| **The Graveyard** (`the-graveyard-fabric`, 5.6M) | Its `SarcophagusBlock.spawnGhost` gives a worldgen sarcophagus a 25% chance to spawn a Wraith when a player opens it (the "loot risks waking" pattern). The Ghoul has a 92-tick emerge-from-ground GeckoLib animation. Also has `BoneDisplayBlock` remains blocks, a horde `CustomSpawner` and `MobSpawnGate`. | Medium. It has the pieces (disturb a block, a mob emerges, an emerge animation) but no dormant population and no noise. | **yes** (4.0.0+mc26.3) | GPL-3.0. Read only. Already in reference-mods.md |
| **Zombie Awareness** (Corosauce, 9.2M) | Zombies hear mining, doors, chests, explosions, eating and so on, scaled by loudness. Also blood scent and light awareness. | Covers the noise-detection bullet, but for *awake* mobs. | no (1.21.1, last 2024-09) | ARR (source public). Read only |
| **Attract to Sound** (`attract-to-sound`, 80k) | Full stealth overhaul: hearing, vision, scent, camo, group leaders and followers. | Covers the noise and stealth bullets for awake mobs. Very large (258 files). | no (26.2 is latest Fabric; actively updated, so 26.3 is likely soon) | GPL-3.0. Read only |
| **Warband** (6.5k) | Squad AI, including `ZombieHordeGoal`/`ZombiePackGoal`/`RegroupGoal` and suspicion/half-notice barks. | Covers the "zombies drift to group up" bullet. | no (26.2) | **MIT** (copyable). github.com/Overshock-Games/Warband `port/26.2` |
| **Consecration** (66k) | Undead resist everything except fire and "holy" sources (smite, silver and others), through `HolySources`, `UndeadTypes` and `api/ConsecrationApi.java`. | Covers the "undead tanky, Smite matters" bullet. | no (1.20.1, last 2024) | LGPL-3.0. Read, or copy keeping LGPL |
| **Neutral Monsters: Stealthy Peaceful Mode** (datapack/mod) | Monsters don't attack unless you linger or provoke them, and bystanders join fights. | Thematically near (avoidable threat), but mechanically different. | yes | MIT, no source |

Nothing else came close. Searches for sleeping, dormant, asleep, wake, awaken, ambush, sitting skeleton, lying zombie, crypt, catacombs, tomb and corpse turned up only player-corpse/grave mods, bed-sleep mods, or horror stalkers. A web search found nothing on CurseForge that does "mobs generated asleep in structures that you sneak past and loot". **The core loop (static dormant undead plus stealth looting plus noise-chain waking) is open territory.**

---

## 2. Project details and the files that implement the relevant mechanism

Clones are in `/tmp/claude-0/undead/src/`.

### Lorespawn (`src/lorespawn`), Unlicense, 1.16 Forge
- `src/main/java/com/lilypuree/msms/spawners/MobAwakener.java`: per-player tick. Every `aggroInterval` it reads the player's noise value and finds spawner blocks within 12 blocks via a per-chunk spawn-point index. If `noise > block.sensitivity`, it spawns the mob at nearby valid ground and caps spawns at 4 per alarm.
- `capability/PlayerAggroHandler.java`: per-player accumulated noise.
- `capability/ChunkSpawnPoints.java` + `util/ChunkUtils.java`: a chunk-level index of spawner block positions, so it never scans blocks. On 26.3 we can use the Fabric Data Attachment API on chunks, or (better) vanilla game-event listeners, which need no index (see section 4).
- `block/BoneDepositBlock.java`: a "potency" blockstate that decreases per spawn and reverts to the parent block at 0. This is the skeleton-from-block idea, unfinished.
- `block/EggSackBlock.java`: connected sacs break together (chain wake).
- `WorldEntitySpawner.java`: replaces vanilla natural spawning and makes dark rooms spawn *less*.

### Vanilla Copper Golem Statue (26.3)
- `common/net/minecraft/world/level/block/CopperGolemStatueBlock.java`: `POSE` enum property, FACING, waterlogging, and pose cycling on interaction.
- `common/.../block/entity/CopperGolemStatueBlockEntity.java`: `createStatue(golem)` stores the mob's data, `removeStatue(state)` recreates it with `EntitySpawnReason.TRIGGERED` and snaps it to the facing, and `getItem` keeps the pose in the item. **No ticker.**
- `common/.../block/WeatheringCopperGolemStatueBlock.java`: the interaction that turns a statue back into a mob.
- `common/net/minecraft/world/entity/animal/golem/CopperGolem.java` `turnToStatue()`: mob to block.
- `client/net/minecraft/client/renderer/blockentity/CopperGolemStatueBlockRenderer.java` + `client/model/object/statue/CopperGolemStatueModel.java`: renders the entity model inside a BER, posed per blockstate.

### Forsaken Corpses (`src/lyingskeletons`), ARR, 1.20.1 Yarn
- `blocks/custom/LyingSkeletonBlock.java` and the `LyingSkeletonWith{Stone,Iron,Gold}PickaxeBlock.java` variants: a separate block class per held tool. That doesn't scale. For us, a block entity holding the weapon/armor stacks is better.
- Corpses are plain JSON block models (assets weren't cloned) plus a cave feature. Read the idea only.

### The Graveyard (`src/graveyard`, branch `26.3`), GPL-3.0
- `common/src/main/java/com/lion/graveyard/blocks/SarcophagusBlock.java` `spawnGhost(...)`: worldgen-only (`PLAYER_PLACED=false`), a 1-in-4 chance on open, spawns a Wraith with `EntitySpawnReason.TRIGGERED`, then marks the block as used.
- `entities/GhoulEntity.java`: `SPAWN_ANIMATION` "animation.ghoul.spawn" (play and hold), `SPAWN_TIMER` synced data (92 ticks), and a "transition lifts the ghoul above the ground". This is the emerge pattern on GeckoLib 5.
- `entities/MobSpawnGate.java`: separates automatic spawn reasons from player-caused ones. That's a useful pattern for "natural spawning reduced, triggered spawning untouched".
- `entities/horde/GraveyardHordeSpawner.java`: a `CustomSpawner`. **Our natural-spawn reduction will not reduce it** unless we hook custom spawners too.

### Attract to Sound (`src/attractsound`, branch `7.0.2-26.2-fabric`), GPL-3.0
- `tracking/SoundTracker.java`: a bounded, lock-protected list of recent sounds, scored by a worker thread (`worker/WorkerScheduler`).
- `network/SoundMessage.java`: sounds are captured **on the client and sent to the server**. That's cheatable, and the opposite of what we want.
- `event/AIModificationEvents.java`: adds goals to *every* mob on load. **Compatibility risk**: a mob we spawn on wake gets their goals too.
- `integration/sbl/SoundAttractSensor.java`: SmartBrainLib sensor integration.
- There's no public API. We can't integrate cleanly; we can only coexist.

### Zombie Awareness (`src/zombieawareness`, branch `1.21.0`), ARR
- `mixin/MixinServerLevel_playSeededSound.java`, `MixinLevelEvent.java`, `MixinExplosion.java`, `MixinNoteBlock.java`: **server-side** sound capture, which is the right place. Read for the list of noise sources and their loudness weights (`client/SoundProfileEntry.java`, `config/ZAConfigFeatures.java`).

### Warband (`src/warband`, branch `port/26.2`), MIT
- `src/main/java/com/warband/ai/goal/ZombieHordeGoal.java`, `ZombiePackGoal.java`, `RegroupGoal.java`, `SuspicionPauseGoal.java`, `InvestigateLastKnownGoal.java`: zombie grouping drift and "half-noticed" behaviour. These are copyable (MIT) once ported to 26.3.

### Consecration (`src/consecration`, branch `1.20.x`), LGPL-3.0
- `common/impl/HolySources.java`, `common/impl/UndeadTypes.java`, `common/ConsecrationEvents.java`, `mixin/MixinLivingEntity.java`: "undead resist all damage except holy". Bows count through a "holy projectile" source list. That's the prior art for "bows need a Smite equivalent".

### Spawn control on 26.3 (so ours doesn't fight it)
- **In Control!** (`src/incontrol`, `26.3_fabric`), MIT, 4.2M downloads: `mixin/NaturalSpawnerMixin.java` uses `@Inject isValidPositionForMob RETURN` and `@WrapOperation` in `spawnMobsForChunkGeneration`. `mixin/FinalizeSpawnMixin.java` uses `@WrapMethod finalizeSpawn`. `spawner/SpawnerSystem.java` is its own custom spawner. Rules live in `config/incontrol/spawn.json`.
- **Mob Filter** (`src/mobfilter`, `main`), MIT (Modrinth says Apache), 714k: `mixins/WorldGenRegionMixin.java` and `ServerLevelMixin.java` cancel `addFreshEntity` at HEAD, `EntityTypeMixin` hooks `EntityType.create(level, reason)`, and `NaturalSpawnerSpawnStateMixin` hooks `afterSpawn`. Rules can match `spawnReason`.
- **Easy Mob Spawn Control** (ARR, no source, 218k): in-game spawn rate and mob cap GUI.
- **Civillis** (ARR, source public, 26.1.2 only): `CivilSpawnHelperMixin` wraps `NaturalSpawner.spawnForChunk`, and `CivilServerLevelSpawnGateMixin` gates spawns near "civilized" blocks.
- Vanilla: gamerules `spawn_monsters`, `spawn_mobs` and `spawn_patrols` (`world/level/gamerules/GameRules.java:77-80`). `NaturalSpawner.java:76` skips mobs with `isPersistenceRequired()` or `requiresCustomPersistence()` when counting the mob cap. That's moot now that dormant undead are blocks, but it matters for woken undead: set persistence on them, or they despawn and refill the cap.

### Other useful mods on 26.3
- **pv-addon-sculk** (MIT, 85k, Fabric 26.3 1.1.0): Plasmo Voice proximity chat triggers sculk sensors and wardens. If our dormant blocks listen to vanilla vibrations, **voice chat wakes the dead for free**.
- **Wooled Boots** (ARR, stops at 1.21.4): a vibration-silencing boot. It shows players expect wool to occlude vibrations.
- **Undead Nights** (ARR, source public, 26.3) and **Zombie Tactics** (GPL, 26.3; the public `fabric` branch is still 1.21.1): horde and zombie AI. Both are compatibility risks (section 5).
- **Sit!** (LGPL, 26.3): player sitting only. Not relevant to mob poses.

---

## 3. Libraries and vanilla systems to build on (B9)

Since dormant undead are blocks, the animation question splits in two:

**A. The dormant body (a block).** No animation library is needed.
- **JSON block models** can now be posed freely: 26.3 `CuboidModelElement` accepts `"rotation": {"origin", "x", "y", "z"}` Euler rotations (`CuboidRotation.EulerXYZRotation`) besides the old single axis/angle form (`client/net/minecraft/client/resources/model/cuboid/CuboidModelElement.java:89-114`). I found no angle clamp in the 26.3 code. A sitting skeleton or a zombie lying in a grave can be a static baked model: zero BER cost, frustum-culled like terrain, with free texture variants.
- The weapon, armor or held item shown on the body: either blockstate variants for a fixed set (vanilla items only), or a small **BER that draws only the item/armor layer** (the copper golem statue renderer is the pattern), with the body itself in the baked model.
- Persistent contents (armor and weapon stacks, loot reference, "already looted" flags) go in a block entity **without a ticker**. For Silk Touch pickup, keep the contents on the item through `DataComponents.BLOCK_ENTITY_DATA`/`BLOCK_STATE`, the way copper golem statues keep their pose.
- **Hearing without ticking**: a block entity implementing `GameEventListener.Provider` is registered automatically in the chunk's game-event registry (`LevelChunk.java:690 addGameEventListener`). Its `handleGameEvent(...)` is called synchronously when a vibration happens within `getListenerRadius()`. No ticker is needed if you react immediately. Vanilla sculk sensors add a ticker only because they *delay* vibrations by travel time (`VibrationSystem.Ticker`). Reusing `VibrationSystem.User`/`Data` gives us, for free: wool occlusion, the `#vibrations` and `#ignore_vibrations_sneaking` tags (sneaking already silences steps), vanilla vibration frequencies for "how loud", and compatibility with voice-chat vibration addons. Reference: `world/level/block/entity/SculkSensorBlockEntity.java` (implements `GameEventListener.Provider<VibrationSystem.Listener>, VibrationSystem`) and `Warden.java` (`VibrationSystem` plus `AngerManagement`).
- Waking spawns the real mob with `EntitySpawnReason.TRIGGERED` (the copper golem and Graveyard pattern) and removes or replaces the block. A chain wake is just the woken mob (or the block) emitting a custom `GameEvent` that neighbouring dormant blocks listen for.

**B. The woken mob (an entity).** Slow, staggering, emerging.
- **Vanilla keyframe animations** (`client/net/minecraft/client/animation/KeyframeAnimation`, `AnimationDefinition`, `definitions/WardenAnimation.java`, `CreakingAnimation.java`, `CopperGolemAnimation.java`) driven by `AnimationState` on the entity (`Warden.emergeAnimationState`/`diggingAnimationState`, `Pose.EMERGING`/`DIGGING`/`SITTING`). This works on a subclass of the vanilla humanoid/zombie/skeleton models with **no dependency**. Blockbench can export vanilla `AnimationDefinition` Java. **My default recommendation**: an emerge animation, a stagger walk and a stumble are all within what Warden and Creaking already do.
- **GeckoLib 5.5.7** (`com.geckolib:geckolib-fabric-26.3`, MIT) only if we want Blockbench `.animation.json` hot-swappable assets, bone-level effects, or animated BERs (`GeoBlockRenderer`). The Graveyard 26.3 is the working example. Cost: a hard dependency, and geo models replace the vanilla model tree. That's fine now that third-party texture-pack compatibility doesn't matter.
- **AzureLib 4.1.8**: only for its extras (per-bone hitboxes). No reason for this mod.
- **SmartBrainLib 2.0.3** (MPL, 26.3): optional, if woken undead AI grows beyond a few Goals. Attract to Sound has an SBL sensor integration, which proves the pattern.
- **Lithostitched** (MIT, 26.3): datapack worldgen modifiers and processors. It's the cleanest way to inject our dormant-undead marker processors into **vanilla and other mods' jigsaw structures** (Lithostitched's `StructureTemplatePoolMixin`/processor retrofits, covered in reference-mods.md).
- **Smite insta-kill**: vanilla `data/minecraft/enchantment/smite.json` is data-driven (`minecraft:damage` add effect against `#minecraft:sensitive_to_smite`, which includes `#minecraft:undead`). **Don't override smite.json**: whole-file replacement collides with other datapacks that rewrite it (e.g. Compatible Sword Enchantments on 26.3 changes Smite's `exclusive_set`). Hook in code instead: on damage, if the weapon has Smite and the target is in `#sensitive_to_smite`, roll the chance, then kill it and spawn particles. For bows, either a new bow enchantment ("Hallowed"-style) or let Smite apply to bows via `supported_items`. Prior art: Consecration's holy projectiles.

---

## 4. Compatibility risks

1. **Spawn mods hooking `addFreshEntity`.** Mob Filter cancels `WorldGenRegion.addFreshEntity`/`ServerLevel.addFreshEntity`, and In Control has `onjoin`/`finalizeSpawn` rules. With dormant undead as blocks this hits only the **wake** (the mob spawn). A user's "no zombies" rule would make a dormant zombie block vanish on wake without a mob appearing. Spawn with `EntitySpawnReason.TRIGGERED`, check the return of `addFreshEntity`, and if it was refused, leave the block or drop the loot. Document this.
2. **Our spawn reduction vs In Control, Mob Filter, Easy Mob Spawn Control and Civillis.** All four touch `NaturalSpawner`: In Control at `isValidPositionForMob` RETURN plus `spawnMobsForChunkGeneration`, Mob Filter at `SpawnState.afterSpawn`, Civillis around `spawnForChunk`. Use a single small, separate hook (a scaled monster cap, or a probability gate on `EntitySpawnReason.NATURAL` only), configurable down to "off", so users of those mods can let theirs win. Don't `@Overwrite` or cancel at HEAD of `spawnForChunk`. Custom spawners (Graveyard hordes, Undead Nights horde nights, patrols, phantoms) bypass natural spawning. Decide whether "much rarer spawns" includes them; vanilla gamerules already cover patrols and phantoms.
3. **AI-overhaul mods applying to our woken undead.** Zombie Tactics mixes into `Zombie.class` (mining, climbing), Warband adds goals to vanilla mob families, and Attract to Sound adds goals to *every* mob on `ENTITY_LOAD`. A subclass of `Zombie`/`AbstractSkeleton` inherits their mixins, which would undo "slow and staggering". If we keep vanilla entity types, they **will** apply. Either use our own entity types that don't extend `Zombie` (extend `Monster` and reuse the model and renderer), or add a compatibility check. Note that our own types lose vanilla interplay (`#minecraft:undead` tag membership we must add, zombie-villager curing, baby zombies, drowned conversion).
4. **Fresh Animations + EMF** (52M + 106M downloads, both on 26.3). Texture compatibility no longer matters, but EMF *replaces vanilla zombie and skeleton animations* when Fresh Animations is loaded. If we animate vanilla entity types through vanilla models, a large share of players will see Fresh Animations' walk instead of our stagger. Custom entity types with their own model layer avoid this (EMF only targets known vanilla and declared models).
5. **Structure mods.** YUNG's Better Dungeons and YUNG's API have **no 26.3 build** (latest 26.1.2), so they aren't a concern now. On 26.3: Dungeons and Taverns (22M, ARR datapack), Repurposed Structures (15M, LGPL), Towns and Towers (19M), Structory, Explorify, The Graveyard. When Dungeons Arise is 1.21.1 only. Their rooms use vanilla spawners (`EntitySpawnReason.SPAWNER`), which our natural-spawn cut won't touch. If our "undead much tankier" change applies to all zombies, spawner dungeons from those mods become much harder. Consider scoping the buffs to our undead types, or apply them deliberately. Graveyard structures plus our crypts in the same biomes could feel redundant. A Lithostitched-based option to sprinkle dormant blocks into other mods' structures is a nice integration point.
6. **Vibration compatibility (a benefit).** Sculk-based hearing automatically interacts with wool, sneaking, Swift Sneak, pv-addon-sculk voice chat, and anything that emits vanilla `GameEvent`s. Ancient cities already have sculk sensors and shriekers, and our blocks should ignore vibrations they relay (sculk chains).
7. **Licenses.** Copyable: Lorespawn (Unlicense), Warband (MIT, 26.2), In Control (MIT), Mob Filter (MIT), GeckoLib (MIT), Lithostitched (MIT), and vanilla patterns. Read only: Attract to Sound, The Graveyard and Zombie Tactics (GPL), Zombie Awareness, Forsaken Corpses, Civillis and Undead Nights (ARR). Consecration (LGPL) is reusable only as LGPL.

---

## 5. Things that might make the owner reconsider part of the design

- **"Warden-style vibration" with blocks is cheaper and better than expected.** Block entities get vibrations through `GameEventListener.Provider` with no ticker, and the vanilla tags already encode "sneaking is quiet" and "wool blocks sound". The one gap is that vanilla has no "noise level" beyond the vibration frequency (1-15) and distance. Armor theft "always risks waking" needs our own roll on interaction, not a vibration.
- **A block entity is needed for the weapon, armor and loot on the body** (and for the listener). It doesn't tick, but it is still a block entity per body. Rooms with "many dormant undead" mean many BEs, plus a BER per body if the held item or armor is rendered dynamically. Precompute fixed visual variants as baked block models where possible, and keep the BER for item and armor only.
- **Chain waking can cascade** across a densely packed crypt (Lorespawn's egg sacs show the effect). Cap wakes per event and per tick, the way Lorespawn's `getMaxSpawns()=4` does.
- **Vanilla types vs custom types** for the woken mobs is the decision with the most knock-on effects (section 4, items 3 and 4). Custom types avoid AI-mod and Fresh Animations interference and let the buffs stay scoped. Vanilla types keep vanilla behaviour and other mods' expectations. I recommend custom types that are tagged `#minecraft:undead`/`#sensitive_to_smite`, and leaving vanilla zombies (which will be rare anyway) alone.
- **Much higher HP plus weak fists** overlaps with Consecration's "resistant except holy" design. That design was popular, and its main complaint was early-game unkillability. Smite's insta-kill chance helps, but early players without Smite need another counter (fire, sunlight still burning, a crafted holy item).
- **"Zombies slowly drift to group up" is only cheap for woken mobs.** For dormant blocks it would mean moving blocks. Either drop it for dormant ones, or do it as an occasional unloaded-chunk "relocation" at generation time.
- **The whole idea is unclaimed**, so there's no reason to shrink scope to avoid duplication. The nearest polished pieces (Graveyard's sarcophagus and ghoul emerge, Forsaken Corpses' posed bodies) are complementary, and both are read only.
