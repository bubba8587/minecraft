# Data-driven registries in Java 26.3: JSON shapes with vanilla examples

Scope: every registry a 26.3 data pack can add to or override (`data/<ns>/<registry path>/<id>.json`), with its codec fields, a minimal real vanilla example, and what the 26.x registries do. Commands, functions, loot/predicate/advancement/recipe syntax and tooling are in `datapacks.md`.
Verified 2026-11-14 against 26.3 (data pack format 121.0): codecs in `src: = /tmp/claude-0/mcsrc/common/`, examples from `jar: = /tmp/claude-0/mcsrc/vanilla-jar/data/minecraft/`, plus the minecraft.wiki 26.1/26.2/26.3 changelogs where noted.

The registry list comes from `src: net/minecraft/resources/RegistryDataLoader.java` (`WORLD_REGISTRIES`, `DIMENSION_REGISTRIES`, `RELOADABLE_REGISTRIES`, `SYNCHRONIZED_REGISTRIES`). Folder = `Registries.<KEY>` path (`src: net/minecraft/core/registries/Registries.java`).

## Conventions used by almost every codec

- **Holder / reference**: a field typed `Holder<X>` accepts a string ID (`"minecraft:foo"`, which resolves into the registry) **or** an inline object. A field typed `HolderSet<X>` accepts `"id"`, `["id", ...]` or `"#tag"`. In 26.3 a list may also mix inline values and IDs (`RegistryCodecs.holderSet`).
- **Block state** (`StateHolder.codec` / `BlockState.CODEC`): `"minecraft:stone"` (default state) or `{"id":"minecraft:oak_log","properties":{"axis":"y"}}`. The old `Name`/`Properties` keys are **gone** from JSON.
- **Colours**: `"#rrggbb"` / `"#aarrggbb"` strings (`ExtraCodecs.STRING_RGB_COLOR`/`STRING_ARGB_COLOR`) in biome effects and environment attributes.
- **Text components**: JSON form in these files.
- **Int/float providers** (worldgen, not loot): `constant uniform biased_to_bottom very_biased_to_bottom clamped weighted_list clamped_normal trapezoid` (int, `IntProviders`) and `constant uniform clamped_normal trapezoid` (float). A bare number means constant. Loot-style **context** providers are a separate system (see `context_int_provider` below).
- **Environment attribute maps** (dimension_type, biome): `{"minecraft:<attr>": value}` or `{"minecraft:<attr>": {"argument": value, "modifier": "<mod>"}}`. Modifiers (`src: net/minecraft/world/attribute/modifier/AttributeModifier.java`): `override alpha_blend add subtract multiply blend_to_gray minimum maximum and nand or nor xor xnor append overlay`. Each attribute type allows only some of them.
- **Environment attribute IDs** (`src: net/minecraft/world/attribute/EnvironmentAttributes.java`):
  - visual: `fog_color fog_start_distance fog_end_distance sky_fog_end_distance cloud_fog_end_distance water_fog_color water_fog_start_distance water_fog_end_distance sky_color sunrise_sunset_color cloud_color cloud_height sun_angle moon_angle star_angle moon_phase star_brightness block_light_tint sky_light_color sky_light_factor night_vision_color ambient_light_color default_dripstone_particle ambient_particles`
  - audio: `background_music music_volume ambient_sounds firefly_bush_sounds`
  - gameplay: `sky_light_level can_start_raid water_evaporates bed_rule straw_bed_rule respawn_anchor_works nether_portal_spawns_piglin fast_lava increased_fire_burnout eyeblossom_open turtle_egg_hatch_chance piglins_zombify snow_golem_melts creaking_active surface_slime_spawn_chance cat_waking_up_gift_chance bees_stay_in_hive monsters_burn can_pillager_patrol_spawn natural_mob_spawns creature_world_gen_spawn_probability villager_activity baby_villager_activity`
  - Each ID is prefixed `minecraft:` in files. Layering order is dimension → biome → timeline (`EnvironmentAttributeSystem`). Biomes only accept positional attributes (`CODEC_ONLY_POSITIONAL`).
- **Reload**: everything here except `loot_table predicate item_modifier slot_source context_*_provider advancement recipe` needs a **server restart**. Static registries are frozen at world load and synced to clients during configuration.

---

## 1. Dimensions

### `dimension_type` (`src: net/minecraft/world/level/dimension/DimensionType.java`)
Fields:
- Required: `has_skylight` `has_ceiling` `has_ender_dragon_fight` (26.1) `coordinate_scale` `min_y` (multiple of 16) `height` (multiple of 16) `logical_height` `infiniburn` (block holder set; ID/list since 26.2) `ambient_light` `monster_spawn_light_level` (int provider 0..15) `monster_spawn_block_light_limit`.
- Optional: `has_fixed_time` (false) `skybox` (`overworld`, etc.) `cardinal_light` (`default`/`nether`) `attributes` (env attribute map) `timelines` (timeline holder set) `default_clock` (world_clock ID; without it `/time` needs `of <clock>`, and sleep/siege markers never apply).
- Removed vs 1.21.x (moved into attributes/timelines): `fixed_time`, `ultrawarm`, `natural`, `piglin_safe`, `bed_works`, `respawn_anchor_works`, `has_raids`, `effects`.
```json
{
  "ambient_light": 0.0, "coordinate_scale": 1.0, "default_clock": "minecraft:overworld",
  "has_ceiling": false, "has_ender_dragon_fight": false, "has_skylight": true,
  "height": 384, "logical_height": 384, "min_y": -64,
  "infiniburn": "#minecraft:infiniburn_overworld",
  "monster_spawn_block_light_limit": 0,
  "monster_spawn_light_level": { "type": "minecraft:uniform", "min_inclusive": 0, "max_inclusive": 7 },
  "timelines": "#minecraft:in_overworld",
  "attributes": {
    "minecraft:gameplay/bed_rule": { "can_set_spawn": "always", "can_sleep": "when_dark", "error_message": { "translate": "block.minecraft.bed.no_sleep" } },
    "minecraft:visual/sky_color": "#78a7ff",
    "minecraft:visual/cloud_height": 192.33
  }
}
```
(trimmed from `jar: dimension_type/overworld.json`. The Nether example sets `cardinal_light: "nether"`, `gameplay/fast_lava`, `gameplay/water_evaporates`, `visual/fog_*`.)

