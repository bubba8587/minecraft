# Client rendering on Minecraft 26.3 + Fabric API 0.166.2

Scope: GPU abstraction (OpenGL/Vulkan), extraction/submit model, RenderPipeline/RenderType/shaders, level render events, debug drawing, entity/block-entity renderers, model layers, special item renderers, particles, tints, chunk layers, fluids, Sodium/Iris. Verified 2026-11-14 against 26.3 decompiled source + Fabric API 26.3 branch; every snippet marked **(ran)** was compiled and executed in a headless client gametest (Vulkan/lavapipe backend) and visually confirmed in screenshots. See `client.md` for the index and GUI/HUD, `client-models.md` for model JSON/FRAPI, `client-gametest.md` for testing.

Source shorthands: `client` = `/tmp/claude-0/mcsrc/client/`, `fapi:<module>` = `/tmp/claude-0/mcsrc/fabric-api/<module>/src/...`, `assets` = `/tmp/claude-0/mcsrc/vanilla-jar/assets/minecraft/`.

---

## 1. Architecture: what changed since 1.21.5

| Era | Change |
|---|---|
| 1.21.5 | `RenderPipeline` (immutable GPU state + shaders + vertex format) replaces `ShaderProgram`/`RenderSystem` state toggling; `GpuDevice`, `RenderPass`, `CommandEncoder`, `GpuBuffer`, `GpuTexture` introduced (Blaze3D abstraction). Core shader `.json` files removed: shaders are referenced from pipelines in code. |
| 1.21.6-1.21.8 | GUI becomes render-state based (`GuiRenderState`, `GuiRenderer#prepare`), 2D `Matrix3x2fStack` for GUI, HUD layers -> Fabric `HudElementRegistry`. |
| 1.21.9-1.21.11 | Entities/block entities/particles extract into render states and **submit** to `SubmitNodeCollector` (feature renderers batch them). `RenderType` split into `RenderType` + `RenderSetup` in `net.minecraft.client.renderer.rendertype`; factories in `RenderTypes`. Gizmos (debug shapes). |
| 26.1 | Unobfuscated; GUI class renamed `GuiGraphicsExtractor`; auto chunk layers from sprite transparency (no `BlockRenderLayerMap`); `FluidModel`; `BlockTintSource`. |
| 26.2 | Backend split: package `com.mojang.renderpearl` with `backend/opengl` and an **optional Vulkan backend**; bind-group layouts on pipelines; raw GL is unsupported. Screen setters move to `Minecraft.gui`. |
| 26.3 | GLFW -> SDL3 windowing/input; `RenderPass#setPipeline` takes a `CompiledRenderPipeline` (`RenderSystem.getCompiledPipeline(pipeline)`); Order-Independent Transparency ("Improved Transparency" option) with `OitPipelineSet`; reversed-Z depth (clear 0.0, compare `GREATER_THAN_OR_EQUAL`). |

Key packages (26.3):
- `com.mojang.renderpearl.api.device` - `GpuDevice` (`RenderSystem.getDevice()`), `GpuBackend`, `DeviceFeatures/Limits`.
- `com.mojang.renderpearl.api.commands` - `CommandEncoder` (`device.createCommandEncoder()`), `RenderPass`, `RenderPassDescriptor`, `GpuFence`.
- `com.mojang.renderpearl.api.pipeline` - `RenderPipeline` (+ `.Builder`, `.Snippet`), `CompiledRenderPipeline`, `BindGroupLayout`, `ColorTargetState`, `BlendFunction`, `DepthStencilState`, `CompareOp`, `PrimitiveTopology`, `PolygonMode`, `ShaderType`.
- `com.mojang.renderpearl.api.buffers` (`GpuBuffer`, `GpuBufferSlice`), `.textures` (`GpuTexture`, `GpuTextureView`, `GpuSampler`), `.vertex` (`VertexFormat`, `VertexFormatElement`).
- `com.mojang.renderpearl.backend.opengl` / `.backend.vulkan` - implementations; never reference directly.
- `com.mojang.blaze3d.systems.RenderSystem` - static glue: `getDevice()`, `getCompiledPipeline(p)`, `getDynamicUniforms()`, `bindDefaultUniforms(pass)`, `getModelViewMatrixCopy()`, `getModelViewStack()`, `getProjectionType()`, `getSequentialBuffer(topology)`, `assertOnRenderThread()`.
- `com.mojang.blaze3d.vertex` - `PoseStack`, `VertexConsumer`, `BufferBuilder`, `DefaultVertexFormat`, `MeshData`.
- `net.minecraft.client.renderer` - `RenderPipelines`, `SubmitNodeCollector`, `StagedVertexBuffer`, `LevelRenderer`, `GameRenderer`, `extract/LevelExtractor`, `state/**` (render states), `feature/**` (feature renderers that consume submits).

Backend selection: `Options#preferredGraphicsBackend()` = `PreferredGraphicsApi.DEFAULT|OPENGL|VULKAN` (`options.txt` key `preferredGraphicsBackend:"vulkan"`). DEFAULT/OPENGL try OpenGL 3.3 core then Vulkan; VULKAN tries Vulkan then OpenGL (src: client `net/minecraft/client/PreferredGraphicsApi.java`). Log line: `Using graphics backend Vulkan, using drivers: ...`. Consequences for mods:
- No `GL11.*`/`GL30.*`/`GlStateManager` calls; no assumptions about GL texture ids. Everything goes through `GpuDevice`/`CommandEncoder`/`RenderPass`.
- Shaders are GLSL `#version 330` source compiled by the game for both backends (frontend `GlslCompiler` -> SPIR-V for Vulkan, src: client `com/mojang/renderpearl/frontend/shaders/`). Uniforms are UBO blocks (`layout(std140) uniform DynamicTransforms {...}`), samplers are named (`Sampler0`), vertex inputs/varyings need explicit `layout(location = N)`, and vanilla shaders use `#extension GL_ARB_separate_shader_objects : require`. Copy a vanilla shader as a template.
- Opening a `RenderPass` while another is open throws `IllegalStateException: Close the existing render pass before performing additional commands` (also triggered by buffer uploads, see section 5).

## 2. The two phases: extraction and submit/draw

