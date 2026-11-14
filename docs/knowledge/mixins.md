# Mixins on Fabric 26.x (Sponge Mixin 0.8.7 Fabric fork + MixinExtras 0.5.5)

Scope: writing, debugging and keeping mixins compatible on unobfuscated MC 26.x with Fabric Loader 0.20.1. Verified 2026-11-14 against Loader 0.20.1 (bundles `net.fabricmc:sponge-mixin:0.17.5+mixin.0.8.7`, `io.github.llamalad7:mixinextras-fabric:0.5.5`, ASM 9.10.1), MC 26.3 source and Fabric API 0.166.2+26.3 mixins.

Sources (short names): `loader-jar` = `fabric-installer.json` and `META-INF/jars/` inside fabric-loader-0.20.1.jar; `sm-jar` = sponge-mixin 0.17.5 (javap); `mx-jar` = mixinextras-fabric 0.5.5 (javap); `fapi` = `/tmp/claude-0/mcsrc/fabric-api/**/mixin/**`; `common`/`client` = 26.3 decompiled source; `fabric-docs` = `/tmp/claude-0/mcsrc/fabric-docs/develop/mixins/*`.

---

## 0. What is different on 26.x (read first)

- **Mojang names at runtime.** MC 26.1+ is unobfuscated. The names you see in the decompiled source (`LivingEntity#actuallyHurt`) are the runtime names. There is no intermediary, so **no refmap** is needed or generated. Built jars contain none (checked the refmod jar and fapi's `fabric-item-api-v1` jar), and no fapi `*.mixins.json` has a `refmap` key. Leave `remap` at its default; it does nothing useful now. Copying a 1.21 mixin that targets `method_1234`/`field_5678` or Yarn names (`PlayerEntity`, `onUse`) will fail with "no targets found". Rename everything to Mojang names.
- **Real local variable and parameter names exist** in the bytecode LVT. MixinExtras `@Local(name = "...")` is now the stable way to capture locals; fapi uses it widely, e.g. `@Local(name = "belowState") BlockState belowState` (fapi `fabric-block-api-v1/.../mixin/block/LivingEntityMixin.java`). Prefer it over `ordinal`/`index`.
- **Lambdas are compiled synthetic methods** `lambda$<enclosingMethod>$<n>`, and you target them by that name: `method = "lambda$slotsChanged$0"` (fapi `EnchantmentMenuMixin`), `"lambda$load$0"` (fapi `WorldLoaderMixin`). These numbers can shift between MC versions. Re-check them every port.
- **Java 25 bytecode**: use `"compatibilityLevel": "JAVA_25"` (the highest enum value in sm-jar `MixinEnvironment$CompatibilityLevel`). Vanilla uses JEP 447 (statements before `super(...)`) in constructors, so constructor `HEAD` is not "before super" in the usual sense; see 4.9.
- **The `fabricloader` version you `depends` on selects the Mixin behaviour level.** `>=0.20.0` gives compat 0.17.5. Enum extension needs `>=0.19.0` (toolchain.md section 8).
- The decompiled source shows **injected interfaces** (`Item.Properties implements FabricItem.Properties`, `ValueInput extends FabricValueInput`). Those come from Fabric API class tweakers, not vanilla. Do not target them as vanilla methods.
- Many 1.21 targets were renamed or removed in 26.1-26.3 (render pipeline rewrite, `Screen#render` becoming `extractRenderState`, `Entity#interactAt` removal, `AxeItem` and others gone in 26.3). See `porting-26x.md`.

---

## 1. Mixin config JSON

`src/main/resources/<modid>.mixins.json` (common) and `src/client/resources/<modid>.client.mixins.json` (client):

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.example.mixin",
  "compatibilityLevel": "JAVA_25",
  "mixins": ["LivingEntityMixin", "accessor.ServerLevelAccessor"],
  "client": [],
  "server": [],
  "injectors": { "defaultRequire": 1 },
  "overwrites": { "requireAnnotations": true },
  "mixinextras": { "minVersion": "0.5.0" }
}
```

Recognised keys (sm-jar `MixinConfig` `@SerializedName`s): `required, minVersion, package, compatibilityLevel, mixins, client, server, priority, mixinPriority, plugin, refmap, refmapWrapper, setSourceFile, verbose, target, parent, requiredFeatures, injectors {defaultRequire, defaultGroup, maxShiftBy, namespace, injectionPoints, dynamicSelectors}, overwrites {conformVisibility, requireAnnotations}`. MixinExtras adds `"mixinextras": {"minVersion": "0.5.0"}`. Set that whenever you use `@Expression`; fapi does (e.g. `fabric-entity-events-v1.mixins.json`).

Register the configs in fabric.mod.json:

```json
"mixins": [
  "example-mod.mixins.json",
  { "config": "example-mod.client.mixins.json", "environment": "client" }
]
```

Rules:
- `package` must contain **only** mixin classes. Loader and Mixin refuse to load classes from a mixin package normally, so helpers, interfaces you inject, events and so on go elsewhere. Config file names must be globally unique; Loader errors with `Non-unique Mixin config name %s used by the mods %s and %s`.
- `defaultRequire: 1` makes every injector that matches nothing a hard error at startup. Keep it on. You learn about broken targets on the next port instead of getting silent no-ops.
- Client-only targets (`net.minecraft.client.*`) go in the client config, and that config is registered with `"environment": "client"`, **or** in the `"client"` array of a shared config. With split source sets, put the class in `src/client/java`. A client mixin that loads on a dedicated server crashes with `ClassNotFoundException`/`NoClassDefFoundError` for `net/minecraft/client/...`.
- A `plugin` (`IMixinConfigPlugin`: `shouldApplyMixin(targetClassName, mixinClassName)`, `preApply`, `postApply`, `getMixins()`) makes mixins conditional, e.g. only when another mod is loaded. Use `FabricLoader.getInstance().isModLoaded(...)` inside it. Examples: fapi `ClientGameTestMixinConfigPlugin`, `IndigoMixinConfigPlugin`.
- Mixin classes should be `abstract` (classes) or `interface` (accessors). That way you can extend the target's superclass and declare `@Shadow abstract` methods without implementing them.

---

## 2. Anatomy of a mixin class

```java
package com.example.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)                       // or @Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class LivingEntityMixin extends Entity {   // extend the target's SUPERCLASS to call inherited methods
    private LivingEntityMixin(EntityType<?> type, Level level) { super(type, level); } // never called

    @Shadow public abstract float getHealth();                // src: common net/minecraft/world/entity/LivingEntity.java
    @Shadow public int hurtTime;                              // public field
    @Shadow @Final private net.minecraft.world.damagesource.CombatTracker combatTracker; // private final field
    @Shadow protected int noActionTime;

    @Unique private int example_mod$hits;                     // new field; @Unique -> renamed on clash
    @Unique private void example_mod$count() { example_mod$hits++; }

    @Inject(method = "actuallyHurt", at = @At("HEAD"))
    private void example_mod$onHurt(ServerLevel level, DamageSource source, float dmg, CallbackInfo ci) {
        example_mod$count();
    }
}
```

- `@Mixin(value = X.class, priority = 1000)`: default priority 1000. A lower value is applied first. For `@Overwrite`/merged members a higher priority wins; for injectors it only orders them. Use `targets = "..."` (binary name with `$`) for private, package-private or anonymous classes such as `"net.minecraft.world.level.chunk.LevelChunkSection$1BlockCounter"` (fapi `ChunkSectionBlockStateCounterMixin`).
- `@Shadow` references an existing member. Its declaration (name, type, static-ness) must match. Add `@Final` for final fields and `@Mutable` if you assign to them. For shadowed private methods, declare them `private` too, or `abstract` in an abstract mixin.
- `@Unique` marks members that are only yours. Always prefix them with `<modid>$` (fabric-docs convention). For private `@Unique` members Mixin renames on conflict; public ones conflict and crash.
- `@Overwrite` replaces a whole method body, and it requires `overwrites.requireAnnotations` plus the javadoc `@author`/`@reason` tags. **Avoid it.** fapi has exactly 1 use. It breaks every other mod touching that method.
- `@Intrinsic` and `@Implements`/`@Interface` (soft implementation): rare. Prefer interface injection (section 6).
- `@Pseudo` lets you target a class that may not exist (another mod). It also needs `@Mixin(targets="...")`.
- `@Debug(export = true, print = true)` on a mixin class exports and prints the transformed target for that class only.
- Accessing the target instance: `((LivingEntity) (Object) this)`. Calling your mixin's added interface from outside: `((MyDuck) entity).example_mod$foo()`. For a final target class, cast through `Object` first.

---

## 3. Target selectors and descriptors

- `method = "name"` matches every overload with that name. That is fine if there is only one, otherwise it is ambiguous and may apply to several overloads. Disambiguate with a descriptor: `method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V"` (fapi `PacketEncoderMixin`).
- Constructors are `"<init>"` (or `"<init>(Lnet/minecraft/...;)V"`); the static initialiser is `"<clinit>"`. Arrays work too: `method = {"foo", "bar"}`.
- `@At(target = ...)` uses the fully qualified member form `Lowner;name(args)ret` for methods and `Lowner;name:Ltype;` for fields:
  - `"Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"`
  - `"Lnet/minecraft/tags/FluidTags;WATER:Lnet/minecraft/tags/TagKey;"`
  - For `NEW`, target the class `"net/minecraft/world/damagesource/CombatTracker"` or a constructor descriptor `"(Ljava/lang/String;Ljava/lang/Throwable;)Lio/netty/handler/codec/EncoderException;"` (fapi `IdDispatchCodecMixin`).
