// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.grave.CryptYardPiece;
import io.github.bubba8587.bubbas_reworked_mobs.grave.GraveText;
import io.github.bubba8587.bubbas_reworked_mobs.grave.GraveyardCommand;
import io.github.bubba8587.bubbas_reworked_mobs.grave.GravestoneBlockEntity;
import io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Gravestones and crypt yards (specs/graveyards.md). The yard test is core (through ReworkedMobsCoreGameTest); the rest
 * is milestone. The crypt below a yard is checked by eye in the client part {@code graves} (the test fields are too
 * shallow for it).
 */
public class GravesGameTest {
	private static final String FIELD = EndermanGameTest.FIELD;

	/**
	 * Core. A built yard (19 x 13, no crypt) has gravestones carrying only glyphs or nothing (never real names) and
	 * sleepers in its graves.
	 */
	public void yardHasStonesWithoutNamesAndSleepers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// 19 wide (six grave columns) so that graves skipped at a chunk border, which falls differently wherever the
		// test field is, still leave several.
		CryptYardPiece piece = GraveyardCommand.build(level, helper.absolutePos(new BlockPos(11, 0, 1)), false, 4242L, 19, 13);
		BoundingBox box = piece.getBoundingBox();
		int stones = 0;
		int sleepers = 0;
		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			var state = level.getBlockState(pos);
			if (state.is(ModBlocks.GRAVESTONE)) {
				stones++;
				helper.assertTrue(level.getBlockEntity(pos) instanceof GravestoneBlockEntity stone && GraveText.noRealNames(stone.getText(SignTextSlot.FRONT))
						&& !stone.getText(SignTextSlot.BACK).hasMessage(false), "a generated gravestone carries readable text at " + pos.toShortString());
			} else if (state.getBlock() instanceof SleeperBlock sleeperBlock) {
				sleepers++;
				// The resting dead (owner, 2026-11-22): a yard grave holds a skeleton that never wakes.
				// (a skeleton, or by climate a husk in deserts and a stray in snow).
				helper.assertTrue(sleeperBlock.variant() == piece.climate().resting() && SleeperBlock.isResting(state),
						"a yard grave's sleeper should be the resting " + piece.climate().resting() + " of a " + piece.climate() + " yard: " + state);
			}
		}
		helper.assertTrue(stones >= 2, "expected a few gravestones, found " + stones);
		helper.assertTrue(sleepers >= 1, "expected sleepers in the graves, found none");
		helper.succeed();
	}

	/** Worldgen text is only ever blank or glyphs, over many stones. */
	@GameTest(maxTicks = 5)
	public void generatedTextIsGlyphsOrBlank(GameTestHelper helper) {
		RandomSource random = RandomSource.create(7L);
		int blank = 0;
		for (int i = 0; i < 2000; i++) {
			SignText text = GraveText.random(random, 0.4F);
			helper.assertTrue(GraveText.noRealNames(text), "generated readable text: " + text.getMessages(false));
			if (!text.hasMessage(false)) {
				blank++;
			}
		}
		helper.assertTrue(blank > 600 && blank < 1000, "blank share off: " + blank + " of 2000");
		// A player's name is readable text: the check catches it.
		SignText named = new SignText(List.of(Component.literal("Steve"), Component.empty(), Component.empty(), Component.empty()),
				List.of(Component.literal("Steve"), Component.empty(), Component.empty(), Component.empty()), DyeColor.BLACK, false);
		helper.assertFalse(GraveText.noRealNames(named), "readable text passed the no-names check");
		helper.succeed();
	}

	/**
	 * A gravestone takes any of the 16 rotations, keeps text front and back (and its dye and glow) through a save and
	 * load, and drops itself when mined with a pickaxe.
	 */
	@GameTest(structure = FIELD, maxTicks = 20)
	public void gravestoneIsASign(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int r = 0; r < 16; r++) {
			BlockPos pos = new BlockPos(2 + r, 1, 3);
			helper.setBlock(pos, ModBlocks.GRAVESTONE.defaultBlockState().setValue(StandingSignBlock.ROTATION, r));
			helper.assertTrue(helper.getBlockState(pos).getValue(StandingSignBlock.ROTATION) == r, "rotation " + r + " didn't stick");
		}
		BlockPos at = helper.absolutePos(new BlockPos(2, 1, 3));
		GravestoneBlockEntity stone = (GravestoneBlockEntity) level.getBlockEntity(at);
		List<Component> front = List.of(Component.literal("Here lies"), Component.literal("Bramble"), Component.empty(), Component.empty());
		List<Component> back = List.of(Component.empty(), Component.literal("back"), Component.empty(), Component.empty());
		stone.setText(new SignText(front, front, DyeColor.RED, true), SignTextSlot.FRONT);
		stone.setText(new SignText(back, back, DyeColor.BLACK, false), SignTextSlot.BACK);
		CompoundTag saved = stone.saveWithFullMetadata(level.registryAccess());
		BlockEntity loaded = BlockEntity.loadStatic(at, stone.getBlockState(), saved, level.registryAccess());
		helper.assertTrue(loaded instanceof GravestoneBlockEntity, "didn't load as a gravestone: " + loaded);
		GravestoneBlockEntity copy = (GravestoneBlockEntity) loaded;
		helper.assertTrue(copy.getText(SignTextSlot.FRONT).getMessages(false).get(1).getString().equals("Bramble")
				&& copy.getText(SignTextSlot.FRONT).getColor() == DyeColor.RED && copy.getText(SignTextSlot.FRONT).hasGlowingText(), "front text lost");
		helper.assertTrue(copy.getText(SignTextSlot.BACK).getMessages(false).get(1).getString().equals("back"), "back text lost");
		List<ItemStack> drops = Block.getDrops(stone.getBlockState(), level, at, stone, null, new ItemStack(Items.IRON_PICKAXE));
		helper.assertTrue(drops.size() == 1 && drops.getFirst().is(ModBlocks.GRAVESTONE.asItem()), "mined, it dropped " + drops);
		helper.succeed();
	}

	/**
	 * The resting dead (owner, 2026-11-22) never wake: an explosion three blocks away, a bell rung beside them and being
	 * robbed by a player leave a yard grave's skeleton asleep (an ordinary sleeper beside it, the control, wakes at the
	 * explosion). Mined with Silk Touch, it keeps that as an item, and placed again it still sleeps through the noise.
	 */
	@GameTest(structure = FIELD, maxTicks = 60)
	public void restingDeadNeverWake(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos restingPos = new BlockPos(8, 1, 4);
		BlockPos controlPos = new BlockPos(8, 1, 12);
		helper.setBlock(restingPos, ModBlocks.sleeper(io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant.SKELETON).defaultBlockState()
				.setValue(SleeperBlock.POSE, io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose.LYING).setValue(SleeperBlock.RESTING, true));
		helper.setBlock(controlPos, ModBlocks.sleeper(io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant.SKELETON).defaultBlockState()
				.setValue(SleeperBlock.POSE, io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose.LYING));
		var resting = helper.getBlockEntity(restingPos, io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity.class);
		var control = helper.getBlockEntity(controlPos, io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity.class);
		resting.setSensitivity(10.0F);
		resting.setGear(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
		// Noise: explosions three blocks from each.
		boom(helper, new BlockPos(11, 1, 4));
		boom(helper, new BlockPos(11, 1, 12));
		helper.assertTrue(control.isWakePending(), "control: an ordinary sleeper should wake at an explosion three blocks off");
		helper.assertFalse(resting.isWakePending(), "the resting dead woke at an explosion");
		// A bell rung right beside it.
		io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBells.rang(level, helper.absolutePos(new BlockPos(9, 1, 4)), null);
		helper.assertFalse(resting.isWakePending(), "the resting dead woke at a bell");
		// Robbed by a player (sensitivity 10 makes an ordinary sleeper's robbery wake roll certain).
		net.minecraft.server.level.ServerPlayer player = TestPlayers.survival(helper);
		player.snapTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(8.5, 1.0, 6.5)));
		BlockPos abs = helper.absolutePos(restingPos);
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND,
				new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(abs).relative(Direction.UP, 0.5), Direction.UP, abs, false));
		helper.assertTrue(player.getInventory().contains(new ItemStack(Items.LEATHER_CHESTPLATE)), "robbing didn't take the chestplate");
		helper.assertFalse(resting.isWakePending(), "the resting dead woke when robbed");
		// Mined with Silk Touch: the item keeps it; placed again, it still sleeps through an explosion.
		ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
		pick.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
				.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
		List<ItemStack> drops = Block.getDrops(level.getBlockState(abs), level, abs, resting, player, pick);
		ItemStack item = drops.stream().filter(d -> d.is(net.minecraft.world.item.Items.AIR) == false
				&& d.getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof SleeperBlock).findFirst().orElse(ItemStack.EMPTY);
		helper.assertFalse(item.isEmpty(), "Silk Touch gave no sleeper item: " + drops);
		var props = item.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
		helper.assertTrue(props != null && Boolean.TRUE.equals(props.get(SleeperBlock.RESTING)), "the item lost its resting state: " + props);
		level.setBlock(abs, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, item);
		BlockPos floor = abs.below();
		player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
				new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor).relative(Direction.UP, 0.5), Direction.UP, floor, false)));
		helper.assertTrue(SleeperBlock.isResting(level.getBlockState(abs)), "placed again, it isn't resting: " + level.getBlockState(abs));
		var placed = helper.getBlockEntity(restingPos, io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity.class);
		boom(helper, new BlockPos(11, 1, 4));
		helper.assertFalse(placed.isWakePending(), "placed again, it woke at an explosion");
		// And nothing wakes it over the next while either.
		helper.runAtTickTime(40, () -> {
			helper.assertTrue(level.getBlockState(abs).getBlock() instanceof SleeperBlock, "the resting dead got up");
			level.getServer().getPlayerList().remove(player);
			helper.succeed();
		});
	}

	private static void boom(GameTestHelper helper, BlockPos at) {
		helper.getLevel().gameEvent(net.minecraft.world.level.gameevent.GameEvent.EXPLODE, helper.absoluteVec(net.minecraft.world.phys.Vec3.atCenterOf(at)),
				net.minecraft.world.level.gameevent.GameEvent.Context.of((net.minecraft.world.entity.Entity) null));
	}

	/** Every block of a built yard (with a crypt) can stay where it is: no candle, lantern, sign or plant without support. */
	@GameTest(structure = CampsGameTest.FIELD, maxTicks = 20)
	public void everyYardBlockIsSupported(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> loose = new java.util.ArrayList<>();
		for (YardKinds.Wealth wealth : YardKinds.Wealth.values()) {
			CryptYardPiece piece = GraveyardCommand.build(level, helper.absolutePos(new BlockPos(16, 7, 1)), 900L + wealth.ordinal(), 15, 13, true,
					YardKinds.State.OVERGROWN, wealth);
			loose.addAll(CampsGameTest.unsupported(level, piece.getBoundingBox()));
		}
		helper.assertTrue(loose.isEmpty(), "blocks that would pop off: " + loose.subList(0, Math.min(12, loose.size())));
		helper.succeed();
	}

	/** A crypt built with nothing round it is closed in: from the inside, open space never leaks out past the crypt's walls, floor or roof. */
	@GameTest(structure = CampsGameTest.FIELD, maxTicks = 20)
	public void cryptsAreClosedInWithoutGround(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (YardKinds.Wealth wealth : YardKinds.Wealth.values()) {
			CryptYardPiece piece = GraveyardCommand.build(level, helper.absolutePos(new BlockPos(16, 7, 1)), 1900L + wealth.ordinal(), 15, 13, true,
					YardKinds.State.OVERGROWN, wealth);
			net.minecraft.world.level.levelgen.structure.BoundingBox box = piece.getBoundingBox();
			int top = piece.gate().getY() - 2;
			java.util.ArrayDeque<BlockPos> open = new java.util.ArrayDeque<>(List.of(piece.vaultCentre()));
			java.util.Set<BlockPos> seen = new java.util.HashSet<>(open);
			while (!open.isEmpty()) {
				BlockPos pos = open.poll();
				helper.assertTrue(pos.getX() > box.minX() && pos.getX() < box.maxX() && pos.getZ() > box.minZ() && pos.getZ() < box.maxZ()
						&& pos.getY() > box.minY(), wealth + " crypt is open to the outside at " + pos);
				for (Direction d : Direction.values()) {
					BlockPos next = pos.relative(d);
					if (next.getY() <= top && seen.add(next) && !level.getBlockState(next).isCollisionShapeFullBlock(level, next)) {
						open.add(next);
					}
				}
			}
		}
		helper.succeed();
	}

	/**
	 * Warding statues per crypt wing (specs/graveyards.md): over 4000 seeds each wing rolls its chance (noble 85 %, family
	 * 65 %, pauper 35 %, within 3 points); without Bubba's Totems & Artifacts none is placed and the crypt builds as before.
	 */
	@GameTest(structure = CampsGameTest.FIELD, maxTicks = 20)
	public void cryptWardStatueOdds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos gate = helper.absolutePos(new BlockPos(16, 7, 1));
		int seeds = 4000;
		for (YardKinds.Wealth wealth : YardKinds.Wealth.values()) {
			int wings = wealth == YardKinds.Wealth.NOBLE ? 3 : 1;
			float chance = switch (wealth) {
				case NOBLE -> io.github.bubba8587.bubbas_reworked_mobs.grave.GraveTuning.CRYPT_STATUE_NOBLE;
				case FAMILY -> io.github.bubba8587.bubbas_reworked_mobs.grave.GraveTuning.CRYPT_STATUE_FAMILY;
				case PAUPER -> io.github.bubba8587.bubbas_reworked_mobs.grave.GraveTuning.CRYPT_STATUE_PAUPER;
			};
			int statues = 0;
			for (long seed = 0; seed < seeds; seed++) {
				statues += piece(level, gate, seed, wealth).wardSpots().size();
			}
			float rate = statues / (float) (seeds * wings);
			helper.assertTrue(Math.abs(rate - chance) < 0.03F, wealth + ": statue rate " + rate + ", expected " + chance);
		}
		boolean totems = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("bubbas_totems_artifacts");
		helper.assertTrue(totems == (CryptYardPiece.wardStatue(Direction.NORTH) != null), "statue lookup doesn't match whether Totems & Artifacts is loaded");
		if (!totems) {
			long seed = 1;
			while (piece(level, gate, seed, YardKinds.Wealth.FAMILY).wardSpots().isEmpty()) {
				seed++;
			}
			CryptYardPiece built = GraveyardCommand.build(level, gate, seed, 15, 13, true, YardKinds.State.OVERGROWN, YardKinds.Wealth.FAMILY);
			for (CryptYardPiece.WardSpot spot : built.wardSpots()) {
				helper.assertTrue(level.getBlockState(spot.pos()).isAir(), "something stands on the statue spot without Totems & Artifacts: " + level.getBlockState(spot.pos()));
			}
		}
		helper.succeed();
	}

	private static CryptYardPiece piece(ServerLevel level, BlockPos gate, long seed, YardKinds.Wealth wealth) {
		return new CryptYardPiece(seed, gate, 15, 13, true, io.github.bubba8587.bubbas_reworked_mobs.grave.YardTrees.forBiome(level.getBiome(gate)),
				YardKinds.State.OVERGROWN, wealth, YardKinds.Climate.TEMPERATE);
	}
}
