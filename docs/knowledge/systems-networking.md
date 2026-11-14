# Networking on 26.3 with Fabric API (fabric-networking-api-v1)

Scope: custom packets, connection phases, threading, configuration tasks, login queries, chat events (fabric-message-api-v1) for MC 26.3 / Fabric API 0.166.2+26.3. Verified 2026-11-14 against fabric-api branch 26.3 sources + testmods and decompiled 26.3.

Packages: common `net.fabricmc.fabric.api.networking.v1.*`, client-only `net.fabricmc.fabric.api.client.networking.v1.*` (put client code in your client source set / `ClientModInitializer`).

## 1. Define a payload

```java
public record PingPayload(int value, BlockPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PingPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("mymod", "ping"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PingPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PingPayload::value,
            BlockPos.STREAM_CODEC, PingPayload::pos,
            PingPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
```
- `CustomPacketPayload.Type<T>(Identifier id)` and `CustomPacketPayload.TypeAndCodec<B,T>(type, codec)` are records. (src: common net/minecraft/network/protocol/common/custom/CustomPacketPayload.java)
- Empty payload: `StreamCodec.unit(INSTANCE)`.
- Hand-written: `CustomPacketPayload.codec(MyPayload::write, MyPayload::new)`.

## 2. Register the payload type (both sides, common initializer)

```java
// in ModInitializer.onInitialize() (runs on client AND dedicated server)
PayloadTypeRegistry.clientboundPlay().register(PingPayload.TYPE, PingPayload.CODEC);   // server -> client
PayloadTypeRegistry.serverboundPlay().register(ClickPayload.TYPE, ClickPayload.CODEC);  // client -> server
PayloadTypeRegistry.clientboundConfiguration().register(...); // FriendlyByteBuf codecs
PayloadTypeRegistry.serverboundConfiguration().register(...);
```
(src: fabric-api fabric-networking-api-v1 api/networking/v1/PayloadTypeRegistry.java)
- Play registries are `PayloadTypeRegistry<RegistryFriendlyByteBuf>`; configuration registries are `PayloadTypeRegistry<FriendlyByteBuf>` (no registry access during configuration).
- Must register before registering a receiver, else `registerGlobalReceiver` throws `IllegalArgumentException`.
- Large payloads (auto-split): `registerLarge(type, codec, int maxPacketSize)` or `registerLarge(type, codec, IntSupplier)`. Security: keep serverbound limits small.
- Decode filtering: `register(type, codec, Predicate<PacketContext> decodePredicate)`; runs on the netty thread. Built-ins: `PayloadTypeRegistry.Predicates.SERVERBOUND_HAS_INFINITE_MATERIALS` (creative), `SERVERBOUND_CAN_USE_COMMAND_BLOCKS`.
- Renamed in 26.x: old `playS2C()/playC2S()/configurationS2C()/configurationC2S()` -> `clientboundPlay()/serverboundPlay()/clientboundConfiguration()/serverboundConfiguration()`. `PacketByteBufs` -> `FriendlyByteBufs` (`create()`, `empty()`, `readBytes`). `S2CPlayChannelEvents` -> `ClientboundPlayChannelEvents`, `C2SPlayChannelEvents` -> `ServerboundPlayChannelEvents`.

## 3. Receive and send (play phase)

Server side (common code):
```java
ServerPlayNetworking.registerGlobalReceiver(ClickPayload.TYPE, (payload, context) -> {
    ServerPlayer player = context.player();            // already on the server thread
    MinecraftServer server = context.server();
    // VALIDATE everything from the client: distance, permissions, loaded chunk, entity existence
    Entity target = player.level().getEntity(payload.entityId());
    if (target instanceof LivingEntity le && le.closerThan(player, 5)) {
        le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100));
    }
    context.responseSender().sendPacket(new PingPayload(1, player.blockPosition()));
});

// send
ServerPlayNetworking.send(serverPlayer, new PingPayload(42, pos));
if (ServerPlayNetworking.canSend(serverPlayer, PingPayload.TYPE)) { ... } // client has a receiver?
for (ServerPlayer p : PlayerLookup.tracking(blockEntity)) ServerPlayNetworking.send(p, payload);
```
Client side (client source set):
```java
ClientPlayNetworking.registerGlobalReceiver(PingPayload.TYPE, (payload, context) -> {
    Minecraft client = context.client();   // on the render/client thread already
    LocalPlayer player = context.player();
    client.gui.hud.setOverlayMessage(Component.literal("ping " + payload.value()), false); // 26.2+: HUD methods live on Gui.hud (src: client gui/Hud.java:1207)
});
ClientPlayNetworking.send(new ClickPayload(entity.getId()));
if (ClientPlayNetworking.canSend(ClickPayload.TYPE)) { ... }  // false on vanilla servers
```
- `ServerPlayNetworking.Context`: `server()`, `player()`, `responseSender()`, `packetContext()`. `ClientPlayNetworking.Context`: `client()`, `player()`, `responseSender()`. (src: ServerPlayNetworking.java:344-370, ClientPlayNetworking.java:261-300)
- THREADING: play handlers (both sides) run on the main game thread; no `server.execute(...)` wrapper needed (old 1.20.4- style `(server, player, handler, buf, sender)` callbacks that needed `server.execute` are gone). Configuration and login handlers run on the netty event loop: schedule game access with `ServerConfigurationNetworking.getServer(listener).execute(...)` / `server.submit(...)`.
- Other APIs: `ServerPlayNetworking.getSender(player).sendPacket(payload, PacketSendListener)`, `createClientboundPacket(payload)` (for bundling: `new ClientboundBundlePacket(List.of(pkt1, pkt2))`), `registerReceiver(listener, type, handler)` (per-connection), `unregisterGlobalReceiver(Identifier)`, `getReceived/getSendable(player)`, `reconfigure(player)` (back into configuration, re-runs tasks).
- `ServerPlayNetworking.send` does not check `canSend`; vanilla clients silently drop unknown custom payloads (unknown ids decode as `DiscardedPayload`, max 1 MiB; src: common ClientboundCustomPayloadPacket.java:18), but check `canSend` if behaviour depends on the client having your mod.
- Inside a codec's `encode` the buffer is a `FabricRegistryFriendlyByteBuf` exposing `fabric_getSendableConfigurationChannels()` (testmod use).

