// dte:C4
package io.github.bubba8587.bubbas_infused_gems.client;

import com.mojang.serialization.MapCodec;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.gem.InfusedGem;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Item tint bubbas_infused_gems:gem_color. */
public record GemColorSource() implements ItemTintSource {
	public static final MapCodec<GemColorSource> MAP_CODEC = MapCodec.unit(GemColorSource::new);
	private static final int DEFAULT = 0xFFB0A0D0;
	@Override
	public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
		Gem gem = stack.get(ModComponents.GEM);
		if (gem != null) {
			return jewel(gem.effect().value().getColor());
		}
		InfusedGem infused = stack.get(ModComponents.INFUSED_GEM);
		return infused != null ? jewel(infused.effect().value().getColor()) : DEFAULT;
	}

	static int jewel(int rgb) {
		return io.github.bubba8587.bubbas_infused_gems.gem.GemColors.jewel(rgb);
	}

	@Override
	public MapCodec<GemColorSource> type() {
		return MAP_CODEC;
	}
}
