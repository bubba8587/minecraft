// dte:A2,A4
package io.github.bubba8587.refmod.test;

import java.util.List;
import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.bubba8587.refmod.blockdata.BarrelTallies;
import io.github.bubba8587.refmod.block.CounterBlockEntity;
import io.github.bubba8587.refmod.block.SignalLampBlock;
import io.github.bubba8587.refmod.component.TallyCount;
import io.github.bubba8587.refmod.entity.PebbleEntity;
import io.github.bubba8587.refmod.event.LampBreakEvents;
import io.github.bubba8587.refmod.pseudoblock.CharmPseudoBlock;
import io.github.bubba8587.refmod.registry.ModAttachments;
import io.github.bubba8587.refmod.registry.ModBlocks;
import io.github.bubba8587.refmod.registry.ModComponents;
import io.github.bubba8587.refmod.registry.ModCreativeTabs;
import io.github.bubba8587.refmod.registry.ModEntities;
import io.github.bubba8587.refmod.registry.ModItemComponents;
import io.github.bubba8587.refmod.registry.ModItems;
import io.github.bubba8587.refmod.registry.ModTags;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Brightness;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** One server game test per refmod behaviour (spec reference-projects.md, refmod requirements 16 and 17). */
public class RefModGameTest {
	private static final BlockPos POS = new BlockPos(2, 1, 2);

	// --- 2. Item + data component -------------------------------------------------------------

	// Core test: registered by RefModCoreGameTest (test-lite, specs/quality.md).
	public void tallyStickCountsUses(GameTestHelper helper) {
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		ItemStack stack = new ItemStack(ModItems.TALLY_STICK);
		helper.assertValueEqual(stack.get(ModComponents.TALLY_COUNT), TallyCount.ZERO, "default tally");
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		ModItems.TALLY_STICK.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		ModItems.TALLY_STICK.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(player.getMainHandItem().get(ModComponents.TALLY_COUNT), new TallyCount(2), "tally after two uses");

		List<Component> lines = player.getMainHandItem().getTooltipLines(Item.TooltipContext.of(helper.getLevel()), player, TooltipFlag.NORMAL);
		boolean found = lines.stream().anyMatch(line -> line.getContents() instanceof TranslatableContents tc
				&& tc.getKey().equals("item.refmod.tally_stick.uses") && tc.getArgs().length == 1 && Integer.valueOf(2).equals(tc.getArgs()[0]));
		helper.assertTrue(found, "tooltip lacks the tally line: " + lines);
		helper.succeed();
	}

	// --- 3. Tool via properties (block transformer) -------------------------------------------

