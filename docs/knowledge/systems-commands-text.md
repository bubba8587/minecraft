# Commands, permissions and text Components on 26.3

Scope: Brigadier commands via fabric-command-api-v2, vanilla permission sets, fabric-permission-api-v1, client commands, `Component`/`Style`/`ClickEvent`/`HoverEvent` for MC 26.3 / Fabric API 0.166.2+26.3. Verified 2026-11-14 against decompiled 26.3 (`net/minecraft/commands/**`, `net/minecraft/server/permissions/**`, `net/minecraft/network/chat/**`), fabric-api 26.3 sources/testmods, Brigadier 1.3.11.

## 1. Registering server commands

```java
CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
    // dispatcher: CommandDispatcher<CommandSourceStack>; buildContext: CommandBuildContext (registry access for args)
    // selection: Commands.CommandSelection (ALL / DEDICATED / INTEGRATED); fields includeDedicated / includeIntegrated (widened by Fabric)
    dispatcher.register(Commands.literal("home")
        .requires(Commands.hasPermission(Commands.LEVEL_ALL))
        .executes(ctx -> goHome(ctx.getSource(), ctx.getSource().getPlayerOrException()))
        .then(Commands.literal("set")
            .executes(ctx -> setHome(ctx.getSource())))
        .then(Commands.argument("target", EntityArgument.player())
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .executes(ctx -> goHome(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));
});
```
(src: fabric-api fabric-command-api-v2 api/command/v2/CommandRegistrationCallback.java: `void register(CommandDispatcher<CommandSourceStack>, CommandBuildContext, Commands.CommandSelection)`)
- `Commands.literal(String)` and `Commands.argument(String, ArgumentType<T>)` (src: common net/minecraft/commands/Commands.java:443-447). Static-import them.
- Return value of `executes` = result int; `Command.SINGLE_SUCCESS` (1). Throw `CommandSyntaxException` for user errors (`new SimpleCommandExceptionType(Component.literal("...")).create()`, `DynamicCommandExceptionType`).
- Fires on every datapack reload (`/reload`) as well as server start, so registration must be idempotent and side-effect free.
- Redirects/aliases: `CommandNode<CommandSourceStack> node = dispatcher.register(...); dispatcher.register(Commands.literal("h").redirect(node));` (redirect does not carry `executes` for zero-arg; add `.executes` on the alias too).

### CommandSourceStack essentials (src: common net/minecraft/commands/CommandSourceStack.java)
`getServer()`, `getLevel()` (ServerLevel), `getPosition()` (Vec3), `getRotation()`, `getEntity()` (@Nullable), `getEntityOrException()`, `getPlayer()` (@Nullable ServerPlayer), `getPlayerOrException()`, `isPlayer()`, `permissions()` (PermissionSet), `withPermission(...)`, `withPosition/withLevel/withEntity...`.
Feedback: `sendSuccess(Supplier<Component> msg, boolean broadcastToOps)`, `sendFailure(Component)`, `sendSystemMessage(Component)`.

### Common argument types (26.3 names)
Brigadier: `IntegerArgumentType.integer(min,max)` / `getInteger`, `LongArgumentType`, `FloatArgumentType`, `DoubleArgumentType`, `BoolArgumentType.bool()` / `getBool`, `StringArgumentType.word() / string() / greedyString()` / `getString`.
Vanilla (`net.minecraft.commands.arguments`): `EntityArgument.entity()/entities()/player()/players()` + `getEntity/getEntities/getPlayer/getPlayers`; `GameProfileArgument.gameProfile()` + `getGameProfiles` (returns `Collection<NameAndId>`); `coordinates.BlockPosArgument.blockPos()` + `getLoadedBlockPos(ctx, name)` / `getBlockPos`; `coordinates.Vec3Argument.vec3()` + `getVec3`; `item.ItemArgument.item(buildContext)` + `getItem` (ItemInput); `blocks.BlockStateArgument.block(buildContext)`; `ComponentArgument.textComponent(buildContext)` + `getResolvedComponent`; `DimensionArgument.dimension()` + `getDimension` (ServerLevel); `IdentifierArgument.id()` + `getId` (was ResourceLocationArgument); `ResourceArgument.resource(buildContext, Registries.X)` + `getResource`/`getSummonableEntityType`; `ResourceKeyArgument`, `ResourceOrTagArgument`, `UuidArgument.uuid()`, `TimeArgument`, `MessageArgument.message()`, `CompoundTagArgument`, `NbtPathArgument`, `ParticleArgument`, `HexColorArgument`, `StringRepresentableArgument`.

