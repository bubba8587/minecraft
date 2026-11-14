package io.github.bubba8587.bubbas_crafting_tweaks.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

// Client game test: ./gradlew :bubbas_crafting_tweaks:runClientGameTest. Screenshots go to build/run/clientGameTest/screenshots/.
@SuppressWarnings("UnstableApiUsage")
public class BubbasCraftingTweaksClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("bubbas_crafting_tweaks-world");
		}
	}
}
