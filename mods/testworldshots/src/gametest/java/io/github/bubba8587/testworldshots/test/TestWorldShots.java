// dte:A3
package io.github.bubba8587.testworldshots.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.stream.Stream;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.entity.SignTextSlot;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.impl.client.gametest.world.TestWorldSaveImpl;

/**
 * Screenshot tour of the Bubba Test World (./gradlew :testworldshots:runClientGameTest). Copies the pristine
 * world from testworlds/ into this run's saves/ folder, opens the copy with every repo mod loaded, switches the
 * player to spectator (sleepers ignore spectators, and a spectator floats and can't bump anything), hides the HUD,
 * and for each shot teleports the camera to an eye position looking at a target, waits for the chunks to render,
 * and screenshots. Chest shots open the chest's menu on the server. Coordinates follow specs/testworld.md, Layout.
 * Screenshots: build/run/clientGameTest/screenshots/NNNN_<name>.png.
 */
@SuppressWarnings("UnstableApiUsage")
public class TestWorldShots implements FabricClientGameTest {
	/** Standing eye height of a player; tp places the feet. */
	private static final double EYE = 1.62;

	private ClientGameTestContext context;
	private TestSingleplayerContext world;

	@Override
	public void runTest(ClientGameTestContext context) {
		this.context = context;
		context.getInput().resizeWindow(1280, 720);
		Path source = Path.of(System.getProperty("bubba.testworld", "../../testworlds/bubba-test-world"));
		Path saves = context.computeOnClient(mc -> mc.getLevelSource().getBaseDir());
		Path copy = saves.resolve("bubba-test-world");
		copyWorld(source, copy);

		// Fabric's public API only opens worlds it created itself; its TestWorldSave implementation opens any
		// save folder by name (WorldOpenFlows.openWorld) and waits for it to load, which is what we need here.
		try (TestSingleplayerContext sp = new TestWorldSaveImpl(context, copy).open()) {
			this.world = sp;
			sp.getConnection().waitForChunksRender();
			sp.getServer().runCommand("gamemode spectator @a");
			context.runOnClient(mc -> {
				if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
			});
			context.getInput().setCursorPos(0, 0);
			tour();
		}
	}

