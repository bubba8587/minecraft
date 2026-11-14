# Models on Minecraft 26.3: blockstates, block models, item model definitions, Fabric Model Loading API, FRAPI

Scope: resource-pack model formats as parsed by the 26.3 client, and Fabric's model APIs (`fabric-model-loading-api-v1` 8.1.1, `fabric-renderer-api-v1` 17.0.19, Indigo 9.1.27). Verified 2026-11-14 against 26.3 decompiled source (`/tmp/claude-0/mcsrc/client`), vanilla assets (`/tmp/claude-0/mcsrc/vanilla-jar/assets/minecraft`), Fabric API 26.3 sources and testmods. Index: `client.md`. Rendering/tints/chunk layers: `client-rendering.md`.

Source shorthands: `client` = `/tmp/claude-0/mcsrc/client/`, `assets` = `/tmp/claude-0/mcsrc/vanilla-jar/assets/minecraft/`, `fapi:<module>` = `/tmp/claude-0/mcsrc/fabric-api/<module>/src/...`.

## 0. What changed (1.21.x -> 26.3) that trips people up
- 1.21.4 introduced item model definitions in `assets/<ns>/items/<item>.json`; **every item needs one** (no more implicit `models/item/<id>.json` lookup). Model `"overrides"` (predicate-based) are gone; use `select`/`condition`/`range_dispatch`.
- 26.x element format changes: element-level `"shade": false` is **no longer read** (silently ignored); use `"shade_direction_override": "up"` (vanilla converted 350 models). Element rotation accepts either `{"origin", "axis", "angle", "rescale"}` or free Euler `{"origin", "x", "y", "z", "rescale"}`. Elements can set `"light_emission": 0..15` (integer only; non-integer throws).
- Texture references may be objects: `{"sprite": "ns:block/x", "force_translucent": true}` (forces the TRANSLUCENT chunk layer). Render layer is otherwise derived from sprite alpha (`client-rendering.md` section 9).
- Blockstate variants gained `"z"` rotation (`x`, `y`, `z` in 90 degree steps, `uvlock`, `weight`).
- New display context `on_shelf` (shelf block) besides `thirdperson_*`, `firstperson_*`, `head`, `gui`, `ground`, `fixed`.
- Item model definitions accept an optional `"transformation"` on `model`, `condition`, `select`, `range_dispatch`, `special` nodes.
- Java class renames: `BakedModel` is gone (since 1.21.5) -> `BlockStateModel` (+ `BlockStateModelPart`) for blocks, `ItemModel` for items; `BlockModel` (JSON) -> `CuboidModel` (`client/resources/model/cuboid/`); `ModelBakery`/`ModelManager` remain. FRAPI `FabricBakedModel` -> `FabricBlockStateModel`; Model Loading `FabricBakedModelManager` -> `FabricModelManager`, `WrapperUnbakedGroupedBlockStateModel` -> `WrapperUnbakedRootBlockStateModel`.

## 1. Blockstate files: `assets/<ns>/blockstates/<block>.json`
Codec: client `renderer/block/dispatch/BlockStateModelDispatcher.java`, `Variant.java`, `multipart/*`.
```json
{ "variants": {
    "axis=x": { "model": "minecraft:block/oak_log_horizontal", "x": 90, "y": 90 },
    "axis=y": { "model": "minecraft:block/oak_log" },
    "axis=z": [ { "model": "modid:block/a", "weight": 3 }, { "model": "modid:block/b", "uvlock": true } ]
} }
```
```json
{ "multipart": [
    { "apply": { "model": "minecraft:block/oak_fence_post" } },
    { "when": { "north": "true" }, "apply": { "model": "minecraft:block/oak_fence_side", "uvlock": true } },
    { "when": { "OR": [ { "east": "true" }, { "west": "low|tall" } ] }, "apply": { "model": "modid:block/x", "y": 90 } }
] }
```
- Variant keys: `""` for blocks without properties; `prop=value,prop2=value` partial matches allowed. Variant fields: `model` (required), `x`, `y`, `z` (0/90/180/270), `uvlock` (bool), `weight` (in arrays, positive int, default 1).
- Multipart `when`: map of property -> `"a|b"` alternatives, or `{"AND": [...]}` / `{"OR": [...]}`; `"!value"` negates a term (src: client `multipart/KeyValueCondition.java`, `NEGATE = "!"`).
- Custom Fabric blockstate model types: `"fabric:type": "<ns>:<id>"` inside a variant (section 5.3).

