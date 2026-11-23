# Testing and debugging Fabric mods on 26.x

Scope: server gametests, client gametests, JUnit unit tests, headless dedicated-server smoke tests, logs, crash reports, mixin debugging, profiling and hot reload for this repo's Fabric mods. Verified 2026-11-14 against Minecraft 26.3 (decompiled source), Fabric API branch 26.3 (`fabric-gametest-api-v1`, `fabric-client-gametest-api-v1` and their testmods), fabric-docs `develop/automatic-testing.md`, and Loom `dev/1.19` source.

Short source paths used below:
- `common` = `/tmp/claude-0/mcsrc/common/`
- `client` = `/tmp/claude-0/mcsrc/client/`
- `fapi` = `/tmp/claude-0/mcsrc/fabric-api/`
- `loom` = FabricMC/fabric-loom `dev/1.19`

---

## 0. What this repo already has
`gradle/fabric-mod.gradle`, applied by every `mods/<id>/build.gradle`:
```groovy
fabricApi {
	configureTests {
		createSourceSet = true
		modId = "${project.mod_id}-test"
		enableGameTests = true
		enableClientGameTests = true
		eula = true
	}
}
test { useJUnitPlatform() }
```
- `mods/refmod/src/gametest/{java,resources}`: a separate test mod (`refmod-test`) with its own `fabric.mod.json` and entrypoints `fabric-gametest` and `fabric-client-gametest`.
- **No `fabric-loader-junit` dependency yet.** Add it before writing `src/test/java` tests (section 3).

Commands (do NOT run Gradle in `/home/user/minecraft` while other agents are using it):

| Command | What runs | Run dir |
|---|---|---|
| `./gradlew :refmod:build` | compile, JUnit `test`, then **`runGameTest`** (Loom wires `check.dependsOn runGameTest`) | `mods/refmod/build/run/gameTest/` |
| `./gradlew :refmod:runGameTest` | server gametests only | same |
| `./gradlew :refmod:runClientGameTest` | client gametests (real client window; xvfb in CI) | `mods/refmod/build/run/clientGameTest/` (deleted before each run by `deleteGameTestRunDir`) |
| `./gradlew :refmod:runServer` / `runClient` | normal dev runs | `mods/refmod/run/` |

Run config names are `gameTest` and `clientGameTest`, so the tasks are `runGameTest` and `runClientGameTest`. **Not** `runGametest`: that is the name inside Fabric API's own build. (src: loom `configuration/fabricapi/FabricApiTesting.java`)

---

## 1. Server gametests (`fabric-gametest-api-v1`)

### 1.1 How 26.x gametests work (vanilla model since 1.21.5)
Vanilla now stores tests in **registries**:
- `Registries.TEST_FUNCTION` (static, `BuiltInRegistries.TEST_FUNCTION`): `Consumer<GameTestHelper>` code.
- `Registries.TEST_INSTANCE` (dynamic, datapack `data/<ns>/test_instance/*.json`): a `GameTestInstance`. Types are registered in `TEST_INSTANCE_TYPE`: `minecraft:function` (`FunctionGameTestInstance`, runs a test function) and `minecraft:block_based` (`BlockBasedTestInstance`, driven by in-world test blocks).
- `Registries.TEST_ENVIRONMENT` (dynamic, `data/<ns>/test_environment/*.json`): a `TestEnvironmentDefinition` that sets world state up and tears it down. Built-in `minecraft:default` is `{"type":"minecraft:all_of","definitions":[]}`.

(src: common `net/minecraft/gametest/framework/{GameTestInstance,FunctionGameTestInstance,TestEnvironmentDefinition,GameTestEnvironments,TestData}.java`; vanilla-jar `data/minecraft/test_instance/always_pass.json`)

Old-style `@GameTest(template=...)`, `@GameTestGenerator`, `@BeforeBatch`, `TestContext`, `FabricGameTest` and `FabricGameTest.EMPTY_STRUCTURE` (Yarn/1.21.4 era) are all **gone**.

### 1.2 Declaring tests with Fabric API
Fabric's annotation is `net.fabricmc.fabric.api.gametest.v1.GameTest`, not a vanilla annotation.
- Annotated methods on a class listed under the `fabric-gametest` entrypoint are auto-registered:
  - as a `TEST_FUNCTION` in `onInitialize`, and
  - as a `FunctionGameTestInstance` in `TEST_INSTANCE`, injected when dynamic registries load (mixin `RegistryDataLoaderMixin`).
- Registration happens only if `-Dfabric-api.gametest` is set **or** you are in a dev environment. That is why tests show up in `/test` in a normal `runClient`.
- (src: fapi `fabric-gametest-api-v1/src/main/java/net/fabricmc/fabric/impl/gametest/{FabricGameTestModInitializer,TestAnnotationLocator}.java`)

Rules, enforced by `TestAnnotationLocator.validateMethod` unless the class implements `CustomTestMethodInvoker`:
- `public`, non-`static`, returns `void`, exactly one parameter of type `net.minecraft.gametest.framework.GameTestHelper`.
- Superclass methods are scanned too.
- **Test id** = `<providing mod id>:<snake_case(SimpleClassName + "_" + methodName)>`. For example `RefModGameTest#placesStone` in mod `refmod-test` becomes `refmod-test:ref_mod_game_test_places_stone`. Use this id with filters and `/test run`.

Annotation attributes and defaults (they map 1:1 onto vanilla `TestData`):
```java
@GameTest(
	environment = "minecraft:default",            // TEST_ENVIRONMENT id
	dimension = "minecraft:overworld",
	structure = "fabric-gametest-api-v1:empty",   // 8x8x8 air box (data/fabric-gametest-api-v1/gametest/structure/empty.snbt)
	maxTicks = 20,                                // timeout; raise it for anything that waits
	setupTicks = 0,
	required = true,                              // false: failure doesn't fail the suite
	rotation = Rotation.NONE,
	manualOnly = false,
	maxAttempts = 1,
	requiredSuccesses = 1,
	skyAccess = false,                            // false: enclosed in barriers
	padding = 1                                   // NB: vanilla JSON default is 0, Fabric annotation default is 1
)
```

Example (pattern from fapi testmod `ExampleFabricTestSuite.java`):
```java
package io.github.bubba8587.refmod.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityTypes;     // 26.x: constants live in EntityTypes, not EntityType
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;

public class RefModGameTest {
	@GameTest
	public void placesStone(GameTestHelper helper) {
		helper.setBlock(1, 1, 1, Blocks.STONE);              // coords are RELATIVE to the test structure origin
		helper.assertBlockPresent(Blocks.STONE, 1, 1, 1);
		helper.succeed();                                     // must succeed, or it times out at maxTicks and fails
	}

	@GameTest(structure = "refmod-test:diamond_on_floor", maxTicks = 100)
	public void structureLoads(GameTestHelper helper) {
		helper.succeedWhen(() -> helper.assertBlock(new BlockPos(0, 1, 0),
				b -> b == Blocks.DIAMOND_BLOCK, b -> Component.literal("expected diamond")));
	}

	@GameTest(dimension = "minecraft:the_nether")
	public void inNether(GameTestHelper helper) {
		helper.assertTrue(helper.getLevel().dimension() == Level.NETHER, "not in nether");
		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void zombieBurns(GameTestHelper helper) {
		helper.setTime(6000);
		var z = helper.spawn(EntityTypes.ZOMBIE, 2, 2, 2);
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(z.isOnFire(), "zombie not burning"))
			.thenSucceed();
	}
}
```

