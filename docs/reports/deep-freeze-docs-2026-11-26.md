# Deep Freeze (26.4) port: docs report, 2026-11-26

Branch `deep_freeze`, worktree `../minecraft-deep_freeze`. Built blind against **26.4-pre-1**: everything compiles, nothing has been run (no game tests, no client screenshots, no playtest), by the owner's instruction. Merge this into `specs/` and `player-docs/` when 26.4 is released and the port is tested.

## State

- Every mod, client source set and game test source set compiles against 26.4-pre-1 (Fabric API `0.167.1+26.4`, Loom 1.19.4, Loader 0.20.1).
- Every mixin target was checked against the 26.4 source by name, signature and `@Shadow` (scripts in the session scratchpad; worth turning into `tools/portcheck.py`). Five needed changes; all done.
- Vanilla JSON keys and ids removed in 26.4 (`block/cube_top`, `core/screenquad`, `flowing_water`, `flowing_lava`, `interpolated`) are used nowhere in our data or assets. Reworked Mobs datagen ran on 26.4 and wrote the same JSON formats (only the new Frostbite files changed).
- Pack formats: data `min/max 128`, resource `min/max 104` (26.4-pre-1 is 128.1 / 104.0). **Recheck on the final release**, which may bump them.
- Nothing is disabled: every project builds on 26.4.

## Spec facts to merge

### specs/build.md
- `fabric.mod.json` takes its Minecraft range from `${minecraft_dependency}` (`gradle/fabric-mod.gradle`; default `~<minecraft_version>`). On a pre-release set `minecraft_dependency=~26.4-` in `gradle.properties`: Fabric reads `26.4-pre-1` as `26.4-pre.1`, so `~26.4-pre-1` matched nothing and no mod loaded. Back to the default once on the final `26.4`.

### specs/undead.md (Undead variants, Scope and variants, Natural spawning)
- New variant `UndeadVariant.FROSTBITE` (appended last): block + item `frostbite_sleeper` ("Sleeping Frostbite"), entity `woken_frostbite` (`WokenFrostbite extends WokenZombie`). Tags: `#woken_undead`, `#grabs_loot`, `#zombies`, `#burn_in_daylight`, vanilla `#powder_snow_walkable_mobs` and `#does_not_break_brittle_ice`. Particles packed ice; sound set mud; poses lying, sitting, face down.
- Woken Frostbite: vanilla Frostbite's melee ways (a hit gives Freezing for 3 s, powder snow doesn't slow it, Frostbite sounds). It doesn't throw Ice Balls.
- It breaks out of ice like a stray (the same crack warning, 1.5 s, ice shatter: `SleeperBreakout`).
- **Ice Caves:** feature `bubbas_reworked_mobs:frozen_frostbite` (`FrozenFrostbiteFeature`), added to `minecraft:ice_caves` at underground decoration. Once per chunk, a 12 % chance (`UndeadTuning.FROZEN_FROSTBITE_CHUNK_CHANCE`), 6 random columns searched top-down between y -50 and 40 for an open spot (2 air over a sturdy floor) inside the Ice Caves biome. It lies down (30 % face down), clear ice on top and in the open sides.
- Woken zombies no longer turn into vanilla Frostbites in powder snow (`WokenZombie.convertsWhenFreezing` false, as `convertsInWater`).
- Natural spawning: 26.4's new Frostbite and wider Stray natural spawns are monsters, so the 15 % monster cap cut already covers them.
- Renderers: sleeper uses `ModelLayers.FROSTBITE` with the outer layer (`frostbite_outer_layer.png`); the woken mob uses `WokenZombieRenderer` with `frostbite.png` (no outer layer yet).

### specs/monster-camps.md
- New camp kind `FROSTBITE` (vanilla Frostbites): cyan banner, white skull, light blue lower triangles; the stray camp's spruce and snow materials; zombie horses for patrols and the stable; no archers (they throw their own Ice Balls).
- Cold biomes (base temperature under 0.15) pick strays or frostbites 50/50 with the coin the camp already rolls for skeletons or zombies (the random stream is unchanged).
- `CampPiece` saves the kind by its enum position (`Kind`), so a new kind must go last (FROSTBITE is after PIGLIN).

