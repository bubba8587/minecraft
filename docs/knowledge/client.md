# Client-side Fabric modding on Minecraft 26.3 (index + core client APIs)

Scope: everything that runs only on the physical client (entrypoint, events, input, commands, HUD, GUI, sounds, toasts, chat), plus links to the rendering, model and client-gametest files. Verified 2026-11-14 against Minecraft 26.3 decompiled sources (`/tmp/claude-0/mcsrc/client`, `.../common`), Fabric API 0.166.2+26.3 sources (branch 26.3) and a compiled + headlessly executed scratch mod (Loom 1.19.4, Loader 0.20.1, Java 25).

Split files:
- `client.md` (this file): architecture overview, entrypoint, lifecycle/tick events, key mappings, client commands, HUD, GUI/screens/widgets/menus, sounds, toasts, chat, options.
- `client-rendering.md`: renderpearl/Blaze3D (OpenGL + Vulkan), extraction vs. submit, RenderPipeline/RenderType/shaders, level render events, gizmos/debug drawing, entity + block entity renderers, model layers, special item renderers, particles, block/item tints, chunk layers (cutout/translucent), fluids, Sodium/Iris compatibility.
- `client-models.md`: blockstates, block model JSON, item model definitions (`assets/<ns>/items/*.json`), Fabric Model Loading API, Fabric Renderer API (FRAPI / Indigo).
- `client-gametest.md`: `fabric-client-gametest-api-v1`, writing and running client gametests headless (verified working recipe).

Source shorthands: `client` = `/tmp/claude-0/mcsrc/client/`, `common` = `/tmp/claude-0/mcsrc/common/`, `fapi:<module>` = `/tmp/claude-0/mcsrc/fabric-api/<module>/src/...`, `assets` = `/tmp/claude-0/mcsrc/vanilla-jar/assets/minecraft/`.

---

## 0. The ten things that will bite you (1.21.x -> 26.3)

