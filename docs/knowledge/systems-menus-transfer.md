# Menus (server side), Transfer API, API Lookup, Block Getter API on 26.3

Scope: container menus and their sync, fabric-menu-api-v1, fabric-transfer-api-v1, fabric-api-lookup-api-v1, fabric-block-getter-api-v2 for MC 26.3 / Fabric API 0.166.2+26.3. Verified 2026-11-14 against decompiled 26.3 (`net/minecraft/world/inventory/**`) and fabric-api 26.3 sources/testmods.

Vocabulary (Mojang vs Yarn): `AbstractContainerMenu` = ScreenHandler, `MenuType` = ScreenHandlerType, `MenuProvider` = NamedScreenHandlerFactory, `Container` = Inventory (block/item storage), `Inventory` = PlayerInventory, `AbstractContainerScreen` = HandledScreen, `ContainerData` = PropertyDelegate.

## 1. AbstractContainerMenu essentials

(src: common net/minecraft/world/inventory/AbstractContainerMenu.java)
- ctor `protected AbstractContainerMenu(@Nullable MenuType<?>, int containerId)`.
- `addSlot(Slot)`, `addStandardInventorySlots(Container playerInv, int left, int top)` (main 27 + hotbar 9; also `addInventoryExtendedSlots`, `addInventoryHotbarSlots`), `addDataSlot(DataSlot)`, `addDataSlots(ContainerData)`.
- Required overrides: `ItemStack quickMoveStack(Player, int slotIndex)` (shift-click) and `boolean stillValid(Player)`.
- Helpers: `moveItemStackTo(stack, startSlot, endSlotExclusive, boolean backwards)`, `static stillValid(ContainerLevelAccess, Player, Block)`, `checkContainerSize(container, n)`, `checkContainerDataCount(data, n)`, `clickMenuButton(Player, int id)` (server receives button clicks from `ServerboundContainerButtonClickPacket`), `removed(Player)`, `clearContainer(player, container)` (drop/return items on close), `sendAllDataToRemote()`.
- `Container.stillValidBlockEntity(be, player)` (4 block reach default). `ContainerLevelAccess.create(level, pos)` / `ContainerLevelAccess.NULL`.
- `Slot(Container, int index, int x, int y)`; override `mayPlace(ItemStack)`, `mayPickup(Player)`, `getMaxStackSize()`, `onTake`; use `setByPlayer(stack)` / `set(stack)` / `setChanged()`.

```java
public class GrinderMenu extends AbstractContainerMenu {
    private final Container container;   // 2 slots: 0 input, 1 output
    private final ContainerData data;    // 0 progress, 1 maxProgress

    // client ctor (MenuType.MenuSupplier signature)
    public GrinderMenu(int id, Inventory inv) { this(id, inv, new SimpleContainer(2), new SimpleContainerData(2)); }

    // server ctor
    public GrinderMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(ModMenus.GRINDER, id);
        checkContainerSize(container, 2); checkContainerDataCount(data, 2);
        this.container = container; this.data = data;
        container.startOpen(inv.player);
        addSlot(new Slot(container, 0, 56, 35));
        addSlot(new Slot(container, 1, 116, 35) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        addStandardInventorySlots(inv, 8, 84);
        addDataSlots(data);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {   // mirrors ChestMenu
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem(); result = stack.copy();
            int own = 2;
            if (index < own) { if (!moveItemStackTo(stack, own, slots.size(), true)) return ItemStack.EMPTY; }
            else if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); container.stopOpen(player); }
    public int progress() { return data.get(0); }
}
```
- Plain menu type: `public static final MenuType<GrinderMenu> GRINDER = Registry.register(BuiltInRegistries.MENU, id("grinder"), new MenuType<>(GrinderMenu::new, FeatureFlags.VANILLA_SET));` (src: MenuType.java:56, fabric testmod `MenuTest`).
- Client screen: `MenuScreens.register(ModMenus.GRINDER, GrinderScreen::new)` in client init (src: client gui/screens/MenuScreens.java:59).
- Opening: `player.openMenu(MenuProvider)` returns `OptionalInt` (server only; no-op on client). Block: `state.getMenuProvider(level, pos)` / BE implementing `MenuProvider`; `new SimpleMenuProvider((id, inv, p) -> new GrinderMenu(id, inv, container, data), Component.translatable("container.mymod.grinder"))`.
- Sync: slots auto-sync; `ContainerData` / `DataSlot` values sync as SIGNED SHORTS (`ClientboundContainerSetDataPacket` writes `writeShort`), so values outside -32768..32767 get truncated: split ints into two slots or send a custom packet. (src: common network/protocol/game/ClientboundContainerSetDataPacket.java)
- Block entity side: implement `ContainerData` as an anonymous class reading BE fields (vanilla `AbstractFurnaceBlockEntity.dataAccess`).

## 2. fabric-menu-api-v1: menus with extra opening data