Every frame: (1) **extraction** on the main thread copies game state into render states (`LevelRenderState`, `EntityRenderState`, `BlockEntityRenderState`, `GuiRenderState`, ...); (2) **submit** turns render states into `SubmitNode`s in a `SubmitNodeCollector`; (3) **draw**: feature renderers batch submits by `RenderType` and record GPU passes. Rules:
- Do world queries only in extract methods (`extractRenderState`, `LevelExtractionEvents`). Submit/draw code must use the render state only.
- Render states are reused between frames/entities: always overwrite every field you read in submit.
- Attach mod data to vanilla render states with `FabricRenderState` (every vanilla render state implements it via mixin): `RenderStateDataKey<T> KEY = RenderStateDataKey.create(() -> "modid:foo")`; `state.setData(KEY, value)` in extraction, `state.getData(KEY)` / `getDataOrDefault` when submitting. (src: fapi:fabric-rendering-v1 `FabricRenderState.java`, `RenderStateDataKey.java`)
- Render-state caches can be invalidated with `InvalidateRenderStateCallback.EVENT` (fires when Fabric invalidates cached extracted state, e.g. resource reload).

## 3. RenderPipeline, RenderType, shaders

### 3.1 Pick a vanilla RenderType first
`net.minecraft.client.renderer.rendertype.RenderTypes` factories (src: client `renderer/rendertype/RenderTypes.java`): `entitySolid(tex)`, `entityCutout(tex)`, `entityCutoutCull(tex)`, `entityCutoutZOffset(tex)`, `entityTranslucent(tex)`, `entityTranslucentCull(tex)`, `entityTranslucentEmissive(tex)`, `eyes(tex)`, `itemCutout/itemTranslucent(tex)`, `armorCutoutNoCull(tex)`, `entityShadow(tex)`, `beaconBeam(tex, translucent)`, `energySwirl(tex, u, v)`, `text(tex)`, `textSeeThrough(tex)`, `lines()`, `linesTranslucent()`, `linesDepthBias()`, `secondaryBlockOutline()`, `debugFilledBox()`, `debugQuads()`, `debugTriangleFan()`, `debugPoint()`, `leash()`, `lightning()`, `endPortal()`, `crumbling(tex)`, `outline(tex)`, `solidMovingBlock()/cutoutMovingBlock()/translucentMovingBlock()`. A model picks its default via `model.renderType(texture)` (EntityModel default = `RenderTypes::entityCutout`).
- `RenderTypes.lines()` uses vertex format `POSITION_COLOR_NORMAL_LINE_WIDTH`: every vertex needs `.setColor(argb).setNormal(pose, nx, ny, nz).setLineWidth(px)`. **(ran)**
- `RenderTypes.debugFilledBox()` = `POSITION_COLOR` QUADS, translucent, depth-tested. **(ran)**

### 3.2 Custom RenderType (vanilla pipeline + own texture/settings)
```java
RenderType MY_TYPE = RenderType.create("modid_glow", RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT)
        .withTexture("Sampler0", Identifier.fromNamespaceAndPath("modid", "textures/entity/glow.png"))
        .useLightmap().useOverlay()
        .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
        .sortOnUpload()
        .createRenderSetup());
```
`RenderSetup.RenderSetupBuilder` (src: client `renderer/rendertype/RenderSetup.java`): `withTexture(samplerName, id[, Supplier<GpuSampler>])`, `useLightmap()`, `useOverlay()`, `affectsCrumbling()`, `sortOnUpload()`, `setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING|...)`, `setTextureTransform(...)`, `setOutline(OutlineProperty.NONE|AFFECTS_OUTLINE|IS_OUTLINE)`, `setOitPipelines(OitPipelineSet)`, `withForcedSolidModelPhase()`. Sizes: `RenderType.BIG_BUFFER_SIZE`, `SMALL_BUFFER_SIZE`, `TRANSIENT_BUFFER_SIZE`.
- Gotcha (from source, not run): when the user enables Video Settings -> Improved Transparency (OIT), translucent (blending) custom geometry is routed through OIT and `PreparedRenderType#drawFromBufferOit` throws `"Render type X does not have OIT pipelines set up."` if the RenderType has no `setOitPipelines(...)`. Vanilla translucent types all set one (e.g. `.setOitPipelines(RenderPipelines.OIT_DEBUG_FILLED_BOX)`). For custom translucent types either reuse a vanilla `OitPipelineSet` matching your vertex format or build one with `OitPipelineSet.builder(suffix, RenderPipeline.builder(snippet))` and `RenderPipelines.register(oitSet)`; test with the option on. UNVERIFIED at runtime.

### 3.3 Custom RenderPipeline (+ own shader)  **(ran)**
```java
public static final RenderPipeline THROUGH_WALLS = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)          // inherits shaders, format, blend, bind groups
                .withLocation(Identifier.fromNamespaceAndPath("modid", "pipeline/through_walls"))
                .withVertexShader(Identifier.fromNamespaceAndPath("modid", "core/demo"))   // assets/modid/shaders/core/demo.vsh
                .withFragmentShader(Identifier.fromNamespaceAndPath("modid", "core/demo")) // assets/modid/shaders/core/demo.fsh
                .withDepthStencilState(Optional.empty())                      // no depth test -> visible through walls
                .build());
```
- `RenderPipelines.register(...)` / `registerOptional(...)` and all `*_SNIPPET` fields are private in vanilla and made **public by `fabric-transitive-access-wideners-v1`** (comments "Access widened by fabric-transitive-access-wideners-v1" in the decompiled source). They work only with Fabric API present.
- Register in a static field initialized during client init (before the first resource reload): `ShaderManager` compiles `RenderPipelines.requiredPipelines()` on reload; `RenderSystem.getCompiledPipeline(p)` throws `"Failed to find or load pipeline <loc>"` for pipelines never registered.
- Builder (src: client `com/mojang/renderpearl/api/pipeline/RenderPipeline.java`): `withLocation`, `withVertexShader/withFragmentShader(String|Identifier)`, `withShaderDefine(key[, int|float])`, `withBindGroupLayout(BindGroupLayouts.X)`, `withPolygonMode`, `withCull(bool)`, `withColorTargetState([index,] ColorTargetState)` (e.g. `new ColorTargetState(BlendFunction.TRANSLUCENT)`, `ColorTargetState.DEFAULT`), `withDepthStencilState(DepthStencilState | Optional)` (`new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, writeDepth)`, `DepthStencilState.DEFAULT`), `withVertexBinding(0, DefaultVertexFormat.X)`, `withPrimitiveTopology(PrimitiveTopology.QUADS|TRIANGLES|LINES|TRIANGLE_FAN|...)`, `withPushConstantSize`, `withSnippet`, `build()` / `buildSnippet()`.
- Useful vanilla snippets (`RenderPipelines.*_SNIPPET`): `GLOBALS`, `MATRICES_FOG`, `MATRICES_FOG_LIGHT_DIR`, `GENERIC_BLOCKS`, `BLOCK`, `TERRAIN`, `ENTITY`, `ENTITY_EMISSIVE`, `EYES`, `ITEM`, `TEXT`, `WORLD_TEXT`, `LINES`, `DEBUG_FILLED`, `PARTICLE`, `GUI`, `GUI_TEXTURED`, `GUI_TEXT`, `OUTLINE`, `CLOUDS`, `END_PORTAL`, `BEACON_BEAM`, plus `OIT_*` variants.
- Bind group layouts (`net.minecraft.client.renderer.BindGroupLayouts`): `GLOBALS`, `PROJECTION`, `DYNAMIC_TRANSFORMS`, `FOG`, `LIGHTING`, `SAMPLER0/1/2` and combos, `CHUNK_SECTION`, `TERRAIN_INFO`, `LIGHTMAP_INFO`, ... Uniform block names in GLSL must match (`Projection`, `DynamicTransforms`, `Fog`, `Globals`, `Lighting`).
- Shader files: `assets/<ns>/shaders/<path>.vsh` and `.fsh`; the id passed to `withVertexShader` is `<ns>:<path>` without extension. Includes: `#include <minecraft:dynamictransforms.glsl>` -> `assets/minecraft/shaders/include/dynamictransforms.glsl`; your own includes go in `assets/<ns>/shaders/include/*.glsl`. Post-effect chains: `assets/<ns>/post_effect/*.json`. There are no core-shader `.json` program files any more.
- Depth is reversed-Z: clear value 0.0, "closer" = greater. Vanilla depth compare is `GREATER_THAN_OR_EQUAL`; use the same when defining depth-tested pipelines.
- `FabricRenderPipeline` extension: `((FabricRenderPipeline.Builder) builder).withUsePipelineDrawModeForGui(true)` lets a GUI pipeline draw non-QUADS topologies (see fapi:fabric-rendering-v1 testmodClient `gui/GuiRendererNonQuadsTest.java`).

