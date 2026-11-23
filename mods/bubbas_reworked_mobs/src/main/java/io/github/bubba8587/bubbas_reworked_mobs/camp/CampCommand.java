// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.camp;

import io.github.bubba8587.bubbas_reworked_mobs.worldgen.ClippedBuild;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * {@code /bubbas_reworked_mobs camp build <kind> <size> [seed]} builds a camp centred where you stand and registers it
 * (its roster appears when you're in range, as for a natural camp). {@code camp reset} runs the daily reset check now (whoever is near),
 * sure to reset every cleared, armed camp nobody is near; {@code camp list} lists the registered camps.
 */
public final class CampCommand {
	private CampCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("camp")
				.then(Commands.literal("build").then(Commands.argument("kind", StringArgumentType.word())
						.then(Commands.argument("size", StringArgumentType.word()).executes(c -> place(c, c.getSource().getLevel().getRandom().nextLong()))
								.then(Commands.argument("seed", LongArgumentType.longArg()).executes(c -> place(c, LongArgumentType.getLong(c, "seed")))))))
				.then(Commands.literal("reset").executes(c -> {
					int n = CampSites.resetCheck(c.getSource().getLevel(), true);
					c.getSource().sendSuccess(() -> Component.literal("Reset " + n + " camp(s)."), true);
					return n;
				}))
				.then(Commands.literal("list").executes(c -> {
					var all = CampSites.all(c.getSource().getLevel());
					for (CampSite site : all) {
						c.getSource().sendSuccess(() -> Component.literal("%s at %s: %s, %s, %d alive".formatted(site.isOutpost() ? "outpost" : site.kind().id(),
								site.centre().toShortString(), site.armed() ? "armed" : "no banner", site.spawned() ? "spawned" : "waiting",
								site.members().size())), false);
					}
					return all.size();
				}));
	}

	private static int place(CommandContext<CommandSourceStack> c, long seed) {
		try {
			CampKind kind = CampKind.valueOf(StringArgumentType.getString(c, "kind").toUpperCase(java.util.Locale.ROOT));
			CampTuning.Size size = CampTuning.Size.valueOf(StringArgumentType.getString(c, "size").toUpperCase(java.util.Locale.ROOT));
			CampPiece piece = build(c.getSource().getLevel(), BlockPos.containing(c.getSource().getPosition()), kind, size, seed);
			c.getSource().sendSuccess(() -> Component.literal("Built a %s %s camp at %s (seed %d).".formatted(size.id(), kind.id(),
					piece.centre().toShortString(), seed)), true);
			return 1;
		} catch (IllegalArgumentException e) {
			c.getSource().sendFailure(Component.literal("Kinds: skeleton, zombie, husk, stray, bogged. Sizes: small, medium, large."));
			return 0;
		}
	}

	/** Builds a camp centred on {@code at}'s column (on its ground) and registers it; returns its piece. */
	public static CampPiece build(ServerLevel level, BlockPos at, CampKind kind, CampTuning.Size size, long seed) {
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()) - 1;
		CampPiece piece = new CampPiece(seed, new BlockPos(at.getX(), ground, at.getZ()), kind, size);
		ClippedBuild.buildNow(level, piece, at);
		CampSites.register(level, piece);
		return piece;
	}
}
