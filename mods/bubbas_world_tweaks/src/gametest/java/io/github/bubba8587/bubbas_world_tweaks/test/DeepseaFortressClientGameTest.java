// dte:C8
package io.github.bubba8587.bubbas_world_tweaks.test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.github.bubba8587.bubbas_world_tweaks.fortress.ConduitShrine;
import io.github.bubba8587.bubbas_world_tweaks.fortress.DeepseaFortressStructure;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** A deep-sea fortress by eye: outside underwater, a corridor, the spawner room. */
@SuppressWarnings("UnstableApiUsage")
public class DeepseaFortressClientGameTest implements FabricClientGameTest {
	private static final int FLOOR = 70;
	private static final int SEA = 110;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ClientTests.run("deepsea")) {
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder()
				.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			sp.getConnection().waitForChunksRender();
			TestServerContext server = sp.getServer();
			BlockPos base = context.computeOnClient(mc -> mc.player.blockPosition());
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("time set 6000");
			server.runCommand("gamemode spectator @p");
			server.runCommand("effect give @p night_vision infinite 0 true");
			AtomicReference<StructureStart> placed = new AtomicReference<>();
			server.runOnServer(s -> placed.set(build(s.overworld(), base.getX(), base.getZ())));
			StructureStart start = placed.get();
			List<StructurePiece> pieces = start.getPieces();

			StructurePiece first = pieces.getFirst();
			BoundingBox fb = first.getBoundingBox();
			shot(context, sp, new Vec3(fb.minX() - 26, fb.minY() + 12, fb.getCenter().getZ() + 0.5), -90.0F, 15.0F, "deepsea-outside");
			shot(context, sp, new Vec3(fb.minX() - 22, FLOOR + 3, fb.getCenter().getZ() + 0.5), -90.0F, -12.0F, "deepsea-pillars");
			shot(context, sp, new Vec3(fb.getCenter().getX() + 0.5, start.getBoundingBox().maxY() + 3, fb.minZ() - 18), 0.0F, 30.0F, "deepsea-outside-high");

			for (StructurePiece piece : pieces) {
				if (piece instanceof NetherFortressPieces.CastleSmallCorridorPiece) {
					Vec3 eye = at(piece, 2.5, 3.6, 0.5);
					shot(context, sp, eye, piece.getOrientation().toYRot(), 5.0F, "deepsea-corridor");
					break;
				}
			}
			for (StructurePiece piece : pieces) {
				if (piece instanceof NetherFortressPieces.MonsterThrone) {
					shot(context, sp, at(piece, 3.5, 6.6, 0.5), piece.getOrientation().toYRot(), 30.0F, "deepsea-spawner-room");
					break;
				}
			}
			for (StructurePiece piece : pieces) {
				if (piece instanceof NetherFortressPieces.StairsRoom) {
					shot(context, sp, at(piece, 3.5, 4.2, 0.4), piece.getOrientation().toYRot(), 15.0F, "deepsea-lifts");
					shot(context, sp, at(piece, 3.5, 9.6, 0.4), piece.getOrientation().toYRot(), 35.0F, "deepsea-lifts-landing");
					break;
				}
			}
			BlockPos heart = ConduitShrine.heart(first.getBoundingBox());
			server.runOnServer(s -> s.overworld().setBlock(heart, Blocks.CONDUIT.defaultBlockState(), 3));
			context.waitTicks(100);
			shot(context, sp, new Vec3(heart.getX() + 0.5, heart.getY() + 2.5, heart.getZ() - 5.0), 0.0F, 25.0F, "deepsea-shrine");
			for (StructurePiece piece : pieces) {
				if (piece instanceof NetherFortressPieces.CastleEntrance) {
					shot(context, sp, at(piece, 6.5, 6.0, 1.5), piece.getOrientation().toYRot(), 20.0F, "deepsea-hall");
					break;
				}
			}
		}
	}

	/** Floods a sea over the fortress's footprint, then places a fortress there. */
	private static StructureStart build(ServerLevel level, int x, int z) {
		long seed = 1;
		BoundingBox box = null;
		for (; seed < 50; seed++) {
			StructurePiecesBuilder trial = new StructurePiecesBuilder();
			DeepseaFortressStructure.layOut(trial, RandomSource.create(seed), x, z, FLOOR, SEA);
			if (trial.build().pieces().stream().anyMatch(p -> p instanceof NetherFortressPieces.MonsterThrone)
					&& trial.build().pieces().stream().anyMatch(p -> p instanceof NetherFortressPieces.CastleSmallCorridorPiece)
					&& trial.build().pieces().stream().anyMatch(p -> p instanceof NetherFortressPieces.StairsRoom)) {
				box = trial.getBoundingBox();
				break;
			}
		}
		BlockState water = Blocks.WATER.defaultBlockState();
		BlockState sand = Blocks.SAND.defaultBlockState();
		int top = SEA - 1;
		for (BlockPos p : BlockPos.betweenClosed(box.minX() - 24, FLOOR - 2, box.minZ() - 24, box.maxX() + 24, top + 12, box.maxZ() + 24)) {
			level.getChunk(p.getX() >> 4, p.getZ() >> 4);
			BlockState want = p.getY() <= FLOOR ? sand : p.getY() <= top ? water : Blocks.AIR.defaultBlockState();
			if (level.getBlockState(p) != want) {
				level.setBlock(p, want, 2);
			}
		}
		return DeepseaTestPieces.fortress(level, seed, x, z, FLOOR, SEA);
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
		context.waitTicks(40);
		context.takeScreenshot(name);
	}
}
