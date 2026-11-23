// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.test;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import io.github.bubba8587.bubbas_totems_artifacts.statue.StatueEffects;
import io.github.bubba8587.bubbas_totems_artifacts.statue.TotemType;
import io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The Totem of the Beginning in a beacon and as a statue: the area pulse reverts a dug-out hole directly. Milestone;
 * needs -PwithMobs (and -PwithBeacons for the beacon), passes at once otherwise. Each test calls the source on every
 * tick for a whole rhythm (a fresh Runnable each tick: the helper keys scheduled runs by Runnable), so every pulse of
 * one whole window runs.
 */
public class BeginningAreaGameTest {
	private static final String FIELD = TestSupport.FIELD;

	/** Beacon: the area pulses revert most of a dug hole; ore spots only ever get ore. */
	@GameTest(structure = FIELD, maxTicks = 600)
	public void beginningBeaconRevertsAHole(GameTestHelper helper) {
		if (!TestSupport.withMobs() || !FabricLoader.getInstance().isModLoaded("bubbas_reworked_beacons")) {
			helper.succeed();
			return;
		}
		run(helper, 5, () -> WithBeacons.pulse(helper, helper.absolutePos(new BlockPos(12, 1, 8))));
	}

	/** Statue: the same within the statue's radius. */
	@GameTest(structure = FIELD, maxTicks = 600)
	public void beginningStatueRevertsAHole(GameTestHelper helper) {
		if (!TestSupport.withMobs()) {
			helper.succeed();
			return;
		}
		helper.assertTrue(StatueEffects.pulses(TotemType.BEGINNING), "a Beginning statue doesn't pulse");
		BlockPos at = helper.absolutePos(new BlockPos(12, 1, 8));
		run(helper, Math.min(2, TotemsTuning.STATUE_RADIUS - 1), () -> StatueEffects.pulse(helper.getLevel(), at, TotemType.BEGINNING));
	}

	private static void run(GameTestHelper helper, int r, Runnable source) {
		Runnable undo = WithMobs.field(helper);
		int period = TotemsTuning.BEGINNING_AREA_PULSE_TICKS;
		for (int t = 1; t <= period + 80; t++) {
			helper.runAfterDelay(t, () -> source.run());
		}
		helper.runAfterDelay(period + 110, () -> {
			try {
				int cells = 0;
				int filled = 0;
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						if (dx * dx + dz * dz > r * r) {
							continue;
						}
						for (int y : new int[] {0, -1}) {
							cells++;
							filled += helper.getBlockState(new BlockPos(12 + dx, y, 8 + dz)).isAir() ? 0 : 1;
						}
						BlockState ore = helper.getBlockState(new BlockPos(12 + dx, -2, 8 + dz));
						helper.assertTrue(ore.isAir() || ore.is(Blocks.IRON_ORE), "an ore spot became " + ore);
					}
				}
				helper.assertTrue(filled * 2 > cells, "the pulses filled only " + filled + " of " + cells + " dug cells");
			} finally {
				undo.run();
			}
			helper.succeed();
		});
	}

	/** Touches Reworked Mobs internals; loaded only when that mod is installed. */
	private static final class WithMobs {
		/** Generated grass at the floor, iron ore two under it, stone elsewhere; real ground dug out 4 deep. */
		static Runnable field(GameTestHelper helper) {
			ServerLevel level = helper.getLevel();
			int floor = helper.absolutePos(BlockPos.ZERO).getY();
			BoundingBox box = BoundingBox.fromCorners(helper.absolutePos(new BlockPos(4, -8, 0)), helper.absolutePos(new BlockPos(20, 9, 16)));
			Runnable undo = io.github.bubba8587.bubbas_reworked_mobs.enderman.TargetWorld.overrideTerrain(level, box,
					pos -> pos.getY() > floor ? Blocks.AIR.defaultBlockState() : pos.getY() == floor ? Blocks.GRASS_BLOCK.defaultBlockState()
							: pos.getY() == floor - 2 ? Blocks.IRON_ORE.defaultBlockState() : Blocks.STONE.defaultBlockState(),
					(x, z) -> floor);
			for (int x = 5; x <= 19; x++) {
				for (int z = 1; z <= 15; z++) {
					for (int y = -7; y <= 8; y++) {
						helper.setBlock(new BlockPos(x, y, z), y > -4 ? Blocks.AIR : Blocks.STONE);
					}
				}
			}
			return undo;
		}
	}

	/** Touches the beacons API; loaded only when that mod is installed. */
	private static final class WithBeacons {
		static void pulse(GameTestHelper helper, BlockPos beacon) {
			ServerLevel level = helper.getLevel();
			var payload = new io.github.bubba8587.bubbas_totems_artifacts.compat.BeginningBeaconPayload();
			ItemStack stack = new ItemStack(ModItems.TOTEM_OF_THE_BEGINNING);
			var context = new io.github.bubba8587.bubbas_reworked_beacons.api.BeaconContext(level, beacon,
					io.github.bubba8587.bubbas_reworked_beacons.api.BeaconMode.SKY, 1, payload.power(stack), new AABB(beacon).inflate(7));
			payload.apply(level, stack, context);
		}
	}
}
