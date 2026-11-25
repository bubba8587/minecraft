// dte:C4
package io.github.bubba8587.bubbas_infused_gems.balance;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** The balance model's numbers. */
public final class Balance {
	public static final String PATH = "/bubbas_infused_gems/balance.json";
	private static Balance instance;

	public final double cap;
	public final double stackR;
	public final int armorSlots;
	public final int weaponSlots;
	public final double armorNorm;
	public final double weaponNorm;
	public final double healBest;
	public final double healExp;
	public final double drawbackOne;
	public final double drawbackCap;
	public final int drawbackHitSeconds;
	/** Healing/Harming on a weapon: the least a strike does per hit. */
	public final double hitStrikeFloor;
	public final double weaponSplit;
	public final List<Infusion> infusions = new ArrayList<>();
	public final Map<String, Double> armorMaterials = new LinkedHashMap<>();
	public final Map<Integer, Double> run = new LinkedHashMap<>();
	public final Map<String, Double> weaponMaterials = new LinkedHashMap<>();
	public final Map<String, Double> grades = new LinkedHashMap<>();
	public final Map<String, Integer> formMaxTier = new LinkedHashMap<>();
	/** Bookshelves needed for each enchanting tier. */
	public final Map<Integer, Integer> tierMinShelves = new LinkedHashMap<>();
	public final Map<Integer, List<Outcome>> tiers = new LinkedHashMap<>();
	public final Map<String, EffectData> effects = new LinkedHashMap<>();
	/** Effects an Infused Gem can be brewed for now. */
	public final Set<String> brewable;
	/** Every gem effect, in the model's order. */
	public final List<String> gemEffects;
	/** Open-question defaults the owner may change. */
	public final boolean strongerApplies;
	/** Gem inversion: the reagent, and the weakest potion variant factor. */
	public final double variantMin;
	public final boolean undeadInversion;

	/** The raw file, for tests that compare against the model's own example builds. */
	public final JsonObject raw;

	public record Infusion(int level, String numeral, String base, String item, double factor) {
	}

	/** One enchant outcome: main grade, other grade, a drawback or not. */
	public record Outcome(String label, String main, String other, String drawback, double odds) {
	}

	public record Attr(String attribute, double amount, String operation, String curve) {
		/** The modifier amount at strength s. */
		public double at(double s) {
			if (this.curve.startsWith("step:")) {
				return s >= Double.parseDouble(this.curve.substring(5)) - 1e-9 ? this.amount : 0.0;
			}
			return switch (this.curve) {
				case "pow" -> Math.pow(this.amount, s) - 1.0;
				case "odds" -> s >= 1.0 - 1e-6 ? 1024.0 : this.amount * s / (1.0 - s);
				default -> this.amount * s;
			};
		}
	}

	public record EffectData(String key, String name, boolean good, String role, String hitKind, double levelOne,
			Map<String, Variant> variants, String source, int hitTicks, List<String> otherPool,
			int hitFloorTicks, double hitFloorHealHp, double hitFloorExhaustion, List<String> drawbackPool, List<Double> drawbackWeights, List<Double> otherWeights, List<Attr> wornAttributes, JsonObject worn, double hitUnit, List<String> opposites,
			int wornAmplifier, String id, Map<String, Double> parts, int color, double wornCap) {
		public boolean wornFlag(String name) {
			return this.worn != null && this.worn.has(name) && this.worn.get(name).getAsBoolean();
		}

		public double wornNumber(String name, double fallback) {
			return this.worn != null && this.worn.has(name) ? this.worn.get(name).getAsDouble() : fallback;
		}

		public double variantFactor(String variant) {
			Variant v = this.variants.get(variant);
			return v == null ? 1.0 : v.factor();
		}
	}

	public record Variant(String name, double factor, String potion, String item, int maxTier) {
	}

	/** A hit's dial set per build. */
	/** Gems in Reworked Beacons: the share of a full netherite set of the gem that a beacon gives. */
	public double beaconShare() {
		return this.raw.has("beacon_share") ? this.raw.get("beacon_share").getAsDouble() : 0.5;
	}

	public double hitCurve(String key, String which, double rel) {
		JsonObject e = this.raw.getAsJsonObject("effects").getAsJsonObject(key);
		if (e == null || !e.has("hit_curve") || !e.getAsJsonObject("hit_curve").has(which)) {
			return Double.NaN;
		}
		var pts = e.getAsJsonObject("hit_curve").getAsJsonArray(which);
		double r = Math.min(1.0, Math.max(0.0, rel));
		double r0 = pts.get(0).getAsJsonArray().get(0).getAsDouble();
		double v0 = pts.get(0).getAsJsonArray().get(1).getAsDouble();
		if (r <= r0) {
			return v0;
		}
		for (int i = 1; i < pts.size(); i++) {
			double r1 = pts.get(i).getAsJsonArray().get(0).getAsDouble();
			double v1 = pts.get(i).getAsJsonArray().get(1).getAsDouble();
			if (r <= r1) {
				return v0 + (v1 - v0) * (r - r0) / (r1 - r0);
			}
			r0 = r1;
			v0 = v1;
		}
		return v0;
	}

