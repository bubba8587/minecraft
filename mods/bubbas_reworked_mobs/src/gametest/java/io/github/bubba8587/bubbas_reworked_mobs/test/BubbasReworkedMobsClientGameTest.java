// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModEntities;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Looks at the sleepers: a sitting skeleton holding a sword, a lying zombie in armor, from the front, the side and
 * close up; the sleeper items in the hotbar; a sleeper placed from its item; a woken zombie in armor; and loot on display.
 * Screenshots land in build/run/clientGameTest/screenshots/.
 */
@SuppressWarnings("UnstableApiUsage")
public class BubbasReworkedMobsClientGameTest implements FabricClientGameTest {
	private static final String ID = "bubbas_reworked_mobs";

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			BlockPos base = context.computeOnClient(mc -> mc.player.blockPosition());
			int x = base.getX();
			int y = base.getY();
			int z = base.getZ();
			server.runCommand("gamerule send_command_feedback false"); // keep chat out of the screenshots
			server.runCommand("time set noon");
			server.runCommand("gamerule spawn_mobs false");
			// sensitivity 0: our own footsteps must not wake them while we look.
			BlockPos skeleton = base.offset(0, 0, 4);
			BlockPos zombie = base.offset(3, 0, 4);
			server.runCommand(cmd("setblock %d %d %d " + ID + ":skeleton_sleeper[facing=north,pose=sitting]"
					+ "{sensitivity:0f,Items:[{Slot:0b,id:\"minecraft:iron_sword\",count:1}]}", skeleton));
			server.runCommand(cmd("setblock %d %d %d " + ID + ":zombie_sleeper[facing=north,pose=lying]"
					+ "{sensitivity:0f,Items:[{Slot:2b,id:\"minecraft:iron_helmet\",count:1},{Slot:3b,id:\"minecraft:iron_chestplate\",count:1},"
					+ "{Slot:4b,id:\"minecraft:golden_leggings\",count:1},{Slot:5b,id:\"minecraft:iron_boots\",count:1}]}", zombie));
			BlockPos sittingZombie = base.offset(-1, 0, 6);
			server.runCommand(cmd("setblock %d %d %d " + ID + ":zombie_sleeper[facing=north,pose=sitting]"
					+ "{sensitivity:0f,Items:[{Slot:0b,id:\"minecraft:stone_axe\",count:1},{Slot:2b,id:\"minecraft:leather_helmet\",count:1}]}", sittingZombie));
			server.runCommand("item replace entity @p hotbar.0 with " + ID + ":skeleton_sleeper");
			server.runCommand("item replace entity @p hotbar.1 with " + ID + ":zombie_sleeper");
			context.waitFor(mc -> mc.level.getBlockState(skeleton).is(ModBlocks.sleeper(UndeadVariant.SKELETON))
					&& mc.level.getBlockState(zombie).is(ModBlocks.sleeper(UndeadVariant.ZOMBIE))
					&& mc.level.getBlockState(sittingZombie).is(ModBlocks.sleeper(UndeadVariant.ZOMBIE)));
			context.runOnClient(mc -> {
				mc.player.getInventory().setSelectedSlot(8); // empty hand, so the held item doesn't fill the view
				mc.player.getAbilities().flying = true; // hold still at camera positions in mid-air
				mc.player.onUpdateAbilities();
			});
			context.waitTicks(10);

			// Front: north of both, looking south at their faces.
			view(context, server, x + 1.5, y, z + 1.0, 0.0F, 25.0F, "sleepers-front");
			// Side: from the east, looking west along the row.
			view(context, server, x + 6.5, y + 1, z + 4.5, 90.0F, 25.0F, "sleepers-side");
			// Eye level from the side: is the sitting skeleton on the ground, legs flat?
			// (spectator, so the camera can sit below a standing player's eye height)
			server.runCommand("gamemode spectator @p");
			view(context, server, x + 2.2, y - 1.1, z + 4.5, 90.0F, 5.0F, "skeleton-side-low");
			server.runCommand("gamemode creative @p");
			// Close-up of the sitting skeleton's sword hand.
			view(context, server, x + 0.5, y + 0.4, z + 2.6, 0.0F, 30.0F, "skeleton-closeup");
			// Close-up of the lying zombie's armor, from above.
			view(context, server, x + 3.5, y + 2.2, z + 2.7, 0.0F, 55.0F, "zombie-closeup");