`CustomTestMethodInvoker`: per-test setup, extra parameters, and bypassing validation. Example from fabric-docs `reference/latest/src/gametest/.../EntityAttributesGameTest.java` and the fapi testmod `CustomTestInvokerTest.java`:
```java
public class MyTests implements CustomTestMethodInvoker {
	@GameTest
	public void hasDiamond(GameTestHelper helper, Block block) {   // 2 params allowed because the class is an invoker
		helper.succeedWhen(() -> helper.assertBlockPresent(block, 0, 1, 0));
	}

	@Override
	public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
		helper.setBlock(0, 1, 0, Blocks.DIAMOND_BLOCK);   // structure is already placed at this point
		method.invoke(this, helper, Blocks.DIAMOND_BLOCK);
	}
}
```
A `GameTestAssertException` thrown from the method is re-thrown unwrapped (`InvocationTargetException` is unwrapped for `RuntimeException`s).

`src/gametest/resources/fabric.mod.json` (already present in refmod):
```json
{ "schemaVersion": 1, "id": "refmod-test", "version": "1.0.0", "environment": "*",
  "entrypoints": {
    "fabric-gametest": ["io.github.bubba8587.refmod.test.RefModGameTest"],
    "fabric-client-gametest": ["io.github.bubba8587.refmod.test.RefModClientGameTest"] } }
```
The test mod's mod id becomes the test namespace. Classes not listed under `fabric-gametest` are ignored, and a listed class with no `@GameTest` methods only logs a warning.

### 1.3 `GameTestHelper` cheat sheet
All positions are relative to the structure; convert with `absolutePos` and `relativePos`. Full list: `grep -n "public " common/net/minecraft/gametest/framework/GameTestHelper.java`.

- **Finish**:
  - `succeed()`
  - `succeedIf(Runnable)`: checks now and succeeds or fails.
  - `succeedWhen(Runnable)`: retries every tick until the assertion stops throwing.
  - `succeedOnTickWhen(int tick, Runnable)`
  - `succeedWhenBlockPresent(Block, pos)`
  - `succeedWhenEntityPresent` / `succeedWhenEntityNotPresent`
  - `succeedWhenEntityData(pos, type, accessor, value)`
  - `fail(String|Component[, BlockPos|Entity])`
  - `failIf(Runnable)`
  - `failIfEver(Runnable)`: runs the check on every remaining tick.
  - `assertionException(...)` builds an exception to throw yourself.
- **Scheduling**:
  - `runAfterDelay(ticks, r)`, `runAtTickTime(tick, r)`, `onEachTick(r)`, `runBeforeTestEnd(r)`, `getTick()`
  - Gotcha: `runAtTickTime`/`runAfterDelay` store runs in an `Object2LongMap<Runnable>` keyed by the Runnable (`GameTestInfo.runAtTickTimeMap`), so scheduling the same Runnable object at several ticks runs it only once, at the last tick given. Wrap it in a fresh lambda per tick (`() -> r.run()`).
  - `startSequence()` returns a `GameTestSequence`, with `.thenWaitUntil([delay,] r)`, `.thenWaitAtLeast(min, r)`, `.thenIdle(n)`, `.thenExecute(r)`, `.thenExecuteAfter(n, r)`, `.thenExecuteFor(n, r)`, `.thenSucceed()`, `.thenFail(supplier)`, `.thenTrigger()`
- **Blocks**:
  - `setBlock(pos|xyz, Block|BlockState[, Direction])`, `getBlockState`, `destroyBlock`, `placeBlock`
  - `useBlock(pos[, player[, hit]])`, `useItemOnBlock(player, item, pos)`, `placeAt(player, stack, pos, face)`
  - `pressButton`, `pullLever`, `pulseRedstone(pos, duration)`, `randomTick`, `tickBlock`, `setBiome`, `tickPrecipitation`
  - `getBlockEntity(pos, Class)`, `forEveryBlockInStructure`
- **Block assertions**:
  - `assertBlockPresent` / `assertBlockNotPresent(Block, pos)`, `assertBlock(pos, Predicate<Block>, Function<Block,Component>)`
  - `assertBlockState(pos, expected | predicate, msg)`, `assertBlockProperty(pos, Property, value)`, `assertBlockTag(TagKey, pos)`
  - `assertBlockEntityData(...)`, `assertRedstoneSignal(pos, dir, IntPredicate, Supplier<Component>)`
  - `assertSameBlockState(s)`
  - `assertContainerContains` / `assertContainerContainsSingle` / `assertContainerEmpty(pos[, item])`
  - `assertAtTickTimeContainerContains|Empty`
- **Entities**:
  - `spawn(type, pos|xyz|Vec3[, amount|reason])`, `spawnWithNoFreeWill(...)`
  - Builders: `spawnEntity(type, pos)` returns `GameTestEntityBuilder` (`.spawnReason(..).rotation(..).requirePersistence(..).spawn()`); `spawnMob(type, pos)` returns `GameTestMobBuilder` (`.withNoFreeWill().spawn()`)
  - `spawnItem(item, pos)`, `findOneEntity`, `findClosestEntity`, `findEntities`, `getEntities`
  - `kill`, `discard`, `hurt(e, DamageSource, amount)`, `killAllEntities`, `killAllEntitiesOfClass`
  - `moveTo(mob, pos)`, `walkTo(mob, pos, speed)`, `withLowHealth(e)`, `makeAboutToDrown(e)`
- **Entity assertions**:
  - `assertEntityPresent` / `assertEntityNotPresent(type[, pos|AABB|xyz][, distance])`, `assertEntitiesPresent(type, n)`
  - `assertEntityInstancePresent`, `assertEntityTouching` / `assertEntityNotTouching`
  - `assertEntityData(pos, type, Predicate)`, `assertEntityProperty(e, fn, expected, Component)`
  - `assertEntityIsHolding`, `assertEntityInventoryContains`, `assertLivingEntityHasMobEffect(e, Holder<MobEffect>, amp)`
  - `assertItemEntityPresent` / `assertItemEntityNotPresent`, `assertItemEntityCountIs`
- **Values**:
  - `assertTrue` / `assertFalse(cond, String|Component)`
  - `assertValueEqual(value, expected, name)`: note the argument order.
  - `assertValueInBetween(lo, v, hi, name)`
