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
 * close up; the sleeper items in the hotbar; a sleeper placed from its item; and a woken zombie in armor.
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
		}
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
