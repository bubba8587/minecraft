// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import net.minecraft.gametest.framework.GameTestHelper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Core suite (test-lite, specs/quality.md "Test suites"): the smoke tests that run on every build and push, one per
 * main mechanic, plus the api/Sleepers contract Bubba's Totems & Artifacts relies on. Each one runs a test whose body
 * lives with its siblings in a milestone class (the method there has no {@code @GameTest} of its own, so the full
 * suite runs it once); the annotation (environment, structure, ticks) is the one it had there. Everything else in this
 * source set is the milestone suite, registered only with -PmilestoneTests. Objectives: the "Milestone test objectives"
 * of specs/undead.md, creepers.md, endermen.md, drowned.md, mob-factory.md, graveyards.md, spiders.md and
 * monster-camps.md.
 */
public class ReworkedMobsCoreGameTest {
	// --- sleepers and waking (specs/undead.md) ---

	@GameTest(environment = "bubbas_reworked_mobs-test:hearing_walk", maxTicks = 100)
	public void walkingNearbyWakesSleeper(GameTestHelper helper) {
		new BubbasReworkedMobsGameTest().walkingNearbyWakesSleeper(helper);
	}

	/** The API contract another mod relies on (Totem of Warding, Grave Talisman). */
	@GameTest(environment = "bubbas_reworked_mobs-test:warding_bell", structure = "bubbas_reworked_mobs-test:hearing_field", maxTicks = 20)
	public void apiCalmsSensesAndKeepsSleepersAsleep(GameTestHelper helper) {
		new SleeperCalmGameTest().apiCalmsSensesAndKeepsSleepersAsleep(helper);
	}

	// --- loot on display (specs/undead.md) ---

	@GameTest(environment = "bubbas_reworked_mobs-test:grab_loot", maxTicks = 40)
	public void wakePrefersAWeaponOverACloserOtherItem(GameTestHelper helper) {
		new GrabLootGameTest().wakePrefersAWeaponOverACloserOtherItem(helper);
	}

	// --- undead variants (specs/undead.md) ---

	@GameTest(maxTicks = 40)
	public void huskBurstsOutOfSandAndBlinds(GameTestHelper helper) {
		new UndeadVariantsGameTest().huskBurstsOutOfSandAndBlinds(helper);
	}

	// --- creepers (specs/creepers.md) ---

	@GameTest(environment = "bubbas_reworked_mobs-test:creeper_speed", structure = CreeperGameTest.FIELD, maxTicks = 140)
	public void creeperSprintingNearbyStartlesWalkingDoesNot(GameTestHelper helper) {
		new CreeperGameTest().sprintingNearbyStartlesWalkingDoesNot(helper);
	}

	// --- endermen and cairns (specs/endermen.md) ---

	@GameTest(environment = "bubbas_reworked_mobs-test:enderman_build", structure = EndermanGameTest.FIELD, maxTicks = 400)
	public void endermanBuilderDeliversCairnBlock(GameTestHelper helper) {
		new EndermanGameTest().builderEndermanDeliversCairnBlock(helper);
	}

	@GameTest(structure = EndermanGameTest.FIELD, maxTicks = 20)
	public void cairnTradePadTakesAndAnswers(GameTestHelper helper) {
		new CairnTradeGameTest().tradePadTakesAndAnswers(helper);
	}

	// --- graveyards (specs/graveyards.md) ---

	@GameTest(structure = EndermanGameTest.FIELD, maxTicks = 20)
	public void graveyardStonesWithoutNamesAndSleepers(GameTestHelper helper) {
		new GravesGameTest().yardHasStonesWithoutNamesAndSleepers(helper);
	}

	// --- drowned (specs/drowned.md) ---

	@GameTest(environment = "bubbas_reworked_mobs-test:drowned_grab", structure = DrownedGameTest.FIELD, maxTicks = 300)
	public void drownedGrabsAndDragsToDeepWater(GameTestHelper helper) {
		new DrownedGameTest().grabsAndDragsToDeepWater(helper);
	}

	// --- Ancient Fabricators (specs/mob-factory.md); body in MobFactoryGameTest ---

	@GameTest(maxTicks = MobFactoryGameTest.SETTLE + MobFactoryGameTest.COUNT + 150)
	public void fabricatorSendsItUpAndItWakesReworked(GameTestHelper helper) {
		new MobFactoryGameTest().teleporterSendsItUpAndItWakesReworked(helper);
	}

	// --- spiders (specs/spiders.md) ---

	@GameTest(environment = SpiderGameTest.ENV, structure = SpiderGameTest.ROOM, maxTicks = 300)
	public void spiderSleeperDropsLatchesAndReelsUp(GameTestHelper helper) {
		new SpiderGameTest().sleeperDropsLatchesAndReelsUp(helper);
	}

	@GameTest(environment = SpiderGameTest.ENV, structure = SpiderGameTest.ROOM)
	public void spunWebHitsOnlyTheStrands(GameTestHelper helper) {
		new GiantSpiderGameTest().webHitsOnlyTheStrands(helper);
	}

	// --- monster camps (specs/monster-camps.md) ---

	@GameTest(structure = CampsGameTest.FIELD, maxTicks = 80)
	public void campRosterAppearsWithALeader(GameTestHelper helper) {
		new CampsGameTest().rosterAppearsWithALeader(helper);
	}
}
