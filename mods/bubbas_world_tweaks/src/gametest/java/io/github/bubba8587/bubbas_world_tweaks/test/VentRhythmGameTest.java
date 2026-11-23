// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.test;

import io.github.bubba8587.bubbas_world_tweaks.lava.LavaGeysers;
import net.minecraft.core.BlockPos;
import io.github.bubba8587.bubbas_world_tweaks.lava.LavaGeyser;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Every magma block under lava is a vent with a steady rhythm of its own, slower under deeper lava. */
public class VentRhythmGameTest {
	/** {gap between eruptions in check steps, 1 if every gap was the same}. */
	private static int[] rhythm(long seed, BlockPos magma, int depth) {
		long last = -1;
		int gap = -1;
		boolean steady = true;
		for (long step = 0; step < 3000; step++) {
			if (LavaGeysers.eruptsAt(seed, magma, depth, step)) {
				if (last >= 0) {
					int g = (int) (step - last);
					steady &= gap < 0 || g == gap;
					gap = g;
				}
				last = step;
			}
		}
		return new int[] {gap, steady ? 1 : 0};
	}

	@GameTest
	public void everyVentKeepsItsOwnSteadyRhythm(GameTestHelper helper) {
		long seed = helper.getLevel().getSeed();
		java.util.Set<Integer> gaps = new java.util.HashSet<>();
		for (int i = 0; i < 20; i++) {
			BlockPos magma = new BlockPos(i * 3, 30, i * 7);
			int[] shallow = rhythm(seed, magma, 1);
			int[] deep = rhythm(seed, magma, 4);
			helper.assertTrue(shallow[1] == 1 && deep[1] == 1, "uneven eruptions at " + magma);
			helper.assertTrue(shallow[0] >= 20 && shallow[0] <= 50, "vent at " + magma + " erupts every " + shallow[0] * 2 + " s, expected 40-100 s");
			helper.assertTrue(deep[0] > shallow[0], "deeper lava doesn't slow the vent at " + magma);
			gaps.add(shallow[0]);
		}
		helper.assertTrue(gaps.size() > 3, "vents all share one rhythm");
		helper.succeed();
	}

	/** A real pool: a single magma block under two lava erupts within one of its cycles, through the tick path's check. */
	@GameTest(maxTicks = 40)
	public void aLoneMagmaBlockErupts(GameTestHelper helper) {
		for (int x = 1; x <= 3; x++) {
			for (int z = 1; z <= 3; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
				helper.setBlock(new BlockPos(x, 1, z), Blocks.LAVA);
				helper.setBlock(new BlockPos(x, 2, z), Blocks.LAVA);
			}
		}
		helper.setBlock(new BlockPos(2, 0, 2), Blocks.MAGMA_BLOCK);
		BlockPos surface = helper.absolutePos(new BlockPos(2, 2, 2));
		var player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.snapTo(surface.getX() + 3.5, surface.getY() + 1.0, surface.getZ() + 0.5, 0.0F, 0.0F);
		AABB around = new AABB(surface).inflate(1.0, 2.0, 1.0);
		for (long step = 0; step < 60; step++) {
			LavaGeysers.checkAround(helper.getLevel(), player, step);
			if (!helper.getLevel().getEntitiesOfClass(LavaGeyser.class, around).isEmpty()) {
				helper.succeed();
				return;
			}
		}
		helper.fail("no eruption from the magma block in 60 checks (120 s)");
	}
}
