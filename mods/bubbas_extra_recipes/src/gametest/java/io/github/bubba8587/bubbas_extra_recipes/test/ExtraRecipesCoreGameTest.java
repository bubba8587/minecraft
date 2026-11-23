// dte:C3
package io.github.bubba8587.bubbas_extra_recipes.test;

import java.io.IOException;

import net.minecraft.gametest.framework.GameTestHelper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Core suite (test-lite, specs/quality.md "Test suites"): the smoke tests that run on every build and push, one per
 * main mechanic. Each one runs a test whose body lives with its siblings in a milestone class (the method there has no
 * {@code @GameTest} of its own, so the full suite runs it once). Everything else in this source set is the milestone
 * suite, registered only with -PmilestoneTests. Objectives: specs/extra-recipes-mod.md, "Milestone test objectives".
 */
public class ExtraRecipesCoreGameTest {
	@GameTest
	public void bundledDatapackIsEnabledAndItsRecipesExist(GameTestHelper helper) throws IOException {
		new BundledPacksGameTest().bundledDatapackIsEnabledAndItsRecipesExist(helper);
	}

	@GameTest
	public void gliderTurnsSlowerThanElytra(GameTestHelper helper) throws ReflectiveOperationException {
		new GliderGameTest().gliderTurnsSlowerThanElytra(helper);
	}

	@GameTest
	public void syntheticOreUsesItsOwnTableAndGivesNoXp(GameTestHelper helper) {
		new SyntheticOreGameTest().syntheticOreUsesItsOwnTableAndGivesNoXp(helper);
	}
}
