package io.github.bubba8587.bubbas_reworked_beacons.test;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

// Core server game tests (test-lite, specs/quality.md "Test suites"): run by ./gradlew :bubbas_reworked_beacons:build. Keep this
// to a small smoke set, one test per main mechanic; everything else goes in milestone classes listed under
// "bubba-milestone-gametest" in fabric.mod.json, with one-line objectives in the mod's spec.
public class BubbasReworkedBeaconsCoreGameTest {
	@GameTest
	public void worldWorks(GameTestHelper helper) {
		helper.setBlock(1, 1, 1, Blocks.STONE);
		helper.assertBlockPresent(Blocks.STONE, 1, 1, 1);
		helper.succeed();
	}
}
