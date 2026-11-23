// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.grave;

import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A gravestone (specs/graveyards.md; B15 reason, owner-approved 2026-11-22: no vanilla block has a headstone shape that
 * carries carved text). "Keep it simple, it should lift Signs" (owner): it is vanilla's standing sign in all but looks.
 * Text front and back, editing on placement, dyes, glow ink, honeycomb wax and the 16-way rotation all come from
 * {@link StandingSignBlock} and {@link SignBlockEntity} unchanged. What differs: its own stone {@link WoodType} (stone
 * sounds), its own edit screen (GravestoneEditScreen, client), its own block
 * entity type (narrower lines to fit the stone's face), a headstone model, and a solid shape you can't walk through.
 */
public class GravestoneBlock extends StandingSignBlock {
	/**
	 * The stone "wood type". Deliberately not registered with {@code WoodType.register}: registering would make vanilla
	 * build sign sprite materials for it, which a gravestone doesn't use. Only its
	 * sounds are read.
	 */
	public static final WoodType STONE = new WoodType("bubbas_gravestone", BlockSetType.STONE, SoundType.STONE, SoundType.STONE,
			SoundEvents.FENCE_GATE_CLOSE, SoundEvents.FENCE_GATE_OPEN);

	/** The stone facing north/south (rotation 0 or 8): 12 wide, 4 deep, 14 tall, on a 14 x 6 plinth. */
	private static final VoxelShape NORTH_SOUTH = Shapes.or(Block.box(2.0, 0.0, 6.0, 14.0, 14.0, 10.0), Block.box(1.0, 0.0, 5.0, 15.0, 2.0, 11.0));
	private static final VoxelShape EAST_WEST = Shapes.or(Block.box(6.0, 0.0, 2.0, 10.0, 14.0, 14.0), Block.box(5.0, 0.0, 1.0, 11.0, 2.0, 15.0));
	/** Turned at an angle: a centred square that covers the stone well enough to stand against. */
	private static final VoxelShape DIAGONAL = Block.box(3.5, 0.0, 3.5, 12.5, 14.0, 12.5);

	public GravestoneBlock(Properties properties) {
		super(STONE, properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GravestoneBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, ModBlockEntities.GRAVESTONE, SignBlockEntity::tick);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(state);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(state);
	}

	private static VoxelShape shape(BlockState state) {
		int rotation = state.getValue(ROTATION);
		if (rotation % 4 != 0) {
			return DIAGONAL;
		}
		return rotation % 8 == 0 ? NORTH_SOUTH : EAST_WEST;
	}

	@Override
	public boolean isPossibleToRespawnInThis(BlockState state) {
		return false;
	}
}
