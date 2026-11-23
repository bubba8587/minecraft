// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper;

import io.github.bubba8587.bubbas_reworked_mobs.undead.SleeperPose;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A dormant undead: block + block entity, modeled on vanilla's copper golem statue. No ticker: hearing is a
 * game-event listener on the block entity, and waking is a scheduled block tick ({@link #tick}).
 */
public class SleeperBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<SleeperPose> POSE = EnumProperty.create("pose", SleeperPose.class);
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	/**
	 * The resting dead (owner, 2026-11-22, specs/graveyards.md "The resting dead"): a sleeper in a yard grave never
	 * wakes, whatever the noise, bell or robbery. Block state, so a mined one keeps it as an item and stays asleep
	 * wherever it is placed (decoration).
	 */
	public static final BooleanProperty RESTING = BooleanProperty.create("resting");
	private static final VoxelShape SITTING_SHAPE = Block.column(12.0, 0.0, 12.0);
	private static final VoxelShape LYING_SHAPE = Block.column(16.0, 0.0, 5.0);

	private final UndeadVariant variant;

	public SleeperBlock(UndeadVariant variant, BlockBehaviour.Properties properties) {
		super(properties);
		this.variant = variant;
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POSE, variant.poses().getFirst()).setValue(WATERLOGGED, false)
				.setValue(RESTING, false));
	}

	/** Whether this sleeper is one of the resting dead, who never wake. */
	public static boolean isResting(BlockState state) {
		return state.hasProperty(RESTING) && state.getValue(RESTING);
	}

	public UndeadVariant variant() {
		return variant;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING, POSE, WATERLOGGED, RESTING);
	}

	/** Faces the player who places it (the pose comes from the item's block_state component, applied by BlockItem). */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED, fluid.is(Fluids.WATER));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SleeperBlockEntity(pos, state);
	}

	/** The block entity renderer draws the whole sleeper (v0); the blockstate model only supplies the break particle. */
	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(POSE) == SleeperPose.LYING ? LYING_SHAPE : SITTING_SHAPE;
	}

	/** Robbing: an empty hand takes one item (weapon first, then armor top-down) and may wake the sleeper. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!(level.getBlockEntity(pos) instanceof SleeperBlockEntity sleeper) || !sleeper.hasGear()) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel serverLevel) {
			SleeperWaking.rob(serverLevel, pos, sleeper, player);
		}
		return InteractionResult.SUCCESS;
	}

	/** Scheduled by hearing or a failed robbing roll: the travel delay is over, wake up. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof SleeperBlockEntity sleeper && sleeper.isWakePending()) {
			SleeperWaking.wake(level, pos, state, sleeper);
		}
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		ItemStack stack = new ItemStack(this);
		if (includeData && level.getBlockEntity(pos) instanceof SleeperBlockEntity sleeper) {
			stack.applyComponents(sleeper.collectComponents());
		}
		stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(POSE, state.getValue(POSE)));
		return stack;
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
		return false;
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
			BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		if (state.getValue(WATERLOGGED)) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}
}