	private Balance(JsonObject o) {
		this.raw = o;
		this.cap = o.get("cap").getAsDouble();
		this.stackR = o.get("stack_r").getAsDouble();
		this.armorSlots = o.get("armor_slots").getAsInt();
		this.weaponSlots = o.get("weapon_slots").getAsInt();
		this.armorNorm = o.get("armor_norm").getAsDouble();
		this.weaponNorm = o.get("weapon_norm").getAsDouble();
		this.healBest = o.getAsJsonObject("heal").get("best").getAsDouble();
		this.healExp = o.getAsJsonObject("heal").get("exp").getAsDouble();
		this.drawbackOne = o.getAsJsonObject("drawback").get("one").getAsDouble();
		this.drawbackCap = o.getAsJsonObject("drawback").get("cap").getAsDouble();
		this.drawbackHitSeconds = o.getAsJsonObject("drawback").get("hit_seconds").getAsInt();
		this.hitStrikeFloor = o.get("hit_strike_floor").getAsDouble();
		this.weaponSplit = o.get("weapon_split").getAsDouble();
		for (JsonElement e : o.getAsJsonArray("infusion")) {
			JsonObject i = e.getAsJsonObject();
			this.infusions.add(new Infusion(i.get("level").getAsInt(), i.get("numeral").getAsString(), i.get("base").getAsString(),
					i.get("item").getAsString(), i.get("factor").getAsDouble()));
		}
		o.getAsJsonObject("armor_materials").entrySet().forEach(e -> this.armorMaterials.put(e.getKey(), e.getValue().getAsDouble()));
		o.getAsJsonObject("run").entrySet().forEach(e -> this.run.put(Integer.parseInt(e.getKey()), e.getValue().getAsDouble()));
		o.getAsJsonObject("weapon_materials").entrySet().forEach(e -> this.weaponMaterials.put(e.getKey(), e.getValue().getAsDouble()));
		o.getAsJsonObject("grades").entrySet().forEach(e -> this.grades.put(e.getKey(), e.getValue().getAsDouble()));
		o.getAsJsonObject("form_max_tier").entrySet().forEach(e -> this.formMaxTier.put(e.getKey(), e.getValue().getAsInt()));
		o.getAsJsonObject("tier_min_shelves").entrySet().forEach(e -> this.tierMinShelves.put(Integer.parseInt(e.getKey()), e.getValue().getAsInt()));
		o.getAsJsonObject("tiers").entrySet().forEach(e -> {
			List<Outcome> list = new ArrayList<>();
			for (JsonElement x : e.getValue().getAsJsonArray()) {
				JsonObject t = x.getAsJsonObject();
				list.add(new Outcome(t.get("label").getAsString(), str(t, "main"), str(t, "other"), str(t, "drawback"),
						t.get("odds").getAsDouble()));
			}
			this.tiers.put(Integer.parseInt(e.getKey()), List.copyOf(list));
		});
		o.getAsJsonObject("effects").entrySet().forEach(e -> {
			if (available(e.getValue().getAsJsonObject())) {
				this.effects.put(e.getKey(), effect(e.getKey(), e.getValue().getAsJsonObject()));
			}
		});
		this.brewable = Set.copyOf(strings(o.getAsJsonArray("brewable")).stream().filter(this.effects::containsKey).toList());
		this.gemEffects = this.effects.values().stream().filter(e -> !e.role().equals("negative")).map(EffectData::key).toList();
		JsonObject rules = o.getAsJsonObject("rules");
		this.strongerApplies = rules.get("gem_vs_potion").getAsString().equals("stronger");
		this.variantMin = o.getAsJsonObject("inversion").get("variant_min").getAsDouble();
		this.undeadInversion = rules.get("undead_inversion").getAsBoolean();

	}

	/** A gem effect from another of our mods (Soulguard: Reworked Mobs) exists only with that mod. */
	private static boolean available(JsonObject e) {
		return !e.has("requires_mod") || net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(e.get("requires_mod").getAsString());
	}

