# Fabric toolchain on 26.x: Loom, fabric.mod.json, Loader API, class tweakers, dev env

Scope: build tooling and low-level mod plumbing for Fabric on unobfuscated Minecraft 26.x. Verified 2026-11-14 against Loom 1.19.4, Gradle 9.8.2, Fabric Loader 0.20.1, Fabric API 0.166.2+26.3, Minecraft 26.3 (Java 25).

Sources used (short names): `loom-jar` = `~/.gradle/caches/modules-2/files-2.1/net.fabricmc/fabric-loom/1.19.4/*.jar` (inspected with javap); `loader-jar` = `.../net.fabricmc/fabric-loader/0.20.1/*.jar`; `ct-jar` = `.../net.fabricmc/class-tweaker/0.3.0`; `fabric-docs` = `/tmp/claude-0/mcsrc/fabric-docs/develop/...`; `fapi` = `/tmp/claude-0/mcsrc/fabric-api/...`; `example-mod` = FabricMC/fabric-example-mod (26.3 branch, checked out at `/tmp/claude-0/ref/fabric-example-mod`); `repo` = this repo (`/home/user/minecraft`).

---

## 1. Current version matrix

| Thing | Version | Where it is pinned |
|---|---|---|
| Minecraft | 26.3 (data pack 121.0, resource pack 97.1, `java_version: 25` in `version.json`) | `gradle.properties: minecraft_version` |
| Fabric Loom | 1.19.4 (maven.fabricmc.net latest release; `1.19-SNAPSHOT` also published) | `gradle.properties: loom_version` |
| Gradle | 9.8.2 | `gradle/wrapper/gradle-wrapper.properties` |
| Fabric Loader | 0.20.1 (bundles Mixin `net.fabricmc:sponge-mixin:0.17.5+mixin.0.8.7`, MixinExtras 0.5.5, ASM 9.10.1) | `loader_version` |
| Fabric API | 0.166.2+26.3 (26.4 snapshot builds exist: `0.166.2+26.4`) | `fabric_api_version` |
| Java | 25 for compile, run and the Gradle daemon JVM | toolchain + `options.release` |

Historic minimums (from the Fabric blog): 26.1 needed Loom 1.15 / Gradle 9.4 / Loader 0.18.4; 26.2 needed Loom 1.17 / Gradle 9.5.1 / Loader 0.19.3; 26.3 was announced with Loom 1.17 / Gradle 9.6 / Loader 0.19.5. Class tweakers need Loader >= 0.18.0 and Loom >= 1.12. Enum extension needs Loader >= 0.19.0 and Loom >= 1.16 (the 26.2 blog says 1.17). The property-based run-config DSL needs Loom >= 1.17.

Check current values at https://fabricmc.net/develop. Maven metadata: `https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml`, `.../fabric-loader/maven-metadata.xml`, `.../net/fabricmc/fabric-api/fabric-api/maven-metadata.xml`.

---

## 2. Loom plugin IDs (src: loom-jar `META-INF/gradle-plugins/*.properties`, fabric-docs `loom/index.md`)

| Plugin id | Implementation class | Use |
|---|---|---|
| `net.fabricmc.fabric-loom` | `LoomNoRemapGradlePlugin` | **Unobfuscated MC (26.1+)**. No mappings, no remapping. |
| `net.fabricmc.fabric-loom-remap` | `LoomRemapGradlePlugin` | Obfuscated MC (1.21.11 and older). The old behaviour with `mappings`, `modImplementation` and `remapJar`. |
| `fabric-loom` | `LoomGradlePlugin` | Legacy id, kept for old obfuscated projects. Applying it after `net.fabricmc.fabric-loom` throws `net.fabricmc.fabric-loom must be applied before fabric-loom`. |
| `net.fabricmc.fabric-loom-companion` | `LoomCompanionGradlePlugin` | For non-Loom subprojects that are part of a mod's classpath group (`loom.mods { sourceSet("main", ":core") }`). |
| `net.fabricmc.fabric-loom-repositories` | `LoomRepositoryPlugin` | Repository-only helper. |

Loom is resolved from the Fabric maven, so `settings.gradle` needs:

```groovy
pluginManagement {
    repositories {
        maven { name = 'Fabric'; url = 'https://maven.fabricmc.net/' }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins { id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0' } // optional: auto-download JDK 25
```

---

## 3. What changed in unobfuscated Loom (1.21.x to 26.x), and how it trips people up

The no-remap plugin throws on any mapping or remap API. These are literal error strings in loom-jar:
- `Cannot use Mojang mappings in a non-obfuscated environment` (`loom.officialMojangMappings()`)
- `Cannot configure layered mappings in a non-obfuscated environment` (`loom.layered {}`, so no Parchment layering)
- `Cannot create remap configurations in a non-obfuscated environment` (`createRemapConfigurations`)
- `Cannot add remap configuration ...`, `Cannot add remapper extension ...`, `Cannot get intermediate mappings provider ...`, `Cannot configure known indyBsms ...`