### 3.4 Drawing with a custom pipeline yourself  **(ran)**
Two choices:
1. **Preferred:** wrap the pipeline in a `RenderType` (3.2 with your pipeline) and submit geometry through `submitCustomGeometry` in a level render event (section 4). The feature system handles buffers, uniforms, sorting.
2. **Manual pass** (custom depth/target handling), adapted from fabric-docs `CustomRenderPipeline.java` and fixed for 26.3. It must run where **no render pass is open**: `LevelRenderEvents.AFTER_RENDER_PASS` (or `BEFORE_RENDER_PASS`). Running it in `AFTER_TRANSLUCENT_TERRAIN` (as fabric-docs does) crashes on 26.3 with `IllegalStateException: Close the existing render pass before performing additional commands` from `StagedVertexBuffer.upload()`.
```java
private static final StagedVertexBuffer STAGED = new StagedVertexBuffer(() -> "modid waypoint", RenderType.SMALL_BUFFER_SIZE);

LevelRenderEvents.AFTER_RENDER_PASS.register(context -> {
    Vec3 cam = context.levelState().cameraRenderState.pos;
    VertexFormat format = THROUGH_WALLS.getVertexFormatBinding(0);
    PrimitiveTopology topo = THROUGH_WALLS.getPrimitiveTopology();
    StagedVertexBuffer.Draw draw = STAGED.appendDraw(format, topo,
            topo == PrimitiveTopology.QUADS ? RenderSystem.getProjectionType().vertexSorting() : null);
    Matrix4f m = new Matrix4f().translate((float) -cam.x, (float) -cam.y, (float) -cam.z);
    VertexConsumer vc = STAGED.getVertexBuilder(draw);
    vc.addVertex(m, x, y, z).setColor(0x80FF00FF); // ... 4 vertices per quad
    STAGED.upload();
    StagedVertexBuffer.ExecuteInfo info = STAGED.getExecuteInfo(draw);
    if (info != null) {
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy());
        RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "modid through walls", target.getColorTextureView(), Optional.empty(),
                target.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(RenderSystem.getCompiledPipeline(THROUGH_WALLS));   // NOT pass.setPipeline(THROUGH_WALLS)
            RenderSystem.bindDefaultUniforms(pass);                              // Projection, Fog, Globals, Lighting
            pass.setUniform("DynamicTransforms", transforms);
            // textured: pass.setUniform("Sampler0", textureView, sampler);
            pass.setVertexBuffer(0, info.vertexBuffer().slice());
            pass.setIndexBuffer(info.indexBuffer(), info.indexType());
            pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }
    STAGED.endFrame();
});
// close STAGED when GameRenderer closes (fabric-docs uses a mixin on GameRenderer#close at RETURN).
```
`DynamicGpuData#writeTransform(Matrix4f modelView[, Vector4f colorModulator][, Vector3f modelOffset, Matrix4f textureMatrix])`. `RenderPass` methods: `setPipeline(CompiledRenderPipeline)`, `setUniform(name, GpuBuffer|GpuBufferSlice)`, `setUniform(name, GpuTextureView, GpuSampler)`, `setVertexBuffer`, `setIndexBuffer`, `drawIndexed`, `draw`, `enableScissor`, `pushDebugGroup/popDebugGroup`. (src: client `com/mojang/renderpearl/api/commands/RenderPass.java`, `CommandEncoder.java`)

## 4. Level (world) render events (fabric-rendering-v1 `level` package)

`WorldRenderEvents` was renamed to `LevelRenderEvents` in 26.1 and reorganized in 26.3 (BEFORE/AFTER_ENTITIES and DEBUG_RENDER are gone). Package `net.fabricmc.fabric.api.client.rendering.v1.level`. (src: fapi:fabric-rendering-v1 client `level/*.java`, testmodClient `LevelRenderEventsTests.java`)

Extraction events (`LevelExtractionEvents`, context `LevelExtractionContext`: `level()`, `camera()`, `deltaTracker()`, `gameRenderer()`, `levelRenderer()`, `levelState()`):
- `AFTER_BLOCK_OUTLINE_EXTRACTION` `(ctx, @Nullable HitResult)` - attach data to `ctx.levelState().blockOutlineRenderState` (setting it to null cancels vanilla outline).
- `END_EXTRACTION` `(ctx)` - extract any custom per-frame data (read the world here).

