# Systems ("plumbing") on MC 26.3 + Fabric API: index

Scope: server/common-side plumbing systems for Fabric mods on Minecraft 26.3 (Fabric API 0.166.2+26.3, Mojang official names). Verified 2026-11-14 against decompiled 26.3 source (`/tmp/claude-0/mcsrc/common`, `client`), FabricMC/fabric-api branch 26.3, fabric-docs `develop/`.

## Files

| File | Covers |
|---|---|
| [systems-serialization.md](systems-serialization.md) | Codec / MapCodec / RecordCodecBuilder, DataResult, StreamCodec / ByteBufCodecs / RegistryFriendlyByteBuf, ValueInput / ValueOutput (NBT save/load), CompoundTag, fabric-serialization-api-v1 |
| [systems-networking.md](systems-networking.md) | CustomPacketPayload, PayloadTypeRegistry, Server/ClientPlayNetworking, threading, configuration tasks, login queries, PlayerLookup, fabric-message-api |
| [systems-persistence.md](systems-persistence.md) | Data Attachment API (persistent, synced, copyOnDeath, global), SavedData / SavedDataStorage, player data, block entity save and sync |
| [systems-commands-text.md](systems-commands-text.md) | Brigadier, CommandRegistrationCallback, arguments, custom ArgumentType, suggestions, vanilla permission sets, fabric-permission-api-v1, client commands, Component / Style / ClickEvent / HoverEvent |
| [systems-events.md](systems-events.md) | Lifecycle, tick, level, chunk, entity, player, interaction events; EventResult; defining your own Event with EventFactory |
| [systems-menus-transfer.md](systems-menus-transfer.md) | AbstractContainerMenu, slots, quickMoveStack, ContainerData, fabric-menu-api-v1 (ExtendedMenuType), transfer API, API Lookup, block getter API |
| [systems-world.md](systems-world.md) | Dimensions, TeleportTransition, biome modification (code + data-driven), 26.3 worldgen Feature refactor, chunk generators, Level/ServerLevel ops, raycasts, entity queries, scheduling |
| [systems-resources-config-serverside.md](systems-resources-config-serverside.md) | fabric-resource-loader-v1 (reload listeners, built-in packs, DataResourceStore), DynamicRegistries, resource conditions, hand-rolled config, server-side-only mods |

## Top surprises vs 1.21.x / Yarn memory (read these first)

1. `net.minecraft.resources.Identifier` IS the Mojang name in 26.x (Mojang renamed `ResourceLocation` -> `Identifier`). `Identifier.fromNamespaceAndPath(ns, path)`, `Identifier.parse`, `Identifier.withDefaultNamespace`. `ResourceKey#identifier()` (not `location()`). (src: common net/minecraft/resources/Identifier.java, ResourceKey.java)
2. NBT save/load uses `ValueInput` / `ValueOutput` (`BlockEntity#loadAdditional(ValueInput)` / `saveAdditional(ValueOutput)`, `Entity#readAdditionalSaveData(ValueInput)` / `addAdditionalSaveData(ValueOutput)`), not raw `CompoundTag`. `CompoundTag` getters return `Optional<...>` or take a default (`getIntOr`).
3. `PayloadTypeRegistry.clientboundPlay()` / `serverboundPlay()` / `clientboundConfiguration()` / `serverboundConfiguration()` (old `playS2C()` / `playC2S()` names are gone).
4. Fabric API renamed to Mojang vocabulary in 26.1: `ServerWorldEvents` -> `ServerLevelEvents`, `ClientCommandManager` -> `ClientCommands`, `ExtendedScreenHandlerType` -> `ExtendedMenuType`, `ExtendedScreenHandlerFactory` -> `ExtendedMenuProvider`, `InventoryStorage` -> `ContainerStorage`, `PacketByteBufs` -> `FriendlyByteBufs`, `ServerEntityWorldChangeEvents` -> `ServerEntityLevelChangeEvents`, `FabricReadView/WriteView` -> `FabricValueInput/Output`, `SimpleResourceReloader` -> `SimpleReloadListener`. Full map: fabric-docs `public/assets/develop/porting/fabric-api-26-1-migration-map.xml`.
5. Permissions are no longer an int level. Vanilla: `PermissionSet` + `Permission` + `PermissionLevel` enum; `.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))` or `source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)`. Fabric adds `fabric-permission-api-v1` (string/typed permission nodes, provider events).
6. 26.3 worldgen: `ConfiguredFeature` + `FeatureConfiguration` were merged into a `Feature` interface, registry `Registries.FEATURE` (`worldgen/feature`, data folder `data/<ns>/worldgen/feature/`); feature codecs go in `BuiltInRegistries.FEATURE_TYPE`. fabric-docs `reference/latest` still shows `ConfiguredFeature` (26.2) and is WRONG for 26.3.
7. 26.3 reloadable registries: recipes and advancements now load via the dynamic-registry path; `DynamicRegistries.registerReloadable(key, codec)` lets mods add their own. Fabric `fabric-resource-loader-v1` replaces v0 (v0 lives in `deprecated/`).
8. Data-driven Fabric biome modifiers exist: `data/<ns>/fabric/biome_modifier/*.json` with types `fabric:add_features`, `fabric:add_spawns`, `fabric:set_attributes`, etc.
9. `ServerPlayer.server` is private in 26.3; use `player.level().getServer()`. `EntityType` constants live in `EntityTypes` (e.g. `EntityTypes.COW`).
10. A vanilla client connecting to a Fabric server that has any modded entries in a synced registry (blocks, items, entity types, menus, argument types...) is disconnected with "This server requires Fabric Loader and Fabric API installed on your client!" (src: fabric-api fabric-registry-sync-v0 impl/registry/sync/RegistrySyncManager.java). Server-side-only mods must avoid synced registry entries (or use Polymer).