### `dimension` (LevelStem, `src: net/minecraft/world/level/dimension/LevelStem.java`)
`{type: dimension_type holder, generator: ChunkGenerator}`. Vanilla ships none: its dimensions are defined inside `worldgen/world_preset`. A pack file `data/mypack/dimension/mining.json` adds a dimension when the world loads (it cannot be added by `/reload`).
- Generator types (`ChunkGenerators`): `noise{biome_source, settings}`, `flat{settings}`, `debug`.
- Biome source types (`BiomeSources`): `fixed{biome}`, `multi_noise{preset | biomes:[{biome, parameters}]}`, `checkerboard{biomes, scale}`, `the_end`.
```json
{ "type": "minecraft:overworld",
  "generator": { "type": "minecraft:noise", "settings": "minecraft:overworld",
    "biome_source": { "type": "minecraft:multi_noise", "preset": "minecraft:overworld" } } }
```

### `world_clock` (26.1, `src: net/minecraft/world/clock/WorldClock.java`)
An empty object: `MapCodec.unitCodec`. Vanilla has `overworld.json` and `the_end.json`, both `{}`. A clock is a named tick counter that can be paused, resumed, set and rate-scaled (`/time of <clock> ...`). It is persisted in `world/data/minecraft/world_clocks.dat`. Timelines and the `time_check` predicate refer to clocks.

### `timeline` (`src: net/minecraft/world/timeline/Timeline.java`, `AttributeTrack.java`, `net/minecraft/util/KeyframeTrack.java`)
Fields:
- `clock` (required, 26.1)
- `period_ticks?` (makes the timeline repeat)
- `tracks?`: map env-attribute → `{keyframes:[{ticks,value}], ease?, modifier?}`
- `time_markers?`: map ID → `ticks` or `{ticks, show_in_commands?}`
- `ease`: `constant linear in_*/out_*/in_out_* (quad cubic quart quint sine expo circ back elastic bounce) cubic_bezier`. Default `linear`.

A dimension type picks its timelines through `timelines` (vanilla tags `#minecraft:in_overworld`, `in_nether`, `in_end`, `universal`).
```json
{
  "clock": "minecraft:overworld", "period_ticks": 24000,
  "time_markers": { "minecraft:day": { "ticks": 1000, "show_in_commands": true }, "minecraft:wake_up_from_sleep": 0 },
  "tracks": {
    "minecraft:gameplay/monsters_burn": { "keyframes": [ { "ticks": 12542, "value": false }, { "ticks": 23460, "value": true } ], "modifier": "or" },
    "minecraft:gameplay/cat_waking_up_gift_chance": { "ease": "constant", "keyframes": [ { "ticks": 362, "value": 0.0 }, { "ticks": 23667, "value": 0.7 } ], "modifier": "maximum" }
  }
}
```
(trimmed `jar: timeline/day.json`. Others: `moon.json`, `early_game.json` (no period: `can_pillager_patrol_spawn` becomes true after 120000 ticks), and `villager_schedule.json` (villager activities by tick).)
Built-in markers: `minecraft:wake_up_from_sleep` and `minecraft:roll_village_siege`, both on the dimension's `default_clock`. Within one clock a marker ID must be unique (`Timeline::validateRegistry`).

---

## 2. Worldgen (`worldgen/*`)

### `worldgen/biome` (`src: net/minecraft/world/level/biome/Biome.java`, `BiomeSpecialEffects.java`, `BiomeGenerationSettings.java`, `MobSpawnSettings.java`)
Fields:
- `has_precipitation`, `temperature`, `temperature_modifier?` (`none`/`frozen`), `downfall`
- `effects`: `water_color` (required), `foliage_color?`, `dry_foliage_color?`, `grass_color?`, `grass_color_modifier?` (`none|dark_forest|swamp`)
- `attributes?` (positional env attributes)
- `carvers` (carver holder set)
- `features`: a list of **11** placed-feature holder sets, one per `GenerationStep.Decoration` in order: raw_generation, lakes, local_modifications, underground_structures, surface_structures, strongholds, underground_ores, underground_decoration, fluid_springs, vegetal_decoration, top_layer_modification.

26.3: sky/fog/water-fog/music/particles moved into `attributes` (26.1 already moved most of `effects`). **Spawns moved** to `attributes."minecraft:gameplay/natural_mob_spawns"` with modifier `overlay` (per-category override plus a merge of spawn costs). `count` is an int provider (it was min/max). `creature_spawn_probability` became `gameplay/creature_world_gen_spawn_probability`.
```json
{
  "has_precipitation": true, "temperature": 0.8, "downfall": 0.4,
  "effects": { "water_color": "#3f76e4" },
  "attributes": {
    "minecraft:visual/sky_color": "#78a7ff",
    "minecraft:gameplay/natural_mob_spawns": {
      "modifier": "overlay",
      "argument": {
        "spawn_costs": {},
        "spawns_by_category": {
          "creature": [ { "type": "minecraft:sheep", "count": 4, "weight": 12 },
                        { "type": "minecraft:horse", "count": { "type": "minecraft:uniform", "min_inclusive": 2, "max_inclusive": 6 }, "weight": 5 } ],
          "monster":  [ { "type": "minecraft:zombie", "count": 4, "weight": 90 } ]
        }
      }
    }
  },
  "carvers": ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"],
  "features": [[], ["minecraft:lake_lava_underground"], [], [], [], [], ["minecraft:ore_iron_upper"], [], [], ["minecraft:patch_grass_plain"], ["minecraft:freeze_top_layer"]]
}
```
(trimmed `jar: worldgen/biome/plains.json`; swamp shows `grass_color_modifier: "swamp"` and `visual/water_fog_end_distance: {argument:0.85, modifier:"multiply"}`.)
- Spawn `spawn_costs` entry: `{"minecraft:enderman": {"energy_budget": 0.15, "charge": 0.7}}`.
- A biome is only used if a biome source places it, via `multi_noise` parameters, a `fixed` source, or a custom `dimension`.

