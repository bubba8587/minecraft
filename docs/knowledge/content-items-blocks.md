# Items, data components, blocks, block entities (Fabric, MC 26.3)

Scope: registering and implementing items and blocks in Java with Fabric API: Item.Properties, data components, tools/armor/food, use flow, creative tabs, blocks/states/shapes/ticks/redstone/waterlogging, block entities, block transformers, fuel/compost/flammability, fluids. Verified 2026-11-14 against decompiled MC 26.3 + fabric-api 26.3. Path shorthand as in `content.md` (`common/...`, `fapi/<module>`, `docsref/` = fabric-docs reference, which targets 26.2: stale spots flagged).

---

## 1. Registering items

```java
// Mirrors vanilla Items.registerItem (common world/item/Items.java ~2937) and docsref/item/ModItems.java
public static Item register(ResourceKey<Item> key, Function<Item.Properties, Item> factory, Item.Properties props) {
    Item item = factory.apply(props.setId(key));          // setId BEFORE construction (constructor reads it)
    return Registry.register(BuiltInRegistries.ITEM, key, item);
}
public static final Item RUBY = register(ModItemIds.RUBY, Item::new, new Item.Properties());
public static final Item LIGHTNING_STICK = register(ModItemIds.LIGHTNING_STICK, LightningStickItem::new, new Item.Properties().stacksTo(1));
```
What the `Item` constructor does (src: common world/item/Item.java:147): computes `descriptionId` (default `item.<ns>.<path>`), builds a `DataComponentInitializers.Initializer` from the properties and registers it in `BuiltInRegistries.DATA_COMPONENT_INITIALIZERS` under the item key; `ITEM_NAME` = translatable descriptionId and `ITEM_MODEL` = item id are added automatically. Consequences:
- The key passed to `setId` must equal the registration key.
- Components are bound to `item.builtInRegistryHolder()` during datapack load (`ReloadableServerResources`), re-bound on `/reload`. Before that, `item.components()`, `new ItemStack(item)`, `item.getDefaultInstance()` throw `Components not bound yet`. Never build ItemStacks in static initializers (creative tab `icon(() -> new ItemStack(X))` is a lazy Supplier: fine). Use `ItemStackTemplate` for immutable/static stacks (`new ItemStackTemplate(item[, count][, DataComponentPatch])`, `.create()`, `ItemStackTemplate.fromNonEmptyStack(stack)`). `ItemInstance` is the common read interface of both.
- In-IDE runs log `Item classes should end with Item and X doesn't.` (likewise `Block`); harmless but name classes `*Item`/`*Block`.

Fabric `FabricItem.Properties` extensions on `Item.Properties` (fapi fabric-item-api-v1 `FabricItem.java`): `modifyComponent(DataComponentType<T>, (original, registries, key) -> newValue)`, `modifyComponents(DataComponentInitializers.Initializer<Item>)`, `equipmentSlot(EquipmentSlotProvider)`, `customDamage(CustomDamageHandler)`, `modelId(Identifier)` (override `ITEM_MODEL`), `itemId()`. `FabricItem` (on `Item`): `getCraftingRemainder(ItemStack)` (stack-aware, returns `ItemStackTemplate`), `canBeEnchantedWith(...)`, `allowComponentsUpdateAnimation(...)`, `allowContinuingBlockBreaking(...)`, `getCreatorNamespace(ItemStack)`.

### 1.1 `Item.Properties` methods (src: common world/item/Item.java:389-737)
`stacksTo(int)` (MAX_STACK_SIZE, max 99), `durability(int)` (MAX_DAMAGE + DAMAGE=0 + stack 1; durable+stackable throws at validation), `rarity(Rarity)`, `fireResistant()` (delayed `DAMAGE_RESISTANT` with `#is_fire`), `craftRemainder(Item|ItemStackTemplate)`, `usingConvertsTo(Item)` (USE_REMAINDER), `useCooldown(float seconds)`, `food(FoodProperties[, Consumable])`, `villagerFood(int)` (26.3), `enchantable(int)`, `repairable(Item|TagKey<Item>)`, `equippable(EquipmentSlot)`, `equippableUnswappable(slot)`, `tool(material, TagKey<Block> minesEfficiently, dmg, speed, disableBlockingSeconds)`, `pickaxe/axe/hoe/shovel(material, dmg, speed)`, `sword(material, dmg, speed)`, `spear(material, attackDuration, damageMultiplier, delay, dismountTime, dismountThreshold, knockbackTime, knockbackThreshold, damageTime, damageThreshold)`, `humanoidArmor(ArmorMaterial, ArmorType)`, `wolfArmor/horseArmor/nautilusArmor(ArmorMaterial)`, `spawnEgg(EntityType<?>)`, `jukeboxPlayable(ResourceKey<JukeboxSong>)`, `potPattern(ResourceKey<DecoratedPotPattern>)`, `trimMaterial(ResourceKey<TrimMaterial>)`, `signText()`, `cookingFuel(ResourceKey<ContextIntProvider>)` (26.3), `brewingFuel(ResourceKey<ContextIntProvider>)` (26.3), `compostable(ResourceKey<ContextIntProvider>)` (26.3), `loweredMobVisibility(ResourceKey<EntityType<?>>...)` (26.3, MOB_VISIBILITY 0.5, needs EQUIPPABLE), `attributes(ItemAttributeModifiers)`, `component(type, value)`, `delayedComponent(type, ctx -> value)`, `delayedHolderComponent(type, ResourceKey)`, `requiredFeatures(FeatureFlag...)`, `overrideDescription(String)`, `useBlockDescriptionPrefix()`, `useItemDescriptionPrefix()`, `setId(ResourceKey<Item>)`.

`delayedComponent` is the only correct way to put dynamic-registry values (damage types, enchantments, jukebox songs, entity type tags, block transformers) into default components; the lambda gets a `HolderLookup.Provider` (`context.getOrThrow(key)`, `context.lookupOrThrow(Registries.X)`).

Removed/changed vs 1.21.x: `AxeItem`/`ShovelItem`/`HoeItem` (26.3), `SwordItem`/`DiggerItem`/`ArmorItem`/`TieredItem` (1.21.2+ already), `Item#getName()` reads `ITEM_NAME` component, `appendHoverText` is `@Deprecated` in favour of tooltip components, `EitherHolder` removed (26.1), `DyeItem` no longer takes a `DyeColor` (use `DataComponents.DYE`, 26.1), `SpawnEggItem.byId/getType/spawnEntity` now static (26.1).

## 2. Data components

