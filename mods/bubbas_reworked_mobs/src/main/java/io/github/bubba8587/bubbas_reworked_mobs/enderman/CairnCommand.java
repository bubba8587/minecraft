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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@code /bubbas_reworked_mobs cairn locate|build|builders|here [seed]} (ops only), for finding and previewing cairns:
 * {@code locate} names the nearest site within 1024 blocks and its progress; {@code build} puts up the nearest site's
 * whole design (within 128 blocks); {@code builders} summons, where the command runs, one persistent
 * enderman per missing block of the nearest site within 64 (at most 12), each carrying its block, so they build it; {@code here} adds a site at your feet (on flat ground), saved with the world, and builds it.
 */
public final class CairnCommand {
	private CairnCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("cairn")
				.then(Commands.literal("locate").executes(CairnCommand::locate))
				.then(Commands.literal("build").executes(CairnCommand::buildNearest))
				.then(Commands.literal("builders").executes(CairnCommand::builders))
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

	private static int builders(CommandContext<CommandSourceStack> c) {
		ServerLevel level = c.getSource().getLevel();
		var at = c.getSource().getPosition();
		CairnSite site = CairnSites.nearestNow(level, BlockPos.containing(at), 64);
		if (site == null) {
			c.getSource().sendFailure(Component.literal("No cairn site within 64 blocks."));
			return 0;
		}
		int summoned = 0;
		for (BlockPos pos : site.order()) {
			if (summoned >= 12) {
				break;
			}
			BlockState want = site.at(pos);
			if (want == null || TerrainKinds.matches(level.getBlockState(pos), want)) {
				continue;
			}
			Enderman enderman = EntityTypes.ENDERMAN.create(level, EntitySpawnReason.COMMAND);
			if (enderman == null) {
				break;
			}
			double angle = summoned * 0.9;
			enderman.snapTo(at.x + Math.cos(angle) * 1.5, at.y, at.z + Math.sin(angle) * 1.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
			enderman.setCarriedBlock(want);
			enderman.setPersistenceRequired();
			level.addFreshEntity(enderman);
			summoned++;
		}
		int count = summoned;
		c.getSource().sendSuccess(() -> Component.literal("Summoned " + count + " builders for " + site), true);
		return count;
	}

	private static int here(CommandContext<CommandSourceStack> c, long seed) {
		ServerLevel level = c.getSource().getLevel();
		CairnSite site = CairnSites.addSavedSite(level, BlockPos.containing(c.getSource().getPosition()), seed);
		site.buildAll(level);
		c.getSource().sendSuccess(() -> Component.literal("Built " + site + ", seed " + seed), true);
		return 1;
	}
}
