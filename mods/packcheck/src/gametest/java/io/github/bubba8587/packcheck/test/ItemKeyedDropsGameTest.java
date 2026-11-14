// dte:A4,B15
package io.github.bubba8587.packcheck.test;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Item-keyed block drops, data only (docs/knowledge/item-keyed-block-drops.md, approaches A and B).
 *
 * <p>refpack overrides {@code minecraft:blocks/barrel}: a barrel placed from an item named "Marked Barrel" drops a
 * diamond, one placed from an item with {@code custom_data {refpack_marker:1b}} drops an emerald, anything else drops
 * the vanilla barrel (keeping its custom name). A {@code location_check} branch that would drop a gold ingot proves
 * that the world position is already air when the loot table runs. Blocks are placed and broken through
 * {@code ServerPlayerGameMode}, the same path a player's packets take.
 */
public class ItemKeyedDropsGameTest {
	private static final BlockPos FLOOR = new BlockPos(1, 1, 1);
	private static final BlockPos PLACED = FLOOR.above();

	private static ServerPlayer survivalPlayer(GameTestHelper helper) {
		helper.setBlock(FLOOR, Blocks.STONE);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		Vec3 stand = helper.absoluteVec(Vec3.atBottomCenterOf(PLACED.north(2)));
		player.snapTo(stand.x, stand.y, stand.z, 0.0F, 30.0F);
		return player;
	}

