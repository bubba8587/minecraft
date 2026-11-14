# Client gametests (fabric-client-gametest-api-v1) on 26.3, incl. headless runs

Scope: writing end-to-end client tests that boot the real game, create worlds, press keys, open screens, take/compare screenshots; running them headless in a Linux container/CI. Verified 2026-11-14: Fabric API 0.166.2+26.3 (module `fabric-client-gametest-api-v1` 6.0.12) sources + a scratch mod that compiled and **passed** `runClientGameTest` under Xvfb with Mesa lavapipe (Vulkan backend), Loom 1.19.4, Loader 0.20.1, JDK 25. Index: `client.md`.

Source shorthand: `fapi` = `/tmp/claude-0/mcsrc/fabric-api/fabric-client-gametest-api-v1/src/`.

## 1. Build setup (Loom)
```groovy
fabricApi {
    configureTests {
        createSourceSet = true                 // src/gametest/{java,resources}, separate test mod
        modId = "modid-test"                   // must match src/gametest/resources/fabric.mod.json "id"
        enableGameTests = true                 // server gametests run in `build`
        enableClientGameTests = true           // adds run config + task `runClientGameTest`
        eula = true
        // clearRunDirectory = true (default)  // wipes build/run/clientGameTest before each run
        // username = "Player0" (default)
    }
}
```
This repo's shared `gradle/fabric-mod.gradle` already does exactly this. The gametest source set sees `main` and `client` classes (a test can import client classes such as your screens and key mappings; verified).

`src/gametest/resources/fabric.mod.json`:
```json
{ "schemaVersion": 1, "id": "modid-test", "version": "1.0.0", "environment": "*",
  "entrypoints": {
    "fabric-gametest":        ["com.example.test.ModGameTest"],
    "fabric-client-gametest": ["com.example.test.ModClientGameTest"] } }
```
Tasks: `./gradlew runClientGameTest` (dev run, `-Dfabric.client.gametest` is set by Loom); production-like: register `net.fabricmc.loom.task.prod.ClientProductionRunTask` with `jvmArgs.add("-Dfabric.client.gametest")` and `useXVFB = true` (defaults true on Linux when env `CI` is set), deps in `productionRuntimeMods`.

## 2. Writing a test  (ran, passed)
```java
public class ModClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext sp = context.worldBuilder().create()) {        // flat, seed 1, peaceful-ish defaults
            sp.getConnection().waitForChunksRender();                                 // NOT sp.getClientLevel() (fabric-docs snippet is wrong for 26.3)
            sp.getServer().runCommand("setblock 0 99 -4 minecraft:stone");            // no leading slash needed
            sp.getServer().runCommand("tp @a 0.5 100 -3.5 0 30");
            sp.getServer().runOnServer(server -> {
                var e = ModEntities.DEMO.create(server.overworld(), EntitySpawnReason.COMMAND);
                e.setPos(-1.5, 100, 0.5);
                server.overworld().addFreshEntity(e);
            });
            context.waitTicks(20);
            context.takeScreenshot("modid_world");                                     // build/run/clientGameTest/screenshots/0000_modid_world.png
            if (context.computeOnClient(mc -> MyClient.hudFrames) == 0) throw new AssertionError("HUD never drawn");

            context.getInput().pressKey(MyClient.OPEN_KEY);                            // KeyMapping
            context.waitForScreen(DemoScreen.class);
            context.getInput().typeChars("hello");
            context.takeScreenshot("modid_screen");
            context.setScreen(() -> null);

            context.getInput().pressKey(o -> o.keyChat);
            context.getInput().typeChars("/demo 5");
            context.getInput().pressKey(InputConstants.KEY_RETURN);
            context.waitTicks(5);
            context.takeScreenshot("modid_after_command");
        }   // closing the context leaves the world and returns to the title screen
    }
}
```
Fail a test by throwing (e.g. `AssertionError`); the runner logs `Client gametests failed with an exception`, the game crashes with a crash report in `build/run/clientGameTest/crash-reports/`, and the Gradle task fails (exit code 255 from the game JVM). Tests run sequentially; after the last one the game closes and the task succeeds.

