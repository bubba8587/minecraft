// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.List;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Looks at the woken undead walking (specs/undead.md "Tuning"): our zombies shamble (arms down, left leg dragging),
 * our skeletons walk with arms out like vanilla zombies, next to a vanilla zombie for comparison. The mobs have no AI
 * and are moved a little every tick from the server, so the client animates a real walk at about our zombies' speed.
 * Several frames across a step cycle from the side, then from the front. Screenshots: gait-*.png.
 */
@SuppressWarnings("UnstableApiUsage")
public class UndeadGaitClientGameTest implements FabricClientGameTest {
	private static final String ID = "bubbas_reworked_mobs";
	private static final String TAG = "gait";
	/** Blocks per tick: about what our slow undead manage. */
	private static final double STEP = 0.06;

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
			server.runCommand("gamerule send_command_feedback false");
			// Night (undead burn in daylight, and the flames hide the animation), with night vision so it's bright.
			server.runCommand("time set midnight");
			server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 2, y, z + 1, x + 22, y + 4, z + 16));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 2, y - 1, z + 1, x + 22, y - 1, z + 16));
			// One file walking east (+x), 2.5 blocks apart: plain zombie, armored zombie, skeleton, armored skeleton with a
			// sword, vanilla zombie (for comparison).
			List<String> mobs = List.of(ID + ":woken_zombie", ID + ":woken_zombie", ID + ":woken_skeleton", ID + ":woken_skeleton", "minecraft:zombie");
			List<String> gear = List.of("",
					"equipment:{head:{id:\"minecraft:iron_helmet\"},chest:{id:\"minecraft:iron_chestplate\"},legs:{id:\"minecraft:iron_leggings\"},feet:{id:\"minecraft:iron_boots\"}},",
					"",
					"equipment:{chest:{id:\"minecraft:golden_chestplate\"},legs:{id:\"minecraft:chainmail_leggings\"},mainhand:{id:\"minecraft:iron_sword\"}},",
					"");
			for (int i = 0; i < mobs.size(); i++) {
				server.runCommand("summon %s %.1f %d %.1f {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:[\"%s%d\",\"%s\"],%sRotation:[270f,0f]}"
						.formatted(mobs.get(i), x + 0.5 + i * 2.5, y, z + 6.5, TAG, i, TAG, gear.get(i)));
			}
			// (Entity tags are server-side only; on the client count the mobs.)
			context.waitFor(mc -> mc.level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, mc.player.getBoundingBox().inflate(30)).size() == mobs.size());
			context.runOnClient(mc -> {
				mc.player.getInventory().setSelectedSlot(8);
				mc.player.getAbilities().flying = true;
				mc.player.onUpdateAbilities();
			});

			// Side view: south of the file, looking north. Frames 5 ticks apart cover about one full step cycle.
			tp(context, server, x + 8.0, y + 0.2, z + 14.5, 180.0F, 6.0F);
			walk(context, server, 30, null); // up to walking speed
			for (int frame = 0; frame < 8; frame++) {
				walk(context, server, 5, "gait-side-" + frame);
			}
			// Front: line them up abreast (z) and look back west at their faces from ahead.
			server.runOnServer(s -> {
				ServerLevel level = s.overworld();
				for (Entity e : level.getAllEntities()) {
					for (int i = 0; i < mobs.size(); i++) {
						if (e.entityTags().contains(TAG + i)) {
							e.setPos(x + 6.5, y, z + 2.5 + i * 2.5);
						}
					}
				}
			});
			tp(context, server, x + 16.0, y + 0.6, z + 7.5, 90.0F, 6.0F);
			walk(context, server, 20, null);
			for (int frame = 0; frame < 4; frame++) {
				walk(context, server, 5, "gait-front-" + frame);
			}
			// Close side view of the two woken zombies (does the armor follow, do the feet stay on the ground?).
			server.runOnServer(s -> {
				for (Entity e : s.overworld().getAllEntities()) {
					for (int i = 0; i < mobs.size(); i++) {
						if (e.entityTags().contains(TAG + i)) {
							e.setPos(x + 1.5 + i * 1.6, y, z + 6.5);
						}
					}
				}
			});
			tp(context, server, x + 3.5, y + 0.3, z + 10.5, 180.0F, 5.0F);
			walk(context, server, 20, null);
			for (int frame = 0; frame < 4; frame++) {
				walk(context, server, 4, "gait-close-" + frame);
			}
		}
	}

	/** Moves every tagged mob {@link #STEP} east per tick for {@code ticks} ticks, then takes a screenshot. */
	private static void walk(ClientGameTestContext context, TestServerContext server, int ticks, String shot) {
		for (int t = 0; t < ticks; t++) {
			server.runOnServer(s -> {
				for (ServerLevel level : s.getAllLevels()) {
					for (Entity e : level.getAllEntities()) {
						if (e.entityTags().contains(TAG) && e instanceof LivingEntity mob) {
							mob.setPos(mob.getX() + STEP, mob.getY(), mob.getZ());
							mob.setYRot(270.0F);
							mob.setYBodyRot(270.0F);
							mob.setYHeadRot(270.0F);
						}
					}
				}
			});
			context.waitTick();
		}
		if (shot != null) {
			context.takeScreenshot(shot);
		}
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand("tp @p %.2f %.2f %.2f %.1f %.1f".formatted(x, y, z, yaw, pitch));
		context.waitFor(mc -> Math.abs(mc.player.getX() - x) < 0.01 && Math.abs(mc.player.getZ() - z) < 0.01);
		context.waitTicks(2);
	}
}