## 2. Block/item model JSON: `assets/<ns>/models/<path>.json`
Parser: client `resources/model/cuboid/CuboidModel.java`, `CuboidModelElement.java`, `CuboidFace.java`, `ItemTransforms.java`.
```json
{
  "parent": "minecraft:block/block",
  "ambientocclusion": true,
  "gui_light": "side",
  "textures": {
    "particle": "#all",
    "all": "modid:block/frame",
    "glass": { "sprite": "modid:block/frame_glass", "force_translucent": true }
  },
  "elements": [
    { "from": [0, 0, 0], "to": [16, 16, 16],
      "rotation": { "origin": [8, 8, 8], "axis": "y", "angle": 22.5, "rescale": false },
      "shade_direction_override": "up",
      "light_emission": 15,
      "faces": {
        "north": { "texture": "#all", "uv": [0, 0, 16, 16], "cullface": "north", "rotation": 90, "tintindex": 0 },
        "up":    { "texture": "#glass" }
      } }
  ],
  "display": {
    "gui": { "rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625] },
    "on_shelf": { "rotation": [0, 180, 0] }
  }
}
```
- Top-level: `parent`, `textures`, `elements`, `display`, `gui_light` (`side`|`front`), `ambientocclusion`. Plus `"fabric:type"` for Fabric custom deserializers.
- Element: `from`/`to` (0..16, may exceed slightly), `rotation` (`origin` + either `axis`/`angle` or `x`/`y`/`z` Euler; `rescale`), `shade_direction_override` (direction name), `light_emission` (int 0-15), `faces` (1-6 of `down/up/north/south/west/east`).
- Face: `texture` (`#slot` or id), `uv` (4 floats; default from element bounds), `cullface`, `rotation` (0/90/180/270), `tintindex` (-1 default; index into the block's `BlockTintSource` list or the item model's `tints`).
- Item sprites: parent `minecraft:item/generated` with `layer0`, `layer1`... (or `item/handheld` for tools).
- Display contexts: `thirdperson_righthand`, `thirdperson_lefthand`, `firstperson_righthand`, `firstperson_lefthand`, `head`, `gui`, `ground`, `fixed`, `on_shelf` (`rotation`, `translation`, `scale`).
- Texture ids are atlas sprite ids (`ns:block/x` -> `assets/ns/textures/block/x.png`). Since 26.x there are separate `blocks` and `items` atlases (`TextureAtlas.LOCATION_BLOCKS` / `LOCATION_ITEMS`; `atlases/blocks.json` covers `textures/block/**` plus a few entity sprites, `atlases/items.json` covers `textures/item/**` + trim permutations). `MaterialBaker` resolves a model texture in the items atlas first, then the blocks atlas (src: client `resources/model/sprite/MaterialBaker.java#bake`). Block models used in terrain must only use blocks-atlas sprites (`textures/block/...`); FRAPI's `SimpleUnbakedExtraModel` rejects block models with sprites from any other atlas. Put block textures under `textures/block/`, item textures under `textures/item/`.
- Animated textures: `x.png.mcmeta` `{"animation": {"frametime": 2, "interpolate": false, "frames": [...]}}`.

## 3. Item model definitions: `assets/<ns>/items/<item_id>.json`
Codec: client `renderer/item/ClientItem.java`, `ItemModels.java` (types), `properties/{conditional,numeric,select}/*Properties.java`, `color/item/ItemTintSources.java`, `special/SpecialModelRenderers.java`.
```json
{ "model": { "type": "minecraft:model", "model": "modid:item/ruby" },
  "hand_animation_on_swap": true, "oversized_in_gui": false, "swap_animation_scale": 1.0 }
```
Block items: `{"model": {"type": "minecraft:model", "model": "modid:block/ruby_block"}}`.

Model node types (`"type"`, namespace optional):
| type | fields |
|---|---|
| `model` | `model` (model id), `tints` (list, optional), `transformation` (optional) |
| `composite` | `models` (list of nodes drawn together) |
| `condition` | `property` (boolean property id + its fields), `on_true`, `on_false`, `transformation` |
| `select` | `property` (+ fields), `cases: [{"when": value-or-list, "model": node}]`, `fallback`, `transformation` |
| `range_dispatch` | `property` (+ fields), `scale` (default 1.0), `entries: [{"threshold": f, "model": node}]`, `fallback`, `transformation` |
| `special` | `base` (model id used for particle/transforms), `model: {"type": "<special renderer>", ...}`, `transformation` |
| `empty` | - |
| `bundle/selected_item` | - |