## 3. API reference (package `net.fabricmc.fabric.api.client.gametest.v1`)
`ClientGameTestContext` (src: fapi client `.../v1/context/ClientGameTestContext.java`):
- Waiting (the game is **paused** between calls; nothing advances unless you wait): `waitTick()`, `waitTicks(n)`, `waitFor(Predicate<Minecraft>[, timeout])` (returns ticks waited; default timeout `DEFAULT_TIMEOUT` = 200 ticks, `NO_TIMEOUT = -1`), `waitForScreen(Class<? extends Screen> | null)`.
- Thread hopping (test runs on its own thread): `runOnClient(mc -> ...)`, `computeOnClient(mc -> value)`; exceptions are rethrown on the test thread with joined async stack traces (`-Dfabric.client.gametest.disableJoinAsyncStackTraces` to disable).
- Screens: `setScreen(() -> screen)`, `clickScreenButton(translationKey)`, `tryClickScreenButton(key)`.
- Screenshots: `takeScreenshot(name)` / `takeScreenshot(TestScreenshotOptions.of(name).withSize(w, h).withDeltaTicks(f).disableCounterPrefix().withDestinationDir(path))` returns the `Path`; `assertScreenshotEquals(name | TestScreenshotComparisonOptions)`, `assertScreenshotContains(...)` (returns match position). Comparison options: `TestScreenshotComparisonOptions.of("template").withRegion(x, y, w, h).withGrayscale().withAlgorithm(TestScreenshotComparisonAlgorithm...).save()` (`save()` also writes the actual image; with `-Dfabric.client.gametest.testModResourcesPath=<src/gametest/resources>` it writes missing templates there). Template files: `templates/<name>.png` at the root of the **test mod's** resources (fapi:fabric-rendering-v1 testmodClient `resources/templates/*.png`).
- `getInput()` -> `TestInput`; `worldBuilder()` -> `TestWorldBuilder`; `restoreDefaultGameOptions()`.

`TestInput`: `holdKey/releaseKey/pressKey(KeyMapping | Function<Options, KeyMapping> | InputConstants.Key | int scancode)`, `holdKeyFor(..., ticks)`, `holdMouse/releaseMouse/pressMouse(button)`, `holdMouseFor`, `holdControl/Shift/Alt` + `release*`, `typeChar(codePoint)`, `typeChars(String)`, `scroll(amount | x, y)`, `setCursorPos(x, y)`, `moveCursor(dx, dy)`, `lookAt(yaw, pitch)`, `lookAt(BlockPos)`, `resizeWindow(w, h)`. Key codes are SDL scancodes (`InputConstants.KEY_*`). Some key methods wait a tick internally; others need `waitTick()` before the effect is visible.

`TestWorldBuilder`: `setUseConsistentSettings(bool)`, `adjustSettings(WorldCreationUiState -> ...)` (e.g. `ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)`), `create()` -> `TestSingleplayerContext`, `createServer([Properties])` -> `TestDedicatedServerContext` (then `.connect()` -> `TestDedicatedServerConnection`).
`TestSingleplayerContext`: `getWorldSave()` (reopen later with `.open()`), `getConnection()`, `getServer()`, `close()` (try-with-resources).
`TestServerConnection`: `waitForChunksDownload([timeout])`, `waitForChunksRender([waitForDownload][, timeout])` (default 1200 ticks), `waitForClientboundPackets()`, `waitForServerboundPackets()`, `waitForClientboundEntityUpdates(EntityType...)`, `getClientPlayer()`, `getServerPlayer()`, `getClientLevel()`, `getServerLevel()`.
`TestServerContext`: `runCommand(String)`, `runOnServer(server -> ...)`, `computeOnServer(...)`, `waitFor(Predicate<MinecraftServer>[, timeout])`.

Determinism defaults applied by the API (src: fapi client `.../v1/package-info.java`): tutorial off, clouds OFF, anisotropic filtering 0, chunk fade 0, onboarding accessibility off, render distance 5, music volume 0; worlds: FLAT, seed `1`, no structures, `ADVANCE_TIME`/`ADVANCE_WEATHER`/`SPAWN_MOBS` false, respawn radius 0; dedicated server: offline mode, `spawn-protection=0`, `max-players=1`, `sync-chunk-writes` false on non-Windows. Exactly one server tick per client tick and at most one client tick per frame. The player spawns at y=-60 in the flat world and **falls** if you teleport them into the air (place a block under them or use creative + flight).

Filtering: `-Dfabric.client.gametest.modid=modid-test[,other]` runs only those mods' tests. The fabric-docs warning about `-Dfabric.client.gametest.disableNetworkSynchronizer` refers to a property that does not exist in the 26.3 module (no reference in source).

