// dte:C4
package io.github.bubba8587.bubbas_infused_gems.effect;

import io.github.bubba8587.bubbas_infused_gems.BubbasInfusedGems;
import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import io.github.bubba8587.bubbas_infused_gems.registry.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

/**
 * Armor gems on the wearer (players and mobs). Attribute effects are transient vanilla attribute modifiers, set when
 * equipment or the wearer's effects change; the rest are small hooks the mixins call (tick, damage taken, after damage,
 * death, gravity, water drag, hunger, visibility, prices, night vision on the client). Not potion effect instances:
 * vanilla levels are whole numbers. Every value goes through {@link GemValues} (undead swap, immunity, and "the
 * stronger applies" against a vanilla effect of the same kind).
 */
public final class WornEffects {
	/** Always-on worn effects are re-applied for this long, checked this often (so they never run out while worn). */
	private static final int ALWAYS_ON_TICKS = 100;
	private static final int ALWAYS_ON_REFRESH = 20;

	private WornEffects() {
	}

	public static AttributeModifier.Operation operation(String name) {
		return switch (name) {
			case "add_multiplied_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
			case "add_multiplied_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
			default -> AttributeModifier.Operation.ADD_VALUE;
		};
	}

	private static Identifier id(String key, int i) {
		return BubbasInfusedGems.id("worn/" + key + "/" + i);
	}

	/** Modifier ids of a "wet only" effect (Conduit Power), shared by armor gems and the hit-applied gem effect. */
	public static Identifier wetId(String key, int i) {
		return BubbasInfusedGems.id("wet/" + key + "/" + i);
	}

	public static GemTotals of(LivingEntity entity) {
		return ((GemEntity) entity).bubbas$wornGems();
	}

	/**
	 * The attribute amount that makes gem s and a vanilla effect at fraction f together give the stronger of the two
	 * (multiplied operations divide out the vanilla part; added ones subtract it). 0 when vanilla is at least as strong.
	 */
	public static double combinedAmount(Balance.Attr a, double s, double f) {
		if (f <= 0.0) {
			return a.at(s);
		}
		if (f >= s) {
			return 0.0;
		}
		if (a.operation().equals("add_value")) {
			return a.at(s) - a.at(f);
		}
		return (1.0 + a.at(s)) / (1.0 + a.at(f)) - 1.0;
	}

	/** Recomputes the wearer's totals from its armor and updates its attribute modifiers (server). */
	public static void refresh(LivingEntity entity) {
		GemEntity g = (GemEntity) entity;
		double absorptionBefore = g.bubbas$wornGems().value("absorption");
		g.bubbas$setWornGems(GemTotals.ofArmor(entity));
		updateAttributes(entity);
		dropKeptEffects(entity);
		double absorptionNow = g.bubbas$wornGems().value("absorption");
		if (absorptionNow > absorptionBefore + 1e-9) {
			// Owner (playtest): a new or bigger shield fills at once on equip (and on joining or respawning), but only if
			// the last equip-fill was at least the refill time ago; otherwise it waits like a refill after damage, so
			// swapping gear can't refill it.
			int refill = (int) Balance.get().effect("absorption").wornNumber("refill_ticks", 600);
			if (entity.tickCount - g.bubbas$lastEquipFillTick() >= refill) {
				entity.setAbsorptionAmount(entity.getMaxAbsorption());
				g.bubbas$setLastEquipFillTick(entity.tickCount);
			} else {
				g.bubbas$setLastHurtTick(entity.tickCount);
			}
		}
	}

	/** Whether the gems keep this vanilla effect on the wearer now (always-on harmful gems; real invisibility at the full set). */
	private static boolean keptOn(LivingEntity entity, Balance.EffectData e) {
		double s = GemValues.rawWorn(entity, e.key());
		if (e.wornFlag("always_on")) {
			return s > 0.0;
		}
		return e.worn() != null && e.worn().has("full_at") && s >= e.wornNumber("full_at", 1.0) - 1e-9;
	}

	/** An effect instance the gems keep on (ambient, no particles, short): told apart from a real potion's. */
	private static boolean isKept(MobEffectInstance inst) {
		return inst.isAmbient() && !inst.isVisible() && !inst.isInfiniteDuration() && inst.getDuration() <= ALWAYS_ON_TICKS;
	}

