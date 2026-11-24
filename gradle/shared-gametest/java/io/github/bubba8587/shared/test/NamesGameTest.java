// dte:B20
package io.github.bubba8587.shared.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

/** Shared core test: every id of ours that other mods display has an en_us name (specs/compat.md). */
public class NamesGameTest {
	/** Our mod namespaces loaded in this run. */
	private static Set<String> ourNamespaces() {
		Set<String> out = new TreeSet<>();
		for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
			String id = mod.getMetadata().getId();
			if (id.startsWith("bubbas_") && !id.endsWith("-test")) out.add(id);
		}
		return out;
	}

	/** Every translatable key in a component tree that has no en_us entry and no fallback. */
	private static void missingKeys(Component c, List<String> out) {
		if (c.getContents() instanceof TranslatableContents t) {
			if (t.getFallback() == null && !Language.getInstance().has(t.getKey())) out.add(t.getKey());
			for (Object arg : t.getArgs()) {
				if (arg instanceof Component a) missingKeys(a, out);
			}
		}
		for (Component s : c.getSiblings()) missingKeys(s, out);
	}

	private static <T> void checkRegistry(Registry<T> registry, Set<String> ns, Function<T, Component> name, List<String> problems) {
		for (Identifier id : registry.keySet()) {
			if (!ns.contains(id.getNamespace())) continue;
			List<String> keys = new ArrayList<>();
			missingKeys(name.apply(registry.getValue(id)), keys);
			for (String k : keys) problems.add(registry.key().identifier().getPath() + " " + id + ": " + k);
		}
	}

	private static void checkKey(String what, Identifier id, String key, List<String> problems) {
		if (!Language.getInstance().has(key)) problems.add(what + " " + id + ": " + key);
	}

	/** Items, blocks, entities, effects, creative tabs, enchantments, structures and item tags all have names. */
	@GameTest
	public void everyIdHasAName(GameTestHelper helper) {
		Set<String> ns = ourNamespaces();
		List<String> problems = new ArrayList<>();
		checkRegistry(BuiltInRegistries.ITEM, ns, item -> item.getDefaultInstance().getHoverName(), problems);
		checkRegistry(BuiltInRegistries.BLOCK, ns, block -> block.getName(), problems);
		checkRegistry(BuiltInRegistries.ENTITY_TYPE, ns, type -> type.getDescription(), problems);
		checkRegistry(BuiltInRegistries.MOB_EFFECT, ns, effect -> effect.getDisplayName(), problems);
		checkRegistry(BuiltInRegistries.CREATIVE_MODE_TAB, ns, tab -> tab.getDisplayName(), problems);
		var access = helper.getLevel().registryAccess();
		checkRegistry(access.lookupOrThrow(Registries.ENCHANTMENT), ns, e -> e.description(), problems);
		for (Identifier id : access.lookupOrThrow(Registries.STRUCTURE).keySet()) {
			if (ns.contains(id.getNamespace())) checkKey("structure", id, Util.makeDescriptionId("structure", id), problems);
		}
		BuiltInRegistries.ITEM.getTags().forEach(tag -> {
			Identifier id = tag.key().location();
			if (ns.contains(id.getNamespace())) checkKey("item tag", id, tag.key().getTranslationKey(), problems);
		});
		helper.assertTrue(problems.isEmpty(), Component.literal("missing en_us names (" + problems.size() + "):\n" + String.join("\n", problems)));
		helper.succeed();
	}
}
