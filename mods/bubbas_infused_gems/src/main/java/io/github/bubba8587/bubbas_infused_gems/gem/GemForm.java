// dte:C4
package io.github.bubba8587.bubbas_infused_gems.gem;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * The potion form an Infused Gem was brewed from: sets the highest enchant tier (normal 1, splash 2, lingering 3). An
 * Infused Gem brewed from an item (golden apple, wither rose...) is form {@code item}: no form in its name, tier 3 (owner,
 * 2026-11-15), unless the model caps that source.
 */
public enum GemForm implements StringRepresentable {
	NORMAL("normal"), SPLASH("splash"), LINGERING("lingering"), ITEM("item");

	public static final Codec<GemForm> CODEC = StringRepresentable.fromEnum(GemForm::values);
	public static final StreamCodec<ByteBuf, GemForm> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], GemForm::ordinal);

	private final String name;

	GemForm(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	public GemForm next() {
		return this == LINGERING || this == ITEM ? this : values()[this.ordinal() + 1];
	}
}
