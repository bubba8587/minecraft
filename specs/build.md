<!-- dte:A2,B1,B10,A1 -->
<!-- covers: build.gradle, settings.gradle, gradle.properties, gradlew, gradlew.bat, gradle/*, gradle/wrapper/*, mods/*/build.gradle, mods/*/gradle.properties -->
# Build

## Every mod and pack lives in this one repo under one Gradle build

All mods are subprojects under `mods/`, datapacks live under `datapacks/` and resource packs under `resourcepacks/`, sharing one build, one toolchain version and one set of conventions. The build uses the Gradle 9.8.2 wrapper.

- **`settings.gradle`** includes each folder under `mods/` that has a `build.gradle`, as a subproject named after the folder. A mod whose `gradle.properties` sets `disabled=<reason>` is left out, with a warning (B10 alwaysLatest).
- **Root `gradle.properties`** holds the toolchain versions every mod shares: Loom, Minecraft, Fabric Loader, Fabric API, Java. It also turns on parallel builds, the build cache and the configuration cache.
- **A mod's own `gradle.properties`** must set `mod_id`, `mod_version` and `maven_group`, and may override any toolchain version.
- **Root `build.gradle`** declares the Loom plugin version once (`apply false`).
- **Each mod's `build.gradle`** applies `net.fabricmc.fabric-loom`, then `gradle/fabric-mod.gradle`, which holds everything shared:
  - split client/common source sets;
  - the dependency on Minecraft, Loader and Fabric API;
  - extra Maven repositories for libraries;
  - `fabric.mod.json` property expansion;
  - the Java toolchain and the sources jar;
  - game tests: `fabricApi.configureTests` with a `gametest` source set, the core/milestone suites, and the client test gate (`specs/quality.md` "Test suites").
- **Mod versions** are `<mod_version>+<minecraft_version>`.

## Author bubba8587, Java packages under io.github.bubba8587

The author name is "bubba8587" on Modrinth and in every mod, and Java packages live under `io.github.bubba8587.<modid>`.

## Recipe book unlocks are generated

Every recipe-book recipe (crafting, cooking, smithing, stonecutting) in our mods and datapacks gets a vanilla-style unlock advancement, `data/<ns>/advancement/recipes/<category>/<recipe>.json`: the recipe shows in the book once the player holds any of its ingredients (sticks ignored). `python3 tools/recipe-unlocks.py` writes them; rerun after adding or changing a recipe, `--check` lists missing or stale ones. Don't hand-edit the generated files.
