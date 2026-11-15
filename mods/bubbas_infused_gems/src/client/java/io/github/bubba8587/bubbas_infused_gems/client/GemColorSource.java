// dte:C4
package io.github.bubba8587.bubbas_infused_gems.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.gem.InfusedGem;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Item tint {@code bubbas_infused_gems:gem_color}: a gem's potion color, read from its own data, so every effect gets
 * its color with no art or stored color per effect. {@code layer: main} is the gem's effect; {@code second} (the
 * highlight experiment) the other effect, else the drawback, else the main color again.
 */
public record GemColorSource(String layer) implements ItemTintSource {
	public static final MapCodec<GemColorSource> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.optionalFieldOf("layer", "main").forGetter(GemColorSource::layer)
	).apply(i, GemColorSource::new));
	private static final int DEFAULT = 0xFFB0A0D0;

	@Override
	public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
		Gem gem = stack.get(ModComponents.GEM);
		if (gem != null) {
			int main = gem.effect().value().getColor();
			if (this.layer.equals("second")) {
				return ARGB.opaque(gem.other().map(r -> r.effect().value().getColor())
						.orElseGet(() -> gem.drawback().map(r -> r.effect().value().getColor()).orElse(main)));
			}
			return ARGB.opaque(main);
		}
		InfusedGem infused = stack.get(ModComponents.INFUSED_GEM);
		return infused != null ? ARGB.opaque(infused.effect().value().getColor()) : DEFAULT;
	}

	@Override
	public MapCodec<GemColorSource> type() {
		return MAP_CODEC;
	}
}
