// dte:C6,B15
package io.github.bubba8587.bubbas_totems_artifacts.statue;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import io.github.bubba8587.bubbas_totems_artifacts.seeking.SeekingMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A totem set down as a statue. */
public class TotemStatueBlock extends HorizontalDirectionalBlock {
	public static final EnumProperty<TotemType> TOTEM = EnumProperty.create("totem", TotemType.class);
	/** The statue. */
	private static final VoxelShape SHAPE = Block.column(10.0, 0.0, 16.0);

	public TotemStatueBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, net.minecraft.core.Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, TOTEM);
	}

	/** Which totem this statue shows. */
	public static TotemType type(BlockState state) {
		return state.getValue(TOTEM);
	}

	/** The statue state for type. */
	public static BlockState stateFor(TotemType type) {
		return ModBlocks.TOTEM_STATUE.defaultBlockState().setValue(TOTEM, type);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!level.isClientSide() && StatueEffects.pulses(type(state))) {
			level.scheduleTick(pos, this, 1);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		TotemType type = type(state);
		if (StatueEffects.pulses(type)) {
			StatueEffects.pulse(level, pos, type);
			level.scheduleTick(pos, this, io.github.bubba8587.bubbas_totems_artifacts.totems.TotemsTuning.STATUE_PULSE_TICKS);
		}
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return StatueEffects.pulses(type(state));
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
			level.scheduleTick(pos, this, 1);
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (type(state) != TotemType.SEEKING || !SeekingMap.accepts(stack)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level instanceof ServerLevel serverLevel) {
			return SeekingMap.insert(serverLevel, pos, player, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		if (type(state) == TotemType.SEEKING) {
			SeekingMap.removed(level, pos);
		}
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** A totem item used on a block. */
	public static InteractionResult placeFrom(UseOnContext use, TotemType type) {
		Player player = use.getPlayer();
		if (player == null || !player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		BlockPlaceContext context = new BlockPlaceContext(use);
		if (!context.canPlace()) {
			return InteractionResult.FAIL;
		}
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = stateFor(type).setValue(FACING, context.getHorizontalDirection().getOpposite());
		if (!state.canSurvive(level, pos) || !level.isUnobstructed(state, pos, CollisionContext.placementContext(player))
				|| !level.setBlock(pos, state, Block.UPDATE_ALL_IMMEDIATE)) {
			return InteractionResult.FAIL;
		}
		ItemStack stack = context.getItemInHand();
		if (player instanceof ServerPlayer serverPlayer) {
			CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, stack);
		}
		SoundType sound = state.getSoundType();
		level.playSound(player, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
		level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}
}
