// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.client.undead.WokenZombieRenderer;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadTuning;
import io.github.bubba8587.bubbas_reworked_mobs.undead.ZombieGait;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Looks at the woken undead walking (specs/undead.md "Tuning", walk animations) with their own AI: each mob chases a
 * pig standing still far to the east, so the zombies' real drag-and-lurch speed pulses are on screen, not a constant
 * push. Our zombies (plain and armored) shamble: drag... drag... LURCH. Our skeletons walk with arms out like
 * vanilla zombies; a vanilla zombie walks alongside for comparison.
 * <p>
 * Also measures how far the client's visible motion runs behind the server's phase (printed as {@code [gait-lag]}),
 * which is what {@code WokenZombieRenderer.CLIENT_LAG_TICKS} should be.
 * <p>
 * Screenshots: gait-side-0..9 (one full cycle, 3 ticks apart, close from the side), gait-front-0..2, gait-file-0..1.
 */
@SuppressWarnings("UnstableApiUsage")
public class UndeadGaitClientGameTest implements FabricClientGameTest {
	private static final String ID = "bubbas_reworked_mobs";
	private static final String TAG = "gait";

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 4, y, z - 6, x + 40, y + 5, z + 20));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 4, y - 1, z - 6, x + 40, y - 1, z + 20));
			context.runOnClient(mc -> {
				mc.player.getInventory().setSelectedSlot(8);
				mc.player.getAbilities().flying = true;
				mc.player.onUpdateAbilities();
			});

			// Close side view from the south: a plain woken zombie walking east in the near lane, an armored one behind it.
			int near = z + 9;
			int far = z + 5;
			Mob plain = summon(context, server, ID + ":woken_zombie", "", x + 1.5, y, near + 0.5, x + 36.5);
			summon(context, server, ID + ":woken_zombie", ARMOR, x - 0.5, y, far + 0.5, x + 36.5);
			int plainId = plain.getId();
			measureLag(context, plainId);
			// South of the plain zombie, a little low, ahead of it by half a cycle's walk.
			// (Positioned near the end of a cycle, so the frames start soon after and the zombie is still beside us.)
			context.waitFor(mc -> phaseTick(mc.level.getEntity(plainId), mc.level.getGameTime()) == 20, 200);
			double camX = context.computeOnClient(mc -> mc.level.getEntity(plainId).getX()) + 1.6;
			tp(context, server, camX, y + 0.2, near + 3.2, 180.0F, 16.0F);
			// One frame every 3 ticks of the (lagged) cycle from the start of a drag, so frames 0..9 run drag ... LURCH.
			// (A screenshot takes a couple of ticks, so each frame waits for its own point in the cycle.)
			context.waitFor(mc -> phaseTick(mc.level.getEntity(plainId), mc.level.getGameTime()) == UndeadTuning.GAIT_PERIOD_TICKS - 1, 200);
			for (int frame = 0; frame < 10; frame++) {
				int f = frame;
				context.waitFor(mc -> phaseTick(mc.level.getEntity(plainId), mc.level.getGameTime()) == f * 3, 200);
				context.runOnClient(mc -> System.out.printf("[gait-frame] side-%d cycle %.2f%n", f, lagged(mc.level.getEntity(plainId), mc.level.getGameTime())));
				context.takeScreenshot("gait-side-" + frame);
			}

			// Front: stand ahead of the plain zombie, low, looking back west at it.
			double frontX = context.computeOnClient(mc -> mc.level.getEntity(plainId).getX()) + 4.0;
			tp(context, server, frontX, y + 0.5, near + 0.5, 90.0F, 6.0F);
			context.waitFor(mc -> lagged(mc.level.getEntity(plainId), mc.level.getGameTime()) < 0.05F, 200);
			int[] frontAt = {12, 6, 6}; // late drag, mid lurch, settling
			for (int frame = 0; frame < frontAt.length; frame++) {
				context.waitTicks(frontAt[frame]);
				int f = frame;
				context.runOnClient(mc -> System.out.printf("[gait-frame] front-%d cycle %.2f%n", f, lagged(mc.level.getEntity(plainId), mc.level.getGameTime())));
				context.takeScreenshot("gait-front-" + frame);
			}

			// The whole file, wider, from the south: the zombies, then behind them a woken skeleton, an armored woken
			// skeleton with a sword and a vanilla zombie.
			double fileStart = context.computeOnClient(mc -> mc.level.getEntity(plainId).getX()) - 3.0;
			summon(context, server, ID + ":woken_skeleton", "", fileStart, y, z + 1.5, x + 36.5);
			summon(context, server, ID + ":woken_skeleton",
					"equipment:{chest:{id:\"minecraft:golden_chestplate\"},legs:{id:\"minecraft:chainmail_leggings\"},mainhand:{id:\"minecraft:iron_sword\"}},",
					fileStart, y, z - 1.5, x + 36.5);
			summon(context, server, "minecraft:zombie", "", fileStart, y, z - 4.5, x + 36.5);
			context.waitTicks(40);
			double fileX = context.computeOnClient(mc -> mc.level.getEntity(plainId).getX());
			tp(context, server, fileX, y + 3.0, near + 7.0, 180.0F, 18.0F);
			context.takeScreenshot("gait-file-0");
			context.waitTicks(12);
			context.takeScreenshot("gait-file-1");
		}
	}

	private static final String ARMOR =
			"equipment:{head:{id:\"minecraft:iron_helmet\"},chest:{id:\"minecraft:iron_chestplate\"},legs:{id:\"minecraft:iron_leggings\"},feet:{id:\"minecraft:iron_boots\"}},";

	/** The zombie's cycle as the renderer shows it (lagged), at the start of this client tick. */
	private static float lagged(Entity e, long gameTime) {
		return ZombieGait.cycle(e.getId(), gameTime - WokenZombieRenderer.CLIENT_LAG_TICKS, 0.0F);
	}

	private static int phaseTick(Entity e, long gameTime) {
		return Math.round(lagged(e, gameTime) * UndeadTuning.GAIT_PERIOD_TICKS) % UndeadTuning.GAIT_PERIOD_TICKS;
	}

	/**
	 * Summons a mob with its own AI facing east and gives it a still, invulnerable pig to chase at {@code targetX} in
	 * its lane. Returns the server's mob.
	 */
	private static Mob summon(ClientGameTestContext context, TestServerContext server, String type, String gear, double x, int y, double z, double targetX) {
		String tag = TAG + "_" + type.replace(':', '_') + "_" + (int) z + "_" + (int) x;
		server.runCommand("summon minecraft:pig %.1f %d %.1f {NoAI:1b,Silent:1b,PersistenceRequired:1b,attributes:[{id:\"minecraft:max_health\",base:1000.0d}],Health:1000.0f,Tags:[\"%s_pig\"]}"
				.formatted(targetX, y, z, tag));
		server.runCommand("summon %s %.1f %d %.1f {Silent:1b,PersistenceRequired:1b,Tags:[\"%s\",\"%s\"],%sRotation:[270f,0f]}"
				.formatted(type, x, y, z, tag, TAG, gear));
		return server.computeOnServer(s -> {
			Mob mob = null;
			LivingEntity pig = null;
			for (ServerLevel level : s.getAllLevels()) {
				for (Entity e : level.getAllEntities()) {
					if (e.entityTags().contains(tag) && e instanceof Mob m) {
						mob = m;
					} else if (e.entityTags().contains(tag + "_pig") && e instanceof LivingEntity l) {
						pig = l;
					}
				}
			}
			System.out.printf("[gait-summon] %s at %.1f %.1f, target %s%n", type, x, z, pig);
			mob.setTarget(pig);
			return mob;
		});
	}

	/**
	 * Over 4 cycles, finds where in the cycle (unlagged) the client sees the zombie move fastest. The renderer's
	 * planted-foot push is fastest in the middle of the lurch, so the lag that lines them up is printed.
	 */
	private static void measureLag(ClientGameTestContext context, int id) {
		context.waitTicks(40); // up to speed
		int period = UndeadTuning.GAIT_PERIOD_TICKS;
		List<double[]> samples = new ArrayList<>();
		for (int t = 0; t < period * 4; t++) {
			samples.add(context.computeOnClient(mc -> {
				Entity e = mc.level.getEntity(id);
				return new double[] {e.getX(), ZombieGait.cycle(id, mc.level.getGameTime(), 0.0F)};
			}));
			context.waitTick();
		}
		double[] byPhase = new double[period];
		for (int i = 1; i < samples.size(); i++) {
			int bucket = Math.round((float) (samples.get(i)[1] * period)) % period;
			byPhase[bucket] += samples.get(i)[0] - samples.get(i - 1)[0];
		}
		int peak = 0;
		StringBuilder trace = new StringBuilder();
		for (int b = 0; b < period; b++) {
			trace.append("%.3f ".formatted(byPhase[b] / 4.0));
			if (byPhase[b] > byPhase[peak]) {
				peak = b;
			}
		}
		float lurchMiddle = UndeadTuning.GAIT_DRAG_FRACTION + (1.0F - UndeadTuning.GAIT_DRAG_FRACTION) / 2.0F;
		float lag = Math.floorMod(Math.round(peak - lurchMiddle * period), period);
		System.out.printf("[gait-lag] client moves fastest at phase tick %d; lag to line up with the lurch: %.0f ticks; per phase tick: %s%n",
				peak, lag, trace);
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand("tp @p %.2f %.2f %.2f %.1f %.1f".formatted(x, y, z, yaw, pitch));
		context.waitFor(mc -> Math.abs(mc.player.getX() - x) < 0.01 && Math.abs(mc.player.getZ() - z) < 0.01);
		context.waitTicks(2);
	}
}
