// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.undead.sleeper;

import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadTuning;
import io.github.bubba8587.bubbas_reworked_mobs.undead.UndeadVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * How buried and frozen sleepers break out.
 * <ul>
 * <li><b>Husks</b> lie buried in sand: waking, the sand over and beside them bursts in a cloud that briefly blinds
 * anyone close.</li>
 * <li><b>Strays</b> lie frozen in ice or packed snow: when something wakes one, the ice first cracks (a sound and
 * shards: the warning), and it breaks free {@link UndeadTuning#STRAY_CRACK_TICKS} later, shattering the ice round it.</li>
 * </ul>
 */
public final class SleeperBreakout {
	private SleeperBreakout() {
	}

	/** Whether this block is what a husk is buried in. */
	public static boolean isSand(BlockState state) {
		return state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.SANDSTONE_SLAB) || state.is(Blocks.SUSPICIOUS_SAND);
	}

	/** Whether this block is what a stray is frozen in. */
	public static boolean isIce(BlockState state) {
		return state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE) || state.is(Blocks.SNOW_BLOCK)
				|| state.is(Blocks.POWDER_SNOW);
	}

	/** The warning, when a stray's wake is scheduled: the ice round it cracks. */
	public static void crack(ServerLevel level, BlockPos pos) {
		level.playSound(null, pos, SoundEvents.GLASS.hitSound().orElseThrow().value(), SoundSource.HOSTILE, 1.0F, 0.6F);
		level.playSound(null, pos, SoundEvents.POWDER_SNOW.breakSound().orElseThrow().value(), SoundSource.HOSTILE, 0.8F, 0.7F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), pos.getX() + 0.5, pos.getY() + 1.0,
				pos.getZ() + 0.5, 30, 0.6, 0.3, 0.6, 0.1);
	}

	/** Just before a buried or frozen sleeper becomes its mob: clears what it lies in, with the burst. */
	public static void breakOut(ServerLevel level, BlockPos pos, UndeadVariant variant) {
		if (variant == UndeadVariant.HUSK) {
			int cleared = clear(level, pos, SleeperBreakout::isSand);
			level.playSound(null, pos, SoundEvents.SAND.breakSound().orElseThrow().value(), SoundSource.HOSTILE, 1.2F, 0.7F);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), pos.getX() + 0.5, pos.getY() + 1.0,
					pos.getZ() + 0.5, 60 + 20 * cleared, 1.0, 0.8, 1.0, 0.15);
			level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 12, 0.8, 0.5, 0.8, 0.02);
			// The puff of sand blinds anyone close for a moment.
			for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(UndeadTuning.HUSK_BLIND_RADIUS),
					EntitySelector.NO_CREATIVE_OR_SPECTATOR)) {
				player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, UndeadTuning.HUSK_BLIND_TICKS, 0));
			}
		} else if (variant == UndeadVariant.STRAY) {
			int cleared = clear(level, pos, SleeperBreakout::isIce);
			level.playSound(null, pos, SoundEvents.GLASS.breakSound().orElseThrow().value(), SoundSource.HOSTILE, 1.0F, 0.8F);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), pos.getX() + 0.5, pos.getY() + 1.0,
					pos.getZ() + 0.5, 40 + 20 * cleared, 0.8, 0.6, 0.8, 0.2);
			level.sendParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 20, 0.8, 0.6, 0.8, 0.05);
		}
	}

	/**
	 * Removes the matching blocks round the sleeper: over it (two up), beside it, and beside the block over it (a lying
	 * one's body reaches into the next block). What it was buried or frozen in.
	 */
	private static int clear(ServerLevel level, BlockPos pos, java.util.function.Predicate<BlockState> what) {
		int cleared = 0;
		BlockPos up = pos.above();
		for (BlockPos at : new BlockPos[] { up, pos.above(2), pos.north(), pos.south(), pos.east(), pos.west(), up.north(), up.south(), up.east(),
				up.west() }) {
			if (what.test(level.getBlockState(at))) {
				level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				cleared++;
			}
		}
		return cleared;
	}
}
