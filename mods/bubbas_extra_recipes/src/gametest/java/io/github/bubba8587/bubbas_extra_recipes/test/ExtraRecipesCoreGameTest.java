// dte:C3
package io.github.bubba8587.bubbas_extra_recipes.test;

import java.io.IOException;

import net.minecraft.gametest.framework.GameTestHelper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Core suite: the smoke tests that run on every build and push, one per main mechanic. */
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
