// dte:C4
package io.github.bubba8587.bubbas_infused_gems.gem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

/** The {@code socketed_gems} component on armor and weapons: the finished gems in it, in socketing order. */
public record SocketedGems(List<Gem> gems) implements TooltipProvider {
	public static final SocketedGems EMPTY = new SocketedGems(List.of());
	public static final Codec<SocketedGems> CODEC = Gem.CODEC.listOf(0, 3).xmap(SocketedGems::new, SocketedGems::gems);
	public static final StreamCodec<RegistryFriendlyByteBuf, SocketedGems> STREAM_CODEC =
			Gem.STREAM_CODEC.apply(ByteBufCodecs.list(3)).map(SocketedGems::new, SocketedGems::gems);

	public SocketedGems {
		gems = List.copyOf(gems);
	}

	public SocketedGems with(Gem gem) {
		List<Gem> list = new ArrayList<>(this.gems);
		list.add(gem);
		return new SocketedGems(list);
	}

	public int size() {
		return this.gems.size();
	}

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> out, TooltipFlag flag, DataComponentGetter components) {
		if (this.gems.isEmpty()) {
			return;
		}
		out.accept(Component.translatable(GemText.KEY + "tooltip.socketed", this.gems.size()).withStyle(ChatFormatting.GRAY));
		for (Gem gem : this.gems) {
			out.accept(Component.literal(" ").append(gem.summary()));
		}
	}
}