Drawing events, in frame order (`LevelRenderEvents`):
| Event | Context | Notes |
|---|---|---|
| `BEFORE_RENDER_PASS` | `LevelTerrainRenderContext` | No pass open: you may open/close your own `RenderPass`. |
| `START_MAIN` | terrain ctx | Inside main pass. |
| `AFTER_OPAQUE_TERRAIN` | terrain ctx | Inside main pass. |
| `COLLECT_SUBMITS` | `LevelRenderContext` | Add submits to `ctx.submitNodeCollector()`. |
| `AFTER_SOLID_FEATURES` | render ctx | Inside main pass. |
| `AFTER_TRANSLUCENT_FEATURES` | render ctx | |
| `BEFORE_BLOCK_OUTLINE` | `(ctx, BlockOutlineRenderState) -> boolean` | return false to cancel vanilla outline. |
| `BEFORE_GIZMOS` | render ctx | |
| `BEFORE_TRANSLUCENT_TERRAIN` | render ctx | `submitCustomGeometry` here works **(ran)**. |
| `AFTER_TRANSLUCENT_TERRAIN` | render ctx | Pass still open with classic transparency: do not open passes/upload buffers. |
| `AFTER_RENDER_PASS` | render ctx | Main pass closed: safe for your own `RenderPass` **(ran)**. |
| `END_MAIN` | render ctx | Before clouds/weather/late debug. |

`LevelRenderContext`: `submitNodeCollector()`, `poseStack()`, `sectionsToRender()`, plus `levelState()` (`LevelRenderState`; camera position `levelState().cameraRenderState.pos`), `gameRenderer()`, `levelRenderer()`.

World-space geometry via submits **(ran)**:
```java
LevelRenderEvents.BEFORE_TRANSLUCENT_TERRAIN.register(context -> {
    Vec3 cam = context.levelState().cameraRenderState.pos;
    PoseStack ps = context.poseStack();
    ps.pushPose();
    ps.translate(-cam.x, -cam.y, -cam.z);                     // world coords from here
    AABB box = new AABB(new BlockPos(0, 100, 0));
    int color = ARGB.colorFromFloat(0.5f, 0, 1, 0);
    context.submitNodeCollector().submitCustomGeometry(ps, RenderTypes.debugFilledBox(),
            (pose, buf) -> { buf.addVertex(pose, (float) box.minX, (float) box.minY, (float) box.minZ).setColor(color); /* ... 24 verts */ });
    context.submitNodeCollector().submitCustomGeometry(ps, RenderTypes.lines(), (pose, buf) -> {
        buf.addVertex(pose, 0f, 100f, 0f).setColor(0xFFFF0000).setNormal(pose, 0, 1, 0).setLineWidth(2f);
        buf.addVertex(pose, 0f, 110f, 0f).setColor(0xFFFF0000).setNormal(pose, 0, 1, 0).setLineWidth(2f);
    });
    ps.popPose();
});
```
Lower-level: `FeatureRendererRegistry.register(FeatureRendererType<T>, Supplier<FeatureRenderer<T>>)` + `((FabricOrderedSubmitNodeCollector) collector).submitCustom(SubmitRenderPhases.X, node)` for a fully custom batched feature (see testmodClient `FeatureRendererTest.java`). Phases: `SOLID, SHADOWS, NAME_TAGS, SEE_THROUGH_NAME_TAGS, TEXTS, SHAPE_OUTLINES, TRANSLUCENT_BLOCKS_AND_ITEMS, TRANSLUCENT_MODELS, TRANSLUCENT_CUSTOM_GEOMETRY, GIZMOS, BREAKING_OVERLAY, WATER_MASK, AFTER_TERRAIN, ALWAYS_ON_TOP, OUTLINE`.

## 5. Rules for manual GPU work
- Uploading/copying buffers or textures (`StagedVertexBuffer.upload`, `CommandEncoder.writeToBuffer`, `copyToBuffer`, `writeToTexture`) is illegal while any `RenderPass` is open.
- Always `try (RenderPass pass = ...)`; leaving a pass open breaks the frame.
- Use `RenderSystem.getDevice()` only after the client started (not in `onInitializeClient`).
- Check `RenderSystem.isOnRenderThread()`; GPU calls only on the render thread.
- Prefer submits over manual passes: they work with OIT, entity outlines, Iris and Sodium.

## 6. Debug drawing: Gizmos (simplest way to draw lines/boxes)  **(ran)**
`net.minecraft.gizmos.Gizmos` (common package, works client and integrated-server side) adds debug primitives into the current thread's `GizmoCollector`. The client installs a per-tick collector around `Minecraft#tick()`, so calls from `ClientTickEvents.END_CLIENT_TICK` work:
```java
ClientTickEvents.END_CLIENT_TICK.register(client -> {
    if (client.player == null) return;
    Gizmos.cuboid(new BlockPos(1, 101, 2), GizmoStyle.strokeAndFill(0xFFFFFF00, 3f, 0x4000FFFF));
    Gizmos.line(Vec3.ZERO, new Vec3(0, 120, 0), 0xFFFF0000, 2f);
    Gizmos.billboardTextOverBlock("hi", pos, 0, 0xFFFFFFFF, 1f);
});
```
API: `cuboid(AABB|BlockPos[, padding], GizmoStyle[, coloredCorner])`, `circle(Vec3, radius, style)`, `line(Vec3, Vec3, argb[, width])`, `arrow(...)`, `rect(...)`, `point(Vec3, argb, size)`, `billboardTextOverBlock/OverMob/billboardText(...)`; each returns `GizmoProperties` with `setAlwaysOnTop()`, `persistForMillis(int)`, `fadeOut()`. `GizmoStyle.stroke(argb[, width])`, `fill(argb)`, `strokeAndFill(stroke, width, fill)`. Calling outside a collector throws `"Gizmos cannot be created here! No GizmoCollector has been registered."`. (src: common `net/minecraft/gizmos/Gizmos.java`, client `Minecraft.java#collectPerTickGizmos`)

## 7. Entity rendering