### 2.1 Vanilla component list (26.3, `net.minecraft.core.component.DataComponents`)
Generic: `CUSTOM_DATA, MAX_STACK_SIZE, MAX_DAMAGE, DAMAGE, UNBREAKABLE, USE_EFFECTS, CUSTOM_NAME, ITEM_NAME, ITEM_MODEL, LORE, RARITY, ENCHANTMENTS, STORED_ENCHANTMENTS, CAN_PLACE_ON, CAN_BREAK, ATTRIBUTE_MODIFIERS, CUSTOM_MODEL_DATA, TOOLTIP_DISPLAY, TOOLTIP_STYLE, REPAIR_COST, CREATIVE_SLOT_LOCK, ENCHANTMENT_GLINT_OVERRIDE, INTANGIBLE_PROJECTILE, FOOD, CONSUMABLE, USE_REMAINDER, USE_COOLDOWN, DAMAGE_RESISTANT, TOOL, WEAPON, ATTACK_RANGE, MINIMUM_ATTACK_CHARGE, DAMAGE_TYPE, ENCHANTABLE, EQUIPPABLE, REPAIRABLE, GLIDER, DEATH_PROTECTION, BLOCKS_ATTACKS, PIERCING_WEAPON, KINETIC_WEAPON, ATTACK_ANIMATION, INTERACT_ANIMATION, BREAK_SOUND`; 26.x additions: `ADDITIONAL_TRADE_COST, BLOCK_TRANSFORMER, VILLAGER_FOOD, COMPOSTABLE, COOKING_FUEL, BREWING_FUEL, MOB_VISIBILITY, DYE` (26.1), `SULFUR_CUBE_CONTENT` (26.2), `WAXED`, `CUSHION_COLOR`, `SIGN_TEXT_FRONT/BACK`. Content: `DYED_COLOR, MAP_ID, MAP_DECORATIONS, CHARGED_PROJECTILES, BUNDLE_CONTENTS, POTION_CONTENTS, POTION_DURATION_SCALE, SUSPICIOUS_STEW_EFFECTS, WRITABLE_BOOK_CONTENT, WRITTEN_BOOK_CONTENT, TRIM, ENTITY_DATA, BUCKET_ENTITY_DATA, BLOCK_ENTITY_DATA, INSTRUMENT, PROVIDES_TRIM_MATERIAL, PROVIDES_BANNER_PATTERNS, JUKEBOX_PLAYABLE, RECIPES, LODESTONE_TRACKER, FIREWORK_EXPLOSION, FIREWORKS, PROFILE, NOTE_BLOCK_SOUND, BANNER_PATTERNS, BASE_COLOR, POT_DECORATIONS, CONTAINER, BLOCK_STATE, BEES, LOCK, CONTAINER_LOOT`, plus entity-variant components (`WOLF_VARIANT`, `CAT_VARIANT`, ... `PAINTING_VARIANT`). (src: common core/component/DataComponents.java)

### 2.2 Custom component type
```java
// docsref/component/ModComponents.java; builder src: common core/component/DataComponentType.java
public static final DataComponentType<Integer> CLICK_COUNT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
    ExampleMod.id("click_count"),
    DataComponentType.<Integer>builder()
        .persistent(ExtraCodecs.NON_NEGATIVE_INT)         // Codec: saved to disk/commands/JSON; omit => transient
        .networkSynchronized(ByteBufCodecs.VAR_INT)       // StreamCodec<? super RegistryFriendlyByteBuf, T>; needed for client display
        .build());                                         // also .cacheEncoding(), .ignoreSwapAnimation()

public record Temperature(float temperature, boolean burnt) implements TooltipProvider {
    public static final Codec<Temperature> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.FLOAT.fieldOf("temperature").forGetter(Temperature::temperature),
        Codec.BOOL.optionalFieldOf("burnt", false).forGetter(Temperature::burnt)).apply(i, Temperature::new));
    public static final StreamCodec<ByteBuf, Temperature> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, Temperature::temperature, ByteBufCodecs.BOOL, Temperature::burnt, Temperature::new);
    @Override public void addToTooltip(Item.TooltipContext ctx, Consumer<Component> out, TooltipFlag flag, DataComponentGetter components) {
        out.accept(Component.translatable("item.example-mod.temperature.info", temperature).withStyle(ChatFormatting.GOLD));
    }
}
```
Rules: component values must be immutable and implement `equals/hashCode` (records). A client without the mod cannot decode a synced custom component (Fabric registry sync will refuse the connection for the missing registry entry).

Tooltips from components: vanilla only calls `TooltipProvider` for its own list; register yours with `ItemComponentTooltipProviderRegistry.addAfter(DataComponents.DAMAGE, MY_TYPE)` / `addBefore` / `addFirst` / `addLast` (overloads take `TooltipProvider.Getter<T>` for non-TooltipProvider types). Hide via `TOOLTIP_DISPLAY`. `TooltipFlag#shouldDisplayAllInformation()` (26.3, Fabric+NeoForge, `FabricTooltipFlag`) for recipe viewers.

### 2.3 Reading/writing on stacks
```java
int n = stack.getOrDefault(MY_TYPE, 0);  stack.set(MY_TYPE, n + 1);   // set(type, null) removes
stack.has(T); stack.get(T); stack.remove(T);
stack.update(MY_TYPE, 0, v -> v + 1);
CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("x", 1));   // NBT bag; prefer real components
stack.applyComponents(DataComponentPatch); stack.copy(); stack.consume(1, livingEntity); stack.hurtAndBreak(1, entity, EquipmentSlot.MAINHAND);
```
Prototype (default) components: `item.components()` / `holder.components()` (bound only after datapack load). `ItemStack#getComponentsPatch()` holds the diff.

### 2.4 Changing default components of other items
```java
// fapi fabric-item-api-v1 testmod DefaultItemComponentTest.java
DefaultItemComponentEvents.MODIFY.register(ctx -> {
    ctx.modify(Items.GOLD_INGOT, b -> b.set(DataComponents.ITEM_NAME, Component.literal("Fool's Gold")));
    ctx.modify(Items.BEEF, b -> b.set(DataComponents.FOOD, null));                                 // remove
    ctx.modify(Items.NETHERITE_AXE, (b, registries, item) -> { /* registries.getOrThrow(Enchantments.FIRE_ASPECT) */ });
    ctx.modify(item -> item.components().has(DataComponents.FIREWORKS), (b, item) -> b.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
});
```
Runs at every component bind (datapack load). Use a later event phase (`MODIFY.addPhaseOrdering(Event.DEFAULT_PHASE, late)`) to see other mods' changes.

## 3. Use / interaction flow and `InteractionResult`

`InteractionResult` is a sealed interface (src: common world/InteractionResult.java): `SUCCESS` (swing, client-predicted), `SUCCESS_SERVER` (swing only from server), `CONSUME` (no swing), `FAIL`, `PASS`, `TRY_WITH_EMPTY_HAND` (block `useItemOn` default: fall through to `useWithoutItem`). `Success#heldItemTransformedTo(ItemStack)` replaces the held stack (buckets, bottles); `withoutItem()` marks not-an-item-interaction. There is no `InteractionResultHolder` anymore (removed 1.21.2).

