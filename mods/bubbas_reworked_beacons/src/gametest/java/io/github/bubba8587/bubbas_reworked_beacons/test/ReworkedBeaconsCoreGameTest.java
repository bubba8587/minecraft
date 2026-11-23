// dte:C5
package io.github.bubba8587.bubbas_reworked_beacons.test;

import net.minecraft.gametest.framework.GameTestHelper;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/** Core suite: one smoke test per main mechanic, bodies in BeaconGameTest. */
public class ReworkedBeaconsCoreGameTest {
	@GameTest(maxTicks = 400)
	public void standalonePaymentBuysItsPower(GameTestHelper helper) {
		new BeaconGameTest().standalonePaymentBuysItsPower(helper);
	}

	@GameTest(maxTicks = 400)
	public void pyramidSizeCapsThePower(GameTestHelper helper) {
		new BeaconGameTest().pyramidSizeCapsThePower(helper);
	}

	@GameTest
	public void pairedBeaconsMakeAWideBox(GameTestHelper helper) {
		new BeaconGameTest().pairedBeaconsMakeAWideBox(helper);
	}

	@GameTest
	public void beaconHangsUnderABlock(GameTestHelper helper) {
		new BeaconGameTest().beaconHangsUnderABlock(helper);
	}
}
