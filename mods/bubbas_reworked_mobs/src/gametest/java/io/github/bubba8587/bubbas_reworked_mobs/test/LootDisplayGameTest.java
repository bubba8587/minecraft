// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.github.bubba8587.bubbas_reworked_mobs.display.DisplayPlacement;
import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplayBlock;
import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplayBlockEntity;
import io.github.bubba8587.bubbas_reworked_mobs.display.LootDisplays;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModEntities;
import io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperHearing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Loot on display (specs/undead.md "Loot on display"): storing, taking per placement, the shovel and its noise,
 * breaking, the command, and woken undead arming themselves from displays. Real paths: ServerPlayerGameMode for
 * using and mining, the command dispatcher, scheduled ticks for waking.
 */
public class LootDisplayGameTest {
	private static final BlockPos DISPLAY = new BlockPos(2, 1, 2);
	private static final float ALWAYS = 1000.0F;

	// --- helpers --------------------------------------------------------------------------------

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(x, 0, z, Blocks.DIRT);
			}
		}
	}

	private static LootDisplayBlockEntity display(GameTestHelper helper, BlockPos pos, DisplayPlacement placement, Direction facing, ItemStack stack) {
		return LootDisplays.place(helper.getLevel(), helper.absolutePos(pos), placement, facing, stack);
	}

	/** A real player in the level (connected, so messages and the player list work) in {@code mode}; remove it with {@link #remove}. */
	private static ServerPlayer player(GameTestHelper helper, GameType mode, BlockPos at, boolean sneaking) {
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-display-player"), false);
		ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(mode);
		Vec3 pos = helper.absoluteVec(Vec3.atBottomCenterOf(at));
		player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
		player.setOnGround(true);
		player.setShiftKeyDown(sneaking);
		return player;
	}

	private static void use(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
		BlockPos abs = helper.absolutePos(pos);
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
	}

	private static void remove(GameTestHelper helper, ServerPlayer player) {
		helper.getLevel().getServer().getPlayerList().remove(player);
	}

	/** A named, enchanted sword: anything lost in a round trip shows. */
	private static ItemStack fancySword(GameTestHelper helper) {
		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		sword.set(DataComponents.CUSTOM_NAME, Component.literal("Grave Edge"));
		sword.setDamageValue(17);
		sword.enchant(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
				.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS), 2);
		return sword;
	}

	// --- storing --------------------------------------------------------------------------------

	/** The item survives a block entity save/load, the minecraft:container component, and a structure template copy. */
	@GameTest
	public void displayKeepsItemThroughSaveLoadAndTemplates(GameTestHelper helper) {
		floor(helper);
		ItemStack sword = fancySword(helper);
		LootDisplayBlockEntity display = display(helper, DISPLAY, DisplayPlacement.STUCK, Direction.EAST, sword);
		helper.assertTrue(ItemStack.matches(display.getItem(), sword), "stored: " + display.getItem());

		ServerLevel level = helper.getLevel();
		CompoundTag saved = display.saveWithFullMetadata(level.registryAccess());
		BlockEntity loaded = BlockEntity.loadStatic(display.getBlockPos(), display.getBlockState(), saved, level.registryAccess());
		helper.assertTrue(loaded instanceof LootDisplayBlockEntity reloaded && ItemStack.matches(reloaded.getItem(), sword), "after save/load: " + saved);

		ItemContainerContents contents = display.collectComponents().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
		helper.assertTrue(contents.nonEmptyItemCopyStream().anyMatch(s -> ItemStack.matches(s, sword)), "minecraft:container component");

		StructureTemplate template = new StructureTemplate();
		template.fillFromWorld(level, helper.absolutePos(DISPLAY), new Vec3i(1, 1, 1), false, List.of());
		BlockPos copy = DISPLAY.east(3);
		template.placeInWorld(level, helper.absolutePos(copy), helper.absolutePos(copy), new StructurePlaceSettings(), level.getRandom(), 2);
		helper.assertBlockProperty(copy, LootDisplayBlock.PLACEMENT, DisplayPlacement.STUCK);
		helper.assertTrue(ItemStack.matches(helper.getBlockEntity(copy, LootDisplayBlockEntity.class).getItem(), sword), "structure template copy");
		helper.succeed();
	}

	// --- taking ---------------------------------------------------------------------------------

	/** Ground, wall and stuck: an empty hand takes the item and the block goes (water stays if it was waterlogged). */
	@GameTest
	public void emptyHandTakesGroundWallAndStuck(GameTestHelper helper) {
		floor(helper);
		helper.setBlock(DISPLAY.north(), Blocks.STONE); // the wall for the wall placement (facing south, away from it)
		ServerPlayer player = player(helper, GameType.SURVIVAL, DISPLAY.south(2), false);
		for (DisplayPlacement placement : List.of(DisplayPlacement.GROUND, DisplayPlacement.WALL, DisplayPlacement.STUCK)) {
			display(helper, DISPLAY, placement, Direction.SOUTH, new ItemStack(Items.IRON_SWORD));
			use(helper, player, DISPLAY);
			helper.assertBlockPresent(Blocks.AIR, DISPLAY);
			helper.assertTrue(player.getInventory().countItem(Items.IRON_SWORD) == 1, placement + ": sword not taken");
			player.getInventory().clearContent();
		}
		helper.assertItemEntityNotPresent(Items.IRON_SWORD, DISPLAY, 3.0); // taken, not dropped

		// Holding something else: nothing happens to the display.
		display(helper, DISPLAY, DisplayPlacement.GROUND, Direction.SOUTH, new ItemStack(Items.IRON_SWORD));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
		use(helper, player, DISPLAY);
		helper.assertBlockPresent(ModBlocks.LOOT_DISPLAY, DISPLAY);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

		// Underwater: placed waterlogged, and taking it leaves the water.
		BlockPos wet = DISPLAY.east(3);
		helper.setBlock(wet, Blocks.WATER);
		display(helper, wet, DisplayPlacement.GROUND, Direction.SOUTH, new ItemStack(Items.TRIDENT));
		helper.assertBlockProperty(wet, LootDisplayBlock.WATERLOGGED, true);
		use(helper, player, wet);
		helper.assertBlockPresent(Blocks.WATER, wet);
		helper.assertTrue(player.getInventory().countItem(Items.TRIDENT) == 1, "trident not taken");
		remove(helper, player);
		helper.succeed();
	}

	/** Buried loot ignores an empty hand; a shovel digs it up (costing the shovel 1 durability). */
	@GameTest
	public void buriedLootNeedsAShovel(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper, GameType.SURVIVAL, DISPLAY.south(2), false);
		display(helper, DISPLAY, DisplayPlacement.BURIED, Direction.SOUTH, new ItemStack(Items.IRON_SWORD));
		use(helper, player, DISPLAY);
		helper.assertBlockPresent(ModBlocks.LOOT_DISPLAY, DISPLAY);
		helper.assertTrue(player.getInventory().countItem(Items.IRON_SWORD) == 0, "took buried loot by hand");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SHOVEL));
		use(helper, player, DISPLAY);
		helper.assertBlockPresent(Blocks.AIR, DISPLAY);
		helper.assertTrue(player.getInventory().countItem(Items.IRON_SWORD) == 1, "shovel did not dig it up");
		helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "shovel durability used");
		remove(helper, player);
		helper.succeed();
	}

	/** What each way of taking sounds like to listeners: the quiet item_interact_finish vs block_destroy, louder to sleepers. */
	@GameTest
	public void takingIsQuietDiggingIsLoud(GameTestHelper helper) {
		floor(helper);
		BlockPos abs = helper.absolutePos(DISPLAY);
		List<Holder<GameEvent>> heard = new ArrayList<>();
		GameEventListener recorder = new GameEventListener() {
			private final PositionSource source = new BlockPositionSource(abs);

			@Override
			public PositionSource getListenerSource() {
				return source;
			}

			@Override
			public int getListenerRadius() {
				return 2;
			}

			@Override
			public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 from) {
				if (BlockPos.containing(from).equals(abs)) {
					heard.add(event);
				}
				return false;
			}
		};
		var registry = helper.getLevel().getChunkAt(abs).getListenerRegistry(SectionPos.blockToSectionCoord(abs.getY()));
		registry.register(recorder);
		try {
			ServerPlayer player = player(helper, GameType.SURVIVAL, DISPLAY.south(2), false);
			display(helper, DISPLAY, DisplayPlacement.GROUND, Direction.SOUTH, new ItemStack(Items.IRON_SWORD));
			use(helper, player, DISPLAY);
			helper.assertTrue(heard.contains(GameEvent.ITEM_INTERACT_FINISH) && !heard.contains(GameEvent.BLOCK_DESTROY), "taking: " + heard);
			heard.clear();
			display(helper, DISPLAY, DisplayPlacement.BURIED, Direction.SOUTH, new ItemStack(Items.IRON_SWORD));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SHOVEL));
			use(helper, player, DISPLAY);
			helper.assertTrue(heard.contains(GameEvent.BLOCK_DESTROY), "digging: " + heard);
			helper.assertTrue(SleeperHearing.baseChance(GameEvent.BLOCK_DESTROY) >= 2.0F * SleeperHearing.baseChance(GameEvent.ITEM_INTERACT_FINISH),
					"digging should be much louder to sleepers than taking");
			remove(helper, player);
		} finally {
			registry.unregister(recorder);
		}
		helper.succeed();
	}

	/**
	 * A sleeper that wakes at any noise it hears, three blocks away: sneaking up and taking ground loot doesn't wake it;
	 * digging up buried loot at the same spot does.
	 */
	@GameTest(environment = "bubbas_reworked_mobs-test:display_noise", maxTicks = 80)
	public void diggingWakesASleeperQuietTakingDoesNot(GameTestHelper helper) {
		floor(helper);
		BlockPos sleeperPos = DISPLAY.east(3);
		helper.setBlock(sleeperPos, ModBlocks.sleeper(UndeadVariant.ZOMBIE).defaultBlockState().setValue(SleeperBlock.POSE, SleeperPose.LYING));
		SleeperBlockEntity sleeper = helper.getBlockEntity(sleeperPos, SleeperBlockEntity.class);
		sleeper.setSensitivity(ALWAYS);
		ServerPlayer sneaker = player(helper, GameType.SURVIVAL, DISPLAY.west(2), true);
		display(helper, DISPLAY, DisplayPlacement.GROUND, Direction.WEST, new ItemStack(Items.IRON_SWORD));
		use(helper, sneaker, DISPLAY);
		helper.assertBlockPresent(Blocks.AIR, DISPLAY);
		helper.assertTrue(sneaker.getInventory().countItem(Items.IRON_SWORD) == 1, "sword not taken");
		helper.assertFalse(sleeper.isWakePending(), "quiet taking woke the sleeper");
		remove(helper, sneaker);

		helper.runAfterDelay(25, () -> {
			helper.assertFalse(sleeper.isWakePending(), "quiet taking woke the sleeper (late)");
			ServerPlayer digger = player(helper, GameType.SURVIVAL, DISPLAY.west(2), false);
			display(helper, DISPLAY, DisplayPlacement.BURIED, Direction.WEST, new ItemStack(Items.IRON_SWORD));
			digger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SHOVEL));
			use(helper, digger, DISPLAY);
			helper.assertBlockPresent(Blocks.AIR, DISPLAY);
			helper.assertTrue(sleeper.isWakePending(), "digging did not wake the sleeper");
			remove(helper, digger);
			helper.succeedWhen(() -> helper.assertEntityPresent(ModEntities.woken(UndeadVariant.ZOMBIE), sleeperPos, 1.5));
		});
	}

	// --- breaking -------------------------------------------------------------------------------

	/** Mining any placement drops the item; so does removing what holds it up (the floor or the wall). */
	@GameTest
	public void breakingDropsTheItem(GameTestHelper helper) {
		floor(helper);
		helper.setBlock(DISPLAY.north(), Blocks.STONE);
		for (DisplayPlacement placement : DisplayPlacement.values()) {
			display(helper, DISPLAY, placement, Direction.SOUTH, new ItemStack(Items.IRON_SWORD));
			ServerPlayer player = player(helper, GameType.SURVIVAL, DISPLAY.south(2), false);
			player.gameMode.destroyBlock(helper.absolutePos(DISPLAY));
			helper.assertBlockPresent(Blocks.AIR, DISPLAY);
			helper.assertItemEntityPresent(Items.IRON_SWORD, DISPLAY, 1.5);
			helper.killAllEntitiesOfClass(ItemEntity.class);
			remove(helper, player);
		}
		BlockPos onDirt = DISPLAY.east(3);
		display(helper, onDirt, DisplayPlacement.GROUND, Direction.SOUTH, new ItemStack(Items.BOW));
		helper.setBlock(onDirt.below(), Blocks.AIR);
		helper.assertBlockPresent(Blocks.AIR, onDirt);
		helper.assertItemEntityPresent(Items.BOW, onDirt, 2.0);
		display(helper, DISPLAY, DisplayPlacement.WALL, Direction.SOUTH, new ItemStack(Items.GOLDEN_SWORD));
		helper.setBlock(DISPLAY.north(), Blocks.AIR);
		helper.assertBlockPresent(Blocks.AIR, DISPLAY);
		helper.assertItemEntityPresent(Items.GOLDEN_SWORD, DISPLAY, 2.0);
		helper.succeed();
	}

	// --- the command ----------------------------------------------------------------------------

	@GameTest
	public void displayCommandPlacesAtPositionAndWhereYouLook(GameTestHelper helper) throws CommandSyntaxException {
		floor(helper);
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(DISPLAY);
		level.getServer().getCommands().getDispatcher().execute(
				"bubbas_reworked_mobs display buried minecraft:diamond_sword %d %d %d east".formatted(at.getX(), at.getY(), at.getZ()),
				level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput());
		helper.assertBlockProperty(DISPLAY, LootDisplayBlock.PLACEMENT, DisplayPlacement.BURIED);
		helper.assertBlockProperty(DISPLAY, LootDisplayBlock.FACING, Direction.EAST);
		helper.assertTrue(helper.getBlockEntity(DISPLAY, LootDisplayBlockEntity.class).getItem().is(Items.DIAMOND_SWORD), "command item");

		// Looking down at the floor two blocks north of the player: a stuck sword there, turned toward the player.
		ServerPlayer player = player(helper, GameType.CREATIVE, new BlockPos(5, 1, 6), false);
		Vec3 eye = player.getEyePosition();
		Vec3 target = helper.absoluteVec(new Vec3(5.5, 1.0, 4.5));
		Vec3 look = target.subtract(eye);
		float yaw = (float) (Math.toDegrees(Math.atan2(look.z, look.x)) - 90.0);
		float pitch = (float) -Math.toDegrees(Math.atan2(look.y, look.horizontalDistance()));
		player.snapTo(player.getX(), player.getY(), player.getZ(), yaw, pitch);
		player.setYHeadRot(yaw); // a living entity looks where its head points
		level.getServer().getCommands().getDispatcher().execute("bubbas_reworked_mobs display stuck minecraft:iron_sword",
				level.getServer().createCommandSourceStack().withLevel(level).withEntity(player).withPosition(player.position())
						.withRotation(new Vec2(pitch, yaw)).withSuppressedOutput());
		BlockPos looked = new BlockPos(5, 1, 4);
		helper.assertBlockProperty(looked, LootDisplayBlock.PLACEMENT, DisplayPlacement.STUCK);
		helper.assertBlockProperty(looked, LootDisplayBlock.FACING, Direction.SOUTH);
		remove(helper, player);
		helper.succeed();
	}

	// --- woken undead ---------------------------------------------------------------------------

	/** An empty-handed zombie wakes and takes the displayed sword (block gone), preferring it over a nearer loose axe. */
	@GameTest(maxTicks = 40)
	public void wokenZombieTakesDisplayedWeaponOverLooseOne(GameTestHelper helper) {
		floor(helper);
		BlockPos sleeperPos = new BlockPos(4, 1, 4);
		helper.setBlock(sleeperPos, ModBlocks.sleeper(UndeadVariant.ZOMBIE).defaultBlockState());
		SleeperBlockEntity sleeper = helper.getBlockEntity(sleeperPos, SleeperBlockEntity.class);
		sleeper.setSensitivity(0.0F);
		BlockPos near = sleeperPos.west(2);
		display(helper, near, DisplayPlacement.BURIED, Direction.EAST, new ItemStack(Items.IRON_SWORD));
		BlockPos far = sleeperPos.north(4);
		display(helper, far, DisplayPlacement.GROUND, Direction.EAST, new ItemStack(Items.DIAMOND_SWORD)); // out of reach
		ItemEntity axe = helper.spawnItem(Items.IRON_AXE, Vec3.atBottomCenterOf(sleeperPos.east()));
		sleeper.scheduleWake(helper.getLevel(), null, null, 1);
		helper.succeedWhen(() -> {
			Monster mob = (Monster) helper.findOneEntity(ModEntities.woken(UndeadVariant.ZOMBIE));
			helper.assertTrue(mob.getMainHandItem().is(Items.IRON_SWORD), "zombie holds " + mob.getMainHandItem());
			helper.assertBlockPresent(Blocks.AIR, near);
			helper.assertBlockPresent(ModBlocks.LOOT_DISPLAY, far);
			helper.assertTrue(axe.isAlive(), "the loose axe should stay (display preferred)");
			helper.assertItemEntityNotPresent(Items.IRON_SWORD); // taken by the mob, not dropped
		});
	}

	/** Skeletons arm from displays too, and take a bow (one of #preferred_on_wake). */
	@GameTest(maxTicks = 40)
	public void wokenSkeletonTakesDisplayedBow(GameTestHelper helper) {
		floor(helper);
		BlockPos sleeperPos = new BlockPos(4, 1, 4);
		helper.setBlock(sleeperPos, ModBlocks.sleeper(UndeadVariant.SKELETON).defaultBlockState());
		SleeperBlockEntity sleeper = helper.getBlockEntity(sleeperPos, SleeperBlockEntity.class);
		sleeper.setSensitivity(0.0F);
		helper.setBlock(sleeperPos.north(2), Blocks.STONE);
		BlockPos wall = sleeperPos.north();
		display(helper, wall, DisplayPlacement.WALL, Direction.SOUTH, new ItemStack(Items.BOW));
		sleeper.scheduleWake(helper.getLevel(), null, null, 1);
		helper.succeedWhen(() -> {
			Monster mob = (Monster) helper.findOneEntity(ModEntities.woken(UndeadVariant.SKELETON));
			helper.assertTrue(mob.getMainHandItem().is(Items.BOW), "skeleton holds " + mob.getMainHandItem());
			helper.assertBlockPresent(Blocks.AIR, wall);
		});
	}
}