- **Players**:
  - `makeMockPlayer(GameType)`: an anonymous `Player`, not on a connection.
  - `makeMockServerPlayer(GameType)`: declared return type is `Player` (cast to `ServerPlayer`); not in the level and `connection` is null, so anything that sends it packets (`ServerPlayNetworking.send`, `sendSystemMessage`) NPEs.
  - `makeMockServerPlayerInLevel()`: a real `ServerPlayer` joined through `PlayerList#placeNewPlayer` on an embedded connection (packets work, attachment sync works). It is **always creative** (its `gameMode()` is overridden to `CREATIVE`, so `gameMode.destroyBlock` drops nothing; use `makeMockServerPlayer(GameType.SURVIVAL)` for survival breaking/drops), and it is `@Deprecated(forRemoval = true)` in 26.3 with no replacement yet. Walking it with `player.setOnGround(true); player.move(MoverType.SELF, vec)` emits real `step` game events (and `setShiftKeyDown(true)` makes them sneaking steps). It stays in the player list after the test: call `helper.getLevel().getServer().getPlayerList().remove(player)`. A **survival player in the level** (connected, so overlay messages work, and taking real durability damage): copy its body without the `gameMode()` override (`CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false)`, `new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation())`, `new EmbeddedChannel(new Connection(PacketFlow.SERVERBOUND))`, `placeNewPlayer(connection, player, cookie)`), then `player.setGameMode(GameType.SURVIVAL)` (bubbas_reworked_mobs `LootDisplayGameTest#player`). Every mock player is named `test-mock-player`, so select a specific one in commands by tag (`player.addTag(..)` + `@a[tag=..,limit=1]`), not by name.
  - Breaking as a player (fires `PlayerBlockBreakEvents`): `serverPlayer.gameMode.destroyBlock(helper.absolutePos(pos))`. Respawn: `getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED)`.
  - Looking: for a living entity, `Entity#pick`, `getViewVector` and `getLookAngle` use the **head** yaw (`LivingEntity#getViewYRot` returns `yHeadRot`), so `snapTo(x, y, z, yRot, xRot)` alone leaves it looking along its old head yaw (south, 0, for a new player); also call `player.setYHeadRot(yRot)` (src: common `world/entity/LivingEntity.java` `getViewYRot`; bit `LootDisplayGameTest#displayCommandPlacesAtPositionAndWhereYouLook`).
  - Projectiles: throw them like the game does, from a mock player positioned with `player.snapTo(x, y, z, yRot, xRot)` and `Projectile.spawnProjectileFromRotation(Factory::new, level, stack, player, 0, 1.5F, 0)`. In refmod a hand-built owner-less `new Snowball(level, x, y, z, stack)` with `setDeltaMovement(0, -1, 0)` dropped through a pig onto the block below without hitting it (cause not traced, UNVERIFIED); the player-thrown version hits.
- **World**: `getLevel()` (a `ServerLevel`), `setTime(ticks)`, `getHeight(Heightmap.Types, x, z)`, `getBounds()`, `getTestRotation()`, `getTestDirection()`.

Name changes that bite you: assertions throw `GameTestAssertException(Component, int tick)`, not the old `GameTestException(String)`. Use `helper.assertionException("translation.key", args)` or `Component.literal`.

### 1.4 Structures (`.snbt` / `.nbt`)
Where structures are looked up, via `StructureTemplateManager` sources (src: common `world/level/levelgen/structure/templatesystem/StructureTemplateManager.java`; fapi mixin `StructureTemplateManagerMixin`):
1. The world's `generated/<ns>/structure/<path>.nbt`, which is what structure blocks save to.
2. Vanilla resources: `data/<ns>/structure/<path>.nbt` (gzipped NBT).
3. **Fabric adds** `data/<ns>/gametest/structure/<path>.snbt` (text SNBT, data-fixed from its `DataVersion`).

`@GameTest(structure = "refmod-test:diamond_on_floor")` finds `src/gametest/resources/data/refmod-test/gametest/structure/diamond_on_floor.snbt` or `.../data/refmod-test/structure/diamond_on_floor.nbt`. Dots are fine in the path, e.g. `exampletestsuite.diamond`.

SNBT shape (fapi `testmod/resources/data/fabric-gametest-api-v1-testmod/gametest/structure/exampletestsuite.diamond.snbt`):
```
{
    DataVersion: 2730,                 // any old version is data-fixed up; 26.3 world_version is 5023
    size: [8, 8, 8],
    data: [ {pos: [0, 0, 0], state: "minecraft:polished_andesite"}, ... ],
    entities: [],
    palette: [ "minecraft:polished_andesite", "minecraft:diamond_block", "minecraft:air" ]
}
```
(This is the `NbtUtils.structureToSnbt` format: `data` lists every block, and `palette` holds the strings. `NbtUtils.snbtToStructure` reads it.)