			// Place a skeleton sleeper from the hotbar item onto the ground in front of us (it turns to face us).
			BlockPos ground = base.offset(7, -1, 3);
			view(context, server, x + 7.5, y, z + 0.5, 0.0F, 45.0F, null);
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
			context.getInput().lookAt(ground);
			context.waitTicks(2);
			BlockPos placedAt = context.computeOnClient(mc -> ((net.minecraft.world.phys.BlockHitResult) mc.hitResult).getBlockPos().above());
			context.getInput().pressKey(o -> o.keyUse);
			context.waitFor(mc -> mc.level.getBlockState(placedAt).is(ModBlocks.sleeper(UndeadVariant.SKELETON)));
			context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(8));
			context.waitTicks(40);
			context.waitFor(mc -> mc.level.getBlockState(placedAt).is(ModBlocks.sleeper(UndeadVariant.SKELETON))); // still asleep
			view(context, server, placedAt.getX() + 0.5, placedAt.getY() + 0.6, placedAt.getZ() - 2.5, 0.0F, 15.0F, "placed-from-item");

			// A woken zombie in armor (vanilla renderer on our entity type).
			server.runCommand(cmd("summon " + ID + ":woken_zombie %d %d %d {NoAI:1b,Rotation:[180f,0f],"
					+ "equipment:{head:{id:\"minecraft:iron_helmet\"},chest:{id:\"minecraft:iron_chestplate\"},mainhand:{id:\"minecraft:iron_sword\"}}}",
					base.offset(-3, 0, 4)));
			context.waitFor(mc -> !mc.level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,
					new AABB(base.offset(-3, 0, 4)).inflate(1.0), e -> e.getType() == ModEntities.woken(UndeadVariant.ZOMBIE)).isEmpty());
			view(context, server, x - 2.5, y, z + 1.5, 0.0F, 15.0F, "woken-zombie");

			lootDisplays(context, server, base);
		}
	}

	/**
	 * Loot on display, north of the spawn: rows of the four placements (ground, buried, wall, stuck) for an iron sword,
	 * a bow and a trident, all facing south toward the camera; a sword and a bow hung on a stone wall; and a grave
	 * (dirt patch, lying zombie sleeper, a shovel and a sword buried at its head).
	 */
	private static void lootDisplays(ClientGameTestContext context, TestServerContext server, BlockPos base) {
		int x = base.getX();
		int y = base.getY();
		int z = base.getZ();
		server.runCommand("gamemode creative @p");
		server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 4, y, z - 30, x + 12, y + 5, z - 5));
		server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 4, y - 2, z - 30, x + 12, y - 1, z - 5));
		String[] items = {"iron_sword", "bow", "trident"};
		for (int row = 0; row < items.length; row++) {
			int rz = z - 8 - row * 5;
			// Columns: ground, buried (in dirt), wall (stone behind, to the north), stuck.
			server.runCommand("setblock %d %d %d minecraft:dirt".formatted(x + 2, y - 1, rz));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x + 4, y, rz - 1, x + 4, y + 1, rz - 1));
			display(server, "ground", items[row], x, y, rz);
			display(server, "buried", items[row], x + 2, y, rz);
			display(server, "wall", items[row], x + 4, y, rz);
			display(server, "stuck", items[row], x + 6, y, rz);
		}
		// A stone wall with a sword and a bow hung on it.
		int wz = z - 24;
		server.runCommand("fill %d %d %d %d %d %d minecraft:stone_bricks".formatted(x - 1, y, wz - 1, x + 3, y + 2, wz - 1));
		display(server, "wall", "iron_sword", x + 1, y + 1, wz);
		display(server, "wall", "bow", x + 2, y, wz);
		// A grave: a dirt patch, a lying zombie sleeper beside it, a shovel and a sword buried at its head.
		int gx = x + 8;
		int gz = z - 24;
		server.runCommand("fill %d %d %d %d %d %d minecraft:coarse_dirt".formatted(gx, y - 1, gz - 1, gx + 1, y - 1, gz + 1));
		server.runCommand("setblock %d %d %d minecraft:rooted_dirt".formatted(gx, y - 1, gz));
		server.runCommand(cmd("setblock %d %d %d " + ID + ":zombie_sleeper[facing=east,pose=lying]{sensitivity:0f}", new BlockPos(gx + 3, y, gz)));
		display(server, "buried", "iron_shovel", gx, y, gz - 1);
		display(server, "buried", "iron_sword", gx + 1, y, gz - 1);
		BlockPos last = new BlockPos(gx + 1, y, gz - 1);
		context.waitFor(mc -> mc.level.getBlockState(last).is(ModBlocks.LOOT_DISPLAY));
		context.waitTicks(10);

		// Sword row: front, side, close-ups.
		int sz = z - 8;
		view(context, server, x + 3.5, y + 0.2, sz + 4.5, 180.0F, 28.0F, "display-sword-front");
		view(context, server, x + 9.5, y + 0.3, sz + 0.5, 90.0F, 30.0F, "display-sword-side");
		view(context, server, x + 1.5, y - 0.4, sz + 2.0, 180.0F, 40.0F, "display-sword-close-ground-buried");
		view(context, server, x + 5.5, y - 0.4, sz + 2.0, 180.0F, 30.0F, "display-sword-close-wall-stuck");
		server.runCommand("gamemode spectator @p"); // eye level, nearly flat: does anything float or clip?
		view(context, server, x + 3.5, y - 1.3, sz + 3.5, 180.0F, 5.0F, "display-sword-low");
		server.runCommand("gamemode creative @p");
		// Bow and trident rows.
		view(context, server, x + 3.0, y + 0.2, sz - 5 + 3.6, 180.0F, 30.0F, "display-bow-front");
		view(context, server, x + 9.5, y + 0.3, sz - 5 + 0.5, 90.0F, 30.0F, "display-bow-side");
		view(context, server, x + 3.0, y + 0.2, sz - 10 + 3.6, 180.0F, 30.0F, "display-trident-front");
		view(context, server, x + 9.5, y + 0.3, sz - 10 + 0.5, 90.0F, 30.0F, "display-trident-side");
		// The wall, straight on and from the side.
		view(context, server, x + 1.5, y, wz + 3.5, 180.0F, 10.0F, "display-wall");
		view(context, server, x + 4.5, y, wz + 1.5, 125.0F, 15.0F, "display-wall-angle");
		// The grave, from the foot and from the side.
		view(context, server, gx + 1.0, y + 0.5, gz + 4.0, 180.0F, 35.0F, "display-grave");
		view(context, server, gx - 2.5, y + 0.2, gz - 0.5, 270.0F, 30.0F, "display-grave-side");
		view(context, server, gx + 1.0, y - 0.4, gz + 1.2, 180.0F, 45.0F, "display-grave-close");
	}

	private static void display(TestServerContext server, String placement, String item, int x, int y, int z) {
		server.runCommand("%s display %s minecraft:%s %d %d %d south".formatted(ID, placement, item, x, y, z));
	}

	private static String cmd(String format, BlockPos pos) {
		return format.formatted(pos.getX(), pos.getY(), pos.getZ());
	}

	private static void view(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch, String shot) {
		server.runCommand("tp @p %.2f %.2f %.2f %.1f %.1f".formatted(x, y, z, yaw, pitch));
		context.waitFor(mc -> Math.abs(mc.player.getX() - x) < 0.01 && Math.abs(mc.player.getZ() - z) < 0.01);
		context.waitTicks(8);
		if (shot != null) {
			context.takeScreenshot(shot);
		}
	}
}