### 7.1 Renderer + render state + model  **(ran)**
```java
public class DemoEntityRenderer extends EntityRenderer<DemoEntity, DemoEntityRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("modid", "demo"), "main");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("modid", "textures/entity/demo.png");
    private final Model model;

    public static class State extends EntityRenderState { public float spin; }

    public static class Model extends EntityModel<State> {
        private final ModelPart cube;
        public Model(ModelPart root) { super(root); cube = root.getChild("cube"); }
        @Override public void setupAnim(State s) { super.setupAnim(s); cube.yRot = s.spin; }   // radians
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("cube", CubeListBuilder.create().texOffs(0, 0).addBox(-8, -16, -8, 16, 16, 16),
                PartPose.offset(0, 24, 0));
        return LayerDefinition.create(mesh, 64, 64);           // texture size
    }

    public DemoEntityRenderer(EntityRendererProvider.Context ctx) { super(ctx); model = new Model(ctx.bakeLayer(LAYER)); }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(DemoEntity e, State s, float partialTicks) {
        super.extractRenderState(e, s, partialTicks);          // fills x/y/z, ageInTicks, lightCoords, outlineColor, nameTag...
        s.spin = s.ageInTicks * 0.1f;
    }
    @Override public void submit(State s, PoseStack ps, SubmitNodeCollector c, CameraRenderState cam) {
        ps.pushPose();
        ps.scale(-1, -1, 1); ps.translate(0, -1.501f, 0);     // standard model-space flip (as LivingEntityRenderer)
        c.submitModel(model, s, ps, model.renderType(TEXTURE), s.lightCoords, OverlayTexture.NO_OVERLAY, s.outlineColor);
        ps.popPose();
        super.submit(s, ps, c, cam);                          // name tag, leash, fire
    }
}
// ClientModInitializer:
ModelLayerRegistry.registerModelLayer(DemoEntityRenderer.LAYER, DemoEntityRenderer::createLayer);
EntityRenderers.register(ModEntities.DEMO, DemoEntityRenderer::new);
```
- `EntityRenderer<T extends Entity, S extends EntityRenderState>`: `createRenderState()`, `extractRenderState(T, S, float partialTicks)`, `submit(S, PoseStack, SubmitNodeCollector, CameraRenderState)`, `shouldRender(...)`, `getRenderOffset(S)`, `getShadowRadius(S)`, `getNameTag(T)`, `finalizeRenderState(T, S)`. (src: client `renderer/entity/EntityRenderer.java`)
- Living mobs: `extends MobRenderer<E, S extends LivingEntityRenderState, M extends EntityModel<? super S>>` with ctor `super(ctx, new MyModel(ctx.bakeLayer(LAYER)), shadowRadius)` and `getTextureLocation(S)`; `LivingEntityRenderer` handles the flip, body rotation, hurt overlay, layers (`addLayer(new MyLayer(this))`, `RenderLayer#submit(PoseStack, SubmitNodeCollector, int light, S, float yRot, float xRot)`). `AgeableMobRenderer` for baby models.
- `LivingEntityRenderState` fields commonly used: `walkAnimationPos`, `walkAnimationSpeed`, `yRot` (head yaw, degrees), `xRot` (pitch, degrees), `bodyRot`, `ageInTicks`, `isBaby`, `hasRedOverlay`. Convert degrees with `Mth.DEG_TO_RAD` (the fabric-docs MiniGolem uses `RAD_TO_DEG`, which is wrong).
- Keyframe animations: `AnimationDefinition` + `KeyframeAnimation anim = DEF.bake(root)`; in `setupAnim`: `anim.apply(state.someAnimationState, state.ageInTicks)`; copy `AnimationState` in extract with `state.animState.copyFrom(entity.animState)`.
- `EntityRenderers.register(EntityType<? extends T>, EntityRendererProvider<T>)` is vanilla (public through Fabric's transitive AW). `EntityRendererRegistry` (Fabric) is `@Deprecated` and forwards.
- Entities that must reach the client must not be `Marker`-like: vanilla `Marker` throws `"Markers should never be sent"` when tracked.
- `EntityRendererProvider.Context`: `bakeLayer(loc)`, `getModelSet()`, `getItemModelResolver()`, `getBlockModelResolver()`, `getEntityRenderDispatcher()`, `getEquipmentRenderer()`, `getEquipmentAssets()`, `getAtlas(id)`, `getSprites()`, `getFont()`, `getResourceManager()`, `getPlayerSkinRenderCache()`, `getMapRenderer()`.
- Add a layer to vanilla/other renderers: `LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, ctx) -> { if (renderer instanceof AvatarRenderer r) helper.register(new MyLayer(r)); })` (was `LivingEntityFeatureRendererRegistrationCallback`). Players render via `AvatarRenderer`.
- Armor for custom armor items: `ArmorRenderer.register((poseStack, collector, stack, humanoidState, slot, light, contextModel) -> ..., items)`; armor model layers: `ModelLayerRegistry.registerArmorModelLayers(ArmorModelSet<ModelLayerLocation>, provider)`. Vanilla equipment textures/models: `assets/<ns>/equipment/<id>.json` (`EquipmentClientInfo`) referenced from the `equippable` component's `asset_id`.
- Cape toggle: `LivingEntityFeatureRenderEvents.ALLOW_CAPE_RENDER` `(AvatarRenderState) -> boolean`.

### 7.2 Block entity renderers
(src: client `renderer/blockentity/BlockEntityRenderer.java`, `CampfireRenderer.java`; compiled, not registered in the run)
```java
public class PedestalRenderer implements BlockEntityRenderer<PedestalBlockEntity, PedestalRenderer.State> {
    public static class State extends BlockEntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public String label = "";
    }
    private final Font font; private final ItemModelResolver items;
    public PedestalRenderer(BlockEntityRendererProvider.Context ctx) { font = ctx.font(); items = ctx.itemModelResolver(); }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(PedestalBlockEntity be, State s, float partialTicks, Vec3 cameraPos,
                                             ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, s, partialTicks, cameraPos, breakProgress); // blockPos, lightCoords, breakProgress
        items.updateForTopItem(s.item, be.getItem(), ItemDisplayContext.FIXED, be.getLevel(), null, (int) be.getBlockPos().asLong());
        s.label = be.getLabel();
    }
    @Override public void submit(State s, PoseStack ps, SubmitNodeCollector c, CameraRenderState cam) {
        ps.pushPose();
        ps.translate(0.5f, 1.2f, 0.5f);
        ps.rotate(Axis.YP.rotationDegrees(45));                // NOT mulPose(quaternion)
        s.item.submit(ps, c, s.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        ps.rotateDegrees(Axis.XP, 180); ps.scale(1 / 48f, 1 / 48f, 1 / 48f);
        c.submitText(ps, -font.width(s.label) / 2f, -20, Component.literal(s.label).getVisualOrderText(), false,
                Font.DisplayMode.NORMAL, s.lightCoords, 0xFFFFFFFF, 0, 0);
        ps.popPose();
    }
}
// ClientModInitializer:
BlockEntityRenderers.register(ModBlockEntities.PEDESTAL, PedestalRenderer::new);
```
- Interface methods: `createRenderState()`, `extractRenderState(T, S, float, Vec3 cameraPos, CrumblingOverlay)`, `submit(S, PoseStack, SubmitNodeCollector, CameraRenderState)`, `shouldRenderOffScreen()`, `getViewDistance()` (default 64), `shouldRender(T, Vec3)`.
- `BlockEntityRendererProvider.Context` record: `blockEntityRenderDispatcher()`, `blockModelResolver()`, `itemModelResolver()`, `entityRenderer()`, `entityModelSet()`, `font()`, `sprites()`, `playerSkinRenderCache()`, `bakeLayer(loc)`.
- `BlockEntityRendererRegistry` (Fabric) is `@Deprecated`; `BlockEntityRenderers.register` is public via transitive AW.
- `submitText(PoseStack, x, y, FormattedCharSequence, dropShadow, Font.DisplayMode NORMAL|SEE_THROUGH|POLYGON_OFFSET, light, argb, backgroundArgb, outlineArgb)`; `submitModel(model, state, ps, renderType, light, overlay, outlineColor)` or the 9-arg form `(..., tint, @Nullable UvMapping, outline)` or `(model, state, ps, light, overlay, tint, SpriteId, SpriteGetter, outline)`; `submitModelPart(...)`, `submitBlockModel(...)`, `submitMovingBlock(...)`, `submitShapeOutline(ps, VoxelShape, renderType, argb, width, afterTerrain)`, `submitItem(...)`, `submitNameTag(...)`, `submitShadow(...)`, `submitCustomGeometry(ps, renderType, (pose, vc) -> ...)`. `collector.order(int)` returns an ordered sub-collector for layering within a phase. (src: client `renderer/OrderedSubmitNodeCollector.java`)
- Sodium: block entities are collected per chunk section; to skip rendering conditionally use Sodium's `BlockEntityRenderHandler.instance().addRenderPredicate(type, predicate)` (section 13).

### 7.3 Model layers
`ModelLayerRegistry.registerModelLayer(ModelLayerLocation, TexturedLayerDefinitionProvider)` (`LayerDefinition createLayerDefinition()`); was `EntityModelLayerRegistry` (src: fapi:fabric-rendering-v1 `ModelLayerRegistry.java`). Bake with `ctx.bakeLayer(loc)` / `Minecraft.getInstance().getEntityModels().bakeLayer(loc)`. Duplicate registration throws.

## 8. Item rendering outside GUI, special item renderers
- Render an item stack in world/BER: `ItemModelResolver#updateForTopItem(ItemStackRenderState, ItemStack, ItemDisplayContext, @Nullable Level, @Nullable ItemOwner, int seed)` during extraction, then `state.submit(poseStack, collector, light, overlay, outlineColor)`; also `updateForLiving`, `updateForNonLiving`.
- Special (code-driven) item renderers referenced by `{"type": "minecraft:special", "base": "<model for particles/transforms>", "model": {"type": "modid:foo", ...}}` in the item model definition: implement `SpecialModelRenderer<T>` (`submit(@Nullable T arg, PoseStack, SubmitNodeCollector, int light, int overlay, boolean hasFoil, int outlineColor)`, `getExtents(Consumer<Vector3fc>)`, `@Nullable T extractArgument(ItemStack)`) plus a `SpecialModelRenderer.Unbaked<T>` record with a `MapCodec` (`bake(BakingContext ctx)` with `ctx.entityModelSet()`, `ctx.sprites()`, `ctx.playerSkinRenderCache()`; `type()` returns the codec). Register the codec: `SpecialModelRenderers.ID_MAPPER.put(Identifier.fromNamespaceAndPath("modid", "foo"), MyUnbaked.MAP_CODEC);` in client init. Vanilla types: `bell, banner, book, conduit, chest, copper_golem_statue, head, player_head, shulker_box, shield, trident, decorated_pot, end_cube`. (src: client `renderer/special/*`; fabric-docs `item/shield/GuiditeShieldSpecialRenderer.java` - its 10-arg `submitModel(..., null)` call does not match 26.3; use the 9-arg sprite overload.)
- Built-in block models (non-JSON block rendering, e.g. heads, banners; used for blocks rendered as moving blocks, in minecarts, item frames): `BuiltInBlockModelsCallback.EVENT.register(builder -> ...)` with `BuiltInBlockModels.special(unbakedSpecial[, transformation])`, `createAir(builder, block)` (src: fapi:fabric-rendering-v1 `BuiltInBlockModelsCallback.java`, client `renderer/block/BuiltInBlockModels.java`).

## 9. Chunk (terrain) render layers: solid / cutout / translucent
- 26.1+: the `ChunkSectionLayer` (`SOLID`, `CUTOUT`, `TRANSLUCENT`) is chosen **per baked quad** from the pixels of the sprite region the quad uses: any partially-alpha pixel -> TRANSLUCENT; only fully transparent + opaque pixels -> CUTOUT; opaque -> SOLID (src: client `renderer/chunk/ChunkSectionLayer.java#byTransparency`, `resources/model/cuboid/FaceBakery.java#computeMaterialTransparency`, `com/mojang/blaze3d/platform/Transparency.java`). There is no `BlockRenderLayerMap`/`ChunkSectionLayerMap` in Fabric API 26.3.
- Force translucency in the model's texture map (vanilla stained glass does this):
```json
"textures": {
  "pane": { "sprite": "minecraft:block/light_blue_stained_glass", "force_translucent": true }
}
```
  (`Material` codec: either a plain id string or `{"sprite": id, "force_translucent": bool}`; src: client `resources/model/sprite/Material.java`, assets `models/block/*stained_glass*.json`.) There is no "force cutout": make the texture fully opaque/fully transparent.
- In code (FRAPI): `MutableQuadView#chunkLayer(ChunkSectionLayer)`; `materialBake(material, flags)` computes it automatically.
- Leaves: `Options#cutoutLeaves()` makes leaves cutout or opaque; FRAPI `FabricBlockStateModel#emitQuads` forces `SOLID` when `ModelBlockRenderer.forceOpaque(...)` says so.
- Item rendering picks `Sheets.cutout*/translucent*ItemSheet` render types the same way.
- Block also needs `BlockBehaviour.Properties#noOcclusion()` (common side) for see-through blocks, otherwise neighbor faces get culled.

## 10. Block and item tints
- Blocks: `BlockColorRegistry.register(List<BlockTintSource> layers, Block... blocks)` (list index = model `tintindex`), or `BlockColorRegistry.register(BlockTintsFactory factory, Block...)` for fully dynamic per-position lists. Was `ColorProviderRegistry.BLOCK`. (src: fapi:fabric-rendering-v1 `BlockColorRegistry.java`, testmodClient `CustomColorResolverTest.java`)
```java
BlockColorRegistry.register(List.of(new BlockTintSource() {
    @Override public int color(BlockState state) { return 0xFF3366FF; }                       // item/particle/no-level
    @Override public int colorInWorld(BlockState s, BlockAndTintGetter level, BlockPos pos) {
        return level.getBlockTint(pos, BiomeColors.GRASS_COLOR_RESOLVER); }
    @Override public Set<Property<?>> relevantProperties() { return Set.of(); }              // properties the tint depends on
}), ModBlocks.MY_BLOCK);
```
  Vanilla helpers: `BlockTintSources.grass()`, `grassBlock()`, `foliage()`, `dryFoliage()`, `water()`, `doubleTallGrass()`, `constant(argb)` (src: client `color/block/BlockColors.java`). Custom biome-blended resolver: `ColorResolverRegistry.register(ColorResolver)` then `level.getBlockTint(pos, resolver)`. Terrain-particle tint opt-out: `ParticleRenderEvents.ALLOW_TERRAIN_PARTICLE_TINT`.
- Items: no Java registry. Tints are declared in the item model definition, `"tints": [ {"type": "minecraft:dye", "default": -6265536}, ... ]`; types: `constant` (`value`), `dye`, `grass` (`temperature`, `downfall`), `firework`, `potion`, `team`, `custom_model_data` (`index`, `default`) (src: client `color/item/ItemTintSources.java`). Custom item tint types: implement `ItemTintSource` (+ `MapCodec`) and register `ItemTintSources.ID_MAPPER.put(id, MAP_CODEC)` in client init (`ExtraCodecs.LateBoundIdMapper`, public `put`). Same pattern for custom item-model properties: `ConditionalItemModelProperties.ID_MAPPER`, `RangeSelectItemModelProperties.ID_MAPPER`, `SelectItemModelProperties.ID_MAPPER` and item model types `ItemModels.ID_MAPPER` (all `public static final`).

## 11. Particles (fabric-particles-v1, client)
Common: `SimpleParticleType T = FabricParticleTypes.simple();` + `Registry.register(BuiltInRegistries.PARTICLE_TYPE, id, T)`. Resource: `assets/<ns>/particles/<path>.json` = `{"textures": ["<ns>:<sprite>", ...]}` with PNGs in `assets/<ns>/textures/particle/` (atlas `particles.json` = directory source `particle`).
Client (renamed `ParticleFactoryRegistry` -> `ParticleProviderRegistry`, `FabricSpriteProvider` -> `FabricSpriteSet`):
```java
ParticleProviderRegistry.getInstance().register(ModParticles.SPARKLE, EndRodParticle.Provider::new);   // (ran) reuse vanilla behaviour
ParticleProviderRegistry.getInstance().register(ModParticles.GLOW, sprites ->
        (options, level, x, y, z, xa, ya, za, random) -> new GlowParticle(level, x, y, z, sprites.get(random)));

class GlowParticle extends SingleQuadParticle {
    GlowParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite); this.lifetime = 20; }
    @Override protected SingleQuadParticle.Layer getLayer() { return SingleQuadParticle.Layer.TRANSLUCENT; }
}
```
- `ParticleProvider<T>`: `@Nullable Particle createParticle(T options, ClientLevel, x, y, z, xAux, yAux, zAux, RandomSource random)` (26.x added the trailing `RandomSource`). `SpriteSet`: `get(RandomSource)`, `get(index, max)`, `first()`. `SingleQuadParticle.Layer`: `OPAQUE`, `TRANSLUCENT`, `OPAQUE_TERRAIN`, `TRANSLUCENT_TERRAIN`, `OPAQUE_ITEMS`, `TRANSLUCENT_ITEMS`, or `new Layer(translucent, atlasId, pipeline[, oitSet])`. Animate with `setSpriteFromAge(spriteSet)` in `tick()` (`SingleQuadParticle#setSpriteFromAge`; see `SimpleAnimatedParticle`).
- Custom particle groups/render types: `ParticleGroupRegistry.register(ParticleRenderType, engine -> group)`, `registerOrdering(...)` (was `ParticleRendererRegistry`).
- Spawn client-side: `level.addParticle(options, x, y, z, xd, yd, zd)`; server-side `ServerLevel#sendParticles(...)`.

## 12. Fluid rendering (fabric-rendering-fluids-v1)
26.1 introduced vanilla `FluidModel`; most fluids need only a model:
```java
FluidRenderingRegistry.register(ModFluids.STILL, ModFluids.FLOWING, new FluidModel.Unbaked(
        new Material(Identifier.fromNamespaceAndPath("modid", "block/acid_still")),
        new Material(Identifier.fromNamespaceAndPath("modid", "block/acid_flow")),
        null,                                   // @Nullable overlay material (e.g. behind glass)
        _ -> 0xFF55FF55));                      // @Nullable BlockTintSource (functional: color(BlockState))
```
- Overloads: `register(fluid, model[, FluidRenderHandler])`, `register(still, flow, model[, handler])`. `FluidRenderHandler#renderFluid(FluidRenderer, BlockPos, BlockAndTintGetter, FluidRenderer.Output, BlockState, FluidState)` only for custom geometry (default delegates to vanilla). `setBlockTransparency(block, bool)` controls overlay texture use next to a block. Layer comes from sprite transparency or `Material` `forceTranslucent`. (src: fapi:fabric-rendering-fluids-v1, testmodClient `FabricFluidRenderingTestModClient.java`, client `renderer/block/FluidModel.java`)
- Sodium depends on `fabric-rendering-fluids-v1` and honors registered handlers.

## 12b. Texture atlases and sprite sources
- Vanilla atlases are defined by `assets/minecraft/atlases/<name>.json` (`blocks`, `items`, `gui`, `particles`, `paintings`, `map_decorations`, `banner_patterns`, `shield_patterns`, `chests`, `shulker_boxes`, `decorated_pot`, `celestials`); mods add sprites by shipping a same-named file in their own namespace with extra `sources` (e.g. `{"sources": [{"type": "minecraft:directory", "source": "entity/mymob", "prefix": "entity/mymob/"}]}`), or simply by placing PNGs in directories already covered (`textures/block/`, `textures/item/`, `textures/gui/sprites/`, `textures/particle/`). Source types include `directory`, `single`, `filter`, `unstitch`, `paletted_permutations`.
- Custom atlas: `AtlasRegistry.register(new AtlasManager.AtlasConfig(textureId, definitionId, createMipmaps[, Set<MetadataSectionType<?>> extraMetadata]))` with `textureId = AtlasRegistry.generateTextureLocation(definitionId)`; definition JSON at `assets/<ns>/atlases/<path>.json`; fetch sprites with `Minecraft.getInstance().getAtlasManager().get(new SpriteId(textureId, spriteId))`; custom `.mcmeta` sections via `sprite.contents().getAdditionalMetadata(type)`. Custom sprite source types: `SpriteSourceRegistry.register(id, MapCodec<? extends SpriteSource>)` (was `AtlasSourceRegistry`). (src: fapi:fabric-rendering-v1 `AtlasRegistry.java`, testmodClient `AtlasTests.java`, `CustomSpriteSourcesTest.java`; client `resources/model/sprite/AtlasManager.java`)
- `SpriteId` (the atlas+sprite pair that 1.21.x Mojang mappings called `Material`; `Material` is now sprite + `forceTranslucent`) = `(Identifier atlasTexture, Identifier sprite)`; `SpriteGetter` resolves them; `Sheets.*_MAPPER` helpers build chest/shield/sign sprite ids.

## 13. Sodium and Iris compatibility (26.3)
Current builds (Modrinth API, 2026-11-14): Sodium `mc26.3-0.9.3-beta.5-fabric` (2026-11-12), Iris `1.11.7+26.3-fabric` (2026-09-30). Sodium's dev-branch `fabric.mod.json` (2026-11-13) declares `breaks: {"iris": "<=1.12.0", "fabric-api": "<0.145.1", ...}`, i.e. the next Sodium will need a newer Iris; always check pairings on Modrinth.

Sodium (`net.caffeinemc.mods.sodium.api.*`, Maven `https://maven.caffeinemc.net/releases`):
- Sodium **implements FRAPI** (`"fabric-renderer-api-v1:contains_renderer": true`, provides `indium`), so models built with Fabric Renderer API / Model Loading API render correctly; Indigo disables itself. Vanilla `BlockStateModel`s and JSON models need nothing special.
- It replaces chunk meshing: mixins into `SectionCompiler`, `ModelBlockRenderer`, `LevelRenderer` terrain paths will not run. Use FRAPI/model-loading hooks or `LevelRenderEvents` instead.
- It replaces the Video Settings screen: add options with Sodium's config API (`ConfigEntryPoint`, see `USAGE.md` in its repo), not by mixing into `VideoSettingsScreen`.
- Animated sprites used in custom rendering that bypasses vanilla helpers: call `SpriteUtil.INSTANCE.markSpriteActive(sprite)` each frame, otherwise the animation may freeze.
- Block entity culling hook: `BlockEntityRenderHandler.instance().addRenderPredicate(type, predicate)`.
- Do not assume `BufferBuilder`/vertex layout internals of terrain; Sodium uses its own vertex formats (`VertexFormatRegistry`, `VertexBufferWriter` for fast writes).
Iris (`net.irisshaders.iris.api.v0.IrisApi.getInstance()`):
- Custom shaders/pipelines are **ignored or broken when a shader pack is active** (Iris docs `docs/development/compatibility/core-shaders.md`): prefer vanilla `RenderTypes`/pipelines; provide fallbacks.
- `isShaderPackInUse()`, `isRenderingShadowPass()`, `assignPipeline(RenderPipeline, IrisProgram)` (API v0.3) to tell Iris which gbuffers program should replace your custom pipeline (`IrisProgram.BASIC, TEXTURED, TERRAIN_*, TRANSLUCENT, ENTITIES, ENTITIES_TRANSLUCENT, BLOCK, PARTICLES, LINES, ...`), `assignPipelineShadow(...)`, `registerShadowRenderCallback(...)` (API v0.4), `isReverseZDuringShaders()`. Compile against Iris with `compileOnly "maven.modrinth:iris:<version>"` and guard calls with `FabricLoader.getInstance().isModLoaded("iris")`. (src: Iris branch `26.3`, `common/src/api/java/net/irisshaders/iris/api/v0/IrisApi.java`)
General compatibility checklist:
1. Submit geometry through `SubmitNodeCollector`/`RenderTypes`; avoid manual `RenderPass`es unless needed.
2. Never call GL directly (Vulkan backend exists; also Iris/Sodium state).
3. Put world-query code in extraction; render from render state (Sodium/Iris may extract on different schedules, shadow pass renders twice).
4. Use Fabric events (`LevelRenderEvents`, `HudElementRegistry`, `ScreenEvents`) rather than mixins into `LevelRenderer`/`GameRenderer`/`Gui`.
5. Use FRAPI or vanilla JSON for block models; chunk-meshing mixins break under Sodium.

## 14. Where to look
- client `com/mojang/renderpearl/**` (device, passes, pipelines, GL and Vulkan backends), `com/mojang/blaze3d/systems/RenderSystem.java`, `com/mojang/blaze3d/vertex/*`.
- client `net/minecraft/client/renderer/{RenderPipelines,BindGroupLayouts,LevelRenderer,GameRenderer,StagedVertexBuffer,SubmitNodeCollector,OrderedSubmitNodeCollector,SubmitNodeCollection}.java`, `rendertype/*`, `feature/*`, `extract/LevelExtractor.java`, `state/**`, `entity/**`, `blockentity/**`, `special/*`, `item/*`, `oit/*`.
- assets `shaders/core/*.vsh|fsh`, `shaders/include/*.glsl`, `post_effect/*.json`.
- fapi:fabric-rendering-v1 testmodClient (`LevelRenderEventsTests`, `FeatureRendererTest`, `ArmorRenderingTests`, `gui/*`, `SpecialBlockRendererTest`, `AtlasTests`, `CustomSpriteSourcesTest`), fapi:fabric-particles-v1 testmodClient, fapi:fabric-rendering-fluids-v1 testmodClient.
- fabric-docs `develop/rendering/world.md`, `develop/blocks/block-entity-renderer.md`, `develop/entities/*` (26.2-era; see compile gotchas above).