### PlayerLookup (who to send to)
`PlayerLookup.all(server)`, `level(serverLevel)`, `tracking(serverLevel, ChunkPos)`, `tracking(Entity)`, `tracking(BlockEntity)`, `tracking(serverLevel, BlockPos)`, `around(serverLevel, Vec3, radius)`, `around(serverLevel, Vec3i, radius)`. (src: api/networking/v1/PlayerLookup.java)

### Connection events
- `ServerPlayConnectionEvents.INIT (listener, server)`, `JOIN (listener, PacketSender sender, server)` (safe to send), `DISCONNECT (listener, server)`.
- `ClientPlayConnectionEvents.INIT/JOIN/DISCONNECT` with `(ClientPacketListener, [PacketSender,] Minecraft)`.
- `ServerConfigurationConnectionEvents.BEFORE_CONFIGURE`, `CONFIGURE (ServerConfigurationPacketListenerImpl, server)`, `DISCONNECT`.
- `ClientConfigurationConnectionEvents.INIT/START/COMPLETE/READY/DISCONNECT`.
- `ServerLoginConnectionEvents.INIT`, `QUERY_START (listener, server, LoginPacketSender, LoginSynchronizer)`, `DISCONNECT`; `ClientLoginConnectionEvents.INIT/QUERY_START/DISCONNECT`.
- `EntityTrackingEvents.START_TRACKING (Entity tracked, ServerPlayer)` / `STOP_TRACKING`: the right hook to send per-entity state to a player who starts seeing it.
- `ServerPlayerEvents.JOIN/LEAVE (ServerPlayer)` (fabric-entity-events) also exist; see systems-events.md.
- Channel registration events: `ClientboundPlayChannelEvents.REGISTER/UNREGISTER`, `ServerboundPlayChannelEvents...`, and configuration equivalents.

## 4. Configuration phase tasks

Configuration runs before the player joins the world (and again on `reconfigure`). Use it to exchange data the client needs before play (registries, settings handshake). Pattern (src: fabric-api networking testmod configuration/NetworkingConfigurationTest.java; real use: fabric-data-attachment-api impl/attachment/sync/AttachmentSync.java):

```java
// common init
PayloadTypeRegistry.clientboundConfiguration().register(HelloConfig.TYPE, HelloConfig.CODEC);   // StreamCodec<FriendlyByteBuf, ..>
PayloadTypeRegistry.serverboundConfiguration().register(HelloAck.TYPE, HelloAck.CODEC);

ServerConfigurationConnectionEvents.CONFIGURE.register((listener, server) -> {
    if (ServerConfigurationNetworking.canSend(listener, HelloConfig.TYPE)) {
        listener.addTask(new HelloTask("data"));       // Fabric interface-injected on ServerConfigurationPacketListenerImpl
    } else {
        // vanilla / mod-less client: skip, or listener.disconnect(Component.literal("Install MyMod"));
    }
});
ServerConfigurationNetworking.registerGlobalReceiver(HelloAck.TYPE, (payload, context) -> {
    context.packetListener().completeTask(HelloTask.KEY);   // MUST complete, or the client never joins
});

public record HelloTask(String data) implements ConfigurationTask {        // net.minecraft.server.network.ConfigurationTask
    public static final ConfigurationTask.Type KEY = new ConfigurationTask.Type("mymod:hello");
    @Override public void start(Consumer<Packet<?>> sender) {
        sender.accept(ServerConfigurationNetworking.createClientboundPacket(new HelloConfig(data)));
    }
    @Override public ConfigurationTask.Type type() { return KEY; }
}

// client init
ClientConfigurationNetworking.registerGlobalReceiver(HelloConfig.TYPE, (payload, context) -> {
    context.responseSender().sendPacket(HelloAck.INSTANCE);
});
```
- `ServerConfigurationNetworking.Context`: `server()`, `packetListener()`, `responseSender()`; also `ServerConfigurationNetworking.isReconfiguring(listener)`, `getServer(listener)`, `send(listener, payload)`.
- `completeTask` throws `IllegalStateException` if that task is not the current one.
- Per-connection state: `PacketContext.Key<T> KEY = PacketContext.key(Identifier)`; `listener.getPacketContext().set(KEY, value)` then later `player.connection.getPacketContext().orElse(KEY, default)` (pattern used by attachment sync to remember what the client supports).