### `worldgen/feature` (was `configured_feature`, `src: net/minecraft/world/level/levelgen/feature/Feature.java`, `FeatureTypes.java`)
`{"type": <feature type>, ...config fields inlined...}`. There is no `config` wrapper since 26.3.
Types (`FeatureTypes.bootstrap`): `bamboo block_blob block_column block_pile blue_ice bonus_chest chorus_plant coral_claw coral_tree delta_feature disk end_gateway end_island end_platform end_podium end_spike fallen_tree fill_layer fossil freeze_top_layer geode huge_brown_mushroom huge_fungus huge_red_mushroom iceberg lake large_dripstone monster_room multiface_growth netherrack_replace_blobs no_op ore overlay projected_random_patchy_square random_boolean_selector random_neighbor_spread random_selector replace_single_block root_system scattered_ore sculk_patch sequence simple_block simple_random_selector single_block_pillar speleothem speleothem_cluster spike spring_feature stepped_column_cluster template tree underwater_magma vegetation_patch vines void_start_platform waterlogged_vegetation_patch weighted_random_selector`.
- Removed in 26.x: `flower`, `no_bonemeal_flower`, `random_patch` (26.1: use `count` + `offset` placement), `coral_mushroom kelp nether_forest_vegetation seagrass sea_pickle twisting_vines weeping_vines desert_well` (26.3).
- Renamed: `forest_rock`→`block_blob`, `ice_spike`→`spike`, `pointed_dripstone`→`speleothem`, `dripstone_cluster`→`speleothem_cluster` (26.2); `glowstone_blob`→`random_neighbor_spread`, `basalt_pillar`→`single_block_pillar`, `basalt_columns`→`stepped_column_cluster` (26.3).
- Bone-meal spawnability is controlled by the tag `tags/worldgen/feature/can_spawn_from_bone_meal.json`.
```json
{ "type": "minecraft:ore", "size": 9, "discard_chance_on_air_exposure": 0.0,
  "targets": [ { "state": "minecraft:iron_ore",
    "target": { "predicate_type": "minecraft:all_of", "rules": [
      { "predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables" },
      { "predicate_type": "minecraft:height_match", "min_inclusive": 0, "max_inclusive": 2031 } ] } } ] }
```
(`jar: worldgen/feature/ore_iron.json`. Rule tests (`RuleTestType`): `always_true block_match blockstate_match tag_match random_block_match random_blockstate_match` plus 26.3 `all_of any_of not height_match`.)
- Tree sub-types: trunk placers `straight forking giant mega_jungle dark_oak fancy bending cherry poplar` (`*_trunk_placer`); foliage placers `blob spruce pine acacia bush fancy jungle mega_pine dark_oak cherry poplar` (`*_foliage_placer`); decorators `trunk_vine leave_vine pale_moss creaking_heart cocoa shelf_mushroom beehive alter_ground attached_to_leaves place_on_ground attached_to_logs`.
- `tree.below_trunk_provider` (rule_based provider) replaced `force_dirt`/`dirt_provider` (26.1), and from 26.2 it has no default.

### `worldgen/placed_feature` (`PlacedFeature`)
`{feature: feature holder (ID or inline), placement: [modifiers]}`.
Placement modifier types (`PlacementModifierTypes`): `block_predicate_filter rarity_filter random_chance surface_relative_threshold_filter surface_water_depth_filter biome count noise_based_count noise_threshold_count count_on_every_layer cuboid environment_scan heightmap height_range in_square offset randomly_selected fixed_placement`. 26.3 renamed `random_offset`→`offset` and added `cuboid`, `random_chance` and `randomly_selected`.
```json
{ "feature": "minecraft:ore_iron",
  "placement": [ { "type": "minecraft:count", "count": 90 }, { "type": "minecraft:in_square" },
    { "type": "minecraft:height_range", "height": { "type": "minecraft:trapezoid", "min_inclusive": { "absolute": 80 }, "max_inclusive": { "absolute": 384 } } },
    { "type": "minecraft:biome" } ] }
```
- Height providers: `constant uniform biased_to_bottom very_biased_to_bottom trapezoid weighted_list`.
- Vertical anchors: `{absolute}`, `{above_bottom}`, `{below_top}`, and `{relative_to_sea_level}` (new in 26.3).
- Block predicates (`BlockPredicateType`): `matching_blocks matching_block_tag matching_fluids matching_biomes has_sturdy_face solid replaceable would_survive inside_world_bounds any_of all_of not true unobstructed height_range volume_match`. Each takes an `offset: [x,y,z]`.

### `worldgen/block_state_provider` (new registry, 26.3; `src: net/minecraft/world/level/levelgen/feature/stateproviders/`)
A named, reusable block state provider. Any feature or tree field typed as a provider, and `block_transformer`, can reference one by ID.
- Types: `simple{state}` `weighted{entries}` `noise` `noise_threshold` `dual_noise` `rotated` `randomized_int` `rule_based{rules:[{if_true: block predicate, then: provider}], fallback?}` `copy_properties{source}` `random_block`. 26.3 dropped the `_state_provider`/`_provider` suffixes.
- A bare block state object is shorthand for `simple`.
- `rule_based` falls through to the next rule. If nothing matches and there is no fallback it yields **nothing**, and the feature or transformer places nothing.
```json
{ "type": "minecraft:rule_based",
  "rules": [ { "if_true": { "type": "minecraft:not", "predicate": { "type": "minecraft:matching_block_tag", "tag": "minecraft:cannot_replace_below_tree_trunk" } },
               "then": { "id": "minecraft:dirt" } } ] }
```
(`jar: worldgen/block_state_provider/soil_beneath_tree.json`; others: `podzol_beneath_tree`, `flower_plain`, `flower_meadow`, `flower_flower_forest`, `cave_vines_body/head`, `mangrove_propagule`.)

### `worldgen/carver` (was `configured_carver`; `WorldCarverTypes`: `cave`, `canyon`)
Fields are inlined (26.3).
- `cave`: `probability y count floor_level horizontal_radius_multiplier vertical_radius_multiplier room_vertical_radius_multiplier start_vertical_radius_multiplier? thickness weird_thickness_bias?`.
- Removed: `replaceable` (carvers replace anything except features), `lava_level` (fluid comes from aquifer/sea level), `debug_settings`. `nether_cave` was removed; the `cave` type now covers the Nether.
```json
{ "type": "minecraft:cave", "probability": 0.15,
  "count": { "type": "minecraft:very_biased_to_bottom", "min_inclusive": 0, "max_inclusive": 14 },
  "y": { "type": "minecraft:uniform", "min_inclusive": { "above_bottom": 8 }, "max_inclusive": { "absolute": 180 } },
  "floor_level": { "type": "minecraft:uniform", "min_inclusive": -1.0, "max_exclusive": -0.4 },
  "horizontal_radius_multiplier": { "type": "minecraft:uniform", "min_inclusive": 0.7, "max_exclusive": 1.4 },
  "vertical_radius_multiplier": { "type": "minecraft:uniform", "min_inclusive": 0.8, "max_exclusive": 1.3 },
  "room_vertical_radius_multiplier": { "type": "minecraft:uniform", "min_inclusive": 0.1, "max_exclusive": 0.9 },
  "thickness": { "type": "minecraft:trapezoid", "min": 0.0, "max": 3.0, "plateau": 1.0 },
  "weird_thickness_bias": true }
```

### `worldgen/noise_settings` (`NoiseGeneratorSettings`)
Fields:
- `noise{min_y,height}` (26.3 removed `size_horizontal`/`size_vertical`; they now live in `interpolated`)
- `default_block`, `default_fluid` (block states)
- `noise_router{...}` (26.3: `barrier`/`fluid_*`/`lava` moved to `aquifers`, `vein_*` moved to ore-vein material rules, and `preliminary_surface_level` was renamed `chunk_surface_level`)
- `material_rule` (was `surface_rule`; ID or inline)
- `spawn_target` (list of `{density function ID: [min,max]}` hypercubes)
- `sea_level`, `disable_mob_generation`
- `aquifers?{barrier,fluid_level_floodedness,fluid_level_spread,lava,surface_level,exclusion}` (absent = no aquifers; replaces `aquifers_enabled`)
- `legacy_random_source`
- `debug_functions?[{label,function}]` (shown in the F3 `chunk_generation_stats` entry)

