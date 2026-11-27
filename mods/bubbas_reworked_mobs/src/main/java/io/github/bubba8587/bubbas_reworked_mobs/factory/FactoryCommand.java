// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.factory;

import net.minecraft.commands.suggestions.SuggestionContext;
import java.util.Arrays;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;

/**
 * {@code /bubbas_reworked_mobs ancient_fabricator ...} (ops only), specs/mob-factory.md:
 * {@code locate} names the nearest Ancient Fabricator within 3000 blocks (natural ones beside ancient cities, and
 * hand-placed ones) and whether it still runs; {@code here <mob> [seed]} and {@code build <pos> <mob> <seed> <turns>}
 * put a whole one up (platform's middle at the position), saved with the world so it runs like a natural one.
 */
public final class FactoryCommand {
	private FactoryCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("ancient_fabricator")
				.then(Commands.literal("locate").executes(FactoryCommand::locate))
				.then(Commands.literal("here")
						.then(Commands.argument("mob", StringArgumentType.word()).suggests(FactoryCommand::mobs)
								.executes(c -> place(c, BlockPos.containing(c.getSource().getPosition()), c.getSource().getLevel().getRandom().nextLong(), 0))
								.then(Commands.argument("seed", LongArgumentType.longArg())
										.executes(c -> place(c, BlockPos.containing(c.getSource().getPosition()), LongArgumentType.getLong(c, "seed"), 0)))))
				.then(Commands.literal("build")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
								.then(Commands.argument("mob", StringArgumentType.word()).suggests(FactoryCommand::mobs)
										.then(Commands.argument("seed", LongArgumentType.longArg())
												.then(Commands.argument("turns", IntegerArgumentType.integer(0, 3))
														.executes(c -> place(c, BlockPosArgument.getBlockPos(c, "pos"), LongArgumentType.getLong(c, "seed"),
																IntegerArgumentType.getInteger(c, "turns"))))))));
	}

	private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> mobs(
			CommandContext<CommandSourceStack> c, com.mojang.brigadier.suggestion.SuggestionsBuilder b) {
		return SuggestionContext.suggest(Arrays.stream(FactoryDesign.Mob.values()).map(FactoryDesign.Mob::id), b);
	}

	private static int locate(CommandContext<CommandSourceStack> c) {
		ServerLevel level = c.getSource().getLevel();
		BlockPos at = BlockPos.containing(c.getSource().getPosition());
		FabricatorSite site = FabricatorSites.nearest(level, at, 3000);
		if (site == null) {
			c.getSource().sendFailure(Component.literal("No Ancient Fabricator within 3000 blocks."));
			return 0;
		}
		BlockPos p = site.pad();
		boolean broken = FabricatorSites.has(level, site, FabricatorSites.BROKEN);
		c.getSource().sendSuccess(() -> Component.literal("Ancient Fabricator making %s: pad at %d %d %d, %d blocks away; %s.".formatted(
				site.mob().id(), p.getX(), p.getY(), p.getZ(), (int) Math.sqrt(p.distSqr(at)), broken ? "stopped for good" : "running")), false);
		return 1;
	}

	private static int place(CommandContext<CommandSourceStack> c, BlockPos origin, long seed, int turns) throws CommandSyntaxException {
		FactoryDesign.Mob mob;
		try {
			mob = FactoryDesign.Mob.byId(StringArgumentType.getString(c, "mob"));
		} catch (IllegalArgumentException e) {
			c.getSource().sendFailure(Component.literal("No Ancient Fabricator mob " + StringArgumentType.getString(c, "mob")));
			return 0;
		}
		ServerLevel level = c.getSource().getLevel();
		FabricatorSite site = FabricatorSites.addSavedSite(level, new FabricatorSite(origin, Rotation.values()[turns], seed, mob, null));
		site.design().place(level, origin, site.rotation());
		Fabricators.prepare(level, site);
		BlockPos spawner = site.spawner();
		c.getSource().sendSuccess(() -> Component.literal("Built an Ancient Fabricator making %s (seed %d, %d blocks); spawner buried at %d %d %d".formatted(
				mob.id(), seed, site.design().size(), spawner.getX(), spawner.getY(), spawner.getZ())), true);
		return 1;
	}
}
