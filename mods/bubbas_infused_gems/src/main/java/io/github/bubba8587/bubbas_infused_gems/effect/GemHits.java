// dte:C4
package io.github.bubba8587.bubbas_infused_gems.effect;

import java.util.ArrayList;
import java.util.List;

import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.gem.GemText;
import io.github.bubba8587.bubbas_infused_gems.registry.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Weapon gems on what they hit (melee) or on the arrow a bow or crossbow fires. One hit applies, per effect, the
 * model's per-hit form: potency s^0.5 and duration s^0.5 of the effect's hit anchor (scaled effects), or a charged
 * strike every 10 s (Healing, Harming). Drawbacks land on the target for 3 s, refreshed by each hit.
 */
public final class GemHits {
	/** One thing a hit applies: an effect at weapon strength s (or a drawback of size s). */
	public record Entry(String key, double s, boolean drawback) {
	}

	private GemHits() {
	}

	public static boolean isRanged(ItemStack weapon) {
		return weapon.is(Items.BOW) || weapon.is(Items.CROSSBOW);
	}

	/** What one hit of this weapon applies, from its socketed gems. */
	public static List<Entry> plan(ItemStack weapon) {
		GemTotals t = GemTotals.ofWeapon(weapon);
		List<Entry> out = new ArrayList<>();
		Balance b = Balance.get();
		t.totals().forEach((k, s) -> {
			if (b.v0Hit.contains(k)) {
				out.add(new Entry(k, s, false));
			}
		});
		t.drawbacks().forEach((k, s) -> {
			if (b.v0Hit.contains(k)) {
				out.add(new Entry(k, s, true));
			}
		});
		return out;
	}

	/** Potency (fraction of the hit anchor) and duration in ticks of one hit. */
	public static double potency(Entry e) {
		return e.drawback() ? e.s() : Math.pow(e.s(), Balance.get().weaponSplit);
	}

	public static int duration(Entry e) {
		Balance b = Balance.get();
		if (e.drawback()) {
			return b.drawbackHitSeconds * 20;
		}
		Balance.EffectData d = b.effect(e.key());
		return Math.max(1, (int) Math.round(Math.pow(e.s(), 1.0 - b.weaponSplit) * d.hitTicks()));
	}

	/** Applies a hit's entries to the target; charged strikes use and reset the attacker's charge. */
	public static void apply(ServerLevel level, LivingEntity target, Entity attacker, Entity direct, List<Entry> entries, boolean charged) {
		Balance b = Balance.get();
		for (Entry e : entries) {
			Balance.EffectData d = b.effect(e.key());
			if (!e.drawback() && d.hitKind().equals("instant")) {
				if (!charged) {
					continue;
				}
				boolean harm = e.key().equals("instant_damage") != target.isInvertedHealAndHarm();
				float amount = (float) (d.hitUnit() * e.s());
				if (harm) {
					target.hurtServer(level, target.damageSources().indirectMagic(direct, attacker), e.key().equals("instant_damage") ? amount
							: (float) (b.effect("instant_damage").hitUnit() * e.s()));
				} else {
					target.heal(e.key().equals("instant_health") ? amount : (float) (b.effect("instant_health").hitUnit() * e.s()));
				}
				continue;
			}
			Holder<MobEffect> effect = ModEffects.gem(e.key());
			if (effect != null) {
				target.addEffect(new MobEffectInstance(effect, duration(e), GemMobEffect.amplifier(potency(e))), attacker);
			}
		}
	}

	/** Whether the attacker's Healing/Harming strike is charged now; if so (and it has one), starts the next charge. */
	public static boolean useCharge(LivingEntity attacker, List<Entry> entries) {
		boolean hasStrike = entries.stream().anyMatch(e -> !e.drawback() && Balance.get().effect(e.key()).hitKind().equals("instant"));
		if (!hasStrike) {
			return false;
		}
		GemEntity g = (GemEntity) attacker;
		long now = attacker.level().getGameTime();
		if (now < g.bubbas$strikeReadyAt()) {
			return false;
		}
		g.bubbas$setStrikeReadyAt(now + Balance.get().harmChargeSeconds * 20L);
		return true;
	}

	/** Melee: called after a successful hit (EnchantmentHelper.doPostAttackEffects). */
	public static void onMeleeHit(ServerLevel level, LivingEntity attacker, LivingEntity target, ItemStack weapon) {
		if (weapon.isEmpty() || isRanged(weapon)) {
			return;
		}
		List<Entry> entries = plan(weapon);
		if (!entries.isEmpty()) {
			apply(level, target, attacker, attacker, entries, useCharge(attacker, entries));
		}
	}

	/**
	 * Where a real tipped arrow carries an effect a gem also gives, the stronger one applies (owner): a vanilla level
	 * as a fraction of the strongest tipped arrow, against the gem's potency.
	 */
	public static double vanillaFraction(String key, int amplifier) {
		Balance.EffectData d = Balance.get().effect(key);
		if (d == null) {
			return 1.0;
		}
		if (d.hitKind().equals("instant")) {
			return Math.min(1.0, (1 << amplifier) / (double) (1 << Math.max(d.hitAmplifier(), 0)));
		}
		if (amplifier >= d.hitAmplifier()) {
			return 1.0;
		}
		return d.levelOne() + (1.0 - d.levelOne()) * amplifier / Math.max(d.hitAmplifier(), 1);
	}

	public static String vanillaKey(Holder<MobEffect> effect) {
		return GemText.key(effect);
	}
}
