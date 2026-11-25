// dte:C1,B16
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.soulfire.SoulBurning;
import io.github.bubba8587.bubbas_reworked_mobs.soulfire.Soulfire;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Soulfire by eye (specs/soulfire.md; client part {@code soulfire}). On a blackstone platform in the sky. Shots:
 * soulfire-zombie (a soul-burning zombie beside a normally burning one), soulfire-player (the player soul-burning, third
 * person), soulfire-player-first-person (the soul burning overlay), soulfire-icons (hotbar: rod, powder, charge, the three Soulguard potions, a tipped arrow, the Captain's
 * Helm), soulfire-effect (the survival inventory's effect list with Soulguard); the icons and effect shots again with the 32x
 * pack (-32x).
 */
@SuppressWarnings("UnstableApiUsage")
public class SoulfireClientGameTest implements FabricClientGameTest {
	private static final String PACK_32X = "bubbas_reworked_mobs:bubbas_reworked_mobs_32x";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("soulfire")) {
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
			server.runCommand("tp @p %d %d %d".formatted(x, y + 2, z - 6));
			context.waitTicks(5);
			server.runCommand("fill %d %d %d %d %d %d minecraft:polished_blackstone".formatted(x - 8, y - 1, z - 8, x + 8, y - 1, z + 8));
			context.waitTicks(5);

			// --- a soul-burning zombie beside a normally burning one ---------------------------------------
			server.runOnServer(s -> {
				var level = s.overworld();
				for (boolean soul : new boolean[] {true, false}) {
					Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
					zombie.snapTo((soul ? x - 1 : x + 2) + 0.5, y, z + 0.5, 180.0F, 0.0F);
					zombie.setYHeadRot(180.0F);
					zombie.setYBodyRot(180.0F);
					zombie.setNoAi(true);
					zombie.setSilent(true);
					zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
					level.addFreshEntity(zombie);
					if (soul) {
						SoulBurning.igniteForSeconds(zombie, 600.0F);
					} else {
						zombie.igniteForSeconds(600.0F);
					}
				}
			});
			context.waitTicks(20);
			tp(context, server, x + 1.0, y + 0.3, z - 3.5, 0.0F, 5.0F);
			context.takeScreenshot("soulfire-zombie");

			// --- the player soul-burning, third person ------------------------------------------------------
			server.runCommand("gamemode survival @p");
			server.runCommand("effect give @p minecraft:resistance infinite 4 true");
			server.runCommand("effect give @p minecraft:regeneration infinite 4 true");
			tp(context, server, x - 4.5, y, z - 4.5, 0.0F, 15.0F);
			server.runOnServer(s -> {
				var player = s.getPlayerList().getPlayers().getFirst();
				SoulBurning.igniteForSeconds(player, 600.0F);
			});
			context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(20);
			context.takeScreenshot("soulfire-player");
			context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
			context.waitTicks(5);
			context.takeScreenshot("soulfire-player-first-person");
			server.runCommand("effect clear @p");
			server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().clearFire());
			server.runCommand("gamemode creative @p");

			// --- icons and the Soulguard effect ---------------------------------------------------------------
			server.runCommand("item replace entity @p hotbar.0 with bubbas_reworked_mobs:soul_blaze_rod");
			server.runCommand("item replace entity @p hotbar.1 with bubbas_reworked_mobs:soul_blaze_powder");
			server.runCommand("item replace entity @p hotbar.2 with bubbas_reworked_mobs:soul_fire_charge");
			server.runCommand("item replace entity @p hotbar.3 with minecraft:potion[potion_contents={potion:\"bubbas_reworked_mobs:soulguard\"}]");
			server.runCommand("item replace entity @p hotbar.4 with minecraft:splash_potion[potion_contents={potion:\"bubbas_reworked_mobs:soulguard\"}]");
			server.runCommand("item replace entity @p hotbar.5 with minecraft:lingering_potion[potion_contents={potion:\"bubbas_reworked_mobs:long_soulguard\"}]");
			server.runCommand("item replace entity @p hotbar.6 with minecraft:tipped_arrow[potion_contents={potion:\"bubbas_reworked_mobs:soulguard\"}]");
			server.runCommand("item replace entity @p hotbar.7 with bubbas_reworked_mobs:captains_helm");
			server.runCommand("item replace entity @p hotbar.8 with minecraft:enchanted_book[stored_enchantments={\"bubbas_reworked_mobs:soulguard\":1}]");
			server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().addEffect(new MobEffectInstance(Soulfire.SOULGUARD, 20 * 180)));
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(3));
			server.runCommand("gamemode survival @p");
			tp(context, server, x + 0.5, y, z - 6.5, 180.0F, 0.0F);
			icons(context, "");
			setPack(context, true);
			icons(context, "-32x");
			setPack(context, false);
		}
	}

	private static void icons(ClientGameTestContext context, String suffix) {
		context.takeScreenshot("soulfire-icons" + suffix);
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitTicks(5);
		context.takeScreenshot("soulfire-effect" + suffix);
		context.setScreen(() -> null);
		context.waitTicks(2);
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.waitFor(mc -> Math.abs(mc.player.getX() - x) < 0.01 && Math.abs(mc.player.getZ() - z) < 0.01);
		context.waitTicks(3);
	}

	private static void setPack(ClientGameTestContext context, boolean on) {
		boolean changed = context.computeOnClient(mc -> {
			PackRepository repo = mc.getResourcePackRepository();
			if (repo.getSelectedIds().contains(PACK_32X) == on) {
				return false;
			}
			if (on) {
				repo.addPack(PACK_32X);
			} else {
				repo.removePack(PACK_32X);
			}
			mc.options.updateResourcePacks(repo);
			return true;
		});
		if (changed) {
			context.waitTicks(2);
			context.waitFor(mc -> mc.gui.overlay() == null, 1200);
			context.waitTicks(5);
		}
	}
}