### Suggestions
```java
Commands.argument("warp", StringArgumentType.word())
    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(WarpData.get(ctx.getSource().getServer()).names(), builder))
// vanilla providers: .suggests(SuggestionProviders.cast(SuggestionProviders.SUMMONABLE_ENTITIES))
// ids: SharedSuggestionProvider.suggestResource(Iterable<Identifier>, builder)
```
(src: common commands/SharedSuggestionProvider.java:137-270, commands/synchronization/SuggestionProviders.java: `ASK_SERVER, AVAILABLE_SOUNDS, SUMMONABLE_ENTITIES, POST_EFFECTS`, `cast(...)`). Custom server-side suggestions on a vanilla argument type work with vanilla clients (client asks server via `ASK_SERVER`).

### Custom ArgumentType
```java
public class ColorArgument implements ArgumentType<Color> {
    @Override public Color parse(StringReader reader) throws CommandSyntaxException { ... }
    @Override public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> ctx, SuggestionsBuilder b) {
        return SharedSuggestionProvider.suggest(NAMES, b);
    }
    @Override public Collection<String> getExamples() { return NAMES; }
}
// in onInitialize (common):
ArgumentTypeRegistry.registerArgumentType(Identifier.fromNamespaceAndPath("mymod", "color"),
        ColorArgument.class, SingletonArgumentInfo.contextFree(ColorArgument::new));
// or SingletonArgumentInfo.contextAware(buildContext -> new ColorArgument(buildContext))
```
(src: fabric-api api/command/v2/ArgumentTypeRegistry.java; testmod CustomArgumentTest.java, argument/SmileyArgumentType.java)
- Argument types are serialized into the command tree sent to clients and live in synced registry `BuiltInRegistries.COMMAND_ARGUMENT_TYPE`: clients without your mod cannot connect (Fabric registry sync disconnects vanilla clients). For server-only mods, build commands from vanilla argument types (e.g. `StringArgumentType` + custom parsing + `.suggests`).
- Args with parameters need a full `ArgumentTypeInfo<A, T extends ArgumentTypeInfo.Template<A>>` (`serializeToNetwork`, `deserializeFromNetwork`, `serializeToJson`, `unpack`). (src: common commands/synchronization/ArgumentTypeInfo.java)

Entity selector options: `EntitySelectorOptionRegistry.register(Identifier, Component description, EntitySelectorOptions.Modifier, Predicate<EntitySelectorParser> canUse)` / `registerNonRepeatable(...)`; `FabricEntitySelectorParser` (was FabricEntitySelectorReader).

## 2. Vanilla permissions in 26.x (no more int levels)

(src: common net/minecraft/server/permissions/)
- `PermissionLevel` enum: `ALL(0), MODERATORS(1), GAMEMASTERS(2), ADMINS(3), OWNERS(4)`; `isEqualOrHigherThan`, `byId(int)`, `CODEC` (names), `INT_CODEC`.
- `Permission`: `Permission.HasCommandLevel(PermissionLevel)`, `Permission.Atom.create("name")` / `create(Identifier)`. Constants in `Permissions`: `COMMANDS_MODERATOR, COMMANDS_GAMEMASTER, COMMANDS_ADMIN, COMMANDS_OWNER, COMMANDS_ENTITY_SELECTORS, CHAT_SEND_MESSAGES, CHAT_SEND_COMMANDS, CHAT_RECEIVE_PLAYER_MESSAGES, CHAT_RECEIVE_SYSTEM_MESSAGES`.
- `PermissionSet` (`boolean hasPermission(Permission)`, `NO_PERMISSIONS`, `ALL_PERMISSIONS`, `union`); `PermissionSetSupplier` (`PermissionSet permissions()`) implemented by `CommandSourceStack` and `ServerPlayer`.
- `PermissionCheck` (`check(PermissionSet)`, data-driven via `BuiltInRegistries.PERMISSION_CHECK_TYPE`): `Commands.LEVEL_ALL / LEVEL_MODERATORS / LEVEL_GAMEMASTERS / LEVEL_ADMINS / LEVEL_OWNERS`.
- Usage:
```java
.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))               // vanilla commands use this
.requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
if (serverPlayer.permissions().hasPermission(Permissions.COMMANDS_ADMIN)) { ... }
server.getProfilePermissions(nameAndId)  // LevelBasedPermissionSet for offline profiles; .level()
```
- Old `source.hasPermission(2)` / `player.hasPermissions(2)` do not exist: replace with the above.