Item hooks (src: common world/item/Item.java):
```java
InteractionResult use(Level level, Player player, InteractionHand hand);     // right-click air (and fallback). Default handles CONSUMABLE, swappable EQUIPPABLE, BLOCKS_ATTACKS, KINETIC_WEAPON
InteractionResult useOn(UseOnContext ctx);                                    // right-click block. Default applies BLOCK_TRANSFORMER component (26.3)
InteractionResult interactLivingEntity(ItemStack, Player, LivingEntity target, InteractionHand);
void onUseTick(Level, LivingEntity, ItemStack, int ticksRemaining);           // while held use
ItemStack finishUsingItem(ItemStack, Level, LivingEntity);                    // after getUseDuration ticks; default CONSUMABLE.onConsume
boolean releaseUsing(ItemStack, Level, LivingEntity, int remainingTime);      // returns boolean now
int getUseDuration(ItemStack, LivingEntity);  ItemUseAnimation getUseAnimation(ItemStack);  boolean useOnRelease(ItemStack);
void inventoryTick(ItemStack, ServerLevel, Entity owner, @Nullable EquipmentSlot slot);       // server-only signature
boolean mineBlock(...); void hurtEnemy(ItemStack, LivingEntity target, LivingEntity attacker); void postHurtEnemy(...);
float getAttackDamageBonus(Entity victim, float damage, DamageSource); @Nullable DamageSource getItemDamageSource(LivingEntity);
boolean overrideStackedOnOther(...); boolean overrideOtherStackedOnMe(...); Optional<TooltipComponent> getTooltipImage(ItemStack);
void onCraftedBy(ItemStack, Player); boolean isFoil(ItemStack); boolean isBarVisible/ getBarWidth/ getBarColor(ItemStack);
```
Continuous use: in `use` call `player.startUsingItem(hand); return InteractionResult.CONSUME;` and give `getUseDuration` > 0 (or just use a `Consumable` component).

Example (docsref/item/custom/LightningStick.java, valid on 26.3):
```java
@Override public InteractionResult use(Level level, Player user, InteractionHand hand) {
    if (level.isClientSide()) return InteractionResult.PASS;            // isClientSide() is a method
    BlockPos p = user.blockPosition().relative(user.getDirection(), 10);
    LightningBolt bolt = new LightningBolt(EntityTypes.LIGHTNING_BOLT, level);   // EntityTypes, not EntityType
    bolt.setPos(Vec3.atBottomCenterOf(p)); level.addFreshEntity(bolt);
    return InteractionResult.SUCCESS;
}
```
Cooldowns: `player.getCooldowns().addCooldown(stack, ticks)` / `addCooldown(Identifier group, ticks)` / `isOnCooldown(stack)` (src: common world/item/ItemCooldowns.java); or declaratively `Item.Properties#useCooldown(seconds)` / `new UseCooldown(seconds, Optional.of(groupId))` which vanilla applies after successful use.

Fabric events instead of subclassing (fapi fabric-events-interaction-v0): `UseItemCallback.EVENT` `(player, level, hand)`, `UseBlockCallback.EVENT` `(player, level, hand, BlockHitResult)`, `UseEntityCallback.EVENT`, `AttackBlockCallback`, `AttackEntityCallback`, `PlayerBlockBreakEvents.BEFORE/AFTER/CANCELED`. 26.1 added narrower ones that keep vanilla logic around the method call: `ItemEvents.USE_ON` / `ItemEvents.USE`, `BlockEvents.USE_ITEM_ON` / `BlockEvents.USE_WITHOUT_ITEM` (return non-null to replace the call). `ItemClickBehaviorCallback` for inventory click behaviour.

## 4. Tools, weapons, shields

```java
// ToolMaterial is a record (src: common world/item/ToolMaterial.java)
// (TagKey<Block> incorrectBlocksForDrops, int durability, float speed, float attackDamageBonus, int enchantmentValue, TagKey<Item> repairItems)
public static final TagKey<Block> INCORRECT_FOR_GUIDITE = TagKey.create(Registries.BLOCK, ExampleMod.id("incorrect_for_guidite_tool"));
public static final ToolMaterial GUIDITE = new ToolMaterial(INCORRECT_FOR_GUIDITE, 455, 5.0F, 1.5F, 22, ModTags.REPAIRS_GUIDITE);

GUIDITE_SWORD   = register(ids.SWORD,   Item::new, new Item.Properties().sword(GUIDITE, 3.0F, -2.4F));
GUIDITE_PICKAXE = register(ids.PICKAXE, Item::new, new Item.Properties().pickaxe(GUIDITE, 1.0F, -2.8F));
GUIDITE_AXE     = register(ids.AXE,     Item::new, new Item.Properties().axe(GUIDITE, 6.0F, -3.1F));   // 26.3: no AxeItem
GUIDITE_SHOVEL  = register(ids.SHOVEL,  Item::new, new Item.Properties().shovel(GUIDITE, 1.5F, -3.0F));
GUIDITE_HOE     = register(ids.HOE,     Item::new, new Item.Properties().hoe(GUIDITE, -2.0F, -1.0F));
GUIDITE_SPEAR   = register(ids.SPEAR,   Item::new, new Item.Properties().spear(GUIDITE, 0.95F, 0.95F, 0.6F, 2.5F, 8.0F, 6.75F, 5.1F, 11.25F, 4.6F)); // see Items.java for vanilla numbers
```
- `pickaxe/axe/hoe/shovel` = `tool(material, BlockTags.MINEABLE_WITH_X, dmg, speed, disableBlocking)` + for axe/hoe/shovel a delayed `BLOCK_TRANSFORMER` pointing at `BlockTransformers.AXE/HOE/SHOVEL` (that is how stripping/tilling/pathing works in 26.3). Axes set `disableBlockingSeconds = 5`.
- Attack damage/speed args are added to the player base (1 dmg, 4.0 speed) via `ATTRIBUTE_MODIFIERS` with ids `Item.BASE_ATTACK_DAMAGE_ID` / `BASE_ATTACK_SPEED_ID`.
- The block tag (`incorrect_for_*`) must exist as data (`data/<ns>/tags/block/incorrect_for_guidite_tool.json`), typically including `#minecraft:incorrect_for_iron_tool` etc. Item tags `minecraft:pickaxes`, `swords`, ... are what enchantments and vanilla checks use: add your tools to them.
- Components directly: `new Tool(List<Tool.Rule> rules, float defaultMiningSpeed, int damagePerBlock, boolean canDestroyBlocksInCreative)` with `Tool.Rule.minesAndDrops(HolderSet<Block>, speed)`, `deniesDrops(...)`, `overrideSpeed(...)`; `new Weapon(int itemDamagePerAttack, float disableBlockingForSeconds)`; `KineticWeapon`, `PiercingWeapon`, `AttackRange`, `SwingAnimation` (spears, 1.21.11+).
- Shield (docsref/item/ModItems.java, valid on 26.3): `ShieldItem::new` with `.durability(336).component(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY).repairable(tag).equippableUnswappable(EquipmentSlot.OFFHAND).delayedComponent(DataComponents.BLOCKS_ATTACKS, ctx -> new BlocksAttacks(0.25F, 1.0F, List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)), new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F), Optional.of(ctx.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)), Optional.of(SoundEvents.SHIELD_BLOCK), Optional.of(SoundEvents.SHIELD_BREAK))).component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK)`. Any item with `BLOCKS_ATTACKS` blocks on use.
- Tweak a builder-made component in place: `Item.Properties#modifyComponent(DataComponents.TOOL, (orig, registries, key) -> new Tool(...))` (fapi testmod `ModifyComponentsInPropertiesTestSetup`).

