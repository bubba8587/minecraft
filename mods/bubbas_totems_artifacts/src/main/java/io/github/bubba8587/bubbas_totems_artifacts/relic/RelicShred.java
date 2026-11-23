// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.relic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A relic map shred's data: which relic, how many pieces the whole map has (4 or 9), how many this stack holds. */
public record RelicShred(String name, int size, int count) {
	public static final Codec<RelicShred> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("name").forGetter(RelicShred::name),
			Codec.intRange(1, 9).fieldOf("size").forGetter(RelicShred::size),
			Codec.intRange(1, 9).fieldOf("count").forGetter(RelicShred::count)).apply(i, RelicShred::new));
	public static final StreamCodec<ByteBuf, RelicShred> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, RelicShred::name,
			ByteBufCodecs.VAR_INT, RelicShred::size,
			ByteBufCodecs.VAR_INT, RelicShred::count, RelicShred::new);

	public boolean complete() {
		return count >= size;
	}

	/** Same relic and same size. */
	public boolean sameMap(RelicShred other) {
		return name.equals(other.name) && size == other.size;
	}
}