(Renamed from `fabric-screen-handler-api-v1`; `ExtendedScreenHandlerType` -> `ExtendedMenuType`, `ExtendedScreenHandlerFactory` -> `ExtendedMenuProvider`, `FabricScreenHandlerFactory` -> `FabricMenuProvider`.) (src: fabric-api fabric-menu-api-v1 api/menu/v1/*)

- `ExtendedMenuType<T extends AbstractContainerMenu, D>(ExtendedFactory<T, D> factory, StreamCodec<? super RegistryFriendlyByteBuf, D> streamCodec)`; factory `T create(int containerId, Inventory inventory, D data)`. Calling the 2-arg `create` throws.
- `ExtendedMenuProvider<D> extends MenuProvider`: `D getScreenOpeningData(ServerPlayer)`.
- `FabricMenuProvider.shouldCloseCurrentScreen()` (default true): return false to keep the mouse position when switching menus.

```java
public static final MenuType<BoxMenu> BOX = Registry.register(BuiltInRegistries.MENU, id("box"),
        new ExtendedMenuType<>(BoxMenu::new, BlockPos.STREAM_CODEC.cast()));          // client: BoxMenu(int, Inventory, BlockPos)

public class BoxBlockEntity extends RandomizableContainerBlockEntity implements ExtendedMenuProvider<BlockPos> {
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inv) { return new BoxMenu(id, inv, this); }
    @Override public BlockPos getScreenOpeningData(ServerPlayer player) { return worldPosition; }
    ...
}
// item-opened menu with a record payload
public record BagData(Optional<BlockPos> pos) {
    public static final StreamCodec<RegistryFriendlyByteBuf, BagData> CODEC =
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs::optional).map(BagData::new, BagData::pos).cast();
}
user.openMenu(new ExtendedMenuProvider<BagData>() {
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new PositionedBagMenu(id, inv, new BagInventory(stack), pos); }
    public Component getDisplayName() { return stack.getHoverName(); }
    public BagData getScreenOpeningData(ServerPlayer p) { return new BagData(Optional.ofNullable(pos)); }
});
```
(src: fabric-api fabric-menu-api-v1 testmod MenuTest.java, block/BoxBlockEntity.java, item/PositionedBagItem.java, menu/PositionedBagMenu.java)

Server-side-only mods: custom `MenuType`s are synced registry entries (vanilla clients cannot open them and Fabric registry sync rejects vanilla clients). Use vanilla types: `MenuType.GENERIC_9x1..9x6`, `GENERIC_3x3`, `ANVIL`, `HOPPER`, etc. with vanilla menus (`ChestMenu.threeRows(id, inv, container)`, `ChestMenu.sixRows(...)`) and intercept clicks by subclassing `ChestMenu` (override `clicked(...)`, UNVERIFIED exact signature) or use the sgui library (eu.pb4:sgui `2.2.1+26.3` on maven.nucleoid.xyz).

## 3. fabric-transfer-api-v1 (items and fluids between blocks)

(src: fabric-api fabric-transfer-api-v1 api/transfer/v1/**; module lifecycle: stable)
Core types: `Storage<T>` (`insert(resource, maxAmount, TransactionContext)`, `extract(...)`, `supportsInsertion()`, `supportsExtraction()`, iterate `StorageView<T>`s, `nonEmptyViews()`), `StorageView<T>` (`getResource`, `getAmount`, `getCapacity`, `isResourceBlank`), `SingleSlotStorage<T>`, `SlottedStorage<T>`, variants `ItemVariant` / `FluidVariant` (immutable resource + components; `ItemVariant.of(stack|item[, DataComponentPatch])`, `FluidVariant.of(fluid)`, `.blank()`, `CODEC`, `PACKET_CODEC`).
Amounts are `long`; fluids in droplets: `FluidConstants.BUCKET = 81000`, `BOTTLE = 27000`, `BLOCK = 81000`, `INGOT = 9000`, `NUGGET = 1000`, `DROPLET = 1`.

Transactions:
```java
try (Transaction tx = Transaction.openOuter()) {
    long inserted = target.insert(ItemVariant.of(Items.DIAMOND), 4, tx);
    if (inserted == 4) tx.commit();   // otherwise auto-abort on close
}
// nested: Transaction.openNested(parentOrNull); simulate: StorageUtil.simulateInsert(storage, res, amt, null)
long moved = StorageUtil.move(from, to, variant -> true, FluidConstants.BUCKET, null); // null = open its own outer tx
```
Lookups:
- `ItemStorage.SIDED : BlockApiLookup<Storage<ItemVariant>, @Nullable Direction>`; `FluidStorage.SIDED` likewise; `ItemStorage.ITEM` / `FluidStorage.ITEM : ItemApiLookup<Storage<...>, ContainerItemContext>` (fluid in buckets/tanks held as items; query via `ContainerItemContext.forPlayerInteraction(player, hand)` UNVERIFIED method name, see `context/ContainerItemContext.java`).
- Vanilla `Container`/`WorldlyContainer` block entities are wrapped automatically (hoppers interop both ways). Manual wrap: `ContainerStorage.of(container, direction)` (was InventoryStorage). Player: `PlayerInventoryStorage.of(player)`, `PlayerInventoryStorage.getCursorStorage(menu)`.
- Implementing: easiest `SingleFluidStorage.withFixedCapacity(capacity, this::setChanged)` / `SingleStackStorage` / `SingleVariantStorage<T>` (override `getBlankVariant()`, `getCapacity(T)`, `onFinalCommit()` to mark dirty), combine with `CombinedStorage`, `FilteringStorage`, `InsertionOnlyStorage`, `ExtractionOnlyStorage`; or extend `SidedStorageBlockEntity`. Custom participants extend `SnapshotParticipant<S>`.
- Persist: `storage.writeValue(ValueOutput)` / `readValue(ValueInput)` on `SingleFluidStorage`; static `SingleVariantStorage.readValue(storage, codec, fallback, input)` / `writeValue(storage, codec, output)`.

```java
public class TankBlockEntity extends BlockEntity {
    final SingleFluidStorage tank = SingleFluidStorage.withFixedCapacity(FluidConstants.BUCKET * 4, this::setChanged);
    @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); tank.writeValue(out); }
    @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); tank.readValue(in); }
}
// registration (common init)
FluidStorage.SIDED.registerForBlockEntity((be, dir) -> be.tank, ModBlockEntities.TANK);   // UNVERIFIED singular helper; generic form below is verified
FluidStorage.SIDED.registerForBlockEntities((be, dir) -> ((TankBlockEntity) be).tank, ModBlockEntities.TANK);
```
(src: transfer testmod ingame/FluidChuteBlockEntity.java, TransferTestInitializer.java)
26.3 change: `FluidVariantAttributes#enableColoredVanillaFluidNames` removed, use `FluidVariantAttributes#getColoredName` (Fabric 26.3 blog).
Gotchas: never mutate storage outside a transaction; only call `setChanged` in `onFinalCommit`; client-side lookups return unreliable data (read-only).

## 4. fabric-api-lookup-api-v1 (generic capability lookup)

(src: fabric-api fabric-api-lookup-api-v1 api/lookup/v1/{block,item,entity,custom}/)
```java
public static final BlockApiLookup<EnergySink, @Nullable Direction> ENERGY =
        BlockApiLookup.get(Identifier.fromNamespaceAndPath("mymod", "energy"), EnergySink.class, Direction.class);

ENERGY.registerSelf(ModBlockEntities.BATTERY);                                    // BE implements EnergySink
ENERGY.registerForBlockEntities((be, dir) -> ((Machine) be).sink(dir), TYPE_A, TYPE_B);
ENERGY.registerForBlocks((level, pos, state, be, dir) -> ..., ModBlocks.CABLE);
ENERGY.registerFallback((level, pos, state, be, dir) -> null);
EnergySink sink = ENERGY.find(level, pos, Direction.UP);                          // or find(level, pos, state, be, ctx)
BlockApiCache<EnergySink, Direction> cache = BlockApiCache.create(ENERGY, serverLevel, pos);  // per-tick queries
sink = cache.find(Direction.UP);
```
- Context type `Void` + `null` when no context needed. `BlockApiLookup.get` with an existing id but different classes throws.
- Also `ItemApiLookup.get(id, apiClass, contextClass)` (`registerForItems`, `registerSelf`, `find(stack, ctx)`), `EntityApiLookup.get(...)`, and `ApiLookupMap` / `ApiProviderMap` for custom lookups.

## 5. fabric-block-getter-api-v2

Interface-injected `FabricBlockGetter` on every `BlockGetter` and `RenderDataBlockEntity` on `BlockEntity` (src: fabric-api fabric-block-getter-api-v2 api/blockgetter/v2/):
- `Object getBlockEntityRenderData(BlockPos)`: thread-safe snapshot for chunk-meshing contexts (block models, tint sources, `getAppearance`). Override `BlockEntity#getRenderData()` to return an immutable snapshot; always `instanceof`-check the result.
- `boolean hasBiomes()` / `Holder<Biome> getBiomeFabric(BlockPos)`: biome access from a plain `BlockGetter` (e.g. render region); prefer `LevelReader#getBiome` when you have a level.
Mainly a client/rendering concern; renamed from "rendering data attachment"/`fabric-block-view-api-v2` era names.

## Where to look
- Vanilla menus: `ChestMenu`, `AbstractFurnaceMenu`, `DispenserMenu`, `BrewingStandMenu` (ContainerData use) in `net/minecraft/world/inventory/`.
- fabric-api `fabric-menu-api-v1/src/testmod`, `fabric-transfer-api-v1/src/testmod/.../ingame/` and `gametests/VanillaStorageTests.java`, `fabric-api-lookup-api-v1/src/testmod`.