## 5. Armor / equipment
```java
// docsref/item/armor/GuiditeArmorMaterial.java; src: common world/item/equipment/ArmorMaterial.java
public static final ResourceKey<EquipmentAsset> GUIDITE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, ExampleMod.id("guidite"));
public static final ArmorMaterial GUIDITE_ARMOR = new ArmorMaterial(
    15,                                                         // base durability multiplier
    Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 8, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3, ArmorType.BODY, 11),
    5,                                                          // enchantment value
    SoundEvents.ARMOR_EQUIP_IRON,                               // Holder<SoundEvent>
    0.0F, 0.0F,                                                 // toughness, knockback resistance
    ModTags.REPAIRS_GUIDITE,                                    // TagKey<Item>
    GUIDITE_ASSET);
HELMET = register(ids.HELMET, Item::new, new Item.Properties().humanoidArmor(GUIDITE_ARMOR, ArmorType.HELMET));
// humanoidArmor already sets durability(type.getDurability(material.durability())); docsref sets it again (redundant)
```
- `Equippable.builder(EquipmentSlot)` -> `.setEquipSound(Holder<SoundEvent>)`, `.setAsset(ResourceKey<EquipmentAsset>)`, `.setCameraOverlay(Identifier)`, `.setAllowedEntities(EntityType<?>... | HolderSet)`, `.setDispensable`, `.setSwappable`, `.setDamageOnHurt`, `.setEquipOnInteract`, `.setCanBeSheared`, `.setShearingSound`, `.build()`. Any item can be wearable via `EQUIPPABLE`.
- Client assets: `assets/<ns>/equipment/<asset path>.json` (layers -> `textures/entity/equipment/<layer type>/<texture>.png`); item model as usual. See resource-pack/client docs.
- Animal armor: `wolfArmor`, `horseArmor`, `nautilusArmor` (26.x). Elytra-like: `GLIDER` component + `EQUIPPABLE` chest; Fabric `EntityElytraEvents.ALLOW/CUSTOM`.

## 6. Food and consumables
```java
// docsref/item/ModItems.java (valid on 26.3)
public static final Consumable POISON_CONSUMABLE = Consumables.defaultFood()
    .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.POISON, 6 * 20, 1), 1.0f)).build();
public static final FoodProperties POISON_FOOD = new FoodProperties.Builder().nutrition(4).saturationModifier(0.3F).alwaysEdible().build();
POISONOUS_APPLE = register(ids.POISONOUS_APPLE, Item::new, new Item.Properties().food(POISON_FOOD, POISON_CONSUMABLE));
```
- `FoodProperties(int nutrition, float saturation, boolean canAlwaysEat)`; `food(FoodProperties)` uses `Consumables.DEFAULT_FOOD`.
- `Consumable(float consumeSeconds, ItemUseAnimation animation, Holder<SoundEvent> sound, boolean hasConsumeParticles, List<ConsumeEffect> onConsumeEffects)`; builder `consumeSeconds`, `animation`, `sound`, `soundAfterConsume`, `hasConsumeParticles`, `onConsume`. Presets: `Consumables.defaultFood()`, `defaultDrink()`, `DEFAULT_FOOD`, `DEFAULT_DRINK`, `HONEY_BOTTLE`, `MILK_BUCKET`, `CHORUS_FRUIT`, `GOLDEN_APPLE`, ...
- Consume effects (`net.minecraft.world.item.consume_effects`): `ApplyStatusEffectsConsumeEffect`, `RemoveStatusEffectsConsumeEffect`, `ClearAllStatusEffectsConsumeEffect.INSTANCE`, `TeleportRandomlyConsumeEffect`, `PlaySoundConsumeEffect`; custom types register a `MapCodec` in `BuiltInRegistries.CONSUME_EFFECT_TYPE`.
- Non-food consumable: only `CONSUMABLE` (e.g. potions, drinks). Leftover: `usingConvertsTo(Items.GLASS_BOTTLE)` (`USE_REMAINDER`).
- Villager food: `villagerFood(nutrition)` (26.3; replaces hard-coded villager food map).