Porting checklist (fabric-docs `versions/26.1.2/develop/porting/index.md` plus the 26.1 blog):
1. Change the plugin id from `fabric-loom` (or `net.fabricmc.fabric-loom-remap`) to `net.fabricmc.fabric-loom`, in `build.gradle` and in `settings.gradle` if it is declared there.
2. Delete the `mappings ...` dependency line. There is nothing to replace it with: 26.x ships with Mojang names and real parameter and local-variable names.
3. `modImplementation` becomes `implementation`. Likewise `modApi` becomes `api`, `modCompileOnly` becomes `compileOnly`, `modRuntimeOnly` becomes `runtimeOnly`, and `modLocalRuntime` becomes `localRuntime`.
4. `remapJar` becomes `jar`, and `remapSourcesJar` becomes `sourcesJar`. There is no `-dev` classifier any more: `build/libs/<name>-<ver>.jar` is the shipping jar. The jar manifest carries `Fabric-Mapping-Namespace: official`.
5. Change the class tweaker or access widener header namespace from `named` (or `intermediary`) to `official`.
6. Move Java to 25: `options.release = 25` and the toolchain set to 25. IntelliJ must be 2025.3+ (the 26.1 blog says this is required for mixins to work correctly).
7. Mods built for 1.21.11 or older will not load on 26.x, and will not even work as `compileOnly`. Every dependency must have a 26.x build.
8. Depend on `fabric-api`, not `fabric`. The `fabric` mod id provide was removed in 26.1.
9. You no longer need a mixin refmap. Remove the `"refmap"` key from mixin configs and any `loom.mixin.defaultRefmapName` setting. Built jars contain no refmap: neither the refmod jar nor fapi's `fabric-item-api-v1` jar has one.

Migrating Yarn source to Mojang names had to happen on the old version, before bumping MC. On 1.21.10 or 1.21.11 with the remap plugin (Loom 1.13+):

```sh
./gradlew migrateMappings --mappings "net.minecraft:mappings:1.21.11" --overrideInputsIHaveABackup
./gradlew migrateClientMappings --mappings "net.minecraft:mappings:1.21.11"       # split client source set
./gradlew migrateClassTweakerMappings --mappings "net.minecraft:mappings:1.21.11" # AW/CT files
```

The alternative is the Ravel IntelliJ plugin, which also handles Kotlin. Neither tool renames Fabric API symbols; see `porting-26x.md`. Mixin target strings always need a manual review afterwards. (src: fabric-docs `versions/1.21.10/develop/porting/mappings/loom.md`)

---

## 4. Canonical 26.3 build files

`gradle.properties` (repo root):

```properties
org.gradle.jvmargs=-Xmx3G
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true   # example-mod and fapi enable it; Loom 1.19 supports it
loom_version=1.19.4
minecraft_version=26.3
loader_version=0.20.1
fabric_api_version=0.166.2+26.3
```

`build.gradle` (single-project form; src: example-mod `build.gradle`, repo `gradle/fabric-mod.gradle`):

```groovy
plugins {
    id 'net.fabricmc.fabric-loom' version "${loom_version}"
    id 'maven-publish'
}

base { archivesName = project.mod_id }   // Gradle 9: archivesBaseName is gone
version = "${project.mod_version}+${project.minecraft_version}"
group = project.maven_group

repositories {
    // Loom auto-adds Fabric maven, Mojang libraries and Maven Central.
    maven { name = 'Modrinth'; url = 'https://api.modrinth.com/maven'; content { includeGroup 'maven.modrinth' } }
    maven { url = 'https://maven.terraformersmc.com/' }   // ModMenu, etc.
}

loom {
    splitEnvironmentSourceSets()
    mods {
        "${project.mod_id}" {
            sourceSet sourceSets.main
            sourceSet sourceSets.client
        }
    }
    // accessWidenerPath = file("src/main/resources/${project.mod_id}.classtweaker")
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    implementation "net.fabricmc:fabric-loader:${project.loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
}

processResources {
    def props = [version: project.version, minecraft_version: project.minecraft_version, loader_version: project.loader_version]
    inputs.properties props                       // config-cache safe: capture values, not `project`
    filesMatching('fabric.mod.json') { expand props }
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
    withSourcesJar()
}
tasks.withType(JavaCompile).configureEach { options.release = 25; options.encoding = 'UTF-8' }

jar {
    def name = project.mod_id                     // capture outside the closure (config cache)
    inputs.property 'mod_id', name
    from('LICENSE') { rename { "${it}_${name}" } }
}
```

This repo's layout: the root `build.gradle` declares the plugin `apply false`. `settings.gradle` auto-includes every `mods/<name>/` that has a `build.gradle`. Each mod applies `id 'net.fabricmc.fabric-loom'` plus `apply from: rootProject.file('gradle/fabric-mod.gradle')`, and sets `mod_id`, `mod_version` and `maven_group` in its own `gradle.properties`. A build log shows tasks `compileJava, compileClientJava, processIncludeJars, jar, sourcesJar, validateAccessWidener`.

### Gradle 9 notes
- The Gradle daemon itself must run on Java 25 for 26.x (26.1 blog: "Minecraft 26.1 requires Java 25 minimum for the Gradle JVM"). Set it in your IDE's Gradle JVM setting or with `org.gradle.java.home`.
- APIs removed or deprecated in Gradle 9 that old Fabric templates used: `archivesBaseName` (use `base.archivesName`), and project-level `sourceCompatibility` (use the `java {}` extension or toolchains). `project` must not be used inside task actions when configuration cache is on. That is why the templates capture `def name = project.mod_id` and declare `inputs.property(...)`.
- Upgrade with `./gradlew wrapper --gradle-version latest`.
- Configuration cache: Loom 1.19 is compatible, and both fapi (`org.gradle.configuration-cache=true`) and example-mod enable it. If a third-party plugin breaks, add `--no-configuration-cache` for that invocation.

---

## 5. Dependency configurations (no-remap Loom)

Configuration constants come from loom-jar `Constants$Configurations`.

| Configuration | Meaning |
|---|---|
| `minecraft` | `"com.mojang:minecraft:26.3"`. Required. |
| `implementation` / `api` / `compileOnly` / `runtimeOnly` | Plain Gradle. Use these for mods too; nothing is remapped. |
| `localRuntime` | Like `runtimeOnly`, but not exposed to dependents or published. Use it for dev-only mods (ModMenu and the like). |
| `include` | Jar-in-jar. Non-transitive. In no-remap Loom the nested jars are added to `jar` (task `processIncludeJars`), not to `remapJar`. A plain library jar (one without `fabric.mod.json`) is auto-wrapped in a generated mod jar whose id derives from the coordinates. Other jar tasks (`shadowJar`) do not get included jars. |
| `productionRuntimeMods` | Mods put on the classpath of `ClientProductionRunTask` / `ServerProductionRunTask`. |
| `annotationProcessor` | Plain Gradle. |
| `mappings`, `mod*` | Remap plugin only. They throw on the no-remap plugin. |

