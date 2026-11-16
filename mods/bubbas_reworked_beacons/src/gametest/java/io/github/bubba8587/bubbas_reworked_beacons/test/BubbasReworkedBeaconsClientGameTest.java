package io.github.bubba8587.bubbas_reworked_beacons.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

// Client game test: ./gradlew :bubbas_reworked_beacons:runClientGameTest. Screenshots go to build/run/clientGameTest/screenshots/.
@SuppressWarnings("UnstableApiUsage")
public class BubbasReworkedBeaconsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("bubbas_reworked_beacons-world");
		}
	}
}
