<!-- dte:B4,B1,B10 -->
<!-- covers: build.gradle, settings.gradle, gradle.properties, gradlew, gradlew.bat, gradle/*, gradle/wrapper/*, mods/*/build.gradle, mods/*/gradle.properties -->
# Build

One Gradle build (Gradle 9.8.2 wrapper) for every mod.

`settings.gradle` includes each folder under `mods/` that has a `build.gradle` as a subproject named after the folder.

Root `gradle.properties` holds the toolchain versions every mod shares: Loom, Minecraft, Fabric Loader, Fabric API, Java. A mod may override any of them in its own `gradle.properties`, which must set `mod_id`, `mod_version` and `maven_group`.

Root `build.gradle` declares the Loom plugin version once (`apply false`). Each mod's `build.gradle` applies `net.fabricmc.fabric-loom` and then `gradle/fabric-mod.gradle`, which holds everything shared: split client/common source sets, the dependency on Minecraft, Loader and Fabric API, extra Maven repositories for libraries, `fabric.mod.json` property expansion, the Java toolchain, sources jar, and game tests (`fabricApi.configureTests` with a `gametest` source set).

Mod versions are `<mod_version>+<minecraft_version>`.
