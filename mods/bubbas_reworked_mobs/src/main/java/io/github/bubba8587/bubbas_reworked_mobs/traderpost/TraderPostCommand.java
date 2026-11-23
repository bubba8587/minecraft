// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.traderpost;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.bubba8587.bubbas_reworked_mobs.worldgen.ClippedBuild;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * {@code /bubbas_reworked_mobs trader_post build [seed]} builds a post centred where you stand; {@code trader_post
 * locate} finds the nearest natural one; {@code trader_post arrive} brings a wandering trader and its llamas to the
 * nearest post within 96 blocks now, as the daily spawn would.
 */
public final class TraderPostCommand {
	static final int LOCATE_RADIUS = 3000;

	private TraderPostCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("trader_post")
				.then(Commands.literal("build").executes(c -> place(c, c.getSource().getLevel().getRandom().nextLong()))
						.then(Commands.argument("seed", LongArgumentType.longArg()).executes(c -> place(c, LongArgumentType.getLong(c, "seed")))))
				.then(Commands.literal("locate").executes(TraderPostCommand::locate))
				.then(Commands.literal("arrive").executes(TraderPostCommand::arrive));
	}

	/** Builds a post now with its floor at the ground where {@code at} is (commands, tests). */
	public static TraderPostPiece build(ServerLevel level, BlockPos at, long seed) {
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()) - 1;
		TraderPostPiece piece = new TraderPostPiece(seed, new BlockPos(at.getX(), ground, at.getZ()));
		ClippedBuild.buildNow(level, piece, at);
		return piece;
	}

	private static int place(CommandContext<CommandSourceStack> c, long seed) {
		TraderPostPiece piece = build(c.getSource().getLevel(), BlockPos.containing(c.getSource().getPosition()), seed);
		c.getSource().sendSuccess(() -> Component.literal("Built a trader post at %s (seed %d).".formatted(piece.centre().toShortString(), seed)), true);
		return 1;
	}

	private static int locate(CommandContext<CommandSourceStack> c) {
		BlockPos at = BlockPos.containing(c.getSource().getPosition());
		TraderPostPiece piece = TraderPosts.nearest(c.getSource().getLevel(), at, LOCATE_RADIUS);
		if (piece == null) {
			c.getSource().sendFailure(Component.literal("No trader post within %d blocks.".formatted(LOCATE_RADIUS)));
			return 0;
		}
		BlockPos o = piece.centre();
		c.getSource().sendSuccess(() -> Component.literal("Nearest trader post at %d %d %d, %d blocks away.".formatted(o.getX(), o.getY(), o.getZ(),
				(int) Math.sqrt(o.distSqr(at)))), false);
		return 1;
	}

	private static int arrive(CommandContext<CommandSourceStack> c) {
		ServerLevel level = c.getSource().getLevel();
		TraderPostPiece piece = TraderPosts.nearest(level, BlockPos.containing(c.getSource().getPosition()), TraderPosts.DRAW_RANGE);
		if (piece == null) {
			c.getSource().sendFailure(Component.literal("No trader post within %d blocks.".formatted(TraderPosts.DRAW_RANGE)));
			return 0;
		}
		if (TraderPosts.arrive(level, piece) == null) {
			c.getSource().sendFailure(Component.literal("The post at %s isn't loaded, or its counter is blocked.".formatted(piece.centre().toShortString())));
			return 0;
		}
		c.getSource().sendSuccess(() -> Component.literal("A wandering trader arrived at the post at %s.".formatted(piece.centre().toShortString())), true);
		return 1;
	}
}
