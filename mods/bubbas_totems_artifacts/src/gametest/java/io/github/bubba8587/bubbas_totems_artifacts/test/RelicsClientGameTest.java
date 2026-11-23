// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.test;

import java.lang.reflect.Field;
import java.util.Locale;

import io.github.bubba8587.bubbas_totems_artifacts.relic.RelicMaps;
import io.github.bubba8587.bubbas_totems_artifacts.relic.RelicShred;
import io.github.bubba8587.bubbas_totems_artifacts.relic.RelicVaultPiece;
import io.github.bubba8587.bubbas_totems_artifacts.relic.Relics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Relic maps and vaults by eye (client part {@code relics}): shreds in the inventory with a tooltip (relics-tooltip,
 * -32x), the finished map in hand (relics-map), a grand vault from the door, the back and the side (relics-vault-*),
 * the grand vault's pedestal with a weapon and the small one's with a totem statue (-pedestal, -pedestal-close), and with -PwithMobs a noble crypt's vault: the crypt's far wall, the stair behind
 * it and the vault (relics-crypt-*).
 */
@SuppressWarnings("UnstableApiUsage")
public class RelicsClientGameTest implements FabricClientGameTest {
	private static final String PACK_32X = "bubbas_totems_artifacts:bubbas_totems_artifacts_32x";
	private static final int Y = 120;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("relics")) {
			return;
		}
		context.getInput().resizeWindow(1280, 720);
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			tooltip(context, sp, "relics-tooltip");
			setPack(context, true);
			tooltip(context, sp, "relics-tooltip-32x");
			setPack(context, false);
			map(context, sp);
			BlockPos base = context.computeOnClient(mc -> mc.player.blockPosition());
			server.runCommand("gamemode spectator @p");
			server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
			vault(context, sp, server, RelicVaultPiece.Kind.GRAND, RelicVaultPiece.Prime.WEAPON, new BlockPos(base.getX(), Y, base.getZ() + 40));
			vault(context, sp, server, RelicVaultPiece.Kind.SMALL, RelicVaultPiece.Prime.STATUE, new BlockPos(base.getX() + 40, Y, base.getZ() + 40));
			if (TestSupport.withMobs()) {
				Mobs.crypt(context, sp, server, base.getX() - 60, base.getZ());
			}
		}
	}

	private static void tooltip(ClientGameTestContext context, TestSingleplayerContext sp, String shot) {
		sp.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
			player.getInventory().clearContent();
			player.getInventory().setItem(9, RelicMaps.shred("wondrous_crab", 9, 1));
			player.getInventory().setItem(10, RelicMaps.shred("wondrous_crab", 9, 2));
			player.getInventory().setItem(11, RelicMaps.shred("egregious_zealot", 4, 2));
			player.getInventory().setItem(12, RelicMaps.shred("gilded_owl", 4, 1));
			player.getInventory().setItem(13, RelicMaps.shred("gilded_owl", 4, 4));
		});
		context.waitFor(mc -> mc.player.getInventory().getItem(10).is(Relics.RELIC_MAP_SHRED));
		context.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
		context.waitForScreen(InventoryScreen.class);
		context.waitTicks(2);
		context.getInput().setCursorPos(5, 5);
		context.waitTicks(2);
		context.takeScreenshot(shot.replace("tooltip", "inventory"));
		double[] pos = context.computeOnClient(mc -> slot(mc, 11));
		context.getInput().setCursorPos(pos[0], pos[1]);
		context.waitTicks(3);
		context.takeScreenshot(shot);
		context.setScreen(() -> null);
	}

	private static void map(ClientGameTestContext context, TestSingleplayerContext sp) {
		sp.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			player.getInventory().clearContent();
			player.getInventory().setItem(0, RelicMaps.map(server.overworld(), new RelicShred("wondrous_crab", 9, 9)));
			player.getInventory().setSelectedSlot(0);
		});
		context.waitFor(mc -> mc.player.getMainHandItem().has(Relics.SHRED));
		context.getInput().lookAt(0.0F, 45.0F);
		context.waitTicks(40);
		context.takeScreenshot("relics-map");
	}

	private static void vault(ClientGameTestContext context, TestSingleplayerContext sp, TestServerContext server, RelicVaultPiece.Kind kind,
			RelicVaultPiece.Prime prime, BlockPos door) {
		tp(context, server, door.getX() + 0.5, door.getY() + 30, door.getZ() - 10, 0, 60);
		context.waitTicks(10);
		long seed = 7L;
		while (new RelicVaultPiece(kind, "wondrous_crab", door, seed).prime() != prime) {
			seed++;
		}
		long chosen = seed;
		RelicVaultPiece piece = server.computeOnServer(s -> {
			RelicVaultPiece p = new RelicVaultPiece(kind, "wondrous_crab", door, chosen);
			p.buildNow(s.overworld());
			return p;
		});
		String name = "relics-vault-" + kind.name().toLowerCase(Locale.ROOT);
		BlockPos c = piece.roomCentre();
		BlockPos ped = piece.pedestal();
		int hw = piece.getBoundingBox().maxX() - door.getX() - 1;
		tp(context, server, door.getX() + 0.5, door.getY() + 1.2, door.getZ() + 1.3, 0, 12);
		sp.getConnection().waitForChunksRender();
		context.waitTicks(10);
		context.takeScreenshot(name + "-door");
		if (kind == RelicVaultPiece.Kind.GRAND) {
			tp(context, server, door.getX() + hw - 0.5, door.getY() + 0.8, ped.getZ() + 0.5, 135, 18);
			context.takeScreenshot(name + "-back");
			tp(context, server, door.getX() - hw + 0.7, door.getY() + 1.4, c.getZ() + 0.5, -90, 10);
			context.takeScreenshot(name + "-side");
			pedestal(context, server, ped, name);
			BlockPos top = piece.stairTop();
			tp(context, server, top.getX() + 0.5, top.getY() + 1.2, top.getZ() + 0.5, 0, 35);
			context.takeScreenshot(name + "-stair");
		} else {
			pedestal(context, server, ped, name);
		}
	}

	private static void pedestal(ClientGameTestContext context, TestServerContext server, BlockPos ped, String name) {
		tp(context, server, ped.getX() + 0.5, ped.getY() + 0.6, ped.getZ() - 1.5, 0, 25);
		context.takeScreenshot(name + "-pedestal");
		tp(context, server, ped.getX() - 0.8, ped.getY() - 0.7, ped.getZ() - 0.8, -45, 22);
		context.takeScreenshot(name + "-pedestal-close");
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.waitTicks(15);
	}

	private static double[] slot(Minecraft mc, int container) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.gui.screen();
		Slot slot = screen.getMenu().slots.stream().filter(s -> s.container == mc.player.getInventory() && s.getContainerSlot() == container)
				.findFirst().orElseThrow();
		double scale = mc.getWindow().getScreenWidth() / (double) mc.getWindow().getGuiScaledWidth();
		return new double[] { (field(screen, "leftPos") + slot.x + 8) * scale, (field(screen, "topPos") + slot.y + 8) * scale };
	}

	private static int field(AbstractContainerScreen<?> screen, String name) {
		try {
			Field f = AbstractContainerScreen.class.getDeclaredField(name);
			f.setAccessible(true);
			return f.getInt(screen);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
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

	/** Reworked Mobs' side; only loaded with that mod. */
	private static final class Mobs {
		private Mobs() {
		}

		static void crypt(ClientGameTestContext context, TestSingleplayerContext sp, TestServerContext server, int x, int z) {
			int ground = Y + 20;
			tp(context, server, x + 0.5, ground + 30, z - 20.5, 0, 50);
			context.waitTicks(10);
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 20, ground - 20, z - 4, x + 20, ground - 1, z + 60));
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 20, ground, z - 4, x + 20, ground, z + 60));
			BlockPos gate = new BlockPos(x, ground + 1, z);
			var crypt = server.computeOnServer(s -> {
				var noble = io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds.Wealth.NOBLE;
				long seed = 1;
				while (io.github.bubba8587.bubbas_reworked_mobs.api.Crypts.attachments(new io.github.bubba8587.bubbas_reworked_mobs.grave.CryptYardPiece(seed,
						gate, 19, 19, true, io.github.bubba8587.bubbas_reworked_mobs.grave.YardTrees.NONE,
						io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds.State.KEPT, noble,
						io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds.Climate.TEMPERATE)).isEmpty()) {
					seed++;
				}
				return io.github.bubba8587.bubbas_reworked_mobs.grave.GraveyardCommand.build(s.overworld(), gate, seed, 19, 19, true,
						io.github.bubba8587.bubbas_reworked_mobs.grave.YardKinds.State.KEPT, noble);
			});
			var vault = (RelicVaultPiece) server.computeOnServer(s -> io.github.bubba8587.bubbas_reworked_mobs.api.Crypts.attachments(crypt).getFirst());
			BlockPos wall = crypt.nobleFarWall();
			tp(context, server, wall.getX() + 0.5, wall.getY() + 0.6, wall.getZ() - 4.5, 0, 10);
			sp.getConnection().waitForChunksRender();
			context.waitTicks(10);
			context.takeScreenshot("relics-crypt-wall");
			BlockPos top = vault.stairTop();
			tp(context, server, top.getX() + 0.5, top.getY() + 1.4, top.getZ() + 0.3, 0, 40);
			context.takeScreenshot("relics-crypt-stair");
			BlockPos door = vault.door();
			tp(context, server, door.getX() + 0.5, door.getY() + 1.2, door.getZ() + 1.3, 0, 12);
			context.takeScreenshot("relics-crypt-vault");
		}
	}
}
