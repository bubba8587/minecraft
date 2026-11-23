// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.test;

import net.minecraft.gametest.framework.GameTestHelper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Core suite (test-lite, specs/quality.md "Test suites"): the smoke tests that run on every build and push, one per
 * main mechanic. Each runs a test whose body lives with its siblings in a milestone class (no {@code @GameTest} of its
 * own there, so the full suite runs it once). Everything else is milestone, registered only with -PmilestoneTests.
 * The sleeper tests need Bubba's Reworked Mobs (-PwithMobs) and pass at once without it.
 * Objectives: specs/totems-artifacts.md, "Milestone test objectives".
 */
public class BubbasTotemsArtifactsCoreGameTest {
	@GameTest
	public void sneakPlacesStatueAndBreakingGivesTotemBack(GameTestHelper helper) {
		new TotemStatueGameTest().sneakPlacesStatueAndBreakingGivesTotemBack(helper);
	}

	@GameTest(environment = "bubbas_totems_artifacts-test:talisman", structure = TestSupport.LANE, maxTicks = 40)
	public void sensesUndeadMobs(GameTestHelper helper) {
		new GraveTalismanGameTest().sensesUndeadMobs(helper);
	}

	@GameTest(environment = "bubbas_totems_artifacts-test:warding_pulse", structure = TestSupport.FIELD, maxTicks = 60)
	public void heldTotemPulsesAndScaresMonsters(GameTestHelper helper) {
		new WardingGameTest().heldTotemPulsesAndScaresMonsters(helper);
	}

	@GameTest(environment = "bubbas_totems_artifacts-test:warding_ward", structure = TestSupport.FIELD, maxTicks = 300)
	public void wardStopsSpawnsAndSendsMonstersOut(GameTestHelper helper) {
		new WardingGameTest().wardStopsSpawnsAndSendsMonstersOut(helper);
	}

	// --- the five totems of 2026-11-16 (bodies in TotemsGameTest) ---

	@GameTest(structure = TestSupport.FIELD, maxTicks = 20)
	public void keepingKeepsEverythingOnce(GameTestHelper helper) {
		new TotemsGameTest().keepingKeepsEverythingOnce(helper);
	}

	@GameTest(structure = TestSupport.FIELD, maxTicks = 100)
	public void gatheringPullsNearbyItems(GameTestHelper helper) {
		new TotemsGameTest().gatheringPullsNearbyItems(helper);
	}

	@GameTest(structure = TestSupport.FIELD, maxTicks = 20)
	public void fortificationStopsBlockDamage(GameTestHelper helper) {
		new TotemsGameTest().fortificationStopsBlockDamage(helper);
	}

	@GameTest(structure = TestSupport.FIELD, maxTicks = 20)
	public void growthSpeedsUpCrops(GameTestHelper helper) {
		new TotemsGameTest().growthSpeedsUpCrops(helper);
	}

	@GameTest(structure = TestSupport.FIELD, maxTicks = 20)
	public void seekingChoosesAndPoints(GameTestHelper helper) {
		new TotemsGameTest().seekingChoosesAndPoints(helper);
	}

	@GameTest(structure = TestSupport.FIELD, maxTicks = 20)
	public void endLandingRules(GameTestHelper helper) {
		new BeginningEndGameTest().endLandingRules(helper);
	}

	// --- 2026-11-22: Gathering ring, Growth planting, Herding, Clear Skies, statues (bodies in NewTotemsGameTest) ---

	@GameTest(structure = TestSupport.FIELD, maxTicks = 300)
	public void gatheringRingFeedsASideHopper(GameTestHelper helper) {
		new NewTotemsGameTest().gatheringRingFeedsASideHopper(helper);
	}

	@GameTest(structure = TestSupport.FIELD, maxTicks = 20)
	public void herdingPastureKeepsLivestockIn(GameTestHelper helper) {
		new NewTotemsGameTest().herdingPastureKeepsLivestockIn(helper);
	}

	@GameTest(structure = TestSupport.FIELD, maxTicks = 20)
	public void clearSkiesKeepsRainOff(GameTestHelper helper) {
		new NewTotemsGameTest().clearSkiesKeepsRainOff(helper);
	}

	@GameTest(structure = TestSupport.FIELD, maxTicks = 40)
	public void statuesPulseTheirArea(GameTestHelper helper) {
		new NewTotemsGameTest().statuesPulseTheirArea(helper);
	}

	// --- relic maps and vaults (bodies in RelicMapsGameTest) ---

	@GameTest
	public void relicShredsCombineOnlySameNameAndSize(GameTestHelper helper) {
		new RelicMapsGameTest().shredsCombineOnlySameNameAndSize(helper);
	}

	@GameTest(structure = TestSupport.NS + ":crypt_field", maxTicks = 20)
	public void relicVaultBuildsSealedWithPartwayStair(GameTestHelper helper) {
		new RelicMapsGameTest().vaultBuildsSealedWithPartwayStair(helper);
	}
}
