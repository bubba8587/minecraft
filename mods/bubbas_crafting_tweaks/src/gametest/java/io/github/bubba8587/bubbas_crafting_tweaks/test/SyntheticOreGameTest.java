// dte:C3,B15
package io.github.bubba8587.bubbas_crafting_tweaks.test;

import java.util.List;

import io.github.bubba8587.bubbas_crafting_tweaks.BubbasCraftingTweaks;
import io.github.bubba8587.bubbas_crafting_tweaks.blockstate.SyntheticOre;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Synthetic Diamond Ore (specs/crafting-tweaks-mod.md; docs/knowledge/item-keyed-block-drops.md approach C, ported from
 * refmod): vanilla deepslate diamond ore gets a {@code synthetic} property, the datapack's blast2diamond result
 * ({@code block_state {synthetic:"true"}}) places it, and the mod's loot override sends synthetic ore to
 * {@code bubbas_crafting_tweaks:blocks/synthetic_diamond_ore}: 25 % chance of one diamond, no Fortune bonus, no XP;
 * silk touch gives the synthetic item back. Placing and mining go through {@code ServerPlayerGameMode}.
 */
public class SyntheticOreGameTest {
	private static final BlockPos FLOOR = new BlockPos(1, 1, 1);
	private static final BlockPos PLACED = FLOOR.above();

	private record Mined(BlockState placed, List<ItemStack> drops, int xpOrbs) {
	}