	private static EffectData effect(String key, JsonObject e) {
		Map<String, Variant> variants = new LinkedHashMap<>();
		if (e.has("variants")) {
			e.getAsJsonObject("variants").entrySet().forEach(v -> {
				JsonObject vo = v.getValue().getAsJsonObject();
				variants.put(v.getKey(), new Variant(v.getKey(), vo.get("factor").getAsDouble(), str(vo, "potion"), str(vo, "item"),
						vo.has("max_tier") ? vo.get("max_tier").getAsInt() : 0));
			});
		}
		JsonObject worn = e.has("worn") ? e.getAsJsonObject("worn") : null;
		List<Attr> attrs = new ArrayList<>();
		if (worn != null && worn.has("attributes")) {
			for (JsonElement a : worn.getAsJsonArray("attributes")) {
				JsonObject ao = a.getAsJsonObject();
				attrs.add(new Attr(ao.get("attribute").getAsString(), ao.get("amount").getAsDouble(), ao.get("operation").getAsString(),
						ao.get("curve").getAsString()));
			}
		}
		JsonObject hit = e.has("hit") ? e.getAsJsonObject("hit") : null;
		Map<String, Double> parts = new LinkedHashMap<>();
		if (e.has("parts")) {
			e.getAsJsonObject("parts").entrySet().forEach(x -> parts.put(x.getKey(), x.getValue().getAsDouble()));
		}
		return new EffectData(key, e.get("name").getAsString(), e.get("good").getAsBoolean(), e.get("role").getAsString(),
				e.get("hit_kind").getAsString(), e.get("level_one").getAsDouble(), Map.copyOf(variants), str(e, "source"),
				hit == null ? 0 : hit.get("ticks").getAsInt(),
				list(e, "other_pool"), hit == null ? 0 : hit.get("floor_ticks").getAsInt(),
				hit == null ? 0.0 : hit.get("floor_heal_hp").getAsDouble(), hit == null ? 0.0 : hit.get("floor_exhaustion").getAsDouble(),
				list(e, "drawback_pool"), doubles(e, "drawback_weights"), doubles(e, "other_weights"), List.copyOf(attrs), worn,
				e.has("hit_unit") ? e.get("hit_unit").getAsDouble() : 1.0, list(e, "opposites"),
				e.has("worn_amplifier") ? e.get("worn_amplifier").getAsInt() : 0,
				e.has("id") ? e.get("id").getAsString() : "minecraft:" + key, Map.copyOf(parts), e.has("color") ? e.get("color").getAsInt() : 0,
				e.has("worn_cap") ? e.get("worn_cap").getAsDouble() : 0.0);
	}

	private static String str(JsonObject o, String name) {
		return o.has(name) && !o.get(name).isJsonNull() ? o.get(name).getAsString() : null;
	}

	private static List<Double> doubles(JsonObject o, String name) {
		List<Double> out = new ArrayList<>();
		if (o.has(name)) {
			o.getAsJsonArray(name).forEach(x -> out.add(x.getAsDouble()));
		}
		return List.copyOf(out);
	}

	private static List<String> list(JsonObject o, String name) {
		return o.has(name) ? strings(o.getAsJsonArray(name)) : List.of();
	}

	private static List<String> strings(JsonArray a) {
		List<String> out = new ArrayList<>();
		a.forEach(x -> out.add(x.getAsString()));
		return List.copyOf(out);
	}

	public static Balance get() {
		if (instance == null) {
			try (InputStream in = Balance.class.getResourceAsStream(PATH)) {
				if (in == null) {
					throw new IllegalStateException("missing " + PATH + " (run python tools/gem-balance.py)");
				}
				instance = new Balance(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
			} catch (java.io.IOException ex) {
				throw new IllegalStateException("can't read " + PATH, ex);
			}
		}
		return instance;
	}

	public EffectData effect(String key) {
		return this.effects.get(key);
	}

	public Infusion infusion(int level) {
		return this.infusions.get(Math.clamp(level, 1, this.infusions.size()) - 1);
	}

	/** Total of one effect from every contribution. */
	public double stack(List<Double> contributions, double norm) {
		List<Double> c = new ArrayList<>(contributions);
		c.sort((a, b) -> Double.compare(b, a));
		double sum = 0.0;
		double w = 1.0;
		for (double x : c) {
			sum += x * w;
			w *= this.stackR;
		}
		return this.cap * sum / norm;
	}

	/** n drawbacks of one effect. */
	/** Drawbacks of one effect of these sizes: they saturate toward drawback.cap. */
	public double drawbackSum(List<Double> sizes) {
		double keep = 1.0;
		for (double x : sizes) {
			keep *= 1.0 - x / this.drawbackCap;
		}
		return this.drawbackCap * (1.0 - keep);
	}

	/** The fixed size of a drawback roll of this grade. */
	public double drawbackSize(String grade) {
		return this.drawbackOne * this.grades.get(grade) / this.grades.get("weak");
	}

	public double drawbackTotal(int n) {
		return n <= 0 ? 0.0 : this.drawbackCap * (1.0 - Math.pow(1.0 - this.drawbackOne / this.drawbackCap, n));
	}

	/** Healing gem: fraction of each survived hit returned at total s. */
	public double healFraction(double s) {
		return this.healBest * Math.pow(Math.max(s, 0.0) / this.cap, this.healExp);
	}
}
