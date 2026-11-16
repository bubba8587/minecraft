// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import io.github.bubba8587.bubbas_reworked_mobs.creeper.CoverFinder;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.CreeperAccess;
import io.github.bubba8587.bubbas_reworked_mobs.creeper.CreeperMind;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Looks at the watcher creeper behind a tree (specs/creepers.md): an armor stand (something watchable that never
 * looks back) stands 11 blocks from a one-log tree with the creeper behind it. Screenshots:
 * creeper-peek (from just behind the stand: half behind the trunk, eyes out), creeper-peek-side (from the side, its
 * place beside the trunk); then the player takes the stand's place: creeper-hidden (looked at: ~95 % behind the
 * trunk), and creeper-caught (the player looks away until it eases back out, then turns straight back).
 */
@SuppressWarnings("UnstableApiUsage")
public class CreeperClientGameTest implements FabricClientGameTest {
	private static final String TAG = "creeper_shot";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("creepers")) {
			return;
		}
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
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("time set noon");
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 12, y, z - 6, x + 12, y + 8, z + 24));
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 12, y - 1, z - 6, x + 12, y - 1, z + 24));
			// A one-log oak, 8 blocks out, with a small crown.
			server.runCommand("fill %d %d %d %d %d %d minecraft:oak_leaves[persistent=true]".formatted(x - 2, y + 3, z + 6, x + 2, y + 5, z + 10));
			server.runCommand("fill %d %d %d %d %d %d minecraft:oak_log".formatted(x, y, z + 8, x, y + 4, z + 8));
			server.runCommand("summon minecraft:armor_stand %d.5 %d %d.5 {Rotation:[0f,0f],Tags:[\"%s_stand\"]}".formatted(x, y, z - 1, TAG));
			server.runCommand("summon minecraft:creeper %d.5 %d %d.5 {Silent:1b,PersistenceRequired:1b,Rotation:[180f,0f],Tags:[\"%s\"]}".formatted(x, y, z + 12, TAG));
			server.runCommand("gamemode spectator @p");
			context.waitTicks(2);
			// The creeper watches the stand.
			onCreeper(server, (creeper, stand) -> {
				CreeperAccess.mind(creeper).watch(stand, creeper.level().getGameTime());
				return null;
			});
			waitAt(context, server, true);
			tp(context, server, x + 0.5, y + 0.45, z - 0.5, 0.0F, 2.0F);
			context.takeScreenshot("creeper-peek");
			tp(context, server, x + 7.5, y + 0.2, z + 9.5, 90.0F, 4.0F);
			context.takeScreenshot("creeper-peek-side");

			// The player takes the stand's place and looks right at the tree.
			server.runCommand("kill @e[tag=%s_stand]".formatted(TAG));
			server.runCommand("gamemode creative @p");
			context.runOnClient(mc -> {
				mc.player.getInventory().setSelectedSlot(8);
				mc.player.getAbilities().flying = false;
				mc.player.onUpdateAbilities();
			});
			tp(context, server, x + 0.5, y, z - 0.5, 0.0F, 0.0F);
			onCreeper(server, (creeper, stand) -> {
				LivingEntity player = creeper.level().players().getFirst();
				CreeperAccess.mind(creeper).watch(player, creeper.level().getGameTime());
				return null;
			});
			waitAt(context, server, false);
			context.waitTicks(4);
			context.takeScreenshot("creeper-hidden");
			// Look away (east) until it eases back out, then turn straight back.
			tp(context, server, x + 0.5, y, z - 0.5, -90.0F, 0.0F);
			waitAt(context, server, true);
			server.runCommand("tp @p %.2f %d %.2f 0 0".formatted(x + 0.5, y, z - 0.5));
			context.waitFor(mc -> Math.abs(net.minecraft.util.Mth.wrapDegrees(mc.player.getYRot())) < 1.0F);
			context.takeScreenshot("creeper-caught");
		}
	}

	private static <T> T onCreeper(TestServerContext server, java.util.function.BiFunction<Creeper, LivingEntity, T> action) {
		return server.computeOnServer(s -> {
			Creeper creeper = null;
			LivingEntity stand = null;
			for (Entity e : s.overworld().getAllEntities()) {
				if (e instanceof Creeper c && e.entityTags().contains(TAG)) {
					creeper = c;
				} else if (e instanceof LivingEntity l && e.entityTags().contains(TAG + "_stand")) {
					stand = l;
				}
			}
			return action.apply(creeper, stand);
		});
	}

	/** Waits until the creeper has reached its spot and sits at its half-hidden ({@code half}) or hidden place. */
	private static void waitAt(ClientGameTestContext context, TestServerContext server, boolean half) {
		for (int i = 0; i < 400; i++) {
			String state = onCreeper(server, (creeper, stand) -> {
				CreeperMind mind = CreeperAccess.mind(creeper);
				CoverFinder.PeekSpot spot = mind.spot();
				if (spot == null || !mind.spotReached()) {
					return null;
				}
				double d = creeper.position().distanceTo(half ? spot.half() : spot.hidden());
				return d < 0.05 ? "at %s, spot %s".formatted(creeper.position(), spot) : null;
			});
			if (state != null) {
				System.out.println("[creeper-shot] " + (half ? "half " : "hidden ") + state);
				return;
			}
			context.waitTick();
		}
		System.out.println("[creeper-shot] stuck: " + onCreeper(server, (creeper, stand) -> {
			CreeperMind mind = CreeperAccess.mind(creeper);
			return "pos %s watched %s spot %s reached %s goals %s".formatted(creeper.position(), mind.watched(), mind.spot(), mind.spotReached(),
					creeper.getGoalSelector().getAvailableGoals().stream().filter(g -> g.isRunning()).map(g -> g.getGoal().getClass().getSimpleName()).toList());
		}));
		throw new AssertionError("creeper never reached its " + (half ? "half-hidden" : "hidden") + " place");
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand("tp @p %.2f %.2f %.2f %.1f %.1f".formatted(x, y, z, yaw, pitch));
		context.waitFor(mc -> Math.abs(mc.player.getX() - x) < 0.01 && Math.abs(mc.player.getZ() - z) < 0.01);
		context.waitTicks(2);
	}
}
