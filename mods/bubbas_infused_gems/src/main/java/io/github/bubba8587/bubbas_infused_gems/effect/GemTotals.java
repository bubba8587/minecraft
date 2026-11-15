// dte:C4
package io.github.bubba8587.bubbas_infused_gems.effect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.gem.SocketedGems;
import io.github.bubba8587.bubbas_infused_gems.registry.ModComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Combined gem strengths of one set of gear (the balance model, specs/effect-gems.md): per effect, the stacked total
 * of every main and "other" roll (gem quality x gear factor, strongest first), and per drawback effect its fixed size
 * by count. Computed when equipment changes, never per tick.
 */
public record GemTotals(Map<String, Double> totals, Map<String, Integer> drawbackCounts, Map<String, Double> drawbacks) {
	public static final GemTotals NONE = new GemTotals(Map.of(), Map.of(), Map.of());
	public static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	public boolean isEmpty() {
		return this.totals.isEmpty() && this.drawbacks.isEmpty();
	}

	/** Main/other total plus drawbacks of the same effect: what the effect does. */
	public double value(String key) {
		// No clamp at 1.0: mobility effects have raised worn caps (owner).
		return this.totals.getOrDefault(key, 0.0) + this.drawbacks.getOrDefault(key, 0.0);
	}

	public boolean has(String key) {
		return this.totals.containsKey(key) || this.drawbacks.containsKey(key);
	}

	/** Every effect key with a value, sorted. */
	public List<String> keys() {
		TreeMap<String, Boolean> all = new TreeMap<>();
		this.totals.keySet().forEach(k -> all.put(k, true));
		this.drawbacks.keySet().forEach(k -> all.put(k, true));
		return new ArrayList<>(all.keySet());
	}

	// ------------------------------------------------------------------ materials

	/** Armor material key of a piece ("netherite"...), or null when it isn't known armor. */
	public static String armorMaterial(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		for (String m : Balance.get().armorMaterials.keySet()) {
			String prefix = m.equals("gold") ? "golden" : m;
			if (path.startsWith(prefix + "_")) {
				return m;
			}
		}
		return null;
	}

	/** Weapon material key ("wood"... "netherite", or "bow"/"crossbow"/"trident"/"mace"), or null. */
	public static String weaponMaterial(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		Map<String, Double> w = Balance.get().weaponMaterials;
		if (w.containsKey(path)) {
			return path;
		}
		for (String m : w.keySet()) {
			String prefix = switch (m) {
				case "wood" -> "wooden";
				case "gold" -> "golden";
				default -> m;
			};
			if (path.startsWith(prefix + "_")) {
				return m;
			}
		}
		return null;
	}

	/** Weapon factor; an unknown (modded) weapon counts as iron. */
	public static double weaponFactor(ItemStack stack) {
		String m = weaponMaterial(stack);
		return Balance.get().weaponMaterials.getOrDefault(m == null ? "iron" : m, 0.87);
	}

	/**
	 * Gear factor of each armor piece (helmet, chestplate, leggings, boots): material x matching run, where a run is the
	 * unbroken stretch of the same material the piece sits in. An unknown or missing piece breaks a run; an unknown piece
	 * with gems counts as iron in a run of 1.
	 */
	public static double[] pieceFactors(String[] layout) {
		Balance b = Balance.get();
		double[] out = new double[layout.length];
		int i = 0;
		while (i < layout.length) {
			int j = i;
			while (layout[i] != null && j + 1 < layout.length && layout[i].equals(layout[j + 1])) {
				j++;
			}
			for (int k = i; k <= j; k++) {
				out[k] = layout[k] == null ? b.armorMaterials.get("iron") * b.run.get(1)
						: b.armorMaterials.get(layout[k]) * b.run.get(j - i + 1);
			}
			i = j + 1;
		}
		return out;
	}

	// ------------------------------------------------------------------ totals

	public static GemTotals ofArmor(LivingEntity entity) {
		ItemStack[] pieces = new ItemStack[ARMOR.length];
		for (int i = 0; i < ARMOR.length; i++) {
			pieces[i] = entity.getItemBySlot(ARMOR[i]);
		}
		return ofArmor(pieces);
	}

	public static GemTotals ofArmor(ItemStack[] pieces) {
		String[] layout = new String[pieces.length];
		for (int i = 0; i < pieces.length; i++) {
			layout[i] = armorMaterial(pieces[i]);
		}
		double[] factors = pieceFactors(layout);
		Map<String, List<Double>> contribs = new HashMap<>();
		Map<String, List<Double>> drawbacks = new HashMap<>();
		boolean any = false;
		for (int i = 0; i < pieces.length; i++) {
			SocketedGems gems = pieces[i].get(ModComponents.SOCKETED_GEMS);
			if (gems != null) {
				for (Gem gem : gems.gems()) {
					add(gem, factors[i], contribs, drawbacks);
					any = true;
				}
			}
		}
		return any ? finish(contribs, drawbacks, Balance.get().armorNorm, true) : NONE;
	}

	public static GemTotals ofWeapon(ItemStack weapon) {
		SocketedGems gems = weapon.get(ModComponents.SOCKETED_GEMS);
		if (gems == null || gems.gems().isEmpty()) {
			return NONE;
		}
		double factor = weaponFactor(weapon);
		Map<String, List<Double>> contribs = new HashMap<>();
		Map<String, List<Double>> drawbacks = new HashMap<>();
		for (Gem gem : gems.gems()) {
			add(gem, factor, contribs, drawbacks);
		}
		return finish(contribs, drawbacks, Balance.get().weaponNorm, false);
	}

	private static void add(Gem gem, double factor, Map<String, List<Double>> contribs, Map<String, List<Double>> drawbacks) {
		if (gem.hasMain()) {
			contribs.computeIfAbsent(gem.key(), k -> new ArrayList<>()).add(gem.main() * factor);
		}
		gem.other().ifPresent(r -> contribs.computeIfAbsent(r.key(), k -> new ArrayList<>()).add(r.strength() * factor));
		gem.drawback().ifPresent(r -> drawbacks.computeIfAbsent(r.key(), k -> new ArrayList<>()).add((double) r.strength()));
	}

	private static GemTotals finish(Map<String, List<Double>> contribs, Map<String, List<Double>> drawbackSizes, double norm, boolean worn) {
		Balance b = Balance.get();
		Map<String, Double> totals = new LinkedHashMap<>();
		contribs.forEach((k, list) -> {
			// Worn: scaled to the effect's own cap (owner: mobility goes past the strongest potion; combat stays at the cap).
			Balance.EffectData e = b.effect(k);
			double scale = worn && e != null && e.wornCap() > 0.0 ? e.wornCap() / b.cap : 1.0;
			double v = b.stack(list, norm) * scale;
			// The perfect set lands on its cap exactly, so "full" effects (infinite air, fire immunity, real invisibility,
			// a 100 % chance) never miss it by rounding.
			double top = worn && e != null && e.wornCap() > 0.0 ? e.wornCap() : b.cap;
			totals.put(k, Math.abs(v - top) < 1e-6 ? top : v);
		});
		Map<String, Double> drawbacks = new LinkedHashMap<>();
		Map<String, Integer> drawbackCounts = new LinkedHashMap<>();
		drawbackSizes.forEach((k, sizes) -> {
			drawbacks.put(k, b.drawbackSum(sizes));
			drawbackCounts.put(k, sizes.size());
		});
		return new GemTotals(Map.copyOf(totals), Map.copyOf(drawbackCounts), Map.copyOf(drawbacks));
	}
}