- Descriptor letters: `Z` boolean, `B` byte, `C` char, `S` short, `I` int, `J` long, `F` float, `D` double, `V` void, `Lpkg/Cls;` object, `[` array. Inner classes use `$`: `Lnet/minecraft/world/item/Item$Properties;`. Generics are erased (`Ljava/util/List;`). See fabric-docs `mixins/bytecode.md` for a full bytecode primer: stack, locals, conditionals, lambdas, `new` patterns.
- The owner in an `INVOKE` target is the class named at the call site (the static receiver type in source), not the class that declares the method. If source calls `this.setHealth(...)` inside `LivingEntity`, the owner is `LivingEntity`. Check this with IntelliJ's bytecode viewer or MCDev completion.
- `@Desc(...)` (`target = @Desc(...)` on injectors, `desc = @Desc` on `@At`) is an alternative structured form. It is rarely used.
- `ordinal = N` on `@At` picks the Nth match (0-based) among the instructions matching that `@At` (within the slice). Omitting it means "all matches". `allow`/`expect`/`require` on the injector bound the count. Example: `@ModifyArg(..., at = @At(value = "INVOKE", target = "...", ordinal = 0), index = 2, allow = 1)` (fapi `WorldLoaderMixin`).
- Use MCDev (IntelliJ) for target completion and `Copy Mixin target`. On https://mcsrc.dev, right-click to copy mixin targets.

