# Vanilla data packs for Minecraft Java 26.3 (data pack format 121.0)

Scope: how to write, load, test and debug vanilla data packs (pack.mcmeta, folder layout, functions/macros, every command, selectors, components-in-commands, loot/predicates/item modifiers, advancements, recipes, tags, game tests, tooling, headless server testing). Per-registry JSON shapes are in `datapack-registries.md`.
Verified 2026-11-14 against the 26.3 release (`version.json`: `world_version` 5023, `data_major` 121, `data_minor` 0, `resource_major` 97, `resource_minor` 1), decompiled source in `build/mcsrc/26.3/common`, vanilla data in `build/mcsrc/26.3/vanilla/data/minecraft` (both unpacked by `tools/mcsrc.py`; first written against a temporary copy in `/tmp/claude-0/mcsrc`), and the minecraft.wiki changelogs for 26.1/26.2/26.3.

Path shorthand: `src:` = `build/mcsrc/26.3/common/`, `jar:` = `build/mcsrc/26.3/vanilla/data/minecraft/` (run `python3 tools/mcsrc.py` to create them).
Packed shapes in this file were load-tested on the vanilla 26.3 server by `datapacks/refpack` (`tools/packload.py`) on 2026-11-14 where marked "verified on the server".

---

## 0. What breaks packs written for 1.21.x (read this first)

Most of these changed in 26.3. If a pack was written for 1.21.11 or earlier, expect all of them to apply.

| Area | Old (≤1.21.11 / early 26.x) | 26.3 | Source |
|---|---|---|---|
| pack.mcmeta | `pack_format: 48` | `min_format`/`max_format` are required for formats >81. `supported_formats` gives an error if min >81 | `src: net/minecraft/server/packs/metadata/pack/PackFormat.java` |
| Loot functions | `"function": "minecraft:set_count"` | `"type": "minecraft:set_count"` | `LootItemFunctions.TYPED_CODEC` dispatches on `type` |
| Loot conditions / predicates | `"condition": "minecraft:..."` | `"type": "minecraft:..."` (required, no default) | `LootItemCondition.DIRECT_CODEC` |
| Function and condition lists | `"functions": [...]`, `"conditions": [...]` | `"modifier"` takes one object, an ID, or a list (a list is shorthand for `sequence`). `"condition"` takes one object or a predicate ID. A list is **not** allowed, so use `all_of` | `LootPool`, `LootPoolEntryContainer`, `LootItemConditionalFunction` |
| Inverted / composite conditions | `inverted.term`, `all_of.terms` | same keys. `terms` also accepts IDs, `#tags` and mixed lists | `InvertedLootItemCondition`, `CompositeLootItemCondition` |
| `reference` condition/function | `{"condition":"reference","name":...}` | removed. Use the bare string ID, e.g. `"condition": "mypack:is_night"` | 26.3 changelog |
| `block_state_property` | condition | removed. Use `match_block` (`blocks`, `state`, `nbt`, `components`, `predicates`) | `LootItemConditionTypes` |
| `value_check` | one type | split into `int_value_check` and `float_value_check` (`value` + `test` range) | `IntValueCheck`, `FloatValueCheck` |
| Number providers | `{"type":"uniform","min":..}` or untyped `{min,max}` | Typed **context_int_provider** / **context_float_provider**. `type` is mandatory. Renames: `sum`→`add`, `product`→`mul`, `minimum`→`min`, `maximum`→`max`, `average`→`avg`. `score` and `binomial` are int-only, `enchantment_level` is float-only | `src: net/minecraft/world/level/storage/loot/providers/number/{ints,floats}` |
| Entity predicates (26.2) | `{"type":"minecraft:zombie","effects":{..},"type_specific":{"type":"player",..}}` | map of sub-predicates: `{"minecraft:entity_type":"minecraft:zombie","minecraft:effects":{..},"minecraft:type_specific/player":{..}}`. Unknown keys are rejected | `src: net/minecraft/advancements/predicates/entity/EntitySubPredicates.java` |
| Block states in JSON | `{"Name":"minecraft:x","Properties":{..}}` | `{"id":"minecraft:x","properties":{..}}`, or just `"minecraft:x"` for the default state. Structure NBT palettes are also written as `id`/`properties` | `StateHolder.codec`, `NbtUtils.writeBlockState` |
| Configured features | `worldgen/configured_feature/x.json` = `{"type":..,"config":{..}}` | `worldgen/feature/x.json` with the config fields inlined at the root | `Registries.FEATURE = "worldgen/feature"` |
| Carvers | `worldgen/configured_carver` with a `config` object | `worldgen/carver` with fields inlined | `Registries.CARVER` |
| Surface rules | `noise_settings.surface_rule` | `noise_settings.material_rule`, inline or an ID into the new `worldgen/material_rule` registry | jar `worldgen/noise_settings/overworld.json` |
| Biome mob spawns | `spawners`, `spawn_costs`, `creature_spawn_probability` | `attributes."minecraft:gameplay/natural_mob_spawns"` and `gameplay/creature_world_gen_spawn_probability` | jar `worldgen/biome/plains.json` |
| `/time set day` | named constants | time markers defined on timelines (`day`, `noon`, `night`, `midnight` on the overworld clock). `/time query daytime/day` are gone, so use `time query time` or `time query <timeline>` | `src: net/minecraft/server/commands/TimeCommand.java` |
| `/item`, `execute if items` | `<slot>` | `<slot source>`: a slot range string like before, or an inline SNBT / ID of a `slot_source` | `SlotSourceArgument` |
| Cooking recipes | `cookingtime` optional (default 200, blasting/smoking 100) | `cookingtime` **required**. Smoker and blast furnace speed now comes from fuel/context providers, so vanilla blasting recipes also say 200 | `AbstractCookingRecipe` |
| Brewing | hardcoded | `recipe` files of type `minecraft:brewing` (279 vanilla files in `jar: recipe/brewing/`) | `BrewingRecipe` |
| Villager trades (26.1) | hardcoded | `villager_trade` + `trade_set` registries | `VillagerTrade`, `TradeSet` |
| Fuel / compostables (26.3) | hardcoded / `#brewing_fuel` tag | item components `cooking_fuel`, `brewing_fuel`, `compostable` that reference `context_int_provider`s. Datapacks cannot change default item components, so these need a mod or a per-stack component | `AbstractFurnaceBlockEntity`, `ComposterBlock` |
| Advancement triggers | `recipe_id`, `block`, `loot_table` fields | renamed to `recipes`, `blocks`, `loot_tables` (ID/list/tag). `player` and entity fields take an inline loot condition or a predicate ID, not a list | 26.3 changelog, `jar: advancement/**` |
| `set_loot_table` | `name` | `loot_table_id` | 26.3 changelog |
| `tag` loot entry | `name` | `items` | 26.3 changelog |
| `exploration_map` | turned `map` into `filled_map` | only adds map data, so the entry item must already be `filled_map` | 26.3 changelog |
| `limit_count.limit` | number or range | object only (`min`/`max` int providers) | 26.3 changelog |
| Sign click commands | always ran | ignored unless block entity `allow_op_features:true` | 26.3 changelog |
| Gamerules (1.21.11) | `doDaylightCycle` | snake_case registry IDs: `advance_time`, `keep_inventory`, ... | `src: net/minecraft/world/level/gamerules/GameRules.java` |
| Text components in commands (1.21.5+) | JSON strings | SNBT (`{text:"hi",color:"red"}`). JSON *files* stay JSON | `ComponentArgument` uses `SnbtGrammar` |
| World folder (26.1) | `DIM-1`, `region/` at root | `dimensions/minecraft/{overworld,the_nether,the_end}/...`. `world/datapacks/` is unchanged | 26.1 changelog |
| Server default (26.3) | `white-list=false` | `white-list=true` by default | `DedicatedServerProperties` |

---

## 1. pack.mcmeta

Source: `src: net/minecraft/server/packs/metadata/pack/PackMetadataSection.java`, `PackFormat.java`, `OverlayMetadataSection.java`, `FeatureFlagsMetadataSection.java`, `resources/ResourceFilterSection.java`.