## 7. Fuel, compost, brewing fuel (26.3 components)
Values are `ResolvableInt`/`ResolvableFloat`: either a constant or a reference to a reloadable `context_int_provider`/`context_float_provider` entry (resolved with a loot context; vanilla references let smokers/blast furnaces halve burn time).
```java
// Reference vanilla providers (recommended; behaves like vanilla in smokers/blast furnaces):
new Item.Properties().cookingFuel(ContextIntProviders.COOKING_TIME_COAL)               // 1600 ticks in furnace, 800 in fast furnaces
                     .compostable(ContextIntProviders.COMPOSTABLE_MEDIUM)              // 65% chance of +1 layer
                     .brewingFuel(ContextIntProviders.BREWING_DEFAULT_USES);           // 20 uses
// Constants:
new Item.Properties().component(DataComponents.COOKING_FUEL,
    new CookingFuel(new ResolvableInt.Constant(2400), ResolvableFloat.fromKey(ContextFloatProviders.COOKING_DEFAULT_SPEED_MULTIPLIER)));
new Item.Properties().component(DataComponents.COMPOSTABLE, new Compostable(new ResolvableInt.Constant(1))); // always +1 layer
// Other mods' / vanilla items:
DefaultItemComponentEvents.MODIFY.register(ctx -> ctx.modify(Items.STICK,
    b -> b.set(DataComponents.COMPOSTABLE, new Compostable(ContextIntProviders.COMPOSTABLE_LOW))));
```
- `Compostable.layers` = number of layers added (clamped 0..7), not a percent. Vanilla keys: `COMPOSTABLE_LOW` (30%), `LOW_MEDIUM` (50%), `MEDIUM` (65%), `MEDIUM_HIGH` (85%), `ALWAYS_ADD_ONE` (100%) (src: common world/level/storage/loot/providers/number/ints/ContextIntProviders.java). The Fabric 26.3 blog example `new Compostable(new ResolvableInt.Constant(1*100))` is wrong (adds 100 layers -> instantly full).
- Cooking keys: `COOKING_TIME_BAMBOO` 50, `WOOL_SLABS` 50, `WOOL_CARPETS` 67, `DRY_PLANTS` 100, `WOOD_ITEMS_EXTRA_SMALL` 100, `WOOL` 100, `WOOD_SLABS` 150, `WOOD_ITEMS_LARGE` 200, `ROOTS`/`WOOD_BLOCKS`/`WOOD_ITEMS_SMALL` 300, `HANGING_SIGNS` 800, `BOATS` 1200, `COAL` 1600, `BLAZE_ROD` 2400, `DRIED_KELP_BLOCK` 4001, `COAL_BLOCK` 16000, `LAVA_BUCKET` 20000. A constant burn time does NOT get halved in blast furnace/smoker; define your own `data/<ns>/context_int_provider/...json` like vanilla's `cooking/time_coal.json` (`{"type":"minecraft:div","left":1600,"right":{"type":"minecraft:conditional","condition":"minecraft:block/fast_cooking",...}}`) and reference it with `cookingFuel(myKey)`.
- Furnace checks `itemStack.has(DataComponents.COOKING_FUEL)`; composter checks `COMPOSTABLE`; there is no item-tag path.
- Removed in 26.3: `FuelRegistry`, `FuelValueEvents` (docsref still uses it), `CompostingChanceRegistry`/`CompostableRegistry` (docsref still uses it), vanilla `FuelValues`.

## 8. Creative tabs (fapi fabric-creative-tab-api-v1)
```java
// docsref/item/ModItems.java
public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, ExampleMod.id("creative_tab"));
public static final CreativeModeTab TAB = FabricCreativeModeTab.builder()        // no Row/column needed (paginated)
    .icon(() -> new ItemStack(ModItems.GUIDITE_SWORD))                            // Supplier: lazy, fine
    .title(Component.translatable("itemGroup.example-mod"))
    .displayItems((params, output) -> { output.accept(ModItems.RUBY); output.accept(ModBlocks.RUBY_BLOCK); })
    .build();
Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, TAB);

// Add to vanilla tabs
CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out -> out.accept(ModItems.RUBY));
CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(out -> out.accept(ModItems.MINI_GOLEM_SPAWN_EGG));
CreativeModeTabEvents.MODIFY_OUTPUT_ALL.register((tab, out) -> {});
// FabricCreativeModeTabOutput: accept, prepend, insertAfter(ItemLike|ItemStack|Predicate, ...), insertBefore(...), with TabVisibility
```
Vanilla tab keys: `BUILDING_BLOCKS, COLORED_BLOCKS, NATURAL_BLOCKS, FUNCTIONAL_BLOCKS, REDSTONE_BLOCKS, TOOLS_AND_UTILITIES, COMBAT, FOOD_AND_DRINKS, INGREDIENTS, SPAWN_EGGS, OP_BLOCKS` (src: common world/item/CreativeModeTabs.java). Renamed in 26.1 from `ItemGroupEvents`/`FabricItemGroup`/`FabricItemGroupEntries`.

## 9. Spawn eggs, block items, buckets, misc item types
- Spawn egg: `register(ids.MINI_GOLEM_SPAWN_EGG, SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntityTypes.MINI_GOLEM))` (sets `ENTITY_DATA` + feature flags). The entity type must be initialized first (class-load order). Spawn egg colours are no longer item properties: give it an item model/texture like any item (client doc).
- Block item: `new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(itemKey))`. Without `useBlockDescriptionPrefix()` the name key is `item.<ns>.<path>` instead of `block.<ns>.<path>`. Vanilla `Items.registerItem` calls `blockItem.registerBlocks(Item.BY_BLOCK, item)`; Fabric's registry-sync `BlockItemTracker` does this for you on `Registry.register`, so `block.asItem()` works. Variants: `DoubleHighBlockItem`, `StandingAndWallBlockItem`, `PlaceOnWaterBlockItem`, `ScaffoldingBlockItem`, `HangingSignItem`, `GameMasterBlockItem`.
- Bucket for a custom fluid: `props -> new BucketItem(ModFluids.ACID_STILL, props)` with `.craftRemainder(Items.BUCKET).stacksTo(1)` (docsref).
- Projectile items: implement `ProjectileItem#asProjectile(Level, Position, ItemStack, Direction)` for dispenser support (see entities doc).
- Dye: `.component(DataComponents.DYE, DyeColor.RED)` + tags `minecraft:dyes`, `wolf_collar_dyes`, `cat_collar_dyes`, `loom_dyes` (26.1).
- Music disc: `.jukeboxPlayable(ResourceKey<JukeboxSong>)` + `data/<ns>/jukebox_song/<id>.json` (data-driven only).

## 10. Item model/texture link (pointers only)
`ITEM_MODEL` defaults to the item id -> `assets/<ns>/items/<path>.json` (item model definition, 1.21.4+) -> e.g. `{"model":{"type":"minecraft:model","model":"<ns>:item/<path>"}}` -> `assets/<ns>/models/item/<path>.json` -> `textures/item/<path>.png`. Lang: `assets/<ns>/lang/en_us.json` keys `item.<ns>.<path>`, `block.<ns>.<path>`. Override model id: `Item.Properties#modelId(Identifier)` (Fabric) or `.component(DataComponents.ITEM_MODEL, id)`. Datagen: `FabricModelProvider` (client doc / datagen doc).

---

## 11. Registering blocks
```java
// docsref/block/ModBlocks.java; vanilla Blocks.register (common world/level/block/Blocks.java:5655)
private static Block register(ResourceKey<Block> key, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties props) {
    return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(props.setId(key)));
}
private static Block register(BlockItemId id, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties props) {
    Block block = register(id.block(), factory, props);
    Registry.register(BuiltInRegistries.ITEM, id.item(), new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(id.item())));
    return block;
}
public static final Block RUBY_BLOCK = register(ModBlockItemIds.RUBY_BLOCK, Block::new,
    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(5.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
public static final Block RUBY_STAIRS = register(ids.RUBY_STAIRS, p -> new StairBlock(RUBY_BLOCK.defaultBlockState(), p), BlockBehaviour.Properties.ofFullCopy(RUBY_BLOCK));
public static final Block RUBY_DOOR = register(ids.RUBY_DOOR, p -> new DoorBlock(BlockSetType.IRON, p), BlockBehaviour.Properties.of().noOcclusion());
```
Collections of colored blocks: `ColorCollection.registerBlocks(baseName, registerFn, factory, props...)` (26.2; see vanilla `Blocks.WOOL`, accessed as `Blocks.WOOL.white()`).