### `@At` values (sm-jar `injection/points/*` `@AtCode`s, plus MixinExtras)

| Value | Matches | Notes |
|---|---|---|
| `HEAD` | first instruction | In constructors: see 4.9 |
| `RETURN` | every `xRETURN` | Inject sees the return value via `CallbackInfoReturnable#getReturnValue` |
| `TAIL` | the **last** `RETURN` only | "end of method" |
| `INVOKE` | before a method call matching `target` | `shift = At.Shift.AFTER` for after the call |
| `INVOKE_ASSIGN` | after a call whose result is stored to a local | For `@ModifyVariable`/`@Inject` after `x = foo()` |
| `INVOKE_STRING` | call to a method taking one String constant | `args = "ldc=someString"` |
| `FIELD` | `GETFIELD/PUTFIELD/GETSTATIC/PUTSTATIC` matching `target` | `opcode = Opcodes.PUTFIELD` to narrow |
| `NEW` | `NEW` of class or ctor descriptor `target` | |
| `CONSTANT` | load of a constant | `args = {"intValue=5"}`, `floatValue=`, `stringValue=`, `classValue=`, `nullValue=true`, `expandZeroConditions=...` |
| `JUMP` | jump instruction | `opcode = Opcodes.IFEQ` etc. |
| `CTOR_HEAD` | constructor head with enforcement | needs `unsafe = true`; `args = "enforce=POST_DELEGATE"` (also `POST_INIT`, `PRE_BODY`, `DEFAULT`) |
| `LOAD` / `STORE` | local variable load or store | Only for `@ModifyVariable` (sm-jar `modify/BeforeLoadLocal`, `AfterStoreLocal`) |
| `MIXINEXTRAS:EXPRESSION` | matches an `@Expression` | MixinExtras 0.5+ |

`shift` takes `NONE`, `BEFORE`, `AFTER` or `BY` (with `by = n`; capped by `maxShiftBy`, default 0 to 3). `@At(id=...)` and `@At(slice=...)` refer to named slices.

### Slices
Restrict where an `@At` searches:

```java
@Inject(method = "actuallyHurt",
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"),
                       to   = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V")),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getAbsorptionAmount()F", ordinal = 0))
```

Slices are inclusive. Multiple named slices are possible with `@Slice(id = "a", ...)` plus `@At(slice = "a")`.

---

## 4. Injectors (Mixin core)

All examples target `LivingEntity#actuallyHurt(ServerLevel, DamageSource, float)` (src: common `net/minecraft/world/entity/LivingEntity.java:1951`) unless noted. Its body calls `getDamageAfterArmorAbsorb(source, dmg)`, `getDamageAfterMagicAbsorb`, `setAbsorptionAmount`, `setHealth(getHealth() - dmg)` and `this.gameEvent(GameEvent.ENTITY_DAMAGE)`, and it contains the constant `3.4028235E37F`.

