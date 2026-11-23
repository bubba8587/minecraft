// dte:C6
package io.github.bubba8587.bubbas_totems_artifacts.relic;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_totems_artifacts.BubbasTotemsArtifacts;
import net.minecraft.network.chat.Component;

/** Relic names: an adjective and a noun, e.g. wondrous_crab. */
public final class RelicNames {
	public static final List<String> ADJECTIVES = List.of("wondrous", "egregious", "forgotten", "gilded", "sunken", "whispering", "crooked",
			"radiant");
	public static final List<String> NOUNS = List.of("crab", "zealot", "lantern", "serpent", "crown", "owl", "anvil", "bell");
	/** The two map sizes: four shreds or nine. */
	public static final List<Integer> SIZES = List.of(4, 9);
	/** Every name, adjective-major. */
	public static final List<String> ALL = all();

	private RelicNames() {
	}

	private static List<String> all() {
		List<String> names = new ArrayList<>();
		for (String adjective : ADJECTIVES) {
			for (String noun : NOUNS) {
				names.add(adjective + "_" + noun);
			}
		}
		return List.copyOf(names);
	}

	public static boolean valid(String name) {
		return ALL.contains(name);
	}

	/** "Wondrous Crab", from the lang file's words. */
	public static Component display(String name) {
		int split = name.indexOf('_');
		if (split < 0) {
			return Component.literal(name);
		}
		String prefix = "relic." + BubbasTotemsArtifacts.MOD_ID + ".";
		return Component.translatable(prefix + "name", Component.translatable(prefix + "adjective." + name.substring(0, split)),
				Component.translatable(prefix + "noun." + name.substring(split + 1)));
	}
}
