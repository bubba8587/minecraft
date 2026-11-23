// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadTuning;
import net.minecraft.gametest.framework.GameTestHelper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Core suite (test-lite, specs/quality.md "Test suites"): the smoke tests that run on every build and push, one per
 * main mechanic. Each one runs a test whose body lives with its siblings in a milestone class (the method there has no
 * {@code @GameTest} of its own, so the full suite runs it once); the annotation (environment, structure, ticks) is the
 * one it had there. Everything else in this source set is the milestone suite, registered only with -PmilestoneTests.
 * Objectives: specs/undead.md, specs/creepers.md, specs/endermen.md and specs/drowned.md, "Milestone test objectives".
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

	@GameTest(environment = "bubbas_reworked_mobs-test:warding_light", structure = "bubbas_reworked_mobs-test:hearing_field", maxTicks = 20)
	public void strongLightCalmsSleepers(GameTestHelper helper) {
		// The rule itself (block light 12 or more: x0.7). The in-world version (a glowstone next to a sleeper) is the
		// milestone SleeperNoiseGameTest.strongLightCalmsSleepersInWorld: the test server's light engine now and then
		// never lights a block placed in a fresh test chunk, which made this core test fail about one build in five.
		helper.assertTrue(io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity.lightCalm(UndeadTuning.LIGHT_CALM_LEVEL) == UndeadTuning.LIGHT_CALM_FACTOR, "light at the calm level should calm");
		helper.assertTrue(io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity.lightCalm(UndeadTuning.LIGHT_CALM_LEVEL - 1) == 1.0F, "light below it should not");
		helper.assertTrue(io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper.SleeperBlockEntity.lightCalm(0) == 1.0F, "dark should not");
		new SleeperCalmGameTest().darkSleeperAtFullSensitivity(helper);
		helper.succeed();
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

	// --- endermen (specs/endermen.md) ---

	@GameTest(maxTicks = 40)
	public void endermanTerrainOracleAnswersFlatLayersAndCairns(GameTestHelper helper) {
		new EndermanGameTest().terrainOracleAnswersFlatLayersAndCairns(helper);
	}

	@GameTest
	public void endermanCairnDesignsVaryAndUseOnlyEndBlocks(GameTestHelper helper) {
		new EndermanGameTest().cairnDesignsVaryAndUseOnlyEndBlocks(helper);
	}

	@GameTest(environment = "bubbas_reworked_mobs-test:enderman_take", structure = EndermanGameTest.FIELD, maxTicks = 400)
	public void endermanTakesStrayBlockAndFillsHole(GameTestHelper helper) {
		new EndermanGameTest().endermanTakesStrayBlockAndFillsHole(helper);
	}

	@GameTest(environment = "bubbas_reworked_mobs-test:enderman_build", structure = EndermanGameTest.FIELD, maxTicks = 400)
	public void endermanBuilderDeliversCairnBlock(GameTestHelper helper) {
		new EndermanGameTest().builderEndermanDeliversCairnBlock(helper);
	}

	@GameTest(environment = "bubbas_reworked_mobs-test:enderman_stare", structure = EndermanGameTest.FIELD, maxTicks = 20)
	public void endermanBusyIgnoresEyeContact(GameTestHelper helper) {
		new EndermanGameTest().busyEndermanIgnoresEyeContact(helper);
	}

	@GameTest(structure = EndermanGameTest.FIELD, maxTicks = 100)
	public void endermanRippleRestoresGroundButNotBuilds(GameTestHelper helper) {
		new EndermanTotemsGameTest().rippleRestoresGroundButNotBuilds(helper);
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

	@GameTest(environment = "bubbas_reworked_mobs-test:drowned_grip", structure = DrownedGameTest.FIELD, maxTicks = 100)
	public void drownedGripLoosensWithHitsAndTightensWhileDragging(GameTestHelper helper) {
		new DrownedGameTest().gripLoosensWithHitsAndTightensWhileDragging(helper);
	}

	@GameTest(environment = "bubbas_reworked_mobs-test:drowned_join", structure = DrownedGameTest.FIELD, maxTicks = 300)
	public void drownedSecondDrownedJoinsAndHoldsHarder(GameTestHelper helper) {
		new DrownedGameTest().secondDrownedJoinsAndHoldsHarder(helper);
	}

	@GameTest(environment = "bubbas_reworked_mobs-test:drowned_stare", structure = DrownedGameTest.FIELD, maxTicks = 120)
	public void drownedStaredAtItSinks(GameTestHelper helper) {
		new DrownedGameTest().staredAtItSinks(helper);
	}

	// --- Ancient Fabricators (specs/mob-factory.md); bodies in MobFactoryGameTest ---

	@GameTest(maxTicks = MobFactoryGameTest.SETTLE + 150)
	public void fabricatorDropsDeactivatedMob(GameTestHelper helper) {
		new MobFactoryGameTest().dropsDeactivatedMobOntoThePad(helper);
	}

	@GameTest(maxTicks = MobFactoryGameTest.SETTLE + MobFactoryGameTest.COUNT + 150)
	public void fabricatorSendsItUpAndItWakesReworked(GameTestHelper helper) {
		new MobFactoryGameTest().teleporterSendsItUpAndItWakesReworked(helper);
	}

	@GameTest(maxTicks = MobFactoryGameTest.SETTLE + MobFactoryGameTest.COUNT + 100)
	public void fabricatorRedstoneStopsTheTeleporter(GameTestHelper helper) {
		new MobFactoryGameTest().redstoneStopsTheTeleporter(helper);
	}

	@GameTest(maxTicks = MobFactoryGameTest.SETTLE + MobFactoryGameTest.COUNT + 120)
	public void fabricatorBreakingTheSpawnerStopsIt(GameTestHelper helper) {
		new MobFactoryGameTest().breakingTheHiddenSpawnerStopsItForGood(helper);
	}

	// --- monster camps (specs/monster-camps.md) ---

	@GameTest(structure = CampsGameTest.FIELD, maxTicks = 80)
	public void campRosterAppearsWithALeader(GameTestHelper helper) {
		new CampsGameTest().rosterAppearsWithALeader(helper);
	}
}
