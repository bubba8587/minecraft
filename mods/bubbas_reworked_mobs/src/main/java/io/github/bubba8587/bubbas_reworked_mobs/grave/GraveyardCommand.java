// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.grave;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * {@code /bubbas_reworked_mobs graveyard place [crypt|nocrypt] [seed]} (ops): builds a crypt yard with its gate at your
 * feet, the yard running south from it, straight into the world (for previews, the test world and tests). Built like worldgen builds
 * it, chunk by chunk, but not saved as a structure start: /locate doesn't find it, and caretaker endermen treat it as
 * ordinary blocks (worldgen's yards are structures they leave alone).
 */
public final class GraveyardCommand {
	private GraveyardCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("graveyard")
				.then(Commands.literal("place")
						.executes(c -> place(c, true, c.getSource().getLevel().getRandom().nextLong()))
						.then(Commands.literal("crypt").executes(c -> place(c, true, c.getSource().getLevel().getRandom().nextLong()))
								.then(Commands.argument("seed", LongArgumentType.longArg()).executes(c -> place(c, true, LongArgumentType.getLong(c, "seed")))))
						.then(Commands.literal("nocrypt").executes(c -> place(c, false, c.getSource().getLevel().getRandom().nextLong()))
								.then(Commands.argument("seed", LongArgumentType.longArg()).executes(c -> place(c, false, LongArgumentType.getLong(c, "seed"))))));
	}

	private static int place(CommandContext<CommandSourceStack> c, boolean crypt, long seed) {
		ServerLevel level = c.getSource().getLevel();
		BlockPos at = BlockPos.containing(c.getSource().getPosition());
		BoundingBox box = placeAt(level, at, crypt, seed);
		c.getSource().sendSuccess(() -> Component.literal("Built a crypt yard%s from %d %d %d to %d %d %d (seed %d).".formatted(
				crypt ? " with a crypt" : "", box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ(), seed)), true);
		return 1;
	}

	/** As {@link #build}, returning the box. */
	public static BoundingBox placeAt(ServerLevel level, BlockPos gate, boolean crypt, long seed) {
		return build(level, gate, crypt, seed).getBoundingBox();
	}

	/** Builds a yard whose gate is at {@code gate} (its ground there), size from the seed; returns its piece. */
	public static CryptYardPiece build(ServerLevel level, BlockPos gate, boolean crypt, long seed) {
		java.util.Random random = new java.util.Random(seed);
		return build(level, gate, crypt, seed, 15 + 2 * random.nextInt(3), 15 + 2 * random.nextInt(3));
	}

	/** As above with a given size (tests fit a yard into their field). */
	public static CryptYardPiece build(ServerLevel level, BlockPos gate, boolean crypt, long seed, int width, int depth) {
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gate.getX(), gate.getZ()) - 1;
		BlockPos origin = new BlockPos(gate.getX() - width / 2, ground, gate.getZ());
		CryptYardPiece piece = new CryptYardPiece(seed, origin, width, depth, crypt, YardTrees.forBiome(level.getBiome(origin)));
		BoundingBox box = piece.getBoundingBox();
		for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
			for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
				ChunkPos chunk = new ChunkPos(cx, cz);
				BoundingBox chunkBox = new BoundingBox(chunk.getMinBlockX(), level.getMinY(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), level.getMaxY(),
						chunk.getMaxBlockZ());
				piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), chunkBox, chunk, gate);
			}
		}
		return piece;
	}
}
