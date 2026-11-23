// dte:C4
package io.github.bubba8587.bubbas_infused_gems.effect;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import org.jspecify.annotations.Nullable;

/** Weapon gems on what they hit or on the arrow a bow or crossbow fires. */
public final class GemHits {
	/** One thing a hit applies: an effect at weapon strength s. */
	public record Entry(String key, double s, boolean drawback) {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("effect").forGetter(Entry::key),
				Codec.DOUBLE.fieldOf("s").forGetter(Entry::s),
				Codec.BOOL.optionalFieldOf("drawback", false).forGetter(Entry::drawback)
		).apply(i, Entry::new));
	}

	private GemHits() {
	}

	/** Potency hits linger at least 4 s. */
	public static final int POTENCY_FLOOR_TICKS = 80;

	public static boolean isRanged(ItemStack weapon) {
		return weapon.is(Items.BOW) || weapon.is(Items.CROSSBOW);
	}

	/** What one hit of this weapon applies, from its socketed gems. */
	public static List<Entry> plan(ItemStack weapon) {
		GemTotals t = GemTotals.ofWeapon(weapon);
		List<Entry> out = new ArrayList<>();
		t.totals().forEach((k, s) -> out.add(new Entry(k, s, false)));
		t.drawbacks().forEach((k, s) -> out.add(new Entry(k, s, true)));
		out.sort((a, b) -> a.key().compareTo(b.key()));
		return out;
	}

	private static String kind(Entry e) {
		return e.drawback() ? "scaled" : Balance.get().effect(e.key()).hitKind();
	}

	/** Potency of one hit. */
	public static double potency(Entry e) {
		return switch (kind(e)) {
			case "onoff" -> 1.0;
			// Levitation and Breeze
			case "control" -> Math.min(1.0, e.s() / Balance.get().cap);
			case "instant" -> e.s();
			default -> {
				if (e.drawback()) {
					yield e.s();
				}
				double p = Math.pow(Math.min(1.0, e.s() / Balance.get().cap), Balance.get().weaponSplit);
				Balance.EffectData d = Balance.get().effect(e.key());
				yield d.good() && d.wornCap() > 0.0 ? Math.min(p, d.wornCap()) : p;
			}
		};
	}

	/** Duration in ticks of one hit. */
	public static int duration(Entry e) {
		Balance b = Balance.get();
		if (e.drawback()) {
			return b.drawbackHitSeconds * 20;
		}
		Balance.EffectData d = b.effect(e.key());
		double rel = Math.min(1.0, e.s() / b.cap);
		double curve = b.hitCurve(e.key(), "seconds", rel);
		if (!Double.isNaN(curve)) {
			return (int) Math.round(curve * 20.0);
		}
		if (kind(e).equals("control")) {
			return Math.max(POTENCY_FLOOR_TICKS, (int) Math.round(Math.pow(rel, 1.0 - b.weaponSplit) * d.hitTicks()));
		}
		double share = kind(e).equals("onoff") ? rel : Math.pow(rel, 1.0 - b.weaponSplit);
		int floor = d.hitFloorTicks();
		if (d.hitFloorHealHp() > 0.0) {
			floor = (int) Math.ceil(d.hitFloorHealHp() / (d.hitUnit() * GemMobEffect.potency(GemMobEffect.amplifier(potency(e)))) /* the effect heals at its 1% steps */ * 20.0);
		}
		return Math.max(floor, (int) Math.round(share * d.hitTicks()));
	}

	/** The magic damage or healing a Healing/Harming strike does per hit. */
	public static float strike(String key, double s) {
		Balance b = Balance.get();
		return (float) Math.max(b.hitStrikeFloor, b.effect(key).hitUnit() * s);
	}

	/** Applies a hit's entries to the target. */
	public static void apply(ServerLevel level, LivingEntity target, @Nullable Entity attacker, @Nullable Entity direct, List<Entry> entries) {
		apply(level, target, attacker, direct, entries, ItemStack.EMPTY);
	}

	/** Applies a hit's entries to the target. */
	public static void apply(ServerLevel level, LivingEntity target, @Nullable Entity attacker, @Nullable Entity direct, List<Entry> entries,
			ItemStack weapon) {
		Balance b = Balance.get();
		for (Entry e : entries) {
			String kind = kind(e);
			if (kind.equals("action")) {
				GemActions.hit(level, e.key(), e.s(), target, attacker, direct, weapon);
				continue;
			}
			if (kind.equals("instant")) {
				boolean harm = e.key().equals("instant_damage") != (b.undeadInversion && target.isInvertedHealAndHarm());
				if (harm) {
					int cooldown = target.damageCooldownTime;
					float lastHurt = ((io.github.bubba8587.bubbas_infused_gems.mixin.LivingEntityAccessor) target).bubbas$lastHurt();
					var strikeType = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
							io.github.bubba8587.bubbas_infused_gems.BubbasInfusedGems.id("gem_strike"));
					target.hurtServer(level, target.damageSources().source(strikeType, direct, attacker), strike("instant_damage", e.s()));
					target.damageCooldownTime = cooldown;
					((io.github.bubba8587.bubbas_infused_gems.mixin.LivingEntityAccessor) target).bubbas$setLastHurt(lastHurt);
				} else {
					target.heal(strike("instant_health", e.s()));
				}
				continue;
			}
			Holder<MobEffect> vanilla = ModEffects.vanilla(e.key());
			if (vanilla != null && !target.canBeAffected(new MobEffectInstance(vanilla))) {
				continue;
			}
			if (kind.equals("onoff")) {
				if (vanilla != null) {
					target.addEffect(new MobEffectInstance(vanilla, duration(e), 0), attacker);
				}
				continue;
			}
			Holder<MobEffect> effect = ModEffects.gem(e.key());
			if (effect != null) {
				target.addEffect(new MobEffectInstance(effect, duration(e), GemMobEffect.amplifier(potency(e))), attacker);
				Balance.EffectData d = b.effect(e.key());
				if (!e.drawback() && d.hitFloorExhaustion() > 0.0 && target instanceof net.minecraft.world.entity.player.Player player) {
					// Hunger: every hit costs at least a food point's worth
					double drained = d.hitUnit() * GemMobEffect.potency(GemMobEffect.amplifier(potency(e))) * duration(e);
					if (drained < d.hitFloorExhaustion()) {
						player.causeFoodExhaustion((float) (d.hitFloorExhaustion() - drained));
					}
				}
			}
		}
	}

	/** A weapon hit landed. */
	public static void onWeaponHit(ServerLevel level, @Nullable LivingEntity attacker, @Nullable Entity direct, LivingEntity target, ItemStack weapon) {
		if (weapon == null || weapon.isEmpty() || isRanged(weapon)) {
			return;
		}
		List<Entry> entries = plan(weapon);
		if (!entries.isEmpty()) {
			apply(level, target, attacker, direct, entries, weapon);
		}
	}

	/** Where a real tipped arrow carries an effect a gem also gives, the stronger one applies. */
	public static double vanillaFraction(String key, int amplifier) {
		return GemValues.levelFraction(key, amplifier);
	}

	public static String vanillaKey(Holder<MobEffect> effect) {
		return GemText.key(effect);
	}
}
