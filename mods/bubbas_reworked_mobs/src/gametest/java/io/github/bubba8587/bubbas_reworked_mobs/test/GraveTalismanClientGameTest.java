// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.client.undead.GraveTalismanGlow;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModEntities;
import io.github.bubba8587.bubbas_reworked_mobs.undead.GraveTalisman;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * The Grave Talisman at night, held in the main hand and in the hotbar: off far from undead, faint with a sleeper 12
 * blocks off, bright with one 6 blocks off, brightest with a woken zombie 2 blocks off. Checks the client's glow level
 * at each step and takes a screenshot (build/run/clientGameTest/screenshots/).
 */
@SuppressWarnings("UnstableApiUsage")
public class GraveTalismanClientGameTest implements FabricClientGameTest {
	private static final String ID = "bubbas_reworked_mobs";

	@Override
	public void runTest(ClientGameTestContext context) {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(base.getX() - 2, base.getY(), base.getZ() - 4, base.getX() + 16, base.getY() + 4, base.getZ() + 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:deepslate_tiles".formatted(base.getX() - 2, base.getY() - 1, base.getZ() - 4, base.getX() + 16, base.getY() - 1, base.getZ() + 4));
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

			sleeper(server, context, base.offset(12, 0, 0));
			expect(context, GraveTalisman.FAINT);
			context.takeScreenshot("talisman-faint");

			sleeper(server, context, base.offset(6, 0, 1));
			expect(context, GraveTalisman.BRIGHT);
			context.takeScreenshot("talisman-bright");

			BlockPos zombie = base.offset(2, 0, -1);
			server.runCommand("summon %s:woken_zombie %d %d %d {NoAI:1b,Rotation:[90f,0f]}".formatted(ID, zombie.getX(), zombie.getY(), zombie.getZ()));
			context.waitFor(mc -> !mc.level.getEntitiesOfClass(Entity.class, new AABB(zombie).inflate(1.0),
					e -> e.getType() == ModEntities.woken(UndeadVariant.ZOMBIE)).isEmpty());
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
			server.runCommand("kill @e[type=%s:woken_zombie]".formatted(ID));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(base.getX() + 1, base.getY(), base.getZ() - 4, base.getX() + 16, base.getY() + 1, base.getZ() + 4));
			expect(context, GraveTalisman.OFF);
		}
	}

	private static void sleeper(TestServerContext server, ClientGameTestContext context, BlockPos pos) {
		server.runCommand("setblock %d %d %d %s:zombie_sleeper[facing=west,pose=lying]{sensitivity:0f}".formatted(pos.getX(), pos.getY(), pos.getZ(), ID));
		context.waitFor(mc -> mc.level.getBlockState(pos).is(ModBlocks.sleeper(UndeadVariant.ZOMBIE)));
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