The beardifier is no longer added implicitly. Use the `beardifier` density function in `final_density`.
```json
{ "sea_level": 63, "disable_mob_generation": false, "legacy_random_source": false,
  "default_block": "minecraft:stone", "default_fluid": "minecraft:water",
  "noise": { "min_y": -64, "height": 384 },
  "material_rule": "minecraft:overworld",
  "noise_router": { "final_density": "minecraft:overworld/final_density", "continents": "minecraft:overworld/continents", "erosion": "minecraft:overworld/erosion", "...": "..." },
  "aquifers": { "barrier": { "type": "minecraft:noise", "noise": "minecraft:aquifer_barrier", "xz_scale": 1.0, "y_scale": 0.5 }, "...": "..." },
  "spawn_target": [ { "minecraft:overworld/continents": [-0.11, 1.0], "minecraft:overworld/erosion": [-1.0, 1.0] } ] }
```
(heavily trimmed `jar: worldgen/noise_settings/overworld.json`; read the real file before editing.)

### `worldgen/material_rule` and `worldgen/material_condition` (new registries, 26.3; `MaterialRules`)
These are the old surface rules, now referenceable by ID. Vanilla has 42 rules (`overworld`, `bedrock_floor`, `overworld/surface`, `overworld/iron_ore_vein`, ...) and 8 conditions (`on_floor`, `under_floor`, `on_ceiling`, ...).
- Rule types: `block{result_state}`, `sequence{sequence}`, `condition{if_true, then_run}`, `bandlands`, `ore_vein` (26.3; ore veins are now material rules), and `noise_gradient{noise, gradient:[{state?}]}` (26.2, per changelog).
- Condition types: `biome vertical_gradient y_above water temperature steep not hole above_preliminary_surface stone_depth noise_threshold{…, is_3d?}`.
- Both accept inline objects or IDs anywhere.
```json
{ "type": "minecraft:condition",
  "if_true": { "type": "minecraft:vertical_gradient", "random_name": "minecraft:bedrock_floor", "true_at_and_below": { "above_bottom": 0 }, "false_at_and_above": { "above_bottom": 5 } },
  "then_run": { "type": "minecraft:block", "result_state": "minecraft:bedrock" } }
```
```json
{ "type": "minecraft:sequence", "sequence": [ "minecraft:bedrock_floor", "minecraft:overworld/copper_ore_vein", "minecraft:overworld/iron_ore_vein",
  { "type": "minecraft:condition", "if_true": { "type": "minecraft:above_preliminary_surface" }, "then_run": "minecraft:overworld/surface" },
  "minecraft:overworld/underground" ] }
```
(`jar: worldgen/material_rule/bedrock_floor.json`, `overworld.json`; condition example `jar: worldgen/material_condition/on_ceiling.json` = `{type:"stone_depth", offset:0, add_surface_depth:false, secondary_depth_range:0, surface_type:"ceiling"}`.)

### `worldgen/density_function` (`src: net/minecraft/world/level/levelgen/densityfunction/`)
26.3 rewrote this registry and evaluation uses single precision.
- Types: `constant{value}` `noise{noise, xz_scale, y_scale, shift_x?, shift_z?}` (it absorbed `shifted_noise`) `shift`/`shift_a`/`shift_b{noise}` `gradient{axis, from_coordinate, from_value, to_coordinate, to_value}` (was `y_clamped_gradient`)
- `add sub mul div min max{left,right}` `abs square cube half_negative quarter_negative squeeze negate reciprocal sqrt log sign{input}` `floor round ceil truncate` `pow{base,exponent}` `lerp{alpha, ...}`
- `clamp` `range_choice` `interval_select{input, thresholds, functions}` (26.2, replaced `weird_scaled_sampler`) `interpolated{input, cell sizes}` `cache{input}` (was `cache_once`; `flat_cache`, `cache_2d` and `cache_all_in_cell` were removed) `slice` `blend_density` `old_blended_noise` `spline` `find_top_surface` `distance_to_point` `end_outer_islands` (was `end_islands`) `beardifier`
- 26.3 field renames: `argument`→`input`/`value`, `argument1/2`→`left/right`.
- Field names above are partly from the changelog. Check `densityfunction/op/*.java` and `jar: worldgen/density_function/**` before writing complex ones.
```json
{ "type": "minecraft:cache", "input": { "type": "minecraft:noise", "noise": "minecraft:continentalness", "shift_x": "minecraft:shift_x", "shift_z": "minecraft:shift_z", "xz_scale": 0.25, "y_scale": 0.0 } }
```
(`jar: worldgen/density_function/overworld/continents.json`; `y.json` = `{type:"gradient", axis:"y", from_coordinate:-4064, from_value:-4064.0, to_coordinate:4062, to_value:4062.0}`.)

### `worldgen/noise` (`NormalNoise`, reworked 26.3)
Fields: `base_octave` (was `firstOctave`), `octave_count` (1..32), `base_amplitude`, `amplitude_modifiers?` (was `amplitudes`), `normalize?` (bool or `"legacy"`).
```json
{ "base_amplitude": 0.9494731054427981, "base_octave": -7, "octave_count": 2 }
```
(`jar: worldgen/noise/pillar.json`)

### `worldgen/structure` (`Structure`, `StructureType`)
- Common fields: `type`, `biomes`, `step`, `spawn_overrides`, `terrain_adaptation?` (`none|bury|beard_thin|beard_box|encapsulate`).
- Types: `buried_treasure desert_pyramid end_city fortress igloo jigsaw jungle_temple mineshaft nether_fossil ocean_monument ocean_ruin ruined_portal shipwreck stronghold swamp_hut woodland_mansion`.
- `jigsaw` fields: `start_pool size start_height start_jigsaw_name? project_start_to_heightmap? max_distance_from_center use_expansion_hack pool_aliases? dimension_padding? liquid_settings?`.
```json
{ "type": "minecraft:jigsaw", "biomes": "#minecraft:has_structure/village_plains", "step": "surface_structures",
  "start_pool": "minecraft:village/plains/town_centers", "size": 6, "start_height": { "absolute": 0 },
  "project_start_to_heightmap": "WORLD_SURFACE_WG", "max_distance_from_center": 80,
  "terrain_adaptation": "beard_thin", "use_expansion_hack": true, "spawn_overrides": {} }
```
`spawn_overrides` maps a category to `{bounding_box: "piece"|"full", spawns: [...]}`. Structures are placed only through a `structure_set`.