`BlockBehaviour.Properties` (src: common world/level/block/state/BlockBehaviour.java): `of()`, `ofFullCopy(block)`, `ofLegacyCopy(block)`, `mapColor(MapColor|DyeColor|Function<BlockState,MapColor>)`, `noCollision()`, `noOcclusion()`, `friction`, `speedFactor`, `jumpFactor`, `bounceRestitution`, `fallDistanceReduction` (new), `sound(SoundType)`, `lightLevel(ToIntFunction<BlockState>)`, `strength(destroy, blast)`, `strength(f)`, `instabreak()`, `destroyTime`, `explosionResistance`, `randomTicks()`, `dynamicShape()`, `noLootTable()`, `overrideLootTable(Optional<ResourceKey<LootTable>>)`, `ignitedByLava()`, `liquid()`, `forceSolidOn/Off()`, `pushReaction(PushReaction)`, `air()`, `isValidSpawn(...)`, `isRedstoneConductor(...)`, `isSuffocating(...)`, `isViewBlocking(StateArgumentPredicate<AABB>)` (now takes an AABB argument), `postProcess`, `emissiveRendering(Predicate<BlockState>)`, `requiresCorrectToolForDrops()`, `offsetType(OffsetType)`, `noTerrainParticles()`, `requiredFeatures(...)`, `instrument(NoteBlockInstrument)`, `replaceable()`, `setId(ResourceKey<Block>)`, `overrideDescription(String)`.
Fabric extras: `FabricBlock.FabricProperties#blockId()` / `blockIdOrThrow()`; `FabricBlock#getAppearance(...)` (connected textures), `FabricBlock#getProvidedEnchantmentPower(...)` (26.3); tag `fabric:can_climb_trapdoor_above` (`BlockFunctionalityTags`).

Mining: hardness from `strength`; tool requirement = `requiresCorrectToolForDrops()` + block tags `minecraft:mineable/pickaxe` etc. + `needs_iron_tool`/`incorrect_for_*` tags (data). Drops come from the block loot table; forgetting it = no drops.

## 12. Block subclasses: states, shapes, interaction
No `codec()` override in 26.3 (removed with the `block_type` registry; delete `MapCodec CODEC` + `codec()` from ported code, including on `BaseEntityBlock` subclasses; docsref still shows it).
```java
public class LampBlock extends Block {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");    // or BlockStateProperties.LIT
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.boxZ(16.0, 13.0, 16.0)); // pre-rotated (north input)

    public LampBlock(BlockBehaviour.Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(FACING, Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LIT, FACING); }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPES.get(s.getValue(FACING)); }
    @Override protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) return InteractionResult.PASS;
        level.setBlockAndUpdate(pos, s.cycle(LIT));
        level.playSound(player, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState s, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;  // default: falls through to useWithoutItem
    }
    @Override protected BlockState rotate(BlockState s, Rotation r) { return s.setValue(FACING, r.rotate(s.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState s, Mirror m) { return s.rotate(m.getRotation(s.getValue(FACING))); }
}
// light: BlockBehaviour.Properties.of().lightLevel(s -> s.getValue(LampBlock.LIT) ? 15 : 0)
```
- Properties: `BooleanProperty.create(name)`, `IntegerProperty.create(name, min, max)`, `EnumProperty.create(name, Class<E extends Enum & StringRepresentable>[, values])`; shared ones in `BlockStateProperties`. Every state combination needs a blockstate JSON variant (client).
- Shapes (`net.minecraft.world.phys.shapes`): `Block.box(x1,y1,z1,x2,y2,z2)` (0..16 pixel units), `Block.cube(size)`, `Block.column(sizeXZ, minY, maxY)`, `Block.boxZ(...)`, `Shapes.or(...)`, `Shapes.block()`, `Shapes.empty()`, `Shapes.rotateHorizontal(north)`, `rotateAll`, `rotateHorizontalAxis`; `getShapeForEachState(fn[, ignoredProps...])` caches per state. Overrides: `getShape`, `getCollisionShape`, `getInteractionShape`, `getVisualShape`, `getOcclusionShape(BlockState)`, `getBlockSupportShape`, `getEntityInsideCollisionShape`. Use `noOcclusion()` for non-full blocks.
- Lifecycle/neighbour hooks (protected unless noted): `onPlace(state, level, pos, oldState, movedByPiston)`, `affectNeighborsAfterRemoval(state, ServerLevel, pos, movedByPiston)` (replaces old `onRemove`), `neighborChanged(state, level, pos, Block, @Nullable Orientation, movedByPiston)`, `updateShape(state, LevelReader, ScheduledTickAccess, pos, Direction, neighborPos, neighborState, RandomSource)`, `canSurvive(state, LevelReader, pos)`, `entityInside(state, level, pos, entity, InsideBlockEffectApplier, boolean isPrecise)`, `onProjectileHit`, `onExplosionHit`, `attack(state, level, pos, player)`; public: `setPlacedBy(level, pos, state, @Nullable LivingEntity, ItemStack)`, `playerWillDestroy(level, pos, state, player)` (returns BlockState), `playerDestroy(...)`, `stepOn(level, pos, state, entity)`, `fallOn(level, state, pos, entity, double fallDistance)`, `animateTick(state, level, pos, random)` (client particles), `getCloneItemStack(LevelReader, pos, state, boolean includeData)`, `getMenuProvider`.
- Update flags: `Block.UPDATE_NEIGHBORS` 1, `UPDATE_CLIENTS` 2, `UPDATE_ALL` 3; `level.setBlock(pos, state, flags)`, `setBlockAndUpdate`.

### 12.1 Ticks
- Random ticks: `.randomTicks()` (or override `isRandomlyTicking(state)`) + `@Override protected void randomTick(BlockState, ServerLevel, BlockPos, RandomSource)`.
- Scheduled ticks: `level.scheduleTick(pos, this, delayTicks)` (from `ScheduledTickAccess`/`LevelAccessor`, also inside `updateShape` via the `ticks` param) + `@Override protected void tick(BlockState, ServerLevel, BlockPos, RandomSource)`.
- Per-tick logic needs a block entity ticker (section 14).

