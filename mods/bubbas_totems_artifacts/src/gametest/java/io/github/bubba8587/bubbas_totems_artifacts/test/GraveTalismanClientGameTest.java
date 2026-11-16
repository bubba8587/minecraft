// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.test;

import io.github.bubba8587.bubbas_totems_artifacts.client.talisman.GraveTalismanGlow;
import io.github.bubba8587.bubbas_totems_artifacts.talisman.GraveTalisman;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * The Grave Talisman at night, held in the main hand and in the hotbar (client test part {@code talisman}): off far
 * from undead, faint with a husk 30 blocks off, bright with a skeleton 18 blocks off, brightest with a zombie 2 blocks
 * off (vanilla undead, so it runs without Reworked Mobs; its sleepers are covered by the server test). Checks the
 * client's glow level at each step and takes a screenshot (build/run/clientGameTest/screenshots/).
 */
@SuppressWarnings("UnstableApiUsage")
public class GraveTalismanClientGameTest implements FabricClientGameTest {
	private static final String ID = "bubbas_totems_artifacts";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("talisman")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			BlockPos base = context.computeOnClient(mc -> mc.player.blockPosition());
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set midnight");
			// Stand at the west end of a dark stone floor; undead appear to the east (+x), the way we look.
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(base.getX() - 2, base.getY(), base.getZ() - 4, base.getX() + 32, base.getY() + 4, base.getZ() + 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:deepslate_tiles".formatted(base.getX() - 2, base.getY() - 1, base.getZ() - 4, base.getX() + 32, base.getY() - 1, base.getZ() + 4));
			server.runCommand("item replace entity @p weapon.mainhand with " + ID + ":grave_talisman");
			server.runCommand("item replace entity @p hotbar.1 with " + ID + ":grave_talisman");
			context.runOnClient(mc -> {
				mc.player.getInventory().setSelectedSlot(0);
				mc.player.getAbilities().flying = true;
				mc.player.onUpdateAbilities();
			});
			server.runCommand("tp @p %d.5 %d %d.5 -90 20".formatted(base.getX(), base.getY(), base.getZ()));
			context.waitTicks(20);

			expect(context, GraveTalisman.OFF);
			context.takeScreenshot("talisman-off");

			summon(server, context, EntityTypes.HUSK, "husk", base.offset(30, 0, 0));
			expect(context, GraveTalisman.FAINT);
			context.takeScreenshot("talisman-faint");

			summon(server, context, EntityTypes.SKELETON, "skeleton", base.offset(18, 0, 1));
			expect(context, GraveTalisman.BRIGHT);
			context.takeScreenshot("talisman-bright");

			summon(server, context, EntityTypes.ZOMBIE, "zombie", base.offset(2, 0, -1));
			expect(context, GraveTalisman.BRIGHTEST);
			context.takeScreenshot("talisman-brightest");
			// From the front, in third person: the lit gem in the player's hand.
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(5);
			context.takeScreenshot("talisman-brightest-third-person");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

			// The same, in the off hand.
			server.runCommand("item replace entity @p weapon.offhand with " + ID + ":grave_talisman");
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(2));
			context.waitTicks(10);
			context.takeScreenshot("talisman-brightest-offhand");

			// Undead gone: it goes dark again.
			server.runCommand("kill @e[type=minecraft:husk]");
			server.runCommand("kill @e[type=minecraft:skeleton]");
			server.runCommand("kill @e[type=minecraft:zombie]");
			expect(context, GraveTalisman.OFF);
		}
	}

	private static void summon(TestServerContext server, ClientGameTestContext context, EntityType<?> type, String name, BlockPos pos) {
		server.runCommand("summon minecraft:%s %d %d %d {NoAI:1b,Silent:1b,Rotation:[90f,0f]}".formatted(name, pos.getX(), pos.getY(), pos.getZ()));
		context.waitFor(mc -> !mc.level.getEntitiesOfClass(Entity.class, new AABB(pos).inflate(1.0), e -> e.getType() == type).isEmpty());
	}

	/** Waits (a search runs every few ticks) until the client's glow level is {@code level}; fails after the default timeout. */
	private static void expect(ClientGameTestContext context, int level) {
		context.waitFor(mc -> GraveTalismanGlow.level() == level);
		context.waitTicks(GraveTalisman.SEARCH_INTERVAL_TICKS + 3);
		int now = context.computeOnClient(mc -> GraveTalismanGlow.level());
		if (now != level) {
			throw new AssertionError("glow " + now + ", expected " + level);
		}
	}
}