### `worldgen/structure_set`
`{structures:[{structure, weight}], placement}`.
- Placement types: `random_spread{spacing, separation, spread_type?, salt, ...}`, `concentric_rings{distance, spread, count, preferred_biomes}`, `dimension_origin` (26.3; a single structure at the dimension's spawn_target / chunk 0,0).
- Shared placement fields: `salt`, `frequency?`, `frequency_reduction_method?`, `exclusion_zone?`, `locate_offset?`.
```json
{ "placement": { "type": "minecraft:random_spread", "salt": 14357618, "separation": 8, "spacing": 32 },
  "structures": [ { "structure": "minecraft:igloo", "weight": 1 } ] }
```

### `worldgen/template_pool`
`{fallback, elements:[{element, weight}]}`.
- Element types: `single_pool_element{location, processors, projection, override_liquid_settings?}`, `legacy_single_pool_element`, `list_pool_element{elements}`, `feature_pool_element{feature, projection}` (26.3: connects to any jigsaw name), `empty_pool_element`.
```json
{ "fallback": "minecraft:empty",
  "elements": [ { "weight": 1, "element": { "element_type": "minecraft:feature_pool_element", "feature": "minecraft:jungle_tree", "projection": "rigid" } } ] }
```

### `worldgen/processor_list`
`{processors:[{processor_type, ...}]}`.
- Processor types: `blackstone_replace block_age block_ignore block_rot capped gravity jigsaw_replacement lava_submerged_block nop protected_blocks rule`.
- `rule.rules[]`: `{input_predicate, location_predicate, position_predicate?, output_state, block_entity_modifier?}`.
- Pos rule tests: `always_true linear_pos axis_aligned_linear_pos`.
- 26.2: `block_rot` sees the output of earlier processors, and `protected_blocks.value` accepts an ID/list.
```json
{ "processors": [ { "processor_type": "minecraft:rule", "rules": [
  { "input_predicate": { "predicate_type": "minecraft:random_block_match", "block": "minecraft:cobblestone", "probability": 0.8 },
    "location_predicate": { "predicate_type": "minecraft:always_true" }, "output_state": "minecraft:mossy_cobblestone" } ] } ] }
```

### `worldgen/world_preset`
`{dimensions: {"minecraft:overworld": LevelStem, ...}}`. A preset shows in the create-world screen if it is tagged in `tags/worldgen/world_preset/normal.json`, and it can be used by the server's `level-type`. 26.2 added a hidden `flat_all_dimensions` preset.
```json
{ "dimensions": {
  "minecraft:overworld": { "type": "minecraft:overworld", "generator": { "type": "minecraft:noise", "settings": "minecraft:overworld", "biome_source": { "type": "minecraft:multi_noise", "preset": "minecraft:overworld" } } },
  "minecraft:the_nether": { "type": "minecraft:the_nether", "generator": { "type": "minecraft:noise", "settings": "minecraft:nether", "biome_source": { "type": "minecraft:multi_noise", "preset": "minecraft:nether" } } },
  "minecraft:the_end": { "type": "minecraft:the_end", "generator": { "type": "minecraft:noise", "settings": "minecraft:end", "biome_source": { "type": "minecraft:the_end" } } } } }
```

### `worldgen/flat_level_generator_preset`
`{display: item, settings: {layers:[{block,height}], biome, features, lakes, structure_overrides?}}`.
```json
{ "display": "minecraft:water_bucket",
  "settings": { "biome": "minecraft:deep_ocean", "features": false, "lakes": false,
    "layers": [ { "block": "minecraft:bedrock", "height": 1 }, { "block": "minecraft:water", "height": 90 } ],
    "structure_overrides": ["minecraft:ocean_ruins", "minecraft:shipwrecks"] } }
```

### `worldgen/multi_noise_biome_source_parameter_list`
`{preset: "minecraft:overworld"|"minecraft:nether"}`. Vanilla files are just preset pointers (the parameter tables are in code). For custom tables put `biomes:[{biome, parameters:{temperature,humidity,continentalness,erosion,weirdness,depth,offset}}]` inline in the dimension's `multi_noise` biome source.

---

## 3. Items, combat, enchanting

### `enchantment` (`src: net/minecraft/world/item/enchantment/Enchantment.java`)
Fields:
- `description`, `exclusive_set?`, `supported_items`, `primary_items?`, `weight`, `max_level`
- `min_cost{base,per_level_above_first}`, `max_cost{...}`, `anvil_cost`
- `slots` (`any|mainhand|offhand|hand|feet|legs|chest|head|armor|body|saddle`)
- `effects` (map of effect component → list)
```json
{ "description": { "translate": "enchantment.minecraft.sharpness" },
  "supported_items": "#minecraft:enchantable/sharp_weapon", "primary_items": "#minecraft:enchantable/melee_weapon",
  "exclusive_set": "#minecraft:exclusive_set/damage", "weight": 10, "max_level": 5,
  "min_cost": { "base": 1, "per_level_above_first": 11 }, "max_cost": { "base": 21, "per_level_above_first": 11 },
  "anvil_cost": 1, "slots": ["mainhand"],
  "effects": { "minecraft:damage": [ { "effect": { "type": "minecraft:add", "value": { "type": "minecraft:linear", "base": 1.0, "per_level_above_first": 0.5 } } } ] } }
```
- Effect components (`EnchantmentEffectComponents`): `damage_protection damage_immunity damage smash_damage_per_fallen_block knockback armor_effectiveness post_attack post_piercing_attack hit_block item_damage equipment_drops location_changed tick ammo_use projectile_piercing projectile_spawned projectile_spread projectile_count trident_return_acceleration fishing_time_reduction fishing_luck_bonus block_experience mob_experience repair_with_xp attributes crossbow_charge_time crossbow_charging_sounds trident_sound prevent_equipment_drop prevent_armor_change trident_spin_attack_strength`.
- Value effects: `add all_of multiply remove_binomial exponential set`.
- Entity effects: `all_of apply_mob_effect change_item_damage damage_entity explode ignite apply_impulse apply_exhaustion play_sound replace_block replace_disk run_function set_block_properties spawn_particles summon_entity`. Location effects add `attribute`.
- Level-based values: `clamped fraction levels_squared linear exponent lookup`.
- Enchantability (which items can be enchanted) comes from item tags under `tags/item/enchantable/*`. Enchanting-table availability comes from `tags/enchantment/in_enchanting_table` etc.

### `enchantment_provider` (`EnchantmentProvider` types `by_cost`, `by_cost_with_difficulty`, `single`)
Used by mob spawn equipment, raids and enderman drops via loot/spawn code.
```json
{ "type": "minecraft:by_cost_with_difficulty", "enchantments": "#minecraft:on_mob_spawn_equipment", "min_cost": 5, "max_cost_span": 17 }
{ "type": "minecraft:single", "enchantment": "minecraft:piercing", "level": 1 }
```

### `damage_type`
Fields: `message_id`, `scaling` (`never|when_caused_by_living_non_player|always`), `exhaustion`, `effects?` (`hurt|thorns|drowning|burning|poking|freezing`), `death_message_type?` (`default|fall_variants|intentional_game_design`). Behaviour flags live in `tags/damage_type/*` (`bypasses_armor`, `is_fire`, `no_knockback`, ...). 26.2 added `minecraft:sulfur_cube_hot`.
```json
{ "exhaustion": 0.1, "message_id": "arrow", "scaling": "when_caused_by_living_non_player" }
```

### `trim_material` / `trim_pattern`
- 26.3 trim_material: `{description, palette_id}`. `asset_name` was replaced by `palette_id`, and `override_armor_assets` was removed (overrides now live in the resource pack's equipment asset).
- trim_pattern: `{asset_id, description, decal}`.
- Trim materials are tied to items through the `provides_trim_material` component. Patterns are referenced by smithing_trim recipes (`pattern`).
```json
{ "description": { "color": "#ECECEC", "translate": "trim_material.minecraft.iron" }, "palette_id": "minecraft:trim/iron" }
{ "asset_id": "minecraft:coast", "decal": false, "description": { "translate": "trim_pattern.minecraft.coast" } }
```

### `banner_pattern`, `decorated_pot_pattern`
- banner_pattern: `{asset_id, translation_key}`. Loom placement is via `tags/banner_pattern/*` and items with the `provides_banner_patterns` component.
- decorated_pot_pattern (synced, data-driven): `{asset_id}`. The sherd item → pattern link is the `provides_pottery_pattern` component.
```json
{ "asset_id": "minecraft:creeper", "translation_key": "block.minecraft.banner.creeper" }
{ "asset_id": "minecraft:blade_pottery_pattern" }
```

### `instrument`, `jukebox_song`, `painting_variant`
```json
{ "description": { "translate": "instrument.minecraft.ponder_goat_horn" }, "range": 256.0, "sound_event": "minecraft:item.goat_horn.sound.0", "use_duration": 7.0 }
{ "comparator_output": 1, "description": { "translate": "jukebox_song.minecraft.13" }, "length_in_seconds": 178.0, "sound_event": "minecraft:music_disc.13" }
{ "asset_id": "minecraft:kebab", "width": 1, "height": 1, "title": { "translate": "painting.minecraft.kebab.title", "color": "yellow" }, "author": { "translate": "painting.minecraft.kebab.author", "color": "gray" } }
```
- instrument (26.3): optional `durability_damage`, and `use_duration` may be 0.
- painting placeability: `tags/painting_variant/placeable`.
- `sound_event` accepts an ID or an inline `{sound_id, range?}`.

---

## 4. Mob variants and sound variants

Variants are synced registries, and each one is validated `RegistryValidator.nonEmpty()`, so **never empty a variant registry**.
- Selection uses `spawn_conditions: [{priority, condition?}]`, and the highest-priority matching entry wins.
- Condition types (`SpawnConditions`): `biome{biomes}`, `structure{structures}`, `moon_brightness{range}`.
- Entities store the variant as the component `minecraft:<mob>/variant` (and `<mob>/sound_variant`), which you can use in `summon`/`data`/predicates.

| Registry | Fields (source codec) | Vanilla example |
|---|---|---|
| `wolf_variant` | `assets{wild,tame,angry}`, `baby_assets{...}`, `spawn_conditions` | `pale.json` |
| `wolf_sound_variant` | `adult_sounds`, `baby_sounds` (each `ambient_sound death_sound growl_sound hurt_sound pant_sound whine_sound step_sound`) | `classic.json` (7 vanilla) |
| `cat_variant` | `asset_id`, `baby_asset_id`, `spawn_conditions` | `tabby.json` |
| `cat_sound_variant` (26.1) | `adult_sounds`/`baby_sounds`: `ambient_sound stray_ambient_sound hiss_sound hurt_sound death_sound eat_sound beg_for_food_sound purr_sound purreow_sound` | `classic`, `royal` |
| `pig_variant` | `asset_id`, `baby_asset_id`, `model?` (`normal`/`cold`), `spawn_conditions` | `temperate.json` |
| `pig_sound_variant` (26.1) | `adult_sounds`/`baby_sounds`: `ambient_sound hurt_sound death_sound step_sound eat_sound` | `classic`, `mini`, `big` |
| `cow_variant` | `asset_id`, `baby_asset_id`, `model?` (`normal|cold|warm`), `spawn_conditions` | `cold.json` |
| `cow_sound_variant` (26.1) | **flat** (no adult/baby split): `ambient_sound hurt_sound death_sound step_sound` | `classic`, `moody` |
| `chicken_variant` | `asset_id`, `baby_asset_id`, `model?`, `spawn_conditions` | `warm.json` |
| `chicken_sound_variant` (26.1) | `adult_sounds`/`baby_sounds`: `ambient_sound hurt_sound death_sound step_sound` | `classic`, `picky` |
| `frog_variant` | `asset_id`, `spawn_conditions` | `warm.json` |
| `zombie_nautilus_variant` (1.21.11) | `asset_id`, `model?` (`warm`), `spawn_conditions` | `temperate`, `warm` |

```json
{ "asset_id": "minecraft:entity/cow/cow_cold", "baby_asset_id": "minecraft:entity/cow/cow_cold_baby", "model": "cold",
  "spawn_conditions": [ { "priority": 1, "condition": { "type": "minecraft:biome", "biomes": "#minecraft:spawns_cold_variant_farm_animals" } } ] }
{ "ambient_sound": "minecraft:entity.cow_moody.ambient", "death_sound": "minecraft:entity.cow_moody.death", "hurt_sound": "minecraft:entity.cow_moody.hurt", "step_sound": "minecraft:entity.cow_moody.step" }
```
- `asset_id` is a texture path under `assets/<ns>/textures/` without `.png`, so the texture has to come from a **resource pack**.
- 26.1 added the `baby_*` assets/sounds. The wolf sound fields moved under `adult_sounds`.
- Sound variants are picked at random among all entries (no spawn conditions; UNVERIFIED exact selection logic).

### `sulfur_cube_archetype` (26.2, `src: net/minecraft/world/entity/SulfurCubeArchetype.java`)
The Sulfur Cube mob takes behaviour from the item it has absorbed: the item's tag membership picks the archetype.
Fields:
- `items` (item holder set, typically `#minecraft:sulfur_cube_archetype/<name>`), `buoyant`
- `explosion?{fuse>0, power≥0, causes_fire}`
- `contact_damage?{amount (float provider), damage_type, attribute_to_source}`
- `knockback_modifiers{horizontal_power, vertical_power}`
- `attribute_modifiers:[{attribute, id, amount, operation}]`
- `sound_settings{hit_sound, push_sound, push_sound_impulse_threshold, push_sound_cooldown}`

Vanilla has 12: `regular bouncy slow_bouncy fast_flat slow_flat light fast_sliding slow_sliding high_resistance sticky explosive hot`.
```json
{ "items": "#minecraft:sulfur_cube_archetype/explosive", "buoyant": true,
  "explosion": { "causes_fire": false, "fuse": 120, "power": 3 },
  "knockback_modifiers": { "horizontal_power": 0.4125, "vertical_power": 0.09 },
  "attribute_modifiers": [ { "attribute": "minecraft:bounciness", "id": "minecraft:explosive_add_bounciness", "amount": 0.5, "operation": "add_value" } ],
  "sound_settings": { "hit_sound": "minecraft:entity.sulfur_cube.explosive.hit", "push_sound": "minecraft:entity.sulfur_cube.explosive.push", "push_sound_cooldown": 0.7, "push_sound_impulse_threshold": 0.1 } }
```
The absorbed item is the `minecraft:sulfur_cube_content` component on the entity/bucket.

---

## 5. Villager trading (26.1)

### `villager_trade` (`src: net/minecraft/world/item/trading/VillagerTrade.java`, `TradeCost.java`)
Fields:
- `wants{id, count? (int provider, default 1), components? (exact component map)}`, `additional_wants?`
- `gives` (ItemStackTemplate)
- `max_uses?` (default 4), `xp?` (default 1), `reputation_discount?` (float, default 0)
- `merchant_predicate?` (loot condition; context `villager_trade`: `this` = trader, `origin`)
- `given_item_modifier?` (item modifier, renamed from `given_item_modifiers` in 26.3; one that yields an empty stack discards the trade)
- `double_trade_price_enchantments?` (enchantment holder set)
```json
{ "wants": { "id": "minecraft:emerald" },
  "gives": { "id": "minecraft:iron_pickaxe" }, "max_uses": 1, "reputation_discount": 0.2,
  "given_item_modifier": [
    { "type": "minecraft:enchant_with_levels", "include_additional_cost_component": true, "levels": { "type": "minecraft:uniform", "min": 5, "max": 19 }, "options": "#minecraft:on_traded_equipment" },
    { "type": "minecraft:filtered", "item_filter": { "items": "minecraft:iron_pickaxe", "predicates": { "minecraft:enchantments": [{}] } }, "on_fail": { "type": "minecraft:discard" } } ] }
```
- `include_additional_cost_component` puts the transient `additional_trade_cost` component on the result, which raises the emerald cost.
- The wiki says this field does not accept item modifier IDs. The source codec is `LootItemFunctions.CODEC`, which accepts IDs, so it is UNVERIFIED whether validation rejects them.

### `trade_set` (`TradeSet.java`)
`{trades: villager_trade holder set (usually a tag), amount: int provider, allow_duplicates?: false, random_sequence?}`.
- Vanilla file keys: `<profession>/level_<1..5>` and `wandering_trader/{buying,common,uncommon}`.
- The profession → trade-set mapping is **hardcoded** in `VillagerProfession.tradeSetsByLevel` (built-in registry). Data packs change trades by overriding those trade_set files or the villager_trade tags (`tags/villager_trade/<profession>/level_N`, `common_smith/level_N`). They cannot add new professions.
```json
{ "amount": 2, "random_sequence": "minecraft:trade_set/armorer/level_1", "trades": "#minecraft:armorer/level_1" }
```
`tags/villager_trade/armorer/level_1.json` = `{"values":["#minecraft:common_smith/level_1","minecraft:armorer/1/emerald_iron_leggings", ...]}`. The `trade_rebalance` experimental pack (`jar: datapacks/trade_rebalance/data/minecraft/...`) is a complete worked example of overriding them.

---

## 6. Block interactions and numbers (26.3)

### `block_transformer` (`src: net/minecraft/core/component/BlockTransformer.java`)
A file is a **list** of rules, tried in order. This is how axes strip, shovels flatten and hoes till.
The item points at a transformer through the `minecraft:block_transformer` component (vanilla: `minecraft:axe`, `minecraft:shovel`, `minecraft:hoe`). A pack can override those three files to change every axe/shovel/hoe, or give a custom item the component (in `give`, loot `set_components`, etc.).
Rule fields:
- `block_state_provider` (required; returning nothing means try the next rule)
- `sound?`, `particle?` (`none|scrape|wax_on|wax_off`), `disallowed_faces?`
- `loot?` (loot table key, e.g. `minecraft:till/rooted_dirt`), `drop_strategy?` (`from_middle|clicked_face`)
- `update_from_neighbors?` (true), `transform_type?` (`single_block|copper_chest`)
- `consume_on_use?` (true; stackable items), `item_damage_per_use?` (0; non-stackable)
```json
[ { "block_state_provider": { "type": "minecraft:rule_based", "rules": [
      { "if_true": { "type": "minecraft:all_of", "predicates": [
          { "type": "minecraft:matching_block_tag", "tag": "minecraft:turns_into_dirt_path" },
          { "type": "minecraft:matching_block_tag", "tag": "minecraft:air", "offset": [0, 1, 0] } ] },
        "then": { "id": "minecraft:dirt_path" } } ] },
    "disallowed_faces": ["down"], "item_damage_per_use": 1, "sound": "minecraft:item.shovel.flatten" } ]
```
(`jar: block_transformer/shovel.json`. In `axe.json`, `copy_properties{source:{id:"minecraft:stripped_oak_log",properties:{axis:"y"}}}` keeps the axis.)

### `context_int_provider` / `context_float_provider` (26.3, reloadable; `src: net/minecraft/world/level/storage/loot/providers/number/{ints,floats}/`)
Named, reusable number providers evaluated against a loot context. A file holds one provider: a bare number = constant, or a typed object (type list in `datapacks.md` §8). They are used by:
- `/compute`, `data modify ... compute`
- loot `rolls`/`set_count`/etc. by ID
- item components `cooking_fuel` (burn time and speed: `cooking/time_*`, `cooking/speed_default`), `brewing_fuel` (`brewing/uses_default`, `brewing/speed_default`) and `compostable` (`compostable/low|low_medium|medium|medium_high|always_add_one`)

Contexts: `container_process` for furnace/brewing fuel (container, block_entity, block_state, origin), and the composter context with block_state.
```json
1.0
```
(`jar: context_float_provider/cooking/normal_speed_multiplier.json`; a whole file can be a bare number.)
```json
{ "type": "minecraft:number_dispatcher",
  "cases": [ { "condition": { "type": "minecraft:match_block", "blocks": "minecraft:composter", "state": { "level": "0" } }, "value": 1 } ],
  "default": { "type": "minecraft:weighted_list", "distribution": [ { "data": 1, "weight": 65 }, { "data": 0, "weight": 35 } ] } }
```
(`jar: context_int_provider/compostable/medium.json`.)
- `cooking/speed_default` = `conditional{condition:"minecraft:block/fast_cooking", on_true:"minecraft:cooking/fast_speed_multiplier", on_false:"...normal_speed_multiplier"}` (`ContextFloatProviders.bootstrap`). This is why blast furnace and smoker recipes now use the same `cookingtime` as furnaces.
- Overriding `context_int_provider/cooking/time_coal.json` changes coal's burn time globally, with no mod required.

### `slot_source` (26.3, reloadable; `src: net/minecraft/world/item/slot/SlotSources.java`)
Named slot selections used by `/item`, `execute if items|slots` and the `slots` loot entry. Vanilla ships none.
Types:
- `slot_range{slots: "container.*", source?: container|this|attacker|...|block_entity}` (default source `container`)
- `group{terms}`, which is also the inline list form
- `filtered{slot_source, item_filter}`
- `limit_slots{slot_source, limit}`
- `contents{slot_source, component}` (e.g. bundle/container contents)
- `empty`
```json
{ "type": "minecraft:filtered", "slot_source": { "type": "minecraft:slot_range", "slots": "hotbar.*" }, "item_filter": { "items": "#minecraft:swords" } }
```
Usage: `execute if items entity @s mypack:hotbar_swords * run ...`, or inline `item replace entity @s {type:"minecraft:slot_range",slots:"armor.*"} with minecraft:air`.

### `predicate`, `item_modifier`, `loot_table`, `advancement`, `recipe`
These are reloadable. Shapes are in `datapacks.md` §8–10.

---

## 7. Chat, UI, misc

### `chat_type`
`{chat: decoration, narration: decoration}`, decoration = `{translation_key, parameters: [sender|target|content], style?}`. Used by `say`, `msg`, `me`, `teammsg` and chat. The server sends the type, and the client formats it with its own language file.
```json
{ "chat": { "translation_key": "commands.message.display.incoming", "parameters": ["sender","content"], "style": { "color": "gray", "italic": true } },
  "narration": { "translation_key": "chat.type.text.narrate", "parameters": ["sender","content"] } }
```

### `dialog` (1.21.6+, `src: net/minecraft/server/dialog/`)
- Types: `notice{action?}`, `confirmation{yes, no}`, `multi_action{actions, columns?, exit_action?}`, `dialog_list{dialogs, columns?, button_width?, exit_action?}`, `server_links{columns?, button_width?, exit_action?}`.
- Common fields: `title`, `external_title?`, `can_close_with_escape?`, `pause?`, `after_action?` (`close|none|wait_for_response`), `body?` (`plain_message{contents,width?}` | `item{item, description?, show_decorations?, show_tooltip?, width?, height?}`), `inputs?` (`text`, `boolean`, `number_range`, `single_option`, each with `key`).
- Buttons: `{label, tooltip?, width?, action?}`. Actions are any static click event (`open_url`, `run_command`, ...), `dynamic/run_command{template}` with `$(key)` from inputs, or `dynamic/custom{id, additions?}`.
- Hooks: tags `tags/dialog/pause_screen_additions` and `tags/dialog/quick_actions`. Show one with `/dialog show` or the `show_dialog` click event.
```json
{ "type": "minecraft:notice", "title": "Welcome", "body": { "type": "minecraft:plain_message", "contents": "Hello!" },
  "action": { "label": "OK", "action": { "type": "minecraft:run_command", "command": "/trigger welcomed" } } }
```
The vanilla `dialog/*.json` files are only `dialog_list`/`server_links` menus. The notice example above is assembled from the codec fields (`NoticeDialog`, `ActionButton`, `CommonButtonData`, `body/PlainMessage`), so treat the exact nesting as UNVERIFIED and validate it with misode's generator.

### `trial_spawner` (`TrialSpawnerConfig`)
Fields: `spawn_range? total_mobs? simultaneous_mobs? total_mobs_added_per_player? simultaneous_mobs_added_per_player? ticks_between_spawn? spawn_potentials loot_tables_to_eject? items_to_drop_when_ominous?`. Spawner blocks reference configs by ID (`normal_config`/`ominous_config` in block entity data).
```json
{ "simultaneous_mobs": 1.0, "simultaneous_mobs_added_per_player": 0.5, "total_mobs": 2.0, "total_mobs_added_per_player": 1.0, "ticks_between_spawn": 20,
  "spawn_potentials": [ { "weight": 1, "data": { "entity": { "id": "minecraft:breeze" } } } ] }
```
(`jar: trial_spawner/trial_chamber/breeze/normal.json`; 28 vanilla files.)

### `test_environment` / `test_instance`
See `datapacks.md` §13.
```json
{ "type": "minecraft:all_of", "definitions": [] }
{ "type": "minecraft:function", "environment": "minecraft:default", "function": "minecraft:always_pass", "max_ticks": 1, "setup_ticks": 1, "required": false, "structure": "minecraft:empty" }
```
(`jar: test_environment/default.json`, `test_instance/always_pass.json`. For pack-only tests use `"type":"minecraft:block_based"`.)

---

## 8. Registries that are *not* data-driven (common surprises)

Built-in, code-only, cannot be added by a data pack: blocks, items, entity types, mob effects, potions, attributes, particle types, sound events (sounds are added via resource packs, and `sound_event` fields accept inline `{sound_id}`), villager professions/types, POI types (tags only), game rules, stats, menus, `test_function`, `trigger_type`, every `*_type` registry (feature_type, loot_function_type, ...), environment attributes, world clock *mechanics*, timeline attribute types, and default item components. Use Fabric or other mods for these. Tags on them are data-driven.

## 9. Where to look

- Registry list and codecs: `src: net/minecraft/resources/RegistryDataLoader.java`. Each `DIRECT_CODEC`/`CODEC` field is the authoritative schema.
- Vanilla bootstrap code (shows intent and defaults): `*s.bootstrap(BootstrapContext)` classes, e.g. `ContextIntProviders`, `TradeSets`, `VillagerTrades`, `Timelines`, `WorldClocks`, `SulfurCubeArchetypes`, `BlockTransformers`.
- Real data: `jar:` (per-type examples above). Per-snapshot diffs: `github.com/misode/mcmeta` branches `<version>-diff`. Schemas: `SpyglassMC/vanilla-mcdoc/java/data/**` (`#[since="26.3"]` annotations).