### 12.2 Redstone
```java
@Override protected boolean isSignalSource(BlockState s) { return true; }
@Override protected int getSignal(BlockState s, BlockGetter level, BlockPos pos, Direction dir) { return s.getValue(POWERED) ? 15 : 0; } // weak; default delegates to ownSignal
@Override protected int getDirectSignal(BlockState s, BlockGetter level, BlockPos pos, Direction dir) { return 0; }       // strong
@Override protected boolean hasAnalogOutputSignal(BlockState s) { return true; }
@Override protected int getAnalogOutputSignal(BlockState s, Level level, BlockPos pos, Direction dir) { return 7; }       // comparator (now gets a Direction)
// reading: level.hasNeighborSignal(pos), level.getBestNeighborSignal(pos); notify: level.updateNeighborsAt(pos, this)
// react: neighborChanged(...)  (Orientation replaces fromPos since 1.21.2)
```
New hook: `ownSignal(BlockState, BlockGetter, BlockPos)` is what default `getSignal` returns. `shouldRedstoneWireConnectTo(state, level, pos, @Nullable Direction)`.

### 12.3 Waterlogging (pattern from common world/level/block/LanternBlock.java)
```java
public class PipeBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    // add WATERLOGGED in createBlockStateDefinition and default state (false)
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(WATERLOGGED, ctx.getLevel().getFluidState(ctx.getClickedPos()).is(Fluids.WATER));
    }
    @Override protected BlockState updateShape(BlockState s, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction d, BlockPos np, BlockState ns, RandomSource r) {
        if (s.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(s, level, ticks, pos, d, np, ns, r);
    }
    @Override protected FluidState getFluidState(BlockState s) {
        return s.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(s);
    }
}
```

## 13. Block content registries (fapi fabric-content-registries-v0, 26.3 contents)
Remaining APIs: `FlammableBlockRegistry`, `OxidizableBlocksRegistry`, `LandPathTypeRegistry`, `VibrationFrequencyRegistry`, `VillagerInteractionRegistries`, `fluid.EntityFluidInteractionRegistry`/`FluidBehavior`. Everything else moved.
```java
FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.RUBY_PLANKS, 5, 20);   // (igniteOdds, burnOdds); vanilla planks 5/20, logs 5/5, leaves 30/60
FlammableBlockRegistry.getDefaultInstance().add(ModTags.MY_LOGS, 5, 5);            // tag overload; getInstance(fireBlock) for other fire blocks
OxidizableBlocksRegistry.registerNextStage(COPPER_THING, EXPOSED_COPPER_THING);
OxidizableBlocksRegistry.registerWaxable(COPPER_THING, WAXED_COPPER_THING);
OxidizableBlocksRegistry.registerWeatheringCopperBlocks(WeatheringCopperCollection<Block>);   // 26.2 collections
LandPathTypeRegistry.register(block, PathType.DAMAGING, PathType.DAMAGING_NEIGHBOR);          // AI pathing
```
Oxidizing blocks must also behave as `WeatheringCopper` (random tick) for oxidation to happen; the registry only provides the mapping.

### 13.1 Block transformers: stripping, tilling, flattening, scraping (26.3)
`DataComponents.BLOCK_TRANSFORMER` (`Holder<BlockTransformer>`) on the held item; `Item#useOn` default calls `transformer.transformBlock(ctx)`. Vanilla transformers `minecraft:axe`, `minecraft:hoe`, `minecraft:shovel` (`BlockTransformers.AXE/HOE/SHOVEL`) in `data/minecraft/block_transformer/*.json`.
```java
// Add to vanilla transformers from code (fapi fabric-item-api-v1 BlockTransformerHelper; testmod CustomBlockTransformsTest)
BlockTransformerHelper.registerStripping(ModBlocks.RUBY_LOG, ModBlocks.STRIPPED_RUBY_LOG);        // wraps target in CopyPropertiesProvider (keeps axis)
BlockTransformerHelper.registerTilling(ModBlocks.RICH_DIRT, Blocks.FARMLAND);
BlockTransformerHelper.registerFlattening(new Block[]{A, B}, Blocks.DIRT_PATH);
// overloads: (Block|Block[]|TagKey<Block>|BlockPredicate from, Block|BlockState|BlockStateProvider to)
BlockTransformerHelper.registerHoe(BlockTransformer.BlockTransformData.builder(
        BlockPredicate.allOf(BlockPredicate.solid(), BlockPredicate.matchesBlocks(Direction.DOWN, Blocks.BEDROCK)), Blocks.PACKED_ICE).build());
// registerAxe/registerHoe/registerShovel(BlockTransformData | BlockTransformer)

// Modify any transformer (e.g. another mod's):
BlockTransformerEvents.MODIFY.register((key, transforms, source, registries) -> {
    if (source.isBuiltIn() && key == SomeMod.CHISEL) transforms.add(BlockTransformer.BlockTransformData.builder(
        BlockPredicate.matchesBlocks(Blocks.NETHER_BRICKS), Blocks.CRACKED_NETHER_BRICKS).sound(Holder.direct(SoundEvents.NETHER_BRICKS_HIT)).build());
});
```
- Your own new transformer: JSON in `data/<ns>/block_transformer/<id>.json` (a list of transform data), then give items `.delayedComponent(DataComponents.BLOCK_TRANSFORMER, ctx -> ctx.getOrThrow(MY_TRANSFORMER_KEY))` or via `DefaultItemComponentEvents` (fapi testmod gives all `#minecraft:pickaxes` a custom transformer). `BlockTransformData` JSON fields: `block_state_provider` (required; usually `minecraft:rule_based` with `rules: [{if_true: <block predicate>, then: <state provider>}]`), `sound`, `particle`, `disallowed_faces`, `loot`, `drop_strategy`, `update_from_neighbors` (default true), `transform_type`, `consume_on_use` (default true), `item_damage_per_use` (default 0) (src: common core/component/BlockTransformer.java:123-155). Vanilla axe rules use `{"type":"minecraft:copy_properties","source":{...}}` to keep the `axis`.
- The `registerStripping/Tilling/Flattening` helpers wrap the target provider in `CopyPropertiesProvider` and are kept equivalent to vanilla's transform data for that tool (e.g. stripping sets `SoundEvents.AXE_STRIP`) (src: fapi fabric-item-api-v1 impl/item/BlockTransformerHelperImpl.java); `registerAxe/Hoe/Shovel(BlockTransformData)` add your data as-is (no sound unless you set one).
- Removed: `StrippableBlockRegistry`, `TillableBlockRegistry`, `FlattenableBlockRegistry`, vanilla `AxeItem.STRIPPABLES`, `ShovelItem.FLATTENABLES`, `HoeItem.TILLABLES`.

