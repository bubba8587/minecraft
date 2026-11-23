// dte:C4
package io.github.bubba8587.bubbas_infused_gems.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.gem.Gem;
import io.github.bubba8587.bubbas_infused_gems.gem.GemForm;
import io.github.bubba8587.bubbas_infused_gems.gem.GemText;
import io.github.bubba8587.bubbas_infused_gems.gem.InfusedGem;
import io.github.bubba8587.bubbas_infused_gems.station.GemEnchanting;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;

/** How a loot gem is made. */
public record GemRollSpec(Optional<List<Holder<MobEffect>>> effects, List<Integer> infusion, List<Integer> tier, String variant,
		float preferHarmful, Optional<Perfect> perfect) {
	public record Perfect(int infusion, float cleanChance) {
		public static final Codec<Perfect> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.intRange(1, 6).fieldOf("infusion").forGetter(Perfect::infusion),
				Codec.floatRange(0.0F, 1.0F).fieldOf("clean_chance").forGetter(Perfect::cleanChance)
		).apply(i, Perfect::new));
	}

	public static final MapCodec<GemRollSpec> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			MobEffect.CODEC.listOf().optionalFieldOf("effects").forGetter(GemRollSpec::effects),
			Codec.intRange(1, 6).listOf(1, 64).fieldOf("infusion").forGetter(GemRollSpec::infusion),
			Codec.intRange(1, 3).listOf(1, 64).fieldOf("tier").forGetter(GemRollSpec::tier),
			Codec.STRING.optionalFieldOf("variant", "normal").forGetter(GemRollSpec::variant),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("prefer_harmful", 0.0F).forGetter(GemRollSpec::preferHarmful),
			Perfect.CODEC.optionalFieldOf("perfect").forGetter(GemRollSpec::perfect)
	).apply(i, GemRollSpec::new));

	/** Every gem effect that is brewed: the default pool. */
	static List<String> craftable() {
		Balance b = Balance.get();
		return b.gemEffects.stream().filter(k -> !"found".equals(b.effect(k).source())).toList();
	}

	/** A finished gem, rolled with this random source. */
	public Gem roll(RandomSource random) {
		Balance b = Balance.get();
		List<String> pool = new ArrayList<>();
		if (this.effects.isPresent()) {
			for (Holder<MobEffect> h : this.effects.get()) {
				if (b.effect(GemText.key(h)) != null) {
					pool.add(GemText.key(h));
				}
			}
		}
		if (pool.isEmpty()) {
			pool.addAll(craftable());
		}
		if (this.preferHarmful > 0.0F && random.nextFloat() < this.preferHarmful) {
			List<String> harmful = pool.stream().filter(k -> !b.effect(k).good()).toList();
			if (!harmful.isEmpty()) {
				pool = harmful;
			}
		}
		String key = pool.get(random.nextInt(pool.size()));
		Balance.EffectData e = b.effect(key);
		int infusionLevel = this.infusion.get(random.nextInt(this.infusion.size()));
		if (this.perfect.isPresent() && infusionLevel == this.perfect.get().infusion()) {
			return perfect(random, key, e, infusionLevel, this.perfect.get().cleanChance());
		}
		String variant = "normal";
		List<String> variants = List.copyOf(e.variants().keySet());
		if ("any".equals(this.variant) && !variants.isEmpty()) {
			variant = variants.get(random.nextInt(variants.size()));
		} else if (!variants.isEmpty() && !variants.contains("normal")) {
			variant = variants.getFirst();
		}
		int tierLevel = this.tier.get(random.nextInt(this.tier.size()));
		InfusedGem infused = new InfusedGem(GemText.holder(key), variant, GemForm.LINGERING, infusionLevel);
		return GemEnchanting.roll(random.nextInt(), tierLevel, infused);
	}

	/** A perfect gem: the strongest variant at this Infusion. */
	private static Gem perfect(RandomSource random, String key, Balance.EffectData e, int infusionLevel, float cleanChance) {
		Balance b = Balance.get();
		String top = e.variants().values().stream().max((x, y) -> Double.compare(x.factor(), y.factor())).map(Balance.Variant::name).orElse("normal");
		List<Balance.Outcome> outcomes = b.tiers.get(3);
		String grade = outcomes.getFirst().main();
		float main = (float) Math.min(1.0, new InfusedGem(GemText.holder(key), top, GemForm.LINGERING, infusionLevel).baseQuality() * b.grades.get(grade));
		Optional<Gem.Roll> drawback = Optional.empty();
		if (random.nextFloat() >= cleanChance && !e.drawbackPool().isEmpty()) {
			// The drawback's size follows tier 3's own drawback outcomes
			List<Balance.Outcome> withDrawback = outcomes.stream().filter(o -> o.drawback() != null && grade.equals(o.main())).toList();
			double total = withDrawback.stream().mapToDouble(Balance.Outcome::odds).sum();
			double r = random.nextDouble() * total;
			String size = withDrawback.getLast().drawback();
			for (Balance.Outcome o : withDrawback) {
				r -= o.odds();
				if (r < 0) {
					size = o.drawback();
					break;
				}
			}
			String effect = GemEnchanting.weighted(random, e.drawbackPool(), e.drawbackWeights());
			drawback = Optional.of(new Gem.Roll(GemText.holder(effect), (float) b.drawbackSize(size)));
		}
		return new Gem(GemText.holder(key), infusionLevel, 3, main, Optional.empty(), drawback);
	}
}