### specs/mob-factory.md
- The office jukebox loops: an empty chest sits on top of it (26.4 jukebox playlists replay the disc when the chest above has none). Remove "Office jukebox loop" from Parked and "waits for vanilla's infinite jukebox".

### specs/effect-gems.md (and the generated `docs/research/effect-gems-balance.md`)
- **Freezing gem**, harmful, on/off kind, brewed from the 26.4 Freezing potion (normal 1:30, long 3:00, no strong): 36 infusion recipes. On a weapon: the vanilla Freezing effect for a duration scaled by strength. Worn: the wearer is held at `0.9 x s` of the way to frozen (`WORN.freezing.frozen_share`), so slowed and shaking but never frozen solid and no freeze damage. Leather armour and fire counter it (vanilla `canFreeze`).
- `tools/gem-balance.py` reads the Minecraft version from `gradle.properties` and finds `build/mcsrc/<version>` in the main checkout from a worktree; it understands 26.4's `MobEffectIds`.
- Remove "Frost waits for the next Minecraft drop's freezing effect" (twice).

### specs/quality.md / tests
- Stubbed until rewritten for the 26.4 command API (`Commands.getDispatcher()` is gone):
  - `RefModGameTest.statsCommandPrintsAndGatesOtherPlayers` fails on purpose ("not yet ported").
  - `GemsCommandGameTest`: the two "/gems is registered" checks are TODOs.
- `LootDisplayGameTest` runs its commands through `performPrefixedCommand`.

## Owner review: choices I made (not in any owner decision)

1. Freezing gem worn effect (held 90 % x strength of the way to frozen, never frozen solid). Alternatives: "Freezing on the wearer all the time" like Glowing, or a drawback-only role.
2. Woken Frostbites don't throw Ice Balls (melee only). Vanilla's throw needs its ranged AI; can be added.
3. Ice Cave numbers: 12 % of Ice Cave chunks, y -50 to 40, 30 % face down, clear ice.
4. Frostbite camps take half of the cold-biome camps (strays had all of them). Banner and materials.
5. Office jukebox: a visible chest on it. Players can open it and put discs in to change the playlist.
6. Woken zombies stay zombies in powder snow (they don't become Frostbites).

## Check when 26.4 is out (visual and runtime, all untested)

- Pack formats on the final release; `minecraft_dependency` back to the default.
- Client screenshots: Frostbite sleeper (outer layer), woken Frostbite (no outer layer: may look plain), Sleeping Frostbite item icon (skull renderer with `zombie/frostbite`), gravestone edit screen (26.4 sign layout: `getSignTextYOffset` -1 and spacing 10 were guessed), enchanting table gem clue (now through `getClue`), Reworked Beacons screen.
- Tooltips: 26.4 word-wraps long tooltip lines (`tooltip_display.split_lines`, default on). Gem and relic tooltips may wrap differently.
- `Mth.clamp` became `Math.clamp` at 22 call sites; `Math.clamp` throws if min > max (Mth didn't).
- 26.4's stricter loot and predicate validation (errors on recursion and missing context) might reject one of our loot tables or modifiers at load. Watch the log.
- Ice Cave Frostbites: how often they show up, and whether the ice looks right.
- The Freezing gem in the balance tool and in game (brewing, worn chill, weapon hit).

## Player docs (when released)

- Reworked Mobs: Frostbites as Sleeping Undead, frozen in ice in Ice Caves; Frostbite camps in cold biomes; the Fabricator office jukebox now plays on a loop. Drop the [Coming in 26.4] tags in `specs/copy-strings.md`.
- Infused Gems: the Freezing gem, brewed from the Freezing potion.
- Every project is on 26.4 (none paused).
- BACKLOG: remove "26.4 Frostbites", "26.4 Freezing gem" and "Bogged camp weeds" (26.4 lets mushrooms survive daylight).