	/** Gear changed: a kept-on effect the gems no longer give goes at once (taking a piece off ends real invisibility). */
	private static void dropKeptEffects(LivingEntity entity) {
		for (Balance.EffectData e : Balance.get().effects.values()) {
			if (e.worn() == null || !(e.wornFlag("always_on") || e.worn().has("full_at")) || keptOn(entity, e)) {
				continue;
			}
			Holder<MobEffect> vanilla = ModEffects.vanilla(e.key());
			MobEffectInstance inst = vanilla == null ? null : entity.getEffect(vanilla);
			if (inst != null && isKept(inst)) {
				entity.removeEffect(vanilla);
			}
		}
	}

	/** Attribute modifiers of every worn effect, after the vanilla effects the wearer has (also on effect changes). */
	public static void updateAttributes(LivingEntity entity) {
		Balance b = Balance.get();
		for (Balance.EffectData e : b.effects.values()) {
			if (e.wornAttributes().isEmpty()) {
				continue;
			}
			boolean wet = e.wornFlag("wet_only");
			double s = GemValues.rawWorn(entity, e.key());
			double f = GemValues.vanillaFraction(entity, e.key());
			for (int i = 0; i < e.wornAttributes().size(); i++) {
				Balance.Attr a = e.wornAttributes().get(i);
				Identifier id = wet ? wetId(e.key(), i) : id(e.key(), i);
				double amount = combinedAmount(a, s, f);
				AttributeInstance inst = attribute(entity, a.attribute());
				if (inst == null) {
					continue;
				}
				if (s > 0.0 && amount != 0.0 && !wet) {
					inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation(a.operation())));
				} else if (!wet || s <= 0.0) {
					inst.removeModifier(id);
				}
			}
		}
		if (entity.getAbsorptionAmount() > entity.getMaxAbsorption()) {
			entity.setAbsorptionAmount(entity.getMaxAbsorption());
		}
		if (entity.getHealth() > entity.getMaxHealth()) {
			entity.setHealth(entity.getMaxHealth());
		}
	}

	private static AttributeInstance attribute(LivingEntity entity, String name) {
		return BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(name)).map(entity::getAttribute).orElse(null);
	}

	/** Conduit Power: its attributes only while in water or rain (armor gems or a gem effect from a hit). */
	public static void wetTick(LivingEntity entity) {
		Balance.EffectData e = Balance.get().effect("conduit_power");
		double s = GemValues.value(entity, "conduit_power");
		boolean on = s > 0.0 && entity.isInWaterOrRain();
		for (int i = 0; i < e.wornAttributes().size(); i++) {
			Balance.Attr a = e.wornAttributes().get(i);
			AttributeInstance inst = attribute(entity, a.attribute());
			if (inst == null) {
				continue;
			}
			if (on) {
				inst.addOrUpdateTransientModifier(new AttributeModifier(wetId("conduit_power", i), a.at(s), operation(a.operation())));
			} else {
				inst.removeModifier(wetId("conduit_power", i));
			}
		}
	}

	/** Server tick of a wearer with gems: health over time, hunger, absorption refill, Conduit Power, pulsed effects. */
	public static void tick(ServerLevel level, LivingEntity entity, GemTotals t) {
		Balance b = Balance.get();
		double regen = GemValues.worn(entity, "regeneration");
		if (regen > 0.0 && entity.getHealth() < entity.getMaxHealth() && !entity.is(EntityTypeTags.IGNORES_POISON_AND_REGEN)) {
			entity.heal((float) (b.effect("regeneration").wornNumber("heal_per_second", 0.8) * regen / 20.0));
		}
		double hunger = GemValues.worn(entity, "hunger");
		if (hunger > 0.0 && entity instanceof Player player) {
			player.causeFoodExhaustion((float) (b.effect("hunger").wornNumber("exhaustion_per_tick", 0.015) * hunger));
		}
		if (entity.tickCount % 10 == 0) {
			double poison = GemValues.worn(entity, "poison");
			if (poison > 0.0 && !entity.is(EntityTypeTags.IGNORES_POISON_AND_REGEN)) {
				float dmg = (float) Math.min(b.effect("poison").wornNumber("damage_per_second", 1.667) * poison / 2.0, entity.getHealth() - 1.0F);
				if (dmg > 0.0F) {
					entity.hurtServer(level, entity.damageSources().magic(), dmg);
				}
			}
			double wither = GemValues.worn(entity, "wither");
			if (wither > 0.0) {
				entity.hurtServer(level, entity.damageSources().wither(), (float) (b.effect("wither").wornNumber("damage_per_second", 0.5) * wither / 2.0));
			}
		}
		if (t.value("absorption") > 0.0) {
			int refill = (int) b.effect("absorption").wornNumber("refill_ticks", 600);
			GemEntity g = (GemEntity) entity;
			if (entity.tickCount - g.bubbas$lastHurtTick() >= refill && entity.getAbsorptionAmount() < entity.getMaxAbsorption()) {
				entity.setAbsorptionAmount(entity.getMaxAbsorption());
			}
		}
		if (t.has("conduit_power")) {
			wetTick(entity);
		}
		// Harmful on/off gems that can't be partial (Glowing, Blindness, Nausea, Darkness): always on (owner), the vanilla
		// effect kept topped up; Invisibility likewise, but only at the perfect complete set (owner, Q33). Levitation is a
		// slow drift instead (gravity hook).
		if (entity.tickCount % ALWAYS_ON_REFRESH == 0) {
			for (Balance.EffectData e : b.effects.values()) {
				if (t.has(e.key()) && keptOn(entity, e)) {
					Holder<MobEffect> vanilla = ModEffects.vanilla(e.key());
					MobEffectInstance now = vanilla == null ? null : entity.getEffect(vanilla);
					if (vanilla != null && (now == null || (!now.isInfiniteDuration() && now.getDuration() < ALWAYS_ON_TICKS - ALWAYS_ON_REFRESH))) {
						entity.addEffect(new MobEffectInstance(vanilla, ALWAYS_ON_TICKS, 0, true, false, true));
					}
				}
			}
		}
	}

	/** Resistance against vanilla's: the strongest of Resistance gems, Turtle Master gems and a vanilla effect applies. */
	private static double resistanceFactor(LivingEntity entity) {
		Balance b = Balance.get();
		double gem = 0.0;
		for (String key : new String[] {"resistance", "turtle_master"}) {
			double s = Math.max(GemValues.rawWorn(entity, key), GemValues.hit(entity, key));
			gem = Math.max(gem, b.effect(key).wornNumber("damage_reduction", 0.4) * s);
		}
		if (gem <= 0.0) {
			return 1.0;
		}
		MobEffectInstance vanilla = entity.getEffect(MobEffects.RESISTANCE);
		double v = vanilla == null || !b.strongerApplies ? 0.0 : Math.min(1.0, (vanilla.getAmplifier() + 1) * 0.2);
		if (v >= 1.0) {
			return 1.0;
		}
		return b.strongerApplies ? (1.0 - Math.max(gem, v)) / (1.0 - v) : 1.0 - gem;
	}

	/** Damage after vanilla's resistance: gem Resistance and Turtle Master (worn and hit), Fire Resistance, worn Harming. */
	public static float modifyDamage(LivingEntity entity, DamageSource source, float damage) {
		if (damage <= 0.0F || source.is(DamageTypeTags.BYPASSES_EFFECTS)) {
			return damage;
		}
		Balance b = Balance.get();
		double factor = 1.0;
		if (!source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
			factor *= resistanceFactor(entity);
		}
		if (source.is(DamageTypeTags.IS_FIRE)) {
			factor *= 1.0 - b.effect("fire_resistance").wornNumber("fire_damage_reduction", 1.0) * GemValues.value(entity, "fire_resistance");
		}
		double harm = GemValues.rawWorn(entity, "instant_damage");
		if (harm > 0.0 && (source.getEntity() != null || source.is(DamageTypeTags.IS_FALL))) {
			factor *= 1.0 + b.healFraction(harm);
		}
		return (float) Math.max(damage * factor, 0.0);
	}

	/** After a hit lands: the Healing gem gives back a share of a survived attack or fall; Infested gems may hatch. */
	public static void afterDamage(LivingEntity entity, DamageSource source, float taken) {
		((GemEntity) entity).bubbas$setLastHurtTick(entity.tickCount);
		if (of(entity).isEmpty()) {
			return;
		}
		double heal = GemValues.rawWorn(entity, "instant_health");
		if (heal > 0.0 && taken > 0.0F && entity.isAlive() && (source.getEntity() != null || source.is(DamageTypeTags.IS_FALL))) {
			entity.heal((float) (taken * Balance.get().healFraction(heal)));
		}
		double infested = GemValues.worn(entity, "infested");
		if (infested > 0.0 && entity.level() instanceof ServerLevel level && !entity.hasEffect(MobEffects.INFESTED)
				&& entity.getRandom().nextDouble() < infested) {
			MobEffects.INFESTED.value().onMobHurt(level, entity, 0, source, taken); // vanilla's own 10% roll follows
		}
	}

	/** Wind Charged, Weaving and Oozing gems: their vanilla death trigger, with chance s (not on top of the real effect). */
	public static void onDeath(LivingEntity entity) {
		if (of(entity).isEmpty() || !(entity.level() instanceof ServerLevel level)) {
			return;
		}
		for (Balance.EffectData e : Balance.get().effects.values()) {
			if (e.worn() != null && e.worn().has("death_chance")) {
				Holder<MobEffect> vanilla = ModEffects.vanilla(e.key());
				double s = GemValues.worn(entity, e.key()) * e.wornNumber("death_chance", 1.0);
				if (vanilla != null && s > 0.0 && !entity.hasEffect(vanilla) && entity.getRandom().nextDouble() < s) {
					vanilla.value().onMobRemoved(level, entity, 0, Entity.RemovalReason.KILLED);
				}
			}
		}
	}

	/**
	 * Levitation gem worn: a slow drift up, vanilla's levitation formula with the rise speed s x rise_per_tick (owner:
	 * never flight). Slow Falling gem: gravity while falling, 0.08 - 0.07 x s at vanilla gravity (scaled for other gravity).
	 */
	public static double gravity(LivingEntity entity, double gravity) {
		double lev = GemValues.rawWorn(entity, "levitation");
		if (lev > 0.0 && !entity.hasEffect(MobEffects.LEVITATION) && !entity.isNoGravity() && !entity.isInLiquid() && !entity.isFallFlying()) {
			double rise = Balance.get().effect("levitation").wornNumber("rise_per_tick", 0.02) * lev;
			return -(rise - entity.getDeltaMovement().y) * 0.2; // travelInAir subtracts it: y += (rise - y) x 0.2, as vanilla does
		}
		if (entity.getDeltaMovement().y > 0.0) {
			return gravity;
		}
		double s = GemValues.value(entity, "slow_falling");
		if (s <= 0.0) {
			return gravity;
		}
		double reduction = Balance.get().effect("slow_falling").wornNumber("gravity_reduction", 0.07) / 0.08;
		return Math.min(gravity, entity.getGravity() * (1.0 - reduction * s));
	}

	/** Dolphin's Grace gem: the water slow-down s of the way to the effect's 0.96. */
	public static double waterDrag(LivingEntity entity, double slowDown) {
		if (entity.hasEffect(MobEffects.DOLPHINS_GRACE)) {
			return slowDown;
		}
		double s = GemValues.value(entity, "dolphins_grace");
		double target = Balance.get().effect("dolphins_grace").wornNumber("drag_target", 0.96);
		return s > 0.0 && slowDown < target ? slowDown + (target - slowDown) * s : slowDown;
	}

	/** Saturation gem: food exhaustion x (1 - s). */
	public static float exhaustion(Player player, float amount) {
		double s = GemValues.value(player, "saturation");
		return s > 0.0 ? (float) (amount * (1.0 - Balance.get().effect("saturation").wornNumber("exhaustion_reduction", 1.0) * s)) : amount;
	}

	/**
	 * Invisibility gem: mobs notice the wearer at (1 - 0.93 x s) of the range (stealth). Read before any vanilla
	 * Invisibility, since the full set adds the real effect on top (its body turns see-through; armor still shows).
	 */
	public static double visibility(LivingEntity entity, double percent) {
		double s = Math.max(GemValues.rawWorn(entity, "invisibility"), GemValues.hit(entity, "invisibility"));
		return s > 0.0 ? percent * (1.0 - Balance.get().effect("invisibility").wornNumber("visibility_reduction", 0.93) * s) : percent;
	}

	/** Hero of the Village gem: the price cut beside vanilla's; the stronger applies. */
	public static double heroModifier(Player player, double vanilla) {
		double s = GemValues.value(player, "hero_of_the_village");
		return s > 0.0 ? Math.max(vanilla, Balance.get().effect("hero_of_the_village").wornNumber("price_cut", 0.55) * s) : vanilla;
	}
}
