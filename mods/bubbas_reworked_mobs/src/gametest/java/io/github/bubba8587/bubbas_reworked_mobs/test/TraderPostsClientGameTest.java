// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.test;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.traderpost.TraderPostPiece;
import io.github.bubba8587.bubbas_reworked_mobs.traderpost.TraderPosts;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Trader posts by eye and in real terrain (specs/monster-camps.md "Trader posts"; client part {@code traderposts}).
 * In a real world (seed 20261116), the natural posts nearest a few points: each is checked for its biome, a flat dry
 * floor, sound blocks and clean fences, no monster spawns, and a trader's walk from the entrance to the counter and
 * the pen gates; each is shot from above, the front, both sides and the pens (traderpost-&lt;i&gt;-*). Then a wandering
 * trader's daily spawn near a player is sent to the first post: it and its llamas are shot there (traderpost-arrival*),
 * the llamas checked penned and tethered.
 */
@SuppressWarnings("UnstableApiUsage")
public class TraderPostsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("traderposts")) {
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
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamemode spectator @p");
			List<TraderPostPiece> found = server.computeOnServer(s -> survey(s.overworld()));
			if (found.isEmpty()) {
				throw new AssertionError("no natural trader post found");
			}
			List<String> failures = new ArrayList<>();
			for (int i = 0; i < Math.min(3, found.size()); i++) {
				TraderPostPiece p = found.get(i);
				look(context, server, p.at(0, 30, 20), p.at(0, 0, 0));
				sp.getConnection().waitForChunksRender();
				context.waitTicks(20);
				String checked = server.computeOnServer(s -> check(s.overworld(), p));
				System.out.println("[traderposts] post " + i + " at " + p.centre().toShortString() + " seed " + p.seed() + " pens " + p.penCount()
						+ " wagon " + p.hasWagon() + ": " + checked);
				if (!checked.startsWith("ok")) {
					failures.add("post at " + p.centre().toShortString() + ": " + checked);
				}
				look(context, server, p.at(0, 16, 18), p.at(0, 0, -1));
				context.takeScreenshot("traderpost-" + i + "-above");
				look(context, server, p.at(0, 3, 15), p.at(0, 2, -5));
				context.takeScreenshot("traderpost-" + i + "-front");
				look(context, server, p.at(-15, 6, 11), p.at(0, 1, -2));
				context.takeScreenshot("traderpost-" + i + "-left");
				look(context, server, p.at(15, 6, -13), p.at(0, 1, 0));
				context.takeScreenshot("traderpost-" + i + "-backright");
				if (i == 0) {
					look(context, server, p.at(1, 3, 1), p.at(0, 2, -5));
					context.takeScreenshot("traderpost-" + i + "-counter");
					look(context, server, p.at(-3, 4, 5), p.at(-7, 1, 0));
					context.takeScreenshot("traderpost-" + i + "-pen");
				}
			}
			// The daily spawn near a player, sent to the post: a survival player 30 blocks off.
			TraderPostPiece p = found.getFirst();
			look(context, server, p.at(0, 16, 18), p.at(0, 0, -1));
			sp.getConnection().waitForChunksRender();
			context.waitTicks(20);
			BlockPos player = p.at(0, 1, 30);
			String arrival = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				BlockPos ground = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, player);
				WanderingTrader trader = TraderPosts.redirect(level, ground);
				if (trader == null) {
					return "FAIL no trader arrived";
				}
				StringBuilder out = new StringBuilder("trader at " + trader.blockPosition().toShortString());
				List<TraderLlama> llamas = level.getEntitiesOfClass(TraderLlama.class, new AABB(p.centre()).inflate(16.0));
				out.append(", ").append(llamas.size()).append(" llamas");
				for (TraderLlama l : llamas) {
					boolean penned = p.penInsides().stream().anyMatch(b -> b.contains(l.position()));
					out.append(penned ? " penned" : " NOT penned").append(TraderPosts.tetheredAtPost(l) ? " tethered" : " NOT tethered");
				}
				if (llamas.size() != 2 || out.indexOf("NOT") >= 0) {
					out.insert(0, "FAIL ");
				}
				return out.toString();
			});
			System.out.println("[traderposts] arrival: " + arrival);
			if (arrival.startsWith("FAIL")) {
				failures.add("arrival: " + arrival);
			}
			context.waitTicks(40);
			look(context, server, p.at(0, 3, 9), p.at(0, 1, -4));
			context.takeScreenshot("traderpost-arrival-front");
			look(context, server, p.at(-2, 5, 6), p.at(-7, 1, 0));
			context.takeScreenshot("traderpost-arrival-pen");
			look(context, server, p.at(0, 14, 14), p.at(0, 0, -1));
			context.takeScreenshot("traderpost-arrival-above");
			server.runCommand("time set midnight");
			look(context, server, p.at(-10, 5, 13), p.at(0, 1, -2));
			context.takeScreenshot("traderpost-night");
			server.runCommand("time set noon");
			if (!failures.isEmpty()) {
				throw new AssertionError(String.join("; ", failures));
			}
		}
	}

	/** The nearest natural posts round a few points (structure starts only), nearest first. */
	private static List<TraderPostPiece> survey(ServerLevel level) {
		List<TraderPostPiece> found = new ArrayList<>();
		for (int[] o : new int[][] { { 0, 0 }, { 1500, 0 }, { 0, 1500 }, { -1500, -1500 }, { 1500, -1500 } }) {
			TraderPostPiece piece = TraderPosts.nearest(level, new BlockPos(o[0], 64, o[1]), 2000);
			if (piece != null && found.stream().noneMatch(p -> p.centre().equals(piece.centre()))) {
				found.add(piece);
			}
		}
		System.out.println("[traderposts] natural posts: " + found.stream().map(p -> p.centre().toShortString() + " seed " + p.seed()).toList());
		// density: every post start within 1500 of the origin
		var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(TraderPosts.KEY);
		var set = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getValue(TraderPosts.SET);
		if (set != null && set.placement() instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement placement) {
			int n = 0;
			int inBiome = 0;
			TagKey<Biome> tag = TagKey.create(Registries.BIOME, BubbasReworkedMobs.id("has_structure/trader_post"));
			int cells = 1500 / (placement.spacing() * 16) + 1;
			for (int gx = -cells; gx < cells; gx++) {
				for (int gz = -cells; gz < cells; gz++) {
					var chunk = placement.getPotentialStructureChunk(level.getSeed(), gx * placement.spacing(), gz * placement.spacing());
					var start = level.getChunk(chunk.x(), chunk.z(), net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS, true)
							.getStartForStructure(structure);
					if (start != null && start.isValid()) {
						n++;
					}
					if (level.getUncachedNoiseBiome(chunk.getMiddleBlockX() >> 2, 72 >> 2, chunk.getMiddleBlockZ() >> 2).is(tag)) {
						inBiome++;
					}
				}
			}
			double side = cells * 2.0 * placement.spacing() * 16 / 1000.0;
			System.out.printf(java.util.Locale.ROOT, "[traderposts] density: %d posts (%d candidates in post biomes) in %.1f x %.1f km (%.2f per km2)%n", n, inBiome, side, side, n / (side * side));
		}
		return found;
	}

	/** Biome, floor, blocks, fences, spawns and walkability of a generated post: "ok ..." or what is wrong. */
	private static String check(ServerLevel level, TraderPostPiece p) {
		BlockPos c = p.centre();
		TagKey<Biome> tag = TagKey.create(Registries.BIOME, BubbasReworkedMobs.id("has_structure/trader_post"));
		var biome = level.getBiome(c.above());
		if (!biome.is(tag)) {
			return "in " + biome.getRegisteredName();
		}
		var plaza = p.plaza();
		int uneven = 0;
		int wet = 0;
		for (int x = plaza.minX(); x <= plaza.maxX(); x++) {
			for (int z = plaza.minZ(); z <= plaza.maxZ(); z++) {
				BlockPos floor = new BlockPos(x, c.getY(), z);
				if (!level.getBlockState(floor).isSolid()) {
					uneven++;
				}
				if (!level.getFluidState(floor.above()).isEmpty() && !level.getBlockState(floor.above()).is(net.minecraft.world.level.block.Blocks.WATER_CAULDRON)) {
					wet++;
				}
			}
		}
		List<String> loose = CampsGameTest.unsupported(level, p.getBoundingBox());
		List<String> fences = CampsGameTest.fenceFaults(level, p.getBoundingBox());
		var monsters = level.getChunkSource().getGenerator().getMobsAt(level, level.structureManager(), MobCategory.MONSTER, c.above());
		StringBuilder out = new StringBuilder("ok, " + biome.getRegisteredName() + ", front " + p.front().getName());
		if (uneven > 0 || wet > 0) {
			out.insert(0, "FLOOR " + uneven + " holes, " + wet + " wet; ");
		}
		if (!loose.isEmpty()) {
			out.insert(0, "LOOSE " + loose.subList(0, Math.min(6, loose.size())) + "; ");
		}
		if (!fences.isEmpty()) {
			out.insert(0, "FENCES " + fences.subList(0, Math.min(6, fences.size())) + "; ");
		}
		if (!monsters.isEmpty()) {
			out.insert(0, "MONSTERS can spawn in the post; ");
		}
		WanderingTrader walker = EntityTypes.WANDERING_TRADER.create(level, EntitySpawnReason.COMMAND);
		walker.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(48.0);
		BlockPos start = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.entrance());
		walker.snapTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 0, 0);
		walker.setOnGround(true);
		level.addFreshEntity(walker);
		List<BlockPos> targets = new ArrayList<>(p.gateFronts());
		targets.add(p.traderSpot());
		for (BlockPos target : targets) {
			var path = walker.getNavigation().createPath(target, 0);
			if (path == null || !path.canReach()) {
				out.insert(0, "UNREACHED " + target.toShortString() + " from " + start.toShortString() + "; ");
			}
		}
		walker.discard();
		return out.toString();
	}

	/** Spectator camera at {@code eye}, looking at {@code target}. */
	private static void look(ClientGameTestContext context, TestServerContext server, BlockPos eye, BlockPos target) {
		int ground = server.computeOnServer(s -> s.overworld().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, eye.getX(), eye.getZ()));
		Vec3 e = Vec3.atCenterOf(eye.getY() < ground + 2 ? eye.atY(ground + 2) : eye);
		Vec3 d = Vec3.atCenterOf(target).subtract(e);
		float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", e.x, e.y - 1.62, e.z, yaw, pitch));
		context.waitTicks(15);
	}
}
