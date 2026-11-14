# Resource packs, models, shaders (and shader-pack mods) for Minecraft 26.3

Scope: everything under `assets/<ns>/` that a resource pack or a Fabric mod's client resources can supply, the JSON/GLSL formats, how the 26.3 client loads them, plus Iris/Sodium/CTM-style mods and tooling. Verified 2026-11-14 against Minecraft 26.3 (resource pack format **97.1**, `version.json` `pack_version.resource_major=97, resource_minor=1`), the decompiled 26.3 client (`/tmp/claude-0/mcsrc/client`), the vanilla jar assets, Iris branch `26.3` (1.11.7), Sodium `dev` (0.9.3 betas) and Blockbench 5.2.2.

Source shorthand: `client <path>` = `/tmp/claude-0/mcsrc/client/<path>`, `common <path>` = `/tmp/claude-0/mcsrc/common/<path>`, `jar <path>` = `/tmp/claude-0/mcsrc/vanilla-jar/assets/minecraft/<path>`.

---

## 0. The big changes in 1.21.x -> 26.x that will catch you out

| Change | Since | What it means |
|---|---|---|
| `ResourceLocation` is now `net.minecraft.resources.Identifier` (Mojang names) | 1.21.11 | Every codec in this doc uses `Identifier.CODEC`. |
| Render layer comes from the texture's pixels, not from Java | 26.1 | `ItemBlockRenderTypes` no longer exists. A quad goes to SOLID, CUTOUT or TRANSLUCENT according to its sprite's alpha. A texture entry can force translucency: `{"sprite": "...", "force_translucent": true}`. (src: client `resources/model/cuboid/FaceBakery.java`, `renderer/chunk/ChunkSectionLayer.java#byTransparency`) |
| Model texture values must be strings or objects | 26.1 | Non-string values are rejected. |
| `transformation` field on all item-model types | 26.1 | `model`, `special`, `composite`, `condition`, `select`, `range_dispatch` take `transformation` (a matrix or a decomposed object). |
| Beds and signs are ordinary JSON block models | 26.2 | The `bed`, `standing_sign` and `hanging_sign` special models are gone. The `beds` and `signs` atlases were merged into `blocks`. Bed textures are `block/<color>_bed_*`, sign textures are `block/<wood>_sign.png`. |
| Experimental Vulkan backend ("renderpearl") | 26.2 | Options -> Video -> Graphics API: Default / Prefer Vulkan / Prefer OpenGL. `options.txt` key `preferredGraphicsBackend`. (src: client `net/minecraft/client/PreferredGraphicsApi.java`, `com/mojang/renderpearl/**`) |
| Every shader is compiled by **shaderc** to SPIR-V, on OpenGL too | 26.3 | GL gets GLSL 330 back from SPIRV-Cross. Use `#include <ns:file.glsl>`; `#moj_import` is gone. Every `in`/`out` needs `layout(location=N)`. Vertex outputs are matched to fragment inputs **by location only**. Use Vulkan built-ins such as `gl_VertexIndex`. |
| `shade` removed from model elements | 26.3 | Replaced by `shade_direction_override` (a direction). `"shade": false` becomes `"shade_direction_override": "up"`. |
| Element `light_emission` (0-15) | 1.21.x | Still present. Vanilla uses it in `block/cross_emissive.json`. |
| Free element rotation | 1.21.11 (per Blockbench) | `rotation` takes either `axis` + any `angle`, or Euler `x`/`y`/`z`. The 22.5-degree restriction is gone. |
| Order-independent transparency (OIT) replaces "Improved/Fabulous transparency" | 26.3 | `post_effect/transparency.json` and `shaders/post/transparency.fsh` were removed. Added new `oit_*` shaders and includes. Debug toggle F3+X. |
| `post_effect/end_of_frame.json` hook and `/posteffect` command | 26.3 | A pack can now ship an always-on full-screen post effect. Data packs/ops can push post effects to players. |
| Equipment `trim_overrides` | 26.3 | Replaces `trim_palette_replacements`. Entity armor-trim textures are no longer in a `trims` atlas; they are paletted on demand from `textures/trims/entity/...` plus a `palette` mcmeta section. |
| Palette textures moved to `textures/palettes/` | 26.3 | `trim/color_palettes/*` became `palettes/trim/*` and `palettes/trim_base.png`. Palette IDs `<ns>:<path>` resolve to `textures/palettes/<path>.png`. |
| Tint source `minecraft:map_color` removed | 26.3 | The component it read is gone. |
| Text shaders merged | 26.2 / 26.3 | `core/text` uses defines `IS_GUI`, `IS_SEE_THROUGH`, `IS_GRAYSCALE`. `text_background` was removed in 26.3. |
| Pack formats over 64 must use `min_format`/`max_format` | 1.21.9+ | `supported_formats` is an **error** for packs whose minimum is over 64 (see §1). |

Resource-pack format by version (per minecraft.wiki changelogs): 26.1 = 84.0, 26.2 = 88.0, 26.3 = 97.1. The last integer-only client format was 64 (src: common `net/minecraft/server/packs/metadata/pack/PackFormat.java#lastPreMinorVersion` -> `CLIENT_RESOURCES -> 64`).

---

## 1. `pack.mcmeta`

Sections the client reads (`new MetadataSectionType(...)` in source): `pack`, `overlays`, `filter`, `language`. `features` is data-pack-only. Texture `.mcmeta` sections (`animation`, `texture`, `gui`, `villager`, `palette`) are covered in §7.

### 1.1 `pack` (src: common `server/packs/metadata/pack/PackMetadataSection.java`, `PackFormat.java`)
Fields:
- `description` (Text component, required).
- `min_format`, `max_format` (`PackFormat`). Either an int `97`, or `[major, minor]` such as `[97, 1]`.
  - A bare int in `min_format` means `major.0`.
  - A bare int in `max_format` means `major.*` (minor = `Integer.MAX_VALUE`).
  - You must give both or neither ("must declare both min_format and max_format").
- `pack_format` (int, legacy) and `supported_formats` (legacy int range `[a,b]` or `{"min_inclusive":a,"max_inclusive":b}`).

Validation rules worth knowing:
- If min major > 64 and `supported_formats` is present, the pack fails with "key supported_formats is deprecated starting from pack format 65. Remove supported_formats".
- If min major <= 64 (the pack also targets old versions), it needs `supported_formats` whose min equals `min_format` major, and a `pack_format` inside the range.
- A pack declaring only `pack_format` > 64 fails: "missing mandatory fields min_format and max_format".

Minimal 26.3-only pack:
```json
{
  "pack": {
    "description": "My pack",
    "min_format": [97, 1],
    "max_format": 97
  }
}
```
Pack spanning 1.20.5 (format 32) to 26.3:
```json
{ "pack": { "description": "x", "pack_format": 32, "supported_formats": [32, 64], "min_format": 32, "max_format": 97 } }
```
`pack.png` is the icon at the pack root.

### 1.2 `overlays` (src: common `server/packs/OverlayMetadataSection.java`)
```json
"overlays": {
  "entries": [
    { "directory": "ov_26_3", "min_format": [97, 0], "max_format": 97 },
    { "directory": "ov_old", "formats": [34, 64], "min_format": 34, "max_format": 64 }
  ]
}
```
- `directory` must match `[-_a-zA-Z0-9.]+`. It is a sibling of `assets/` at the pack root, holding its own `assets/...`.
- Entries take `min_format`/`max_format` and/or legacy `formats`. If any entry's minimum is <= 64, every entry must carry `formats`.
- Applicable overlays are stacked on top of the base pack, in order.

### 1.3 `filter` (src: common `server/packs/resources/ResourceFilterSection.java`, `util/IdentifierPattern.java`)
```json
"filter": { "block": [ { "namespace": "minecraft", "path": "textures/block/.*_ore\\.png" } ] }
```
- Both fields are optional Java regexes, matched with `Pattern.asPredicate`, so they are **find** matches, not full matches.
- It hides matching files from **lower-priority** packs, including vanilla.