1. **Mojang names everywhere; Yarn is dead.** `DrawContext` -> `GuiGraphicsExtractor`, `MatrixStack` -> `PoseStack`, `RenderLayer` -> `RenderType` (now in `net.minecraft.client.renderer.rendertype`), `KeyBinding` -> `KeyMapping`, `Identifier` *is* the Mojang name now (`net.minecraft.resources.Identifier`, created with `Identifier.fromNamespaceAndPath(ns, path)` / `Identifier.withDefaultNamespace(path)` / `Identifier.parse("ns:path")`).
2. **GUI drawing is "extraction", and most methods were renamed.** `GuiGraphics` no longer exists: the class is `net.minecraft.client.gui.GuiGraphicsExtractor`. `drawString` -> `text`, `drawCenteredString` -> `centeredText`, `renderItem` -> `item`, `renderItemDecorations` -> `itemDecorations`, `renderTooltip` -> `setTooltipForNextFrame`. `Screen#render` -> `Screen#extractRenderState`, `renderBackground` -> `extractBackground`, `AbstractWidget#renderWidget` -> `extractWidgetRenderState`. (src: client `net/minecraft/client/gui/GuiGraphicsExtractor.java`, `gui/screens/Screen.java`)
3. **Screen/HUD moved off `Minecraft`** (26.2): `Minecraft.getInstance().setScreen(s)` -> `Minecraft.getInstance().gui.setScreen(s)`; current screen `mc.gui.screen()`; toasts `mc.gui.toastManager()`; HUD object `mc.gui.hud` (class `net.minecraft.client.gui.Hud`), chat `mc.gui.hud.getChat()`. (src: client `gui/Gui.java`, `gui/Hud.java`)
4. **GLFW is gone, SDL3 replaced it (26.3).** Key codes in `InputConstants.KEY_*` are now **SDL scancodes** (e.g. `KEY_A = 4`, `KEY_0 = 39`), mouse buttons are `MOUSE_BUTTON_LEFT = 1`, `MIDDLE = 2`, `RIGHT = 3`. Never hardcode GLFW numbers; use `InputConstants` or `org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_*`. (src: client `com/mojang/blaze3d/platform/InputConstants.java`)
5. **Rendering backend is abstracted ("renderpearl") with OpenGL and Vulkan backends.** No raw GL calls. GPU API lives in `com.mojang.renderpearl.api.*` (`GpuDevice`, `RenderPass`, `RenderPipeline`, `GpuTexture`...). Default order: try OpenGL, fall back to Vulkan (or the reverse if the user picks Vulkan in Video Settings -> Graphics API). See `client-rendering.md`.
6. **Render-state extraction everywhere.** Entity, block entity, HUD, GUI, level rendering: first copy data into a `*RenderState` ("extract"), then "submit" to a `SubmitNodeCollector`. Never read the `Level`/entity from the submit/draw phase.
7. **Renderer registries are vanilla now.** `EntityRendererRegistry` / `BlockEntityRendererRegistry` (Fabric) are `@Deprecated`; call vanilla `EntityRenderers.register(...)` / `BlockEntityRenderers.register(...)` (made public by Fabric's transitive access wideners). `EntityModelLayerRegistry` -> `ModelLayerRegistry`, `ColorProviderRegistry` -> `BlockColorRegistry`, `HudRenderCallback` removed -> `HudElementRegistry`, `WorldRenderEvents` -> `LevelRenderEvents` + `LevelExtractionEvents`.
8. **`BlockRenderLayerMap` is gone.** Chunk layer (solid/cutout/translucent) is computed per quad from the sprite's pixels; force translucency in the model JSON with `{"sprite": "...", "force_translucent": true}`. See `client-rendering.md` section 9.
9. **`PoseStack#mulPose(Quaternionf)` no longer exists** (only `mulPose(Matrix4fc)` / `mulPose(Transformation)`). Use `poseStack.rotate(Axis.YP.rotationDegrees(45))` or `poseStack.rotateDegrees(Axis.YP, 45)`. The fabric-docs BER example that calls `mulPose(Axis.XP.rotationDegrees(90))` does not compile on 26.3. (src: client `com/mojang/blaze3d/vertex/PoseStack.java`)
10. **Text colors are ARGB; alpha 0 = invisible.** `graphics.text(font, "x", x, y, 0xFFFFFF)` draws nothing (it checks `ARGB.alpha(color) != 0`). Use `0xFFFFFFFF` / `CommonColors.WHITE`. `text(font, str, x, y, color)` defaults to **drop shadow = true**.

Also: Java 25 unnamed lambda params (`_`) are used throughout Fabric API sources; `EntityType.COW` is now `EntityTypes.COW`; dye-colored block families are `ColorCollection`s (`Blocks.WOOL.white()`, `Blocks.STAINED_GLASS.red()`, there is no `Blocks.WHITE_WOOL`).

---

## 1. Project layout and client entrypoint

Use Loom split source sets (`loom { splitEnvironmentSourceSets() }`): common code in `src/main/java`, client-only in `src/client/java`, client resources in `src/client/resources` (or keep assets in `src/main/resources/assets/...`, both end up in the jar). The compiler then prevents common code from touching client classes. `@Environment(EnvType.CLIENT)` is not needed in `src/client`.

`fabric.mod.json`:
```json
"entrypoints": {
  "main":   ["com.example.ExampleMod"],
  "client": ["com.example.client.ExampleModClient"]
}
```
```java
public class ExampleModClient implements net.fabricmc.api.ClientModInitializer {
    @Override public void onInitializeClient() { /* register everything client-side here */ }
}
```
`onInitializeClient` runs before the window/GPU device exists and before resources load. Safe: registering events, key mappings, renderers, model layers, render pipelines (static fields), HUD elements, particle providers. Not safe: touching `Minecraft.getInstance().level`, `RenderSystem.getDevice()`, textures, fonts. Use `ClientLifecycleEvents.CLIENT_STARTED` for "after the game window is up".

---

## 2. Client lifecycle and tick events (fabric-lifecycle-events-v1)

Package `net.fabricmc.fabric.api.client.event.lifecycle.v1` (src: fapi:fabric-lifecycle-events-v1 client):

| Event | Callback signature |
|---|---|
| `ClientLifecycleEvents.CLIENT_STARTED` | `(Minecraft client)` |
| `ClientLifecycleEvents.CLIENT_STOPPING` | `(Minecraft client)` |
| `ClientTickEvents.START_CLIENT_TICK` / `END_CLIENT_TICK` | `(Minecraft client)` |
| `ClientTickEvents.START_LEVEL_TICK` / `END_LEVEL_TICK` | `(ClientLevel level)` (renamed from `*_WORLD_TICK`) |
| `ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE` | `(Minecraft client, ClientLevel level)` (was `ClientWorldEvents`) |
| `ClientChunkEvents.CHUNK_LOAD` / `CHUNK_UNLOAD` | `(ClientLevel level, LevelChunk chunk)` |
| `ClientEntityEvents.ENTITY_LOAD` / `ENTITY_UNLOAD` | `(Entity entity, ClientLevel level)` |
| `ClientBlockEntityEvents.BLOCK_ENTITY_LOAD` / `UNLOAD` | `(BlockEntity be, ClientLevel level)` |

```java
ClientTickEvents.END_CLIENT_TICK.register(client -> {
    if (client.player == null) return;            // title screen / loading
    while (MY_KEY.consumeClick()) { ... }
});
```
Client ticks run at 20 TPS on the render thread; `client.player`/`client.level` are `null` outside a world. Client tick runs inside a per-tick gizmo collector, so `Gizmos.*` debug shapes can be emitted from `END_CLIENT_TICK` (see `client-rendering.md` section 6).

Useful `Minecraft` accessors (src: client `net/minecraft/client/Minecraft.java`): `getInstance()`, fields `player`, `level`, `options`, `font`, `gui`, `gameRenderer`; methods `getSoundManager()`, `getTextureManager()`, `getResourceManager()`, `getWindow()`, `getDeltaTracker()`, `getEntityRenderDispatcher()`, `getBlockEntityRenderDispatcher()`, `getItemModelResolver()`, `getModelManager()`, `getAtlasManager()`, `getEntityModels()`, `textInputManager()`, `hasShiftDown()/hasControlDown()/hasAltDown()`, `isLocalServer()`, `getConnection()`-style access via `player.connection` (`ClientPacketListener#sendChat(String)`, `#sendCommand(String)`).

---

## 3. Key mappings (fabric-key-mapping-api-v1)

Renamed in 26.1: `KeyBindingHelper.registerKeyBinding` -> `KeyMappingHelper.registerKeyMapping` (package `net.fabricmc.fabric.api.client.keymapping.v1`). Categories are vanilla records since 1.21.9: `KeyMapping.Category`. (src: client `net/minecraft/client/KeyMapping.java`, fapi:fabric-key-mapping-api-v1 testmodClient `KeyMappingsTest.java`)

```java
public static final KeyMapping.Category CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath("modid", "main"));
public static final KeyMapping OPEN_KEY = KeyMappingHelper.registerKeyMapping(
        new KeyMapping("key.modid.open", InputConstants.Type.KEYBOARD, InputConstants.KEY_J, CATEGORY));
// toggle-style key (like sprint/sneak toggles):
public static final KeyMapping STICKY = KeyMappingHelper.registerKeyMapping(
        new ToggleKeyMapping("key.modid.sticky", InputConstants.KEY_R, CATEGORY, () -> true, false));

ClientTickEvents.END_CLIENT_TICK.register(client -> {
    while (OPEN_KEY.consumeClick()) client.gui.setScreen(new MyScreen(Component.literal("Demo")));
    if (STICKY.isDown()) { ... }
});
```
- Constructors: `KeyMapping(String name, int keysym, Category)`, `KeyMapping(String name, InputConstants.Type type, int value, Category)`, `KeyMapping(..., Category, int order)`. Vanilla built-in categories: `MOVEMENT, MISC, MULTIPLAYER, GAMEPLAY, INVENTORY, CREATIVE, SPECTATOR, DEBUG`.
- `KeyMapping.Category.register(id)` throws if the id is already registered; call once (static field). Category label translation key = `id.toLanguageKey("key.category")` -> `"key.category.modid.main"`. Key name is used directly as translation key (`"key.modid.open"`). Lang file: `assets/modid/lang/en_us.json`.
- Mod categories are sorted after all vanilla ones (Fabric `CategoryComparator`).
- `consumeClick()` returns true once per press (counts presses; loop with `while`). `isDown()` = currently held. `KeyMappingHelper.getBoundKeyOf(km)` returns the user-rebound `InputConstants.Key`.
- Mouse binding: `InputConstants.Type.MOUSE` with `InputConstants.MOUSE_BUTTON_*` (1 = left, 2 = middle, 3 = right, 4..8 extra).
- Raw key state without a mapping: `InputConstants.isKeyDown(int scancode)` (reads `SDL_GetKeyboardState`).
- Verified at runtime: pressing the mapping via client gametest `context.getInput().pressKey(OPEN_KEY)` triggered `consumeClick()` and opened the screen.

---

## 4. Client commands (fabric-command-api-v2, client side)

Package `net.fabricmc.fabric.api.client.command.v2`. Renamed in 26.1: `ClientCommandManager` -> `ClientCommands`. (src: fapi:fabric-command-api-v2 client + testmodClient `ClientCommandTest.java`)

```java
ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(
    ClientCommands.literal("demo")
        .requires(FabricClientCommandSource::attended)   // 26.2+: refuse when triggered by a server-sent click event
        .then(ClientCommands.argument("n", IntegerArgumentType.integer()).executes(ctx -> {
            int n = IntegerArgumentType.getInteger(ctx, "n");
            ctx.getSource().sendFeedback(Component.literal("n=" + n));   // or sendError(...)
            return Command.SINGLE_SUCCESS;
        }))));
```
- `FabricClientCommandSource`: `sendFeedback(Component)`, `sendError(Component)`, `getClient()`, `getPlayer()` (`LocalPlayer`), `getLevel()` (`ClientLevel`), `getEntity()`, `getPosition()`, `getRotation()`, `getMeta(String)`, `attended()`.
- Registry-backed arguments need `buildContext` (e.g. `ItemArgument.item(buildContext)`).
- `ClientCommands.refreshCommandCompletions()` after a `requires(...)` condition changes; `ClientCommands.getActiveDispatcher()` for the live dispatcher.
- Client commands shadow server commands of the same name; they are executed locally when typed in chat. Verified: typing `/demo 5` via gametest input ran the command (feedback appeared in chat).

---

## 5. HUD (fabric-rendering-v1 `hud` package)

`HudRenderCallback` was removed in 26.1. Use `HudElementRegistry` (package `net.fabricmc.fabric.api.client.rendering.v1.hud`). (src: fapi:fabric-rendering-v1 client `hud/*`, testmodClient `HudTests.java`, `HudStatusBarHeightsTest.java`)

```java
HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR,
        Identifier.fromNamespaceAndPath("modid", "demo_hud"),
        (GuiGraphicsExtractor graphics, DeltaTracker delta) -> {
            Minecraft mc = Minecraft.getInstance();
            graphics.fill(2, 2, 120, 30, 0x80000000);
            graphics.text(mc.font, "Demo HUD", 4, 4, CommonColors.WHITE, true);
            graphics.item(new ItemStack(Items.DIAMOND), 4, 12);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("hud/heart/full"), 24, 14, 9, 9);
        });
```
(This exact element was rendered in a headless gametest screenshot.)

- `HudElement` = `void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker)`.
- `HudElementRegistry`: `addFirst(id, el)` (under everything), `addLast(id, el)` (above everything), `attachElementBefore(vanillaId, id, el)`, `attachElementAfter(vanillaId, id, el)`, `removeElement(id)`, `replaceElement(id, original -> newElement)` (wrap vanilla, e.g. push/scale/pop `graphics.pose()` around `original.extractRenderState(...)`).
- `VanillaHudElements` ids, in draw order: `MISC_OVERLAYS` (vignette, spyglass, powder snow), `CROSSHAIR`, `SPECTATOR_MENU`, `HOTBAR`, `ARMOR_BAR`, `HEALTH_BAR`, `FOOD_BAR`, `AIR_BAR`, `MOUNT_HEALTH`, `INFO_BAR` (xp/locator/jump bar), `EXPERIENCE_LEVEL`, `INFO_BAR_FOREGROUND`, `HELD_ITEM_TOOLTIP`, `SPECTATOR_TOOLTIP`, `MOB_EFFECTS`, `BOSS_BAR`, `SLEEP`, `DEMO_TIMER`, `SCOREBOARD`, `OVERLAY_MESSAGE`, `TITLE_AND_SUBTITLE`, `CHAT`, `PLAYER_LIST`, `SUBTITLES`.
- Status bars stacked above the hotbar: `HudStatusBarHeightRegistry.addLeft/addRight(id, StatusBarHeightProvider)` (`int getStatusBarHeight(Player)`), `replaceLeft/replaceRight`, `getHeight(id)` (y offset from screen bottom; use `graphics.guiHeight() - HudStatusBarHeightRegistry.getHeight(id)` instead of the hardcoded `39 + ...`). Frozen once the client finished starting.
- Partial tick: `deltaTracker.getGameTimeDeltaPartialTick(false)`; `getGameTimeDeltaTicks()`, `getRealtimeDeltaTicks()`.
- HUD hidden (F1): `mc.gui.hud.isHidden()` / `toggle()`.
- Item decorations overlay on any GUI item: `ExtractItemDecorationsCallback.EVENT` `(graphics, font, stack, x, y)` (was `DrawItemStackOverlayCallback`/`RenderItemDecorationsCallback`).

---

## 6. Drawing with `GuiGraphicsExtractor` (HUD, screens, widgets)

All calls *record* into a `GuiRenderState`; actual GPU work happens later in `GuiRenderer`. Coordinates are scaled GUI pixels. Matrix stack is 2D: `graphics.pose()` returns a JOML `Matrix3x2fStack` (`pushMatrix()`, `popMatrix()`, `translate(x, y)`, `scale(s)`/`scale(x, y)`, `rotate(radians)`); there is no z. (src: client `net/minecraft/client/gui/GuiGraphicsExtractor.java`)

| Purpose | 26.3 method |
|---|---|
| size | `guiWidth()`, `guiHeight()` |
| rectangle | `fill(x0, y0, x1, y1, argb)`, `fill(RenderPipeline, x0, y0, x1, y1, argb)`, `fillGradient(x0, y0, x1, y1, argbTop, argbBottom)`, `outline(x, y, w, h, argb)`, `horizontalLine(x0, x1, y, argb)`, `verticalLine(x, y0, y1, argb)` |
| text | `text(Font, String/Component/FormattedCharSequence, x, y, argb[, dropShadow])`, `centeredText(Font, String/Component/FormattedCharSequence, centerX, y, argb)`, `textWithWordWrap(Font, FormattedText, x, y, width, argb[, shadow])` (returns height), `textWithBackdrop(...)` |
| items | `item(ItemStack, x, y[, seed])`, `item(LivingEntity owner, stack, x, y, seed)`, `fakeItem(stack, x, y)`, `itemDecorations(Font, stack, x, y[, countText])` |
| texture region | `blit(RenderPipeline, Identifier texture, x, y, float u, float v, int w, int h, int texW, int texH[, int argb])`; `blit(pipeline, tex, x, y, u, v, w, h, srcW, srcH, texW, texH[, argb])` (scaled); `blit(Identifier, x0, y0, x1, y1, u0, u1, v0, v1)` (normalized UV) |
| GUI atlas sprite | `blitSprite(RenderPipeline, Identifier sprite, x, y, w, h[, float alpha / int argb])` (honors nine-slice/tile scaling from `.mcmeta`); partial: `blitSprite(pipeline, sprite, spriteW, spriteH, texX, texY, x, y, w, h[, argb])`; `blitSprite(pipeline, TextureAtlasSprite, x, y, w, h[, argb])` |
| scissor | `enableScissor(x0, y0, x1, y1)`, `disableScissor()`, `containsPointInScissor(x, y)` |
| layering | `nextStratum()` (start a new layer drawn above everything so far), `blurBeforeThisStratum()` |
| tooltip | `setTooltipForNextFrame(Font, Component, x, y)`, `setTooltipForNextFrame(Font, ItemStack, x, y)`, `setComponentTooltipForNextFrame(Font, List<Component>, x, y)` |
| other | `entity(...)`, `skin(...)`, `book(...)`, `bannerPattern(...)`, `map(MapRenderState)`, `requestCursor(CursorType)` |

- `RenderPipeline` argument for textures: almost always `RenderPipelines.GUI_TEXTURED` (`net.minecraft.client.renderer.RenderPipelines`). Others: `GUI_TEXTURED_PREMULTIPLIED_ALPHA`, `GUI` (untextured), `GUI_INVERT`, `GUI_TEXT_HIGHLIGHT`.
- Texture path for `blit` is the full file path: `Identifier.withDefaultNamespace("textures/gui/container/dispenser.png")`. Sprite ids for `blitSprite` are atlas ids **without** `textures/gui/sprites/` and `.png`: file `assets/modid/textures/gui/sprites/widget/foo.png` -> `Identifier.fromNamespaceAndPath("modid", "widget/foo")`. The `gui` atlas (`assets/minecraft/atlases/gui.json`) is a `directory` source over `gui/sprites` (+ `mob_effect/` prefix over `mob_effect`), so any namespace's `textures/gui/sprites/**` is stitched automatically.
- GUI sprite scaling metadata `foo.png.mcmeta`: `{"gui": {"scaling": {"type": "nine_slice", "width": 32, "height": 32, "border": 4}}}`; other types `stretch` (default) and `tile` (`width`, `height`). Animated GUI sprites use the normal `"animation"` block.
- Custom 3D-in-GUI ("picture in picture") renderers: `PictureInPictureRendererRegistry.register(ctx -> new MyPipRenderer(...))` (`ctx.minecraft()`); see fapi:fabric-rendering-v1 testmodClient `gui/PictureInPictureRendererTest.java`.
- Tooltip components: `ClientTooltipComponentCallback.EVENT` maps a `TooltipComponent` to a `ClientTooltipComponent`.

---

## 7. Screens and widgets

(src: client `gui/screens/Screen.java`, `gui/components/*`; reference fabric-docs `reference/latest/src/client/java/com/example/docs/rendering/screens/*` updated to 26.3 names below)

```java
public class DemoScreen extends Screen {
    private final @Nullable Screen parent;
    private EditBox box;
    public DemoScreen(Component title, @Nullable Screen parent) { super(title); this.parent = parent; }

    @Override protected void init() {                        // called on open AND on every resize
        box = addRenderableWidget(new EditBox(font, width / 2 - 100, 60, 200, 20, Component.literal("input")));
        box.setMaxLength(64);
        box.setResponder(text -> { /* on change */ });
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(width / 2 - 50, 90, 100, 20).build());
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);       // background + widgets
        g.centeredText(font, title, width / 2, 20, 0xFFFFFFFF);
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return true; }
}
// open: Minecraft.getInstance().gui.setScreen(new DemoScreen(Component.literal("Demo"), null));
```
- Fields: `minecraft`, `font`, `width`, `height`, `title`. Lifecycle: `init()`, `tick()`, `removed()`, `added()`, `onClose()`, `resize(w, h)`, `rebuildWidgets()`, `shouldCloseOnEsc()`, `isPauseScreen()`, `isInGameUi()`.
- Adding: `addRenderableWidget(w)` (drawn + interactive), `addRenderableOnly(r)`, `addWidget(listener)`, `removeWidget`, `clearWidgets`.
- Background hooks: `extractBackground(...)`, `extractBlurredBackground(g)`, `extractMenuBackground(g)`, `extractTransparentBackground(g)`, `extractPanorama(g, a)`.
- Input methods take event records: `keyPressed(KeyEvent)` (`key()`, `keycode()`, `modifiers()`), `mouseClicked(MouseButtonEvent, boolean doubleClick)`, `mouseReleased(MouseButtonEvent)`, `mouseDragged(MouseButtonEvent, dx, dy)`, `mouseScrolled(x, y, scrollX, scrollY)`, `charTyped(CharacterEvent)` (`codepoint()`, `codepointAsString()`).
- Widgets (`net.minecraft.client.gui.components`): `Button` (`Button.builder(msg, onPress).bounds(x, y, w, h).pos/size/width/tooltip(Tooltip.create(...)).build()`; subclass `Button.Plain`), `EditBox` (`getValue`, `setValue`, `setResponder`, `setMaxLength`, `setHint(Component)`, `addFormatter(EditBox.TextFormatter)`, `setTextColor`, `setBordered`; no `setFilter` in 26.3), `MultiLineEditBox`, `Checkbox`, `CycleButton`, `AbstractSliderButton`, `StringWidget`, `MultiLineTextWidget`, `ImageButton`, `SpriteIconButton`, `ObjectSelectionList`/`ContainerObjectSelectionList`, `ImageWidget`, `ItemDisplayWidget`, `PlayerFaceWidget`, tabs (`tabs/*`), layouts (`gui/layouts/*`: `LinearLayout`, `GridLayout`, `HeaderAndFooterLayout`, `FrameLayout`).
- Custom widget: `extends AbstractWidget`, implement `protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a)` and `protected void updateWidgetNarration(NarrationElementOutput out)`; `isHovered()`, `getX()/getY()`, `width/height`.
- **Custom text-input widgets (SDL, 26.3):** notify the IME/text-input manager when focus changes or typing silently breaks: `Minecraft.getInstance().onTextInputFocusChange(this, focused)` (vanilla `EditBox` already does). (src: client `gui/components/EditBox.java:539`, fabric blog 26.3)
- Verified at runtime: the screen above with `EditBox` + `Button` + `blitSprite(..."widget/button"...)` + `blit(...dispenser.png...)` rendered correctly and accepted typed characters.

### 7.1 Adding to / listening on vanilla screens (fabric-screen-api-v1)
(src: fapi:fabric-screen-api-v1 client `ScreenEvents.java`, `Screens.java`, testmodClient `ScreenTests.java`)
```java
ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
    if (screen instanceof TitleScreen) {
        Screens.getWidgets(screen).add(Button.builder(Component.literal("Demo"),
                b -> client.gui.setScreen(new DemoScreen(Component.literal("Demo"), screen))).bounds(4, 4, 60, 20).build());
        ScreenEvents.afterExtract(screen).register((s, g, mouseX, mouseY, tickProgress) -> { /* draw on top */ });
        ScreenKeyboardEvents.allowKeyPress(screen).register((s, keyEvent) -> true);   // false cancels
    }
});
```
- Global: `ScreenEvents.BEFORE_INIT`, `ScreenEvents.AFTER_INIT` `(Minecraft, Screen, int w, int h)`.
- Per-screen (register inside `BEFORE_INIT`/`AFTER_INIT`, they are cleared when the screen re-inits): `ScreenEvents.remove(screen)`, `beforeExtract(screen)`, `afterBackground(screen)`, `afterForeground(screen)` (container screens: above slots/labels, below carried item/tooltip), `afterExtract(screen)`, `beforeTick(screen)`, `afterTick(screen)`. Old names `beforeRender/afterRender` are now `beforeExtract/afterExtract`.
- `ScreenMouseEvents.allow/before/afterMouseClick|Release|Drag|Scroll(screen)`, `ScreenKeyboardEvents.allow/before/afterKeyPress|KeyRelease|CharType(screen)`; `allow*` returns boolean to cancel.
- `Screens.getWidgets(screen)` (mutable list of `AbstractWidget`s, adding registers them as children), `Screens.getFont(screen)`, `Screens.getMinecraft(screen)`.

### 7.2 Container (menu) screens
(src: client `gui/screens/inventory/AbstractContainerScreen.java`, `gui/screens/MenuScreens.java`)
```java
public class DirtChestScreen extends AbstractContainerScreen<DirtChestMenu> {
    private static final Identifier BG = Identifier.withDefaultNamespace("textures/gui/container/dispenser.png");
    public DirtChestScreen(DirtChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);                       // or (menu, inv, title, imageWidth, imageHeight)
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        g.blit(RenderPipelines.GUI_TEXTURED, BG, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, 256, 256);
    }
}
// ClientModInitializer:
MenuScreens.register(ModMenuTypes.DIRT_CHEST, DirtChestScreen::new);
```
- Fields: `imageWidth/imageHeight` (default 176x166, final, set via constructor), `leftPos/topPos`, `titleLabelX/Y`, `inventoryLabelX/Y`, `menu`, `hoveredSlot`. Hooks: `extractContents`, `extractLabels(g, x, y)`, `extractSlots`, `extractSlot`, `extractTooltip`, `extractCarriedItem`, `containerTick()`, `slotClicked(...)`.
- `MenuScreens.register` is private in vanilla, made public by `fabric-transitive-access-wideners-v1` (no Fabric wrapper class; Fabric's old `HandledScreens` is gone). Extended menus with open data are in `fabric-menu-api-v1` (common side: `ExtendedMenuType`, see that module).

---

## 8. Sounds (client)

- Play UI sound: `mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f))` (overloads `forUI(SoundEvent|Holder<SoundEvent>, pitch[, volume])`, also `forMusic`, `forLocalAmbience`, `forAmbientAddition`, `forAmbientMood`, `forJukeboxSong`). `SoundManager#playDelayed(instance, ticks)`, `stop(instance)`, `stop(Identifier, SoundSource)`, `stop()`.
- Moving / looping sound: `extends AbstractTickableSoundInstance` (ctor `(SoundEvent, SoundSource, RandomSource)`; use `SoundInstance.createUnseededRandom()`), set `volume`, `pitch`, `looping`, `x/y/z`, override `tick()` and call `stop()` when done; `canStartSilent()`. (fabric-docs reference `sound/instance/CustomSoundInstance.java` matches 26.3 API.)
- Custom audio streams: implement `FabricSoundInstance#getAudioStream(SoundBufferLibrary, Identifier, boolean)` and use sound id `FabricSoundInstance.EMPTY_SOUND` (src: fapi:fabric-sound-api-v1, testmodClient `SineSound.java`).
- Sound events themselves (`sounds.json`, `SoundEvent` registration) are common/resource-pack topics.
- Headless/CI: OpenAL fails without an audio device ("Error starting SoundSystem. Turning off sounds"); the game continues fine, calls become no-ops.

## 9. Toasts, chat, overlay messages, titles

- System toast: `SystemToast.add(mc.gui.toastManager(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, title, message)` or `addOrUpdate(...)`; custom id: `new SystemToast.SystemToastId()` (optionally with display time). Custom toast: implement `net.minecraft.client.gui.components.toasts.Toast` (`getWantedVisibility()`, `update(ToastManager, long fullyVisibleForMs)`, `extractRenderState(GuiGraphicsExtractor, Font, long)`, optional `width()/height()/getToken()/getSoundEvent()`), add with `mc.gui.toastManager().addToast(toast)`.
- Local chat line (not sent to server): `mc.gui.hud.getChat().addClientSystemMessage(Component)`; `mc.player.sendSystemMessage(Component)` (also local on the client), action bar: `mc.player.sendOverlayMessage(Component)` or `mc.gui.hud.setOverlayMessage(Component, boolean animate)`; titles: `mc.gui.hud.setTitle/setSubtitle/setTimes/clearTitles`.
- Send chat/command to server: `mc.player.connection.sendChat("hi")`, `mc.player.connection.sendCommand("time set day")` (no leading slash).
- Chat events (`ClientReceiveMessageEvents`, `ClientSendMessageEvents`) live in `fabric-message-api-v1` (client package) - not covered here in depth.
- Verified: `/demo 5` produced feedback `n=5`, the client system line and a toast in a gametest screenshot.

## 10. Client-side data and options

- Vanilla options: `mc.options` (`Options`), values are `OptionInstance<T>`: `mc.options.guiScale().get()/set(...)`, `renderDistance()`, `cloudStatus()`, `preferredGraphicsBackend()` (`PreferredGraphicsApi.DEFAULT/OPENGL/VULKAN`), `getSoundSourceOptionInstance(SoundSource)`, key mappings `mc.options.keyUse`, `keyChat`, `keyInventory`, ... `mc.options.save()` writes `options.txt`. Mod key mappings are appended to `options.txt` automatically.
- Own config: write a file under `FabricLoader.getInstance().getConfigDir()` (plain JSON via Gson/Codec). There is no Fabric config API in Fabric API; common third-party choices are Cloth Config / YACL / ModMenu integration (separate mods).
- Sodium replaces the Video Settings screen; to add options there use Sodium's config API (see `client-rendering.md` section 13).
- Per-world client caches: `FabricLoader.getInstance().getGameDir()`; key by server address/world name yourself.

---

## 11. Where to look

- Vanilla GUI: client `net/minecraft/client/gui/{GuiGraphicsExtractor,Gui,Hud}.java`, `gui/screens/**`, `gui/components/**`, `gui/render/GuiRenderer.java`, `gui/render/pip/*`.
- Input: client `com/mojang/blaze3d/platform/{InputConstants,Window,TextInputManager}.java`, `net/minecraft/client/{KeyMapping,ToggleKeyMapping,KeyboardHandler,MouseHandler}.java`, `input/*Event.java`.
- Fabric testmods (best examples): `fapi:*/src/testmodClient/java/**` - rendering (HUD, tooltip, level render events, PiP, armor), screen, key mapping, command, sound, particles, fluids, model loading, renderer.
- Fabric porting notes: https://fabricmc.net/2026/03/14/261.html, .../2026/06/15/262.html, .../2026/09/15/263.html; rename tables in `fabric-docs/versions/26.1.2/develop/porting/fabric-api.md`.
- fabric-docs `develop/rendering/*` and `reference/latest` are written against 26.2: several snippets do **not** compile on 26.3 (`PoseStack#mulPose(Quaternionf)`, `singleplayer.getClientLevel().waitForChunksRender()`, 10-arg `submitModel(...)`, drawing a `RenderPass` from `AFTER_TRANSLUCENT_TERRAIN`). Treat them as hints, verify against source.