Common patterns:

```groovy
dependencies {
    // A single Fabric API module instead of the whole aggregate:
    implementation fabricApi.module("fabric-networking-api-v1", project.fabric_api_version)

    // Mod from Modrinth maven: maven.modrinth:<slug-or-project-id>:<version-number-or-version-id>
    localRuntime "maven.modrinth:modmenu:<version>"
    compileOnly  "maven.modrinth:sodium:<version>"

    // Library shaded as jar-in-jar AND on the compile classpath:
    include(implementation("com.example:somelib:1.2.3"))

    // Depend on another Loom subproject (no namedElements needed on no-remap Loom):
    implementation project(":core")
    clientImplementation project(":core").sourceSets.client.output   // with split sources
}
```

- `fabricApi.module(name, fapiVersion)` returns the module dependency, and `fabricApi.moduleVersion(name, fapiVersion)` returns just its version string (src: loom-jar `FabricApiExtension`).
- Fabric API itself ships as one aggregate `fabric-api` artifact (POM lists 47 modules, including `fabric-api-deprecated`) plus `fabric-api-bom` and `fabric-api-catalog` (src: fapi dirs).

---

## 6. `loom {}` DSL reference (verified against loom-jar `LoomGradleExtensionAPI`)

```groovy
loom {
    accessWidenerPath = file("src/main/resources/example-mod.classtweaker") // class tweaker OR access widener
    fabricModJsonPath = file("src/main/resources/fabric.mod.json")         // for interface-injection processing
    log4jConfigs.from(file("log4j-dev.xml"))
    enableTransitiveAccessWideners = true          // apply transitive-* entries from dependencies (default true)
    enableModProvidedJavadoc = true
    runtimeOnlyLog4j = false                       // true = log4j only at runtime, forcing SLF4J in code
    runtimeOnlyLwjglGraphics = false               // Property<Boolean>, exists in 1.19.4
    splitModDependencies = true                    // with split source sets, split deps by env (UNVERIFIED default)
    uncompressNestedJars = true                    // UNVERIFIED semantics/default

    splitEnvironmentSourceSets()                   // creates `client` source set; MC split into common + clientOnly jars
    serverOnlyMinecraftJar() / clientOnlyMinecraftJar() / splitMinecraftJar()

    interfaceInjection {
        enableDependencyInterfaceInjection = true  // apply deps' inject-interface entries (FAPI relies on this)
        // isEnabled, interfaceInjectionSourceSets also exist
    }

    mods {
        "example-mod" { sourceSet sourceSets.main; sourceSet sourceSets.client }
        "example-mod-test" { sourceSet sourceSets.gametest }   // separate classpath group
        // sourceSet("main", ":core")  configuration configurations.shade  dependency(...)
    }

    decompilers {                                   // vineflower is the default; cfr, fernflower also built-in
        vineflower { memory = 4096; maxThreads = 8; options += [key: "value"] }
    }

    mixin {                                         // mostly irrelevant without remapping
        useLegacyMixinAp = false                    // default false since Loom 1.12
        // defaultRefmapName, refmapTargetNamespace, messages, showMessageTypes, add(sourceSet, refmapName)
    }

    runs { ... }                                    // see section 7
}
```

`loom.nestJars(jarTaskProvider, fileCollectionOrConfiguration)` nests jars into an arbitrary `Jar` task. Other members: `getNamedMinecraftJars()` (the MC jars on the classpath), `getMinecraftVersion()`, `addMinecraftJarProcessor(Class, args...)` (custom jar processors), `customMinecraftMetadata` and `versionsManifests {}` (custom or experimental version manifests), `disableDeprecatedPomGeneration(publication)`.

The docs' `remapArchives`, `setupRemappedVariants`, `remapJar {...}`, `modImplementation` and `mappings loom.officialMojangMappings()` examples (fabric-docs `loom/options.md`) apply to **remap Loom only**.

Gradle properties understood by Loom (`Constants$Properties`): `fabric.loom.disableObfuscation`, `fabric.loom.dontRemap`, `fabric.loom.disableRemappedVariants`, `fabric.loom.decompileCacheMaxFiles`, `fabric.loom.decompileCacheMaxAge`, `fabric.loom.enableMinecraftVerification`, `fabric.loom.renderdoc.enabled` (registers `runClientRenderDoc`), `fabric.loom.runtimeJavaCompatibilityVersion`, `fabric.loom.experimental.sandbox`.

---

## 7. Source sets, run configs, tasks

### Split environment source sets
`splitEnvironmentSourceSets()` creates `src/client/java` and `src/client/resources`. `main` compiles against the **common** MC jar only, so any reference from `main` to `net.minecraft.client.*` (`Minecraft`, `Screen`, renderers) is a compile error. `client` sees both. Both are packaged into one jar. The manifest gets `Fabric-Loom-Split-Environment: true` and `Fabric-Loom-Client-Only-Entries: <list of client classes and files>` (src: built `mods/refmod/build/libs/refmod-0.1.0+26.3.jar`). Put client entrypoints and client mixin configs in `src/client`. `genSources` produces `genCommonSourcesWithVineflower` and `genClientOnlySourcesWithVineflower` (src: `/tmp/claude-0/gensrc.log`).

Always declare both source sets in `loom.mods { "<modid>" { sourceSet main; sourceSet client } }`. Otherwise Loader sees two classpath entries as different "mods" in dev, and resources or mixins from one half go missing (fabric-docs `loom/classpath-groups.md`).