### 1.4 `language` (src: client `resources/metadata/language/LanguageMetadataSection.java`, `resources/language/LanguageInfo.java`)
```json
"language": { "tlh_aa": { "name": "tlhIngan Hol", "region": "Qo'noS", "bidirectional": false } }
```
- The key is 1-16 chars. `name` and `region` must be non-empty strings. `bidirectional` defaults to false; true means RTL.
- Then ship `assets/<ns>/lang/tlh_aa.json`.

---

## 2. Folder layout: what exists under `assets/minecraft/` in the 26.3 jar

Top-level entries in `jar .` with file counts:

| Path | Count | Loader / format |
|---|---|---|
| `atlases/*.json` | 12 | Atlas source lists: `banner_patterns`, `blocks`, `celestials`, `chests`, `decorated_pot`, `gui`, `items`, `map_decorations`, `paintings`, `particles`, `shield_patterns`, `shulker_boxes` (src: client `resources/model/sprite/AtlasManager.java`) |
| `blockstates/*.json` | 1288 | §3 (src: client `resources/model/BlockStateModelLoader.java`, `renderer/block/dispatch/BlockStateModelDispatcher.java`) |
| `equipment/*.json` | 45 | §6 (src: client `resources/model/EquipmentAssetManager.java`, `EquipmentClientInfo.java`) |
| `font/*.json`, `font/include/*.json` | 7 | §9 |
| `gpu_warnlist.json` | 1 | `{"renderer":[regex],"version":[regex],"vendor":[regex]}`. Matches trigger the GPU warning. |
| `items/*.json` | 1658 | Item model definitions, §5 (src: client `resources/model/ClientItemInfoLoader.java`) |
| `lang/en_us.json`, `lang/deprecated.json` | 2 | §11. All other languages come from the asset index, not the jar. |
| `models/block`, `models/item` | 4192 | §4 |
| `particles/*.json` | 115 | `{"textures": ["minecraft:generic_0", ...]}`. Sprite IDs are in the `particles` atlas (`textures/particle/`). |
| `post_effect/*.json` | 5 | `blur`, `creeper`, `entity_outline`, `invert`, `spider`. §10 |
| `regional_compliancies.json` | 1 | Region-specific play-time warnings |
| `shaders/core`, `shaders/include`, `shaders/post` | 89 | §10 |
| `texts/` | 4 | `credits.json`, `end.txt`, `postcredits.txt`, `splashes.txt`. §12 |
| `textures/` | 4182 | Subfolders: `block colormap effect entity environment font gui item map misc mob_effect painting palettes particle trims` |
| `waypoint_style/*.json` | 2 | §8 |

Not in the jar, but valid pack content: `sounds.json` and `sounds/**/*.ogg` (vanilla ships these through the asset index), other `lang/*.json`, and `font/unifont.zip` / `.hex`. Mods can add any namespace. The paths are the same under `assets/<modid>/`.

Notable texture subfolders:
- `textures/environment/` holds `clouds.png`, `end_sky.png`, `rain.png`, `snow.png` and `celestial/` (`sun.png`, `end_flash.png`, `moon/`). `celestial/` is stitched into the `celestials` atlas.
- `textures/gui/sprites/` is the GUI atlas (sprite IDs without the prefix, e.g. `minecraft:widget/button`). Also `gui/title/background/panorama_N.png`.
- `textures/palettes/` holds `trim_base.png` and `trim/*.png`.
- `textures/trims/items/*_trim.png` holds item trim overlays. `textures/trims/entity/{humanoid,humanoid_leggings}/*.png` holds entity trims, each with a `palette` mcmeta.
- `textures/entity/equipment/<layer_type>/<asset>.png` holds armor and body textures.
- `textures/colormap/` holds `grass.png`, `foliage.png`, `dry_foliage.png`.

Atlases and their sources (jar `atlases/`):

| Atlas | Sources | Mipmaps |
|---|---|---|
| `blocks` | dir `block` -> prefix `block/`; dir `entity/conduit`; single `entity/bell/bell_body`; single `entity/enchantment/enchanting_table_book` | yes |
| `items` | dir `item` -> prefix `item/`; `paletted_permutations` for item trims | no |
| `gui` | dir `gui/sprites` (prefix `""`); dir `mob_effect` -> prefix `mob_effect/` | no. Only atlas that reads the `gui` mcmeta section. |
| `celestials` | `environment/celestial` | no |
| `particles` | `particle` | no |
| `banner_patterns` | `entity/banner` | no |
| `shield_patterns` | `entity/shield` | no |
| `chests` | `entity/chest` | no |
| `shulker_boxes` | `entity/shulker` | no |
| `decorated_pot` | `entity/decorated_pot` | no |
| `paintings` | `painting` | no |
| `map_decorations` | `map/decorations` | no |

Gotchas:
- There is **no `trims` (armor) atlas** any more.
- Models look a texture up in the **items atlas first, then blocks** (src: client `resources/model/sprite/MaterialBaker.java#bake`). An item model may reference `block/...` textures and a block model may reference `item/...`, provided the sprite was stitched into one of those two atlases.

---

## 3. Blockstates (`assets/<ns>/blockstates/<block>.json`)

Codec: client `renderer/block/dispatch/BlockStateModelDispatcher.java`. The file must contain `variants` or `multipart` (or both, without overlapping states).

### 3.1 Variants
`"variants": { "<prop>=<val>,<prop>=<val>": <model-or-weighted-list>, "": ... }`

A model object (`Variant.MAP_CODEC`):
- `model` (required, ID under `models/`).
- `x`, `y`, `z`: `Quadrant`, one of 0/90/180/270. **`z` is new.**
- `uvlock` (bool, default false).

A weighted list is `[ {model..., "weight": 1}, ... ]`, where `weight` is a positive int, default 1. Real example, jar `blockstates/stone.json`:
```json
{ "variants": { "": [
  { "model": "minecraft:block/stone" },
  { "model": "minecraft:block/stone_mirrored" },
  { "model": "minecraft:block/stone", "y": 180 },
  { "model": "minecraft:block/stone_mirrored", "y": 180 } ] } }
```

### 3.2 Multipart
`"multipart": [ { "when": <condition>, "apply": <model-or-weighted-list> } ]`. `when` is optional.

A condition is either:
- `{prop: terms}`, a key/value map, AND-ed. Terms are a string like `"side|up"`, with `!` to negate (e.g. `"!none"`). For legacy reasons ints and bools are accepted.
- `{"AND": [cond...]}` or `{"OR": [cond...]}`, with exactly one key.

Unknown property names throw. A condition that is always true or always false logs a warning. Excerpt from jar `blockstates/redstone_wire.json`:
```json
{ "apply": { "model": "minecraft:block/redstone_dust_dot" },
  "when": { "OR": [ { "east": "none", "north": "none", "south": "none", "west": "none" },
                    { "east": "side|up", "north": "side|up" } ] } }
```

### 3.3 Built-in (Java) block models a pack cannot replace through blockstates
`client renderer/block/BuiltInBlockModels.java` hard-wires these: air, mob heads/skulls, player heads, banners and wall banners, (dyed) shulker boxes, ender/normal/trapped/copper chests, copper golem statue, bell (moving part), conduit, decorated pot, enchanting table (book), end gateway, end portal, and `wildflowers`/`pink_petals` (flower beds).

- Their blockstate JSON exists but typically points to a particle-only model. Example, jar `models/block/chest.json`: `{"textures":{"particle":"minecraft:block/oak_planks"}}`.
- Retexture them via their entity textures (atlases `chests`, `shulker_boxes`, `banner_patterns`, ...) instead.

---

## 4. Block and item models (`assets/<ns>/models/**.json`)

Parsed by Gson (not a Codec) in client `resources/model/cuboid/CuboidModel.java`, `CuboidModelElement.java`, `CuboidFace.java`, `ItemTransform(s).java`.

Top-level fields:
- `parent`: model ID. Special values are `builtin/generated` (the extruded-sprite generator, `ItemModelGenerator`; vanilla `item/generated` uses it) and `builtin/missing`.
- `ambientocclusion`: bool. Inherited from the parent when absent, default true.
- `gui_light`: `"front"` or `"side"`. Items use `front`, blocks use `side`.
- `textures`: map of slot -> value. A value is:
  - `"#otherSlot"`, a reference;
  - `"ns:path"`, a sprite ID;
  - `{"sprite": "ns:path", "force_translucent": true}` (new in 26.1).

  Special slots: `particle`, and `layer0..layer4` for `builtin/generated`. Child textures override the parent's; references resolve across the whole chain. Unresolved references are logged as "Unresolved texture references in ...".
