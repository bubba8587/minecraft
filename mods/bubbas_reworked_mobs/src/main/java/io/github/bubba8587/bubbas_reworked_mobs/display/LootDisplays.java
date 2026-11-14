// dte:C1,B15
package io.github.bubba8587.bubbas_reworked_mobs.display;

import java.util.function.Predicate;

import io.github.bubba8587.bubbas_reworked_mobs.BubbasReworkedMobs;
import io.github.bubba8587.bubbas_reworked_mobs.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Putting items on display and taking them off: by players (empty hand; a shovel for buried loot) and by mobs
 * (any mob in {@code #grabs_loot} grabbing what is lying around, see {@code loot/LootGrabbing}). Taking always removes the block. Noise: an ordinary take is the quiet
 * {@code item_interact_finish} game event (vanilla: ignored while sneaking); digging is {@code block_destroy}, which
 * sleepers hear about three times as well (specs/undead.md "Hearing and waking").
 */
public final class LootDisplays {
	public static final String BURIED_HINT = "block." + BubbasReworkedMobs.MOD_ID + ".loot_display.buried_hint";

	private LootDisplays() {
	}

	/** Puts {@code stack} (one of it) on display at {@code pos}, replacing what is there. */
	public static LootDisplayBlockEntity place(Level level, BlockPos pos, DisplayPlacement placement, Direction facing, ItemStack stack) {
		BlockState state = ModBlocks.LOOT_DISPLAY.stateFor(level, pos, placement, facing);
		level.setBlock(pos, state, Block.UPDATE_ALL);
		if (!(level.getBlockEntity(pos) instanceof LootDisplayBlockEntity display)) {
			throw new IllegalStateException("no loot display block entity at " + pos);
		}
		display.setItem(stack);
		return display;
	}

	/** Empty-hand take: the item goes to the player, the block goes away, with a pickup sound and a quiet game event. */
	public static boolean take(ServerLevel level, BlockPos pos, Player player) {
		ItemStack stack = removeAndTake(level, pos);
		if (stack.isEmpty()) {
			return false;
		}
		player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
		level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3F, 0.9F + level.getRandom().nextFloat() * 0.2F);
		level.gameEvent(player, GameEvent.ITEM_INTERACT_FINISH, pos);
		return true;
	}

	/** Shovel on buried loot: dig it up. Louder than taking (the dig sound of the ground, and a block_destroy event). */
	public static boolean digUp(ServerLevel level, BlockPos pos, Player player, InteractionHand hand) {
		ItemStack stack = removeAndTake(level, pos);
		if (stack.isEmpty()) {
			return false;
		}
		player.getItemInHand(hand).hurtAndBreak(1, player, hand);
		player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
		BlockState ground = level.getBlockState(pos.below());
		level.playSound(null, pos, ground.isAir() ? SoundEvents.GRAVEL_BREAK : ground.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
		level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3F, 0.9F);
		level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
		return true;
	}

	/**
	 * The nearest loot display holding an item whose block centre is within {@code range} of {@code centre}. Walks the
	 * block entities of the (already loaded) chunks in range rather than every block position, so it costs the
	 * number of block entities nearby, not the volume. Never loads chunks.
	 */
	public static @Nullable BlockPos findNearest(ServerLevel level, Vec3 centre, double range, Predicate<LootDisplayBlockEntity> allowed) {
		double rangeSqr = range * range;
		@Nullable BlockPos best = null;
		double bestSqr = Double.MAX_VALUE;
		int minX = SectionPos.blockToSectionCoord(centre.x - range);
		int maxX = SectionPos.blockToSectionCoord(centre.x + range);
		int minZ = SectionPos.blockToSectionCoord(centre.z - range);
		int maxZ = SectionPos.blockToSectionCoord(centre.z + range);
		for (int cx = minX; cx <= maxX; cx++) {
			for (int cz = minZ; cz <= maxZ; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity entity : chunk.getBlockEntities().values()) {
					if (!(entity instanceof LootDisplayBlockEntity display) || display.isRemoved() || display.getItem().isEmpty()) {
						continue;
					}
					BlockPos pos = display.getBlockPos();
					double distSqr = centre.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
					if (distSqr <= rangeSqr && distSqr < bestSqr && allowed.test(display)) {
						best = pos;
						bestSqr = distSqr;
					}
				}
			}
		}
		return best;
	}

	/** Whether {@code pos} still holds a loot display with an item (cheap: one block entity lookup). */
	public static boolean hasItemAt(Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof LootDisplayBlockEntity display && !display.isRemoved() && !display.getItem().isEmpty();
	}

	/**
	 * A mob taking the item off the display at {@code pos} (any placement: undead don't need shovels). The block goes.
	 *
	 * @return the item, or empty if there was no display there
	 */
	public static ItemStack takeForMob(ServerLevel level, BlockPos pos) {
		ItemStack stack = removeAndTake(level, pos);
		if (!stack.isEmpty()) {
			level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.HOSTILE, 0.3F, 0.7F);
		}
		return stack;
	}

	/** Empties the display at {@code pos} and removes its block (leaving water if it was waterlogged). */
	private static ItemStack removeAndTake(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(ModBlocks.LOOT_DISPLAY) || !(level.getBlockEntity(pos) instanceof LootDisplayBlockEntity display)) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = display.takeItem();
		level.setBlock(pos, state.getValue(LootDisplayBlock.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(),
				Block.UPDATE_ALL);
		return stack;
	}
}