### Run configurations
Default runs are `client` and `server`, giving the tasks `runClient` and `runServer`. All run in `run/` with `-Dfabric.development=true`. IDE run configs are generated when `.idea` exists (task `genEclipseRuns`; `vscode` gives `genVsCodeProject`). Fabric API DSL adds more:

```groovy
fabricApi {
    configureDataGeneration {       // -> run config "datagen" -> task runDatagen
        client = true               // run datagen with the client (needed for model/blockstate providers using client classes)
        outputDirectory = file("src/main/generated")   // default
        addToResources = true       // default: generated dir added to main resources
        createSourceSet = false     // true -> separate `datagen` source set; then set modId = "example-datagen"
        strictValidation = false    // -Dfabric-api.datagen.strict-validation
        createRunConfiguration = true
    }
    configureTests {                // -> runGameTest (server, run dir build/run/gameTest)
        createSourceSet = true      //    runClientGameTest (client, build/run/clientGameTest)
        modId = "example-mod-test"  // required when createSourceSet = true (source set `gametest`)
        enableGameTests = true
        enableClientGameTests = true
        eula = true                 // writes eula.txt (task acceptGameTestEula)
        clearRunDirectory = true    // task deleteGameTestRunDir
        username = "Player0"
    }
}
```

Datagen passes `-Dfabric-api.datagen`, `-Dfabric-api.datagen.output-dir=...` and `-Dfabric-api.datagen.modid=...`. Server gametests use `-Dfabric-api.gametest`. Client gametests use `-Dfabric.client.gametest` (and `fabric.client.gametest.testModResourcesPath`). (src: loom-jar `configuration/fabricapi/*` strings)

### Datagen (fabric-data-generation-api-v1; verified in `mods/refmod`)
There is no separate datagen note; this is the working 26.3 recipe (src: fapi fabric-data-generation-api-v1 testmod `DataGeneratorTestEntrypoint.java`; refmod `src/client/java/.../datagen/RefModDataGenerator.java`).
- Per-mod `build.gradle`: `fabricApi { configureDataGeneration { client = true } }` (a second `fabricApi {}` block after the shared one is fine). `client = true` is needed because `FabricModelProvider` (package `net.fabricmc.fabric.api.client.datagen.v1.provider`) and `BlockModelGenerators`/`ItemModelGenerators` (`net.minecraft.client.data.models`) are client classes, so with split source sets the entrypoint class lives in `src/client/java`.
- `fabric.mod.json`: `"fabric-datagen": ["<fqcn of your DataGeneratorEntrypoint>"]`. In `onInitializeDataGenerator(FabricDataGenerator g)`: `var pack = g.createPack(); pack.addProvider(MyRecipes::new); ...`.
- Provider constructors: `FabricRecipeProvider`, `FabricBlockLootSubProvider`, `FabricLanguageProvider`, `FabricTagsProvider.ItemTagsProvider` take `(FabricPackOutput, CompletableFuture<HolderLookup.Provider>)` (`ItemTagsProvider` has a 3-arg overload with a `BlockTagsProvider` for `copy(...)`); `FabricModelProvider` takes `(FabricPackOutput)` only. Methods: recipes `createRecipeProvider(...)` (content.md 7.4); loot `generate()` with `dropSelf(block)`; tags `addTags(registries)` with `builder(TagKey).add(ResourceKey)` (keys, not objects: `item.builtInRegistryHolder().key()`); lang `generateTranslations(registries, b)` with `b.add(Item|Block|EntityType|ResourceKey<CreativeModeTab>|TagKey|String key, value)`; models `generateBlockStateModels(BlockModelGenerators)` (`createTrivialCube(block)`, or for a boolean property copy vanilla `createRedstoneLamp`: `BlockModelGenerators.plainVariant(TexturedModel.CUBE.create(block, gen.modelOutput))`, `gen.createSuffixedVariant(block, "_on", ModelTemplates.CUBE_ALL, TextureMapping::cube)`, `gen.blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(BlockModelGenerators.createBooleanModelDispatch(PROP, on, off)))`; these are access-widened by Fabric) and `generateItemModels(ItemModelGenerators)` (`generateFlatItem(item, ModelTemplates.FLAT_ITEM | FLAT_HANDHELD_ITEM)`). `MultiVariant` is `net.minecraft.client.data.models.MultiVariant`. Block items get an `items/<id>.json` pointing at the block model without extra code.
- `./gradlew :<mod>:runDatagen` writes `src/main/generated/{assets,data}` plus a `.cache/` folder; recipes also emit `data/<ns>/advancement/recipes/<category>/<id>.json`. Textures are not generated: ship `textures/block|item/*.png` yourself.

Custom run:

```groovy
loom.runs {
    client {
        jvmArguments.add("-Dmixin.debug.export=true")
        systemProperties.put("mixin.debug.verbose", "true")
        programArguments.addAll("--username", "Dev")
        environmentVars.put("FOO", "1")
    }
    secondClient {
        inherit client
        displayName = "Minecraft Client 2"
        runDirectory = file("run2")
        sourceSet = sourceSets.client.name
    }
    remove server
}
loom.runs.configureEach { generateRunConfig = true }   // for subprojects (default: root project only)
```

The full property list (loom-jar `RunConfiguration`): `displayName, jvmArguments, programArguments, environmentVars, systemProperties, runtimeEnvironment ("client"|"server"), appendProjectPathToDisplayName, mainClass, sourceSet, runDirectory, generateRunConfig, preferGradleTask, ideConfigFolder, devLaunchMainClass`. Methods: `inherit(other)`, `client()`, `server()`, `serverWithGui()`, `task { RunGameTask -> }`. The old methods `vmArg`, `programArg`, `property`, `runDir`, `source`, `name`, `ideConfigGenerated` and `environment` still exist on `RunConfigSettings` but are deprecated. The property API exists since Loom 1.17.