- `display`: per-context transforms with keys `thirdperson_righthand`, `thirdperson_lefthand`, `firstperson_righthand`, `firstperson_lefthand`, `head`, `gui`, `ground`, `fixed`, **`on_shelf`** (new, for shelf blocks). Each has `rotation` [deg xyz], `translation` [px] and `scale`.
  - Translation is clamped to +/-80 px (5 blocks); scale is clamped to +/-4.
  - A missing context means no transform; left-hand contexts fall back to the right-hand ones.
  - Context names come from common `world/item/ItemDisplayContext.java`.
- `elements`: list of cuboids. If absent, the parent's elements are used.

Element fields:
- `from`, `to`: [x,y,z] in model pixels. Each component must be within -16..32.
- `rotation`, one of two forms:
  - `{"origin":[x,y,z], "axis":"x|y|z", "angle": <any float>, "rescale": bool}`;
  - `{"origin":[...], "x":deg, "y":deg, "z":deg, "rescale": bool}`, Euler angles applied as ZYX.
  - Arbitrary angles are allowed (vanilla `template_hanging_sign_rot_3.json` uses `-67.5`). `rescale` scales the result to keep the block footprint.
- `shade_direction_override`: `"down|up|north|south|west|east"`. The element is shaded as if every face faced that direction. Vanilla uses `"up"` in 333 elements (crosses, plants). **`shade` no longer exists** and is silently ignored.
- `light_emission`: int 0..15, otherwise a parse error. The faces render at at least this block-light level.
- `faces`: map from direction to a face; 1-6 entries.

Face fields:
- `texture` (required, normally `#slot`).
- `uv`: [u1,v1,u2,v2] in 0..16. Defaults to the element's projected bounds. Reversing min and max mirrors the face.
- `rotation`: 0/90/180/270.
- `cullface`: a direction; the face is culled when that neighbour hides it.
- `tintindex`: int, default -1 (no tint).

Real example, jar `models/block/cross_emissive.json` (excerpt):
```json
{ "ambientocclusion": false, "textures": { "particle": "#cross" },
  "elements": [ { "from": [0.8,0,8], "to": [15.2,16,8],
      "rotation": { "origin": [8,8,8], "axis": "y", "angle": 45, "rescale": true },
      "shade_direction_override": "up", "light_emission": 15,
      "faces": { "north": { "uv": [0,0,16,16], "texture": "#cross_emissive" },
                 "south": { "uv": [0,0,16,16], "texture": "#cross_emissive" } } } ] }
```
Forced-translucent texture, jar `models/block/green_stained_glass.json`:
```json
{ "parent": "minecraft:block/cube_all",
  "textures": { "all": { "force_translucent": true, "sprite": "minecraft:block/green_stained_glass" } } }
```

Render-pass rule (26.1+). For each quad, the client checks the UV region of its sprite (all animation frames):
- any partial alpha -> TRANSLUCENT;
- only 0/255 alpha with some 0 -> CUTOUT;
- otherwise SOLID.

`force_translucent` forces TRANSLUCENT. Items use `translucent*Sheet` or `cutout*Sheet` the same way. So to make glass translucent, give the PNG partial alpha, or use `force_translucent` when mipmaps would blur it. (src: client `renderer/texture/SpriteContents.java#computeTransparency`, `resources/model/geometry/BakedQuad.java` `MaterialInfo.of`.)

Item model JSON (`models/item/*.json`) is the same format, e.g. jar `models/item/diamond.json`: `{"parent":"minecraft:item/generated","textures":{"layer0":"minecraft:item/diamond"}}`.

---

## 5. Item model definitions (`assets/<ns>/items/<id>.json`)

Which file is used comes from the item stack's `minecraft:item_model` component (`DataComponents.ITEM_MODEL`, an `Identifier`). It defaults to the item ID, so `items/diamond.json` -> `assets/minecraft/items/diamond.json`. Custom items in a pure resource+data pack: set `item_model` to `mypack:ruby` and ship `assets/mypack/items/ruby.json`.

### 5.1 Root (src: client `renderer/item/ClientItem.java`)
| Field | Type | Default |
|---|---|---|
| `model` | item model (below) | required |
| `hand_animation_on_swap` | bool | `true` |
| `oversized_in_gui` | bool | `false`. When true, the GUI does not clip the model to the 16x16 slot. |
| `swap_animation_scale` | float | `1.0` |

### 5.2 Model types (`ItemModels.ID_MAPPER`, src: client `renderer/item/ItemModels.java`)
Every type except `empty` and `bundle/selected_item` accepts an optional `transformation`. This is either a 16-float row matrix or `{"translation":[3], "left_rotation":quat, "scale":[3], "right_rotation":quat}` (src: common `com/mojang/math/Transformation.java#EXTENDED_CODEC`).
- A quaternion is `[x,y,z,w]` or `{"angle":rad,"axis":[x,y,z]}`.
- Parent transformations compose with children. They are applied **after** the model's `display` transform.

| `type` | Fields |
|---|---|
| `minecraft:model` | `model` (model ID, required); `tints` (list of tint sources; index = face `tintindex`) |
| `minecraft:composite` | `models` (list of item models) |
| `minecraft:condition` | `property` + its fields; `on_true`; `on_false` |
| `minecraft:select` | `property` + its fields; `cases`: [{`when`: value or [values], `model`}]; `fallback` (optional) |
| `minecraft:range_dispatch` | `property` + its fields; `scale` (float, default 1, multiplies the value); `entries`: [{`threshold`: float, `model`}]; `fallback`. Picks the highest threshold <= value. |
| `minecraft:special` | `base` (model ID, which supplies `display`, particle texture and transforms); `model` (special model, §5.6) |
| `minecraft:bundle/selected_item` | none. Renders the selected bundle item. |
| `minecraft:empty` | none |

Real example, jar `items/bow.json`:
```json
{ "model": { "type": "minecraft:condition", "property": "minecraft:using_item",
  "on_false": { "type": "minecraft:model", "model": "minecraft:item/bow" },
  "on_true": { "type": "minecraft:range_dispatch", "property": "minecraft:use_duration", "scale": 0.05,
    "entries": [ { "threshold": 0.65, "model": { "type": "minecraft:model", "model": "minecraft:item/bow_pulling_1" } },
                 { "threshold": 0.9,  "model": { "type": "minecraft:model", "model": "minecraft:item/bow_pulling_2" } } ],
    "fallback": { "type": "minecraft:model", "model": "minecraft:item/bow_pulling_0" } } } }
```
Select with `local_time`, from jar `items/chest.json`:
```json
{ "model": { "type": "minecraft:select", "property": "minecraft:local_time", "pattern": "MM-dd",
  "cases": [ { "when": ["12-24","12-25","12-26"], "model": { "type": "minecraft:special", "base": "minecraft:item/chest",
               "model": { "type": "minecraft:chest", "texture": "minecraft:christmas" } } } ],
  "fallback": { "type": "minecraft:special", "base": "minecraft:item/chest",
                "model": { "type": "minecraft:chest", "texture": "minecraft:normal" } } } }
```

