// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.camp.CampCommand;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampKind;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampPiece;
import io.github.bubba8587.bubbas_reworked_mobs.camp.CampRoster;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Nether outposts by eye and in real terrain (specs/monster-camps.md "Nether outposts"; client part {@code outposts}).
 * In a real world (seed 20261116), the natural outposts nearest a few points of the Nether: each is checked for its
 * biome and for lava under its clearing, a piglin can path from its fire to a tower platform, and each is shot from
 * above, close, the gate and inside the portal room (outpost-&lt;i&gt;-*); then one's roster and penned hoglins
 * (outpost-roster).
 */
@SuppressWarnings("UnstableApiUsage")
public class OutpostsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("outposts")) {
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
			server.runCommand("gamemode spectator @p");
			List<CampPiece> found = server.computeOnServer(s -> survey(s.getLevel(Level.NETHER)));
			if (found.isEmpty()) {
				throw new AssertionError("no natural Nether outpost found");
			}
			List<String> failures = new ArrayList<>();
			server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
			for (int i = 0; i < Math.min(3, found.size()); i++) {
				CampPiece piece = found.get(i);
				BlockPos c = piece.centre();
				tp(context, server, c.getX() + 0.5, c.getY() + 3, c.getZ() + 0.5, 0, 90);
				sp.getConnection().waitForChunksRender();
				String checked = server.computeOnServer(s -> check(s.getLevel(Level.NETHER), piece));
				System.out.println("[outposts] outpost " + i + " at " + c.toShortString() + " " + piece.size().id() + ", portal " + piece.portal() + ": " + checked);
				if (!checked.startsWith("ok")) {
					failures.add("outpost at " + c.toShortString() + ": " + checked);
				}
				int r = piece.size().radius;
				tp(context, server, c.getX() + 0.5, c.getY() + 11, c.getZ() - r + 1.5, 0, 50);
				context.takeScreenshot("outpost-" + i + "-above");
				tp(context, server, c.getX() + r - 1.5, c.getY() + 4.0, c.getZ() - r + 1.5, 45, 20);
				context.takeScreenshot("outpost-" + i + "-close");
				tp(context, server, c.getX() - r + 1.5, c.getY() + 5.0, c.getZ() + r - 1.5, 225, 25);
				context.takeScreenshot("outpost-" + i + "-side");
				BlockPos mouth = piece.tunnelStep(6);
				BlockPos eye = mouth.relative(piece.portalStairs(), 4);
				float back = (float) (Math.toDegrees(Math.atan2(-piece.portalStairs().getStepZ(), -piece.portalStairs().getStepX())) - 90.0);
				tp(context, server, eye.getX() + 0.5, eye.getY() + 3.0, eye.getZ() + 0.5, back, 35);
				context.takeScreenshot("outpost-" + i + "-tunnel");
				BlockPos[] gate = piece.gateEnds();
				if (gate != null) {
					double gx = gate[0].getX() + 0.5 + (gate[0].getX() - gate[1].getX()) * 1.2;
					double gz = gate[0].getZ() + 0.5 + (gate[0].getZ() - gate[1].getZ()) * 1.2;
					float yaw = (float) (Math.toDegrees(Math.atan2(gate[1].getZ() - gate[0].getZ(), gate[1].getX() - gate[0].getX())) - 90.0);
					tp(context, server, gx, gate[0].getY() + 2.0, gz, yaw, 5);
					context.takeScreenshot("outpost-" + i + "-gate");
				}
				BlockPos portal = piece.portalBase().relative(piece.portalAlong(), 1).relative(piece.portalStairs(), 4);
				float yaw = (float) (Math.toDegrees(Math.atan2(-piece.portalStairs().getStepZ(), -piece.portalStairs().getStepX())) - 90.0);
				tp(context, server, portal.getX() + 0.5 + piece.portalAlong().getStepX() * 0.5, portal.getY() + 0.6,
						portal.getZ() + 0.5 + piece.portalAlong().getStepZ() * 0.5, yaw, 10);
				context.takeScreenshot("outpost-" + i + "-portal");
			}
			// The first outpost's roster and hoglins, with a survival player near (in gold, so the piglins stay calm).
			CampPiece piece = found.getFirst();
			BlockPos c = piece.centre();
			server.runCommand("gamemode survival @p");
			server.runCommand("effect give @p minecraft:resistance infinite 4 true");
			server.runCommand("item replace entity @p armor.head with minecraft:golden_helmet");
			tp(context, server, c.getX() + 9.5, c.getY() + 4.0, c.getZ() - 9.5, 45, 15);
			context.waitTicks(60);
			context.takeScreenshot("outpost-roster");
			String livestock = server.computeOnServer(s -> {
				ServerLevel nether = s.getLevel(Level.NETHER);
				StringBuilder out = new StringBuilder();
				for (BlockPos post : piece.tetherPosts()) {
					var pen = new net.minecraft.world.phys.AABB(post).inflate(1.9, 2.0, 1.9);
					var hoglins = nether.getEntitiesOfClass(net.minecraft.world.entity.monster.hoglin.Hoglin.class, pen.inflate(8.0),
							h -> h.entityTags().contains(CampRoster.LIVESTOCK_TAG));
					long penned = hoglins.stream().filter(h -> pen.contains(h.position())).count();
					long leashed = hoglins.stream().filter(h -> h.isLeashed()).count();
					out.append("pen ").append(post.toShortString()).append(": ").append(hoglins.size()).append(" hoglins, ").append(penned)
							.append(" penned, ").append(leashed).append(" leashed; ");
					if (hoglins.isEmpty() || penned != hoglins.size() || leashed < 1) {
						out.append("FAIL ");
					}
				}
				return out.toString();
			});
			System.out.println("[outposts] livestock: " + livestock);
			if (livestock.contains("FAIL")) {
				failures.add("outpost hoglins should be penned, one leashed: " + livestock);
			}
			BlockPos post = piece.tetherPosts().isEmpty() ? c : piece.tetherPosts().getFirst();
			server.runCommand("gamemode spectator @p");
			tp(context, server, post.getX() + 5.5, post.getY() + 4.0, post.getZ() + 5.5, 135, 30);
			context.takeScreenshot("outpost-pen");
			server.runCommand("effect clear @p");
			if (!failures.isEmpty()) {
				throw new AssertionError(String.join("; ", failures));
			}
		}
	}

	/** The nearest natural outposts round a few points of the Nether (structure starts only), nearest first. */
	private static List<CampPiece> survey(ServerLevel nether) {
		List<CampPiece> found = new ArrayList<>();
		for (int[] o : new int[][] { { 0, 0 }, { 1500, 0 }, { 0, 1500 }, { -1500, -1500 }, { -1500, 1500 } }) {
			CampPiece piece = CampCommand.nearest(nether, new BlockPos(o[0], 64, o[1]), CampKind.PIGLIN, 2000);
			if (piece != null && found.stream().noneMatch(p -> p.centre().equals(piece.centre()))) {
				found.add(piece);
			}
		}
		System.out.println("[outposts] natural outposts: " + found.stream().map(p -> p.centre().toShortString() + " " + p.size().id()).toList());
		return found;
	}

	/** Biome, lava and walkability of a generated outpost: "ok ..." or what is wrong. */
	private static String check(ServerLevel nether, CampPiece piece) {
		BlockPos c = piece.centre();
		var biome = nether.getBiome(c.above());
		if (!biome.is(Biomes.CRIMSON_FOREST) && !biome.is(Biomes.NETHER_WASTES)) {
			return "in " + biome.getRegisteredName();
		}
		int r = piece.size().radius;
		int lava = 0;
		for (int dx = -r - 1; dx <= r + 1; dx++) {
			for (int dz = -r - 1; dz <= r + 1; dz++) {
				if (dx * dx + dz * dz > (r + 1) * (r + 1)) {
					continue;
				}
				for (int dy = -8; dy <= 3; dy++) {
					if (nether.getFluidState(c.offset(dx, dy, dz)).is(net.minecraft.tags.FluidTags.LAVA)) {
						lava++;
					}
				}
			}
		}
		if (lava > 0) {
			return lava + " lava blocks in the clearing";
		}
		var piglin = net.minecraft.world.entity.EntityTypes.PIGLIN.create(nether, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		BlockPos start = null;
		for (BlockPos column : piece.groundSpots()) {
			for (int dy = 3; dy >= -3 && start == null; dy--) {
				BlockPos at = column.atY(c.getY() + dy);
				if (nether.getBlockState(at.below()).isFaceSturdy(nether, at.below(), net.minecraft.core.Direction.UP)
						&& nether.getBlockState(at).getCollisionShape(nether, at).isEmpty() && nether.getBlockState(at.above()).getCollisionShape(nether, at.above()).isEmpty()) {
					start = at;
				}
			}
			if (start != null) {
				break;
			}
		}
		if (start == null) {
			return "nowhere to stand by the fire";
		}
		piglin.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(48.0);
		piglin.snapTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 0, 0);
		piglin.setOnGround(true);
		nether.addFreshEntity(piglin);
		StringBuilder out = new StringBuilder("ok, " + biome.getRegisteredName());
		List<BlockPos> targets = new ArrayList<>();
		if (piece.rampEnds() != null) {
			targets.add(piece.rampEnds()[1]);
		}
		if (piece.gateEnds() != null) {
			targets.add(piece.gateEnds()[0]);
		}
		targets.add(piece.tunnelStep(4));
		for (BlockPos target : targets) {
			var path = piglin.getNavigation().createPath(target, 1);
			boolean reach = path != null && path.canReach();
			out.append(", ").append(target.toShortString()).append(reach ? " reached" : " NOT reached" + (path == null ? "" : " (ends " + path.getEndNode().asBlockPos().toShortString() + ")"));
		}
		if (out.indexOf("NOT") >= 0) {
			piglin.discard();
			return "from the fire " + start.toShortString() + ": " + out;
		}
		piglin.discard();
		return out.toString();
	}

	private static void tp(ClientGameTestContext context, TestServerContext server, double x, double y, double z, float yaw, float pitch) {
		server.runCommand(String.format(java.util.Locale.ROOT, "execute in minecraft:the_nether run tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.waitTicks(15);
	}
}
