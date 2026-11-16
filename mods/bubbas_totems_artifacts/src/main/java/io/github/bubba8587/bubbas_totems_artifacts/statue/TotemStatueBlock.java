// dte:C6,B15
package io.github.bubba8587.bubbas_totems_artifacts.statue;

import io.github.bubba8587.bubbas_totems_artifacts.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
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

/**
 * A totem set down as a statue (owner, 2026-11-16; B15 exception: one shared block for every totem, the owner's
 * request). Purely decorative: no pulse, no ward, no beacon effect, no block entity, no ticking. Mining it drops the
 * totem back (loot table blocks/totem_statue). Placed from the totem item by sneaking and using it on a block
 * ({@link #placeFrom}).
 *
 * <p>Which totem it is: the {@code totem} property ({@link TotemType}, each with its own model). Minecraft refuses a
 * block state property with a single value, so while Warding is the only totem the property is left off the block
 * and every statue is a warding statue ({@link #type}). When the second totem arrives the property switches on by
 * itself; statues already in worlds then load with the default value, warding, which is what they are. At that point
 * add the variants to blockstates/totem_statue.json and a block_state_property condition per type to the loot table.
 */
public class TotemStatueBlock extends HorizontalDirectionalBlock {
	public static final EnumProperty<TotemType> TOTEM = EnumProperty.create("totem", TotemType.class);
	/** Whether the block has the {@link #TOTEM} property yet (two or more totems). */
	public static final boolean HAS_TYPES = TotemType.values().length > 1;
	/** The placeholder statue: a 10x10 plinth and an 8-wide column, 14 high. */
	private static final VoxelShape SHAPE = Block.column(10.0, 0.0, 14.0);

	public TotemStatueBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, net.minecraft.core.Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
		if (HAS_TYPES) {
			builder.add(TOTEM);
		}
	}

	/** Which totem this statue shows. */
	public static TotemType type(BlockState state) {
		return state.hasProperty(TOTEM) ? state.getValue(TOTEM) : TotemType.WARDING;
	}

	/** The statue state for {@code type}. */
	public static BlockState stateFor(TotemType type) {
		BlockState state = ModBlocks.TOTEM_STATUE.defaultBlockState();
		return HAS_TYPES ? state.setValue(TOTEM, type) : state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/**
	 * A totem item used on a block: while sneaking, set it down as a statue (the totem is used up, as a block item
	 * would be). Without sneaking it passes, so holding a totem never places it by accident. Mirrors BlockItem.place.
	 */
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
