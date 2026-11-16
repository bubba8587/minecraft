// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import net.minecraft.gametest.framework.GameTestHelper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Core suite (test-lite, specs/quality.md "Test suites"): the smoke tests that run on every build and push, one per
 * main mechanic. Each one runs a test whose body lives with its siblings in a milestone class (the method there has no
 * {@code @GameTest} of its own, so the full suite runs it once); the annotation (environment, structure, ticks) is the
 * one it had there. Everything else in this source set is the milestone suite, registered only with -PmilestoneTests.
 * Objectives: specs/undead.md and specs/creepers.md, "Milestone test objectives".
 */
public class ReworkedMobsCoreGameTest {
	@GameTest
	public void placedSleeperItemKeepsGearPoseAndFacesPlacer(GameTestHelper helper) {
		new BubbasReworkedMobsGameTest().placedSleeperItemKeepsGearPoseAndFacesPlacer(helper);
	}

	@GameTest(environment = "bubbas_reworked_mobs-test:hearing_walk", maxTicks = 100)
	public void walkingNearbyWakesSleeper(GameTestHelper helper) {
		new BubbasReworkedMobsGameTest().walkingNearbyWakesSleeper(helper);
	}

	@GameTest(maxTicks = 40)
	public void wakeSpawnsTunedMobWithGearFacingTheSource(GameTestHelper helper) {
		new BubbasReworkedMobsGameTest().wakeSpawnsTunedMobWithGearFacingTheSource(helper);
	}

	@GameTest
	public void miningWithSilkTouchPickaxeGivesSleeperItemAndDropsGear(GameTestHelper helper) {
		new BubbasReworkedMobsGameTest().miningWithSilkTouchPickaxeGivesSleeperItemAndDropsGear(helper);
	}

	@GameTest
	public void emptyHandTakesGroundWallAndStuck(GameTestHelper helper) {
		new LootDisplayGameTest().emptyHandTakesGroundWallAndStuck(helper);
	}

	@GameTest(environment = "bubbas_reworked_mobs-test:grab_loot", maxTicks = 40)
	public void wakePrefersAWeaponOverACloserOtherItem(GameTestHelper helper) {
		new GrabLootGameTest().wakePrefersAWeaponOverACloserOtherItem(helper);
	}

	@GameTest(maxTicks = 40)
	public void zombieHeadshotsHurtMore(GameTestHelper helper) {
		new UndeadCombatGameTest().zombieHeadshotsHurtMore(helper);
	}

	@GameTest(environment = "bubbas_reworked_mobs-test:warding_light", structure = "bubbas_reworked_mobs-test:hearing_field", maxTicks = 600)
	public void strongLightCalmsSleepers(GameTestHelper helper) {
		new SleeperCalmGameTest().strongLightCalmsSleepers(helper);
	}

	@GameTest(environment = "bubbas_reworked_mobs-test:warding_bell", structure = "bubbas_reworked_mobs-test:hearing_field", maxTicks = 20)
	public void bellsAreLoudToSleepers(GameTestHelper helper) {
		new SleeperCalmGameTest().bellsAreLoudToSleepers(helper);
	}

	@GameTest(environment = "bubbas_reworked_mobs-test:warding_bell", structure = "bubbas_reworked_mobs-test:hearing_field", maxTicks = 20)
	public void apiCalmsSensesAndKeepsSleepersAsleep(GameTestHelper helper) {
		new SleeperCalmGameTest().apiCalmsSensesAndKeepsSleepersAsleep(helper);
	}

	// --- creepers (specs/creepers.md) ---

	@GameTest(environment = "bubbas_reworked_mobs-test:creeper_meter", structure = CreeperGameTest.FIELD, maxTicks = 140)
	public void creeperHitsFillTheMeterAndItDrains(GameTestHelper helper) {
		new CreeperGameTest().hitsFillTheMeterAndItDrains(helper);
	}

	@GameTest(environment = "bubbas_reworked_mobs-test:creeper_speed", structure = CreeperGameTest.FIELD, maxTicks = 140)
	public void creeperSprintingNearbyStartlesWalkingDoesNot(GameTestHelper helper) {
		new CreeperGameTest().sprintingNearbyStartlesWalkingDoesNot(helper);
	}

	@GameTest(environment = "bubbas_reworked_mobs-test:creeper_fuse", structure = CreeperGameTest.FIELD, maxTicks = 80)
	public void creeperFullMeterHissesAndDefuses(GameTestHelper helper) {
		new CreeperGameTest().fullMeterHissesAndDefuses(helper);
	}

	@GameTest(environment = "bubbas_reworked_mobs-test:creeper_blind", structure = CreeperGameTest.FIELD, maxTicks = 80)
	public void blindedCreeperIgnoresMotion(GameTestHelper helper) {
		new CreeperGameTest().blindedCreeperIgnoresMotion(helper);
	}

	@GameTest(environment = "bubbas_reworked_mobs-test:creeper_charged", structure = CreeperGameTest.FIELD, maxTicks = 100)
	public void chargedCreeperStaysVanilla(GameTestHelper helper) {
		new CreeperGameTest().chargedCreeperStaysVanilla(helper);
	}
}