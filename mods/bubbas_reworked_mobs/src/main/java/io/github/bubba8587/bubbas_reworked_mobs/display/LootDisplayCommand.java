// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.display;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * {@code /bubbas_reworked_mobs display <placement> <item> [<pos> [north|south|east|west]]}: for creative building and
 * tests (ops only). Without a position it goes where you are looking: on the wall you look at for {@code wall}, on
 * the floor you look at otherwise, turned toward you. How survival players place displays is not decided yet.
 */
public final class LootDisplayCommand {
	private static final String KEY = "commands." + BubbasReworkedMobs.MOD_ID + ".display.";
	private static final double REACH = 20.0;
	private static final SimpleCommandExceptionType NOT_LOOKING = new SimpleCommandExceptionType(Component.translatable(KEY + "not_looking"));
	private static final SimpleCommandExceptionType NEEDS_WALL = new SimpleCommandExceptionType(Component.translatable(KEY + "needs_wall"));
	private static final SimpleCommandExceptionType NO_ROOM = new SimpleCommandExceptionType(Component.translatable(KEY + "no_room"));
	private static final SimpleCommandExceptionType NO_SUPPORT = new SimpleCommandExceptionType(Component.translatable(KEY + "no_support"));

	private LootDisplayCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> build(CommandBuildContext context) {
		LiteralArgumentBuilder<CommandSourceStack> display = Commands.literal("display");
		for (DisplayPlacement placement : DisplayPlacement.values()) {
			ArgumentBuilder<CommandSourceStack, ?> at = Commands.argument("pos", BlockPosArgument.blockPos())
					.executes(c -> placeAt(c, placement, c.getSource().getEntity() != null
							? c.getSource().getEntity().getDirection().getOpposite() : Direction.NORTH));
			for (Direction facing : Direction.Plane.HORIZONTAL) {
				at.then(Commands.literal(facing.getSerializedName()).executes(c -> placeAt(c, placement, facing)));
			}
			display.then(Commands.literal(placement.getSerializedName())
					.then(Commands.argument("item", ItemArgument.item())
							.executes(c -> placeLooking(c, placement))
							.then(at)));
		}
		return display;
	}

	private static int placeLooking(CommandContext<CommandSourceStack> c, DisplayPlacement placement) throws CommandSyntaxException {
		Entity entity = c.getSource().getEntityOrException();
		if (!(entity.pick(REACH, 1.0F, false) instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
			throw NOT_LOOKING.create();
		}
		Direction face = hit.getDirection();
		if (placement == DisplayPlacement.WALL && !face.getAxis().isHorizontal()) {
			throw NEEDS_WALL.create();
		}
		BlockPos pos = hit.getBlockPos().relative(face);
		Direction facing = placement == DisplayPlacement.WALL ? face : entity.getDirection().getOpposite();
		return place(c, pos, placement, facing);
	}

	private static int placeAt(CommandContext<CommandSourceStack> c, DisplayPlacement placement, Direction facing) throws CommandSyntaxException {
		return place(c, BlockPosArgument.getLoadedBlockPos(c, "pos"), placement, facing);
	}

	private static int place(CommandContext<CommandSourceStack> c, BlockPos pos, DisplayPlacement placement, Direction facing)
			throws CommandSyntaxException {
		ServerLevel level = c.getSource().getLevel();
		ItemStack stack = ItemArgument.getItem(c, "item").createItemStack(1);
		BlockState there = level.getBlockState(pos);
		if (!there.canBeReplaced() && !there.is(ModBlocks.LOOT_DISPLAY)) {
			throw NO_ROOM.create();
		}
		BlockState state = ModBlocks.LOOT_DISPLAY.stateFor(level, pos, placement, facing);
		if (!state.canSurvive(level, pos)) {
			throw NO_SUPPORT.create();
		}
		LootDisplays.place(level, pos, placement, facing, stack);
		c.getSource().sendSuccess(() -> Component.translatable(KEY + "success", stack.getDisplayName(), placement.getSerializedName(),
				pos.getX(), pos.getY(), pos.getZ()), true);
		return 1;
	}
}
