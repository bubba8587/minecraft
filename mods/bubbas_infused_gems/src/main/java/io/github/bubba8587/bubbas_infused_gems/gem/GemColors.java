// dte:C4
package io.github.bubba8587.bubbas_infused_gems.gem;

import net.minecraft.util.ARGB;

/** Gem colors: the effect's potion color made a jewel. */
public final class GemColors {
	/** How far saturation and brightness move toward 1. */
	public static final float SATURATE = 0.55F;
	public static final float BRIGHTEN = 0.35F;
	/** Text never goes below this brightness. */
	public static final float TEXT_MIN_BRIGHTNESS = 0.8F;

	private GemColors() {
	}

	/** The potion color with more saturation and brightness, same hue. */
	public static int jewel(int rgb) {
		float[] hsb = java.awt.Color.RGBtoHSB(ARGB.red(rgb), ARGB.green(rgb), ARGB.blue(rgb), null);
		float s = hsb[1] < 0.08F ? hsb[1] : hsb[1] + (1 - hsb[1]) * SATURATE;
		float b = hsb[2] + (1 - hsb[2]) * BRIGHTEN;
		return ARGB.opaque(java.awt.Color.HSBtoRGB(hsb[0], s, b));
	}

	/** The jewel color for text: at least TEXT_MIN_BRIGHTNESS bright. */
	public static int text(int rgb) {
		int jewel = jewel(rgb);
		float[] hsb = java.awt.Color.RGBtoHSB(ARGB.red(jewel), ARGB.green(jewel), ARGB.blue(jewel), null);
		return java.awt.Color.HSBtoRGB(hsb[0], hsb[1], Math.max(hsb[2], TEXT_MIN_BRIGHTNESS)) & 0xFFFFFF;
	}
}