Properties:
- `condition` (boolean): `using_item`, `broken`, `damaged`, `fishing_rod/cast`, `has_component` (`component`, `ignore_default`), `bundle/has_selected_item`, `selected`, `carried`, `extended_view`, `keybind_down` (`keybind`), `view_entity`, `component` (component predicate), `custom_model_data` (`index`).
- `range_dispatch` (numeric): `bundle/fullness`, `damage`, `cooldown`, `time`, `compass`, `crossbow/pull`, `use_cycle`, `use_duration`, `count`, `custom_model_data`.
- `select`: `custom_model_data`, `main_hand`, `charge_type`, `trim_material`, `block_state` (`block_state_property`), `display_context`, `local_time`, `context_entity_type`, `context_dimension`, `component` (`component`).
- Tints (`tints` list on `model` nodes): `constant` (`value`), `dye` (`default`), `grass` (`temperature`, `downfall`), `firework`, `potion`, `team`, `custom_model_data` (`index`, `default`). Colors are ARGB ints (e.g. `-6265536`).
- Special renderer types: `bell, banner, book, conduit, chest, copper_golem_statue, head, player_head, shulker_box, shield, trident, decorated_pot, end_cube` (+ mod-registered).

Examples from vanilla (assets `items/bow.json`, `items/shield.json`, `items/leather_helmet.json`):
```json
{ "model": { "type": "minecraft:condition", "property": "minecraft:using_item",
    "on_false": { "type": "minecraft:model", "model": "minecraft:item/bow" },
    "on_true": { "type": "minecraft:range_dispatch", "property": "minecraft:use_duration", "scale": 0.05,
        "entries": [ { "threshold": 0.65, "model": { "type": "minecraft:model", "model": "minecraft:item/bow_pulling_1" } },
                     { "threshold": 0.9,  "model": { "type": "minecraft:model", "model": "minecraft:item/bow_pulling_2" } } ],
        "fallback": { "type": "minecraft:model", "model": "minecraft:item/bow_pulling_0" } } } }
```
```json
{ "model": { "type": "minecraft:special", "base": "minecraft:item/shield",
    "model": { "type": "minecraft:shield" } } }
```
Custom code-driven types (register in client init; all mappers are `public static final ExtraCodecs.LateBoundIdMapper`): `ItemModels.ID_MAPPER`, `SpecialModelRenderers.ID_MAPPER`, `ItemTintSources.ID_MAPPER`, `ConditionalItemModelProperties.ID_MAPPER`, `RangeSelectItemModelProperties.ID_MAPPER`, `SelectItemModelProperties.ID_MAPPER`. Special renderer walkthrough: `client-rendering.md` section 8.

Datagen of these files (`FabricModelProvider`, `BlockModelGenerators`, `ItemModelGenerators`, `ItemModelUtils.plainModel/conditional/...`) is covered in toolchain.md section 7 "Datagen"; fabric-docs reference `ExampleModModelProvider.java`.

## 4. Runtime access to baked models
- `Minecraft.getInstance().getModelManager()`; `getItemModel(Identifier)`, `getItemProperties(Identifier)`, `getBlockStateModelSet()`, `getBlockModelSet()`, `getFluidStateModelSet()` (src: client `resources/model/ModelManager.java`); per-state `BlockStateModel` with `collectParts(RandomSource, List<BlockStateModelPart>)`, `particleMaterial()`, `materialFlags()`.
- Extra standalone models (not referenced by any blockstate/item): register with the Model Loading API (`ExtraModelKey`, 5.1) and fetch with `((FabricModelManager) mc.getModelManager()).getModel(KEY)` (`ModelManager implements FabricModelManager` in 26.3, so a cast is not even needed).
- Render a block model from a BER/entity: `collector.submitBlockModel(poseStack, renderType, parts, tintLayers, light, overlay, outlineColor)` or the vanilla `BlockModelResolver` from the renderer context.

## 5. Fabric Model Loading API (fabric-model-loading-api-v1)
Package `net.fabricmc.fabric.api.client.model.loading.v1`. Note from the 26.1 blog: this module (and FRAPI) lagged at 26.1 launch; on 26.3 it is present and functional. (src: fapi:fabric-model-loading-api-v1 client + testmodClient `ModelTestModClient.java`)