	private void tour() {
		// Hub: spawn plaza with the station signs at z 3 (facing south).
		shot("hub_overview", 0.5, -50, 20, 0.5, -60, 2);
		shot("hub_signs", 1, -58.3, 8.5, 1, -59.6, 3.5);
		shot("hub_signs_left", -3, -58.6, 6.2, -3, -59.5, 3.5);
		shot("hub_signs_right", 4.5, -58.6, 6.2, 4.5, -59.5, 3.5);
		shot("hub_fresh_copy_sign", 7.5, -58.6, 7.2, 7.5, -59.5, 5.5);

		// Graveyard: x -44..-14, z -44..-14. Rows: skeletons z -18, zombies z -24; sitting x -42..-33, lying -30..-21.
		shot("graveyard_overview", -10, -46, -8, -29, -60, -29);
		shot("graveyard_entrance_signs", -19, -58.2, -8.8, -19, -59.5, -14);
		rowShots("skeletons", -18);
		rowShots("zombies", -24);
		shot("graveyard_close_skeleton_sitting", -40, -58.2, -15.6, -38.6, -59.6, -18);
		shot("graveyard_close_zombie_lying", -31.5, -57.8, -20.8, -29.5, -59.7, -24);
		shot("graveyard_wool_pen", -39, -52.5, -29, -39, -60, -33);
		shot("graveyard_crypt_above", -25, -50, -28, -25, -60, -39);
		shot("graveyard_crypt_inside", -25, -57.3, -33.2, -25, -59.8, -39.5);
		shot("graveyard_crypt_side", -17.5, -55.8, -39, -25, -60, -39);
		shot("graveyard_tools_chest", -16.5, -58.6, -13.6, -16.5, -59.6, -16);

		// Loot on display: rows wall z -30, stuck -27, ground -24, buried -21 at x -3..3, signs at x -5.
		shot("loot_overview", 0.5, -54, -13, 0.5, -60, -25);
		shot("loot_row_wall", -0.5, -58.3, -25.6, -0.5, -59, -30);
		shot("loot_row_stuck", -0.5, -57.4, -22.6, -0.5, -60, -27);
		shot("loot_row_ground", -0.5, -57.4, -19.6, -0.5, -60, -24);
		shot("loot_row_buried", -0.5, -57.4, -16.6, -0.5, -60, -21);
		shot("loot_side", 10.5, -55.5, -25.5, 0, -60, -25.5);
		shot("loot_grabbing", 3, -58.2, -22, 6.5, -60, -23);
		shot("loot_signs", -2.5, -58.4, -19.2, -4.5, -59.6, -25.5);

		// Combat arena: fenced x 14..40, z -44..-14; chests and signs at z -12; sleepers at z -40 and -30.
		shot("arena_overview", 27.5, -48, -5, 27.5, -60, -30);
		shot("arena_chests_signs", 27.5, -57.8, -7.6, 27.5, -59.7, -12);
		shot("arena_geared_sleepers", 27.5, -54.5, -31.5, 27.5, -60, -40);
		shot("arena_unarmed_sleepers", 27.5, -56.3, -23.5, 27.5, -60, -30);

		// Crafting Tweaks: x -30..-14, z -4..12; stations along x -20 face east, ores at x -26, frames at x -27.
		shot("crafting_overview", -9, -52, 4, -22, -60, 4);
		shot("crafting_station_crafting", -16, -58.2, -3, -20, -59.6, -3);
		shot("crafting_station_blast_furnace", -16, -58.2, 1, -20, -59.6, 1);
		shot("crafting_station_smoker", -16, -58.2, 5, -20, -59.6, 5);
		shot("crafting_ore_columns", -22.6, -58.8, 3.5, -26, -59, 3.5);
		shot("crafting_ore_columns_angle", -22.8, -57.6, 7.6, -26, -59, 3.5);
		shot("crafting_paper_frames", -23.6, -58.6, 9, -26.5, -59.3, 9);
		shot("crafting_paper_frames_angle", -23.4, -58.4, 12, -26.5, -59.4, 9);

		// Glider tower: x 20..24, z 4..8, platform at y -1 with the Wings chest at (21, 0, 6).
		shot("tower_outside", -4, -44, 34, 22, -32, 6);
		shot("tower_entrance", 16.2, -58.4, 7.5, 19.5, -59.5, 5.5);
		shot("tower_top", 26.5, 3, 11.5, 21.5, 0, 6);
		shot("tower_top_glide_sign", 21.5, 1.2, 5.75, 21.5, 0.4, 4.5);
		shot("tower_top_glider_sign", 21.5, 1.2, 7.25, 21.5, 0.4, 8.5);

		// Infused Gems: x 30..46, z 2..14; signs at z 5 face north over the chest row at z 6.
		shot("gems_overview", 38.5, -49, -5, 38.5, -60, 8);
		shot("gems_row_brew", 33, -58.2, 1.4, 33, -59.6, 5.5);
		shot("gems_row_enchant", 38.5, -58.2, 1.4, 38.5, -59.6, 5.5);
		shot("gems_row_socket", 44, -58.2, 1.4, 44, -59.6, 5.5);
		shot("gems_library_doorway", 40.5, -58.5, 6.9, 40.5, -59.5, 10.5);
		shot("gems_library_inside", 39.4, -57.9, 11.6, 40.8, -59.6, 9.2);
		shot("gems_library_above", 40.5, -53.5, 13.5, 40.5, -60, 10);
		shot("gems_target_pen", 33.5, -56.8, 7.4, 33.5, -60, 12);
		// The set signs at x 44 face west with the library wall at x 42: read them from inside that wall
		// (a spectator sees through the block it is in).
		shot("gems_set_signs", 42.2, -58.3, 11, 44.5, -59.5, 11);
		shot("gems_set_chests_above", 43, -55, 11, 45.5, -60, 11);

		chest("gems_chest_best_build", 37, -60, 6);
		chest("gems_chest_ready_made_gems", 38, -60, 6);
		chest("gems_chest_tank_set", 45, -60, 9);
		chest("gems_chest_diver_set", 45, -60, 10);
		chest("gems_chest_healer_set", 45, -60, 11);
		chest("gems_chest_run_test", 45, -60, 12);
		chest("gems_chest_weapons", 45, -60, 13);
		chest("gems_chest_drawback_gear", 46, -60, 9);

		grounds();
	}

