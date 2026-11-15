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

/**
 * Item tint {@code bubbas_infused_gems:gem_color}: a gem's potion color, read from its own data, so every effect gets
 * its color with no art or stored color per effect. A finished gem takes its main effect's color only (owner: the
 * second-effect highlight was tried and dropped). The potion color is pushed toward full saturation and brightness so
 * gems read as jewels rather than flat potion swatches (owner).
 */
public record GemColorSource() implements ItemTintSource {
	public static final MapCodec<GemColorSource> MAP_CODEC = MapCodec.unit(GemColorSource::new);
	private static final int DEFAULT = 0xFFB0A0D0;
	/** How far saturation and brightness move toward 1 (0 = the plain potion color). */
	static final float SATURATE = 0.55F;
	static final float BRIGHTEN = 0.35F;

	@Override
	public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
		Gem gem = stack.get(ModComponents.GEM);
		if (gem != null) {
			return jewel(gem.effect().value().getColor());
		}
		InfusedGem infused = stack.get(ModComponents.INFUSED_GEM);
		return infused != null ? jewel(infused.effect().value().getColor()) : DEFAULT;
	}

	/** The potion color with more saturation and brightness, same hue. Grays (no hue) only get brighter. */
	static int jewel(int rgb) {
		float[] hsb = java.awt.Color.RGBtoHSB(ARGB.red(rgb), ARGB.green(rgb), ARGB.blue(rgb), null);
		float s = hsb[1] < 0.08F ? hsb[1] : hsb[1] + (1 - hsb[1]) * SATURATE;
		float b = hsb[2] + (1 - hsb[2]) * BRIGHTEN;
		return ARGB.opaque(java.awt.Color.HSBtoRGB(hsb[0], s, b));
	}

	@Override
	public MapCodec<GemColorSource> type() {
		return MAP_CODEC;
	}
}
