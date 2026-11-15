// dte:C4
package io.github.bubba8587.bubbas_infused_gems.effect;

import java.util.List;
import java.util.function.BiConsumer;

import io.github.bubba8587.bubbas_infused_gems.BubbasInfusedGems;
import io.github.bubba8587.bubbas_infused_gems.balance.Balance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

/**
 * A gem's hit-applied effect (weapon gems, and weapon drawbacks on the target): the vanilla effect made partial, its
 * potency counted in 1% steps by the amplifier (amplifier 45 = 46% of the strongest vanilla level), with vanilla's icon
 * (client) and timer. Attribute effects use the balance model's worn attributes at that potency; damage, healing and
 * hunger tick at the model's rates; the hook effects (fire, falling, water drag, hunger, stealth, prices, night vision,
 * resistance) read the potency through {@link GemValues}. Turtle Master's own effect is one of these too.
 */
public class GemMobEffect extends MobEffect {
	private static final int DAMAGE_INTERVAL = 10;
	private final String key;
	private final List<Balance.Attr> attrs;
	private final boolean wet;

	public GemMobEffect(String key, MobEffectCategory category, int color) {
		super(category, color);
		this.key = key;
		Balance.EffectData e = Balance.get().effect(key);
		this.wet = e != null && e.wornFlag("wet_only");
		this.attrs = e == null || this.wet ? List.of() : e.wornAttributes();
	}

	public String key() {
		return this.key;
	}

	public static double potency(int amplifier) {
		return Math.clamp((amplifier + 1) / 100.0, 0.0, 1.0);
	}

	public static int amplifier(double potency) {
		return Math.clamp((int) Math.round(potency * 100.0) - 1, 0, 99);
	}

	private Identifier modifierId(int i) {
		return BubbasInfusedGems.id("effect/" + this.key + "/" + i);
	}

	@Override
	public void createModifiers(int amplifier, BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
		double p = potency(amplifier);
		for (int i = 0; i < this.attrs.size(); i++) {
			Balance.Attr a = this.attrs.get(i);
			Identifier id = this.modifierId(i);
			BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(a.attribute())).ifPresent(h -> consumer.accept(h,
					new AttributeModifier(id, a.at(p), WornEffects.operation(a.operation()))));
		}
	}

	@Override
	public void removeAttributeModifiers(AttributeMap attributes) {
		for (int i = 0; i < this.attrs.size(); i++) {
			Identifier id = this.modifierId(i);
			BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(this.attrs.get(i).attribute())).ifPresent(h -> {
				AttributeInstance inst = attributes.getInstance(h);
				if (inst != null) {
					inst.removeModifier(id);
				}
			});
		}
		if (this.wet) {
			List<Balance.Attr> wetAttrs = Balance.get().effect(this.key).wornAttributes();
			for (int i = 0; i < wetAttrs.size(); i++) {
				Identifier id = WornEffects.wetId(this.key, i);
				BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(wetAttrs.get(i).attribute())).ifPresent(h -> {
					AttributeInstance inst = attributes.getInstance(h);
					if (inst != null) {
						inst.removeModifier(id);
					}
				});
			}
		}
	}

	@Override
	public void addAttributeModifiers(AttributeMap attributes, int amplifier) {
		this.createModifiers(amplifier, (h, modifier) -> {
			AttributeInstance inst = attributes.getInstance(h);
			if (inst != null) {
				inst.removeModifier(modifier.id());
				inst.addPermanentModifier(modifier);
			}
		});
	}

	@Override
	public void onEffectAdded(LivingEntity mob, int amplifier) {
		super.onEffectAdded(mob, amplifier);
		if (this.key.equals("absorption")) {
			// The shield fills when the effect is new, not on every refreshing hit.
			float shield = (float) Balance.get().effect("absorption").wornAttributes().getFirst().at(potency(amplifier));
			mob.setAbsorptionAmount(Math.max(mob.getAbsorptionAmount(), shield));
		}
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
		return switch (this.key) {
			case "regeneration", "hunger", "conduit_power", "saturation" -> true;
			case "poison", "wither" -> tickCount % DAMAGE_INTERVAL == 0;
			default -> false;
		};
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
		double p = potency(amplification);
		Balance.EffectData e = Balance.get().effect(this.key);
		switch (this.key) {
			case "regeneration" -> {
				if (!mob.is(EntityTypeTags.IGNORES_POISON_AND_REGEN) && mob.getHealth() < mob.getMaxHealth()) {
					mob.heal((float) (e.hitUnit() * p / 20.0));
				}
			}
			case "hunger" -> {
				if (mob instanceof Player player) {
					player.causeFoodExhaustion((float) (e.hitUnit() * p));
				}
			}
			case "poison" -> {
				float dmg = (float) Math.min(e.hitUnit() * p * DAMAGE_INTERVAL / 20.0, mob.getHealth() - 1.0F);
				if (dmg > 0.0F && !mob.is(EntityTypeTags.IGNORES_POISON_AND_REGEN)) {
					mob.hurtServer(level, mob.damageSources().magic(), dmg);
				}
			}
			case "wither" -> mob.hurtServer(level, mob.damageSources().wither(), (float) (e.hitUnit() * p * DAMAGE_INTERVAL / 20.0));
			case "conduit_power" -> WornEffects.wetTick(mob);
			case "saturation" -> {
				if (mob instanceof Player player) {
					WornEffects.restoreFood(player, p);
				}
			}
			default -> {
			}
		}
		return true;
	}

	public static MobEffectCategory category(boolean good) {
		return good ? MobEffectCategory.BENEFICIAL : MobEffectCategory.HARMFUL;
	}
}