	@GameTest
	public void flintHatchetStripsLog(GameTestHelper helper) {
		helper.setBlock(POS, Blocks.OAK_LOG);
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FLINT_HATCHET));
		helper.useBlock(POS, player);
		helper.assertBlockPresent(Blocks.STRIPPED_OAK_LOG, POS);
		helper.assertTrue(new ItemStack(ModItems.FLINT_HATCHET).has(DataComponents.BLOCK_TRANSFORMER), "hatchet has no block_transformer component");
		helper.succeed();
	}

	// --- 4. Item component on an existing item (fuel) -----------------------------------------

	@GameTest(maxTicks = 40)
	public void stickBurnsFiftyTicks(GameTestHelper helper) {
		CookingFuel fuel = new ItemStack(Items.STICK).get(DataComponents.COOKING_FUEL);
		helper.assertTrue(fuel != null, "stick has no cooking_fuel component");
		helper.assertValueEqual(fuel.burnTime(), new ResolvableInt.Constant(ModItemComponents.STICK_BURN_TICKS), "stick burn time");

		helper.setBlock(POS, Blocks.FURNACE);
		FurnaceBlockEntity furnace = helper.getBlockEntity(POS, FurnaceBlockEntity.class);
		furnace.setItem(0, new ItemStack(Items.COBBLESTONE));
		furnace.setItem(1, new ItemStack(Items.STICK));
		helper.runAfterDelay(2, () -> {
			CompoundTag tag = furnace.saveWithoutMetadata(helper.getLevel().registryAccess());
			helper.assertValueEqual(tag.getIntOr("lit_total_time", -1), ModItemComponents.STICK_BURN_TICKS, "furnace lit_total_time");
			helper.succeed();
		});
	}

	// --- 5. Block + block state ---------------------------------------------------------------

	@GameTest
	public void signalLampToggles(GameTestHelper helper) {
		helper.setBlock(POS, ModBlocks.SIGNAL_LAMP);
		helper.assertBlockProperty(POS, SignalLampBlock.LIT, false);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		helper.useBlock(POS, player);
		helper.assertBlockProperty(POS, SignalLampBlock.LIT, true);
		helper.useBlock(POS, player);
		helper.assertBlockProperty(POS, SignalLampBlock.LIT, false);
		helper.getLevel().getServer().getPlayerList().remove(player);
		helper.succeed();
	}

	// --- 6. Block entity ----------------------------------------------------------------------

	// Core test: registered by RefModCoreGameTest (test-lite, specs/quality.md).
	public void counterBlockCountsSavesAndSyncs(GameTestHelper helper) {
		helper.setBlock(POS, ModBlocks.COUNTER_BLOCK);
		helper.runAfterDelay(30, () -> {
			CounterBlockEntity be = helper.getBlockEntity(POS, CounterBlockEntity.class);
			long ticks = be.getTicks();
			helper.assertValueInBetween(25L, ticks, 31L, "counted ticks");

			ServerLevel level = helper.getLevel();
			CompoundTag saved = be.saveWithFullMetadata(level.registryAccess());
			helper.assertValueEqual(saved.getLongOr(CounterBlockEntity.TICKS_KEY, -1L), ticks, "saved ticks");
			BlockEntity loaded = BlockEntity.loadStatic(be.getBlockPos(), be.getBlockState(), saved, level.registryAccess());
			helper.assertTrue(loaded instanceof CounterBlockEntity, "loadStatic did not make a CounterBlockEntity");
			helper.assertValueEqual(((CounterBlockEntity) loaded).getTicks(), ticks, "loaded ticks");

			helper.assertTrue(be.getUpdatePacket() != null, "no client sync packet");
			helper.assertValueEqual(be.getUpdateTag(level.registryAccess()).getLongOr(CounterBlockEntity.TICKS_KEY, -1L), ticks, "update tag ticks");
			helper.succeed();
		});
	}

	// --- 7. Creative tab ----------------------------------------------------------------------

	@GameTest
	public void creativeTabListsEveryRefmodItem(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(ModCreativeTabs.REFMOD_KEY);
		tab.buildContents(new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true, level.registryAccess()));
		List<Item> shown = tab.getDisplayItems().stream().map(ItemStack::getItem).toList();
		List<Item> expected = BuiltInRegistries.ITEM.stream()
				.filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("refmod")).toList();
		helper.assertTrue(!expected.isEmpty() && shown.containsAll(expected), "tab shows " + shown + " but refmod has " + expected);
		helper.succeed();
	}

	// --- 8. Entity: thrown pebble deals 1 damage ----------------------------------------------

	@GameTest(maxTicks = 60)
	public void pebbleDealsOneDamage(GameTestHelper helper) {
		helper.setBlock(POS.below(), Blocks.STONE);
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, POS);
		float start = pig.getHealth();
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		Vec3 above = helper.absoluteVec(Vec3.atBottomCenterOf(POS.above(3)));
		player.snapTo(above.x, above.y, above.z, 0.0F, 90.0F); // looking straight down at the pig
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PEBBLE, 2));
		ModItems.PEBBLE.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "pebbles left after throwing");
		helper.assertEntityPresent(ModEntities.PEBBLE);
		helper.succeedWhen(() -> {
			helper.assertValueEqual(pig.getHealth(), start - PebbleEntity.DAMAGE, "pig health");
			helper.assertEntityNotPresent(ModEntities.PEBBLE);
		});
	}

	// --- 10. Data attachment: persistent, survives death --------------------------------------

	@GameTest
	public void lampTogglesAttachmentPersistsAndSurvivesDeath(GameTestHelper helper) {
		helper.setBlock(POS, ModBlocks.SIGNAL_LAMP);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		helper.assertValueEqual(ModAttachments.getLampToggles(player), 0, "initial toggles");
		helper.useBlock(POS, player);
		helper.useBlock(POS, player);
		helper.useBlock(POS, player);
		helper.assertValueEqual(ModAttachments.getLampToggles(player), 3, "toggles after three uses");

		// Saved with the player and read back.
		ServerLevel level = helper.getLevel();
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		player.saveWithoutId(out);
		CompoundTag tag = out.buildResult();
		helper.assertTrue(tag.getCompoundOrEmpty("fabric:attachments").contains("refmod:lamp_toggles"), "attachment not saved: " + tag.getCompoundOrEmpty("fabric:attachments"));
		ServerPlayer reloaded = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
		helper.assertValueEqual(ModAttachments.getLampToggles(reloaded), 3, "toggles after reload");

		// Respawn after death copies it (copyOnDeath; Fabric transfers it in ServerPlayerEvents.AFTER_RESPAWN).
		ServerPlayer respawned = level.getServer().getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
		helper.assertTrue(respawned != player, "respawn did not create a new player");
		helper.assertValueEqual(ModAttachments.getLampToggles(respawned), 3, "toggles after death");
		level.getServer().getPlayerList().remove(respawned);
		helper.succeed();
	}

	// --- 11. Command ----------------------------------------------------------------------------

	@GameTest
	public void statsCommandPrintsAndGatesOtherPlayers(GameTestHelper helper) throws CommandSyntaxException {
		// TODO 26.4: rewrite on Commands.parseBox()/performCommand (getDispatcher() is gone)
		helper.fail("statsCommandPrintsAndGatesOtherPlayers: not yet ported to the 26.4 command API");
	}

	// --- 12. Event: lamp breaks are counted ----------------------------------------------------

	@GameTest
	public void lampBreakEventCounts(GameTestHelper helper) {
		helper.setBlock(POS, ModBlocks.SIGNAL_LAMP);
		helper.setBlock(POS.east(), Blocks.STONE);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		int before = LampBreakEvents.lampsBroken();
		player.gameMode.destroyBlock(helper.absolutePos(POS.east()));
		helper.assertValueEqual(LampBreakEvents.lampsBroken(), before, "count after breaking stone");
		player.gameMode.destroyBlock(helper.absolutePos(POS));
		helper.assertBlockPresent(Blocks.AIR, POS);
		helper.assertValueEqual(LampBreakEvents.lampsBroken(), before + 1, "count after breaking a lamp");
		helper.getLevel().getServer().getPlayerList().remove(player);
		helper.succeed();
	}

	// --- 13. Mixin: snowballs hurt ------------------------------------------------------------

	@GameTest(maxTicks = 60)
	public void snowballsHurtWithMixin(GameTestHelper helper) {
		helper.setBlock(POS.below(), Blocks.STONE);
		Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, POS);
		float start = pig.getHealth();
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		Vec3 above = helper.absoluteVec(Vec3.atBottomCenterOf(POS.above(3)));
		player.snapTo(above.x, above.y, above.z, 0.0F, 90.0F); // looking straight down at the pig
		Projectile.spawnProjectileFromRotation(Snowball::new, helper.getLevel(), new ItemStack(Items.SNOWBALL), player, 0.0F, 1.5F, 0.0F);
		helper.succeedWhen(() -> helper.assertValueEqual(pig.getHealth(), start - 2.0F, "pig health after snowball"));
	}

	// --- 14. Datagen outputs: recipe, loot table, item tag ------------------------------------

	// Core test: registered by RefModCoreGameTest (test-lite, specs/quality.md).
	public void datagenRecipeLootAndTagLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CraftingInput input = CraftingInput.of(1, 1, List.of(new ItemStack(Items.COBBLESTONE)));
		Optional<ItemStack> result = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level)
				.map(holder -> holder.value().assemble(input));
		helper.assertTrue(result.isPresent(), "no crafting recipe for cobblestone");
		helper.assertTrue(result.get().is(ModItems.PEBBLE) && result.get().getCount() == 4, "cobblestone crafts " + result.get());

		helper.assertTrue(new ItemStack(ModItems.PEBBLE).is(ModTags.THROWABLES), "pebble not in #refmod:throwables");

		helper.setBlock(POS, ModBlocks.SIGNAL_LAMP);
		level.destroyBlock(helper.absolutePos(POS), true);
		helper.assertItemEntityPresent(ModBlocks.SIGNAL_LAMP.asItem(), POS, 2.0);
		helper.succeed();
	}

	// --- 17a. No new blocks: display-entity pseudo-block ----------------------------------------

	/** Right-click a block with the charm (through ServerPlayerGameMode, like the real packet) and return the hitbox. */
	private static Interaction placeCharm(GameTestHelper helper, ServerPlayer player) {
		BlockPos abs = helper.absolutePos(POS);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0.0, 0.5, 0.0), Direction.UP, abs, false);
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
		List<Interaction> hitboxes = helper.getLevel().getEntitiesOfClass(Interaction.class, new AABB(abs.above()),
				e -> e.hasAttached(ModAttachments.CHARM_DISPLAY));
		helper.assertValueEqual(hitboxes.size(), 1, "charm hitboxes above the block");
		return hitboxes.getFirst();
	}

	@GameTest
	public void lanternCharmPlacesDisplayAndHitboxAndPicksUp(GameTestHelper helper) {
		helper.setBlock(POS, Blocks.STONE);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LANTERN_CHARM, 2));
		Interaction hitbox = placeCharm(helper, player);

		// The look: an item_display showing the charm with the item_model component pointing at the placed model.
		Entity linked = helper.getLevel().getEntity(hitbox.getAttached(ModAttachments.CHARM_DISPLAY));
		helper.assertTrue(linked instanceof Display.ItemDisplay, "hitbox does not point at an item_display: " + linked);
		Display.ItemDisplay display = (Display.ItemDisplay) linked;
		helper.assertValueEqual(display.getItemStack().get(DataComponents.ITEM_MODEL), CharmPseudoBlock.PLACED_MODEL, "display item_model");
		helper.assertValueEqual(display.getItemTransform(), ItemDisplayContext.FIXED, "display item transform");
		helper.assertValueEqual(display.getBrightnessOverride(), new Brightness(15, 15), "display brightness");
		helper.assertTrue(display.affectedByCulling(), "display has no culling box (width/height unset)");
		helper.assertValueEqual(hitbox.getWidth(), CharmPseudoBlock.SIZE, "hitbox width");
		helper.assertValueEqual(hitbox.blockPosition(), helper.absolutePos(POS.above()), "hitbox position");

		// Right-click the hitbox through the real server packet handler, where Fabric's UseEntityCallback fires.
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		Vec3 stand = helper.absoluteVec(Vec3.atBottomCenterOf(POS.above().north()));
		player.snapTo(stand.x, stand.y, stand.z, 0.0F, 30.0F);
		player.connection.handleInteract(new ServerboundInteractPacket(hitbox.getId(), InteractionHand.MAIN_HAND, Vec3.ZERO, false));
		helper.assertTrue(hitbox.isRemoved() && display.isRemoved(), "charm entities not removed after right-click");
		helper.assertItemEntityPresent(ModItems.LANTERN_CHARM, POS.above(), 1.0);
		helper.getLevel().getServer().getPlayerList().remove(player);
		helper.succeed();
	}

	@GameTest
	public void lanternCharmConsumesItemAndNeedsFreeSpace(GameTestHelper helper) {
		helper.setBlock(POS, Blocks.STONE);
		helper.setBlock(POS.above(), Blocks.STONE);
		ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LANTERN_CHARM, 2));
		BlockPos abs = helper.absolutePos(POS);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
		helper.assertEntityNotPresent(EntityTypes.INTERACTION);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 2, "charms after a blocked placement");

		helper.setBlock(POS.above(), Blocks.AIR);
		placeCharm(helper, player);
		helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "charms after placing one in survival");
		helper.succeed();
	}

	@GameTest
	public void lanternCharmEntitiesSaveAndLoad(GameTestHelper helper) {
		helper.setBlock(POS, Blocks.STONE);
		ServerLevel level = helper.getLevel();
		Interaction hitbox = CharmPseudoBlock.place(level, helper.absolutePos(POS), 0.0F);
		helper.assertTrue(hitbox != null, "charm not placed");
		Display.ItemDisplay display = (Display.ItemDisplay) level.getEntity(hitbox.getAttached(ModAttachments.CHARM_DISPLAY));

		// Both are ordinary entities, saved in the chunk's entity storage like any other.
		TagValueOutput hitboxOut = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		hitbox.saveWithoutId(hitboxOut);
		Interaction hitboxLoaded = new Interaction(EntityTypes.INTERACTION, level);
		hitboxLoaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), hitboxOut.buildResult()));
		helper.assertValueEqual(hitboxLoaded.getAttached(ModAttachments.CHARM_DISPLAY), display.getUUID(), "loaded hitbox link");
		helper.assertValueEqual(hitboxLoaded.getHeight(), CharmPseudoBlock.SIZE, "loaded hitbox height");

		TagValueOutput displayOut = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		display.saveWithoutId(displayOut);
		Display.ItemDisplay displayLoaded = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, level);
		displayLoaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), displayOut.buildResult()));
		helper.assertValueEqual(displayLoaded.getItemStack().get(DataComponents.ITEM_MODEL), CharmPseudoBlock.PLACED_MODEL, "loaded item_model");
		helper.assertValueEqual(displayLoaded.getUUID(), display.getUUID(), "loaded display UUID");
		hitbox.discard();
		display.discard();
		helper.succeed();
	}

	// --- 17b. No new blocks: per-position data on a vanilla barrel (chunk attachment) ----------

	private static void useTallyOnBarrel(GameTestHelper helper, ServerPlayer player) {
		BlockPos abs = helper.absolutePos(POS);
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(abs), Direction.NORTH, abs, false));
	}

	@GameTest
	public void barrelTallyLivesInChunkAttachmentAndSurvivesChunkSaveLoad(GameTestHelper helper) {
		helper.setBlock(POS, Blocks.BARREL);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TALLY_STICK));
		useTallyOnBarrel(helper, player);
		useTallyOnBarrel(helper, player);

		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(POS);
		helper.assertValueEqual(BarrelTallies.get(level, abs), 2, "barrel tally after two clicks");
		helper.assertTrue(player.containerMenu == player.inventoryMenu, "the barrel menu opened");
		helper.assertValueEqual(player.getMainHandItem().get(ModComponents.TALLY_COUNT), TallyCount.ZERO, "tally stick's own count");
		helper.assertBlockPresent(Blocks.BARREL, POS);

		// Save the chunk the way ChunkMap does, check the NBT, then read it back into a fresh chunk object.
		LevelChunk chunk = level.getChunkAt(abs);
		CompoundTag tag = SerializableChunkData.copyOf(level, chunk).write();
		helper.assertTrue(tag.getCompoundOrEmpty("fabric:attachments").contains("refmod:barrel_tallies"),
				"chunk NBT lacks the attachment: " + tag.getCompoundOrEmpty("fabric:attachments"));
		// Drop the light data so reading the copy does not queue light updates for the live chunk.
		tag.getListOrEmpty("sections").forEach(section -> {
			if (section instanceof CompoundTag sectionTag) {
				sectionTag.remove("BlockLight");
				sectionTag.remove("SkyLight");
			}
		});
		ChunkAccess reloaded = SerializableChunkData.parse(level, level.palettedContainerFactory(), tag)
				.read(level, level.getPoiManager(), level.getChunkSource().chunkMap.storageInfo(), chunk.getPos());
		helper.assertTrue(reloaded != chunk, "read returned the live chunk");
		BarrelTallies loaded = reloaded.getAttached(ModAttachments.BARREL_TALLIES);
		helper.assertTrue(loaded != null && loaded.get(abs) == 2, "tally after chunk reload: " + loaded);
		level.getServer().getPlayerList().remove(player);
		helper.succeed();
	}

	@GameTest
	public void barrelTallyClearedWhenBarrelRemoved(GameTestHelper helper) {
		helper.setBlock(POS, Blocks.BARREL);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TALLY_STICK));
		useTallyOnBarrel(helper, player);
		ServerLevel level = helper.getLevel();
		BlockPos abs = helper.absolutePos(POS);
		helper.assertValueEqual(BarrelTallies.get(level, abs), 1, "tally before removal");

		// Not a player break: PlayerBlockBreakEvents would miss this; the BarrelBlock mixin catches it.
		level.destroyBlock(abs, false);
		BarrelTallies left = level.getChunkAt(abs).getAttached(ModAttachments.BARREL_TALLIES);
		helper.assertTrue(left == null || left.get(abs) == 0, "stale tally left in the chunk: " + left);

		helper.setBlock(POS, Blocks.BARREL);
		helper.assertValueEqual(BarrelTallies.get(level, abs), 0, "tally of a new barrel at the same spot");
		level.getServer().getPlayerList().remove(player);
		helper.succeed();
	}
}
