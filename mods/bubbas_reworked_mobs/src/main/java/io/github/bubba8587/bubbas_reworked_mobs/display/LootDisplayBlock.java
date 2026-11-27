// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.display;

import net.minecraft.resources.ResourceKey;
import java.util.Optional;
import java.util.Map;
import net.minecraft.world.level.block.sounds.BlockSoundSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.sounds.BlockSoundSets;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Loot on display: one real item shown with its own item model (drawn by the client's block entity renderer; the
 * block model is invisible). Shared code, not undead-specific (specs/undead.md "Loot on display"). No ticker, no
 * collision: you walk over or through it, and it pops (dropping the item) when what holds it up goes.
 */
public class LootDisplayBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
	public static final EnumProperty<DisplayPlacement> PLACEMENT = EnumProperty.create("placement", DisplayPlacement.class);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	/** Hardness of a buried display (dug like packed dirt; shovels are fast, it is in #minecraft:mineable/shovel). */
	public static final float BURIED_HARDNESS = 0.8F;

	private static final VoxelShape GROUND_SHAPE = Block.column(14.0, 0.0, 1.5);
	private static final VoxelShape BURIED_SHAPE = Block.column(6.0, 0.0, 5.0);
	private static final VoxelShape STUCK_SHAPE = Block.column(6.0, 0.0, 13.0);
	/** Keyed by facing (away from the wall); the north-facing plate lies against the south face. */
	private static final Map<Direction, VoxelShape> WALL_SHAPES = Shapes.rotateHorizontal(Block.boxZ(12.0, 2.0, 14.0, 14.5, 16.0));

	public LootDisplayBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(PLACEMENT, DisplayPlacement.GROUND).setValue(FACING, Direction.NORTH)
				.setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(PLACEMENT, FACING, WATERLOGGED);
	}

	/** The state for a display at {@code pos} (waterlogged if there is water there). */
	public BlockState stateFor(LevelReader level, BlockPos pos, DisplayPlacement placement, Direction facing) {
		return defaultBlockState().setValue(PLACEMENT, placement).setValue(FACING, facing)
				.setValue(WATERLOGGED, level.getFluidState(pos).is(Fluids.WATER));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LootDisplayBlockEntity(pos, state);
	}

	/** The block entity renderer draws the item; the blockstate model only supplies the break particle. */
	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(PLACEMENT)) {
			case GROUND -> GROUND_SHAPE;
			case BURIED -> BURIED_SHAPE;
			case STUCK -> STUCK_SHAPE;
			case WALL -> WALL_SHAPES.get(state.getValue(FACING));
		};
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	/** Buried loot digs like dirt (slow by hand, quick with a shovel); the rest comes away at once, like a flower. */
	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		if (state.getValue(PLACEMENT) != DisplayPlacement.BURIED) {
			return super.getDestroyProgress(state, player, level, pos);
		}
		return player.getDestroySpeed(state) / BURIED_HARDNESS / 30.0F;
	}

	/** Buried loot sounds like the ground it is dug from; the rest like the metal it mostly is. */
	@Override
	protected Optional<ResourceKey<BlockSoundSet>> getSounds(BlockState state) {
		return Optional.of(state.getValue(PLACEMENT) == DisplayPlacement.BURIED ? BlockSoundSets.ROOTED_DIRT : BlockSoundSets.CHAIN);
	}

	// --- taking -----------------------------------------------------------------------------------

	/** A shovel digs up buried loot; any other held item does its own thing (only an empty hand takes the rest). */
	@Override
	protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hitResult) {
		if (state.getValue(PLACEMENT) == DisplayPlacement.BURIED && itemStack.is(ItemTags.SHOVELS)) {
			if (level instanceof ServerLevel serverLevel && LootDisplays.digUp(serverLevel, pos, player, hand)) {
				return InteractionResult.SUCCESS_SERVER;
			}
			return InteractionResult.SUCCESS;
		}
		return itemStack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
	}

	/** Empty hand: take the item (buried loot only shows a hint: it needs a shovel). */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (state.getValue(PLACEMENT) == DisplayPlacement.BURIED) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable(LootDisplays.BURIED_HINT));
			}
			return InteractionResult.SUCCESS;
		}
		if (level instanceof ServerLevel serverLevel) {
			LootDisplays.take(serverLevel, pos, player);
		}
		return InteractionResult.SUCCESS;
	}

	/** Middle click gives (in creative) the displayed item itself. */
	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return level.getBlockEntity(pos) instanceof LootDisplayBlockEntity display ? display.getItem().copy() : ItemStack.EMPTY;
	}

	// --- support, rotation, water ---------------------------------------------------------------

	/** Floor placements need a floor (a centre-sturdy top face, as a torch does); wall placement needs the wall behind it. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (state.getValue(PLACEMENT) == DisplayPlacement.WALL) {
			Direction facing = state.getValue(FACING);
			BlockPos wall = pos.relative(facing.getOpposite());
			return level.getBlockState(wall).isFaceSturdy(level, wall, facing, SupportType.CENTER);
		}
		return Block.canSupportCenter(level, pos.below(), Direction.UP);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
			BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		if (state.getValue(WATERLOGGED)) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		if (!state.canSurvive(level, pos)) {
			return state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return type != PathComputationType.WATER || state.getValue(WATERLOGGED);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource() : super.getFluidState(state);
	}
}