	/** Right-click the top of FLOOR with the stack, then mine what was placed with the tool, both as the player. */
	private static List<ItemStack> placeAndBreak(GameTestHelper helper, ServerPlayer player, ItemStack placed, ItemStack tool) {
		ServerLevel level = helper.getLevel();
		BlockPos floor = helper.absolutePos(FLOOR);
		Block expected = placed.is(Items.BARREL) ? Blocks.BARREL : Blocks.DEEPSLATE_DIAMOND_ORE;
		player.setItemInHand(InteractionHand.MAIN_HAND, placed); // survival: placing uses the stack up
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(floor).add(0.0, 0.5, 0.0), Direction.UP, floor, false));
		helper.assertBlockPresent(expected, PLACED);
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);
		helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(PLACED)), "destroyBlock refused");
		helper.assertBlockPresent(Blocks.AIR, PLACED);
		List<ItemStack> drops = level.getEntities(EntityTypes.ITEM, new AABB(helper.absolutePos(PLACED)).inflate(1.5), e -> true)
			.stream().map(ItemEntity::getItem).toList();
		level.getServer().getPlayerList().remove(player);
		return drops;
	}

	private static void assertSingleDrop(GameTestHelper helper, List<ItemStack> drops, ItemStack expected, String what) {
		helper.assertValueEqual(drops.size(), 1, what + ": number of drops " + drops);
		helper.assertTrue(ItemStack.isSameItemSameComponents(drops.getFirst(), expected) && drops.getFirst().getCount() == expected.getCount(),
			what + ": dropped " + drops.getFirst() + " " + drops.getFirst().getComponentsPatch() + ", expected " + expected + " " + expected.getComponentsPatch());
	}

	@GameTest
	public void namedBarrelUsesItsOwnDrop(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		ItemStack barrel = new ItemStack(Items.BARREL);
		barrel.set(DataComponents.CUSTOM_NAME, Component.literal("Marked Barrel"));
		ServerLevel level = helper.getLevel();
		BlockPos floor = helper.absolutePos(FLOOR);
		// Check the placed block entity really carries the name before breaking it.
		player.setItemInHand(InteractionHand.MAIN_HAND, barrel.copy());
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(floor).add(0.0, 0.5, 0.0), Direction.UP, floor, false));
		BlockEntity be = level.getBlockEntity(helper.absolutePos(PLACED));
		helper.assertTrue(be != null && Component.literal("Marked Barrel").equals(be.collectComponents().get(DataComponents.CUSTOM_NAME)),
			"placed barrel's components: " + (be == null ? null : be.collectComponents()));
		level.removeBlock(helper.absolutePos(PLACED), false);

		assertSingleDrop(helper, placeAndBreak(helper, player, barrel, ItemStack.EMPTY), new ItemStack(Items.DIAMOND), "Marked Barrel");
		helper.succeed();
	}

	@GameTest
	public void customDataBarrelUsesItsOwnDropAndLocationCheckSeesAir(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		ItemStack barrel = new ItemStack(Items.BARREL);
		CompoundTag marker = new CompoundTag();
		marker.putBoolean("refpack_marker", true);
		barrel.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
		ServerLevel level = helper.getLevel();
		BlockPos floor = helper.absolutePos(FLOOR);
		player.setItemInHand(InteractionHand.MAIN_HAND, barrel.copy());
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(floor).add(0.0, 0.5, 0.0), Direction.UP, floor, false));
		BlockEntity be = level.getBlockEntity(helper.absolutePos(PLACED));
		// custom_data is not consumed by the barrel, so it sits in the block entity's leftover "components" map.
		helper.assertTrue(be != null && be.components().has(DataComponents.CUSTOM_DATA),
			"placed barrel kept no custom_data: " + (be == null ? null : be.components()));
		level.removeBlock(helper.absolutePos(PLACED), false);

		// Emerald, not gold: the location_check branch (first in the table) never matches on a player break.
		assertSingleDrop(helper, placeAndBreak(helper, player, barrel, ItemStack.EMPTY), new ItemStack(Items.EMERALD), "custom_data barrel");
		helper.succeed();
	}

	@GameTest
	public void unmarkedBarrelsKeepVanillaDrops(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		assertSingleDrop(helper, placeAndBreak(helper, player, new ItemStack(Items.BARREL), ItemStack.EMPTY), new ItemStack(Items.BARREL), "plain barrel");

		ServerPlayer player2 = survivalPlayer(helper);
		ItemStack other = new ItemStack(Items.BARREL);
		other.set(DataComponents.CUSTOM_NAME, Component.literal("Some Other Name"));
		helper.getLevel().getEntities(EntityTypes.ITEM, new AABB(helper.absolutePos(PLACED)).inflate(3), e -> true).forEach(ItemEntity::discard);
		// Vanilla behaviour kept: the custom name is copied back onto the dropped barrel.
		assertSingleDrop(helper, placeAndBreak(helper, player2, other.copy(), ItemStack.EMPTY), other, "barrel with another name");
		helper.succeed();
	}

	/**
	 * Approach B (no block entity): bubbas_crafting_tweaks' "Synthetic Diamond Ore" (blast2diamond, an item_name)
	 * places plain deepslate diamond ore. The name has nowhere to live, so the placed block mines like natural ore.
	 */
	@GameTest
	public void syntheticOreFromBlastingPlacesPlainOre(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("bubbas_crafting_tweaks", "blast2diamond"));
		SingleRecipeInput input = new SingleRecipeInput(new ItemStack(Items.COAL_BLOCK));
		@SuppressWarnings("unchecked")
		Recipe<SingleRecipeInput> recipe = (Recipe<SingleRecipeInput>) level.recipeAccess().byKey(key).orElseThrow().value();
		ItemStack synthetic = recipe.assemble(input);
		helper.assertValueEqual(synthetic.getHoverName().getString(), "Synthetic Diamond Ore", "blast2diamond result name");

		ServerPlayer player = survivalPlayer(helper);
		List<ItemStack> drops = placeAndBreak(helper, player, synthetic, new ItemStack(Items.IRON_PICKAXE));
		helper.assertTrue(level.getBlockState(helper.absolutePos(PLACED)).isAir(), "ore still there");
		assertSingleDrop(helper, drops, new ItemStack(Items.DIAMOND), "Synthetic Diamond Ore (pack only)");

		// A block_state component naming a property the block doesn't have is skipped on placement
		// (BlockItemStateProperties#apply), so the approach-C item degrades to plain ore when the mod is missing.
		level.getEntities(EntityTypes.ITEM, new AABB(helper.absolutePos(PLACED)).inflate(3), e -> true).forEach(ItemEntity::discard);
		ItemStack withState = recipe.assemble(input); // the first stack was used up placing it
		withState.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(BooleanProperty.create("synthetic"), true));
		List<ItemStack> drops2 = placeAndBreak(helper, survivalPlayer(helper), withState, new ItemStack(Items.IRON_PICKAXE));
		assertSingleDrop(helper, drops2, new ItemStack(Items.DIAMOND), "ore item with block_state {synthetic:true}, no mod");
		helper.succeed();
	}
}
