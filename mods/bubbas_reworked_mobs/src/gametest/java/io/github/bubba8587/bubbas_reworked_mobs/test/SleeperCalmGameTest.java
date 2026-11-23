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

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * What calms or stirs sleepers here (specs/undead.md "Warding"): strong light, bells, and the api/Sleepers hook other
 * mods use (Bubba's Totems & Artifacts' Totem of Warding and Grave Talisman). Milestone, but the api/Sleepers test is
 * core (through ReworkedMobsCoreGameTest), and the light test runs through SleeperNoiseGameTest.
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

	/** Milestone: with a glowstone block next to it (light 14) a sleeper takes noise x0.7, through the real light engine. */
	public void strongLightCalmsSleepers(GameTestHelper helper) {
		// The rule (block light at the calm level or more: x0.7; below it, or dark, none), then the real thing.
		helper.assertTrue(SleeperBlockEntity.lightCalm(UndeadTuning.LIGHT_CALM_LEVEL) == UndeadTuning.LIGHT_CALM_FACTOR, "light at the calm level should calm");
		helper.assertTrue(SleeperBlockEntity.lightCalm(UndeadTuning.LIGHT_CALM_LEVEL - 1) == 1.0F, "light below it should not");
		helper.assertTrue(SleeperBlockEntity.lightCalm(0) == 1.0F, "dark should not");
		BlockPos pos = new BlockPos(4, 1, 4);
		SleeperBlockEntity sleeper = sleeper(helper, pos);
		helper.assertTrue(sleeper.effectiveSensitivity(helper.getLevel()) == 1.0F, "dark sleeper not at full sensitivity");
		// Place the lamp a second in: placed on tick 0 of a freshly generated test chunk, the light engine sometimes
		// never lights it (block light 0 for the whole run; seen on the creepers-era base too). While light hasn't
		// arrived, re-place it every second.
		BlockPos lamp = helper.absolutePos(pos.east());
		helper.onEachTick(() -> {
			long t = helper.getTick();
			if (t >= 20 && t % 20 == 0 && helper.getLevel().getBrightness(LightLayer.BLOCK, lamp) == 0) {
				helper.setBlock(pos.east(), Blocks.AIR);
				helper.setBlock(pos.east(), Blocks.GLOWSTONE);
			}
		});
		helper.succeedWhen(() -> helper.assertTrue(
				Math.abs(sleeper.effectiveSensitivity(helper.getLevel()) - UndeadTuning.LIGHT_CALM_FACTOR) < 1.0E-4F,
				"lit sleeper sensitivity: " + sleeper.effectiveSensitivity(helper.getLevel()) + ", block light "
						+ helper.getLevel().getBrightness(LightLayer.BLOCK, helper.absolutePos(pos)) + ", at the lamp "
						+ helper.getLevel().getBrightness(LightLayer.BLOCK, lamp) + (helper.getTick() >= 598 ? lightEngineState(helper) : "")));
	}

	/**
	 * Why light never arrived, for the failure message: the light engine's own queue. Seen 2026-11-16 (before the test
	 * server's clock was frozen, not since): an update scheduled but never run, so the whole level's light stood still.
	 * Also dumps the worker threads to the log, to see what holds the light executor if it happens again.
	 */
	private static String lightEngineState(GameTestHelper helper) {
		StringBuilder threads = new StringBuilder();
		Thread.getAllStackTraces().forEach((thread, stack) -> {
			if (thread.getName().startsWith("Worker") || java.util.Arrays.stream(stack).anyMatch(f -> f.getClassName().contains("Light"))) {
				threads.append("\n THREAD ").append(thread.getName()).append(' ').append(thread.getState());
				for (int i = 0; i < Math.min(30, stack.length); i++) {
					threads.append("\n   at ").append(stack[i]);
				}
			}
		});
		System.out.println("LIGHT ENGINE THREADS" + threads);
		try {
			Object engine = helper.getLevel().getChunkSource().getLightEngine();
			java.lang.reflect.Field scheduled = net.minecraft.server.level.ThreadedLevelLightEngine.class.getDeclaredField("scheduled");
			scheduled.setAccessible(true);
			java.lang.reflect.Field tasks = net.minecraft.server.level.ThreadedLevelLightEngine.class.getDeclaredField("lightTasks");
			tasks.setAccessible(true);
			return "; light engine: update scheduled " + scheduled.get(engine) + ", queued tasks " + ((java.util.List<?>) tasks.get(engine)).size()
					+ " (threads in the log)";
		} catch (ReflectiveOperationException e) {
			return "; light engine: " + e;
		}
	}

	/**
	 * A bell rung 14 blocks away (beyond the 12 where ordinary noise fades out) wakes a sleeper; one 23 blocks away
	 * only stirs (the bell's own linear falloff, 32 blocks), and the bell's plain vibration is not counted twice.
	 */
	@GameTest(environment = "bubbas_reworked_mobs-test:warding_bell", structure = "bubbas_reworked_mobs-test:hearing_field", maxTicks = 20)
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
	 * Core (the contract other mods rely on). The api/Sleepers hook: calm takes points off sleepers in range only; keepAsleep zeroes the sensitivity of
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