	/** Gem Testing Grounds, south of the hub (x -47..46, z 11..63; specs/testworld.md, Layout). */
	private void grounds() {
		shot("grounds_hub_signs", 0, -58.4, 7.5, 0, -59.5, 11.5);
		shot("grounds_hub_west_sign", -7, -58.6, 6.2, -7, -59.5, 3.5);
		shot("grounds_overview", 0.5, -25, -2, 0.5, -60, 38);
		shot("grounds_overview_west", -24, -30, 8, -24, -60, 40);
		shot("grounds_overview_east", 26, -30, 8, 26, -60, 40);

		// Wardrobe: aisle x -3..3, z 18..32; wall signs at x -3 (helpful, face east) and x 3 (harmful, materials, tools).
		shot("wardrobe_entrance", 0.5, -58.2, 14.5, 0.5, -59, 26);
		for (double z : new double[] {21, 25.5, 29.5}) {
			shot("wardrobe_west_z" + (int) z, -0.8, -58.5, z, -3.5, -58.6, z);
			shot("wardrobe_east_z" + (int) z, 1.8, -58.5, z, 4.5, -58.6, z);
		}
		wardrobeClick();
		shot("stands", 0.5, -57.6, 31.5, 0.5, -59.4, 37);

		// Arrow gallery x -44..-30, z 19..31; Live range x -46..-32, z 37..47.
		shot("gallery_overview", -37, -50, 13, -37, -60, 25);
		shot("gallery_kit_signs", -37, -58.4, 15.6, -37, -59, 18.5);
		shot("gallery_spot_lever", -37, -58.2, 23.4, -36.5, -59.5, 20);
		shot("gallery_front_dispenser", -36.5, -58.4, 22.5, -36.5, -59, 30);
		galleryLever();
		shot("range_overview", -39, -47, 34, -39, -58, 44);
		shot("range_signs", -40, -58.4, 41.2, -40, -59, 38.5);
		shot("range_turrets", -39.5, -58.4, 39.5, -39.5, -57, 45.5);

		// Drop pads x -22..-10 (pillars at z 23), jump steps x -13..-7 (z 40), cobweb field x -23..-17 (z 38..46).
		shot("drops_overview", -16, -48, 12, -16, -52, 26);
		shot("drops_signs", -18, -58.4, 16.2, -18, -59.5, 20);
		shot("drops_top_24", -10, -34.4, 25.3, -10, -60, 31);
		shot("jumps_webs", -16, -55, 31, -16, -60, 41);
		shot("webs_signs", -20, -58.4, 33.5, -20, -59.5, 36.5);

		// Deep pool x 8..18, z 20..30; sky column x 21..25.
		shot("pool_outside", 13, -56, 12, 13, -57, 24);
		shot("pool_above", 13, -46, 25.5, 13, -60, 25.6);
		shot("pool_signs", 7.5, -58.4, 16, 7.5, -59.5, 19.5);
		shot("sky_column", 14, -44, 12, 24, -42, 23);
		shot("sky_signs", 23, -58.4, 17, 23, -59.5, 20.5);

		// Mob pen x 30..44, z 19..31 (spawn signs at z 18); dummies x 8..24, z 36..46; villagers x 30..44.
		shot("pen_overview", 37, -53, 10, 37, -59, 25);
		shot("pen_signs_west", 34.5, -58.4, 15, 34.5, -59.5, 18.5);
		shot("pen_signs_east", 41.5, -58.4, 15, 41.5, -59.5, 18.5);
		shot("dummies_overview", 16, -55, 31, 16, -60, 43);
		shot("dummies_signs_west", 12, -58.4, 34, 12, -59.5, 37.5);
		shot("dummies_signs_east", 19, -58.4, 34, 19, -59.5, 37.5);
		shot("villagers", 37, -57.4, 34, 37, -59.5, 42);
		dummiesReset();

		// Row 3 (z 52..63): sculk hall x -42..-38, lava moat x -28..-20, dark room x -12..-4, food bench x 0..8, mining wall x 12..22.
		shot("sculk_overview", -40, -53, 46, -40, -60, 58);
		shot("sculk_signs", -40, -58.4, 48.6, -40, -59.5, 52.5);
		shot("sculk_inside", -40, -58.4, 53.5, -40, -59.5, 61);
		shot("lava_overview", -24, -53, 47, -24, -61, 57);
		shot("lava_signs", -23.5, -58.4, 49.6, -23.5, -59.5, 53.5);
		shot("dark_signs", -7, -58.4, 48.6, -7, -59.5, 52.5);
		shot("dark_inside_exit_sign", -7.5, -58.4, 56.5, -7.8, -58.6, 54);
		shot("dark_inside_zombie", -7.5, -58.4, 55.5, -7.5, -59, 59.5);
		shot("food_bench", 4.5, -58, 49.5, 4.5, -59.5, 54);
		shot("mining_wall", 17, -56.4, 47, 17, -59, 55);
		chest("mining_tools_chest", 16, -60, 53);
	}