Making a structure:
- `/test export*` and `/test create` export via `StructureUtils.testStructuresTargetDir`. **Nothing sets that in Fabric 26.3** (a property `fabric-api.gametest.structures.output-dir` appears in fapi's build.gradle but no Java code reads it), so export prints "Test structure exporting is disabled".
- Workaround A: build the structure in `runClient`, save it with a Structure Block (SAVE mode, name `refmod-test:foo`), then copy `run/saves/<world>/generated/refmod-test/structure/foo.nbt` to `src/gametest/resources/data/refmod-test/structure/foo.nbt`.
- Workaround B: run the client with `-DMC_DEBUG_ENABLED -DMC_DEBUG_SAVE_STRUCTURES_AS_SNBT` (vanilla debug flags, see 6.5). Structure blocks then save `.snbt` to `generated/<ns>/structure/`. Copy that to `.../gametest/structure/`.
- For simple cases skip structures entirely: use the default empty structure and `helper.setBlock` in code.

### 1.5 Test environments and JSON-defined tests
Environment definition types (src: common `TestEnvironmentDefinition.java`):

| type | fields |
|---|---|
| `all_of` | `definitions: [env, ...]` |
| `clock_time` | `clock: <world_clock>`, `time: int` (26.x has `WorldClock`s, replacing a single day time) |
| `difficulty` | `difficulty` |
| `function` | `setup?: <function id>`, `teardown?: <function id>`; run at permission level GAMEMASTER |
| `game_rules` | `rules: { ... }` (GameRuleMap) |
| `timeline_attributes` | `timelines: [<timeline id>...]` |
| `weather` | `weather: "clear" | "rain" | "thunder"` |

`src/gametest/resources/data/refmod-test/test_environment/rainy_peaceful.json`:
```json
{ "type": "minecraft:all_of", "definitions": [
  { "type": "minecraft:weather", "weather": "rain" },
  { "type": "minecraft:difficulty", "difficulty": "peaceful" } ] }
```
Use it with `@GameTest(environment = "refmod-test:rainy_peaceful")`. The registry lookup is `getOrThrow`, so a typo crashes test registration at world load.

A pure-datapack test, which needs no Java (`TestData.CODEC` fields; `function` must already exist in `TEST_FUNCTION`, or use `minecraft:block_based`):
```json
// data/<ns>/test_instance/my_test.json
{ "type": "minecraft:block_based", "environment": "minecraft:default",
  "structure": "<ns>:my_test", "max_ticks": 100,
  "setup_ticks": 0, "required": true, "rotation": "none", "manual_only": false,
  "max_attempts": 1, "required_successes": 1, "sky_access": false, "padding": 0 }
```
Block-based tests use the in-world Test Block (start/log/accept/fail modes) inside the structure. Running datapack tests on the vanilla test server: `GameTestMainUtil` options `--universe`, `--report <file>`, `--tests <ns:glob*>`, `--verify`, `--repeatCount`, `--packs <dir>`. Fabric replaces this entry point with its own system properties.

### 1.6 Running and reports
JVM system properties (src: fapi `impl/gametest/GameTestSystemProperties.java`, `FabricGameTestRunner.java`, mixin `server/MainMixin.java`):

| Property | Effect |
|---|---|
| `-Dfabric-api.gametest` | The dedicated server launches `GameTestServer` instead of a normal server. **The EULA check is bypassed.** Experimental features are on. It exits when done, and exits with **-1 if the server fails to start** (otherwise Gradle would pass with zero tests). |
| `-Dfabric-api.gametest.report-file=<path>` | Writes a JUnit-style XML (`JUnitLikeTestReporter`, parent dirs created). |
| `-Dfabric-api.gametest.filter=<selector>` | Namespaced id selector with wildcards, e.g. `refmod-test:*`, `refmod-test:ref_mod_game_test_places_*`. |
| `-Dfabric-api.gametest.verify=true` | Runs selected tests 100 times per 90° rotation (catches flaky or rotation-dependent tests). Needs a filter. |

`GameTestServerMixin` makes `isDedicatedServer()` return true during gametests, so dedicated-only commands are registered and mods that check for a dedicated server behave as they would on one.

Loom's `configureTests` creates the `gameTest` run, inheriting `server`, with property `fabric-api.gametest` and run dir `build/run/gameTest`. It does **not** set a report file. Add one (put it *after* the `fabricApi { configureTests ... }` block, e.g. at the end of `gradle/fabric-mod.gradle`):
```groovy
loom {
	runs {
		named('gameTest') {
			vmArg "-Dfabric-api.gametest.report-file=${project.layout.buildDirectory.file('test-results/gametest/TEST-gametest.xml').get().asFile}"
			// vmArg "-Dfabric-api.gametest.filter=${project.mod_id}-test:*"
		}
	}
}
tasks.named('runGameTest') {
	outputs.file(project.layout.buildDirectory.file('test-results/gametest/TEST-gametest.xml'))
}
```
- Putting it under `build/test-results/**` lets CI report actions and the `upload-artifact` step from `publishing.md` pick it up.
- A Gradle-only ad-hoc filter: `./gradlew :refmod:runGameTest -Dfabric-api.gametest.filter=...`. This does NOT forward to the forked JVM. Use a project property instead: `if (project.hasProperty('gtFilter')) vmArg "-Dfabric-api.gametest.filter=${project.gtFilter}"`.

Note for multi-mod builds: gametests run per subproject with only that mod and its test mod on the classpath.

In-game (`runClient`, dev env): tests are registered, so:
- `/test run <id>`, `/test runmultiple <id> [n]`, `/test runclosest`, `/test runthese`, `/test runthat`, `/test runfailed`
- `/test verify <id>`, `/test locate <id>`
- `/test resetclosest`, `/test clearall`, `/test stop`, `/test pos`
- `/test create <id> [w h d]`

(src: common `gametest/framework/TestCommand.java`). This is the fastest way to iterate on one test visually.

### 1.7 Gotchas
- Forgetting `helper.succeed()` makes the test fail with a timeout after `maxTicks` (default **20 ticks = 1 s**).
- Coordinates are relative, and y=0 is the structure's floor layer. The default empty structure is 8×8×8 of air, and the test is enclosed in barriers unless `skyAccess = true`.
- Tests in one batch run in parallel in the same world, so do not depend on global state such as game rules or time unless you use an environment. `setTime` affects the whole level. Batches are grouped by `environment` (vanilla `GameTestBatchFactory` keys batches on environment + dimension), so giving a test its own environment (an empty `{"type":"minecraft:all_of","definitions":[]}` file is enough) runs it alone: useful when tests can hear or see each other (game-event listeners reach 16 blocks, across neighbouring test structures).
- Registry ids: use `Identifier` (26.x Mojang name, `net.minecraft.resources.Identifier`), never `ResourceLocation` (pre-1.21.11).
- Don't put gametest classes in `src/main`. With `createSourceSet = true` they stay out of the release jar.

---

## 2. Client gametests (`fabric-client-gametest-api-v1`)

### 2.1 Model
- Implement `net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest` (`void runTest(ClientGameTestContext context)`) and list it under the `fabric-client-gametest` entrypoint.
- Enabled by `-Dfabric.client.gametest`, which `configureTests` sets on the `clientGameTest` run, inheriting `client`, run dir `build/run/clientGameTest`, `--username Player0`.
- Tests run **sequentially** on a dedicated "Test thread". The game stays **paused** except inside wait calls.
- With a server running, there is exactly one server tick per client tick, and at most one client tick per frame.
- After each test the runner asserts that no world is open and no server is running. Always use try-with-resources on contexts.
- When all tests finish the client stops. The first uncaught exception is rethrown on the client thread, so the game crashes, the process exits with an error, and the Gradle task fails. Remaining tests do not run.
- (src: fapi `fabric-client-gametest-api-v1/src/client/java/.../api/client/gametest/v1/package-info.java`, `impl/client/gametest/FabricClientGameTestRunner.java`, `threading/ThreadingImpl.java`)

System properties:
- `fabric.client.gametest`: enables the tests.
- `fabric.client.gametest.modid=a,b`: runs only tests provided by those mod ids.
- `fabric.client.gametest.testModResourcesPath`: Loom sets this to the gametest resources dir; it is where `assertScreenshotEquals(...).save()` writes templates.
- `fabric.client.gametest.disableJoinAsyncStackTraces`

(src: `impl/client/gametest/TestSystemProperties.java`)

The fabric-docs warning about `-Dfabric.client.gametest.disableNetworkSynchronizer` is **stale**: no such property exists in the 26.3 source.

### 2.2 API (`net.fabricmc.fabric.api.client.gametest.v1.*`)
`context.ClientGameTestContext`:
- Constants: `DEFAULT_TIMEOUT = 200` ticks (10 s), `NO_TIMEOUT = -1`.
- Waiting:
  - `waitTick()`, `waitTicks(n)`
  - `waitFor(Predicate<Minecraft>[, timeout])` returns ticks waited
  - `waitForScreen(Class<? extends Screen> | null)`
- Screens:
  - `setScreen(Supplier<Screen>)`
  - `clickScreenButton(translationKey)`, `tryClickScreenButton(key)`
- Screenshots:
  - `takeScreenshot(name | TestScreenshotOptions)` returns a `Path` under `<gameDir>/screenshots/`
  - `assertScreenshotEquals(templateName | TestScreenshotComparisonOptions)`
  - `assertScreenshotContains(template)` returns a `Vector2i` position
- Threads:
  - `runOnClient(FailableConsumer<Minecraft>)`
  - `computeOnClient(FailableFunction<Minecraft,T>)`
- Other: `getInput()` returns a `TestInput`; `worldBuilder()` returns a `TestWorldBuilder`; `restoreDefaultGameOptions()`.

`world.TestWorldBuilder`:
- `setUseConsistentSettings(bool)`
- `adjustSettings(Consumer<WorldCreationUiState>)`
- `create()` returns a `TestSingleplayerContext`
- `createServer([Properties])` returns a `TestDedicatedServerContext`

Consistent-settings defaults: FLAT world, seed `1`, no structures, `ADVANCE_TIME=false`, `ADVANCE_WEATHER=false`, `SPAWN_MOBS=false`, `RESPAWN_RADIUS=0`.

Contexts:
- `TestSingleplayerContext` (AutoCloseable): `getWorldSave()` returns a `TestWorldSave` (`getSaveDirectory()`, `open()`), plus `getConnection()` and `getServer()`.
- `TestServerContext`: `runCommand(String)`, `runOnServer(...)`, `computeOnServer(...)`, `waitFor(Predicate<MinecraftServer>[, timeout])`.
- `TestDedicatedServerContext` (AutoCloseable): a real dedicated server in the same JVM. `connect()` returns a `TestDedicatedServerConnection`.
  - Defaults: `online-mode=false`, `spawn-protection=0`, `max-players=1`, `sync-chunk-writes=false` on non-Windows.
  - **Needs `eula.txt` in the run dir.** `configureTests { eula = true }` writes it via the `acceptGameTestEula` task.
- `TestServerConnection`:
  - `waitForChunksDownload()`, `waitForChunksRender([waitForDownload][, timeout])`
  - `waitForClientboundPackets()`, `waitForServerboundPackets()`
  - `waitForClientboundEntityUpdates(EntityType, ...)`
  - `getClientPlayer()` (LocalPlayer), `getServerPlayer()`, `getClientLevel()`, `getServerLevel()`

`TestInput`:
- Keys: `holdKey` / `releaseKey` / `pressKey` / `holdKeyFor(..., ticks)` taking `KeyMapping`, `Function<Options,KeyMapping>` (e.g. `o -> o.keyUse`), `InputConstants.Key`, or an `int` key code.
- Modifiers: `holdControl`, `holdShift`, `holdAlt` and their releases.
- Mouse: `holdMouse` / `releaseMouse` / `pressMouse` / `holdMouseFor`, `scroll(amount | x, y)`, `setCursorPos`, `moveCursor`.
- Camera and text: `lookAt(yaw, pitch | BlockPos)`, `typeChar(codePoint)`, `typeChars(String)`.
- Window: `resizeWindow(w, h)`.
- **26.x uses SDL3, not GLFW, for input.** `InputConstants.KEY_T == 23` and `KEY_RETURN == 40` are SDL scancodes. Always use `InputConstants.KEY_*` constants or key mappings; never raw GLFW numbers from old examples. (src: client `com/mojang/blaze3d/platform/InputConstants.java`)

Screenshot options:
- `TestScreenshotOptions.of(name)`
- `TestScreenshotComparisonOptions.of(templateName | NativeImage)` with `.withGrayscale()`, `.withRegion(x, y, w, h)`, `.withAlgorithm(TestScreenshotComparisonAlgorithm.exact() | meanSquaredDifference(f) | defaultAlgorithm())`, `.save()`, `.saveWithFileName(..)`
- Common to both: `.disableCounterPrefix()`, `.withSize(w, h)`, `.withDeltaTicks(f)`, `.withDestinationDir(path)`
- Templates are looked up in the test mod's resources at `templates/<name>.png`, e.g. `src/gametest/resources/templates/sound_button.png`.

Example (patterned on fapi testmod `ClientGameTestTest.java`):
```java
public class RefModClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			sp.getConnection().waitForChunksRender();
			context.takeScreenshot("refmod-world");

			BlockPos pos = context.computeOnClient(mc -> BlockPos.containing(mc.player.position()).east(2).above(2));
			sp.getServer().runCommand("setblock %d %d %d minecraft:chest".formatted(pos.getX(), pos.getY(), pos.getZ()));
			context.waitFor(mc -> mc.level.getBlockState(pos).is(Blocks.CHEST));
			context.getInput().lookAt(pos);
			context.waitTick();
			context.getInput().pressKey(o -> o.keyUse);
			context.waitForScreen(ContainerScreen.class);
			context.setScreen(() -> null);
		}

		try (TestDedicatedServerContext server = context.worldBuilder().createServer();
		     TestDedicatedServerConnection conn = server.connect()) {
			conn.waitForChunksRender();
			context.takeScreenshot("refmod-server");
		}
	}
}
```
Results of an action often appear only after a tick (`waitTick`) or a packet round trip (`waitForServerboundPackets` then `waitForClientboundPackets`). When debugging, take a screenshot first. 26.x client API differences: the current screen is `client.gui.screen()`, and the F3 overlay is `client.debugEntries.setOverlayVisible(bool)` (from the fapi testmod).

### 2.3 Headless in CI
- Loom 1.19 dev run tasks (`AbstractRunTask`) automatically wrap **client** runs in `xvfb-run -a` when **env `CI` is set, the OS is Linux, and `xvfb-run` is on PATH** (`getUseXvfb()` can override this). GitHub Actions sets `CI=true`. (src: loom `task/AbstractRunTask.java` ~L180, `util/XVFBExistsValueSource.java`)
- Install: `sudo apt-get install -y xvfb`. Fabric API's CI also installs `mesa-vulkan-drivers` and runs a matrix over `preferredGraphicsBackend:"opengl"` and `"vulkan"` via `run/options.txt`.
- 26.x has a Vulkan backend. `PreferredGraphicsApi.DEFAULT` tries GL, then Vulkan; client args include `--graphicsBackend`, `--vulkanValidation`, `--renderDebugLabels`. (src: client `PreferredGraphicsApi.java`, `main/Main.java`)
- Upload `mods/*/build/run/clientGameTest/screenshots/` as an artifact with `if: ${{ !cancelled() }}`. The workflow is in `publishing.md` 5.1.

Production-environment variant (fabric-docs `reference/latest/build.gradle`). It runs the built jar through the real launcher path. On 26.x the mods collection defaults to the `jar` output plus `productionRuntimeMods`:
```groovy
dependencies {
	productionRuntimeMods "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
}
tasks.register('runProductionClientGameTest', net.fabricmc.loom.task.prod.ClientProductionRunTask) {
	jvmArgs.add('-Dfabric.client.gametest')
	useXVFB = true   // default: true only on Linux with env CI set
	// runDir defaults to <project>/run
}
```
The test mod lives in the `gametest` source set, so it is not in the main jar. For a production client gametest, add the gametest jar to `mods` (e.g. `mods.from(tasks.named('gametestJar'))`; the task name is UNVERIFIED). Alternatively, keep production runs for smoke tests and dev runs for gametests.

---

## 3. Unit tests (fabric-loader-junit)

Add to `gradle/fabric-mod.gradle`:
```groovy
dependencies {
	testImplementation "net.fabricmc:fabric-loader-junit:${project.loader_version}"   // 0.20.1 exists; brings junit-jupiter-engine + junit-platform-launcher (runtime)
}
test {
	useJUnitPlatform()                 // already present
	// systemProperty 'fabric.side', 'server'   // default is CLIENT (see below)
}
```
- `fabric-loader-junit` registers a JUnit `LauncherSessionListener` that boots **Knot**, with `fabric.development=true` and `fabric.unitTest=true`.
- Test classes therefore load through Fabric's class loader, with **mixins and class tweakers applied**.
- The side defaults to CLIENT and can be overridden with `-Dfabric.side=server`. (src: fabric-loader `junit/.../FabricLoaderLauncherSessionListener.java`)
- **Mod entrypoints do not run.** The fabric-docs example calls its own `BeanTypes.register()` in `@BeforeAll`.
- It is pinned to JUnit BOM 5.10.0. On Gradle 9 the `junit-platform-launcher` runtime dependency is required, and fabric-loader-junit supplies it.

Registry-dependent classes need a bootstrap. Without it you get `IllegalArgumentException: Not bootstrapped (called from registry ResourceKey[minecraft:root / minecraft:game_event])` from `ExceptionInInitializerError`, the first time something touches `Items`, `Blocks`, or `BuiltInRegistries`.
```java
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

class MyCodecTest {
	@BeforeAll
	static void beforeAll() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		MyModRegistries.register();     // your own static registration, if the test needs it
	}

	@Test
	void stackTemplate() {
		ItemStackTemplate t = new ItemStackTemplate(Items.DIAMOND, 65);   // 26.x immutable stack template
		Assertions.assertTrue(t.is(Items.DIAMOND));
		Assertions.assertEquals(65, t.count());
	}
}
```
(src: fabric-docs `reference/latest/src/test/java/com/example/docs/codec/BeanTypeTest.java`; common `net/minecraft/server/Bootstrap.java`, `SharedConstants.java#tryDetectVersion`)

What is usable after `Bootstrap.bootStrap()`:
- Built-in static registries and their contents (`BuiltInRegistries.*`, `Items`, `Blocks`, `EntityTypes`, `DataComponents`)
- Codecs, `JsonOps`/`NbtOps`, `Component`, `Identifier`, `BlockState` properties, `ItemStackTemplate`, `ItemStack` basics, and math utils.

What is not usable:
- Anything needing a `ServerLevel` or `MinecraftServer`, dynamic registries (biomes, enchantments, damage types, recipes, loot: these load from datapacks), tags (unbound until a server loads them), or client classes that touch rendering or `Minecraft.getInstance()`.
- Use gametests for those.
- `Bootstrap.validate()` also checks translations and attributes; normally skip it.

Gotchas:
- Tests live in `src/test/java`. The `test` source set sees `main`. With `splitEnvironmentSourceSets()` it does **not** see `client` classes unless you add `sourceSets.client.output` to the test classpath (UNVERIFIED whether Loom does this automatically).
- Keep pure logic in plain classes without Minecraft types so most tests need no bootstrap.

---

## 4. Headless dedicated-server smoke test

Goal: boot the server with the mod, reach "Done", then stop. This catches mixin apply failures, registry errors and missing dependencies on the server side, including client-class leaks into common code.

Option A: dev run with stdin. Loom's `runServer` passes `nogui` by default (`RunConfigSettings` "By default nogui is specified"), and `RunGameTask` forwards stdin.
```bash
mkdir -p mods/refmod/run && echo 'eula=true' > mods/refmod/run/eula.txt   # agreeing to https://aka.ms/MinecraftEULA
printf 'stop\n' | timeout 600 ./gradlew :refmod:runServer --console=plain | tee server.log
grep -q 'Done (' server.log && ! grep -qE 'ERROR|Exception|Mixin apply failed' server.log
```
`DedicatedServer` queues console lines (`consoleInput`) and drains them each tick, so a `stop` piped in before startup completes runs on the first tick. "Done (Xs)! For help, type \"help\"" is logged just before that (src: common `server/dedicated/DedicatedServer.java` L165-173, L246, L423-432). Grepping for `ERROR` is noisy; tune the patterns.

Option B: auto-stop entrypoint, the pattern Fabric API's CI uses (`runAutoTestServer` with `-Dfabric.autoTest`). Put it in the gametest source set:
```java
public class AutoStopServer implements DedicatedServerModInitializer {
	@Override public void onInitializeServer() {
		if (System.getProperty("refmod.autoTest") == null) return;
		int[] ticks = {0};
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (++ticks[0] == 50) {
				MixinEnvironment.getCurrentEnvironment().audit();   // force-load all mixin targets: fails on broken mixins
				server.halt(false);
			}
		});
	}
}
```
Register it under `"server"` in `src/gametest/resources/fabric.mod.json`, then:
```groovy
loom { runs { autoTestServer { inherit server; vmArg '-Drefmod.autoTest'; source sourceSets.gametest; runDir 'build/run/autoTest' } } }
```
(src: fapi `fabric-api-base/src/testmod/.../FabricApiAutoTestServer.java`, `build-logic/.../fabric-api.root-testing.gradle`). You still need an `eula.txt` in that run dir: write it in CI (`mkdir -p ... && echo eula=true > .../eula.txt`).

Option C: production server via the Fabric installer (closest to users):
```groovy
tasks.register('runProdServer', net.fabricmc.loom.task.prod.ServerProductionRunTask) {
	installerVersion = '1.1.2'          // required; latest on maven.fabricmc.net
	// loaderVersion / minecraftVersion default to the project's
	runDir = file('build/run/prodServer')
	programArgs.add('nogui')            // UNVERIFIED whether the prod task adds nogui itself
}
// dependencies { productionRuntimeMods "net.fabricmc.fabric-api:fabric-api:${fabric_api_version}" }
```
Then run `printf 'stop\n' | ./gradlew :refmod:runProdServer` after writing `build/run/prodServer/eula.txt`.

Vanilla server CLI (src: common `server/Main.java`):
- `--nogui`
- `--initSettings`: writes `server.properties` and `eula.txt`, then quits.
- `--universe <dir>`, `--world <name>`, `--port <n>`
- `--safeMode`: vanilla datapack only.
- `--forceUpgrade`, `--eraseCache`, `--recreateRegionFiles`, `--bonusChest`, `--demo`
- `--jfrProfile`, `--pidFile <path>`, `--serverId`

The EULA (`eula.txt`, `eula=true`) is required unless `-Dfabric-api.gametest` is set.

Useful `server.properties` for smoke tests:
- `level-type=minecraft:flat`
- `online-mode=false`
- `sync-chunk-writes=false` (much faster on Linux)
- `pause-when-empty-seconds` (default 60)
- `max-tick-time` (default 60000 ms; `ServerWatchdog` crashes the server if one tick exceeds it; `-1` disables it, useful while paused in a debugger)

---

## 5. Logs and crash reports

### 5.1 Log files
- Dev runs use Loom's `log4j2.fabric.xml` (src: loom `src/main/resources/log4j2.fabric.xml`):
  - `logs/latest.log` and the console at `${sys:fabric.log.level:-info}`
  - `logs/debug.log` at `${sys:fabric.log.debug.level:-debug}` (everything at debug and above)
  - Rolled logs become `logs/YYYY-MM-DD-N.log.gz` and `logs/debug-N.log.gz`
  - Logger `net.minecraft` is capped at `fabric.log.level`
- Locations:
  - dev: `mods/<id>/run/logs/`
  - gametests: `mods/<id>/build/run/gameTest/logs/` and `.../clientGameTest/logs/`
  - production: `<instance>/logs/`
- `-Dfabric.log.level=debug` shows debug output on the console and in latest.log for everything.
- Fabric Loader also reads `fabric.log.level` and `fabric.log.file` (src: fabric-loader `impl/util/SystemProperties.java`).
- Per-mod debug logging (fabric-docs `develop/debugging.md`): create `log4j-dev.xml` at the project root and add it with `loom.log4jConfigs.from "log4j-dev.xml"`:
  ```xml
  <Configuration><Loggers>
    <Logger name="refmod" level="debug" additivity="false"><AppenderRef ref="SysOut"/><AppenderRef ref="ServerGuiConsole"/></Logger>
  </Loggers></Configuration>
  ```
  Use `LoggerFactory.getLogger("<modid>")` (SLF4J) so this applies.
- Thread names in log lines tell you the side: `Render thread` is the client and `Server thread` the integrated or dedicated server. Client gametests add a `Test thread`.

### 5.2 Crash reports
- Path: `crash-reports/crash-<yyyy-MM-dd_HH.mm.ss>-server.txt` (server, watchdog) or `-client.txt` (client) under the game dir (src: common `server/MinecraftServer.java` L770, `server/dedicated/ServerWatchdog.java` L62; client `Minecraft.java` L982).
- Anatomy (src: common `CrashReport.java#getFriendlyReport`, `SystemReport.java`):
  1. Header `---- Minecraft Crash Report ----` plus a joke comment.
  2. `Time:`, then `Description:` (e.g. "Ticking entity", "Exception in server tick loop", "Initializing game", "Watching Server").
  3. The **root exception and stack trace**. Read this first and find the first frame in your package. Mixin-injected frames show as `handler$zza000$refmod$onTick` and similar.
  4. "A detailed walkthrough of the error...", then `-- Head --` (Thread and stacktrace), then category blocks such as `-- Entity being ticked --`, `-- Block being ticked --`, `-- Affected level --` (coordinates, entity type).
  5. `-- System Details --`: Minecraft Version / Version ID, OS, Java Version (should be 25), JVM Flags, **Debug Flags** (`-DMC_DEBUG_*`), memory, CPUs, and **`Fabric Mods`** (every loaded mod with version, added by `fabric-crash-report-info-v1`), then server/world details.
- A JVM-level crash (native, GPU driver) produces `hs_err_pid*.log` in the working dir, not a crash report.
- `ServerWatchdog`: "A single server tick took X seconds (should be max Y)", then a crash report with **all thread dumps**. This is how you find deadlocks and infinite loops on the server thread.
- Client force-crash for testing: hold the `key.debug.crash` mapping, F3+C by default. In 26.x debug keys are rebindable `KeyMapping`s in `Options` (`keyDebugCrash`, `keyDebugReloadResourcePacks`, ...), category DEBUG.
- Fabric Loader dependency errors (missing or incompatible mods) are not crash reports. They are a "Incompatible mods found!" GUI or log section with a resolution list.

### 5.3 Common 26.x failure signatures
- `NoSuchFieldError` / `NoSuchMethodError` / `ClassNotFoundException` naming Yarn or intermediary (`class_1234`, `method_5678`, `net.minecraft.entity.player.PlayerEntity`): a 1.21.x mod jar on 26.x, or a dependency built for obfuscated MC.
- `Mixin apply for mod X failed ... InvalidInjectionException: @Inject annotation on Y could not find any targets`: the target signature changed. Check the 26.3 source and the mixin export (6.2).
- `Not bootstrapped`: a unit test that touches registries without `Bootstrap.bootStrap()`.
- `Registry is already frozen` / `Trying to access unbound value`: registering too late (outside `onInitialize`), or reading a tag or holder before a server has loaded data.
- Client-only class loaded on the dedicated server (`ClassNotFoundException: net.minecraft.client.*`): common code references client classes. The smoke test (4) catches this.

---

## 6. Debugging tools

### 6.1 IDE
- Debug (not Run) the `runClient` / `runServer` configs. IntelliJ configs are generated by Loom when `runs.configureEach { ideConfigGenerated = true }` is set (the default for the root project; subprojects need it set). Alternatively, use Gradle run tasks with the debugger attached.
- **Hotswap**: Build Project (Ctrl+F9) while debugging. Standard HotSwap only replaces method bodies.
- With **JetBrains Runtime** as the run JDK plus VM arg `-XX:+AllowEnhancedClassRedefinition`, you can add or remove methods, fields and classes. (src: fabric-docs `develop/getting-started/intellij-idea/launching-the-game.md`)
- Mixin hotswap: add `-javaagent:"<path to sponge-mixin jar from External Libraries>"` to the run config VM args. Mixin method bodies can then be hotswapped.
- Conditional breakpoints on the `Server thread` are fine. A long pause on the server thread trips the watchdog: set `max-tick-time=-1` on dedicated servers. Singleplayer has no watchdog.

### 6.2 Mixin debugging
Run-config JVM args:
- `-Dmixin.debug.export=true` writes transformed classes to `<runDir>/.mixin.out/class/**.class`. Decompile them in IntelliJ to see exactly what your injectors produced. The Stonecutter template adds this to every run config.
- `-Dmixin.debug.verbose=true`: verbose apply logs.
- `-Dmixin.debug=true`: enables all debug options (verbose, export, checks). `-Dmixin.checks.interfaces=true` checks that mixins implement their interfaces fully.
- `-Dmixin.dumpTargetOnFailure=true`: dumps the target class bytecode when an injection fails.
- `MixinEnvironment.getCurrentEnvironment().audit()` in a test force-applies all mixins, so failures surface at test time instead of when a class first loads.

The `mixin.*` property names come from Sponge Mixin's `MixinEnvironment.Option`. They are not in our sources, but they are long-standing (UNVERIFIED against the exact bundled mixin version).

Gradle:
```groovy
loom { runs { configureEach { vmArg '-Dmixin.debug.export=true' } } }
```

### 6.3 Fabric Loader debug properties
There is no single `-Dfabric.debug` switch. The real properties (src: fabric-loader `impl/util/SystemProperties.java`, master):

| Property | Use |
|---|---|
| `fabric.log.level` / `fabric.log.debug.level` | log levels (Loom log4j config + loader) |
| `fabric.debug.disableModIds=a,b` | start without specific mods (bisecting) |
| `fabric.debug.throwDirectly` | don't wrap entrypoint exceptions |
| `fabric.debug.logClassLoad`, `fabric.debug.logClassLoadErrors`, `fabric.debug.logTransformErrors` | class-loading traces |
| `fabric.debug.logLibClassification` | why a jar is treated as lib or mod |
| `fabric.debug.disableClassPathIsolation` | troubleshoot classpath isolation |
| `fabric.debug.disableModShuffle` | deterministic mod order (dev shuffles mods to expose order bugs) |
| `fabric.debug.loadLate=a,b` | load mods late |
| `fabric.debug.replaceVersion=modid:1.2.3` | spoof a version to test dependency ranges |
| `fabric.debug.discoveryTimeout`, `fabric.debug.resolutionTimeout` | slow mod discovery |
| `fabric.addMods=path1:path2` | add extra mod jars or dirs without copying |
| `fabric.development`, `fabric.side`, `fabric.unitTest`, `fabric.noGui` | environment flags |

### 6.4 In-game commands and keys (26.3)
- `/debug start` … `/debug stop`: profiles ticks and writes results under `<gameDir>/debug/`. `/debug function <fn>` traces a function's commands (datapack debugging).
- `/jfr start` … `/jfr stop`: Java Flight Recorder recording with Minecraft's custom events. Writes `debug/<env>-<timestamp>.jfr` plus a `jfr-report-*.json` summary. Registered only if `jdk.jfr` is available. Open the file in JDK Mission Control. Startup option: `--jfrProfile` (server and client). The vanilla jar ships `flightrecorder-config.jfc`.
- `/perf start` / `/perf stop` (dedicated only): 10 s metrics zip to `debug/profiling/`.
- `/tick query | rate <tps> | freeze | step <n> | sprint <time> | unfreeze`: control ticking to reproduce timing bugs.
- `/test ...` (1.6).
- Dev-only commands (`/raid`, `/debugpath`, `/debugmobspawning`, `/warden_spawn_tracker`, `/spawn_armor_trims`, `/serverpack`, `/debugconfig`) require `-DMC_DEBUG_ENABLED -DMC_DEBUG_DEV_COMMANDS`. `SharedConstants.IS_RUNNING_IN_IDE` is never set in 26.3, so they don't appear in Fabric dev by default.
- Debug keys (F3 + key, rebindable under Controls, Debug):
  - F3+T reload resource packs; F3+A reload chunks
  - F3+B hitboxes; F3+G chunk borders; F3+H advanced tooltips
  - F3+I copy recreate command; F3+C copy location (hold for crash)
  - F3+N spectator toggle; F3+F4 game mode switcher; F3+F6 debug options screen
  - F3+L profiling; F3+1/2/3/4 profiler, fps, network and lightmap charts
  - F3+S dump dynamic textures; F3+V dump version info
  - (src: client `Options.java` L686-717: SDL scancodes 23=T, 4=A, 5=B, 10=G, 11=H, 12=I, 6=C, 17=N, 15=L, 22=S, 25=V, 61=F4, 63=F6)
- The F3 overlay content is configurable in 26.x (`client.debugEntries`, F3+F6 screen).

### 6.5 Vanilla `MC_DEBUG_*` flags
- JVM system properties. **`-DMC_DEBUG_ENABLED` is required first**, then e.g. `-DMC_DEBUG_<NAME>`.
- `-DMC_DEBUG_PRINT_PROPERTIES` lists which flags were read.
- Active flags appear in crash reports under "Debug Flags".
- (src: common `SharedConstants.java` L39-123)

Useful ones:

| Flag | Effect |
|---|---|
| `SAVE_STRUCTURES_AS_SNBT` | structure blocks save `.snbt` (gametest structures) |
| `DEV_COMMANDS` | extra commands (6.4) |
| `VERBOSE_COMMAND_ERRORS`, `COMMAND_STACK_TRACES` | full stack traces for command failures |
| `DONT_SAVE_WORLD` | throwaway worlds |
| `VALIDATE_RESOURCE_PATH_CASE` | catch upper-case resource paths (they break on Linux servers) |
| `SHAPES`, `NEIGHBORSUPDATE`, `PATHFINDING`, `GOAL_SELECTOR`, `BRAIN`, `POI`, `BEES`, `RAIDS`, `STRUCTURES`, `GAME_EVENT_LISTENERS`, `BLOCK_BREAK` | debug renderers and info for that system |
| `DUMP_TEXTURE_ATLAS` | dump atlases (missing textures) |
| `SYNCHRONOUS_GL_LOGS` | GL debug logs synchronously |
| `MONITOR_TICK_TIMES` | tick timing |
| `DISABLE_STRUCTURES`, `DISABLE_FEATURES`, `DISABLE_CARVERS`, `DISABLE_ORE_VEINS`, `DISABLE_AQUIFERS`, `DISABLE_SURFACE`, `DISABLE_FLUID_GENERATION`, `ONLY_GENERATE_HALF_THE_WORLD`, `GENERATE_SQUARE_TERRAIN_WITHOUT_NOISE` | worldgen isolation |
| `JFR_PROFILING_ENABLE_LEVEL_LOADING` | include level loading in JFR |

Gradle: `loom { runs { client { vmArg '-DMC_DEBUG_ENABLED'; vmArg '-DMC_DEBUG_DEV_COMMANDS' } } }`.

### 6.6 Profiling
- **spark** (Modrinth `spark`, Fabric build `1.10.187-fabric` supports 26.1–26.3, published 2026-09-19).
  - Commands: `/spark profiler start` / `stop`, `/spark profiler --timeout 30`, `/spark tps`, `/spark health`, `/spark heapsummary`, `/spark gc`. Results upload to spark.lucko.me (an external upload: don't run it on private data without consent).
  - Dev runtime dependency: `runtimeOnly "maven.modrinth:spark:1.10.187-fabric"` via the Modrinth maven already declared. Whether spark's jar works on the dev classpath unremapped is UNVERIFIED; it should work on 26.x because it is unobfuscated.
- **JFR**: `/jfr start|stop`, or the JVM arg `-XX:StartFlightRecording=duration=60s,filename=rec.jfr,settings=profile`.
- **Tracy** (client frame profiler): client args `--tracy` / `--tracyNoImages`. Loom run tasks have a `tracy { tracyCapture = file('tracy-capture'); output = file('profile.tracy'); maxShutdownWaitSeconds = 10 }` block (`ClientProductionRunTask` and `runClientGameTest` in Fabric API).
- **RenderDoc**: Loom has `RenderDocRunTask` / `RenderDocRunUITask` (task names UNVERIFIED; run `./gradlew tasks --all | grep -i renderdoc`).
- Vanilla F3+L profiling, or `/debug start|stop`, for tick-section breakdowns.

### 6.7 Hot reload without restart
- Java method bodies: debugger hotswap (6.1).
- `assets/` (models, textures, lang, shaders): F3+T. Gradle must copy resources first, so in IntelliJ "Build Project" or `./gradlew :refmod:processResources`. The dev run reads from `build/resources/main`, not `src`.
- `data/` (recipes, loot, tags, worldgen JSON, functions, advancements, test environments and instances): `/reload`. Worldgen and dimension changes, and anything in `RegistryDataLoader` (dynamic registries such as biomes, enchantments, damage types, dialogs), are read only at world load. Rejoin the world or restart the server.
- Registered Java content (blocks, items): requires a restart.
- Mixins: restart unless the mixin agent is attached (6.1).

---

## 7. Copy-paste: complete test and debug block for `gradle/fabric-mod.gradle`
Append after the existing `fabricApi { configureTests { ... } }` and `test { ... }` blocks:
```groovy
dependencies {
	testImplementation "net.fabricmc:fabric-loader-junit:${project.loader_version}"
}

loom {
	runs {
		configureEach {
			vmArg '-Dmixin.debug.export=true'
		}
		named('gameTest') {
			vmArg "-Dfabric-api.gametest.report-file=${project.layout.buildDirectory.file('test-results/gametest/TEST-gametest.xml').get().asFile}"
			if (project.hasProperty('gtFilter')) vmArg "-Dfabric-api.gametest.filter=${project.property('gtFilter')}"
		}
		named('clientGameTest') {
			if (project.hasProperty('cgtMods')) vmArg "-Dfabric.client.gametest.modid=${project.property('cgtMods')}"
		}
	}
}

tasks.named('runGameTest') {
	outputs.file(project.layout.buildDirectory.file('test-results/gametest/TEST-gametest.xml'))
}
```
- Usage: `./gradlew :refmod:runGameTest -PgtFilter='refmod-test:*'`.
- Verify after editing: `./gradlew :refmod:tasks --group fabric` should list `runGameTest`, `runClientGameTest`, `deleteGameTestRunDir` and `acceptGameTestEula`.
- Verified in Loom `dev/1.19` `RunConfigSettings`: `vmArg(String)`, `vmArgs(...)`, `property(name[, value])`, `programArg(...)`, `inherit(RunConfigSettings)`, `source(SourceSet|String)`, `runDir(String)`, `name(String)` and `ideConfigGenerated(boolean)`. The Kotlin equivalent is `jvmArguments.add(...)`.

---

## 8. Where to look
- `/tmp/claude-0/mcsrc/fabric-api/fabric-gametest-api-v1/src/{main,testmod}`
- `/tmp/claude-0/mcsrc/fabric-api/fabric-client-gametest-api-v1/src/{client,testmodClient}` (javadoc in `api/client/gametest/v1/**`)
- `/tmp/claude-0/mcsrc/common/net/minecraft/gametest/framework/` (`GameTestHelper`, `TestData`, `TestEnvironmentDefinition`, `TestCommand`, `GameTestMainUtil`)
- `/tmp/claude-0/mcsrc/fabric-docs/develop/automatic-testing.md`, `develop/debugging.md`, `develop/loom/{fabric-api,production-run-tasks}.md`, `reference/latest/{build.gradle,src/gametest,src/test}`
- `/tmp/claude-0/mcsrc/fabric-api/.github/workflows/build.yml` (server_test and client_test jobs) and `build-logic/src/main/groovy/fabric-api.root-testing.gradle`
- Loom: `configuration/fabricapi/FabricApiTesting.java`, `task/AbstractRunTask.java` (xvfb), `task/prod/*ProductionRunTask.java`, `src/main/resources/log4j2.fabric.xml`
