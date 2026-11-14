package io.github.bubba8587.packcheck.test;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

// Server game tests: run by ./gradlew :packcheck:build. Replace this with tests of the mod's behavior.
public class PackcheckGameTest {
	@GameTest
	public void worldWorks(GameTestHelper helper) {
		helper.setBlock(1, 1, 1, Blocks.STONE);
		helper.assertBlockPresent(Blocks.STONE, 1, 1, 1);
		helper.succeed();
	}
}
