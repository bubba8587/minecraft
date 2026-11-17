// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.test;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * The look of an Ancient Fabricator (mob factory) (specs/mob-factory.md; client part {@code factories}): builds factories with the op
 * command deep under a real (default-preset) world and photographs them. A zombie factory from many angles
 * (factory-overview, -side, the dripper from below, the side and the pad, -platform, sculk close-ups, -pad, -face,
 * -warden, -office, -beam, -tower, -chains; all without night vision, as in play, except one labelled
 * -overview-nightvision); it prints block light at a few spots ("[fabricator-light]"), then one wall and overview per other mob (factory-face-creeper, ...). Run with
 * {@code -PwithBeacons} to see the paired beam (Reworked Beacons on the classpath).
 */
@SuppressWarnings("UnstableApiUsage")
public class FactoriesClientGameTest implements FabricClientGameTest {
	private static final int DEPTH_Y = -38;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("factories")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> {
					s.setWorldType(new WorldCreationUiState.WorldTypeEntry(
							s.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
					s.setSeed("20261116");
					s.setGenerateStructures(true);
					s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
				})
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set midnight");
			server.runCommand("gamemode spectator @p");
			BlockPos spawn = context.computeOnClient(mc -> mc.player.blockPosition());

			String[] mobs = { "zombie", "creeper", "spider", "skeleton", "enderman" };
			for (int i = 0; i < mobs.length; i++) {
				BlockPos o = new BlockPos(spawn.getX() + i * 160, DEPTH_Y, spawn.getZ());
				tp(context, server, o.getX() + 0.5, o.getY() + 6, o.getZ() + 16.5, 180.0F, 8.0F);
				server.runCommand("bubbas_reworked_mobs ancient_fabricator build %d %d %d %s %d 0".formatted(o.getX(), o.getY(), o.getZ(), mobs[i], 1000L + i));
				context.waitTicks(30);
				sp.getConnection().waitForChunksRender();
				context.waitTicks(20);
				if (i == 0) {
					fullTour(context, sp, server, o);
				} else {
					faceShot(context, server, o, "factory-face-" + mobs[i]);
					shot(context, server, o, 0.5, 9.0, 18.5, 180.0F, 12.0F, "factory-overview-" + mobs[i]);
				}
			}
			mechanics(context, sp, server, new BlockPos(spawn.getX(), DEPTH_Y, spawn.getZ()));
			worldgen(context, sp, server, spawn);
		}
	}

	/**
	 * The zombie Fabricator at work, seen by a creative player on the deck (a player near makes it run; creative ones
	 * don't anger the Warden): a deactivated mob on the pad mid-count (factory-countdown), the lift (factory-lift) and
	 * the mob arriving on the surface in particles (factory-arrival).
	 */
	private static void mechanics(ClientGameTestContext context, TestSingleplayerContext sp, TestServerContext server, BlockPos o) {
		server.runCommand("gamerule spawn_mobs true");
		server.runCommand("difficulty normal");
		server.runCommand("gamemode creative @p");
		context.runOnClient(mc -> {
			mc.player.getAbilities().flying = true;
			mc.player.onUpdateAbilities();
		});
		tp(context, server, o.getX() + 0.5, o.getY() + 3.5, o.getZ() + 10.5, 180.0F, 30.0F);
		sp.getConnection().waitForChunksRender();
		// Forced on, it skips the rest between counts (about a minute in play), so the shots don't wait.
		server.runOnServer(s -> {
			var site = io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorSites.nearest(s.overworld(), o, 8);
			if (site != null) {
				io.github.bubba8587.bubbas_reworked_mobs.factory.Fabricators.force(s.overworld(), site, true);
			}
		});
		boolean shot = false;
		for (int i = 0; i < 900 && !shot; i++) {
			context.waitTick();
			shot = server.computeOnServer(s -> {
				var level = s.overworld();
				var site = io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorSites.nearest(level, o, 8);
				var running = site == null ? null : io.github.bubba8587.bubbas_reworked_mobs.factory.Fabricators.running(level, site);
				return running != null && running.step() >= 5 && !level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, site.padArea(),
						io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorMobs::isDeactivated).isEmpty();
			});
		}
		System.out.println("[fabricator] countdown " + shot);
		hudlessShot(context, server, "factory-countdown");
		for (int i = 0; i < 400; i++) {
			context.waitTick();
			boolean lifting = server.computeOnServer(s -> {
				var level = s.overworld();
				var site = io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorSites.nearest(level, o, 8);
				var running = site == null ? null : io.github.bubba8587.bubbas_reworked_mobs.factory.Fabricators.running(level, site);
				return running != null && running.lifting();
			});
			if (lifting) {
				break;
			}
		}
		context.waitTicks(12);
		hudlessShot(context, server, "factory-lift");
		// Find the mob that went up, while it lies limp in its particles.
		net.minecraft.world.phys.Vec3 arrived = null;
		for (int i = 0; i < 1200 && arrived == null; i++) {
			context.waitTick();
			arrived = server.computeOnServer(s -> {
				var level = s.overworld();
				for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new net.minecraft.world.phys.AABB(o).inflate(110, 200, 110))) {
					var state = e.getAttached(io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorMobs.DEACTIVATED);
					if (state != null && state.stage() == io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorMobs.ARRIVED) {
						return e.position();
					}
				}
				return null;
			});
		}
		System.out.println("[fabricator] arrival " + arrived);
		if (arrived != null) {
			server.runCommand("gamemode spectator @p");
			// A camera spot with a clear line of sight to it (destinations are random, often among trees).
			net.minecraft.world.phys.Vec3 at = arrived;
			double[] cam = server.computeOnServer(s -> {
				var level = s.overworld();
				net.minecraft.world.phys.Vec3 eye = at.add(0, 0.8, 0);
				for (double r : new double[] { 4.0, 3.0, 5.5 }) {
					for (int k = 0; k < 16; k++) {
						double a = Math.PI * 2.0 * k / 16.0;
						net.minecraft.world.phys.Vec3 c = at.add(Math.cos(a) * r, 2.2, Math.sin(a) * r);
						if (level.getBlockState(BlockPos.containing(c)).isAir() && level.clip(new net.minecraft.world.level.ClipContext(c, eye,
								net.minecraft.world.level.ClipContext.Block.VISUAL, net.minecraft.world.level.ClipContext.Fluid.NONE,
								net.minecraft.world.phys.shapes.CollisionContext.empty())).getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
							double dx = eye.x - c.x;
							double dz = eye.z - c.z;
							float yaw = (float) (Math.atan2(-dx, dz) * 180.0 / Math.PI);
							float pitch = (float) (Math.atan2(c.y - eye.y, Math.hypot(dx, dz)) * 180.0 / Math.PI);
							return new double[] { c.x, c.y, c.z, yaw, pitch };
						}
					}
				}
				return new double[] { at.x + 0.5, at.y + 3.0, at.z + 4.0, 180.0, 32.0 };
			});
			tp(context, server, cam[0], cam[1], cam[2], (float) cam[3], (float) cam[4]);
			context.waitTicks(8);
			context.takeScreenshot("factory-arrival");
		}
		server.runCommand("gamerule spawn_mobs false");
		server.runCommand("gamemode spectator @p");
	}

	/** A shot without the hotbar: spectator for a moment (the Fabricator keeps running that long), then creative again. */
	private static void hudlessShot(ClientGameTestContext context, TestServerContext server, String name) {
		server.runCommand("gamemode spectator @p");
		context.waitTicks(2);
		context.takeScreenshot(name);
		server.runCommand("gamemode creative @p");
		context.runOnClient(mc -> {
			mc.player.getAbilities().flying = true;
			mc.player.onUpdateAbilities();
		});
	}

	/**
	 * A Fabricator built by worldgen beside a real ancient city (the one nearest spawn): its overview and the doorway
	 * where its tunnel meets the city (factory-worldgen, factory-city-door).
	 */
	private static void worldgen(ClientGameTestContext context, TestSingleplayerContext sp, TestServerContext server, BlockPos spawn) {
		System.out.println("[fabricator] " + server.computeOnServer(s -> {
			var level = s.overworld();
			var holder = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
					.getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.ANCIENT_CITY);
			var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, net.minecraft.core.HolderSet.direct(holder), spawn, 100, false);
			if (found == null) {
				return "vanilla locate: no ancient city";
			}
			BlockPos c = found.getFirst();
			int[] cell = io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorSites.cellOf(level, c.getX(), c.getZ());
			var site = io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorSites.cellSite(level, cell[0], cell[1]);
			return "natural " + io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorSites.naturalSites(level) + " structures " + level.getServer().getWorldGenSettings().options().generateStructures() + "; vanilla locate: ancient city at " + c.toShortString() + ", cell " + cell[0] + "," + cell[1] + ", our site " + site
					+ (site == null ? "" : " city " + site.world(site.cityLocal()).toShortString());
		}));
		double[] view = server.computeOnServer(s -> {
			var level = s.overworld();
			io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorSite site = null;
			for (var s2 : io.github.bubba8587.bubbas_reworked_mobs.factory.FabricatorSites.near(level, spawn.atY(0), 4000)) {
				if (s2.cityLocal() != null && (site == null || s2.origin().distSqr(spawn) < site.origin().distSqr(spawn))) {
					site = s2;
				}
			}
			if (site == null) {
				return null;
			}
			BlockPos over = site.world(new BlockPos(0, 9, 18));
			float yaw = 180.0F + 90.0F * site.rotation().ordinal();
			BlockPos city = site.world(site.cityLocal());
			double sx = site.world(new BlockPos(0, 1, 12)).getX();
			double sz = site.world(new BlockPos(0, 1, 12)).getZ();
			double dx = city.getX() - sx;
			double dz = city.getZ() - sz;
			double len = Math.hypot(dx, dz);
			double door = len - 96 - 6;
			double cx = sx + dx / len * door;
			double cz = sz + dz / len * door;
			float doorYaw = (float) (Math.atan2(-dx, dz) * 180.0 / Math.PI);
			level.getChunk(city);
			var cityStructure = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
					.getValueOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.ANCIENT_CITY);
			boolean real = level.structureManager().getStructureAt(city.atY(-30), cityStructure).isValid()
					|| level.structureManager().getStructureAt(city.atY(-45), cityStructure).isValid();
			System.out.println("[fabricator] nearest natural " + site + " city " + city.toShortString() + " pad " + site.pad().toShortString()
					+ "; a real ancient city there: " + real);
			return new double[] { over.getX() + 0.5, over.getY(), over.getZ() + 0.5, yaw, cx, site.origin().getY() + 1.5, cz, doorYaw };
		});
		if (view == null) {
			System.out.println("[fabricator] no natural Fabricator within 4000 blocks");
			return;
		}
		tp(context, server, view[0], view[1], view[2], (float) view[3], 12.0F);
		for (int i = 0; i < 6; i++) {
			sp.getConnection().waitForChunksRender();
			context.waitTicks(40);
		}
		context.takeScreenshot("factory-worldgen");
		tp(context, server, view[4], view[5], view[6], (float) view[7], 5.0F);
		sp.getConnection().waitForChunksRender();
		context.waitTicks(40);
		context.takeScreenshot("factory-city-door");
		server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
		context.waitTicks(4);
		context.takeScreenshot("factory-city-door-nightvision");
		server.runCommand("effect clear @p");
	}

	private static void fullTour(ClientGameTestContext context, TestSingleplayerContext sp, TestServerContext server, BlockPos o) {
		shot(context, server, o, 0.5, 9.0, 18.5, 180.0F, 12.0F, "factory-overview");
		shot(context, server, o, 6.5, 6.0, 12.5, 150.0F, -8.0F, "factory-side");
		shot(context, server, o, 0.5, 1.8, 3.5, 180.0F, -89.0F, "factory-dripper-below");
		shot(context, server, o, -13.5, 9.0, 3.5, -90.0F, -6.0F, "factory-dripper-side");
		shot(context, server, o, 0.5, 1.5, 10.5, 180.0F, -38.0F, "factory-dripper-pad");
		shot(context, server, o, 0.5, 5.0, 12.5, 180.0F, 30.0F, "factory-platform");
		shot(context, server, o, -3.5, 2.6, -3.5, 150.0F, 38.0F, "factory-sculk-deck");
		shot(context, server, o, 6.5, 3.0, 11.5, -140.0F, 18.0F, "factory-sculk-stairs");
		// The teleporter pad mid-count, its particles running (they run even with nothing on it: a player could ride it).
		tp(context, server, o.getX() + 0.5, o.getY() + 3.5, o.getZ() + 9.5, 180.0F, 35.0F);
		context.waitTicks(10);
		for (int i = 0; i < 4; i++) {
			server.runCommand("particle minecraft:reverse_portal %d.5 %d.2 %d.5 0.7 1.5 0.7 0.02 120 force".formatted(o.getX(), o.getY(), o.getZ() + 3));
			server.runCommand("particle minecraft:portal %d.5 %d.5 %d.5 1.0 0.2 1.0 0.4 80 force".formatted(o.getX(), o.getY(), o.getZ() + 3));
			context.waitTicks(4);
		}
		context.takeScreenshot("factory-pad");
		faceShot(context, server, o, "factory-face");
		shot(context, server, o, 8.5, 8.0, 0.5, -90.0F, 4.0F, "factory-warden");
		shot(context, server, o, -3.5, 7.0, 7.5, -125.0F, 8.0F, "factory-warden-wide");
		shot(context, server, o, 15.3, 8.6, -2.6, -42.0F, 32.0F, "factory-office");
		shot(context, server, o, 2.5, 3.0, 4.5, 122.0F, -35.0F, "factory-beam");
		shot(context, server, o, -6.5, 8.0, 10.5, 240.0F, 10.0F, "factory-tower");
		shot(context, server, o, 3.5, 3.0, 9.5, 130.0F, -28.0F, "factory-chains");
		// Block light at a few spots (owner: dark like an ancient city), printed for the report.
		System.out.println("[fabricator-light] " + server.computeOnServer(srv -> {
			net.minecraft.server.level.ServerLevel level = srv.overworld();
			StringBuilder out = new StringBuilder();
			int[][] spots = { { 0, 0, 3 }, { 0, 0, 0 }, { -7, 0, -8 }, { 7, 0, 7 }, { 0, 0, 8 }, { 0, 8, -9 }, { -6, 1, -1 },
					{ 16, 6, -1 }, { 12, 6, 0 }, { 0, 7, 3 }, { -15, -4, 6 }, { 15, -4, 12 }, { 0, 2, 20 } };
			String[] names = { "pad middle", "deck centre", "deck back-left", "deck front-right", "deck front edge", "face wall middle",
					"beside the floor beacon", "office", "window ledge", "under the dripper tip", "cavern floor west", "cavern floor east", "tunnel" };
			for (int k = 0; k < spots.length; k++) {
				BlockPos p = o.offset(spots[k][0], spots[k][1], spots[k][2]);
				out.append(names[k]).append(' ').append(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, p)).append("; ");
			}
			return out.toString();
		}));
		// One overview with night vision for detail (not how it looks in play).
		server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
		shot(context, server, o, 0.5, 9.0, 18.5, 180.0F, 12.0F, "factory-overview-nightvision");
		server.runCommand("effect clear @p");
	}

	private static void shot(ClientGameTestContext context, TestServerContext server, BlockPos o, double x, double y, double z, float yaw,
			float pitch, String name) {
		tp(context, server, o.getX() + x, o.getY() + y, o.getZ() + z, yaw, pitch);
		context.waitTicks(10);
		context.takeScreenshot(name);
	}

	/** The face wall square on, from close in front (under the short spikes) with a wide field of view. */
	private static void faceShot(ClientGameTestContext context, TestServerContext server, BlockPos o, String name) {
		context.runOnClient(mc -> mc.options.fov().set(95));
		shot(context, server, o, 0.5, 8.5, -2.5, 180.0F, 0.0F, name);
		context.runOnClient(mc -> mc.options.fov().set(70));
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand("tp @p %.2f %.2f %.2f %.1f %.1f".formatted(x, y, z, yaw, pitch));
		context.waitFor(mc -> Math.abs(mc.player.getX() - x) < 0.01 && Math.abs(mc.player.getZ() - z) < 0.01);
		context.waitTicks(2);
	}
}
