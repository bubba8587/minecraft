// dte:A2,A4
package io.github.bubba8587.refmod.blockdata;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.bubba8587.refmod.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Extra data on an existing vanilla block without a new block or block entity (spec requirement 17b, B15 fewNewBlocks):
 * the tally counters of every vanilla barrel in one chunk, keyed by absolute position, stored as a chunk attachment
 * ({@link ModAttachments#BARREL_TALLIES}). Immutable: every change replaces the whole value, which marks the chunk
 * unsaved and resyncs it (attachment values must never be mutated in place).
 */
public record BarrelTallies(Map<BlockPos, Integer> counts) {
	public static final BarrelTallies EMPTY = new BarrelTallies(Map.of());

	private record Entry(BlockPos pos, int count) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
				Codec.INT.fieldOf("count").forGetter(Entry::count)).apply(i, Entry::new));
	}

	/** Saved as a list of {pos, count}: map keys in a Codec must be strings, and a BlockPos is not. */
	public static final Codec<BarrelTallies> CODEC = Entry.CODEC.listOf().xmap(
			list -> new BarrelTallies(list.stream().collect(Collectors.toUnmodifiableMap(Entry::pos, Entry::count))),
			tallies -> tallies.counts.entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).toList());

	public static final StreamCodec<RegistryFriendlyByteBuf, BarrelTallies> STREAM_CODEC = ByteBufCodecs
			.<RegistryFriendlyByteBuf, BlockPos, Integer, Map<BlockPos, Integer>>map(HashMap::new, BlockPos.STREAM_CODEC, ByteBufCodecs.VAR_INT)
			.map(BarrelTallies::new, BarrelTallies::counts);

	public BarrelTallies {
		counts = Map.copyOf(counts);
	}

	public int get(BlockPos pos) {
		return counts.getOrDefault(pos, 0);
	}

	public BarrelTallies with(BlockPos pos, int count) {
		Map<BlockPos, Integer> next = new HashMap<>(counts);
		if (count <= 0) {
			next.remove(pos);
		} else {
			next.put(pos.immutable(), count);
		}
		return new BarrelTallies(next);
	}

	// --- Level-level helpers: find the chunk that owns the position -------------------------------

	/** The tally at {@code pos}, or 0. Validates on read: a stale entry (the barrel is gone) reads as 0. */
	public static int get(Level level, BlockPos pos) {
		if (!level.getBlockState(pos).is(Blocks.BARREL)) {
			return 0;
		}
		BarrelTallies tallies = level.getChunkAt(pos).getAttached(ModAttachments.BARREL_TALLIES);
		return tallies == null ? 0 : tallies.get(pos);
	}

	public static int increment(Level level, BlockPos pos) {
		LevelChunk chunk = level.getChunkAt(pos);
		int next = get(level, pos) + 1;
		chunk.setAttached(ModAttachments.BARREL_TALLIES, chunk.getAttachedOrElse(ModAttachments.BARREL_TALLIES, EMPTY).with(pos, next));
		return next;
	}

	/** Called when the barrel at {@code pos} is removed; drops the entry and the whole attachment once the chunk has none. */
	public static void clear(Level level, BlockPos pos) {
		LevelChunk chunk = level.getChunkAt(pos);
		BarrelTallies tallies = chunk.getAttached(ModAttachments.BARREL_TALLIES);
		if (tallies == null || !tallies.counts.containsKey(pos)) {
			return;
		}
		BarrelTallies next = tallies.with(pos, 0);
		if (next.counts.isEmpty()) {
			chunk.removeAttached(ModAttachments.BARREL_TALLIES);
		} else {
			chunk.setAttached(ModAttachments.BARREL_TALLIES, next);
		}
	}
}
