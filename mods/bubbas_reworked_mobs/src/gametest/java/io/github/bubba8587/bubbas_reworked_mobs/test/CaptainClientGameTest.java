// dte:C1,B15,B16,B17
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.function.Function;

import io.github.bubba8587.bubbas_reworked_mobs.captain.Captains;
import io.github.bubba8587.bubbas_reworked_mobs.captain.SoulfireBlaze;
import io.github.bubba8587.bubbas_reworked_mobs.captain.WitherCaptain;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModItems;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * The Wither Captain by eye (specs/wither-captain.md; client part {@code captain}). On a blackstone platform in the
 * sky, night vision on. Shots: captain-summon-front, captain-summon-side (the summoning pose, aura and particles),
 * captain-normal, soulfire-blaze, helm-front, helm-side, helm-close (worn on an armour stand), helm-icon (hotbar), helm-player-front/side (third person).
 */
@SuppressWarnings("UnstableApiUsage")
public class CaptainClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("captain")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			BlockPos base = context.computeOnClient(mc -> mc.player.blockPosition());
			int x = base.getX();
			int y = base.getY() + 20;
			int z = base.getZ();
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
			server.runCommand("tp @p %d %d %d".formatted(x, y + 2, z - 6));
			context.waitTicks(5);
			server.runCommand("fill %d %d %d %d %d %d minecraft:polished_blackstone".formatted(x - 12, y - 1, z - 10, x + 12, y - 1, z + 10));
			server.runCommand("gamemode spectator @p");
			context.waitTicks(5);

			// --- the captain, summoning, and a Soulfire Blaze to the side -----------------------------------
			onServer(server, level -> {
				WitherCaptain captain = Captains.WITHER_CAPTAIN.create(level, EntitySpawnReason.COMMAND);
				captain.snapTo(x + 0.5, y, z + 0.5, 180.0F, 0.0F);
				captain.finalizeSpawn(level, level.getCurrentDifficultyAt(captain.blockPosition()), EntitySpawnReason.COMMAND, null);
				captain.setYHeadRot(180.0F);
				captain.setYBodyRot(180.0F);
				captain.setNoAi(true);
				captain.setSilent(true);
				level.addFreshEntity(captain);
				captain.startSummoning(level);
				SoulfireBlaze blaze = Captains.SOULFIRE_BLAZE.create(level, EntitySpawnReason.COMMAND);
				blaze.snapTo(x - 5.5, y + 0.5, z + 0.5, 180.0F, 0.0F);
				blaze.setYHeadRot(180.0F);
				blaze.setYBodyRot(180.0F);
				blaze.setNoAi(true);
				blaze.setSilent(true);
				level.addFreshEntity(blaze);
				ArmorStand stand = EntityTypes.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND);
				stand.snapTo(x + 6.5, y, z + 0.5, 180.0F, 0.0F);
				stand.setYBodyRot(180.0F);
				stand.setYHeadRot(180.0F);
				stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CAPTAINS_HELM));
				level.addFreshEntity(stand);
				return null;
			});
			context.waitTicks(30);
			tp(context, server, x + 0.5, y + 0.4, z - 3.5, 0.0F, -8.0F);
			context.takeScreenshot("captain-summon-front");
			tp(context, server, x + 4.0, y + 0.4, z + 0.5, 90.0F, -8.0F);
			context.takeScreenshot("captain-summon-side");
			tp(context, server, x - 5.5, y + 0.2, z - 3.0, 0.0F, -5.0F);
			context.takeScreenshot("soulfire-blaze");

			// --- the helm on an armour stand ---------------------------------------------------------------
			tp(context, server, x + 6.5, y + 0.6, z - 2.5, 0.0F, 5.0F);
			context.takeScreenshot("helm-front");
			tp(context, server, x + 9.0, y + 0.6, z + 0.5, 90.0F, 5.0F);
			context.takeScreenshot("helm-side");
			tp(context, server, x + 7.6, y + 1.0, z - 0.6, 45.0F, 20.0F);
			context.takeScreenshot("helm-close");

			// --- a captain in its normal phase (a fresh one in the same spot) -----------------------------
			onServer(server, level -> {
				net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(x - 20, y - 5, z - 20, x + 20, y + 10, z + 20);
				level.getEntitiesOfClass(WitherCaptain.class, box, c -> true).forEach(WitherCaptain::discard);
				level.getEntitiesOfClass(SoulfireBlaze.class, box, b -> b.captain() != null).forEach(SoulfireBlaze::discard);
				WitherCaptain captain = Captains.WITHER_CAPTAIN.create(level, EntitySpawnReason.COMMAND);
				captain.snapTo(x + 0.5, y, z + 0.5, 180.0F, 0.0F);
				captain.finalizeSpawn(level, level.getCurrentDifficultyAt(captain.blockPosition()), EntitySpawnReason.COMMAND, null);
				captain.setYHeadRot(180.0F);
				captain.setYBodyRot(180.0F);
				captain.setNoAi(true);
				captain.setSilent(true);
				level.addFreshEntity(captain);
				return null;
			});
			context.waitTicks(20);
			tp(context, server, x + 0.5, y + 0.4, z - 3.5, 0.0F, -8.0F);
			context.takeScreenshot("captain-normal");

			// --- the helm icon in the hotbar ---------------------------------------------------------------
			server.runCommand("gamemode creative @p");
			server.runCommand("item replace entity @p hotbar.0 with bubbas_reworked_mobs:captains_helm");
			server.runCommand("item replace entity @p hotbar.1 with bubbas_reworked_mobs:nether_star_shard");
			server.runCommand("item replace entity @p hotbar.2 with bubbas_reworked_mobs:wither_captain_spawn_egg");
			server.runCommand("item replace entity @p hotbar.3 with bubbas_reworked_mobs:soulfire_blaze_spawn_egg");
			tp(context, server, x + 0.5, y + 0.4, z - 8.5, 180.0F, 0.0F);
			context.takeScreenshot("helm-icon");

			// --- a player wearing the helm, third person front and side --------------------------------------
			server.runCommand("item replace entity @p armor.head with bubbas_reworked_mobs:captains_helm");
			server.runCommand("item replace entity @p hotbar.0 with minecraft:air");
			tp(context, server, x + 0.5, y, z - 8.5, 0.0F, 15.0F);
			context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(10);
			context.takeScreenshot("helm-player-front");
			tp(context, server, x + 0.5, y, z - 8.5, 90.0F, 15.0F);
			context.waitTicks(10);
			context.takeScreenshot("helm-player-side");
			context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
		}
	}

	private static <T> T onServer(TestServerContext server, Function<ServerLevel, T> action) {
		return server.computeOnServer(s -> action.apply(s.overworld()));
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand("tp @p %.2f %.2f %.2f %.1f %.1f".formatted(x, y, z, yaw, pitch));
		context.waitFor(mc -> Math.abs(mc.player.getX() - x) < 0.01 && Math.abs(mc.player.getZ() - z) < 0.01);
		context.waitTicks(3);
	}
}
