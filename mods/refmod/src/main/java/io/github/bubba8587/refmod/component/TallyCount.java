// dte:A2,A4
package io.github.bubba8587.refmod.component;

import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

/** Item + data component: how many times a tally stick has been used. Immutable record (components must be). */
public record TallyCount(int uses) implements TooltipProvider {
	public static final TallyCount ZERO = new TallyCount(0);
	public static final Codec<TallyCount> CODEC = RecordCodecBuilder.create(i -> i.group(
			ExtraCodecs.NON_NEGATIVE_INT.fieldOf("uses").forGetter(TallyCount::uses)
	).apply(i, TallyCount::new));
	public static final StreamCodec<ByteBuf, TallyCount> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TallyCount::uses,
			TallyCount::new);

	public TallyCount increment() {
		return new TallyCount(uses + 1);
	}

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
		consumer.accept(Component.translatable("item.refmod.tally_stick.uses", uses).withStyle(ChatFormatting.GRAY));
	}
}