### 4.1 `@Inject` with `CallbackInfo` / `CallbackInfoReturnable`
```java
@Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
private void example_mod$cancel(ServerLevel level, DamageSource source, float dmg, CallbackInfo ci) {
    if (source.is(DamageTypeTags.IS_FALL)) ci.cancel();              // void method -> CallbackInfo
}

@Inject(method = "getMaxHealth", at = @At("RETURN"), cancellable = true)
private void example_mod$max(CallbackInfoReturnable<Float> cir) {   // non-void -> CallbackInfoReturnable<T> (boxed)
    cir.setReturnValue(cir.getReturnValue() * 2);                   // setReturnValue implies cancel
}
```
- The handler takes the target's parameters (all of them, or none), then `ci`/`cir`, then optional `@Local` captures. It must be `void` and private. Make it `static` only if the target is static.
- `cancellable = true` is required to call `cancel()`/`setReturnValue()`.
- Cancelling at HEAD skips everyone else's later injections and vanilla logic, so it is the most incompatible way to do things. Cancel only conditionally.
- Avoid the legacy `locals = LocalCapture.CAPTURE_FAILHARD` style. It is brittle and every local must be listed. Use `@Local`.

### 4.2 `@Redirect` (avoid; use `@WrapOperation`)
Replaces one call or field access. **Only one mod can redirect a given instruction**; a second one crashes or gets skipped. fapi still has 58 for historical reasons, but new code should use `@WrapOperation` (120 uses in fapi), `@ModifyExpressionValue` or `@WrapWithCondition`.

### 4.3 `@ModifyArg` / `@ModifyArgs`
```java
@ModifyArg(method = "actuallyHurt",
           at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"),
           index = 0)
private float example_mod$clampHealth(float health) { return Math.max(health, 1.0F); }
```
`index` picks the argument (0-based); you can omit it if only one argument has that type. Add the target's parameters after the modified argument to receive them. `@ModifyArgs(... ) void m(Args args)` (`args.get(i)`, `args.set(i, v)`) allocates on every call and breaks on some targets. Prefer several `@ModifyArg`s.

### 4.4 `@ModifyVariable`
```java
@ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true)
private float example_mod$halve(float dmg) { return dmg / 2; }
```
Selection is by type, plus `ordinal` (Nth local of that type), `index` (LVT slot), `name` (works on 26.x thanks to the LVT) or `argsOnly`. Pick the injection point carefully: `STORE` (an `@At("STORE")` value accepted by `@ModifyVariable`) and `LOAD` are common. Prefer `@ModifyExpressionValue` on the expression that produces the value.

