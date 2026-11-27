// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.test;

import java.util.List;

import com.mojang.datafixers.util.Pair;
import io.github.bubba8587.bubbas_world_tweaks.fortress.ConduitShrine;
import io.github.bubba8587.bubbas_world_tweaks.fortress.DeepseaFortresses;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** A naturally generated deep-sea fortress in a seeded normal world, by eye: outside, pillars, a corridor, the lifts, the shrine with a conduit. */
@SuppressWarnings("UnstableApiUsage")
public class DeepseaFortressClientGameTest implements FabricClientGameTest {
	private static final String SEED = "20261122";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("deepsea")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.setUseConsistentSettings(false)
				.adjustSettings(s -> {
					s.setSeed(SEED);
					s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
				})
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set 6000");
			server.runCommand("gamemode spectator @p");
			server.runCommand("effect give @p night_vision infinite 0 true");
			BlockPos found = server.computeOnServer(s -> nearest(s.overworld()));
			BlockPos centre = found.offset(11, 0, 11);
			server.runCommand("tp @p %d 70 %d".formatted(centre.getX(), centre.getZ()));
			context.waitTicks(20);
			sp.getConnection().waitForChunksRender();
			StructureStart start = server.computeOnServer(s -> {
				Structure fortress = s.overworld().registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(DeepseaFortresses.STRUCTURE);
				for (int y = 62; y > -60; y--) {
					StructureStart at = s.overworld().structureManager().getStructureAt(centre.atY(y), fortress);
					if (at != null) {
						return at;
					}
				}
				return null;
			});
			if (start == null) {
				throw new AssertionError("no deep-sea fortress at " + centre);
			}
			List<StructurePiece> pieces = start.getPieces();
			BoundingBox fb = pieces.getFirst().getBoundingBox();
			System.out.println("DEEPSEA client: fortress " + start.getBoundingBox() + ", crossing " + fb);

			shot(context, sp, open(server, new Vec3(fb.minX() - 12.5, fb.minY() + 7, fb.getCenter().getZ() + 0.5)), -90.0F, 12.0F, "deepsea-outside");
			shot(context, sp, open(server, new Vec3(fb.minX() + 3.5, fb.minY() + 2, fb.minZ() + 3.5)), -45.0F, 0.0F, "deepsea-pillars");
			for (StructurePiece piece : pieces) {
				if (piece instanceof NetherFortressPieces.CastleSmallCorridorPiece) {
					shot(context, sp, at(piece, 2.5, 3.6, 0.5), piece.getOrientation().toYRot(), 5.0F, "deepsea-corridor");
					break;
				}
			}
			for (StructurePiece piece : pieces) {
				if (piece instanceof NetherFortressPieces.StairsRoom) {
					shot(context, sp, at(piece, 3.5, 4.2, 0.4), piece.getOrientation().toYRot(), 15.0F, "deepsea-lifts");
					break;
				}
			}
			BlockPos heart = ConduitShrine.heart(fb);
			server.runOnServer(s -> s.overworld().setBlock(heart, Blocks.CONDUIT.defaultBlockState(), 3));
			context.waitTicks(100);
			shot(context, sp, new Vec3(heart.getX() + 0.5, heart.getY() + 2.5, heart.getZ() - 5.0), 0.0F, 25.0F, "deepsea-shrine");
		}
	}

	/** The first spot at or above eye with open water for the camera (eye block and the one above). */
	private static Vec3 open(TestServerContext server, Vec3 eye) {
		return server.computeOnServer(s -> {
			for (int dy = 0; dy < 30; dy++) {
				BlockPos p = BlockPos.containing(eye.x, eye.y + dy, eye.z);
				if (s.overworld().getBlockState(p).is(Blocks.WATER) && s.overworld().getBlockState(p.above()).is(Blocks.WATER)) {
					return new Vec3(eye.x, p.getY() + 0.5, eye.z);
				}
			}
			return eye;
		});
	}

	/** The nearest deep-sea fortress start to the origin (its chunk corner). */
	private static BlockPos nearest(ServerLevel level) {
		Holder<Structure> fortress = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(DeepseaFortresses.STRUCTURE);
		Pair<BlockPos, Holder<Structure>> hit = level.getChunkSource().getGenerator()
				.findNearestMapStructure(level, HolderSet.direct(fortress), BlockPos.ZERO, 100, false);
		if (hit == null) {
			throw new AssertionError("no deep-sea fortress near the origin of seed " + SEED);
		}
		return hit.getFirst();
	}

	/** World position of local (x, y, z) in an oriented piece. */
	private static Vec3 at(StructurePiece piece, double x, double y, double z) {
		BoundingBox b = piece.getBoundingBox();
		Direction o = piece.getOrientation();
		double wx = switch (o) {
			case WEST -> b.maxX() + 1 - z;
			case EAST -> b.minX() + z;
			default -> b.minX() + x;
		};
		double wz = switch (o) {
			case NORTH -> b.maxZ() + 1 - z;
			case SOUTH -> b.minZ() + z;
			default -> b.minZ() + x;
		};
		return new Vec3(wx, b.minY() + y, wz);
	}

	private static void shot(ClientGameTestContext context, TestSingleplayerContext sp, Vec3 eye, float yaw, float pitch, String name) {
		context.runOnClient(mc -> mc.player.snapTo(eye.x, eye.y - mc.player.getEyeHeight(), eye.z, yaw, pitch));
		context.waitTicks(5);
		sp.getConnection().waitForChunksRender();
		context.waitTicks(30);
		context.takeScreenshot(name);
	}
}
