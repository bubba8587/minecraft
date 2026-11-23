// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.spider;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;

/** {@code /bubbas_reworked_mobs spider nest|giant}: builds a nest in the room above, or a giant spider with its webs. */
public final class SpiderCommand {
	private SpiderCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("spider")
				.then(Commands.literal("nest").executes(SpiderCommand::nest))
				.then(Commands.literal("giant").executes(SpiderCommand::giant));
	}

	private static int nest(CommandContext<CommandSourceStack> c) {
		ServerLevel level = c.getSource().getLevel();
		BlockPos at = BlockPos.containing(c.getSource().getPosition());
		int drop = SpiderNestFeature.findCeiling(level, at.getX(), at.getZ(), at.getY() + 40, at.getY(), level.getRandom());
		if (drop == Integer.MIN_VALUE) {
			c.getSource().sendFailure(Component.literal("No ceiling with a tall enough room above " + at.toShortString() + "."));
			return 0;
		}
		c.getSource().sendSuccess(() -> Component.literal("Built a spider nest above " + at.toShortString() + " (drop " + drop + ")."), true);
		return 1;
	}

	private static int giant(CommandContext<CommandSourceStack> c) {
		ServerLevel level = c.getSource().getLevel();
		BlockPos at = BlockPos.containing(c.getSource().getPosition());
		GiantSpider giant = Spiders.GIANT_SPIDER.create(level, EntitySpawnReason.COMMAND);
		if (giant == null) {
			c.getSource().sendFailure(Component.literal("Couldn't make a giant spider."));
			return 0;
		}
		giant.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
		giant.setPersistenceRequired();
		level.addFreshEntity(giant);
		int strands = 0;
		for (int i = 0; i < 4; i++) {
			strands += giant.spin(level);
		}
		int total = strands;
		c.getSource().sendSuccess(() -> Component.literal("Giant spider at " + at.toShortString() + ", " + total + " web strands spun."), true);
		return 1;
	}
}
