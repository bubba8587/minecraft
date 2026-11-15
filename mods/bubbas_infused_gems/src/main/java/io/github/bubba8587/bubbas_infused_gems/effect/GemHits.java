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

/**
 * Weapon gems on what they hit (melee, thrown tridents) or on the arrow a bow or crossbow fires. One hit applies, per
 * effect, the model's per-hit form against the strongest potion of the effect (owner): potency s^0.5 and duration
 * s^0.5 of its 1.0 (scaled effects, as gem mob effects), the vanilla effect for s of the 1.0 duration (on/off harmful
 * effects), or a small strike on every hit (Healing, Harming; owner). Drawbacks land on the target for 3 s, refreshed by each
 * hit. A gem effect and a vanilla effect of the same kind don't add: the stronger applies (LivingEntityMixin).
 */
public final class GemHits {
	/** One thing a hit applies: an effect at weapon strength s (or a drawback of size s). */
	public record Entry(String key, double s, boolean drawback) {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("effect").forGetter(Entry::key),
				Codec.DOUBLE.fieldOf("s").forGetter(Entry::s),
				Codec.BOOL.optionalFieldOf("drawback", false).forGetter(Entry::drawback)
		).apply(i, Entry::new));
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
		t.totals().forEach((k, s) -> out.add(new Entry(k, s, false)));
		t.drawbacks().forEach((k, s) -> out.add(new Entry(k, s, true)));
		out.sort((a, b) -> a.key().compareTo(b.key()));
		return out;
	}

	private static String kind(Entry e) {
		return e.drawback() ? "scaled" : Balance.get().effect(e.key()).hitKind();
	}

	/** Potency (fraction of the effect's 1.0 level) of one hit. */
	public static double potency(Entry e) {
		return switch (kind(e)) {
			case "onoff" -> 1.0;
			case "instant" -> e.s();
			default -> e.drawback() ? e.s() : Math.pow(e.s(), Balance.get().weaponSplit);
		};
	}

	/**
	 * Duration in ticks of one hit (owner): at most hit.ticks (8 s) at the best weapon, less with less strength, never
	 * under the effect's floor (a few seconds, or for Regeneration long enough to heal its floor amount).
	 */
	public static int duration(Entry e) {
		Balance b = Balance.get();
		if (e.drawback()) {
			return b.drawbackHitSeconds * 20;
		}
		Balance.EffectData d = b.effect(e.key());
		double rel = Math.min(1.0, e.s() / b.cap);
		double share = kind(e).equals("onoff") ? rel : Math.pow(rel, 1.0 - b.weaponSplit);
		int floor = d.hitFloorTicks();
		if (d.hitFloorHealHp() > 0.0) {
			floor = (int) Math.ceil(d.hitFloorHealHp() / (d.hitUnit() * potency(e)) * 20.0);
		}
		return Math.max(floor, (int) Math.round(share * d.hitTicks()));
	}

	/** The magic damage or healing a Healing/Harming strike does per hit: hit_unit x s, at least the floor. */
	public static float strike(String key, double s) {
		Balance b = Balance.get();
		return (float) Math.max(b.hitStrikeFloor, b.effect(key).hitUnit() * s);
	}

	/** Applies a hit's entries to the target. */
	public static void apply(ServerLevel level, LivingEntity target, @Nullable Entity attacker, @Nullable Entity direct, List<Entry> entries) {
		Balance b = Balance.get();
		for (Entry e : entries) {
			String kind = kind(e);
			if (kind.equals("instant")) {
				boolean harm = e.key().equals("instant_damage") != (b.undeadInversion && target.isInvertedHealAndHarm());
				if (harm) {
					// The strike is its own damage, not part of the hit: without this the hit's invulnerability frames would
					// swallow all but the part above the weapon's damage.
					target.damageCooldownTime = 0;
					target.hurtServer(level, target.damageSources().indirectMagic(direct, attacker), strike("instant_damage", e.s()));
				} else {
					target.heal(strike("instant_health", e.s()));
				}
				continue;
			}
			Holder<MobEffect> vanilla = ModEffects.vanilla(e.key());
			if (vanilla != null && !target.canBeAffected(new MobEffectInstance(vanilla))) {
				continue; // a spider shrugs off poison, a wither skeleton wither, as in vanilla
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
			}
		}
	}

	/**
	 * A weapon hit landed (vanilla's post-attack hook: players' melee and stabs, mobs' melee, sweeps, thrown tridents).
	 * Bows and crossbows are skipped: their arrows carry the gems.
	 */
	public static void onWeaponHit(ServerLevel level, @Nullable LivingEntity attacker, @Nullable Entity direct, LivingEntity target, ItemStack weapon) {
		if (weapon == null || weapon.isEmpty() || isRanged(weapon)) {
			return;
		}
		List<Entry> entries = plan(weapon);
		if (!entries.isEmpty()) {
			apply(level, target, attacker, direct, entries);
		}
	}

	/** Where a real tipped arrow carries an effect a gem also gives, the stronger one applies (owner). */
	public static double vanillaFraction(String key, int amplifier) {
		return GemValues.levelFraction(key, amplifier);
	}

	public static String vanillaKey(Holder<MobEffect> effect) {
		return GemText.key(effect);
	}
}
