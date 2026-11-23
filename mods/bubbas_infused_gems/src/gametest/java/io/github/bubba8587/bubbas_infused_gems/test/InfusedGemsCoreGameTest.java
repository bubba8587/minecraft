// dte:C4
package io.github.bubba8587.bubbas_infused_gems.test;

import net.minecraft.gametest.framework.GameTestHelper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Core suite: the smoke tests that run on every build and push, one per main mechanic. */
public class InfusedGemsCoreGameTest {
	@GameTest
	public void balanceJsonLoadsWithKnownValues(GameTestHelper helper) {
		new BalanceGameTest().balanceJsonLoadsWithKnownValues(helper);
	}

	@GameTest
	public void potionsAndBaseGemsBrewInfusedGems(GameTestHelper helper) {
		new BrewingGameTest().potionsAndBaseGemsBrewInfusedGems(helper);
	}

	@GameTest
	public void enchantingTableMakesAFinishedGem(GameTestHelper helper) {
		new EnchantingGameTest().enchantingTableMakesAFinishedGem(helper);
	}

	@GameTest
	public void smithingTableSocketsUpToTheLimit(GameTestHelper helper) {
		new SocketingGameTest().smithingTableSocketsUpToTheLimit(helper);
	}

	@GameTest
	public void wornEffectsMatchTheJson(GameTestHelper helper) {
		new GemEffectsGameTest().wornEffectsMatchTheJson(helper);
	}

	@GameTest
	public void meleeWeaponGemsHitTheTarget(GameTestHelper helper) {
		new GemEffectsGameTest().meleeWeaponGemsHitTheTarget(helper);
	}

	@GameTest
	public void gemsCommandListsCombinedEffects(GameTestHelper helper) {
		new GemsCommandGameTest().gemsCommandListsCombinedEffects(helper);
	}

	@GameTest
	public void listedTablesKeepTheirLootAndHoldGems(GameTestHelper helper) {
		new LootGameTest().listedTablesKeepTheirLootAndHoldGems(helper);
	}
}
