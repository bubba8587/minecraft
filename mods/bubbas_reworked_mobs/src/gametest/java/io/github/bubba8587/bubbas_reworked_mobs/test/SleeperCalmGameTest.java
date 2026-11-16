// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.api.Sleepers;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadTuning;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBells;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlock;
import io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What calms or stirs sleepers here (specs/undead.md "Warding"): strong light, bells, and the api/Sleepers hook other
 * mods use (Bubba's Totems & Artifacts' Totem of Warding and Grave Talisman). All core, run through
 * ReworkedMobsCoreGameTest.
 */
public class SleeperCalmGameTest {
	private static SleeperBlockEntity sleeper(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, ModBlocks.sleeper(UndeadVariant.SKELETON).defaultBlockState().setValue(SleeperBlock.POSE, SleeperPose.SITTING)
				.setValue(SleeperBlock.FACING, Direction.EAST));
		return helper.getBlockEntity(pos, SleeperBlockEntity.class);
	}

	private static long now(GameTestHelper helper) {
		return helper.getLevel().getGameTime();
	}

	/**
	 * Core. A sleeper in the dark takes noise in full; with a glowstone block next to it (light 14) it takes x0.7.
	 * The test server's light engine catches up slowly (light was still 0 after 40 ticks), hence the long timeout.
	 */
	public void strongLightCalmsSleepers(GameTestHelper helper) {
		BlockPos pos = new BlockPos(4, 1, 4);
		SleeperBlockEntity sleeper = sleeper(helper, pos);
		helper.assertTrue(sleeper.effectiveSensitivity(helper.getLevel()) == 1.0F, "dark sleeper not at full sensitivity");
		helper.setBlock(pos.east(), Blocks.GLOWSTONE);
		helper.succeedWhen(() -> helper.assertTrue(
				Math.abs(sleeper.effectiveSensitivity(helper.getLevel()) - UndeadTuning.LIGHT_CALM_FACTOR) < 1.0E-4F,
				"lit sleeper sensitivity: " + sleeper.effectiveSensitivity(helper.getLevel()) + ", block light "
						+ helper.getLevel().getBrightness(LightLayer.BLOCK, helper.absolutePos(pos))));
	}

	/**
	 * Core. A bell rung 14 blocks away (beyond the 12 where ordinary noise fades out) wakes a sleeper; one 23 blocks away
	 * only stirs (the bell's own linear falloff, 32 blocks), and the bell's plain vibration is not counted twice.
	 */
	public void bellsAreLoudToSleepers(GameTestHelper helper) {
		BlockPos bell = new BlockPos(0, 1, 8);
		helper.setBlock(bell, Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.FLOOR));
		SleeperBlockEntity near = sleeper(helper, new BlockPos(14, 1, 8));
		SleeperBlockEntity far = sleeper(helper, new BlockPos(23, 1, 8));
		((BellBlock) Blocks.BELL).attemptToRing(helper.getLevel(), helper.absolutePos(bell), Direction.EAST);
		helper.assertTrue(near.isWakePending(), "bell 14 blocks off did not wake the sleeper");
		float expected = SleeperBells.loudness(23.0);
		float heard = far.getDisturbance(now(helper));
		helper.assertTrue(!far.isWakePending() && heard >= UndeadTuning.STIR_THRESHOLD && Math.abs(heard - expected) < 1.0F,
				"bell 23 blocks off: meter " + heard + ", expected " + expected);
		helper.succeed();
	}

	/**
	 * Core. The api/Sleepers hook: calm takes points off sleepers in range only; keepAsleep zeroes the sensitivity of
	 * sleepers inside its area until released (or until it lapses); isSleeper and nearestSqr find sleepers.
	 */
	public void apiCalmsSensesAndKeepsSleepersAsleep(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos nearPos = new BlockPos(9, 1, 8);
		BlockPos farPos = new BlockPos(22, 1, 8);
		SleeperBlockEntity near = sleeper(helper, nearPos);
		SleeperBlockEntity far = sleeper(helper, farPos);
		near.disturb(now(helper), 70.0F);
		far.disturb(now(helper), 70.0F);
		Vec3 centre = helper.absoluteVec(new Vec3(12.5, 1.0, 8.5));

		int calmed = Sleepers.calm(level, centre, 8.0, 40.0F);
		helper.assertTrue(calmed == 1, "calm reached " + calmed + " sleepers, expected 1");
		helper.assertTrue(Math.abs(near.getDisturbance(now(helper)) - 30.0F) < 0.6F, "near sleeper after calm: " + near.getDisturbance(now(helper)));
		helper.assertTrue(far.getDisturbance(now(helper)) > 60.0F, "calm reached a sleeper 10 blocks out");

		helper.assertTrue(Sleepers.isSleeper(level, helper.absolutePos(nearPos)), "isSleeper missed a sleeper");
		helper.assertFalse(Sleepers.isSleeper(level, helper.absolutePos(new BlockPos(5, 1, 5))), "isSleeper on air");
		double d = Sleepers.nearestSqr(level, centre, 40.0);
		helper.assertTrue(Math.abs(d - centre.distanceToSqr(Vec3.atCenterOf(helper.absolutePos(nearPos)))) < 1.0E-6, "nearestSqr: " + d);
		helper.assertTrue(Sleepers.nearestSqr(level, centre, 2.0) == Double.MAX_VALUE, "nearestSqr found one beyond its range");

		AABB area = new AABB(Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(0, 0, 0))), Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(16, 4, 16))));
		Object key = new Object();
		Sleepers.keepAsleep(level, key, area, 400);
		helper.assertTrue(near.effectiveSensitivity(level) == 0.0F, "a sleeper kept asleep can still wake");
		helper.assertTrue(far.effectiveSensitivity(level) == 1.0F, "keepAsleep reached outside its area");
		Sleepers.release(level, key);
		helper.assertTrue(near.effectiveSensitivity(level) == 1.0F, "release didn't end the area");
		Sleepers.keepAsleep(level, key, area, 1);
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(near.effectiveSensitivity(level) == 1.0F, "a keep-asleep area that wasn't refreshed didn't lapse");
			helper.succeed();
		});
	}
}
