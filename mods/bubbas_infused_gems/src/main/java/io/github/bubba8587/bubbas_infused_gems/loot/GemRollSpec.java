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

/**
 * How a loot gem is made (the loot functions' shared fields): one of {@code effects} (none listed = any craftable gem
 * effect), an Infusion level and a tier picked from their lists (a repeated value is a weight), the effect's weakest
 * potion variant ({@code "normal"}) or any of its variants ({@code "any"}), then rolled like the enchanting table does.
 * The numbers live in tools/gem-balance.py (LOOT), which writes the loot files.
 */
public record GemRollSpec(Optional<List<Holder<MobEffect>>> effects, List<Integer> infusion, List<Integer> tier, String variant) {
	public static final MapCodec<GemRollSpec> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			MobEffect.CODEC.listOf().optionalFieldOf("effects").forGetter(GemRollSpec::effects),
			Codec.intRange(1, 6).listOf(1, 64).fieldOf("infusion").forGetter(GemRollSpec::infusion),
			Codec.intRange(1, 3).listOf(1, 64).fieldOf("tier").forGetter(GemRollSpec::tier),
			Codec.STRING.optionalFieldOf("variant", "normal").forGetter(GemRollSpec::variant)
	).apply(i, GemRollSpec::new));

	/** Every gem effect that is brewed (not found-only): the default pool. */
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
		} else {
			pool.addAll(craftable());
		}
		if (pool.isEmpty()) {
			pool.addAll(craftable());
		}
		String key = pool.get(random.nextInt(pool.size()));
		Balance.EffectData e = b.effect(key);
		String variant = "normal";
		List<String> variants = List.copyOf(e.variants().keySet());
		if ("any".equals(this.variant) && !variants.isEmpty()) {
			variant = variants.get(random.nextInt(variants.size()));
		} else if (!variants.isEmpty() && !variants.contains("normal")) {
			variant = variants.getFirst();
		}
		int infusionLevel = this.infusion.get(random.nextInt(this.infusion.size()));
		int tierLevel = this.tier.get(random.nextInt(this.tier.size()));
		InfusedGem infused = new InfusedGem(GemText.holder(key), variant, GemForm.LINGERING, infusionLevel);
		return GemEnchanting.roll(random.nextInt(), tierLevel, infused);
	}
}