	private static ItemStack syntheticFromRecipe(GameTestHelper helper) {
		ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, BubbasCraftingTweaks.id("blast2diamond"));
		@SuppressWarnings("unchecked")
		Recipe<SingleRecipeInput> recipe = (Recipe<SingleRecipeInput>) helper.getLevel().recipeAccess().byKey(key).orElseThrow().value();
		return recipe.assemble(new SingleRecipeInput(new ItemStack(Items.COAL_BLOCK)));
	}

	private static ItemStack pickaxe(GameTestHelper helper, ResourceKey<Enchantment> enchantment, int level) {
		ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
		if (enchantment != null) {
			tool.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment), level);
		}
		return tool;
	}

	/** A survival player right-clicks the top of FLOOR with the stack, then mines the placed block with the tool. */
	private static Mined placeAndMine(GameTestHelper helper, ItemStack placed, ItemStack tool) {
		ServerLevel level = helper.getLevel();
		helper.setBlock(FLOOR, Blocks.STONE);
		helper.setBlock(PLACED, Blocks.AIR);
		AABB around = new AABB(helper.absolutePos(PLACED)).inflate(2.0);
		level.getEntities(EntityTypes.ITEM, around, e -> true).forEach(ItemEntity::discard);
		level.getEntities(EntityTypes.EXPERIENCE_ORB, around, e -> true).forEach(e -> e.discard());

		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		Vec3 stand = helper.absoluteVec(Vec3.atBottomCenterOf(PLACED.north(2)));
		player.snapTo(stand.x, stand.y, stand.z, 0.0F, 30.0F);
		BlockPos floor = helper.absolutePos(FLOOR);
		player.setItemInHand(InteractionHand.MAIN_HAND, placed);
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(floor).add(0.0, 0.5, 0.0), Direction.UP, floor, false));
		BlockState state = level.getBlockState(helper.absolutePos(PLACED));
		helper.assertTrue(state.is(Blocks.DEEPSLATE_DIAMOND_ORE), "placed " + state);
		player.setItemInHand(InteractionHand.MAIN_HAND, tool);
		helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(PLACED)), "destroyBlock refused");
		helper.assertBlockPresent(Blocks.AIR, PLACED);
		List<ItemStack> drops = level.getEntities(EntityTypes.ITEM, around, e -> true).stream().map(ItemEntity::getItem).toList();
		int orbs = level.getEntities(EntityTypes.EXPERIENCE_ORB, around, e -> true).size();
		level.getServer().getPlayerList().remove(player);
		return new Mined(state, drops, orbs);
	}

	private static int count(List<ItemStack> drops, ItemStack like) {
		return drops.stream().filter(s -> ItemStack.isSameItemSameComponents(s, like)).mapToInt(ItemStack::getCount).sum();
	}

	@GameTest
	public void syntheticPropertyIsAddedAndNaturalIsTheDefault(GameTestHelper helper) {
		BlockState def = Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState();
		helper.assertTrue(def.hasProperty(SyntheticOre.SYNTHETIC), "deepslate_diamond_ore has no synthetic property");
		helper.assertValueEqual(def.getValue(SyntheticOre.SYNTHETIC), false, "default synthetic");
		helper.assertValueEqual(Blocks.DEEPSLATE_DIAMOND_ORE.getStateDefinition().getPossibleStates().size(), 2, "state count");
		helper.assertTrue(!Blocks.DIAMOND_ORE.defaultBlockState().hasProperty(SyntheticOre.SYNTHETIC), "plain diamond_ore got the property too");
		helper.assertTrue(Blocks.DEEPSLATE_DIAMOND_ORE.asItem() == Items.DEEPSLATE_DIAMOND_ORE, "the block item changed");
		helper.succeed();
	}

	@GameTest
	public void naturalOreKeepsVanillaDropsAndXp(GameTestHelper helper) {
		Mined plain = placeAndMine(helper, new ItemStack(Items.DEEPSLATE_DIAMOND_ORE), pickaxe(helper, null, 0));
		helper.assertValueEqual(plain.placed().getValue(SyntheticOre.SYNTHETIC), false, "plain item placed synthetic");
		helper.assertValueEqual(count(plain.drops(), new ItemStack(Items.DIAMOND)), 1, "diamonds from natural ore " + plain.drops());
		helper.assertValueEqual(plain.drops().size(), 1, "drops " + plain.drops());
		helper.assertTrue(plain.xpOrbs() > 0, "natural ore dropped no XP");

		Mined fortune = placeAndMine(helper, new ItemStack(Items.DEEPSLATE_DIAMOND_ORE), pickaxe(helper, Enchantments.FORTUNE, 3));
		int diamonds = count(fortune.drops(), new ItemStack(Items.DIAMOND));
		helper.assertTrue(diamonds >= 1 && diamonds <= 4, "fortune III diamonds: " + fortune.drops());

		Mined silk = placeAndMine(helper, new ItemStack(Items.DEEPSLATE_DIAMOND_ORE), pickaxe(helper, Enchantments.SILK_TOUCH, 1));
		helper.assertValueEqual(count(silk.drops(), new ItemStack(Items.DEEPSLATE_DIAMOND_ORE)), 1, "silk-touched natural ore " + silk.drops());
		helper.assertValueEqual(silk.xpOrbs(), 0, "silk touch XP");
		helper.succeed();
	}

	@GameTest
	public void syntheticOreUsesItsOwnTableAndGivesNoXp(GameTestHelper helper) {
		ItemStack synthetic = syntheticFromRecipe(helper);
		helper.assertValueEqual(synthetic.getHoverName().getString(), "Synthetic Diamond Ore", "recipe result name");

		// Mined by a survival player: synthetic state placed, at most one diamond (25 % chance), never XP, never ore.
		for (int i = 0; i < 6; i++) {
			ItemStack tool = i % 2 == 0 ? pickaxe(helper, null, 0) : pickaxe(helper, Enchantments.FORTUNE, 3);
			Mined mined = placeAndMine(helper, synthetic.copy(), tool);
			helper.assertValueEqual(mined.placed().getValue(SyntheticOre.SYNTHETIC), true, "synthetic item placed natural ore");
			helper.assertTrue(mined.drops().stream().allMatch(s -> s.is(Items.DIAMOND)) && count(mined.drops(), new ItemStack(Items.DIAMOND)) <= 1,
				"drops from synthetic ore " + mined.drops());
			helper.assertValueEqual(mined.xpOrbs(), 0, "XP orbs from synthetic ore");
		}

		// Silk touch gives the synthetic item back, identical to the recipe result (same components, so they stack).
		Mined silk = placeAndMine(helper, synthetic.copy(), pickaxe(helper, Enchantments.SILK_TOUCH, 1));
		helper.assertValueEqual(silk.drops().size(), 1, "silk-touched synthetic drops " + silk.drops());
		helper.assertTrue(ItemStack.isSameItemSameComponents(silk.drops().getFirst(), synthetic),
			"silk-touched synthetic gave " + silk.drops().getFirst().getComponentsPatch() + ", recipe gives " + synthetic.getComponentsPatch());
		helper.assertValueEqual(silk.xpOrbs(), 0, "XP orbs from silk-touched synthetic ore");

		// Round trip: the silk-touched item places synthetic ore again.
		Mined again = placeAndMine(helper, silk.drops().getFirst().copy(), pickaxe(helper, Enchantments.SILK_TOUCH, 1));
		helper.assertValueEqual(again.placed().getValue(SyntheticOre.SYNTHETIC), true, "silk-touched item placed natural ore");
		helper.assertTrue(ItemStack.isSameItemSameComponents(again.drops().getFirst(), synthetic), "second silk touch " + again.drops());
		helper.succeed();
	}

	/**
	 * The odds, by rolling deepslate_diamond_ore's (overridden) loot table for the synthetic state many times with a fixed
	 * seed: about 25 % one diamond, otherwise nothing, and Fortune III doesn't change it. Natural ore always drops.
	 */
	@GameTest
	public void syntheticOreDropsADiamondAboutOneTimeInFour(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		LootTable table = level.getServer().reloadableRegistries().getLootTable(Blocks.DEEPSLATE_DIAMOND_ORE.getLootTable().orElseThrow());
		BlockState synthetic = Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState().setValue(SyntheticOre.SYNTHETIC, true);
		int rolls = 2000;
		for (ItemStack tool : List.of(pickaxe(helper, null, 0), pickaxe(helper, Enchantments.FORTUNE, 3))) {
			RandomSource random = RandomSource.create(8587L);
			int diamonds = 0;
			int empty = 0;
			for (int i = 0; i < rolls; i++) {
				List<ItemStack> drops = table.getRandomItems(blockParams(helper, synthetic, tool), random);
				int n = count(drops, new ItemStack(Items.DIAMOND));
				helper.assertTrue(n <= 1 && drops.size() == n, "synthetic roll gave " + drops);
				diamonds += n;
				empty += drops.isEmpty() ? 1 : 0;
			}
			// Binomial(2000, 0.25): mean 500, sd ~19; 420..580 is over 4 sd either way.
			helper.assertTrue(diamonds >= 420 && diamonds <= 580, "diamonds in " + rolls + " synthetic rolls with " + tool + ": " + diamonds);
			helper.assertValueEqual(empty, rolls - diamonds, "rolls that dropped nothing");
		}
		BlockState natural = Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState();
		RandomSource random = RandomSource.create(8587L);
		for (int i = 0; i < 200; i++) {
			List<ItemStack> drops = table.getRandomItems(blockParams(helper, natural, pickaxe(helper, null, 0)), random);
			helper.assertTrue(count(drops, new ItemStack(Items.DIAMOND)) == 1 && drops.size() == 1, "natural roll gave " + drops);
		}
		helper.succeed();
	}

	private static LootParams blockParams(GameTestHelper helper, BlockState state, ItemStack tool) {
		return new LootParams.Builder(helper.getLevel())
			.withParameter(LootContextParams.ORIGIN, helper.absoluteVec(Vec3.atCenterOf(PLACED)))
			.withParameter(LootContextParams.TOOL, tool)
			.withParameter(LootContextParams.BLOCK_STATE, state)
			.create(LootContextParamSets.BLOCK);
	}

	/** Decided and documented: pick-block on synthetic ore gives the plain ore item (creative can convert it). */
	@GameTest
	public void pickBlockOnSyntheticGivesPlainOre(GameTestHelper helper) {
		helper.setBlock(PLACED, Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState().setValue(SyntheticOre.SYNTHETIC, true));
		BlockPos abs = helper.absolutePos(PLACED);
		ItemStack picked = helper.getLevel().getBlockState(abs).getCloneItemStack(helper.getLevel(), abs, true);
		helper.assertTrue(ItemStack.isSameItemSameComponents(picked, new ItemStack(Items.DEEPSLATE_DIAMOND_ORE)),
			"pick-block gave " + picked + " " + picked.getComponentsPatch());
		helper.succeed();
	}

	@GameTest
	public void savesWithAndWithoutThePropertyLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockState natural = Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState();
		BlockState synthetic = natural.setValue(SyntheticOre.SYNTHETIC, true);

		// Natural ore is the default state, which 26.3 writes as a bare block id: byte-for-byte what vanilla writes.
		Tag naturalTag = BlockState.CODEC.encodeStart(NbtOps.INSTANCE, natural).getOrThrow();
		helper.assertValueEqual(naturalTag, StringTag.valueOf("minecraft:deepslate_diamond_ore"), "natural ore encoding");
		Tag syntheticTag = BlockState.CODEC.encodeStart(NbtOps.INSTANCE, synthetic).getOrThrow();
		helper.assertTrue(syntheticTag instanceof CompoundTag c && c.getCompoundOrEmpty("properties").getStringOr("synthetic", "").equals("true"),
			"synthetic ore encoding " + syntheticTag);

		// An old save (no property) decodes as natural, in both palette forms.
		helper.assertValueEqual(BlockState.CODEC.parse(NbtOps.INSTANCE, StringTag.valueOf("minecraft:deepslate_diamond_ore")).getOrThrow(), natural, "bare id");
		CompoundTag noProps = new CompoundTag();
		noProps.putString("id", "minecraft:deepslate_diamond_ore");
		helper.assertValueEqual(BlockState.CODEC.parse(NbtOps.INSTANCE, noProps).getOrThrow(), natural, "{id} without properties");

		// The mod removed: the block has a single state again, and its codec ignores the "properties" it no longer knows.
		// Stone is such a block today; the same entry decodes to plain stone, both through the codec and NbtUtils.
		CompoundTag unknown = new CompoundTag();
		unknown.putString("id", "minecraft:stone");
		CompoundTag props = new CompoundTag();
		props.putString("synthetic", "true");
		unknown.put("properties", props);
		helper.assertValueEqual(BlockState.CODEC.parse(NbtOps.INSTANCE, unknown).getOrThrow(), Blocks.STONE.defaultBlockState(), "unknown property via codec");
		helper.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK, unknown), Blocks.STONE.defaultBlockState(), "unknown property via NbtUtils");

		// A whole chunk: save it the way ChunkMap does, read it back, and read it again with the property stripped.
		helper.setBlock(FLOOR, natural);
		helper.setBlock(PLACED, synthetic);
		BlockPos naturalPos = helper.absolutePos(FLOOR);
		BlockPos syntheticPos = helper.absolutePos(PLACED);
		LevelChunk chunk = level.getChunkAt(syntheticPos);
		helper.assertTrue(chunk == level.getChunkAt(naturalPos), "test blocks straddle a chunk border");
		CompoundTag tag = SerializableChunkData.copyOf(level, chunk).write();
		int stripped = 0;
		ChunkAccess reloaded = read(level, chunk, tag.copy());
		helper.assertValueEqual(reloaded.getBlockState(syntheticPos), synthetic, "synthetic ore after chunk reload");
		helper.assertValueEqual(reloaded.getBlockState(naturalPos), natural, "natural ore after chunk reload");
		for (Tag section : tag.getListOrEmpty("sections")) {
			ListTag palette = ((CompoundTag) section).getCompoundOrEmpty("block_states").getListOrEmpty("palette");
			for (int i = 0; i < palette.size(); i++) {
				if (palette.get(i) instanceof CompoundTag entry && entry.getStringOr("id", "").equals("minecraft:deepslate_diamond_ore")) {
					entry.remove("properties");
					stripped++;
				}
			}
		}
		helper.assertValueEqual(stripped, 1, "palette entries with properties");
		ChunkAccess old = read(level, chunk, tag);
		helper.assertValueEqual(old.getBlockState(syntheticPos), natural, "ore saved without the property");
		helper.succeed();
	}

	private static ChunkAccess read(ServerLevel level, LevelChunk chunk, CompoundTag tag) {
		// Drop the light data so reading the copy does not queue light updates for the live chunk.
		tag.getListOrEmpty("sections").forEach(section -> {
			if (section instanceof CompoundTag sectionTag) {
				sectionTag.remove("BlockLight");
				sectionTag.remove("SkyLight");
			}
		});
		return SerializableChunkData.parse(level, level.palettedContainerFactory(), tag)
			.read(level, level.getPoiManager(), level.getChunkSource().chunkMap.storageInfo(), chunk.getPos());
	}
}