A test class may also implement `ClientModInitializer` to register test-only hooks (Fabric's own testmods do `implements ClientModInitializer, FabricClientGameTest` and list the class in both `client` and `fabric-client-gametest` entrypoints).

## 4. Running headless (Linux container / CI)  (verified recipe)
Facts discovered while running on this machine (Ubuntu 24.04, Mesa 25.2.8, no GPU):
1. **OpenGL under Xvfb fails**: the GL backend requests an sRGB-capable framebuffer (`SDL_GL_SetAttribute(SDL_GL_FRAMEBUFFER_SRGB_CAPABLE=22, 1)`, src: client `com/mojang/renderpearl/backend/opengl/GlBackend.java:60-64`, GL 3.3 core forward-compatible), and Xvfb's GLX offers no sRGB fbconfigs at any depth -> `BackendCreationException: Failed to create window for OpenGL context: Couldn't find matching GLX visual`. `LIBGL_ALWAYS_SOFTWARE`, `+extension GLX`, other depths do not help.
2. The game then tries **Vulkan**; without an ICD it fails with `Installed Vulkan doesn't implement the VK_KHR_surface extension` and the run dies. Installing Mesa's software Vulkan driver (lavapipe) fixes it: `apt-get install -y mesa-vulkan-drivers` (provides `/usr/share/vulkan/icd.d/lvp_icd.json`). Log then shows `Using graphics backend Vulkan, using drivers: 1.4.318 llvmpipe Mesa 25.2.8`. Fabric API's own CI does the same (`.github/workflows/build.yml`: `apt-get install -y mesa-vulkan-drivers`, matrix over `preferredGraphicsBackend` opengl/vulkan written into `run/options.txt`).
3. Loom 1.19 requires a JDK 25 **Gradle JVM** (`Dependency requires at least JVM runtime version 25`); in this container it is at `/opt/jdk25` (`/usr/lib/jvm` only has 21). Set `JAVA_HOME=/opt/jdk25`.
4. Audio: no ALSA device -> `Error starting SoundSystem. Turning off sounds & music` (harmless). `XDG_RUNTIME_DIR` unset only produces a warning; set it to a 0700 dir to silence.

Command that passed (about 30 s per run after the first build):
```bash
export JAVA_HOME=/opt/jdk25 XDG_RUNTIME_DIR=/tmp/xdg-rt; mkdir -p -m 700 /tmp/xdg-rt
xvfb-run -a -s "-screen 0 1920x1080x24" ./gradlew runClientGameTest      # add --offline if deps are cached
# outputs: build/run/clientGameTest/screenshots/NNNN_<name>.png, logs/latest.log, crash-reports/
```
(In this multi-mod repo: `./gradlew :<modname>:runClientGameTest`, run dir under `mods/<modname>/build/run/clientGameTest/`.) Default window/screenshot size is 854x480 (`MainTarget.DEFAULT_WIDTH/HEIGHT`); change with `context.getInput().resizeWindow(w, h)`. Screenshots are PNGs that can be inspected directly (an agent can `Read` them).

Things worth asserting in client tests:
- Registration side effects: counters bumped in HUD elements / render events (`computeOnClient`), key mapping opens screen (`waitForScreen`), command feedback (chat) via screenshot or by reading `mc.gui.hud.getChat()`.
- Rendering: `assertScreenshotEquals(TestScreenshotComparisonOptions.of("x").withRegion(...))` against committed templates; keep regions small and avoid animated content; lavapipe vs GPU output can differ slightly (prefer `withGrayscale()` / `assertScreenshotContains`).
- Crash-freedom of render paths: just reaching `takeScreenshot` after `waitTicks` executes a full frame of all your renderers (this is how the "render pass already open" bug in section 3.4 of `client-rendering.md` was found).

## 5. Where to look
- fapi `client/java/net/fabricmc/fabric/api/client/gametest/v1/**` (API + javadoc), `impl/client/gametest/**` (runner, input, screenshot comparison), `testmodClient/java/.../ClientGameTestTest.java` (broad example: title screen, chest, chat, cow sync, reopen world, dedicated server, reconfiguration).
- Other modules' testmods that are also client gametests: fabric-rendering-v1 `HudTests`, `HudStatusBarHeightsTest`, `LevelRenderEventsTests`; fabric-screen-api-v1; fabric-key-mapping-api-v1.
- fabric-docs `develop/automatic-testing.md`, `develop/loom/fabric-api.md#tests`, `develop/loom/production-run-tasks.md`.
