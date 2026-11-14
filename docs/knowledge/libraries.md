# Third-party Fabric libraries on Minecraft 26.3

Scope: which popular Fabric libraries have Fabric builds for 26.3 right now, how to depend on them with our Loom 1.19 / no-remap setup, and the core API of each. Verified 2026-11-14 against MC 26.3, Fabric Loader 0.20.1, Fabric API 0.166.2+26.3. Sources: Modrinth API v2 (`/v2/project/<slug>/version?loaders=["fabric"]`), each maven's `maven-metadata.xml` (HTTP 200 checked), and shallow clones of each library's 26.3 branch (class/method names below were grepped from those clones unless marked UNVERIFIED).

---

## 0. How to read and use this file

### Our Gradle setup (no remapping)
- 26.x Loom (`net.fabricmc.fabric-loom`) does not remap. Use plain `implementation`, `compileOnly`, `runtimeOnly`, `localRuntime` and `include`. `modImplementation`, `modCompileOnly`, `modRuntimeOnly` and `modLocalRuntime` are gone. Every library README still shows `mod*`. Strip the `mod` prefix. Mod Menu's README says so: "Prior to Minecraft 26.1 (or when using mappings), use `modImplementation`".
- Jar-in-jar still works: `implementation include("group:artifact:ver")` (Mod Menu's own build does `implementation include(fabricApi.module(...))` and `compileOnly include("eu.pb4:placeholder-api:...")`).
- The shared build `gradle/fabric-mod.gradle` already declares these repos: Modrinth maven (`maven.modrinth` group only), TerraformersMC, Shedaniel, isXander, Ladysnake, Nucleoid. Any other repo listed below has to go in the mod's own `build.gradle` `repositories {}` block. Use `exclusiveContent` or `content { includeGroup ... }` so Gradle doesn't query every repo for every artifact.
- Library POMs declare `compile` deps on Fabric API, Fabric Loader and other libs. For example, puzzleslib-fabric 26.3.18's POM pulls fabric-api 0.165.0+26.3, forgeconfigapiport-fabric and fabric-loader. Gradle picks the highest version, which is usually harmless. If a transitive dep drags in something you don't want, add `{ transitive = false }` or `exclude group: ...`. Modonomicon's docs use `transitive = false`.
- The game has been unobfuscated since 26.1, so library jars use Mojang names. `net.minecraft.resources.Identifier` is the real Mojang class in 26.x: `ResourceLocation` was renamed, and every 26.3 library below uses `Identifier.fromNamespaceAndPath(...)` / `Identifier.parse(...)` (src: common `net/minecraft/resources/Identifier.java`). Don't confuse it with the Yarn-era `net.minecraft.util.Identifier`.
- Modrinth maven: `maven.modrinth:<slug-or-project-id>:<version_number-or-version-id>`, served at `https://api.modrinth.com/maven/maven/modrinth/<slug>/<ver>/<slug>-<ver>.jar`. When one `version_number` is shared across several MC versions (example: player-animation-library `1.2.9` exists for 26.1, 26.2 and 26.3), Modrinth serves the newest one. Pin the immutable version ID instead, e.g. `maven.modrinth:player-animation-library:lQIjpkZc`.

### Status legend
- OK: a Fabric build that lists `26.3` exists on Modrinth or the official maven.
- BETA: the 26.3 build is tagged beta/alpha.
- NO: no Fabric 26.3 build. The latest supported MC version is given.

---

## 1. Availability summary (Fabric, MC 26.3)

| Library | Status | Version for 26.3 (date) | Maven coordinate (group:artifact) | Repo |
|---|---|---|---|---|
| Mod Menu | OK | 21.0.1 (2026-11-12) | `com.terraformersmc:modmenu` | maven.terraformersmc.com |
| YACL | OK | 3.9.7+26.3-fabric (2026-09-20) | `dev.isxander:yet-another-config-lib` | maven.isxander.dev/releases |
| Cloth Config | OK | 26.3.159 (2026-09-23) | `me.shedaniel.cloth:cloth-config-fabric` | maven.shedaniel.me |
| owo-lib | **NO** (latest 0.13.1+26.2, 2026-08-19) | - | `io.wispforest:owo-lib` | maven.wispforest.io/releases |
| Fzzy Config | OK | 0.7.7+fix3+26.3 (2026-10-03) | `me.fzzyhmstrs:fzzy_config` | maven.fzzyhmstrs.me |
| MidnightLib | OK | 1.10.0+26.3-fabric (2026-10-14) | `eu.midnightdust:midnightlib` | maven.midnightdust.eu/releases |
| Forge Config API Port | OK | 26.3.1 (2026-09-21) | `fuzs.forgeconfigapiport:forgeconfigapiport-fabric` | raw.githubusercontent.com/Fuzss/modresources/main/maven |
| Cardinal Components API | OK | 8.1.0 (2026-09-17) | `org.ladysnake.cardinal-components-api:cardinal-components-<module>` | maven.ladysnake.org/releases |
| GeckoLib | OK | 5.5.7 (2026-09-22) | `com.geckolib:geckolib-fabric-26.3` (**group changed** from `software.bernie.geckolib`) | dl.cloudsmith.io/public/geckolib3/geckolib/maven |
| AzureLib | OK | 4.1.8 (2026-11-09) | `mod.azure.azurelib:azurelib-fabric-26.3` | maven.azuredoom.com/mods |
| Trinkets (emilyalexandra) | **NO** (latest 3.10.0, MC 1.21.1) | - | `dev.emi:trinkets` | maven.terraformersmc.com |
| Trinkets Updated (Patbox fork) | OK | 4.2.1+26.3 (2026-09-26) | `eu.pb4:trinkets` | maven.nucleoid.xyz |
| Accessories (wisp-forest) | **NO** (latest 1.4.3-beta, MC 1.21.10) | - | `io.wispforest:accessories-fabric` | maven.wispforest.io/releases |
| Architectury API | OK | 22.0.5+fabric (2026-10-09) | `dev.architectury:architectury-fabric` | maven.architectury.dev |
| Resourceful Lib | OK | 6.0.1 (2026-09-27) | `com.teamresourceful.resourcefullib:resourcefullib-fabric-26.3` | maven.teamresourceful.com/repository/maven-public |
| Puzzles Lib | OK | 26.3.18 (2026-11-12) | `fuzs.puzzleslib:puzzleslib-fabric` | Fuzss modresources (as FCAP) |
| Moonlight Lib | **NO** (Fabric latest 1.21.1; 26.1.2 exists for NeoForge only) | - | Modrinth maven | - |
| Balm | OK | 26.3.0.3+fabric-26.3 (2026-10-02) | `net.blay09.mods:balm-fabric` | maven.twelveiterations.com/repository/maven-public |
| EMI | **NO** (latest 1.1.24+1.21.1; no 26.x branch) | - | `dev.emi:emi-fabric` | maven.terraformersmc.com (redirects to repo.sleeping.town) |
| REI | OK | 26.3.824 (2026-10-07) | `me.shedaniel:RoughlyEnoughItems-api-fabric` / `-fabric` | maven.shedaniel.me |
| JEI | BETA | 31.17.0.113 (2026-11-12) | `mezz.jei:jei-26.3-common-api`, `jei-26.3-fabric-api`, `jei-26.3-fabric` | maven.blamejared.com |
| RRV (Reliable Recipe Viewer) | OK | 8.11.3+26.3-fabric (2026-11-03) | Modrinth maven `rrv` | - |
| Patchouli | **NO** (latest 26.1-94-beta; its metadata pins `minecraft >=26.1 <26.2`) | - | `vazkii.patchouli:Patchouli` | maven.blamejared.com |
| Lavender | **NO** (latest 0.1.15+1.21.4) | - | `io.wispforest:lavender` | maven.wispforest.io/releases |
| Modonomicon | OK | 2.20.1 (2026-11-06) | `com.klikli_dev:modonomicon-26.3-fabric` | dl.cloudsmith.io/public/klikli-dev/mods/maven |
| Polymer (all modules) | OK | 0.18.7+26.3 (2026-11-03) | `eu.pb4:polymer-<module>` | maven.nucleoid.xyz |
| Placeholder API | OK | 3.2.0+26.3 (2026-09-09) | `eu.pb4:placeholder-api` | maven.nucleoid.xyz |
| sgui | OK (maven only, not on Modrinth) | 2.2.1+26.3 (maven 2026-09-26) | `eu.pb4:sgui` | maven.nucleoid.xyz |
| Server Translations API | OK (maven only) | 3.2.0+26.3 (maven 2026-09-07) | `xyz.nucleoid:server-translations-api` | maven.nucleoid.xyz |
| fabric-permissions-api (lucko) | OK | 0.7.0 (2026-05-27; declares 26.1.2, 26.2, 26.3) | `me.lucko:fabric-permissions-api` | Maven Central |
| fabric-permission-api-v1 (Fabric API) | OK | ships in Fabric API 0.166.2+26.3 | part of `fabric-api` | maven.fabricmc.net |
| LuckPerms (Fabric) | OK | v5.5.85-fabric (2026-09-19; 26.1.2-26.3) | API: `net.luckperms:api:5.5` | Maven Central |
| Sodium | OK / BETA | 0.9.2+mc26.3 (2026-09-15) stable; 0.9.3-beta.5+mc26.3 (2026-11-12) | `net.caffeinemc:sodium-fabric` | maven.caffeinemc.net/releases |
| Iris | OK | 1.11.7+26.3-fabric (2026-09-30) | Modrinth maven `iris` | - |
| Lithium | OK | mc26.3-0.26.3-fabric (2026-10-28) | Modrinth maven `lithium` (not on caffeinemc maven) | - |
| Indium | **NO, and not needed** (latest 1.0.36 for MC 1.21.1) | - | - | - |
| FerriteCore | OK | 9.0.0-fabric (2026-03-24; 26.1.2-26.3) | Modrinth maven `ferrite-core` | - |
| ModernFix | **NO** on Fabric (latest Fabric is 1.21.4; 26.1.2 exists for NeoForge only) | - | - | - |
| Entity Culling | OK | 1.12.1 (2026-11-13) | Modrinth maven `entityculling` | - |
| Jade | OK | 26.3.5+fabric (2026-10-02) | Modrinth maven `jade` | - |
| WTHIT | OK | fabric-21.0.0 (2026-11-08) | `mcp.mobius.waila:wthit-api` / `wthit` (+ `lol.bai:badpackets`) | maven2.bai.lol (redirects to maven4.bai.lol) |
| Fabric Language Kotlin | OK | 1.14.2+kotlin.2.4.21 (2026-10-09; 26.1.2-26.3) | `net.fabricmc:fabric-language-kotlin` | maven.fabricmc.net |
| LambDynamicLights (API) | OK | 4.13.0+26.3 (2026-09-15) | `dev.lambdaurora.lambdynamiclights:lambdynamiclights-api` | maven.gegy.dev/releases |
| Cloth API | **DEAD** (last maven publish 2022, branches end at 1.19) | - | - | - |
| MixinSquared | OK (loader-agnostic) | 0.3.8 (maven 2026-11-08) | `com.github.bawnorton.mixinsquared:mixinsquared-fabric` | maven.bawnorton.com/releases |
| MixinExtras | bundled | 0.5.5 ships inside Fabric Loader 0.20.1 | (don't add) | - |
| DevAuth (original) | UNVERIFIED on 26.x (README lists Fabric 1.14-1.21.11) | 1.2.2 | `me.djtheredstoner:DevAuth-fabric` | pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1 |
| DevAuth Neo (fork) | OK | 1.2.0 (26.3) | `net.litetex.mcm:dev-auth-neo` (Maven Central) or Modrinth `dev-auth-neo` | Maven Central |
| Pehkui | **NO** (latest 3.8.3, MC 1.21.1, last update 2024-06) | - | - | - |
| Origins / Apoli | **NO** (latest 1.13.0-pre.4+mc.1.21.1, 2026-10-18) | - | - | - |
| Create (Fabric) | **NO** (create-fabric latest 6.0.8.1 for 1.20.1; main Create is NeoForge 1.21.1 only) | - | - | - |
| Player Animation Library (PAL) | BETA | 1.2.9 (`lQIjpkZc`, 2026-11-04) | Modrinth maven `player-animation-library` | - |
| playerAnimator (KosmX) | **NO** (latest 2.0.4 for 1.21.1) | - | - | - |
| MezzConfig (required by JEI) | BETA | 0.7.1 (2026-11-09) | Modrinth `mezzconfig` | - |
| YUNG's API | **NO** on Fabric 26.3 (latest 26.1.2) | - | - | - |
| Curios | NeoForge only (its Fabric listing is 1.16.5) | - | - | - |

Other popular libraries that do have Fabric 26.3 builds (Modrinth `categories:library`, sorted by downloads; runtime deps of popular mods): Collective 26.3.0-8.86, Bookshelf 26.3.0.4, Konkrete 1.11.1, CreativeCore 2.14.25, malilib 0.30.2, TerraBlender 26.3.0.0.9 (beta), Iceberg 1.4.3.1, Prism 1.1.3, Searchables 1.0.3, libIPN 6.9.0, SuperMartijn642's Config Lib 1.1.8a, Resourceful Config 6.0.2, Lithostitched 2.0.6, GlitchCore 26.3.0.0.5 (beta), Athena CTM 4.7.4, Kiwi 26.3.1 (beta), Cicada 0.15.3, LibJF 26.3.1, Continuity 3.0.1, ImmediatelyFast 1.17.1, Sodium Extra 0.9.6-beta.4, MoreCulling 1.9.2 (beta), C2ME 0.4.2-alpha, Krypton 0.3.2, spark 1.10.187.

---

## 2. Config libraries

### Mod Menu (TerraformersMC) - OK, 21.0.1
Adds the mod list screen and the "Config" button. It's a dependency of almost every config library.
```groovy
repositories { maven { url = 'https://maven.terraformersmc.com/' } }   // already in fabric-mod.gradle
dependencies { implementation "com.terraformersmc:modmenu:21.0.1" }    // or compileOnly to avoid a hard dep
```
- Mod Menu 21.x jar-in-jars Placeholder API (Modrinth lists it as "required", but it is bundled via `compileOnly include("eu.pb4:placeholder-api:3.2.0+26.3")`). Its `fabric.mod.json` depends on `fabric-resource-loader-v1`, `fabric-screen-api-v1`, `fabric-key-mapping-api-v1`, `fabric-lifecycle-events-v1` and `minecraft >=26.3-`.
- API (src: ModMenu 26.3 `com/terraformersmc/modmenu/api/`). Use the `modmenu` entrypoint:
```java
public class MyModMenu implements com.terraformersmc.modmenu.api.ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> MyConfigScreens.create(parent);   // ConfigScreenFactory<S extends Screen> { @Nullable S create(Screen parent); }
    }
    // also: getProvidedConfigScreenFactories(), getUpdateChecker(), attachModpackBadges(Consumer<String>)
}
```
```json
"entrypoints": { "modmenu": [ "com.example.client.MyModMenu" ] },
"custom": { "modmenu": { "links": { "modmenu.discord": "https://..." }, "badges": [ "library" ], "parent": "mymod" } }
```
Badges you can declare are `library` and `deprecated`. Others are ignored. Put this class in the `client` source set.

### YetAnotherConfigLib (YACL) - OK, 3.9.7+26.3-fabric
The most popular modern config GUI. It has a builder API and an annotation "autogen" config, with a Gson/JSON5 serializer. Its only declared dependency is Fabric API. Note that 3.9.8+ are built for the 26.4 snapshots, so stay on `3.9.7+26.3-fabric`.
```groovy
repositories { maven { url = 'https://maven.isxander.dev/releases' } }  // already in fabric-mod.gradle
dependencies { implementation "dev.isxander:yet-another-config-lib:3.9.7+26.3-fabric" }
```
Config class with autogen (src: YACL port/26.3 `src/testmod/.../AutogenConfigTest.java`, package `dev.isxander.yacl3.config.v2.api`):
```java
public class MyConfig {
    public static final ConfigClassHandler<MyConfig> HANDLER = ConfigClassHandler.createBuilder(MyConfig.class)
        .id(Identifier.fromNamespaceAndPath("mymod", "config"))
        .serializer(c -> GsonConfigSerializerBuilder.create(c)
            .setPath(FabricLoader.getInstance().getConfigDir().resolve("mymod.json5"))
            .setJson5(true).build())
        .build();

    @AutoGen(category = "general") @SerialEntry(comment = "Enable the thing")
    public boolean enabled = true;
    @AutoGen(category = "general") @IntSlider(min = 0, max = 10, step = 1) @SerialEntry
    public int power = 5;
}
// init:   MyConfig.HANDLER.load();   read: MyConfig.HANDLER.instance().enabled;   write: MyConfig.HANDLER.save();
// screen: MyConfig.HANDLER.generateGui().generateScreen(parent)
```
Builder API (src: `GuiTest.java`):
```java
YetAnotherConfigLib.create(MyConfig.HANDLER, (defaults, config, builder) -> builder
    .title(Component.literal("My Mod"))
    .category(ConfigCategory.createBuilder().name(Component.literal("General"))
        .option(Option.<Boolean>createBuilder()
            .name(Component.literal("Enabled"))
            .description(OptionDescription.of(Component.literal("...")))
            .binding(defaults.enabled, () -> config.enabled, v -> config.enabled = v)
            .controller(BooleanControllerBuilder::create)
            .build())
        .build()))
  .generateScreen(parent);
```
Autogen annotations live in `dev.isxander.yacl3.config.v2.api.autogen`: `TickBox`, `Boolean`, `IntSlider`, `LongSlider`, `FloatSlider`, `DoubleSlider`, `IntField`, `LongField`, `FloatField`, `DoubleField`, `StringField`, `ColorField`, `EnumCycler`, `Dropdown`, `ItemField`, `ListGroup`, `MasterTickBox`, `Label`, `CustomName`, `CustomDescription`, `CustomImage`, `CustomFormat`, `FormatTranslation`.

### Cloth Config - OK, 26.3.159
An older builder GUI plus AutoConfig (annotation config with Gson, Jankson or Toml4j serializers). REI requires it. Maven version strings have no `+fabric` suffix: `26.3.159`.
```groovy
dependencies { implementation("me.shedaniel.cloth:cloth-config-fabric:26.3.159") { exclude group: 'net.fabricmc.fabric-api' } }
```
AutoConfig (src: ClothConfig v26.3 `common/.../autoconfig/example/ExampleInits.java`):
```java
@Config(name = "mymod")
public class MyConfig implements ConfigData { public boolean enabled = true; @ConfigEntry.BoundedDiscrete(min = 0, max = 10) public int power = 5; }
ConfigHolder<MyConfig> holder = AutoConfig.register(MyConfig.class, GsonConfigSerializer::new); // also JanksonConfigSerializer, Toml4jConfigSerializer, PartitioningSerializer.wrap(...)
MyConfig cfg = holder.getConfig();      // or AutoConfig.getConfigHolder(MyConfig.class).getConfig()
Screen s = AutoConfig.getConfigScreen(MyConfig.class, parent).get();   // client
```
Manual builder: `ConfigBuilder b = ConfigBuilder.create().setParentScreen(parent).setTitle(...); ConfigEntryBuilder e = b.entryBuilder(); b.getOrCreateCategory(Component.literal("General")).addEntry(e.startBooleanToggle(Component.literal("Enabled"), cfg.enabled).setDefaultValue(true).setSaveConsumer(v -> cfg.enabled = v).build()); b.setSavingRunnable(holder::save); return b.build();`

### owo-lib (wisp-forest) - **NOT available for 26.3**
owo-lib provides owo-config (annotation-processor generated config wrappers), owo-ui (declarative/XML UI), registration helpers and networking. The latest build is `0.13.1+26.2` (2026-08-19). There's no 26.3 branch (branches: `26.1`, `26.1-Neo`, `26.2`). The 26.2 jar's `fabric.mod.json` declares `"minecraft": ">=26.1"`, so Loader will try to load it on 26.3, but it's untested (UNVERIFIED, and owo is mixin-heavy). Don't build on it for 26.3. Maven: `io.wispforest:owo-lib:0.13.1+26.2` at `https://maven.wispforest.io/releases`.

### Fzzy Config - OK, 0.7.7+fix3+26.3
A Kotlin-based config library. It does auto GUI, server-to-client sync, validation (`ValidatedInt`, `ValidatedDouble`, ...) and permissions. **It requires Fabric Language Kotlin at runtime** (a Modrinth dep). It can be used from Java through `ConfigApiJava`.
```groovy
repositories { maven { url = 'https://maven.fzzyhmstrs.me/' } }
dependencies { implementation "me.fzzyhmstrs:fzzy_config:0.7.7+fix3+26.3"; runtimeOnly "net.fabricmc:fabric-language-kotlin:1.14.2+kotlin.2.4.21" }
```
```java
public class MyConfig extends me.fzzyhmstrs.fzzy_config.config.Config {
    public MyConfig() { super(Identifier.fromNamespaceAndPath("mymod", "main")); }  // open class Config(identifier, subfolder="", folder=ns, name=path)
    public boolean enabled = true;
    public ValidatedInt power = new ValidatedInt(5, 10, 0);   // UNVERIFIED ctor arg order (default, max, min); check validation/number/ValidatedInt.kt
}
public static final MyConfig CONFIG = ConfigApiJava.registerAndLoadConfig(MyConfig::new, RegisterType.BOTH); // BOTH = synced + GUI; SERVER; CLIENT
```
(src: fconfig master `api/ConfigApiJava.kt`, `api/RegisterType.kt`, `config/Config.kt`; master is the 26.3 branch, `minecraftVersion = 26.3`.)

### MidnightLib - OK, 1.10.0+26.3-fabric
Very small. It's a single-class config with static fields and an auto-generated screen that registers itself with Mod Menu. Its only dependency is Fabric API.
```groovy
repositories { maven { url = 'https://maven.midnightdust.eu/releases' } }
dependencies { implementation include("eu.midnightdust:midnightlib:1.10.0+26.3-fabric") }
```
```java
public class MyConfig extends eu.midnightdust.lib.config.MidnightConfig {
    @Entry(category = "general") public static boolean enabled = true;          // fields MUST be public static
    @Entry(category = "general", min = 0, max = 10, isSlider = true) public static int power = 5;
    @Entry(category = "general", idMode = 0) public static Identifier item = Identifier.withDefaultNamespace("diamond");
}
MidnightConfig.init("mymod", MyConfig.class);          // in onInitialize
Screen s = MidnightConfig.getScreen(parent, "mymod");   // manual open
```
Annotations: `@Entry(width, min, max, name, selectionMode, idMode, isColor, isSlider, precision, category)`, `@Client`, `@Server`, `@Hidden`, `@Comment`, `@Condition` (src: `eu/midnightdust/lib/config/MidnightConfig.java`).

### Forge Config API Port (FCAP) - OK, 26.3.1
Gives you NeoForge's `ModConfigSpec` (TOML via NightConfig) on Fabric. Use it for multiloader mods or when a dependency needs it (Puzzles Lib does). The current API package is `fuzs.forgeconfigapiport.fabric.api.v5`.
```groovy
repositories { maven { url = 'https://raw.githubusercontent.com/Fuzss/modresources/main/maven/' } }
dependencies { implementation "fuzs.forgeconfigapiport:forgeconfigapiport-fabric:26.3.1" }
```
```java
ModConfigSpec.Builder b = new ModConfigSpec.Builder();                  // net.neoforged.neoforge.common.ModConfigSpec
ModConfigSpec.BooleanValue ENABLED = b.comment("Enable").define("enabled", true);
ModConfigSpec SPEC = b.build();
ConfigRegistry.INSTANCE.register("mymod", ModConfig.Type.COMMON, SPEC);  // net.neoforged.fml.config.ModConfig
// client: ConfigScreenFactoryRegistry.INSTANCE.register("mymod", (modId, parent) -> ...);  events: ModConfigEvents
```
It also accepts the legacy `net.minecraftforge.fml.config.IConfigSpec`.

---

## 3. Content and data libraries

### Cardinal Components API (CCA) vs Fabric Data Attachment API
**CCA 8.1.0 (OK).** Modules: `cardinal-components-base` (always), plus `-entity`, `-block`, `-chunk`, `-item`, `-level`, `-leveldata`, `-scoreboard`. **8.0 renamed modules to Mojmap:** old `-world` is now `-level`, and old `-level` (level properties) is now `-leveldata`. The aggregate `cardinal-components-api` maven artifact is empty, so depend on the individual modules.
```groovy
repositories { maven { url = 'https://maven.ladysnake.org/releases' } }  // already in fabric-mod.gradle
dependencies {
    implementation include("org.ladysnake.cardinal-components-api:cardinal-components-base:8.1.0")
    implementation include("org.ladysnake.cardinal-components-api:cardinal-components-entity:8.1.0")
}
```
The component interface changed. The README still shows `ComponentV3`/`readFromNbt(CompoundTag)`, which is wrong for 8.x. In 8.x the base is `org.ladysnake.cca.api.v8.component.CardinalComponent` with `readData(ValueInput)` / `writeData(ValueOutput)`. `org.ladysnake.cca.api.v3.component.Component` is `@Deprecated(since="8.0.0")` and just extends it (src: CCA 26.3 `cardinal-components-base/.../api/v8/component/CardinalComponent.java`).
```java
public interface ManaComponent extends CardinalComponent { int get(); void set(int v); }
public final class PlayerMana implements ManaComponent, AutoSyncedComponent {
    private final Player owner; private int mana;
    public PlayerMana(Player p) { owner = p; }
    public int get() { return mana; }
    public void set(int v) { mana = v; MyComponents.MANA.sync(owner); }
    @Override public void readData(ValueInput in) { mana = in.getIntOr("mana", 0); }   // ValueInput API: see vanilla net/minecraft/world/level/storage/ValueInput.java
    @Override public void writeData(ValueOutput out) { out.putInt("mana", mana); }
}
public final class MyComponents implements EntityComponentInitializer {
    public static final ComponentKey<ManaComponent> MANA =
        ComponentRegistry.getOrCreate(Identifier.fromNamespaceAndPath("mymod", "mana"), ManaComponent.class);
    @Override public void registerEntityComponentFactories(EntityComponentFactoryRegistry r) {
        r.registerForPlayers(MANA, PlayerMana::new, RespawnCopyStrategy.ALWAYS_COPY);
        // r.beginRegistration(Monster.class, KEY).after(OTHER).respawnStrategy(...).impl(Impl.class).end(e -> new Impl());
        // r.registerFor(Zombie.class, KEY, factory); r.registerFor(cls -> ..., KEY, factory)
    }
}
```
```json
"entrypoints": { "cardinal-components": [ "com.example.MyComponents" ] },
"custom": { "cardinal-components": [ "mymod:mana" ] }
```
Every component ID **must** be listed under `custom.cardinal-components`. Read with `MyComponents.MANA.get(player)`. Sync with `KEY.sync(provider)` or `KEY.sync(provider, writer, predicate)`. The other interfaces are `ServerTickingComponent`, `ClientTickingComponent`, `ServerLoadAwareComponent`, `ClientLoadAwareComponent` and `RespawnableComponent`. The item module now only offers `registerItemComponentMigrations(ItemComponentMigrationRegistry)` to migrate old item components to vanilla `DataComponentType`s. For item data, use vanilla data components.

**Fabric Data Attachment API (`fabric-data-attachment-api-v1`, in Fabric API).** Targets are `Entity`, `BlockEntity`, `ServerLevel`, `ChunkAccess` and `GlobalAttachments` (server-wide; get it via `Level#globalAttachments()` / `MinecraftServer#globalAttachments()`). Source: fabric-api `fabric-data-attachment-api-v1/.../AttachmentTarget.java`, `GlobalAttachments.java`.
```java
public static final AttachmentType<Integer> MANA = AttachmentRegistry.create(
    Identifier.fromNamespaceAndPath("mymod", "mana"),
    b -> b.initializer(() -> 0).persistent(Codec.INT).copyOnDeath()
          .syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly()));   // all() / targetOnly() / allButTarget()
int m = player.getAttachedOrCreate(MANA);  player.setAttached(MANA, m + 1);  player.modifyAttached(MANA, v -> v + 1);
// also: createPersistent(id, codec), createDefaulted(id, supplier), getAttached, hasAttached, removeAttached, onAttachedSet(type)
```
Gotcha: if you mutate a mutable attachment in place on a `BlockEntity` or `ChunkAccess`, call `be.setChanged()` / `chunk.markUnsaved()` yourself. `setAttached` does it for you.

**When to use which:**
- Fabric attachments are the default: there's no extra dependency, they use Codec/StreamCodec, they persist and sync, they support global/server-wide data, and the mod stays vanilla-compatible on the server.
- Use CCA when you need things attachments don't have: scoreboard/team components, `LevelData` components, per-component ticking (server and client), load/unload callbacks, custom sync packets with `PlayerSyncPredicate`, fine-grained `RespawnCopyStrategy` (ALWAYS_COPY / INVENTORY / LOSSLESS_ONLY / NEVER_COPY), component ordering (`after`), or an interface-typed component API that other mods implement. Also use it when interoperating with mods that already expose CCA components.
- For per-ItemStack data, use neither. Use vanilla `DataComponentType` (see the items knowledge file).

### GeckoLib 5 - OK, 5.5.7
Keyframe animation engine for Blockbench models (entities, blocks/BEs, items, armor). **The maven group is now `com.geckolib`** (the 26.x artifact is `geckolib-fabric-26.3`; the old `software.bernie.geckolib` group 404s for 26.3), and the Java package is `com.geckolib.*`.
```groovy
repositories { exclusiveContent { forRepository { maven { url = 'https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/' } }; filter { includeGroupAndSubgroups('com.geckolib') } } }
dependencies { implementation "com.geckolib:geckolib-fabric-26.3:5.5.7" }
```
Entity (src: geckolib main `common/src/main/java/com/geckolib/`):
```java
public class Drake extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkIdleController());          // uses "move.walk" / "misc.idle"
        controllers.add(new AnimationController<>("attack", 5, test ->
            test.isMoving() ? test.setAndContinue(RawAnimation.begin().thenLoop("walk")) : PlayState.STOP));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
// client: EntityRenderers.register(MY_TYPE, ctx -> new GeoEntityRenderer<>(ctx, MY_TYPE));  // class GeoEntityRenderer<T extends Entity & GeoAnimatable, R extends EntityRenderState>
//     or new GeoEntityRenderer<>(ctx, new DefaultedEntityGeoModel<>(Identifier.fromNamespaceAndPath("mymod", "drake")))
```
Asset layout for `DefaultedEntityGeoModel(mymod:drake)`: `assets/mymod/geckolib/models/entity/drake.geo.json`, `assets/mymod/geckolib/animations/entity/drake.animation.json`, `assets/mymod/textures/entity/drake.png` (src: `cache/GeckoLibResources.java` `MODELS_PATH = geckolib/models`, `ANIMATIONS_PATH = geckolib/animations`; `model/DefaultedGeoModel.java`). `RawAnimation` methods: `begin()`, `thenPlay`, `thenLoop`, `thenWait(ticks)`, `thenPlayAndHold`, `thenPlayXTimes`, `then(name, LoopType)`. The handler returns `PlayState.CONTINUE`, `PAUSE` or `STOP`. GeckoLib 5 renders through render states (`EntityRenderState`). Its wiki pages "What's a RenderState?" and "Updating from GeckoLib4" are at wiki.geckolib.com/docs/geckolib5.

### AzureLib - OK, 4.1.8
A GeckoLib 4 fork with a rewritten "Az" API: animators and commands instead of in-entity controllers.
```groovy
repositories { maven { url = 'https://maven.azuredoom.com/mods' } }
dependencies { implementation "mod.azure.azurelib:azurelib-fabric-26.3:4.1.8" }
```
```java
public class DrakeAnimator extends AzEntityAnimator<Drake> {           // mod.azure.azurelib.animation.impl
    @Override public void registerControllers(AzAnimationControllerContainer<Drake> c) { c.add(AzAnimationController.builder(this, "base_controller").build()); } // UNVERIFIED builder shape
    @Override public Identifier getAnimationLocation(Drake d) { return Identifier.fromNamespaceAndPath("mymod", "animations/entity/drake.animation.json"); }
}
static final AzCommand WALK = AzCommand.create("base_controller", "walking", AzPlayBehaviors.LOOP);
WALK.sendForEntity(drake);     // trigger from tick (client side)
public class DrakeRenderer extends AzEntityRenderer<Drake> {
    public DrakeRenderer(EntityRendererProvider.Context ctx) {
        super(AzEntityRendererConfig.<Drake>builder(GEO, TEX).setAnimatorProvider(DrakeAnimator::new).build(), ctx); }
}
```
(src: AzureLib 26.3 `docs/azurelib/entities/entities.mdx`, `animation/AzAnimator.java`.) Use GeckoLib unless you need AzureLib's extras: hitboxes, limb health, Better Combat hooks.

### Trinkets / Accessories (accessory slots)
- **Trinkets (emilyalexandra, `dev.emi.trinkets`) has no 26.x build.** Latest is 3.10.0 for 1.21.1 (2024-07).
- **Accessories (wisp-forest) has no 26.x build** either (latest 1.4.3-beta for 1.21.10; the newest branch is `1.21.11`).
- **Trinkets Updated (Patbox fork) is OK: `eu.pb4:trinkets:4.2.1+26.3`** at maven.nucleoid.xyz. It's a universal jar for Fabric and NeoForge. **The package changed to `eu.pb4.trinkets.api`**, so it's not source-compatible with `dev.emi.trinkets`. There's also a `trinkets-polymer` add-on (Modrinth, 4.2.0.0+26.3 beta) that makes it server-side.
```java
public class Ring extends Item implements eu.pb4.trinkets.api.callback.TrinketCallback {   // UNVERIFIED exact package of TrinketCallback (api/callback/)
    @Override public void tick(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity) { ... }
    @Override public void forEachTrinketModifier(ItemStack stack, TrinketSlotAccess slot, LivingEntity e, Identifier id,
                                                 BiConsumer<Holder<Attribute>, AttributeModifier> out) {
        out.accept(Attributes.MOVEMENT_SPEED, new AttributeModifier(id.withSuffix("mymod/speed"), 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)); }
}
TrinketEquipCallback.EVENT.register((stack, slot, entity) -> { ... });     // eu.pb4.trinkets.api.event.*
TrinketsApi.getAttachment(livingEntity); TrinketsApi.getPlayerSlots(player);
```
Data: `data/trinkets/slots/<group>/<slot>.json` (e.g. `{"icon":"trinkets:container/slots/charm"}`), `data/trinkets/entities/<file>.json`, and item tags `data/trinkets/tags/item/<group>/<slot>.json`. Client renderers implement `TrinketRenderer#submit(..., PoseStack, SubmitNodeCollector, int light, LivingEntityRenderState, ...)`, which uses the 26.x submit-based rendering (src: Patbox/trinkets `src/testmod/java/eu/pb4/trinkets/TestTrinket.java`).

### Architectury API - OK, 22.0.5
A multiloader abstraction for registries, events, networking and menus. REI depends on it.
```groovy
repositories { maven { url = 'https://maven.architectury.dev/' } }
dependencies { implementation "dev.architectury:architectury-fabric:22.0.5" }
```
`DeferredRegister<Item> ITEMS = DeferredRegister.create("mymod", Registries.ITEM); RegistrySupplier<Item> RUBY = ITEMS.register("ruby", () -> new Item(...)); ITEMS.register();`. Events are in `dev.architectury.event.events.common.*` (`LifecycleEvent`, `TickEvent`, `PlayerEvent`, `BlockEvent`, `EntityEvent`, `InteractionEvent`, `LootEvent`, `CommandRegistrationEvent`, ...). On a Fabric-only mod, prefer Fabric API directly.

### Resourceful Lib - OK, 6.0.1
Team Resourceful's multiloader utilities (registries, networking, codecs, UI). Fabric builds 6.x are for 26.3. Note that the "latest" Fabric upload on Modrinth is 4.0.2 for 26.1, posted later in time.
```groovy
repositories { maven { url = 'https://maven.teamresourceful.com/repository/maven-public/' } }
dependencies { implementation "com.teamresourceful.resourcefullib:resourcefullib-fabric-26.3:6.0.1" }
```
API not reviewed here (UNVERIFIED). It's mostly pulled in as a runtime dependency of Team Resourceful mods. Its sibling Resourceful Config is at 6.0.2.

### Puzzles Lib - OK, 26.3.18
Fuzss's multiloader library. It **requires Forge Config API Port** and Fabric API.
```groovy
repositories { maven { url = 'https://raw.githubusercontent.com/Fuzss/modresources/main/maven/' } }
dependencies { implementation "fuzs.puzzleslib:puzzleslib-fabric:26.3.18" }
```
API not reviewed here (UNVERIFIED). It's usually only needed as a runtime dependency of Fuzss mods.

### Moonlight Lib - **NOT available on Fabric 26.3**
The latest Fabric build is for 1.21.1 (3.8.3). `26.1.2-4.3.0` is NeoForge-only. Supplementaries and other MehVahdJukaar mods are therefore NeoForge-only on 26.x.

### Balm - OK, 26.3.0.3
TwelveIterations (BlayTheNinth) multiloader layer, used by Waystones, Crafting Tweaks and others.
```groovy
repositories { maven { url = 'https://maven.twelveiterations.com/repository/maven-public/' } }
dependencies { implementation "net.blay09.mods:balm-fabric:26.3.0.3" }   // 26.3.0.4-SNAPSHOT also published
```
API not reviewed (UNVERIFIED).

---

## 4. Recipe viewers

**EMI is not available on 26.x.** Its newest branch is `1.21` (MC 1.21.1, EMI 1.1.24, last commit 2026-10-31). On 26.3 the choices are **REI** (stable), **JEI** (beta, now requires MezzConfig) and **RRV**, a newer viewer that's popular on 26.x and that WTHIT integrates with. Write integration code against `compileOnly` APIs and gate it behind a `FabricLoader.getInstance().isModLoaded(...)` check or an optional entrypoint, so your mod runs without the viewer.

### REI (Roughly Enough Items) - OK, 26.3.824
It requires **Cloth Config** and **Architectury API** at runtime.
```groovy
repositories { maven { url = 'https://maven.shedaniel.me/' } }    // already in fabric-mod.gradle (same repo as architectury mirror)
dependencies {
    compileOnly "me.shedaniel:RoughlyEnoughItems-api-fabric:26.3.824"
    localRuntime "me.shedaniel:RoughlyEnoughItems-fabric:26.3.824"     // dev-only runtime; or runtimeOnly
}
```
Entrypoints (src: REI 26.3 `fabric/.../PluginDetectorImpl.java`): `rei_client` (`REIClientPlugin`), `rei_server` (`REIServerPlugin`), `rei_common` (`REICommonPlugin`).
```java
public class MyReiClient implements me.shedaniel.rei.api.client.plugins.REIClientPlugin {
    static final CategoryIdentifier<MyDisplay> MY = CategoryIdentifier.of("mymod", "grinding");
    @Override public void registerCategories(CategoryRegistry r) { r.add(new MyCategory()); r.addWorkstations(MY, EntryStacks.of(MyBlocks.GRINDER)); }
    @Override public void registerDisplays(DisplayRegistry r) { /* r.add(new MyDisplay(...)); or r.beginFiller(...) */ }
    // also registerScreens, registerExclusionZones, registerEntries, registerEntryRenderers, registerCollapsibleEntries, registerFavorites
}
// REICommonPlugin: registerDisplaySerializer(DisplaySerializerRegistry), registerDisplays(ServerDisplayRegistry), registerItemComparators, registerFluidSupport
```
In 1.21.2+, recipes no longer sync to the client, so REI has displays generated server-side and needs `registerDisplaySerializer` for custom displays. This is UNVERIFIED in detail; see `api/.../common/display/DisplaySerializerRegistry.java`.

### JEI - BETA, 31.17.0.113
The Fabric build requires **MezzConfig** (Modrinth `mezzconfig` 0.7.1, beta) and has MezzConfigGUI as an optional extra.
```groovy
repositories { maven { url = 'https://maven.blamejared.com/' } }
dependencies {
    compileOnly "mezz.jei:jei-26.3-common-api:31.17.0.113"
    compileOnly "mezz.jei:jei-26.3-fabric-api:31.17.0.113"
    localRuntime "mezz.jei:jei-26.3-fabric:31.17.0.113"
    localRuntime "maven.modrinth:mezzconfig:0.7.1"
}
```
The Fabric entrypoint key is **`jei_mod_plugin`** (src: JEI 26.3 `Fabric/.../FabricPluginFinder.java`). The `@JeiPlugin` annotation is for NeoForge only.
```java
public class MyJei implements mezz.jei.api.IModPlugin {
    public static final IRecipeType<MyRecipe> GRINDING = IRecipeType.create("mymod", "grinding", MyRecipe.class);
    @Override public Identifier getPluginUid() { return Identifier.fromNamespaceAndPath("mymod", "jei"); }
    @Override public void registerCategories(IRecipeCategoryRegistration r) { r.addRecipeCategories(new MyCategory(r.getJeiHelpers().getGuiHelper())); }
    @Override public void registerRecipes(IRecipeRegistration r) { r.addRecipes(GRINDING, list); r.addIngredientInfo(MyItems.RUBY, Component.literal("...")); }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r) { r.addCraftingStation(GRINDING, MyBlocks.GRINDER); } // UNVERIFIED method name in 26.3
}
// IRecipeCategory<T>: getRecipeType(), getTitle(), getWidth(), getHeight(), getIcon(), setRecipe(IRecipeLayoutBuilder, T, IFocusGroup),
//   draw(T, IRecipeSlotsView, GuiGraphicsExtractor g, double mx, double my)   <- 26.x vanilla renamed GuiGraphics -> GuiGraphicsExtractor
// For vanilla RecipeType: IRecipeType.create(RecipeType<R>) returns IRecipeHolderType<R>
```
(src: JEI 26.3 `Common/src/api/java/mezz/jei/api/IModPlugin.java`, `recipe/category/IRecipeCategory.java`, `recipe/types/IRecipeType.java`; `GuiGraphicsExtractor` exists at client `net/minecraft/client/gui/GuiGraphicsExtractor.java`.)

### RRV (Reliable Recipe Viewer) - OK, 8.11.3+26.3-fabric
Rebuilt from EIV ("Extended Item View"). Fabric API is its only dependency. Get it from Modrinth maven (`maven.modrinth:rrv:8.11.3+26.3-fabric`). Its plugin API wasn't reviewed (UNVERIFIED; source at github.com/cassiancc/ReliableRecipeViewer).

---

## 5. In-game documentation / guide books

- **Patchouli: NOT available on 26.3.** The latest is `26.1-94-beta`, and its `fabric.mod.json` pins `"minecraft": ">=26.1 <26.2"`. The repo's only 26.x branch is `26.1`.
- **Lavender (wisp-forest, owo-based): NOT available.** The latest is 0.1.15+1.21.4.
- **GuideME (AE2's guide system):** NeoForge-only (26.3.7-alpha).
- **Modonomicon: OK, 2.20.1 for 26.3.** It's the only maintained Fabric guide-book library on 26.3. The jar is a single file holding API and implementation. It optionally integrates with JEI.
```groovy
repositories { maven { url = 'https://dl.cloudsmith.io/public/klikli-dev/mods/maven/'; content { includeGroup 'com.klikli_dev' } } }
dependencies { implementation("com.klikli_dev:modonomicon-26.3-fabric:2.20.1") { transitive = false } }
```
Books are data-driven under `data/<ns>/modonomicon/books/<book_id>/` (src: `api/ModonomiconConstants.java` `MODONOMICON_DATA_PATH = "modonomicon/books"`, plus `modonomicon/multiblocks`, `modonomicon/research`). Layout, from the 26.3 branch's generated demo:
```
book.json                      {"name":"book.mymod.guide.name","generate_book_item":true,"display_mode":"node","model":"modonomicon:modonomicon_green", "creative_tab":"modonomicon:modonomicon", ...}
theme.json
categories/<cat>.json          {"name":"...","icon":{"id":"minecraft:paper"},"display_mode":"node","sort_number":0,"background":"modonomicon:textures/gui/dark_slate_seamless.png",...}
entries/<cat>/<entry>.json     {"type":"modonomicon:content","category":"mymod:<cat>","name":"...","icon":{"id":"..."},"x":0,"y":0,"condition":{"type":"modonomicon:none"},...}
entries/<cat>/<entry>/pages/<page>.json  {"type":"modonomicon:text","title":"...","text":"...","sort_number":0}
```
Datagen providers live in `com.klikli_dev.modonomicon.api.datagen.*`. Gotcha from the docs: if Fabric datagen starts emitting Modonomicon's own files, restrict it with `fabricApi { configureDataGeneration { modId = "<your_mod_id>" } }`. Page JSON shapes beyond `modonomicon:text` are UNVERIFIED here; read `common/.../book/page/` in the repo.

---

## 6. Server-side (vanilla-client compatible) stack: Patbox / Nucleoid
All of these are at `https://maven.nucleoid.xyz/` (already in fabric-mod.gradle). Jar-in-jar them with `include`. They work on dedicated servers and in singleplayer, and clients need no mod.

### Polymer 0.18.7+26.3 (all modules OK)
Artifacts (all `eu.pb4:<artifact>:0.18.7+26.3`, maven 200):
| Artifact | Purpose |
|---|---|
| `polymer-common` | shared base (pulled in transitively) |
| `polymer-core` | server-side items, blocks, block entities, entities, item groups, `/polymer` commands |
| `polymer-blocks` | textured server-side blocks via resource-pack model swaps on spare vanilla block states (`PolymerTexturedBlock`, `PolymerBlockResourceUtils`, `BlockModelType`) |
| `polymer-resource-pack` | builds a resource pack from mod assets (`PolymerResourcePackUtils`) |
| `polymer-resource-pack-extras` | extra RP helpers (UNVERIFIED specifics) |
| `polymer-autohost` | hosts and sends the generated RP to players |
| `polymer-virtual-entity` | packet-only display/interaction entities (`ElementHolder`, `BlockDisplayElement`, `ItemDisplayElement`, `TextDisplayElement`, attachments `ChunkAttachment`/`EntityAttachment`/`BlockBoundAttachment`) |
| `polymer-networking` | versioned custom packets that work with optional client mods |
| `polymer-reg-sync-manipulator` | keeps polymer entries out of Fabric registry sync |
| `polymer-sound-patcher` | sound helpers (UNVERIFIED) |
| `polymer-bundled` | all of the above in one artifact |
```groovy
dependencies {
    implementation include("eu.pb4:polymer-core:0.18.7+26.3")
    implementation include("eu.pb4:polymer-resource-pack:0.18.7+26.3")
    implementation include("eu.pb4:polymer-blocks:0.18.7+26.3")
    implementation include("eu.pb4:polymer-virtual-entity:0.18.7+26.3")
}
```
API (src: polymer dev/26.3 `polymer-core/.../api/item/PolymerItem.java`, `api/block/PolymerBlock.java`):
```java
// Item with a custom model sent to vanilla clients (model from the item_model component; RP provides it)
public class Ruby extends SimplePolymerItem { public Ruby(Item.Properties p) { super(p, Items.EMERALD); } } // SimplePolymerItem(Properties[, Item polymerItem[, boolean useModel]])
// or implement PolymerItem:  Item getPolymerItem(ItemStack stack, PacketContext ctx);
//    optional: Identifier getPolymerItemModel(ItemStack, PacketContext, HolderLookup.Provider); void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext, HolderLookup.Provider)
// Block shown as a vanilla state:
public class MyBlock extends Block implements PolymerBlock {
    @Override public BlockState getPolymerBlockState(BlockState state, @Nullable PacketContext ctx) { return Blocks.STONE.defaultBlockState(); } }
// Textured block (polymer-blocks):
static final BlockState MODEL = PolymerBlockResourceUtils.requestBlock(BlockModelType.FULL_BLOCK, PolymerBlockModel.of(Identifier.fromNamespaceAndPath("mymod", "block/ruby_block")));
public class RubyBlock extends Block implements PolymerTexturedBlock { public BlockState getPolymerBlockState(BlockState s, PacketContext c) { return MODEL; } }
// Resource pack:
PolymerResourcePackUtils.addModAssets("mymod"); PolymerResourcePackUtils.markAsRequired();
// Server-side creative tab: PolymerItemGroupUtils.builder()... ; PolymerItemGroupUtils.registerPolymerItemGroup(id, group)
```
Polymer rules from its docs: code must be thread-safe (it can run on the network thread), never implement Polymer interfaces on vanilla classes via mixin, and never add block states to non-polymer blocks. Polymer's markdown docs still use Yarn names (`ServerPlayerEntity`, `ItemGroup`, `getNbt`). Use the Mojang names from the source.

### Placeholder API (Text Placeholder API) 3.2.0+26.3 - OK
Text formatting (QuickText/Simplified Text Format tags) plus `%namespace:placeholder%` placeholders. **The 3.x API changed**: there are separate server and common registries.
```java
Placeholders.registerServer(Identifier.fromNamespaceAndPath("mymod", "mana"),
    (ServerPlaceholderContext ctx, String arg) -> ctx.hasPlayer() ? PlaceholderResult.value(String.valueOf(Mana.get(ctx.player()))) : PlaceholderResult.invalid("no player")); // UNVERIFIED accessor names hasPlayer()/player()
// common (client+server): Placeholders.registerCommon(id, (PlaceholderContext ctx, String arg) -> ...)
Component c = NodeParser.builder().quickText().serverPlaceholders().build()
    .parseComponent("<red>Mana:</red> %mymod:mana%", ServerPlaceholderContext.of(player).asParserContext());
Component simple = TagParser.QUICK_TEXT.parseComponent("<b>hi</b>", ParserContext.of());
```
(src: TextPlaceholderAPI 26.3 `api/Placeholders.java`, `api/Placeholder.java` `Handler<Ctx,Arg>#onPlaceholderRequest`, `src/testmod/.../TestMod.java`.) Other parsers: `TagParser.SIMPLIFIED_TEXT_FORMAT`, `TagParser.QUICK_TEXT_WITH_STF`, `TagParser.DEFAULT`, `MarkdownLiteParserV1`, `LegacyFormattingParser`.

### sgui 2.2.1+26.3 - OK (maven only)
Server-side GUIs built on vanilla menus: chest, anvil text input, sign, book, merchant, hotbar.
```java
SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x3, player, false);   // (MenuType<?>, ServerPlayer, boolean manipulatePlayerSlots)
gui.setTitle(Component.literal("Shop"));
gui.setSlot(0, new GuiElementBuilder(Items.DIAMOND).setName(Component.literal("Buy")).glow()
    .setCallback((index, clickType, action, g) -> { /* ... */ }));
gui.open();
// override onClick(int index, ClickType type, ContainerInput action, GuiElement element), onTick(), canPlayerClose(), onClose()
// others: AnvilInputGui, SignGui, BookGui, MerchantGui, HotbarGui, layered/*
```
(src: sgui main `api/gui/SimpleGui.java`, `src/testmod/.../SGuiTest.java`. Note the vanilla 26.x click-action type is `ContainerInput`.)

### Server Translations API 3.2.0+26.3 - OK (maven only)
`implementation include("xyz.nucleoid:server-translations-api:3.2.0+26.3")`. Put lang files in **`data/<modid>/lang/en_us.json`** (not `assets/`) and use plain `Component.translatable("key")`. Translation happens per player, using the client's language. There's no other API to call.

### Permissions: lucko `fabric-permissions-api` vs Fabric API `fabric-permission-api-v1`
**Fabric API `fabric-permission-api-v1`** (in Fabric API 26.x; src: fabric-api `fabric-permission-api-v1/src/main/java/net/fabricmc/fabric/api/permission/v1/`):
- `PermissionContextOwner` is mixed into `Entity` and `CommandSourceStack` (mixins `EntityMixin`, `CommandSourceStackMixin`). It gives you `checkPermission(Identifier)` returning `TriState`, `checkPermission(Identifier, boolean default)`, `checkPermission(Identifier, PermissionLevel default)`, and typed `checkPermission(PermissionNode<T>, T default)`.
- `PermissionNode.of(id)` / `ofInteger(id)` / `ofString(id)` / `ofCustom(id, Codec<T>, Class<T>)` give boolean, int, string and custom values (the old "options/meta" concept).
- `PermissionPredicates.require(id[, default | PermissionLevel])` for Brigadier `.requires(...)`.
- Offline players: `PermissionContext.offlinePlayer(UUID or NameAndId, server)` returns `CompletableFuture<MutablePermissionContext>`.
- Providers hook `PermissionEvents.ON_REQUEST` and `PermissionEvents.PREPARE_OFFLINE_PLAYER`.
- The vanilla level type is `net.minecraft.server.permissions.PermissionLevel` with `isEqualOrHigherThan`.
```java
static final PermissionNode<Boolean> FLY = PermissionNode.of(Identifier.fromNamespaceAndPath("mymod", "fly"));
dispatcher.register(Commands.literal("fly").requires(PermissionPredicates.require(FLY, PermissionLevel.GAMEMASTERS)).executes(...)); // UNVERIFIED enum constant name; see PermissionLevel.java
if (player.checkPermission(FLY, false)) { ... }
int homes = player.checkPermission(PermissionNode.ofInteger(Identifier.fromNamespaceAndPath("mymod", "max_homes")), 3);
```
**lucko `me.lucko:fabric-permissions-api:0.7.0`** (Maven Central; fabric.mod.json id `fabric-permissions-api-v0`; package `me.lucko.fabric.api.permissions.v0`): `Permissions.check(entityOrSource, "mymod.fly"[, default | int opLevel])`, `Permissions.require("node", level)` for commands, `Options.get(...)`, plus offline-check events. The README's version matrix says 0.7.0 is for 26.1. Its Modrinth upload declares 26.1.2, 26.2 and 26.3.

**Which to use:** for new 26.3 code, use **Fabric's v1**. It needs no extra dependency and has typed values. **LuckPerms Fabric v5.5.85 implements both**: its `fabric.mod.json` depends on `fabric-permission-api-v1`, its `FabricPermissionsApiV1Listener` registers on `PermissionEvents.ON_REQUEST` and `PREPARE_OFFLINE_PLAYER`, and it jar-in-jars `me.lucko:fabric-permissions-api:0.7.0` (src: LuckPerms `fabric/build.gradle`, `fabric/src/main/resources/fabric.mod.json`). Use lucko v0 only for compatibility with older mods or providers that implement only v0. String nodes like `mymod.fly` and Identifier nodes like `mymod:fly` are different keys. Check how LuckPerms maps v1 Identifiers to node strings before you write docs for server admins (UNVERIFIED).

**LuckPerms API** (`net.luckperms:api:5.5`, Maven Central, `compileOnly`): `LuckPermsProvider.get().getUserManager().getUser(uuid)`. You rarely need it. Permission checks go through the APIs above.

---

## 7. Performance / rendering mods (compat targets, rarely compile deps)

| Mod | 26.3 status | Notes for mod authors |
|---|---|---|
| Sodium | 0.9.2+mc26.3 stable (2026-09-15); 0.9.3-beta.1-5 (2026-10-15 to 11-12) | `provides: ["indium"]`, so **Sodium implements Fabric Renderer API (FRAPI) itself and Indium is obsolete.** Its `breaks` list includes `iris <=1.12.0`. Every Iris release for 26.3 so far (latest 1.11.7) pins Sodium 0.9.2, so **with Iris, use Sodium 0.9.2, not the 0.9.3 betas.** Maven: `net.caffeinemc:sodium-fabric:0.9.2+mc26.3` at maven.caffeinemc.net/releases. API packages: `net.caffeinemc.mods.sodium.api.*` (e.g. `api.config.ConfigEntryPoint` for adding to Sodium's options screen, `api.blockentity.BlockEntityRenderPredicate`). |
| Iris | 1.11.7+26.3-fabric | Requires Sodium 0.9.x. Check for shaders with `net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse()` (class present in jar). `compileOnly "maven.modrinth:iris:1.11.7+26.3-fabric"`. |
| Lithium | mc26.3-0.26.3-fabric | Server/logic optimizations. No API needed. Modrinth maven only. |
| Indium | NO, and **not needed** | Last build was for 1.21.1. Sodium provides `indium`. |
| FerriteCore | 9.0.0-fabric | Memory. Covers 26.1.2-26.3. |
| ModernFix | NO on Fabric | Fabric stops at 1.21.4. The 26.1 branch publishes NeoForge only. |
| Entity Culling | 1.12.1 | Mods with entities or BEs that must always render can opt out (UNVERIFIED API; see tr7zw/EntityCulling). |
| Sodium Extra, MoreCulling, ImmediatelyFast, Continuity, C2ME, Krypton | have 26.3 builds (some beta/alpha) | |

Rendering gotcha: FRAPI custom models (`fabric-renderer-api-v1`) work with Sodium directly. Don't add Indium as a dependency.

---

## 8. Block/entity info overlays

### Jade - OK, 26.3.5+fabric (Modrinth maven)
```groovy
dependencies { compileOnly "maven.modrinth:jade:26.3.5+fabric"; localRuntime "maven.modrinth:jade:26.3.5+fabric" }
```
Fabric entrypoint key `jade` (src: Jade 26.3-fabric `src/main/resources/fabric.mod.json`):
```java
@WailaPlugin   // snownee.jade.api.WailaPlugin (needed on NeoForge; harmless on Fabric)
public class MyJade implements snownee.jade.api.IWailaPlugin {
    @Override public void register(IWailaCommonRegistration r) { r.registerBlockDataProvider(MyServerData.INSTANCE, GrinderBlockEntity.class); }
    @Override public void registerClient(IWailaClientRegistration r) { r.registerBlockComponent(MyTooltip.INSTANCE, GrinderBlock.class); }
}
public enum MyTooltip implements IBlockComponentProvider {
    INSTANCE;
    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) { tooltip.add(Component.literal("...")); }
    @Override public Identifier getUid() { return Identifier.fromNamespaceAndPath("mymod", "grinder"); }
}
```
`"entrypoints": { "jade": ["com.example.compat.MyJade"] }`. Docs: jademc.readthedocs.io.

### WTHIT - OK, fabric-21.0.0
Requires **Bad Packets** (`lol.bai:badpackets:fabric-0.12.3`). Artifacts use a loader prefix inside the version string: `wthit-api:fabric-21.0.0`, `wthit:fabric-21.0.0`, and `mojmap-21.0.0` for platform-independent code.
```groovy
repositories { maven { url = 'https://maven2.bai.lol'; content { includeGroup 'lol.bai'; includeGroup 'mcp.mobius.waila' } } }
dependencies {
    compileOnly "mcp.mobius.waila:wthit-api:fabric-21.0.0"
    localRuntime "mcp.mobius.waila:wthit:fabric-21.0.0"
    localRuntime "lol.bai:badpackets:fabric-0.12.3"
}
```
Plugins are declared in **`waila_plugins.json` or `wthit_plugins.json`** at the jar root, not in an entrypoint. In 21.x the format splits plugins into common and client, and `IWailaPlugin` is `@Deprecated`:
```json
{ "mymod:main": { "common": "com.example.compat.MyWthitCommon", "client": "com.example.compat.MyWthitClient" } }
```
```java
public class MyWthitClient implements mcp.mobius.waila.api.IWailaClientPlugin {
    @Override public void register(IClientRegistrar r) { r.body(new MyProvider(), GrinderBlock.class); }   // also head/tail/icon/override/redirect/dataContext/eventListener
}
public class MyWthitCommon implements mcp.mobius.waila.api.IWailaCommonPlugin { @Override public void register(ICommonRegistrar r) { /* server data providers */ } }
```
(src: wthit dev/26.3 `src/api/java/mcp/mobius/waila/api/IWailaClientPlugin.java`, `IClientRegistrar.java`, `platform/fabric/src/main/resources/waila_plugins.json`.)

---

## 9. Language, mixin and dev tooling

### Fabric Language Kotlin 1.14.2+kotlin.2.4.21 - OK
```groovy
plugins { id 'org.jetbrains.kotlin.jvm' version '2.4.21' }   // plugin portal pom verified 200
dependencies { implementation "net.fabricmc:fabric-language-kotlin:1.14.2+kotlin.2.4.21" }
```
```json
"entrypoints": { "main": [ { "adapter": "kotlin", "value": "com.example.MyMod" } ] },
"depends": { "fabric-language-kotlin": ">=1.14.2+kotlin.2.4.21" }
```
It bundles the Kotlin stdlib and common kotlinx libraries. `value` can be a class, an `object`, `pkg.MyModKt::init` (top-level function) or a field reference. Our toolchain is Java 25, so set Kotlin `jvmTarget` to 25 to match (UNVERIFIED that Kotlin 2.4.21 accepts JVM 25 as a target; fall back to 21 if it doesn't). Fzzy Config needs FLK at runtime.

### MixinExtras - bundled
Fabric Loader 0.20.1 bundles `io.github.llamalad7:mixinextras-fabric:0.5.5` and `net.fabricmc:sponge-mixin:0.17.5+mixin.0.8.7` (src: maven.fabricmc.net `fabric-loader-0.20.1.json`). Use `@WrapOperation`, `@ModifyExpressionValue`, `@WrapMethod`, `@Local`, `@Share` and the rest without adding a dependency.

### MixinSquared 0.3.8 - OK
Lets you mixin into other mods' mixin handlers, or cancel their mixins. Not tied to an MC version.
```groovy
repositories { maven { url = 'https://maven.bawnorton.com/releases' } }
dependencies { implementation include(annotationProcessor("com.github.bawnorton.mixinsquared:mixinsquared-fabric:0.3.8")) }
```
`@TargetHandler(mixin = "com.other.mixin.FooMixin", name = "handlerMethod")` on your injector, with `@Mixin(value = TargetClass.class, priority = 1500)`. To cancel another mod's mixin, implement `com.bawnorton.mixinsquared.api.MixinCanceller#shouldCancel(List<String> targetClassNames, String mixinClassName)` and register it under the **`mixinsquared`** entrypoint (src: MixinSquared `fabric/.../MixinSquaredApiImplLoader.java`).

### DevAuth (log in with a real Microsoft account in dev)
- **DevAuth Neo 1.2.0 (26.3, recommended):** `localRuntime "net.litetex.mcm:dev-auth-neo:1.2.0"` (Maven Central, 200) or `maven.modrinth:dev-auth-neo:1.2.0`. Enable it with `-Ddevauth.enabled=1` (or env `DEVAUTH_ENABLED`). It defaults to an embedded-browser auth-code flow, which downloads CEF (~150 MB) into `~/.dev-auth-neo/jcef`. Tokens are cached in `~/.dev-auth-neo/microsoft_accounts.json`.
- **Original DevAuth `me.djtheredstoner:DevAuth-fabric:1.2.2`** (Azure DevOps maven): its README lists Fabric 1.14 through 1.21.11. Its only mixin targets `net.minecraft.client.main.Main#main` with `remap=false`, so it may work on 26.x (UNVERIFIED). Enable with `-Ddevauth.enabled=true`.
- Dev-only: `localRuntime` doesn't end up in the published POM or jar. Never `include` it.

---

## 10. Misc libraries

### LambDynamicLights 4.13.0+26.3 - OK
```groovy
repositories { maven { url = 'https://maven.gegy.dev/releases/' } }
dependencies { compileOnly "dev.lambdaurora.lambdynamiclights:lambdynamiclights-api:4.13.0+26.3" }
```
- **Data-driven (preferred; needs no code or dependency).** Ship item light sources in a resource pack: `assets/<ns>/dynamiclights/item/<name>.json` with `{"match":{"items":["mymod:glow_wand"]},"luminance":10}`. Entity sources go in `dynamiclights/entity/` (src: `assets/lambdynlights/dynamiclights/item/blaze.json`, `resource/LightSourceLoader.java`).
- **Code:** use the entrypoint key `"lambdynlights:initializer"` with `DynamicLightsInitializer#onInitializeDynamicLights(DynamicLightsContext ctx)`. Then use `ctx.itemLightSourceManager()`, `ctx.entityLightSourceManager()` and `ctx.dynamicLightBehaviorManager()` (src: `api/src/main/java/dev/lambdaurora/lambdynlights/api/`). It's incompatible with Sodium Dynamic Lights and RyoamicLights.

### Player Animation Library (PAL, ZigyTheBird) 1.2.9 - BETA
This is the 26.x successor to KosmX playerAnimator, which has no 26.x build. Better Combat 3.2.2+26.3 requires it. It's a merged Fabric+NeoForge jar. Pin by version ID: `implementation "maven.modrinth:player-animation-library:lQIjpkZc"`. API not reviewed (UNVERIFIED).

### Pehkui - NOT available
The last release is for 1.21.1 (2024-06). Use the vanilla attribute **`minecraft:scale`** instead (`Attributes.SCALE`, range 0.0625-16.0, syncable; src: common `net/minecraft/world/entity/ai/attributes/Attributes.java`). Set it with an `AttributeModifier` or with `/attribute @s minecraft:scale base set 2`.

### Origins / Apoli - NOT available
The latest is `1.13.0-pre.4+mc.1.21.1` (2026-10-18). Development is still on 1.21.1.

### Create - NOT available on Fabric 26.3
Create Fabric (Fabricators of Create) is at 6.0.8.1 for 1.20.1 (2025-12). Main Create is 6.0.10 for NeoForge 1.21.1. Neither has 26.x.

### Cloth API - dead
Not on Modrinth (404). The last maven publish (`me.shedaniel.cloth.api:cloth-api` 4.0.65) is from 2022, and branches end at 1.19. Use Fabric API events instead. Don't confuse it with Cloth Config, which is alive.

---

## 11. Gotchas and recent changes (1.21.x to 26.x)

1. Every README still shows `modImplementation`. Strip the `mod` prefix (`implementation`, `compileOnly`, `localRuntime`).
2. GeckoLib changed its maven group and package: `software.bernie.geckolib` became `com.geckolib`, in both the artifact and the Java package.
3. CCA 8: Mojmap module renames (`-world` to `-level`, `-level` to `-leveldata`). The component base is now `org.ladysnake.cca.api.v8.component.CardinalComponent` with `readData(ValueInput)` / `writeData(ValueOutput)`. Item components are gone in favour of vanilla data components (migration only).
4. EMI, Patchouli, Lavender, owo-lib, Trinkets (original), Accessories, Moonlight, ModernFix (Fabric), Pehkui, Origins, Create and YUNG's API have **no Fabric 26.3 builds**. Mods that hard-depend on them can't be used on 26.3.
5. JEI on Fabric now also requires MezzConfig. JEI's Fabric plugin entrypoint is `jei_mod_plugin`.
6. Sodium provides `indium`, so Indium is obsolete. The Sodium 0.9.3 betas break every Iris release for 26.3 so far (`iris <=1.12.0`), so pair Iris 1.11.7 with Sodium 0.9.2.
7. Fabric API now has its own permission API (`fabric-permission-api-v1`) with typed nodes and `TriState` results. LuckPerms supports it.
8. Placeholder API 3.x split registration into `Placeholders.registerServer` and `registerCommon`, and parsing goes through `NodeParser.builder()...parseComponent(text, ParserContext)`.
9. WTHIT 21 splits plugins into `IWailaCommonPlugin` and `IWailaClientPlugin` (the single `IWailaPlugin` is deprecated), declared in `waila_plugins.json`.
10. Trinkets on 26.x means Patbox's fork, `eu.pb4.trinkets`. The API package differs from `dev.emi.trinkets`.
11. Modrinth maven: if a `version_number` is reused across MC versions, pin the version ID.
12. Mod Menu 21 bundles Placeholder API. You don't need to add it yourself.
13. YACL 3.9.8 and later are built for 26.4 snapshots. Pin `+26.3-fabric`.
14. Vanilla renames that show up in library APIs: `GuiGraphics` is now `GuiGraphicsExtractor` (JEI's `IRecipeCategory#draw`), click handling uses `ContainerInput` (sgui `onClick`), and rendering is submit-based (`SubmitNodeCollector` in Trinkets renderers). `Identifier` replaced `ResourceLocation`.

---

## 12. Recommendations for 26.3

| Need | Default pick | Fallback | Why |
|---|---|---|---|
| Config file + screen | **YACL 3.9.7+26.3-fabric** plus **Mod Menu 21.0.1** (`compileOnly`/optional) | **MidnightLib** for tiny configs (static fields, auto Mod Menu). **Cloth Config** if REI is already present. **Fzzy Config** if you need server-synced, validated config and accept the Kotlin runtime. FCAP for multiloader TOML. | YACL is actively maintained, has both autogen and a builder, JSON5 with comments, and only needs Fabric API. owo-config is not on 26.3. |
| Persistent data on entities/BEs/chunks/levels/server | **Fabric Data Attachment API** (in Fabric API) | **Cardinal Components API 8.1.0** when you need ticking, scoreboard/team/LevelData targets, respawn strategies, custom sync or interface-typed components. ItemStacks: vanilla data components. | No extra dependency, Codec-based persistence and StreamCodec sync, global attachments. CCA is still maintained for the advanced cases. |
| Animated entities/blocks/items | **GeckoLib 5.5.7** (`com.geckolib:geckolib-fabric-26.3`) | **AzureLib 4.1.8**. Player animations: **Player Animation Library 1.2.9** (beta). | GeckoLib has the largest user base, a 26.3 build and a maintained wiki. AzureLib is a viable fork with a different API. |
| Recipe viewer integration | **REI 26.3.824** (stable): `compileOnly` the API, plugin via `rei_client`/`rei_common` | **JEI 31.17.0.113** (beta, needs MezzConfig), entrypoint `jei_mod_plugin`. Also consider RRV. | EMI doesn't exist on 26.x. REI is the only stable mainstream viewer. Supporting REI plus JEI covers most packs. Keep all integrations optional. |
| Server-side-only content (vanilla clients) | **Polymer 0.18.7+26.3** (`polymer-core` + `-resource-pack` + `-blocks` + `-virtual-entity` + `-autohost`), with **sgui** for menus, **Placeholder API** for text and **Server Translations API** for i18n | Plain vanilla mechanics (data packs, display entities, dialogs) where they're enough | It's the only mature server-side content stack, it's maintained in lockstep with MC releases, and all modules have 26.3 builds. |
| In-game guide book | **Modonomicon 2.20.1** | Vanilla written books / dialogs via data pack, or a web wiki | Patchouli (stops at 26.1), Lavender (1.21.4) and GuideME (NeoForge-only) aren't on Fabric 26.3. |
| Permissions | **Fabric `fabric-permission-api-v1`** (in Fabric API): `PermissionNode`, `PermissionPredicates.require(...)` | lucko **fabric-permissions-api 0.7.0** (`include` it) for compatibility with v0-only providers or older mods | No extra dependency, typed values, and LuckPerms 5.5.85 implements it (and bundles v0 too). |
| Accessory/trinket slots | **Trinkets Updated 4.2.1+26.3** (`eu.pb4:trinkets`) | none on Fabric 26.3 | The original Trinkets and Accessories have no 26.x builds. |
| Block/entity tooltip overlay | **Jade 26.3.5** (entrypoint `jade`) | **WTHIT 21.0.0** (`waila_plugins.json`) | Jade has about 17x WTHIT's downloads. Both have 26.3 builds. |
| Dynamic lights | **LambDynamicLights** data-driven JSON (no dependency) | LDL API entrypoint `lambdynlights:initializer` | The JSON works without a compile dependency. |
| Kotlin | **Fabric Language Kotlin 1.14.2+kotlin.2.4.21** + Kotlin Gradle plugin 2.4.21 | - | The only Kotlin adapter. |
| Dev login | **DevAuth Neo 1.2.0** (`localRuntime`) | original DevAuth 1.2.2 (UNVERIFIED on 26.x) | The fork targets 26.3 explicitly. |

### Not available for 26.3 (do not plan on these)
EMI, Patchouli, Lavender, owo-lib (26.2 only), Trinkets (original `dev.emi`), Accessories, Moonlight Lib (Fabric), ModernFix (Fabric), Indium (obsolete), Pehkui, Origins/Apoli, Create (Fabric), YUNG's API (Fabric), playerAnimator (KosmX), Curios (NeoForge-only anyway), GuideME (NeoForge-only), Cloth API (dead).

### Where to look for more
- Live availability: `curl -sG https://api.modrinth.com/v2/project/<slug>/version --data-urlencode 'loaders=["fabric"]' --data-urlencode 'game_versions=["26.3"]'`.
- Available maven versions: `curl -s <repo>/<group/as/path>/<artifact>/maven-metadata.xml`.
- Library source for 26.3: GitHub branches are `26.3` (ModMenu, Architectury, REI, JEI, Balm, LDL, AzureLib, CCA), `v26.3` (ClothConfig), `port/26.3` (YACL), `dev/26.3` (Polymer, WTHIT), `26.3-fabric` (Jade), `version/26.3` (Modonomicon), `26.3.x` (FCAP, Puzzles Lib), `main` (GeckoLib, sgui, Server-Translations, Trinkets Updated), `master` (Fzzy Config).
- Fabric API modules referenced here: `/tmp/claude-0/mcsrc/fabric-api/fabric-data-attachment-api-v1/` and `/tmp/claude-0/mcsrc/fabric-api/fabric-permission-api-v1/` (testmods show usage).
