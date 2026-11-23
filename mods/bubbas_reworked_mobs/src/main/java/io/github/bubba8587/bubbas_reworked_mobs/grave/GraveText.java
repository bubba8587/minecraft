// dte:C1
package io.github.bubba8587.bubbas_reworked_mobs.grave;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignText;

/**
 * What worldgen carves on a gravestone (owner, 2026-11-22: never real names). Either nothing, or a few short words in
 * the enchanting table's alphabet (the {@code minecraft:alt} font: ordinary letters drawn as glyphs), centred on the
 * middle lines like an epitaph.
 */
public final class GraveText {
	public static final FontDescription.Resource ALT = new FontDescription.Resource(Identifier.withDefaultNamespace("alt"));
	private static final String LETTERS = "abcdefghijklmnopqrstuvwxyz";

	private GraveText() {
	}

	/** Blank with {@code blankChance}, else 1 to 3 lines of glyph words. */
	public static SignText random(RandomSource random, float blankChance) {
		Component[] lines = { Component.empty(), Component.empty(), Component.empty(), Component.empty() };
		if (random.nextFloat() >= blankChance) {
			int count = 1 + random.nextInt(3);
			int first = count == 3 ? 0 : 1;
			for (int i = 0; i < count; i++) {
				lines[first + i] = glyphs(random);
			}
		}
		List<Component> list = new ArrayList<>(List.of(lines));
		return new SignText(list, list, DyeColor.BLACK, false);
	}

	/** One line: one or two words of 2 to 6 glyphs. */
	public static Component glyphs(RandomSource random) {
		StringBuilder text = new StringBuilder();
		int words = 1 + random.nextInt(2);
		for (int w = 0; w < words; w++) {
			if (w > 0) {
				text.append(' ');
			}
			int length = 2 + random.nextInt(5);
			for (int i = 0; i < length; i++) {
				text.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
			}
		}
		return Component.literal(text.toString()).withStyle(style -> style.withFont(ALT));
	}

	/** Whether a carved line is only glyphs or nothing (no readable text): what the "no real names" rule checks. */
	public static boolean noRealNames(SignText text) {
		for (Component line : text.getMessages(false)) {
			if (!line.getString().isEmpty() && !ALT.equals(line.getStyle().getFont())) {
				return false;
			}
		}
		return true;
	}
}