### 5.1 Plugins
```java
public static final ExtraModelKey<BlockStateModel> HALF_RED_SAND = ExtraModelKey.create(() -> "modid:half_red_sand");

ModelLoadingPlugin.register(ctx -> {
    // extra model, baked alongside everything else
    ctx.addModel(HALF_RED_SAND, SimpleUnbakedExtraModel.blockStateModel(Identifier.fromNamespaceAndPath("modid", "half_red_sand")));

    // supply blockstate models in code instead of a blockstate JSON
    ctx.registerBlockStateResolver(ModBlocks.MY_CROP, rc -> {
        BlockStateModel.UnbakedRoot m = new SingleVariant.Unbaked(new Variant(Identifier.fromNamespaceAndPath("modid", "block/crop"))).asRoot();
        for (BlockState s : rc.block().getStateDefinition().getPossibleStates()) rc.setModel(s, m);
    });

    // modify vanilla / other mods
    ctx.modifyBlockModelOnLoad().register(ModelModifier.OVERRIDE_PHASE, (model, mc) ->
            mc.state().is(Blocks.OAK_FENCE) ? model : model);
    ctx.modifyBlockModelAfterBake().register(ModelModifier.WRAP_PHASE, (model, mc) ->
            mc.state() == Blocks.GOLD_BLOCK.defaultBlockState() ? new MyWrapper(model) : model);
    ctx.modifyItemModelOnLoad().register(ModelModifier.WRAP_PHASE, (clientItem, mc) ->
            mc.itemId().equals(ItemIds.BONE_MEAL.identifier())
                ? new ClientItem(ItemModelUtils.plainModel(Identifier.fromNamespaceAndPath("modid", "item/emerald")), clientItem.properties(), clientItem.registrySwapper())
                : clientItem);
});
```
- `Context`: `registerBlockStateResolver(Block, BlockStateResolver)`, `addModel(ExtraModelKey<T>, UnbakedExtraModel<T>)`, events `modifyModelOnLoad()` (`UnbakedModel`, ctx `id()`), `modifyBlockModelOnLoad()` / `modifyBlockModelBeforeBake()` (`BlockStateModel.UnbakedRoot`, ctx `state()`[, `baker()`]), `modifyBlockModelAfterBake()` (`BlockStateModel`, ctx `state()`, `sourceModel()`, `baker()`), `modifyItemModelOnLoad()` (`ClientItem`, ctx `itemId()`), `modifyItemModelBeforeBake()` (`ItemModel.Unbaked`, ctx `itemId()`, `bakingContext()`, `transformation()`), `modifyItemModelAfterBake()` (`ItemModel`, + `sourceModel()`).
- Phases: `ModelModifier.OVERRIDE_PHASE` -> `DEFAULT_PHASE` -> `WRAP_PHASE` -> `WRAP_LAST_PHASE`.
- `PreparableModelLoadingPlugin.register(DataLoader<T>, (data, ctx) -> ...)` to load data (e.g. JSON from resources) asynchronously before the plugin runs.
- `SimpleUnbakedExtraModel.blockStateModel(id[, ModelState])` or `new SimpleUnbakedExtraModel<>(id, (resolvedModel, baker) -> T)`; custom `UnbakedExtraModel<T>`: `resolveDependencies(Resolver)` + `T bake(ModelBaker)`.
- Wrappers: `WrapperBlockStateModel` (override `emitQuads`, `createGeometryKey`, ...), `WrapperUnbakedModel`, `WrapperUnbakedRootBlockStateModel`, `WrapperUnbakedItemModel`, `WrapperBakedItemModel`; `CompositeBlockStateModel`.
- Hooking a reload listener after models: `ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(id, listener)` + `addListenerOrdering(ResourceReloaderKeys.Client.MODELS, id)` (fabric-resource-loader-v1).

### 5.2 Custom JSON model formats (`UnbakedModel`)
`UnbakedModelDeserializer.register(Identifier.fromNamespaceAndPath("modid", "builtin_mesh"), new MyDeserializer())`; any model JSON with `"fabric:type": "modid:builtin_mesh"` is parsed by `UnbakedModel deserialize(JsonObject, JsonDeserializationContext)`. A common pattern (testmod `SimpleUnbakedModelDeserializer`) returns `new CuboidModel(UnbakedGeometry elements, GuiLight, Boolean ao, ItemTransforms, TextureSlots.Data, Identifier parent)` with custom `UnbakedGeometry` whose `QuadCollection bake(TextureSlots, ModelBaker, ModelState, ModelDebugName)` builds a FRAPI mesh (5.4).
```json
{ "fabric:type": "modid:builtin_mesh", "parent": "block/block",
  "textures": { "frame": "block/obsidian", "particle": "#frame" }, "mesh": "frame" }
```