`RunGameTask`/`AbstractRunTask` extends `JavaExec` and adds `useXvfb`, `microsoftAuthenticationEnabled`, `offline` (CLI `--offline`), `profile` (CLI `--profile=<name>`) and `tracy {}`.

### Built-in Microsoft login (replaces DevAuth in most cases)
Loom 1.19 has the tasks `microsoftLogin` ("Logs in to Minecraft with a Microsoft account", device-code flow) and `microsoftLogout`. After logging in, `runClient` launches with your real account ("Using Microsoft account {} to launch Minecraft"). The token is stored encrypted in platform secure storage. Use `./gradlew runClient --offline` to skip it, or `--profile=<name>` for named accounts. On platforms without secure storage the stored account gets deleted. (src: loom-jar `task/launch/auth/*`, `AbstractRunTask` strings.) Exact persistence location: UNVERIFIED. An alternative third-party mod is "DevAuth Neo" (Modrinth slug `dev-auth-neo`, lists 26.3) as a `localRuntime` dependency. Its config is UNVERIFIED.

### Other tasks
- `genSources`: decompiles MC with Vineflower and attaches the sources in the IDE. Class tweaker and interface-injection changes need a Gradle refresh plus `genSources` to show up. Injected interfaces appear in the decompiled source, e.g. `public static class Properties implements net.fabricmc.fabric.api.item.v1.FabricItem.Properties` in `Item.java`.
- `downloadAssets`, `extractNatives`, `validateAccessWidener` (part of `check`), `migrateMappings`, `migrateClassTweakerMappings`, `exportClasspath`.
- Registrable task classes: `net.fabricmc.loom.task.FabricModJsonV1Task` (generate fabric.mod.json from a `json {}` block), `net.fabricmc.loom.task.DownloadTask` (`url`, `output`, `sha1`, `maxAge`), `net.fabricmc.loom.task.ValidateMixinNameTask` (checks the mixin class name matches its target; `source(sourceSets.main.output)`), `net.fabricmc.loom.task.tool.ModEnigmaTask`, `net.fabricmc.loom.task.prod.ClientProductionRunTask` / `ServerProductionRunTask` (`mods.from(...)`, `jvmArgs`, `programArgs`, `runDir`, `javaLauncher`, `useXVFB`, `tracy {}`; the server task also takes `installerVersion` (required), `loaderVersion` and `minecraftVersion`).

```groovy
tasks.register("runProductionClientGameTest", net.fabricmc.loom.task.prod.ClientProductionRunTask) {
    jvmArgs.add("-Dfabric.client.gametest")
    useXVFB = true       // default true on Linux when CI env var set; needs `apt install xvfb`
}
dependencies { productionRuntimeMods "net.fabricmc.fabric-api:fabric-api:${fabric_api_version}" }
```

### Caches and fixing them
- `~/.gradle/caches/fabric-loom` holds MC jars, assets and decompile caches for all projects. `.gradle/loom-cache` in the root project holds nested jars. `build/loom-cache` is per project.
- Corrupt cache: `./gradlew --refresh-dependencies` (or delete `~/.gradle/caches/fabric-loom/<mcversion>`).

---

## 7.5 CI

The example-mod workflow (`.github/workflows/build.yml`) runs `actions/checkout@v6`, `gradle/actions/wrapper-validation@v6`, then `actions/setup-java@v5` with `java-version: '25'` and `distribution: 'microsoft'`, then `./gradlew build`, then uploads `build/libs/`.

---

## 8. `fabric.mod.json` (schemaVersion 1)

Keys accepted by the 0.20.1 parser (src: loader-jar `V1ModMetadataParser` string constants): `schemaVersion, id, version, provides, environment, entrypoints, jars, mixins, accessWidener, depends, recommends, suggests, conflicts, breaks, requires (legacy, ignored with a warning), name, description, authors, contributors, contact, license, icon, languageAdapters, custom`. Any other root key logs `Unsupported root entry`. There is **no** `classTweaker` key: the class tweaker file goes in `accessWidener`.

```jsonc
{
  "schemaVersion": 1,                    // must be 1; put it first
  "id": "example-mod",                   // regex [a-z][a-z0-9-_]{1,63}: lowercase only, 2-64 chars
  "version": "${version}",               // non-empty; ideally SemVer-ish, else plain-string comparison
  "provides": ["example_mod_legacy_id"], // aliases; other mods with these ids get rejected
  "environment": "*",                    // "*" | "client" | "server" (physical side)
  "entrypoints": {
    "main":   ["com.example.ExampleMod"],                        // ModInitializer
    "client": ["com.example.client.ExampleModClient"],           // ClientModInitializer (after main, physical client)
    "server": ["com.example.server.ExampleModServer"],           // DedicatedServerModInitializer (after main, dedicated only)
    "preLaunch": ["com.example.EarlyRiser"],                     // PreLaunchEntrypoint: before MC classes load; avoid
    "fabric-datagen": ["com.example.datagen.ExampleDataGen"],    // DataGeneratorEntrypoint
    "fabric-gametest": ["com.example.test.ExampleTests"],        // any class with @net.fabricmc.fabric.api.gametest.v1.GameTest methods
    "fabric-client-gametest": ["com.example.test.ClientTests"],  // FabricClientGameTest
    "modmenu": ["com.example.client.ModMenuIntegration"],        // third-party entrypoints work the same way
    "kotlinthing": [{ "adapter": "kotlin", "value": "com.example.Thing" }],  // object form
    "other": ["com.example.Foo::bar", "com.example.Foo::FIELD"]  // static method / static field references
  },
  "jars": [{ "file": "META-INF/jars/somelib-1.2.3.jar" }],      // filled by Loom `include`; don't hand-write
  "languageAdapters": { "kotlin": "net.fabricmc.language.kotlin.KotlinAdapter" },
  "mixins": [
    "example-mod.mixins.json",
    { "config": "example-mod.client.mixins.json", "environment": "client" }
  ],
  "accessWidener": "example-mod.classtweaker",                  // class tweaker or access widener, one file
  "depends":    { "fabricloader": ">=0.20.1", "minecraft": "~26.3", "java": ">=25", "fabric-api": "*" },
  "recommends": { "modmenu": "*" },        // missing -> warning
  "suggests":   { "emi": "*" },            // metadata only
  "breaks":     { "badmod": "<2.0" },      // present -> hard crash
  "conflicts":  { "quirkymod": "*" },      // present -> warning
  "name": "Example Mod",
  "description": "...",
  "authors": ["Me", { "name": "Tiny Potato", "contact": { "homepage": "https://..." } }],
  "contributors": [],
  "contact": { "homepage": "https://...", "sources": "https://...", "issues": "https://...", "email": "a@b.c", "irc": "irc://...", "discord": "https://..." },
  "license": "MIT",                        // string or array; SPDX ids recommended
  "icon": "assets/example-mod/icon.png",   // or { "16": "...", "128": "..." }; square PNG
  "custom": {
    "modmenu": { "badges": ["library"] },
    "fabric-api:module-lifecycle": "stable"
    // "loom:injected_interfaces": {...} is the legacy way; prefer inject-interface in a class tweaker
  }
}
```

