// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Floors for the plain (structure-less) 8 x 8 test areas, shared by the test classes. */
final class TestFloors {
	private TestFloors() {
	}

	/** An 8 x 8 floor of {@code block} at y 0. */
	static void floor(GameTestHelper helper, Block block) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(x, 0, z, block);
			}
		}
	}

	/** An 8 x 8 stone floor at y 0. */
	static void stone(GameTestHelper helper) {
		floor(helper, Blocks.STONE);
	}
}