### Minimal 26.3-only pack (vanilla feature packs use exactly this shape)
```json
{
  "pack": {
    "description": "My pack",
    "min_format": 121,
    "max_format": 121
  }
}
```
- A format is `major` or `[major, minor]`. `min_format: 121` means `121.0`. `max_format: 121` means `121.*`, because the top codec fills in `Integer.MAX_VALUE` for the minor (`PackFormat.TOP_CODEC`). Write `[121, 0]` to pin the exact minor.
- `description` is a text component in JSON form (string, object or array).
- To cover every 26.x release: `"min_format": 101, "max_format": 121` (26.1 = 101.1, 26.2 = 107.1, 26.3 = 121.0; 26.4 snapshot 8 = 127.0). This only works if the content is compatible, and in practice 26.3 broke most loot/worldgen JSON, so use overlays.
- `lastPreMinorVersion(SERVER_DATA) = 81`. The validation rules in `IntermediaryFormat.validate`:
  - min_format > 81: `supported_formats` is an **error** ("deprecated starting from pack format 82"). `pack_format` is optional but must lie in range if present.
  - min_format ≤ 81 (supporting old clients too): you **must** also give `supported_formats: [min, ≥81]`, and `pack_format` must be in range. `supported_formats[0]` must equal the min_format major.
  - Only `pack_format` ≤ 81: the legacy single-version form still loads.
  - `min_format` without `max_format` (or the reverse) is an error.

### Multi-version pack with overlays
```json
{
  "pack": {
    "description": "1.21.5 .. 26.3",
    "pack_format": 71,
    "supported_formats": [71, 121],
    "min_format": 71,
    "max_format": 121
  },
  "overlays": {
    "entries": [
      { "min_format": 121, "max_format": 121, "directory": "ov_26_3" },
      { "formats": [71, 81], "min_format": 71, "max_format": 81, "directory": "ov_legacy" }
    ]
  }
}
```
- Overlay entry keys: `min_format`, `max_format`, `formats` (legacy int range) and `directory` (regex `[-_a-zA-Z0-9.]+`). If **any** overlay has an effective min ≤81, **every** overlay must carry `formats` (`validateHolderList` with `requireOldField`).
- An overlay directory mirrors the pack root (`ov_26_3/data/...`). Overlays listed later win over earlier ones and over the root.
- Fabric adds conditional overlays under the key `fabric:overlays` (`FabricOverlayMetadataSection`: `entries[{condition, directory}]`) and per-file `"fabric:load_conditions"` (`src: fabric-api/fabric-resource-conditions-api-v1/.../ResourceConditions.java`). These keys are only honoured with Fabric API loaded.

### Other sections
```json
{
  "features": { "enabled": ["minecraft:trade_rebalance"] },
  "filter": { "block": [ { "namespace": "minecraft", "path": "loot_table/chests/.*" } ] }
}
```
- `features.enabled`: the feature flags in 26.3 are `minecraft:vanilla`, `minecraft:trade_rebalance`, `minecraft:redstone_experiments` and `minecraft:minecart_improvements` (`src: net/minecraft/world/flag/FeatureFlags.java`). The built-in experimental packs live at `jar: datapacks/{trade_rebalance,redstone_experiments,minecart_improvements}/`.
- `filter.block`: a list of `{namespace?, path?}` regexes. Matching files from **lower-priority** packs are hidden, which is how you delete a vanilla file that has no "empty" form.

---

## 2. Folder layout (singular names since 1.21)

```
<pack>/pack.mcmeta
<pack>/pack.png                          (optional)
<pack>/data/<ns>/function/**/*.mcfunction
<pack>/data/<ns>/structure/**/*.nbt      (structure templates; vanilla ships only .nbt)
<pack>/data/<ns>/tags/<registry path>/**/*.json   (e.g. tags/block, tags/item, tags/function, tags/worldgen/biome)
<pack>/data/<ns>/<registry path>/**/*.json
```
The registry path comes from `Registries.elementsDirPath(key)` = `key.identifier().getPath()`, and tags from `"tags/" + path` (`src: net/minecraft/core/registries/Registries.java`).

### Every data-driven registry folder in 26.3
From `src: net/minecraft/resources/RegistryDataLoader.java` (60 registries).

**Static world registries (`WORLD_REGISTRIES`).** These load once when the world/server starts. **`/reload` does NOT reload them**, so restart the server after edits. Most are synced to clients.
`advancement`† `banner_pattern` `block_transformer` `cat_sound_variant` `cat_variant` `chat_type` `chicken_sound_variant` `chicken_variant` `cow_sound_variant` `cow_variant` `damage_type` `decorated_pot_pattern` `dialog` `dimension_type` `enchantment` `enchantment_provider` `frog_variant` `instrument` `jukebox_song` `painting_variant` `pig_sound_variant` `pig_variant` `sulfur_cube_archetype` `test_environment` `test_instance` `timeline` `trade_set` `trial_spawner` `trim_material` `trim_pattern` `villager_trade` `wolf_sound_variant` `wolf_variant` `world_clock` `zombie_nautilus_variant`
`worldgen/biome` `worldgen/block_state_provider` `worldgen/carver` `worldgen/density_function` `worldgen/feature` `worldgen/flat_level_generator_preset` `worldgen/material_condition` `worldgen/material_rule` `worldgen/multi_noise_biome_source_parameter_list` `worldgen/noise` `worldgen/noise_settings` `worldgen/placed_feature` `worldgen/processor_list` `worldgen/structure` `worldgen/structure_set` `worldgen/template_pool` `worldgen/world_preset`
(† `advancement` is actually in the reloadable list below.)

**Dimension registry (`DIMENSION_REGISTRIES`):** `dimension` (LevelStem: `{type, generator}`). Read at world load.

**Reloadable (`RELOADABLE_REGISTRIES`, picked up by `/reload`):** `loot_table` `predicate` `item_modifier` `slot_source` `context_float_provider` `context_int_provider` `advancement` `recipe`. Also reloaded: `function`, and tags of the built-in registries (block, item, entity_type, fluid, game_event, function, potion, point_of_interest_type, ...) via `TagLoader.loadTagsForExistingRegistries`.

**Diff against `jar:`**
- In the source but absent from the vanilla jar (valid, just empty in vanilla): `dimension` (vanilla dimensions come from `worldgen/world_preset`), `item_modifier`, `slot_source`, `tags/function`.
- In the jar but not registries: `datapacks/` (built-in feature packs), `structure/` (NBT templates), `tags/`.
- Vanilla tag folders in 26.3: `tags/{banner_pattern,block,damage_type,dialog,enchantment,entity_type,fluid,game_event,instrument,item,painting_variant,point_of_interest_type,potion,timeline,villager_trade}` and `tags/worldgen/{biome,feature,flat_level_generator_preset,structure,world_preset}`. Any registry can have tags, e.g. `tags/predicate`, `tags/item_modifier`, `tags/context_int_provider`, `tags/trade_set`, `tags/zombie_nautilus_variant`, and **`tags/recipe`** (reloadable registries get tags too: `ResourceManagerRegistryLoadTask` calls `TagLoader.loadTagsForRegistry` for every registry it loads). A recipe tag works wherever a recipe holder set is taken, e.g. `recipe_crafted.recipes: "#mypack:cooking"` (verified on the server: refpack `tags/recipe/ref_cooking.json`, `advancement/cook_anything.json`).
- Brand-new folders since 1.21.11: `block_transformer`, `context_float_provider`, `context_int_provider`, `slot_source`, `sulfur_cube_archetype`, `trade_set`, `villager_trade`, `world_clock`, `*_sound_variant` (cat/pig/cow/chicken; wolf already existed), `worldgen/block_state_provider`, `worldgen/material_rule`, `worldgen/material_condition`, `decorated_pot_pattern` (now data-driven and synced), and `recipe/brewing/*` (a vanilla subfolder only; any recipe path works).
- Renamed: `worldgen/configured_feature`→`worldgen/feature`, `worldgen/configured_carver`→`worldgen/carver`, plus `tags/worldgen/configured_feature`→`tags/worldgen/feature`.

**Gotcha:** the old plural folders (`functions/`, `loot_tables/`, `tags/blocks/`, `advancements/`, ...) were removed in 1.21 and are silently ignored. A file in a wrong folder raises no error.

---

## 3. Functions (`data/<ns>/function/*.mcfunction`)

