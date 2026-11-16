// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.enderman;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * {@code /bubbas_reworked_mobs cairn locate|build|here [seed]} (ops only), for finding and previewing cairns:
 * {@code locate} names the nearest site within 1024 blocks and its progress; {@code build} puts up the nearest site's
 * whole design (within 128 blocks); {@code here} adds a site at your feet (on flat ground) and builds it.
 */
public final class CairnCommand {
	private CairnCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("cairn")
				.then(Commands.literal("locate").executes(CairnCommand::locate))
				.then(Commands.literal("build").executes(CairnCommand::buildNearest))
				.then(Commands.literal("here").executes(c -> here(c, c.getSource().getLevel().getRandom().nextLong()))
						.then(Commands.argument("seed", LongArgumentType.longArg()).executes(c -> here(c, LongArgumentType.getLong(c, "seed")))));
	}

	private static int locate(CommandContext<CommandSourceStack> c) {
		ServerLevel level = c.getSource().getLevel();
		BlockPos at = BlockPos.containing(c.getSource().getPosition());
		CairnSite site = CairnSites.nearestNow(level, at, 1024);
		if (site == null) {
			c.getSource().sendFailure(Component.literal("No cairn site within 1024 blocks."));
			return 0;
		}
		BlockPos o = site.origin();
		int standing = level.isLoaded(o) ? site.standing(level) : -1;
		c.getSource().sendSuccess(() -> Component.literal("Cairn site at %d %d %d, %d blocks away; %s; its top rod points %s.".formatted(
				o.getX(), o.getY(), o.getZ(), (int) Math.sqrt(o.distSqr(at)),
				standing < 0 ? "not loaded" : standing + " of " + site.size() + " blocks standing", site.pointer().getSerializedName())), false);
		return 1;
	}

	private static int buildNearest(CommandContext<CommandSourceStack> c) {
		ServerLevel level = c.getSource().getLevel();
		CairnSite site = CairnSites.nearestNow(level, BlockPos.containing(c.getSource().getPosition()), 128);
		if (site == null) {
			c.getSource().sendFailure(Component.literal("No cairn site within 128 blocks."));
			return 0;
		}
		site.buildAll(level);
		c.getSource().sendSuccess(() -> Component.literal("Built " + site), true);
		return 1;
	}

	private static int here(CommandContext<CommandSourceStack> c, long seed) {
		ServerLevel level = c.getSource().getLevel();
		CairnSite site = CairnSites.addSite(level, BlockPos.containing(c.getSource().getPosition()), seed);
		site.buildAll(level);
		c.getSource().sendSuccess(() -> Component.literal("Built " + site + ", seed " + seed), true);
		return 1;
	}
}
