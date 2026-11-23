// dte:C3,B16
package io.github.bubba8587.bubbas_extra_recipes.test;

import java.lang.reflect.Field;

import io.github.bubba8587.bubbas_extra_recipes.BubbasExtraRecipes;
import io.github.bubba8587.bubbas_extra_recipes.blockstate.SyntheticOre;
import io.github.bubba8587.bubbas_extra_recipes.registry.ModItems;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Client game test (./gradlew :bubbas_extra_recipes:runClientGameTest). Screenshots go to
 * build/run/clientGameTest/screenshots/:
 * the Glider in the inventory with its tooltip; a player wearing it, standing and gliding, from the front and back;
 * synthetic (left) and natural (right) deepslate diamond ore side by side; then the Glider again with the 32x pack on.
 */
@SuppressWarnings("UnstableApiUsage")
public class BubbasExtraRecipesClientGameTest implements FabricClientGameTest {
	private static final String PACK_32X = BubbasExtraRecipes.id(BubbasExtraRecipes.MOD_ID + "_32x").toString();

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("glider")) {
			return;
		}
		context.getInput().resizeWindow(1280, 720);
		// Leave 32x off at the start even if an earlier, interrupted run saved it in options.txt.
		setPack(context, PACK_32X, false);
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			boolean looksOn = context.computeOnClient(mc -> mc.getResourcePackRepository().getSelectedIds()
					.contains(BubbasExtraRecipes.RESOURCEPACK_ID.toString()));
			if (!looksOn) {
				throw new AssertionError("bundled resource pack not enabled by default: "
						+ context.computeOnClient(mc -> mc.getResourcePackRepository().getSelectedIds()));
			}

			inventoryTooltip(context, singleplayer, "glider-tooltip");
			wearing(context, singleplayer);
			ores(context, singleplayer);

			setPack(context, PACK_32X, true);
			inventoryTooltip(context, singleplayer, "glider-tooltip-32x");
			gliding(context, singleplayer, CameraType.THIRD_PERSON_BACK, "glider-gliding-back-32x");
			setPack(context, PACK_32X, false);
		}
	}

	private static void inventoryTooltip(ClientGameTestContext context, TestSingleplayerContext singleplayer, String shot) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			player.setGameMode(GameType.SURVIVAL);
			player.getInventory().clearContent();
			player.getInventory().setItem(0, new ItemStack(ModItems.GLIDER));
		});
		context.waitFor(mc -> mc.player.getInventory().getItem(0).is(ModItems.GLIDER));
		context.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
		context.waitForScreen(InventoryScreen.class);
		context.waitTicks(2);
		double[] pos = context.computeOnClient(BubbasExtraRecipesClientGameTest::gliderSlotInWindow);
		context.getInput().setCursorPos(pos[0], pos[1]);
		context.waitTicks(3);
		context.takeScreenshot("bubbas_extra_recipes-" + shot);
		context.setScreen(() -> null);
	}

	/** A player in survival on the ground, wearing the Glider, seen from behind and from the front. */
	private static void wearing(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			player.getInventory().clearContent();
			player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.GLIDER));
		});
		context.waitFor(mc -> mc.player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.GLIDER) && mc.player.onGround());
		context.getInput().lookAt(30.0F, 15.0F);
		for (CameraType camera : new CameraType[] {CameraType.THIRD_PERSON_BACK, CameraType.THIRD_PERSON_FRONT}) {
			context.runOnClient(mc -> mc.options.setCameraType(camera));
			context.waitTicks(5);
			context.takeScreenshot("bubbas_extra_recipes-glider-standing-" + (camera == CameraType.THIRD_PERSON_BACK ? "back" : "front"));
		}
		gliding(context, singleplayer, CameraType.THIRD_PERSON_BACK, "glider-gliding-back");
		gliding(context, singleplayer, CameraType.THIRD_PERSON_FRONT, "glider-gliding-front");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
	}

	/** Teleport 40 blocks up, start gliding with the jump key (as a player would), then screenshot. */
	private static void gliding(ClientGameTestContext context, TestSingleplayerContext singleplayer, CameraType camera, String shot) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			player.setGameMode(GameType.SURVIVAL);
			player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.GLIDER));
			player.teleportTo(player.getX(), player.getY() + 40, player.getZ());
		});
		context.waitFor(mc -> !mc.player.onGround() && mc.player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.GLIDER));
		context.getInput().lookAt(30.0F, 10.0F);
		context.waitTicks(3);
		context.getInput().holdKeyFor(options -> options.keyJump, 2);
		context.waitTicks(3);
		if (!context.computeOnClient(mc -> mc.player.isFallFlying())) {
			throw new AssertionError("pressing jump in the air with the Glider on did not start gliding");
		}
		context.runOnClient(mc -> mc.options.setCameraType(camera));
		context.waitTicks(10);
		context.takeScreenshot("bubbas_extra_recipes-" + shot);
		// Land: back to the ground so the next step starts from a known place.
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			player.stopFallFlying();
			BlockPos spawn = server.overworld().getRespawnData().pos();
			BlockPos top = server.overworld().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, spawn);
			player.teleportTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
			player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
			player.fallDistance = 0;
		});
		context.waitFor(mc -> mc.player.onGround() && !mc.player.isFallFlying());
	}

	/** Synthetic (west, left) and natural (east, right) deepslate diamond ore side by side on stone. */
	private static void ores(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		BlockPos ore = context.computeOnClient(mc -> mc.player.blockPosition().north(3).above());
		BlockPos synth = ore.west();
		singleplayer.getServer().runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(synth.getX() - 1, ore.getY() - 1, ore.getZ(), ore.getX() + 1, ore.getY() - 1, ore.getZ()));
		singleplayer.getServer().runCommand("setblock %d %d %d minecraft:deepslate_diamond_ore".formatted(ore.getX(), ore.getY(), ore.getZ()));
		singleplayer.getServer().runCommand("setblock %d %d %d minecraft:deepslate_diamond_ore[synthetic=true]".formatted(synth.getX(), synth.getY(), synth.getZ()));
		context.waitFor(mc -> mc.level.getBlockState(ore).is(Blocks.DEEPSLATE_DIAMOND_ORE)
				&& !mc.level.getBlockState(ore).getValue(SyntheticOre.SYNTHETIC)
				&& mc.level.getBlockState(synth).getValue(SyntheticOre.SYNTHETIC));
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		context.getInput().lookAt(ore);
		context.waitTicks(5);
		context.takeScreenshot("bubbas_extra_recipes-synthetic-ore-left-natural-right");
	}

	private static void setPack(ClientGameTestContext context, String id, boolean on) {
		boolean changed = context.computeOnClient(mc -> {
			PackRepository repo = mc.getResourcePackRepository();
			if (!repo.getAvailableIds().contains(id)) {
				throw new AssertionError("no resource pack " + id + "; available: " + repo.getAvailableIds());
			}
			if (repo.getSelectedIds().contains(id) == on) {
				return false;
			}
			if (on) {
				repo.addPack(id);
			} else {
				repo.removePack(id);
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

	private static double[] gliderSlotInWindow(Minecraft mc) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.gui.screen();
		Slot slot = screen.getMenu().slots.stream().filter(s -> s.getItem().is(ModItems.GLIDER)).findFirst()
				.orElseThrow(() -> new AssertionError("glider not in the open inventory"));
		double scale = mc.getWindow().getScreenWidth() / (double) mc.getWindow().getGuiScaledWidth();
		return new double[] {(intField(screen, "leftPos") + slot.x + 8) * scale, (intField(screen, "topPos") + slot.y + 8) * scale};
	}

	// leftPos/topPos are protected; Minecraft 26.x is unobfuscated, so reflection by Mojang name is stable.
	private static int intField(AbstractContainerScreen<?> screen, String name) {
		try {
			Field f = AbstractContainerScreen.class.getDeclaredField(name);
			f.setAccessible(true);
			return f.getInt(screen);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
