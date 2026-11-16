// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.factory;

import java.util.Arrays;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.block.Rotation;

/**
 * {@code /bubbas_reworked_mobs ancient_fabricator here <mob> [seed]} and {@code ancient_fabricator build <pos> <mob> <seed> <turns>}
 * (ops only): puts up a whole Ancient Fabricator, the mob factory of specs/mob-factory.md (owner named them 2026-11-16), with its platform's middle at the position, for
 * previews. Look only for now (owner, 2026-11-16: the look is reviewed before mechanics): the Warden stands in its
 * window without AI and nothing runs.
 */
public final class FactoryCommand {
	private FactoryCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("ancient_fabricator")
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
		return SharedSuggestionProvider.suggest(Arrays.stream(FactoryDesign.Mob.values()).map(FactoryDesign.Mob::id), b);
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
		Rotation rotation = Rotation.values()[turns];
		FactoryDesign design = FactoryDesign.design(seed, mob);
		design.place(level, origin, rotation);
		spawnWarden(level, design, origin, rotation);
		BlockPos spawner = FactoryDesign.toWorld(origin, design.spawner(), rotation);
		c.getSource().sendSuccess(() -> Component.literal("Built an Ancient Fabricator making %s (seed %d, %d blocks); spawner buried at %d %d %d".formatted(
				mob.id(), seed, design.size(), spawner.getX(), spawner.getY(), spawner.getZ())), true);
		return 1;
	}

	/** The Warden at its window, looking out over the platform. Without AI for now (look only). */
	static void spawnWarden(ServerLevel level, FactoryDesign design, BlockPos origin, Rotation rotation) {
		Warden warden = EntityTypes.WARDEN.create(level, EntitySpawnReason.STRUCTURE);
		if (warden == null) {
			return;
		}
		BlockPos at = FactoryDesign.toWorld(origin, design.wardenSpot(), rotation);
		// Local -x (out of the window) is yaw 90; each clockwise turn adds 90.
		float yaw = 90.0F + 90.0F * rotation.ordinal();
		warden.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 25.0F);
		warden.setYHeadRot(yaw);
		warden.setYBodyRot(yaw);
		warden.setNoAi(true);
		warden.setPersistenceRequired();
		level.addFreshEntity(warden);
	}
}