## 5. Login phase (query/response, raw buffers)

Login queries use raw `FriendlyByteBuf` and channel `Identifier`s (no payload types):
```java
ServerLoginConnectionEvents.QUERY_START.register((listener, server, sender, synchronizer) -> {
    sender.sendPacket(CHANNEL, FriendlyByteBufs.create());
});
ServerLoginNetworking.registerGlobalReceiver(CHANNEL, (server, listener, understood, buf, synchronizer, responseSender) -> {
    if (!understood) { listener.disconnect(Component.literal("Mod required")); return; }
    synchronizer.waitFor(server.submit(() -> { /* server-thread checks */ }));  // delays login until done
});
// client
ClientLoginNetworking.registerGlobalReceiver(CHANNEL, (client, listener, buf, callbacksConsumer) ->
        CompletableFuture.completedFuture(FriendlyByteBufs.create()));   // return null future value = "not understood"
```
Runs on netty threads. Prefer configuration tasks for anything new; login is for very early gating. (src: ServerLoginNetworking.java, testmod login/NetworkingLoginQueryTest.java)

## 6. fabric-message-api-v1 (chat)

`net.fabricmc.fabric.api.message.v1` (src: fabric-message-api-v1):
- `ServerMessageEvents.ALLOW_CHAT_MESSAGE (PlayerChatMessage, ServerPlayer, ChatType.Bound) -> boolean`, `ALLOW_GAME_MESSAGE (server, Component, boolean overlay)`, `ALLOW_COMMAND_MESSAGE (PlayerChatMessage, CommandSourceStack, ChatType.Bound)` (for `/say`, `/me`, `/msg` style signed commands).
- `CHAT_MESSAGE`, `GAME_MESSAGE`, `COMMAND_MESSAGE`: post-send notifications, same parameters, void.
- `ServerMessageDecoratorEvent.EVENT` (vanilla `ChatDecorator`: `Component decorate(@Nullable ServerPlayer, Component)`), phases `ServerMessageDecoratorEvent.CONTENT_PHASE` then `STYLING_PHASE`:
```java
ServerMessageDecoratorEvent.EVENT.register(ServerMessageDecoratorEvent.STYLING_PHASE, (sender, message) ->
        message.copy().withStyle(ChatFormatting.GOLD));
```
Gotcha: the Javadoc example in that class still uses Yarn names (`styled`, `getPlayerManager`): ignore it. Cancelling a signed chat message breaks nothing, but modifying content after signing makes it show as modified on clients.

Sending messages: `player.sendSystemMessage(Component)`, `player.sendOverlayMessage(Component)` (action bar), `player.sendSystemMessage(Component, boolean overlay)`, `server.getPlayerList().broadcastSystemMessage(Component, boolean overlay)`. (src: common ServerPlayer.java:1820-1837, PlayerList.java:710)

## 7. Gotchas
- Register payload types in the common initializer, not only client: the dedicated server needs them too.
- A client-only class referenced from a common class crashes the dedicated server (classloading). Keep `ClientPlayNetworking` calls in client sources.
- Never trust serverbound data; re-validate reach, permission (`player.permissions().hasPermission(...)`), and item possession.
- `RegistryFriendlyByteBuf` codecs cannot be used for configuration payloads.
- Fabric registry sync itself is a configuration task (`fabric:registry/sync`); vanilla clients get disconnected if your mod adds synced registry entries (see systems-resources-config-serverside.md).
- Vanilla `ServerboundCustomClickActionPacket` (from `ClickEvent.Custom` in text/dialogs) ends in `MinecraftServer#handleCustomClickAction(Identifier, Optional<Tag>)` which only logs; Fabric API has no hook, so use a mixin there. (src: common MinecraftServer.java:2043)

## Where to look
- fabric-api `fabric-networking-api-v1/src/testmod/.../play/NetworkingPlayPacketTest.java`, `configuration/`, `login/`, `splitter/` (large payloads), `context/PacketContextTest.java`.
- fabric-docs `develop/networking.md` (accurate for 26.x names).