## 14. Block entities
```java
// Block
public class CounterBlock extends BaseEntityBlock {            // or extends Block implements EntityBlock
    public CounterBlock(Properties p) { super(p); }             // NO codec() in 26.3
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CounterBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState s, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.COUNTER, CounterBlockEntity::tick);
    }
}
// Type registration (either form)
public static final BlockEntityType<CounterBlockEntity> COUNTER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
    ExampleMod.id("counter"), FabricBlockEntityTypeBuilder.create(CounterBlockEntity::new, ModBlocks.COUNTER_BLOCK).build());
// vanilla ctor is public now: new BlockEntityType<>(CounterBlockEntity::new, Set.of(ModBlocks.COUNTER_BLOCK))
// FabricBlockEntityTypeBuilder: addBlock(s), canPotentiallyExecuteCommands(bool); build(Type) deprecated. type.addValidBlock(block) via FabricBlockEntityType.

// Block entity
public class CounterBlockEntity extends BlockEntity {
    private int clicks;
    public CounterBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.COUNTER, pos, state); }
    public static void tick(Level level, BlockPos pos, BlockState state, CounterBlockEntity be) { /* server tick */ }

    @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); out.putInt("clicks", clicks); }
    @Override protected void loadAdditional(ValueInput in)  { super.loadAdditional(in); clicks = in.getIntOr("clicks", 0); }

    // client sync
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    public void increment() { clicks++; setChanged(); level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS); }
}
```
- Saving uses `ValueOutput`/`ValueInput` (`net.minecraft.world.level.storage`, since 1.21.6), not raw NBT: `putInt/putLong/putFloat/putDouble/putBoolean/putString/putIntArray`, `store(name, Codec<T>, value)`, `storeNullable`, `store(MapCodec, value)`, `child(name)`, `childrenList(name)`, `list(name, Codec)`, `discard(name)`; reading `getIntOr(name, def)`, `getInt(name)` -> Optional, `getStringOr`, `read(name, Codec)` -> Optional, `child`/`childOrEmpty`, `childrenListOrEmpty`, `listOrEmpty(name, Codec)`, `lookup()` (HolderLookup.Provider). Items: `out.store("Item", ItemStack.CODEC, stack)`; inventories: `ContainerHelper.saveAllItems(ValueOutput, NonNullList<ItemStack>[, boolean alsoWhenEmpty])` / `ContainerHelper.loadAllItems(ValueInput, NonNullList<ItemStack>)` (src: common world/ContainerHelper.java).
- `setChanged()` marks the chunk dirty but does NOT sync; call `level.sendBlockUpdated(...)` to push `getUpdatePacket()` to watchers. `getUpdateTag` is also used for initial chunk data.
- Item <-> block entity data: override `applyImplicitComponents(DataComponentGetter)`, `collectImplicitComponents(DataComponentMap.Builder)`, `removeComponentsFromTag(ValueOutput)`; stacks carry `BLOCK_ENTITY_DATA` (`TypedEntityData<BlockEntityType<?>>`).
- Removal hook: `BlockEntity#preRemoveSideEffects(BlockPos, BlockState)` (drop contents here; replaces doing it in `onRemove`). `setRemoved()`.
- Inventory: implement `Container`/`WorldlyContainer` (docsref/container/ImplementedContainer.java) or use Fabric Transfer API (`ContainerStorage`, `ItemStorage.SIDED`). Menus: menu doc.
- Renderer: `BlockEntityRenderers.register(type, Renderer::new)` (client; render-state based, client doc).
- Fabric block-entity data attachments (fabric-data-attachment-api-v1) let you add data to vanilla BEs without mixins.

## 15. Fluids
```java
// docsref/fluid/ (26.2, signatures match 26.3 FlowingFluid/Fluid abstract methods)
public abstract class AcidFluid extends FlowingFluid {
    @Override public Fluid getFlowing() { return ModFluids.ACID_FLOWING; }
    @Override public Fluid getSource() { return ModFluids.ACID_STILL; }
    @Override public Item getBucket() { return ModItems.ACID_BUCKET; }
    @Override protected boolean canConvertToSource(ServerLevel level) { return false; }
    @Override protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) { Block.dropResources(state, level, pos, state.hasBlockEntity() ? level.getBlockEntity(pos) : null); }
    @Override protected int getSlopeFindDistance(LevelReader level) { return 4; }
    @Override public int getDropOff(LevelReader level) { return 1; }
    @Override public int getTickDelay(LevelReader level) { return 5; }
    @Override protected float getExplosionResistance() { return 100f; }
    @Override protected boolean canBeReplacedWith(FluidState s, BlockGetter level, BlockPos pos, Fluid other, Direction dir) { return false; }
    @Override protected BlockState createLegacyBlock(FluidState s) { return ModBlocks.ACID.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(s)); }
    @Override public boolean isSame(Fluid f) { return f == getSource() || f == getFlowing(); }
    public static class Flowing extends AcidFluid { /* createFluidStateDefinition adds LEVEL; getAmount = state.getValue(LEVEL); isSource false */ }
    public static class Source extends AcidFluid { /* getAmount 8; isSource true */ }
}
ACID_STILL = Registry.register(BuiltInRegistries.FLUID, ModFluidIds.ACID_STILL, new AcidFluid.Source());
ACID = register(ModBlockIds.ACID, p -> new LiquidBlock(ModFluids.ACID_STILL, p), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER));  // no block item
```
- Fluid tags drive behaviour: add to `#minecraft:water`-like tags only if you want water physics. Fabric fluid behaviour API (26.2, non-experimental in 26.3): `EntityFluidInteractionRegistry.register(TagKey<Fluid>, FluidBehavior.simple()...build())` (swimming, boats, drowning, `onFluidEntered/Exited`). Flow interception: `FluidFlowEvents.ALLOW` (`boolean allowFlow(FluidState, LevelAccessor, BlockPos)`; the 26.3 blog calls it `FluidFlowCallback.EVENT`, the actual class is `net.fabricmc.fabric.api.block.v1.FluidFlowEvents`).
- Rendering: `FluidRenderingRegistry.register(...)` with a `FluidModel.Unbaked` (26.1, client doc). Fog: docsref/fog.

## 16. Gotchas checklist
- Forgot `setId` -> NPE at construction. Key mismatch between `setId` and `Registry.register` -> components bound under the wrong key.
- `new ItemStack(...)` in static init or during mod init -> `Components not bound yet`.
- Ported `AxeItem`/`HoeItem`/`ShovelItem`, `FuelRegistry`, `CompostingChanceRegistry`, `StrippableBlockRegistry`, `codec()` overrides, `TradeOfferHelper`, `FabricPotionBrewingBuilder` won't compile on 26.3.
- Client-only code (`isClientSide()` branches that touch `Minecraft`) must live in the client source set.
- Block items without `useBlockDescriptionPrefix()` get the wrong translation key.
- No loot table JSON -> block drops nothing. No `requiresCorrectToolForDrops` -> drops with bare hands.
- Blockstate JSON must cover every property combination or the block renders as missing model.