Parser: `src: net/minecraft/commands/functions/CommandFunction.java`, `MacroFunction.java`, `FunctionBuilder.java`.
- One command per line, with **no leading `/`**. A leading `/` is a hard error that tells you to drop it. `#` starts a comment. `//` is a hard error.
- A trailing `\` continues the line: the next line is trimmed and appended. Max line length is 2,000,000 chars.
- Any parse error means the **whole function fails to load**. The error is logged (`Failed to load function ...`), and `function` then reports an unknown function. Check `logs/latest.log` after `/reload`.
- Function IDs are `ns:path/to/file` with no extension.
- Function tags: `data/minecraft/tags/function/load.json` runs after every (re)load, `tick.json` runs every tick (`ServerFunctionManager.LOAD_FUNCTION_TAG/TICK_FUNCTION_TAG`).
```json
{ "values": ["mypack:load"] }
```

### Macros
- A line starting with `$` is a macro line and may contain `$(name)`. Values come from an NBT compound:
```mcfunction
# mypack:give_n
$give @s $(item) $(count)
```
```mcfunction
function mypack:give_n {item:"minecraft:diamond",count:3}
function mypack:give_n with storage mypack:args
function mypack:give_n with entity @s SelectedItem
function mypack:give_n with block ~ ~-1 ~ components."minecraft:custom_data"
```
- `function <name> [<compound>]` or `function <name> with (block <pos>|entity <target>|storage <id>) [<path>]` (`FunctionCommand`). The path must resolve to a compound.
- Value stringification (`MacroFunction.stringify`): floats/doubles use plain decimal notation (up to 15 digits, no suffix), byte/short/long print as numbers with no suffix, strings are inserted raw without quotes, and compounds/lists print as SNBT. **Inject quoted strings yourself:** `$say "$(text)"`.
- A missing key raises `commands.function.error.missing_argument`. Calling a macro function without arguments is an error.
- Each macro function caches its **8** most recent argument sets (`MAX_CACHE_ENTRIES`). Beyond that every call re-parses its macro lines, so keep hot-path macros' argument variety low.
- `function` on a tag (`#ns:tag`) runs every member.

### return
`return <int>`, `return fail`, `return run <command>` (`ReturnCommand`). `return` ends the current function. The value goes to `execute store result ... run function ...` and to `execute if function` (passes if the return is non-zero and not failed).

### schedule
`schedule function <fn|#tag> <time> [append|replace]`, `schedule clear <fn>`. Times: `20t`, `1s`, `1d`. A scheduled function runs as the server at world spawn, **not** as the original executor.

### Execution limits (gamerules, `src: GameRules.java`)
`max_command_sequence_length` (65536 commands per tick-run), `max_command_forks` (65536), `max_block_modifications` (32768 for fill/clone). The server.properties `function-permission-level` (default 2) is the permission functions run at.

---

## 4. All commands in 26.3

Registered in `src: net/minecraft/commands/Commands.java` (constructor). Implementations are in `src: net/minecraft/server/commands/*.java`, except `test` (`net/minecraft/gametest/framework/TestCommand.java`).

**Always registered:** `advancement attribute execute bossbar clear clone compute damage data datapack debug defaultgamemode dialog difficulty effect me enchant experience/xp fill fillbiome forceload function gamemode gamerule give help item kick kill list locate loot msg/tell/w swing particle place playsound posteffect random reload recipe fetchprofile return ride rotate say schedule scoreboard seed version setblock spawnpoint setworldspawn spectate spreadplayers stopsound stopwatch summon tag team teammsg/tm teleport/tp tellraw test tick time title trigger waypoint weather worldborder`
**Dedicated server only:** `ban ban-ip banlist deop op pardon pardon-ip perf save-all save-off save-on setidletimeout stop transfer whitelist`
**Integrated (singleplayer/LAN host) only:** `publish unpublish`
**Conditional:** `jfr` (if the JVM profiler is available). Dev-only (`SharedConstants.DEBUG_DEV_COMMANDS` / IDE): `raid debugpath debugmobspawning warden_spawn_tracker spawn_armor_trims serverpack debugconfig chase`.
`seed` and `version` need permission level 2 on dedicated servers and 0 in singleplayer.

### New since 1.21.x (26.1–26.3)
| Command | Syntax | Notes |
|---|---|---|
| `swing` (26.1, extended 26.3) | `swing [<targets>] [mainhand\|offhand [<animation> [<duration>]]]` | animation `whack` (default) or `stab`. Duration is a time with min 1 (default 6t). Returns the count of living entities swung |
| `fetchprofile` | `fetchprofile name <name>`, `id <uuid>`, `entity <entity>` | prints the game profile. The entity form works on players and mannequins |
| `time` (reworked 26.1) | `time [of <clock>] (set <time>\|set <timemarker>\|add <time>\|pause\|resume\|rate <0.00001..1000>\|query time\|query <timeline> [repetition])`, `time query gametime` | `set`/`add` return the clock's total ticks. Time markers come from timelines. Errors if the dimension has no `default_clock` and no `of` is given |
| `stopwatch` | `stopwatch create <id>`, `query <id> [<scale>]`, `restart <id>`, `remove <id>` | real-time stopwatch. `query` returns `(int)(elapsedSeconds*scale)`. Also `execute if stopwatch <id> <float range in seconds>` |
| `compute` (26.3) | `compute (default\|block <pos>\|entity <target>) (integer <int_provider>\|float <float_provider> [<scale>])` | evaluates a `context_int_provider`/`context_float_provider` (ID or inline SNBT). Float mode returns `floor(value*scale)`. Loot context sets: `command_compute_default` (origin, this), `command_compute_position` (+block_state, block_entity), `command_compute_entity` (+target_entity) (`LootContextSources`) |
| `posteffect` (26.3) | `posteffect add <players> <id>`, `remove <players> <id>`, `clear <players>`, `list <player>` | applies a resource-pack post effect (`assets/<ns>/post_effect/<id>.json`). The server cannot check that it exists |
| `unpublish` (26.2) | `unpublish` | integrated only. `publish` lost its gamemode argument in 26.3 |
| `datapack create` (1.21.9+) | `datapack create <id> <description>` | scaffolds `world/datapacks/<id>` with a pack.mcmeta |

### Changed recently
- **`execute`** (`ExecuteCommand`): subcommands `align anchored as at facing [entity] in on positioned [as|over] rotated [as] store summon if unless run`.
  - `if/unless`: `biome block blocks data dimension entity function items loaded predicate score slots stopwatch`.
  - `on`: `attacker controller leasher origin owner passengers target vehicle` (26.2: vexes count as owned by their evoker).
  - `store result|success`: `block <pos> <path> <type> <scale>`, `bossbar <id> value|max`, `entity <target> <path> <type> <scale>`, `score <holders> <obj>`, `storage <id> <path> <type> <scale>`, where type is `byte|short|int|long|float|double`.
  - **26.3**: `if items (block <pos>|entity <targets>) <slot_source> <item_predicate>` (returns the summed item **count**) and **new** `if slots (block|entity) <slot_source>` (returns the slot count). The slot source is a slot-range string (`container.*`, `weapon.mainhand`, `armor.chest`) or an inline/ID `slot_source`, evaluated in the `command_slot_source` loot context.
- **`item`** (`server/commands/item/ItemCommands.java`): `item (replace|fill|override) (block <pos>|entity <targets>) <slots> (with <item> [<count 1..99>] | from (block <pos>|entity <sources>) <source_slots> [<modifier>])` and `item modify (block|entity) <slots> <modifier>`.
  - `replace` pairs destination and source in order and ignores the extras. `fill` cycles the sources. `override` clears destination slots it has no source for.
  - `from entity` now takes multiple entities, and their slots are concatenated.