Built-in dependency ids: `minecraft`, `java`, `fabricloader`, `mixinextras` (nested in Loader; also provides `com_github_llamalad7_mixinextras`). On mod ids: Loader rejects an uppercase character, a leading non-letter, or a single-character id (error strings in loader-jar `MetadataVerifier`). Docs that say "ASCII letters" are wrong about uppercase.

### Version ranges (fabric-docs `loader/fabric-mod-json.md`)
- A value is a string or an array of strings (OR). Inside one string, space-separated comparators are ANDed: `">=26.1 <26.2"`.
- Operators: `*`, `=` (or a bare version), `>`, `>=`, `<`, `<=`, `~X.Y` (same minor: `~26.1-rc.2` equals `>=26.1-rc.2 <26.2-`), `^X` (same major), and `X.Y.x` (or `.X`/`.*`) wildcards. `26.1.x` equals `~26.1-`.
- Shorter versions are padded with zeros (`26.1` == `26.1.0`). Anything after `+` is ignored, so `0.166.2+26.3` == `0.166.2+26.4` for comparison purposes. **Never use the FAPI version to pin an MC version.**
- Pre-releases: append `-` to include them, e.g. `>26.2- <26.2` matches only 26.2 snapshots. MC version normalisation: `26.1` becomes `26.1.0`, `26.1-snapshot-1` becomes `26.1-alpha.1`, and week snapshots `26w14a` become `26.1.1-alpha.26.14.a`. RCs and pres become `-rc.N` / `-beta.N`.
- Non-SemVer versions are compared as strings, and then `<`/`>` are not allowed.
- Recommended for a 26.3 mod: `"minecraft": "~26.3"`, or `">=26.3 <26.4-"` to exclude snapshots.
- The test tool is https://dexman545.github.io/outlet-database/floaderValidator.
- Users can override dependencies without editing jars via `config/fabric_loader_dependencies.json` (`{"version":1,"overrides":{...}}`; src: loader-jar `DependencyOverrides`). There is also the system property `-Dfabric.debug.replaceVersion=modid:1.0.0`.

**The `fabricloader` version you depend on also changes Mixin behaviour.** Loader maps your minimum `fabricloader` dependency to a Mixin compatibility level (loader-jar `FabricMixinVersions`):

| `fabricloader` minimum | Mixin compat |
|---|---|
| >=0.20.0 | 0.17.5 |
| >=0.19.4 | 0.17.4 |
| >=0.19.0 | 0.17.1 (needed for enum extension) |
| >=0.18.4 | 0.17.0 |
| >=0.17.3 | 0.16.5 |
| >=0.16.0 | 0.14.0 |
| older | 0.10.0 legacy |

The docs state that enum extension needs an explicit `"fabricloader": ">=0.19.0"`.

---

## 9. Fabric Loader API (src: loader-jar `net/fabricmc/loader/api/*`, javap)

```java
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.EnvType;

FabricLoader fl = FabricLoader.getInstance();
fl.isModLoaded("sodium");                         // boolean; also true for `provides` aliases
fl.getModContainer("example-mod")                  // Optional<ModContainer>
  .map(c -> c.getMetadata().getVersion().getFriendlyString());
fl.getAllMods();                                   // Collection<ModContainer>
fl.getConfigDir();                                 // Path: <gameDir>/config   (getConfigDirectory(): File, legacy)
fl.getGameDir();                                   // Path                      (getGameDirectory(): File, legacy)
fl.isDevelopmentEnvironment();                     // true when -Dfabric.development=true (Loom runs)
fl.getEnvironmentType() == EnvType.CLIENT;         // PHYSICAL side: CLIENT or SERVER
fl.getRawGameVersion();                            // "26.3"
fl.getGameInstance();                              // MinecraftServer (dedicated) or Minecraft (client); avoid
fl.getLaunchArguments(boolean sanitize);
fl.getObjectShare();                               // cross-mod key/value: get/put/putIfAbsent/remove/whenAvailable("modid:key", (k,v)->{})
fl.getMappingResolver();                           // useless on 26.x: runtime namespace is "official" == Mojang names

// Custom entrypoints
List<MyApi> apis = fl.getEntrypoints("example-mod:api", MyApi.class);
for (EntrypointContainer<MyApi> c : fl.getEntrypointContainers("example-mod:api", MyApi.class)) {
    c.getProvider().getMetadata().getId(); c.getEntrypoint(); c.getDefinition();
}
fl.invokeEntrypoints("example-mod:api", MyApi.class, api -> api.register(registry));
```

