// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.test;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Core suite (test-lite, specs/quality.md "Test suites"): the smoke tests that run on every build and push, one per
 * main mechanic. Each runs a test whose body lives with its siblings in a milestone class (no {@code @GameTest} of its
 * own there, so the full suite runs it once). Everything else is milestone, registered only with -PmilestoneTests.
 * The sleeper tests need Bubba's Reworked Mobs (-PwithMobs) and pass at once without it.
 * Objectives: specs/totems-artifacts.md, "Milestone test objectives".
 */
public class BubbasTotemsArtifactsCoreGameTest {
	/**
	 * Saved worlds: items saved under their old Bubba's Reworked Mobs ids load as this mod's items (Fabric registry
	 * aliases), both by registry lookup and through the item stack codec a world save uses.
	 */
	@GameTest
	public void oldReworkedMobsIdsLoadAsTheseItems(GameTestHelper helper) {
		check(helper, "grave_talisman", ModItems.GRAVE_TALISMAN);
		check(helper, "totem_of_warding", ModItems.TOTEM_OF_WARDING);
		helper.succeed();
	}

	private static void check(GameTestHelper helper, String name, Item expected) {
		Identifier old = Identifier.fromNamespaceAndPath(ModItems.OLD_NAMESPACE, name);
		helper.assertTrue(BuiltInRegistries.ITEM.getValue(old) == expected, old + " doesn't look up as the new item");
		CompoundTag saved = new CompoundTag();
		saved.putString("id", old.toString());
		saved.putInt("count", 1);
		ItemStack stack = ItemStack.CODEC.parse(helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), saved)
				.getOrThrow(e -> new AssertionError("old " + name + " stack didn't load: " + e));
		helper.assertTrue(stack.is(expected), "old " + name + " stack loaded as " + stack);
		// Saved again, it is written under the new id.
		var written = ItemStack.CODEC.encodeStart(helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
		helper.assertTrue(written instanceof CompoundTag tag && tag.getStringOr("id", "").equals("bubbas_totems_artifacts:" + name),
				"re-saved " + name + " as " + written);
	}

	@GameTest
	public void sneakPlacesStatueAndBreakingGivesTotemBack(GameTestHelper helper) {
		new TotemStatueGameTest().sneakPlacesStatueAndBreakingGivesTotemBack(helper);
	}

	@GameTest(environment = "bubbas_totems_artifacts-test:talisman", structure = TestSupport.LANE, maxTicks = 40)
	public void sensesUndeadMobs(GameTestHelper helper) {
		new GraveTalismanGameTest().sensesUndeadMobs(helper);
	}

	@GameTest(environment = "bubbas_totems_artifacts-test:talisman", structure = TestSupport.LANE, maxTicks = 40)
	public void sensesSleepers(GameTestHelper helper) {
		new GraveTalismanGameTest().sensesSleepers(helper);
	}

	@GameTest(environment = "bubbas_totems_artifacts-test:warding_pulse", structure = TestSupport.FIELD, maxTicks = 60)
	public void heldTotemPulsesAndScaresMonsters(GameTestHelper helper) {
		new WardingGameTest().heldTotemPulsesAndScaresMonsters(helper);
	}

	@GameTest(environment = "bubbas_totems_artifacts-test:warding_pulse", structure = TestSupport.FIELD, maxTicks = 20)
	public void pulseCalmsSleepers(GameTestHelper helper) {
		new WardingGameTest().pulseCalmsSleepers(helper);
	}

	@GameTest(environment = "bubbas_totems_artifacts-test:warding_ward", structure = TestSupport.FIELD, maxTicks = 300)
	public void wardStopsSpawnsAndSendsMonstersOut(GameTestHelper helper) {
		new WardingGameTest().wardStopsSpawnsAndSendsMonstersOut(helper);
	}

	@GameTest(environment = "bubbas_totems_artifacts-test:warding_ward", structure = TestSupport.FIELD, maxTicks = 20)
	public void fullWardKeepsSleepersAsleep(GameTestHelper helper) {
		new WardingGameTest().fullWardKeepsSleepersAsleep(helper);
	}

	@GameTest(environment = "bubbas_totems_artifacts-test:warding_barrier", structure = TestSupport.FIELD, maxTicks = 300)
	public void wardPushesBackMobsThatDoNotPath(GameTestHelper helper) {
		new WardingGameTest().wardPushesBackMobsThatDoNotPath(helper);
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

	@GameTest(structure = TestSupport.FIELD, maxTicks = 20)
	public void endStatuePlacesWithoutTeleportingAndDropsItsTotem(GameTestHelper helper) {
		new TotemStatueGameTest().endStatuePlacesWithoutTeleportingAndDropsItsTotem(helper);
	}
}
