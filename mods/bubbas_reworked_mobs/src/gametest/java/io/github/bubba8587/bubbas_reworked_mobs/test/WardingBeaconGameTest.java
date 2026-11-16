// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModItems;
import io.github.bubba8587.bubbas_reworked_mobs.warding.Wards;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The Totem of Warding in a Bubba's Reworked Beacons beacon: a pulse wards the beacon's area (no monster spawns, any
 * pyramid; sleepers kept asleep only with 4 layers). Without Reworked Beacons (the default test run) it passes at once;
 * run with -PwithBeacons -PmilestoneTests to test it for real.
 */
public class WardingBeaconGameTest {
	@GameTest(maxTicks = 20)
	public void totemInABeaconWardsItsArea(GameTestHelper helper) {
		if (!FabricLoader.getInstance().isModLoaded("bubbas_reworked_beacons")) {
			helper.succeed();
			return;
		}
		var level = helper.getLevel();
		BlockPos beacon = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockPos near = beacon.offset(15, 0, -12);
		BlockPos far = beacon.offset(45, 0, 0);
		WithBeacons.pulse(helper, beacon, 1);
		helper.assertTrue(WithBeacons.accepts(), "the payload doesn't accept the totem");
		helper.assertTrue(Wards.blocksNaturalSpawn(level, EntityTypes.ZOMBIE, near), "1-layer ward lets a zombie spawn 15 blocks out");
		helper.assertFalse(Wards.blocksNaturalSpawn(level, EntityTypes.ZOMBIE, far), "1-layer ward (range 20) reaches 45 blocks");
		helper.assertFalse(Wards.keepsSleepersAsleep(level, near), "1-layer ward keeps sleepers asleep");
		WithBeacons.pulse(helper, beacon, 4);
		helper.assertTrue(Wards.blocksNaturalSpawn(level, EntityTypes.ZOMBIE, far), "4-layer ward (range 50) misses 45 blocks out");
		helper.assertTrue(Wards.keepsSleepersAsleep(level, near), "4-layer ward doesn't keep sleepers asleep");
		Wards.remove(level, beacon);
		helper.succeed();
	}

	/** Touches the beacons API; loaded only when that mod is installed. */
	private static final class WithBeacons {
		static boolean accepts() {
			return new io.github.bubba8587.bubbas_reworked_mobs.compat.WardingBeaconPayload().accepts(new ItemStack(ModItems.TOTEM_OF_WARDING));
		}

		static void pulse(GameTestHelper helper, BlockPos beacon, int levels) {
			var level = helper.getLevel();
			var stack = new ItemStack(ModItems.TOTEM_OF_WARDING);
			var payload = new io.github.bubba8587.bubbas_reworked_mobs.compat.WardingBeaconPayload();
			var context = new io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext(level, beacon,
					io.github.bubba8587.bubbas_reworked_beacons.api.BeaconMode.SKY, levels, payload.power(stack),
					io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext.skyArea(level, beacon, levels));
			payload.apply(level, stack, context);
		}
	}
}