### 4.5 `@ModifyConstant` (prefer MixinExtras)
```java
@ModifyConstant(method = "actuallyHurt", constant = @Constant(floatValue = 3.4028235E37F))
private float example_mod$cap(float original) { return Float.MAX_VALUE; }
```
Note that some constants are inlined by javac. A `static final` primitive or String from any class (even another class's constant) gets copied into the using method, so you have to target the literal. 0 and 1 compare-to-zero forms need `expandZeroConditions`. `@ModifyExpressionValue(at = @At(value = "CONSTANT", args = "floatValue=..."))` is the chainable alternative.

### 4.6 `@Surrogate`, `@Group`, `@Coerce`
- `@Group(name, min, max)` lets one of several alternative injectors match.
- `@Coerce` on a handler parameter accepts a supertype, or a type you cannot reference (a private class), as `Object`. fapi uses it 5 times.

### 4.7 Injector options common to all
`require` (min matches, else error), `expect` (min, warning only; with `-Dmixin.debug.countInjections`), `allow` (max), `order` (sm 0.17 and MixinExtras support an `order` int; default 1000, lower applies first), `remap` (irrelevant on 26.x), `id`, `constraints`.

### 4.8 Static and interface targets
Fabric's fork supports injectors in interface mixins. Static targets need static handlers. Mixing into an **interface** (default or static methods) is allowed. Add new methods through an injected interface (section 6) rather than `@Unique` public methods on interface mixins. Accessor interfaces (section 5) are interfaces too, but they are a different thing.

### 4.9 Constructors
- Inject at `@At("TAIL")` or `@At("RETURN")` to run after construction (field initialisers done).
- Fabric relaxes the upstream restriction, so default injection points like HEAD work inside `<init>` (fabric-docs `loader/index.md`: "allowing all default injection points inside constructors"). On 26.x, vanilla constructors may run code **before** `super(...)` (JEP 447), so "HEAD" may precede super. To be explicit, use `@At(value = "CTOR_HEAD", unsafe = true, args = "enforce=POST_DELEGATE")` (after the `super()`/`this()` call) or `enforce=POST_INIT` (after field initialisers).
- Use `@ModifyArg` on the `super(...)` INVOKE (`target = "Lnet/minecraft/world/entity/Entity;<init>(...)V"`) to alter what the parent receives.
- You cannot cancel a constructor.

### 4.10 Records, enums, lambdas, inner and anonymous classes
- **Records** (e.g. `ChunkPos` is a record since 26.1; `ToolMaterial`, `ItemStackTemplate`): they are final classes with final fields. `@Inject` into the canonical constructor `<init>` or accessor methods works. Adding fields with `@Unique` works but they are not part of `equals`/`hashCode`/codec. Changing a component value needs `@Mutable @Shadow @Final` plus an assignment, or `@ModifyVariable(argsOnly = true)` at ctor HEAD. A sealed interface such as `InteractionResult` cannot get new implementors.
- **Enums**: inject as normal. Adding constants means enum extension (toolchain.md section 10), meaning an `enum` mixin.
- **Lambdas**: target `lambda$outer$N`. The parameters are captured variables first, then the lambda parameters. A method reference (`Foo::bar`) does not create a synthetic method, so target `bar` itself or wrap the `invokedynamic` creation site. MixinExtras `@WrapOperation` can wrap the lambda call site by targeting the functional interface `INVOKE` (e.g. `Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V`).
- **Anonymous and local classes**: `@Mixin(targets = "net.minecraft.network.protocol.common.custom.CustomPacketPayload$1")` (fapi `CustomPayloadStreamCodecMixin`). Names like `$1` or `$1BlockCounter` are compiler-assigned and fragile.

---

## 5. Accessors and invokers (`@Accessor`, `@Invoker`)

Use these to read and write private fields or call private methods without a class tweaker. They work on mod classes too and need no Gradle refresh. (src: fabric-docs `mixins/accessors.md`; fapi has 99 `@Accessor`s and 28 `@Invoker`s.)

```java
package com.example.mixin.accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("noActionTime") int example_mod$getNoActionTime();      // instance getter: prefix with modid
    @Accessor("noActionTime") void example_mod$setNoActionTime(int v); // setter
    @Accessor("combatTracker") @Mutable void example_mod$setCombatTracker(CombatTracker t); // final field -> @Mutable

    @Invoker("getDamageAfterArmorAbsorb")
    float example_mod$invokeArmor(DamageSource source, float damage);
}

@Mixin(SomeClass.class)
public interface SomeClassAccessor {
    @Accessor("SOME_STATIC") static Map<String, Integer> getSomeStatic() { throw new AssertionError(); }
    @Invoker("<init>") static SomeClass create(int a, String b) { throw new AssertionError(); } // constructor invoker
}

// use
int t = ((LivingEntityAccessor) entity).example_mod$getNoActionTime();
((FinalTargetAccessor) (Object) finalTargetInstance).example_mod$x();   // final target class: cast through Object
```

- An accessor interface may contain only `@Accessor`/`@Invoker` methods. It goes in the mixin package and is listed in the config (`"accessor.LivingEntityAccessor"`).
- The name can be inferred from `getX`/`setX`/`isX`/`callX`/`invokeX`, but always give it explicitly. Modid-prefixed method names cannot be inferred.
- Accessors cannot make a private **type** usable in your signatures. Use a class tweaker `accessible class`, or `@Coerce`/`Object`.

---

## 6. Interface injection ("duck interfaces")

Two variants:
1. **Plain duck**: `public interface ExampleDuck { int example_mod$getHits(); }` outside the mixin package. The mixin `implements ExampleDuck` and implements the method. Callers cast: `((ExampleDuck) entity).example_mod$getHits()`.
2. **Class-tweaker injected**, visible at compile time with no cast:
   ```java
   package com.example.api;                         // NOT the mixin package
   public interface BucketEmptySoundGetter {
       default Optional<SoundEvent> example_mod$getBucketEmptySound() { throw new UnsupportedOperationException(); }
   }
   ```
   ```
   inject-interface  net/minecraft/world/level/material/FlowingFluid  com/example/api/BucketEmptySoundGetter
   ```
   ```java
   @Mixin(FlowingFluid.class)
   abstract class FlowingFluidMixin extends Fluid implements BucketEmptySoundGetter {
       @Override public Optional<SoundEvent> example_mod$getBucketEmptySound() {
           return Optional.of(this.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY);
       }
   }
   ```
   (src: fabric-docs `class-tweakers/interface-injection.md`, reference `FlowingFluidMixin.java`.) Methods must be `default` for the class tweaker. Subclasses may override.

---

## 7. MixinExtras 0.5.5 (bundled, no setup)

Loader bundles MixinExtras as a nested mod (`META-INF/jars/mixinextras-fabric-0.5.5.jar`, mod id `mixinextras`) and initialises it. Do not shade it or call `MixinExtrasBootstrap`. Packages: `com.llamalad7.mixinextras.injector.*`, `.injector.wrapoperation.*`, `.injector.wrapmethod.*`, `.sugar.*`, `.expression.*` (src: mx-jar).

These injectors **chain**: several mods can apply them to the same instruction. That is why they are preferred over `@Redirect`, `@ModifyConstant`, `@Overwrite` and cancelling `@Inject`.

### `@ModifyExpressionValue`: change the result of a call, field read, constant, `new` or instanceof
```java
@ModifyExpressionValue(method = "actuallyHurt",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
private float example_mod$armor(float original) { return original * 0.9F; }
```

### `@ModifyReturnValue`: change what a method returns (at each `RETURN`)
```java
@ModifyReturnValue(method = "getMaxHealth", at = @At("RETURN"))
private float example_mod$max(float original) { return original + 4; }
```

### `@ModifyReceiver`: change the object a call is invoked on
`@ModifyReceiver(method=..., at=@At(value="INVOKE", target=...)) private Foo m(Foo receiver, <call args...>)`.

### `@WrapOperation`: wrap a call, field get/set, `instanceof` or `new`; call `original` zero or more times
```java
@WrapOperation(method = "actuallyHurt",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
private void example_mod$wrapSetHealth(LivingEntity instance, float health, Operation<Void> original) {
    if (!example_mod$godMode(instance)) original.call(instance, health);   // receiver first for instance calls
}
```
- `Operation<R>#call(Object... args)` takes the receiver (for non-static calls) followed by the arguments. Passing the wrong count throws `IncorrectArgumentCountException`.
- Primitive returns are boxed (`Operation<Float>`, `Operation<Void>`).
- Wrapping a field `GETFIELD` gives `(Owner instance, Operation<T>)`. `PUTFIELD` gives `(Owner instance, T value, Operation<Void>)`.

### `@WrapWithCondition`: skip a void call (or field set) conditionally
```java
@WrapWithCondition(method = "actuallyHurt",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;gameEvent(Lnet/minecraft/core/Holder;)V"))
private boolean example_mod$noDamageEvent(LivingEntity instance, Holder<GameEvent> event) { return !example_mod$quiet; }
```
The call-site owner is `LivingEntity` even though `gameEvent(Holder)` is declared in `Entity` (src: common `Entity.java:1455`). Verified with `javap -c` on the real jar: `invokevirtual gameEvent:(Lnet/minecraft/core/Holder;)V` with owner `LivingEntity`. Using `Entity` as the owner here would match nothing.

### `@WrapMethod`: wrap an entire method (the chainable replacement for `@Overwrite` and HEAD cancelling)
```java
@WrapMethod(method = "decode")
private void wrapWithContext(ChannelHandlerContext ctx, ByteBuf input, List<Object> out, Operation<Void> original) {
    ScopedValue.where(PacketContextImpl.VALUE, this.packetContext).run(() -> original.call(ctx, input, out));
}
```
(src: fapi `fabric-networking-api-v1/.../PacketDecoderMixin.java`.) The parameters are the target's parameters followed by `Operation<R>`, and it returns R. Not calling `original` replaces the method in a way that composes with other wrappers. fapi has 15 uses.

### Sugar: `@Local`, `@Share`, `@Cancellable`
```java
@Inject(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
private void example_mod$log(ServerLevel level, DamageSource source, float dmg, CallbackInfo ci,
                             @Local(name = "originalDamage") float originalDamage,      // by LVT name (26.x has names)
                             @Local(ordinal = 0) LocalFloatRef dmgRef) {                // mutable reference
    dmgRef.set(dmgRef.get() * 0.5F);
}
```
- `@Local` attributes (mx-jar): `name[]`, `ordinal`, `index`, `argsOnly`, `type`, `print` (prints the local table at that point to the log, a great debugging aid). Use `LocalRef<T>`/`LocalIntRef`/`LocalFloatRef`/etc. (`com.llamalad7.mixinextras.sugar.ref.*`) to **write** a local.
- `@Share("key") LocalRef<T> ref` shares a value between several handlers of the same mixin in the same target method invocation. fapi `LivingEntityMixin` in content-registries shares a `TagKey<Fluid>` between a `@ModifyExpressionValue` and a `@ModifyArg`. `namespace` sets the sharing scope.
- `@Cancellable CallbackInfo ci` adds cancellation to non-`@Inject` handlers such as `@WrapOperation`. fapi has 1 use.
- Sugar works on all injector types, including Mixin's own.

### `@Expression` / `@Definition` (MixinExtras 0.5): match by Java-like expression
```java
@Definition(id = "HalfTransparentBlock", type = HalfTransparentBlock.class)
@Expression("? instanceof HalfTransparentBlock")
@ModifyExpressionValue(method = "tesselate", at = @At("MIXINEXTRAS:EXPRESSION"))
private boolean modifyNonOverlayCheck(boolean original, @Local(name = "relativeBlock") Block block) { ... }
```
(src: fapi `fabric-rendering-fluids-v1/.../FluidRendererMixin.java`)

```java
@Definition(id = "WATER", field = "Lnet/minecraft/tags/FluidTags;WATER:Lnet/minecraft/tags/TagKey;")
@Definition(id = "getFluidHeight", method = "Lnet/minecraft/world/entity/LivingEntity;getFluidHeight(Lnet/minecraft/tags/TagKey;)D")
@Expression("this.getFluidHeight(WATER)")
@ModifyExpressionValue(method = "aiStep", at = @At("MIXINEXTRAS:EXPRESSION"))
```
(src: fapi content-registries `fluid/LivingEntityMixin.java`)

- `@Definition` takes `id` plus one of `method[]`, `field[]`, `type[]` or `local[]` (`@Local`), and optionally `remap`. `@Expression` takes `value[]` and `id`.
- `?` is a wildcard. Expressions can match calls, field accesses, `instanceof`, casts, `new`, arithmetic, comparisons, array access and literals.
- When the expression matches a whole subexpression, `@ModifyExpressionValue` modifies the value. Use `@At(value = "MIXINEXTRAS:EXPRESSION", ...)` with an `@Expression` containing `@(...)` to mark a sub-part (UNVERIFIED syntax details; see the MixinExtras wiki "Expressions").
- Add `"mixinextras": {"minVersion": "0.5.0"}` to the config.

Choosing an injector:

| You want to... | Use |
|---|---|
| Run code at a point, maybe cancel | `@Inject` (cancel sparingly) |
| Change a call's argument | `@ModifyArg` |
| Change a value produced by an expression | `@ModifyExpressionValue` |
| Change a method's return value | `@ModifyReturnValue` |
| Skip a void call | `@WrapWithCondition` |
| Replace, guard or decorate a call | `@WrapOperation` |
| Replace or decorate a whole method | `@WrapMethod` |
| Read or modify a local | `@Local` (+ `LocalRef`) |
| Pass data between your handlers | `@Share` |
| Match something awkward (instanceof, comparisons, nested) | `@Expression` |
| Change a constant | `@ModifyExpressionValue` on `CONSTANT`, else `@ModifyConstant` |
| Never | `@Overwrite`, `@Redirect` (unless unavoidable) |

---

## 8. Debugging

JVM flags (add to `loom.runs.client.jvmArguments` or IDE VM options; property names from sm-jar `MixinEnvironment$Option`):

| Flag | Effect |
|---|---|
| `-Dmixin.debug.export=true` | Writes every transformed class to `run/.mixin.out/class/**.class`. Open them in IntelliJ to see the real result |
| `-Dmixin.debug.export.decompile=true` | Also decompiles the exports (needs a decompiler on the classpath; UNVERIFIED with Vineflower) |
| `-Dmixin.debug.export.filter=net.minecraft.world.entity.*` | Restricts which classes are exported |
| `-Dmixin.debug.verbose=true` | Verbose logging of mixin application |
| `-Dmixin.debug=true` | Enables all debug options |
| `-Dmixin.debug.countInjections=true` | Warns when `expect` is not met |
| `-Dmixin.debug.strict=true`, `-Dmixin.debug.verify=true` | Stricter checks; bytecode verification |
| `-Dmixin.checks=true` / `-Dmixin.checks.interfaces=true` | Checks that mixins implement their declared interfaces fully |
| `-Dmixin.dumpTargetOnFailure=true` | Dumps the target class bytes when an injection fails |
| `-Dmixin.hotSwap=true` | Mixin hot-swap agent support (see toolchain.md section 11) |
| `-Dmixin.ignoreRequired=true` | Turns `required` failures into warnings (diagnostic only) |

- `@Debug(export = true)` on one mixin class exports just its targets.
- `@Local(print = true)` dumps the available locals at the injection point.
- Loader extras: `-Dfabric.debug.logTransformErrors=true`, `-Dfabric.debug.logClassLoadErrors=true`.
- IntelliJ: View > Show Bytecode on the decompiled MC class shows the real owner and descriptor for `@At` targets.
- From the CLI, javap the jars Loom caches: `javap -c -l -p -cp ~/.gradle/caches/fabric-loom/26.3/minecraft-common.jar net.minecraft.world.entity.LivingEntity`. Use `minecraft-client-only.jar` for client classes. `-l` prints the LocalVariableTable, so you can check `@Local(name=...)` names. For example, `actuallyHurt` has locals `level, source, dmg, originalDamage, absorbedDamage, serverPlayer`.
- Breakpoints work inside handler methods (the code is merged into the target class), as long as the sources are attached.

---

## 9. Most common crash and failure causes

| Symptom (log text) | Cause / fix |
|---|---|
| `InvalidInjectionException: ... No targets found` / `Critical injection failure: @Inject annotation on X could not find any targets matching 'foo'` / `... required 1 but 0 succeeded` | Wrong method name or descriptor, an old Yarn or intermediary name, the method renamed in 26.x, or the wrong owner in `@At(target)`. Check against the 26.3 source. |
| `InvalidInjectionException: Invalid descriptor on ...! Expected (...)V but found (...)V` | Handler parameters do not match: missing or extra target params, `CallbackInfo` vs `CallbackInfoReturnable`, or wrong primitive vs boxed type. |
| `Mixin transformation of X failed` then `NoClassDefFoundError: net/minecraft/client/...` on a server | Client mixin in the common config, or a client class referenced from common code or a mixin signature. Move it to the client config or source set. |
| `IllegalClassLoadError: com.example.mixin.Foo is in a defined mixin package ... and cannot be referenced directly` | Something references a mixin class directly. Move helpers out of the mixin package, and cast to an interface instead. |
| `MixinTargetAlreadyLoadedException` / `Attempted to load class X too early` | Your code loaded the target class before mixins applied, e.g. a static reference in a `preLaunch` entrypoint, or a mixin plugin touching MC classes. |
| `@Shadow field/method ... was not located in the target class` | The shadow name or type is wrong, or you used `@Shadow` on a superclass member (extend the superclass instead). |
| `Cannot cancel ... not cancellable` | Missing `cancellable = true`. |
| `@Redirect conflict` / `... is already redirected by ...` | Two mods redirect the same call. Switch to `@WrapOperation`. |
| `ClassCastException` on a `(Duck) obj` cast | The mixin implementing the duck did not apply (failed silently with `defaultRequire` 0, or a config not registered in fabric.mod.json). |
| `Non-unique Mixin config name` | Two mods ship the same config file name. Prefix it with your modid. |
| `Unsupported compatibility level`/class version errors | Missing or wrong `compatibilityLevel`. Use `JAVA_25`. |
| Mixin silently does nothing | `defaultRequire` is 0, the config is not listed in fabric.mod.json, the mixin is not listed in the config's arrays, or it is listed under `client` but you are testing a server. |
| `VerifyError` / `Bad local variable type` | `@ModifyVariable` or `LocalRef` type mismatch, or `ordinal` picked the wrong local. Use `@Local(name=...)`. |

---

## 10. Compatibility etiquette (checklist)

1. Prefer Fabric API events and hooks over a mixin (see the other knowledge files). Prefer data (tags, components, datapack registries) over code.
2. Prefer MixinExtras chainable injectors over `@Redirect`, `@Overwrite` and HEAD-cancelling `@Inject`.
3. Inject as narrowly as possible: precise `@At` with descriptors, slices, and `ordinal` only when necessary.
4. Prefix every added member (`@Unique` fields and methods, duck methods, accessor methods) with `<modid>$`.
5. Keep handlers small and delegate to normal classes outside the mixin package.
6. Keep `defaultRequire: 1`, and run the game once per MC bump to catch failures.
7. Make optional-integration mixins conditional with a config plugin plus `@Pseudo`/`targets`.
8. Do not mix into other mods' internals unless you coordinate with their authors.
9. Never call `MixinExtrasBootstrap.init()`, never shade Mixin or MixinExtras, and never bundle ASM.
10. Use `Loom ValidateMixinNameTask` (toolchain.md section 7) if you follow the `<Target>Mixin` naming convention.

---

## 11. Where to look

- Real-world 26.3 mixins: `/tmp/claude-0/mcsrc/fabric-api/*/src/{main,client}/java/net/fabricmc/fabric/mixin/**`. Grep for an annotation to find examples: `grep -rn "@WrapOperation" --include=*.java /tmp/claude-0/mcsrc/fabric-api`.
- Testmod mixins: `*/src/testmod*/java/**/mixin/**`.
- fabric-docs: `develop/mixins/bytecode.md` (bytecode, descriptors, locals, lambdas, `new` patterns) and `develop/mixins/accessors.md`.
- Target classes: `/tmp/claude-0/mcsrc/common/net/minecraft/**` and `/tmp/claude-0/mcsrc/client/net/minecraft/**`.
- Upstream docs: SpongePowered Mixin wiki (still mostly valid), and the MixinExtras wiki (github.com/LlamaLad7/MixinExtras/wiki).