- `ModContainer`: `getMetadata()`, `getRootPaths()`, `findPath("assets/x/y.json")` returning `Optional<Path>` (the safe way to read your own jar's files), `getOrigin()`, `getContainingMod()`, `getContainedMods()`. `getRootPath()`/`getPath()` are deprecated.
- `ModMetadata`: `getId, getVersion, getName, getDescription, getAuthors, getContributors, getContact, getLicense, getIconPath(size), getEnvironment, getProvides, getDependencies, getDepends/getRecommends/getSuggests/getConflicts/getBreaks, getCustomValue(key)` (returns a `CustomValue` tree: `getAsObject/getAsArray/getAsString/getAsNumber/getAsBoolean`), `containsCustomValue`.
- `Version.parse(String)` and `SemanticVersion.parse(String)`, `VersionPredicate.parse(">=1.2")`, for version checks against other mods.
- Initializer interfaces: `net.fabricmc.api.ModInitializer#onInitialize()`, `ClientModInitializer#onInitializeClient()`, `DedicatedServerModInitializer#onInitializeServer()`, `net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint#onPreLaunch()`.
- `@Environment(EnvType.CLIENT)` / `@EnvironmentInterface` (`net.fabricmc.api`) are still present. Loader strips annotated members on the wrong side unless `-Dfabric.disableEnvironmentStrip=true` is set. With split source sets you should rarely need them.

**Physical vs logical side:** `getEnvironmentType()` reports the physical side. In singleplayer, the integrated server runs inside the CLIENT environment, so server logic must check `level.isClientSide()` (verified in `common net/minecraft/world/level/Level.java`) or run in server events. It must never branch on EnvType.

### Loader system properties (loader-jar `SystemProperties`)
`fabric.development`, `fabric.side`, `fabric.modsFolder`, `fabric.addMods` (extra mod paths), `fabric.gameVersion`, `fabric.log.level`, `fabric.log.file`, `fabric.classPathGroups` (set by Loom from `loom.mods`), `fabric.disableEnvironmentStrip`, `fabric.debug.disableModIds`, `fabric.debug.logClassLoad`, `fabric.debug.logClassLoadErrors`, `fabric.debug.logTransformErrors`, `fabric.debug.throwDirectly`, `fabric.debug.disableClassPathIsolation`, `fabric.debug.disableModShuffle`, `fabric.debug.loadLate`, `fabric.debug.replaceVersion`, `fabric.debug.discoveryTimeout`, `fabric.debug.resolutionTimeout`, `fabric.loader.useCompatibilityClassLoader`, `fabric.noGui` (no Swing error window), `fabric.unitTest`.

---

## 10. Class tweakers (successor of access wideners)

src: fabric-docs `class-tweakers/*.md`; ct-jar `ClassTweakerReaderImpl`; fapi `*/src/main/resources/*.classtweaker`.

- File: `src/main/resources/<modid>.classtweaker` (fapi uses `.classtweaker`; `.accesswidener` still works).
- Register it in **both** places: `loom { accessWidenerPath = file(...) }` and `"accessWidener": "<modid>.classtweaker"` in fabric.mod.json. One file per mod.
- Header, first line, no leading whitespace (`Leading whitespace is not allowed`), and the namespace must equal the runtime namespace, `official` (`Namespace (%s) does not match current runtime namespace (%s)`):
  ```
  classTweaker v1 official      # access + inject-interface
  classTweaker v2 official      # also extend-enum
  accessWidener v2 official     # legacy header, still accepted
  ```
- Separate tokens with any whitespace (tabs or spaces). `#` starts a comment, including at the end of a line. Class names use internal form `a/b/C` with `$` for inner classes; dotted names are an error.

Directives (prefix any of them with `transitive-` to expose the change to mods that depend on yours; this only matters for libraries):

```
accessible   class   net/minecraft/world/inventory/MenuType$MenuSupplier
accessible   method  net/minecraft/world/entity/ai/sensing/SensorType  <init>  (Ljava/util/function/Supplier;)V
accessible   field   net/minecraft/world/damagesource/DamageSources  damageTypes  Lnet/minecraft/core/Registry;
extendable   class   <cls>                 # public + non-final
extendable   method  <cls> <name> <desc>   # non-final (+protected if private)
mutable      field   <cls> <name> <desc>   # strip final
inject-interface  net/minecraft/world/level/material/FlowingFluid  com/example/BucketEmptySoundGetter
inject-interface  net/minecraft/world/level/material/FlowingFluid  com/example/GenericInterface<+Ljava/lang/String;[Ljava/lang/Boolean;>
extend-enum  net/minecraft/world/inventory/RecipeBookType  EXAMPLE_MOD_RECIPE_BOOK_TYPE      # needs v2 header
transitive-accessible field net/minecraft/world/level/storage/loot/LootPool entries Ljava/util/List;
```

Semantics:
- `accessible` on a class or field makes it public. On a method it makes it public, and final if the method was private. Making a member accessible also makes its class accessible.
- `extendable` on a class makes it public and non-final. On a method it makes it non-final, and protected if it was private.
- `mutable` is for fields only.
- Use class tweakers when you need to: reference a private, protected or package class type; subclass a final class; override a final method; or make an injected interface or added enum constant visible at compile time. Use a Mixin `@Accessor`/`@Invoker` for plain field and method access instead: those need no Gradle refresh and also work on other mods' classes.
- **Class tweakers can only target vanilla Minecraft classes.** They do not apply to other mods.
- Unknown targets are silently ignored at runtime. Run `./gradlew validateAccessWidener` (it is part of `check`) to catch typos.
- After editing, refresh Gradle and re-run `genSources` to see the changes in the decompiled source.
- Inject-interface rules: every interface method must be `default`, usually throwing `UnsupportedOperationException`, then implemented by a mixin on the target. Prefix the method names with your modid (`example_mod$getFoo`). The interface must not live in the mixin package. Generic signatures go in `<...>` using JVM signature syntax.
- Enum extension: write an `enum` annotated `@Mixin(Target.class)` in the mixin package whose constants are the new entries. Use `@Shadow` on a constructor to pass arguments, and `MixinIntrinsics.currentEnumOrdinal()` (from `org.spongepowered.asm.mixin`) for ordinal-dependent constructors. Add an `extend-enum` line (v2 header) only if you want the constant visible at compile time. Without it, look it up with `Target.valueOf("EXAMPLE_MOD_X")`. Pitfalls: exhaustive `switch` expressions elsewhere throw on unknown constants, and serialized or ordinal-indexed enums (e.g. `Axolotl.Variant`) break saves.
- Fabric API itself uses `transitive-inject-interface` heavily. For example, `fabric-item-api-v1.classtweaker` injects `FabricItem` into `Item`, `FabricItem$Properties` into `Item$Properties`, `FabricItemStack` into `ItemStack` and `FabricTooltipFlag` into `TooltipFlag`. `fabric-block-api-v1` injects `FabricBlock` into `Block`. That is why methods like `Block#getProvidedEnchantmentPower` exist in your IDE.
- In 26.3 there is no `loom:injected_interfaces` usage left in fapi (`grep` finds none).

---

## 11. Development environment tips

- IntelliJ IDEA 2025.3+ with the **Minecraft Development** plugin (MCDev). It gives mixin target completion, copying class tweaker entries (`Copy / Paste Special > AW Entry`), and mixin inspections. Eclipse needs 2025-12+ for Java 25.
- https://mcsrc.dev decompiles any MC version in the browser (Vineflower to WASM) and copies mixin targets and class tweaker lines via right-click "Copy Class Tweaker / Access Widener".
- **Hotswap**: start the run config in Debug mode, then Build Project (Ctrl+F9). A plain JDK can only change method bodies. The **JetBrains Runtime** (JBR 25) with `-XX:+AllowEnhancedClassRedefinition` can add and remove methods, fields and classes. JBR replaces DCEVM, which does not exist for 25. Point the run config's JRE at JBR.
- **Mixin hotswap**: add `-javaagent:"<absolute path to sponge-mixin-0.17.5+mixin.0.8.7.jar>"` to the run config VM options. Find the path under External Libraries; it lives in `~/.gradle/caches/modules-2/files-2.1/net.fabricmc/sponge-mixin/`. Or set `-Dmixin.hotSwap=true` (UNVERIFIED whether the agent is still required with that).
- Reload in game: `F3+T` reloads assets, `/reload` reloads data.
- Logs are in `run/logs/latest.log` (also `debug.log`), crash reports in `run/crash-reports/`, and mixin exports in `run/.mixin.out/` (see mixins.md).
- Reproducing a production environment: build the jar and use `ServerProductionRunTask`/`ClientProductionRunTask`, or drop it into a real Fabric install. On 26.x, dev and prod names are identical (no intermediary), so "works in dev, breaks in prod" now mostly comes from classpath grouping or missing `include`d dependencies, not remapping.
- Run multiple clients by defining a second run (section 7), each with its own `runDirectory` and `--username`.
- RenderDoc: set `fabric.loom.renderdoc.enabled=true` in gradle.properties, then run `runClientRenderDoc`.

## 12. Logging

- Use SLF4J. Loader and MC put it on the classpath, backed by log4j2.
  ```java
  public static final Logger LOGGER = LoggerFactory.getLogger("example-mod"); // org.slf4j
  // or vanilla's helper: com.mojang.logging.LogUtils.getLogger()  (logger named after the calling class)
  LOGGER.info("Loaded {} entries", n); LOGGER.debug(...); LOGGER.warn("x", throwable);
  ```
- `debug` is hidden by default. In dev, add `log4j-dev.xml` at the project root and set `loom.log4jConfigs.from "log4j-dev.xml"`:
  ```xml
  <Configuration><Loggers>
    <Logger name="example-mod" level="debug" additivity="false">
      <AppenderRef ref="SysOut"/><AppenderRef ref="ServerGuiConsole"/>
    </Logger>
  </Loggers></Configuration>
  ```
- Loader's own verbosity is controlled by `-Dfabric.log.level=debug`.
- Keep production logs quiet: use `info` only for one-time startup facts, and `warn`/`error` when something is actually wrong.

## 13. Gotchas (quick list)

- Copy-pasting a 1.21 build script gives `Cannot use Mojang mappings in a non-obfuscated environment` or "Could not find method modImplementation()". Remove `mappings` and the `mod*` configurations.
- Calling `net.minecraft.client.*` from `src/main` fails to compile under split source sets, by design. Move the code into `src/client` and reach it via the `client` entrypoint.
- Forgetting `loom.mods { ... sourceSet client }` means client resources or mixins are not found in dev.
- `fabric.mod.json` placeholders like `${version}` only work if `processResources` expands them and declares them as `inputs`. Otherwise the configuration cache or up-to-date checks serve a stale file.
- An uppercase mod id is a load error.
- Depending on `"fabric": "*"` fails on 26.1+. Use `fabric-api`.
- A class tweaker with header `named` or `intermediary` fails with a namespace mismatch. Use `official`.
- `include` adds nested jars only to `jar`. If you build a shadow or fat jar, nest them yourself with `loom.nestJars(...)`.
- Datagen output goes to `src/main/generated` by default and is added to resources. Commit it or regenerate it in CI. Do not hand-edit it.
- 26.x snapshot MC versions in `depends` need `-` suffixed ranges, or the snapshot will not satisfy `~26.4`.