- **`data`** (`server/commands/data/DataCommands.java`): `data get|merge|remove|modify (block <pos>|entity <e>|storage <id>) ...`. Modify ops: `append insert <i> merge prepend set`. Sources: `from <src> [<path>]`, `string <src> [<path> [start [end]]]`, `value <nbt>`, and **26.3** `compute (default|block <pos>|entity <e>) (integer|float) <provider>`, which writes an IntTag/FloatTag. Example: `data modify storage mypack:s x set compute entity @n[type=pig] integer {type:"minecraft:score",target:{type:"minecraft:context",target:"target_entity"},score:"points"}`.
- **`attribute`**: `attribute <target> <attribute> get [<scale>] | base get [<scale>] | base set <value> | base reset | modifier add <id> <value> add_value|add_multiplied_base|add_multiplied_total | modifier remove <id> | modifier value get <id> [<scale>]`. Attribute IDs have no `generic.` prefix (`minecraft:max_health`, `minecraft:scale`, and 26.2's `air_drag_modifier`, `bounciness`, `friction_modifier`, `name_tag_distance`, `below_name_distance`).
- **`damage`**: `damage <target> <amount> [<damage_type> [at <pos> | by <entity> [from <cause>]]]`.
- **`ride`**: `ride <target> mount <vehicle>` / `ride <target> dismount`.
- **`rotate`**: `rotate <target> <rotation> | facing <pos> | facing entity <entity> [eyes|feet]`.
- **`random`**: `random (value|roll) <int range> [<sequence id>]`, `random reset (*|<sequence>) [<seed> [<includeWorldSeed> [<includeSequenceId>]]]`. `roll` broadcasts the result.
- **`tick`**: `tick query`, `rate <float>`, `step [<time>]`, `step stop`, `sprint <time>`, `sprint stop`, `freeze`, `unfreeze`. In 26.3, step/stop failures raise errors instead of returning 0.
- **`dialog`**: `dialog show <players> <dialog ID or inline SNBT>`, `dialog clear <players>`.
- **`waypoint`**: `waypoint list`, `waypoint modify <entity> color <team_color>|color hex <RRGGBB>|color reset|style set <style id>|style reset`. 26.2: team colours accept only lowercase snake_case (`dark_purple`).
- **`transfer`** (dedicated only): `transfer <host> [<port> [<players>]]`. The target server needs `accepts-transfers=true`.
- **`test`**: see section 13.
- **`debug`**: `debug start`, `debug stop` (tick profiling), `debug function <name|#tag>` (writes a full command trace to `<server dir>/debug/debug-trace-<date>.txt`).
- **`place`**: `place feature <feature (ID or inline in 26.3)> [<pos>]`, `place jigsaw <pool> <target> <max_depth 1..20> [<pos>]`, `place structure <structure> [<pos>]`, `place template <id> [<pos> [<rotation> [<mirror> [<integrity> [<seed> [strict]]]]]]`.
- **`fill`**: `fill <from> <to> <block> [replace [<filter>]|keep|outline|hollow|destroy] [strict]`. **`clone`**: `clone [from <dim>] <begin> <end> [to <dim>] <dest> [strict] [replace|masked|filtered <filter>] [force|move|normal]`.
- **`gamerule`**: `gamerule <id> [<value>]`, where `id` is the snake_case name or the qualified `minecraft:<id>` (`GameRuleCommand` registers both). Full list in `GameRules.java`: `advance_time advance_weather allow_entering_nether_using_portals block_drops block_explosion_drop_decay command_blocks_work command_block_output drowning_damage elytra_movement_check ender_pearls_vanish_on_death entity_drops fall_damage fire_damage fire_spread_radius_around_player forgive_dead_players freeze_damage global_sound_events immediate_respawn keep_inventory lava_source_conversion limited_crafting locator_bar log_admin_commands max_block_modifications max_command_forks max_command_sequence_length max_entity_cramming max_minecart_speed max_snow_accumulation_height mob_drops mob_explosion_drop_decay mob_griefing natural_health_regeneration player_movement_check players_nether_portal_creative_delay players_nether_portal_default_delay players_sleeping_percentage projectiles_can_break_blocks pvp raids random_tick_speed reduced_debug_info respawn_radius send_command_feedback show_advancement_messages show_death_messages spawner_blocks_work spawn_mobs spawn_monsters spawn_patrols spawn_phantoms spawn_wandering_traders spawn_wardens spectators_generate_chunks spread_vines tnt_explodes tnt_explosion_drop_decay universal_anger water_source_conversion`.
- **`scoreboard`**: `objectives add|remove|list|setdisplay|modify <obj> displayname|rendertype|displayautoupdate|numberformat [blank|fixed <c>|styled <style>]`, `players add|remove|set|get|list|reset|enable|operation|display name|display numberformat`. 26.2: a player without a score no longer shows in the tab list.
- **`give`** (26.3): over-limit counts now raise an error. **`team join/leave`** (26.3) returns only the entities that changed. **`spreadplayers`** (26.3): safe blocks come from `#entities_can_teleport_to`. **`tellraw`** (26.3): `nbt` components resolve at most 64k times.

---

## 5. Selectors and argument syntax

- Selectors (`EntitySelectorParser`): `@p @a @r @s @e @n` (`@n` = nearest entity).
- Options (`EntitySelectorOptions`): `name distance level x y z dx dy dz x_rotation y_rotation limit sort gamemode team tag nbt scores advancements type predicate`. Negate with `!`, e.g. `type=!#minecraft:skeletons`, `predicate=!mypack:in_water`.
- Ranges: `1..5`, `..5`, `5..`, `-1.5..`.
- Coordinates: absolute, `~` relative, `^` local.
- Time arguments: `10`, `10t`, `2s`, `1d`.
- Block predicate: `#minecraft:logs[axis=y]{...}`.
- Block state: `minecraft:oak_log[axis=x]`.
- Resource-or-ID arguments (`ResourceOrIdArgument`) accept a registry ID **or** inline SNBT for: `loot_table`, `predicate` (`execute if predicate`), item modifier (`item modify`), dialog, feature (`place feature`), float/int providers (`compute`), and slot source. Example: `execute if predicate {type:"minecraft:random_chance",chance:0.5} run say lucky`.
- Item stack argument (`ItemParser`): `id[component=value, !component, ...]`, e.g.:
  - `give @s minecraft:diamond_sword[enchantments={"minecraft:sharpness":5},custom_name={text:"Edge",italic:false},!minecraft:tooltip_display] 1`
  - Component values are SNBT run through the component's codec. Enchantments are a bare map `{id:level}` with no `levels` wrapper. `custom_name`/`item_name`/`lore` take a text component directly (SNBT), not a JSON string. `!comp` removes a default component.
- Item predicate (`ItemPredicateArgument`, `ComponentPredicateParser`): `<id|#tag|*>[<test>,...]`. Tests:
  - `comp` (present), `comp=value` (exact)
  - `pred~value` (data-component predicate, e.g. `damage~{durability:{min:3}}`, `enchantments~[{enchantments:"#minecraft:mining"}]`, `custom_data~{key:1}`)
  - `count=5` or `count~{min:2}`
  - `!test` (negate), `a|b` (alternatives)
  - Example: `clear @s *[custom_data~{mypack:{token:1b}}]`.
  - `custom_data~` compares NBT **with types**: `{relic:1}` (int) does not match a stored `relic:1b`. JSON files that write custom data (`set_components`/`set_custom_data` in loot, item stacks in recipes) store a JSON number such as `1` as a **byte** (`1b`), so match it with `1b` (verified on the server: refpack `item_modifier/named_relic.json` + `function/loot_demo.mcfunction`).

---

## 6. Scoreboards and storage

- Objectives: `scoreboard objectives add <name> <criterion> [<display>]`. Criteria include `dummy`, `trigger`, `deathCount`, `playerKillCount`, `totalKillCount`, `health`, `xp`, `level`, `food`, `air`, `armor`, `minecraft.<stat_type>:<ns>.<id>` (e.g. `minecraft.used:minecraft.carrot_on_a_stick`, `minecraft.custom:minecraft.jump`), and team kill criteria.
- Fake players (`#temp`, `$x`) are fine as score holders.
- Math: `scoreboard players operation <t> <o> (+=|-=|*=|/=|%=|=|<|>|><) <s> <o>`. Division floors.
- Command storage: `data modify storage <ns:id> <path> ...`. It persists per world in `world/data/<namespace>/command_storage.dat` (26.1 split per namespace; `DimensionStorageFileFix` migrates the old `command_storage_<ns>.dat`). Storage is fast compared to entity NBT and is the standard place for state, macro arguments and lists.
- `execute store result storage ns:x path int 1 run ...` is the bridge from command results to NBT. `execute store result score ... run data get storage ...` goes the other way.

---

## 7. Text components: JSON files vs SNBT in commands

- **JSON files** (advancement title, `pack.description`, dialogs, chat_type, item components in loot/recipes) use the JSON codec: `{"text":"Hi","color":"gold","bold":true}`.
- **Commands** parse components as **SNBT** (`ComponentArgument` → `SnbtGrammar` → `ComponentSerialization.CODEC`). Examples (`ComponentArgument.EXAMPLES`): `"hello world"`, `'hello world'`, `{text:"hello world"}`, `[""]`.
  ```mcfunction
  tellraw @a {text:"Click",color:"aqua",underlined:true,click_event:{action:"run_command",command:"/trigger menu"},hover_event:{action:"show_text",value:"Opens menu"}}
  tellraw @a [{selector:"@s"},{text:" has "},{score:{name:"@s",objective:"points"}}]
  ```
- Style keys (`Style.java`): `color shadow_color bold italic underlined strikethrough obfuscated click_event hover_event insertion font`. They are **snake_case** (since 1.21.5): `click_event`/`hover_event`, not `clickEvent`.
- `click_event.action` (`ClickEvent.Action`):
  - `open_url{url}`, `open_file{path}`, `run_command{command}`, `suggest_command{command}`
  - `change_page{page:int}`, `copy_to_clipboard{value}`
  - `show_dialog{dialog: ID or inline}`, `custom{id, payload?}`
- `hover_event.action`: `show_text{value}`, `show_item{id,count?,components?}`, `show_entity{id,uuid,name?}`.
- Content types (`ComponentSerialization`): `text`, `translatable` (`translate`, `with`, `fallback`), `keybind`, `score{name,objective}`, `selector{selector,separator?}`, `nbt{nbt, block|entity|storage, interpret?, plain?, separator?}`, and `object` (atlas sprite / player head, with `fallback`, 26.1).
- 26.1: with `interpret:false`, `nbt` pretty-prints. `plain:true` strips styling. `plain` and `interpret` are mutually exclusive.
- `run_command` from **signs** needs block-entity `allow_op_features:true` since 26.3.

---

## 8. Predicates, loot tables, item modifiers (26.3 shapes)

Codecs: `src: net/minecraft/world/level/storage/loot/{LootTable,LootPool}.java`, `entries/LootPoolEntries.java`, `functions/LootItemFunctions.java`, `predicates/LootItemConditionTypes.java`, `providers/number/**`, `parameters/LootContextParamSets.java`.

### The holder rule (applies everywhere in 26.3)
Every field typed as predicate, item modifier, loot table, number provider, slot source, recipe or advancement accepts the forms below. **Exception: files in static (world) registries** (`villager_trade`, `trade_set`, `enchantment`, worldgen, ...) load before the reloadable registries exist, so there only the inline form works. An ID there stops the server with `Registry does not exist: minecraft:context_int_provider` (or `minecraft:item_modifier`, ...), e.g. `villager_trade.max_uses: "mypack:rolls"` or `given_item_modifier: "mypack:mod"` (verified on the server). IDs do work in reloadable files (loot tables, predicates, item modifiers, recipes, advancements, the providers themselves) and in commands:
- an **inline object**, or
- a **string ID** (a reference into the registry folder).

List-typed fields (e.g. `all_of.terms`, `sequence.functions`, `slot_source group.terms`) accept a single ID, a list of IDs, a list of inline values, a **mix**, or a `#tag`. Inline lists are only allowed in top-level files. Because of this the old `reference` types were removed.

### Predicate file (`data/<ns>/predicate/*.json`)
```json
{ "type": "minecraft:match_tool", "predicate": { "items": "minecraft:shears" } }
```
- Condition types: `inverted{term}`, `any_of{terms}`, `all_of{terms}`, `random_chance{chance: float provider}`, `random_chance_with_enchanted_bonus`, `entity_properties{entity, predicate}`, `killed_by_player`, `entity_scores{entity, scores:{obj: int range}}`, `match_block`, `match_tool{predicate}`, `table_bonus`, `survives_explosion`, `damage_source_properties`, `location_check`, `weather_check`, `time_check{clock, value, period?}`, `int_value_check{value, test}`, `float_value_check{value, test}`, `enchantment_active_check`, `environment_attribute_check{attribute, value}`.
- Entity targets (`entity`): `this`, `attacker`, `direct_attacker`, `attacking_player`, `target_entity`, `interacting_entity`.
- Vanilla predicates now exist: `minecraft:tool/can_shear`, `minecraft:tool/can_silk_touch`, `minecraft:block/fast_cooking` (`jar: predicate/`).

### Entity predicate body (since 26.2: component-style map)
```json
{
  "minecraft:entity_type": "#minecraft:skeletons",
  "minecraft:flags": { "is_on_fire": true },
  "minecraft:equipment": { "mainhand": { "items": "minecraft:bow" } },
  "minecraft:type_specific/player": { "gamemode": ["survival"], "food": { "level": { "max": 6 } } },
  "minecraft:entity_tags": { "all_of": ["boss"], "none_of": ["dead"] }
}
```
- Keys (`EntitySubPredicates`) are identifiers, so the `minecraft:` prefix may be left out (`"equipment": {...}` loads; refpack `predicate/holding_flint.json`): `components distance effects entity_tags entity_type equipment flags location movement movement_affected_by nbt passenger periodic_tick predicates slots stepping_on targeted_entity team vehicle type_specific/cube_mob type_specific/fishing_hook type_specific/lightning type_specific/player type_specific/raider type_specific/sheep`.
- Mob variants are matched through `components`/`predicates`, e.g. `"minecraft:components": {"minecraft:villager/variant": "minecraft:desert"}`.

### Loot table
```json
{
  "type": "minecraft:block",
  "random_sequence": "mypack:blocks/thing",
  "modifier": { "type": "minecraft:explosion_decay" },
  "pools": [
    {
      "rolls": 1,
      "bonus_rolls": 0.0,
      "condition": "minecraft:tool/can_silk_touch",
      "modifier": [ { "type": "minecraft:set_count", "count": { "type": "minecraft:uniform", "min": 1, "max": 3 } } ],
      "entries": [
        { "type": "minecraft:item", "name": "minecraft:diamond", "weight": 1,
          "condition": { "type": "minecraft:match_block", "blocks": "minecraft:wheat", "state": { "age": "7" } } }
      ]
    }
  ]
}
```
- `rolls` is a context_int_provider, `bonus_rolls` a context_float_provider. An int constant can be written as a bare number (`ConstantValue.INLINE_CODEC`).
- `type` = loot context param set: `empty generic chest command command_slot_source command_compute_default command_compute_position command_compute_entity selector villager_trade fishing entity equipment archaeology gift barter vault advancement_reward advancement_entity advancement_location block_use block shearing entity_interact block_interact container_process enchanted_damage enchanted_item enchanted_location enchanted_entity hit_block`.
- Entry types: `empty item loot_table dynamic tag slots alternatives sequence group`. Every entry has `condition?` and `modifier?`.
  - `tag` uses `items` + `expand`.
  - `loot_table` uses `value` (ID/list/inline/tag) + `expand`.
  - `slots` (new) uses `slot_source`.
- Loot function types (`LootItemFunctions.bootstrap`): `set_count set_item enchant_with_levels enchant_randomly set_enchantments set_custom_data set_components furnace_smelt enchanted_count_increase set_damage set_attributes set_name exploration_map set_stew_effect copy_name set_contents modify_contents filtered limit_count apply_bonus set_loot_table explosion_decay set_lore fill_player_head copy_custom_data copy_state set_banner_pattern set_potion set_random_dyes set_random_potion set_instrument sequence copy_components set_fireworks set_firework_explosion set_book_cover set_written_book_pages set_writable_book_pages toggle_tooltips set_ominous_bottle_amplifier set_custom_model_data discard`. Every function takes `condition?`.
- Item modifier file (`data/<ns>/item_modifier/*.json`): exactly **one typed function object**. The registry loads files with `LootItemFunctions.TYPED_CODEC`, so a bare top-level list fails with `Not a JSON object` and stops the server; wrap several functions in `{"type":"minecraft:sequence","functions":[...]}` (`functions` is a holder set: inline objects, IDs or a `#tag`). The bare-list shorthand only works in *fields* typed as a modifier (`modifier`, `given_item_modifier`, inline SNBT in `item modify`), which use `DIRECT_CODEC` (src: `RegistryDataLoader.RELOADABLE_REGISTRIES`, `LootItemFunctions.java`; verified on the 26.3 server). Use it with `item modify entity @s weapon.mainhand mypack:upgrade`.

### Number providers (`context_int_provider` / `context_float_provider`)
- Int types: `abs avg binomial{n,p} conditional{condition,on_true,on_false?} constant{value} sub{left,right} environment_attribute{attribute} from_float{input} max/min/add/mul/avg{inputs} floor_mod floor_div mod div{left,right} negate{input} number_dispatcher{cases:[{condition,value}],default?} pow{base,exponent} score{target,score} storage{storage,path} uniform{min,max} weighted_list{distribution:[{data,weight}]}`.
- Float types: `abs avg ceil conditional constant cos sub enchantment_level{amount} environment_attribute floor from_int length{inputs} max min mod negate number_dispatcher pow mul div round sin sqrt storage add truncate uniform weighted_list`.
- Field names are verified from codecs (`AggregateProvider`→`inputs`, `BinaryProvider`→`left/right`, `UnaryProvider`→`input`, `RangeProvider`→`min/max`, `PowerProvider`→`base/exponent`, `StoredNumberAccess`→`storage/path`, `ScoreboardValue`→`target/score`). The wiki's older snapshot text (`summands`, `operands`, `number_provider`) is **outdated**.
- Score target forms: `{type:"minecraft:fixed",name:"#x"}` or `{type:"minecraft:context",target:"this"}`.

---

## 9. Advancements

Codec: `src: net/minecraft/advancements/{Advancement,DisplayInfo,AdvancementRewards}.java`.
```json
{
  "parent": "minecraft:adventure/root",
  "display": {
    "icon": { "id": "minecraft:diamond" },
    "title": { "text": "Shiny" },
    "description": "Get a diamond",
    "frame": "goal",
    "background": "minecraft:gui/advancements/backgrounds/stone",
    "show_toast": true, "announce_to_chat": true, "hidden": false
  },
  "criteria": {
    "got": { "trigger": "minecraft:inventory_changed", "conditions": { "items": [ { "items": "minecraft:diamond" } ] } }
  },
  "requirements": [["got"]],
  "rewards": { "experience": 10, "loot": "mypack:reward", "recipes": ["mypack:thing"], "function": "mypack:on_reward" },
  "sends_telemetry_event": false
}
```
- `icon` is an ItemStackTemplate (`{id,count?,components?}` or a bare ID string).
- `background` is a texture asset ID without `textures/` or `.png`.
- `rewards.loot` is a loot table holder set (ID, list or `#tag`).
- Root advancements (no parent) need a `background` to show a tab.
- The `player` condition (on all triggers but `impossible`) is a loot condition: `{"type":"minecraft:entity_properties","entity":"this","predicate":{...}}`, or a predicate ID. The bare entity-predicate shorthand and condition lists were removed in 26.3.
- Triggers (`src: net/minecraft/advancements/triggers/CriteriaTriggers.java`): `impossible player_killed_entity entity_killed_player enter_block inventory_changed recipe_unlocked player_hurt_entity entity_hurt_player enchanted_item filled_bucket brewed_potion construct_beacon used_ender_eye summoned_entity bred_animals location slept_in_bed cured_zombie_villager villager_trade item_durability_changed levitation changed_dimension tick tame_animal placed_block consume_item effects_changed used_totem nether_travel fishing_rod_hooked channeled_lightning shot_crossbow spear_mobs killed_by_arrow hero_of_the_village voluntary_exile slide_down_block bee_nest_destroyed target_hit item_used_on_block default_block_use any_block_use player_generates_container_loot thrown_item_picked_up_by_entity thrown_item_picked_up_by_player player_interacted_with_entity player_sheared_equipment started_riding lightning_strike using_item fall_from_height ride_entity_in_lava kill_mob_near_sculk_catalyst allay_drop_item_on_block avoid_vibration recipe_crafted crafter_recipe_crafted fall_after_explosion`.
- 26.3 field renames: `recipe_crafted`/`crafter_recipe_crafted`: `recipe_id`→`recipes`. `recipe_unlocked`: `recipe`→`recipes`. `enter_block`/`slide_down_block`/`bee_nest_destroyed`: `block`→`blocks` (+`state`). `player_generates_container_loot`: `loot_table`→`loot_tables`. `brewed_potion.potion` is now a `potion_contents` component predicate.
- Classic patterns:
  - Function on an event: trigger with `rewards.function`, then `advancement revoke @s only ns:adv` inside the function so it can fire again.
  - `minecraft:tick` + a player predicate as a cheap per-player tick filter.

---

## 10. Recipes (`data/<ns>/recipe/*.json`)

Serializers (`src: net/minecraft/world/item/crafting/RecipeSerializers.java`): `crafting_shaped crafting_shapeless crafting_dye crafting_imbue crafting_transmute crafting_decorated_pot crafting_special_bookcloning crafting_special_mapextending crafting_special_firework_rocket crafting_special_firework_star crafting_special_firework_star_fade crafting_special_bannerduplicate crafting_special_shielddecoration crafting_special_repairitem smelting blasting smoking campfire_cooking stonecutting smithing_transform smithing_trim brewing`.
Removed since 1.21.x: `crafting_special_armordye` (→`crafting_dye`), `crafting_special_tippedarrow` (→`crafting_imbue`), `crafting_special_mapcloning` (→`crafting_transmute`), `crafting_special_suspiciousstew`. There are no other special serializers.

Common fields (`Recipe.BookInfo`/`CommonInfo`): `category` (crafting: `building|redstone|equipment|misc`; cooking: `food|blocks|misc`), `group` (string), `show_notification` (bool, default true; now on most types).
- **Ingredient** (`Ingredient.CODEC`) = non-empty holder set of items: `"minecraft:stick"`, `["minecraft:a","minecraft:b"]` or `"#minecraft:planks"`. The pre-1.21.2 `{"item":..}`/`{"tag":..}` objects are gone, air is rejected, and there are no component-matching ingredients in vanilla.
- **Result** (`ItemStackTemplate`): `"minecraft:x"` or `{"id","count"(1..99),"components"}`.

```json
{ "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["##","##"], "key": {"#": "minecraft:oak_planks"}, "result": {"id":"minecraft:crafting_table"} }
{ "type": "minecraft:crafting_shapeless", "ingredients": ["minecraft:melon_slice"], "result": "minecraft:melon_seeds" }
{ "type": "minecraft:smelting", "ingredient": "minecraft:clay_ball", "result": {"id":"minecraft:brick"}, "experience": 0.3, "cookingtime": 200 }
{ "type": "minecraft:stonecutting", "ingredient": "minecraft:tuff", "result": {"id":"minecraft:tuff_wall"} }
{ "type": "minecraft:smithing_transform", "template": "minecraft:netherite_upgrade_smithing_template", "base": "minecraft:diamond_axe", "addition": "#minecraft:netherite_tool_materials", "result": {"id":"minecraft:netherite_axe"} }
{ "type": "minecraft:smithing_trim", "template": "minecraft:eye_armor_trim_smithing_template", "base": "#minecraft:trimmable_armor", "addition": "#minecraft:trim_materials", "pattern": "minecraft:eye" }
{ "type": "minecraft:crafting_transmute", "group": "shulker_box_dye", "input": "#minecraft:shulker_boxes", "material": "minecraft:red_dye", "result": {"id":"minecraft:red_shulker_box"}, "material_count": [1,1], "add_material_count_to_result": false }
{ "type": "minecraft:crafting_dye", "target": "minecraft:wolf_armor", "dye": "#minecraft:dyes", "result": {"id":"minecraft:wolf_armor"} }
{ "type": "minecraft:crafting_imbue", "source": "minecraft:lingering_potion", "material": "minecraft:arrow", "result": {"id":"minecraft:tipped_arrow","count":8} }
{ "type": "minecraft:brewing",
  "input": { "item": "minecraft:potion", "potion_contents": { "potions": "minecraft:water" } },
  "reagent": { "item": "minecraft:stone" },
  "output": { "id": "minecraft:potion", "components": { "minecraft:potion_contents": { "potion": "minecraft:mundane" } } } }
```
- Cooking recipes: `ingredient`, `result`, `cookingtime` (**required**), `experience` (default 0). Blast furnace and smoker speed-ups come from `context_float_provider` `cooking/speed_default`, so a custom blasting recipe whose old time was 100 should now say 200.
- Brewing: `input`/`reagent` are `PotionIngredient` (`item` ingredient plus optional `potion_contents` **predicate**, which uses `potions`, plural). `output` is an ItemStackTemplate. Any items work in any slot.
- Other special recipes take configurable ingredients:
  - `crafting_special_bookcloning{source, material, allowed_generations, result}`
  - `crafting_special_mapextending{map, material, result}`
  - `crafting_special_firework_rocket{shell, fuel, star, result}`
  - `crafting_special_firework_star{dye, fuel, shapes{...}, trail, twinkle, result}`
  - `crafting_special_firework_star_fade{target, dye, result}`
  - `crafting_special_bannerduplicate{banner, result}`
  - `crafting_special_shielddecoration{target, banner, result}`
  - `crafting_decorated_pot{back,left,right,front,result}`
  - `crafting_special_repairitem{}`
- Recipes are **not** auto-unlocked. Unlock them through `advancement` rewards, `recipe give`, or the `limited_crafting` gamerule semantics.
- Override a vanilla recipe by using the same path. To remove one, use the pack.mcmeta `filter` section. There is no empty-recipe type, and an invalid recipe is **not** skipped: in 26.3 recipes load through `RegistryDataLoader` like other registries, so one bad file (e.g. a shapeless recipe with `"ingredients": []`: `List is too short: 0, expected range [1-9]`) logs `Registry loading errors` and the server refuses to start (verified on the server).

---

## 11. Tags

`TagFile` (`src: net/minecraft/tags/TagFile.java`, `TagEntry.java`):
```json
{ "replace": false, "values": [ "minecraft:stone", "#minecraft:logs", { "id": "#c:ores", "required": false } ] }
```
- `replace: true` discards lower-priority definitions. Otherwise all packs merge.
- An entry that cannot be resolved makes the **whole tag fail** unless it has `required:false`. Always mark optional cross-mod entries `required:false`.
- `#c:` **convention tags** are not vanilla. They ship with Fabric API (`fabric-convention-tags-v2`, e.g. `data/c/tags/item/ingots/iron.json`, ~109 item and ~60 block tag files) and NeoForge. On a pure vanilla server `#c:...` does not exist, so reference them only inside `required:false` entries or inside your own `c` namespace files. Fabric's `c:ingots` is a union of `#c:ingots/<material>` subtags.
- Function tags (`tags/function`) are reloadable. Tags of static world registries (`tags/worldgen/biome`, `tags/enchantment`, `tags/damage_type`, `tags/timeline`, `tags/villager_trade`, ...) need a restart.
- 26.x tag renames to watch:
  - block: `dry_vegetation_may_place_on`→`supports_dry_vegetation`, `bamboo_plantable_on`→`supports_bamboo`, `small_dripleaf_placeable`→`supports_small_dripleaf`, `big_dripleaf_placeable`→`supports_big_dripleaf`, `mushroom_grow_block`→`overrides_mushroom_light_requirement`, `snow_layer_can_survive_on`→`support_override_snow_layer`, `snow_layer_cannot_survive_on`→`cannot_support_snow_layer` (26.1); `concrete_powder`→`concrete_powders` (26.2); `convertable_to_mud`→`convertible_to_mud` (26.3).
  - Removed: `overworld_carver_replaceables`/`nether_carver_replaceables` (26.3; carvers replace anything), item `#dyeable` (26.1), item `#brewing_fuel` (26.3).
  - Structure: `on_*_explorer_maps`→`on_woodland_mansion_maps`, `on_ocean_monument_maps`, `on_jungle_pyramid_maps`, `on_swamp_hut_maps`, `on_buried_trial_chambers_map` (26.3).

---

## 12. Data components inside data packs

- Item stacks in JSON (`ItemStackTemplate`): `{"id":"minecraft:x","count":1,"components":{"minecraft:custom_name":{"text":"A"},"!minecraft:food":{}}}`. A `!`-prefixed key removes a component.
- Component IDs in 26.3 (`src: net/minecraft/core/component/DataComponents.java`) include the new `additional_trade_cost`, `dye`, `block_transformer`, `villager_food`, `sulfur_cube_content`, `compostable`, `cooking_fuel`, `brewing_fuel`, `attack_animation`, `interact_animation`, `mob_visibility`, `sign_text_front`, `sign_text_back`, `waxed`, `cushion/color`. Removed: `swing_animation` and `map_color` (26.3). `pot_decorations` is now an object `{back,left,right,front}` of item stacks (26.3).
- See the item-components knowledge file for full component formats.

---

## 13. Game tests from a data pack

Source: `src: net/minecraft/gametest/framework/{GameTestInstance,TestData,TestEnvironmentDefinition,TestCommand,GameTestMainUtil}.java`.
- `test_environment/<id>.json` types:
  - `all_of{definitions:[...]}`
  - `clock_time{clock, time}` (26.1, replaced `time_of_day`)
  - `difficulty{difficulty}` (26.2)
  - `function{setup?, teardown?}` (function IDs)
  - `game_rules{rules:{...}}`
  - `timeline_attributes{timelines:[...]}`
  - `weather{weather: clear|rain|thunder}`
- `test_instance/<id>.json` types: `block_based` and `function`. `function` references the `test_function` registry, which only Java code fills, so **datapack-only tests use `block_based`**. Shared fields (`TestData`):
```json
{
  "type": "minecraft:block_based",
  "environment": "minecraft:default",
  "structure": "mypack:my_test",
  "max_ticks": 100,
  "setup_ticks": 0,
  "required": true,
  "rotation": "none",
  "manual_only": false,
  "max_attempts": 1,
  "required_successes": 1,
  "sky_access": false,
  "padding": 0,
  "dimension": "minecraft:overworld"
}
```
- Block-based tests: the structure (`data/<ns>/structure/my_test.nbt`) contains `minecraft:test_block` blocks in modes `start`, `log`, `fail`, `accept`. A redstone pulse into `accept` passes and into `fail` fails. Build structures in-game with `test create <id> [w h d]`, then `test export*`.
- `/test` subcommands: `run <selector> [<numberOfTimes> [<untilFailed> [<rotationSteps> [<testsPerRow>]]]]`, `runmultiple <selector> [<amount>]`, `runthese`, `runclosest`, `runthat`, `runfailed [...]`, `verify <selector>`, `locate <selector>`, `resetclosest|resetthese|resetthat`, `clearthat|clearthese`, `clearall [<radius>]`, `stop`, `pos [<var>]`, `create <id> [<w> [<h> <d>]]`, `export <test>`, `exportclosest|exportthese|exportthat`. Selectors are namespaced ID wildcards (`mypack:*`).
- Headless CI run (wiki "GameTest" page plus `GameTestMainUtil` options):
  ```bash
  java -DbundlerMainClass=net.minecraft.gametest.Main -jar server.jar \
       --universe gametestserver --packs ./packs --tests 'mypack:*' --report results.xml [--verify true] [--repeatCount N]
  ```
  `--packs` copies every zip or folder with a pack.mcmeta into `<universe>/gametestworld/datapacks`. The universe dir is **deleted and recreated** on each run. The exit status reflects required-test failures (UNVERIFIED exact exit codes).

### PackTest (third-party, `misode/packtest` README, read 2026-11-14)
Not vanilla: a Fabric mod that turns `data/<ns>/test/*.mcfunction` into game tests. Running it needs the mod jar (here `tools/packtest.py`, owner approval required).
- Directives are comments at the top: `# @environment <id>`, `# @dimension <id>`, `# @template <structure>` (default `minecraft:empty`), `# @timeout <ticks>` (default 100), `# @setup_ticks`, `# @optional`, `# @skyaccess`, `# @rotation`, `# @manual`, `# @max_attempts`, `# @required_successes`, `# @padding`, and `# @dummy [<pos>]` (spawns a fake player and makes it `@s`; needed for anything that targets a player).
- Commands: `assert [not] <condition>`, `await [not] <condition>`, `await delay <time>`, `fail <text>`, `succeed`, `log <text>`, and `dummy <name> spawn|respawn|leave|jump|sneak|sprint|drop|swap|selectslot|use item|use block <pos>|use entity <e>|attack <e>|mine <pos>`.
- Conditions: `block <pos> <block>`, `data ...`, `entity <selector>`, `predicate <id>`, `score ...` (same syntax as `execute if`), `chat <regex> [<receivers>]`. There is no `items` condition: write `execute store result score #n obj if items entity @s container.* <predicate>` and then `assert score #n obj matches 1..`.
- Examples: `datapacks/refpack/data/refpack/test/*.mcfunction`.

---

## 14. Performance practices

- Keep `#minecraft:tick` tiny. Gate per-player work with `execute as @a[...]` filters, and spread work across ticks with `schedule` or a modulo counter.
- Selectors: put the cheap, indexable filters first. `type=` and `tag=` are cheap. `distance=`/`dx` limit the search box. `nbt=` and `predicate=` are expensive, so use them last or replace them with tags/scores. Prefer `@n`/`@p` over `@e[limit=1,sort=nearest]`. Never `@e` with only `nbt=`.
- Avoid `data get entity @s` on players every tick, because serialising player NBT is costly. Prefer scoreboard criteria, predicates, `execute if items`, and attributes.
- Command storage beats marker entities for state. Use marker entities (`minecraft:marker`, never ticked or sent to clients) only when you need a position.
- Macros: each invocation with new arguments re-parses the macro lines, and only 8 argument sets are cached per function. Do not run macros every tick with varying values. Use `execute store` / scoreboard maths instead.
- `execute if function` and `return` exit early. They are cheaper than layered `execute if` chains that re-evaluate selectors.
- Prefer predicate files and IDs (cached registry objects) over large inline SNBT predicates in hot commands. Inline values are parsed once at function load, but large ones bloat the code.
- Worldgen: feature `count` is limited to 0..4096 (26.1). Feature placement must stay within neighbouring chunks.

---

## 15. Debugging

- `/reload` output plus `logs/latest.log`. Search for `Failed to load function`, `Couldn't parse element`, `Failed to parse`, `Unknown registry key`, `Errors in registry`. Registry parse errors abort world load with a "Datapack validation failed" screen or, on a dedicated server, `Failed to load datapacks, can't proceed with server load`. In 26.3 this is true for the **reloadable** registries too (loot_table, predicate, item_modifier, recipe, advancement, context providers, slot_source), because they load through `RegistryDataLoader`; only functions still fail one by one. The log block reads `Registry loading errors:` / `> Errors in registry minecraft:<registry>:` / `>> Errors in element <ns:id>:` followed by a `Caused by:` line with the codec message, plus a follow-up `Unbound values in registry` for the same ID (verified on the server). `--safeMode` on the server starts with vanilla packs only.
- `/datapack list [available|enabled]` and `/datapack enable|disable "file/<name>"`. Pack IDs are `file/<folder or zip name>`. A pack that fails pack.mcmeta validation does not appear in the list (check the log).
- `/debug function <fn>` writes a per-command trace with results to `debug/debug-trace-*.txt`. `/debug start` … `/debug stop` profiles ticks. `/perf start|stop` (dedicated) writes zips to `debug/profiling` (`MetricsPersister.PROFILING_RESULTS_DIR`). `/jfr start|stop` writes a JFR recording.
- `/tick freeze` + `/tick step 1` to step logic. `/tick rate 100` / `/tick sprint 1d` to fast-forward.
- `/compute` to test number providers. `execute if predicate <inline>` to test predicates. `loot spawn ~ ~ ~ loot <table>` and `loot give @s loot <inline table>` to test loot.
- `data get storage mypack:s` / `data get entity @s` to inspect NBT. The F3 debug screen in 26.1 has separate entries for block/fluid/entity tags (`looking_at_block_tags`, etc.).
- Datagen reports: `java -DbundlerMainClass=net.minecraft.data.Main -jar server.jar --reports --server` writes `generated/reports/` (commands tree JSON, registries, item components, blocks). This is the best authoritative command-tree reference. Note that 26.3 removed the `definition` field from `blocks.json`.

---

## 16. Tooling (status checked 2026-11-14)

| Tool | Status for 26.3 | Notes |
|---|---|---|
| **Spyglass** (VS Code "Data-pack Helper Plus", id `SPGoding.datapack-language-server`) | v4.14.0 published 2026-10-13 on the VS Code marketplace and Open VSX. `@spyglassmc/java-edition` 0.3.70 on npm (2026-10-13). Schemas come from `SpyglassMC/vanilla-mcdoc`, which has `#[since="26.3"]` definitions for `slot_source`, `block_transformer`, `context_int/float` providers, `material_rule`, `sulfur_cube_archetype`, `trade_set`, `world_clock` | Pick the version via `spyglass.json`/`pack.mcmeta`. Version data comes from `misode/mcmeta` (already lists 26.4 snapshots) |
| **misode.github.io generators** | Supports 26.3 and 26.4 (`src/config.json` versions list). Generators exist for every 26.x registry: `worldgen/feature` and `worldgen/carver` (26.3+, the old `configured_*` ones are capped at 26.2), `worldgen/material_rule`/`material_condition`, `slot_source`, `context_*_provider`, `block_transformer`, `villager_trade`, `trade_set`, `world_clock`, `sulfur_cube_archetype`, sound variants | **No** `worldgen/block_state_provider` generator yet. Misode's changelog (`misode.github.io/changelog`) and `mcmeta` diffs (`github.com/misode/mcmeta/tree/<version>-diff`) are the fastest way to see per-snapshot JSON changes |
| **beet / bolt / mecha** (Python ≥3.14) | beet 0.121.2 (2026-10-22): `LATEST_MINECRAFT_VERSION = "26.3"`, has resource classes for `ContextIntProvider`, `ContextFloatProvider`, `SlotSource`, `BlockTranformer` [sic], `VillagerTrade`, `TradeSet`, `WorldClock`, `WorldgenFeature`, `WorldgenCarver`, `WorldgenMaterialRule/Condition` (old `configured_*` kept for older targets). mecha 0.107.0 (2026-10-21) ships command trees up to `26_3.json`. bolt 0.53.0 (2026-10-21) | `pip install beet bolt mecha`. Configure `minecraft: "26.3"` in `beet.json`. beet writes `min_format`/`max_format` from its `pack_format_registry.json` |
| **NBT editors** | Misode's VS Code "NBT Viewer" (`Misodee.vscode-nbt`, last release 0.6.7 in 2022) still opens .nbt/.mca/.snbt generically. NBTExplorer is unmaintained. `nbtlib` 2.0.4 (Python). `amulet-core` 1.9.52 | All are format-agnostic for NBT, but none of them know 26.x semantics. 26.1 moved world data (`dimensions/minecraft/<dim>/region`, `data/minecraft/*.dat`), and structure palettes use `id`/`properties`. Region tools such as MCA Selector or Amulet need 26.1+ support for the new folder layout: UNVERIFIED |
| **Datapack linting in CI** | Run the server's GameTest main (section 13), or start a dedicated server and scan `latest.log` | No official linter. Spyglass CLI: UNVERIFIED |

---

## 17. Testing a data pack on a headless dedicated server

```bash
mkdir -p /srv/mc && cd /srv/mc
# server.jar for 26.3: from https://piston-meta.mojang.com/mc/game/version_manifest_v2.json -> versions[id=26.3].url -> downloads.server.url
java -Xmx4G -jar server.jar --nogui --initSettings   # writes server.properties + eula.txt, then exits
sed -i 's/eula=false/eula=true/' eula.txt
cat >> server.properties <<'EOF'
online-mode=false
white-list=false
level-type=minecraft:flat
generate-structures=false
level-name=world
initial-enabled-packs=vanilla,file/mypack
enable-rcon=true
rcon.password=dev
rcon.port=25575
function-permission-level=2
spawn-protection=0
EOF
mkdir -p world/datapacks && cp -r ~/mypack world/datapacks/mypack
java -Xmx4G -jar server.jar --nogui
```
- Main options (`src: net/minecraft/server/Main.java`): `--nogui --initSettings --demo --bonusChest --forceUpgrade --eraseCache --recreateRegionFiles --safeMode --universe <dir> --world <name> --port <n> --serverId --jfrProfile --pidFile`.
- `level-type` is a world preset ID, lowercased: `minecraft:normal` (default), `minecraft:flat`, `minecraft:large_biomes`, `minecraft:amplified`, `minecraft:single_biome_surface`, `minecraft:debug_all_block_states`, or your own `worldgen/world_preset`. `generator-settings` takes JSON for flat/single-biome.
- **26.3 defaults `white-list=true`**, so set it false or whitelist your test account. With `online-mode=false`, offline clients can join.
- `initial-enabled-packs`/`initial-disabled-packs` only apply when the world is **first created**. Afterwards use `/datapack enable`. Packs dropped into `world/datapacks/` are auto-enabled at world creation and listed as available later.
- Drive the server non-interactively through stdin (a fifo or `tmux send-keys`), RCON (`mcrcon -p dev "reload"`), or the Minecraft Server Management Protocol (`management-server-enabled=true`, JSON-RPC over WebSocket, v3.1.0 in 26.3). Note that 26.3 removed some log lines that server wrappers used to grep, and the changelog points to the management protocol instead.
- **No players means no loaded chunks.** At startup nothing is loaded around spawn for an empty server, so `setblock`/`summon` at spawn fail or do nothing. `forceload add` loads its chunk over the following ticks: run it in a `#minecraft:load` function so blocks are usable by the time console commands arrive. Entities in a freshly forceloaded chunk become visible to selectors only a few ticks (about a second) later: `summon` still prints `Summoned new Pig`, but `@e` then finds nothing (`No entity was found`) and the entity is gone (verified on the server with refpack and `tools/packload.py`).
- Iterate: edit the files, run `reload`, and check `logs/latest.log`. Restart for worldgen or other static-registry changes, and for new dimensions use a fresh world (`rm -rf world/dimensions world/level.dat`, or a new `level-name`).
- For loot/predicate/function work, `level-type=minecraft:flat` with `generate-structures=false` starts fastest. For worldgen packs use a new world each time, and consider `/locate`, `/place feature`, `/place structure` and `/fillbiome`.
