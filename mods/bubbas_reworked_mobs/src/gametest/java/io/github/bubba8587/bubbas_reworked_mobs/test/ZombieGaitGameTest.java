// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModEntities;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadTuning;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * The shamble's real motion (specs/undead.md "Tuning", walk animations): a woken zombie chasing with its own AI moves
 * in pulses (a long, slower drag, then a quicker step) but covers the same ground as a zombie of the same speed walking steadily. The
 * steady reference is a vanilla zombie with our zombie's speed, chasing in the next lane.
 */
public class ZombieGaitGameTest {
	private static final String LANE = "bubbas_reworked_mobs-test:drift_lane";
	/** Start measuring once both are up to speed, for this many whole cycles. */
	private static final int START = 30;
	private static final int CYCLES = 5;

	private static Mob helmeted(Mob mob) {
		mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET)); // no burning
		mob.setPersistenceRequired();
		return mob;
	}

	private static Mob pig(GameTestHelper helper, BlockPos pos) {
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, pos);
		pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
		pig.setHealth(1000.0F);
		return pig;
	}

	@GameTest(environment = "bubbas_reworked_mobs-test:gait_pulse", structure = LANE, maxTicks = 300)
	public void wokenZombieDragsAndStepsAtTheSameAverageSpeed(GameTestHelper helper) {
		Mob ours = helmeted(helper.spawn(ModEntities.woken(UndeadVariant.ZOMBIE), new BlockPos(1, 1, 1)));
		Zombie steady = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 1, 3));
		helmeted(steady);
		steady.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(UndeadTuning.ZOMBIE_SPEED);
		steady.setBaby(false);
		ours.setTarget(pig(helper, new BlockPos(18, 1, 1)));
		steady.setTarget(pig(helper, new BlockPos(18, 1, 3)));
		int ticks = CYCLES * UndeadTuning.GAIT_PERIOD_TICKS;
		double[] step = new double[ticks];
		double[] last = new double[4];
		helper.runAtTickTime(START, () -> {
			last[0] = ours.getX();
			last[1] = steady.getX();
			last[2] = ours.getX();
			last[3] = steady.getX();
		});
		for (int t = 0; t < ticks; t++) {
			int i = t;
			helper.runAtTickTime(START + 1 + t, () -> {
				step[i] = ours.getX() - last[0];
				last[0] = ours.getX();
			});
		}
		helper.runAtTickTime(START + 1 + ticks, () -> {
			double oursAvg = (ours.getX() - last[2]) / ticks;
			double steadyAvg = (steady.getX() - last[3]) / ticks;
			double min = Double.MAX_VALUE;
			double max = 0.0;
			int slow = 0;
			for (double s : step) {
				min = Math.min(min, s);
				max = Math.max(max, s);
				if (s < 0.97 * oursAvg) {
					slow++;
				}
			}
			StringBuilder trace = new StringBuilder();
			for (int t = 0; t < UndeadTuning.GAIT_PERIOD_TICKS; t++) {
				trace.append("%.3f ".formatted(step[t]));
			}
			String info = "ours %.4f/tick, steady %.4f/tick, step min %.4f max %.4f, %d slow ticks; first cycle: %s"
					.formatted(oursAvg, steadyAvg, min, max, slow, trace);
			System.out.println("[gait] " + info);
			helper.assertTrue(steadyAvg > 0.01, "the steady zombie didn't walk: " + info);
			helper.assertValueInBetween(0.85, oursAvg / steadyAvg, 1.15, "average speed vs steady (" + info + ")");
			helper.assertTrue(min < 0.95 * oursAvg, "no slower drag ticks: " + info);
			helper.assertTrue(max > 1.25 * oursAvg, "no faster step ticks: " + info);
			helper.assertTrue(slow >= CYCLES * UndeadTuning.GAIT_PERIOD_TICKS / 2, "the drag should be most of the cycle: " + info);
			helper.succeed();
		});
	}
}
