// dte:C4
package io.github.bubba8587.bubbas_infused_gems.effect;

import io.github.bubba8587.bubbas_infused_gems.BubbasInfusedGems;
import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

/**
 * Armor gems on the wearer (players and mobs). Attribute effects are transient vanilla attribute modifiers, set when
 * equipment changes; the rest are small hooks the mixins call (tick, damage taken, after damage, gravity, night
 * vision on the client). Not potion effect instances: vanilla levels are whole numbers.
 */
public final class WornEffects {
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

	public static GemTotals of(LivingEntity entity) {
		return ((GemEntity) entity).bubbas$wornGems();
	}

	/** Recomputes the wearer's totals from its armor and updates its attribute modifiers (server, on equipment change). */
	public static void refresh(LivingEntity entity) {
		GemTotals totals = GemTotals.ofArmor(entity);
		((GemEntity) entity).bubbas$setWornGems(totals);
		Balance b = Balance.get();
		for (String key : b.v0Worn) {
			Balance.EffectData e = b.effect(key);
			double s = totals.value(key);
			for (int i = 0; i < e.wornAttributes().size(); i++) {
				Balance.Attr a = e.wornAttributes().get(i);
				int index = i;
				BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(a.attribute())).ifPresent(h -> {
					AttributeInstance inst = entity.getAttribute(h);
					if (inst == null) {
						return;
					}
					if (s > 0.0) {
						inst.addOrUpdateTransientModifier(new AttributeModifier(id(key, index), a.at(s), operation(a.operation())));
					} else {
						inst.removeModifier(id(key, index));
					}
				});
			}
		}
		if (entity.getAbsorptionAmount() > entity.getMaxAbsorption()) {
			entity.setAbsorptionAmount(entity.getMaxAbsorption());
		}
	}

	/** Server tick of a wearer with gems: regeneration, poison, wither, hunger, absorption refill. */
	public static void tick(ServerLevel level, LivingEntity entity, GemTotals t) {
		Balance b = Balance.get();
		double regen = t.value("regeneration");
		if (regen > 0.0 && entity.getHealth() < entity.getMaxHealth() && !entity.is(EntityTypeTags.IGNORES_POISON_AND_REGEN)) {
			entity.heal((float) (b.effect("regeneration").wornNumber("heal_per_second", 0.8) * regen / 20.0));
		}
		double hunger = t.value("hunger");
		if (hunger > 0.0 && entity instanceof Player player) {
			player.causeFoodExhaustion((float) (b.effect("hunger").wornNumber("exhaustion_per_tick", 0.015) * hunger));
		}
		if (entity.tickCount % 10 == 0) {
			double poison = t.value("poison");
			if (poison > 0.0 && !entity.is(EntityTypeTags.IGNORES_POISON_AND_REGEN)) {
				float dmg = (float) Math.min(b.effect("poison").wornNumber("damage_per_second", 1.667) * poison / 2.0, entity.getHealth() - 1.0F);
				if (dmg > 0.0F) {
					entity.hurtServer(level, entity.damageSources().magic(), dmg);
				}
			}
			double wither = t.value("wither");
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
	}

	/** Damage after vanilla's resistance: gem Resistance (worn and hit-applied), Fire Resistance, worn Harming. */
	public static float modifyDamage(LivingEntity entity, DamageSource source, float damage) {
		if (damage <= 0.0F || source.is(DamageTypeTags.BYPASSES_EFFECTS)) {
			return damage;
		}
		Balance b = Balance.get();
		GemTotals t = of(entity);
		double factor = 1.0;
		if (!source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
			factor *= 1.0 - b.effect("resistance").wornNumber("damage_reduction", 0.8) * t.value("resistance");
			var gemResistance = io.github.bubba8587.bubbas_infused_gems.registry.ModEffects.gem("resistance");
			if (gemResistance != null && entity.hasEffect(gemResistance)) {
				factor *= ((GemMobEffect) gemResistance.value()).damageFactor(entity.getEffect(gemResistance).getAmplifier());
			}
		}
		if (source.is(DamageTypeTags.IS_FIRE)) {
			factor *= 1.0 - b.effect("fire_resistance").wornNumber("fire_damage_reduction", 1.0) * t.value("fire_resistance");
		}
		double harm = t.value("instant_damage");
		if (harm > 0.0 && (source.getEntity() != null || source.is(DamageTypeTags.IS_FALL))) {
			factor *= 1.0 + b.healFraction(harm);
		}
		return (float) Math.max(damage * factor, 0.0);
	}

	/** Healing gem: a survived attack or fall gives back a share of the health it took. */
	public static void afterDamage(LivingEntity entity, DamageSource source, float taken) {
		((GemEntity) entity).bubbas$setLastHurtTick(entity.tickCount);
		double heal = of(entity).value("instant_health");
		if (heal > 0.0 && taken > 0.0F && entity.isAlive() && (source.getEntity() != null || source.is(DamageTypeTags.IS_FALL))) {
			entity.heal((float) (taken * Balance.get().healFraction(heal)));
		}
	}

	/** Slow Falling gem: gravity while falling, 0.08 - 0.07 x s at vanilla gravity (scaled for other gravity). */
	public static double gravity(LivingEntity entity, double gravity) {
		double s = of(entity).value("slow_falling");
		if (s <= 0.0 || entity.getDeltaMovement().y > 0.0) {
			return gravity;
		}
		double reduction = Balance.get().effect("slow_falling").wornNumber("gravity_reduction", 0.07) / 0.08;
		return Math.min(gravity, entity.getGravity() * (1.0 - reduction * s));
	}
}
