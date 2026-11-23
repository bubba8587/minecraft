// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.bogged.BoggedLurk;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModEntities;
import io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadTuning;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Husk and stray sleepers and the lurking bogged (specs/undead.md "Undead variants"). Milestone.
 */
public class UndeadVariantsGameTest {
	private static final BlockPos SLEEPER = new BlockPos(3, 1, 3);

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(x, 0, z, Blocks.STONE);
			}
		}
	}

	private static SleeperBlockEntity sleeper(GameTestHelper helper, UndeadVariant variant) {
		helper.setBlock(SLEEPER, ModBlocks.sleeper(variant).defaultBlockState().setValue(SleeperBlock.POSE, SleeperPose.LYING)
				.setValue(SleeperBlock.FACING, Direction.EAST));
		return helper.getBlockEntity(SLEEPER, SleeperBlockEntity.class);
	}

	private static ServerPlayer player(GameTestHelper helper, double x, double z, float yRot) {
		ServerPlayer player = SleeperNoiseGameTest.survivalPlayer(helper);
		Vec3 abs = helper.absoluteVec(new Vec3(x, 1.0, z));
		player.snapTo(abs.x, abs.y, abs.z, yRot, 0.0F);
		player.setYHeadRot(yRot);
		player.setOnGround(true);
		return player;
	}

	private static void remove(GameTestHelper helper, ServerPlayer player) {
		helper.getLevel().getServer().getPlayerList().remove(player);
	}

	@GameTest(maxTicks = 40)
	public void huskBurstsOutOfSandAndBlinds(GameTestHelper helper) {
		floor(helper);
		SleeperBlockEntity husk = sleeper(helper, UndeadVariant.HUSK);
		helper.setBlock(SLEEPER.above(), Blocks.SAND);
		ServerPlayer player = player(helper, 5.5, 3.5, 90.0F);
		husk.scheduleWake(helper.getLevel(), null, null, 1);
		helper.succeedWhen(() -> {
			helper.assertEntityPresent(ModEntities.woken(UndeadVariant.HUSK));
			helper.assertBlockPresent(Blocks.AIR, SLEEPER.above());
			helper.assertTrue(player.hasEffect(MobEffects.BLINDNESS), "the sand burst should blind a player 2 blocks away");
			remove(helper, player);
		});
	}

	@GameTest(maxTicks = 80)
	public void strayCracksItsIceBeforeBreakingFree(GameTestHelper helper) {
		floor(helper);
		SleeperBlockEntity stray = sleeper(helper, UndeadVariant.STRAY);
		helper.setBlock(SLEEPER.above(), Blocks.PACKED_ICE);
		stray.scheduleWake(helper.getLevel(), null, null, 1);
		helper.runAfterDelay(UndeadTuning.STRAY_CRACK_TICKS - 10, () -> {
			// The warning: still frozen a second after it was woken.
			helper.assertBlockPresent(ModBlocks.sleeper(UndeadVariant.STRAY), SLEEPER);
			helper.assertBlockPresent(Blocks.PACKED_ICE, SLEEPER.above());
		});
		helper.runAfterDelay(UndeadTuning.STRAY_CRACK_TICKS + 10, () -> {
			helper.assertEntityPresent(ModEntities.woken(UndeadVariant.STRAY));
			helper.assertBlockPresent(Blocks.AIR, SLEEPER.above());
			helper.succeed();
		});
	}

	@GameTest
	public void huskAndStrayTags(GameTestHelper helper) {
		var husk = ModEntities.woken(UndeadVariant.HUSK).builtInRegistryHolder();
		var stray = ModEntities.woken(UndeadVariant.STRAY).builtInRegistryHolder();
		helper.assertTrue(husk.is(EntityTypeTags.ZOMBIES) && !husk.is(EntityTypeTags.BURN_IN_DAYLIGHT), "husks are zombies that don't burn");
		helper.assertTrue(stray.is(EntityTypeTags.SKELETONS) && stray.is(EntityTypeTags.BURN_IN_DAYLIGHT), "strays are skeletons that burn");
		helper.succeed();
	}

	/** A pool two deep over stone: the bogged's hiding spot is its feet at (2, 1, 2). */
	private static BlockPos pool(GameTestHelper helper) {
		floor(helper);
		for (int x = 1; x <= 3; x++) {
			for (int z = 1; z <= 3; z++) {
				helper.setBlock(x, 1, z, Blocks.WATER);
				helper.setBlock(x, 2, z, Blocks.WATER);
			}
		}
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				if (x < 1 || x > 3 || z < 1 || z > 3) {
					helper.setBlock(x, 1, z, Blocks.STONE);
					helper.setBlock(x, 2, z, Blocks.STONE);
				}
			}
		}
		return new BlockPos(2, 1, 2);
	}

	@GameTest(maxTicks = 60)
	public void boggedLurksWhileWatchedAndAmbushesAnAvertedBack(GameTestHelper helper) {
		BlockPos spot = pool(helper);
		BlockPos abs = helper.absolutePos(spot);
		helper.assertTrue(abs.equals(BoggedLurk.hideAt(helper.getLevel(), abs.getX(), abs.getZ(), abs.getY() + 2)), "a 2-deep pool should be a hiding spot: " + abs + " vs " + BoggedLurk.hideAt(helper.getLevel(), abs.getX(), abs.getZ(), abs.getY() + 2) + " top "
				+ helper.getLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, abs.getX(), abs.getZ()));
		Bogged bogged = helper.spawn(net.minecraft.world.entity.EntityTypes.BOGGED, spot);
		BoggedLurk.lurkAt(bogged, abs);
		// Facing the bogged from 5 blocks east (yaw 90 looks toward -x).
		ServerPlayer player = player(helper, 7.5, 2.5, 90.0F);
		player.snapTo(player.getX(), helper.absoluteVec(new Vec3(0, 3.0, 0)).y, player.getZ(), 90.0F, 0.0F);
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(BoggedLurk.isLurking(bogged) && bogged.getPose() == Pose.CROUCHING, "watched, it should stay crouched in hiding");
			helper.assertTrue(bogged.getTarget() == null, "a lurking bogged holds its fire");
			player.snapTo(player.getX(), player.getY(), player.getZ(), -90.0F, 0.0F); // turns away
			player.setYHeadRot(-90.0F);
		});
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(!BoggedLurk.isLurking(bogged) && bogged.getTarget() == player, "it should ambush once the player's back is turned");
			remove(helper, player);
			helper.succeed();
		});
	}
}
