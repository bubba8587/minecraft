// dte:C7,B16
package io.github.bubba8587.bubbas_reworked_mining.test;

import io.github.bubba8587.bubbas_reworked_mining.BubbasReworkedMining;
import io.github.bubba8587.bubbas_reworked_mining.registry.ModItems;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * The quartz set by eye (client test part {@code quartz}): every piece in the hotbar next to iron, a pickaxe and a sword
 * held, and the armor worn on two armor stands (quartz next to iron) and by the player in third person, each at 16x
 * and 32x. Screenshots go to build/run/clientGameTest/screenshots/.
 */
@SuppressWarnings("UnstableApiUsage")
public class BubbasReworkedMiningClientGameTest implements FabricClientGameTest {
	private static final String ID = BubbasReworkedMining.MOD_ID;
	private static final String PACK_32X = ID + ":" + ID + "_32x";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("quartz")) {
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
			server.runCommand("gamerule show_advancement_messages false");
			server.runCommand("time set noon");
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(base.getX() - 8, base.getY(), base.getZ() - 8, base.getX() + 8, base.getY() + 6, base.getZ() + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:netherrack".formatted(base.getX() - 8, base.getY() - 1, base.getZ() - 8, base.getX() + 8, base.getY() - 1, base.getZ() + 8));
			// Facing south (+z), looking slightly down at the stands.
			server.runCommand("tp @p %d.5 %d %d.5 0 15".formatted(base.getX(), base.getY(), base.getZ()));

			Item[] set = {ModItems.QUARTZ_SWORD, ModItems.QUARTZ_PICKAXE, ModItems.QUARTZ_AXE, ModItems.QUARTZ_SHOVEL, ModItems.QUARTZ_HOE,
					ModItems.QUARTZ_HELMET, ModItems.QUARTZ_CHESTPLATE, ModItems.QUARTZ_LEGGINGS, Items.IRON_PICKAXE};
			server.runOnServer(s -> {
				ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
				for (int i = 0; i < set.length; i++) {
					p.getInventory().setItem(i, new ItemStack(set[i]));
				}
				ServerLevel level = p.level();
				stand(level, base.getX() - 0.6, base.getY(), base.getZ() + 3.5, ModItems.QUARTZ_HELMET, ModItems.QUARTZ_CHESTPLATE,
						ModItems.QUARTZ_LEGGINGS, ModItems.QUARTZ_BOOTS, ModItems.QUARTZ_PICKAXE);
				stand(level, base.getX() + 1.6, base.getY(), base.getZ() + 3.5, Items.IRON_HELMET, Items.IRON_CHESTPLATE,
						Items.IRON_LEGGINGS, Items.IRON_BOOTS, Items.IRON_PICKAXE);
			});

			for (boolean x32 : new boolean[] {false, true}) {
				setPack(context, x32);
				String suffix = x32 ? "-32x" : "";
				// The two stands from up close, HUD hidden: quartz (right) next to iron (left).
				server.runCommand("tp @p %d.5 %d %d.5 0 10".formatted(base.getX(), base.getY(), base.getZ() + 1));
				hud(context, false);
				context.waitTicks(10);
				context.takeScreenshot("quartz-stands" + suffix);
				hud(context, true);
				// Held in first person, with the hotbar showing every piece.
				for (int slot : new int[] {1, 0}) {
					select(context, server, slot);
					context.waitTicks(30);
					context.runOnClient(mc -> mc.gui.toastManager().clear());
					context.takeScreenshot((slot == 1 ? "quartz-held-pickaxe" : "quartz-held-sword") + suffix);
				}
				// Worn by the player, seen from the front with the stands behind the camera, holding the pickaxe.
				server.runOnServer(s -> {
					ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
					p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.QUARTZ_HELMET));
					p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.QUARTZ_CHESTPLATE));
					p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.QUARTZ_LEGGINGS));
					p.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.QUARTZ_BOOTS));
				});
				server.runCommand("tp @p %d.5 %d %d.5 180 10".formatted(base.getX(), base.getY(), base.getZ() - 4));
				select(context, server, 1);
				hud(context, false);
				context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
				context.waitTicks(20);
				context.takeScreenshot("quartz-worn-front" + suffix);
				context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
				hud(context, true);
				server.runOnServer(s -> {
					ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
					for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
						p.setItemSlot(slot, ItemStack.EMPTY);
					}
				});
				server.runCommand("tp @p %d.5 %d %d.5 0 15".formatted(base.getX(), base.getY(), base.getZ()));
			}
			setPack(context, false);
		}
	}

	private static void select(ClientGameTestContext context, TestServerContext server, int slot) {
		server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().getInventory().setSelectedSlot(slot));
		context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(slot));
	}

	private static void hud(ClientGameTestContext context, boolean shown) {
		context.runOnClient(mc -> {
			if (mc.gui.hud.isHidden() == shown) {
				mc.gui.hud.toggle();
			}
			mc.gui.toastManager().clear();
		});
	}

	private static void stand(ServerLevel level, double x, double y, double z, Item head, Item chest, Item legs, Item feet, Item hand) {
		ArmorStand stand = new ArmorStand(level, x, y, z);
		stand.setYRot(180.0F);
		stand.setYBodyRot(180.0F);
		stand.setYHeadRot(180.0F);
		stand.setShowArms(true);
		stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(head));
		stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chest));
		stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(legs));
		stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(feet));
		stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(hand));
		level.addFreshEntity(stand);
	}

	/** Turns the built-in 32x pack on or off and waits for the reload. */
	private static void setPack(ClientGameTestContext context, boolean on) {
		boolean changed = context.computeOnClient(mc -> {
			PackRepository repo = mc.getResourcePackRepository();
			if (!repo.getAvailableIds().contains(PACK_32X)) {
				throw new AssertionError("no resource pack " + PACK_32X + "; available: " + repo.getAvailableIds());
			}
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
