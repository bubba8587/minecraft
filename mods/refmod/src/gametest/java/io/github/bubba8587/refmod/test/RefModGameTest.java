package io.github.bubba8587.refmod.test;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

public class RefModGameTest {
	@GameTest
	public void placesStone(GameTestHelper helper) {
		helper.setBlock(1, 1, 1, Blocks.STONE);
		helper.assertBlockPresent(Blocks.STONE, 1, 1, 1);
		helper.succeed();
	}
}