### 5.3 Custom blockstate model types (`CustomUnbakedBlockStateModel`)
`CustomUnbakedBlockStateModel.register(Identifier.fromNamespaceAndPath("modid", "biome_dependent"), MyUnbaked.CODEC)`; the unbaked record implements `CustomUnbakedBlockStateModel` (`codec()`, `resolveDependencies(Resolver)`, `BlockStateModel bake(ModelBaker)`), and nested vanilla variants decode with `BlockStateModel.Unbaked.CODEC`. Used in blockstate JSON:
```json
{ "variants": { "": {
    "fabric:type": "modid:biome_dependent",
    "regular_model": { "model": "block/stone" },
    "biome_model":   { "model": "block/gold_block" },
    "biome_tag": "is_river" } } }
```
The baked `BlockStateModel` implements `emitQuads(QuadEmitter, BlockAndTintGetter, BlockPos, BlockState, RandomSource, Predicate<Direction> cullTest)` (FRAPI default method on vanilla interface), `createGeometryKey(...)` (return a value-equal key for caching identical geometry or null), `particleMaterial(level, pos, state)`, `materialFlags(...)`, plus vanilla `collectParts(RandomSource, List<BlockStateModelPart>)`, `particleMaterial()`, `materialFlags()`. Full example: fapi:fabric-renderer-api-v1 testmodClient `BiomeDependentBlockStateModel.java`, `FrameBlockStateModel.java`, `PillarBlockStateModel.java`.

### 5.4 Fabric Renderer API (FRAPI) meshes
Package `net.fabricmc.fabric.api.client.renderer.v1` (src: fapi:fabric-renderer-api-v1 client + testmodClient `PillarGeometry.java`).
```java
public record PillarGeometry() implements UnbakedGeometry {
    @Override public QuadCollection bake(TextureSlots textures, ModelBaker baker, ModelState state, ModelDebugName name) {
        MutableMesh mesh = Renderer.get().mutableMesh();
        QuadEmitter e = mesh.emitter();
        e.pushTransform(ModelStateHelper.asQuadTransform(state, baker.materials()));     // apply blockstate rotation
        Material.Baked mat = baker.materials().get(Objects.requireNonNull(textures.getMaterial("pillar")), name);
        for (Direction side : Direction.values()) {
            e.square(side, 0, 0, 1, 1, 0).materialBake(mat, MutableQuadView.BAKE_LOCK_UV).emit();
        }
        e.popTransform();
        return new MeshQuadCollection(mesh.immutableCopy());
    }
}
```
- `Renderer.get()`: `mutableMesh()`, `quadEmitter(Consumer<MutableQuadView>)`, `altModelBlockRenderer(ao, cull, blockColors)`. Indigo is the default implementation; Sodium registers its own (`Renderer.register`).
- `QuadEmitter`/`MutableQuadView`: `pos(i, x, y, z)`, `uv(i, u, v)`, `uvUnitSquare()`, `color(i, argb)`, `multiplyColor`, `normal(i, ...)`, `lightmap(i, packed)`, `minLightmap`, `square(face, left, bottom, right, top, depth)`, `nominalFace`, `cullFace`, `materialBake(Material.Baked, flags)` (sets sprite UVs, atlas, animated flag, chunk layer, item render types), `chunkLayer(ChunkSectionLayer)`, `emissive(bool)`, `shadeMode(ShadeMode.VANILLA|ENHANCED)`, `ambientOcclusion(TriState)`, `shadeDirectionOverride(Direction)`, `tintIndex(i)`, `foilType(...)`, `tag(int)`, `copyFrom`, `fromBakedQuad(BakedQuad)`, `emit()`, `pushTransform(QuadTransform)`/`popTransform()` (a `QuadTransform` returning false drops the quad). Bake flags: `BAKE_LOCK_UV`, `BAKE_ROTATE_90/180/270`, `BAKE_NORMALIZED`, `BAKE_FLIP_U/V`.
- `ModelHelper`, `ModelStateHelper`, `SpriteFinder` (`SpriteFinderGetter`), `FabricBlockModelRenderState`/`FabricLayerRenderState` for item/block render-state hooks.
- Compatibility: Sodium implements FRAPI, so FRAPI models render the same with and without Sodium; Iris handles FRAPI quads via Sodium.

## 6. Where to look
- client `resources/model/**` (loading/baking: `ModelBakery`, `ModelManager`, `ModelDiscovery`, `cuboid/*`, `sprite/{Material,SpriteId,TextureSlots,AtlasManager}`), `renderer/block/dispatch/**`, `renderer/item/**`, `renderer/special/**`.
- assets `blockstates/`, `models/block/`, `models/item/`, `items/`, `atlases/`, `equipment/`.
- fapi:fabric-model-loading-api-v1 and fapi:fabric-renderer-api-v1 testmodClient (Java + `resources/assets/*-testmod/{blockstates,models,items}`).
