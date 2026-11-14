package io.github.bubba8587.bubbas_reworked_mobs.test;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

// Server game tests: run by ./gradlew :bubbas_reworked_mobs:build. Replace this with tests of the mod's behavior.
public class BubbasReworkedMobsGameTest {
	@GameTest
	public void worldWorks(GameTestHelper helper) {
		helper.setBlock(1, 1, 1, Blocks.STONE);
		helper.assertBlockPresent(Blocks.STONE, 1, 1, 1);
		helper.succeed();
	}
}