### 5.3 `condition` properties (src: client `renderer/item/properties/conditional/*`)
| `property` | Extra fields |
|---|---|
| `minecraft:using_item` | none |
| `minecraft:broken` | none |
| `minecraft:damaged` | none |
| `minecraft:fishing_rod/cast` | none |
| `minecraft:bundle/has_selected_item` | none |
| `minecraft:selected` | none. True when the item is in the selected hotbar slot. |
| `minecraft:carried` | none. True when the item is on the cursor. |
| `minecraft:extended_view` | none. True while Shift shows the extended tooltip view. |
| `minecraft:view_entity` | none |
| `minecraft:keybind_down` | `keybind` (key mapping name, e.g. `"key.jump"`; invalid names fail to parse) |
| `minecraft:has_component` | `component` (component ID); `ignore_default` (bool, default false) |
| `minecraft:component` | `predicate` (data-component predicate type ID) + `value` (that predicate's body), from `DataComponentPredicate.singleCodec("predicate")` |
| `minecraft:custom_model_data` | `index` (int, default 0). Reads `flags[index]`. |

### 5.4 `select` properties (src: client `renderer/item/properties/select/*`)
| `property` | Extra fields | `when` values |
|---|---|---|
| `minecraft:main_hand` | none | `left` / `right` |
| `minecraft:charge_type` | none | `none` / `rocket` / `arrow` |
| `minecraft:trim_material` | none | trim material ID |
| `minecraft:block_state` | `block_state_property` (string) | property value string, read from the `block_state` component |
| `minecraft:display_context` | none | `ItemDisplayContext` names (`gui`, `on_shelf`, ...) |
| `minecraft:local_time` | `pattern` (ICU date pattern, required); `locale` (default `""`); `time_zone` (optional) | formatted string |
| `minecraft:context_entity_type` | none | entity type ID |
| `minecraft:context_dimension` | none | dimension ID |
| `minecraft:component` | `component` (non-transient component ID) | values in that component's codec |
| `minecraft:custom_model_data` | `index` | `strings[index]` |

`when` accepts a single value or a list. Duplicate case values are rejected.

### 5.5 `range_dispatch` properties (src: client `renderer/item/properties/numeric/*`)
| `property` | Extra fields |
|---|---|
| `minecraft:bundle/fullness` | none |
| `minecraft:damage` | `normalize` (bool, default true) |
| `minecraft:count` | `normalize` (default true) |
| `minecraft:cooldown` | none |
| `minecraft:time` | `source`: `daytime` / `moon_phase` / `random`; `wobble` (default true) |
| `minecraft:compass` | `target`: `none` / `lodestone` / `spawn` / `recovery`; `wobble` (default true) |
| `minecraft:crossbow/pull` | none |
| `minecraft:use_cycle` | `period` (positive float, default 1) |
| `minecraft:use_duration` | `remaining` (default false) |
| `minecraft:custom_model_data` | `index`. Reads `floats[index]`. |

### 5.6 Special models (`type` inside `special.model`; src: client `renderer/special/SpecialModelRenderers.java`)
| `type` | Fields |
|---|---|
| `minecraft:banner` | `color` (dye); `attachment`: `ground` (default) / `wall` |
| `minecraft:bell` | none |
| `minecraft:book` | `open_angle`, `page1`, `page2` (floats; pages 0..1) |
| `minecraft:chest` | `texture` (ID under `textures/entity/chest/`, e.g. `minecraft:normal`, `christmas`, `ender`, `trapped`, `copper_*`); `openness` (0..1, default 0); `chest_type`: `single`/`left`/`right` |
| `minecraft:conduit` | none |
| `minecraft:copper_golem_statue` | `texture`; `pose`: `standing`/`sitting`/`running`/`star` |
| `minecraft:decorated_pot` | none. Reads `pot_decorations`. |
| `minecraft:end_cube` | `effect`: `portal` / `gateway` |
| `minecraft:head` | `kind`: `skeleton`/`wither_skeleton`/`player`/`zombie`/`creeper`/`piglin`/`dragon`; `texture` (optional override); `animation` (float, default 0) |
| `minecraft:player_head` | none. Reads `profile`. |
| `minecraft:shield` | none |
| `minecraft:shulker_box` | `texture` (e.g. `minecraft:shulker`); `openness` |
| `minecraft:trident` | none |

Removed in 26.2: `bed`, `standing_sign`, `hanging_sign`. Beds are now `composite` items of `block/<color>_bed_head` and `_foot` models; see jar `items/red_bed.json`.

### 5.7 Tint sources (`tints[]`; src: client `color/item/ItemTintSources.java`)
| `type` | Fields |
|---|---|
| `minecraft:constant` | `value` (RGB int or [r,g,b] floats) |
| `minecraft:dye` | `default` (RGB). Reads `dyed_color`. |
| `minecraft:firework` | `default` |
| `minecraft:potion` | `default` |
| `minecraft:team` | `default` |
| `minecraft:grass` | `temperature`, `downfall` (0..1). Samples `colormap/grass.png`. |
| `minecraft:custom_model_data` | `index` (default 0), `default`. Reads `colors[index]`. |

(`minecraft:map_color` was removed in 26.3.)

Example, jar `items/grass_block.json`:
```json
{ "model": { "type": "minecraft:model", "model": "minecraft:block/grass_block",
  "tints": [ { "type": "minecraft:grass", "downfall": 1.0, "temperature": 0.5 } ] } }
```

### 5.8 Mods adding new types
These are `public static final LateBoundIdMapper` and Fabric's transitive access widener makes them accessible:
- `ItemModels.ID_MAPPER`
- `ItemTintSources.ID_MAPPER`
- `ConditionalItemModelProperties.ID_MAPPER`
- `SelectItemModelProperties.ID_MAPPER`
- `RangeSelectItemModelProperties.ID_MAPPER`
- `SpecialModelRenderers.ID_MAPPER`

Call `.put(Identifier, MapCodec)` during client init, as in fabric-api testmod `fabric-rendering-v1/src/testmodClient/.../SpecialBlockRendererTest.java`: `SpecialModelRenderers.ID_MAPPER.put(RENDERER_ID, Unbaked.MAP_CODEC)`.

---

## 6. Equipment assets (`assets/<ns>/equipment/<id>.json`)

Referenced by the `equippable` component's `asset_id` (an `equipment` asset key). Codec: client `resources/model/EquipmentClientInfo.java`.

```json
{ "layers": { "<layer_type>": [ { "texture": "ns:id", "dyeable": { "color_when_undyed": -6265536 }, "use_player_texture": false } ] },
  "trim_overrides": [ { "when": { "material": "minecraft:diamond", "pattern": "minecraft:coast" },
                        "texture": "ns:trim_pattern_asset", "palette": "minecraft:trim/diamond_darker" } ] }
```
- `layers` is a non-empty map; each list is non-empty.
- Layer types: `humanoid`, `humanoid_leggings`, `humanoid_baby`, `wings`, `wolf_body`, `horse_body`, `llama_body`, `pig_saddle`, `strider_saddle`, `camel_saddle`, `camel_husk_saddle`, `horse_saddle`, `donkey_saddle`, `mule_saddle`, `zombie_horse_saddle`, `skeleton_horse_saddle`, `happy_ghast_body`, `nautilus_saddle`, `nautilus_body`.
- The texture path is `textures/entity/equipment/<layer_type>/<path>.png`.
- `dyeable` is optional: present means the layer is tinted by `dyed_color`. `color_when_undyed` is optional RGB; without it the layer is hidden when undyed.
- `use_player_texture` (default false) uses the player's cape or elytra skin texture (vanilla `elytra.json`).
- `trim_overrides`: the first matching entry wins.
  - `when` needs at least one of `material` or `pattern`.
  - Each entry needs at least `texture` or `palette`. If `palette` is omitted, no recolor is applied.
  - The trim texture path is `textures/trims/entity/<layer_type>/<pattern asset path>.png`.

Real example, jar `equipment/diamond.json`:
```json
{ "layers": { "horse_body": [{"texture":"minecraft:diamond"}], "humanoid": [{"texture":"minecraft:diamond"}],
  "humanoid_baby": [{"texture":"minecraft:diamond"}], "humanoid_leggings": [{"texture":"minecraft:diamond"}],
  "nautilus_body": [{"texture":"minecraft:diamond"}] },
  "trim_overrides": [ { "palette": "minecraft:trim/diamond_darker", "when": { "material": "minecraft:diamond" } } ] }
```

Entity trim paletting (26.3):
- Each entity trim PNG carries `"palette": {"base_palette": "trim_base"}` in its `.png.mcmeta`.
- `PalettedTextureManager` (client `resources/palette/`) remaps the PNG's colors from `textures/palettes/trim_base.png` to the material's palette (`TrimMaterial.paletteId`, e.g. `minecraft:trim/gold` -> `textures/palettes/trim/gold.png`). It does this at runtime into dynamic `paletted/atlas_N` textures.
- A palette is a 1-row PNG whose pixels map index-to-index.

---

## 7. Textures and `.png.mcmeta`

Sections (src: client `resources/metadata/**`):

**`animation`** (`AnimationMetadataSection`, `AnimationFrame`):
- `frametime`: positive int, default 1 tick.
- `interpolate`: bool, default false.
- `width`, `height`: frame size. Default is a square of min(w,h).
- `frames`: list of ints, or `{"index": n, "time": t}`.

Frames are laid out row by row in the PNG. Excerpt from jar `textures/block/prismarine.png.mcmeta`:
```json
{ "animation": { "frametime": 300, "interpolate": true, "frames": [0,1,0,2,0,3, ...] } }
```

**`texture`** (`TextureMetadataSection`):
- `blur`: bool, default false (linear filtering).
- `clamp`: bool, default false.
- `mipmap_strategy`: `auto` (default) / `mean` / `cutout` / `strict_cutout` / `dark_cutout`.
- `alpha_cutoff_bias`: float, default 0.

Vanilla leaves use `{"texture": {"mipmap_strategy": "dark_cutout"}}` (jar `textures/block/acacia_leaves.png.mcmeta`); misc overlays use `blur: true`.

**`gui`** (`GuiMetadataSection`; only read for the `gui` atlas): `{"gui": {"scaling": {...}}}`. `type` is one of:
- `stretch` (default);
- `tile` with `width`, `height` (positive ints);
- `nine_slice` with `width`, `height`, `border` (an int, or `{left,top,right,bottom}`) and `stretch_inner` (bool, default false). Borders must leave a center slice.

Examples:
```json
{ "gui": { "scaling": { "type": "nine_slice", "width": 200, "height": 20, "border": 3 } } }
```
(jar `textures/gui/sprites/widget/button.png.mcmeta`)
```json
{ "gui": { "scaling": { "type": "nine_slice", "width": 12, "height": 5, "border": { "left": 5, "right": 5, "top": 1, "bottom": 1 } } } }
```
(jar `textures/gui/sprites/hud/locator_bar_background.png.mcmeta`)

**`villager`**: `{"villager": {"hat": "none|partial|full"}}`, on villager and zombie-villager profession/type textures.

**`palette`**: `{"palette": {"base_palette": "<palette id>"}}`, on entity trim textures (§6).

Other texture notes:
- **Colormaps**: `textures/colormap/grass.png`, `foliage.png` and `dry_foliage.png` (256x256), sampled by temperature/downfall. Biome water colors are biome data (data pack), not a colormap.
- **Tooltip styles**: the `tooltip_style` component `ns:foo` uses GUI sprites `ns:tooltip/foo_background` and `ns:tooltip/foo_frame` (src: client `gui/screens/inventory/tooltip/TooltipRenderUtil.java`).
- Atlas sprite IDs drop `textures/` and `.png`, and get the atlas prefix (e.g. `minecraft:block/stone`).

### 7.1 Atlas source types (src: client `renderer/texture/atlas/SpriteSources.java`, `sources/*`)
| `type` | Fields |
|---|---|
| `minecraft:directory` | `source` (dir under `textures/`, recursive), `prefix` (string prepended to sprite IDs) |
| `minecraft:single` | `resource` (texture ID), `sprite` (optional sprite ID; default = resource) |
| `minecraft:filter` | `pattern`: `{namespace?, path?}` regexes. Removes matching sprites added by **earlier** sources/packs. |
| `minecraft:unstitch` | `resource`; `divisor_x`/`divisor_y` (default 1); `regions`: [{`sprite`, `x`, `y`, `width`, `height`}] in divisor units |
| `minecraft:paletted_permutations` | `textures` (list of texture IDs); `palette_key` (palette ID); `permutations` (map suffix -> palette ID); `separator` (default `"_"`). Generates `<texture><sep><suffix>` sprites. |

Atlas files from all packs are **merged**, not replaced: sources are appended in pack order. Vanilla item trims:
```json
{ "type": "minecraft:paletted_permutations", "palette_key": "minecraft:trim_base",
  "permutations": { "gold": "minecraft:trim/gold", "...": "..." },
  "textures": [ "minecraft:trims/items/helmet_trim", "minecraft:trims/items/chestplate_trim" ] }
```
(Palette IDs resolve to `textures/palettes/<path>.png` as of 26.3.)

Fabric extras: `AtlasRegistry.register(AtlasManager.AtlasConfig)` adds a whole new atlas; `SpriteSourceRegistry.register(Identifier, MapCodec)` adds a source type (src: fabric-api `fabric-rendering-v1/src/client/java/net/fabricmc/fabric/api/client/rendering/v1/`).

---

## 8. Waypoint styles (`assets/<ns>/waypoint_style/<id>.json`)

Used by the locator bar (src: client `resources/WaypointStyle.java`):
- `near_distance`: int, default 128.
- `far_distance`: int, default 332.
- `sprites`: non-empty list.

Each sprite resolves to GUI sprite `hud/locator_bar_dot/<id>`. Example, jar `waypoint_style/bowtie.json`:
```json
{ "near_distance": 64, "sprites": [ "minecraft:bowtie", "minecraft:default_0", "minecraft:default_1", "minecraft:default_2", "minecraft:default_3" ] }
```

---

## 9. Fonts (`assets/<ns>/font/<id>.json`)

File: `{"providers": [ {type..., "filter": {...}} ]}` (src: client `gui/font/FontManager.java`, `gui/font/providers/*`, `com/mojang/blaze3d/font/SpaceProvider.java`).

`filter` (optional, all providers) is a map of font options to bools: `"uniform"` (Force Unicode font) and `"jp"` (Japanese glyph variants). The provider is used only when every listed option matches.

| `type` | Fields |
|---|---|
| `bitmap` | `file` (texture ID incl. `.png`, under `textures/`); `ascent` (int, required); `height` (int, default 8); `chars` (list of equal-length strings, one per glyph row; `\u0000` = empty) |
| `ttf` | `file` (ID under `font/`, e.g. `minecraft:foo.ttf`); `size` (default 11); `oversample` (default 1); `shift` ([x,y], default [0,0]); `skip` (string or list of strings of codepoints to skip) |
| `space` | `advances`: map codepoint -> float advance |
| `unihex` | `hex_file` (zip of `.hex`, ID under `font/`); `size_overrides`: [{`from`, `to`, `left`, `right`}] |
| `reference` | `id` (another font, e.g. `minecraft:include/default`) |

jar `font/default.json`:
```json
{ "providers": [
  { "type": "reference", "id": "minecraft:include/space" },
  { "type": "reference", "id": "minecraft:include/default", "filter": { "uniform": false } },
  { "type": "reference", "id": "minecraft:include/unifont" } ] }
```
- Vanilla fonts: `default`, `uniform`, `alt` (enchanting), `illageralt`, plus `include/{default,space,unifont}`.
- Text components choose one with `"font": "ns:id"`.

---

## 10. Shaders

### 10.1 What a resource pack can still override
Pipelines are Java objects (`RenderPipeline`, built in client `net/minecraft/client/renderer/RenderPipelines.java`). They fix blend state, depth state, vertex format, bind-group layouts (uniform and sampler names) and shader **defines**. A pack cannot change any of that.

A pack **can** replace the GLSL source a pipeline references:
- `withVertexShader("core/terrain")` -> `assets/minecraft/shaders/core/terrain.vsh`;
- fragment -> `.fsh`;
- includes -> `shaders/include/*.glsl`.

The loader collects every file under `shaders/` ending `.vsh`/`.fsh` keyed by `ns:path-without-extension`, plus `shaders/include/**.glsl` (src: client `renderer/ShaderManager.java#loadConfigs`). A failed compile falls back or crashes, depending on whether the pipeline is required or optional (`RenderPipelines.registerOptional`).

The jar has these core shaders:
- `block`, `terrain`, `entity`, `item`, `particle`, `text`, `glint`, `gui`, `clouds`, `sky`, `stars`, `panorama`, `lightmap`, `world_border`;
- `position*` variants, `screenquad`;
- `blit_screen`, `blit_depth`, `integrate_depth`;
- `animate_sprite*`;
- `oit_composite`, `oit_depth_bounds_cull`;
- `rendertype_{beacon_beam,crumbling,end_portal,entity_shadow,leash,lightning,lines,outline,water_mask}`.

Includes: `animation_sprite chunksection dynamictransforms fog globals light matrix oit oit_add_transmittance oit_common oit_depth_bounds oit_depth_sample oit_sample projection sample_lightmap terrainglobals texture_sampling`. Post shaders: `bits blit box_blur color_convolve entity_outline_box_blur entity_sobel invert rotscale spiderclip`.

### 10.2 GLSL rules in 26.3 (shaderc path)
Source: client `com/mojang/renderpearl/frontend/shaders/GlslCompiler.java`, `backend/opengl/GlPipelineRecompiler.java`.

Compilation path:
- Source goes through `shaderc_compile_into_spv` with target env Vulkan 1.2, `auto_bind_uniforms`, and the pipeline defines added as macros.
- The OpenGL backend converts SPIR-V back to GLSL 330 with SPIRV-Cross. Vulkan consumes the SPIR-V directly.

Header and includes:
- Header used by every vanilla shader:
  ```glsl
  #version 330
  #extension GL_ARB_separate_shader_objects : require
  ```
- Includes are `#include <minecraft:fog.glsl>` (an Identifier, resolved to `shaders/include/fog.glsl`). Missing or malformed IDs fail the compile. **`#moj_import` no longer works.**
- Shaders used before resource packs load (e.g. `core/position_tex_color`) inline their blocks instead of including (see the comment in the jar file).

Inputs, outputs and built-ins:
- Every `in`/`out` needs `layout(location = N)`. Vertex-to-fragment varyings are linked **by location only**.
- Vertex attribute names must match the vertex format: `Position`, `Color`, `UV0`, `UV1`, `UV2`, `UV3`, `Normal`, `LineWidth`, `ChunkPosition`, `ChunkVisibility` (src: client `DefaultVertexFormat.java`).
- Use Vulkan-style built-ins: `gl_VertexIndex` (see jar `core/screenquad.vsh`) and `gl_InstanceIndex`. `RENDERPEARL_INSTANCE_INDEX_INCLUDES_BASE_INSTANCE` tells you which instance-index semantics apply.

Uniforms and samplers:
- Non-opaque uniforms must live in `layout(std140) uniform <Name> { ... };` blocks whose names match the pipeline's bind groups.
- Only samplers can be loose (`uniform sampler2D Sampler0;`).
- Bind-group names (src: client `BindGroupLayouts.java`):
  - uniform blocks: `DynamicTransforms`, `Projection`, `ChunkSection`, `TerrainUniform`, `Fog`, `Globals`, `Lighting`, `CloudInfo`, `LightmapInfo`, `SpriteAnimationInfo`;
  - texel buffer: `CloudFaces`;
  - samplers: `Sampler0`, `Sampler1`, `Sampler2` (lightmap), `InSampler`, `DissolveMaskSampler`, `Sprite`, `CurrentSprite`, `NextSprite`, `GlintSampler`, `DepthBoundsSampler`, `Coeff0..N` (OIT).

Example blocks (jar `shaders/include/globals.glsl`, `dynamictransforms.glsl`):
```glsl
layout(std140) uniform Globals { ivec3 CameraBlockPos; float GlintAlpha; vec3 CameraOffset; float GameTime;
                                 vec2 ScreenSize; int MenuBlurRadius; int UseRgss; };
layout(std140) uniform DynamicTransforms { mat4 ModelViewMat; mat4 TextureMat; vec4 ColorModulator; vec3 ModelOffset; };
```
Block member order is data layout. Copy vanilla blocks exactly; 26.3 reshuffled `Globals` and `DynamicTransforms`.

Defines you will see:
- `ALPHA_CUTOUT`, `MULTIDRAW_TERRAIN`, `IS_GUI`, `IS_SEE_THROUGH`, `IS_GRAYSCALE`;
- OIT: `OIT`, `OIT_DEPTH_BOUNDS`, `OIT_TRANSMITTANCE`, `OIT_ACCUMULATE`, `OIT_ALPHA_ONLY`, `OIT_ADDITIVE`, `OIT_ALWAYS_WRITE_DEPTH`, `OIT_OPAQUE_PARTS_THRESHOLD`, `OIT_WAVELET_RANK`, `OIT_COEFF_COUNT`, `OIT_COEFF_ATTACHMENT_COUNT`;
- backend: `RENDERPEARL_DEPTH_IS_ZERO_TO_ONE`, `RENDERPEARL_EXPLICIT_DEPTH_INVARIANCE`.

Translucent-capable shaders (terrain, entity, item, particle, text, clouds, ...) are compiled several times with different OIT defines and must keep those `#ifdef` branches.

### 10.3 Post effects (`assets/<ns>/post_effect/<id>.json`; src: client `renderer/PostChainConfig.java`, `PostPass.java`, `UniformValue.java`)

Top level:
- `targets`: map ID -> `{width?, height?, persistent: false, clear_color: 0 (ARGB)}`. An empty `{}` means screen-sized.
- `passes`: list.

Each pass:
- `vertex_shader` and `fragment_shader` (shader IDs, e.g. `minecraft:core/screenquad`, `minecraft:post/blit`).
- `output` (target ID).
- `inputs`. Each input is either:
  - `{sampler_name, target, use_depth_buffer=false, bilinear=false}`, or
  - `{sampler_name, location (texture ID), width, height, bilinear=false}`.
  
  Sampler names must be unique. **In GLSL the sampler is `<sampler_name>Sampler`** (e.g. `"In"` -> `uniform sampler2D InSampler;`).
- `uniforms`: map from block name to a list of `{name, type, value}`. `type` is one of `int`, `ivec3`, `float`, `vec2`, `vec3`, `vec4`, `matrix4x4`. List order is the std140 member order.

Every pass also gets a `SamplerInfo` block `{ vec2 OutSize; vec2 InSize; }` (more sizes per input).

External targets:
- For `end_of_frame`, `/posteffect`, spectator effects and `blur`, only `minecraft:main` is available.
- `entity_outline` additionally gets `minecraft:entity_outline`.

Fragment example, jar `shaders/post/invert.fsh`:
```glsl
uniform sampler2D InSampler;
layout(location = 0) in vec2 texCoord;
layout(std140) uniform SamplerInfo { vec2 OutSize; vec2 InSize; };
layout(std140) uniform InvertConfig { float InverseAmount; };
layout(location = 0) out vec4 fragColor;
void main(){ vec4 c = texture(InSampler, texCoord); fragColor = vec4(mix(c, 1.0 - c, InverseAmount).rgb, 1.0); }
```
(With the matching `"uniforms": {"InvertConfig": [{"name":"InverseAmount","type":"float","value":0.8}]}` in jar `post_effect/invert.json`.)

Which post effects run (src: client `renderer/GameRenderer.java`):
- `minecraft:end_of_frame` (26.3). Always requested; a pack that ships `post_effect/end_of_frame.json` gets a permanent full-screen effect that runs before the others.
- Player post effects set by `/posteffect add|remove|clear|list <targets> <id>`. Needs permission level 2; the server sends `ClientboundPostEffectsPacket` (src: common `server/commands/PostEffectCommand.java`). This is how a data pack plus resource pack combo triggers custom screen effects.
- Spectating a creeper, spider or enderman gives `creeper`, `spider` or `invert`.
- `blur` is used for the menu background blur and reads `Globals.MenuBlurRadius`.
- `entity_outline` is used for the glowing effect.

There is no `transparency` chain any more (OIT is built in).

---

## 11. Sounds (`assets/<ns>/sounds.json`, `sounds/**.ogg`) and language files

### 11.1 `sounds.json` (src: client `resources/sounds/SoundEventRegistrationSerializer.java`, `Sound.java`)
```json
{ "block.example.chime": {
    "replace": false,
    "subtitle": "subtitles.block.example.chime",
    "sounds": [ "example:block/chime1",
                { "name": "example:block/chime2", "volume": 1.0, "pitch": 1.0, "weight": 1, "stream": false,
                  "preload": false, "attenuation_distance": 16, "type": "file" },
                { "name": "minecraft:block.note_block.bell", "type": "event" } ] } }
```
- Event keys are sound-event IDs within the file's namespace.
- `replace: true` discards lower packs' entries for that event; otherwise lists merge.
- `name` resolves to `sounds/<path>.ogg` when `type` is `file`.
- Ogg Vorbis only. Positional sounds must be **mono**.
- Mods register `SoundEvent`s in Java (`Registries.SOUND_EVENT`); a pack can only add sound files to existing events, or define events that commands play by ID (`/playsound` accepts unregistered IDs).

### 11.2 Language files
- `lang/<code>.json` is a flat `{ "key": "value" }` map, merged across packs and namespaces; later packs win.
- Placeholders are `%s` and `%1$s`.
- `lang/deprecated.json` (vanilla only) has `{"removed":[keys], "renamed":{old:new}}`. `ClientLanguage` applies it, so renamed keys in old packs keep working (src: common `locale/DeprecatedTranslationsInfo.java`).

---

## 12. Texts and misc
- `texts/splashes.txt`: one splash per line. Packs replace the whole file (it is not merged).
- `texts/credits.json`: `[{"section": "...", "disciplines": [{"discipline": "...", "titles": [{"title": "...", "names": ["..."]}]}]}]`.
- `texts/end.txt` (the end poem; `PLAYERNAME` is substituted) and `texts/postcredits.txt`.
- `regional_compliancies.json`: `{"KOR": [{"delay": 1440, "period": 60, "title": "<lang key>", "message": "<lang key>"}]}`.
- Panorama: `textures/gui/title/background/panorama_0..5.png`.

---

## 13. Fabric mods shipping assets
- A mod's `src/main/resources/assets/<modid>/...` is served as a mod resource pack, using every format above. Use your mod ID as the namespace.
- Built-in optional packs: `ResourceLoader.registerBuiltinPack(Identifier id, ModContainer container, PackActivationType type)`, with overloads taking a `Component` display name and/or a sub-path. Content lives in `resources/resourcepacks/<id path>/` (src: fabric-api `fabric-resource-loader-v1/src/main/java/net/fabricmc/fabric/api/resource/v1/ResourceLoader.java`).
- Datagen providers exist for models, blockstates and item definitions (fabric-data-generation-api). See the separate datagen knowledge file if present.
- Do not look for `BlockRenderLayerMap`/`ItemBlockRenderTypes`. Layering is per texture (§4).

---

## 14. Shader packs: Iris (and OptiFine)

### 14.1 Availability for 26.x (checked on Modrinth API, 2026-11-14)
- **Iris** `1.11.7+26.3-fabric` (2026-09-30) and `-neoforge`. 1.11.5/1.11.6 were released on 26.3 launch day.
  - Iris requires Sodium; 1.11.7 declares `mc26.3-0.9.2-fabric` as its dependency.
  - Branches `26.1`, `26.2`, `26.3` exist on github.com/IrisShaders/Iris.
  - The 26.3 build itself uses Loom 1.17.20, Loader 0.19.3 and Fabric API 0.160.5+26.3 (Iris `build.gradle.kts`).
- **Sodium** for 26.3: latest is `mc26.3-0.9.3-beta.5` (2026-11-12); `0.9.2` is the stable release paired with Iris.
- **OptiFine** download page lists up to Minecraft 26.2 (HD U K2 pre-releases). There is no 26.3 build as of this date. UNVERIFIED beyond the page listing.

### 14.2 Iris cannot run on the Vulkan backend
`IrisMixinPlugin` reads `options.txt` `preferredGraphicsBackend`. If that contains `vulkan`, it applies only `VKOnly` mixins, and the shader-pack screen offers "Iris cannot run when using Vulkan. Would you like to switch to OpenGL?" (src: Iris `common/src/main/java/net/irisshaders/iris/mixin/IrisMixinPlugin.java`, `compat/sodium/config/ShaderPackScreenPlaceholder.java`). Iris patches `GlRenderPipeline` and `GlProgram` directly.

### 14.3 Shader-pack format basics (OptiFine/Iris compatible)
A zip or folder in `.minecraft/shaderpacks/`, containing a `shaders/` directory.

- Programs are `<name>.vsh`, `.fsh`, optional `.gsh`, `.tcs`/`.tes` (tessellation), and `.csh` (compute; `_a`..`_z` suffixes for extra dispatches). All are GLSL; the pack picks its own `#version`.
- Program groups (Iris `shaderpack/loading/ProgramGroup.java`), in pipeline order:
  - `setup` and `begin` (compute);
  - `shadow` (`shadow`, `shadow_solid`, `shadow_cutout`, `shadow_water`, `shadow_entities`, `shadow_lightning`, `shadow_block`);
  - `shadowcomp[N]`;
  - `prepare[N]`;
  - `gbuffers_*`: `basic`, `line`, `textured`, `textured_lit`, `skybasic`, `skytextured`, `clouds`, `terrain`, `terrain_solid`, `terrain_cutout`, `damagedblock`, `block`, `block_translucent`, `beaconbeam`, `item`, `entities`, `entities_translucent`, `lightning`, `particles`, `particles_translucent`, `entities_glowing`, `armor_glint`, `spidereyes`, `hand`, `weather`, `water`, `hand_water`;
  - `deferred[N]` and `composite[N]`, where N is up to 99;
  - `final`;
  - `dh_terrain`, `dh_water`, `dh_generic`, `dh_shadow` for Distant Horizons.
  
  Missing gbuffers programs fall back along the OptiFine fallback chain (e.g. `terrain` -> `textured_lit` -> `textured` -> `basic`).
- Config files at `shaders/`:
  - `shaders.properties`: options, screens/sliders, `program.<name>.enabled`, blend/alpha overrides, custom textures, `iris.features.required/optional`, uniforms/variables.
  - `block.properties` (`block.<id>=<block ids/states/tags>` and `layer.<type>=...`), `item.properties`, `entity.properties`. These feed `mc_Entity`, `currentRenderedItemId` and so on.
  - `dimension.properties` (`dimension.<folder>=<dimension id>`). Without it, the legacy folders `world0`, `world-1` and `world1` are used for per-dimension program overrides.
  - `lang/*.lang` for option names.
- Iris feature flags (`iris.features.required`): `SEPARATE_HARDWARE_SAMPLERS`, `HIGHER_SHADOWCOLOR`, `CUSTOM_IMAGES`, `PER_BUFFER_BLENDING`, `COMPUTE_SHADERS`, `TESSELLATION_SHADERS`, `ENTITY_TRANSLUCENT`, `REVERSED_CULLING`, `BLOCK_EMISSION_ATTRIBUTE`, `CAN_DISABLE_WEATHER`, `SSBO`, `FADE_VARIABLE`, `TEXTURE_FILTERING` (Iris `FeatureFlags.java`).
- Iris PBR support: `_n`/`_s` normal and specular textures next to normal textures (Iris `pbr/`). Format details: UNVERIFIED beyond the existence of the PBR atlas code.

### 14.4 How a Fabric mod's rendering interacts with Iris and Sodium
- Resource-pack and mod **core shader overrides are ignored when an Iris shader pack is active**. Iris substitutes its own programs per `RenderPipeline` (Iris doc `docs/development/compatibility/core-shaders.md`).
- Iris maps vanilla pipelines to programs in a static table (`IrisPipelines`). A mod's own `RenderPipeline` that is not mapped gets `null` and renders with its own unpatched shader. With a shader pack on, that usually means it draws into gbuffers with the wrong outputs: invisible, unlit, or broken.
- Fix with the Iris API `net.irisshaders.iris.api.v0.IrisApi`:
  ```java
  if (FabricLoader.getInstance().isModLoaded("iris")) {
      IrisApi.getInstance().assignPipeline(MY_PIPELINE, IrisProgram.ENTITIES_TRANSLUCENT);
      // also: assignPipelineShadow(pipeline, IrisShadowProgram.X), isShaderPackInUse(), isRenderingShadowPass(),
      //       registerShadowRenderCallback(cb), getSunPathRotation(), isReverseZDuringShaders()
  }
  ```
  - `IrisProgram` values: `BASIC`, `TEXTURED`, `TERRAIN`, `TERRAIN_SOLID`, `TERRAIN_CUTOUT`, `TRANSLUCENT`, `SKY_BASIC`, `SKY_TEXTURED`, `ARMOR_GLINT`, `ENTITIES`, `ENTITIES_TRANSLUCENT`, `CLOUDS`, `BLOCK`, `BLOCK_TRANSLUCENT`, `HAND`, `HAND_TRANSLUCENT`, `PARTICLES`, `PARTICLES_TRANSLUCENT`, `EMISSIVE_ENTITIES`, `BEACON_BEAM`, `LINES`.
  - The `RenderPipeline` type here is `com.mojang.renderpearl.api.pipeline.RenderPipeline`.
  - `assignPipeline` throws if the pipeline is already assigned.
  - Compile against the Iris API jar as `compileOnly`; the Maven coordinates are UNVERIFIED (Iris has historically published to `maven.irisshaders.dev` and Modrinth Maven).
- The safest route is to reuse vanilla pipelines and `RenderType`s (entity cutout or translucent, item sheets) and emit standard vertex formats. Those are already mapped. Avoid custom vertex formats.
- **Sodium** replaces chunk (terrain) rendering with its own shaders (`assets/sodium/shaders/blocks/block_layer_opaque.{vsh,fsh}`, `#version 460 core`, which also `#include <minecraft:oit.glsl>`). Consequences:
  - Resource-pack overrides of `core/terrain` do nothing with Sodium installed.
  - Custom block models must go through vanilla `BlockStateModel` or the Fabric Renderer API. Sodium ships its own FRAPI implementation (module `frapi`, `SodiumRenderer`), so Indium is obsolete; its last build targets 1.20.6.
  - Per-quad emissive and transparency info from JSON models is respected by Sodium. UNVERIFIED in detail for `light_emission`.
- Iris treats Sodium terrain pipelines specially: it checks for a `sodium` namespace, blend state and the `CUTOUT` define (`IrisPipelines.getPipeline`).

---

## 15. Non-vanilla resource-pack features (mods)
Vanilla has no CTM, random textures, emissives, CIT or custom entity models. The `.jem` CEM format is OptiFine's; vanilla entity models are Java `LayerDefinition`s and cannot be changed from JSON.

26.3 builds that exist on Modrinth:
- **Continuity** `3.0.1+26.3` (2026-09-16): OptiFine-format connected textures (`assets/<ns>/optifine/ctm/**.properties`, methods such as `ctm`, `horizontal`, `vertical`, `top`, `random`, `repeat`, `fixed`, `overlay*`) and emissive textures (`_e` suffix via `optifine/emissive.properties`). It requires FRAPI (Indigo or Sodium).
- **Entity Texture Features** `7.2.7` (Fabric, NeoForge): random/custom entity textures (`optifine/random/entity/...`), emissive entities, blinking.
- **Entity Model Features** `3.3.12`: OptiFine CEM `.jem`/`.jpm` models.
- **Polytone** `26.3-6.8.7`: data-driven colormaps, block and fluid tints, biome fog colors, particle and sound tweaks.
- **CIT**: CIT Resewn stopped at 1.19.4. There is no maintained 26.3 CIT mod (search found only niche "variants-cit"). Use vanilla item model definitions instead (§5), e.g. `select` on `minecraft:component` with `component: "minecraft:custom_name"`, `condition` with `minecraft:component` predicates, or `custom_model_data`.
- **Fresh Animations** (`1.11.2`, 26.2 and 26.3) is a resource pack that needs ETF+EMF.

---

## 16. Tooling and testing

### 16.1 Blockbench
Version 5.2.2 is current (git tags; source updated 2026-10).
- Format "Java Block/Item" (`js/formats/java/java_block.ts`) has a per-project `java_block_version` up to `'26.3'`.
- It exports:
  - `light_emission`;
  - `shade_direction_override` (only when the project version is >= 26.3; older versions get `shade: false`);
  - free and Euler rotations (>= 1.21.11; otherwise angles snap to 22.5);
  - `force_translucent` texture objects.
- On import it maps `shade: false` to `shade_direction_override: "up"`.
- It writes models only. Write `items/*.json` and blockstates by hand, with datagen, or with misode's generators. Exactly which asset generators misode offers for 26.3 is UNVERIFIED; it has historically had model, blockstate, item definition, atlas, font, equipment and post-effect editors.
- "Modded Entity" exports Java code for Fabric/Forge entity models. Bedrock formats are irrelevant here.

### 16.2 Textures
- Any PNG editor works (Aseprite, GIMP, Krita, Paint.NET). Keep 8-bit RGBA.
- Remember that **any partial alpha now moves a quad to the translucent pass**. Clean up semi-transparent edge pixels on cutout textures (leaves, plants), or they become translucent and sort.
- Animated strips: frames stacked vertically (or set `width`/`height`).
- PackSquash can optimise and validate packs (UNVERIFIED 26.3 support).

### 16.3 Quick test loop
- Put an **unzipped folder** in `.minecraft/resourcepacks/` and enable it. Edit files in place, then press **F3+T** to reload resources (debug keys are now rebindable options in 26.x; `KeyboardHandler` -> `reloadResourcePacks()`).
- `/reload` reloads data packs only.
- Watch `latest.log` for:
  - `Unable to parse ...` and `Exception loading blockstate definition` (blockstates);
  - `Missing textures in model ...` and `Unresolved texture references in ...`;
  - `Couldn't parse GLSL: ...` (shaderc errors with file and line);
  - `Failed to parse post chain at ...`;
  - `Invalid sounds.json in resourcepack`.
- **F3+S** dumps every atlas and dynamic texture (`TextureManager#dumpAllSheets`) to the debug texture folder (`TextureUtil.getDebugTexturePath`, under the game dir) to check stitching and paletting.
- Post effects: `/posteffect add @s mypack:fx` (cheats or op) and `/posteffect clear @s`. Or name the file `post_effect/end_of_frame.json` for always-on.
- Item models: `/give @s stick[item_model="mypack:ruby"]`. Component-driven variants: `/give @s stick[custom_model_data={floats:[1.0],flags:[true],strings:["a"],colors:[16711680]}]`.
- Shaders: test on **both** Graphics API settings (OpenGL and Vulkan). Code that SPIRV-Cross accepts on GL still has to be valid Vulkan GLSL for shaderc, which is the same front end for both.
- Fabric dev: assets in `src/main/resources/assets/<modid>/` hot-reload with F3+T when running from the IDE with resources on the classpath (the Gradle `runClient` task copies resources, so rebuild resources first). Do not run Gradle in the shared build here.

---

## 17. Where to look in the source
- Pack metadata: common `net/minecraft/server/packs/**` (`PackFormat`, `OverlayMetadataSection`, `ResourceFilterSection`), client `resources/metadata/**`.
- Models: client `resources/model/cuboid/*` (JSON model), `resources/model/sprite/*` (`TextureSlots`, `Material`, `MaterialBaker`, `AtlasManager`), `renderer/block/dispatch/**` (blockstates), `renderer/block/BuiltInBlockModels.java`.
- Item definitions: client `renderer/item/**`, `renderer/special/**`, `color/item/**`.
- Atlases and textures: client `renderer/texture/atlas/**`, `renderer/texture/SpriteContents.java`, `MipmapStrategy.java`, `resources/palette/**`.
- Shaders: client `renderer/RenderPipelines.java`, `BindGroupLayouts.java`, `ShaderManager.java`, `PostChain*.java`, `PostPass.java`, `com/mojang/renderpearl/**` (backend abstraction, shaderc/SPIRV-Cross).
- Fonts: client `gui/font/**`, `com/mojang/blaze3d/font/**`.
- Sounds: client `resources/sounds/**`, `sounds/SoundManager.java`.
- Changelogs: minecraft.wiki "Java Edition 26.1/26.2/26.3", sections "Item models", "Shaders", "Textures", "Resource pack", "Texture atlas".