## 3. fabric-permission-api-v1 (new in 26.x Fabric API)

Purpose: a provider-agnostic permission node API (like LuckPerms' fabric-permissions-api, now in Fabric API). Mods CHECK permissions; permission/claim mods PROVIDE answers via an event. Without any provider every check returns your default. (src: fabric-api fabric-permission-api-v1 api/permission/v1/*, package-info.java)

- `PermissionContextOwner` is interface-injected on `Entity`/`Player` and `CommandSourceStack` (decompiled `CommandSourceStack implements ..., PermissionContextOwner`). Methods:
  - `TriState checkPermission(Identifier)`
  - `boolean checkPermission(Identifier, boolean defaultValue)`
  - `boolean checkPermission(Identifier, PermissionLevel defaultLevel)` (default = vanilla op level check)
  - `<T> T checkPermission(PermissionNode<T>)` / `checkPermission(node, T default)`
  - `getPermissionContext()`
- Typed nodes: `PermissionNode.of(Identifier | ns, path)` (Boolean), `ofInteger(...)`, `ofString(...)`, `ofCustom(id, Codec<T>, Class<T> | Predicate<Object>)`.
- Command predicate helpers: `PermissionPredicates.require(Identifier[, boolean default | PermissionLevel])`, `require(PermissionNode<Boolean>[, default | PermissionLevel])`.
- Context: `PermissionContext` keys `NAME, POSITION, BLOCK_POSITION, ENTITY, COMMAND_SOURCE_STACK, LEVEL, SERVER`; `PermissionContext.Type {PLAYER, ENTITY, SYSTEM, OTHER}`; `context.mutable().set(KEY, value)` for location-specific checks; `PermissionContext.create(uuid, type, PermissionLevel)`; `PermissionContext.offlinePlayer(uuid|NameAndId, server)` -> `CompletableFuture<MutablePermissionContext>`; custom keys `PermissionContext.key(Identifier)`.
- Providers: `PermissionEvents.ON_REQUEST.register((context, node) -> value or null)` (first non-null wins); `PermissionEvents.PREPARE_OFFLINE_PLAYER` to load offline data asynchronously.

```java
static final PermissionNode<Boolean> HOME_OTHERS = PermissionNode.of("mymod", "command/home/others");
static final PermissionNode<Integer> MAX_HOMES = PermissionNode.ofInteger("mymod", "max_homes");

dispatcher.register(Commands.literal("home")
    .requires(PermissionPredicates.require(Identifier.fromNamespaceAndPath("mymod", "command/home"), true))
    .then(Commands.argument("target", EntityArgument.player())
        .requires(PermissionPredicates.require(HOME_OTHERS, PermissionLevel.GAMEMASTERS))
        .executes(...)));

int max = player.checkPermission(MAX_HOMES, 3);
boolean ok = player.getPermissionContext().mutable()
        .set(PermissionContext.BLOCK_POSITION, pos)
        .checkPermission(Identifier.fromNamespaceAndPath("mymod", "build"), true);
```
Note: `require(PermissionNode<Boolean>, PermissionLevel)` evaluates the vanilla level as the default; `require(Identifier)` with no default denies when no provider answers.

## 4. Client commands (fabric-command-api-v2 client)

```java
ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
    dispatcher.register(ClientCommands.literal("coords")
        .requires(FabricClientCommandSource::attended)   // only when the user typed it (not via a server-sent ClickEvent)
        .executes(ctx -> {
            FabricClientCommandSource src = ctx.getSource();
            src.sendFeedback(Component.literal(src.getPlayer().blockPosition().toShortString()));
            return 1;
        }));
});
```
(src: fabric-api fabric-command-api-v2/src/client/.../ClientCommands.java, FabricClientCommandSource.java)
- `ClientCommands` (was `ClientCommandManager`): `literal`, `argument`, `getActiveDispatcher()`, `refreshCommandCompletions()`.
- `FabricClientCommandSource`: `sendFeedback`, `sendError`, `getClient()`, `getPlayer()` (LocalPlayer), `getLevel()` (ClientLevel), `getEntity()`, `getPosition()`, `getRotation()`, `getMeta(String)`, `attended()`.
- Client commands are handled locally and take priority over a same-named server command.

## 5. Text: Component, Style, events

(src: common net/minecraft/network/chat/Component.java, MutableComponent.java, Style.java, ClickEvent.java, HoverEvent.java)

Factories: `Component.literal(s)`, `translatable(key)`, `translatable(key, Object... args)` (args may be Components), `translatableWithFallback(key, fallback[, args])`, `translatableEscape`, `empty()`, `keybind(name)`, `nbt(...)`, `score(...)`, `selector(...)`, `object(ObjectInfo[, fallback])` (sprite/player-head object components), `nullToEmpty(s)`, `translationArg(Date|UUID|Identifier|ChunkPos|URI|Message)`. Shared: `CommonComponents` (`EMPTY`, `NEW_LINE`, `SPACE`, `GUI_DONE`, `GUI_YES`, ...), `ComponentUtils`.

MutableComponent: `append(String|Component)`, `withStyle(ChatFormatting...)`, `withStyle(UnaryOperator<Style>)`, `withStyle(Style)`, `withColor(int rgb | TextColor)`, `withoutShadow()`, `setStyle`, `copy()` (on Component).
Style: `Style.EMPTY.withColor(ChatFormatting|int|TextColor)`, `withShadowColor(argb)`, `withBold/withItalic/withUnderlined/withStrikethrough/withObfuscated(Boolean)`, `withClickEvent(ClickEvent)`, `withHoverEvent(HoverEvent)`, `withInsertion(String)`, `withFont(FontDescription)`, `applyFormat(ChatFormatting)`.

ClickEvent / HoverEvent are sealed-style interfaces with records (since 1.21.5), not `new ClickEvent(Action, String)`:
```java
MutableComponent link = Component.literal("[Teleport]").withStyle(s -> s
        .withColor(ChatFormatting.AQUA)
        .withClickEvent(new ClickEvent.RunCommand("/home"))
        .withHoverEvent(new HoverEvent.ShowText(Component.translatable("mymod.tooltip.home"))));
```
- `ClickEvent.OpenUrl(URI)`, `OpenFile(String path)` (client-only, not allowed from server), `RunCommand(String)`, `SuggestCommand(String)`, `ChangePage(int)`, `CopyToClipboard(String)`, `ShowDialog(Holder<Dialog>)`, `Custom(Identifier id, Optional<Tag> payload)`.
- `HoverEvent.ShowText(Component)`, `ShowItem(ItemStackTemplate)` (not ItemStack in 26.x), `ShowEntity(HoverEvent.EntityTooltipInfo(EntityType<?>, UUID, @Nullable Component name))`.
- JSON (datapacks, `/tellraw`): snake_case keys `click_event` / `hover_event` with `"action"`; e.g. `{"text":"x","click_event":{"action":"run_command","command":"/home"},"hover_event":{"action":"show_text","value":"hi"}}`; `open_url` uses `"url"`, `change_page` `"page"`, `copy_to_clipboard` `"value"`, `show_dialog` `"dialog"`, `custom` `"id"` + `"payload"`; `show_entity` uses `"id"`, `"uuid"`, `"name"`. (Style codec fields: `color, shadow_color, bold, italic, underlined, strikethrough, obfuscated, click_event, hover_event, insertion, font`.)
- Codecs: `ComponentSerialization.CODEC`, `ComponentSerialization.STREAM_CODEC`.
- `RunCommand` from chat/books/dialogs runs as the clicking player via `ClientPacketListener#sendUnattendedCommand`: the client shows a confirmation screen when the command fails to parse, needs a signature (signed message args), or needs elevated permissions (`multiplayer.confirm_command.*`). (src: client multiplayer/ClientPacketListener.java:2565) Client commands gated by `.requires(FabricClientCommandSource::attended)` will not run from server-sent click events.
- Translations: put keys in `assets/<modid>/lang/en_us.json`. On server-only mods clients do not have your lang file: send `Component.literal`, or use `translatableWithFallback(key, fallback)`, or a server-side translation library.

## Where to look
- Vanilla command implementations: `net/minecraft/server/commands/*.java` (e.g. `TeleportCommand`, `GiveCommand`) for idiomatic argument use.
- fabric-api `fabric-command-api-v2/src/testmod/.../CommandTest.java`, `CustomArgumentTest.java`, `EntitySelectorGameTest.java`; `testmodClient/.../ClientCommandTest.java`.
- fabric-api `fabric-permission-api-v1/src/testmod/.../PermissionTestMod.java`, `example/PermissionMap.java` (example provider).
- fabric-docs `develop/commands/{basics,arguments,suggestions}.md`, `develop/text-and-translations.md`.