	/**
	 * Clicks the wardrobe's Speed sign as the player (the sign's own click path, as a right click runs it) and checks the
	 * worn chestplate got gems; then a third-person picture of the player in it, and the Take off sign empties the slot.
	 */
	private void wardrobeClick() {
		world.getServer().runCommand("gamemode creative @a");
		world.getServer().runCommand("tp @a 0.5 -60 19.5 90 0");
		context.waitTicks(5);
		clickSign(-3, -58, 19);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
			boolean gems = chest.getComponents().keySet().stream()
					.anyMatch(t -> "bubbas_infused_gems:socketed_gems".equals(String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(t))));
			if (!chest.is(Items.NETHERITE_CHESTPLATE) || !gems) {
				throw new AssertionError("wardrobe Speed sign did not put on a gemmed netherite chestplate: " + chest);
			}
		});
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(5);
		world.getConnection().waitForChunksRender();
		context.waitTicks(5);
		context.takeScreenshot("wardrobe_speed_set_worn");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		clickSign(-3, -59, 25);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			for (Item tool : new Item[] {Items.NETHERITE_PICKAXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_AXE}) {
				if (!player.getInventory().hasAnyMatching(stack -> stack.is(tool))) {
					throw new AssertionError("wardrobe Haste sign did not give " + tool);
				}
			}
		});
		clickSign(3, -59, 27);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			if (!player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() || !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
				throw new AssertionError("wardrobe Take off sign left armor on");
			}
			player.getInventory().clearContent();
		});
		world.getServer().runCommand("gamemode spectator @a");
		context.waitTicks(2);
	}

	/**
	 * Flips the gallery lever on (as a player's click does), checks the command block clock under the floor fires the
	 * dispensers (arrows in the air), screenshots one in flight, and flips it off again.
	 */
	private void galleryLever() {
		BlockPos lever = new BlockPos(-35, -60, 20);
		world.getServer().runOnServer(server -> {
			BlockState state = server.overworld().getBlockState(lever);
			((LeverBlock) state.getBlock()).pull(state, server.overworld(), lever, null);
		});
		context.waitTicks(30);
		world.getServer().runOnServer(server -> {
			if (server.overworld().getEntitiesOfClass(AbstractArrow.class, new AABB(-46, -62, 18, -29, -55, 33)).isEmpty()) {
				throw new AssertionError("gallery lever on, but no dispenser fired after 30 ticks");
			}
		});
		shot("gallery_arrows_flying", -33, -57, 25.5, -37, -59, 25);
		world.getServer().runOnServer(server -> {
			BlockState state = server.overworld().getBlockState(lever);
			((LeverBlock) state.getBlock()).pull(state, server.overworld(), lever, null);
		});
	}

	/** Kills a dummy, clicks the Reset dummies sign, and checks all five are back (once each) and the trigger is gone. */
	private void dummiesReset() {
		world.getServer().runCommand("kill @e[tag=tw_d_pig]");
		context.waitTicks(2);
		world.getServer().runCommand("gamemode creative @a");
		clickSign(21, -60, 37);
		context.waitTicks(3);
		world.getServer().runOnServer(server -> {
			int n = server.overworld().getEntities(EntityTypeTest.forClass(Entity.class), e -> e.isAlive() && e.entityTags().contains("tw_dummy")).size();
			if (n != 5) {
				throw new AssertionError("Reset dummies left " + n + " dummies, expected 5");
			}
			if (!server.overworld().getBlockState(new BlockPos(9, -61, 39)).is(Blocks.GRASS_BLOCK)) {
				throw new AssertionError("Reset dummies left its redstone block in the floor");
			}
		});
		world.getServer().runCommand("gamemode spectator @a");
	}

	/** Runs a sign's click commands for the player, as a right click on its front does. */
	private void clickSign(int x, int y, int z) {
		BlockPos pos = new BlockPos(x, y, z);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			if (!(server.overworld().getBlockEntity(pos) instanceof SignBlockEntity sign)
					|| !sign.executeClickCommandsIfPresent(server.overworld(), player, pos, SignTextSlot.FRONT)) {
				throw new AssertionError("no click sign at " + pos.toShortString());
			}
		});
		context.waitTicks(3);
	}

	/** One graveyard row (z): each half (sitting, lying) from the south and from the north. */
	private void rowShots(String kind, double z) {
		for (Object[] half : new Object[][] {{"sitting", -37.5}, {"lying", -25.5}}) {
			double x = (double) half[1];
			shot("graveyard_" + kind + "_" + half[0] + "_from_south", x, -56.4, z + 4.6, x, -60, z);
			shot("graveyard_" + kind + "_" + half[0] + "_from_north", x, -56.4, z - 4.6, x, -60, z);
		}
	}

	/** Puts the camera (eye) at ex,ey,ez looking at tx,ty,tz, waits for the view to render, screenshots. */
	private void shot(String name, double ex, double ey, double ez, double tx, double ty, double tz) {
		double dx = tx - ex, dy = ty - ey, dz = tz - ez;
		double yaw = Math.toDegrees(Math.atan2(-dx, dz));
		double pitch = Math.toDegrees(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		world.getServer().runCommand(String.format(Locale.ROOT, "tp @a %.3f %.3f %.3f %.2f %.2f", ex, ey - EYE, ez, yaw, pitch));
		context.waitTicks(2);
		world.getConnection().waitForChunksRender();
		context.waitTicks(5);
		context.takeScreenshot(name);
	}

	/** Opens the chest at x,y,z (the player stands next to it so the menu stays valid) and screenshots its menu. */
	private void chest(String name, int x, int y, int z) {
		world.getServer().runCommand(String.format(Locale.ROOT, "tp @a %.1f %d %.1f", x - 1.5, y, z + 0.5));
		context.waitTicks(2);
		BlockPos pos = new BlockPos(x, y, z);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			if (!(server.overworld().getBlockEntity(pos) instanceof MenuProvider menu)) {
				throw new AssertionError("no chest at " + pos.toShortString() + " for " + name);
			}
			player.openMenu(menu);
		});
		context.waitForScreen(ContainerScreen.class);
		context.getInput().setCursorPos(0, 0);
		context.waitTicks(3);
		context.takeScreenshot(name);
		// Hover the first slot so its tooltip (gems socketed in it, effects) shows.
		double[] slot = context.computeOnClient(TestWorldShots::firstSlotCenter);
		context.getInput().setCursorPos(slot[0], slot[1]);
		context.waitTicks(2);
		context.takeScreenshot(name + "_tooltip");
		context.getInput().setCursorPos(0, 0);
		world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().closeContainer());
		context.waitForScreen(null);
	}

	/** Window pixel position of the centre of the open container's first slot. */
	private static double[] firstSlotCenter(Minecraft mc) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.gui.screen();
		Slot slot = screen.getMenu().slots.getFirst();
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

	/** Copies the pristine world into the run's saves folder (never opened in place, specs/testworld.md). */
	private static void copyWorld(Path from, Path to) {
		if (!Files.isRegularFile(from.resolve("level.dat"))) {
			throw new AssertionError("no test world at " + from.toAbsolutePath() + " (python tools/build-testworld.py)");
		}
		try (Stream<Path> files = Files.walk(from)) {
			for (Path f : (Iterable<Path>) files::iterator) {
				Path target = to.resolve(from.relativize(f).toString());
				if (Files.isDirectory(f)) {
					Files.createDirectories(target);
				} else if (!f.getFileName().toString().equals("session.lock")) {
					Files.copy(f, target, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
