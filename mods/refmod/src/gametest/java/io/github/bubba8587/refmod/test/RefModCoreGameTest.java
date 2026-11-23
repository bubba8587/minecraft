// dte:A2,A4
package io.github.bubba8587.refmod.test;

import net.minecraft.gametest.framework.GameTestHelper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Core suite (test-lite, specs/quality.md "Test suites"): the smoke tests that run on every build and push. refmod is
 * the toolchain proof, so its core is one item, one block entity and the datagen output. Each one runs a test whose
 * body lives in RefModGameTest (the method there has no {@code @GameTest} of its own, so the full suite runs it once).
 * Everything else (RefModGameTest, SyntheticOreGameTest) is the milestone suite, registered only with -PmilestoneTests.
 */
public class RefModCoreGameTest {
	@GameTest
	public void tallyStickCountsUses(GameTestHelper helper) {
		new RefModGameTest().tallyStickCountsUses(helper);
	}

	@GameTest(maxTicks = 60)
	public void counterBlockCountsSavesAndSyncs(GameTestHelper helper) {
		new RefModGameTest().counterBlockCountsSavesAndSyncs(helper);
	}

	@GameTest
	public void datagenRecipeLootAndTagLoad(GameTestHelper helper) {
		new RefModGameTest().datagenRecipeLootAndTagLoad(helper);
	}
}
