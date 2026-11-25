// dte:C1,B15,B17,B21
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.github.bubba8587.bubbas_reworked_mobs.camp.CampCommand;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampKind;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampPiece;
import io.github.bubba8587.bubbas_reworked_mobs.camp.Camps;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampTuning;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Monster camps by eye (specs/monster-camps.md; client part {@code camps}).
 * <ul>
 * <li>Built on a flat world, one of each kind and size: camp-&lt;kind&gt;-above and camp-&lt;kind&gt;-close, then a
 * skeleton camp with its roster out at noon, in the shade (camp-roster), inside its room (camp-room) and at night (camp-roster-night).</li>
 * <li>Stables, chests and the room stairs (also alone as the part {@code campextras}): camp-stable-&lt;kind&gt;-close /
 * -clearing (a stable with its mounts), camp-skull-chest, camp-buried-chest, camp-tower-chest (inside, night vision),
 * camp-stairs-up-&lt;kind&gt; (the tunnel's top from the room's doorway).</li>
 * <li>In a real world (seed 20261116): how often camps generate (printed: camps per 1000 x 1000 blocks by kind,
 * typical distance to the nearest of each kind), and the nearest natural camp (camp-natural-0-above / -close).</li>
 * </ul>
 */
@SuppressWarnings("UnstableApiUsage")
public class CampsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (ClientTests.named("campsurvey")) {
			surveyOnly(context);
		}
		boolean all = ClientTests.run("camps");
		if (!all && !ClientTests.named("campextras")) {
			return;
		}
		extras(context);
		if (all) {
			built(context);
			natural(context);
		}
	}

	private static void built(ClientGameTestContext context) {
		try (TestSingleplayerContext sp = context.worldBuilder().adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			setup(server);
			BlockPos base = context.computeOnClient(mc -> mc.player.blockPosition());
			CampKind[] kinds = java.util.Arrays.stream(CampKind.values()).filter(k -> !k.nether()).toArray(CampKind[]::new);
			CampTuning.Size[] sizes = { CampTuning.Size.LARGE, CampTuning.Size.MEDIUM, CampTuning.Size.SMALL, CampTuning.Size.MEDIUM, CampTuning.Size.MEDIUM };
			List<BlockPos> centres = new ArrayList<>();
			List<CampPiece> pieces = new ArrayList<>();
			for (int i = 0; i < kinds.length; i++) {
				BlockPos at = base.offset(i * 40, 0, 0);
				int k = i;
				tp(context, server, at.getX() + 0.5, at.getY() + 30, at.getZ() - 20.5, 0, 50);
				sp.getConnection().waitForChunksRender();
				CampPiece piece = server.computeOnServer(s -> CampCommand.build(s.overworld(), at, kinds[k], sizes[k], 100L + k));
				centres.add(piece.centre());
				pieces.add(piece);
			}
			for (int i = 0; i < kinds.length; i++) {
				BlockPos c = centres.get(i);
				tp(context, server, c.getX() + 0.5, c.getY() + 22, c.getZ() - 20.5, 0, 45);
				context.takeScreenshot("camp-" + kinds[i].id() + "-above");
				tp(context, server, c.getX() + 9.5, c.getY() + 3.5, c.getZ() - 9.5, 45, 12);
				context.takeScreenshot("camp-" + kinds[i].id() + "-close");
			}
			// More seeds of each kind, a size each, from above.
			CampTuning.Size[] all = CampTuning.Size.values();
			for (int i = 0; i < kinds.length; i++) {
				for (int j = 0; j < 3; j++) {
					BlockPos at = base.offset(i * 40, 0, 50 + j * 40);
					int k = i;
					int n = j;
					tp(context, server, at.getX() + 0.5, at.getY() + 30, at.getZ() - 20.5, 0, 50);
					sp.getConnection().waitForChunksRender();
					CampPiece extra = server.computeOnServer(s -> CampCommand.build(s.overworld(), at, kinds[k], all[n], 500L + k * 10 + n));
					BlockPos ec = extra.centre();
					tp(context, server, ec.getX() + 0.5, ec.getY() + 26, ec.getZ() - 16.5, 0, 58);
					context.takeScreenshot("camp-" + kinds[i].id() + "-seed" + j + "-" + all[j].id());
				}
			}
			// Large camps with each newer piece, two kinds each, from two angles and above.
			String[] features = { "skull", "winding", "ramp", "bridge", "buried", "ribcage" };
			CampKind[][] featureKinds = { { CampKind.SKELETON, CampKind.HUSK }, { CampKind.ZOMBIE, CampKind.STRAY }, { CampKind.BOGGED, CampKind.SKELETON },
					{ CampKind.HUSK, CampKind.ZOMBIE }, { CampKind.STRAY, CampKind.SKELETON }, { CampKind.ZOMBIE, CampKind.BOGGED } };
			for (int fi = 0; fi < features.length; fi++) {
				for (int ki = 0; ki < 2; ki++) {
					CampKind kind = featureKinds[fi][ki];
					BlockPos at = base.offset(fi * 50, 0, 200 + ki * 50);
					if (CLIENT_FEATURES != null && !CLIENT_FEATURES.contains(features[fi])) {
						continue;
					}
					int f = fi;
					long seed = 0;
					while (feature(new CampPiece(seed, at, kind, CampTuning.Size.LARGE), f) == null) {
						seed++;
					}
					long s = seed;
					tp(context, server, at.getX() + 0.5, at.getY() + 30, at.getZ() - 20.5, 0, 50);
					sp.getConnection().waitForChunksRender();
					CampPiece piece = server.computeOnServer(sv -> CampCommand.build(sv.overworld(), at, kind, CampTuning.Size.LARGE, s));
					BlockPos p = feature(piece, f);
					BlockPos c = piece.centre();
					String name = "camp-" + features[fi] + "-" + kind.id();
					tp(context, server, c.getX() + 0.5, c.getY() + 30, c.getZ() - 22.5, 0, 52);
					context.takeScreenshot(name + "-above");
					// the side the camera looks from: the skull's face, across a ramp or bridge, the tower from outside
					Vec3 toward = switch (f) {
						case 0, 4 -> Vec3.atLowerCornerOf(c.subtract(p));
						case 5 -> {
							BlockPos[] e = piece.ribcageEnds();
							yield new Vec3(e[1].getX() - e[0].getX(), 0, e[1].getZ() - e[0].getZ()).scale(-1);
						}
						case 1 -> Vec3.atLowerCornerOf(p.subtract(c));
						case 2 -> {
							BlockPos[] e = piece.rampEnds();
							yield new Vec3(-(e[0].getZ() - e[1].getZ()), 0, e[0].getX() - e[1].getX());
						}
						default -> {
							BlockPos[] e = piece.bridgeEnds();
							yield new Vec3(-(e[1].getZ() - e[0].getZ()), 0, e[1].getX() - e[0].getX());
						}
					};
					toward = new Vec3(toward.x, 0, toward.z).normalize();
					boolean skullish = f == 0 || f == 4;
					for (int a = 0; a < (skullish ? 3 : 2); a++) {
						double ang = Math.atan2(toward.z, toward.x) + (a == 1 ? 0.8 : 0.0);
						double dist = a == 2 ? 34.0 : skullish ? 18.0 : features[fi].equals("winding") ? 18.0 : 13.0;
						double ex = p.getX() + 0.5 + Math.cos(ang) * dist;
						double ez = p.getZ() + 0.5 + Math.sin(ang) * dist;
						double ey = p.getY() + (features[fi].equals("winding") ? 4.0 : 3.0);
						float yaw = (float) (Math.toDegrees(Math.atan2(p.getZ() + 0.5 - ez, p.getX() + 0.5 - ex)) - 90.0);
						tp(context, server, ex, ey, ez, yaw, 8);
						context.takeScreenshot(name + "-" + (a == 0 ? "front" : a == 1 ? "side" : "far"));
					}
				}
			}
			// The skeleton camp's roster: a creative (not spectating) player nearby makes it appear.
			BlockPos c = centres.getFirst();
			server.runCommand("gamemode creative @p");
			tp(context, server, c.getX() + 12.5, c.getY() + 5.0, c.getZ() - 12.5, 45, 15);
			context.waitTicks(40);
			context.takeScreenshot("camp-roster");
			// The lean-to, and the roster at night, out in the open.
			server.runCommand("gamemode spectator @p");
			BlockPos lean = pieces.getFirst().leanToSpot();
			Vec3 from = Vec3.atCenterOf(lean).add(Vec3.atLowerCornerOf(c.subtract(lean)).normalize().scale(5.0)).add(0, 1.5, 0);
			float yaw = (float) (Math.toDegrees(Math.atan2(lean.getZ() - from.z, lean.getX() - from.x)) - 90.0);
			tp(context, server, from.x, from.y, from.z, yaw, 20);
			context.takeScreenshot("camp-leanto");
			server.runCommand("time set midnight");
			tp(context, server, c.getX() + 12.5, c.getY() + 5.0, c.getZ() - 12.5, 45, 15);
			context.waitTicks(100);
			context.takeScreenshot("camp-roster-night");
		}
	}

	/** Only the frequency survey (part {@code campsurvey}, never in the full suite: {@code camps} prints it too). */
	private static void surveyOnly(ClientGameTestContext context) {
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> {
					s.setWorldType(new WorldCreationUiState.WorldTypeEntry(
							s.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
					s.setSeed("20261116");
					s.setGenerateStructures(true);
					s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
				})
				.create()) {
			TestServerContext server = sp.getServer();
			setup(server);
			server.computeOnServer(s -> survey(s.overworld()));
		}
	}

	/** Stables with their mounts, the tower and skull chests, and the room stairs seen from below. */
	private static void extras(ClientGameTestContext context) {
		try (TestSingleplayerContext sp = context.worldBuilder().adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			setup(server);
			BlockPos base = context.computeOnClient(mc -> mc.player.blockPosition());
			// stables: a large camp of three kinds, the roster cleared away so only the mounts stand
			CampKind[] stableKinds = { CampKind.ZOMBIE, CampKind.HUSK, CampKind.SKELETON };
			for (int i = 0; i < stableKinds.length; i++) {
				CampKind kind = stableKinds[i];
				BlockPos at = base.offset(i * 50, 0, 0);
				tp(context, server, at.getX() + 0.5, at.getY() + 30, at.getZ() - 20.5, 0, 50);
				sp.getConnection().waitForChunksRender();
				CampPiece piece = server.computeOnServer(sv -> CampCommand.build(sv.overworld(), at, kind, CampTuning.Size.LARGE, 40L + kind.ordinal()));
				context.waitTicks(40); // light settled under the roof
				server.runOnServer(sv -> {
					for (var site : io.github.bubba8587.bubbas_reworked_mobs.camp.CampSites.all(sv.overworld())) {
						if (site.centre().equals(piece.centre())) {
							io.github.bubba8587.bubbas_reworked_mobs.camp.CampRoster.spawn(sv.overworld(), site).forEach(net.minecraft.world.entity.Entity::discard);
						}
					}
				});
				BlockPos in = piece.stableInside();
				net.minecraft.core.Direction out = piece.stableFacing();
				String mounts = server.computeOnServer(sv -> sv.overworld().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
						new net.minecraft.world.phys.AABB(in).inflate(5.0), m -> m.entityTags().contains(io.github.bubba8587.bubbas_reworked_mobs.camp.CampRoster.LIVESTOCK_TAG))
						.stream().map(m -> m.getType().getDescriptionId().replaceFirst("^.*[.]", "") + (m.isLeashed() ? " leashed" : "")).toList().toString());
				System.out.println("[camps] " + kind.id() + " stable mounts: " + mounts);
				Vec3 close = Vec3.atBottomCenterOf(in).add(out.getStepX() * 5.5, 1.6, out.getStepZ() * 5.5);
				tp(context, server, close.x, close.y, close.z, out.getOpposite().toYRot(), 10);
				context.waitTicks(10);
				context.takeScreenshot("camp-stable-" + kind.id() + "-close");
				Vec3 clearing = Vec3.atBottomCenterOf(piece.centre()).add(0.0, 3.5, 0.0).add(Vec3.atLowerCornerOf(piece.centre().subtract(in)).normalize().scale(3.0));
				float yaw = (float) (Math.toDegrees(Math.atan2(in.getZ() + 0.5 - clearing.z, in.getX() + 0.5 - clearing.x)) - 90.0);
				tp(context, server, clearing.x, clearing.y, clearing.z, yaw, 12);
				context.takeScreenshot("camp-stable-" + kind.id() + "-clearing");
			}
			server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
			// the skull chests (full and half-buried) and the winding tower's, seen from inside
			String[] chests = { "skull", "buried", "tower" };
			for (int i = 0; i < chests.length; i++) {
				int f = i;
				long seed = 0;
				while (!chestPiece(new CampPiece(seed, base.offset(i * 50, 0, 60), CampKind.SKELETON, CampTuning.Size.LARGE), f)) {
					seed++;
				}
				BlockPos at = base.offset(i * 50, 0, 60);
				long sd = seed;
				tp(context, server, at.getX() + 0.5, at.getY() + 30, at.getZ() - 20.5, 0, 50);
				sp.getConnection().waitForChunksRender();
				CampPiece piece = server.computeOnServer(sv -> CampCommand.build(sv.overworld(), at, CampKind.SKELETON, CampTuning.Size.LARGE, sd));
				BlockPos chest = f == 2 ? piece.windingChest() : piece.skullChest();
				// in the skull, from its middle; in the tower's core, from the opposite corner
				BlockPos eye = f == 2 ? new BlockPos(2 * piece.windingTop().getX() - chest.getX(), chest.getY(), 2 * piece.windingTop().getZ() - chest.getZ())
						: piece.skullInside().above();
				double ex = eye.getX() + 0.5;
				double ey = f == 2 ? chest.getY() + 0.3 : eye.getY() - 0.2;
				double ez = eye.getZ() + 0.5;
				float yaw = (float) (Math.toDegrees(Math.atan2(chest.getZ() + 0.5 - ez, chest.getX() + 0.5 - ex)) - 90.0);
				tp(context, server, ex, ey, ez, yaw, 30);
				context.takeScreenshot("camp-" + chests[i] + "-chest");
			}
			// the room stairs from the room's doorway, looking up and out
			CampKind[] stairKinds = { CampKind.SKELETON, CampKind.HUSK };
			for (int i = 0; i < stairKinds.length; i++) {
				CampKind kind = stairKinds[i];
				BlockPos at = base.offset(i * 50, 12, 120); // on a stone block (room depth)
				tp(context, server, at.getX() + 0.5, at.getY() + 30, at.getZ() - 20.5, 0, 50);
				sp.getConnection().waitForChunksRender();
				server.runCommand("fill %d %d %d %d %d %d stone".formatted(at.getX() - 16, at.getY() - 13, at.getZ() - 16, at.getX() + 16, at.getY() - 2, at.getZ() + 16));
				server.runCommand("fill %d %d %d %d %d %d grass_block".formatted(at.getX() - 16, at.getY() - 1, at.getZ() - 16, at.getX() + 16, at.getY() - 1, at.getZ() + 16));
				CampPiece piece = server.computeOnServer(sv -> CampCommand.build(sv.overworld(), at, kind, CampTuning.Size.MEDIUM, 9L + kind.ordinal()));
				BlockPos door = piece.tunnelStep(0).below();
				net.minecraft.core.Direction out = piece.stairsOut();
				tp(context, server, door.getX() + 0.5 - out.getStepX() * 1.5, door.getY() + 0.2, door.getZ() + 0.5 - out.getStepZ() * 1.5, out.toYRot(), -32);
				context.takeScreenshot("camp-stairs-up-" + kind.id());
			}
			server.runCommand("effect clear @p");
		}
	}

	/** Whether a camp plan has the chest piece: 0 a full skull rock, 1 a half-buried one, 2 a winding tower. */
	private static boolean chestPiece(CampPiece piece, int f) {
		return switch (f) {
			case 0 -> piece.skullChest() != null && !piece.skullBuried();
			case 1 -> piece.skullChest() != null && piece.skullBuried();
			default -> piece.windingChest() != null;
		};
	}

	private static void natural(ClientGameTestContext context) {
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
			setup(server);
			List<CampPiece> found = server.computeOnServer(s -> survey(s.overworld()));
			for (int i = 0; i < Math.min(1, found.size()); i++) {
				CampPiece piece = found.get(i);
				BlockPos c = piece.centre();
				tp(context, server, c.getX() + 0.5, c.getY() + 40, c.getZ() - 20.5, 0, 50);
				sp.getConnection().waitForChunksRender();
				tp(context, server, c.getX() + 0.5, c.getY() + 22, c.getZ() - 20.5, 0, 45);
				context.takeScreenshot("camp-natural-" + i + "-above");
				tp(context, server, c.getX() + 9.5, c.getY() + 9.0, c.getZ() - 9.5, 45, 32);
				context.takeScreenshot("camp-natural-" + i + "-close");
				if (i == 0) {
					// A zombie on the camp's surface can walk down into its room.
					String reach = server.computeOnServer(srv -> {
						var level = srv.overworld();
						var zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
						int sx = c.getX() + 4;
						int sz = c.getZ();
						zombie.snapTo(sx + 0.5, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz), sz + 0.5, 0, 0);
						zombie.setOnGround(true);
						level.addFreshEntity(zombie);
						var path = zombie.getNavigation().createPath(piece.roomSpot(), 2); // a bone block may sit on the exact cell
						zombie.discard();
						return path == null ? "no path" : path.canReach() ? "reachable" : "ends at " + path.getEndNode();
					});
					System.out.println("[camps] natural camp room from the surface: " + reach);
					if (!"reachable".equals(reach)) {
						throw new AssertionError("a camp's room should be reachable from the surface: " + reach);
					}
					// Inside its underground room (night vision: it's dark).
					server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
					BlockPos room = piece.roomSpot();
					tp(context, server, room.getX() + 0.5, room.getY() + 0.6, room.getZ() + 0.5, 0, 15);
					context.takeScreenshot("camp-room");
					tp(context, server, room.getX() + 0.5, room.getY() + 0.6, room.getZ() + 0.5, 180, 15);
					context.takeScreenshot("camp-room-back");
					server.runCommand("effect clear @p");
				}
			}
		}
	}

	/**
	 * Counts the camps whose start chunk is within 2048 blocks of spawn (structure starts only: cheap), prints the
	 * density and nearest distances, and returns the nearest few camps of different kinds (centres).
	 */
	private static List<CampPiece> survey(ServerLevel level) {
		var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(Camps.CAMP_KEY);
		var set = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getValue(
				net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE_SET, io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs.id("monster_camps")));
		RandomSpreadStructurePlacement placement = (RandomSpreadStructurePlacement) set.placement();
		long seed = level.getSeed();
		BlockPos spawn = level.getRespawnData().pos();
		int half = 2048;
		int cell = placement.spacing() * 16;
		Map<CampKind, List<CampPiece>> byKind = new EnumMap<>(CampKind.class);
		int cells = 0;
		for (int gx = Math.floorDiv(spawn.getX() - half, cell); gx <= Math.floorDiv(spawn.getX() + half, cell); gx++) {
			for (int gz = Math.floorDiv(spawn.getZ() - half, cell); gz <= Math.floorDiv(spawn.getZ() + half, cell); gz++) {
				var chunk = placement.getPotentialStructureChunk(seed, gx * placement.spacing(), gz * placement.spacing());
				cells++;
				var access = level.getChunk(chunk.x(), chunk.z(), ChunkStatus.STRUCTURE_STARTS, true);
				StructureStart start = access == null ? null : access.getStartForStructure(structure);
				if (start != null && start.isValid() && start.getPieces().getFirst() instanceof CampPiece piece) {
					byKind.computeIfAbsent(piece.kind(), k -> new ArrayList<>()).add(piece);
				}
			}
		}
		double area = (2.0 * half / 1000.0) * (2.0 * half / 1000.0);
		int total = byKind.values().stream().mapToInt(List::size).sum();
		StringBuilder report = new StringBuilder("[camps] survey of %d cells over %.1f km2 round spawn: %d camps (%.1f per 1000x1000)"
				.formatted(cells, area, total, total / area));
		// Typical distance to the nearest of each kind, from a grid of points.
		for (CampKind kind : CampKind.values()) {
			List<CampPiece> camps = byKind.getOrDefault(kind, List.of());
			List<Double> nearest = new ArrayList<>();
			for (int px = -1536; px <= 1536; px += 256) {
				for (int pz = -1536; pz <= 1536; pz += 256) {
					double best = Double.MAX_VALUE;
					for (CampPiece p : camps) {
						BlockPos c = p.centre();
						best = Math.min(best, Math.hypot(c.getX() - (spawn.getX() + px), c.getZ() - (spawn.getZ() + pz)));
					}
					nearest.add(best);
				}
			}
			nearest.sort(Double::compare);
			double median = nearest.get(nearest.size() / 2);
			report.append("; %s %d (%.1f/km2, median nearest %s)".formatted(kind.id(), camps.size(), camps.size() / area,
					median == Double.MAX_VALUE ? "none" : Math.round(median) + " blocks"));
		}
		System.out.println(report);
		for (int radius : new int[] { CampTuning.PATROL_TERRITORY, 256, 384, 448 }) {
			territoryCoverage(level, spawn, byKind.values().stream().flatMap(List::stream).toList(), radius);
		}
		// The nearest camp of each kind, nearest first.
		List<CampPiece> picks = new ArrayList<>();
		for (CampKind kind : CampKind.values()) {
			byKind.getOrDefault(kind, List.of()).stream().min((a, b) -> Double.compare(a.centre().distSqr(spawn), b.centre().distSqr(spawn))).ifPresent(picks::add);
		}
		picks.sort((a, b) -> Double.compare(a.centre().distSqr(spawn), b.centre().distSqr(spawn)));
		System.out.println("[camps] nearest of each kind: " + picks.stream().map(CampPiece::centre).toList());
		return picks;
	}

	/**
	 * Patrol territory coverage (specs/monster-camps.md "Camp patrols"): the share of land points (every 64 blocks within
	 * 1536 of spawn, so camps lie round every point) within {@code radius} of a camp, for camp-biome land and
	 * all dry land (not ocean, river or beach). Every camp counts as armed, as generated.
	 */
	private static void territoryCoverage(ServerLevel level, BlockPos spawn, List<CampPiece> camps, int radius) {
		var generator = level.getChunkSource().getGenerator();
		var random = level.getChunkSource().randomState();
		var campBiomes = net.minecraft.tags.TagKey.create(Registries.BIOME, io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs.id("has_structure/monster_camp"));
		double r2 = (double) radius * radius;
		int campLand = 0;
		int campLandIn = 0;
		int land = 0;
		int landIn = 0;
		for (int px = -1536; px <= 1536; px += 64) {
			for (int pz = -1536; pz <= 1536; pz += 64) {
				int x = spawn.getX() + px;
				int z = spawn.getZ() + pz;
				int y = generator.getFirstOccupiedHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, level, random);
				var biome = level.getUncachedNoiseBiome(x >> 2, y >> 2, z >> 2);
				boolean in = false;
				for (CampPiece camp : camps) {
					double dx = camp.centre().getX() - x;
					double dz = camp.centre().getZ() - z;
					if (dx * dx + dz * dz <= r2) {
						in = true;
						break;
					}
				}
				if (biome.is(campBiomes)) {
					campLand++;
					campLandIn += in ? 1 : 0;
				}
				if (!biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN) && !biome.is(net.minecraft.tags.BiomeTags.IS_RIVER) && !biome.is(net.minecraft.tags.BiomeTags.IS_BEACH)) {
					land++;
					landIn += in ? 1 : 0;
				}
			}
		}
		System.out.println("[camps] patrol territory %d%s: covers %.1f%% of camp-biome land (%d points), %.1f%% of all dry land (%d points)".formatted(
				radius, radius == CampTuning.PATROL_TERRITORY ? " (current)" : "", 100.0 * campLandIn / Math.max(1, campLand), campLand, 100.0 * landIn / Math.max(1, land), land));
	}

	/** Where a camp's newer piece stands (0 skull rock, 1 winding tower, 2 ramp, 3 bridge), or null without one. */
	private static BlockPos feature(CampPiece piece, int f) {
		return switch (f) {
			case 0 -> piece.skullBuried() ? null : piece.skullInside();
			case 4 -> piece.skullBuried() ? piece.skullInside() : null;
			case 5 -> piece.ribcageEnds() == null ? null : piece.ribcageEnds()[0].relative(net.minecraft.core.Direction.getApproximateNearest(
					piece.ribcageEnds()[1].getX() - piece.ribcageEnds()[0].getX(), 0, piece.ribcageEnds()[1].getZ() - piece.ribcageEnds()[0].getZ()), 5);
			case 1 -> piece.windingTop() == null ? null : piece.windingTop().below(6);
			case 2 -> piece.rampEnds() == null ? null : piece.rampEnds()[0];
			default -> {
				BlockPos[] e = piece.bridgeEnds();
				yield e == null ? null : new BlockPos((e[0].getX() + e[1].getX()) / 2, e[0].getY() - 3, (e[0].getZ() + e[1].getZ()) / 2);
			}
		};
	}

	/** Feature names to shoot (system property camps.features, comma-separated), or null for all. */
	private static final java.util.Set<String> CLIENT_FEATURES = System.getProperty("camps.features") == null ? null
			: java.util.Set.of(System.getProperty("camps.features").split(","));

	private static void setup(TestServerContext server) {
		server.runCommand("gamerule send_command_feedback false");
		server.runCommand("gamerule spawn_mobs false");
		server.runCommand("gamerule advance_time false");
		server.runCommand("time set noon");
		server.runCommand("weather clear");
		server.runCommand("gamemode spectator @p");
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.waitTicks(15);
	}
}
